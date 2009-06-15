/*
 * $Id: TestLockssKeyStore.java,v 1.3 2009-06-15 07:52:07 tlipkis Exp $
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
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.test.*;

import sun.security.x509.*;
import sun.security.provider.IdentityDatabase;
import sun.security.provider.SystemSigner;

public class TestLockssKeyStore extends LockssTestCase {

  static final String PASSWD = "teacup";

  KeyStore createKeyStore(File file, String keyPass) throws Exception {
    Properties p = new Properties();
    p.put(KeyStoreUtil.PROP_KEYSTORE_FILE, file.toString());
    p.put(KeyStoreUtil.PROP_KEYSTORE_PASSWORD, PASSWD);
    p.put(KeyStoreUtil.PROP_KEY_PASSWORD, keyPass);
    return KeyStoreUtil.createKeyStore(p);
  }

  LockssKeyStore createFromProp(String name, String file,
				String pass, String keyPass) {
    LockssKeyStore lk = new LockssKeyStore(name);
    lk.setFilename(file);
//     lk.setType(config.get(KEYSTORE_PARAM_TYPE, defaultKeyStoreType));
    lk.setPassword(pass);
    lk.setKeyPassword(keyPass);
    lk.setType(LockssKeyStoreManager.DEFAULT_DEFAULT_KEYSTORE_TYPE);
    return lk;
  }

  LockssKeyStore createFromFile(String name, String file,
				String pass, String keyPassFile) {
    LockssKeyStore lk = new LockssKeyStore(name);
    lk.setFilename(file);
//     lk.setType(config.get(KEYSTORE_PARAM_TYPE, defaultKeyStoreType));
    lk.setPassword(pass);
    lk.setKeyPasswordFile(keyPassFile);
    lk.setType(LockssKeyStoreManager.DEFAULT_DEFAULT_KEYSTORE_TYPE);
    return lk;
  }

  public void testNoFile() throws Exception {
    File tmpDir = getTempDir("kstmp");
    File file = new File(tmpDir, "ks1");
    assertFalse(file.exists());
    try {
      LockssKeyStore lk = createFromProp("lkone", file.toString(),
					 PASSWD, "pass42");
      lk.load();
      fail("Missing keystore file should fail");
    } catch (LockssKeyStore.UnavailableKeyStoreException e) {
    }
  }

  public void testLoad(String filename, String passwd, String keyPasswd)
      throws Exception {
    if (passwd == null) passwd = "password";
    if (keyPasswd == null) keyPasswd = "password";
    File file1 = new File(filename);
    try {
      LockssKeyStore lk = createFromProp("lkone", file1.toString(),
					 passwd, "wrong");
      lk.load();
      fail("Wrong password should fail");
    } catch (LockssKeyStore.UnavailableKeyStoreException e) {
    }
    try {
      LockssKeyStore lk = createFromProp("lkone", file1.toString(),
					 "wrong", keyPasswd);
      lk.load();
      fail("Wrong password should fail");
    } catch (LockssKeyStore.UnavailableKeyStoreException e) {
    }
    LockssKeyStore lk = createFromProp("lkone", file1.toString(),
				       passwd, keyPasswd);
    lk.load();
  }

  public void testLoad(String filename) throws Exception {
    testLoad(filename, null, null);
  }

  // Harness to test loading of actual keystores
  public void testLoad() throws Exception {
//     testLoad("/tmp/keystore");
  }

  public void testFromProps() throws Exception {
    File tmpDir = getTempDir("kstmp");
    File file = new File(tmpDir, "ks1");
    assertFalse(file.exists());
    createKeyStore(file, "pass42");
    assertTrue(file.exists());
    try {
      LockssKeyStore lk = createFromProp("lkone", file.toString(),
					 PASSWD, "wrong");
      lk.load();
      fail("Wrong password should fail");
    } catch (LockssKeyStore.UnavailableKeyStoreException e) {
    }
    try {
      LockssKeyStore lk = createFromProp("lkone", file.toString(),
					 "wrong", "pass42");
      lk.load();
      fail("Wrong password should fail");
    } catch (LockssKeyStore.UnavailableKeyStoreException e) {
    }
    LockssKeyStore lk = createFromProp("lkone", file.toString(),
				       PASSWD, "pass42");
    lk.load();
    assertNotNull(lk.getKeyManagerFactory());
    assertNotNull(lk.getTrustManagerFactory());
  }

  public void testFromFile() throws Exception {
    String pwd = "longish_password";
    File tmpDir = getTempDir("kstmp");
    File file = new File(tmpDir, "ks1");
    File passfile = new File(tmpDir, "pass");
    File badpassfile = new File(tmpDir, "badpass");
    assertFalse(file.exists());
    createKeyStore(file, pwd);
    assertTrue(file.exists());
    FileTestUtil.writeFile(passfile, pwd);
    FileTestUtil.writeFile(badpassfile, "not-the-password");
    try {
      LockssKeyStore lk = createFromFile("lkone", file.toString(),
					 PASSWD, badpassfile.toString());
      lk.load();
      fail("Wrong password should fail");
    } catch (LockssKeyStore.UnavailableKeyStoreException e) {
    }
    assertTrue(passfile.exists());
    LockssKeyStore lk = createFromFile("lkone", file.toString(),
				       PASSWD, passfile.toString());
    lk.load();
    assertFalse(passfile.exists());
    assertNotNull(lk.getKeyManagerFactory());
    assertNotNull(lk.getTrustManagerFactory());
  }

  public void testCreate() throws Exception {
    File tmpDir = getTempDir("kstmp");
    File file = new File(tmpDir, "ks1");
    assertFalse(file.exists());
    LockssKeyStore lk = createFromProp("lkone", file.toString(),
				       PASSWD, "pass42");
    lk.setMayCreate(true);
    assertFalse(file.exists());
    ConfigurationUtil.addFromArgs(ConfigManager.PARAM_PLATFORM_FQDN, "fq.dn");
    lk.load();
    assertTrue(file.exists());
    assertNotNull(lk.getKeyManagerFactory());
    assertNotNull(lk.getTrustManagerFactory());

    LockssKeyStore lk2 = createFromProp("lktwo", file.toString(),
					PASSWD, "pass42");
    lk2.load();
    Collection aliases =
      ListUtil.fromIterator(new EnumerationIterator(lk2.getKeyStore().aliases()));
    assertEquals(SetUtil.set("fq.dn.key", "fq.dn.cert"),
		 SetUtil.theSet(aliases));
  }

}
