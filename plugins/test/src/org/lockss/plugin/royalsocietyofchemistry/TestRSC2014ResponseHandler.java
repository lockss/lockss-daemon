/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.royalsocietyofchemistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.lockss.repository.LockssRepository;
import org.lockss.test.*;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.plugin.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.base.DefaultUrlCacher;
import org.lockss.plugin.definable.*;

public class TestRSC2014ResponseHandler extends LockssTestCase {

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String RESOLVER_URL_KEY = "resolver_url";
  static final String GRAPHICS_URL_KEY = "graphics_url";
  static final String BASE_URL2_KEY = ConfigParamDescr.BASE_URL2.getKey();
  static final String JOURNAL_CODE_KEY = "journal_code";
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();

  private static final Logger log = Logger.getLogger(TestRSC2014ResponseHandler.class);

  static final String PLUGIN_ID = "org.lockss.plugin.royalsocietyofchemistry.ClockssRSC2014Plugin";
  static final String BOOKS_ID = "org.lockss.plugin.royalsocietyofchemistry.ClockssRSCBooksPlugin";

  private static final String TEXT = "text that is longer than reported";
  private static final int LEN_TOOSHORT = TEXT.length() - 4;
  private static final int LEN = TEXT.length();
  private static final String TEXT_CONTENT_TYPE = "text/html; charset=utf-8";



  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    MockLockssDaemon daemon = getMockLockssDaemon();
    PluginManager pluginMgr = daemon.getPluginManager();
    pluginMgr.setLoadablePluginsReady(true);
    daemon.setDaemonInited(true);
    pluginMgr.startService();
    daemon.getAlertManager();
    daemon.getCrawlManager();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }


  private ArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return PluginTestUtil.createAndStartAu(PLUGIN_ID,  config);
  }

  private ArchivalUnit makeBooksAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return PluginTestUtil.createAndStartAu(BOOKS_ID,  config);
  }

  public void testShouldCacheWrongSize2() throws Exception {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://pubs.example.com/");
    props.setProperty(RESOLVER_URL_KEY, "http://xlink.example.com/");
    props.setProperty(GRAPHICS_URL_KEY, "http://img.example.com/");
    props.setProperty(BASE_URL2_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_CODE_KEY, "an");
    props.setProperty(VOLUME_NAME_KEY, "123");
    props.setProperty(YEAR_KEY, "2013");
    ArchivalUnit RSCau = makeAuFromProps(props);
    String url = "http://pubs.example.com/en/content/articlelanding/2013/an/b916736f";
    CIProperties cprops = new CIProperties();

    cprops.setProperty("Content-Length", Integer.toString(LEN_TOOSHORT));
    UrlData ud = new UrlData(new StringInputStream(TEXT), cprops, url);
    MyDefaultUrlCacher cacher = new MyDefaultUrlCacher(RSCau, ud);
    try {
      cacher.storeContent();
      //fail("storeContent() should have thrown WrongLength");
    } catch (CacheException e) {
      fail("storeContent() shouldn't have thrown", e);
      //        assertClass(CacheException.UnretryableException.class, e);
    }
    assertTrue(cacher.wasStored);

  }
  
  public void testBooksShouldCacheWrongSize2() throws Exception {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://pubs.example.com/");
    props.setProperty(GRAPHICS_URL_KEY, "http://img.example.com/");
    props.setProperty(YEAR_KEY, "2013");
    ArchivalUnit booksau = makeBooksAuFromProps(props);
    String url = "http://pubs.example.com/en/content/chapter/am9780851860343-00441/978-0-85186-034-3";
    CIProperties cprops = new CIProperties();

    cprops.setProperty("Content-Length", Integer.toString(LEN_TOOSHORT));
    UrlData ud = new UrlData(new StringInputStream(TEXT), cprops, url);
    MyDefaultUrlCacher cacher = new MyDefaultUrlCacher(booksau, ud);
    try {
      cacher.storeContent();
      // WrongLength is mapped to Success;
    } catch (CacheException e) {
      fail("storeContent() shouldn't have thrown", e);
    }
    assertTrue(cacher.wasStored);

  }
  
  public void testForTextContent() throws Exception {
    MockCachedUrl cu;
    
    String urlStr1= " http://pubs.example.com/en/content/articlelanding/2013/an/b916736f";
    String urlStr2= " http://pubs.example.com/en/content/articlepdf/2013/an/b916736f";
    
    
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://pubs.example.com/");
    props.setProperty(RESOLVER_URL_KEY, "http://xlink.example.com/");
    props.setProperty(GRAPHICS_URL_KEY, "http://img.example.com/");
    props.setProperty(BASE_URL2_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_CODE_KEY, "an");
    props.setProperty(VOLUME_NAME_KEY, "123");
    props.setProperty(YEAR_KEY, "2013");
    ArchivalUnit RSCau = makeAuFromProps(props);
    
    ContentValidatorFactory cvfact = new RSCContentValidator.Factory();
    RSCContentValidator.TextTypeValidator contentValidator = 
    (RSCContentValidator.TextTypeValidator) cvfact.createContentValidator(RSCau, "text/html");
    if (contentValidator == null) 
      fail("contentValidator == null");
    
    cu = new MockCachedUrl(urlStr1, RSCau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    contentValidator.validate(cu);
    cu = new MockCachedUrl(urlStr2, RSCau);
    cu.setContent(TEXT);
    cu.setContentSize(LEN);
    cu.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, TEXT_CONTENT_TYPE);
    try {
      contentValidator.validate(cu);
      fail("Bad cu should throw exception");
    }catch (Exception e) {
      log.info(e.toString());
      // okay, fall-thru - we expected this to happen
      assertClass(ContentValidationException.class, e);
    }
     
  }
  

  // DefaultUrlCacher that remembers that it stored
  private class MyDefaultUrlCacher extends DefaultUrlCacher {
    boolean wasStored = false;

    public MyDefaultUrlCacher(ArchivalUnit owner, UrlData ud) {
      super(owner, ud);
    }


    @Override
    protected void storeContentIn(String url, InputStream input,
				  CIProperties headers,
				  boolean doValidate, List<String> redirUrls)
            throws IOException {
      super.storeContentIn(url, input, headers, doValidate, redirUrls);
      wasStored = true;
    }
  }

}
