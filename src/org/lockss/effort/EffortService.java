/*
 * $Id: EffortService.java,v 1.1.2.5 2004-10-04 17:56:34 dshr Exp $
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
 * API for effort proof services.
 */
public interface EffortService extends LockssManager {
  static final String PREFIX = Configuration.PREFIX + "effort.";

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
  public boolean proveEffort(Proof ep,
			     Deadline timer,
			     ProofCallback cb,
			     Serializable cookie);

  /** Test whether an effort proof could be successfully sceduled before a
   * given deadline.
   * @param ep the <code>EffortService.Proof</code> to be scheduled
   * @param when the deadline
   * @return true if such a request could be accepted into the scedule.
   */
  public boolean canProofBeScheduledBefore(Proof ep, Deadline when);

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
  public boolean generateVote(Vote vote,
			     Deadline timer,
			     VoteCallback cb,
			     Serializable cookie);

  /** Test whether a vote generation could be successfully scheduled before a
   * given deadline.
   * @param vote the <code>EffortService.Vote</code> to be scheduled
   * @param when the deadline
   * @return true if such a request could be accepted into the scedule.
   */
  public boolean canVoteBeScheduledBefore(Vote vote, Deadline when);

  /** Return true if the EffortService has nothing to do.  Useful in unit
   * tests. */
  public boolean isIdle();

  /** Cancel generation of the specified proof.
   * @param ep the <code>EffortService.Proof</code> to be cancelled.
   */
  public void cancelProofs(Proof ep);

    public Proof makeProof();

    public Vote makeVote();

  /**
   * <code>EffortService.Callback</code> is used to notify hash requestors
   * that their hash has succeeded or failed.
   */
  public interface ProofCallback extends Serializable {
    /**
     * Called to indicate generation of a proof of effort is complete.
     * @param ep the <code>EffortService.Proof</code> in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Proof ep,
				   Serializable cookie,
				   Exception e);
  }

  public interface VoteCallback extends Serializable {
    /**
     * Called to indicate generation of a vote is complete.
     * @param ep the <code>EffortService.Vote</code> in question
     * @param cookie used to disambiguate callbacks
     * @param e the exception that caused the effort proof to fail
     */
    public void generationFinished(EffortService.Vote vote,
				   Serializable cookie,
				   Exception e);
  }

  /**
   * <code>EffortService.Proof</code> is used to describe effort proofs
   * to be generated by the effort service.
   */
  public interface Proof extends Serializable {
    // XXX
    /**
     * Return the <code>EffortService</code> instance in use
     */
    public EffortService getEffortService();

      public List getProof();
    public boolean isVerified();
  }

  /**
   * <code>EffortService.Proof</code> is used to describe effort proofs
   * to be generated by the effort service.
   */
  public interface Vote extends Serializable {
    // XXX
    /**
     * Return the <code>EffortService</code> instance in use
     */
    public EffortService getEffortService();

    public List getVote();
    public boolean isValid();
    public boolean isAgreement();
  }

  /** Exception thrown if a hash could not be completed by the deadline. */
  public static class Timeout extends Exception {
    public Timeout(String msg) {
      super(msg);
    }
  }

}
