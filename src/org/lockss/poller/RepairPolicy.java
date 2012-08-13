/*
 * $Id: RepairPolicy.java,v 1.1 2012-08-13 20:47:28 barry409 Exp $
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

import java.util.*;
import java.net.MalformedURLException;

import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.config.CurrentConfig;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.poller.ReputationTransfers;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.LcapStreamComm;
import org.lockss.protocol.PeerIdentity;
import org.lockss.repository.RepositoryNode;
import org.lockss.state.AuState;
import org.lockss.util.Logger;

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
   * Release any resources.
   * After this call, results of calls on this object are not defined.
   */
  public void release() {
    reputationTransfers.release();
  }

  /**
   * @return true iff this daemon should serve the given repair.
   */
  public boolean serveRepair(PeerIdentity pid, ArchivalUnit au, String url) {
    log.debug2("called serveRepair: "+pid+", "+au+", "+url);
    boolean allowRepairs = 
      CurrentConfig.getBooleanParam(PARAM_ALLOW_V3_REPAIRS,
                                    DEFAULT_ALLOW_V3_REPAIRS);
    if (!allowRepairs) {
      log.debug2("no v3 repairs allowed: false.");
      return false;
    }

    boolean openAccessNeedsAgreement =
      CurrentConfig.getBooleanParam(PARAM_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT,
				    DEFAULT_OPEN_ACCESS_REPAIR_NEEDS_AGREEMENT);
    if (!openAccessNeedsAgreement) {
      AuState aus = AuUtil.getAuState(au);
      if (aus.isOpenAccess()) {
	log.debug2("Open access au: true.");
	return true;
      }
    }

    boolean repairAnyTrustedPeer =
      CurrentConfig.getBooleanParam(PARAM_REPAIR_ANY_TRUSTED_PEER,
				    DEFAULT_REPAIR_ANY_TRUSTED_PEER);
    log.warning("****"+scomm);
    log.warning("****"+scomm.isTrustedNetwork());
    log.warning("****"+repairAnyTrustedPeer);

    if (scomm.isTrustedNetwork() && repairAnyTrustedPeer) {
      log.debug2("Trusted peer: true.");
      return true;
    }

    boolean perUrlAgreement =
      CurrentConfig.getBooleanParam(PARAM_ENABLE_PER_URL_AGREEMENT,
                                    DEFAULT_ENABLE_PER_URL_AGREEMENT);
    if (perUrlAgreement) {
      return serveUrlRepair(pid, au, url);
    } else {
      return serveAuRepair(pid, au);
    }
  }

  // todo(bhayes): this code should not be run in production.
  /**
   * @param pid0 The peer requesting a repair.
   * @param au The ArchivalUnit for which the repair is requested.
   * @param url The URL for which the repair is requested.
   * @return true iff the given peer has previously had a high enough
   * agreement with us on the URL, or has had reputation transferred
   * from a peer who has.
   */
  boolean serveUrlRepair(PeerIdentity pid0, ArchivalUnit au, String url) {
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
	   reputationTransfers.getAllReputationsTransferredFrom(pid0)) {
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
   * @param pid0 The peer requesting a repair.
   * @param au The ArchivalUnit for which the repair is requested.
   * @return true iff the given peer has previously had a high enough
   * agreement with us on the ArchivalUnit, or has had reputation
   * transferred from a peer who has.
   */
  boolean serveAuRepair(PeerIdentity pid0, ArchivalUnit au) {
    double minPercentForRepair = pollManager.getMinPercentForRepair();
    log.debug2("Minimum percent agreement required for repair: "
	       + minPercentForRepair);

    for (PeerIdentity pid: 
	   reputationTransfers.getAllReputationsTransferredFrom(pid0)) {
      float percentAgreement = idManager.getHighestPercentAgreement(pid, au);
      log.debug2("peer " + pid + " has agreement " + percentAgreement);
      if (percentAgreement >= minPercentForRepair) {
	log.debug2("Returning true: " + pid);
	return true;
      }
    }
    log.debug2("Returning false.");
    return false;
  }
}