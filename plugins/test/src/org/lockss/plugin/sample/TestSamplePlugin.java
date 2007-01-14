/*
 * $Id: TestSamplePlugin.java,v 1.3 2007-01-14 08:06:05 tlipkis Exp $
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

package org.lockss.plugin.sample;

import java.net.*;
import java.util.Properties;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;

public class TestSamplePlugin extends LockssTestCase {
  private SamplePlugin plugin;

  public void setUp() throws Exception {
    super.setUp();
    plugin = new SamplePlugin();
    plugin.initPlugin(getMockLockssDaemon());
  }

  public void testGetAUNullConfig() throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  private SampleArchivalUnit makeAuFromProps(Properties props)
      throws ArchivalUnit.ConfigurationException {
    Configuration config = ConfigurationUtil.fromProps(props);
    return (SampleArchivalUnit)plugin.configureAu(config, null);
  }

  public void testGetAuHandlesBadUrl()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(SamplePlugin.AUPARAM_VOL, "2");
    props.setProperty(SamplePlugin.AUPARAM_BASE_URL, "foobar");
    try {
      SampleArchivalUnit au = makeAuFromProps(props);
      fail ("Didn't throw InstantiationException when given a bad url");
    } catch (ArchivalUnit.ConfigurationException auie) {
      ConfigParamDescr.InvalidFormatException murle =
        (ConfigParamDescr.InvalidFormatException)auie.getCause();
      assertNotNull(auie.getCause());
    }
  }

  public void testGetAuConstructsProperAU()
      throws ArchivalUnit.ConfigurationException, MalformedURLException {
    Properties props = new Properties();
    props.setProperty(SamplePlugin.AUPARAM_VOL, "2");
    props.setProperty(SamplePlugin.AUPARAM_BASE_URL, "http://www.example.com/");

    SampleArchivalUnit au = makeAuFromProps(props);
    assertEquals("www.example.com, vol. 2", au.getName());
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.sample.SamplePlugin",
		 plugin.getPluginId());
  }

  public void testGetAUConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
                               ConfigParamDescr.VOLUME_NUMBER),
		 plugin.getLocalAuConfigDescrs());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestSamplePlugin.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
