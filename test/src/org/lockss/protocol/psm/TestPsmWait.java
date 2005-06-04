/*
 * $Id: TestPsmWait.java,v 1.1 2005-06-04 21:37:12 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol.psm;

import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestPsmWait extends LockssTestCase {

  PsmState[] states1 = {
    new PsmState("Start")
  };


  public void testConst() {
    PsmWait w = new PsmWait();
    assertTrue(w.isWaitAction());
  }

  public void testRun() {
    final long calcTime = 54321;
    PsmWait w = new PsmWait() {
	protected long calculateTimeout(PsmEvent event, PsmInterp interp) {
	  return calcTime;
	}};
    TimeoutRecordingInterp interp =
      new TimeoutRecordingInterp(new PsmMachine("Test1", states1 , "Start"),
				 null);

    PsmEvent e = w.run(PsmEvents.Start, interp);
    assertTrue(e instanceof PsmWaitEvent);
    assertEquals(calcTime, ((PsmWaitEvent)e).getTimeout());
  }

  public void testTimeoutInTrigger() {
    PsmWait w = PsmWait.TIMEOUT_IN_TRIGGER;
    TimeoutRecordingInterp interp =
      new TimeoutRecordingInterp(new PsmMachine("Test1", states1 , "Start"),
				 null);
    PsmEvent trigger = new PsmEvent().withUserVal(222333);
    PsmEvent e = w.run(trigger, interp);
    assertTrue(e instanceof PsmWaitEvent);
    assertEquals(222333, ((PsmWaitEvent)e).getTimeout());
  }

  class TimeoutRecordingInterp extends PsmInterp {
    List durs = new ArrayList();
    TimeoutRecordingInterp(PsmMachine stateMachine, Object userData) {
      super(stateMachine, userData);
    }
    void setCurrentStateTimeout(long duration) {
      durs.add(new Long(duration));
    }
  }
}
