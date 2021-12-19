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

package org.lockss.plugin.bioone;

import org.apache.commons.io.IOUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;


public class TestBioOne2020MetadataExtractorFactory extends LockssTestCase {

  private static Logger log = Logger.getLogger(TestBioOne2020MetadataExtractorFactory.class);

  private static final String PLUGIN_ID = "org.lockss.plugin.bioone.BioOne2020Plugin";

  private ArchivalUnit mau;
  private String tempDirPath;
  private MockLockssDaemon daemon;
  private PluginManager pluginMgr;

  private static String BASE_URL = "https://www.bioone.com/";
  private static String JID = "canadian-journal-of-animal-science";
  private static String YEAR = "2019";
  
  // the metadata that should be extracted
  static String goodDate = "2011/09";
  static String[] goodAuthors = new String[] {"Zuo,Yun", "Song,Yu"};
  static String goodTitle = "Ecoregion and farm size differences in dairy feed and manure nitrogen management: A survey";
  static String goodPublisher = "Canadian Science Publishing";


  private static final String ABS_URL =  BASE_URL + "journals/canadian-journal-of-animal-science/volume-91/issue-3/CJAS2010-004/Ecoregion-and-farm-size-differences-in-dairy-feed-and-manure/10.1139/CJAS2010-004.full";


  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = setUpDiskSpace();
    startMockDaemon();
    mau = createAu();
  }

  public void startMockDaemon() {
    daemon = getMockLockssDaemon();
    pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }


  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_ID, thisAuConfig());
  }

  private Configuration thisAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JID);
    conf.put("year", YEAR);
    return conf;
  }


  String goodHtmlContent =
          "<meta name=\"citation_journal_title\" content=\"Canadian Journal of Animal Science\"  />\n" +
          "\t\t<meta name=\"citation_journal_abbrev\" content=\"cjas\"  />\n" +
          "\t\t<meta name=\"citation_title\" content=\"Ecoregion and farm size differences in dairy feed and manure nitrogen management: A survey\"  />\n" +
          "\t\t<meta name=\"citation_article_type\" content=\"Paper\"  />\n" +
          "\t\t<meta name=\"citation_volume\" content=\"91\"  />\n" +
          "\t\t<meta name=\"citation_issue\" content=\"3\"  />\n" +
          "\t\t<meta name=\"citation_issn\" content=\"0008-3984\"  /><!--ISSN-->\n" +
          "\t\t<meta name=\"citation_issn\" content=\"1918-1825\"  /><!--eISSN-->\n" +
          "\t\t<meta name=\"citation_publication_date\" content=\"2011/09\"  />\n" +
          "\t\t<meta name=\"citation_doi\" content=\"10.1139/CJAS2010-004\"  />\n" +
          "\t\t<meta name=\"citation_publisher\" content=\"Canadian Science Publishing\"  />\n" +
          "\t\t<meta name=\"citation_firstpage\" content=\"459\"  />\n" +
          "\t\t<meta name=\"citation_lastpage\" content=\"473\"  />\n" +
          "\t\t<meta name=\"citation_author\" content=\"Zuo,Yun\"  />\n" +
          "\t\t<meta name=\"citation_author\" content=\"Song,Yu\"  />\n" +
          "\t\t<meta name=\"citation_author_institution\" content=\"Agriculture and Agri-Food Canada, PO Box 1000, Agassiz, British Columbia, Canada V0M 1A0\"  />\n" +
          "\t\t<meta name=\"citation_author_institution\" content=\"Alberta Agriculture and Rural Development, 5030-50th Street, Lacombe, Alberta, Canada T4L1W8\"  />\n" +
          "\t\t<meta name=\"citation_author_institution\" content=\"Statistics Canada, 120 Parkdale Avenue, Ottawa, Ontario, Canada K1A 0T6.\"  />\n" +
          "\t\t<meta name=\"citation_author_institution\" content=\"ECOMatters Inc., Pinawa, Manitoba, Canada R0E 1L0\"  />\n" +
          "\t\t<meta name=\"citation_fulltext_html_url\" content=\"https://bioone.org/journals/canadian-journal-of-animal-science/volume-91/issue-3/CJAS2010-004/Ecoregion-and-farm-size-differences-in-dairy-feed-and-manure/10.1139/CJAS2010-004.full\"  />\n" +
          "\t\t<meta name=\"citation_abstract_html_url\" content=\"https://bioone.org/journals/canadian-journal-of-animal-science/volume-91/issue-3/CJAS2010-004/Ecoregion-and-farm-size-differences-in-dairy-feed-and-manure/10.1139/CJAS2010-004.short\"  />\n" +
          "\t\t<meta name=\"citation_pdf_url\" content=\"https://bioone.org/journals/canadian-journal-of-animal-science/volume-91/issue-3/CJAS2010-004/Ecoregion-and-farm-size-differences-in-dairy-feed-and-manure/10.1139/CJAS2010-004.pdf\"  />\n" +
          "\t\t<meta name=\"citation_abstract\" content=\"Sheppard, S. C., Bittman, S., Swift, M. L., Beaulieu, M. and Sheppard, M. I. 2011. Ecoregion and farm size differences in dairy feed and manure nitrogen management: A survey. Can. J. Anim. Sci. 91: 459-473. This paper describes the activity of dairy farmers in Canada in 2005 related to the use of nitrogen (N) and especially practices that led to loss of ammonia (NH3). The data were obtained from a large-scale, statistically structured survey conducted across Canada. The survey sampling was stratified into 10 Ecoregions and across farm size. Numbers of lactating cows per farm were nearly twofold more in the west than the east. In western Canada less than 31% of barns were “tie-stall” type whereas 80% were tie-stall in the St. Lawrence Lowlands. The numbers of hours lactating cows spent in barns, standing yards, exercise fields and pasture varied with Ecoregion and farm size, important data in relation to NH3 emissions. Pasturing was more common in the east than west. Matching feed crude protein concentrations to physiological needs seems a potential best management practice, and smaller farms with tie-stalls seemed more prone to adjusting feed to individual cows compared with large farms with loose housing. Manure handling was divided, with slurry prominent especially in the west. Manure spreading practices also varied by Ecoregion. Overall, it is clear that national averages do not well represent dairy farm management: Ecoregion and farm size differences are significant.\"  />\n" +
          "\t\t<meta name=\"citation_keywords\" content=\"ammonia; ammoniac; Best Management Practices; emissions; émissions; Meilleurs pratiques de gestion; odeur; odour; \"  />\n";

  public void testExtractGoodHtmlContent() throws Exception {

    List<ArticleMetadata> mdlist = setupContentForAU(mau, ABS_URL, goodHtmlContent, true);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    assertEquals(goodPublisher, md.get(MetadataField.FIELD_PUBLISHER));
    assertEquals(goodTitle, md.get(MetadataField.FIELD_ARTICLE_TITLE));
    assertEquals(goodDate, md.get(MetadataField.FIELD_DATE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));

  }

  /* private support methods */
  private List<ArticleMetadata> setupContentForAU(ArchivalUnit au, String url,
                                                  String content,
                                                  boolean isHtmlExtractor) throws IOException, PluginException {
    FileMetadataExtractor me;

    InputStream input = null;
    CIProperties props = null;

    input = IOUtils.toInputStream(content, "utf-8");
    props = getContentHtmlProperties();
    me = new BioOne2020MetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/html");

    UrlData ud = new UrlData(input, props, url);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
    CachedUrl cu = uc.getCachedUrl();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    return mle.extract(MetadataTarget.Any(), cu);
  }

  private CIProperties getContentHtmlProperties() {
    CIProperties cProps = new CIProperties();
    // the CU checks the X-Lockss-content-type, not the content-type to determine encoding
    cProps.put(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html; charset=UTF-8");
    cProps.put("Content-type",  "text/html; charset=UTF-8");
    return cProps;
  }
}
