
/*

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.aiaa;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.aiaa.AIAAHtmlMetadataExtractorFactory;
import org.lockss.plugin.copernicus.CopernicusRisMetadataExtractorFactory;
import org.lockss.plugin.simulated.*;

/*
 * Meta content from the web from a real AIAA abstract 
 * 
 * 
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US" lang="en-US">
<head><title>Self Stabilization (AIAA)</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="robots" content="noarchive,nofollow" />
<link rel="schema.DC" href="http://purl.org/DC/elements/1.0/"></link><meta name="dc.Title" content="Self Stabilization"></meta><meta name="dc.Creator" content="ShlomiDolev"></meta><meta name="dc.Date" scheme="WTN8601" content="2012-05-23"></meta><meta name="dc.Type" content="research-article"></meta><meta name="dc.Format" content="text/HTML"></meta><meta name="dc.Language" content="en"></meta><meta name="dc.Coverage" content="world"></meta>
<meta name="MSSmartTagsPreventParsing" content="true"/>
<link rel="stylesheet" type="text/css" href="/templates/jsp/_style2/_pagebuilder/reset-fonts-grids.css">
*/

public class TestAIAAMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestAIAAMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = "org.lockss.plugin.aiaa.AIAAPlugin";
  private static String BASE_URL = "http://arc.aiaa.org/";
  private static String SIM_ROOT = BASE_URL + "jacic/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,	simAuConfig(tempDirPath));
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, auConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "3");
    conf.put("fileTypes",""	+ (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "text/html");
    return conf;
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2004");
    conf.put("journal_id", "jacic");
    conf.put("volume_name","8");
    return conf;
  }

  // the metadata that should be extracted
  String goodDate = "2012-05-23";
  String goodCreator = "ShlomiDolev";
  String goodFormat = "text/HTML";
  String goodTitle = "Self Stabilization";
  String goodType = "research-article";
  String goodPublisher = "American Institute of Aeronautics and Astronautics";

  // a chunk of html source code from the publisher's site from where the 
  // metadata should be extracted

  String goodContent = 
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en-US\" lang=\"en-US\">\n" +
"<head><title>Self Stabilization (AIAA)</title>\n" +
"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
"<meta name=\"robots\" content=\"noarchive,nofollow\" />\n" +
"<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\"></link><meta name=\"dc.Title\" content=\"Self Stabilization\">" +
"</meta><meta name=\"dc.Creator\" content=\"ShlomiDolev\"></meta>" +
"<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"2012-05-23\"></meta>" +
"<meta name=\"dc.Type\" content=\"research-article\"></meta>" +
"<meta name=\"dc.Format\" content=\"text/HTML\"></meta><meta name=\"dc.Language\" content=\"en\"></meta>" +
"<meta name=\"dc.Coverage\" content=\"world\"></meta>" +
"<meta name=\"MSSmartTagsPreventParsing\" content=\"true\"/>\n" +
"<link rel=\"stylesheet\" type=\"text/css\" href=\"/templates/jsp/_style2/_pagebuilder/reset-fonts-grids.css\">";	
   

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new AIAAHtmlMetadataExtractorFactory.AIAAHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodTitle, md.get(MetadataField.DC_FIELD_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodFormat, md.get(MetadataField.DC_FIELD_FORMAT));
    assertEquals(goodType, md.get(MetadataField.DC_FIELD_TYPE));
    assertEquals(goodCreator, md.get(MetadataField.DC_FIELD_CREATOR));
  }

  public void testDOIExtraction() throws Exception {
    String url = BASE_URL + "doi/abs/10.2514/1.55555";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new AIAAHtmlMetadataExtractorFactory.AIAAHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals("10.2514/1.55555", md.get(MetadataField.FIELD_DOI));
  }

  /**
   * Inner class that where a number of Archival Units can be created
   *
   */
  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
	throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class to create a html source code simulated content
   *
   */
  public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

  }
}
