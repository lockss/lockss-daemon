/*
 * $Id: TestHighWirePressH20ArticleIteratorFactory.java,v 1.1 2013-09-16 22:06:45 etenbrink Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;

public class TestHighWirePressH20ArticleIteratorFactory extends ArticleIteratorTestCase {

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content

  private final String PLUGIN_NAME = "org.lockss.plugin.highwire.ClockssHighWirePressH20Plugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://bjo.bmj.com/";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, "1");
  private String BASE_AU_URL = BASE_URL + "content/1/";
  private static final int DEFAULT_FILESIZE = 3000;

  protected String cuRole = null;
  ArticleMetadataExtractor.Emitter emitter;
  protected boolean emitDefaultIfNone = false;
  FileMetadataExtractor me = null;
  MetadataTarget target;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }

  @Override
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME,  AU_CONFIG);
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(VOLUME_NAME_KEY, "1");
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "2");
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_HTML);
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }


  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_AU_URL), getRootUrls(artIter));
  }


  //
  // We are set up to match any of "<base_url>content/<vol>/<iss>/<pg>.full(.pdf)"
  //

  public void testUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertMatchesRE(pat, "http://bjo.bmj.com/content/1/1/1.full");
    assertMatchesRE(pat, "http://bjo.bmj.com/content/1/1/1.full.pdf");
    // but not to ...
    assertNotMatchesRE(pat, "http://bjo.bmj.com/content/1/1/1.full.pdf+html");
    assertNotMatchesRE(pat, "http://bjo.bmj.com/content/1/1/1.abstract");
    assertNotMatchesRE(pat, "http://bjo.bmj.com/content/1/1/1.extract");
    assertNotMatchesRE(pat, "http://bjo.bmj.com/email?gca=bjophthalmol;96/1/1&current-view-path=/content/96/1/1.extract");

    // wrong base url
    assertNotMatchesRE(pat, "http://ametsoc.org/bitstream/handle/foobar");
  }

  //
  // We are set up to match any of "<base_url>content/<vol>/.*[.]body"
  //

  public void testBookUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertMatchesRE(pat, "http://bjo.bmj.com/content/1/SEC8.body");
    assertMatchesRE(pat, "http://bjo.bmj.com/content/1/SEC131/SEC133.body");
    assertMatchesRE(pat, "http://bjo.bmj.com/content/1/SEC131/SEC133/SEC178.body");
    // but not to ...
    assertNotMatchesRE(pat, "http://bjo.bmj.com/content/1/SEC131/SEC133.extract");
  }

  //
  // simAU was created with only one depth
  // 1 filetype (html) and 2 files of each type
  // So the total number of files of all types is 2
  // simAU file structures looks like this branch01/01file.html
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with HighWirePressH20:
     *  <base_url>content/<vol>/<iss>/pg.full
     */

    String pat1 = "(\\d+)file[.]html";
    // turn xxfile.html into full text
    String repAbs = "/content/1/1/$1.full";
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat1, repAbs);

    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullText= 0;
    int countMetadata = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      count ++;
      //log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
        ++countFullText;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      if (cu != null) {
        ++countMetadata;
      }
    }
    // potential article count is 2 (1 branches * 2 files each branch) = 2
    int expCount = 2;

    log.debug3("Article count is " + count);
    assertEquals(expCount, count);

    // you will get a full text for ALL articles
    assertEquals(expCount, countFullText);
    
    // you will get metadata for ALL articles
    assertEquals(expCount, countMetadata); //don't count the one where we removed everything
  }
}
