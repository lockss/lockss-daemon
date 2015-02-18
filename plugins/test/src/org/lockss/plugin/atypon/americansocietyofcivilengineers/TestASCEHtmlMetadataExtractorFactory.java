/* $Id$

 Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.americansocietyofcivilengineers;

import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.plugin.simulated.*;

/*
 * Invokes BaseAtyponHtmlMetadataExtractorFactory.
 * 
 * One of the articles used to get the HTML source for this plugin is:
 * http://ascelibrary.org/doi/full/10.1061/%28ASCE%291076-0431%282009%2915%3A1%284%29
 */
public class TestASCEHtmlMetadataExtractorFactory extends LockssTestCase {

  static Logger log = Logger.getLogger("TestASCEMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // simulated au to generate content
  private ArchivalUnit asceau; // asce au
  private static String PLUGIN_NAME = "org.lockss.plugin.atypon.americansocietyofcivilengineers.ClockssASCEPlugin";
  private static String BASE_URL = "http://ascelibrary.org/";
  private final String JOURNAL_ID = "jaeied";
  private final String JOURNAL_ISSN = "1076-0431";
  private final String YEAR = "2009";
  private final String VOLUME_NAME = "15";
  
  // Simulated journal ID: "ASCE Journal"
  private static String SIM_ROOT = BASE_URL + "ascejn/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    //sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
    //                                         simAuConfig(tempDirPath));
    asceau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, ASCEAuConfig());
  }

  public void tearDown() throws Exception {
    //sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  // Configuration method. 
  Configuration ASCEAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JOURNAL_ID);
    conf.put("journal_issn", JOURNAL_ISSN);
    conf.put("year", YEAR);
    conf.put("volume_name", VOLUME_NAME);
    return conf;
  }

  // the metadata that should be extracted
  String goodDCTitle = 
        "Framework for Teaching Engineering Capstone Design Courses "
        + "with Emphasis on Application of Internet-Based Technologies";
  String[] goodDCCreators = 
      new String[] {"Jonathan U.Dougherty",
                    "M. KevinParfitt"};
  String goodDCPublisher = "American Society of Civil Engineers";
  String goodDCDate = "2012-02-27";
  String goodDCFormat = "text/HTML";
  String goodDCSource = "http://dx.doi.org/10.1061/(ASCE)1076-0431(2009)15:1(4)";
  String goodDCLanguage = "en";
  String goodDCCoverage = "world";
  String[] goodDCIdentifiers =
      new String[] {"10.1061/(ASCE)1076-0431(2009)15:1(4)",
                    "1.3071861",
                    "004901QAE",
                    "AE/2008/022232",
                    "1076-0431()15:1L.4;1"}; 
  String goodIssn = "1076-0431";
  
  // a chunk of html source code from the publisher's site from where the 
  // metadata should be extracted
  String goodContent = 
      "<meta name=\"dc.Title\" content=\"" + goodDCTitle + "\"></meta>"
      + "<meta name=\"dc.Creator\" content=\"" + goodDCCreators[0] + "\"></meta>"
      + "<meta name=\"dc.Creator\" content=\"" + goodDCCreators[1] + "\"></meta>"
      + "<meta name=\"dc.Publisher\" content=\"" + goodDCPublisher + "\"></meta>"
      + "<meta name=\"dc.Date\" scheme=\"WTN8601\" content=\"" + goodDCDate + "\"></meta>"
      + "<meta name=\"dc.Format\" content=\"" + goodDCFormat + "\"></meta>"
      + "<meta name=\"dc.Identifier\" scheme=\"doi\" content=\"" + goodDCIdentifiers[0] + "\"></meta>"
      + "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"" + goodDCIdentifiers[1] + "\"></meta>"
      + "<meta name=\"dc.Identifier\" scheme=\"publisher-id\" content=\"" + goodDCIdentifiers[2] + "\"></meta>"
      + "<meta name=\"dc.Identifier\" scheme=\"editor-id\" content=\"" + goodDCIdentifiers[3] + "\"></meta>"
      + "<meta name=\"dc.Identifier\" scheme=\"sisac\" content=\"" + goodDCIdentifiers[4] + "\"></meta>"
      + "<meta name=\"dc.Source\" content=\"" + goodDCSource + "\"></meta>"
      + "<meta name=\"dc.Language\" content=\"" + goodDCLanguage + "\"></meta>"
      + "<meta name=\"dc.Coverage\" content=\"" + goodDCCoverage + "\"></meta>";
      
  // Method that creates a simulated Cached URL from the source code 
  // provided by the goodContent string. It then asserts that the metadata 
  // extracted with ASCEHtmlMetadataExtractorFactory
  // match the metadata in the source code. 
  public void testExtractFromGoodContent() throws Exception {
    // HTML file which the metadata extracted from.
    String url = "http://ascelibrary.org/doi/full/10.1061/%28ASCE%291076-0431%282009%2915%3A1%284%29";
    MockCachedUrl cu = new MockCachedUrl(url, asceau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    
    // Create FileMetadataExtractor object through ASCEHtmlMetadataExtractor().
    FileMetadataExtractor me = 
          new BaseAtyponHtmlMetadataExtractorFactory.BaseAtyponHtmlMetadataExtractor();
    
    // Create the metadata list containing all articles for this AU.
    // In this test case, the list has only one item.
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);

    assertEquals(goodDCTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDCPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodDCDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodDCFormat, md.get(MetadataField.DC_FIELD_FORMAT));
    assertEquals(goodDCLanguage, md.get(MetadataField.DC_FIELD_LANGUAGE));
    assertEquals(goodDCCoverage, md.get(MetadataField.DC_FIELD_COVERAGE));
    assertEquals(goodDCIdentifiers[0], md.get(MetadataField.FIELD_DOI));
    assertEquals(Arrays.asList(goodDCCreators), md.getList(MetadataField.FIELD_AUTHOR));
  }

}
