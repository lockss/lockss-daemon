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

package org.lockss.plugin.pub2web.ms;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.lockss.repository.LockssRepository;
import org.lockss.test.*;
import org.lockss.util.CIProperties;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.HttpResultMap;
import org.lockss.plugin.*;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.base.DefaultUrlCacher;
import org.lockss.plugin.definable.*;

public class TestMsResponseHandler extends LockssTestCase {

  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String ROOT_URL = "http://www.jrnl.com/"; //this is not a real url

  private static final Logger log = Logger.getLogger(TestMsResponseHandler.class);
  static final String PLUGIN_ID = "org.lockss.plugin.pub2web.ms.ClockssMicrobiologySocietyJournalsPlugin";
  static final String PluginName = "Microbiology Society Journals Plugin (CLOCKSS)";

  private static final String TEXT = "text that is longer than reported";
  private static final int LEN_TOOSHORT = TEXT.length() - 4;


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

  public void testHandles500Result() throws Exception {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(VOL_KEY, "96");
    props.setProperty(JID_KEY, "jgv");
    ArchivalUnit au = makeAuFromProps(props);
    Plugin plugin = au.getPlugin();
    String suppl_url = ROOT_URL + "deliver/fulltext/mgen/9/99/ecs/article/119999/resources/00099.pdf?itemId=/content/suppdata/mgen/10.1099/mgen.0.000099-1&mimeType=pdf";
    MockLockssUrlConnection conn = new MockLockssUrlConnection();
    
    conn.setURL("http://uuu17/");
    CacheException exc =
        ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn,
            500, "foo");
    assertClass(CacheException.RetrySameUrlException.class, exc);
    
    conn.setURL(suppl_url);
    exc = ((HttpResultMap)plugin.getCacheResultMap()).mapException(au, conn, 500, "foo");
    assertTrue(exc instanceof CacheException.NoRetryDeadLinkException);
    
  }
  
  public void testShouldCacheWrongSize() throws Exception {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, ROOT_URL);
    props.setProperty(VOL_KEY, "96");
    props.setProperty(JID_KEY, "jgv");
    ArchivalUnit MSau = makeAuFromProps(props);
    String url = ROOT_URL + "content/journal/jgv/10.1099/vir.0.080989-0";
    CIProperties cprops = new CIProperties();
    
    cprops.setProperty("Content-Length", Integer.toString(LEN_TOOSHORT));
    UrlData ud = new UrlData(new StringInputStream(TEXT), cprops, url);
    MyDefaultUrlCacher cacher = new MyDefaultUrlCacher(MSau, ud);
    try {
      cacher.storeContent();
      // storeContent() should have thrown WrongLength, but is mapped to no_store/no_fail exception and returns
    } catch (CacheException e) {
      // fail("storeContent() shouldn't have thrown", e);
      assertClass(CacheException.RetryableException.class, e);
    } catch (Exception x) {
      fail("storeContent() shouldn't have thrown", x);
    }
    
    assertTrue(cacher.wasStored);
  }
  
  
  // DefaultUrlCacher that remembers that it stored
  private class MyDefaultUrlCacher extends DefaultUrlCacher {
    boolean wasStored = true;
    
    public MyDefaultUrlCacher(ArchivalUnit owner, UrlData ud) {
      super(owner, ud);
    }
    
    @Override
    protected void storeContentIn(String url, InputStream input,
                                  CIProperties headers,
                                  boolean doValidate, List<String> redirUrls)
            throws IOException {
      super.storeContentIn(url, input, headers, doValidate, redirUrls);
      if (infoException != null &&
          infoException.isAttributeSet(CacheException.ATTRIBUTE_NO_STORE)) {
        wasStored = false;
      }
    }
  }
  
}
