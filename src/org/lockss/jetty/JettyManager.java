/*
 * $Id: JettyManager.java,v 1.16 2004-09-27 22:39:13 smorabito Exp $
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
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.mortbay.http.*;
import org.mortbay.util.Code;

/**
 * Abstract base class for LOCKSS managers that use/start Jetty services.
 * Note: this class may be used in an environment where the LOCKSS app is
 * not running (<i>e.g.</i>, for {@link org.lockss.servlet.TinyUi}), so it
 * must not rely on any non-static app services, nor any other managers.
 */
public abstract class JettyManager
  extends BaseLockssManager implements ConfigurableManager {
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

  protected HttpServer runningServer;
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
  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
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

  long[] delayTime = {10 * Constants.SECOND, 60 * Constants.SECOND, 0};

  protected boolean startServer(HttpServer server, int port) {
    try {
      for (int ix = 0; ix < delayTime.length; ix++) {
	try {
	  server.start();
	  runningServer = server;
	  runningOnPort(port);
	  return true;
	} catch (org.mortbay.util.MultiException e) {
	  log.debug("multi", e);
	  log.debug2("first", e.getException(0));
	  log.warning("Addr in use, sleeping " +
		      StringUtil.timeIntervalToString(delayTime[ix]));
	  Deadline.in(delayTime[ix]).sleep();
	}
      }
    } catch (Exception e) {
      log.warning("Couldn't start servlets", e);
    }
    return false;
  }

  public void stopServer() {
    try {
      if (runningServer != null) {
	runningOnPort(-1);
	runningServer.stop();
	runningServer = null;
      }
    } catch (InterruptedException e) {
      log.warning("Interrupted while stopping server");
    }
  }

  protected void runningOnPort(int port) {
    if (log.isDebug2()) {
      log.debug2("runningOnPort(" + port + "), in use: " + portsInUse);
    }
    if (runningOnPort > 0) {
      portsInUse.remove(new Integer(runningOnPort));
      runningOnPort = -1;
    }
    if (port > 0) {
      portsInUse.add(new Integer(port));
      runningOnPort = port;
    }
  }

  protected boolean isServerRunning() {
    return (runningOnPort > 0);
  }

  public static boolean isPortInUse(int port) {
    if (log.isDebug2()) {
      log.debug2("portsInUse(" + port + ") = " +
		 portsInUse.contains(new Integer(port)));
    }
    return portsInUse.contains(new Integer(port));
  }
}
