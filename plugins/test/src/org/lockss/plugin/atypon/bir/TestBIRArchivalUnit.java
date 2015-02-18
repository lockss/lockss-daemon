/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.atypon.bir;

import java.util.Properties;

import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.definable.*;
import org.lockss.test.*;
import org.lockss.util.*;

//
// This plugin test framework is set up to run the same tests in two variants - CLOCKSS and GLN
// without having to actually duplicate any of the written tests
//
public class TestBIRArchivalUnit extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  static final String BASE_URL_KEY = ConfigParamDescr.BASE_URL.getKey();
  static final String JID_KEY = ConfigParamDescr.JOURNAL_ID.getKey();
  static final String VOL_KEY = ConfigParamDescr.VOLUME_NAME.getKey();

  static Logger log = Logger.getLogger(TestBIRArchivalUnit.class);
  
  public void setUp() throws Exception {
    super.setUp();
    setUpDiskSpace();
    theDaemon = getMockLockssDaemon();
    theDaemon.getHashService();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  private static final String gln_user_msg = 
      "Atypon Systems hosts this archival unit (AU) " +
          "and requires that you " +
          "<a href=\'http://www.birpublications.org/action/institutionLockssIpChange\'>" +
          "register the IP address " +
          "of this LOCKSS box in your institutional account as a crawler</a> before allowing " +
          "your LOCKSS box to harvest this AU. Failure to comply with this publisher requirement " +
          "may trigger crawler traps on the Atypon Systems platform, and your LOCKSS box or your " +
          "entire institution may be temporarily banned from accessing the site. You only need " +
          "to register the IP address of your LOCKSS box once for all AUs published by " +
          "the British Institute of Radiology.";

  public void testSpecificUserMsg() throws Exception {
    // set up a BiR child
    Properties props = new Properties();
    props.setProperty(VOL_KEY, Integer.toString(17));
    props.setProperty(JID_KEY, "eint");
    props.setProperty(BASE_URL_KEY, "http://www.birpublications.org/");
    Configuration config = ConfigurationUtil.fromProps(props);
    
    DefinablePlugin ap = new DefinablePlugin();
    ap.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.atypon.bir.BIRAtyponPlugin");
    DefinableArchivalUnit BirAU = (DefinableArchivalUnit)ap.createAu(config);
    
    log.debug3("testing GLN user message");
    assertEquals(gln_user_msg, BirAU.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null));

    // now check clockss version non-message
    DefinablePlugin cap = new DefinablePlugin();
    cap.initPlugin(getMockLockssDaemon(),
        "org.lockss.plugin.atypon.bir.ClockssBIRAtyponPlugin");
    DefinableArchivalUnit ClBirAU = (DefinableArchivalUnit)cap.createAu(config);
    
    log.debug3("testing CLOCKSS absence of user message");
    assertEquals(null, ClBirAU.getProperties().getString(DefinableArchivalUnit.KEY_AU_CONFIG_USER_MSG, null));    
    
  }

}

