/*
 $Id:$

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.europeanmathematicalsociety;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseCachedUrlSet;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.*;

public class TestEuropeanMathematicalSocietyArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();

  private static final String BASE_URL = "http://www.ems-ph.org/";
  private static final String JOURNAL_ISSN = "1234-6789";
  private static final String VOLUME = "21";
  private static final String JOURNAL_ID = "jid";
  private final Configuration AU_CONFIG = ConfigurationUtil.fromArgs(
      BASE_URL_KEY, BASE_URL,
      VOLUME_NAME_KEY, VOLUME,
      JOURNAL_ISSN_KEY, JOURNAL_ISSN,
      JOURNAL_ID_KEY, JOURNAL_ID);
  
  private static final Logger log = Logger.getLogger(TestEuropeanMathematicalSocietyArchivalUnit.class);
  
  static final String PLUGIN_ID = "org.lockss.plugin.europeanmathematicalsociety.EuropeanMathematicalSocietyPlugin";
  static final String PluginName = "European Mathematical Society Journals Plugin";
  
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }
  
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  private DefinableArchivalUnit makeAu()
      throws Exception {
    
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(), PLUGIN_ID);
    DefinableArchivalUnit au = (DefinableArchivalUnit)ap.createAu(AU_CONFIG);
    return au;
  }
  
  //
  // Test the crawl rules
  //
  
  public void testShouldCacheProperPages() throws Exception {
    ArchivalUnit  msau = makeAu();
    theDaemon.getLockssRepository(msau);
    theDaemon.getNodeManager(msau);
    BaseCachedUrlSet cus = new BaseCachedUrlSet(msau,
        new RangeCachedUrlSetSpec(BASE_URL));
    
    // manifest page and TOC
    shouldCacheTest(BASE_URL+"journals/all_issues.php?issn="+JOURNAL_ISSN, true, msau, cus);
    
    // contents
    shouldCacheTest(BASE_URL+"journals/show_issue.php?issn="+JOURNAL_ISSN+"&vol="+VOLUME+"&iss=3", true, msau, cus);
    shouldCacheTest(BASE_URL+"journals/show_abstract.php?issn="+JOURNAL_ISSN+"&vol="+VOLUME+"&iss=4&rank=4", true, msau, cus);
    shouldCacheTest(BASE_URL+"journals/show_pdf.php?issn="+JOURNAL_ISSN+"&vol="+VOLUME+"&iss=4&rank=4", true, msau, cus);
    // shouldCacheTest(BASE_URL+"journals/abstract/"+JOURNAL_ID+"/2010-001-001/2010-001-001-01.pdf", true, msau, cus);
    
    // images (etc.) 
    shouldCacheTest(BASE_URL+"img/libraries/cluetip/images/arrowleft.gif", true, msau, cus);
    shouldCacheTest(BASE_URL+"css/progress.css", true, msau, cus);
    
    // excluded
    shouldCacheTest(BASE_URL+"show_issue.php?issn=1663-487X&vol=1&iss=3", false, msau, cus);
    shouldCacheTest(BASE_URL+"journals/title_index.php?jrn=qt", false, msau,cus);
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache,
      ArchivalUnit au, CachedUrlSet cus) {
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
  public void testStartUrlConstruction() throws Exception {
    String expected = BASE_URL + "journals/all_issues.php?issn=" + JOURNAL_ISSN;
    
    DefinableArchivalUnit au = makeAu();
    assertEquals(ListUtil.list(expected), au.getStartUrls());
  }
  
  
  public void testGetName() throws Exception {
    DefinableArchivalUnit au = makeAu();
    
    assertEquals(PluginName + ", Base URL " + BASE_URL + ", ISSN " + JOURNAL_ISSN +
        ", Volume " + VOLUME, au.getName());
  }
  
}

