/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.bepress;

import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;
import org.lockss.plugin.bepress.TestBePressMetadataExtractor.MySimulatedPlugin;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestBePressArticleIteratorFactory extends ArticleIteratorTestCase {
  
  static Logger log = Logger.getLogger("TestBePressArticleIterator");
  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
      PluginTestUtil.createAu("org.lockss.plugin.bepress.BePressPlugin",
                              ConfigurationUtil.fromArgs("base_url", "http://www.example.com/",
                                                         "journal_abbr", "jour",
                                                         "volume", "123"));
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.example.com/jour"),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/vol123/iss4/art5");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/default/vol123/iss4/art5");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/vol123/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/default/vol123/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/iss4/art5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/default/vol123/iss4/art5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/default/vol123/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol999/iss4/art5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/default/vol999/iss4/art5");    
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol999/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/default/vol999/iss4/editorial5");
    
    assertMatchesRE(pat, "http://www.example.com/jour/vol123/iss4/art5");
    assertMatchesRE(pat, "http://www.example.com/jour/default/vol123/iss4/art5");
    assertMatchesRE(pat, "http://www.example.com/jour/vol123/iss4/editorial5");
    assertMatchesRE(pat, "http://www.example.com/jour/default/vol123/iss4/editorial5");
    
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol123");
    assertNotMatchesRE(pat, "http://www.example.com/jour/default/vol123");
    
    assertMatchesRE(pat, "http://www.example.com/jour/vol123/iss4");
    assertMatchesRE(pat, "http://www.example.com/jour/default/vol123/iss4");
    
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol123/iss4/art5/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/default/vol123/iss4/art5/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol123/iss4/editorial5/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/default/vol123/iss4/editorial5/wrong");
  }
  
  public void testUrlsWithoutPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/123/4/5");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/default/123/4/5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/123/4/5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/default/123/4/5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/999/4/5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/default/999/4/5");
    
    assertMatchesRE(pat, "http://www.example.com/jour/123/4/5");
    assertMatchesRE(pat, "http://www.example.com/jour/default/123/4/5");
    
    assertNotMatchesRE(pat, "http://www.example.com/jour/123");
    assertNotMatchesRE(pat, "http://www.example.com/jour/default/123");
    
    assertMatchesRE(pat, "http://www.example.com/jour/123/4");
    assertMatchesRE(pat, "http://www.example.com/jour/default/123/4");
    
    assertNotMatchesRE(pat, "http://www.example.com/jour/123/4/5/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/default/123/4/5/wrong");
  }
  
  public void testShortArticleUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/vol123/A456");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/vol123/P456");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/vol123/R456");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/vol123/S456");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/A456");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/P456");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/R456");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/S456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol999/A456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol999/P456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol999/R456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol999/S456");
    assertMatchesRE(pat, "http://www.example.com/jour/vol123/A456");
    assertMatchesRE(pat, "http://www.example.com/jour/vol123/P456");
    assertMatchesRE(pat, "http://www.example.com/jour/vol123/R456");
    assertMatchesRE(pat, "http://www.example.com/jour/vol123/S456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol123");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol123/A456/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol123/P456/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol123/R456/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/vol123/S456/wrong");
  }
  
  private String articleContent=
      "<head>" +
          "<title>The Economists' Voice</title>" +
          "<!-- FILE article_meta-tags.inc --><!-- FILE: /main/production/doc/data/assets/site/article_meta-tags.inc -->" +
          "<meta name=\"bepress_citation_journal_title\" content=\"The Economists' Voice\">" +
          "<meta name=\"bepress_citation_authors\" content=\"DeAuthor, J. Smarty\">" +
          "<meta name=\"bepress_citation_title\" content=\"An Article Title?\">" +
          "<meta name=\"bepress_citation_date\" content=\"09/15/2004\">" +
          "<meta name=\"bepress_citation_volume\" content=\"1\">" +
          "<meta name=\"bepress_citation_issue\" content=\"1\">" +
          "<meta name=\"bepress_citation_firstpage\" content=\"1\">" +
          "<meta name=\"bepress_citation_pdf_url\" content=\"http://www.bepress.com/cgi/viewcontent.cgi?article=1000&amp;context=ev\">" +
          "<meta name=\"bepress_citation_abstract_html_url\" content=\"http://www.bepress.com/ev/vol1/iss1/art1\">" +
          "<meta name=\"bepress_citation_publisher\" content=\"bepress\">" +
          "<meta name=\"bepress_citation_doi\" content=\"10.2202/1111-11111.1000\">" +
          "<!-- FILE: article_meta-tags.inc (cont) -->" +
          "<meta  http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" +
          "</head>";

  private String tocContent = 
      "<head>" +
          "<title>The Economists' Voice</title>" +
          "<meta  http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">" +
          "</head>";

  /*
   * Now that we check for metadata to verify that a cu represents and
   * article before creating the ArticleFiles, null content will mean no AF
   */
  public void testCreateArticleFiles() throws Exception {
    String art_url = "http://www.example.com/jour/vol123/iss2/art1";
    String ambig_url = "http://www.example.com/jour/vol123/iss2";
    CachedUrl art_cu = au.makeCachedUrl(art_url);
    MockCachedUrl ambig_cu = new MockCachedUrl(ambig_url, au);
    
    SubTreeArticleIterator artIter = createSubTreeIter();
    
    // test against a CU that has a definite article structure (so content isn't even checked)
    ArticleFiles af = createArticleFiles(artIter, art_cu);
    assertEquals(art_cu, af.getFullTextCu());
    assertEquals(art_cu, af.getRoleCu(ArticleFiles.ROLE_ABSTRACT));
    
    // test against a CU that could either be a TOC or an article
    // null content will mean this isn't considered an article, so no AF
    af = createArticleFiles(artIter, ambig_cu);
    assertEquals(null, af);
    
    // the html matches that of an article abstract
    ambig_cu.setContent(articleContent);
    ambig_cu.setContentSize(articleContent.length());
    ambig_cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    af = createArticleFiles(artIter, ambig_cu);
    assertEquals(ambig_cu, af.getFullTextCu());
    assertEquals(ambig_cu, af.getRoleCu(ArticleFiles.ROLE_ABSTRACT));
    
    // the html matches that of a table of contents head section
    ambig_cu.setContent(tocContent);
    ambig_cu.setContentSize(tocContent.length());
    ambig_cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    af = createArticleFiles(artIter, ambig_cu);
    assertEquals(null, af);
  }						    

}
