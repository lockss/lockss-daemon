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

import java.util.List;
import java.util.regex.Pattern;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataListExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;


/*
 *  For now this is a limited test. Test that parsing and retrieving data from 
 *  an XML file works equivalently for ONE article in the 
 *  xml file when
 *      - no global map; article map with one article node
 *      - global map; no article map nor article node
 *  Set up two schema helpers and then use both and verify the results are the same
 *  Still to write - generic XML parser testing to catch edge cases
 */
public class TestTaylorAndFrancisSourceXmlMetadataExtractor
extends LockssTestCase {
  static Logger log = Logger.getLogger(TestTaylorAndFrancisSourceXmlMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.taylorandfrancis.ClockssTaylorAndFrancisSourcePlugin";
  private static String BASE_URL = "http://www.source.org/";
  private static String YEAR = "2013";
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    mau = new MockArchivalUnit();

    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau.setConfiguration(auConfig());

    PluginTestUtil.createAndStartAu(PLUGIN_NAME, auConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", YEAR);
    return conf;
  }
  
  private static String goodPublisher = "Taylor & Francis";

  private static String article_articleID = "9973";
  private static String article_journal = "Annals of Clinical Psychiatry";
  private static String article_title = "Exellent Article Title";
  private static String article_date = "1989";
  private static String articleSchemaContent = 
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
          "<!DOCTYPE article SYSTEM \"http://cats.tfinforma.com/dtd/tfja/dtd/TFJA.dtd\">" +
          "<article id=\"a9973\" documenttype=\"Original\" articleid=\"" +
          article_articleID +
          "\" doi=\"11.1111/9973\" " +
          "tagger=\"TechBooks\" yearofpub=\"" + article_date + "\" coverdate=\"September 1989\" " +
          "copyrighttf=\"yes\" copyrightowner=\"American Academy of Clinical Psychiatrists\" numcolorpages=\"0\">" +
          "<meta productid=\"UACP\" firstpage=\"147\" lastpage=\"152\" pagecount=\"6\" " +
          "volumenum=\"1\" issuenum=\"3\" pdffilename=\"UACP_test_9973_O.pdf\" pdffilesize=\"506696\" pdfpagecount=\"6\" contenttype=\"Review-static/background\" partofspecissue=\"no\" seq=\"2\">" +
          "<journalcode>UACP</journalcode>" +
          "<issn type=\"print\">1111-1234</issn>" +
          "<issn type=\"electronic\">1111-2345</issn>" +
          "<coden>Annals of Clinical Psychiatry, Vol. 1, No. 3, September 1989: pp. 147&ndash;152</coden>" +
          "<author seq=\"1\" corresponding=\"yes\">" +
          "<name>" +
          "<givenname>Author</givenname>" +
          "<inits>Q.</inits>" +
          "<surname>Writer</surname>" +
          "<degree>Ph.D., M.D.</degree>" +
          "</name>" +
          "</author>" +
          "<author seq=\"2\"><name><givenname>am</givenname><surname>Pencil</surname><degree>M.D.</degree></name>" +
          "</author>" +
          "<production-dates printpubdate=\"01Sep1989\" receiveddate=\"24May1989\" reviseddate=\"05Jul1989\"/>" +
          "</meta>" +
          "<journaltitle>" +
          article_journal +
          "</journaltitle>" +
          "<supertitle>Winner: 1989 AACP Clinical Research Award</supertitle>" +
          "<title>" + article_title + "</title>" +
          "<shorttitle/>" +
          "</article>";
  
  private static String unarticle_articleID = "J060";
  private static String unarticle_journal = "Journal of Pharmacy Teaching";
  private static String unarticle_title = "BOOK REVIEWS";
  private static String unarticle_date = "2000";
  private static String unarticleSchemaContent = 
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
          "<!DOCTYPE unarticle SYSTEM \"http://cats.tfinforma.com/dtd/tfja/dtd/TFJA.dtd\">" +
          "<unarticle id=\"a" + unarticle_articleID + "\" documenttype=\"BookReview\" articleid=\"" + unarticle_articleID + "\" " +
          "copyrighttf=\"yes\" doi=\"11.1111.J060\" tagger=\"StanInfo\" yearofpub=\"" + unarticle_date + "\" " +
          "productfree=\"no\" copyrightowner=\"Taylor &amp; Francis\">" +
          "<meta productid=\"WJPT\" volumenum=\"8\" issuenum=\"2\" firstpage=\"89\" " +
          "lastpage=\"96\" pagecount=\"8\" pdffilename=\"WJPT_J060.pdf\" pdffilesize=\"25975\" pdfpagecount=\"8\" seq=\"7\">" +
          "<journalcode>WJPT</journalcode>" +
          "<issn type=\"print\">1044-0054</issn>" +
          "<coden>Journal of Pharmacy Teaching, Vol. 8, No. 2, 2000: pp. 89&ndash;96</coden>" +
          "<production-dates printpubdate=\"18Oct2000\" qaapprovdate=\"14Oct2008\"/>" +
          "</meta>" +
          "<title>" + unarticle_title + "</title>" +
          "<shorttitle/>" +
          "<doititle/>" +
          "<intro/>" +
          "<section1 id=\"s0001\"><title/></section1>" +
          "</unarticle>";

  private static String tfdoc_articleID = "UACP8888";
  private static String tfdoc_journal = "Annals of Clinical Psychiatry";
  private static String tfdoc_title = "A typical Title: Subtitle";
  private static String tfdoc_date = "2004";
  private static String tfdocSchemaContent = 
      "<?xml version=\"1.0\"?>" +
          "<!DOCTYPE tfdoc SYSTEM \"http://public.metapress.com/dtd/tandfr/tandf.dtd\">" +
          "<tfdoc version=\"1.1\" jid=\"uACP\">" +
          "<docmeta>" +
          "<jnl-title>Annals of Clinical Psychiatry</jnl-title>" +
          "<pubinfo>" +
          "<pubname>Taylor &amp; Francis</pubname>" +
          "<address><street>325 Chestnut Street</street><city>Philadelphia</city><state>PA</state><postcode>19106</postcode></address>" +
          "<vol>16</vol>" +
          "<issue>2</issue>" +
          "<issn type=\"print\">1111-1234</issn>" +
          "<issn type=\"online\">1111-2345</issn>" +
          "</pubinfo>" +
          "<jnl-code>uACP</jnl-code>" +
          "<coverdate><year>2004</year><month>April-June</month></coverdate>" +
          "<cpyrt><date><year>2004</year></date><cpyrtnme>Taylor &amp; Francis Inc.</cpyrtnme></cpyrt>" +
          "</docmeta>" +
          "<article aid=\"UACP8888\">" +
          "<artmeta>" +
          "<article-type type=\"paper\"/>" +
          "<article-number>8888</article-number>" +
      "<doi>11.1111/8888</doi>" +
          "<fpage>75</fpage>" +
          "<lpage>85</lpage>" +
          "<countgroup>" +
          "<pagecount count=\"11\"/>" +
          "<figcount count=\"0\"/>" +
          "<tabcount count=\"1\"/>" +
          "<refcount count=\"131\"/>" +
          "</countgroup>" +
          "</artmeta>" +
          "<header>" +
          "<titlegrp>" +
          "<lrh>J. AUTHOR ET AL.</lrh>" +
          "<rrh>A TYPICAL TITLE</rrh>" +
          "<title><pgnum id=\"pg75\"/>A typical Title: Subtitle</title>" +
          "</titlegrp>" +
          "<authgrp>" +
          "<author rid=\"aff1\">" +
          "<fname>JOE</fname><lname>AUTHOR</lname><deg>M.D.</deg></author>" +
          "<author rid=\"aff1\"><fname>RAY</fname><lname>WRITER</lname></author>" +
          "<author rid=\"aff1\"><fname>KARL</fname><lname>PENCIL</lname><deg>M.D.</deg></author>" +
          "<author rid=\"aff2\"><fname>SARA</fname><lname>PEN</lname><deg>M.D.</deg></author>" +
          "<aff id=\"aff1\"><school>University of California</school><address>" +
          "<city>Los Angeles</city></address>, and <orgname>Harbor-UCLA Medical Center</orgname>" +
          "<address><city>Torrance</city>, <state>California</state>, <country>USA</country></address></aff>" +
          "<aff id=\"aff2\"><orgname>Metropolitan State Hospital</orgname>" +
          "<address><city>Norwalk</city>, <state>California</state>, <country>USA</country></address></aff>" +
          "</authgrp>" +
          "</header>" +
          "</article>" +
          "</tfdoc>";

  
  public void testExtractarticleSchema() throws Exception {
    CIProperties xmlHeader = new CIProperties();   
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    String xml_url = BASE_URL + YEAR + "/UACP/001/UACP_i_001_01_tfja.zip!/UACP_A_9973_O.xml";
    MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    mcu.setContent(articleSchemaContent);
    mcu.setContentSize(articleSchemaContent.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    // Now add all the pdf files in our AU since we check for them before emitting
    mau.addUrl(BASE_URL + YEAR + "/UACP/001/UACP_i_001_01_tfja.zip!/UACP_test_9973_O.pdf", true, true, xmlHeader);

    FileMetadataExtractor me = new TaylorAndFrancisSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(
        MetadataTarget.Any(), "text/xml");
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(article_title, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(article_articleID, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(article_date, md.get(MetadataField.FIELD_DATE));
    assertEquals(article_journal, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
  }
  
  public void testExtractunarticleSchema() throws Exception {
    CIProperties xmlHeader = new CIProperties();   
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    String xml_url = BASE_URL + YEAR + "/WJPT/008/WJPT_i_008_04_tfja.zip!/WJPT_I_8_04/WJPT_A_J060.xml";
    MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    mcu.setContent(unarticleSchemaContent);
    mcu.setContentSize(unarticleSchemaContent.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    // Now add all the pdf files in our AU since we check for them before emitting
    mau.addUrl(BASE_URL + YEAR + "/WJPT/008/WJPT_i_008_04_tfja.zip!/WJPT_I_8_04/WJPT_J060.pdf", true, true, xmlHeader);

    FileMetadataExtractor me = new TaylorAndFrancisSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(
        MetadataTarget.Any(), "text/xml");
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(unarticle_title, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(unarticle_articleID, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(unarticle_date, md.get(MetadataField.FIELD_DATE));
    assertEquals(unarticle_journal, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
  }
  
  public void testExtracttfdpcSchema() throws Exception {
    CIProperties xmlHeader = new CIProperties();   
    xmlHeader.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    String xml_url = BASE_URL + YEAR + "/UACP/016/UACP_i_016_01_tandf.zip!/UACP_16_1/UACP_8888.xml";
    MockCachedUrl mcu = mau.addUrl(xml_url, true, true, xmlHeader);
    mcu.setContent(tfdocSchemaContent);
    mcu.setContentSize(tfdocSchemaContent.length());
    mcu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/xml");
    // Now add all the pdf files in our AU since we check for them before emitting
    // because no file name given in xml, pdf file must have same base and name up to suffix as xml
    mau.addUrl(BASE_URL + YEAR + "/UACP/016/UACP_i_016_01_tandf.zip!/UACP_16_1/UACP_8888.pdf", true, true, xmlHeader);

    FileMetadataExtractor me = new TaylorAndFrancisSourceXmlMetadataExtractorFactory().createFileMetadataExtractor(
        MetadataTarget.Any(), "text/xml");
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), mcu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(tfdoc_title, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(tfdoc_articleID, md.get(MetadataField.FIELD_PROPRIETARY_IDENTIFIER));
    assertEquals(tfdoc_date, md.get(MetadataField.FIELD_DATE));
    assertEquals(tfdoc_journal, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
  }
  

}
