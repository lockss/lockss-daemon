/*
 * $Id$
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

package org.lockss.uiapi.util;

import java.util.*;


/**
 * An ArrayList wrapper that supports named entries and preserves list order
 */
public class KeyedList {

  private ArrayList keys;
  private ArrayList values;


  /**
   * Constructor
   */
  public KeyedList() {
    this.keys   = new ArrayList();
    this.values = new ArrayList();
  }

  /**
   * Put a named parameter into the list - names and values cannot be null
   * @param name Parameter name
   * @param value Content
   */
  public void put(Object name, Object value) {
    verifyName(name);
    keys.add(name);
    values.add(value);
  }

  /**
   * Fetch the first occurance of a named parameter from the list
   * @param name Parameter name
   * @return String content
   */
  public Object get(Object name) {
    int index = keys.indexOf(name);

    if (index == -1) {
      return null;
    }
    verifySize();
    return values.get(index);
  }

  /**
   * Fetch a key by index
   * @param index Index of list item
   * @return Item name
   */
  public Object getKey(int index) {
    verifySize();
    return keys.get(index);
  }

  /**
   * Fetch a value by index
   * @param index Index of list item
   * @return Item value
   */
  public Object getValue(int index) {
    verifySize();
    return values.get(index);
  }

  /**
   * Fetch the size of the list
   * @return List size
   */
  public int size() {
    verifySize();
    return keys.size();
  }

  /**
   * Verify list lengths are in synch
   */
  private void verifySize() {

    if (keys.size() != values.size()) {
      throw new IllegalStateException("Key list size ("
                                    + keys.size()
                                    + ") is not equal to value list size ("
                                    + values.size()
                                    + ")");
    }
  }

  /**
   * Verify that name is non-null
   * @param name Name to verify
   * @throws IllegalArgumentException if name is null
   */
  private void verifyName(Object name) {
    if (name == null) {
      throw new IllegalArgumentException("Key name cannot be null");
     }
  }
}
