package org.lockss.laaws;

import org.lockss.laaws.api.cfg.*;
import org.lockss.laaws.api.rs.StreamingArtifactsApi;
import org.lockss.plugin.ArchivalUnit;

public abstract class Worker {

  protected V2AuMover auMover;
  protected MigrationManager migreationMgr;
  protected MigrationTask task;
  protected ArchivalUnit au;
  protected String auid;
  protected boolean terminated = false;
  protected Counters ctrs;

  protected final StreamingArtifactsApi artifactsApi;
  protected final StreamingArtifactsApi artifactsApiLongCall;
  protected final AusApi cfgAusApiClient;
  protected final UsersApi cfgUsersApiClient;
  protected final ConfigApi cfgConfigApiClient;

  public Worker(V2AuMover auMover, MigrationTask task) {
    this.auMover = auMover;
    this.migreationMgr = auMover.getMigrationMgr();
    this.task = task;
    this.au = task.getAu();
    if (this.au != null) {
      this.auid = au.getAuId();
    }
    this.ctrs = task.getCounters();
    artifactsApi = auMover.getRepoArtifactsApiClient();
    artifactsApiLongCall = auMover.getRepoArtifactsApiLongCallClient();
    cfgAusApiClient = auMover.getCfgAusApiClient();
    cfgUsersApiClient = auMover.getCfgUsersApiClient();
    cfgConfigApiClient = auMover.getCfgConfigApiClient();
  }

  protected void addError(String msg) {
    task.addError(msg);
  }

  protected boolean isAbort() {
    return auMover.isAbort();
  }
}
