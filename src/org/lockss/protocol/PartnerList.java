/*
 * $Id: PartnerList.java,v 1.16.16.1 2004-02-03 01:03:41 tlipkis Exp $
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
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.apache.commons.collections.LRUMap;

/**
 * PartnerList implements the LCAP V1 unicast partner list.
 * Partners (IP addresses) can be added and removed, checked for presence,
 * and record the last time a message was received.
 */
class PartnerList {
  static final String PREFIX = LcapRouter.PREFIX;
  static final String PARAM_MIN_PARTNER_REMOVE_INTERVAL =
    PREFIX + "minPartnerRemoveInterval";
  static final String PARAM_MAX_PARTNERS = PREFIX + "maxPartners";
  static final String PARAM_DEFAULT_LIST = PREFIX + "defaultPartnerList";
  static final String PARAM_RECENT_MULTICAST_INTERVAL =
    PREFIX + "recentMulticastInterval";

  static final long DEFAULT_MIN_PARTNER_REMOVE_INTERVAL = Constants.HOUR;
  static final int DEFAULT_MAX_PARTNERS = 3;
  static final long DEFAULT_RECENT_MULTICAST_INTERVAL = 90 * Constants.MINUTE;

  static Logger log = Logger.getLogger("PartnerList");

  static Random random = new Random();

  // partners is an LRUMap that records the most recent time an entry was
  // automatically removed
  LRUMap partners = new LRUMap(DEFAULT_MAX_PARTNERS) {
      protected void processRemovedLRU(Object key, Object value) {
	lastPartnerRemoveTime = TimeBase.nowMs();
      }
    };
  long lastPartnerRemoveTime = 0;

  Map lastMulticastReceived = new LRUMap(100);
  List defaultPartnerList = new ArrayList();
  long recentMulticastInterval;
  long minPartnerRemoveInterval;
  int maxPartners;
  IPAddr localIP;

  /** Create a PartnerList */
  public PartnerList() {
  }

  /** Tell us the local IP address */
  public void setLocalIP(IPAddr local) {
    localIP = local;
  }

  /** Configure the PartnerList */
  public void setConfig(Configuration config, Configuration oldConfig,
			Set changedKeys) {
    maxPartners = config.getInt(PARAM_MAX_PARTNERS, DEFAULT_MAX_PARTNERS);
    if (maxPartners != partners.getMaximumSize()) {
      partners.setMaximumSize(maxPartners);
    }
    minPartnerRemoveInterval =
      config.getTimeInterval(PARAM_MIN_PARTNER_REMOVE_INTERVAL,
			     DEFAULT_MIN_PARTNER_REMOVE_INTERVAL);
    recentMulticastInterval =
      config.getTimeInterval(PARAM_RECENT_MULTICAST_INTERVAL,
			     DEFAULT_RECENT_MULTICAST_INTERVAL);
    if (changedKeys.contains(PARAM_DEFAULT_LIST)) {
      String s = config.get(PARAM_DEFAULT_LIST, "");
      List stringList = StringUtil.breakAt(s, ';');
      List newDefaultList = new ArrayList();
      for (Iterator iter = stringList.iterator(); iter.hasNext(); ) {
	try {
	  newDefaultList.add(IPAddr.getByName((String)iter.next()));
	} catch (UnknownHostException e) {
	  log.warning("Can't add default partner", e);
	}
      }
      if (newDefaultList.isEmpty()) {
	log.error("Default partner list is empty");
      }
      defaultPartnerList = newDefaultList;
      if (partners.isEmpty()) {
	addDefaultPartners();
      }
    }
  }

  /** Return a snapshot of the partner set */
  public Collection getPartners() {
    return new ArrayList(partners.keySet());
  }

  /** Inform the PartnerList that a multicast packet was received.
   * @param ip the address of the packet sender
   */
  public void multicastPacketReceivedFrom(IPAddr ip) {
    removePartner(ip);
    lastMulticastReceived.put(ip, nowLong());
  }

  /** Possibly add a partner to the list
   * @param partnerIP the address of the partner
   * @probability the probability of adding the partner
   */
  public void addPartner(IPAddr partnerIP, double probability) {
    log.debug2("addPartner(" + partnerIP + ", " + probability + ")");
    if (ProbabilisticChoice.choose(probability)) {
      addPartner(partnerIP);
    }
  }

  void addPartner(IPAddr partnerIP) {
    // don't ever add ourself
    if (partnerIP.equals(localIP)) {
      return;
    }
    // don't add if recently received multicast from him
    Long lastRcv = (Long)lastMulticastReceived.get(partnerIP);
    if (lastRcv != null &&
	(TimeBase.msSince(lastRcv.longValue()) < recentMulticastInterval)) {
      return;
    }
    if (log.isDebug() && !partners.containsKey(partnerIP)) {
      log.debug("Adding partner " + partnerIP);
    }
    partners.put(partnerIP, nowLong());
    if (TimeBase.msSince(lastPartnerRemoveTime) > minPartnerRemoveInterval) {
      removeLeastRecent();
    }
    if (partners.isEmpty()) {
      addFromDefaultList();
    }
  }

  void addDefaultPartners() {
    lastPartnerRemoveTime = TimeBase.nowMs();
    // randomly permute the defaultPartnerList
    Collections.shuffle(defaultPartnerList);
    // then add all its elements.
    for (Iterator iter = defaultPartnerList.iterator(); iter.hasNext(); ) {
      IPAddr ip = (IPAddr)iter.next();
      addPartner(ip);
    }
  }

  void removeLeastRecent() {
    IPAddr oldest = (IPAddr)partners.getFirstKey();
    if (oldest != null) {
      removePartner(oldest);
    }
  }

  /** Remove a partner from the list
   * @param partnerIP the partner to remove
   */
  public void removePartner(IPAddr partnerIP) {
    if (partners.containsKey(partnerIP)) {
      log.debug("Removing partner " + partnerIP);
      partners.remove(partnerIP);
      lastPartnerRemoveTime = TimeBase.nowMs();
    }
  }

  void addFromDefaultList() {
    if (!defaultPartnerList.isEmpty()) {
      int ix = random.nextInt(defaultPartnerList.size());
      addPartner((IPAddr)defaultPartnerList.get(ix));
    }
  }

  Long nowLong() {
    return new Long(TimeBase.nowMs());
  }

//   static class Element {
//     IPAddr addr;
//     long lastReceiveTime;

//     Element(LcapIdentity id) throws UnknownHostException {
//       this(id.getAddress());
//     }

//     Element(IPAddr ip) {
//       addr = ip;
//     }
//   }
}
