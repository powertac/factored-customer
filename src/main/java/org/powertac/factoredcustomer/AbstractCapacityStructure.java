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
 * Abstract type for capacity substructures. Each subclass matches an xml
 * element tag s.t. for "tag" there must be a subclass named
 * org.powertac.factoredcustomer.tagStructure.
 * @author John Collins
 */
public abstract class AbstractCapacityStructure
{
  protected CapacityStructure master;

  public AbstractCapacityStructure (CapacityStructure master)
  {
    super();
    this.master = master;
  }

  // abstract structure handler
  abstract void decodeElement (Element clause);
}
