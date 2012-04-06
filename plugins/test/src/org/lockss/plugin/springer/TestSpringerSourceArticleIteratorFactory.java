/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer;

import java.io.File;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestSpringerSourceArticleIteratorFactory extends ArticleIteratorTestCase {
	
	private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
	private final String PLUGIN_NAME = "org.lockss.plugin.springer.ClockssSpringerSourcePlugin";
	private static final int DEFAULT_FILESIZE = 3000;

  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
    
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);
    
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }
  
  public void tearDown() throws Exception {
	    sau.deleteContentTree();
	    super.tearDown();
	  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, springerAuConfig());
  }
  
  Configuration simAuConfig(String rootPath) {
	    Configuration conf = ConfigManager.newConfiguration();
	    conf.put("root", rootPath);
	    conf.put("base_url", "http://www.example.com/");
	    conf.put("year", "2012");
	    conf.put("depth", "1");
	    conf.put("branch", "4");
	    conf.put("numFiles", "7");
	    conf.put("fileTypes",
	             "" + (  SimulatedContentGenerator.FILE_TYPE_XML
	                   | SimulatedContentGenerator.FILE_TYPE_PDF));
	    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
	    return conf;
	  }
  
  Configuration springerAuConfig() {
	    return ConfigurationUtil.fromArgs("base_url",
				 "http://www.example.com/",
				 "year", "2012");
	  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.example.com/2012"),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    assertNotMatchesRE(pat, "http://www.wrong.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/1066/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.wrong!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/ISU=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/PDF/article.wrong");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/wrong/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/wrong/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/wrong=2012_53/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/wrong=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/wrong=2012.3/ISU=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/wrong=2/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/DIFF-STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=3/ART=2012_53/BodyRef/PDF/article.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=23/VOL=2012.2/ISU=8/ART=2012_23/BodyRef/PDF/article.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=2-3/ART=2012_53/BodyRef/PDF/random_article.pdf");
   }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat1 = "branch(\\d+)/(\\d+file\\.xml)";
    String rep1 = "STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=2-3/ART=2012_53/random_article.xml.Meta";
    PluginTestUtil.copyAu(sau, au, ".*[^.][^p][^d][^f]$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep2 = "STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=2-3/ART=2012_53/BodyRef/PDF/random_article.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
  
    String pdfUrl = "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=2-3/ART=2012_53/BodyRef/PDF/random_article.pdf";
    String metadataUrl = "http://www.example.com/2012/STUFF_07-26-12.zip!/JOU=2/VOL=2012.3/ISU=2-3/ART=2012_53/random_article.xml.Meta";
    CachedUrl cu = au.makeCachedUrl(pdfUrl);
    assertNotNull(cu);
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, cu);
    assertNotNull(af);
    assertEquals(cu, af.getFullTextCu());
    assertEquals(cu, af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF));
    //assertEquals(au.makeCachedUrl(metadataUrl), af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA));
  }			

}