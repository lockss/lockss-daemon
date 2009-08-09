/*
 * $Id: KeyStoreUtil.java,v 1.4 2009-08-09 07:39:47 tlipkis Exp $
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


  // Moved here mostly intact from EditKeyStores so it can be used by other
  // code.  Should be integrated with methods above.
  public static void createPLNKeyStores(File inDir,
					File outDir,
					List hostlist)
      throws NoSuchAlgorithmException, NoSuchProviderException {

    String[] hosts = (String[])hostlist.toArray(new String[0]);
    KeyStore[] ks = new KeyStore[hosts.length];
    String[] pwd = new String[hosts.length];

    for (int i = 0; i < hosts.length; i++) {
      ks[i] = null;
      pwd[i] = null;
    }
    /*
     * Read in any existing keystores and their passwords
     */
    for (int i = 0; i < hosts.length; i++) {
      readKeyStore(hosts, ks, pwd, i, inDir);
    }
    /*
     * Create a password for each machine's keystore
     */
    for (int i = 0; i < hosts.length; i++) {
      if (pwd[i] == null) {
	pwd[i] = randomString(20);
      }
    }
    /*
     * Create a keystore for each machine with a certificate
     * and a private key.
     */
    for (int i = 0; i <hosts.length; i++) {
      if (ks[i] == null) {
	ks[i] = createKeystore(hosts[i], pwd[i]);
      }
    }
    /*
     * Build an array of the certificates
     */
    java.security.cert.Certificate[] cert =
      new X509Certificate[hosts.length];
    for (int i = 0; i < hosts.length; i++) {
      cert[i] = getCertificate(hosts, ks, i);
    }
    /*
     * Add all but the local certificate to the keyStore
     */
    for (int i = 0; i < hosts.length; i++) {
      addCertificates(hosts, ks[i], cert, i);
    }
    /*
     * Verify the key stores
     */
    boolean ok = true;
    for (int i = 0; i < hosts.length; i++) {
      if (!verifyKeyStore(hosts, ks, pwd, i)) {
	ok = false;
      }
    }
    if (ok) {
      /*
       * Write out each keyStore and its password
       */
      for (int i = 0; i < hosts.length; i++) {
	writeKeyStore(hosts, ks, pwd, i, outDir);
      }
    }
    for (int i = 0; i < hosts.length; i++) {
      listKeyStore(hosts, ks, pwd, i);
    }
  }

  private static String keyStoreType[] = { "JCEKS", "JKS" };
  private static String keyStoreSPI[]  = { "SunJCE", null };

  public static KeyStore createKeystore(String domainName, String password) {
    //  No KeyStore - make one.
    KeyStore ks = null;
    //  Will probably not work if JCEKS is not available
    for (int i = 0; i < keyStoreType.length; i++) {
      try {
	if (keyStoreSPI[i] == null) {
	  ks = KeyStore.getInstance(keyStoreType[i]);
	} else {
	  ks = KeyStore.getInstance(keyStoreType[i], keyStoreSPI[i]);
	}
      } catch (KeyStoreException e) {
	OUTdebug("KeyStore.getInstance(" + keyStoreType[i] + ") threw " + e);
      } catch (NoSuchProviderException e) {
	OUTdebug("KeyStore.getInstance(" + keyStoreType[i] + ") threw " + e);
      }
      if (ks != null) {
	OUTdebug("Using key store type " + keyStoreType[i]);
	break;
      }
    }
    if (ks == null) {
      OUTerror("No key store available");
      return null;  // will fail subsequently
    }
    String keyStoreFile = domainName + ".jceks";
    String keyStorePassword = domainName;
    initializeKeyStore(ks, domainName, password);
    return ks;
  }

  private static String keySuffix = ".key";
  private static String crtSuffix = ".crt";
  private static void initializeKeyStore(KeyStore keyStore,
					 String domainName, String password) {
    String keyAlias = domainName + keySuffix;
    String certAlias = domainName + crtSuffix;
    String keyStorePassword = domainName;
    String keyStoreFileName = domainName + ".jceks";
    File keyStoreFile = new File(keyStoreFileName);
    if (keyStoreFile.exists()) {
      OUTdebug("Key store file " + keyStoreFileName + " exists");
      return;
    }
    String keyAlgName = "RSA";
    String sigAlgName = "MD5WithRSA";
    OUTdebug("About to create a CertAndKeyGen: " + keyAlgName + " " + sigAlgName);
    CertAndKeyGen keypair;
    try {
      keypair = new CertAndKeyGen(keyAlgName, sigAlgName);
    } catch (NoSuchAlgorithmException e) {
      OUTdebug("new CertAndKeyGen(" + keyAlgName + "," + sigAlgName +
	       ") threw " + e);
      return;
    }
    OUTdebug("About to generate a key pair");
    try {
      keypair.generate(1024);
    } catch (InvalidKeyException e) {
      OUTdebug("keypair.generate(1024) threw " + e);
      return;
    }
    OUTdebug("About to get a PrivateKey");
    PrivateKey privKey = keypair.getPrivateKey();
    OUTdebug("MyKey: " + privKey.getAlgorithm() + " " +
	     privKey.getFormat());
    OUTdebug("About to get a self-signed certificate");
    X509Certificate[] chain = new X509Certificate[1];
    try {
      X500Name x500Name = new X500Name("CN=" + domainName + ", " +
				       "OU=LOCKSS Team, O=Stanford, " +
				       "L=Stanford, S=California, C=US");
      chain[0] = keypair.getSelfCertificate(x500Name, 365*24*60*60);
    } catch (IOException e) {
      OUTdebug("new X500Name() threw " + e);
      return;
    } catch (CertificateException e) {
      OUTdebug("keypair.getSelfCertificate() threw " + e);
      return;
    } catch (InvalidKeyException e) {
      OUTdebug("keypair.getSelfCertificate() threw " + e);
      return;
    } catch (SignatureException e) {
      OUTdebug("keypair.getSelfCertificate() threw " + e);
      return;
    } catch (NoSuchAlgorithmException e) {
      OUTdebug("keypair.getSelfCertificate() threw " + e);
      return;
    } catch (NoSuchProviderException e) {
      OUTdebug("keypair.getSelfCertificate() threw " + e);
      return;
    }
    OUTdebug("Certificate: " + chain[0].toString());
    OUTdebug("About to keyStore.load(null)");
    try {
      keyStore.load(null, keyStorePassword.toCharArray());
    } catch (IOException e) {
      OUTdebug("keyStore.load() threw " + e);
      return;
    } catch (CertificateException e) {
      OUTdebug("keyStore.load() threw " + e);
      return;
    } catch (NoSuchAlgorithmException e) {
      OUTdebug("keyStore.load() threw " + e);
      return;
    }
    OUTdebug("About to store " + certAlias + " in key store");
    try {
      keyStore.setCertificateEntry(certAlias, chain[0]);
    } catch (KeyStoreException e) {
      OUTdebug("keyStore.setCertificateEntry() threw " + e);
      return;
    }
    OUTdebug("About to store " + keyAlias + " in key store");
    try {
      keyStore.setKeyEntry(keyAlias, privKey,
			   password.toCharArray(), chain);
    } catch (KeyStoreException e) {
      OUTdebug("keyStore.setKeyEntry() threw " + e);
      return;
    }
    try {
      OUTdebug("About to getKeyEntry()");
      Key myKey = keyStore.getKey(keyAlias,
				  password.toCharArray());
      OUTdebug("MyKey: " + myKey.getAlgorithm() + " " +
	       myKey.getFormat());
    } catch (Throwable e) {
      OUTerror("getKeyEntry() threw: " + e);
    }
    OUTdebug("Done storing");
  }

  public static java.security.cert.Certificate
    getCertificate(String[] domainNames, KeyStore[] keyStores, int i) {
    java.security.cert.Certificate ret;
    String alias = domainNames[i] + crtSuffix;
    try {
      ret = keyStores[i].getCertificate(alias);
    } catch (KeyStoreException e) {
      OUTerror("keyStore.getCertificate(" + alias + ") threw: " + e);
      return null;
    }
    OUTdebug(alias + ": " + ret.getType());
    return ret;
  }

  public static void addCertificates(String[] domainNames,
				     KeyStore keyStore,
				     java.security.cert.Certificate[] certs,
				     int i) {
    for (int j = 0; j <domainNames.length; j++) {
      if (j != i) {
	String alias = domainNames[j] + crtSuffix;
	OUTdebug("About to store " + alias + " in keyStore for " +
		 domainNames[i]);
	try {
	  keyStore.setCertificateEntry(alias, certs[j]);
	} catch (KeyStoreException e) {
	  OUTdebug("keyStore.setCertificateEntry(" + alias + "," +
		   domainNames[i] + ") threw " + e);
	  return;
	}
      }
    }
  }

  private static void writeKeyStore(String domainNames[],
				    KeyStore kss[],
				    String passwords[],
				    int i,
				    File outDir) {
    String domainName = domainNames[i];
    KeyStore ks = kss[i];
    String password = passwords[i];
    if (domainName == null || ks == null || password == null) {
      return;
    }
    if (!outDir.exists() || !outDir.isDirectory()) {
      OUTerror("No directory " + outDir);
      return;
    }
    File keyStoreFile = new File(outDir, domainName + ".jceks");
    File passwordFile = new File(outDir, domainName + ".pass");
    String keyStorePassword = domainName;
    try {
      OUTdebug("Writing KeyStore to " + keyStoreFile);
      FileOutputStream fos = new FileOutputStream(keyStoreFile);
      ks.store(fos, keyStorePassword.toCharArray());
      fos.close();
      OUTdebug("Done storing KeyStore in " + keyStoreFile);
    } catch (Exception e) {
      OUTdebug("ks.store(" + keyStoreFile + ") threw " + e);
    }
    try {
      OUTdebug("Writing Password to " + passwordFile);
      PrintWriter pw = new PrintWriter(new FileOutputStream(passwordFile));
      pw.print(password);
      pw.close();
      OUTdebug("Done storing Password in " + passwordFile);
    } catch (Exception e) {
      OUTdebug("ks.store(" + passwordFile + ") threw " + e);
    }
  }

  private static void readKeyStore(String domainNames[],
				   KeyStore kss[],
				   String passwords[],
				   int i,
				   File inDir) {
    String domainName = domainNames[i];
    if (domainName == null) {
      return;
    }
    File keyStoreFile = new File(inDir, domainName + ".jceks");
    File passwordFile = new File(inDir, domainName + ".pass");
    String password = null;
    try {
      if (!passwordFile.exists() || !passwordFile.isFile()) {
	OUTdebug("No password file " + passwordFile);
	return;
      }
      OUTdebug("Trying to read password from " + passwordFile);
      FileInputStream fis = new FileInputStream(passwordFile);
      byte[] buf = new byte[fis.available()];
      int l = fis.read(buf);
      if (l != buf.length) {
	OUTdebug("password read short " + l + " != " + buf.length);
	return;
      }
      password = new String(buf);
    } catch (Exception e) {
      OUTdebug("Read password threw " + e);
      return;
    }
    KeyStore ks = null;
    try {
      ks = KeyStore.getInstance(keyStoreType[0], keyStoreSPI[0]);
      OUTdebug("Trying to read KeyStore from " + keyStoreFile);
      FileInputStream fis = new FileInputStream(keyStoreFile);
      ks.load(fis, domainName.toCharArray());
    } catch (Exception e) {
      OUTdebug("ks.load(" + keyStoreFile + ") threw " + e);
      return;
    }
    String keyStorePassword = domainName;
    passwords[i] = password;
    kss[i] = ks;
    OUTdebug("KeyStore and password for " + domainName + " read");
  }

  private static boolean verifyKeyStore(String domainNames[],
					KeyStore kss[],
					String passwords[],
					int i) {
    boolean ret = false;
    boolean[] hasKey = new boolean[domainNames.length];
    boolean[] hasCert = new boolean[domainNames.length];
    for (int j = 0; j < domainNames.length; j++) {
      hasKey[j] = false;
      hasCert[j] = false;
    }
    OUTdebug("start of key store verification for " + domainNames[i]);
    try {
      for (Enumeration en = kss[i].aliases(); en.hasMoreElements(); ) {
        String alias = (String) en.nextElement();
	OUTdebug("Next alias " + alias);
	int k = -1;
	for (int j = 0; j < domainNames.length; j++) {
	  if (alias.startsWith(domainNames[j])) {
	    k = j;
	  }
	}
	if (k < 0) {
	  OUTerror(alias + " not in domain names");
	  return ret;
	}
        if (kss[i].isCertificateEntry(alias)) {
	  OUTdebug("About to Certificate");
          java.security.cert.Certificate cert = kss[i].getCertificate(alias);
          if (cert == null) {
            OUTerror(alias + " null cert chain");
	    return ret;
	  }
	  OUTdebug("Cert for " + alias);
          hasCert[k] = true;
        } else if (kss[i].isKeyEntry(alias)) {
	  OUTdebug("About to getKey");
  	  Key privateKey = kss[i].getKey(alias, passwords[i].toCharArray());
	  if (privateKey != null) {
	    OUTdebug("Key for " + alias);
	    hasKey[k] = true;
	  } else {
	    OUTerror("No private key for " + alias);
	    return ret;
	  }
        } else {
  	  OUTerror(alias + " neither key nor cert");
	  return ret;
        }
      }
      OUTdebug("end of key store verification for "+ domainNames[i]);
    } catch (Exception ex) {
      OUTerror("listKeyStore() threw " + ex);
      return ret;
    }
    if (!hasKey[i]) {
      OUTdebug("no key for " + domainNames[i]);
      return ret;
    }
    for (int j = 0; j < domainNames.length; j++) {
      if (!hasCert[j]) {
	OUTdebug("no cert for " + domainNames[j]);
	return ret;
      }
    }
    ret = true;
    return ret;
  }

  private static void listKeyStore(String domainNames[],
				   KeyStore kss[],
				   String passwords[],
				   int i) {
    OUTdebug("start of key store for " + domainNames[i]);
    try {
      for (Enumeration en = kss[i].aliases(); en.hasMoreElements(); ) {
        String alias = (String) en.nextElement();
	OUTdebug("Next alias " + alias);
        if (kss[i].isCertificateEntry(alias)) {
	  OUTdebug("About to Certificate");
          java.security.cert.Certificate cert = kss[i].getCertificate(alias);
          if (cert == null) {
            OUTdebug(alias + " null cert chain");
          } else {
            OUTdebug("Cert for " + alias + " is " + cert.toString());
          }
        } else if (kss[i].isKeyEntry(alias)) {
	  OUTdebug("About to getKey");
  	  Key privateKey = kss[i].getKey(alias, passwords[i].toCharArray());
  	  OUTdebug(alias + " key " + privateKey.getAlgorithm() +
		   "/" + privateKey.getFormat());
        } else {
  	  OUTerror(alias + " neither key nor cert");
        }
      }
      OUTdebug("end of key store for "+ domainNames[i]);
    } catch (Exception ex) {
      OUTerror("listKeyStore() threw " + ex);
    }
  }
  private static void OUTdebug(String s) {
    if (false)
      System.err.println("debug:" + s);
  }
  private static void OUTerror(String s) {
    if (true)
      System.err.println("error: " + s);
  }
}
