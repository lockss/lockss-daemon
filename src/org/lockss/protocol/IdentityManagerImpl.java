/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import java.net.UnknownHostException;
import java.util.*;

import org.apache.commons.collections.*;

import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.poller.*;
import org.lockss.protocol.IdentityManager.MalformedIdentityKeyException;
import org.lockss.repository.*;
import org.lockss.state.HistoryRepository;
import org.lockss.state.AuState;
import org.lockss.plugin.AuUtil;
import org.lockss.util.*;
import org.lockss.util.SerializationException.FileNotFound;
import org.lockss.hasher.*;

/**
 * <p>Abstraction for identity of a LOCKSS cache. Currently wraps an
 * IP address.<p>
 * @author Claire Griffin
 * @version 1.0
 */
public class IdentityManagerImpl extends BaseLockssDaemonManager
  implements IdentityManager, ConfigurableManager {

  /**
   * <p>A logger for this class.</p>
   */
  protected static Logger log = Logger.getLogger("IdentityManagerImpl");

  /**
   * <p>The MAX_DELTA parameter.</p>
   */
  static final String PARAM_MAX_DELTA = PREFIX + "maxReputationDelta";

  /**
   * <p>The default value for the MAX_DELTA parameter.</p>
   */
  static final int DEFAULT_MAX_DELTA = 100;

  /**
   * <p>The AGREE_DELTA parameter.</p>
   */
  static final String PARAM_AGREE_DELTA = PREFIX + "agreeDelta";

  /**
   * <p>The default value for the AGREE_DELTA parameter.</p>
   */
  static final int DEFAULT_AGREE_DELTA = 100;

  /**
   * <p>The DISAGREE_DELTA parameter.</p>
   */
  static final String PARAM_DISAGREE_DELTA = PREFIX + "disagreeDelta";

  /**
   * <p>The default value for the DISAGREE_DELTA parameter.</p>
   */
  static final int DEFAULT_DISAGREE_DELTA = -150;

  /**
   * <p>The CALL_INTERNAL parameter.</p>
   */
  static final String PARAM_CALL_INTERNAL = PREFIX + "callInternalDelta";

  /**
   * <p>The default value for the CALL_INTERNAL parameter.</p>
   */
  static final int DEFAULT_CALL_INTERNAL = 100;

  /**
   * <p>The SPOOF_DETECTED parameter.</p>
   */
  static final String PARAM_SPOOF_DETECTED = PREFIX + "spoofDetected"; //no

  /**
   * <p>The default value for the SPOOF_DETECTED parameter.</p>
   */
  static final int DEFAULT_SPOOF_DETECTED = -30;

  /**
   * <p>The REPLAY_DETECTED parameter.</p>
   */
  static final String PARAM_REPLAY_DETECTED = PREFIX + "replayDetected";

  /**
   * <p>The default value for the REPLAY_DETECTED parameter.</p>
   */
  static final int DEFAULT_REPLAY_DETECTED = -20;

  /**
   * <p>The ATTACK_DETECTED parameter.</p>
   */
  static final String PARAM_ATTACK_DETECTED = PREFIX + "attackDetected"; //no

  /**
   * <p>The default value for the ATTACK_DETECTED parameter.</p>
   */
  static final int DEFAULT_ATTACK_DETECTED = -500;

  /**
   * <p>The VOTE_NOTVERIFIED parameter.</p>
   */
  static final String PARAM_VOTE_NOTVERIFIED = PREFIX + "voteNotVerified ";

  /**
   * <p>The default value for the VOTE_NOTVERIFIED parameter.</p>
   */
  static final int DEFAULT_VOTE_NOTVERIFIED = -30;

  /**
   * <p>The VOTE_VERIFIED parameter.</p>
   */
  static final String PARAM_VOTE_VERIFIED = PREFIX + "voteVerified";

  /**
   * <p>The default value for the VOTE_VERIFIED parameter.</p>
   */
  static final int DEFAULT_VOTE_VERIFIED = 40;

  /**
   * <p>The VOTE_DISOWNED parameter.</p>
   */
  static final String PARAM_VOTE_DISOWNED = PREFIX + "voteDisowned";

  /**
   * <p>The default value for the VOTE_DISOWNED parameter.</p>
   */
  static final int DEFAULT_VOTE_DISOWNED = -400;

  /**
   * <p>The initial reputation numerator.</p>
   */
  static final int REPUTATION_NUMERATOR = 1000;
  
  /**
   * <p>The number of update events between IDDB serializations.
   * This parameter is a maximum, and does not alter the fact that
   * the IDDB table is serialized at the end of every poll, and
   * whenever a peer is deleted (for example, due to polling group
   * mismatch).</p>
   */
  public static final String PARAM_UPDATES_BEFORE_STORING =
    PREFIX + "updatesBeforeStoring";
  public static final long DEFAULT_UPDATES_BEFORE_STORING = 100;

  /**
   * <p>The initial list of V3 peers for this cache.</p>
   */
  public static final String PARAM_INITIAL_PEERS = PREFIX + "initialV3PeerList";
  public static final List DEFAULT_INITIAL_PEERS = Collections.EMPTY_LIST;

  /**
   * True to enable V1 identities
   */
  public static final String PARAM_ENABLE_V1 = PREFIX + "v1Enabled";
  public static final boolean DEFAULT_ENABLE_V1 = true;

  /** Maps PeerId to UI URL stem.  Useful for testing frameworks to point
   * nonstandard ports.  List of PeerId,URL-stem;,...*/
  public static final String PARAM_UI_STEM_MAP =
    PREFIX + "pidUiStemMap";

  /** Maps PeerId to PeerAddress.  Useful to allow a node behind NAT to
   * reach others nodes behind the same NAT using the internal address.
   * List of PeerId,Peer;,...  Daemon restart required to remove mappings. */
  public static final String PARAM_PEER_ADDRESS_MAP = PREFIX + "peerAddressMap";

  /**
   * The max size of the LRU cache from AuId to agreement map.
   */
  public static final String PARAM_AGREE_MAPS_CACHE_MAX
    = PREFIX + "agreeMapsCache.max";
  public static final int DEFAULT_AGREE_MAPS_CACHE_MAX = 50;

  /**
   * <p>An instance of {@link LockssRandom} for use by this class.</p>
   */
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

  /**
   * <p>IP address of our identity (not necessarily this machine's IP
   * if behind NAT).<p>
   * <p>All current identities are IP-based; future ones may not
   * be.</p>
   */
  protected IPAddr theLocalIPAddr = null;

  /**
   * <p>Array of PeerIdentity for each of our local identities,
   * (potentially) one per protocol version.
   */
  protected PeerIdentity localPeerIdentities[];

  /**
   * <p>A mapping of PeerIdentity objects to status objects.  This is
   * the map that is actually serialized onto disk for persistence</p>
   */
  protected Map<PeerIdentity,PeerIdentityStatus> theLcapIdentities;

  /**
   * Maps PeerIdentity key (<i>eg</i>, <tt>TCP:[192.168.0.1]:9729</tt>) to
   * unique PeerIdentity object.  Multiple unnormalized keys, in addition
   * to the single normalized key, may map to the same PeerIdentity.
   */
  private Map<String,PeerIdentity> pidMap;

  /**
   * Set of all PeerIdentity objects.  Separate set required when
   * enumerating pids as map may contain mappings from unnormalized keys.
   */
  // Currently duplicates theLcapIdentities.keySet(), but that will be
  // going away.
  private Set<PeerIdentity> pidSet;

  /**
   * <p>The IDDB file.</p>
   */
  File iddbFile = null;

  int[] reputationDeltas = new int[10];

  /**
   * <p>Maps ArchivalUnit, by auid, to its agreement map.  Each
   * agreement map in turn maps a PeerIdentity to its corresponding
   * IdentityAgreement object. The LRU cache allows IdentityAgreement
   * objects to be collected when they are no longer in use.</p>
   */
  private final UniqueRefLruCache agreeMapsCache =
    new UniqueRefLruCache(DEFAULT_AGREE_MAPS_CACHE_MAX);

  private float minPercentPartialAgreement = DEFAULT_MIN_PERCENT_AGREEMENT;

  private long updatesBeforeStoring = DEFAULT_UPDATES_BEFORE_STORING;
  private long updates = 0;

  private IdentityManagerStatus status;

  private Map<PeerIdentity,String> pidUiStemMap;

  /**
   * <p>Builds a new IdentityManager instance.</p>
   */
  public IdentityManagerImpl() {
    theLcapIdentities = new HashMap();
    pidMap = new HashMap();
    pidSet = new HashSet();
  }

  public void initService(LockssDaemon daemon) throws LockssAppException {
    // Set up local identities *before* processing rest of config.  (Else
    // any reference to to our ID string will create a non-local identity,
    // which will later be replaced by the local identity
    setupLocalIdentities();
    super.initService(daemon);
  }

  /**
   * <p>Sets up the local identities.</p>
   * <p>This is protected only so it can be overridden in a mock
   * subclass in another package (TestRemoteApi), which won't be
   * necessary when there's an interface for the mock class to
   * implement instead.</p>
   */
  protected void setupLocalIdentities() {
    localPeerIdentities = new PeerIdentity[Poll.MAX_PROTOCOL + 1];
    boolean hasLocalIdentity = false;

    // setConfig() has not yet run.  All references to the config must be
    // explicit.
    Configuration config = ConfigManager.getCurrentConfig();

    String localV1IdentityStr = getLocalIpParam(config);
    if (config.getBoolean(PARAM_ENABLE_V1, DEFAULT_ENABLE_V1)) {
      // Find local IP addr and create V1 identity if configured
      if (localV1IdentityStr != null) {
	try {
	  theLocalIPAddr = IPAddr.getByName(localV1IdentityStr);
	} catch (UnknownHostException uhe) {
	  String msg = "Cannot start: Can't lookup \"" + localV1IdentityStr + "\"";
	  log.critical(msg);
	  throw new LockssAppException("IdentityManager: " + msg);
	}
	try {
	  localPeerIdentities[Poll.V1_PROTOCOL] =
	    findLocalPeerIdentity(localV1IdentityStr);
	} catch (MalformedIdentityKeyException e) {
	  String msg = "Cannot start: Can't create local identity:" +
	    localV1IdentityStr;
	  log.critical(msg, e);
	  throw new LockssAppException("IdentityManager: " + msg);
	}

	hasLocalIdentity = true;
      }
    }
    // Create V3 identity if configured
    String v3idstr = config.get(PARAM_LOCAL_V3_IDENTITY);
    if (StringUtil.isNullString(v3idstr) &&
        config.containsKey(PARAM_LOCAL_V3_PORT)) {
      int localV3Port = config.getInt(PARAM_LOCAL_V3_PORT, -1);
      if (localV3Port > 0 && localV1IdentityStr != null) {
        v3idstr = IDUtil.ipAddrToKey(localV1IdentityStr, localV3Port);
      }
    }
    if (v3idstr != null) {
      try {
        localPeerIdentities[Poll.V3_PROTOCOL] = findLocalPeerIdentity(v3idstr);
      } catch (MalformedIdentityKeyException e) {
        String msg = "Cannot start: Cannot create local V3 identity: " +
	  v3idstr;
        log.critical(msg, e);
        throw new LockssAppException("IdentityManager: " + msg);
      }

      hasLocalIdentity = true;
    }
    
    // Make sure we have configured at least one local identity.
    if (!hasLocalIdentity) {
      String msg = "Cannot start: Must configure at least one local V1 or "
                   + "local V3 identity!";
      log.critical(msg);
      throw new LockssAppException("IdentityManager: " + msg);
    }
  }

  /**
   * <p>Starts the identity manager.</p>
   * @see LockssManager#startService()
   */
  public void startService() {
    super.startService();
    
    // Register a message handler with LcapRouter to peek at incoming
    // messages.
    LcapRouter router = getDaemon().getRouterManager();
    router.registerMessageHandler(new LcapRouter.MessageHandler() {
      public void handleMessage(LcapMessage msg) {
        try {
          PeerIdentityStatus status = findPeerIdentityStatus(msg.m_originatorID);
          if (status != null) {
            status.messageReceived(msg);
            if (++updates > updatesBeforeStoring) {
              storeIdentities();
              updates = 0;
            }
          }
        } catch (Exception ex) {
          log.error("Unable to checkpoint iddb file!", ex);
        }
      }

      public String toString() {
        return "[IdentityManager Message Handler]";
      }
    });

    // If requested, delete any old IDDB files.
    if (CurrentConfig.getBooleanParam(PARAM_DELETE_OLD_IDDB_FILES,
                                      DEFAULT_DELETE_OLD_IDDB_FILES)) {
      unlinkOldIddbFiles();
    }

    reloadIdentities();

    if (localPeerIdentities[Poll.V1_PROTOCOL] != null)
      log.info("Local V1 identity: " + getLocalPeerIdentity(Poll.V1_PROTOCOL));
    if (localPeerIdentities[Poll.V3_PROTOCOL] != null)
      log.info("Local V3 identity: " + getLocalPeerIdentity(Poll.V3_PROTOCOL));

    status = makeStatusAccessor();
    getDaemon().getStatusService().registerStatusAccessor("Identities",
							  status);

    Vote.setIdentityManager(this);
    LcapMessage.setIdentityManager(this);
  }

  protected IdentityManagerStatus makeStatusAccessor() {
    return new IdentityManagerStatus(this);
  }

  /**
   * <p>Stops the identity manager.</p>
   * @see LockssManager#stopService()
   */
  public void stopService() {
    try {
      storeIdentities();
    }
    catch (ProtocolException ex) {}
    super.stopService();
    getDaemon().getStatusService().unregisterStatusAccessor("Identities");
    Vote.setIdentityManager(null);
    LcapMessage.setIdentityManager(null);
  }

  public List<PeerIdentityStatus> getPeerIdentityStatusList() {
    synchronized (theLcapIdentities) {
      return new ArrayList(theLcapIdentities.values());
    }
  }

  /**
   * <p>Finds or creates unique instances of both PeerIdentity and
   * PeerIdentityStatus</p>
   * 
   * @param id
   * @return
   * @throws MalformedIdentityKeyException 
   */
  private PeerIdentityStatus findPeerIdentityStatus(PeerIdentity id) {
    PeerIdentityStatus status = null;
    synchronized (theLcapIdentities) {
      status = theLcapIdentities.get(id);
      if (status == null) {
        LcapIdentity lcapId = findLcapIdentity(id, id.getIdString());
        log.debug2("Making new PeerIdentityStatus for: " + id);
        status = new PeerIdentityStatus(lcapId);
      }
    }
    return status;
  }
  
  
  /**
   * @param pid The PeerIdentity.
   * @return The PeerIdentityStatus associated with the given PeerIdentity.
   */
  public PeerIdentityStatus getPeerIdentityStatus(PeerIdentity pid) {
    return findPeerIdentityStatus(pid);
  }
  
  /**
   * @param key The Identity Key
   * @return The PeerIdentityStatus associated with the given PeerIdentity.
   */
  public PeerIdentityStatus getPeerIdentityStatus(String key) {
    synchronized (pidMap) {
      PeerIdentity pid = pidMap.get(key);
      if (pid != null) {
        return getPeerIdentityStatus(pid);
      } else {
        return null;
      }
    }
  }

  /**
   * <p>Finds or creates unique instances of both PeerIdentity and
   * LcapIdentity.</p>
   * <p>Eventually, LcapIdentity won't always be created here.</p>
   */
  private PeerIdentity findLocalPeerIdentity(String key)
      throws MalformedIdentityKeyException {
    PeerIdentity pid;
    synchronized (pidMap) {
      pid = pidMap.get(key);
      if (pid == null || !pid.isLocalIdentity()) {
        pid = ensureNormalizedPid(key, new PeerIdentity.LocalIdentity(key));
        pidMap.put(key, pid);
	pidSet.add(pid);
      }
    }
    // for now always make sure LcapIdentity instance exists
    findLcapIdentity(pid, key);
    return pid;
  }

  /**
   * <p>Finds or creates unique instances of PeerIdentity.</p>
   */
  public PeerIdentity findPeerIdentity(String key)
      throws MalformedIdentityKeyException {
    synchronized (pidMap) {
      PeerIdentity pid = pidMap.get(key);
      if (pid == null) {
        pid = ensureNormalizedPid(key, new PeerIdentity(key));
	pidMap.put(key, pid);
	pidSet.add(pid);
      }
      return pid;
    }
  }

  private PeerIdentity ensureNormalizedPid(String key, PeerIdentity pid) {
    String normKey = pid.getKey();
    if (!key.equals(normKey)) {
      // The key we were given is unnormalized, see if we have already
      // have a pid for the normalized key
      PeerIdentity normPid = pidMap.get(normKey);
      if (normPid == null) {
	// this is a new pid, store under normalized key and continue using
	// it
	log.debug("Unnormalized key (new): " + key + " != " + normKey);
	pidMap.put(normKey, pid);
      } else {
	// use existing pid
	log.debug("Unnormalized key: " + key + " != " + normKey);
	return normPid;
      }
    }
    return pid;
  }

  /**
   * <p>Finds or creates unique instances of both PeerIdentity and
   * LcapIdentity.</p>
   * <p>Eventually, LcapIdentity won't always be created here.
   */
  private PeerIdentity findPeerIdentityAndData(String key)
      throws MalformedIdentityKeyException {
    PeerIdentity pid = findPeerIdentity(key);
    // for now always make sure LcapIdentity instance exists
    findLcapIdentity(pid, key);
    return pid;
  }

  /**
   * <p>Finds or creates unique instances of both PeerIdentity and
   * LcapIdentity.</p>
   * <p>Eventually, LcapIdentity won't always be created here.</p>
   */
  private PeerIdentity findPeerIdentityAndData(IPAddr addr, int port)
      throws MalformedIdentityKeyException {
    String key = IDUtil.ipAddrToKey(addr, port);
    PeerIdentity pid = findPeerIdentity(key);
    // for now always make sure LcapIdentity instance exists
    findLcapIdentity(pid, addr, port);
    return pid;
  }

  /**
   * <p>Finds or creates unique instances of LcapIdentity.</p>
   */
  public LcapIdentity findLcapIdentity(PeerIdentity pid, String key) {
    synchronized (theLcapIdentities) {
      PeerIdentityStatus status = theLcapIdentities.get(pid);
      if (status == null) {
        log.debug2("Making new PeerIdentityStatus for: " + pid);
        status = new PeerIdentityStatus(new LcapIdentity(pid, key));
        theLcapIdentities.put(pid, status);
      }
      return status.getLcapIdentity();
    }
  }

  /**
   * <p>Finds or creates unique instances of LcapIdentity.</p>
   */
  protected LcapIdentity findLcapIdentity(PeerIdentity pid,
                                          IPAddr addr,
                                          int port) {
    synchronized (theLcapIdentities) {
      PeerIdentityStatus status = theLcapIdentities.get(pid);
      if (status == null) {
        log.debug2("Making new PeerIdentityStatus for: " + pid);
        status = new PeerIdentityStatus(new LcapIdentity(pid, addr, port));
        theLcapIdentities.put(pid, status);
      }
      return status.getLcapIdentity();
    }
  }

  /**
   * <p>Returns the peer identity matching the IP address and port;
   * An instance is created if necesary.</p>
   * <p>Used only by LcapDatagramRouter (and soon by its stream
   * analog).</p>
   * @param addr The IPAddr of the peer, null for the local peer.
   * @param port The port of the peer.
   * @return The PeerIdentity representing the peer.
   */
  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr, int port)
      throws MalformedIdentityKeyException {
    if (addr == null) {
      log.warning("ipAddrToPeerIdentity(null) is deprecated.");
      log.warning("  Use getLocalPeerIdentity() to get a local identity");
      // XXX return V1 identity until all callers fixed
      return localPeerIdentities[Poll.V1_PROTOCOL];
    }
    else {
      return findPeerIdentityAndData(addr, port);
    }
  }

  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr)
      throws MalformedIdentityKeyException {
    return ipAddrToPeerIdentity(addr, 0);
  }

  /**
   * <p>Returns the peer identity matching the String IP address and
   * port. An instance is created if necesary. Used only by
   * LcapMessage (and soon by its stream analog).
   * @param idKey the ip addr and port of the peer, null for the local
   *              peer.
   * @return The PeerIdentity representing the peer.
   */
  public PeerIdentity stringToPeerIdentity(String idKey)
      throws IdentityManager.MalformedIdentityKeyException {
    if (idKey == null) {
      log.warning("stringToPeerIdentity(null) is deprecated.");
      log.warning("  Use getLocalPeerIdentity() to get a local identity");
      // XXX return V1 identity until all callers fixed
      return localPeerIdentities[Poll.V1_PROTOCOL];
    }
    else {
      return findPeerIdentityAndData(idKey);
    }
  }

  public IPAddr identityToIPAddr(PeerIdentity pid) {
    LcapIdentity lid = theLcapIdentities.get(pid).getLcapIdentity();
    if (lid == null) {
      log.error(pid.toString() + " has no LcapIdentity");
    } else if (lid.getPort() != 0) {
      log.error(pid.toString() + " is not a V1 identity");
    } else {
      return lid.getAddress();
    }
    throw new IllegalArgumentException(pid.toString());
  }

  /**
   * Returns the local peer identity.
   * @param pollVersion The poll protocol version.
   * @return The local peer identity associated with the poll version.
   * @throws IllegalArgumentException if the pollVersion is not
   *                                  configured or is outside the
   *                                  legal range.
   */
  public PeerIdentity getLocalPeerIdentity(int pollVersion) {
    PeerIdentity pid = null;
    try {
      pid = localPeerIdentities[pollVersion];
    } catch (ArrayIndexOutOfBoundsException e) {
      // fall through
    }
    if (pid == null) {
      throw new IllegalArgumentException("Illegal poll version: " +
					 pollVersion);
    }
    return pid;
  }

  /**
   * @return a list of all local peer identities.
   */
  public List<PeerIdentity> getLocalPeerIdentities() {
    List<PeerIdentity> res = new ArrayList();
    for (PeerIdentity pid : localPeerIdentities) {
      if (pid != null) {
	res.add(pid);
      }
    }
    return res;
  }

  /**
   * <p>Returns the IPAddr of the local peer.</p>
   * @return The IPAddr of the local peer.
   */
  public IPAddr getLocalIPAddr() {
    return theLocalIPAddr;
  }

  /**
   * <p>Determines if this PeerIdentity is the same as the local
   * host.</p>
   * @param id The PeerIdentity.
   * @return true if is the local identity, false otherwise.
   */
  public boolean isLocalIdentity(PeerIdentity id) {
    return id.isLocalIdentity();
  }

  /**
   * <p>Determines if this PeerIdentity is the same as the local
   * host.</p>
   * @param idStr The string representation of the voter's
   *        PeerIdentity.
   * @return true if is the local identity, false otherwise.
   */
  public boolean isLocalIdentity(String idStr) {
    try {
      return isLocalIdentity(stringToPeerIdentity(idStr));
    } catch (IdentityManager.MalformedIdentityKeyException e) {
      return false;
    }
  }

  /**
   * <p>Associates the event with the peer identity.</p>
   * @param id    The PeerIdentity.
   * @param event The event code.
   * @param msg   The LcapMessage involved.
   */
  public void rememberEvent(PeerIdentity id, int event, LcapMessage msg) {
    LcapIdentity lid = theLcapIdentities.get(id).getLcapIdentity();
    if (lid != null) {
      lid.rememberEvent(event, msg);
    }
  }

  /**
   * <p>Returns the max value of an Identity's reputation.</p>
   * @return The int value of max reputation.
   */
  public int getMaxReputation() {
    return REPUTATION_NUMERATOR;
  }

  /**
   * <p>Returns the reputation of the peer.</p>
   * @param id The PeerIdentity.
   * @return The peer's reputation.
   */
  public int getReputation(PeerIdentity id) {
    int ret = 0;
    LcapIdentity lid = theLcapIdentities.get(id).getLcapIdentity();
    if (lid == null) {
      log.error("Can't find LcapIdentity for " + id.toString());
    } else {
      ret = lid.getReputation();
    }
    return ret;
  }

  /**
   * <p>Returns the amount of reputation change that reflects the
   * specified kind of event.</p>
   * @param changeKind The type of event.
   * @return The delta that would be applied to a peer's reputation.
   */
  protected int getReputationDelta(int changeKind) {
    int ret = -1;
    if (changeKind >= 0 && changeKind < reputationDeltas.length)
      ret = reputationDeltas[changeKind];
    return ret;
  }

  /**
   * <p>Makes the change to the reputation of the peer "id" matching
   * the event "changeKind".
   * @param id         The PeerIdentity of the peer to affect.
   * @param changeKind The type of event that is being reflected.
   */
  public void changeReputation(PeerIdentity id, int changeKind) {
    int delta = getReputationDelta(changeKind);
    int max_delta = reputationDeltas[MAX_DELTA];
    LcapIdentity lid = theLcapIdentities.get(id).getLcapIdentity();
    if (lid == null) {
      log.error("Can't find LcapIdentity for " + id.toString());
      return;
    }
    int reputation = lid.getReputation();

    if (id.isLocalIdentity()) {
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

  File setupIddbFile() {
    if (iddbFile == null) {
      String iddbDir = CurrentConfig.getParam(PARAM_IDDB_DIR);
      if (iddbDir != null) {
	iddbFile = new File(iddbDir, IDDB_FILENAME);
      }
    }
    return iddbFile;
  }

  /**
   * <p>Reloads the peer data from the identity database.</p>
   * <p>This may overwrite the LcapIdentity instance for local
   * identity(s). That may not be appropriate if this is ever called
   * other than at startup.</p>
   * @see #reloadIdentities(ObjectSerializer)
   */
  void reloadIdentities() {
    reloadIdentities(makeIdentityListSerializer());
  }

  /**
   * <p>Reloads the peer data from the identity database using the
   * given deserializer.</p>
   * @param deserializer An ObjectSerializer instance.
   * @see #reloadIdentities()
   */
  void reloadIdentities(ObjectSerializer deserializer) {
    if (setupIddbFile() == null) {
      log.warning("Cannot load identities; no value for '"
          + PARAM_IDDB_DIR + "'.");
      return;
    }

    synchronized (iddbFile) {
      try {
        // CASTOR: Remove unwrap() call; add cast to HashMap
        HashMap map = 
          (HashMap<PeerIdentity,PeerIdentityStatus>) deserializer.
          deserialize(iddbFile);
        synchronized (theLcapIdentities) {
          theLcapIdentities.putAll(map);
        }
      }
      catch (SerializationException.FileNotFound e) {
        log.warning("No identity database");
      }
      catch (Exception e) {
        log.warning("Could not load identity database", e);
      }
    }
  }

  /**
   * <p>Used by the PollManager to record the result of tallying a
   * poll.</p>
   * @see #storeIdentities(ObjectSerializer)
   */
  public void storeIdentities()
      throws ProtocolException {
    storeIdentities(makeIdentityListSerializer());
  }

  /**
   * <p>Records the result of tallying a poll using the given
   * serializer.</p>
   */
  public void storeIdentities(ObjectSerializer serializer)
      throws ProtocolException {
    if (setupIddbFile() == null) {
      log.warning("Cannot store identities; no value for '"
          + PARAM_IDDB_DIR + "'.");
      return;
    }

    synchronized (iddbFile) {
      try {
        File dir = iddbFile.getParentFile();
        if (dir != null) {
	  FileUtil.ensureDirExists(dir);
	}
        // CASTOR: Remove call to wrap()
        serializer.serialize(iddbFile, wrap(theLcapIdentities));
      }
      catch (Exception e) {
        log.error("Could not store identity database", e);
        throw new ProtocolException("Unable to store identity database.");
      }
    }
  }

  /**
   * <p>Builds an ObjectSerializer suitable for storing identity
   * maps.</p>
   * @return An initialized ObjectSerializer instance.
   */
  private ObjectSerializer makeIdentityListSerializer() {
    XStreamSerializer serializer = new XStreamSerializer(getDaemon());
    return serializer;
  }

  /**
   * <p>Copies the identity database file to the stream.</p>
   * @param out OutputStream instance.
   */
  public void writeIdentityDbTo(OutputStream out) throws IOException {
    // XXX hokey way to have the acceess performed by the object that has the
    // appropriate lock
    if (setupIddbFile() == null) {
      return;
    }
    if (iddbFile.exists()) {
      synchronized (iddbFile) {
        InputStream in =
          new BufferedInputStream(new FileInputStream(iddbFile));
        try {
          StreamUtil.copy(in, out);
        } finally {
          IOUtil.safeClose(in);
        }
      }
    }
  }

  /**
   * @deprecated
   */
  public IdentityListBean getIdentityListBean() {
    throw new UnsupportedOperationException("getIdentityListBean() has " + "" +
    		                            "been deprecated.");
  }

  /**
   * <p>Return a collection o all V1-style PeerIdentities.</p>
   */
  public Collection getUdpPeerIdentities() {
    Collection retVal = new ArrayList();
    for (PeerIdentity id : pidSet) {
      if (id.getPeerAddress() instanceof PeerAddress.Udp &&
	  !id.isLocalIdentity())
	retVal.add(id);
    }
    return retVal;
  }

  /**
   * <p>Return a collection of all V3-style PeerIdentities.</p>
   */
  public Collection getTcpPeerIdentities() {
    return getTcpPeerIdentities(PredicateUtils.truePredicate());
  }

  /**
   * Return a filtered collection of V3-style PeerIdentities.
   */
  public Collection getTcpPeerIdentities(Predicate peerPredicate) {
    Collection retVal = new ArrayList();
    for (PeerIdentity id : pidSet) {
      if (id.getPeerAddress() instanceof PeerAddress.Tcp
	  && !id.isLocalIdentity()
	  && peerPredicate.evaluate(id)) {
	retVal.add(id);
      }
    }
    return retVal;
  }

  /**
   * <p>Castor+XStream transition helper method, that wraps the
   * identity map into the object expected by serialization code.</p>
   * @param theIdentities The {@link #theLcapIdentities} map.
   * @return An object suitable for serialization.
   */
  private Serializable wrap(Map theIdentities) {
    return (Serializable)theIdentities;
  }

  /**
   * <p>Castor+XStream transition helper method, that unwraps the
   * identity map when it returns from serialized state.</p>
   * @param obj The object returned by deserialization code.
   * @return An unwrapped identity map.
   */
//  private HashMap unwrap(Object obj) {
//    return (HashMap)obj;
//  }

  /**
   * <p>Signals that we've agreed with pid on a top level poll on
   * au.</p>
   * <p>Only called if we're both on the winning side.</p>
   * @param pid The PeerIdentity of the agreeing peer.
   * @param au  The {@link ArchivalUnit}.
   */
  public void signalAgreed(PeerIdentity pid, ArchivalUnit au) {
    signalPartialAgreement(pid, au, 1.0f);
  }

  /**
   * <p>Signals that we've disagreed with pid on any level poll on
   * au.</p>
   * <p>Only called if we're on the winning side.</p>
   * @param pid The PeerIdentity of the disagreeing peer.
   * @param au  The {@link ArchivalUnit}.
   */
  public void signalDisagreed(PeerIdentity pid, ArchivalUnit au) {
    signalPartialAgreement(pid, au, 0.0f);
  }

  /**
   * Signal that we've reached partial agreement with a peer during a
   * V3 poll on au.
   *
   * @param pid
   * @param au
   * @param percent
   */
  public void signalPartialAgreement(PeerIdentity pid, ArchivalUnit au,
                                     float percent) {
    signalPartialAgreement(AgreementType.POR, pid, au, percent);
  }

  /**
   * Get the percent agreement for a V3 poll on a given AU.
   *
   * @param pid The {@link PeerIdentity}.
   * @param au The {@link ArchivalUnit}.
   *
   * @return The percent agreement for this AU and peer.
   */
  public float getPercentAgreement(PeerIdentity pid, ArchivalUnit au) {
    AuAgreements auAgreements = findAuAgreements(au);
    float percentAgreement = auAgreements.
      findPeerAgreement(pid, AgreementType.POR). getPercentAgreement();
    return percentAgreement;
  }
  
  public float getHighestPercentAgreement(PeerIdentity pid, ArchivalUnit au) {
    AuAgreements auAgreements = findAuAgreements(au);
    float highestPercentAgreement = auAgreements.
      findPeerAgreement(pid, AgreementType.POR). getHighestPercentAgreement();
    return highestPercentAgreement;
  }

  /**
   * Record the agreement hint we received from one of our votes in a
   * V3 poll on au.
   *
   * @param pid
   * @param au
   * @param percent
   */
  public void signalPartialAgreementHint(PeerIdentity pid, ArchivalUnit au,
					 float percent) {
    signalPartialAgreement(AgreementType.POR_HINT, pid, au, percent);
  }

  /**
   * Signal partial agreement with a peer on a given archival unit following
   * a V3 poll.
   *
   * @param agreementType The {@link AgreementType} to be recorded.
   * @param pid The {@link PeerIdentity} of the agreeing peer.
   * @param au The {@link ArchivalUnit}.
   * @param agreement A number between {@code 0.0} and {@code
   *                   1.0} representing the percentage of agreement
   *                   on the portion of the AU polled.
   */
  public void signalPartialAgreement(AgreementType agreementType, 
				     PeerIdentity pid, ArchivalUnit au,
                                     float agreement) {
    if (log.isDebug3()) {
      log.debug3("called signalPartialAgreement("+
		 "agreementType="+agreementType+
		 ", pid="+pid+
		 ", au="+au+
		 ", agreement"+agreement+
		 ")");
    }
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    if (pid == null) {
      throw new IllegalArgumentException("Called with null pid");
    }
    if (agreement < 0.0f || agreement > 1.0f) {
      throw new IllegalArgumentException("pecentAgreement must be between "+
					 "0.0 and 1.0. It was: "+agreement);
    }
    AuAgreements auAgreements = findAuAgreements(au);
    if (auAgreements == null) {
      log.error("No auAgreements: " + au.getName());
    } else {
      auAgreements.signalPartialAgreement(pid, agreementType, agreement,
					  TimeBase.nowMs());
      auAgreements.store(getHistoryRepository(au));
      AuState aus = AuUtil.getAuState(au);
      // XXX ER/EE AuState s.b. updated with repairer count, not repairee.
      int willingRepairers =
	auAgreements.countAgreements(AgreementType.POR,
				     minPercentPartialAgreement);
      aus.setNumWillingRepairers(willingRepairers);
    }
  }

  /**
   * Signal the completion of a local hash check.
   *
   * @param filesCount The number of files checked.
   * @param urlCount The number of URLs checked.
   * @param agreeCount The number of files which agreed with their
   * previous hash value.
   * @param disagreeCount The number of files which disagreed with
   * their previous hash value.
   * @param missingCount The number of files which had no previous
   * hash value.
   */
  public void signalLocalHashComplete(LocalHashResult lhr) {
    log.debug("called signalLocalHashComplete("+ lhr + ")");
  }

  /**
   * Get the percent agreement hint for a V3 poll on a given AU.
   *
   * @param pid The {@link PeerIdentity}.
   * @param au The {@link ArchivalUnit}.
   *
   * @return The percent agreement hint for this AU and peer.
   */
  public float getPercentAgreementHint(PeerIdentity pid, ArchivalUnit au) {
    AuAgreements auAgreements = findAuAgreements(au);
    return auAgreements.findPeerAgreement(pid, AgreementType.POR_HINT).
      getPercentAgreement();
  }
  
  public float getHighestPercentAgreementHint(PeerIdentity pid, ArchivalUnit au) {
    AuAgreements auAgreements = findAuAgreements(au);
    return auAgreements.findPeerAgreement(pid, AgreementType.POR_HINT).
      getHighestPercentAgreement();
  }

  /**
   * A list of peers with whom we have had a POR poll and a result
   * above the minimum threshold for repair.
   *
   * NOTE: No particular order should be assumed.
   * NOTE: This does NOT use the "hint", which would be more reasonable.
   *
   * @param au ArchivalUnit to look up PeerIdentities for.
   * @return List of peers from which to try to fetch repairs for the
   *         AU. Never {@code null}.
   */
  public List<PeerIdentity> getCachesToRepairFrom(ArchivalUnit au) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    // NOTE: some tests rely on the MockIdentityManager changing
    // getAgreed() and having that change getCachesToRepairFrom
    return new ArrayList(getAgreed(au).keySet());
  }

  /**
   * Count the peers with whom we have had a POR poll and a result
   * above the minimum threshold for repair.
   *
   * NOTE: This does NOT use the "hint", which would be more reasonable.
   *
   * @param au ArchivalUnit to look up PeerIdentities for.
   * @return Count of peers we believe are willing to send us repairs for
   * this AU.
   */
  public int countCachesToRepairFrom(ArchivalUnit au) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    AuAgreements auAgreements = findAuAgreements(au);
    return auAgreements.countAgreements(AgreementType.POR,
					minPercentPartialAgreement);
  }

  /**
   * Return a mapping for each peer for which we have an agreement of
   * the requested type, to the {@link PeerAgreement} record for that
   * peer.
   *
   * @param au The {@link ArchivalUnit} in question.
   * @param type The {@link AgreementType} to look for.
   * @return A Map mapping each {@link PeerIdentity} which has an
   * agreement of the requested type to the {@link PeerAgreement} for
   * that type.
   */
  public Map<PeerIdentity, PeerAgreement> getAgreements(ArchivalUnit au,
							AgreementType type) {
    AuAgreements auAgreements = findAuAgreements(au);
    return auAgreements.getAgreements(type);
  }

  public boolean hasAgreed(String ip, ArchivalUnit au)
      throws IdentityManager.MalformedIdentityKeyException {
    return hasAgreed(stringToPeerIdentity(ip), au);
  }

  public boolean hasAgreed(PeerIdentity pid, ArchivalUnit au) {
    AuAgreements auAgreements = findAuAgreements(au);
    return auAgreements.hasAgreed(pid, minPercentPartialAgreement);
  }

  /** Convenience method returns agreement on AU au, of AgreementType type
   * with peer pid.  Returns -1.0 if no agreement of the specified type has
   * been recorded. */
  public float getPercentAgreement(PeerIdentity pid,
				   ArchivalUnit au,
				   AgreementType type) {
    PeerAgreement pa = findAuAgreements(au).findPeerAgreement(pid, type);
    return pa.getPercentAgreement();
  }

  /** Convenience method returns highest agreement on AU au, of
   * AgreementType type with peer pid.  Returns -1.0 if no agreement of the
   * specified type has been recorded. */
  public float getHighestPercentAgreement(PeerIdentity pid,
					  ArchivalUnit au,
					  AgreementType type) {
    PeerAgreement pa = findAuAgreements(au).findPeerAgreement(pid, type);
    return pa.getHighestPercentAgreement();
  }

  /**
   * <p>Return map peer -> last agree time. Used for logging and
   * debugging.</p>
   */
  public Map<PeerIdentity, Long> getAgreed(ArchivalUnit au) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    AuAgreements auAgreements = findAuAgreements(au);
    Map<PeerIdentity, PeerAgreement> agreements =
      auAgreements.getAgreements(AgreementType.POR);
    Map<PeerIdentity, Long> result = new HashMap();
    for (Map.Entry<PeerIdentity, PeerAgreement> ent: agreements.entrySet()) {
      PeerAgreement agreement = ent.getValue();
      long percentAgreementTime = ent.getValue().getPercentAgreementTime();
      if (agreement.getHighestPercentAgreement() 
	  >= minPercentPartialAgreement) {
	PeerIdentity pid = ent.getKey();
	result.put(pid, Long.valueOf(percentAgreementTime));
      }
    }
    return result;
  }

  public AuAgreements findAuAgreements(ArchivalUnit au) {
    AuAgreements auAgreements;
    String auId = au.getAuId();

    HistoryRepository hRep = getHistoryRepository(au);
    auAgreements = (AuAgreements)agreeMapsCache.get(auId);

    if (auAgreements == null) {
      auAgreements = AuAgreements.make(hRep, this);
      // Multiple threads might have constructed an instance, but
      // exactly one will put it in the cache. The rest will get that
      // copy.
      auAgreements = (AuAgreements)agreeMapsCache.putIfNew(auId, auAgreements);
    }
    return auAgreements;
  }

  public boolean hasAgreeMap(ArchivalUnit au) {
    synchronized (agreeMapsCache) {
      AuAgreements auAgreements =
	(AuAgreements)agreeMapsCache.get(au.getAuId());
      // If we have a value in the map, it should be synched to the
      // file.
      if (auAgreements != null) {
	return auAgreements.haveAgreements();
      }
    }
    
    HistoryRepository hRep = getHistoryRepository(au);
    return hRep.getIdentityAgreementFile().exists();
  }
  
  public synchronized void removePeer(String key) {
    log.debug("Removing peer " + key);
    PeerIdentity pid = (PeerIdentity)(pidMap.get(key));
    // Must remove all mappings from unnormalized key, in addition to the
    // normalized one.  This operation is rare, so searching for them is
    // ok.
    List<String> allKeys = new ArrayList<String>();
    for (Map.Entry<String,PeerIdentity> ent : pidMap.entrySet()) {
      if (ent.getValue() == pid) {
	allKeys.add(ent.getKey());
      }
    }
    for (String onekey : allKeys) {
      pidMap.remove(onekey);
    }
    pidSet.remove(pid);
    theLcapIdentities.remove(pid);
    try {
      storeIdentities();
    } catch (Exception ex) {
      log.error("Unable to store IDDB!", ex);
    }
  }

  // NOTE: The calls to HistoryRepository to store and load
  // AuAgreements are serialized by locking the AuAgreements instance
  // for the AU. The writeTo and ReadFrom calls are used from
  // RemoteApi to save and restore back-ups, and need to make sure
  // that the locking of the HistoryRepository is correct, so those
  // calls are sent through the AuAgreements, via the
  // IdentityManagerImpl.

  /**
   * <p>Copies the identity agreement file for the AU to the given
   * stream.</p>
   * @param au  An archival unit.
   * @param out An output stream.
   * @throws IOException if input or output fails.
   */
  public void writeIdentityAgreementTo(ArchivalUnit au, OutputStream out)
      throws IOException {
    // have the file access performed by the AuAgreements instance,
    // since it has the appropriate lock
    AuAgreements auAgreements = findAuAgreements(au);
    auAgreements.writeTo(getHistoryRepository(au), out);
  }

  /**
   * <p>Installs the contents of the stream as the identity agreement
   * file for the AU.</p>
   * @param au An archival unit.
   * @param in An input stream to read from.
   * @return {@code true} if the copy was successful and the au's
   * {@link AuAgreements} instance now reflects the new content.
   */
  public void readIdentityAgreementFrom(ArchivalUnit au, InputStream in)
      throws IOException {
    // have the file access performed by the AuAgreements instance,
    // since it has the appropriate lock
    AuAgreements auAgreements = findAuAgreements(au);
    auAgreements.readFrom(getHistoryRepository(au), this, in);
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
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
      updatesBeforeStoring =
        config.getLong(PARAM_UPDATES_BEFORE_STORING,
                       DEFAULT_UPDATES_BEFORE_STORING);
      minPercentPartialAgreement =
        config.getPercentage(PARAM_MIN_PERCENT_AGREEMENT,
                             DEFAULT_MIN_PERCENT_AGREEMENT);
      if (changedKeys.contains(PARAM_UI_STEM_MAP)) {
	pidUiStemMap = makePidUiStemMap(config.getList(PARAM_UI_STEM_MAP));
      }
      int agreeMapsCacheMax =
	config.getInt(PARAM_AGREE_MAPS_CACHE_MAX, DEFAULT_AGREE_MAPS_CACHE_MAX);
      agreeMapsCache.setMaxSize(agreeMapsCacheMax);
      setPeerAddresses(config.getList(PARAM_PEER_ADDRESS_MAP));
      configV3Identities();
    }
  }

  /**
   * Configure initial list of V3 peers.
   */
  private void configV3Identities() {
    List ids = CurrentConfig.getList(PARAM_INITIAL_PEERS,
                                     DEFAULT_INITIAL_PEERS);
    for (Iterator iter = ids.iterator(); iter.hasNext(); ) {
      try {
	// Just ensure the peer is in the ID map.
	findPeerIdentity((String)iter.next());
      } catch (MalformedIdentityKeyException e) {
	log.error("Malformed initial peer", e);
      }
    }
  }

  /** Set up any explicit mappings from PeerIdentity to PeerAddress.
   * Allows different peers to address the same peer differently, e,g, when
   * multiple peers are behind the same NAT. */
  void setPeerAddresses(Collection<String> peerAddressPairs) {
    if (peerAddressPairs != null) {
      for (String pair : peerAddressPairs) {
	List<String> lst = StringUtil.breakAt(pair, ',', -1, true, true);
	if (lst.size() == 2) {
	  String peer = lst.get(0);
	  String addr = lst.get(1);
	  log.debug("Setting address of " + peer + " to " + addr);
	  try {
	    PeerIdentity pid = stringToPeerIdentity(peer);
	    pid.setPeerAddress(PeerAddress.makePeerAddress(addr));
	  } catch (IdentityManager.MalformedIdentityKeyException e) {
	    log.error("Couldn't set address of " + peer + " to " + addr, e);
	  }
	} else {
	  log.error("Malformed peer,address pair: " + pair);
	}
      }
    }
  }

  Map<PeerIdentity,String> makePidUiStemMap(List<String> pidStemList) {
    if (pidStemList == null) {
      return null;
    }
    Map<PeerIdentity,String> res = new HashMap<PeerIdentity,String>();
    for (String one : pidStemList) {
      List<String> lst = StringUtil.breakAt(one, ',', -1, true, true);
      if (lst.size() == 2) {
	try {
	  PeerIdentity pid = stringToPeerIdentity(lst.get(0));
	  res.put(pid, lst.get(1));
	  if (log.isDebug3()) {
	    log.debug3("pidUiStemMap.put(" + pid + ", " + lst.get(1) + ")");
	  }
	} catch (IdentityManager.MalformedIdentityKeyException e) {
	  log.warning("Bad peer in pidUiStemMap: " +lst.get(0), e);
	}
      }
    }
    return res;
  }

  protected String getLocalIpParam(Configuration config) {
    // overridable for testing
    return config.get(PARAM_LOCAL_IP);
  }

  public String getUiUrlStem(PeerIdentity pid) {
    if (pidUiStemMap != null) {
      if (log.isDebug3()) {
	log.debug3("getUiUrlStem(" + pid + "): " + pidUiStemMap.get(pid));
      }
      return pidUiStemMap.get(pid);
    }
    return null;
  }

  boolean areMapsEqualSize() {
    return pidMap.size() == theLcapIdentities.size();
  }

  void unlinkOldIddbFiles() {
    String oldIddbFile = IdentityManager.V1_IDDB_FILENAME;
    File f = new File(oldIddbFile);
    if (f.exists() && f.canWrite()) {
      f.delete();
    }
  }

  HistoryRepository getHistoryRepository(ArchivalUnit au) {
    return getDaemon().getHistoryRepository(au);
  }

}
