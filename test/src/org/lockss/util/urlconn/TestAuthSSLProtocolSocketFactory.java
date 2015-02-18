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
//import java.net.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import static org.lockss.daemon.LockssKeyStoreManager.*;


public class TestAuthSSLProtocolSocketFactory extends LockssTestCase {

  static final String PASSWD = "teapot";
  static final String PASSWD1 = "Fezzik";
  static final String PASSWD2 = "Vizzini";
  static final String KEY_PASSWD1 = "Vroomfondel";
  static final String KEY_PASSWD2 = "Slartibartfast";

  MockLockssDaemon daemon;
  LockssKeyStoreManager keystoreMgr;
  RandomManager rmgr;
  File keyStoreDir;
  String keyStoreFileName;
  boolean didMakeStandaloneSecureRandom = false;


  public void setUp() throws Exception {
    super.setUp();
    ConfigurationUtil.addFromArgs(PARAM_EXIT_IF_MISSING_KEYSTORE, "false");
    keyStoreDir = getTempDir("TestBlockingSslStreamComm1");
    keyStoreFileName = new File(keyStoreDir, "test.keystore").toString();

    daemon = getMockLockssDaemon();
    rmgr = new TestingRandomManager();
    rmgr.initService(daemon);
    daemon.setRandomManager(rmgr);
  }

  void startKeyManager() {
    keystoreMgr = daemon.getKeystoreManager();
    keystoreMgr.startService();
    daemon.setDaemonRunning(true);
  }

  KeyStore createKeyStore(File file, String pass, String keyPass)
      throws Exception {
    Properties p = new Properties();
    p.put(KeyStoreUtil.PROP_KEYSTORE_FILE, file.toString());
    p.put(KeyStoreUtil.PROP_KEYSTORE_PASSWORD, pass);
    p.put(KeyStoreUtil.PROP_KEY_PASSWORD, keyPass);
    return KeyStoreUtil.createKeyStore(p);
  }

  void addKsProp(Properties p,
		 String name, String filename,
		 String pass, String keyPass) {
    String prefix = PARAM_KEYSTORE + "." + name + ".";
    p.put(prefix + KEYSTORE_PARAM_NAME, name);
    p.put(prefix + KEYSTORE_PARAM_TYPE, "JCEKS");
    p.put(prefix + KEYSTORE_PARAM_FILE, filename);
    if (pass != null) {
      p.put(prefix + KEYSTORE_PARAM_PASSWORD, pass);
    }
    p.put(prefix + KEYSTORE_PARAM_KEY_PASSWORD, keyPass);
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

  public void testMissingServerKeystore() throws Exception {
    startKeyManager();
    LockssSecureSocketFactory lsf =
      new MyLockssSecureSocketFactory("nsks", null);
    AuthSSLProtocolSocketFactory spsf = (AuthSSLProtocolSocketFactory)
      lsf.getHttpClientSecureProtocolSocketFactory();
    try {
      spsf.getSSLContext();
      fail("Missing server keystore should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testMissingClientKeystore() throws Exception {
    startKeyManager();
    LockssSecureSocketFactory lsf =
      new MyLockssSecureSocketFactory(null, "nsks");
    AuthSSLProtocolSocketFactory spsf = (AuthSSLProtocolSocketFactory)
      lsf.getHttpClientSecureProtocolSocketFactory();
    try {
      spsf.getSSLContext();
      fail("Missing client keystore should throw");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testServerAuth() throws Exception {
    startKeyManager();
    File file = new File(getTempDir("fnord"), "k1");
    createKeyStore(file, PASSWD1, KEY_PASSWD1);
    Properties props = new Properties();
    addKsProp(props, "ks1", file.toString(), null, KEY_PASSWD1);
    ConfigurationUtil.addFromProps(props);
    LockssSecureSocketFactory lsf =
      new MyLockssSecureSocketFactory("ks1", null);
    AuthSSLProtocolSocketFactory authFact = (AuthSSLProtocolSocketFactory)
      lsf.getHttpClientSecureProtocolSocketFactory();
    assertFalse(authFact.hasKeyManagers());
    assertFalse(authFact.hasTrustManagers());
    SSLContext ctxt = authFact.getSSLContext();
    assertFalse(authFact.hasKeyManagers());
    assertTrue(authFact.hasTrustManagers());
    assertFalse(didMakeStandaloneSecureRandom);
  }

  public void testClientAuth() throws Exception {
    startKeyManager();
    File file = new File(getTempDir("fnord"), "k1");
    createKeyStore(file, PASSWD1, KEY_PASSWD1);
    Properties props = new Properties();
    addKsProp(props, "ks1", file.toString(), null, KEY_PASSWD1);
    ConfigurationUtil.addFromProps(props);
    LockssSecureSocketFactory lsf
      = new MyLockssSecureSocketFactory(null, "ks1");
    AuthSSLProtocolSocketFactory authFact = (AuthSSLProtocolSocketFactory)
      lsf.getHttpClientSecureProtocolSocketFactory();
    assertFalse(authFact.hasKeyManagers());
    assertFalse(authFact.hasTrustManagers());
    SSLContext ctxt = authFact.getSSLContext();
    assertTrue(authFact.hasKeyManagers());
    assertFalse(authFact.hasTrustManagers());
    assertFalse(didMakeStandaloneSecureRandom);
  }

  public void testServerAuthDaemonNotRunning() throws Exception {
    File file = new File(getTempDir("fnord"), "k1");
    createKeyStore(file, PASSWD1, KEY_PASSWD1);
    Properties props = new Properties();
    addKsProp(props, "ks1", file.toString(), null, KEY_PASSWD1);
    ConfigurationUtil.addFromProps(props);
    LockssSecureSocketFactory lsf =
      new MyLockssSecureSocketFactory("ks1", null);
    AuthSSLProtocolSocketFactory authFact = (AuthSSLProtocolSocketFactory)
      lsf.getHttpClientSecureProtocolSocketFactory();
    assertFalse(authFact.hasKeyManagers());
    assertFalse(authFact.hasTrustManagers());
    SSLContext ctxt = authFact.getSSLContext();
    assertFalse(authFact.hasKeyManagers());
    assertTrue(authFact.hasTrustManagers());
    assertTrue(didMakeStandaloneSecureRandom);
  }

  class MyLockssSecureSocketFactory extends LockssSecureSocketFactory {
    public MyLockssSecureSocketFactory(String serverAuthKeystoreName,
				       String clientAuthKeystoreName) {
      super(serverAuthKeystoreName, clientAuthKeystoreName);
    }

    @Override
    protected AuthSSLProtocolSocketFactory
      newAuthSSLProtocolSocketFactory(String serverAuthKeystoreName,
				      String clientAuthKeystoreName) {
      return new MyAuthSSLProtocolSocketFactory(serverAuthKeystoreName,
						clientAuthKeystoreName);
    }
  }

  /** @see LockssTestCase#TestingRandomManager */
  class MyAuthSSLProtocolSocketFactory extends AuthSSLProtocolSocketFactory {
    public MyAuthSSLProtocolSocketFactory(String publicKeyStoreName,
					  String privateKeyStoreName) {
      super(publicKeyStoreName, privateKeyStoreName);
    }

    @Override
    SecureRandom getSecureRandom()
	throws NoSuchAlgorithmException, NoSuchProviderException {
      SecureRandom rng = super.getSecureRandom();
      LockssRandom lrand = new LockssRandom();
      byte[] rseed = new byte[4];
      lrand.nextBytes(rseed);
      rng.setSeed(rseed);
      didMakeStandaloneSecureRandom = true;
      return rng;
    }
  }
}
