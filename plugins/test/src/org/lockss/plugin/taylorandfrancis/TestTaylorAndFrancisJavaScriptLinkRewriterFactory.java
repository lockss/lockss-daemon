/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

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
