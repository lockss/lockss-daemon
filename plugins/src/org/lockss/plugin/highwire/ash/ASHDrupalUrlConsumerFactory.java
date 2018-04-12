/*
 * $Id$
 */

/*

Copyright (c) 2018 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.highwire.ash;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.plugin.highwire.HighWireDrupalSigninUrlConsumerFactory;
import org.lockss.util.Logger;

/**
 * @since 1.68.0 with storeAtOrigUrl()
 */
public class ASHDrupalUrlConsumerFactory extends HighWireDrupalSigninUrlConsumerFactory {
  private static final Logger log = Logger.getLogger(ASHDrupalUrlConsumerFactory.class);
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
  public class ASHDrupalUrlConsumer extends HighWireDrupalSigninUrlConsumer {
    
    
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
