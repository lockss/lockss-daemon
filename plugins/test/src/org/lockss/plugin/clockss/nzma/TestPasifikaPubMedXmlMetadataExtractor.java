/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.nzma;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestPasifikaPubMedXmlMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestPasifikaPubMedXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String BASE_URL = "http://www.source.org/";
  
  /*
   * Set up the metadata expected for each of the above tests
   */
  private static final String pdfUrl1 = "http://www.source.com/2017/NZMJv130i1452.pdf";
  //each PDF is the entire issue so multiple articles will point to the same pdf

  private static CIProperties xmlHeader = new CIProperties();
  private static String xml_url = "http://www.source.com/2017/NZMJv130i1452.xml";
  private MockCachedUrl mcu;
  private FileMetadataExtractor me;
  private FileMetadataListExtractor mle;

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

    // the following is consistent across all tests; only content changes
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    mau.addUrl(pdfUrl1, true, true, xmlHeader);

    me = new PasifikaPubMedXmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/xml");
    mle = new FileMetadataListExtractor(me);
    
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2017");
    return conf;
  }
 
  private static final String realXMLFile = "NZMATest.xml";


  
  public void testFromXMLFile() throws Exception {
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realXMLFile);
      String string_input = StringUtil.fromInputStream(file_input);
      IOUtil.safeClose(file_input);

      // set up the content for this test
      mcu.setContent(string_input);
      mcu.setContentSize(string_input.length());

      List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
      assertNotEmpty(mdlist);
      assertEquals(3, mdlist.size());
      ArticleMetadata mdRecord = mdlist.get(0);
      assertNotNull(mdRecord);
      //log.info(mdRecord.ppString(2));

    }finally {
      IOUtil.safeClose(file_input);
    }

  }
  

}
