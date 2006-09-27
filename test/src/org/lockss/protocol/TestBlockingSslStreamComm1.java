/*
 * $Id: TestBlockingSslStreamComm1.java,v 1.2 2006-09-27 18:46:39 dshr Exp $
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

package org.lockss.protocol;

/*
 * NB - this and its companion TestBlockingSslStreamComm2 should really be
 * two variants in one test.  They were separated because executing this
 * one first and the permanent keystore version second failed,  where in
 * the other order they both succeeded.  This seems to be some issue with
 * Junit class initialization,  but rather than tackling it I opted to
 * split them.  At some point these two files should be re-combined.
 */

import java.util.*;
import java.io.*;
import java.net.*;
import junit.framework.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.Queue;
import org.lockss.test.*;

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
 * This is the test class for org.lockss.protocol.BlockingStreamComm
 * when using the CLOCKSS temporary keystore
 */
public class TestBlockingSslStreamComm1 extends TestBlockingStreamComm {
  public static Class testedClasses[] = {
    BlockingStreamComm.class,
    BlockingPeerChannel.class,
  };

  TestBlockingSslStreamComm1(String name) {
    super(name);
  }

  void setupCommArrayEntry(int ix) {
    comms[ix] = new MyBlockingSslStreamComm(pids[ix]);
  }

  class MyBlockingSslStreamComm extends MyBlockingStreamComm {
    SocketFactory mySockFact;
    MyBlockingSslStreamComm(PeerIdentity localId) {
      super(localId);
    }

    SocketFactory getSocketFactory() {
      super.getSocketFactory();
      if (mySockFact == null) {
          mySockFact = new MySslSocketFactory(super.superSockFact);
      }
      return mySockFact;
    }
  }

  /** Socket factory creates either real or internal sockets, and
   * MyBlockingPeerChannels.
   */
  class MySslSocketFactory extends MySocketFactory {
    BlockingStreamComm.SocketFactory mySockFact;
    MySslSocketFactory(BlockingStreamComm.SocketFactory s) {
      super(s);
      mySockFact = s;
    }

    public ServerSocket newServerSocket(int port, int backlog)
          throws IOException {
        if (useInternalSockets) {
          return new InternalServerSocket(port, backlog);
        } else {
        ServerSocket ss = mySockFact.newServerSocket(port, backlog);
        assertTrue(ss instanceof SSLServerSocket);
        return ss;
        }
    }

    public Socket newSocket(IPAddr addr, int port) throws IOException {
        if (useInternalSockets) {
          return new InternalSocket(addr.getInetAddr(), port);
        } else {
        Socket s = mySockFact.newSocket(addr, port);
        assertTrue(s instanceof SSLSocket);
        return s;
        }
    }
  }


  /** SSL with temporary keystore */
  public static class SslStreamsTempKeys extends TestBlockingSslStreamComm1 {
    private static String PREFIX = "javax.net.ssl.keyStore";
    private static String PREFIX2 = "javax.net.ssl.trustStore";
    public SslStreamsTempKeys(String name) {
      super(name);
    }
    public void addSuiteProps(Properties p) {
      setupKeyStore(p);
      p.setProperty(BlockingStreamComm.PARAM_USE_V3_OVER_SSL, "true");
      p.setProperty(BlockingStreamComm.PARAM_SSL_TEMP_KEYSTORE, "true");
      p.setProperty(BlockingStreamComm.PARAM_USE_SSL_CLIENT_AUTH, "true");
    }
    //  Add any necessary properties to the system properties
    //  to describe the KeyStore to be used for SSL,  creating
    //  the KeyStore if necessary.
    private void setupKeyStore(Properties p) {
      String keyStoreFile = p.getProperty(PREFIX, null);
      if (keyStoreFile != null) {
        log.debug("Using KeyStore from p: " + keyStoreFile);
	return;
      }
      log.debug("No " + PREFIX + " in p");
      keyStoreFile = System.getProperty(PREFIX, null);
      if (keyStoreFile != null) {
        log.debug("Using KeyStore from system: " + keyStoreFile);
	return;
      }
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
	  log.debug("KeyStore.getInstance(" + keyStoreType[i] + ") threw " + e);
	} catch (NoSuchProviderException e) {
	  log.debug("KeyStore.getInstance(" + keyStoreType[i] + ") threw " + e);
	}
	if (ks != null) {
	  log.debug("Using key store type " + keyStoreType[i]);
	  break;
	}
      }
      if (ks == null) {
	log.error("No key store available");
	return;  // will fail subsequently
      }
      //  XXX should not be in /tmp
      keyStoreFile="/tmp/keystore";
      //  XXX generate random password
      String keyStorePassword = "A Very Bad Password";
      p.setProperty(PREFIX, keyStoreFile);
      p.setProperty(PREFIX + "Password", keyStorePassword);
      p.setProperty(PREFIX + "Type", ks.getType());
      p.setProperty(PREFIX2, keyStoreFile);
      p.setProperty(PREFIX2 + "Password", keyStorePassword);
      p.setProperty(PREFIX2 + "Type", ks.getType());
      System.setProperty(PREFIX, keyStoreFile);
      System.setProperty(PREFIX + "Password", keyStorePassword);
      System.setProperty(PREFIX + "Type", ks.getType());
      System.setProperty(PREFIX2, keyStoreFile);
      System.setProperty(PREFIX2 + "Password", keyStorePassword);
      System.setProperty(PREFIX2 + "Type", ks.getType());
      initializeKeyStore(ks, p);
    }

    private void initializeKeyStore(KeyStore keyStore, Properties p) {
      String keyAlias = "MyKey";
      String certAlias = "MyCert";
      String keyStorePassword = p.getProperty(PREFIX + "Password");
      String keyStoreFileName = p.getProperty(PREFIX);
      File keyStoreFile = new File(keyStoreFileName);
      if (keyStoreFile.exists()) {
	log.debug("About to load: " + keyStoreFileName);
	try {
	  keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword.toCharArray());
	} catch (IOException e) {
	  log.debug("keyStore.load() threw " + e);
	  return;
        } catch (CertificateException e) {
  	  log.debug("keyStore.load() threw " + e);
  	  return;
        } catch (NoSuchAlgorithmException e) {
  	  log.debug("keyStore.load() threw " + e);
  	  return;
	} catch (Throwable e) {
	  log.error("keyStore.load() threw: " + e);
	  e.printStackTrace();
	  return;
	}
	try {
          for (Enumeration e = keyStore.aliases(); e.hasMoreElements(); ) {
            String alias = (String)e.nextElement();
            log.debug("store contains: " + alias + (keyStore.isKeyEntry(alias) ? " key" : " cert"));
          }
	} catch (Throwable e) {
	  log.error("keyStore.{aliases,isKeyEntry}() threw: " + e);
	  e.printStackTrace();
	  return;
	}

	java.security.cert.Certificate cert;
	Key key;
	try {
	  cert = keyStore.getCertificate(certAlias);
	} catch (KeyStoreException e) {
	  log.error("keyStore.getCertificate(MyCert) threw: " + e);
	  return;
	}
	log.debug("MyCert: " + cert.getType());
	try {
	  log.debug("keyStore.isKeyEntry(" + keyAlias + ") = " + keyStore.isKeyEntry(keyAlias));
	  log.debug("About to  keyStore.getKey(MyKey," + keyStorePassword);
	  key = keyStore.getKey(keyAlias, keyStorePassword.toCharArray());
	} catch (KeyStoreException e) {
	  log.error("keyStore.getKey(MyKey) threw: " + e);
	  return;
	} catch (NoSuchAlgorithmException e) {
	  log.error("keyStore.getKey(MyKey) threw: " + e);
	  return;
	} catch (UnrecoverableKeyException e) {
	  log.error("keyStore.getKey(MyKey) threw: " + e);
	  return;
	} catch (Throwable e) {
	  log.error("keyStore.getKey(MyKey) threw: " + e);
	  e.printStackTrace();
	  return;
	}
	log.debug("MyKey: " + key.getAlgorithm() + " " + key.getFormat());
	return;
      }
      String keyAlgName = "RSA";
      String sigAlgName = "MD5WithRSA";
	log.debug("About to create a CertAndKeyGen: " + keyAlgName + " " + sigAlgName);
      CertAndKeyGen keypair;
      try {
        keypair = new CertAndKeyGen(keyAlgName, sigAlgName);
      } catch (NoSuchAlgorithmException e) {
	log.debug("new CertAndKeyGen(" + keyAlgName + "," + sigAlgName + ") threw " + e);
	return;
      }
	log.debug("About to generate a key pair");
      try {
        keypair.generate(1024);
      } catch (InvalidKeyException e) {
	log.debug("keypair.generate(1024) threw " + e);
	return;
      }
	log.debug("About to get a PrivateKey");
      PrivateKey privKey = keypair.getPrivateKey();
	log.debug("MyKey: " + privKey.getAlgorithm() + " " + privKey.getFormat());
	log.debug("About to get a self-signed certificate");
      X509Certificate[] chain = new X509Certificate[1];
      try {
        X500Name x500Name = new X500Name("CN=Test Key, OU=LOCKSS Team, O=Stanford, L=Stanford, S=California, C=US");
        chain[0] = keypair.getSelfCertificate(x500Name, 365*24*60*60);
      } catch (IOException e) {
	log.debug("new X500Name() threw " + e);
	return;
      } catch (CertificateException e) {
	log.debug("keypair.getSelfCertificate() threw " + e);
	return;
      } catch (InvalidKeyException e) {
	log.debug("keypair.getSelfCertificate() threw " + e);
	return;
      } catch (SignatureException e) {
	log.debug("keypair.getSelfCertificate() threw " + e);
	return;
      } catch (NoSuchAlgorithmException e) {
	log.debug("keypair.getSelfCertificate() threw " + e);
	return;
      } catch (NoSuchProviderException e) {
	log.debug("keypair.getSelfCertificate() threw " + e);
	return;
      }
	log.debug3("Certificate: " + chain[0].toString());
	log.debug("About to keyStore.load(null)");
      try {
	keyStore.load(null, keyStorePassword.toCharArray());
      } catch (IOException e) {
	log.debug("keyStore.load() threw " + e);
	return;
      } catch (CertificateException e) {
	log.debug("keyStore.load() threw " + e);
	return;
      } catch (NoSuchAlgorithmException e) {
	log.debug("keyStore.load() threw " + e);
	return;
      }
	log.debug("About to store " + certAlias + " in key store");
      try {
        keyStore.setCertificateEntry(certAlias, chain[0]);
      } catch (KeyStoreException e) {
	log.debug("keyStore.setCertificateEntry() threw " + e);
	return;
      }
	log.debug("About to store " + keyAlias + " in key store");
      try {
        keyStore.setKeyEntry(keyAlias, privKey, keyStorePassword.toCharArray(), chain);
      } catch (KeyStoreException e) {
	log.debug("keyStore.setKeyEntry() threw " + e);
	return;
      }
      try {
	log.debug("About to getKeyEntry()");
	Key myKey = keyStore.getKey(keyAlias, keyStorePassword.toCharArray());
	log.debug("MyKey: " + myKey.getAlgorithm() + " " + myKey.getFormat());
      } catch (Throwable e) {
	log.error("getKeyEntry() threw: " + e);
      }
	log.debug("Done storing");
      try {
	log.debug("Storing KeyStore in " + keyStoreFile);
	FileOutputStream fos = new FileOutputStream(keyStoreFile);
	keyStore.store(fos, keyStorePassword.toCharArray());
	fos.close();
	log.debug("Done storing KeyStore in " + keyStoreFileName);
      } catch (Exception e) {
	log.debug("ks.store(" + keyStoreFileName + ") threw " + e);
      }
    }
  }

  public static Test suite() {
    return variantSuites(new Class[] {
      SslStreamsTempKeys.class,
    });
  }
}
