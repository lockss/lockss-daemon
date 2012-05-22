
/*

 Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.libertasacademica;

import java.io.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.repository.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

/*
<meta content="Cancer Informatics, Evolutionary Bioinformatics Online, bioinformatics, biomarkers, computational biology, cancer, informatics, computational biology, phylogeny, phylogenetics, evolutionary biology, science news articles, science articles, proteomics, caBIG, biomedical informatics, informatics, proteomics, genomics, biomarkers, pathology, pathology informatics, radiology, radiology informatics, cancer genes, open access review, open access, biology, microarray, Libertas Academica, biological, evidence-bas...rch papers, review articles, scientific journal, science journals, medical journal, biology, journal publisher, biology, disease, journals, peer-reviewed journals, scientific, research papers, review articles, science journals, journal publisher, international science journal, subscribe, libertas academica, peer-reviewed Open Access journals, Cancer Informatics, Evolutionary Bioinformatics Online, open access journals, peer-reviewed journals, scientific, research papers, review articles, original research" name="keywords">
<meta content="The Use of Ion Chromatography for the Determination of Clean-In-Place-200 (CIP-200) Detergent Traces" name="citation_title">
<meta content="Wilfredo Resto" name="citation_author">
<meta content="Joan Roque" name="citation_author">
<meta content="Rosamil Rey" name="citation_author">
<meta content="Héctor Colón" name="citation_author">
<meta content="" name="citation_author">
<meta content="José Zayas" name="citation_author">
<meta content="2007/02/27" name="citation_publication_date">
<meta content="Analytical Chemistry Insights" name="citation_journal_title">
<meta content="2006" name="citation_volume">
<meta content="1" name="citation_issue">
<meta content="5" name="citation_firstpage">
<meta content="12" name="citation_lastpage">
*/

public class TestLibertasAcademicaMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger("TestLibertasAcademicaMetadataExtractor");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = "org.lockss.plugin.libertasacademica.LibertasAcademicaPlugin";
  private static String BASE_URL = "http://www.la-press.com/";
  private static String SIM_ROOT = BASE_URL + "xyzjn/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(
				  LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,	simAuConfig(tempDirPath));
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, auConfig());
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
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("year", "2006");
    conf.put("journal_id", "11");
    return conf;
  }

  // the metadata that should be extracted
  String goodVolume = "2006";
  String goodIssue = "1";
  String goodStartPage = "2";
  String goodEndPage = "14";
  String goodDate = "2007/02/27";
  String[] goodAuthors = {"Wilfredo Resto",
		  			   	  "Joan Roque",
		  			      "Rosamil Rey",
		  			      "Héctor Colón",
		  			      "José Zayas"};
  String goodArticleTitle = "The Use of Ion Chromatography for the Determination of Clean-In-Place-200 (CIP-200) Detergent Traces";
  String goodJournalTitle = "Analytical Chemistry Insights";
  String goodKeywords = "Cancer Informatics, Evolutionary Bioinformatics Online, bioinformatics, biomarkers";

  // a chunk of html source code from the publisher's site from where the 
  // metadata should be extracted

  String goodContent = 
    "<meta content=\"Sitefinity 3.6.1936.2:1\" name=\"Generator\">\n"
    + "<meta content=\"" + goodJournalTitle + "\" name=\"citation_journal_title\">\n"
    + authorHtml()
    + "<meta content=\"\" name=\"citation_author\">\n"
    + "<meta content=\"" + goodArticleTitle + "\" name=\"citation_title\">\n"
    + "<meta content=\"" + goodVolume + "\" name=\"citation_volume\">\n"
    + "<meta content=\"" + goodIssue + "\" name=\"citation_issue\">\n"
    + "<meta content=\"" + goodStartPage + "\" name=\"citation_firstpage\">\n"
    + "<meta content=\"" + goodEndPage + "\" name=\"citation_lastpage\">\n"
    + "<meta content=\"http://www.igi-global.com/Bookstore/Article.aspx?TitleId=55656\" name=\"citation_abstract_html_url\">\n"
    + "<meta content=\"http://www.igi-global.com/ViewTitle.aspx?TitleId=55656\" name=\"citation_pdf_url\">\n"
    + "<meta content=\"en\" name=\"citation_language\">\n"
    + "<meta content=\"" + goodKeywords + "\"name=\"citation_keywords\">\n"
    + "<meta content=\"" + goodDate + "\" name=\"citation_publication_date\">\n";	
   
	public String authorHtml() {
		  String authorHtml = "";
		  for(String auth : goodAuthors) {
		  	authorHtml = authorHtml + "<meta content=\"" + auth + "\" name=\"citation_author\">\n";
		  }
		  return authorHtml;
	}
  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the NatureHtmlMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new LibertasAcademicaHtmlMetadataExtractorFactory.LibertasAcademicaHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    assertEquals(Arrays.asList(goodAuthors).toString(), md.getList(MetadataField.FIELD_AUTHOR).toString());
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodEndPage, md.get(MetadataField.FIELD_END_PAGE));
    assertEquals("[" + goodKeywords + "]", md.getList(MetadataField.FIELD_KEYWORDS).toString());
  }

  // a chunk of html source code from where the NatureHtmlMetadataExtractorFactory should NOT be able to extract metadata
  String badContent = "<HTML><HEAD><TITLE>"
    + goodArticleTitle
    + "</TITLE></HEAD><BODY>\n"
    + "<meta name=\"foo\""
    + " content=\"bar\">\n"
    + "  <div id=\"issn\">"
    + "<!-- FILE: /data/templates/www.example.com/bogus/issn.inc -->MUMBLE: "
    + goodVolume + " </div>\n";

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
    FileMetadataExtractor me = new LibertasAcademicaHtmlMetadataExtractorFactory.LibertasAcademicaHtmlMetadataExtractor();
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

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
			
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
