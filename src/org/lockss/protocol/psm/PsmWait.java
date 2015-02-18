/*
* $Id$
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * An "action" that causes the machine to wait for another event.  Various
 * methods of computing a timeout are available
 */
public class PsmWait extends PsmAction {

  /** PsmAction instance that causes interpreter to wait, with no timrout */
  public static final PsmWait FOREVER = new PsmWait();

  /** PsmAction instance that causes interpreter to set a timeout from the
   * userVal in the trigger event, then wait */
  public static final PsmWait TIMEOUT_IN_TRIGGER = new PsmWait() {
      protected long calculateTimeout(PsmEvent triggerEvent,
				      PsmInterp interp) {
	return triggerEvent.getUserVal();
      }};

  private long defaultTimeout = 0;

  /** Create a PsmWait action with no timeout.  An instance of this is in
   * PsmWait.FOREVER, so there is usually no reason to call this
   * constructor. */
  public PsmWait() {
    super();
  }

  /** Create an action that sets the specified timeout, then waits */
  public PsmWait(long defaultTimeout) {
    super();
    this.defaultTimeout = defaultTimeout;
  }

  /** Returns a PsmWaitEvent with the timeout, if any.  To compute a
   * timeout, override {@link #calculateTimeout(PsmEvent, PsmInterp)}. */
  protected final PsmEvent run(PsmEvent triggerEvent, PsmInterp interp) {
    long timeout = calculateTimeout(triggerEvent, interp);
    if (timeout > 0) {
      return new PsmWaitEvent(timeout);
    }
    return PsmWaitEvent.FOREVER;
  }

  /** Default behavior is to return the value passed to the constructor;
   * override to perform timeout calculation at runtime
   */
  protected long calculateTimeout(PsmEvent triggerEvent, PsmInterp interp) {
    return defaultTimeout;
  }

  public String toString() {
    return "[Wait]";
  }
}
