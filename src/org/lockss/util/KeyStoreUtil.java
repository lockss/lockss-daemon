/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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
import java.security.*;
import java.security.cert.*;
import java.lang.reflect.*;

import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.config.*;

/**
 * Utilities for creating keystores
 */
public class KeyStoreUtil {

  private static final Logger log = Logger.getLogger(KeyStoreUtil.class);


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
  /** Default SHA256WithRSA */
  public static final String PROP_SIG_ALGORITHM = "SigAlgorithm";
  /** X500Name.  Default 5 years */
  public static final String PROP_X500_NAME = "X500Name";
  /** Default 2048 */
  public static final String PROP_KEY_BITS = "KeyBits";
  /** Seconds.  Default 5 years */
  public static final String PROP_EXPIRE_IN = "ExpireIn";
  

  public static final String DEFAULT_KEYSTORE_TYPE = "JCEKS";
  public static final String DEFAULT_KEYSTORE_PROVIDER = "SunJCE";

  public static final String DEFAULT_KEY_ALIAS = "MyKey";
  public static final String DEFAULT_CERT_ALIAS = "MyCert";
  public static final String DEFAULT_KEY_ALGORITHM = "RSA";
  public static final String DEFAULT_SIG_ALGORITHM = "SHA256WithRSA";
  public static final String DEFAULT_X500_NAME = "CN=LOCKSS box";
  public static final int DEFAULT_KEY_BITS = 2048;
  public static final long DEFAULT_EXPIRE_IN = 5 * Constants.YEAR / 1000;


  public static String randomString(int len, SecureRandom rng) {
    return org.apache.commons.lang3.RandomStringUtils.random(len, 32, 126,
							     false, false,
							     null, rng);
  }

  public static String randomString(int len, LockssDaemon daemon)
      throws NoSuchAlgorithmException,
	     NoSuchProviderException {
    RandomManager rmgr = daemon.getRandomManager();
    SecureRandom rng = rmgr.getSecureRandom();
    return randomString(len, rng);
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
    storeKeyStore(keyStore, new File(filename), keyStorePassword);
  }

  static void storeKeyStore(KeyStore keyStore,
			    File keyStoreFile, String keyStorePassword)
      throws FileNotFoundException,
	     KeyStoreException,
	     NoSuchAlgorithmException,
	     CertificateException,
	     IOException {
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
					List hostlist,
					SecureRandom rng)
      throws Exception {

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
      Exception err = null;
      try {
	readKeyStore(hosts, ks, pwd, i, inDir);
      } catch (Exception e) {
	log.error("Couldn't read keystore for " + hosts[i], e);
	err = e;
      }
      if (err != null) {
	throw err;
      }
    }
    /*
     * Create a password for each machine's keystore
     */
    for (int i = 0; i < hosts.length; i++) {
      if (pwd[i] == null) {
	pwd[i] = randomString(20, rng);
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

  public static void createSharedPLNKeyStores(File dir,
					      List hostlist,
					      File pubKeyStoreFile,
					      String pubKeyStorePassword,
					      SecureRandom rng)
      throws Exception {

    String[] hosts = (String[])hostlist.toArray(new String[0]);
    KeyStore[] ks = new KeyStore[hosts.length];
    String[] pwd = new String[hosts.length];

    for (int i = 0; i < hosts.length; i++) {
      ks[i] = null;
      pwd[i] = null;
    }
    /*
     * Create or read the public keystore
     */
    KeyStore pubStore = createKeystore0();
    if (pubKeyStoreFile.exists()) {
      FileInputStream fis = new FileInputStream(pubKeyStoreFile);
      try {
	log.debug("Trying to read PubStore from " + pubKeyStoreFile);
	pubStore.load(fis, pubKeyStorePassword.toCharArray());
      } catch (Exception e) {
	log.debug("ks.load(" + pubKeyStoreFile + ")", e);
	throw e;
      } finally {
	IOUtil.safeClose(fis);
      }
    } else {
      pubStore.load(null, pubKeyStorePassword.toCharArray());
    }      
    /*
     * Create a password for each machine's keystore
     */
    for (int i = 0; i < hosts.length; i++) {
      if (pwd[i] == null) {
	pwd[i] = randomString(20, rng);
      }
    }
    /*
     * Create a keystore for each machine with a certificate
     * and a private key.
     */
    for (int i = 0; i <hosts.length; i++) {
      String host = hosts[i];
      String certAlias = host + crtSuffix;
      Properties p = new Properties();
      p.put(PROP_KEY_ALIAS, host + keySuffix);
      p.put(PROP_CERT_ALIAS, certAlias);
      p.put(PROP_KEY_PASSWORD, pwd[i]);
      p.put(PROP_KEYSTORE_PASSWORD, host);
      ks[i] = createKeyStore(p);

      /*
       * Add its certificate to the public keystore.
       */
      java.security.cert.Certificate cert = ks[i].getCertificate(certAlias);
      log.debug("About to store " + certAlias + " in keyStore for " +
		hosts[i]);
      try {
	pubStore.setCertificateEntry(certAlias, cert);
      } catch (KeyStoreException e) {
	log.debug("pubStore.setCertificateEntry(" + certAlias + "," +
		  host + ") threw " + e);
	throw e;
      }
    }
    /*
     * Write out each keyStore and its password
     */
    for (int i = 0; i < hosts.length; i++) {
      storeKeyStore(ks[i], new File(dir, hosts[i] + ".jceks"), hosts[i]);
      writePasswordFile(new File(dir, hosts[i] + ".pass"), pwd[i]);
    }
    storeKeyStore(pubStore, pubKeyStoreFile, pubKeyStorePassword);

    if (log.isDebug()) {
      for (int i = 0; i < hosts.length; i++) {
	listKeyStore(hosts, ks, pwd, i);
      }
    }
  }

  private static String keyStoreType[] = { "JCEKS", "JKS" };
  private static String keyStoreSPI[]  = { "SunJCE", null };

  private static KeyStore createKeystore(String domainName, String password)
      throws CertificateException,
	     IOException,
	     InvalidKeyException,
	     KeyStoreException,
	     NoSuchAlgorithmException,
	     NoSuchProviderException,
	     SignatureException,
	     UnrecoverableKeyException {
    KeyStore ks = createKeystore0();
    if (ks == null) {
      log.error("No key store available");
      return null;  // will fail subsequently
    }
    initializeKeyStore(ks, domainName, password);
    return ks;
  }

  private static KeyStore createKeystore0()
      throws CertificateException,
	     IOException,
	     InvalidKeyException,
	     KeyStoreException,
	     NoSuchAlgorithmException,
	     NoSuchProviderException,
	     SignatureException,
	     UnrecoverableKeyException {
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
	log.debug("KeyStore.getInstance(" + keyStoreType[i] + ") threw " + e);
	throw e;
      } catch (NoSuchProviderException e) {
	log.debug("KeyStore.getInstance(" + keyStoreType[i] + ") threw " + e);
	throw e;
      }
      if (ks != null) {
	log.debug("Using key store type " + keyStoreType[i]);
	break;
      }
    }
    return ks;
  }

  private static String keySuffix = ".key";
  private static String crtSuffix = ".crt";
  private static void initializeKeyStore(KeyStore keyStore,
					 String domainName, String password)
      throws IOException,
	     CertificateException,
	     InvalidKeyException,
	     SignatureException,
	     NoSuchAlgorithmException,
	     NoSuchProviderException,
	     KeyStoreException,
	     UnrecoverableKeyException {
    String keyAlias = domainName + keySuffix;
    String certAlias = domainName + crtSuffix;
    String keyStorePassword = domainName;
    String keyStoreFileName = domainName + ".jceks";
    File keyStoreFile = new File(keyStoreFileName);
    if (keyStoreFile.exists()) {
      log.debug("Key store file " + keyStoreFileName + " exists");
      throw new IOException("Key store file " + keyStoreFileName + " exists");
    }
    String keyAlgName = DEFAULT_KEY_ALGORITHM;
    String sigAlgName = DEFAULT_SIG_ALGORITHM;
    log.debug("About to create a CertAndKeyGen: " + keyAlgName + " " + sigAlgName);
    CertAndKeyGen keypair;
    try {
      keypair = new CertAndKeyGen(keyAlgName, sigAlgName);
    } catch (NoSuchAlgorithmException e) {
      log.debug("new CertAndKeyGen(" + keyAlgName + "," + sigAlgName +
	       ") threw " + e);
      throw e;
    }
    log.debug("About to generate a key pair");
    try {
      keypair.generate(1024);
    } catch (InvalidKeyException e) {
      log.debug("keypair.generate(1024) threw " + e);
      throw e;
    }
    log.debug("About to get a PrivateKey");
    PrivateKey privKey = keypair.getPrivateKey();
    log.debug("MyKey: " + privKey.getAlgorithm() + " " +
	      privKey.getFormat());
    log.debug("About to get a self-signed certificate");
    X509Certificate[] chain = new X509Certificate[1];
    X500Name x500Name = new X500Name("CN=" + domainName + ", " +
				     "OU=LOCKSS Team, O=Stanford, " +
				     "L=Stanford, S=California, C=US");
    chain[0] = keypair.getSelfCertificate(x500Name, 365*24*60*60);
    log.debug("Certificate: " + chain[0].toString());
    log.debug("About to keyStore.load(null)");
    try {
      keyStore.load(null, keyStorePassword.toCharArray());
    } catch (IOException e) {
      log.debug("keyStore.load() threw " + e);
      throw e;
    } catch (CertificateException e) {
      log.debug("keyStore.load() threw " + e);
      throw e;
    } catch (NoSuchAlgorithmException e) {
      log.debug("keyStore.load() threw " + e);
      throw e;
    }
    log.debug("About to store " + certAlias + " in key store");
    try {
      keyStore.setCertificateEntry(certAlias, chain[0]);
    } catch (KeyStoreException e) {
      log.debug("keyStore.setCertificateEntry() threw " + e);
      throw e;
    }
    log.debug("About to store " + keyAlias + " in key store");
    try {
      keyStore.setKeyEntry(keyAlias, privKey,
			   password.toCharArray(), chain);
    } catch (KeyStoreException e) {
      log.debug("keyStore.setKeyEntry() threw " + e);
      throw e;
    }
    log.debug("About to getKeyEntry()");
    Key myKey = keyStore.getKey(keyAlias,
				password.toCharArray());
    log.debug("MyKey: " + myKey.getAlgorithm() + " " +
	      myKey.getFormat());
    log.debug("Done storing");
  }

  public static java.security.cert.Certificate
    getCertificate(String[] domainNames, KeyStore[] keyStores, int i)
      throws KeyStoreException {
    java.security.cert.Certificate ret;
    String alias = domainNames[i] + crtSuffix;
    try {
      ret = keyStores[i].getCertificate(alias);
      log.debug(alias + ": " + ret.getType());
      return ret;
    } catch (KeyStoreException e) {
      log.error("keyStore.getCertificate(" + alias + ") threw: " + e);
      throw e;
    }
  }

  public static void addCertificates(String[] domainNames,
				     KeyStore keyStore,
				     java.security.cert.Certificate[] certs,
				     int i) throws KeyStoreException {
    KeyStoreException err = null;
    for (int j = 0; j <domainNames.length; j++) {
      if (j != i) {
	String alias = domainNames[j] + crtSuffix;
	log.debug("About to store " + alias + " in keyStore for " +
		 domainNames[i]);
	try {
	  keyStore.setCertificateEntry(alias, certs[j]);
	} catch (KeyStoreException e) {
	  log.debug("keyStore.setCertificateEntry(" + alias + "," +
		   domainNames[i] + ") threw " + e);
	  err = e;
	}
      }
    }
    if (err != null) {
      throw err;
    }
  }

  private static void writePasswordFile(File passwordFile,
					String password) {
    try {
      log.debug("Writing Password to " + passwordFile);
      PrintWriter pw = new PrintWriter(new FileOutputStream(passwordFile));
      pw.print(password);
      pw.close();
      log.debug("Done storing Password in " + passwordFile);
    } catch (Exception e) {
      log.debug("ks.store(" + passwordFile + ")", e);
    }
  }

  private static void writeKeyStore(String domainNames[],
				    KeyStore kss[],
				    String passwords[],
				    int i,
				    File outDir)
      throws FileNotFoundException {
    String domainName = domainNames[i];
    KeyStore ks = kss[i];
    String password = passwords[i];
    if (domainName == null || ks == null || password == null) {
      return;
    }
    if (!outDir.exists() || !outDir.isDirectory()) {
      log.error("No directory " + outDir);
      throw new FileNotFoundException("No directory " + outDir);
    }
    File keyStoreFile = new File(outDir, domainName + ".jceks");
    File passwordFile = new File(outDir, domainName + ".pass");
    String keyStorePassword = domainName;
    try {
      log.debug("Writing KeyStore to " + keyStoreFile);
      FileOutputStream fos = new FileOutputStream(keyStoreFile);
      ks.store(fos, keyStorePassword.toCharArray());
      fos.close();
      log.debug("Done storing KeyStore in " + keyStoreFile);
    } catch (Exception e) {
      log.debug("ks.store(" + keyStoreFile + ") threw " + e);
    }
    writePasswordFile(passwordFile, password);
  }

  private static void readKeyStore(String domainNames[],
				   KeyStore kss[],
				   String passwords[],
				   int i,
				   File inDir) throws Exception {
    String domainName = domainNames[i];
    if (domainName == null) {
      return;
    }
    File keyStoreFile = new File(inDir, domainName + ".jceks");
    File passwordFile = new File(inDir, domainName + ".pass");
    String password = null;
    try {
      if (!passwordFile.exists() || !passwordFile.isFile()) {
	log.debug("No password file " + passwordFile);
	return;
      }
      log.debug("Trying to read password from " + passwordFile);
      FileInputStream fis = new FileInputStream(passwordFile);
      byte[] buf = new byte[fis.available()];
      int l = fis.read(buf);
      if (l != buf.length) {
	log.debug("password read short " + l + " != " + buf.length);
	return;
      }
      password = new String(buf);
    } catch (IOException e) {
      log.debug("Read password threw " + e);
      throw e;
    }
    KeyStore ks = null;
    try {
      ks = KeyStore.getInstance(keyStoreType[0], keyStoreSPI[0]);
      log.debug("Trying to read KeyStore from " + keyStoreFile);
      FileInputStream fis = new FileInputStream(keyStoreFile);
      ks.load(fis, domainName.toCharArray());
    } catch (Exception e) {
      log.debug("ks.load(" + keyStoreFile + ") threw " + e);
      throw e;
    }
    String keyStorePassword = domainName;
    passwords[i] = password;
    kss[i] = ks;
    log.debug("KeyStore and password for " + domainName + " read");
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
    log.debug("start of key store verification for " + domainNames[i]);
    try {
      for (Enumeration en = kss[i].aliases(); en.hasMoreElements(); ) {
        String alias = (String) en.nextElement();
	log.debug("Next alias " + alias);
	int k = -1;
	for (int j = 0; j < domainNames.length; j++) {
	  if (alias.startsWith(domainNames[j])) {
	    k = j;
	  }
	}
	if (k < 0) {
	  log.error(alias + " not in domain names");
	  return ret;
	}
        if (kss[i].isCertificateEntry(alias)) {
	  log.debug("About to Certificate");
          java.security.cert.Certificate cert = kss[i].getCertificate(alias);
          if (cert == null) {
            log.error(alias + " null cert chain");
	    return ret;
	  }
	  log.debug("Cert for " + alias);
          hasCert[k] = true;
        } else if (kss[i].isKeyEntry(alias)) {
	  log.debug("About to getKey");
  	  Key privateKey = kss[i].getKey(alias, passwords[i].toCharArray());
	  if (privateKey != null) {
	    log.debug("Key for " + alias);
	    hasKey[k] = true;
	  } else {
	    log.error("No private key for " + alias);
	    return ret;
	  }
        } else {
  	  log.error(alias + " neither key nor cert");
	  return ret;
        }
      }
      log.debug("end of key store verification for "+ domainNames[i]);
    } catch (Exception ex) {
      log.error("listKeyStore() threw " + ex);
      return ret;
    }
    if (!hasKey[i]) {
      log.debug("no key for " + domainNames[i]);
      return ret;
    }
    for (int j = 0; j < domainNames.length; j++) {
      if (!hasCert[j]) {
	log.debug("no cert for " + domainNames[j]);
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
    log.debug("start of key store for " + domainNames[i]);
    try {
      for (Enumeration en = kss[i].aliases(); en.hasMoreElements(); ) {
        String alias = (String) en.nextElement();
	log.debug("Next alias " + alias);
        if (kss[i].isCertificateEntry(alias)) {
	  log.debug("About to getCertificate");
          java.security.cert.Certificate cert = kss[i].getCertificate(alias);
          if (cert == null) {
            log.debug(alias + " null cert chain");
          } else {
            log.debug("Cert for " + alias + " is " + cert.toString());
          }
        } else if (kss[i].isKeyEntry(alias)) {
	  log.debug("About to getKey");
  	  Key privateKey = kss[i].getKey(alias, passwords[i].toCharArray());
  	  log.debug(alias + " key " + privateKey.getAlgorithm() +
		   "/" + privateKey.getFormat());
        } else {
  	  log.error(alias + " neither key nor cert");
        }
      }
      log.debug("end of key store for "+ domainNames[i]);
    } catch (Exception ex) {
      log.error("listKeyStore() threw " + ex);
    }
  }

  // Quick fix for Java 1.7.0_111.  Wrappers for CertAndKeyGen and X500Name
  // which dispatch to whichever of sun.security.x509 or
  // sun.security.tools.keytool exists at runtime.

  static final String PKG_NAME_1 = "sun.security.x509.";
  static final String PKG_NAME_2 = "sun.security.tools.keytool.";
  static final String CertAndKeyGenName = "CertAndKeyGen";
  static final String X500NameName = "X500Name";

  static Class cakgClass;
  static Class x500Class;
  static {
    cakgClass = firstClass(new String[] {PKG_NAME_1 + CertAndKeyGenName,
					 PKG_NAME_2 + CertAndKeyGenName});
    x500Class = firstClass(new String[] {PKG_NAME_1 + X500NameName,
					 PKG_NAME_2 + X500NameName});
  }

  static Class firstClass(String[] names) {
    for (String name : names) {
      try {
	return Class.forName(name);
      } catch (ClassNotFoundException e) {
      }
    }
    return null;
  }

  public static class CertAndKeyGen {
    Object cakg;

    public CertAndKeyGen(String keyType, String sigAlg)
	throws NoSuchAlgorithmException {
      try {
	Constructor constr = cakgClass.getConstructor(String.class,
						      String.class);
	cakg = constr.newInstance(keyType, sigAlg);
      } catch (NoSuchMethodException |
	       InstantiationException |
	       IllegalAccessException e) {
	log.error("Couldn't invoke CertAndKeyGen constructor", e);
	throw new IllegalStateException(e);
      } catch (InvocationTargetException e) {
	Throwable cause = e.getCause();
	try {
	  throw cause;
	} catch (NoSuchAlgorithmException cc) {
	  throw cc;
	} catch (RuntimeException cc) {
	  throw cc;
	} catch (Throwable cc) {
	  throw new IllegalStateException(cc);
	}
      }
    }

    public void generate(int keyBits) throws InvalidKeyException {
      try {
	Method meth = cakgClass.getMethod("generate", int.class);
	meth.invoke(cakg, keyBits);
      } catch (NoSuchMethodException |
	       IllegalAccessException e) {
	log.error("Couldn't invoke CertAndKeyGen.generate()", e);
	throw new IllegalStateException(e);
      } catch (InvocationTargetException e) {
	Throwable cause = e.getCause();
	try {
	  throw cause;
	} catch (InvalidKeyException cc) {
	  throw cc;
	} catch (RuntimeException cc) {
	  throw cc;
	} catch (Throwable cc) {
	  throw new IllegalStateException(cc);
	}
      }
    }

    public PrivateKey getPrivateKey () {
      try {
	Method meth = cakgClass.getMethod("getPrivateKey");
	return (PrivateKey)meth.invoke(cakg);
      } catch (NoSuchMethodException |
	       InvocationTargetException |
	       IllegalAccessException e) {
	log.error("Couldn't invoke CertAndKeyGen.getPrivateKey()", e);
	throw new IllegalStateException(e);
      }
    }

    public X509Certificate getSelfCertificate(X500Name myname, long validity)
	throws CertificateException, InvalidKeyException, SignatureException,
	       NoSuchAlgorithmException, NoSuchProviderException {
      try {
	Method meth = cakgClass.getMethod("getSelfCertificate",
					  x500Class, long.class);
 	return
	  (X509Certificate)meth.invoke(cakg, myname.getX500Name(), validity);
      } catch (NoSuchMethodException |
	       IllegalAccessException e) {
	log.error("Couldn't invoke CertAndKeyGen.getSelfCertificate()", e);
	throw new IllegalStateException(e);
      } catch (InvocationTargetException e) {
	Throwable cause = e.getCause();
	try {
	  throw cause;
	} catch (CertificateException cc) {
	  throw cc;
	} catch (InvalidKeyException cc) {
	  throw cc;
	} catch (SignatureException cc) {
	  throw cc;
	} catch (NoSuchAlgorithmException cc) {
	  throw cc;
	} catch (NoSuchProviderException cc) {
	  throw cc;
	} catch (RuntimeException cc) {
	  throw cc;
	} catch (Throwable cc) {
	  throw new IllegalStateException(cc);
	}
      }
    }
  }

  public static class X500Name {
    Object x5n;

    public X500Name(String name) throws IOException {
      try {
	Constructor constr = x500Class.getConstructor(String.class);
	x5n = constr.newInstance(name);
      } catch (NoSuchMethodException |
	       InstantiationException |
	       IllegalAccessException e) {
	log.error("Couldn't invoke X500Name constructor", e);
	throw new IllegalStateException(e);
      } catch (InvocationTargetException e) {
	Throwable cause = e.getCause();
	try {
	  throw cause;
	} catch (IOException cc) {
	  throw cc;
	} catch (RuntimeException cc) {
	  throw cc;
	} catch (Throwable cc) {
	  throw new IllegalStateException(cc);
	}
      }
    }

    public X500Name(String commonName, String organizationUnit,
                    String organizationName, String localityName,
                    String stateName, String country)
	throws IOException {
      try {
	Constructor constr = x500Class.getConstructor(String.class,
						      String.class,
						      String.class,
						      String.class,
						      String.class,
						      String.class);
	x5n = constr.newInstance(commonName, organizationUnit,
				 organizationName, localityName,
				 stateName, country);
      } catch (NoSuchMethodException |
	       InstantiationException |
	       IllegalAccessException e) {
	log.error("Couldn't invoke X500Name constructor", e);
	throw new IllegalStateException(e);
      } catch (InvocationTargetException e) {
	Throwable cause = e.getCause();
	try {
	  throw cause;
	} catch (IOException cc) {
	  throw cc;
	} catch (RuntimeException cc) {
	  throw cc;
	} catch (Throwable cc) {
	  throw new IllegalStateException(cc);
	}
      }
    }

    Object getX500Name() {
      return x5n;
    }
  }
}
