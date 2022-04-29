/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.jasper;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.AuParamType;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.BaseUrlFetcher;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

import com.fasterxml.jackson.databind.*;

public class JasperUrlFetcher extends BaseUrlFetcher {
  private static final Logger log = Logger.getLogger(JasperUrlFetcher.class);

  /** Auth string returned from POST */
  public static final String FACADE_KEY_AUTHORIZATION_STRING =
    "authorizationString";
  /** Signal to JasperUrlFetcherFactory to make a JasperUrlPoster
   * UrlFetcher */
  public static String FACADE_KEY_MAKE_POSTER = "makePostFetcher";

  public JasperUrlFetcher(CrawlerFacade crawlerFacade, String url) {
    super(crawlerFacade, url);
  }
  
  @Override
  protected void addRequestHeaders() {
    super.addRequestHeaders();
    setRequestProperty("Authorization", getAuthorizationString());
  }

  @Override
  public FetchResult fetch() throws CacheException {
    establishSessionIfNeeded();
    return super.fetch();
  }

  protected String getAuthorizationString() {
    return (String)crawlFacade.getStateObj(FACADE_KEY_AUTHORIZATION_STRING);
  }

  protected void establishSessionIfNeeded() throws CacheException {
    log.debug3("Check if a session needs to be established");
    if (getAuthorizationString() != null) {
      return;
    }
    log.debug2("Attempting to establish a session");

    String postUrl = String.format("%sservices/xauthn/?op=login",
                                   au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()));
    crawlFacade.putStateObj(FACADE_KEY_MAKE_POSTER, "true");
    UrlFetcher uf;
    try {
      uf = crawlFacade.makeUrlFetcher(postUrl);
    } finally {
      crawlFacade.putStateObj(FACADE_KEY_MAKE_POSTER, null);
    }
    uf.setRedirectScheme(UrlFetcher.REDIRECT_SCHEME_DONT_FOLLOW);

    log.debug2("Ready to make the POST request to establish the session");
    FetchResult fr = uf.fetch();
    log.debug2(String.format("The fetch result from the POST request was: %s", fr));
    if (fr != FetchResult.FETCHED) {
      throw new CacheException.PermissionException("Authorizing POST failed");
    }
  }

  /** UrlFetcher used to make authorization POST request */
  static class JasperUrlPoster extends BaseUrlFetcher {
    public JasperUrlPoster(CrawlerFacade crawlerFacade, String url) {
      super(crawlerFacade, url);
    }

    @Override
    protected int getMethod() {
      return LockssUrlConnection.METHOD_POST;
    }

    @Override
    protected void customizeConnection(LockssUrlConnection conn)
        throws IOException {
      super.customizeConnection(conn);
      try {
        String userPassStr =
          au.getConfiguration().get(ConfigParamDescr.USER_CREDENTIALS.getKey());
        List<String> userPass =
          (List<String>)AuParamType.UserPasswd.parse(userPassStr);
        conn.setRequestProperty("content-type", Constants.FORM_ENCODING_URL);
        conn.setRequestEntity(String.format("email=%s&password=%s",
                                            UrlUtil.encodeUrl(userPass.get(0)),
                                            UrlUtil.encodeUrl(userPass.get(1))));
      } catch (AuParamType.InvalidFormatException e) {
        CacheException ce =
          new CacheException.PermissionException("Malformed credentials");
        ce.initCause(e);
        throw ce;
      }
    }

    @Override
    protected void consume(FetchedUrlData fud) throws IOException {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode json = objectMapper.readTree(fud.getInputStream());
      String authStr = String.format("LOW %s:%s",
                                     json.get("values").get("s3").get("access").asText(),
                                     json.get("values").get("s3").get("secret").asText());
      log.debug3(String.format("Resulting authorization string: %s", authStr));
      crawlFacade.putStateObj(FACADE_KEY_AUTHORIZATION_STRING, authStr);
    }

  }

}
