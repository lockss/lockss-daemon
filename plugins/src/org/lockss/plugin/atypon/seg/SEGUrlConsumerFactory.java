/*
 * $Id$
 */

/*

Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
all rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Stanford University shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from Stanford University.

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
    log.debug3("Creating a UrlConsumer");
    return new HighWireDrupalUrlConsumer(facade, fud);
  }
  
  public static Pattern getOrigPattern() {
    return origPat;
  }
  
  public static Pattern getDestPattern() {
    return destPat;
  }

  public class HighWireDrupalUrlConsumer extends HttpToHttpsUrlConsumer {
    
    protected Configuration auconfig;
    
    public HighWireDrupalUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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

        log.debug3("Fei - pdf " + fud.fetchUrl + " " + fud.origUrl);
      }
      return should;
    }
  }
  
}
