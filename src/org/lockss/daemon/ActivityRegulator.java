/*
 * $Id: ActivityRegulator.java,v 1.3 2003-04-16 02:23:48 aalto Exp $
 */

/*

Copyright (c) 2001-2002 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.lockss.util.Logger;
import org.lockss.util.Deadline;
import org.lockss.util.Constants;

/**
 * The ActivityAllower is queried by the various managers when they wish to
 * start some activity on an {@link ArchivalUnit} or {@link CachedUrlSet}.
 * It keeps track of which activities are currently running, and decides
 * whether or not new activities can be permitted to start.  If so, it adjusts
 * the state to reflect the new activity.
 */
public class ActivityRegulator {

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
   * Integer representing the standard poll activity.  CUS level.
   */
  public static final int STANDARD_POLL = 13;

  /**
   * Integer representing no activity. AU or CUS level.
   */
  static final int NO_ACTIVITY = -1;

  static final int RELATION_PARENT = -1;
  static final int RELATION_SAME = 0;
  static final int RELATION_CHILD = 1;
  static final int RELATION_NONE = 99;

  private static Logger logger = Logger.getLogger("ActivityRegulator");

  HashMap auMap = new HashMap();
  HashMap cusMap = new HashMap();

  public ActivityRegulator() {
    logger.debug2("ActivityRegulator created.");
  }

  public void freeAllLocks() {
    auMap = new HashMap();
    cusMap = new HashMap();
  }

  /**
   * Tries to start a particular AU-level activity.  If it can, it sets
   * the state to indicate that the requested activity is now running.
   * @param activity the activity int
   * @param au the {@link ArchivalUnit}
   * @param expireIn expire in X ms
   * @return true iff the activity was marked as started
   */
  public synchronized boolean startAuActivity(int activity, ArchivalUnit au,
                                              long expireIn) {
    // check if the au is free for this activity
    int auActivity = getAuActivity(au);
    if (!isAllowedOnAu(activity, auActivity)) {
      // au is being acted upon
      logger.debug3("AU '" + au.getName() + "' busy with " +
                    activityCodeToString(auActivity) + ". Couldn't start " +
                    activityCodeToString(activity));
      return false;
    }

    setAuActivity(au, activity, expireIn);
    logger.debug3("Started " + activityCodeToString(activity) + " on AU '" +
                  au.getName() + "'");
    return true;
  }

  /**
   * Tries to start a particular CUS-level activity.  If it can, it sets
   * the state to inidicate that the requested activity is now running.
   * @param activity the activity int
   * @param cus the {@link CachedUrlSet}
   * @param expireIn expire in X ms
   * @return true iff the activity was marked as started
   */
  public synchronized boolean startCusActivity(int activity, CachedUrlSet cus,
                                               long expireIn) {
    // first, check if au is busy
    int auActivity = getAuActivity(cus.getArchivalUnit());
    if (!isAllowedOnAu(activity, auActivity)) {
      // au is being acted upon at the AU level
      logger.debug3("AU '" + cus.getArchivalUnit().getName() + "' busy with " +
                    activityCodeToString(auActivity) + ". Couldn't start " +
                    activityCodeToString(activity) + " on CUS '" +
                    cus.getUrl() + "'");
      return false;
    }
    // check if the cus is free for this activity
    int cusActivity = getCusActivity(cus);
    if (!isAllowedOnCus(activity, cusActivity, RELATION_SAME)) {
      logger.debug3("CUS '" + cus.getUrl() + "' busy with " +
                    activityCodeToString(cusActivity) + ". Couldn't start " +
                    activityCodeToString(activity) + " on CUS '" +
                    cus.getUrl() + "'");
      return false;
    }
    if (checkForRelatedCusActivity(activity, cus)) {
      return false;
    }
    setCusActivity(cus, activity, expireIn);
    logger.debug3("Started " + activityCodeToString(activity) + " on CUS '" +
                  cus.getUrl() + "'");
    return true;
  }

  /**
   * Alert the ActivityRegulator that a particular AU-level activity has
   * finished.
   * @param activity the activity int
   * @param au the {@link ArchivalUnit}
   */
  public synchronized void auActivityFinished(int activity, ArchivalUnit au) {
    // change state to reflect
    endAuActivity(activity, au);
    logger.debug3("Finished " + activityCodeToString(activity) + " on AU '" +
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
    logger.debug3("Finished " + activityCodeToString(activity) + " on CUS '" +
                  cus.getUrl() + "'");

    boolean otherAuActivity = false;
    String auKeyPrefix = getAuPrefix(cus);
    Iterator cusIt = cusMap.entrySet().iterator();
    while (cusIt.hasNext()) {
      // check each other cus to see if it's acting on this AU
      Map.Entry entry = (Map.Entry)cusIt.next();
      String key = (String)entry.getKey();
      if (key.startsWith(auKeyPrefix)) {
        ActivityEntry value = (ActivityEntry)entry.getValue();
        if (value.isExpired()) {
          logger.debug3("Removing expired "+activityCodeToString(value.activity) +
                        " on CUS '" + key + "'");
          cusMap.remove(key);
          continue;
        }
        if (value.activity!=NO_ACTIVITY) {
          otherAuActivity = true;
          break;
        }
      }
    }
    if (!otherAuActivity) {
      // no other CUS activity on this AU, so free it up
      endAuActivity(CUS_ACTIVITY, cus.getArchivalUnit());
      logger.debug3("Finished " + activityCodeToString(CUS_ACTIVITY) +
                    " on AU '" + cus.getArchivalUnit().getName() + "'");
    }
  }

  boolean checkForRelatedCusActivity(int activity, CachedUrlSet cus) {
    String cusKey = getCusKey(cus);
    Iterator cusIt = cusMap.entrySet().iterator();
    while (cusIt.hasNext()) {
      // check each other cus to see if it's related to this one
      Map.Entry entry = (Map.Entry)cusIt.next();
      String entryKey = (String)entry.getKey();
      // need to append '/' to protect against substring matches
      // i.e. /test vs. /test2
      int relation = getRelation(entryKey, cusKey);
      if (relation!=RELATION_NONE) {
        ActivityEntry value = (ActivityEntry)entry.getValue();
        if (value.isExpired()) {
          logger.debug3("Removing expired " +
                        activityCodeToString(value.activity) +
                        " on CUS '" + entryKey + "'");
          cusMap.remove(entryKey);
          continue;
        }
        if (!isAllowedOnCus(activity, value.activity, relation)) {
          String relationStr = (relation==RELATION_CHILD ? "Child" : "Parent");
          logger.debug3(relationStr + " CUS '" + cus.getUrl() + "' busy with " +
                        activityCodeToString(value.activity) +
                        ". Couldn't start " + activityCodeToString(activity) +
                        " on CUS '" + cus.getUrl() + "'");
          return true;
        }
      }
    }
    return false;
  }

  static int getRelation(String key1, String key2) {
    String key1Sub = key1.substring(0, key1.lastIndexOf("::")) + "/";
    String key2Sub = key2.substring(0, key2.lastIndexOf("::")) + "/";
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

  static boolean isAllowedOnAu(int newActivity, int auActivity) {
    switch (auActivity) {
      case NEW_CONTENT_CRAWL:
      case TOP_LEVEL_POLL:
      case TREEWALK:
        // no new activity allowed
        return false;
      case CUS_ACTIVITY:
        // only other CUS activity is allowed while CUS activity is going on
        return (isCusActivity(newActivity));
      case NO_ACTIVITY:
      default:
        return true;
    }
  }

  static boolean isAllowedOnCus(int newActivity, int cusActivity, int relation) {
    switch (cusActivity) {
      case BACKGROUND_CRAWL:
      case REPAIR_CRAWL:
        if (relation==RELATION_SAME) {
          // only one action on a CUS at a time
          return false;
        } else if (relation==RELATION_PARENT) {
          // if this CUS is a parent, any action allowed
          return true;
        } else {
          // if this CUS is a child, only crawls allowed
          return ((newActivity==BACKGROUND_CRAWL) ||
                  (newActivity==REPAIR_CRAWL));
        }
      case STANDARD_POLL:
        if (relation==RELATION_SAME) {
          // only one action on a CUS at a time unless it's another poll and
          // the ranges are different
          if (newActivity==STANDARD_POLL) {

          }
          return false;
        } else if (relation==RELATION_PARENT) {
          // if this CUS is a parent, no action allowed
          return false;
        } else {
          // if this CUS is a child, only crawls allowed
          return ((newActivity==BACKGROUND_CRAWL) ||
                  (newActivity==REPAIR_CRAWL));
        }
      case NO_ACTIVITY:
      default:
        return true;
    }
  }

  int getAuActivity(ArchivalUnit au) {
    ActivityEntry entry = (ActivityEntry)auMap.get(getAuKey(au));
    if (entry==null)  {
      return NO_ACTIVITY;
    } else if (entry.isExpired()) {
      auMap.remove(getAuKey(au));
      logger.debug3("Removing expired " + activityCodeToString(entry.activity) +
                    " on AU '" + au.getName() + "'");
      return NO_ACTIVITY;
    } else {
      return entry.activity;
    }
  }

  void setAuActivity(ArchivalUnit au, int activity, long expireIn) {
    auMap.put(getAuKey(au), new ActivityEntry(activity, expireIn));
  }

  void endAuActivity(int activity, ArchivalUnit au) {
    int curActivity = getAuActivity(au);
    // only end if this is my activity, in case it already timed out and
    // something else started
    if (curActivity == activity) {
      auMap.remove(getAuKey(au));
    } else {
      logger.debug3(activityCodeToString(curActivity) + " running on AU '"+
                    au.getName() + "', so couldn't end " +
                    activityCodeToString(activity));
    }
  }

  int getCusActivity(CachedUrlSet cus) {
    ActivityEntry entry = (ActivityEntry)cusMap.get(getCusKey(cus));
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

  void setCusActivity(CachedUrlSet cus, int activity, long expireIn) {
    // set CUS state
    cusMap.put(getCusKey(cus), new ActivityEntry(activity, expireIn));
    // set AU state to indicate CUS activity (resets expiration time)
    auMap.put(getAuKey(cus.getArchivalUnit()),
              new ActivityEntry(CUS_ACTIVITY, expireIn));
  }

  void endCusActivity(int activity, CachedUrlSet cus) {
    int curActivity = getCusActivity(cus);
    // only end if this is my activity, in case it already timed out and
    // something else started
    if (curActivity == activity) {
      cusMap.remove(getCusKey(cus));
    } else {
      logger.debug3(activityCodeToString(curActivity) + " running on CUS '" +
                    cus.getUrl() + "', so couldn't end " +
                    activityCodeToString(activity));
    }
  }

  static String getAuPrefix(CachedUrlSet cus) {
    return getAuKey(cus.getArchivalUnit()) + "::";
  }

  static String getAuKey(ArchivalUnit au) {
    return au.getGloballyUniqueId();
  }

  static String getCusKey(CachedUrlSet cus) {
    String key = getAuPrefix(cus) + cus.getUrl() + "::";
    if (cus.getSpec() instanceof RangeCachedUrlSetSpec) {
      RangeCachedUrlSetSpec rSpec = (RangeCachedUrlSetSpec)cus.getSpec();
      if ((rSpec.getLowerBound()!=null) || (rSpec.getUpperBound()!=null)) {
        key += rSpec.getLowerBound() + "-" + rSpec.getUpperBound();
      }
    }
    return key;
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
      case STANDARD_POLL:
        return "Standard Poll";
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
      case STANDARD_POLL:
      case NO_ACTIVITY:
        return true;
      default:
        return false;
    }
  }

  static class ActivityEntry {
    int activity;
    Deadline expiration;

    ActivityEntry(int activity, long expireIn) {
      this.activity = activity;
      expiration = Deadline.in(expireIn);
    }

    boolean isExpired() {
      return expiration.expired();
    }
  }

}
