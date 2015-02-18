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

package org.lockss.plugin.portlandpress;

import java.util.Iterator;
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

public class TestPortlandPressArticleIteratorFactory extends ArticleIteratorTestCase {
  
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
  
  private final String PLUGIN_NAME = "org.lockss.plugin.portlandpress.PortlandPressPlugin";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.biochemj.org/";
  private final String VOLUME_NAME = "399";
  private final String JOURNAL_ID = "bj";
  
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      "journal_id", JOURNAL_ID,
      VOLUME_NAME_KEY, VOLUME_NAME);
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
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME,  AU_CONFIG);
  }
  
  Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put(BASE_URL_KEY, BASE_URL);
    conf.put("depth", "1");
    conf.put("branch", "2");
    conf.put("numFiles", "3");
    conf.put("fileTypes",
        "" + (SimulatedContentGenerator.FILE_TYPE_HTML |
            SimulatedContentGenerator.FILE_TYPE_PDF ));
    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
    return conf;
  }
  
  
  public void testRoots() throws Exception {      
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + JOURNAL_ID + "/"),
        getRootUrls(artIter));
  }
  
  
  public void testUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(pat, "http://www.biochemj.org/bj/399/0009/bj3990009.htm"); //full text
    assertNotMatchesRE(pat, "http://www.biochemj.org/bj/399/0009/3990009.pdf"); //pdf not abstract - no jid at front
    assertMatchesRE(pat, "http://www.biochemj.org/bj/399/bj3990009.htm");
    assertNotMatchesRE(pat, "http://www.biochemj.org/bj/ev/046/bj3990009_ev.htm");
    assertNotMatchesRE(pat, "http://www.biochemj.org/bj/046/bj3990009add.htm"); //wrong volume
    assertNotMatchesRE(pat, "http://www.biochemj.org/bj/399/bj3990009add.htm"); //supplemental stuff page
    assertNotMatchesRE(pat, "http://www.clim-past.net/8/1/2012/cp-8-1-2012-supplement.pdf");
  }
  
  
  // simAU was created with only one depth, but 2 branches
  // 2 filetypes (html & pdf) and 2 files of each type
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    
    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with this plugin:
     *  <base_url>/bj/399/bj399###.htm
     *  <base_url>/bj/399/bj399###add.htm
     *  <base_url/bj/399/###/399###.pdf  (note - no bj at the front of filename!)
     *  <base_url/bj/399/###/bj399###.htm
     *  <base_url/bj/ev/399/###/bj399###_ev.htm
     *  <base_url>/48/<page#>/2011/art#file.{html, pdf & ris} 
     *  We have a depth of 1 and a branch of 2 to play with   
     */
    String pat1 = "branch(\\d+)/(\\d+)file[.]html";
    String rep1 = "bj/399/bj399$1$2.htm"; // make the html files abstract
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat1, rep1);
    
    String pat2 = "branch(\\d+)/(\\d+)file[.]pdf";
    String rep2 = "bj/399/$1$2/399$1$2.pdf"; //make the pdf files one level down pdf content
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat2, rep2);
    
    /* for the 01file, make full text html files as well*/
    String pat3 = "branch(\\d+)/([01]+)file[.]html";
    String rep3 = "bj/399/$1$2/bj399$1$2.htm"; // make the full text html
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat3, rep3);
    
    // for the 01file, make extended version html files
    String pat4 = "branch(\\d+)/([02]+)file[.]html";
    String rep4 = "bj/ev/399/$1$2/bj399$1$2.htm"; // make the full text html
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat4, rep4);
    
    // for the 01file, make 'add' html files
    String pat5 = "branch(\\d+)/([03]+)file[.]html";
    String rep5 = "bj/399/bj399$1$2add.htm";
    PluginTestUtil.copyAu(sau, au, ".*[.]html$", pat5, rep5);
    
    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    int count = 0;
    int countFullText= 0;
    int countMetadata = 0;
    int countHtm = 0;
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      count ++;
      //log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
        ++countFullText;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      if (cu != null) {
        ++countMetadata; //could be abstract or full
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_HTML);
      if (cu != null) {
        ++countHtm;
      }
    }
    // potential article count is 6
    int expCount = 6;
    
    log.debug3("Article count is " + count);
    assertEquals(expCount, count);
    
    // you will get a full text for combos with pdf or full
    assertEquals(expCount, countFullText);
    
    assertEquals(expCount, countMetadata);
    
    assertEquals(2, countHtm);
  }

}
