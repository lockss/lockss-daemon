/*

Copyright (c) 2000-2025 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.laaws;

import com.google.gson.Gson;
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

  protected Gson getGson() {
    return org.lockss.laaws.client.JSON.getGson();
  }
}
