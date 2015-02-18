/*
 * $Id$
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

package org.lockss.plugin.springer;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.crawler.*;
import org.lockss.extractor.*;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.plugin.simulated.*;

public class TestSpringerArticleIteratorFactory
  extends ArticleIteratorTestCase {
  static Logger log = Logger.getLogger("TestSpringerArticleIteratorFactory");

  private SimulatedArchivalUnit simau;	// Simulated AU to generate content
  private ArchivalUnit spau;		// Springer AU
  private MockLockssDaemon theDaemon;
  private CrawlManager crawlMgr;
  private static final int DEFAULT_FILESIZE = 3000;
  private static int fileSize = DEFAULT_FILESIZE;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.springer.ClockssSpringerExplodedPlugin";

  private static String BASE_URL =
    "http://source.lockss.org/sourcefiles/springer-released/";

  public void setUp() throws Exception {
    super.setUp();
    ConfigurationUtil.addFromArgs("org.lockss.plugin.simulated.SimulatedContentGenerator.doSpringer",
				  "true");
    simau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    spau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, springerAuConfig());
  }

  public void tearDown() throws Exception {
    simau.deleteContentTree();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF |
				SimulatedContentGenerator.FILE_TYPE_XML));
    return conf;
  }

  Configuration springerAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2009");
    return conf;
  }

  Pattern pat1 = Pattern.compile("(.*)\\.xml\\.Meta$",
				 Pattern.CASE_INSENSITIVE);
  Pattern pat2 = Pattern.compile("(.*)/BodyRef/PDF(/.*)\\.pdf$",
				 Pattern.CASE_INSENSITIVE);

  public void testArticleCountAndType(String articleMimeType,
				      boolean isDefaultTarget,
				      int expCount)
      throws Exception {
    PluginTestUtil.crawlSimAu(simau);
    PluginTestUtil.copyAu(simau, spau);

    Iterator<ArticleFiles> it =
      isDefaultTarget
      ? spau.getArticleIterator()
      : spau.getArticleIterator(new MetadataTarget().setFormat(articleMimeType));
    int count = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      CachedUrl cu = af.getFullTextCu();
      assertNotNull(cu);
      String pdfUrl = cu.getUrl();
      String contentType = cu.getContentType();
      assertNotNull(contentType);
      assertTrue(contentType,
	     contentType.toLowerCase().startsWith(articleMimeType));
      CachedUrl xmlCu = af.getRoleCu("xml");
      if (isDefaultTarget) {
	assertNotNull("XML role is null", xmlCu);
	String xmlUrl = xmlCu.getUrl();
	Matcher m1 = pat1.matcher(xmlUrl);
	assertTrue(xmlUrl, m1.matches());
	Matcher m2 = pat2.matcher(pdfUrl);
	assertTrue(pdfUrl, m2.matches());
	assertEquals(m1.group(1), m2.group(1)+m2.group(2));
      } else {	
	assertNull("XML role is not null", xmlCu);
      }

      log.debug("count " + count + " url " + cu.getUrl() + " " + contentType);
      count++;
    }
    log.debug("Article count is " + count);
    assertEquals(expCount, count);
  }

  public void testArticleCountAndDefaultType() throws Exception {
    testArticleCountAndType("application/pdf", true, 28);
  }

  public void testArticleCountAndHtmlType() throws Exception {
    testArticleCountAndType("text/html", false, 12);
  }

}
