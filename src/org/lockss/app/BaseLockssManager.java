/*
 * $Id: BaseLockssManager.java,v 1.22 2009-06-01 07:45:40 tlipkis Exp $
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

package org.lockss.app;

import org.lockss.config.*;
import org.lockss.util.*;

/**
 * Base implementation of LockssManager
 */

public abstract class BaseLockssManager implements LockssManager {

  private static Logger log = Logger.getLogger("BaseLockssManager");

  protected LockssApp theApp = null;
  private Configuration.Callback configCallback;
  protected boolean isInited = false;
  protected boolean shuttingDown = false;

  protected String getClassName() {
    return ClassUtil.getClassNameWithoutPackage(getClass());
  }

  /**
   * Called to initialize each service in turn.  Service should extend
   * this to perform any internal initialization necessary before service
   * can be called from outside.  No calls to other services may be made in
   * this method.
   * @param app the {@link LockssApp}
   * @throws LockssAppException
   */
  public void initService(LockssApp app) throws LockssAppException {
    if (log.isDebug2()) log.debug2(getClassName() + ".initService()");
    if(theApp == null) {
      theApp = app;
      registerDefaultConfigCallback();
    }
    else {
      throw new LockssAppException("Multiple Instantiation.");
    }
  }

  /** Called to start each service in turn, after all services have been
   * initialized.  Service should extend this to perform any startup
   * necessary. */
  public void startService() {
    isInited = true;
    if (log.isDebug2()) log.debug2(getClassName() + ".startService()");
  }

  /** Called to stop a service.  Service should extend this to stop all
   * ongoing activity (<i>eg</i>, threads). */
  public void stopService() {
    shuttingDown = true;
    // checkpoint here
    unregisterConfig();
    // Logically, we should set theApp = null here, but that breaks several
    // tests, which sometimes stop managers twice.
//     theApp = null;
  }

  /** Return the app instance in which this manager is running */
  public LockssApp getApp() {
    return theApp;
  }

  /** Return true if the manager is shutting down */
  public boolean isShuttingDown() {
    return shuttingDown;
  }

  /**
   * Return true iff all the app services have been initialized.
   * @return true if the app is inited
   */
  protected boolean isAppInited() {
    return theApp.isAppInited();
  }

  /**
   * Return true iff this manager's init has completed.  This can differ
   * from isAppInited in some testing situations where additional
   * managers are started after the daemon is running
   * @return true if the manager is inited
   */
  protected boolean isInited() {
    return isInited;
  }

  private void registerConfigCallback(Configuration.Callback callback) {
    if(callback == null || this.configCallback != null) {
      throw new LockssAppException("Invalid callback registration: "
				       + callback);
    }
    configCallback = callback;

    theApp.getConfigManager().registerConfigurationCallback(configCallback);
  }

  private void registerDefaultConfigCallback() {
    if (this instanceof ConfigurableManager) {
      Configuration.Callback cb =
	new DefaultConfigCallback((ConfigurableManager)this);
      registerConfigCallback(cb);
    }
  }

  private void unregisterConfig() {
    if(configCallback != null) {
      theApp.getConfigManager().unregisterConfigurationCallback(configCallback);
      configCallback = null;
    }
  }

  /** Convenience method to (re)invoke the manager's setConfig(new, old,
   * ...) method with the current config and empty previous config. */
  protected void resetConfig() {
    if (this instanceof ConfigurableManager) {
      ConfigurableManager cmgr = (ConfigurableManager)this;
      Configuration cur = CurrentConfig.getCurrentConfig();
      cmgr.setConfig(cur, ConfigManager.EMPTY_CONFIGURATION,
		     Configuration.DIFFERENCES_ALL);
    } else {
      throw new RuntimeException("Not a ConfigurableManager");
    }
  }

  private static class DefaultConfigCallback
    implements Configuration.Callback {

    ConfigurableManager mgr;
    DefaultConfigCallback(ConfigurableManager mgr) {
      this.mgr = mgr;
    }

    public void configurationChanged(Configuration newConfig,
				     Configuration prevConfig,
				     Configuration.Differences changedKeys) {
      mgr.setConfig(newConfig, prevConfig, changedKeys);
    }
  }
}
