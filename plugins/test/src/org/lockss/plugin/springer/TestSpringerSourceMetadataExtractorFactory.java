/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.springer;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the xml source for this plugin is:
 * <base url>/2010/ftp_PUB_10-05-17_06-11-02.zip/JOU=11864/VOL=2008.9/ISU=2-3/ART=2008_64/11864_2008_Article.xml.Meta 
 */
public class TestSpringerSourceMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestSpringerMetadataExtractorFactory");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // simulated au to generate content
  private ArchivalUnit ssau; // springer source au

  private static String PLUGIN_NAME =
    "org.lockss.plugin.springer.ClockssSpringerSourcePlugin";

  private static String BASE_URL = "http://clockss-ingest.lockss.org/sourcefiles/springer-dev/";
  private final String YEAR = "2012";

  // Simulated journal ID: "Spring source Journal"
  private static String SIM_ROOT = BASE_URL + "ssjn/";

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
    ssau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, springerSourceAuConfig());
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
    conf.put("fileTypes","" + (SimulatedContentGenerator.FILE_TYPE_PDF 
                               + SimulatedContentGenerator.FILE_TYPE_XML));
    return conf;
  }

  // Configuration method. 
  Configuration springerSourceAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", YEAR);
    return conf;
  }

  // the metadata that should be extracted
  String goodTitle = "Title";
  ArrayList<String> goodAuthors = new ArrayList<String>();
  String goodIssn = "5555-5555";
  String goodVolume = "Volume";
  String goodDate = "2008-12";
  String goodIssue = "Issue";
  String goodDoi = "10.1066/DOI";
  String goodSource = "USA";
  ArrayList<String> goodKeywords = new ArrayList<String>();
  String goodDescription = "Summary";
  String goodRights = "Rights";
  String goodPublisher = "Publisher";
  String goodEissn = "6666-6666";
  String goodJournalTitle = "Journal";
  String goodLanguage = "Language";
  String goodStart = "Start";
  String goodEnd = "End";
  String hardwiredPublisher = "Springer";
  String genJournalTitle = "UNKNOWN_TITLE/journalId=00238";
    
  String goodContent = 
		  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
  "<!DOCTYPE Publisher PUBLIC \"-//Springer-Verlag//DTD A++ V2.4//EN\" \"http://devel.springer.de/A++/V2.4/DTD/A++V2.4.dtd\">"+
  "<Publisher>"+
     "<PublisherInfo>"+
        "<PublisherName>Publisher</PublisherName>"+
        "<PublisherLocation>PublisherLocation</PublisherLocation>"+
     "</PublisherInfo>"+
     "<Journal OutputMedium=\"All\">"+
        "<JournalInfo JournalProductType=\"ArchiveJournal\" NumberingStyle=\"Unnumbered\">"+
           "<JournalID>00000</JournalID>"+
           "<JournalPrintISSN>5555-5555</JournalPrintISSN>"+
           "<JournalElectronicISSN>6666-6666</JournalElectronicISSN>"+
           "<JournalTitle>Journal</JournalTitle>"+
           "<JournalAbbreviatedTitle>Jour</JournalAbbreviatedTitle>"+
           "<JournalSubjectGroup>"+
              "<JournalSubject Type=\"Primary\">Subject1</JournalSubject>"+
              "<JournalSubject Type=\"Secondary\">Subject2</JournalSubject>"+
              "<JournalSubject Type=\"Secondary\">Subject3</JournalSubject>"+
              "<JournalSubject Type=\"Secondary\">Subject4</JournalSubject>"+
           "</JournalSubjectGroup>"+
        "</JournalInfo>"+
        "<Volume OutputMedium=\"All\">"+
           "<VolumeInfo TocLevels=\"0\" VolumeType=\"Regular\">"+
              "<VolumeIDStart>Volume</VolumeIDStart>"+
              "<VolumeIDEnd>Volume</VolumeIDEnd>"+
              "<VolumeIssueCount>Issues</VolumeIssueCount>"+
           "</VolumeInfo>"+
           "<Issue IssueType=\"Regular\" OutputMedium=\"All\">"+
              "<IssueInfo IssueType=\"Regular\" TocLevels=\"0\">"+
                 "<IssueIDStart>Issue</IssueIDStart>"+
                 "<IssueIDEnd>Issue</IssueIDEnd>"+
                 "<IssueArticleCount>Articles</IssueArticleCount>"+
                 "<IssueHistory>"+
                    "<OnlineDate>"+
                       "<Year>2008</Year>"+
                       "<Month>11</Month>"+
                       "<Day>8</Day>"+
                    "</OnlineDate>"+
                    "<PrintDate>"+
                       "<Year>2008</Year>"+
                       "<Month>11</Month>"+
                       "<Day>7</Day>"+
                    "</PrintDate>"+
                    "<CoverDate>"+
                       "<Year>2008</Year>"+
                       "<Month>12</Month>"+
                    "</CoverDate>"+
                    "<PricelistYear>2008</PricelistYear>"+
                 "</IssueHistory>"+
                 "<IssueCopyright>"+
                    "<CopyrightHolderName>Copyright</CopyrightHolderName>"+
                    "<CopyrightYear>Rights</CopyrightYear>"+
                 "</IssueCopyright>"+
              "</IssueInfo>"+
              "<Article ID=\"s12080-008-0021-5\" OutputMedium=\"All\">"+
                 "<ArticleInfo ArticleType=\"OriginalPaper\" ContainsESM=\"No\" Language=\"Language\" NumberingStyle=\"Unnumbered\" TocLevels=\"0\">"+
                          "<ArticleID>ID</ArticleID>"+
                          "<ArticleDOI>10.1066/DOI</ArticleDOI>"+
                          "<ArticleSequenceNumber>3</ArticleSequenceNumber>"+
                          "<ArticleTitle Language=\"Language\">Title</ArticleTitle>"+
                          "<ArticleCategory>Original paper</ArticleCategory>"+
                          "<ArticleFirstPage>Start</ArticleFirstPage>"+
                          "<ArticleLastPage>End</ArticleLastPage>"+
                          "<ArticleHistory>"+
                       "<RegistrationDate>"+
                          "<Year>2008</Year>"+
                          "<Month>7</Month>"+
                          "<Day>10</Day>"+
                       "</RegistrationDate>"+
                       "<Received>"+
                          "<Year>2008</Year>"+
                          "<Month>4</Month>"+
                          "<Day>3</Day>"+
                       "</Received>"+
                       "<Accepted>"+
                          "<Year>2008</Year>"+
                          "<Month>7</Month>"+
                          "<Day>3</Day>"+
                       "</Accepted>"+
                       "<OnlineDate>"+
                          "<Year>2008</Year>"+
                          "<Month>7</Month>"+
                          "<Day>30</Day>"+
                       "</OnlineDate>"+
                    "</ArticleHistory>"+
                          "<ArticleCopyright>"+
                       "<CopyrightHolderName>Copyright</CopyrightHolderName>"+
                       "<CopyrightYear>Rights</CopyrightYear>"+
                    "</ArticleCopyright>"+
                          "<ArticleGrants Type=\"Regular\">"+
                              "<MetadataGrant Grant=\"OpenAccess\"/>"+
                              "<AbstractGrant Grant=\"OpenAccess\"/>"+
                              "<BodyPDFGrant Grant=\"Restricted\"/>"+
                              "<BodyHTMLGrant Grant=\"Restricted\"/>"+
                              "<BibliographyGrant Grant=\"Restricted\"/>"+
                              "<ESMGrant Grant=\"Restricted\"/>"+
                          "</ArticleGrants>"+
                      "</ArticleInfo>"+
                 "<ArticleHeader>"+
                    "<AuthorGroup>"+
                       "<Author AffiliationIDS=\"Aff1 Aff2\" CorrespondingAffiliationID=\"Aff1\">"+
                          "<AuthorName DisplayOrder=\"Western\">"+
                             "<GivenName>A.</GivenName>"+
                             "<FamilyName>Author</FamilyName>"+
                          "</AuthorName>"+
                       "</Author>"+
                       "<Author AffiliationIDS=\"Aff1\">"+
                          "<AuthorName DisplayOrder=\"Western\">"+
                             "<GivenName>B.</GivenName>"+
                             "<FamilyName>Author</FamilyName>"+
                          "</AuthorName>"+
                       "</Author>"+
                       "<Author AffiliationIDS=\"Aff2\">"+
                          "<AuthorName DisplayOrder=\"Western\">"+
                             "<GivenName>C.</GivenName>"+
                             "<FamilyName>Author</FamilyName>"+
                          "</AuthorName>"+
                       "</Author>"+
                       "<Affiliation ID=\"Aff1\">"+
                          "<OrgDivision>Fake Division</OrgDivision>"+
                          "<OrgName>Fake Organization</OrgName>"+
                          "<OrgAddress>"+
                             "<City>FreeTown</City>"+
                             "<State>IA</State>"+
                             "<Postcode>00000</Postcode>"+
                             "<Country>USA</Country>"+
                          "</OrgAddress>"+
                       "</Affiliation>"+
                    "</AuthorGroup>"+
                    "<Abstract ID=\"Abs1\" Language=\"Language\">"+
                       "<Heading>Abstract</Heading>"+
                       "<Para>Summary</Para>"+
                    "</Abstract>"+
                    "<KeywordGroup Language=\"Language\">"+
                       "<Heading>Keywords</Heading>"+
                       "<Keyword>Keyword1</Keyword>"+
                       "<Keyword>Keyword2</Keyword>"+
                       "<Keyword>Keyword3</Keyword>"+
                    "</KeywordGroup>"+
                 "</ArticleHeader>"+
                 "<NoBody/>"+
              "</Article>"+
           "</Issue>"+
        "</Volume>"+
     "</Journal>"+
  "</Publisher>";
  
  String noTittleIssnEissnJournalIdContent = 
		  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
  "<!DOCTYPE Publisher PUBLIC \"-//Springer-Verlag//DTD A++ V2.4//EN\" \"http://devel.springer.de/A++/V2.4/DTD/A++V2.4.dtd\">"+
  "<Publisher>"+
     "<PublisherInfo>"+
        "<PublisherName>Publisher</PublisherName>"+
        "<PublisherLocation>PublisherLocation</PublisherLocation>"+
     "</PublisherInfo>"+
     "<Journal OutputMedium=\"All\">"+
        "<JournalInfo JournalProductType=\"ArchiveJournal\" NumberingStyle=\"Unnumbered\">"+
           "<JournalAbbreviatedTitle>Jour</JournalAbbreviatedTitle>"+
           "<JournalSubjectGroup>"+
              "<JournalSubject Type=\"Primary\">Subject1</JournalSubject>"+
              "<JournalSubject Type=\"Secondary\">Subject2</JournalSubject>"+
              "<JournalSubject Type=\"Secondary\">Subject3</JournalSubject>"+
              "<JournalSubject Type=\"Secondary\">Subject4</JournalSubject>"+
           "</JournalSubjectGroup>"+
        "</JournalInfo>"+
        "<Volume OutputMedium=\"All\">"+
           "<VolumeInfo TocLevels=\"0\" VolumeType=\"Regular\">"+
              "<VolumeIDStart>Volume</VolumeIDStart>"+
              "<VolumeIDEnd>Volume</VolumeIDEnd>"+
              "<VolumeIssueCount>Issues</VolumeIssueCount>"+
           "</VolumeInfo>"+
           "<Issue IssueType=\"Regular\" OutputMedium=\"All\">"+
              "<IssueInfo IssueType=\"Regular\" TocLevels=\"0\">"+
                 "<IssueIDStart>Issue</IssueIDStart>"+
                 "<IssueIDEnd>Issue</IssueIDEnd>"+
                 "<IssueArticleCount>Articles</IssueArticleCount>"+
                 "<IssueHistory>"+
                    "<OnlineDate>"+
                       "<Year>2008</Year>"+
                       "<Month>11</Month>"+
                       "<Day>8</Day>"+
                    "</OnlineDate>"+
                    "<PrintDate>"+
                       "<Year>2008</Year>"+
                       "<Month>11</Month>"+
                       "<Day>7</Day>"+
                    "</PrintDate>"+
                    "<CoverDate>"+
                       "<Year>2008</Year>"+
                       "<Month>12</Month>"+
                    "</CoverDate>"+
                    "<PricelistYear>2008</PricelistYear>"+
                 "</IssueHistory>"+
                 "<IssueCopyright>"+
                    "<CopyrightHolderName>Copyright</CopyrightHolderName>"+
                    "<CopyrightYear>Rights</CopyrightYear>"+
                 "</IssueCopyright>"+
              "</IssueInfo>"+
              "<Article ID=\"s12080-008-0021-5\" OutputMedium=\"All\">"+
                 "<ArticleInfo ArticleType=\"OriginalPaper\" ContainsESM=\"No\" Language=\"Language\" NumberingStyle=\"Unnumbered\" TocLevels=\"0\">"+
                          "<ArticleID>ID</ArticleID>"+
                          "<ArticleDOI>10.1066/DOI</ArticleDOI>"+
                          "<ArticleSequenceNumber>3</ArticleSequenceNumber>"+
                          "<ArticleTitle Language=\"Language\">Title</ArticleTitle>"+
                          "<ArticleCategory>Original paper</ArticleCategory>"+
                          "<ArticleFirstPage>Start</ArticleFirstPage>"+
                          "<ArticleLastPage>End</ArticleLastPage>"+
                          "<ArticleHistory>"+
                       "<RegistrationDate>"+
                          "<Year>2008</Year>"+
                          "<Month>7</Month>"+
                          "<Day>10</Day>"+
                       "</RegistrationDate>"+
                       "<Received>"+
                          "<Year>2008</Year>"+
                          "<Month>4</Month>"+
                          "<Day>3</Day>"+
                       "</Received>"+
                       "<Accepted>"+
                          "<Year>2008</Year>"+
                          "<Month>7</Month>"+
                          "<Day>3</Day>"+
                       "</Accepted>"+
                       "<OnlineDate>"+
                          "<Year>2008</Year>"+
                          "<Month>7</Month>"+
                          "<Day>30</Day>"+
                       "</OnlineDate>"+
                    "</ArticleHistory>"+
                          "<ArticleCopyright>"+
                       "<CopyrightHolderName>Copyright</CopyrightHolderName>"+
                       "<CopyrightYear>Rights</CopyrightYear>"+
                    "</ArticleCopyright>"+
                          "<ArticleGrants Type=\"Regular\">"+
                              "<MetadataGrant Grant=\"OpenAccess\"/>"+
                              "<AbstractGrant Grant=\"OpenAccess\"/>"+
                              "<BodyPDFGrant Grant=\"Restricted\"/>"+
                              "<BodyHTMLGrant Grant=\"Restricted\"/>"+
                              "<BibliographyGrant Grant=\"Restricted\"/>"+
                              "<ESMGrant Grant=\"Restricted\"/>"+
                          "</ArticleGrants>"+
                      "</ArticleInfo>"+
                 "<ArticleHeader>"+
                    "<AuthorGroup>"+
                       "<Author AffiliationIDS=\"Aff1 Aff2\" CorrespondingAffiliationID=\"Aff1\">"+
                          "<AuthorName DisplayOrder=\"Western\">"+
                             "<GivenName>A.</GivenName>"+
                             "<FamilyName>Author</FamilyName>"+
                          "</AuthorName>"+
                       "</Author>"+
                       "<Author AffiliationIDS=\"Aff1\">"+
                          "<AuthorName DisplayOrder=\"Western\">"+
                             "<GivenName>B.</GivenName>"+
                             "<FamilyName>Author</FamilyName>"+
                          "</AuthorName>"+
                       "</Author>"+
                       "<Author AffiliationIDS=\"Aff2\">"+
                          "<AuthorName DisplayOrder=\"Western\">"+
                             "<GivenName>C.</GivenName>"+
                             "<FamilyName>Author</FamilyName>"+
                          "</AuthorName>"+
                       "</Author>"+
                       "<Affiliation ID=\"Aff1\">"+
                          "<OrgDivision>Fake Division</OrgDivision>"+
                          "<OrgName>Fake Organization</OrgName>"+
                          "<OrgAddress>"+
                             "<City>FreeTown</City>"+
                             "<State>IA</State>"+
                             "<Postcode>00000</Postcode>"+
                             "<Country>USA</Country>"+
                          "</OrgAddress>"+
                       "</Affiliation>"+
                    "</AuthorGroup>"+
                    "<Abstract ID=\"Abs1\" Language=\"Language\">"+
                       "<Heading>Abstract</Heading>"+
                       "<Para>Summary</Para>"+
                    "</Abstract>"+
                    "<KeywordGroup Language=\"Language\">"+
                       "<Heading>Keywords</Heading>"+
                       "<Keyword>Keyword1</Keyword>"+
                       "<Keyword>Keyword2</Keyword>"+
                       "<Keyword>Keyword3</Keyword>"+
                    "</KeywordGroup>"+
                 "</ArticleHeader>"+
                 "<NoBody/>"+
              "</Article>"+
           "</Issue>"+
        "</Volume>"+
     "</Journal>"+
  "</Publisher>";
    
  // empty content
  String emptyContent =
      "<component>" +
      "</component>";
  
  // bad content -- not XML
  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">" +
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodDescription + " </div>\n";


  
  public void testExtractFromBadContent() throws Exception {
      
    String url = "http://clockss-ingest.lockss.org/sourcefiles/springer-dev/2012/ftp_PUB_11-11-17_06-38-38.zip!/JOU=00238/VOL=2011.34/ISU=6/ART=476/BodyRef/PDF/238_2010_Article_476.pdf";
    MockCachedUrl cu = new MockCachedUrl(url, ssau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    
    FileMetadataExtractor me =
      new SpringerSourceMetadataExtractorFactory.SpringerSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
  }

  public void testExtractFromNoTittleIssnEissnJournalIdContent() throws Exception {
    
    String url = "http://clockss-ingest.lockss.org/sourcefiles/springer-dev/2012/ftp_PUB_11-11-17_06-38-38.zip!/JOU=00238/VOL=2011.34/ISU=6/ART=476/BodyRef/PDF/238_2010_Article_476.pdf";
    MockCachedUrl cu = new MockCachedUrl(url, ssau);
    cu.setContent(noTittleIssnEissnJournalIdContent);
    cu.setContentSize(noTittleIssnEissnJournalIdContent.length());
    
    FileMetadataExtractor me =
      new SpringerSourceMetadataExtractorFactory.SpringerSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(genJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodEnd, md.get(MetadataField.FIELD_END_PAGE));
  }
  
  public void testExtractFromGoodContent() throws Exception {
    goodAuthors.add("Author, A.");
    goodAuthors.add("Author, B.");
    goodAuthors.add("Author, C.");
    goodKeywords.add("Keyword1");
    goodKeywords.add("Keyword2");
    goodKeywords.add("Keyword3");
	  
    String url = "http://clockss-ingest.lockss.org/sourcefiles/springer-dev/2012/ftp_PUB_11-11-17_06-38-38.zip!/JOU=00238/VOL=2011.34/ISU=6/ART=476/BodyRef/PDF/238_2010_Article_476.pdf";
    MockCachedUrl cu = new MockCachedUrl(url, ssau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    
    FileMetadataExtractor me =
      new SpringerSourceMetadataExtractorFactory.SpringerSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(goodDescription, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(goodRights, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(hardwiredPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodEissn, md.get(MetadataField.FIELD_EISSN));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodStart, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodEnd, md.get(MetadataField.FIELD_END_PAGE));
  }
  
  // Inner class that where a number of Archival Units can be created
  // for simulated content.
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

  // Inner class to create HTML source code simulated content.
  public static class MySimulatedContentGenerator
    extends SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }
    public String getHtmlFileContent(String filename, int fileNum, 
                                     int depth, int branchNum, 
                                     boolean isAbnormal) {
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