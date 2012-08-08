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

package org.lockss.plugin.ashdin;

import java.io.*;
import java.util.regex.Pattern;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.repository.*;
import org.lockss.plugin.*;
import org.lockss.plugin.ashdin.TestAshdinMetadataExtractorFactory.MySimulatedContentGenerator;
import org.lockss.plugin.simulated.*;


public class TestAshdinArticleIteratorFactory extends ArticleIteratorTestCase {
  static Logger log = Logger.getLogger("TestAshdinArticleIteratorFactory");

  // Simulated AU to generate content
  private SimulatedArchivalUnit sau;
  private final String PLUGIN_NAME = "org.lockss.plugin.ashdin.AshdinPlugin";
  private static final int DEFAULT_FILESIZE = 3000;
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
    return PluginTestUtil.createAndStartAu(PLUGIN_NAME, ashdinAuConfig());
  }

  public static class MySimulatedPlugin extends SimulatedPlugin {
    public ArchivalUnit createAu0(Configuration auConfig)
        throws ArchivalUnit.ConfigurationException {
      ArchivalUnit au = new SimulatedArchivalUnit(this);
      au.setConfiguration(auConfig);
      return au;
    }

    public SimulatedContentGenerator getContentGenerator(Configuration cf,
        String fileRoot) {
      return new MySimulatedContentGenerator(fileRoot);
    }

  }


  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", "http://www.ashdin.com/");
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes",
             "" + (SimulatedContentGenerator.FILE_TYPE_PDF));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  Configuration ashdinAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.ashdin.com/");
    conf.put("year", "2010");
    conf.put("journal_code", "acpsf");
    return conf;
  }
  

  public void testRoots() throws Exception {      
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.ashdin.com/journals"),
                 getRootUrls(artIter));
  }
  
  
  
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    assertNotMatchesRE(pat, "http://www.ashdin.com/journals/ACPSF/j0143.pdfwrong");
    assertNotMatchesRE(pat, "http://www.ashdin.com/journals/ACPSF/014174823t49006/j0143.pdfwrong");
    assertMatchesRE(pat, "http://www.ashdin.com/journals/ACPSF/235465.pdf");
    assertNotMatchesRE(pat, "http://www.example.com/content/");
    assertNotMatchesRE(pat, "http://www.example.com/content/j");
    assertNotMatchesRE(pat, "http://www.example.com/content/j0123/j383.pdfwrong");
  }
  
  
  

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
   String pat2 = "/journals/([^/]+)/([^/]+)\\.pdf";
    String rep2 = "/journals/ACPSF/235465.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
    String url = "http://www.ashdin.com/journals/ACPSF/235465.pdf";
    CachedUrl cu = au.makeCachedUrl(url);
    assertNotNull(cu);
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, cu);
    assertEquals(cu, af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF));
        
  }
 

}
