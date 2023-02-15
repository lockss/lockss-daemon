/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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