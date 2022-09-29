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

package org.lockss.plugin.elsevier;

import java.util.*;
import java.util.regex.Pattern;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.elsevier.ElsevierTocMetadataExtractorFactory.ElsevierTocMetadataExtractor;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://i-perception.perceptionweb.com/journal/I/volume/1/article/i0402 
 */
public class TestElsevierTocMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestElsevierTocMetadataExtractorFactory");

  private ArchivalUnit hau;		//Elsevier AU
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.elsevier.ClockssElsevierSourcePlugin";

  private static String BASE_URL = "http://test.com/";
  private static String YEAR = "2012";
  // use a global cu for the dataset.toc - set contents in individual tests
  private static MockCachedUrl DCU;

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, elsevierFtpAuConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }


  Configuration elsevierFtpAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", YEAR);
    return conf;
  }
  
  public void testFindingDataset() throws Exception {
    String OXMurl = BASE_URL + YEAR +"/OXM30010/00029343.tar!/01250008/12000332/main.pdf";
    String OMurl = BASE_URL + YEAR + "/OM08032A/OM08032A.tar!/00992399/003405SS/07011582/main.pdf";
    String OXM_dataset_url= BASE_URL + YEAR + "/OXM30010/dataset.toc";
    String OM_dataset_url = BASE_URL + YEAR + "/OM08032A/OM08032A.tar!/dataset.toc";
   
    ElsevierTocMetadataExtractor me =
      new ElsevierTocMetadataExtractorFactory.ElsevierTocMetadataExtractor();
    assertNotNull(me);
    
    //Make sure that the extractor will correctly find the expected dataset file
    assertEquals(OM_dataset_url, me.getToc(OMurl));
    assertEquals(OXM_dataset_url, me.getToc(OXMurl));   
    
    // Make sure the extractor will correctly find the expected PDF from the data in the 
    // dataset.toc file, depending on what the dataset_url is
    // use the testing extractor in order to let class know where the dataset url is
    TestingElsevierTocMetadataExtractor tme =
        new TestingElsevierTocMetadataExtractor();
    assertNotNull(tme);
    String OXM_t3 = "OXM30010 00029343 01250008 12000332";
    String OM_t3 = "OM08032A 00992399 003405SS 07011582";
    //Make sure that the extractor will correctly find the expected dataset file
    assertEquals(OMurl, tme.getUrlFrom(OM_t3,OM_dataset_url));
    assertEquals(OXMurl, tme.getUrlFrom(OXM_t3, OXM_dataset_url));   
  }
  
  String goodVolume = "volume";
  String goodDate = "2011-07-23";
  String goodUrl = "http://test.com/2012/0XFAKE0X/1231231.tar!/3453453/5675675/main.pdf";
  String goodJournal = "Journal";
  String goodDoi = "10.1016/d.rhodes.2011.05.001";
  String goodLanguage = "EN";
  String goodIssue = "Issue";
  String goodRights = "Rights";
  String goodTitle = "This tests a title which spans two lines";
  List<?> goodAuthors = 
      ListUtil.list("AuthorLongName, A.", "AuthorLongerName, B.",
          "AuthorEvenLongerName, C.","AuthorWantsALongName, D.",
          "AuthorHasALongerName, E.", "AuthorHasAnotherLongerName, F.",
          "AuthorHasATerriblyLongerName, G.");
  String goodSummary = "This summary also spans several lines";
  List<?> goodKeywords = ListUtil.list("testing1", "testing2",  "testing3");
  String goodStart = "10";
  String goodEnd = "20";
 
  //Represents a metadata section for two dummy articles structured as in dataset.toc
  String goodContent =		  
      "_t1 Issn\n"+
          "_t3 0XFAKE0X 1231231 3453453 5675675\n"+
          "_ps [PS000]\n"+
          "_ii S0000-0000(00)00000-0\n"+
          "_ii [DOI] 55.5555/d.rhodes.2011.05.010\n"+
          "_ty FLA\n"+
          "_t1 Issn\n"+
          "_pd Date\n"+
          "_jn Journal\n"+
          "_cr Rights\n"+ 
          "_is Issue\n"+
          "_la EN\n"+
          "_ti This tests a title which\n"+
          " spans two lines\n"+
          "_au AuthorLongName, A.\n"+
          "_au AuthorLongerName, B.\n"+
          "_au AuthorEvenLongerName, C.\n"+
          "_au AuthorWantsALongName, D.\n"+
          "_au AuthorHasALongerName, E.\n"+
          "_au AuthorHasAnotherLongerName, F.\n"+
          "_au AuthorHasATerriblyLongerName, G.\n"+
          "_ca AuthorLongName, A.\n"+
          "_ab This summary\n"+
          "	also spans\n"+
          "	several lines\n"+ 
          "_dt 20110723\n"+
          "_la EN\n"+
          "_ii S0000-0000(00)00000-0\n"+
          "_vl volume\n"+
          "_ii [DOI] 10.1016/d.rhodes.2011.05.001\n"+
          "_ty FLA\n"+
          "_li EN\n"+
          "_kw testing1\n"+
          "_kw testing2\n"+
          "_kw testing3\n"+
          "_pg 10-20\n"+
          "_mf [XML JA 5.1.0 ARTICLE] main\n"+
          "_mf [PDF 1.7 6.2 DISTILLED OPTIMIZED BOOKMARKED] main\n"+
          "_mf [Raw ASCII] main\n"+
          "_t3 0XFAKE0X 1231231 3453453 6786786\n"+
          "_ps [PS000]\n"+
          "_dt 201201\n"+
          "_ti This tests a title which\n"+
          " spans two lines\n"+
          "_au Author, A.\n"+
          "_au Author, B.\n"+
          "_au Author, C.\n"+
          "_au Author, D.\n"+
          "_au Author, E.\n"+
          "_ca Author, A.\n"+
          "_ab This summary\n"+
          " also spans\n"+
          " several lines\n"+
          "_la EN\n"+
          "_kw test1\n"+
          "_kw test2\n"+
          "_kw test3\n"+
          "_jn Journal\n"+
          "_pg 20-30\n"+
          "_mf [XML JA 5.1.0 ARTICLE] main\n"+
          "_mf [PDF 1.7 6.2 DISTILLED OPTIMIZED BOOKMARKED] main\n"+
          "_mf [Raw ASCII] main";

  /* 
   * This is hard to test because the ElsevierTocMetadataExtractor goes and finds
   * the metadata CachedUrl - so for looking at content, use our Test version of the extractor
   * which returns the necessary MockCachedUrl instead of looking in the AU for a real
   * CachedUlr
   */
  public void testExtractFromGoodContent() throws Exception {
    // one of possible PDFs defined by the good content
    String url = BASE_URL + YEAR + "/0XFAKE0X/1231231.tar!/3453453/5675675/main.pdf";
    String dataset_url = BASE_URL + YEAR + "/0XFAKE0X/dataset.toc";
    // no need to give it content, it isn't accessed
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    // Set up the dataset_toc info in to the global DCU returned by the test extractor
    DCU = new MockCachedUrl(dataset_url, hau);
    DCU.setContent(goodContent);
    DCU.setContentSize(goodContent.length());
    DCU.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");    
    ElsevierTocMetadataExtractor me =
      new TestingElsevierTocMetadataExtractor();
    assertNotNull(me);
    
    //Make sure that the extractor will correctly find the expected dataset file
    String calculated_dataset_url = me.getToc(url);
    assertEquals(dataset_url, calculated_dataset_url);
    
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    // this will use the pdf url to find the correctly named DCU
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodUrl, md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodSummary, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(goodKeywords,  md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(goodStart, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodRights, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodEnd, md.get(MetadataField.FIELD_END_PAGE));
  }
  
  
  String testContent = 
      "_t1 OXH26350 02726386\n" + 
      "_jn American Journal of Kidney Diseases\n" +
      "_cr Copyright (c) 2011 The National Kidney Foundation, Inc.\n" +
      "_t2 OXH26350 02726386 005901S1\n" +
      "_ps [S300]\n" +
      "_vl 59\n" +
      "_is 1\n" +
      "_pr A1-A8,e1-e420\n" +
      "_cf [name] United States Renal Data System 2011 Annual Data Report: Atlas\n" +
      "    of Chronic Kidney Disease & End-Stage Renal Disease in the United States\n" +
      "_xt Supplement 1\n" +
      "_dt 201201\n" +
      "_t3 OXH26350 02726386 005901S1 11016325\n" +
      "_ps [S300]\n" +
      "_ii S0272-6386(11)01632-5\n" +
      "_ii [DOI] 10.1053/S0272-6386(11)01632-5\n" +
      "_ty MIS\n" +
      "_li EN\n" +
      "_ti Masthead\n" +
      "_pg 6A,8A,10A,12A,14A,16A,18A-19A\n" +
      "_mf [PDF 1.7 6.2 DISTILLED OPTIMIZED] main\n" +
      "_mf [XML JA 5.1.0 SIMPLE-ARTICLE] main\n" +
      "_mf [Raw ASCII] main\n";

  String testVolume = "59";
  String testDate = "2012-01";
  String testUrl = "http://test.com/2012/OXH26350/02726386.tar!/005901S1/11016325/main.pdf";
  String testJournal = "American Journal of Kidney Diseases";
  String testIssn = "0272-6386";
  String testDoi = "10.1053/S0272-6386(11)01632-5";
  String testLanguage = null;
  String testIssue = "1";
  String testRights = "Copyright (c) 2011 The National Kidney Foundation, Inc.";
  String testTitle = "Masthead";
  String testAuthors = null;
  String testSummary = null;
  String testKeywords = null;
  String testStart = "6A";
  String testEnd = "19A";
  String testPublisher = "Elsevier";
  
  public void testExtractFromTestContent() throws Exception {
    //String url = "OXH26350 02726386 005901S1 11016325";
    // the pdf defined by the test content
    String url = BASE_URL + YEAR + "/OXH26350/02726386.tar!/005901S1/11016325/main.pdf";
    String dataset_url = BASE_URL + YEAR + "/OXH26350/dataset.toc";
    // no need to give the PDF contents, it's not checked
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    // create and fill the global test dataset CU
    DCU = new MockCachedUrl(dataset_url, hau);
    DCU.setContent(testContent);
    DCU.setContentSize(testContent.length());
    DCU.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");        

    ElsevierTocMetadataExtractor me =
      new TestingElsevierTocMetadataExtractor();
    assertNotNull(me);
    
    //Make sure that the extractor will correctly find the expected dataset file
    String calculated_dataset_url = me.getToc(url);
    assertEquals(dataset_url, calculated_dataset_url);    
    
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(testVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(testDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(testUrl, md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(testJournal, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(testIssn, md.get(MetadataField.FIELD_ISSN));
    assertEquals(testDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(testLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(testAuthors, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(testSummary, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(testKeywords, md.get(MetadataField.FIELD_KEYWORDS));
    assertEquals(testStart, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(testRights, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(testIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(testTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(testEnd, md.get(MetadataField.FIELD_END_PAGE));
    assertEquals(testPublisher, md.get(MetadataField.FIELD_PUBLISHER));
  }

  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
    "<meta name=\"foo\"" +  " content=\"bar\">\n" +
    "  <div id=\"issn\">\n" +
    "_t2 0XFAKE0X 1231231 3453453 6786786\n"+
    "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " +
    goodSummary + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = BASE_URL + YEAR + "/0XFAKE0X/234234.tar!/456456/567567/main.pdf";    
    String dataset_url = BASE_URL + YEAR + "/0XFAKE0X/dataset.toc";

    MockCachedUrl cu = new MockCachedUrl(url, hau);
    // Set up the dataset_toc info in to the global DCU returned by the test extractor
    DCU = new MockCachedUrl(dataset_url, hau);
    DCU.setContent(badContent);
    DCU.setContentSize(badContent.length());
    DCU.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");       
    ElsevierTocMetadataExtractor me =
      new TestingElsevierTocMetadataExtractor();
    assertNotNull(me);
    
    //Make sure that the extractor will correctly find the expected dataset file
    String calculated_dataset_url = me.getToc(url);
    assertEquals(dataset_url, calculated_dataset_url);
    
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertEmpty(mdlist);
  }
  
  /*
   * The ElsevierTocMetadataExtractor generates the dataset.toc url inside the 
   * extract() method and then creates a CachedUrl() and uses that. 
   * There is no easy way to hook this in to a test framework which requires a 
   * MockCachedUrl that we can put contents in to. So work around this by 
   * creating a subclass of ElsevierTocMetadatExtractor and overriding the
   * method that creates the dataset.toc 
   */
  public static class TestingElsevierTocMetadataExtractor extends ElsevierTocMetadataExtractor  {
    protected static Pattern DATE_PATTERN = 
        Pattern.compile("^http://[^/]+/([^/]+)/", Pattern.CASE_INSENSITIVE);


    @Override
    protected CachedUrl getMetadataCU(String metadata_url_string, CachedUrl pdfCu) {
      return DCU;
     }
    
    protected String getUrlFrom(String identifier, String dataset_url)
    {
      
      super.base_url = BASE_URL;
      super.year = YEAR;
      super.dsetInTar =  dataset_url.contains(".tar!/");
      return super.getUrlFrom(identifier);
    }
  }
}