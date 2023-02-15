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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.RegexpCssLinkExtractor;
import org.lockss.extractor.LinkExtractor.Callback;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.test.*;

public class TestTafHtmlLinkExtractorFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  TafHtmlLinkExtractorFactory fact;
  MockArchivalUnit m_mau;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;


  public void setUp() throws Exception {
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
    fact = new TafHtmlLinkExtractorFactory();
    // extractor will get set for the specific test 
  }
  
  /** Test case input */
  static final String testOnClickLinkInput = 
      "<p onclick=\"window.open('leaf.htm','MyWindow',"
    +     "'toolbar=no,status=no,menubar=no,width=795,height=520,"
    +     "scrollbars=yes,top=0,left=0')\">\n"
    + "  http://www.xyz.com/path/leaf.htm"
    + "</p>";
  

  /** Test case extracted link */
  static final String expectedOnClickLinkPath = 
      "http://www.xyz.com/path/leaf.htm";

  
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

  /**
   * Test extracting a link from the first argument of the window.open()
   * call in an "onClick" attribute on a paragraph tag.
   *  
   * @throws Exception
   */
  public void testJavaScriptFunctionLinkExtractingPath() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = 
        new ByteArrayInputStream(testOnClickLinkInput.getBytes());
    LinkExtractor ext  = 
        fact.createLinkExtractor("text/html");
    final List<String> foundLink = new ArrayList<String>();
    ext.extractUrls(mockAu, in, "UTF-8", 
                    "http://www.xyz.com/path/path1", new Callback() {
      public void foundLink(String url) {
        foundLink.add(url);
      }
    });
    assertEquals(Collections.singletonList(expectedOnClickLinkPath), foundLink);
  }
  
  /*
   * This next section of text mimics the BaseAtypon test fairly closely.  But we got bitten by a 
   * T&F specific link extractor causing loss of BaseAtypon (parent) link extractor functionality
   * so this test will ensure that the T&F custom link extractor still does the other stuff it needs
   * to do that it would otherwise have inherited.
   */
  
  private static final String DOI_START = "11.1111";
  private static final String DOI_END = "TEST";
  
  private static final String downloadCitForm = 
      "<div>" +
          " <br />" +
          " <!-- download options -->" +
          " <form action=\"/action/downloadCitation\" name=\"frmCitmgr\" method=\"post\" target=\"_self\"><input type=\"hidden\" name=\"doi\" value=\"" +
          DOI_START + "/" + DOI_END + "\" />" +
          "    <input type=\"hidden\" name=\"downloadFileName\" value=\"siam_siread52_1\" />" +
          "    <input type='hidden' name='include' value='cit' />" +
          "    <table summary=\"\">" +
          "    <tr class=\"formats\"><th>Format</th>" +
          "     <td>" +
          "      <input type=\"radio\" name=\"format\" value=\"ris\" id=\"ris\" onclick=\"toggleImport(this);\" checked>" +
          "      <label for=\"ris\">RIS (ProCite, Reference Manager)</label><br />" +
          "      <input type=\"radio\" name=\"format\" value=\"endnote\" id=\"endnote\" onclick=\"toggleImport(this);\">" +
          "      <label for=\"endnote\">EndNote</label><br />" +
          "      <input type=\"radio\" name=\"format\" value=\"bibtex\" id=\"bibtex\" onclick=\"toggleImport(this);\" />" +
          "      <label for=\"bibtex\">BibTex</label><br />" +
          "      <input type=\"radio\" name=\"format\" value=\"medlars\" id=\"medlars\" onclick=\"toggleImport(this);\"/>" +
          "      <label for=\"medlars\">Medlars</label><br />" +
          "      <input type=\"radio\" name=\"format\" value=\"refworks\" id=\"refworks\" onclick=\"toggleimport(this);\">" +
          "      <label for=\"refworks\">RefWorks</label><br />" +
          "      <input type=\"radio\" name=\"format\" value=\"refworks-cn\" id=\"refworks-cn\" onclick=\"toggleimport(this);\">" +
          "      <label for=\"refworks-cn\">RefWorks (China)</label>" +
          "      </td>" +
          "     </tr>" +
          "     <tr class=\"directImport\"><th><label for=\"direct\">Direct import</label></th>" +
          "       <td><input type='checkbox' name='direct' id='direct' checked=\"checked\" /></td>" +
          "      </tr>" +
          "      <tr>" +
          "        <td class=\"submit\" colspan='2'>" +
          "          <input type='submit' name='submit' value='Download publication citation data' onclick=\"onCitMgrSubmit()\" class=\"formbutton\"/>" +
          "        </td>" +
          "       </tr>" +
          "       </table>" +
          "  </form>" +
          "</div>";
  
  private static final String citationForm=
      "<html><head><title>Test Title</title></head><body>" + downloadCitForm + "</body>\n</html>";
  
  private final String BASE_URL = "http://BaseAtypon.org/";
  Set<String> expectedUrls;
  public void testCitationsForm() throws Exception {
    UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();
    expectedUrls = SetUtil.set(
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=ris&include=cit",
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=bibtex&include=cit",
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks&include=cit",
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks-cn&include=cit",
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=endnote&include=cit",
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=medlars&include=cit",     
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=ris&include=cit",
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=bibtex&include=cit",
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks&include=cit",
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks-cn&include=cit",
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=endnote&include=cit",
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=medlars&include=cit");      


    String norm_url;
    Set<String> result_strings = parseSingleSource(citationForm);
    Set<String> norm_urls = new HashSet<String>();
    final String refworks_url = BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks&include=cit";
    final String refworks_cn_url = BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks-cn&include=cit";

    for (String url : result_strings) {
      norm_url = normalizer.normalizeUrl(url, m_mau);
      log.debug3("normalized citation form URL: " + norm_url);
      assertTrue(expectedUrls.contains(norm_url));
      norm_urls.add(norm_url);
    }
    // these two were excluded by BaseAtypon
    assertFalse(norm_urls.contains(refworks_url)); 
    assertFalse(norm_urls.contains(refworks_cn_url));
  }
  
  private Set<String> parseSingleSource(String source)
      throws Exception {
    LinkExtractor ue = new RegexpCssLinkExtractor();
    m_mau.setLinkExtractor("text/css", ue);
    MockCachedUrl mcu =
        new org.lockss.test.MockCachedUrl("http://BaseAtypon.org/", m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor = fact.createLinkExtractor("html");

    m_extractor.extractUrls(m_mau,
        new org.lockss.test.StringInputStream(source), ENC,
        BASE_URL, m_callback);
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
