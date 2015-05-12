/*
 * $Id$
 */
/*

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.jstor;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;


public class TestJstorMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestJstorMetadataExtractor.class);

  private MockArchivalUnit mau;
  private static String BASE_URL = "http://www.jstor.org/";
  //this is funky but true
  private static String BASE_URL2 = "https://www.jstor.org/";
  private CIProperties pdfHeader = new CIProperties();
  private CIProperties textHeader = new CIProperties();

  public void setUp() throws Exception {
    super.setUp();

    mau = new MockArchivalUnit();
    mau.setConfiguration(auConfig());

    // set up headers for creating mock CU's of the appropriate type
    pdfHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "application/pdf");
    textHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/plain");

  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Configuration method.
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("base_url2", BASE_URL2);
    conf.put("journal_id", "xxxx");
    conf.put("volume_name","123");
    return conf;
  }

  // the metadata that should be extracted
  String goodDate = "2010/11/01";
  String[] goodAuthors = new String[] {"D. Author", "S. Author2"};
  String goodFormat = "text/HTML";
  String goodTitle = "Title of Article";
  String goodType = "research-article";
  String goodPublisher = "University of California Press on behalf of the National Association of Biology Teachers";
  static String goodDOI = "10.1137/10081839X";

  String goodJournal = "Journal Name";
  String goodStartPage = "532";
  String goodVolume = "72";
  String goodIssue = "9";
  String goodIssn = "00027685";
  String goodURL = "http://www.jstor.org/stable/" + goodDOI;

  private static String RIS_URL = BASE_URL2 + "action/downloadSingleCitationSec" +
      "?format=refman&doi=" + goodDOI;
  private static String PDFPLUS_URL = BASE_URL + "stable/pdfplus/" + goodDOI + ".pdf";

  /*
   *  A ris file example
Provider: JSTOR http://www.jstor.org
Database: JSTOR
Content: text/plain



TY  - JOUR
JO  - The American Biology Teacher
TI  - From the President
VL  - 72
IS  - 9
PB  - University of California Press on behalf of the National Association of Biology Teachers
SN  - 00027685
UR  - http://www.jstor.org/stable/10.1525/abt.2010.72.9.1
AU  - Jaskot, Bunny
DO  - 10.1525/abt.2010.72.9.1
T3  -
Y1  - 2010/11/01
SP  - 532
CR  - Copyright &#169; 2010 National Association of Biology Teachers
M1  - ArticleType: research-article / Full publication date: November/December 2010 / Copyright &#169; 2010 National Association of Biology Teachers
ER  -

   */


  private String createGoodRisContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("Provider: JSTOR http://www.jstor.org\n");
    sb.append("Database: JSTOR\n");
    sb.append("Content: text/plain\n\n\n");

    sb.append("TY  - JOUR");
    sb.append("\nJO  - ");
    sb.append(goodJournal);
    sb.append("\nT1  - ");
    sb.append(goodTitle);
    sb.append("\nVL  - ");
    sb.append(goodVolume);
    sb.append("\nIS  - ");
    sb.append(goodIssue);
    sb.append("\nPB  - ");
    sb.append(goodPublisher);
    sb.append("\nSN  - ");
    sb.append(goodIssn);
    sb.append("\nUR  - ");
    sb.append(goodURL);
    for(String auth : goodAuthors) {
      sb.append("\nAU  - ");
      sb.append(auth);
    }
    sb.append("\nDO  - ");
    sb.append(goodDOI);
    sb.append("\nT3  - ");
        //nothing follows
    sb.append("\nY1  - ");
    sb.append(goodDate);
    sb.append("\nSP  - ");
    sb.append(goodStartPage);

    sb.append("\nCR  - ");
    sb.append("Copyright &#169; 2010 National Association of Biology Teachers");
    sb.append("\nM1  - ");
    sb.append("ArticleType: research-article / Full publication date: November/December 2010 / Copyright &#169; 2010 National Association of Biology Teachers");

    sb.append("\nER  -");
    return sb.toString();
  }
  /**
   * Method that creates a simulated Cached URL from the source code provided by
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the MetaPressRisMetadataExtractorFactory, match the metadata in the source code.
   * @throws Exception
   */
  public void testExtractGoodRisContent() throws Exception {
    String goodContent = createGoodRisContent();
    log.debug3(goodContent);

    MockCachedUrl mcu = mau.addUrl(RIS_URL, true, true, textHeader);
    mcu.setContent(goodContent);
    mcu.setContentSize(goodContent.length());
    // add a content file because the emitter checks for one
    mau.addUrl(PDFPLUS_URL, true, true, pdfHeader);

    FileMetadataExtractor me = new JstorRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/plain");
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

/*
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    Iterator<String> actAuthIter = md.getList(MetadataField.FIELD_AUTHOR).iterator();
    for(String expAuth : goodAuthors) {
      assertEquals(expAuth, actAuthIter.next());
    }
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));

    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodURL, md.get(MetadataField.FIELD_ACCESS_URL));
    */

  }

  /* the extractor checks if data is missing it uses possible alternate RIS tags */
  private String createEmptyRisContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("Provider: JSTOR http://www.jstor.org\n");
    sb.append("Database: JSTOR\n");
    sb.append("Content: text/plain\n\n\n");

    sb.append("TY  - JOUR");
    sb.append("\nJO  - ");
    sb.append("\nT1  - ");
    sb.append("\nVL  - ");
    sb.append("\nIS  - ");
    sb.append("\nPB  - ");
    sb.append("\nSN  - ");
    sb.append("\nUR  - ");
    sb.append("\nDO  - ");
    sb.append("\nT3  - ");
    sb.append("\nY1  - ");
    sb.append("\nSP  - ");
    sb.append("\nM1  - ");
    sb.append("ArticleType: / Full publication date: /");
    sb.append("\nER  -");
    return sb.toString();
  }
  /**
   * Method that creates a simulated Cached URL from the source code provided by
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the MetaPressRisMetadataExtractorFactory, match the metadata in the source code.
   * @throws Exception
   */


  /*
   * Since the plugin creates the URL used for citation (metadata) information,
   * in the event that we come up with a "bad" url it returns just an empty RIS
   * file. In this case, we don't want to emit.

Provider: JSTOR http://www.jstor.org
Database: JSTOR
Content: text/plain



TY  - JOUR
JO  -
TI  -
VL  -
IS  -
PB  -
SN  -
UR  -
DO  -
T3  -
Y1  -
SP  -
M1  - ArticleType:  / Full publication date:  /
ER  -
   *
   */
  public void testExtractEmptyContent() throws Exception {
    String emptyContent = createEmptyRisContent();
    log.debug3(emptyContent);
    MockCachedUrl mcu = mau.addUrl(RIS_URL, true, true, textHeader);
    mcu.setContent(emptyContent);
    mcu.setContentSize(emptyContent.length());
    // no need to add a full content URL, there isn't any content

    FileMetadataExtractor me = new JstorRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/plain");
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertEmpty(mdlist);

  }

}
