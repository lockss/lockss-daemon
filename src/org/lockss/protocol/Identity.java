/*
 * $Id: Identity.java,v 1.1 2002-10-02 15:41:43 claire Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

import java.net.*;

/**
 * quick and dirty wrapper class for a network identity.
 * Should this class implement <code>PeerIdentity</code>?
 * @author Claire Griffin
 * @version 1.0
 */
public class Identity {

  private InetAddress m_address;
  private int m_port;
  private static Identity theLocalIdentity;

  /**
   * construct a new Identity from the information found in
   * a datagram socket
   * @param socket
   */
  Identity(DatagramSocket socket) {
    m_address = socket.getInetAddress();
    m_port = socket.getPort();
  }

  /**
   * construct a new Identity from an address and port
   * @param addr the InetAddress
   * @param port the port
   */
  Identity(InetAddress addr, int port) {
    m_address = addr;
    m_port = port;
  }

  /**
   * public constructor for creation of an Identity object
   * from a DatagramSocket
   * @param socket the DatagramSocket
   * @return newly constructed <code>Identity<\code>
   */
  public static Identity getIdentity(DatagramSocket socket) {
    return new Identity(socket);
  }

  /**
   * public constructor for the creation of an Identity object
   * from an address and port.
   * @param addr the InetAddress
   * @param port the port id
   * @return a newly constructed Identity
   */
  public static Identity getIdentity(InetAddress addr, int port) {
    return new Identity(addr,port);
  }

  /**
   * public constructor for the creation of an Identity object that
   * represents the local address and port
   * @param socket the DatagramSocket used to extract the local info.
   * @return a newly constructed Identity
   */
  public static Identity getLocalIdentity(DatagramSocket socket) {
    if(theLocalIdentity == null) {
      theLocalIdentity = new Identity(socket.getLocalAddress(),
                                      socket.getLocalPort());
    }
    return theLocalIdentity;
  }

  public boolean isLocalIdentity() {
    if(theLocalIdentity != null) {
      return isEqual(theLocalIdentity);
    }
    return false;
  }

  public boolean isEqual(Identity id) {
    if(m_port == id.getPort()) {
      if(id.getAddress().equals(m_address)) {
        return true;
      }
    }
    return false;
  }

  // accessor methods
  public InetAddress getAddress() {
    return m_address;
  }

  public int getPort() {
    return m_port;
  }
}