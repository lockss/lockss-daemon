/*
 * $Id: IdentityManager.java,v 1.45 2004-09-20 14:20:37 dshr Exp $
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
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.*;
import org.lockss.util.*;
import org.lockss.app.*;
import org.lockss.poller.Vote;

/**
 * Abstraction for identity of a LOCKSS cache.  Currently wraps an IP address.
 * @author Claire Griffin
 * @version 1.0
 */
public class IdentityManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  public static Logger log = Logger.getLogger("IdentityManager");

  public static final String PARAM_LOCAL_IP =
    Configuration.PREFIX + "localIPAddress";

  static final String PREFIX = Configuration.PREFIX + "id.";
  static final String PARAM_MAX_DELTA = PREFIX + "maxReputationDelta";
  static final int DEFAULT_MAX_DELTA = 100;

  static final String PARAM_AGREE_DELTA = PREFIX + "agreeDelta";
  static final int DEFAULT_AGREE_DELTA = 100;

  static final String PARAM_DISAGREE_DELTA = PREFIX + "disagreeDelta";
  static final int DEFAULT_DISAGREE_DELTA = -150;

  static final String PARAM_CALL_INTERNAL = PREFIX + "callInternalDelta";
  static final int DEFAULT_CALL_INTERNAL = 100;

  static final String PARAM_SPOOF_DETECTED = PREFIX + "spoofDetected"; //no
  static final int DEFAULT_SPOOF_DETECTED = -30;

  static final String PARAM_REPLAY_DETECTED = PREFIX + "replayDetected";
  static final int DEFAULT_REPLAY_DETECTED = -20;

  static final String PARAM_ATTACK_DETECTED = PREFIX + "attackDetected"; //no
  static final int DEFAULT_ATTACK_DETECTED = -500;

  static final String PARAM_VOTE_NOTVERIFIED = PREFIX + "voteNotVerified ";
  static final int DEFAULT_VOTE_NOTVERIFIED = -30;

  static final String PARAM_VOTE_VERIFIED = PREFIX + "voteVerified";
  static final int DEFAULT_VOTE_VERIFIED = 40;

  static final String PARAM_VOTE_DISOWNED = PREFIX + "voteDisowned";
  static final int DEFAULT_VOTE_DISOWNED = -400;

  public static final String PARAM_IDDB_DIR = PREFIX + "database.dir";

  static final String IDDB_FILENAME = "iddb.xml";
  // fully qualify for XmlMarshaller
  public static final String MAPPING_FILE_NAME =
      "/org/lockss/protocol/idmapping.xml";

  /* Reputation constants */
  public static final int MAX_DELTA = 0;
  public static final int AGREE_VOTE = 1;
  public static final int DISAGREE_VOTE = 2;
  public static final int CALL_INTERNAL = 3;
  public static final int SPOOF_DETECTED = 4;
  public static final int REPLAY_DETECTED = 5;
  public static final int ATTACK_DETECTED = 6;
  public static final int VOTE_NOTVERIFIED = 7;
  public static final int VOTE_VERIFIED = 8;
  public static final int VOTE_DISOWNED = 9;

  static final int INITIAL_REPUTATION = 500;
  static final int REPUTATION_NUMERATOR = 1000;

  static LockssRandom theRandom = new LockssRandom();

  /*
   * There are two classes representing the identity of a peer.
   * PeerData is used only by the IdentityManager; instances
   * contain all the actual information about the peer.  PeerIdentity
   * is an opaque cookie that the IdentityManager provides to its
   * clients; they use it to refer to a peer without having a reference
   * to an instance of PeerData.  Peers have very long lives and
   * potentially have a lot of information but only a few will be
   * actively in use at any one time. The PeerData
   * is found by looking up the PeerIdentity in a HashMap called
   * thePeerCache;  it should be a cache of active PeerData
   * instances but it currently contains all of them.
   * XXX currently using LcapIdentity instead of PeerData
   */
  protected static String localIdentityStr;
  protected static LcapIdentity theLocalLcapIdentity;
  protected static PeerIdentity theLocalPeerIdentity;
  protected static IPAddr theLocalIPAddr = null;

  int[] reputationDeltas = new int[10];

  private Map agreeMap = null;
  private Map disagreeMap = null;

  //derivable from the above two; included for speed
  private Map cachesToFetchFrom = null;

  private String identityMapLock = "lock";

  HashMap theIdentities = new HashMap(); // all known identities

  public IdentityManager() { }

  public void initService(LockssDaemon daemon) throws LockssAppException {
    super.initService(daemon);
    localIdentityStr = Configuration.getParam(PARAM_LOCAL_IP);
    try {
      theLocalIPAddr = IPAddr.getByName(localIdentityStr);
    } catch (UnknownHostException uhe) {
      log.error(PARAM_LOCAL_IP +
		" can't be looked up - IdentityManager cannot start.");
      throw new
	LockssAppException(PARAM_LOCAL_IP +
			   " can't be looked up - IdentityManager cannot start.");
    }
    if (localIdentityStr == null) {
      log.error(PARAM_LOCAL_IP +
		" is not set - IdentityManager cannot start.");
      throw new
	LockssAppException(PARAM_LOCAL_IP +
			      " is not set - IdentityManager cannot start.");
    }
  }

  /**
   * start the identity manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    theLocalPeerIdentity = PeerIdentity.stringToIdentity(localIdentityStr);
    synchronized (theIdentities) {
      if (!theIdentities.containsKey(theLocalPeerIdentity)) {
	// XXX V1-specific
	theIdentities.put(theLocalPeerIdentity,
			  new LcapIdentity(theLocalIPAddr, 0));
      }
    }
    reloadIdentities();
    
    log.info("Local identity: " + getLocalPeerIdentity());
    getDaemon().getStatusService().registerStatusAccessor("Identities",
							  new Status());
    Vote.setIdentityManager(this); 
    LcapMessage.setIdentityManager(this);
    PartnerList.setIdentityManager(this);
    IdentityAgreement.setIdentityManager(this);
  }

  /**
   * stop the identity manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    try {
      storeIdentities();
    }
    catch (ProtocolException ex) {
    }
    super.stopService();
    Vote.setIdentityManager(null);
    LcapMessage.setIdentityManager(null);
    PartnerList.setIdentityManager(null);
  }

  /**
   * ipAddrToPeerIdentity returns the peer identity matching the
   * IP address and port.  An instance is created if necesary.
   * Used only by LcapRouter (and soon by its stream analog).
   * @param addr the IPAddr of the peer, null for the local peer
   * @param port the port of the peer
   * @return the PeerIdentity representing the peer
   */
  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr, int port) {
    PeerIdentity ret = null;
    if (addr == null) {
      ret = theLocalPeerIdentity;
    } else {
      ret = PeerIdentity.ipAddrToIdentity(addr, port);
      synchronized (theIdentities) {
	if (!theIdentities.containsKey(ret)) {
	  theIdentities.put(ret, new LcapIdentity(addr, port));
	}
      }
    }
    return ret;
  }

  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr) {
    return ipAddrToPeerIdentity(addr, 0);
  }

  /**
   * stringToPeerIdentity returns the peer identity matching the
   * String ip address and port.  An instance is created if necesary.
   * Used only by LcapMessage (and soon by its stream analog).
   * @param idKey the ip addr and port of the peer, null for the local peer
   * @return the PeerIdentity representing the peer
   */
  public PeerIdentity stringToPeerIdentity(String idKey)
    throws UnknownHostException {
    PeerIdentity ret = null;
    if (idKey == null) {
      ret = theLocalPeerIdentity;
    } else {
      ret = PeerIdentity.stringToIdentity(idKey);
      synchronized (theIdentities) {
	if (!theIdentities.containsKey(ret)) {
	  theIdentities.put(ret, new LcapIdentity(idKey));
	}
      }
    }
    return ret;
  }


  public IPAddr identityToIPAddr(PeerIdentity pid)
    throws UnknownHostException {
    LcapIdentity lid = (LcapIdentity)theIdentities.get(pid);
    if (lid == null) {
      log.error(pid.toString() + " has no LcapIdentity");
    } else if (lid.getPort() != 0) {
      log.error(pid.toString() + " is not a V1 identity");
    } else {
      return lid.getAddress();
    }
    throw new UnknownHostException(pid.toString());
  }
 
  /**
   * getLocalPeerIdentity returns the local peer identity
   * @return the local peer identity
   */
  public PeerIdentity getLocalPeerIdentity() {
    return theLocalPeerIdentity;
  }

  /**
   * getLocalIPAddr returns the IPAddr of the local peer
   * @return the IPAddr of the local peer
   */
  public static IPAddr getLocalIPAddr() {
    return theLocalIPAddr;
  }

  /**
   * return true if this PeerIdentity is the same as the local host
   * @param id the PeerIdentity
   * @return boolean true if is the local identity, false otherwise
   */
  public boolean isLocalIdentity(PeerIdentity id) {
    return (theLocalPeerIdentity == id);
  }

  /**
   * return true if this PeerIdentity is the same as the local host
   * @param id a String representing the PeerIdentity
   * @return boolean true if is the local identity, false otherwise
   */
  public boolean isLocalIdentity(String idStr) {
    boolean ret = false;
    try {
      ret = isLocalIdentity(stringToPeerIdentity(idStr));
    } catch (UnknownHostException uhe) {
      // No action intended
    }
    return ret;
  }

  /**
   * rememberEvent associates the event with the peer identity
   * @param id the PeerIdentity
   * @param event the event code
   * @param msg the LcapMessage involved
   */
  public void rememberEvent(PeerIdentity id, int event, LcapMessage msg) {
    LcapIdentity lid = (LcapIdentity)theIdentities.get(id);
    if (lid != null)
      lid.rememberEvent(event, msg);
  }
  /**
   * return the max value of an Identity's reputation
   * @return the int value of max reputation
   */
  public int getMaxReputation() {
    return REPUTATION_NUMERATOR;
  }

  /**
   * getReputation returns the reputation of the peer
   * @param id the PeerIdentity
   * @return the reputation
   */
  public int getReputation(PeerIdentity id) {
    int ret = 0;
    LcapIdentity lid = (LcapIdentity)theIdentities.get(id);
    if (lid == null) {
      log.error("Can't find LcapIdentity for " + id.toString());
    } else {
      ret = lid.getReputation();
    }
    return ret;
  }

  /**
   * getReputationDelta returns the amount of reputation change that
   * reflects the specified kind of event.
   * @param changeKind the type of event
   * @return the delta that would be applied to a peer's reputation
   */
  protected int getReputationDelta(int changeKind) {
    int ret = -1;
    if (changeKind >= 0 && changeKind < reputationDeltas.length)
      ret = reputationDeltas[changeKind];
    return ret;
  }
    
  /**
   * changeReputation makes the change to the reputation of the peer "id"
   * matching the event "changeKind"
   * @param id the PeerIdentity of the peer to affect
   * @param changeKind the type of event that is being reflected
   */
  public void changeReputation(PeerIdentity id, int changeKind) {
    int delta = getReputationDelta(changeKind);
    int max_delta = reputationDeltas[MAX_DELTA];
    LcapIdentity lid = (LcapIdentity)theIdentities.get(id);
    if (lid == null) {
      log.error("Can't find LcapIdentity for " + id.toString());
      return;
    }
    int reputation = lid.getReputation();
    
    if (id == theLocalPeerIdentity) {
      log.debug(id.toString() + " ignoring reputation delta " + delta);
      return;
    }

    delta = (int) (((float) delta) * theRandom.nextFloat());

    if (delta > 0) {
      if (delta > max_delta) {
	delta = max_delta;
      }
      if (delta > (REPUTATION_NUMERATOR - reputation)) {
	delta = (REPUTATION_NUMERATOR - reputation);
      }
    } else if (delta < 0) {
      if (delta < (-max_delta)) {
	delta = -max_delta;
      }
      if ((reputation + delta) < 0) {
	delta = -reputation;
      }
    }
    if (delta != 0)
      log.debug(id.toString() +" change reputation from " + reputation +
		" to " + (reputation + delta));
    lid.changeReputation(delta);
  }


  private void reloadIdentities() {
    try {
      String iddbDir = Configuration.getParam(PARAM_IDDB_DIR);
      if (iddbDir==null) {
        log.warning("No value found for config parameter '" +
		    PARAM_IDDB_DIR+"'");
        return;
      }
      String fn = iddbDir + File.separator + IDDB_FILENAME;

      // load the identity list via the marshaller
      XmlMarshaller marshaller = new XmlMarshaller();
      IdentityListBean idlb = (IdentityListBean)marshaller.load(fn,
          IdentityListBean.class, MAPPING_FILE_NAME);
      if (idlb==null) {
        log.warning("Unable to read Identity file:" + fn);
      } else {
        setIdentities(idlb.getIdBeans());
      }
      if (!theIdentities.containsKey(theLocalPeerIdentity)) {
	theIdentities.put(theLocalPeerIdentity,
			  new LcapIdentity(localIdentityStr));
      }
    } catch (Exception e) {
      log.warning("Couldn't load identity database: " + e.getMessage());
    }
  }


  /**
   * storeIdentities is used by the PollManager to record the result of
   * tallying a poll.
   */
  public void storeIdentities() throws ProtocolException {
    try {
      String fn = Configuration.getParam(PARAM_IDDB_DIR);
      if (fn==null) {
        log.warning("No value found for config parameter '" +
		    PARAM_IDDB_DIR+"'");
        return;
      }

      // store the identity list via the marshaller
      XmlMarshaller marshaller = new XmlMarshaller();
      marshaller.store(fn, IDDB_FILENAME, getIdentityListBean(),
		       MAPPING_FILE_NAME);

    } catch (Exception e) {
      log.error("Couldn't store identity database: ", e);
      throw new ProtocolException("Unable to store identity database.");
    }
  }


  public IdentityListBean getIdentityListBean() {
    synchronized(theIdentities) {
      List beanList = new ArrayList(theIdentities.size());
      Iterator mapIter = theIdentities.values().iterator();
      while(mapIter.hasNext()) {
        LcapIdentity id = (LcapIdentity) mapIter.next();
        IdentityBean bean = new IdentityBean(id.getIdKey(),id.getReputation());
        beanList.add(bean);
      }
      IdentityListBean listBean = new IdentityListBean(beanList);
      return listBean;
    }
  }

  private void setIdentities(Collection idList) {
    Iterator beanIter = idList.iterator();
    synchronized(theIdentities) {
      while (beanIter.hasNext()) {
        IdentityBean bean = (IdentityBean)beanIter.next();
        String idKey = bean.getKey();
        try {
	  PeerIdentity pid = PeerIdentity.stringToIdentity(idKey);
          LcapIdentity id = new LcapIdentity(idKey, bean.getReputation());
          theIdentities.put(pid, id);
        }
        catch (UnknownHostException ex) {
          log.warning("Error reloading identity-Unknown Host: " + idKey);
        }
      }
    }
  }



  /**
   * Signals that we've agreed with id on a top level poll on au.
   * Only called if we're both on the winning side
   * @param pid the PeerIdentity of the agreeing peer
   * @param au the {@link ArchivalUnit}
   */
  public void signalAgreed(PeerIdentity pid, ArchivalUnit au) {
    signalAgreed(pid, au, TimeBase.nowMs());
  }

  private void signalAgreed(PeerIdentity pid, ArchivalUnit au, long time) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (pid == null) {
      throw new IllegalArgumentException("Called with null pid");
    }
    synchronized (identityMapLock) { //using as the lock for all 3 maps
      ensureIdentityMapsLoaded(au);
      Map map = (Map)agreeMap.get(au);
      if (map == null) {
	map = new HashMap();
	agreeMap.put(au, map);
      }
      map.put(pid, new Long(time));

      map = (Map)cachesToFetchFrom.get(au);
      if (map == null) {
	map = new HashMap();
	cachesToFetchFrom.put(au, map);
      }
      map.put(pid, new Long(time));
      storeIdentityAgreement(au);
    }
  }

  /**
   * Signals that we've disagreed with id on any level poll on au.
   * Only called if we're on the winning side
   * @param pid the PeerIdentity of the disagreeing peer
   * @param au the {@link ArchivalUnit}
   */
  public void signalDisagreed(PeerIdentity pid, ArchivalUnit au) {
    signalDisagreed(pid, au, TimeBase.nowMs());
  }

  private void signalDisagreed(PeerIdentity pid, ArchivalUnit au, long time) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (pid == null) {
      throw new IllegalArgumentException("Called with null pid");
    }
    synchronized (identityMapLock) { //using as the lock for all 3 maps
      ensureIdentityMapsLoaded(au);
      Map map = (Map)disagreeMap.get(au);
      if (map == null) {
	map = new HashMap();
	disagreeMap.put(au, map);
      }
      map.put(pid, new Long(time));

      map = (Map)cachesToFetchFrom.get(au);
      if (map == null) {
 	return;
      }
      map.remove(pid);
      if (map.size() == 0) {
 	cachesToFetchFrom.remove(au);
      }
      storeIdentityAgreement(au);
    }
  }

  /**
   * @param au ArchivalUnit to look up PeerIdentities for
   * @return a map of PeerIdentity -> last agreed time.
   */
  public Map getCachesToRepairFrom(ArchivalUnit au) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    synchronized (identityMapLock) { //using as the lock for all 3 maps
      ensureIdentityMapsLoaded(au);
      return (Map)cachesToFetchFrom.get(au);
    }
  }

  public Map getAgreed(ArchivalUnit au) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    synchronized (identityMapLock) { //using as the lock for all 3 maps
      ensureIdentityMapsLoaded(au);
      return (Map)agreeMap.get(au);
    }
  }

  private void ensureIdentityMapsLoaded(ArchivalUnit au) {
    if (agreeMap == null || disagreeMap == null || cachesToFetchFrom == null) {
      agreeMap = new HashMap();
      disagreeMap = new HashMap();
      cachesToFetchFrom = new HashMap();
      loadIdentityAgreement(au);
    }
  }

  private void loadIdentityAgreement(ArchivalUnit au) {
    HistoryRepository hRep = getDaemon().getHistoryRepository(au);
    List list = hRep.loadIdentityAgreements();
    if (list != null) {
      Iterator it = list.iterator();
      while (it.hasNext()) {
	IdentityAgreement ida = (IdentityAgreement)it.next();
	if (ida.getLastAgree() > 0) {
	  signalAgreed(ida.getPeerIdentity(), au, ida.getLastAgree());
	}
	if (ida.getLastDisagree() > 0) {
	  signalDisagreed(ida.getPeerIdentity(), au, ida.getLastDisagree());
	}
      }
    }
  }

  //only called within a synchronized block, so we don't need to
  private void storeIdentityAgreement(ArchivalUnit au) {
    HistoryRepository hRep = getDaemon().getHistoryRepository(au);
    hRep.storeIdentityAgreements(generateIdentityAgreementList(au));
  }


  //only called within a synchronized block, so we don't need to
  private List generateIdentityAgreementList(ArchivalUnit au) {
    List list = new ArrayList();
    Map map = new HashMap();

    Map agreeMapForAu = (Map)agreeMap.get(au);
    if (agreeMapForAu != null && agreeMapForAu.size() > 0) {
      Iterator it = agreeMapForAu.keySet().iterator();
      while (it.hasNext()) {
	PeerIdentity pid = (PeerIdentity)it.next();
	Long time = (Long)agreeMapForAu.get(pid);
	IdentityAgreement ida = new IdentityAgreement(pid);
	ida.setLastAgree(time.longValue());

	list.add(ida);
	map.put(pid, ida);
      }
    }

    Map disagreeMapForAu = (Map)disagreeMap.get(au);
    if (disagreeMapForAu != null && disagreeMapForAu.size() > 0) {
      Iterator it = disagreeMapForAu.keySet().iterator();
      while (it.hasNext()) {
	PeerIdentity pid = (PeerIdentity)it.next();
	Long time = (Long)disagreeMapForAu.get(pid);
	IdentityAgreement ida = (IdentityAgreement)map.get(pid);
	if (ida == null) { //wasn't set in the previous loop
	  ida = new IdentityAgreement(pid);
	  list.add(ida);
	  map.put(pid, ida);
	}
	ida.setLastDisagree(time.longValue());
      }
    }
    return list;
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    reputationDeltas[MAX_DELTA] =
        config.getInt(PARAM_MAX_DELTA, DEFAULT_MAX_DELTA);
    reputationDeltas[AGREE_VOTE] =
        config.getInt(PARAM_AGREE_DELTA, DEFAULT_AGREE_DELTA);
    reputationDeltas[DISAGREE_VOTE] =
        config.getInt(PARAM_DISAGREE_DELTA, DEFAULT_DISAGREE_DELTA);
    reputationDeltas[CALL_INTERNAL] =
        config.getInt(PARAM_CALL_INTERNAL, DEFAULT_CALL_INTERNAL);
    reputationDeltas[SPOOF_DETECTED] =
        config.getInt(PARAM_SPOOF_DETECTED, DEFAULT_SPOOF_DETECTED);
    reputationDeltas[REPLAY_DETECTED] =
        config.getInt(PARAM_REPLAY_DETECTED, DEFAULT_REPLAY_DETECTED);
    reputationDeltas[ATTACK_DETECTED] =
        config.getInt(PARAM_ATTACK_DETECTED, DEFAULT_ATTACK_DETECTED);
    reputationDeltas[VOTE_NOTVERIFIED] =
        config.getInt(PARAM_VOTE_NOTVERIFIED, DEFAULT_VOTE_NOTVERIFIED);
    reputationDeltas[VOTE_VERIFIED] =
        config.getInt(PARAM_VOTE_VERIFIED, DEFAULT_VOTE_VERIFIED);
    reputationDeltas[VOTE_DISOWNED] =
        config.getInt(PARAM_VOTE_DISOWNED, DEFAULT_VOTE_DISOWNED);
  }

  private static final List statusSortRules =
    ListUtil.list(new StatusTable.SortRule("ip", true));

  private static final List statusColDescs =
    ListUtil.list(
		  new ColumnDescriptor("ip", "IP",
				       ColumnDescriptor.TYPE_STRING),
		  new ColumnDescriptor("lastPkt", "Last Pkt",
				       ColumnDescriptor.TYPE_DATE,
				       "Last time a packet that originated " +
				       "at IP was received"),
		  new ColumnDescriptor("lastOp", "Last Op",
				       ColumnDescriptor.TYPE_DATE,
				       "Last time a non-NoOp packet that " +
				       "originated at IP was received"),
		  new ColumnDescriptor("origTot", "Orig Tot",
				       ColumnDescriptor.TYPE_INT,
				       "Total packets received that " +
				       "originated at IP."),
		  new ColumnDescriptor("origOp", "Orig Op",
				       ColumnDescriptor.TYPE_INT,
				       "Total non-noop packets received that "+
				       "originated at IP."),
		  new ColumnDescriptor("sendOrig", "1 Hop",
				       ColumnDescriptor.TYPE_INT,
				       "Packets arriving from originator " +
				       "in one hop."),
		  new ColumnDescriptor("sendFwd", "Fwd",
				       ColumnDescriptor.TYPE_INT,
				       "Packets forwarded by IP to us."),
		  new ColumnDescriptor("dup", "Dup",
				       ColumnDescriptor.TYPE_INT,
				       "Duplicate packets received from IP."),
		  new ColumnDescriptor("reputation", "Reputation",
				       ColumnDescriptor.TYPE_INT)
		  );

  public static class IdentityAgreement {
    private long lastAgree = 0;
    private long lastDisagree = 0;
    private String id = null;
    private static IdentityManager idMgr = null;

    public IdentityAgreement(PeerIdentity pid) {
      this.id = pid.getIdString();
    }

    // needed for marshalling
    public IdentityAgreement() {}

    private static void setIdentityManager(IdentityManager idm) {
      idMgr = idm;
    }

    public long getLastAgree() {
      return lastAgree;
    }

    public void setLastAgree(long lastAgree) {
      this.lastAgree = lastAgree;
    }

    public long getLastDisagree() {
      return lastDisagree;
    }

    public void setLastDisagree(long lastDisagree) {
      this.lastDisagree = lastDisagree;
    }

    public String getId() {
      return id;
    }

    public PeerIdentity getPeerIdentity() {
      PeerIdentity ret = null;
      try {
	ret = idMgr.stringToPeerIdentity(id);
      } catch (UnknownHostException uhe) {
	// No action intended
      }
      return ret;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[IdentityAgreement: ");
      sb.append("id=");
      sb.append(id);
      sb.append(", ");
      sb.append("lastAgree=");
      sb.append(lastAgree);
      sb.append(", ");
      sb.append("lastDisagree=");
      sb.append(lastDisagree);
      sb.append("]");
      return sb.toString();
    }

    public boolean equals(Object obj) {
      if (obj instanceof IdentityAgreement) {
        IdentityAgreement ida = (IdentityAgreement)obj;
        return (id.equals(ida.getId())
            && ida.getLastDisagree() == lastDisagree
            && ida.getLastAgree() == lastAgree);
      }
      return false;
    }
  }

  // for marshalling purposes, this class has to exist
  public static class IdentityAgreementList {
    private List idAgreeList;
    public IdentityAgreementList() { }
    public IdentityAgreementList(List list) {
      idAgreeList = list;
    }

    public List getList() {
      return idAgreeList;
    }

    public void setList(List list) {
      idAgreeList = list;
    }
  }

  private class Status implements StatusAccessor {

    public String getDisplayName() {
      return "Cache Identities";
    }

    public void populateTable(StatusTable table) {
      String key = table.getKey();
      table.setColumnDescriptors(statusColDescs);
      table.setDefaultSortRules(statusSortRules);
      table.setRows(getRows(key));
//       table.setSummaryInfo(getSummaryInfo(key));
    }

    public boolean requiresKey() {
      return false;
    }

    private List getRows(String key) {
      List table = new ArrayList();
      for (Iterator iter = theIdentities.values().iterator();
	   iter.hasNext();) {
	table.add(makeRow((LcapIdentity)iter.next()));
      }
      return table;
    }

    private Map makeRow(LcapIdentity id) {
      Map row = new HashMap();
      String idKey = id.getIdKey();
      PeerIdentity pid = PeerIdentity.stringToIdentity(idKey);
      if (isLocalIdentity(pid)) {
	StatusTable.DisplayedValue val =
	  new StatusTable.DisplayedValue(pid.toString());
	val.setBold(true);
	row.put("ip", val);
      } else {
	row.put("ip", pid.toString());
      }
      row.put("lastPkt", new Long(id.getLastActiveTime()));
      row.put("lastOp", new Long(id.getLastOpTime()));
      row.put("origTot", new Long(id.getEventCount(LcapIdentity.EVENT_ORIG)));
      row.put("origOp",
	      new Long(id.getEventCount(LcapIdentity.EVENT_ORIG_OP)));
      row.put("sendOrig",
	      new Long(id.getEventCount(LcapIdentity.EVENT_SEND_ORIG)));
      row.put("sendFwd",
	      new Long(id.getEventCount(LcapIdentity.EVENT_SEND_FWD)));
      row.put("dup", new Long(id.getEventCount(LcapIdentity.EVENT_DUPLICATE)));
      row.put("reputation", new Long(id.getReputation()));
      return row;
    }

    private List getSummaryInfo(String key) {
      List res = new ArrayList();
//       res.add(new StatusTable.SummaryInfo("Total bytes hashed",
// 					  ColumnDescriptor.TYPE_INT,
// 					  new Integer(0)));
      return res;
    }
  }
}
