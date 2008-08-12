/*
 * $Id: IdentityManagerImpl.java,v 1.26 2008-08-12 18:34:27 dshr Exp $
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
import org.lockss.state.HistoryRepository;
import org.lockss.util.*;
import org.lockss.util.SerializationException.FileNotFound;

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
   * <p>A mapping of human-readable Peer Identity keys (for example,
   * <tt>&quot;TCP:[192.168.0.1]:9729&quot;</tt>) to PeerIdentity objects.</p>
   */
  private Map<String,PeerIdentity> thePeerIdentities;

  /**
   * <p>The IDDB file.</p>
   */
  File iddbFile = null;

  int[] reputationDeltas = new int[10];

  private boolean isMergeRestoredAgreemMap = DEFAULT_MERGE_RESTORED_AGREE_MAP;

  /**
   * <p>Maps ArchivalUnit, by auid, to its agreement map.  Each agreement
   * map in tern maps a PeerIdentity to its corresponding IdentityAgreement
   * object.</p>
   */
  private Map<String,Map> agreeMaps = new HashMap();

  private float minPercentPartialAgreement = DEFAULT_MIN_PERCENT_AGREEMENT;

  private long updatesBeforeStoring = DEFAULT_UPDATES_BEFORE_STORING;
  private long updates = 0;

  private IdentityManagerStatus status;

  /**
   * <p>Builds a new IdentityManager instance.</p>
   */
  public IdentityManagerImpl() {
    theLcapIdentities = new HashMap();
    thePeerIdentities = new HashMap();
  }

  public void initService(LockssDaemon daemon) throws LockssAppException {
    super.initService(daemon);
    // initializing these here makes testing more predictable
    setupLocalIdentities();
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

    // Create local PeerIdentity and LcapIdentity instances
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
  private PeerIdentityStatus findPeerIdentityStatus(PeerIdentity id)
      throws MalformedIdentityKeyException {
    PeerIdentityStatus status = null;
    synchronized (theLcapIdentities) {
      status = theLcapIdentities.get(id);
      if (status == null) {
        log.debug("Looking up Lcap Id by id: " + id + " and string " +
                  id.getIdString());
        LcapIdentity lcapId = findLcapIdentity(id, id.getIdString());
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
    synchronized (theLcapIdentities) {
      return theLcapIdentities.get(pid);
    }
  }
  
  /**
   * @param key The Identity Key
   * @return The PeerIdentityStatus associated with the given PeerIdentity.
   */
  public PeerIdentityStatus getPeerIdentityStatus(String key) {
    synchronized (thePeerIdentities) {
      PeerIdentity pid = thePeerIdentities.get(key);
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
    synchronized (thePeerIdentities) {
      pid = thePeerIdentities.get(key);
      if (pid == null || !pid.isLocalIdentity()) {
        pid = new PeerIdentity.LocalIdentity(key);
        thePeerIdentities.put(key, pid);
      }
    }
    // for now always make sure LcapIdentity instance exists
    findLcapIdentity(pid, key);
    return pid;
  }

  /**
   * <p>Finds or creates unique instances of PeerIdentity.</p>
   */
  public PeerIdentity findPeerIdentity(String key) {
    synchronized (thePeerIdentities) {
      PeerIdentity pid = thePeerIdentities.get(key);
      if (pid == null) {
        pid = new PeerIdentity(key);
        thePeerIdentities.put(key, pid);
      }
      return pid;
    }
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
  private PeerIdentity findPeerIdentityAndData(IPAddr addr, int port) {
    String key = IDUtil.ipAddrToKey(addr, port);
    PeerIdentity pid = findPeerIdentity(key);
    // for now always make sure LcapIdentity instance exists
    findLcapIdentity(pid, addr, port);
    return pid;
  }

  /**
   * <p>Finds or creates unique instances of LcapIdentity.</p>
   */
  public LcapIdentity findLcapIdentity(PeerIdentity pid, String key)
      throws IdentityManager.MalformedIdentityKeyException {
    synchronized (theLcapIdentities) {
      PeerIdentityStatus status = theLcapIdentities.get(pid);
      if (status == null) {
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
  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr, int port) {
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

  public PeerIdentity ipAddrToPeerIdentity(IPAddr addr) {
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
    for (PeerIdentity id : thePeerIdentities.values()) {
      try {
        if (id.getPeerAddress() instanceof PeerAddress.Udp && !id.isLocalIdentity())
          retVal.add(id);
      } catch (MalformedIdentityKeyException e) {
        log.warning("Malformed identity key: " + id);
      }
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
    for (PeerIdentity id : thePeerIdentities.values()) {
      try {
        if (id.getPeerAddress() instanceof PeerAddress.Tcp
	    && !id.isLocalIdentity()
	    && peerPredicate.evaluate(id)) {
          retVal.add(id);
	}
      } catch (MalformedIdentityKeyException e) {
        log.warning("Malformed identity key: " + id);
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
    signalAgreed(pid, au, TimeBase.nowMs(), 1.0f);
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
    // If the agreement is below a certain percent, this is a disagreement.
    // The percent agreement is configured with the
    // MIN_PERCENT_PARTIAL_AGREEMENT parameter, and the MIN_URL_AGREEMENT
    // parameter.
    if (percent >= minPercentPartialAgreement)
      signalAgreed(pid, au, TimeBase.nowMs(), percent);
    else
      signalDisagreed(pid, au, TimeBase.nowMs(), percent);

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
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      IdentityAgreement ida = (IdentityAgreement)map.get(pid);
      if (ida == null) {
        return 0.0f;
      } else {
        return ida.getPercentAgreement();
      }
    }
  }
  
  public float getHighestPercentAgreement(PeerIdentity pid, ArchivalUnit au) {
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      IdentityAgreement ida = (IdentityAgreement)map.get(pid);
      if (ida == null) {
        return 0.0f;
      } else {
        return ida.getHighestPercentAgreement();
      }
    }
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
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (pid == null) {
      throw new IllegalArgumentException("Called with null pid");
    }
    log.debug3("signalPartialAgreementHint(" + pid + "," + au + "," + percent + ")");
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      IdentityAgreement ida = findPeerIdentityAgreement(map, pid);
      ida.setPercentAgreementHint(percent);
      storeIdentityAgreement(au);
    }
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
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      IdentityAgreement ida = (IdentityAgreement)map.get(pid);
      if (ida == null) {
        return 0.0f;
      } else {
        return ida.getPercentAgreementHint();
      }
    }
  }
  
  public float getHighestPercentAgreementHint(PeerIdentity pid, ArchivalUnit au) {
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      IdentityAgreement ida = (IdentityAgreement)map.get(pid);
      if (ida == null) {
        return 0.0f;
      } else {
        return ida.getHighestPercentAgreementHint();
      }
    }
  }

  /**
   * For V1 polls, the partialAgreement flag is meaningless (and ignored)
   */
  private void signalAgreed(PeerIdentity pid, ArchivalUnit au,
                            long time, float percentAgreement) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (pid == null) {
      throw new IllegalArgumentException("Called with null pid");
    }
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      IdentityAgreement ida = findPeerIdentityAgreement(map, pid);
      if (time > ida.getLastAgree()) {
	ida.setLastAgree(time);
        ida.setPercentAgreement(percentAgreement);
	storeIdentityAgreement(au);
      }
    }
  }

  /**
   * <p>Signals that we've disagreed with pid on any level poll on
   * au.</p>
   * <p>Only called if we're on the winning side.</p>
   * @param pid The PeerIdentity of the disagreeing peer.
   * @param au  The {@link ArchivalUnit}.
   */
  public void signalDisagreed(PeerIdentity pid, ArchivalUnit au) {
    signalDisagreed(pid, au, TimeBase.nowMs(), 0.0f);
  }

  /**
   * For V1 polls, the percentAgreement flag is meaningless (and ignored)
   */
  private void signalDisagreed(PeerIdentity pid, ArchivalUnit au,
                               long time, float percentAgreement) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (pid == null) {
      throw new IllegalArgumentException("Called with null pid");
    }
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      IdentityAgreement ida = findPeerIdentityAgreement(map, pid);
      if (time > ida.getLastDisagree()) {
        ida.setLastDisagree(time);
        ida.setPercentAgreement(percentAgreement);
        storeIdentityAgreement(au);
      }
    }
  }

  /**
   * <p>Peers with whom we have had any disagreement since the last
   * toplevel agreement are placed at the end of the list.</p>
   * @param au ArchivalUnit to look up PeerIdentities for.
   * @return List of peers from which to try to fetch repairs for the
   *         AU.
   */
  public List getCachesToRepairFrom(ArchivalUnit au) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    Map map = findAuAgreeMap(au);
    List res = new LinkedList();
    synchronized (map) {
      for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry ent = (Map.Entry)it.next();
        IdentityAgreement ida = (IdentityAgreement)ent.getValue();
        if (ida.hasAgreed()) {
          if (ida.getLastDisagree() > ida.getLastAgree()) {
            res.add(ent.getKey());
          } else {
            res.add(0, ent.getKey());
          }
        }
      }
    }
    return res;
  }

  public boolean hasAgreed(String ip, ArchivalUnit au)
      throws IdentityManager.MalformedIdentityKeyException {
    return hasAgreed(stringToPeerIdentity(ip), au);
  }

  public boolean hasAgreed(PeerIdentity pid, ArchivalUnit au) {
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      IdentityAgreement ida = (IdentityAgreement)map.get(pid);
      return ida != null ? ida.hasAgreed() : false;
    }
  }

  /**
   * <p>Returns a collection of IdentityManager.IdentityAgreement for
   * each peer that we have a record of agreeing or disagreeing with
   * us.
   */
  public Collection getIdentityAgreements(ArchivalUnit au) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      return new ArrayList(map.values());
    }
  }

  /**
   * <p>Return map peer -> last agree time. Used for logging and
   * debugging.</p>
   */
  public Map getAgreed(ArchivalUnit au) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    Map map = findAuAgreeMap(au);
    Map res = new HashMap();
    synchronized (map) {
      for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
	Map.Entry ent = (Map.Entry)it.next();
	IdentityAgreement ida = (IdentityAgreement)ent.getValue();
	if (ida.hasAgreed()) {
	  res.put(ent.getKey(), new Long(ida.getLastAgree()));
	}
      }
    }
    return res;
  }

  /**
   * <p>Returns map peer -> last disagree time. Used for logging and
   * debugging</p>.
   */
  public Map getDisagreed(ArchivalUnit au) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    }
    Map map = findAuAgreeMap(au);
    Map res = new HashMap();
    synchronized (map) {
      for (Iterator it = map.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry ent = (Map.Entry)it.next();
        IdentityAgreement ida = (IdentityAgreement)ent.getValue();
        if (ida.getLastDisagree() != 0) {
          res.put(ent.getKey(), new Long(ida.getLastDisagree()));
        }
      }
    }
    return res;
  }

  private IdentityAgreement findPeerIdentityAgreement(Map map,
                                                      PeerIdentity pid) {
    // called in synchronized block

    IdentityAgreement ida = (IdentityAgreement)map.get(pid);
    if (ida == null) {
      ida = new IdentityAgreement(pid);
      map.put(pid, ida);
    }
    return ida;
  }

  static String AGREE_MAP_INIT_KEY = "needs_init";

  Map findAuAgreeMap(ArchivalUnit au) {
    Map map;
    synchronized (agreeMaps) {
      map = (Map)agreeMaps.get(au.getAuId());
      if (map == null) {
        map = new HashMap();
        map.put(AGREE_MAP_INIT_KEY, "true");
        agreeMaps.put(au.getAuId(), map);
      }
    }
    synchronized (map) {
      if (map.containsKey(AGREE_MAP_INIT_KEY)) {
        loadIdentityAgreement(map, au);
        map.remove(AGREE_MAP_INIT_KEY);
      }
    }
    return map;
  }

  public boolean hasAgreeMap(ArchivalUnit au) {
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      return !map.isEmpty();
    }
  }
  
  public synchronized void removePeer(String key) {
    log.debug("Removing peer " + key);
    PeerIdentity pid = (PeerIdentity)(thePeerIdentities.get(key));
    thePeerIdentities.remove(key);
    theLcapIdentities.remove(pid);
    try {
      storeIdentities();
    } catch (Exception ex) {
      log.error("Unable to store IDDB!", ex);
    }
  }

  /**
   * <p>Copies the identity agreement file for the AU to the given
   * stream.</p>
   * @param au  An archival unit.
   * @param out An output stream.
   * @throws IOException if input or output fails.
   */
  public void writeIdentityAgreementTo(ArchivalUnit au, OutputStream out)
      throws IOException {
    // XXX hokey way to have the acceess performed by the object that has the
    // appropriate lock
    HistoryRepository hRep = getDaemon().getHistoryRepository(au);
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      File file = hRep.getIdentityAgreementFile();
      InputStream in = new BufferedInputStream(new FileInputStream(file));
      try {
        StreamUtil.copy(in, out);
      } finally {
        IOUtil.safeClose(in);
      }
    }
  }

  /**
   * <p>Installs the contents of the stream as the identity agreement
   * file for the AU.</p>
   * @param au An archival unit.
   * @param in An input stream to read from.
   */
  public void readIdentityAgreementFrom(ArchivalUnit au, InputStream in)
      throws IOException {
    // XXX hokey way to have the acceess performed by the object that has the
    // appropriate lock
    HistoryRepository hRep = getDaemon().getHistoryRepository(au);
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      File file = hRep.getIdentityAgreementFile();
      OutputStream out = new FileOutputStream(file);
      try {
        StreamUtil.copy(in, out);
      } finally {
        IOUtil.safeClose(out);
        map.put(AGREE_MAP_INIT_KEY, "true");	// ensure map is reread
      }
    }
  }

  private Map loadIdentityAgreement(Map map, ArchivalUnit au) {
    //only called within a synchronized block, so we don't need to
    HistoryRepository hRep = getDaemon().getHistoryRepository(au);
    List list = hRep.loadIdentityAgreements();
    if (map == null) {
      map = new HashMap();
    }
    if (list == null) {
      if (!isMergeRestoredAgreemMap) {
        map.clear();
      }
    } else {
      Set prevOnlyPids = new HashSet(map.keySet());
      for (Iterator it = list.iterator(); it.hasNext(); ) {
        IdentityAgreement ida = (IdentityAgreement)it.next();
        try {
          PeerIdentity pid = stringToPeerIdentity(ida.getId());
          if (isMergeRestoredAgreemMap) {
            ida = mergeIdAgreement((IdentityAgreement)map.get(pid), ida);
          } else {
            prevOnlyPids.remove(pid);
          }
          map.put(pid, ida);
        } catch (MalformedIdentityKeyException e) {
          log.warning("Couldn't load agreement for key " + ida.getId(), e);
        }
      }
      if (!isMergeRestoredAgreemMap) {
        for (Iterator it = prevOnlyPids.iterator(); it.hasNext(); ) {
          Object pid = it.next();
          if (pid instanceof PeerIdentity) {
            map.remove(pid);
          }
        }
      }
    }
    return map;
  }

  private void storeIdentityAgreement(ArchivalUnit au) {
    HistoryRepository hRep = getDaemon().getHistoryRepository(au);
    Map map = findAuAgreeMap(au);
    synchronized (map) {
      hRep.storeIdentityAgreements(new ArrayList(map.values()));
    }
  }

  private IdentityAgreement mergeIdAgreement(IdentityAgreement prev,
					     IdentityAgreement ida) {
    if (prev == null) {
      return ida;
    }
    prev.mergeFrom(ida);
    return prev;
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
      isMergeRestoredAgreemMap =
        config.getBoolean(PARAM_MERGE_RESTORED_AGREE_MAP,
                          DEFAULT_MERGE_RESTORED_AGREE_MAP);
      minPercentPartialAgreement =
        config.getPercentage(PARAM_MIN_PERCENT_AGREEMENT,
                             DEFAULT_MIN_PERCENT_AGREEMENT);
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
      // Just ensure the peer is in the ID map.
      findPeerIdentity((String)iter.next());
    }
  }

  protected String getLocalIpParam(Configuration config) {
    // overridable for testing
    return config.get(PARAM_LOCAL_IP);
  }

  boolean areMapsEqualSize() {
    return thePeerIdentities.size() == theLcapIdentities.size();
  }

  void unlinkOldIddbFiles() {
    String oldIddbFile = IdentityManager.V1_IDDB_FILENAME;
    File f = new File(oldIddbFile);
    if (f.exists() && f.canWrite()) {
      f.delete();
    }
  }

}
