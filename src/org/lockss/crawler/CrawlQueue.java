/*
 * $Id$
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.*;
import java.io.*;
import java.text.*;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.buffer.*;
import org.lockss.util.*;

/**
 * CrawlQueue is a priority queue of CrawlUrlData keyed by URL String.  It
 * is not synchronized.  CrawlQueue sorts {@link CrawlUrlData}s (viewed as
 * {@link CrawlUrl}s) by the specified comparator.  If comparator is null,
 * the urls are sorted first by depth then alphabetically, resulting in a
 * breadth-first crawl.  The comparator should never return 0; if it does
 * the results are undefined.
 */
public class CrawlQueue {
  static Logger log = Logger.getLogger("CrawlQueue");

  Buffer sorted;
  Map<String,CrawlUrlData> map;

  /** Create a CrawlQueue that sorts {@link CrawlUrlData} objects (viewed
   * as {@link CrawlUrl}) by the specified comparator.
   * @param comparator the comparator that determines the order of URLs in
   * the queue.  If null, the crawl will be breadth first, with the URLs at
   * each level fetched in the order they were discovered (at the previous
   * level).
   */
  public CrawlQueue(Comparator<CrawlUrl> comparator) {
    if (comparator == null) {
      sorted = new UnboundedFifoBuffer();
    } else {
      sorted = new PriorityBuffer(comparator);
    }
    map = new HashMap<String,CrawlUrlData>();
  }

  /** Add CrawlUrlData to the queue.
   * @throws IllegalArgumentException if the CrawlUrlData is already in the
   * queue
   */
  public void add(CrawlUrlData curl) {
    String url = curl.getUrl();
    if (map.containsKey(url)) {
      throw new IllegalArgumentException("Illegal to replace entry: " + url);
    }
    map.put(url, curl);
    int size = sorted.size();
    sorted.add(curl);
    if (sorted.size() != size + 1) {
      log.error("Adding " + curl +
		" didn't increase size of crawl queue; "
		+ "CrawlUrlComparator must have returned zero");
    }
  }

  /** Return the first CrawlUrlData in the queue */
  public CrawlUrlData first() {
    return (CrawlUrlData)sorted.get();
  }

  /** For unit tests */
  Collection<CrawlUrlData> asList() {
    return new ArrayList(sorted);
  }

  /** Remove the first CrawlUrlData from the queue and return it */
  public CrawlUrlData remove() {
    CrawlUrlData res = (CrawlUrlData)sorted.remove();
    map.remove(res.getUrl());
    return res;
  }

  /** Retun the CrawlUrlData corresponding to the URL */
  public CrawlUrlData get(String url) {
    return map.get(url);
  }

  /** Return the number of elements in the queue */
  public int size() {
    return sorted.size();
  }

  public boolean isEmpty() {
    return map.isEmpty();
  }

  public String toString() {
    return "[" + StringUtil.separatedString(sorted, ", ") + "]";
  }

  /** A comparator that implements a breadth-first crawl where, at each
   * level, the URLs are fetched in alphabetic order */
  public static class AlphabeticalBreadthFirstUrlComparator
    implements Comparator<CrawlUrl> {
    public int compare(CrawlUrl curl1, CrawlUrl curl2) {
      int res = curl1.getDepth() - curl2.getDepth();
      if (res == 0) {
	res = curl1.getUrl().compareTo(curl2.getUrl());
      }
      return res;
    }
  }
}
