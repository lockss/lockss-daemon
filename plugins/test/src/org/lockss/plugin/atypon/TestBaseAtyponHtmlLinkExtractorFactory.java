/*
 * $Id: TestBaseAtyponHtmlLinkExtractorFactory.java,v 1.4 2014-08-20 23:01:39 alexandraohlson Exp $
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

package org.lockss.plugin.atypon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.extractor.HtmlFormExtractor;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
import org.lockss.extractor.RegexpCssLinkExtractor;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.util.Constants;
import org.lockss.util.SetUtil;


public class TestBaseAtyponHtmlLinkExtractorFactory extends LockssTestCase {

  private BaseAtyponHtmlLinkExtractorFactory fact;
  private LinkExtractor m_extractor;
  private MyLinkExtractorCallback m_callback;
  static String ENC = Constants.DEFAULT_ENCODING;
  private MockArchivalUnit m_mau;
  private final String BASE_URL = "http://BaseAtypon.org/";
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

  private static final String citSearchForm = 
      "      <div id=\"citation\">" +
          "  <form action=\"/action/quickLink\" id=\"citationSearchForm\" method=\"GET\"><div class=\"yui3-g paddedgrid\">" +
          "<div class=\"yui3-u-1-2\">" +
          "<div class=\"inner\">" +
          "<input type=\"text\" name=\"quickLinkJournal\" placeholder=\"Journal\" class=\"filterJournals fill publicationAutocomplete\" autocomplete=\"off\" />" +
          "</div>" +
          "</div>" +
          "<div class=\"yui3-u-1-9 yui3-u\">" +
          "<div class=\"inner\">" +
          "<input type=\"text\" name=\"quickLinkVolume\" placeholder=\"Volume\" class=\"fill\" autocomplete=\"off\"/>" +
          "<input type=\"hidden\" name=\"quickLink\" value=\" \"/>" +
          "</div>" +
          "</div>" +
          "<div class=\"yui3-u-1-9 yui3-u\">" +
          "<div class=\"inner\">" +
          "<input type=\"text\" name=\"quickLinkIssue\" placeholder=\"Issue\" class=\"fill\" autocomplete=\"off\"/>" +
          "</div>" +
          "</div>" +
          "<div class=\"yui3-u-1-9 yui3-u\">" +
          "<div class=\"inner\">" +
          "<input type=\"text\" name=\"quickLinkPage\" placeholder=\"Page\" class=\"fill\" autocomplete=\"off\"/>" +
          "</div>" +
          "</div>" +
          "<div class=\"yui3-u-1-6\">" +
          "<div class=\"inner\">" +
          "<input type=\"submit\" alt=\"\" value=\"\" class=\"fill\" />" +
          "</div>" +
          "</div>" +
          "</div></form>" +
          "</div>";

  private static final String citationForm=
      "<html><head><title>Test Title</title></head><body>" + downloadCitForm + "</body>\n</html>";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();
    // CREATE A TESTING ADEQUATE VERSION of the link extractor
    m_extractor = createTestingLinkExtractor("html");    
    
    //fact = new BaseAtyponHtmlLinkExtractorFactory();
    //m_extractor = fact.createLinkExtractor("html");

  }
  // Copied pretty much from BaseAtyponHtmlLInkExtractorFactory
  private org.lockss.extractor.LinkExtractor createTestingLinkExtractor(String mimeType) {
      Map<String, HtmlFormExtractor.FormFieldRestrictions> baseRestrictor
      = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();

      baseRestrictor = setUpBaseRestrictor();
       Map<String, HtmlFormExtractor.FieldIterator> generators;
      generators = new HashMap<String, HtmlFormExtractor.FieldIterator>();

      // set up the link extractor using the TESTING constructor
      //JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor();
      JsoupHtmlLinkExtractor extractor = new JsoupHtmlLinkExtractor(false, true,
          baseRestrictor, generators);
     extractor.setFormRestrictors(baseRestrictor); 
      return extractor;

  }
  
  //copied directly from BaseAtyponHtmlLinkExtractor
  private Map<String, HtmlFormExtractor.FormFieldRestrictions> setUpBaseRestrictor() {
    Set<String> include = new HashSet<String>();
    Set<String> exclude = new HashSet<String>();
    Map<String, HtmlFormExtractor.FormFieldRestrictions> restrictor
    = new HashMap<String, HtmlFormExtractor.FormFieldRestrictions>();

    /* only include forms with the name "frmCitMgr" */
    include = SetUtil.fromCSV("frmCitmgr");
    HtmlFormExtractor.FormFieldRestrictions include_restrictions = new HtmlFormExtractor.FormFieldRestrictions(include,null);
    restrictor.put(HtmlFormExtractor.FORM_NAME, include_restrictions);
    
    /* now set up an exclude restriction on "format" */ 
    exclude = SetUtil.fromCSV("refworks,refworks-cn"); 
    HtmlFormExtractor.FormFieldRestrictions exclude_restrictions = new HtmlFormExtractor.FormFieldRestrictions(null, exclude);
    restrictor.put("format", exclude_restrictions);
    
    return restrictor;
  }
  
  
  
  Set<String> expectedUrls;

  /* Since this sample form comes from SIAM this is very similar to the TestSiamHtmLinkExtractor, but 
   * since this is the less restricted BaseAtyponHtnlLinkExtractor there are more expected URLs so it's
   * not exactly the same test.
   */

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
  
  private static final String abstractFormGuts =
      " <table border=\"0\" cellpadding=\"2\" cellspacing=\"0\" width=\"100%\"><tr><td align=\"center\" class=\"section_head quickSearch_head\">" +
      "Quick Search</td></tr><tr><td class=\"quickSearch_content\"><form method=\"post\" action=\"\" " +
      "onSubmit=\"onAuthorSearchClick(this); return false;\" name=\"frmQuickSearch\"><input type=\"hidden\" name=\"type\" value=\"simple\"/><input type=\"hidden\" name=\"action\" " +
      "value=\"search\"/><input type=\"hidden\" name=\"nh\" value=\"10\"/><input type=\"hidden\" name=\"displaySummary\" value=\"false\"/>" +
      "<table width=\"100%\" border=\"0\" cellpadding=\"4\" cellspacing=\"0\" bgcolor=\"#FFFFFF\"><tr><td valign=\"top\" width=\"100%\">" +
      "<span class=\"black9pt\"><select name=\"dbname\" size=\"1\"><option value=\"fus\" selected=\"\">" +
      "Future Science</option><script type=\"text/javascript\">" +
      "                               genSideQuickSearch('8','medline','PubMed');" +
      "                       </script>  <script type=\"text/javascript\">" +
      "                               genSideQuickSearch('16','crossref','CrossRef'); " +
      "                       </script> </select> for </span></td></tr>" +
      "<!-- quicksearch authors --><tr><td valign=\"top\" width=\"100%\" class=\"pageTitle\">" +
      "Author:</td></tr><tr><td valign=\"top\" width=\"100%\" class=\"black9pt\">" +
      "<table border=\"0\" cellpadding=\"2\" cellspacing=\"1\" width=\"100%\"><tr><td valign=\"top\">" +
      "<input class=\"input_boxes\" value=\"Matyus, Peter\" name=\"author\" type=\"checkbox\"/></td><td>" +
      "<input type=\"HIDDEN\" name=\"checkboxNum\" value=\"1\"/> Peter   Matyus </td></tr>" +
      "</table></td></tr><!-- /quicksearch authors --><!-- quicksearch keywords --><!-- /quicksearch keywords --><tr>" +
      "<td valign=\"top\" width=\"100%\" class=\"black9pt\"><input type=\"hidden\" name=\"result\" value=\"true\"/>" +
      "<input type=\"hidden\" name=\"type\" value=\"simple\"/>" +
      "<span class=\"black9pt\"><input type=\"image\" border=\"0\" src=\"/templates/jsp/_midtier/_FFA/_fus/images/searchButton.gif\" " +
      "align=\"right\" alt=\"Search\"/></span></td></tr>" +
      " </table></form></td></tr>" +
      "</table>";

  private static final String abstractWithForm=
      "<html><head><title>Test Title</title></head><body>" + abstractFormGuts + "</body>\n</html>";

  // Using this form nothing should get picked up because the FORM_NAME won't match the include restrictor
  public void testOtherForm() throws Exception {

    Set<String> result_strings = parseSingleSource(abstractWithForm);
    for (String url : result_strings) {
      log.debug3("abstract form URL: " + url);
    }
    // this form will produce 1 URL even though it doesn't match the form name requirement "frmCitmgr"
    // because the "submit" button is an image and we need images to go through so pages look correct
    assertEquals(1, result_strings.size());
  }

  
  private Set<String> parseSingleSource(String source)
      throws Exception {
    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new JsoupHtmlLinkExtractor();
    m_mau.setLinkExtractor("html", ue);
    MockCachedUrl mcu =
        new org.lockss.test.MockCachedUrl("http://BaseAtypon.org/", m_mau);
    mcu.setContent(source);

    m_callback.reset();
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
