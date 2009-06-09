/*
 * $Id: AlertFilter.java,v 1.4 2009-06-09 06:11:53 tlipkis Exp $
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

import org.lockss.util.*;

/** AlertFilter specifies a pattern and an action to be taken for all
 * alerts that match the pattern */
public class AlertFilter implements LockssSerializable {
  private AlertPattern pattern;
  private AlertAction action;

  public AlertFilter() {
  }

  public AlertFilter(AlertPattern pattern, AlertAction action) {
    this.pattern = pattern;
    this.action = action;
  }

  public AlertPattern getPattern() {
    return pattern;
  }

  public void setPattern(AlertPattern pattern) {
    this.pattern = pattern;
  }

  public AlertAction getAction() {
    return action;
  }

  public void setAction(AlertAction action) {
    this.action = action;
  }

//   public boolean equals(Object obj) {
//     if (obj instanceof AlertFilter) {
//       AlertFilter filt = (AlertFilter)obj;
//       return pattern.equals(filt.getPattern()) &&
// 	action.equals(filt.getAction());
//     }
//     return false;
//   }

  public boolean equals(Object o) {
    if (o instanceof AlertFilter) {
      AlertFilter filt = (AlertFilter)o;
      return pattern.equals(filt.pattern)
	&& action.equals(filt.action);
    }
    return false;
  }
  public String toString() {
    return "[AlertFilter: " + pattern + "," + action + "]";
  }
}
