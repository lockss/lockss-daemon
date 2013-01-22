
/*

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

package org.lockss.plugin.internationalunionofcrystallography;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.maffey.MaffeyHtmlMetadataExtractorFactory;
import org.lockss.plugin.simulated.*;

/*
<meta content="Cancer Informatics, Evolutionary Bioinformatics Online, bioinformatics, biomarkers, computational biology, cancer, informatics, computational biology, phylogeny, phylogenetics, evolutionary biology, science news articles, science articles, proteomics, caBIG, biomedical informatics, informatics, proteomics, genomics, biomarkers, pathology, pathology informatics, radiology, radiology informatics, cancer genes, open access review, open access, biology, microarray, Libertas Academica, biological, evidence-bas...rch papers, review articles, scientific journal, science journals, medical journal, biology, journal publisher, biology, disease, journals, peer-reviewed journals, scientific, research papers, review articles, science journals, journal publisher, international science journal, subscribe, libertas academica, peer-reviewed Open Access journals, Cancer Informatics, Evolutionary Bioinformatics Online, open access journals, peer-reviewed journals, scientific, research papers, review articles, original research" \" content=\"keywords">
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

public class TestInternationalUnionOfCrystallographyMetadataExtractor extends LockssTestCase {

  static Logger log = Logger.getLogger(
      TestInternationalUnionOfCrystallographyMetadataExtractor.class.getName());

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = 
      "org.lockss.plugin.internationalunionofcrystallography.InternationalUnionOfCrystallographyPlugin";
  private static String BASE_URL = "http://journals.iucr.org/";
  private static String SCRIPTS_URL = "http://scripts.iucr.org/";
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
    conf.put("scripts_url", SCRIPTS_URL);
    conf.put("year", "2010");
    conf.put("issue", "1");
    conf.put("journal_id", "e");
    return conf;
  }

  // the metadata that should be extracted
  String goodVolume = "66";
  String goodIssue = "1";
  String goodStartPage = "1";
  String goodEndPage = "1";
  String goodDate = "2010-01-01";
  String[] goodAuthors = {
      "Jiao, Z.-W.",
      "Wang, R.-J.",
      "Wang, X.-Q.",
      "Shen, D.-Z.",
      "Shen, G.-Q."};
  String goodArticleTitle = "LaZnB5O10, the first lanthanum zinc borate";
  String goodJournalTitle = "Acta Crystallographica Section E: Structure Reports Online";
  String goodKeywords = "";
  String goodDOI = "10.1107/S1600536809050922";
  String goodISSN = "1600-5368";
  String goodEISSN = "1600-5368";

  // a chunk of html source code from the publisher's site from where the 
  // metadata should be extracted

  String goodContent = 
      "<meta name=\"DC.coverage\" content=\"\"  />\n"
    + "<meta name=\"DC.source\" content=\"urn:issn:" + goodISSN + "\"  />\n"
    + "<meta name=\"DC.rights\" content=\"http://creativecommons.org/licenses/by/2.0/uk\"  />\n"
    + "<meta name=\"DC.creator\" content=\"" + goodAuthors[0] + "\"  />\n"
    + "<meta name=\"DC.creator\" content=\"" + goodAuthors[1] + "\"  />\n"
    + "<meta name=\"DC.creator\" content=\"" + goodAuthors[2] + "\"  />\n"
    + "<meta name=\"DC.creator\" content=\"" + goodAuthors[3] + "\"  />\n"
    + "<meta name=\"DC.creator\" content=\"" + goodAuthors[4] + "\"  />\n"
    + "<meta name=\"DC.date\" content=\"" + goodDate + "\"  />\n"
    + "<meta name=\"DC.copyright\" content=\"http://creativecommons.org/licenses/by/2.0/uk\"  />\n"
    + "<meta name=\"DC.identifier\" content=\"doi:" + goodDOI + "\"  />\n"
    + "<meta name=\"DC.publisher\" content=\"International Union of Crystallography\"  />\n"
    + "<meta name=\"DC.contributor\" content=\"\"  />\n"
    + "<meta name=\"DC.link\" content=\"http://scripts.iucr.org/cgi-bin/paper?br2126\"  />\n"
    + "<meta name=\"DC.teaser\" content=\"\"  />\n"
    + "<meta name=\"DC.language\" content=\"en\"  />\n"
    + "<meta name=\"DC.description\" content=\"Lanthanum zinc penta&#173;borate, LaZnB5O10, was synthesized by flux-supported solid-state reaction. It is a member of the LnMB5O10 (Ln = rare earth ion and M = divalent metal ion) structure type. The crystal shows a three-dimensional structure constructed from two-dimensional {[B5O10]5&#8722;}n layers with the lanthanum (coordination number nine) and zinc (coordination number six) ions filling in the inter&#173;layers.\"  />\n"
    + "<meta name=\"DC.format\" content=\"\"  />\n"
    + "<meta name=\"DC.relation\" content=\"\"  />\n"
    + "<meta name=\"DC.type\" content=\"\"  />\n"
    + "<meta name=\"DC.title\" content=\"" + goodArticleTitle + "\"  />\n"
    + "<meta name=\"DCTERMS.abstract\" content=\"Lanthanum zinc penta&#173;borate, LaZnB5O10, was synthesized by flux-supported solid-state reaction. It is a member of the LnMB5O10 (Ln = rare earth ion and M = divalent metal ion) structure type. The crystal shows a three-dimensional structure constructed from two-dimensional {[B5O10]5&#8722;}n layers with the lanthanum (coordination number nine) and zinc (coordination number six) ions filling in the inter&#173;layers.\"  />\n"
    + "<meta name=\"prism.number\" content=\"1\"  />\n"
    + "<meta name=\"prism.volume\" content=\"" + goodVolume + "\"  />\n"
    + "<meta name=\"prism.publicationDate\" content=\"" + goodDate + "\"  />\n"
    + "<meta name=\"prism.publicationName\" content=\"Acta Crystallographica Section E: Structure Reports Online\"  />\n"
    + "<meta name=\"prism.copyright\" content=\"http://creativecommons.org/licenses/by/2.0/uk\"  />\n"
    + "<meta name=\"prism.issn\" content=\"" + goodISSN + "\"  />\n"
    + "<meta name=\"prism.section\" content=\"inorganic compounds\"  />\n"
    + "<meta name=\"prism.startingPage\" content=\"" + goodStartPage + "\"  />\n"
    + "<meta name=\"prism.rightsAgent\" content=\"med@iucr.org\"  />\n"
    + "<meta name=\"prism.endingPage\" content=\"" + goodEndPage + "\"  />\n"
    + "<meta name=\"prism.eissn\" content=\"" + goodEISSN + "\"  />\n"
    + "<meta name=\"keywords\" lang=\"en\" content=\"\"  />\n"
    + "<meta name=\"copyright\" content=\"http://creativecommons.org/licenses/by/2.0/uk\"  />\n"
    + "<meta name=\"ROBOTS\" content=\"NOARCHIVE\"  />\n"
    + "<meta name=\"citation_fulltext_url\" content=\"http://journals.iucr.org/e/issues/2010/01/00/br2126/\"  />\n"
    + "<meta name=\"citation_lastpage\" content=\"" + goodEndPage + "\"  />\n"
    + "<meta name=\"citation_volume\" content=\"" + goodVolume + "\"  />\n"
    + "<meta name=\"citation_journal_abbrev\" content=\"Acta Cryst E\"  />\n"
    + "<meta name=\"citation_journal_abbrev\" content=\"Acta Cryst Sect E\"  />\n"
    + "<meta name=\"citation_journal_abbrev\" content=\"Acta Crystallogr E\"  />\n"
    + "<meta name=\"citation_journal_abbrev\" content=\"Acta Crystallogr Sect E\"  />\n"
    + "<meta name=\"citation_journal_abbrev\" content=\"Acta Crystallogr E Struct Rep Online\"  />\n"
    + "<meta name=\"citation_journal_abbrev\" content=\"Acta Crystallogr Sect E Struct Rep Online\"  />\n"
    + "<meta name=\"citation_issue\" content=\"" + goodIssue + "\"  />\n"
    + "<meta name=\"citation_date\" content=\"" + goodDate + "\"  />\n"
    + "<meta name=\"citation_firstpage\" content=\"" + goodStartPage + "\"  />\n"
    + "<meta name=\"citation_title\" content=\"" + goodArticleTitle + "\"  />\n"
    + "<meta name=\"citation_journal_title\" content=\"" + goodJournalTitle + "\"  />\n"
    + "<meta name=\"citation_author\" content=\"" + goodAuthors[0] + "\"  />\n"
    + "<meta name=\"citation_author_institution\" content=\"Department of Chemistry, Tsinghua University, Beijing 100084, People's Republic of China\"  />\n"
    + "<meta name=\"citation_author\" content=\"" + goodAuthors[1] + "\"  />\n"
    + "<meta name=\"citation_author_institution\" content=\"Department of Chemistry, Tsinghua University, Beijing 100084, People's Republic of China\"  />\n"
    + "<meta name=\"citation_author\" content=\"" + goodAuthors[2] + "\"  />\n"
    + "<meta name=\"citation_author_institution\" content=\"Department of Chemistry, Tsinghua University, Beijing 100084, People's Republic of China\"  />\n"
    + "<meta name=\"citation_author\" content=\"" + goodAuthors[3] + "\"  />\n"
    + "<meta name=\"citation_author_institution\" content=\"Department of Chemistry, Tsinghua University, Beijing 100084, People's Republic of China\"  />\n"
    + "<meta name=\"citation_author_email\" content=\"jzwzwj@163.com\"  />\n"
    + "<meta name=\"citation_author\" content=\"" + goodAuthors[4] + "\"  />\n"
    + "<meta name=\"citation_author_institution\" content=\"Department of Chemistry, Tsinghua University, Beijing 100084, People's Republic of China\"  />\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=Abdullaev G. K.; citation_author=Mamedov K. S.; citation_author=Dzhafarov G. G.; citation_author=Aliev O. A.; citation_year=1980; citation_journal_title=Zh. Neorg. Khim.; citation_volume=25; citation_firstpage=364; citation_lastpage=367; \"/>\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=Becker P.; citation_year=1998; citation_journal_title=Adv. Mater.; citation_volume=10; citation_firstpage=979; citation_lastpage=991; \"/>\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=Bernadette S.; citation_author=Marcus V.; citation_author=Claude F.; citation_year=1980; citation_journal_title=J. Solid State Chem.; citation_volume=34; citation_firstpage=271; citation_lastpage=277; \"/>\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=Brese N. E.; citation_author=O'Keeffe M.; citation_year=1991; citation_journal_title=Acta Cryst. B; citation_volume=47; citation_firstpage=192; citation_lastpage=197; \"/>\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=Campa J. A.; citation_author=Cascales C.; citation_author=Gutierrez Puebla E.; citation_author=Mira J.; citation_author=Monge M. A.; citation_author=Rasines I.; citation_author=Ruvas J.; citation_author=Ruiz Valero C.; citation_year=1995; citation_journal_title=J. Alloys Compd.; citation_volume=225; citation_firstpage=225; citation_lastpage=229; \"/>\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=North A. C. T.; citation_author=Phillips D. C.; citation_author=Mathews F. S.; citation_year=1968; citation_journal_title=Acta Cryst. A; citation_volume=24; citation_firstpage=351; citation_lastpage=359; \"/>\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=Sheldrick G. M.; citation_year=2008; citation_journal_title=Acta Cryst. A; citation_volume=64; citation_firstpage=112; citation_lastpage=122; \"/>\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=Thakare D. S.; citation_author=Omanwar S. K.; citation_author=Muthal P. L.; citation_author=Dhopte S. M.; citation_author=Kondawar V. K.; citation_author=Mohari S. V.; citation_year=2004; citation_journal_title=Phys. Status Solidi; citation_volume=201; citation_firstpage=574; citation_lastpage=581; \"/>\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=Yavetskiy R. P.; citation_author=Tolmachev A. V.; citation_author=Dolzhenkova E. F.; citation_author=Baumer V. N.; citation_year=2007; citation_journal_title=J. Alloys Compd.; citation_volume=429; citation_firstpage=77; citation_lastpage=81; \"/>\n"
    + "<meta name=\"citation_reference\" content=\"citation_author=Ye Q.; citation_author=Chai B. H. T.; citation_year=1999; citation_journal_title=J. Cryst. Growth; citation_volume=197; citation_firstpage=228; citation_lastpage=235; \"/>\n"
    + "<meta name=\"citation_publisher\" content=\"International Union of Crystallography\"  />\n"
    + "<meta name=\"citation_doi\" content=\"" + goodDOI + "\"  />\n"
    + "<meta name=\"citation_language\" content=\"en\"  />\n"
    + "<meta name=\"citation_abstract_html_url\" content=\"http://scripts.iucr.org/cgi-bin/paper?br2126\"  />\n"
    + "<meta name=\"citation_pdf_url\" content=\"http://journals.iucr.org/e/issues/2010/01/00/br2126/br2126.pdf\"  />\n"
    + "<meta name=\"citation_issn\" content=\"" + goodISSN + "\"  />\n"
    + "<meta name=\"citation_keywords\" content=\"" + goodKeywords + "\"  />\n";

  /**
   * Method that creates a simulated Cached URL from the source code provided by 
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the NatureHtmlMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://journals.iucr.org/e/issues/2010/01/00/br2126/index.html";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = 
        new InternationalUnionOfCrystallographyHtmlMetadataExtractorFactory
            .InternationalUnionOfCrystallographyMetadataExtractor();
    FileMetadataListExtractor mle =
      new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
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
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    assertEquals(goodEISSN, md.get(MetadataField.FIELD_EISSN));
    assertEquals("[" + goodKeywords + "]", md.getList(MetadataField.FIELD_KEYWORDS).toString());
  }

  // a chunk of html source code from where the 
  // InternationaUnionOfCrystallographyHtmlMetadataExtractorFactory 
  // should NOT be able to extract metadata
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
    FileMetadataExtractor me = new MaffeyHtmlMetadataExtractorFactory.MaffeyHtmlMetadataExtractor();
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
