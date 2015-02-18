/*
 * $Id$
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.MalformedURLException;
import java.util.*;

import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.poller.ReputationTransfers;
import org.lockss.protocol.AgreementType;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.LcapStreamComm;
import org.lockss.protocol.PeerAgreement;
import org.lockss.protocol.PeerIdentity;
import org.lockss.repository.RepositoryNode;
import org.lockss.state.AuState;
import org.lockss.util.Logger;


/**
 * Control the daemon's policy to respond to repair requests.
 */
public class RepairPolicy {
  // todo(bhayes): rationalize and incorporate
  // V3Poller.isWillingRepairer and
  // V3PollUtilV3PollFactory.countWillingRepairers.

  private static final Logger log = Logger.getLogger("RepairPolicy");

  private static final String PREFIX = Configuration.PREFIX + "poll.v3.";

  /**
   * If false, do not serve any repairs via V3.
   */
  public static final String PARAM_ALLOW_V3_REPAIRS =
    PREFIX + "allowV3Repairs";
  public static final boolean DEFAULT_ALLOW_V3_REPAIRS = true;
  
  /**
   * If true, previous agreement will be required to serve repairs even for
   * open access AUs
   */
  public static final String PARAM_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT =
    PREFIX + "openAccessRepairNeedsAgreement";
  public static final boolean
    DEFAULT_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT = false;

  /**
   * If true, serve repairs to any trusted peer.  (A peer is trusted iff we
   * are communicating with it securely, and its identity has been verified
   * to match one of the public certs in our LCAP keystore.
   */
  public static final String PARAM_REPAIR_ANY_TRUSTED_PEER =
    PREFIX + "repairAnyTrustedPeer";
  public static final boolean DEFAULT_REPAIR_ANY_TRUSTED_PEER = false;

  /**
   * If true, use per-URL agreement to determine whether it's OK to serve
   * a repair.  If false, rely on partial agreement level for serving
   * repairs.
   */
  public static final String PARAM_ENABLE_PER_URL_AGREEMENT =
    PREFIX + "enablePerUrlAgreement";
  public static final boolean DEFAULT_ENABLE_PER_URL_AGREEMENT = false;

  private boolean allowRepairs = DEFAULT_ALLOW_V3_REPAIRS;
  private boolean openAccessNeedsAgreement =
    DEFAULT_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT;
  private boolean repairAnyTrustedPeer = DEFAULT_REPAIR_ANY_TRUSTED_PEER;
  // todo(bhayes): URL agreement should be a per-AU property, not a
  // daemon property.
  private boolean perUrlAgreement = DEFAULT_ENABLE_PER_URL_AGREEMENT;

  // An agreement of any of these types will permit a repair.
  private static final EnumSet<AgreementType> permitRepairAgreements =
    EnumSet.of(AgreementType.POR, AgreementType.SYMMETRIC_POR,
	       AgreementType.POP, AgreementType.SYMMETRIC_POP);

  private final PollManager pollManager;
  private final IdentityManager idManager;
  private final LcapStreamComm scomm;
  private final ReputationTransfers reputationTransfers;

  public RepairPolicy(LockssDaemon daemon) {
    this(daemon.getPollManager(), daemon.getIdentityManager(),
	 daemon.getStreamCommManager());
  }

  RepairPolicy(PollManager pollManager, IdentityManager idManager,
		      LcapStreamComm scomm) {
    this.pollManager = pollManager;
    this.idManager = idManager;
    this.scomm = scomm;
    this.reputationTransfers = new ReputationTransfers(idManager);
    if (pollManager == null) {
      throw new IllegalStateException("No PollManager supplied.");
    }
    if (idManager == null) {
      throw new IllegalStateException("No IdentityManager supplied.");
    }
    if (scomm == null) {
      throw new IllegalStateException("No LcapStreamComm supplied.");
    }
  }

  /**
   * @return the current ReputationTransfers objects.
   */
  ReputationTransfers getReputationTransfers() {
    return reputationTransfers;
  }

  public void setConfig(Configuration newConfig,
			Configuration oldConfig,
			Configuration.Differences changedKeys) {
    allowRepairs = 
      newConfig.getBoolean(PARAM_ALLOW_V3_REPAIRS,
			   DEFAULT_ALLOW_V3_REPAIRS);
    openAccessNeedsAgreement =
      newConfig.getBoolean(PARAM_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT,
			   DEFAULT_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT);
    repairAnyTrustedPeer =
      newConfig.getBoolean(PARAM_REPAIR_ANY_TRUSTED_PEER,
			   DEFAULT_REPAIR_ANY_TRUSTED_PEER);
    perUrlAgreement =
      newConfig.getBoolean(PARAM_ENABLE_PER_URL_AGREEMENT,
			   DEFAULT_ENABLE_PER_URL_AGREEMENT);

    reputationTransfers.setConfig(newConfig, oldConfig, changedKeys);
  }

  /**
   * @param reqPid The PeerIdentity of the requesting peer.
   * @param au The ArchivalUnit being repaired.
   * @param url The URL being repaired.
   * @return true iff this daemon should serve the given repair.
   */
  public boolean shouldServeRepair(PeerIdentity reqPid,
				   ArchivalUnit au, String url) {
    log.debug2("called serveRepair: "+reqPid+", "+au+", "+url);
    if (!allowRepairs) {
      log.debug2("no v3 repairs allowed: false.");
      return false;
    }

    if (!openAccessNeedsAgreement) {
      AuState aus = AuUtil.getAuState(au);
      if (aus.isOpenAccess()) {
	log.debug2("Open access au: true.");
	return true;
      }
    }

    if (scomm.isTrustedNetwork() && repairAnyTrustedPeer) {
      log.debug2("Trusted peer: true.");
      return true;
    }

    if (perUrlAgreement) {
      return shouldServeUrlRepair(reqPid, au, url);
    } else {
      return shouldServeAuRepair(reqPid, au);
    }
  }

  // Note: this code is not run in production.
  /**
   * @param reqPid The peer requesting a repair.
   * @param au The ArchivalUnit for which the repair is requested.
   * @param url The URL for which the repair is requested.
   * @return true iff the given peer has previously had a high enough
   * agreement with us on the URL, or has had reputation transferred
   * from a peer who has.
   */
  boolean shouldServeUrlRepair(PeerIdentity reqPid,
			       ArchivalUnit au, String url) {
    RepositoryNode node;
    try {
      node = AuUtil.getRepositoryNode(au, url);
    } catch (MalformedURLException ex) {
      // Log the error, but certainly don't serve the repair.
      log.error("serveRepairs: The URL " + url + " appears to be malformed. "
		+ "Cannot serve repairs for this URL.");
      return false;
    }

    for (PeerIdentity pid:
	   reputationTransfers.getAllReputationsTransferredFrom(reqPid)) {
      if (node.hasAgreement(pid)) {
	log.debug2("Previous agreement found for peer " + pid + " on URL "
		  + url);
	return true;
      }
    }
    log.debug2("No previous agreement found for URL " + url);
    return false;
  }

  /**
   * @param reqPid The peer requesting a repair.
   * @param au The ArchivalUnit for which the repair is requested.
   * @return true iff the given peer has previously had a high enough
   * agreement with us on the ArchivalUnit, or has had reputation
   * transferred from a peer who has.
   */
  boolean shouldServeAuRepair(PeerIdentity reqPid, ArchivalUnit au) {
    double minPercentForRepair = pollManager.getMinPercentForRepair();
    log.debug2("Minimum percent agreement required for repair: "
	       + minPercentForRepair);


    for (AgreementType type: permitRepairAgreements) {
      Map<PeerIdentity, PeerAgreement> agreements =
	idManager.getAgreements(au, type);
      for (PeerIdentity pid: 
	     reputationTransfers.getAllReputationsTransferredFrom(reqPid)) {
	PeerAgreement agreement = agreements.get(pid);
	if (agreement != null) {
	  float percentAgreement = agreement.getHighestPercentAgreement();
	  log.debug2("peer " + pid + " has "+ type +
		     " agreement " + percentAgreement);
	  if (percentAgreement >= minPercentForRepair) {
	    log.debug2("Returning true: " + pid);
	    return true;
	  }
	}
      }
    }
    log.debug2("Returning false.");
    return false;
  }
}