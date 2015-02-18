/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

*/

package org.lockss.util;

import java.io.Serializable;
import java.util.*;

/**
 * ExtMapBean: class to support Castor XML export of a Map.  Because Castor
 * is broken, and loses 'key' information when exporting a Map, as well as being
 * unable to marshall a List of Lists or Object[]s, this class
 * creates two Lists of keys and values for marshalling.
 */
public class ExtMapBean implements Serializable {
  private HashMap elements;


  /**
   * Null constructor used by the marshaller to  construct the class
   */
  public ExtMapBean() {
    elements = new HashMap();
  }

  /**
   * Constructor used by code to create a new class for a given HashMap
   * in preparation for mashalling
   * @param map HashMap
   */
  public ExtMapBean(HashMap map) {
    elements = map;
  }

  /**
   * Returns an ArrayList collection to be used by the marshaller to store
   * the HashMap as a an array wrapped as ExtMapElements.
   * @return Collection an ArrayList of ExtMapElement
   */
  public Collection getValues() {
    Collection map_values = new ArrayList(elements.size());
    Set keyset = elements.keySet();
    for (Iterator it = keyset.iterator(); it.hasNext(); ) {
      String key = (String) it.next();
      map_values.add(new ExtMapElement(key, elements.get(key)));
    }
    return map_values;
  }

  /**
   * Used by the marshaller to add a single element from the xml file into the
   * HashMap.
   * @param element ExtMapElement which to be stored as a HashMap entry
   */
  public void addElement(ExtMapElement element) {
    elements.put(element.getElementKey(), element.getElementValue());
  }

  public void setValues(Collection values) {
    for(Iterator it= values.iterator();it.hasNext();) {
      addElement((ExtMapElement) it.next());
    }
  }
  public HashMap getMap() {
    return elements;
  }

  void setMap(HashMap map) {
    elements = map;
  }
}
