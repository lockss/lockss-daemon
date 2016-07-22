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
  
  static Logger log = Logger.getLogger("TestIgiGlobalMetadataExtractor");
  
  private MockLockssDaemon theDaemon;
  private static ArchivalUnit bau;
  
  private static IgiGlobalContentValidator.HtmlContentValidator contentValidator;
  
  // variables that will set up for each variant of the test (eg. LOCKSS v. CLOCKSS or JOURNAL v. BOOK
  private static Configuration AU_CONFIG;
  private static String PLUGIN_NAME; 
  private static String BASE_URL = "http://www.igiglobal.com/";
  private static String URL1_STRING;
  private static String URL2_STRING;
  private static String URL3_STRING;
  private static String URL4_STRING;
  
  
  // variant of the plugin metadata extraction test for the JOURNAL version of the plugin
  public static class TestJournalPlugin extends TestIgiGlobalContentValidator {
    
    public void setUp() throws Exception {
      
      PLUGIN_NAME="org.lockss.plugin.igiglobal.IgiGlobalPlugin";  
      AU_CONFIG = ConfigurationUtil.fromArgs(
          "base_url", BASE_URL,
          "volume", "2",
          "journal_issn","1546-2234" );
      super.setUp();
      
      URL1_STRING = BASE_URL + "gateway/article/81299";
      URL2_STRING = BASE_URL + "gateway/article/full-text-html/81299";
      URL3_STRING = BASE_URL + "gateway/article/full-text-pdf/81299";
      URL4_STRING = BASE_URL + "pdf.aspx?tid=100015&ptid=71664&ctid=17&t=Call+For+Articles";
    }
    
  }
  
  // variant of the plugin metadata extraction test for the BOOK version of the plugin
  public static class TestBooksPlugin extends TestIgiGlobalContentValidator {
    
    public void setUp() throws Exception {
      // set up stuff explicit to this variant and call parent
      PLUGIN_NAME="org.lockss.plugin.igiglobal.ClockssIgiGlobalBooksPlugin";  
      AU_CONFIG = ConfigurationUtil.fromArgs(
          "base_url", BASE_URL,
          "volume", "464",
          "book_isbn", "9781591407928" );
      super.setUp();
      
      URL1_STRING = BASE_URL + "gateway/chapter/81299";
      URL2_STRING = BASE_URL + "gateway/chapter/full-text-html/81299";
      URL3_STRING = BASE_URL + "gateway/chapter/full-text-pdf/81299";
      URL4_STRING = BASE_URL + "pdf.aspx?tid=107536&ptid=46985&ctid=15&t=Title+Page";
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
    assertNotEquals(AU_CONFIG, null);
    
    bau = PluginTestUtil.createAndStartAu(PLUGIN_NAME, AU_CONFIG);
    
    ContentValidatorFactory cvfact = new IgiGlobalContentValidator.Factory();
    if (cvfact == null) 
      fail("cvfact == null");
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
    setUp();
    MockCachedUrl cu = new MockCachedUrl(URL1_STRING, bau);
    cu.setContent("text");
    cu.setContentSize("text".length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html; charset=utf-8");
    contentValidator.validate(cu);
    cu = new MockCachedUrl(URL2_STRING, bau);
    cu.setContent("text");
    cu.setContentSize("text".length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html; charset=utf-8");
    contentValidator.validate(cu);
    cu = new MockCachedUrl(URL3_STRING, bau);
    cu.setContent("text");
    cu.setContentSize("text".length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html; charset=utf-8");
    contentValidator.validate(cu);
    cu = new MockCachedUrl(URL4_STRING, bau);
    cu.setContent("text");
    cu.setContentSize("text".length());
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html; charset=utf-8");
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
