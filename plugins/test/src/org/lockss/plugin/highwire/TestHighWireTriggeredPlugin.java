/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.wrapper.*;
import org.lockss.util.urlconn.*;
import org.lockss.util.urlconn.CacheException.RetryDeadLinkException;

public class TestHighWireTriggeredPlugin extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();
  static final String VOL_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();

  private DefinablePlugin plugin;

  public TestHighWireTriggeredPlugin(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.highwire.HighWireTriggeredPlugin");

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
    props.setProperty(BASE_URL_KEY, "http://pediatrics.aappublications.org/");
    props.setProperty(VOL_KEY, "52");
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
    props.setProperty(VOL_KEY, "5");
    props.setProperty(BASE_URL_KEY, "ediatrics.aappublications.org/");
    props.setProperty(VOL_NAME_KEY,"sjd");
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
    props.setProperty(VOL_KEY, "52");
    props.setProperty(BASE_URL_KEY, "http://pediatrics.aappublications.org/");
    props.setProperty(VOL_NAME_KEY ,"52");
//     props.setProperty(YEAR_KEY, "2004");

    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("HighWire Press Plugin (H10c for CLOCKSS Triggered Content), "
        + "Base URL http://pediatrics.aappublications.org/, Volume 52", au.getName());
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.highwire.HighWireTriggeredPlugin",
		 plugin.getPluginId());
  }

  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
                               ConfigParamDescr.VOLUME_NAME),
//                                ConfigParamDescr.YEAR),
		 plugin.getLocalAuConfigDescrs());
  }

  public void testHandles404Result() throws Exception {
    assertClass(RetryDeadLinkException.class, 
        ( (HttpResultMap) plugin.getCacheResultMap()).mapException(null, "", 404, null));

  }
  public void testGetArticleMetadataExtractor() { 
  Properties props = new Properties();
  props.setProperty(BASE_URL_KEY, "http://pediatrics.aappublications.org/");
  props.setProperty(VOL_KEY, "52");
  props.setProperty(VOL_NAME_KEY, "52");
 //  props.setProperty(YEAR_KEY, "2004");
  DefinableArchivalUnit au = null;
  try {
    au = makeAuFromProps(props);
  }
  catch (ConfigurationException ex) {
  }
 
  assertTrue(""+plugin.getArticleMetadataExtractor(MetadataTarget.Any(), au),
           plugin.getArticleMetadataExtractor(MetadataTarget.Any(), au) instanceof ArticleMetadataExtractor );
  assertTrue(""+plugin.getFileMetadataExtractor(MetadataTarget.Any(), "text/html", au),
           plugin.getFileMetadataExtractor(MetadataTarget.Any(), "text/html", au) instanceof
         FileMetadataExtractor);
} 
  public void testGetHashFilterFactory() {
    assertNull(plugin.getHashFilterFactory("BogusFilterFactory"));
    assertNotNull(plugin.getHashFilterFactory("application/pdf"));
    assertTrue(WrapperUtil.unwrap(plugin.getHashFilterFactory("application/pdf"))
	       instanceof org.lockss.plugin.highwire.HighWirePdfFilterFactory);
  }
  public void testGetArticleIteratorFactory() { 
    assertTrue(WrapperUtil.unwrap(plugin.getArticleIteratorFactory())
      instanceof org.lockss.plugin.highwire.HighWirePressArticleIteratorFactory);
  }
}
