/*
 * $Id: AuSpecificManagerHandler.java,v 1.3 2003-09-04 23:11:17 tyronen Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import org.lockss.util.Logger;
import org.lockss.daemon.ActivityRegulator;
import org.lockss.plugin.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.state.NodeManagerImpl;

/**
 * This class organizes LockssManagers which are specific to the ArchivalUnit.
 * It stores these in a hashmap of hashmaps, with the first keyed on manager
 * type and the second on ArchivalUnit.
 *
 * @author Emil Aalto
 * @version 1.0
 */

public class AuSpecificManagerHandler {
  private static Logger log = Logger.getLogger("AuSpecificManagerHandler");
  private LockssDaemon theDaemon;

  // order is unimportant here.  This is a hashmap of hashmaps.
  protected static HashMap auSpecificManagers;

  protected AuSpecificManagerHandler(LockssDaemon theDaemon, int managerNum) {
    this.theDaemon = theDaemon;
    auSpecificManagers = new HashMap(managerNum);
  }

  /**
   * Starts any managers necessary to handle the ArchivalUnit.
   * @param au the ArchivalUnit
   */
  public void startAUManagers(ArchivalUnit au) {
    // this order can't be changed, as the other two use the ActivityRegulator
    // and the NodeManager uses the LockssRepository
    addAuSpecificManager(LockssDaemon.ACTIVITY_REGULATOR, au);
    addAuSpecificManager(LockssDaemon.LOCKSS_REPOSITORY, au);
    addAuSpecificManager(LockssDaemon.NODE_MANAGER, au);
  }

  protected LockssManager addAuSpecificManager(String managerKey,
                                               ArchivalUnit au) {
    HashMap managerMap = getAUSpecificManagerMap(managerKey);
    LockssManager manager = (LockssManager)managerMap.get(au);
    if (manager == null) {
      manager = getNewManager(managerKey, au);
      managerMap.put(au, manager);
      manager.initService(theDaemon);
      manager.startService();
    }
    return manager;
  }

  protected LockssManager getNewManager(String managerKey, ArchivalUnit au) {
    LockssManager manager;
    if (managerKey.equals(LockssDaemon.ACTIVITY_REGULATOR)) {
      log.debug2("Adding new activity regulator...");
      manager = ActivityRegulator.createNewActivityRegulator(au);
    } else if (managerKey.equals(LockssDaemon.LOCKSS_REPOSITORY)) {
      log.debug2("Adding new lockss repository...");
      manager = LockssRepositoryImpl.createNewLockssRepository(au);
    } else if (managerKey.equals(LockssDaemon.NODE_MANAGER)) {
      log.debug2("Adding new node manager...");
      manager = NodeManagerImpl.createNewNodeManager(au);
    } else {
      log.error("Unsupported au-specific manager: " + managerKey);
      throw new IllegalArgumentException("Unsupported au-specific manager: " +
                                         managerKey);
    }
    return manager;
  }

  /**
   * Get a LockssManager instance
   * @param managerKey the manager type
   * @param au the ArchivalUnit
   * @return the LockssManager
   */
  public LockssManager getAUSpecificManager(String managerKey, ArchivalUnit au) {
    return (LockssManager)getAUSpecificManagerMap(managerKey).get(au);
  }

  /**
   * The LockssManager entries for a particular type.
   * @param managerKey the manager type
   * @return an Iterator of Map.Entry objects
   */
  public Iterator getEntries(String managerKey) {
    return getAUSpecificManagerMap(managerKey).entrySet().iterator();
  }

  /**
   * Calls 'stopService()' on all managers, and clears the map.
   */
  public void stopAllManagers() {
    Iterator managerEntries = auSpecificManagers.entrySet().iterator();
    while (managerEntries.hasNext()) {
      Map.Entry entry = (Map.Entry)managerEntries.next();
      log.debug2("Stopping all " + entry.getKey() + " instances...");
      HashMap auMap = (HashMap)entry.getValue();
      Iterator values = auMap.values().iterator();
      while (values.hasNext()) {
        LockssManager manager = (LockssManager) values.next();
        manager.stopService();
      }
    }
    auSpecificManagers.clear();
  }

  protected HashMap getAUSpecificManagerMap(String managerKey) {
    HashMap map = (HashMap)auSpecificManagers.get(managerKey);
    if (map==null) {
      log.debug3("Initializing hashmap for "+managerKey);
      //map = new HashMap();
      // Use of the WrappedMap takes care of the case when wrapped AUs are used
      map = WrapperState.createWrappedMapIfAvailable();
      auSpecificManagers.put(managerKey, map);
    }
    return map;
  }

}
