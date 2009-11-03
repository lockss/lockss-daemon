/*
 * $Id: NewContentCrawler.java,v 1.57.12.1 2009-11-03 23:44:51 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.util.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.state.*;

public class NewContentCrawler extends FollowLinkCrawler {

  private static Logger logger = Logger.getLogger("NewContentCrawler");

  private SpiderCrawlSpec spec;
  private int refetchDepth = -1;

  public NewContentCrawler(ArchivalUnit au, CrawlSpec crawlSpec, AuState aus) {
    super(au, crawlSpec, aus);
    spec = (SpiderCrawlSpec) crawlSpec;
    crawlStatus = new CrawlerStatus(au, spec.getStartingUrls(),
                                    getTypeString());
  }

  public int getType() {
    return Crawler.NEW_CONTENT;
  }

  public String getTypeString() {
    return "New Content";
  }

  public boolean isWholeAU() {
    return true;
  }

  protected int getRefetchDepth() {
    if (refetchDepth == -1) {
      int refetchDepth0 = spec.getRefetchDepth();
      String key = StringUtil.replaceString(PARAM_REFETCH_DEPTH,
                                            "<auid>", au.getAuId());
      refetchDepth = CurrentConfig.getIntParam(key, refetchDepth0);
      if (refetchDepth != refetchDepth0) {
        logger.info("Crawl spec refetch depth (" + refetchDepth0 +
                    ") overridden by parameter (" + refetchDepth + ")");
      }
    }
    return refetchDepth;
  }

  /**
   * Return start URLs from crawl spec
   */
  protected Collection<String> getUrlsToFollow(){
    return spec.getStartingUrls();
  }

  protected boolean shouldFollowLink(){
    return true;
  }

}
