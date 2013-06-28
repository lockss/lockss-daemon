/* $Id: TestAIPJatsSourceMetadataExtractorFactory.java,v 1.1 2013-06-28 03:01:03 ldoan Exp $

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

package org.lockss.plugin.americaninstituteofphysics;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/*
 * Test file used to extract metadata:
 * <base_url>/JAP/v111/i11/112601_1/Markup/VOR_10.1063_1.4726155.xml
 * Test cases extracting metadata from:
 *      1. the original xml file from the publisher
 *      2. xml file with missing journal title
 *      3. xml file with missing journal title, journal id and issn
 *      4. xml file with missing journal title, journal id, issn and eissn
 */

public class TestAIPJatsSourceMetadataExtractorFactory
  extends LockssTestCase {
  
  static Logger log = 
      Logger.getLogger("TestAIPJatsSourceMetadataExtractorFactory");

  private MockLockssDaemon theDaemon;
  private ArchivalUnit au; // aipjats source au

  private static final String PLUGIN_NAME =
      "org.lockss.plugin.americaninstituteofphysics.ClockssAIPJatsSourcePlugin";

  private static final String BASE_URL = 
      "http://clockss-ingest.lockss.org/sourcefiles/aipjats-released/";
  
  private static final String YEAR = "2013";
  
  private static final String GOOD_CONTENT_FILE_NAME = "test_jats.xml";
  
  // missing journal title
  private static final String MISSING_JOURNAL_META_CONTENT_FILE_NAME_1 = 
      "test_jats_missing_journal_meta1.xml";
  
  // missing journal title, journal id and issn
  private static final String MISSING_JOURNAL_META_CONTENT_FILE_NAME_2 = 
      "test_jats_missing_journal_meta2.xml";

  // missing journal title, journal id, issn and eissn
  private static final String MISSING_JOURNAL_META_CONTENT_FILE_NAME_3 = 
      "test_jats_missing_journal_meta3.xml";
  
  private static final String TEST_URL = 
      "http://clockss-ingest.lockss.org/sourcefiles/aipjats-released/2013/"
      + "test_76_clockss_aip_2013-06-07_084326.zip!/JAP/v111/i11/112601_1/"
      + "Markup/VOR_10.1063_1.4726155.xml";
  
  // expected metadata
  private static final String GOOD_ARTICLE_TITLE = "Junit Article Title";
  private static final String GOOD_PUB_DATE = "2012-06-01";
  private static final String GOOD_COPYRIGHT_YEAR = "2012";
  private static final String GOOD_DOI = "10.0000/1.0000000";
  private static final String GOOD_ISSN = "1111-1111";
  private static final String GOOD_EISSN = "2222-2222";
  private static final String GOOD_ISSUE = "11";
  private static final String GOOD_JOURNAL_TITLE = "Junit Journal Title";
  private static final String GOOD_JOURNAL_ID = "JUNITJOURNALID";
  private static final String GOOD_VOLUME = "111";
  private static final String HARDWIRED_PUBLISHER = 
      "American Institute of Physics";

  // expected journal title for missing only journal title
  private static final String GEN_JOURNAL_TITLE_WITH_ISSN = 
      "UNKNOWN_TITLE/issn=1111-1111";
  
  // expected journal title for missing journal title, journal id, & issn
  private static final String GEN_JOURNAL_TITLE_WITH_EISSN = 
      "UNKNOWN_TITLE/eissn=2222-2222";
  
  // expected journal title for missing journal title, journal id, issn & eissn
  private static final String GEN_JOURNAL_TITLE_WITH_JOURNAL_ID = 
      "UNKNOWN_TITLE/journalId=JAP";

  private static String goodAuthors[] = { "Smith, O.", "Jones, E.", 
    "Brown, I.", "Williams, A. V.", "Davis, E." };
  
  private static String goodKeywords[] = { "kwd1", "kwd2", "kwd3", 
    "kwd4", "kwd5", "kwd6", "kwd7", "kwd8", "kwd9", "kwd10", "kwd11", 
    "kwd12", "kwd13", "kwd14", "kwd15" }; 
  
  private static final String EMPTY_CONTENT =
      "<article>" +
      "</article>";
  
  private static final String BAD_CONTENT =
    "<HTML><HEAD><TITLE>" + GOOD_ARTICLE_TITLE + "</TITLE></HEAD><BODY>\n"
    + "<meta name=\"foo\"" +  " content=\"bar\">\n"
    + "  <div id=\"issn\">"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + " </div>\n";

  private String goodInputContent = getInputFile(GOOD_CONTENT_FILE_NAME);
  
  private String missingJournalMetaInputContent1 = 
      getInputFile(MISSING_JOURNAL_META_CONTENT_FILE_NAME_1);
  
  private String missingJournalMetaInputContent2 = 
      getInputFile(MISSING_JOURNAL_META_CONTENT_FILE_NAME_2);

  private String missingJournalMetaInputContent3 = 
      getInputFile(MISSING_JOURNAL_META_CONTENT_FILE_NAME_3);
  
  public TestAIPJatsSourceMetadataExtractorFactory() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    au = createAu();
  }

  private ArchivalUnit createAu() 
      throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME,
                                           aipJatsSourceAuConfig());
  }
  
  private Configuration aipJatsSourceAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", YEAR);
    return conf;
  }
  
    // get test input files from current directory
  private String getInputFile(String filename) {
    String jatsStr;
    try {
      InputStream jatsIn = getClass().getResourceAsStream(filename);
      jatsStr = StringUtil.fromInputStream(jatsIn);
    }
    catch (IOException e) {
       throw new RuntimeException(e);
    }
    return (jatsStr);
  }

  public void testExtractFromEmptyContent() throws Exception {
    String url = TEST_URL;
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(EMPTY_CONTENT);
    cu.setContentSize(EMPTY_CONTENT.length());
    
    FileMetadataExtractor me = new AIPJatsSourceMetadataExtractorFactory
                                  .AIPJatsSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_DOI));
  }
    
  public void testExtractFromBadContent() throws Exception {
    String url = TEST_URL;
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(BAD_CONTENT);
    cu.setContentSize(BAD_CONTENT.length());
    
    FileMetadataExtractor me = new AIPJatsSourceMetadataExtractorFactory
                                  .AIPJatsSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_DOI));
  }
   
  // original xml file from the publisher
  public void testExtractFromGoodContent() throws Exception {
    String url = TEST_URL;
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(goodInputContent);
    cu.setContentSize(goodInputContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    
    FileMetadataExtractor me = new AIPJatsSourceMetadataExtractorFactory
                                   .AIPJatsSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(GOOD_ISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(GOOD_EISSN, md.get(MetadataField.FIELD_EISSN));
    assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(GOOD_PUB_DATE, md.get(MetadataField.FIELD_DATE));
    assertEquals(GOOD_COPYRIGHT_YEAR, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(GOOD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(GOOD_JOURNAL_TITLE, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(GOOD_JOURNAL_ID,
        md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertSameElements(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
  } 
  
  // xml file missing journal title
  public void testExtractFromMissingJournalMetaContent1() throws Exception {
    String url = TEST_URL;
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(missingJournalMetaInputContent1);
    cu.setContentSize(missingJournalMetaInputContent1.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");

    FileMetadataExtractor me = new AIPJatsSourceMetadataExtractorFactory
                                   .AIPJatsSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(GOOD_ISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(GOOD_EISSN, md.get(MetadataField.FIELD_EISSN));
    assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(GOOD_PUB_DATE, md.get(MetadataField.FIELD_DATE));
    assertEquals(GOOD_COPYRIGHT_YEAR, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(GOOD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(GOOD_JOURNAL_ID,
        md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(GEN_JOURNAL_TITLE_WITH_ISSN,
        md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertSameElements(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
  }

  // xml file missing journal title, journal id and issn
  public void testExtractFromMissingJournalMetaContent2() throws Exception {
    String url = TEST_URL;
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(missingJournalMetaInputContent2);
    cu.setContentSize(missingJournalMetaInputContent2.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");

    FileMetadataExtractor me = new AIPJatsSourceMetadataExtractorFactory
                                   .AIPJatsSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(GOOD_EISSN, md.get(MetadataField.FIELD_EISSN));
    assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(GOOD_PUB_DATE, md.get(MetadataField.FIELD_DATE));
    assertEquals(GOOD_COPYRIGHT_YEAR, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(GOOD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(GEN_JOURNAL_TITLE_WITH_EISSN,
        md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertSameElements(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
  }

  // xml file missing journal title, journal id, issn and eissn
  public void testExtractFromMissingJournalMetaContent3() throws Exception {
    String url = TEST_URL;
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(missingJournalMetaInputContent3);
    cu.setContentSize(missingJournalMetaInputContent3.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");

    FileMetadataExtractor me = new AIPJatsSourceMetadataExtractorFactory
                                   .AIPJatsSourceMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(GOOD_DOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(GOOD_ISSUE, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(GOOD_VOLUME, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(GOOD_PUB_DATE, md.get(MetadataField.FIELD_DATE));
    assertEquals(GOOD_COPYRIGHT_YEAR, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(HARDWIRED_PUBLISHER, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(GOOD_ARTICLE_TITLE, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(GEN_JOURNAL_TITLE_WITH_JOURNAL_ID,
        md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertSameElements(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
  }
  
}