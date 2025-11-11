/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.ubiquitypress.upn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;

public class UbiquityPartnerNetworkHttpResponseHandler implements CacheResultHandler {
    
  private static final Logger logger = Logger.getLogger(UbiquityPartnerNetworkHttpResponseHandler.class);

  protected static final Pattern ISSUE_PAT = 
      Pattern.compile("/volume/[0-9]+/issue/[0-9]+");

  /*
    2025: There have been a lot of files that we don't have access to on the html versions of articles. 
    Example html article page: https://jotsjournal.org/articles/52/files/63858eb401680.html
    Example broken media link files: https://jotsjournal.org/articles/52/files/aba1.jpg
    UPN said this was expected so we will downgrade these 403s to warnings so that crawl can complete. 
   */ 
  protected static final Pattern MEDIA_PAT = 
      Pattern.compile("/articles/[0-9]+/files/.*\\.(css|jpg|png|gif|webmanifest)");
  /* 
    (continued)
   * There are also a lot of bad citation links in these html pages. For example: https://jotsjournal.org/articles/30/files/wright
   * leads to a 403 from page https://jotsjournal.org/articles/30/files/638465267c2b9.html. 
   * AUID: org|lockss|plugin|ubiquitypress|upn|UbiquityPartnerNetworkPlugin&base_url~https%3A%2F%2Fjotsjournal%2Eorg%2F&year~2016 
   * Let's downgrade to a warning these links that likely end with a name. 
   */
  protected static final Pattern AUTHOR_PAT = 
      Pattern.compile("/articles/[0-9]+/files/[a-zA-Z]+$");

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to UbiquityPartnerNetworkHttpResponseHandler.init()");
   }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);  
    Matcher issueMat = ISSUE_PAT.matcher(url);  
    Matcher mediaMat = MEDIA_PAT.matcher(url);  
    Matcher authorMat = AUTHOR_PAT.matcher(url);      
    switch (responseCode) {
      case 404: 
        logger.debug3("found 404");
        if (issueMat.find()) {
          logger.debug3("found issue page");
          return new CacheException.RetryableNetworkException_5_10S();
        }
        return new CacheException.NoRetryDeadLinkException("404 Not Found");
      case 403: 
        logger.debug3("found 403");
        if (url.contains("files/article.xsl") || url.contains("article.css") || mediaMat.find() || authorMat.find()) {
          logger.debug3("Bad media file or forbidden access, downgrading to warning.");
          return new CacheException.NoRetryDeadLinkException("403 Forbidden (non-fatal)");
        }
        return new CacheException.RetrySameUrlException("403 Forbidden error");
      default:
        logger.debug2("default");
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
  }
}
