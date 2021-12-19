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

package org.lockss.plugin.highwire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * @since 1.68.0 with storeAtOrigUrl()
 */
public class HighWireJCoreSigninUrlConsumerFactory extends HighWireJCoreUrlConsumerFactory {
  private static final Logger log = Logger.getLogger(HighWireJCoreSigninUrlConsumerFactory.class);
  
  //For a signin redirect - it could be multiple hops, eg 
  //   http://www.bloodjournal.org/content/129/23/3111.full.pdf
  //   https://signin.hematology.org/Login.aspx?vi
  //   http://www.bloodjournal.org/content/129/23/3111.full.pdf?sso-checked=true
  //   http://www.bloodjournal.org/content/bloodjournal/129/23/3111.full.pdf?sso-checked=true
  // so allow for more than 2 hops and don't worry about the addition of a jid in the url pattern
  protected static final String SIG_ORIG_STRING = "/content(/.+/[^/]+)$";
  // Same as the original but with optional extra directory just after "content/"
  protected static final String DEST_WITH_JID_STRING = "/content/[^/]+(/.+/[^/]+)(\\?sso-checked=true)$";
  
  protected static final Pattern origPat = Pattern.compile(SIG_ORIG_STRING, Pattern.CASE_INSENSITIVE);
  protected static final Pattern destPat = Pattern.compile(DEST_WITH_JID_STRING, Pattern.CASE_INSENSITIVE);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a UrlConsumer");
    return new HighWireJCoreSigninUrlConsumer(facade, fud);
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
  public class HighWireJCoreSigninUrlConsumer extends HighWireJCorelUrlConsumer {
    
    
    public HighWireJCoreSigninUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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
      if (!should && fud.redirectUrls != null && (fud.redirectUrls.size() > 0)) {
        //the fetched = original or fetched = original with ?sso-checked=true
        if (fud.fetchUrl.equals(fud.origUrl) ||
            fud.fetchUrl.equals(fud.origUrl + "?sso-checked=true") ) {
          should = true;
        } else {
          // fetched = original + journal_id dir after content + ?sso-checked=true
          Matcher destMat = destPat.matcher(fud.fetchUrl);
          Matcher origMat = origPat.matcher(fud.origUrl);
          if (destMat.find() && origMat.find()) {
            should =  origMat.group(1).equals(destMat.group(1));
          }
        }
        log.debug3("SO redirect: " + fud.redirectUrls.size() + " " + fud.origUrl + " to " + fud.fetchUrl + " : " + should);
        if (!should) {
          log.warning("myfud: " + fud.redirectUrls.size() + " " + fud.redirectUrls.toString());
        }
      }
      return should;
    }
    
  }
  
}
