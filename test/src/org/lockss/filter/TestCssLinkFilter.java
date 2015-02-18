/*
 * $Id$
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

import org.lockss.test.*;
import org.lockss.util.*;

public class TestCssLinkFilter extends LockssTestCase {

  private static Logger logger = Logger.getLogger("TestCssLinkFilter");
  
  private static final String[][] array0 = {
  };
      
  private static final String[][] array1 = {
      {"tag here", "replace me", "replaced him"},
  };
      
  private static final String[][] array2 = {
      {"tag here", "replace me", "replaced him"},
      {"tag there", "replace she", "replaced her"},
  };
      
  private static final String[][] arrayShort = {
      {"tag here", "replace me"},
      {"tag there", "replace she", "replaced her"},
  };
      

  public void testCanNotCreateWithNullReader() {
    try {
      CssLinkFilter filter =
	new CssLinkFilter(null, 9, "foo", "bar", "bletch");
      fail("Trying to create a CssLinkFilter with a null Reader should throw "+
	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  // Annoying null pointer
  public void dontTestCanNotCreateWithNullTag() {
    try {
      CssLinkFilter filter =
	new CssLinkFilter(new StringReader("foo"), 9, null, "bar", "bletch");
      fail("Trying to create a CssLinkFilter with a null tag should throw "+
	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  // Annoying null pointer
  public void dontTestCanNotCreateWithNullRegex() {
    try {
      CssLinkFilter filter =
	new CssLinkFilter(new StringReader("foo"), 9, "foo", null, "bletch");
      fail("Trying to create a CssLinkFilter with a null regex should throw "+
	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testMakeNestedFailWithNullReader() {
    try {
      CssLinkFilter filter =
	CssLinkFilter.makeNestedFilter(null, array1, true);
      fail("Trying to create a CssLinkFilter with a null Reader should throw "+
	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testMakeNestedFailWithNullArray() {
    try {
      CssLinkFilter filter =
	CssLinkFilter.makeNestedFilter(new StringReader("foo"), null, true);
      fail("Trying to create a CssLinkFilter with a null array should throw "+
	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testMakeNestedFailWithEmptyArray() {
    try {
      CssLinkFilter filter =
	CssLinkFilter.makeNestedFilter(new StringReader("foo"), array0, true);
      fail("Trying to create a CssLinkFilter with an empty array should throw "+
	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testMakeNestedFailWithShortArray() {
    try {
      CssLinkFilter filter =
	CssLinkFilter.makeNestedFilter(new StringReader("foo"), arrayShort, true);
      fail("Trying to create a CssLinkFilter with an empty array should throw "+
	   "an IllegalArgumentException");
    } catch(IllegalArgumentException iae) {
    }
  }

  public void testDoesNotChangeContentWithoutTag() throws IOException {
    String content = "There is no tag in this content";
    assertFilterString(content, content, array1);
  }

  public void testDoesNotChangeContentWithTagButNoMatch() throws IOException {
    String content = "There is a tag here in this content\n" +
	"and another tag here";
    assertFilterString(content, content, array1);
  }

  public void testReadReturnsNegOneWhenEmpty() throws IOException {
    CssLinkFilter reader =
	CssLinkFilter.makeNestedFilter(new StringReader(""), array1, true);
    char[] buf = new char[10];
    assertEquals(-1, reader.read());
    assertEquals(-1, reader.read(buf));
    reader = CssLinkFilter.makeNestedFilter(new StringReader("foobar"), array1,
					    true);
    assertEquals("foobar", StringUtil.fromReader(reader));
    assertEquals(-1, reader.read());
    assertEquals(-1, reader.read(buf));
  }

  public void testReplacesWhenTagFollowedByMatch() throws IOException {
    String content = "There is a tag here replace me in this content";
    String expected = "There is a tag here replaced him in this content";
    assertFilterString(expected, content, array1);
  }

  public void dontTestReplacesWhenTagFollowedLaterByMatch() throws IOException {
    String content = "A tag here followed by replace me in this content";
    String expected = "A tag here followed by replaced him in this content";
    assertFilterString(expected, content, array1);
  }

  public void testReplacesWhenTagFollowedByMatchMultiLength()
      throws IOException {
    String content = "tag here replace me in this content";
    String expected = "tag here replaced him in this content";
    String prefix = "+";
    for (int i = 0; i < 100; i++) {
      StringBuffer sb = new StringBuffer();
      for (int j = 0; j < i; j++) {
        sb.append(prefix);
      }
      String pad = sb.toString();
      assertFilterString(pad + expected, pad + content, array1);
    }
  }

  /** Check that the filtered string matches expected.  Test with varying
   * buffer lengths and offsets */
  private void assertFilterString(String expected, String input,
				  String[][] array)
      throws IOException {
    for (int len = 1; len <= input.length() * 2; len++) {
      Reader reader = CssLinkFilter.makeNestedFilter(new StringReader(input),
						     array, true);
      assertReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
    for (int len = 1; len <= input.length() * 2; len++) {
      Reader reader = CssLinkFilter.makeNestedFilter(new StringReader(input),
						     array, true);
      assertOffsetReaderMatchesString(expected, reader, len);
      assertEquals(-1, reader.read());
    }
      Reader reader = CssLinkFilter.makeNestedFilter(new StringReader(input),
						     array, true);
    assertReaderMatchesStringSlow(expected, reader);
    assertEquals(-1, reader.read());
  }

}
