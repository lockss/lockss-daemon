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

package org.lockss.util;

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import javax.net.ssl.*;

import org.apache.commons.collections.ListUtils;

import org.lockss.test.*;
import org.lockss.config.*;

public class TestKeyStoreUtil extends LockssTestCase {

  public void setUp() throws Exception {
    super.setUp();
  }

  static String PASSWD = "a-passwd";
  static String KEY_PASSWD = "a-nother-passwd";


  Properties initProps() {
    Properties p = new Properties();
    p.put(KeyStoreUtil.PROP_KEYSTORE_PASSWORD, PASSWD);
    p.put(KeyStoreUtil.PROP_KEY_PASSWORD, KEY_PASSWD);
    return p;
  }

  void assertCharsBetween(int low, int hi, String str) {
    for (int ch : str.toCharArray()) {
      assertTrue(ch >= low && ch <= hi);
    }
  }

  public void testRandomString() throws Exception {
    SecureRandom rng = MiscTestUtil.getSecureRandom();
    String r10 = KeyStoreUtil.randomString(10, rng);
    String r1024 = KeyStoreUtil.randomString(1024, rng);
    assertEquals(10, r10.length());
    assertEquals(1024, r1024.length());
    assertCharsBetween(32, 126, r10);
    assertCharsBetween(32, 126, r1024);
    assertNotEquals(r1024, KeyStoreUtil.randomString(1024, rng));
  }

  public void testDefaults() throws Exception {
    Properties p = initProps();
    KeyStore ks = KeyStoreUtil.createKeyStore(p);
    List aliases = ListUtil.fromIterator(new EnumerationIterator(ks.aliases()));
    assertIsomorphic(SetUtil.set("mykey", "mycert"), SetUtil.theSet(aliases));
    assertNotNull(ks.getCertificate("mycert"));
    assertNull(ks.getCertificate("foocert"));
    assertEquals("JCEKS", ks.getType());
  }

  public void testStoreJks() throws Exception {
    File dir = getTempDir();
    File file = new File(dir, "test.ks");
    Properties p = initProps();
    p.put(KeyStoreUtil.PROP_KEYSTORE_FILE, file.toString());
    p.put(KeyStoreUtil.PROP_KEYSTORE_TYPE, "JKS");
    p.put(KeyStoreUtil.PROP_KEYSTORE_PROVIDER, "");
    assertFalse(file.exists());
    KeyStore ks = KeyStoreUtil.createKeyStore(p);
    assertTrue(file.exists());

    KeyStore ks2 = loadKeyStore(ks.getType(), file, PASSWD);
    List aliases =
      ListUtil.fromIterator(new EnumerationIterator(ks2.aliases()));
    assertIsomorphic(SetUtil.set("mykey", "mycert"), SetUtil.theSet(aliases));
    assertNotNull(ks2.getCertificate("mycert"));
    assertNull(ks2.getCertificate("foocert"));
    assertEquals("JKS", ks2.getType());
  }

  public void testStore() throws Exception {
    File dir = getTempDir();
    File file = new File(dir, "test.ks");
    Properties p = initProps();
    p.put(KeyStoreUtil.PROP_KEYSTORE_FILE, file.toString());
    assertFalse(file.exists());
    KeyStore ks = KeyStoreUtil.createKeyStore(p);
    assertTrue(file.exists());

    KeyStore ks2 = loadKeyStore(ks.getType(), file, PASSWD);
    List aliases =
      ListUtil.fromIterator(new EnumerationIterator(ks2.aliases()));
    assertIsomorphic(SetUtil.set("mykey", "mycert"), SetUtil.theSet(aliases));
    assertNotNull(ks2.getCertificate("mycert"));
    assertNull(ks2.getCertificate("foocert"));
    assertEquals("JCEKS", ks2.getType());
  }

  KeyStore loadKeyStore(String type, String file, String passwd)
      throws Exception {
    return loadKeyStore(type, new File(file), passwd);
  }

  KeyStore loadKeyStore(String type, File file, String passwd)
      throws Exception {
    KeyStore ks = KeyStore.getInstance(type);
    FileInputStream fis = new FileInputStream(file);
    ks.load(fis, passwd.toCharArray());
    return ks;
  }

  public void testCreateSharedPLNKeyStores() throws Exception {
    List<String> hosts = ListUtil.list("host1", "host2.foo.bar", "host3");
    List<String> hosts2 = ListUtil.list("host3", "host4");
    File dir = getTempDir();
    File pub = new File(dir, "pub.ks");
    KeyStoreUtil.createSharedPLNKeyStores(dir, hosts, pub, "pubpass",
					  MiscTestUtil.getSecureRandom());
    assertPubKs(pub, "pubpass", hosts);
    for (String host : hosts) {
      assertPrivateKs(new File(dir, host + ".jceks"),
		      StringUtil.fromFile(new File(dir, host + ".pass")),
		      host);
    }
    KeyStore pubks1 = loadKeyStore("jceks", new File(dir, "pub.ks"),
				   "pubpass");

    Certificate host1cert1 = pubks1.getCertificate("host1.crt");
    Certificate host3cert1 = pubks1.getCertificate("host3.crt");

    String host1priv1 = StringUtil.fromFile(new File(dir, "host1.jceks"));
    String host3priv1 = StringUtil.fromFile(new File(dir, "host3.jceks"));

    // Now add host4 and generate a new key for host3
    KeyStoreUtil.createSharedPLNKeyStores(dir, hosts2, pub, "pubpass",
					  MiscTestUtil.getSecureRandom());
    List<String> both = ListUtils.sum(hosts, hosts2);
    assertPubKs(pub, "pubpass", both);
    for (String host : both) {
      assertPrivateKs(new File(dir, host + ".jceks"),
		      StringUtil.fromFile(new File(dir, host + ".pass")),
		      host);
    }
    KeyStore pubks2 = loadKeyStore("jceks", new File(dir, "pub.ks"),
				   "pubpass");
    // host1 should have the same cert, host3 not
    Certificate host1cert2 = pubks2.getCertificate("host1.crt");
    Certificate host3cert2 = pubks2.getCertificate("host3.crt");
    assertEquals(host1cert1, host1cert2);
    assertNotEquals(host3cert1, host3cert2);

    // host1's private key file should be the same, host3's not
    String host1priv2 = StringUtil.fromFile(new File(dir, "host1.jceks"));
    String host3priv2 = StringUtil.fromFile(new File(dir, "host3.jceks"));
    assertEquals(host1priv1, host1priv2);
    assertNotEquals(host3priv1, host3priv2);
  }

  void assertPubKs(File file, String pass, List<String> hosts)
      throws Exception {
    KeyStore ks = loadKeyStore("jceks", file, pass);
    List aliases =
      ListUtil.fromIterator(new EnumerationIterator(ks.aliases()));
    assertEquals(hosts.size(), aliases.size());
    for (String host : hosts) {
      String alias = host + ".crt";
      Certificate cert = ks.getCertificate(alias);
      assertNotNull(cert);
      assertEquals("X.509", cert.getType());
    }
  }

  void assertPrivateKs(File file, String pass, String alias) throws Exception {
    KeyStore ks = loadKeyStore("jceks", file, alias);
    List aliases =
      ListUtil.fromIterator(new EnumerationIterator(ks.aliases()));
    assertEquals(2, aliases.size());
    Certificate cert = ks.getCertificate(alias + ".crt");
    assertNotNull(cert);
    assertEquals("X.509", cert.getType());
    assertTrue(ks.isKeyEntry(alias + ".key"));
    assertTrue(ks.isCertificateEntry(alias + ".crt"));
    Key key = ks.getKey(alias + ".key", pass.toCharArray());
    assertNotNull(key);
    assertEquals("RSA", key.getAlgorithm());
  }

}

