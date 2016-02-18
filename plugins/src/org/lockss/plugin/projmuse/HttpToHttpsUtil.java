/*
 * $Id: ProjectMuseArticleIteratorFactory.java 40690 2015-03-18 18:12:56Z thib_gc $
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.projmuse;

import java.io.IOException;
import java.net.*;

import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.StringUtil;

/**
 * @deprecated In 1.70, use functionality of HttpToHttpsUrlConsumer,
 *             BaseUrlHttpHttpsUrlNormalizer, etc.
 */
@Deprecated
public class HttpToHttpsUtil {

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
