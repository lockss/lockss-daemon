/*
 * $Id: ArchivalUnit.java,v 1.1 2003-02-24 22:13:41 claire Exp $
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

package org.lockss.plugin;
import gnu.regexp.*;
import java.util.*;
import org.lockss.state.*;
import org.lockss.daemon.*;

/**
 * An <code>ArchivalUnit</code> represents a publication unit
 * (<i>eg</i>, a journal volume).  It:
 * <ul>
 * <li>Is the nexus of the plugin
 * <li>Is separately configurable
 * <li>Has a {@link CrawlSpec} that directs the crawler
 * </ul>
 * Plugins must provide a class that implements this (possibly by extending
 * <code>BaseArchivalUnit</code>).
 */
public interface ArchivalUnit {

  /**
   * Supply (possibly changed) configuration information to an existing AU.
   */
  public void setConfiguration(Configuration config);

  /**
   * Determine whether the url falls within the CrawlSpec.
   * @param url the url to test
   * @return true if it should be cached
   */
  public boolean shouldBeCached(String url);

  /**
   * Create a <code>CachedUrlSet</code> representing the content in this AU
   * that matches the url and regexp.
   * @param url the CachedUrlSet url
   * @param lwrBound the lower bound of the match range
   * @param uprBound the upper bound of the match range
   * @return the created CachedUrlSet
   */
  public CachedUrlSet makeCachedUrlSet(String url, String lwrBound,
                                       String uprBound);

  /**
   * Return the <code>CachedUrlSet</code> representing the entire contents
   * of this AU
   * @return the top-level CachedUrlSet
   */
  public CachedUrlSet getAUCachedUrlSet();

  /**
   * Return the {@link CrawlSpec}
   * @return the CrawlSpec for the AU
   */
  public CrawlSpec getCrawlSpec();

  /**
   * Returns a unique string identifier for the Plugin.
   * @return a unique id
   */
  public String getPluginId();

  /**
   * Returns a unique string identifier for the ArchivalUnit instance
   * within the Plugin.  This must be completely determined by the subset of
   * the AU's configuration info that's necessary to identify the AU.
   * @return a unique id
   */
  public String getAUId();

  /**
   * Sleeps for the interval needed between requests to the server
   */
  // tk - shouldn't be here
  public void pause();

  /**
   * Needs to be overridden to hash ArchivalUnits properly.
   * @return the hashcode
   */
  public int hashCode();

  /**
   * Needs to be overridden to hash ArchivalUnits properly.
   * @param obj the object to compare to
   * @return true if equal
   */
  public boolean equals(Object obj);

  /**
   * @return list of urls which need to be recrawled during a new content
   * crawl
   */
  public List getNewContentCrawlUrls();


  /**
   * Query the AUState object to determine if this is the proper time to
   * do a new content crawl.
   *
   * @param aus state object for this archival unit
   * @return true if we should do a new content crawl
   */
  public boolean shouldCrawlForNewContent(AuState aus);

  public class ConfigurationException extends Exception {
    private String msg;
    private Throwable nestedException;

    public ConfigurationException(String msg) {
      this(msg, null);
    }

    public ConfigurationException(String msg, Throwable e) {
      this.msg = msg;
      this.nestedException = e;
    }

    public Throwable getNestedException() {
      return nestedException;
    }

    public String getMsg() {
      return msg;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append(msg);
      if (nestedException != null) {
	sb.append("\nnested exception:\n");
	sb.append(nestedException.toString());
      }
      return sb.toString();
    }
  }
}
