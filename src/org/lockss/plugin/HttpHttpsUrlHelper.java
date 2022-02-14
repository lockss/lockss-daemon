/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin;

import java.net.MalformedURLException;
import java.util.*;

import org.apache.commons.collections4.map.Flat3Map;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.util.UrlUtil;

/**
 * <p>
 * A URL normalizer-like helper for HTTP-to-HTTPS plugin transitions, that
 * accepts a set of target URL-typed plugin parameters, and normalizes incoming
 * URLs from the same host as a target URL to the same protocol (HTTP or HTTPS)
 * as that target URL. 
 * </p>
 * <p>
 * Example:
 * </p>
 * <pre>
au = // an AU with foo_url := http://foo.example.com/
     //        and bar_url := https://bar.example.com/
h = new HttpHttpsUrlHelper(au, "foo_url", "bar_url");
h.makeSame("http://foo.example.com/abc.html")  -> http://foo.example.com/abc.html
h.makeSame("https://foo.example.com/abc.html") -> http://foo.example.com/abc.html
h.makeSame("http://bar.example.com/def.html")  -> https://bar.example.com/def.html
h.makeSame("https://foo.example.com/def.html") -> https://bar.example.com/def.html
h.makeSame("http://www.example.com/xyz.html")  -> http://www.example.com/xyz.html
h.makeSame("https://www.example.com/xyz.html") -> https://www.example.com/xyz.html
</pre>
 * 
 * @author Thib Guicherd-Callin
 * @since 1.75.4
 */
public class HttpHttpsUrlHelper {

  protected ArchivalUnit au;

  protected Map<String, Boolean> httpUrls;
    
  /**
   * <p>
   * Makes a new instance with 'base_url' as the only key.
   * </p>
   * <p>
   * Equivalent to {@code new HttpHttpsUrlHelper(au, "base_url")}.
   * </p>
   * 
   * @param au
   *          An archival unit.
   * @since 1.75.4
   * @see HttpHttpsUrlHelper#HttpHttpsUrlHelper(ArchivalUnit, String...)
   */
  public HttpHttpsUrlHelper(ArchivalUnit au) {
    this(au, ConfigParamDescr.BASE_URL.getKey());
  }
   
  /**
   * <p>
   * Makes an instance with the given plugin parameter keys.
   * </p>
   * 
   * @param au
   *          An archival unit.
   * @since 1.75.4
   */
  public HttpHttpsUrlHelper(ArchivalUnit au, String... params)
      throws IllegalArgumentException {
    this.au = au;
    this.httpUrls = (params.length <= 3) ? new Flat3Map<>() : new HashMap<>();
    for (String param : params) {
      String url = au.getConfiguration().get(param);
      if (url == null) {
        throw new IllegalArgumentException(String.format("AU %s (%s) has no param %s",
                                                         au.getName(),
                                                         au.getAuId(),
                                                         param));
      }
      if (!UrlUtil.isHttpOrHttpsUrl(url)) {
        throw new IllegalArgumentException(String.format("AU %s (%s) param %s is not http:// or https:// URL: %s",
                                                         au.getName(),
                                                         au.getAuId(),
                                                         param,
                                                         url));
      }
      try {
        httpUrls.put(UrlUtil.getHost(url),
                     UrlUtil.isHttpUrl(url));
      }
      catch (MalformedURLException mue) {
        throw new IllegalArgumentException(String.format("AU %s (%s) param %s is a malformed URL: %s",
                                                         au.getName(),
                                                         au.getAuId(),
                                                         param,
                                                         url),
                                           mue);
      }
    }
  }

  /**
   * <p>
   * Normalizes an arbitrary URL against the AU's target URLs.
   * </p>
   * 
   * @param url
   *          An arbitrary URL.
   * @return The URL with the same protocol as one of the AU's target URL (or
   *         the unchanged URL if it is not on the host of the target URLs).
   * @since 1.75.4
   */
  public String normalize(String url) {
    try {
      if (url == null) {
        return url;
      }
      String host = UrlUtil.getHost(url);
      Boolean isSupposedToBeHttp = httpUrls.get(host);
      if (isSupposedToBeHttp == null) {
        return url;
      }
      return (isSupposedToBeHttp ? "http://" : "https://") + UrlUtil.stripProtocol(url);
    }
    catch (MalformedURLException mue) {
      return url;
    }
  }
  
}
