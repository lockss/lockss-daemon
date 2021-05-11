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

package org.lockss.plugin.ojs3;

import org.lockss.config.Configuration;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.FetchedUrlData;
import org.lockss.plugin.UrlConsumer;
import org.lockss.plugin.UrlConsumerFactory;
import org.lockss.plugin.base.HttpToHttpsUrlConsumer;
import org.lockss.util.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OJS3UrlConsumerFactory implements UrlConsumerFactory {
  private static final Logger log = Logger.getLogger(OJS3UrlConsumerFactory.class);

  // Some PDF got redirected to a different domain
  //  https://scholarworks.iu.edu/journals/index.php/ijpbl/article/view/28134/33423 will be redireted to
  // https://docs.lib.purdue.edu/cgi/viewcontent.cgi?article=1520&context=ijpbl
  protected static final String ORIG_STRING = ".*/article/view/\\d+/\\d+$";

  protected static final String DEST_STRING = "/cgi/viewcontent\\.cgi\\?article=/(.*)$";
  
  protected static final Pattern origPat = Pattern.compile(ORIG_STRING, Pattern.CASE_INSENSITIVE);
  protected static final Pattern destPat = Pattern.compile(DEST_STRING, Pattern.CASE_INSENSITIVE);
  
  @Override
  public UrlConsumer createUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
    return new OJS3UrlConsumer(facade, fud);
  }
  
  public static Pattern getOrigPattern() {
    return origPat;
  }
  
  public static Pattern getDestPattern() {
    return destPat;
  }

  public class OJS3UrlConsumer extends HttpToHttpsUrlConsumer {
    
    protected Configuration auconfig;
    
    public OJS3UrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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
      
      if (!should && origPat.matcher(fud.origUrl).find()) {
        
        Matcher destMat = null;
        should = (fud.redirectUrls != null
            && fud.redirectUrls.size() >= 1
            && (fud.redirectUrls.get(0).contains("/cgi/viewcontent.cgi?article=")));
        if (should
            && fud.fetchUrl != null) {
          log.debug3(String.format("fud.fetchUrl: %s fud.redirectUrls: %s", fud.fetchUrl, fud.redirectUrls.get(0)));
        } else if (fud.fetchUrl != null) {
          log.debug3(String.format("fud.fetchUrl: %s", fud.fetchUrl));
        }
      }
      return should;
    }
  }
  
}
