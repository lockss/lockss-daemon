/* $Id$ */
/*
 Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.massachusettsmedicalsociety;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/* Sample RIS file
TY  - JOUR
T1  - Viral Hepatitis, Type B
AU  - Krugman, Saul
AU  - Overby, Lacy R.
AU  - Mushahwar, Isa K.
AU  - Ling, Chung-Mei
AU  - Frösner, Gert G.
AU  - Deinhardt, Friedrich
Y1  - 1979/01/18
PY  - 1979
DA  - 1979/01/18
N1  - doi: 10.1056/NEJM197901183000301
DO  - 10.1056/NEJM197901183000301
T2  - New England Journal of Medicine
JF  - New England Journal of Medicine
JO  - N Engl J Med
SP  - 101
EP  - 106
VL  - 300
IS  - 3
PB  - Massachusetts Medical Society
SN  - 0028-4793
M3  - doi: 10.1056/NEJM197901183000301
UR  - http://dx.doi.org/10.1056/NEJM197901183000301
Y2  - 2012/02/29
ER  -*/ 
public class TestMassachusettsMedicalSocietyMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(TestMassachusettsMedicalSocietyMetadataExtractor.class);

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = "org.lockss.plugin.massachusettsmedicalsociety.MassachusettsMedicalSocietyPlugin";

  private static String BASE_URL = "http://www.nejm.org/";
  private static String BASE_URL2 = "http://cdn.nejm.org/";
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
    conf.put("fileTypes",""	+ (SimulatedContentGenerator.FILE_TYPE_PDF + SimulatedContentGenerator.FILE_TYPE_HTML));
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
    conf.put("base_url2", BASE_URL2);
    conf.put("volume_name", "300");
    conf.put("journal_id", "nejm");
    return conf;
  }

  // the metadata that should be extracted
  String goodDoi = "10.1056/NEJM197901183000301";
  String goodVolume = "300";
  String goodIssue = "3";
  String goodStartPage = "101";
  String goodEndPage = "106";
  String goodIssn = "0028-4793";
  String goodDate = "1979/01/18";
  String goodAuthors[] = {"Krugman, Saul",
		   	  "Overby, Lacy R.",
		   	  "Mushahwar, Isa K.",
		   	  "Ling, Chung-Mei",
		   	  "Frösner, Gert G."};
  String goodArticleTitle = "Viral Hepatitis, Type B";
  String goodPublisher = "Massachusetts Medical Society";
  String goodJournalTitle = "New England Journal of Medicine";
  String goodAccessUrl = BASE_URL + "doi/full/" + goodDoi;

  private String createGoodContent() {
	  StringBuilder sb = new StringBuilder();
	  sb.append("TY  - JOUR");
	  for(String auth : goodAuthors) {
		  sb.append("\nAU  - ");
		  sb.append(auth);
	  }
	  sb.append("\nY1  - ");
	  sb.append(goodDate);
	  sb.append("\nPY  - ");
	  sb.append(goodDate);
	  sb.append("\nDA  - ");
	  sb.append(goodDate);
	  sb.append("\nT2  - ");
	  sb.append(goodJournalTitle);
	  sb.append("\nJF  - ");
	  sb.append(goodJournalTitle);
	  sb.append("\nJO  - N Engl J Med");
	  sb.append("\nSP  - ");
	  sb.append(goodStartPage);
	  sb.append("\nEP  - ");
	  sb.append(goodEndPage);
	  sb.append("\nVL  - ");
	  sb.append(goodVolume);
	  sb.append("\nIS  - ");
	  sb.append(goodIssue);
	  sb.append("\nPB  - ");
	  sb.append(goodPublisher);
	  sb.append("\nSN  - ");
	  sb.append(goodIssn);
	  sb.append("\nDO  - " + goodDoi);
	  sb.append("\nT1  - ");
	  sb.append(goodArticleTitle);
	  sb.append("\nM3  - doi: " + goodDoi);
	  //sb.append("\nUR  - http://dx.doi.org/" + goodDoi);
	  sb.append("\nY2  - 2012/02/29");
	  sb.append("\nER  -");
	  return sb.toString();
  }
  // MostlyGoodContent has no access url
  private String createMostlyGoodContent() {
    StringBuilder sb = new StringBuilder();
    sb.append("TY  - JOUR");
    for(String auth : goodAuthors) {
            sb.append("\nAU  - ");
            sb.append(auth);
    }
    sb.append("\nY1  - ");
    sb.append(goodDate);
    sb.append("\nPY  - ");
    sb.append(goodDate);
    sb.append("\nDA  - ");
    sb.append(goodDate);
    sb.append("\nT2  - ");
    sb.append(goodJournalTitle);
    sb.append("\nJF  - ");
    sb.append(goodJournalTitle);
    sb.append("\nJO  - N Engl J Med");
    sb.append("\nSP  - ");
    sb.append(goodStartPage);
    sb.append("\nEP  - ");
    sb.append(goodEndPage);
    sb.append("\nVL  - ");
    sb.append(goodVolume);
    sb.append("\nIS  - ");
    sb.append(goodIssue);
    sb.append("\nPB  - ");
    sb.append(goodPublisher);
    sb.append("\nSN  - ");
    sb.append(goodIssn);
    sb.append("\nDO  - " + goodDoi);
    sb.append("\nT1  - ");
    sb.append(goodArticleTitle);
    sb.append("\nM3  - doi: " + goodDoi);
    sb.append("\nY2  - 2012/02/29");
    sb.append("\nER  -");
    return sb.toString();
}
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the MassachussetsMedicalSocietyRisMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
	String goodContent = createGoodContent();
	//log.info(goodContent);
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_RIS);
    FileMetadataExtractor me = new MassachusettsMedicalSocietyRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), Constants.MIME_TYPE_RIS);
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    Iterator<String> actAuthIter = md.getList(MetadataField.FIELD_AUTHOR).iterator();
    for(String expAuth : goodAuthors) {
    	assertEquals(expAuth, actAuthIter.next());
    }
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodAccessUrl, md.get(MetadataField.FIELD_ACCESS_URL));
  }
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the MostlyGoodContent String (has no access url, which is manufactured from the
   * DOI, if it exists. It then asserts that the metadata extracted, by using
   * the MassachussetsMedicalSocietyRisMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromMostlyGoodContent() throws Exception {
        String goodContent = createMostlyGoodContent();
        //log.info(goodContent);
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_RIS);
    FileMetadataExtractor me = new MassachusettsMedicalSocietyRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), Constants.MIME_TYPE_RIS);
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodDoi, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(goodIssn, md.get(MetadataField.FIELD_ISSN));
    Iterator<String> actAuthIter = md.getList(MetadataField.FIELD_AUTHOR).iterator();
    for(String expAuth : goodAuthors) {
        assertEquals(expAuth, actAuthIter.next());
    }
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodAccessUrl, md.get(MetadataField.FIELD_ACCESS_URL));

  }
  // a chunk of RIS source code from where the NatureHtmlMetadataExtractorFactory should NOT be able to extract metadata
  String badContent = "SN - " + goodIssn
    + "\nT1 - " + goodArticleTitle;

  /**
   * Method that creates a simulated Cached URL from the source code provided by the badContent Sring. It then asserts that NO metadata is extracted by using 
   * the NatureHtmlMetadataExtractorFactory as the source code is broken.
   * @throws Exception
   */
  public void testExtractFromBadContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new MassachusettsMedicalSocietyRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any, Constants.MIME_TYPE_RIS);
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertNull(md.get(MetadataField.FIELD_DOI));
    assertNull(md.get(MetadataField.FIELD_VOLUME));
    assertNull(md.get(MetadataField.FIELD_ISSUE));
    assertNull(md.get(MetadataField.FIELD_START_PAGE));
    assertNull(md.get(MetadataField.FIELD_ISSN));

    assertEquals(0, md.rawSize());
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

  }
}
