/*
 * $Id: AlertConfig.java,v 1.4.68.1 2009-11-03 23:44:51 edwardsb1 Exp $
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

import org.lockss.util.LockssSerializable;

/** AlertConfig stores the state of the alert notification config */
public class AlertConfig implements LockssSerializable {
  protected List<AlertFilter> filters;

  /**
   * Simple constructor to allow bean creation during unmarshalling.
   */
  public AlertConfig() {
    filters = new ArrayList<AlertFilter>();
  }

  public AlertConfig(List<AlertFilter> filters) {
    this.filters = filters;
  }

  public AlertConfig(AlertConfig config) {
    filters = config.getFilters();
  }

  public List<AlertFilter> getFilters() {
    if (filters == null) {
      return Collections.EMPTY_LIST;
    }
    return filters;
  }

  /**
   * Sets the filters
   * @param filters list of {@link AlertFilter}s
   */
  public void setFilters(List<AlertFilter> filters) {
    this.filters = filters;
  }

  public void addFilter(AlertFilter filter) {
    this.filters.add(filter);
  }

  public boolean equals(Object obj) {
    if (obj instanceof AlertConfig) {
      AlertConfig config = (AlertConfig)obj;
      return getFilters().equals(config.getFilters());
    }
    return false;
  }

  public int hashCode() {
    if (filters == null) {
      return 0;
    }
    return filters.hashCode();
  }

  public String toString() {
    return "{AlertConfig: " + filters + "]";
  }
}
