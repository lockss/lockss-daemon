/*
 * $Id: CrawlWindowRule.java,v 1.1 2003-10-09 22:55:11 eaalto Exp $
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

/**
 * Interface for crawl windows, used to determine whether a crawl should be
 * permitted.  An individual window may either not match, or specify
 * inclusion or exclusion.  Plugins may implement this or use one of the
 * supplied implementations in {@link CrawlWindowRules}.
 */
public interface CrawlWindowRule {
  /** The date matches this rule, and should be included */
  public static final int INCLUDE = 1;
  /** The date matches this rule, and should be excluded */
  public static final int EXCLUDE = 2;
  /** The date does not match this rule */
  public static final int IGNORE = 0;

  /**
   * Returns the action to be taken, using the system time and server time zone.
   * @return the appropriate action.
   */
  public int canCrawl();

  /**
   * Returns the action to be taken, using the given date and server time zone.
   * @param serverDate the serverDate
   * @return the appropriate action.
   */
  public int canCrawl(Date serverDate);

  /**
   * Class used to match a date to a calendar window.  Examples include
   * intervals (Mon->Fri), collections (Mon, Wed, Fri), and paired windows
   * (Mon->Wed, 7-8pm).
   */
  public static abstract class Window {
    public abstract boolean isMatch(Calendar serverCal);
  }
}
