/*
 * $Id: AlertConfig.java,v 1.2 2004-09-28 08:53:20 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.util.*;

/** AlertConfig stores the state of the alert notification config */
public class AlertConfig {
  protected List filters;

  /**
   * Simple constructor to allow bean creation during unmarshalling.
   */
  public AlertConfig() {
//     filters = new ArrayList();
    filters = new Vector();
  }

  public AlertConfig(List filters) {
    this.filters = filters;
  }

  public AlertConfig(AlertConfig config) {
    filters = config.getFilters();
  }

  public List getFilters() {
    if (filters == null) {
      return Collections.EMPTY_LIST;
    }
    return filters;
  }

  /**
   * Sets the filters
   * @param filters list of {@link AlertFilter}s
   */
  public void setFilters(List filters) {
    this.filters = filters;
  }

//   public void addFilters(AlertFilter filter) {
//     this.filters.add(filter);
//   }

//   public AlertFilter getFilter() {
//     return (AlertFilter)filters.get(0);
//   }

//   public void setFilter(AlertFilter filter) {
//     this.filters = ListUtil.list(filter);
//   }

  public boolean equals(Object obj) {
    if (obj instanceof AlertConfig) {
      AlertConfig config = (AlertConfig)obj;
      return getFilters().equals(config.getFilters());
    }
    return false;
  }

  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  public String toString() {
    return "{AlertConfig: " + filters + "]";
  }
}
