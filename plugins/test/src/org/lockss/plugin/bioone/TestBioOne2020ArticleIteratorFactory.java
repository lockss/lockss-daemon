/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.bioone;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;

import java.util.regex.Pattern;

public class TestBioOne2020ArticleIteratorFactory extends ArticleIteratorTestCase {

  private static Logger log = Logger.getLogger(TestBioOne2020ArticleIteratorFactory.class);

  private static final String PLUGIN_ID = "org.lockss.plugin.bioone.BioOne2020Plugin";
  private ArchivalUnit mau;
  private String tempDirPath;
  private MockLockssDaemon daemon;
  private PluginManager pluginMgr;

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content

  public void startMockDaemon() {
    daemon = getMockLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private final String BASE_URL = "https://bioone.org/";
  private final String JOURNAL_ID = "canadian-journal-of-animal-science";
  private final String YEAR = "2019";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
          BASE_URL_KEY, BASE_URL,
          JOURNAL_ID_KEY, JOURNAL_ID,
          YEAR_KEY, YEAR);

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
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
            PluginTestUtil.createAndStartAu(PLUGIN_ID, AU_CONFIG);
  }
  
  public void testUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    assertMatchesRE(pat, "https://bioone.org/journals/canadian-journal-of-animal-science/volume-91/issue-3/CJAS2010-004/Ecoregion-and-farm-size-differences-in-dairy-feed-and-manure/10.1139/CJAS2010-004.full");
    assertMatchesRE(pat, "https://bioone.org/journals/canadian-journal-of-animal-science/volume-99/issue-1/cjas-2017-0087/Dehydrated-citrus-pulp-in-broiler-diets/10.1139/cjas-2017-0087.full");
    assertMatchesRE(pat, "https://bioone.org/journalArticle/Download?fullDOI=10.1139%2Fcjas-2016-0174");
    
    assertNotMatchesRE(pat, "https://bioone.org/journal/canadian-journal-of-animal-science/volume-99/issue-1/cjas-2017-0087/Dehydrated-citrus-pulp-in-broiler-diets/10.1139/cjas-2017-0087");
    assertNotMatchesRE(pat, "https://bioone.org/canadian-journal-of-animal-science/cjas-2017-0087/Dehydrated-citrus-pulp-in-broiler-diets/10.1139/cjas-2017-0087.full");
  }

}
