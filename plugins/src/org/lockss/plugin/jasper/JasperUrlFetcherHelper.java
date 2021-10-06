/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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

package org.lockss.plugin.jasper;

import java.io.IOException;

import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang3.StringUtils;
import org.lockss.daemon.ConfigParamDescr;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;

import com.fasterxml.jackson.databind.*;

public class JasperUrlFetcherHelper {

  private static final Logger log = Logger.getLogger(JasperUrlFetcherHelper.class);
  
  protected String authorizationString;
  
  protected ArchivalUnit au;

  protected CrawlerFacade crawlerFacade;

  public JasperUrlFetcherHelper(ArchivalUnit au,
                                CrawlerFacade crawlerFacade) {
    this.au = au;
    this.crawlerFacade = crawlerFacade;
    this.authorizationString = null;
    log.debug2(String.format("Creating URL fetcher helper for %s", au));
  }
  
  public String getAuthorizationString() {
    return authorizationString;
  }
  
  public void ensureExecuted(LockssUrlConnectionPool pool) throws IOException {
    if (authorizationString == null) {
      doPost(pool);
    }
  }
  
  /**
   * Cribbed from https://github.com/jjjake/internetarchive/blob/v2.1.0/internetarchive/config.py#L41-L73
   */
  protected void doPost(LockssUrlConnectionPool pool) throws IOException {
    log.debug2(String.format("Doing POST request for %s", au));
    String baseUrl = au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey());
    String userPass = au.getConfiguration().get(ConfigParamDescr.USER_CREDENTIALS.getKey());
    HttpClientUrlConnection conn =
        (HttpClientUrlConnection)UrlUtil.openConnection(LockssUrlConnection.METHOD_POST,
                                                        String.format("%sservices/xauthn/?op=login", baseUrl),
                                                        pool);
    // FIXME redirect policy?
    conn.setRequestProperty("content-type", Constants.FORM_ENCODING_URL);
    conn.setRequestEntity(new StringRequestEntity(String.format("email=%s&password=%s",
                                                  UrlUtil.encodeUrl(StringUtils.substringBefore(userPass, ':')),
                                                  UrlUtil.encodeUrl(StringUtils.substringAfter(userPass, ':')))));
    try {
      conn.execute();
    }
    catch (IOException ioe) {
      log.debug("POST request did not succeed", ioe);
      throw ioe;
    }
    if (conn.getResponseCode() != 200) {
      String str = String.format("Response to POST request was %d %s",
                                 conn.getResponseCode(),
                                 conn.getResponseMessage());
      log.debug2(str);
      throw new IOException(str);
    }
    if (!"application/json".equalsIgnoreCase(conn.getResponseContentType())) {
      String str = String.format("Response to POST request had unexpected content type %s",
                                 conn.getResponseContentType());
      log.debug2(str);
      throw new IOException(str);
    }
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode json = objectMapper.readTree(conn.getUncompressedResponseInputStream());
    authorizationString = String.format("LOW %s:%s",
                                        json.get("values").get("s3").get("access").asText(),
                                        json.get("values").get("s3").get("secret").asText());
    log.debug3(String.format("Resulting authorization string: %s", authorizationString));
  }
  
}
