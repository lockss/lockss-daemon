/*
 * $Id: HashSvcQueueImpl.java,v 1.11 2008-05-19 07:42:12 tlipkis Exp $
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
 * Implementation of API for content and name hashing services that uses a
 * HashQueue, onto which it enqueues requests.
 *
 * HashQueue refuses to overcommit the available resources, and it does
 * time-slice scheduling so that large requests, which can take hours, do
 * not lock out smaller requests.
 */
public class HashSvcQueueImpl
  extends BaseLockssDaemonManager implements HashService, ConfigurableManager {

  protected static Logger log = Logger.getLogger("HashSvcQueueImpl");

  // Queue of outstanding hash requests.  The currently executing request,
  // if any, is on the queue, but not necessarily still at the head.
  // Currently there is a single hash queue.  (Might make sense to have
  // more if there are multiple disks.)
  private HashQueue theQueue = null;
  private long estPadConstant = 0;
  private long estPadPercent = 0;

  public HashSvcQueueImpl() {}

  /**
   * start the hash service.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    log.debug("startService()");
    theQueue = new HashQueue();
    theQueue.init();
    getDaemon().getStatusService().registerStatusAccessor("HashQ",
							  theQueue.getStatusAccessor());
  }

  /**
   * stop the hash service
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // TODO: checkpoint here.
    if (theQueue != null) {
      getDaemon().getStatusService().unregisterStatusAccessor("HashQ");
      theQueue.stop();
    }
    theQueue = null;

    super.stopService();
  }

  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    estPadConstant = config.getTimeInterval(PARAM_ESTIMATE_PAD_CONSTANT,
					    DEFAULT_ESTIMATE_PAD_CONSTANT);
    estPadPercent = config.getLong(PARAM_ESTIMATE_PAD_PERCENT,
				   DEFAULT_ESTIMATE_PAD_PERCENT);
  }

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
			      Object cookie) {
    if (hasher == null) throw new NullPointerException("null hasher");
    HashQueue.Request req =
      new HashQueue.Request(hasher.getCachedUrlSet(), deadline,
			    callback, cookie,
			    hasher,
			    hasher.getEstimatedHashDuration());
    return scheduleReq(req);
  }

  /** Cancel all hashes on the specified AU.  Temporary until a better
   * cancel mechanism is implemented.
   * @param au the AU
   */
  public void cancelAuHashes(ArchivalUnit au) {
    if (theQueue == null) {
      throw new IllegalStateException("HashService has not been initialized");
    }
    theQueue.cancelAuHashes(au);
  }

  /** Return the average hash speed, or -1 if not known.
   * @param digest the hashing algorithm
   * @return hash speed in bytes/ms, or -1 if not known
   */
  public int getHashSpeed(MessageDigest digest) {
    if (theQueue == null) {
      throw new IllegalStateException("HashService has not been initialized");
    }
    return theQueue.getHashSpeed(digest);
  }

  /** Add the configured padding percentage, plus the constant */
  public long padHashEstimate(long estimate) {
    return estimate + ((estimate * estPadPercent) / 100) + estPadConstant;
  }

  private boolean scheduleReq(HashQueue.Request req) {
    if (theQueue == null) {
      throw new IllegalStateException("HashService has not been initialized");
    }
    return theQueue.scheduleReq(req);
  }

  public boolean canHashBeScheduledBefore(long duration, Deadline when) {
    return theQueue.getAvailableHashTimeBefore(when) >= duration;
  }

  /** Return true if the HashService has nothing to do.  Useful in unit
   * tests. */
  public boolean isIdle() {
    return theQueue.isIdle();
  }

  /** Create a queue ready to receive and execute requests */
  protected void start() {
    theQueue = new HashQueue();
    theQueue.init();
  }

  /** Stop any queue runner(s) and destroy the queue */
  protected void stop() {
    if (theQueue != null) {
      theQueue.stop();
    }
    theQueue = null;
  }

}
