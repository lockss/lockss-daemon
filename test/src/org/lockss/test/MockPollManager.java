/*
* $Id: MockPollManager.java,v 1.3 2003-02-20 02:23:40 aalto Exp $
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

import java.io.IOException;
import java.util.Hashtable;
import org.lockss.app.*;
import org.lockss.daemon.CachedUrlSet;
import org.lockss.poller.PollManager;
import org.lockss.protocol.LcapMessage;

/**
 * Mock override of the PollManager
 */
public class MockPollManager extends PollManager {
  public static Hashtable thePolls = new Hashtable();
  public static final String NAME_REQUESTED = "name_requested";
  public static final String CONTENT_REQUESTED = "content_requested";
  public static final String SUSPENDED = "suspended";
  public static final String RESUMED = "resumed";

  public MockPollManager() { }
  public void initService(LockssDaemon daemon) throws LockssDaemonException { }
  public void startService() { }
  public void stopService() {
    thePolls = new Hashtable();
  }

  public void requestPoll(CachedUrlSet cus, String lwrBound, String uprBound,
                          int opcode) throws IOException {
    // note: uses a different key than the other two, since we're not
    // creating an actual challenge and verifier to key off of.
    if (opcode == LcapMessage.CONTENT_POLL_REQ) {
      thePolls.put(cus.getUrl(), CONTENT_REQUESTED);
    }
    else if (opcode == LcapMessage.NAME_POLL_REQ) {
      thePolls.put(cus.getUrl(), NAME_REQUESTED);
    }
  }

  public void suspendPoll(String key) {
    thePolls.put(key, SUSPENDED);
  }

  public void resumePoll(boolean replayNeeded, Object key) {
    thePolls.put(key, RESUMED);
  }

  public String getPollStatus(String key) {
    return (String)thePolls.get(key);
  }
}