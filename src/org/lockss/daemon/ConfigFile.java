/*
 * $Id: ConfigFile.java,v 1.1 2004-06-14 03:08:43 smorabito Exp $
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

package org.lockss.daemon;

import java.io.*;
import java.net.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * A simple wrapper class around the text representation of a
 * generic config (either plain text or XML)
 */

public class ConfigFile {
  public static final int XML_FILE = 0;
  public static final int PROPERTIES_FILE = 1;

  private static final int HTTP_OK = 200;

  private int m_fileType;
  private String m_lastModified;
  private String m_fileUrl;
  private String m_fileContents;

  private static Logger log = Logger.getLogger("ConfigFile");

  /**
   * Read the contents of a file or the results of a URL fetch into
   * memory.
   *
   * @throws IOException
   */
  public ConfigFile(String url)
      throws IOException {
    if (!load(url)) {
      log.warning("Configuration file not loaded: " + url);
    }
  }

  public String getFileUrl() {
    return m_fileUrl;
  }

  public int getFileType() {
    return m_fileType;
  }

  public String getLastModified() {
    return m_lastModified;
  }

  /**
   * Return an inputstream of the file contents.
   */
  public InputStream getInputStream() {
    if (m_fileContents == null) {
      throw new IllegalStateException("Config file not loaded: " +
				      this.toString());
    }
    
    return new ReaderInputStream(new StringReader(m_fileContents));
  }

  /**
   * Attempt to re-load the file contents.
   */
  public void reload() throws IOException {
    if (m_fileUrl != null) {
      load(m_fileUrl);
    }
  }

  /**
   * Load a URL or file.  If there is no current "last modified"
   * time, try to load it unconditionally.  Otherwise, send a
   * conditional GET with an "if-modified-since" header.
   */
  private synchronized boolean load(String url) throws IOException {
    Reader in = null;
    Writer out = new StringWriter();
    LockssUrlConnection conn = null;
    m_fileUrl = url;

    // TODO: This check may or may not even be necessary.  If it
    // proves to be useful, it may need to be improved in the
    // future.
    if (StringUtil.endsWithIgnoreCase(url, ".xml")) {
      m_fileType = ConfigFile.XML_FILE;
    } else {
      m_fileType = ConfigFile.PROPERTIES_FILE;
    }

    // Open an output stream to write to our string
    try {
      URL u = new URL(url);
      conn = UrlUtil.openConnection(url);

      if (m_lastModified != null) {
	 conn.setIfModifiedSince(m_lastModified);
      }

      conn.execute();

      if (conn.isHttp() && conn.getResponseCode() == HTTP_OK) {
	m_lastModified = conn.getResponseHeaderValue("last-modified");
	in = new InputStreamReader(conn.getResponseInputStream());
	log.debug2("New file, or file changed.  Loading file from " +
		   "remote connection:" + url);
      } else if (!conn.isHttp()) {
	in = new InputStreamReader(conn.getResponseInputStream());
      }
      log.debug2("File not changed, not reloading: " + url);
    } catch (MalformedURLException ex) {
      in = new InputStreamReader(new FileInputStream(url));
      log.debug2("Loading local file unconditionally: " + url);
    } catch (IOException ex) {
      log.warning("Unexpected exception trying to load " +
		  "config file (" + url + "): " + ex);
      throw ex;
    }

    if (in != null) {
      char[] buf = new char[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
	out.write(buf, 0, len);
      }

      out.flush();
      m_fileContents = out.toString();
      in.close();
      out.close();
    }

    return m_fileContents != null;
  }

  /**
   * Used for logging and testing and debugging.
   */
  public String toString() {
    return "{url=" + m_fileUrl + "; isLoaded=" + (m_fileContents != null) +
      "; lastModified=" + m_lastModified + "}";
  }
}
