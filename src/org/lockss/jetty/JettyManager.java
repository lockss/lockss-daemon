/*
 * $Id: JettyManager.java,v 1.9.4.1 2004-07-26 23:35:56 tlipkis Exp $
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

package org.lockss.jetty;

import java.util.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.mortbay.util.Code;

/**
 * Abstract base class for LOCKSS managers that use/start Jetty services
 */
public abstract class JettyManager extends BaseLockssManager {
  static final String PREFIX = Configuration.PREFIX + "jetty.debug";

  static final String PARAM_JETTY_DEBUG = PREFIX;
  static final boolean DEFAULT_JETTY_DEBUG = false;

  static final String PARAM_JETTY_DEBUG_PATTERNS = PREFIX + ".patterns";

  static final String PARAM_JETTY_DEBUG_VERBOSE = PREFIX + ".verbose";
  static final int DEFAULT_JETTY_DEBUG_VERBOSE = 0;
//   static final String PARAM_JETTY_DEBUG_OPTIONS = PREFIX + ".options";

  private static Logger log = Logger.getLogger("JettyMgr");
  private static boolean jettyLogInited = false;
  private static Set portsInUse = Collections.synchronizedSet(new HashSet());

  protected int runningOnPort = -1;

  public JettyManager() {
  }

  /**
   * start the manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    installJettyLog();
    resetConfig();
  }

  // synchronized on class
  private static synchronized void installJettyLog() {
    // install Jetty logger once only
    if (!jettyLogInited) {
      org.mortbay.util.Log.instance().add(new LoggerLogSink());
      jettyLogInited = true;
    }
  }

  // Set Jetty debug properties from config params
  protected void setConfig(Configuration config, Configuration prevConfig,
			   Set changedKeys) {
    if (jettyLogInited) {
      if (changedKeys.contains(PARAM_JETTY_DEBUG)) {
	boolean deb = config.getBoolean(PARAM_JETTY_DEBUG, 
					DEFAULT_JETTY_DEBUG);
	log.info("Turning Jetty DEBUG " + (deb ? "on." : "off."));
	Code.setDebug(deb);
      }
      if (changedKeys.contains(PARAM_JETTY_DEBUG_PATTERNS)) {
	String pat = config.get(PARAM_JETTY_DEBUG_PATTERNS);
	log.info("Setting Jetty debug patterns to: " + pat);
	Code.setDebugPatterns(pat);
      }
      if (changedKeys.contains(PARAM_JETTY_DEBUG_VERBOSE)) {
	int ver = config.getInt(PARAM_JETTY_DEBUG_VERBOSE, 
				DEFAULT_JETTY_DEBUG_VERBOSE);
	log.info("Setting Jetty verbosity to: " + ver);
	Code.setVerbose(ver);
      }
    }
  }

  protected void runningOnPort(int port) {
    if (port > 0) {
      portsInUse.add(new Integer(port));
      runningOnPort = port;
    } else {
      if (runningOnPort > 0) {
	portsInUse.remove(new Integer(runningOnPort));
	runningOnPort = -1;
      }
    }
  }

  public static boolean isPortInUse(int port) {
    return portsInUse.contains(new Integer(port));
  }
}
