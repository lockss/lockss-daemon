/*
 * $Id$
 */
/*

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

package org.lockss.plugin.spandidos;

import org.apache.commons.io.IOUtils;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.config.Tdb;
import org.lockss.config.TdbAu;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.PluginException;
import org.lockss.extractor.*;
import org.lockss.plugin.*;
import org.lockss.plugin.atypon.BaseAtyponHtmlMetadataExtractorFactory;
import org.lockss.plugin.atypon.rsp.TestRoyalSocietyPublishingHtmlCrawlFilterFactory;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;
import org.lockss.util.TypedEntryMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;


public class TestSpandidosMetadataExtractor extends LockssTestCase {

  private static Logger log = Logger.getLogger(TestSpandidosMetadataExtractor.class);

  private static final String PLUGIN_ID = "org.lockss.plugin.spandidos.SpandidosPlugin";

  private ArchivalUnit mau;
  private String tempDirPath;
  private MockLockssDaemon daemon;
  private PluginManager pluginMgr;

  /*
  https://www.spandidos-publications.com/etm/6/6/1365
  https://www.spandidos-publications.com/etm/6/6/1365/download
  https://www.spandidos-publications.com/etm/6/6/1365?text=abstract
   https://www.spandidos-publications.com/etm/6/6/1365?text=fulltext
  */

  private static String BASE_URL = "https://www.spandidos-publications.com/";
  private static String JID = "ol";
  private static String VOLUME_NAME = "6";
  
  // the metadata that should be extracted
  static String goodDate = "2013-12-01";
  static String[] goodAuthors = new String[] {"Zuo,Yun", "Song,Yu"};
  static String goodFormat = "text/HTML";
  static String goodTitle = "article title";
  static String goodPublisher = "Spandidos Publications";


  private static final String ABS_URL =  BASE_URL + JID + "/6/6/1234?text=abstract";
  private static final String PDF_URL =  BASE_URL + JID + "/6/6/1234/download";


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
    conf.put("volume_name", VOLUME_NAME);
    return conf;
  }


  String goodHtmlContent =
          "    <meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\" />\n" +
          "    <meta charset=\"utf-8\" />\n" +
          "    <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\" />    \n" +
          "    <meta name=\"citation_doi\" content=\"10.3892/etm.2013.1334\" />\n" +
          "    <meta name=\"citation_journal_title\" content=\"Experimental and Therapeutic Medicine\" />\n" +
          "    <meta name=\"citation_title\" content=\"article title\" />\n" +
          "    <meta name=\"citation_author\" content=\"Zuo,Yun\" />    \n" +
          "    <meta name=\"citation_author_institution\" content=\"Department of Oncology, Zhangjiagang First Hospital, Zhangjiagang, Jiangsu 215600, P.R. China\" />    \n" +
          "    <meta name=\"citation_author\" content=\"Song,Yu\" />    \n" +
          "    <meta name=\"citation_author_institution\" content=\"Department of Oncology, Zhangjiagang First Hospital, Zhangjiagang, Jiangsu 215600, P.R. China\" />    \n" +
          "    <meta name=\"citation_publication_date\" content=\"2013-12-01\" />\n" +
          "    <meta name=\"citation_volume\" content=\"6\" />\n" +
          "    <meta name=\"citation_issue\" content=\"6\" />\n" +
          "    <meta name=\"citation_firstpage\" content=\"1365\" /> \n" +
          "    <meta name=\"citation_lastpage\" content=\"1368\" /> \n" +
          "    <meta name=\"citation_publisher\" content=\"Spandidos Publications\" />\n" +
          "    <meta name=\"citation_issn\" content=\"1792-0981\" />\n" +
          "    <meta name=\"citation_abstract_html_url\" content=\"http://www.spandidos-publications.com/etm/6/6/1365/abstract\" />\n" +
          "    <meta name=\"citation_pdf_url\" content=\"http://www.spandidos-publications.com/etm/6/6/1365/download\" />\n" +
          "    <meta name=\"citation_fulltext_world_readable\" content=\"\" />\n" +
          "    <meta name=\"keywords\" content=\"cancer, oncology, oncogene, molecular medicine, tumor, apoptosis, gene, cell, research, scientific journals, tumour suppressor genes, cell cycle, growth factors\" />\n" +
          "";

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
    me = new SpandidosHtmlMetadataExtractorFactory().createFileMetadataExtractor(MetadataTarget.Any(), "text/html");

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
