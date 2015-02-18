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

package org.lockss.plugin.ingenta;

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestIngentaHtmlLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  IngentaHtmlLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new IngentaHtmlLinkRewriterFactory();
  }

  /**
   * Make a basic Ingenta test AU to which URLs can be added.
   * 
   * @return a basic Ingenta test AU
   * @throws ConfigurationException if can't set configuration
   */
  MockArchivalUnit makeAu() throws ConfigurationException {
    MockArchivalUnit mau = new MockArchivalUnit();
    Configuration config =ConfigurationUtil.fromArgs(
        "base_url", "http://www.ingentaconnect.com/",
        "api_url", "http://api.ingentaconnect.com/",
        "graphics_url", "http://graphics.ingentaconnect.com/");
    mau.setConfiguration(config);
    mau.setUrlStems(ListUtil.list(
        "http://www.ingentaconnect.com/",
        "http://api.ingentaconnect.com/",
        "http://graphics.ingentaconnect.com/"
        ));
    return mau;
  }

  static final String input_1 =
    "/org/lockss/plugin/ingenta/IngentaHtmlLinkRewriter_input_1.html";
  static final String output_1 = 
    "/org/lockss/plugin/ingenta/IngentaHtmlLinkRewriter_output_1.html";
  static final String output_2 = 
      "/org/lockss/plugin/ingenta/IngentaHtmlLinkRewriter_output_2.html";
  static final String output_3 = 
      "/org/lockss/plugin/ingenta/IngentaHtmlLinkRewriter_output_3.html";
  static final String output_4 = 
      "/org/lockss/plugin/ingenta/IngentaHtmlLinkRewriter_output_4.html";
  
  static final String baseInUrl =
      "http://www.ingentaconnect.com/content/manup/vcb/2004/00000005/00000001/art00001";
    
  static final String baseOutUrl = 
      "http://api.ingentaconnect.com/content/manup/vcb/2004/00000005/00000001/art00001";
    

  /**
   * Paameterized test to see if links of the input resource file
   * are rewritten as specified by the output resource file.
   * 
   * @param mau the AU for the rewriting
   * @param inRes the name of the file resource to read
   * @param outRes the name of the file resource to compare against
   * @throws Exception if something bad happens
   */
  void pageRewriteTest(MockArchivalUnit mau, String inRes, String outRes) 
      throws Exception {
    InputStream input = null;
    InputStream filtered = null;
    InputStream expected = null;

    try {
      input = getResourceAsStream(inRes);
      ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
        @Override
        public String rewrite(String url) {
          // TODO Auto-generated method stub
          return "/ServeContent?url=" + UrlUtil.encodeQueryArg(url);
        }
      }; 
      filtered = fact.createLinkRewriter("text/html", 
        mau, input, "UTF-8", 
        baseInUrl, 
        xfm);
      expected = getResourceAsStream(outRes);
      String s_expected = StringUtil.fromInputStream(expected);
      String s_filtered = StringUtil.fromInputStream(filtered); 
      assertEquals(s_expected, s_filtered);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw ex;
    } finally {
      IOUtil.safeClose(input);
      IOUtil.safeClose(filtered);
      IOUtil.safeClose(expected);
    }
  }
  
  /**
   * Test when HTML CU has MIME type and PDF CU does not.
   * @throws Exception if something goes wrong
   */
  public void testCusWithHtmlMimeTypeOnly() throws Exception {
    MockArchivalUnit mau = makeAu();
    CIProperties pdfHeaders = new CIProperties();
    pdfHeaders.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true", 
               true, // exists
               true, //shouldCache
               pdfHeaders);

    CIProperties htmlHeaders = new CIProperties();
    htmlHeaders.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true&mimetype=text/html", 
               true, // exists
               true, //shouldCache
               htmlHeaders);

    pageRewriteTest(mau, input_1, output_1);
  }
  
  /**
   * Test when HTML CU has no MIME type and PDF CU does.
   * @throws Exception if something goes wrong
   */
  public void testCusWithPdfMimeTypeOnly() throws Exception {
    MockArchivalUnit mau = makeAu();
    CIProperties pdfHeaders = new CIProperties();
    pdfHeaders.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true&mimetype=application/pdf", 
               true, // exists
               true, //shouldCache
               pdfHeaders);

    CIProperties htmlHeaders = new CIProperties();
    htmlHeaders.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true", 
               true, // exists
               true, //shouldCache
               htmlHeaders);

    pageRewriteTest(mau, input_1, output_2);
  }
  
  /**
   * Test when both HTML and PDF CUs have MIME types.
   * @throws Exception if something goes wrong
   */
  public void testCusBothWithMimeTypes() throws Exception {
    MockArchivalUnit mau = makeAu();
    CIProperties pdfHeaders = new CIProperties();
    pdfHeaders.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true&mimetype=application/pdf", 
               true, // exists
               true, //shouldCache
               pdfHeaders);

    CIProperties htmlHeaders = new CIProperties();
    htmlHeaders.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true&mimetype=text/html", 
               true, // exists
               true, //shouldCache
               htmlHeaders);

    pageRewriteTest(mau, input_1, output_3);
  }
  
  /**
   * Test when there are HTML and PDF CUs with and without MIME types.
   * @throws Exception if something goes wrong
   */
  public void testCusWithAndWithoutMimeTypes() throws Exception {
    MockArchivalUnit mau = makeAu();
    CIProperties pdfHeaders = new CIProperties();
    pdfHeaders.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true", 
               true, // exists
               true, //shouldCache
               pdfHeaders);

    mau.addUrl(baseOutUrl + "?crawler=true&mimetype=application/pdf", 
        true, // exists
        true, //shouldCache
        pdfHeaders);

    CIProperties htmlHeaders = new CIProperties();
    htmlHeaders.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true", 
               true, // exists
               true, //shouldCache
               htmlHeaders);

    mau.addUrl(baseOutUrl + "?crawler=true&mimetype=text/html", 
        true, // exists
        true, //shouldCache
        htmlHeaders);
    
    pageRewriteTest(mau, input_1, output_3);
  }
  
  /**
   * Test when there are no matching CUs.In this case, no ingenta-specific
   * redirection to 'api.ingentaconnect.com' is done (shouldn't happen in
   * real life, but best to test it).
   * 
   * @throws Exception if something goes wrong
   */
  public void testWithNoCus() throws Exception {
    MockArchivalUnit mau = makeAu();
    pageRewriteTest(mau, input_1, output_4);
  }
  
  /**
   * Test when both HTML and PDF CUs have no content.
   * @throws Exception if something goes wrong
   */
  public void testCusWithNoContent() throws Exception {
    MockArchivalUnit mau = makeAu();
    CIProperties pdfHeaders = new CIProperties();
    pdfHeaders.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true&mimetype=application/pdf", 
               false, // exists -- false for no content
               true, //shouldCache
               pdfHeaders);
    
    CIProperties htmlHeaders = new CIProperties();
    htmlHeaders.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    mau.addUrl(baseOutUrl + "?crawler=true&mimetype=text/html", 
               false, // exists -- false for no content
               true, //shouldCache
               pdfHeaders);
    pageRewriteTest(mau, input_1, output_4);
  }

}
