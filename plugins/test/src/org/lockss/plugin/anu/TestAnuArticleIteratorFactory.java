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

package org.lockss.plugin.anu;

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

public class TestAnuArticleIteratorFactory extends ArticleIteratorTestCase {

  private static Logger log = Logger.getLogger(TestAnuArticleIteratorFactory.class);

  private static final String PLUGIN_ID = "org.lockss.plugin.anu.ClockssAnuPlugin";
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
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "https://press.anu.edu.au/";
  private final String JOURNAL_ID = "australian-journal-biography-and-history";
  private final String VOLUME_NAME = "5";
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
    Pattern pdf = AnuArticleIteratorFactory.PDF_PATTERN;
    // make sure the first pass pattern succeeds
    assertMatchesRE(pat, "https://press-files.anu.edu.au/downloads/press/n8684/pdf/01_white.pdf");
    assertMatchesRE(pat, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/Notes_documents4.pdf");
    assertMatchesRE(pat, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/book.pdf");
    assertMatchesRE(pat, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/ch0154.pdf");
    assertMatchesRE(pat, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/ch0748.pdf");
    assertMatchesRE(pat, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/contributors26.pdf");
    assertMatchesRE(pat, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/preface17.pdf");
    // and then the actual pdf files
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/n8684/pdf/10_goot_rowse.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/n8684/pdf/book_review_01_allbrook.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/ch0651.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/ch0748.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/contributors26.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/preface17.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/prelims48.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p74151/pdf/reviews.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p91411/pdf/8-1-A-1.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p92031/pdf/8-4-BR-1.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p92031/pdf/8-4-NA-1.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p12311/pdf/11_review_hr1_2000.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/p12311/pdf/1_cover_hr1_2000.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/n7524/pdf/hong_kong_revolt.pdf");
    assertMatchesRE(pdf, "https://press-files.anu.edu.au/downloads/press/n7524/pdf/hunting_sorcerers.pdf");
  }

}

