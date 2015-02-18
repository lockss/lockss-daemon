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

/**
 * PsmResponse maps an event to a response, which is either an action or a
 * the name of new state to which to transition.  Responses match all
 * events that are subsumed by the response's event, so the order in which
 * they appear in a state's response array is important.
 */
public class PsmResponse {
  private PsmEvent event;
  private String newState;
  private PsmAction action;

  /** Create a response that maps the event to the named new state.
   * @param event the pattern event against which incoming events are matched
   * @param newState name of state to transition to if matching event is
   * signalled.
   */
  public PsmResponse(PsmEvent event, String newState) {
    if (event == null) {
      throw new PsmException.IllegalStateMachine("event is null");
    }
    if (StringUtil.isNullString(newState)) {
      throw new PsmException.IllegalStateMachine("newState is null string");
    }
    this.event = event;
    this.newState = newState;
  }

  /** Create a response that maps the event to the specified action.
   * @param event the pattern event against which incoming events are matched
   * @param action the action to perform if matching event is signalled.
   */
  public PsmResponse(PsmEvent event, PsmAction action) {
    if (event == null)
      throw new PsmException.IllegalStateMachine("event is null");
    if (action == null)
      throw new PsmException.IllegalStateMachine("action is null");
    this.event = event;
    this.action = action;
  }

  /** Return the event against which incoming events are matched */
  public PsmEvent getEvent() {
    return event;
  }

  /** Return true if the reponse to the event is to transition to a new
   * state */
  public boolean isTransition() {
    return newState != null;
  }

  /** Return true if the reponse to the event is to perform an action */
  public boolean isAction() {
    return action != null;
  }

  /** Return the new state, iff a transition response */
  public String getNewState() {
    return newState;
  }

  /** Return the action, iff an action response */
  public PsmAction getAction() {
    return action;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[Resp: ");
    sb.append(event);
    sb.append(" -> ");
    if (isAction()) {
      sb.append(action);
    } else if (isTransition()) {
      sb.append(newState);
    } else {
      sb.append("[???]");
    }
    sb.append("]");
    return sb.toString();
  }
}
