/*
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

package org.lockss.plugin.springer.link;

import java.io.*;
import java.util.Iterator;
import java.util.List;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataListExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.CachedUrl;
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
