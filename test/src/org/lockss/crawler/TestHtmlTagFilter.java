/*
 * $Id: TestHtmlTagFilter.java,v 1.8 2003-06-20 22:34:53 claire Exp $
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

package org.lockss.crawler;
import junit.framework.TestCase;
import java.io.*;
import java.util.*;
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


  public TestHtmlTagFilter(String msg) {
    super(msg);
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
			  new HtmlTagFilter.TagPair("", "blah"));
      fail("Trying to create a HtmlTagFilter with a TagPair with "+
	   "an empty string should throw an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }

    try {
      HtmlTagFilter filter =
	new HtmlTagFilter(new StringReader("blah"),
			  new HtmlTagFilter.TagPair("blah", ""));
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

    assertEquals(-1, reader.read());
  }


  public void testDoesNotFilterContentWithoutTags() throws IOException {
    String content = "This is test content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content.toString()),
			new HtmlTagFilter.TagPair("blah", "blah2"));

    assertReaderMatchesString(content, reader);
  }

  public void testFiltersSingleCharTags() throws IOException {
    String content = "This <is test >content";
    String expectedContent = "This content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content),
			new HtmlTagFilter.TagPair("<", ">"));

    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersSingleTagsNoNesting() throws IOException {
    String content = "This "+startTag1+"is test "+endTag1+"content";
    String expectedContent = "This content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    assertReaderMatchesString(expectedContent, reader);
  }

  public void testCaseInsensitive() throws IOException {
    HtmlTagFilter.TagPair tagPair =
      new HtmlTagFilter.TagPair(startTag1, endTag1, true);

    String content = "This <Start> is test <END>content";
    String expectedContent = "This content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair);

    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersIgnoreEndTagWithNoStartTag() throws IOException {
    String content = "This is test "+endTag1+"content";
    String expectedContent = "This is test "+endTag1+"content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersTrailingTag() throws IOException {
    String content = "This "+startTag1+"is test content";
    String expectedContent = "This ";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersSingleTagsNestingVariant1() throws IOException {
    String content =
      "This "+startTag1+startTag1
      +"is test "+endTag1+endTag1+"content";
    String expectedContent = "This content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    assertReaderMatchesString(expectedContent, reader);
  }


  public void testFiltersSingleTagsNestingVariant2() throws IOException {
    String content =
      "This "+startTag1+"is "+startTag1
      +"test "+endTag1+endTag1+"content";
    String expectedContent = "This content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersSingleTagsNestingVariant3() throws IOException {
    String content =
      "This "+startTag1+"is "+startTag1
      +"test "+endTag1+"error "+endTag1+"content";
    String expectedContent = "This content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersSingleTagsNestingVariant4() throws IOException {
    String content =
      "This "+startTag1+startTag1
      +"test "+endTag1+"is "+endTag1+"content";
    String expectedContent = "This content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);

    assertReaderMatchesString(expectedContent, reader);
  }


  public void testFiltersMultipleTagsNoNesting() throws IOException {
    String content =
      "This "+startTag1+"is "+endTag1
      +"test "+startTag2+"content"+endTag2+"here";
    String expectedContent = "This test here";
    
    HtmlTagFilter reader =
      HtmlTagFilter.makeNestedFilter(new StringReader(content),
				     ListUtil.list(tagPair1, tagPair2));
    
    assertReaderMatchesString(expectedContent, reader);
  }
  
  public void testFiltersMultipleTagsComplexNesting() throws IOException {
    String content =
      startTag2+startTag1+endTag2+"blah1"+endTag1+endTag2+"blah2";
    
    String expectedContent = "blah2";

    HtmlTagFilter reader =
      HtmlTagFilter.makeNestedFilter(new StringReader(content),
				     ListUtil.list(tagPair1, tagPair2));

    assertReaderMatchesString(expectedContent, reader);

    expectedContent = "blah1"+endTag1+endTag2+"blah2";
    reader =
      HtmlTagFilter.makeNestedFilter(new StringReader(content),
				     ListUtil.list(tagPair2, tagPair1));
    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersMultipleTagsSimpleNesting() throws IOException {
    String content =
      "This "+startTag1+"is "+startTag2
      +"test "+endTag2+"content"+endTag1+"here";
    String expectedContent = "This here";

    HtmlTagFilter reader =
      HtmlTagFilter.makeNestedFilter(new StringReader(content),
				     ListUtil.list(tagPair2, tagPair1));

    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersSingleTagCloseNextToOpen() throws IOException {
    String content =
      "This "+startTag1+"is "+endTag1+startTag1
      +"test content"+endTag1+"here";
    String expectedContent = "This here";

    HtmlTagFilter reader =
      HtmlTagFilter.makeNestedFilter(new StringReader(content),
				     ListUtil.list(tagPair2, tagPair1));

    assertReaderMatchesString(expectedContent, reader);
  }

  public void testReadWithBuffer() throws IOException {
    String content = "This "+startTag1+"is test "+endTag1+"content";
    String expectedContent = "This content";

    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);


    char actual[] = new char[expectedContent.length()];
    reader.read(actual);
    assertEquals(expectedContent, new String(actual));
    assertEquals(-1, reader.read(actual));
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

  public void testArrayReadUnfilteredString() throws IOException {
    String content = "This is test content";
    char chars[] = new char[256];
    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);
    assertEquals(20, reader.read(chars));
    assertEquals(content, new String(chars, 0, 20));
  }
    
  public void testArrayReadUnfilteredStringWithOffset() throws IOException {
    String content = "This is test content";
    char chars[] = {'a', 'b', 'c', 'd', 'e', 'f', 'g',
		    'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o' };
    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);
    assertEquals(10, reader.read(chars, 5, 10));
    assertEquals("abcdeThis is te", new String(chars, 0, 15));
  }
    
  public void testArrayReadFilteredString() throws IOException {
    String content = "This "+startTag1+"is test "+endTag1+"content";
    String expectedContent = "This content";

    char chars[] = new char[256];
    HtmlTagFilter reader =
      new HtmlTagFilter(new StringReader(content), tagPair1);
    assertEquals(expectedContent.length(), reader.read(chars));
    assertEquals(expectedContent,
		 new String(chars, 0, expectedContent.length()));
  }
    

  public void testMarkSupportedReturnsFalse() {
    HtmlTagFilter reader = new HtmlTagFilter(new StringReader(""),
					     tagPair1);
    assertFalse(reader.markSupported());
  }


  private void assertReaderMatchesString(String expected, Reader reader)
      throws IOException{
    StringBuffer actual = new StringBuffer(expected.length());
    int kar;
    while ((kar = reader.read()) != -1) {
      actual.append((char)kar);
    }
    assertEquals(expected, actual.toString());
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

