/*
 * $Id: NodeStateBean.java,v 1.1 2003-03-15 02:45:28 aalto Exp $
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


package org.lockss.state;

import java.util.*;
import org.lockss.plugin.CachedUrlSet;

/**
 * NodeState contains the current state information for a node, as well as the
 * poll histories.
 */
public class NodeStateBean {
  CrawlStateBean crawlBean;
  List pollBeans = new ArrayList();

  public NodeStateBean() { }

  NodeStateBean(NodeState nodeState) {
    this.crawlBean = new CrawlStateBean(nodeState.getCrawlState());
    Iterator polls = nodeState.getActivePolls();
    while (polls.hasNext()) {
      PollState poll = (PollState)polls.next();
      pollBeans.add(new PollStateBean(poll));
    }
  }

  /**
   * Returns the CrawlStateBean.
   * @return the {@link CrawlStateBean}
   */
  public CrawlStateBean getCrawlStateBean() {
    return crawlBean;
  }

  /**
   * Sets the CrawlStateBean
   * @param newBean the new {@link CrawlStateBean}
   */
  public void setCrawlStateBean(CrawlStateBean newBean) {
    this.crawlBean = newBean;
  }

  /**
   * Returns a list of PollStateBeans.
   * @return a list of {@link PollStateBean} objects
   */
  public List getPollBeans() {
    return pollBeans;
  }

  /**
   * Sets the list of PollStateBeans.
   * @param newList a new list of {@link PollStateBean} objects
   */
  public void setPollBeans(List newList) {
    pollBeans = newList;
  }

}