/*
 * $Id: MockHistoryRepository.java,v 1.1 2003-02-19 00:38:06 aalto Exp $
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

import java.util.HashMap;
import org.lockss.app.*;
import org.lockss.state.*;
import org.lockss.daemon.ArchivalUnit;

/**
 * MockHistoryRepository is a mock implementation of the HistoryRepository.
 */
public class MockHistoryRepository implements HistoryRepository, LockssManager {
  public HashMap storedHistories = new HashMap();
  public HashMap storedAus = new HashMap();

  public MockHistoryRepository() { }

  public void initService(LockssDaemon daemon) throws LockssDaemonException { }
  public void startService() { }
  public void stopService() {
    storedHistories = new HashMap();
    storedAus = new HashMap();
  }

  public void storePollHistories(NodeState nodeState) {
    storedHistories.put(nodeState.getCachedUrlSet(),
                        nodeState.getPollHistories());
  }

  /**
   * Doesn't function.
   * @param nodeState the NodeState
   */
  public void loadPollHistories(NodeState nodeState) {
  }

  public void storeAuState(AuState auState) {
    storedAus.put(auState.getArchivalUnit(), auState);
  }

  public AuState loadAuState(ArchivalUnit au) {
    return (AuState)storedAus.get(au);
  }
}