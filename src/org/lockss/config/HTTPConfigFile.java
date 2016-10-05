/*
 * $Id$
 */

/*

Copyright (c) 2001-2016 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.io.input.TeeInputStream;

import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.hasher.*;
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

  public static final String PARAM_CHARSET_UTIL = PREFIX + "charset.util";
  public static final boolean DEFAULT_CHARSET_UTIL = true;

  private String m_httpLastModifiedString = null;

  private LockssUrlConnectionPool m_connPool;
  private boolean checkAuth = false;
  private boolean charsetUtil = true;
  private MessageDigest chkDig;
  private String chkAlg;

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
      String ctype = conn.getResponseContentType();
      String charset = HeaderUtil.getCharsetOrDefaultFromContentType(ctype);
      Reader rdr = CharsetUtil.getReader(in, charset);
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

  FileConfigFile failoverFcf;

  /** Return an InputStream open on the HTTP url.  If in accessible and a
      local copy of the remote file exists, failover to it. */
  protected InputStream openInputStream() throws IOException {
    try {
      InputStream in = openHttpInputStream();
      if (in != null) {
	// If we got remote content, clear any local failover copy as it
	// may now be obsolete
	failoverFcf = null;
      }
      return in;
    } catch (IOException e) {
      // The HTTP fetch failed.  First see if we already found a failover
      // file.
      if (failoverFcf == null) {
	if (m_cfgMgr == null) {
	  throw e;
	}
	ConfigManager.RemoteConfigFailoverInfo rcfi =
	  m_cfgMgr.getRcfi(m_fileUrl);
	if (rcfi == null || !rcfi.exists()) {
	  throw e;
	}
	File failoverFile = rcfi.getPermFileAbs();
	if (failoverFile == null) {
	  throw e;
	}
	String chksum = rcfi.getChksum();
	if (chksum != null) {
	  HashResult hr = HashResult.make(chksum);
	  try {
	    HashResult fileHash = hashFile(failoverFile, hr.getAlgorithm());
	    if (!hr.equals(fileHash)) {
	      log.error("Failover file checksum mismatch");
	      if (log.isDebug2()) {
		log.debug2("state   : " + hr);
		log.debug2("computed: " + fileHash);
	      }
	      throw new IOException("Failover file checksum mismatch");
	    }
	  } catch (NoSuchAlgorithmException nsae) {
	    log.error("Failover file found has unsupported checksum: " +
		      hr.getAlgorithm());
	    throw e;
	  } catch (IOException ioe) {
	    log.error("Can't read failover file", ioe);
	    throw e;
	  }
	} else if (CurrentConfig.getBooleanParam(ConfigManager.PARAM_REMOTE_CONFIG_FAILOVER_CHECKSUM_REQUIRED,
						 ConfigManager.DEFAULT_REMOTE_CONFIG_FAILOVER_CHECKSUM_REQUIRED)) {
	  log.error("Failover file found but required checksum is missing");
	  throw e;
	}

	// Found one, 
	long date = rcfi.getDate();
	log.info("Couldn't load remote config URL: " + m_fileUrl +
		 ": " + e.toString());
	log.info("Substituting local copy created: " + new Date(date));
	failoverFcf = new FileConfigFile(failoverFile.getPath());
	m_loadedUrl = failoverFile.getPath();
      }
      return failoverFcf.openInputStream();
    }
  }

  // XXX Find a place for this
  HashResult hashFile(File file, String alg)
      throws NoSuchAlgorithmException, IOException {
    InputStream is = null;
    try {
      MessageDigest md = MessageDigest.getInstance(alg);
      is = new BufferedInputStream(new FileInputStream(file));
      StreamUtil.copy(is,
		      new org.apache.commons.io.output.NullOutputStream(),
		      -1, null, false, md);
      return HashResult.make(md.digest(), alg);
    } finally {
      IOUtil.safeClose(is);
    }
  }

  protected InputStream openHttpInputStream() throws IOException {
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
    if (in != null) {
      m_loadedUrl = null; // we're no longer loaded from failover, if we were.
      File tmpCacheFile;
      // If so configured, save the contents of the remote file in a locally
      // cached copy.
      if (m_cfgMgr != null &&
	  (tmpCacheFile =
	   m_cfgMgr.getRemoteConfigFailoverTempFile(m_fileUrl)) != null) {
	try {
	  log.log((  m_cfgMgr.haveConfig()
		     ? Logger.LEVEL_DEBUG
		     : Logger.LEVEL_INFO),
		  "Copying remote config: " + m_fileUrl);
	  OutputStream out =
	    new BufferedOutputStream(new FileOutputStream(tmpCacheFile));
	  out = makeHashedOutputStream(out);
	  out = new GZIPOutputStream(out, true);
	  InputStream wrapped = new TeeInputStream(in, out, true);
	  return wrapped;
	} catch (IOException e) {
	  log.error("Error opening remote config failover temp file: " +
		    tmpCacheFile, e);
	  return in;
	}
      }
    }
    return in;
  }

  OutputStream makeHashedOutputStream(OutputStream out) {
    String hashAlg =
      CurrentConfig.getParam(ConfigManager.PARAM_REMOTE_CONFIG_FAILOVER_CHECKSUM_ALGORITHM,
			     ConfigManager.DEFAULT_REMOTE_CONFIG_FAILOVER_CHECKSUM_ALGORITHM);
    if (!StringUtil.isNullString(hashAlg)) {
      try {
	chkDig = MessageDigest.getInstance(hashAlg);
	chkAlg = hashAlg;
	return new HashedOutputStream(out, chkDig);
      } catch (NoSuchAlgorithmException ex) {
	log.warning(String.format("Checksum algorithm %s not found, "
				  + "checksumming disabled", hashAlg));
      }
    }
    return out;
  }

  // Finished reading input, so failover file, if any, has now been
  // written and its checksum calculated.  Store it in rcfi.
  protected void loadFinished() {
    if (chkDig != null) {
      ConfigManager.RemoteConfigFailoverInfo rcfi =
	m_cfgMgr.getRcfi(m_fileUrl);
      if (rcfi != null) {
	HashResult hres = HashResult.make(chkDig.digest(), chkAlg);
	rcfi.setChksum(hres.toString());
      }
    }      
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
