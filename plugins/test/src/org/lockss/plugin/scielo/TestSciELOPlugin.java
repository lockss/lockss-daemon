/*
 * $Id: TestSciELOPlugin.java 43339 2015-07-31 21:11:05Z etenbrink $
 */

/*

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

package org.lockss.plugin.scielo;

import java.net.MalformedURLException;
import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.ConfigurationException;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.ListUtil;

public class TestSciELOPlugin extends LockssTestCase {
  
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ISSN_KEY = ConfigParamDescr.JOURNAL_ISSN.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  private MockLockssDaemon theDaemon;
  private DefinablePlugin plugin;
  
  public TestSciELOPlugin(String msg) {
    super(msg);
  }
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.scielo.SciELOPlugin");
    
  }
  
  public void testGetAuNullConfig()
      throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  public void testCreateAu() throws ConfigurationException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ISSN_KEY, "X090-999X");
    props.setProperty(YEAR_KEY, "2013");
    makeAuFromProps(props);
  }
  
  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }
  
  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(YEAR_KEY, "2013");
    props.setProperty(JOURNAL_ISSN_KEY, "X090-999X");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    
    String starturls[] = {
        "http://www.example.com/scielo.php?script=sci_issues&pid=X090-999X&lng=en",
        "http://www.example.com/scielo.php?script=sci_issues&pid=X090-999X&lng=es",
        "http://www.example.com/scielo.php?script=sci_issues&pid=X090-999X&lng=pt"
        };
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("SciELO Journals Plugin, Base URL http://www.example.com/, ISSN X090-999X, Year 2013",
        au.getName());
    assertEquals(ListUtil.fromArray(starturls), au.getStartUrls());
  }
  
  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.scielo.SciELOPlugin",
        plugin.getPluginId());
  }
  
  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(
        ConfigParamDescr.BASE_URL,
        ConfigParamDescr.JOURNAL_ISSN,
        ConfigParamDescr.YEAR),
        plugin.getLocalAuConfigDescrs());
  }
  
  // Test the crawl rules for APS
  public void testShouldCacheProperPages() throws Exception {
    String ROOT_URL = "http://www.scielo.ex/";
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(JOURNAL_ISSN_KEY, "X090-999X");
    props.setProperty(YEAR_KEY, "2013");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    theDaemon.getLockssRepository(au);
    /*
     */
    // Test for pages that should get crawled or not
    // permission page/start url
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issues&pid=X090-999X&lng=es", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issues&pid=X090-999X&lng=en", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issues&pid=X090-999X&lng=pt", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issues&pid=X090-999X", false, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issues&pid=X090-999X&lng=xx", true, au);
    // issue
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issuetoc&pid=X090-999X20130001", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issuetoc&pid=X090-999X20130002&lng=pt", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issuetoc&pid=X090-999X20130003&lng=en", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issuetoc&pid=X090-999X20130004&lng=fr", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issuetoc&pid=X090-999X20130006a&lng=es", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_issuetoc&pid=X090-999X2013000a6&lng=es", false, au);
    // article files
    shouldCacheTest(ROOT_URL + "pdf/abcd/v27n1/X090-999X-abcd-27-01-00088.pdf", true, au);
    shouldCacheTest(ROOT_URL + "pdf/abcd/v27n1/pt_X090-999X-abcd-27-01-00088.pdf", true, au);
    shouldCacheTest(ROOT_URL + "pdf/abcd/v27n1/pt_X090-999X-abcd-27-01-00088.epdf", false, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_abstract&pid=SX090-999X2013000100003&lng=en", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_abstract&pid=SX090-999X2013000100003&lng=es", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_abstract&pid=SX090-999X2013000100003&lng=es&tlng=pt", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_abstract&pid=SX090-999X2013000100003", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_abstract&pid=SX090-999X2013000100003999", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_arttext&pid=SX090-999X2013000100086&lng=en&tlng=pt", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_arttext&pid=SX090-999X2013000100086&lng=en&tlng=xx", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_arttext&pid=SX090-999X2013000100086&lng=xx", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_arttext&pid=SX090-999X2013000100086&lng=xx&tlng=pt", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_pdf&pid=SX090-999X2013000100084&lng=en", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_pdf&pid=SX090-999X2013000100084&lng=en&tlng=pt", true, au);
    shouldCacheTest(ROOT_URL + "scieloOrg/php/articleXML.php?pid=SX090-999X2013000100001&lang=en", true, au);
    shouldCacheTest(ROOT_URL + "scieloOrg/php/articleXML.php?pid=SX090-999X2013000100001&lang=es", true, au);
    
    shouldCacheTest(ROOT_URL + "img/es/lead.gif", true, au);
    shouldCacheTest("http://cdn.mathjax.org/mathjax/latest/MathJax.js", true, au);
    shouldCacheTest("http://cdn.scielo.org/img/es/lead.gif", false, au);
    shouldCacheTest("https://ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js", true, au);
    shouldCacheTest(ROOT_URL + "scielo.php?script=sci_serial&pid=0102-6720&lng=pt", false, au);
    
    // should not get crawled - LOCKSS
    shouldCacheTest("http://lockss.stanford.edu", false, au);
    
  }
  
  private void shouldCacheTest(String url, boolean shouldCache, ArchivalUnit au) {
    log.info ("shouldCacheTest url: " + url);
    assertEquals(shouldCache, au.shouldBeCached(url));
  }
  
}