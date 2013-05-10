/*

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
 * http://clockss-ingest.lockss.org/sourcefiles/aip-dev/2010/AIP_xml_9.tar.gz/AIP_xml_9.tar/./APPLAB/vol_96/iss_1/01212_1.xml 
 */
public class TestAmericanInstituteOfPhysicsSourceMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestAmericanInstituteOfPhysicsMetadataExtractorFactory");

  private ArchivalUnit hau;		//BloomsburyQatar AU
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME =
    "org.lockss.plugin.americaninstituteofphysics.ClockssAmericanInstituteOfPhysicsSourcePlugin";

  private static String BASE_URL = "http://www.example.com";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();//even though you don't use path, you need to call method
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, aipAuConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }


  Configuration aipAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2012");
    return conf;
  }
  
  String goodTitle = "Title";
  ArrayList<String> goodAuthors = new ArrayList<String>();
  String goodIssn = "5555-5555";
  String goodVolume = "Volume";
  String goodDate = "Date";
  String goodIssue = "Issue";
  String goodDoi = "10.1066/1492.1608";
  String goodSource = "USA";
  ArrayList<String> goodKeywords = new ArrayList<String>();
  String goodDescription = "Summary";
  String goodRights = "Rights";
  String goodJournal = "Fake Journal";
  
  String goodContent =
	  "<article xmlns:m=\"http://www.w3.org/1998/Math/MathML\">"+
	  "<front>"+
	  "<titlegrp>"+
	  "<title>"+
	  "Title"+
	  "</title>"+
	  "</titlegrp>"+
	  "<authgrp>"+
	  "<author affref=\"a1\"><fname>A.</fname><surname>Author</surname></author>"+
	  "<author affref=\"a2\" anref=\"n1\"><fname>B.</fname><surname>Author</surname></author>"+
	  "<author affref=\"a1\"><fname>C.</fname><middlename>D.</middlename><surname>Author</surname></author>"+
	  "</authgrp>"+
	  "<pubfront><journal coden=\"XXXX\" issn=\"5555-5555\" jcode=\"XX\" short=\"This. is. Fake.\">Fake Journal</journal><volume>Volume</volume>"+
	  "<issue printdate=\"2010-10-03\">Issue</issue>"+
	  "<fpage>003352</fpage>"+
	  "<lpage>003352-6</lpage>"+
	  "<seqno>1</seqno>"+
	  "<numpages>6</numpages>"+
	  "<doi><?xpp :pdfs;><?xpp co;blue?>10.1066/1492.1608<?xpp co;normal?><?xpp :pdfe;WWW?></doi>"+
	  "<aipkey>1.23581321</aipkey>"+
	  "<history>"+
	  "<received date=\"2009-09-18\"/>"+
	  "<accepted date=\"2009-11-04\"/>"+
	  "<published date=\"Date\"/><online date=\"unimportant\"/>"+
	  "</history>"+
	  "</pubfront>"+
	  "<pacs pacsyr=\"2010\">"+
	  "<pacscode>5555Fx</pacscode>"+
	  "</pacs>"+
	  "<spin>"+
	  "<jouidx>"+
	  "<country>USA</country>"+
	  "<totalidx>3</totalidx>"+
	  "<addidx>Keyword1</addidx>"+
	  "<addidx>Keyword2</addidx>"+
	  "<addidx>Keyword3</addidx>"+
	  "</jouidx>"+
	  "</spin>"+
	  "<abstract>"+
	  "Summary"+
	  "</abstract>"+
	  "<cpyrt><cpyrtdate date=\"Rights\"></cpyrtdate></cpyrt>"+
	  "</front>"+
	  "</article>";


  public void testExtractFromGoodContent() throws Exception {
	  goodAuthors.add("Author, A.");
	  goodAuthors.add("Author, B.");
	  goodAuthors.add("Author, C. D.");
	  goodKeywords.add("Keyword1");
	  goodKeywords.add("Keyword2");
	  goodKeywords.add("Keyword3");
	  
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    FileMetadataExtractor me =
      new AmericanInstituteOfPhysicsSourceMetadataExtractorFactory.AIPXmlMetadataExtractor();
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
    assertEquals(goodSource, md.get(MetadataField.DC_FIELD_SOURCE));
    assertEquals(goodKeywords, md.getList(MetadataField.FIELD_KEYWORDS));
    assertEquals(goodDescription, md.get(MetadataField.DC_FIELD_DESCRIPTION));
    assertEquals(goodRights, md.get(MetadataField.DC_FIELD_RIGHTS));
    assertEquals(goodJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
  }
  
  String badContent =
    "<HTML><HEAD><TITLE>" + goodTitle + "</TITLE></HEAD><BODY>\n" + 
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
      new AmericanInstituteOfPhysicsSourceMetadataExtractorFactory.AIPXmlMetadataExtractor();
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
  }
  
  String missingJournalContent =
      "<article xmlns:m=\"http://www.w3.org/1998/Math/MathML\">"+
      "<front>"+
      "<pubfront><volume>Volume</volume>"+
      "</pubfront>"+
      "</front>"+
      "</article>";
  String emptyJournalContent =
      "<article xmlns:m=\"http://www.w3.org/1998/Math/MathML\">"+
      "<front>"+
      "<pubfront><journal coden=\"XXXX\" issn=\"5555-5555\" jcode=\"SE\" short=\"S. Empty\"></journal><volume>Volume</volume>"+
      "</pubfront>"+
      "</front>"+
      "</article>";
  
  public void testExtractJournalContent() throws Exception {

    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(missingJournalContent);
    cu.setContentSize(missingJournalContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    FileMetadataExtractor me =
        new AmericanInstituteOfPhysicsSourceMetadataExtractorFactory.AIPXmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(null, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(null, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    
    cu.setContent(emptyJournalContent);
    cu.setContentSize(emptyJournalContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "application/xml");
    List<ArticleMetadata> md2list = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(md2list);
    ArticleMetadata md2 = md2list.get(0);
    assertNotNull(md2);
    
    assertEquals(goodIssn, md2.get(MetadataField.FIELD_ISSN));
    assertEquals(goodVolume, md2.get(MetadataField.FIELD_VOLUME));
    assertEquals(null, md2.get(MetadataField.FIELD_JOURNAL_TITLE));
  }

}