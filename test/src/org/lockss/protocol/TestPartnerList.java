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

package org.lockss.protocol;

import java.io.*;
import java.net.*;
import java.util.*;
import junit.framework.TestCase;

import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.repository.LockssRepositoryImpl;

/**
 * Test class for org.lockss.protocol.PartnerList
 */

public class TestPartnerList extends LockssTestCase {
  static Logger log = Logger.getLogger("TestPartnerList");

  static final Set EMPTY_SET = Collections.EMPTY_SET;
  static final int DEF_MIN_PARTNER_REMOVE_INTERVAL = 10;
  static final int DEF_MAX_PARTNERS = 3;
  static final int DEF_MIN_PARTNERS = 1;
  static final int DEF_MULTICAST_INTERVAL = 10;
  static final String IP1 = "1.1.1.1";
  static final String IP2 = "1.1.1.2";
  static final String DEF_PARTNERS = IP1 + ";" + IP2;
  static final String IP3 = "1.1.1.3";
  static final String IP4 = "1.1.1.4";

  PartnerList pl;
  PeerIdentity peer1;
  PeerIdentity peer2;
  PeerIdentity peer3;
  PeerIdentity peer4;
  private MockLockssDaemon daemon;
  private IdentityManager idmgr = null;

  private void setConfig(PartnerList pl, Configuration config) {
    pl.setConfig(config, ConfigManager.EMPTY_CONFIGURATION,
		 config.differences(ConfigManager.EMPTY_CONFIGURATION));
  }

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();
    TimeBase.setSimulated();
    String tempDirPath = null;
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    }
    catch (IOException ex) {
      fail("unable to create a temporary directory");
    }

    Properties p = new Properties();
    p.setProperty(IdentityManager.PARAM_IDDB_DIR, tempDirPath + "iddb");
    p.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    p.setProperty(IdentityManager.PARAM_LOCAL_IP, "127.0.0.1");
    ConfigurationUtil.setCurrentConfigFromProps(p);
    if (idmgr == null) {
      idmgr = daemon.getIdentityManager();
      idmgr.startService();
    }
    peer1 = idmgr.stringToPeerIdentity("1.1.1.1");
    peer2 = idmgr.stringToPeerIdentity("1.1.1.2");
    peer3 = idmgr.stringToPeerIdentity("1.1.1.3");
    peer4 = idmgr.stringToPeerIdentity("1.1.1.4");
    pl = new PartnerList(idmgr);
    setConfig(pl, getConfig(DEF_MIN_PARTNER_REMOVE_INTERVAL,
			    DEF_MIN_PARTNERS, DEF_MAX_PARTNERS,
			    DEF_MULTICAST_INTERVAL, DEF_PARTNERS));
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  private Configuration getConfig(int minPartnerRemoveInterval,
				  int minPartners,
				  int maxPartners,
				  int multicastInterval,
				  String defaultPartners) {
    Properties prop = new Properties();
    prop.put(PartnerList.PARAM_MIN_PARTNER_REMOVE_INTERVAL,
	     Integer.toString(minPartnerRemoveInterval));
    prop.put(PartnerList.PARAM_MIN_PARTNERS, Integer.toString(minPartners));
    prop.put(PartnerList.PARAM_MAX_PARTNERS, Integer.toString(maxPartners));
    prop.put(PartnerList.PARAM_RECENT_MULTICAST_INTERVAL,
	     Integer.toString(multicastInterval));
    prop.put(PartnerList.PARAM_DEFAULT_LIST, defaultPartners);
    return ConfigurationUtil.fromProps(prop);
  }

  private void removeAll() {
    for (Iterator iter = pl.getPartners().iterator(); iter.hasNext(); ) {
      PeerIdentity id = (PeerIdentity)iter.next();
      pl.removePartner(id);
    }
  }

  public void testInitialPartnerList() {
    // partners in newly configured PartnerList should be default partner list
    assertEquals(SetUtil.set(peer1, peer2), setOf(pl.getPartners()));
  }

  private Set setOf(Collection coll) {
    if (coll == null || coll instanceof Set) {
      return (Set)coll;
    }
    return new HashSet(coll);
  }

  public void testRemovePartner() {
    assertEquals(SetUtil.set(peer1, peer2), setOf(pl.getPartners()));
    pl.removePartner(peer1);
    assertEquals(SetUtil.set(peer2), setOf(pl.getPartners()));
  }

  public void testIsPartner() {
    assertTrue(pl.isPartner(peer1));
    assertTrue(pl.isPartner(peer2));
    pl.removePartner(peer1);
    assertFalse(pl.isPartner(peer1));
    assertTrue(pl.isPartner(peer2));
  }

  public void testNoAddMulticastPartner() {
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.multicastPacketReceivedFrom(peer3);
    // peer3 shouldn't get added because recently seen a multicast from it
    pl.addPartner(peer3);
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
  }

  public void testRemoveMulticastPartner() {
    // this depends on not exceeding max partners (which is 3)
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.addPartner(peer3);
    assertEquals(SetUtil.set(peer3), setOf(pl.getPartners()));
    pl.multicastPacketReceivedFrom(peer3);
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
  }

  public void testAddPartner() {
    // this depends on not exceeding max partners (which is 3)
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.addPartner(peer3);
    assertEquals(SetUtil.set(peer3), setOf(pl.getPartners()));
    pl.addPartner(peer4, 0.0);
    assertEquals(SetUtil.set(peer3), setOf(pl.getPartners()));
    pl.addPartner(peer4, 1.0);
    assertEquals(SetUtil.set(peer3, peer4), setOf(pl.getPartners()));
  }

  public void testAddPartnerOverMax() {
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.addPartner(peer1);
    assertEquals(SetUtil.set(peer1), setOf(pl.getPartners()));
    // want them added at different times so can predict remove order
    TimeBase.step();
    pl.addPartner(peer2, 1.0);
    assertEquals(SetUtil.set(peer1, peer2), setOf(pl.getPartners()));
    TimeBase.step();
    pl.addPartner(peer3, 1.0);
    assertEquals(SetUtil.set(peer1, peer2, peer3), setOf(pl.getPartners()));
    TimeBase.step();
    // adding this one should cause oldest (peer1) to be removed
    pl.addPartner(peer4, 1.0);
    assertEquals(SetUtil.set(peer2, peer3, peer4), setOf(pl.getPartners()));
    TimeBase.step();
    pl.addPartner(peer1, 1.0);
    assertEquals(SetUtil.set(peer1, peer3, peer4), setOf(pl.getPartners()));
  }

  public void testAddPartnerTimeToRemove() {
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.addPartner(peer1);
    assertEquals(SetUtil.set(peer1), setOf(pl.getPartners()));
    // want them added at different times so can predict remove order
    TimeBase.step();
    pl.addPartner(peer2, 1.0);
    assertEquals(SetUtil.set(peer1, peer2), setOf(pl.getPartners()));
    // step past minPartnerRemoveInterval
    TimeBase.step(1000);
    // adding this should remove oldest, peer1, because it's been more the
    // minPartnerRemoveInterval since last remove
    pl.addPartner(peer3, 1.0);
    assertEquals(SetUtil.set(peer2, peer3), setOf(pl.getPartners()));
  }

  public void testAddPartnerRestoresDefault() {
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    // make sure past lastPartnerRemoveTime
    TimeBase.step(1000);
    // adding this should then remove it, then add one from the default list
    pl.addPartner(peer3);
    Set p = setOf(pl.getPartners());
    assertEquals(1, p.size());
    assertTrue(p.equals(SetUtil.set(peer1)) ||
	       p.equals(SetUtil.set(peer2)));
  }

  public void testAddFromDefaultWhenEmpty() {
    pl = new PartnerList(idmgr);
    setConfig(pl, getConfig(DEF_MIN_PARTNER_REMOVE_INTERVAL,
			    DEF_MIN_PARTNERS, DEF_MAX_PARTNERS,
			    DEF_MULTICAST_INTERVAL, ""));
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    // make sure past lastPartnerRemoveTime
    TimeBase.step(1000);
    // adding this should then remove it, then add one from the default list
    // which is empty, leaving the partner list empty, and not throwing
    pl.addPartner(peer3);
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
  }

}
