/*
 * $Id: IcpEncoder.java,v 1.2.2.1 2005-09-07 18:17:11 thib_gc Exp $
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

package org.lockss.proxy.icp;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * <p>Defines a simple abstraction for classes that are able to
 * encode an {@link IcpMessage} into a {@link DatagramPacket}.</p>
 * @author Thib Guicherd-Callin
 * @see Factory
 * @see IcpMessage
 */
public interface IcpEncoder {

  /**
   * <p>An abstraction for classes that need to obtain ICP encoders.</p>
   * @author Thib Guicherd-Callin
   * @see IcpEncoder
   */
  public interface Factory {

    /**
     * <p>Builds a new ICP encoder.</p>
     * @return An object conforming to {@link IcpEncoder}.
     */
    IcpEncoder makeIcpEncoder();
    
  }
  
  /**
   * <p>Equivalent to calling
   * {@link #encode(IcpMessage, InetAddress, int)} with the port
   * argument equal to {@link IcpMessage#ICP_PORT}.</p>
   * @param message
   * @param recipient
   * @return
   */
  DatagramPacket encode(IcpMessage message,
                        InetAddress recipient);
  
  /**
   * <p>Constructs a UDP packet to the given address and port, from
   * the given ICP message argument.</p>
   * @param message   An ICP message to translate into a packet.
   * @param recipient The destination IP.
   * @param port      The destination port.
   * @return A UDP packet <code>p</code> representing the message,
   *         such that <code>p.getAddress() == recipient</code> and
   *         <code>p.getPort() == port</code>.
   */
  DatagramPacket encode(IcpMessage message,
                        InetAddress recipient,
                        int port);
  
}
