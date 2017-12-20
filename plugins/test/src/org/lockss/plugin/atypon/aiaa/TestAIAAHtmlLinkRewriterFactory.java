/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.aiaa;

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestAIAAHtmlLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  AIAAHtmlLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AIAAHtmlLinkRewriterFactory();
  }
  
  static final String testLinkSubmitArticles = 
      "<div>\n" +
     "  <a href=\"javascript:submitArticles(document.frmAbs, '/action/showCitFormats'," + 
          "'Please check at least one article.')\">Download to Citation Manager</a>\n" +
    "</div>";
  
  // the AIAA link rewriter prewrites this javascript as a regular link similar to the link extractor
  // after which the standard ServeContent rewrite is applied
  static final String testLinkSubmitArticlesRewritten =
      "<div>\n" +
     "  <a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Faction%2FshowCitFormats%3Fdoi%3D10.1111%2F10articlefoo\"" +
    ">Download to Citation Manager</a>\n" +
     "</div>";
      

  static final String testBaseRewriting =
    "<form action=\"/action/downloadCitation\" name=\"frmCitmgr\" method=\"post\" target=\"_self\"> " +
      "<input type='submit' name='submit' value='Download article citation data' " + 
       " onclick=\"onCitMgrSubmit()\" class=\"formbutton\"/> " +
      "</div>" +
      "</form>";
  
  static final String testBaseRewritingRewritten =
      "<form action=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Faction%2FdownloadCitation%3Fdoi%3D10.3920%252FCEP160023%26format%3Dris%26include%3Dcit\" " +
          "name=\"frmCitmgr\" method=\"post\" target=\"_self\"> " +
          "<input type='submit' name='submit' value='Download article citation data'  " +
          "onclick=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Faction%2FdownloadCitation%3Fdoi%3D10.3920%252FCEP160023%26format%3Dris%26include%3Dcit\" " +
          "class=\"formbutton\"/>" +
          " </div></form>";

  /**
   * Make a basic AIAA test AU to which URLs can be added.
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


  public void testJavaScriptFunctionLinkRewritingShowCitJavascript() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkSubmitArticles.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/doi/full/10.1111/10articlefoo", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testLinkSubmitArticlesRewritten, fout);
  }
  
  // make sure inheritence from parent is working
  public void testBaseLinkRewriting() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testBaseRewriting.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/action/showCitFormats?doi=10.3920%2FCEP160023", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testBaseRewritingRewritten, fout);
  }

}
