/*
 * $Id$
 *

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

import org.lockss.app.*;
import org.lockss.config.Configuration;

/** AlertManager handles alert conditions, sending mail and maintaining a
 * log.
 */
public interface AlertManager extends LockssManager {

  static final String PREFIX = Configuration.PREFIX + "alert.";

  static final String PARAM_ALERTS_ENABLED = PREFIX + "enabled";
  static final boolean DEFAULT_ALERTS_ENABLED = true;

  /**
   * Raise an alert
   * @param alert the alert to raise
   */
  public void raiseAlert(Alert alert);

  /**
   * Convenience method to set the text of an alert and raise() it
   * @param alert the alert to raise
   * @param text text to be stored in text attribute of alert
   */
  public void raiseAlert(Alert alert, String text);

  public AlertConfig getConfig();

  public void updateConfig(AlertConfig config) throws Exception;
}
