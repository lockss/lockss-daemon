/*
 * $Id$
 */

/*

Copyright (c) 2000-2015 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.springer.link;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

import org.lockss.daemon.*;
import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.base.SimpleUrlConsumer;
import org.lockss.util.urlconn.CacheException;

/**
 * <p>
 * A custom URL consumer that identifies specific redirect chains and stores the
 * content at the origin of the chain (e.g. to support collecting and repairing
 * redirect chains that begin with fixed URLs but end with one-time URLs).
 * </p>
 * 
 * @since 1.67.5
 */
public class SpringerLinkUrlConsumer extends SimpleUrlConsumer {
  
  public static final String DOWNLOAD_URL_KEY = "download_url";
  public static final String ACCESS_STRING = "accesspage";

  
  protected Pattern origPdfPat;
  protected Pattern destPdfPat;
  
  public SpringerLinkUrlConsumer(CrawlerFacade facade,
                                FetchedUrlData fud) {
    super(facade, fud);
    origPdfPat = makeOrigPdfPattern(au.getConfiguration().get(ConfigParamDescr.BASE_URL.getKey()));
    destPdfPat = makeDestPdfPattern(au.getConfiguration().get(DOWNLOAD_URL_KEY));
  }

  @Override
  public void consume() throws IOException {
    if (shouldStoreAtOrigUrl()) {
      storeAtOrigUrl();
    }
    if((fud.origUrl != null && fud.origUrl.contains(ACCESS_STRING)) ||
       (fud.fetchUrl != null && fud.fetchUrl.contains(ACCESS_STRING))) {
      throw new CacheException.PermissionException("Found a login page");
    }
    super.consume();
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
   * @since 1.67.5
   */
  protected boolean shouldStoreAtOrigUrl() {
    return fud.redirectUrls != null
        && fud.redirectUrls.size() == 1
        && fud.redirectUrls.get(0).equals(fud.fetchUrl)
        && destPdfPat.matcher(fud.fetchUrl).find()
        && origPdfPat.matcher(fud.origUrl).find();
  }
  
  /**
   * 
   * @param baseUrl
   * @return
   * @since 1.67.5
   */
  protected static Pattern makeOrigPdfPattern(String baseUrl) {
    return Pattern.compile(String.format("^%s(content|download)/(pdf|epub)/.*\\.(pdf|epub)$",
                                         baseUrl),
                           Pattern.CASE_INSENSITIVE);
  }

  /**
   * 
   * @param downloadUrl
   * @return
   * @since 1.67.5
   */
  protected static Pattern makeDestPdfPattern(String downloadUrl) {
    return Pattern.compile(String.format("^%sstatic/(pdf|epub)/.*\\.(pdf|epub)\\?[^=]*=[^&]*",
                                         downloadUrl),
                           Pattern.CASE_INSENSITIVE);
  }
  
}
