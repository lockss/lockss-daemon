/*
 * $Id$
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.IPAddr;

/**
 * <p>An abstraction for classes that are able to instantiate
 * ICP messages.</p>
 * @author Thib Guicherd-Callin
 * @see IcpMessage
 */
public interface IcpFactory {

  /**
   * <p>Produces an ICP message from a received UDP packet.</p>
   * @param udpPacket A UDP packet.
   * @return An ICP message based on the UDP packet.
   * @throws IcpException if the UDP packet cannot be parsed
   *                              into an ICP message.
   */
  IcpMessage makeMessage(DatagramPacket udpPacket)
      throws IcpException;

  /**
   * <p>Equivalent to calling
   * {@link #makeQuery(IPAddr, String, boolean, boolean)} with the
   * two boolean arguments being false.</p>
   * @param requesterAddress
   * @param query
   * @return A query message.
   * @see #makeQuery(IPAddr, String, boolean, boolean)
   */
  IcpMessage makeQuery(IPAddr requesterAddress,
                       String query);

  /**
   * <p>Produces an ICP query using the given URL, with optional
   * parameters.</p>
   * @param requesterAddress The address of the original requester.
   * @param query            A URL query.
   * @param requestSrcRtt    Request a source return time trip.
   * @param requestHitObj    Request a hit object.
   * @return A query message.
   * @see IcpMessage#getRequester
   */
  IcpMessage makeQuery(IPAddr requesterAddress,
                       String query,
                       boolean requestSrcRtt,
                       boolean requestHitObj);

}