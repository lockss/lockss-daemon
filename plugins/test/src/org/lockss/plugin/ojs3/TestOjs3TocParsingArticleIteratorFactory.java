/* $Id$

Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ojs3;

import java.io.*;

import org.apache.commons.io.IOUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.*;
import org.lockss.util.*;

/* 
 * 
 */

public class TestOjs3TocParsingArticleIteratorFactory extends ArticleIteratorTestCase {
  
  static Logger log = Logger.getLogger(TestOjs3TocParsingArticleIteratorFactory.class);

  SimulatedArchivalUnit sau;
  private final String PLUGIN_NAME = "org.lockss.plugin.ojs3.Ojs3Plugin";
  private final String BASE_URL = "https://www.foo.com/";
  private final String JOURNAL_ID = "test";
  private final String YEAR = "2018";
  private static final int DEFAULT_FILESIZE = 3000;
  
  // expected urls for article 1
  private final String EXPECTED_ABS_URL_1 =
    "https://www.foo.com/index.php/test/article/view/8110";
  private final String EXPECTED_ARTICLE_METADATA_URL_1 =
    "https://www.foo.com/index.php/test/article/view/8110";
  private final String EXPECTED_PDF_1 = 
		    "https://www.foo.com/index.php/test/article/view/8110/8601";
  private final String EXPECTED_HTML_1 = 
		    "https://www.foo.com/index.php/test/article/view/8110/8514";

  String [] expectedUrls1 = { EXPECTED_ABS_URL_1,
                              EXPECTED_ARTICLE_METADATA_URL_1,
                              EXPECTED_PDF_1,
                              EXPECTED_PDF_1,
                              EXPECTED_HTML_1};


  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
    ConfigurationUtil.addFromArgs(CachedUrl.PARAM_ALLOW_DELETE, "true");
  }
    
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return 
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, Ojs3AuConfig());
  }     

  Configuration Ojs3AuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_id", JOURNAL_ID);
    conf.put("year", YEAR);
    return conf;
  }
  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", Integer.toString(1));
    conf.put("branch", Integer.toString(4));
    conf.put("numFiles", Integer.toString(3));
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_PDF
            | SimulatedContentGenerator.FILE_TYPE_XML));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }
  


  
  // Simulated content URLs stored in the UrlCacher object.
  public void storeTestContent(String url, String htmlString) throws Exception {
    log.debug3("storeTestContent() url: " + url);
    InputStream input = null;
    CIProperties props = null;
    // issue table of content
    if (url.contains("478")) { 
      input = getTestTocInputStream(htmlString);
      props = getHtmlProperties();
    }else if (url.endsWith("8601") && url.contains("download")) { 
        // pdf
        input = new StringInputStream("");
        props = getPdfProperties();
    } else if (url.endsWith("8110") || url.endsWith("8514")) {
      // abs - for metadata/
      input = new StringInputStream("<html></html>");
      props = getHtmlProperties();
    } else {
    	// default blank html
      input = new StringInputStream("<html></html>");
      props = getHtmlProperties();
    }
    UrlData ud = new UrlData(input, props, url);
    UrlCacher uc = au.makeUrlCacher(ud);
    uc.storeContent();
  }  
  
  public void testCreateArticleFiles(String htmlString) throws Exception {

    simCrawlBasic(htmlString);
    // access Ojs3ArticleItrerator
    Iterator<ArticleFiles> it = au.getArticleIterator();
    // ensure you have found articles - this would not otherwise not fail before completion
    assertTrue(it.hasNext());
    int count = 0;
    while (it.hasNext()) {
      ArticleFiles af1 = it.next();
      log.info("article file af1: " + af1.toString());
      count+=1;
      // assert article 1
      String[] actualUrls1 = { af1.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
                               af1.getRoleUrl(ArticleFiles.ROLE_ARTICLE_METADATA),
                               af1.getFullTextUrl(),
                               af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF),
                               af1.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML) };

      for (int i = 0;i< actualUrls1.length; i++) {
        log.info("expected url1: " + expectedUrls1[i]);
        log.info("  actual url1: " + actualUrls1[i]);

        // "scholarworks.iu.edu" has speical case, which only has html page, no other aspects of article
        if (!BASE_URL.contains("scholarworks.iu.edu")) {
          assertEquals(expectedUrls1[i], actualUrls1[i]);
        }
      }
      
    }
    assertEquals(count,1);
  }

  public void testDifferentHtmlStructures(){
    try {
      testCreateArticleFiles("test_toc.html");
      testCreateArticleFiles("test_toc_2.html");
      testCreateArticleFiles("test_toc_3.html");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  /*
   * The following tests are from the older version of the OJS3 Article Iterator Factory.
   * 
   * 
   * Test of the Article Iterator pattern matching when only issue level content exists
   *
   *   
  public void testIssueUrlsWithPrefixes() throws Exception {
    Iterator<ArticleFiles> artIter = getArticleIteratorAfterSimCrawl(au, "issue");
    Pattern pat = getPattern((SubTreeArticleIterator) artIter);
    // issue
    assertMatchesRE(pat,"https://www.foo.com/index.php/test/issue/view/99");
    // wont exist
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/article/view/99/100");
    // abstract, wont exist
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/article/view/99");
    // an article view but not an abstract
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/article/view/99/22");
    // probably pdf
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/article/download/99/22");

  }

  /*
   * Test of the Article Iterator pattern matching when "typical" single level article content exists
   *
   *   
  public void testArticleSingleUrlsWithPrefixes() throws Exception {
    Iterator<ArticleFiles> artIter = getArticleIteratorAfterSimCrawl(au, "article/123");
    Pattern pat = getPattern((SubTreeArticleIterator) artIter);
    // issue
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/issue/view/99");
    // wont exist
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/article/view/99/100");
    // abstract, wont exist
    assertMatchesRE(pat,"https://www.foo.com/index.php/test/article/view/99");
    // an article view but not an abstract
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/article/view/99/22");
    // probably pdf
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/article/download/99/22");
  }
  /*
   * Test of the Article Iterator pattern matching when only double level article content exists
   *
   *   
  public void testArticleDoubleUrlsWithPrefixes() throws Exception {
    Iterator<ArticleFiles> artIter = getArticleIteratorAfterSimCrawl(au, "article/123/456");
    Pattern pat = getPattern((SubTreeArticleIterator) artIter);

    // issue
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/issue/view/99");
    // wont exist
    assertMatchesRE(pat,"https://www.foo.com/index.php/test/article/view/99/100");
    // abstract, wont exist
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/article/view/99");
    // an article view but not an abstract
    assertMatchesRE(pat,"https://www.foo.com/index.php/test/article/view/99/22");
    // probably pdf
    assertNotMatchesRE(pat,"https://www.foo.com/index.php/test/article/download/99/22");
  }


  private Iterator<ArticleFiles> getArticleIteratorAfterSimCrawl(ArchivalUnit au,
                                                                        String urlType)
      throws Exception {
    ArrayList<Pattern> removalPatterns =  new ArrayList<>();
    removalPatterns.add(Pattern.compile("/article/view/[^/]+$"));
    removalPatterns.add(Pattern.compile("/article/view/[^/]+/[^/]+$"));
    // simulate the crawl
    simCrawlBasic();
    if (Objects.equals(urlType, "issue")) {
      // remove all article urls
      deleteFromCrawl(removalPatterns);
    } else if (Objects.equals(urlType, "article/123/456")) {
      removalPatterns.remove(1);
      // remove single level article urls
      deleteFromCrawl(removalPatterns);
    }
    // return an articleIterator for the crawl content
    return au.getArticleIterator();
  }*/

  private void simCrawlBasic(String htmlString) throws Exception {
    ArrayList<String> articleUrls = new ArrayList<>();
    articleUrls.add(String.format("%sindex.php/%s/issue/view/478",
        BASE_URL, JOURNAL_ID)); // table of contents
    articleUrls.add(String.format("%sindex.php/%s/issue/view/478/577",
        BASE_URL, JOURNAL_ID)); // pdf of entire issue
    articleUrls.add(String.format("%sindex.php/%s/article/view/8110",
        BASE_URL, JOURNAL_ID)); // abstract
    // this should get ignored
    articleUrls.add(String.format("%s%s/article/view/8110",
        BASE_URL, JOURNAL_ID)); // no index.php abstract
    articleUrls.add(String.format("%sindex.php/%s/article/view/8110/8514",
        BASE_URL, JOURNAL_ID)); // full-text html
    articleUrls.add(String.format("%sindex.php/%s/article/view/8110/8601",
        BASE_URL, JOURNAL_ID)); // pdf landing - html
    articleUrls.add(String.format("%sindex.php/%s/article/download/8110/8601",
        BASE_URL, JOURNAL_ID)); // pdf
    articleUrls.add(String.format("%sindex.php/%s/article/view/8220/8514",
        BASE_URL, JOURNAL_ID)); // full-text html
    articleUrls.add(String.format("%sindex.php/%s/article/view/8220/8601",
        BASE_URL, JOURNAL_ID)); // pdf landing - html

    // Store test cases - articleUrls
    for (String url : articleUrls) {
      log.info("testCreateArticleFiles() url: " + url);
      storeTestContent(url, htmlString);
    }
  }
  private void deleteFromCrawl(ArrayList<Pattern> removalPatterns) throws IOException {
    for (CachedUrl cu : AuUtil.getCuIterable(au)) {
      String url = cu.getUrl();
      for (Pattern pat : removalPatterns) {
        Matcher art = pat.matcher(url);
        if (art.find()) {
          log.debug3("deleting: " + url + " as well as all children of this url path...");
          cu.delete();
        }
      }
    }
  }

  private static final String abstractMetadata=
		  "<!DOCTYPE html>\n" + 
		  "<html lang=\"en-US\" xml:lang=\"en-US\">\n" + 
		  "<head>\n" + 
		  "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" + 
		  "<title>Journal title</title>\n" + 
		  "<meta name=\"generator\" content=\"Open Journal Systems 3.1.1.4\">\n" + 
		  "<meta name=\"gs_meta_revision\" content=\"1.1\"/>\n" + 
		  "<meta name=\"citation_journal_title\" content=\"Journal of the Things\"/>\n" + 
		  "<meta name=\"citation_journal_abbrev\" content=\"1\"/>\n" + 
		  "<meta name=\"citation_issn\" content=\"1111-1111\"/>\n" + 
		  "<meta name=\"citation_author\" content=\"Barbara J. Smart\"/>\n" + 
		  "<meta name=\"citation_author\" content=\"Sarah Intelligent\"/>\n" + 
		  "<meta name=\"citation_title\" content=\"Concepts: Smart Thoughts\"/>\n" + 
		  "<meta name=\"citation_date\" content=\"2017/11/02\"/>\n" + 
		  "<meta name=\"citation_volume\" content=\"17\"/>\n" + 
		  "<meta name=\"citation_issue\" content=\"4\"/>\n" + 
		  "<meta name=\"citation_firstpage\" content=\"17\"/>\n" + 
		  "<meta name=\"citation_lastpage\" content=\"30\"/>\n" + 
		  "<meta name=\"citation_doi\" content=\"10.14434/test.1234\"/>\n" + 
		  "<meta name=\"citation_abstract_html_url\" content=\"https://www.foo.com/journals/index.php/test/article/view/8110\"/>\n" + 
		  "<meta name=\"citation_language\" content=\"en\"/>\n" + 
		  "<meta name=\"citation_pdf_url\" content=\"https://www.foo.com/index.php/test/article/download/8110/8601\"/>\n" + 
		  "<link rel=\"schema.DC\" href=\"http://purl.org/dc/elements/1.1/\" />\n" + 
		  "</head><body class=\"pkp_page_article pkp_op_view\">\n" + 
		  "</body>\n" + 
		  "</html>\n"; 

    
  // Response header to be stored with the simulated HTML content URLs
  // Note the X-Locks-content-type is necessary (not Content-type)
  // in order to be found by the LOCKSS Daemon. 
  private CIProperties getHtmlProperties() {
    CIProperties htmlProps = new CIProperties();
    htmlProps.put("RESPONSE","HTTP/1.0 200 OK");
    htmlProps.put("Date", "Fri, 06 Apr 2012 18:22:49 GMT");
    htmlProps.put("Server", "Apache/2.2.3 (CentOS)");
    htmlProps.put("X-Powered-By", "PHP/5.2.17");
    htmlProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
    htmlProps.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
    htmlProps.put("Pragma", "no-cache");
    htmlProps.put("X-Lockss-content-type", "text/html; charset=UTF-8");
    htmlProps.put("X-Cache", "MISS from lockss.org");
    htmlProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
    htmlProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
    htmlProps.put("Connection", "close");
    return htmlProps;
  }
  
  // Response header to be stored with the simulated PDF content URLs
  // Note the X-Locks-content-type is necessary (not Content-type)
  // in order to be found by the LOCKSS Daemon. 
  private CIProperties getPdfProperties() {
    CIProperties pdfProps = new CIProperties();
    pdfProps.put("RESPONSE","HTTP/1.0 200 OK");
    pdfProps.put("Date", "Fri, 06 Apr 2012 18:22:49 GMT");
    pdfProps.put("Server", "Apache/2.2.3 (CentOS)");
    pdfProps.put("X-Powered-By", "PHP/5.2.17");
    pdfProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
    pdfProps.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
    pdfProps.put("Pragma", "no-cache");
    pdfProps.put("X-Lockss-content-type", "application/pdf; charset=UTF-8");
    pdfProps.put("X-Cache", "MISS from lockss.org");
    pdfProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
    pdfProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
    pdfProps.put("Connection", "close");
    return pdfProps;
  }

  // Read the test TOC file test_toc_junit.html from current directory
  // Prepare input stream for UrlCacher storeContent() method
  private InputStream getTestTocInputStream(String htmlDoc) throws IOException {
    InputStream htmlIn = getResourceAsStream(htmlDoc);
    String absHtml = StringUtil.fromInputStream(htmlIn);
    return IOUtils.toInputStream(absHtml);
  }

}
