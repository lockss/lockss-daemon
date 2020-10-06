/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

public class TestBionOne2020ArticleIteratorFactory extends ArticleIteratorTestCase {

  private static Logger log = Logger.getLogger(TestBionOne2020ArticleIteratorFactory.class);

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
