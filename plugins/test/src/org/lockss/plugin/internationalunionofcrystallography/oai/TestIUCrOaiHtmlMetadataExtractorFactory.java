/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.internationalunionofcrystallography.oai;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.maffey.MaffeyHtmlMetadataExtractorFactory;
import org.lockss.plugin.simulated.*;

/* Snippet of the meta data from https://journals.iucr.org/e/issues/2015/02/00/wm5120/index.html on 02/03/2021
<meta name="citation_fulltext_url" content="https://journals.iucr.org/e/issues/2015/02/00/wm5120/" />
<meta name="citation_lastpage" content="m49" />
<meta name="citation_volume" content="71" />
<meta name="citation_issue" content="2" />
<meta name="citation_firstpage" content="m48" />
<meta name="citation_date" content="2015-02-01" />
<meta name="citation_title" content="Crystal structure of bis&#173;(tetra&#173;phenyl&#173;phospho&#173;nium) bis&#173;(cyanido-&#954;C)(29H,31H-tetra&#173;benzo[b,g,l,q]porphinato-&#954;4N29,N30,N31,N32)ferrate(II) acetone disolvate" />
<meta name="citation_journal_title" content="Acta Crystallographica Section E: Crystallographic Communications" />
<meta name="citation_author" content="Nishi, M." />
<meta name="citation_author_institution" content="Department of Chemistry, Kumamoto University, Kurokami 2-39-1, Chuo-ku, Kumamoto 860-8555, Japan" />
<meta name="citation_author" content="Matsuda, M." />
<meta name="citation_author" content="Hoshino, N." />
<meta name="citation_author" content="Akutagawa, T." />
<meta name="citation_doi" content="10.1107/S2056989015001735" />
<meta name="citation_language" content="en" />
<meta name="citation_abstract_html_url" content="https://scripts.iucr.org/cgi-bin/paper?wm5120" />
<meta name="citation_pdf_url" content="https://journals.iucr.org/e/issues/2015/02/00/wm5120/wm5120.pdf" />
<meta name="citation_issn" content="2056-9890" />
*/

/*
  tagMap.put("citation_author", MetadataField.FIELD_AUTHOR);
  tagMap.put("citation_date", MetadataField.FIELD_DATE);
  tagMap.put("citation_title", MetadataField.FIELD_ARTICLE_TITLE);
  tagMap.put("citation_journal_title", MetadataField. FIELD_PUBLICATION_TITLE);
  tagMap.put("citation_volume", MetadataField.FIELD_VOLUME);
  tagMap.put("citation_issue", MetadataField.FIELD_ISSUE);
  tagMap.put("citation_firstpage", MetadataField.FIELD_START_PAGE);
  tagMap.put("citation_lastpage", MetadataField.FIELD_END_PAGE);
  tagMap.put("citation_doi", MetadataField.FIELD_DOI);
  tagMap.put("citation_issn", MetadataField.FIELD_ISSN);
tagMap.put("citation_isbn", MetadataField.FIELD_ISBN);
  tagMap.put("citation_pdf_url", MetadataField.FIELD_ACCESS_URL);
  tagMap.put("citation_language", MetadataField.FIELD_LANGUAGE);
  tagMap.put("citation_abstract_html_url", MetadataField.FIELD_ABSTRACT);
*/

public class TestIUCrOaiHtmlMetadataExtractorFactory extends LockssTestCase {
  static Logger log = Logger.getLogger(
      TestIUCrOaiHtmlMetadataExtractorFactory.class.getName());

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau;
  private static String PLUGIN_NAME =
      "org.lockss.plugin.internationalunionofcrystallography.oai.IUCrOaiPlugin";
  private static String BASE_URL = "https://journals.iucr.org/";
  private static String SCRIPTS_URL = "https://scripts.iucr.org/";
  private static String BASE_URL_HTTP = "http://journals.iucr.org/";
  private static String SCRIPTS_URL_HTTP = "http://scripts.iucr.org/";
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

    sau = PluginTestUtil.createAndStartSimAu(TestIUCrOaiHtmlMetadataExtractorFactory.MySimulatedPlugin.class,	simAuConfig(tempDirPath));
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
    conf.put("script_url", SCRIPTS_URL);
    conf.put("au_oai_set", "actacryste");
    conf.put("au_oai_date", "2015-02");
    conf.put("year", "2015");
    conf.put("issue", "2");
    return conf;
  }

  // the metadata that should be extracted
  String goodVolume = "71";
  String goodIssue = "2";
  String goodStartPage = "m48";
  String goodEndPage = "m49";
  String goodDate = "2015-02-01";
  String[] goodAuthors = {
      "Nishi, M.",
      "Matsuda,M.",
      "Hoshino, N.",
      "Akutagawa, T."
  };
  String goodArticleTitle = "Crystal structure of bis-(tetra-phenyl-phospho-nium) bis-(cyanido-κC)(29H,31H-tetra-benzo[b,g,l,q]porphinato-κ4N29,N30,N31,N32)ferrate(II) acetone disolvate";
  String goodJournalTitle = "Acta Crystallographica Section E: Crystallographic Communications";
  String goodDOI = "10.1107/S2056989015001735";
  String goodISSN = "2056-9890";
  String pdfPath = "e/issues/2010/01/00/br2126/br2126.pdf";
  String goodPdfUrl = BASE_URL + pdfPath;
  String abstractPath = "cgi-bin/paper?br2126";
  String goodAbstractUrl = SCRIPTS_URL + abstractPath;

  // a chunk of html source code from the publisher's site from where the
  // metadata should be extracted
  String goodContent = "<meta name=\"citation_fulltext_url\" content=\"https://journals.iucr.org/e/issues/2015/02/00/wm5120/\" />\n"
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
    + "<meta name=\"citation_author_institution\" content=\"Department of Chemistry, Kumamoto University, Kurokami 2-39-1, Chuo-ku, Kumamoto 860-8555, Japan\" />\n"
    + "<meta name=\"citation_author\" content=\"" + goodAuthors[1] + "\"  />\n"
    + "<meta name=\"citation_author_institution\" content=\"Department of Chemistry, Kumamoto University, Kurokami 2-39-1, Chuo-ku, Kumamoto 860-8555, Japan\" />\n"
    + "<meta name=\"citation_author\" content=\"" + goodAuthors[2] + "\"  />\n"
    + "<meta name=\"citation_author_institution\" content=\"Department of Chemistry, Kumamoto University, Kurokami 2-39-1, Chuo-ku, Kumamoto 860-8555, Japan\" />\n"
    + "<meta name=\"citation_author\" content=\"" + goodAuthors[3] + "\"  />\n"
    + "<meta name=\"citation_doi\" content=\"" + goodDOI + "\"  />\n"
    + "<meta name=\"citation_language\" content=\"en\"  />\n"
    + "<meta name=\"citation_abstract_html_url\" content=\"" + SCRIPTS_URL_HTTP + abstractPath + "\"  />\n"
    + "<meta name=\"citation_pdf_url\" content=\"" + BASE_URL_HTTP + pdfPath + "\"  />\n"
    + "<meta name=\"citation_issn\" content=\"" + goodISSN + "\"  />\n";


  /**
   * Method that creates a simulated Cached URL from the source code provided by
   * the goodContent String. It then asserts that the metadata extracted, by using
   * the NatureHtmlMetadataExtractorFactory, match the metadata in the source code.
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "https://journals.iucr.org/e/issues/2015/02/00/wm5120/index.html";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me =
        new IUCrOaiHtmlMetadataExtractorFactory
            .IUCrOaiHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
        new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodVolume, md.get(MetadataField.FIELD_VOLUME));
    assertEquals(goodIssue, md.get(MetadataField.FIELD_ISSUE));
    assertEquals(goodStartPage, md.get(MetadataField.FIELD_START_PAGE));
    //assertEquals(Arrays.asList(goodAuthors).toString(), md.getList(MetadataField.FIELD_AUTHOR).toString());
    assertEquals(goodArticleTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodJournalTitle, md.get(MetadataField.FIELD_JOURNAL_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(goodEndPage, md.get(MetadataField.FIELD_END_PAGE));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
    assertEquals(goodISSN, md.get(MetadataField.FIELD_ISSN));
    log.info(goodPdfUrl);
    log.info(md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(goodPdfUrl, md.get(MetadataField.FIELD_ACCESS_URL));
    assertEquals(goodAbstractUrl, md.get(MetadataField.FIELD_ABSTRACT));
  }

  // a chunk of html source code that
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
      return new TestIUCrOaiHtmlMetadataExtractorFactory.MySimulatedContentGenerator(fileRoot);
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
