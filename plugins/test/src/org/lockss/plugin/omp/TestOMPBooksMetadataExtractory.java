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

package org.lockss.plugin.omp;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.TitleConfig;
import org.lockss.extractor.*;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.clockss.mdpi.TestMDPIXmlMetadataExtractor;
import org.lockss.plugin.emerald.EmeraldHtmlMetadataExtractorFactory;
import org.lockss.plugin.illiesia.IlliesiaMetadataExtractorFactory;
import org.lockss.plugin.nature.NatureHtmlMetadataExtractorFactory;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockArchivalUnit;
import org.lockss.test.MockCachedUrl;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One of the articles used to get the html source for this plugin is:
 * http://www.emeraldinsight.com/journals.htm?issn=0961-5539&volume=14&issue=5&articleid=1455115&show=html&view=printarticle
 */
public class TestOMPBooksMetadataExtractory extends LockssTestCase {

  private static final Logger log = Logger.getLogger(TestOMPBooksMetadataExtractory.class);

  private MockLockssDaemon theDaemon;
  private MockArchivalUnit mau;

  private static String PLUGIN_NAME = "org.lockss.plugin.omp.OMPBooksPlugin";
  private static String BASE_URL = "http://www.omp.com/";

  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace(); // you need this to have startService work properly...

    theDaemon = getMockLockssDaemon();
    mau = new MockArchivalUnit();

    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    mau.setConfiguration(auConfig());

  }

  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }

  /**
   * Configuration method.
   * @return
   */
  Configuration auConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("publisher_id", "edinburgh-diamond");
    conf.put("resource_id", "ed-9781912669325");
    return conf;
  }

  String goodContent = "<meta name=\"generator\" content=\"Open Monograph Press 3.3.0.14\">\n" +
          "  <meta name=\"gs_meta_revision\" content=\"1.1\"/>\n" +
          "  <meta name=\"citation_title\" content=\"We Have Great Stuff: Colouring Book Volume 1\"/>\n" +
          "  <meta name=\"citation_language\" content=\"en\"/>\n" +
          "  <meta name=\"citation_publication_date\" content=\"2019-04-01\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Stewart Lamb Cromar\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Jackie Aim\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Beth Rossi\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Laura Beattie\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Chinyere Herbert\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Tracey McShane\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Marta Christiansen\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Kirsty McNab\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Sarah Thomas\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Qi Li\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Asthana Devika\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Emily Tanner\"/>\n" +
          "  <meta name=\"citation_author\" content=\"Crystal Check\"/>\n" +
          "  <meta name=\"citation_abstract\" xml:lang=\"en\" content=\"The University of Edinburgh as well as having great students and staff, has great stuff. Since its very beginnings, the University has collected books, art, archives, manuscripts, musical instruments and objects to inspire its community in learning, teaching and research.\n" +
          "  One of the world’s great collections has been built up over hundreds of years and constantly surprises those who come to view and enjoy the items.\n" +
          "  The illustrations in this book are inspired by items and images within the collections and were collated by students during the Festival of Creative Learning Week 2019, and by staff in the Information Services Group.\n" +
          "  We hope you enjoy and become curious to find out more.\"/>\n" +
          "  <meta name=\"citation_doi\" content=\"10.2218/ED.9781912669325\"/>\n" +
          "  <meta name=\"citation_keywords\" xml:lang=\"en\" content=\"Colouring book\"/>\n" +
          "  <meta name=\"citation_keywords\" xml:lang=\"en\" content=\"University of Edinburgh\"/>\n" +
          "  <meta name=\"citation_keywords\" xml:lang=\"en\" content=\"Edinburgh\"/>\n" +
          "  <meta name=\"citation_pdf_url\" content=\"//books.ed.ac.uk/edinburgh-diamond/catalog/download/7/13/379\"/>\n" +
          "  <meta name=\"citation_publisher\" content=\"Edinburgh Diamond | Books\"/>\n" +
          "  <link rel=\"schema.DC\" href=\"http://purl.org/dc/elements/1.1/\" />\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Stewart Lamb Cromar\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Jackie Aim\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Beth Rossi\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Laura Beattie\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Chinyere Herbert\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Tracey McShane\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Marta Christiansen\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Kirsty McNab\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Sarah Thomas\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Qi Li\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Asthana Devika\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Emily Tanner\"/>\n" +
          "  <meta name=\"DC.Creator.PersonalName\" content=\"Crystal Check\"/>\n" +
          "  <meta name=\"DC.Date.created\" scheme=\"ISO8601\" content=\"2019-04-01\"/>\n" +
          "  <meta name=\"DC.Date.dateSubmitted\" scheme=\"ISO8601\" content=\"2022-12-14\"/>\n" +
          "  <meta name=\"DC.Date.modified\" scheme=\"ISO8601\" content=\"2022-12-16\"/>\n" +
          "  <meta name=\"DC.Description\" xml:lang=\"en\" content=\"The University of Edinburgh as well as having great students and staff, has great stuff. Since its very beginnings, the University has collected books, art, archives, manuscripts, musical instruments and objects to inspire its community in learning, teaching and research.\n" +
          "  One of the world’s great collections has been built up over hundreds of years and constantly surprises those who come to view and enjoy the items.\n" +
          "  The illustrations in this book are inspired by items and images within the collections and were collated by students during the Festival of Creative Learning Week 2019, and by staff in the Information Services Group.\n" +
          "  We hope you enjoy and become curious to find out more.\"/>\n" +
          "  <meta name=\"DC.Identifier\" content=\"ed-9781912669325\"/>\n" +
          "  <meta name=\"DC.Identifier.DOI\" content=\"10.2218/ED.9781912669325\"/>\n" +
          "  <meta name=\"DC.Identifier.URI\" content=\"//books.ed.ac.uk/edinburgh-diamond/catalog/book/ed-9781912669325\"/>\n" +
          "  <meta name=\"DC.Language\" scheme=\"ISO639-1\" content=\"en\"/>\n" +
          "  <meta name=\"DC.Rights\" content=\"Copyright (c) 2022 Stewart Lamb Cromar; Jackie Aim, Beth Rossi, Laura Beattie, Chinyere Herbert, Tracey McShane, Marta Christiansen, Kirsty McNab, Sarah Thomas (Illustrator); Qi Li; Asthana Devika, Emily Tanner, Crystal Check (Illustrator)\"/>\n" +
          "  <meta name=\"DC.Rights\" content=\"https://creativecommons.org/licenses/by/4.0\"/>\n" +
          "  <meta name=\"DC.Source\" content=\"Edinburgh Diamond | Books\"/>\n" +
          "  <meta name=\"DC.Source.URI\" content=\"//books.ed.ac.uk/edinburgh-diamond\"/>\n" +
          "  <meta name=\"DC.Subject\" xml:lang=\"en\" content=\"Colouring book\"/>\n" +
          "  <meta name=\"DC.Subject\" xml:lang=\"en\" content=\"University of Edinburgh\"/>\n" +
          "  <meta name=\"DC.Subject\" xml:lang=\"en\" content=\"Edinburgh\"/>\n" +
          "  <meta name=\"DC.Title\" content=\"We Have Great Stuff: Colouring Book Volume 1\"/>\n" +
          "  <meta name=\"DC.Type\" content=\"Text.Book\"/>";

  public void testExtractFromGoodContent() throws Exception {
    String url = "https://books.ed.ac.uk/edinburgh-diamond/catalog/book/ed-9781912669325";
    MockCachedUrl cu = new MockCachedUrl(url, mau);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new NatureHtmlMetadataExtractorFactory.NatureHtmlMetadataExtractor();
    FileMetadataListExtractor mle =
            new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any, cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals("10.2218/ED.9781912669325", md.get(MetadataField.FIELD_DOI));
  }
}
