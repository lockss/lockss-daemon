/*
 * $Id: MockEffortService.java,v 1.1.2.5 2004-10-05 00:37:24 dshr Exp $
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

package org.lockss.effort;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.plugin.*;
import org.lockss.config.*;

/**
 * Mock effort proof service.
 */
public class MockEffortService extends BaseLockssDaemonManager
  implements EffortService {
  static final String PREFIX = Configuration.PREFIX + "effort.";

  static Logger log = Logger.getLogger("MockEffortService");

  private EffortService theEffortService;
  private boolean myGenerateProofResult;
  private boolean myVerifyProofResult;
  private List myProof;
  private Exception myProofException;
  private boolean myGenerateVoteResult;
  private boolean myVerifyVoteResult;
  private boolean myAgreeVoteResult;
  private List myVote;
  private Exception myVoteException;
  private long proofDuration;
  private long voteDuration;

  public MockEffortService() {
    super();
    theEffortService = this;
    myGenerateProofResult = false;
    myVerifyProofResult = false;
    myProof = ListUtil.list();
    myProofException = null;
    proofDuration = 500;
    myGenerateVoteResult = false;
    myVerifyVoteResult = false;
    myAgreeVoteResult = false;
    myVote = ListUtil.list();
    myVoteException = null;
    voteDuration = 500;
  }

  public void setGenerateProofResult(boolean res) {
    myGenerateProofResult = res;
  }
  public void setVerifyProofResult(boolean res) {
    myVerifyProofResult = res;
  }
  public void setProof(List l) {
    myProof = l;
  }
  public void setProofDuration(long dur) {
    proofDuration = dur;
  }
  public void setProofException(Exception e) {
    myProofException = e;
  }

  public void setGenerateVoteResult(boolean res) {
    myGenerateVoteResult = res;
  }
  public void setVerifyVoteResult(boolean res) {
    myVerifyVoteResult = res;
  }
  public void setAgreeVoteResult(boolean res) {
    myAgreeVoteResult = res;
  }
  public void setVote(List l) {
    myVote = l;
  }
  public void setVoteDuration(long dur) {
    voteDuration = dur;
  }
  public void setVoteException(Exception e) {
    myVoteException = e;
  }

  /**
   * Ask for the effort proof specified by the <code>EffortService.Proof</code>
   * object to be generated so that it can later be retrieved from the
   * object.
   * @param ep     the <code>EffortService.Proof</code> to be generated.
   * @param timer  the <code>Deadline</code> by which generation must be
   *               complete.
   * @param cb     the object whose <code>generationFinished()</code>
   *               method will be called when generation is complete.
   * @param cookie used to disambiguate callback
   * @return       <code>true</code> if generation could be scheduled
   *               <code>false</code> otherwise.
   */
  public boolean proveEffort(Proof effortProof,
			     Deadline timer,
			     ProofCallback callback,
			     Serializable cookie) {
    final Proof ep = effortProof;
    final ProofCallback cb = callback;
    final Deadline dl = timer;
    final Exception ex = myProofException;
    // XXX
    TimerQueue.Callback tqcb = new TimerQueue.Callback() {
	public void timerExpired(Object tqCookie) {
	  log.debug("Effort generation callback for " + ((String) tqCookie));
	  cb.generationFinished(ep, dl, (Serializable)tqCookie, ex);
	}
      };
    TimerQueue.schedule(Deadline.in(proofDuration), tqcb, cookie);
    log.debug("Generation callback in " + proofDuration + " scheduled for " +
	      ((String) cookie));
    return true;
  }

  /**
   * Ask for the effort proof specified by the <code>EffortService.Proof</code>
   * object to be verified so that the result of verification can later
   * be obtained from the object.
   * @param ep     the <code>EffortService.Proof</code> to be generated.
   * @param timer  the <code>Deadline</code> by which verification must be
   *               complete.
   * @param cb     the object whose <code>generationFinished()</code>
   *               method will be called when verification is complete.
   * @param cookie used to disambiguate callback
   * @return       <code>true</code> if generation could be scheduled
   *               <code>false</code> otherwise.
   */
  public boolean verifyProof(Proof effortProof,
			     Deadline timer,
			     ProofCallback callback,
			     Serializable cookie) {
    final Proof ep = effortProof;
    final ProofCallback cb = callback;
    final Deadline dl = timer;
    final Exception ex = myProofException;
    // XXX
    TimerQueue.Callback tqcb = new TimerQueue.Callback() {
	public void timerExpired(Object tqCookie) {
	  log.debug("Effort callback for " + ((String) tqCookie) + " with " +
		    dl.getRemainingTime() + " to go");
	  cb.verificationFinished(ep, dl, (Serializable)tqCookie, ex);
	}
      };
    TimerQueue.schedule(Deadline.in(proofDuration), tqcb, cookie);
    log.debug("Callback in " + proofDuration + " scheduled for " +
	      ((String) cookie));
    return true;
  }

  /** Test whether an effort proof could be successfully sceduled before a
   * given deadline.
   * @param proof the <code>EffortService.Proof</code> to be scheduled
   * @param when the deadline
   * @return true if such a request could be accepted into the scedule.
   */
  public boolean canProofBeScheduledBefore(Proof ep, Deadline when) {
    // XXX
    return true;
  }

  /**
   * Ask for the vote specified by the <code>EffortService.Vote</code>
   * object to be generated so that it can later be retrieved from the
   * object.
   * @param vote   the <code>EffortService.Vote</code> to be generated.
   * @param timer  the <code>Deadline</code> by which generation must be
   *               complete.
   * @param cb     the object whose <code>generationFinished()</code>
   *               method will be called when generation is complete.
   * @param cookie used to disambiguate callback
   * @return       <code>true</code> if generation could be scheduled
   *               <code>false</code> otherwise.
   */
  public boolean generateVote(Vote voteSpec,
			      Deadline timer,
			      VoteCallback callback,
			      Serializable cookie) {
    final Vote vote = voteSpec;
    final VoteCallback cb = callback;
    final Deadline dl = timer;
    final Exception ex = myProofException;
    // XXX
    TimerQueue.Callback tqcb = new TimerQueue.Callback() {
	public void timerExpired(Object tqCookie) {
	  log.debug("Vote generation callback for " + ((String) tqCookie));
	  cb.generationFinished(vote, dl, (Serializable)tqCookie, ex);
	}
      };
    TimerQueue.schedule(Deadline.in(voteDuration), tqcb, cookie);
    log.debug("Callback in " + voteDuration + " scheduled for " +
	      ((String) cookie));
    return true;
  }

  /**
   * Ask for the vote specified by the <code>EffortService.Vote</code>
   * object to be verified so that the result of verification can later
   * be obtained from the object.
   * @param vote   the <code>EffortService.Vote</code> to be generated.
   * @param timer  the <code>Deadline</code> by which verification must be
   *               complete.
   * @param cb     the object whose <code>generationFinished()</code>
   *               method will be called when verification is complete.
   * @param cookie used to disambiguate callback
   * @return       <code>true</code> if verification could be scheduled
   *               <code>false</code> otherwise.
   */
  public boolean verifyVote(Vote voteSpec,
			    Deadline timer,
			    VoteCallback callback,
			    Serializable cookie) {
    final Vote vote = voteSpec;
    final VoteCallback cb = callback;
    final Deadline dl = timer;
    final Exception ex = myProofException;
    // XXX
    TimerQueue.Callback tqcb = new TimerQueue.Callback() {
	public void timerExpired(Object tqCookie) {
	  log.debug("Vote generation callback for " + ((String) tqCookie));
	  cb.verificationFinished(vote, dl, (Serializable)tqCookie, ex);
	}
      };
    TimerQueue.schedule(Deadline.in(voteDuration), tqcb, cookie);
    log.debug("Callback in " + voteDuration + " scheduled for " +
	      ((String) cookie));
    return true;
  }

  /** Test whether a vote could be successfully scheduled before a
   * given deadline.
   * @param vote the <code>EffortService.Vote</code> to be generated
   * @param when the deadline
   * @return true if such a request could be accepted into the scedule.
   */
  public boolean canVoteBeScheduledBefore(Vote vote, Deadline when) {
    // XXX - need control over this
    return true;
  }

  /** Return true if the EffortService has nothing to do.  Useful in unit
   * tests. */
  public boolean isIdle() {
    //  XXX - this needs to go false while processing
    return true;
  }

  /** Cancel generation of the specified proof.
   * @param ep the <code>EffortService.Proof</code> to be cancelled.
   */
  public void cancelProofs(Proof ep) {
    // XXX need an implementation
  }

  public Proof makeProof() {
    return new ProofImpl();
  }

  public Vote makeVote() {
    return new VoteImpl();
  }

  /**
   * <code>EffortService.Proof</code> is used to describe effort proofs
   * to be generated by the effort service.
   */
  public class ProofImpl implements Proof {
    private boolean generateResult;
    private boolean verifyResult;
    private List proof;
    private Exception ex;

    ProofImpl() {
      generateResult = myGenerateProofResult;
      verifyResult = myVerifyProofResult;
      proof = myProof;
      ex = myProofException;
    }
    /**
     * Return the <code>EffortService</code> instance in use
     */
    public EffortService getEffortService() {
      return theEffortService;
    }

    public List getProof() {
      return proof;
    }

    public boolean isVerified() {
      return (ex == null && verifyResult);
    }

    protected boolean generate() {
      return generateResult;
    }

    protected boolean verify() {
      return verifyResult;
    }

    protected Exception getException() {
      return ex;
    }
  }

  /**
   * <code>EffortService.Vote</code> is used to describe effort proofs
   * to be generated by the effort service.
   */
  public class VoteImpl implements Vote {
    private boolean generateResult;
    private boolean verifyResult;
    private boolean agreeResult;
    private List vote;
    private Exception ex;

    VoteImpl() {
      generateResult = myGenerateVoteResult;
      verifyResult = myVerifyVoteResult;
      agreeResult = myAgreeVoteResult;
      vote = myVote;
      ex = myVoteException;
    }
    // XXX
    /**
     * Return the <code>EffortService</code> instance in use
     */
    public EffortService getEffortService() {
      return theEffortService;
    }

    public List getVote() {
      return vote;
    }

    public boolean isValid() {
      return (ex == null && verifyResult);
    }

    public boolean isAgreement() {
      return (ex == null && agreeResult);
    }

    protected boolean generate() {
      return generateResult;
    }

    protected boolean verify() {
      return verifyResult;
    }

    protected Exception getException() {
      return ex;
    }
  }


}
