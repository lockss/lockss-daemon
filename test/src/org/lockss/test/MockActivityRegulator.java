/*
 * $Id: MockActivityRegulator.java,v 1.5 2003-06-26 00:18:24 troberts Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;
import java.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import junit.framework.*;

public class MockActivityRegulator extends ActivityRegulator {
  boolean shouldStartAuActivity = false;
  boolean shouldStartCusActivity = false;
  int finishedAuActivity = -1;
  int finishedCusActivity = -1;
  CachedUrlSet finishedCus = null;
  Map finishedCusActivities = new HashMap();
  Set lockedCuses = new HashSet();


  public MockActivityRegulator(ArchivalUnit au) {
    super(au);
  }

  public void startService() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void stopService() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void auActivityFinished(int activity) {
    finishedAuActivity = activity;
  }

  public void assertNewContentCrawlFinished() {
    assertFinished(NEW_CONTENT_CRAWL);
  }

  public void assertRepairCrawlFinished(CachedUrlSet cus) {
    assertFinished(REPAIR_CRAWL, cus);
  }

  public void assertFinished(int activity, CachedUrlSet cus) {
    Integer finishedActivity = (Integer)finishedCusActivities.get(cus);
    if (finishedActivity == null) {
      Assert.fail("No finished activities for "+cus);
    }

    Assert.assertEquals(("Activity "
			 +ActivityRegulator.activityCodeToString(activity)
			 +" not finished on "+cus),
			activity, finishedActivity.intValue());
  }

  public void assertFinished(int activity) {
    Assert.assertEquals(("Activity "
			 +ActivityRegulator.activityCodeToString(activity)
			 +" not finished"),
			activity, finishedAuActivity);
  }

  public void cusActivityFinished(int activity, CachedUrlSet cus) {
    finishedCusActivities.put(cus, new Integer(activity));
  }

  public void setStartAuActivity(boolean shouldStartAuActivity) {
    this.shouldStartAuActivity = shouldStartAuActivity;
  }

  public Lock startAuActivity(int newActivity, long expireIn) {
    return shouldStartAuActivity ? new Lock(0, expireIn) : null;
  }

  public void setStartCusActivity(CachedUrlSet cus,
				  boolean shouldStartCusActivity) {
    if (shouldStartCusActivity) {
      lockedCuses.remove(cus);
    } else {
      lockedCuses.add(cus);
    }
  }

  public Lock startCusActivity(int newActivity, CachedUrlSet cus,
			       long expireIn) {
    return (lockedCuses.contains(cus) ? null : new Lock(0, expireIn));
  }
}
