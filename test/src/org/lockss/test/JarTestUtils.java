/*
 * $Id: JarTestUtils.java,v 1.1 2004-09-01 20:14:44 smorabito Exp $
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

import java.util.*;
import java.io.*;
import java.util.jar.*;
import org.lockss.util.*;
import java.security.*;
import java.security.cert.*;

import sun.security.x509.*;

public class JarTestUtils {
  private static Logger log = Logger.getLogger("JarTestUtils");

  /**
   * Creat a JAR file using the specified map as a set of
   * jar entry names and resources (file or JAR resources) to
   * store.
   *
   * @param out The name of the file to create.
   * @param entries A map containing jar entry names mapped
   * to resource names to store in the JAR.
   * @param makePlugin If true, mark all *Plugin.xml files as
   * LOCKSS loadable plugins.
   */
  public static void createResourceJar(String out, Map entries,
				       boolean makePlugin)
      throws IOException {
    Manifest manifest = getManifest();

    for (Iterator iter = entries.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      manifest.getEntries().put(key, new Attributes());
      if (key.endsWith("Plugin.xml")) {
	manifest.getAttributes(key).putValue("Lockss-Plugin", "true");
      }
    }

    JarOutputStream jar =
      new JarOutputStream(new FileOutputStream(out), manifest);

    for (Iterator iter = entries.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      String resourceName = (String)entries.get(key);
      // Load the resource
      InputStream is =
	ClassLoader.getSystemResourceAsStream(resourceName);
      if (is == null) {
	log.warning("Resource " + resourceName + " could not be loaded, "+
		    "cannot include in jar file " + out);
	// This will still get included in the manifest, but
	// that should not cause any harm in testing.
	continue;
      }
      JarEntry entry = new JarEntry(key);
      jar.putNextEntry(entry);
      StreamUtil.copy(is, jar);
      jar.closeEntry();
    }

    jar.finish();
    jar.close();
  }

  /**
   * Create a JAR file using the specified map as a set of
   * jar entry names and file contents.  Used for unit testing.
   *
   * @param out  The name of the file to create.
   * @param entries A map containing jar entry names mapped
   * to strings to save as the contents of those entries.
   */
  public static void createStringJar(String out, Map entries)
      throws IOException {
    Manifest manifest = getManifest();

    for (Iterator iter = entries.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      manifest.getEntries().put(key, new Attributes());
    }

    JarOutputStream jar =
      new JarOutputStream(new FileOutputStream(out), manifest);
    // Add entries to jar.
    for (Iterator iter = entries.keySet().iterator(); iter.hasNext(); ) {
      String key = (String)iter.next();
      String contents = (String)entries.get(key);
      JarEntry entry = new JarEntry(key);
      jar.putNextEntry(entry);
      StreamUtil.copy(new ReaderInputStream(new StringReader(contents)), jar);
      jar.closeEntry();
    }

    jar.finish();
    jar.close();
  }


  /**
   * Set up a new manifest.
   */
  private static Manifest getManifest() {
    Manifest manifest = new Manifest();
    Attributes mainAttrs = manifest.getMainAttributes();
    // MANIFEST_VERSION is required by the Sun API, without
    // it the manifest will not be written properly.
    mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    mainAttrs.putValue("Created-By", "LOCKSS");
    return manifest;
  }

}
