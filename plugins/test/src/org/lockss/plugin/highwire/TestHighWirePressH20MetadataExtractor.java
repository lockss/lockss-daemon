/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.highwire;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the HTML source for this plugin is:
 * http://ajpendo.physiology.org/content/301/5/E767.full
 *
 */
public class TestHighWirePressH20MetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestHighWirePressH20MetadataExtractor");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit hwau; // HighWire AU
  // private static final String issnTemplate = "%1%2%3%1-%3%1%2%3";	

  private static String PLUGIN_NAME = "org.lockss.plugin.highwire.HighWirePressH20Plugin"; // XML file in org.lockss.plugin.highwire package

  private static String BASE_URL = "http://www.bmj.com/";
  private static String SIM_ROOT = BASE_URL + "hwjn/"; // Simulated journal ID: "HighWire Journal"

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
    hwau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, highWirePressH20AuConfig());
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
    conf.put("numFiles", "7");
    conf.put("fileTypes",""	+ (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration highWirePressH20AuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "19");
    conf.put("journal_id", "hwjn");
    return conf;
  }

  // the metadata that should be extracted
  String goodFormat = "text/html";
  String goodLanguage = "en";
  String goodDOI = "10.1234/testdoi.00123.2011";
  String goodArticleTitle = "Empirical results of measuring X & Y in Z";
  String goodDCDate = "2011-11-01";
  String goodDate = "11/01/2011";
  String goodPublisher = "ABC Publicorp Inc., &c.";
  String[] goodDCContributors = new String[] {"Alfred Baker", "Charles D. Elfman", "Fiona Gale", "Hiram I. J. Kostiuk", "Lenore Mikkelson", "Noah O. Prada"};
  String[] goodAuthors = new String[] {"Baker, Alfred", "Elfman, Charles D.", "Gale, Fiona", "Kostiuk, Hiram I. J.", "Mikkelson, Lenore", "Prada, Noah O."};
  String goodJournalTitle = "Doctor Friendly's Fake Journal of Laughs";
  String goodISSN = "1234-5678";
  String goodEISSN = "9876-5432";
  String goodVolume = "415";
  String goodIssue = "5";
  String goodStartPage = "R543";
  String goodEndPage = "R556";
  String goodURL = "http://fakejournaloflaughs.drfriendly.org/415/5/R543";
  String goodMjid = "acupmed;30/1/8";
  String goodPropId = "acupmed";

  // a chunk of html source code from the publisher's site from where the metadata should be extracted
  String goodContent = "<meta name=\"DC.Format\" content=\"" + goodFormat + "\" />" +
  		"<meta name=\"DC.Language\" content=\"" + goodLanguage + "\" />" +
  		"<meta content=\"" + goodArticleTitle + "\" name=\"DC.Title\" />" +
  		"<meta content=\"" + goodDOI + "\" name=\"DC.Identifier\" />" +
  		"<meta content=\"" + goodDCDate + "\" name=\"DC.Date\" />" +
  		"<meta content=\"" + goodPublisher + "\" name=\"DC.Publisher\" />" +
  		"<meta content=\"" + goodDCContributors[0] + "\" name=\"DC.Contributor\" />" +
  		"<meta content=\"" + goodDCContributors[1] + "\" name=\"DC.Contributor\" />" +
  		"<meta content=\"" + goodDCContributors[2] + "\" name=\"DC.Contributor\" />" +
  		"<meta content=\"" + goodDCContributors[3] + "\" name=\"DC.Contributor\" />" +
  		"<meta content=\"" + goodDCContributors[4] + "\" name=\"DC.Contributor\" />" +
  		"<meta content=\"" + goodDCContributors[5] + "\" name=\"DC.Contributor\" />" +
  		"<meta content=\"" + goodJournalTitle + "\" name=\"citation_journal_title\" />" +
  		"<meta content=\"" + goodISSN + "\" name=\"citation_issn\" />" +
  		"<meta content=\"" + goodEISSN + "\" name=\"citation_issn\" />" +
                "<meta content=\"" + goodMjid + "\" name=\"citation_mjid\" />" +
  		"<meta content=\"" + goodAuthors[0] + ";" + 
  		goodAuthors[1] + ";" + 
  		goodAuthors[2] + ";" + 
  		goodAuthors[3] + ";" + 
  		goodAuthors[4] + ";" +
  		goodAuthors[5] + "\" name=\"citation_authors\" />" +
  		"<meta content=\"" + goodArticleTitle + "\" name=\"citation_title\" />" +
  		"<meta content=\"" + goodDate + "\" name=\"citation_date\" />" +
  		"<meta content=\"" + goodVolume + "\" name=\"citation_volume\" />" +
  		"<meta content=\"" + goodIssue + "\" name=\"citation_issue\" />" +
  		"<meta content=\"" + goodStartPage + "\" name=\"citation_firstpage\" />" +
  		"<meta content=\"" + goodEndPage + "\" name=\"citation_lastpage\" />" +
  		"<meta content=\"" + goodDOI + "\" name=\"citation_doi\" />" +
  		"<meta content=\"" + goodURL + "\" name=\"citation_public_url\" />";
		
  /**
   * Method that creates a simulated Cached URL from the source code provided by the goodContent String.
   * It then asserts that the metadata extracted with TaylorAndFrancisHtmlMetadataExtractorFactory
   * match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.bmj.com/hwjn";
    MockCachedUrl cu = new MockCachedUrl(url, hwau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new HighWirePressH20HtmlMetadataExtractorFactory.HighWirePressH20HtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodFormat, md.get(MetadataField.FIELD_FORMAT));
    assertEquals(goodLanguage, md.get(MetadataField.FIELD_LANGUAGE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(url, md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(goodPropId, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
  }

  // a chunk of HTML source code from where the TaylorAndFrancisHtmlMetadataExtractorFactory should NOT be able to extract metadata
  String badContent = "<html><head><title>" 
	+ goodArticleTitle
    + "</title></head><body>\n"
    + "<meta name=\"foo\""
    + " content=\"bar\">\n"
    + "  <div id=\"issn\">"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + goodISSN + " </div>\n";

  /**
   * Method that creates a simulated Cached URL from the source code provided by the badContent String. It then asserts that NO metadata is extracted by using 
   * the HighWirePressH20HtmlMetadataExtractorFactory as the source code is broken.
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hwau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new HighWirePressH20HtmlMetadataExtractorFactory.HighWirePressH20HtmlMetadataExtractor();
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

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class to create HTML source code simulated content
   *
   */
  public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
			
      String file_content = "<html><head><title>" + filename + "</title></head><body>\n";
			
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";			

      file_content += getHtmlContent(fileNum, depth, branchNum,	isAbnormal);
      file_content += "\n</body></html>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
		    + file_content);

      return file_content;
    }
  }
}
