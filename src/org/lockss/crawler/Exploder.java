/*
 * $Id: Exploder.java,v 1.1.2.1 2007-09-11 19:14:55 dshr Exp $
 */

/*

Copyright (c) 2007 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.CrawlSpec;
import org.lockss.plugin.UrlCacher;
import org.lockss.crawler.BaseCrawler;

/**
 * The abstract base class for exploders, which handle extracting files from
 * archives.
 *
 * @author  David S. H. Rosenthal
 * @version 0.0
 */

public abstract class Exploder {
  protected UrlCacher urlCacher;
  protected int maxRetries;
  protected CrawlSpec crawlSpec;
  protected BaseCrawler crawler;
  protected boolean explodeFiles;
  protected boolean storeArchive;

  /**
   * Constructor
   * @param uc UrlCacher for the archive
   * @param maxRetries
   * @param crawlSpec the CrawlSpec for the crawl that found the archive
   * @param crawler the crawler that found the archive
   * @param explode true to explode the archives
   * @param store true to store the archive as well
   */
  protected Exploder(UrlCacher uc, int maxRetries, CrawlSpec crawlSpec,
		     BaseCrawler crawler, boolean explode, boolean store) {
    urlCacher = uc;
    this.maxRetries = maxRetries;
    this.crawlSpec = crawlSpec;
    this.crawler = crawler;
    explodeFiles = explode;
    storeArchive = store;
  }

  /**
   * Explode the archive into its constituent elements
   */
  protected abstract void explodeUrl();

}
