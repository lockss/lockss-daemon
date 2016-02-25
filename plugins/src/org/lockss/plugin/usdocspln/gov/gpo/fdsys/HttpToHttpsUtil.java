/*
 * $Id: $
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.usdocspln.gov.gpo.fdsys;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

import org.lockss.config.CurrentConfig;
import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.CacheException;

/**
 * @deprecated In 1.70, use functionality of HttpToHttpsUrlFetcher,
 *             HttpToHttpsUrlConsumer, BaseUrlHttpHttpsUrlNormalizer, etc.
 */
@Deprecated
public class HttpToHttpsUtil {

  public static final int VERSION = 2; // to help diagnose copy-paste errors
  
  /**
   * @deprecated In 1.70, use the real AuUtil
   */
  @Deprecated
  public static class AuUtil {
    
    public static boolean isParamUrlHttp(ArchivalUnit au, String paramKey) {
      String url = au.getConfiguration().get(paramKey);
      return url != null && UrlUtil.isHttpUrl(url);
    }
    
    public static boolean isParamUrlHttps(ArchivalUnit au, String paramKey) {
      String url = au.getConfiguration().get(paramKey);
      return url != null && UrlUtil.isHttpsUrl(url);
    }
    
    public static boolean isBaseUrlHttp(ArchivalUnit au) {
      return isParamUrlHttp(au, ConfigParamDescr.BASE_URL.getKey());
    }
    
    public static String normalizeHttpHttpsFromParamUrl(ArchivalUnit au,
                                                        String paramKey,
                                                        String url) {
      if (isParamUrlHttp(au, paramKey) && UrlUtil.isHttpsUrl(url)) {
        return UrlUtil.replaceScheme(url, "https", "http");
      }
      if (isParamUrlHttps(au, paramKey) && UrlUtil.isHttpUrl(url)) {
        return UrlUtil.replaceScheme(url, "http", "https");
      }
      return url;
    }
    
    public static String normalizeHttpHttpsFromBaseUrl(ArchivalUnit au,
                                                       String url) {
      return normalizeHttpHttpsFromParamUrl(au, ConfigParamDescr.BASE_URL.getKey(), url);
    }
      
  }
  
  /**
   * @deprecated In 1.70, use the real BaseUrlHttpHttpsUrlNormalizer
   */
  @Deprecated
  public static class BaseUrlHttpHttpsUrlNormalizer implements UrlNormalizer {

    @Override
    public String normalizeUrl(String url,
                               ArchivalUnit au)
        throws PluginException {
      if (UrlUtil.isSameHost(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()),
                             url)) {
        url = AuUtil.normalizeHttpHttpsFromBaseUrl(au, url);
      }
      return additionalNormalization(url, au);
    }
    
    public String additionalNormalization(String url,
                                          ArchivalUnit au)
        throws PluginException {
      return url;
    }

  }

  /**
   * @deprecated In 1.70, use the real HttpToHttpsUrlConsumer
   */
  @Deprecated
  public static class HttpToHttpsUrlConsumer extends SimpleUrlConsumer {

    public HttpToHttpsUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }

    @Override
    public void consume() throws IOException {
      if (shouldStoreAtOrigUrl()) {
        storeAtOrigUrl();
      }
      super.consume();
    }

    public boolean shouldStoreAtOrigUrl() {
      return AuUtil.isBaseUrlHttp(au)
             && fud.redirectUrls != null
             && fud.redirectUrls.size() == 1
             && fud.fetchUrl.equals(fud.redirectUrls.get(0))
             && UrlUtil.isHttpUrl(fud.origUrl)
             && UrlUtil.isHttpsUrl(fud.fetchUrl)
             && UrlUtil.stripProtocol(fud.origUrl).equals(UrlUtil.stripProtocol(fud.fetchUrl));
    }
  
  }
    
  /**
   * @deprecated In 1.70, use the real HttpToHttpsUrlConsumerFactory
   */
  @Deprecated
  public static class HttpToHttpsUrlConsumerFactory implements UrlConsumerFactory {

    @Override
    public UrlConsumer createUrlConsumer(CrawlerFacade crawlFacade,
                                         FetchedUrlData fud) {
      return new HttpToHttpsUrlConsumer(crawlFacade, fud);
    }

  }

  /**
   * @deprecated In 1.70, use the real HttpToHttpsUrlFetcher
   */
  @Deprecated
  public static class HttpToHttpsUrlFetcher extends BaseUrlFetcher {

    private static final Logger log = Logger.getLogger(HttpToHttpsUrlFetcher.class);
    
    public HttpToHttpsUrlFetcher(CrawlerFacade crawlFacade, String url) {
      super(crawlFacade, url);
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
        String resolvedLocation = org.lockss.util.UrlUtil.resolveUri(fetchUrl, location);
        String newUrlString = resolvedLocation;
        if (CurrentConfig.getBooleanParam(PARAM_NORMALIZE_REDIRECT_URL,
                                          DEFAULT_NORMALIZE_REDIRECT_URL)) {
          try {
            newUrlString = org.lockss.util.UrlUtil.normalizeUrl(resolvedLocation, au);
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
        if (au.isLoginPageUrl(newUrlString)) {
          String msg = "Redirected to login page: " + newUrlString;
          throw new CacheException.PermissionException(msg);
        }
        if (redirectScheme.isRedirectOption(RedirectScheme.REDIRECT_OPTION_IF_CRAWL_SPEC)) {
          if (!au.shouldBeCached(newUrlString)) {
            String msg = "Redirected to excluded URL: " + newUrlString;
            log.warning(msg + " redirected from: " + origUrl);
            throw new CacheException.RedirectOutsideCrawlSpecException(msg);
          }
        }

        if (!org.lockss.util.UrlUtil.isSameHost(fetchUrl, newUrlString)) {
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

        if (!org.lockss.util.UrlUtil.isDirectoryRedirection(fetchUrl, newUrlString)) {
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
    
    /**
     * <p>
     * Determines if the triple of a fetch URL, its redirect URL, and the
     * normalized redirect URL is an HTTP-to-HTTPS redirect that is then
     * normalized back to the HTTP URL. In {@link BaseUrlFetcher}, this is always
     * false. In this class ({@link HttpToHttpsUrlFetcher}), this is true if the
     * redirect is otherwise exact and the fetch URL and normalized redirect URL
     * are identical. (If some site slightly alters the redirect URL, for example
     * in case, or if the URL normalizer does, then this method needs to be
     * overridden to allow for more flexibility.)
     * </p>
     * 
     * @param fetched
     *          The fetch URL
     * @param redirect
     *          The redirect URL (the URL the fetch redirected to)
     * @param normalized
     *          The normalized redirect URL
     * @return True if and only if the given triple represents an exact
     *         HTTP-HTTPS-HTTP loop
     * @since 1.70
     * @see HttpToHttpsUrlFetcher#isHttpToHttpsRedirect(String, String, String)
     */
    @Override
    protected boolean isHttpToHttpsRedirect(String fetched,
                                            String redirect,
                                            String normalized) {
      return UrlUtil.isHttpUrl(fetched)
          && UrlUtil.isHttpsUrl(redirect)
          && UrlUtil.isHttpUrl(normalized)
          && UrlUtil.stripProtocol(fetched).equals(UrlUtil.stripProtocol(redirect))
          && fetched.equals(normalized);
    }
    
  }
  
  /**
   * @deprecated In 1.70, use the real HttpToHttpsUrlFetcherFactory
   */
  @Deprecated
  public static class HttpToHttpsUrlFetcherFactory implements UrlFetcherFactory {

    @Override
    public UrlFetcher createUrlFetcher(CrawlerFacade crawlFacade, String url) {
      return new HttpToHttpsUrlFetcher(crawlFacade, url);
    }
    
  }

  /**
   * @deprecated In 1.70, use the real UrlUtil
   */
  @Deprecated
  public static class UrlUtil {
    
    public static String getHost(String urlStr) throws MalformedURLException {
      URL url = new URL(urlStr);
      return url.getHost();
    }

    public static boolean isHttpUrl(String url) {
      return StringUtil.startsWithIgnoreCase(url, "http:");
    }

    public static boolean isHttpsUrl(String url) {
      return StringUtil.startsWithIgnoreCase(url, "https:");
    }

    public static boolean isSameHost(String url1, String url2) {
      try {
        return getHost(url1).equalsIgnoreCase(getHost(url2));
      } catch (MalformedURLException e) {
        return false;
      }
    }
    
    public static String replaceScheme(String url, String from, String to) {
      int flen = from.length();
      if (StringUtil.startsWithIgnoreCase(url, from) &&
          url.length() > flen &&
          url.charAt(flen) == ':') {
        return to + url.substring(flen);
      }
      return url;
    }

    public static String stripProtocol(String url) {
      final String PROTOCOL_SUBSTRING = "://";
      if (url == null) return null;
      int pos = url.indexOf(PROTOCOL_SUBSTRING);
      if (pos >= 0) {
        return url.substring(pos + PROTOCOL_SUBSTRING.length());
      }
      return url;
    }

  }
  
}
