/*
 * $Id: CrawlRules.java,v 1.1 2002-10-16 04:52:55 tal Exp $
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

package org.lockss.daemon;
import java.util.*;
import gnu.regexp.*;
import org.lockss.util.*;

/**
 * Several useful CrawlRule implementations.
  */
public class CrawlRules {
  /**
   * CrawlRule.RE is a w3mir-type rule, which consists of a regular
   * expression and an action specifying whether a matching URL should be
   * included or excluded.
   */
  public static class RE implements CrawlRule {
    private gnu.regexp.RE regexp;
    private int action;
  
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
     * Create a rule matching the given RE
     * @param regexp regular expression.
     * @param action one of the constants above.
     * @throws NullPointerException if the regexp is null.
     */
    public RE(gnu.regexp.RE regexp, int action) {
      if (regexp == null) {
	throw new NullPointerException("CrawlRules.RE with null RE");
      }
      this.regexp = regexp;
      this.action = action;
    }
  
    /**
     * Create a rule matching the given RE
     * @param reString regular expression string
     * @param action one of the constants above.
     * @throws REException if an illegal regular expression is provided.
     */
    public RE(String reString, int action) throws REException {
      this(new gnu.regexp.RE(reString), action);
    }
  
    /**
     * Determine whether the URL is included, excluded or ignored by this rule
     * @param url URL to check.
     * @return MATCH_INCLUDE if the URL should be fetched, MATCH_EXCLUDE if
     * if shouldn't be fetched, or MATCH_IGNORE if this rule is agnostic
     * about the URL.
     */
    public int match(String url) {
      boolean match = (null != regexp.getMatch(url));
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
      return "[CrawlRule.RE: '" + regexp + "', " + action + "]";
    }
  }

  /**
   * CrawlRules.FirstMatch matches against a list of
   * {@link CrawlRule}s and returns the first match it finds, or
   * <code>CrawlRule.IGNORE</code> if none match.
   */
  public static class FirstMatch implements CrawlRule {
    private List rules;
  
    /**
     * Create a rule that matches against the given list of rules
     * @param rules list of {@link CrawlRules}s
     * @throws NullPointerException if the list is null.
     */
    public FirstMatch(List rules) {
      if (rules == null) {
	throw new NullPointerException("CrawlRules.FirstMatch with null list");
      }
      this.rules = ListUtil.immutableListOfType(rules, CrawlRule.class);
    }
  
    /**
     * @param str String to check against this rule
     * @return FETCH if the string matches and should be fetched, IGNORE 
     * if str matches and should not be fetched, IGNORE if str doesn't
     * match.
     */
    public int match(String url) {
      for (Iterator iter = rules.iterator(); iter.hasNext(); ) {
	int match = ((CrawlRule)iter.next()).match(url);
	if (match != CrawlRule.IGNORE) {
	  return match;
	}
      }
      return IGNORE;
    }
  
    public String toString() {
      return "[CrawlRule.FirstMatch: " + rules + "]";
    }
  }
}
