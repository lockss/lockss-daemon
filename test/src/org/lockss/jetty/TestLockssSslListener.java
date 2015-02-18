/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.jetty;

import java.io.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;

import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestLockssSslListener extends LockssTestCase {

  static final String BAD_PROTO = "SSLv3";
  static final String KEYSTORE_PASS = "Keystore Password";
  static final String KEY_PASS = "No Donut!";

  LockssKeyStoreManager keystoreMgr;
  String keyStoreFileName;

  public void setUp() throws Exception {
    super.setUp();
    keyStoreFileName = new File(getTempDir(), "test.keystore").toString();
    MockLockssDaemon daemon = getMockLockssDaemon();
    RandomManager rmgr = new TestingRandomManager();
    rmgr.initService(daemon);
    daemon.setRandomManager(rmgr);
    
    keystoreMgr = daemon.getKeystoreManager();
    keystoreMgr.startService();
  }

  KeyStore createKeyStore(String file, String pass, String keyPass)
      throws Exception {
    Properties p = new Properties();
    p.put(KeyStoreUtil.PROP_KEYSTORE_FILE, file);
    p.put(KeyStoreUtil.PROP_KEYSTORE_PASSWORD, pass);
    p.put(KeyStoreUtil.PROP_KEY_PASSWORD, keyPass);
    return KeyStoreUtil.createKeyStore(p);
  }

  void addKsProp(Properties p,
		 String name, String filename,
		 String pass, String keyPass) {
    String prefix = LockssKeyStoreManager.PARAM_KEYSTORE + "." + name + ".";
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_NAME, name);
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_TYPE, "JCEKS");
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_FILE, filename);
    if (pass != null) {
      p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_PASSWORD, pass);
    }
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_KEY_PASSWORD, keyPass);
  }

  public void testDisableProtocols() throws Exception {
    createKeyStore(keyStoreFileName, KEYSTORE_PASS, KEY_PASS);
    Properties props = new Properties();
    addKsProp(props, "ks1", keyStoreFileName, null, KEY_PASS);
    ConfigurationUtil.addFromProps(props);
    LockssKeyStore lks = keystoreMgr.getLockssKeyStore("ks1");

    int port = TcpTestUtil.findUnboundTcpPort();

    org.mortbay.util.InetAddrPort hostPort =
      new org.mortbay.util.InetAddrPort("127.0.0.1", port);
    LockssSslListener lsl = new LockssSslListener(hostPort);
    KeyManagerFactory kmf = keystoreMgr.getKeyManagerFactory("ks1");

    lsl.setKeyManagerFactory(kmf);
    lsl.setDisableProtocols(ListUtil.list(BAD_PROTO));
    SSLServerSocket sock = (SSLServerSocket) lsl.newServerSocket(hostPort, 5);
    assertFalse(ListUtil.fromArray(sock.getEnabledProtocols()).contains(BAD_PROTO));
  }
}
