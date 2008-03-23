/*
 * $Id: TestJarValidator.java,v 1.13 2008-03-23 17:02:46 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
  private String tamperedJar = "org/lockss/test/tampered-plugin.jar";
  private String noManifestJar = "org/lockss/test/nomanifest-plugin.jar";

  // JAR files created by Brent for more complete testing...
  private String Expired1Jar = "org/lockss/test/Expired1.jar";
  private String Expired2Jar = "org/lockss/test/Expired2.jar";
  private String Future1Jar = "org/lockss/test/Future1.jar";
  private String Future2Jar = "org/lockss/test/Future2.jar";
  private String Good1Jar = "org/lockss/test/Good1.jar";
  private String Good2Jar = "org/lockss/test/Good2.jar";
  private String Modified1Jar = "org/lockss/test/Modified1.jar";
  private String Modified2Jar = "org/lockss/test/Modified2.jar";
  private String Unrecognized1Jar = "org/lockss/test/Unrecognized1.jar";
  private String Unrecognized2Jar = "org/lockss/test/Unrecognized2.jar";
  private String Unsigned1Jar = "org/lockss/test/Unsigned1.jar";
  private String Unsigned2Jar = "org/lockss/test/Unsigned2.jar";
  
  private String dirNonExisting = "/aftae/gthua/hjtno/gueao/";
  private String fileExisting = "org/lockss/util/FileExisting";

  private String pubKeystoreName = "org/lockss/test/public.keystore";
  private KeyStore m_pubKeystore;

  private KeyStore getKeystoreResource(String name, String pass)
      throws Exception {
    KeyStore ks = KeyStore.getInstance("JKS", "SUN");
    ks.load(ClassLoader.getSystemClassLoader().
	    getResourceAsStream(name), pass.toCharArray());
    return ks;
  }

  public void setUp() throws Exception {
    m_pubKeystore = getKeystoreResource(pubKeystoreName, password);
  }

  public void testNoPluginDir() throws Exception {
    MockCachedUrl goodCu =
      new MockCachedUrl("http://foo.com/good.jar", goodJar, true);
    JarValidator validator = new JarValidator(m_pubKeystore, null);
    try {
      validator.getBlessedJar(goodCu);
      fail("Should have thrown JarValidationException.");
    } catch (JarValidator.JarValidationException ignore) {
      //expected
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
      //expected
    }
    assertNull("File should not exist.", f);
  }

  public void testGoodJar() throws Exception {
    MockCachedUrl goodCu =
      new MockCachedUrl("http://foo.com/good.jar", goodJar, true);
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    f = validator.getBlessedJar(goodCu);
    assertNotNull(f);
    assertTrue(f.exists());
  }

  public void testBadJar() throws Exception {
    MockCachedUrl badCu =
      new MockCachedUrl("http://foo.com/bad.jar", badJar, true);
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(badCu);
      fail("Should have thrown JarValidationException");
    } catch (JarValidator.JarValidationException ignore) {
      //expected
    }
    assertNull(f);
  }

  /**
   * Ensure that a tampered jar will not load.
   */
  public void testTamperedJar() throws Exception {
    MockCachedUrl tamperedCu =
      new MockCachedUrl("http://foo.com/tampered.jar", tamperedJar, true);
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(tamperedCu);
    } catch (JarValidator.JarValidationException ignore) {
      //expected
    }
    assertNull(f);
  }

  /**
   * Ensure that a jar with no manifest will not load.
   */
  public void testNoManifestJar() throws Exception {
    MockCachedUrl noManifestCu =
      new MockCachedUrl("http://foo.com/nomanifest.jar", noManifestJar, true);
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(noManifestCu);
    } catch (JarValidator.JarValidationException ignore) {
      //expected
    }
    assertNull(f);
  }


  public void testUnsignedJar() throws Exception {
    // Don't sign the test jar.
    MockCachedUrl unsignedCu =
      new MockCachedUrl("http://foo.com/unsigned.jar", unsignedJar, true);
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(unsignedCu);
      fail("Should have thrown JarValidationException");
    } catch (JarValidator.JarValidationException ignore) {
      //expected
    }
    assertNull(f);
  }
  
  // The below tests were written based on "JAR File Tests" by Brent E. Edwards.
  
  // *** Single-.java file tests:
  
  // Does the validator accept JAR files of one .java file with a good, known signature?
  
  
  public void testGood1() throws Exception {
    MockCachedUrl mcuGood1 =
      new MockCachedUrl("http://foo.com/Good1.jar", Good1Jar, true);    
    examineInputStream(mcuGood1);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = validator.getBlessedJar(mcuGood1);
    assertNotNull(f);
    assertTrue(f.exists());
  }
  
  // Does the validator accept JAR files of one .java file whose signature is expired?
  public void testExpired1() throws Exception {
    MockCachedUrl mcuExpired1 =
      new MockCachedUrl("http://foo.com/Expired1.jar", Expired1Jar, true);
    examineInputStream(mcuExpired1);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuExpired1);
      fail("testExpired1: getting an expired jar should have caused an exception.");
    } catch (JarValidator.JarValidationException ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }

  // Does the validator accept JAR files of one .java file whose signature is not yet available?
  public void testFuture1() throws Exception {
    MockCachedUrl mcuFuture1 =
      new MockCachedUrl("http://foo.com/Future1.jar", Future1Jar, true);
    examineInputStream(mcuFuture1);

    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuFuture1);
      fail("testFuture1: getting a future jar should have caused an exception.");
    } catch (JarValidator.JarValidationException ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }
  
  // Does the validator accept JAR files of one .java file without a signature?
  public void testUnsigned1() throws Exception {
    MockCachedUrl mcuUnsigned1 =
      new MockCachedUrl("http://foo.com/Unsigned1.jar", Unsigned1Jar, true);
    examineInputStream(mcuUnsigned1);

    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuUnsigned1);
      fail("testUnsigned1: getting an unsigned jar should have caused an exception.");
    } catch (JarValidator.JarValidationException ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }

  // Does the validator accept JAR files of one .java file whose signature does not match the file?
  public void testModified1() throws Exception {
    MockCachedUrl mcuModified1 =
      new MockCachedUrl("http://foo.com/Modified1.jar", Modified1Jar, true);
    examineInputStream(mcuModified1);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuModified1);
      fail("testModified1: getting a modified jar should have caused an exception.");
    } catch (Exception ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }
  
  // Does the validator accept JAR files of one .java file with a good signature from a source that it does not recognize?
  public void testUnrecognized1() throws Exception {
    MockCachedUrl mcuUnrecognized1 =
      new MockCachedUrl("http://foo.com/Unrecognized1.jar", Unrecognized1Jar, true);
    examineInputStream(mcuUnrecognized1);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuUnrecognized1);
      fail("testModified1: getting a modified jar should have caused an exception.");
    } catch (JarValidator.JarValidationException ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }
  
  // *** Multiple-.java file tests.  (In each test, multiple files will be present with one file under the given condition.)

  // Does the validator accept JAR files with two .java files with a good, known signature?
  public void testGood2() throws Exception {
    MockCachedUrl mcuGood2 =
      new MockCachedUrl("http://foo.com/Good2.jar", Good2Jar, true);
    examineInputStream(mcuGood2);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = validator.getBlessedJar(mcuGood2);
    assertNotNull(f);
    assertTrue(f.exists());
  }
  
  // Does the validator accept JAR files with two .java file whose signature is expired?
  public void testExpired2() throws Exception {
    MockCachedUrl mcuExpired2 =
      new MockCachedUrl("http://foo.com/Expired2.jar", Expired2Jar, true);
    examineInputStream(mcuExpired2);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuExpired2);
      fail("testExpired2: getting an expired jar should have caused an exception.");
    } catch (JarValidator.JarValidationException ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }

  // Does the validator accept JAR files with two .java files whose signature is not yet available?
  public void testFuture2() throws Exception {
    MockCachedUrl mcuFuture2 =
      new MockCachedUrl("http://foo.com/Future2.jar", Future2Jar, true);
    examineInputStream(mcuFuture2);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuFuture2);
      fail("testFuture2: getting a future jar should have caused an exception.");
    } catch (JarValidator.JarValidationException ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }
  
  // Does the validator accept JAR files with two .java files without a signature?
  public void testUnsigned2() throws Exception {
    MockCachedUrl mcuUnsigned2 =
      new MockCachedUrl("http://foo.com/Unsigned2.jar", Unsigned2Jar, true);
    examineInputStream(mcuUnsigned2);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuUnsigned2);
      fail("testUnsigned2: getting an unsigned jar should have caused an exception.");
    } catch (JarValidator.JarValidationException ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }
  
  // Does the validator accept JAR files with two .java files whose signature does not match the file?
  public void testModified2() throws Exception {
    MockCachedUrl mcuModified2 =
      new MockCachedUrl("http://foo.com/Modified2.jar", Modified2Jar, true);
    examineInputStream(mcuModified2);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuModified2);
      fail("testModified2: getting a modified jar should have caused an exception.");
    } catch (Exception ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }
  
  // Does the validator accept JAR files with two .java files with a good signature from a source that it does not recognize?
  public void testUnrecognized2() throws Exception {
    MockCachedUrl mcuUnrecognized2 =
      new MockCachedUrl("http://foo.com/Unrecognized2.jar", Unrecognized2Jar, true);
    examineInputStream(mcuUnrecognized2);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      f = validator.getBlessedJar(mcuUnrecognized2);
      fail("testModified2: getting a modified jar should have caused an exception.");
    } catch (JarValidator.JarValidationException ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }
  
  // ** Tests of JarValidator:
  
  // What happens if the keystore has not been loaded?
//  public void testUnloadedKeyStore() throws Exception {
//    KeyStore ks = KeyStore.getInstance("JKS", "SUN");
//    // Note: NO call to ks.load.
//
//    try {
//      MockCachedUrl mcuGood2 =
//        new MockCachedUrl("http://foo.com/Good2.jar", Good2Jar, true);
//      JarValidator validator = new JarValidator(ks, getTempDir());
//      validator.getBlessedJar(mcuGood2);
//      
//      fail("testUnloadedKeyStore: We should have caused an exception when we got the blessed jar.");
//    } catch (Exception e) {
//      // The expected behavior.
//    }
//  }
  
  // What happens if pluginDir is null?
  public void testNullPlugInDir() throws Exception {
    try {
      MockCachedUrl mcuGood2 =
        new MockCachedUrl("http://foo.com/Good2.jar", Good2Jar, true);
      JarValidator validator = new JarValidator(m_pubKeystore, null);
      validator.getBlessedJar(mcuGood2);
      
      fail("testNullPlugInDir: We should have caused an exception when we got the blessed jar.");
    } catch (Exception e) {
      // The expected behavior.
    }    
  }
  
  // What happens if pluginDir doesn't exist?
  public void testNonExistingPlugInDir() throws Exception {
    try {
      MockCachedUrl mcuGood2 =
        new MockCachedUrl("http://foo.com/Good2.jar", Good2Jar, true);
      JarValidator validator = new JarValidator(m_pubKeystore, new File(dirNonExisting));
      validator.getBlessedJar(mcuGood2);
      
      fail("testNullPlugInDir: We should have caused an exception when we got the blessed jar.");
    } catch (Exception e) {
      // The expected behavior.
    }    
  }
  
  // What happens if pluginDir points to a file, not a directory?
  public void testPlugInDirToFile() throws Exception {
    try {
      MockCachedUrl mcuGood2 =
        new MockCachedUrl("http://foo.com/Good2.jar", Good2Jar, true);
      JarValidator validator = new JarValidator(m_pubKeystore, new File(fileExisting));
      validator.getBlessedJar(mcuGood2);
      
      fail("testPlugInDirToFile: We should have caused an exception when we got the blessed jar.");      
    } catch (Exception e) {
      // The expected behavior.
    }
  }
  
  // What happens if pluginDir can't be written to?
  public void testPlugInDirUnwritable() throws Exception {
    File fileUnwritable;
    
    fileUnwritable = getTempDir("readonly");
    fileUnwritable.setReadOnly();

    try {
      MockCachedUrl mcuGood2 =
        new MockCachedUrl("http://foo.com/Good2.jar", Good2Jar, true);
      JarValidator validator = new JarValidator(m_pubKeystore, fileUnwritable);
      validator.getBlessedJar(mcuGood2);
      
      fail("testPlugInDirUnwritable: We should have caused an exception when we got the blessed jar.");      
    } catch (IOException e) {
      // The expected behavior.
    }
  }

  // ** Tests of getBlessedJob
  // What happens if cu is null?
  public void testNullCU() throws Exception {
    MockCachedUrl mcuNull = null;
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    
    try {
      validator.getBlessedJar(mcuNull);
      fail("testNullCU: Using a null CachedUrl should have caused an exception.");
    } catch (Exception e) {
      // The expected behavior.
    }
  }
  
  // What happens if cu doesn't contain a JAR?
  public void testCUNotJar() throws Exception {
    MockCachedUrl mcuNull = 
      new MockCachedUrl("http://foo.com/FileExisting", fileExisting, true);
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    
    try {
      validator.getBlessedJar(mcuNull);
      fail("testCUNotJar: Using a CachedUrl that's not a jar should have caused an exception.");
    } catch (Exception e) {
      // The expected behavior.
    }
  }
  
  // Verify that turning off the expired flag allows us to read expired jars.
  public void testAllowExpired() throws Exception {
    MockCachedUrl mcuExpired2 =
      new MockCachedUrl("http://foo.com/Expired2.jar", Expired2Jar, true);
    examineInputStream(mcuExpired2);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    validator.allowExpired(true);
      
    f = validator.getBlessedJar(mcuExpired2);
    
    assertNotNull(f);
    assertTrue(f.exists());
  }
  
  
  // Verify that turning on the expired flag disallows us to read expired jars.
  public void testDisallowExpired() throws Exception {
    MockCachedUrl mcuExpired2 =
      new MockCachedUrl("http://foo.com/Expired2.jar", Expired2Jar, true);
    examineInputStream(mcuExpired2);
    
    JarValidator validator =
      new JarValidator(m_pubKeystore, getTempDir());
    File f = null;
    try {
      validator.allowExpired(false);
      
      f = validator.getBlessedJar(mcuExpired2);
      fail("testExpired2: getting an expired jar should have caused an exception.");
    } catch (JarValidator.JarValidationException ignore) {
      // Expected; ignore.
    }
    assertNull(f);
  }

  // If you have lots of time, reproduce every .jar test twice,
  // once with validator.allowExpired(false), and once with 
  // validator.allowExpired(true), just to make sure that you
  // get the same answers.

  
  /**
   * @param mcuTest -- the mock cached url that we want to verify exists.
   * @throws IOException
   */
  private void examineInputStream(MockCachedUrl mcuTest) throws IOException {
    InputStream isUnsigned1;
    isUnsigned1 = mcuTest.getUnfilteredInputStream();
    assertNotNull(isUnsigned1);
    assertTrue(isUnsigned1.available() > 0);
  }
  

}
