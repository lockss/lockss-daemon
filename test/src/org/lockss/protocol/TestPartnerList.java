/*
 * $Id: TestPartnerList.java,v 1.9.14.1 2004-02-03 01:03:39 tlipkis Exp $
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

package org.lockss.protocol;

import java.io.*;
import java.net.*;
import java.util.*;
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for org.lockss.protocol.PartnerList
 */

public class TestPartnerList extends LockssTestCase {
  static Logger log = Logger.getLogger("TestPartnerList");

  static final Set EMPTY_SET = Collections.EMPTY_SET;
  static final int DEF_MIN_PARTNER_REMOVE_INTERVAL = 10;
  static final int DEF_MAX_PARTNERS = 3;
  static final int DEF_MULTICAST_INTERVAL = 10;
  static final String IP1 = "1.1.1.1";
  static final String IP2 = "1.1.1.2";
  static final String DEF_PARTNERS = IP1 + ";" + IP2;
  static final String IP3 = "1.1.1.3";
  static final String IP4 = "1.1.1.4";

  PartnerList pl;
  IPAddr inet1;
  IPAddr inet2;
  IPAddr inet3;
  IPAddr inet4;

  private void setConfig(PartnerList pl, Configuration config) {
    pl.setConfig(config, ConfigManager.EMPTY_CONFIGURATION,
		 config.keySet());
  }

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
    pl = new PartnerList();
    setConfig(pl, getConfig(DEF_MIN_PARTNER_REMOVE_INTERVAL, DEF_MAX_PARTNERS,
			    DEF_MULTICAST_INTERVAL, DEF_PARTNERS));
    inet1 = IPAddr.getByName(IP1);
    inet2 = IPAddr.getByName(IP2);
    inet3 = IPAddr.getByName(IP3);
    inet4 = IPAddr.getByName(IP4);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  private Configuration getConfig(int minPartnerRemoveInterval,
				  int maxPartners,
				  int multicastInterval,
				  String defaultPartners) {
    Properties prop = new Properties();
    prop.put(PartnerList.PARAM_MIN_PARTNER_REMOVE_INTERVAL,
	     Integer.toString(minPartnerRemoveInterval));
    prop.put(PartnerList.PARAM_MAX_PARTNERS, Integer.toString(maxPartners));
    prop.put(PartnerList.PARAM_RECENT_MULTICAST_INTERVAL,
	     Integer.toString(multicastInterval));
    prop.put(PartnerList.PARAM_DEFAULT_LIST, defaultPartners);
    return ConfigurationUtil.fromProps(prop);
  }

  private void removeAll() {
    for (Iterator iter = pl.getPartners().iterator(); iter.hasNext(); ) {
      IPAddr ip = (IPAddr)iter.next();
      pl.removePartner(ip);
    }
  }

  public void testInitialPartnerList() {
    // partners in newly configured PartnerList should be default partner list
    assertEquals(SetUtil.set(inet1, inet2), setOf(pl.getPartners()));
  }

  private Set setOf(Collection coll) {
    if (coll == null || coll instanceof Set) {
      return (Set)coll;
    }
    return new HashSet(coll);
  }

  public void testRemovePartner() {
    assertEquals(SetUtil.set(inet1, inet2), setOf(pl.getPartners()));
    pl.removePartner(inet1);
    assertEquals(SetUtil.set(inet2), setOf(pl.getPartners()));
  }

  public void testNoAddMulticastPartner() {
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.multicastPacketReceivedFrom(inet3);
    // inet3 shouldn't get added because recently seen a multicast from it
    pl.addPartner(inet3);
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
  }

  public void testRemoveMulticastPartner() {
    // this depends on not exceeding max partners (which is 3)
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.addPartner(inet3);
    assertEquals(SetUtil.set(inet3), setOf(pl.getPartners()));
    pl.multicastPacketReceivedFrom(inet3);
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
  }

  public void testAddPartner() {
    // this depends on not exceeding max partners (which is 3)
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.addPartner(inet3);
    assertEquals(SetUtil.set(inet3), setOf(pl.getPartners()));
    pl.addPartner(inet4, 0.0);
    assertEquals(SetUtil.set(inet3), setOf(pl.getPartners()));
    pl.addPartner(inet4, 1.0);
    assertEquals(SetUtil.set(inet3, inet4), setOf(pl.getPartners()));
  }

  public void testAddPartnerOverMax() {
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.addPartner(inet1);
    assertEquals(SetUtil.set(inet1), setOf(pl.getPartners()));
    // want them added at different times so can predict remove order
    TimeBase.step();
    pl.addPartner(inet2, 1.0);
    assertEquals(SetUtil.set(inet1, inet2), setOf(pl.getPartners()));
    TimeBase.step();
    pl.addPartner(inet3, 1.0);
    assertEquals(SetUtil.set(inet1, inet2, inet3), setOf(pl.getPartners()));
    TimeBase.step();
    // adding this one should cause oldest (inet1) to be removed
    pl.addPartner(inet4, 1.0);
    assertEquals(SetUtil.set(inet2, inet3, inet4), setOf(pl.getPartners()));
    TimeBase.step();
    pl.addPartner(inet1, 1.0);
    assertEquals(SetUtil.set(inet1, inet3, inet4), setOf(pl.getPartners()));
  }

  public void testAddPartnerTimeToRemove() {
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    pl.addPartner(inet1);
    assertEquals(SetUtil.set(inet1), setOf(pl.getPartners()));
    // want them added at different times so can predict remove order
    TimeBase.step();
    pl.addPartner(inet2, 1.0);
    assertEquals(SetUtil.set(inet1, inet2), setOf(pl.getPartners()));
    // step past minPartnerRemoveInterval
    TimeBase.step(1000);
    // adding this should remove oldest, inet1, because it's been more the
    // minPartnerRemoveInterval since last remove
    pl.addPartner(inet3, 1.0);
    assertEquals(SetUtil.set(inet2, inet3), setOf(pl.getPartners()));
  }

  public void testAddPartnerRestoresDefault() {
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    // make sure past lastPartnerRemoveTime
    TimeBase.step(1000);
    // adding this should then remove it, then add one from the default list
    pl.addPartner(inet3);
    Set p = setOf(pl.getPartners());
    assertEquals(1, p.size());
    assertTrue(p.equals(SetUtil.set(inet1)) ||
	       p.equals(SetUtil.set(inet2)));
  }

  public void testAddFromDefaultWhenEmpty() {
    pl = new PartnerList();
    setConfig(pl, getConfig(DEF_MIN_PARTNER_REMOVE_INTERVAL, DEF_MAX_PARTNERS,
			    DEF_MULTICAST_INTERVAL, ""));
    removeAll();
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
    // make sure past lastPartnerRemoveTime
    TimeBase.step(1000);
    // adding this should then remove it, then add one from the default list
    // which is empty, leaving the partner list empty, and not throwing
    pl.addPartner(inet3);
    assertEquals(EMPTY_SET, setOf(pl.getPartners()));
  }

}
