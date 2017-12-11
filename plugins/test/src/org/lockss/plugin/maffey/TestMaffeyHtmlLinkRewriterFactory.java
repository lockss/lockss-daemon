/*
 * $Id$
 */

/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.maffey;

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestMaffeyHtmlLinkRewriterFactory extends LockssTestCase {

  MaffeyHtmlLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MaffeyHtmlLinkRewriterFactory();
  }
  
  static final String testLinkInputRelPath1 = 
      "<div>\n" +
     "  <a href=\"/foo\">" +
    "</div>";
  
  static final String testLinkOutput =
      "<div>\n" +
     "  <a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Ffoo\">" +
     "</div>";
  
  static final String testTopFormNonLink =
      "<meta name=\"citation_lastpage\" content=\"3\">" +
          "<meta name=\"citation_pdf_url\" " +
          "content=\"http://insights.sagepub.com/redirect_file.php?fileId=6960&filename=5207-CMRO-Jaundice-in-Gall-Bladder-Cancer-–-The-Yellow-Signal.pdf&fileType=pdf\">" +
      "<div class=\"articlediv_outerleft\" style=\"padding: 0;width: 100%;\">" +
          "<div class=\"\" style=\"font: 12px\">" +
          "<p class=\"article_authors_p\">Author name</p>" +
          "<p><a href=\"journal-clinical-medicine-reviews-in-oncology-j163\" style=\"\" class=\"greenlink_article\">" +
          "<em>Clinical Medicine Reviews in Oncology</em></a> <a" +
          "href=\"./journal.php?journal_id=163&tab=volume#issue760\">2015:5</a>  1-3                " +
          "</p>" +
          "<p>DOI: <a style=\"\" href=\"http://dx.doi.org/10.4137/CMRO.S31252\">10.4137/CMRO.S31252</a>" +
          "</p>" +
          "</div><!-- files ends -->" +
          "<br/>" +
          "Further metadata provided in PDF<br/><br/>" +
          "<form method=\"post\" action=\"shop_cart.php\" id=\"pay_per_view\"" +
          "style=\"margin-bottom:0px;padding:0px; margin-top: 0px\">" +
          "<input type=\"hidden\" name=\"pa\" value=\"add\"/>" +
          "<input type=\"hidden\" name=\"article_title\" value=\"Jaundice in Gall Bladder Cancer &ndash; The Yellow Signal\"/>" +
          "<input type=\"hidden\" name=\"base_price\" value=\"59\"/>" +
          "<input type=\"hidden\" name=\"article_id\" value=\"5207\"/>" +
          "<input type='image' src='img/article_pay_per_view_btn.gif' alt='Article Pay Per View'/>" +
          "</form>" +
          "</div><!-- articlediv_outerleft ends -->";
  //This isn't testing full rewrite...the ServeContent is added to the relative link in the wild
  static final String testTopFormLink_expected =
      "<meta name=\"citation_lastpage\" content=\"3\">" +
          "<meta name=\"citation_pdf_url\" " +
          "content=\"http://insights.sagepub.com/redirect_file.php?fileId=6960&filename=5207-CMRO-Jaundice-in-Gall-Bladder-Cancer-–-The-Yellow-Signal.pdf&fileType=pdf\">" +
      "<div class=\"articlediv_outerleft\" style=\"padding: 0;width: 100%;\">" +
          "<div class=\"\" style=\"font: 12px\">" +
          "<p class=\"article_authors_p\">Author name</p>" +
          "<p><a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Ffoo%2Fjournal-clinical-medicine-reviews-in-oncology-j163\" style=\"\" class=\"greenlink_article\">" +
          "<em>Clinical Medicine Reviews in Oncology</em></a> <a" +
          "href=\"./journal.php?journal_id=163&tab=volume#issue760\">2015:5</a>  1-3                " +
          "</p>" +
          "<p>DOI: <a style=\"\" href=\"http://dx.doi.org/10.4137/CMRO.S31252\">10.4137/CMRO.S31252</a>" +
          "</p>" +
          "</div><!-- files ends -->" +
          "<br/>" +
          "Further metadata provided in PDF<br/><br/>" +
          "<A HREF=\"redirect_file.php?fileType=pdf&fileId=6960&filename=5207-CMRO-Jaundice-in-Gall-Bladder-Cancer-–-The-Yellow-Signal.pdf\"" +
          " target=_blank class=dwnload>\nDownload Article PDF</A>" +
          "</div><!-- articlediv_outerleft ends -->";  

  static final String testFormNonLink_downloadItem =
      "<meta name=\"citation_lastpage\" content=\"3\">" +
          "<meta name=\"citation_pdf_url\" " +
          "content=\"http://insights.sagepub.com/redirect_file.php?fileId=6960&filename=5207-CMRO-Jaundice-in-Gall-Bladder-Cancer-–-The-Yellow-Signal.pdf&fileType=pdf\">" +
      "<div class=\"abstract\" id='downloads'>" +
          "<strong><a name='downloads>'>Downloads</a></strong>" +
          "<div class='downloadsSegmentLeft'>" +
          "<form method=\"post\" action=\"shop_cart.php\" id=\"pay_per_view\" style=\"margin:0;\">" +
          "<input type=\"hidden\" name=\"pa\" value=\"add\"/>" +
          "<input type=\"hidden\" name=\"article_title\" value=\"Jaundice in Gall Bladder Cancer &ndash; The Yellow Signal\"/>" +
          "<input type=\"hidden\" name=\"base_price\" value=\"59\"/>" +
          "<input type=\"hidden\" name=\"article_id\" value=\"5207\"/>" +
          "<input type='image' src='./img/article_pay_per_view_btn.gif' alt='Article Pay Per View'/>" +
          "</form>" +
          "</div>" +
          "</div>";

  static final String testDownloadItem_PdfLink =
      "<meta name=\"citation_lastpage\" content=\"3\">" +
          "<meta name=\"citation_pdf_url\" " +
          "content=\"http://insights.sagepub.com/redirect_file.php?fileId=6960&filename=5207-CMRO-Jaundice-in-Gall-Bladder-Cancer-–-The-Yellow-Signal.pdf&fileType=pdf\">" +
      "<div class=\"abstract\" id='downloads'>" +
          "<strong><a name='downloads>'>Downloads</a></strong>" +
          "<div class='downloadsSegmentLeft'>" +
          "<A HREF=\"redirect_file.php?fileType=pdf&fileId=6960&filename=5207-CMRO-Jaundice-in-Gall-Bladder-Cancer-–-The-Yellow-Signal.pdf\"" +
          " target=_blank class=dwnload>\nDownload Article PDF</A>" +
          "</div>" +
          "</div>";  




  /**
   * Make a basic Maffey test AU to which URLs can be added.
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


  public void testMaffeyRewritingSimple() throws Exception {
    MockArchivalUnit mockAu = makeAu();

    //    InputStream in = new ByteArrayInputStream(testLinkInputRelPath1.getBytes());
    InputStream in = new ByteArrayInputStream(testLinkInputRelPath1.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testLinkOutput, fout);
  }

  public void testMaffeyFormLinkRewriting_topItem() throws Exception {
    MockArchivalUnit mockAu = makeAu();

    InputStream in = new ByteArrayInputStream(testTopFormNonLink.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    log.debug3(fout);
    assertEquals(testTopFormLink_expected, fout);
  }  

  public void testMaffeyFormLinkRewriting_downloadItem() throws Exception {
    MockArchivalUnit mockAu = makeAu();

    InputStream in = new ByteArrayInputStream(testFormNonLink_downloadItem.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/foo/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    log.debug3(fout);
    assertEquals(testDownloadItem_PdfLink, fout);
  }    
  
}
