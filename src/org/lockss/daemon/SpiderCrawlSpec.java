/*
 * $Id: SpiderCrawlSpec.java,v 1.6 2007-03-17 21:31:31 dshr Exp $
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

package org.lockss.daemon;

import java.util.*;
import org.lockss.util.*;

/**
 * Specification for a crawl: a list of starting URLs and a rule
 * that determines whether a candidate URL should be included in the crawl.
 */
public final class SpiderCrawlSpec extends BaseCrawlSpec {

  private List startList;
  private int refetchDepth = -1;
  private String arcPattern = null;

  /**
   * Create a SpiderCrawlSpec with the specified start list and rule.
   * @param startUrls a list of Strings specifying starting points
   * for the crawl
   * @param permissionUrls a list of urls from which permission can be obtained.
   * @param rule filter to determine which URLs encountered in the crawl
   * should themselves be crawled.  A null rule is always true.
   * @param refetchDepth depth to always refetch
   * @throws IllegalArgumentException if the url list is empty.
   * @throws NullPointerException if any elements of startUrls is null.
   * @throws ClassCastException if any elements of startUrls is not a String.
   */
  public SpiderCrawlSpec(List startUrls,
		       List permissionUrls,
		       CrawlRule rule,
		       int refetchDepth)
      throws ClassCastException {
    this(startUrls, permissionUrls, rule, refetchDepth,
         null, null);
  }

  /**
   * Create a SpiderCrawlSpec with the specified start list and rule.
   * @param startUrls a list of Strings specifying starting points
   * for the crawl
   * @param permissionUrls a list of urls from which permission can be obtained.
   * @param rule filter to determine which URLs encountered in the crawl
   * should themselves be crawled.  A null rule is always true.
   * @param refetchDepth depth to always refetch
   * @param permissionChecker a permissionChecker specified by plugin
   * @throws IllegalArgumentException if the url list is empty.
   * @throws NullPointerException if any elements of startUrls is null.
   * @throws ClassCastException if any elements of startUrls is not a String.
   */
  public SpiderCrawlSpec(List startUrls,
			 List permissionUrls,
			 CrawlRule rule,
			 int refetchDepth,
			 PermissionChecker permissionChecker,
			 LoginPageChecker loginPageChecker)
      throws ClassCastException {
    this(startUrls, permissionUrls, rule, refetchDepth, permissionChecker,
	 loginPageChecker, null);
  }

  /**
   * Create a SpiderCrawlSpec with the specified start list and rule.
   * @param startUrls a list of Strings specifying starting points
   * for the crawl
   * @param permissionUrls a list of urls from which permission can be obtained.
   * @param rule filter to determine which URLs encountered in the crawl
   * should themselves be crawled.  A null rule is always true.
   * @param refetchDepth depth to always refetch
   * @param permissionChecker a permissionChecker specified by plugin
   * @param arcPattern regexp to recognize ARC files
   * @throws IllegalArgumentException if the url list is empty.
   * @throws NullPointerException if any elements of startUrls is null.
   * @throws ClassCastException if any elements of startUrls is not a String.
   */
  public SpiderCrawlSpec(List startUrls,
			 List permissionUrls,
			 CrawlRule rule,
			 int refetchDepth,
			 PermissionChecker permissionChecker,
			 LoginPageChecker loginPageChecker,
			 String arcPattern)
      throws ClassCastException {
    super(permissionUrls, rule, permissionChecker, loginPageChecker);
    if(startUrls.isEmpty()) {
      throw new
          IllegalArgumentException("CrawlSpec starting url must not be empty");
    }

    if(refetchDepth < 1) {
      throw new IllegalArgumentException("Refetch depth must be at least 1");
    }
    startList = ListUtil.immutableListOfType(startUrls, String.class);
    this.refetchDepth = refetchDepth;
    this.arcPattern = arcPattern;
  }

  /**
   * Create a SpiderCrawlSpec with the specified start list and rule.  Defaults to
   * refetchDepth of 1.
   * @param startUrls a list of Strings specifying starting points
   * for the crawl
   * @param rule filter to determine which URLs encountered in the crawl
   * should themselves be crawled.  A null rule is always true.
   * @throws IllegalArgumentException if the url list is empty.
   * @throws NullPointerException if any elements of startUrls is null.
   * @throws ClassCastException if any elements of startUrls is not a String.
   */
  public SpiderCrawlSpec(List startUrls, CrawlRule rule) throws ClassCastException {
    this(startUrls, startUrls, rule, 1);
  }

  /**
   * Create a SpiderCrawlSpec with the specified single start url and rule.
   * Defaults to recrawl depth of 1.
   * @param url specifies the starting point for the crawl
   * @param rule filter to determine which URLs encountered in the crawl
   * should themselves be crawled.  A null rule is always true.
   * @throws NullPointerException if the url is null.
   */
  public SpiderCrawlSpec(String url, CrawlRule rule) {
    this(ListUtil.list(url), rule);
  }

  /**
   * Create a SpiderCrawlSpec with the specified single start url and rule.
   * @param url specifies the starting point for the crawl
   * @param rule filter to determine which URLs encountered in the crawl
   * should themselves be crawled.  A null rule is always true.
   * @param refetchDepth depth to always refetch
   * @throws NullPointerException if the url is null.
   */
  public SpiderCrawlSpec(String url, CrawlRule rule, int refetchDepth) {
    this(ListUtil.list(url), ListUtil.list(url), rule, refetchDepth);
  }

  /**
   * Get the starting point list.
   * @return an immutable list of URLs, as Strings
   */
  public List getStartingUrls() {
    return startList;
  }

  /**
   * @return depth to recrawl when doing a new content crawl.
   * 1 means just the starting urls, 2 is all of them and everything
   * they link directly to, etc.
   */
  public int getRefetchDepth() {
    return refetchDepth;
  }

  /**
   * @return pattern to recognize ARC files
   */
  public String arcFilePattern() {
    return arcPattern;
  }

}


