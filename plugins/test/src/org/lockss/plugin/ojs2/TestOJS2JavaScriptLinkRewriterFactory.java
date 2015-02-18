/*
 * $Id$
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs2;

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestOJS2JavaScriptLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  OJS2JavaScriptLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new OJS2JavaScriptLinkRewriterFactory();
  }
  
  static final String testLinkInputAbsPath = 
      "<div>\n"
    + "  <a href=\"javascript:openRTWindow('http://www.xyz.com/path/leaf');\">http://www.xyz.com/path/leaf</a>\n"
    + "</div>";
  
  static final String testLinkInputRelPath1 = 
      "<div>\n"
    + "  <a href=\"javascript:openRTWindow('/path/leaf');\">http://www.xyz.com/path/leaf</a>\n"
    + "</div>";
  
  static final String testLinkInputRelPath2 = 
      "<div>\n"
    + "  <a href=\"javascript:openRTWindow('leaf');\">http://www.xyz.com/path/leaf</a>\n"
    + "</div>";
  
  static final String testLinkOutput =
      "<div>\n"
    + "  <a href=\"javascript:openRTWindow('http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fpath%2Fleaf');\">http://www.xyz.com/path/leaf</a>\n"
    + "</div>";
      
  static final String testRefreshInputAbsPath =
      "<head>\n"
    + "  <meta http-equiv=\"refresh\" content=\"2;url=javascript:openRTWindow('http://www.xyz.com/path/leaf');\">\n"
    + "</head>";
  
  static final String testRefreshInputRelPath1 =
      "<head>\n"
    + "  <meta http-equiv=\"refresh\" content=\"2;url=javascript:openRTWindow('/path/leaf');\">\n"
    + "</head>";
  
  static final String testRefreshInputRelPath2 =
      "<head>\n"
    + "  <meta http-equiv=\"refresh\" content=\"2;url=javascript:openRTWindow('leaf');\">\n"
    + "</head>";
  
  static final String testRefreshOutput =
      "<head>\n"
    + "  <meta http-equiv=\"refresh\" content=\"2;url=javascript:openRTWindow('http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fpath%2Fleaf');\">\n"
    + "</head>";
  
  static final String testLinkInputNoRewritePath = 
      "<div>\n"
    + "  <a href=\"javascript:function('http://www.xyz.com/path/leaf');\">http://www.xyz.com/path/leaf</a>\n"
    + "</div>";
  

  
  /**
   * Make a basic Ingenta test AU to which URLs can be added.
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

  public void testJavaScriptFunctionLinkRewritingAbsolutePath() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkInputAbsPath.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testLinkOutput, fout);
  }

  public void testJavaScriptFunctionLinkRewritingRelativePath1() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkInputRelPath1.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testLinkOutput, fout);
  }

  public void testJavaScriptFunctionLinkRewritingRelativePath2() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkInputRelPath2.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testLinkOutput, fout);
  }

  public void testJavaScriptFunctionLinkRewritingRelativePath3() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkOutput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testLinkOutput, fout);
  }

  public void testJavaScriptFunctionRefreshRewritingAbsolutePath() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testRefreshInputAbsPath.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testRefreshOutput, fout);
  }

  public void testJavaScriptFunctionRefreshRewritingRelativePath1() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testRefreshInputRelPath1.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testRefreshOutput, fout);
  }

  public void testJavaScriptFunctionRefreshRewritingRelativePath2() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testRefreshInputRelPath2.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testRefreshOutput, fout);
  }

  public void testJavaScriptFunctionRefreshRewritingRelativePath3() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testRefreshOutput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testRefreshOutput, fout);
  }

  public void testJavaScriptFunctionNoRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkInputNoRewritePath.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testLinkInputNoRewritePath, fout);
  }
}
