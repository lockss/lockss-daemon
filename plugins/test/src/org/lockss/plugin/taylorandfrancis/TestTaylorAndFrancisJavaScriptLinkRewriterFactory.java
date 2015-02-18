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

package org.lockss.plugin.taylorandfrancis;

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestTaylorAndFrancisJavaScriptLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  TaylorAndFrancisJavaScriptLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new TaylorAndFrancisJavaScriptLinkRewriterFactory();
  }
  
  /** Input test case for window.open() */
  static final String testOnClickLinkInput = 
      "<p onclick=\"window.open('leaf',"
    +     "'MyWindow','toolbar=no,status=no,menubar=no,width=795,height=520,"
    +     "scrollbars=yes,top=0,left=0')\">\n"
    + "  http://www.xyz.com/path/leaf"
    + "</p>";
  
  /** Output test case with rewritten URL */
  static final String testOnClickLinkOutput =
      "<p onclick=\"window.open("
    +   "'http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fpath%2Fleaf',"
    +   "'MyWindow','toolbar=no,status=no,menubar=no,width=795,height=520,"
    +   "scrollbars=yes,top=0,left=0')\">\n"
    + "  http://www.xyz.com/path/leaf"
    + "</p>";

  /** Input test case for javascript:window.close() */
  static final String testWindowCloseInput = 
      "<a href='javascript:window.close()'>";
  
  /** Output test case for rewritten javascript call */
  static final String testWindowCloseOutput = 
      "<a href='javascript:window.close()'>";
  
  
  /**
   * Make a basic TaylorAndFrancis test AU to which URLs can be added.
   * 
   * @return a basic Ingenta test AU
   * @throws ConfigurationException if can't set configuration
   */
  MockArchivalUnit makeAu() throws ConfigurationException {
    MockArchivalUnit mau = new MockArchivalUnit();
    Configuration config =ConfigurationUtil.fromArgs(
        "base_url", "http://www.xyz.com/");
    mau.setConfiguration(config);
    mau.setUrlStems(ListUtil.list(
        "http://www.xyz.com/"
        ));
    return mau;
  }

  /**
   * Test rewriting a link from the first argument of the window.open()
   * call in an "onClick" attribute on a paragraph tag.
   *  
   * @throws Exception
   */
  public void testJavaScriptFunctionOpenLinkRewriting() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testOnClickLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = fact.createLinkRewriter(
                                 "text/html", mockAu, in, "UTF-8", 
                                 "http://www.xyz.com/path/path1", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testOnClickLinkOutput, fout);
  }
  
  /**
   * Test rewriting a link from the first argument of the window.open()
   * call in an "onClick" attribute on a paragraph tag.
   *  
   * @throws Exception
   */
  public void testJavaScriptFunctionCloseLinkRewriting() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testWindowCloseInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = fact.createLinkRewriter(
                                 "text/html", mockAu, in, "UTF-8", 
                                 "http://www.xyz.com/path/path1", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testWindowCloseOutput, fout);
  }

}
