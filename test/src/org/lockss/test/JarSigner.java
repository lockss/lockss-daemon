/*
 * $Id: JarSigner.java,v 1.2 2004-09-02 23:10:12 smorabito Exp $
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

import java.util.jar.*;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import sun.misc.BASE64Encoder;
import sun.security.util.ManifestDigester;
import sun.security.util.SignatureFile;
import org.lockss.util.*;

/**
 * This class provides a method for signing a JAR file
 * programmatically, and is used by plugin and extension JAR
 * validation and loading unit tests.
 */
public class JarSigner {

  private String alias;
  private PrivateKey privateKey;
  private X509Certificate[] certChain;
  private char[] password;
  private MessageDigest messageDigest;

  private static BASE64Encoder b64Encoder = new BASE64Encoder();

  private static Logger log = Logger.getLogger("JarSigner");

  /**
   * Create an instance of a JAR signer using the specified message
   * digest as the hash.
   *
   * @param keystore The keystore containing the private key with
   * which to sign jars.
   * @param alias The alias to use when signing jars.
   * @param pass The password to use when signing.
   * @param messageDigest The message digest to use when signing
   * jars (i.e., SHA-1).
   *
   * @throws IllegalArgumentException if no private key is found for
   * the specified alias.
   * @throws KeyStoreException if there is a problem with the keystore.
   * @throws NoSuchAlgorithmException if the SHA-1 algorithm is not available.
   * @throws UnrecoverableKeyException
   * @throws NoSuchProviderException
   * @throws IOException
   */
  public JarSigner(KeyStore keystore, String alias, String pass,
		   MessageDigest messageDigest)
      throws NoSuchAlgorithmException, KeyStoreException,
	     CertificateException, UnrecoverableKeyException,
	     NoSuchProviderException, IOException {

    this.alias = alias;
    this.password = pass.toCharArray();
    this.messageDigest = messageDigest;
    
    Key key = keystore.getKey(alias, password);

    if (key instanceof PrivateKey) {
      this.privateKey = (PrivateKey)key;
    } else {
      throw new IllegalArgumentException("No private key " +
					 "found for alias " + alias);
    }

    java.security.cert.Certificate[] certs =
      keystore.getCertificateChain(alias);

    // Convert to an array of X509Certificates.
    this.certChain = new X509Certificate[certs.length];

    try {
      for (int i = 0; i < certChain.length; i++) {
	certChain[i] = (X509Certificate)certs[i];
      }
    } catch (ClassCastException ex) {
      throw new IllegalArgumentException("Certificate chain not " +
					 "of type X.509");
    }
  }

  /**
   * Create an instance of a JAR signer using the specified message
   * using SHA1 as the hash.
   *
   * @param keystore The keystore containing the private key with
   * which to sign jars.
   * @param alias The alias to use when signing jars.
   * @param password The password to use when signing.
   *
   * @throws IllegalArgumentException if no private key is found for
   * the specified alias.
   * @throws KeyStoreException if there is a problem with the keystore.
   * @throws NoSuchAlgorithmException if the SHA-1 algorithm is not available.
   * @throws UnrecoverableKeyException
   * @throws NoSuchProviderException
   * @throws IOException
   */
  public JarSigner(KeyStore keystore, String alias, String password)
      throws NoSuchAlgorithmException, KeyStoreException,
	     CertificateException, UnrecoverableKeyException,
	     NoSuchProviderException, IOException {
    this(keystore, alias, password, MessageDigest.getInstance("SHA1"));
  }

  /**
   * Sign the specified JAR file and write it to the specified output
   * file.
   */
  public void signJar(String in, String out)
      throws IOException, NoSuchAlgorithmException,
	     InvalidKeyException, SignatureException,
	     CertificateException {
    boolean overwrite = false;

    File inFile = new File(in);
    File outFile = new File(out);

    if (inFile.getAbsolutePath().equals(outFile.getAbsolutePath())) {
      overwrite = true;
    }

    File tempFile = null; // will only be used if overwrite is true.

    JarFile inJar = new JarFile(inFile);
    JarOutputStream outJar = null;

    try {
      // Get the source manifest.
      Manifest manifest = getManifest(inJar);
      digestEntries(manifest, inJar);
      // Create the signature file and the signature block
      SignatureFile sigFile = createSignatureFile(manifest);
      SignatureFile.Block block = sigFile.generateBlock(this.privateKey,
							this.certChain, true);
      // Open the output stream and pass it the proper manifest.
      if (overwrite) {
	tempFile = File.createTempFile("jarsigner-", ".jar");
	outJar =
	  new JarOutputStream(new FileOutputStream(tempFile), manifest);
      } else {
	outJar =
	  new JarOutputStream(new FileOutputStream(outFile), manifest);
      }
      // Add the signature file.
      String sigFileName = sigFile.getMetaName();
      outJar.putNextEntry(new JarEntry(sigFileName));
      sigFile.write(outJar);
      outJar.closeEntry();
      // Add the signature block file.
      String sigBlockName = block.getMetaName();
      outJar.putNextEntry(new JarEntry(sigBlockName));
      block.write(outJar);
      outJar.closeEntry();
      // Copy the remaining entries.
      Iterator iter = manifest.getEntries().keySet().iterator();
      while (iter.hasNext()) {
	String entryName = (String)iter.next();
	JarEntry entry = inJar.getJarEntry(entryName);
	if (entry != null &&
	    !entry.getName().startsWith("META-INF") &&
	    !entry.isDirectory()) {
	  copyJarEntry(entry, inJar, outJar);
	}
      }
    } finally {
      if (inJar != null) inJar.close();

      if (outJar != null) {
	outJar.flush();
	outJar.finish();
	outJar.close();
      }

      // If overwriting, copy the temp file over the out file.
      if (overwrite && tempFile != null && tempFile.exists()) {
	if (outFile.exists()) {
	  outFile.delete();
	}
	InputStream is = new FileInputStream(tempFile);
	OutputStream os = new FileOutputStream(outFile);
	StreamUtil.copy(is, os);
	tempFile.delete();
      }
    }
  }

  /**
   * Sign the specified jar, overwriting it with the signed version.
   */
  public void signJar(String jarFile)
      throws IOException, NoSuchAlgorithmException,
	     InvalidKeyException, SignatureException,
	     CertificateException {
    signJar(jarFile, jarFile);
  }

  /**
   * Copy a jar file entry from the input jar file to the output stream.
   */
  private void copyJarEntry(JarEntry je, JarFile in, JarOutputStream out)
      throws IOException {
    out.putNextEntry(je);
    StreamUtil.copy(in.getInputStream(je), out);
    out.closeEntry();
  }

  /**
   * Given a manifest object, create a SignatureFile.
   */
  private SignatureFile createSignatureFile(Manifest manifest) throws IOException {
    ManifestDigester manifestDigester =
      new ManifestDigester(serializeManifest(manifest));
    return new SignatureFile(new MessageDigest[] { messageDigest },
			     manifest, manifestDigester, alias, true);
  }

  /**
   * Safely retrieve the manifest from a JAR file.  If none exists,
   * create an empty one.
   */
  private Manifest getManifest(JarFile jarFile)
      throws IOException {

    // create the manifest object
    Manifest manifest = jarFile.getManifest();

    if (manifest == null) {
      manifest = new Manifest();
    }

    // Ensure the manifest contains only the correct
    // entries.
    canonicalizeManifest(manifest, jarFile);

    return manifest;
  }


  /**
   * Given a manifest file and given a jar file, make sure that
   * the contents of the manifest file are correct.
   */
  private void canonicalizeManifest(Manifest manifest, JarFile jarFile)
      throws IOException {

    Map map = manifest.getEntries();

    // If the manifest contains entries that are not in the JAR,
    // remove them.
    if (map.size() > 0) {
      for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
	String element = (String)iter.next();
	if (jarFile.getEntry(element) == null) {
	  log.info("manifest entry " + element +
		   " is not in JAR.  Deleting.");
	  map.remove(element);
	}
      }
    } else {
      // if there are no pre-existing entries in the manifest,
      // then we put a few required defaults in.
      Attributes attributes = manifest.getMainAttributes();
      attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
      attributes.putValue("Created-By", "LOCKSS");
    }

    // If the JAR contains entries that are not in the manifest, add
    // them.
    Enumeration e = jarFile.entries();
    while (e.hasMoreElements()) {
      JarEntry entry = (JarEntry)e.nextElement();
      if (!map.containsKey(entry.getName()) &&
	  !entry.getName().startsWith("META-INF") &&
	  !entry.isDirectory()) {
	log.info("Jar entry " + entry.getName() +
		 " was not in manifest.  Adding.");
	map.put(entry.getName(), new Attributes());
      }
    }
  }

  /**
   * Hash each of the jar entries in the given manifest and update
   * their digest entries.
   */
  private void digestEntries(Manifest manifest,
			     JarFile jarFile)
      throws IOException, NoSuchAlgorithmException {

    Map jarEntries = manifest.getEntries();

    for (Iterator iter = jarEntries.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      JarEntry jarEntry = jarFile.getJarEntry(key);

      if (jarEntry == null) {
	// This should never happen!
	log.warning("Warning: Expected jar entry " + key +
		    " did not exist in file " + jarFile);
	return;
      }

      log.debug2("signing: " + key);
      Attributes attributes = manifest.getAttributes(key);
      if (attributes == null) {
	attributes = new Attributes();
      }
      attributes.putValue(messageDigest.getAlgorithm() + "-Digest",
			  digestEntry(jarFile.getInputStream(jarEntry)));
    }
  }

  /**
   * Hash the given input stream and return a base64 encoded string
   * representing the digest.
   */
  private String digestEntry(InputStream inputStream)
      throws IOException {
    byte[] buffer = new byte[2048];
    int read = 0;
    while((read = inputStream.read(buffer)) > 0) {
      messageDigest.update(buffer, 0, read);
    }
    inputStream.close();

    return b64Encoder.encode(messageDigest.digest());
  }

  /**
   * Return the given Manifest as a byte array.
   */
  private byte[] serializeManifest(Manifest manifest) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    manifest.write(baos);
    baos.flush();
    baos.close();
    return baos.toByteArray();
  }
}
