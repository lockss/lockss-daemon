/*
 * $Id: HTTPConfigFile.java,v 1.2 2005-02-02 09:42:48 tlipkis Exp $
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

import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * A simple wrapper class around the text representation of a generic
 * config loaded from a remote location via HTTP (either plain text or
 * XML).
 */

public class HTTPConfigFile extends ConfigFile {

  public HTTPConfigFile(String url) throws IOException {
    super(url);
  }

  /**
   * Given a URL, open an input stream, handling the appropriate
   * if-modified-since behavior.
   */
  private InputStream getUrlInputStream(String url)
      throws IOException, MalformedURLException {
    InputStream in = null;
    
    LockssUrlConnection conn = UrlUtil.openConnection(url);

    if (m_config != null && m_lastModified != null) {
      log.debug2("Setting request if-modified-since to: " + m_lastModified);
      conn.setIfModifiedSince(m_lastModified);
    }

    conn.execute();

    int resp = conn.getResponseCode();
    String respMsg = conn.getResponseMessage();
    log.debug2(url + " request got response: " + resp + ": " + respMsg);
    switch (resp) {
    case HttpURLConnection.HTTP_OK:
      m_loadError = null;
      m_lastModified = conn.getResponseHeaderValue("last-modified");
      log.debug2("Storing this config's last-modified as: " +
		 m_lastModified);
      in = conn.getResponseInputStream();
      log.debug2("New file, or file changed.  Loading file from " +
		 "remote connection:" + url);
      break;
    case HttpURLConnection.HTTP_NOT_MODIFIED:
      m_loadError = null;
      log.debug2("HTTP content not changed, not reloading.");
      break;
    default:
      m_loadError = resp + ": " + respMsg;
    }
    
    return in;
  }

  /**
   * Load a URL or file.  If there is no current "last modified"
   * time, try to load it unconditionally.  Otherwise, send a
   * conditional GET with an "if-modified-since" header.
   */
  protected synchronized boolean reload() throws IOException {
    m_lastAttempt = TimeBase.nowMs();
    InputStream in = null;
    m_IOException = null;

    // Open an output stream to write to our string
    try {
      // KLUDGE: Part of the XML config file transition.  If this is
      // an HTTP URL and we have never loaded the file before, see if an
      // XML version of the file is available first.  If none can be
      // found, try the original URL.
      //
      // This logic can and should go away when we're no longer in a
      // transition period, and the platform knows about XML config
      // files.
      if (!Boolean.getBoolean("org.lockss.config.noXmlHack") &&
	  m_config == null &&
	  m_fileType == PROPERTIES_FILE) {
	String xmlUrl = makeXmlUrl(m_fileUrl);

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
		     "load original URL: " + m_fileUrl);
	  in = getUrlInputStream(m_fileUrl);
	}
      } else {
	in = getUrlInputStream(m_fileUrl);	
      }
    } catch (MalformedURLException ex) {
      log.error("Malformed config file URL: " + ex);
      return false;
    } catch (IOException ex) {
      log.warning("Unexpected exception trying to load " +
		  "config file (" + m_fileUrl + "): " + ex);
      m_IOException = ex;
      throw ex;
    }

    // "in" will be null if HTTP contents did not change (this is the
    // usual case), or if getUrlInputStream threw an error.
    if (in != null) {
      setConfigFrom(in);
      in.close();
    }

    return m_IOException == null;
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

}
