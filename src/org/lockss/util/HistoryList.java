/*
 * $Id: HistoryList.java,v 1.4 2007-01-18 02:42:52 tlipkis Exp $
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
 * A list with a maximum size, which discards elements as new ones are added
 */
public class HistoryList extends AbstractList {

  private int max;
  private List list;

  /**
   * Create a new HistoryList with room for n elements.
   * @param max maximum number of elements to keep on the list.
   */
  public HistoryList(int max) {
    this.max = max;
    list = new LinkedList();
  }

  /**
   * Add to the end of the list, discarding an old entry if necessary to
   * keep list within maximum size.
   * @param obj the object to add
   */
  public boolean add(Object obj) {
    trim(max - 1);
    list.add(obj);
    return true;
  }

  /**
   * Change the maximum number of elements kept on the list.
   * @param max maximum number of elements to keep on the list.
   */
  public void setMax(int max) {
    this.max = max;
    trim(max);
  }

  public Iterator iterator() {
    return list.iterator();
  }

  public int size() {
    return list.size();
  }

  public Object get(int ix) {
    return list.get(ix);
  }

  public Object set(int ix, Object element) {
    return list.set(ix, element);
  }

  private void trim(int n) {
    for (int s = list.size(); s > n; s--) {
      list.remove(0);
    }
  }

}
