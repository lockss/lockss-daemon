/*
 * $Id: AuSuspectUrlVersions.java,v 1.3.2.1 2013-08-08 05:47:48 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.repository;

import java.util.*;
import org.lockss.util.*;
import org.lockss.hasher.HashResult;

/**
 * Instances represent the set of versions of urls in an AU that
 * have been marked suspect because they failed a local hash
 * verification, meaning that either the url's content or its
 * stored hash is corrupt.  This class is thread safe.
 */
public class AuSuspectUrlVersions implements LockssSerializable {
  public class SuspectUrlVersion implements LockssSerializable {
    private final String url;
    private final int version;
    private final long created;
    private final HashResult computedHash;
    private final HashResult storedHash;

    protected SuspectUrlVersion(String url, int version) {
      this.url = url;
      this.version = version;
      this.created = TimeBase.nowMs();
      this.computedHash = null;
      this.storedHash = null;
    }

    protected SuspectUrlVersion(String url, int version, String algorithm,
				byte[] computedHash, byte[] storedHash) {
      this.url = url;
      this.version = version;
      this.created = TimeBase.nowMs();
      this.computedHash = HashResult.make(computedHash, algorithm);
      this.storedHash = HashResult.make(storedHash, algorithm);
    }

    protected SuspectUrlVersion(String url, int version,
				HashResult computedHash,
				HashResult storedHash) {
      this.url = url;
      this.version = version;
      this.created = TimeBase.nowMs();
      this.computedHash = computedHash;
      this.storedHash = storedHash;
    }

    public String getUrl() {
      return url;
    }
    public int getVersion() {
      return version;
    }
    public long getCreated() {
      return created;
    }
    public int hashCode() {
      return url.hashCode() + version;
    }
    public HashResult getComputedHash() {
      return computedHash;
    }
    public HashResult getStoredHash() {
      return storedHash;
    }
    public boolean equals(Object obj) {
      if (obj instanceof SuspectUrlVersion) {
	SuspectUrlVersion suv = (SuspectUrlVersion) obj;
	return (url.equals(suv.getUrl()) && version == suv.getVersion());
      }
      return false;
    }
  }

  private Set<SuspectUrlVersion> suspectVersions =
    new HashSet<SuspectUrlVersion>();

  protected AuSuspectUrlVersions() {
  }

  /**
   * Return true if the version of the url has been marked suspect.
   * @return true if version of url has been marked suspect
   */
  public synchronized boolean isSuspect(String url, int version) {
    return suspectVersions.contains(new SuspectUrlVersion(url, version));
  }

  /**
   * Mark the version of the url as suspect
   */
  public synchronized void markAsSuspect(String url, int version) {
    if (isSuspect(url, version)) {
      throw new UnsupportedOperationException("Re-marking as suspect");
    }
    suspectVersions.add(new SuspectUrlVersion(url, version));
  }

  /**
   * Mark the version of the url as suspect
   */
  public synchronized void markAsSuspect(String url, int version,
					 String algorithm,
					 byte[] computedHash,
					 byte[] storedHash) {
    if (isSuspect(url, version)) {
      throw new UnsupportedOperationException("Re-marking as suspect");
    }
    suspectVersions.add(new SuspectUrlVersion(url, version, algorithm,
					      computedHash, storedHash));
  }

  /**
   * Mark the version of the url as suspect
   */
  public synchronized void markAsSuspect(String url, int version,
					 HashResult computedHash,
					 HashResult storedHash) {
    if (isSuspect(url, version)) {
      throw new UnsupportedOperationException("Re-marking as suspect");
    }
    suspectVersions.add(new SuspectUrlVersion(url, version,
					      computedHash, storedHash));
  }

  /** Return true if the set is empty */
  public synchronized boolean isEmpty() {
    return suspectVersions.isEmpty();
  }

  /** Return the collection of SuspectUrlVersion */
  public synchronized Collection<SuspectUrlVersion> getSuspectList() {
    return new ArrayList(suspectVersions);
  }

}