/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.silverchair;

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestScHtmlLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  ScHtmlLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new ScHtmlLinkRewriterFactory();
  }
  
  static final String testLinkInputRelPath = 
      "<div>\n" +
     "  <a href=\"/foo\">http://www.xyz.com/foo</a>" +
    "</div>";
  
  static final String testLinkOutput =
      "<div>\n" +
     "  <a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Ffoo\">http://www.xyz.com/foo</a>" +
     "</div>";
  
  static final String testLinkInputAbsPath = 
      "<div>\n" +
      "  <a href=\"http://www.xyz.com/foo\">http://www.xyz.com/foo</a>" +
      "</div>";
  
  static final String testTocPdfLinkInput = 
      "<div>\n" +
      "<div class=\"al-other-resource-links\">\n" + 
      "<a class=\"al-link pdf pdfaccess pdf-link\" data-article-id=\"2678901\"" +
      " data-article-url=\"/data/Journals/LSHSS/936194/LSHSS_48_2_99.pdf\"" +
      " data-ajax-url=\"/Content/CheckPdfAccess\">\n" + 
      "<i class=\"icon-file-pdf\"></i> <span>PDF</span>\n" + 
      "</a>\n" + 
      "<a class=\"al-link pdf pdfaccess readcube-epdf-link\" data-article-id=\"2678901\"" +
      " data-article-url=\"/epdf.aspx?doi=10.1044/2017_LSHSS-16-0030\"" +
      " data-ajax-url=\"/Content/CheckPdfAccess\">\n" + 
      "<i class=\"icon-file-pdf\"></i> <span>PDF</span>\n" + 
      "</a>\n" + 
      "</div>";
  
  static final String testTocPdfLinkOutput = 
      "<div>\n" +
      "<div class=\"al-other-resource-links\">\n" + 
      "<a class=\"rewritten pdf link\" data-article-id=\"2678901\"" +
      " data-article-url=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdata%2FJournals%2FLSHSS%2F936194%2FLSHSS_48_2_99.pdf\"" +
      " href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdata%2FJournals%2FLSHSS%2F936194%2FLSHSS_48_2_99.pdf\"" +
      " target=_blank>\n" +
      "<i class=\"icon-file-pdf\"></i> <span>PDF</span>\n" + 
      "</a>\n" + 
      "<a class=\"al-link pdf pdfaccess readcube-epdf-link\" data-article-id=\"2678901\"" +
      " data-article-url=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fepdf.aspx%3Fdoi%3D10.1044%2F2017_LSHSS-16-0030\"" +
      " data-ajax-url=\"/Content/CheckPdfAccess\">\n" + 
      "<i class=\"icon-file-pdf\"></i> <span>PDF</span>\n" + 
      "</a>\n" + 
      "</div>";
  
  static final String testNavLinkInput = 
      "<div>\n" +
      "<div class=\"widget-ArticleJumpLinks widget-instance-ArticleJumpLinks Widget\">\n" + 
      "<ul data-magellan-expedition=\"fixed\">\n" + 
      "<li class=\"section-jump-link head-1\" data-magellan-arrival=\"164998999\">" +
      "<a class=\"scrollTo\" href=\"#164998999\">Conclusion</a></li>\n" + 
      "<li class=\"section-jump-link head-1\" data-magellan-arrival=\"164998888\">" +
      "<a class=\"scrollTo\" href=\"#164998888\">References</a></li>\n" + 
      "</ul>\n" + 
      "</div>";
  
  static final String testNavLinkOutput = 
      "<div>\n" +
      "<div class=\"widget-ArticleJumpLinks widget-instance-ArticleJumpLinks Widget\">\n" + 
      "<ul data-magellan-expedition=\"fixed\">\n" + 
      "<li class=\"section-jump-link head-1\" data-magellan-arrival=\"164998999\">" +
      "<a class=\"scrollTo\" href=\"#164998999\">Conclusion</a></li>\n" + 
      "<li class=\"section-jump-link head-1\" data-magellan-arrival=\"164998888\">" +
      "<a class=\"scrollTo\" href=\"#164998888\">References</a></li>\n" + 
      "</ul>\n" + 
      "</div>";
  
  static final String testTabLinkInput =
      "<ul class=\"small-centered\">\n" + 
      "  <li class=\"menu-icon item-with-dropdown bdr\">\n" + 
      "   <a class=\"toolbox-dropdown\" id=\"site-menu\" data-dropdown=\"filterDrop\">\n" + 
      "    <i class=\"icon-filter\"></i><span>View</span>\n" + 
      "   </a>\n" + 
      "   <ul id=\"filterDrop\" class=\"f-dropdown\" data-dropdown-content=\"\">\n" + 
      "    <li class=\"articleTab\" style=\"display: none;\"><a class=\"tab-item\" href=\"#articleTab\"><span>Article</span></a></li>\n" + 
      "    <li class=\"figureTab\"><a class=\"tab-item\" href=\"#figureTab\"><span>Figures</span></a></li>\n" + 
      "    <li class=\"tableTab\"><a class=\"tab-item\" href=\"#tableTab\"><span>Tables</span></a></li>\n" + 
      "   </ul>\n" + 
      "  </li>\n" + 
      "  <li id=\"ctl00_ctl00_BodyContent_PageContent_LinkPdfReadcube\" class=\"menu-icon bdr toolbar-pdf\">\n" + 
      "   <a id=\"pdfLink\" class=\"al-link pdf article-pdfLink pdfaccess pdf-link\" data-article-id=\"2622001\"" +
      " data-article-url=\"/data/journals/lshss/936194/lshss_48_2_108.pdf\" data-ajax-url=\"/Content/CheckPdfAccess\">\n" + 
      "   <i class=\"icon-file-pdf\"></i><span>PDF</span>\n" + 
      "   </a>\n" + 
      "   <a id=\"epdfLink\" class=\"al-link pdf article-pdfLink pdfaccess readcube-epdf-link\" data-article-id=\"2622001\"" +
      " data-article-url=\"/epdf.aspx?doi=10.1044/2017_lshss-16-0058\" data-ajax-url=\"/Content/CheckPdfAccess\">\n" + 
      "   <i class=\"icon-file-pdf\"></i><span>PDF</span>\n" + 
      "   </a>\n" + 
      "  </li>\n" + 
      "  <li class=\"menu-icon item-with-dropdown bdr\">\n" + 
      "   <a class=\"toolbox-dropdown\" id=\"plus-icon\" data-dropdown=\"plusDrop\"><i class=\"icon-attachment\"></i>" +
      "<span>Supplemental</span></a>\n" + 
      "   <ul id=\"plusDrop\" class=\"f-dropdown\" data-dropdown-content=\"\">\n" + 
      "    <li class=\"supplementalTab\"><a class=\"tab-item\" href=\"#supplementalTab\"><span>Data Supplements</span></a></li>\n" + 
      "   </ul>\n" + 
      "  </li>\n" + 
      "  <li class=\"menu-icon item-with-dropdown bdr toolbar-share\">\n" + 
      "   <a class=\"toolbox-dropdown\" id=\"share-icon\" data-dropdown=\"shareDrop\">\n" + 
      "   <i class=\"icon-share-alt\"></i><span>Share</span>\n" + 
      "   </a>\n" + 
      "  </li>\n" + 
      "  <li class=\"menu-icon item-with-dropdown last toolbar-tools\">\n" + 
      "   <a class=\"toolbox-dropdown\" id=\"settings-icon\" data-dropdown=\"settingsDrop\">\n" + 
      "   <i class=\"icon-tools\"></i><span>Tools</span>\n" + 
      "   </a>\n" + 
      "   <ul id=\"settingsDrop\" class=\"f-dropdown\" data-dropdown-content=\"\">\n" + 
      "    <li>" +
      "    <div class=\"widget-ToolboxGetCitation widget-instance-Get Citation\">\n" + 
      "     <a href=\"#\" data-reveal-id=\"getCitation\" data-reveal=\"\">" +
      "     <i class=\"icon-read-more\"></i><span>Get Citation</span></a>\n" + 
      "    </div>\n" + 
      "    </li>\n" + 
      "   </ul>\n" + 
      "  </li>\n" + 
      "</ul>";
  
  static final String testTabLinkOutput =
      "<ul class=\"small-centered\">\n" + 
      "  <li class=\"menu-icon item-with-dropdown bdr\">\n" + 
      "   <a class=\"toolbox-dropdown\" id=\"site-menu\" data-dropdown=\"filterDrop\">\n" + 
      "    <i class=\"icon-filter\"></i><span>View</span>\n" + 
      "   </a>\n" + 
      "   <ul id=\"filterDrop\" class=\"f-dropdown\" data-dropdown-content=\"\">\n" + 
      "    <li class=\"articleTab\" style=\"display: none;\"><a class=\"tab-item\" href=\"#articleTab\"><span>Article</span></a></li>\n" + 
      "    <li class=\"figureTab\"><a class=\"tab-item\" href=\"#figureTab\"><span>Figures</span></a></li>\n" + 
      "    <li class=\"tableTab\"><a class=\"tab-item\" href=\"#tableTab\"><span>Tables</span></a></li>\n" + 
      "   </ul>\n" + 
      "  </li>\n" + 
      "  <li id=\"ctl00_ctl00_BodyContent_PageContent_LinkPdfReadcube\" class=\"menu-icon bdr toolbar-pdf\">\n" + 
      "   <a id=\"pdfLink\" class=\"rewritten pdf link\" data-article-id=\"2622001\"" +
      " data-article-url=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdata%2Fjournals%2Flshss%2F936194%2Flshss_48_2_108.pdf\"" +
      " href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdata%2Fjournals%2Flshss%2F936194%2Flshss_48_2_108.pdf\"" +
      " target=_blank>\n" +
      "   <i class=\"icon-file-pdf\"></i><span>PDF</span>\n" + 
      "   </a>\n" + 
      "   <a id=\"epdfLink\" class=\"al-link pdf article-pdfLink pdfaccess readcube-epdf-link\" data-article-id=\"2622001\"" +
      " data-article-url=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fepdf.aspx%3Fdoi%3D10.1044%2F2017_lshss-16-0058\"" +
      " data-ajax-url=\"/Content/CheckPdfAccess\">\n" + 
      "   <i class=\"icon-file-pdf\"></i><span>PDF</span>\n" + 
      "   </a>\n" + 
      "  </li>\n" + 
      "  <li class=\"menu-icon item-with-dropdown bdr\">\n" + 
      "   <a class=\"toolbox-dropdown\" id=\"plus-icon\" data-dropdown=\"plusDrop\"><i class=\"icon-attachment\"></i>" +
      "<span>Supplemental</span></a>\n" + 
      "   <ul id=\"plusDrop\" class=\"f-dropdown\" data-dropdown-content=\"\">\n" + 
      "    <li class=\"supplementalTab\"><a class=\"tab-item\" href=\"#supplementalTab\"><span>Data Supplements</span></a></li>\n" + 
      "   </ul>\n" + 
      "  </li>\n" + 
      "  <li class=\"menu-icon item-with-dropdown bdr toolbar-share\">\n" + 
      "   <a class=\"toolbox-dropdown\" id=\"share-icon\" data-dropdown=\"shareDrop\">\n" + 
      "   <i class=\"icon-share-alt\"></i><span>Share</span>\n" + 
      "   </a>\n" + 
      "  </li>\n" + 
      "  <li class=\"menu-icon item-with-dropdown last toolbar-tools\">\n" + 
      "   <a class=\"toolbox-dropdown\" id=\"settings-icon\" data-dropdown=\"settingsDrop\">\n" + 
      "   <i class=\"icon-tools\"></i><span>Tools</span>\n" + 
      "   </a>\n" + 
      "   <ul id=\"settingsDrop\" class=\"f-dropdown\" data-dropdown-content=\"\">\n" + 
      "    <li>" +
      "    <div class=\"widget-ToolboxGetCitation widget-instance-Get Citation\">\n" + 
      "     <a href=\"#\" data-reveal-id=\"getCitation\" data-reveal=\"\">" +
      "     <i class=\"icon-read-more\"></i><span>Get Citation</span></a>\n" + 
      "    </div>\n" + 
      "    </li>\n" + 
      "   </ul>\n" + 
      "  </li>\n" + 
      "</ul>";
  
  static final String testTocRewriteInput =
      "<div class=\"al-article-box al-normal shrink\">\n" + 
      "    <div class=\"right al-article-items\">\n" + 
      "        <div class=\"customLink\">\n" + 
      "            <a href=\"/article.aspx?articleid=2599999\">Interactive Book Reading to Accelerate Word Learning by Anyone</a>\n" + 
      "        </div>\n" + 
      "        <div class=\"al-other-resource-links\">\n" + 
      "            <a class=\"al-link pdf pdfaccess pdf-link\" data-article-id=\"2599999\" data-article-url=\"/data/Journals/LSHSS/936999/LSHSS_48_1_16.pdf\" data-ajax-url=\"/Content/CheckPdfAccess\">\n" + 
      "            <i class=\"icon-file-pdf\"></i> <span>PDF</span>\n" + 
      "            </a>\n" + 
      "            <a class=\"al-link pdf pdfaccess readcube-epdf-link\" data-article-id=\"2599999\" data-article-url=\"/epdf.aspx?doi=10.1044/2016_LSHSS-99-0099\" data-ajax-url=\"/Content/CheckPdfAccess\">\n" + 
      "            <i class=\"icon-file-pdf\"></i> <span>PDF</span>\n" + 
      "            </a>\n" + 
      "        </div>\n" + 
      "        <div class=\"al-authors-list\">\n" + 
      "            Art Auth1, Art Auth2, Art Auth3, and Art Auth4\n" + 
      "        </div>\n" + 
      "        <div class=\"al-terms-wrapper\">\n" + 
      "            <span class=\"al-terms-header\">Tags:</span>\n" + 
      "            <span><a class=\"al-term sriTopiclink\">child</a>, <a class=\"al-term sriTopiclink\">therapeutics</a>, <a class=\"al-term sriTopiclink\">specific language impairment</a></span>\n" + 
      "        </div>\n" + 
      "        <div class=\"al-cite-description\">\n" + 
      "            <span>Language, Speech, and Hearing Services in Schools</span>, January 2017, Vol. 48, 99. doi:10.1044/2016_LSHSS-99-0099\n" + 
      "        </div>\n" + 
      "        <div class=\"al-expanded-section\" id=\"target2599999\" hidden=\"\"></div>\n" + 
      "    </div>\n" + 
      "</div>\n";

  static final String testTocRewriteOutput =
      "<div class=\"al-article-box al-normal shrink\">\n" + 
      "    <div class=\"right al-article-items\">\n" + 
      "        <div class=\"customLink\">\n" +
      "            <a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Farticle.aspx%3Farticleid%3D2599999\">Interactive Book Reading to Accelerate Word Learning by Anyone</a>\n" + 
      "        </div>\n" + 
      "        <div class=\"al-other-resource-links\">\n" + 
      "            <a class=\"rewritten pdf link\" data-article-id=\"2599999\"" +
      " data-article-url=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdata%2FJournals%2FLSHSS%2F936999%2FLSHSS_48_1_16.pdf\"" +
      " href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdata%2FJournals%2FLSHSS%2F936999%2FLSHSS_48_1_16.pdf\" target=_blank>\n" + 
      "            <i class=\"icon-file-pdf\"></i> <span>PDF</span>\n" + 
      "            </a>\n" + 
      "            <a class=\"al-link pdf pdfaccess readcube-epdf-link\" data-article-id=\"2599999\"" +
      " data-article-url=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fepdf.aspx%3Fdoi%3D10.1044%2F2016_LSHSS-99-0099\"" +
      " data-ajax-url=\"/Content/CheckPdfAccess\">\n" + 
      "            <i class=\"icon-file-pdf\"></i> <span>PDF</span>\n" + 
      "            </a>\n" + 
      "        </div>\n" + 
      "        <div class=\"al-authors-list\">\n" + 
      "            Art Auth1, Art Auth2, Art Auth3, and Art Auth4\n" + 
      "        </div>\n" + 
      "        <div class=\"al-terms-wrapper\">\n" + 
      "            <span class=\"al-terms-header\">Tags:</span>\n" + 
      "            <span><a class=\"al-term sriTopiclink\">child</a>, <a class=\"al-term sriTopiclink\">therapeutics</a>, <a class=\"al-term sriTopiclink\">specific language impairment</a></span>\n" + 
      "        </div>\n" + 
      "        <div class=\"al-cite-description\">\n" + 
      "            <span>Language, Speech, and Hearing Services in Schools</span>, January 2017, Vol. 48, 99. doi:10.1044/2016_LSHSS-99-0099\n" + 
      "        </div>\n" + 
      "        <div class=\"al-expanded-section\" id=\"target2599999\" hidden=\"\"></div>\n" + 
      "    </div>\n" + 
      "</div>\n";
  
  
  static final String testNoRewriteLink =
      "<div>\n" +
     "  <a href=\"http://www.google.com/search/foo\">" +
     "</div>";
  
  
  /**
   * Make a basic test AU to which URLs can be added.
   * 
   * @return a basic test AU
   * @throws ConfigurationException if can't set configuration
   */
  MockArchivalUnit makeAu() throws ConfigurationException {
    MockArchivalUnit mau = new MockArchivalUnit();
    Configuration config = ConfigurationUtil.fromArgs(
        "base_url", "http://www.xyz.com/");
    mau.setConfiguration(config);
    mau.setUrlStems(ListUtil.list(
        "http://www.xyz.com/"
        ));
    return mau;
  }

  public void testLinkRewritingAbsolutePath() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkInputAbsPath.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/foo", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testLinkOutput, fout);
  }

  public void testLinkRewritingRelativePath1() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkInputRelPath.getBytes());
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

  public void testLinkRewritingRelativePath2() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testLinkOutput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF-8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testLinkOutput, fout);
  }

  public void testTocPdfLinkRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testTocPdfLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testTocPdfLinkOutput, fout);
  }

  public void testNavLinkRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testNavLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testNavLinkOutput, fout);
  }

  public void testTabLinkRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testTabLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testTabLinkOutput, fout);
  }

  public void testTocRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testTocRewriteInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testTocRewriteOutput, fout);
  }

  public void testNoRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testNoRewriteLink.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testNoRewriteLink, fout);
  }
}
