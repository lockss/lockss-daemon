/*
 * $Id: ActivityRegulator.java,v 1.1 2003-04-12 01:14:29 aalto Exp $
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

  private static Logger logger = Logger.getLogger("ActivityRegulator");

  HashMap auMap = new HashMap();
  HashMap cusMap = new HashMap();

  public ActivityRegulator() {
    logger.debug2("ActivityRegulator created.");
  }

  /**
   * Tries to start a particular AU-level activity.  If it can, it sets
   * the state to indicate that the requested activity is now running.
   * @param activity the activity int
   * @param au the {@link ArchivalUnit}
   * @return true iff the activity was marked as started
   */
  public synchronized boolean startAuActivity(int activity, ArchivalUnit au) {
    // check if the au is free for this activity
    int auActivity = getAuActivity(au);
    if (auActivity!=NO_ACTIVITY) {
      // au is being acted upon
      logger.debug3("AU '" + au.getName() + "' busy with " +
                    activityCodeToString(auActivity) + ". Couldn't start " +
                    activityCodeToString(activity));
      return false;
    }

    setAuActivity(au, activity);
    logger.debug3("Started " + activityCodeToString(activity) + " on AU '" +
                  au.getName() + "'");
    return true;
  }

  /**
   * Tries to start a particular CUS-level activity.  If it can, it sets
   * the state to inidicate that the requested activity is now running.
   * @param activity the activity int
   * @param cus the {@link CachedUrlSet}
   * @return true iff the activity was marked as started
   */
  public synchronized boolean startCusActivity(int activity, CachedUrlSet cus) {
    // first, check if au is busy
    int auActivity = getAuActivity(cus.getArchivalUnit());
    if ((auActivity!=NO_ACTIVITY) && (auActivity!=CUS_ACTIVITY)) {
      // au is being acted upon at the AU level
      logger.debug3("AU '" + cus.getArchivalUnit().getName() + "' busy with " +
                    activityCodeToString(auActivity) + ". Couldn't start " +
                    activityCodeToString(activity) + " on CUS '" +
                    cus.getUrl() + "'");
      return false;
    }
    // check if the cus is free for this activity
    int cusActivity = getCusActivity(cus);
    if (cusActivity!=NO_ACTIVITY) {
      logger.debug3("CUS '" + cus.getUrl() + "' busy with " +
                    activityCodeToString(cusActivity) + ". Couldn't start " +
                    activityCodeToString(activity) + " on CUS '" +
                    cus.getUrl() + "'");
      return false;
    }
    setCusActivity(cus, activity);
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
    endAuActivity(au);
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
    endCusActivity(cus);
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
        Integer value = (Integer)entry.getValue();
        if (value.intValue()!=NO_ACTIVITY) {
          otherAuActivity = true;
          break;
        }
      }
    }
    if (!otherAuActivity) {
      // no other CUS activity on this AU, so free it up
      endAuActivity(cus.getArchivalUnit());
      logger.debug3("Finished " + activityCodeToString(CUS_ACTIVITY) +
                    " on AU '" + cus.getArchivalUnit().getName() + "'");
    }
  }

  int getAuActivity(ArchivalUnit au) {
    Integer curActivity = (Integer)auMap.get(getAuKey(au));
    if (curActivity==null) {
      return NO_ACTIVITY;
    } else {
      return curActivity.intValue();
    }
  }

  void setAuActivity(ArchivalUnit au, int activity) {
    auMap.put(getAuKey(au), new Integer(activity));
  }

  void endAuActivity(ArchivalUnit au) {
    auMap.remove(getAuKey(au));
  }

  int getCusActivity(CachedUrlSet cus) {
    Integer curActivity = (Integer)cusMap.get(getCusKey(cus));
    if (curActivity==null) {
      return NO_ACTIVITY;
    } else {
      return curActivity.intValue();
    }
  }

  void setCusActivity(CachedUrlSet cus, int activity) {
    // set CUS state
    cusMap.put(getCusKey(cus), new Integer(activity));
    // set AU state to indicate CUS activity
    auMap.put(getAuKey(cus.getArchivalUnit()), new Integer(CUS_ACTIVITY));
  }

  void endCusActivity(CachedUrlSet cus) {
    cusMap.remove(getCusKey(cus));
  }

  static String getAuPrefix(CachedUrlSet cus) {
    return getAuKey(cus.getArchivalUnit()) + "::";
  }

  static String getAuKey(ArchivalUnit au) {
    return au.getGloballyUniqueId();
  }

  static String getCusKey(CachedUrlSet cus) {
    return getAuPrefix(cus) + cus.getUrl();
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

}
