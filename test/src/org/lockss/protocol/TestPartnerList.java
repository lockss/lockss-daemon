/*
 * $Id: TestPartnerList.java,v 1.2 2003-03-21 07:28:56 tal Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
  static final int DEF_MAX_LIFE = 10;
  static final int DEF_MAX_PARTNERS = 3;
  static final int DEF_MULTICAST_INTERVAL = 10;
  static final String IP1 = "1.1.1.1";
  static final String IP2 = "1.1.1.2";
  static final String DEF_PARTNERS = IP1 + ";" + IP2;
  static final String IP3 = "1.1.1.3";
  static final String IP4 = "1.1.1.4";

  PartnerList pl;
  InetAddress inet1;
  InetAddress inet2;
  InetAddress inet3;
  InetAddress inet4;

  public void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
    pl = new PartnerList();
    pl.setConfig(getConfig(DEF_MAX_LIFE, DEF_MAX_PARTNERS,
			   DEF_MULTICAST_INTERVAL, DEF_PARTNERS));
    inet1 = InetAddress.getByName(IP1);
    inet2 = InetAddress.getByName(IP2);
    inet3 = InetAddress.getByName(IP3);
    inet4 = InetAddress.getByName(IP4);
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  private Configuration getConfig(int maxLife, int maxPartners,
				  int multicastInterval,
				  String defaultPartners) {
    Properties prop = new Properties();
    prop.put(PartnerList.PARAM_MAX_PARTNER_LIFE, Integer.toString(maxLife));
    prop.put(PartnerList.PARAM_MAX_PARTNERS, Integer.toString(maxPartners));
    prop.put(PartnerList.PARAM_RECENT_MULTICAST_INTERVAL,
	     Integer.toString(multicastInterval));
    prop.put(PartnerList.PARAM_DEFAULT_LIST, defaultPartners);
    return ConfigurationUtil.fromProps(prop);
  }

  public void removeAll() {
    for (Iterator iter = pl.getPartners().iterator(); iter.hasNext(); ) {
      InetAddress ip = (InetAddress)iter.next();
      pl.removePartner(ip);
    }
  }

  public void testInitialPartnerList() {
    // partners in newly configured PartnerList should be default partner list
    assertEquals(SetUtil.set(inet1, inet2), pl.getPartners());
  }

  public void testRemovePartner() {
    assertEquals(SetUtil.set(inet1, inet2), pl.getPartners());
    pl.removePartner(inet1);
    assertEquals(SetUtil.set(inet2), pl.getPartners());
  }

  public void testNoAddMulticastPartner() {
    // this depends on not exceeding max partners (which is 3)
    removeAll();
    assertEquals(new HashSet(), pl.getPartners());
    pl.multicastPacketReceivedFrom(inet3);
    pl.addPartner(inet3);
    assertEquals(Collections.EMPTY_SET, pl.getPartners());
  }

  public void testRemoveMulticastPartner() {
    // this depends on not exceeding max partners (which is 3)
    removeAll();
    assertEquals(new HashSet(), pl.getPartners());
    pl.addPartner(inet3);
    assertEquals(SetUtil.set(inet3), pl.getPartners());
    pl.multicastPacketReceivedFrom(inet3);
    assertEquals(Collections.EMPTY_SET, pl.getPartners());
  }

  public void testAddPartner() {
    // this depends on not exceeding max partners (which is 3)
    removeAll();
    assertEquals(new HashSet(), pl.getPartners());
    pl.addPartner(inet3);
    assertEquals(SetUtil.set(inet3), pl.getPartners());
    pl.addPartner(inet4, 0.0);
    assertEquals(SetUtil.set(inet3), pl.getPartners());
    pl.addPartner(inet4, 1.0);
    assertEquals(SetUtil.set(inet3, inet4), pl.getPartners());
  }

  public void testAddPartnerOverMax() {
    removeAll();
    assertEquals(new HashSet(), pl.getPartners());
    // make sure past lastPartnerRemoveTime
    TimeBase.step(1000);		
    pl.addPartner(inet1);
    assertEquals(SetUtil.set(inet1), pl.getPartners());
    // want them added at different times so can predict remove order
    TimeBase.step();		
    pl.addPartner(inet2, 1.0);
    assertEquals(SetUtil.set(inet1, inet2), pl.getPartners());
    TimeBase.step();		
    pl.addPartner(inet3, 1.0);
    assertEquals(SetUtil.set(inet1, inet2, inet3), pl.getPartners());
    TimeBase.step();		
    // adding this one should cause oldest (inet1) to be removed
    pl.addPartner(inet4, 1.0);
    assertEquals(SetUtil.set(inet2, inet3, inet4), pl.getPartners());
    TimeBase.step();		
    // no removal this time; lastPartnerRemoveTime not exceeded
    pl.addPartner(inet1, 1.0);
    assertEquals(SetUtil.set(inet1, inet2, inet3, inet4), pl.getPartners());
  }

  public void testAddPartnerRestoresDefault() {
    pl = new PartnerList();
    // configure with maxPartners = 0
    pl.setConfig(getConfig(DEF_MAX_LIFE, 0,
			   DEF_MULTICAST_INTERVAL, DEF_PARTNERS));
    removeAll();
    assertEquals(new HashSet(), pl.getPartners());
    // make sure past lastPartnerRemoveTime
    TimeBase.step(1000);		
    // adding this should then remove it, then add one from the default list
    pl.addPartner(inet3);
    Set p = pl.getPartners();
    assertEquals(1, p.size());
    assertTrue(p.equals(SetUtil.set(inet1)) ||
	       p.equals(SetUtil.set(inet2)));
  }

}
