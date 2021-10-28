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

package org.lockss.plugin.spandidos;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.test.*;
import org.lockss.util.Constants;
import org.lockss.util.ListUtil;
import org.lockss.util.Logger;

public class TestSpandidosArticleIteratorFactory extends ArticleIteratorTestCase {

  private static Logger log = Logger.getLogger(TestSpandidosArticleIteratorFactory.class);

  private static final String PLUGIN_ID = "org.lockss.plugin.spandidos.SpandidosPlugin";

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
  private final String BASE_URL = "https://www.spandidos-publications.com/";
  private final String JOURNAL_ID = "ol";
  private final String VOLUME_NAME = "6";
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

    assertMatchesRE(pat, "https://www.spandidos-publications.com/10.3892/ol.2020.11880");
    assertMatchesRE(pat, "https://www.spandidos-publications.com/10.3892/ol.2020.11984");

    assertNotMatchesRE(pat, "https://www.spandidos-publications.com/ol/6/1");
    assertNotMatchesRE(pat, "https://www.spandidos-publications.com/jid/5/4");
  }

}
