/*
 * $Id: JarConfigFile.java,v 1.6 2010-02-23 00:28:25 pgust Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.io.*;
import java.net.*;
import java.util.jar.*;

/**
 * A ConfigFile loaded from an entry in a JAR file.
 *
 * JAR Urls should look something like:
 *    jar:file:///path/to/jar!/some/resource
 */
public class JarConfigFile extends BaseConfigFile {
  private File m_jarFile;
//   private String m_entryTime;

  public JarConfigFile(String url) {
    super(url);
  }

  protected InputStream openInputStream() throws IOException {
    String lm = calcNewLastModified();

    // Only reload the file if the last modified timestamp is different.
    if (lm.equals(m_lastModified)) {
      log.debug2("Jar has not changed on disk, not reloading: " + m_fileUrl);
      return null;
    }
    log.debug2("Loading JAR config file: " + m_fileUrl);
    URL jarUrl = new URL(m_fileUrl);
    final JarURLConnection con = (JarURLConnection)jarUrl.openConnection();
    JarEntry entry = con.getJarEntry();
    if (m_jarFile == null) {
      JarFile jf = con.getJarFile();
      String name = jf.getName();
      log.debug3("jf.name: " + name);
      m_jarFile = new File(name);
    }
//     m_entryTime = Long.toString(entry.getTime());
    return new FilterInputStream(con.getInputStream()) {
    	public void close() throws IOException
    	{
    		super.close();
    		// close connection JAR file to unlock file on Windows
    		con.getJarFile().close();
    	}
    };
  }

  protected String calcNewLastModified() {
    if (m_jarFile != null) {
      return Long.toString(m_jarFile.lastModified());
    }
    return "Never";
  }

  File getFile() {
    return m_jarFile;
  }
}
