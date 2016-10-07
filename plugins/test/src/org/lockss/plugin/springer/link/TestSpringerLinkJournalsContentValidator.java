/*
 * $Id$
 */

/*

Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer.link;

import junit.framework.Test;

import org.lockss.test.*;
import org.lockss.util.*;

import java.util.Properties;

import org.lockss.config.*;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.plugin.*;

// super class for this plugin - variants defined within it
public class TestSpringerLinkJournalsContentValidator extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestSpringerLinkJournalsContentValidator.class);
  
  private MockLockssDaemon theDaemon;
  
  private static SpringerLinkJournalsContentValidator.HtmlContentValidator contentValidator;
  
  private static final String BASE_URL = "https://link.test.com/"; //this is not a real url
  private static final String DOWNLOAD_URL = "http://download.test.com/"; //this is not a real url
  private static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  private static final String DOWNLOAD_URL_KEY = "download_url";
  private static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ISSN_KEY = "journal_eissn";
  static final String PLUGIN_ID = "org.lockss.plugin.springer.link.SpringerLinkJournalsPlugin";
  static final String PluginName = "SpringerLink Journals Plugin";
  private static final String TEXT = "text";
  private static final int LEN = TEXT.length();
  private static final String TEXT_CONTENT_TYPE = "text/html; charset=utf-8";
  
  // variables that will set up for each variant of the test (eg. LOCKSS v. CLOCKSS or JOURNAL v. BOOK
  private ArchivalUnit bau;
  String pluginName;
  Configuration AuConfig;
  
  String urlStr1;
  String urlStr2;
  String urlStr3;
  
  public static class TestJournalPlugin extends TestSpringerLinkJournalsContentValidator {

    public void setUp() throws Exception {
      
      pluginName = "org.lockss.plugin.springer.link.SpringerLinkJournalsPlugin";  
      Properties props = new Properties();
      props.setProperty(ISSN_KEY, "1234-5678");
      props.setProperty(VOL_KEY, Integer.toString(206));
      props.setProperty(BASE_URL_KEY, BASE_URL);
      props.setProperty(DOWNLOAD_URL_KEY, DOWNLOAD_URL);
    
      AuConfig = ConfigurationUtil.fromProps(props);
      super.setUp();
      
      urlStr1 = BASE_URL + "article/10.1234/s135-013-246-0 ";
      urlStr2 = BASE_URL + "article/10.1234/s246-013-135-0/fulltext.html";
      urlStr3 = BASE_URL + "content/pdf/10.1007%2Fs11356-013-2162-3.pdf";
    }
    
  }
  
  // The super (of the variants) methods
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    
    theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();
    theDaemon.getPluginManager().setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    theDaemon.getPluginManager().startService();
    theDaemon.getCrawlManager();
    
    assertNotEquals(AuConfig, null);
    
    bau = PluginTestUtil.createAndStartAu(pluginName, AuConfig);
    
    ContentValidatorFactory cvfact = new SpringerLinkJournalsContentValidator.Factory();
    contentValidator = (SpringerLinkJournalsContentValidator.HtmlContentValidator) cvfact.createContentValidator(bau, "text/html");
    if (contentValidator == null) 
      fail("contentValidator == null");
  }
  
  public void tearDown() throws Exception {
    theDaemon.stopDaemon();
    super.tearDown();
  }
  
  
  /**
   * Method that creates a simulated Cached URL with appropriate content-type 
   * @throws Exception
   */
  public void testForTextContent() throws Exception {
    MockCachedUrl cu;
    
    setUp();
    cu = new MockCachedUrl(urlStr1, bau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    contentValidator.validate(cu);
    cu = new MockCachedUrl(urlStr2, bau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    contentValidator.validate(cu);
    cu = new MockCachedUrl(urlStr3, bau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    try {
      contentValidator.validate(cu);
      fail("Bad cu should throw exception");

    }catch (Exception e) {
      // okay, fall-thru
    }
    
  }
  
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestJournalPlugin.class,
    });
  }
}