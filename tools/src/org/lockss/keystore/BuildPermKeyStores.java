/*
 * $Id: BuildPermKeyStores.java,v 1.1 2007-08-03 23:06:35 dshr Exp $
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

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;
import sun.security.x509.X500Name;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.X509Key;
import sun.security.x509.X500Signer;
import sun.security.pkcs.PKCS10;
import sun.security.provider.IdentityDatabase;
import sun.security.provider.SystemSigner;

/**
 * A tool to build key stores for V3 over SSL support in CLOCKSS
 */

public class BuildPermKeyStores {

    public static void main(String[] args) {
	KeyStore[] ks = new KeyStore[args.length];
	String[] pwd = new String[args.length];
	/*
	 * Create a password for each machine's keystore
	 */
	for (int i = 0; i < args.length; i++) {
		pwd[i] = randomString();
	}
	/*
	 * Create a keystore for each machine with a certificate
	 * and a private key.
	 */
	for (int i = 0; i <args.length; i++) {
	    ks[i] = createKeystore(args[i], pwd[i]);
	}
	/*
	 * Build an array of the certificates
	 */
	java.security.cert.Certificate[] cert =
	    new X509Certificate[args.length];
	for (int i = 0; i < args.length; i++) {
	    cert[i] = getCertificate(args, ks, i);
	}
	/*
	 * Add all but the local certificate to the keyStore
	 */
	for (int i = 0; i < args.length; i++) {
	    addCertificates(args, ks[i], cert, i);
	}
	/*
	 * Write out each keyStore and its password
	 */
	for (int i = 0; i < args.length; i++) {
	    writeKeyStore(args[i], ks[i], pwd[i]);
	}
    }

    private static String PREFIX = "javax.net.ssl.keyStore";
    private static String PREFIX2 = "javax.net.ssl.trustStore";

    public static String randomString() {
	    String ret = null;
	    SecureRandom rng = null;
	    try {
		rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
	    } catch (NoSuchAlgorithmException ex) {
		OUTerror("SecureRandom threw: " + ex);
		return (ret);
	    } catch (NoSuchProviderException ex) {
		OUTerror("SecureRandom threw: " + ex);
		return (ret);
	    }
	    byte[] rand = new byte[20];
	    rng.nextBytes(rand);
	    for (int i = 0; i < rand.length; i++) {
	      if (rand[i] < 0) {
		rand[i] = (byte) (128 + (int)rand[i]);
	      }
	      if (rand[i] == 127) {
		rand[i] = 0;
	      }
	      if (rand[i] < 32) {
		rand[i] += 32;
	      }
	    }
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    baos.write(rand, 0, rand.length);
	    try {
	        ret = baos.toString("ASCII");
	    } catch (UnsupportedEncodingException ex) {
		OUTerror("ByteArrayOutputStream.toString threw: " + ex);
	    }
	    OUTdebug("password is " + (ret == null ? "null" : ret));
	    return (ret);
    }

    public static KeyStore createKeystore(String domainName, String password) {
	//  No KeyStore - make one.
	KeyStore ks = null;
	//  Will probably not work if JCEKS is not available
	String keyStoreType[] = { "JCEKS", "JKS" };
	String keyStoreSPI[]  = { "SunJCE", null };
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
	String keyStorePassword = password;
	System.setProperty(PREFIX, keyStoreFile);
	System.setProperty(PREFIX + "Password", keyStorePassword);
	System.setProperty(PREFIX + "Type", ks.getType());
	System.setProperty(PREFIX2, keyStoreFile);
	System.setProperty(PREFIX2 + "Password", keyStorePassword);
	System.setProperty(PREFIX2 + "Type", ks.getType());
	initializeKeyStore(ks, domainName, password);
	return ks;
    }

    private static String keySuffix = ".key";
    private static String crtSuffix = ".crt";
    private static void initializeKeyStore(KeyStore keyStore,
					   String domainName, String password) {
	String keyAlias = domainName + keySuffix;
	String certAlias = domainName + crtSuffix;
	String keyStorePassword = password;
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
				 keyStorePassword.toCharArray(), chain);
	} catch (KeyStoreException e) {
	    OUTdebug("keyStore.setKeyEntry() threw " + e);
	    return;
	}
	try {
	    OUTdebug("About to getKeyEntry()");
	    Key myKey = keyStore.getKey(keyAlias,
					keyStorePassword.toCharArray());
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

    private static void writeKeyStore(String domainName, KeyStore ks, String password) {
	String keyStoreFile = domainName + ".jceks";
	String passwordFile = domainName + ".pass";
	String keyStorePassword = password;
	System.setProperty(PREFIX, keyStoreFile);
	System.setProperty(PREFIX + "Password", keyStorePassword);
	System.setProperty(PREFIX + "Type", ks.getType());
	System.setProperty(PREFIX2, keyStoreFile);
	System.setProperty(PREFIX2 + "Password", keyStorePassword);
	System.setProperty(PREFIX2 + "Type", ks.getType());
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
	    pw.println(keyStorePassword);
	    pw.close();
	    OUTdebug("Done storing Password in " + passwordFile);
	} catch (Exception e) {
	    OUTdebug("ks.store(" + passwordFile + ") threw " + e);
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
