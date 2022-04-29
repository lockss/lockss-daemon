/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss.iop;

import java.util.regex.Pattern;
import org.lockss.daemon.*;

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

public class TestIopSourceArticleIteratorFactory extends ArticleIteratorTestCase {

  private final String PLUGIN_NAME = "org.lockss.plugin.clockss.iop.ClockssIopSourcePlugin";
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String BASE_URL = "http://clockss-ingest.lockss.org/sourcefiles/iop-released/";
  private static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private static final String YEAR = "2015";
  private final Configuration AU_CONFIG = 
    ConfigurationUtil.fromArgs(BASE_URL_KEY, BASE_URL,
    YEAR_KEY, YEAR);
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();

    au = createAu();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  protected ArchivalUnit createAu() throws ArchivalUnit.ConfigurationException {
    return
    PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
  }

  public void testUrlsWithPrefixes() throws Exception {
    SubTreeArticleIterator artIter = createSubTreeIter();
    Pattern pat = getPattern(artIter);

    assertNotMatchesRE(pat, "http://wrong-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0953-4075.tar.gz!/0953-4075/48/19/194001/b_48_19_194001.xml");
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2011/extra/20-10-2015/0953-4075.tar.gz!/0953-4075/48/19/194001/b_48_19_194001.xml");
    //excluding manifest xml files
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0953-4075.tar.gz!/0953-4075/48/19/194001/manifest.xml");
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0022-3727.tar.gz!/0022-3727/48/35/355104/manifest.xml");
    //all other article xml files
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0953-4075.tar.gz!/0953-4075/48/19/194001/b_48_19_194001.xml");
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0022-3727.tar.gz!/0022-3727/48/35/355104/d_48_35_355104.xml");
    // even ones which might have the word manifest in them though this hasn't been seen
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0953-4075.tar.gz!/0953-4075/48/19/194001/manifest_194001.xml");
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0953-4075.tar.gz!/0953-4075/48/19/194001/b_48_manifest_194001.xml");

    // for realsies
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0004-637X.tar.gz!/0004-637X/805/1/18/apj_805_1_18.pdf");
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0004-637X.tar.gz!/0004-637X/805/1/18/apj_805_1_18.xml");
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0004-637X.tar.gz!/0004-637X/805/1/18/apj_805_1_18am.pdf");
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0004-637X.tar.gz!/0004-637X/805/1/18/apj_805_1_18o.pdf");
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0004-637X.tar.gz!/0004-637X/805/1/18/manifest.xml");

    // support for new variants with iop-delivered content
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/20-10-2015/0004-637X.tar.gz!/0004-637X/805/1/18/apj_805_1_18.article");
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/HD1_1/0253-6102.tar.gz!/0253-6102/63/1/14/ctp_63_1_014.pdf");    
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/HD1_1/0253-6102.tar.gz!/0253-6102/63/1/14/ctp_63_1_014.article");    
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/HD1_1/0253-6102.tar.gz!/0253-6102/63/1/14/ctp_63_1_014.xml");    
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2015/HD1_1/0253-6102.tar.gz!/0253-6102/63/1/14/.artiel");    

    
  }

}