/*
 * $Id: RegexpUtil.java,v 1.7 2007-03-16 23:32:11 dshr Exp $
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

package org.lockss.util;
import java.util.*;
import org.apache.commons.collections.map.*;
import org.apache.oro.text.regex.*;

/**
 * Utilities for regexp processing.
 */
public class RegexpUtil {

  private static ThreadLocal reInst = new ThreadLocal() {
      protected Object initialValue() {
	return new REInst();
      }
    };

  /** Return a thread-local Perl5Compiler */
  public static Perl5Compiler getCompiler() {
    return ((REInst)reInst.get()).compiler;
  }

  /** Return a thread-local Perl5Matcher */
  public static Perl5Matcher getMatcher() {
    return ((REInst)reInst.get()).matcher;
  }

  /** Compile a regexp into a pattern, without throwing a checked
   * exception.  Intended for compiling a constant pattern in a static
   * initializer, where exception handling is awkward, and there is no
   * possibility of the pattern being malformed.
   * @throws RuntimeException if RE is malformed */
  public static Pattern uncheckedCompile(String re) {
    return uncheckedCompile(re, Perl5Compiler.DEFAULT_MASK);
  }

  /** Compile a regexp into a pattern, without throwing a checked
   * exception.  Intended for compiling a constant pattern in a static
   * initializer, where exception handling is awkward, and there is no
   * possibility of the pattern being malformed.
   * @throws RuntimeException if RE is malformed */
  public static Pattern uncheckedCompile(String re, int options) {
    try {
      return getCompiler().compile(re, options);
    } catch (MalformedPatternException e) {
      throw new RuntimeException("Malformed RE: " + e.getMessage());
    }
  }

  // cache of compiled patterns
  static LRUMap compiledPatterns = new LRUMap(100);

  /** Compile a pattern and perform a match.  Meant to be used in
   * situations where the caller cannot pre-compile the pattern.  Keeps a
   * cache of recently compiled patterns.
   * @param s String to match
   * @param re regular expression
   * @return true iff s matches re
   * @throws RuntimeException if re is malformed */
  public static boolean isMatchRe(String s, String re) {
    Pattern pat = (Pattern)compiledPatterns.get(re);
    if (pat == null) {
      pat = uncheckedCompile(re, Perl5Compiler.READ_ONLY_MASK);
      compiledPatterns.put(re, pat);
    }
    return getMatcher().contains(s, pat);
  }

  private static class REInst {
    private Perl5Compiler compiler = new Perl5Compiler();
    private Perl5Matcher matcher = new Perl5Matcher();
  }

  /** Static utilities intended for use with JXPath.
   * JXPathContext.setFunctions() makes all the static methods in a class
   * available to xpath expressions, so a separate class is used to expose
   * only appropriate methods.
   * @see org.lockss.daemon.TitleSetXpath
   */
  public static class XpathUtil {
    /** Compile a pattern and perform a match.  As a special case, an empty
     * pattern matches nothing.  This is because forgetting to enclose a
     * pattern in quotes in the xpath expression can easily result in a
     * null-string pattern instead of an error.
     * @param s String to match
     * @param re regular expression
     * @return true iff s matches re
     * @throws RuntimeException if re is malformed */
    public static boolean isMatchRe(String s, String re) {
      if (re.equals("")) {
	return false;
      }
      return RegexpUtil.isMatchRe(s, re);
    }
  }
}
