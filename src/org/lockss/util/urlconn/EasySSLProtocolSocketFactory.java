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

/* Portions of this code are
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.lockss.util.urlconn;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;

//HC3 import org.apache.commons.httpclient.ConnectTimeoutException;
//HC3 import org.apache.commons.httpclient.HttpClientError;
//HC3 import org.apache.commons.httpclient.params.HttpConnectionParams;
//HC3 import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
//HC3 import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.logging.Log; 
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
//import org.apache.commons.httpclient.contrib.ssl.*;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;

import org.lockss.app.*;
import org.lockss.daemon.*;

/**
 * <p>
 * EasySSLProtocolSocketFactory can be used to creats SSL {@link Socket}s 
 * that accept self-signed certificates. 
 * </p>
 * <p>
 * This socket factory SHOULD NOT be used for productive systems 
 * due to security reasons, unless it is a concious decision and 
 * you are perfectly aware of security implications of accepting 
 * self-signed certificates
 * </p>
 *
 * <p>
 * Example of using custom protocol socket factory for a specific host:
 *     <pre>
 *     Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
 *
 *     HttpClient client = new HttpClient();
 *     client.getHostConfiguration().setHost("localhost", 443, easyhttps);
 *     // use relative url only
 *     GetMethod httpget = new GetMethod("/");
 *     client.executeMethod(httpget);
 *     </pre>
 * </p>
 * <p>
 * Example of using custom protocol socket factory per default instead of the standard one:
 *     <pre>
 *     Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
 *     Protocol.registerProtocol("https", easyhttps);
 *
 *     HttpClient client = new HttpClient();
 *     GetMethod httpget = new GetMethod("https://localhost/");
 *     client.executeMethod(httpget);
 *     </pre>
 * </p>
 * 
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 * 
 * <p>
 * DISCLAIMER: HttpClient developers DO NOT actively support this component.
 * The component is provided as a reference material, which may be inappropriate
 * for use without additional customization.
 * </p>
 */

//HC3 public class EasySSLProtocolSocketFactory implements SecureProtocolSocketFactory {
public class EasySSLProtocolSocketFactory implements LayeredConnectionSocketFactory {
  
    public static EasySSLProtocolSocketFactory INSTANCE =
      new EasySSLProtocolSocketFactory();

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(EasySSLProtocolSocketFactory.class);

    private SSLContext sslcontext = null;

    /**
     * Constructor for EasySSLProtocolSocketFactory.
     */
    public EasySSLProtocolSocketFactory() {
        super();
    }

    private static SSLContext createEasySSLContext() {
      try {
	LockssDaemon daemon = LockssDaemon.getLockssDaemon();
	SecureRandom rng;
	if (daemon.isDaemonRunning()) {
	  RandomManager rmgr = daemon.getRandomManager();
	  rng = rmgr.getSecureRandom();
	} else {
	  rng = SecureRandom.getInstance(RandomManager.DEFAULT_SECURE_RANDOM_ALGORITHM,
					 RandomManager.DEFAULT_SECURE_RANDOM_PROVIDER);
	}
	SSLContext context = SSLContext.getInstance("SSL");
	context.init(null, 
		     new TrustManager[] {new EasyX509TrustManager(null)}, 
		     rng);
	return context;
      } catch (Exception e) {
	LOG.error(e.getMessage(), e);
//HC3         throw new HttpClientError(e.toString());
	throw new Error(e.toString());
      }
    }

    private SSLContext getSSLContext() {
        if (this.sslcontext == null) {
            this.sslcontext = createEasySSLContext();
        }
        return this.sslcontext;
    }

//HC3     /**
//HC3      * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
//HC3      */
//HC3     public Socket createSocket(
//HC3         String host,
//HC3         int port,
//HC3         InetAddress clientHost,
//HC3         int clientPort)
//HC3         throws IOException, UnknownHostException {
//HC3 
//HC3         return getSSLContext().getSocketFactory().createSocket(
//HC3             host,
//HC3             port,
//HC3             clientHost,
//HC3             clientPort
//HC3         );
//HC3     }

//HC3     /**
//HC3      * Attempts to get a new socket connection to the given host within the given time limit.
//HC3      * <p>
//HC3      * To circumvent the limitations of older JREs that do not support connect timeout a 
//HC3      * controller thread is executed. The controller thread attempts to create a new socket 
//HC3      * within the given limit of time. If socket constructor does not return until the 
//HC3      * timeout expires, the controller terminates and throws an {@link ConnectTimeoutException}
//HC3      * </p>
//HC3      *  
//HC3      * @param host the host name/IP
//HC3      * @param port the port on the host
//HC3      * @param clientHost the local host name/IP to bind the socket to
//HC3      * @param clientPort the port on the local machine
//HC3      * @param params {@link HttpConnectionParams Http connection parameters}
//HC3      * 
//HC3      * @return Socket a new socket
//HC3      * 
//HC3      * @throws IOException if an I/O error occurs while creating the socket
//HC3      * @throws UnknownHostException if the IP address of the host cannot be
//HC3      * determined
//HC3      */
//HC3     public Socket createSocket(
//HC3         final String host,
//HC3         final int port,
//HC3         final InetAddress localAddress,
//HC3         final int localPort,
//HC3         final HttpConnectionParams params
//HC3     ) throws IOException, UnknownHostException, ConnectTimeoutException {
//HC3         if (params == null) {
//HC3             throw new IllegalArgumentException("Parameters may not be null");
//HC3       }
//HC3       int timeout = params.getConnectionTimeout();
//HC3       if (timeout == 0) {
//HC3 	return createSocket(host, port, localAddress, localPort);
//HC3       } else {
//HC3 	// To be eventually deprecated when migrated to Java 1.4 or above
//HC3         return ControllerThreadSocketFactory.createSocket(
//HC3                 this, host, port, localAddress, localPort, timeout);
//HC3       }
//HC3     }

//HC3     /**
//HC3      * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
//HC3      */
//HC3     public Socket createSocket(String host, int port)
//HC3         throws IOException, UnknownHostException {
//HC3         return getSSLContext().getSocketFactory().createSocket(
//HC3             host,
//HC3             port
//HC3         );
//HC3     }

//HC3     /**
//HC3      * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
//HC3      */
//HC3     public Socket createSocket(
//HC3         Socket socket,
//HC3         String host,
//HC3         int port,
//HC3         boolean autoClose)
//HC3         throws IOException, UnknownHostException {
//HC3         return getSSLContext().getSocketFactory().createSocket(
//HC3             socket,
//HC3             host,
//HC3             port,
//HC3             autoClose
//HC3         );
//HC3     }

    public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().equals(EasySSLProtocolSocketFactory.class));
    }

    public int hashCode() {
        return EasySSLProtocolSocketFactory.class.hashCode();
    }

    @Override
    public Socket connectSocket(int connectTimeout, Socket socket,
	      HttpHost httpHost, InetSocketAddress remoteAddress,
	      InetSocketAddress localAddress, HttpContext context)
	      throws IOException {
      // TODO: Implement it.
      return null;
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
      // TODO: Implement it.
      return null;
    }

    @Override
    public Socket createLayeredSocket(Socket socket, String host, int port,
	      HttpContext context) throws IOException, UnknownHostException {
      // TODO: Implement it.
      return null;
    }
}
