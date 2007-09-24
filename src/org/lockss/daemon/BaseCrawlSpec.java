/*
 * $Id: BaseCrawlSpec.java,v 1.9 2007-09-24 18:37:11 dshr Exp $
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
public abstract class BaseCrawlSpec implements CrawlSpec {

  protected List permissionList;
//  protected List permissionCheckers = Collections.EMPTY_LIST;
  protected PermissionChecker permissionChecker;
  protected LoginPageChecker loginPageChecker = null;
  protected CrawlRule rule;
  protected CrawlWindow window;

  /**
   * Create a BaseCrawlSpec with the specified permission list, permission
   * checker list and rule.
   * @param permissionUrls a list of urls from which permission can be obtained.
   * @param rule filter to determine which URLs encountered in the crawl
   * should themselves be crawled.  A null rule is always true.
   * @param permissionChecker a permissionChecker specified by the plugin
   * @throws IllegalArgumentException if the url list is empty.
   */
  protected BaseCrawlSpec(List permissionUrls,
			  CrawlRule rule,
			  PermissionChecker permissionChecker,
			  LoginPageChecker loginPageChecker)
      throws ClassCastException {

    if(permissionUrls.isEmpty()) {
      //do we want to throw if we have no permission urls?
      throw new IllegalArgumentException("Permission list must not be empty");
    }
    //XXX do we allow null CrawlRule ?
//     if(rule == null){
//       throw new IllegalArgumentException("CrawlRule must not be null");
//     }
    this.rule = rule;
    permissionList = ListUtil.immutableListOfType(permissionUrls, String.class);

    this.permissionChecker = permissionChecker;

    this.loginPageChecker = loginPageChecker;
  }

  /**
   * Returns the CrawlWindow, or null.
   * @return the {@link CrawlWindow}
   */
  public CrawlWindow getCrawlWindow() {
    return window;
  }

  /**
   * Sets the CrawlWindow (null for none) to determine when crawling is
   * permitted.  A null window always allows.
   * @param window the {@link CrawlWindow}
   */
  public void setCrawlWindow(CrawlWindow window) {
    this.window = window;
  }

  public List getPermissionPages() {
    return permissionList;
  }

  /**
   * Returns whether the rule is null
   * @author Rebecca Illowsky
   */
  public boolean isRuleNull(){
      return (rule == null);
  }

  /**
   * Determine whether a url is part of this CrawlSpec.
   * @param url The url to test
   * @return true iff the url matches the rule
   * @throws NullPointerException if the url is null.
   */
  public boolean isIncluded(String url) {
    return (rule == null) ? true : (rule.match(url) == CrawlRule.INCLUDE);
  }

  public boolean inCrawlWindow() {
    return (window==null) ? true : window.canCrawl();
  }

  public PermissionChecker getPermissionChecker() {
    return permissionChecker;
  }

  public LoginPageChecker getLoginPageChecker() {
    return loginPageChecker;
  }

  public String getExploderPattern() {
    return null;
  }

  // XXX temporary
  public void setExploderPattern(String pat) {
  }

  public ExploderHelper getExploderHelper() {
    return null;
  }

  // XXX temporary
  public void setExploderHelper(ExploderHelper eh) {
  }

}


