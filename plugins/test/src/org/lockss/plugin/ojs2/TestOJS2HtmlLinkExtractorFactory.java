/*
 * $Id: TestOJS2HtmlLinkExtractorFactory.java,v 1.2 2015-01-30 22:54:39 etenbrink Exp $
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.test.*;

public class TestOJS2HtmlLinkExtractorFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  OJS2HtmlLinkExtractorFactory fact;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    fact = new OJS2HtmlLinkExtractorFactory();
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
  
  static final String testMetaInput =
      "<head>\n" +
      "  <meta name=\"citation_pdf_url\"           content=\"http://www.xyz.com/index.php/edui/article/download/2850/5314\"/>\n" + 
      "  <meta name=\"citation_fulltext_html_url\" content=\"http://www.xyz.com/index.php/edui/article/view/2850/5315\"/>\n" + 
      "  <meta name=\"citation_fulltext_html_url\" content=\"http://www.xyz.com/index.php/edui/article/view/2850/5317\"/>\n" + 
      "  <meta name=\"citation_fulltext_html_url\" content=\"http://www.xyz.com/index.php/edui/article/view/2850/5318\"/>\n" + 
      "  <meta name=\"citation_abstract_url\"      content=\"http://www.xyz.com/index.php/edui/article/view/2850\"/>\n" + 
      "</head>";
  
  static final String expectedAbsLinkPath = "http://www.xyz.com/path/leaf";
  static final String expectedRelLinkPath = "http://www.xyz.com/foo/leaf";


  
  /**
   * Make a basic OJS test AU to which URLs can be added.
   * 
   * @return a basic OJS test AU
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

  public void testJavaScriptFunctionLinkExtractingAbsolutePath() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkInputAbsPath.getBytes());
    LinkExtractor ext  = 
        fact.createLinkExtractor("text/html");
    final List<String> foundLink = new ArrayList<String>();
    ext.extractUrls(mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", new Callback() {
      @Override
      public void foundLink(String url) {
        foundLink.add(url);
      }
    });
    assertEquals(Collections.singletonList(expectedAbsLinkPath), foundLink);
  }

  public void testJavaScriptFunctionLinkExtractingRelativePath1() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkInputRelPath1.getBytes());
    LinkExtractor ext  = 
        fact.createLinkExtractor("text/html");
    final List<String> foundLink = new ArrayList<String>();
    ext.extractUrls(mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", new Callback() {
      @Override
      public void foundLink(String url) {
        foundLink.add(url);
      }
    });
    assertEquals(Collections.singletonList(expectedAbsLinkPath), foundLink);
  }

  public void testJavaScriptFunctionLinkExtractingRelativePath2() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkInputRelPath2.getBytes());
    LinkExtractor ext  = 
        fact.createLinkExtractor("text/html");
    final List<String> foundLink = new ArrayList<String>();
    ext.extractUrls(mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", new Callback() {
      @Override
      public void foundLink(String url) {
        foundLink.add(url);
      }
    });
    assertEquals(Collections.singletonList(expectedRelLinkPath), foundLink);
  }

  public void testJavaScriptFunctionRefreshExtractingAbsolutePath() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testRefreshInputAbsPath.getBytes());
    LinkExtractor ext  = 
        fact.createLinkExtractor("text/html");
    final List<String> foundLink = new ArrayList<String>();
    ext.extractUrls(mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", new Callback() {
      @Override
      public void foundLink(String url) {
        foundLink.add(url);
      }
    });
    assertEquals(Collections.singletonList(expectedAbsLinkPath), foundLink);
  }

  public void testJavaScriptFunctionRefreshExtractingingRelativePath1() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testRefreshInputRelPath1.getBytes());
    LinkExtractor ext  = 
        fact.createLinkExtractor("text/html");
    final List<String> foundLink = new ArrayList<String>();
    ext.extractUrls(mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", new Callback() {
      @Override
      public void foundLink(String url) {
        foundLink.add(url);
      }
    });
    assertEquals(Collections.singletonList(expectedAbsLinkPath), foundLink);
  }

  public void testJavaScriptFunctionRefreshExtractingRelativePath2() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testRefreshInputRelPath2.getBytes());
    LinkExtractor ext  = 
        fact.createLinkExtractor("text/html");
    final List<String> foundLink = new ArrayList<String>();
    ext.extractUrls(mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", new Callback() {
      @Override
      public void foundLink(String url) {
        foundLink.add(url);
      }
    });
    assertEquals(Collections.singletonList(expectedRelLinkPath), foundLink);
  }
  
  public void testMetaExtracting() throws Exception {
    ArrayList<String> expectedMetaLinks = new ArrayList<String>();
    expectedMetaLinks.add("http://www.xyz.com/index.php/edui/article/download/2850/5314"); // citation_pdf_url
    expectedMetaLinks.add("http://www.xyz.com/index.php/edui/article/view/2850/5315"); // citation_fulltext_html_url
    expectedMetaLinks.add("http://www.xyz.com/index.php/edui/article/view/2850/5317"); // citation_fulltext_html_url
    expectedMetaLinks.add("http://www.xyz.com/index.php/edui/article/view/2850/5318"); // citation_fulltext_html_url
    
    MockArchivalUnit mockAu = makeAu();
    InputStream in = new ByteArrayInputStream(testMetaInput.getBytes());
    LinkExtractor ext  = fact.createLinkExtractor("text/html");
    final List<String> foundLink = new ArrayList<String>();
    ext.extractUrls(mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", new Callback() {
      @Override
      public void foundLink(String url) {
        foundLink.add(url);
      }
    });
    assertEquals(expectedMetaLinks, foundLink);
  }
  
}
