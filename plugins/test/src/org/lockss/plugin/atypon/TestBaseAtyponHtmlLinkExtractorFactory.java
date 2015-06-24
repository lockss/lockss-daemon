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

package org.lockss.plugin.atypon;

import java.util.HashSet;
import java.util.Set;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.atypon.BaseAtyponUrlNormalizer;
import org.lockss.extractor.JsoupHtmlLinkExtractor;
import org.lockss.extractor.LinkExtractor;
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
    //log.setLevel("debug3");
    super.setUp();
    m_mau = new MockArchivalUnit();
    m_callback = new MyLinkExtractorCallback();

    fact = new BaseAtyponHtmlLinkExtractorFactory();
    m_extractor = fact.createLinkExtractor("html");

  }
  
  Set<String> expectedUrls;
  
  /*-----------------TESTING THE CITATION DOWNLOAD LINK EXTRACTOR -------------- */

  /* Since this sample form comes from SIAM this is very similar to the TestSiamHtmLinkExtractor, but 
   * since this is the less restricted BaseAtyponHtnlLinkExtractor there are more expected URLs so it's
   * not exactly the same test.
   * 
   * Newly implmemented - we ONLY want to collect urls that are format=ris&include=cit. 
   * exclude all others
   */

  public void testCitationsForm() throws Exception {
    UrlNormalizer normalizer = new BaseAtyponUrlNormalizer();
    expectedUrls = SetUtil.set(
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=ris&include=cit",
/*        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=bibtex&include=cit",
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks&include=cit",
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks-cn&include=cit",
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=endnote&include=cit",
        BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=medlars&include=cit",     
*/     
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=ris&include=cit"
/*        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=bibtex&include=cit",
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks&include=cit",
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks-cn&include=cit",
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=endnote&include=cit",
        BASE_URL + "action/downloadCitation?direct=on&doi=" + DOI_START + "%2F" + DOI_END + "&format=medlars&include=cit"
*/        
        );      


    String norm_url;
    Set<String> result_strings = parseSingleSource(citationForm, "abs", null);
    Set<String> norm_urls = new HashSet<String>();
    final String refworks_url = BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks&include=cit";
    final String refworks_cn_url = BASE_URL + "action/downloadCitation?doi=" + DOI_START + "%2F" + DOI_END + "&format=refworks-cn&include=cit";

    assertEquals(2, result_strings.size());
    for (String url : result_strings) {
      norm_url = normalizer.normalizeUrl(url, m_mau);
      log.debug3("normalized citation form URL: " + norm_url);
      assertTrue(expectedUrls.contains(norm_url));
      norm_urls.add(norm_url);
    }
    // these were all excluded by BaseAtypon
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

    Set<String> result_strings = parseSingleSource(abstractWithForm, "abs", null);
    for (String url : result_strings) {
      log.debug3("abstract form URL: " + url);
    }
    // this form will produce 1 URL even though it doesn't match the form name requirement "frmCitmgr"
    // because the "submit" button is an image and we need images to go through so pages look correct
    assertEquals(1, result_strings.size());
  }
  
  /*------TESTING THE popRef and popRefFull javascript extractors-----------*/
  
  private static final String segRefFull_input=
      "<a href=\"javascript:popRefFull('fig1')\" class=\"ref\">" +
      "<img border=\"1\" align=\"bottom\" id=\"fig1image\" alt=\"\" " +
      "src=\"/imagesource/home/xxx/yyy/zzz/journals/content/jnamex" +
      "/2013/gabc.2013.99.issue-99/gabc2013-0099.9/20139999/images/small" +
      "/figure1.gif\">" +
      "<br><strong>View larger image </strong>(64K)<br><br></a>";
    private static final String seg_doi =  "99.9999/gabc2013-0099.9";
    public void testSEGImages() throws Exception {
      Set<String> result_strings = parseSingleSource(segRefFull_input, "full", seg_doi);
      for (String url : result_strings) {
        log.debug3("segImages URL: " + url);
      }
      // get two URLS - the generated showPopupFull url & the imagesource url
      assertEquals(2, result_strings.size());
      assertTrue(result_strings.contains(BASE_URL + "action/showFullPopup" +
          "?id=fig1&doi=99.9999%2Fgabc2013-0099.9")); 
      assertTrue(result_strings.contains(BASE_URL + "imagesource/home/xxx/yyy/zzz/journals/content/jnamex/2013/" + 
          "gabc.2013.99.issue-99/gabc2013-0099.9/20139999/images/small/figure1.gif"));
    }
    
    private static final String amsci_popref =
          "<tbody><tr bgcolor=\"foo\">" +
          "<td align=\"center\" bgcolor=\"foo\" valign=\"top\">" +
          "<a class=\"ref\" href=\"javascript:popRef('F1')\">" +
          "<img src=\"/na101/home/literatum/publisher/ammons/journals/" +
          "content/it/2014/it.2014.3.issue-1/05.08.it.3.3/20140321/images/small/05_08_it_3_3_f1.gif\" " +
          "alt=\"\" id=\"F1image\" align=\"bottom\" border=\"1\"><br>" +
          "<strong>View larger version</strong>(17K)<br><br></a></td>" +
          "</td></tr></tbody>";
    private static final String amsci_doi = "10.1111/05.08.IT.3.3";
    public void testamsciImages() throws Exception {

      Set<String> result_strings = parseSingleSource(amsci_popref, "full", amsci_doi);
      for (String url : result_strings) {
        log.debug3("amsciImages URL: " + url);
      }
      // get two URLS - the generated showPopupFull url & the imagesource url
      assertEquals(2, result_strings.size());
      assertTrue(result_strings.contains(BASE_URL + "action/showPopup?citid=citart1&" +
          "id=F1&doi=10.1111%2F05.08.IT.3.3")); 
      assertTrue(result_strings.contains(BASE_URL + "na101/home/literatum/publisher/ammons/journals/" + 
          "content/it/2014/it.2014.3.issue-1/05.08.it.3.3/20140321/images/small/05_08_it_3_3_f1.gif"));
    }
  
  /*-------TESTING THE window.FigureViewer link extractor -----------*/
  /* currently only implemented under Maney. Requires 1.67 */
    private static final String SCRIPT_TAG = "script";
    
    /*
     * First the html for a limited test - just the image tag that needs expanding
     */
    private static final String test1_urlprefix = 
        "na101/home/literatum/publisher/maney/journals/" +                                                                 
        "content/amb/2013/amb.2013.60.issue-3/0002698013z.00000000033/production" +                                                                    
        "images/";

    private static final String test1_figureLinksHtml=
        "<html><head><title>Test Title</title></head><body>" +
            "<div class=\"holder\">" +
            "<a title=\"Open Figure Viewer\" onclick=\"showFigures(this,event); return false;\" href=\"JavaScript:void(0);\" class=\"thumbnail\">" +
            "<img alt=\"figure\" " +
            "src=\"/" + test1_urlprefix + "/images/small/s3-g1.gif\">" +
            "</img>" +
            "</a>" +
            "<span class=\"overlay\"></span>" +
            "</div>" +
            "<div>" +
            "<" + SCRIPT_TAG + " type=\"text/javascript\">" +
            "  window.figureViewer={doi:\'10.1179/0002698013Z.00000000033\',\n" +
            "path:\'/" + test1_urlprefix + "\',figures:[{i:\'S3F1\',g:[{m:\'s3-g1.gif\',l:\'s3-g1.jpeg\',size:\'116 kB\'}]}" +
            "   ,{i:\'S3F2\',g:[{m:\'s3-g2.gif\',l:\'s3-g2.jpeg\',size:\'70 kB\'}]}" +
            "   ,{i:\'S3F3\',g:[{m:\'s3-g3.gif\',l:\'s3-g3.jpeg\',size:\'61 kB\'}]}" +
            "   ,{i:\'S3F4\',g:[{m:\'s3-g4.gif\',l:\'s3-g4.jpeg\',size:\'37 kB\'}]}" +
            "   ,{i:\'S3F5\',g:[{m:\'s3-g5.gif\',l:\'s3-g5.jpeg\',size:\'28 kB\'}]}" +
            "   ]}</" + SCRIPT_TAG + ">" +
            "</div>" +
            
            "</body>" +
            "</html>";
    private static final String test2_urlprefix = "na101/home/literatum/publisher/maney/journals/content/amb/2013/" +
            "amb.2013.60.issue-4/0002698013z.00000000038/20131129";
    private static final String test2_figureLinksHtml = 
    "<html><head><title>Test Title</title></head><body>" +
    "<div class=\"holder\">" +
    "<a title=\"Open Figure Viewer\" onclick=\"showFigures(this,event); return false;\" href=\"JavaScript:void(0);\" class=\"thumbnail\">" +
    "<img alt=\"figure\" src=\"/" + test2_urlprefix + "/images/small/0002698013z.00000000038.01.gif\" />" +
    "</a>" +
    "<" + SCRIPT_TAG + " type=\"text/javascript\">" +
    "  window.figureViewer={doi:\'10.1179/0002698013Z.00000000038\'," +
    "path:\'/" + test2_urlprefix + "\'," +
    "figures:[{i:\'F1\',g:[{m:\'0002698013z.00000000038.01.gif\',l:" +
    "\'0002698013z.00000000038.01.jpeg\',size:\'102 KB\'}]}" +
    " ]}</" + SCRIPT_TAG + ">" +
    "</div>" +
            "</body>" +
            "</html>";
    /*
     * Now more complete html code to test that other links still work as expected
     * 
     */
    private static final String fullHtml =
    "<!DOCTYPE html>" +
    "<html lang=\"en\" class=\"pb-page\">" +
    "<head data-pb-dropzone=\"head\">" +
    "<title>TEST FOO</title>" +
    "<script type=\"text/javascript\" src=\"/wro/product.js\"></script>" +
    "<link rel=\"stylesheet\" type=\"text/css\" href=\"/wro/product.css\">" +
    "</head>" +
    "<body>" +
    " <div id=\"pb-page-content\">" +
    " <div data-pb-dropzone=\"main\" data-pb-dropzone-name=\"Main\">" +
    " <section class=\"widget general-image none  widget-none  widget-compact\" id=\"ec8b7042-594c-48ae-b50a-619a7c70b9b6\">" +
    " <div class=\"wrapped 1_12\" >" +
    " <div class=\"widget-body body body-none  body-compact\"><a href=\"/\">" +
    " <img src=\"/pb/assets/raw/Maney_logo.jpg\"/>" +
    " </a></div>" +
    "  </div>" +
    "  </section>" +
    "</div></div></body></html>";

    
    private static final String test3_figureLinksWithSpacesAndNewlines=
        "<html><head><title>Test Title</title></head><body>" +
            "<div class=\"holder\">" +
            "<a title=\"Open Figure Viewer\" onclick=\"showFigures(this,event); return false;\" href=\"JavaScript:void(0);\" class=\"thumbnail\">" +
            "<img alt=\"figure\" " +
            "src=\"/" + test1_urlprefix + "/images/small/s3-g1.gif\">" +
            "</img>" +
            "</a>" +
            "<span class=\"overlay\"></span>" +
            "</div>" +
            "<div>" +
            "<" + SCRIPT_TAG + " type=\"text/javascript\">" +
            "    window.figureViewer={  doi  : \'10.1179/0002698013Z.00000000033\'  ,  \n  " +
            " path : \'/" + test1_urlprefix + "\' ,  figures : [ { i : \'S3F1\' , \n " +
                          " g : [ { m : \'s3-g1.gif\' , l : \'s3-g1.jpeg\',size:\'116 kB\' } ] }\n" +
            "\n   ] } </" + SCRIPT_TAG + ">" +
            "</div>" +
            "\n" +          
            "</body>" +
            "</html>";
    
    private static final String test_noLinks=

        "<html><head><title>Test Title</title></head><body>" +
    "<div class=\"holder\">" +
    "<a title=\"Open Figure Viewer\" onclick=\"showFigures(this,event); return false;\" href=\"JavaScript:void(0);\" class=\"thumbnail\">" +
    "</a>" +
    "<a title=\"Open Figure Viewer\" onclick=\"showFigures(this,event); return false;\" class=\"foo\">" +
    "</a>" +
    "<a fooarg=\"fooval\"></a>" +
    "</div></body></html>";
    
    private static final String test_nrc_links=
        "<html><head><title>Test Title</title></head><body>" +
            "<a href=\"/journal/gen\">" +
            " <img class=\"pubCoverImg\" src=\"/na101/home/literatum/publisher/nrc/journals/covergifs/gen/cover.jpg\" alt=\"Genome\" />" +
            "</a>" +
            "<a class=\"ref aff\" href=\"javascript:popRef('aff1')\"><sup><i>a</i></sup></a>" +
            "<a class=\"openLayerForItem\" itemid=\"tabX\" href=\"javascript:void(0);\">Table 1</a>" +
            "<a class=\"ref openTablesLayer\" href=\"javascript:void(0);\" id=\"tabY\" doi=\"10.1139/g2012-037\">" +
            "<img src=\"/templates/jsp/_style2/_nrc/images/dummy_table_thumb.gif\" width=\"150\" height=\"100\" " +
            "align=\"bottom\" border=\"1\" alt=\"Data table\" /><p class=\"red-link-left\">&raquo;View table</p></a>" +
            "<a name=\"f1\"><!--FIG--></a>" +
            "<a doi=\"10.1111/test.doi\" id=\"f2\" class=\"red-link-left openFigLayer\"> " +
            "<img src=\"/na101/home/literatum/publisher/nrc/journals/content/gen/2012/gen.2012.5507/g2012-037/production/images/small/g2012-037f1.gif\" " +
            "align=\"bottom\" border=\"1\" alt=\"\" /><p>&raquo;View larger version</p></a>" +
            "</div></body></html>";

    private static final String test_popRefVariants=
        "<html><head><title>Test Title</title></head><body>" +
            "<a class=\"FOO\" href=\"javascript:popRef('foo1')\"><sup><i>a</i></sup></a>" +
            "<a class=\"FOO\" href=\"javascript:popRef2('foo2')\"><sup><i>a</i></sup></a>" +
            "<a class=\"FOO\" href=\"javascript:popRefFull('foo3')\"><sup><i>a</i></sup></a>" +
                "</div></body></html>";


    public void testNoLinks() throws Exception {
      Set<String> result_strings = parseSingleSource(test_noLinks, "full", null);

      assertEquals(0, result_strings.size());
      
    }
 
    public void test1FigureLinks() throws Exception {
       // the snippet of html used to set this up only establishes the small
       // size for the first image. So it is correct that the other images, which
       // come form the figureViewer only include medium and large
      expectedUrls = SetUtil.set(
          BASE_URL + test1_urlprefix + "/images/small/s3-g1.gif",
          BASE_URL + test1_urlprefix + "/images/medium/s3-g1.gif",
          BASE_URL + test1_urlprefix + "/images/large/s3-g1.jpeg",
          BASE_URL + test1_urlprefix + "/images/medium/s3-g2.gif",
          BASE_URL + test1_urlprefix + "/images/large/s3-g2.jpeg",
          BASE_URL + test1_urlprefix + "/images/medium/s3-g3.gif",
          BASE_URL + test1_urlprefix + "/images/large/s3-g3.jpeg",
          BASE_URL + test1_urlprefix + "/images/medium/s3-g4.gif",
          BASE_URL + test1_urlprefix + "/images/large/s3-g4.jpeg",
          BASE_URL + test1_urlprefix + "/images/medium/s3-g5.gif",
          BASE_URL + test1_urlprefix + "/images/large/s3-g5.jpeg");

      Set<String> result_strings = parseSingleSource(test1_figureLinksHtml, "full", null);

      assertEquals(11, result_strings.size());
      
      for (String url : result_strings) {
        log.debug3("URL: " + url);
        assertTrue(expectedUrls.contains(url));
      }
      
      // Now try it not on a "full" page, should just extract the one listed lnk 
      result_strings = parseSingleSource(test1_figureLinksHtml, "abs", null);

      assertEquals(1, result_strings.size());
      for (String url : result_strings) {
        log.debug("URL: " + url);
        assertEquals(BASE_URL + test1_urlprefix + "/images/small/s3-g1.gif", url);
      }
      
    }
    
    public void test2FigureLinks() throws Exception {

      expectedUrls = SetUtil.set(
          BASE_URL + test2_urlprefix + "/images/small/0002698013z.00000000038.01.gif",
          BASE_URL + test2_urlprefix + "/images/medium/0002698013z.00000000038.01.gif",
          BASE_URL + test2_urlprefix + "/images/large/0002698013z.00000000038.01.jpeg");

      Set<String> result_strings = parseSingleSource(test2_figureLinksHtml, "full", null);

      assertEquals(3, result_strings.size());
      
      for (String url : result_strings) {
        log.debug3("URL: " + url);
        assertTrue(expectedUrls.contains(url));
      }
    }
    
    public void testNRCLinks() throws Exception {

      expectedUrls = SetUtil.set(
          // not yet doing the open "openLayerForItem\" 
          //BASE_URL + "/action/showFullPopup?id=tabX&doi=",
          // from the href= + image
          BASE_URL + "na101/home/literatum/publisher/nrc/journals/covergifs/gen/cover.jpg",
          BASE_URL + "journal/gen",
          // from the popRef
          BASE_URL + "action/showPopup?citid=citart1&id=aff1&doi=11.1111%2FTEST",
          //from the openTablesLayer + img
          BASE_URL + "action/showFullPopup?id=tabY&doi=10.1139%2Fg2012-037",          
          BASE_URL + "templates/jsp/_style2/_nrc/images/dummy_table_thumb.gif",
          // from the openFigLayer link + img 
          BASE_URL + "action/showFullPopup?id=f2&doi=10.1111%2Ftest.doi",
          BASE_URL + "na101/home/literatum/publisher/nrc/journals/content/gen/2012/gen.2012.5507/g2012-037/production/images/small/g2012-037f1.gif");
      
      Set<String> result_strings = parseSingleSource(test_nrc_links, "full", null);

      log.debug3("in testNRCLinks");
      if (log.isDebug3()) {
        for (String url : result_strings) {
          log.debug3("URL: " + url);
        }
      }
      
      assertEquals(7, result_strings.size());
      // loop over the expected URLs and make sure each is in the result
      for (String url : expectedUrls) {
        log.debug3("expectedURL: " + url);
        assertTrue(result_strings.contains(url));
      }

      
    }

    public void testPopRefVariants() throws Exception {

      expectedUrls = SetUtil.set(
          // from the popRef
          BASE_URL + "action/showPopup?citid=citart1&id=foo1&doi=11.1111%2FTEST",
          //from the popRef2
          BASE_URL + "action/showPopup?citid=citart1&id=foo2&doi=11.1111%2FTEST",
          // from the popRefFull
          BASE_URL + "action/showFullPopup?id=foo3&doi=11.1111%2FTEST");
      
      Set<String> result_strings = parseSingleSource(test_popRefVariants, "full", null);

      log.debug3("in testPopRefVariants");
      if (log.isDebug3()) {
        for (String url : result_strings) {
          log.debug3("URL: " + url);
        }
      }
      
      assertEquals(3, result_strings.size());
      // loop over the expected URLs and make sure each is in the result
      for (String url : expectedUrls) {
        log.debug3("expectedURL: " + url);
        assertTrue(result_strings.contains(url));
      }

      
    }
    
     //This test makes sure other base link extraction continues to work
     public void testfullHtml() throws Exception {
   
      Set<String> result_strings = parseSingleSource(fullHtml, "full", null);
      expectedUrls = SetUtil.set(
      BASE_URL + "pb/assets/raw/Maney_logo.jpg",
      BASE_URL + "wro/product.js",
      BASE_URL + "wro/product.css",
      BASE_URL);

      assertEquals(4, result_strings.size());
      for (String url : result_strings) {
        log.debug("URL: " + url);
        assertTrue(expectedUrls.contains(url));
      }
    }
    
     public void test3FigureLinks() throws Exception {
        //Testing to make sure that the image extracgtor can handle spaces
        // and newlines within the regexp section
       expectedUrls = SetUtil.set(
           BASE_URL + test1_urlprefix + "/images/small/s3-g1.gif",
           BASE_URL + test1_urlprefix + "/images/medium/s3-g1.gif",
           BASE_URL + test1_urlprefix + "/images/large/s3-g1.jpeg");

       Set<String> result_strings = parseSingleSource(test3_figureLinksWithSpacesAndNewlines, "full", null);

       assertEquals(3, result_strings.size());
       
       for (String url : result_strings) {
         log.debug3("URL: " + url);
         assertTrue(expectedUrls.contains(url));
       }
       
     }
    
    // this is copied directory from the Jsoup test to make sure that our class extension
    // hasn't broken fallback behavior
    public void testResolvesHtmlEntities() throws Exception {
      String url1 = "http://www.example.com/bioone/?"
          + "request=get-toc&issn=0044-7447&volume=32&issue=1";

      String source = "<html><head><title>Test</title></head><body>"
          + "<a href=http://www.example.com/bioone/?"
          + "request=get-toc&#38;issn=0044-7447&#38;volume=32&issue=1>link1</a>";
      assertEquals(SetUtil.set(url1), parseSingleSource(source, "full", null));

      // ensure character entities processed before rel url resolution
      source = "<html><head><title>Test</title></head><body>"
          + "<base href=http://www.example.com/foo/bar>"
          + "<a href=&#46&#46/xxx>link1</a>";
      assertEquals(SetUtil.set("http://www.example.com/xxx"),
          parseSingleSource(source, "full", null));
    }

  
  /*------------------SUPPORT FUNCTIONS --------------------- */

  // all setting of page type (full, abs) to test restrictions
  // if doi argument is null, use default; otherwise use given
  private Set<String> parseSingleSource(String source, String page_type, String page_doi)
      throws Exception {
    String srcUrl;
    if (page_doi == null) {
      srcUrl = BASE_URL + "doi/" + page_type + "/" + DOI_START + "/" + DOI_END;
    } else {
      srcUrl = BASE_URL + "doi/" + page_type + "/" + page_doi;
    }
    MockArchivalUnit m_mau = new MockArchivalUnit();
    LinkExtractor ue = new JsoupHtmlLinkExtractor();
    m_mau.setLinkExtractor("html", ue);
    MockCachedUrl mcu =
        new org.lockss.test.MockCachedUrl(srcUrl, m_mau);
    mcu.setContent(source);

    m_callback.reset();
    m_extractor.extractUrls(m_mau,
        new org.lockss.test.StringInputStream(source), ENC,
        srcUrl, m_callback);
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
