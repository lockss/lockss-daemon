/*
 * $Id: NodeManagerServiceImpl.java,v 1.2 2003-03-01 03:21:30 aalto Exp $
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
import org.lockss.util.Logger;

/**
 * Implementation of the NodeManagerService.
 */
public class NodeManagerServiceImpl implements NodeManagerService {
  private static LockssDaemon theDaemon;
  private static LockssManager theManager = null;
  private static HashMap auMaps = new HashMap();
  private static Logger logger = Logger.getLogger("NodeManagerService");

  public NodeManagerServiceImpl() { }

  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if (theManager == null) {
      theDaemon = daemon;
      theManager = this;
    } else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  public void startService() {
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
      public void configurationChanged(Configuration oldConfig,
                                       Configuration newConfig,
                                       Set changedKeys) {
        setConfig(newConfig, oldConfig);
      }
    });
  }

  public void stopService() {
    // checkpoint here
    stopAllManagers();
    theManager = null;
  }

  private void setConfig(Configuration config, Configuration oldConfig) {
  }


  private void stopAllManagers() {
    Iterator entries = auMaps.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      NodeManager manager = (NodeManager)entry.getValue();
      manager.stopService();
    }
  }

  public NodeManager getNodeManager(ArchivalUnit au) {
    NodeManager nodeMan = (NodeManager)auMaps.get(au);
    if (nodeMan==null) {
      logger.error("NodeManager not created for au: "+au);
      throw new IllegalArgumentException("NodeManager not created for au.");
    }
    return nodeMan;
  }

  public synchronized void addNodeManager(ArchivalUnit au) {
    NodeManager nodeManager = (NodeManager)auMaps.get(au);
    if (nodeManager==null) {
      nodeManager = new NodeManagerImpl(au);
      auMaps.put(au, nodeManager);
      nodeManager.initService(theDaemon);
      nodeManager.startService();
    }
  }
}
