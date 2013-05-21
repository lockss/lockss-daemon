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

package org.lockss.plugin.metapress;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.*;
import org.lockss.util.*;
import java.util.Iterator;

public class TestMetaPressArticleIteratorFactory extends ArticleIteratorTestCase {
	
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
  private final String PLUGIN_NAME = "org.lockss.plugin.metapress.ClockssMetaPressPlugin";       
  private final String BASE_URL = "http://uksg.metapress.com/";
  private static final int DEFAULT_FILESIZE = 3000;
  
  private final String EXPECTED_PDF_URL = "http://uksg.metapress.com/content/823xp7lgublqah49/fulltext.pdf";
  private final String EXPECTED_FULL_TEXT_HTML_URL = "http://uksg.metapress.com/content/823xp7lgublqah49/fulltext.html";
  private final String EXPECTED_CITATION_RIS_URL = "http://uksg.metapress.com/export.mpx?code=823XP7LGUBLQAH49&mode=ris";
  private final String EXPECTED_FULL_TEXT_URL = EXPECTED_PDF_URL;

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
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME,
          ConfigurationUtil.fromArgs("base_url",
              "http://uksg.metapress.com/",
              "volume_name", "5",
              "journal_issn", "0953-0460"));
  }             
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", "http://uksg.metapress.com/");
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_PDF)
           + (SimulatedContentGenerator.FILE_TYPE_HTML)
           + (SimulatedContentGenerator.FILE_TYPE_TXT) );
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }
  
  Configuration metapressAuConfig() {
    return ConfigurationUtil.fromArgs("base_url",
        "http://uksg.metapress.com/",
        "volume_name", "5",
        "journal_issn", "0953-0460");
  }     

  public void testRoots() throws Exception {      
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://uksg.metapress.com/content"),
		 getRootUrls(artIter));
  }
   
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    assertNotMatchesRE(pat, "http://uksg.metapress.com/contentt/823xp7lgublqah49/j0143.pdfwrong");
    assertNotMatchesRE(pat, "http://uksg.metapress.com/contentt/volume/823xp7lgublqah49/j0143.pdfwrong");
    assertMatchesRE(pat, "http://uksg.metapress.com/content/823xp7lgublqah49/fulltext.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/content/");
    assertNotMatchesRE(pat, "http://www.example.com/content/j");
    assertNotMatchesRE(pat, "http://www.example.com/content/j0123/j383.pdfwrong");
  }
  
  public void testCitationRis() throws Exception {
    Pattern PATTERN = Pattern.compile("/content/([a-z0-9]{16})/fulltext\\.pdf$", Pattern.CASE_INSENSITIVE);
    String pdfUrl = "http://uksg.metapress.com/content/823xp7lgublqah49/fulltext.pdf";
    Matcher mat = PATTERN.matcher(pdfUrl);
    assertTrue(mat.find());
    // citation ris: http://uksg.metapress.com/export.mpx?code=823XP7LGUBLQAH49&mode=ris";
    String citStr = mat.replaceFirst("/export.mpx?code=$1&mode=ris");
    String[] citStrSplitEqual = citStr.split("=", 3);
    String[] citStrSplitAmpersand = citStrSplitEqual[1].split("&", 2);
    String codeStrUpper = citStrSplitAmpersand[0].toUpperCase();
    String citStrCodeUpper = citStrSplitEqual[0] + "=" + codeStrUpper + "&"
        + citStrSplitAmpersand[1] + "=" + citStrSplitEqual[2];
    log.info("citStrCodeUpper: " + citStrCodeUpper);
    assertEquals(EXPECTED_CITATION_RIS_URL, citStrCodeUpper);
   }
  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    // create urls to store in UrlCacher
    String[] urls = { BASE_URL + "content/823xp7lgublqah49/fulltext.pdf",
                      BASE_URL + "content/823xp7lgublqah49/fulltext.html",
                      BASE_URL + "export.mpx?code=823XP7LGUBLQAH49&amp;mode=ris" };
                                           
    // get cached url content type and properties from simulated contents
    // for UrclCacher.storeContent()
    Iterator<CachedUrlSetNode> cuIter = sau.getAuCachedUrlSet().contentHashIterator();
    CachedUrl cuPdf = null;
    CachedUrl cuHtml = null;
    CachedUrlSetNode cusn;
    while (cuIter.hasNext()) {
      cusn = cuIter.next();
      if (!cusn.hasContent()) {
        continue;
      }
      if (cusn.getType() == CachedUrlSetNode.TYPE_CACHED_URL) {
        CachedUrl cu = (CachedUrl)cusn;
        if (cuPdf == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF)) {
          //log.info("pdf contenttype: " + cu.getContentType());
          cuPdf = cu;
        } else if (cuHtml == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML)) {
          //log.info("html contenttype: " + cu.getContentType());
          cuHtml = cu;
        }
      }
    }
    // store content using cached url content type and properties
    UrlCacher uc;
    for (String url : urls) {
      //log.info("url: " + url);
      uc = au.makeUrlCacher(url);
      if (url.contains("full")) {
        uc.storeContent(cuHtml.getUnfilteredInputStream(), cuHtml.getProperties());
      } else if (url.contains("pdf")) {
        uc.storeContent(cuPdf.getUnfilteredInputStream(), cuPdf.getProperties());
      }
    }
 
   // get article iterator, get article files and the appropriate urls according
    // to their roles.
    String [] expectedUrls = { EXPECTED_PDF_URL,
                               EXPECTED_FULL_TEXT_URL,
                               EXPECTED_FULL_TEXT_HTML_URL };
    for (SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) {
      ArticleFiles af = artIter.next();
      String[] actualUrls = { af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF),
                              af.getFullTextUrl(),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML), };
                              
      //log.info("actualUrls: " + actualUrls.length);
      for (int i = 0;i< actualUrls.length; i++) {
        //log.info("expected url: " + expectedUrls[i]);
        //log.info("  actual url: " + actualUrls[i]);
        assertEquals(expectedUrls[i], actualUrls[i]);
      }   
    }

  }    
  
}
