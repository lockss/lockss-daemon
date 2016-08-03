/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ingenta;

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.definable.DefinablePlugin;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestIngentaHtmlLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  IngentaHtmlLinkRewriterFactory hfact;
  //IngentaJavascriptLinkRewriterFactory jfact;

  public void setUp() throws Exception {
    super.setUp();
    hfact = new IngentaHtmlLinkRewriterFactory();
    //jfact = new IngentaJavascriptLinkRewriterFactory();
  }
  private static final String BOOK_PLUGIN = "org.lockss.plugin.ingenta.ClockssIngentaBooksPlugin";
  private static final String JOURNAL_PLUGIN = "org.lockss.plugin.ingenta.ClockssIngentaJournalPlugin";
  private static final String TRANSFORM_SERVE_CONTENT_BASE = "http://www.foobar.org/ServeContent?url=";

  // The various permutations of one Journal article
  // the original landing page article url 
  static final String baseArtUrl =
      "http://www.ingentaconnect.com/content/manup/vcb/2004/00000005/00000001/art00001";
  // the base of the article (without mimetype, etc) when from the api site
  static final String apiArtUrl =
      "http://api.ingentaconnect.com/content/manup/vcb/2004/00000005/00000001/art00001";
  // The link that is used to direct to the one-time PDF which we ignore and replace with the 
  static final String JRNL_LINK_TO_REWRITE = "/search/download?pub=infobike%3a%2f%2fmanup%2fvcb%2f2004%2f00000005%2f00000001%2fart00001&mimetype=application%2fpdf&exitTargetId=1356554103389";
  // url encoded crawler stable version of the link
  static final String JRNL_LINK_REWRITTEN = 
      "http%3A%2F%2Fapi.ingentaconnect.com%2Fcontent%2Fmanup%2Fvcb%2F2004%2F00000005%2F00000001%2Fart00001%3Fcrawler%3Dtrue";
  // TODO: not url_encoded because not handled by the base class transform
  static final String JRNL_LINK_REWRITTEN_DECODED = 
      "http://api.ingentaconnect.com/content/manup/vcb/2004/00000005/00000001/art00001?crawler=true";

  
  // The various permutations of one Book chapter
  // the original landing page book chapter url
  static final String baseChaptUrl =
      "http://www.ingentaconnect.com/content/bkpub/2nk9qe/1999/00000001/00000001/art00003";
  // The link that is used to direct to the one-time PDF which we ignore and replace with the 
  static final String CHAPT_ONETIME = "bkpub%2f2nk9qe%2f1999%2f00000001%2f00000001%2fart00003&mimetype=application%2fpdf&exitTargetId=1356554103389";
  static final String CHAPT_ONETIME_ENCODED = "bkpub%252f2nk9qe%252f1999%252f00000001%252f00000001%252fart00003%26mimetype%3Dapplication%252fpdf%26exitTargetId%3D1356554103389";
  static final String CHAPT_LINK_TO_REWRITE = "/search/download?pub=infobike%3a%2f%2f" + CHAPT_ONETIME;
  // url encoded crawler stable version of the link - unchanged but for transform
  // the url normalizer will do the rest (to crawler stable version)
  static final String CHAPT_LINK_REWRITTEN = 
      TRANSFORM_SERVE_CONTENT_BASE + 
      "http%3A%2F%2Fwww.ingentaconnect.com%2Fsearch%2Fdownload%3Fpub%3Dinfobike%253a%252f%252f" +
      CHAPT_ONETIME_ENCODED;

  /*
   * LINK TYPE A: basic href 
   * This content is now down, but it is on our boxes and needs to be supported in this form
   */
  static final String testBasicLinkInput = 
      "<span class=\"orangebutton\"><span class=\"orangeleftside icbutton\"> " + 
          "<a href=\"" + JRNL_LINK_TO_REWRITE +
          "\"" +
          " class=\"no-underline contain\" target=\"_blank\">PDF (relative)</a>" +
          "</span>";
  //Once we've been properly transformed, the internal link has been changed
  static final String testBasicLinkOutput = 
      "<span class=\"orangebutton\"><span class=\"orangeleftside icbutton\"> " + 
          "<a href=\"" + TRANSFORM_SERVE_CONTENT_BASE + 
          JRNL_LINK_REWRITTEN +
          "\"" +
          " class=\"no-underline contain\" target=\"_blank\">PDF (relative)</a>" +
          "</span>";

  /*
   * Some pages used a non-standard link attribute for full-text
   * From already collected url:
   * Berghahn - Anthropology in Action V22
   * http://www.ingentaconnect.com/content/berghahn/antiac/2015/00000022/00000001/art00001
   * <div class="right-col-download contain">
   * <a class="fulltext pdf btn btn-general icbutton" data-popup='/search/download?pub=infobike%3a%2f%2fberghahn%2fantiac%2f2015%2f00000022%2f00000001%2fart00001&mimetype=application%2fpdf&exitTargetId=1458391746369' title="PDF download of Introduction to the Special Issue: Negotiating Care in Uncertain Settings and Looking Beneath the Surface of Health Governance Projects" class="no-underline contain" ><i class="fa fa-arrow-circle-o-down"></i></a>&nbsp;<span class="rust"><strong>Download</strong> <br />(PDF 65.8544921875 kb)</span>&nbsp;
   * </div>
   */
  static final String testNewLinkAttrInput = "<div class=\"right-col-download contain\">" +
      "<a class=\"fulltext pdf btn btn-general icbutton\" " +
      "data-popup=\'" +
      JRNL_LINK_TO_REWRITE +
      "\'" +
      "title=\"PDF download of Introduction to the Special Issue: Negotiating Care in Uncertain Settings and Looking Beneath the Surface of Health Governance Projects\" class=\"no-underline contain\" >" +
      "<i class=\"fa fa-arrow-circle-o-down\"></i></a>&nbsp;" +
      "<span class=\"rust\"><strong>Download</strong> <br />(PDF 65.8544921875 kb)</span>&nbsp;" +
      "</div>";

  //* TODO: for now this one won't get the transform ServeContent? put on by the base class
  //* because it doesn't know to look for data-popup
  //* Once this is fixed, modify this test to add in the TRANSFORM_SERVE_CONTENT_BASE
  //* make the REWRITTEN not the decoded version
  static final String testNewLinkAttrOutput = "<div class=\"right-col-download contain\">" +
      "<a class=\"fulltext pdf btn btn-general icbutton\" " +
      "data-popup='" + 
      JRNL_LINK_REWRITTEN_DECODED +
      "'" +
      "title=\"PDF download of Introduction to the Special Issue: Negotiating Care in Uncertain Settings and Looking Beneath the Surface of Health Governance Projects\" class=\"no-underline contain\" >" +
      "<i class=\"fa fa-arrow-circle-o-down\"></i></a>&nbsp;" +
      "<span class=\"rust\"><strong>Download</strong> <br />(PDF 65.8544921875 kb)</span>&nbsp;" +
      "</div>";

  /* 
   * And now some use a javascript onclick method
   * Annals of Glaciology Volume 56, Number 70
   * from: http://www.ingentaconnect.com/contentone/igsoc/agl/2015/00000056/00000070/art00003
   * <a class="fulltext pdf btn btn-general icbutton" onclick="javascript:popup('/search/download?pub=infobike%3a%2f%2figsoc%2fagl%2f2015%2f00000056%2f00000070%2fart00003&amp;mimetype=application%2fpdf&amp;exitTargetId=1466181818752','downloadWindow','900','800')" title="PDF download of Volume and frequency of ice avalanches from Taconnaz hanging glacier, French Alps"><i class="fa fa-arrow-circle-o-down"></i></a>
   */
  static final String testOnClickLinkInput = 
      "<a class=\"fulltext pdf btn btn-general icbutton\" " +
          "onclick=\"javascript:popup('" +
          JRNL_LINK_TO_REWRITE +
          "','downloadWindow','900','800')\" "+ 
          "title=\"PDF download of title\" class=\"no-underline contain\" >";     
  static final String testOnClickLinkOutput = 
      "<a class=\"fulltext pdf btn btn-general icbutton\" " +
          "onclick=\"javascript:popup('" +
          TRANSFORM_SERVE_CONTENT_BASE + 
          JRNL_LINK_REWRITTEN +
          "','downloadWindow','900','800')\" "+ 
          "title=\"PDF download of title\" class=\"no-underline contain\" >";    
  
  /* 
   * And now some use a javascript onclick method
   * Annals of Glaciology Volume 56, Number 70
   * from: http://www.ingentaconnect.com/contentone/igsoc/agl/2015/00000056/00000070/art00003
   * <a class="fulltext pdf btn btn-general icbutton" onclick="javascript:popup('/search/download?pub=infobike%3a%2f%2figsoc%2fagl%2f2015%2f00000056%2f00000070%2fart00003&amp;mimetype=application%2fpdf&amp;exitTargetId=1466181818752','downloadWindow','900','800')" title="PDF download of Volume and frequency of ice avalanches from Taconnaz hanging glacier, French Alps"><i class="fa fa-arrow-circle-o-down"></i></a>
   */
  static final String testOnClickBookLinkInput = 
      "<a class=\"fulltext pdf btn btn-general icbutton\" " +
          "onclick=\"javascript:popup('" +
          CHAPT_LINK_TO_REWRITE +
          "','downloadWindow','900','800')\" "+ 
          "title=\"PDF download of title\" class=\"no-underline contain\" >";     
  static final String testOnClickBookLinkOutput = 
      "<a class=\"fulltext pdf btn btn-general icbutton\" " +
          "onclick=\"javascript:popup('" +
          CHAPT_LINK_REWRITTEN +
          "','downloadWindow','900','800')\" "+ 
          "title=\"PDF download of title\" class=\"no-underline contain\" >";     

  /**
   * Make a basic Ingenta JOURNAL test AU to which URLs can be added.
   * 
   * @return a basic Ingenta test AU
   * @throws ConfigurationException if can't set configuration
   */
  MockArchivalUnit makeAu() throws ConfigurationException, FileNotFoundException {
    MockArchivalUnit mau = new MockArchivalUnit();
    Configuration config =ConfigurationUtil.fromArgs(
        "base_url", "http://www.ingentaconnect.com/",
        "api_url", "http://api.ingentaconnect.com/",
        "graphics_url", "http://graphics.ingentaconnect.com/");
    mau.setConfiguration(config);
    // must do this to set the pluginId which is used to differentiate books/journals
    DefinablePlugin mp = new DefinablePlugin();
    mp.initPlugin(getMockLockssDaemon(),
        JOURNAL_PLUGIN);
    mau.setPlugin(mp);
    mau.setPluginId(JOURNAL_PLUGIN);
    mau.setUrlStems(ListUtil.list(
        "http://www.ingentaconnect.com/",
        "http://api.ingentaconnect.com/",
        "http://graphics.ingentaconnect.com/"
        ));

    // now add the PDF and HTML full-text that it will redirect to
    // for the various tests
    // For simplicity sake, use the same journal article for all three types of full-text links
    CIProperties pdfHeaders = new CIProperties();
    pdfHeaders.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    mau.addUrl(apiArtUrl + "?crawler=true", // pdf with no explicity mimetype
        true, // exists
        true, //shouldCache
        pdfHeaders);

    //http://api.ingentaconnect.com/content/manup/vcb/2004/00000005/00000001/art00001?crawler=true
    CIProperties htmlHeaders = new CIProperties();
    htmlHeaders.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    mau.addUrl(apiArtUrl + "?crawler=true&mimetype=text/html", 
        true, // exists
        true, //shouldCache
        htmlHeaders);
    // the landing page - not really needed for the test, but be clean
    mau.addUrl(baseArtUrl,
        true, // exists
        true, //shouldCache
        htmlHeaders);

    return mau;
  }
  
  
  // Make a basic book plugin and add in a few URLs for testing
  
  MockArchivalUnit makeBookAu() throws ConfigurationException, FileNotFoundException {
    MockArchivalUnit mau = new MockArchivalUnit();
    Configuration config =ConfigurationUtil.fromArgs(
        "base_url", "http://www.ingentaconnect.com/");
    mau.setConfiguration(config);
    // must do this to set the pluginId which is used to differentiate books/journals
    DefinablePlugin mp = new DefinablePlugin();
    mp.initPlugin(getMockLockssDaemon(),
        BOOK_PLUGIN);
    mau.setPlugin(mp);
    mau.setPluginId(BOOK_PLUGIN);
    mau.setUrlStems(ListUtil.list(
        "http://www.ingentaconnect.com/"
        ));
    
    // now add the PDF and HTML full-text that it will redirect to
    // for the various tests
    CIProperties pdfHeaders = new CIProperties();
    pdfHeaders.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    mau.addUrl(baseChaptUrl + "?crawler=true&mimetype=application/pdf", 
        true, // exists
        true, //shouldCache
        pdfHeaders);

    CIProperties htmlHeaders = new CIProperties();
    htmlHeaders.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    // the landing page - not really needed for the test, but be clean
    mau.addUrl(baseChaptUrl,
        true, // exists
        true, //shouldCache
        htmlHeaders);
    return mau;
  }

  /**
   * Test rewriting a basic href link for a full-text article
   *  
   * @throws Exception
   */
  public void testBasicLinkRewriting() throws Exception {
    MockArchivalUnit mockAu = makeAu();

    InputStream in = new ByteArrayInputStream(testBasicLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return TRANSFORM_SERVE_CONTENT_BASE + url;
      }
    };
    InputStream newIn = hfact.createLinkRewriter(
        "text/html", mockAu, in, "UTF-8", 
        baseArtUrl, xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    log.debug3(fout);
    assertEquals(testBasicLinkOutput, fout);
  }

  /**
   * Test rewriting new data-popup attr for a full-text article
   *  
   * @throws Exception
   */
  public void testNewAttrLinkRewriting() throws Exception {
    MockArchivalUnit mockAu = makeAu();

    InputStream in = new ByteArrayInputStream(testNewLinkAttrInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = hfact.createLinkRewriter(
        "text/html", mockAu, in, "UTF-8", 
        baseArtUrl, xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    log.info(fout);
    assertEquals(testNewLinkAttrOutput, fout);
  }

  /**
   * Test rewriting javascript:popup() link for a full-text article
   *  
   * @throws Exception
   */
  public void testonClickLinkRewriting() throws Exception {
    MockArchivalUnit mockAu = makeAu();

    InputStream in = new ByteArrayInputStream(testOnClickLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = hfact.createLinkRewriter(
        "text/html", mockAu, in, "UTF-8", 
        baseArtUrl, xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    log.info(fout);
    assertEquals(testOnClickLinkOutput, fout);
  }
  
  /**
   * Test rewriting javascript:popup() link for a full-text book chapter
   * The link shouldn't get changed to crawler stable as that will happen 
   * later in the url normalizer
   *  
   * @throws Exception
   */
  public void testonClickLinkBookRewriting() throws Exception {
    MockArchivalUnit mockAu = makeBookAu();

    InputStream in = new ByteArrayInputStream(testOnClickBookLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = hfact.createLinkRewriter(
        "text/html", mockAu, in, "UTF-8", 
        baseChaptUrl, xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    log.info(fout);
    assertEquals(testOnClickBookLinkOutput, fout);
  }

}
