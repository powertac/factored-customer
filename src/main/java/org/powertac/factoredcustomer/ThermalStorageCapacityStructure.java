/*
 * Copyright (c) 2014 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.factoredcustomer;

import org.w3c.dom.Element;

/**
 * CapacityStructure that decodes xml elements with tag="thermalStorageCapacity"
 * @author John Collins
 */
public class ThermalStorageCapacityStructure extends AbstractCapacityStructure
{
  // current state
  double temperature = 0.0;

  // model parameters
  double performanceCoefficient = 1.0; // kW(heat)/kW(elec)
  double maxUpRegulation = 0.0; // kW
  double maxDownRegulation = 0.0; // kW
  double constantLoad = 0.0; // kW
  double lossPerK = 0.0; // kW/K
  double heatCapacity = 0.0; // kWh/K
  double lowTempLimit = 0.0; // deg C
  double highTempLimit = 0.0; // deg C
  double nominalTemp = 0.0; // deg C

  public ThermalStorageCapacityStructure (CapacityStructure master)
  {
    super(master);
  }

  @Override
  void decodeElement (Element xml)
  {
    Element clause =
        (Element) xml.getElementsByTagName("performanceCoefficient").item(0);
    performanceCoefficient = Double.parseDouble(clause.getAttribute("value"));

    clause = (Element) xml.getElementsByTagName("maxRegulation").item(0);
    maxUpRegulation = Double.parseDouble(clause.getAttribute("up"));
    maxDownRegulation = Double.parseDouble(clause.getAttribute("down"));

    clause = (Element) xml.getElementsByTagName("heatLoss").item(0);
    constantLoad = Double.parseDouble(clause.getAttribute("constant"));
    lossPerK = Double.parseDouble(clause.getAttribute("perK"));

    clause = (Element) xml.getElementsByTagName("heatCapacity").item(0);
    heatCapacity = Double.parseDouble(clause.getAttribute("value"));

    clause = (Element) xml.getElementsByTagName("tempRange").item(0);
    lowTempLimit = Double.parseDouble(clause.getAttribute("low"));
    highTempLimit = Double.parseDouble(clause.getAttribute("high"));
    nominalTemp = Double.parseDouble(clause.getAttribute("nominal"));

    // initialize
    temperature = nominalTemp; // TODO - should be random initial state
  }

}
