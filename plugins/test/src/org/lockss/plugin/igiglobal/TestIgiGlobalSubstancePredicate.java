/**
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

package org.lockss.plugin.igiglobal;

import java.util.List;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;
import org.lockss.plugin.igiglobal.IgiGlobalSubstancePredicateFactory.IgiGlobalSubstancePredicate;


public class TestIgiGlobalSubstancePredicate extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;
  private IgiGlobalSubstancePredicateFactory sfact;
  private IgiGlobalSubstancePredicate subP;
  
  private static final String BASE_URL = "http://www.igi-global.com/";
  private final String url_abs = BASE_URL + "gateway/article/148741";
  private final String url_abs2 = BASE_URL + "gateway/article/148742";
  private final String url_full = BASE_URL + "gateway/article/full-text-html/148741";
  private final String url_pdf = BASE_URL + "gateway/article/full-text-pdf/148741";
  private final String url_viewtitle = BASE_URL + "viewtitle.aspx?titleid=148741";
  private final String url_pdfAspx = BASE_URL + "pdf.aspx?tid=148741&ptid=118392&ctid=4&t=Call+For+Articles";
  private final String url_pdfAspx2 = BASE_URL + "pdf.aspx?tid=148741&ptid=118392&ctid=4&t=Landing+Page+Again";
  
//  protected static Logger logger = Logger.getLogger(TestIgiGlobalSubstancePredicate.class);
  
  private CIProperties htmlHeader;
  private CIProperties pdfHeader;
  private final String goodHtmlContent = "<html><body><h1>It works!</h1></body></html>";
  private final String goodPdfContent = "Foo"; // doesn't really matter
  
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...
    
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau = new MockArchivalUnit();
    mau.setConfiguration(auConfig());
    
    // This is the same as the IgiGlobalPlugin's substance patterns
    List subPat = ListUtil.list(
        "/gateway/article/full-text-html/[0-9]+$",
        "/gateway/article/full-text-pdf/[0-9]+$",
        "/viewtitle[.]aspx[?]titleid=[0-9]+$", 
        "/pdf[.]aspx[?]");
    // set the substance check pattern on the mock AU
    mau.setSubstanceUrlPatterns(compileRegexps(subPat));   
    sfact = new IgiGlobalSubstancePredicateFactory();
    subP = sfact.makeSubstancePredicate(mau);
    
    // set up headers to use in the tests
    htmlHeader = new CIProperties();    
    htmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    pdfHeader = new CIProperties();    
    pdfHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
    
    
  }
  
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume", "2");
    conf.put("journal_issn","1546-2234");
    return conf;
  }
  
  public void tearDown() throws Exception {
    getMockLockssDaemon().stopDaemon();
    super.tearDown();
  }
  
  List<Pattern> compileRegexps(List<String> regexps)
      throws MalformedPatternException {
    return RegexpUtil.compileRegexps(regexps);
  }
  
  
  // a full html version of the article 
  public void testSubstantiveHtml() throws Exception {
    
    MockCachedUrl cu = mau.addUrl(url_full, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    cu = mau.addUrl(url_pdf, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    
    // for igiglobal substance must be full or pdf AND the mime-type
    // must match the type of file 
    assertTrue(subP.isSubstanceUrl(url_full));
    assertTrue(subP.isSubstanceUrl(url_pdf));
  }
  
  
  // a pdf of the article
  public void testSubstantivePdfFiles() throws Exception {
    
    MockCachedUrl cu = mau.addUrl(url_pdfAspx, true, true, pdfHeader);
    cu.setContent(goodPdfContent);
    cu.setContentSize(goodPdfContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
    cu = mau.addUrl(url_pdfAspx2, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    cu = mau.addUrl(url_viewtitle, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    
    // for igiglobal substance must be full or pdf AND the mime-type
    // must match the type of file 
    assertTrue(subP.isSubstanceUrl(url_pdfAspx));
    assertFalse(subP.isSubstanceUrl(url_pdfAspx2));
    assertTrue(subP.isSubstanceUrl(url_viewtitle));
  }
  
  
  // Abstracts are not substance
  public void testAbstractFiles() throws Exception {
    
    MockCachedUrl cu = mau.addUrl(url_abs, true, true, htmlHeader);
    mau.addUrl(url_abs, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    
    // a mismatched url and mime-type
    MockCachedUrl cu2 = mau.addUrl(url_abs2, true, true, pdfHeader);
    mau.addUrl(url_abs2, true, true, pdfHeader);
    cu2.setContent(goodHtmlContent);
    cu2.setContentSize(goodHtmlContent.length());
    cu2.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
    
    assertFalse(subP.isSubstanceUrl(url_abs));
    assertFalse(subP.isSubstanceUrl(url_abs2));
  }
  
}

