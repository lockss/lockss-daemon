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

package org.lockss.plugin.medknow;

import java.util.regex.Pattern;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.*;
import org.lockss.util.*;
import java.util.Iterator;

/*
 * Stores sample article URLs with different format: abstract, full text HTML 
 * and full text PDF.
 * 
 * Issue table of content:
 * http://www.medknowarticleiteratortest.org/showBackIssue.asp?issn=0189-6725;year=2012;volume=9;issue=1
 * 
 * Articles:
 * Abstract - http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=0
 * Full-text HTML - http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe
 * Full-text PDF - http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=2
 * Full-text Mobile- http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=3
 * Full-text EPUB- http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=4
 */
public class TestMedknowArticleIteratorFactory extends ArticleIteratorTestCase {
	
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
  private final String PLUGIN_NAME = "org.lockss.plugin.medknow.MedknowPlugin";       

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();

  private final String BASE_URL = "http://www.afrjpaedsurg.org/";
  private final String JOURNAL_ISSN = "0189-6725";
  private final String VOLUME_NAME = "9";
  private final String YEAR = "2012";
  
  private static final int DEFAULT_FILESIZE = 3000;
  
  private final String EXPECTED_ABS_URL = "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=0";
  private final String EXPECTED_PDF_URL = "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=2";
    
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
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, MedknowAuConfig());
  }
    
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", "http://www.afrjpaedsurg.org/");
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_PDF)
           + (SimulatedContentGenerator.FILE_TYPE_HTML)
           + (SimulatedContentGenerator.FILE_TYPE_TXT));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  // Set configuration attributes to create plugin AU (archival unit)
  Configuration MedknowAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put(JOURNAL_ISSN_KEY, JOURNAL_ISSN);
    conf.put(VOLUME_NAME_KEY, VOLUME_NAME);
    conf.put(YEAR_KEY, YEAR);
    return conf;
  }

  public void testRoots() throws Exception {      
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.afrjpaedsurg.org/"),
		 getRootUrls(artIter));
  }
   
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    assertNotMatchesRE(pat, "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=wrong");
    assertMatchesRE(pat, "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=0");
    assertNotMatchesRE(pat, "http://www.example.com/content/");
    assertNotMatchesRE(pat, "http://www.example.com/content/j");
    assertNotMatchesRE(pat, "http://www.example.com/content/j0123/j383.pdfwrong");
  }
  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    // create urls to store in UrlCacher
    String[] urls = { BASE_URL + "article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=0",
                      BASE_URL + "article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=2" };
                                           
    // get cached url content type and properties from simulated contents
    // for UrclCacher.storeContent()
    Iterator<CachedUrlSetNode> cuIter = sau.getAuCachedUrlSet().contentHashIterator();
    CachedUrl cuAbs = null;
    CachedUrl cuPdf = null;
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
        } else if (cuAbs == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML)) {
          // log.info("abs html contenttype: " + cu.getContentType());
          cuAbs = cu;
        }
      }
    }   
    // store content using cached url content type and properties
    UrlCacher uc;
    for (String url : urls) {
      //log.info("url: " + url);
      uc = au.makeUrlCacher(url);
      if (url.contains("type=0")) {
        uc.storeContent(cuAbs.getUnfilteredInputStream(), cuAbs.getProperties());
      } else if (url.contains("type=2")) {
        uc.storeContent(cuPdf.getUnfilteredInputStream(), cuPdf.getProperties());
      }
    }
 
    // get article iterator, get article files and the appropriate urls according
    // to their roles.
    String [] expectedUrls = { EXPECTED_ABS_URL,
                               EXPECTED_PDF_URL };
    for (SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) {
      ArticleFiles af = artIter.next();
      String[] actualUrls = { af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF) };
                              
      //log.info("actualUrls: " + actualUrls.length);
      for (int i = 0;i< actualUrls.length; i++) {
        //log.info("expected url: " + expectedUrls[i]);
        //log.info("  actual url: " + actualUrls[i]);
        assertEquals(expectedUrls[i], actualUrls[i]);
      }   
    }
  }    
  
}
