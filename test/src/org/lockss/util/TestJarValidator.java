/*
 * $Id: TestJarValidator.java,v 1.5 2004-09-25 00:49:10 smorabito Exp $
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
  
  public static Class testedClasses[] = {
    org.lockss.util.JarValidator.class
  };

  private static final String password = "f00bar";

  private String goodJar = "org/lockss/test/good-plugin.jar";
  private String badJar = "org/lockss/test/bad-plugin.jar";
  private String unsignedJar = "org/lockss/test/unsigned-plugin.jar";

  private String pubKeystoreName = "org/lockss/test/public.keystore";
  private KeyStore pubKeystore;

  private KeyStore getKeystoreResource(String name, String pass) 
      throws Exception {
    KeyStore ks = KeyStore.getInstance("JKS", "SUN");
    ks.load(ClassLoader.getSystemClassLoader().
	    getResourceAsStream(name), pass.toCharArray());
    return ks;
  }

  public void setUp() throws Exception {
    pubKeystore = getKeystoreResource(pubKeystoreName, password);
  }

  public void testNoPluginDir() throws Exception {
    MockCachedUrl goodCu =
      new MockCachedUrl("http://foo.com/good.jar", goodJar, true);
    JarValidator validator = new JarValidator(pubKeystore, null);
    try {
      File f = validator.getBlessedJar(goodCu);
      fail("Should have thrown JarValidationException.");
    } catch (JarValidator.JarValidationException ignore) {
      ;
    }
  }

  public void testNullKeystore() throws Exception {
    MockCachedUrl goodCu =
      new MockCachedUrl("http://foo.com/good.jar", goodJar, true);
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
    MockCachedUrl goodCu =
      new MockCachedUrl("http://foo.com/good.jar", goodJar, true);
    JarValidator validator =
      new JarValidator(pubKeystore, getTempDir());
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
    MockCachedUrl badCu =
      new MockCachedUrl("http://foo.com/bad.jar", badJar, true);
    JarValidator validator =
      new JarValidator(pubKeystore, getTempDir());
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
      new MockCachedUrl("http://foo.com/unsigned.jar", unsignedJar, true);
    JarValidator validator =
      new JarValidator(pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(unsignedCu);
      fail("Should have thrown JarValidationException");
    } catch (JarValidator.JarValidationException ignore) {
      ;
    }
    assertNull(f);
  }
}
