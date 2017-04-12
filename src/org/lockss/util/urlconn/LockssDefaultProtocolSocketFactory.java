/*

Copyright (c) 2000-2017 Board of Trustees of Leland Stanford Jr. University,
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

//HC3 import org.apache.commons.httpclient.ConnectTimeoutException;
//HC3 import org.apache.commons.httpclient.params.HttpConnectionParams;
//HC3 import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
//HC3 import org.lockss.util.*;

/**
 * Extension of DefaultProtocolSocketFactory to set SO_KEEPALIVE, which
 * isn't handled by HttpConnection
 */
public class LockssDefaultProtocolSocketFactory
//HC3   extends DefaultProtocolSocketFactory {
  extends PlainConnectionSocketFactory {

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

//HC3   /**
//HC3    * This is the only factory method that handles params, thus the only one
//HC3    * we need to override.
//HC3    * @param host the host name/IP
//HC3    * @param port the port on the host
//HC3    * @param localAddress the local host name/IP to bind the socket to
//HC3    * @param localPort the local port to bing the socket to
//HC3    * @param params {@link HttpConnectionParams Http connection parameters}
//HC3    * 
//HC3    * @return Socket a new socket
//HC3    * 
//HC3    * @throws IOException if an I/O error occurs while creating the socket
//HC3    * @throws UnknownHostException if the IP address of the host cannot be
//HC3    * determined
//HC3    * @throws ConnectTimeoutException if socket cannot be connected within
//HC3    * the given time limit
//HC3    */
//HC3   public Socket createSocket(final String host, final int port,
//HC3 			     final InetAddress localAddress,
//HC3 			     final int localPort,
//HC3 		             final HttpConnectionParams params)
//HC3       throws IOException, UnknownHostException, ConnectTimeoutException {
//HC3     if (params == null) {
//HC3       throw new IllegalArgumentException("Parameters may not be null");
//HC3     Socket sock = new Socket();
//HC3     sock.bind(new InetSocketAddress(localAddress, localPort));
//HC3     sock.setKeepAlive(params.getBooleanParameter(HttpClientUrlConnection.SO_KEEPALIVE,
//HC3 	                                         false));
//HC3     int timeout = params.getConnectionTimeout();
//HC3     if (timeout == 0) {
//HC3       sock.connect(new InetSocketAddress(host, port));
//HC3     } else {
//HC3       try {
//HC3 	sock.connect(new InetSocketAddress(host, port), timeout);
//HC3       } catch (SocketTimeoutException e) {
//HC3         // Reproduce httpclient behavior - distinguish connect timeout from
//HC3         // data timeout
//HC3         String msg =
//HC3           "The host did not accept the connection within timeout of " 
//HC3           + timeout + " ms";
//HC3         throw new ConnectTimeoutException(msg, e);
//HC3       }
//HC3     }
//HC3     return sock;
//HC3   }

  @Override
  public Socket createSocket(HttpContext context) throws IOException {
    Socket socket = super.createSocket(context);
 
    if (context == null) {
      return socket;
    }

    boolean keepAlive = false;
    Boolean keepAliveAttr =
	(Boolean)context.getAttribute(HttpClientUrlConnection.SO_KEEPALIVE);
    if (keepAliveAttr != null) {
      keepAlive = keepAliveAttr.booleanValue();
    }

    socket.setKeepAlive(keepAlive);
    return socket;
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
