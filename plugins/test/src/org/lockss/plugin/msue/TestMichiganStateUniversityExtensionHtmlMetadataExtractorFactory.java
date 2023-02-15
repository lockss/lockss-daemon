/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.msue;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the HTML source for this plugin is:
 * http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e49/index.html
 *
 */
public class TestMichiganStateUniversityExtensionHtmlMetadataExtractorFactory 
  extends LockssTestCase {

  static Logger log = 
      Logger.getLogger("TestMichiganStateUniversityExtensionMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau;
  private ArchivalUnit msueau;

  private static String PLUGIN_NAME =
      "org.lockss.plugin.msue.MichiganStateUniversityExtensionPlugin";

  private static String BASE_URL = "http://web2.msue.msu.edu/";
  private static String SIM_ROOT = 
      BASE_URL + "Bulletins/Bulletin/PDF/Historical/finished_pubs/e49/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class, 
                                              simAuConfig(tempDirPath));
    msueau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, 
                                    michiganStateUniversityExtensionAuConfig());
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
    conf.put("fileTypes","" + (SimulatedContentGenerator.FILE_TYPE_PDF + 
                               SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("default_article_mime_type", "application/html");
    return conf;
  }

  /**
   * Configuration method. 
   * @return
   */
  Configuration michiganStateUniversityExtensionAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "1927");
    return conf;
  }

  // The metadata that should be extracted
  String goodArticleNumber = "e99";
  String[] goodAuthors = new String[] {"E.F. Farnsworth", "L.M. Sutter"};
  String goodDate = "June 2012";
  String goodArticleTitle = "Something Terribly Interesting: A Stirring Report";
  String goodRenamedArticleTitle = "An Alternate Title";
  String goodJournalTitle = "Michigan State University Extension Bulletin";
  String goodPdfUrl = "http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e49/e49.pdf";
  
  String goodContent = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">" + "\n" +
    "<html>" + "\n" +
    "<head>" + "\n" +
    "  <title>" + goodArticleNumber + goodArticleTitle + "</title>" + "\n" +
    "  <meta http-equiv=\"Content-Type\"" + "\n" +
    " content=\"text/html; charset=windows-1252\">" + "\n" +
    "  <meta content=\"\" name=\"keywords\">" + "\n" +
    "  <meta content=\"MSHTML 6.00.2900.3059\" name=\"GENERATOR\">" + "\n" +
    "</head>" + "\n" +
    "<body style=\"color: rgb(0, 0, 0); background-color: rgb(255, 255, 240);\">" + "\n" +
    "<p align=\"right\"><font face=\"times new roman,times\" size=\"3\">Michigan" + "\n" +
    "State University Extension </font><br>" + "\n" +
    "</p>" + "\n" +
    "<font face=\"times new roman,times\" size=\"2\"><a name=\"TOC\"></a></font>" + "\n" +
    "<h3>Something Terribly Interesting: A Stirring Report</h3>" + "\n" +
    "<hr>Text <br>" + "\n" +
    "Other text <br>" + "\n" +
    "More text<br>" + "\n" +
    "<br>" + "\n" +
    "<br>" + "\n" +
    "<p>E.F. Farnsworth, Department; L.M. Sutter, Other Department<br>" + "\n" +
    "Issued&nbsp; June&nbsp; 2012 &nbsp; 12 pages&nbsp;&nbsp; <a" + "\n" +
    " href=\"http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e49/e49.pdf\">(Fil." + "\n" +
    "1)</a><br>" + "\n" +
    "J.P. Doe<br>" + "\n" +
    "Reprinted&nbsp; June&nbsp; 2013 &nbsp; 12 pages&nbsp;&nbsp; <a" + "\n" +
    " href=\"http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e49/e49print2.pdf\">(Fil." + "\n" +
    "2)</a><br>" + "\n" +
    "J.P. Doe<br>" + "\n" +
    "Revised&nbsp; March&nbsp; 2014 &nbsp; 12 pages&nbsp;&nbsp; <a" + "\n" +
    " href=\"http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e49/e49rev1.pdf\">(Fil." + "\n" +
    "3)</a><br>" + "\n" +
    "J.P. Doe<br>" + "\n" +
    "Revised&nbsp; April&nbsp; 2015 &nbsp; 12 pages&nbsp;&nbsp; <a" + "\n" +
    " href=\"http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e49/e49rev2.pdf\">(Fil." + "\n" +
    "4)</a><br>" + "\n" +
    "<B>Renamed: An Alternate Title</b><br>" + "\n" +
    "J.P. Doe<br>" + "\n" +
    "Revised&nbsp; April&nbsp; 2016 &nbsp; 12 pages&nbsp;&nbsp; <a" + "\n" +
    " href=\"http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e49/e49rev3.pdf\">(Fil." + "\n" +
    "5)</a><br>" + "\n" +
    "</p>" + "\n" +
    "<p><span style=\"font-weight: bold;\">Archive" + "\n" +
    "Text goes here.</span><br>" + "\n" +
    "</p>" + "\n" +
    "<hr>" + "\n" +
    "</body>" + "\n" +
    "</html>";
  
  /**
   * Method that creates a simulated Cached URL from the source code provided
   * by the goodContent String. It then asserts that the metadata extracted with
   * MichiganStateUniversityExtensionHtmlMetadataExtractorFactory
   * match the metadata in the source code.
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://web2.msue.msu.edu/Bulletins/Bulletin/PDF/Historical/finished_pubs/e49/index.html";
    MockCachedUrl cu = new MockCachedUrl(url, msueau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = 
      new MichiganStateUniversityExtensionHtmlMetadataExtractorFactory.MichiganStateUniversityExtensionHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodPdfUrl, md.get(MetadataField.FIELD_ACCESS_URL));
    md = mdlist.get(4);
    assertEquals(goodRenamedArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
  }

  // A chunk of HTML source code from where the 
  // MichiganStateUniversityExtensionHtmlMetadataExtractorFactory
  // should NOT be able to extract metadata
  String badContent = "<html><head><title>" 
  + goodArticleTitle
    + "</title></head><body>\n"
    + "<meta name=\"foo\""
    + " content=\"bar\">\n"
    + "  <div id=\"issn\">"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + goodDate + " </div>\n";

  /**
   * Method that creates a simulated Cached URL from the source code 
   * provided by the badContent String. It then asserts that NO metadata is 
   * extracted from the broken source code by the metadata extractor
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, msueau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new MichiganStateUniversityExtensionHtmlMetadataExtractorFactory.MichiganStateUniversityExtensionHtmlMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
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
   * Inner class to create HTML source code simulated content
   *
   */
  public static class MySimulatedContentGenerator extends SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
      
      String file_content = "<html><head><title>" + filename + "</title></head><body>\n";
      
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";     

      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</body></html>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
        + file_content);

      return file_content;
    }
  }
}
