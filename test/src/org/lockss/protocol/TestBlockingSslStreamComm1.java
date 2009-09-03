/*
 * $Id: TestBlockingSslStreamComm1.java,v 1.10 2009-09-03 00:53:39 tlipkis Exp $
 */

/*

Copyright (c) 2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.protocol;

import java.util.*;
import java.io.*;
import java.net.*;
import junit.framework.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.test.*;

import sun.security.x509.*;
import sun.security.provider.IdentityDatabase;
import sun.security.provider.SystemSigner;

/**
 * This is the test class for org.lockss.protocol.BlockingStreamComm
 * using SSL
 */
public class TestBlockingSslStreamComm1 extends TestBlockingStreamComm {
  public static Class testedClasses[] = {
    BlockingStreamComm.class,
    BlockingPeerChannel.class,
  };


  LockssKeyStoreManager keystoreMgr;

  File keyStoreDir;
  String keyStorePassword = "Bad Password";
  String keyPassword = "No Donut!";
  String keyStoreFileName = null;

  TestBlockingSslStreamComm1(String name) {
    super(name);
  }

  protected boolean isSsl() {
    return true;
  }

  public void setUp() throws Exception {
    keyStoreDir = getTempDir("TestBlockingSslStreamComm1");
    keyStoreFileName = new File(keyStoreDir, "test.keystore").toString();
    super.setUp();
    shutdownOutputSupported = false;
    setupKeyStore();
    MockLockssDaemon daemon = getMockLockssDaemon();
    RandomManager rmgr = new MyRandomManager();
    rmgr.initService(daemon);
    daemon.setRandomManager(rmgr);
    
    keystoreMgr = daemon.getKeystoreManager();
    keystoreMgr.startService();
  }

  static String KS_NAME = "ks1";


  @Override
  public void addSuiteProps(Properties p) {
    super.addSuiteProps(p);
    p.setProperty(BlockingStreamComm.PARAM_USE_V3_OVER_SSL, "true");
    p.setProperty(BlockingStreamComm.PARAM_SSL_KEYSTORE_NAME, KS_NAME);
    p.setProperty(BlockingStreamComm.PARAM_USE_SSL_CLIENT_AUTH, "true");
    setKeyStoreProps(p, "id1", KS_NAME,
		     keyStoreFileName, keyStorePassword, keyPassword, null);
  }

  void setKeyStoreProps(Properties p, String id,
			String ksname, String filename,
			String kspasswd, String privatePassword,
			String privatePasswordFile) {
    String pref = LockssKeyStoreManager.PARAM_KEYSTORE + "." + id + ".";
    p.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_NAME, ksname);
    p.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_FILE, filename);
    p.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_TYPE, "JCEKS");
    p.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_PASSWORD, kspasswd);
    if (privatePassword != null) {
      p.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_KEY_PASSWORD,
	    privatePassword);
    }
    if (privatePasswordFile != null) {
      p.put(pref + LockssKeyStoreManager.KEYSTORE_PARAM_KEY_PASSWORD_FILE,
	    privatePasswordFile);
    }
  }

  private KeyStore setupKeyStore() throws Exception {
    Properties p = new Properties();
    p.put(KeyStoreUtil.PROP_KEYSTORE_FILE, keyStoreFileName);
    p.put(KeyStoreUtil.PROP_KEYSTORE_PASSWORD, keyStorePassword);
    p.put(KeyStoreUtil.PROP_KEY_PASSWORD, keyPassword);
    return KeyStoreUtil.createKeyStore(p);
  }

  // Harness to test that clientAuth works as intended.  I.e., if either or
  // both sides require client auth, connections in either direction are
  // successful only if the certificate is verified.

  void createKeystores(File dir, List hostnames)
      throws NoSuchAlgorithmException, NoSuchProviderException {
    KeyStoreUtil.createPLNKeyStores(null, dir, hostnames);
  }

  public void testClientAuth(Properties comm1Props,
			     Properties comm2Props,
			     boolean expectedSuccess)
      throws Exception {
    createKeystores(keyStoreDir, ListUtil.list("host1", "host2"));
    createKeystores(keyStoreDir, ListUtil.list("bad1", "bad2"));
    setKeyStoreProps(cprops, "ii11", "cks1",
		     new File(keyStoreDir, "host1.jceks").toString(), "host1",
		     null, new File(keyStoreDir, "host1.pass").toString());
    setKeyStoreProps(cprops, "ii22", "cks2",
		     new File(keyStoreDir, "host2.jceks").toString(), "host2",
		     null, new File(keyStoreDir, "host2.pass").toString());
    setKeyStoreProps(cprops, "ii33", "cks3",
		     new File(keyStoreDir, "bad1.jceks").toString(), "bad1",
		     null, new File(keyStoreDir, "bad1.pass").toString());
    cprops.setProperty(BlockingStreamComm.PARAM_MIN_FILE_MESSAGE_SIZE, "5000");
    ConfigurationUtil.addFromProps(cprops);
    
    comm1 = new MyBlockingStreamComm(setupPid(1));
    comm2 = new MyBlockingStreamComm(setupPid(2));
    SimpleQueue hsq1 = new SimpleQueue.Fifo();
    SimpleQueue hsq2 = new SimpleQueue.Fifo();
    comm1.setHandShakeQueue(hsq1);
    comm2.setHandShakeQueue(hsq2);

    comm1.setInstanceConfig(comm1Props);
    comm2.setInstanceConfig(comm2Props);

    setupComm(1, comm1);
    setupComm(2, comm2);

    PeerMessage msgIn;
    msg2 = makePeerMessage(1, "1234567890123456789012345678901234567890", 10);
    comm1.sendTo(msg1, pid2, null);
    if (comm1.isSsl()) {
      if (comm1.isClientAuth()) {
	Object hs = hsq1.get(TIMEOUT_SHOULDNT);
	assertNotNull("Expected handShake event didn't occur", hs);
	if (expectedSuccess) {
	  assertTrue(hs instanceof SSLSocket);
	} else {
	  assertTrue(hs instanceof SSLPeerUnverifiedException);
	}
      } else {
	assertEquals(null, hsq1.get(TIMEOUT_SHOULD));
      }
      if (expectedSuccess) {
	msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
	assertEqualsMessageFrom(msg1, pid1, msgIn);
	assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
      } else {
	msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULD);
	assertEquals(null, msgIn);
      }
    } else {
      assertEquals(null, hsq1.get(TIMEOUT_SHOULD));
      if (expectedSuccess) {
	msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULDNT);
	assertEqualsMessageFrom(msg1, pid1, msgIn);
	assertTrue(msgIn.toString(), msgIn instanceof MemoryPeerMessage);
      } else {
	msgIn = (PeerMessage)rcvdMsgs2.get(TIMEOUT_SHOULD);
	assertEquals(null, msgIn);
      }
    }
    if (comm2.isSsl()) {
      if (comm2.isClientAuth()) {
	Object hs = hsq2.get(TIMEOUT_SHOULDNT);
	assertNotNull("Expected handShake event didn't occur", hs);
	if (expectedSuccess) {
	  assertTrue(hs instanceof SSLSocket);
	} else {
	  assertTrue(hs instanceof SSLPeerUnverifiedException);
	}
      } else {
	assertEquals(null, hsq2.get(TIMEOUT_SHOULD));
      }
    } else {
      assertEquals(null, hsq2.get(TIMEOUT_SHOULD));
    }
  }

  static final String PARAM_USE_V3_OVER_SSL =
    BlockingStreamComm.PARAM_USE_V3_OVER_SSL;
  static final String PARAM_USE_SSL_CLIENT_AUTH =
    BlockingStreamComm.PARAM_USE_SSL_CLIENT_AUTH;
  static final String PARAM_SSL_KEYSTORE_NAME = 
    BlockingStreamComm.PARAM_SSL_KEYSTORE_NAME;


  // Valid certs, both require client auth, should connect
  public void testClientAuthOk() throws Exception {
    Properties p1 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "true",
				      PARAM_SSL_KEYSTORE_NAME, "cks1");
    Properties p2 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "true",
				      PARAM_SSL_KEYSTORE_NAME, "cks2");
    testClientAuth(p1, p2, true);
  }

  // Invalid certs, both require client auth, should not connect
  public void testClientAuthFailCert() throws Exception {
    Properties p1 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "true",
				      PARAM_SSL_KEYSTORE_NAME, "cks1");
    Properties p2 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "true",
				      PARAM_SSL_KEYSTORE_NAME, "cks3");
    testClientAuth(p1, p2, false);
  }

  // Valid certs, Only sender requires client auth, should connect
  public void testClientAuthSenderOnly() throws Exception {
    Properties p1 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "true",
				      PARAM_SSL_KEYSTORE_NAME, "cks1");
    Properties p2 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "false",
				      PARAM_SSL_KEYSTORE_NAME, "cks2");
    testClientAuth(p1, p2, true);
  }

  // Invalid certs, Only sender requires client auth, should not connect
  public void testClientAuthSenderOnlyFail() throws Exception {
    Properties p1 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "true",
				      PARAM_SSL_KEYSTORE_NAME, "cks1");
    Properties p2 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "false",
				      PARAM_SSL_KEYSTORE_NAME, "cks3");
    testClientAuth(p1, p2, false);
  }

  // Valid certs, Only receiver requires client auth, should connect
  public void testClientAuthReceiverOnly() throws Exception {
    Properties p1 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "false",
				      PARAM_SSL_KEYSTORE_NAME, "cks1");
    Properties p2 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "true",
				      PARAM_SSL_KEYSTORE_NAME, "cks2");
    testClientAuth(p1, p2, true);
  }

  // Invalid certs, Only receiver requires client auth, should not connect
  public void testClientAuthReceiverOnlyFail() throws Exception {
    Properties p1 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "false",
				      PARAM_SSL_KEYSTORE_NAME, "cks1");
    Properties p2 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "true",
				      PARAM_SSL_KEYSTORE_NAME, "cks3");
    testClientAuth(p1, p2, false);
  }

  // Valid certs, Only sender uses SSL, should not connect
  public void testClientAuthSenderOnlySsl() throws Exception {
    Properties p1 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "false",
				      PARAM_SSL_KEYSTORE_NAME, "cks1");
    Properties p2 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "false",
				      PARAM_USE_SSL_CLIENT_AUTH, "false",
				      PARAM_SSL_KEYSTORE_NAME, "cks2");
    isCheckSocketType = false;
    testClientAuth(p1, p2, false);
  }

  // Valid certs, Only receiver uses SSL, should not connect
  public void testClientAuthReceiverOnlySsl() throws Exception {
    Properties p1 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "false",
				      PARAM_USE_SSL_CLIENT_AUTH, "false",
				      PARAM_SSL_KEYSTORE_NAME, "cks1");
    Properties p2 = PropUtil.fromArgs(PARAM_USE_V3_OVER_SSL, "true",
				      PARAM_USE_SSL_CLIENT_AUTH, "false",
				      PARAM_SSL_KEYSTORE_NAME, "cks2");
    isCheckSocketType = false;
    testClientAuth(p1, p2, false);
  }

  // Set the seed of all SecureRandom instances.  These tests create lots
  // of instances; if each one generates its own seed the kernel's entropy
  // pool (/dev/random) gets exhausted and the tests block while more is
  // collected.  This can take several minutes on an otherwise idle
  // machine.
  // Successive seeds are obtained from a regular LockssRandom.  Using a
  // constant seed triggers an NPE in SSLSessionContextImpl; if every
  // SecureRandom instance produces the same sequence of bytes, duplicate
  // session keys result, causing collisions in the sessionCache.

  class MyRandomManager extends RandomManager {
    LockssRandom lrand = new LockssRandom();
    byte[] rseed = new byte[4];

    @Override
    protected void initRandom(SecureRandom rng) {
      lrand.nextBytes(rseed);
      rng.setSeed(rseed);
    }
  }

  // Run all tests in TestBlockingStreamComm, with SSL enabled
  public static class SslStreams extends TestBlockingSslStreamComm1 {
    public SslStreams(String name) {
      super(name);
    }
  }
  public static Test suite() {
    return variantSuites(new Class[] {
	SslStreams.class,
      });
  }
}
