/*
 * $Id: PollManager.java,v 1.166.2.2 2006-06-26 23:45:10 smorabito Exp $
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


package org.lockss.poller;

import java.io.*;
import java.util.*;

import org.lockss.alert.*;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.daemon.*;
import org.lockss.daemon.status.StatusService;
import org.lockss.hasher.HashService;
import org.lockss.plugin.*;
import org.lockss.poller.v3.*;
import org.lockss.poller.v3.V3Serializer.PollSerializerException;
import org.lockss.protocol.*;
import org.lockss.state.NodeManager;
import org.lockss.util.*;
import EDU.oswego.cs.dl.util.concurrent.*;

/**
 * <p>Class that manages the polling process.</p>
 * @author Claire Griffin
 * @version 1.0
 */

public class PollManager
  extends BaseLockssDaemonManager implements ConfigurableManager {

  protected static Logger theLog = Logger.getLogger("PollManager");

  static final String PREFIX = Configuration.PREFIX + "poll.";
  static final String PARAM_RECENT_EXPIRATION = PREFIX + "expireRecent";

  static final long DEFAULT_RECENT_EXPIRATION = Constants.DAY;

  // Items are moved between thePolls and theRecentPolls, so it's simplest
  // to synchronize all accesses on a single object.  That object is
  // currently thePolls, which is itself synchronized.

  private static Hashtable thePolls = new Hashtable();

  // all accesses must be synchronized on pollMapLock
  private static FixedTimedMap theRecentPolls =
    new FixedTimedMap(DEFAULT_RECENT_EXPIRATION);

  private static Object pollMapLock = thePolls;

  private static PollManager theManager = null;
  private static LcapRouter.MessageHandler m_msgHandler;
  private static IdentityManager theIDManager;
  private static HashService theHashService;
  private static LcapRouter theRouter = null;
  private AlertManager theAlertManager = null;
  private static SystemMetrics theSystemMetrics = null;
  private AuEventHandler auEventHandler;
  private HashMap serializedPollers;
  private HashMap serializedVoters;
  
  // Executor used to carry out serialized poll operations. 
  // Implementations include a queued poll executor and a pooled poll executor.
  private PollRunner theTaskRunner;

  // our configuration variables
  protected long m_recentPollExpireTime = DEFAULT_RECENT_EXPIRATION;

  // The PollFactory instances
  PollFactory [] pf = {
    null,
    new V1PollFactory(),
    null, // new V2PollFactory(),
    new V3PollFactory(),
  };

  public PollManager() {
  }

  /**
   * start the poll manager.
   * @see org.lockss.app.LockssManager#startService()
   */
  public void startService() {
    super.startService();
    // Create a poll runner.
    theTaskRunner = new PollRunner();
    
    // the services we use on an ongoing basis
    theIDManager = theDaemon.getIdentityManager();
    theHashService = theDaemon.getHashService();
    theAlertManager = theDaemon.getAlertManager();

    // register a message handler with the router
    theRouter = theDaemon.getRouterManager();
    m_msgHandler =  new RouterMessageHandler();
    theRouter.registerMessageHandler(m_msgHandler);

    // get System Metrics
    theSystemMetrics = theDaemon.getSystemMetrics();
    // register our status
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.registerStatusAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME,
				      new PollerStatus.ManagerStatus(this));
    statusServ.registerStatusAccessor(PollerStatus.POLL_STATUS_TABLE_NAME,
				      new PollerStatus.PollStatus(this));
    statusServ.registerObjectReferenceAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME,
                                               ArchivalUnit.class,
                                               new PollerStatus.ManagerStatusAuRef(this));
    // V3 status
    statusServ.registerStatusAccessor(V3PollStatus.POLLER_STATUS_TABLE_NAME,
                                      new V3PollStatus.V3PollerStatus(this));
    statusServ.registerStatusAccessor(V3PollStatus.VOTER_STATUS_TABLE_NAME,
                                      new V3PollStatus.V3VoterStatus(this));
    statusServ.registerStatusAccessor(V3PollStatus.POLLER_DETAIL_TABLE_NAME,
                                      new V3PollStatus.V3PollerStatusDetail(this));
    statusServ.registerStatusAccessor(V3PollStatus.VOTER_DETAIL_TABLE_NAME,
                                      new V3PollStatus.V3VoterStatusDetail(this));
    statusServ.registerStatusAccessor(V3PollStatus.ACTIVE_REPAIRS_TABLE_NAME,
                                      new V3PollStatus.V3ActiveRepairs(this));
    statusServ.registerStatusAccessor(V3PollStatus.COMPLETED_REPAIRS_TABLE_NAME,
                                      new V3PollStatus.V3CompletedRepairs(this));

    // register our AU event handler
    auEventHandler = new AuEventHandler.Base() {
	public void auCreated(ArchivalUnit au) {
 	  restoreAuPolls(au);
	}
	public void auDeleted(ArchivalUnit au) {
	  cancelAuPolls(au);
	}};
    theDaemon.getPluginManager().registerAuEventHandler(auEventHandler);

    // One time load of an in-memory map of AU IDs to directories. 
    preloadStoredPolls();
  }

  /**
   * stop the poll manager
   * @see org.lockss.app.LockssManager#stopService()
   */
  public void stopService() {
    if (auEventHandler != null) {
      theDaemon.getPluginManager().unregisterAuEventHandler(auEventHandler);
      auEventHandler = null;
    }
    // unregister our status
    StatusService statusServ = theDaemon.getStatusService();
    statusServ.unregisterStatusAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(PollerStatus.POLL_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.POLLER_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.VOTER_STATUS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.POLLER_DETAIL_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.VOTER_DETAIL_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.ACTIVE_REPAIRS_TABLE_NAME);
    statusServ.unregisterStatusAccessor(V3PollStatus.COMPLETED_REPAIRS_TABLE_NAME);
    statusServ.unregisterObjectReferenceAccessor(PollerStatus.MANAGER_STATUS_TABLE_NAME,
                                                 ArchivalUnit.class);

    // unregister our router
    theRouter.unregisterMessageHandler(m_msgHandler);

    // Stop the poll runner.
    if (theTaskRunner != null) {
      theTaskRunner.stop();
    }

    // null anything which might cause problems
    theTaskRunner = null;
    theIDManager = null;
    theHashService = null;
    theSystemMetrics = null;
    thePolls.clear();
    theRecentPolls.clear();
    super.stopService();
  }


  /** Cancel all polls on the specified AU.
   * @param au the AU
   */
  void cancelAuPolls(ArchivalUnit au) {
    // first collect polls to cancel
    Set toCancel = new HashSet();
    synchronized (pollMapLock) {
      for (Iterator it = thePolls.values().iterator(); it.hasNext(); ) {
	PollManagerEntry pme = (PollManagerEntry) it.next();
	ArchivalUnit pau = pme.poll.getCachedUrlSet().getArchivalUnit();
	if (pau == au && !pme.isPollCompleted()) {
	  toCancel.add(pme);
	}
      }
    }
    // then actually cancel them while not holding lock
    for (Iterator it = toCancel.iterator(); it.hasNext(); ) {
      PollManagerEntry pme = (PollManagerEntry) it.next();
      ArchivalUnit pau = pme.poll.getCachedUrlSet().getArchivalUnit();
      theHashService.cancelAuHashes(pau);
      pme.poll.stopPoll();
    }
  }

  /**
   * Call a poll.  Only used by the tree walk.
   * @param pollspec the <code>PollSpec</code> that defines the subject of
   *                 the <code>Poll</code>.
   * @return the poll, if it was successfuly called, else null.
   */
  public Poll callPoll(PollSpec pollspec) {
    BasePoll thePoll = null;
    PollFactory pollFact = getPollFactory(pollspec);
    if (pollFact == null) {
      return null;
    } else {
      long duration = pollFact.calcDuration(pollspec, this);
      if (duration <= 0) {
	theLog.debug("Duration for " + pollspec + " too short " + duration);
	return null;
      }
      try {
	thePoll = makePoll(pollspec, duration,
			   theIDManager.getLocalPeerIdentity(pollspec.getProtocolVersion()),
			   LcapMessage.getDefaultHashAlgorithm(),
                           null);
	if (thePoll != null) {
	  if (pollFact.callPoll(thePoll, theDaemon)) {
	    return thePoll;
	  } else {
	    theLog.debug("pollFact.callPoll() returned false");
	  }
	} else {
	  theLog.debug("makePoll(" + pollspec + ") returned null");
	}
      } catch (ProtocolException ex) {
	theLog.debug("Error in makePoll or callPoll", ex);
      }
    }
    return null;
  }

  /**
   * Is a poll of the given type and spec currently running
   * @param spec the PollSpec definining the location of the poll.
   * @return true if we have a poll which is running that matches pollspec
   */
  public boolean isPollRunning(PollSpec spec) {
    synchronized (pollMapLock) {
      for (Iterator it = thePolls.values().iterator(); it.hasNext(); ) {
	PollManagerEntry pme = (PollManagerEntry)it.next();
	if (pme.isSamePoll(spec)) {
	  return !pme.isPollCompleted();
	}
      }
    }
    return false;
  }
  
  public boolean isV3PollerRunning(PollSpec spec) {
    synchronized (pollMapLock) {
      for (Iterator it = thePolls.values().iterator(); it.hasNext(); ) {
        PollManagerEntry pme = (PollManagerEntry)it.next();
        if (pme.getPoll() instanceof V3Poller &&
            pme.getPollSpec().getAuId().equals(spec.getAuId())) {
          return !pme.isPollCompleted();
        }
      }
    }
    return false;
  }

  /** Return the PollManagerEntry for the poll with the specified key. */
  public PollManagerEntry getPollManagerEntry(String key) {
    return (PollManagerEntry)thePolls.get(key);
  }

  /** Find the poll either in current or recent polls */
  PollManagerEntry getCurrentOrRecentPollEntry(String key) {
    synchronized (pollMapLock) {
      PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
      if(pme == null) {
	pme = (PollManagerEntry)theRecentPolls.get(key);
      }
      return pme;
    }
  }

  // XXX: V3 -- Only required for V1 polls.
  public ActivityRegulator.Lock acquirePollLock(String key) {
    ActivityRegulator.Lock lock = null;
    PollManagerEntry pme = getCurrentOrRecentPollEntry(key);
    if(pme != null) {
      PollTally tally = pme.poll.getVoteTally();
      if(tally != null) {
	lock = tally.getActivityLock();
	tally.setActivityLock(null);
      }
    }
    return lock;
  }

  /**
   * suspend a poll while we wait for a repair
   * @param key the identifier key of the poll to suspend
   */
  // XXX: V3 -- Only required for V1 polls.
  public void suspendPoll(String key) {
    PollManagerEntry pme;
    synchronized (pollMapLock) {
      pme = getCurrentOrRecentPollEntry(key);
      if (pme != null) {
	theRecentPolls.remove(key);
	thePolls.put(key, pme);
	pme.setPollSuspended();
      }
    }
    if (pme == null) {
      theLog.debug2("ignoring suspend request for unknown key " + key);
    } else {
      theLog.debug("suspended poll " + key);
    }
  }


  /**
   * resume a poll that had been suspended for a repair and check the repair
   * @param replayNeeded true we now need to replay the poll results
   * @param key the key of the suspended poll
   */
  // XXX: V3 -- Only required for V1 polls.
  public void resumePoll(boolean replayNeeded,
			 Object key,
			 ActivityRegulator.Lock lock) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if(pme == null) {
      theLog.debug2("ignoring resume request for unknown key " + key);
      return;
    }
    theLog.debug("resuming poll " + key);
    PollTally tally = pme.getPoll().getVoteTally();
    tally.setActivityLock(lock);
    long expiration = 0;
    Deadline d;
    NodeManager nm = theDaemon.getNodeManager(tally.getArchivalUnit());
    nm.startPoll(tally.getCachedUrlSet(), tally, true);
    if (replayNeeded) {
      theLog.debug2("starting replay of poll " + key);
      PollFactory pollFact = getPollFactory(pme.poll.getVersion());
      // should be equivalent to this.  is it?
      //       PollFactory pollFact = getPollFactory(pme.spec);
      if (pollFact != null) {
	expiration = pollFact.getMaxPollDuration(Poll.V1_CONTENT_POLL);
      } else {
	expiration = 0; // XXX
      }
      d = Deadline.in(expiration);
      tally.startReplay(d);
    }
    else {
      pme.poll.stopPoll();
    }
    theLog.debug3("completed resume poll " + (String) key);
  }

  /**
   * handle an incoming message packet.  This will create a poll if
   * one is not already running. It will then call recieveMessage on
   * the poll.  This was moved from node state which kept track of the polls
   * running in the node.  This will need to be moved or amended to support this.
   * @param msg the message used to generate the poll
   * @throws IOException thrown if the poll was unsuccessfully created
   */
  void handleIncomingMessage(LcapMessage msg) throws IOException {
    if (theLog.isDebug2()) theLog.debug2("Got a message: " + msg);
    PollFactory fact = getPollFactory(msg);
    if(fact.isDuplicateMessage(msg, this)) {
      theLog.debug3("Dropping duplicate message:" + msg);
      return;
    }
    String key = msg.getKey();
    PollManagerEntry pme = getCurrentOrRecentPollEntry(key);
    if(pme != null) {
      if(pme.isPollCompleted() || pme.isPollSuspended()) {
	theLog.debug("Message received after poll was closed." + msg);
	return;
      }
    }
    BasePoll p = findPoll(msg);
    if (p != null) {
      p.setMessage(msg);
      p.receiveMessage(msg);
    }
  }

  /**
   * Find the poll defined by the <code>Message</code>.  If the poll
   * does not exist this will create a new poll (iff there are no conflicts)
   * @param msg <code>Message</code>
   * @return <code>Poll</code> which matches the message opcode, or a new
   * poll, or null if the new poll would conflict with a currently running poll.
   * @throws IOException if message opcode is unknown.
   */
  synchronized BasePoll findPoll(LcapMessage msg) throws IOException {
    String key = msg.getKey();
    BasePoll ret = null;

    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    if (pme == null) {
      theLog.debug3("findPoll: Making new poll: " + key);
      ret = makePoll(msg);
      if (theLog.isDebug3()) {
	if (ret != null) {
	  theLog.debug3("findPoll: Made new poll: " + key);
	} else {
	  theLog.debug3("findPoll: Did not make new poll: " + key);
	}
      }
    }
    else {
      theLog.debug3("findPoll: Returning existing poll: " + key);
      ret = pme.poll;
    }
    return ret;
  }

  /**
   * make a new poll of the type and version defined by the incoming message.
   * @param msg <code>Message</code> to use for
   * @return a new Poll object of the required type, or null if we don't
   * want to run this poll now (<i>ie</i>, due to a conflict with another
   * poll).
   * @throws ProtocolException if message opcode is unknown
   */
  BasePoll makePoll(LcapMessage msg) throws ProtocolException {
    theLog.debug2("makePoll: From message: " + msg);
    BasePoll poll = null;
    PollSpec spec = null;

    // XXX: V3 Refactor - this could be cleaned up
    if (msg instanceof V1LcapMessage) {
      V1LcapMessage v1msg = (V1LcapMessage)msg;
      spec = new PollSpec(v1msg);
    } else if (msg instanceof V3LcapMessage) {
      V3LcapMessage v3msg = (V3LcapMessage)msg;
      spec = new PollSpec(v3msg);
    } else {
      throw new ProtocolException("Unexpected LCAP Message type.");
    }

    long duration = msg.getDuration();
    PeerIdentity orig = msg.getOriginatorId();
    String hashAlg = msg.getHashAlgorithm();
    poll = makePoll(spec, duration, orig, hashAlg, msg);
    if (poll != null) {
      poll.setMessage(msg);
    }
    return poll;
  }

  BasePoll makePoll(PollSpec spec,
		    long duration,
		    PeerIdentity orig,
		    String hashAlg,
                    LcapMessage msg) throws ProtocolException {
    theLog.debug3("makePoll: From pollSpec " + spec);
    BasePoll ret_poll = null;
    CachedUrlSet cus = spec.getCachedUrlSet();
    // check for presence of item in the cache
    if (cus == null) {
      theLog.debug2("Ignoring poll request, don't have AU: " + spec.getAuId());
      return null;
    }
    ArchivalUnit au = cus.getArchivalUnit();
    if (!spec.getPluginVersion().equals(au.getPlugin().getVersion())) {
      theLog.debug("Ignoring poll request for " + au.getName() +
                   ", plugin version mismatch; have: " +
                   au.getPlugin().getVersion() +
                   ", need: " + spec.getPluginVersion());
      return null;
    }
    theLog.debug("Making poll from: " + spec);
    // create the appropriate poll for the message type
    PollFactory pollFact = getPollFactory(spec);
    ret_poll = pollFact.createPoll(spec, theDaemon,
                                   orig, duration, hashAlg, msg);

    if (ret_poll == null) {
      return null;
    } else {
      thePolls.put(ret_poll.getKey(), new PollManagerEntry(ret_poll));
      return ret_poll;
    }
  }

  /**
   * close the poll from any further voting
   * @param key the poll signature
   */
  public void closeThePoll(String key)  {
    PollManagerEntry pme = (PollManagerEntry)thePolls.remove(key);
    if(pme== null || pme.poll == null) {
      theLog.warning("Attempt to close unknown poll : " + key);
      return;
    }
    // mark the poll completed because if we need to call a repair poll
    // we don't want this one to be in conflict with it.
    // PollTally tally = pme.poll.getVoteTally();
    BasePoll p = pme.getPoll();
    pme.setPollCompleted();
    synchronized (pollMapLock) {
      theRecentPolls.put(key, pme);
    }

    // XXX: V3 -- Only required for V1 polls.
    //
    // Don't tell the node manager about verify polls
    // If closing a name poll that started ranged subpolls, don't tell
    // the node manager about it until all ranged subpolls have finished
    if ((p.getType() == Poll.V1_NAME_POLL ||
        p.getType() == Poll.V1_CONTENT_POLL) &&
        !p.isSubpollRunning()) {
      V1PollTally tally = (V1PollTally)p.getVoteTally();
      // if closing last name poll, concatenate all the name lists into the
      // first tally and pass that to node manager
      if (p.getType() == Poll.V1_NAME_POLL) {
        V1PollTally lastTally = (V1PollTally)tally;
        tally = lastTally.concatenateNameSubPollLists();
      }
      NodeManager nm = theDaemon.getNodeManager(tally.getArchivalUnit());
      theLog.debug("handing poll results to node manager: " + tally);
      nm.updatePollResults(p.getCachedUrlSet(), tally);
      try {
        theIDManager.storeIdentities();
      } catch (ProtocolException ex) {
        theLog.error("Unable to write Identity DB file.");
      }
      // free the activity lock
      ActivityRegulator.Lock lock = tally.getActivityLock();
      if(lock != null) {
        lock.expire();
      }
    }
  }

  /**
   * getActivePollSpecIterator returns an Iterator over the set of
   * PollSpec instances which currently have active polls on the given au.
   * @return Iterator over set of PollSpec
   */
  protected Iterator getActivePollSpecIterator(ArchivalUnit au,
					       BasePoll dontIncludePoll) {
    Set pollspecs = new HashSet();
    synchronized (pollMapLock) {
      for (Iterator iter = thePolls.values().iterator(); iter.hasNext(); ) {
	PollManagerEntry pme = (PollManagerEntry)iter.next();
	ArchivalUnit pau = pme.poll.getCachedUrlSet().getArchivalUnit();
	if (pau == au &&
	    pme.poll != dontIncludePoll &&
	    !pme.isPollCompleted()) {
	  pollspecs.add(pme.poll.getPollSpec());
	}
      }
    }
    return (pollspecs.iterator());
  }

  void raiseAlert(Alert alert) {
    theAlertManager.raiseAlert(alert);
  }

  /**
   * Ask that the specified poll runner task be executed.
   */
  public void runTask(PollRunner.Task task) {
    theTaskRunner.runTask(task);
  }
  
  /**
   * send a message to the multicast address for this archival unit
   * @param msg the LcapMessage to send
   * @param au the ArchivalUnit for this message
   * @throws IOException
   */
  void sendMessage(V1LcapMessage msg, ArchivalUnit au) throws IOException {
    if(theRouter != null) {
      theRouter.send(msg, au);
    }
  }

  /**
   * send a message to the unicast address given by an identity
   * @param msg the LcapMessage to send
   * @param au the ArchivalUnit for this message
   * @param id the PeerIdentity of the identity to send to
   * @throws IOException
   */
  void sendMessageTo(V1LcapMessage msg, ArchivalUnit au, PeerIdentity id)
      throws IOException {
    theRouter.sendTo(msg, au, id);
  }

  /**
   * send a message to the unicast address given by an identity
   * @param msg the LcapMessage to send
   * @param id the PeerIdentity of the identity to send to
   * @throws IOException
   */
  public void sendMessageTo(V3LcapMessage msg, PeerIdentity id)
      throws IOException {
    theRouter.sendTo(msg, id);
  }

  IdentityManager getIdentityManager() {
    return theIDManager;
  }

  HashService getHashService() {
    return theHashService;
  }

  public void setConfig(Configuration newConfig,
			Configuration oldConfig,
			Configuration.Differences changedKeys) {
    if (changedKeys.contains(PREFIX)) {
      m_recentPollExpireTime =
	newConfig.getTimeInterval(PARAM_RECENT_EXPIRATION,
				  DEFAULT_RECENT_EXPIRATION);
      theRecentPolls.setInterval(m_recentPollExpireTime);
    }
    for (int i = 0; i < pf.length; i++) {
      if (pf[i] != null) {
	pf[i].setConfig(newConfig, oldConfig, changedKeys);
      }
    }
  }

  public PollFactory getPollFactory(PollSpec spec) {
    return getPollFactory(spec.getProtocolVersion());
  }

  public PollFactory getPollFactory(LcapMessage msg) {
    return getPollFactory(msg.getProtocolVersion());
  }

  public PollFactory getPollFactory(int version) {
    if (version > 0 || version <= pf.length) {
      return pf[version];
    }
    theLog.error("Unknown poll version: " + version, new Throwable());
    return null;
  }


  /**
   * Load and start V3 polls that are found in a serialized state
   * on the disk.
   */
  private void preloadStoredPolls() {
    this.serializedPollers = new HashMap();
    this.serializedVoters = new HashMap();
    String relStateDir =
      CurrentConfig.getParam(V3Serializer.PARAM_V3_STATE_LOCATION,
                             V3Serializer.DEFAULT_V3_STATE_LOCATION);
    File stateDir =
      ConfigManager.getConfigManager().getPlatformDir(relStateDir);
    File[] dirs = stateDir.listFiles();
    if (dirs == null || dirs.length == 0) {
      theLog.debug2("No saved polls found.");
      return;
    }
    for (int ix = 0; ix < dirs.length; ix++) {
      File poller = new File(dirs[ix],
                             V3PollerSerializer.POLLER_STATE_BEAN);
      if (poller != null && poller.exists()) {
        // Add this poll dir to the serialized polls map.
        try {
          V3PollerSerializer pollSerializer =
            new V3PollerSerializer(theDaemon, dirs[ix]);
          PollerStateBean psb = pollSerializer.loadPollerState();
          theLog.debug2("Found saved poll for AU " + psb.getAuId()
                        + " in directory " + dirs[ix]);
          Set pollsForAu = null;
          if ((pollsForAu = (Set)serializedPollers.get(psb.getAuId())) == null) {
            pollsForAu = new HashSet();
            serializedPollers.put(psb.getAuId(), pollsForAu);
          }
          pollsForAu.add(dirs[ix]);
        } catch (PollSerializerException e) {
          theLog.error("Unable to restore poller from: " + dirs[ix], e);
          continue;
        }
      } else {
        theLog.warning("No serialized poller found in dir " + dirs[ix]);
      }
      File voter = new File(dirs[ix],
                            V3VoterSerializer.VOTER_USER_DATA_FILE);

      if (voter != null && voter.exists()) {
        theLog.info("Found serialized voter in file: " + voter);
        try {
          V3VoterSerializer voterSerializer =
            new V3VoterSerializer(theDaemon, dirs[ix]);
          VoterUserData vd = voterSerializer.loadVoterUserData();
          theLog.debug2("Found saved poll for AU " + vd.getAuId()
                        + " in directory " + dirs[ix]);
          Set pollsForAu = null;
          if ((pollsForAu = (Set)serializedVoters.get(vd.getAuId())) == null) {
            pollsForAu = new HashSet();
            serializedVoters.put(vd.getAuId(), pollsForAu);
          }
          pollsForAu.add(dirs[ix]);
        } catch (PollSerializerException e) {
          theLog.error("Unable to restore voter from: " + dirs[ix], e);
          continue;
        }
      } else {
        theLog.warning("No serialized voter found in dir " + dirs[ix]);
      }
    }
  }

  public void restoreAuPolls(ArchivalUnit au) {
    // Shouldn't happen.
    if (serializedPollers == null) {
      throw new NullPointerException("Null serialized poll map.");
    }
    if (serializedVoters == null) {
      throw new NullPointerException("Null serialized voter map.");
    }
    // Restore any pollers for this AU.
    Set pollDirs = (Set)serializedPollers.get(au.getAuId());
    if (pollDirs != null) {
      Iterator pollDirIter = pollDirs.iterator();
      while (pollDirIter.hasNext()) {
        File dir = (File)pollDirIter.next();
        try {
          V3Poller p = new V3Poller(theDaemon, dir);
          addPoll(p);
          p.startPoll();
        } catch (PollSerializerException e) {
          theLog.error("Unable to restore poller from dir: " + dir, e);
        }
      }
    }
    
    // Restore any voters for this AU.
    Set voterDirs = (Set)serializedVoters.get(au.getAuId());
    if (voterDirs != null) {
      Iterator voterDirIter = voterDirs.iterator();
      while (voterDirIter.hasNext()) {
        File dir = (File)voterDirIter.next();
        try {
          V3Voter v = new V3Voter(theDaemon, dir);
          addPoll(v);
          v.startPoll();
        } catch (PollSerializerException e) {
          theLog.error("Unable to restore poller from dir: " + dir, e);
        }
      }
    }
  }
  
  //--------------- PollerStatus Accessors -----------------------------
  public Collection getV1Polls() {
    Collection polls = new ArrayList();
    synchronized (pollMapLock) {
      for (Iterator it = thePolls.values().iterator(); it.hasNext(); ) {
        PollManagerEntry pme = (PollManagerEntry)it.next();
        if (pme.getType() == Poll.V1_CONTENT_POLL ||
            pme.getType() == Poll.V1_NAME_POLL ||
            pme.getType() == Poll.V1_VERIFY_POLL) {
          polls.add(pme);
        }
      }
      for (Iterator it = theRecentPolls.values().iterator(); it.hasNext(); ) {
        PollManagerEntry pme = (PollManagerEntry)it.next();
        if (pme.getType() == Poll.V1_CONTENT_POLL ||
            pme.getType() == Poll.V1_NAME_POLL ||
            pme.getType() == Poll.V1_VERIFY_POLL) {
          polls.add(pme);
        }
      }
    }
    return polls;
  }

  public Collection getV3Pollers() {
    Collection polls = new ArrayList();
    synchronized (pollMapLock) {
      for (Iterator it = thePolls.values().iterator(); it.hasNext(); ) {
        PollManagerEntry pme = (PollManagerEntry)it.next();
        if (pme.isV3Poll() && pme.getPoll() instanceof V3Poller) {
          polls.add(pme.getPoll());
        }
      }
      for (Iterator it = theRecentPolls.values().iterator(); it.hasNext(); ) {
        PollManagerEntry pme = (PollManagerEntry)it.next();
        if (pme.isV3Poll() && pme.getPoll() instanceof V3Poller) {
          polls.add(pme.getPoll());
        }
      }
    }
    return polls;
  }

  public Collection getV3Voters() {
    Collection polls = new ArrayList();
    synchronized (pollMapLock) {
      for (Iterator it = thePolls.values().iterator(); it.hasNext(); ) {
        PollManagerEntry pme = (PollManagerEntry)it.next();
        if (pme.isV3Poll() && pme.getPoll() instanceof V3Voter) {
          polls.add(pme.getPoll());
        }
      }
      for (Iterator it = theRecentPolls.values().iterator(); it.hasNext(); ) {
        PollManagerEntry pme = (PollManagerEntry)it.next();
        if (pme.isV3Poll() && pme.getPoll() instanceof V3Voter) {
          polls.add(pme.getPoll());
        }
      }
    }
    return polls;
  }

  public BasePoll getPoll(String key) {
    PollManagerEntry pme = getCurrentOrRecentPollEntry(key);
    if(pme != null) {
      return pme.getPoll();
    }
    return null;
  }

  //-------------- TestPollManager Accessors ----------------------------
  /**
   * remove the poll represented by the given key from the poll table and
   * return it.
   * @param key the String representation of the polls key
   * @return Poll the poll if found or null
   */
  BasePoll removePoll(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.remove(key);
    return (pme != null) ? pme.poll : null;
  }

  void addPoll(BasePoll p) {
    thePolls.put(p.getKey(), new PollManagerEntry(p));
  }

  boolean isPollActive(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    return (pme != null) ? pme.isPollActive() : false;
  }

  boolean isPollClosed(String key) {
    PollManagerEntry pme;
    synchronized (pollMapLock) {
      pme = (PollManagerEntry)theRecentPolls.get(key);
    }
    return (pme != null) ? pme.isPollCompleted() : false;
  }

  boolean isPollSuspended(String key) {
    PollManagerEntry pme = (PollManagerEntry)thePolls.get(key);
    return (pme != null) ? pme.isPollSuspended() : false;
  }

  static BasePoll makeTestPoll(LcapMessage msg) throws ProtocolException {
    if(theManager == null) {
      theManager = new PollManager();
    }
    return theManager.makePoll(msg);
  }

  long getSlowestHashSpeed() {
    return theSystemMetrics.getSlowestHashSpeed();
  }

  long getBytesPerMsHashEstimate()
      throws SystemMetrics.NoHashEstimateAvailableException {
    return theSystemMetrics.getBytesPerMsHashEstimate();
  }

  public boolean hasPoll(String key) {
    return thePolls.contains(key);
  }


  // ----------------  Callbacks -----------------------------------

  class RouterMessageHandler implements LcapRouter.MessageHandler {
    public void handleMessage(LcapMessage msg) {
      theLog.debug3("received from router message:" + msg.toString());
      try {
	handleIncomingMessage(msg);
      }
      catch (IOException ex) {
	theLog.error("handleIncomingMessage() threw", ex);
      }
    }
  }


  /**
   * <p>PollManagerEntry: </p>
   * <p>Description: Class to represent the data store in the polls table.
   * Only used by V1 polls.</p>
   * @version 1.0
   */

  public static class PollManagerEntry {
    private BasePoll poll;
    private PollSpec spec;
    private int type;
    private Deadline pollDeadline;
    private Deadline deadline;
    private String key;

    PollManagerEntry(BasePoll p) {
      poll = p;
      spec = p.getPollSpec();
      type = p.getPollSpec().getPollType();
      key = p.getKey();
      pollDeadline = p.getDeadline();
      deadline = null;
    }

    boolean isPollActive() {
      return poll.isPollActive();
    }

    boolean isPollCompleted() {
      return poll.isPollCompleted();
    }

    boolean isPollSuspended() {
      if (isV3Poll()) return false;
      return poll.getVoteTally().stateIsSuspended();
    }

    synchronized void setPollCompleted() {
      if (!isV3Poll()) {
        PollTally tally = poll.getVoteTally();
        tally.tallyVotes();
      }
    }

    synchronized void setPollSuspended() {
      poll.getVoteTally().setStateSuspended();
      if(deadline != null) {
	deadline.expire();
	deadline = null;
      }
    }

    public String getStatusString() {
      // Hack for V3.
      if (isV3Poll()) {
        return poll.getStatusString();
      } else {
        if (isPollCompleted()) {
          return poll.getVoteTally().getStatusString();
        }
        else if(isPollActive()) {
          return "Active";
        }
        else if(isPollSuspended()) {
          return "Repairing";
        }
        return "Unknown";
      }
    }

    public String getTypeString() {
      return Poll.POLL_NAME[type];
    }

    public String getShortKey() {
      return(key.substring(0,10));
    }

    public String getKey() {
      return key;
    }

    public BasePoll getPoll() {
      return poll;
    }

    public int getType() {
      return type;
    }

    public PollSpec getPollSpec() {
      return spec;
    }

    public Deadline getPollDeadline() {
      return pollDeadline;
    }

    public Deadline getDeadline() {
      return deadline;
    }

    public boolean isSamePoll(PollSpec otherSpec) {
      if(this.type == otherSpec.getPollType()) {
	return this.spec.getCachedUrlSet().equals(otherSpec.getCachedUrlSet());
      }
      return false;
    }

    /**
     * Convenience method
     * @return True iff this is a V3 poll.
     */
    // XXX: V3 -- Remove when V1 polling is no longer supported.
    public boolean isV3Poll() {
      return (this.type == Poll.V3_POLL);
    }
  }
}
