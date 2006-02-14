/*
 * $Id: CrawlRules.java,v 1.9 2006-02-14 05:20:23 tlipkis Exp $
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
import org.apache.oro.text.regex.*;
import org.lockss.util.*;

/**
 * Several useful CrawlRule implementations.
  */
public class CrawlRules {

  private static Logger logger = Logger.getLogger("CrawlRules");

  /**
   * CrawlRule.RE is a w3mir-type rule, which consists of a regular
   * expression and an action specifying whether a matching URL should be
   * included or excluded.
   */
  public static class RE implements CrawlRule {
    protected Pattern regexp;
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
    public RE(Pattern regexp, int action) {
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
     * @throws LockssRegexpException if an illegal regular expression is
     * provided.
     */
    public RE(String reString, int action) throws LockssRegexpException {
      try {
	regexp =
	  RegexpUtil.getCompiler().compile(reString,
					   Perl5Compiler.READ_ONLY_MASK);
	this.action = action;
      } catch (MalformedPatternException e) {
	throw new LockssRegexpException(e.getMessage());
      }
    }

    /**
     * Perform the match in a synchronized block so that the pattern isn't
     * concurrently used by multiple threads.
     */
    protected boolean isMatch(String url) {
      return isMatch(RegexpUtil.getMatcher(), url);
    }

    /**
     * Perform the match in a synchronized block so that the pattern isn't
     * concurrently used by multiple threads.
     */
    protected synchronized boolean isMatch(Perl5Matcher matcher, String url) {
      return matcher.contains(url, regexp);
    }

    /**
     * Determine whether the URL is included, excluded or ignored by this rule
     * @param url URL to check.
     * @return MATCH_INCLUDE if the URL should be fetched, MATCH_EXCLUDE if
     * if shouldn't be fetched, or MATCH_IGNORE if this rule is agnostic
     * about the URL.
     */
    public int match(String url) {
      boolean match = isMatch(url);
      if (logger.isDebug3()) {
	logger.debug3(this + ".match("+url+"): " + matchAction(match));
      }
      return matchAction(match);
    }

    /** Map the match result and the specified action to one of
     * MATCH_INCLUDE,MATCH_EXCLUDE or MATCH_IGNORE */
    protected int matchAction(boolean match) {
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
      return "[CrawlRule.RE: '" + regexp.getPattern() + "', " + action + "]";
    }
  }

  /**
   * CrawlRule.REMatchCondition is a base class for rules that match
   * against an RE then apply a condition to one or more subexpressions
   * (parts of the match designated by grouping parentheses).  For purposes
   * of the MATCH_ actions, it is a match if the pattern matches and the
   * condition is true.
   */
  public abstract static class REMatchCondition extends RE {
    public REMatchCondition(Pattern regexp, int action) {
      super(regexp, action);
    }

    public REMatchCondition(String reString, int action)
	throws LockssRegexpException {
      super(reString, action);
    }

    /** Apply a condition to the result of a match */
    protected abstract boolean isConditionMet(MatchResult matchResult);

    /**
     * Apply the matcher then check the condition if the match succeeds
     */
    public int match(String url) {
      if (logger.isDebug3()) {
	logger.debug3("Match called with "+url);
      }
      Perl5Matcher matcher = RegexpUtil.getMatcher();
      boolean match = isMatch(matcher, url);
      if (match) {
	match &= isConditionMet(matcher.getMatch());
      }
      return matchAction(match);
    }
  }

  /**
   * CrawlRule.REMatchRange matches an RE then checks that the
   * subexpression falls within the specified range (inclusive).
   */
  public static class REMatchRange extends REMatchCondition {
    static final int MODE_LONG = 0;
    static final int MODE_COMP = 1;

    int mode;

    long minLong;
    long maxLong;

    Comparable minComp;
    Comparable maxComp;

    /** Create an integer range matcher.
     * @param reString regular expression string
     * @param action one of the constants above.
     * @param min the minimum subexpression value
     * @param max the maximum subexpression value
     * @throws LockssRegexpException if an illegal regular expression is
     * provided.
     */
    public REMatchRange(String reString, int action, long min, long max)
	throws LockssRegexpException {
      super(reString, action);
      this.minLong = min;
      this.maxLong = max;
      mode = MODE_LONG;
    }

    /** Create a String (alphabetical) range matcher.
     * @param reString regular expression string
     * @param action one of the constants above.
     * @param min the minimum subexpression value
     * @param max the maximum subexpression value
     * @throws LockssRegexpException if an illegal regular expression is
     * provided.
     */
    public REMatchRange(String reString, int action, String min, String max)
	throws LockssRegexpException {
      super(reString, action);
      if (min == null || max == null) {
	throw new NullPointerException("REMatchRange has null min or max");
      }
      this.minComp = min;
      this.maxComp = max;
      mode = MODE_COMP;
    }

    /** Return true iff the subexpression falls within the range */
    protected boolean isConditionMet(MatchResult matchResult) {
      String sub = matchResult.group(1);
      if (sub == null) {
	return false;
      }
      switch (mode) {
      case MODE_LONG:
	try {
	  long val = Long.parseLong(sub);
	  return minLong <= val && val <= maxLong;
	} catch (NumberFormatException e) {
	  return false;
	}
      case MODE_COMP:
	return minComp.compareTo(sub) <= 0 && maxComp.compareTo(sub) >= 0;
      }
      return false;
    }
  }

  /**
   * CrawlRule.REMatchSet matches an RE then checks that the
   * subexpression is contained within the specified set.
   */
  public static class REMatchSet extends REMatchCondition {
    Set set;

    /** Create a set matcher.
     * @param reString regular expression string
     * @param action one of the constants above.
     * @param set a Set of Strings
     * @throws LockssRegexpException if an illegal regular expression is
     * provided.
     */
    public REMatchSet(String reString, int action, Set set)
	throws LockssRegexpException {
      super(reString, action);
      if (set == null) {
	throw new NullPointerException("REMatchSet has null set");
      }
      this.set = set;
    }

    /** Return true iff the subexpression is a member of the set */
    protected boolean isConditionMet(MatchResult matchResult) {
      String sub = matchResult.group(1);
      if (sub == null) {
	return false;
      }
      if (logger.isDebug3()) {
	logger.debug3("Checking if "+sub+" is in "+set);
	if (set.contains(sub)) {
	  logger.debug3("It is");
	} else {
	  logger.debug3("It isn't");
	}
      }
      return set.contains(sub);
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
     * @param url URL string to check against this rule
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
