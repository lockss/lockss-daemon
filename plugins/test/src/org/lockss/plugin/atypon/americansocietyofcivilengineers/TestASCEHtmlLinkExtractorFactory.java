/*
 * $Id$
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

package org.lockss.plugin.atypon.americansocietyofcivilengineers;

import java.util.HashSet;
import java.util.Set;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponHtmlLinkExtractorFactory;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.RegexpCssLinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;


public class TestASCEHtmlLinkExtractorFactory extends LockssTestCase {
  UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();

  private ASCEHtmlLinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String ASCE_BASE_URL = "http://ascelibrary.org/";
  private static final String DOI_START = "10.1061";
  private static final String DOI_END = "%28ASCE%291076-0431%282009%2915%3A1%2814%29";
  
  private static final String citationForm=
      "<html><head><title>Test Title</title></head><body>" +
          "<div>" +
          "  <form action=\"/action/downloadCitation\" name=\"frmCitmgr\" method=\"post\" target=\"_self\">" +
          "<input type=\"hidden\" name=\"doi\" value=\"10.1061/(ASCE)1076-0431(2009)15:1(14)\" />" +
          "  <input type=\"hidden\" name=\"downloadFileName\" value=\"asce_jaeied15_14\" />" +
          "  <input type='hidden' name='include' value='cit' />" +
          "  <table summary=\"\">" +
          "  <tr class=\"formats\">" +
          "  <th>Format</th>" +
          "    <td>" +
          "    <input type=\"radio\" name=\"format\" value=\"ris\" id=\"ris\" onclick=\"toggleImport(this);\" checked>" +
          "    <label for=\"ris\">RIS (ProCite, Reference Manager)</label>" +
          "    <br />" +
          "    <input type=\"radio\" name=\"format\" value=\"endnote\" id=\"endnote\" onclick=\"toggleImport(this);\">" +
          "    <label for=\"endnote\">EndNote</label>" +
          "    <br />" +
          "    <input type=\"radio\" name=\"format\" value=\"bibtex\" id=\"bibtex\" onclick=\"toggleImport(this);\" />" +
          "    <label for=\"bibtex\">BibTex</label>" +
          "    <br />" +
          "    <input type=\"radio\" name=\"format\" value=\"medlars\" id=\"medlars\" onclick=\"toggleImport(this);\"/>" +
          "    <label for=\"medlars\">Medlars</label>" +
          "    <br />" +
          "    <input type=\"radio\" name=\"format\" value=\"refworks\" id=\"refworks\" onclick=\"toggleimport(this);\">" +
          "    <label for=\"refworks\">RefWorks</label>" +
          "    <br />" +
          "    <input type=\"radio\" name=\"format\" value=\"refworks-cn\" id=\"refworks-cn\" onclick=\"toggleimport(this);\">" +
          "    <label for=\"refworks-cn\">RefWorks (China)</label>" +
          "    </td>" +
          "    </tr>" +
          "   <tr class=\"directImport\">" +
          "   <th><label for=\"direct\">Direct import</label></th>" +
          "   <td><input type='checkbox' name='direct' id='direct' checked=\"checked\" /></td>" +
          "   </tr>" +
          "   <tr><td class=\"helpLink\" colspan=\"2\">" +
          "   <a href=\"/help?context=citationManager\" onclick=\"popupHelp(this.href); return false;\" class=\"link\" title=\"Opens new window\" target=\"_blank\">Tips on citation download</a>" +
          "  </td></tr>" +
          "  <tr><td class=\"submit\" colspan='2'>" +
          "  <input type='submit' name='submit' value='Download citation data' onclick=\"onCitMgrSubmit()\" class=\"formbutton\"/>" +
          "  </td></tr>" +
          "  </table></form>" +
          "</div>" +
          "</body>" +
          "</html>";
      

  @Override
  public void setUp() throws Exception {
      super.setUp();
      m_mau = new MockArchivalUnit();
      m_callback = new MyLinkExtractorCallback();
      fact = new ASCEHtmlLinkExtractorFactory();
      m_extractor = fact.createLinkExtractor("html");
 
    }
  Set<String> expectedUrls;
      
  public void testCitationsForm() throws Exception {
    // in real life, the crawl rules would exclude the last of these 
    expectedUrls = SetUtil.set(
        ASCE_BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=ris&include=cit",
        /*
        ASCE_BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=bibtex&include=cit",
        ASCE_BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=endnote&include=cit",
        ASCE_BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=medlars&include=cit",*/
        ASCE_BASE_URL + "help?context=citationManager");      
    String norm_url;
    Set<String> result_strings = parseSingleSource(citationForm);
    Set<String> norm_urls = new HashSet<String>();
    
    // if the additional SIAM restriction didn't work, you would end up with 13 URLS due to duplication 
    // baseatypon now restricts the refworks and refworks-cn, so that's two less than it otherwise would have been
    // and now baseatypon excludes all formats except for ris
    assertEquals(2, result_strings.size());

    for (String url : result_strings) {
      norm_url = normalizer.normalizeUrl(url, m_mau);
      //log.info("normalized citation form URL: " + norm_url);
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
        new org.lockss.test.MockCachedUrl("http://epubs.siam.org/", m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor.extractUrls(m_mau,
        new org.lockss.test.StringInputStream(source), ENC,
        ASCE_BASE_URL, m_callback);
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
