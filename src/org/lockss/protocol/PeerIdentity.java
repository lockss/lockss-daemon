/*
 * $Id: PeerIdentity.java,v 1.1 2004-09-20 14:20:37 dshr Exp $
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

import java.io.Serializable;
import java.util.*;
import org.lockss.util.*;

/**
 * PeerIdentity is an opaque "cookie" that the IdentityManager
 * hands out to clients.  It uniquely specifies a peer identity
 * so that == can be used to test for peer equality.
 * @author David Rosenthal
 * @version 1.0
 */
public class PeerIdentity implements Serializable {
  static Logger theLog=Logger.getLogger("PeerIdentity");
  private static HashMap instances = new HashMap();
  private String key;
  private PeerIdentity(String newKey) {
    key = newKey;
    instances.put(key, this);
  }

  /**
   * ipAddrToIdentity returns the instance representing this peer's
   * identity, creating it if necessary.  This method should be used
   * only by the IdentityManager.
   * @param addr the IPAddr of the peer
   * @param port the port of the peer
   * @return the PeerIdentity of the peer
   */
  protected static PeerIdentity ipAddrToIdentity(IPAddr addr, int port) {
    String newKey = addr.toString() +
      ( port == 0 ? "" : ":" + String.valueOf(port));

    return stringToIdentity(newKey);
  }

  /**
   * stringToIdentity returns the instance representing this peer's
   * identity, creating it if necessary.  This method should be used
   * only by the IdentityManager.
   * @param newKey the String representing the IP address and optional port
   * @return the PeerIdentity of the peer
   */
  protected static synchronized PeerIdentity stringToIdentity(String newKey) {
    if (instances.containsKey(newKey)) {
      return (PeerIdentity)instances.get(newKey);
    }
    return new PeerIdentity(newKey);
  }


  /**
   * toString results in a string describing the peer that is
   * understandable to humans.
   */
  public String toString() {
    return "[Peer: " + key + "]";
  }

  /**
   * getIdString results in a string describing the peer that is
   * parseable and convertable into a PeerIdentity object.  At
   * present this is a dotted-quad IP address optionally followed
   * by a colon and a numberic port number
   */
  public String getIdString() {
    return key;
  }

  public boolean equals(Object obj) {
    boolean ret = false;
    if (obj instanceof PeerIdentity) {
      ret = (this == obj);
    }
    return ret;
  }

  public int hashCode() {
    return key.hashCode();
  }
}
