/*
 * $Id: HashService.java,v 1.8 2003-02-07 19:15:48 aalto Exp $
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

package org.lockss.hasher;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.app.*;

/**
 * API for content and name hashing services.
 * HashService maintains a HashQueue, onto which it enqueues requests.
 *
 * HashQueue refuses to overcommit the available resources, and it does
 * time-slice scheduling so that large requests, which can take hours, do
 * not lock out smaller requests.
 */
public class HashService implements LockssManager {
  // Queue of outstanding hash requests.  The currently executing request,
  // if any, is on the queue, but not necessarily still at the head.
  // Currently there is a single hash queue.  (Might make sense to have
  // more if there are multiple disks.)
  private static HashQueue theQueue = null;
  private static HashService theService = null;
  private LockssDaemon theDaemon = null;

  public HashService() {}

  /**
   * init the plugin manager.
   * @param daemon the LockssDaemon instance
   * @throws LockssDaemonException if we already instantiated this manager
   * @see org.lockss.app.LockssManager#initService(LockssDaemon daemon)
   */
  public void initService(LockssDaemon daemon) throws LockssDaemonException {
    if(theService == null) {
      theDaemon = daemon;
      theService = this;
    }
    else {
      throw new LockssDaemonException("Multiple Instantiation.");
    }
  }

  /**
   * start the plugin manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    theQueue = new HashQueue();
    theQueue.init();
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    // TODO: checkpoint here.
    if (theQueue != null) {
      theQueue.stop();
    }
    theQueue = null;

    theService = null;
  }

  /**
   * Ask for the content of the <code>CachedUrlSet</code> object to be
   * hashed by the <code>hasher</code> before the expiration of
   * <code>deadline</code>, and the result provided to the
   * <code>callback</code>.
   * @param urlset   a <code>CachedUrlSet</code> object representing
   *                 the content to be hashed.
   * @param hasher   a <code>MessageDigest</code> object to which
   *                 the content will be provided.
   * @param deadline the time by which the callbeack must have been
   *                 called.
   * @param callback the object whose <code>hashComplete()</code>
   *                 method will be called when hashing succeds
   *                 or fails.
   * @param cookie   used to disambiguate callbacks
   * @return <code>true</code> if the request has been queued,
   *         <code>false</code> if the resources to do it are not
   *         available.
   */
  public boolean hashContent(CachedUrlSet urlset,
				    MessageDigest hasher,
				    Deadline deadline,
				    Callback callback,
				    Serializable cookie) {
    HashQueue.Request req =
      new HashQueue.Request(urlset, hasher, deadline,
			    callback, cookie,
			    urlset.getContentHasher(hasher),
			    urlset.estimatedHashDuration());
    return scheduleReq(req);
  }

  /**
   * Ask for the names in the <code>CachedUrlSet</code> object to be
   * hashed by the <code>hasher</code> before the expiration of
   * <code>deadline</code>, and the result provided to the
   * <code>callback</code>.
   * @param urlset   a <code>CachedUrlSet</code> object representing
   *                 the content to be hashed.
   * @param hasher   a <code>MessageDigest</code> object to which
   *                 the content will be provided.
   * @param deadline the time by which the callbeack must have been
   *                 called.
   * @param callback the object whose <code>hashComplete()</code>
   *                 method will be called when hashing succeeds
   *                 or fails.
   * @param cookie   used to disambiguate callbacks
   * @return <code>true</code> if the request has been queued,
   *         <code>false</code> if the resources to do it are not
   *         available.
   */
  public boolean hashNames(CachedUrlSet urlset,
				  MessageDigest hasher,
				  Deadline deadline,
				  Callback callback,
				  Serializable cookie) {
    HashQueue.Request req =
      new HashQueue.Request(urlset, hasher, deadline,
			    callback, cookie,
			    // tk - get better duration estimate
			    urlset.getNameHasher(hasher), 1000);
    return scheduleReq(req);
  }

  private boolean scheduleReq(HashQueue.Request req) {
    if (theQueue == null) {
      throw new IllegalStateException("HashService has not been initialized");
    }
    return theQueue.scheduleReq(req);
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

  /**
   * <code>HashService.Callback</code> is used to notify hash requestors
   * that their hash has succeeded or failed.
   */
  public interface Callback extends Serializable {
    /**
     * Called to indicate that hashing the content or names of a
     * <code>CachedUrlSet</code> object has succeeded, if <code>e</code>
     * is null,  or has failed otherwise.
     * @param urlset  the <code>CachedUrlSet</code> being hashed.
     * @param cookie  used to disambiguate callbacks.
     * @param hasher  the <code>MessageDigest</code> object that
     *                contains the hash.
     * @param e       the exception that caused the hash to fail.
     */
    public void hashingFinished(CachedUrlSet urlset,
				Object cookie,
				MessageDigest hasher,
				Exception e);
  }

  /** Exception thrown if a hash could not be completed by the deadline. */
  public static class Timeout extends Exception {
    public Timeout(String msg) {
      super(msg);
    }
  }

}
