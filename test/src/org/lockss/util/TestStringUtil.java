/*
 * $Id: TestStringUtil.java,v 1.6 2002-10-31 01:58:54 aalto Exp $
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

/**
 * This is the test class for org.lockss.util.StringUtil
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public class TestStringUtil extends TestCase{
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
  }

  public void testReplaceStringSingleExistingSubstring(){
    String testStr = "blahTestblah";
    assertEquals("blahtestblah", StringUtil.replaceString(testStr, "Test", "test"));
  }

  public void testReplaceStringMultiExistingSubstring(){
    String testStr = "blahTestblah";
    assertEquals("BlahTestBlah", StringUtil.replaceString(testStr, "blah", "Blah"));
  }

  public void testReplacementStringContainsReplacedString(){
    assertEquals("1234456",
  		 StringUtil.replaceString("123456", "4", "44"));
  }

  public void testReplacementStringDiffLength(){
    assertEquals("12347856",
  		 StringUtil.replaceString("123456", "4", "478"));
  }

  public void testReplaceEqualStrings(){
    assertEquals("TestBlahTest",
		 StringUtil.replaceString("TestBlahTest", "Blah", "Blah"));
  }


//    public void testReplaceStringNullFirst(){
//    }

}

