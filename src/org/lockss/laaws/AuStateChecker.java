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
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.EnumMap;
import org.lockss.app.LockssDaemon;
import org.lockss.config.Configuration;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.model.cfg.AuConfiguration;
import org.lockss.laaws.model.cfg.V2AuStateBean;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.poller.PollManager;
import org.lockss.protocol.AgreementType;
import org.lockss.protocol.AuAgreements;
import org.lockss.protocol.AuAgreementsBean;
import org.lockss.protocol.DatedPeerIdSet;
import org.lockss.protocol.DatedPeerIdSetBean;
import org.lockss.protocol.DatedPeerIdSetImpl;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.IdentityManagerImpl;
import org.lockss.protocol.PeerAgreement;
import org.lockss.repository.AuSuspectUrlVersions;
import org.lockss.repository.AuSuspectUrlVersions.SuspectUrlVersion;
import org.lockss.repository.AuSuspectUrlVersionsBean;
import org.lockss.repository.RepositoryManager;
import org.lockss.state.AuState;
import org.lockss.util.Logger;

public class AuStateChecker extends Worker {

  private static final Logger log = Logger.getLogger(AuStateChecker.class);

  IdentityManagerImpl idManager;
  RepositoryManager repoManager;
  PollManager pollManager;

  public AuStateChecker(V2AuMover auMover, MigrationTask task) {
    super(auMover, task);
    this.auMover = auMover;
    this.au = task.getAu();
    IdentityManager idmgr = LockssDaemon.getLockssDaemon().getIdentityManager();
    if (idmgr instanceof IdentityManagerImpl) {
      idManager = ((IdentityManagerImpl) LockssDaemon.getLockssDaemon().getIdentityManager());
    }
    repoManager = LockssDaemon.getLockssDaemon().getRepositoryManager();
    pollManager = LockssDaemon.getLockssDaemon().getPollManager();
  }

  public void run() {
    try {
      String auName = au.getName();
      log.debug2("Starting Au State Check: " + auName);
      log.info(auName + ": Checking AU Agreements...");
      checkAuAgreements(au);
      log.info(auName + ": Checking AU Suspect Urls...");
      checkAuSuspectUrlVersions(au);
      log.info(auName + ": Checking No Au Peer Set...");
      checkNoAuPeerSet(au);
      log.info(auName + ": Checking AU State...");
      checkAuState(au);
      log.info(auName + ": Checking AU Configuration...");
      checkAuConfig(au);
    }
    catch (Exception ex) {
      String msg = "Au State Check failed: " + ex.getMessage();
      log.error(msg, ex);
      task.addError(msg);
    }
  }

  private void checkAuAgreements(ArchivalUnit au) {
    AuAgreements v1 = idManager.findAuAgreements(au);
    AuAgreementsBean v1Bean = v1.getPrunedBean(au.getAuId());
    String auName = au.getName();
    String err = null;
    if (v1 != null) {
      try {
        final String json = cfgApiClient.getAuAgreements(au.getAuId());
        Gson gson = new GsonBuilder().registerTypeAdapter(
                new TypeToken<EnumMap<AgreementType, PeerAgreement>>() {
                }.getType(),
                new EnumMapInstanceCreator<AgreementType, PeerAgreement>(AgreementType.class))
            .create();
        AuAgreementsBean v2Bean = gson.fromJson(json, AuAgreementsBean.class);
        if (v2Bean.equals(v1Bean)) {
          log.info("V2 Au Agreements are the same");
        }
        else {
          if (log.isDebug()) {
            log.debug("AuAgreements v1Bean: " + v1Bean.toString());
            log.debug("AuAgreements v2Bean: " + v2Bean.toString());
          }
          err = auName + ": V2 Au Agreements do not match.";
          log.error(err);
        }
      }
      catch (Exception ex) {
        err = auName + ": Attempt to check v2 au agreements failed: " + ex.getMessage();
        log.error(err, ex);
      }
    }
    else {
      log.warning("No Au agreements found for au in V1");
    }
    if (err != null) {
      task.addError(err);
      //what should we do to propagate this up.
    }
  }

  private void checkAuSuspectUrlVersions(ArchivalUnit au) {
    String auName = au.getName();
    String err = null;

    AuSuspectUrlVersions asuv = AuUtil.getSuspectUrlVersions(au);
    final Collection<SuspectUrlVersion> suspectList = asuv.getSuspectList();
    if (asuv != null) {
      AuSuspectUrlVersionsBean v1Bean = asuv.getBean(au.getAuId());
      try {
        final String json = cfgApiClient.getAuSuspectUrlVersions(au.getAuId());
        AuSuspectUrlVersionsBean v2Bean = new Gson().fromJson(json, AuSuspectUrlVersionsBean.class);
        if (v2Bean.equals(v1Bean)) {
          log.info("V2 Au Suspect Url Versions are the same");
        }
        else {
          if (log.isDebug()) {
            log.debug("AuSuspectUrlVersions v1Bean: " + v1Bean.toString());
              log.debug("AuSuspectUrlVersions v2Bean: " + v2Bean.toString());
          }
          err = auName + ": V2 Au Suspect Url Versions do not match.";
          log.error(err);
        }
      }
      catch (Exception ex) {
        err = auName + ": Attempt to check au suspect url versions failed: " + ex.getMessage();
        log.error(err, ex);
      }
    }
    else {
      log.info(auName + ": No v1 Au suspect url versions found.");
    }
    if (err != null) {
      task.addError(err);
      //what should we do to propagate this up.
    }
  }


  private void checkNoAuPeerSet(ArchivalUnit au) {
    DatedPeerIdSet v1 = pollManager.getNoAuPeerSet(au);
    String auName = au.getName();
    String err = null;

    if (v1 instanceof DatedPeerIdSetImpl) {
      try {
        DatedPeerIdSetBean v1Bean = ((DatedPeerIdSetImpl) v1).getBean(au.getAuId());
        String json = cfgApiClient.getNoAuPeers(au.getAuId());
        DatedPeerIdSetBean v2Bean = new Gson().fromJson(json, DatedPeerIdSetBean.class);
        if (v2Bean.equals(v1Bean)) {
          log.info("V2 No AU PeerSet are the same");
        }
        else {
          if (log.isDebug()) {
            log.debug("NoAuPeerSet v1Bean: " + v1Bean.toString());
            log.debug("NoAuPeerSet v2Bean: "+ v2Bean.toString());
          }
          err = auName + ": V2 No AU PeerSet do not match.";
          log.error(err);
        }
        log.info(auName + ": Successfully checked no Au peer set.");
      }
      catch (Exception ex) {
        err = auName + ": Attempt to check no AU peer set failed: " + ex.getMessage();
        log.error(err, ex);
      }
    }
    else {
      log.warning(auName + ": No Au peer set found for au");
    }
    if (err != null) {
      task.addError(err);
    }
  }

  private void checkAuConfig(ArchivalUnit au) {
    AuConfiguration v2Config;
    String auName = au.getName();
    String err = null;
    Configuration v1 = au.getConfiguration();
    AuConfiguration v1Config = new AuConfiguration().auId(au.getAuId());
    try {
      if (v1 != null) {
        v1.keySet().stream().filter(key -> !key.equalsIgnoreCase("reserved.repository"))
            .forEach(key -> v1Config.putAuConfigItem(key, v1.get(key)));
        v2Config = cfgApiClient.getAuConfig(au.getAuId());
        if (v2Config == null) {
          if (!v1Config.getAuConfig().isEmpty()) {
            err = auName + ": Not configured in V2.";
            log.error(err);
          }
        } else if (v2Config.equals(v1Config)) {
          log.info("Au Config is the same");
        } else {
          if (log.isDebug()) {
            log.debug("AuConfiguration v1Bean: " + v1Config.toString());
            log.debug("AuConfiguration v2Bean: "+ v2Config.toString());
          }
          err = auName + ": V2 AU Configuration does not match.";
          log.error(err);
        }
      }
    }
    catch (Exception ex) {
      err = auName + ": Attempt to check v2 Au configuration failed: " + ex.getMessage();
      log.error(err, ex);
    }
    if (err != null) {
      task.addError(err);
      //what should we do to propagate this up.
    }
  }


  private void checkAuState(ArchivalUnit au) {
    AuState v1 = AuUtil.getAuState(au);
    String auName = au.getName();
    String err = null;
    if (v1 != null) {
      try {
        V2AuStateBean v1Bean = new V2AuStateBean(v1);
        String json = cfgApiClient.getAuState(au.getAuId());
        V2AuStateBean v2Bean = new Gson().fromJson(json, V2AuStateBean.class);
        v2Bean.setCdnStems(v2Bean.getCdnStems());
        if (v2Bean.equals(v1Bean)) {
          log.info("V2 AuState is the same");
        }
        else {
          if (log.isDebug()) {
            log.debug("AuState v1Bean: "+ v1Bean.toString());
            log.debug("AuState v2Bean: "+ v2Bean.toString());
          }
          err = auName + ": V2 AuState does not match.";
          log.error(err);
        }
      }
      catch (ApiException apie) {
        err = auName + ": Attempt to check au state failed: " + apie.getCode() +
            "- " + apie.getMessage();
        log.error(err, apie);
      }
    }
    else {
      log.warning(auName + ": No State information found for au");
    }
    if (err != null) {
      task.addError(err);
      //what should we do to propagate this up.
    }
  }

  class EnumMapInstanceCreator<K extends Enum<K>, V> implements
      InstanceCreator<EnumMap<K, V>> {

    private final Class<K> enumClazz;

    public EnumMapInstanceCreator(final Class<K> enumClazz) {
      super();
      this.enumClazz = enumClazz;
    }

    @Override
    public EnumMap<K, V> createInstance(final Type type) {
      return new EnumMap<K, V>(enumClazz);
    }
  }

}
