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

package org.lockss.plugin.pub2web.ms;

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestMsHtmlLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  MsHtmlLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new MsHtmlLinkRewriterFactory();
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
  
  static final String testContentLinkInput = 
      "<div class=\"contentTypeOptions\">\n" + 
      "<ul class=\"flat bobby fulltext\">\n" + 
      "<li class=\"html\">\n" +
      "<a href=\"/deliver/fulltext/10.1128/9781555819453/chap2.html?itemId=/content/book/10.1128/9781555819453.chap2" +
      "&amp;mimeType=html&amp;isFastTrackArticle=\"" +
      " title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink html\">HTML</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "94.35\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "<li class=\"pdf\">\n" +
      "<a href=\"/deliver/fulltext/10.1128/9781555819453/9781555819446_Chap02.pdf?itemId=/content/book/10.1128/9781555819453.chap2&amp;mimeType=pdf&amp;isFastTrackArticle=\"" +
      " title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink pdf\">PDF</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "372.32\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "<li class=\"xml\" style=\"display: none;\">\n" +
      "<a href=\"/deliver/fulltext/10.1128/9781555819453/chap2.xml?itemId=/content/book/10.1128/9781555819453.chap2&amp;mimeType=xml&amp;isFastTrackArticle=\"" +
      " title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink xml\">XML</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "140.32\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "</ul>\n" + 
      "</div>\n";
  
  static final String testContentLinkOutput = 
      "<div class=\"contentTypeOptions\">\n" + 
      "<ul class=\"flat bobby fulltext\">\n" + 
      "<li class=\"html\">\n" +
      "<a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2F10.1128%2F9781555819453%2Fchap2.html%3FitemId%3D%2Fcontent%2Fbook%2F10.1128%2F9781555819453.chap2%26amp%3BmimeType%3Dhtml%26amp%3BisFastTrackArticle%3D\"" + 
      " title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink html\">HTML</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "94.35\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "<li class=\"pdf\">\n" +
      "<a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2F10.1128%2F9781555819453%2F9781555819446_Chap02.pdf%3FitemId%3D%2Fcontent%2Fbook%2F10.1128%2F9781555819453.chap2%26amp%3BmimeType%3Dpdf%26amp%3BisFastTrackArticle%3D\"" +
      " title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink pdf\">PDF</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "372.32\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "<li class=\"xml\" style=\"display: none;\">\n" +
      "<a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2F10.1128%2F9781555819453%2Fchap2.xml%3FitemId%3D%2Fcontent%2Fbook%2F10.1128%2F9781555819453.chap2%26amp%3BmimeType%3Dxml%26amp%3BisFastTrackArticle%3D\"" +
      " title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink xml\">XML</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "140.32\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "</ul>\n" + 
      "</div>\n";
  
  static final String testRewriteInput =
      "<div class=\"access-options textoptionsFulltext\"><!-- FREE.TAG -->\n" + 
      "<div class=\"contentTypeOptions\">\n" +
      "<div class=\"flat bobby fulltext list-group\">\n" + 
      "<a href=\"/deliver/fulltext/jmmcr/2/2/jmmcr000020.pdf" +
      "?itemId=/content/journal/jmmcr/10.1099/jmmcr.0.000020&amp;mimeType=pdf&amp;isFastTrackArticle=\"" +
      " title=\"\" class=\"externallink pdf list-group-item list-group-item-info\" rel=\"external\">\n" +
      "<div class=\"fa fa-file-pdf-o full-text-icon\"></div>PDF\n" + 
      "<div class=\"fulltextsize \">\n" + 
      "276.26\n" + 
      "Kb\n" + 
      "</div></a>\n" + 
      "<a href=\"/content/journal/jmmcr/10.1099/jmmcr.0.000020#tab2\"" +
      " title=\"\" class=\"html list-group-item list-group-item-info\" rel=\"external\">\n" +
      "<div class=\"fa fa-file-html-o full-text-icon\"></div>HTML\n" + 
      "<div class=\"fulltextsize \">\n" + 
      "40.74\n" + 
      "Kb\n" + 
      "</div></a>\n" + 
      "</div></div>\n" + 
      "</div>\n" +
      "\n" +
      "<div id=\"tab3\" class=\"dataandmedia hidden-js-div tabbedsection tab-pane\"" +
      " data-ajaxurl=\"/content/journal/jmmcr/10.1099/jmmcr.0.000020/figures?fmt=ahah\">\n" + 
      "<p class=\"loading-message\"><i class=\"fa fa-spinner fa-spin fa-2x\"></i> &nbsp;Figure data loading....</p>\n" + 
      "</div>\n" + 
      "\n" +
      "<div id=\"itemFullTextId\" class=\"itemFullTextHtml retrieveFullTextHtml hidden-js-div\"" +
      " data-fullTexturl=\"/deliver/fulltext/jmmcr/2/2/jmmcr000020.html" +
      "?itemId=/content/journal/jmmcr/10.1099/jmmcr.0.000020&mimeType=html&fmt=ahah\">\n" + 
      "/deliver/fulltext/jmmcr/2/2/jmmcr000020.html" +
      "?itemId=/content/journal/jmmcr/10.1099/jmmcr.0.000020&mimeType=html&fmt=ahah\n" + 
      "</div>";

  static final String testRewriteOutput =
      "<div class=\"access-options textoptionsFulltext\"><!-- FREE.TAG -->\n" + 
      "<div class=\"contentTypeOptions\">\n" + 
      "<div class=\"flat bobby fulltext list-group\">\n" + 
      "<a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2Fjmmcr%2F2%2F2%2Fjmmcr000020.pdf" +
      "%3FitemId%3D%2Fcontent%2Fjournal%2Fjmmcr%2F10.1099%2Fjmmcr.0.000020%26amp%3BmimeType%3Dpdf%26amp%3BisFastTrackArticle%3D\"" +
      " title=\"\" class=\"externallink pdf list-group-item list-group-item-info\" rel=\"external\">\n" +
      "<div class=\"fa fa-file-pdf-o full-text-icon\"></div>PDF\n" + 
      "<div class=\"fulltextsize \">\n" + 
      "276.26\n" + 
      "Kb\n" + 
      "</div></a>\n" + 
      "<a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2Fjmmcr%2F2%2F2%2Fjmmcr000020.html" +
      "%3FitemId%3D%2Fcontent%2Fjournal%2Fjmmcr%2F10.1099%2Fjmmcr.0.000020%26mimeType%3Dhtml%26fmt%3Dahah\"" +
      " title=\"\" class=\"html list-group-item list-group-item-info\" rel=\"external\" target=_blank>\n" +
      "<div class=\"fa fa-file-html-o full-text-icon\"></div>HTML\n" + 
      "<div class=\"fulltextsize \">\n" + 
      "40.74\n" + 
      "Kb\n" + 
      "</div></a>\n" + 
      "</div></div>\n" + 
      "</div>\n" +
      "\n" +
      "<div id=\"tab3\" class=\"dataandmedia hidden-js-div tabbedsection tab-pane\"" +
      " data-ajaxurl=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fcontent%2Fjournal%2Fjmmcr%2F10.1099%2Fjmmcr.0.000020%2Ffigures%3Ffmt%3Dahah\">\n" + 
      "<p class=\"loading-message\"><i class=\"fa fa-spinner fa-spin fa-2x\"></i> &nbsp;Figure data loading....</p>\n" + 
      "</div>\n" + 
      "\n" +
      "<div id=\"itemFullTextId\" class=\"itemFullTextHtml retrieveFullTextHtml hidden-js-div\"" +
      " data-fullTexturl=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2Fjmmcr%2F2%2F2%2Fjmmcr000020.html" +
      "%3FitemId%3D%2Fcontent%2Fjournal%2Fjmmcr%2F10.1099%2Fjmmcr.0.000020%26mimeType%3Dhtml%26fmt%3Dahah\">\n" + 
      "/deliver/fulltext/jmmcr/2/2/jmmcr000020.html" +
      "?itemId=/content/journal/jmmcr/10.1099/jmmcr.0.000020&mimeType=html&fmt=ahah\n" + 
      "</div>";
  
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

  public void testContentLink() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testContentLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testContentLinkOutput, fout);
  }

  public void testRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testRewriteInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testRewriteOutput, fout);
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
