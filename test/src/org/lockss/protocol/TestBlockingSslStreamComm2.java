/*
 * $Id: TestBlockingSslStreamComm2.java,v 1.3 2006-09-28 02:00:04 tlipkis Exp $
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
 * NB - this and its companion TestBlockingSslStreamComm1 should really be
 * two variants in one test.  They were separated because executing this
 * one second and the temporary keystore version first failed,  where in
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

import sun.security.x509.*;
import sun.security.pkcs.PKCS10;
import sun.security.provider.IdentityDatabase;
import sun.security.provider.SystemSigner;

/**
 * This is the test class for org.lockss.protocol.BlockingStreamComm
 * when using the permanent CLOCKSS keystore
 */
public class TestBlockingSslStreamComm2 extends TestBlockingStreamComm {
  private static String tempDirPath = "/tmp/";
  protected static SSLSocketFactory mySslSocketFactory = null;
  protected static SSLServerSocketFactory mySslServerSocketFactory = null;

  public static Class testedClasses[] = {
    BlockingStreamComm.class,
    BlockingPeerChannel.class,
  };

  TestBlockingSslStreamComm2(String name) {
    super(name);
    try {
      tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    } catch (IOException e) {
      // do nothing
    }
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

    public void setConfig(Configuration config,
                          Configuration prevConfig,
                          Configuration.Differences changedKeys) {
      if (mySslServerSocketFactory != null) {
        //  This is a second or subsequent BlockingStreamComm
        //  so we have to fake its initialization.
        sslServerSocketFactory = mySslServerSocketFactory;
        sslSocketFactory = mySslSocketFactory;
        log.debug("Reusing socket factories " + sslServerSocketFactory + " & " + sslSocketFactory);
      }
      super.setConfig(config, prevConfig, changedKeys);
      if (mySslServerSocketFactory == null) {
        //  This is the first BlockingStreamComm instance
        mySslServerSocketFactory = sslServerSocketFactory;
        mySslSocketFactory = sslSocketFactory;
        log.debug("Remembering socket factories " + sslServerSocketFactory + " & " + sslSocketFactory);
      }
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

  /** SSL with permanent keystore */
  public static class SslStreamsPermKeys extends TestBlockingSslStreamComm2 {

    File keyStoreFile = null;
    String keyStoreFileName = null;
    String keyStorePassWord = "localhost";
    File privateKeyPassWordFile = null;
    String privateKeyPassWordFileName = null;
    String privateKeyPassWord = "MoreBadPassWord";

    File badKeyStoreFile = null;
    String badKeyStoreFileName = null;
    String badKeyStorePassWord = "not localhost";
    File badPrivateKeyPassWordFile = null;
    String badPrivateKeyPassWordFileName = null;
    String badPrivateKeyPassWord = "not MoreBadPassWord";

    public SslStreamsPermKeys(String name) {
      super(name);
    }

    public void addSuiteProps(Properties p) {
      if (setupGoodKeyStore() && setupBadKeyStore()) {
        p.setProperty(BlockingStreamComm.PARAM_USE_V3_OVER_SSL, "true");
	p.setProperty(BlockingStreamComm.PARAM_SSL_TEMP_KEYSTORE, "false");
	p.setProperty(BlockingStreamComm.PARAM_USE_SSL_CLIENT_AUTH, "true");
	/* File name for SSL key store */
	p.setProperty(BlockingStreamComm.PARAM_SSL_KEYSTORE, keyStoreFileName);
	/* File name for SSL key store password **/
	p.setProperty(BlockingStreamComm.PARAM_SSL_PRIVATE_KEY_PASSWORD_FILE,
		      privateKeyPassWordFileName);
	/* SSL protocol to use **/
	p.setProperty(BlockingStreamComm.PARAM_SSL_PROTOCOL, "TLSv1");
	p.setProperty("org.lockss.platform.fqdn", keyStorePassWord);
      } else {
	fail("can't set up key store");
      }
    }

    //  Create the good KeyStore
    private boolean setupGoodKeyStore() {
      // Create a file for the private key password and put it there
      privateKeyPassWordFileName = tempDirPath + "private.key";
      try {
        privateKeyPassWordFile = new File(privateKeyPassWordFileName);
        privateKeyPassWordFile.deleteOnExit();
        //  Write the private key password to the file
        if (!privateKeyPassWordFile.exists()) {
  	  log.debug("Storing private key password in " +
  		    privateKeyPassWordFileName);
  	  FileOutputStream fos = new FileOutputStream(privateKeyPassWordFile);
  	  fos.write(privateKeyPassWord.getBytes());
  	  fos.close();
  	  log.debug("Done storing private key password in " +
  		    privateKeyPassWordFileName);
        }
      } catch (Exception e) {
	log.error(privateKeyPassWordFileName + " threw " + e);
	return false;
      }
      //  Create a temporary file to hold the key store
      keyStoreFileName = tempDirPath + "keystore.jceks";
      keyStoreFile = new File(keyStoreFileName);
      keyStoreFile.deleteOnExit();
      if (keyStoreFile.exists()) {
        return true;
      }
      // Create a CertAndKeyGen instance
      String keyAlgName = "RSA";
      String sigAlgName = "MD5WithRSA";
      log.debug("About to create a CertAndKeyGen: " + keyAlgName +
		" " + sigAlgName);
      CertAndKeyGen keypair;
      try {
        keypair = new CertAndKeyGen(keyAlgName, sigAlgName);
      } catch (NoSuchAlgorithmException e) {
	log.error("new CertAndKeyGen(" + keyAlgName + "," +
		  sigAlgName + ") threw " + e);
	return false;
      }
      // Generate a key pair
      log.debug("About to generate a key pair");
      try {
        keypair.generate(1024);
      } catch (InvalidKeyException e) {
	log.error("keypair.generate(1024) threw " + e);
	return false;
      }
      // Extract the private key
      log.debug("About to get a PrivateKey");
      PrivateKey privKey = keypair.getPrivateKey();
      log.debug("PrivateKey: " + privKey.getAlgorithm() + " " +
		privKey.getFormat());
      log.debug("About to get a self-signed certificate");
      X509Certificate[] chain = new X509Certificate[1];
      try {
	String certName =
	    "CN=Test Key, OU=LOCKSS Team, O=Stanford, L=Stanford, " +
	    "S=California, C=US";
        X500Name x500Name = new X500Name(certName);
        chain[0] = keypair.getSelfCertificate(x500Name, 365*24*60*60);
      } catch (IOException e) {
	log.error("new X500Name() threw " + e);
	return false;
      } catch (CertificateException e) {
	log.error("keypair.getSelfCertificate() threw " + e);
	return false;
      } catch (InvalidKeyException e) {
	log.error("keypair.getSelfCertificate() threw " + e);
	return false;
      } catch (SignatureException e) {
	log.error("keypair.getSelfCertificate() threw " + e);
	return false;
      } catch (NoSuchAlgorithmException e) {
	log.error("keypair.getSelfCertificate() threw " + e);
	return false;
      } catch (NoSuchProviderException e) {
	log.error("keypair.getSelfCertificate() threw " + e);
	return false;
      }
      log.debug3("chain[0]: " + chain[0].toString());
      // Create the key store
      KeyStore keyStore = null;
      try {
        keyStore = KeyStore.getInstance("JCEKS", "SunJCE");
      } catch (KeyStoreException e) {
	log.error("KeyStore.getInstance() threw " + e);
	return false;
      } catch (NoSuchProviderException e) {
	log.error("KeyStore.getInstance() threw " + e);
      }
      //  Initialize it
      log.debug("About to keyStore.load(null)");
      try {
	keyStore.load(null, keyStorePassWord.toCharArray());
      } catch (IOException e) {
	log.debug("keyStore.load() threw " + e);
	return false;
      } catch (CertificateException e) {
	log.debug("keyStore.load() threw " + e);
	return false;
      } catch (NoSuchAlgorithmException e) {
	log.debug("keyStore.load() threw " + e);
	return false;
      }
      //  Store the certificate
      String certAlias = "localhost.crt";
      log.debug("About to store " + certAlias + " in key store");
      try {
        keyStore.setCertificateEntry(certAlias, chain[0]);
      } catch (KeyStoreException e) {
	log.error("keyStore.setCertificateEntry() threw " + e);
	return false;
      }
      //  Store the private key
      String keyAlias = "localhost.key";
      log.debug("About to store " + keyAlias + " in key store");
      try {
        keyStore.setKeyEntry(keyAlias, privKey,
			     privateKeyPassWord.toCharArray(), chain);
      } catch (KeyStoreException e) {
	log.error("keyStore.setKeyEntry() threw " + e);
	return false;
      }
      logKeyStore(keyStore,  privateKeyPassWord.toCharArray());
      //  Write the key store to the file
      try {
	log.debug("Storing KeyStore in " + keyStoreFileName);
	FileOutputStream fos = new FileOutputStream(keyStoreFile);
	keyStore.store(fos, keyStorePassWord.toCharArray());
	fos.close();
	log.debug("Done storing KeyStore in " + keyStoreFileName);
      } catch (Exception e) {
	log.error("ks.store(" + keyStoreFileName + ") threw " + e);
	return false;
      }
      return true;
    }

    //  Create the bad KeyStore
    private boolean setupBadKeyStore() {
      // Create a file for the private key password and put it there
      badPrivateKeyPassWordFileName = tempDirPath + "badPrivate.key";
      try {
        badPrivateKeyPassWordFile = new File(badPrivateKeyPassWordFileName);
        badPrivateKeyPassWordFile.deleteOnExit();
	if (!badPrivateKeyPassWordFile.exists()) {
          //  Write the key store to the file
  	  log.debug("Storing private key password in " +
  	      badPrivateKeyPassWordFileName);
  	  FileOutputStream fos = new FileOutputStream(badPrivateKeyPassWordFile);
  	  fos.write(badPrivateKeyPassWord.getBytes());
  	  fos.close();
  	  log.debug("Done storing private key password in " +
  		    badPrivateKeyPassWordFileName);
	}
      } catch (IOException e) {
        log.error(badPrivateKeyPassWordFileName + " threw " + e);
	return false;
      }
      //  Create a temporary file to hold the key store
      badKeyStoreFileName = tempDirPath + "badkeystore.jceks";
      badKeyStoreFile = new File(badKeyStoreFileName);
      badKeyStoreFile.deleteOnExit();
      if (badKeyStoreFile.exists()) {
	return true;
      }
      // Create a CertAndKeyGen instance
      String keyAlgName = "RSA";
      String sigAlgName = "MD5WithRSA";
      log.debug("About to create a CertAndKeyGen: " + keyAlgName +
		" " + sigAlgName);
      CertAndKeyGen keypair1;
      try {
        keypair1 = new CertAndKeyGen(keyAlgName, sigAlgName);
      } catch (NoSuchAlgorithmException e) {
	log.error("new CertAndKeyGen(" + keyAlgName + "," +
		  sigAlgName + ") threw " + e);
	return false;
      }
      // Generate a key pair
      log.debug("About to generate a key pair");
      try {
        keypair1.generate(1024);
      } catch (InvalidKeyException e) {
	log.error("keypair1.generate(1024) threw " + e);
	return false;
      }
      log.debug("About to get a self-signed certificate");
      X509Certificate[] chain = new X509Certificate[1];
      try {
	String certName =
	    "CN=Test Key, OU=LOCKSS Team, O=Stanford, L=Stanford, " +
	    "S=California, C=US";
        X500Name x500Name = new X500Name(certName);
        chain[0] = keypair1.getSelfCertificate(x500Name, 365*24*60*60);
      } catch (IOException e) {
	log.error("new X500Name() threw " + e);
	return false;
      } catch (CertificateException e) {
	log.error("keypair1.getSelfCertificate() threw " + e);
	return false;
      } catch (InvalidKeyException e) {
	log.error("keypair1.getSelfCertificate() threw " + e);
	return false;
      } catch (SignatureException e) {
	log.error("keypair1.getSelfCertificate() threw " + e);
	return false;
      } catch (NoSuchAlgorithmException e) {
	log.error("keypair1.getSelfCertificate() threw " + e);
	return false;
      } catch (NoSuchProviderException e) {
	log.error("keypair1.getSelfCertificate() threw " + e);
	return false;
      }
      log.debug3("chain[0]: " + chain[0].toString());
      log.debug("About to create a CertAndKeyGen: " + keyAlgName +
		" " + sigAlgName);
      CertAndKeyGen keypair2;
      try {
        keypair2 = new CertAndKeyGen(keyAlgName, sigAlgName);
      } catch (NoSuchAlgorithmException e) {
	log.error("new CertAndKeyGen(" + keyAlgName + "," +
		  sigAlgName + ") threw " + e);
	return false;
      }
      // Generate a key pair
      log.debug("About to generate a key pair");
      try {
        keypair2.generate(1024);
      } catch (InvalidKeyException e) {
	log.error("keypair2.generate(1024) threw " + e);
	return false;
      }
      // Extract the private key
      log.debug("About to get a PrivateKey");
      PrivateKey privKey = keypair2.getPrivateKey();
      log.debug("PrivateKey: " + privKey.getAlgorithm() + " " +
		privKey.getFormat());
      // Create the key store
      KeyStore keyStore = null;
      try {
        keyStore = KeyStore.getInstance("JCEKS", "SunJCE");
      } catch (KeyStoreException e) {
	log.error("KeyStore.getInstance() threw " + e);
	return false;
      } catch (NoSuchProviderException e) {
	log.error("KeyStore.getInstance() threw " + e);
      }
      //  Initialize it
      log.debug("About to keyStore.load(null)");
      try {
	keyStore.load(null, badKeyStorePassWord.toCharArray());
      } catch (IOException e) {
	log.debug("keyStore.load() threw " + e);
	return false;
      } catch (CertificateException e) {
	log.debug("keyStore.load() threw " + e);
	return false;
      } catch (NoSuchAlgorithmException e) {
	log.debug("keyStore.load() threw " + e);
	return false;
      }
      //  Store the certificate
      String certAlias = "localhost.crt";
      log.debug("About to store " + certAlias + " in key store");
      try {
        keyStore.setCertificateEntry(certAlias, chain[0]);
      } catch (KeyStoreException e) {
	log.error("keyStore.setCertificateEntry() threw " + e);
	return false;
      }
      //  Store the private key
      String keyAlias = "localhost.key";
      log.debug("About to store " + keyAlias + " in key store");
      try {
        keyStore.setKeyEntry(keyAlias, privKey,
			     badPrivateKeyPassWord.toCharArray(), chain);
      } catch (KeyStoreException e) {
	log.error("keyStore.setKeyEntry() threw " + e);
	return false;
      }
      logKeyStore(keyStore,  badPrivateKeyPassWord.toCharArray());
      //  Write the key store to the file
      try {
	log.debug("Storing KeyStore in " + badKeyStoreFileName);
	FileOutputStream fos = new FileOutputStream(badKeyStoreFile);
	keyStore.store(fos, badKeyStorePassWord.toCharArray());
	fos.close();
	log.debug("Done storing KeyStore in " + badKeyStoreFileName);
      } catch (Exception e) {
	log.error("ks.store(" + badKeyStoreFileName + ") threw " + e);
	return false;
      }
      return true;
    }

    public void testBadKeyStorePassword() {
      Properties changeProps = new Properties();
      changeProps.setProperty("org.lockss.platform.fqdn", badKeyStorePassWord);
      ConfigurationUtil.addFromProps(changeProps);
      try {
        setupPid(2);
        testIncomingRcvPeerId(pid2.getIdString(), false);
      } catch (IOException ex) {
        return;
      }
      fail("did not throw IOException");
    }

    public void testBadKeyStore() {
      Properties changeProps = new Properties();
      changeProps.setProperty(BlockingStreamComm.PARAM_SSL_KEYSTORE, badKeyStoreFileName);
      ConfigurationUtil.addFromProps(changeProps);
      try {
        setupPid(2);
        testIncomingRcvPeerId(pid2.getIdString(), false);
      } catch (IOException ex) {
        return;
      }
      fail("did not throw IOException");
    }

    public void testBadPrivateKeyPassWord() {
      Properties changeProps = new Properties();
      changeProps.setProperty(BlockingStreamComm.PARAM_SSL_PRIVATE_KEY_PASSWORD_FILE,
		      badPrivateKeyPassWordFileName);
      ConfigurationUtil.addFromProps(changeProps);
      try {
        setupPid(2);
        testIncomingRcvPeerId(pid2.getIdString(), false);
      } catch (IOException ex) {
        return;
      }
      fail("did not throw IOException");
    }

    public void testMissingKeyStoreFile() {
      Properties changeProps = new Properties();
      changeProps.setProperty(BlockingStreamComm.PARAM_SSL_KEYSTORE, keyStoreFileName + ".absent");
      ConfigurationUtil.addFromProps(changeProps);
      try {
        setupPid(2);
        testIncomingRcvPeerId(pid2.getIdString(), false);
      } catch (IOException ex) {
        return;
      }
      fail("did not throw IOException");
    }

    public void testMissingPrivateKeyPassWordFile() {
      Properties changeProps = new Properties();
      changeProps.setProperty(BlockingStreamComm.PARAM_SSL_PRIVATE_KEY_PASSWORD_FILE,
		      privateKeyPassWordFileName + ".absent");
      ConfigurationUtil.addFromProps(changeProps);
      try {
        setupPid(2);
        testIncomingRcvPeerId(pid2.getIdString(), false);
      } catch (IOException ex) {
        return;
      }
      fail("did not throw IOException");
    }

    // private debug output of keystore
    private void logKeyStore(KeyStore ks, char[] privateKeyPassWord) {
      log.debug3("start of key store");
      try {
        for (Enumeration en = ks.aliases(); en.hasMoreElements(); ) {
          String alias = (String) en.nextElement();
          log.debug3("Next alias " + alias);
          if (ks.isCertificateEntry(alias)) {
            log.debug3("About to getCertificate");
            java.security.cert.Certificate cert = ks.getCertificate(alias);
            if (cert == null) {
              log.debug3(alias + " null cert chain");
	    } else {
              log.debug3("Cert for " + alias + " is " + cert.toString());
	    }
          } else if (ks.isKeyEntry(alias)) {
            log.debug3("About to getKey");
            Key privateKey = ks.getKey(alias, privateKeyPassWord);
            log.debug3(alias + " key " + privateKey.getAlgorithm() + "/" + privateKey.getFormat());
          } else {
            log.debug3(alias + " neither key nor cert");
          }
        }
        log.debug3("end of key store");
      } catch (Exception ex) {
        log.error("logKeyStore() threw " + ex);
      }
    }

  }

  public static Test suite() {
    return variantSuites(new Class[] {
      SslStreamsPermKeys.class,
    });
  }
}
