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

package org.lockss.plugin.springer.link;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestSpringerLinkHtmlHashFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private SpringerLinkHtmlHashFilterFactory fact ;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new SpringerLinkHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }
  // this example is from an article
  private static final String HtmlHashA =
    "<div class=\"c-article-header\">" +
    "<header>" +
    "<h1 class=\"c-article-title\" data-test=\"article-title\" data-article-title=\"\" itemprop=\"name headline\">Very Fancy Title</h1>" +
    "<p lang=\"en\">A user-friendly staged concept. English version</p>" +
    "</div>" +
    "</header>" ;
 
  private static final String HtmlHashAFiltered =
    "<h1 class=\"c-article-title\" data-test=\"article-title\" data-article-title=\"\" itemprop=\"name headline\">Very Fancy Title</h1>" +
    "<p lang=\"en\">A user-friendly staged concept. English version</p>" ;


  private static final String articleHeader =
    "<aside class=\"c-ad c-ad--728x90\" data-test=\"springer-doubleclick-ad\">" +
    "  <div class=\"c-ad__inner\">" +
    "    <p class=\"c-ad__label\">Advertisement</p>" +
    "    <div id=\"div-gpt-ad-LB1\" data-gpt-unitpath=\"/270604982/springerlink/40554/article\" data-gpt-sizes=\"728x90\" data-gpt-targeting=\"pos=LB1;articleid=s40554-018-0065-9;\"></div>" +
    "  </div>" +
    "</aside>" +
    "<div class=\"c-article-header\">" +
    "  <header>" +
    "    <ul class=\"c-article-identifiers\" data-test=\"article-identifier\">" +
    "      <li class=\"c-article-identifiers__item\" data-test=\"article-category\">Research</li>" +
    "      <li class=\"c-article-identifiers__item\"><span class=\"c-article-identifiers__open\" data-test=\"open-access\">Open Access</span></li>" +
    "      <li class=\"c-article-identifiers__item\"><a href=\"#article-info\" data-track=\"click\" data-track-action=\"publication date\" data-track-label=\"link\">Published: <time datetime=\"2019-01-09\" itemprop=\"datePublished\">09 January 2019</time></a></li>" +
    "    </ul>" +
    "    <h1 class=\"c-article-title\" data-test=\"article-title\" data-article-title=\"\" itemprop=\"name headline\">Embodied meaning: a systemic functional perspective on paralanguage</h1>" +
    "    <p class=\"c-article-info-details\" data-container-section=\"info\"><a data-test=\"journal-link\" href=\"/journal/40554\"><i data-test=\"journal-title\">Functional Linguistics</i></a><b data-test=\"journal-volume\"><span class=\"u-visually-hidden\">volume</span>&nbsp;6</b>, Article&nbsp;number:&nbsp;<span data-test=\"article-number\">1</span> (<span data-test=\"article-publication-year\">2019</span>)<a href=\"#citeas\" class=\"c-article-info-details__cite-as u-hide-print\" data-track=\"click\" data-track-action=\"cite this article\" data-track-label=\"link\">Cite this article</a></p>" +
    "    <div data-test=\"article-metrics\">" +
    "      <div id=\"altmetric-container\">" +
    "        <div class=\"c-article-metrics-bar__wrapper u-clear-both\">" +
    "          <ul class=\"c-article-metrics-bar u-list-reset\">" +
    "            <li class=\" c-article-metrics-bar__item\">" +
    "              <p class=\"c-article-metrics-bar__count\">4856 <span class=\"c-article-metrics-bar__label\">Accesses</span></p>" +
    "            </li>" +
    "            <li class=\"c-article-metrics-bar__item\">" +
    "              <p class=\"c-article-metrics-bar__count\">4 <span class=\"c-article-metrics-bar__label\">Citations</span></p>" +
    "            </li>" +
    "            <li class=\"c-article-metrics-bar__item\">" +
    "              <p class=\"c-article-metrics-bar__count\">1 <span class=\"c-article-metrics-bar__label\">Altmetric</span></p>" +
    "            </li>" +
    "            <li class=\"c-article-metrics-bar__item\">" +
    "              <p class=\"c-article-metrics-bar__details\"><a href=\"/article/10.1186%2Fs40554-018-0065-9/metrics\" data-track=\"click\" data-track-action=\"view metrics\" data-track-label=\"link\" rel=\"nofollow\">Metrics <span class=\"u-visually-hidden\">details</span></a></p>" +
    "            </li>" +
    "          </ul>" +
    "        </div>" +
    "      </div>" +
    "    </div>" +
    "  </header>" +
    "</div>";

  private static final String filteredArticleHeader =
    "<h1 class=\"c-article-title\" data-test=\"article-title\" data-article-title=\"\" itemprop=\"name headline\">Embodied meaning: a systemic functional perspective on paralanguage</h1>" +
    "<p class=\"c-article-info-details\" data-container-section=\"info\"><a data-test=\"journal-link\" href=\"/journal/40554\"><i data-test=\"journal-title\">Functional Linguistics</i></a><b data-test=\"journal-volume\"><span class=\"u-visually-hidden\">volume</span>&nbsp;6</b>, Article&nbsp;number:&nbsp;<span data-test=\"article-number\">1</span> (<span data-test=\"article-publication-year\">2019</span>)<a href=\"#citeas\" class=\"c-article-info-details__cite-as u-hide-print\" data-track=\"click\" data-track-action=\"cite this article\" data-track-label=\"link\">Cite this article</a></p>";

  private static final String pTagWRights =
    "<h1 class=\"c-article-title\" data-test=\"article-title\" data-article-title=\"\" itemprop=\"name headline\">Editorial Introduction</h1>" +
    "<p class=\"c-article-rights\">" +
    "<a data-track=\"click\" data-track-action=\"view rights and permissions\" data-track-label=\"link\" href=\"https://s100.copyright.com/AppDispatchServlet?title=Editorial%20Introduction&amp;author=Robert%20G.%20Resta&amp;contentID=10.1023%2FA%3A1022814018694&amp;copyright=National%20Society%20of%20Genetic%20Counselors%2C%20Inc.&amp;publication=1059-7700&amp;publicationDate=1999-02&amp;publisherName=SpringerNature&amp;orderBeanReset=true\">" +
    "Reprints and Permissions</a>" +
    "</p>";

  private static final String filteredPTagWRights =
    "<h1 class=\"c-article-title\" data-test=\"article-title\" data-article-title=\"\" itemprop=\"name headline\">Editorial Introduction</h1>";

  private static final String pTagWAccess =
    "<p>" +
    "<i>The federal Office of Disease Prevention and Health Promotion (U.S. Public Health Service) has again prepared a compilation of goals and objectives for the health of the Nation's people, applicable to anticipated achievements within the decade ending in the year 2000. This effort involved work groups from numerous federal agencies, testimony in regional hearings from 800 citizens, and enrollment of a consortium of 300 national organizations. The final product, Healthy People 2000, was released in September 1990, and contains 298 measurable objectives. Content regarding children, genetic disorders, developmental disabilities, and disabilities in general is less featured and less specific than was hoped for. Ten objectives of relatively direct concern to geneticists are discussed, plus 23 others of interest in the maternal and child health field. On balance, it is suggested that the Healthy People 2000 objectives can be useful and stimulating for workers in genetics, child health care, and disabilities' services. Regrettably, there was no simultaneous proposal of legislation that would provide new federal programs, funding, or assistance to states</i>" +
    "</p>" +
    "<p class=\"c-article-access-provider__text\">" +
    "Access provided by " +
    "<span class=\"js-institution-name\">Stanford University</span>" +
    "</p>" +
    "<p class=\"c-article-access-provider__text\"" +
    "<a href=\"/content/pdf/10.1007/BF00962914.pdf\" target=\"_blank\" rel=\"noopener\" data-track=\"click\" data-track-action=\"download pdf\" data-track-label=\"inline link\">" +
    "Download</a>";

  private static final String filteredPTagWAccess =
    "<p>" +
    "<i>The federal Office of Disease Prevention and Health Promotion (U.S. Public Health Service) has again prepared a compilation of goals and objectives for the health of the Nation's people, applicable to anticipated achievements within the decade ending in the year 2000. This effort involved work groups from numerous federal agencies, testimony in regional hearings from 800 citizens, and enrollment of a consortium of 300 national organizations. The final product, Healthy People 2000, was released in September 1990, and contains 298 measurable objectives. Content regarding children, genetic disorders, developmental disabilities, and disabilities in general is less featured and less specific than was hoped for. Ten objectives of relatively direct concern to geneticists are discussed, plus 23 others of interest in the maternal and child health field. On balance, it is suggested that the Healthy People 2000 objectives can be useful and stimulating for workers in genetics, child health care, and disabilities' services. Regrettably, there was no simultaneous proposal of legislation that would provide new federal programs, funding, or assistance to states</i>" +
    "</p>";

  private static final String changingATagAttr =
    "<p>" +
    "<a>A Psycho-Educational Intervention for People with a Family History of Depression: Pilot Results</a>" +
    "</p>" +
    "<p>Mental illness jar model illustration adapted from Peay and Austin (" +
    "<a data-track=\"click\" data-track-action=\"reference anchor\" data-track-label=\"link\" data-test=\"citation-ref\" aria-label=\"Reference 2011\" title=\"Peay, H., &amp; Austin, J. (2011). How to talk with families about genetics and psychiatric illness. New York: W.W. Norton Company Inc..\" " +
    "href=\"/article/10.1007/s10897-016-0011-5#ref-CR29\" id=\"ref-link-section-d52245e910\">" +
    "2011</a>" +
    ")</p>" +
    "<p>Blaise Pascal, Pensees</p>" +
    "<p>" +
    "Huntington s disease, an autosomal-dominant neurodegenerative disorder, manifests as choreiform movements, cognitive changes, dystonia, mood and behavioral changes (Ross et al. <a data-track=\"click\" data-track-action=\"reference anchor\" data-track-label=\"link\" data-test=\"citation-ref\" aria-label=\"Reference 2014\" title=\"Ross, C. A., Aylward, E. H., Wild, E. J., Langbehn, D. R., Long, J. D., Warner, J. H., et al. (2014). Huntington disease: natural history, biomarkers and prospects for therapeutics. Nature Reviews Neurology, 10, 204 216.\" href=\"/article/10.1007/s10897-016-0007-1#ref-CR8\" id=\"ref-link-section-d54510e386\">" +
    " 2014</a>" +
    "<a data-track=\"click\" data-track-action=\"reference anchor\" data-track-label=\"link\" data-test=\"citation-ref\" aria-label=\"Reference 2007\" title=\"Walker, F. O. (2007). Huntington s disease. Lancet, 369, 218 228.\" href=\"/article/10.1007/s10897-016-0007-1#ref-CR11\" id=\"ref-link-section-d54510e389\">" +
    "2007</a>" +
    " ). The discovery of the <i>" +
    " HD</i>" +
    " gene in 1993 led to advances in detecting pathognomonic CAG expansions responsible for phenotypic manifestations of illness (The Huntington s Disease Collaborative Research Group <a data-track=\"click\" data-track-action=\"reference anchor\" data-track-label=\"link\" data-test=\"citation-ref\" aria-label=\"Reference 1993\" title=\"The Huntington s Disease Collaborative Research Group. (1993). A novel gene containing a trinucleotide repeat that is expanded and unstable on Huntington s disease chromosomes. Cell, 72, 971 983.\" href=\"/article/10.1007/s10897-016-0007-1#ref-CR10\" id=\"ref-link-section-d54510e395\">" +
    " 1993</a>" +
    "<p>" +
    "Interpreting and prioritizing core ethical principles (such as autonomy, beneficence, nonmaleficence and justice) in the face of difficult clinical scenarios is often not straightforward. This is particularly so with vulnerable populations such as children or, as this case illustrates, those with intellectual disability.</p>" +
    "<p>" ;

  private static final String filteredChangingATagAttr =
    "<p>" +
    "<a>A Psycho-Educational Intervention for People with a Family History of Depression: Pilot Results</a>" +
    "</p>" +
    "<p>Mental illness jar model illustration adapted from Peay and Austin (" +
    "<a data-track=\"click\" data-track-action=\"reference anchor\" data-track-label=\"link\" data-test=\"citation-ref\" aria-label=\"Reference 2011\" title=\"Peay, H., &amp; Austin, J. (2011). How to talk with families about genetics and psychiatric illness. New York: W.W. Norton Company Inc..\" " +
    "href=\"/article/10.1007/s10897-016-0011-5#ref-CR29\" >" +
    "2011</a>" +
    ")</p>" +
    "<p>Blaise Pascal, Pensees</p>" +
    "<p>" +
    "Huntington s disease, an autosomal-dominant neurodegenerative disorder, manifests as choreiform movements, cognitive changes, dystonia, mood and behavioral changes (Ross et al. <a data-track=\"click\" data-track-action=\"reference anchor\" data-track-label=\"link\" data-test=\"citation-ref\" aria-label=\"Reference 2014\" title=\"Ross, C. A., Aylward, E. H., Wild, E. J., Langbehn, D. R., Long, J. D., Warner, J. H., et al. (2014). Huntington disease: natural history, biomarkers and prospects for therapeutics. Nature Reviews Neurology, 10, 204 216.\" href=\"/article/10.1007/s10897-016-0007-1#ref-CR8\" >" +
    " 2014</a>" +
    "<a data-track=\"click\" data-track-action=\"reference anchor\" data-track-label=\"link\" data-test=\"citation-ref\" aria-label=\"Reference 2007\" title=\"Walker, F. O. (2007). Huntington s disease. Lancet, 369, 218 228.\" href=\"/article/10.1007/s10897-016-0007-1#ref-CR11\" >" +
    "2007</a>" +
    " ). The discovery of the <i>" +
    " HD</i>" +
    " gene in 1993 led to advances in detecting pathognomonic CAG expansions responsible for phenotypic manifestations of illness (The Huntington s Disease Collaborative Research Group <a data-track=\"click\" data-track-action=\"reference anchor\" data-track-label=\"link\" data-test=\"citation-ref\" aria-label=\"Reference 1993\" title=\"The Huntington s Disease Collaborative Research Group. (1993). A novel gene containing a trinucleotide repeat that is expanded and unstable on Huntington s disease chromosomes. Cell, 72, 971 983.\" href=\"/article/10.1007/s10897-016-0007-1#ref-CR10\" >" +
    " 1993</a>" +
    "<p>" +
    "Interpreting and prioritizing core ethical principles (such as autonomy, beneficence, nonmaleficence and justice) in the face of difficult clinical scenarios is often not straightforward. This is particularly so with vulnerable populations such as children or, as this case illustrates, those with intellectual disability.</p>" +
    "<p>" ;
  /*
    "<p>" +
    "<a>A Psycho-Educational Intervention for People with a Family History of Depression: Pilot Results</a>" +
    "</p>" +
    "<p>Mental illness jar model illustration adapted from Peay and Austin (" +
    ")</p>" +
    "<p>Blaise Pascal, Pensees</p>" +
    "<p>" +
    "Huntington s disease, an autosomal-dominant neurodegenerative disorder, manifests as choreiform movements, cognitive changes, dystonia, mood and behavioral changes (Ross et al." +
    " ). The discovery of the <i>" +
    " HD</i>" +
    " gene in 1993 led to advances in detecting pathognomonic CAG expansions responsible for phenotypic manifestations of illness (The Huntington s Disease Collaborative Research Group " +
    "<p>" +
    "Interpreting and prioritizing core ethical principles (such as autonomy, beneficence, nonmaleficence and justice) in the face of difficult clinical scenarios is often not straightforward. This is particularly so with vulnerable populations such as children or, as this case illustrates, those with intellectual disability.</p>" +
    "<p>" ;
*/
  private static final String problemPTag =
    "<p>AllThats Left" +
    "</p>" +
    "<p class=\"mb0\" data-component='SpringerLinkArticleCollections'>" +
    "We re sorry, something doesn't seem to be working properly.</p>" +
    "<p>" +
    " Please try refreshing the page. If that doesn't work, please contact support so we can address the problem.</p>";

  private static final String filteredProblemPTag =
    "<p>AllThats Left" +
    "</p>";

  private static final String pTagArticleAccessText =
    "<p class=\"c-article-access-provider__text\">\n" +
    "      Access provided by <span class=\"js-institution-name\">\n" +
    "      Stanford University</span>\n" +
    "</p>\n" +
    "<p class=\"c-article-access-provider__text\">\n" +
    "<a href=\"/content/pdf/10.1023/A:1013828717421.pdf\" target=\"_blank\" rel=\"noopener\" data-track=\"click\" data-track-action=\"download pdf\" data-track-label=\"inline link\">\n" +
    "Download</a></p>";

  private static final String filteredPTagArticleAccessText = "";

  private static final String aTagWBadHref =
    "<p class=\"c-article-rights\">" +
    "<a data-track=\"click\" data-track-action=\"view rights and permissions\" data-track-label=\"link\" href=\"https://s100.copyright.com/AppDispatchServlet?title=Book%20Review%3A%20The%20Genetic%20Testing%20of%20Children.%20By%20Angus%20Clarke%20%28ed.%29.%20Bios%20Scientific%20Publishers%2C%20Oxford%2C%20UK%2C%201998%2C%20334%20pp.%20%28hardback%29&amp;author=Verle%20Headings&amp;contentID=10.1023%2FA%3A1022942618101&amp;publication=1059-7700&amp;publicationDate=1999-10&amp;publisherName=SpringerNature&amp;orderBeanReset=true\">" +
    "Reprints and Permissions</a>" +
    "</p>";

  private static final String filteredATagWBadHref = "";

  // this example is a little different; it's from an article
  /*
  private static final String HtmlHashB =
    "<table width=\"186\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"+
    "<tr> "+
    "<td class=\"texttah11\" width=\"184\">"+
    "<table class=\"texttah11\" width=\"177\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"+
    "<tbody>" + "<tr>"+
    "<td valign=\"top\" width=\"13\"><img src=\"img/kv.gif\" vspace=\"4\" width=\"5\" height=\"5\"></td>"+
    "<td width=\"165\" class=\"green\">Viewed by : <span class=more3 >1106</span></td>"+
    "</tr>"+
    "</tbody> </table> </td> </tr> </table>Hello World";
  private static final String HtmlHashBFiltered =
    "Hello World";
  private static final String HtmlHashC =
    "<p><!--Load time 0.240586 seconds.-->Hello World</p>"+
    "<!--SESID=\"020254c9122ebd1bf1f37e24b639181d\"-->";
  private static final String HtmlHashCFiltered =
    "<p>Hello World</p>";
  private static final String HtmlHashD =
    "<a class=\"antetka\" href='journal_home_page.php?journal_id=1&page=taxon&SESID=020254c9122ebd1bf1f37e24b639181d'>Taxon</a>"+
    "<a class=\"green\" href=\"journals/HelloWorld\">Hello World</a>";
  private static final String HtmlHashDFiltered =
    "<a class=\"green\" href=\"journals/HelloWorld\">Hello World</a>";
  private static final String HtmlHashE =
    "<input type=\"hidden\" name=\"SESID\" value=\"020254c9122ebd1bf1f37e24b639181d\">Hello World";
  private static final String HtmlHashEFiltered = "Hello World";
  private static final String HtmlHashF =
    "<iframe target=basketadd src =\"/none.php?SESID=754755beffdf3915cf0ea0ff54719eeb\""+
    "name=basketadd width=\"0\" scrolling=\"no\" frameborder=\"0\" height=\"0\">\""+
    "</iframe>Hello World";
  private static final String HtmlHashG = "<tr height=19 bgcolor=\"#f0f0E0\" onmouseover=\"this.style.backgroundColor='#fefef5';style.cursor='hand';\""+
    "onclick=\"document.location.href='journal_home_page.php?journal_id=2&page=home&SESID=020254c9122ebd1bf1f37e24b639181d';\""+
    "onmouseout=\"this.style.backgroundColor='#f0f0E0';\"></tr>Hello World";
  private static final String HtmlHashH = "<a href=\"javascript:void(0);\" "+
    "onclick=\"displayMessage2('login-form.php?SESID=020254c9122ebd1bf1f37e24b639181d',604,348);return false;\" class=\"menu\">Email/RSS Alerts</a>"+
    "Hello World";
  private static final String HtmlHashI = "<script type=\"text/javascript\">"+
    "Not Included"+"</script>Hello World";
  private static final String HtmlHashJ = "<noscript>"+
    "Not Included"+"</noscript>Hello World"; 
  private static final String HtmlHashK = "<td width=\"186\" valign=\"top\" class=textver10>"+
    "Not Included"+"</td>Hello World";
  private static final String HtmlHashL = "<table width=\"186\">"+
    "Not Included"+"</table>Hello World";

  private static final String HtmlHashN = 
    "<td class=\"green2\" valign=\"top\"><b>doi: "+
    "</b>10.3897/biorisk.7.1969<br><b>Published:</b> 17.10.2012"+
    "<br /><br /><b>Viewed by: </b>3424"+
    "<td class=\"more3\">Hello World </td>";
  private static final String HtmlHashNFiltered = "<td class=\"more3\">Hello World </td>";
  private static final String HtmlHashO = "<td align=center><a href=\"journals/zookeys/issue/341/\" class=more3>Current Issue</a></td>"+
    "Hello World";
  private static final String HtmlHashP = "<td align=\"left\" class=\"texttah11\" width=\"200px\"></td>"+
        "<td align=\"left\" class=\"texttah11\">Pages:&nbsp;1-20&nbsp;| Viewed by:&nbsp;2128</td>";
  private static final String HtmlHashPFiltered = "<td align=\"left\" class=\"texttah11\" width=\"200px\"></td>";
*/

  public void testFilterA() throws Exception {
    InputStream inA;

    // viewed-by test 
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(HtmlHashA), ENC);
    String filtStrA = StringUtil.fromInputStream(inA);
    assertEquals(HtmlHashAFiltered, filtStrA);
  }

  public void testFilterHeader() throws Exception {
    InputStream inA;
    // viewed-by test
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(articleHeader), ENC);
    assertEquals(filteredArticleHeader, StringUtil.fromInputStream(inA));
  }

  public void testPTagRights() throws Exception {
    InputStream inA;
    // viewed-by test
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(pTagWRights), ENC);
    assertEquals(filteredPTagWRights, StringUtil.fromInputStream(inA));
  }

  public void testPTagAccess() throws Exception {
    InputStream inA;
    // viewed-by test
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(pTagWAccess), ENC);
    assertEquals(filteredPTagWAccess, StringUtil.fromInputStream(inA));
  }

  public void testchangingATagAttr() throws Exception {
    InputStream inA;
    // viewed-by test
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(changingATagAttr), ENC);
    assertEquals(filteredChangingATagAttr, StringUtil.fromInputStream(inA));
  }

  public void testProblemPTag() throws Exception {
    InputStream inA;
    // viewed-by test
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(problemPTag), ENC);
    assertEquals(filteredProblemPTag, StringUtil.fromInputStream(inA));
  }

  public void testPTagArticleAccessText() throws Exception {
    InputStream inA;
    // viewed-by test
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(pTagArticleAccessText), ENC);
    assertEquals(filteredPTagArticleAccessText, StringUtil.fromInputStream(inA));
  }

  public void testTagWBadHref() throws Exception {
    InputStream inA;
    // viewed-by test
    inA = fact.createFilteredInputStream(mau,
        new StringInputStream(aTagWBadHref), ENC);
    assertEquals(filteredATagWBadHref, StringUtil.fromInputStream(inA));
  }

  /*
  public void testFilterB() throws Exception {
    InputStream inB;

    inB = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashB), ENC);    
    String filtStrB = StringUtil.fromInputStream(inB);
    assertEquals(HtmlHashBFiltered, filtStrB);
   
  }
  public void testFilterC() throws Exception {
    InputStream in;

    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashC), ENC);    
    String filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashCFiltered, filtStr);
   
  }
  public void testFilterD() throws Exception {
    InputStream in;

    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashD), ENC);    
    String filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashDFiltered, filtStr);
   
  }
  public void testFilterE() throws Exception {
    InputStream in;
    // all these should match, once filtered, the string HtmlHashEFiltered
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashE), ENC);    
    String filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashF), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashG), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashH), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashI), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashJ), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashK), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashL), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashO), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashEFiltered, filtStr);

  }
  */

  /*
  public void testFilterViewedBy() throws Exception {
    InputStream in;
    String filtStr = null;
    
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashN), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashNFiltered, filtStr);
    
    in = fact.createFilteredInputStream(mau, 
        new StringInputStream(HtmlHashP), ENC);    
    filtStr = StringUtil.fromInputStream(in);
    assertEquals(HtmlHashPFiltered, filtStr);

  }
  */
  
/*
  static final String input_1 = 
      "org/lockss/plugin/pensoft/orig.html";
  static final String input_2 = 
      "org/lockss/plugin/pensoft/orig_mod.html";

  //Test using real, downloaded files to be sure to catch all issues
  public void testCase_fromFile() throws Exception {
    InputStream input1 = null;
    InputStream input2 = null;
    InputStream filtered1 = null;
    InputStream filtered2 = null;

    try {
    input1 = getClass().getClassLoader().getResourceAsStream(input_1);
    filtered1 = fact.createFilteredInputStream(mau,
        input1,Constants.DEFAULT_ENCODING);
    
    input2 = getClass().getClassLoader().getResourceAsStream(input_2);
    filtered2 = fact.createFilteredInputStream(mau,
        input2,Constants.DEFAULT_ENCODING);
    
    String s_filtered1 = StringUtil.fromInputStream(filtered1);
    String s_filtered2 = StringUtil.fromInputStream(filtered2); 
    assertEquals(s_filtered1, s_filtered2);
    } finally {
      IOUtil.safeClose(input1);
      IOUtil.safeClose(input2);
      IOUtil.safeClose(filtered1);
      IOUtil.safeClose(filtered2);
    }
  }
*/
/*  
  String realTOCFile = "test_TOC.html";
  String realABSFile = "test_viewedby.html";
  String realFullFile = "test_Full.html";
  String TOCFilteredFile = "org/lockss/plugin/pensoft/TOC_filtered.html";
  String ABSFilteredFile = "org/lockss/plugin/pensoft/ABS_filtered.html";
  String FullFilteredFile = "org/lockss/plugin/pensoft/Full_filtered.html";

  String BASE_URL = "http://pensoft.net/";
  public void testTOCFile() throws Exception {
    //CIProperties xmlHeader = new CIProperties();
    InputStream file_input = null;
    PrintStream filtered_output = null;
    try {
      file_input = getResourceAsStream(realTOCFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      InputStream inA;

      // viewed-by visual test for issue/TOC 
      inA = fact.createFilteredInputStream(mau, 
            new StringInputStream(string_input), ENC);
      String filtStrA = StringUtil.fromInputStream(inA);
      OutputStream outS = new FileOutputStream(TOCFilteredFile);
      filtered_output = new PrintStream(outS);
      filtered_output.print(filtStrA);
      IOUtil.safeClose(filtered_output);
      
    }finally {
      IOUtil.safeClose(file_input);
      IOUtil.safeClose(filtered_output);
    }

  }
  public void testABSFile() throws Exception {
    //CIProperties xmlHeader = new CIProperties();
    InputStream file_input = null;
    PrintStream filtered_output = null;
    try {
      file_input = getResourceAsStream(realABSFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      InputStream inA;

      // viewed-by visual test for abstract 
      inA = fact.createFilteredInputStream(mau, 
            new StringInputStream(string_input), ENC);
      String filtStrA = StringUtil.fromInputStream(inA);
      OutputStream outS = new FileOutputStream(ABSFilteredFile);
      filtered_output = new PrintStream(outS);
      filtered_output.print(filtStrA);
      IOUtil.safeClose(filtered_output);
      
    }finally {
      IOUtil.safeClose(file_input);
      IOUtil.safeClose(filtered_output);
    }

  }
  public void testFullFile() throws Exception {
    //CIProperties xmlHeader = new CIProperties();
    InputStream file_input = null;
    PrintStream filtered_output = null;
    try {
      file_input = getResourceAsStream(realFullFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      InputStream inA;

      // viewed-by test for full html  
      inA = fact.createFilteredInputStream(mau, 
            new StringInputStream(string_input), ENC);
      String filtStrA = StringUtil.fromInputStream(inA);
      OutputStream outS = new FileOutputStream(FullFilteredFile);
      filtered_output = new PrintStream(outS);
      filtered_output.print(filtStrA);
      IOUtil.safeClose(filtered_output);
      
    }finally {
      IOUtil.safeClose(file_input);
      IOUtil.safeClose(filtered_output);
    }

  }
  */
}
