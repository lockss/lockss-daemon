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

package org.lockss.plugin.atypon.ammonsscientific;

import java.io.File;
import java.util.Iterator;
import java.util.Stack;
import java.util.regex.Pattern;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestAmmonsScientificArticleIteratorFactory extends ArticleIteratorTestCase {
	
  private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
  private final String PLUGIN_NAME = "org.lockss.plugin.ammonsscientific.ClockssAmmonsScientificPlugin";
  private final String ARTICLE_FAIL_MSG = "Article files not created properly";
  private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String BASE_URL2_KEY = ConfigParamDescr.BASE_URL2.getKey();
  static final String JOURNAL_ABBR_KEY = ConfigParamDescr.JOURNAL_ABBR.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  private final String BASE_URL = "http://www.example.com/";
  private final String BASE_URL2 = "http:/cdn.example.com/";
  private final String VOLUME_NAME = "108";
  private final String JOURNAL_ABBR = "PR0";
  private final String CRAWLRULE0 = String.format("toc/%s/%s/", JOURNAL_ABBR, VOLUME_NAME);
  private final String CRAWLRULE1 = String.format("doi/(pdf|pdfplus|abs|full|suppl)/\\d+\\.\\d+/(\\d+\\.){0,5}%s\\.%s.*", JOURNAL_ABBR, VOLUME_NAME);
  private static final int DEFAULT_FILESIZE = 3000;
  private static Pattern regexpattern;
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
            BASE_URL2_KEY, BASE_URL2,
            VOLUME_NAME_KEY, VOLUME_NAME,
            JOURNAL_ABBR_KEY, JOURNAL_ABBR);

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
    PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }
  
  Configuration simAuConfig(String rootPath) {
	    Configuration conf = ConfigManager.newConfiguration();
	    conf.put("root", rootPath);
	    conf.put(BASE_URL_KEY, BASE_URL);
	    conf.put("journal_abbr", JOURNAL_ABBR);
	    conf.put("volume_name", VOLUME_NAME);
	    conf.put("depth", "1");
	    conf.put("branch", "4");
	    conf.put("numFiles", "7");
	    conf.put("fileTypes",
	             "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
	                   | SimulatedContentGenerator.FILE_TYPE_PDF));
	    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
	    return conf;
	  }
 

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list(BASE_URL), getRootUrls(artIter));
  }

  public void testCrawlRules() throws Exception {
    //SubTreeArticleIterator artIter = createSubTreeIter();
    //Pattern pat = getPattern(artIter);
    // use a substance pattern from the crawl rules
    String re_pattern =BASE_URL+String.format(CRAWLRULE1);    
    regexpattern = Pattern.compile(re_pattern);
    Pattern pat = regexpattern;

    assertNotMatchesRE(pat, "http://www.wrong.com/doi/abs/10.2446/01.02.03.PR0.108.4.567-589");
    assertNotMatchesRE(pat, BASE_URL + "dooi/abs/10.2446/01.02.03.PR0.108.4.567-589");
    assertNotMatchesRE(pat, BASE_URL + "doi/abs/x.2446/01.02.03.PR0.108.4.567-589");
    assertNotMatchesRE(pat, BASE_URL + "doi/hello/10.2446/01.02.03.108.PR0.4.567-589");  
    assertNotMatchesRE(pat, BASE_URL + "doi/abs/10.2446/01.02.03.PRo.4.567-589");
    assertNotMatchesRE(pat, BASE_URL + "doi/pdf/10.2466/PR0.110.2.353-354");
    assertMatchesRE(pat, BASE_URL + "doi/abs/10.2446/0001.2.98.PR0.108.4.567-589");
    assertMatchesRE(pat, BASE_URL + "doi/pdf/10.2466/PR0.108.2.353-354");
    assertMatchesRE(pat, BASE_URL + "doi/pdfplus/10.2446/01.02.03.04.PR0.108.409.567-54489");
    assertMatchesRE(pat, BASE_URL + "doi/suppl/10.2446/01.02.03.PR0.108.4.56237-589");
    
    re_pattern =BASE_URL+String.format(CRAWLRULE0);    
    regexpattern = Pattern.compile(re_pattern);
    pat = regexpattern;

    assertMatchesRE(pat, BASE_URL + "toc/PR0/108/3");
    assertNotMatchesRE(pat, BASE_URL + "doi/abs/10.2446/0001.2.98.PR0.108.4.567-589");

  }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String[] urls = {
        BASE_URL + "doi/pdf/10.2466/PR01231",
        BASE_URL + "doi/suppl/10.2466/PR01231/suppl_file/PR01231_appendix.pdf",
        BASE_URL + "action/downloadCitation?format=ris&doi=10.2466%2FPR01231&include=cit&direct=checked",
        BASE_URL + "doi/full/10.2466/PR012345",
        BASE_URL + "doi/pdf/10.2466/PR01231113",
        BASE_URL + "doi/full/10.2466/PR01231113",
        BASE_URL + "doi/media/10.2466/PR01231113",
        BASE_URL + "doi/image/10.2466/PR01231113",
        BASE_URL + "action/downloadCitation?format=ris&doi=10.2466%2FPR01231113&include=cit&direct=checked",
        BASE_URL + "doi/media/10.2466/PR012315002",
        BASE_URL + "doi/pdf/10.2466/PR0123456",
        BASE_URL + "doi/full/10.2466/PR0123456",
        BASE_URL + "doi/suppl/10.2466/PR0123456/suppl_file/PR0123456_appendix.pdf",
        BASE_URL + "action/downloadCitation?format=ris&doi=10.2466%2FPR0123456&include=cit&direct=checked",
        BASE_URL + "doi/",
        BASE_URL + "bq/352/12"
    };
    Iterator<CachedUrlSetNode> cuIter = sau.getAuCachedUrlSet().contentHashIterator();
    
    if(cuIter.hasNext()){
            CachedUrlSetNode cusn = cuIter.next();
            CachedUrl cuPdf = null;
            CachedUrl cuHtml = null;
            UrlCacher uc;
            while(cuIter.hasNext() && (cuPdf == null || cuHtml == null))
                {
                if(cusn.getType() == CachedUrlSetNode.TYPE_CACHED_URL && cusn.hasContent())
                {
                        CachedUrl cu = (CachedUrl)cusn;
                        if(cuPdf == null && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_PDF))
                        {
                                cuPdf = cu;
                        }
                        else if (cuHtml == null && cu.getContentType().toLowerCase().startsWith(Constants.MIME_TYPE_HTML))
                        {
                                cuHtml = cu;
                        }
                }
                cusn = cuIter.next();
                }
            for(String url : urls)
            {
                    uc = au.makeUrlCacher(url);
                    if(url.contains("pdf")){
                        uc.storeContent(cuPdf.getUnfilteredInputStream(), cuPdf.getProperties());
                    }
                    else if(url.contains("full") || url.contains("ris")){
                        uc.storeContent(cuHtml.getUnfilteredInputStream(), cuHtml.getProperties());
                    }
            }
    }
  
    Stack<String[]> expStack = new Stack<String[]>();
    String [] af1 = {
                              BASE_URL + "doi/full/10.2466/PR01231113",
                              BASE_URL + "doi/pdf/10.2466/PR01231113",
                              BASE_URL + "doi/full/10.2466/PR01231113",
                              null,
                              BASE_URL + "action/downloadCitation?format=ris&doi=10.2466%2FPR01231113&include=cit&direct=checked",
                              null
                              };
    String [] af2 = {
                              BASE_URL + "doi/full/10.2466/PR012345",
                              null,
                              BASE_URL + "doi/full/10.2466/PR012345",
                              null,
                              null,
                              null
                              };
    String [] af3 = {
                              BASE_URL + "doi/full/10.2466/PR0123456",
                              BASE_URL + "doi/pdf/10.2466/PR0123456",
                              BASE_URL + "doi/full/10.2466/PR0123456",
                              null,
                              BASE_URL + "action/downloadCitation?format=ris&doi=10.2466%2FPR0123456&include=cit&direct=checked",
                              BASE_URL + "doi/suppl/10.2466/PR0123456/suppl_file/PR0123456_appendix.pdf"
                              };
    String [] af4 = {
                              BASE_URL + "doi/pdf/10.2466/PR01231",
                              BASE_URL + "doi/pdf/10.2466/PR01231",
                              null,
                              null,
                              BASE_URL + "action/downloadCitation?format=ris&doi=10.2466%2FPR01231&include=cit&direct=checked",
                              BASE_URL + "doi/suppl/10.2466/PR01231/suppl_file/PR01231_appendix.pdf"
                              };
    expStack.push(af4);
    expStack.push(af3);
    expStack.push(af2);
    expStack.push(af1);
  
    for ( SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) {
              ArticleFiles af = artIter.next();
              String[] act = {
                                      af.getFullTextUrl(),
                                      af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_PDF),
                                      af.getRoleUrl(ArticleFiles.ROLE_FULL_TEXT_HTML),
                                      af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT),
                                      af.getRoleUrl(ArticleFiles.ROLE_CITATION),
                                      af.getRoleUrl(ArticleFiles.ROLE_SUPPLEMENTARY_MATERIALS)
                                      };
              String[] exp = expStack.pop();
              if(act.length == exp.length){
                      for(int i = 0;i< act.length; i++){
                              assertEquals(ARTICLE_FAIL_MSG + " Expected: " + exp[i] + " Actual: " + act[i], exp[i],act[i]);
                      }
              }
              else fail(ARTICLE_FAIL_MSG + " length of expected and actual ArticleFiles content not the same");
    }     
  
  }                     

}