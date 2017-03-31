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

import java.io.InputStream;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.StringInputStream;
import org.lockss.util.Constants;
import org.lockss.util.StringUtil;

public class TestTafHtmlHashFilterFactory extends LockssTestCase {
  private TafHtmlHashFilterFactory fact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    fact = new TafHtmlHashFilterFactory();
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
      " Volume 47 ";
  
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
" Hello World Hello Article ";
  
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
" Hello Kitty ";
  

  /*
   *  Compare Html and HtmlHashFiltered
   */
  public void testWithPrevNext() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withPrevNext), Constants.DEFAULT_ENCODING);
    assertEquals(withoutPrevNext, StringUtil.fromInputStream(actIn));
  }
  
  public void testManifest() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(manifest), Constants.DEFAULT_ENCODING);
    assertEquals(manifestFiltered, StringUtil.fromInputStream(actIn));
  }
  
  public void testWithTocLink() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withTocLink), Constants.DEFAULT_ENCODING);
    assertEquals(withoutTocLink, StringUtil.fromInputStream(actIn));
  }
  
  public void testPeopleAlsoRead() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withPeopleAlsoRead), Constants.DEFAULT_ENCODING);
    //System.out.println("[" + StringUtil.fromInputStream(actIn) + "]");

    assertEquals(withoutPeopleAlsoRead, StringUtil.fromInputStream(actIn));
  }
  
  public void testArticleMetrics() throws Exception {
    InputStream actIn = fact.createFilteredInputStream(mau,
        new StringInputStream(withArticleMetrics), Constants.DEFAULT_ENCODING);
    assertEquals(withoutArticleMetrics, StringUtil.fromInputStream(actIn));
  }
 
}
