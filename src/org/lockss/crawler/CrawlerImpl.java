/*
 * $Id: CrawlerImpl.java,v 1.9 2004-02-09 22:09:14 tlipkis Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.crawler;

import java.io.*;
import java.util.*;
import java.net.URL;
import org.lockss.daemon.*;
import org.lockss.state.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;

/**
 * The crawler.
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public abstract class CrawlerImpl implements Crawler {
  /**
   * TODO
   * 1) write state to harddrive using whatever system we come up for for the
   * rest of LOCKSS
   * 2) check deadline and die if we run too long
   */

  private static Logger logger = Logger.getLogger("CrawlerImpl");

  protected ArchivalUnit au;

  protected Crawler.Status crawlStatus = null;

  protected int numUrlsFetched = 0;

  protected CrawlSpec spec = null;

  protected AuState aus = null;

  protected boolean crawlAborted = false;


  protected abstract boolean doCrawl0(Deadline deadline);
  public abstract int getType();

  protected CrawlerImpl(ArchivalUnit au, CrawlSpec spec, AuState aus) {
    if (au == null) {
      throw new IllegalArgumentException("Called with null au");
    } else if (spec == null) {
      throw new IllegalArgumentException("Called with null spec");
    } else if (aus == null) {
      throw new IllegalArgumentException("Called with null aus");
    }
    this.au = au;
    this.spec = spec;
    this.aus = aus;
  }

  public ArchivalUnit getAu() {
    return au;
  }


  public Crawler.Status getStatus() {
    return crawlStatus;
  }


  public void abortCrawl() {
    crawlAborted = true;
  }

  /**
   * Main method of the crawler; it loops through crawling and caching
   * urls.
   *
   * @param deadline when to terminate by
   * @return true if no errors
   */
  public boolean doCrawl(Deadline deadline) {
    if (deadline == null) {
      throw new IllegalArgumentException("Called with a null Deadline");
    }
    try {
      return doCrawl0(deadline);
    } finally {
      crawlStatus.signalCrawlEnded();
    }
  }



  boolean crawlPermission(CachedUrlSet ownerCus) {
    boolean crawl_ok = false;
    int err = Crawler.STATUS_PUB_PERMISSION;

    // fetch and cache the manifest page
    String manifest = au.getManifestPage();
    Plugin plugin = au.getPlugin();
    UrlCacher uc = plugin.makeUrlCacher(ownerCus, manifest);
    try {
      if (au.shouldBeCached(manifest)) {
        // check for proper crawl window
        if ((au.getCrawlSpec()==null) || (au.getCrawlSpec().canCrawl())) {
          // check for the permission on the page without caching it
          InputStream is = uc.getUncachedInputStream();
          // set the reader to our default encoding
          //XXX try to extract encoding from source
          Reader reader = new InputStreamReader(is, Constants.DEFAULT_ENCODING);
          crawl_ok = au.checkCrawlPermission(reader);
          if (!crawl_ok) {
            logger.error("Couldn't start crawl due to missing permission.");
          } else {
            logger.debug2("Permission granted. Caching permission page.");
            uc.cache();
          }
        } else {
          logger.debug("Couldn't start crawl due to crawl window restrictions.");
          err = Crawler.STATUS_WINDOW_CLOSED;
        }
      } else {
	logger.debug("Manifest not within CrawlSpec");
      }
    } catch (IOException ex) {
      logger.warning("Exception reading manifest: "+ex);
      crawlStatus.setCrawlError(Crawler.STATUS_FETCH_ERROR);
    }
    if (!crawl_ok) {
      crawlStatus.setCrawlError(Crawler.STATUS_FETCH_ERROR);
    }
    return crawl_ok;
  }

  protected static boolean isSupportedUrlProtocol(String url) {
    try {
//       URL ur = new URL(srcUrl, url);
      URL ur = new URL(url);
      // some 1.4 machines will allow this, so we explictly exclude it for now.
      if (StringUtil.getIndexIgnoringCase(ur.toString(), "https") != 0) {
        return true;
      }
    }
    catch (Exception ex) {
    }
    return false;
  }

  public void setWatchdog(LockssWatchdog wdog) {
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[CrawlerImpl: ");
    sb.append(au.toString());
    sb.append("]");
    return sb.toString();
  }

}
