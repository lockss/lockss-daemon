/*
 * $Id: AuStateBean.java,v 1.22 2007-10-04 04:06:17 tlipkis Exp $
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


package org.lockss.state;

import java.util.HashSet;

/**
 * AuStateBean is a settable version of AuState used purely for
 * marshalling purposes.  Notice there is no need to marshal the treewalk time,
 * because any time the server is restarted a treewalk should be run.
 */
public class AuStateBean extends AuState {
  /**
   * Simple constructor to allow bean creation during unmarshalling.
   */
  public AuStateBean() {
    super(null, null);
  }

  /**
   * Constructor to create the bean from an AuState prior to marshalling.
   * @param auState the AuState
   */
  AuStateBean(AuState auState) {super(auState.au, auState.lastCrawlTime,
				      auState.lastCrawlAttempt,
				      auState.lastCrawlResult,
				      auState.lastCrawlResultMsg,
				      auState.lastTopLevelPoll,
				      auState.lastTreeWalk, auState.crawlUrls,
				      auState.clockssSubscriptionStatus,
				      auState.v3Agreement, null);
  }

  /**
   * Sets the crawl urls
   * @param newCol a new collection of Strings
   */
  public void setCrawlUrls(HashSet newCol) {
    crawlUrls = newCol;
  }

  /**
   * Sets the last crawl time to a new value.
   * @param newCrawlTime in ms
   */
  public void setLastCrawlTime(long newCrawlTime) {
    lastCrawlTime = newCrawlTime;
  }

  /**
   * Sets the last top level poll time to a new value.
   * @param newPollTime in ms
   */
  public void setLastTopLevelPollTime(long newPollTime) {
    lastTopLevelPoll = newPollTime;
  }

  /**
   * Sets the CLOCKSS subscription status to a new value.
   * @param newSubscriptionStatus
   */
  public void setClockssSubscriptionStatus(int newSubscriptionStatus) {
    clockssSubscriptionStatus = newSubscriptionStatus;
  }
}
