/*
 * $Id: BaseUrlCacher.java,v 1.30 2004-03-10 08:49:55 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

import org.lockss.app.*;
import org.lockss.plugin.*;
import org.lockss.repository.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * Basic, fully functional UrlCacher.  Utilizes the LockssRepository for
 * caching, and {@link LockssUrlConnection}s for fetching.  Plugins may
 * extend this to achieve, <i>eg</i>, specialized host connection or
 * authentication.  The redirection semantics offered here must be
 * preserved.
 */
public class BaseUrlCacher implements UrlCacher {
  protected static Logger logger = Logger.getLogger("UrlCacher");

  /** Maximum number of redirects that will be followed */
  static final int MAX_REDIRECTS = 10;

  // Preferred date format according to RFC 2068(HTTP1.1),
  // RFC 822 and RFC 1123
  public static SimpleDateFormat GMT_DATE_FORMAT =
    new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
  static {
    GMT_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  protected CachedUrlSet cus;
  protected Plugin plugin;
  protected String origUrl;		// URL with which I was created
  protected String fetchUrl;		// possibly affected by redirects
  private List otherNames;
  protected boolean forceRefetch = false;
  protected int redirectScheme = REDIRECT_SCHEME_FOLLOW;
  private LockssUrlConnectionPool connectionPool;
  private LockssUrlConnection conn;
  private LockssRepository repository;
  private CacheResultMap resultMap;
  private CIProperties uncachedProperties;

  public BaseUrlCacher(CachedUrlSet owner, String url) {
    this.cus = owner;
    this.origUrl = url;
    this.fetchUrl = url;
    ArchivalUnit au = owner.getArchivalUnit();
    plugin = au.getPlugin();
    repository = plugin.getDaemon().getLockssRepository(au);
    resultMap = ((BasePlugin)plugin).getCacheResultMap();
  }

  /**
   * Returns the original URL (the one the UrlCacher was created with),
   * independent of any redirects followed.
   * @return the url string
   */
  public String getUrl() {
    return origUrl;
  }

  /**
   * Overrides normal <code>toString()</code> to return a string like
   * "BUC: <url>"
   * @return the class-url string
   */
  public String toString() {
    return "[BUC: "+origUrl+"]";
  }

  /**
   * Return the CachedUrlSet to which this UrlCacher belongs.
   * @return the owner CachedUrlSet
   */
  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  /**
   * Return the ArchivalUnit to which this CachedUrl belongs.
   * @return the owner ArchivalUnit
   */
  public ArchivalUnit getArchivalUnit() {
    return cus.getArchivalUnit();
  }

  /**
   * Return <code>true</code> if the underlying url is one that the plug-in
   * believes should be preserved.  (<i>Ie</i>, is within the AU's
   * crawlSpec.)
   * @return a boolean indicating if it should be cached
   */
  public boolean shouldBeCached() {
    return getArchivalUnit().shouldBeCached(origUrl);
  }

  /**
   * Return a CachedUrl for the content stored.  May be
   * called only after the content is completely written.
   * @return CachedUrl for the content stored.
   */
  public CachedUrl getCachedUrl() {
    return plugin.makeCachedUrl(cus, origUrl);
  }

  public void setConnectionPool(LockssUrlConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  public void setForceRefetch(boolean force) {
    this.forceRefetch = force;
  }

  public void setRedirectScheme(int scheme) {
    this.redirectScheme = scheme;
  }

  public void cache() throws IOException {
    String lastModified = null;
    if (!forceRefetch) {
      CachedUrl cachedVersion = plugin.makeCachedUrl(cus, origUrl);

      // if it's been cached, get the last modified date and use that
      if ((cachedVersion!=null) && cachedVersion.hasContent()) {
	CIProperties cachedProps = cachedVersion.getProperties();
	lastModified =
	  cachedProps.getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);
      }
    }
    cache(lastModified);
  }

  private void cache(String lastModified) throws IOException {
    logger.debug3("Pausing before fetching content");
    getArchivalUnit().pauseBeforeFetch();
    logger.debug3("Done pausing");
    InputStream input = getUncachedInputStream(lastModified);
    // null input indicates unmodified content, so skip caching
    if (input!=null) {
      try {
	CIProperties headers = getUncachedProperties();
	if (headers == null) {
	  String err = "Received null headers for url '" + origUrl + "'.";
	  logger.error(err);
	  throw new NullPointerException(err);
	}
	storeContent(input, headers);
      } finally {
	input.close();
      }
    }
  }

  /** Store into the repository the content and headers from a successful
   * fetch.  If redirects were followed and the redirect scheme is
   * REDIRECT_SCHEME_STORE_ALL, store the content and headers under each
   * name in the chain of redirections.
   */
  public void storeContent(InputStream input, CIProperties headers)
      throws IOException {
    if (logger.isDebug2()) logger.debug2("Storing url '"+ origUrl +"'");
    storeContentIn(origUrl, input, headers);
    if (otherNames != null) {
      CachedUrl cu = getCachedUrl();
      for (Iterator iter = otherNames.iterator(); iter.hasNext(); ) {
	String name = (String)iter.next();
	if (logger.isDebug2())
	  logger.debug2("Storing in redirected-to url '"+ name +"'");
	InputStream is = cu.getUnfilteredInputStream();
	if (name.equals(fetchUrl)) {
	  // this one was not redirected, don't store a redirected-to property
	  CIProperties newHeaders  = new CIProperties();
	  for (Iterator pi = headers.keySet().iterator(); pi.hasNext(); ) {
	    String key = (String)pi.next();
	    if (!key.equalsIgnoreCase(CachedUrl.PROPERTY_REDIRECTED_TO)) {
	      newHeaders.setProperty(key, headers.getProperty(key));
	    }
	  }
	  storeContentIn(name, is, newHeaders);
	} else {
	  storeContentIn(name, is, headers);
	}
      }
    }
  }

  public void storeContentIn(String url, InputStream input, CIProperties headers)
      throws IOException {
    RepositoryNode leaf = null;
    try {
      leaf = repository.createNewNode(url);
      leaf.makeNewVersion();

      OutputStream os = leaf.getNewOutputStream();
      StreamUtil.copy(input, os);
      input.close();
      os.close();
    }
    catch (IOException ex) {
      throw resultMap.getRepositoryException(ex);
    }

    leaf.setNewProperties(headers);
    leaf.sealNewVersion();
  }

  public InputStream getUncachedInputStream() throws IOException {
    return getUncachedInputStream(null);
  }

  /**
   * Gets an InputStream for this URL, using the last modified time as
   * 'if-modified-since'.  If a 304 is generated (not modified), it returns
   * null.
   * @param lastModified the last modified time
   * @return the InputStream, or null
   * @throws IOException
   */
  protected InputStream getUncachedInputStream(String lastModified)
      throws IOException {
    InputStream input = null;
    try {
      openConnection(lastModified);
      if (conn.isHttp()) {
	// http connection; check response code
	int code = conn.getResponseCode();
	if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
	  logger.debug2("Unmodified content not cached for url '" +
			origUrl + "'");
	  return null;
	}
      }
      input = conn.getResponseInputStream();
    } finally {
      if (conn != null && input == null) {
	conn.release();
      }
    }
    return input;
  }

  public CIProperties getUncachedProperties() throws IOException {
    if (conn == null) {
      throw new UnsupportedOperationException("Called getUncachedProperties before calling getUncachedInputStream.");
    }
//     openConnection();
    if (uncachedProperties == null) {
      CIProperties props = new CIProperties();
      // set header properties in which we have interest

      props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE,
			conn.getResponseContentType());
      props.setProperty(CachedUrl.PROPERTY_FETCH_TIME,
			Long.toString(TimeBase.nowMs()));
      // XXX this property does not have consistent semantics.  It will be
      // set to the first url in a chain of redirects that led to content,
      // which could be different depending on fetch order.
      props.setProperty(CachedUrl.PROPERTY_ORIG_URL, origUrl);
      conn.storeResponseHeaderInto(props, CachedUrl.HEADER_PREFIX);
      String actualURL = conn.getActualUrl();
      if (!origUrl.equals(actualURL)) {
	props.setProperty(CachedUrl.PROPERTY_REDIRECTED_TO, actualURL);
      }
      uncachedProperties = props;
    }
    return uncachedProperties;
  }

  void checkConnectException(LockssUrlConnection conn) throws IOException {
    if(conn.isHttp()) {
      if (logger.isDebug3()) {
	logger.debug3("Response: " + conn.getResponseCode() + ": " +
		      conn.getResponseMessage());
      }
      CacheException c_ex = resultMap.checkResult(conn);
      if(c_ex != null) {
	// The stack below here is misleading.  Makes more sense for it
	// to reflect the point at which it's thrown
	c_ex.fillInStackTrace();
        throw c_ex;
      }
    }
  }

  /**
   * If we haven't already connected, creates a connection from url, setting
   * the user-agent and ifmodifiedsince values.  Then actually connects to the
   * site and throws if we get an error code
   */
  private void openConnection(String lastModified) throws IOException {
    if (conn==null) {
      switch (redirectScheme) {
      case REDIRECT_SCHEME_FOLLOW:
      case REDIRECT_SCHEME_DONT_FOLLOW:
	openOneConnection(lastModified);
	break;
      case REDIRECT_SCHEME_STORE_ALL:
	openWithRedirects(lastModified);
	break;
      }
    }
  }

  private void openWithRedirects(String lastModified) throws IOException {
    int retry = 0;
    while (true) {
      try {
	openOneConnection(lastModified);
	break;
      } catch (CacheException.NoRetryNewUrlException e) {
	if (++retry >= MAX_REDIRECTS) {
	  logger.warning("Max redirects hit, not redirecting " + origUrl +
			 " past " + fetchUrl);
	  throw e;
	} else if (!processRedirectResponse()) {
	  throw e;
	}
      }
    }
  }

  protected LockssUrlConnection makeConnection(String url,
					       LockssUrlConnectionPool pool)
      throws IOException {
    return UrlUtil.openConnection(url, pool);
  }

  /**
   * If we haven't already connected, creates a connection from url, setting
   * the user-agent and ifmodifiedsince values.  Then actually connects to the
   * site and throws if we get an error code
   */
  private void openOneConnection(String lastModified) throws IOException {
    try {
      conn = makeConnection(fetchUrl, connectionPool);
      switch (redirectScheme) {
      case REDIRECT_SCHEME_FOLLOW:
	conn.setFollowRedirects(true);
	break;
      case REDIRECT_SCHEME_STORE_ALL:
      case REDIRECT_SCHEME_DONT_FOLLOW:
	conn.setFollowRedirects(false);
	break;
      }
      conn.setRequestProperty("user-agent", LockssDaemon.getUserAgent());
      if (lastModified != null) {
	conn.setIfModifiedSince(lastModified);
      }
      conn.execute();
    }
    catch (IOException ex) {
      logger.warning("openConnection", ex);
      throw resultMap.getHostException(ex);
    } catch (RuntimeException e) {
      logger.warning("openConnection: unexpected exception", e);
      throw e;
    }
    checkConnectException(conn);
  }

  private boolean processRedirectResponse() {
    //get the location header to find out where to redirect to
    String location = conn.getResponseHeaderValue("location");
    if (location == null) {
      // got a redirect response, but no location header
      logger.error("Received redirect response " + conn.getResponseCode()
		   + " but no location header");
      return false;
    }
    if (logger.isDebug3()) {
      logger.debug3("Redirect requested from '" + fetchUrl +
		    "' to '" + location + "'");
    }
    // update the current location with the redirect location.
    try {
      String newUrlString = UrlUtil.resolveUri(fetchUrl, location);
      if (redirectScheme == REDIRECT_SCHEME_STORE_ALL) {
	if (!getArchivalUnit().shouldBeCached(newUrlString)) {
	  logger.warning("Redirect not in crawl spec: " + newUrlString +
			 " from: " + origUrl);
	  return false;
	}
      }
      conn.release();
      conn = null;

      // XXX
      // The names .../foo and .../foo/ map to the same repository node, so
      // the case of a slash-appending redirect requires special handling.
      // (Still. sigh.)  We want to record the fact of the redirection, but
      // only end up with one name in the list, so don't add the new one if
      // it's just a slash addition

      if (!isSlashAppended(fetchUrl, newUrlString)) {
	if (otherNames == null) {
	  otherNames = new ArrayList();
	}
	otherNames.add(newUrlString);
      }
      fetchUrl = newUrlString;
      logger.debug2("Following redirect to " + newUrlString);
      return true;
    } catch (MalformedURLException e) {
      logger.warning("Redirected location '" + location + "' is malformed", e);
      return false;
    }
  }

  boolean isSlashAppended(String name, String redir) {
    int len = name.length();
    if (redir.length() != (len + 1)) return false;
    if (redir.charAt(len) != '/') return false;
    if (redir.startsWith(name)) return true;
    try {
      URL uname = new URL(name);
      URL uredir = new URL(redir);
      return (uname.getHost().equalsIgnoreCase(uredir.getHost()) &&
	      uname.getProtocol().equalsIgnoreCase(uredir.getProtocol()) &&
	      uname.getPort() == uredir.getPort() &&
	      uredir.getPath().startsWith(uname.getPath()));
    } catch (MalformedURLException e) {
      return false;
    }
  }
}
