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

package org.lockss.plugin.liberquarterly;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the source for this plugin is:
 * http://liber.library.uu.nl/index.php/lq/article/view/7824/7978
 */
public class TestLiberQuarterlyMetadataExtractorFactory extends LockssTestCase {
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit hau;
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME = "org.lockss.plugin.liberquarterly.LiberQuarterlyPlugin";

  private static String BASE_URL = "http://www.example.com/";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
					     simAuConfig(tempDirPath));
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, springerAuConfig());
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
    conf.put("journal_id", "fdr");
    conf.put("year", "2012");
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration springerAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "fdr");
    conf.put("year", "2012");
    return conf;
  }
 
  List<String> goodAuthors = new ArrayList<String>();
  String goodIssn = "0000-000X";
  String goodVolume = "Volume";
  String goodIssue = "Issue";
  String goodArticle = "Article Title";
  String goodDate = "Date";
  String goodJournal = "Journal Title";
  String goodPublisher = "Publisher";
  
  String goodContent = 
	"<meta name=\"DC.Creator.PersonalName\" content=\"John A. Author; John B. Author\"/>"+
	"<meta name=\"DC.Date.created\" scheme=\"ISO8601\" content=\"2005-09-28\"/>"+
	"<meta name=\"DC.Date.dateSubmitted\" scheme=\"ISO8601\" content=\"2012-05-18\"/>"+
	"<meta name=\"DC.Date.issued\" scheme=\"ISO8601\" content=\"2005-09-01\"/>"+
	"<meta name=\"DC.Date.modified\" scheme=\"ISO8601\" content=\"2012-05-18\"/>"+
	"<meta name=\"DC.Description\" xml:lang=\"en\" content=\"Abstract\"/>"+
	"<meta name=\"DC.Format\" scheme=\"IMT\" content=\"text/html\"/>"+		
	"<meta name=\"DC.Format\" scheme=\"IMT\" content=\"application/pdf\"/>"+		
	"<meta name=\"DC.Identifier\" content=\"URN:NGN:NF:UI:10-1-112358\"/>"+
		"<meta name=\"DC.Identifier.URI\" content=\"http://fake.com\"/>"+
	"<meta name=\"DC.Language\" scheme=\"ISO639-1\" content=\"\"/>"+
	"<meta name=\"DC.Rights\" content=\"Rights\"/>"+
	"<meta name=\"DC.Source\" content=\"Publisher\"/>"+
	"<meta name=\"DC.Source.ISSN\" content=\"0000-000X\"/>"+
	"<meta name=\"DC.Source.Issue\" content=\"Issue\"/>"+
	"<meta name=\"DC.Source.URI\" content=\"http://fake.com\"/>"+
	"<meta name=\"DC.Source.Volume\" content=\"Volume\"/>"+
	"<meta name=\"DC.Title\" content=\"Article Title\"/>"+
		"<meta name=\"DC.Type\" content=\"Text.Serial.Journal\"/>"+
	"<meta name=\"DC.Type.articleType\" content=\"Articles\"/>"+	
		"<meta name=\"gs_meta_revision\" content=\"1.1\" />"+
	"<meta name=\"citation_journal_title\" content=\"Journal Title\"/>"+
	"<meta name=\"citation_issn\" content=\"0000-000X\"/>"+
	"<meta name=\"citation_authors\" content=\"John A. Author; John B. Author\"/>"+
	"<meta name=\"citation_title\" content=\"Article Title\"/>"+

	"<meta name=\"citation_date\" content=\"Date\"/>"+

	"<meta name=\"citation_volume\" content=\"Volume\"/>"+
	"<meta name=\"citation_issue\" content=\"Issue\"/>"+
		"<meta name=\"citation_abstract_html_url\" content=\"http://fake.com\"/>"+
	"<meta name=\"citation_fulltext_html_url\" content=\"http://fake.com\"/>"+
	"<meta name=\"citation_pdf_url\" content=\"http://fake.com\"/>";


  public void testExtractFromGoodContent() throws Exception {
	goodAuthors.add("John A. Author");
	goodAuthors.add("John B. Author");
	  
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    FileMetadataExtractor me =
      new LiberQuarterlyMetadataExtractorFactory.LiberQuarterlyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodArticle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodArticle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodAuthors + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new LiberQuarterlyMetadataExtractorFactory.LiberQuarterlyMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));
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
   */
  public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
			
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
			
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";			

      file_content += getHtmlContent(fileNum, depth, branchNum,	isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
		    + file_content);

      return file_content;
    }
  }
}