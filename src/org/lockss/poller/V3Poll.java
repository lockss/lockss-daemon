/*
 * $Id: V3Poll.java,v 1.2 2005-03-18 09:09:16 smorabito Exp $
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

package org.lockss.poller;

import org.lockss.protocol.*;

/**
 * Initial stub implementation of V3Poll.
 *
 * XXX: This class contains no code!  It is a skeleton implementation
 * of BasePoll only.
 *
 */

public class V3Poll extends BasePoll {
  
  public PollTally getVoteTally() {
    return null;
  }

  public V3Poll(PollSpec pollspec, PollManager pm,
		PeerIdentity orig, String key, long duration) {
    super(pollspec, pm, orig, key, duration);
  }

  public void receiveMessage(LcapMessage msg) {
    ;
  }

  public void startPoll() {
    ;
  }

  public boolean isErrorState() {
    return false;
  }

  public void stopPoll() {
    ;
  }

  public Vote copyVote(Vote vote, boolean agree) {
    return null;
  }

}
