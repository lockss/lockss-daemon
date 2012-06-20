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

package org.lockss.plugin.libertasacademica;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
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
 * PDF Full Text: http://www.la-press.com/redirect_file.php?fileType=pdf&fileId=4199&filename=3103-ACI-Holistic-Control-of-Herbal-Teas-and-Tinctures-Based-on-Sage-(Salvia-of.pdf&nocount=1
 * HTML Abstract: http://www.la-press.com/holistic-control-of-herbal-teas-and-tinctures-based-on-sage-salvia-off-article-a3103
 * <meta content="http://la-press.com/redirect_file.php?fileId=4199&filename=3103-ACI-Holistic-Control-of-Herbal-Teas-and-Tinctures-Based-on-Sage-(Salvia-of.pdf&fileType=pdf" name="citation_pdf_url">
 */
public class TestLibertasAcademicaArticleIteratorFactory extends ArticleIteratorTestCase {
	
	private static Logger log = Logger.getLogger("TestLibertasAcademicaArticleIteratorFactor");
	private final String ARTICLE_FAIL_MSG = "Article files not created properly";
	private final String PATTERN_FAIL_MSG = "Article file URL pattern changed or incorrect";
	private final String PLUGIN_NAME = "org.lockss.plugin.libertasacademica.LibertasAcademicaPlugin";
	static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
	static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
	static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
	private final String BASE_URL = "http://www.la-press.com/";
	private final String YEAR = "2010";
	private final String JOURNAL_ID = "11";
	private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
												BASE_URL_KEY, BASE_URL,
												YEAR_KEY, YEAR,
												JOURNAL_ID_KEY, JOURNAL_ID);
	private static final int DEFAULT_FILESIZE = 3000;

  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
    
    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    ConfigurationUtil.setFromArgs(LockssRepositoryImpl.PARAM_CACHE_LOCATION,
				  tempDirPath);
    
  }
  
  public void tearDown() throws Exception {
	    super.tearDown();
	  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals("Article file root URL pattern changed or incorrect" ,ListUtil.list( BASE_URL),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, "http://www.wrong.com/hello-world-article-a123");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "hello-world-article-");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "/hello-world-article-a123");
    assertNotMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "hello-world-articl-a123");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "hello-world-article-a123");
    assertMatchesRE(PATTERN_FAIL_MSG, pat, BASE_URL + "hello-world-foo-bar-article-a4");
   }

  public void testCreateArticleFiles() throws Exception {
    String[] urls = {
					    BASE_URL + "hello-world-foo-bar-article-a12",
					    BASE_URL + "foo-bar-hello-world-article-a1234",
					    BASE_URL + "redirect_file.php?fileType=pdf&fileId=1111&filename=Hello-World-(Foo.pdf&nocount=1",
					    BASE_URL + "redirect_file.php?fileType=pdf&fileId=2222&filename=1234-Foo-Bar-(Hello.pdf&nocount=1",
					    BASE_URL + "no-pdf-article-a21",
					    BASE_URL + "bad-pdf-article-a22",
					    BASE_URL + "lockss.php?t=lockss&pa=issue&j_id=1&year=2010",
					    BASE_URL + "no-content-article-a23"
    				};
    
	    for(String url : urls)
	    {
		    UrlCacher uc = au.makeUrlCacher(url);
		    if(url.contains("-article-a12")){
		    	uc.storeContent(getAbsAlteredInputStream("http://la-press.com/redirect_file.php?fileId=1111&filename=Hello-World-(Foo.pdf&fileType=pdf"), getAbsProperties());
			} else if(url.contains("-article-a1234")) {
				uc.storeContent(getAbsAlteredInputStream("http://la-press.com/redirect_file.php?fileId=2222&filename=1234-Foo-Bar-(Hello.pdf&fileType=pdf"), getAbsProperties());
			} else if(url.contains("-article-a21")) {
				uc.storeContent(getAbsAlteredInputStream(null), getAbsProperties());
			} else if(url.contains("-article-a22")) {
				uc.storeContent(getAbsAlteredInputStream("http://la-press.com/redirect_file.php?fileId=3333&filename=Bad-PDF.pdf&fileType=pdf"), getAbsProperties());
			}
	    }
    
    Stack<String[]> expStack = new Stack<String[]>();
    String [] af1 = {
    				BASE_URL + "redirect_file.php?fileType=pdf&fileId=1111&filename=Hello-World-(Foo.pdf&nocount=1",
    				BASE_URL + "hello-world-foo-bar-article-a12"
		    		};
    String [] af2 = {
					BASE_URL + "redirect_file.php?fileType=pdf&fileId=2222&filename=1234-Foo-Bar-(Hello.pdf&nocount=1",
    				BASE_URL + "hello-world-foo-bar-article-a1234"
		    		};
								
    expStack.push(af2);
    expStack.push(af1);
    
    for ( SubTreeArticleIterator artIter = createSubTreeIter(); artIter.hasNext(); ) 
    {
    		ArticleFiles af = artIter.next();
    		String[] act = {
					af.getFullTextUrl(),
					af.getRoleUrl(ArticleFiles.ROLE_ABSTRACT)
					};
    		String[] exp = expStack.pop();
    		if(act.length == exp.length){
    			for(int i = 0;i< act.length; i++){
	    			assertEquals(ARTICLE_FAIL_MSG + " Expected: " + exp[i] + " Actual: " + act[i], exp[i],act[i]);
	    		}
    		}
    		else fail(ARTICLE_FAIL_MSG + " length of expected and actual ArticleFiles content not the same:" + exp.length + "!=" + act.length);
    }
    
  }
  
	private CIProperties getAbsProperties() {
		CIProperties absProps = new CIProperties();
		absProps.put("RESPONSE","HTTP/1.0 200 OK");
		absProps.put("Date", "Fri, 06 Apr 2012 18:22:49 GMT");
		absProps.put("Server", "Apache/2.2.3 (CentOS)");
		absProps.put("X-Powered-By", "PHP/5.2.17");
		absProps.put("Expires", "Thu, 19 Nov 1981 08:52:00 GMT");
		absProps.put("Cache-Control", "no-store, no-cache, must-revalidate, post-check=0, pre-check=0");
		absProps.put("Pragma", "no-cache");
		absProps.put("Content-Type", "text/html; charset=UTF-8");
		absProps.put("X-Cache", "MISS from lockss.org");
		absProps.put("X-Cache-Lookup", "MISS from lockss.org:8888");
		absProps.put("Via", "1.1 lockss.org:8888 (squid/2.7.STABLE7)");
		absProps.put("Connection", "close");
		return absProps;
	}
	
	private InputStream getAbsAlteredInputStream(String url) throws IOException {
		InputStream htmlIn = getClass().getResourceAsStream("abstract.html");
		if(url == null) {
			return htmlIn;
		} else if (htmlIn == null) {
			log.debug("Unable to load test rescource");
			return null;
		} else {
			return new AlteredInputStream(htmlIn, "<meta name=\"citation_pdf_url\" content=\"" + url +"\">", "<Hello World/>");
		}
	}
	//Replaces the all the first instance of oldText with newText when reading form a string
	private class AlteredInputStream extends InputStream {
		byte[] newBytes;
		byte[] oldBytes;
		ArrayList<Integer> buffer;
		ArrayList<Integer> alterBuffer;
		ArrayList<Integer> oldByteList;
		ArrayList<Integer> newByteList;
		Boolean alterStream = false;
		InputStream in;
		
		public AlteredInputStream(InputStream in, String newText, String oldText) throws IOException {
			newBytes = newText.getBytes();
			oldBytes = oldText.getBytes();
			buffer = new ArrayList<Integer>(oldBytes.length);
			this.in = in;
			for(int i = 0; i < oldBytes.length; i++) {
				 buffer.add(in.read());
			}
			newByteList = new ArrayList<Integer>(newBytes.length);
			for (int i : newBytes) {
				newByteList.add(i);
			}
			alterBuffer = newByteList;
			oldByteList = new ArrayList<Integer>(oldBytes.length);
			for (int i : oldBytes) {
				alterBuffer.add(i);
			}
		}

		@Override
		public int read() throws IOException {
			if (Arrays.equals(buffer.toArray(), oldByteList.toArray())) {
				buffer = new ArrayList<Integer>(oldBytes.length);
				for(int i = 0; i < oldBytes.length; i++) {
					 buffer.add(in.read());
				}
				alterStream = true;
			}
			if (alterBuffer.isEmpty()) {
				alterStream = false;
				alterBuffer = newByteList;
			}
			if (alterStream) {
				return (Integer)alterBuffer.remove(0);
			} else {
				buffer.add(in.read());
				return (Integer)buffer.remove(0);
			}
		}
		
	}
}
