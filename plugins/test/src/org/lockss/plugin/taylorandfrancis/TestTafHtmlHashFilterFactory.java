/*  $Id: TestBaseAtyponHtmlHashFilterFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 
 Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,

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

package org.lockss.plugin.taylorandfrancis;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.atypon.BaseAtyponLoginPageChecker;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.HeaderUtil;
import org.lockss.util.StringUtil;

public class TestTafHtmlHashFilterFactory extends LockssTestCase {
  private TafHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new TafHtmlHashFilterFactory();
  }

  public String getFileAsString(String fName) throws Exception {
    InputStream actIn = fact.createFilteredInputStream(
        mau,
        getResourceAsStream(fName),
        Constants.ENCODING_UTF_8);
    String fStr = StringUtil.fromInputStream(actIn);
    return fStr;
  }

  public String filterString(String string) throws Exception {
    InputStream actIn = fact.createFilteredInputStream(
        mau,
        new StringInputStream(string),
        Constants.DEFAULT_ENCODING);
    String fStr = StringUtil.fromInputStream(actIn);
    return fStr;
  }

  private static final String withPrevNext =
  "    <div class=\"overview borderedmodule-last\">\n" +
  "<div class=\"hd\">\n" +
"      <h2>\n" +
"      <a href=\"vol_47\">Volume 47</a>,\n\n" +
"<span style=\"float: right;margin-right: 5px\">\n" +
"      \n" +
"      \n" +
"<a href=\"/47/2\" title=\"Previous issue\">&lt; Prev</a>\n\n\n" +

"|\n\n\n" +
"<a href=\"47/4\" title=\"Next issue\">Next &gt;</a>\n" +
"</span>\n" +
"      </h2>\n" +
"  </div>\n";

  private static final String withoutPrevNext =
      "";//"" Volume 47 ";
  
  private static final String manifest =
"<!DOCTYPE html>\n"+
" <html>\n"+
" <head>\n"+
"     <title>2012 CLOCKSS Manifest Page</title>\n"+
"     <meta charset=\"UTF-8\" />\n"+
" </head>\n"+
" <body>\n"+
" <h1>2012 CLOCKSS Manifest Page</h1>\n"+
" <ul>\n"+
"     \n"+
"     <li><a href=\"http://www.online.com/toc/20/17/4\">01 Oct 2012 (Vol. 17 Issue 4 Page 291-368)</a></li>\n"+
"     \n"+
"     <li><a href=\"http://www.online.com/toc/20/17/2-3\">01 Jul 2012 (Vol. 17 Issue 2-3 Page 85-290)</a></li>\n"+
"     \n"+
"     <li><a href=\"http://www.online.com/toc/20/17/1\">01 Jan 2012 (Vol. 17 Issue 1 Page 1-84)</a></li>\n"+
"     \n"+
" </ul>\n"+
" <p>\n"+
"     <img src=\"http://www.lockss.org/images/LOCKSS-small.gif\" height=\"108\" width=\"108\" alt=\"LOCKSS logo\"/>\n"+
"     CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit.\n"+
" </p>\n"+
" </body>\n"+
" </html>\n";
 
  private static final String manifestFiltered =
      " 01 Oct 2012 (Vol. 17 Issue 4 Page 291-368) 01 Jul 2012 (Vol. 17 Issue 2-3 Page 85-290) 01 Jan 2012 (Vol. 17 Issue 1 Page 1-84) ";

  private static final String withTocLink =
"     <div class=\"options\">\n"+
"   <ul>\n"+
"       <li class=\"publisherImprint\">\n"+
"           Route\n"+
"       </li>\n"+
"       <li>\n"+
"           <a href=\"/toc/rmle20/15/4\">\n"+
"              Sample copy\n"+
"           </a>\n"+
"       </li>\n"+
"       </div> ";
 
  private static final String withoutTocLink =
      "";

   private static final String withPeopleAlsoRead =
"    <div class=\"overview borderedmodule-last\">Hello World" +
    "      <div class=\"foo\"> Hello Article" +
//Originally had this line - but this leads to Hello Article getting included twice
// because both regex"overview" and "tocArticleEntry are in the include filter
// which needs investigation but that isn't the point of this test...
//    "      <div class=\"tocArticleEntry\"> Hello Article </div>" +
"    <div class=\"widget combinedRecommendationsWidget none  widget-none  widget-compact-vertical\" id=\"abcde\"  >" + //147_1
"      <div class=\"wrapped \" ><h1 class=\"widget-header header-none  header-compact-vertical\">People also read</h1>" + //148_2 wrapped
"        <div class=\"widget-body body body-none  body-compact-vertical\">" + //149_3 widget-body
"          <div class=\"relatedArt\">" +       //150_4 related_art
"            <div class=\"sidebar\">" + //151_5 sidebar
"              <div class=\"relatedItem\"> " +  //152_6 relatedItem                   
"                <div class=\"article-card col-md-1-4\">" + //153_7 article-card
"                  <div class=\"header\">" + //154_8  header
"                    <div class=\"art_title  hlFld-Title\">" + //155_9 art_title
"                      <div class=\"article_type\">Article" + //156_10 article_type tests "_"
"                      </div><a class=\"ref nowrap\" href=\"/doi/full/10.1080/2049761X.2015.1107307?src=recsys\">Cape Town Convention closing opinions in aircraft finance transactions: custom, standards and practice</a><span class=\"access-icon oa\"></span>" + //156_10 article-type
"                    </div>" + //155_9 art_title
"                  </div>" + //154_8  header
"                  <div class=\"footer\"><a class=\"entryAuthor search-link\" href=\"/author/Durham%2C+Phillip+L\"><span class=\"hlFld-ContribAuthor\">Phillip L Durham</span></a> et al." + //160_11 footer
"                    <div class=\"card-section\">Cape Town Convention Journal" + //161_12 card-section
"                    </div>" + //161_12 card-section
"                    <div class=\"card-section\">" + //163_13 card-section
"                      <div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online: </strong>4 Nov 2015</span>" + //164_14 tocEPubDate
"                      </div>" + //164_14 tocEPubDate
"                    </div>" + //163_13 card-section
"                  </div><span class=\"access-icon oa\"></span>" + //160_11 footer
"                </div>" + //153_7 article-card
"              </div>" + //152_6 relatedItem
"            </div>" + //151_5 sidebar
"          </div>" + //150_4 related_art
"        </div>" + //149_3 widget-body
"      </div>" + //148_2 wrapped
"    </div>" + //147_1
"    </div>" ; 
  
  
  private static final String withoutPeopleAlsoRead =  
"";//"" Hello World Hello Article ";
  
  private static final String withArticleMetrics = 
"    <div class=\"overview borderedmodule-last\">Hello Kitty" +      
"    <div class=\"widget literatumArticleMetricsWidget none  widget-none\" id=\"123\"  >" +   
"      <div class=\"section citations\">" +
"        <div class=\"title\">" +
"          Citations" +
"          <span> CrossRef </span> " +
"          <span class=\"value\">0</span>" +
"          <span> Scopus </span>" +
"          <span class=\"value\">6</span>" +
"        </div>" +
"      </div>" +
"    </div>" +
"    </div>";  

  private static final String withoutArticleMetrics =  
"";//"" Hello Kitty ";
  
  private static final String withTocArticleEntry = "<div class=\"tocArticleEntry\"> Hello Article </div>" +
  "<div class=\"tocArticleEntry include-metrics-panel\"> Hello World </div>";
  
  private static final String withoutTocArticleEntry =
      "";//"" Hello Article Hello World ";
  
  private static final String hasBulletList =
  "    <div class=\"overview borderedmodule-last\">Hello World" +
  "The following aspects undergo changes: " +
  "<table class=\"listgroup\" border=\"0\" width=\"95%\" list-type=\"bullet\">" +
  "<tr class=\"li1\"><td valign=\"top\" class=\"list-td\">•</td><td colspan=\"5\" valign=\"top\">" +
  "<p>view and interpretation of the history of building, art, and culture</p></td></tr>" +
  "<tr class=\"li1\"><td valign=\"top\" class=\"list-td\">•</td><td colspan=\"5\" valign=\"top\">" +
  "<p>theories and practices of preservation, as well as</p></td></tr>" +
  "<tr class=\"li1\"><td valign=\"top\" class=\"list-td\">•</td><td colspan=\"5\" valign=\"top\">" +
  "<p>‘archival taxonomies’, " +
  "</p></td></tr></table></div>";
  private static final String hasNoBulletList =
      "    <div class=\"overview borderedmodule-last\">Hello World" +
      "The following aspects undergo changes: " +
      "<table class=\"listgroup\" border=\"0\" width=\"95%\" list-type=\"bullet\">" +
      "<tr class=\"li1\"><td valign=\"top\" class=\"list-td\">X</td><td colspan=\"5\" valign=\"top\">" +
      "<p>view and interpretation of the history of building, art, and culture</p></td></tr>" +
      "<tr class=\"li1\"><td valign=\"top\" class=\"list-td\">X</td><td colspan=\"5\" valign=\"top\">" +
      "<p>theories and practices of preservation, as well as</p></td></tr>" +
      "<tr class=\"li1\"><td valign=\"top\" class=\"list-td\">X</td><td colspan=\"5\" valign=\"top\">" +
      "<p>‘archival taxonomies’, " +
      "</p></td></tr></table></div>";
  private static final String noBulletList =
  "    <div class=\"overview borderedmodule-last\">Hello World" +
  "The following aspects undergo changes: " +
  "<table class=\"listgroup\" border=\"0\" width=\"95%\" list-type=\"bullet\">" +
  "<tr class=\"li1\"><td valign=\"top\" class=\"list-td\"><ul class=\"listLabel\"><li></li></ul></td><td colspan=\"5\" valign=\"top\">" +
  "<p>view and interpretation of the history of building, art, and culture</p></td></tr>" +
  "<tr class=\"li1\"><td valign=\"top\" class=\"list-td\"><ul class=\"listLabel\"><li></li></ul></td><td colspan=\"5\" valign=\"top\">" +
  "<p>theories and practices of preservation, as well as</p></td></tr>" +
  "<tr class=\"li1\"><td valign=\"top\" class=\"list-td\"><ul class=\"listLabel\"><li></li></ul></td><td colspan=\"5\" valign=\"top\">" +
  "<p>‘archival taxonomies’, " +
  "</p></td></tr></table></div>";


  private static final String tocList =
    "<div class=\"widget tocListWidget none  widget-none  widget-compact-vertical\" id=\"04c2ca1e-eb2b-40c6-9382-86aa5eb0dcfb\">\n" +
        "<div class=\"wrapped \">\n" +
        "<div class=\"widget-body body body-none  body-compact-vertical\"><script type=\"text/javascript\" async=\"\" src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.0/MathJax.js?config=TeX-AMS-MML_HTMLorMML\"></script>\n" +
        "<fieldset class=\"tocListWidgetContainer activeWidget\">\n" +
        "<div class=\"checkbox-holder\">\n" +
        "<label for=\"markall\" class=\"checkbox--primary\">\n" +
        "<input type=\"checkbox\" name=\"markall\" id=\"markall\" onclick=\"onClickMarkAll('frmAbs')\">\n" +
        "<span class=\"box-btn\"></span>\n" +
        "<a href=\"#\" title=\"Export Citations\" class=\"toc-download-citations\">\n" +
        "<i class=\"icon-quote\"></i>\n" +
        "<span>Download citations</span>\n" +
        "</a>\n" +
        "</label>\n" +
        "</div>\n" +
        "<div class=\"citations-modal\" id=\"downloadCitationModal\">\n" +
        "<div class=\"citations-modal__content\">\n" +
        "<span class=\"citations-modal__close\">×</span>\n" +
        "<div class=\"citations-modal__header\">\n" +
        "<h2>Download citations</h2>\n" +
        "<div><b id=\"citationsCount\">0 </b>\n" +
        "<spanc class=\"selected-title\">Citations Selected</spanc>\n" +
        "</div>\n" +
        "</div>\n" +
        "<div>\n" +
        "<form action=\"/action/downloadCitation\" method=\"post\" target=\"_self\" name=\"downloadCitationForm\">\n" +
        "<h3>Choose format</h3>\n" +
        "<label for=\"ris\" class=\"checkbox--primary\">\n" +
        "<input onclick=\"onlyOne(document.downloadCitationForm, this);\" id=\"ris\" type=\"checkbox\" value=\"ris\" checked=\"checked\">\n" +
        "<span class=\"box-btn-dot\"></span>\n" +
        "<span class=\"box-txt\">RIS (ProCit, Reference Manager)</span>\n" +
        "</label>\n" +
        "<br>\n" +
        "<label for=\"bibtex\" class=\"checkbox--primary\">\n" +
        "<input onclick=\"onlyOne(document.downloadCitationForm, this);\" id=\"bibtex\" type=\"checkbox\" value=\"bibtex\">\n" +
        "<span class=\"box-btn-dot\"></span>\n" +
        "<span class=\"box-txt\">BibTeX</span>\n" +
        "</label>\n" +
        "<br>\n" +
        "<label for=\"refworks\" class=\"checkbox--primary\">\n" +
        "<input onclick=\"onlyOne(document.downloadCitationForm, this);\" id=\"refworks\" type=\"checkbox\" value=\"refworks\">\n" +
        "<span class=\"box-btn-dot\"></span>\n" +
        "<span class=\"box-txt\">RefWorks Direct Export</span>\n" +
        "</label>\n" +
        "<br>\n" +
        "<span class=\"select-arrow\"><i class=\"icon-bottom-arrow\"></i></span>\n" +
        "<div class=\"downloadCitations\">\n" +
        "<span class=\"form-button\">Download citations</span>\n" +
        "</div>\n" +
        "<input type=\"hidden\" name=\"format\">\n" +
        "<input type=\"hidden\" name=\"direct\" value=\"true\">\n" +
        "<input type=\"hidden\" name=\"doi\">\n" +
        "<input type=\"hidden\" name=\"downloadFileName\" value=\"tandf_citations\">\n" +
        "<input type=\"hidden\" name=\"href\" value=\"/toc/talc20/38/1\">\n" +
        "</form>\n" +
        "</div>\n" +
        "</div>\n" +
        "</div>\n" +
        "<script> loadUpDownloadCitationsModal();</script>\n" +
        "<form name=\"frmAbs\">\n" +
        "<fieldset class=\"tocTools\">\n" +
        "<div class=\"tocListDropZone float-right\" data-pb-dropzone=\"tocListDropZone\">\n" +
        "</div>\n" +
        "</fieldset>\n" +
        "<div class=\"tocListtDropZone2\" data-pb-dropzone=\"tocListtDropZone2\">\n" +
        "</div>\n" +
        "<div class=\"tocContent\">\n" +
        "<div class=\"tocHeading\"><span class=\"subj-group\">Research Articles </span></div><table class=\"articleEntry\" width=\"100%\" border=\"0\"><tbody><tr><td valign=\"top\"><div class=\"tocArticleEntry include-metrics-panel toc-citation\"><div class=\"citation-item-checkbox-container\"><label tabindex=\"0\" class=\"checkbox--primary\"><input type=\"checkbox\" name=\"10.1080/03115518.2013.806209\"><span class=\"box-btn\"></span></label></div><div class=\"article-type\">Article</div><div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1080/03115518.2013.806209\"><span class=\"hlFld-Title\">Archaeocyaths of the White Point Conglomerate, Kangaroo Island, South Australia</span></a></div><div class=\"tocentryright\"><div class=\"tocAuthors afterTitle\"><div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\"><span><a href=\"/author/Kruse%2C+Peter+D\">Peter D. Kruse</a></span> &amp; <span><a href=\"/author/Moreno-Eiris%2C+Elena\">Elena Moreno-Eiris</a></span></span></div></div><div class=\"tocPageRange maintextleft\">Pages: 1-64</div><div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online:</strong> 21 Aug 2013</span></div></div><div class=\"sfxLinkButton\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1080/03115518.2013.806209&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dliteratum%3Atandf%26id%3Ddoi%3A10.1080%2F03115518.2013.806209\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/98617/sfxbutton\" alt=\"OpenURL Stanford University\"></a></div><div class=\"tocDeliverFormatsLinks\"><a href=\"/doi/abs/10.1080/03115518.2013.806209\">Abstract</a> | <a class=\"ref nowrap full\" href=\"/doi/full/10.1080/03115518.2013.806209\">Full Text</a> | <a class=\"ref nowrap references\" href=\"/doi/ref/10.1080/03115518.2013.806209\">References</a> | <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1080/03115518.2013.806209\">PDF (11214 KB)</a> | <a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D1%26pageCount%3D64%26author%3DPeter%2BD.%2BKruse%252C%2B%252C%2BElena%2BMoreno-Eiris%26orderBeanReset%3Dtrue%26imprint%3DTaylor%2B%2526%2BFrancis%26volumeNum%3D38%26issueNum%3D1%26contentID%3D10.1080%252F03115518.2013.806209%26title%3DArchaeocyaths%2Bof%2Bthe%2BWhite%2BPoint%2BConglomerate%252C%2BKangaroo%2BIsland%252C%2BSouth%2BAustralia%26numPages%3D64%26pa%3D%26issn%3D0311-5518%26publisherName%3Dtandfuk%26publication%3DTALC%26rpt%3Dn%26endPage%3D64%26publicationDate%3D01%252F02%252F2014\" class=\"rightslink\" target=\"_blank\" title=\"Opens new window\">Permissions</a>&nbsp;<div id=\"Abs031155182013806209\" class=\"previewViewSection tocPreview\"><div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1080/03115518.2013.806209', 'Abs031155182013806209');\"></div><p class=\"previewContent\"></p></div></div><div class=\"metrics-panel\"><ul class=\"altmetric-score true\"><li><span>580</span>Views</li><li><span>10</span>CrossRef citations</li><li class=\"value\" data=\"10.1080/03115518.2013.806209\">Altmetric</li></ul></div><span class=\"access-icon free\"></span></div></td></tr></tbody></table><table class=\"articleEntry\" width=\"100%\" border=\"0\"><tbody><tr><td valign=\"top\"><div class=\"tocArticleEntry include-metrics-panel toc-citation\"><div class=\"citation-item-checkbox-container\"><label tabindex=\"0\" class=\"checkbox--primary\"><input type=\"checkbox\" name=\"10.1080/03115518.2013.828251\"><span class=\"box-btn\"></span></label></div><div class=\"article-type\">Article</div><div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1080/03115518.2013.828251\"><span class=\"hlFld-Title\">Two new kalligrammatids (Insecta, Neuroptera) from the Middle Jurassic of Daohugou, Inner Mongolia, China</span></a></div><div class=\"tocentryright\"><div class=\"tocAuthors afterTitle\"><div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\"><span><a href=\"/author/Liu%2C+Qing\">Qing Liu</a></span>, <span><a href=\"/author/Zheng%2C+Daran\">Daran Zheng</a></span>, <span><a href=\"/author/Zhang%2C+Qi\">Qi Zhang</a></span>, <span><a href=\"/author/Wang%2C+Bo\">Bo Wang</a></span>, <span><a href=\"/author/Fang%2C+Yan\">Yan Fang</a></span> &amp; <span><a href=\"/author/Zhang%2C+Haichun\">Haichun Zhang</a></span></span></div></div><div class=\"tocPageRange maintextleft\">Pages: 65-69</div><div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online:</strong> 10 Sep 2013</span></div></div><div class=\"sfxLinkButton\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1080/03115518.2013.828251&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dliteratum%3Atandf%26id%3Ddoi%3A10.1080%2F03115518.2013.828251\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/98617/sfxbutton\" alt=\"OpenURL Stanford University\"></a></div><div class=\"tocDeliverFormatsLinks\"><a href=\"/doi/abs/10.1080/03115518.2013.828251\">Abstract</a> | <a class=\"ref nowrap full\" href=\"/doi/full/10.1080/03115518.2013.828251\">Full Text</a> | <a class=\"ref nowrap references\" href=\"/doi/ref/10.1080/03115518.2013.828251\">References</a> | <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1080/03115518.2013.828251\">PDF (553 KB)</a> | <a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D65%26pageCount%3D5%26author%3DQing%2BLiu%252C%2B%252C%2BDaran%2BZheng%252C%2Bet%2Bal%26orderBeanReset%3Dtrue%26imprint%3DTaylor%2B%2526%2BFrancis%26volumeNum%3D38%26issueNum%3D1%26contentID%3D10.1080%252F03115518.2013.828251%26title%3DTwo%2Bnew%2Bkalligrammatids%2B%2528Insecta%252C%2BNeuroptera%2529%2Bfrom%2Bthe%2BMiddle%2BJurassic%2Bof%2BDaohugou%252C%2BInner%2BMongolia%252C%2BChina%26numPages%3D5%26pa%3D%26issn%3D0311-5518%26publisherName%3Dtandfuk%26publication%3DTALC%26rpt%3Dn%26endPage%3D69%26publicationDate%3D01%252F02%252F2014\" class=\"rightslink\" target=\"_blank\" title=\"Opens new window\">Permissions</a>&nbsp;<div id=\"Abs031155182013828251\" class=\"previewViewSection tocPreview\"><div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1080/03115518.2013.828251', 'Abs031155182013828251');\"></div><p class=\"previewContent\"></p></div></div><div class=\"metrics-panel\"><ul class=\"altmetric-score true\"><li><span>227</span>Views</li><li><span>9</span>CrossRef citations</li><li class=\"value\" data=\"10.1080/03115518.2013.828251\">Altmetric</li></ul></div><span class=\"access-icon free\"></span></div></td></tr></tbody></table><table class=\"articleEntry\" width=\"100%\" border=\"0\"><tbody><tr><td valign=\"top\"><div class=\"tocArticleEntry include-metrics-panel toc-citation\"><div class=\"citation-item-checkbox-container\"><label tabindex=\"0\" class=\"checkbox--primary\"><input type=\"checkbox\" name=\"10.1080/03115518.2013.828253\"><span class=\"box-btn\"></span></label></div><div class=\"article-type\">Article</div><div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1080/03115518.2013.828253\"><span class=\"hlFld-Title\">Trilobites from the Middle Ordovician Stairway Sandstone, Amadeus Basin, central Australia</span></a></div><div class=\"tocentryright\"><div class=\"tocAuthors afterTitle\"><div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\"><span><a href=\"/author/Jakobsen%2C+Kristian+G\">Kristian G. Jakobsen</a></span>, <span><a href=\"/author/Nielsen%2C+Arne+T\">Arne T. Nielsen</a></span>, <span><a href=\"/author/Harper%2C+David+A+T\">David A. T. Harper</a></span> &amp; <span><a href=\"/author/Brock%2C+Glenn+A\">Glenn A. Brock</a></span></span></div></div><div class=\"tocPageRange maintextleft\">Pages: 70-96</div><div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online:</strong> 11 Sep 2013</span></div></div><div class=\"sfxLinkButton\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1080/03115518.2013.828253&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dliteratum%3Atandf%26id%3Ddoi%3A10.1080%2F03115518.2013.828253\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/98617/sfxbutton\" alt=\"OpenURL Stanford University\"></a></div><div class=\"tocDeliverFormatsLinks\"><a href=\"/doi/abs/10.1080/03115518.2013.828253\">Abstract</a> | <a class=\"ref nowrap full\" href=\"/doi/full/10.1080/03115518.2013.828253\">Full Text</a> | <a class=\"ref nowrap references\" href=\"/doi/ref/10.1080/03115518.2013.828253\">References</a> | <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1080/03115518.2013.828253\">PDF (3632 KB)</a> | <a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D70%26pageCount%3D27%26author%3DKristian%2BG.%2BJakobsen%252C%2B%252C%2BArne%2BT.%2BNielsen%252C%2Bet%2Bal%26orderBeanReset%3Dtrue%26imprint%3DTaylor%2B%2526%2BFrancis%26volumeNum%3D38%26issueNum%3D1%26contentID%3D10.1080%252F03115518.2013.828253%26title%3DTrilobites%2Bfrom%2Bthe%2BMiddle%2BOrdovician%2BStairway%2BSandstone%252C%2BAmadeus%2BBasin%252C%2Bcentral%2BAustralia%26numPages%3D27%26pa%3D%26issn%3D0311-5518%26publisherName%3Dtandfuk%26publication%3DTALC%26rpt%3Dn%26endPage%3D96%26publicationDate%3D01%252F02%252F2014\" class=\"rightslink\" target=\"_blank\" title=\"Opens new window\">Permissions</a>&nbsp;<div id=\"Abs031155182013828253\" class=\"previewViewSection tocPreview\"><div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1080/03115518.2013.828253', 'Abs031155182013828253');\"></div><p class=\"previewContent\"></p></div></div><div class=\"metrics-panel\"><ul class=\"altmetric-score true\"><li><span>236</span>Views</li><li><span>2</span>CrossRef citations</li><li class=\"value\" data=\"10.1080/03115518.2013.828253\">Altmetric</li></ul></div><span class=\"access-icon free\"></span></div></td></tr></tbody></table><table class=\"articleEntry\" width=\"100%\" border=\"0\"><tbody><tr><td valign=\"top\"><div class=\"tocArticleEntry include-metrics-panel toc-citation\"><div class=\"citation-item-checkbox-container\"><label tabindex=\"0\" class=\"checkbox--primary\"><input type=\"checkbox\" name=\"10.1080/03115518.2013.828252\"><span class=\"box-btn\"></span></label></div><div class=\"article-type\">Article</div><div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1080/03115518.2013.828252\"><span class=\"hlFld-Title\">The first Australian palynologist: Isabel Clifton Cookson (1893–1973) and her scientific work</span></a></div><div class=\"tocentryright\"><div class=\"tocAuthors afterTitle\"><div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\"><span><a href=\"/author/Riding%2C+James+B\">James B. Riding</a></span> &amp; <span><a href=\"/author/Dettmann%2C+Mary+E\">Mary E. Dettmann</a></span></span></div></div><div class=\"tocPageRange maintextleft\">Pages: 97-129</div><div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online:</strong> 10 Sep 2013</span></div></div><div class=\"sfxLinkButton\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1080/03115518.2013.828252&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dliteratum%3Atandf%26id%3Ddoi%3A10.1080%2F03115518.2013.828252\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/98617/sfxbutton\" alt=\"OpenURL Stanford University\"></a></div><div class=\"tocDeliverFormatsLinks\"><a href=\"/doi/abs/10.1080/03115518.2013.828252\">Abstract</a> | <a class=\"ref nowrap full\" href=\"/doi/full/10.1080/03115518.2013.828252\">Full Text</a> | <a class=\"ref nowrap references\" href=\"/doi/ref/10.1080/03115518.2013.828252\">References</a> | <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1080/03115518.2013.828252\">PDF (1327 KB)</a> | <a class=\"ref nowrap\" href=\"/doi/suppl/10.1080/03115518.2013.828252\">Supplemental</a> | <a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D97%26pageCount%3D33%26author%3DJames%2BB.%2BRiding%252C%2B%252C%2BMary%2BE.%2BDettmann%26orderBeanReset%3Dtrue%26imprint%3DTaylor%2B%2526%2BFrancis%26volumeNum%3D38%26issueNum%3D1%26contentID%3D10.1080%252F03115518.2013.828252%26title%3DThe%2Bfirst%2BAustralian%2Bpalynologist%253A%2BIsabel%2BClifton%2BCookson%2B%25281893%25E2%2580%25931973%2529%2Band%2Bher%2Bscientific%2Bwork%26numPages%3D33%26pa%3D%26issn%3D0311-5518%26publisherName%3Dtandfuk%26publication%3DTALC%26rpt%3Dn%26endPage%3D129%26publicationDate%3D01%252F02%252F2014\" class=\"rightslink\" target=\"_blank\" title=\"Opens new window\">Permissions</a>&nbsp;<div id=\"Abs031155182013828252\" class=\"previewViewSection tocPreview\"><div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1080/03115518.2013.828252', 'Abs031155182013828252');\"></div><p class=\"previewContent\"></p></div></div><div class=\"metrics-panel\"><ul class=\"altmetric-score true\"><li><span>178</span>Views</li><li><span>4</span>CrossRef citations</li><li class=\"value\" data=\"10.1080/03115518.2013.828252\">Altmetric</li></ul></div><span class=\"access-icon free\"></span></div></td></tr></tbody></table><table class=\"articleEntry\" width=\"100%\" border=\"0\"><tbody><tr><td valign=\"top\"><div class=\"tocArticleEntry include-metrics-panel toc-citation\"><div class=\"citation-item-checkbox-container\"><label tabindex=\"0\" class=\"checkbox--primary\"><input type=\"checkbox\" name=\"10.1080/03115518.2014.843385\"><span class=\"box-btn\"></span></label></div><div class=\"article-type\">Article</div><div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1080/03115518.2014.843385\"><span class=\"hlFld-Title\">A new Middle Jurassic Chinese fossil clarifies the systematic composition of the Heterophlebioptera (Odonata: Trigonoptera)</span></a></div><div class=\"tocentryright\"><div class=\"tocAuthors afterTitle\"><div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\"><span><a href=\"/author/Nel%2C+Andr%C3%A9\">André Nel</a></span>, <span><a href=\"/author/Azar%2C+Dany\">Dany Azar</a></span> &amp; <span><a href=\"/author/Huang%2C+Di-Ying\">Di-Ying Huang</a></span></span></div></div><div class=\"tocPageRange maintextleft\">Pages: 130-134</div><div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online:</strong> 15 Oct 2013</span></div></div><div class=\"sfxLinkButton\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1080/03115518.2014.843385&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dliteratum%3Atandf%26id%3Ddoi%3A10.1080%2F03115518.2014.843385\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/98617/sfxbutton\" alt=\"OpenURL Stanford University\"></a></div><div class=\"tocDeliverFormatsLinks\"><a href=\"/doi/abs/10.1080/03115518.2014.843385\">Abstract</a> | <a class=\"ref nowrap full\" href=\"/doi/full/10.1080/03115518.2014.843385\">Full Text</a> | <a class=\"ref nowrap references\" href=\"/doi/ref/10.1080/03115518.2014.843385\">References</a> | <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1080/03115518.2014.843385\">PDF (264 KB)</a> | <a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D130%26pageCount%3D5%26author%3DAndr%25C3%25A9%2BNel%252C%2B%252C%2BDany%2BAzar%252C%2Bet%2Bal%26orderBeanReset%3Dtrue%26imprint%3DTaylor%2B%2526%2BFrancis%26volumeNum%3D38%26issueNum%3D1%26contentID%3D10.1080%252F03115518.2014.843385%26title%3DA%2Bnew%2BMiddle%2BJurassic%2BChinese%2Bfossil%2Bclarifies%2Bthe%2Bsystematic%2Bcomposition%2Bof%2Bthe%2BHeterophlebioptera%2B%2528Odonata%253A%2BTrigonoptera%2529%26numPages%3D5%26pa%3D%26issn%3D0311-5518%26publisherName%3Dtandfuk%26publication%3DTALC%26rpt%3Dn%26endPage%3D134%26publicationDate%3D01%252F02%252F2014\" class=\"rightslink\" target=\"_blank\" title=\"Opens new window\">Permissions</a>&nbsp;<div id=\"Abs031155182014843385\" class=\"previewViewSection tocPreview\"><div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1080/03115518.2014.843385', 'Abs031155182014843385');\"></div><p class=\"previewContent\"></p></div></div><div class=\"metrics-panel\"><ul class=\"altmetric-score true\"><li><span>86</span>Views</li><li><span>1</span>CrossRef citations</li><li class=\"value\" data=\"10.1080/03115518.2014.843385\">Altmetric</li></ul></div><span class=\"access-icon free\"></span></div></td></tr></tbody></table><table class=\"articleEntry\" width=\"100%\" border=\"0\"><tbody><tr><td valign=\"top\"><div class=\"tocArticleEntry include-metrics-panel toc-citation\"><div class=\"citation-item-checkbox-container\"><label tabindex=\"0\" class=\"checkbox--primary\"><input type=\"checkbox\" name=\"10.1080/03115518.2014.843145\"><span class=\"box-btn\"></span></label></div><div class=\"article-type\">Article</div><div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1080/03115518.2014.843145\"><span class=\"hlFld-Title\">New potential nearest living relatives for Araucariaceae producing fossil Wollemi Pine-type pollen (<i>Dilwynites granulatus</i> W.K. Harris, 1965)</span></a></div><div class=\"tocentryright\"><div class=\"tocAuthors afterTitle\"><div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\"><span><a href=\"/author/Macphailm%2C+Mike\">Mike Macphailm</a></span> &amp; <span><a href=\"/author/Carpenter%2C+Raymond+J\">Raymond J. Carpenter</a></span></span></div></div><div class=\"tocPageRange maintextleft\">Pages: 135-139</div><div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online:</strong> 14 Oct 2013</span></div></div><div class=\"sfxLinkButton\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1080/03115518.2014.843145&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dliteratum%3Atandf%26id%3Ddoi%3A10.1080%2F03115518.2014.843145\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/98617/sfxbutton\" alt=\"OpenURL Stanford University\"></a></div><div class=\"tocDeliverFormatsLinks\"><a href=\"/doi/abs/10.1080/03115518.2014.843145\">Abstract</a> | <a class=\"ref nowrap full\" href=\"/doi/full/10.1080/03115518.2014.843145\">Full Text</a> | <a class=\"ref nowrap references\" href=\"/doi/ref/10.1080/03115518.2014.843145\">References</a> | <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1080/03115518.2014.843145\">PDF (256 KB)</a> | <a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D135%26pageCount%3D5%26author%3DMike%2BMacphailm%252C%2B%252C%2BRaymond%2BJ.%2BCarpenter%26orderBeanReset%3Dtrue%26imprint%3DTaylor%2B%2526%2BFrancis%26volumeNum%3D38%26issueNum%3D1%26contentID%3D10.1080%252F03115518.2014.843145%26title%3DNew%2Bpotential%2Bnearest%2Bliving%2Brelatives%2Bfor%2BAraucariaceae%2Bproducing%2Bfossil%2BWollemi%2BPine-type%2Bpollen%2B%2528Dilwynites%2Bgranulatus%2BW.K.%2BHarris%252C%2B1965%2529%26numPages%3D5%26pa%3D%26issn%3D0311-5518%26publisherName%3Dtandfuk%26publication%3DTALC%26rpt%3Dn%26endPage%3D139%26publicationDate%3D01%252F02%252F2014\" class=\"rightslink\" target=\"_blank\" title=\"Opens new window\">Permissions</a>&nbsp;<div id=\"Abs031155182014843145\" class=\"previewViewSection tocPreview\"><div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1080/03115518.2014.843145', 'Abs031155182014843145');\"></div><p class=\"previewContent\"></p></div></div><div class=\"metrics-panel\"><ul class=\"altmetric-score true\"><li><span>367</span>Views</li><li><span>7</span>CrossRef citations</li><li class=\"value\" data=\"10.1080/03115518.2014.843145\">Altmetric</li></ul></div><span class=\"access-icon free\"></span></div></td></tr></tbody></table><table class=\"articleEntry\" width=\"100%\" border=\"0\"><tbody><tr><td valign=\"top\"><div class=\"tocArticleEntry include-metrics-panel toc-citation\"><div class=\"citation-item-checkbox-container\"><label tabindex=\"0\" class=\"checkbox--primary\"><input type=\"checkbox\" name=\"10.1080/03115518.2014.843376\"><span class=\"box-btn\"></span></label></div><div class=\"article-type\">Article</div><div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1080/03115518.2014.843376\"><span class=\"hlFld-Title\">Two new species of <i>Nevania</i> (Hymenoptera: Evanioidea: Praeaulacidae: Nevaniinae) from the Middle Jurassic of China</span></a></div><div class=\"tocentryright\"><div class=\"tocAuthors afterTitle\"><div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\"><span><a href=\"/author/Li%2C+Long-Feng\">Long-Feng Li</a></span>, <span><a href=\"/author/Shih%2C+Chung-Kun\">Chung-Kun Shih</a></span> &amp; <span><a href=\"/author/Ren%2C+Dong\">Dong Ren</a></span></span></div></div><div class=\"tocPageRange maintextleft\">Pages: 140-147</div><div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online:</strong> 14 Oct 2013</span></div></div><div class=\"sfxLinkButton\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1080/03115518.2014.843376&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dliteratum%3Atandf%26id%3Ddoi%3A10.1080%2F03115518.2014.843376\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/98617/sfxbutton\" alt=\"OpenURL Stanford University\"></a></div><div class=\"tocDeliverFormatsLinks\"><a href=\"/doi/abs/10.1080/03115518.2014.843376\">Abstract</a> | <a class=\"ref nowrap full\" href=\"/doi/full/10.1080/03115518.2014.843376\">Full Text</a> | <a class=\"ref nowrap references\" href=\"/doi/ref/10.1080/03115518.2014.843376\">References</a> | <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1080/03115518.2014.843376\">PDF (881 KB)</a> | <a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D140%26pageCount%3D8%26author%3DLong-Feng%2BLi%252C%2B%252C%2BChung-Kun%2BShih%252C%2Bet%2Bal%26orderBeanReset%3Dtrue%26imprint%3DTaylor%2B%2526%2BFrancis%26volumeNum%3D38%26issueNum%3D1%26contentID%3D10.1080%252F03115518.2014.843376%26title%3DTwo%2Bnew%2Bspecies%2Bof%2BNevania%2B%2528Hymenoptera%253A%2BEvanioidea%253A%2BPraeaulacidae%253A%2BNevaniinae%2529%2Bfrom%2Bthe%2BMiddle%2BJurassic%2Bof%2BChina%26numPages%3D8%26pa%3D%26issn%3D0311-5518%26publisherName%3Dtandfuk%26publication%3DTALC%26rpt%3Dn%26endPage%3D147%26publicationDate%3D01%252F02%252F2014\" class=\"rightslink\" target=\"_blank\" title=\"Opens new window\">Permissions</a>&nbsp;<div id=\"Abs031155182014843376\" class=\"previewViewSection tocPreview\"><div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1080/03115518.2014.843376', 'Abs031155182014843376');\"></div><p class=\"previewContent\"></p></div></div><div class=\"metrics-panel\"><ul class=\"altmetric-score true\"><li><span>140</span>Views</li><li><span>7</span>CrossRef citations</li><li class=\"value\" data=\"10.1080/03115518.2014.843376\">Altmetric</li></ul></div><span class=\"access-icon free\"></span></div></td></tr></tbody></table><table class=\"articleEntry\" width=\"100%\" border=\"0\"><tbody><tr><td valign=\"top\"><div class=\"tocArticleEntry include-metrics-panel toc-citation\"><div class=\"citation-item-checkbox-container\"><label tabindex=\"0\" class=\"checkbox--primary\"><input type=\"checkbox\" name=\"10.1080/03115518.2014.843381\"><span class=\"box-btn\"></span></label></div><div class=\"article-type\">Article</div><div class=\"art_title linkable\"><a class=\"ref nowrap\" href=\"/doi/full/10.1080/03115518.2014.843381\"><span class=\"hlFld-Title\">Associated conchs and opercula of <i>Triplicatella disdoma</i> (Hyolitha) from the early Cambrian of South Australia</span></a></div><div class=\"tocentryright\"><div class=\"tocAuthors afterTitle\"><div class=\"articleEntryAuthor all\"><span class=\"articleEntryAuthorsLinks\"><span><a href=\"/author/Skovsted%2C+Christian+B\">Christian B. Skovsted</a></span>, <span><a href=\"/author/Topper%2C+Timothy+P\">Timothy P. Topper</a></span>, <span><a href=\"/author/Betts%2C+Marissa+J\">Marissa J. Betts</a></span> &amp; <span><a href=\"/author/Brock%2C+Glenn+A\">Glenn A. Brock</a></span></span></div></div><div class=\"tocPageRange maintextleft\">Pages: 148-153</div><div class=\"tocEPubDate\"><span class=\"maintextleft\"><strong>Published online:</strong> 20 Jan 2014</span></div></div><div class=\"sfxLinkButton\"><a href=\"/servlet/linkout?suffix=s0&amp;dbid=16384&amp;type=tocOpenUrl&amp;doi=10.1080/03115518.2014.843381&amp;url=http%3A%2F%2Flibrary.stanford.edu%2Fsfx%3Fsid%3Dliteratum%3Atandf%26id%3Ddoi%3A10.1080%2F03115518.2014.843381\" title=\"OpenURL Stanford University\" onclick=\"newWindow(this.href);return false\" class=\"sfxLink\"><img src=\"/userimages/98617/sfxbutton\" alt=\"OpenURL Stanford University\"></a></div><div class=\"tocDeliverFormatsLinks\"><a href=\"/doi/abs/10.1080/03115518.2014.843381\">Abstract</a> | <a class=\"ref nowrap full\" href=\"/doi/full/10.1080/03115518.2014.843381\">Full Text</a> | <a class=\"ref nowrap references\" href=\"/doi/ref/10.1080/03115518.2014.843381\">References</a> | <a class=\"ref nowrap pdf\" target=\"_blank\" title=\"Opens new window\" href=\"/doi/pdf/10.1080/03115518.2014.843381\">PDF (674 KB)</a> | <a href=\"/servlet/linkout?type=rightslink&amp;url=startPage%3D148%26pageCount%3D6%26author%3DChristian%2BB.%2BSkovsted%252C%2B%252C%2BTimothy%2BP.%2BTopper%252C%2Bet%2Bal%26orderBeanReset%3Dtrue%26imprint%3DTaylor%2B%2526%2BFrancis%26volumeNum%3D38%26issueNum%3D1%26contentID%3D10.1080%252F03115518.2014.843381%26title%3DAssociated%2Bconchs%2Band%2Bopercula%2Bof%2BTriplicatella%2Bdisdoma%2B%2528Hyolitha%2529%2Bfrom%2Bthe%2Bearly%2BCambrian%2Bof%2BSouth%2BAustralia%26numPages%3D6%26pa%3D%26issn%3D0311-5518%26publisherName%3Dtandfuk%26publication%3DTALC%26rpt%3Dn%26endPage%3D153%26publicationDate%3D01%252F02%252F2014\" class=\"rightslink\" target=\"_blank\" title=\"Opens new window\">Permissions</a>&nbsp;<div id=\"Abs031155182014843381\" class=\"previewViewSection tocPreview\"><div class=\"closeButton\" onclick=\"showHideTocPublicationAbs('10.1080/03115518.2014.843381', 'Abs031155182014843381');\"></div><p class=\"previewContent\"></p></div></div><div class=\"metrics-panel\"><ul class=\"altmetric-score true\"><li><span>124</span>Views</li><li><span>8</span>CrossRef citations</li><li class=\"value\" data=\"10.1080/03115518.2014.843381\">Altmetric</li></ul></div><span class=\"access-icon free\"></span></div></td></tr></tbody></table>\n" +
        "</div>\n" +
        "<input type=\"hidden\" name=\"href\" value=\"/toc/talc20/38/1\">\n" +
        "<input type=\"hidden\" name=\"title\" value=\"Alcheringa: An Australasian Journal of Palaeontology (2014)\">\n" +
        "</form>\n" +
        "</fieldset>\n" +
        "<div class=\"altmetric-Key hidden\" data=\"be0ef6915d1b2200a248b7195d01ef22\"></div></div>\n" +
        "</div>\n" +
        "</div>";

  /*
   *  Compare Html and HtmlHashFiltered
   */
  public void testWithPrevNext() throws Exception {
    String filteredwithPrevNext = filterString(withPrevNext);
    assertEquals(withoutPrevNext, filteredwithPrevNext);
  }
  
  public void testManifest() throws Exception {
    String filteredmanifest = filterString(manifest);
    assertEquals(manifestFiltered, filteredmanifest);
  }
  
  public void testWithTocLink() throws Exception {
    String filteredwithTocLink = filterString(withTocLink);
    assertEquals(withoutTocLink, filteredwithTocLink);
  }
  
  public void testPeopleAlsoRead() throws Exception {
    String filteredwithPeopleAlsoRead = filterString(withPeopleAlsoRead);
    //System.out.println("[" + StringUtil.fromInputStream(actIn) + "]");
    assertEquals(withoutPeopleAlsoRead, filteredwithPeopleAlsoRead);
  }
  
  public void testArticleMetrics() throws Exception {
    String filteredwithArticleMetrics = filterString(withArticleMetrics);
    assertEquals(withoutArticleMetrics, filteredwithArticleMetrics);
  }
 
  public void testTocArticleEntry() throws Exception {
    String filteredwithTocArticleEntry = filterString(withTocArticleEntry);
    assertEquals(withoutTocArticleEntry, filteredwithTocArticleEntry);
  }

  public void testIortArticleOldNew() throws Exception {
    // this tests whether an old html document crawl in 2016 will hash compare to the "same" article from 2021
    // the html documents are Open Access
    String art1 = getFileAsString("ARTICLE17453670810016597.2021.html");
    String art2 = getFileAsString("ARTICLE17453670810016597.2016.html");
    //log.info(StringUtils.difference(lstr,istr));
    assertEquals(art1, art2);

  }

  public void testTaclTocOldNew() throws Exception {
    // this tests whether an old html document crawl in 2016 will hash compare to the "same" TOC from 2021
    // the html documents are Open Access
    String art1 = getFileAsString("TOCtacl20384.2019.html");
    String art2 = getFileAsString("TOCtacl20384.2016.html");
    assertEquals(art1, art2);
  }
}
