/*
 * $Id: 
 */

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

package org.lockss.plugin.pensoft;

import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.pensoft.PensoftHtmlMetadataExtractorFactory;
import org.lockss.plugin.simulated.*;

/*

<meta name="citation_abstract_html_url" content="http://www.pensoft.net/journals/compcytogen/article/1143/abstract/male-meiosis-heterochromatin-characterization-and-chromosomal-location-of-rdna-in-microtomus-lunifer-berg-1900-hemiptera"/>
<meta name="dc.title" content="Male meiosis, heterochromatin characterization and chromosomal location of rDNA in Microtomus lunifer (Berg, 1900) (Hemiptera: Reduviidae: Hammacerinae)" />
<meta name="dc.creator" content="Mar’a Poggio" />
<meta name="dc.contributor" content="Mar’a Poggio" /><meta name="dc.creator" content="Mar’a Bressa" />
<meta name="dc.contributor" content="Mar’a Bressa" /><meta name="dc.creator" content="Alba Papeschi" />
<meta name="dc.contributor" content="Alba Papeschi" />
<meta name="dc.type" content="Research Articles" />
<meta name="dc.source" content="Comparative Cytogenetics 2011 5: 1" />
<meta name="dc.date" content="2011-05-05" />
<meta name="dc.identifier" content="10.3897/compcytogen.v5i1.1143" />
<meta name="dc.publisher" content="Pensoft Publishers" />
<meta name="dc.rights" content="http://creativecommons.org/licenses/by/3.0/" />
<meta name="dc.format" content="text/html" />
<meta name="dc.language" content="en" />

<meta name="prism.publicationName" content="Comparative Cytogenetics" />
<meta name="prism.issn" content="1993-078X" />
<meta name="prism.publicationDate" content="2011-05-05" /> 
<meta name="prism.volume" content="5" />
<meta name="prism.number" content="1" />
<meta name="prism.doi" content="10.3897/compcytogen.v5i1.1143" />
<meta name="prism.section" content="Research Articles" />
<meta name="prism.startingPage" content="1" />
<meta name="prism.endingPage" content="22" />
<meta name="prism.copyright" content="2011 Mar’a Poggio, Mar’a Bressa, Alba Papeschi" />
<meta name="prism.rightsAgent" content="Comparative Cytogenetics@pensoft.net" />

<meta name="eprints.title" content="Male meiosis, heterochromatin characterization and chromosomal location of rDNA in Microtomus lunifer (Berg, 1900) (Hemiptera: Reduviidae: Hammacerinae)" />
<meta name="eprints.creators_name" content="Poggio, Mar’a " /> <meta name="eprints.creators_name" content="Bressa, Mar’a " /> <meta name="eprints.creators_name" content="Papeschi, Alba " /> 
<meta name="eprints.type" content="Research Articles" />
<meta name="eprints.datestamp" content="2011-05-05" />
<meta name="eprints.ispublished" content="pub" />
<meta name="eprints.date" content="2011" />
<meta name="eprints.date_type" content="published" />
<meta name="eprints.publication" content="Pensoft Publishers" />
<meta name="eprints.volume" content="5" />
<meta name="eprints.pagerange" content="1-22" />

<meta name="citation_journal_title" content="Comparative Cytogenetics" />
<meta name="citation_publisher" content="Pensoft Publishers" />
<meta name="citation_author" content="Mar’a Poggio" /> <meta name="citation_author" content="Julia Bressa" /> <meta name="citation_author" content="Alba Papeschi" /> 
<meta name="citation_title" content="Male meiosis, heterochromatin characterization and chromosomal location of rDNA in Microtomus lunifer (Berg, 1900) (Hemiptera: Reduviidae: Hammacerinae)" />
<meta name="citation_volume" content="5" />
<meta name="citation_issue" content="1" />
<meta name="citation_firstpage" content="1" />
<meta name="citation_lastpage" content="22" />
<meta name="citation_doi" content="10.3897/compcytogen.v5i1.1143" />
<meta name="citation_issn" content="1993-078X" />
<meta name="citation_date" content="2011/05/05" />

*/

public class TestPensoftHtmlMetadataExtractorFactory extends LockssTestCase {

  static Logger log = Logger.getLogger("TestPensoftHtmlMetadataExtractorFactory" +
  		"");

  private MockLockssDaemon theDaemon;
  private SimulatedArchivalUnit sau; // Simulated AU to generate content
  private ArchivalUnit bau;
  private static String PLUGIN_NAME = "org.lockss.plugin.pensoft.PensoftPlugin";
  private static String BASE_URL = "http://www.examplePress.com/";
  private static String SIM_ROOT = BASE_URL + "simroot/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();

    sau = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin.class,   simAuConfig(tempDirPath));
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
    conf.put("fileTypes",""     + (SimulatedContentGenerator.FILE_TYPE_HTML + SimulatedContentGenerator.FILE_TYPE_HTML));
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
    conf.put("journal_name", "abc");
    conf.put("year", "2008");
    return conf;
  }

  // the metadata that should be extracted
  String goodVolume = "4";
  String goodIssue = "1";
  String goodStartPage = "1";
  String goodEndPage = "22";
  String goodDate = "2005/05/05";
  String[] goodAuthors = {"Mar’a Poggio", "Julia Bressa", "Papeschi, Alba"};
  String goodArticleTitle = "Male meiosis, heterochromatin characterization and chromosomal location of rDNA in Microtomus lunifer (Berg, 1900) (Hemiptera: Reduviidae: Hammacerinae)";
  String goodJournalTitle = "Analytical Comparative Cytogenetics";
  String goodKeywords = "meiosis, heterochromatin, rDNA";

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
    + "<meta content=\"en\" name=\"citation_language\">\n"
    + "<meta content=\"" + goodKeywords + "\"name=\"keywords\">\n"
    + "<meta content=\"" + goodDate + "\" name=\"citation_date\">\n";       
   
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
   * the PensoftHtmlMetadataExtractorFactory, match the metadata in the source code. 
   * @throws Exception
   */
  public void testExtractFromGoodContent() throws Exception {
    String url = "http://www.example.com/vol1/issue2/art3/";
    MockCachedUrl cu = new MockCachedUrl(url, bau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new PensoftHtmlMetadataExtractorFactory.PensoftHtmlMetadataExtractor();
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

  // a chunk of html source code from where the PensoftHtmlMetadataExtractorFactory should NOT be able to extract metadata
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
    FileMetadataExtractor me = new PensoftHtmlMetadataExtractorFactory.PensoftHtmlMetadataExtractor();
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
  public static class MySimulatedContentGenerator extends       SimulatedContentGenerator {
    protected MySimulatedContentGenerator(String fileRoot) {
      super(fileRoot);
    }

    public String getHtmlFileContent(String filename, int fileNum, int depth, int branchNum, boolean isAbnormal) {
                        
      String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
                        
      file_content += "  <meta name=\"lockss.filenum\" content=\""+ fileNum + "\">\n";
      file_content += "  <meta name=\"lockss.depth\" content=\"" + depth + "\">\n";
      file_content += "  <meta name=\"lockss.branchnum\" content=\"" + branchNum + "\">\n";                     

      file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
      file_content += "\n</BODY></HTML>";
      logger.debug2("MySimulatedContentGenerator.getHtmlFileContent: "
                    + file_content);

      return file_content;
    }
  }
}
