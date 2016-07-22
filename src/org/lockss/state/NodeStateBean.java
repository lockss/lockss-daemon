/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.state;

import java.util.*;
import org.lockss.util.LockssSerializable;

/**
 * NodeStateBean is a settable version of the NodeState, to allow marshalling.
 */
public class NodeStateBean implements LockssSerializable {
  List pollBeans = new ArrayList();
  int curState;
  long hashDuration = -1;

  /**
   * Simple constructor to allow bean creation during unmarshalling.
   */
  public NodeStateBean() { }

  /**
   * Constructor to create the NodeStateBean from a NodeState prior to
   * marshalling.
   * @param nodeState the NodeState
   */
  NodeStateBean(NodeState nodeState) {
    Iterator polls = nodeState.getActivePolls();
    hashDuration = nodeState.getAverageHashDuration();
    curState = nodeState.getState();
  }

  /**
   * Returns a list of PollStateBeans.
   * @return a list of {@link PollStateBean} objects
   */
  public List getPollBeans() {
    return pollBeans;
  }

  /**
   * Sets the list of PollStateBeans.
   * @param newList a new list of {@link PollStateBean} objects
   */
  public void setPollBeans(List newList) {
    pollBeans = newList;
  }

  /**
   * Returns the last hash duration.
   * @return the last duration
   */
  public long getAverageHashDuration() {
    return hashDuration;
  }

  /**
   * Sets the last hash duration.
   * @param newDuration the new duration
   */
  public void setAverageHashDuration(long newDuration) {
    hashDuration = newDuration;
  }

  /**
   * Returns the state.
   * @return the state
   */
  public int getState() {
    return curState;
  }

  /**
   * Sets the state.
   * @param newState the state
   */
  public void setState(int newState) {
    curState = newState;
  }

}
