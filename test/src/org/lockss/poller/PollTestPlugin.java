/*
 * $Id: PollTestPlugin.java,v 1.15 2006-06-04 06:27:17 tlipkis Exp $
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
package org.lockss.poller;

import java.io.*;
import java.util.*;
import java.net.*;
import java.security.MessageDigest;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * Mock plugin for testing polling.
 */
public class PollTestPlugin {

  public static class PTCachedUrlSet extends MockCachedUrlSet {
    private long duration = 60000;
    private int bytes = 1000;

    public PTCachedUrlSet(MockArchivalUnit owner, CachedUrlSetSpec spec) {
      super(owner,spec);
    }
    /**
     * Set the duration that will be returned by estimatedHashDuration()
     * @param duration ths estimated hash duration
     */
    public void setHashEstimate(long duration) {
      this.duration = duration;
    }

    public long estimatedHashDuration() {
      return duration;
    }

    public Iterator flatSetIterator() {
      ArrayList al = new ArrayList();
      al.add(this);
      return al.iterator();
    }

    public Iterator contentHashIterator() {
      return CollectionUtil.EMPTY_ITERATOR;
    }

    /** Set the actual hash duration and size.
     * @param duration the number of milliseconds the hash will take
     * @param bytes the number of bytes that must be hashed
     */
    public void setHashDuration(long duration, int bytes) {
      this.duration = duration;
      this.bytes = bytes;
    }

    public CachedUrlSetHasher getContentHasher(MessageDigest digest) {
      return new CusHasher(digest, duration, bytes);
    }

    public CachedUrlSetHasher getNameHasher(MessageDigest digest) {
      return new CusHasher(digest, duration, bytes);
    }
  }

/**
 * CachedUrlSetHasher that takes a programmable time to process a
 * programmable number of bytes.
 */
  static class CusHasher extends NullPlugin.CachedUrlSetHasher {
    MessageDigest digest;
    long duration;
    int bytes;

    public CusHasher(MessageDigest digest, long duration, int bytes) {
      this.digest = digest;
      this.duration = duration;
      this.bytes = bytes;
    }

    public long getEstimatedHashDuration() {
      return 1000;
    }

    public boolean finished() {
      return bytes <= 0;
    }

    public int hashStep(int numBytes) {
      if (finished()) {
        return 0;
      }
      numBytes = Math.max(1, Math.min(bytes, numBytes));
      TimerUtil.guaranteedSleep(duration / (bytes / numBytes));

      bytes -= numBytes;
      return numBytes;
    }

    public MessageDigest[] getDigests() {
      return new MessageDigest[] {digest};
    }

  }

  public static class PTArchivalUnit extends MockArchivalUnit {

    public PTArchivalUnit(CrawlSpec spec) {
      super(spec);
    }

    public static MockArchivalUnit createFromListOfRootUrls(String[] rootUrls) {
      CrawlSpec rootSpec =
	new SpiderCrawlSpec(ListUtil.fromArray(rootUrls), null);
      return new PTArchivalUnit(rootSpec);
    }

     public CachedUrlSet makeCachedUrlSet(CachedUrlSetSpec cuss) {
       return new PTCachedUrlSet(this, cuss);
     }

//     public CachedUrlSet makeCachedUrlSet(String url, String regexp) {
//       return new PTCachedUrlSet(this,new MockCachedUrlSetSpec(url,regexp));
//     }

    public boolean shouldBeCached(String url) {
      return true;
    }
  }
}
