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

import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;
import org.lockss.util.SetUtil;

/**
 * @since 1.68.0 with storeAtOrigUrl()
 */
public class HighWireJCoreUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(HighWireJCoreUrlConsumerFactory.class);
  
  // Looking for 2 patterns in the original URL, one for PDFs and the other for TOCs
  // https://www.jrheum.org/content/46/11/E011.full.pdf" -> "https://www.jrheum.org/content/jrheum/46/11/E011.full.pdf",
  // https://www.ghspjournal.org/content/7/4.toc" -> "https://www.ghspjournal.org/content/7/4"
  protected static final String ORIG_STRING = "/content(/[^/]+)(/[^/]+/[^/]+[.]full[.]pdf|/.+[.]toc)$";
  // Should match either PDF URL that has an added sub-directory after /content/ or a TOC URL without the .toc extension
  protected static final String DEST_STRING = "/content(/[^/]+)((/[^/]+)/[^/]+/[^/]+[.]full[.]pdf|/[^/.]+)$";

  protected static final Pattern origPat = Pattern.compile(ORIG_STRING, Pattern.CASE_INSENSITIVE);
  protected static final Pattern destPat = Pattern.compile(DEST_STRING, Pattern.CASE_INSENSITIVE);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a UrlConsumer");
    return new HighWireJCorelUrlConsumer(facade, fud);
  }
  
  public static Pattern getOrigPattern() {
    return origPat;
  }
  
  public static Pattern getDestPattern() {
    return destPat;
  }
  
  /**
   * <p>
   * A custom URL consumer that identifies specific redirect chains and stores the
   * content at the origin of the chain 
   * 
   * The article PDFs are redirected to a url with 
   * So while the link will look like this:
   *   http://www.bmj.com/content/349/bmj.g7460.full.pdf
   * it will end up here:
   *   http://www.bmj.com/content/bmj/349/bmj.g7460.full.pdf
   * 
   * Another example is
   *   http://www.bmj.com/content/350/7989.toc
   * redirects to
   *   http://www.bmj.com/content/350/7989
   * </p>
   * 
   * @since 1.68.0
   */
  public class HighWireJCorelUrlConsumer extends HttpToHttpsUrlConsumer {
    
    protected Configuration auconfig;
    
    public HighWireJCorelUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
      super(facade, fud);
      auconfig = au.getConfiguration();
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
      log.info("checking if super.shouldStore is true. for origUrl: " + fud.origUrl + "  fetchUrl: " + fud.fetchUrl);
      if (!should) {
        Matcher destMat = null;
        Matcher destMatPdf = null;
        should = (fud.redirectUrls != null
            && fud.redirectUrls.size() >= 1
        );
        log.info("super.should store is not true, checking if the redirect occurred");
        if (should) {
          log.info("  should store is now true, meaning, the redirect occurred");
          if ((destMat = destPat.matcher(fud.fetchUrl)).find()
              && origPat.matcher(fud.origUrl).find()) {
            log.info("  the first pattern matched (the one that always matches? ");
            if (fud.origUrl.endsWith(".toc")) {
              log.info("     it was a .toc ");
              // if the origUrl is a TOC, check that the first group of the dest {/content/(<vol>)(/<iss>)} matches the AU volume
              should = destMat.group(1).equals(auconfig.get("volume_name"));
            } else if (fud.origUrl.endsWith(".pdf")) {
              log.info("     it was a .pdf ");
              // if the origUrl is a PDF, check that the second group of the dest {/content/(sub-dir)(/<vol>/<iss>/<article_id>} ends with .pdf
              should = destMat.group(2).endsWith(".pdf");
              log.info("     and the group2 matched? : " + should);
            } else {
              log.warning("Huh! Should not happen: " + fud.fetchUrl + " " + fud.origUrl);
            }
          }
        }
      }
      return should;
    }
  }
  
}
