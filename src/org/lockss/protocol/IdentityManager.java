/*
* $Id: IdentityManager.java,v 1.44 2004-09-16 21:29:16 dshr Exp $
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

/**
 * Abstraction for identity of a LOCKSS cache.  Currently wraps an IP address.
 * @author Claire Griffin
 * @version 1.0
 */
public class IdentityManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  static Logger log = Logger.getLogger("IdentityManager");

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

  static Random theRandom = new Random();

  protected static String localIdentityStr;
  LcapIdentity theLocalIdentity;

  int[] reputationDeltas = new int[10];

  private Map agreeMap = null;
  private Map disagreeMap = null;

  //derivable from the above two; included for speed
  private Map cachesToFetchFrom = null;

  private String identityMapLock = "lock";

  HashMap theIdentities = new HashMap(); // all known identities

  public IdentityManager() { }

  public void initService(LockssDaemon daemon) {
    super.initService(daemon);
    localIdentityStr = Configuration.getParam(PARAM_LOCAL_IP);
    log.debug("localIdentityStr: " + localIdentityStr);
    if (localIdentityStr == null) {
      log.error(PARAM_LOCAL_IP +
		" is not set - IdentityManager cannot start.");
      throw new
	LockssAppException(PARAM_LOCAL_IP +
			      " is not set - IdentityManager cannot start.");
    }
    makeLocalIdentity();
  }

  protected void makeLocalIdentity() throws LockssAppException {
    try {
      IPAddr addr = IPAddr.getByName(localIdentityStr);
      // XXX the local identity for now is V1
      theLocalIdentity = new LcapIdentity(addr, 0);
    } catch (UnknownHostException uhe) {
      log.error("Could not resolve: " + localIdentityStr, uhe);
      throw new
	LockssAppException("Could not resolve: " + localIdentityStr);
    }
  }

  /**
   * start the identity manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    reloadIdentities();
    log.info("Local identity: " + getLocalIdentity());
    getDaemon().getStatusService().registerStatusAccessor("Identities",
							  new Status());
  }

  /**
   * stop the plugin manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    try {
      storeIdentities();
    }
    catch (ProtocolException ex) {
    }
    super.stopService();
  }

  /**
   * public constructor for the creation of an Identity object
   * from an address.
   * @param addr the IPAddr
   * @return a pre-existing or newly constructed Identity
   */
  public LcapIdentity findIdentity(IPAddr addr, int port) {
    LcapIdentity ret;

    if(addr == null)  {
      ret = getLocalIdentity();
    }
    else  {
      String idKey = LcapIdentity.makeIdKey(addr, port);
      ret = getIdentity(idKey);
      if(ret == null)  try {
        ret = new LcapIdentity(idKey, INITIAL_REPUTATION);
        theIdentities.put(idKey, ret);
      } catch (UnknownHostException uhe) {
	log.warning("Can't create identity for " + idKey);
      }
    }

    return ret;
  }

  /**
   * public constructor for the creation of an Identity object
   * from an ID key.
   * @param idKey the ID key
   * @return a pre-existing or newly constructed Identity
   */
  public LcapIdentity findIdentity(String idKey) {
    LcapIdentity ret;

    if(idKey == null)  {
      ret = getLocalIdentity();
    }
    else  {
      ret = getIdentity(idKey);
      if(ret == null)  try {
        ret = new LcapIdentity(idKey, INITIAL_REPUTATION);
        theIdentities.put(idKey, ret);
      } catch (UnknownHostException uhe) {
	log.warning("Can't create identity for " + idKey);
      }
    }

    return ret;
  }


  /**
   * get and return an already created identity
   * @param idKey the key for the identity we want to find
   * @return the LcapIdentity or null
   */
  public LcapIdentity getIdentity(Object idKey)  {
    return (LcapIdentity)theIdentities.get(idKey);
  }

  /**
   * Get the Identity of the local host
   * @return LcapIdentity for the local host
   */
  public LcapIdentity getLocalIdentity() {
    return theLocalIdentity;
  }

  /**
   * Get the local host name
   * @return hostname as a String
   */
  public static String getLocalHostName() {
    return localIdentityStr;
  }

  /**
   * return true if this Identity is the same as the local host
   * @param id the LcapIdentity
   * @return boolean true if is the local identity, false otherwise
   */
  public boolean isLocalIdentity(LcapIdentity id) {
    return theLocalIdentity.isEqual(id);
  }

  /**
   * return true if this Identity is the same as the local host
   * @param id the ID string
   * @return boolean true if this is the local identity, false otherwise
   */
  public boolean isLocalIdentity(String id) {
    return (localIdentityStr.equals(id));
  }

  /**
   * returns true if the IPAddr is the same as the IPAddr for our
   * local host
   * @param addr the address to check
   * @return boolean true if the this is IPAddr is considered local
   */
  public boolean isLocalIdentity(IPAddr addr) {
    return theLocalIdentity.m_address.equals(addr);
  }

  /**
   * return the max value of an Identity's reputation
   * @return the int value of max reputation
   */
  public int getMaxReputation() {
    return REPUTATION_NUMERATOR;
  }

  public void changeReputation(String idKey, int changeKind) {
    LcapIdentity id = findIdentity(idKey);
    changeReputation(id, changeKind);
  }

  public void changeReputation(LcapIdentity id, int changeKind) {
    int delta = reputationDeltas[changeKind];
    int max_delta = reputationDeltas[MAX_DELTA];
    int reputation = id.getReputation();
    
    if (id == theLocalIdentity) {
      log.debug(id.getIdKey() + " ignoring reputation delta " + delta);
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
      log.debug(id.getIdKey() +" change reputation from " + reputation +
		" to " + (reputation + delta));
    id.changeReputation(delta);
  }


  void reloadIdentities() {
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
    } catch (Exception e) {
      log.warning("Couldn't load identity database: " + e.getMessage());
    }
  }

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


  IdentityListBean getIdentityListBean() {
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

  void setIdentities(Collection idList) {
    Iterator beanIter = idList.iterator();
    synchronized(theIdentities) {
      while (beanIter.hasNext()) {
        IdentityBean bean = (IdentityBean)beanIter.next();
        String idKey = bean.getKey();
        try {
          LcapIdentity id = new LcapIdentity(idKey, bean.getReputation());
          theIdentities.put(idKey, id);
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
   * @param cacheAddr the address
   * @param au the {@link ArchivalUnit}
   */
  public void signalAgreed(String cacheAddr, ArchivalUnit au) {
    signalAgreed(cacheAddr, au, TimeBase.nowMs());
  }

  private void signalAgreed(String cacheAddr, ArchivalUnit au, long time) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (cacheAddr == null) {
      throw new IllegalArgumentException("Called with null cacheAddr");
    }
    synchronized (identityMapLock) { //using as the lock for all 3 maps
      ensureIdentityMapsLoaded(au);
      Map map = (Map)agreeMap.get(au);
      if (map == null) {
	map = new HashMap();
	agreeMap.put(au, map);
      }
      map.put(cacheAddr, new Long(time));

      map = (Map)cachesToFetchFrom.get(au);
      if (map == null) {
	map = new HashMap();
	cachesToFetchFrom.put(au, map);
      }
      map.put(cacheAddr, new Long(time));
      storeIdentityAgreement(au);
    }
  }

  /**
   * Signals that we've disagreed with id on any level poll on au.
   * Only called if we're on the winning side
   * @param cacheAddr the address
   * @param au the {@link ArchivalUnit}
   */
  public void signalDisagreed(String cacheAddr, ArchivalUnit au) {
    signalDisagreed(cacheAddr, au, TimeBase.nowMs());
  }

  private void signalDisagreed(String cacheAddr, ArchivalUnit au, long time) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (cacheAddr == null) {
      throw new IllegalArgumentException("Called with null cacheAddr");
    }
    synchronized (identityMapLock) { //using as the lock for all 3 maps
      ensureIdentityMapsLoaded(au);
      Map map = (Map)disagreeMap.get(au);
      if (map == null) {
	map = new HashMap();
	disagreeMap.put(au, map);
      }
      map.put(cacheAddr, new Long(time));

      map = (Map)cachesToFetchFrom.get(au);
      if (map == null) {
 	return;
      }
      map.remove(cacheAddr);
      if (map.size() == 0) {
 	cachesToFetchFrom.remove(au);
      }
      storeIdentityAgreement(au);
    }
  }

  /**
   * @param au ArchivalUnit to look up LcapIdentities for
   * @return a map of LcapIdentity -> last agreed time.
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
	  signalAgreed(ida.getId(), au, ida.getLastAgree());
	}
	if (ida.getLastDisagree() > 0) {
	  signalDisagreed(ida.getId(), au, ida.getLastDisagree());
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
	String id = (String)it.next();
	Long time = (Long)agreeMapForAu.get(id);
	IdentityAgreement ida = new IdentityAgreement(id);
	ida.setLastAgree(time.longValue());

	list.add(ida);
	map.put(id, ida);
      }
    }

    Map disagreeMapForAu = (Map)disagreeMap.get(au);
    if (disagreeMapForAu != null && disagreeMapForAu.size() > 0) {
      Iterator it = disagreeMapForAu.keySet().iterator();
      while (it.hasNext()) {
	String id = (String)it.next();
	Long time = (Long)disagreeMapForAu.get(id);
	IdentityAgreement ida = (IdentityAgreement)map.get(id);
	if (ida == null) { //wasn't set in the previous loop
	  ida = new IdentityAgreement(id);
	  list.add(ida);
	  map.put(id, ida);
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

    public IdentityAgreement(String id) {
      this.id = id;
    }

    // needed for marshalling
    public IdentityAgreement() {}

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
      if (isLocalIdentity(idKey)) {
	StatusTable.DisplayedValue val =
	  new StatusTable.DisplayedValue(idKey);
	val.setBold(true);
	row.put("ip", val);
      } else {
	row.put("ip", idKey);
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
