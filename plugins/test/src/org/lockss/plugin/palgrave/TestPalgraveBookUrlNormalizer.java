/*
 * $Id $
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

package org.lockss.plugin.palgrave;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.UrlNormalizer;
import org.lockss.plugin.palgrave.PalgraveBookUrlNormalizer;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockLockssDaemon;

public class TestPalgraveBookUrlNormalizer extends LockssTestCase {
  private static final String BASE_URL = "http://www.palgraveconnect.com/";
  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau; 
  
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
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    return conf;
  }
    
  public void testUrlNormalizer() throws Exception {
    UrlNormalizer normalizer = new PalgraveBookUrlNormalizer();

    http://www.palgraveconnect.com/pc/books2013/browse/inside/chapter/1234567890123.0011/1234567890123.0011.html?chapterDoi=1234567890123.0011&focus=pdf-viewer
      // don't do anything to a normal url
       log.debug3("1");
      assertEquals("http://www.palgraveconnect.com/pc/books2013/browse/inside/download/1234567890123.pdf",
          normalizer.normalizeUrl("http://www.palgraveconnect.com/pc/books2013/browse/inside/download/1234567890123.pdf", mau));
      log.debug3("2");

      // remove focus=pdf-viewer at end of url
      assertEquals("http://www.palgraveconnect.com/pc/books2013/browse/inside/chapter/1234567890123.0011/1234567890123.0011.html?chapterDoi=1234567890123.0011",
          normalizer.normalizeUrl("http://www.palgraveconnect.com/pc/books2013/browse/inside/chapter/1234567890123.0011/1234567890123.0011.html?chapterDoi=1234567890123.0011&focus=pdf-viewer", mau));
      log.debug3("3");

      // remove page=X
      assertEquals("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9780230389113.0008/9780230389113.0008.html?chapterDoi=9780230389113.0008",
          normalizer.normalizeUrl("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9780230389113.0008/9780230389113.0008.html?page=0&chapterDoi=9780230389113.0008", mau));
      log.debug3("4");

      //
      assertEquals("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9780230389113.0007/9780230389113.0007.html?chapterDoi=9780230389113.0007",
          normalizer.normalizeUrl("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9780230389113.0007/9780230389113.0007.html?chapterDoi=9780230389113.0007&focus=pdf-viewer", mau));
      log.debug3("5");

      //
      assertEquals("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9780230389113.0007/9780230389113.0007.html?chapterDoi=9780230389113.0007",
          normalizer.normalizeUrl("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9780230389113.0007/9780230389113.0007.html?page=0&chapterDoi=9780230389113.0007&focus=pdf-viewer", mau));
      log.debug3("6");

      // #Page=X at the end of the url
      assertEquals("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9780230389113/9780230389113.0006.html?chapterDoi=9780230389113.0006",
          normalizer.normalizeUrl("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9780230389113/9780230389113.0006.html?chapterDoi=9780230389113.0006&focus=pdf-viewer#page=0", mau));
      
      log.debug3("7");

      // #Page=X at the end of the url
      assertEquals("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9781137006004.0016/9781137006004.0016.html?chapterDoi=9781137006004.0016",
          normalizer.normalizeUrl("http://www.palgraveconnect.com/pc/busman2013/browse/inside/chapter/9781137006004.0016/9781137006004.0016.html?chapterDoi=9781137006004.0016#page=1", mau));
      
      log.debug3("7.5");
      // url not in the host
      assertEquals("https://www.google.com/search?hl=EN&tbo=p&tbm=bks&q=intitle:IBM+intitle:and+intitle",
          normalizer.normalizeUrl("https://www.google.com/search?hl=EN&tbo=p&tbm=bks&q=intitle:IBM+intitle:and+intitle", mau));
      log.debug3("done");


  }
  

}
