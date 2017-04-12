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

import java.io.IOException;
import java.net.*;

import org.apache.commons.collections.map.*;
//HC3 import org.apache.commons.httpclient.*;
//HC3 import org.apache.commons.httpclient.params.HttpConnectionParams;
//HC3 import org.apache.commons.httpclient.protocol.*;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.lockss.util.*;

/**
 * A SecureProtocolSocketFactory that dispatches to another
 * SecureProtocolSocketFactory associated with the connection's host and
 * port, or to a default SecureProtocolSocketFactory.  This is installed as
 * HttpClient's global factory bu HttpClientUrlConnection
 */
public class DispatchingSSLProtocolSocketFactory
//HC3   implements SecureProtocolSocketFactory {
  implements LayeredConnectionSocketFactory {

  public static DispatchingSSLProtocolSocketFactory INSTANCE =
    new DispatchingSSLProtocolSocketFactory();

  private static final Logger log =
    Logger.getLogger("DispatchingSSLProtocolSocketFactory");

  MultiKeyMap hostmap = new MultiKeyMap(); // Maps host,port to factory
//HC3   SecureProtocolSocketFactory defaultFact;
  LayeredConnectionSocketFactory defaultFact;

  /**
   * Constructor for DispatchingSSLProtocolSocketFactory.
   */
  public DispatchingSSLProtocolSocketFactory() {
    super();
  }

  /** Associate a SecureProtocolSocketFactory with a host,port pair.  The
   * factory will be used when opening a LockssUrlConnection to that host &
   * port.
   * @param host the hostname
   * @param port the port number
   * @param fact SecureProtocolSocketFactory to be used when opening
   * connections to the host,port
   */
  public void setFactory(String host,
			 int port,
//HC3 			       SecureProtocolSocketFactory fact) {
			 LayeredConnectionSocketFactory fact) {
    log.debug3("setFactory(" + host + ", " + port + ", " + fact + ")");
    hostmap.put(host.toLowerCase(), port, fact);
  }

  /** Set the default SecureProtocolSocketFactory */
//HC3   public void setDefaultFactory(SecureProtocolSocketFactory fact) {
  public void setDefaultFactory(LayeredConnectionSocketFactory fact) {
    defaultFact = fact;
  }

  /** Return the SecureProtocolSocketFactory associated with the host,port,
   * if any, else the default SecureProtocolSocketFactory
   * @param host the hostname
   * @param port the port number
   * @return the SecureProtocolSocketFactory to use for this host,port
   */
//HC3   SecureProtocolSocketFactory getFactory(String host, int port) {
  LayeredConnectionSocketFactory getFactory(String host, int port) {
//HC3     SecureProtocolSocketFactory ret =
//HC3       (SecureProtocolSocketFactory)hostmap.get(host.toLowerCase(), port);
      LayeredConnectionSocketFactory ret =
        (LayeredConnectionSocketFactory)hostmap.get(host.toLowerCase(), port);
    if (ret != null) {
      log.debug2("getFactory(" + host + ", " + port + "): " + ret);
      return ret;
    }
    log.debug2("getFactory(" + host + ", " + port + "): " + defaultFact);
    return defaultFact;
  }

//HC3   public Socket createSocket(String host,
//HC3 			     int port,
//HC3 			     InetAddress clientHost,
//HC3 			     int clientPort)
//HC3       throws IOException, UnknownHostException {
//HC3
//HC3     return getFactory(host, port).createSocket(host,
//HC3 					       port,
//HC3 					       clientHost,
//HC3 					       clientPort);
//HC3   }

//HC3   public Socket createSocket(final String host,
//HC3 			     final int port,
//HC3 			     final InetAddress localAddress,
//HC3 			     final int localPort,
//HC3 			     final HttpConnectionParams params)
//HC3       throws IOException, UnknownHostException, ConnectTimeoutException {
//HC3     return getFactory(host, port).createSocket(host,
//HC3 					             port,
//HC3 					             localAddress,
//HC3 					             localPort,
//HC3 					             params);
//HC3   }

//HC3   public Socket createSocket(String host, int port)
//HC3     throws IOException, UnknownHostException {
//HC3     return getFactory(host, port).createSocket(host, port);
//HC3   }

//HC3   public Socket createSocket(Socket socket,
//HC3 			     String host,
//HC3 			     int port,
//HC3 			     boolean autoClose)
//HC3       throws IOException, UnknownHostException {
//HC3     return getFactory(host, port).createSocket(socket, host, port, autoClose);
//HC3   }

  @Override
  public Socket connectSocket(int connectTimeout, Socket socket,
      HttpHost httpHost, InetSocketAddress remoteAddress,
      InetSocketAddress localAddress, HttpContext context)
      throws IOException {
    return getFactory(httpHost.getHostName(), httpHost.getPort())
	.connectSocket(connectTimeout, socket, httpHost, remoteAddress,
	    localAddress, context);
  }

  @Override
  public Socket createSocket(HttpContext context) throws IOException {
    HttpHost httpHost =
	(HttpHost)context.getAttribute(HttpClientUrlConnection.SO_HTTP_HOST);
    return getFactory(httpHost.getHostName(), httpHost.getPort())
	.createSocket(context);
  }

  @Override
  public Socket createLayeredSocket(Socket socket, String host, int port,
      HttpContext context) throws IOException, UnknownHostException {
    return getFactory(host, port)
	.createLayeredSocket(socket, host, port, context);
  }
}
