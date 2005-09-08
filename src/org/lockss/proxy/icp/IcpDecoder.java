/*
 * $Id: IcpDecoder.java,v 1.3 2005-09-08 01:24:41 thib_gc Exp $
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

/**
 * <p>Defines a simple abstraction for classes that are able to
 * parse a {@link DatagramPacket} into an {@link IcpMessage}.</p>
 * @author Thib Guicherd-Callin
 * @see Factory
 * @see IcpMessage
 */
public interface IcpDecoder {

  /*
   * begin NESTED INTERFACE
   * ======================
   */
  /**
   * <p>An abstraction for classes that need to obtain ICP decoders.</p>
   * @author Thib Guicherd-Callin
   * @see IcpDecoder
   */
  public interface Factory {

    /**
     * <p>Builds a new ICP decoder.</p>
     * @return An object conforming to {@link IcpDecoder}.
     */
    IcpDecoder makeIcpDecoder();
    
  }
  /*
   * end NESTED INTERFACE
   * ====================
   */
  
  /**
   * <p>Translates the given UDP packet into an {@link IcpMessage}
   * instance.</p>
   * @param packet A UDP packet.
   * @return An ICP message <code>m</code> representing the data in
   *         the packet, such that
   *         <code>m.getUdpAddress() == packet.getAddress()</code>
   *         and
   *         <code>m.getUdpPort() == packet.getPort()</code>.
   * @throws IcpProtocolException if the packet is malformed.
   * @see IcpMessage#getUdpAddress
   * @see IcpMessage#getUdpPort
   * @see IcpMessage#setUdpAddress
   * @see IcpMessage#setUdpPort
   */
  IcpMessage parseIcp(DatagramPacket packet) throws IcpProtocolException;

}
