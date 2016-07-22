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

package org.lockss.plugin.igiglobal;

import junit.framework.Test;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.plugin.*;

// super class for this plugin - variants defined within it
public class TestIgiGlobalContentValidator extends LockssTestCase {
  
  static Logger log = Logger.getLogger(TestIgiGlobalContentValidator.class);
  
  private MockLockssDaemon theDaemon;
  
  private static IgiGlobalContentValidator.HtmlContentValidator contentValidator;
  
  private static final String BASE_URL = "http://www.igiglobal.com/";
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
  String urlStr4;
  
  // variant of thurlStr4tadata extraction test for the JOURNAL version of the plugin
  public static class TestJournalPlugin extends TestIgiGlobalContentValidator {
    
    public void setUp() throws Exception {
      
      pluginName = "org.lockss.plugin.igiglobal.IgiGlobalPlugin";  
      AuConfig = ConfigurationUtil.fromArgs(
          "base_url", BASE_URL,
          "volume", "2",
          "journal_issn","1546-2234" );
      super.setUp();
      
      urlStr1 = BASE_URL + "gateway/article/81299";
      urlStr2 = BASE_URL + "gateway/article/full-text-html/81299";
      urlStr3 = BASE_URL + "gateway/article/full-text-pdf/81299";
      urlStr4 = BASE_URL + "pdf.aspx?tid=100015&ptid=71664&ctid=17&t=Call+For+Articles";
    }
    
  }
  
  // variant of the plugin metadata extraction test for the BOOK version of the plugin
  public static class TestBooksPlugin extends TestIgiGlobalContentValidator {
    
    
    public void setUp() throws Exception {
      // set up stuff explicit to this variant and call parent
      pluginName="org.lockss.plugin.igiglobal.ClockssIgiGlobalBooksPlugin";  
      AuConfig = ConfigurationUtil.fromArgs(
          "base_url", BASE_URL,
          "volume", "464",
          "book_isbn", "9781591407928" );
      super.setUp();
      
      urlStr1 = BASE_URL + "gateway/chapter/81299";
      urlStr2 = BASE_URL + "gateway/chapter/full-text-html/81299";
      urlStr3 = BASE_URL + "gateway/chapter/full-text-pdf/81299";
      urlStr4 = BASE_URL + "pdf.aspx?tid=107536&ptid=46985&ctid=15&t=Title+Page";
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
    
    //igiConf is only set up in variants - check before using?
    assertNotEquals(AuConfig, null);
    
    bau = PluginTestUtil.createAndStartAu(pluginName, AuConfig);
    
    ContentValidatorFactory cvfact = new IgiGlobalContentValidator.Factory();
    contentValidator = (IgiGlobalContentValidator.HtmlContentValidator) cvfact.createContentValidator(bau, "text/html");
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
    contentValidator.validate(cu);
    cu = new MockCachedUrl(urlStr4, bau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    try {
      contentValidator.validate(cu);
      fail("Bad cu should throw exception");
    } catch (Exception e) {
      // okay, fall-thru
    }
    
  }
  
  
  public static Test suite() {
    return variantSuites(new Class[] {
        TestJournalPlugin.class,
        TestBooksPlugin.class,
    });
  }
}
