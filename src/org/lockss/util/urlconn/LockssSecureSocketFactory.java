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

//HC3 import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;

/** Encapsulates secure socket factory used by implementations of
 * LockssUrlConnection.
 */
public class LockssSecureSocketFactory {
  protected String serverAuthKeystoreName;
  protected String clientAuthKeystoreName;
//HC3   protected SecureProtocolSocketFactory httpClientSecureSockFact;
  protected LayeredConnectionSocketFactory httpClientSecureSockFact;

  public LockssSecureSocketFactory(String serverAuthKeystoreName,
				   String clientAuthKeystoreName) {
    this.serverAuthKeystoreName = serverAuthKeystoreName;
    this.clientAuthKeystoreName = clientAuthKeystoreName;
  }

//HC3   /** Return (creating if necessary) an HttpClient
//HC3    * SecureProtocolSocketFactory */
//HC3   public SecureProtocolSocketFactory getHttpClientSecureProtocolSocketFactory() {
  /** Return (creating if necessary) an HttpClient
   * LayeredConnectionSocketFactory */
  public LayeredConnectionSocketFactory
  getHttpClientSecureProtocolSocketFactory() {
    if (httpClientSecureSockFact == null) {
      httpClientSecureSockFact =
	newAuthSSLProtocolSocketFactory(serverAuthKeystoreName,
					clientAuthKeystoreName);
    }
    return httpClientSecureSockFact;
  }

  // Overridden for testing to supply a SecureRandom that doesn't use up
  // kernel randomness
  protected AuthSSLProtocolSocketFactory
    newAuthSSLProtocolSocketFactory(String serverAuthKeystoreName,
				    String clientAuthKeystoreName) {
    return new AuthSSLProtocolSocketFactory(serverAuthKeystoreName,
					    clientAuthKeystoreName);
  }

  public boolean requiresServerAuth() {
    return serverAuthKeystoreName != null;
  }

  public String getServerAuthKeystoreName() {
    return serverAuthKeystoreName;
  }

  public String getClientAuthKeystoreName() {
    return clientAuthKeystoreName;
  }

}
