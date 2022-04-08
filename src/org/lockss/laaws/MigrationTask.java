/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.function.*;
import org.apache.commons.lang3.builder.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

public class MigrationTask {

  private static Logger log = Logger.getLogger("MigrationTask");


  public enum TaskType {
    COPY_CU_VERSIONS,
    CHECK_CU_VERSIONS,
    COPY_AU_STATE,
    CHECK_AU_STATE,
    FINISH_AU_BULK,
    FINISH_ALL;

    private V2AuMover.Phase phase;;

    static {
      COPY_CU_VERSIONS.phase = V2AuMover.Phase.COPY;
      CHECK_CU_VERSIONS.phase = V2AuMover.Phase.VERIFY;
      COPY_AU_STATE.phase = V2AuMover.Phase.STATE;
//       CHECK_AU_STATE.phase = V2AuMover.Phase.STATE;
      FINISH_AU_BULK.phase = V2AuMover.Phase.INDEX;
    }

    public V2AuMover.Phase getTaskPhase() {
      return phase;
    }
  }

  V2AuMover auMover;
  CachedUrl cu;
  ArchivalUnit au;
  TaskType type;
  Counters counters;
  AuStatus auStat;
  CountUpDownLatch countDownLatch;
  CountUpDownLatch waitLatch;
  BiConsumer completionAction;


  public MigrationTask(V2AuMover mover, TaskType type) {
    this.auMover = mover;
    this.type = type;
  }

  public V2AuMover.Phase getTaskPhase() {
    return type.getTaskPhase();
  }

  public static MigrationTask copyCuVersions(V2AuMover mover,
                                             ArchivalUnit au,
                                             CachedUrl cu) {
    return new MigrationTask(mover, TaskType.COPY_CU_VERSIONS)
      .setCu(cu)
      .setAu(cu.getArchivalUnit());
  }

  public static MigrationTask checkCuVersions(V2AuMover mover,
                                              ArchivalUnit au, CachedUrl cu) {
    return new MigrationTask(mover, TaskType.CHECK_CU_VERSIONS)
        .setCu(cu)
        .setAu(cu.getArchivalUnit());
  }

  public static MigrationTask copyAuState(V2AuMover mover, ArchivalUnit au) {
    return new MigrationTask(mover, TaskType.COPY_AU_STATE)
      .setAu(au);
  }

  public static MigrationTask checkAuState(V2AuMover mover, ArchivalUnit au) {
    return new MigrationTask(mover, TaskType.CHECK_AU_STATE)
        .setAu(au);
  }

  public static MigrationTask finishAuBulk(V2AuMover mover, ArchivalUnit au) {
    return new MigrationTask(mover, TaskType.FINISH_AU_BULK)
      .setAu(au);
  }

  public static MigrationTask finishAll(V2AuMover mover) {
    return new MigrationTask(mover, TaskType.FINISH_ALL);
  }

  public MigrationTask setAu(ArchivalUnit au) {
    this.au = au;
    return this;
  }

  public MigrationTask setCu(CachedUrl cu) {
    this.cu = cu;
    return this;
  }

  public MigrationTask setCounters(Counters ctrs) {
    this.counters = ctrs;
    return this;
  }

  public MigrationTask setAuStatus(AuStatus stat) {
    this.auStat = stat;
    return this;
  }

  public MigrationTask setWaitLatch(CountUpDownLatch latch) {
    this.waitLatch = latch;
    return this;
  }

  public MigrationTask setCountDownLatch(CountUpDownLatch latch) {
    this.countDownLatch = latch;
    return this;
  }

  public MigrationTask setCompletionAction(BiConsumer consumer) {
    this.completionAction = consumer;
    return this;
  }

  public void addError(String msg) {
    this.auStat.addError(msg);
  }

  public Counters getCounters() {
    return counters;
  }

  public AuStatus getAuStatus() {
    return auStat;
  }

  public V2AuMover getAuMover() {
    return auMover;
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public CachedUrl getCu() {
    return cu;
  }

  public TaskType getType() {
    return type;
  }

  public CountUpDownLatch getWaitLatch() {
    return waitLatch;
  }

  public CountUpDownLatch getCountDownLatch() {
    return countDownLatch;
  }

  public boolean countDown() {
    if (countDownLatch != null) {
      return countDownLatch.countDown();
    }
    return false;
  }

  public void await() throws InterruptedException {
    if (waitLatch != null) {
      log.info(getType() + " waiting for " + waitLatch);
      waitLatch.await();
      log.info(getType() + " done waiting for " + waitLatch);
    }
  }


  public void complete(Exception e) {
    if (completionAction != null) {
      completionAction.accept(this, e);
    }
  }

  public int hashCode() {
    return new HashCodeBuilder(31, 41).
      append(type).
      append(au).
      append(cu).
      toHashCode();
  }

  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj == this) { return true; }
    if (obj.getClass() != getClass()) {
      return false;
    }
    MigrationTask rhs = (MigrationTask)obj;
    return new EqualsBuilder()
      .append(type, rhs.type)
      .append(au, rhs.au)
      .append(cu, rhs.cu)
      .isEquals();
  }
}
