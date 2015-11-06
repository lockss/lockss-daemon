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

package org.lockss.plugin.bmc;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;


/* Sample Html File.
  
<meta name="citation_journal_title" content="Acta Veterinaria Scandinavica" />
<meta name="citation_publisher" content="BioMed Central Ltd" />
<meta name="citation_authors" content="Sara Frosth; Jannice S Slettemeås; Hannah J Jørgensen; Øystein Angen; Anna Aspán" />
<meta name="citation_title" content="Development and comparison of a real-time PCR assay for detection of Dichelobacter nodosus with culturing and conventional PCR: harmonisation between three laboratories" />

<meta name="citation_volume" content="54" />
<meta name="citation_issue" content="1" />
<meta name="citation_date" content="2012-01-31" />
<meta name="citation_firstpage" content="6" />
<meta name="citation_doi" content="10.1186/1751-0147-54-6" />
 
 */
public class TestBioMedCentralPluginMetadataExtractorFactory extends LockssTestCase {

  static Logger log = Logger.getLogger("TestBioMedCentralPluginMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = "org.lockss.plugin.bmc.ClockssBioMedCentralPlugin";

  private static String BASE_URL = "http://www.actavetscand.com/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
        simAuConfig(tempDirPath));
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, biomedcentralAuConfig());
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
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put(
        "fileTypes",
        ""
            + (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }

  /**
   * Configuration method.
   * 
   * @return
   */

  Configuration biomedcentralAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "54");
    conf.put("journal_issn", "1751-0147");
    return conf;
  }

  // the metadata that should be extracted
  String goodVolume = "54";
  String goodDOI = "10.1186/1751-0147-54-6";
  String goodIssue = "1";
  String goodStartPage = "6";
  String goodDate = "2012-01-31";
  String[] goodAuthors = new String[] { "Sara Frosth", "Jannice S Slettemeås",
      "Hannah J Jørgensen", "Øystein Angen", "Anna Aspán" };
  String goodArticleTitle = "Development and comparison of a real-time PCR assay for detection of Dichelobacter nodosus with culturing and conventional PCR: harmonisation between three laboratories";
  String goodJournalTitle = "Acta Veterinaria Scandinavica";
  String goodISSN = "1751-0147";

  String goodContent = "<HTML><HEAD><TITLE>" + "blabla"
      + "</TITLE></HEAD><BODY>\n"
      + "<meta name=\"citation_journal_title\" content=\""
      + goodJournalTitle
      + "\">\n"
      + "<meta name=\"citation_authors\"  content=\"Sara Frosth, Jannice S Slettemeås, Hannah J Jørgensen, Øystein Angen, Anna Aspán\">"
      + "<meta name=\"citation_title\" content=\""
      + goodArticleTitle
      + "\">\n"

      + "<meta name=\"citation_journal_title\" content=\""
      + goodJournalTitle
      + "\">\n"
      + "<meta name=\"citation_publication_date\" content=\""
      + goodDate
      + "\">\n"
      + "<meta name=\"citation_volume\""
      + " content=\""
      + goodVolume
      + "\">\n"
      + "<meta name=\"citation_issue\" content=\""
      + goodIssue
      + "\">\n"

      + "<meta name=\"dc.date\" content=\""
      + goodDate
      + "\">\n"
      + "<meta name=\"citation_firstpage\""
      + " content=\""
      + goodStartPage
      + "\">\n"
      + "<meta name=\"citation_doi\""
      + " content=\""
      + goodDOI
      + "\">\n"
      + "<meta name=\"citation_issn\""
      + " content=\""
      + goodISSN
      + "\">\n";

  /**
   * Method that creates a simulated Cached URL from the source code provided by
   * the goodContent String. It then asserts that the metadata extracted, by
   * using the BioMedCentralPluginMetadataExtractorFactory, match the metadata in the
   * source code.
   * 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.actavetscand.com/content/54/1/6";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new BioMedCentralPluginHtmlMetadataExtractorFactory.BioMedCentralPluginHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    System.out.println("author::" + md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(Arrays.asList(goodAuthors),
        md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));

    assertEquals(goodISSN, md.get(MetadataField.FIELD_EISSN));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
  }

  String badContent = "<HTML><HEAD><TITLE>" + goodArticleTitle
      + "</TITLE></HEAD><BODY>\n" + "<meta name=\"foo\""
      + " content=\"bar\">\n" + "  <div id=\"issn\">"
      + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
      + goodISSN + " </div>\n";

  /**
   * Method that creates a simulated Cached URL from the source code provided by
   * the badContent String. It then asserts that NO metadata is extracted by
   * using the BioMedCentralHtmlMetadataExtractorFactory as the source code is
   * broken.
   * 
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.actavetscand.com/content/54/1/6";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new BioMedCentralPluginHtmlMetadataExtractorFactory.BioMedCentralPluginHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertEquals(1, md.rawSize());
    assertEquals("bar", md.getRaw("foo"));
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

    public SimulatedContentGenerator getContentGenerator(Configuration cf,
        String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class to create a html source code simulated content
   * 
   */
  public static class MySimulatedContentGenerator extends
      SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

  }
}