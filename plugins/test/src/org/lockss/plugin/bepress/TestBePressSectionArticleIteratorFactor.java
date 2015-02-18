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

import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;

/**
 * <p>This is actually for {@link BePressSectionArticleIteratorFactor.Section},
 * not <code>BePressSectionArticleIteratorFactor</code>.</p>
 * @author Thib Guicherd-Callin
 */
public class TestBePressSectionArticleIteratorFactor extends ArticleIteratorTestCase {

  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
      PluginTestUtil.createAu("org.lockss.plugin.bepress.BerkeleyElectronicPressSectionPlugin",
                              ConfigurationUtil.fromArgs("base_url", "http://www.example.com/",
                                                         "journal_abbr", "jour",
                                                         "journal_section", "sect",
                                                         "volume", "123"));
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.example.com/jour/sect"),
                 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/vol123/iss4/art5");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/default/vol123/iss4/art5");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/vol123/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/default/vol123/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/iss4/art5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/default/vol123/iss4/art5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/default/vol123/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol999/iss4/art5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/vol999/iss4/art5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol999/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/vol999/iss4/editorial5");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/vol123/iss4/art5");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/default/vol123/iss4/art5");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/vol123/iss4/editorial5");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/default/vol123/iss4/editorial5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol123");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/vol123");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol123/iss4");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/vol123/iss4");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol123/iss4/art5/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/vol123/iss4/art5/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol123/iss4/editorial5/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/vol123/iss4/editorial5/wrong");
  }
  
  public void testUrlsWithoutPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/123/4/5");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/default/123/4/5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/123/4/5");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/default/123/4/5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/999/4/5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/999/4/5");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/123/4/5");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/default/123/4/5");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/123");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/123");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/123/4");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/123/4");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/123/4/5/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/default/123/4/5/wrong");
  }
  
  public void testShortArticleUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/vol123/A456");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/vol123/P456");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/vol123/R456");
    assertNotMatchesRE(pat, "http://www.wrong.com/jour/sect/vol123/S456");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/A456");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/P456");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/R456");
    assertNotMatchesRE(pat, "http://www.example.com/wrong/vol123/S456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol999/A456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol999/P456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol999/R456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol999/S456");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/vol123/A456");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/vol123/P456");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/vol123/R456");
    assertMatchesRE(pat, "http://www.example.com/jour/sect/vol123/S456");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol123");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol123/A456/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol123/P456/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol123/R456/wrong");
    assertNotMatchesRE(pat, "http://www.example.com/jour/sect/vol123/S456/wrong");
  }
  
  public void testCreateArticleFiles() throws Exception {
    String url = "http://www.example.com/jour/sect/vol123/S456";
    CachedUrl cu = au.makeCachedUrl(url);
    SubTreeArticleIterator artIter = createSubTreeIter();
    ArticleFiles af = createArticleFiles(artIter, cu);
    // even without content this will be considered an article
    // because it isn't an ambiguous, possibly TOC, url
    assertEquals(cu, af.getFullTextCu());
    assertEquals(cu, af.getRoleCu(ArticleFiles.ROLE_ABSTRACT));
  }                                                 

}
