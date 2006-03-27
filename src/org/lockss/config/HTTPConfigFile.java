/*
 * $Id: HTTPConfigFile.java,v 1.7 2006-03-27 08:49:55 tlipkis Exp $
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
import java.util.zip.*;

import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * A ConfigFile loaded from a URL.
 *
 */
public class HTTPConfigFile extends ConfigFile {

  public static final String PREFIX = Configuration.PREFIX + "config.";

  // Connect and data timeouts
  /** Connect Timeout for property server */
  public static final String PARAM_CONNECT_TIMEOUT = PREFIX+ "timeout.connect";
  /** Data timeout for property server */
  public static final long DEFAULT_CONNECT_TIMEOUT = 1 * Constants.MINUTE;
  public static final String PARAM_DATA_TIMEOUT = PREFIX + "timeout.data";
  public static final long DEFAULT_DATA_TIMEOUT = 10 * Constants.MINUTE;

  private String m_httpLastModifiedString = null;

  public HTTPConfigFile(String url) {
    super(url);
  }

  // overridden for testing
  protected LockssUrlConnection openUrlConnection(String url)
      throws IOException {
    // XXX If/when more than one file is fetched from props server, this
    // should use a common connection pool so connection gets resused.
    // XXX should do something about closing connection promptly
    LockssUrlConnectionPool connPool = new LockssUrlConnectionPool();
    Configuration conf = ConfigManager.getCurrentConfig();

    connPool.setConnectTimeout(conf.getTimeInterval(PARAM_CONNECT_TIMEOUT,
						    DEFAULT_CONNECT_TIMEOUT));
    connPool.setDataTimeout(conf.getTimeInterval(PARAM_DATA_TIMEOUT,
						 DEFAULT_DATA_TIMEOUT));
    return UrlUtil.openConnection(url, connPool);
  }

  /** Don't check for new file on every load, only when asked.
   */
  protected boolean isCheckEachTime() {
    return false;
  }

  /**
   * Given a URL, open an input stream, handling the appropriate
   * if-modified-since behavior.
   */
  private InputStream getUrlInputStream(String url)
      throws IOException, MalformedURLException {
    InputStream in = null;

    LockssUrlConnection conn = openUrlConnection(url);

    if (m_config != null && m_lastModified != null) {
      log.debug2("Setting request if-modified-since to: " + m_lastModified);
      conn.setIfModifiedSince(m_lastModified);
    }
    conn.setRequestProperty("Accept-Encoding", "gzip");

    conn.execute();

    int resp = conn.getResponseCode();
    String respMsg = conn.getResponseMessage();
    log.debug2(url + " request got response: " + resp + ": " + respMsg);
    switch (resp) {
    case HttpURLConnection.HTTP_OK:
      m_loadError = null;
      m_httpLastModifiedString = conn.getResponseHeaderValue("last-modified");
      in = conn.getResponseInputStream();
      log.debug2("New file, or file changed.  Loading file from " +
		 "remote connection:" + url);
      String encoding = conn.getResponseContentEncoding();
      if ("gzip".equalsIgnoreCase(encoding) ||
	  "x-gzip".equalsIgnoreCase(encoding)) {
	log.debug3("Wrapping in GZIPInputStream");
	in = new GZIPInputStream(in);
      }
      break;
    case HttpURLConnection.HTTP_NOT_MODIFIED:
      m_loadError = null;
      log.debug2("HTTP content not changed, not reloading.");
      IOUtil.safeRelease(conn);
      break;
    case HttpURLConnection.HTTP_NOT_FOUND:
      m_loadError = conn.getResponseMessage();
      throw new FileNotFoundException(m_loadError);
    default:
      m_loadError = resp + ": " + respMsg;
      throw new IOException(m_loadError);
    }

    return in;
  }

  protected InputStream openInputStream() throws IOException {
    InputStream in = null;
    m_IOException = null;

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
	if (in == null) {
	  throw new FileNotFoundException("No XML file: " + xmlUrl);
	}
	// This is really an XML file, deceitfully set the URL and
	// file type for when we reload.
	m_fileType = XML_FILE;
	m_fileUrl = xmlUrl;
      } catch (Exception dontCare) {
	// Couldn't load it as an XML file, try to load the real URL name.
	log.debug2("Second pass: That didn't work, trying to " +
		   "load original URL: " + m_fileUrl);
	in = getUrlInputStream(m_fileUrl);
      }
    } else {
      in = getUrlInputStream(m_fileUrl);
    }
    return in;
  }

  protected String calcNewLastModified() {
    return m_httpLastModifiedString;
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
