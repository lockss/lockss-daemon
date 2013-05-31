/*
 * $Id: LocalPollFactory.java,v 1.1.2.1 2013-05-31 19:45:49 dshr Exp $
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import java.io.IOException;
import java.util.*;

import org.mortbay.util.*;

import org.apache.commons.collections.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.config.Configuration.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.poller.*;
import org.lockss.protocol.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.util.StringUtil;

public class LocalPollFactory extends BasePollFactory {
  public static Logger log = Logger.getLogger("LocalPollFactory");

  private static final String PREFIX = Configuration.PREFIX + "poll.v3.";
  
  /** If set to 'false', do not start Local Polls.  This parameter is used
   * by NodeManagerImpl and PollManager.
   */
  public static final String PARAM_ENABLE_LOCAL_POLLER =
    PREFIX + "enableLocalPoller";
  public static final boolean DEFAULT_ENABLE_LOCAL_POLLER = true;

  private PollManager pollMgr;
  protected IdentityManager idMgr;

  public LocalPollFactory(PollManager pollMgr) {
    this.pollMgr = pollMgr;
  }

  /**
   * Create a LocalPoller
   */
  public BasePoll createPoll(PollSpec pollspec, LockssDaemon daemon,
                             PeerIdentity orig, long duration,
                             String hashAlg, LcapMessage msg)
      throws ProtocolException {
    if (idMgr == null) {
      idMgr = daemon.getIdentityManager();
    }
    if (pollspec.getProtocolVersion() != Poll.LOCAL_PROTOCOL) {
      throw new ProtocolException("bad version " +
				  pollspec.getProtocolVersion());
    }
    if (pollspec.getPollType() != Poll.LOCAL_POLL) {
      throw new ProtocolException("Unexpected poll type:" +
				  pollspec.getPollType());
    }
    if (duration <= 0) {
      throw new ProtocolException("bad duration " + duration);
    }
    if (msg == null) {
      // If there's no message, we're making a poller
      try {
	return makeLocalPoller(daemon, pollspec, orig, duration, hashAlg);
      } catch (V3Serializer.PollSerializerException ex) {
	log.error("Serialization exception creating new V3Poller: ", ex);
	return null;
      }
    }
    log.error("Local poll with a message?");
    return null;
  }

  /**
   * Construct a new Local Poller to perform a local poll.
   * 
   * @param daemon The LOCKSS daemon.
   * @param pollspec  The Poll Spec for this poll.
   * @param orig  The caller of the poll.
   * @param duration  The duration of the poll.
   * @param hashAlg  The Hash Algorithm used to call the poll.
   * @return A Local Poller.
   * @throws V3Serializer.PollSerializerException
   */
  private V3Poller makeLocalPoller(LockssDaemon daemon, PollSpec pollspec,
                                PeerIdentity orig, long duration,
                                String hashAlg)
      throws V3Serializer.PollSerializerException {
    log.debug("Creating LocalPoller for: " + pollspec);
    String key =
      String.valueOf(B64Code.encode(ByteArray.makeRandomBytes(20)));
    return null; //new LocalPoller(pollspec, daemon, orig, key, duration, hashAlg);
    // XXX LocalPoller needs to implement BasePoll
    // XXX BasePoll probably needs to include a lot of abstract methods
    // XXX that are now only on V3Poller.  See casts to (V3Poller) in
    // XXX poller/PollManager.java and poller/v3/V3PollStatus.java
  }
  
  // Not used.
  public int getPollActivity(PollSpec pollspec, PollManager pm) {
    return ActivityRegulator.STANDARD_CONTENT_POLL;
  }

  public void setConfig(Configuration newConfig, Configuration oldConfig,
                        Differences changedKeys) {
  }

  /** Not used.  Only implemented because our interface demands it. */
  public long getMaxPollDuration(int pollType) {
    return 0;
  }

  public long calcDuration(PollSpec ps, PollManager pm) {
    return PollUtil.calcV3Duration(ps, pm);
  }

  public boolean isDuplicateMessage(LcapMessage msg, PollManager pm) {
    return false;
  }
}
