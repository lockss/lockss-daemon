/*
 * $Id: NodeManagerServiceImpl.java,v 1.7 2003-04-05 04:03:42 claire Exp $
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
import org.lockss.daemon.status.*;

/**
 * Implementation of the NodeManagerService.
 */
public class NodeManagerServiceImpl extends BaseLockssManager
    implements NodeManagerService {
  private HashMap auMap = new HashMap();
  private static Logger logger = Logger.getLogger("NodeManagerService");

  public NodeManagerServiceImpl() { }


  protected void setConfig(Configuration config, Configuration oldConfig,
			   Set changedKeys) {
  }

  public void startService() {
    super.startService();
    // register our status
    StatusService statusServ = theDaemon.getStatusService();
    NodeManagerStatus nmStatus = new NodeManagerStatus(this);

    statusServ.registerStatusAccessor(NodeManagerStatus.SERVICE_STATUS_TABLE_NAME,
                                      new NodeManagerStatus.ServiceStatus());
    statusServ.registerStatusAccessor(NodeManagerStatus.MANAGER_STATUS_TABLE_NAME,
                                      new NodeManagerStatus.ManagerStatus());

    statusServ.registerStatusAccessor(NodeManagerStatus.POLLHISTORY_STATUS_TABLE_NAME,
                                      new NodeManagerStatus.PollHistoryStatus());
  }

  public void stopService() {
    // checkpoint here
    // unregister our status accessors
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.unregisterStatusAccessor(NodeManagerStatus.SERVICE_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(NodeManagerStatus.MANAGER_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(NodeManagerStatus.POLLHISTORY_STATUS_TABLE_NAME);

    stopAllManagers();
    super.stopService();
  }

  private void stopAllManagers() {
    Iterator entries = auMap.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry)entries.next();
      NodeManager manager = (NodeManager)entry.getValue();
      manager.stopService();
    }
  }

  public NodeManager getNodeManager(ArchivalUnit au) {
    NodeManager nodeMan = (NodeManager)auMap.get(au);
    if (nodeMan==null) {
      logger.error("NodeManager not found for au: "+au);
      throw new IllegalArgumentException("NodeManager not found for au.");
    }
    return nodeMan;
  }

  public synchronized void addNodeManager(ArchivalUnit au) {
    NodeManager nodeManager = (NodeManager)auMap.get(au);
    if (nodeManager==null) {
      nodeManager = new NodeManagerImpl(au);
      auMap.put(au, nodeManager);
      nodeManager.initService(theDaemon);
      nodeManager.startService();
    }
  }

  // support for status

  Iterator getEntries() {
    return auMap.entrySet().iterator();
  }
}
