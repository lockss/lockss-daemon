/*
 * $Id: Configuration.java,v 1.2 2002-09-02 04:22:44 tal Exp $
 */

/*

Copyright (c) 2001-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;
import java.io.*;
import java.net.*;
import java.text.*;
import org.mortbay.tools.*;
import org.lockss.util.*;

/** <code>Configuration</code> provides access to the LOCKSS configuration
 * parameters.  Instances of (concrete subclasses of)
 * <code>Configuration</code> hold a set of configuration parameters, and
 * have a standard set of accessors.  Static methods on this class provide
 * convenient access to parameter values in the "current" configuration;
 * these accessors all have <code>Param</code> in their name.  (If called
 * on a <code>Configuration</code> <i>instance</i>, they will return values
 * from the current configuration, not that instance.  So don't do that.)
 * */
public abstract class Configuration {
  public static final String PREFIX = "org.lockss.";

  // MUST pass in explicit log level to avoid recursive call back to
  // Configuration to get Config log level.
  protected static Logger log = Logger.getLogger("Config", Logger.LEVEL_INFO);

  private static List configChangedCallbacks = new ArrayList();

  private static String configUrls;	// <sp> separated string of urls
  private static List configUrlList;	// list of urls

  // Current configuration instance.
  // Start with an empty one to avoid errors in the static accessors.
  private static Configuration currentConfig = newConfiguration();

  private static HandlerThread handlerThread; // reload handler thread

  // Factory to create instance of appropriate class
  static Configuration newConfiguration() {
    return new ConfigurationPropTreeImpl();
  }

  /** Return current configuration */
  public static Configuration getCurrentConfig() {
    return currentConfig;
  }

  static void setCurrentConfig(Configuration newConfig) {
    if (newConfig == null) {
      log.warning("attempt to install null Configuration");
    }
    currentConfig = newConfig;
  }

  static void runCallbacks(Configuration oldConfig,
			   Configuration newConfig) {
    for (Iterator iter = configChangedCallbacks.iterator();
	 iter.hasNext();) {
      ConfigurationCallback c = (ConfigurationCallback)iter.next();
      c.configurationChanged(oldConfig, newConfig,
			     newConfig.differentKeys(oldConfig));
    }
  }

  void setConfigUrls(String urls) {
    configUrls = urls;
    configUrlList = new ArrayList();
    for (StringTokenizer st = new StringTokenizer(configUrls);
	 st.hasMoreElements(); ) {
      String url = st.nextToken();
      configUrlList.add(url);
    }
  }

  /**
   * Return a new <code>Configuration</code> instance loaded from the
   * url list
   */
  static Configuration readConfig(List urlList) {
    if (urlList == null) {
      return null;
    }
    Configuration newConfig = newConfiguration();
    boolean gotIt = newConfig.loadList(urlList);
    return gotIt ? newConfig : null;
  }

  static boolean updateConfig() {
    Configuration newConfig = readConfig(configUrlList);
    return installConfig(newConfig);
  }

  static boolean installConfig(Configuration newConfig) {
    if (newConfig == null) {
      return false;
    }
    Configuration oldConfig = currentConfig;
    if (newConfig.equals(oldConfig)) {
      log.info("Config unchanged, not updated");
      return false;
    }
    setCurrentConfig(newConfig);
    log.info("Config updated");
    runCallbacks(oldConfig, newConfig);
    return true;
  }

  /**
   * Register a <code>ConfigurationCallback</code>, which will be
   * called whenever the current configuration has changed.
   * @param c <code>ConfigurationCallback</code> to add.
   */
  public static void
    registerConfigurationCallback(ConfigurationCallback c) {
    if (!configChangedCallbacks.contains(c)) {
      configChangedCallbacks.add(c);
    }
  }
      
  /**
   * Unregister a <code>ConfigurationCallback</code>.
   * @param c <code>ConfigurationCallback</code> to remove.
   */
  public static void
    unregisterConfigurationCallback(ConfigurationCallback c) {
    configChangedCallbacks.remove(c);
  }

  // instance methods

  /**
   * Try to load config from a list or urls
   * @return true iff properties were successfully loaded
   */
  boolean loadList(List urls) {
    for (Iterator iter = urls.iterator(); iter.hasNext();) {
      String url = (String)iter.next();
      try {
	load(url);
      } catch (IOException e) {
	// This load failed.  Fail the whole thing.
	log.warning("Couldn't load props from " + url, e);
	reset();			// ensure config is empty
	return false;
      }
    }
    return true;
  }

  void load(String url) throws IOException {
    InputStream istr = UrlUtil.openInputStream(url);
    load(new BufferedInputStream(istr));
  }

  abstract boolean load(InputStream istr)
      throws IOException;

  abstract Set differentKeys(Configuration otherConfig);

  public String get(String key, String dfault) {
    String val = get(key);
    if (val == null) {
      val = dfault;
    }
    return val;
  }

  // must be implemented by implementation subclass

  public abstract void reset();

  public abstract boolean equals(Object c);

  public abstract String get(String key);

  public abstract boolean getBoolean(String key);

  public abstract boolean getBoolean(String key, boolean dFault);

  public abstract Configuration getConfigTree(String key);

  public abstract Iterator keyIterator();

  public abstract Iterator nodeIterator();

  public abstract Iterator nodeIterator(String key);

  // static convenience methods

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static String getParam(String key) {
    return currentConfig.get(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static String getParam(String key, String dfault) {
    return currentConfig.get(key, dfault);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static boolean getBooleanParam(String key) {
    return currentConfig.getBoolean(key);
  }

  /** Static convenience method to get param from current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static boolean getBooleanParam(String key, boolean dfault) {
    return currentConfig.getBoolean(key, dfault);
  }

  /** Static convenience method to get a <code>Configuration</code>
   * subtree from the current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static Configuration paramConfigTree(String key) {
    return currentConfig.getConfigTree(key);
  }

  /** Static convenience method to get key iterator from the
   * current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static Iterator paramKeyIterator() {
    return currentConfig.keyIterator();
  }

  /** Static convenience method to get a node iterator from the
   * current configuration.
   * Don't accidentally use this on a <code>Configuration</code> instance.
   */
  public static Iterator paramNodeIterator(String key) {
    return currentConfig.nodeIterator(key);
  }

  static void startHandler() {
    if (handlerThread != null) {
      log.warning("Handler's already running; stopping old one first");
      stopHandler();
    } else {
      log.info("Starting handler");
    }
    handlerThread = new HandlerThread("ConfigHandler");
    handlerThread.start();
  }

  public static void stopHandler() {
    if (handlerThread != null) {
      log.info("Stopping handler");
      handlerThread.stopHandler();
      handlerThread = null;
    } else {
      log.warning("Attempt to stop handler when it isn't running");
    }
  }

  // Handler thread, periodicially reloads config

  private static class HandlerThread extends Thread {
    private long lastReload = 0;
    private boolean goOn = false;

    private HandlerThread(String name) {
      super(name);
    }

    public void run() {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      long reloadInterval = 600000;
      goOn = true;

      // repeat every 10ish minutes until first successful load, then
      // according to org.lockss.parameterReloadInterval, or 30 minutes.
      while (goOn) {
	if (updateConfig()) {
	  // true iff loaded config has changed
	  if (!goOn) {
	    break;
	  }
	  lastReload = System.currentTimeMillis();
	  //  	stopAndOrStartThings(true);
	  reloadInterval = Integer.getInteger(Configuration.PREFIX +
					      "parameterReloadInterval",
					      1800000).longValue();
	}
	ProbabilisticTimer nextReload =
	  new ProbabilisticTimer(reloadInterval, reloadInterval/4);
	if (goOn) {
	  nextReload.sleepUntil();
	}
      }
    }

    private void stopHandler() {
      goOn = false;
      this.interrupt();
    }
  }
}
