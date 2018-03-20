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

package org.lockss.plugin.highwire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lockss.daemon.Crawler.CrawlerFacade;
import org.lockss.plugin.*;
import org.lockss.util.Logger;

/**
 * @since 1.68.0 with storeAtOrigUrl()
 */
public class HighWireDrupalSigninUrlConsumerFactory extends HighWireDrupalUrlConsumerFactory {
  private static final Logger log = Logger.getLogger(HighWireDrupalSigninUrlConsumerFactory.class);
  
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
    return new HighWireDrupalSigninUrlConsumer(facade, fud);
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
  public class HighWireDrupalSigninUrlConsumer extends HighWireDrupalUrlConsumer {
    
    
    public HighWireDrupalSigninUrlConsumer(CrawlerFacade facade, FetchedUrlData fud) {
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
      if (!should && fud.redirectUrls != null && (fud.redirectUrls.size() > 1)) {
        //the fetched = original or fetched = original with ?sso-checked=true
        if (fud.fetchUrl.equals(fud.origUrl) ||
        		fud.fetchUrl.equals(fud.origUrl + "?sso-checked=true") ) {
        	  should = true;
        } else {
        	// fetched = original + journal_id dir after content + ?sso-checked=true
          Matcher destMat = destPat.matcher(fud.fetchUrl);
          Matcher origMat = origPat.matcher(fud.origUrl);
          should =  (destMat.find() && origMat.find()
        		  && origMat.group(1) == destMat.group(1)); 
        }
        log.debug3("SO redirect: " + fud.redirectUrls.size() + " " + fud.origUrl + " to " + fud.fetchUrl + " : " + should);
        if (!should) {
          log.debug3("myfud: " + fud.redirectUrls.size() + " " + fud.redirectUrls.toString());
        }
      }
      return should;
    }
    
  }
  
}
