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

package org.lockss.plugin.atypon.seg;

import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SEGUrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(SEGUrlConsumerFactory.class);
  
  // Looking for 2 patterns in the original URL, one for PDFs and the other for TOCs
  //https://library.seg.org/doi/epdf/10.1190/1.9781560803713
  //https://library.seg.org/doi/pdf/10.1190/1.9781560803713
  protected static final String ORIG_STRING = "/epdf/(.*)$";
  // Should match either PDF URL that has an added sub-directory after /content/ or a TOC URL without the .toc extension
  protected static final String DEST_STRING = "/pdf/(.*)$";
  
  protected static final Pattern origPat = Pattern.compile(ORIG_STRING, Pattern.CASE_INSENSITIVE);
  protected static final Pattern destPat = Pattern.compile(DEST_STRING, Pattern.CASE_INSENSITIVE);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    return new SEGUrlConsumer(facade, fud);
  }
  
  public static Pattern getOrigPattern() {
    return origPat;
  }
  
  public static Pattern getDestPattern() {
    return destPat;
  }

  public class SEGUrlConsumer extends HttpToHttpsUrlConsumer {
    
    protected Configuration auconfig;
    
    public SEGUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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
        Matcher destMat = null;
        should = (fud.redirectUrls != null
            && fud.redirectUrls.size() >= 1
            && (destMat = destPat.matcher(fud.fetchUrl)).find()
            && origPat.matcher(fud.origUrl).find());
      }
      return should;
    }
  }
  
}
