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
import org.lockss.protocol.*;

/**
 * Top of hierarchy of incoming message events.  Extends PsmEvent with a
 * slot for the received message.
 */
public class PsmMsgEvent extends PsmEvent {
  private LcapMessage receivedMessage;

  public PsmMsgEvent() {
  }

  /** Create a message event holding the received message */
  public PsmMsgEvent(LcapMessage receivedMessage) {
    this.receivedMessage = receivedMessage;
  }

  public LcapMessage getMessage() {
    return receivedMessage;
  }

  /** This is the normal way to create a message event holding an incoming
   * message: it returns a copy of the object with the message set to
   * the argument.  Is essentially a constructor, done this way because
   * events are usually referred to by name as prototypical instances.
   * Also avoids each event class needing explicit constructors.
   */
  public PsmMsgEvent withMessage(LcapMessage msg) {
    PsmMsgEvent res = (PsmMsgEvent)copy();
    res.setMessage(msg);
    return res;
  }

  private void setMessage(LcapMessage receivedMessage) {
    this.receivedMessage = receivedMessage;
  }

  /** Look up the message opcode in the supplied map to find an prototype
   * instance of (a subclass of) PsmMsgEvent, and return a new event of the
   * same class, holding the supplied message.  If the opcode is not in the
   * map, the returned event will be an instance of PsmMsgEvent itself.
   * @param msg an incoming message
   * @param opcodeMap Map from integer opcodes to prototype instance of
   * appripriate PsmMsgEvent suclass.
   */
  public static PsmMsgEvent fromMessage(V3LcapMessage msg, Map opcodeMap) {
    PsmMsgEvent inst =
      (PsmMsgEvent)opcodeMap.get(new Integer(msg.getOpcode()));
    if (inst == null) {
      return new PsmMsgEvent(msg);
    }
    return inst.withMessage(msg);
  }
}
