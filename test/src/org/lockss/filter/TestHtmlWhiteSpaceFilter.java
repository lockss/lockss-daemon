/*
 * $Id: TestHtmlWhiteSpaceFilter.java,v 1.1 2013-09-06 19:02:08 alexandraohlson Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.io.*;

import org.lockss.test.*;
import org.lockss.util.*;

public class TestHtmlWhiteSpaceFilter extends LockssTestCase {
  public static final String htmlStart = "<html><head></head><body><div>";
  public static final String htmlEnd = "</div></body></html>";

/* Do not need to duplicate the WhiteSpaceFilter tests, just be sure the HTML
 * variations do what you think they are doing
 */

  // does it collapse all white space to 1 space?
  public void testCollapseWhiteSpace() throws IOException {
    assertFilterStringHtml("Test frob", "Test  frob");
    assertFilterStringHtml("Test frob", "   Test  frob   ");
  }

  public void testDoesntCollapseSingleSpace() throws IOException {
    assertFilterStringHtml("Test frob", "Test frob");
    assertFilterStringHtml("Test frob", " Test frob ");
  }

  // does it turn &nbsp in to a space
  public void testNbspToSpace() throws IOException {
    String testString = "Test&nbspfrob&nbsp";
    assertFilterStringHtml("Test frob", testString);
  }

  // does it turn the &nbsp in to a space BEFORE collapsing space
  public void testNbspBeforeCollapsing() throws IOException {
    String testString = "Test&nbspfrob &nbsp boo";
    assertFilterStringHtml("Test frob boo", testString);
  }
  
  // does it remove single spaces between tags? <tag>X</tag>
  // does it collapse all space BEFORE removing singletons between tags?
  public void testInterTagSpaces() throws IOException {
    String testString = "<div> </div>";
    assertFilterStringHtml("<div></div>", testString);
    assertFilterStringHtml("<div></div>", "<div>        </div>");
  }

  // does it remove all single spaces before a tag open?
  public void testBeforeTagSpaces() throws IOException {
    String testString = "<table>foo<tr><td> foo</td></tr></table";
    assertFilterStringHtml("<table>foo<tr><td>foo</td></tr></table>", testString);
  }
  
  // does it remove all single spaces after a tag close?
  public void testAfterTagSpaces() throws IOException {
    String testString = "<table>foo<tr><td>foo </td></tr></table";
    assertFilterStringHtml("<table>foo<tr><td>foo</td></tr></table>", testString);
  }

  public void testTagSpacesCollapse() throws IOException {
    String testString = "<table>foo<tr>   <td>   foo   </td> </tr>    </table";
    assertFilterStringHtml("<table>foo<tr><td>foo</td></tr></table>", testString);
  }
  
  
  private void assertFilterStringHtml(String expected, String input)
      throws IOException {
    // make the string or html snippet valid html by wrapping it 
    String htmlExpected = htmlStart + expected + htmlEnd;
    String htmlInput = htmlStart + expected + htmlEnd;
    
    Reader reader = new HtmlWhiteSpaceFilter(new StringReader(htmlInput));
    assertReaderMatchesStringSlow(htmlExpected, reader);
    assertEquals(-1, reader.read());
  }

}
