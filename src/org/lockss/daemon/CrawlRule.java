/*
 * $Id$
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

/**
 * Interface for crawl rules, used to determine whether a URL should be
 * followed during a crawl.  An individual rule may either not match,
 * or specify
 * inclusion or exclusion.  Plugins may implement this or use one of the
 * supplied implementations in {@link CrawlRules}.
 */
public interface CrawlRule {
  /** The URL matches this rule, and should be included */
  public static final int INCLUDE = 1;
  /** The URL matches this rule, and should be excluded */
  public static final int EXCLUDE = 2;
  /** The URL does not match this rule */
  public static final int IGNORE = 0;

  /**
   * Match a URL against the rule.
   * @param url String to check against this rule
   * @return <code>CrawlRule.INCLUDE</code> if the url should be included,
   *      <code>CrawlRule.EXCLUDE</code> if the url should be excluded, or
   *      <code>CrawlRule.IGNORE</code> if the url does not match this rule.
   */
  public int match(String url);
}
