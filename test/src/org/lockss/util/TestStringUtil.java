/*
 * $Id: TestStringUtil.java,v 1.13 2003-03-11 23:28:00 tal Exp $
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

package org.lockss.util;

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.util.StringUtil
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public class TestStringUtil extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.util.StringUtil.class
  };

  public TestStringUtil(String msg) {
    super(msg);
  }

  public void testNullStr() {
    assertNull(StringUtil.trimAfterChars(null, null));
  }

  public void testNullChars() {
    String testStr = "test";
    assertEquals(testStr,
		 StringUtil.trimAfterChars(testStr, null));
  }

  public void testNoMatch() {
    String testStr = "test!blah";
    assertEquals("test!blah",
		 StringUtil.trimAfterChars(testStr, " "));
  }

  public void testSingleChar() {
    String testStr = "test!blah";
    assertEquals("test",
		 StringUtil.trimAfterChars(testStr, "!"));
  }

  public void testMultiChars() {
    assertEquals("test",
		 StringUtil.trimAfterChars("test!blah", "! \""));
    assertEquals("test",
		 StringUtil.trimAfterChars("test blah", "! \""));
    assertEquals("test",
		 StringUtil.trimAfterChars("test\"blah", "! \""));
  }

  public void testGetIndexIgnoringCaseNullString() {
    assertEquals(-1, StringUtil.getIndexIgnoringCase(null, "blah"));
  }

  public void testGetIndexIgnoringCaseNullSubString() {
    assertEquals(-1, StringUtil.getIndexIgnoringCase("blah", null));
  }

  public void testGetIndexIgnoringCaseCapedString() {
    String testStr = "blahTeStblah";
    assertEquals(4, StringUtil.getIndexIgnoringCase(testStr, "test"));
  }

  public void testGetIndexIgnoringCaseDoesntChangeStrs() {
    String testStr = "blahTeStblah";
    String testSubStr = "test";
    StringUtil.getIndexIgnoringCase(testStr, testSubStr);
    assertEquals("test", testSubStr);
    assertEquals("blahTeStblah", testStr);
  }

  public void testSeparatedString() {
    assertEquals("1,2,3",
		 StringUtil.separatedString(ListUtil.list("1","2","3"), ","));
    assertEquals("'1','2','3'",
		 StringUtil.separatedDelimitedString(ListUtil.list("1","2",
								   "3"),
						     ",", "'"));
    assertEquals("[1],[2],[3]",
		 StringUtil.separatedDelimitedString(ListUtil.list("1","2",
								   "3"),
						     ",", "[", "]"));
    String a[] = {"a", "b", "c"};
    assertEquals("a,b,c", StringUtil.separatedString(a, ","));
  }

  public void testEscapeNonAlphaNum() {
    assertEquals("", StringUtil.escapeNonAlphaNum(""));
    assertEquals("123", StringUtil.escapeNonAlphaNum("123"));
    assertEquals("\\ 1\\.2\\\\3\\?", StringUtil.escapeNonAlphaNum(" 1.2\\3?"));
  }

  public void testCountOccurences() {
    assertEquals(0, StringUtil.countOccurences("", ""));
    assertEquals(0, StringUtil.countOccurences("test", ""));
    assertEquals(2, StringUtil.countOccurences("test.test", "test"));
    assertEquals(2, StringUtil.countOccurences("testtest", "test"));
    assertEquals(2, StringUtil.countOccurences("xxxxxy", "xx"));
  }

  public void testReplaceStringNonExistingSubstring(){
    String testStr = "blahTestblah";
    assertEquals(testStr, StringUtil.replaceString(testStr, "!", "8"));
    assertEquals(testStr, StringUtil.replaceString(testStr, "lah1", "8"));
  }

  public void testReplaceStringSingleExistingSubstring(){
    String testStr = "blahTestblah";
    // same length
    assertEquals("blah1234blah",
		 StringUtil.replaceString(testStr, "Test", "1234"));
    // shorter
    assertEquals("blahabcblah",
		 StringUtil.replaceString(testStr, "Test", "abc"));
    // longer
    assertEquals("blahCheeseblah",
		 StringUtil.replaceString(testStr, "Test", "Cheese"));
  }

  public void testReplaceStringMultiExistingSubstring(){
    String testStr = "blahTestblah";
    // same length
    assertEquals("BrieTestBrie",
		 StringUtil.replaceString(testStr, "blah", "Brie"));
    // shorter
    assertEquals("blahabcblah",
		 StringUtil.replaceString(testStr, "Test", "abc"));
    // longer
    assertEquals("splungeTestsplunge",
		 StringUtil.replaceString(testStr, "blah", "splunge"));
  }

  public void testReplacementStringContainsReplacedString(){
    assertEquals("1234456",
  		 StringUtil.replaceString("123456", "4", "44"));
    assertEquals("123444456",
  		 StringUtil.replaceString("1234456", "4", "44"));
  }

  public void testOverlap(){
    String testStr = "xxx1xxx2xxx3xxx";
    assertEquals("ddx1ddx2ddx3ddx",
		 StringUtil.replaceString(testStr, "xx", "dd"));
  }

  public void testReplaceEqualStrings(){
    String testStr = "xxx1xxx2xxx3xxx";
    assertSame(testStr, StringUtil.replaceString(testStr, "1", "1"));
    assertSame(testStr, StringUtil.replaceString(testStr, "xx", "xx"));
  }

  public void testBreakAt() {
    Vector v = new Vector();
    assertEquals(v, StringUtil.breakAt(null, ' '));
    assertIsomorphic(ListUtil.list("foo"), StringUtil.breakAt("foo", ' '));
    assertIsomorphic(ListUtil.list("foo", "bar"),
		     StringUtil.breakAt("foo bar", ' '));
    assertIsomorphic(ListUtil.list("foo", "", "bar"),
		     StringUtil.breakAt("foo  bar", ' '));
    assertIsomorphic(ListUtil.list("foo", "bar"),
		     StringUtil.breakAt("foo bar ddd", ' ', 2));
  }

  public void testFromReader() throws Exception {
    String s = "asdfjsfd";
    Reader r = new InputStreamReader(new StringInputStream(s));
    assertEquals(s, StringUtil.fromReader(r));
  }

  public void testEqualStrings() {
    assertTrue(StringUtil.equalStrings(null, null));
    assertFalse(StringUtil.equalStrings("1", null));
    assertFalse(StringUtil.equalStrings(null, "1"));
    assertTrue(StringUtil.equalStrings("foo", "foo"));
    assertFalse(StringUtil.equalStrings("foo", "bar"));
  }

  public void testGensym() {
    String base = "foo";
    String g1 = StringUtil.gensym(base);
    String g2 = StringUtil.gensym(base);
    assertTrue(g1.startsWith(base));
    assertNotEquals(g1, g2);
  }

  public void testTrimHostName() {
    assertEquals("foo", StringUtil.trimHostName("www.foo.com"));
    assertEquals("foo", StringUtil.trimHostName("foo.com"));
    assertEquals("foo.bar", StringUtil.trimHostName("www.foo.bar.com"));
    assertEquals("foo.bar", StringUtil.trimHostName("foo.bar.com"));

    // should leave all these alone
    assertEquals("foo", StringUtil.trimHostName("foo"));
    assertEquals("", StringUtil.trimHostName(""));
    assertNull(StringUtil.trimHostName(null));

    // trimming both www. and .com would cause error
    assertEquals("www.com", StringUtil.trimHostName("www.com"));
    // would leave empty string
    assertEquals("www..com", StringUtil.trimHostName("www..com"));
  }
}
