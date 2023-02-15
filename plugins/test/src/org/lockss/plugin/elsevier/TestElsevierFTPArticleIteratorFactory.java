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

package org.lockss.plugin.elsevier;

import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestElsevierFTPArticleIteratorFactory extends ArticleIteratorTestCase {
	
	private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
	private final String PLUGIN_NAME = "org.lockss.plugin.elsevier.ClockssElsevierSourcePlugin";
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
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, elsevierAuConfig());
  }
  
  Configuration simAuConfig(String rootPath) {
    ConfigurationUtil.addFromArgs(
        SimulatedContentGenerator.CONFIG_PREFIX + "doTarFile",
        "true");
	    Configuration conf = ConfigManager.newConfiguration();
	    conf.put("root", rootPath);
	    conf.put("base_url", "http://www.example.com/");
	    conf.put("year", "2012");
	    conf.put("depth", "1");
	    conf.put("branch", "4");
	    conf.put("numFiles", "7");
	    conf.put("fileTypes",
	             "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
	                   | SimulatedContentGenerator.FILE_TYPE_PDF));
	    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
	    return conf;
	  }
  
  Configuration elsevierAuConfig() {
	  	/*Configuration conf = ConfigManager.newConfiguration();
	  	conf.put("base_url", "http://www.example.com/");
	  	conf.put("year", "2012");
	  	return conf;*/
	    return ConfigurationUtil.fromArgs("base_url",
				 "http://www.example.com/", "year", "2012");
	  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.example.com/2012"),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(pat, "http://www.example.com/2012//0000000/0000000/000000/main.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/XXX00000//0000000/000000/main.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000//000000/main.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000/0000000//main.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000/000000/main.pdf");
    assertNotMatchesRE(pat, "http://www.wrong.com/2012/XXX00000/0000000.tar!/0000000/000000/main.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2013/XXX00000/0000000.tar!/0000000/000000/main.pdf");
    // the pattern is now much looser - based on depth, not whether all numeric
    assertMatchesRE(pat, "http://www.example.com/2012/XXX00000/X00000.tar!/0000000/000000/main.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/X00000/000000/main.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/000X0000/000000/main.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/0000000X/000000/main.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/0000000/Z000000/main.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/0000000/000Z000/main.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/0000000/000000Z/main.pdf");
    // these can have an X as the final character and so we allow it
    assertMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/0000000/000000X/main.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/0000000/000000/wrong.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/0000000/000000/main.wrong");
    assertNotMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000000.tar!/0000000/000000.tar");
    assertMatchesRE(pat, "http://www.example.com/2012/QQQ01230/0012300.tar!/00444400/0023232/main.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/XQ00/000.tar!/1231231/4564564/main.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/XXX00000/0000333.tar!/0000111/000222/main.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/XXX27620/0038294.tar!/00350003/11000423/main.pdf");
   }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat1 = "branch(\\d+)/(\\d+file\\.html)";
    String rep1 = "/XX00/00.tar!/00/00/main.xml";
    PluginTestUtil.copyAu(sau, au, ".*[^.][^p][^d][^f]$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep2 = "/XX00/00.tar!/00/00/main.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
  
    String url = "http://www.example.com/2012/XX00/00.tar!/00/00/main.xml";
    CachedUrl cu = au.makeCachedUrl(url);
    assertNotNull(cu);
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
  }

  public void testCopyTarArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with this plugin:
     */
    PluginTestUtil.copyAu(sau, au, "\\.tar",
        Arrays.asList(
            PluginTestUtil.makePatRep(
                "content.tar!/branch(\\d+)/(\\d+)file\\.html",
                "2012/XX00/000.tar!/0000$10/00$200/main.xml"
            ),
            PluginTestUtil.makePatRep(
                "content.tar!/branch(\\d+)/(\\d+)file\\.pdf",
                "2012/XX00/000.tar!/0000$10/00$200/main.pdf"
            )
        )
    );

    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int countAf = 0;
    int countFullTextPdf = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      log.debug("Af: " + af.toString());
      countAf ++;
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
        String url = cu.getUrl();
        String contentType = cu.getContentType();
        log.debug("countFullText url " + url + " " + contentType);
        if (url.endsWith(".pdf")) {
          ++countFullTextPdf;
        }
      }
    }

    assertEquals(28, countAf); // (5 x 4 branches)
    assertEquals(28, countFullTextPdf); // ensure there is a corresponding pdf fulltext
  }

}