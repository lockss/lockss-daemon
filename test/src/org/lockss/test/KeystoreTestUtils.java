/*
 * $Id: KeystoreTestUtils.java,v 1.2 2005-02-02 09:42:20 tlipkis Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.util.*;
import java.io.*;
import java.util.jar.*;
import org.lockss.util.*;
import java.security.*;
import java.security.cert.*;

import sun.security.x509.*;

public class KeystoreTestUtils {

  private static Logger log = Logger.getLogger("KeystoreTestUtils");

  /**
   * Generate a public/private keypair and two keystores to hold them.
   * By default, this method will generate 512-bit keys and a self-
   * signed certificate valid for one day.
   *
   * @param pub   The public keystore file to create.
   * @param priv  The private keystore file to create.
   * @param password  The password to use for both the keystore and the
   *                  generated private key.
   * @param alias  The alias for the generated key and certificate.
   */
  public static void createKeystores(String pub, String priv,
				     String password, String alias,
				     String name)
      throws IOException, CertificateException, KeyStoreException,
	     InvalidKeyException, NoSuchAlgorithmException,
	     SignatureException, NoSuchProviderException {
    KeystoreTestUtils.createKeystores(pub, priv, password, password, alias,
				      name, "LOCKSS", "Stanford University",
				      "Palo Alto", "California",
				      "US", 1, 512);
  }

  /**
   * Generate a public/private keypair and keystores to hold them.
   *
   * @param pub   The public keystore file to create.
   * @param priv  The private keystore file to create.
   * @param keyPwd  The private key password.
   * @param storePwd  The public and private keystore password (if null,
   *                  this will be the same as the private key password).
   * @param alias   The alias for the generated key and certificate.
   * @param cn    The Common Name for the X.509 cert's DN.
   * @param ou    The Organization Unit for the X.509 cert's DN.
   * @param o   The Organization for the X.509 cert's DN.
   * @param l   The City/Locality for the X.509 cert's DN.
   * @param s   The State for the X.509 cert's DN.
   * @param c   The Country for the X.509 cert's DN.
   * @param days  The number of days the certificate is valid.
   * @param len   The size of the key in bits.
   */
  public static void createKeystores(String pub, String priv,
				     String keyPwd, String storePwd,
				     String alias, String cn, String ou,
				     String o, String l, String s, String c,
				     int days, int len)
      throws IOException, CertificateException, KeyStoreException,
	     InvalidKeyException, NoSuchAlgorithmException,
	     SignatureException, NoSuchProviderException {

    if (keyPwd == null) {
      throw new IllegalArgumentException("Key Password must not be null.");
    }

    if (storePwd == null) {
      storePwd = keyPwd;
    }

    if (pub == null) {
      throw new IllegalArgumentException("Public keystore must not be null.");
    }

    if (priv == null) {
      throw new IllegalArgumentException("Private keystore must not be null.");
    }

    File pubFile = new File(pub);
    File privFile = new File(priv);

    char[] keyPass = keyPwd.toCharArray();
    char[] storePass = storePwd.toCharArray();

    // Create private keystore
    KeyStore privateKs = KeyStore.getInstance("JKS", "SUN");
    // Create public keystore
    KeyStore publicKs = KeyStore.getInstance("JKS", "SUN");

    // Initialize the keystores
    privateKs.load(null, storePass);
    publicKs.load(null, storePass);

    // Generate a <i>len</i>-bit Digital Signature Algorithm (DSA) key pair
    // and certificate
    CertAndKeyGen keypair = new CertAndKeyGen("DSA", "SHA1WithDSA");
    X500Name dn = new X500Name(cn, ou, o, l, s, c);

    keypair.generate(len);
    PrivateKey privKey = keypair.getPrivateKey();

    X509Certificate selfCert =
      keypair.getSelfCertificate(dn, days * 24 * 60 * 60);

    // Add private key to keystore
    privateKs.setKeyEntry(alias, privKey, keyPass,
			  new X509Certificate[] { selfCert });

    // Add certificate to public keystore
    publicKs.setCertificateEntry(alias, selfCert);

    OutputStream privOut = new FileOutputStream(priv);
    OutputStream pubOut = new FileOutputStream(pub);

    privateKs.store(privOut, storePass);
    publicKs.store(pubOut, storePass);

    privOut.close();
    pubOut.close();
  }
}
