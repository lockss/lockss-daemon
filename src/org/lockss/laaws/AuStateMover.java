/*
 * 2022, Board of Trustees of Leland Stanford Jr. University,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.lockss.laaws;

import com.google.gson.Gson;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.laaws.model.cfg.AuConfiguration;
import org.lockss.laaws.model.cfg.V2AuStateBean;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.poller.PollManager;
import org.lockss.protocol.AuAgreements;
import org.lockss.protocol.DatedPeerIdSet;
import org.lockss.protocol.DatedPeerIdSetImpl;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.IdentityManagerImpl;
import org.lockss.repository.AuSuspectUrlVersions;
import org.lockss.repository.RepositoryManager;
import org.lockss.state.AuState;
import org.lockss.util.Logger;

public class AuStateMover extends Worker {

  private static final Logger log = Logger.getLogger(AuStateMover.class);
  IdentityManagerImpl idManager;
  RepositoryManager repoManager;
  PollManager pollManager;

  public AuStateMover(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    IdentityManager idmgr = LockssDaemon.getLockssDaemon().getIdentityManager();
    if (idmgr instanceof IdentityManagerImpl) {
      idManager = ((IdentityManagerImpl) LockssDaemon.getLockssDaemon().getIdentityManager());
    }
    repoManager = LockssDaemon.getLockssDaemon().getRepositoryManager();
    pollManager = LockssDaemon.getLockssDaemon().getPollManager();
  }

  public void run() {
    log.debug2("Starting Au State Mover: ");
    String auName = au.getName();
    log.info(auName + ": Moving AU State...");
    moveAuState(au);
    log.info(auName + ": Moving AU Agreements...");
    moveAuAgreements(au);
    log.info(auName + ": Moving AU Suspect Urls...");
    moveAuSuspectUrlVersions(au);
    log.info(auName + ": Moving No Au Peer Set...");
    moveNoAuPeerSet(au);
    //This needs to be last
    log.info(auName + ": Moving AU Configuration...");
    moveAuConfig(au);
  }

  /**
   * Make a synchronous rest call to configuration service to add configuration info for an au.
   *
   * @param au The ArchivalUnit whose configuration is to be move
   */
  private void moveAuConfig(ArchivalUnit au) {
    Configuration v1config = au.getConfiguration();
    AuConfiguration v2config = new AuConfiguration().auId(au.getAuId());
    String auName = au.getName();
    if (v1config != null) {
      try {
        // copy the keys
        v1config.keySet().stream().filter(key -> !key.equalsIgnoreCase("reserved.repository"))
            .forEach(key -> v2config.putAuConfigItem(key, v1config.get(key)));
        // send the configuration
        cfgApiClient.putAuConfig(au.getAuId(), v2config);
        log.info(auName + ": Successfully moved AU Configuration.");
      }
      catch (Exception ex) {
        String err = auName + ": Attempt to move au configuration failed: " + ex.getMessage();
        log.error(err, ex);
        task.addError(err);
      }
    }
    else {
      log.warning(auName + ": No Configuration found for au");
    }
  }

  /**
   * Make a synchronous rest call to V2 configuration service to add the V1 Au Agreement Table.
   *
   * @param au the archival unit to be updated.
   */
  private void moveAuAgreements(ArchivalUnit au) {
    AuAgreements v1AuAgreements = idManager.findAuAgreements(au);
    String auName = au.getName();
    if (v1AuAgreements != null) {
      try {
        String json_str = new Gson().toJson(v1AuAgreements.getPrunedBean(au.getAuId()));
        cfgApiClient.patchAuAgreements(au.getAuId(), json_str, auMover.makeCookie());
        log.info(auName + ": Successfully moved AU Agreements.");
      }
      catch (Exception ex) {
        String err = auName + ": Attempt to move au agreements failed: " + ex.getMessage();
        log.error(err, ex);
        task.addError(err);
      }
    }
    else {
      log.warning("No Au agreements found for au.");
    }
  }

  /**
   * Make a synchronous rest call to V2 configuration service to add the V1 Au Suspect Urls list.
   *
   * @param au the archival unit to be updated.
   */
  private void moveAuSuspectUrlVersions(ArchivalUnit au) {
    AuSuspectUrlVersions asuv = AuUtil.getSuspectUrlVersions(au);
    String auName = au.getName();
    if (asuv != null) {
      try {
        String json_str = new Gson().toJson(asuv.getBean(au.getAuId()));
        cfgApiClient.putAuSuspectUrlVersions(au.getAuId(), json_str,
            auMover.makeCookie());
        log.info(auName + ": Successfully moved AU Suspect Url Versions.");
      }
      catch (Exception ex) {
        String err =
            auName + ": Attempt to move au suspect url versions failed: " + ex.getMessage();
        log.error(err, ex);
        task.addError(err);
      }
    }
    else {
      log.warning(auName + ": No Au suspect url versions found.");
    }
  }


  /**
   * Make a synchronous rest call to V2 configuration service to add the V1 Au NoAuPeerSet list.
   *
   * @param au the archival unit to be updated.
   */
  private void moveNoAuPeerSet(ArchivalUnit au) {
    DatedPeerIdSet noAuPeerSet = pollManager.getNoAuPeerSet(au);
    String auName = au.getName();
    if (noAuPeerSet instanceof DatedPeerIdSetImpl) {
      try {
        String json_str = new Gson().toJson(((DatedPeerIdSetImpl) noAuPeerSet).getBean(au.getAuId()));
        cfgApiClient.putNoAuPeers(au.getAuId(), json_str, auMover.makeCookie());
        log.info(auName + ": Successfully moved no Au peers.");
      }
      catch (Exception ex) {
        String err = auName + ": Attempt to move no AU peers failed: " + ex.getMessage();
        log.error(err, ex);
        task.addError(err);
      }
    }
    else {
      log.info(auName + ": No Au peer set found for au");
    }
  }

  /**
   * Make a synchronous rest call to configuration service to add state info for an au.
   *
   * @param au The ArchivalUnit  to move
   */
  private void moveAuState(ArchivalUnit au) {
    AuState v1State = AuUtil.getAuState(au);
    String auName = au.getName();
    if (v1State != null) {
      try {
        V2AuStateBean v2State = new V2AuStateBean(v1State);
        String json_str = new Gson().toJson(v2State.toMap());
        cfgApiClient.patchAuState(au.getAuId(), json_str, auMover.makeCookie());
        log.info(auName + ": Successfully moved AU State.");
      }
      catch (Exception ex) {
        String err = auName + ": Attempt to move au state failed: " + ex.getMessage();
        log.error(err, ex);
        task.addError(err);
      }
    }
    else {
      log.warning(auName + ": No State information found for au");
    }
  }
}