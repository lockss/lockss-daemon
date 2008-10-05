/*
* $Id: PsmEvents.java,v 1.3 2005-10-17 07:49:03 tlipkis Exp $
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
 * Collection of generally useful event classes, and instances of those
 * that carry no additional data
 */
public class PsmEvents {

  /** Subsumes all events */
  public final static PsmEvent Event = new PsmEvent();

  /** Synonym for {@link PsmEvents#Event}, more readable at end of list of
   * responses */
  public final static PsmEvent Else = Event;

  /** Event received by start state when machine is started */
  public final static PsmEvent Start = new Start();

  /** Event received by resumable state when machine is resumed */
  public final static PsmEvent Resume = new Resume();

  /** Generic error */
  public final static PsmEvent Error = new Error();

  /** Subevent of Error */
  public final static PsmEvent Timeout = new Timeout();

  /** Useful for <code><i>event</i>.isa(PsmEvents.MsgEvent)</code>, not to
      convey an actual event (becuase it has no message) */
  public final static PsmEvent MsgEvent = new PsmMsgEvent();

  private static class Start extends PsmEvent {}
  private static class Resume extends PsmEvent {}
  private static class Error extends PsmEvent {}
  private static class Timeout extends Error {}

}
