/*
 * $Id: ConfigFile.java,v 1.5 2004-07-19 08:26:22 tlipkis Exp $
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
import java.util.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * A simple wrapper class around the text representation of a
 * generic config (either plain text or XML)
 */

public class ConfigFile {
  public static final int XML_FILE = 0;
  public static final int PROPERTIES_FILE = 1;

  private int m_fileType;
  private String m_lastModified;
  private String m_fileUrl;
  private String m_fileContents;
  private String m_loadError = "Not loaded yet";
  private IOException m_IOException;
  private long m_lastAttempt;

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

  public long getLastAttemptTime() {
    return m_lastAttempt;
  }

  public String getLoadErrorMessage() {
    return m_loadError;
  }

  public boolean isLoaded() {
    return m_loadError == null;
  }

  /**
   * Return an inputstream of the file contents.
   */
  public InputStream getInputStream() throws IOException {
    if (m_fileContents == null) {
      if (m_IOException != null) {
	throw m_IOException;
      } else if (m_loadError != null) {
	throw new IOException("Error reading file: " + m_loadError);
      } else {
	throw new IllegalStateException("Config file not loaded: " +
					this.toString());
      }
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
   * KLUDGE: Part of the XML configuration file transition.
   *
   * Given a URL, return a version that ends with ".xml".  For
   * example, "lockss.txt" -> "lockss.xml", "foobar" -> "foobar.xml"
   */
  private String makeXmlUrl(String url) {
    return StringUtil.upToFinal(url, ".") + ".xml";
  }

  /**
   * KLUDGE: Part of the XML configuration file transition.
   *
   * Given a URL, attempt to open an input stream.
   */
  private Reader getUrlInputStream(String url)
      throws IOException, MalformedURLException {
    Reader in = null;

    URL u = new URL(url);
    LockssUrlConnection conn = UrlUtil.openConnection(url);

    if (m_fileContents != null && m_lastModified != null) {
      log.debug2("Setting request if-modified-since to: " + m_lastModified);
      conn.setIfModifiedSince(m_lastModified);
    }

    conn.execute();

    if (conn.isHttp()) {
      int resp = conn.getResponseCode();
      String respMsg = conn.getResponseMessage();
      log.debug2(url + " request got response: " + resp + ": " + respMsg);
      switch (resp) {
      case HttpURLConnection.HTTP_OK:
	m_loadError = null;
	m_lastModified = conn.getResponseHeaderValue("last-modified");
	log.debug2("Storing this config's last-modified as: " +
		   m_lastModified);
	in = new InputStreamReader(conn.getResponseInputStream());
	log.debug2("New file, or file changed.  Loading file from " +
		   "remote connection:" + url);
	break;
      case HttpURLConnection.HTTP_NOT_MODIFIED:
	m_loadError = null;
	log.debug3("Not reloading");
	break;
      default:
	m_loadError = resp + ": " + respMsg;
      }
    } else {
      in = new InputStreamReader(conn.getResponseInputStream());
      m_loadError = null;
    }
    return in;
  }

  /**
   * Load a URL or file.  If there is no current "last modified"
   * time, try to load it unconditionally.  Otherwise, send a
   * conditional GET with an "if-modified-since" header.
   */
  private synchronized boolean load(String url) throws IOException {
    m_lastAttempt = TimeBase.nowMs();
    Reader in = null;
    Writer out = new StringWriter();
    m_fileUrl = url;

    // TODO: This check may or may not even be necessary.  If it
    // proves to be useful, it may need to be improved in the
    // future.
    if (StringUtil.endsWithIgnoreCase(url, ".xml")) {
      m_fileType = ConfigFile.XML_FILE;
    } else {
      m_fileType = ConfigFile.PROPERTIES_FILE;
    }

    m_IOException = null;
    // Open an output stream to write to our string
    try {
      // KLUDGE: Part of the XML config file transition.  If the file
      // is already an XML file, just try to open it.
      //
      // Otherwise, first XML-ize the URL and try to open it.  If that
      // doesn't work, try opening the original URL.
      //
      // This logic can and should go away when we're no longer in a
      // transition period, and the platform knows about XML config
      // files.
      if (m_fileType == XML_FILE) {
	in = getUrlInputStream(url);
      } else {
	String xmlUrl = makeXmlUrl(url);

	try {
	  log.debug2("First pass: Trying to load XML-ized URL: " + xmlUrl);
	  in = getUrlInputStream(xmlUrl);
	} catch (Exception ignore) {;}
	
	if (in != null) {
	  // This is really an XML file, deceitfully set the URL and
	  // file type for when we reload.
	  m_fileType = XML_FILE;
	  m_fileUrl = xmlUrl;
	} else {
	  // This is not an XML file, try to load the real URL name.
	  log.debug2("Second pass: That didn't work, trying to " +
		     "load original URL: " + url);
	  in = getUrlInputStream(url);
	}

      }
    } catch (MalformedURLException ex) {
      in = new InputStreamReader(new FileInputStream(url));
      log.debug2("Loading local file unconditionally: " + url);
    } catch (IOException ex) {
      log.warning("Unexpected exception trying to load " +
		  "config file (" + url + "): " + ex);
      m_IOException = ex;
      throw ex;
    }

    if (in != null) {
      StreamUtil.copy(in, out);

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
