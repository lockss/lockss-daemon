package org.lockss.plugin;

import gnu.regexp.RE;
import gnu.regexp.REException;


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

/**
 * This class encapsulates a w3mir-type rule, which consists of a regular
 * expression and a flag to indicate whether the a matching url should be
 * cached or ignored
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
  //class equavalent to the Fetch/Ignore rules w3mir uses
public class CrawlRule{
  private RE regExp;
  private boolean shouldFetch;
  public static final int FETCH = 1;
  public static final int IGNORE = 0;
  public static final int NO_MATCH = -1;
  
  /**
   * 
   * @param regExpStr perl style regular expression
   * @param shouldFetch true if urls matching regExpStr should be fetched
   * false if they should be ignored
   * @throws REException if an illegal regular expression is provided
   */
  public CrawlRule(String regExpStr, boolean shouldFetch) throws REException{
    this.regExp = new RE(regExpStr);
    this.shouldFetch = shouldFetch;
  }
  
  /**
   * @param str String to check against this rule
   * @return FETCH if the string matches and should be fetched, IGNORE 
   * if str matches and should not be fetched, NO_MATCH if str doesn't
   * match.
   */
  public int matches(String str){
    if (regExp.isMatch(str)){
      return (shouldFetch ? FETCH : IGNORE);
    }
    return NO_MATCH;
  }
  
  public String toString(){
    return (shouldFetch ? "Fetch: " : "Ignore: ")+regExp;
  }
}
