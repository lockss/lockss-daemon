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

package org.lockss.plugin.highwire;

import java.util.List;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.plugin.highwire.HighWireDrupalSubstancePredicate.DrupalSubstancePredicate;

public class TestHighWireDrupalSubstancePredicate extends LockssTestCase {
  private MockArchivalUnit mau;
  private HighWireDrupalSubstancePredicate.Factory sfact;
  private DrupalSubstancePredicate subP;
  
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private static final String BASE_URL = "http://www.example.com/";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, "1");
  
  private final String url_abs = BASE_URL + "content/1/1/104";
  private final String url_abs2 = BASE_URL + "content/1/1/104.abstract";
  private final String url_full = BASE_URL + "content/1/1/104.full";
  private final String url_pdf = BASE_URL + "content/1/1/104.full.pdf";
  private final String url_pdf2 = BASE_URL + "content/1/1/199.full.pdf";
  private final String url_pdf_landing = BASE_URL + "content/1/1/104.full.pdf+html";
  private final String url_pdf_landing_i1 = BASE_URL + "content/1/1/104.full.pdf+html?frame=header";
  private final String url_pdf_landing_i2 = BASE_URL + "content/1/1/104.full.pdf+html?frame=sidebar";
  
  private CIProperties htmlHeader;
  private CIProperties pdfHeader;
  private final String goodHtmlContent = "<html><body><h1>It works!</h1></body></html>";
  private final String goodPdfContent = "Foo"; // doesn't really matter
  
  
  public void setUp() throws Exception {
    super.setUp();
    mau = new MockArchivalUnit();
    mau.setConfiguration(AU_CONFIG);
    
    // This is the same as the Plugin's substance patterns
    List subPat = ListUtil.list(
        "^https?://www.example.com/content(/[^/.]+)?/([^/.]+)(/[^/.]+)?/(((?:bmj\\.)?[^/.]+?|\\d+\\.\\d+))(\\.(?:full([.]pdf)?)?)$"
        );
    // set the substance check pattern on the mock AU
    mau.setSubstanceUrlPatterns(compileRegexps(subPat));   
    sfact = new HighWireDrupalSubstancePredicate.Factory();
    subP = sfact.makeSubstancePredicate(mau);
    
    // set up headers to use in the tests
    htmlHeader = new CIProperties();    
    htmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    pdfHeader = new CIProperties();    
    pdfHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
    
    
  }
  
  public void tearDown() throws Exception {
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
    cu = mau.addUrl(url_pdf_landing, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    cu = mau.addUrl(url_pdf_landing_i1, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    cu = mau.addUrl(url_pdf_landing_i2, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    
    // substance must be full or pdf AND the mime-type must match the type of file 
    assertTrue(subP.isSubstanceUrl(url_full));
    assertFalse(subP.isSubstanceUrl(url_pdf_landing));
    assertFalse(subP.isSubstanceUrl(url_pdf_landing_i1));
    assertFalse(subP.isSubstanceUrl(url_pdf_landing_i2));
  }
  
  
  // a pdf of the article
  public void testSubstantivePdfFiles() throws Exception {
    
    MockCachedUrl cu = mau.addUrl(url_pdf, true, true, pdfHeader);
    cu.setContent(goodPdfContent);
    cu.setContentSize(goodPdfContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_PDF);
    cu = mau.addUrl(url_pdf2, true, true, htmlHeader);
    cu.setContent(goodHtmlContent);
    cu.setContentSize(goodHtmlContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_HTML);
    
    // substance must be full or pdf AND the mime-type must match the type of file 
    assertTrue(subP.isSubstanceUrl(url_pdf));
    assertFalse(subP.isSubstanceUrl(url_pdf2));
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

