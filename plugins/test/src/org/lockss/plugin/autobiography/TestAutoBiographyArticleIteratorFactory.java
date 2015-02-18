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

package org.lockss.plugin.autobiography;

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

public class TestAutoBiographyArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  
  private final String PLUGIN_NAME =
      "org.lockss.plugin.autobiography.ClockssAutoBiographyPlugin";
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
        PluginTestUtil.createAndStartAu(PLUGIN_NAME, abAuConfig());
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "jrnl");
    conf.put("volume_name", "12");
    conf.put("depth", "1");
    conf.put("branch", "4");
    conf.put("numFiles", "7");
    conf.put("fileTypes", "" + (
        SimulatedContentGenerator.FILE_TYPE_XML |
        SimulatedContentGenerator.FILE_TYPE_PDF |
        SimulatedContentGenerator.FILE_TYPE_HTML |
        SimulatedContentGenerator.FILE_TYPE_TXT));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  
  Configuration abAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", "http://www.example.com/");
    conf.put("journal_id", "jrnl");
    conf.put("volume_name", "12");
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.example.com/"),
        getRootUrls(artIter));
  }
  
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(pat, "http://www.wrong.com/jrnl_2000_12_3/index.htm");
    assertNotMatchesRE(pat, "http://www.wrong.com/jrnl_2000_12_3/10.5555_000000.htm");
    assertNotMatchesRE(pat, "http://www.example2.com/jrnl_2000_12_3/index.htm");
    assertNotMatchesRE(pat, "http://www.example2.com/jrnl_2000_12_3/10.5555_000000.htm");
    assertNotMatchesRE(pat, "http://www.example.com/jrnlwrong_2000_12_3/index.htm");
    assertNotMatchesRE(pat, "http://www.example.com/jrnlwrong_2000_12_3/10.5555_000000.htm");
    assertNotMatchesRE(pat, "http://www.example.com/wrong_2000_12_3/index.htm");
    assertNotMatchesRE(pat, "http://www.example.com/wrong_2000_12_3/10.5555_000000.htm");
    assertNotMatchesRE(pat, "http://www.example.com/jrnl_2000_wrong_3/index.htm");
    assertNotMatchesRE(pat, "http://www.example.com/jrnl_1991_wrong_3/10.5555_000000.htm");
    
    assertNotMatchesRE(pat, "http://www.example.com/jrnl_2000_12_3/index.htm");
    assertNotMatchesRE(pat, "http://www.example.com/jrnl_2000_12_3/10.5555_000000.xml");
    assertNotMatchesRE(pat, "http://www.example.com/jrnl_2000_12_3/10.5555_000000.txt");
    assertMatchesRE(pat, "http://www.example.com/jrnl_2000_12_3/10.5555_000000.htm");
    assertMatchesRE(pat, "http://www.example.com/jrnl_2000_12_12/10.5555_000000.htm");
    assertMatchesRE(pat, "http://www.example.com/jrnl_2000_12_12/10.5555_000000.pdf");
  }
  
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat1 = "branch(\\d+)/(\\d+file\\.xml)";
    String rep1 = "jrnl_2000_12_$1/10.555_0000000.xml";
    PluginTestUtil.copyAu(sau, au, ".*\\.xml$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+file\\.html)";
    String rep2 = "jrnl_2000_12_$1/10.555_0000000.htm";
    PluginTestUtil.copyAu(sau, au, ".*\\.html$", pat2, rep2);
    String pat3 = "branch(\\d+)/(\\d+file\\.txt)";
    String rep3 = "jrnl_2000_12_$1/10.555_0000000.txt";
    PluginTestUtil.copyAu(sau, au, ".*\\.txt$", pat3, rep3);
    String pat4 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep4 = "jrnl_2000_12_$1/10.555_0000000.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat4, rep4);
    
    String htmUrl = "http://www.example.com/jrnl_2000_12_1/10.555_0000000.htm";
    String pdfUrl = "http://www.example.com/jrnl_2000_12_1/10.555_0000000.pdf";
    String xmlUrl = "http://www.example.com/jrnl_2000_12_1/10.555_0000000.xml";
    String txtUrl = "http://www.example.com/jrnl_2000_12_1/10.555_0000000.txt";
    CachedUrl htmCu = au.makeCachedUrl(htmUrl);
    CachedUrl pdfCu = au.makeCachedUrl(pdfUrl);
    CachedUrl xmlCu = au.makeCachedUrl(xmlUrl);
    CachedUrl txtCu = au.makeCachedUrl(txtUrl);
    
    
    assertNotNull(htmCu);
    assertNotNull(pdfCu);
    assertNotNull(xmlCu);
    assertNotNull(txtCu);
    
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    assert(it.hasNext());
    ArticleFiles af = it.next();
    assertNotNull(af);
    assertEquals(htmCu.toString(), af.getFullTextCu().toString());
    assertEquals(htmCu.toString(), af.getRoleCu(ArticleFiles.ROLE_ABSTRACT).toString());
  }
  
}