/*
 * $Id: CrawlWindowRules.java,v 1.1 2003-10-09 22:55:11 eaalto Exp $
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
 * Several useful CrawlWindowRule implementations.
 */
public class CrawlWindowRules {

  public static class BaseCrawlWindow implements CrawlWindowRule {
    protected CrawlWindowRule.Window window;
    protected int action;
    protected TimeZone timeZone;

    /** Include if match, else ignore */
    public static final int MATCH_INCLUDE = 1;
    /** Exclude if match, else ignore */
    public static final int MATCH_EXCLUDE = 2;
    /** Include if no match, else ignore */
    public static final int NO_MATCH_INCLUDE = 3;
    /** Exclude if no match, else ignore */
    public static final int NO_MATCH_EXCLUDE = 4;
    /** Include if match, else exclude */
    public static final int MATCH_INCLUDE_ELSE_EXCLUDE = 5;
    /** Exclude if match, else include */
    public static final int MATCH_EXCLUDE_ELSE_INCLUDE = 6;

    /**
     * @param window the Window
     * @param action one of the constants above.
     * @param serverTimeZone the server time zone
     */
    public BaseCrawlWindow(CrawlWindowRule.Window window, int action,
                           TimeZone serverTimeZone) {
      if (window == null) {
        throw new NullPointerException("CrawlWindowRules.BaseCrawlWindow with null Window");
      }
      this.window = window;
      this.action = action;
      this.timeZone = serverTimeZone;
    }

    public void setServerTimeZone(TimeZone serverTimeZone) {
      this.timeZone = serverTimeZone;
    }

    public int canCrawl() {
      return canCrawl(TimeBase.nowDate());
    }

    /**
     * Determine whether the date is included, excluded or ignored by this rule
     * @param serverDate date to check.
     * @return MATCH_INCLUDE if the URL should be fetched, MATCH_EXCLUDE if
     * if shouldn't be fetched, or MATCH_IGNORE if this rule is agnostic
     * about the URL.
     */
    public int canCrawl(Date serverDate) {
      Calendar serverCal;
      if (timeZone!=null) {
        serverCal = Calendar.getInstance(timeZone);
      } else {
        serverCal = Calendar.getInstance();
      }
      // set to the date to test
      serverCal.setTime(serverDate);
      boolean match = window.isMatch(serverCal);
      switch (action) {
        case MATCH_INCLUDE:
          return (match ? INCLUDE : IGNORE);
        case MATCH_EXCLUDE:
          return (match ? EXCLUDE : IGNORE);
        case NO_MATCH_INCLUDE:
          return (!match ? INCLUDE : IGNORE);
        case NO_MATCH_EXCLUDE:
          return (!match ? EXCLUDE : IGNORE);
        case MATCH_INCLUDE_ELSE_EXCLUDE:
          return (match ? INCLUDE : EXCLUDE);
        case MATCH_EXCLUDE_ELSE_INCLUDE:
          return (!match ? INCLUDE : EXCLUDE);
      }
      return IGNORE;
    }

    public String toString() {
      return "[CrawlWindowRules.BaseCrawlWindow: " + window + "]";
    }
  }

  /**
   * CrawlWindowRules.WindowSet matches against a list of
   * {@link CrawlWindowRule}s and returns <code>CrawlWindowRule.EXCLUDE</code>
   * if there are any exclusions, otherwise <code>CrawlWindowRule.INCLUDE</code>
   * if a window matches, or
   * <code>CrawlWindowRule.IGNORE</code> if none match.
   */
  public static class WindowSet implements CrawlWindowRule {
    private Set windows;

    /**
     * Create a window that matches against the given list of windows
     * @param windows list of {@link CrawlWindow}s
     * @throws NullPointerException if the list is null.
     */
    public WindowSet(Set windows) {
      if (windows == null) {
        throw new NullPointerException("CrawlWindowRules.WindowSet with null set");
      }
      this.windows = SetUtil.immutableSetOfType(windows, CrawlWindowRule.class);
    }

    public int canCrawl() {
      return canCrawl(TimeBase.nowDate());
    }

    public int canCrawl(Date serverDate) {
      boolean foundMatch = false;
      Iterator iter = windows.iterator();
      while (iter.hasNext()) {
        int match = ((CrawlWindowRule)iter.next()).canCrawl(serverDate);
        if (match == EXCLUDE) {
          return EXCLUDE;
        } else if (match == INCLUDE) {
          foundMatch = true;
        }
      }

      if (foundMatch) {
        return INCLUDE;
      } else {
        return IGNORE;
      }
    }

    public String toString() {
      return "[CrawlWindowRules.FirstMatch: " + windows + "]";
    }
  }
}
