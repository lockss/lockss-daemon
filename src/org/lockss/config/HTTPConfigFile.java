/*
 * $Id$
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
import org.apache.oro.text.regex.*;

/**
 * A ConfigFile loaded from a URL.
 *
 */
public class HTTPConfigFile extends BaseConfigFile {

  public static final String PREFIX = Configuration.PREFIX + "config.";

  // Connect and data timeouts
  /** Amount of time the daemon will wait for the property server to open
   * a connection. */
  public static final String PARAM_CONNECT_TIMEOUT = PREFIX+ "timeout.connect";
  /** Amount of time the daemon will wait to receive data on an open
   * connection to the property server. */
  public static final long DEFAULT_CONNECT_TIMEOUT = 1 * Constants.MINUTE;

  public static final String PARAM_DATA_TIMEOUT = PREFIX + "timeout.data";
  public static final long DEFAULT_DATA_TIMEOUT = 10 * Constants.MINUTE;

  private String m_httpLastModifiedString = null;

  private LockssUrlConnectionPool m_connPool;
  private boolean checkAuth = false;

  public HTTPConfigFile(String url) {
    super(url);
  }

  public void setConnectionPool(LockssUrlConnectionPool connPool) {
    m_connPool = connPool;
  }

  LockssUrlConnectionPool getConnectionPool() {
    if (m_connPool == null) {
      m_connPool = new LockssUrlConnectionPool();
    }
    return m_connPool;
  }

  // overridden for testing
  protected LockssUrlConnection openUrlConnection(String url)
      throws IOException {
    Configuration conf = ConfigManager.getCurrentConfig();

    LockssUrlConnectionPool connPool = getConnectionPool();

    connPool.setConnectTimeout(conf.getTimeInterval(PARAM_CONNECT_TIMEOUT,
						    DEFAULT_CONNECT_TIMEOUT));
    connPool.setDataTimeout(conf.getTimeInterval(PARAM_DATA_TIMEOUT,
						 DEFAULT_DATA_TIMEOUT));
    LockssUrlConnection conn = UrlUtil.openConnection(url, connPool);
    if (m_cfgMgr != null) {
      LockssSecureSocketFactory fact = m_cfgMgr.getSecureSocketFactory();
      if (fact != null) {
	checkAuth = true;
	conn.setSecureSocketFactory(fact);
      }
    }
    return conn;
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
    try {
      return getUrlInputStream0(url);
    } catch (javax.net.ssl.SSLHandshakeException e) {
      m_loadError = "Could not authenticate server: " + e.getMessage();
      throw new IOException(m_loadError, e);
    } catch (javax.net.ssl.SSLKeyException e) {
      m_loadError = "Could not authenticate; bad client or server key: "
	+ e.getMessage();
      throw new IOException(m_loadError, e);
    } catch (javax.net.ssl.SSLPeerUnverifiedException e) {
      m_loadError = "Could not verify server identity: " + e.getMessage();
      throw new IOException(m_loadError, e);
    } catch (javax.net.ssl.SSLException e) {
      m_loadError = "Error negotiating SSL seccion: " + e.getMessage();
      throw new IOException(m_loadError, e);
    }
  }

  private InputStream getUrlInputStream0(String url)
      throws IOException, MalformedURLException {
    InputStream in = null;
    LockssUrlConnection conn = openUrlConnection(url);

    Configuration conf = ConfigManager.getPlatformConfig();
    String proxySpec = conf.get(ConfigManager.PARAM_PROPS_PROXY);
    String proxyHost = null;
    int proxyPort = 0;

    try {
      HostPortParser hpp = new HostPortParser(proxySpec);
      proxyHost = hpp.getHost();
      proxyPort = hpp.getPort();
    } catch (HostPortParser.InvalidSpec e) {
      log.warning("Illegal props proxy: " + proxySpec, e);
    }

    if (proxyHost != null) {
      log.debug2("Setting request proxy to: " + proxyHost + ":" + proxyPort);
      conn.setProxy(proxyHost, proxyPort);
    }
    if (m_config != null && m_lastModified != null) {
      log.debug2("Setting request if-modified-since to: " + m_lastModified);
      conn.setIfModifiedSince(m_lastModified);
    }
    conn.setRequestProperty("Accept-Encoding", "gzip");

    if (m_props != null) {
      Object x = m_props.get(Constants.X_LOCKSS_INFO);
      if (x instanceof String) {
	conn.setRequestProperty(Constants.X_LOCKSS_INFO, (String)x);
      }
    }
    conn.execute();
    if (checkAuth && !conn.isAuthenticatedServer()) {
      IOUtil.safeRelease(conn);
      throw new IOException("Config server not authenticated");
    }

    int resp = conn.getResponseCode();
    String respMsg = conn.getResponseMessage();
    log.debug2(url + " request got response: " + resp + ": " + respMsg);
    switch (resp) {
    case HttpURLConnection.HTTP_OK:
      m_loadError = null;
      m_httpLastModifiedString = conn.getResponseHeaderValue("last-modified");
      log.debug2("New file, or file changed.  Loading file from " +
		 "remote connection:" + url);
      in = conn.getUncompressedResponseInputStream();
      break;
    case HttpURLConnection.HTTP_NOT_MODIFIED:
      m_loadError = null;
      log.debug2("HTTP content not changed, not reloading.");
      IOUtil.safeRelease(conn);
      break;
    case HttpURLConnection.HTTP_NOT_FOUND:
      m_loadError = resp + ": " + respMsg;
      IOUtil.safeRelease(conn);
      throw new FileNotFoundException(m_loadError);
    case HttpURLConnection.HTTP_FORBIDDEN:
      m_loadError = findErrorMessage(resp, conn);
      IOUtil.safeRelease(conn);
      throw new IOException(m_loadError);
    default:
      m_loadError = resp + ": " + respMsg;
      IOUtil.safeRelease(conn);
      throw new IOException(m_loadError);
    }

    return in;
  }

  private static Pattern HINT_PAT =
    RegexpUtil.uncheckedCompile("LOCKSSHINT: (.+) ENDHINT",
				Perl5Compiler.CASE_INSENSITIVE_MASK);


  // If there is a response body, include any text between LOCKSSHINT: and
  // ENDHINT in the error message.
  private String findErrorMessage(int resp, LockssUrlConnection conn) {
    String msg = resp + ": " + conn.getResponseMessage();
    try {
      long len = conn.getResponseContentLength();
      if (len == 0 || len > 10000) {
	return msg;
      }
      InputStream in = conn.getUncompressedResponseInputStream();
      // XXX should use the charset parameter in the Content-Type: header
      // if any
      String ctype = conn.getResponseContentType();
      String charset = HeaderUtil.getCharsetOrDefaultFromContentType(ctype);
      Reader rdr = new InputStreamReader(in, charset);
      String body = StringUtil.fromReader(rdr, 10000);
      if (StringUtil.isNullString(body)) {
	return msg;
      }
      Perl5Matcher matcher = RegexpUtil.getMatcher();
      if (matcher.contains(body, HINT_PAT)) {
	MatchResult matchResult = matcher.getMatch();
	String hint = matchResult.group(1);
	return msg + "\n" + hint;
      }
      return msg;
    } catch (Exception e) {
      log.warning("Error finding hint", e);
      return msg;
    } finally {
      IOUtil.safeRelease(conn);
    }
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
