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

import java.io.*;
import java.net.*;
import java.security.*;
import javax.net.ssl.*;
//HC3 import org.apache.commons.httpclient.ConnectTimeoutException;
//HC3 import org.apache.commons.httpclient.params.HttpConnectionParams;
//HC3 import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
//HC3 import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.daemon.*;

/**
 * AuthSSLProtocolSocketFactory can be used to validate the identity of the
 * HTTPS server against a list of trusted certificates and to authenticate
 * to the HTTPS server using a private key.
 * 
 * AuthSSLProtocolSocketFactory will enable server authentication when
 * supplied with a KeyStore containg one or several trusted certificates.
 * The client secure socket will reject the connection during the SSL
 * session handshake if the target HTTPS server attempts to authenticate
 * itself with a non-trusted certificate.
 * 
 * Use JDK keytool utility to import a trusted certificate and generate a
 * truststore file:
 *    <pre>
 *     keytool -import -alias "my server cert" -file server.crt -keystore my.truststore
 *    </pre>
 * 
 * AuthSSLProtocolSocketFactory will enable client authentication when
 * supplied with a KeyStore containg a private key/public certificate pair.
 * The client secure socket will use the private key to authenticate itself
 * to the target HTTPS server during the SSL session handshake if requested
 * to do so by the server.  The target HTTPS server will in its turn verify
 * the certificate presented by the client in order to establish client's
 * authenticity
 * 
 * Use the following sequence of actions to generate a keystore file
 *   <ul>
 *     <li>
 *      Use JDK keytool utility to generate a new key
 *      <pre>keytool -genkey -v -alias "my client key" -validity 365 -keystore my.keystore</pre>
 *      For simplicity use the same password for the key as that of the keystore
 *     </li>
 *     <li>
 *      Issue a certificate signing request (CSR)
 *      <pre>keytool -certreq -alias "my client key" -file mycertreq.csr -keystore my.keystore</pre>
 *     </li>
 *     <li>
 *      Send the certificate request to the trusted Certificate Authority for signature. 
 *      One may choose to act as her own CA and sign the certificate request using a PKI 
 *      tool, such as OpenSSL.
 *     </li>
 *     <li>
 *       Import the trusted CA root certificate
 *       <pre>keytool -import -alias "my trusted ca" -file caroot.crt -keystore my.keystore</pre> 
 *     </li>
 *     <li>
 *       Import the PKCS#7 file containg the complete certificate chain
 *       <pre>keytool -import -alias "my client key" -file mycert.p7 -keystore my.keystore</pre> 
 *     </li>
 *     <li>
 *       Verify the content the resultant keystore file
 *       <pre>keytool -list -v -keystore my.keystore</pre> 
 *     </li>
 *   </ul>
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 * 
 */

public class AuthSSLProtocolSocketFactory
//HC3   implements SecureProtocolSocketFactory {
  implements LayeredConnectionSocketFactory {

  private static final Logger log =
    Logger.getLogger("AuthSSLProtocolSocketFactory");

  public static final String DEFAULT_SSL_PROTOCOL = "SSL";

  private String privateKeyStoreName;
  private String publicKeyStoreName;
  private String sslProtocol = DEFAULT_SSL_PROTOCOL;
  private SSLContext sslcontext = null;
  private boolean hasKeyManagers = false;
  private boolean hasTrustManagers = false;
  private SSLConnectionSocketFactory sslCsf = null;

  /**
   * Constructor for AuthSSLProtocolSocketFactory.  Either a public
   * truststore or a private keystore must be given.
   * 
   * @param publicKeyStoreName name of the truststore managed by {@link
   * org.lockss.daemon.LockssKeyStoreManager}.  If null, server
   * authentication will not be performed.
   * @param privateKeyStoreName name of the private keystore managed by {@link
   * org.lockss.daemon.LockssKeyStoreManager}.  If null, client
   * authentication will not be performed.
   */
  public AuthSSLProtocolSocketFactory(String publicKeyStoreName,
				      String privateKeyStoreName) {
    super();
    this.publicKeyStoreName = publicKeyStoreName;
    this.privateKeyStoreName = privateKeyStoreName;
  }

  private SSLContext createSSLContext() throws IOException {
    LockssDaemon daemon = LockssDaemon.getLockssDaemon();
    LockssKeyStoreManager keystoreMgr;
    SecureRandom rng;
    try {
      if (daemon.isDaemonRunning()) {
	keystoreMgr = daemon.getKeystoreManager();
	RandomManager rmgr = daemon.getRandomManager();
	rng = rmgr.getSecureRandom();
      } else {
	rng = getSecureRandom();
	keystoreMgr = new LockssKeyStoreManager();
	keystoreMgr.initService(daemon);
	keystoreMgr.startService();
	Configuration platConfig = ConfigManager.getPlatformConfig();
	keystoreMgr.setConfig(platConfig, null, platConfig.differences(null));
      }
      KeyManager[] kma = null;
      if (privateKeyStoreName != null) {
	KeyManagerFactory kmf =
	  keystoreMgr.getKeyManagerFactory(privateKeyStoreName, "ClientAuth");
	if (kmf != null) {
	  kma = kmf.getKeyManagers();
	} else if (false) {
	  throw new IllegalArgumentException("Private keystore not found: "
					     + privateKeyStoreName);
	}
      }
      TrustManager[] tma = null;
      if (publicKeyStoreName != null) {
	TrustManagerFactory tmf =
	  keystoreMgr.getTrustManagerFactory(publicKeyStoreName, "ServerAuth");
	if (tmf != null) {
	  tma = tmf.getTrustManagers();
	} else if (false) {
	  throw new IllegalArgumentException("Public keystore not found: "
					     + publicKeyStoreName);
	}
      }
      // Now create an SSLContext from the KeyManager
      SSLContext ctxt = null;
      ctxt = SSLContext.getInstance(sslProtocol); // "SSL"
      ctxt.init(kma, tma, rng);
      log.debug2("createSSLContext: " + ctxt);
      hasKeyManagers = kma != null && kma.length != 0;
      hasTrustManagers = tma != null && tma.length != 0;
      return ctxt;
    } catch (NoSuchAlgorithmException ex) {
      throw new IOException("Can't create SSL Context", ex);
    } catch (NoSuchProviderException ex) {
      throw new IOException("Can't create SSL Context", ex);
    } catch (KeyManagementException ex) {
      throw new IOException("Can't create SSL Context", ex);
    }
  }

  // Overridden for testing to supply a SecureRandom that doesn't use up
  // kernel randomness
  SecureRandom getSecureRandom()
      throws NoSuchAlgorithmException, NoSuchProviderException {
    return
      SecureRandom.getInstance(RandomManager.DEFAULT_SECURE_RANDOM_ALGORITHM,
			       RandomManager.DEFAULT_SECURE_RANDOM_PROVIDER);
  }

  SSLContext getSSLContext() throws IOException {
    if (this.sslcontext == null) {
      this.sslcontext = createSSLContext();
      sslCsf = new SSLConnectionSocketFactory(getSSLContext());
    }
    return this.sslcontext;
  }

//HC3   /**
//HC3    * Attempts to get a new socket connection to the given host within the
//HC3    * given time limit.  <p> To circumvent the limitations of older JREs
//HC3    * that do not support connect timeout a controller thread is
//HC3    * executed. The controller thread attempts to create a new socket within
//HC3    * the given limit of time. If socket constructor does not return until
//HC3    * the timeout expires, the controller terminates and throws an {@link
//HC3    * ConnectTimeoutException} </p>
//HC3    *  
//HC3    * @param host the host name/IP
//HC3    * @param port the port on the host
//HC3    * @param clientHost the local host name/IP to bind the socket to
//HC3    * @param clientPort the port on the local machine
//HC3    * @param params {@link HttpConnectionParams Http connection parameters}
//HC3    * 
//HC3    * @return Socket a new socket
//HC3    * 
//HC3    * @throws IOException if an I/O error occurs while creating the socket
//HC3    * @throws UnknownHostException if the IP address of the host cannot be
//HC3    * determined
//HC3    */
//HC3   public Socket createSocket(final String host,
//HC3 			     final int port,
//HC3 			     final InetAddress localAddress,
//HC3 			     final int localPort,
//HC3                              final HttpConnectionParams params
//HC3 			     )
//HC3      throws IOException, UnknownHostException, ConnectTimeoutException {
//HC3     if (params == null) {
//HC3       throw new IllegalArgumentException("Parameters may not be null");
//HC3     int timeout = params.getConnectionTimeout();
//HC3     if (timeout == 0) {
//HC3       return createSocket(host, port, localAddress, localPort);
//HC3     } else {
//HC3       // To be eventually deprecated when migrated to Java 1.4 or above
//HC3       return ControllerThreadSocketFactory.createSocket(this, host, port,
//HC3 							localAddress, localPort, timeout);
//HC3     }
//HC3   }

//HC3   public Socket createSocket(String host,
//HC3 			     int port,
//HC3 			     InetAddress clientHost,
//HC3 			     int clientPort)
//HC3       throws IOException, UnknownHostException {
//HC3     SSLSocketFactory fact = getSSLContext().getSocketFactory();
//HC3     return getSSLContext().getSocketFactory().createSocket(host,
//HC3 							   port,
//HC3 							   clientHost,
//HC3 							   clientPort);
//HC3   }

//HC3   public Socket createSocket(String host, int port)
//HC3       throws IOException, UnknownHostException {
//HC3     return getSSLContext().getSocketFactory().createSocket(host, port);
//HC3   }

//HC3   public Socket createSocket(Socket socket,
//HC3 			     String host,
//HC3 			     int port,
//HC3 			     boolean autoClose)
//HC3       throws IOException, UnknownHostException {
//HC3     return getSSLContext().getSocketFactory().createSocket(socket, host,
//HC3 							   port, autoClose);
//HC3   }

  @Override
  public Socket connectSocket(int connectTimeout, Socket socket,
      HttpHost httpHost, InetSocketAddress remoteAddress,
      InetSocketAddress localAddress, HttpContext context)
      throws IOException {
    return sslCsf.connectSocket(connectTimeout, socket, httpHost, remoteAddress,
	localAddress, context);
  }

  @Override
  public Socket createSocket(HttpContext context) throws IOException {
    return sslCsf.createSocket(context);
  }

  @Override
  public Socket createLayeredSocket(Socket socket, String host, int port,
      HttpContext context) throws IOException, UnknownHostException {
    return sslCsf.createLayeredSocket(socket, host, port, context);
  }

  // for testing
  boolean hasKeyManagers() {
    return hasKeyManagers;
  }

  boolean hasTrustManagers() {
    return hasTrustManagers;
  }
}
