/* $Id:

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/iop-released/2011/20-10-2015/0953-4075.tar.gz!/0953-4075/48/19/194001/b_48_19_194001.xml");
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

    
  }

}