/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.taylorandfrancis;

import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.*;
import org.lockss.util.ListUtil;

/*
 * This is a very limited test. Most of the ArticleIterator is tested by the TestBaseAtyponArticleIteratorFactory
 * but Taylor & Francis overrides the iterator in order to ignore certain URLs (an T&F bug)
 * so this  just tests that additional functionality. 
 */

public class TestTafArticleIteratorFactory extends ArticleIteratorTestCase {

  private SimulatedArchivalUnit sau;      // Simulated AU to generate content
  private final String PLUGIN_NAME = "org.lockss.plugin.taylorandfrancis.ClockssTaylorAndFrancisPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.tandfonline.com/";
  private final String JOURNAL_ID = "xxxx";
  private final String VOLUME_NAME = "123";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      JOURNAL_ID_KEY, JOURNAL_ID,
      VOLUME_NAME_KEY, VOLUME_NAME);

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
    conf.put("depth", "1");
    conf.put("branch", "5");
    conf.put("numFiles", "3");
    conf.put("fileTypes",
        "" + SimulatedContentGenerator.FILE_TYPE_HTML);
    conf.put("binFileSize", ""+"3000");
    return conf;
  }


  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + "doi/"),
        getRootUrls(artIter));
  }


  //
  // We are set up to match any of pdf|full|pdfplus
  //

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // we match to doi/(abs|full|pdf|pdfplus)
    assertMatchesRE(pat, "http://www.tandfonline.com/doi/pdf/10.1137/100818522"); 
    // abstract is only a match if "abstract only" type of au
    assertNotMatchesRE(pat, "http://www.tandfonline.com/doi/abs/10.1137/100818522");
    assertMatchesRE(pat, "http://www.tandfonline.com/doi/full/10.1137/100818522");
    assertMatchesRE(pat, "http://www.tandfonline.com/doi/pdfplus/10.1137/100818522");
    // and this is the problem URL
    // eg: http://www.tandfonline.com/doi/abs/10.2989/null?sequence=tsfs20%2F2011%2Ftsfs20.v073.i01%2Ftsfs20.v073.i01%2Fproduction
    assertMatchesRE(pat, "http://www.tandfonline.com/doi/full/10.2989/null?sequence=tsfs20%2F2011%2Ftsfs20.v073.i01%2Ftsfs20.v073.i01%2Fproduction");
  }
  
  // Set up some content to test generation of ArticleFiles  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with Atypon:
     *  <base_url>/doi/{abs,full}/10.1137/X.XXXXXX
     *  also create the bogus URLs that we hope to have excluded...
     */

    // turn xxfile.html in to both abstracts and fulls and bad URLs
    // make DOI include a slash in suffix
    String pat1 = "branch(\\d+)/(\\d+)file\\.html";
    String repAbs = "doi/abs/10.1137/b$1/art$2";
    String repFull = "doi/full/10.1137/b$1/art$2";
    String repBad1 = "doi/abs/10.1137/null?sequence=b$1%2Fart$2%2Fproduction";
    String repBad2 = "doi/full/10.1137/null?sequence=b$1%2Fart$2%2Fproduction";

    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, repAbs);
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, repFull);
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, repBad1);
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, repBad2);
    
    // at this point we have 5 branches with 3 articles each (or 15 articles)
    // but the bogus URLs would make it look like more if they weren't 
    // successfully excluded
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullText= 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      count ++;
      log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
        ++countFullText;
      }
    }
    int expCount = 15; 

    log.debug3("Article count is " + count);
    assertEquals(expCount, count);
    assertEquals(expCount, countFullText);
  }
}
