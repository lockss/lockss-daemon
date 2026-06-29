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
package org.lockss.plugin.ojs3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ContentValidationException;
import org.lockss.plugin.ContentValidationException.WrongLength;
import org.lockss.util.Logger;
import org.lockss.util.urlconn.CacheException;
import org.lockss.util.urlconn.CacheResultHandler;
import org.lockss.util.urlconn.CacheResultMap;
import org.lockss.util.urlconn.CacheException.WarningOnly;


public class Ojs3HttpResponseHandler implements CacheResultHandler {
  private static final Logger logger = Logger.getLogger(Ojs3HttpResponseHandler.class);

  //Some Wasit Journal of Engineering Sciences AUs have file content-length header errors,
  //so we are downgrading these to a warning. 
  protected static final Pattern WASIT_PAT = Pattern.compile("ejuow[.]uowasit[.]edu[.]iq");

  @Override
  public void init(CacheResultMap crmap) {
    logger.warning("Unexpected call to init()");
    throw new UnsupportedOperationException("Unexpected call to PeerJHttpResponseHandler.init()");
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     int responseCode) {
    logger.debug2(url);
    switch (responseCode) {
      case 401:
        logger.debug3("found 401");
        if (url.contains("user/setLocale/")) {
          logger.debug3("is a set/Locale/ url allowing");
          return new CacheException.WarningOnly("401 Unauthorized (non-fatal)");
        }
        return new CacheException.PermissionException("401 Unauthorized");
      case 500:
        logger.debug3("found 500");
        if(url.contains("citationstylelanguage/get")){
          logger.debug3("URL is citation link. Downgrade error to warning.");
          return new CacheException.WarningOnly("500 Unauthorized (non-fatal)");
        //2025: there are some broken WebFeedGatewayPlugin links in Universidad de Valparaíso - Chile AUs
        }else if(url.contains("gateway/plugin/WebFeedGatewayPlugin")){
          logger.debug3("URL is broken WebFeedGatewayPlugin url. Downgrade error to warning.");
          return new CacheException.WarningOnly("500 Unauthorized (non-fatal)");
        }else{return new CacheException.RetrySameUrlException("500 Internal Server Error");}
      default:
        logger.warning("Unexpected responseCode (" + responseCode + ") in handleResult(): AU " + au.getName() + "; URL " + url);
        throw new UnsupportedOperationException("Unexpected responseCode (" + responseCode + ")");
    }
  }

  @Override
  public CacheException handleResult(ArchivalUnit au,
                                     String url,
                                     Exception ex) {
    if(ex instanceof WrongLength){
      Matcher wasitMat = WASIT_PAT.matcher(url);
      if(wasitMat.find()){
        return new WarningOnly(ex.getMessage());
        //may eventually need to replace with RetryableNoFailException_2_10S(ex) 
      }
      else{
        return new CacheException.RetrySameUrlException("Wrong length but pattern does not match Wasit Journal of Engineering Sciences pattern.");
      }
    }
    logger.warning("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
    throw new UnsupportedOperationException("Unexpected call to handleResult(): AU " + au.getName() + "; URL " + url, ex);
  }

}