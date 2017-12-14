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

package org.lockss.plugin.pub2web.asm;

import java.io.*;

import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.servlet.ServletUtil;
import org.lockss.test.*;

public class TestAsmHtmlLinkRewriterFactory extends LockssTestCase {
  static String ENC = Constants.DEFAULT_ENCODING;

  AsmHtmlLinkRewriterFactory fact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new AsmHtmlLinkRewriterFactory();
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
  
  static final String testXmlLinkInput = 
      "<div>\n" +
      "<li class=\"xml\"><a href=\"/deliver/fulltext/ecosalplus/7/7/ESP-0099-2016.xml?itemId=/content/journal/ecosalplus/10.1128/ecosalplus.ESP-0099-2016&amp;mimeType=xml&amp;isFastTrackArticle=\" title=\"\" rel=\"external\" class=\"externallink xml\" >XML</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "123.45\n" + 
      "Kb\n" + 
      "</div>\n" +
      "</li>\n" +
      "</div>";
  
  static final String testXmlLinkOutput = 
      "<div>\n" + 
      "<li class=\"xml\"><a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2Fecosalplus%2F7%2F7%2FESP-0099-2016.xml%3FitemId%3D%2Fcontent%2Fjournal%2Fecosalplus%2F10.1128%2Fecosalplus.ESP-0099-2016%26amp%3BmimeType%3Dxml%26amp%3BisFastTrackArticle%3D\" title=\"\" rel=\"external\" class=\"externallink xml\" >XML</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "123.45\n" + 
      "Kb\n" + 
      "</div>\n" +
      "</li>\n" +
      "</div>";
  
  static final String testHtmlLinkInput = 
      "<div>\n" + 
      "<ul><li class=\"html\">\n" +
      "<a href=\"/deliver/fulltext/ecosalplus/7/7/ESP-0099-2016.html?" +
      "itemId=/content/journal/ecosalplus/10.1128/ecosalplus.ESP-0099-2016" +
      "&amp;mimeType=html&amp;isFastTrackArticle=\"" +
      " title=\"\" rel=\"external\" class=\"externallink html\" >HTML</a>\n" + 
      "<div class=\"fulltextsize\">\n" +
      "123.45\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li></ul>\n" +
      "</div>";
  
  static final String testHtmlLinkOutput = 
      "<div>\n" + 
      "<ul><li class=\"html\" style=\"display: block\">\n" +
      "<a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com" +
      "%2Fdeliver%2Ffulltext%2Fecosalplus%2F7%2F7%2FESP-0099-2016.html%3F" +
      "itemId%3D%2Fcontent%2Fjournal%2Fecosalplus%2F10.1128%2Fecosalplus.ESP-0099-2016" +
      "%26amp%3BmimeType%3Dhtml%26amp%3BisFastTrackArticle%3D\"" +
      " title=\"\" rel=\"external\" class=\"externallink html\" >HTML</a>\n" + 
      "<div class=\"fulltextsize\">\n" +
      "123.45\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li></ul>\n" +
      "</div>";
  
  static final String testMediaLinkInput =
      "<a class=\"media-link\"" +
      " href=\"/content/ecosalplus/10.1128/ecosalplus.ESP-0099-2016.f1\"" +
      " id=\"/content/ecosalplus/10.1128/ecosalplus.ESP-0099-2016.f1\">\n" + 
      "   <img src=\"/docserver/ahah/fulltext/ecosalplus/7/7/ecosalplus.ESP-0099-2016.f1_thmb.gif\"" +
      " alt=\"FIGURE 1\" border=\"0\">\n" + 
      "   <p>\n" + 
      "      <span class=\"figure-duplicate-label\">\n" + 
      "         <span class=\"label\">FIGURE 1</span>\n" + 
      "      </span>Click to view\n" + 
      "   </p>\n" + 
      "</a>";
  
  static final String testMediaLinkOutput =
      "<a class=\"media-link\"" +
      " href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fcontent%2Fecosalplus%2F10.1128%2Fecosalplus.ESP-0099-2016.f1\"" +
      " id=\"/content/ecosalplus/10.1128/ecosalplus.ESP-0099-2016.f1\" target=_blank>\n" + 
      "   <img src=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdocserver%2Fahah%2Ffulltext%2Fecosalplus%2F7%2F7%2Fecosalplus.ESP-0099-2016.f1_thmb.gif\"" +
      " alt=\"FIGURE 1\" border=\"0\">\n" + 
      "   <p>\n" + 
      "      <span class=\"figure-duplicate-label\">\n" + 
      "         <span class=\"label\">FIGURE 1</span>\n" + 
      "      </span>Click to view\n" + 
      "   </p>\n" + 
      "</a>";
  
  static final String testTocRewriteInput =
      "<div class=\"right-contant col-sm-9 contain \"> \n" + 
      "<h2>Volume 7, Issue 7, \n" + 
      "2016\n" + 
      "</h2> \n" + 
      "<div class=\"toc\">\n" + 
      "<div class=\"toc\">\n" + 
      "<ul class=\"sections flat\">\n" + 
      "<li class=\"section tocHeading\">\n" + 
      "<h3 class=\"heading\">\n" + 
      "<i class=\"fa fa-plus-circle\" title=\"expand toc heading\"></i>\n" + 
      "<i class=\"fa fa-minus-circle hide\" title=\"expand toc heading\"></i>\n" + 
      "</h3>\n" + 
      "<div class=\"tocheadingarticlesloading hidden-js-div\">\n" + 
      "<img src=\"/images/jp/spinner.gif\" alt=\"Loading Accepted Manuscripts...\" />\n" + 
      "<span>Loading Accepted Manuscripts...</span>\n" + 
      "</div>\n" + 
      "<div class=\"tocheadingarticlelisting retrieveTocheadingArticle hidden-js-div\">" +
      "/content/journal/ecosalplus/7/1/articles?fmt=ahah&tocHeading=" +
      "http://asm.metastore.ingenta.com/content/journal/ecosalplus/reviewArticle" +
      "</div>\n" + 
      "</li>\n" + 
      "</ul>\n" + 
      "</div>\n" + 
      "<form id=\"ahahTocArticles\">\n" + 
      "<input type=\"hidden\" name=\"articleIds\" value=\"\" />\n" + 
      "<input type=\"hidden\" name=\"fmt\" value=\"ahah\" />\n" + 
      "<input type=\"hidden\" name=\"ahahcontent\" value=\"toc\" />\n" + 
      "</form>\n" + 
      "</div>\n" + 
      "</div>";

  static final String testTocRewriteOutput =
      "<div class=\"right-contant col-sm-9 contain \"> \n" + 
      "<h2>Volume 7, Issue 7, \n" + 
      "2016\n" + 
      "</h2> \n" + 
      "<div class=\"toc\">\n" + 
      "<div class=\"toc\">\n" + 
      "<ul class=\"sections flat\">\n" + 
      "<li class=\"section tocHeading\">\n" + 
      "<h3 class=\"heading\">\n" + 
      "<i class=\"fa fa-plus-circle\" title=\"expand toc heading\"></i>\n" + 
      "<i class=\"fa fa-minus-circle hide\" title=\"expand toc heading\"></i>\n" + 
      "</h3>\n" + 
      "<div class=\"tocheadingarticlesloading hidden-js-div\">\n" + 
      "<img src=\"http://www.foobar.org/ServeContent?" +
      "url=http%3A%2F%2Fwww.xyz.com%2Fimages%2Fjp%2Fspinner.gif\" alt=\"Loading Accepted Manuscripts...\" />\n" + 
      "<span>Loading Accepted Manuscripts...</span>\n" + 
      "</div>\n" +
      "<div display=block><p>" +
      "<a HREF=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com" +
      "%2Fcontent%2Fjournal%2Fecosalplus%2F7%2F1%2Farticles%3Ffmt%3Dahah%26tocHeading%3D" +
      "http%3A%2F%2Fasm.metastore.ingenta.com%2Fcontent%2Fjournal%2Fecosalplus%2FreviewArticle\"" +
      " target=_blank>" + 
      "List of articles</a></div>\n" + 
      "</li>\n" + 
      "</ul>\n" + 
      "</div>\n" + 
      "<form id=\"ahahTocArticles\">\n" + 
      "<input type=\"hidden\" name=\"articleIds\" value=\"\" />\n" + 
      "<input type=\"hidden\" name=\"fmt\" value=\"ahah\" />\n" + 
      "<input type=\"hidden\" name=\"ahahcontent\" value=\"toc\" />\n" + 
      "</form>\n" + 
      "</div>\n" + 
      "</div>";
  
  static final String testContentLinkInput = 
      "<div class=\"contentTypeOptions\">\n" + 
      "<ul class=\"flat bobby fulltext\">\n" + 
      "<li class=\"html\"><a href=\"/deliver/fulltext/10.1128/9781555819453/chap2.html?itemId=/content/book/10.1128/9781555819453.chap2&amp;mimeType=html&amp;isFastTrackArticle=\" title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink html\">HTML</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "94.35\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "<li class=\"pdf\"><a href=\"/deliver/fulltext/10.1128/9781555819453/9781555819446_Chap02.pdf?itemId=/content/book/10.1128/9781555819453.chap2&amp;mimeType=pdf&amp;isFastTrackArticle=\" title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink pdf\">PDF</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "372.32\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "<li class=\"xml\" style=\"display: none;\"><a href=\"/deliver/fulltext/10.1128/9781555819453/chap2.xml?itemId=/content/book/10.1128/9781555819453.chap2&amp;mimeType=xml&amp;isFastTrackArticle=\" title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink xml\">XML</a>\n" + 
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
      "<li class=\"html\" style=\"display: block\"><a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2F10.1128%2F9781555819453%2Fchap2.html%3FitemId%3D%2Fcontent%2Fbook%2F10.1128%2F9781555819453.chap2%26amp%3BmimeType%3Dhtml%26amp%3BisFastTrackArticle%3D\" title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink html\">HTML</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "94.35\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "<li class=\"pdf\"><a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2F10.1128%2F9781555819453%2F9781555819446_Chap02.pdf%3FitemId%3D%2Fcontent%2Fbook%2F10.1128%2F9781555819453.chap2%26amp%3BmimeType%3Dpdf%26amp%3BisFastTrackArticle%3D\" title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink pdf\">PDF</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "372.32\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "<li class=\"xml\" style=\"display: none;\"><a href=\"http://www.foobar.org/ServeContent?url=http%3A%2F%2Fwww.xyz.com%2Fdeliver%2Ffulltext%2F10.1128%2F9781555819453%2Fchap2.xml%3FitemId%3D%2Fcontent%2Fbook%2F10.1128%2F9781555819453.chap2%26amp%3BmimeType%3Dxml%26amp%3BisFastTrackArticle%3D\" title=\"Opens in new window/tab\" rel=\"external\" class=\"externallink xml\">XML</a>\n" + 
      "<div class=\"fulltextsize\">\n" + 
      "140.32\n" + 
      "Kb\n" + 
      "</div>\n" + 
      "</li> \n" + 
      "</ul>\n" + 
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

  public void testXmlRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testXmlLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testXmlLinkOutput, fout);
  }

  public void testHtmlRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testHtmlLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testHtmlLinkOutput, fout);
  }

  public void testMediaLinkRewrite() throws Exception {
    MockArchivalUnit mockAu = makeAu();
    
    InputStream in = new ByteArrayInputStream(testMediaLinkInput.getBytes());
    ServletUtil.LinkTransform xfm = new ServletUtil.LinkTransform() {
      public String rewrite(String url) {
        return "http://www.foobar.org/ServeContent?url=" + url;
      }
    };
    InputStream newIn = 
        fact.createLinkRewriter("text/html", mockAu, in, "UTF8", "http://www.xyz.com/path/bar", xfm);
    String fout = StringUtil.fromReader(new InputStreamReader(newIn, "UTF-8"));
    assertEquals(testMediaLinkOutput, fout);
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
