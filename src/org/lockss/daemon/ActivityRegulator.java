/*
 * $Id$
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
import org.lockss.config.Configuration;

/**
 * The ActivityRegulator is queried by the various managers when they wish to
 * start some activity on an {@link ArchivalUnit} or {@link CachedUrlSet}.
 * It keeps track of which activities are currently running, and decides
 * whether or not new activities can be permitted to start.  If so, it adjusts
 * the state to reflect the new activity.
 */
public class ActivityRegulator
  extends BaseLockssDaemonManager implements LockssAuManager {

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
  public static final int NO_ACTIVITY = -1;

  private static Logger logger = Logger.getLogger("ActivityRegulator");

//XXX are these still needed?
  static final long STANDARD_LOCK_LENGTH = Constants.HOUR;
  static final long LOCK_EXTENSION_LENGTH = Constants.HOUR;

//XXX should probably change this to a List, and eliminate the 'get()' calls
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
      if (logger.isDebug()) {
        logger.debug("AU '" + au.getName() + "' busy with " +
                     activityCodeToString(curActivity) + ". Couldn't start " +
                     activityCodeToString(newActivity));
      }
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
      if (logger.isDebug()) {
        logger.debug("AU '" + cus.getArchivalUnit().getName() + "' busy with " +
                     activityCodeToString(auActivity) + ". Couldn't start " +
                     activityCodeToString(newActivity) + " on CUS '" +
                     cus.getUrl() + "'");
      }
      return null;
    }
    if (checkForRelatedCusActivity(newActivity, cus)) {
      return null;
    }
    return setCusActivity(cus, newActivity, expireIn);
  }

  /**
   * Called when a CUS-lock is freed.  It checks the AU to see if any other
   * CUS-locks are active; if not, the AU-level CUS_ACTIVITY lock is freed.
   */
  synchronized void checkAuActivity() {
    boolean otherAuActivity = false;
    List expiredKeys = new ArrayList(1);
    Iterator cusIt = cusMap.entrySet().iterator();
    while (cusIt.hasNext()) {
      // check each other cus to see if it's acting on this AU
      Map.Entry entry = (Map.Entry)cusIt.next();
      Lock value = (Lock)entry.getValue();
      if (value.isExpired()) {
        logger.debug2("Removing expired "+activityCodeToString(value.activity) +
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
        if (logger.isDebug2()) {
          logger.debug2(activityCodeToString(curActivity) + " running on AU '" +
                        au.getName() + "', so couldn't end " +
                        activityCodeToString(CUS_ACTIVITY));
        }
      }
      if (logger.isDebug()) {
        logger.debug("Finished " + activityCodeToString(CUS_ACTIVITY) +
                     " on AU '" + au.getName() + "'");
      }
    }
  }

  /**
   * Checks to see if there are any other CUS-locks active which would
   * interfere with the new activity.
   * @param newActivity int
   * @param cus CachedUrlSet
   * @return boolean true iff conflicting activity
   */
  boolean checkForRelatedCusActivity(int newActivity, CachedUrlSet cus) {
    Iterator cusIt = cusMap.entrySet().iterator();
    List expiredKeys = new ArrayList(1);
    boolean otherActivity = false;
    while (cusIt.hasNext()) {
      // check each other cus to see if it's related to this one
      Map.Entry entry = (Map.Entry)cusIt.next();
      CachedUrlSet entryCus = (CachedUrlSet)entry.getKey();
      int relation = entryCus.cusCompare(cus);
      if (relation!=CachedUrlSet.NO_RELATION) {
        Lock value = (Lock)entry.getValue();
        if (value.isExpired()) {
          logger.debug2("Removing expired " +
                        activityCodeToString(value.activity) +
                        " on CUS '" + entryCus + "'");
          expiredKeys.add(entryCus);
          continue;
        }
        // see if it conflicts
        if (!isAllowedOnCus(newActivity, value.activity, relation)) {
          if (logger.isDebug()) {
            logger.debug(relationToString(relation) + " CUS busy with " +
                         activityCodeToString(value.activity) +
                         ". Couldn't start " +
                         activityCodeToString(newActivity) +
                         " on CUS '" + cus + "'");
          }
          // found conflicting activity
          otherActivity = true;
          break;
        } else {
          if (logger.isDebug2()) {
            logger.debug2(relationToString(relation) + " CUS activity " +
                          activityCodeToString(value.activity) +
                          " doesn't interfere with " +
                          activityCodeToString(newActivity));
          }
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

  /**
   * Returns true iff the new activity does not conflict with the current
   * activity on the AU.
   * @param newActivity int
   * @param curActivity int
   * @return boolean true iff no conflict
   */
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
        return true;
      default:
        logger.debug("Unexpected AU activity: "+curActivity+", "+
                     activityCodeToString(curActivity));
        return true;
    }
  }

  /**
   * Returns true iff the new activity does not conflict with the current
   * activity on another CUS.
   * @param newActivity int
   * @param curActivity int
   * @param relation the relationship between the active CUS and the new one
   * @return boolean true iff no conflict
   */
  static boolean isAllowedOnCus(int newActivity, int curActivity, int relation) {
    switch (curActivity) {
      case BACKGROUND_CRAWL:
      case REPAIR_CRAWL:
        switch (relation) {
          case CachedUrlSet.SAME_LEVEL_OVERLAP:
            return false;
          case CachedUrlSet.ABOVE:
            // if this CUS is a parent, any action allowed
            return true;
          case CachedUrlSet.BELOW:
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL));
          case CachedUrlSet.SAME_LEVEL_NO_OVERLAP:
            return true;
          default:
            logger.debug("Unexpected relation: "+relation+", "+
                         relationToString(relation));
            return true;
        }
      case STANDARD_CONTENT_POLL:
      case STANDARD_NAME_POLL:
        switch (relation) {
          case CachedUrlSet.SAME_LEVEL_OVERLAP:
            return ((newActivity==STANDARD_NAME_POLL) ||
                    (newActivity==STANDARD_CONTENT_POLL) ||
                    (newActivity==SINGLE_NODE_CONTENT_POLL));
          case CachedUrlSet.ABOVE:
            // if this CUS is a parent, allow content polls and repair crawls on
            // sub-nodes (PollManager should have blocked any truly illegal ones)
            return ((newActivity==STANDARD_CONTENT_POLL) ||
                     (newActivity==REPAIR_CRAWL));
           case CachedUrlSet.BELOW:
            // if this CUS is a child, only crawls allowed, and single node
            // content polls
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL) ||
                    (newActivity==SINGLE_NODE_CONTENT_POLL));
          case CachedUrlSet.SAME_LEVEL_NO_OVERLAP:
            return true;
          default:
            logger.debug("Unexpected relation: "+relation+", "+
                         relationToString(relation));
            return true;
        }
      case SINGLE_NODE_CONTENT_POLL:
        switch (relation) {
          case CachedUrlSet.SAME_LEVEL_OVERLAP:
            // only one action on a CUS at a time unless it's a name poll
            return false;
          case CachedUrlSet.ABOVE:
            // if this CUS is a parent, allow anything below
            return true;
          case CachedUrlSet.BELOW:
            // if this CUS is a child, only crawls allowed
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL));
          case CachedUrlSet.SAME_LEVEL_NO_OVERLAP:
            return true;
          default:
            logger.debug("Unexpected relation: "+relation+", "+
                         relationToString(relation));
            return true;

        }
      case NO_ACTIVITY:
        return true;
      default:
        logger.debug("Unexpected CUS activity: "+curActivity+", "+
                     activityCodeToString(curActivity));
        return true;
    }
  }

  /**
   * Returns the current AU activity, or NO_ACTIVITY if no lock or expired.
   * @return int the activity
   */
  int getAuActivity() {
    if (curAuActivityLock==null)  {
      return NO_ACTIVITY;
    } else if (curAuActivityLock.isExpired()) {
      if (logger.isDebug2()) {
        logger.debug2("Removing expired " +
                      activityCodeToString(curAuActivityLock.activity) +
                      " on AU '" + au.getName() + "'");
      }
      curAuActivityLock = null;
      return NO_ACTIVITY;
    } else {
      return curAuActivityLock.activity;
    }
  }

  /**
   * Sets the AU activity lock to a new lock.
   * @param activity int the new activity
   * @param expireIn long
   * @return Lock the AU lock
   */
  Lock setAuActivity(int activity, long expireIn) {
    if (curAuActivityLock!=null) {
      // expire old lock
      curAuActivityLock.expire();
    }
    curAuActivityLock = new Lock(activity, expireIn);
    if (logger.isDebug()) {
      logger.debug("Started " + activityCodeToString(activity) + " on AU '" +
                   au.getName() + "'");
    }
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
                                                 CachedUrlSet cus,
                                                 int newActivity,
                                                 long expireIn) {
    // since the code is much simpler for the single CUS version,
    // it was duplicated instead of using a single entry collection
    if (auLock==null) {
      logger.debug2("Null AU lock given for AU '"+au.getName()+
                    "'.  Unable to convert to CUS lock.");
      return null;
    }

    int oldAuActivity = auLock.activity;

    setAuActivity(CUS_ACTIVITY, expireIn);
    //XXX this check probably isn't needed, and is accomplished in
    //'getCusActivityLock()'
    Lock cusLock = (Lock)cusMap.get(cus);
    if (cusLock!=null) {
      if (logger.isDebug()) {
        logger.debug("CUS '" + cus.getUrl() + "' already busy with " +
                     activityCodeToString(cusLock.activity) +
                     ".  Couldn't start " +
                     activityCodeToString(newActivity) + ".");
      }
      return null;
    }

    cusLock = getCusActivityLock(cus, newActivity, expireIn);
    if (cusLock!=null) {
      if (logger.isDebug()) {
        logger.debug("Changing lock on AU '" + au.getName() + "'from " +
                     activityCodeToString(oldAuActivity) + " to CUS activity " +
                     activityCodeToString(newActivity) +
                     " on CUS '" + cus.getUrl() + "' with expiration in " +
                     StringUtil.timeIntervalToString(expireIn));
      }
    }
    return cusLock;
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

    int oldAuActivity = auLock.activity;
    // temporary set to allow CUS to set properly
    setAuActivity(CUS_ACTIVITY, STANDARD_LOCK_LENGTH);

    long maxExpire = 0;
    if (logger.isDebug()) {
      if (cusLockReq.size() > 0) {
        logger.debug("Changing lock on AU '" + au.getName() + "'from " +
                     activityCodeToString(oldAuActivity) + " to CUS activity.");
      } else {
        logger.debug("No CUS locks requested.  Expiring " +
                     activityCodeToString(oldAuActivity) +
                     "lock on AU '" + au.getName() + "'.");
      }
    }
    List lockList = new ArrayList(cusLockReq.size());
    Iterator iter = cusLockReq.iterator();
    while (iter.hasNext()) {
      CusLockRequest request = (CusLockRequest)iter.next();
      CachedUrlSet cus = request.cus;
      int newActivity = request.activity;
      long expireIn = request.expireIn;
      Lock lock = getCusActivityLock(cus, newActivity, expireIn);
      if (lock!=null) {
        maxExpire = Math.max(maxExpire, expireIn);
        lockList.add(lock);
      }
    }
    // set to longest CUS duration
    setAuActivity(CUS_ACTIVITY, maxExpire);
    return lockList;
  }

  /**
   * Gets the activity for the CUS, or NO_ACTIVITY if null or expired.
   * @param cus CachedUrlSet
   * @return int the activity
   */
  //XXX this is currently only used in tests
  int getCusActivity(CachedUrlSet cus) {
    Lock entry = (Lock)cusMap.get(cus);
    if (entry==null)  {
      return NO_ACTIVITY;
    } else if (entry.isExpired()) {
      cusMap.remove(cus);
      if (logger.isDebug2()) {
        logger.debug2("Removing expired " + activityCodeToString(entry.activity) +
                      " on AU '" + cus.getUrl() + "'");
      }
      return NO_ACTIVITY;
    } else {
      return entry.activity;
    }
  }

  /**
   * Sets the activity for a CUS.  Also sets the AU activity to CUS_ACTIVITY,
   * with the same expire time.
   * @param cus CachedUrlSet the CUS to set
   * @param activity int the new activity
   * @param expireIn long
   * @return Lock the newly created lock
   */
  Lock setCusActivity(CachedUrlSet cus, int activity, long expireIn) {
    //XXX this can probably be removed, since any locks will already have
    // been expired
    // XXX NO!  If isAllowedOnCus() allows a new activity to replace an
    // existing one, checkForRelatedCusActivity() will return false and
    // getCusActivityLock() will call this with the existing lock still set.
    Lock cusLock = (CusLock)cusMap.get(cus);
    if (cusLock!=null) {
      // expire old lock
      cusLock.expire();
    }

    // set CUS state
    cusLock = new CusLock(cus, activity, expireIn);
    cusMap.put(cus, cusLock);
    if ((curAuActivityLock==null) ||
        (curAuActivityLock.expiration.before(Deadline.in(expireIn)))) {
      // set AU state to indicate CUS activity (sets expiration time to latest)
      setAuActivity(CUS_ACTIVITY, expireIn);
    }
    if (logger.isDebug()) {
      logger.debug("Started " + activityCodeToString(activity) + " on CUS '" +
                   cus.getUrl() + "'");
    }
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

  static String relationToString(int relation) {
    switch (relation) {
      case CachedUrlSet.ABOVE:
        return "Parent";
      case CachedUrlSet.BELOW:
        return "Child";
      case CachedUrlSet.SAME_LEVEL_OVERLAP:
        return "Overlapping";
      case CachedUrlSet.SAME_LEVEL_NO_OVERLAP:
        return "Non-overlapping";
      case CachedUrlSet.NO_RELATION:
        return "No relation";
      default:
        return "Unknown";
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

  /**
   * The activity lock class.  For the basic lock (AU-level), the CUS defaults
   * to null.
   */
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
      if (logger.isDebug()) {
        logger.debug("Ending " + activityCodeToString(activity) + " on " +
                     (cus == null ? "AU '" + au.getName() + "'"
                      : "CUS '" + cus.getUrl() + "'"));
        if (expiration.expired()) {
          logger.debug("Lock already expired.");
        }
      }
      expiration.expire();
    }

    public void extend() {
      extend(LOCK_EXTENSION_LENGTH);
    }

    public void extend(long extension) {
      expiration.later(extension);
      if (logger.isDebug()) {
        logger.debug("Extending " + activityCodeToString(activity) + " on " +
                     (cus == null ? "AU '" + au.getName() + "'"
                      : "CUS '" + cus.getUrl() + "' by " +
                      StringUtil.timeIntervalToString(extension)));
      }
    }

    public int getActivity() {
      return activity;
    }

    /**
     * Sets a new activity and expiration.  If this is a CUS-level lock, also
     * checks the current AU lock and extends CUS_ACTIVITY if necessary.
     * @param newActivity int
     * @param expireIn long
     */
    public void setNewActivity(int newActivity, long expireIn) {
      if (logger.isDebug()) {
        logger.debug("Changing activity on " +
                     (cus == null ? "AU '" + au.getName() + "'"
                      : "CUS '" + cus.getUrl() + "'") + "from " +
                     activityCodeToString(activity) + " to " +
                     activityCodeToString(newActivity) +
                     " with expiration in " +
                     StringUtil.timeIntervalToString(expireIn));
      }
      activity = newActivity;
      expiration = Deadline.in(expireIn);
      if (cus!=null) {
        if ((curAuActivityLock!=null) &&
            (curAuActivityLock.getActivity()==CUS_ACTIVITY)) {
          curAuActivityLock.extend(expireIn);
        } else {
          logger.error("CUS lock changing during non-CUS activity.");
        }
      }
    }

    public ArchivalUnit getArchivalUnit() {
      return au;
    }

    public CachedUrlSet getCachedUrlSet() {
      return cus;
    }
  }

  /**
   * The CUS-level lock, an extension of the basic AU lock.
   */
  public class CusLock extends Lock {
    public CusLock(CachedUrlSet cus, int activity, long expireIn) {
      super(activity, expireIn);
      this.cus = cus;
    }

    /**
     * Overridden to check the AU to see if CUS_ACTIVITY should be terminated.
     */
    public void expire() {
      super.expire();
      checkAuActivity();
    }

    /**
     * Overridden to also extend the AU CUS_ACTIVITY lock.
     * @param extension long
     */
    public void extend(long extension) {
      super.extend(extension);
      curAuActivityLock.extend(extension);
    }
  }

  /**
   * A request for an activity lock.  Convenient struct.
   */
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
