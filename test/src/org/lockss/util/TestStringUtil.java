/*
 * $Id: TestStringUtil.java,v 1.31 2004-01-28 05:23:10 tlipkis Exp $
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

import junit.framework.TestCase;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
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

  public void testReplaceFirst() {
    assertEquals("aabbcc", StringUtil.replaceFirst("aaddcc", "dd", "bb"));
    assertEquals("aabb", StringUtil.replaceFirst("aadd", "dd", "bb"));
    assertEquals("aabbccdd", StringUtil.replaceFirst("aaddccdd", "dd", "bb"));
    assertEquals("aabbddcc", StringUtil.replaceFirst("aaddddcc", "dd", "bb"));
    assertEquals("aabbcc", StringUtil.replaceFirst("aabbcc", "dd", "bb"));

    String s = "foo";
    assertSame(s, StringUtil.replaceFirst(s, "123", "123"));
    assertSame(s, StringUtil.replaceFirst(s, "", "123"));
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
    assertEquals(v, StringUtil.breakAt("", ' '));
    assertIsomorphic(ListUtil.list("foo"), StringUtil.breakAt("foo", ' '));
    assertIsomorphic(ListUtil.list("foo", "bar"),
		     StringUtil.breakAt("foo bar", ' '));
    assertIsomorphic(ListUtil.list("foo", "", "bar"),
		     StringUtil.breakAt("foo  bar", ' '));
    assertIsomorphic(ListUtil.list("foo", ""),
		     StringUtil.breakAt("foo ", ' '));
    assertIsomorphic(ListUtil.list("", "foo"),
		     StringUtil.breakAt(" foo", ' '));
    assertIsomorphic(ListUtil.list("foo", "bar"),
		     StringUtil.breakAt("foo bar ddd", ' ', 2));

    assertIsomorphic(ListUtil.list("", ""),
		     StringUtil.breakAt("+", '+', -1, false));
    assertIsomorphic(ListUtil.list(),
		     StringUtil.breakAt("+", '+', -1, true));
    assertIsomorphic(ListUtil.list("", "foo"),
		     StringUtil.breakAt("+foo", '+', -1, false));
    assertIsomorphic(ListUtil.list("foo"),
		     StringUtil.breakAt("+foo", '+', -1, true));
    assertIsomorphic(ListUtil.list("foo", ""),
		     StringUtil.breakAt("foo+", '+', -1, false));
    assertIsomorphic(ListUtil.list("foo"),
		     StringUtil.breakAt("foo+", '+', -1, true));
    assertIsomorphic(ListUtil.list("foo "),
		     StringUtil.breakAt("foo +", '+', -1, true, false));
    assertIsomorphic(ListUtil.list("foo"),
		     StringUtil.breakAt("foo +", '+', -1, true, true));
    assertIsomorphic(ListUtil.list("foo"),
		     StringUtil.breakAt("foo + ", '+', -1, true, true));
    assertIsomorphic(ListUtil.list("foo ", " "),
		     StringUtil.breakAt("foo + ", '+', -1, true, false));

  }

  public void testFromReader() throws Exception {
    String s = "asdfjsfd";
    Reader r = new InputStreamReader(new StringInputStream(s));
    assertEquals(s, StringUtil.fromReader(r));
  }

  public void testFromInputStream() throws Exception {
    String s = "asdfjsfd";
    assertEquals(s, StringUtil.fromInputStream(new StringInputStream(s)));
  }

  public void testEqualStrings() {
    assertTrue(StringUtil.equalStrings(null, null));
    assertFalse(StringUtil.equalStrings("1", null));
    assertFalse(StringUtil.equalStrings(null, "1"));
    assertTrue(StringUtil.equalStrings("foo", "foo"));
    assertFalse(StringUtil.equalStrings("foo", "Foo"));
    assertFalse(StringUtil.equalStrings("foo", "bar"));
  }

  public void testEqualStringsIgnoreCase() {
    assertTrue(StringUtil.equalStringsIgnoreCase(null, null));
    assertFalse(StringUtil.equalStringsIgnoreCase("1", null));
    assertFalse(StringUtil.equalStringsIgnoreCase(null, "1"));
    assertTrue(StringUtil.equalStringsIgnoreCase("foo", "foo"));
    assertTrue(StringUtil.equalStringsIgnoreCase("foo", "Foo"));
    assertFalse(StringUtil.equalStringsIgnoreCase("foo", "bar"));
  }

  public void testIsNullString() {
    assertTrue(StringUtil.isNullString(null));
    assertTrue(StringUtil.isNullString(""));
    assertFalse(StringUtil.isNullString(" "));
  }

  public void testGensym() {
    String base = "foo";
    String g1 = StringUtil.gensym(base);
    String g2 = StringUtil.gensym(base);
    assertTrue(g1.startsWith(base));
    assertNotEquals(g1, g2);
  }

  public void testShortNameObject() {
    Object o = null;
    assertNull(StringUtil.shortName(o));
    assertEquals("foo", StringUtil.shortName("foo"));
    assertEquals("foo", StringUtil.shortName("bar.foo"));
  }

  public void testShortNameClass() {
    assertEquals("Date", StringUtil.shortName(java.util.Date.class));
  }

  public void testShortNameMethod() throws NoSuchMethodException {
    Method meth = this.getClass().getDeclaredMethod("testShortNameMethod",
						    new Class[0]);
    assertEquals("TestStringUtil.testShortNameMethod",
		 StringUtil.shortName(meth));
  }

  public void testStackTraceString() {
    String s = StringUtil.stackTraceString(new Exception());
    String exp = "java.lang.Exception" + Constants.EOL +
      "\tat org.lockss.util.TestStringUtil.testStackTraceString(TestStringUtil.java:";
System.out.println("s: "+s);
    assertTrue(s.startsWith(exp));
  }

  public void testTrimBlankLines() {
    assertEquals("foo", StringUtil.trimBlankLines("foo"));
    assertEquals("foo", StringUtil.trimBlankLines("\nfoo"));
    assertEquals("foo", StringUtil.trimBlankLines("\nfoo\n"));
    assertEquals("foo", StringUtil.trimBlankLines("\n\nfoo\n\n"));
    assertEquals("foo\nbar", StringUtil.trimBlankLines("\n\nfoo\nbar\n\n"));
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

  static final int SECOND = 1000;
  static final int MINUTE = 60 * SECOND;
  static final int HOUR = 60 * MINUTE;
  static final int DAY = 24 * HOUR;
  static final int WEEK = 7 * DAY;

  public void testParseTimeInterval() throws Exception {
    assertEquals(0, StringUtil.parseTimeInterval("0"));
    assertEquals(0, StringUtil.parseTimeInterval("0s"));
    assertEquals(0, StringUtil.parseTimeInterval("0m"));
    assertEquals(0, StringUtil.parseTimeInterval("0h"));
    assertEquals(0, StringUtil.parseTimeInterval("0d"));
    assertEquals(0, StringUtil.parseTimeInterval("0w"));
    assertEquals(1000, StringUtil.parseTimeInterval("1000"));
    assertEquals(SECOND, StringUtil.parseTimeInterval("1s"));
    assertEquals(MINUTE, StringUtil.parseTimeInterval("1m"));
    assertEquals(HOUR, StringUtil.parseTimeInterval("1h"));
    assertEquals(DAY, StringUtil.parseTimeInterval("1d"));
    assertEquals(WEEK, StringUtil.parseTimeInterval("1w"));
    assertEquals(StringUtil.parseTimeInterval("60s"),
		 StringUtil.parseTimeInterval("1m"));
    assertEquals(StringUtil.parseTimeInterval("120m"),
		 StringUtil.parseTimeInterval("2h"));
    assertEquals(StringUtil.parseTimeInterval("72h"),
		 StringUtil.parseTimeInterval("3d"));
    assertEquals(StringUtil.parseTimeInterval("14d"),
		 StringUtil.parseTimeInterval("2w"));
    try {
      StringUtil.parseTimeInterval("2x");
      fail("should have thrown NumberFormatException");
    } catch (NumberFormatException e) {
    }
    try {
      StringUtil.parseTimeInterval("");
      fail("should have thrown NumberFormatException");
    } catch (NumberFormatException e) {
    }
  }

  public void testTimeInterval() throws Exception {
    assertEquals("0ms", StringUtil.timeIntervalToString(0));
    assertEquals("1000ms", StringUtil.timeIntervalToString(SECOND));
    assertEquals("9000ms", StringUtil.timeIntervalToString(SECOND * 9));
    assertEquals("10s", StringUtil.timeIntervalToString(SECOND * 10));
    assertEquals("1m0s", StringUtil.timeIntervalToString(MINUTE));
    assertEquals("1h0m0s", StringUtil.timeIntervalToString(HOUR));
    assertEquals("2d3h0m",
		 StringUtil.timeIntervalToString(DAY * 2 + HOUR * 3));
    assertEquals("20d23h0m",
		 StringUtil.timeIntervalToString(WEEK * 3 - (HOUR * 1)));
    assertEquals("3w0d0h", StringUtil.timeIntervalToString(WEEK * 3));
  }

  public void testTrimStackTrace() {
    String s1 = "Exception string: Nested error: java.io.FileNotFoundException: /tmp/iddb/idmapping.xml (No such file or directory)";
    String s2 = "java.io.FileNotFoundException: /tmp/iddb/idmapping.xml (No such file or directory)";
    String s2a = s2 + "junk";
    String s3 = "	at java.io.FileInputStream.open(Native Method)";
    String st1 = s2 + "\n" + s3;
    assertEquals(s3, StringUtil.trimStackTrace(s1, st1));
    String st2 = s2a + "\n" + s3;
    assertEquals(st2, StringUtil.trimStackTrace(s1, st2));
  }

  public void testEndsWithIgnoreCaase() {
    assertTrue(StringUtil.endsWithIgnoreCase("", ""));
    assertTrue(StringUtil.endsWithIgnoreCase("1", ""));
    assertTrue(StringUtil.endsWithIgnoreCase("1", "1"));
    assertTrue(StringUtil.endsWithIgnoreCase("foo.opt", ".opt"));
    assertFalse(StringUtil.endsWithIgnoreCase("", "2"));
    assertFalse(StringUtil.endsWithIgnoreCase("1", "2"));
    assertFalse(StringUtil.endsWithIgnoreCase("21", "2"));
    assertFalse(StringUtil.endsWithIgnoreCase("foo.opt", "xopt"));
  }

  public void testStartsWithIgnoreCaase() {
    assertTrue(StringUtil.startsWithIgnoreCase("", ""));
    assertTrue(StringUtil.startsWithIgnoreCase("1", ""));
    assertTrue(StringUtil.startsWithIgnoreCase("1", "1"));
    assertTrue(StringUtil.startsWithIgnoreCase("foo.opt", "foo."));
    assertFalse(StringUtil.startsWithIgnoreCase("", "2"));
    assertFalse(StringUtil.startsWithIgnoreCase("1", "2"));
    assertFalse(StringUtil.startsWithIgnoreCase("12", "2"));
    assertFalse(StringUtil.startsWithIgnoreCase("foo.opt", "foox"));
  }

  public void testTitleCase() {
    String txt1 = "this is 8 words.  can it be handled?";
    String txt2 = "This Is 8 Words.  Can It Be Handled?";
    assertEquals(StringUtil.titleCase(txt1),txt2);
  }

  public void testFromFile() {
    try {
      String txt = "Here is some weird text.\nIt has !@#$%^&*()214 in it.";
      String path = getTempDir().getAbsolutePath() + File.separator +
          "test.txt";
      FileWriter fw = new FileWriter(path);
      fw.write(txt);
      fw.close();
      String readtxt = StringUtil.fromFile(path);
      assertEquals(txt, readtxt);
      File fl = new File(path);
      fl.delete();
    }
    catch (IOException e) {
      fail(e.getMessage());
    }
  }

  public void testUpToFinal() {
    assertEquals("foo", StringUtil.upToFinal("foo.bar", "."));
    assertEquals("foo.a", StringUtil.upToFinal("foo.a.bar", "."));
    assertEquals("foo", StringUtil.upToFinal("foo", "."));
    assertEquals("", StringUtil.upToFinal(".foo", "."));
  }

}
