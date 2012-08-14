/*
 * $Id: V3Repairer.java,v 1.1 2012-08-14 21:27:13 barry409 Exp $
 */

/*

Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.poller.v3;

import org.lockss.app.LockssDaemon;
import org.lockss.protocol.psm.PsmMachine;
import org.lockss.protocol.LcapMessage;
import org.lockss.protocol.V3LcapMessage;
import org.lockss.util.Logger;


/**
 */
public class V3Repairer extends V3Voter {
  // todo(bhayes): Inheriting from V3Voter is wrong, but
  // convenient. Various routines in PollManager and elsewhere do
  // instanceof on BasePoll objects to figure out what to do with
  // them.

  private static final Logger log = Logger.getLogger("V3Repairer");

  /**
   * Return true iff this is a valid starting message.
   */
  public static boolean canStart(LcapMessage msg) {
    return msg.getOpcode() == V3LcapMessage.MSG_REPAIR_REQ;
  }

  // todo(bhayes): This should not extend V3Voter, as that drags in a
  // vast amount of mechanism we have to be sure not to invoke.
  public V3Repairer(LockssDaemon daemon, V3LcapMessage msg)
      throws V3Serializer.PollSerializerException {
    
    super(daemon, msg);
    log.debug("Hey, made a repairer");
  }

  /**
   * <p>Start the V3Repairer running and serve repairs.  Called by
   * {@link org.lockss.poller.v3.V3PollFactory} when a repair request message
   * has been received, and by {@link org.lockss.poller.PollManager} when
   * restoring serialized voters. XXX or maybe not.</p>
   */
  @Override public void startPoll() {
    log.debug("Starting repairer " + getKey());

    resumeOrStartStateMachine();
  }

  @Override public void stopPoll(final int status) {
    log.debug("Stopping V3Repairer: "+status);
    super.stopPoll(status);
  }

  // Allow serializing?
  // todo(bhayes): Need some way to call stopPoll.

  private Class getRepairerActionsClass() {
    return RepairerActions.class;
  }

  @Override protected PsmMachine makeMachine() {
    try {
      PsmMachine.Factory fact = RepairerStateMachineFactory.class.newInstance();
      return fact.getMachine(getRepairerActionsClass());
    } catch (Exception e) {
      String msg = "Can't create voter state machine";
      log.critical(msg, e);
      throw new RuntimeException(msg, e);
    }
  }

}