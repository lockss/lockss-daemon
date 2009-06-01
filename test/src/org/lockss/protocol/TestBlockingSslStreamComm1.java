/*
 * $Id: TestBlockingSslStreamComm1.java,v 1.7 2009-06-01 07:56:02 tlipkis Exp $
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

/*
 * NB - this and its companion TestBlockingSslStreamComm2 should really be
 * two variants in one test.  They were separated because executing this
 * one first and the permanent keystore version second failed,  where in
 * the other order they both succeeded.  This seems to be some issue with
 * Junit class initialization,  but rather than tackling it I opted to
 * split them.  At some point these two files should be re-combined.
 */

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
    File keyStoreDir = getTempDir("TestBlockingSslStreamComm1");
    keyStoreFileName = new File(keyStoreDir, "test.keystore").toString();
    super.setUp();
    shutdownOutputSupported = false;
    setupKeyStore();
    keystoreMgr = getMockLockssDaemon().getKeystoreManager();
    keystoreMgr.startService();
  }

  static String KS_NAME = "ks1";
  static String PREF = LockssKeyStoreManager.PARAM_KEYSTORE + ".id1.";


  @Override
  public void addSuiteProps(Properties p) {
    p.setProperty(BlockingStreamComm.PARAM_USE_V3_OVER_SSL, "true");
    p.setProperty(BlockingStreamComm.PARAM_SSL_KEYSTORE_NAME, KS_NAME);
    p.setProperty(BlockingStreamComm.PARAM_USE_SSL_CLIENT_AUTH, "true");

    p.put(PREF + LockssKeyStoreManager.KEYSTORE_PARAM_NAME, KS_NAME);
    p.put(PREF + LockssKeyStoreManager.KEYSTORE_PARAM_FILE,
	  keyStoreFileName);
    p.put(PREF + LockssKeyStoreManager.KEYSTORE_PARAM_TYPE, "JCEKS");
    p.put(PREF + LockssKeyStoreManager.KEYSTORE_PARAM_PASSWORD,
	  keyStorePassword);
    p.put(PREF + LockssKeyStoreManager.KEYSTORE_PARAM_KEY_PASSWORD,
	  keyPassword);
  }

  private KeyStore setupKeyStore() throws Exception {
    Properties p = new Properties();
    p.put(KeyStoreUtil.PROP_KEYSTORE_FILE, keyStoreFileName);
    p.put(KeyStoreUtil.PROP_KEYSTORE_PASSWORD, keyStorePassword);
    p.put(KeyStoreUtil.PROP_KEY_PASSWORD, keyPassword);
    return KeyStoreUtil.createKeyStore(p);
  }

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
