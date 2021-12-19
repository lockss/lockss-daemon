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

package org.lockss.plugin.highwire.ash;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.highwire.HighWireJCoreSigninUrlConsumerFactory;
import org.lockss.util.Logger;

/**
 * @since 1.68.0 with storeAtOrigUrl()
 */
public class ASHJCoreUrlConsumerFactory extends HighWireJCoreSigninUrlConsumerFactory {
  private static final Logger log = Logger.getLogger(ASHJCoreUrlConsumerFactory.class);
  //For a search redirect 
  private static final String SEARCH_STR = "/search/volume";
  private static final String CHECKED_STR = "sso-checked=true";
  
  //For a supplement redirect - it could be multiple hops, eg 
  //   http://www.bloodjournal.org/highwire/filestream/318501/field_highwire_adjunct_files/0/blood-2012-10-455055-1.pdf
  //   http://www.bloodjournal.org/highwire/filestream/318501/field_highwire_adjunct_files/0/blood-2012-10-455055-1.full.pdf
  //   https://signin.hematology.org/Login.aspx?vi=9&vt=...&DPLF=Y
  //   http://www.bloodjournal.org/highwire/filestream/318501/field_highwire_adjunct_files/0/blood-2012-10-455055-1.full.pdf?sso-checked=true
  //   http://www.bloodjournal.org/content/bloodjournal/suppl/2012/12/18/blood-2012-10-455055.DC1/blood-2012-10-455055-1.pdf?sso-checked=true
  // so allow for more than 2 hops and don't worry about the addition of a jid in the url pattern
  private static final String ORIG_FILE_STRING = "(/[^/?]+)$";
  // Same as the original but with optional CHECKED_STR
  private static final String DEST_FILE_STRING = "(/[^/?]+)(\\?sso-checked=true)?$";
  
  private static final Pattern origPat = Pattern.compile(ORIG_FILE_STRING, Pattern.CASE_INSENSITIVE);
  private static final Pattern destPat = Pattern.compile(DEST_FILE_STRING, Pattern.CASE_INSENSITIVE);
  
  // for testing
  public static Pattern getOrigPattern() {
    return origPat;
  }
  public static Pattern getDestPattern() {
    return destPat;
  }
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a UrlConsumer");
    return new ASHDrupalUrlConsumer(facade, fud);
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
  public class ASHDrupalUrlConsumer extends HighWireJCoreSigninUrlConsumer {
    
    
    public ASHDrupalUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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
      if (!should && fud != null && fud.redirectUrls != null) {
        if (fud.origUrl.contains(SEARCH_STR) &&
            fud.fetchUrl.contains(SEARCH_STR) && fud.fetchUrl.endsWith(CHECKED_STR)) {
          should = true;
        } else {
          Matcher destMat = destPat.matcher(fud.fetchUrl);
          Matcher origMat = origPat.matcher(fud.origUrl);
          if (destMat.find() && origMat.find()) {
            should =  origMat.group(1).equals(destMat.group(1));
          }
        }
        if (!should) {
          log.warning("SSO redirect: " + should + ": " + fud.redirectUrls.size()  + ": " + fud.origUrl + ": " + fud.redirectUrls.toString());
        }
      }
      return should;
    }
    
  }
  
}
