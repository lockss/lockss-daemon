/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.highwire;

import java.util.regex.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;

public class TestHighWireArticleIteratorFactory extends ArticleIteratorTestCase {
  static Logger log = Logger.getLogger(TestHighWireArticleIteratorFactory.class);

  private SimulatedArchivalUnit sau;	// Simulated AU to generate content

  private static String PLUGIN_NAME =
    "org.lockss.plugin.highwire.HighWirePressPlugin";

  private static String BASE_URL = "http://pediatrics.aappublications.org/";
  private static String SIM_ROOT = BASE_URL + "cgi/reprint/";

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();
    au = createAu();
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    // theDaemon.stopDaemon();
    super.tearDown();
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", SIM_ROOT);
    conf.put("depth", "0");
    conf.put("branch", "0");
    conf.put("numFiles", "2");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF |
        SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, ConfigurationUtil
        .fromArgs("base_url", "http://pediatrics.aappublications.org/",
            "volume_name", "52", "journal_issn", "1098-4275"));
  }


  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    System.out.println("Root Urls::" + getRootUrls(artIter));
    assertEquals(ListUtil.list( "http://pediatrics.aappublications.org/cgi/content/full/52/"
        ,"http://pediatrics.aappublications.org/cgi/reprint/52/"),
        getRootUrls(artIter));
  }
  
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertMatchesRE(pat,
        "http://pediatrics.aappublications.org/cgi/reprint/foo;52/Supplement_3/S69.pdf");
    assertMatchesRE(pat,
        "http://pediatrics.aappublications.org/cgi/reprint/52/supplement_3/S69.pdf");
   assertNotMatchesRE(pat,
        "http://pediatrics.aappublications.org/cgi/reprin/1014174823t49006/j0143.pdfwrong");
    assertNotMatchesRE(pat,
        "http://pediatrics.aappublications.org/cgi/reprintt/1014174823t49006/j0143.pdfwrong");
    assertNotMatchesRE(pat, "http://www.example.com/content/");
    assertNotMatchesRE(pat, "http://www.example.com/content/j");
    assertNotMatchesRE(pat,
        "http://www.example.com/content/j0123/j383.pdfwrong");
  }
  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    String pat0 = "001file[.]html";
    String rep0 = "52/1/S1";
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat0, rep0);
    String pat1 = "001file[.]pdf";
    String rep1 = "52/1/S1.pdf";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat1, rep1);
    
    String pdfurl = "http://pediatrics.aappublications.org/cgi/reprint/52/1/S1.pdf";
    String url = "http://pediatrics.aappublications.org/cgi/reprint/52/1/S1";
    
    au.makeCachedUrl(url);
    CachedUrl cu = au.makeCachedUrl(pdfurl);
    assertNotNull(cu);
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = artIter.next();
    assertNotNull(af);
    System.out.println("article files::" + af);
    assertEquals(url, af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF_LANDING_PAGE).getUrl());
    assertEquals(pdfurl, af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF).getUrl());
  }
}
