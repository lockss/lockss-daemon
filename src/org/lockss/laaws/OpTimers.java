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
import org.lockss.util.*;
import org.lockss.laaws.V2AuMover.Phase;
import static org.lockss.laaws.Counters.CounterType;

/*
 * Collection of counters and timers for either the whole request or
 * a single AU.
 */
public class OpTimers {
  private static final Logger log = Logger.getLogger(V2AuMover.class);

  V2AuMover auMover;
  protected Map<Phase,StopWatch> timerMap = new HashMap<>();
  protected Counters ctrs = new Counters();

  public OpTimers(V2AuMover auMover) {
    this.auMover = auMover;
  }

  public Counters getCounters() {
    return ctrs;
  }

  public void start(Phase phase) {
    timerMap.put(phase, StopWatch.createStarted());
  }

  public void stop(Phase phase) {
    if (!timerMap.containsKey(phase)) {
      throw new IllegalStateException("No " + phase + " timer, can't stop");
    }
    timerMap.get(phase).stop();
  }

  public void suspend(Phase phase) {
    if (!timerMap.containsKey(phase)) {
      throw new IllegalStateException("No " + phase + " timer, can't suspend");
    }
    timerMap.get(phase).suspend();
  }

  public void resume(Phase phase) {
    if (!timerMap.containsKey(phase)) {
      throw new IllegalStateException("No " + phase + " timer, can't resume");
    }
    timerMap.get(phase).resume();
  }

  public boolean hasStarted(Phase phase) {
    return timerMap.containsKey(phase) && getStartTime(phase) > 0;
  }

  public long getStartTime(Phase phase) {
    if (!timerMap.containsKey(phase)) {
      throw new IllegalStateException("No " + phase + " timer, can't get start time");
    }
    return timerMap.get(phase).getStartTime();
  }

  public long getStopTime(Phase phase) {
    if (!timerMap.containsKey(phase)) {
      throw new IllegalStateException("No " + phase + " timer, can't get stop time");
    }
    return timerMap.get(phase).getStopTime();
  }

  public long getElapsedTime(Phase phase) {
    if (!timerMap.containsKey(phase)) {
      throw new IllegalStateException("No " + phase + " timer, can't get elapsed time");
    }
    return timerMap.get(phase).getTime();
  }

  public void addCounterStatus(StringBuilder sb, Phase phase) {
    addCounterStatus(sb, phase, null);
  }

  public void addCounterStatus(StringBuilder sb, Phase phase,
                               String separator) {
    // No stats if not started yet
    if (!hasStarted(phase)) {
      return;
    }
    Counters ctrs = getCounters();
    if (separator != null) {
      sb.append(separator);
    }
    sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.URLS_MOVED),
                                          "URL"));
    if (ctrs.isNonZero(CounterType.URLS_SKIPPED)) {
      sb.append(" (");
      sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.URLS_SKIPPED), "URL"));
      sb.append(" skipped)");
    }
    sb.append(", ");
    sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.ARTIFACTS_MOVED),
                                          "version"));
    if (ctrs.isNonZero(CounterType.ARTIFACTS_SKIPPED)) {
      sb.append(" (");
      sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.ARTIFACTS_SKIPPED), "version"));
      sb.append(" skipped)");
    }
    sb.append(", ");
    sb.append(StringUtil.bigNumberOfUnits(ctrs.getVal(CounterType.BYTES_MOVED),
                                          "byte"));
    sb.append(", in ");
    sb.append(StringUtil.timeIntervalToString(getElapsedTime(phase)));
    if (ctrs.getVal(CounterType.BYTES_MOVED) > 0) {
      sb.append(", at ");
      sb.append(StringUtil.byteRateToString(ctrs.getVal(CounterType.BYTES_MOVED),
                                            getElapsedTime(phase)));

    }
    if (ctrs.getVal(CounterType.VERIFY_TIME) > 0 ||
        ctrs.getVal(CounterType.STATE_TIME) > 0) {
      double c = ctrs.getVal(CounterType.COPY_TIME);
      double v = ctrs.getVal(CounterType.VERIFY_TIME);
      double s = ctrs.getVal(CounterType.STATE_TIME);
      double t = c + v + s;
      double cp = Math.round(100*c/t);
      double vp = Math.round(100*v/t);
      double sp = Math.round(100*s/t);
      if (log.isDebug3()) {
        log.debug3("c: " + c + ", v: " + v + ", s: " + s + ", t: " + t +
                   ", cp: " + cp + ", vp: " + vp + ", sp: " + sp);
      }
      sb.append(" (");
      appendPhaseStats(sb, ctrs, phase, cp, CounterType.BYTES_MOVED, " copy");
      if (vp != 0.0) {
        if (cp != 0.0) {
          sb.append(", ");
        }
        appendPhaseStats(sb, ctrs, phase, vp, CounterType.BYTES_VERIFIED, " verify");
      }
      if (sp != 0.0) {
        if (cp + vp != 0.0) {
          sb.append(", ");
        }
        sb.append(V2AuMover.percentFormat(sp));
        sb.append("% state");
      }
      sb.append(")");
    }
  }

  void appendPhaseStats(StringBuilder sb, Counters ctrs, Phase phase,
                        double percent, CounterType ct, String name) {
    if (percent != 0.0) {
      sb.append(V2AuMover.percentFormat(percent));
      sb.append("%");
      sb.append(name);
      if (ctrs.getVal(ct) > 0) {
        long ms = (long)(getElapsedTime(phase) * (percent / 100.0));
        if (ms > 0) {
          sb.append(" (");
          sb.append(StringUtil.byteRateToString(ctrs.getVal(ct), ms));
          sb.append(")");
        }
      }
    }
  }
}

