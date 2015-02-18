/* $Id$

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.wolterskluwer;

import java.util.*;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.clockss.SourceXmlMetadataExtractorTest;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;

public class TestWoltersKluwerSourceXmlMetadataExtractorHelper
extends SourceXmlMetadataExtractorTest {

  static Logger log = 
      Logger.getLogger(TestWoltersKluwerSourceXmlMetadataExtractorHelper.class);

  static FileMetadataListExtractor wk_mle;
  static FileMetadataListExtractor nocheck_mle;


  public void setUp() throws Exception {
    super.setUp();
    // for tests that also check for content
    wk_mle =
        new FileMetadataListExtractor(new WoltersKluwerSourceXmlMetadataExtractorFactory.WoltersKluwerSourceXmlMetadataExtractor());
    // for tests that use a no-check-for-pdf version of the extractor
    nocheck_mle = new FileMetadataListExtractor(new TestWKSourceXmlMetadataExtractor());


  }

  private static final String BASIC_XML_FILENAME = "20140615.0";
  private static final String ACCESS_URL = "0125480-201406150-00002";
  private static final String BASIC_PDF_FILENAME = ACCESS_URL+".pdf";

  private static final String TEST_XML_URL = "ADAPA20140615.0/" + BASIC_XML_FILENAME;
  // need to add a "0" to precede the BASIC_PDF_FILENAME... GRRR
  private static final String TEST_PDF_URL = "ADAPA20140615.0/" + "0"+BASIC_PDF_FILENAME;
  private static final String wk_mime = "application/sgml";

  // expected metadata
  private static final String GOOD_JOURNAL_TITLE = "Hello Oligarchy: With Songs & Dances";
  private static final String GOOD_PUB_YEAR = "2014";
  private static final String GOOD_PUB_MONTH = "June";
  private static final String GOOD_PUB_DAY = "15";
  private static final String GOOD_PUB_DATE = "2014-06";
  //private static final String GOOD_PUB_DATE = "2014-06-15";
  private static final String GOOD_DOI = "10.1097/01.CNE.0000428768.49988.91";
  private static final String GOOD_ISSN = "0163-2108";
  private static final String GOOD_ISSUE = "8";
  private static final String GOOD_VOLUME = "24";
  private static final String GOOD_ARTICLE_TITLE = "Special Topic";
  private static final String GOOD_ARTICLE_SUBTITLE = "An Intro to Plutocrats";
  private static final String GOOD_TITLE = GOOD_ARTICLE_TITLE + ":" + GOOD_ARTICLE_SUBTITLE;
  private static final String A1_FN = "Anna";
  private static final String A1_MN = "B.";
  private static final String A1_SN = "Cross";
  private static final String A2_FN = "Doug";
  private static final String A2_SN = "Evans";

  private static ArrayList goodAuthors = (ArrayList) ListUtil.list(
      A1_SN + ", " + A1_FN + " " + A1_MN,
      A2_SN + ", " + A2_FN);

  private static final String BASIC_CONTENT =
      "<!DOCTYPE dg SYSTEM \"ovidbase.dtd\">" +
          "<DG><COVER NAME=\"G0256406-201406150-00000\">" +
          "<D V=\"2009.2F\" AN=\""+ACCESS_URL+"\" FILE=\"G0256406-201406150-00001\" CME=\"CME\">" +
          "<BB>" +
          "<TG>" +
          "<TI>" + GOOD_ARTICLE_TITLE + "</TI>" +
          "<STI>" + GOOD_ARTICLE_SUBTITLE + "</STI></TG>" +
          "<BY>" +
          "<PN><FN>"+A1_FN+"</FN><MN>"+A1_MN+"</MN><SN>"+A1_SN+"</SN><DEG>MD</DEG></PN>" +
          "<PN><FN>"+A2_FN+"</FN><SN>"+A2_SN+"</SN><DEG>MD, MSc</DEG></PN>" +
          "</BY>" +
          "<SO>" +
          "<PB>"+GOOD_JOURNAL_TITLE+"</PB>" +
          "<ISN>"+GOOD_ISSN+"</ISN>" +
          "<DA>"+"<MO>"+GOOD_PUB_MONTH+"</MO><YR>"+GOOD_PUB_YEAR+"</YR></DA>" +
          //    "<DA><DY>"+GOOD_PUB_DAY+"</DY><MO>"+GOOD_PUB_MONTH+"</MO><YR>"+GOOD_PUB_YEAR+"</YR></DA>" +
          "<V>"+GOOD_VOLUME+"</V>" +
          "<IS><IP>"+GOOD_ISSUE+"</IP></IS>" +
          "<PG>1&ndash;6</PG></SO>" +
          "<CP>&copy; 2014 Lippincott Williams &amp; Wilkins.</CP>" +
          "<DT>Article</DT><XUI XDB=\"pub-doi\" UI=\""+GOOD_DOI+"\"></BB>" +
          "</D></DG>";

  private static final String EMPTY_CONTENT =
      "<!DOCTYPE dg SYSTEM \"ovidbase.dtd\">"+
          "<DG>"+
          "</DG>";
  // Try some disallowed content: 
  // Bad date: date without a year
  private static final String BAD_PUB_DAY = "77";
  private static final String BAD_PUB_MONTH = "Juneteenth";
  private static final String BAD_PUB_YEAR = "";
  // Bad authors name combos:
  private static final String BAD1_FN = "Firstname";
  private static final String BAD1_MN = "M";
  private static final String BAD1_SN = "LastName";
  private static final String BAD2_FN = "Cher";
  private static final String BAD2_SN = "";
  // bad DOI (whichshould be corrected)
  private static final String BAD_DOI = "  DOI:  "+GOOD_DOI;
  //private static final String BAD_DOI = "  ";
  // Bad Title: only subtitle
  private static final String BAD_ARTICLE_SUBTITLE = "Just a Subtitle";
  private static final String BAD_ARTICLE_TITLE = ":" +BAD_ARTICLE_SUBTITLE;
  private static ArrayList badAuthors = (ArrayList) ListUtil.list(
      BAD1_SN + ", " + BAD1_FN + " " + BAD1_MN,
      ", " + BAD2_FN);

  private static final String BAD_CONTENT =
      "<!DOCTYPE dg SYSTEM \"ovidbase.dtd\">" +
          "<DG><COVER NAME=\"G0256406-201406150-00000\">" +
          "<D V=\"2009.2F\" AN=\""+ACCESS_URL+"\" FILE=\"G0256406-201406150-00001\" CME=\"CME\">" +
          "<BB>" +
          "<TG>" +
          "<TI></TI>" +
          "<STI>" + BAD_ARTICLE_SUBTITLE + "</STI></TG>" +
          "<BY>" +
          "<PN><FN>"+BAD1_FN+"</FN><MN>"+BAD1_MN+"</MN><SN>"+BAD1_SN+"</SN><DEG>MD</DEG></PN>" +
          "<PN><FN>"+BAD2_FN+"</FN><SN>"+BAD2_SN+"</SN><DEG>MD, MSc</DEG></PN>" +
          "</BY>" +
          "<SO>" +
          "<PB>"+GOOD_JOURNAL_TITLE+"</PB>" +
          "<ISN>"+GOOD_ISSN+"</ISN>" +
          "<DA><DY>"+BAD_PUB_DAY+"</DY><MO>"+BAD_PUB_MONTH+"</MO><YR>"+BAD_PUB_YEAR+"</YR></DA>" +
          "<V>"+GOOD_VOLUME+"</V>" +
          "<IS><IP>"+GOOD_ISSUE+"</IP></IS>" +
          "<PG>1&ndash;6</PG></SO>" +
          "<CP>&copy; 2014 Lippincott Williams &amp; Wilkins.</CP>" +
          "<DT>Article</DT><XUI XDB=\"pub-doi\" UI=\""+BAD_DOI+"\"></BB>" +
          "</D>" +
          // 
          "<D V=\"2009.2F\" AN=\""+ACCESS_URL+"\" FILE=\"G0256406-201406150-00002\" CME=\"CME\">" +
          "<BB>" +
          "<TG>" +
          "<TI></TI>" +
          "<STI>" + BAD_ARTICLE_SUBTITLE + "</STI></TG>" +
          "<BY>" +
          "<PN><FN>"+BAD1_FN+"</FN><MN>"+BAD1_MN+"</MN><SN>"+BAD1_SN+"</SN><DEG>MD</DEG></PN>" +
          "<PN><FN>"+BAD2_FN+"</FN><SN>"+BAD2_SN+"</SN><DEG>MD, MSc</DEG></PN>" +
          "</BY>" +
          "<SO>" +
          "<PB>"+GOOD_JOURNAL_TITLE+"</PB>" +
          "<ISN>"+GOOD_ISSN+"</ISN>" +
          "<DA><DY>"+BAD_PUB_DAY+"</DY><MO>"+BAD_PUB_MONTH+"</MO><YR>"+BAD_PUB_YEAR+"</YR></DA>" +
          "<V>"+GOOD_VOLUME+"</V>" +
          "<IS><IP>"+GOOD_ISSUE+"</IP></IS>" +
          "<PG>1&ndash;6</PG></SO>" +
          "<CP>&copy; 2014 Lippincott Williams &amp; Wilkins.</CP>" +
          "<DT>Article</DT><XUI XDB=\"pub-doi\" UI=\"" + BAD_DOI + "\"></BB>" +
          "</D>" +
          "</DG>";


  public void testExtractFromEmptyContent() throws Exception {
    String xml_url = getBaseUrl() + getYear() + "/" + TEST_XML_URL;
    String pdf_url = getBaseUrl() + getYear() + "/" + TEST_PDF_URL;

    ArrayList<String> pdfList = new ArrayList<String>();
    pdfList.add(pdf_url);

    //FileMetadataExtractor me = new WoltersKluwerSourceXmlMetadataExtractorFactory
    //.WoltersKluwerSourceXmlMetadataExtractor();
    //FileMetadataListExtractor mle =
    //  new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdList = extractFromContent(xml_url, wk_mime, 
        EMPTY_CONTENT, wk_mle, pdfList);
    assertEmpty(mdList);

  }

  static private final Map<MetadataField, String> expectedBadMD =
      new HashMap<MetadataField,String>();
  static {
    expectedBadMD.put(MetadataField.FIELD_DOI, GOOD_DOI);
    expectedBadMD.put(MetadataField.FIELD_ARTICLE_TITLE, BAD_ARTICLE_TITLE);
    expectedBadMD.put(MetadataField.FIELD_DATE, null);
    expectedBadMD.put(MetadataField.FIELD_AUTHOR, badAuthors.toString());
  }

  public void testExtractFromBadContent() throws Exception {
    String xml_url = getBaseUrl() + getYear() + "/" + TEST_XML_URL;
    String pdf_url = getBaseUrl() + getYear() + "/" + TEST_PDF_URL;

    ArrayList<String> pdfList = new ArrayList<String>();
    pdfList.add(pdf_url);

    //FileMetadataExtractor me = new WoltersKluwerSourceXmlMetadataExtractorFactory
    //.WoltersKluwerSourceXmlMetadataExtractor();
    //FileMetadataListExtractor mle =
    //new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdList = extractFromContent(xml_url, wk_mime, 
        BAD_CONTENT, wk_mle, pdfList);
    log.setLevel("debug3");
    debug3_MDList(mdList);
    for (int i = 0; i< mdList.size(); i++) {
      ArticleMetadata md = mdList.get(i);
      checkOneMD(md, expectedBadMD);
    }
  }

  // original xml file from the publisher

  static private final Map<MetadataField, String> expectedBasicMD =
      new HashMap<MetadataField,String>();
  static {
    expectedBasicMD.put(MetadataField.FIELD_ISSN, GOOD_ISSN);
    expectedBasicMD.put(MetadataField.FIELD_ACCESS_URL, TEST_PDF_URL);
    expectedBasicMD.put(MetadataField.FIELD_PUBLICATION_TITLE, GOOD_JOURNAL_TITLE);
    expectedBasicMD.put(MetadataField.FIELD_DOI, GOOD_DOI);
    expectedBasicMD.put(MetadataField.FIELD_DATE, GOOD_PUB_DATE);
    expectedBasicMD.put(MetadataField.FIELD_VOLUME, GOOD_VOLUME);
    expectedBasicMD.put(MetadataField.FIELD_ISSUE, GOOD_ISSUE);
    expectedBasicMD.put(MetadataField.FIELD_ARTICLE_TITLE, GOOD_TITLE);
    expectedBasicMD.put(MetadataField.FIELD_AUTHOR, goodAuthors.toString());
  }

  public void testExtractFromBasicContent() throws Exception {
    String xml_url = getBaseUrl() + getYear() + "/" + TEST_XML_URL;
    String pdf_url = getBaseUrl() + getYear() + "/" + TEST_PDF_URL;
    ArrayList<String> pdfList = new ArrayList<String>();
    pdfList.add(pdf_url);

    //FileMetadataExtractor me = new WoltersKluwerSourceXmlMetadataExtractorFactory.WoltersKluwerSourceXmlMetadataExtractor();
    //FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdList = extractFromContent(xml_url, wk_mime,
        BASIC_CONTENT, wk_mle, pdfList);

    // Need to correct the expectedBasicMD to include baseUrl/Year
    expectedBasicMD.put(MetadataField.FIELD_ACCESS_URL, pdf_url);
    for (int i = 0; i< mdList.size(); i++) {
      ArticleMetadata md = mdList.get(i);
      checkOneMD(md, expectedBasicMD);
    }
  }

  public static final String wkLeader = "<!DOCTYPE dg SYSTEM \"ovidbase.dtd\">" + 
      "<DG>";

  public static final String wkArticleTop = 
      "<D AN=\"000\" V=\"000.2\" PDF-ONLY=\"YES\" PUBSTATE=\"POST-AUTHOR-CORRECTIONS\" FILE=\"G000\">" +
          "<BB>" +
          "<TG>" +
          "<TI>Testing and Other Purposes</TI></TG>";

  public static final String wkBY = 
      "<BY>" +
          "<PN><FN>Fred</FN><MN>Q.</MN><SN>Writer</SN><DEG>M.D., Ph.D.</DEG></PN>" +
          "<PN><FN>Suzie</FN><SN>Floobitz</SN><DEG>M.D.</DEG></PN>" +
          "<AF><P>Information about hte authors first part</P></AF>" +
          "<BT><P>Accepted August 21, 2009.</P><P>This work was supported by various grants</P><P>Correspondence to the authors</P></BT>" +
          "</BY>";
  public static final String wkISNDate = 
      "<SO>" +
          "<PB>Journal of the Testing</PB>" +
          "<ISN>0000-1111</ISN>" +
          "<DA><DY>23</DY><MO>October</MO><YR>2009</YR></DA>" +
          "<V>Publish Ahead of Print</V>" +
          "<IS><IP>&NA;</IP></IS>" +
          "<PG>&NA;</PG></SO>";

  public static final String wkXUI = 
      "<DT>RESEARCH ARTICLE</DT><XUI XDB=\"pub-doi\" UI=\"10.1111/TEST.0000\"><XUI XDB=\"pub-sub-type\" UI=\"PAP-NEW\">";

  public static final String wkArtBottom = 
      "</BB>" +
      "<AB><HD>ABSTRACT</HD>" +
          "<ABS><HD>Objective&colon;</HD>" +
          "<P>Despite the known association between testing and good outcomes</P></ABS>" +
          "<ABS><HD>Method&colon;</HD>" +
          "<P>People don't always test well.</P></ABS>" +
          "<ABS><HD>Results&colon;</HD>" +
          " <P>Test more rigorously</P></ABS>" +
          "<ABS><HD>Conclusions&colon;</HD>" +
          "<P>This plugin works.</P>" +
          "<P>Clinical trial of this plugin.  URL&colon; <URL>http&colon;//www.lockss.org</URL>. Unique identifier&colon; TEST000.</P></ABS></AB>" +
          "<KWS><HD>Key Words&colon;</HD><KW>testing</KW><KW>depression</KW><KW>treatment</KW></KWS>" +
          "</D>";

  public static final String wkEnder = "</DG>";


  // Run this test using the test version of WK md extractor which doesn't
  // need content PDF files to emit
  public void testDOIEvaluator() throws Exception {
    String good_doi = "10.1234/floobitz";
    String difficult_doi = "DOI : 10.1234/floobitz";
    String difficult_doi_result = "10.1234/floobitz";
    String impossible_doi = "0.1234/floobitz";
    String xui_doi = 
        "<DT>RESEARCH ARTICLE</DT><XUI XDB=\"pub-doi\" UI=\"" + 
            good_doi + 
            "\"><XUI XDB=\"pub-sub-type\" UI=\"PAP-NEW\">";

    String xmlDoc = 
        wkLeader + 
        wkArticleTop + wkBY + wkISNDate + xui_doi + wkArtBottom + 
        wkEnder;

    //FileMetadataExtractor me = new TestWKSourceXmlMetadataExtractor();
    //assertNotNull(me);
    //    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);

    // Use test version of extractor - no PDFs needed
    List<ArticleMetadata> mdList = extractFromContent(xmlDoc, nocheck_mle);
    checkOneMD(mdList, MetadataField.FIELD_DOI, good_doi);

    // evaluator figures it out
    xui_doi = 
        "<DT>RESEARCH ARTICLE</DT><XUI XDB=\"pub-doi\" UI=\"" + 
            difficult_doi + 
            "\"><XUI XDB=\"pub-sub-type\" UI=\"PAP-NEW\">";
    xmlDoc = 
        wkLeader + 
        wkArticleTop + wkBY + wkISNDate + xui_doi + wkArtBottom + 
        wkEnder;
    mdList = extractFromContent(xmlDoc, nocheck_mle);
    checkOneMD(mdList, MetadataField.FIELD_DOI, difficult_doi_result);

    // we have to let this one go
    xui_doi = 
        "<DT>RESEARCH ARTICLE</DT><XUI XDB=\"pub-doi\" UI=\"" + 
            impossible_doi + 
            "\"><XUI XDB=\"pub-sub-type\" UI=\"PAP-NEW\">";
    xmlDoc = 
        wkLeader + 
        wkArticleTop + wkBY + wkISNDate + xui_doi + wkArtBottom + 
        wkEnder;
    mdList = extractFromContent(xmlDoc, nocheck_mle);
    checkOneMD(mdList, MetadataField.FIELD_DOI, null);
  }

  // Run this test using the test version of WK md extractor which doesn't
  // need content PDF files to emit
  public void testISSNEvaluator() throws Exception {

    String issn_good = "1070-8022";
    String issn_1 = "1070 -8022";
    String issn_2 = "1070- 8022";
    String issn_3 = "1070-8022&sol;11&sol;0000&ndash;0000";
    String issn_4 = "10708022";
    String issn_bad = "J Nurses Staff Dev"; //yup. really happened

    String isn_start = "<SO>" +
        "<PB>Journal of the Testing</PB>" +
        "<ISN>";

    String isn_end = "</ISN>" +
        "<DA><DY>23</DY><MO>October</MO><YR>2009</YR></DA>" +
        "<V>Publish Ahead of Print</V>" +
        "<IS><IP>&NA;</IP></IS>" +
        "<PG>&NA;</PG></SO>";    

    String xmlDoc = 
        wkLeader + 
        wkArticleTop + isn_start + issn_good + isn_end + wkArtBottom + 
        wkArticleTop + isn_start + issn_1 + isn_end + wkArtBottom + 
        wkArticleTop + isn_start + issn_2 + isn_end + wkArtBottom + 
        wkArticleTop + isn_start + issn_3 + isn_end + wkArtBottom + 
        wkArticleTop + isn_start + issn_4 + isn_end + wkArtBottom + 
        wkEnder;

    // Use test version of extractor - no PDFs needed
    List<ArticleMetadata> mdList = extractFromContent(xmlDoc, nocheck_mle);
    Iterator<ArticleMetadata> mdIt = mdList.iterator();
    // each one of these should have been evaluated and become a "good" issn
    while (mdIt.hasNext()) {
      checkOneMD(mdIt.next(), MetadataField.FIELD_ISSN, issn_good);
    }

    xmlDoc = 
        wkLeader + 
        wkArticleTop + isn_start + issn_bad + isn_end + wkArtBottom + 
        wkEnder;
    mdList = extractFromContent(xmlDoc, nocheck_mle);
    checkOneMD(mdList, MetadataField.FIELD_ISSN, null);
  }


  public class TestWKSourceXmlMetadataExtractor extends WoltersKluwerSourceXmlMetadataExtractorFactory.WoltersKluwerSourceXmlMetadataExtractor {
    // 
    // Override implementation of getFilenamesAssociatedWithRecord to force
    // emit for testing purposes - while allowing use of WK extractor.
    // If a null list is returned, preEmitCheck no files to check and returns "true"
    // allowing emit.
    //
    protected ArrayList<String> getFilenamesAssociatedWithRecord(SourceXmlSchemaHelper helper, 
        CachedUrl cu,
        ArticleMetadata oneAM) {
      return null;
    }

  }

}