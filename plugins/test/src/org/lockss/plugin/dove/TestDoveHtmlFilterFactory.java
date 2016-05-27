/*
 * $Id:$
 */
package org.lockss.plugin.dove;

import java.io.*;

import org.lockss.util.*;
import org.lockss.test.*;

public class TestDoveHtmlFilterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  private DoveHtmlCrawlFilterFactory cfact;
  private DoveHtmlHashFilterFactory hfact;
  private MockArchivalUnit mau;

  public void setUp() throws Exception {
    super.setUp();
    cfact = new DoveHtmlCrawlFilterFactory();
    hfact = new DoveHtmlHashFilterFactory();
    mau = new MockArchivalUnit();
  }

  private static final String toc_bit = 
      "<html class=\"no-js\"><!--<![endif]--><head></head>" +
          "<body>" +
          "<div role=\"main\" class=\"main\" id=\"content\">" +
          "<p class=\"back\">Back to " +
          "<a href=\"https://www.dovepress.com/browse_journals.php\">Browse Journals</a>" +
          "<a href=\"https://www.dovepress.com/the-journal\">Therapeutics and Clinical Risk Management</a>" +
          "<a href=\"https://www.dovepress.com/the-journal-archive1-v879\">Volume 11</a> default</p>" +
          "<div class=\"tabs-bg group journal-articles group\">" +
          "<div class=\"tabs-padding group\">     " +
          "<div class=\"article-labels group\">" +
          "<img src=\"tcrm_toc_files/logo_pubmed.png\" alt=\"Pub Med\" title=\"Pub Med\"> " +
          "<div class=\"highly-accessed\"></div>  " +
          "</div>" +
          "<br clear=\"all\">" +
          "<div class=\"archive\">" +
          "<ul data-journal_id=\"1\">" +
          "<li><a href=\"https://www.dovepress.com/the-journal-archive1\" class=\"\" data-volume_id=\"0\">View all</a> (1106)</li>" +
          "<li>" +
          "<a href=\"https://www.dovepress.com/the-journal-i1123-j1\" class=\"\">Volume 12, 2016</a> (91)" +
          "</li>" +
          "<li>" +
          "<a href=\"https://www.dovepress.com/the-journal-i1010-j1\" class=\"here\">Volume 11, 2015</a> (210)" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "<!-- /end categories -->" +
          "</div>" +
          "</div>" +
          "<div class=\"categories-bg group\"><div class=\"tabs-padding group\"><div class=\"tabs\">" +
          "<h4>Archive: Volume 11, 2015</h4>" +
          "</div>" +
          "<div class=\"tab-content\">" +
          "<div class=\"volume-issues issue-1010 \">      <div class=\"tab-item\">" +
          "<div class=\"article-labels group\">" +
          "<div class=\"tag\">Original Research</div>" +
          "</div>" +
          "<h3>" +
          "<a href=\"https://www.dovepress.com/foo-article-TCRM\">Foo</a>" +
          "</h3>" +
          "<p>authors</p>" +
          "<p class=\"journal\">" +
          "<a href=\"https://www.dovepress.com/journal\">This Journal</a> " +
          "<a href=\"https://www.dovepress.com/this-journal-archive1-v879\">2015</a>, 11:1853-186</p>" +
          "<p class=\"journal\">Published Date: <strong>17 December 2015</strong></p>" +
          "</div>" +
          "<!-- /end tab-item -->" +
          "</div></div>" +
          "</div></div>" +
          "</div>" +
          "<!-- /end main -->" +
          "</body></html>";
  private static final String toc_bit_hashfiltered = 
          "<div class=\"volume-issues issue-1010 \">      <div class=\"tab-item\">" +
          "<h3>" +
          "<a href=\"https://www.dovepress.com/foo-article-TCRM\">Foo</a>" +
          "</h3>" +
          "<p>authors</p>" +
          "<p class=\"journal\">" +
          "<a href=\"https://www.dovepress.com/journal\">This Journal</a> " +
          "<a href=\"https://www.dovepress.com/this-journal-archive1-v879\">2015</a>, 11:1853-186</p>" +
          "<p class=\"journal\">Published Date: <strong>17 December 2015</strong></p>" +
          "</div>" +
          "<!-- /end tab-item -->" +
          "</div>";
  private static final String toc_bit_crawlfiltered = 
      "<html class=\"no-js\"><!--<![endif]--><head></head>" +
          "<body>" +
          "<div role=\"main\" class=\"main\" id=\"content\">" +
          "<p class=\"back\">Back to " +
          "<a href=\"https://www.dovepress.com/browse_journals.php\">Browse Journals</a>" +
          "<a href=\"https://www.dovepress.com/the-journal\">Therapeutics and Clinical Risk Management</a>" +
          "<a href=\"https://www.dovepress.com/the-journal-archive1-v879\">Volume 11</a> default</p>" +
          "<div class=\"categories-bg group\"><div class=\"tabs-padding group\">" +
          "<div class=\"tab-content\">" +
          "<div class=\"volume-issues issue-1010 \">      <div class=\"tab-item\">" +
          "<div class=\"article-labels group\">" +
          "<div class=\"tag\">Original Research</div>" +
          "</div>" +
          "<h3>" +
          "<a href=\"https://www.dovepress.com/foo-article-TCRM\">Foo</a>" +
          "</h3>" +
          "<p>authors</p>" +
          "<p class=\"journal\">" +
          "<a href=\"https://www.dovepress.com/journal\">This Journal</a> " +
          "<a href=\"https://www.dovepress.com/this-journal-archive1-v879\">2015</a>, 11:1853-186</p>" +
          "<p class=\"journal\">Published Date: <strong>17 December 2015</strong></p>" +
          "</div>" +
          "<!-- /end tab-item -->" +
          "</div></div>" +
          "</div></div>" +
          "</div>" +
          "<!-- /end main -->" +
          "</body></html>";

  private static final String article_bit = 
      "<html>" +
          "<head></head>" +
          "<body>" +
          "<div role=\"main\" class=\"main\" id=\"content\">" +
          "<div class=\"tab-content\">" +
          "<div class=\"articles\">" +
          "<div class=\"intro\">  " +
          "</div>" +
          "<!-- /end intro -->" +
          "<div class=\"copy\">" +
          "The abstract goes here..." +
          "<strong> Keywords:</strong> word" +
          "<a href=\"https://www.dovepress.com/the-link-article-TCRM\" target=\"_blank\">A Letter to the Editor has been received and published for this article.</a>" +
          "<p class=\"article-cc-license\"></p>" +
          "<a class=\"download-btn print-hide\" href=\"https://www.dovepress.com/getfile.php?fileID=1\" id=\"download-pdf\">" +
          "Download Article <span>[PDF]</span></a>&nbsp;" +
          "<a class=\"download-btn print-hide\" href=\"https://www.dovepress.com/fulltext-article-TCRM\" id=\"view-full-text\">" +
          "View Full Text <span>[HTML]</span></a>&nbsp;" +
          "<div id=\"article-fulltext\">" +
          "<p class=\"h1\">Introduction</p>" +
          "</div>" +
          "</div>" +
          "<!-- /end copy -->" +
          "</div>" +
          "<!-- /end articles -->" +
          "</div>" +
          "<!-- /end tab-content -->" +
          "<div class=\"categories-bg group\">" +
          "<div class=\"tabs-padding group\"><h2>Readers of this article also read:</h2>" +
          "<div class=\"tab-content\">" +
          "<div class=\"tab-item\">" +
          "<div class=\"article-labels group\">" +
          "<div class=\"tag\">Review</div>" +
          "</div>" +
          "<h3>" +
          "<a href=\"https://www.dovepress.com/CIA-recommendation1\">Causative</a>" +
          "</h3>" +
          "<p>an author</p>" +
          "<p class=\"journal\">" +
          "<a href=\"https://www.dovepress.com/foo\"</a> " +
          "<a href=\"https://www.dovepress.com/otherjournal-archive4-v852\">2015</a>, 10:1873-187</p>" +
          "<p class=\"journal\">Published Date: <strong>19 November 2015</strong></p>" +
          "</div>" +
          "<!-- /end tab-item -->" +
          "</div>" +
          "<!-- /end tabs-content -->" +
          "</div></div>" +
          "</div>" +
          "<!-- /end main -->" +
          "</body></html>";
  private static final String article_bit_hashfiltered = 
          "<div class=\"articles\">" +
          "<div class=\"intro\">  " +
          "</div>" +
          "<!-- /end intro -->" +
          "<div class=\"copy\">" +
          "The abstract goes here..." +
          "<strong> Keywords:</strong> word" +
          "<a href=\"https://www.dovepress.com/the-link-article-TCRM\" target=\"_blank\">A Letter to the Editor has been received and published for this article.</a>" +
          "<p class=\"article-cc-license\"></p>" +
          "<a class=\"download-btn print-hide\" href=\"https://www.dovepress.com/getfile.php?fileID=1\" id=\"download-pdf\">" +
          "Download Article <span>[PDF]</span></a>&nbsp;" +
          "<a class=\"download-btn print-hide\" href=\"https://www.dovepress.com/fulltext-article-TCRM\" id=\"view-full-text\">" +
          "View Full Text <span>[HTML]</span></a>&nbsp;" +
          "<div id=\"article-fulltext\">" +
          "<p class=\"h1\">Introduction</p>" +
          "</div>" +
          "</div>" +
          "<!-- /end copy -->" +
          "</div>";
  private static final String article_bit_crawlfiltered = 
      "<html>" +
          "<head></head>" +
          "<body>" +
          "<div role=\"main\" class=\"main\" id=\"content\">" +
          "<div class=\"tab-content\">" +
          "<div class=\"articles\">" +
          "<div class=\"intro\">  " +
          "</div>" +
          "<!-- /end intro -->" +
          "<div class=\"copy\">" +
          "<a class=\"download-btn print-hide\" href=\"https://www.dovepress.com/getfile.php?fileID=1\" id=\"download-pdf\">" +
          "Download Article <span>[PDF]</span></a>" +
          "<a class=\"download-btn print-hide\" href=\"https://www.dovepress.com/fulltext-article-TCRM\" id=\"view-full-text\">" +
          "View Full Text <span>[HTML]</span></a>" +
          "<div id=\"article-fulltext\">" +
          "<p class=\"h1\">Introduction</p>" +
          "</div>" +
          "</div>" +
          "<!-- /end copy -->" +
          "</div>" +
          "<!-- /end articles -->" +
          "</div>" +
          "<!-- /end tab-content -->" +
          "</div>" +
          "<!-- /end main -->" +
          "</body></html>";
  
  private static final String manifest_bit=
      "<html>" +
          "<head></head>" +
          "<body>" +
          "<div role=\"main\" class=\"main\" id=\"content\">" +
          "<div class=\"tabs-bg group\">" +
          "<div class=\"tabs-padding group\">" +
          "<h1>CLOCKSS - Published Issues: Therapeutics and Clinical Risk Management 2015</h1>" +
          "<div class=\"copy sitemap\">" +
          "<ul>" +
          "<li>" +
          "<a href=\"https://www.dovepress.com/the-journal-i1010-j1\">The Journal 2015:default</a>" +
          "</li>" +
          "</ul>" +
          "</div>" +
          "</div></div>     " +
          "</div>" +
          "</body></html>";

  private static final String manifest_bit_hashfiltered =
          "<div class=\"copy sitemap\">" +
          "<ul>" +
          "<li>" +
          "<a href=\"https://www.dovepress.com/the-journal-i1010-j1\">The Journal 2015:default</a>" +
          "</li>" +
          "</ul>" +
          "</div>";



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
  
  public void testManifestFiltering() throws Exception {
    InputStream inStream;
    //crawl-filter
    inStream = cfact.createFilteredInputStream(mau,
        new StringInputStream(manifest_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(manifest_bit, StringUtil.fromInputStream(inStream)); // unaltered
    //hash-filter
    inStream = hfact.createFilteredInputStream(mau,
        new StringInputStream(manifest_bit),
        Constants.DEFAULT_ENCODING);
    assertEquals(manifest_bit_hashfiltered, StringUtil.fromInputStream(inStream));

  }  

}
