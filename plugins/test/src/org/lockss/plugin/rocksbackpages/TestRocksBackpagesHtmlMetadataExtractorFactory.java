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


package org.lockss.plugin.rocksbackpages;

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
public class TestRocksBackpagesHtmlMetadataExtractorFactory 
  extends LockssTestCase {
  
  static Logger log = Logger.getLogger(
    TestRocksBackpagesHtmlMetadataExtractorFactory.class);

  private MockLockssDaemon theDaemon;
  private ArchivalUnit nau;
  
  private static final String PLUGIN_NAME = "org.lockss.plugin."
      + "rocksbackpages.RocksBackpagesPlugin";

  private static final String BASE_URL = "http://www.example.com/";
  private static final String JID = "rolling-stone";
  private static final String YEAR = "1967";
 
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
  
  String goodPublication = "Rolling Stone";
  String goodDate = "9 November 1967";
  String goodAuthor = "Michael Lydon";
    
  // a chunk of html source code from the publisher's site from where the 
  // metadata should be extracted
  String goodContent = 
        "<h1 class=\"article\"> Where’s the Money from Monterey Pop?</h1>"

        +"<p class=\"article-details\">"
            +"<span class=\"writer\">Michael Lydon</span>,"
            +"<span class=\"publication\">Rolling Stone</span>,"
            +"<span class=\"date\">9 November 1967</span>"
        +"</p>";
        
  public void testExtractFromGoodContent() throws Exception {
    
    String absUrl = 
    "https://www.example.com/Library/Publication/rolling-stone?year=1967";
    
    MockCachedUrl cu = new MockCachedUrl(absUrl, nau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");

    // Create FileMetadataExtractor object through
    // NWAMetadataExtractorFactory().
    FileMetadataExtractor me = 
        new RocksBackpagesHtmlMetadataExtractorFactory.
                              RocksBackpagesMetadataExtractor();
    
    // Create the metadata list for this AU.
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodPublication, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
  }
  
}


