/*
 * $Id: TestHtmlTagFilter.java,v 1.6 2006-11-07 20:44:47 troberts Exp $
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

import java.io.*;
import java.util.*;

import org.lockss.config.Configuration;
import org.lockss.filter.HtmlTagFilter.MissingEndTagException;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestHtmlTagFilter extends LockssTestCase {
  private static final String startTag1 = "<start>";
  private static final String endTag1 = "<end>";

  private static final String startTag2 = "<script>";
  private static final String endTag2 = "</script>";

  private static final HtmlTagFilter.TagPair tagPair1 =
    new HtmlTagFilter.TagPair(startTag1, endTag1);

  private static final HtmlTagFilter.TagPair tagPair2 =
    new HtmlTagFilter.TagPair(startTag2, endTag2);

  private static final String PARAM_THROW_IF_NO_END_TAG =
    Configuration.PREFIX + "HtmlTagFilter.throwIfNoEndTag";



  /** Check that the filtered string matches expected.  Test with varying
   * buffer lengths and offsets */
  private void assertFilterString(String expected, String input,
				  HtmlTagFilter.TagPair tagPair)
      throws IOException {
    for (int len = 1; len <= input.length() * 2; len++) {
      Reader reader = new HtmlTagFilter(new StringReader(input), tagPair);
      assertReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
    for (int len = 1; len <= input.length() * 2; len++) {
      Reader reader = new HtmlTagFilter(new StringReader(input), tagPair);
      assertOffsetReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
    Reader reader = new HtmlTagFilter(new StringReader(input), tagPair);
    assertReaderMatchesStringSlow(expected, reader);
    assertEquals(-1, reader.read());
  }

  /** Check that the filtered string matches expected.  Test with varying
   * buffer lengths and offsets */
  private void assertFilterString(String expected, String input,
				  List tagPairs)
      throws IOException {
    for (int len = 1; len <= input.length() * 2; len++) {
      Reader reader = HtmlTagFilter.makeNestedFilter(new StringReader(input),
						     tagPairs);
      assertReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
    for (int len = 1; len <= input.length() * 2; len++) {
      Reader reader = HtmlTagFilter.makeNestedFilter(new StringReader(input),
						     tagPairs);
      assertOffsetReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
    Reader reader = HtmlTagFilter.makeNestedFilter(new StringReader(input),
						   tagPairs);
    assertReaderMatchesStringSlow(expected, reader);
    assertEquals(-1, reader.read());
  }


  public void testCanNotCreateWithNullReader() {
    try {
      HtmlTagFilter filter =
	new HtmlTagFilter(null, new HtmlTagFilter.TagPair("blah", "blah"));
      fail("Trying to create a HtmlTagFilter with a null Reader should throw "+
	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testCanNotCreateWithNullTagPair() {
    try {
      HtmlTagFilter filter =
	new HtmlTagFilter(new StringReader("blah"),
			  (HtmlTagFilter.TagPair)null);
      fail("Trying to create a HtmlTagFilter with a null TagPair should "+
	   "throw an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testCanNotCreateWithNullTagPairList() {
    try {
      HtmlTagFilter filter =
	HtmlTagFilter.makeNestedFilter(new StringReader("blah"),
				       (List)null);
      fail("Trying to create a HtmlTagFilter with a null TagPair list should "+
	   "throw an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testCanNotCreateWithEmptyTagPairList() {
    try {
      HtmlTagFilter filter =
	HtmlTagFilter.makeNestedFilter(new StringReader("blah"),
				       new LinkedList());
      fail("Trying to create a HtmlTagFilter with an empty TagPair should "+
	   "throw an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testCanNotCreateWithEmptyStringTags() {
    try {
      HtmlTagFilter filter =
	new HtmlTagFilter(new StringReader("blah"),
			  new HtmlTagFilter.TagPair(new String(), "blah"));
      fail("Trying to create a HtmlTagFilter with a TagPair with "+
	   "an empty string should throw an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }

    try {
      HtmlTagFilter filter =
	new HtmlTagFilter(new StringReader("blah"),
			  new HtmlTagFilter.TagPair("blah", new String()));
      fail("Trying to create a HtmlTagFilter with a TagPair with "+
	   "an empty string should throw an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testDoesNotModifyTagList() {
    List list = new LinkedList();
    list.add(tagPair1);
    list.add(tagPair2);
    HtmlTagFilter filter =
      HtmlTagFilter.makeNestedFilter(new StringReader("blah"), list);
    assertEquals(2, list.size());
    assertEquals(tagPair1, (HtmlTagFilter.TagPair)list.get(0));
    assertEquals(tagPair2, (HtmlTagFilter.TagPair)list.get(1));
  }

  public void testReadReturnsNegOneWhenEmpty() throws IOException {
    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(""),
			new HtmlTagFilter.TagPair("blah", "blah2"));
    char[] buf = new char[10];
    assertEquals(-1, reader.read());
    assertEquals(-1, reader.read(buf));
    reader = new HtmlTagFilter(new StringReader("foobar"),
			       new HtmlTagFilter.TagPair("blah", "blah2"));
    assertEquals("foobar", StringUtil.fromReader(reader));
    assertEquals(-1, reader.read());
    assertEquals(-1, reader.read(buf));

    reader = new HtmlTagFilter(new StringReader("<foobar>"),
			       new HtmlTagFilter.TagPair("<", ">"));
    assertEquals(-1, reader.read());
    assertEquals(-1, reader.read(buf));
  }


  public void testDoesNotFilterContentWithoutTags() throws IOException {
    String content = "This is test content";
    assertFilterString(content, content,
		       new HtmlTagFilter.TagPair("blah", "blah2"));
  }

  public void testFiltersSingleCharTags() throws IOException {
    String content = "This <is test >content";
    String expectedContent = "This content";
    assertFilterString(expectedContent, content,
		       new HtmlTagFilter.TagPair("<", ">"));
  }

  public void testFiltersSingleTagsNoNesting() throws IOException {
    String content = "This "+startTag1+"is test "+endTag1+"content";
    String expectedContent = "This content";
    assertFilterString(expectedContent, content, tagPair1);
  }

  public void testCaseInsensitive() throws IOException {
    String content = "This <Start> is test <END>content";
    String expectedContent = "This content";
    assertFilterString(expectedContent, content,
		       new HtmlTagFilter.TagPair(startTag1, endTag1, true));
  }

  public void testIgnoreEndTagWithNoStartTag() throws IOException {
    String content = "This is test "+endTag1+"content";
    String expectedContent = "This is test "+endTag1+"content";
    assertFilterString(expectedContent, content, tagPair1);
  }

  public void testNoEndTagDefault() throws IOException {
    String content = "This "+startTag1+"is test content";
    String expectedContent = "This ";
    try {
      assertFilterString(expectedContent, content, tagPair1);
      fail("Trying to filter content with missing end tag should throw");
    } catch (HtmlTagFilter.MissingEndTagException ex) {
      //expected
    }
  }

  public void testNoEndTagParamFalse() throws IOException {
    Properties p = new Properties();
    p.setProperty(PARAM_THROW_IF_NO_END_TAG, "false");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    String content = "This "+startTag1+"is test content";
    String expectedContent = "This ";
    assertFilterString(expectedContent, content, tagPair1);
  }

  public void testNoEndTagParamTrue() throws IOException {
    Properties p = new Properties();
    p.setProperty(PARAM_THROW_IF_NO_END_TAG, "true");
    ConfigurationUtil.setCurrentConfigFromProps(p);

    String content = "This "+startTag1+"is test content";
    String expectedContent = "This ";
    try {
      assertFilterString(expectedContent, content, tagPair1);
      fail("Trying to filter content with missing end tag should throw");
    } catch (HtmlTagFilter.MissingEndTagException ex) {
      //expected
    }
  }

  public void testFiltersSingleTagsNestingVariant1() throws IOException {
    String content =
      "This "+startTag1+startTag1
      +"is test "+endTag1+endTag1+"content";
    String expectedContent = "This content";
    assertFilterString(expectedContent, content, tagPair1);
  }

  public void testFiltersSingleTagsNestingVariant2() throws IOException {
    String content =
      "This "+startTag1+"is "+startTag1
      +"test "+endTag1+endTag1+"content";
    String expectedContent = "This content";
    assertFilterString(expectedContent, content, tagPair1);
  }

  public void testFiltersSingleTagsNestingVariant3() throws IOException {
    String content =
      "This "+startTag1+"is "+startTag1
      +"test "+endTag1+"error "+endTag1+"content";
    String expectedContent = "This content";
    assertFilterString(expectedContent, content, tagPair1);
  }

  public void testFiltersSingleTagsNestingVariant4() throws IOException {
    String content =
      "This "+startTag1+startTag1
      +"test "+endTag1+"is "+endTag1+"content";
    String expectedContent = "This content";
    assertFilterString(expectedContent, content, tagPair1);
  }

  public void testFiltersSingleTagsIgnoreNesting() throws IOException {
    HtmlTagFilter.TagPair tagPair =
      new HtmlTagFilter.TagPair(startTag1, endTag1, true, false);

    String content =
      "This "+startTag1+startTag1
      +"test "+endTag1+"is content";
    String expectedContent = "This is content";
    assertFilterString(expectedContent, content, tagPair);
  }

  public void testFiltersMultipleTagsNoNesting() throws IOException {
    String content =
      "This "+startTag1+"is "+endTag1
      +"test "+startTag2+"content"+endTag2+"here";
    String expectedContent = "This test here";
    assertFilterString(expectedContent, content,
		       ListUtil.list(tagPair1, tagPair2));
  }

  public void testFiltersMultipleTagsComplexNesting() throws IOException {
    String content =
      startTag2+startTag1+endTag2+"blah1"+endTag1+endTag2+"blah2";

    String expectedContent = "blah2";
    assertFilterString(expectedContent, content,
		       ListUtil.list(tagPair1, tagPair2));

    expectedContent = "blah1"+endTag1+endTag2+"blah2";
    assertFilterString(expectedContent, content,
		       ListUtil.list(tagPair2, tagPair1));
  }

  public void testFiltersMultipleTagsSimpleNesting() throws IOException {
    String content =
      "This "+startTag1+"is "+startTag2
      +"test "+endTag2+"content"+endTag1+"here";
    String expectedContent = "This here";
    assertFilterString(expectedContent, content,
		       ListUtil.list(tagPair2, tagPair1));
  }

  public void testFiltersSingleTagCloseNextToOpen() throws IOException {
    String content =
      "This "+startTag1+"is "+endTag1+startTag1
      +"test content"+endTag1+"here";
    String expectedContent = "This here";
    assertFilterString(expectedContent, content,
		       ListUtil.list(tagPair2, tagPair1));
  }

  /**
   * To catch an old error case involving single char reads when the string
   * be filtered matched the size of the buffer
   * @throws IOException
   */
  public void testSingleCharReadOverBuffer() throws IOException {
    String config = "org.lockss.filter.buffer_capacity=5";
    ConfigurationUtil.setCurrentConfigFromString(config);

    //5 chars between the start and end tags
    String content = "This <is test >content";
    String expectedContent = "This content";
    assertFilterString(expectedContent, content,
		       new HtmlTagFilter.TagPair("<", ">"));
  }

  public void testTagBridgesBuffer() throws IOException {
    String config = "org.lockss.filter.buffer_capacity=8";
    ConfigurationUtil.setCurrentConfigFromString(config);

    //<table> will get split into two different buffers
    String content = "This <table>is test </table>content";
    String expectedContent = "This content";

    List list =
      ListUtil.list(new HtmlTagFilter.TagPair("<table", "</table>", true));

    assertFilterString(expectedContent, content, list);
  }

  public void testArrayReadThrowsOnNullArray() throws IOException {
    String content = "This is test content";
    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    try {
      reader.read(null, 0, 5);
      fail("Calling read with an null array should have thrown");
    } catch (RuntimeException e) {
    }
  }

  public void testArrayReadThrowsOnBadOffset() throws IOException {
    String content = "This is test content";
    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    try {
      reader.read(new char[10], -1, 5);
      fail("Calling read with a negative offset should have thrown");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      reader.read(new char[10], 10, 5);
      fail("Calling read with an offset bigger than array length "
	   +"should have thrown");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testArrayReadThrowsOnBadOffsetLengthCombo() throws IOException {
    String content = "This is test content";
    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    try {
      reader.read(new char[10], 0, 12);
      fail("Calling read with a length bigger than array length should throw");
    } catch (IndexOutOfBoundsException e) {
    }

    try {
      reader.read(new char[10], 5, 6);
      fail("Calling read with (offset+length) bigger than "
	   +"array length should thrown");
    } catch (IndexOutOfBoundsException e) {
    }
  }

  public void testClose() throws IOException {
    String testStr = "Test string";
    InstrumentedStringReader reader = new InstrumentedStringReader(testStr);
    HtmlTagFilter filt = new HtmlTagFilter(reader, tagPair1);
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

  public void testMarkSupportedReturnsFalse() {
    HtmlTagFilter reader = new HtmlTagFilter(new StringReader(""),
					     tagPair1);
    assertFalse(reader.markSupported());
  }


  //Tests for TagPair
  public void testCanNotCreateTagPairWithNullStrings() {
    try {
      HtmlTagFilter.TagPair pair = new HtmlTagFilter.TagPair(null, "blah");
      fail("Trying to create a tag pair with a null string should throw "+
	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
    try {
      HtmlTagFilter.TagPair pair = new HtmlTagFilter.TagPair("blah", null);
      fail("Trying to create a tag pair with a null string should throw "+
 	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testTagPairNotEqual() {
    HtmlTagFilter.TagPair pair1 = new HtmlTagFilter.TagPair("blah1", "blah2");
    HtmlTagFilter.TagPair pair2 = new HtmlTagFilter.TagPair("bleh3", "bleh4");
    assertNotEquals(pair1, pair2);
  }

  public void testTagPairIsEqual() {
    HtmlTagFilter.TagPair pair1 = new HtmlTagFilter.TagPair("blah1", "blah2");
    HtmlTagFilter.TagPair pair2 = new HtmlTagFilter.TagPair("blah1", "blah2");
    assertEquals(pair1, pair2);
  }

  public void testEqualsWithDiffObject() {
    HtmlTagFilter.TagPair pair1 = new HtmlTagFilter.TagPair("blah1", "blah2");
    HtmlTagFilter.TagPair pair2 = new HtmlTagFilter.TagPair("blah1", "blah2");
    assertNotEquals(pair1, "Test string");
  }

  public void testTagPairNotEqualHash() {
    //To be fair, this is not guaranteed to succeed, since two objects
    //may hash to each other.  However, if all 7 of these have the same hash
    //value, somethign is probably wrong.
    HtmlTagFilter.TagPair pair1 = new HtmlTagFilter.TagPair("blah1", "bleh1");
    HtmlTagFilter.TagPair pair2 = new HtmlTagFilter.TagPair("blah2", "bleh2");
    HtmlTagFilter.TagPair pair3 = new HtmlTagFilter.TagPair("blah3", "bleh3");
    HtmlTagFilter.TagPair pair4 = new HtmlTagFilter.TagPair("blah4", "bleh4");
    HtmlTagFilter.TagPair pair5 = new HtmlTagFilter.TagPair("blah5", "bleh5");
    HtmlTagFilter.TagPair pair6 = new HtmlTagFilter.TagPair("blah6", "bleh6");
    HtmlTagFilter.TagPair pair7 = new HtmlTagFilter.TagPair("blah7", "bleh7");
    assertFalse((pair1.hashCode() == pair2.hashCode() &&
		 pair2.hashCode() == pair3.hashCode() &&
		 pair3.hashCode() == pair4.hashCode() &&
		 pair4.hashCode() == pair5.hashCode() &&
		 pair5.hashCode() == pair6.hashCode() &&
		 pair6.hashCode() == pair7.hashCode()));
  }

  public void testTagPairHasEqualHash() {
    HtmlTagFilter.TagPair pair1 = new HtmlTagFilter.TagPair("blah1", "blah2");
    HtmlTagFilter.TagPair pair2 = new HtmlTagFilter.TagPair("blah1", "blah2");
    assertEquals(pair1.hashCode(), pair2.hashCode());
  }
}
