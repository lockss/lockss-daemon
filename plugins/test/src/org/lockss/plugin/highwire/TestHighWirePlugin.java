/*
 * $Id: TestHighWirePlugin.java,v 1.9 2009-05-27 16:39:04 dshr Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire;

import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.extractor.*;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.wrapper.*;
import org.lockss.util.urlconn.*;
import org.lockss.util.urlconn.CacheException.RetryDeadLinkException;

public class TestHighWirePlugin extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
//   static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

  private DefinablePlugin plugin;

  public TestHighWirePlugin(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.highwire.HighWirePlugin");

  }

  public void testGetAuNullConfig()
      throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }
  public void testCreateAu() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(VOL_KEY, "32");
//     props.setProperty(YEAR_KEY, "2004");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }

  }

  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }

  public void testGetAuHandlesBadUrl()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "322");
    props.setProperty(BASE_URL_KEY, "blah");
//     props.setProperty(YEAR_KEY, "2004");

    try {
      DefinableArchivalUnit au = makeAuFromProps(props);
      fail ("Didn't throw InstantiationException when given a bad url");
    } catch (ArchivalUnit.ConfigurationException auie) {
      ConfigParamDescr.InvalidFormatException murle =
        (ConfigParamDescr.InvalidFormatException)auie.getCause();
      assertNotNull(auie.getCause());
    }
  }

  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(VOL_KEY, "322");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
//     props.setProperty(YEAR_KEY, "2004");

    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("HighWire Press Plugin (Legacy), Base URL http://www.example.com/, Volume 322", au.getName());
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.highwire.HighWirePlugin",
		 plugin.getPluginId());
  }

  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.VOLUME_NUMBER,
			       ConfigParamDescr.BASE_URL),
//                                ConfigParamDescr.YEAR),
		 plugin.getLocalAuConfigDescrs());
  }

  public void testHandles404Result() throws Exception {
    String name = RetryDeadLinkException.class.getName();
    Class expected = Class.forName(name);
    Class found =( (HttpResultMap) plugin.getCacheResultMap()).getExceptionClass(404);
    assertEquals(expected, found);

  }

  public void testGetMetadataExtractor() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(VOL_KEY, "32");
//     props.setProperty(YEAR_KEY, "2004");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    assertNull(plugin.getMetadataExtractor("BogusExtractor", au));
    assertNotNull(plugin.getMetadataExtractor("text/html", au));
    assertTrue(plugin.getMetadataExtractor("text/html", au) instanceof
	       org.lockss.extractor.SimpleMetaTagMetadataExtractor);
  }
  public void testGetFilterFactory() {
    assertNull(plugin.getFilterFactory("BogusFilterFactory"));
    assertNotNull(plugin.getFilterFactory("application/pdf"));
    assertTrue(WrapperUtil.unwrap(plugin.getFilterFactory("application/pdf"))
	       instanceof org.lockss.plugin.highwire.HighWirePdfFilterFactory);
  }
  public void testGetArticleIteratorFactory() {
    assertNull(plugin.getArticleIteratorFactory("BogusArticleIterator"));
    assertNotNull(plugin.getArticleIteratorFactory("text/html"));
    assertTrue(WrapperUtil.unwrap(plugin.getArticleIteratorFactory("text/html"))
	       instanceof org.lockss.plugin.highwire.HighWireArticleIteratorFactory);
  }
}
