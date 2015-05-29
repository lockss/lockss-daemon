/* $Id$ */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.acsess;

import java.util.*;

import org.lockss.test.*;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.*;
import org.lockss.plugin.*;

/* 
 * Metadata from abstract or full text html:
 * https://dl.sciencesocieties.org/publications/aj/abstracts/106/1/185
 */
public class TestACSESSJournalsHtmlMetadataExtractorFactory 
  extends LockssTestCase {
  
  private MockLockssDaemon theDaemon;
  private ArchivalUnit aau;
  
  private static final String PLUGIN_ID = 
      "org.lockss.plugin.acsess.ClockssACSESSJournalsPlugin";

  private static final String BASE_URL = "http://www.example.com/";
  private static final String JID = "xjid";
  private static final String VOL = "106";
 
  public void setUp() throws Exception {
    super.setUp();
    // even though you don't use path, you need to call method setUpDiskSpace
    String tempDirPath = setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    aau = PluginTestUtil.createAndStartAu(PLUGIN_ID, nwaAuConfig());
  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  private Configuration nwaAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JID);
    conf.put("volume_name", VOL);
    return conf;
  }
  
  // the metadata that should be extracted
  String goodFormat = "text/html";
  String goodLanguage = "en";
  String goodIdentifier = "11.1111/cs2014-47-3-1";
  String goodPublisher = "A Publisher";
  String goodContributor = "Contributor 1";
  String goodPublicationTitle = "XYZ Journal";
  String goodIssn = "1111-2222";
  String goodAuthor1 = "A, Author1";
  String goodAuthor2 = "B, Author2";
  List<String> goodAuthors = Arrays.asList("A, Author1", "B, Author2");
  String goodArticleTitle = "Article Title";
  String goodDate = "2014/5-6/01";  
  String goodVolume = "47";
  String goodIssue = "3";
  String goodStartPage = "4";
  String goodEndPage = "11";
  String goodDoi = "10.2134/cs2014-47-3-1";

  // a chunk of html source code from the publisher's site from where the 
  // metadata should be extracted
  String goodContent = 
      "<meta content=\"text/html\" name=\"DC.Format\"/>" +
      "<meta content=\"en\" name=\"DC.Language\"/>" +
      "<meta content=\"Article Title\" name=\"DC.Title\"/>" +
      "<meta content=\"11.1111/cs2014-47-3-1\" name=\"DC.Identifier\"/>" +
      "<meta content=\"--\" name=\"DC.Date\"/>" +
      "<meta content=\"A Publisher\" name=\"DC.Publisher\"/>" +
      "<meta content=\"Contributor 1\" name=\"DC.Contributor\"/>" +
      "<meta content=\"XYZ Journal\" name=\"citation_journal_title\"/>" +
      "<meta content=\"1111-2222\" name=\"citation_issn\"/>" +
      "<meta content=\"A, Author1\" name=\"citation_author\"/>" +
      "<meta content=\"B, Author2\" name=\"citation_author\"/>" +
      "<meta content=\"Article Title\" name=\"citation_title\"/>" +
      "<meta content=\"2014/5-6/01\" name=\"citation_publication_date\"/>" +
      "<meta content=\"47\" name=\"citation_volume\"/>" +
      "<meta content=\"3\" name=\"citation_issue\"/>" +
      "<meta content=\"4\" name=\"citation_firstpage\"/>" +
      "<meta content=\"11\" name=\"citation_lastpage\"/>" +
      "<meta content=\"47/3/4\" name=\"citation_id\"/>" +
      "<meta content=\"CNS;47/3/4\" name=\"citation_mjid\"/>" +
      "<meta content=\"10.2134/cs2014-47-3-1\" name=\"citation_doi\"/>";
        
  public void testExtractFromGoodContent() throws Exception {
    
    String absUrl = 
        "https://www.example.com/publications/xjid/abstracts/106/1/57";

    MockCachedUrl cu = new MockCachedUrl(absUrl, aau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");

    // Create FileMetadataExtractor object through
    // NWAMetadataExtractorFactory().
    FileMetadataExtractor me = 
        new ACSESSJournalsHtmlMetadataExtractorFactory.
                              ACSESSJournalsHtmlMetadataExtractor();
    
    // Create the metadata list for this AU.
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
   
    assertEquals(goodFormat, md.get(MetadataField.DC_FIELD_FORMAT));
    assertEquals(goodLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodIdentifier, md.get(MetadataField.DC_FIELD_IDENTIFIER));
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodContributor, md.get(MetadataField.DC_FIELD_CONTRIBUTOR));
    assertEquals(goodPublicationTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    assertSameElements(goodAuthors, md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));  
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));    
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodEndPage, md.get(MetadataField.FIELD_END_PAGE));    
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
  }
  
}