/*
 * $Id: TestIeeePlugin.java,v 1.3 2007-01-14 08:06:09 tlipkis Exp $
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

package org.lockss.plugin.ieee;

import java.net.*;
import java.util.Properties;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;

public class TestIeeePlugin extends LockssTestCase {
  private DefinablePlugin plugin;
  static final ConfigParamDescr PU_NUMBER = new ConfigParamDescr();
  static {
    PU_NUMBER.setKey("Pu_Number");
    PU_NUMBER.setDisplayName("Publication Number");
    PU_NUMBER.setType(ConfigParamDescr.TYPE_POS_INT);
    PU_NUMBER.setSize(10);
    PU_NUMBER.setDescription("IEEE publication Number(e.g. '2').");
  }
  static final String PUNUM_KEY = PU_NUMBER.getKey();
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();


  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.ieee.IeeePlugin");

  }

  public void testGetAUNullConfig() throws ArchivalUnit.ConfigurationException {
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
    props.setProperty(YEAR_KEY, "2003");
    props.setProperty(BASE_URL_KEY, "foobar");
    props.setProperty(PUNUM_KEY, "4");
    try {
      DefinableArchivalUnit au = makeAuFromProps(props);
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
    props.setProperty(YEAR_KEY, "2003");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(PUNUM_KEY,"4");

    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("www.example.com, puNumber 4, 2003", au.getName());
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.ieee.IeeePlugin",
		 plugin.getPluginId());
  }

  public void testGetAUConfigProperties() {
    assertEquals(ListUtil.list(ConfigParamDescr.BASE_URL,
                               PU_NUMBER,
			       ConfigParamDescr.YEAR),
		 plugin.getLocalAuConfigDescrs());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestIeeePlugin.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
