/*
* $Id: PsmState.java,v 1.9 2006-01-12 03:13:30 smorabito Exp $
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
 * Defines a single state in a state machine.  Has a name, optional entry
 * action and array of event responses.  Final states are defined as those
 * with no responses.
 */
public class PsmState {
  public static final PsmResponse[] EMPTY_RESPONSE_ARRAY = new PsmResponse[0];

  private String name;
  private PsmAction entryAction;
  private PsmResponse[] responses;
  private boolean resumable = false;
  private int isSucceed = NEITHER;
  private static final int NEITHER = 0;
  private static final int SUCCEED = 1;
  private static final int FAIL = 2;

  /** Create a state.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param responses array of responses
   */
  // all other constructors call this
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse[] responses) {
    this.name = name;
    this.entryAction = entryAction;
    this.responses = responses;
    validate();
  }

  /** Create a state with no responses.  This state is a final state.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   */
  public PsmState(String name, PsmAction entryAction) {
    this(name, entryAction, EMPTY_RESPONSE_ARRAY);
  }

  /** Create a state with a single response.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the single response
   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1) {
    this(name, entryAction, responseArray(ListUtil.list(response1)));
  }

  /** Create a state with two responses.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the first response
   * @param response2 the second response
   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1, PsmResponse response2) {
    this(name, entryAction,
	 responseArray(ListUtil.list(response1, response2)));
  }

  /** Create a state with three responses.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3) {
    this(name, entryAction,
	 responseArray(ListUtil.list(response1, response2,
				     response3)));
  }

  /** Create a state with four responses.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4) {
    this(name, entryAction,
	 responseArray(ListUtil.list(response1, response2,
				     response3, response4)));
  }

  /** Create a state with five responses.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   * @param response5 the fifth response

   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4,
		  PsmResponse response5) {
    this(name, entryAction,
	 responseArray(ListUtil.list(response1, response2,
				     response3, response4,
				     response5)));
  }

  /** Create a state with six responses.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   * @param response5 the fifth response
   * @param response6 the sixth response
   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4,
		  PsmResponse response5, PsmResponse response6) {
    this(name, entryAction,
	 responseArray(ListUtil.list(response1, response2,
				     response3, response4,
				     response5, response6)));
  }

  /** Create a state with seven responses.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   * @param response5 the fifth response
   * @param response6 the sixth response
   * @param response7 the seventh response
   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4,
		  PsmResponse response5, PsmResponse response6,
		  PsmResponse response7) {
    this(name, entryAction,
	 responseArray(ListUtil.list(response1, response2,
				     response3, response4,
				     response5, response6,
				     response7)));
  }
  
  /** Create a state with eight responses.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   * @param response5 the fifth response
   * @param response6 the sixth response
   * @param response7 the seventh response
   * @param response8 the eighth response
   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4,
		  PsmResponse response5, PsmResponse response6,
		  PsmResponse response7, PsmResponse response8) {
    this(name, entryAction,
	 responseArray(ListUtil.list(response1, response2,
				     response3, response4,
				     response5, response6,
				     response7, response8)));
  }

  /** Create a state with nine responses.
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   * @param response5 the fifth response
   * @param response6 the sixth response
   * @param response7 the seventh response
   * @param response8 the eighth response
   * @param response9 the nineth response   
   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4,
		  PsmResponse response5, PsmResponse response6,
		  PsmResponse response7, PsmResponse response8,
		  PsmResponse response9) {
    this(name, entryAction,
	 responseArray(ListUtil.list(response1, response2,
				     response3, response4,
				     response5, response6,
				     response7, response8,
				     response9)));
  }

  /** Create a state with ten responses.  (I would kill for varargs about now)
   * @param name state name
   * @param entryAction action to be performed upon entry into the state.
   * This action is not considered to have a causitive event.
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   * @param response5 the fifth response
   * @param response6 the sixth response
   * @param response7 the seventh response
   * @param response8 the eighth response
   * @param response9 the nineth response   
   * @param response10 the tenth response   
   */
  public PsmState(String name, PsmAction entryAction,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4,
		  PsmResponse response5, PsmResponse response6,
		  PsmResponse response7, PsmResponse response8,
		  PsmResponse response9, PsmResponse response10) {
    this(name, entryAction,
	 responseArray(ListUtil.list(response1, response2,
				     response3, response4,
				     response5, response6,
				     response7, response8,
				     response9, response10)));
  }
    
  /** Create a state with no responses and no entry action.  This state is
   * a final state.
   * @param name state name
   * This action is not considered to have a causitive event.
   */
  public PsmState(String name) {
    this(name, PsmWait.FOREVER, EMPTY_RESPONSE_ARRAY);
  }

  private void validate() {
    if (name == null)
      throw new PsmException.IllegalStateMachine("name is null");
    for (int ix = 0; ix < responses.length; ix++) {
      PsmResponse resp = responses[ix];
      if (resp == null) {
	throw new PsmException.IllegalStateMachine("Response array contains null(s)");
      }
      PsmEvent event = resp.getEvent();
      if (event.isWaitEvent()) {
	String msg = "Not allowed to respond to wait events: " + event;
	throw new PsmException.IllegalStateMachine(msg);
      }
      for (int iy = 0; iy < ix; iy++) {
	if (event.isa(responses[iy].getEvent())) {
	  String msg = "State " + getName() + ": Response " + ix + " (" +
	    resp + ") subsumed by response " + iy + " (" + responses[iy] + ")";
	  throw new PsmException.IllegalStateMachine(msg);
	}
      }
    }
  }

  /** Return the name of the state */
  public String getName() {
    return name;
  }

  /** Return the state's entry action, or null if none. */
  public PsmAction getEntryAction() {
    return entryAction;
  }

  /** Return the state's response array */
  public PsmResponse[] getResponses() {
    return responses;
  }

  /** Find the first response that matches the event */
  public PsmResponse getResponse(PsmEvent event) {
    for (int ix = 0; ix < responses.length; ix++) {
      PsmResponse resp = responses[ix];
      if (event.isa(resp.getEvent())) {
	return resp;
      }
    }
    return null;
  }

  /** Returns true iff this is a final state (<i>Ie</i>, it has no
   * responses). */
  public boolean isFinal() {
    return responses.length == 0;
  }

  /** Makes this state a success state.  Returns this so can be chained. */
  public PsmState succeed() {
    if (!isFinal())
      throw new PsmException.IllegalStateMachine("Non-final success state");
    isSucceed = SUCCEED;
    return this;
  }

  /** Makes this state a failure state.  Returns this so can be chained. */
  public PsmState fail() {
    if (!isFinal())
      throw new PsmException.IllegalStateMachine("Non-final failure state");
    isSucceed = FAIL;
    return this;
  }

  /** Returns true iff this is a success final state. */
  public boolean isSucceed() {
    return isSucceed == SUCCEED;
  }

  /** Returns true iff this is a failure final state. */
  public boolean isFail() {
    return isSucceed == FAIL;
  }

  public String toString() {
    return "[State: " + getName() + "]";
  }

  public boolean isResumable() {
    return resumable;
  }

  public PsmState setResumable(boolean flag) {
    this.resumable = flag;
    return this;
  }

  // prototype for List.toArray() calls
  static final PsmResponse[] EMPTY_PSM_RESPONSE_ARRAY = new PsmResponse[0];

  static PsmResponse[] responseArray(List lst) {
    return (PsmResponse[])lst.toArray(EMPTY_PSM_RESPONSE_ARRAY);
  }
}
