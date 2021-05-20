/*
 * $Id$
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.oro.text.regex.*;
import org.lockss.test.*;
import org.lockss.util.*;
//import org.lockss.daemon.*;

/**
 * This is the test class for org.lockss.alert.RegexpUtil
 */
public class TestRegexpUtil extends LockssTestCase {
  private static Logger log = Logger.getLogger("TestRegexpUtil");

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  volatile Perl5Compiler tcomp;
  volatile Perl5Matcher tmatch;

  public void testPerThreadObjects() throws Exception {
    Perl5Compiler c1 = RegexpUtil.getCompiler();
    Perl5Matcher m1 = RegexpUtil.getMatcher();

    // should get same instances back in same thread
    assertSame(c1, RegexpUtil.getCompiler());
    assertSame(m1, RegexpUtil.getMatcher());

    // should get different instances in another thread
    Thread th = new Thread() {
	public void run() {
	  tcomp = RegexpUtil.getCompiler();
	  tmatch = RegexpUtil.getMatcher();
	}};
    th.start();
    th.join();
    assertTrue(tcomp instanceof Perl5Compiler);
    assertTrue(tmatch instanceof Perl5Matcher);
    assertNotSame(c1, tcomp);
    assertNotSame(m1, tmatch);

  }

  public void testIsMatchRe() throws Exception {
    assertTrue(RegexpUtil.isMatchRe("foobar", "ob"));
    assertTrue(RegexpUtil.isMatchRe("foobar", "bar$"));
    assertTrue(RegexpUtil.isMatchRe("foobar", "^foobar$"));
    // repeat to ensure that compiled-pattern caching doesn't break anything
    // (should also test that cache works)
    assertTrue(RegexpUtil.isMatchRe("foobar", "^foobar$"));
    assertFalse(RegexpUtil.isMatchRe("foobar", "^obar"));
    // repeat
    assertFalse(RegexpUtil.isMatchRe("foobar", "^obar"));
  }

  public void testCompileRegexps() throws Exception {
    List<Pattern> pats = RegexpUtil.compileRegexps(ListUtil.list("fo+l",
								 "ba*h"));
    assertEquals(ListUtil.list("fo+l", "ba*h"),
		 RegexpUtil.regexpCollection(pats));
  }

  public void testIsMatch() throws Exception {
    List<Pattern> pats = RegexpUtil.compileRegexps(ListUtil.list("fo+l",
								 "ba*h"));
    assertTrue(RegexpUtil.isMatch("foool", pats));
    assertFalse(RegexpUtil.isMatch("fl", pats));
    assertTrue(RegexpUtil.isMatch("bh", pats));
    assertTrue(RegexpUtil.isMatch("bah", pats));
  }

  public void testXPathIsMatchRe() throws Exception {
    assertTrue(RegexpUtil.XpathUtil.isMatchRe("foobar", "ob"));
    assertFalse(RegexpUtil.XpathUtil.isMatchRe("foobar", ""));
  }

  java.util.regex.Pattern comp(String re) {
    return java.util.regex.Pattern.compile(re);
  }

  java.util.regex.Pattern comp(String re, int flags) {
    return java.util.regex.Pattern.compile(re, flags);
  }

  public void testPatEquals() {
    assertTrue(RegexpUtil.patEquals(comp("[abc]+"), comp("[abc]+")));
    assertTrue(RegexpUtil.patEquals(comp("[abc]+"), comp("[abc]+", 0)));
    assertFalse(RegexpUtil.patEquals(comp("[abc]+"),
				     comp("[abc]+", java.util.regex.Pattern.CASE_INSENSITIVE)));
    assertFalse(RegexpUtil.patEquals(comp("[abc]+"), comp("[abc]*")));
  }

  public void testQuotemeta() {
    assertEquals("", RegexpUtil.quotemeta(""));
    assertEquals("foo", RegexpUtil.quotemeta("foo"));
    assertEquals("foo\\.b\\[a\\]\\ r", RegexpUtil.quotemeta("foo.b[a] r"));
    assertEquals("foo\\.b\\(a\\)\\ r", RegexpUtil.quotemeta("foo.b(a) r"));
  }
}
