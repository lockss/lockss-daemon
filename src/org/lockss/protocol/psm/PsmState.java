/*
* $Id: PsmState.java,v 1.1 2005-02-23 02:19:04 tlipkis Exp $
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
 * action and array of event responses.
 */
public class PsmState {
  public static final PsmResponse[] EMPTY_RESPONSE_ARRAY = new PsmResponse[0];

  private String name;
  private PsmAction entryAction;
  private PsmResponse[] responses;
  private boolean isFinal = false;

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

  /** Create a state with no responses.  This state can never be left, so
   * makes sense only as a final state.
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

  /** Create a state with siz responses.
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

  /** Create a state with no responses and no entry action.  This state can
   * never be left, so makes sense only as a final state.
   * @param name state name
   * This action is not considered to have a causitive event.
   */
  public PsmState(String name) {
    this(name, null, EMPTY_RESPONSE_ARRAY);
  }

  /** Create a state.
   * @param name state name
   * @param responses array of responses
   */
  public PsmState(String name, PsmResponse[] responses) {
    this(name, null, responses);
  }

  /** Create a state with a single response.
   * @param name state name
   * @param response1 the single response
   */
  public PsmState(String name, PsmResponse response1) {
    this(name, null, responseArray(ListUtil.list(response1)));
  }

  /** Create a state with two responses.
   * @param name state name
   * @param response1 the first response
   * @param response2 the second response
   */
  public PsmState(String name, PsmResponse response1, PsmResponse response2) {
    this(name, null, responseArray(ListUtil.list(response1, response2)));
  }

  /** Create a state with three responses.
   * @param name state name
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   */
  public PsmState(String name,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3) {
    this(name, null, responseArray(ListUtil.list(response1, response2,
						 response3)));
  }

  /** Create a state with four responses.
   * @param name state name
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   */
  public PsmState(String name,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4) {
    this(name, null, responseArray(ListUtil.list(response1, response2,
						 response3, response4)));
  }

  /** Create a state with five responses.
   * @param name state name
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   * @param response5 the fifth response
   */
  public PsmState(String name,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4,
		  PsmResponse response5) {
    this(name, null, responseArray(ListUtil.list(response1, response2,
						 response3, response4,
						 response5)));
  }

  /** Create a state with siz responses.
   * @param name state name
   * @param response1 the first response
   * @param response2 the second response
   * @param response3 the third response
   * @param response4 the fourth response
   * @param response5 the fifth response
   * @param response6 the sixth response
   */
  public PsmState(String name,
		  PsmResponse response1, PsmResponse response2,
		  PsmResponse response3, PsmResponse response4,
		  PsmResponse response5, PsmResponse response6) {
    this(name, null, responseArray(ListUtil.list(response1, response2,
						 response3, response4,
						 response5, response6)));
  }

  private void validate() {
    if (name == null)
      throw new PsmException.IllegalStateMachine("name is null");
    for (int ix = 0; ix < responses.length; ix++) {
      if (responses[ix] == null) {
	throw new PsmException.IllegalStateMachine("Response array contains null(s)");
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

  /** Makes this state a final state.  Returns this so can be chained. */
  public PsmState setFinal() {
    isFinal = true;
    return this;
  }

  /** Returns true iff this is a final state. */
  public boolean isFinal() {
    return isFinal;
  }

  public String toString() {
    return "[State: " + getName() + "]";
  }

  // prototype for List.toArray() calls
  static final PsmResponse[] EMPTY_PSM_RESPONSE_ARRAY = new PsmResponse[0];

  static PsmResponse[] responseArray(List lst) {
    return (PsmResponse[])lst.toArray(EMPTY_PSM_RESPONSE_ARRAY);
  }
}
