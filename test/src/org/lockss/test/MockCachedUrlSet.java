/*
 * $Id: MockCachedUrlSet.java,v 1.16 2003-01-28 00:32:11 aalto Exp $
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

package org.lockss.test;

import java.util.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is a mock version of <code>CachedUrlSet</code> used for testing
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class MockCachedUrlSet implements CachedUrlSet {
  private ArchivalUnit au;
  private CachedUrlSetSpec spec;

  private HashSet cachedUrls = new HashSet();
  private Vector urls = null;
  private Iterator flatIterator = null;
  private Iterator leafIterator = null;
  private Collection flatSource = null;
  private Collection leafSource = null;

  private Hashtable ucHash = new Hashtable();
  private Hashtable cuHash = new Hashtable();

  private static final Logger logger = Logger.getLogger("MockCachedUrlSet");

  public MockCachedUrlSet(ArchivalUnit owner, CachedUrlSetSpec spec) {
    this.spec = spec;
    this.au = owner;
  }

  public CachedUrlSetSpec getSpec() {
    return spec;
  }

  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  public MockCachedUrlSet(CachedUrlSetSpec spec) {
    this(new MockArchivalUnit(), spec);
  }

  public boolean containsUrl(String url) {
    return url.startsWith((String)spec.getPrimaryUrl());
  }

  public boolean isCached(String url) {
    CachedUrl cu = (CachedUrl) cuHash.get(url);
    return cu.exists();
  }

  public Iterator flatSetIterator() {
    if (flatSource!=null) {
      return flatSource.iterator();
    }
    return flatIterator;
  }

  public void setFlatIterator(Iterator it) {
    flatIterator = it;
  }

  public void setFlatItSource(Collection col) {
    flatSource = col;
  }

  public Iterator leafIterator() {
    if (leafSource!=null) {
      return leafSource.iterator();
    }
    return leafIterator;
  }

  public void setLeafIterator(Iterator it) {
    leafIterator = it;
  }

  public void setLeafItSource(Collection col) {
    leafSource = col;
  }


  // Methods used by the poller

  CachedUrlSetHasher contentHasher = null;
  CachedUrlSetHasher nameHasher = null;

  public void setContentHasher(CachedUrlSetHasher hasher) {
    contentHasher = hasher;
  }

  public void setNameHasher(CachedUrlSetHasher hasher) {
    nameHasher = hasher;
  }

  public CachedUrlSetHasher getContentHasher(MessageDigest hasher) {
    return contentHasher;
  }

  public CachedUrlSetHasher getNameHasher(MessageDigest hasher) {
    return nameHasher;
  }

  long hashEstimate = 0;

  public long estimatedHashDuration() {
    return hashEstimate;
  }

  public void setEstimatedHashDuration(long n) {
    hashEstimate = n;
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
  }

  // Methods used by the crawler

  public CachedUrl makeCachedUrl(String url) {
    return null;
  }

  public UrlCacher makeUrlCacher(String url) {
    UrlCacher uc = null;
    if (ucHash != null) {
      uc = (UrlCacher)ucHash.get(url);
      logger.debug(uc+" came from ucHash");
    } else {
      logger.debug("ucHash is null, so makeUrlCacher is returning null");
    }
    return uc;
  }

  //methods used to generate proper mock objects

  /**
   * Sets up a cached url and url cacher for this url
   * @param source content to associate with this url
   * @param url url for which we should set up a CachedUrl and UrlCacher
   * @param exists whether this url should act like it's already in the cache
   * @param shouldCache whether this url should say to cache it or not
   * @param props Properties to be associated with this url
   */
  public void addUrl(String source, String url,
		     boolean exists, boolean shouldCache,
		     Properties props) {
    MockCachedUrl cu = new MockCachedUrl(url);
    cu.setContent(source);
    cu.setProperties(props);
    cu.setExists(exists);

    MockUrlCacher uc = new MockUrlCacher(url, this);
    uc.setShouldBeCached(shouldCache);
    uc.setCachedUrl(cu);

    logger.debug("Adding "+url+" to cuHash and ucHash");

    cuHash.put(url, cu);
    ucHash.put(url, uc);
  }

  /**
   * Same as above, but with exists defaulting to false, shouldCache to false
   * and props to "content-type=text/html"
   * @param source the content
   * @param url the url
   */
  public void addUrl(String source, String url) {
    addUrl(source, url, false, true);
  }

  public void addUrl(String source, String url,
		     boolean exists, boolean shouldCache) {
    Properties props = new Properties();
    props.setProperty("content-type", "text/html");
    addUrl(source, url, exists, shouldCache, props);
  }

  public String getPrimaryUrl() {
    if (spec!=null) {
      return spec.getPrimaryUrl();
    } else {
      return null;
    }
  }

  public String getIdString() {
    if (spec!=null) {
      return spec.getIdString();
    } else {
      return null;
    }
  }

  public void addCachedUrl(String url) {
    cachedUrls.add(url);
  }

  public Set getCachedUrls() {
    return cachedUrls;
  }

  public int hashCode() {
    if (spec!=null) {
      return spec.hashCode();
    } else {
      return 0;
    }
  }

  public boolean equals(Object obj) {
    if (obj instanceof CachedUrlSet) {
      return getIdString().equals(((CachedUrlSet)obj).getIdString());
    } else {
      throw new IllegalArgumentException("Trying to compare a set and a non-set.");
    }
  }
}
