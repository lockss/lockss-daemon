/* 
 * $Id$
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

package org.lockss.plugin.heterocycles;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/* 
 * Tests only issue number since other metadata comes from the tdb file.  
 * The issue number is extracted from the pdf url:
 *      <heterocyclesbase>.com/clockss/downloads/PDF/23208/83/1    
 */
public class TestHeterocyclesMetadataExtractorFactory extends LockssTestCase {
  
  static Logger log = Logger.getLogger(
      TestHeterocyclesMetadataExtractorFactory.class);

  private MockLockssDaemon theDaemon;
  private ArchivalUnit hau;
  
  private static final String PLUGIN_NAME = 
      "org.lockss.plugin.heterocycles.ClockssHeterocyclesPlugin";

  private static final String BASE_URL = "http://www.example.com/";
  private static final String VOLUME_NAME = "87";

  public void setUp() throws Exception {
    super.setUp();
    // even though you don't use path, you need to call method setUpDiskSpace
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, heterocyclesAuConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  private Configuration heterocyclesAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", VOLUME_NAME);
    return conf;
  }
  
  String goodIssue = "5";
  
  public void testExtract() throws Exception {
    
    String pdfUrl = "http://www.example.com/clockss/downloads/PDF/99999/87/5";
    
    MockCachedUrl cu = new MockCachedUrl(pdfUrl, hau);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");

    // Create FileMetadataExtractor object through 
    // HeterocyclesMetadataExtractorFactory().
    FileMetadataExtractor me = new HeterocyclesMetadataExtractorFactory
                                             .HeterocyclesMetadataExtractor();
    
    // Create the metadata list for this AU.
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
  }
  
}