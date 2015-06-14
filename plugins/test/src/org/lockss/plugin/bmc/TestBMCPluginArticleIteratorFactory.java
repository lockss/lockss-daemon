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

package org.lockss.plugin.bmc;

//import java.io.File;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
//import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
//import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestBMCPluginArticleIteratorFactory extends ArticleIteratorTestCase {
	
  /**
   * Simulated AU to generate content
   */
  private SimulatedArchivalUnit sau;
  private final String ARTICLE_FAIL_MSG = "Article files not created properly";
  private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
  private static String PLUGIN_NAME = "org.lockss.plugin.bmc.BMCPlugin";
  private static String BASE_URL = "http://www.helloworld.com/ ";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String VOLUME_NAME = "1";
  private final String JOURNAL_ISSN = "1471-2253";
  private static final int DEFAULT_FILESIZE = 3000;
  private final Configuration AU_CONFIG = 
	  ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
                                      VOLUME_NAME_KEY, VOLUME_NAME,             
                                      JOURNAL_ISSN_KEY, JOURNAL_ISSN);

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
      /* reverting temporarily to pre-nondef_param plugin (no journal_code)
       PluginTestUtil.createAndStartAu(PLUGIN_NAME,
			      ConfigurationUtil.fromArgs("base_url",
							 "http://www.helloworld.com/ ",
							 "volume_name", "1",
							 "journal_code", "bmcanesthesiol",
							 "journal_issn", "1471-2253"));
      */
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes",
	             "" + (SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  
  
  //http://www.biomedcentral.com/content/pdf/1472-6831-12-60.pdf
  //http://www.biomedcentral.com/content/pdf/1471-2253-14-S1-A12.pdf

  public void testReplacement() {
    Pattern PDF_PATTERN = Pattern.compile("/content/pdf/([\\d]+-[\\d]+)-([\\d]+)-([\\dS]+)(-([\\dA-Z]+))?\\.pdf$", Pattern.CASE_INSENSITIVE);
    String url = "http://www.biomedcentral.com/content/pdf/1472-6831-12-60.pdf";
    String expectedUrl = "http://www.biomedcentral.com/1472-6831/12/60/abstract";
    String newUrl;
    Matcher mat = PDF_PATTERN.matcher(url);
    if (mat.find()) {
      if (mat.group(5) != null) {
        // supplement article has extra level
        newUrl = mat.replaceFirst("/$1/$2/$3/$5/abstract");
      } else {
        newUrl = mat.replaceFirst("/$1/$2/$3/abstract");
      }
      log.debug3("newUrl is " + newUrl);
      assertEquals(expectedUrl,newUrl);
    }
  }

  public void _testRoots() throws Exception {      
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals("Article file root URL pattern changed or incorrect", ListUtil.list("http://www.helloworld.com/1471-2253/1"),
		 getRootUrls(artIter));
  }
  
  public void _testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.helloworld.com/content/pdf/1471-2253-1-2.pdfll");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://inderscience.metapress.com/contentt/volume/1014174823t49006/j0143.pdfwrong");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.helloworld.com/content/pdf/1471-2253-1-2.pdf");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.example.com/content/");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.example.com/content/j");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.example.com/content/j0123/j383.pdfwrong");
  }
  
  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat2 = "content/pdf/([^/]+)-([^/]+)-([^/]+)\\.pdf";
    String rep2 = "content/pdf/([^/]+)-([^/]+)-([^/]+)\\.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
    String url = "http://www.helloworld.com/content/pdf/1471-2253-1-2.pdf";
    CachedUrl cu = au.makeCachedUrl(url);
    assertNotNull(cu);
    /* removing until we revert BMCPlugin back to nondef_params
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, cu);
    assertEquals(cu, af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF));
    */
  }	


}
