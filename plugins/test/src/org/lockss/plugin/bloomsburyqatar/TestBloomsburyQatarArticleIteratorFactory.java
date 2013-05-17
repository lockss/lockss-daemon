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

package org.lockss.plugin.bloomsburyqatar;

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

public class TestBloomsburyQatarArticleIteratorFactory extends ArticleIteratorTestCase {
	
	private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
	private final String PLUGIN_NAME = "org.lockss.plugin.bloomsburyqatar.ClockssBloomsburyQatarPlugin";
	private static final int DEFAULT_FILESIZE = 3000;

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
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, bloomsburyQatarAuConfig());
  }
  
  Configuration simAuConfig(String rootPath) {
	    Configuration conf = ConfigManager.newConfiguration();
	    conf.put("root", rootPath);
	    conf.put("base_url", "http://www.example.com/");
	    conf.put("depth", "1");
	    conf.put("branch", "4");
	    conf.put("numFiles", "7");
	    conf.put("fileTypes",
	             "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
	                   | SimulatedContentGenerator.FILE_TYPE_PDF));
	    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
	    return conf;
	  }
  
  Configuration bloomsburyQatarAuConfig() {
	    return ConfigurationUtil.fromArgs("base_url",
				 "http://www.example.com/",
				 "volume_name", "123",
				 "journal_dir", "bq");
	  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.example.com/doi/pdf/10.5339"),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    assertNotMatchesRE(pat, "http://www.wrong.com/doi/pdf/10.5339/bq.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/dooi/pdf/10.5339/bq.123.1");
    assertNotMatchesRE(pat, "http://www.example.com//pdf/10.5339/bq.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/doi/fulll/10.5339/bq.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/doi//10.5339/bq.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/doi/pdf/11.5339/bq.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/doi/pdf/10.5449/bq.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/doi/pdf//bq.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/doi/pdf/10.5339/beq.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/doi/pdf/10.5339/bqe.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/doi/pdf/10.5339/.123.1");
    assertNotMatchesRE(pat, "http://www.example.com/doi/pdf/10.5339/bqabc.1");
    assertMatchesRE(pat, "http://www.example.com/doi/pdf/10.5339/bq.123.");
    assertMatchesRE(pat, "http://www.example.com/doi/pdf/10.5339/bq.123.1");
    assertMatchesRE(pat, "http://www.example.com/doi/pdf/10.5339/bq.123.2");
    assertMatchesRE(pat, "http://www.example.com/doi/pdf/10.5339/bq.123.2323");
   }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat1 = "branch(\\d+)/(\\d+file\\.html)";
    String rep1 = "doi/full/10.5339/bq.123.1";
    PluginTestUtil.copyAu(sau, au, ".*[^.][^p][^d][^f]$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep2 = "doi/pdf/10.5339/bq.123.1";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
  
    String url = "http://www.example.com/doi/pdf/10.5339/bq.123.1";
    CachedUrl cu = au.makeCachedUrl(url);
    assertNotNull(cu);
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, cu);
    assertNotNull(af);    
    assertEquals(cu, af.getFullTextCu());
  }			

}