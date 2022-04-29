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

package org.lockss.jetty;

import org.mortbay.http.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.ResourceManager;
import org.lockss.util.*;

/**
 * Abstract base class for LOCKSS managers that use/start Jetty services.
 * Note: this class may be used in an environment where the LOCKSS app is
 * not running (<i>e.g.</i>, for {@link org.lockss.servlet.TinyUi}), so it
 * must not rely on any non-static app services, nor any other managers.
 */
public abstract class JettyManager
  extends BaseLockssManager implements ConfigurableManager {
  static final String PREFIX = Configuration.PREFIX + "jetty.";
  static final String DEBUG_PREFIX = Configuration.PREFIX + "jetty.debug";

  public static final String PARAM_NAMED_SERVER_PRIORITY =
    PREFIX + "<name>.priority";

  /** The maximum form data size that Jetty will accept.  Daemon restart
   * required for changes to take effect. */
  public static final String PARAM_MAX_FORM_SIZE =
    PREFIX + "MaxFormSize";

  public static final int DEFAULT_MAX_FORM_SIZE = 2000000;

  private String prioParam;

  private static Logger log = Logger.getLogger("JettyMgr");
  protected static int maxFormSize = -1;
  private static boolean isJettyInited = false;

  protected ResourceManager resourceMgr;
  // Used as token in resource reservations, and in messages
  protected String serverName;
  protected HttpServer runningServer;
  protected int ownedPort = -1;
  protected int ownedSslPort = -1;

  public JettyManager() {
  }

  public JettyManager(String serverName) {
    this.serverName = serverName;
    prioParam = StringUtil.replaceString(PARAM_NAMED_SERVER_PRIORITY,
					 "<name>", serverName);
  }

  protected String getServerName() {
    return serverName;
  }

  /**
   * Start the manager.  Note: not called by TinyUI.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    resourceMgr = getApp().getResourceManager();
    oneTimeJettySetup();
    resetConfig();			// run setConfig() unconditionally
					// to set defaults in subclasses
  }

  // synchronized on class
  private static synchronized void oneTimeJettySetup() {
    if (!isJettyInited) {
      // Tell Jetty to allow symbolic links in file resources
      System.setProperty("org.mortbay.util.FileResource.checkAliases",
			 "false");
      String maxform = CurrentConfig.getParam(PARAM_MAX_FORM_SIZE,
					      DEFAULT_MAX_FORM_SIZE + "");
      System.setProperty("org.mortbay.http.HttpRequest.maxFormContentSize",
			 maxform);
      maxFormSize = CurrentConfig.getIntParam(PARAM_MAX_FORM_SIZE,
					      DEFAULT_MAX_FORM_SIZE);
      // Jetty grabs System property in static initializer.  Ensure no
      // loading order dependence
      try {
	int max = Integer.parseInt(maxform);
	HttpRequest.__maxFormContentSize = max;
      } catch (NumberFormatException e) {
	log.error("Can't set max form size: " + e.toString());
      }

      // Override some default error status strings with messages that are
      // more user-friendly
      HttpResponse.__statusMsg.put(new Integer(HttpResponse.__401_Unauthorized),
				   "Invalid Username or Password");
      HttpResponse.__statusMsg.put(new Integer(HttpResponse.__502_Bad_Gateway),
				   "Upstream Error");
      isJettyInited = true;
    }
  }

  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    if (runningServer != null && changedKeys.contains(prioParam)) {
      setListenerParams(runningServer);
    }
  }

  public int getMaxFormSize() {
    return maxFormSize;
  }

  long[] delayTime = {10 * Constants.SECOND, 60 * Constants.SECOND, 0};

  protected boolean startServer(HttpServer server, int port) {
    return startServer(server, port, 0);
  }

  protected boolean startServer(HttpServer server, int port, int sslPort) {
    return startServer(server, port, sslPort, getServerName());
  }

  protected boolean startServer(HttpServer server, int port, int sslPort,
				String serverName) {
    try {
      if (!isInited()) return false;
      if (resourceMgr != null) {
	if (!resourceMgr.reserveTcpPort(port, serverName)) {
	  log.warning(serverName + " not started; port " + port + " is in use");
	  return false;
	}
	ownedPort = port;
	if (sslPort > 0) {
	  if (!resourceMgr.reserveTcpPort(sslPort, serverName)) {
	    // Release regular port if failed
	    resourceMgr.releaseTcpPort(port, serverName);
	    log.warning(serverName + " not started; SSL port " + port +
			" is in use");
	    return false;
	  }
	}
	ownedSslPort = sslPort;
      }
      setListenerParams(server);
      for (int ix = 0; ix < delayTime.length; ix++) {
	try {
	  server.start();
	  runningServer = server;
	  return true;
	} catch (org.mortbay.util.MultiException e) {
	  log.debug("multi", e);
	  log.debug2("first", e.getException(0));
	  if (delayTime[ix] > 0) {
	    log.warning("Addr in use, sleeping " +
			StringUtil.timeIntervalToString(delayTime[ix]));
	    Deadline.in(delayTime[ix]).sleep();
	  }
	}
      }
    } catch (Exception e) {
      log.warning("Couldn't start servlets", e);
    }
    releasePort(serverName);
    ownedPort = -1;
    return false;
  }


  /** Set the priority at which all requests will be handled */
  private void setListenerParams(HttpServer server) {
    String name = getServerName();
    int prio = getPriorityFromParam(name);
    log.debug("Setting priority of " + name + " listener to " + prio);
    HttpListener listeners[] = server.getListeners();
    for (int ix = 0; ix < listeners.length; ix++) {
      if (listeners[ix] instanceof org.mortbay.util.ThreadPool) {
	org.mortbay.util.ThreadPool tpool =
	  (org.mortbay.util.ThreadPool)listeners[ix];
	if (prio != -1) {
	  tpool.setThreadsPriority(prio);
	}
	// Set the name for threads in the pool, to id them in thread
	// dumps.  I think this is suppoesd to be done with
	// ThreadPool.setName(), as setPoolName() affects more stuff, but
	// this is what's necessary in Jetty 4.2.17.
	if (!tpool.isStarted()) {	// can't change name after started
	  tpool.setName(name);
	}
      }
    }
  }

  int getPriorityFromParam(String name) {
    if (prioParam == null) {
      prioParam = StringUtil.replaceString(PARAM_NAMED_SERVER_PRIORITY,
					   "<name>", name);
    }
    return CurrentConfig.getIntParam(prioParam, -1);
  }

  private void releasePort(String serverName) {
    if (resourceMgr != null) {
      resourceMgr.releaseTcpPort(ownedPort, serverName);
      ownedPort = -1;
      if (ownedSslPort > 0) {
	resourceMgr.releaseTcpPort(ownedSslPort, serverName);
	ownedSslPort = -1;
      }
    }
  }

  protected void stopServer() {
    stopServer(getServerName());
  }

  protected void stopServer(String serverName) {
    try {
      if (runningServer != null) {
	runningServer.stop();
	runningServer = null;
	releasePort(serverName);
      }
    } catch (InterruptedException e) {
      log.warning("Interrupted while stopping server");
    }
  }

  public boolean isServerRunning() {
    return runningServer != null;
  }

  public boolean isRunningOnPort(int port) {
    return ownedPort == port && isServerRunning();
  }

}
