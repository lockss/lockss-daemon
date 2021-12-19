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

package org.lockss.plugin.highwire.nas;

import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.highwire.HighWireJCoreUrlConsumerFactory;
import org.lockss.util.Logger;

import java.util.regex.Pattern;

public class NasDrupalUrlConsumerFactory extends HighWireJCoreUrlConsumerFactory {

  private static final Logger log = Logger.getLogger(NasDrupalUrlConsumerFactory.class);

  // extensions are too many to account for, e.g. avi, pdf, xlsx, mov, mp4, txt, etc...
  protected static final String ORIG_SUPPL_STRING = "/filestream/[^/]+/field_highwire_adjunct_files/";
  protected static final String DEST_SUPPL_STRING = "/content/pnas/suppl/\\d{2,4}/\\d{2}/\\d{2}/[^/]+DCSupplemental/";

  protected static final Pattern origSupplPat = Pattern.compile(ORIG_SUPPL_STRING, Pattern.CASE_INSENSITIVE);
  protected static final Pattern destSupplPat = Pattern.compile(DEST_SUPPL_STRING, Pattern.CASE_INSENSITIVE);

  //x-highwire-filestream-for: 	http://sass.highwire.org/pnas/suppl/2018/12/21/1812570116.DCSupplemental/pnas.1812570116.sm01.avi
  //x-lockss-node-url: 	https://www.pnas.org/content/pnas/suppl/2018/12/21/1812570116.DCSupplemental/pnas.1812570116.sm01.avi
  //x-lockss-orig-url: 	https://www.pnas.org/highwire/filestream/842404/field_highwire_adjunct_files/1/pnas.1812570116.sm01.avi

  @Override
  public UrlConsumer createUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
    log.debug3("Creating a UrlConsumer");
    return new NasDrupalUrlConsumer(facade, fud);
  }

  public class NasDrupalUrlConsumer extends HighWireJCorelUrlConsumer {

    protected Configuration auconfig;

    public NasDrupalUrlConsumer(Crawler.CrawlerFacade facade, FetchedUrlData fud) {
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
            && (destSupplPat.matcher(fud.fetchUrl)).find()
            && origSupplPat.matcher(fud.origUrl).find());
      }
      return should;
    }

  }

}
