/* $Id$

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.pion;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import junit.framework.Test;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.CachedUrlSetSpec;
import org.lockss.daemon.SingleNodeCachedUrlSetSpec;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CachedUrlSetNode;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.SubTreeArticleIterator;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.test.ArticleIteratorTestCase;
import org.lockss.test.PrivilegedAccessor;
import org.lockss.util.ListUtil;
import org.lockss.util.StringUtil;

public class TestPionArticleIteratorFactory 
  extends ArticleIteratorTestCase {
  
  SimulatedArchivalUnit sau;
    
  private static final String PLUGIN_NAME = "org.lockss.plugin.pion.ClockssPionPlugin";
  private static final String BASE_URL = "http://www.envplan.com/";
  private static final String JOURNAL_CODE = "epa";
  private static final String SHORT_JOURNAL_CODE = "a";
  private static final String VOLUME_NAME = "42";
  
  private static final int DEFAULT_FILESIZE = 3000;
  
  
  public void setUp() throws Exception {
    super.setUp();
    
    // au is protected archival unit from super class ArticleIteratorTestCase
    au = createAu(); 
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }
  
  public void tearDown() throws Exception {
    sau.deleteContentTree();
    super.tearDown();
  }
  
  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
        PluginTestUtil.createAndStartAu(PLUGIN_NAME,  pionAuConfig());
  }
    
  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("base_url", BASE_URL);
    conf.put("depth", Integer.toString(1));
    conf.put("branch", Integer.toString(2));
    conf.put("numFiles", Integer.toString(4));
    conf.put("fileTypes",
        "" + (  SimulatedContentGenerator.FILE_TYPE_PDF        
            | SimulatedContentGenerator.FILE_TYPE_XML));
    conf.put("binFileSize", "" + DEFAULT_FILESIZE);
    return conf;
  }

  private Configuration pionAuConfig() {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("base_url", BASE_URL);
    conf.put("journal_code", JOURNAL_CODE);
    conf.put("short_journal_code", SHORT_JOURNAL_CODE);
    conf.put("volume_name", VOLUME_NAME);
    return conf;
  }
  
  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL + JOURNAL_CODE + "/"), 
                 getRootUrls(artIter));
  }

  protected Pattern getPattern(SubTreeArticleIterator iter) {
    try {
      return (Pattern)PrivilegedAccessor.getValue(iter, "pat");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void testUrls() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    // editorials
    assertMatchesRE(pat, "http://www.envplan.com/epa/editorials/a39367.pdf");
    // full-text
    assertMatchesRE(pat, "http://www.envplan.com/epa/fulltext/a42/a39367.pdf");

    // but not ...

    // supplimentary material
    assertNotMatchesRE(pat, "http://www.envplan.com/epa/misc/a42/a39367.pdf");

    // wrong base url
    assertNotMatchesRE(pat, "http://envplan.com/epa/fulltext/a42/a39367.pdf");
  }
  
  //
  // simAU was created with only one depth
  // 1 filetype (html) and 2 files of each type
  // So the total number of files of all types is 2
  // simAU file structures looks like this branch01/01file.html
  //
  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);

    /*
     *  Go through the simulated content you just crawled and modify the results to emulate
     *  what you would find in a "real" crawl with Pion:
     */
    
    // abstracts for all fulltext and editorials
    String pat1 = "branch([12])/(00[1234]+)file[.]pdf";
    String rep1 = "abstract.cgi?id="+SHORT_JOURNAL_CODE.toLowerCase()+"$1$2";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat1, rep1);
    
    // fulltext
    String pat2 = "branch([1])/(00[1234]+)file[.]pdf";
    String rep2 = JOURNAL_CODE + "/fulltext/"
                 +SHORT_JOURNAL_CODE.toLowerCase() + VOLUME_NAME + "/"
    		 +SHORT_JOURNAL_CODE.toLowerCase()+"$1$2.pdf";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat2, rep2);
    
    // editorials
    String pat3 = "branch([2])/(00[123]+)file[.]pdf";
    String rep3 = JOURNAL_CODE + "/editorials/"
                 +SHORT_JOURNAL_CODE.toLowerCase()+"$1$2.pdf";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat3, rep3);

    // citations for some fulltext and editorials
    String pat4 = "branch([12])/(00[123]+)file[.]pdf";
    String rep4 = "ris.cgi?id="+SHORT_JOURNAL_CODE.toLowerCase()+"$1$2";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat4, rep4);
    
    // supplimentary material for some fulltext
    String pat5 = "branch([1])/(00[123]+)file[.]pdf";
    String rep5 = "misc.cgi?id="+SHORT_JOURNAL_CODE.toLowerCase()+"$1$2";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat5, rep5);

    // references for all fulltext
    String pat6 = "branch([1])/(00[1234]+)file[.]pdf";
    String rep6 = "ref.cgi?id="+SHORT_JOURNAL_CODE.toLowerCase()+"$1$2";
    PluginTestUtil.copyAu(sau, au, ".*[.]pdf$", pat6, rep6);

    int count = 0;
    int countAbstract= 0;
    int countArticleMetadata = 0;
    int countCitation = 0;
    int countFullText = 0;
    int countFullTextPdf = 0;
    int countReferences = 0;
    int countSupplimentaryMaterials = 0;

    Iterator<ArticleFiles> it = au.getArticleIterator(MetadataTarget.Any());
    while (it.hasNext()) {
      ArticleFiles af = it.next();
      count ++;

      //log.info(af.toString());
      CachedUrl cu = af.getFullTextCu();
      if ( cu != null) {
        ++countFullText;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_ABSTRACT);
      if (cu != null) {
        ++countAbstract; 
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_ARTICLE_METADATA);
      if (cu != null) {
        ++countArticleMetadata;

      }
      cu = af.getRoleCu(ArticleFiles.ROLE_CITATION + "_Ris");
      if (cu != null) {
        ++countCitation;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF);
      if (cu != null) {
        ++countFullTextPdf;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_REFERENCES);
      if (cu != null) {
        ++countReferences;
      }
      cu = af.getRoleCu(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS);
      if (cu != null) {
        ++countSupplimentaryMaterials;
      }
    }

    assertEquals(7, count);
    assertEquals(7, countAbstract);
    assertEquals(7, countArticleMetadata);
    assertEquals(6, countCitation);
    assertEquals(7, countFullText);
    assertEquals(7, countFullTextPdf);
    assertEquals(4, countReferences);
    assertEquals(3, countSupplimentaryMaterials);
  }
}
