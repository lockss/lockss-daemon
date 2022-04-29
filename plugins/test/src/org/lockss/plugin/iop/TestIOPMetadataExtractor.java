/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.iop;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://www.IOPScience.com/cppm/vol5/iss1/17/
 *
 */
public class TestIOPMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestIOPMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau; // IOPScience AU

  // XML file in org.lockss.plugin.IOPScience package
  private static String PLUGIN_NAME = "org.lockss.plugin.iop.ClockssIOPSciencePlugin";

  private static String BASE_URL = "http://iopscience.iop.org/";
  private static String SIM_ROOT = BASE_URL + "xyzjn/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,	simAuConfig(tempDirPath));
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, natureAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "2");
    conf.put("branch", "3");
    conf.put("numFiles", "7");
    conf.put("fileTypes",""	+ (
        SimulatedContentGenerator.FILE_TYPE_PDF +
        SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration natureAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "1");
    conf.put("journal_issn", "2043-6262");
    return conf;
  }
  
  String url = "http://iopscience.iop.org/2043-6262/1/4/043003";
  // the metadata that should be extracted
  String goodDOI = "10.1088/2043-6262/1/4/043003";
  String goodArticleTitle = "Polymer materials with spatially graded morphologies: " +
      "preparation, characterization and utilization";
  String goodVolume = "1";
  String goodIssue = "4";
  String goodStartPage = "043003";
  String goodISSN = "2043-6262";
  String goodDate = "2011-01-25";
  String[] goodAuthors = new String[] {
      "Daisuke Fujiki", "Chuanming Jing", "Dan-Thuy Van-Pham", "Hideyuki Nakanishi"
      ,"Tomohisa Norisuye","Qui Tran-Cong-Miyata"};
  String goodJournalTitle = "Advances in Natural Sciences: Nanoscience and Nanotechnology";

  // a chunk of html source code from the publisher's site from where the 
  // metadata should be extracted
  String goodContent = 
      "<HTML><HEAD><TITLE>"+ "blabla"+"</TITLE></HEAD><BODY>\n"
    + "<meta name=\"citation_journal_title\" content=\""+ goodJournalTitle+ "\">\n"
    + "<meta name=\"citation_author\" content=\""+ goodAuthors[0]+ "\">\n"
    + "<meta name=\"citation_author\" content=\""+ goodAuthors[1]+ "\">\n"
    + "<meta name=\"citation_author\" content=\""+ goodAuthors[2]+ "\">\n"
    + "<meta name=\"citation_author\" content=\""+ goodAuthors[3]+ "\">\n"
    + "<meta name=\"citation_author\" content=\""+ goodAuthors[4]+ "\">\n"
    + "<meta name=\"citation_author\" content=\""+ goodAuthors[5]+ "\">\n"
    + "<meta name=\"citation_title\" content=\""+ goodArticleTitle+ "\">\n"
    + "<meta name=\"citation_journal_title\" content=\""+ goodJournalTitle+ "\">\n"
    + "<meta name=\"citation_publication_date\" content=\""+ goodDate+ "\">\n"
    + "<meta name=\"citation_volume\""+ " content=\""+ goodVolume+ "\">\n"
    + "<meta name=\"citation_issue\" content=\""+ goodIssue	+ "\">\n"
    + "<meta name=\"citation_firstpage\""+ " content=\""+ goodStartPage	+ "\">\n"
    + "<meta name=\"citation_doi\"" + " content=\""     + goodDOI + "\">\n"
    + "<meta name=\"citation_issn\"" + " content=\""     + goodISSN + "\">\n"
    + "<meta name=\"citation_fulltext_html_url\" content=\"" + url + "/article/\"/>"
  ;	
		
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the IOPScienceHtmlMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = 
        new IOPScienceHtmlMetadataExtractorFactory.IOPScienceHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));

    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodAuthors[0], md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(url, md.get(MetadataField.FIELD_ACCESS_URL));
  }

  // a chunk of html source code from where the IOPScienceHtmlMetadataExtractorFactory
  // should NOT be able to extract metadata
  String badContent = "<HTML><HEAD><TITLE>"
    + goodArticleTitle
    + "</TITLE></HEAD><BODY>\n"
    + "<meta name=\"foo\""
    + " content=\"bar\">\n"
    + "  <div id=\"issn\">"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + goodISSN + " </div>\n";

  /**
   * Method that creates a simulated Cached URL from the source code provided by the
   * badContent String. It then asserts that NO metadata is extracted by using 
   * the IOPScienceHtmlMetadataExtractorFactory as the source code is broken.
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = 
        new IOPScienceHtmlMetadataExtractorFactory.IOPScienceHtmlMetadataExtractor();
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

    assertEquals(1, md.rawSize());
    assertEquals("bar", md.getRaw("foo"));
  }	

  /**
   * Inner class that where a number of Archival Units can be created
   *
   */
  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
	throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf, String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class to create a html source code simulated content
   *
   */
  public static class MySimulatedContentGenerator extends	SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, 
        int branchNum, boolean isAbnormal) {
			
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
			
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";			

      file_content += getHtmlContent(fileNum, depth, branchNum,	isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
		    + file_content);

      return file_content;
    }
  }
}
