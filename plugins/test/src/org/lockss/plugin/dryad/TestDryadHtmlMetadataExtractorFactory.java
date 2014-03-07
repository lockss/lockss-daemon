/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.dryad;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://datadryad.org/resource/doi:10.5061/dryad.ck1rq
 */
public class TestDryadHtmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(TestDryadHtmlMetadataExtractorFactory.class);
  
  //Simulated AU to generate content
  private SimulatedArchivalUnit sau; 
  //Dryad AU
  private ArchivalUnit hau; 
  private MockLockssDaemon theDaemon;

  private static String PLUGIN_NAME = 
      "org.lockss.plugin.dryad.ClockssDryadPlugin";

  private static String BASE_URL = "http://datadryad.org/";
  private static String SIM_ROOT = BASE_URL;

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
    hau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, dryadAuConfig());
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
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + SimulatedContentGenerator.FILE_TYPE_HTML);
    conf.put("binFileSize", "7");
    return conf;
  }

  Configuration dryadAuConfig() {
     Configuration conf = ConfigManager.newConfiguration();

    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "1");
    return conf;
  }

  String goodDate = "2013-03-01T15:36:26Z";
  String goodSubject = "Quantitative Genetics; QTL mapping; genomics";
  String goodDescription = "Some text description.";
  String goodType = "Article";
  String goodArticle = "Data from: Some Arb. Article";
  String goodAuthor = "Auth, One A";
  String goodDoi = "10.5061/dryad.ck1rq";
  String journalTitle = "Dryad Digital Repository";

  // Unfortunately, it has to be on one line for an accurate representation (and
  // to work)
  String goodContent =
      "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
      "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
      "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
      "<head>" +
      "" +
      "<title>Data from: Some Arb. Article - Dryad</title>" +
      "<link href=\"http://purl.org/dc/terms/\" rel=\"schema.DCTERMS\">" +
      "<link href=\"http://purl.org/dc/elements/1.1/\" rel=\"schema.DC\">" +
      "<meta content=\"Auth, One A.\" name=\"DC.creator\">" +
      "<meta content=\"Buth, Two\" name=\"DC.creator\">" +
      "<meta content=\"Cuth, T\" name=\"DC.creator\">" +
      "<meta content=\"Wytham Woods\" name=\"DCTERMS.spatial\">" +
      "<meta content=\"51°46’N\" name=\"DCTERMS.spatial\">" +
      "<meta content=\"1°20’W\" name=\"DCTERMS.spatial\">" +
      "<meta content=\"1985-2011\" name=\"DCTERMS.temporal\">" +
      "<meta scheme=\"DCTERMS.W3CDTF\" content=\"2013-03-01T15:36:26Z\" name=\"DCTERMS.dateAccepted\">" +
      "<meta scheme=\"DCTERMS.W3CDTF\" content=\"2013-03-01T15:36:26Z\" name=\"DCTERMS.available\">" +
      "<meta scheme=\"DCTERMS.W3CDTF\" content=\"2013-07-25\" name=\"DCTERMS.issued\">" +
      "<meta content=\"doi:10.5061/dryad.ck1rq\" name=\"DC.identifier\">" +
      "<meta content=\"Auth OA, Buth T, Cuth T (2013) Some Arb. Article. Molecular Ecology 2(1): 949-962.\" name=\"DCTERMS.bibliographicCitation\">" +
      "<meta scheme=\"DCTERMS.URI\" content=\"http://hdl.handle.net/10255/dryad.46937\" name=\"DC.identifier\">" +
      "<meta content=\"Some text description.\" name=\"DC.description\">" +
      "<meta content=\"doi:10.5061/dryad.ck1rq/1\" name=\"DCTERMS.hasPart\">" +
      "<meta content=\"doi:10.5061/dryad.ck1rq/2\" name=\"DCTERMS.hasPart\">" +
      "<meta content=\"doi:10.5061/dryad.ck1rq/3\" name=\"DCTERMS.hasPart\">" +
      "<meta content=\"doi:10.1111/mec.12376\" name=\"DCTERMS.isReferencedBy\">" +
      "<meta content=\"PMID:23889544\" name=\"DCTERMS.isReferencedBy\">" +
      "<meta content=\"Quantitative Genetics\" name=\"DC.subject\">" +
      "<meta content=\"QTL mapping\" name=\"DC.subject\">" +
      "<meta content=\"genomics\" name=\"DC.subject\">" +
      "<meta content=\"Data from: Some Arb. Article\" name=\"DC.title\">" +
      "<meta xml:lang=\"*\" content=\"Article\" name=\"DC.type\">" +
      "<meta content=\"Auth, One A\" name=\"DC.contributor\">" +
      "<meta content=\"MolEcol-MEC-12-1402\" name=\"DC.identifier\">" +
      "<style type=\"text/css\" media=\"screen\">" +
      "</head>" +
      "" +
      "</html>";

  public void testExtractFromGoodContent() throws Exception {
    String url = "http://msp.org/camcos/2012/7-2/p04.xhtml";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = 
        new DryadHtmlMetadataExtractorFactory.
            DryadHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodAuthor, md.get(MetadataField.FIELD_AUTHOR));
    assertEquals(goodArticle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    // the following all come from tdb
    assertNull(md.get(MetadataField.FIELD_PUBLISHER));
    assertNull(md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
  }

  String badContent = 
      "<HTML><HEAD><TITLE>" + journalTitle + 
      "</TITLE>\n" + "<meta name=\"foo\" content=\"bar\">\n</HEAD><BODY>" + 
      "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: " + 
      goodDescription + " </div>\n";

  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, hau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    FileMetadataExtractor me = 
        new DryadHtmlMetadataExtractorFactory.
            DryadHtmlMetadataExtractor();
    assertNotNull(me);
    log.debug3("Extractor: " + me.toString());
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DATE));
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_AUTHOR));
    assertNull(md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertNull(md.get(MetadataField.FIELD_DATE));

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