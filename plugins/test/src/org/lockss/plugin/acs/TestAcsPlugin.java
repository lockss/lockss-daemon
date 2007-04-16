/*
 * $Id: TestAcsPlugin.java,v 1.4 2007-04-16 17:15:13 troberts Exp $
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

package org.lockss.plugin.acs;

import java.net.*;
import java.util.Properties;
import org.lockss.test.*;
import org.lockss.util.ListUtil;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;

public class TestAcsPlugin extends LockssTestCase {
  private DefinablePlugin plugin;
  static final ConfigParamDescr JOURNAL_KEY = new ConfigParamDescr();
  static {
    JOURNAL_KEY.setKey("journal_key");
    JOURNAL_KEY.setDisplayName("Journal ID");
    JOURNAL_KEY.setType(ConfigParamDescr.TYPE_STRING);
    JOURNAL_KEY.setSize(20);
    JOURNAL_KEY.setDescription("Key used to identify journal in script (e.g. 'jcisd8').");
  }

  static final ConfigParamDescr ARTICLE_URL = new ConfigParamDescr();
  static {
    ARTICLE_URL.setKey("article_url");
    ARTICLE_URL.setDisplayName("Article URL");
    ARTICLE_URL.setType(ConfigParamDescr.TYPE_URL);
    ARTICLE_URL.setSize(40);
    ARTICLE_URL.setDescription("base url for articles");
  }
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String YEAR_KEY = ConfigParamDescr.YEAR.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NUMBER.getKey();
  static final String JRNL_KEY = JOURNAL_KEY.getKey();
  static final String ARTICAL_KEY = ARTICLE_URL.getKey();

  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.acs.AcsPlugin");
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
    props.setProperty(ARTICAL_KEY, "http://www.example.com/");
    props.setProperty(JRNL_KEY,"abcd");
    props.setProperty(BASE_URL_KEY, "blah");
    props.setProperty(YEAR_KEY, "2003");
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
    props.setProperty(VOL_KEY, "322");
    props.setProperty(ARTICAL_KEY, "http://www.example.com/");
    props.setProperty(JRNL_KEY,"abcd");
    props.setProperty(BASE_URL_KEY, "http://www.example.com/");
    props.setProperty(YEAR_KEY, "2003");

    DefinableArchivalUnit au = makeAuFromProps(props);
    assertEquals("www.example.com, abcd, vol. 322", au.getName());
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.acs.AcsPlugin",
		 plugin.getPluginId());
  }

  public void testGetAuConfigProperties() {
    assertIsomorphic(ListUtil.list(ConfigParamDescr.VOLUME_NUMBER,
				   ConfigParamDescr.BASE_URL,
				   ARTICLE_URL,
				   JOURNAL_KEY),
		     plugin.getLocalAuConfigDescrs());
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestAcsPlugin.class.getName()};
    junit.swingui.TestRunner.main(testCaseList);
  }
}
