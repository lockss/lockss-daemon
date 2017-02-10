/*
 * $Id$
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
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.apache.commons.collections.map.LRUMap;

/**
 * PartnerList implements the LCAP V1 unicast partner list.
 * Partners (IP addresses) can be added and removed, checked for presence,
 * and record the last time a message was received.
 */
class PartnerList {
  static final String PREFIX = LcapDatagramRouter.PREFIX;
  static final String PARAM_DEFAULT_LIST = PREFIX + "defaultPartnerList";

  static final String PARAM_MIN_PARTNER_REMOVE_INTERVAL =
    PREFIX + "minPartnerRemoveInterval";
  static final long DEFAULT_MIN_PARTNER_REMOVE_INTERVAL = Constants.HOUR;

  static final String PARAM_MAX_PARTNERS = PREFIX + "maxPartners";
  static final int DEFAULT_MAX_PARTNERS = 3;

  static final String PARAM_MIN_PARTNERS = PREFIX + "minPartners";
  static final int DEFAULT_MIN_PARTNERS = 1;

  static final String PARAM_RECENT_MULTICAST_INTERVAL =
    PREFIX + "recentMulticastInterval";
  static final long DEFAULT_RECENT_MULTICAST_INTERVAL = 90 * Constants.MINUTE;


  static Logger log = Logger.getLogger("PartnerList");

  static LockssRandom random = new LockssRandom();

  // partners is an LRUMap that records the most recent time an entry was
  // automatically removed
  LRUMap partners = new PartnersLRUMap(DEFAULT_MAX_PARTNERS);
  long lastPartnerRemoveTime = 0;

  Map lastMulticastReceived = new LRUMap(100);
  List defaultPartnerList = new ArrayList();
  long recentMulticastInterval;
  long minPartnerRemoveInterval;
  int maxPartners;
  int minPartners;
  private IdentityManager idMgr;

  /** Create a PartnerList */
  public PartnerList(IdentityManager idMgr) {
    this.idMgr = idMgr;
  }

  /** Configure the PartnerList */
  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    maxPartners = config.getInt(PARAM_MAX_PARTNERS, DEFAULT_MAX_PARTNERS);
    minPartners = Math.min(maxPartners,
			   config.getInt(PARAM_MIN_PARTNERS,
					 DEFAULT_MIN_PARTNERS));
    if (maxPartners != partners.maxSize()) {
      LRUMap newPartners = new PartnersLRUMap(maxPartners);
      newPartners.putAll(partners);
      partners = newPartners;
    }
    minPartnerRemoveInterval =
      config.getTimeInterval(PARAM_MIN_PARTNER_REMOVE_INTERVAL,
			     DEFAULT_MIN_PARTNER_REMOVE_INTERVAL);
    recentMulticastInterval =
      config.getTimeInterval(PARAM_RECENT_MULTICAST_INTERVAL,
			     DEFAULT_RECENT_MULTICAST_INTERVAL);
    if (changedKeys.contains(PARAM_DEFAULT_LIST)) {
      List stringList = config.getList(PARAM_DEFAULT_LIST);
      List newDefaultList = new ArrayList();
      for (Iterator iter = stringList.iterator(); iter.hasNext(); ) try {
	PeerIdentity partner = idMgr.stringToPeerIdentity((String)iter.next());
	newDefaultList.add(partner);
      } catch (IdentityManager.MalformedIdentityKeyException uhe) {
	log.error("default partner list throvi ws " + uhe);
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

  /**
   * Inform the PartnerList that a multicast packet was received.
   * @param id the PeerIdentity of the packet sender
   */
  public void multicastPacketReceivedFrom(PeerIdentity id) {
    removePartner(id);
    lastMulticastReceived.put(id, nowLong());
  }

  /**
   * Return true if the id is currently on the partner list
   * @param id the PeerIdentity to check
   */
  public boolean isPartner(PeerIdentity id) {
    return partners.containsKey(id);
  }

  /** Possibly add a partner to the list
   * @param id the PeerIdentity of the partner
   * @probability the probability of adding the partner
   */
  public void addPartner(PeerIdentity id, double probability) {
    log.debug2("addPartner(" + id + ", " + probability + ")");
    if (ProbabilisticChoice.choose(probability)) {
      addPartner(id);
    }
  }

  void addPartner(PeerIdentity id) {
    // don't ever add ourself
    if (idMgr.isLocalIdentity(id)) {
      return;
    }
    // don't add if recently received multicast from him
    Long lastRcv = (Long)lastMulticastReceived.get(id);
    if (lastRcv != null &&
	(TimeBase.msSince(lastRcv.longValue()) < recentMulticastInterval)) {
      return;
    }
    if (log.isDebug() && !isPartner(id)) {
      log.debug("Adding partner " + id);
    }
    partners.put(id, nowLong());
    if (TimeBase.msSince(lastPartnerRemoveTime) > minPartnerRemoveInterval) {
      removeLeastRecent();
    }
    if (partners.size() < minPartners) {
      addFromDefaultList();
    }
  }

  void addDefaultPartners() {
    lastPartnerRemoveTime = TimeBase.nowMs();
    // randomly permute the defaultPartnerList
    Collections.shuffle(defaultPartnerList);
    // then add all its elements.
    for (Iterator iter = defaultPartnerList.iterator(); iter.hasNext(); ) {
      PeerIdentity id = (PeerIdentity)iter.next();
      addPartner(id);
    }
  }

  void removeLeastRecent() {
    PeerIdentity oldest = (PeerIdentity)partners.firstKey();
    if (oldest != null) {
      removePartner(oldest);
    }
  }

  /** Remove a partner from the list
   * @param id the partner to remove
   */
  public void removePartner(PeerIdentity id) {
    if (isPartner(id)) {
      log.debug("Removing partner " + id);
      partners.remove(id);
      lastPartnerRemoveTime = TimeBase.nowMs();
    }
  }

  void addFromDefaultList() {
    if (!defaultPartnerList.isEmpty()) {
      // randomly permute the defaultPartnerList
      Collections.shuffle(defaultPartnerList);
      // then add the first one that isn't already in list
      for (Iterator iter = defaultPartnerList.iterator(); iter.hasNext(); ) {
	PeerIdentity id = (PeerIdentity)iter.next();
	if (!isPartner(id)) {
	  addPartner(id);
	  return;
	}
      }
    }
  }

  Long nowLong() {
    return new Long(TimeBase.nowMs());
  }

  // LRUMap that records last removal time
  class PartnersLRUMap extends LRUMap {
    PartnersLRUMap(int size) {
      super(size);
    }
    protected boolean removeLRU(LinkEntry entry) {
      lastPartnerRemoveTime = TimeBase.nowMs();
      return true;
    }
  }

}
