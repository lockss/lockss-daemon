/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.nature;

import java.io.*;
import java.util.*;

import org.lockss.state.NodeManager;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

public class TestNatureArticleIteratorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestNatureArticleIteratorFactory");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit nau;		// Nature AU
  private MockLockssDaemon theDaemon;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.nature.ClockssNaturePublishingGroupPlugin";

  private static String BASE_URL = "http://www.nature.com/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    nau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, natureAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes",
             "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
                   | SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+fileSize);
    return conf;
  }

  Configuration natureAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "aps");
    conf.put("volume_name", "123");
    conf.put("year", "2008");
    return conf;
  }

  public void testArticleCountAndType() throws Exception {
    int expCount = 28;
    PluginTestUtil.crawlSimAu(sau);
    String pat1 = "branch(\\d+)/(\\d+file\\.html)";
    String rep1 = "aps/journal/v123/n$1/full/$2";
    PluginTestUtil.copyAu(sau, nau, ".*[^.][^p][^d][^f]$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep2 = "aps/journal/v123/n$1/pdf/$2";
    PluginTestUtil.copyAu(sau, nau, ".*\\.pdf$", pat2, rep2);
    
    // Remove some URLs
    int deleted = 0; 
    for (CachedUrl cu : AuUtil.getCuIterable(nau)) {
        String url = cu.getUrl();
        if (url.contains("/journal/") && (url.endsWith("1file.html") || url.endsWith("2file.pdf"))) {
          deleteBlock(cu);
          ++deleted;
        }
    }
    assertEquals(8, deleted);

    Iterator<ArticleFiles> it = nau.getArticleIterator();
    int count = 0;
    int countHtmlOnly = 0;
    int countPdfOnly = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      String url = cu.getUrl();
      assertNotNull(cu);
      String contentType = cu.getContentType();
      log.debug("count " + count + " url " + url + " " + contentType);
      count++;
      if (af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) == null) {
        ++countHtmlOnly;
      }
      if (af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) == url) {
        ++countPdfOnly;
      }
    }
    log.debug("Article count is " + count);
    assertEquals(expCount, count);
    assertEquals(4, countHtmlOnly);
    assertEquals(4, countPdfOnly);
  }

//  public void testArticleCountAndDefaultType() throws Exception {
//    testArticleCountAndType("text/html", true, 24);
//  }
//
//  public void testArticleCountAndPdf() throws Exception {
//    testArticleCountAndType("application/pdf", false, 0);
//  }

  private void deleteBlock(CachedUrl cu) throws IOException {
    log.info("deleting " + cu.getUrl());
    CachedUrlSetSpec cuss = new SingleNodeCachedUrlSetSpec(cu.getUrl());
    ArchivalUnit au = cu.getArchivalUnit();
    CachedUrlSet cus = au.makeCachedUrlSet(cuss);
    NodeManager nm = au.getPlugin().getDaemon().getNodeManager(au);
    nm.deleteNode(cus);
  }

  
}
