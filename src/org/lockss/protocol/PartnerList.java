/*
 * $Id: PartnerList.java,v 1.4 2003-03-21 20:49:27 tal Exp $
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
 * Partner list implements the LCAP V1 unicast partner list.
 * Partners (IP addresses) can be added and removed, checked for presence,
 * and record the last time a message was received.
 */
class PartnerList {
  static final String PREFIX = LcapRouter.PREFIX;
  static final String PARAM_MAX_PARTNER_LIFE = PREFIX + "maxPartnerLife";
  static final String PARAM_MAX_PARTNERS = PREFIX + "maxPartners";
  static final String PARAM_DEFAULT_LIST = PREFIX + "defaultPartnerList";
  static final String PARAM_RECENT_MULTICAST_INTERVAL =
    PREFIX + "recentMulticastInterval";

  static Logger log = Logger.getLogger("PartnerList");

  static Random random = new Random();

  Map partners = new HashMap();
  Map lastMulticastReceived = new LRUMap(100);
  List defaultPartnerList;
  long lastPartnerRemoveTime = 0;
  long recentMulticastInterval = 0;
  long maxPartnerLife = 0;
  long maxPartners = 0;

  PartnerList() {
  }

  void setConfig(Configuration config) {
    maxPartners = config.getInt(PARAM_MAX_PARTNERS, 2);
    maxPartnerLife = config.getInt(PARAM_MAX_PARTNER_LIFE, 0);
    recentMulticastInterval =
      config.getInt(PARAM_RECENT_MULTICAST_INTERVAL, 0);
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

  Set getPartners() {
    return new HashSet(partners.keySet());
  }

  void multicastPacketReceivedFrom(InetAddress ip) {
    removePartner(ip);
    lastMulticastReceived.put(ip, nowLong());
  }

  void addPartner(InetAddress partnerIP, double probability) {
    if (ProbabilisticChoice.choose(probability)) {
      addPartner(partnerIP);
    }
  }

  void addPartner(InetAddress partnerIP) {
    // don't add if recently received multicast from him
    Long lastRcv = (Long)lastMulticastReceived.get(partnerIP);
    if (lastRcv != null &&
	((TimeBase.nowMs() - lastRcv.longValue()) < recentMulticastInterval)) {
      return;
    }
    partners.put(partnerIP, nowLong());
    if (partners.size() > maxPartners &&
	((TimeBase.nowMs() - lastPartnerRemoveTime) > maxPartnerLife)) {
      removeLeastRecent();
    }
    if (partners.size() == 0) {
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
    InetAddress oldest = findLeastRecent();
    if (oldest != null) {
      removePartner(oldest);
    }
  }

  InetAddress findLeastRecent() {
    long oldestRcvTime = Long.MAX_VALUE;
    InetAddress oldest = null;
    for (Iterator iter = partners.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry entry = (Map.Entry)iter.next();
      InetAddress ip = (InetAddress)entry.getKey();
      Long l = (Long)entry.getValue();
      if (l != null && l.longValue() < oldestRcvTime) {
	oldestRcvTime = l.longValue();
	oldest = ip;
      }
    }
    return oldest;
  }

  void removePartner(InetAddress partnerIP) {
    if (partners.containsKey(partnerIP)) {
      partners.remove(partnerIP);
      lastPartnerRemoveTime = TimeBase.nowMs();
    }
  }

  void addFromDefaultList() {
    int ix = random.nextInt(defaultPartnerList.size());
    partners.put(defaultPartnerList.get(ix), nowLong());
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
