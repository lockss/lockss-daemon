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

package org.lockss.plugin.ingenta;

import java.util.regex.Pattern;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ingenta.TestIngentaHtmlMetadataExtractorFactory.MySimulatedContentGenerator;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://www
 * .ingentaconnect.com/content/maney/bjdd/2011/00000057/00000113/art00004
 */
public class TestIngentaArticleIteratorFactory extends ArticleIteratorTestCase {
  static Logger log = Logger.getLogger(TestIngentaArticleIteratorFactory.class);

  // Simulated AU to generate content
  private SimulatedArchivalUnit sau;
  private static String PLUGIN_NAME = "org.lockss.plugin.ingenta.ClockssIngentaJournalPlugin";
  private static String BASE_URL = "http://www.ingentaconnect.com/";
  private static String SIM_ROOT = BASE_URL;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, ingentaAuConfig());
  }

  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
        throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf,
        String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put(
        "fileTypes",
        ""
            + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/pdf");
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration ingentaAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("api_url", "http://api.ingentaconnect.com/");
    conf.put("graphics_url", "http://graphics.ingentaconnect.com/");
    conf.put("journal_issn", "1468-2737");
    conf.put("volume_name", "1");
    conf.put("publisher_id", "maney");
    conf.put("journal_id", "amb");
    return conf;
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();

    assertEquals(
        ListUtil.list("http://api.ingentaconnect.com/content/maney/amb"),
        getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat,
        "http://api.ingentaconnect.com/contentt/1014174823t49006/j0143.pdfwrong");
    assertNotMatchesRE(
        pat,
        "http://api.ingentaconnect.com/contentt/maney/amb/1938/00000001/00000003/art00001");
    assertMatchesRE(
        pat,
        "http://api.ingentaconnect.com/content/maney/amb/1938/00000001/00000003/art00001?crawler=true");
    assertNotMatchesRE(pat, "http://www.example.com/content/");
    assertNotMatchesRE(pat, "http://www.example.com/content/j");
    assertNotMatchesRE(pat,
        "http://www.example.com/content/j0123/j383.pdfwrong");
  }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat2 = "/content/(.*)\\?crawler=true";
    String rep2 = "/content/maney/hrj/2000/00000001/00000003/art00001?crawler=true";
    PluginTestUtil.copyAu(sau, au, ".*?crawler=true", pat2, rep2);
    String url = "http://api.ingentaconnect.com/content/maney/amb/2000/00000001/00000003/art00001?crawler=true";
    CachedUrl cu = au.makeCachedUrl(url);
    assertNotNull(cu);
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, cu);
    //assertNotNull(af);
    //assertEquals(cu, af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF));

  }

}
