/*
 * $Id: ActivityRegulator.java,v 1.20 2003-06-25 21:16:34 eaalto Exp $
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
import org.lockss.app.BaseLockssManager;

/**
 * The ActivityAllower is queried by the various managers when they wish to
 * start some activity on an {@link ArchivalUnit} or {@link CachedUrlSet}.
 * It keeps track of which activities are currently running, and decides
 * whether or not new activities can be permitted to start.  If so, it adjusts
 * the state to reflect the new activity.
 */
public class ActivityRegulator extends BaseLockssManager {

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

  static final int RELATION_PARENT = -1;
  static final int RELATION_SAME = 0;
  static final int RELATION_CHILD = 1;
  static final int RELATION_NONE = 99;

  private static Logger logger = Logger.getLogger("ActivityRegulator");

  HashMap cusMap = new HashMap();
  ArchivalUnit au;
  Lock curAuActivity = null;

  public ActivityRegulator(ArchivalUnit au) {
    this.au = au;
  }

  public void startService() {
    super.startService();
  }

  public void stopService() {
    logger.debug2("ActivityRegulator stopped.");
    cusMap.clear();
    super.stopService();
  }

  protected void setConfig(Configuration newConfig,
                           Configuration prevConfig,
                           Set changedKeys) {
    // nothing to config
  }

  /**
   * Tries to start a particular AU-level activity.  If it can, it sets
   * the state to indicate that the requested activity is now running.
   * @param newActivity the activity int
   * @param expireIn expire in X ms
   * @return the ActivityLock iff the activity was marked as started, else null
   */
  public synchronized Lock startAuActivity(int newActivity, long expireIn) {
    // check if the au is free for this activity
    int curActivity = getAuActivity();
    if (!isAllowedOnAu(newActivity, curActivity)) {
      // au is being acted upon
      logger.debug2("AU '" + au.getName() + "' busy with " +
                    activityCodeToString(curActivity) + ". Couldn't start " +
                    activityCodeToString(newActivity));
      return null;
    }

    logger.debug("Started " + activityCodeToString(newActivity) + " on AU '" +
                 au.getName() + "'");
    return setAuActivity(newActivity, expireIn);
  }

  /**
   * Tries to start a particular CUS-level activity.  If it can, it sets
   * the state to inidicate that the requested activity is now running.
   * @param newActivity the activity int
   * @param cus the {@link CachedUrlSet}
   * @param expireIn expire in X ms
   * @return the ActivityLock iff the activity was marked as started, else null
   */
  public synchronized Lock startCusActivity(int newActivity, CachedUrlSet cus,
                                               long expireIn) {
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
    // check if the cus is free for this activity
    int curActivity = getCusActivity(cus);
    if (!isAllowedOnCus(newActivity, curActivity, RELATION_SAME)) {
      logger.debug2("CUS '" + cus.getUrl() + "' busy with " +
                    activityCodeToString(curActivity) + ". Couldn't start " +
                    activityCodeToString(newActivity) + " on CUS '" +
                    cus.getUrl() + "'");
      return null;
    }
    if (checkForRelatedCusActivity(newActivity, cus)) {
      return null;
    }
    logger.debug("Started " + activityCodeToString(newActivity) + " on CUS '" +
                 cus.getUrl() + "'");
    return setCusActivity(cus, newActivity, expireIn);
  }

  /**
   * Alert the ActivityRegulator that a particular AU-level activity has
   * finished.
   * @param activity the activity int
   */
  public synchronized void auActivityFinished(int activity) {
    // change state to reflect
    endAuActivity(activity);
    logger.debug("Finished " + activityCodeToString(activity) + " on AU '" +
                  au.getName() + "'");
  }

  /**
   * Alert the ActivityRegulator that a particular CUS-level activity has
   * finished.
   * @param activity the activity int
   * @param cus the {@link CachedUrlSet}
   */
  public synchronized void cusActivityFinished(int activity, CachedUrlSet cus) {
    endCusActivity(activity, cus);
    logger.debug("Finished " + activityCodeToString(activity) + " on CUS '" +
                  cus.getUrl() + "'");

    boolean otherAuActivity = false;
    Iterator cusIt = cusMap.entrySet().iterator();
    while (cusIt.hasNext()) {
      // check each other cus to see if it's acting on this AU
      Map.Entry entry = (Map.Entry)cusIt.next();
      Lock value = (Lock)entry.getValue();
      if (value.isExpired()) {
        logger.debug3("Removing expired "+activityCodeToString(value.activity) +
                      " on CUS '" + entry.getKey() + "'");
        cusMap.remove(entry.getKey());
        continue;
      }
      if (value.activity!=NO_ACTIVITY) {
        otherAuActivity = true;
        break;
      }
    }
    if (!otherAuActivity) {
      // no other CUS activity on this AU, so free it up
      endAuActivity(CUS_ACTIVITY);
      logger.debug2("Finished " + activityCodeToString(CUS_ACTIVITY) +
                    " on AU '" + au.getName() + "'");
    }
  }

  boolean checkForRelatedCusActivity(int newActivity, CachedUrlSet cus) {
    String cusKey = getCusKey(cus);
    Iterator cusIt = cusMap.entrySet().iterator();
    while (cusIt.hasNext()) {
      // check each other cus to see if it's related to this one
      Map.Entry entry = (Map.Entry)cusIt.next();
      String entryKey = (String)entry.getKey();
      // need to append '/' to protect against substring matches
      // i.e. /test vs. /test2
      int relation = getRelation(entryKey, cusKey);
      if ((relation!=RELATION_NONE) &&
          (relation!=RELATION_SAME)) {
        Lock value = (Lock)entry.getValue();
        if (value.isExpired()) {
          logger.debug3("Removing expired " +
                        activityCodeToString(value.activity) +
                        " on CUS '" + entryKey + "'");
          cusMap.remove(entryKey);
          continue;
        }
        if (!isAllowedOnCus(newActivity, value.activity, relation)) {
          String relationStr = (relation==RELATION_CHILD ? "Child" : "Parent");
          logger.debug2(relationStr + " CUS busy with " +
                        activityCodeToString(value.activity) +
                        ". Couldn't start " + activityCodeToString(newActivity) +
                        " on CUS '" + cus.getUrl() + "'");
          return true;
        }
      }
    }
    return false;
  }

  static int getRelation(String key1, String key2) {
    String key1Sub = key1.substring(0, key1.lastIndexOf("::")) +
        UrlUtil.URL_PATH_SEPARATOR;
    String key2Sub = key2.substring(0, key2.lastIndexOf("::")) +
        UrlUtil.URL_PATH_SEPARATOR;
    if (key1Sub.equals(key2Sub)) {
      return RELATION_SAME;
    } else if (key1Sub.startsWith(key2Sub)) {
      return RELATION_CHILD;
    } else if (key2Sub.startsWith(key1Sub)) {
      return RELATION_PARENT;
    } else {
      return RELATION_NONE;
    }
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

//XXX fix multiple-scheduling error.
  static boolean isAllowedOnCus(int newActivity, int curActivity, int relation) {
    switch (curActivity) {
      case BACKGROUND_CRAWL:
      case REPAIR_CRAWL:
        switch (relation) {
          case RELATION_SAME:
            // only one action on a CUS at a time except for name polls
            // (resuming after repair crawl)
            return (newActivity==STANDARD_NAME_POLL);
          case RELATION_PARENT:
            // if this CUS is a parent, any action allowed
            return true;
          case RELATION_CHILD:
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL));
          default:
            return true;
        }
      case STANDARD_CONTENT_POLL:
      case STANDARD_NAME_POLL:
        switch (relation) {
          case RELATION_SAME:
            // only one action on a CUS at a time unless it's a name poll or
            // a repair crawl
            return ((newActivity==STANDARD_NAME_POLL) ||
                    (newActivity==REPAIR_CRAWL));
          case RELATION_PARENT:
            // if this CUS is a parent, allow content polls and repair crawls on
            // sub-nodes (PollManager should have blocked any truly illegal ones)
            return ((newActivity==STANDARD_CONTENT_POLL) ||
                     (newActivity==REPAIR_CRAWL));
          case RELATION_CHILD:
            // if this CUS is a child, only crawls allowed, and single node
            // content polls
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL) ||
                    (newActivity==SINGLE_NODE_CONTENT_POLL));
          default:
            return true;
        }
      case SINGLE_NODE_CONTENT_POLL:
        switch (relation) {
          case RELATION_SAME:
            // only one action on a CUS at a time unless it's a name poll
            return (newActivity==REPAIR_CRAWL);
          case RELATION_PARENT:
            // if this CUS is a parent, allow anything below
            return true;
          case RELATION_CHILD:
            // if this CUS is a child, only crawls allowed
            return ((newActivity==BACKGROUND_CRAWL) ||
                    (newActivity==REPAIR_CRAWL));
          default:
            return true;

        }
      case NO_ACTIVITY:
      default:
        return true;
    }
  }

  int getAuActivity() {
    if (curAuActivity==null)  {
      return NO_ACTIVITY;
    } else if (curAuActivity.isExpired()) {
      logger.debug3("Removing expired " +
                    activityCodeToString(curAuActivity.activity) +
                    " on AU '" + au.getName() + "'");
      curAuActivity = null;
      return NO_ACTIVITY;
    } else {
      return curAuActivity.activity;
    }
  }

  Lock setAuActivity(int activity, long expireIn) {
    curAuActivity = new Lock(activity, expireIn);
    return curAuActivity;
  }

  void endAuActivity(int activity) {
    int curActivity = getAuActivity();
    // only end if this is my activity, in case it already timed out and
    // something else started
    if (curActivity == activity) {
      curAuActivity = null;
    } else {
      logger.debug2(activityCodeToString(curActivity) + " running on AU '"+
                    au.getName() + "', so couldn't end " +
                    activityCodeToString(activity));
    }
  }

  int getCusActivity(CachedUrlSet cus) {
    Lock entry = (Lock)cusMap.get(getCusKey(cus));
    if (entry==null)  {
      return NO_ACTIVITY;
    } else if (entry.isExpired()) {
      cusMap.remove(getCusKey(cus));
      logger.debug3("Removing expired " + activityCodeToString(entry.activity) +
                    " on AU '" + cus.getUrl() + "'");
      return NO_ACTIVITY;
    } else {
      return entry.activity;
    }
  }

  Lock setCusActivity(CachedUrlSet cus, int activity, long expireIn) {
    // set CUS state
    Lock cusLock = new Lock(activity, expireIn);
    cusMap.put(getCusKey(cus), cusLock);
    // set AU state to indicate CUS activity (resets expiration time)
    curAuActivity = new Lock(CUS_ACTIVITY, expireIn);
    return cusLock;
  }

  void endCusActivity(int activity, CachedUrlSet cus) {
    int curActivity = getCusActivity(cus);
    // only end if this is my activity, in case it already timed out and
    // something else started
    if (curActivity == activity) {
      cusMap.remove(getCusKey(cus));
    } else {
      logger.debug2(activityCodeToString(curActivity) + " running on CUS '" +
                    cus.getUrl() + "', so couldn't end " +
                    activityCodeToString(activity));
    }
  }

  static String getCusKey(CachedUrlSet cus) {
    CachedUrlSetSpec spec = cus.getSpec();
    StringBuffer sb = new StringBuffer(cus.getUrl().length() * 2);
    sb.append(cus.getUrl());
    sb.append("::");
    if (spec.isRangeRestricted()) {
      RangeCachedUrlSetSpec rSpec = (RangeCachedUrlSetSpec)spec;
      sb.append(rSpec.getLowerBound());
      sb.append("-");
      sb.append(rSpec.getUpperBound());
    } else if (spec.isSingleNode()) {
      sb.append(".-.");
    }
    return sb.toString();
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

  public static class Lock {
    public int activity;
    Deadline expiration;

    public Lock(int activity, long expireIn) {
      this.activity = activity;
      expiration = Deadline.in(expireIn);
    }

    public boolean isExpired() {
      return expiration.expired();
    }
  }

  // lock is created for new activity, returned to caller
  // caller can change activity, prod to keep from expiring, ask if expired

  /**
   * Factory method to create ActivityRegulator instances.
   * @param au the {@link ArchivalUnit}
   * @return the ActivityRegulator instance for that au
   */
  public static ActivityRegulator createNewActivityRegulator(ArchivalUnit au) {
    return new ActivityRegulator(au);
  }
}
