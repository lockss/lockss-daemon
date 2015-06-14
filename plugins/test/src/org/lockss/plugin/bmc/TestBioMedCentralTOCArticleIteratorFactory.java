/*
 * $Id:$
 */
/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bmc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestBioMedCentralTOCArticleIteratorFactory extends
ArticleIteratorTestCase {

  /**
   * Simulated AU to generate content
   */
  protected static Logger log = Logger.getLogger(TestBioMedCentralTOCArticleIteratorFactory.class);
  private static String PLUGIN_NAME = "org.lockss.plugin.bmc.ClockssBioMedCentralPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.jcheminf.com/";
  private final String JOURNAL_ISSN = "1751-0147";
  private final String VOLUME_NAME = "54";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
      JOURNAL_ISSN_KEY, JOURNAL_ISSN,
      VOLUME_NAME_KEY, VOLUME_NAME);



  /*
   *  *   http://www.wjso.com/content/12/1/193 (where volume is a number)
   *   http://bsb.eurasipjournals.com/content/2014/1/18 (where volume is a year)
   *   http://www.genomebiology.com/2013/14/11/314  (where volume is 14 but use year instead of "content")
   *   
   *   supplements articles:
   *   http://www.genomebiology.com/2008/9/S1/S3  (volume 9, year is in place of "content")
   *   http://breast-cancer-research.com/content/14/S1/O2
   *   http://www.actavetscand.com/content/50/S1/S4  (final identifier can have various letters, A2, O1, P3)
   *   
   *   and then the legacy weird ones that make this harder
   *   http://breast-cancer-research.com/content/14/4/R104
   *   http://genomebiology.com/2002/3/7/research/0032
   *           other words used to distinguish type of article - review, reports, comment
   */

  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
    //log.setLevel("debug3");
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  /*  http://www.genomebiology.com/content/14/8 (volume 14, issue 8)
   *  http://www.wjso.com/content/12/August/2014  (volume 12, August)
   *  http://www.actavetscand.com/content/52/November/2010 (volume 52)
   *  http://bsb.eurasipjournals.com/content/2014/September/2014 (volume 2014)    
   *  http://www.genomebiology.com/supplements/9/S2
   *  http://www.actavetscand.com/supplements/52/S1
   *  
   */
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertMatchesRE(pat, "http://www.jcheminf.com/content/54/1");
    assertMatchesRE(pat, "http://www.jcheminf.com/supplements/54/S1");
    assertMatchesRE(pat, "http://www.jcheminf.com/content/54/March/2014");
    assertNotMatchesRE(pat, "http://www.jcheminf.com/content/pdf/1471-2253-1-2.pdf");
    assertNotMatchesRE(pat, "http://www.jcheminf.com/content/54/1/6");
    assertNotMatchesRE(pat, "http://www.example.com/content/54");
  }


  private final String TOC_BASIC_URL = BASE_URL + "content/" + VOLUME_NAME + "/6";
  private final String TOC_MONTH_URL = BASE_URL + "content/" + VOLUME_NAME + "/March/2014";
  private final String TOC_SUPPLEMENT_URL = BASE_URL + "supplements/" + VOLUME_NAME + "/S1";

  // most article urls follow a set pattern like this one.
  private final String EXPECTED_ART_BASIC_1_PDF = BASE_URL + "content/pdf/" + JOURNAL_ISSN + 
      "-" + VOLUME_NAME + "-123.pdf";
  private final String EXPECTED_ART_BASIC_1_ABSTRACT = TOC_BASIC_URL + "/123/abstract";
  private final String EXPECTED_ART_BASIC_1_FULL = TOC_BASIC_URL + "/123";
  // pdf names can be arbitrary strings 
  private final String EXPECTED_ART_BASIC_2_PDF = BASE_URL + "content/pdf/" + "wrd" + VOLUME_NAME + "124.pdf";
  private final String EXPECTED_ART_BASIC_2_ABSTRACT = TOC_BASIC_URL + "/124/abstract";
  private final String EXPECTED_ART_BASIC_2_FULL = TOC_BASIC_URL + "/124";
  private final String EXPECTED_ART_BASIC_2_DATA = TOC_BASIC_URL + "/124/additional";

  // genomebiology uses year in place of "content" in the urls 
  private final String EXPECTED_ART_MONTH_1_PDF = BASE_URL + "content/pdf/foo-2014-54-11-314.pdf"; 
  private final String EXPECTED_ART_MONTH_1_ABSTRACT = BASE_URL + "2014/" + VOLUME_NAME + "/11/314/abstract"; 
  private final String EXPECTED_ART_MONTH_1_FULL = BASE_URL + "2014/" + VOLUME_NAME + "/11/314";

  // supplements and basic articles (letters, reviews) may not have abstract
  private final String EXPECTED_ART_SUPPLEMENT_1_PDF = BASE_URL + "content/pdf/" +
      JOURNAL_ISSN + "-" + VOLUME_NAME + "-S1-A4.pdf";
  private final String EXPECTED_ART_SUPPLEMENT_1_FULL = TOC_SUPPLEMENT_URL + "/A4";
  private final String EXPECTED_ART_SUPPLEMENT_2_PDF = BASE_URL + "content/pdf/fooS1P13.pdf";
  private final String EXPECTED_ART_SUPPLEMENT_2_FULL = TOC_SUPPLEMENT_URL + "/P13";


  // from the standard toc
  String [] expectedUrlsBasic1 = { EXPECTED_ART_BASIC_1_ABSTRACT,
      EXPECTED_ART_BASIC_1_ABSTRACT, //metadata
      EXPECTED_ART_BASIC_1_PDF, //full_text_cu
      EXPECTED_ART_BASIC_1_FULL, //full text hamlt
      EXPECTED_ART_BASIC_1_PDF }; //full_text pdf
  String [] expectedUrlsBasic2 = { EXPECTED_ART_BASIC_2_ABSTRACT,
      EXPECTED_ART_BASIC_2_ABSTRACT, //metadata
      EXPECTED_ART_BASIC_2_PDF,
      EXPECTED_ART_BASIC_2_FULL,
      EXPECTED_ART_BASIC_2_PDF }; 

  //from the odd month-as-content toc
  String [] expectedUrlsMonth1 = { EXPECTED_ART_MONTH_1_ABSTRACT,
      EXPECTED_ART_MONTH_1_ABSTRACT, //metadata
      EXPECTED_ART_MONTH_1_PDF,
      EXPECTED_ART_MONTH_1_FULL,
      EXPECTED_ART_MONTH_1_PDF }; 

  // from the supplement toc
  String [] expectedUrlsSupp1 = { null,
      EXPECTED_ART_SUPPLEMENT_1_FULL, //metadata
      EXPECTED_ART_SUPPLEMENT_1_PDF,
      EXPECTED_ART_SUPPLEMENT_1_FULL,
      EXPECTED_ART_SUPPLEMENT_1_PDF }; 
  String [] expectedUrlsSupp2 = { null,
      EXPECTED_ART_SUPPLEMENT_2_FULL, //metadata
      EXPECTED_ART_SUPPLEMENT_2_PDF,
      EXPECTED_ART_SUPPLEMENT_2_FULL,
      EXPECTED_ART_SUPPLEMENT_2_PDF }; 

  public void storeTestContent(String url) throws Exception {
    log.debug3("storeTestContent() url: " + url);
    InputStream input = null;
    CIProperties props = null;
    String content_string;
    // issue table of content 
    if (url.contains("March") || url.endsWith("6") || url.endsWith("S1")) { 
      if (url.endsWith("6")) {
        content_string = basicTocContent();
      } else if (url.endsWith("S1")) {
        content_string = supplementTocContent();
      } else {
        content_string = monthTocContent();
      }
      input = new StringInputStream(content_string);
      log.debug3("html props - toc");
      props = getHtmlProperties();
     } else if (url.endsWith("pdf")) {
      // pdf
      input = new StringInputStream("");
      log.debug3("pdf props - article");
      props = getPdfProperties();
     } else { // for all others 
      //else if (url.endsWith("abstract") || url.endsWith("3") || url.endsWith("4")) {
      // abs/full-text html
      input = new StringInputStream("<html></html>");
      log.debug3("html props - article");
      props = getHtmlProperties();
    }
    assertNotNull(props);
    UrlData ud = new UrlData(input, props, url);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
  }  

  public void testCreateArticleFiles_BasicTOC() throws Exception {

    ArrayList<String> articleUrls = new ArrayList<String>();
    articleUrls.add(TOC_BASIC_URL); // table of contents 1
    // now the articles
    articleUrls.add(EXPECTED_ART_BASIC_1_PDF);
    articleUrls.add(EXPECTED_ART_BASIC_1_ABSTRACT);
    articleUrls.add(EXPECTED_ART_BASIC_1_FULL);
    articleUrls.add(EXPECTED_ART_BASIC_2_PDF);
    articleUrls.add(EXPECTED_ART_BASIC_2_ABSTRACT);
    articleUrls.add(EXPECTED_ART_BASIC_2_FULL);
    articleUrls.add(EXPECTED_ART_BASIC_2_DATA);

    // and one item that shouldn't end up in an article files object
    articleUrls.add(BASE_URL + "supplements/all");
    articleUrls.add(TOC_BASIC_URL + "123/figure/F9");
    articleUrls.add(TOC_BASIC_URL + "123/citation");
    articleUrls.add(BASE_URL + "content/download/figures/" + JOURNAL_ISSN + "-54-6-123.pdf");

    // Store test cases - articleUrls
    Iterator<String> itr = articleUrls.iterator();
    while (itr.hasNext()) {
      String url = itr.next();
      log.debug3("testCreateArticleFiles() url: " + url);
      storeTestContent(url);
    }

    // access BioMedCentralTOCArticleItrerator
    Iterator<ArticleFiles> it = au.getArticleIterator();
    while (it.hasNext()) {
      ArticleFiles af1 = it.next();
      log.debug3("article file af1: " + af1.toString());

      // assert article 1
      String[] actualUrls1 = { af1.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
          af1.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
          af1.getFullTextUrl(),                                                                      
          af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
          af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) };

      for (int i = 0;i< actualUrls1.length; i++) {
        log.debug3("expected url1: " + expectedUrlsBasic1[i]);
        log.debug3("  actual url1: " + actualUrls1[i]);
        assertEquals(expectedUrlsBasic1[i], actualUrls1[i]);
      }

      // assert article 2 - with frame src
      if (it.hasNext()) {
        ArticleFiles af2 = it.next();
        log.debug3("article file af2: " + af2.toString());

        String[] actualUrls2 = { af2.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
            af2.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
            af2.getFullTextUrl(),                                                                      
            af2.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
            af2.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) };

        for (int i = 0;i< actualUrls2.length; i++) {
          log.debug3("expected url2: " + expectedUrlsBasic2[i]);
          log.debug3("  actual url2: " + actualUrls2[i]);
          assertEquals(expectedUrlsBasic2[i], actualUrls2[i]);
        }
        // this one also had supplementary data landing page
        //assertEquals(EXPECTED_ART_BASIC_2_DATA, af2.getRoleUrl(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS));
      }

    }

  }

  public void testCreateArticleFiles_MonthTOC() throws Exception {

    ArrayList<String> articleUrls = new ArrayList<String>();
    articleUrls.add(TOC_MONTH_URL); // table of contents 2
    // now the articles
    articleUrls.add(EXPECTED_ART_MONTH_1_PDF);
    articleUrls.add(EXPECTED_ART_MONTH_1_ABSTRACT);
    articleUrls.add(EXPECTED_ART_MONTH_1_FULL);

    // Store test cases - articleUrls
    Iterator<String> itr = articleUrls.iterator();
    while (itr.hasNext()) {
      String url = itr.next();
      log.debug3("testCreateArticleFiles() url: " + url);
      storeTestContent(url);
    }

    // access BioMedCentralTOCArticleItrerator
    Iterator<ArticleFiles> it = au.getArticleIterator();
    while (it.hasNext()) {
      ArticleFiles af1 = it.next();
      log.debug3("article file af1: " + af1.toString());

      // assert article 1
      String[] actualUrls1 = { af1.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
          af1.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
          af1.getFullTextUrl(),                                                                      
          af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
          af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) };

      for (int i = 0;i< actualUrls1.length; i++) {
        log.debug3("expected url1: " + expectedUrlsMonth1[i]);
        log.debug3("  actual url1: " + actualUrls1[i]);
        assertEquals(expectedUrlsMonth1[i], actualUrls1[i]);
      }

    }

  }

  public void testCreateArticleFiles_SupplementTOC() throws Exception {

    ArrayList<String> articleUrls = new ArrayList<String>();
    articleUrls.add(TOC_SUPPLEMENT_URL); // table of contents 3
    // now the articles
    // supplements don't seem to have abstracts
    articleUrls.add(EXPECTED_ART_SUPPLEMENT_1_PDF);
    articleUrls.add(EXPECTED_ART_SUPPLEMENT_1_FULL);
    articleUrls.add(EXPECTED_ART_SUPPLEMENT_2_PDF);
    articleUrls.add(EXPECTED_ART_SUPPLEMENT_2_FULL);

    // Store test cases - articleUrls
    Iterator<String> itr = articleUrls.iterator();
    while (itr.hasNext()) {
      String url = itr.next();
      log.debug3("testCreateArticleFiles() url: " + url);
      storeTestContent(url);
    }

    // access BioMedCentralTOCArticleItrerator
    Iterator<ArticleFiles> it = au.getArticleIterator();
    while (it.hasNext()) {
      ArticleFiles af1 = it.next();
      log.debug3("article file af1: " + af1.toString());

      // assert article 1
      String[] actualUrls1 = { af1.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
          af1.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
          af1.getFullTextUrl(),                                                                      
          af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
          af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) };

      for (int i = 0;i< actualUrls1.length; i++) {
        log.debug3("expected url1: " + expectedUrlsSupp1[i]);
        log.debug3("  actual url1: " + actualUrls1[i]);
        assertEquals(expectedUrlsSupp1[i], actualUrls1[i]);
      }

      // assert article 2 - with frame src
      if (it.hasNext()) {
        ArticleFiles af2 = it.next();
        log.debug3("article file af2: " + af2.toString());

        String[] actualUrls2 = { af2.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
            af2.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
            af2.getFullTextUrl(),                                                                      
            af2.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
            af2.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) };

        for (int i = 0;i< actualUrls2.length; i++) {
          log.debug3("expected url2: " + expectedUrlsSupp2[i]);
          log.debug3("  actual url2: " + actualUrls2[i]);
          assertEquals(expectedUrlsSupp2[i], actualUrls2[i]);
        }
      }
    }

  }

  // Response header to be stored with the simulated HTML content URLs
  // Note the X-Locks-content-type is necessary (not Content-type)
  // in order to be found by the LOCKSS Daemon. 
  private CIProperties getHtmlProperties() {
    CIProperties htmlProps = new CIProperties();
    htmlProps.put("RESPONSE","HTTP/1.0 200 OK");
    htmlProps.put("Date", "Fri, 06 Apr 2012 18:22:49 GMT");
    htmlProps.put("Server", "Apache/2.2.3 (CentOS)");
    htmlProps.put("X-Powered-By", "PHP/5.2.17");
    htmlProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
    htmlProps.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
    htmlProps.put("Pragma", "no-cache");
    htmlProps.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    htmlProps.put("X-Cache", "MISS from lockss.org");
    htmlProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
    htmlProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
    htmlProps.put("Connection", "close");
    return htmlProps;
  }

  // Response header to be stored with the simulated PDF content URLs
  // Note the X-Locks-content-type is necessary (not Content-type)
  // in order to be found by the LOCKSS Daemon. 
  private CIProperties getPdfProperties() {
    CIProperties pdfProps = new CIProperties();
    pdfProps.put("RESPONSE","HTTP/1.0 200 OK");
    pdfProps.put("Date", "Fri, 06 Apr 2012 18:22:49 GMT");
    pdfProps.put("Server", "Apache/2.2.3 (CentOS)");
    pdfProps.put("X-Powered-By", "PHP/5.2.17");
    pdfProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
    pdfProps.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
    pdfProps.put("Pragma", "no-cache");
    pdfProps.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    pdfProps.put("X-Cache", "MISS from lockss.org");
    pdfProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
    pdfProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
    pdfProps.put("Connection", "close");
    return pdfProps;
  }

  private  final String TOC_START=
      "<!DOCTYPE html>" +
          "<html><head></head><body>" +
          "<div>biomedcentralarticle</div>" +
          "<table class=\"articles-feed\" cellspacing=\"0\" cellpadding=\"0\">" +
          "<tbody><tr>";

  private  final String    ART_ENTRY_START=
      "<td class=\"article-entry\">" +
          "<p class=\"header\">" +
          "<span class=\"article-type\">Research</span> &nbsp;" +
          "<a href=\"/about/access\"><img alt=\"Open Access\"></a>" +
          "</p>" +
          "<p class=\"article-title-block\">" +
          "<span class=\"article-title\"><strong>" +
          "<a href=\"";                                                                                                                                                      

  private  final String ART_ENTRY_POST_TITLE =                                                                                                                                             
      "\"><p>Title of Article Is Here</p></a></strong></span>" +
          "</p>" +
          "<p class=\"citation\">authors, etc go here</p>" +
          "<div class=\"abstract-toggled\"  style=\"display: none;\"></div>" +
          "<p class=\"nav\"><span class=\"left\">";


  private  final String  ART_ENTRY_END= "</span></span></p> </td>";

  private  final String TOC_END =
      "</tr> </tbody>" +
          "</table>" +
          "</body>" +
          "</html>";

  private  final String ARTICLE_GUTS_BASIC_1 =
      "<a href=\"" +                                                                                                                                                     
          EXPECTED_ART_BASIC_1_ABSTRACT +
          "\" class=\"abstract-link\">Abstract</a>     | <a href=\"" +                                                                                                       
          EXPECTED_ART_BASIC_1_FULL +
          "\" class=\"fulltext-link\">Full text</a>    | <a href=\"" +                                                                                                       
          EXPECTED_ART_BASIC_1_PDF +
          "\" class=\"pdf-link\">PDF</a>               | <a href=\"" +                                                                                                       
          BASE_URL + "pubmed/foo" +                                                                                                                                          
          "\" class=\"pubmed-link\">PubMed</a>";
  private  final String ARTICLE_GUTS_BASIC_2 =
      "<a href=\"" +                                                                                                                                                     
          EXPECTED_ART_BASIC_2_ABSTRACT +
          "\" class=\"abstract-link\">Abstract</a>     | <a href=\"" +                                                                                                       
          EXPECTED_ART_BASIC_2_FULL +
          "\" class=\"fulltext-link\">Full text</a>    | <a href=\"" +                                                                                                       
          EXPECTED_ART_BASIC_2_PDF +
          "\" class=\"pdf-link\">PDF</a>               | <a href=\"" +                                                                                                       
          BASE_URL + "pubmed/foo" +                                                                                                                                          
          "\" class=\"pubmed-link\">PubMed</a>";
  
  private  final String ARTICLE_GUTS_MONTH_1 =
      "<a href=\"" +                                                                                                                                                     
          EXPECTED_ART_MONTH_1_ABSTRACT +
          "\" class=\"abstract-link\">Abstract</a>     | <a href=\"" +                                                                                                       
          EXPECTED_ART_MONTH_1_FULL +
          "\" class=\"fulltext-link\">Full text</a>    | <a href=\"" +                                                                                                       
          EXPECTED_ART_MONTH_1_PDF +
          "\" class=\"pdf-link\">PDF</a>               | <a href=\"" +                                                                                                       
          BASE_URL + "pubmed/foo" +                                                                                                                                          
          "\" class=\"pubmed-link\">PubMed</a>";
  
  private  final String ARTICLE_GUTS_SUPPL_1 =
      "<a href=\"" +                                                                                                                                                                                                                                                            
          EXPECTED_ART_SUPPLEMENT_1_FULL +
          "\" class=\"fulltext-link\">Full text</a>    | <a href=\"" +                                                                                                       
          EXPECTED_ART_SUPPLEMENT_1_PDF +
          "\" class=\"pdf-link\">PDF</a>";
  private  final String ARTICLE_GUTS_SUPPL_2 =
      "<a href=\"" +                                                                                                                                                                                                                                                            
          EXPECTED_ART_SUPPLEMENT_2_FULL +
          "\" class=\"fulltext-link\">Full text</a>    | <a href=\"" +                                                                                                       
          EXPECTED_ART_SUPPLEMENT_2_PDF +
          "\" class=\"pdf-link\">PDF</a>";
  
  private String basicTocContent() {
    StringBuilder sb = new StringBuilder();
        sb.append(TOC_START);
        //first article
        sb.append(ART_ENTRY_START);
        sb.append(EXPECTED_ART_BASIC_1_FULL);
        sb.append(ART_ENTRY_POST_TITLE);
        sb.append(ARTICLE_GUTS_BASIC_1);
        sb.append(ART_ENTRY_END);
        //second article
        sb.append(ART_ENTRY_START);
        sb.append(EXPECTED_ART_BASIC_2_FULL);
        sb.append(ART_ENTRY_POST_TITLE);
        sb.append(ARTICLE_GUTS_BASIC_2);
        sb.append(ART_ENTRY_END);
        sb.append(TOC_END);
    return sb.toString();
  }
  private String monthTocContent() {
    StringBuilder sb = new StringBuilder();
        sb.append(TOC_START);
        //first article
        sb.append(ART_ENTRY_START);
        sb.append(EXPECTED_ART_MONTH_1_FULL);
        sb.append(ART_ENTRY_POST_TITLE);
        sb.append(ARTICLE_GUTS_MONTH_1);
        sb.append(ART_ENTRY_END);
        sb.append(TOC_END);
    return sb.toString();      
  }
  private String supplementTocContent() {
    StringBuilder sb = new StringBuilder();
        sb.append(TOC_START);
        //first article
        sb.append(ART_ENTRY_START);
        sb.append(EXPECTED_ART_SUPPLEMENT_1_FULL);
        sb.append(ART_ENTRY_POST_TITLE);
        sb.append(ARTICLE_GUTS_SUPPL_1);
        sb.append(ART_ENTRY_END);
        //second article
        sb.append(ART_ENTRY_START);
        sb.append(EXPECTED_ART_SUPPLEMENT_2_FULL);
        sb.append(ART_ENTRY_POST_TITLE);
        sb.append(ARTICLE_GUTS_SUPPL_2);
        sb.append(ART_ENTRY_END);
        sb.append(TOC_END);
    return sb.toString();        
  }  
  
  

}
