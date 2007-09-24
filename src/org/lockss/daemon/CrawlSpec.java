/*
 * $Id: CrawlSpec.java,v 1.20 2007-09-24 18:37:11 dshr Exp $
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

package org.lockss.daemon;

import java.util.*;

/**
 * This interface is implemented by BaseCrawlSpec.
 * Specification for a crawl:
 * a list of starting URLs or an OaiHandler Url (for Oai Crawl) and
 * a rule that determines whether a candidate URL should be included in the crawl.
 */
public interface CrawlSpec {

  /**
   * Returns the CrawlWindow, or null.
   * @return the {@link CrawlWindow}
   */
  public CrawlWindow getCrawlWindow();

  /**
   * Sets the CrawlWindow (null for none) to determine when crawling is
   * permitted.  A null window always allows.
   * @param window the {@link CrawlWindow}
   */
  public void setCrawlWindow(CrawlWindow window);

  /**
   * Gets the permission pages list
   * @return a list of permission pages URLS, as Strings
   */
  public List getPermissionPages();

  /**
   * Returns whether the rule is null
   * @author Rebecca Illowsky
   */
    public boolean isRuleNull();

  /**
   * Determine whether a url is part of this CrawlSpec.
   * @param url The url to test
   * @return true iff the url matches the rule
   * @throws NullPointerException if the url is null.
   */
  public boolean isIncluded(String url);

  /**
   * Checks the crawlWindow to see if it is a good time to crawl
   * @return true iff the crawl time falls into the crawl window
   */
  public boolean inCrawlWindow();

  /**
   * Gets the list of permission checkers
   * @return a list of permission checkers
   */
  public PermissionChecker getPermissionChecker();

  /**
   * Gets the login page checker
   * @return the LoginPageChecker for this crawl, or null if there isn't one
   */
  public LoginPageChecker getLoginPageChecker();

  /**
   * Gets the pattern to recognize archive files
   * @return pattern to recognize archive files to be exploded
   */
  public String getExploderPattern();

  // XXX temporary
  public void setExploderPattern(String pat);

  /**
   * Gets the ExploderHelper that provides publisher-specific info
   * for the process of exploding archives.
   * @return instance of ExploderHelper
   */
  public ExploderHelper getExploderHelper();

  // XXX temporary
  public void setExploderHelper(ExploderHelper eh);

}


