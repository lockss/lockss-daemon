/*
* $Id: PollFactory.java,v 1.5 2004-09-29 01:19:11 dshr Exp $
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

import org.lockss.protocol.*;
import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * PollFactory instances create Poll objects of the appropriate version.
 */
/**
 * <p>PollFactory instances create Poll objects of the appropriate version.</p>
 * @author David Rosenthal
 * @version 1.0
 */

public interface PollFactory {

  /**
   * Call a poll.  Only used by the tree walk via the poll manager.
   * For V1 sends the poll request.
   * @param poll the <code>Poll</code> to be called
   * @param pm the PollManager that called this method
   * @param im the IdentityManager
   * @return true if the poll was successfuly called.
   */
  boolean callPoll(Poll poll,
		   PollManager pm,
		   IdentityManager im);


  /**
   * createPoll is invoked when (a) an incoming message requires a new
   * Poll to be created (msg != null) and (b) when a new Poll is required
   * with no incoming message (msg == null).  V1 uses only case (a).
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method
   * @param im the IdentityManager
   * @param orig the PeerIdentity that called the poll
   * @param challenge the poll challenge
   * @param verifier the poll verifier
   * @param duration the duration of the poll
   * @param hashAlg the hash algorithm in use
   * @return a Poll object describing the new poll.
   */
  BasePoll createPoll(PollSpec pollspec,
		      PollManager pm,
		      IdentityManager im,
		      PeerIdentity orig,
		      byte[] challenge,
		      byte[] verifier,
		      long duration,
		      String hashAlg) throws ProtocolException;

  /**
   * shouldPollBeCreated is invoked to check for conflicts or other
   * version-specific reasons why the poll should not be created at
   * this time.
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method.
   * @param im the IdentityManager
   * @param challenge the poll challenge
   * @param orig the PeerIdentity that called the poll
   * @return true if it is OK to call the poll
   */
   boolean shouldPollBeCreated(PollSpec pollspec,
			       PollManager pm,
			       IdentityManager im,
			       byte[] challenge,
			       PeerIdentity orig);

  /**
   * getPollActivity returns the type of activity defined by ActivityRegulator
   * that describes this poll.
   * @param pollspec the PollSpec for the poll.
   * @param pm the PollManager that called this method.
   * @return one of the activity codes defined by ActivityRegulator
   */
   int getPollActivity(PollSpec pollspec,
		       PollManager pm);

  /**
   * setConfig updates the poll factory's configuration
   * @param newConfig the new gonfiguration of the daemon
   * @param oldConfig the previous configuration of the daemon
   * @param changedKeys the items that have changed
   */
  public void setConfig(Configuration newConfig,
			Configuration oldConfig,
			Configuration.Differences changedKeys);

  public long getMaxPollDuration(int pollType);

  public long getMinPollDuration(int pollType);

  public long calcDuration(PollSpec ps, PollManager pm);
}
