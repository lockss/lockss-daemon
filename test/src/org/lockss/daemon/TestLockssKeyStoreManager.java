/*
 * $Id: TestLockssKeyStoreManager.java,v 1.2 2009-06-09 06:13:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import java.util.*;
import java.io.*;
import java.security.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestLockssKeyStoreManager extends LockssTestCase {

  static final String PASSWD1 = "Fezzik";
  static final String PASSWD2 = "Vizzini";
  static final String KEY_PASSWD1 = "Vroomfondel";
  static final String KEY_PASSWD2 = "Slartibartfast";

  KeyStore createKeyStore(File file, String pass, String keyPass)
      throws Exception {
    Properties p = new Properties();
    p.put(KeyStoreUtil.PROP_KEYSTORE_FILE, file.toString());
    p.put(KeyStoreUtil.PROP_KEYSTORE_PASSWORD, pass);
    p.put(KeyStoreUtil.PROP_KEY_PASSWORD, keyPass);
    return KeyStoreUtil.createKeyStore(p);
  }

  void addKsProp(Properties p,
		 String name, String file,
		 String pass, String keyPass) {
    String prefix = LockssKeyStoreManager.PARAM_KEYSTORE + "." + name + ".";
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_NAME, name);
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_FILE, file);
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_TYPE, "JCEKS");
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_PASSWORD, pass);
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_KEY_PASSWORD, keyPass);
  }

  void addKsFile(Properties p,
		 String name, String file,
		 String pass, String keyPassFile) {
    String prefix = LockssKeyStoreManager.PARAM_KEYSTORE + "." + name + ".";
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_NAME, name);
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_FILE, file);
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_TYPE, "JCEKS");
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_PASSWORD, pass);
    p.put(prefix + LockssKeyStoreManager.KEYSTORE_PARAM_KEY_PASSWORD_FILE,
	  keyPassFile);
  }


  public void testOne() throws Exception {
    File dir = getTempDir("fnord");
    File file1 = new File(dir, "k1");
    File file2 = new File(dir, "k2");
    File passfile = new File(dir, "pass");
    FileTestUtil.writeFile(passfile, KEY_PASSWD2);
    
    createKeyStore(file1, PASSWD1, KEY_PASSWD1);
    createKeyStore(file2, PASSWD2, KEY_PASSWD2);

    Properties p = new Properties();
    addKsProp(p, "KS1", file1.toString(), PASSWD1, KEY_PASSWD1);
    addKsFile(p, "KS2", file2.toString(), PASSWD2,
	      new File(dir, "NO_FILE").toString());
    addKsFile(p, "KS3", file2.toString(), PASSWD2, passfile.toString());
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertTrue(passfile.exists());
    LockssKeyStoreManager keystoreMgr =
      getMockLockssDaemon().getKeystoreManager();
    keystoreMgr.startService();
    assertFalse(passfile.exists());

    LockssKeyStore lks1 = keystoreMgr.getLockssKeyStore("KS1");
    assertNotNull(lks1.getKeyManagerFactory());
    assertNotNull(lks1.getTrustManagerFactory());

    assertNull(keystoreMgr.getLockssKeyStore("KS2"));

    LockssKeyStore lks3 = keystoreMgr.getLockssKeyStore("KS3");
    assertNotNull(lks3.getKeyManagerFactory());
    assertNotNull(lks3.getTrustManagerFactory());
  }
}
