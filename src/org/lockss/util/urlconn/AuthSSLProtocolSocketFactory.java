/*
 * $Id$
 */
/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

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
  implements SecureProtocolSocketFactory {

  private static final Logger log =
    Logger.getLogger("AuthSSLProtocolSocketFactory");

  public static final String DEFAULT_SSL_PROTOCOL = "SSL";

  private LockssKeyStoreManager keystoreMgr;
  private String privateKeyStoreName;
  private String publicKeyStoreName;
  private String sslProtocol = DEFAULT_SSL_PROTOCOL;
  private SSLContext sslcontext = null;
  private boolean hasKeyManagers = false;
  private boolean hasTrustManagers = false;

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
    }
    return this.sslcontext;
  }

  /**
   * Attempts to get a new socket connection to the given host within the
   * given time limit.  <p> To circumvent the limitations of older JREs
   * that do not support connect timeout a controller thread is
   * executed. The controller thread attempts to create a new socket within
   * the given limit of time. If socket constructor does not return until
   * the timeout expires, the controller terminates and throws an {@link
   * ConnectTimeoutException} </p>
   *  
   * @param host the host name/IP
   * @param port the port on the host
   * @param clientHost the local host name/IP to bind the socket to
   * @param clientPort the port on the local machine
   * @param params {@link HttpConnectionParams Http connection parameters}
   * 
   * @return Socket a new socket
   * 
   * @throws IOException if an I/O error occurs while creating the socket
   * @throws UnknownHostException if the IP address of the host cannot be
   * determined
   */
  public Socket createSocket(final String host,
			     final int port,
			     final InetAddress localAddress,
			     final int localPort,
			     final HttpConnectionParams params
			     )
      throws IOException, UnknownHostException, ConnectTimeoutException {
    if (params == null) {
      throw new IllegalArgumentException("Parameters may not be null");
    }
    int timeout = params.getConnectionTimeout();
    if (timeout == 0) {
      return createSocket(host, port, localAddress, localPort);
    } else {
      // To be eventually deprecated when migrated to Java 1.4 or above
      return ControllerThreadSocketFactory.createSocket(this, host, port,
							localAddress, localPort, timeout);
    }
  }

  public Socket createSocket(String host,
			     int port,
			     InetAddress clientHost,
			     int clientPort)
      throws IOException, UnknownHostException {
    SSLSocketFactory fact = getSSLContext().getSocketFactory();
    return getSSLContext().getSocketFactory().createSocket(host,
							   port,
							   clientHost,
							   clientPort);
  }

  public Socket createSocket(String host, int port)
      throws IOException, UnknownHostException {
    return getSSLContext().getSocketFactory().createSocket(host, port);
  }

  public Socket createSocket(Socket socket,
			     String host,
			     int port,
			     boolean autoClose)
      throws IOException, UnknownHostException {
    return getSSLContext().getSocketFactory().createSocket(socket, host,
							   port, autoClose);
  }

  // for testing
  boolean hasKeyManagers() {
    return hasKeyManagers;
  }

  boolean hasTrustManagers() {
    return hasTrustManagers;
  }
}
