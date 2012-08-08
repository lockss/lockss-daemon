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

package org.lockss.plugin.massachusettsmedicalsociety;

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

/*
 * HTML Full Text: http://www.nejm.org/doi/full/10.1056/NEJMoa042957
 * PDF Full Text: http://www.nejm.org/doi/pdf/10.1056/NEJMoa042957
 * Citation (containing metadata): www.nejm.org/action/downloadCitation?format=(ris|endnote|bibTex|medlars|procite|referenceManager)&doi=10.1056%2FNEJMoa042957&include=cit&direct=checked
 * Supplemental Meterials: http://www.nejm.org/doi/suppl/10.1056/NEJMoa042957/suppl_file/nejmoa042957_(disclosures|protocol|appendix).pdf
 */
public class TestMassachusettsMedicalSocietyArticleIteratorFactory extends ArticleIteratorTestCase {
	
	private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	private final String ARTICLE_FAIL_MSG = "Article files not created properly";
	private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
	private final String PLUGIN_NAME = "org.lockss.plugin.massachusettsmedicalsociety.MassachusettsMedicalSocietyPlugin";
	static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
	static final String BASE_URL2_KEY = ConfigParamDescr.BASE_URL2.getKey();
	static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
	static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
	private final String BASE_URL = "http://www.example.com/";
	private final String BASE_URL2 = "http:/cdn.example.com/";
	private final String VOLUME_NAME = "352";
	private final String JOURNAL_ID = "nejm";
	private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
												BASE_URL_KEY, BASE_URL,
												BASE_URL2_KEY, BASE_URL2,
												VOLUME_NAME_KEY, VOLUME_NAME,
												JOURNAL_ID_KEY, JOURNAL_ID);
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
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }
  
  Configuration simAuConfig(String rootPath) {
	    Configuration conf = ConfigManager.newConfiguration();
	    conf.put("root", rootPath);
	    conf.put(BASE_URL_KEY, BASE_URL);
	    conf.put("depth", "1");
	    conf.put("branch", "2");
	    conf.put("numFiles", "6");
	    conf.put("fileTypes",
	             "" + (  SimulatedContentGenerator.FILE_TYPE_HTML
	                   | SimulatedContentGenerator.FILE_TYPE_PDF));
	    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
	    return conf;
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals("Article file root URL pattern changed or incorrect" ,ListUtil.list( BASE_URL + "doi"),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.wrong.com/doi/full/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "dooi/full/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "/full/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi1/full/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "//10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdfplus/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/ful/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/124/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.wrong.com/doi/pdf/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "dooi/pdf/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "/pdf/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi1/pdf/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/abs/10.5339/nejm12315");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/suppl/10.5339123/nejm12315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/full/10.5339/nejm12315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/full/10.533329/nejm123324315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/full/1023.5339/nejmb123b315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/full/10232.533339/nejm12315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdf/10.5339/nejm12315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdf/10.533329/nejm123324315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdf/1023.5339/nejmb123b315");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "doi/pdf/10232.533339/nejm12315");
   }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String[] urls = {
					    BASE_URL + "doi/pdf/10.5339/nejm1231",
					    BASE_URL + "doi/suppl/10.5339/nejm1231/suppl_file/nejm1231_appendix.pdf",
					    BASE_URL + "action/downloadCitation?format=ris&doi=10.5339%2Fnejm1231&include=cit&direct=checked",
					    BASE_URL + "doi/full/10.5339/nejm12345",
					    BASE_URL + "doi/pdf/10.5339/nejm1231113",
					    BASE_URL + "doi/full/10.5339/nejm1231113",
					    BASE_URL + "doi/media/10.5339/nejm1231113",
					    BASE_URL + "doi/image/10.5339/nejm1231113",
					    BASE_URL + "action/downloadCitation?format=ris&doi=10.5339%2Fnejm1231113&include=cit&direct=checked",
					    BASE_URL + "doi/media/10.5339/nejm12315002",
					    BASE_URL + "doi/pdf/10.5339/nejm123456",
					    BASE_URL + "doi/full/10.5339/nejm123456",
					    BASE_URL + "doi/suppl/10.5339/nejm123456/suppl_file/nejm123456_appendix.pdf",
					    BASE_URL + "action/downloadCitation?format=ris&doi=10.5339%2Fnejm123456&include=cit&direct=checked",
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
    				BASE_URL + "doi/full/10.5339/nejm1231113",
		    		BASE_URL + "doi/pdf/10.5339/nejm1231113",
		    		BASE_URL + "doi/full/10.5339/nejm1231113",
		    		null,
		    		BASE_URL + "action/downloadCitation?format=ris&doi=10.5339%2Fnejm1231113&include=cit&direct=checked",
		    		null
		    		};
    String [] af2 = {
    				BASE_URL + "doi/full/10.5339/nejm12345",
    				null,
    				BASE_URL + "doi/full/10.5339/nejm12345",
    				null,
    				null,
    				null
		    		};
    String [] af3 = {
    				BASE_URL + "doi/full/10.5339/nejm123456",
    				BASE_URL + "doi/pdf/10.5339/nejm123456",
    				BASE_URL + "doi/full/10.5339/nejm123456",
    				null,
    				BASE_URL + "action/downloadCitation?format=ris&doi=10.5339%2Fnejm123456&include=cit&direct=checked",
    				BASE_URL + "doi/suppl/10.5339/nejm123456/suppl_file/nejm123456_appendix.pdf"
		    		};
    String [] af4 = {
    				BASE_URL + "doi/pdf/10.5339/nejm1231",
    				BASE_URL + "doi/pdf/10.5339/nejm1231",
    				null,
    				null,
    				BASE_URL + "action/downloadCitation?format=ris&doi=10.5339%2Fnejm1231&include=cit&direct=checked",
    				BASE_URL + "doi/suppl/10.5339/nejm1231/suppl_file/nejm1231_appendix.pdf"
		    		};
    expStack.push(af4);
    expStack.push(af3);
    expStack.push(af2);
    expStack.push(af1);
    
    for ( SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) 
    {
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
