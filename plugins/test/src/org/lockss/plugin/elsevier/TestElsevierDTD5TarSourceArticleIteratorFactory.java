/*
 * $Id$
 */
/*

/*

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

package org.lockss.plugin.elsevier;

import java.util.regex.Pattern;

import org.lockss.config.Configuration;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;
import org.lockss.test.*;

public class TestElsevierDTD5TarSourceArticleIteratorFactory extends ArticleIteratorTestCase {
	
	
  private final String PLUGIN_NAME = "org.lockss.plugin.elsevier.ClockssElsevierDTD5SourcePlugin";
  private final String BASE_URL = "http://www.example.com/";
  private final String YEAR = "2014";
  private  final String TAR_A_BASE = BASE_URL + "CLKS3A.tar";
  private  final String TAR_B_BASE = BASE_URL + "CLKS3B.tar"; 
  private  final String TAR_C_BASE = BASE_URL + "CLKS3C.tar"; 
  

  private final Configuration AU_CONFIG = 
      ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
          YEAR_KEY, YEAR);
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();

  public void setUp() throws Exception {
    super.setUp();
    au = createAu();
  }
  
  public void tearDown() throws Exception {
	    super.tearDown();
	  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
      PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }
  
  // The article iterator picks up all top-level dataset.xml files and all end-level main.xml files
  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);
    
    assertMatchesRE(pat,"http://www.example.com/2014/CLKS0000000000003A.tar!/CLKS0000000000003/dataset.xml");
    // this will never happen, but it's not excluded in the pattern
    assertMatchesRE(pat,"http://www.example.com/2014/CLKS0000000000003B.tar!/CLKS0000000000003/dataset.xml");
    assertMatchesRE(pat,"http://www.example.com/2014/CLKS0000000000237A.tar!/CLKS0000000000003/dataset.xml");
    assertMatchesRE(pat,"http://www.example.com/2014/CLKS0000000000237A.tar!/CLKS0000000000003/21735794/v89i9/S2173579414001698/main.xml");
    // these shouldn't get picked up...
    assertNotMatchesRE(pat,"http://www.example.com/2014/CLKS0000000000237A.tar!/CLKS0000000000003/21735794/v89i9/issue.xml");
    assertNotMatchesRE(pat,"http://www.example.com/2014/CLKS0000000000237A.tar!/CLKS0000000000003/21735794/v89i9/S2173579414001698/dataset.xml");
   }


}