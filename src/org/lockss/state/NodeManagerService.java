/*
 * $Id: NodeManagerService.java,v 1.1 2003-03-01 01:57:56 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.state;

import java.util.*;
import org.lockss.app.*;
import org.lockss.daemon.Configuration;
import org.lockss.plugin.ArchivalUnit;

/**
 * Implementation of the NodeManager.
 */
public class NodeManagerService implements LockssManager {
  private static LockssDaemon theDaemon;
  private static LockssManager theManager = null;
  private static HashMap auMaps = new HashMap();

  public NodeManagerService() { }

  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see org.lockss.app.LockssManager#initService(LockssDaemon daemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if (theManager == null) {
      theDaemon = daemon;
      theManager = this;
    } else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
      public void configurationChanged(Configuration oldConfig,
                                       Configuration newConfig,
                                       Set changedKeys) {
        setConfig(newConfig, oldConfig);
      }
    });

    startAllManagers();
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // checkpoint here
    stopAllManagers();
    theManager = null;
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
  }

  private void startAllManagers() {
    Iterator auIt = theDaemon.getPluginManager().getAllAUs().iterator();
    while (auIt.hasNext()) {
      ArchivalUnit au = (ArchivalUnit)auIt.next();
      NodeManager manager = managerFactory(au);
      manager.initService(theDaemon);
      manager.startService();
    }
  }

  private void stopAllManagers() {
    Iterator entries = auMaps.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      NodeManager manager = (NodeManager)entry.getValue();
      manager.stopService();
    }
  }

  /**
   * Factory method to retrieve NodeManager.
   * @param au the ArchivalUnit being managed
   * @return the current NodeManager
   */
  public synchronized NodeManager managerFactory(ArchivalUnit au) {
    NodeManager nodeManager = (NodeManager)auMaps.get(au);
    if (nodeManager==null) {
      nodeManager = new NodeManagerImpl(au);
      auMaps.put(au, nodeManager);
    }
    return nodeManager;
  }
}
