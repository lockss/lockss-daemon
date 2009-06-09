/*
 * $Id: AlertAction.java,v 1.3.68.1 2009-06-09 05:47:47 tlipkis Exp $
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

import org.lockss.config.Configuration;
import org.lockss.app.*;

/** AlertAction is the interface for notifying something or someone of an
 * Alert (e.g., logging, mailing). */
public interface AlertAction {
  static final String PREFIX = Configuration.PREFIX + "alert.action.";

  /** Record a single Alert */
  public void record(LockssDaemon daemon, Alert alert);

  /** Record a list of similar Alerts */
  public void record(LockssDaemon daemon, List alerts);

  /** Return true if this action can advantageously group similar Alerts */
  public boolean isGroupable();

  /** Return the maximum time an alert should remain pending before it it
   * reported in a group */
  public long getMaxPendTime();

  /** Implementations must implement a suitable equals() and hasCode() in
   * order for grouped reporting to work */
  public boolean equals(Object obj);

  /** Implementations must implement a suitable equals() and hasCode() in
   * order for grouped reporting to work */
  public int hashCode();
}
