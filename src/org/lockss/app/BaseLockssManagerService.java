/*
 * $Id: BaseLockssManagerService.java,v 1.2 2003-06-20 22:34:50 claire Exp $
 */

/*

Copyright (c) 2001-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.plugin.ArchivalUnit;

public abstract class BaseLockssManagerService extends BaseLockssManager {
  protected HashMap auMap = new HashMap();
  private static Logger logger = Logger.getLogger("BaseLockssManagerService");

  public BaseLockssManagerService() { }

  public void startService() {
    super.startService();
  }

  public void stopService() {
    stopAllManagers();
    auMap.clear();
    super.stopService();
  }

  private void stopAllManagers() {
    logger.debug2("Stopping all managers...");
    Iterator entries = auMap.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Map.Entry) entries.next();
      LockssManager manager = (LockssManager) entry.getValue();
      manager.stopService();
    }
  }

  protected LockssManager getLockssManager(ArchivalUnit au) {
    LockssManager lockssMan = (LockssManager) auMap.get(au);
    if (lockssMan == null) {
      logger.error("Manager not found for au: " + au);
      throw new IllegalArgumentException("Manager not found for au.");
    }
    return lockssMan;
  }

  protected synchronized void addLockssManager(ArchivalUnit au) {
    LockssManager manager = (LockssManager) auMap.get(au);
    if (manager == null) {
      logger.debug2("Adding new manager...");
      manager = createNewManager(au);
      auMap.put(au, manager);
      manager.initService(theDaemon);
      manager.startService();
    }
  }

  protected abstract LockssManager createNewManager(ArchivalUnit au);
}