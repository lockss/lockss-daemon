/*
 * $Id: BaseArchivalUnit.java,v 1.5 2003-01-28 00:32:11 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.daemon;

import gnu.regexp.*;
import org.lockss.util.*;

/**
 * Abstract base class for ArchivalUnits.
 * Plugins may extend this to get some common ArchivalUnit functionality.
 */
public abstract class BaseArchivalUnit implements ArchivalUnit {
  private static final int
    DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS = 10000;
  private static Logger logger = Logger.getLogger("ArchivalUnit");

  private CrawlSpec crawlSpec;

  /**
   * Must invoke this constructor in plugin subclass.
   * @param spec the CrawlSpec
   */
  protected BaseArchivalUnit(CrawlSpec spec) {
    crawlSpec = spec;
  }

  // Factories that must be implemented by plugin subclass

  /**
   * Create an instance of the plugin-specific implementation of
   * CachedUrlSet, with the specified owner and CachedUrlSetSpec
   * @param owner the ArchivalUnit owner
   * @param cuss the spec
   * @return the cus
   */
  public abstract CachedUrlSet cachedUrlSetFactory(ArchivalUnit owner,
						      CachedUrlSetSpec cuss);

  /**
   * Create an instance of the plugin-specific implementation of
   * CachedUrl, with the specified owner and url
   * @param owner the CachedUrlSet owner
   * @param url the url
   * @return the CachedUrl
   */
  public abstract CachedUrl cachedUrlFactory(CachedUrlSet owner,
						String url);

  /**
   * Create an instance of the plugin-specific implementation of
   * UrlCacher, with the specified owner and url
   * @param owner the CachedUrlSet owner
   * @param url the url
   * @return the UrlCacher
   */
  public abstract UrlCacher urlCacherFactory(CachedUrlSet  owner,
						String url);

  /**
   * Return the CrawlSpec.
   * @return the spec
   */
  public CrawlSpec getCrawlSpec() {
    return crawlSpec;
  }

  /**
   * Determine whether the url falls within the CrawlSpec.
   * @param url the url
   * @return true if it is included
   */
  public boolean shouldBeCached(String url) {
    return getCrawlSpec().isIncluded(url);
  }

  /**
   * Create a CachedUrlSet representing the content in this AU
   * that matches the CachedUrlSetSpec
   * @param cuss the spec
   * @return the CachedUrlSet
   */
  public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
    CachedUrlSet cus = cachedUrlSetFactory(this, cuss);
    return cus;
  }

  /**
   * Create a CachedUrlSet representing the content in this AU
   * that matches the url and regexp.
   * @param url the string
   * @param regexp the regexp
   * @return the CachedUrlSet
   * @throws REException
   */
  public CachedUrlSet makeCachedUrlSet(String url, String regexp)
      throws REException {
    return makeCachedUrlSet(new RECachedUrlSetSpec(url, regexp));
  }

  /**
   * Return the CachedUrlSet representing the entire contents
   * of this AU
   * @return the CachedUrlSet
   */
  public CachedUrlSet getAUCachedUrlSet() {
    // tk this needs to compute the top-level CUSS
    return makeCachedUrlSet(null);
  }

  public void pause() {
    pause(DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS);
  }

  /**
   * Concats the plugin id and au id to generate a unique id.
   * @return the unique id
   */
  public String getIdString() {
    return getPluginId() + ":" + getAUId();
  }

  /**
   * Overrides Object.hashCode();
   * Returns the hashcode of the id string.
   * @return the hashcode
   */
  public int hashCode() {
    return getIdString().hashCode();
  }

  /**
   * Overrides Object.equals().
   * Returns true if the id strings are equal
   * @param obj the object to compare to
   * @return true if the id strings are equal
   */
  public boolean equals(Object obj) {
    if (obj instanceof ArchivalUnit) {
      return getIdString().equals(((ArchivalUnit)obj).getIdString());
    } else {
      logger.error("Trying to compare an au and a non-au.");
      throw new IllegalArgumentException("Trying to compare an au and a non-au.");
    }
  }

  protected void pause(int milliseconds) {
    try {
      Thread thread = Thread.currentThread();
      thread.sleep(milliseconds);
    } catch (InterruptedException ie) { }
  }

}
