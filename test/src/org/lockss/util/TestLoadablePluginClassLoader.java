/*
 * $Id: TestLoadablePluginClassLoader.java,v 1.1 2004-09-01 20:14:44 smorabito Exp $
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

package org.lockss.util;

import java.io.*;
import java.util.*;
import java.net.*;

import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.daemon.*;

/**
 * Test class for <code>org.lockss.util.LoadablePluginClassLoader</code>.
 */
public class TestLoadablePluginClassLoader extends LockssTestCase {

  private LoadablePluginClassLoader loader;

  public static Class testedClasses[] = {
    org.lockss.util.LoadablePluginClassLoader.class
  };

  public void setUp() throws Exception {
    String testJar = getTempDir().getAbsolutePath() +
      File.separator + "test.jar";
    // Create a jar file with a single resource (a text file)
    Map entries = new HashMap();
    entries.put("test.txt", "foo bar baz quux");
    JarTestUtils.createStringJar(testJar, entries);
    URL[] urls = new URL[] { new URL(FileTestUtil.urlOfFile(testJar)) };
    loader = new LoadablePluginClassLoader(urls);
  }

  /**
   * Not much of a test honestly - simply verify that the classloader
   * will in fact correctly load a resource out of a jar.
   */
  public void testLoadResourceInJar() throws Exception {
    // Attempt to load the file as a resource.
    InputStream is = loader.getResourceAsStream("test.txt");
    assertNotNull(is);
    Reader in = new InputStreamReader(is);
    StringWriter out = new StringWriter();
    StreamUtil.copy(in, out);
    String fileContents = out.toString();
    assertEquals("foo bar baz quux", fileContents);
  }

  /**
   * Test to be sure that the classloader will delegate up the
   * classloader chain.
   */
  public void testLoadClassNotInJar() throws Exception {
    Class c = loader.loadClass("java.lang.String");
    assertNotNull(c);
    Object o = c.newInstance();
    assertTrue(o instanceof java.lang.String);
  }
}
