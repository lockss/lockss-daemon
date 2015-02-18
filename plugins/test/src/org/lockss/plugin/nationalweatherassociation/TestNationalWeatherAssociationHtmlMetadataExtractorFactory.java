/* $Id$ */

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

package org.lockss.plugin.nationalweatherassociation;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/* 
 * Tests metadata extracted from abstracts.
 *   <nwabase>.org/xjid/abstracts/2013/2013-XJID10/abstract.php    
 */
public class TestNationalWeatherAssociationHtmlMetadataExtractorFactory 
  extends LockssTestCase {
  
  static Logger log = Logger.getLogger(
      TestNationalWeatherAssociationHtmlMetadataExtractorFactory.class);

  private MockLockssDaemon theDaemon;
  private ArchivalUnit nau;
  
  private static final String PLUGIN_NAME = "org.lockss.plugin."
      + "nationalweatherassociation.NationalWeatherAssociationPlugin";

  private static final String BASE_URL = "http://www.example.com/";
  private static final String JID = "xjid";
  private static final String YEAR = "2013";
 
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

    nau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, nwaAuConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  private Configuration nwaAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JID);
    conf.put("year", YEAR);
    return conf;
  }
  
  // the metadata that should be extracted
  
  String goodLanguage = "EN-US";
  String goodDate = "2012-02-22";
  String goodAuthor = "NXX IT Committee";
    
  // a chunk of html source code from the publisher's site from where the 
  // metadata should be extracted
  String goodContent = 
      "<meta name=\"creator\" content=\"NXX IT Committee\" />"
        + "<meta name=\"date.created\" scheme=\"ISO1111\" content=\"2012-02-22\" />"
        + "<meta name=\"language\" scheme=\"DCS.RXXXX\" content=\"EN-US\" />";
        
  public void testExtractFromGoodContent() throws Exception {
    
    String absUrl = 
        "http://www.example.com/xjid/abstracts/2013/2013-XJID10/abstract.php";
    
    MockCachedUrl cu = new MockCachedUrl(absUrl, nau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");

    // Create FileMetadataExtractor object through
    // NWAMetadataExtractorFactory().
    FileMetadataExtractor me = 
        new NationalWeatherAssociationHtmlMetadataExtractorFactory.
                              NationalWeatherAssociationHtmlMetadataExtractor();
    
    // Create the metadata list for this AU.
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodLanguage, md.get(MetadataField.FIELD_LANGUAGE));
  }
  
}