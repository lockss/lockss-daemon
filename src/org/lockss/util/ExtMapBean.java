/*
 * $Id: ExtMapBean.java,v 1.2 2003-11-19 23:51:10 eaalto Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;

/**
 * ExtMapBean: class to support Castor XML export of a Map.  Because Castor
 * is broken, and loses 'key' information when exporting a Map, as well as being
 * unable to marshall a List of Lists or Object[]s, this class
 * creates two Lists of keys and values for marshalling.
 */
public class ExtMapBean {
  public ArrayList keyList;
  public ArrayList valueList;

  /**
   * Empty constructor for bean creation during marshalling
   */
  public ExtMapBean() {
  }

  /**
   * Accessor for the intermediate list form of keys.
   * @return the list
   */
  public ArrayList getKeyList() {
    return keyList;
  }

  /**
   * Setter for the intermediate list form of keys.
   * @param list the new list
   */
  public void setKeyList(ArrayList list) {
    keyList = list;
  }

  /**
   * Accessor for the intermediate list form of values.
   * @return the list
   */
  public ArrayList getValueList() {
    return valueList;
  }

  /**
   * Setter for the intermediate list form of values.
   * @param list the new list
   */
  public void setValueList(ArrayList list) {
    valueList = list;
  }

  /**
   * Constructs the map of ExternalizableMapEntries from the internal lists.
   * @return a HashMap of externalizable map entries
   */
  public HashMap getMapFromLists() {
    HashMap map = new HashMap();
    if ((keyList!=null) && (keyList.size() > 0)) {
      for (int ii=0; ii<keyList.size(); ii++) {
        if ((valueList==null) || (ii>valueList.size())) {
          break;
        }
        Object value = valueList.get(ii);
        if (value!=null) {
          map.put(keyList.get(ii), value);
        }
      }
    }
    return map;
  }

  /**
   * Sets the internal lists from a map of ExternalizableMapEntries.
   * @param map a Map of externalizable map entries
   */
  public void setListsFromMap(Map map) {
    keyList = new ArrayList();
    valueList = new ArrayList();
    Iterator entries = map.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      keyList.add(entry.getKey());
      valueList.add(entry.getValue());
    }
  }
}