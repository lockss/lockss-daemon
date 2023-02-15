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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;


public class TestElsevierDTD5XmlMetadataExtractor extends SourceXmlMetadataExtractorTest {

  private static final Logger log = Logger.getLogger(TestElsevierDTD5XmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  protected ArchivalUnit tarAu;

  private static String PLUGIN_NAME = "org.lockss.plugin.elsevier.ClockssElsevierDTD5SourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  private static String YEAR_NAME = "2014";
  private static String TAR_A_BASE = BASE_URL + YEAR_NAME + "/CLKS003A.tar";
  private static String TAR_B_BASE = BASE_URL + YEAR_NAME + "/CLKS003B.tar";
  private static String SUBDIR = "!/CLKS003/"; 
  private static String BOOK_A_BASE = BASE_URL + YEAR_NAME + "/CLKSB001A.tar";
  private static String BOOK_B_BASE = BASE_URL + YEAR_NAME + "/CLKSB001B.tar";

  CIProperties  tarHeader;

  /* for testing validation */
  private static Map<String, String> pubTitleMap;
  private static Map<String, String> dateMap;
  private static Map<String, String> accessUrlMap;
  private static Map<String, String> volMap;
  private static Map<String, String> issueMap;
  private static Map<String, List<String>>authorMap;
  // for testing books do a doi to isbn mapping
  private static Map<String, String> isbnMap;
  
  static FileMetadataListExtractor els_mle;
  static FileMetadataListExtractor nocheck_mle;

  private static final String testDatasetFile = "testDataset.xml";
  private static final String realTARFile_A = "CLKS003A.tar";
  private static final String realTARFile_B = "CLKS003B.tar";

  private static final String realBookTar_A = "CLKSB001A.tar";
  private static final String realBookTar_B = "CLKSB001B.tar";
  

  public void setUp() throws Exception {
    super.setUp();

    tarHeader = new CIProperties();   
    tarHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/tar");
    tarAu = createTarAu();
    
    // for tests that also check for content
    els_mle =
        new FileMetadataListExtractor(new ElsevierDTD5XmlSourceMetadataExtractorFactory.ElsevierDTD5XmlSourceMetadataExtractor());
    // for tests that use a no-check-for-pdf version of the extractor
    nocheck_mle = new FileMetadataListExtractor(new TestElsevierDTD5MetadataExtractor());
    setUpExpectedJournalTarContent();
    setUpExpectedBookTarContent();
  }

  protected ArchivalUnit createTarAu() throws ArchivalUnit.ConfigurationException {
    // in this directory this is file "test_elsevierdtd5.tdb" but it becomes xml
    try {
      ConfigurationUtil.addFromUrl(getResource("test_elsevierdtd5.xml"));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.error(e.getMessage(), e);
    }
    Tdb tdb = ConfigManager.getCurrentConfig().getTdb();
    TdbAu tdbau1 = tdb.getTdbAusLikeName("Elsevier Source Content 2014").get(0);
    assertNotNull("Didn't find named TdbAu",tdbau1);
    return
        PluginTestUtil.createAndStartAu(tdbau1);
  }  

  /*
   * The tests to run for this class
   */
  
  public void testSimpleMainXML() throws Exception {
    log.debug3("testSimpleMainXML");
    String xml_url = TAR_A_BASE + SUBDIR + "01420615/v64sC/S0142061514004608/main.xml";
    List<ArticleMetadata> mdList = extractFromContent(xml_url, "text/xml", simpleMain, nocheck_mle, null);
    assertEquals(1, mdList.size());
    validateSingleMainMetadataRecord(mdList.get(0), "10.1016/j.jidx.2014.07.028", "article");
    
  }
  public void testSimpleDatasetXML() throws Exception {
    log.debug3("testSimpleDatasetXML");
    String file_input =  StringUtil.fromInputStream(getResourceAsStream(testDatasetFile));
    String xml_url = TAR_A_BASE + SUBDIR + "dataset.xml";

    List<ArticleMetadata> mdList = extractFromContent(xml_url, "text/xml", file_input, nocheck_mle, null);
    assertEquals(6, mdList.size());
    Iterator<ArticleMetadata> mdIt = mdList.iterator();
    ArticleMetadata mdRecord = null;
    while (mdIt.hasNext()) {
      mdRecord = (ArticleMetadata) mdIt.next();
      validateDatasetMetadataRecord(mdRecord);
    }
    
  }  
  
  public void testFunctionalFromJournalTarHierarchy() throws Exception {
    log.debug3("in testFromTarHierarchy");
    // load the tarballs
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realTARFile_A);
      //UrlCacher uc = au.makeUrlCacher(TAR_A_BASE);
      //uc.storeContent(file_input, tarHeader);
      UrlCacher uc = tarAu.makeUrlCacher(new UrlData(file_input, tarHeader, TAR_A_BASE));
      uc.storeContent();
      IOUtil.safeClose(file_input);

      file_input = getResourceAsStream(realTARFile_B);
      //uc = au.makeUrlCacher(TAR_B_BASE);
      //uc.storeContent(file_input, tarHeader);
      uc = tarAu.makeUrlCacher(new UrlData(file_input, tarHeader, TAR_B_BASE));
      uc.storeContent();
      IOUtil.safeClose(file_input);

    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.error(e.getMessage(), e);
    }finally {
      IOUtil.safeClose(file_input);
    }

    CachedUrlSet cus = tarAu.getAuCachedUrlSet();
    for (CachedUrl cu : cus.getCuIterable()) {
      log.debug3("AU - cu is: " + cu.getUrl());
      cu.release();
    }

    // We need to start from the level of the ArticleMetadataExtractor
    MyListEmitter emitter = new MyListEmitter();
    ArticleMetadataExtractor amEx = new ElsevierDeferredArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);

    Iterator<ArticleFiles> it = tarAu.getArticleIterator(MetadataTarget.Any());
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.debug3("Metadata test - articlefiles " + af.toString());
      //CachedUrl cu = af.getFullTextCu();
      CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      log.debug3("metadata cu is " + cu.getUrl());
      //List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
      amEx.extract(MetadataTarget.Any(), af, emitter);
      List<ArticleMetadata> returnList = emitter.getAmList();

      assertNotNull(returnList);
      log.debug3("size of returnList is " + returnList.size());
      Iterator<ArticleMetadata> mdIt = returnList.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        validateCompleteJournalMetadataRecord(mdRecord);
      }      
    }
  }
  
  public void testFunctionalFromBookTar() throws Exception {
    log.debug3("in testFromBookTar");
    // load the tarballs
    InputStream file_input = null;
    try {
      file_input = getResourceAsStream(realBookTar_A);
      UrlCacher uc = tarAu.makeUrlCacher(new UrlData(file_input, tarHeader, BOOK_A_BASE));
      uc.storeContent();
      IOUtil.safeClose(file_input);

      file_input = getResourceAsStream(realBookTar_B);
      uc = tarAu.makeUrlCacher(new UrlData(file_input, tarHeader, BOOK_B_BASE));
      uc.storeContent();
      IOUtil.safeClose(file_input);

    } catch (IOException e) {
      // TODO Auto-generated catch block
      log.error(e.getMessage(), e);
    }finally {
      IOUtil.safeClose(file_input);
    }

    CachedUrlSet cus = tarAu.getAuCachedUrlSet();
    for (CachedUrl cu : cus.getCuIterable()) {
      log.debug3("AU - cu is: " + cu.getUrl());
      cu.release();
    }

    // We need to start from the level of the ArticleMetadataExtractor
    MyListEmitter emitter = new MyListEmitter();
    ArticleMetadataExtractor amEx = new ElsevierDeferredArticleMetadataExtractor(ArticleFiles.ROLE_ARTICLE_METADATA);

    Iterator<ArticleFiles> it = tarAu.getArticleIterator(MetadataTarget.Any());
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.debug3("Metadata test - articlefiles " + af.toString());
      //CachedUrl cu = af.getFullTextCu();
      CachedUrl cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      log.debug3("metadata cu is " + cu.getUrl());
      //List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
      amEx.extract(MetadataTarget.Any(), af, emitter);
      List<ArticleMetadata> returnList = emitter.getAmList();

      assertNotNull(returnList);
      log.debug3("size of returnList is " + returnList.size());
      Iterator<ArticleMetadata> mdIt = returnList.iterator();
      ArticleMetadata mdRecord = null;
      while (mdIt.hasNext()) {
        mdRecord = (ArticleMetadata) mdIt.next();
        validateCompleteBookMetadataRecord(mdRecord);
      }      
    }
  }
  

  /*
   *  The supporting methods
   */
  private void setUpExpectedJournalTarContent() {
    /* maps the DOIs in the metadata to the expected values */
    log.debug3("setUpExpectedJournalTarContent");
    pubTitleMap = 
        new HashMap<String, String>();
    {
      pubTitleMap.put("10.1016/j.jidx.2014.07.028", "International Journal of XXX");
      pubTitleMap.put("10.1016/j.jidx2.2014.05.013", "Revista");
      pubTitleMap.put("10.1016/S1473-1111(14)70840-0", "The Journal");
      pubTitleMap.put("10.1016/S0140-1111(14)61865-1", "The Other Journal");
      pubTitleMap.put("10.1016/j.foo.2014.08.001", "Foo");
      pubTitleMap.put("10.1016/j.foo.2014.08.123", "Foo");
    };

    dateMap = 
        new HashMap<String, String>();
    {
      dateMap.put("10.1016/j.jidx.2014.07.028", "2014-07-30");
      dateMap.put("10.1016/j.jidx2.2014.05.013", "2014-07-09");
      dateMap.put("10.1016/S1473-1111(14)70840-0", "2014-09-01");
      dateMap.put("10.1016/S0140-1111(14)61865-1", "2014");// will get from main.xml as backup
      dateMap.put("10.1016/j.foo.2014.08.001", "2014-08-20");
      dateMap.put("10.1016/j.foo.2014.08.123", "2014-08-20"); 
    };

    accessUrlMap =
        new HashMap<String, String>();
    {
      accessUrlMap.put("10.1016/j.jidx.2014.07.028", TAR_A_BASE + SUBDIR + "01420615/v64sC/S0142061514004608/main.pdf");
      accessUrlMap.put("10.1016/j.jidx2.2014.05.013", TAR_A_BASE + SUBDIR + "00349356/v61i9/S0034935614001819/main.pdf");
      accessUrlMap.put("10.1016/S1473-1111(14)70840-0", TAR_A_BASE + SUBDIR + "14733099/v14i10/S1473309914708400/main.pdf");
      accessUrlMap.put("10.1016/S0140-1111(14)61865-1", TAR_B_BASE + SUBDIR + "01406736/v384sS1/S0140673614618651/main.pdf");
      accessUrlMap.put("10.1016/j.foo.2014.08.001", TAR_B_BASE + SUBDIR + "00191035/v242sC/S0019103514004151/main.pdf");
      accessUrlMap.put("10.1016/j.foo.2014.08.123", TAR_B_BASE + SUBDIR + "00191035/v242sC/S0019103514003856/main.pdf");
    };

    ArrayList<String> goodAuthors = new ArrayList<String>();
    {
      goodAuthors.add("Writer, Bob");
      goodAuthors.add("Q. Text, Samantha");
    }
    ArrayList<String> simpleAuthors = new ArrayList<String>();
    {
      simpleAuthors.add("Simple, Josh");
    }
    ArrayList<String> extendedAuthors = new ArrayList<String>();
    {
      extendedAuthors.add("Writer, Bob");
      extendedAuthors.add("Q. Text, Samantha");
      extendedAuthors.add("The COLLABORATIVE Investigators");
    }

    authorMap =
        new HashMap<String, List<String>>();
    {
      authorMap.put("10.1016/j.jidx.2014.07.028", goodAuthors);
      authorMap.put("10.1016/j.jidx2.2014.05.013", goodAuthors);
      authorMap.put("10.1016/S1473-1111(14)70840-0", extendedAuthors);
      authorMap.put("10.1016/S0140-1111(14)61865-1", simpleAuthors);
      authorMap.put("10.1016/j.foo.2014.08.001", goodAuthors);
      authorMap.put("10.1016/j.foo.2014.08.123", goodAuthors);
    };

  
  volMap =
      new HashMap<String, String>();
  {
    volMap.put("10.1016/j.jidx.2014.07.028", "64");
    volMap.put("10.1016/j.jidx2.2014.05.013", "61");
    volMap.put("10.1016/S1473-1111(14)70840-0", "14");
    volMap.put("10.1016/S0140-1111(14)61865-1", "384");
    volMap.put("10.1016/j.foo.2014.08.001", "242");
    volMap.put("10.1016/j.foo.2014.08.123", "242");
  };
  
  issueMap =
      new HashMap<String, String>();
  {
    issueMap.put("10.1016/j.jidx.2014.07.028", "C");
    issueMap.put("10.1016/j.jidx2.2014.05.013", "9");
    issueMap.put("10.1016/S1473-1111(14)70840-0", "10");
    issueMap.put("10.1016/S0140-1111(14)61865-1", "S1");
    issueMap.put("10.1016/j.foo.2014.08.001", "C");
    issueMap.put("10.1016/j.foo.2014.08.123", "C");
  };

  }
  
  private void setUpExpectedBookTarContent() {
    /* maps the DOIs in the metadata to the expected values */
    log.debug3("setUpExpectedBookTarContent");
    // we only need a doi to isbn map
    isbnMap =
        new HashMap<String, String>();
    {
      isbnMap.put("10.1016/B0-12-222222-2/00352-5","978-0-12-222222-2");
      isbnMap.put("10.1016/B978-0-12-222222-2.09000-5","978-0-12-222222-2");
      isbnMap.put("10.1016/B0-12-222222-2/00115-0","978-0-12-222222-2");
      isbnMap.put("10.1016/B0-12-222222-2/00346-X","978-0-12-222222-2");
      isbnMap.put("10.1016/B0-12-222222-2/00064-8","978-0-12-222222-2");
      isbnMap.put("10.1016/B978-0-12-488888-8.00001-5","978-0-12-488888-8");
      isbnMap.put("10.1016/B978-0-12-488888-8.09995-5","978-0-12-488888-8");
      isbnMap.put("10.1016/B978-0-12-488888-8.09998-0","978-0-12-488888-8");
      isbnMap.put("10.1016/B978-0-12-488888-8.09997-9","978-0-12-488888-8");
      isbnMap.put("10.1016/B978-0-444-55555-5.00001-3","978-0-444-55555-5");
      isbnMap.put("10.1016/B978-0-444-55555-5.00006-2","978-0-444-55555-5");
      isbnMap.put("10.1016/B978-0-444-55555-5.00007-4","978-0-444-55555-5");
      isbnMap.put("10.1016/B978-0-444-55555-5.05001-5","978-0-444-55555-5");
      isbnMap.put("10.1533/9780857000000.1.107","978-0-85700-000-0");
      isbnMap.put("10.1016/B978-0-85700-000-0.50027-3","978-0-85700-000-0");
      isbnMap.put("10.1533/9781845692483.2.103","978-1-84566-666-6");    
      isbnMap.put("10.1533/9781845692483.2.130","978-1-84566-666-6");    
      isbnMap.put("10.1533/9781845692483.2.176","978-1-84566-666-6");    
      isbnMap.put("10.1533/9781845692483.2.203","978-1-84566-666-6");    
    };


  }


  private String common_issn = "1111-1111";
  private String common_article_title = "Article about Important Things";
  //private String common_simple_article_title = "Simple Article Title for Update";
  private String common_simple_article_title = "Newsdesk Simple Dochead";
  
  /*
   * When testing a complete extraction out of the tarset, the MD record will be completely filled in
   * and pdf-existence will get established
   */
  private void validateCompleteJournalMetadataRecord(ArticleMetadata am) {
    log.debug3("valideCompleteMetadatRecord");
    String doi_val = am.get(MetadataField.FIELD_DOI);
    /* make sure we can pick up both types of xml article data */ 
    log.debug3("doi val is: " + doi_val);

    if ("JA 5.2.0 SIMPLE-ARTICLE".equals(am.getRaw(ElsevierJournalsDatasetXmlSchemaHelper.dataset_dtd_metadata))) {
      log.debug3("simple-article");
      assertEquals(common_simple_article_title, am.get(MetadataField.FIELD_ARTICLE_TITLE));
    } else { 
      assertEquals(common_article_title, am.get(MetadataField.FIELD_ARTICLE_TITLE));
    }
    assertEquals(common_issn, am.get(MetadataField.FIELD_ISSN));
    assertEquals(authorMap.get(doi_val), am.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(dateMap.get(doi_val), am.get(MetadataField.FIELD_DATE));
    assertEquals(accessUrlMap.get(doi_val), am.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(volMap.get(doi_val), am.get(MetadataField.FIELD_VOLUME));
    assertEquals(issueMap.get(doi_val), am.get(MetadataField.FIELD_ISSUE));
    assertEquals(pubTitleMap.get(doi_val), am.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals("Elsevier", am.get(MetadataField.FIELD_PROVIDER));
    assertEquals("Elsevier", am.get(MetadataField.FIELD_PUBLISHER));
    log.debug3(am.ppString(2));
  }
  
  /*
   * When testing a complete extraction out of the tarset, the MD record will be completely filled in
   * and pdf-existence will get established
   */
  
  ArrayList<String> chapterAuthorList = new ArrayList<String>();
  {
     chapterAuthorList.add("Gordon, S.");
  }

  private void validateCompleteBookMetadataRecord(ArticleMetadata am) {
    log.debug3("validateCompleteBookMetadatRecord");
    String doi_val = am.get(MetadataField.FIELD_DOI);
    /* make sure we can pick up both types of xml article data */ 
    log.debug3("doi val is: " + doi_val);

    String isbn = isbnMap.get(doi_val);
    assertEquals(isbn, am.get(MetadataField.FIELD_ISBN));
    switch (isbn) {
      case "978-0-12-222222-2" :
        assertEquals("Encyclopedia Chapter Item", am.get(MetadataField.FIELD_ARTICLE_TITLE));
        assertEquals("Encyclopedia of Important Stuff, Three-Volume Set", am.get(MetadataField.FIELD_PUBLICATION_TITLE));
        //assertEquals("2004-10-21", am.get(MetadataField.FIELD_DATE));
        break;
      case "978-0-12-488888-8" :
        assertEquals("Doing Data Analysis", am.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Doing Data Analysis Chapter", am.get(MetadataField.FIELD_ARTICLE_TITLE));
        //assertEquals("2014-11-13", am.get(MetadataField.FIELD_DATE));
        break;
      case "978-0-444-55555-5" :
        assertEquals("Book About Chemistry", am.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Chemistry Book Chapter", am.get(MetadataField.FIELD_ARTICLE_TITLE));
        //assertEquals("2013-01-03", am.get(MetadataField.FIELD_DATE));
        break;
      case "978-0-85700-000-0" :
        assertEquals("Handbook", am.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Handbook Chapter", am.get(MetadataField.FIELD_ARTICLE_TITLE));
        //assertEquals("2014-01-28", am.get(MetadataField.FIELD_DATE));
        break;
      case "978-1-84566-666-6" :
        assertEquals("Cotton", am.get(MetadataField.FIELD_PUBLICATION_TITLE));
        assertEquals("Cotton Chapter", am.get(MetadataField.FIELD_ARTICLE_TITLE));
        //assertEquals("2014-01-31", am.get(MetadataField.FIELD_DATE));
        assertEquals(chapterAuthorList, am.getList(MetadataField.FIELD_AUTHOR));
        break;
      default:
         log.debug3("we didn't recognize the ISBN from this doi");
         assertTrue(false);
    }
    assertEquals("Elsevier", am.get(MetadataField.FIELD_PROVIDER));
    assertEquals("Elsevier", am.get(MetadataField.FIELD_PUBLISHER));
    log.debug3(am.ppString(2));
  }
  /*
   * When testing no-pdf-check basic XML parsing, you will get partial MD records
   * depending on whether the info comes from dataset.xml or from main.xml
   */
  private void validateDatasetMetadataRecord(ArticleMetadata am) {
    log.debug3("valideDatasetMetadatRecord");
    String doi_val = am.get(MetadataField.FIELD_DOI);
    assertEquals(common_issn, am.get(MetadataField.FIELD_ISSN));

    log.debug3("doi val is: " + doi_val);
    //The dataset no longer cooks the date - gets it from main.xml
    //assertEquals(null, am.get(MetadataField.FIELD_DATE));
    assertEquals(pubTitleMap.get(doi_val), am.get(MetadataField.FIELD_PUBLICATION_TITLE));
  }

  /*
   * You will have to tell it the DOI and the schema because those normally come from dataset
   */
  private void validateSingleMainMetadataRecord(ArticleMetadata am, String doi_val, String schema) {
    log.debug3("valideSingleMainMetadatRecord");    
    if ("simple-article".equals(schema)) {
      assertEquals(common_simple_article_title, am.get(MetadataField.FIELD_ARTICLE_TITLE));
    } else { 
      assertEquals(common_article_title, am.get(MetadataField.FIELD_ARTICLE_TITLE));
    }

    log.debug3("doi val is: " + doi_val);
    assertEquals(authorMap.get(doi_val), am.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(volMap.get(doi_val), am.get(MetadataField.FIELD_VOLUME));
    assertEquals(issueMap.get(doi_val), am.get(MetadataField.FIELD_ISSUE));
    assertEquals("Comment", am.getRaw(ElsevierJournalsMainDTD5XmlSchemaHelper.common_dochead));
    assertEquals(doi_val, am.getRaw(ElsevierJournalsMainDTD5XmlSchemaHelper.common_doi));
    assertEquals("2014", am.getRaw(ElsevierJournalsMainDTD5XmlSchemaHelper.common_copyright));
  }  
  
  private static final String simpleMain = 
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
          "<!DOCTYPE article PUBLIC \"-//ES//DTD journal article DTD version 5.2.0//EN//XML\" \"art520.dtd\">" +
          "<article docsubtype=\"fla\" xml:lang=\"en\">" +
          "<item-info><jid>TEST</jid>" +
          "<aid>9906</aid>" +
          "<ce:article-number>e09906</ce:article-number>" +
          "<ce:pii>S9999-9994(15)00010-0</ce:pii>" +
          "<ce:doi>10.1016/j.jidx.2014.07.028</ce:doi>" +
          "<ce:copyright type=\"full-transfer\" year=\"2014\">Elsevier GmbH</ce:copyright>" +
          "</item-info>" +
          "<head>" +
          "<ce:dochead id=\"cedoch10\"><ce:textfn>Comment</ce:textfn></ce:dochead>" +
          "<ce:title id=\"tm005\">Article about Important Things</ce:title>" +
          "<ce:author-group id=\"ag005\">" +
          "<ce:author id=\"au005\">" +
          "<ce:given-name>Bob</ce:given-name><ce:surname>Writer</ce:surname>" +
          "<ce:cross-ref id=\"ar005\" refid=\"af005\"><ce:sup>a</ce:sup></ce:cross-ref>" +
          "<ce:cross-ref id=\"ar010\" refid=\"cor1\"><ce:sup>⁎</ce:sup></ce:cross-ref>" +
          "<ce:e-address id=\"em005\" type=\"email\">bobwriter@gmail.com</ce:e-address>" +
          "</ce:author>" +
          "<ce:author id=\"au001\">" +
          "<ce:given-name>Samantha</ce:given-name><ce:surname>Q. Text</ce:surname>" +
          "<ce:cross-ref id=\"ar001\" refid=\"af001\"><ce:sup>a</ce:sup></ce:cross-ref>" +
          "<ce:cross-ref id=\"ar010\" refid=\"cor1\"><ce:sup>⁎</ce:sup></ce:cross-ref>" +
          "<ce:e-address id=\"em005\" type=\"email\">samqt@gmail.com</ce:e-address>" +
          "</ce:author>" +
          "</ce:author-group>" +
          "<ce:date-received day=\"1\" month=\"1\" year=\"2014\"/>" +
          "<ce:date-revised day=\"26\" month=\"7\" year=\"2014\"/>" +
          "<ce:date-accepted day=\"3\" month=\"8\" year=\"2014\"/>" +
          "<ce:abstract class=\"author\" xml:lang=\"en\" id=\"ab005\"><ce:section-title id=\"st050\">Abstract</ce:section-title>" +
          "<ce:abstract-sec id=\"as005\"><ce:simple-para id=\"sp005\">Abstract goes here.</ce:simple-para></ce:abstract-sec>" +
          "</ce:abstract>" +
          "</head>" +
          "<body>" +
          "</body>" +
          "<tail>" +
          "</tail>" +
          "</article>";
  
  static class MyListEmitter implements ArticleMetadataExtractor.Emitter {
    
    List<ArticleMetadata> amlst = new ArrayList<ArticleMetadata>();

    public void emitMetadata(ArticleFiles af, ArticleMetadata md) {
      if (log.isDebug3()) log.debug3("emit("+af+", "+md+")");
      if (md != null) {
        log.debug3("add " + md + " to amlist");
        amlst.add(md);
      };
    }

    public List<ArticleMetadata> getAmList() {
      return amlst;
    }
    
  }
  
  
  /*
   *  A test version of the extractor that allows for suppression of the file check
   *  this allows for basic XML parsing tests without having to provide the actual file content
   */
  public class TestElsevierDTD5MetadataExtractor extends ElsevierDTD5XmlSourceMetadataExtractorFactory.ElsevierDTD5XmlSourceMetadataExtractor {
    // 
    // Override implementation of getFilenamesAssociatedWithRecord to force
    // emit for testing purposes - while allowing use of Elsevier extractor.
    // If a null list is returned, preEmitCheck returns "true"
    // allowing emit.
    //
    protected ArrayList<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      return null;
    }

  }
}
