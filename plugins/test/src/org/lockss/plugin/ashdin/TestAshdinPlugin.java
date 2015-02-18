/*
 * $Id$
 */

/*

 Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.ashdin;

import java.net.*;
import java.util.List;
import java.util.Properties;
import org.lockss.test.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.FileMetadataExtractor;
import org.lockss.extractor.MetadataTarget;
import org.lockss.util.ListUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArchivalUnit.*;
import org.lockss.plugin.definable.*;

public class TestAshdinPlugin extends LockssTestCase {
  private DefinablePlugin plugin;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  
  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.ashdin.AshdinPlugin");
  }

  public void testCreateAu() {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "www.ashdin.com/");
    props.setProperty(YEAR_KEY, "2012");

    DefinableArchivalUnit au = null;
    try {
      au = makeAuFromProps(props);
    } catch (ConfigurationException ex) {
    }
  }

  public void testGetAuNullConfig() throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) {
    }
  }

  private DefinableArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (DefinableArchivalUnit) plugin.configureAu(config, null);
  }

  public void testGetAuConstructsProperAu()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(BASE_URL_KEY, "http://www.ashdin.com/");
    props.setProperty(YEAR_KEY, "2012");
    props.setProperty("journal_code", "acpsf");

    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals(
        "Ashdin Plugin (LOCKSS), Base URL http://www.ashdin.com/, Journal Code acpsf, Year 2012",
        au.getName());
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.ashdin.AshdinPlugin", plugin.getPluginId());
  }

  public void testGetAuConfigProperties() {

    assertEquals(plugin.findAuConfigDescr(BASE_URL_KEY),
        ConfigParamDescr.BASE_URL);
    assertEquals(plugin.findAuConfigDescr(YEAR_KEY),
        ConfigParamDescr.YEAR);
 
  }
}
