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

package org.lockss.plugin.medknow;

import java.io.InputStream;
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
  private final String EXPECTED_HTML_URL = "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe";
  private final String EXPECTED_CITE_URL = "http://www.afrjpaedsurg.org/citation.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe";
  private final String EXPECTED_RIS_URL = "http://www.afrjpaedsurg.org/citeman.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;t=2";
  
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
    assertNotMatchesRE(pat, "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7");
    assertMatchesRE(pat, "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=");
    assertMatchesRE(pat, "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe");
    assertMatchesRE(pat, "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=0");
    assertMatchesRE(pat, "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=2");
    assertNotMatchesRE(pat, "http://www.afrjpaedsurg.org/article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=1");
    assertNotMatchesRE(pat, "http://www.example.com/content/");
    assertNotMatchesRE(pat, "http://www.example.com/content/j");
    assertNotMatchesRE(pat, "http://www.example.com/content/j0123/j383.pdfwrong");
  }
  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    // create urls to store in UrlCacher
    String[] urls = { BASE_URL + "article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=0",
                      BASE_URL + "article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;type=2",
                      BASE_URL + "article.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe",
                      BASE_URL + "citation.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe",
                      BASE_URL + "citeman.asp?issn=0189-6725;year=2012;volume=9;issue=1;spage=3;epage=7;aulast=Ibekwe;t=2"};
    
    // get cached url content type and properties from simulated contents
    // for UrclCacher.storeContent()
    CachedUrl cuAbs = null;
    CachedUrl cuPdf = null;
    CachedUrl cuHtml = null;
    for (CachedUrl cu : AuUtil.getCuIterable(sau)) {
        if (cuPdf == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF)) {
          //log.info("pdf contenttype: " + cu.getContentType());
          cuPdf = cu;
        } else if (cuAbs == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML)) {
          // log.info("abs html contenttype: " + cu.getContentType());
          cuAbs = cu;
        } else if (cuHtml == null 
            && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML)) {
          // log.info("abs html contenttype: " + cu.getContentType());
          cuHtml = cu;
        }
        if (cuPdf != null && cuAbs != null && cuHtml != null) {
          break;
        }
    }
    // store content using cached url content type and properties
    UrlCacher uc;
    for (String url : urls) {
      //log.info("url: " + url);
      InputStream input = null;
      CIProperties props = null;
      if (url.contains("type=0")) {
        input = cuAbs.getUnfilteredInputStream();
        props = cuAbs.getProperties();
      } else if (url.contains("type=2")) {
        input = cuPdf.getUnfilteredInputStream();
        props = cuPdf.getProperties();
      } else {
        input = cuHtml.getUnfilteredInputStream();
        props = cuHtml.getProperties();
      }
      UrlData ud = new UrlData(input, props, url);
      uc = au.makeUrlCacher(ud);
      uc.storeContent();
    }

    // get article iterator, get article files and the appropriate urls according
    // to their roles.
    String [] expectedUrls = { EXPECTED_ABS_URL,
                               EXPECTED_PDF_URL,
                               EXPECTED_HTML_URL,
                               EXPECTED_CITE_URL,
                               EXPECTED_RIS_URL};
    int count = 0;
    for (SubTreeArticleIterator artIter = createSubTreeIter(MetadataTarget.Any()); artIter.hasNext(); count++) {
      ArticleFiles af = artIter.next();
      String[] actualUrls = { af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF),
                              af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
                              af.getRoleUrl(ArticleFiles.ROLE_CITATION),
                              af.getRoleUrl(ArticleFiles.ROLE_CITATION_RIS)
                              };
      //log.info("actualUrls: " + actualUrls.length);
      for (int i = 0;i< actualUrls.length; i++) {
        //log.info("expected url: " + expectedUrls[i]);
        //log.info("  actual url: " + actualUrls[i]);
        assertEquals(expectedUrls[i], actualUrls[i]);
      }
    }
    assertEquals(count, 1);
  }
  
}
