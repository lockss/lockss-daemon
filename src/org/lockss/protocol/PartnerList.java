/*
 * $Id: PartnerList.java,v 1.11 2003-04-23 20:33:39 tal Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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
  static final long DEFAULT_RECENT_MULTICAST_INTERVAL = Constants.HOUR;

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
  List defaultPartnerList;
  long recentMulticastInterval;
  long minPartnerRemoveInterval;

  /** Create a PartnerList */
  public PartnerList() {
  }

  /** Configure the PartnerList */
  public void setConfig(Configuration config) {
    int maxPartners = config.getInt(PARAM_MAX_PARTNERS, DEFAULT_MAX_PARTNERS);
    if (maxPartners != partners.getMaximumSize()) {
      partners.setMaximumSize(maxPartners);
    }
    minPartnerRemoveInterval =
      config.getTimeInterval(PARAM_MIN_PARTNER_REMOVE_INTERVAL,
			     DEFAULT_MIN_PARTNER_REMOVE_INTERVAL);
    recentMulticastInterval =
      config.getTimeInterval(PARAM_RECENT_MULTICAST_INTERVAL,
			     DEFAULT_RECENT_MULTICAST_INTERVAL);
    String s = config.get(PARAM_DEFAULT_LIST, "");
    List stringList = StringUtil.breakAt(s, ';');
    List newDefaultList = new ArrayList();
    for (Iterator iter = stringList.iterator(); iter.hasNext(); ) {
      try {
	newDefaultList.add(InetAddress.getByName((String)iter.next()));
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

  /** Return a snapshot of the partner set */
  public Collection getPartners() {
    return new ArrayList(partners.keySet());
  }

  /** Inform the PartnerList that a multicast packet was received.
   * @param ip the address of the packet sender
   */
  public void multicastPacketReceivedFrom(InetAddress ip) {
    removePartner(ip);
    lastMulticastReceived.put(ip, nowLong());
  }

  /** Possibly add a partner to the list
   * @param partnerIP the address of the partner
   * @probability the probability of adding the partner
   */
  public void addPartner(InetAddress partnerIP, double probability) {
    log.debug2("addPartner(" + partnerIP + ", " + probability + ")");
    if (ProbabilisticChoice.choose(probability)) {
      addPartner(partnerIP);
    }
  }

  void addPartner(InetAddress partnerIP) {
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
    for (Iterator iter = defaultPartnerList.iterator(); iter.hasNext(); ) {
      InetAddress ip = (InetAddress)iter.next();
      addPartner(ip);
    }
  }

  void removeLeastRecent() {
    InetAddress oldest = (InetAddress)partners.getFirstKey();
    if (oldest != null) {
      removePartner(oldest);
    }
  }

  /** Remove a partner from the list
   * @param partnerIP the partner to remove
   */
  public void removePartner(InetAddress partnerIP) {
    if (partners.containsKey(partnerIP)) {
      log.debug("Removing partner " + partnerIP);
      partners.remove(partnerIP);
      lastPartnerRemoveTime = TimeBase.nowMs();
    }
  }

  void addFromDefaultList() {
    if (!defaultPartnerList.isEmpty()) {
      int ix = random.nextInt(defaultPartnerList.size());
      partners.put(defaultPartnerList.get(ix), nowLong());
    }
  }

  Long nowLong() {
    return new Long(TimeBase.nowMs());
  }

//   static class Element {
//     InetAddress addr;
//     long lastReceiveTime;

//     Element(LcapIdentity id) throws UnknownHostException {
//       this(id.getAddress());
//     }

//     Element(InetAddress ip) {
//       addr = ip;
//     }
//   }
}
