/*
 * $Id: TestStringFilter.java,v 1.2 2003-10-07 22:22:42 troberts Exp $
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

  public void testDoesntFilterIfNoMatchingString() throws IOException {
    String str = "This is a test string";
    StringFilter sf = new StringFilter(new StringReader(str), "REMOVE");
    assertReaderMatchesString(str, sf);
  }

  //Series of tests where the buffer size is larget than the filtered string
  public void testFiltersOneString() throws IOException {
    String str = "This is a REMOVEtest string";
    StringFilter sf = new StringFilter(new StringReader(str), "REMOVE");
    assertReaderMatchesString("This is a test string", sf);
  }

  public void testFiltersOneStringBeginning() throws IOException {
    String str = "REMOVEThis is a test string";
    StringFilter sf = new StringFilter(new StringReader(str), "REMOVE");
    assertReaderMatchesString("This is a test string", sf);
  }

  public void testFiltersMultiCharRead() throws IOException {
    String str = "ThisREMOVE is a test string";
    StringFilter sf = new StringFilter(new StringReader(str), "REMOVE");
    char buf[] = new char[21];
    sf.read(buf);
    System.err.println("|"+new String(buf)+"|");
    assertEquals("This is a test string", new String(buf));
  }

  public void testFiltersOneStringEnd() throws IOException {
    String str = "This is a test stringREMOVE";
    StringFilter sf = new StringFilter(new StringReader(str), "REMOVE");
    assertReaderMatchesString("This is a test string", sf);
  }

  public void testFiltersOneStringMulti() throws IOException {
    String str = "This is a REMOVEtest stringREMOVE";
    StringFilter sf = new StringFilter(new StringReader(str), "REMOVE");
    assertReaderMatchesString("This is a test string", sf);
  }

  //Series of tests where the buffer size is the same size as the
  //filtered string
  public void testFiltersOneStringSameSizeAsBuff() throws IOException {
    String config = "org.lockss.filter.buffer_capacity=6";
    ConfigurationUtil.setCurrentConfigFromString(config);
    String str = "REMOVEThis is a test string";
    StringFilter sf = new StringFilter(new StringReader(str), "REMOVE");
    assertReaderMatchesString("This is a test string", sf);
  }

  public void testFiltersOneStringOverflowBuffer() throws IOException {
    String config = "org.lockss.filter.buffer_capacity=6";
    ConfigurationUtil.setCurrentConfigFromString(config);
    String str = "ThisREMOVE is a test string";
    StringFilter sf = new StringFilter(new StringReader(str), "REMOVE");
    assertReaderMatchesString("This is a test string", sf);
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
      StringFilter.makeNestedFilter(new StringReader("test"), null);
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
    StringFilter sf =
      StringFilter.makeNestedFilter(new StringReader(str),
 				    ListUtil.list("REMOVE", "ALSO"));
    assertReaderMatchesString("This is a test string", sf);
  }

  public static void main(String[] argv) {
    String[] testCaseList = { TestStringFilter.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }

}
