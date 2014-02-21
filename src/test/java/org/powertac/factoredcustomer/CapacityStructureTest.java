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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.repo.RandomSeedRepo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author jcollins
 */
public class CapacityStructureTest
{

  private Node capacityNode;
  private RandomSeedRepo randomSeedRepo;
  private FactoredCustomerService service;
  private CustomerStructure customerStructure;
  private DefaultCapacityBundle capacityBundle;

  @Before
  public void setUp () throws Exception
  {
    randomSeedRepo = new RandomSeedRepo();
    service = mock(FactoredCustomerService.class);
    when(service.getRandomSeedRepo()).thenReturn(randomSeedRepo);

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    //Get the DOM Builder
    DocumentBuilder builder = factory.newDocumentBuilder();
    //Load and Parse the XML document
    Document document =
      builder.parse(this.getClass().getClassLoader()
          .getResourceAsStream("customers/Frosty.xml"));
    Element top =
      (Element) (document.getDocumentElement().getElementsByTagName("customer")
          .item(0));
    customerStructure =
      new CustomerStructure("test-customer", top);
    Node bundleNode = top.getElementsByTagName("capacityBundle").item(0);
    capacityNode =
      ((Element) bundleNode).getElementsByTagName("capacity").item(0);
    capacityBundle =
      new DefaultCapacityBundle(service, customerStructure,
                                (Element) bundleNode);
  }

  /**
   * Test method for {@link org.powertac.factoredcustomer.CapacityStructure#CapacityStructure(org.powertac.factoredcustomer.FactoredCustomerService, java.lang.String, org.w3c.dom.Element, org.powertac.factoredcustomer.DefaultCapacityBundle)}.
   */
  @Test
  public void testCapacityStructure ()
  {
    CapacityStructure struct =
      new CapacityStructure(service, "test", (Element) capacityNode,
                            capacityBundle);
    assertNotNull("created something", struct);
    assertEquals("correct name", "test", struct.capacityName);
    assertEquals("correct number of substructures", 1,
                 struct.subStructures.size());
    AbstractCapacityStructure acs =
      struct.subStructures.get("thermalStorageCapacity");
    assertNotNull("contains correct substructure");
    assertEquals("correct class",
                 "org.powertac.factoredcustomer.ThermalStorageCapacityStructure",
                 acs.getClass().getName());
    ThermalStorageCapacityStructure tsc = (ThermalStorageCapacityStructure) acs;
    assertEquals("COP", 3.0, tsc.performanceCoefficient, 1e-6);
    assertEquals("up-reg", 0.0, tsc.maxUpRegulation, 1e-6);
    assertEquals("down-reg", 500.0, tsc.maxDownRegulation, 1e-6);
    assertEquals("load", 16.0, tsc.constantLoad, 1e-6);
    assertEquals("loss/K", 0.324, tsc.lossPerK, 1e-6);
    assertEquals("heat cap", 570.0, tsc.heatCapacity, 1e-6);
    assertEquals("low temp", -35.0, tsc.lowTempLimit, 1e-6);
    assertEquals("high temp", -15.0, tsc.highTempLimit, 1e-6);
    assertEquals("nominal temp", -25.0, tsc.nominalTemp, 1e-6);
  }
}
