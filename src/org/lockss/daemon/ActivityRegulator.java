/*
 * $Id: ActivityRegulator.java,v 1.27 2004-02-07 06:44:05 eaalto Exp $
 */

/*

Copyright (c) 2001-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.repository.LockssRepository;

/**
 * The ActivityAllower is queried by the various managers when they wish to
 * start some activity on an {@link ArchivalUnit} or {@link CachedUrlSet}.
 * It keeps track of which activities are currently running, and decides
 * whether or not new activities can be permitted to start.  If so, it adjusts
 * the state to reflect the new activity.
 */
public class ActivityRegulator
  extends BaseLockssManager implements LockssAuManager {

// AU level
  /**
   * Integer representing the new content crawl activity.  AU level.
   */
  public static final int NEW_CONTENT_CRAWL = 1;

  /**
   * Integer representing the top level poll activity.  AU level.
   */
  public static final int TOP_LEVEL_POLL = 2;

  /**
   * Integer representing the treewalk activity.  AU level.
   */
  public static final int TREEWALK = 3;

  /**
   * Integer representing activity on a CUS in this AU.  AU level.
   * Blocks AU level activity, but not other CUS level activity.
   */
  static final int CUS_ACTIVITY = 9;

// CUS level
  /**
   * Integer representing the repair crawl activity.  CUS level
   */
  public static final int REPAIR_CRAWL = 11;

  /**
   * Integer representing the background crawl activity.  CUS level.
   */
  public static final int BACKGROUND_CRAWL = 12;

  /**
   * Integer representing the content poll activity.  CUS level.
   */
  public static final int STANDARD_CONTENT_POLL = 13;

  /**
   * Integer representing the single node content poll activity.  CUS level.
   */
  public static final int SINGLE_NODE_CONTENT_POLL = 14;

  /**
   * Integer representing the name poll activity.  CUS level.
   */
  public static final int STANDARD_NAME_POLL = 15;

  /**
   * Integer representing no activity. AU or CUS level.
   */
  static final int NO_ACTIVITY = -1;

  private static Logger logger = Logger.getLogger("ActivityRegulator");

//XXX set properly
  static final long STANDARD_LOCK_LENGTH = Constants.HOUR;
  static final long LOCK_EXTENSION_LENGTH = Constants.HOUR;

  HashMap cusMap = new HashMap();
  ArchivalUnit au;
  Lock curAuActivityLock = null;
  volatile boolean serviceActive = false;

  public ActivityRegulator(ArchivalUnit au) {
    this.au = au;
  }

  public void startService() {
    super.startService();
    serviceActive = true;
  }

  public void stopService() {
    serviceActive = false;
    logger.debug2("ActivityRegulator stopped.");
    cusMap.clear();
    super.stopService();
  }

  protected void setConfig(Configuration newConfig,
                           Configuration prevConfig,
                           Set changedKeys) {
    // nothing to config
  }

  public void setAuConfig(Configuration auConfig) {
  }

  /**
   * Tries to get a lock for the AU activity.  If it can, it creates the lock
   * to indicate that the activity has started.  Lock.expire() should be called
   * to free the lock.
   * @param newActivity the activity int
   * @param expireIn expire in X ms
   * @return the ActivityLock iff the activity was marked as started, else null
   */
  public synchronized Lock getAuActivityLock(int newActivity, long expireIn) {
    if (!serviceActive) {
      // service not started, or currently stopped
      return null;
    }
    // check if the au is free for this activity
    int curActivity = getAuActivity();
    if (!isAllowedOnAu(newActivity, curActivity)) {
      // au is being acted upon
      logger.debug2("AU '" + au.getName() + "' busy with " +
                    activityCodeToString(curActivity) + ". Couldn't start " +
                    activityCodeToString(newActivity));
      return null;
    }

    return setAuActivity(newActivity, expireIn);
  }

  /**
   * Tries to get a lock for a particular CUS-level activity.  If it can, it
   * creates the lock to indicate that the requested activity is now running.
   * Lock.expire() should be called to free the lock.
   * @param cus the {@link CachedUrlSet}
   * @param newActivity the activity int
   * @param expireIn expire in X ms
   * @return the ActivityLock iff the activity was marked as started, else null
   */
  public synchronized Lock getCusActivityLock(CachedUrlSet cus, int newActivity,
      long expireIn) {
    if (!serviceActive) {
      // service not started, or currently stopped
      return null;
    }
    // first, check if au is busy
    int auActivity = getAuActivity();
    if (!isAllowedOnAu(newActivity, auActivity)) {
      // au is being acted upon at the AU level
      logger.debug2("AU '" + cus.getArchivalUnit().getName() + "' busy with " +
                    activityCodeToString(auActivity) + ". Couldn't start " +
                    activityCodeToString(newActivity) + " on CUS '" +
                    cus.getUrl() + "'");
      return null;
    }
    if (checkForRelatedCusActivity(newActivity, cus)) {
      return null;
    }
    return setCusActivity(cus, newActivity, expireIn);
  }

  synchronized void checkAuActivity() {
    boolean otherAuActivity = false;
    List expiredKeys = new ArrayList(1);
    Iterator cusIt = cusMap.entrySet().iterator();
    while (cusIt.hasNext()) {
      // check each other cus to see if it's acting on this AU
      Map.Entry entry = (Map.Entry)cusIt.next();
      Lock value = (Lock)entry.getValue();
      if (value.isExpired()) {
        logger.debug3("Removing expired "+activityCodeToString(value.activity) +
                      " on CUS '" + entry.getKey() + "'");
        expiredKeys.add(entry.getKey());
        continue;
      }
      if (value.activity!=NO_ACTIVITY) {
        otherAuActivity = true;
        break;
      }
    }
    cusIt = expiredKeys.iterator();
    while (cusIt.hasNext()) {
      // remove the expired keys (avoids concurrent mod exception)
      cusMap.remove(cusIt.next());
    }

    if (!otherAuActivity) {
      // no other CUS activity on this AU, so free it up
      int curActivity = getAuActivity();
      if (curActivity == CUS_ACTIVITY) {
        curAuActivityLock = null;
      } else {
        logger.debug2(activityCodeToString(curActivity) + " running on AU '"+
                      au.getName() + "', so couldn't end " +
                      activityCodeToString(CUS_ACTIVITY));
      }
      logger.debug2("Finished " + activityCodeToString(CUS_ACTIVITY) +
                    " on AU '" + au.getName() + "'");
    }
  }

  boolean checkForRelatedCusActivity(int newActivity, CachedUrlSet cus) {
    Iterator cusIt = cusMap.entrySet().iterator();
    List expiredKeys = new ArrayList(1);
    boolean otherActivity = false;
    while (cusIt.hasNext()) {
      // check each other cus to see if it's related to this one
      Map.Entry entry = (Map.Entry)cusIt.next();
      CachedUrlSet entryCus = (CachedUrlSet)entry.getKey();
      int relation =
          theDaemon.getLockssRepository(au).cusCompare(entryCus, cus);
      if (relation!=LockssRepository.NO_RELATION) {
        Lock value = (Lock)entry.getValue();
        if (value.isExpired()) {
          logger.debug3("Removing expired " +
                        activityCodeToString(value.activity) +
                        " on CUS '" + entryCus + "'");
          expiredKeys.add(entryCus);
          continue;
        }
        String relationStr = "";
        switch (relation) {
          case LockssRepository.ABOVE:
            relationStr = "Parent";
            break;
          case LockssRepository.BELOW:
            relationStr = "Child";
            break;
          case LockssRepository.SAME_LEVEL_OVERLAP:
            relationStr = "Overlapping";
            break;
          case LockssRepository.SAME_LEVEL_NO_OVERLAP:
            relationStr = "Non-overlapping";
        }
        if (!isAllowedOnCus(newActivity, value.activity, relation)) {
          logger.debug2(relationStr + " CUS busy with " +
                        activityCodeToString(value.activity) +
                        ". Couldn't start " + activityCodeToString(newActivity) +
                        " on CUS '" + cus + "'");
          // found conflicting activity
          otherActivity = true;
          break;
        } else {
          logger.debug3(relationStr + " CUS activity " +
                        activityCodeToString(value.activity) +
                        " doesn't interfere with " +
                        activityCodeToString(newActivity));
        }
      }
    }

    cusIt = expiredKeys.iterator();
    while (cusIt.hasNext()) {
      // remove the expired keys (avoids concurrent mod exception)
      cusMap.remove(cusIt.next());
    }

    return otherActivity;
  }

  static boolean isAllowedOnAu(int newActivity, int curActivity) {
    switch (curActivity) {
      case NEW_CONTENT_CRAWL:
      case TREEWALK:
        // no new activity allowed
        return false;
      case TOP_LEVEL_POLL:
        // allow other polls to be called
        // poll manager will disallow inappropriate ones
        return ((newActivity==TOP_LEVEL_POLL) ||
                (newActivity==STANDARD_CONTENT_POLL));
      case CUS_ACTIVITY:
        // only other CUS activity is allowed while CUS activity is going on
        return (isCusActivity(newActivity));
      case NO_ACTIVITY:
      default:
        return true;
    }
  }

//XXX clean up properly
  static boolean isAllowedOnCus(int newActivity, int curActivity, int relation) {
    switch (curActivity) {
      case BACKGROUND_CRAWL:
      case REPAIR_CRAWL:
        switch (relation) {
          case LockssRepository.SAME_LEVEL_OVERLAP:
            return false;
          case LockssRepository.ABOVE:
            // if this CUS is a parent, any action allowed
            return true;
          case LockssRepository.BELOW:
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL));
          case LockssRepository.SAME_LEVEL_NO_OVERLAP:
          default:
            return true;
        }
      case STANDARD_CONTENT_POLL:
      case STANDARD_NAME_POLL:
        switch (relation) {
          case LockssRepository.SAME_LEVEL_OVERLAP:
            return ((newActivity==STANDARD_NAME_POLL) ||
                    (newActivity==STANDARD_CONTENT_POLL) ||
                    (newActivity==SINGLE_NODE_CONTENT_POLL));
          case LockssRepository.ABOVE:
            // if this CUS is a parent, allow content polls and repair crawls on
            // sub-nodes (PollManager should have blocked any truly illegal ones)
            return ((newActivity==STANDARD_CONTENT_POLL) ||
                     (newActivity==REPAIR_CRAWL));
           case LockssRepository.BELOW:
            // if this CUS is a child, only crawls allowed, and single node
            // content polls
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL) ||
                    (newActivity==SINGLE_NODE_CONTENT_POLL));
          case LockssRepository.SAME_LEVEL_NO_OVERLAP:
          default:
            return true;
        }
      case SINGLE_NODE_CONTENT_POLL:
        switch (relation) {
          case LockssRepository.SAME_LEVEL_OVERLAP:
            // only one action on a CUS at a time unless it's a name poll
            return false;
          case LockssRepository.ABOVE:
            // if this CUS is a parent, allow anything below
            return true;
          case LockssRepository.BELOW:
            // if this CUS is a child, only crawls allowed
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL));
          case LockssRepository.SAME_LEVEL_NO_OVERLAP:
          default:
            return true;

        }
      case NO_ACTIVITY:
      default:
        return true;
    }
  }
/*
  static boolean isAllowedOnCus(int newActivity, int curActivity, int relation) {
    switch (curActivity) {
      case BACKGROUND_CRAWL:
      case REPAIR_CRAWL:
        switch (relation) {
          case LockssRepository.SAME_LEVEL_OVERLAP:
            // only one action on a CUS at a time except for name polls
            // (resuming after repair crawl)
            return (newActivity==STANDARD_NAME_POLL);
          case LockssRepository.ABOVE:
            // if this CUS is a parent, any action allowed
            return true;
          case LockssRepository.BELOW:
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL));
          case LockssRepository.SAME_LEVEL_NO_OVERLAP:
          default:
            return true;
        }
      case STANDARD_CONTENT_POLL:
      case STANDARD_NAME_POLL:
        switch (relation) {
          case LockssRepository.SAME_LEVEL_OVERLAP:
            // only one action on a CUS at a time unless it's a name poll or
            // a repair crawl
            return ((newActivity==STANDARD_NAME_POLL) ||
                    (newActivity==REPAIR_CRAWL) ||
                    (newActivity==STANDARD_CONTENT_POLL) ||
                    (newActivity==SINGLE_NODE_CONTENT_POLL));
          case LockssRepository.ABOVE:
            // if this CUS is a parent, allow content polls and repair crawls on
            // sub-nodes (PollManager should have blocked any truly illegal ones)
            return ((newActivity==STANDARD_CONTENT_POLL) ||
                     (newActivity==REPAIR_CRAWL));
           case LockssRepository.BELOW:
            // if this CUS is a child, only crawls allowed, and single node
            // content polls
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL) ||
                    (newActivity==SINGLE_NODE_CONTENT_POLL));
          case LockssRepository.SAME_LEVEL_NO_OVERLAP:
          default:
            return true;
        }
      case SINGLE_NODE_CONTENT_POLL:
        switch (relation) {
          case LockssRepository.SAME_LEVEL_OVERLAP:
            // only one action on a CUS at a time unless it's a name poll
            return (newActivity==REPAIR_CRAWL);
          case LockssRepository.ABOVE:
            // if this CUS is a parent, allow anything below
            return true;
          case LockssRepository.BELOW:
            // if this CUS is a child, only crawls allowed
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL));
          case LockssRepository.SAME_LEVEL_NO_OVERLAP:
          default:
            return true;

        }
      case NO_ACTIVITY:
      default:
        return true;
    }
  }
*/

  int getAuActivity() {
    if (curAuActivityLock==null)  {
      return NO_ACTIVITY;
    } else if (curAuActivityLock.isExpired()) {
      logger.debug3("Removing expired " +
                    activityCodeToString(curAuActivityLock.activity) +
                    " on AU '" + au.getName() + "'");
      curAuActivityLock = null;
      return NO_ACTIVITY;
    } else {
      return curAuActivityLock.activity;
    }
  }

  Lock setAuActivity(int activity, long expireIn) {
    if (curAuActivityLock!=null) {
      // expire old lock
      curAuActivityLock.expire();
    }
    curAuActivityLock = new Lock(activity, expireIn);
    logger.debug("Started " + activityCodeToString(activity) + " on AU '" +
        au.getName() + "'");
    return curAuActivityLock;
  }

  /**
   * Converts an AU-level lock into a CUS-level lock.  Returns null if not
   * possible.
   * @param auLock the AU Lock
   * @param cus the CachedUrlSet
   * @param newActivity the CUS activity
   * @param expireIn expiration time
   * @return Lock the CusLock
   */
  public synchronized Lock changeAuLockToCusLock(Lock auLock,
      CachedUrlSet cus, int newActivity, long expireIn) {
    // since the code is much simpler for the single CUS version,
    // it was duplicated instead of using a single entry collection
    if (auLock==null) {
      logger.debug2("Null AU lock given for AU '"+au.getName()+
          "'.  Unable to convert to CUS lock.");
      return null;
    }

    setAuActivity(CUS_ACTIVITY, expireIn);
    Lock cusLock = (Lock)cusMap.get(cus);
    if (cusLock!=null) {
      logger.debug2("CUS '"+cus.getUrl()+"' already busy with "+
          activityCodeToString(cusLock.activity)+".  Couldn't start "+
          activityCodeToString(newActivity) + ".");
      return null;
    }

    logger.debug2("Changing lock on AU '"+au.getName()+ "'from "+
        activityCodeToString(auLock.activity) + " to CUS activity "+
        activityCodeToString(newActivity)+
        " on CUS '"+cus.getUrl()+"' with expiration in "+
        StringUtil.timeIntervalToString(expireIn));
    return getCusActivityLock(cus, newActivity, expireIn);
  }

  /**
   * Converts an AU-level lock into a group of CUS-level locks.  If any locks
   * are unattainable, they are skipped.
   * @param auLock the AU lock
   * @param cusLockReq a list of CusLockRequest objects
   * @return List of CusLocks
   */
  public synchronized List changeAuLockToCusLocks(Lock auLock,
      Collection cusLockReq) {
    if (auLock==null) {
      logger.debug2("Null AU lock given for AU '"+au.getName()+
          "'.  Unable to convert to CUS locks.");
      return new ArrayList();
    }

    // temporary set to allow CUS to set properly
    setAuActivity(CUS_ACTIVITY, STANDARD_LOCK_LENGTH);

    long maxExpire = 0;
    List lockList = new ArrayList(cusLockReq.size());
    Iterator iter = cusLockReq.iterator();
    while (iter.hasNext()) {
      CusLockRequest request = (CusLockRequest)iter.next();
      CachedUrlSet cus = request.cus;
      int newActivity = request.activity;
      long expireIn = request.expireIn;
      maxExpire = Math.max(maxExpire, expireIn);
      Lock cusLock = (Lock)cusMap.get(cus);
      if (cusLock!=null) {
        logger.debug2("CUS '"+cus.getUrl()+"' already busy with "+
            activityCodeToString(cusLock.activity)+".  Couldn't start "+
            activityCodeToString(newActivity) + ".");
        continue;
      }

      logger.debug2("Changing lock on AU '"+au.getName()+ "'from "+
          activityCodeToString(auLock.activity) + " to CUS activity "+
          activityCodeToString(newActivity)+
          " on CUS '"+cus.getUrl()+"' with expiration in "+
          StringUtil.timeIntervalToString(expireIn));
      Lock lock = getCusActivityLock(cus, newActivity, expireIn);
      if (lock!=null) {
        lockList.add(lock);
      }
    }
    // set to longest CUS duration
    setAuActivity(CUS_ACTIVITY, maxExpire);
    return lockList;
  }

  int getCusActivity(CachedUrlSet cus) {
    Lock entry = (Lock)cusMap.get(cus);
    if (entry==null)  {
      return NO_ACTIVITY;
    } else if (entry.isExpired()) {
      cusMap.remove(cus);
      logger.debug3("Removing expired " + activityCodeToString(entry.activity) +
                    " on AU '" + cus.getUrl() + "'");
      return NO_ACTIVITY;
    } else {
      return entry.activity;
    }
  }

  Lock setCusActivity(CachedUrlSet cus, int activity, long expireIn) {
    Lock cusLock = (CusLock)cusMap.get(cus);
    if (cusLock!=null) {
      // expire old lock
      cusLock.expire();
    }

    // set CUS state
    cusLock = new CusLock(cus, activity, expireIn);
    cusMap.put(cus, cusLock);
    // set AU state to indicate CUS activity (resets expiration time)
    setAuActivity(CUS_ACTIVITY, expireIn);
    logger.debug("Started " + activityCodeToString(activity) + " on CUS '" +
        cus.getUrl() + "'");
    return cusLock;
  }

  public static String activityCodeToString(int activity) {
    switch (activity) {
      case NEW_CONTENT_CRAWL:
        return "New Content Crawl";
      case BACKGROUND_CRAWL:
        return "Background Crawl";
      case REPAIR_CRAWL:
        return "Repair Crawl";
      case TOP_LEVEL_POLL:
        return "Top Level Poll";
      case STANDARD_CONTENT_POLL:
        return "Content Poll";
      case SINGLE_NODE_CONTENT_POLL:
        return "Single Node Content Poll";
      case STANDARD_NAME_POLL:
        return "Name Poll";
      case TREEWALK:
        return "Treewalk";
      case CUS_ACTIVITY:
        return "CUS Activity";
      case NO_ACTIVITY:
      default:
        return "No Activity";
    }
  }

  static boolean isAuActivity(int activity) {
    switch (activity) {
      case NEW_CONTENT_CRAWL:
      case TOP_LEVEL_POLL:
      case TREEWALK:
      case CUS_ACTIVITY:
      case NO_ACTIVITY:
        return true;
      default:
        return false;
    }
  }

  static boolean isCusActivity(int activity) {
    switch (activity) {
      case BACKGROUND_CRAWL:
      case REPAIR_CRAWL:
      case STANDARD_CONTENT_POLL:
      case SINGLE_NODE_CONTENT_POLL:
      case STANDARD_NAME_POLL:
      case NO_ACTIVITY:
        return true;
      default:
        return false;
    }
  }

  public class Lock {
    protected CachedUrlSet cus;
    protected int activity;
    protected Deadline expiration;

    public Lock(int activity, long expireIn) {
      this.activity = activity;
      expiration = Deadline.in(expireIn);

      // defaults to none
      this.cus = null;
    }

    public boolean isExpired() {
      return expiration.expired();
    }

    public void expire() {
      logger.debug2("Ending "+activityCodeToString(activity)+" on "+
                    (cus==null ? "AU '"+au.getName()+"'"
                     : "CUS '"+cus.getUrl()+"'"));
      if (expiration.expired()) {
        logger.debug2("Lock already expired.");
      }
      expiration.expire();
    }

    public void extend() {
      expiration.later(LOCK_EXTENSION_LENGTH);
      logger.debug2("Extending "+activityCodeToString(activity)+" on "+
                    (cus==null ? "AU '"+au.getName()+"'"
                     : "CUS '"+cus.getUrl()+"' by "+
                     StringUtil.timeIntervalToString(LOCK_EXTENSION_LENGTH)));
    }

    public int getActivity() {
      return activity;
    }

    public void setNewActivity(int newActivity, long expireIn) {
      logger.debug2("Changing activity on "+
          (cus==null ? "AU '"+au.getName()+ "'"
          : "CUS '"+cus.getUrl()+"'") + "from "+
          activityCodeToString(activity) + " to "+
          activityCodeToString(newActivity)+
          " with expiration in "+
          StringUtil.timeIntervalToString(expireIn));
      activity = newActivity;
      expiration = Deadline.in(expireIn);
      curAuActivityLock = new Lock(CUS_ACTIVITY, expireIn);
    }

    public ArchivalUnit getArchivalUnit() {
      return au;
    }

    public CachedUrlSet getCachedUrlSet() {
      return cus;
    }
  }

  public class CusLock extends Lock {
    public CusLock(CachedUrlSet cus, int activity, long expireIn) {
      super(activity, expireIn);
      this.cus = cus;
    }

    public void expire() {
      super.expire();
      checkAuActivity();
    }

    public void extend() {
      super.extend();
      curAuActivityLock.extend();
    }
  }

  public static class CusLockRequest {
    CachedUrlSet cus;
    int activity;
    long expireIn;

    public CusLockRequest(CachedUrlSet cus, int activity, long expireIn) {
      this.cus = cus;
      this.activity = activity;
      this.expireIn = expireIn;
    }
  }

  /**
   * Factory to create ActivityRegulator instances.
   */
  public static class Factory implements LockssAuManager.Factory {
    public LockssAuManager createAuManager(ArchivalUnit au) {
      return new ActivityRegulator(au);
    }
  }
}
