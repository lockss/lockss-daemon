/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.jasper;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.SingleNodeCachedUrlSetSpec;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;

import java.io.IOException;
import java.util.regex.Pattern;

public class TestJasperArticleIteratorFactory extends ArticleIteratorTestCase {

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content

  private final String PLUGIN_NAME = "org.lockss.plugin.jasper.ClockssJasperPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String COLLECTION_KEY = ConfigParamDescr.COLLECTION.getKey();
  private final String BASE_URL = "https://archive.org/";
  private final String DOWNLOAD_URL = "https://archive.org/download/";
  private final String COLLECTION = "111113";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      COLLECTION_KEY, COLLECTION,
      "user_pass", "user:pass");
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

    ConfigurationUtil.addFromArgs(CachedUrl.PARAM_ALLOW_DELETE, "true");
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
    conf.put(BASE_URL_KEY, DOWNLOAD_URL);
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "4");
    conf.put("fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_HTML |
            SimulatedContentGenerator.FILE_TYPE_PDF ));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    log.info("Pattern is: " + pat.toString());
    String targzPath = COLLECTION + "/rama-2021-06-25-11-25-53.tar.gz!";
    String metadataPath = "/2051-5960/00003741594643f4996e2555a01e03c7/data/metadata/metadata.json";
    assertMatchesRE(pat, DOWNLOAD_URL + targzPath + metadataPath);
  }
/*
  //
  // simAU was created with only one depth, but 5 branches
  // 3 filetypes (html & pdf & txt) and 4 files of each type
  // So the total number of files of all types is 40 (5 * (4*2)) + 4 RIS (handled independently)
  // simAU file structures looks like this branch01/01file.html or branch04/08file.pdf, etc
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    //  Go through the simulated content you just crawled and modify the results to emulate
    //  what you would find in a "real" crawl with Atypon:
    //  <base_url>/doi/{abs,pdf,full,pdfplus}/10.1137/X.XXXXXX
    //  Currently this test doesn't create any "ref" or "suppl" files

    // turn xxfile.html in to both abstracts and fulls
    String pat1 = "branch(\\d+)/(\\d+)file\\.html";
    String pdf = "JournalofSuccessfulDataTransfers/rama-2021-06-25-11-25-53\\.tar\\.gz!/$1/data/$2\\.pdf";
    String json = "JournalofSuccessfulDataTransfers/rama-2021-06-25-11-25-53\\.tar\\.gz!/$1/data/metadata/metadata\\.json";
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, pdf);
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat1, json);

    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
      String url = cu.getUrl();
      log.info(url);
    }

    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullText= 0;
    int countMetadata = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info(af.toString());
      count ++;
      //log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
        ++countFullText;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      if (cu != null) {
        ++countMetadata; //could be ris file or abstract or full
      }
    }
    // potential article count is 20 (5 branches * 4 files each branch) = 20 (4 RIS files don't count as articles)
    // subtract the one where we removed everything
    // subtract the one for abstract only. This would only count as an article
    // in an "abstracts only AU
    int expCount = 1;

    log.debug3("Article count is " + count);
    assertEquals(expCount, count);

    // you will only get a full text for combos with pdf, pdfplus or full
    // so there are only 2 cases - the one with nothing; the one with only abstract;
    assertEquals(1, countFullText);

    // you need to have either an abstract or a full text to have metadata 4 cases don't have either
    // we're only going to get the right information if the TARGET is not NULL and set to other than isArticle()
    //assertEquals(16, countMetadata);
    //Now that we pick up downloaded RIS urls, we should always have this
    // note that with only an abstract, we no longer create an artilefiles
    assertEquals(expCount, countMetadata); //don't count the one where we removed everything
  }
*/
  private void deleteBlock(CachedUrl cu) throws IOException {
    //log.info("deleting " + cu.getUrl());
    cu.delete();
  }
}
