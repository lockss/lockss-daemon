/*
 * $Id: TestHtmlTagFilter.java,v 1.1 2002-12-10 02:20:35 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
	new HtmlTagFilter(new StringReader("blah"), 
			  (List)null);
      fail("Trying to create a HtmlTagFilter with a null TagPair list should "+
	   "throw an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testCanNotCreateWithEmptyTagPairList() {
    try {
      HtmlTagFilter filter = 
	new HtmlTagFilter(new StringReader("blah"), new LinkedList());
      fail("Trying to create a HtmlTagFilter with an empty TagPair should "+
	   "throw an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testDoesNotFilterContentWithOutTags() throws IOException {
    String content = "This is test content";
    
    HtmlTagFilter reader = 
      new HtmlTagFilter(new StringReader(content.toString()),
			new HtmlTagFilter.TagPair("blah", "blah2"));

    assertReaderMatchesString(content, reader);
  }

  public void testFiltersSingleTagsNoNesting() throws IOException {
    String startTag = "<start>";
    String endTag = "<end>";
    String content = "This "+startTag+"is test "+endTag+"content";
    String expectedContent = "This content";

    HtmlTagFilter reader = 
      new HtmlTagFilter(new StringReader(content),
			new HtmlTagFilter.TagPair(startTag, endTag));
    
    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersTrailingTag() throws IOException {
    String startTag = "<start>";
    String endTag = "<end>";
    String content = "This "+startTag+"is test content";
    String expectedContent = "This ";

    HtmlTagFilter reader = 
      new HtmlTagFilter(new StringReader(content),
			new HtmlTagFilter.TagPair(startTag, endTag));
    
    assertReaderMatchesString(expectedContent, reader);
//     for (int ix=0; ix<expectedContent.length(); ix++) {
//       assertEquals(expectedContent.charAt(ix), reader.read());
//     }
//     assertEquals(-1, reader.read());
  }

  public void testFiltersSingleTagsNesting() throws IOException {
    String startTag = "<start>";
    String endTag = "<end>";
    String content = 
      "This "+startTag+"is "+startTag
      +"test "+endTag+endTag+"content";
    String expectedContent = "This content";

    HtmlTagFilter reader = 
      new HtmlTagFilter(new StringReader(content),
			new HtmlTagFilter.TagPair(startTag, endTag));
    
    assertReaderMatchesString(expectedContent, reader);
//     for (int ix=0; ix<expectedContent.length(); ix++) {
//       assertEquals(expectedContent.charAt(ix), reader.read());
//     }
//     assertEquals(-1, reader.read());
  }


   public void testFiltersMultipleTagsNoNesting() throws IOException {
     String startTag1 = "<start>";
     String endTag1 = "<end>";
     String startTag2 = "<script>";
     String endTag2 = "</script>";
     HtmlTagFilter.TagPair pair1 = 
       new HtmlTagFilter.TagPair(startTag1, endTag1);
     HtmlTagFilter.TagPair pair2 = 
       new HtmlTagFilter.TagPair(startTag2, endTag2);

    String content = 
      "This "+startTag1+"is "+endTag1
      +"test "+startTag2+"content"+endTag2+"here";
    String expectedContent = "This test here";

    HtmlTagFilter reader = 
      new HtmlTagFilter(new StringReader(content),
			ListUtil.list(pair1, pair2));
    
    assertReaderMatchesString(expectedContent, reader);
//     for (int ix=0; ix<expectedContent.length(); ix++) {
//       assertEquals(expectedContent.charAt(ix), reader.read());
//     }
//     assertEquals(-1, reader.read());
  }

   public void testFiltersMultipleTagsComplexNesting() throws IOException {
     String startTag1 = "<start>";
     String endTag1 = "<end>";
     String startTag2 = "<script>";
     String endTag2 = "</script>";
     HtmlTagFilter.TagPair pair1 = 
       new HtmlTagFilter.TagPair(startTag1, endTag1);
     HtmlTagFilter.TagPair pair2 = 
       new HtmlTagFilter.TagPair(startTag2, endTag2);

    String content = 
      startTag2+startTag1+endTag2+"blah"+endTag1+endTag2+"blah";

    String expectedContent = "blah";

    HtmlTagFilter reader = 
      new HtmlTagFilter(new StringReader(content),
			ListUtil.list(pair1, pair2));
    
    assertReaderMatchesString(expectedContent, reader);

    expectedContent = "blah"+endTag1+endTag2+"blah";
    reader = 
      new HtmlTagFilter(new StringReader(content),
			ListUtil.list(pair2, pair1));
    assertReaderMatchesString(expectedContent, reader);
  }

  public void testFiltersMultipleTagsSimpleNesting() throws IOException {
     String startTag1 = "<start>";
     String endTag1 = "<end>";
     String startTag2 = "<script>";
     String endTag2 = "</script>";
     HtmlTagFilter.TagPair pair1 = 
       new HtmlTagFilter.TagPair(startTag1, endTag1);
     HtmlTagFilter.TagPair pair2 = 
       new HtmlTagFilter.TagPair(startTag2, endTag2);

    String content = 
      "This "+startTag1+"is "+startTag2
      +"test "+endTag2+"content"+endTag1+"here";
    String expectedContent = "This here";

    HtmlTagFilter reader = 
      new HtmlTagFilter(new StringReader(content),
			ListUtil.list(pair2, pair1));
    
    assertReaderMatchesString(expectedContent, reader);
  }

  public void testReadWithBuffer() throws IOException {
    String startTag = "<start>";
    String endTag = "<end>";
    String content = "This "+startTag+"is test "+endTag+"content";
    String expectedContent = "This content";

    HtmlTagFilter reader = 
      new HtmlTagFilter(new StringReader(content),
			new HtmlTagFilter.TagPair(startTag, endTag));
    
    char actual[] = new char[expectedContent.length()];
    reader.read(actual);
    assertEquals(expectedContent, new String(actual));
    assertEquals(0, reader.read(actual));
  }

  public void testReadWithOffsetAndLength() throws IOException {
    String startTag = "<start>";
    String endTag = "<end>";
    String content = "This "+startTag+"is test "+endTag+"content";
    String expectedContent = "is cont";

    HtmlTagFilter reader = 
      new HtmlTagFilter(new StringReader(content),
			new HtmlTagFilter.TagPair(startTag, endTag));
    
    char actual[] = new char[expectedContent.length()];
    assertEquals(7, reader.read(actual, 2, 7));
    assertEquals(expectedContent, new String(actual));

    actual = new char[3];
    reader.read(actual);
    assertEquals("ent", new String(actual));
  }

  

  private void assertReaderMatchesString(String expected, 
					 Reader reader) //XXX should be reader
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
    assertTrue(!pair1.equals(pair2));
  }

  public void testTagPairIsEqual() {
    HtmlTagFilter.TagPair pair1 = new HtmlTagFilter.TagPair("blah1", "blah2");
    HtmlTagFilter.TagPair pair2 = new HtmlTagFilter.TagPair("blah1", "blah2");
    assertEquals(pair1, pair2);
  }

  public void testTagPairNotEqualHash() {
    HtmlTagFilter.TagPair pair1 = new HtmlTagFilter.TagPair("blah1", "blah2");
    HtmlTagFilter.TagPair pair2 = new HtmlTagFilter.TagPair("bleh3", "bleh4");
    assertTrue(pair1.hashCode() != pair2.hashCode());
  }
  public void testTagPairHasEqualHash() {
    HtmlTagFilter.TagPair pair1 = new HtmlTagFilter.TagPair("blah1", "blah2");
    HtmlTagFilter.TagPair pair2 = new HtmlTagFilter.TagPair("blah1", "blah2");
    assertEquals(pair1.hashCode(), pair2.hashCode());
  }


}

