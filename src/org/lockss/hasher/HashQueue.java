/*
 * $Id: HashQueue.java,v 1.1 2002-09-19 20:49:36 tal Exp $
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

class HashQueue implements Serializable {
  protected static Logger log = Logger.getLogger("HashQueue");

  private LinkedList qlist = new LinkedList();
  private HashThread hashThread;
  private BinarySemaphore sem = new BinarySemaphore();
  private int hashPriority = -1;
  private int hashStepBytes = 10000;

  HashQueue() {
  }

  synchronized Request head() {
    // There seems to be no efficient way to determine if list is empty
    return (qlist.size() != 0) ? (Request)qlist.getFirst() : null;
  }

  // scan queue, removing and notifying hashes that have finshed or errored
  // or passed their deadline.
  void removeCompleted() {
    List done = new ArrayList();
    synchronized (qlist) {
      for (ListIterator iter = qlist.listIterator(); iter.hasNext();) {
	Request req = (Request)iter.next();
	if (req.e != null || req.urlsetHasher.finished()) {
	  iter.remove();
	  done.add(req);
	} else if (req.deadline.expired()) {
	  iter.remove();
	  req.e = new HashService.Timeout("hash not finished before deadline");
	  done.add(req);
	}
      }
    }
    doCallbacks(done);
  }

  void doCallbacks(List list) {
    for (Iterator iter = qlist.iterator(); iter.hasNext();) {
      Request req = (Request)iter.next();
      req.callback.hashingFinished(req.urlset, req.cookie,
				   req.hasher, req.e);
    }
  }

  // Insert request in queue iff it can finish in time and won't prevent
  // any that are already queued from finishing in time.
  synchronized boolean insert(Request req) {
    long now = System.currentTimeMillis();
    long totalDuration = 0;
    long durationThroughNewReq = -1;
    int pos = 0;
    for (ListIterator iter = qlist.listIterator(); iter.hasNext();) {
      Request qreq = (Request)iter.next();
      if (runBefore(req, qreq)) {
	// New req goes here.  Add its duration to the total.
	totalDuration += req.curEst();
	durationThroughNewReq = totalDuration;
	// Now check that all that follow could still finish in time
	// Requires backing up so will start with one we just looked at
	iter.previous();
	while (iter.hasNext()) {
	  qreq = (Request)iter.next();
	  totalDuration += qreq.curEst();
	  if (!canFinish(now, totalDuration, qreq.deadline)) {
	    return false;
	  }
	}
      } else {
	pos++;
	totalDuration += qreq.curEst();
      }
    }
    // check that new req can finish in time
    if (!canFinish(now,
		   (  durationThroughNewReq >= 0
		      ? durationThroughNewReq
		      : totalDuration + req.curEst()),
		   req.deadline)) {
      return false;
    }
    qlist.add(pos, req);
    return true;
  }

  boolean schedule(Request req) {
    if (!insert(req)) {
      return false;
    }
    ensureQRunner();
    sem.give();
    return true;
  }

  // tk - this should take into account other factors, such as
  // giving priority to requests with the least time remaining.
  boolean runBefore(Request r1, Request r2) {
    return r1.deadline.before(r2.deadline);
  }

  private boolean canFinish(long now, long duration, Deadline deadline) {
    return (now + duration) <= deadline.getExpirationTime();
  }

  void init() {
    registerConfigCallback();
  }

  // Register config callback
  private void registerConfigCallback() {
    Configuration.registerConfigurationCallback(new Configuration.Callback() {
	public void configurationChanged(Configuration oldConfig,
					 Configuration newConfig,
					 Set changedKeys) {
	  setConfig();
	}
      });
  }

  private void setConfig() {
    hashPriority =
      Configuration.getIntParam(Configuration.PREFIX + "hasher.priority", -1);
    hashStepBytes = 0;
      Configuration.getIntParam(Configuration.PREFIX + "hasher.stepBytes",
				10000);
  }

  // Request - hash queue element.
  static class Request implements Serializable {
    CachedUrlSet urlset;
    MessageDigest hasher;
    Deadline deadline;
    HashService.Callback callback;
    Object cookie;
    CachedUrlSetHasher urlsetHasher;
    long origEst;
    long timeUsed = 0;
    Exception e;

    Request(CachedUrlSet urlset,
	    MessageDigest hasher,
	    Deadline deadline,
	    HashService.Callback callback,
	    Object cookie,
	    CachedUrlSetHasher urlsetHasher,
	    long estimatedDuration) {
      this.urlset = urlset;
      this.hasher = hasher;
      this.deadline = deadline;
      this.callback = callback;
      this.cookie = cookie;
      this.urlsetHasher = urlsetHasher;
      this.origEst = estimatedDuration;
    }

    // tk penalize if exceeded estimate
    long curEst() {
      long t = origEst - timeUsed;
      return (t > 0 ? t : 0);
    }

    boolean finished() {
      return (e != null) || urlsetHasher.finished();
    }
  }

  public void stop() {
    if (hashThread != null) {
      log.info("Stopping Q runner");
      hashThread.stopQRunner();
      hashThread = null;
    }
  }

  // tk add watchdog
  void ensureQRunner() {
    if (hashThread == null) {
      log.info("Starting Q runner");
      hashThread = new HashThread("HashQ");
      hashThread.start();
    }
  }

  // Hash thread
  private class HashThread extends Thread {
    private boolean goOn = false;

    private HashThread(String name) {
      super(name);
    }

    public void run() {
      if (hashPriority > 0) {
	Thread.currentThread().setPriority(hashPriority);
      }
      goOn = true;

      ProbabilisticTimer timeout = new ProbabilisticTimer(60000, 10000);
      Request req;
      try {
	while (goOn) {
	  sem.take(timeout);
	  if ((req = head()) != null) {
	    CachedUrlSetHasher ush = req.urlsetHasher;
	    long startTime = System.currentTimeMillis();
	    try {
	      while (goOn && req == head() && !req.finished()) {
		ush.hashStep(hashStepBytes);
	      }
	      if (req.deadline.expired()) {
		throw
		  new HashService.Timeout("hash not finished before deadline");
	      }
	    } catch (Exception e) {
	      // tk - should this catch all Throwable?
	      req.e = e;
	    }
	    req.timeUsed += System.currentTimeMillis() - startTime;
	    if (req.finished()) {
	      removeCompleted();
	    }
	  }
//  	  lastReload = System.currentTimeMillis();
//  	  reloadInterval = Integer.getInteger(Configuration.PREFIX +
//  					      "parameterReloadInterval",
//  					      1800000).longValue();
//  	}
//  	ProbabilisticTimer nextReload =
//  	  new ProbabilisticTimer(reloadInterval, reloadInterval/4);
//  	log.info(nextReload.toString());
//  	if (goOn) {
//  	  nextReload.sleepUntil();
//  	}
	  
	}
//        } catch (InterruptedException e) {
      } finally {
	hashThread = null;
      }
    }

    private void stopQRunner() {
      goOn = false;
      this.interrupt();
    }
  }
}
