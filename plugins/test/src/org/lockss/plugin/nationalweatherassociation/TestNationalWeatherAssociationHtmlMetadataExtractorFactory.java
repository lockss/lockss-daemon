/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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