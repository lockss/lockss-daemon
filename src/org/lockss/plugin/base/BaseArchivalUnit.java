/*
 * $Id: BaseArchivalUnit.java,v 1.1 2003-02-24 22:13:42 claire Exp $
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

package org.lockss.plugin.base;

import java.util.*;
import gnu.regexp.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.state.*;
import org.lockss.daemon.*;

/**
 * Abstract base class for ArchivalUnits.
 * Plugins may extend this to get some common ArchivalUnit functionality.
 */
public abstract class BaseArchivalUnit implements ArchivalUnit {
  private static final int
    DEFAULT_MILLISECONDS_BETWEEN_CRAWL_HTTP_REQUESTS = 10000;

  private Plugin plugin;
  protected CrawlSpec crawlSpec;
  private String idStr = null;

  /**
   * Must invoke this constructor in plugin subclass.
   * @param spec the CrawlSpec
   */
  protected BaseArchivalUnit(Plugin myPlugin, CrawlSpec spec) {
    this(myPlugin);
    crawlSpec = spec;
  }

  protected BaseArchivalUnit(Plugin myPlugin) {
    plugin = myPlugin;
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
  public abstract UrlCacher urlCacherFactory(CachedUrlSet owner,
					     String url);

  /**
   * Return the Plugin's ID.
   * @return the Plugin's ID.
   */
  public String getPluginId() {
    return plugin.getPluginId();
  }

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
   * @param url the url string
   * @param lwrBound the lower boundary of our match range
   * @param uprBound the upper boundary of our match range
   * @return the newly created CachedUrlSet
   */
  public CachedUrlSet makeCachedUrlSet(String url, String lwrBound, String uprBound) {
    return makeCachedUrlSet(new RangeCachedUrlSetSpec(url, lwrBound, uprBound));
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

  public String toString() {
    return "[BAU: "+getPluginId()+":"+getAUId()+"]";
  }

  /**
   * Overrides Object.hashCode();
   * Returns the sum of the hashcodes of the two ids.
   * @return the hashcode
   */
  public int hashCode() {
    return getPluginId().hashCode() + getAUId().hashCode();
  }

  public List getNewContentCrawlUrls() {
    return null;
  }

  /**
   * Overrides Object.equals().
   * Returns true if the ids are equal
   * @param obj the object to compare to
   * @return true if the ids are equal
   */
  public boolean equals(Object obj) {
    if (obj instanceof ArchivalUnit) {
      ArchivalUnit au = (ArchivalUnit)obj;
      return ((getPluginId().equals(au.getPluginId())) &&
              (getAUId().equals(au.getAUId())));
    } else {
      return false;
    }
  }

  public boolean shouldCrawlForNewContent(AuState aus) {
    return false;
  }

  protected void pause(int milliseconds) {
    try {
      Thread thread = Thread.currentThread();
      thread.sleep(milliseconds);
    } catch (InterruptedException ie) { }
  }

}
