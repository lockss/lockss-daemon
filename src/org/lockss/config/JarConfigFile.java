/*
 * $Id: JarConfigFile.java,v 1.1 2005-01-10 06:23:33 smorabito Exp $
 */

/*

Copyright (c) 2001-2003 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * A Configuration loaded from a JAR file.
 */

public class JarConfigFile extends ConfigFile {

  public JarConfigFile(String url) throws IOException {
    super(url);
  }

  /**
   * Reload the config file.  This is something of a special case
   * where it doesn't (yet) make sense to check last-modified times,
   * so this implementation only loads the config file once.
   *
   * JAR Urls should look something like:
   *    jar:file:///path/to/jar!/some/resource
   */
  protected synchronized boolean reload() throws IOException {
    if (m_lastModified == null) {
      log.debug2("Loading JAR config file: " + m_fileUrl);
      InputStream in = null;
      JarEntry entry = null;
      m_IOException = null;
      m_lastAttempt = TimeBase.nowMs();
      try {
	URL jarUrl = new URL(m_fileUrl);
	JarURLConnection con = (JarURLConnection)jarUrl.openConnection();
	entry = con.getJarEntry();
	in = con.getInputStream();
      } catch (IOException ex) {
	log.warning("Unexpected exception trying to load " +
		    "config file (" + m_fileUrl + "): " + ex);
	m_IOException = ex;
	throw ex;
      }
      if (in != null && entry != null) {
	setConfigFrom(in);
	m_lastModified = Long.toString(entry.getTime());
	in.close();
      }
    }
    return m_IOException == null;
  }
}
