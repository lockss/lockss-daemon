/*
 * $Id$
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.hasher;
import java.io.*;
import java.util.*;
import java.security.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.crawler.CrawlManager;
import org.lockss.plugin.*;

/**
 * General class to handle content hashing
 */
public abstract class GenericHasher implements CachedUrlSetHasher {
  protected static Logger log = Logger.getLogger("GenericHasher");

  protected CachedUrlSet cus = null;
  protected ArchivalUnit au = null;
  protected MessageDigest digest = null;
  protected CachedUrlSetNode curNode = null;
  protected CachedUrl curCu = null;
  protected CrawlManager crawlMgr = null;

  protected Iterator iterator = null;
  protected boolean isFinished = false;
  protected boolean isAborted = false;
  protected boolean isFiltered = true;

  private boolean isTrace = log.isDebug3();

  protected GenericHasher(CachedUrlSet cus) {
    if (cus == null) {
      throw new NullPointerException("Called with a null CachedUrlSet");
    }
    this.cus = cus;
    au = cus.getArchivalUnit();
    iterator = getIterator(cus);
    if (iterator == null) {
      throw new IllegalArgumentException(cus + " returned null iterator");
    }
    LockssDaemon daemon = AuUtil.getDaemon(au);
    if (daemon != null) {
      crawlMgr = daemon.getCrawlManager();
    }
  }

  protected GenericHasher(CachedUrlSet cus, MessageDigest digest) {
    this(cus);
    if (digest == null) {
      throw new IllegalArgumentException("Called with a null MessageDigest");
    }
    this.digest = digest;
  }

  /** Subclass must override to return a short string describing the type
   * of hash, to be used in a status table
   */
  public abstract String typeString();

  /** Subclass must override to return the CUS iterator appropriate for the
   * hash
   */
  protected abstract Iterator getIterator(CachedUrlSet cus);

  /* Subclass should override this to hash the specified element
   */
  protected abstract long hashNodeUpToNumBytes(int numBytes)
      throws IOException;

  /** Subclass should override if it wants to exclude from the hash some
   * nodes that are returned by the iterator */
  protected boolean isIncluded(CachedUrlSetNode node) {
    return true;
  }

  public void setFiltered(boolean val) {
    isFiltered = val;
  }

  protected InputStream getInputStream(CachedUrl cu) {
    return isFiltered ? cu.openForHashing() : cu.getUnfilteredInputStream();
  }

  protected InputStream getInputStream(CachedUrl cu,
				       HashedInputStream.Hasher hasher) {
    return isFiltered ? cu.openForHashing(hasher)
      : cu.getUnfilteredInputStream(hasher);
  }

  /** Subclass should override to return proper array of digest */
  public MessageDigest[] getDigests() {
    MessageDigest[] res = new MessageDigest[] { digest };
    return res;
  }

  /** Subclass should override to record the actual hash time, if
   * appropriate for the hash type
   */
  public abstract void storeActualHashDuration(long elapsed, Exception err);


  public CachedUrlSet getCachedUrlSet() {
    return cus;
  }

  public long getEstimatedHashDuration() {
    return cus.estimatedHashDuration();
  }

  /**
   * @return true if there is nothing left to hash
   */
  public boolean finished() {
    return isFinished;
  }

  /**
   * @param numBytes maximum number of bytes to hash (counting delimiters)
   * @return number of bytes actually hashed.  This will only be less than
   * numBytes if there is nothing left to hash.
   * @throws IOException
   */
  public int hashStep(int numBytes) throws IOException {
    if (isAborted) {
      throw new IllegalStateException("Hash step called after hasher aborted");
    }
    int bytesLeftToHash = numBytes;
    if (finished()) {
      log.warning("Hash step called after hasher was finished");
      return 0;
    }

    if (isTrace) log.debug3(numBytes+" bytes left to hash in this step");

    int totalBytesHashed = 0;
    while (bytesLeftToHash > 0) {
      if (curNode == null) {
	curNode = getNextNode();
	if (curNode != null) {
	  if (isTrace) log.debug3("Getting next element to hash");
	}
	else {
	  if (isTrace) log.debug3("No more elements to hash");
	  done();
	  return numBytes - bytesLeftToHash;
	}
      }
      long numBytesHashed =
	hashNodeUpToNumBytes(bytesLeftToHash);
      bytesLeftToHash -= numBytesHashed;
      totalBytesHashed += numBytesHashed;
    }
    return totalBytesHashed;
  }

  /** Called when hash is complete */
  protected void done() {
    this.isFinished = true;
  }

  protected CachedUrlSetNode getNextNode() {
    while (iterator.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode)iterator.next();
      if (isIncluded(node)) {
	return node;
      }
    }
    return null;
  }

  protected CachedUrl getCurrentCu() {
    if (curCu == null) {
      curCu = AuUtil.getCu(curNode);
    }
    return curCu;
  }

  protected void endOfNode() {
    curNode = null;
    curCu = null;
  }

  public void abortHash() {
    isAborted = true;
  }
}
