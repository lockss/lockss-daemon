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

package org.lockss.plugin.clockss.cambridge;

import java.util.regex.Pattern;
import org.lockss.daemon.*;

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.test.*;

public class TestCambridgeArticleIteratorFactory extends ArticleIteratorTestCase {

  private final String PLUGIN_NAME = "org.lockss.plugin.clockss.cambridge.ClockssCambridgeUniversityPressSourcePlugin";
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String BASE_URL = "http://clockss-ingest.lockss.org/sourcefiles/cambridge-released/";
  private static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  private static final String YEAR = "2019";
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

    assertNotMatchesRE(pat, "http://wrong-ingest.lockss.org/sourcefiles/cambridge-released/2019/HTR94_04.zip!/HTR/HTR94_04/S0017816001038019h.xml");
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/cambridge-released/2011/HTR94_04.zip!/HTR/HTR94_04/S0017816001038019h.xml");
    //accept h.xml and plain .xml files - plain variant added in 2019
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/cambridge-released/2019/HTR94_04.zip!/HTR/HTR94_04/S0017816001038019h.xml");
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/cambridge-released/2019/0967-1994_ZYG_17_4.zip!/S0967199409990232.xml");
    //but not w.xml files (which uses negative lookahead to avoid) 
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/cambridge-released/2019/HTR94_04.zip!/HTR/HTR94_04/S0017816001038019w.xml");
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/cambridge-released/2019/HTR94_04.zip!/HTR/HTR94_04/S0017816001038019w.xml");

    //haven't seen this, but in case we get a plain xml that doesn't end in just numberes (which is why using negative lookahead instead of ([0-9]|h)\.xml
    assertMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/cambridge-released/2019/0967-1994_ZYG_17_4.zip!/S0967199409990xyz.xml");
    
    // and don't find the pdf files with the iterator pattern, just by substitution
    assertNotMatchesRE(pat, "http://clockss-ingest.lockss.org/sourcefiles/cambridge-released/2019/HTR94_04.zip!/HTR/HTR94_04/S0017816001038019a.pdf");
    
  }

}