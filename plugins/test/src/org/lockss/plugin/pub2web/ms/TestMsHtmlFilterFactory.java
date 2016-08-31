/*
 * $Id:$
 */
package org.lockss.plugin.pub2web.ms;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestMsHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private MsHtmlCrawlFilterFactory cfact;
  private MsHtmlHashFilterFactory hfact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    cfact = new MsHtmlCrawlFilterFactory();
    hfact = new MsHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String toc_bit = "";
  private static final String toc_bit_hashfiltered = "";
  private static final String toc_bit_crawlfiltered = "";

  private static final String article_bit = "";
  private static final String article_bit_hashfiltered = "";
  private static final String article_bit_crawlfiltered = "";
  
  private static final String mainContent =
      "<main class=\"col-xs-12 col-sm-12 col-md-9 content main-content-container\">" +
          "<ul class=\"togglecontent flat\">" +
          "<h1>CLOCKSS system has permission to ingest, preserve, and serve this Archival Unit</h1>" +
          "<li>" +
          "<span class=\"access_icon_s keyicon accesskey-icon\" title=\"SUBSCRIBED CONTENT\">s</span>" +
          "<a href=\"/content/journal/jmm/64/12\" title=\"\" ><span class=\"issuenumber\">Issue 12</span><span class=\"issueyear\">, " +
          "December" +
          "</span></a>" +
          "</li>" +
          "</ul>" +
          "</main>";
  
  
  private static final String relatedcontentlist =
      "<div id=\"relatedcontent\" class=\"hidden-js-toggle related-content\">" +
          "<div class=\"morelikethiscontainer\">" +
          "<div class=\"hiddenmorelikethisids hidden-js-div\"></div>" +
          "<div class=\"hiddenmorelikethiswebid hidden-js-div\">/content/journal/jmm/10.1099/foo</div>" +
          "<div class=\"hiddenmorelikethisfields hidden-js-div\">dcterms_title,dcterms_subject,pub_serialTitle</div>" +
          "<div class=\"hiddenmorelikethisrestrictions hidden-js-div\">pub_serialIdent:journal/jmm AND -contentType:BlogPost</div>" +
          "<div class=\"hiddenmorelikethisnumber hidden-js-div\">6</div>" +
          "<div class=\"hiddenmorelikethisnumbershown hidden-js-div\">4</div>" +
          "<i class=\"fa fa-spinner fa-spin\"></i>" +
          "</div> ";
  private static final String otherjournallist =
      "<div id=\"otherJournals\" class=\"hidden-js-toggle related-content\">" +
          "<div class=\"morelikethiscontainer\">" +
          "<div class=\"hiddenmorelikethisids hidden-js-div\"></div>" +
          "<div class=\"hiddenmorelikethiswebid hidden-js-div\">/content/journal/jmm/10.1099/foo</div>" +
          "<div class=\"hiddenmorelikethisfields hidden-js-div\">dcterms_title,dcterms_subject</div>" +
          "<div class=\"hiddenmorelikethisrestrictions hidden-js-div\">-pub_serialIdent:journal/jmm AND -contentType:BlogPost</div>" +
          "<div class=\"hiddenmorelikethisnumber hidden-js-div\">6</div>" +
          "<div class=\"hiddenmorelikethisnumbershown hidden-js-div\">4</div>" +
          "<i class=\"fa fa-spinner fa-spin\"></i>" +
          "</div> ";

  public void testMainComposite() throws Exception {
    InputStream inStream;
 
    //hash-filter
    inStream = hfact.createFilteredInputStream(mau,
        new StringInputStream(mainContent),
        Constants.DEFAULT_ENCODING);
    assertEquals(mainContent, StringUtil.fromInputStream(inStream));

  }

  public void testTOCFiltering() throws Exception {
    InputStream inStream;
    //crawl-filter
    inStream = cfact.createFilteredInputStream(mau,
        new StringInputStream(toc_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(toc_bit_crawlfiltered, StringUtil.fromInputStream(inStream));
    //hash-filter
    inStream = hfact.createFilteredInputStream(mau,
        new StringInputStream(toc_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(toc_bit_hashfiltered, StringUtil.fromInputStream(inStream));

  }

  public void testArticleLandingFiltering() throws Exception {
    InputStream inStream;
    //crawl-filter
    inStream = cfact.createFilteredInputStream(mau,
        new StringInputStream(article_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(article_bit_crawlfiltered, StringUtil.fromInputStream(inStream));
    //hash-filter
    inStream = hfact.createFilteredInputStream(mau,
        new StringInputStream(article_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(article_bit_hashfiltered, StringUtil.fromInputStream(inStream));

  }
  
  public void testTabContents() throws Exception {
    InputStream inStream;

    //hash-filter
    inStream = hfact.createFilteredInputStream(mau,
        new StringInputStream(relatedcontentlist),
        Constants.DEFAULT_ENCODING);
    assertEquals("", StringUtil.fromInputStream(inStream));
    inStream = hfact.createFilteredInputStream(mau,
        new StringInputStream(otherjournallist),
        Constants.DEFAULT_ENCODING);
    assertEquals("", StringUtil.fromInputStream(inStream));
  }
}
