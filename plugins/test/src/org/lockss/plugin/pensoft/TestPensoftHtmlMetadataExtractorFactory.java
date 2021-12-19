/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.pensoft;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.pensoft.PensoftHtmlMetadataExtractorFactory;

/*
 * Metadata on abstract page:
 * http://www.pensoft.net/journals/ZZZ/article/1548/abstract/Article-title-for-this-Example
<meta name="Allow-search" content="yes">
<meta name="Audience" content="all">
<meta name="Rating" content="all">
<meta name="Voluntary content rating" content="all">
<meta name="resource-type" content="document">
<meta name="revisit-after" content="1 day">
<meta name="distribution" content="global">
<meta name="robots" content="index, follow">
<meta name="keywords" content="woodborers; Palearctic; Oriental; Hyperxiphia">
<meta name="description" content="Five species of Something are recognized, E. leucopoda Takeuchi, 1938, from Japan, E. potanini (Jakovlev, 1891) from Japan, Russia, Korea, and China, E. pseud">
<meta name="title" content="Review of Article title for this Example"/><meta name="citation_pdf_url" content="http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=3070"/><meta name="citation_xml_url" content="http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=3069"/><meta name="citation_fulltext_html_url" content="http://www.pensoft.net/journals/ZZZ/article/1548/Article-title-for-this-Example"/>
<meta name="citation_abstract_html_url" content="http://www.pensoft.net/journals/ZZZ/article/1548/abstract/Article-title-for-this-Example"/>
<meta name="dc.title" content="Review of Article title for this Example" />
<meta name="dc.creator" content="Smith Davids" />
<meta name="dc.contributor" content="Smith Davids" />
<meta name="dc.creator" content="Abcde  Stuvwxy" />
<meta name="dc.contributor" content="Abcde  Stuvwxy" />
<meta name="dc.type" content="Research Article" />
<meta name="dc.source" content="Journal of ZZZ Research 2011 23: 1" />
<meta name="dc.date" content="2011-10-21" />
<meta name="dc.identifier" content="10.3897/ZZZ.23.1548" />
<meta name="dc.publisher" content="Pensoft Publishers" />
<meta name="dc.rights" content="http://creativecommons.org/licenses/by/3.0/" />
<meta name="dc.format" content="text/html" />
<meta name="dc.language" content="en" />

<meta name="prism.publicationName" content="Journal of ZZZ Research" />
<meta name="prism.issn" content="1314-2607" />
<meta name="prism.publicationDate" content="2011-10-21" />
<meta name="prism.volume" content="23" />

<meta name="prism.doi" content="10.3897/ZZZ.23.1548" />
<meta name="prism.section" content="Research Article" />
<meta name="prism.startingPage" content="1" />
<meta name="prism.endingPage" content="22" />
<meta name="prism.copyright" content="2011 Smith Davids, Abcde  Stuvwxy" />
<meta name="prism.rightsAgent" content="Journal of ZZZ Research@pensoft.net" />

<meta name="eprints.title" content="Review of Article title for this Example" />
<meta name="eprints.creators_name" content="Davids, Smith " /> <meta name="eprints.creators_name" content="Stuvwxy, Abcde " />
<meta name="eprints.type" content="Research Article" />
<meta name="eprints.datestamp" content="2011-10-21" />
<meta name="eprints.ispublished" content="pub" />
<meta name="eprints.date" content="2011" />
<meta name="eprints.date_type" content="published" />
<meta name="eprints.publication" content="Pensoft Publishers" />
<meta name="eprints.volume" content="23" />
<meta name="eprints.pagerange" content="1-22" />

<meta name="citation_journal_title" content="Journal of ZZZ Research" />
<meta name="citation_publisher" content="Pensoft Publishers" />
<meta name="citation_author" content="Smith Davids" /> <meta name="citation_author" content="Abcde  Stuvwxy" />
<meta name="citation_title" content="Review of Article title for this Example" />
<meta name="citation_volume" content="23" />

<meta name="citation_firstpage" content="1" />
<meta name="citation_lastpage" content="22" />
<meta name="citation_doi" content="10.3897/ZZZ.23.1548" />
<meta name="citation_issn" content="1314-2607" />
<meta name="citation_date" content="2011/10/21" />

 */

public class TestPensoftHtmlMetadataExtractorFactory extends LockssTestCase {

  static Logger log = Logger.getLogger("TestPensoftHtmlMetadataExtractorFactory" +
  		"");

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

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

    mau = new MockArchivalUnit();
    mau.setConfiguration(pensoftAuConfig());  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configuration method.
   * @return
   */
  Configuration pensoftAuConfig() {
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
  String[] goodAuthors = {"María Poggio", "Julia Bressa", "Papeschi, Alba"};
  String goodArticleTitle = "Male meiosis, heterochromatin characterization and chromosomal location of rDNA in Microtomus lunifer (Berg, 1900) (Hemiptera: Reduviidae: Hammacerinae)";
  String goodJournalTitle = "Analytical Comparative Cytogenetics";
  String goodKeywords = "meiosis, heterochromatin, rDNA";
  String goodMetaPDF = "http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=1234";
  String goodCrawlPDF = "http://www.pensoft.net/inc/journals/download.php?fileId=1234&fileTable=J_GALLEYS";
  String goodXML = "http://www.pensoft.net/inc/journals/download.php?fileTable=J_GALLEYS&fileId=World";
  String goodHTML = "http://www.pensoft.net/journals/helloworld/article/2065/Hello-world-as-a-title";
  String goodABS = "http://www.pensoft.net/journals/helloworld/article/2065/abstract/Hello-world-as-a-title";


  final String crawlRegExp = "(http://www.pensoft.net/inc/journals/download.php\\?)(fileId=[\\d]+)(\\&)(fileTable=J_GALLEYS)$";
  final String metadataRegExp = "(http://www.pensoft.net/inc/journals/download.php\\?)(fileTable=J_GALLEYS)(\\&)(fileId=[\\d]+)$";

 public void testRegExp() throws Exception {
    Pattern crawlPattern = Pattern.compile(crawlRegExp, Pattern.CASE_INSENSITIVE);
    Pattern metadataPattern = Pattern.compile(metadataRegExp, Pattern.CASE_INSENSITIVE);
    String crawlPdf = null;
    Matcher mat = metadataPattern.matcher(goodMetaPDF);
      if (mat.matches()) {
        crawlPdf = mat.replaceFirst("$1$4$3$2");
        mat = crawlPattern.matcher(crawlPdf);
        assertTrue(mat.matches());
      }
      assertEquals(crawlPdf, goodCrawlPDF);

 }


  // a chunk of metadata html source code from where the
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
    + "<meta content=\"" + goodDate + "\" name=\"citation_date\">\n"
    + "<meta name=\"citation_pdf_url\" content=\"" + goodMetaPDF + "\"/>"
    + "<meta name=\"citation_xml_url\" content=\""+ goodXML + "\"/>"
    + "<meta name=\"citation_fulltext_html_url\" content=\"" + goodHTML + "\"/>"
    + "<meta name=\"citation_abstract_html_url\" content=\"" + goodABS + "\"/>"
   ;

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
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new PensoftHtmlMetadataExtractorFactory.PensoftHtmlMetadataExtractor();
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
    //assertEquals("[" + goodKeywords + "]", md.getList(MetadataField.FIELD_KEYWORDS).toString());
    assertEquals(goodMetaPDF, md.getRaw("citation_pdf_url"));
    assertEquals(goodXML, md.getRaw("citation_xml_url"));
    assertEquals(goodHTML, md.getRaw("citation_fulltext_html_url"));
    assertEquals(goodABS, md.getRaw("citation_abstract_html_url"));

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
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    cu.setContent(badContent);
    cu.setContentSize(badContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new PensoftHtmlMetadataExtractorFactory.PensoftHtmlMetadataExtractor();
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

}
