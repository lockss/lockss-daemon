/*
 * $Id: HashBlock.java,v 1.8 2007-05-09 10:34:16 smorabito Exp $
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

package org.lockss.hasher;
import java.security.*;
import java.util.*;

import org.lockss.plugin.*;
import org.lockss.util.LockssSerializable;

/** Result of a single-block V3 hash, passed to the ContentHasher's
 * HashBlockCallback.  A block contains multiple versions, which can be
 * iterated over or returned as an array.  This array is sorted and iterated
 * in the order <i>newest</i> to <i>oldest</i> version. */
public class HashBlock implements LockssSerializable {
  String url;
  TreeSet versions;
  long totalFilteredBytes = 0;

  public HashBlock(CachedUrl cu) {
    this.versions = new TreeSet();
    this.url = cu.getUrl();
  }

  public String getUrl() {
    return url;
  }

  public String toString() {
    return "[HBlock: " + getUrl() + "]";
  }
  
  public void addVersion(long unfilteredOffset,
                         long unfilteredLength,
                         long filteredOffset,
                         long filteredLength,
                         MessageDigest[] digests,
                         int repositoryVersion,
                         Throwable hashError) {
    versions.add(new HashBlock.Version(unfilteredOffset, unfilteredLength,
                                       filteredOffset, filteredLength,
                                       digests, repositoryVersion,
                                       hashError));
    totalFilteredBytes += filteredLength; 
  }
  
  public int size() {
    return versions.size();
  }
  
  public long getTotalFilteredBytes() {
    return totalFilteredBytes;
  }
  
  public Iterator versionIterator() {
    return versions.iterator();
  }
  
  public HashBlock.Version currentVersion() {
    if (versions.size() > 0) {
      return (HashBlock.Version)versions.first();
    } else {
      return null;
    }
  }
  
  public HashBlock.Version lastVersion() {
    if (versions.size() > 0) {
      return (HashBlock.Version)versions.last();
    } else {
      return null;
    }
  }
  
  public HashBlock.Version[] getVersions() {
    Version[] retVal =
      (Version[])versions.toArray(new Version[versions.size()]); 
    return retVal;
  }
   

  /**
   * Internal representation of a version of a hash block.  Natural sort
   * order is by repository version.
   *
   */
  public static class Version implements Comparable {
    long filteredOffset;
    long filteredLength;
    long unfilteredOffset;
    long unfilteredLength;
    boolean endOfFile;
    boolean lastVersion;
    byte[][] hashes;
    int repositoryVersion;
    Throwable hashError;

    public Version(long unfilteredOffset, long unfilteredLength,
                   long filteredOffset, long filteredLength,
                   MessageDigest[] digests, int repositoryVersion,
                   Throwable hashError) {
      this.unfilteredOffset = unfilteredOffset;
      this.unfilteredLength = unfilteredLength;
      this.filteredOffset = filteredOffset;
      this.filteredLength = filteredLength;
      this.repositoryVersion = repositoryVersion;
      this.hashError = hashError;
      setDigests(digests);
    }

    public void setFilteredOffset(long offset) {
      filteredOffset = offset;
    }

    public long getFilteredOffset() {
      return filteredOffset;
    }

    public void setFilteredLength(long length) {
      filteredLength = length;
    }

    public long getFilteredLength() {
      return filteredLength;
    }

    public void setUnfilteredOffset(long offset) {
      unfilteredOffset = offset;
    }

    public long getUnfilteredOffset() {
      return unfilteredOffset;
    }

    public void setUnfilteredLength(long length) {
      unfilteredLength = length;
    }

    public long getUnfilteredLength() {
      return unfilteredLength;
    }

    public void setDigests(MessageDigest[] digests) {
      int len = digests.length;
      hashes = new byte[len][];
      for (int i = 0; i < len; i++) {
        hashes[i] = digests[i].digest();
      }
    }

    public byte[][] getHashes() {
      return hashes;
    }
    
    public void setHashError(Throwable t) {
      hashError = t;
    }
    
    public Throwable getHashError() {
      return hashError;
    }

    public void setEndOfFile(boolean val) {
      endOfFile = val;
    }

    public boolean isEndOfFile() {
      return endOfFile;
    }

    public void setLastVersion(boolean val) {
      lastVersion = val;
    }

    public boolean isLastVersion() {
      return lastVersion;
    }

    public boolean isWholeFile() {
      return unfilteredOffset == 0 && endOfFile;
    }
    
    public int getRepositoryVersion() {
      return repositoryVersion;
    }
    
    // This mess is necessary because BlockHasher may add versions in
    // an arbitrary order, but we must be sure to iterate over them
    // in a predictable order.  The blocks are therefore held in a sorted
    // set, ordered by repository version number, from most recent to least
    // recent.

    public boolean equals(Object o) {
      if (!(o instanceof HashBlock.Version)) {
        return false;
      }
      if (this == o) {
        return true;
      }
      HashBlock.Version v = (HashBlock.Version)o;
      return (this.endOfFile == v.endOfFile &&
              this.lastVersion == v.lastVersion && 
              this.filteredLength == v.filteredLength &&
              this.filteredOffset == v.filteredOffset &&
              this.unfilteredLength == v.unfilteredLength &&
              this.unfilteredOffset == v.unfilteredOffset &&
              this.repositoryVersion == v.repositoryVersion);
    }
    
    public int hashCode() {
      int result = 17;
      result = result * 37 + repositoryVersion;
      result = result * 37 + (int)(unfilteredOffset ^ (unfilteredOffset >>> 32));
      result = result * 37 + (int)(unfilteredLength ^ (unfilteredLength >>> 32));
      result = result * 37 + (int)(filteredOffset ^ (filteredOffset >>> 32));
      result = result * 37 + (int)(filteredLength ^ (filteredLength >>> 32));
      result = result * 37 + (endOfFile ? 1 : 0);
      result = result * 37 + (lastVersion ? 1 : 0);
      return result;
    }
    
    public int compareTo(Object o) {
      final int BEFORE = -1;
      final int EQUAL = 0;
      final int AFTER = 1;
      
      if (!(o instanceof HashBlock.Version)) {
        throw new ClassCastException();
      }
      
      if (this == o) {
        return 0;
      }

      HashBlock.Version v = (HashBlock.Version)o;
      
      if (this.repositoryVersion > v.repositoryVersion) {
        return BEFORE;
      }
      
      if (this.repositoryVersion < v.repositoryVersion) {
        return AFTER;
      }
      
      return EQUAL;
    }
  }
}
