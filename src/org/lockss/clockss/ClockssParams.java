/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.clockss;

import java.net.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.util.*;

/**
 * Centralized place for CLOCKSS parameters
 */

public class ClockssParams
  extends BaseLockssDaemonManager
  implements ConfigurableManager {

  static Logger log = Logger.getLogger("ClockssParams");

  public static final String PREFIX = Configuration.PREFIX + "clockss.";

  /** Enable/disable CLOCKSS subscription detection */
  public static final String PARAM_ENABLE_CLOCKSS_SUBSCRIPTION_DETECTION =
    PREFIX + "detectSubscription";

  public static final boolean DEFAULT_ENABLE_CLOCKSS_SUBSCRIPTION_DETECTION =
    false;

  /** Second IP address, for CLOCKSS subscription detection */
  public static final String PARAM_CLOCKSS_SUBSCRIPTION_ADDR =
    PREFIX + "clockssAddr";

  /** Second IP address, for CLOCKSS subscription detection */
  public static final String PARAM_INSTITUTION_SUBSCRIPTION_ADDR =
    PREFIX + "institutionAddr";

  private boolean isDetectSubscription =
    DEFAULT_ENABLE_CLOCKSS_SUBSCRIPTION_DETECTION;
  private IPAddr institutionSubscriptionAddr;
  private IPAddr clockssSubscriptionAddr;

  public void startService() {
    super.startService();
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      isDetectSubscription =
	config.getBoolean(PARAM_ENABLE_CLOCKSS_SUBSCRIPTION_DETECTION,
			  DEFAULT_ENABLE_CLOCKSS_SUBSCRIPTION_DETECTION);

      String inst = config.get(PARAM_INSTITUTION_SUBSCRIPTION_ADDR);
      if (inst == null) {
	institutionSubscriptionAddr = null;
      } else {
	try {
	  institutionSubscriptionAddr = IPAddr.getByName(inst);
	} catch (UnknownHostException e) {
	  log.warning("Couldn't parse institution subscription addr: " + inst);
	}
      }
      String sub = config.get(PARAM_CLOCKSS_SUBSCRIPTION_ADDR);
      if (sub == null) {
	clockssSubscriptionAddr = null;
      } else {
	try {
	  clockssSubscriptionAddr = IPAddr.getByName(sub);
	} catch (UnknownHostException e) {
	  log.warning("Couldn't parse CLOCKSS subscription addr: " + sub);
	}
      }
      log.debug("CLOCKSS institution addr: " + institutionSubscriptionAddr +
		", CLOCKSS addr: " + clockssSubscriptionAddr);
    }
  }

  public boolean isDetectSubscription() {
    return isDetectSubscription;
  }

  public IPAddr getInstitutionSubscriptionAddr() {
    return institutionSubscriptionAddr;
  }

  public IPAddr getClockssSubscriptionAddr() {
    return clockssSubscriptionAddr;
  }
}
