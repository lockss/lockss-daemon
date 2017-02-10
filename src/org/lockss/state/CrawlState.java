/*
 * $Id$
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

package org.lockss.state;

import org.lockss.util.LockssSerializable;

/**
 * CrawlState contains the crawl-related state information for a node.
 */
public class CrawlState implements LockssSerializable {
  public static final int NEW_CONTENT_CRAWL = 1;
  public static final int REPAIR_CRAWL = 2;
  public static final int BACKGROUND_CRAWL = 4;
  public static final int NODE_DELETED = 8;

  public static final int SCHEDULED = 1;
  public static final int RUNNING = 2;
  public static final int FINISHED = 4;

  int type;
  int status;
  long startTime;

  /**
   * Simple constructor to allow bean creation during unmarshalling.  Not used
   * by CrawlState, but by it's sub-class, CrawlStateBean.
   */
  public CrawlState() { }

  /**
   * Constructor to create CrawlState from a CrawlStateBean after unmarshalling.
   * @param bean CrawlStateBean the bean
   */
  CrawlState(CrawlStateBean bean) {
    this.type = bean.getType();
    this.status = bean.getStatus();
    this.startTime = bean.getStartTime();
  }

  /**
   * Constructor to create a standard CrawlState.
   * @param type the type
   * @param status the status
   * @param startTime the start time
   */
  public CrawlState(int type, int status, long startTime) {
    this.type = type;
    this.status = status;
    this.startTime = startTime;
  }
  /**
   * Returns the crawl type.
   * @return an int representing the type
   */
  public int getType() {
    return type;
  }

  /**
   * Return the crawl type as a string
   * @return a String representing the type
   */
  public String getTypeString() {
    String str;

    switch (type) {
      case NEW_CONTENT_CRAWL:
        str = "New Content";
        break;
      case REPAIR_CRAWL:
        str = "Repair";
        break;
      case BACKGROUND_CRAWL:
        str = "Background";
        break;
      case NODE_DELETED:
        str = "Node Deleted";
        break;
      default:
        str = "None";
    }
    return str;
  }

  /**
   * Returns the status of the crawl.
   * @return an int representing the current status
   */
  public int getStatus() {
    return status;
  }

  /**
   * return the status of the crawl as a string.
   * @return a String representing the current status
   */
  public String getStatusString() {
    String str;

    switch (status) {
      case SCHEDULED:
        str = "scheduled";
        break;
      case RUNNING:
        str = "running";
        break;
      case FINISHED:
        str = "finished";
        break;
      default:
        str = "Undefined";
    }
    return str;
  }

  /**
   * Returns the start time of the crawl.
   * @return the start time in ms
   */
  public long getStartTime() {
    return startTime;
  }

}
