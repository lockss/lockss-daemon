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

package org.lockss.plugin.georgthiemeverlag;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

public class TestGeorgThiemeVerlagPdfMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(TestGeorgThiemeVerlagPdfMetadataExtractorFactory.class);
  
  private SimulatedArchivalUnit sau;  // Simulated AU to generate content
  private static String PLUGIN_NAME = 
      "org.lockss.plugin.georgthiemeverlag.ClockssGeorgThiemeVerlagPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  
  private static final String BASE_URL = "http://www.example.com/";
  private final String JOURNAL_ID = "10.1055/s-00000002";
  private final String VOLUME_NAME = "2010";
  
  //GeorgThiemeVerlag AU
  private ArchivalUnit hau; 
  private MockLockssDaemon theDaemon;

  private static String SIM_ROOT = BASE_URL;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(SimulatedPlugin.class,
        simAuConfig(tempDirPath));
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, georgthiemeverlagAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put(JOURNAL_ID_KEY, JOURNAL_ID);
    conf.put(VOLUME_NAME_KEY, VOLUME_NAME);
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "2");
    conf.put("fileTypes", "" +
        SimulatedContentGenerator.FILE_TYPE_HTML +
        SimulatedContentGenerator.FILE_TYPE_PDF);
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration georgthiemeverlagAuConfig() {
     Configuration conf = ConfigManager.newConfiguration();

    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(JOURNAL_ID_KEY, JOURNAL_ID);
    conf.put(VOLUME_NAME_KEY, VOLUME_NAME);
    return conf;
  }

  String goodDate = "2010/08/25";
  String goodSubject = "Subj";
  String goodArticle = "Some Article Title";
  String goodAuthor = "O. A. Auth";
  String goodDoi = "10.1055/s-0029-1215009";
  String goodVol = "6";
  String journalTitle = "Aktuelle Dermatologie";
  String goodPublisher = "Georg Thieme Verlag KG";

  String goodContent = " Arbitrary content\n";

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/ejournals/pdf/10.1055/s-0029-1215009.pdf";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
    FileMetadataExtractor me = 
        new GeorgThiemeVerlagPdfMetadataExtractorFactory.
            GeorgThiemeVerlagPdfMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertNull(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertNull(goodArticle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(goodVol, md.get(MetadataField.FIELD_VOLUME));
    assertNull(journalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    // the following value now hardcoded
    // assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    
    // the following does not exist in test data
    assertNull(md.get(MetadataField.FIELD_END_PAGE));
  }

  String badContent = " Arbitrary content\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me = 
        new GeorgThiemeVerlagPdfMetadataExtractorFactory.
            GeorgThiemeVerlagPdfMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
  }
  
}