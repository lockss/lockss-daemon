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

package org.lockss.alert;

import java.util.*;

/** AlertLog keeps a history of recent alerts */
public class  AlertLog {

  private String name;
  private int max;
  private List list;

  /**
   * Create a new AlertLog with room for 100 elements.
   */
  public AlertLog(String name) {
    this(name, 100);
  }

  /**
   * Create a new AlertLog with room for n elements.
   * @param capacity maximum number of elements to keep on the list.
   */
  public AlertLog(String name, int capacity) {
    this.max = capacity;
    this.name = name;
    list = new LinkedList();
  }

  public String getName() {
    return name;
  }

  /**
   * Change the maximum number of elements kept on the list.
   * @param max maximum number of elements to keep on the list.
   */
  public void setMax(int max) {
    this.max = max;
    trim(max);
  }

  /**
   * Add to the end of the list, discarding an old entry if necessary to
   * keep list within maximum size.
   * @param alert the alert to add
   */
  public boolean add(Alert alert) {
    trim(max - 1);
    list.add(alert);
    return true;
  }

  public Iterator iterator() {
    return list.iterator();
  }

  public int size() {
    return list.size();
  }

  public Alert get(int index) {
    return (Alert)list.get(index);
  }

  public boolean isEmpty() {
    return list.isEmpty();
  }

  private void trim(int n) {
    for (int s = list.size(); s > n; s--) {
      list.remove(0);
    }
  }

  public String toString() {
    return "[AlertLog: " + name + list + "]";
  }
}
