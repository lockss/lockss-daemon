/*
* $Id: PsmEvent.java,v 1.1 2005-02-23 02:19:05 tlipkis Exp $
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
 * currently <i>isA</i> is determined by class hierarchy.
 */
public class PsmEvent {

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

}
