/*
 * $Id: BaseLockssManager.java,v 1.18 2005-12-01 23:28:01 troberts Exp $
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

  private LockssManager theManager = null;
  protected LockssApp theApp = null;
  private Configuration.Callback configCallback;
  private String className = ClassUtil.getClassNameWithoutPackage(getClass());
  private static Logger log = Logger.getLogger("BaseLockssManager");

  /**
   * Called to initialize each service in turn.  Service should extend
   * this to perform any internal initialization necessary before service
   * can be called from outside.  No calls to other services may be made in
   * this method.
   * @param app the {@link LockssApp}
   * @throws LockssAppException
   */
  public void initService(LockssApp app) throws LockssAppException {
    if (log.isDebug2()) log.debug2(className + ".initService()");
    if(theManager == null) {
      theApp = app;
      theManager = this;
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
    if (log.isDebug2()) log.debug2(className + ".startService()");
  }

  /** Called to stop a service.  Service should extend this to stop all
   * ongoing activity (<i>eg</i>, threads). */
  public void stopService() {
    // checkpoint here
    unregisterConfig();
    theManager = null;
  }

  /** Return the app instance in which this manager is running */
  public LockssApp getApp() {
    return theApp;
  }

  /**
   * Return true iff all the app services have been initialized.
   * @return true if the app is inited
   */
  protected boolean isAppInited() {
    return theApp.isAppInited();
  }

  private void registerConfigCallback(Configuration.Callback callback) {
    if(callback == null || this.configCallback != null) {
      throw new LockssAppException("Invalid callback registration: "
				       + callback);
    }
    configCallback = callback;
    Configuration.registerConfigurationCallback(configCallback);
  }

  private void registerDefaultConfigCallback() {
    if (this instanceof ConfigurableManager) {
      configCallback = new DefaultConfigCallback((ConfigurableManager)this);
      Configuration.registerConfigurationCallback(configCallback);
    }
  }

  private void unregisterConfig() {
    if(configCallback != null) {
      Configuration.unregisterConfigurationCallback(configCallback);
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
