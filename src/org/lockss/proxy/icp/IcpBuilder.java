/*
 * $Id: IcpBuilder.java,v 1.4 2005-09-08 01:24:41 thib_gc Exp $
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

import java.net.InetAddress;

/**
 * <p>Specifies an interface for classes that are able to produce
 * ICP queries, ICP responses and other ICP messages.</p>
 * @author Thib Guicherd-Callin
 * @see Factory
 * @see IcpMessage
 */
public interface IcpBuilder {

  /*
   * begin NESTED INTERFACE
   * ======================
   */
  /**
   * <p>An abstraction for classes that need to obtain ICP builders.</p>
   * @author Thib Guicherd-Callin
   * @see IcpBuilder
   */
  public interface Factory {

    /**
     * <p>Builds a new ICP builder.</p>
     * @return An object conforming to {@link IcpBuilder}.
     */
    IcpBuilder makeIcpBuilder();
    
  }
  /*
   * end NESTED INTERFACE
   * ====================
   */
  
  /**
   * <p>Produces a denied message in response to a query.</p>
   * @param query The ICP query.
   * @return A denied message in response to the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query.
   * @see IcpMessage#isQuery
   */
  IcpMessage makeDenied(IcpMessage query)
      throws IcpProtocolException;
  
  /**
   * <p>Produces an ICP source-echo message.</p>
   * @param query A URL query.
   * @return A source-echo message.
   */
  IcpMessage makeDiscoveryEcho(String query);

  /**
   * <p>Produces an error message in response to a query.</p>
   * @param query The ICP query.
   * @return An error message in response to the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query.
   * @see IcpMessage#isQuery
   */
  IcpMessage makeError(IcpMessage query)
      throws IcpProtocolException;

  /**
   * <p>Produces a hit response to a query.</p>
   * @param query The ICP query.
   * @return A hit response based on the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query.
   */
  IcpMessage makeHit(IcpMessage query)
      throws IcpProtocolException;
  
  /**
   * <p>Produces a hit response to a query, with the given source
   * return trip time.</p>
   * @param query          The ICP query.
   * @param srcRttResponse A source return trip time.
   * @return A hit response based on the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query, or if the query did not
   *                              request a source return trip time
   *                              response.
   */
  IcpMessage makeHit(IcpMessage query,
                     short srcRttResponse)
      throws IcpProtocolException;
  
  /**
   * <p>Produces a hit-object response to a query using the given
   * array of bytes.</p>
   * @param query         The ICP query.
   * @param payloadObject A payload as an array of bytes.
   * @return A hit-object response based on the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query.
   * @throws NullPointerException if payloadObject is null. 
   */
  IcpMessage makeHitObj(IcpMessage query,
                        byte[] payloadObject)
      throws IcpProtocolException;

  /**
   * <p>Produces a hit-object response to a query using the given
   * array of bytes, with the given source return trip time.</p>
   * @param query          The ICP query.
   * @param srcRttResponse A source return trip time.
   * @param payloadObject A payload as an array of bytes.
   * @return A hit response based on the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query, or if the query did not
   *                              request a source return trip time
   *                              response.
   * @throws NullPointerException if payloadObject is null. 
   */
  IcpMessage makeHitObj(IcpMessage query,
                        short srcRttResponse,
                        byte[] payloadObject)
      throws IcpProtocolException;

  /**
   * <p>Produces a miss response to a query.</p>
   * @param query The ICP query.
   * @return A miss response based on the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query.
   */
  IcpMessage makeMiss(IcpMessage query)
      throws IcpProtocolException;
  
  /**
   * <p>Produces a miss response to a query, with the given source
   * return trip time.</p>
   * @param query          The ICP query.
   * @param srcRttResponse A source return trip time.
   * @return A miss response based on the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query, or if the query did not
   *                              request a source return trip time
   *                              response.
   */
  IcpMessage makeMiss(IcpMessage query,
                      short srcRttResponse)
      throws IcpProtocolException;
  
  /**
   * <p>Produces a miss-no-fectch response to a query.</p>
   * @param query The ICP query.
   * @return A miss-no-fetch response based on the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query.
   */
  IcpMessage makeMissNoFetch(IcpMessage query)
      throws IcpProtocolException;

  /**
   * <p>Produces a miss-no-fetch response to a query, with the given
   * source return trip time.</p>
   * @param query          The ICP query.
   * @param srcRttResponse A source return trip time.
   * @return A miss response based on the query.
   * @throws IcpProtocolException if the argument message is not a
   *                              query, or if the query did not
   *                              request a source return trip time
   *                              response.
   */
  IcpMessage makeMissNoFetch(IcpMessage query,
                             short srcRttResponse)
      throws IcpProtocolException;

  /**
   * <p>Equivalent to calling
   * {@link #makeQuery(InetAddress, String, boolean, boolean)} with the
   * two boolean arguments being false.</p>
   * @param requesterAddress
   * @param query
   * @return A query message.
   * @see #makeQuery(InetAddress, String, boolean, boolean)
   */
  IcpMessage makeQuery(InetAddress requesterAddress,
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
  IcpMessage makeQuery(InetAddress requesterAddress,
                       String query,
                       boolean requestSrcRtt,
                       boolean requestHitObj);
  
  /**
   * <p>Produces an ICP source-echo message.</p>
   * @param query A URL query.
   * @return A source-echo message.
   */
  IcpMessage makeSourceEcho(String query);
  
}
