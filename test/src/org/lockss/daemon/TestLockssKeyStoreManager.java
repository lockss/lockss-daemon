/*
 * $Id$
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
import static org.lockss.daemon.LockssKeyStoreManager.*;
import static org.lockss.daemon.LockssKeyStore.LocationType;

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

  String ksprefix(String name) {
    return PARAM_KEYSTORE + "." + name + ".";
  }

  void addKsProp(Properties p,
		  String name,
		  String pass, String keyPass) {
    String prefix = ksprefix(name);
    p.put(prefix + KEYSTORE_PARAM_NAME, name);
    p.put(prefix + KEYSTORE_PARAM_TYPE, "JCEKS");
    p.put(prefix + KEYSTORE_PARAM_PASSWORD, pass);
    p.put(prefix + KEYSTORE_PARAM_KEY_PASSWORD, keyPass);
  }

  void addKsFile(Properties p,
		 String name,
		 String pass, String keyPassFile) {
    String prefix = ksprefix(name);
    p.put(prefix + KEYSTORE_PARAM_NAME, name);
    p.put(prefix + KEYSTORE_PARAM_TYPE, "JCEKS");
    p.put(prefix + KEYSTORE_PARAM_PASSWORD, pass);
    p.put(prefix + KEYSTORE_PARAM_KEY_PASSWORD_FILE, keyPassFile);
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
    addKsProp(p, "KS1", PASSWD1, KEY_PASSWD1);
    p.put(ksprefix("KS1") + KEYSTORE_PARAM_FILE, file1.toString());
    addKsFile(p, "KS2", PASSWD2,
	      new File(dir, "NO_FILE").toString());
    p.put(ksprefix("KS2") + KEYSTORE_PARAM_FILE, file2.toString());
    addKsFile(p, "KS3", PASSWD2, passfile.toString());
    p.put(ksprefix("KS3") + KEYSTORE_PARAM_FILE, file2.toString());
    ConfigurationUtil.setCurrentConfigFromProps(p);

    assertTrue(passfile.exists());
    LockssKeyStoreManager keystoreMgr =
      getMockLockssDaemon().getKeystoreManager();
    keystoreMgr.startService();
    assertFalse(passfile.exists());

    LockssKeyStore lks1 = keystoreMgr.getLockssKeyStore("KS1");
    assertEquals(LocationType.File, lks1.getLocationType());
    assertNotNull(lks1.getKeyManagerFactory());
    assertNotNull(lks1.getTrustManagerFactory());

    assertNull(keystoreMgr.getLockssKeyStore("KS2"));

    LockssKeyStore lks3 = keystoreMgr.getLockssKeyStore("KS3");
    assertNotNull(lks3.getKeyManagerFactory());
    assertNotNull(lks3.getTrustManagerFactory());
  }

  static final String PREF1 = LockssKeyStoreManager.PARAM_KEYSTORE + ".1.";
  static final String PREF2 = LockssKeyStoreManager.PARAM_KEYSTORE + ".2.";
  static final String PREF3 = LockssKeyStoreManager.PARAM_KEYSTORE + ".3.";
  static final String PREF4 = LockssKeyStoreManager.PARAM_KEYSTORE + ".4.";

  public void testLocationsAndAttrs() throws Exception {
    String URL = "http://u.r.l/pa/th";
    Properties p = new Properties();
    p.put(PREF1 + KEYSTORE_PARAM_NAME, "ksname1");
    p.put(PREF1 + KEYSTORE_PARAM_FILE, "filename");
    p.put(PREF2 + KEYSTORE_PARAM_NAME, "ksname22");
    p.put(PREF2 + KEYSTORE_PARAM_RESOURCE, "res/ource");
    p.put(PREF3 + KEYSTORE_PARAM_NAME, "ksname333");
    p.put(PREF3 + KEYSTORE_PARAM_URL, URL);
    p.put(PREF3 + KEYSTORE_PARAM_TYPE, "kstype");
    p.put(PREF3 + KEYSTORE_PARAM_PROVIDER, "ksprovider");
    p.put(PREF3 + KEYSTORE_PARAM_PASSWORD, "kspassword");
    p.put(PREF3 + KEYSTORE_PARAM_KEY_PASSWORD, "keyPassword");
    p.put(PREF3 + KEYSTORE_PARAM_KEY_PASSWORD_FILE, "pwf");
    p.put(PREF4 + KEYSTORE_PARAM_NAME, "ksname4");
    p.put(PREF4 + KEYSTORE_PARAM_FILE, "filename4");
    p.put(PREF4 + KEYSTORE_PARAM_CREATE, "true");

    ConfigurationUtil.addFromProps(p);
    LockssKeyStoreManager keystoreMgr = new MyLockssKeyStoreManager();
    MockLockssDaemon daemon = getMockLockssDaemon();
    daemon.setKeystoreManager(keystoreMgr);
    keystoreMgr.initService(daemon);
    keystoreMgr.startService();

    LockssKeyStore lk1 = keystoreMgr.getLockssKeyStore("ksname1");
    assertEquals(LocationType.File, lk1.getLocationType());
    assertEquals("filename", lk1.getLocation());
    assertFalse(lk1.getMayCreate());

    LockssKeyStore lk2 = keystoreMgr.getLockssKeyStore("ksname22");
    assertEquals(LocationType.Resource, lk2.getLocationType());
    assertEquals("res/ource", lk2.getLocation());

    LockssKeyStore lk3 = keystoreMgr.getLockssKeyStore("ksname333");
    assertEquals(LocationType.Url, lk3.getLocationType());
    assertEquals(URL, lk3.getLocation());
    assertEquals("kstype", lk3.getType());
    assertEquals("ksprovider", lk3.getProvider());
    assertFalse(lk3.getMayCreate());

    LockssKeyStore lk4 = keystoreMgr.getLockssKeyStore("ksname4");
    assertEquals(LocationType.File, lk4.getLocationType());
    assertEquals("filename4", lk4.getLocation());
    assertTrue(lk4.getMayCreate());
  }

  public void testMissingKeystore() throws Exception {
    Properties p = new Properties();
    addKsProp(p, "KS1", PASSWD1, KEY_PASSWD1);
    p.put(ksprefix("KS1") + KEYSTORE_PARAM_FILE, "no-such-file.notakeystore");
    p.put(PARAM_EXIT_IF_MISSING_KEYSTORE, "false");

    ConfigurationUtil.setCurrentConfigFromProps(p);
    LockssKeyStoreManager keystoreMgr =
      getMockLockssDaemon().getKeystoreManager();
    keystoreMgr.startService();
    assertNull(keystoreMgr.getLockssKeyStore("KS1"));
    try {
      keystoreMgr.getLockssKeyStore("KS1", "CriticalServiceName");
      fail("Getting non-existent criticl keystore shouldthrow");
    } catch (IllegalArgumentException e) {
    }
  }

  static class MyLockssKeyStoreManager extends LockssKeyStoreManager {
    @Override
    void loadKeyStores() {
    }
  }
}
