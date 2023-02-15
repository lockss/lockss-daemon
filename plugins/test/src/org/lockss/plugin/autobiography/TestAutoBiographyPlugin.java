/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.autobiography;

import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.plugin.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.definable.*;
import org.lockss.plugin.wrapper.*;

public class TestAutoBiographyPlugin extends LockssTestCase {
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_ID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOLUME_NAME_KEY = ConfigParamDescr.VOLUME_NAME.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();

  private DefinablePlugin plugin;

  public TestAutoBiographyPlugin(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.autobiography.ClockssAutoBiographyPlugin");
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
    props.setProperty(JOURNAL_ID_KEY, "aub");
    props.setProperty(VOLUME_NAME_KEY, "12");
    props.setProperty(YEAR_KEY, "2000");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    au.getName();
  }

  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit)plugin.configureAu(config, null);
  }

  public void testGetAuHandlesBadUrl()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "blah");
    props.setProperty(JOURNAL_ID_KEY, "bjnl");
    props.setProperty(VOLUME_NAME_KEY, "2");
    props.setProperty(YEAR_KEY, "2000");
    
    try {
      DefinableArchivalUnit au = makeAuFromProps(props);
      fail ("Didn't throw InstantiationException when given a bad url");
      assertNull(au);
    } catch (ArchivalUnit.ConfigurationException auie) {
      assertNotNull(auie.getCause());
    }
  }
  
  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "aub");
    props.setProperty(VOLUME_NAME_KEY, "3");
    
    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("Auto/Biography Triggered Content Plugin, Base URL http://www.example.com/, " +
        "Volume 3", au.getName());
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.autobiography.ClockssAutoBiographyPlugin",
        plugin.getPluginId());
  }

  public void testGetAuConfigProperties() {
    assertEquals(ListUtil.list(
        ConfigParamDescr.BASE_URL,
        ConfigParamDescr.JOURNAL_ID,
        ConfigParamDescr.VOLUME_NAME),
        plugin.getLocalAuConfigDescrs());
  }

  // XXX add back !

  public void testGetArticleMetadataExtractor() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(JOURNAL_ID_KEY, "aub");
    props.setProperty(VOLUME_NAME_KEY, "1");
    props.setProperty(YEAR_KEY, "2000");
    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    }
    catch (ConfigurationException ex) {
    }
    assertTrue(""+plugin.getArticleMetadataExtractor(MetadataTarget.Any(), au),
        plugin.getArticleMetadataExtractor(null, au) instanceof ArticleMetadataExtractor);
    assertFalse(""+plugin.getFileMetadataExtractor(MetadataTarget.Any(), "text/html", au),
        plugin.getFileMetadataExtractor(MetadataTarget.Any(), "text/html", au) instanceof
        FileMetadataExtractor
        );
    assertTrue(""+plugin.getFileMetadataExtractor(MetadataTarget.Any(), "application/xml", au),
        plugin.getFileMetadataExtractor(MetadataTarget.Any(), "application/xml", au) instanceof
        FileMetadataExtractor
        );
  }
  
  
  public void testGetArticleIteratorFactory() {
    assertTrue(WrapperUtil.unwrap(plugin.getArticleIteratorFactory())
        instanceof org.lockss.plugin.autobiography.AutoBiographyArticleIteratorFactory);
  }
}
