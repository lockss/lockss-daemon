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

package org.lockss.plugin.casaeditriceclueb;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the xml source for this plugin is:
 * http://clockss-ingest.lockss.org/sourcefiles/clueb-dev/2010/CLUEB_CHAPTERS.zip!/8849112416/10.1400_52474.xml
 */
public class TestCasaEditriceCluebSourceXmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestCluebSourceXmlMetadataExtractorFactory");

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  private ArchivalUnit hau;
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.casaeditriceclueb.ClockssCasaEditriceCluebSourcePlugin";

  private static String BASE_URL = "http://www.example.com";
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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, cluebAuConfig());
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
    conf.put("year", "2012");
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_HTML);
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration cluebAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2012");
    return conf;
  }
  
  String goodAuthor = "Author, John A.";
  String goodDoi = "10.1000/5555";
  String goodJournal = "Journal Title";
  String goodPublisher = "Publisher";
  String goodArticle = "Article Title";
  String goodDate = "Date";
  String goodStartPage = "1";
    
  String goodContent =
		  "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><ONIXDOIMonographChapterWorkRegistrationMessage xmlns=\"http://www.editeur.org/onix/DOIMetadata/1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><DOIMonographChapterWork>"+
		  "<NotificationType>06</NotificationType>"+
		  "<DOI>10.1000/5555</DOI>"+
		  "<DOIWebsiteLink>http://digital.casalini.it/10.1000/5555</DOIWebsiteLink>"+
		  "<RegistrantName>Registrant</RegistrantName>"+
		  "<RegistrationAuthority>Authority</RegistrationAuthority>"+
		  "<MonographicPublication>"+
		  "<MonographicWork>"+
		  "<Title>"+
		  "<TitleType>00</TitleType>"+
		  "<TitleText>Journal Title</TitleText>"+
		  "</Title>"+
		  "</MonographicWork>"+
		  "<MonographicProduct>"+
		  "<ProductIdentifier>"+
		  "<ProductIDType>02</ProductIDType>"+  
		  "<IDValue>00000000</IDValue>"+
		  "</ProductIdentifier>"+
		  "<ProductIdentifier>"+
		  "<ProductIDType>03</ProductIDType>"+  
		  "<IDValue>88000000X</IDValue>"+
		  "</ProductIdentifier>"+
		  "<ProductForm>BA</ProductForm>"+
		  "<Publisher>"+
		  "<PublishingRole>01</PublishingRole>"+
		  "<PublisherName>Publisher</PublisherName>"+
		  "</Publisher>"+
		  "<CountryOfPublication>IT</CountryOfPublication>"+
		  "</MonographicProduct>"+
		  "</MonographicPublication>"+
		  "<ContentItem>"+
		  "<SequenceNumber>1</SequenceNumber>"+
		  "<NumberOfPages>25</NumberOfPages>"+
		  "<Title>"+
		  "<TitleType>2</TitleType>"+
		  "<TitleText>Article Title</TitleText>"+
		  "</Title><Contributor>"+
		  "<ContributorRole>C2</ContributorRole>"+
		  "<PersonNameInverted>Author, John A.</PersonNameInverted>"+
		  "</Contributor>"+
		  "<Language>"+
		  "<LanguageRole>02</LanguageRole>"+
		  "<LanguageCode>ita</LanguageCode>"+
		  "</Language>"+
		  "<PublicationDate>Date</PublicationDate>"+
		  "</ContentItem>"+
		  "</DOIMonographChapterWork></ONIXDOIMonographChapterWorkRegistrationMessage>";


  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    FileMetadataExtractor me =
      new CasaEditriceCluebSourceXmlMetadataExtractorFactory.CluebXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodArticle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
  }
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodJournal + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodArticle + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me =
      new CasaEditriceCluebSourceXmlMetadataExtractorFactory.CluebXmlMetadataExtractor();
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