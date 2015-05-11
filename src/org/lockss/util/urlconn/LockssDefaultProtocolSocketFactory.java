/*
 * $Id: LockssDefaultProtocolSocketFactory.java 39864 2015-02-18 09:10:24Z thib_gc $
 *

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util.urlconn;

import java.io.*;
import java.net.*;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;
import org.lockss.util.*;

/**
 * Extension of DefaultProtocolSocketFactory to set SO_KEEPALIVE, which
 * isn't handled by HttpConnection
 */
public class LockssDefaultProtocolSocketFactory
  extends DefaultProtocolSocketFactory {

  /**
   * The factory singleton.
   */
  private static final LockssDefaultProtocolSocketFactory factory =
    new LockssDefaultProtocolSocketFactory();
    
  /**
   * Returns singleton instance of the LockssDefaultProtocolSocketFactory.
   * @return a LockssDefaultProtocolSocketFactory
   */
  public static LockssDefaultProtocolSocketFactory getSocketFactory() {
    return factory;
  }
    
  public LockssDefaultProtocolSocketFactory() {
    super();
  }

  /**
   * This is the only factory method that handles params, thus the only one
   * we need to override.
   * @param host the host name/IP
   * @param port the port on the host
   * @param localAddress the local host name/IP to bind the socket to
   * @param localPort the local port to bing the socket to
   * @param params {@link HttpConnectionParams Http connection parameters}
   * 
   * @return Socket a new socket
   * 
   * @throws IOException if an I/O error occurs while creating the socket
   * @throws UnknownHostException if the IP address of the host cannot be
   * determined
   * @throws ConnectTimeoutException if socket cannot be connected within
   * the given time limit
   */
  public Socket createSocket(final String host, final int port,
			     final InetAddress localAddress,
			     final int localPort,
			     final HttpConnectionParams params)
      throws IOException, UnknownHostException, ConnectTimeoutException {
    if (params == null) {
      throw new IllegalArgumentException("Parameters may not be null");
    }
    Socket sock = new Socket();
    sock.bind(new InetSocketAddress(localAddress, localPort));
    sock.setKeepAlive(params.getBooleanParameter(HttpClientUrlConnection.SO_KEEPALIVE,
						 false));
    int timeout = params.getConnectionTimeout();
    if (timeout == 0) {
      sock.connect(new InetSocketAddress(host, port));
    } else {
      try {
	sock.connect(new InetSocketAddress(host, port), timeout);
      } catch (SocketTimeoutException e) {
	// Reproduce httpclient behavior - distinguish connect timeout from
	// data timeout
	String msg =
	  "The host did not accept the connection within timeout of " 
	  + timeout + " ms";
	throw new ConnectTimeoutException(msg, e);
      }
    }
    return sock;
  }

  /**
   * All instances of LockssDefaultProtocolSocketFactory are equal.
   * Something in HttpClient relies on this working this way
   */
  public boolean equals(Object obj) {
    return ((obj != null) && obj instanceof LockssDefaultProtocolSocketFactory);
  }

  /**
   * All instances of LockssDefaultProtocolSocketFactory are equal
   */
  public int hashCode() {
    return LockssDefaultProtocolSocketFactory.class.hashCode();
  }
}
