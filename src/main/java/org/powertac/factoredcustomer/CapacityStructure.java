/* Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.factoredcustomer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.*;

/**
 * Data-holder class for parsed configuration elements of one capacity.
 * All members are declared final in the package scope.
 * 
 * @author Prashant Reddy
 */
public class CapacityStructure
{
  protected Logger log = Logger.getLogger(CapacityStructure.class.getName());

  public enum InfluenceKind {
    DIRECT, DEVIATION, NONE
  }

  public enum BaseCapacityType {
    POPULATION, INDIVIDUAL, TIMESERIES
  }

  public enum ElasticityModelType {
    CONTINUOUS, STEPWISE
  }

  final String capacityName;
  final String description;
  
  FactoredCustomerService service;

  BaseCapacityType baseCapacityType;
  ProbabilityDistribution basePopulationCapacity;
  ProbabilityDistribution baseIndividualCapacity;
  TimeseriesStructure baseTimeseriesStructure;

  double[] dailySkew;
  double[] hourlySkew;

  InfluenceKind temperatureInfluence;
  // key: degrees Celsius
  final Map<Integer, Double> temperatureMap = new HashMap<Integer, Double>();
  double temperatureReference;
  InfluenceKind windSpeedInfluence;
  // key: speed in m/s
  final Map<Integer, Double> windSpeedMap = new HashMap<Integer, Double>();
  InfluenceKind windDirectionInfluence;
  // key: angle 0-360
  final Map<Integer, Double> windDirectionMap = new HashMap<Integer, Double>();
  InfluenceKind cloudCoverInfluence;
  // key: 0 (clear) -- 100 (overcast)
  final Map<Integer, Double> cloudCoverMap = new HashMap<Integer, Double>();
  // key: hour of day
  final Map<Integer, Double> benchmarkRates = new HashMap<Integer, Double>();
  ElasticityModelType elasticityModelType;
  Element elasticityModelXml;

  // sub-structures
  final Map<String, AbstractCapacityStructure> subStructures =
    new HashMap<String, AbstractCapacityStructure>();

  double[] curtailmentShifts; // index = timeslot

  CapacityStructure (FactoredCustomerService service, String name, Element clause,
                     DefaultCapacityBundle bundle)
  {
    capacityName = name;
    description = clause.getAttribute("description");
    this.service = service;

    NodeList capacityNodes = clause.getChildNodes();
    for (int i = 0; i < capacityNodes.getLength(); i++) {
      Node capacityNode = capacityNodes.item(i);
      if (capacityNode.getNodeType() == Node.ELEMENT_NODE) {
        Element capacityElement = (Element)capacityNode;
        String tag = capacityElement.getTagName();
        if (tag.equals("baseCapacity"))
          processBaseCapacity(capacityElement);
        else if (tag.equals("influenceFactors"))
          processInfluenceFactors(capacityElement);
        else {
          // convert tag to classname, look up class
          String classname =
            getPackageName() + "." + StringUtils.capitalize(tag) + "Structure";
          try {
            // look up the class, create an instance, and call its
            // handleClause() method
            Class<?> clazz = Class.forName(classname);
            // cannot use this.getClass() in constructor...
            Class<?> arg = CapacityStructure.class;
            Constructor<?> constructor =
              (Constructor<?>) clazz.getConstructor(CapacityStructure.class);
            AbstractCapacityStructure instance =
              (AbstractCapacityStructure) constructor.newInstance(this);
            subStructures.put(tag, instance);
            instance.decodeElement(capacityElement);
          }
          catch (ClassNotFoundException e) {
            log.error("Capacity handler class " + classname + " not found");
          }
          catch (NoSuchMethodException e) {
            log.error("Cannot find method: " + e.toString());
          }
          catch (Exception e) {
            log.error(e.toString());
          }
        }
      }
    }
  }

  private void processBaseCapacity (Element clause)
  {
    baseCapacityType =
      Enum.valueOf(BaseCapacityType.class, clause.getAttribute("type"));
    switch (baseCapacityType) {
    case POPULATION:
      Element populationCapacityElement =
        (Element) clause.getElementsByTagName("populationCapacity").item(0);
      basePopulationCapacity =
        new ProbabilityDistribution(service, populationCapacityElement);
      baseIndividualCapacity = null;
      baseTimeseriesStructure = null;
      break;
    case INDIVIDUAL:
      basePopulationCapacity = null;
      Element individualCapacityElement =
        (Element) clause.getElementsByTagName("individualCapacity").item(0);
      baseIndividualCapacity =
        new ProbabilityDistribution(service, individualCapacityElement);
      baseTimeseriesStructure = null;
      break;
    case TIMESERIES:
      basePopulationCapacity = null;
      baseIndividualCapacity = null;
      Element timeseriesModelElement =
        (Element) clause.getElementsByTagName("timeseriesModel").item(0);
      baseTimeseriesStructure = new TimeseriesStructure(timeseriesModelElement);
      break;
    default:
      throw new Error("Unexpected base capacity type: " + baseCapacityType);
    }
  }

  private void processInfluenceFactors (Element xml)
  {
    Element dailySkewElement =
      (Element) xml.getElementsByTagName("dailySkew").item(0);
    dailySkew =
      ParserFunctions.parseDoubleArray(dailySkewElement.getAttribute("array"));

    Element hourlySkewElement =
      (Element) xml.getElementsByTagName("hourlySkew").item(0);
    hourlySkew =
      ParserFunctions.parseDoubleArray(hourlySkewElement.getAttribute("array"));

    Element temperatureInfluenceElement =
      (Element) xml.getElementsByTagName("temperature").item(0);
    temperatureInfluence =
      Enum.valueOf(InfluenceKind.class,
                   temperatureInfluenceElement.getAttribute("influence"));
    if (temperatureInfluence != InfluenceKind.NONE) {
      ParserFunctions.parseRangeMap(temperatureInfluenceElement
          .getAttribute("rangeMap"), temperatureMap);
      if (temperatureInfluence == InfluenceKind.DEVIATION) {
        temperatureReference =
          Double.parseDouble(temperatureInfluenceElement
              .getAttribute("reference"));
      }
      else
        temperatureReference = Double.NaN;
    }
    else
      temperatureReference = Double.NaN;

    Element windSpeedInfluenceElement =
      (Element) xml.getElementsByTagName("windSpeed").item(0);
    windSpeedInfluence =
      Enum.valueOf(InfluenceKind.class,
                   windSpeedInfluenceElement.getAttribute("influence"));
    if (windSpeedInfluence != InfluenceKind.NONE) {
      ParserFunctions.parseRangeMap(windSpeedInfluenceElement
          .getAttribute("rangeMap"), windSpeedMap);
    }

    Element windDirectionInfluenceElement =
      (Element) xml.getElementsByTagName("windDirection").item(0);
    windDirectionInfluence =
      Enum.valueOf(InfluenceKind.class,
                   windDirectionInfluenceElement.getAttribute("influence"));
    if (windDirectionInfluence != InfluenceKind.NONE) {
      ParserFunctions.parseRangeMap(windDirectionInfluenceElement
          .getAttribute("rangeMap"), windDirectionMap);
    }

    Element cloudCoverInfluenceElement =
      (Element) xml.getElementsByTagName("cloudCover").item(0);
    cloudCoverInfluence =
      Enum.valueOf(InfluenceKind.class,
                   cloudCoverInfluenceElement.getAttribute("influence"));
    if (cloudCoverInfluence != InfluenceKind.NONE) {
      ParserFunctions.parseRangeMap(cloudCoverInfluenceElement
          .getAttribute("percentMap"), cloudCoverMap);
    }

    Element priceElasticityElement =
      (Element) xml.getElementsByTagName("priceElasticity").item(0);
    Element benchmarkRatesElement =
      (Element) priceElasticityElement.getElementsByTagName("benchmarkRates")
          .item(0);
    ParserFunctions.parseRangeMap(benchmarkRatesElement
        .getAttribute("rangeMap"), benchmarkRates);

    elasticityModelXml =
      (Element) priceElasticityElement.getElementsByTagName("elasticityModel")
          .item(0);
    elasticityModelType =
      Enum.valueOf(ElasticityModelType.class,
                   elasticityModelXml.getAttribute("type"));

    Element curtailmentElement =
      (Element) xml.getElementsByTagName("curtailment").item(0);
    curtailmentShifts =
      (curtailmentElement != null)? ParserFunctions
          .parseDoubleArray(curtailmentElement.getAttribute("shifts")): null;
  }

  // extract package name from fully-qualified classname
  private String getPackageName () {
    String fullName = this.getClass().getName();
    int lastDot = fullName.lastIndexOf ('.');
    if (lastDot==-1){ return ""; }
    return fullName.substring (0, lastDot);
   }
} // end class

