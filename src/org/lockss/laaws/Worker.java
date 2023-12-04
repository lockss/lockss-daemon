package org.lockss.laaws;

import org.lockss.laaws.api.cfg.AusApi;
import org.lockss.laaws.api.cfg.UsersApi;
import org.lockss.laaws.api.rs.StreamingArtifactsApi;
import org.lockss.plugin.ArchivalUnit;

public abstract class Worker {

  protected V2AuMover auMover;
  protected MigrationTask task;
  protected ArchivalUnit au;
  protected String auid;
  protected boolean terminated = false;
  protected Counters ctrs;

  protected final StreamingArtifactsApi artifactsApi;
  protected final AusApi cfgApiClient;
  protected final UsersApi cfgUsersApiClient;

  public Worker(V2AuMover auMover, MigrationTask task) {
    this.auMover = auMover;
    this.task = task;
    this.au = task.getAu();
    if (this.au != null) {
      this.auid = au.getAuId();
    }
    this.ctrs = task.getCounters();
    artifactsApi = auMover.getRepoArtifactsApiClient();
    cfgApiClient = auMover.getCfgAusApiClient();
    cfgUsersApiClient = auMover.getCfgUsersApiClient();
  }

  protected void addError(String msg) {
    task.addError(msg);
  }

  protected boolean isAbort() {
    return auMover.isAbort();
  }
}
