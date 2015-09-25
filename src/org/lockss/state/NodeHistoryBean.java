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

package org.lockss.state;

import java.util.*;

import org.lockss.util.LockssSerializable;

/**
 * NodeHistoryBean allows the marshalling of a group of PollHistoryBeans.
 */
public class NodeHistoryBean implements LockssSerializable {
  public List historyBeans;

  /**
   * Empty constructor for bean creation during unmarshalling.
   */
  public NodeHistoryBean() {
  }

  /**
   * Gets the collection of PollHistoryBeans.
   * @return a Collection of PollHistoryBeans
   */
  public List getHistoryBeans() {
    return historyBeans;
  }

  /**
   * Sets the collection of PollHistoryBeans
   * @param historyBeans a Collection of PollHistoryBeans
   */
  public void setHistoryBeans(List historyBeans) {
    this.historyBeans = historyBeans;
  }

  public static List fromListToBeanList(List thePollHistories) {
    if (thePollHistories==null) {
      return Collections.EMPTY_LIST;
    }
    else {
      List histBeans = new ArrayList(thePollHistories.size());
      Iterator histIter = thePollHistories.iterator();
      while (histIter.hasNext()) {
        PollHistory history = (PollHistory)histIter.next();
        histBeans.add(new PollHistoryBean(history)); // convert to a bean
      }
      return histBeans;
    }
  }

  public static List fromBeanListToList(List thePollHistoryBeans) {
    // create new list
    if (thePollHistoryBeans == null) {
      thePollHistoryBeans = new ArrayList();
    }
    List pollHistories = new ArrayList(thePollHistoryBeans.size());
    Iterator beanIter = thePollHistoryBeans.iterator();
    while (beanIter.hasNext()) {
      PollHistoryBean bean = (PollHistoryBean)beanIter.next();
      pollHistories.add(bean.getPollHistory()); // get PollHistory from bean
    }
    return pollHistories;
  }

}
