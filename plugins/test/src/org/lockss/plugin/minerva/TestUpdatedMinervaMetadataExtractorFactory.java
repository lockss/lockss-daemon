/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.minerva;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.minerva.UpdatedMinervaMetadataExtractorFactory.UpdatedMinervaMetadataExtractor;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the xml source for this plugin is:
 * http://minerva.mic.ul.ie/vol12/Seclusion.pdf
 */
public class TestUpdatedMinervaMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestUpdatedMinervaMetadataExtractorFactory");

  private MockLockssDaemon theDaemon;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
  }
  
  String goodIssn = "5555-5555";
  String goodJournal = "Minerva - An Internet Journal of Philosophy";
  String goodVolume = "66";
  String goodStartPage = "123";
  String goodEndPage = "134";
  String goodDate = "2012";
  String goodAuthor = "John A. Author";
  String goodArticle = "This is a Test Article PDF ";
  
  public void testExtractFromGoodContent() throws Exception {
    UpdatedMinervaMetadataExtractor me =
      new UpdatedMinervaMetadataExtractorFactory.UpdatedMinervaMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    
    ArticleMetadata am = me.extractFrom((InputStream)(new FileInputStream(
    		new File("./plugins/test/src/org/lockss/plugin/minerva/TestMetadata.pdf"))));
    assertNotNull(am);
    
    assertEquals(goodIssn, am.get(MetadataField.FIELD_ISSN));
    assertEquals(goodJournal, am.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodVolume, am.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodStartPage, am.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodEndPage, am.get(MetadataField.FIELD_END_PAGE));
    assertEquals(goodDate, am.get(MetadataField.FIELD_DATE));
    assertEquals(goodAuthor, am.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticle, am.get(MetadataField.FIELD_ARTICLE_TITLE));
  }
}