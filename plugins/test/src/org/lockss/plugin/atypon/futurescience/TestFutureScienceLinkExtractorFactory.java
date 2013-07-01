/*
 * $Id: TestFutureScienceLinkExtractorFactory.java,v 1.1 2013-07-01 22:18:05 alexandraohlson Exp $
 */
/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.atypon.futurescience;

/* used by the temporary read from file portion */
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.RegexpCssLinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.IOUtil;
import org.lockss.util.SetUtil;
import org.lockss.util.StringUtil;


public class TestFutureScienceLinkExtractorFactory extends LockssTestCase {
  UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

  private FutureScienceLinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String FS_BASE_URL = "http://future-science.com/";
  private static final String DOI_START = "11.1111";
  private static final String DOI_END = "TEST";
  
  private static final String citationForm=
      "<html><head><title>Test Title</title></head><body>" +
      "<div class=\"citationFormats\">" +
          " <p>Download a citation file in RIS format.</p>" +
          "    <hr/>" +
          "    <!-- list of articles -->" +
          "    <div class=\"articleList\">" +
          "        <span class=\"sectionTitle\">Download article metadata for:</span>" +
          "        <hr/>" +
          "        <div class=\"art_title\">" +
          "            <a href=\"/doi/abs/" + DOI_START + "/" + DOI_END + "\">Conference Report</a>" +
          "        </div>" +
          "        <div class=\"art_authors\">Naidong Weng, Jenny Zheng, and Mike Lee</div>" +
          "        <span class=\"journalName\">Bioanalysis</span>" +
          "        <span class=\"year\">2012</span>" +
          "            <span class=\"volume\">4</span>:<span class=\"issue\">15</span>," +
          "                    <span class=\"page\">1843-1847" +
          "                    </span>" +
          "        <br/>" +
          "        <hr/>" +
          "    </div>" +
          "    <br/>" +
          "    <!-- download options -->" +
          "    <form action=\"/action/downloadCitation\" id=\"downloadCitation\" name=\"frmCitmgr\" method=\"post\" " +
          "target=\"_self\"><input type=\"hidden\" name=\"doi\" value=\"" + DOI_START + "/" + DOI_END + "\" />" +
          "            <input type=\"hidden\" name=\"downloadFileName\" value=\"fus_bio4_1843\" />" +
          "        <input type=\"hidden\" name=\"direct\" value=\"true\" />" +
          "<input type='hidden' name='include' value='cit'/>" +
          "        <input id=\"submit\" type='submit' name='submit' value='Download article metadata' onclick=\"onCitMgrSubmit()\"/></form>" +
          "</div>" +
          "</body>" +
          "</html>";;
      

  @Override
  public void setUp() throws Exception {
      super.setUp();
      m_mau = new MockArchivalUnit();
      m_callback = new MyLinkExtractorCallback();
      fact = new FutureScienceLinkExtractorFactory();
      m_extractor = fact.createLinkExtractor("html");
 
    }
  Set<String> expectedUrls;
      
  public void testCitationsForm() throws Exception {
    expectedUrls = SetUtil.set(
        FS_BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=ris&include=cit",
        FS_BASE_URL + "doi/abs/11.1111/TEST");
    String norm_url;
    Set<String> result_strings = parseSingleSource(citationForm);
    Set<String> norm_urls = new HashSet<String>();
    
    for (String url : result_strings) {
      norm_url = normalizer.normalizeUrl(url, m_mau);
      log.info("normalized form URL: " + norm_url);
      norm_urls.add(norm_url);
    }
    assertEquals(expectedUrls,norm_urls);
  }
  
  private Set<String> parseSingleSource(String source)
      throws Exception {
    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new RegexpCssLinkExtractor();
    m_mau.setLinkExtractor("text/css", ue);
    MockCachedUrl mcu =
      new org.lockss.test.MockCachedUrl("http://future-science.com/", m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor.extractUrls(m_mau,
                            new org.lockss.test.StringInputStream(source), ENC,
                            FS_BASE_URL, m_callback);
    return m_callback.getFoundUrls();
  }
  
  private static class MyLinkExtractorCallback implements
  LinkExtractor.Callback {

    Set<String> foundUrls = new java.util.HashSet<String>();

    public void foundLink(String url) {
      foundUrls.add(url);
    }

    public Set<String> getFoundUrls() {
      return foundUrls;
    }

    public void reset() {
      foundUrls = new java.util.HashSet<String>();
    }
}
  

}
