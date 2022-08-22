/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.app.LockssDaemon;
import org.lockss.config.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

/**
 * Non abstract base class that holds all the basic logic for fetching
 * urls and some http specific logic
 */
public class BaseUrlFetcher implements UrlFetcher {
  
  private static final Logger log = Logger.getLogger("BaseUrlFetcher");
  
  /** If true, use so_keepalive on server connections. */
  public static final String PARAM_SO_KEEPALIVE =
    Configuration.PREFIX + "baseuc.socketKeepAlive";
  public static final boolean DEFAULT_SO_KEEPALIVE = false;
  
  /** Limit on rewinding the network input stream after checking for a
   * login page.  If LoginPageChecker returns false after reading father
   * than this the page will be refetched. */
  public static final String PARAM_LOGIN_CHECKER_MARK_LIMIT =
    Configuration.PREFIX + "baseuc.loginPageCheckerMarkLimit";
  public static final int DEFAULT_LOGIN_CHECKER_MARK_LIMIT = 24 * 1024;
  
  /** Maximum number of redirects that will be followed */
  static final int MAX_REDIRECTS = 10;
  
  /** If true, normalize redirect targets (location header). */
  public static final String PARAM_NORMALIZE_REDIRECT_URL =
    Configuration.PREFIX + "baseuc.normalizeRedirectUrl";
  public static final boolean DEFAULT_NORMALIZE_REDIRECT_URL = true;
  
  public static final String SET_COOKIE_HEADER = "Set-Cookie";
  private static final String SHOULD_REFETCH_ON_SET_COOKIE =
    "refetch_on_set_cookie";
  private static final boolean DEFAULT_SHOULD_REFETCH_ON_SET_COOKIE = true;
  
  /** If true, any Referer sent with the request will be recorded in the
   * {@value CachedUrl.PROPERTY_REQ_REFERRER property.  Used by repair
   * crawler */
  public static final String PARAM_RECORD_REFERRER =
    Configuration.PREFIX + "baseuc.recordReferrer";
  public static final boolean DEFAULT_RECORD_REFERRER = true;
  
  /** If true, X-Lockss-Auid: header will be included in proxy requests.
   * Use in order to get an accurate copy of an AU from the audit proxy. */
  public static final String PARAM_PROXY_BY_AUID =
    Configuration.PREFIX + "baseuc.proxyByAuid";
  public static final boolean DEFAULT_PROXY_BY_AUID = false;
  
  /** If true, any thread watchdog will be stopped while waiting on a rate
   * limiter. */
  public static final String PARAM_STOP_WATCHDOG_DURING_PAUSE =
    Configuration.PREFIX + "baseuc.stopWatchdogDuringPause";
  public static final boolean DEFAULT_STOP_WATCHDOG_DURING_PAUSE = false;


  protected final String origUrl;	// URL with which I was created
  protected String fetchUrl;		// possibly affected by redirects
  protected RedirectScheme redirectScheme = REDIRECT_SCHEME_FOLLOW;
  protected LockssUrlConnectionPool connectionPool;
  protected LockssUrlConnection conn;
  protected String proxyHost = null;
  protected int proxyPort;
  protected IPAddr localAddr = null;
  protected BitSet fetchFlags;
  protected CIProperties uncachedProperties;
  protected List<String> redirectUrls;
  protected ArchivalUnit au;
  protected Properties reqProps;
  protected final CacheResultMap resultMap;
  protected CrawlRateLimiter crl;
  protected String previousContentType;
  protected UrlConsumerFactory urlConsumerFactory;
  protected CrawlerStatus crawlStatus;
  protected Crawler.CrawlerFacade crawlFacade;
  protected LockssWatchdog wdog;
  protected CrawlUrl curl;

  public BaseUrlFetcher(Crawler.CrawlerFacade crawlFacade, String url) {
    this.origUrl = url;
    this.fetchUrl = url;
    this.crawlFacade = crawlFacade;
    this.au = crawlFacade.getAu();
    Plugin plugin = au.getPlugin();
    resultMap = plugin.getCacheResultMap();
    crawlStatus = crawlFacade.getCrawlerStatus();
    fetchFlags = new BitSet();
  }
  
  public void setUrlConsumerFactory(UrlConsumerFactory consumer) {
    urlConsumerFactory = consumer;
  }
  
  protected UrlConsumerFactory getUrlConsumerFactory() {
    if (urlConsumerFactory == null) {
      urlConsumerFactory = au.getUrlConsumerFactory();
    }
    return urlConsumerFactory;
  }
  
  //This should only rethrow the CacheException if it is fatal
  public FetchResult fetch() throws CacheException {
    /*
     * If a RepositoryException is thrown, adds the URL to the fail set and
     * signals a STATUS_REPO_ERR for the URL, and returns NOT_FETCHED.
     * 
     * If a RedirectOutsideCrawlSpecException is thrown, adds the URL to the
     * Excluded URLs, and returns NOT_FETCHED.
     * 
     * If some other CacheException is thrown, adds the URL to the Failed URLs
     * and signals an error (sometimes STATUS_FETCH_ERROR) for the URL, and
     * returns NOT_FETCHED (if the CacheException is non-fatal) or rethrows (if
     * the CacheException is fatal).
     * 
     * If some other Exception is thrown, simply logs (if the crawl is being
     * aborted) or adds the URL to the fail set and signals a STATUS_FETCH_ERROR
     * (if not), and returns NOT_FETCHED.
     * 
     * Otherwise, returns the fetch result, which can span all values (FETCHED,
     * FETCHED_NOT_MODIFIED, NOT_FETCHED).
     * 
     * Note that currently, if the fetch throws an IOException, it falls under
     * the category of "some other Exception".
     */
    String lastModified = null;
    try{
      if (!forceRefetch()) {
        lastModified = getLastModified();
      }
      return fetchWithRetries(lastModified);
    } catch (CacheException.RepositoryException ex) {
      // Failed.  Don't try this one again during this crawl.
      crawlFacade.addToFailedUrls(origUrl);
      if (origUrl.equals(fetchUrl)) {
	log.error("Repository error with " + fetchUrl, ex);
      } else {
	log.error("Repository error with " + origUrl +
		  " redirected to " + fetchUrl, ex);
      }
      crawlStatus.signalErrorForUrl(origUrl, ex);
      if(!crawlStatus.isCrawlError()) {
        crawlStatus.setCrawlStatus(Crawler.STATUS_REPO_ERR);
      }
    } catch (CacheException.RedirectOutsideCrawlSpecException ex) {
      // Count this as an excluded URL
      crawlStatus.signalUrlExcluded(origUrl, ex.getMessage());
      if (fetchFlags.get(UrlCacher.IS_PERMISSION_FETCH)) {
	// and throw if fetching a permission page
	// XXX Awkward - suggests decision made at wrong level
	throw ex;
      } else {
	// else no error
      }
    } catch (CacheException ex) {
      // Failed.  Don't try this one again during this crawl.
      crawlFacade.addToFailedUrls(origUrl);
      crawlStatus.signalErrorForUrl(origUrl, ex);
      if (ex.isAttributeSet(CacheException.ATTRIBUTE_FAIL)) {
	if (origUrl.equals(fetchUrl)) {
	  log.siteError("Problem caching " + origUrl + ". Continuing", ex);
	} else {
	  log.siteError("Problem caching " + origUrl +
			" redirected to " + fetchUrl + ". Continuing", ex);
	}
        if(!crawlStatus.isCrawlError()) {
          crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR, ex.getMessage());
        }
      }
      if (ex.isAttributeSet(CacheException.ATTRIBUTE_FATAL) ||
	  fetchFlags.get(UrlCacher.IS_PERMISSION_FETCH)) {
        throw ex;
      }
    } catch (Exception ex) {
      if (crawlFacade.isAborted()) {
        log.debug("Expected exception while aborting crawl: " + ex);
      } else {
        crawlFacade.addToFailedUrls(origUrl);
        crawlStatus.signalErrorForUrl(origUrl, ex.getMessage(),
                                      CrawlerStatus.Severity.Error);
        if(!crawlStatus.isCrawlError()) {
          crawlStatus.setCrawlStatus(Crawler.STATUS_FETCH_ERROR);
        }
        //XXX not expected
        log.error("Unexpected Exception during crawl, continuing", ex);
      }
    } 
    return FetchResult.NOT_FETCHED;
  }
  
  protected FetchResult fetchWithRetries(String lastModified)
      throws IOException {
    int retriesLeft = -1;
    int totalRetries = -1;
    InputStream input = null;
    CIProperties headers = null;
    log.debug2("Fetching " + origUrl);
    while (true) {
      try {
        if (wdog != null) {
          wdog.pokeWDog();
        }
        input = getUncachedInputStream(lastModified);
        headers = getUncachedProperties();
        if (input == null){
          //If input is null then ifModifiedSince returned not modified
	  log.debug3("Not modified: " + fetchUrl);
          return FetchResult.FETCHED_NOT_MODIFIED;
        } else if (headers == null) {
          // This is impossible
          throw resultMap.mapException(au, conn,
                                       new IllegalStateException("headers can't be empty"),
                                       null);
        } else {
          FetchedUrlData fud = new FetchedUrlData(origUrl, fetchUrl,
						  input, headers,
						  redirectUrls, this);
          fud.setStoreRedirects(redirectScheme.isRedirectOption(RedirectScheme.REDIRECT_OPTION_STORE_ALL));
          fud.setFetchFlags(fetchFlags);
          consume(fud);
          return FetchResult.FETCHED;
        }
      } catch (CacheException e) {
        if (!e.isAttributeSet(CacheException.ATTRIBUTE_RETRY)) {
	  log.debug3("No retry", e);
          throw e;
        }
        if (retriesLeft < 0) {
          retriesLeft = crawlFacade.getRetryCount(e);
          totalRetries = retriesLeft;
        }
        if (log.isDebug2()) {
          log.debug("Retryable (" + retriesLeft + ") exception caching "
		       + origUrl, e);
        } else {
          log.debug("Retryable (" + retriesLeft + ") exception caching "
		       + origUrl + ": " + e.toString());
        }
        if (--retriesLeft > 0) {
          long delayTime = crawlFacade.getRetryDelay(e);
          Deadline wait = Deadline.in(delayTime);
          log.debug3("Waiting " +
			StringUtil.timeIntervalToString(delayTime) +
			" before retry");
          while (!wait.expired()) {
            try {
              wait.sleep();
            } catch (InterruptedException ie) {
              // no action
            }
          }
          reset();
        } else {
          log.warning("Failed to cache (" + totalRetries + "), skipping: "
			 + origUrl);
          throw e;
        }
      } finally {
        IOUtil.safeClose(input);
      }
    }
  }
  
  protected void consume(FetchedUrlData fud) throws IOException {
    getUrlConsumerFactory().createUrlConsumer(crawlFacade, fud).consume();
  }

  protected String getLastModified(){
    String lastModified = null;
    CachedUrl cachedVersion = au.makeCachedUrl(origUrl);
    if ((cachedVersion!=null) && cachedVersion.hasContent()) {
      CIProperties cachedProps = cachedVersion.getProperties();
      lastModified =
	cachedProps.getProperty(CachedUrl.PROPERTY_LAST_MODIFIED);
      cachedVersion.release();
    }
    return lastModified;
  }
  
  protected boolean forceRefetch(){
    return fetchFlags.get(UrlCacher.REFETCH_FLAG) ||
      (isStateChangingMethod(getMethod()) && forceRefetchOnPost());
  }

  /** Override if state-changing requests (e.g.,POST) should check for
   * and send If-Modified-Since */
  protected boolean forceRefetchOnPost(){
    return true;
  }

  protected boolean isStateChangingMethod(int method) {
    switch (method) {
    case LockssUrlConnection.METHOD_POST:
//     case LockssUrlConnection.METHOD_PUT:
//     case LockssUrlConnection.METHOD_DELETE:
      return true;
    default:
      return false;
    }
  }
  
  public void setConnectionPool(LockssUrlConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }
  
  public void setRedirectScheme(RedirectScheme scheme) {
    if (log.isDebug3()) log.debug3("setRedirectScheme: " + scheme);
    this.redirectScheme = scheme;
  }
  
  public void setCrawlUrl(CrawlUrl curl) {
    if(curl.getUrl().equals(origUrl)) {
      this.curl = curl;
    }
  }
  
  public void setProxy(String proxyHost, int proxyPort) {
    this.proxyHost = proxyHost;
    this.proxyPort = proxyPort;
  }

  public void setLocalAddress(IPAddr localAddr) {
    this.localAddr = localAddr;
  }

  public void setFetchFlags(BitSet fetchFlags) {
    this.fetchFlags = fetchFlags;
  }

  public BitSet getFetchFlags() {
    return fetchFlags;
  }

  public String getUrl() {
    return origUrl;
  }
  
  public ArchivalUnit getArchivalUnit() {
    return au;
  }
  
  public void setCrawlRateLimiter(CrawlRateLimiter crl) {
    this.crl = crl;
  }
  
  public void setRequestProperty(String key, String value) {
    if (reqProps == null) {
      reqProps = new Properties();
    }
    reqProps.put(key, value);
  }
  
  public void setPreviousContentType(String previousContentType) {
    this.previousContentType = previousContentType;
  }
  
  public final InputStream getUncachedInputStream()
      throws IOException {
    String lastModified = null;
    if (!forceRefetch()) {
      lastModified = getLastModified();
    }
    return getUncachedInputStream(lastModified);
  }
  /**
   * Gets an InputStream for this URL, using the last modified time as
   * 'if-modified-since'.  If a 304 is generated (not modified), it returns
   * null.
   * @return the InputStream, or null
   * @throws IOException
   */
  protected final InputStream getUncachedInputStream(String lastModified)
      throws IOException {
    InputStream input = getUncachedInputStreamOnly(lastModified);
    CIProperties headers = getUncachedProperties();
    if (headers.get(SET_COOKIE_HEADER) != null) {
      if (au.shouldRefetchOnCookies()) {
        log.debug3("Found set-cookie header, refetching");
        IOUtil.safeClose(input);
        input = null; // ensure don't reclose in finally if next line throws
        releaseConnection();
        input = getUncachedInputStreamOnly(lastModified);
        if (input == null) {
          log.warning("Got null input stream on second call to "
			 + "getUncachedInputStream");
        }
        headers = getUncachedProperties();
      }
    }
    if (input != null) {
      // Check for login page if got new content
      input = checkLoginPage(input, headers, lastModified);
    }
    return input;
  }
  
  protected InputStream getUncachedInputStreamOnly(String lastModified)
      throws IOException {
    InputStream input = null;
    try {
      openConnection(lastModified);
      if (conn.isHttp()) {
        // http connection; check response code
        int code = conn.getResponseCode();
        if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
          log.debug2("Unmodified content not cached for url '" +
			origUrl + "'");
          return null;
        }
      }
      input = conn.getResponseInputStream();
      if (input == null) {
        log.warning("Got null input stream back from conn.getResponseInputStream");
      }
      if (!input.markSupported()) {
        input = new BufferedInputStream(input);
      }
    } finally {
      if (conn != null && input == null) {
        log.debug3("Releasing connection");
        IOUtil.safeRelease(conn);
      }
    }
    return input;
  }
  
  /**
   * If we haven't already connected, creates a connection from url, setting
   * the user-agent and ifmodifiedsince values.  Then actually connects to the
   * site and throws if we get an error code
   */
  protected void openConnection(String lastModified) throws IOException {
    if (conn==null) {
      if (redirectScheme.isRedirectOption(RedirectScheme.REDIRECT_OPTION_IF_CRAWL_SPEC +
					  RedirectScheme.REDIRECT_OPTION_ON_HOST_ONLY)) {
        openWithRedirects(lastModified);
      } else {
        openOneConnection(lastModified);
      }
    }
  }
  
  protected void openWithRedirects(String lastModified) throws IOException {
    int retry = 0;
    while (true) {
      try {
        openOneConnection(lastModified);
        break;
      } catch (CacheException.NoRetryNewUrlException e) {
        if (++retry >= MAX_REDIRECTS) {
          log.warning("Max redirects hit, not redirecting " + origUrl +
			 " past " + fetchUrl);
          throw e;
        } else if (!processRedirectResponse()) {
          throw e;
        }
      }
    }
  }
  
  /**
   * Create a connection object from url, set the user-agent and
   * ifmodifiedsince values.  Then actually connect to the site and throw
   * if we get an error response
   */
  protected void openOneConnection(String lastModified) throws IOException {
    if (conn != null) {
      throw new IllegalStateException("Must call reset() before reusing UrlCacher");
    }
    try {
      conn = makeConnection(getRequestUrl(), connectionPool);
      if (proxyHost != null) {
        if (log.isDebug3()) log.debug3("Proxying through " + proxyHost
					     + ":" + proxyPort);
        conn.setProxy(proxyHost, proxyPort);
	if (CurrentConfig.getBooleanParam(PARAM_PROXY_BY_AUID,
					  DEFAULT_PROXY_BY_AUID)) {
          conn.setRequestProperty(Constants.X_LOCKSS_AUID, au.getAuId());
	}
      }
      if (localAddr != null) {
        conn.setLocalAddress(localAddr);
      }
      if (CurrentConfig.getBooleanParam(PARAM_SO_KEEPALIVE,
					DEFAULT_SO_KEEPALIVE)) {
	conn.setKeepAlive(true);
      }
      for (String cookie : au.getHttpCookies()) {
        int pos = cookie.indexOf("=");
        if (pos > 0) {
          conn.addCookie(cookie.substring(0, pos), cookie.substring(pos + 1));
        } else {
          log.error("Illegal cookie: " + cookie);
        }
      }
      String userPass = getUserPass();
      if (!StringUtil.isNullString(userPass)) {
	try {
	  List<String> lst =
	    (List<String>)(AuParamType.UserPasswd.parse(userPass));
	  if (lst.size() == 2) {
	    conn.setCredentials(lst.get(0), lst.get(1));
	  }
	} catch (AuParamType.InvalidFormatException e) {
	  log.warning("Invalid user:pass for AU, not used: " +  au);
	}
      }
      // Add global request headers first so plugin can override
      addRequestHeaders();
      addPluginRequestHeaders();
      if (reqProps != null) {
        for (Iterator iter = reqProps.keySet().iterator(); iter.hasNext(); ) {
          String key = (String)iter.next();
          conn.setRequestProperty(key, reqProps.getProperty(key));
        }
      }
      conn.setFollowRedirects(redirectScheme.isRedirectOption(RedirectScheme.REDIRECT_OPTION_FOLLOW_AUTO));
      conn.setRequestProperty("user-agent", LockssDaemon.getUserAgent());

      if (lastModified != null) {
        conn.setIfModifiedSince(lastModified);
      }
      pauseBeforeFetch();
      customizeConnection(conn);
      executeConnection(conn);
    } catch (IOException ex) {
      log.debug2("openConnection", ex);
      throw resultMap.mapException(au, conn, ex, null);
    } catch (RuntimeException e) {
      log.warning("openConnection: unexpected exception", e);
      throw e;
    }
    checkConnectException(conn);
  }
  
  /** Override to change connection settings */
  protected void customizeConnection(LockssUrlConnection conn)
      throws IOException {
  }

  /** Override to modify/wrap conn.execute() */
  protected void executeConnection(LockssUrlConnection conn)
      throws IOException {
    conn.execute();
  }

  protected InputStream checkLoginPage(InputStream input, CIProperties headers,
				       String lastModified)
      throws IOException {
    LoginPageChecker checker = au.getLoginPageChecker();
    if (checker != null) {
      log.debug3("Found a login page checker");
      if (!input.markSupported()) {
        input = new BufferedInputStream(input);
      }
      input.mark(CurrentConfig.getIntParam(PARAM_LOGIN_CHECKER_MARK_LIMIT,
					   DEFAULT_LOGIN_CHECKER_MARK_LIMIT));
      String contentEncoding =
	headers.getProperty(CachedUrl.PROPERTY_CONTENT_ENCODING);
      InputStream uncIn =
	StreamUtil.getUncompressedInputStreamOrFallback(input,
							contentEncoding,
							fetchUrl);
      if (uncIn != input) {
	// Stream no longer has an encoding, and we don't know its length
	// (not that the login page checker is likely to care).
	// Don't modify the Properties that were passed in
	headers = CIProperties.fromProperties(headers);
	headers.remove(CachedUrl.PROPERTY_CONTENT_ENCODING);
	headers.remove("Content-Length");
      }

      String charset = AuUtil.getCharsetOrDefault(uncachedProperties);
      Reader reader = CharsetUtil.getReader(uncIn, charset);
      try {
        if (checker.isLoginPage(headers, reader)) {
          throw new CacheException.PermissionException("Found a login page");
        } else {
          input = resetInputStream(input, lastModified);
        }
      } catch (PluginException e) {
        //XXX: this should be changed so that plugin exception perpetuates
        throw new RuntimeException(e);
      }	
    } else {
      log.debug3("Didn't find a login page checker");
    }
    return input;
  }
  
  /**
   * Try to reset the provided input stream, if we can't then return
   * new input stream for the given url
   */
  public InputStream resetInputStream(InputStream is, 
				      String lastModified) throws IOException {
    try {
      if (wdog != null) {
        wdog.pokeWDog();
      }
      is.reset();
    } catch (IOException e) {
      log.debug("Couldn't reset input stream, so getting new one", e);
      is.close();
      releaseConnection();
      is = new BufferedInputStream(getUncachedInputStreamOnly(lastModified));
    }
    return is;
  }

  protected LockssUrlConnection makeConnection(String url,
					       LockssUrlConnectionPool pool)
      throws IOException {
    LockssUrlConnection res = makeConnection0(url, pool);
    String cookiePolicy = au.getCookiePolicy();
    if (cookiePolicy != null) {
      res.setCookiePolicy(cookiePolicy);
    }
    return res;
  }

  /** Overridable so testing code can return a MockLockssUrlConnection */
  protected LockssUrlConnection makeConnection0(String url,
						LockssUrlConnectionPool pool)
      throws IOException {
    return UrlUtil.openConnection(getMethod(), url, pool);
  }

  protected int getMethod() {
    return LockssUrlConnection.METHOD_GET;
  }

  /** Return the URL to use in the HTTP request, normally fetchUrl */
  protected String getRequestUrl() throws IOException {
    return fetchUrl;
  }

  protected String getUserPass() {
    Configuration auConfig = au.getConfiguration();
    if (auConfig != null) {		// can be null in unit tests
      String val = auConfig.get(ConfigParamDescr.USER_CREDENTIALS.getKey());
      val = CurrentConfig.getIndirect(val, null);
      return val;
    }
    return null;
  }

  protected void pauseBeforeFetch() {
    if (crl != null) {
      long wDogInterval = 0;
      if (wdog != null &&
	  CurrentConfig.getBooleanParam(PARAM_STOP_WATCHDOG_DURING_PAUSE,
					DEFAULT_STOP_WATCHDOG_DURING_PAUSE)) {
	wDogInterval = wdog.getWDogInterval();
      }
      try {
	if (wDogInterval > 0) {
	  wdog.stopWDog();
	}
	crl.pauseBeforeFetch(fetchUrl, previousContentType);
      } finally {
	if (wDogInterval > 0) {
	  wdog.startWDog(wDogInterval);
	}
      }
    }
  }
  
  protected void checkConnectException(LockssUrlConnection conn) throws IOException {
    if (conn.isHttp()) {
      if (log.isDebug3()) {
        log.debug3("Response: " + conn.getResponseCode() + ": " +
		      conn.getResponseMessage());
      }
      CacheException c_ex = resultMap.checkResult(au, conn);
      if (c_ex != null) {
        // The stack below here is misleading.  Makes more sense for it
        // to reflect the point at which it's thrown
        c_ex.fillInStackTrace();
        throw c_ex;
      }
    }
  }
  
  protected void addRequestHeaders() {
    List<String> hdrs =
      CurrentConfig.getList(CrawlManagerImpl.PARAM_REQUEST_HEADERS,
			    CrawlManagerImpl.DEFAULT_REQUEST_HEADERS);
    if (hdrs != null) {
      for (String hdr : hdrs) {
	addHdr(hdr);
      }
    }
  }
  
  protected void addPluginRequestHeaders() {
    for (String hdr : au.getHttpRequestHeaders()) {
      addHdr(hdr);
    }
  }
  
  /** Add request header if string is <tt>key:val</tt>, remove if juat
   * <tt>key:</tt> */
  private void addHdr(String hdr) {
    int pos = hdr.indexOf(":");
    if (pos > 0) {
      String key = hdr.substring(0, pos);
      if (pos < hdr.length() - 1) {
	setRequestProperty(key, hdr.substring(pos + 1));
      } else if (reqProps != null) {
	reqProps.remove(key);
      }
    }
  }


  /** Handle a single redirect response: determine whether it should be
   * followed and change the state (fetchUrl) to set up for the next fetch.
   * @return true if another request should be issued, false if not. */
  protected boolean processRedirectResponse() throws CacheException {
    //get the location header to find out where to redirect to
    String location = conn.getResponseHeaderValue("location");
    if (location == null) {
      // got a redirect response, but no location header
      log.siteError("Received redirect response " + conn.getResponseCode()
		       + " but no location header");
      return false;
    }
    if (log.isDebug3()) {
      log.debug3("Redirect requested from '" + fetchUrl +
		    "' to '" + location + "'");
    }
    // update the current location with the redirect location.
    try {
      String resolvedLocation = UrlUtil.resolveUri(fetchUrl, location);
      String newUrlString = resolvedLocation;
      if (CurrentConfig.getBooleanParam(PARAM_NORMALIZE_REDIRECT_URL,
                                        DEFAULT_NORMALIZE_REDIRECT_URL)) {
        try {
          newUrlString = UrlUtil.normalizeUrl(resolvedLocation, au);
          log.debug3("Normalized to '" + newUrlString + "'");
          if (isHttpToHttpsRedirect(fetchUrl, resolvedLocation, newUrlString)) {
            log.debug3("HTTP to HTTPS redirect normalized back to HTTP; keeping '"
                       + resolvedLocation + "'");
            newUrlString = resolvedLocation;
          }
        } catch (PluginBehaviorException e) {
          log.warning("Couldn't normalize redirect URL: " + newUrlString, e);
        }
      }
      // Check redirect to login page *before* crawl spec, else plugins
      // would have to include login page URLs in crawl spec
      checkRedirectAction(newUrlString);
      if (redirectScheme.isRedirectOption(RedirectScheme.REDIRECT_OPTION_IF_CRAWL_SPEC)) {
        if (!au.shouldBeCached(newUrlString)) {
          String msg = "Redirected to excluded URL: " + newUrlString;
          log.warning(msg + " redirected from: " + origUrl);
          throw new CacheException.RedirectOutsideCrawlSpecException(msg);
        }
      }

      if (!UrlUtil.isSameHost(fetchUrl, newUrlString)) {
        if (redirectScheme.isRedirectOption(RedirectScheme.REDIRECT_OPTION_ON_HOST_ONLY)) {
          log.warning("Redirect to different host: " + newUrlString +
			 " from: " + origUrl);
          return false;
        } else if (!crawlFacade.hasPermission(newUrlString)) {
          log.warning("No permission for redirect to different host: "
                         + newUrlString + " from: " + origUrl);
          return false;
        }
      }
      releaseConnection();

      // XXX
      // The names .../foo and .../foo/ map to the same repository node, so
      // the case of a slash-appending redirect requires special handling.
      // (Still. sigh.)  The node should be written only once, so don't add
      // another entry for the slash redirection.

      if (!UrlUtil.isDirectoryRedirection(fetchUrl, newUrlString)) {
        if (redirectUrls == null) {
          redirectUrls = new ArrayList();
        }
        redirectUrls.add(newUrlString);
      }
      fetchUrl = newUrlString;
      log.debug2("Following redirect to " + newUrlString);
      return true;
    } catch (MalformedURLException e) {
      log.siteWarning("Redirected location '" + location +
			 "' is malformed", e);
      return false;
    }
  }
  
  protected void checkRedirectAction(String url) throws CacheException {
    CacheException ex =
      crawlFacade.getAuCacheResultMap().mapUrl(au, conn, origUrl, url,
                                               "Redirect from " + origUrl);
    log.critical("checkRedirectAction: " + url + ": " + ex);
    if (ex != null) {
      throw ex;
    }
  }

  public CIProperties getUncachedProperties()
      throws UnsupportedOperationException {
    if (conn == null) {
      throw new UnsupportedOperationException("Called getUncachedProperties "
					      + "before calling getUncachedInputStream.");
    }
    if (uncachedProperties == null) {
      CIProperties props = new CIProperties();
      // set header properties in which we have interest
      String ctype = conn.getResponseContentType();
      if (ctype != null) {
	props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, ctype);
      }
      props.setProperty(CachedUrl.PROPERTY_FETCH_TIME,
			Long.toString(TimeBase.nowMs()));
      if (origUrl != fetchUrl &&
	  !UrlUtil.isDirectoryRedirection(origUrl, fetchUrl)) {
	// XXX this property does not have consistent semantics.  It will be
	// set to the first url in a chain of redirects that led to content,
	// which could be different depending on fetch order.
	props.setProperty(CachedUrl.PROPERTY_ORIG_URL, origUrl);
      }
      if (reqProps != null) {
	String referrer = reqProps.getProperty(Constants.HTTP_REFERER);
	if (CurrentConfig.getBooleanParam(PARAM_RECORD_REFERRER,
					  DEFAULT_RECORD_REFERRER) &&
	    !StringUtil.isNullString(referrer)) {
	  props.setProperty(CachedUrl.PROPERTY_REQ_REFERRER,
			    referrer);
	}
      }
      conn.storeResponseHeaderInto(props, CachedUrl.HEADER_PREFIX);
      String actualURL = conn.getActualUrl();
      if (!origUrl.equals(actualURL)) {
	props.setProperty(CachedUrl.PROPERTY_CONTENT_URL, actualURL);
      }
      if (redirectUrls != null && !redirectUrls.isEmpty()) {
	props.setProperty(CachedUrl.PROPERTY_REDIRECTED_TO,
			  redirectUrls.get(0));
      } else if (!origUrl.equals(actualURL)) {
	props.setProperty(CachedUrl.PROPERTY_REDIRECTED_TO, actualURL);
      }
      uncachedProperties = props;
    }
    return uncachedProperties;
  }
  
  protected void releaseConnection() {
    if (conn != null) {
      log.debug3("conn isn't null, releasing");
      conn.release();
      conn = null;
    }
  }
  
  /**
   * Reset the UrlFetcher to its pre-opened state, so that it can be
   * reopened.
   */
  public void reset() {
    releaseConnection();
    fetchUrl = origUrl;
    redirectUrls = null;
    uncachedProperties = null;
  }

  @Override
  public void setWatchdog(LockssWatchdog wdog) {
    this.wdog = wdog;
  }
  
  public LockssWatchdog getWatchdog() {
    return wdog;
  }
  
  /**
   * <p>
   * Determines if the triple of a fetch URL, its redirect URL, and the
   * normalized redirect URL is an HTTP-to-HTTPS redirect that is then
   * normalized back to the HTTP URL. In {@link BaseUrlFetcher}, this is always
   * false; in {@link HttpToHttpsUrlFetcher}, it is a customizable action.
   * </p>
   * 
   * @param fetched
   *          The fetch URL
   * @param redirect
   *          The redirect URL (the URL the fetch redirected to)
   * @param normalized
   *          The normalized redirect URL
   * @return True if and only if the given triple represents an HTTP-HTTPS-HTTP
   *         loop (always false in {@link BaseUrlFetcher})
   * @since 1.70
   * @see HttpToHttpsUrlFetcher#isHttpToHttpsRedirect(String, String, String)
   */
  protected boolean isHttpToHttpsRedirect(String fetched,
                                          String redirect,
                                          String normalized) {
    return false;
  }
 
}
