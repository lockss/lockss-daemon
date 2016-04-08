/*
 * $Id$
 */

/*

 Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.metapress;

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
﻿
TY  - JOUR
JF  - Electronic Government, an International Journal 
T1  - Will knowledge management work in the government?
VL  - 1
IS  - 1
SP  - 1
EP  - 7
PY  - 2004/01/01/
UR  - http://inderscience.metapress.com/content/NXP64LTA63J4R5EH
AU  - Liebowitz, Jay
N2  - Knowledge management is the process of creating value from an organisation&#39;s intangible assets. The ability to share and leverage knowledge internally and externally to create knowledge and innovation is a cornerstone of knowledge management. Industry has been involved in formal knowledge management practices over the years, even hiring Chief Knowledge Officers to spearhead their knowledge management initiatives. The government has lagged behind a bit but realises the importance of knowledge management to their organisations in times of shrinking budgets, ensuing retirements and the need to better share information and knowledge within and across government organisations. The key question is whether knowledge management can succeed in the government. This paper explores some issues to help shed light on this subject.
ER  -
  */ 
public class TestMetapressRisMetadataExtractorFactory extends LockssTestCase {

  static Logger log = Logger.getLogger(TestMetapressRisMetadataExtractorFactory.class);

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = "org.lockss.plugin.metapress.ClockssMetaPressPlugin";

  private static String BASE_URL = "http://inderscience.metapress.com/";
  private static String SIM_ROOT = BASE_URL + "content/kv824m8x38336011";

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
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, metapressAuConfig());
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
  Configuration metapressAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("volume_name", "8");
    conf.put( "journal_issn", "1740-7508");
    return conf;
  }

  // the metadata that should be extracted
  String goodVolume = "1";
  String goodIssue = "1";
  String goodStartPage = "1";
  String goodEndPage = "7";
  String goodIssn = "0028-4793";
  String goodDate = "1979/01/18";
  String goodAuthors[] = {"Liebowitz, Jay"};
  String goodArticleTitle = "Knowledge management is the process of creating value from an organisation&#39;s intangible assets. The ability to share and leverage knowledge internally and externally to create knowledge and innovation is a cornerstone of knowledge management. Industry has been involved in formal knowledge management practices over the years, even hiring Chief Knowledge Officers to spearhead their knowledge management initiatives. The government has lagged behind a bit but realises the importance of knowledge management to their organisations in times of shrinking budgets, ensuing retirements and the need to better share information and knowledge within and across government organisations. The key question is whether knowledge management can succeed in the government. This paper explores some issues to help shed light on this subject";
  String goodJournalTitle = "Electronic Government, an International Journal";

  private String createGoodContent() {
	  StringBuilder sb = new StringBuilder();
	  sb.append("TY  - JOUR");
	  for(String auth : goodAuthors) {
		  sb.append("\nAU  - ");
		  sb.append(auth);
	  }
	  sb.append("\nPY  - ");
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
	  sb.append("\nSN  - ");
	  sb.append(goodIssn);
	  sb.append("\nT1  - ");
	  sb.append(goodArticleTitle);
	  sb.append("\nER  - ");
	  return sb.toString();
  }
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the MetaPressRisMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
	  String goodContent = createGoodContent();
	  log.info(goodContent);
    String url = "http://inderscience.metapress.com/content/kv824m8x38336011";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_RIS);
    FileMetadataExtractor me = new MetapressRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any, Constants.MIME_TYPE_RIS);
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
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
  }

  
  String multiLineRis = 
      "" +
          "TY  - JOUR\n" +
          "JF  - International Journal of Nanomanufacturing\n" +
          "T1  - Synthesis and properties of Eu\n" +
          " &lt;sup&gt;3+&lt;/sup&gt;\n" +
          " -doped LaPO\n" +
          " &lt;sub&gt;4&lt;/sub&gt;\n" +
          " powders\n" +
          "VL  - 9\n" +
          "IS  - 2\n" +
          "SP  - 130\n" +
          "EP  - 136\n" +
          "PY  - 2013/01/01/\n" +
          "UR  - http://dx.doi.org/10.1504/IJNM.2013.055139\n" +
          "DO  - 10.1504/IJNM.2013.055139\n" +
          "AU  - Yang, Yuguo\n" +
          "N2  - Eu 3+ -doped LaPO 4 powders were produced from direct liquid-liquid reaction of lanthanum chloride (LaCl 3 ) and ammonium phosphate ((NH 4 ) 2 HPO\n" + 
          "4 ). The sample was characterised by XRD and FTIR, which reveal that there is a phase transformation after the heat-treatment at 700ºC. The optical proper\n" +
          "-doped LaPO 4 powders were investigated. Meanwhile, the influence of the dosage concentration of Eu 3+ on the luminescent properties was stu\n" +
          "died. When above 5 mol%, the luminescence intensity decreases.\n" +
          "ER  -\n";
      
  
  public void testExtractFromMultiLineContent() throws Exception {
    String url = "http://inderscience.metapress.com/content/kv824m8x38336011";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(multiLineRis);
    cu.setContentSize(multiLineRis.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, Constants.MIME_TYPE_RIS);
    FileMetadataExtractor me = new MetapressRisMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any, Constants.MIME_TYPE_RIS);
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    //log.info(md.ppString(2));
    assertNotNull(md);
    assertEquals("9", md.get(MetadataField.FIELD_VOLUME));
    assertEquals("2", md.get(MetadataField.FIELD_ISSUE));
    assertEquals("130", md.get(MetadataField.FIELD_START_PAGE));
    assertEquals("Synthesis and properties of Eu" +
          " &lt;sup&gt;3+&lt;/sup&gt;" +
          " -doped LaPO" +
          " &lt;sub&gt;4&lt;/sub&gt;" +
          " powders", md.get(MetadataField.FIELD_ARTICLE_TITLE));
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
  public static class MySimulatedContentGenerator extends 
    SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

  }
}