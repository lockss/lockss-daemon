/*
 * $Id: V3PollFactory.java,v 1.1.2.5 2004-10-06 00:26:22 dshr Exp $
 */

/*

Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller;

import java.io.*;
import java.security.*;
import java.util.*;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.protocol.ProtocolException;
import org.lockss.util.*;
import org.lockss.hasher.HashService;
import org.lockss.daemon.status.*;
import org.lockss.state.*;
import org.lockss.config.*;
import org.mortbay.util.B64Code;
import org.lockss.alert.AlertManager;
import org.lockss.alert.*;

/**
 * <p>Class that creates V3 Poll objects</p>
 * @author David Rosenthal
 * @version 1.0
 */

public class V3PollFactory implements PollFactory {

  // XXX V1 hangover
  protected static long m_minContentPollDuration;
  protected static long m_maxContentPollDuration;
  protected static long m_minNamePollDuration;
  protected static long m_maxNamePollDuration;
  // XXX end V1 hangover
  protected int m_quorum = 0;

  protected static Logger theLog = Logger.getLogger("V3PollFactory");

  protected V3PollFactory() {
    // XXX
  }

  /**
   * Call a poll - i.e. start the processing of a V3 poll called by
   * this peer.  The <code>poll</code> parameter is a <code>V3Poller</code>
   * object created recently via the <code>createPoll()</code> method.
   * Only used by the tree walk via the poll manager.
   * @param pollspec the <code>PollSpec</code> that defines the subject of
   *                 the <code>Poll</code>.
   * @param pm       the PollManager that called this method
   * @return true if the poll was successfuly called.
   */
  public boolean callPoll(Poll poll,
			  PollManager pm,
			  IdentityManager im) {
    boolean ret = false;
    PollSpec pollspec = poll.getPollSpec();
    if (pollspec.getPollVersion() != Poll.V3_POLL) {
      theLog.warning("Bad poll version for: " + pollspec.toString());
      return ret;
    }
    //  XXX do whatever to get poll started
    ret = true;
    // XXX
    return ret;
  }
 
  /**
   * <code>createPoll()</code. is invoked in two circumstances.
   * When an incoming message is an invitation to vote in a poll
   * called by some other peer a <code>V3Voter</code> object will
   * be created.  When the tree walk needs a new poll a <code>V3Poller</code>
   * object will be created.  Shortly,  <code>callPoll()</code>
   * will be called with the <code>V3Poller</code> as an argument.
   * The difference is whether the <code>orig</code> parameter is
   * remote or local.
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method
   * @param orig the PeerIdentity of the peer calling the poll
   * @param challenge a <code>byte[]</code> with the poller's nonce
   * @param verifier not used by V3 polls
   * @param duration the duration of the poll
   * @param hashAlg the hash algorithm to use
   * @return a Poll object describing the new poll.
   */
  public BasePoll createPoll(PollSpec pollspec,
			     PollManager pm,
			     IdentityManager im,
			     PeerIdentity orig,
			     byte[] challenge,
			     byte[] verifier,
			     long duration,
			     String hashAlg) throws ProtocolException {
    BasePoll ret_poll = null;

    if (pollspec.getPollVersion() != 3) {
      throw new ProtocolException("V3PollFactory: bad version " +
				  pollspec.getPollVersion());
    }
    if (im.isLocalIdentity(orig)) {
	ret_poll = new V3Poller(pollspec, pm, orig, challenge, duration, hashAlg);
    } else {
	ret_poll = new V3Voter(pollspec, pm, orig, challenge, duration, hashAlg);
    }
    // XXX other stuff?
    return ret_poll;
  }



  /**
   * shouldPollBeCreated is invoked to check for conflicts or other
   * version-specific reasons why the poll should not be created at
   * this time.
   * @param msg the LcapMessage that triggered the new Poll
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method.
   * @return true if it is OK to call the poll
   */
   public boolean shouldPollBeCreated(PollSpec pollspec,
				      PollManager pm,
				      IdentityManager im,
				      byte[] challenge,
				      PeerIdentity orig) {
     boolean ret = false;
     // XXX
     ret = true;
     // XXX
     return ret;
   }


  /**
   * getPollActivity returns the type of activity defined by ActivityRegulator
   * that describes this poll.
   * @param msg the LcapMessage that triggered the new Poll
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method.
   * @return one of the activity codes defined by ActivityRegulator
   */
   public int getPollActivity(PollSpec pollspec,
			      PollManager pm) {
       return(ActivityRegulator.STANDARD_CONTENT_POLL);
   }

  /**
   * check for conflicts between the poll defined by the Message and any
   * currently existing poll.
   * @param cus the <code>CachedUrlSet</code> from the url and reg expression
   * @return the CachedUrlSet of the conflicting poll.
   */
  private CachedUrlSet checkForConflicts(CachedUrlSet cus,
					 PollManager pm,
					 BasePoll poll) {

    Iterator iter = pm.getActivePollSpecIterator(poll);
    theLog.debug("checkForConflicts on " + cus);
    while(iter.hasNext()) {
      PollSpec ps = (PollSpec)iter.next();
      theLog.debug("compare " + cus + " with " + ps.getCachedUrlSet());
      if (ps.getPollType() != Poll.VERIFY_POLL) {
	CachedUrlSet pcus = ps.getCachedUrlSet();
        int rel_pos = cus.cusCompare(pcus);
        if(rel_pos != CachedUrlSet.SAME_LEVEL_NO_OVERLAP &&
           rel_pos != CachedUrlSet.NO_RELATION) {
	  theLog.debug("New poll on " + cus + " conflicts with " + pcus);
          return pcus;
        }
      }
    }
    theLog.debug("New poll on " + cus + " no conflicts");
    return null;
  }

  // Poll time calculation
  public long calcDuration(int opcode, CachedUrlSet cus, PollManager pm) {
    long ret = -1;
    // XXX
    ret = 1000000;
    return ret;
  }

  public boolean canPollBeScheduled(long pollTime, long hashTime,
				  PollManager pm) {
    boolean ret = false;
    theLog.debug("Try to schedule " + pollTime + " poll " + hashTime + " poll");
    if (hashTime > pollTime) {
      theLog.warning("Total hash time " +
		     StringUtil.timeIntervalToString(hashTime) +
		     " greater than max poll time " +
		     StringUtil.timeIntervalToString(pollTime));
      return false;
    }
    Deadline when = Deadline.in(pollTime);
    // XXX ret = canHashBeScheduledBefore(hashTime, when, pm);
    return ret;
  }

  /**
   * setConfig updates the poll factory's configuration
   * @param newConfig the new gonfiguration of the daemon
   * @param oldConfig the previous configuration of the daemon
   * @param changedKeys the items that have changed
   */
  public void setConfig(Configuration newConfig,
			Configuration oldConfig,
			Configuration.Differences changedKeys) {
  }

    public long getMaxPollDuration(int pollType) {
	long ret = 0;
	// XXX
	return ret;
    }

    public long getMinPollDuration(int pollType) {
	long ret = 0;
	// XXX
	return ret;
    }

  protected int getQuorum() {
    return m_quorum;
  }

    public long calcDuration(PollSpec ps, PollManager pm) {
	long ret = 1000000;
	// XXX
	return ret;
    }
}
