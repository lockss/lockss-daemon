/*
 * $Id: MockCachedUrlSet.java,v 1.47 2004-03-09 23:37:52 tlipkis Exp $
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

package org.lockss.test;

import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * This is a mock version of <code>CachedUrlSet</code> used for testing
 *
 * @author  Thomas S. Robertson
 * @version 0.0
 */

public class MockCachedUrlSet implements CachedUrlSet {
  private MockArchivalUnit au;
  private CachedUrlSetSpec spec;
  private String url;

  private boolean isLeafIsSet = false;
  private boolean isLeaf = false;

  private boolean hasContentIsSet = false;
  private boolean hasContent = false;

  private Set cachedUrls = new HashSet();
  private Set forceCachedUrls = new HashSet();

  private Vector urls = null;
  private Iterator flatIterator = null;
  private Iterator hashIterator = null;
  private Collection flatSource = null;
  private Collection hashSource = null;

  private Hashtable ucHash = new Hashtable();
  private Hashtable cuHash = new Hashtable();

  private long actualHashDuration;

  private Map cacheAttempts = new HashMap();

  private static final Logger logger = Logger.getLogger("MockCachedUrlSet");

  public MockCachedUrlSet() {
    this(new MockArchivalUnit(), (CachedUrlSetSpec)null);
  }

  public MockCachedUrlSet(MockArchivalUnit owner, CachedUrlSetSpec spec) {
    this.spec = spec;
    this.au = owner;
  }

  public MockCachedUrlSet(String url) {
    this.url = url;
  }

  public CachedUrlSetSpec getSpec() {
    return spec;
  }

  public void setSpec(CachedUrlSetSpec newSpec) {
    spec = newSpec;
  }

  public ArchivalUnit getArchivalUnit() {
    return au;
  }

  public void setArchivalUnit(MockArchivalUnit newAu) {
    au = newAu;
  }

  public MockCachedUrlSet(CachedUrlSetSpec spec) {
    this(new MockArchivalUnit(), spec);
  }

  public boolean containsUrl(String url) {
    return spec.matches(url);
  }

  public boolean hasContent() {
    if (hasContentIsSet) {
      return this.hasContent;
    }
    CachedUrl cu = (CachedUrl)cuHash.get(getUrl());
    if (cu != null)
      return cu.hasContent();
    else
      return false;
  }

  public void setHasContent(boolean hasContent) {
    this.hasContentIsSet = true;
    this.hasContent = hasContent;
  }

  public boolean isLeaf() {
    if (isLeafIsSet) {
      return isLeaf;
    }
    return (((flatIterator==null) || (!flatIterator.hasNext())) &&
            ((hashIterator==null) || (!hashIterator.hasNext())) &&
            ((flatSource==null) || (flatSource.size() == 0)) &&
            ((hashSource==null) || (hashSource.size() == 0)));
  }

  public void setIsLeaf(boolean isLeaf) {
    this.isLeafIsSet = true;
    this.isLeaf = isLeaf;
  }

  public int getType() {
    return CachedUrlSetNode.TYPE_CACHED_URL_SET;
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

  public Iterator contentHashIterator() {
    if (hashSource!=null) {
      return hashSource.iterator();
    }
    return hashIterator;
  }

  public void setHashIterator(Iterator it) {
    hashIterator = it;
  }

  public void setHashItSource(Collection col) {
    hashSource = col;
  }


  // Methods used by the poller

  CachedUrlSetHasher contentHasher = null;
  CachedUrlSetHasher nameHasher = null;
  byte[] contentToBeHashed = null;
  byte[] namesToBeHashed = null;

  public void setContentHasher(CachedUrlSetHasher hasher) {
    contentHasher = hasher;
  }

  public void setNameHasher(CachedUrlSetHasher hasher) {
    nameHasher = hasher;
  }

  public void setContentToBeHashed(byte[] content) {
    contentToBeHashed = content;
  }

  public void setNamesToBeHashed(byte[] names) {
    namesToBeHashed = names;
  }

  public CachedUrlSetHasher getContentHasher(MessageDigest hasher) {
    if (contentToBeHashed != null)
      hasher.update(contentToBeHashed);
    return contentHasher;
  }

  public CachedUrlSetHasher getNameHasher(MessageDigest hasher) {
    if (namesToBeHashed != null)
      hasher.update(namesToBeHashed);
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
    actualHashDuration = elapsed;
  }

  public long getActualHashDuration() {
    return actualHashDuration;
  }

  // Methods used by the crawler

  public CachedUrl makeCachedUrl(String url) {
    CachedUrl cu = null;
    if (cuHash != null) {
      cu = (CachedUrl)cuHash.get(url);
      logger.debug(cu+" came from cuHash");
    } else {
      logger.debug("cuHash is null, so makeCachedUrl is returning null");
    }
    return cu;
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

  public void addContent(String url, String content) {
    MockCachedUrl cu = (MockCachedUrl)makeCachedUrl(url);
    if (cu != null) {
      cu.setContent(content);
    }
  }

  /**
   * Sets up a cached url and url cacher for this url
   * @param source content to associate with this url
   * @param url url for which we should set up a CachedUrl and UrlCacher
   * @param exists whether this url should act like it's already in the cache
   * @param shouldCache whether this url should say to cache it or not
   * @param props CIProperties to be associated with this url
   */
  public void addUrl(String url,
		     boolean exists, boolean shouldCache,
		     CIProperties props) {
    addUrl(url, exists, shouldCache, props, null, 0);
  }

  private void addUrl(String url,
		      boolean exists, boolean shouldCache,
		      CIProperties props, Exception cacheException,
		      int timesToThrow) {
    MockCachedUrl cu = new MockCachedUrl(url, this);
//     cu.setContent(source);
    cu.setProperties(props);
    cu.setExists(exists);

    MockUrlCacher uc = makeMockUrlCacher(url, this);
    uc.setShouldBeCached(shouldCache);
    if (shouldCache && au != null) {
      au.addUrlToBeCached(url);
    }
    uc.setCachedUrl(cu);
    if (cacheException != null) {
      if (cacheException instanceof IOException) {
	uc.setCachingException((IOException)cacheException, timesToThrow);
      } else if (cacheException instanceof RuntimeException) {
	uc.setCachingException((RuntimeException)cacheException, timesToThrow);
      }
    }
    logger.debug("Adding "+url+" to cuHash and ucHash");

    cuHash.put(url, cu);
    ucHash.put(url, uc);
  }

  protected MockUrlCacher makeMockUrlCacher(String url,
					    MockCachedUrlSet parent) {
    return new MockUrlCacher(url, this);
  }

  /**
   * To be used when you want to set up a url that will throw an exception
   * @param url the url
   * @param cacheException the IOException to throw
   * @param timesToThrow number of times to throw the exception
   */
  public void addUrl(String url,
		     Exception cacheException, int timesToThrow) {
    addUrl(url, false, true, new CIProperties(),
	   cacheException, timesToThrow);
  }

  /**
   * Same as above, but with exists defaulting to false, shouldCache to false
   * and props to "content-type=text/html"
   * @param source the content
   * @param url the url
   */
  public void addUrl(String url) {
    addUrl(url, false, true);
  }

  public void addUrl(String url,
		     boolean exists, boolean shouldCache) {
    CIProperties props = new CIProperties();
    props.setProperty(CachedUrl.PROPERTY_CONTENT_TYPE, "text/html");
    addUrl(url, exists, shouldCache, props);
  }

  public String getUrl() {
    if (url != null) {
      return url;
    }
    if (spec!=null) {
      return spec.getUrl();
    } else {
      return null;
    }
  }

  public void addCachedUrl(String url) {
    cachedUrls.add(url);
  }

  public void addForceCachedUrl(String url) {
    forceCachedUrls.add(url);
  }

  public Set getCachedUrls() {
    return cachedUrls;
  }

  public Set getForceCachedUrls() {
    return forceCachedUrls;
  }

  public int hashCode() {
    if (spec!=null) {
      return spec.hashCode();
    } else {
      return 0;
    }
  }

  public void signalCacheAttempt(String url) {
    Integer numTimesCached = (Integer) cacheAttempts.get(url);
    if (numTimesCached == null) {
      cacheAttempts.put(url, new Integer(1));
    } else {
      cacheAttempts.put(url, new Integer(numTimesCached.intValue()+1));
    }
  }
  public int getNumCacheAttempts(String url) {
    Integer num = (Integer)cacheAttempts.get(url);
    return (num == null ? 0 : num.intValue());
  }

  public boolean equals(Object obj) {
    if (obj instanceof CachedUrlSet) {
      CachedUrlSet cus = (CachedUrlSet)obj;
      if (spec==null) {
        return (cus.getSpec()==null);
      } else {
        return spec.equals(cus.getSpec());
      }
    } else {
      return false;
    }
  }
}
