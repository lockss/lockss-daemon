/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
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

package org.lockss.plugin.psychiatryonline;

import java.util.regex.Pattern;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.FileMetadataListExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.*;
import org.lockss.util.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class TestPsychiatryOnlineArticleIteratorFactory extends ArticleIteratorTestCase {
	
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
  private final String PLUGIN_NAME = "org.lockss.plugin.psychiatryonline.PsychiatryOnlineTextbooksPlugin";
  private final String BASE_URL = "http://www.psychiatryonline.com/";
  private static final int DEFAULT_FILESIZE = 3000;
  
  private final String EXPECTED_BOOK_URL = "http://www.psychiatryonline.com/resourceToc.aspx?resourceID=5";
  private final String EXPECTED_FULL_TEXT_URL = EXPECTED_BOOK_URL;
  
  protected String cuRole = null;
  ArticleMetadataExtractor.Emitter emitter;
  protected boolean emitDefaultIfNone = false;
  FileMetadataExtractor me = null; 
  MetadataTarget target;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    
    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, poAuConfig());
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" +
        (SimulatedContentGenerator.FILE_TYPE_HTML) );
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }
  
  Configuration poAuConfig() {
    return ConfigurationUtil.fromArgs(
        "base_url", BASE_URL,
        "resource_id", "5");
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL), getRootUrls(artIter));
  }
  
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertMatchesRE   (pat, "http://www.psychiatryonline.com/resourceTOC.aspx?resourceID=5");
    assertMatchesRE   (pat, "http://www.psychiatryonline.com/resourceTOC.aspx?resourceID=4");
    assertNotMatchesRE(pat, "http://uksg.psychiatryonline.com/resourceTOC.aspx?resourceID=4");
    assertNotMatchesRE(pat, "http://uksg.psychiatryonline.com/contentt/823xp7lgublqah49");
    assertNotMatchesRE(pat, "http://www.example.com/content/");
  }
  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    // create urls to store in UrlCacher
    String[] urls = { BASE_URL + "resourceTOC.aspx?resourceID=5" };
    
    // get cached url content type and properties from simulated contents
    // for UrclCacher.storeContent()
    CachedUrl cuHtml = null;
    for (CachedUrl cu : AuUtil.getCuIterable(sau)) {
        if (cuHtml == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML)) {
          //log.info("html contenttype: " + cu.getContentType());
          cuHtml = cu;
	  break;
        }
    }
    // store content using cached url content type and properties
    UrlCacher uc;
    for (String url : urls) {
      //log.info("url: " + url);
      
      if (url.contains("full")) {
        UrlData ud = new UrlData(
            cuHtml.getUnfilteredInputStream(), cuHtml.getProperties(),url);
        uc = au.makeUrlCacher(ud);
        uc.storeContent();
      }
    }
    
    // get article iterator, get article files and the appropriate urls according
    // to their roles.
    String [] expectedUrls = { EXPECTED_FULL_TEXT_URL };
    for (SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) {
      ArticleFiles af = artIter.next();
      String[] actualUrls = { af.getFullTextUrl(),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML), };
      
      //log.info("actualUrls: " + actualUrls.length);
      for (int i = 0;i< actualUrls.length; i++) {
        //log.info("expected url: " + expectedUrls[i]);
        //log.info("  actual url: " + actualUrls[i]);
        assertEquals(expectedUrls[i], actualUrls[i]);
      }
    }
  }
  
  // the metadata that could be found
  String goodDOI = "10.1176/appi.books.9781585622825";
  String goodDate = "2000";
  String goodPublisher = "American Psychiatric Publishing Inc.";
  String[] goodAuthors = new String[] {"Alan F. Schatzberg, M.D.; Jonathan O. Cole, M.D.; Charles DeBattista, D.M.H., M.D."};
  String goodPublicationTitle = "Manual of Clinical Psychopharmacology, Sixth Edition";
  String goodISBN = "978-1-58562-317-4";
  String goodURL = "http://www.psychiatryonline.com/resourceTOC.aspx?resourceID=29";
  
  // no goodContent, all hard-coded
  String goodContent = "no content is used for metadata";
  
  /**
   * Method that creates a simulated Cached URL
   * It then asserts that the metadata extracted matches the metadata here
   * @throws Exception
   */
  public void testExtractFromPlugin() throws Exception {
    String url = "http://www.psychiatryonline.com/resourceTOC.aspx?resourceID=29";
    MockCachedUrl cu = new MockCachedUrl(url, au);
    cu.setContent(goodContent);
    cu.setContentSize(goodContent.length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    FileMetadataExtractor me = new PsychiatryOnlineMetadataExtractorFactory.PsychiatryOnlineMetadataExtractor();
    FileMetadataListExtractor mle = new FileMetadataListExtractor(me);
    List<ArticleMetadata> mdlist = mle.extract(MetadataTarget.Any(), cu);
    assertNotEmpty(mdlist);
    ArticleMetadata md = mdlist.get(0);
    assertNotNull(md);
    
    assertEquals(goodPublicationTitle, md.get(MetadataField.FIELD_PUBLICATION_TITLE));
    assertEquals(Arrays.asList(goodAuthors), md.getList(MetadataField.FIELD_AUTHOR));
    assertEquals(goodDOI, md.get(MetadataField.FIELD_DOI));
  }
  
}
