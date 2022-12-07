/*

Copyright (c) 2021-2022 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.commons.lang3.time.StopWatch;
import java.util.*;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.laaws.V2AuMover.Phase;
import org.lockss.util.*;
import static org.lockss.laaws.Counters.CounterType;

/** Counters and timers plus per-AU state */
public class AuStatus extends OpTimers {
  private static final Logger log = Logger.getLogger(V2AuMover.class);

  ArchivalUnit au;
  String auid;
  String auname;
  boolean isBulk;
  Phase auPhase = Phase.START;
  protected Map<Phase,CountUpDownLatch> latchMap = new HashMap<>();
  boolean abortAu;
  boolean hasV1Content;

  public AuStatus(V2AuMover auMover, ArchivalUnit au) {
    super(auMover);
    this.au = au;
    auid = au.getAuId();
    auname = au.getName();
  }

  public ArchivalUnit getAu() {
    return au;
  }

  public String getAuName() {
    return auname;
  }

  public String getAuId() {
    return auid;
  }

  public boolean isBulk() {
    return isBulk;
  }

  public AuStatus setIsBulk(boolean isBulk) {
    this.isBulk = isBulk;
    return this;
  }

  public void setHasV1Content(boolean val) {
    hasV1Content = val;
  }

  public boolean hasV1Content() {
    return hasV1Content;
  }

  public Phase getPhase() {
    return auPhase;
  }

  public AuStatus setPhase(Phase phase) {
    auPhase = phase;
    return this;
  }

  public void setLatch(Phase phase, CountUpDownLatch latch) {
    latchMap.put(phase, latch);
  }

  public CountUpDownLatch getLatch(Phase phase) {
    return latchMap.get(phase);
  }

  public void endPhase() {
    auMover.exitPhase(this);
  }

  public void abortAu() {
    abortAu = true;
  }

  public boolean isAbort() {
    return abortAu || auMover.isAbort();
  }

  public int hashCode() {
    return 33 * auid.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj == null) { return false; }
    if (obj == this) { return true; }
    if (obj.getClass() != getClass()) {
      return false;
    }
    return auid.equals(((AuStatus)obj).auid);
  }
}

