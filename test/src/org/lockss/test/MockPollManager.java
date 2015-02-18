/*
* $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.LcapMessage;
import org.lockss.protocol.ProtocolException;

/**
 * Mock override of the PollManager
 */
public class MockPollManager extends PollManager {
  public Hashtable thePolls = new Hashtable();
  public static final String NAME_REQUESTED = "name_requested";
  public static final String CONTENT_REQUESTED = "content_requested";
  public static final String SUSPENDED = "suspended";
  public static final String RESUMED = "resumed";

  public MockPollManager() {
    super();
  }
  public void initService(LockssDaemon daemon) throws LockssAppException {
    theLog.debug("MockPollManager: initService");
    super.initService(daemon);
  }
  public void startService() {
    theLog.debug("MockPollManager: startService");
    super.startService();
  }

  public void stopService() {
    theLog.debug("MockPollManager: stopService");
    super.stopService();
    thePolls = new Hashtable();
  }

  public Poll callPoll(PollSpec pollspec) {
    Poll ret = null;
    theLog.debug("MockPollManager: call V" + pollspec.getProtocolVersion() + " poll");
    switch (pollspec.getProtocolVersion()) {
    case 1:
      try {
	sendV1PollRequest(pollspec);
	ret = new MockPoll();
      } catch (IOException ioe) {
	theLog.error("Exception sending V1 poll request for " +
		     pollspec + ioe);
      }
      break;
    default:
      theLog.error("No support for V" + pollspec.getProtocolVersion() + " yet");
      break;
    }
    theLog.debug("MockPollManager: call " +
		 (ret != null ? "succeeds" : "fails"));
    return ret;
  }


  private void sendV1PollRequest(PollSpec ps) throws IOException {
    // note: uses a different key than the other two, since we're not
    // creating an actual challenge and verifier to key off of.
    int opcode = ps.getPollType();
    if (opcode == Poll.V1_CONTENT_POLL) {
      theLog.debug("MockPollManager: send V1 content poll request");
      thePolls.put(ps.getUrl(), CONTENT_REQUESTED);
    }
    else if (opcode == Poll.V1_NAME_POLL) {
      theLog.debug("MockPollManager: send V1 name poll request");
     thePolls.put(ps.getUrl(), NAME_REQUESTED);
    } else {
      theLog.debug("MockPollManager: send V1 bogus poll request");
    }
  }

  public boolean isPollRunning(PollSpec ps) {
    boolean ret = thePolls.get(ps.getUrl()) != null;
    theLog.debug("MockPollManager: isPollRunning(" + ps.getUrl() + ") " + ret);
    return ret;
  }

  public void suspendPoll(String key) {
    thePolls.put(key, SUSPENDED);
  }

  public void resumePoll(boolean replayNeeded, String key) {
    thePolls.put(key, RESUMED);
  }

  public String getPollStatus(String key) {
    String ret = (String)thePolls.get(key);
    theLog.debug("MockPollManager: getPollStatus(" + key + ") " + ret);
    return ret;
  }

}
