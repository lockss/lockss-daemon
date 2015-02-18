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
import org.lockss.util.*;
import org.lockss.protocol.*;

/**
 * Top of hierarchy of state machine events.  Events are signalled by
 * incoming messages, timeouts, and (returned from) actions.  Each state
 * maps events to responses.  New events can be defined as subclesses;
 * currently <i>isA</i> is determined by class hierarchy.  Events may also
 * be used to communicate a small piece of data from one action (the event
 * creator) to a response action.
 */
public class PsmEvent implements Cloneable {
  private long userVal;

  /** Tests whether this event is a kind of the specified event.  Assumed
   * to be reflexive (<i>foo</i>.isa(<i>foo</i>) is always true.  Currently
   * defined by class hierarchy, but could be changed if needs to be a
   * lattice instead of a tree.
   * @param event the potentially subsuming event
   * @return true iff this is subsumed by <code>event</code>
   */
  public final boolean isa(PsmEvent event) {
    return event.getClass().isInstance(this);
  }

  public String toString() {
    return StringUtil.shortName(getClass());
  }

  /** Return true if this is a wait event.
   */
  final boolean isWaitEvent() {
    return this instanceof PsmWaitEvent;
  }

  /** Return true if this is a NoOp event.
   */
  final boolean isNoOpEvent() {
    return this instanceof PsmEvents.NoOp;
  }

  /** An uninterpreted long value.  Intended to be used to communicate a
   * timeout value from an action that initiates a long-running activity
   * (and which therefore likely knows how long that activity should take)
   * to a PsmWait.TIMEOUT_IN_TRIGGER action run in response (which is where
   * the timeout value is needed). Not to be confused with {@link
   * PsmWaitEvent#getTimeout()}, which actually causes the interpreter to
   * start the timer.
   */
  public long getUserVal() {
    return userVal;
  }

  /** This is the normal way to set the user value in an event: it returns
   * a copy of the object with the user value set to the argument.  Is
   * essentially a constructor, done this way because events are usually
   * referred to by name as prototypical instances.  Also avoids each event
   * class needing explicit constructors.
   * @see #getUserVal()
   */
  public PsmEvent withUserVal(long val) {
    PsmEvent res = copy();
    res.setUserVal(val);
    return res;
  }

  // not public so can't change existing event instances
  private void setUserVal(long val) {
    userVal = val;
  }

  public PsmEvent copy() {
    try {
      return (PsmEvent)super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e.toString());
    }
  }

}
