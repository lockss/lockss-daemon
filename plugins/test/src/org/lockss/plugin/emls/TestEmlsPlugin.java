/*
 * $Id: TestEmlsPlugin.java,v 1.4 2008-07-22 06:43:13 thib_gc Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.emls;

import java.net.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.ListUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.definable.*;

public class TestEmlsPlugin extends LockssTestCase {
  private DefinablePlugin plugin;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();

  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.emls.EmlsPlugin");
  }

  public void testGetAuNullConfig() throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
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
    props.setProperty(VOL_KEY, "3");

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
    props.setProperty(BASE_URL_KEY,
                      "http://extra.shu.ac.uk/emls/");
    props.setProperty(VOL_KEY, "3");

    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("Early Modern Literary Studies Plugin, Base URL http://extra.shu.ac.uk/emls/, Volume 3", au.getName());
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.emls.EmlsPlugin",
                 plugin.getPluginId());
  }

  public void testGetAuConfigProperties() {
    for (Iterator iter = plugin.getLocalAuConfigDescrs().iterator() ; iter.hasNext() ; ) {
      ConfigParamDescr desc = (ConfigParamDescr)iter.next();
      if (desc.equals(ConfigParamDescr.BASE_URL)) { continue; }
      if (desc.equals(ConfigParamDescr.VOLUME_NUMBER)) { continue; }
      if ("issues".equals(desc.getKey())) {
        assertEquals(ConfigParamDescr.TYPE_SET, desc.getType());
        assertFalse(desc.isDefinitional());
        continue;
      }
      fail("Unexpected config param: " + desc.getKey());
    }
  }
}
