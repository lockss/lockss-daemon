/*
 * $Id: PeerIdentity.java,v 1.11 2008-11-26 01:10:53 edwardsb1 Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.*;
import org.lockss.util.*;

/**
 * PeerIdentity is an opaque "cookie" that the IdentityManager
 * hands out to clients.  It uniquely specifies a peer identity
 * so that == can be used to test for peer equality.
 * @author David Rosenthal
 * @version 1.0
 */
public class PeerIdentity implements LockssSerializable {
  static Logger theLog=Logger.getLogger("PeerIdentity");
  protected String key;
  protected transient PeerAddress pAddr;

  PeerIdentity(String newKey)
      throws IdentityManager.MalformedIdentityKeyException {
    key = newKey;
    pAddr = PeerAddress.makePeerAddress(key);
  }

  // Here only for Mock subclass
  PeerIdentity() {
  }

  /**
   * toString results in a string describing the peer that is
   * understandable to humans.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    if (isLocalIdentity()) sb.append("L");
    sb.append("Peer: ");
    sb.append(key);
//     sb.append(", ");
//     sb.append(System.identityHashCode(this));
    sb.append("]");
    return sb.toString();
  }
  
  /**
   * getIdString returns a string describing the peer that is
   * parseable and convertable into a PeerIdentity object.  At
   * present this is a dotted-quad IP address optionally followed
   * by a colon and a numeric port number
   */
  public String getIdString() {
    return key;
  }

  public PeerAddress getPeerAddress() {
    return pAddr;
  }

  void setPeerAddress(PeerAddress addr) {
    this.pAddr = addr;
  }

  /** If this PeerIdentity has an associated IP address, return it.
      Violates encapsulation of PeerIdentity and PeerAddress. */
  public IPAddr getIPAddr() {
    if (pAddr instanceof PeerAddress.Ip) {
      return ((PeerAddress.Ip)pAddr).getIPAddr();
    }
    return null;
  }

  /** Return a best guess at the URL at which to reach this peer's UI. */
  public String getUiUrlStem(int defaultPort) {
    IdentityManager idm =
      (IdentityManager)LockssDaemon.getManager(LockssDaemon.IDENTITY_MANAGER);
    if (idm != null) {
      String stem = idm.getUiUrlStem(this);
      if (stem != null) {
	return stem;
      }
    }
    IPAddr addr = getIPAddr();
    if (addr != null) {
      return "http://" + addr + ":" + defaultPort;
    }
    return null;
  }

  /** Return true iff this is a local PeerIdentity.
   */
  public boolean isLocalIdentity() {
    return false;
  }

  /**
   * <p>Automatically substitutes the right singleton identity after
   * deserialization.</p>
   * @param lockssContext A {@link LockssApp} context object.
   */
  protected Object postUnmarshalResolve(LockssApp lockssContext) {
    IdentityManager idm =
      (IdentityManager)lockssContext.getManagerByKey(LockssDaemon.IDENTITY_MANAGER);
    try {
      return idm.findPeerIdentity(key);
    } catch (IdentityManager.MalformedIdentityKeyException e) {
      theLog.error("Bad serialized peer id: " + key, e);
      return null;
    }
  }
  
  static class LocalIdentity extends PeerIdentity {
    LocalIdentity(String key)
	throws IdentityManager.MalformedIdentityKeyException {
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
