/*
 * $Id: TestJarValidator.java,v 1.1 2004-09-01 20:14:44 smorabito Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;
import java.net.*;
import java.security.KeyStore;
import junit.framework.*;

import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.daemon.*;


/**
 * Test class for <code>org.lockss.util.JarValidator</code>.
 */
public class TestJarValidator extends LockssTestCase {

  private static final String name = "Good User";
  private static final String alias = "good";
  private static final String badName = "Bad User";
  private static final String badAlias = "bad";
  private static final String password = "f00bar";

  public static Class testedClasses[] = {
    org.lockss.util.JarValidator.class
  };

  private String goodPubKeystore;
  private String goodPrivKeystore;
  private String badPubKeystore;
  private String badPrivKeystore;

  private String testJar;

  private KeyStore keystore;
  private File tmpDir;

  public void setUp() throws Exception {
    tmpDir = getTempDir();
    // Set up full paths.
    goodPrivKeystore = getFullPath("good-private.keystore");
    goodPubKeystore = getFullPath("good-public.keystore");
    badPrivKeystore = getFullPath("bad-private.keystore");
    // not used, but it'll get created anyway.
    badPubKeystore = getFullPath("bad-public.keystore");
    // The jar file we'll be creating.
    testJar = getFullPath("test.jar");
    // Generate keystores used to sign and verify good code.
    KeystoreTestUtils.createKeystores(goodPubKeystore, goodPrivKeystore,
				      password, alias, name);
    // Generate keystores used to sign bad code.
    KeystoreTestUtils.createKeystores(badPubKeystore, badPrivKeystore,
				      password, badAlias, badName);
    // Load the keystore we'll use for verifying code.
    keystore = KeyStore.getInstance("JKS", "SUN");
    keystore.load(new FileInputStream(goodPubKeystore),
		  password.toCharArray());

    // Make a test jar.
    Map entries = new HashMap();
    entries.put("foo/bar/Baz.txt", "This is a test file.");
    JarTestUtils.createStringJar(testJar, entries);
  }

  public void testNullKeystore() throws Exception {
    JarSigner signer = new JarSigner(goodPrivKeystore, alias, password);
    signer.signJar(testJar);
    MockCachedUrl goodCu =
      new MockCachedUrl("http://foo.com/test.jar", testJar);
    // Create a validator with a null keystore -- should fail validation.
    JarValidator validator =
      new JarValidator(null, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(goodCu);
      fail("Should have thrown JarValidationException.");
    } catch (JarValidator.JarValidationException ignore) {
      ;
    }
    assertNull("File should not exist.", f);
  }

  public void testGoodJar() throws Exception {
    JarSigner signer = new JarSigner(goodPrivKeystore, alias, password);
    signer.signJar(testJar);
    MockCachedUrl goodCu =
      new MockCachedUrl("http://foo.com/test.jar", testJar);
    JarValidator validator =
      new JarValidator(keystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(goodCu);
    } catch (JarValidator.JarValidationException ex) {
      fail("Should not have thrown.");
    }
    assertNotNull(f);
    assertTrue(f.exists());
  }

  public void testBadJar() throws Exception {
    JarSigner signer = new JarSigner(badPrivKeystore, badAlias, password);
    signer.signJar(testJar);
    MockCachedUrl badCu =
      new MockCachedUrl("http://foo.com/test.jar", testJar);
    JarValidator validator =
      new JarValidator(keystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(badCu);
      fail("Should have thrown JarValidationException");
    } catch (JarValidator.JarValidationException ignore) {
      ;
    }
    assertNull(f);
  }

  public void testUnsignedJar() throws Exception {
    // Don't sign the test jar.
    MockCachedUrl unsignedCu =
      new MockCachedUrl("http://foo.com/test.jar", testJar);
    JarValidator validator =
      new JarValidator(keystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(unsignedCu);
      fail("Should have thrown JarValidationException");
    } catch (JarValidator.JarValidationException ignore) {
      ;
    }
    assertNull(f);
  }


  /**
   * Given a file name, return the full path to its location in the
   * working directory.
   */
  private String getFullPath(String fileName) throws IOException {
    if (tmpDir == null) {
      tmpDir = getTempDir();
    }
    return tmpDir.getAbsolutePath() + File.separator + fileName;
  }
}
