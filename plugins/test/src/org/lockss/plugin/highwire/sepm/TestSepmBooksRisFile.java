/*
 * $Id:$
 */
/*

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
 all rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of his software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.highwire.sepm;

import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestSepmBooksRisFile extends LockssTestCase {

  static Logger log = Logger.getLogger(TestSepmBooksRisFile.class);

  private MockArchivalUnit mau;
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME = "org.lockss.plugin.highwire.sepm.ClockssSepmBooksH20Plugin";
  private static String BASE_URL = "http://www.source.org/";
  // Using the getResourceAsStream() will find these in the current directory
  private static final String realRisFile = "sepm_procite.ris";


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
    new SepmBooksRisMetadataExtractorFactory();
  }
  
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "scnotes");
    return conf;
  }
  
  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }
  

  public void testFromRisFile() throws Exception {
      InputStream file_input = null;
      try {
        file_input = getResourceAsStream(realRisFile);
        String string_input = StringUtil.fromInputStream(file_input);
        IOUtil.safeClose(file_input);

        //http://scnotes.sepmonline.org/citmgr?type=procite&gcadoi=10.2110%2Fsepmscn.054.001
        CIProperties risHeader = new CIProperties();   
        String ris_url = BASE_URL + "citmgr?type=procite&gcadoi=10.2110%2Fsepmscn.054.001";
        risHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");
        MockCachedUrl mcu = mau.addUrl(ris_url, true, true, risHeader);
        mcu.setContent(string_input);
        mcu.setContentSize(string_input.length());
        mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");

      FileMetadataExtractor me = new SepmBooksRisMetadataExtractorFactory().createFileMetadataExtractor(
          MetadataTarget.Any(), "text/plain");
        FileMetadataListExtractor mle =
            new FileMetadataListExtractor(me);
        List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
        assertNotEmpty(mdlist);
        log.debug3(mdlist.get(0).ppString(2));
      } finally {
        IOUtil.safeClose(file_input);
      }
  }

}
