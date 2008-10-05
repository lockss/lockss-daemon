/*
 * $Id: TestBlackwellPlugin.java,v 1.3 2007-02-15 19:48:27 troberts Exp $
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

package org.lockss.plugin.blackwell;

import java.net.*;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;

public class TestBlackwellPlugin extends LockssTestCase {
  static Logger log = Logger.getLogger("TestBlackwellPlugin");

  private DefinablePlugin plugin;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JOURNAL_KEY = "journal_id";
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();

  public void setUp() throws Exception {
    super.setUp();
    plugin = new DefinablePlugin();
    plugin.initPlugin(getMockLockssDaemon(),
                      "org.lockss.plugin.blackwell.BlackwellPlugin");
  }

  public void testGetAuNullConfig() 
      throws ArchivalUnit.ConfigurationException {
    try {
      plugin.configureAu(null, null);
      fail("Didn't throw ArchivalUnit.ConfigurationException");
    } catch (ArchivalUnit.ConfigurationException e) { }
  }

  public void testGetPluginId() {
    assertEquals("org.lockss.plugin.blackwell.BlackwellPlugin",
		 plugin.getPluginId());
  }

  public void testGetAuConfigProperties() {
    List descrs = plugin.getLocalAuConfigDescrs();
    log.debug("descrs: " + descrs);
    assertEquals(SetUtil.set(ConfigParamDescr.BASE_URL,
			     ConfigParamDescr.VOLUME_NAME,
			     ConfigParamDescr.JOURNAL_ID,
			     ConfigParamDescr.JOURNAL_ISSN),
		 SetUtil.theSet(descrs));
  }

}
