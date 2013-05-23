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

package org.lockss.plugin.americaninstituteofphysics;

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

public class TestAmericanInstituteOfPhysicsSourceArticleIteratorFactory extends ArticleIteratorTestCase {
	
	private SimulatedArchivalUnit sau;	// Simulated AU to generate content
	
	private final String PLUGIN_NAME = "org.lockss.plugin.americaninstituteofphysics.ClockssAmericanInstituteOfPhysicsSourcePlugin";
	private static final int DEFAULT_FILESIZE = 3000;

  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
    
    String tempDirPath = setUpDiskSpace();
    
    sau = PluginTestUtil.createAndStartSimAu(simAuConfig(tempDirPath));
  }
  
  public void tearDown() throws Exception {
	    sau.deleteContentTree();
	    super.tearDown();
	  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, aipAuConfig());
  }
  
  Configuration simAuConfig(String rootPath) {
	    Configuration conf = ConfigManager.newConfiguration();
	    conf.put("root", rootPath);
	    conf.put("base_url", "http://www.example.com/");
	    conf.put("year", "2012");
	    conf.put("depth", "1");
	    conf.put("branch", "4");
	    conf.put("numFiles", "7");
	    conf.put("fileTypes",
	             "" + (  SimulatedContentGenerator.FILE_TYPE_XML
	                   | SimulatedContentGenerator.FILE_TYPE_PDF));
	    conf.put("binFileSize", ""+DEFAULT_FILESIZE);
	    return conf;
	  }
  
  Configuration aipAuConfig() {
	    return ConfigurationUtil.fromArgs("base_url",
				 "http://www.example.com/",
				 "year", "2012");
	  }

  public void testRoots() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertEquals(ListUtil.list("http://www.example.com/2012"),
		 getRootUrls(artIter));
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertNotMatchesRE(pat, "http://www.wrong.com/2012/AIP_xml_0.tar.gz!/FAKE/vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2011/AIP_xml_0.tar.gz!/FAKE/vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AxIxP_xml_0.tar.gz!/FAKE/vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AI_xml_0.tar.gz!/FAKE/vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/xml_0.tar.gz!/FAKE/vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xemal_0.tar.gz!/FAKE/vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0!/FAKE/vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.wrong.wrong!/FAKE/vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!//FAKE/vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!//vol_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!/FAKE/wrong_3/iss_2/10322_1.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!/FAKE/vol_3/wrong_2/10322_1.xml");
    assertMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!/FAKE/vol_3/iss_2/a10322_1.xml"); //some do have letters in last part
    assertMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!/FAKE/vol_3/iss_2/103a22_1.xml"); //some do have letters in last part
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!/FAKE/vol_3/iss_2/10322_2.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!/FAKE/vol_3/iss_2/10322_.xml");
    assertNotMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!/FAKE/vol_3/iss_2/10322_.pdf");
    assertMatchesRE(pat, "http://www.example.com/2012/AIP_xml_33.tar.gz!/FAKE/vol_33/iss_32/1403222_1.xml");
    assertMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!/DIFF/vol_3/iss_2/10322_1.xml");
    assertMatchesRE(pat, "http://www.example.com/2012/AIP_xml_0.tar.gz!/FAKE/vol_3/iss_2/1220322_1.xml");
    assertMatchesRE(pat, "http://www.example.com/2012/AIP_xml_03.tar.gz!/FAKE/vol_32/iss_22/10322_1.xml");
   }

  public void testCreateArticleFiles() throws Exception {
    PluginTestUtil.crawlSimAu(sau);
    String pat1 = "branch(\\d+)/(\\d+file\\.xml)";
    String rep1 = "AIP_xml_0.tar.gz!/FAKE/vol_00/iss_00/112304_1.xml";
    PluginTestUtil.copyAu(sau, au, ".*[^.][^p][^d][^f]$", pat1, rep1);
    String pat2 = "branch(\\d+)/(\\d+file\\.pdf)";
    String rep2 = "AIP_pdf_0.tar.gz!/FAKE/vol_00/iss_00/112304_1.pdf";
    PluginTestUtil.copyAu(sau, au, ".*\\.pdf$", pat2, rep2);
  
    String pdfUrl = "http://www.example.com/2012/AIP_pdf_0.tar.gz!/FAKE/vol_00/iss_00/112304_1.pdf";
    String url = "http://www.example.com/2012/AIP_xml_0.tar.gz!/FAKE/vol_00/iss_00/112304_1.xml";
    CachedUrl cu = au.makeCachedUrl(url);
    assertNotNull(cu);
    SubTreeArticleIterator artIter = createSubTreeIter();
    assertNotNull(artIter);
    ArticleFiles af = createArticleFiles(artIter, cu);
    assertNotNull(af);
    assertEquals(cu, af.getFullTextCu());
    
    //XXX:If this test is run, you must uncomment out the guessAdditionalFiles(...) call
    //in AmericanInstituteOfPhysicsSourceArticleIteratorFactory (Line ~90)
    //assertEquals(au.makeCachedUrl(pdfUrl), af.getRoleCu(ArticleFiles.ROLE_FULL_TEXT_PDF));
  }			

}