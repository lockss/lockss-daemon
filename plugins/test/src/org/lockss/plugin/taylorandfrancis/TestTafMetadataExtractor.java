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

package org.lockss.plugin.taylorandfrancis;

import java.io.*;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/*
 * 
 * Testing T&F Metadata extraction is complicated because we have to verify whether the CU being extracted
 * by verifying its retrieved metadata against the AU it's part of (to avoid emitting data for overcollected
 * CU's due to crawl leakage). 
 * 
 * Each test AU has a corresponding set of information in test_tandf_setup.xml tdb file
 * The AU configuration in this file and the information in the tdb file must stay in sync
 *
 */
public class TestTafMetadataExtractor extends LockssTestCase {

  private static final Logger log = Logger.getLogger(TestTafMetadataExtractor.class);

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static String BASE_URL = "http://www.tandfonline.com/";  

  
  
  // setup for TEST AU#1  
  private ArchivalUnit tfau1;  
  private static final String AU1_NAME = "Life, Pain & Death Volume 19";
  // for verification
  static String goodUrl1 = "http://www.tandfonline.com/toc/lpd/19/6";
  static String goodJournalTitle = "Life, Pain & Death";
  static String goodVolume = "19";
  static String goodISSN = "1965-1234";
  static String goodEISSN = "1965-4321";
  static String goodJID = "lpd";
  
  //setup for TEST AU#2
  private ArchivalUnit tfau2;  
  private static final String AU2_NAME = "Alternate TandF Journal Volume 4";  
  // for verification
  static String goodUrl2 = "http://www.tandfonline.com/toc/atfj/4/6";
  static String alternateJournalTitle = "Alternate TandF Journal";
  static String alternateVolume = "4";
  static String alternateISSN = "1800-5555";
  static String alternateEISSN = "1801-6666";
  static String alternateJID = "atfj";

  //setup for TEST AU#3
  private ArchivalUnit tfau3;
  private static final String AU3_NAME = "Life, Pain & Death Volume 4";  
  // for verification - same as AU#1 but different volume
  static String goodUrl3 = "http://www.tandfonline.com/toc/lpd/4/6";
  static String thirdJournalTitle = "Life, Pain & Death";
  static String thirdVolume = "4";
  static String thirdISSN = "1800-5555";
  static String thirdEISSN = "1801-6666";
  static String thirdJID = "lpd";
  
  private MockLockssDaemon theDaemon;
  

  /* (non-Javadoc)
   * @see org.lockss.test.LockssTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); 
    
    // get the daemon up and going for testing
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    
    ConfigurationUtil.addFromUrl(getResource("test_tandf_setup.xml"));
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();

    TdbAu tau1 = tdb.getTdbAusLikeName(AU1_NAME).get(0);
    assertNotNull("Didn't find named TdbAu: " + AU1_NAME, tau1);
    tfau1 = PluginTestUtil.createAndStartAu(tau1);
    assertNotNull(tfau1);
    TypedEntryMap auConfig =  tfau1.getProperties();
    assertEquals(BASE_URL, auConfig.getString(BASE_URL_KEY));
    
    TdbAu tau2 = tdb.getTdbAusLikeName(AU2_NAME).get(0);
    assertNotNull("Didn't find named TdbAu: " + AU2_NAME, tau2);
    tfau2 = PluginTestUtil.createAndStartAu(tau2);
    assertNotNull(tfau2);
    
    TdbAu tau3 = tdb.getTdbAusLikeName(AU3_NAME).get(0);
    assertNotNull("Didn't find named TdbAu: " + AU3_NAME, tau3);
    tfau3 = PluginTestUtil.createAndStartAu(tau3);
    assertNotNull(tfau3);
  }

  public void tearDown() throws Exception {
    //sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  // the metadata that should be extracted
  String goodDOI = "10.1080/09639284.2010.501577";
  String goodDOI_pt1 = "10.1080";
  String goodDOI_pt2 = "09639284.2010.501577";
  String goodIssue = "6";
  String goodStartPage = "555";
  String goodAuthor = "Melvin Harbison";
  String[] goodAuthors = new String[] {"Melvin Harbison", "Desmond McCallan"};
  String goodSubject = "food and culture; alternative treatment approaches; rhyme therapy"; // Cardinality of dc.subject is single even though content is identical to the keywords field
  String goodDescription = "The description is a lengthier statement, often with mixed punctuation. This should do; let's try it.";
  String goodDate = "2011-09-21";
  String origPublisher = "Gleeson";
  String goodPublisher = "Taylor & Francis Group";  
  String goodType = "content-article";
  String goodFormat = "text/HTML";
  String goodLanguage = "en";
  String goodCoverage = "world";
  String goodSource = "http://dx.doi.org/10.1080/09639284.2010.501577";
  String goodArticleTitle = "Something Terribly Interesting: A Stirring Report";
  String[] goodKeywords = new String[] {"food and culture", "alternative treatment approaches", "rhyme therapy"}; 
  String goodAbsUrl = "http://www.tandfonline.com/doi/abs/10.1080/09639284.2010.501577";
  String goodPdfUrl = "http://www.tandfonline.com/doi/pdf/10.1080/09639284.2010.501577";
  String goodHtmUrl = "http://www.tandfonline.com/doi/full/10.1080/09639284.2010.501577";
  
  //because the T&F UR in ris content is usually a dx.doi.org site
  private  String doiURL = "http://dx.doi.org/" + goodDOI; 

  
  /* THIS SECTION TESTS THE HTML TAG EXTRACTION */

  // Maps to TFAU1
  // a chunk of html source code from the publisher's site from where the metadata should be extracted
  String goodContentAU1 = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n"
          + "<title>Taylor &amp; Francis Online  :: Something Terribly Interesting: A Stirring Report - Research and Results - Volume 19,\nIssue 6 </title>\n"
          + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
          + "<meta name=\"robots\" content=\"noarchive,nofollow\" />\n"
          + "<link rel=\"schema.DC\" href=\"http://purl.org/DC/elements/1.0/\"></link><meta name=\"dc.Title\" content=\"" + goodArticleTitle + "\"></meta>"
          + "<meta name=\"dc.Creator\" content=\" " + goodAuthors[0] + " \"></meta>" //spaces within content field in dc.Creator appear like this in the HTML
          + "<meta name=\"dc.Creator\" content=\" " + goodAuthors[1] + " \"></meta>" 
          + "<meta name=\"dc.Subject\" content=\"" + goodSubject + "\"></meta>" 
          + "<meta name=\"dc.Description\" content=\"" + goodDescription + "\"></meta>"
          + "<meta name=\"dc.Publisher\" content=\" " + origPublisher + " \"></meta>" //spaces within content field in dc.Publisher appear like this in the HTML
          + "<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"" + goodDate + "\"></meta>"
          + "<meta name=\"dc.Type\" content=\"" + goodType + "\"></meta>"
          + "<meta name=\"dc.Format\" content=\"" + goodFormat + "\"></meta>"
          + "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"501577\"></meta>"
          + "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"" + goodDOI + "\"></meta>"
          + "<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"Life, Pain &amp; Death, Vol. 19, No. 6, December 2010, pp. 555-567\"></meta>"
          + "<meta name=\"dc.Source\" content=\"" + goodSource + "\"></meta>"
          + "<meta name=\"dc.Language\" content=\"" + goodLanguage + "\"></meta>"
          + "<meta name=\"dc.Coverage\" content=\"" + goodCoverage + "\"></meta>"
          + "<meta name=\"keywords\" content=\"" + goodKeywords[0] + "; " + goodKeywords[1] + "; " + goodKeywords[2] + "\"></meta>";
  
  /**
   * Method that creates a simulated Cached URL from the source code provided by the goodContent String.
   * It then asserts that the metadata extracted with TaylorAndFrancisHtmlMetadataExtractorFactory
   * match the metadata in the source code. 
   * @throws Exception
   */
    public void testExtractFromGoodContentAU1() throws Exception {
    List<ArticleMetadata> mdlist;
    
    mdlist = setupContentForAU(tfau1, goodUrl1, goodContentAU1);
    
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodAuthors[0], md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodDate, md.get(MetadataField.DC_FIELD_DATE));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodSubject, md.get(MetadataField.DC_FIELD_SUBJECT));
    assertEquals(goodDescription, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(goodPublisher, md.get(MetadataField.DC_FIELD_PUBLISHER));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodFormat, md.get(MetadataField.DC_FIELD_FORMAT));
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodCoverage, md.get(MetadataField.DC_FIELD_COVERAGE));
    assertEquals(goodSource, md.get(MetadataField.DC_FIELD_SOURCE));
    assertEquals(Arrays.asList(goodKeywords), md.getList(MetadataField.FIELD_KEYWORDS));
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
 
  public void testExtractFromBadContent() throws Exception {
    List<ArticleMetadata> mdlist;
    
    mdlist = setupContentForAU(tfau1, goodUrl1, badContent);
    // nothing was emitted because we couldn't get enough metadata to verify it belongs in this au
    assertEmpty(mdlist);
  }	

  // Maps to TFAU3
  String encodedContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n"
      + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n"
      + "<meta name=\"dc.Identifier\" scheme=\"coden\" content=\""
      + "\u201c" + goodJournalTitle + "\u201d" + ", Vol. 4, No. 6, December 2010, pp. 555" + "\u2013" + "567\"></meta>"
      + "<meta name=\"dc.Title\" content=\""
      + "\u201c" + goodJournalTitle + "\u201d" + "\"></meta>"      
      + "</head>";
  
  // Even though it has encoded quotation marks, the titles will match because
  // the title is a subset of the other
  public void testEncodedContent() throws Exception {
    String goodTitle = "\u201c" + goodJournalTitle + "\u201d";
    List<ArticleMetadata> mdlist;
    
    mdlist = setupContentForAU(tfau3, goodUrl3, encodedContent);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodTitle, md.get(MetadataField.DC_FIELD_TITLE));
    assertEquals("555", md.get(MetadataField.FIELD_START_PAGE));
  }

  // Maps to TFAU1
  // When a the coden doesn't have spaces before the comma, it can cause problems - note the use of -u2013 for the hyphen - endash - encoding safe
  String noSpaceContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html lang=\"en\" xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n" +
      "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>\n" +
      "<meta name=\"dc.Title\" content=\"An Integrative, Empowerment Model for Helping Lesbian, Gay, and Bisexual Youth Negotiate the Coming-Out Process\"></meta>" +
      "<meta name=\"dc.Publisher\" content=\" Taylor &amp; Francis Group \"></meta>" +
      "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"678176\"></meta>" +
      "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"10.1080/15538605.2012.678176\"></meta>" +
      "<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"" + goodJournalTitle +",Vol. 19, No. 2, April-June 2012, pp. 96\u2013117\"></meta>";

  public void testNoSpaceContent() throws Exception {
    String goodTitle = "An Integrative, Empowerment Model for Helping Lesbian, Gay, and Bisexual Youth Negotiate the Coming-Out Process";
    List<ArticleMetadata> mdlist;
    
    mdlist = setupContentForAU(tfau1, goodUrl1, noSpaceContent);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodTitle, md.get(MetadataField.DC_FIELD_TITLE));
    assertEquals("96", md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
  }
  
  // Metadata is for Irish Educational Studies, Vol 31
  // which does not match the TFAU2 against which we're testing
  String wrongJournalContent =
      "<meta name=\"dc.Title\" content=\"Engagement\"></meta>"+
          "<meta name=\"dc.Creator\" content=\" Stephanie   Burley \"></meta>"+
          "<meta name=\"dc.Publisher\" content=\" Taylor &amp; Francis Group \"></meta>"+
          "<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"2012-06-19\"></meta>"+
          "<meta name=\"dc.Format\" content=\"text/HTML\"></meta>"+
          "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"596666\"></meta>"+
          "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"10.1080/03323315.2011.596666\"></meta>"+
          "<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"Irish Educational Studies, Vol. 31, No. 2, June 2012, pp. 175\u2013190\"></meta>"+
          "<meta name=\"dc.Source\" content=\"http://dx.doi.org/10.1080/03323315.2011.596666\"></meta>";
  
  public void testWrongJournalTitleContent() throws Exception {
   List<ArticleMetadata> mdlist;
    
    mdlist = setupContentForAU(tfau2, goodUrl2, wrongJournalContent);
    // nothing got emitted because it shouldn't have been in this AU
    assertEmpty(mdlist);
  }

// Because it can't pick up a Journal Title or Volume from the coden line, it doesn't emit
  String badIdentifierContent =
      "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"567009\"></meta>" +
"<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"10.1080/13567888.2011.567009\"></meta>" + 
"<meta name=\"dc.Identifier\" scheme=\"coden\" content=\"Volume 17, Comment 1 � January 2011\"></meta>";
  
  public void testbadIdentifierContent() throws Exception {
   List<ArticleMetadata> mdlist;
    
    mdlist = setupContentForAU(tfau2, goodUrl2, wrongJournalContent);
    // nothing got emitted because it shouldn't have been in this AU
    assertEmpty(mdlist);
  }
  
  

  /* THIS SECTION TESTS THE RIS EXTRACTION */
  /*
   * TY  - JOUR
  TY  - JOUR
  T1  - Title of Article
  AU  - Author1, Suzie
  AU  - Author2, Kevin
  Y1  - 2011/02/18
  PY  - 2011
  DA  - 2011/04/07
  N1  - doi: 10.1080/19419899.2010.534489
  DO  - 10.1080/19419899.2010.534489
  T2  - Psychology & Sexuality
  JF  - Psychology & Sexuality
  JO  - Psychology & Sexuality
  SP  - 159
  EP  - 180
  VL  - 2
  IS  - 2
  PB  - Routledge
  SN  - 1941-9899
  M3  - doi: 10.1080/19419899.2010.534489
  UR  - http://dx.doi.org/10.1080/19419899.2010.534489
  Y2  - 2013/07/26
  ER  - 
   * 
   */
  
  String goodRisDate = "2011/09/21";
  String goodEndPage = "666";
  String goodImprintPublisher = "Routledge";
  
  private String createGoodRisContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - JOUR");
    for(String auth : goodAuthors) {
      sb.append("\nAU  - ");
      sb.append(auth);
    }
    sb.append("\nY1  - ");
    sb.append(goodRisDate);
    sb.append("\nDA  - ");
    sb.append(goodRisDate);
    sb.append("\nJO  - ");
    sb.append(goodJournalTitle);
    sb.append("\nSP  - ");
    sb.append(goodStartPage);
    sb.append("\nEP  - ");
    sb.append(goodEndPage);
    sb.append("\nVL  - ");
    sb.append(goodVolume);
    sb.append("\nIS  - ");
    sb.append(goodIssue);
    sb.append("\nSN  - ");
    sb.append(goodISSN);
    sb.append("\nT1  - ");
    sb.append(goodArticleTitle);
    sb.append("\nPB  - ");
    sb.append(goodImprintPublisher);
    sb.append("\nDO  - ");
    sb.append(goodDOI);
    sb.append("\nUR  - ");
    sb.append(doiURL);
    sb.append("\nER  - ");
    return sb.toString();
  }
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the MetaPressRisMetadataExtractorFactory, match the metadata in the source code. 
   * This matches to TFAU1
   * @throws Exception
   */
  public void testExtractGoodRisContent() throws Exception {
    String goodContent = createGoodRisContent();
    log.debug3(goodContent);
    String url = BASE_URL + "action/downloadCitation?doi=" + goodDOI_pt1 + "%2F" + goodDOI_pt2 + "&format=ris&include=cit";  
    UrlData ud = new UrlData(IOUtils.toInputStream(goodContent, "utf-8"),getContentRisProperties(),url);
    UrlCacher uc = tfau1.makeUrlCacher(ud);
    uc.storeContent();
    CachedUrl cu = uc.getCachedUrl();
    
    FileMetadataExtractor me = new TafRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/plain");
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);

    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodEndPage, md.get(MetadataField.FIELD_END_PAGE));
    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    Iterator<String> actAuthIter = md.getList(MetadataField.FIELD_AUTHOR).iterator();
    for(String expAuth : goodAuthors) {
      assertEquals(expAuth, actAuthIter.next());
    }
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodRisDate, md.get(MetadataField.FIELD_DATE));

    assertEquals(goodImprintPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    // The access url should not be set. Daemon will use full_text_cu
    assertNotEquals(doiURL, md.get(MetadataField.FIELD_ACCESS_URL));

  }
  
  private String createWrongAURisContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - JOUR");
    for(String auth : goodAuthors) {
      sb.append("\nAU  - ");
      sb.append(auth);
    }
    sb.append("\nDA  - ");
    sb.append(goodRisDate);
    sb.append("\nJO  - ");
    sb.append(alternateJournalTitle);
    sb.append("\nVL  - ");
    sb.append(goodVolume);
    sb.append("\nER  - ");
    return sb.toString();
  }
  
  public void testExtractWrongAURisContent() throws Exception {
    String wrongContent = createWrongAURisContent();
    log.debug3(wrongContent);
    String url = BASE_URL + "action/downloadCitation?doi=" + goodDOI_pt1 + "%2F" + goodDOI_pt2 + "&format=ris&include=cit";  
    UrlData ud = new UrlData(IOUtils.toInputStream(wrongContent, "utf-8"),getContentRisProperties(),url);
    UrlCacher uc = tfau1.makeUrlCacher(ud);
    uc.storeContent();
    CachedUrl cu = uc.getCachedUrl();
    
    FileMetadataExtractor me = new TafRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/plain");
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);

    assertNotEmpty(mdlist);  // no longer check tdb title vs metadata data on Oct/2021

  }
  
  public void testTitleNormalization() throws Exception {
 
    
    assertEquals("one two three", TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle("One Two Three"));
    assertEquals("one and three",TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle("One & Three"));
    assertEquals("one and three",TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle("One and Three"));
    assertEquals("one two three", TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle("The One Two Three"));
    assertEquals( "theatre is great", TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle("Theatre is Great"));
    assertEquals("theatre is great", TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle("The Theatre is Great"));
    assertEquals(TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle("The Title of the Book & Other Mysteries"),
        TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle("the title of the book and other mysteries"));
    
    String titleWithoutControlChars = "A long hyphenated-title with a \"wonderful\" inset-quotation";
    String titleWithControlChars = "A long hyphenated\u2013title with a \u201cwonderful\u201d inset\u2013quotation";
    assertNotEquals(titleWithoutControlChars, titleWithControlChars);
    // normalize both
    assertEquals(TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle(titleWithControlChars),
        TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle(titleWithoutControlChars));
    // normalize only the first, lower case the one without control chars
    assertEquals(titleWithoutControlChars.toLowerCase(), TaylorAndFrancisHtmlMetadataExtractorFactory.normalizeTitle(titleWithControlChars));
  }


  /* private support methods */
  
  private List<ArticleMetadata> setupContentForAU(ArchivalUnit au, String url, String content) throws IOException, PluginException {
    UrlData ud = new UrlData(IOUtils.toInputStream(content, "utf-8"),getContentHtmlProperties(),url);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
    CachedUrl cu = uc.getCachedUrl();
    FileMetadataExtractor me = new TaylorAndFrancisHtmlMetadataExtractorFactory.TaylorAndFrancisHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    return mle.extract(MetadataTarget.Any(), cu);
  }
  
  private CIProperties getContentHtmlProperties() {
    CIProperties cProps = new CIProperties();
    // the CU checks the X-Lockss-content-type, not the content-type to determine encoding
    cProps.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html; charset=UTF-8");
    cProps.put("Content-type",  "text/html; charset=UTF-8");
    return cProps;
  }
  private CIProperties getContentRisProperties() {
    CIProperties cProps = new CIProperties();
    // the CU checks the X-Lockss-content-type, not the content-type to determine encoding
    cProps.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain; charset=UTF-8");
    cProps.put("Content-type",  "text/plain; charset=UTF-8");
    return cProps;
  }
}

