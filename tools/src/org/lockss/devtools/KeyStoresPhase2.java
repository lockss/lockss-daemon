/*
 * $Id$
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

package org.lockss.devtools;

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.*;
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
import sun.security.pkcs.PKCS10;

/**
 * This program is invoked with a list of domain names.  For each
 * domain name,  it loads the keystore from the file with extension
 * .jceks, extracts the self-signed certificate,  and adds it to
 * the keystore for every other domain name.
 */

public class KeyStoresPhase2 {

    public static void main(String[] args) {
	KeyStore[] ks = new KeyStore[args.length];
	/*
	 * Create a keystore for each machine with a certificate
	 * and a private key.
	 */
	for (int i = 0; i <args.length; i++) {
	    ks[i] = loadKeystore(args[i]);
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
	 * Write out each keyStore
	 */
	for (int i = 0; i < args.length; i++) {
	    writeKeyStore(args[i], ks[i]);
	}
    }

    private static String PREFIX = "javax.net.ssl.keyStore";
    private static String PREFIX2 = "javax.net.ssl.trustStore";

    public static KeyStore loadKeystore(String domainName) {
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
	String keyStoreFileName = domainName + ".jceks";
	String keyStorePassword = domainName;
	System.setProperty(PREFIX, keyStoreFileName);
	System.setProperty(PREFIX + "Password", keyStorePassword);
	System.setProperty(PREFIX + "Type", ks.getType());
	System.setProperty(PREFIX2, keyStoreFileName);
	System.setProperty(PREFIX2 + "Password", keyStorePassword);
	System.setProperty(PREFIX2 + "Type", ks.getType());
	File keyStoreFile = new File(keyStoreFileName);
	if (keyStoreFile.exists()) {
	    try {
		FileInputStream fis = new FileInputStream(keyStoreFile);
		ks.load(fis, domainName.toCharArray());
		return ks;
	    } catch (IOException ex) {
		OUTdebug("loading keystore threw: " + ex);
	    } catch (NoSuchAlgorithmException ex) {
		OUTdebug("loading keystore threw: " + ex);
	    } catch (CertificateException ex) {
		OUTdebug("loading keystore threw: " + ex);
	    }
	}
	return null;
    }

    private static String keySuffix = ".key";
    private static String crtSuffix = ".crt";

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

    private static void writeKeyStore(String domainName, KeyStore ks) {
	String keyStoreFile = domainName + ".jceks";
	String keyStorePassword = "A Very Bad Password";
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
    }

    static void writePrivateKey(String domainName, PrivateKey key) {
	OUTdebug("writePrivateKey for " + domainName);
	try {
	    FileOutputStream fos =
		new FileOutputStream(domainName + ".priv");
	    fos.write(key.getEncoded());
	    fos.flush();
	    fos.close();
	} catch(IOException e) {
	    OUTdebug("write private key threw: " + e);
	}
	String keyStorePassword = System.getProperty(PREFIX + "Password");
	try {
	    FileOutputStream fos =
		new FileOutputStream(domainName + ".pass");
	    fos.write(keyStorePassword.getBytes());
	    fos.flush();
	    fos.close();
	} catch(IOException e) {
	    OUTdebug("write password threw: " + e);
	}
	
    }

    private static void OUTdebug(String s) {
	if (true)
	    System.err.println("debug:" + s);
    }
    private static void OUTerror(String s) {
	if (true)
	    System.err.println("error: " + s);
    }
}
