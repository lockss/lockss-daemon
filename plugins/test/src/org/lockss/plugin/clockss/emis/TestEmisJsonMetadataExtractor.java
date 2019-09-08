/*
 * Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 */

package org.lockss.plugin.clockss.emis;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.CIProperties;
import org.lockss.util.IOUtil;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;

public class TestEmisJsonMetadataExtractor extends LockssTestCase {

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String BASE_URL = "http://www.source.org/";
  private static final String json_article_url = BASE_URL + "2019/store/10/01/01/01/mif.json";
  private static final String pdf_url = BASE_URL + "2019/store/10/01/01/01/003.pdf";
  private static final String[] Authors = {"Harjulehto, Petteri", "H\\\"ast\\\"o, Peter"};
  private static final List<String> AUTHORS = ListUtil.fromArray(Authors);
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    mau = new MockArchivalUnit();

    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau.setConfiguration(auConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configuration method.
   * @return a configuration object.
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2017");
    return conf;
  }



  private static final String realJsonFile = "mif1.json";

  public void testFromArticleJsonFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realJsonFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      CIProperties xmlHeader = new CIProperties();
      xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/json");
      MockCachedUrl mcu = mau.addUrl(json_article_url, true, true, xmlHeader);
      // Now add all the pdf files in our AU since we check for them before emitting
      mau.addUrl(pdf_url, true, true, xmlHeader);

      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());
      mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/json");

      FileMetadataExtractor me = new EmisJsonMetadataExtractorFactory().createFileMetadataExtractor(
          MetadataTarget.Any(), "application/json");
      FileMetadataListExtractor mle =
          new FileMetadataListExtractor(me);
      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(1, mdlist.size());

      // check each returned md against expected values
      Iterator<ArticleMetadata> mdIt = mdlist.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        assertEquals(mdRecord.get(MetadataField.FIELD_PUBLICATION_TITLE),"Ann. Acad. Sci. Fenn., Math.");
        assertEquals(mdRecord.get(MetadataField.FIELD_ARTICLE_TITLE),"Lebesgue points in variable exponent spaces");
        assertEquals(mdRecord.get(MetadataField.FIELD_ACCESS_URL),"http://www.emis.de/journals/AASF/Vol29/harjul.html");
        assertEquals(mdRecord.get(MetadataField.FIELD_ISSN),"1239-629X");
        assertEquals(mdRecord.get(MetadataField.FIELD_VOLUME), "29");
        assertEquals(mdRecord.get(MetadataField.FIELD_ISSUE),"2");
        assertEquals(mdRecord.get(MetadataField.FIELD_DATE),"2004");
        assertEquals(mdRecord.getList(MetadataField.FIELD_AUTHOR),AUTHORS);
      }
    }finally {
      IOUtil.safeClose(file_input);
    }

  }

}
