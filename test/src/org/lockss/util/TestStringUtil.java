/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

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

  public void testTruncateAtAnyNullStr() {
    assertNull(StringUtil.truncateAtAny(null, null));
  }

  public void testTruncateAtAnyNullChars() {
    String testStr = "test";
    assertEquals(testStr,
		 StringUtil.truncateAtAny(testStr, null));
  }

  public void testTruncateAtAnyNoMatch() {
    String testStr = "test!blah";
    assertEquals("test!blah",
		 StringUtil.truncateAtAny(testStr, " "));
  }

  public void testTruncateAtAnySingleChar() {
    String testStr = "test!blah";
    assertEquals("test",
		 StringUtil.truncateAtAny(testStr, "!"));
  }

  public void testTruncateAtAnyMultiChars() {
    assertEquals("test",
		 StringUtil.truncateAtAny("test!blah", "! \""));
    assertEquals("test",
		 StringUtil.truncateAtAny("test blah", "! \""));
    assertEquals("test",
		 StringUtil.truncateAtAny("test\"blah", "! \""));
    // ensure finds the first occurrence of *any* of the chars
    assertEquals("test",
		 StringUtil.truncateAtAny("test !blah", "! \""));

  }

  public void testTruncateAt() {
    assertNull(StringUtil.truncateAt(null, 'a'));
    assertEquals("test",
		 StringUtil.truncateAt("test!blah", '!'));
    assertEquals("",
		 StringUtil.truncateAt("!blah", '!'));
    assertEquals("test",
		 StringUtil.truncateAt("test|", '|'));
    assertEquals("test",
		 StringUtil.truncateAt("test|foo|bar", '|'));
    assertEquals("test|blah",
		 StringUtil.truncateAt("test|blah", '0'));
  }

  public void testElideMiddleToMaxLen() {
    assertNull(StringUtil.elideMiddleToMaxLen(null, 10));
    assertEquals("test",
		 StringUtil.elideMiddleToMaxLen("test", 10));
    assertEquals("test123456",
		 StringUtil.elideMiddleToMaxLen("test123456", 10));
    assertEquals("test...3456",
		 StringUtil.elideMiddleToMaxLen("test123456", 9));
    assertEquals("foo...bar",
		 StringUtil.elideMiddleToMaxLen("foonlyrebar", 6));
  }

  public void testIndexOfIgnoreCase() {
    String testStr, testSubStr;
    // both null cases
    assertEquals(-1, StringUtil.indexOfIgnoreCase(null, "blah"));
    assertEquals(-1, StringUtil.indexOfIgnoreCase("blah", null));
    assertEquals(0, StringUtil.indexOfIgnoreCase("foo", "foo"));
    assertEquals(0, StringUtil.indexOfIgnoreCase("foo1", "foo"));
    // simple tests
    testStr = "blahTeStblahtest";
    assertEquals(4, StringUtil.indexOfIgnoreCase(testStr, "test"));
    assertEquals(4, StringUtil.indexOfIgnoreCase(testStr, "test", 0));
    assertEquals(4, StringUtil.indexOfIgnoreCase(testStr, "test", 4));
    assertEquals(12, StringUtil.indexOfIgnoreCase(testStr, "test", 5));
  }

  public void testSeparatedString() {
    assertEquals("1, 2, 3",
		 StringUtil.separatedString(ListUtil.list("1","2","3")));
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
    assertEquals("2,6,3", StringUtil.separatedString(new int[]{2,6,3}, ","));
    assertEquals("2,6,3", StringUtil.separatedString(new long[]{2,6,3}, ","));
  }

  public void testEscapeNonAlphaNum() {
    assertEquals("", StringUtil.escapeNonAlphaNum(""));
    assertEquals("123", StringUtil.escapeNonAlphaNum("123"));
    assertEquals("\\ 1\\.2\\\\3\\?", StringUtil.escapeNonAlphaNum(" 1.2\\3?"));
  }

  public void testCkvEscape() {
    assertEquals("", StringUtil.ckvEscape(""));
    assertSame("foo", StringUtil.ckvEscape("foo"));
    assertEquals("foo\\\\bar\\,x", StringUtil.ckvEscape("foo\\bar,x"));
    assertEquals("key\\=val", StringUtil.ckvEscape("key=val"));
    assertEquals("key\\=val\\,k", StringUtil.ckvEscape("key=val,k"));
  }

  public void testCsvEncode() {
    assertEquals("", StringUtil.csvEncode(""));
    assertSame("foo", StringUtil.csvEncode("foo"));
    assertEquals("foo\\bar", StringUtil.csvEncode("foo\\bar"));
    assertEquals("\"foo,bar\"", StringUtil.csvEncode("foo,bar"));
    assertEquals("\"quote\"\"me\"\"\"", StringUtil.csvEncode("quote\"me\""));
    assertEquals("\"quote \"\"me\"\" now\"",
		 StringUtil.csvEncode("quote \"me\" now"));
  }

  public void testCsvEncodeValues() {
    //assertEquals("", StringUtil.csvEncodeValues(null));
    assertEquals("", StringUtil.csvEncodeValues(Collections.<String>emptyList()));
    assertEquals("", StringUtil.csvEncodeValues(new String[0]));
    assertEquals("", StringUtil.csvEncodeValues(new String[]{""}));
    assertEquals(",", StringUtil.csvEncodeValues(new String[]{"", ""}));
    assertEquals(",,", StringUtil.csvEncodeValues(new String[]{"", "", ""}));
    assertEquals("foo", StringUtil.csvEncodeValues(new String[]{"foo"}));
    // Quotes are doubled, and the phrase is quoted
    assertEquals("\"\"\"foo\"\"\"", StringUtil.csvEncodeValues(new String[]{"\"foo\""}));
    assertEquals("\" \"\"foo\"\" \"", StringUtil.csvEncodeValues(new String[]{" \"foo\" "}));
    assertEquals("foo,bar", StringUtil.csvEncodeValues(new String[]{"foo", "bar"}));
    assertEquals("foo,\" bar \"", StringUtil.csvEncodeValues(new String[]{"foo", " bar "}));
    assertEquals("\"foo,bar\",bar", StringUtil.csvEncodeValues(new String[]{"foo,bar", "bar"}));
    assertEquals("\"foo,bar\",\" \"\"bar\"\" \"", StringUtil.csvEncodeValues(new String[]{"foo,bar", " \"bar\" "}));
    assertEquals("\"foo,bar\",\"\"\"bar\"\"\"", StringUtil.csvEncodeValues(new String[]{"foo,bar", "\"bar\""}));
  }

  public void testCountOccurences() {
    assertEquals(0, StringUtil.countOccurences("", ""));
    assertEquals(0, StringUtil.countOccurences("test", ""));
    assertEquals(2, StringUtil.countOccurences("test.test", "test"));
    assertEquals(2, StringUtil.countOccurences("testtest", "test"));
    assertEquals(2, StringUtil.countOccurences("xxxxxy", "xx"));
  }

  void assertWH(int w, int h, StringUtil.CharWidthHeight cwh) {
    assertEquals("Wrong width", w, cwh.getWidth());
    assertEquals("Wrong height", h, cwh.getHeight());
  }

  public void testCountWidthAndHeight() {
    assertWH(0, 0, StringUtil.countWidthAndHeight(null));
    assertWH(0, 0, StringUtil.countWidthAndHeight(""));
    assertWH(3, 1, StringUtil.countWidthAndHeight("123"));
    assertWH(3, 1, StringUtil.countWidthAndHeight("123\n"));
    assertWH(3, 1, StringUtil.countWidthAndHeight("123\r"));
    assertWH(3, 1, StringUtil.countWidthAndHeight("123\r\n"));
    assertWH(3, 2, StringUtil.countWidthAndHeight("123\n\n"));
    assertWH(3, 2, StringUtil.countWidthAndHeight("123\n "));
    assertWH(5, 3, StringUtil.countWidthAndHeight("123\n11111\nxx"));
    assertWH(5, 3, StringUtil.countWidthAndHeight("123\r11111\rxx"));
    assertWH(5, 3, StringUtil.countWidthAndHeight("123\r\n11111\r\nxx"));
  }

  public void testGetTextBetween() {
      // Check first match is found
      assertEquals(" and ", StringUtil.getTextBetween("here and there", "here", "there"));
      // Remember to use match text as specific as possible
      assertEquals("Journal", StringUtil.getTextBetween("The <b>journal</b> has this name: <b>Journal</b>", "name: <b>", "</b>"));
      // Check non-greediness
      assertEquals("two", StringUtil.getTextBetween("One two three, One five three", "One ", " three"));
  }

  public void testCommonPrefix() {
    assertEquals("", StringUtil.commonPrefix("", ""));
    assertEquals("", StringUtil.commonPrefix("one", "cone"));
    assertEquals("co", StringUtil.commonPrefix("come", "cone"));
    assertEquals("", StringUtil.commonPrefix("CAPS", "caps"));
    assertEquals("", StringUtil.commonPrefix("parse", "arse"));
    assertEquals("LaTe", StringUtil.commonPrefix("LaTeX", "LaTex"));
    assertEquals("LaTe", StringUtil.commonPrefix("LaTex", "LaTeX"));
    assertEquals("locks", StringUtil.commonPrefix("locks", "lockss"));
    // Try comparing ASCII chars with same char value
    assertEquals("", StringUtil.commonPrefix("\t", "9"));
    assertEquals("", StringUtil.commonPrefix("\0", "0"));
  }

  public void testCommonSuffix() {
    assertEquals("", StringUtil.commonSuffix("", ""));
    assertEquals("one", StringUtil.commonSuffix("one", "cone"));
    assertEquals("e", StringUtil.commonSuffix("come", "cone"));
    assertEquals("", StringUtil.commonSuffix("CAPS", "caps"));
    assertEquals("arse", StringUtil.commonSuffix("parse", "arse"));
    assertEquals("", StringUtil.commonSuffix("LaTeX", "LaTex"));
    assertEquals("", StringUtil.commonSuffix("LaTex", "LaTeX"));
    assertEquals("s", StringUtil.commonSuffix("locks", "lockss"));
    // Try comparing ASCII chars with same char value
    assertEquals("", StringUtil.commonSuffix("\t", "9"));
    assertEquals("", StringUtil.commonSuffix("\0", "0"));
  }

  public void testReplaceStringNonExistingSubstring(){
    String testStr = "blahTestblah";
    // if no substitutions are made, original string should be returned
    assertSame(testStr, StringUtil.replaceString(testStr, "!", "8"));
    assertSame(testStr, StringUtil.replaceString(testStr, "lah1", "8"));
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
    assertSame("aabbcc", StringUtil.replaceFirst("aabbcc", "dd", "bb"));

    String s = "foo";
    assertSame(s, StringUtil.replaceFirst(s, "123", "123"));
    assertSame(s, StringUtil.replaceFirst(s, "", "123"));
  }

  public void testReplaceLast() {
    assertEquals("aabbcc", StringUtil.replaceLast("aaddcc", "dd", "bb"));
    assertEquals("aabb", StringUtil.replaceLast("aadd", "dd", "bb"));
    assertEquals("aaddccbb", StringUtil.replaceLast("aaddccdd", "dd", "bb"));
    assertEquals("aaddbbcc", StringUtil.replaceLast("aaddddcc", "dd", "bb"));
    assertSame("aabbcc", StringUtil.replaceLast("aabbcc", "dd", "bb"));

    String s = "foo";
    assertSame(s, StringUtil.replaceLast(s, "123", "123"));
    assertSame(s, StringUtil.replaceLast(s, "", "123"));
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
    assertIsomorphic(ListUtil.list("foo", "bar"),
		     StringUtil.breakAt("foo bar ddd eee fff", ' ', 2));

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

  public void testBreakAtString() {
    Vector v = new Vector();
    assertEquals(v, StringUtil.breakAt(null, " "));
    assertEquals(v, StringUtil.breakAt(null, "   "));
    assertEquals(v, StringUtil.breakAt("", " "));
    assertEquals(v, StringUtil.breakAt("", "   "));
    assertIsomorphic(ListUtil.list("foo"), StringUtil.breakAt("foo", " "));
    assertIsomorphic(ListUtil.list("foo"), StringUtil.breakAt("foo", "  "));
    assertIsomorphic(ListUtil.list("foo", "bar"),
		     StringUtil.breakAt("foo bar", " "));
    assertIsomorphic(ListUtil.list("foo", "bar"),
		     StringUtil.breakAt("fooXYbar", "XY"));
    assertIsomorphic(ListUtil.list("foo", "", "bar"),
		     StringUtil.breakAt("fooXYXYbar", "XY"));
    assertIsomorphic(ListUtil.list("foo", ""),
		     StringUtil.breakAt("foo  ", "  "));
    assertIsomorphic(ListUtil.list("", "foo"),
		     StringUtil.breakAt(" foo", " "));
    assertIsomorphic(ListUtil.list("", "foo"),
		     StringUtil.breakAt("  foo", "  "));
    assertIsomorphic(ListUtil.list("foo", "bar"),
		     StringUtil.breakAt("fooZZbarZZddd", "ZZ", 2));

    assertIsomorphic(ListUtil.list("", ""),
		     StringUtil.breakAt("XX", "XX", -1, false));
    assertIsomorphic(ListUtil.list(),
		     StringUtil.breakAt("+", "+", -1, true));
    assertIsomorphic(ListUtil.list("", "foo"),
		     StringUtil.breakAt("+foo", "+", -1, false));
    assertIsomorphic(ListUtil.list("foo"),
		     StringUtil.breakAt("+foo", "+", -1, true));
    assertIsomorphic(ListUtil.list("foo", ""),
		     StringUtil.breakAt("foo+", "+", -1, false));
    assertIsomorphic(ListUtil.list("foo"),
		     StringUtil.breakAt("foo+", "+", -1, true));
    assertIsomorphic(ListUtil.list("foo "),
		     StringUtil.breakAt("foo +", "+", -1, true, false));
    assertIsomorphic(ListUtil.list("foo"),
		     StringUtil.breakAt("foo +", "+", -1, true, true));
    assertIsomorphic(ListUtil.list("foo"),
		     StringUtil.breakAt("foo + ", "+", -1, true, true));
    assertIsomorphic(ListUtil.list("foo ", " "),
		     StringUtil.breakAt("foo + ", "+", -1, true, false));

  }

  static final String sbefore = "Here is some weird text. It has \n\n\r\n\r\r newlines.";
  static final String safter =  "Here is some weird text. It has \n\n\n\n\n newlines.";

  public void testGetLineReader() throws Exception {
    assertReaderMatchesString(safter, StringUtil.getLineReader(new StringReader(sbefore)));
  }

  void testGetLineContinuationReader(String exp, String src,
				     int bufmin, int bufmax)
      throws IOException {
    for (int size = bufmin; size <= bufmax; size++) {
      Reader rdr = new StringReader(src);
      assertReaderMatchesString(exp,
				StringUtil.getLineContinuationReader(rdr),
				size);
    }
  }

  public void testGetLineContinuationReader() throws Exception {
    // important to test all the buffer size edge cases
    testGetLineContinuationReader("abc", "abc", 1, 4);
    testGetLineContinuationReader("a\nbc", "a\nbc", 1, 5);
    testGetLineContinuationReader("abc", "a\\\nbc", 1, 5);
    testGetLineContinuationReader("abc", "\\\nabc", 1, 5);
    testGetLineContinuationReader("abc", "\\\nabc\\\n", 1, 7);
    testGetLineContinuationReader("a\nbc", "a\\\n\nbc", 1, 6);
    testGetLineContinuationReader("a\nbc", "a\n\\\nbc", 1, 6);
    testGetLineContinuationReader("a\\bc", "a\\bc", 1, 5);
    testGetLineContinuationReader("a\\\\bc", "a\\\\bc", 1, 6);
  }

  void assertLineSequence(List<String> exp, String str) throws IOException {
    BufferedReader rdr = new BufferedReader(new StringReader(str));
    List<String> act = new ArrayList<String>();
    String s;
    while ((s = StringUtil.readLineWithContinuation(rdr)) != null) {
      act.add(s);
    }
    assertEquals(exp, act);
  }

  public void testReadLineWithContinuation() throws Exception {
    assertLineSequence(ListUtil.list(), "");
    assertLineSequence(ListUtil.list(" "), " ");
    assertLineSequence(ListUtil.list("foo"), "foo");
    assertLineSequence(ListUtil.list("foo"), "foo\n");
    assertLineSequence(ListUtil.list("foo", "bar"), "foo\nbar");
    assertLineSequence(ListUtil.list("foo"), "foo\\");
    assertLineSequence(ListUtil.list("foo"), "foo\\\n");
    assertLineSequence(ListUtil.list("foobar"), "foo\\\nbar");
    assertLineSequence(ListUtil.list("foo\\bar"), "foo\\bar");
  }

  void assertLinesSequence(List<String> exp, String str, int maxSize)
      throws IOException {
    BufferedReader rdr = new BufferedReader(new StringReader(str));
    List<String> act = new ArrayList<String>();
    StringBuilder sb = new StringBuilder();
    while (StringUtil.readLinesWithContinuation(rdr, sb, maxSize)) {
      act.add(sb.toString());
      sb.setLength(0);
    }
    assertEquals(exp, act);
  }

  public void testReadLinesWithContinuation() throws Exception {
    assertLinesSequence(ListUtil.list(), "", 1);
    assertLinesSequence(ListUtil.list(" \n"), " ", 10);
    assertLinesSequence(ListUtil.list("foo\nbar\n"), "foo\nbar", 100);
    assertLinesSequence(ListUtil.list("foo\nbar\n"), "foo\nbar\n", 100);
    assertLinesSequence(ListUtil.list("foo\n", "bar\n"), "foo\nbar", 3);
    assertLinesSequence(ListUtil.list("foo\n", "bar\n"), "foo\nbar\n", 3);
    assertLinesSequence(ListUtil.list("foo\n", "bar\n"), "fo\\\no\nbar\n", 3);
  }

  public void testFromReader() throws Exception {
    Reader r = new InputStreamReader(new StringInputStream(sbefore));
    assertEquals(safter, StringUtil.fromReader(r));
  }

  String makeTestString(int approxLen) {
    String s = "a\nsdfjsfdsdfkljasdlkjasdflkjasdlfkjasdflkjasldkfjasd";
    if (s.length() >= approxLen) return s;
    StringBuilder sb = new StringBuilder(approxLen + 100);
    while (sb.length() < approxLen) {
      sb.append(s);
    }
    return sb.toString();
  }

  public void testFromReaderUnderLimit() throws Exception {
    String s = makeTestString(100000);
    Reader r = new InputStreamReader(new StringInputStream(s));
    assertEquals(s, StringUtil.fromReader(r, 200000));
  }

  public void testFromReaderOverLimit() throws Exception {
    String s = makeTestString(100000);
    Reader r = new InputStreamReader(new StringInputStream(s));
    try {
      StringUtil.fromReader(r, 50000);
      fail("Should have thrown, over max size");
    } catch (StringUtil.FileTooLargeException e) {
    }
  }

  public void testFillFromReader() throws Exception {
    String s = makeTestString(10000);
    int slen = s.length();
    Reader r = new StringReader(s);
    StringBuilder sb = new StringBuilder();
    assertTrue(StringUtil.fillFromReader(r, sb, 1000));
    assertEquals(1000, sb.length());
    assertTrue(StringUtil.fillFromReader(r, sb, 1000));
    assertEquals(2000, sb.length());
    assertTrue(StringUtil.fillFromReader(r, sb, 1000000));
    assertEquals(slen, sb.length());
    assertEquals(s, sb.toString());
  }

  public void testFromInputStream() throws Exception {
    String s = "asdfjsfd";
    assertEquals(s, StringUtil.fromInputStream(new StringInputStream(s)));
  }

  public void testToOutputStream() throws Exception {
    String s = "asdfjsfd";
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StringUtil.toOutputStream(baos, s);
    assertEquals(s, baos.toString());
  }

  public void testToWriter() throws Exception {
	String s = "asdfjsfd";
	StringWriter swrtr = new StringWriter();
	StringUtil.toWriter(swrtr, s);
	assertEquals(s, swrtr.toString());
  }

  public void testCompareNullLow() {
    assertEquals(0, StringUtil.compareToNullLow(null, null));
    assertEquals(-1, StringUtil.compareToNullLow(null, "a"));
    assertEquals(1, StringUtil.compareToNullLow("a", null));
    assertEquals(1, StringUtil.compareToNullLow("b", "a"));
    assertEquals(-1, StringUtil.compareToNullLow("a", "b"));
    assertEquals(0, StringUtil.compareToNullLow("a", "a"));
  }

  public void testCompareNullHigh() {
    assertEquals(0, StringUtil.compareToNullHigh(null, null));
    assertEquals(1, StringUtil.compareToNullHigh(null, "a"));
    assertEquals(-1, StringUtil.compareToNullHigh("a", null));
    assertEquals(1, StringUtil.compareToNullHigh("b", "a"));
    assertEquals(-1, StringUtil.compareToNullHigh("a", "b"));
    assertEquals(0, StringUtil.compareToNullHigh("a", "a"));
  }

  static public void assertPreOrder(String s1, String s2) {
    assertTrue(StringUtil.preOrderCompareTo(s1, s2) < 0);
    assertTrue(StringUtil.preOrderCompareTo(s2, s1) > 0);
  }

  static public void assertPreOrderNullHigh(String s1, String s2) {
    assertTrue(StringUtil.preOrderCompareToNullHigh(s1, s2) < 0);
    assertTrue(StringUtil.preOrderCompareToNullHigh(s2, s1) > 0);
  }

  public void testPreOrderCompareTo() {
    assertEquals(0, StringUtil.preOrderCompareTo("", ""));
    assertEquals(0, StringUtil.preOrderCompareTo("a", "a"));
    assertEquals(0, StringUtil.preOrderCompareTo("uni-\u00eb-code",
						 "uni-\u00eb-code"));
    assertEquals(0, StringUtil.preOrderCompareTo("1/", "1/"));
    assertEquals(0, StringUtil.preOrderCompareTo("1/2.3", "1/2.3"));
    assertPreOrder("a", "b");
    assertPreOrder("", "1");
    assertPreOrder("abc", "abc/");
    assertPreOrder("abc", "abc.");

    // This is where preOrderCompareTo differs from natural String order
    assertFalse("a/".compareTo("a.") < 0);
    assertPreOrder("a/", "a.");
    assertFalse("a/b".compareTo("a.b") < 0);
    assertPreOrder("a/b", "a.b");
  }

  public void testPreOrderCompareToNullHigh() {
    assertEquals(0, StringUtil.preOrderCompareToNullHigh("", ""));
    assertEquals(0, StringUtil.preOrderCompareToNullHigh("a", "a"));
    assertEquals(0, StringUtil.preOrderCompareToNullHigh(null, null));
    assertEquals(0, StringUtil.preOrderCompareToNullHigh("1/2.3", "1/2.3"));
    assertPreOrderNullHigh("a/b", null);
    assertPreOrderNullHigh("a.b", null);

    assertTrue(StringUtil.compareToNullHigh("a/", "a.") > 0);
    assertPreOrderNullHigh("a/", "a.");
    assertTrue(StringUtil.compareToNullHigh("a/b", "a.b") > 0);
    assertPreOrderNullHigh("a/b", "a.b");
  }

  public void testPreOrderComparator() {
    Collection coll = new TreeSet(StringUtil.PRE_ORDER_COMPARATOR);
    coll.addAll(ListUtil.list("http://foo:80/",
			      "http://foo/a/b.c",
			      "http://foo/a/b/c",
			      "",
			      "http://foo/a.b/c",
			      "http://foo/a/b",
			      "http://foo/a/",
			      "http://foo/"));
    assertIsomorphic(ListUtil.list("",
				   "http://foo/",
				   "http://foo/a/",
				   "http://foo/a/b",
				   "http://foo/a/b/c",
				   "http://foo/a/b.c",
				   "http://foo/a.b/c",
				   "http://foo:80/"),
		     coll);
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

  public void testCaseIndependentComparator() {
    assertIsomorphic(ListUtil.list("Aardvark", "frog", "Goblin",
				   "homeopath", "homeoPathIc"),
		     StringUtil.caseIndependentSortedSet(ListUtil.list("homeoPathIc", "frog", "homeopath",
								       "Goblin", "Aardvark")));
  }

  public void testIsNullString() {
    assertTrue(StringUtil.isNullString(null));
    assertTrue(StringUtil.isNullString(""));
    assertTrue(StringUtil.isNullString(new String()));
    assertFalse(StringUtil.isNullString(" "));
  }

  public void testIsAscii() {
    try {
      StringUtil.isAscii(null);
      fail("isAscii(null) should throw");
    } catch (NullPointerException e) {
    }
    assertTrue(StringUtil.isAscii(""));
    assertTrue(StringUtil.isAscii("abczABCZ0129"));
    assertTrue(StringUtil.isAscii(" &*()!@#$%-_\\|'\";:/?.>,<]}[{~`+="));
    assertFalse(StringUtil.isAscii("entrée"));
  }

  void assertCopy(String exp, String orig, int from, int to, int len) {
    StringBuilder sb = new StringBuilder(orig);
    StringUtil.copyChars(sb, from, to, len);
    assertEquals(exp, sb.toString());
  }

  public void testCopyChars() {
    assertCopy("foo", "foo", 0, 0, 0);
    assertCopy("foofoo", "barfoo", 3, 0, 3);
    assertCopy("barbar", "barfoo", 0, 3, 3);
    assertCopy("bfoooo", "barfoo", 3, 1, 3);
    assertCopy("bfofoo", "barfoo", 3, 1, 2);
    assertCopy("bararf", "barfoo", 1, 3, 3);
    assertCopy("bararo", "barfoo", 1, 3, 2);
  }

  public void testGensym() {
    String base = "foo";
    String g1 = StringUtil.gensym(base);
    String g2 = StringUtil.gensym(base);
    assertTrue(g1.startsWith(base));
    assertNotEquals(g1, g2);
  }

  public void testTab() {
    assertEquals("", StringUtil.tab(0));
    assertEquals(" ", StringUtil.tab(1));
    assertEquals("       ", StringUtil.tab(7));
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

  public void testSanitizeToIdentifier() {
    assertEquals("foobar123", StringUtil.sanitizeToIdentifier("foobar123"));
    assertEquals("fooBAR", StringUtil.sanitizeToIdentifier("foo.BAR"));
    assertEquals("fooBar_",
		 StringUtil.sanitizeToIdentifier(" +.!|,foo.Bar?<>_"));
  }

  public void testBasename() {
    assertEquals(null, StringUtil.basename(""));
    assertEquals(null, StringUtil.basename("/"));
    assertEquals(null, StringUtil.basename("http://"));
    assertEquals("http:", StringUtil.basename("http:/"));

    assertEquals("foo", StringUtil.basename("foo"));
    assertEquals("foo.bar", StringUtil.basename("foo.bar"));
    assertEquals("bar", StringUtil.basename("foo/bar"));
    assertEquals("bar.txt", StringUtil.basename("foo/bar.txt"));
    assertEquals("article.pdf",
                 StringUtil.basename("https://host.tld/foo/article.pdf"));
    assertEquals("article.pdf",
                 StringUtil.basename("https://host.tld/foo/article.pdf?a=b&ccc=dddd"));
    assertEquals("article.pdf",
                 StringUtil.basename("https://host.tld/foo/article.pdf?"));
    assertEquals("download",
                 StringUtil.basename("https://here.there/articles/10.18352/ts.327/galley/319/download/"));
    assertEquals("download",
                 StringUtil.basename("https://here.there/articles/10.18352/ts.327/galley/319/download/?foo=bar"));
    assertEquals("download",
                 StringUtil.basename("https://here.there/articles/10.18352/ts.327/galley/319/download/?"));
  }

  public void testStackTraceString() {
    String s = StringUtil.stackTraceString(new Exception());
    String exp = "java.lang.Exception" + Constants.EOL +
      "\tat org.lockss.util.TestStringUtil.testStackTraceString(TestStringUtil.java:";
    assertTrue(s.startsWith(exp));
  }

  public void testTrimBlankLines() {
    assertEquals("foo", StringUtil.trimBlankLines("foo"));
    assertEquals("foo", StringUtil.trimBlankLines("\nfoo"));
    assertEquals("foo", StringUtil.trimBlankLines("\nfoo\n"));
    assertEquals("foo", StringUtil.trimBlankLines("\n\nfoo\n\n"));
    assertEquals("foo\nbar", StringUtil.trimBlankLines("\n\nfoo\nbar\n\n"));
  }

  public void testNormalizeEols() {
    assertSame("foo", StringUtil.normalizeEols("foo"));

    assertEquals("\nfoo", StringUtil.normalizeEols("\nfoo"));
    assertEquals("\nfoo", StringUtil.normalizeEols("\rfoo"));
    assertEquals("\nfoo", StringUtil.normalizeEols("\r\nfoo"));
    assertEquals("\nf oo", StringUtil.normalizeEols("\nf oo"));

    assertEquals("f\noo", StringUtil.normalizeEols("f\noo"));
    assertEquals("f\noo", StringUtil.normalizeEols("f\roo"));
    assertEquals("f\noo", StringUtil.normalizeEols("f\r\noo"));
    assertEquals("f\n oo", StringUtil.normalizeEols("f\n oo"));

    assertEquals("foo\n", StringUtil.normalizeEols("foo\n"));
    assertEquals("foo\n", StringUtil.normalizeEols("foo\r"));
    assertEquals("foo\n", StringUtil.normalizeEols("foo\r\n"));
    assertEquals("foo \n ", StringUtil.normalizeEols("foo \r "));

    assertEquals("\nf\noo\n", StringUtil.normalizeEols("\nf\noo\r"));
    assertEquals("\nf\noo\n", StringUtil.normalizeEols("\nf\roo\r\n"));
    assertEquals("\nf\noo\n", StringUtil.normalizeEols("\r\nf\r\noo\r\n"));
    assertEquals("\nf\n oo\n", StringUtil.normalizeEols("\nf\n oo\r"));
  }

  public void testTrimLeadingWhitespace() {
    assertSame("foo",
	       StringUtil.trimNewlinesAndLeadingWhitespace("foo"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\noo"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\roo"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\r\noo"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\n oo"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\n\r oo"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\n   oo"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\n\too"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\n\t \too"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\n \t \t oo"));
    assertEquals("foo",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f\n\n\t\too"));
    // does not trim trailing whitespace (other than newlines)
    assertEquals("f  oo ",
		 StringUtil.trimNewlinesAndLeadingWhitespace("f  \n\n\t\too "));
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
    assertEquals(1234, StringUtil.parseTimeInterval("1234ms"));
    assertEquals(StringUtil.parseTimeInterval("60s"),
		 StringUtil.parseTimeInterval("1m"));
    assertEquals(StringUtil.parseTimeInterval("120m"),
		 StringUtil.parseTimeInterval("2h"));
    assertEquals(StringUtil.parseTimeInterval("72h"),
		 StringUtil.parseTimeInterval("3d"));
    assertEquals(StringUtil.parseTimeInterval("14d"),
		 StringUtil.parseTimeInterval("2w"));
    assertEquals(StringUtil.parseTimeInterval((365 * 3) + "d"),
		 StringUtil.parseTimeInterval("3y"));
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

  public void testTimeIntervalToString() throws Exception {
    assertEquals("0ms", StringUtil.timeIntervalToString(0));
    assertEquals("1000ms", StringUtil.timeIntervalToString(SECOND));
    assertEquals("-1000ms", StringUtil.timeIntervalToString(- SECOND));
    assertEquals("9000ms", StringUtil.timeIntervalToString(SECOND * 9));
    assertEquals("-9000ms", StringUtil.timeIntervalToString(- SECOND * 9));
    assertEquals("10s", StringUtil.timeIntervalToString(SECOND * 10));
    assertEquals("1m0s", StringUtil.timeIntervalToString(MINUTE));
    assertEquals("1h0m0s", StringUtil.timeIntervalToString(HOUR));
    assertEquals("2d3h0m",
		 StringUtil.timeIntervalToString(DAY * 2 + HOUR * 3));
    assertEquals("20d23h0m",
		 StringUtil.timeIntervalToString(WEEK * 3 - (HOUR * 1)));
    assertEquals("-20d23h0m",
		 StringUtil.timeIntervalToString(- (WEEK * 3 - (HOUR * 1))));
    assertEquals("3w0d0h", StringUtil.timeIntervalToString(WEEK * 3));
  }

  public void testTimeIntervalToLong() throws Exception {
    assertEquals("0 seconds", StringUtil.timeIntervalToLongString(0));
    assertEquals("1 second", StringUtil.timeIntervalToLongString(SECOND));
    assertEquals("-1 second", StringUtil.timeIntervalToLongString(- SECOND));
    assertEquals("9 seconds", StringUtil.timeIntervalToLongString(SECOND * 9));
    assertEquals("-9 seconds",
		 StringUtil.timeIntervalToLongString(- SECOND * 9));
    assertEquals("10 seconds",
		 StringUtil.timeIntervalToLongString(SECOND * 10));
    assertEquals("1 minute", StringUtil.timeIntervalToLongString(MINUTE));
    assertEquals("1 hour", StringUtil.timeIntervalToLongString(HOUR));
    assertEquals("2 days, 3 hours",
		 StringUtil.timeIntervalToLongString(DAY * 2 + HOUR * 3));
    assertEquals("20 days, 23 hours, 45 minutes",
		 StringUtil.timeIntervalToLongString(WEEK * 3 - (HOUR * 1)
						     + MINUTE * 45));
    assertEquals("12 days, 13 minutes, 1 second",
		 StringUtil.timeIntervalToLongString(DAY * 12 + MINUTE * 13
						     + SECOND));
    assertEquals("21 days", StringUtil.timeIntervalToLongString(WEEK * 3));
  }

  public void testParseSize() throws Exception {
    long k = 1024;
    long m = k*k;
    assertEquals(0, StringUtil.parseSize("0"));
    assertEquals(0, StringUtil.parseSize(" 0 "));
    assertEquals(0, StringUtil.parseSize("0b"));
    assertEquals(0, StringUtil.parseSize("0 B"));
    assertEquals(0, StringUtil.parseSize("0GB"));
    assertEquals(0, StringUtil.parseSize("0 gb"));
    assertEquals(1000, StringUtil.parseSize("1000"));
    assertEquals(1000, StringUtil.parseSize("1000b"));
    assertEquals(123*k, StringUtil.parseSize("123KB"));
    assertEquals((long)(4.2*m), StringUtil.parseSize("4.2mb"));
    assertEquals(45*m*k, StringUtil.parseSize("45gb"));
    assertEquals((long)(66.0*m*m), StringUtil.parseSize("66 tb"));
    assertEquals(666*m*m*k, StringUtil.parseSize("666 pb"));
    try {
      StringUtil.parseSize("2x");
      fail("should have thrown NumberFormatException");
    } catch (NumberFormatException e) {
    }
    try {
      StringUtil.parseSize("");
      fail("should have thrown NumberFormatException");
    } catch (NumberFormatException e) {
    }
    try {
      StringUtil.parseSize("4.4.4");
      fail("should have thrown NumberFormatException");
    } catch (NumberFormatException e) {
    }
  }

  public void testSizeKBToString() throws Exception {
    assertEquals("123KB", StringUtil.sizeKBToString(123));
    assertEquals("123MB", StringUtil.sizeKBToString(123 * 1024));
    assertEquals("3.7MB", StringUtil.sizeKBToString((long)(3.7 * 1024)));
    assertEquals("3.7GB", StringUtil.sizeKBToString((long)(3.7 * 1024*1024)));
    assertEquals("432GB", StringUtil.sizeKBToString(432 * 1024*1024));
    assertEquals("3.7TB", StringUtil.sizeKBToString((long)(3.7 * 1024*1024*1024)));
  }

  public void testSizeToString() throws Exception {
    assertEquals("0B", StringUtil.sizeToString(0));
    assertEquals("123B", StringUtil.sizeToString(123));
    assertEquals("1023B", StringUtil.sizeToString(1023));
    assertEquals("1.0KB", StringUtil.sizeToString(1024));
    assertEquals("1.0KB", StringUtil.sizeToString(1025));
    assertEquals("123KB", StringUtil.sizeToString(123*1024));
    assertEquals("123MB", StringUtil.sizeToString(123*1024*1024));
  }

  public void testByteRate() throws Exception {
    assertEquals("0", StringUtil.byteRateToString(0, 1000));
    assertEquals("(undefined)", StringUtil.byteRateToString(1000, 0));
    assertEquals("1000B/s", StringUtil.byteRateToString(1000, 1000));
    assertEquals("977KB/s", StringUtil.byteRateToString(1000, 1));
    assertEquals("3.26KB/s", StringUtil.byteRateToString(1000, 300));
    assertEquals("12.01KB/s", StringUtil.byteRateToString(123, 10));
    assertEquals("1.2KB/s", StringUtil.byteRateToString(123, 100));
    assertEquals("97.66KB/s", StringUtil.byteRateToString(100000, 1000));
    assertEquals("9.54MB/s", StringUtil.byteRateToString(100000, 10));
    assertEquals("118MB/s", StringUtil.byteRateToString(1234567, 10));
    assertEquals("11.77MB/s", StringUtil.byteRateToString(1234567, 100));
    assertEquals("1.18MB/s", StringUtil.byteRateToString(1234567, 1000));
    assertEquals("8.1B/s", StringUtil.byteRateToString(10000, 1234567));
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

  public void testEndsWithIgnoreCase() {
    assertTrue(StringUtil.endsWithIgnoreCase("", ""));
    assertTrue(StringUtil.endsWithIgnoreCase("1", ""));
    assertTrue(StringUtil.endsWithIgnoreCase("1", "1"));
    assertTrue(StringUtil.endsWithIgnoreCase("foo.opt", ".opt"));
    assertFalse(StringUtil.endsWithIgnoreCase("", "2"));
    assertFalse(StringUtil.endsWithIgnoreCase("1", "2"));
    assertFalse(StringUtil.endsWithIgnoreCase("21", "2"));
    assertFalse(StringUtil.endsWithIgnoreCase("foo.opt", "xopt"));
  }

  public void testStartsWithIgnoreCase() {
    assertTrue(StringUtil.startsWithIgnoreCase("", ""));
    assertTrue(StringUtil.startsWithIgnoreCase("1", ""));
    assertTrue(StringUtil.startsWithIgnoreCase("1", "1"));
    assertTrue(StringUtil.startsWithIgnoreCase("foo.opt", "foo."));
    assertFalse(StringUtil.startsWithIgnoreCase("", "2"));
    assertFalse(StringUtil.startsWithIgnoreCase("1", "2"));
    assertFalse(StringUtil.startsWithIgnoreCase("12", "2"));
    assertFalse(StringUtil.startsWithIgnoreCase("foo.opt", "foox"));
  }

  public void testHasRepeatedChar() {
    assertFalse(StringUtil.hasRepeatedChar(""));
    assertFalse(StringUtil.hasRepeatedChar("a"));
    assertFalse(StringUtil.hasRepeatedChar("ab"));
    assertFalse(StringUtil.hasRepeatedChar("aba"));
    assertFalse(StringUtil.hasRepeatedChar("abab"));
    assertTrue(StringUtil.hasRepeatedChar("aa"));
    assertTrue(StringUtil.hasRepeatedChar("aab"));
    assertTrue(StringUtil.hasRepeatedChar("baa"));
    assertTrue(StringUtil.hasRepeatedChar("baab"));
    assertTrue(StringUtil.hasRepeatedChar("aaa"));
    assertFalse(StringUtil.hasRepeatedChar("Fran\u00E7ais"));
    assertTrue(StringUtil.hasRepeatedChar("Fran\u00E7\u00E7ais"));
  }

  public void testTitleCase() {
    assertEquals("", StringUtil.titleCase(""));
    String txt1 = "this is 8 words.  can it be handled?";
    String txt2 = "This Is 8 Words.  Can It Be Handled?";
    assertEquals(txt2, StringUtil.titleCase(txt1));
    assertEquals(txt2, StringUtil.titleCase(txt1, ' '));

    assertEquals("Content-Type", StringUtil.titleCase("content-type", '-'));
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

  public void testFromFileFile() {
    try {
      String txt = "Another string.";
      File file = new File(getTempDir(), "test.txt");
      FileWriter fw = new FileWriter(file);
      fw.write(txt);
      fw.close();
      String readtxt = StringUtil.fromFile(file);
      assertEquals(txt, readtxt);
      file.delete();
    }
    catch (IOException e) {
      fail(e.getMessage());
    }
  }

  public void testToFile() throws Exception {
    String txt = "Here is some weird text.\nIt has !@#$%^&*()214 in it.";
    File file = new File(getTempDir(), "foo.txt");
    StringUtil.toFile(file, txt);
    assertReaderMatchesString(txt, new FileReader(file));
  }

  public void testUpToFinal() {
    assertEquals("foo", StringUtil.upToFinal("foo.bar", "."));
    assertEquals("foo.a", StringUtil.upToFinal("foo.a.bar", "."));
    assertEquals("foo", StringUtil.upToFinal("foo", "."));
    assertEquals("", StringUtil.upToFinal(".foo", "."));
  }

  public void testRemoveTrailing() {
    assertEquals("", StringUtil.removeTrailing("", "/"));
    assertEquals("foo", StringUtil.removeTrailing("foo", "."));
    assertEquals("foo", StringUtil.removeTrailing("foo/", "/"));
    assertEquals("foo", StringUtil.removeTrailing("foobar", "bar"));
    assertEquals("fo/o", StringUtil.removeTrailing("fo/o/", "/"));
    assertEquals("/o", StringUtil.removeTrailing("/o/", "/"));
    assertEquals("", StringUtil.removeTrailing("/", "/"));
  }

  public void testNthIndexOf() {
    assertEquals(-1, StringUtil.nthIndexOf(1, "xyz", "q"));
    assertEquals(-1, StringUtil.nthIndexOf(11, "xxxyzx", "x"));
    assertEquals(0, StringUtil.nthIndexOf(1, "xyz", "x"));
    assertEquals(1, StringUtil.nthIndexOf(2, "xxyz", "x"));

    assertEquals(7, StringUtil.nthIndexOf(4, "xyzxyzxxxyz", "x"));
    assertEquals(9, StringUtil.nthIndexOf(2, "xyzzzzzysxyz", "xyz"));
  }

  public void testProtectedDivide() {
    assertEquals("2", StringUtil.protectedDivide(10, 5));
    assertEquals("inf", StringUtil.protectedDivide(10, 0));
    assertEquals("infinite", StringUtil.protectedDivide(10, 0, "infinite"));
  }

  public void testFindStringNoReader() throws IOException {
    try {
      StringUtil.containsString(null, "Random String");
      fail("StringUtil.containsString(null, String) should throw a NPE");
    } catch (NullPointerException ex) {
    }
  }

  public void testFindStringNoString() throws IOException {
    try {
      StringUtil.containsString(new StringReader("Random String"), null);
      fail("StringUtil.containsString(Reader, null) should throw a NPE");
    } catch (NullPointerException ex) {
    }
  }

  public void testFindString() throws IOException {
    String stringToFind = "special string";
    String readerStr = "Blah blah blah "+stringToFind+"blah blah";
    assertTrue("Didn't find string when it should",
	       StringUtil.containsString(new StringReader(readerStr),
					 stringToFind));
  }

  public void testFindStringBeginning() throws IOException {
    String stringToFind = "special string";
    String readerStr = stringToFind + "Blah blah blah blah blah";
    assertTrue("Didn't find string when it should",
	       StringUtil.containsString(new StringReader(readerStr),
					 stringToFind));
  }

  public void testFindStringEndShort() throws IOException {
    String stringToFind = "special string";
    String readerStr = "Blah blah blah "+stringToFind;
    assertTrue("Didn't find string when it should",
	       StringUtil.containsString(new StringReader(readerStr),
					 stringToFind));
  }

  public void testFindStringEndLong() throws IOException {
    String padding = org.apache.commons.lang3.StringUtils.repeat("X ", 80);
    String stringToFind = "This permision string is longer than 20 characters";
    String readerStr = padding + stringToFind;
    assertTrue("Didn't find string when it should",
	       StringUtil.containsString(new StringReader(readerStr),
					 stringToFind, false, 100));
  }

  //To make sure searching for an empty string throws
  public void testFindStringBlankString() throws IOException {
    try {
      StringUtil.containsString(new StringReader("Blah blah blah"), "");
      fail("Search for empty string should throw");
    } catch (IllegalArgumentException ex) {
    }
  }

  public void testFindStringBlankStringEmptyReader() throws IOException {
    try {
      StringUtil.containsString(new StringReader(""), "");
      fail("Search for empty string should throw");
    } catch (IllegalArgumentException ex) {
    }
  }

  public void testFindString0Buffer() throws IOException {
    try {
      StringUtil.containsString(new StringReader("Blah blah blah"), "blah", 0);
      fail("Calling containsString with a 0 buffer should throw");
    } catch (IllegalArgumentException ex) {
    }
  }

  //To make sure searching and empty reader fails, but doesn't throw
  public void testFindStringEmptyReader() throws IOException {
    assertFalse("Search of empty reader should always fail",
		StringUtil.containsString(new StringReader(""), "blah blah"));
  }

  public void testContainsStringMetaChars() throws IOException {
    assertFalse("Incorrectly matched metachar",
		StringUtil.containsString(new StringReader("test blah test"),
					  "bl.h"));
    assertTrue("Didn't match metachar",
		StringUtil.containsString(new StringReader("test [acb]\\s test"),
					  "[acb]\\s"));
  }

  public void testContainsStringDefaultCaseSensitive() throws IOException {
    assertFalse("Incorrectly matched string ignoring case by default",
		StringUtil.containsString(new StringReader("Test BlaH test"),
					  "blah"));
    assertFalse("Incorrectly matched string ignoring case by default",
		StringUtil.containsString(new StringReader("Test BlaH test"),
					  "BLAH"));
  }

  public void testContainsStringParamCaseSensitive() throws IOException {
    assertFalse("Incorrectly matched string ignoring case",
		StringUtil.containsString(new StringReader("Test BlaH test"),
					  "blah", false));

    assertFalse("Incorrectly matched string ignoring case",
		StringUtil.containsString(new StringReader("Test BlaH test"),
					  "BLAH", false));
  }

  public void testContainsStringParamCaseInsensitive() throws IOException {
    assertTrue("Didn't matched string ignoring case",
	       StringUtil.containsString(new StringReader("Test BlaH test"),
					 "blah", true));
    assertTrue("Didn't matched string ignoring case",
	       StringUtil.containsString(new StringReader("Test BlaH test"),
					 "BLAH", true));
  }

  public void testContainsStringPartialMatchPartialBuffer() throws IOException {
    String testStr = "123456abcdefghi1234";
    String searchStr = "abcdefghi";
    assertTrue("Didn't find string when it should",
	       StringUtil.containsString(new StringReader(testStr),
					 searchStr, 10));
  }

  public void testContainsStringPartialMatchFullBuffer() throws IOException {
    String testStr = "123456abcdefghi1234567890";
    String searchStr = "abcdefghi";
    assertTrue("Didn't find string when it should",
	       StringUtil.containsString(new StringReader(testStr),
					 searchStr, 10));
  }

  public void testContainsStringPartialMatchFullBufferCaseInsensitive ()
      throws IOException {
    String testStr = "123456abcdefghi1234567890";
    String searchStr = "abcdeFGHI";
    assertTrue("Didn't find string when it should",
	       StringUtil.containsString(new StringReader(testStr),
					 searchStr, true, 10));
  }

  // This test fails with the implementation of containsString() that uses
  // Boyer-Moore
  public void testContainsStringPartialMatchFullBufferCaseSensitive ()
      throws IOException {
    String testStr = "123456abcdefghi1234567890";
    String searchStr = "abcdeFGHI";
    assertFalse("Found string when it shouldn't",
	       StringUtil.containsString(new StringReader(testStr),
					 searchStr, 10));
    assertTrue("Didn't find string when it should",
	       StringUtil.containsString(new StringReader(testStr),
					 searchStr.toLowerCase(), 10));
  }


  public void testContainsStringPartialMatchIncomplete() throws IOException {
    String testStr = "123456aaaaa";
    String searchStr = "aaaaaaaaa";
    assertFalse("Found string when it shouldn't",
		StringUtil.containsString(new StringReader(testStr),
					  searchStr, 10));
}

  public void testFindStringRequiresBackup() throws IOException {
    String stringToFind = "ab";
    String readerStr = "aab";
    assertTrue("Didn't find string when it should",
               StringUtil.containsString(new StringReader(readerStr),
                                         stringToFind));
  }

  public void testFindStringMatchInMiddle() throws IOException {
    String stringToFind = "abcdef";
    String readerStr = "pwpwpwallplplplplp";
    assertFalse("Found string when it shouldn't",
                StringUtil.containsString(new StringReader(readerStr),
                                          stringToFind, 7));
  }


  //network streams can underfill buffers; this test make sure we
  //handle a situation when the reader will return a series of small chars
  public void testFindStringUnderfullsBuffer() throws IOException {
    String stringToFind = "abcdefgh";
    String readerStr = "blah abcdefgh blah";
    assertTrue("Didn't find string when it should",
               StringUtil.containsString(new SlowStringReader(readerStr),
                                         stringToFind));
  }

  public void testNumberOfUnit() {
    assertEquals("0 boxes", StringUtil.numberOfUnits(0, "box", "boxes"));
    assertEquals("1 box", StringUtil.numberOfUnits(1, "box", "boxes"));
    assertEquals("2 boxes", StringUtil.numberOfUnits(2, "box", "boxes"));
    assertEquals("-3 boxes", StringUtil.numberOfUnits(-3, "box", "boxes"));
  }

  public void testEqualsIgnoreCase() {
    assertTrue(StringUtil.equalsIgnoreCase('a', 'a'));
    assertTrue(StringUtil.equalsIgnoreCase('a', 'A'));
    assertTrue(StringUtil.equalsIgnoreCase('A', 'a'));
    assertTrue(StringUtil.equalsIgnoreCase('A', 'A'));
    assertTrue(StringUtil.equalsIgnoreCase('!', '!'));
    assertFalse(StringUtil.equalsIgnoreCase('!', 'a'));
    assertFalse(StringUtil.equalsIgnoreCase('b', 'a'));
  }
  
  public void testToUnaccented() {
    assertEquals("abcde", StringUtil.toUnaccented("abcde"));
    // german date March 13, 2010 (a-umlaut)
    assertEquals("2010/Marz/13", StringUtil.toUnaccented("2010/M\u00e4rz/13"));
    // french: third quarter (latin small 'e' with grave)
    assertEquals("deuxieme trimestre", 
                 StringUtil.toUnaccented("deuxi\u00e8me trimestre"));
    // spanish: Good day (small letter 'i' with acute
    assertEquals("Buenos dias", StringUtil.toUnaccented("Buenos d\u00edas"));
    // norwegian: Now is the time (small letter 'a' with ring above
    assertEquals("Na er tiden", StringUtil.toUnaccented("N\u00e5 er tiden"));
    // swedish: Now is the time (small letter 'a' with diaeresis
    assertEquals("Nu ar det dags", StringUtil.toUnaccented("Nu \u00e4r det dags"));
    assertEquals("University of Lodz", StringUtil.toUnaccented("University of Łódź"));
    assertEquals("oe", StringUtil.toUnaccented("œ"));
    assertEquals("OE", StringUtil.toUnaccented("Œ"));
    assertEquals("ffl", StringUtil.toUnaccented("\uFB04"));
  }

  public void testHash64NullPayload() {
    try {
      StringUtil.hash64(null);
      fail("Calling hash64 with a null payload should throw");
    } catch (NullPointerException npe) {
    }
  }

  public void testHash64EmptyPayload() {
    assertEquals(-1621285313438006658L, StringUtil.hash64(""));
  }

  public void testHash64LOCKSSPayload() {
    assertEquals(-1302921253488513054L, StringUtil.hash64("LOCKSS"));
  }

  public void testHash64UnicodePayload() {
    assertEquals(-9103326884462245297L, StringUtil.hash64("Buenos d\u00edas"));
  }

//   public void testToUnaccentedFast() {
//     assertEquals("abcde", StringUtil.toUnaccentedFast("abcde"));
//     // german date March 13, 2010 (a-umlaut)
//     assertEquals("2010/Marz/13",
// 		 StringUtil.toUnaccentedFast("2010/M\u00e4rz/13"));
//     // french: third quarter (latin small 'e' with grave)
//     assertEquals("deuxieme trimestre", 
//                  StringUtil.toUnaccentedFast("deuxi\u00e8me trimestre"));
//     // spanish: Good day (small letter 'i' with acute
//     assertEquals("Buenos dias",
// 		 StringUtil.toUnaccentedFast("Buenos d\u00edas"));
//     // norwegian: Now is the time (small letter 'a' with ring above
//     assertEquals("Na er tiden",
// 		 StringUtil.toUnaccentedFast("N\u00e5 er tiden"));
//     // swedish: Now is the time (small letter 'a' with diaeresis
//     assertEquals("Nu ar det dags",
// 		 StringUtil.toUnaccentedFast("Nu \u00e4r det dags"));
//   }

  private static class SlowStringReader extends StringReader {
    public SlowStringReader(String str) {
      super(str);
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
      return super.read(cbuf, off, (len < 2 ? len : 2));
    }
  }

  public void testBlankOutNLsAndTabs() {
    assertEquals("", StringUtil.blankOutNlsAndTabs(null));
    assertEquals("", StringUtil.blankOutNlsAndTabs(""));
    assertEquals(" ", StringUtil.blankOutNlsAndTabs(" "));
    assertEquals("abc", StringUtil.blankOutNlsAndTabs("abc"));
    assertEquals(" abc ", StringUtil.blankOutNlsAndTabs(" abc "));
    assertEquals(" ", StringUtil.blankOutNlsAndTabs("\n"));
    assertEquals(" ", StringUtil.blankOutNlsAndTabs("\t"));
    assertEquals(" ", StringUtil.blankOutNlsAndTabs("\n\n"));
    assertEquals(" ", StringUtil.blankOutNlsAndTabs("\n\t"));
    assertEquals(" ", StringUtil.blankOutNlsAndTabs("\t\n"));
    assertEquals(" ", StringUtil.blankOutNlsAndTabs("\t\t"));
    assertEquals(" ", StringUtil.blankOutNlsAndTabs("\t\n\t\n\n\t\n\t"));
    assertEquals(" a", StringUtil.blankOutNlsAndTabs("\na"));
    assertEquals(" a", StringUtil.blankOutNlsAndTabs("\ta"));
    assertEquals(" a", StringUtil.blankOutNlsAndTabs("\n\na"));
    assertEquals(" a", StringUtil.blankOutNlsAndTabs("\n\ta"));
    assertEquals(" a", StringUtil.blankOutNlsAndTabs("\t\na"));
    assertEquals(" a", StringUtil.blankOutNlsAndTabs("\t\ta"));
    assertEquals(" a", StringUtil.blankOutNlsAndTabs("\t\n\t\n\n\t\n\ta"));
    assertEquals("a ", StringUtil.blankOutNlsAndTabs("a\n"));
    assertEquals("a ", StringUtil.blankOutNlsAndTabs("a\t"));
    assertEquals("a ", StringUtil.blankOutNlsAndTabs("a\n\n"));
    assertEquals("a ", StringUtil.blankOutNlsAndTabs("a\n\t"));
    assertEquals("a ", StringUtil.blankOutNlsAndTabs("a\t\n"));
    assertEquals("a ", StringUtil.blankOutNlsAndTabs("a\t\t"));
    assertEquals("a ", StringUtil.blankOutNlsAndTabs("a\t\n\t\n\n\t\n\t"));
    assertEquals(" a ", StringUtil.blankOutNlsAndTabs("\na\t"));
    assertEquals(" a ", StringUtil.blankOutNlsAndTabs("\ta\n"));
    assertEquals(" a ", StringUtil.blankOutNlsAndTabs("\n\na\t\t"));
    assertEquals(" a ", StringUtil.blankOutNlsAndTabs("\n\ta\t\n"));
    assertEquals(" a ", StringUtil.blankOutNlsAndTabs("\t\na\n\t"));
    assertEquals(" a ", StringUtil.blankOutNlsAndTabs("\t\ta\n\n"));
    assertEquals(" a ", StringUtil.blankOutNlsAndTabs("\t\n\t\na\n\t\n\t"));
    assertEquals("a a", StringUtil.blankOutNlsAndTabs("a\na"));
    assertEquals("a a", StringUtil.blankOutNlsAndTabs("a\ta"));
    assertEquals("a a", StringUtil.blankOutNlsAndTabs("a\n\na"));
    assertEquals("a a", StringUtil.blankOutNlsAndTabs("a\n\ta"));
    assertEquals("a a", StringUtil.blankOutNlsAndTabs("a\t\na"));
    assertEquals("a a", StringUtil.blankOutNlsAndTabs("a\t\ta"));
    assertEquals("a a", StringUtil.blankOutNlsAndTabs("a\t\n\t\n\n\t\n\ta"));
    assertEquals(" a bcd e", StringUtil.blankOutNlsAndTabs("\na\tbcd\t\ne"));
    assertEquals("ab c de ", StringUtil.blankOutNlsAndTabs("ab\t\tc\n\nde\n"));
    assertEquals("a b\r cd\r ",
	StringUtil.blankOutNlsAndTabs("a\n\tb\r\ncd\r\n"));
  }
  
  public void testNormalizeQuotationMarks() throws Exception {
    assertEquals("\"foo\"", StringUtil.normalizeQuotationMarks("\u201cfoo\u201d"));
    assertEquals("\"foo\"", StringUtil.normalizeQuotationMarks("\u201ffoo\u201d"));
  }
  
  public void testNormalizeApostrophes() throws Exception {
    assertEquals("'foo'", StringUtil.normalizeApostrophes("\u2018foo\u2019"));
    assertEquals("'foo'", StringUtil.normalizeApostrophes("\u201bfoo\u2019"));
  }
  
  public void testNormalizeDashes() throws Exception {
    assertEquals("a-z", StringUtil.normalizeDashes("a-z"));
    assertEquals("a-z", StringUtil.normalizeDashes("a\u2012z"));
    assertEquals("a-z", StringUtil.normalizeDashes("a\u2013z"));
    assertEquals("a-z", StringUtil.normalizeDashes("a\u2014z"));
    assertEquals("a-z", StringUtil.normalizeDashes("a\u2015z"));
  }
  
  public void testNonNull() throws Exception {
    assertSame("foo", StringUtil.nonNull("foo"));
    assertSame("", StringUtil.nonNull(""));
    assertEquals("", StringUtil.nonNull(null));
  }
  
}
