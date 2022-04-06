package org.lockss.laaws;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import org.lockss.laaws.V2AuMover.DigestCachedUrl;
import org.lockss.laaws.api.rs.StreamingCollectionsApi;
import org.lockss.laaws.api.cfg.AusApi;
import org.lockss.laaws.client.ApiException;
import org.lockss.laaws.model.rs.Artifact;
import org.lockss.laaws.model.rs.ArtifactPageInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.UrlUtil;

public abstract class Worker {

  protected V2AuMover auMover;
  protected MigrationTask task;
  protected ArchivalUnit au;
  protected boolean terminated = false;
  protected Counters ctrs;

  protected final StreamingCollectionsApi collectionsApi;
  protected final AusApi cfgApiClient;

  public Worker(V2AuMover auMover, MigrationTask task) {
    this.auMover = auMover;
    this.task = task;
    this.au = task.getAu();
    this.ctrs = task.getCounters();
    collectionsApi = auMover.getRepoCollectionsApiClient();
    cfgApiClient = auMover.getCfgAusApiClient();
  }

  protected void addError(String msg) {
    task.addError(msg);
  }

  protected boolean isAbort() {
    return auMover.isAbort();
  }
}
