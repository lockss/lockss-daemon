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

package org.lockss.plugin.highwire.ers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.highwire.HighWireJCoreSigninUrlConsumerFactory;
import org.lockss.util.Logger;

public class ERSScolarisUrlConsumerFactory extends HighWireJCoreSigninUrlConsumerFactory{

    private static final Logger log = Logger.getLogger(ERSScolarisUrlConsumerFactory.class);
    private static final String MANIFEST_STR = "manifest.html";
    private static final String CHECKED_STR = "implicit-login=true";
  
  /**
   *  https://publications.ersnet.org/lockss-manifest/books/handbook_978-1-84984-122-1_manifest.html
   
      https://idp.sams-sigma.com/authorize?client_id=ERS...shibboleth_dest=https%3A//publications.ersnet.org/openid-connect/sams-sigma/login-redirect&shibboleth=true

      https://publications.ersnet.org/openid-connect/sams-sigma?code=5VcjLV&state=%257B%2522token%2522%253A%2522fXibUMzhQPApOS2BS-HsOY7HX5JVIfabnQ4AwbJ71KY%2522%252C%2522destination%2522%253A%2522%255C%252Flockss-manifest%255C%252Fbooks%255C%252Fhandbook_978-1-84984-122-1_manifest.html%2522%257D

      https://publications.ersnet.org/lockss-manifest/books/handbook_978-1-84984-122-1_manifest.html?implicit-login=true%26333 
  */
  private static final String ORIG_FILE_STRING = "(/[^/?]+)$";
  // Same as the original but with optional CHECKED_STR
  private static final String DEST_FILE_STRING = "(/[^/?]+)(\\?implicit-login=true%2[0-9]+)?$";

private static final Pattern origPat = Pattern.compile(ORIG_FILE_STRING, Pattern.CASE_INSENSITIVE);
  private static final Pattern destPat = Pattern.compile(DEST_FILE_STRING, Pattern.CASE_INSENSITIVE);
    
  public static Pattern getOrigPattern() {
    return origPat;
  }
  public static Pattern getDestPattern() {
    return destPat;
  }
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a UrlConsumer");
    return new ERSScolarisUrlConsumer(facade, fud);
  }
  
  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain 
   * 
   * </p>
   * 
   * @since 1.68.0
   */
  public class ERSScolarisUrlConsumer extends HighWireJCoreSigninUrlConsumer {
    
    public ERSScolarisUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
    }
    
    /**
     * <p>
     * Determines if a particular redirect chain should cause content to be stored
     * only at the origin URL ({@link FetchedUrlData#origUrl}).
     * </p>
     * 
     * @return True if and only if the fetched URL data represents a particular
     *         redirect chain that should cause content to be stored only at the
     *         origin URL.
     */
    @Override
    public boolean shouldStoreAtOrigUrl() {
      boolean should = super.shouldStoreAtOrigUrl();
      log.info("in url consumer factory, should boolean = " + should);
      log.info("original url = " + fud.origUrl);
      log.info("fetch url = " + fud.fetchUrl);
      if (!should && fud != null && fud.redirectUrls != null) {
        if (fud.origUrl.contains(MANIFEST_STR) &&
            fud.fetchUrl.contains(MANIFEST_STR) && fud.fetchUrl.endsWith(CHECKED_STR)) {
          should = true;
        } else {
          Matcher destMat = destPat.matcher(fud.fetchUrl);
          Matcher origMat = origPat.matcher(fud.origUrl);
          if (destMat.find() && origMat.find()) {
            should =  origMat.group(1).equals(destMat.group(1));
          }
        }
        log.info("after check, url consumer factory, should boolean = " + should);
        if (!should) {
          log.info("Authorization redirect: " + should + ": " + fud.redirectUrls.size()  + ": " + fud.origUrl + ": " + fud.redirectUrls.toString());
        }
      }
      return should;
    }
    
  }
    
}
