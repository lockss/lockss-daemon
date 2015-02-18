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

package org.lockss.util.urlconn;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import static org.lockss.daemon.LockssKeyStoreManager.*;


public class TestDispatchingSSLProtocolSocketFactory extends LockssTestCase {

  DispatchingSSLProtocolSocketFactory fact;
  EasySSLProtocolSocketFactory easyFact;

  public void setUp() throws Exception {
    super.setUp();
    fact = new DispatchingSSLProtocolSocketFactory();
    easyFact = new EasySSLProtocolSocketFactory();
    fact.setDefaultFactory(easyFact);
  }

  public void testDefaultFact() {
    assertSame(easyFact, fact.getFactory("foohost", 1234));
  }

  public void testHostFact() {
    String host = "props.lockss.org";
    int port = 8000;
    SecureProtocolSocketFactory hostFact =
      new AuthSSLProtocolSocketFactory(null, null);
    assertSame(easyFact, fact.getFactory(host, port));
    fact.setFactory(host, port, hostFact);
    assertSame(hostFact, fact.getFactory(host, port));
    assertSame(easyFact, fact.getFactory("foo." + host, port));
    assertSame(easyFact, fact.getFactory(host, port + 1));
    assertSame(easyFact, fact.getFactory("foohost", 1234));
  }

  public void testCreateSocket() throws IOException {
    String host1 = "host1.tld";
    String host2 = "host2.foo";
    int port1 = 8000;
    int port2 = 9876;
    MySecureProtocolSocketFactory fact1 = new MySecureProtocolSocketFactory();
    MySecureProtocolSocketFactory fact2 = new MySecureProtocolSocketFactory();
    MySecureProtocolSocketFactory fact3 = new MySecureProtocolSocketFactory();
    MySecureProtocolSocketFactory fact4 = new MySecureProtocolSocketFactory();
    fact.setFactory(host1, port1, fact1);
    fact.setFactory(host1, port2, fact2);
    fact.setFactory(host2, port1, fact3);
    fact.setDefaultFactory(fact4);
    fact.createSocket(host1, port1);
    assertEquals(ListUtil.list(host1), fact1.getHosts());
    assertEmpty(fact2.getHosts());
    fact.createSocket(host1, port2);
    assertEquals(ListUtil.list(host1), fact1.getHosts());
    assertEquals(ListUtil.list(host1), fact2.getHosts());
    fact.createSocket(host2, port1);
    assertEquals(ListUtil.list(host1), fact1.getHosts());
    assertEquals(ListUtil.list(host1), fact2.getHosts());
    assertEquals(ListUtil.list(host2), fact3.getHosts());
    assertEmpty(fact4.getHosts());
    fact.createSocket(host2, port2);
    assertEquals(ListUtil.list(host2), fact4.getHosts());
  }

  static class MySecureProtocolSocketFactory
    implements SecureProtocolSocketFactory {

    List hosts = new ArrayList();

    List getHosts() {
      return hosts;
    }

    public Socket createSocket(String host, int port,
			       InetAddress clientHost, int clientPort) {
      hosts.add(host);
      return null;
    }
    public Socket createSocket(String host, int port,
			       InetAddress localAddress, int localPort,
			       HttpConnectionParams params) {
      hosts.add(host);
      return null;
    }

    public Socket createSocket(String host, int port) {
      hosts.add(host);
      return null;
    }

    public Socket createSocket(Socket socket, String host,
			       int port, boolean autoClose) {
      hosts.add(host);
      return null;
    }

  }

}
