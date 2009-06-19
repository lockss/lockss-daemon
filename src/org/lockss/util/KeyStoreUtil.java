/*
 * $Id: KeyStoreUtil.java,v 1.1.2.2 2009-06-19 08:24:41 tlipkis Exp $
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
import java.security.cert.*;
import javax.net.ssl.*;

import sun.security.x509.*;
import sun.security.pkcs.PKCS10;
import sun.security.provider.IdentityDatabase;
import sun.security.provider.SystemSigner;

import org.lockss.config.*;


/**
 * Utilities for creating keystores
 */
public class KeyStoreUtil {
  protected static Logger log = Logger.getLogger("KeyStoreUtil");


  // Large set of args passed in Properties or Configuration
  /** File to write keystore to */
  public static final String PROP_KEYSTORE_FILE = "File";
  /** Optional, default is JCEKS */
  public static final String PROP_KEYSTORE_TYPE = "Type";
  /** Optional, default is SunJCE */
  public static final String PROP_KEYSTORE_PROVIDER = "Provider";
  /** KeyStore password */
  public static final String PROP_KEYSTORE_PASSWORD = "Password";
  /** Private key password */
  public static final String PROP_KEY_PASSWORD = "KeyPassword";
  /** Default MyKey */
  public static final String PROP_KEY_ALIAS = "KeyAlias";
  /** Default MyCert */
  public static final String PROP_CERT_ALIAS = "CertAlias";
  /** Default RSA */
  public static final String PROP_KEY_ALGORITHM = "KeyAlgorithm";
  /** Default MD5WithRSA */
  public static final String PROP_SIG_ALGORITHM = "SigAlgorithm";
  /** X500Name.  Default 5 years */
  public static final String PROP_X500_NAME = "X500Name";
  /** Default 1024 */
  public static final String PROP_KEY_BITS = "KeyBits";
  /** Seconds.  Default 5 years */
  public static final String PROP_EXPIRE_IN = "ExpireIn";
  

  public static final String DEFAULT_KEYSTORE_TYPE = "JCEKS";
  public static final String DEFAULT_KEYSTORE_PROVIDER = "SunJCE";

  public static final String DEFAULT_KEY_ALIAS = "MyKey";
  public static final String DEFAULT_CERT_ALIAS = "MyCert";
  public static final String DEFAULT_KEY_ALGORITHM = "RSA";
  public static final String DEFAULT_SIG_ALGORITHM = "MD5WithRSA";
  public static final String DEFAULT_X500_NAME = "CN=LOCKSS box";
  public static final int DEFAULT_KEY_BITS = 1024;
  public static final long DEFAULT_EXPIRE_IN = 5 * Constants.YEAR / 1000;


  public static String randomString(int len)
      throws NoSuchAlgorithmException,
	     NoSuchProviderException {
    SecureRandom rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
    return org.apache.commons.lang.RandomStringUtils.random(len, 32, 126,
							    false, false,
							    null, rng);
  }

  public static KeyStore createKeyStore(Properties p)
      throws CertificateException,
	     IOException,
	     InvalidKeyException,
	     KeyStoreException,
	     NoSuchAlgorithmException,
	     NoSuchProviderException,
	     SignatureException,
	     UnrecoverableKeyException {
    KeyStore ks;
    String provider = p.getProperty(PROP_KEYSTORE_PROVIDER,
				    DEFAULT_KEYSTORE_PROVIDER);
    if (StringUtil.isNullString(provider)) {
      ks = KeyStore.getInstance(p.getProperty(PROP_KEYSTORE_TYPE,
					      DEFAULT_KEYSTORE_TYPE));
    } else {	
      ks = KeyStore.getInstance(p.getProperty(PROP_KEYSTORE_TYPE,
					      DEFAULT_KEYSTORE_TYPE),
				provider);
    }
    initializeKeyStore(ks, p);
    String keyStoreFileName = p.getProperty(PROP_KEYSTORE_FILE);
    if (!StringUtil.isNullString(keyStoreFileName)) { 
      storeKeyStore(ks, keyStoreFileName,
		    p.getProperty(PROP_KEYSTORE_PASSWORD));
    }
    return ks;
  }

  static void storeKeyStore(KeyStore keyStore,
			    String filename, String keyStorePassword)
      throws FileNotFoundException,
	     KeyStoreException,
	     NoSuchAlgorithmException,
	     CertificateException,
	     IOException {
    File keyStoreFile = new File(filename);
    OutputStream outs = null;
    try {
      log.debug3("Storing KeyStore in " + keyStoreFile);
      outs = new BufferedOutputStream(new FileOutputStream(keyStoreFile));
      keyStore.store(outs, keyStorePassword.toCharArray());
      outs.close();
    } finally {
      IOUtil.safeClose(outs);
    }
  }


  private static void initializeKeyStore(KeyStore keyStore, Properties p)
      throws CertificateException,
	     IOException,
	     InvalidKeyException,
	     KeyStoreException,
	     NoSuchAlgorithmException,
	     NoSuchProviderException,
	     SignatureException,
	     UnrecoverableKeyException {
    initializeKeyStore(keyStore, ConfigManager.fromProperties(p));
  }

  private static void initializeKeyStore(KeyStore keyStore,
					 Configuration config)
      throws CertificateException,
	     IOException,
	     InvalidKeyException,
	     KeyStoreException,
	     NoSuchAlgorithmException,
	     NoSuchProviderException,
	     SignatureException,
	     UnrecoverableKeyException {
    String keyAlias = config.get(PROP_KEY_ALIAS, DEFAULT_KEY_ALIAS);
    String certAlias = config.get(PROP_CERT_ALIAS, DEFAULT_CERT_ALIAS);
    String keyAlgName = config.get(PROP_KEY_ALGORITHM, DEFAULT_KEY_ALGORITHM);
    String sigAlgName = config.get(PROP_SIG_ALGORITHM, DEFAULT_SIG_ALGORITHM);
    String keyStorePassword = config.get(PROP_KEYSTORE_PASSWORD);
    String keyPassword = config.get(PROP_KEY_PASSWORD);
    int keyBits = config.getInt(PROP_KEY_BITS, DEFAULT_KEY_BITS);
    long expireIn = config.getTimeInterval(PROP_EXPIRE_IN, DEFAULT_EXPIRE_IN);
    String x500String = config.get(PROP_X500_NAME, DEFAULT_X500_NAME);

    CertAndKeyGen keypair = new CertAndKeyGen(keyAlgName, sigAlgName);
    keypair.generate(keyBits);

    PrivateKey privKey = keypair.getPrivateKey();
    log.debug3("PrivKey: " + privKey.getAlgorithm()
	       + " " + privKey.getFormat());

    X509Certificate[] chain = new X509Certificate[1];

    X500Name x500Name = new X500Name(x500String);
    chain[0] = keypair.getSelfCertificate(x500Name, expireIn);
    log.debug3("Certificate: " + chain[0].toString());

    keyStore.load(null, keyStorePassword.toCharArray());
    keyStore.setCertificateEntry(certAlias, chain[0]);
    keyStore.setKeyEntry(keyAlias, privKey,
			 keyPassword.toCharArray(), chain);
    Key myKey = keyStore.getKey(keyAlias, keyPassword.toCharArray());
    log.debug("MyKey: " + myKey.getAlgorithm() + " " + myKey.getFormat());
  }

}
