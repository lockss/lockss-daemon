/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ashdin;

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
 * One of the articles used to get the html source for this plugin is:
 * http://www.ingentaconnect.com/content/maney/bjdd/2011/00000057/00000113/art00004
 */
public class TestAshdinMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger("TestAshdinMetadataExtractorFactory");
  
  //Simulated AU to generate content
  private SimulatedArchivalUnit sau; 
  //Ingenta AU
  private ArchivalUnit hau; 
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME = "org.lockss.plugin.ashdin.AshdinPlugin";

  private static String BASE_URL = "http://www.ashdin.com/";
  private static final int DEFAULT_FILESIZE = 3000;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
        tempDirPath);
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,
        simAuConfig(tempDirPath));
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, ashdinAuConfig());
  }

  public void tearDown() throws Exception {
    sau.deleteContentTree();
    theDaemon.stopDaemon();
    super.tearDown();
  }

  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", "http://www.ashdin.com/");
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes",
             "" + (SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+ DEFAULT_FILESIZE);
    return conf;
  }
  Configuration ashdinAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.ashdin.com/");
    conf.put("year", "2010");
    conf.put("journal_code", "acpsf");
    return conf;
  }

  String goodDate = "Date";
  String goodJournal = "Journal of Testing";
  String goodArticle = "Article Title";
  String goodAuthor = "John A. Author; John B. Author";
  String goodDoi = "10.5555/TEST_DOI";
  String goodVolume = "Volume";
  
  String goodContent =
      "<HTML><BODY>\n"+  
	  "<div class=\"abs\" xmlns=\"http://www.w3.org/1999/xhtml\">\n"+
	  "<pre>Journal of Testing<br />Vol.Volume (Date), Article ID F0A0K0E, 12 pages &nbsp;[<a class=\"hrefcont\" href=\"TESTING.pdf\">Full-Text PDF</a>]<br/>doi:10.5555/TEST_DOI</pre>\n"+
	  "<h2 class=\"abstitle\">Article Title</h2>\n"+
	  "<h3 class=\"absauthor\">John A. Author<sup>1</sup>; John B. Author<sup>2</sup></h3>\n"+
	  "<p class=\"absaddr\"><sup>1</sup>Department of Testing, USA<br/>\n"+
	  "<sup>2</sup>Department of Computer Science, Brazil<br/>\n"+ 
	  "<br/>";
  
   
	public void testExtractFromGoodContent() throws Exception {
		String url = "http://www.ashdin.com/journals/ACPSF/235465.aspx";
		MockCachedUrl cu = new MockCachedUrl(url, hau);
		cu.setContent(goodContent);
		cu.setContentSize(goodContent.length());
		cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
		FileMetadataExtractor me = new AshdinMetadataExtractorFactory.AshdinHtmlMetadataExtractor();
		assertNotNull(me);

		FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
		List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
		assertNotEmpty(mdlist);
		ArticleMetadata md = mdlist.get(0);
		assertNotNull(md);
		assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
		assertEquals(goodArticle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
		assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
		assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
		assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
		assertEquals(goodJournal, md.get(MetadataField.FIELD_JOURNAL_TITLE));
	}

   String badContent = "<HTML><HEAD><TITLE>" + goodArticle
      + "</TITLE></HEAD><BODY>\n" + "<meta name=\"foo\""
      + " content=\"bar\">\n" + "  <pre xmlns=\"issn\">"
      + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
      + goodJournal + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me = new AshdinMetadataExtractorFactory.AshdinHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertEquals(1, md.rawSize());
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

    public SimulatedContentGenerator getContentGenerator(Configuration cf,
        String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }

  /**
   * Inner class to create a html source code simulated content
   */
  public static class MySimulatedContentGenerator extends
      SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth,
        int branchNum, boolean isAbnormal) {

      String file_content = "<HTML><HEAD><TITLE>" + filename
          + "</TITLE></HEAD><BODY>\n";

      file_content += "  <meta name=\"lockss.filenum\" content=\"" + fileNum
          + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth
          + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\""
          + branchNum + "\">\n";

      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
          + file_content);

      return file_content;
    }
  }
}