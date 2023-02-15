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