/*
 * $Id: TestStringFilter.java,v 1.6 2005-02-21 03:03:23 tlipkis Exp $
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

package org.lockss.filter;
import org.lockss.test.*;
import org.lockss.util.*;
import java.util.*;
import java.io.*;

public class TestStringFilter extends LockssTestCase {

  abstract class Fact {
    abstract StringFilter make(String input);
  }

  /** Check that the filtered string matches expected.  Test with varying
   * buffer lengths */
  private void assertFilterString(String expected, String input,
				  final String filter,
				  final boolean ignoreCase)
      throws IOException {
    assertFilterString(expected, input, new Fact() {
	StringFilter make(String input) {
	  StringFilter filt =
	    new StringFilter(new StringReader(input), filter);
	  filt.setIgnoreCase(ignoreCase);
	  return filt;
	}});
  }

  private void assertFilterString(String expected, String input,
				  final List filterList,
				  final boolean ignoreCase)
      throws IOException {
    assertFilterString(expected, input, new Fact() {
	StringFilter make(String input) {
	  StringFilter filt =
	    StringFilter.makeNestedFilter(new StringReader(input), filterList);
	  filt.setIgnoreCase(ignoreCase);
	  return filt;
	}});
  }

  private void assertReplaceString(String expected, String input,
				   final String[][] strArray,
				   final boolean ignoreCase)
      throws IOException {
    assertFilterString(expected, input, new Fact() {
	StringFilter make(String input) {
	  StringFilter filt =
	    StringFilter.makeNestedFilter(new StringReader(input), strArray,
					  ignoreCase);
	  return filt;
	}});
  }

  /** Check that the filtered string matches expected.  Test with varying
   * buffer lengths */
  private void assertFilterString(String expected, String input,
				  final String filter,
				  final int bufSize)
      throws IOException {
    assertFilterString(expected, input, new Fact() {
	StringFilter make(String input) {
	  StringFilter filt =
	    new StringFilter(new StringReader(input), bufSize, filter);
	  return filt;
	}});
  }

  private void assertReplaceString(String expected, String input,
				   final String search, final String replace,
				   final boolean ignoreCase)
      throws IOException {
    assertFilterString(expected, input, new Fact() {
	StringFilter make(String input) {
	  StringFilter filt =
	    new StringFilter(new StringReader(input), search, replace);
	  filt.setIgnoreCase(ignoreCase);
	  return filt;
	}});
  }

  private void assertReplaceString(String expected, String input,
				   final String search, final String replace,
				   final int bufSize)
      throws IOException {
    assertFilterString(expected, input, new Fact() {
	StringFilter make(String input) {
	  StringFilter filt =
	    new StringFilter(new StringReader(input), bufSize,
			     search, replace);
	  return filt;
	}});
  }

  /** Check that the filtered string matches expected.  Test with varying
   * buffer lengths */
  private void assertFilterString(String expected, String input,
				  Fact filterFact)
      throws IOException {
    for (int len = 1; len <= input.length() * 2; len++) {
      StringFilter reader = filterFact.make(input);
      assertReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
    for (int len = 1; len <= input.length() * 2; len++) {
      StringFilter reader = filterFact.make(input);
      assertOffsetReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
    StringFilter reader = filterFact.make(input);
    assertReaderMatchesStringSlow(expected, reader);
    assertEquals(-1, reader.read());
  }

  public void testConstThrowsOnNullReader() {
    try {
      StringFilter sf = new StringFilter(null, "Blah");
      fail("StringFilter's constructor should have failed on a null reader");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testConstThrowsOnNullString() {
    try {
      StringFilter sf = new StringFilter(new StringReader(""), (String)null);
      fail("StringFilter's constructor should have failed on a null string");
    } catch (IllegalArgumentException e) {
    }
  }

//   public void testConstThrowsOnNullList() {
//     try {
//       StringFilter sf = new StringFilter(new StringReader(""), (List)null);
//       fail("StringFilter's constructor should have failed on a null list");
//     } catch (IllegalArgumentException e) {
//     }
//   }

//   public void testConstThrowsOnEmptyList() {
//     try {
//       StringFilter sf =
// 	new StringFilter(new StringReader(""), ListUtil.list());
//       fail("StringFilter's constructor should have failed on a empty list");
//     } catch (IllegalArgumentException e) {
//     }
//   }

  public void testClose() throws IOException {
    String testStr = "Test string";
    InstrumentedStringReader reader = new InstrumentedStringReader(testStr);
    StringFilter filt = new StringFilter(reader, "FOO");
    assertEquals('T', filt.read());
    assertFalse(reader.isClosed());
    filt.close();
    assertTrue(reader.isClosed());
    try {
      int c = filt.read();
      fail("StringFilter shouldn't be readable after close()");
    } catch (IOException e) {
    }
  }

  public void testDoesntFilterIfNoMatchingString() throws IOException {
    String str = "This is a test string";
    assertFilterString(str, str, "REMOVE", false);
  }

  //Series of tests where the buffer size is larger than the filtered string
  public void testFiltersOneString() throws IOException {
    String str = "This is a REMOVEtest string";
    assertFilterString("This is a test string", str, "REMOVE", false);
  }

  public void testFiltersOneStringIgnoreCase() throws IOException {
    String str = "This is a ReMovetest string";
    assertFilterString("This is a test string", str, "REMOVE", true);
  }

  public void testFiltersOneStringDontIgnoreCase() throws IOException {
    String str = "This is a ReMovetest string";
    assertFilterString("This is a ReMovetest string", str, "REMOVE", false);
  }

  public void testFiltersOneStringBeginning() throws IOException {
    String str = "REMOVEThis is a test string";
    assertFilterString("This is a test string", str, "REMOVE", false);
  }

  public void testFiltersOneStringEnd() throws IOException {
    String str = "This is a test stringREMOVE";
    assertFilterString("This is a test string", str, "REMOVE", false);
  }

  public void testFiltersOneStringMulti() throws IOException {
    String str = "This is a REMOVEtest stringREMOVE";
    assertFilterString("This is a test string", str, "REMOVE", false);
  }

  //Series of tests where the buffer size is the same size as the
  //filtered string
  public void testFiltersOneStringSameSizeAsBuff() throws IOException {
    String str = "REMOVEThis is a test string";
    assertFilterString("This is a test string", str, "REMOVE", 6);
  }

  public void testFiltersOneStringOverflowBuffer() throws IOException {
    String str = "ThisREMOVE is a test string";
    assertFilterString("This is a test string", str, "REMOVE", 6);
  }

  public void testMakeNestedFiltersNullReader() {
    try {
      StringFilter.makeNestedFilter(null, ListUtil.list("test"));
      fail("Calling makeNestedFilter with a null reader should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testMakeNestedFiltersNullList() {
    try {
      StringFilter.makeNestedFilter(new StringReader("test"), (List)null);
      fail("Calling makeNestedFilter with a null list should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testMakeNestedFiltersNullArray() {
    try {
      StringFilter.makeNestedFilter(new StringReader("test"), (String[][])null,
                                    false);
      fail("Calling makeNestedFilter with a null list should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testMakeNestedFiltersEmptyList() {
    try {
      StringFilter.makeNestedFilter(new StringReader("test"), ListUtil.list());
      fail("Calling makeNestedFilter with an empty list should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  //Series of tests with multiple strings
  public void testFiltersMultipleString() throws IOException {
    String str = "ThisREMOVE is a ALSOtest string";
    assertFilterString("This is a test string", str,
		       ListUtil.list("REMOVE", "ALSO"),
		       false);
  }

  // same tests, testing string replacement
  // large buffer
  public void testDoesntReplaceIfNoMatchingString() throws IOException {
    String str = "This is a test string";
    assertReplaceString(str, str, "REMOVE", "REPLACE", false);
    assertReplaceString(str, str, "REMOVE", "REPLACE", 6);
  }

  public void testReplacesOneString() throws IOException {
    String str = "This is a REMOVEtest string";
    assertReplaceString("This is a REPLACEtest string", str,
			"REMOVE", "REPLACE", false);
    assertReplaceString("This is a REPLACEtest string", str,
			"REMOVE", "REPLACE", 6);
  }

  public void testReplacesOneStringIgnoreCase() throws IOException {
    String str = "This is a ReMovetest string";
    assertReplaceString("This is a REPLACEtest string", str,
			"REMOVE", "REPLACE", true);
  }

  public void testReplacesOneStringDontIgnoreCase() throws IOException {
    String str = "This is a ReMovetest string";
    assertReplaceString(str, str, "REMOVE", "REPLACE", false);
    assertReplaceString(str, str, "REMOVE", "REPLACE", 6);
  }

  public void testReplacesOneStringBeginning() throws IOException {
    String str = "REMOVEThis is a test string";
    assertReplaceString("REPLACEThis is a test string", str,
			"REMOVE", "REPLACE", false);
    assertReplaceString("REPLACEThis is a test string", str,
			"REMOVE", "REPLACE", 6);
  }

  public void testReplacesOneStringEnd() throws IOException {
    String str = "This is a test stringREMOVE";
    assertReplaceString("This is a test stringREPLACE", str,
			"REMOVE", "REPLACE", false);
    assertReplaceString("This is a test stringREPLACE", str,
			"REMOVE", "REPLACE", 6);
  }

  public void testReplacesOneStringMulti() throws IOException {
    String str = "This is a REMOVEtest stringREMOVE";
    assertReplaceString("This is a REPLACEtest stringREPLACE", str,
			"REMOVE", "REPLACE", false);
    assertReplaceString("This is a REPLACEtest stringREPLACE", str,
			"REMOVE", "REPLACE", 6);
  }

  public void testReplaceWithEmptyString() throws IOException {
    String str = "This is a REMOVEtest stringREMOVE";
    assertReplaceString("This is a test string", str,
			"REMOVE", "", false);
    assertReplaceString("This is a test string", str,
			"REMOVE", "", 6);
  }

  //small buffer
  public void testReplacesOneStringSameSizeAsBuff() throws IOException {
    String str = "REMOVEThis is a test string";
    assertReplaceString("REPLACEThis is a test string", str,
			"REMOVE", "REPLACE", 6);
  }

  public void testReplacesOneStringOverflowBuffer() throws IOException {
    String str = "ThisREMOVE is a test string";
    assertReplaceString("ThisREPLACE is a test string", str,
			"REMOVE", "REPLACE", 6);
  }

  public void testReplaceWithNull() throws IOException {
    String str = "This is a REMOVEtest string";
    assertReplaceString("This is a test string", str, "REMOVE", null, false);
    assertReplaceString("This is a test string", str, "REMOVE", null, 6);
  }

  // multi string
  public void testReplacesMultipleStringOneReplace() throws IOException {
    String str = "ThisREMOVE is a ALSOtest string";
    assertReplaceString("ThisREPLACE is a test string", str,
			new String[][] {
			  { "REMOVE", "REPLACE" },
			  { "ALSO", null }},
			false);
  }

  public void testReplacesMultipleStringTwoReplace() throws IOException {
    String str = "ThisREMOVE is a ALSOtest string";
    assertReplaceString("ThisREPLACE is a GONEtest string", str,
			new String[][] {
			  { "REMOVE", "REPLACE" },
			  { "ALSO", "GONE" }},
			false);
  }

  public void testReplacesMultipleStringTwoReplaceMixedCase()
      throws IOException {
    String str = "ThisREMOVE is a Alsotest string";
    assertReplaceString("ThisREPLACE is a Alsotest string", str,
			new String[][] {
			  { "REMOVE", "REPLACE" },
			  { "ALSO", "GONE" }},
			false);
  }

  public void testReplacesMultipleStringTwoReplaceMixedCaseIgnore()
      throws IOException {
    String str = "ThisRemove is a aLSotest string";
    assertReplaceString("ThisREPLACE is a GONEtest string", str,
			new String[][] {
			  { "REMOVE", "REPLACE" },
			  { "ALSO", "GONE" }},
			true);
  }


  public static void main(String[] argv) {
    String[] testCaseList = { TestStringFilter.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}
