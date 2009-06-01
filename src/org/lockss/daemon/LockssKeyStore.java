/*
 * $Id: LockssKeyStore.java,v 1.1 2009-06-01 07:48:32 tlipkis Exp $
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

import java.io.*;
import java.util.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;

import org.lockss.util.*;

/** Wrapper around a keystore to manager passwords and loading and manager
 * KeyManagerFactory */
public class LockssKeyStore {
  protected static Logger log = Logger.getLogger("LockssKeyStore");

  String name;
  String type;
  String provider;
  String filename;
  String password;
  String keyPassword;
  String keyPasswordFile;
  KeyStore keystore;
  KeyManagerFactory kmf;
  TrustManagerFactory tmf;
  boolean initted = false;

  LockssKeyStore(String name) {
    if (name == null) {
      throw new NullPointerException();
    }
    this.name = name;
  }

  String getName() {
    return name;
  }

  void setName(String val) {
    name = val;
  }

  String getType() {
    return type;
  }

  void setType(String val) {
    type = val;
  }

  String getProvider() {
    if (provider == null) {
      if ("JKS".equals(type)) {
	return null;
      }
      if ("JCEKS".equals(type)) {
	return "SunJCE";
      }
    }
    return provider;
  }

  void setProvider(String val) {
    provider = val;
  }

  String getFilename() {
    return filename;
  }

  void setFilename(String val) {
    filename = val;
  }

  void setPassword(String val) {
    password = val;
  }

  void setKeyPassword(String val) {
    keyPassword = val;
  }

  void setKeyPasswordFile(String val) {
    keyPasswordFile = val;
  }

  KeyManagerFactory getKeyManagerFactory() {
    return kmf;
  }

  TrustManagerFactory getTrustManagerFactory() {
    return tmf;
  }

  /** Load the keystore from a file */
  synchronized void init() throws UnavailableKeyStoreException {
    if (initted) {
      return;
    }
    check();
    try {
      if (keyPassword == null) {
	keyPassword = readPasswdFile();
      }
      createKeyStore();
      createKeyManagerFactory();
      createTrustManagerFactory();
      initted = true;
    } catch (Exception e) {
      throw new UnavailableKeyStoreException(e);
    }
  }

  // check for required params
  void check() {
    if (filename == null
	|| password == null
	|| keyPassword == null && keyPasswordFile == null) {
      throw new NullPointerException("filename: " + filename + ", " + "password: " + password + ", " + "keyPassword: " + keyPassword + ", " + "keyPasswordFile: " + keyPasswordFile);
    }
  }

  /** Read password from file, then overwrite and delete file */
  String readPasswdFile() throws IOException {
    File file = new File(keyPasswordFile);
    FileInputStream fis = new FileInputStream(file);
    long llen = file.length();
    if (llen > 1000) {
      throw new IOException("Unreasonably large password file: " + llen);
    }
    int len = (int)llen;
    byte[] pwdChars = new byte[len];
    try {
      try {
	int nread = StreamUtil.readBytes(fis, pwdChars, len);
	if (nread != len) {
	  throw new IOException("short read");
	}
      } finally {
	IOUtil.safeClose(fis);
      }
    } finally {
      overwriteAndDelete(keyPasswordFile, len);
    }
    return new String(pwdChars);
  }

  /** Overwrite and delete file, trap and log any exceptions */
  void overwriteAndDelete(String filename, int len) {
    File file = new File(filename);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      byte[] junk = new byte[len];
      for (int i = 0; i < len; i++) junk[i] = 0;
      fos.write(junk);
    } catch (IOException e) {
      log.error("Couldn't overwrite file: " + file, e);
    } finally {
      IOUtil.safeClose(fos);
    }
    file.delete();
  }

  /** Load the keystore from the file */
  void createKeyStore()
      throws KeyStoreException,
	     IOException,
	     NoSuchAlgorithmException,
	     NoSuchProviderException,
	     CertificateException {
    InputStream ins = null;
    try {
      KeyStore ks;
      if (getProvider() == null) {
	ks = KeyStore.getInstance(getType());
      } else {
	ks = KeyStore.getInstance(getType(), getProvider());
      }
      ins = new BufferedInputStream(new FileInputStream(new File(filename)));
      ks.load(ins, password.toCharArray());
      keystore = ks;
    } finally {
      IOUtil.safeClose(ins);
    }
  }

  /** Create a KeyManagerFactory from the keystore and key password */
  void createKeyManagerFactory()
      throws NoSuchAlgorithmException,
	     KeyStoreException,
	     UnrecoverableKeyException {
    kmf =
      KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keystore, keyPassword.toCharArray());
  }

  /** Create a TrustManagerFactory from the keystore and key password */
  void createTrustManagerFactory()
      throws KeyStoreException,
	     NoSuchAlgorithmException,
	     CertificateException {
    tmf = 
      TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(keystore);
  }

  // private debug output of keystore
  private void logKeyStore(KeyStore ks, char[] keyPassword) {
    log.debug3("start of key store");
    try {
      for (Enumeration en = ks.aliases(); en.hasMoreElements(); ) {
        String alias = (String)en.nextElement();
        if (ks.isCertificateEntry(alias)) {
          java.security.cert.Certificate cert = ks.getCertificate(alias);
          if (cert == null) {
	    log.debug3("Null cert chain for: " + alias);
          } else {
            log.debug3("Cert chain for " + alias + " is " + cert);
          }
        } else if (ks.isKeyEntry(alias)) {
  	  Key privateKey = ks.getKey(alias, keyPassword);
  	  log.debug3(alias + " key " + privateKey.getAlgorithm()
		     + "/" + privateKey.getFormat());
        } else {
  	  log.debug3(alias + " neither key nor cert");
        }
      }
    } catch (Exception e) {
      log.error("logKeyStore() threw", e);
    }
  }

  public class UnavailableKeyStoreException extends Exception {
    UnavailableKeyStoreException(Throwable cause) {
      super(cause);
    }
  }

}
