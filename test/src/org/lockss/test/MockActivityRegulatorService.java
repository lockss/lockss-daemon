/*
 * $Id: MockActivityRegulatorService.java,v 1.1 2003-06-17 22:27:08 aalto Exp $
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

package org.lockss.test;

import java.util.*;
import org.lockss.app.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.daemon.*;

/**
 * Mock implementation of the LockssRepositoryService.
 */
public class MockActivityRegulatorService implements ActivityRegulatorService {
  private static LockssDaemon theDaemon;
  private static LockssManager theManager = null;
  public HashMap auMaps = new HashMap();

  public MockActivityRegulatorService() { }

  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if (theManager == null) {
      theDaemon = daemon;
      theManager = this;
    } else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  public void startService() {
  }

  public void stopService() {
  }

  public ActivityRegulator getActivityRegulator(ArchivalUnit au) {
    ActivityRegulator actReg = (ActivityRegulator)auMaps.get(au);
    if (actReg==null) {
      throw new IllegalArgumentException("ActivityRegulator not created for au.");
    }
    return actReg;
  }

  public synchronized void addActivityRegulator(ArchivalUnit au) {
    ActivityRegulator actReg = (ActivityRegulator)auMaps.get(au);
    if (actReg==null) {
      actReg = new MockActivityRegulator(au);
      auMaps.put(au, actReg);
    }
  }
}
