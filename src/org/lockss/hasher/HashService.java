/*
 * $Id$
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.hasher;

import java.security.MessageDigest;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.plugin.*;

/**
 * API for content and name hashing services.
 */
public interface HashService extends LockssManager {
  static final String PREFIX = Configuration.PREFIX + "hasher.";

  static final String PARAM_PRIORITY = PREFIX + "priority";
  static final int DEFAULT_PRIORITY = Thread.MIN_PRIORITY;

  static final String PARAM_STEP_BYTES = PREFIX + "stepBytes";
  static final int DEFAULT_STEP_BYTES = 10000;

  static final String PARAM_NUM_STEPS = PREFIX + "numSteps";
  static final int DEFAULT_NUM_STEPS = 10;

  static final String PARAM_NAME_HASH_ESTIMATE = PREFIX + "nameHashEstimate";
  static final long DEFAULT_NAME_HASH_ESTIMATE = Constants.SECOND;

  static final String PARAM_COMPLETED_MAX = PREFIX + "historySize";
  static final int DEFAULT_COMPLETED_MAX = 50;

  /** Constant by which hash estimates are increased */
  static final String PARAM_ESTIMATE_PAD_CONSTANT =
    PREFIX + "estimate.pad.constant";
  static final long DEFAULT_ESTIMATE_PAD_CONSTANT = 10;

  /** Percentage by which hash estimates are increased */
  static final String PARAM_ESTIMATE_PAD_PERCENT =
    PREFIX + "estimate.pad.percent";
  static final int DEFAULT_ESTIMATE_PAD_PERCENT = 10;



  public static final int CONTENT_HASH = 1;
  public static final int NAME_HASH = 2;

  /**
   * Ask for the <code>CachedUrlSetHasher</code> to be
   * executed by the <code>hasher</code> before the expiration of
   * <code>deadline</code>, and the result provided to the
   * <code>callback</code>.
   * @param hasher   an instance of a <code>CachedUrlSetHasher</code>
   *                 representing a specific <code>CachedUrlSet</code>
   *                 and hash type
   * @param deadline the time by which the callback must have been
   *                 called.
   * @param callback the object whose <code>hashComplete()</code>
   *                 method will be called when hashing succeds
   *                 or fails.
   * @param cookie   used to disambiguate callbacks
   * @return <code>true</code> if the request has been queued,
   *         <code>false</code> if the resources to do it are not
   *         available.
   */
  public boolean scheduleHash(CachedUrlSetHasher hasher,
			      Deadline deadline,
			      Callback callback,
			      Object cookie);

  /** Return the average hash speed, or -1 if not known.
   * @param digest the hashing algorithm
   * @return hash speed in bytes/ms, or -1 if not known
   */
  public int getHashSpeed(MessageDigest digest);

  /** Add the configured padding percentage, plus the constant */
  public long padHashEstimate(long estimate);

  /** Test whether a hash request could be successfully sceduled before a
   * given deadline.
   * @param duration the estimated hash time needed.
   * @param when the deadline
   * @return true if such a request could be accepted into the scedule.
   */
  public boolean canHashBeScheduledBefore(long duration, Deadline when);

  /** Return true if the HashService has nothing to do.  Useful in unit
   * tests. */
  public boolean isIdle();

  /** Cancel all hashes on the specified AU.  Temporary until a better
   * cancel mechanism is implemented.
   * @param au the AU
   */
  public void cancelAuHashes(ArchivalUnit au);

  /**
   * <code>HashService.Callback</code> is used to notify hash requestors
   * that their hash has succeeded or failed.
   */
  public interface Callback {
    /**
     * Called to indicate that hashing the content or names of a
     * <code>CachedUrlSet</code> object has succeeded, if <code>e</code>
     * is null,  or has failed otherwise.
     * @param urlset  the <code>CachedUrlSet</code> being hashed.
     * @param timeUsed amount of time hash actually took, in ms.
     * @param cookie  used to disambiguate callbacks.
     * @param hasher  the <code>CachedUrlSetHasher</code> object that
     *                contains the hash.
     * @param e       the exception that caused the hash to fail.
     */
    public void hashingFinished(CachedUrlSet urlset,
				long timeUsed,
				Object cookie,
				CachedUrlSetHasher hasher,
				Exception e);
  }

  /** Exception thrown if a hash could not be completed by the deadline. */
  public static class Timeout extends Exception {
    public Timeout(String msg) {
      super(msg);
    }
  }

  /** Marker Exception used to tell CachedUrlSet to set hash estimate to an
   * exact value rather than its normal averaging algorithm. */
  public static class SetEstimate extends Exception {
  }

}
