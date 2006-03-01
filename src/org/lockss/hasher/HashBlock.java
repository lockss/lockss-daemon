/*
 * $Id: HashBlock.java,v 1.3 2006-03-01 02:50:14 smorabito Exp $
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

import org.lockss.plugin.*;
import org.lockss.util.LockssSerializable;

/** Result of a single-block V3 hash, passed to the ContentHasher's
 * HashBlockCallback */
public class HashBlock implements LockssSerializable {
  String url;
  long filteredOffset;
  long filteredLength;
  long unfilteredOffset;
  long unfilteredLength;
  boolean endOfFile;
  boolean lastVersion;
  byte[][] hashes;

  public HashBlock(CachedUrl cu) {
    this.url = cu.getUrl();
  }
  
  public String getUrl() {
    return url;
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
    for (int i = 0; i < digests.length; i++) {
      hashes[i] = digests[i].digest();
    }
  }
  
  public byte[][] getHashes() {
    return hashes;
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

  public String toString() {
    return "[HBlock: " + getUrl() + "]";
  }

}
