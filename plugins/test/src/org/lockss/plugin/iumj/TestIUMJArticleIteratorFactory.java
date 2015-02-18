/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.iumj;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Stack;
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

public class TestIUMJArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;      // Simulated AU to generate content
  
  private final String PLUGIN_NAME = "org.lockss.plugin.iumj.IUMJPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.example.net/";
  private final String VOLUME_NAME = "55";
  private final String YEAR = "2006";
  private final String CRAWLRULE0 = String.format("IUMJ/(YEAR|ABS|FULLTEXT|FTDLOAD)/%s/", YEAR, VOLUME_NAME);
  private final String CRAWLRULE1 = String.format("IUMJ/FTDLOAD/%s/%s/([^/])+/pdf", YEAR, VOLUME_NAME);
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, VOLUME_NAME,
      YEAR_KEY, YEAR);
  private static final int DEFAULT_FILESIZE = 3000;
  private final String ARTICLE_FAIL_MSG = "Article files not created properly";
  
  protected String cuRole = null;
  ArticleMetadataExtractor.Emitter emitter;
  protected boolean emitDefaultIfNone = false;
  FileMetadataExtractor me = null; 
  MetadataTarget target;
  private static Pattern regexpattern;
  
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
      PluginTestUtil.createAndStartAu(PLUGIN_NAME,  AU_CONFIG);
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "5");
    conf.put("fileTypes", "" + (
        SimulatedContentGenerator.FILE_TYPE_HTML |
        SimulatedContentGenerator.FILE_TYPE_PDF |
        SimulatedContentGenerator.FILE_TYPE_XML |
        SimulatedContentGenerator.FILE_TYPE_TXT));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  
  
  public void testRoots() throws Exception {      
    SubTreeArticleIterator artIter = createSubTreeIter();
    //http://www.example.net/IUMJ/FTDLOAD/2006/
    assertEquals(ListUtil.list(BASE_URL +"IUMJ/FTDLOAD/"+ YEAR + "/"), getRootUrls(artIter));
  }
  
  public void testCrawlRules() throws Exception {
    // use a substance pattern from the crawl rules
    String re_pattern = BASE_URL+String.format(CRAWLRULE0);
    regexpattern = Pattern.compile(re_pattern);
    Pattern pat = regexpattern;
    //http://www.iumj.indiana.edu/IUMJ/ABS/2006/2675
    //http://www.iumj.indiana.edu/IUMJ/FULLTEXT/2006/55/2579
    assertNotMatchesRE(pat, "http://www.wrong.com/doi/abs/10.2446/01.02.03.PR0.108.4.567-589");
    assertNotMatchesRE(pat, BASE_URL + "IUMJK/ABS/2006/2675");
    assertNotMatchesRE(pat, BASE_URL + "IUMJ/FULLTEXT/2012/55/2579");
    
    assertMatchesRE(pat, BASE_URL + "IUMJ/ABS/2006/2675");
    assertMatchesRE(pat, BASE_URL + "IUMJ/FTDLOAD/2006/55/2558/pdf");
    assertMatchesRE(pat, BASE_URL + "IUMJ/FULLTEXT/2006/55/2579");
    
    //http://www.iumj.indiana.edu/IUMJ/FTDLOAD/2006/55/2558/pdf
    re_pattern =BASE_URL+String.format(CRAWLRULE1);    
    regexpattern = Pattern.compile(re_pattern);
    pat = regexpattern;
    
    assertMatchesRE(pat, BASE_URL + "IUMJ/FTDLOAD/2006/55/2558/pdf");
    assertNotMatchesRE(pat, BASE_URL + "IUMJ/FTDLOAD/2006/55/2558/213/pdf");
    
  }
  
  /*
   * PDF Full Text: http://www.iumj.indiana.edu/IUMJ/FTDLOAD/2006/55/55001/pdf
   * HTML Abstract: http://www.iumj.indiana.edu/IUMJ/ABS/2006/55001
   * HTML FullText: http://www.iumj.indiana.edu/IUMJ/FULLTEXT/2006/55/55001
   * Metadata: http://www.iumj.indiana.edu/META/2006/55001.xml
   */
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String[] urls = {
        BASE_URL + "IUMJ/YEAR/2006",
        BASE_URL + "IUMJ/ISSUE/2006/1",

        BASE_URL + "IUMJ/FTDLOAD/2006/55/55001/pdf",
        BASE_URL + "IUMJ/FULLTEXT/2006/55/55001",
        BASE_URL + "META/2006/55001.xml",
        BASE_URL + "IUMJ/ABS/2006/55001",
        BASE_URL + "oai/2006/55/55001/55001.html",

        BASE_URL + "IUMJ/FTDLOAD/2006/55/55002/pdf",
//      BASE_URL + "IUMJ/FULLTEXT/2006/55/55002",
        BASE_URL + "META/2006/55002.xml",
        BASE_URL + "IUMJ/ABS/2006/55002",
        BASE_URL + "oai/2006/55/55002/55002.html",

        BASE_URL + "IUMJ/FTDLOAD/2006/55/55003/pdf",
        BASE_URL + "IUMJ/FULLTEXT/2006/55/55003",
        BASE_URL + "META/2006/55003.xml",
//        BASE_URL + "IUMJ/ABS/2006/55003",
//        BASE_URL + "oai/2006/55/55003/55003.html",

        BASE_URL + "IUMJ/FTDLOAD/2006/55/55008/pdf",
        BASE_URL + "IUMJ/FULLTEXT/2006/55/55008",
//        BASE_URL + "META/2006/55008.xml",
        BASE_URL + "IUMJ/ABS/2006/55008",
        BASE_URL + "oai/2006/55/55008/55008.html",
        BASE_URL + "",
        BASE_URL
    };
    CachedUrl cuPdf = null;
    CachedUrl cuHtml = null;
    CachedUrl cuXml = null;
    for (CachedUrl cu : AuUtil.getCuIterable(sau)) {
      if (cuPdf == null && 
          cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF))
    	{
    	  cuPdf = cu;
    	}
      else if (cuHtml == null && 
	        cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML))
    	{
    	  cuHtml = cu;
    	}
      else if (cuXml == null && 
	        cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_XML))
    	{
    	  cuXml = cu;
    	}
      if (cuPdf != null && cuHtml != null && cuXml != null) {
        break;
      }
    }
      
    for (String url : urls) {
      if (url.contains("IUMJ/FULLTEXT")) {
        storeContent(cuHtml.getUnfilteredInputStream(),
        cuHtml.getProperties(), url);
      }
      else if (url.contains("/pdf")) {
        storeContent(cuPdf.getUnfilteredInputStream(),
        cuPdf.getProperties(), url);
      }
      else if (url.contains("META")) {
        storeContent(cuXml.getUnfilteredInputStream(),
        cuXml.getProperties(), url);
      }
      else {
        storeContent(cuHtml.getUnfilteredInputStream(),
        cuHtml.getProperties(), url);
      }
    }
    
    
    Stack<String[]> expStack = new Stack<String[]>();
    String [] af1 = {
        BASE_URL + "IUMJ/FTDLOAD/2006/55/55001/pdf",
        BASE_URL + "META/2006/55001.xml",
        BASE_URL + "IUMJ/FULLTEXT/2006/55/55001",
        BASE_URL + "IUMJ/ABS/2006/55001",
        BASE_URL + "oai/2006/55/55001/55001.html"};
    
    String [] af2 = {
        BASE_URL + "IUMJ/FTDLOAD/2006/55/55002/pdf",
        BASE_URL + "META/2006/55002.xml",
        null,
        BASE_URL + "IUMJ/ABS/2006/55002",
        BASE_URL + "oai/2006/55/55002/55002.html"};
    
    String [] af3 = {
        BASE_URL + "IUMJ/FTDLOAD/2006/55/55003/pdf",
        BASE_URL + "META/2006/55003.xml",
        BASE_URL + "IUMJ/FULLTEXT/2006/55/55003",
        null,
        null};
    
    String [] af4 = {
        BASE_URL + "IUMJ/FTDLOAD/2006/55/55008/pdf",
        null,
        BASE_URL + "IUMJ/FULLTEXT/2006/55/55008",
        BASE_URL + "IUMJ/ABS/2006/55008",
        BASE_URL + "oai/2006/55/55008/55008.html"};
    
    expStack.push(af4);
    expStack.push(af3);
    expStack.push(af2);
    expStack.push(af1);
    
    for ( SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) 
    {
      ArticleFiles af = artIter.next();
      String[] act = {
          af.getFullTextUrl(),
          af.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
          af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE),
          af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
          af.getRoleUrl(ArticleFiles.ROLE_CITATION)
      };
      String[] exp = expStack.pop();
      if(act.length == exp.length){
        for(int i = 0;i< act.length; i++){
          assertEquals(ARTICLE_FAIL_MSG + " Expected: " + exp[i] + " Actual: " + act[i], exp[i],act[i]);
        }
      }
      else fail(ARTICLE_FAIL_MSG + " length of expected and actual ArticleFiles content not the same:" + exp.length + "!=" + act.length);
    }
  }
}
