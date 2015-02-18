/* $Id$

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

package org.lockss.plugin.atypon.ammonsscientific;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/*
 * One of the articles used to get the html source for this plugin is:
 * http://www.amsciepub.com/doi/abs/10.2466/07.17.21.PMS.113.6.703-714 
 */
public class TestAmmonsScientificMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestAmmonsScientificMetadataExtractorFactory");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit hau;
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.atypon.ammonsscientific.ClockssAmmonsScientificPlugin";

  private static String BASE_URL = "http://www.example.com";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, ammonsAuConfig());
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
    conf.put("volume_name", "ammons");
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
				SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration ammonsAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", "fdr");
    conf.put("volume_name", "ammons");
    return conf;
  }
  
  String goodDate = "Date";
  String dateScheme = "WTN8601";
  String goodArticleTitle = "Article Title";
  String goodJournalTitle = "Paa and Moo Journal";
  String goodPublisher = "Publisher";
  String goodSubject = "Subject";
  String goodDescription = "Summary";
  String goodType = "Type";
  String goodFormat = "Format";
  String doiScheme = "doi";
  String goodDoi = "10.2446/12.34.56";
  String goodLanguage = "Language";
  String goodRights = "\u00a9 Paa and Moo Journal 2011"; // Copyright unicode
  String goodCoverage = "Coverage";
  String authorA = "A. Author";
  String authorB = "B. Author";
  String authorC = "C. Author";
  String authorD = "D. Author";
  
  ArrayList<String> goodAuthors = new ArrayList<String>();
 
  //Unfortunately, it has to be on one line for an accurate representation (and to work)
  String goodContent =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
        + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
        + "<html>"
	+ "<head>"	    
	+ "<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\"></link>"
	+ "<meta name=\"dc.Title\" content=\"" + goodArticleTitle + "\"></meta>"
	+ "<meta name=\"dc.Creator\" content=\""+ authorA + "\"></meta>"
	+ "<meta name=\"dc.Creator\" content=\"" + authorB + "\"></meta>"
	+ "<meta name=\"dc.Creator\" content=\"" + authorC + "\"></meta>"
	+ "<meta name=\"dc.Creator\" content=\"" + authorD + "\"></meta>"
	+ "<meta name=\"dc.Description\" content=\"" + goodDescription + "\"></meta>"
	+ "<meta name=\"dc.Publisher\" content=\"" + goodPublisher + "\"></meta>"
	+ "<meta name=\"dc.Date\" scheme=\"" + dateScheme + "\" content=\"" + goodDate + "\"></meta>"
	+ "<meta name=\"dc.Type\" content=\"" + goodType + "\"></meta>"
	+ "<meta name=\"dc.Format\" content=\"" + goodFormat + "\"></meta>"
	+ "<meta name=\"dc.Language\" content=\"" + goodLanguage + "\"></meta>"
	+ "<meta name=\"dc.Coverage\" content=\"" + goodCoverage + "\"></meta>"
	+ "<meta name=\"dc.Rights\" content=\"" + goodRights + "\"></meta>"
	+ "</head>"
	+ "</html>";


  public void testExtractFromGoodContent() throws Exception {
    goodAuthors.add(authorA);
    goodAuthors.add(authorB);
    goodAuthors.add(authorC);
    goodAuthors.add(authorD);
	  
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me =
      new ClockssAmmonsScientificHtmlMetadataExtractorFactory.ClockssAmmonsMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodFormat, md.get(MetadataField.FIELD_FORMAT));
    assertEquals(goodLanguage, md.get(MetadataField.FIELD_LANGUAGE));
    assertEquals(goodCoverage, md.get(MetadataField.FIELD_COVERAGE));
    assertEquals(goodType, md.get(MetadataField.DC_FIELD_TYPE));
    assertEquals(goodRights, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(goodDescription, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
}
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodArticleTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodDescription + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new ClockssAmmonsScientificHtmlMetadataExtractorFactory.ClockssAmmonsMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
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
    assertEquals(1, md.rawSize());
    assertEquals("bar", md.getRaw("foo"));
  }
  
  /*
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
  
  /*
   * Inner class to create a html source code simulated content
   */
  public static class MySimulatedContentGenerator extends SimulatedContentGenerator {
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