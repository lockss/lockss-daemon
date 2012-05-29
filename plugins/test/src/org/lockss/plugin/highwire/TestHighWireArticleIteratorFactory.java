/*
 * $Id: TestHighWireArticleIteratorFactory.java,v 1.8 2012-05-29 20:34:27 akanshab01 Exp $
 */

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

package org.lockss.plugin.highwire;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

public class TestHighWireArticleIteratorFactory extends ArticleIteratorTestCase {
  static Logger log = Logger.getLogger("TestHighWirPressArticleIteratorFactory");

  private static final int DEFAULT_FILESIZE = 3000;

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit hau;		// HighWire AU
  private MockLockssDaemon theDaemon;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.highwire.HighWirePressPlugin";

  private static String BASE_URL = "http://pediatrics.aappublications.org/";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
    
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
                                  tempDirPath);
  //  http://inderscience.metapress.com/
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }

  /*public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }*/

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "2");
    conf.put("branch", "2");
    conf.put("numFiles", "4");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration highWireAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume", "52");
    return conf;
  }


protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, ConfigurationUtil
        .fromArgs("base_url", "http://pediatrics.aappublications.org/",
            "volume_name", "52", "journal_issn", "1098-4275"));
  }

 

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    System.out.println("Root Urls::" + getRootUrls(artIter));
    assertEquals(ListUtil.list( "http://pediatrics.aappublications.org/cgi/content/full/52/"
        ,"http://pediatrics.aappublications.org/cgi/reprint/52/"),
        getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertMatchesRE(pat,
        "http://pediatrics.aappublications.org/cgi/reprint/foo;52/Supplement_3/S69.pdf");
    assertMatchesRE(pat,
        "http://pediatrics.aappublications.org/cgi/reprint/52/supplement_3/S69.pdf");
   assertNotMatchesRE(pat,
        "http://pediatrics.aappublications.org/cgi/reprin/1014174823t49006/j0143.pdfwrong");
    assertNotMatchesRE(pat,
        "http://pediatrics.aappublications.org/cgi/reprintt/1014174823t49006/j0143.pdfwrong");
    assertNotMatchesRE(pat, "http://www.example.com/content/");
    assertNotMatchesRE(pat, "http://www.example.com/content/j");
    assertNotMatchesRE(pat,
        "http://www.example.com/content/j0123/j383.pdfwrong");
  }
  

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String url = "http://pediatrics.aappublications.org/cgi/reprint/foo;125/Supplement_3/S69.pdf";
    CachedUrl cu = au.makeCachedUrl(url);
    assertNotNull(cu);
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, cu);
    System.out.println("article files::" + af);
    assertEquals(cu, af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF));

  }
}
