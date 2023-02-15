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

package org.lockss.plugin.highwire.bmj;

import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.highwire.HighWireJCoreUrlConsumerFactory;
import org.lockss.util.Logger;

import java.util.regex.Pattern;

public class BMJJCoreUrlConsumerFactory extends HighWireJCoreUrlConsumerFactory {

  private static final Logger log = Logger.getLogger(BMJJCoreUrlConsumerFactory.class);

  protected static final String WWW_BASE_URL = "https?://www\\.bmj\\.com/";
  protected static final String NO_WWW_BASE_URL = "https?://bmj\\.com/";

  protected static final Pattern wwwPat = Pattern.compile(WWW_BASE_URL, Pattern.CASE_INSENSITIVE);
  protected static final Pattern noWwwPat = Pattern.compile(NO_WWW_BASE_URL, Pattern.CASE_INSENSITIVE);

  @Override
  public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a UrlConsumer");
    return new BMJJCoreUrlConsumer(facade, fud);
  }

  public class BMJJCoreUrlConsumer extends HighWireJCorelUrlConsumer {

    protected Configuration auconfig;

    public BMJJCoreUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
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
      if (!should) {
        should = (fud.redirectUrls != null
            && fud.redirectUrls.size() >= 1
            && (
                (wwwPat.matcher(fud.fetchUrl)).find()
                && noWwwPat.matcher(fud.origUrl).find()
            ) || (
                (noWwwPat.matcher(fud.fetchUrl)).find()
                && wwwPat.matcher(fud.origUrl).find()
            )
        );
      }
      log.info("should: " + should );
      log.info(String.valueOf(wwwPat));
      log.info(String.valueOf(noWwwPat));
      log.info(fud.origUrl);
      log.info(fud.fetchUrl);
      return should;
    }

  }

}