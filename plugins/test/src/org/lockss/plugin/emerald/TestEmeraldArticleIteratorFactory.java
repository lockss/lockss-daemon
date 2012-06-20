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

package org.lockss.plugin.emerald;

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

public class TestEmeraldArticleIteratorFactory extends ArticleIteratorTestCase {
	
	private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
	private final String PLUGIN_NAME = "org.lockss.plugin.emerald.EmeraldPlugin";
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
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, emeraldAuConfig());
  }
  
  Configuration simAuConfig(String rootPath) {
	    Configuration conf = ConfigManager.newConfiguration();
	    conf.put("root", rootPath);
	    conf.put("base_url", "http://www.example.com/");
	    conf.put("journal_issn", "5555-5555");
	    conf.put("volume_name", "16");
	    conf.put("depth", "1");
	    conf.put("branch", "4");
	    conf.put("numFiles", "7");
	    conf.put("fileTypes",
	             "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
	                   | SimulatedContentGenerator.FILE_TYPE_PDF));
	    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
	    return conf;
	  }
  
  Configuration emeraldAuConfig() {
	    return ConfigurationUtil.fromArgs("base_url",
				 "http://www.example.com/",
				 "journal_issn", "5555-5555",
				 "volume_name", "16");
	  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.example.com/"),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=pdf");
    assertNotMatchesRE(pat, "http://www.wrong.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journal.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journals.html?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journals.htm?issn=0000-0000&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&volume=00&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&vol=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&volume=16&articleid=1465119&show=pdf&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid&show=html&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=show=html&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&=html&view=printarticle");
    assertNotMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=xml&view=printarticle");
    
    
    assertMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertMatchesRE(pat, "http://www.example.com/books.htm?issn=5555-5555&volume=16&chapterid=1465119&show=html&view=printarticle");
    assertMatchesRE(pat, "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=1&articleid=1465119&show=html&view=printarticle");
    assertMatchesRE(pat, "http://www.example.com/books.htm?issn=5555-5555&volume=16&chapterid=1465119&show=html&view=printarticle");
   }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat1 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep1 = "journals.htm?issn=5555-5555&volume=16&issue=3&articleid=1482932&show=html&view=printarticle";
    PluginTestUtil.copyAu(sau, au, ".*[^.][^p][^d][^f]$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep2 = "journals.htm?issn=5555-5555&volume=16&articleid=1482932&show=pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
  
    String pdfUrl = "http://www.example.com/journals.htm?issn=5555-5555&volume=16&issue=3&articleid=1482932&show=html&view=printarticle";
    CachedUrl cu = au.makeCachedUrl(pdfUrl);
    assertNotNull(cu);
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, cu);
    assertNotNull(af);
    assertEquals(cu, af.getFullTextCu());
    assertEquals(cu, af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML));
  }			

}