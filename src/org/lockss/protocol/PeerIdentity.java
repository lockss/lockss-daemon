/*
 * $Id: PeerIdentity.java,v 1.3 2004-09-29 06:39:14 tlipkis Exp $
 */

/*

Copyright (c) 2004 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.protocol;

import java.util.*;
import org.lockss.util.*;

/**
 * PeerIdentity is an opaque "cookie" that the IdentityManager
 * hands out to clients.  It uniquely specifies a peer identity
 * so that == can be used to test for peer equality.
 * @author David Rosenthal
 * @version 1.0
 */
public class PeerIdentity {
  static Logger theLog=Logger.getLogger("PeerIdentity");
  private String key;

  PeerIdentity(String newKey) {
    key = newKey;
  }

  /**
   * toString results in a string describing the peer that is
   * understandable to humans.
   */
  public String toString() {
    return "[Peer: " + key + "]";
  }

  /**
   * getIdString returns a string describing the peer that is
   * parseable and convertable into a PeerIdentity object.  At
   * present this is a dotted-quad IP address optionally followed
   * by a colon and a nuberic port number
   */
  public String getIdString() {
    return key;
  }

  /** Return true iff this is a local PeerIdentity.
   */
  public boolean isLocalIdentity() {
    return false;
  }

  static class LocalIdentity extends PeerIdentity {
    LocalIdentity(String key) {
      super(key);
    }

    /** Return true iff this is a local PeerIdentity
     * @return true
     */
    public boolean isLocalIdentity() {
      return true;
    }
  }
}
