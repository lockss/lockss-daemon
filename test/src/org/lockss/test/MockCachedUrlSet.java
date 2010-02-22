/*
 * $Id: MockCachedUrlSet.java,v 1.53 2010-02-22 07:02:39 tlipkis Exp $
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

  private Iterator flatIterator = null;
  private Iterator hashIterator = null;
  private Collection flatSource = null;
  private Collection hashSource = null;


  private long actualHashDuration;

  private Map cacheAttempts = new HashMap();

  private static final Logger logger = Logger.getLogger("MockCachedUrlSet");

  public MockCachedUrlSet() {
    this(new MockArchivalUnit(), (MockCachedUrlSetSpec)null);
  }

  public MockCachedUrlSet(MockArchivalUnit owner, CachedUrlSetSpec spec) {
    this(owner);
    this.spec = spec;
  }

  public MockCachedUrlSet(MockArchivalUnit owner) {
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
    if (au != null) {
      CachedUrl cu = au.makeCachedUrl(getUrl());
      return cu != null && cu.hasContent();
    }
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

  public CachedUrlSetHasher getContentHasher(MessageDigest digest) {
    if (contentToBeHashed != null) {
      digest.update(contentToBeHashed);
    }
    return contentHasher;
  }

  public CachedUrlSetHasher getNameHasher(MessageDigest digest) {
    if (namesToBeHashed != null) {
      digest.update(namesToBeHashed);
    }
    return nameHasher;
  }

  private long hashEstimate = 0;

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

  //methods used to generate proper mock objects


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

  public int cusCompare(CachedUrlSet cus2) {
    // check that they're in the same AU
    if (!this.getArchivalUnit().equals(cus2.getArchivalUnit())) {
      return NO_RELATION;
    }
    CachedUrlSetSpec spec1 = this.getSpec();
    CachedUrlSetSpec spec2 = cus2.getSpec();
    String url1 = this.getUrl();
    String url2 = cus2.getUrl();

    // check for top-level urls
    if (spec1.isAu() || spec2.isAu()) {
      if (spec1.equals(spec2)) {
        return SAME_LEVEL_OVERLAP;
      } else if (spec1.isAu()) {
        return ABOVE;
      } else {
        return BELOW;
      }
    }

    if (!url1.endsWith(UrlUtil.URL_PATH_SEPARATOR)) {
      url1 += UrlUtil.URL_PATH_SEPARATOR;
    }
    if (!url2.endsWith(UrlUtil.URL_PATH_SEPARATOR)) {
      url2 += UrlUtil.URL_PATH_SEPARATOR;
    }
    if (url1.equals(url2)) {
      //the urls are on the same level; check for overlap
      if (spec1.isDisjoint(spec2)) {
        return SAME_LEVEL_NO_OVERLAP;
      } else {
        return SAME_LEVEL_OVERLAP;
      }
    } else if (spec1.subsumes(spec2)) {
      // parent
      return ABOVE;
    } else if (spec2.subsumes(spec1)) {
      // child
      return BELOW;
    } else if (spec2.isSingleNode()) {
      if (url1.startsWith(url2)) {
        return SAME_LEVEL_NO_OVERLAP;
      }
      // else, cus2 probably has a range which excludes url1
    } else if (spec1.isSingleNode()) {
      if (url2.startsWith(url1)) {
        return SAME_LEVEL_NO_OVERLAP;
      }
      // else, cus1 probably has a range which excludes url2
    }
    // no connection between the two urls
    return NO_RELATION;
  }

}
