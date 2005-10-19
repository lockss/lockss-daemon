/*
 * $Id: BlockHasher.java,v 1.2.2.1 2005-10-19 00:24:34 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * General class to handle content hashing
 */
public class BlockHasher extends GenericHasher {
  protected static Logger log = Logger.getLogger("BlockHasher");

  private byte[] contentBytes = null;
  private HashBlock hblock;
  MessageDigest[] digests;
  byte[][]initByteArrays;
  EventHandler cb;
  private InputStream is = null;
  private long nodeBytesHashed;

  public BlockHasher(CachedUrlSet cus, MessageDigest[] digests,
		     byte[][]initByteArrays, EventHandler cb) {
    super(cus);
    if (digests == null) throw new NullPointerException("null digests");
    if (initByteArrays == null)
      throw new NullPointerException("null initByteArrays");
    if (digests.length != initByteArrays.length)
      throw new
	IllegalArgumentException("Unequal length digests and initByteArrays");
    this.digests = digests;
    this.initByteArrays = initByteArrays;
    this.cb = cb;
  }

  private String ts = null;
  public String typeString() {
    if (ts == null) {
      ts = "B(" + digests.length + ")";
    }
    return ts;
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
    // XXX need to account for number of parallel hashes
    cus.storeActualHashDuration(elapsed, err);
  }

  protected Iterator getIterator(CachedUrlSet cus) {
    return cus.contentHashIterator();
  }

  /** V3 hashes only content nodes */
  protected boolean isIncluded(CachedUrlSetNode node) {
    if (isTrace)
      log.debug3("isIncluded(" + node.getUrl() + "): " + node.hasContent());
    return node.hasContent();
  }

  public MessageDigest[] getDigests() {
    return digests;
  }

  protected int hashNodeUpToNumBytes(int numBytes)
      throws IOException {
    getCurrentCu();
    int remaining = numBytes;
    int bytesHashed = 0;
    if (isTrace) log.debug3("hashing content");
    if (is == null) {
      if (!curCu.hasContent()) {
	// shouldn't happen
	log.warning("No content: " + curCu);
	endOfNode();
	return 0;
      }
      startNode();
    }
    if (contentBytes == null || contentBytes.length < (remaining)) {
      contentBytes = new byte[numBytes + 100];
    }
    while (remaining > 0) {
      int bytesRead = is.read(contentBytes, 0, remaining);
      if (isTrace) log.debug3("Read "+bytesRead+" bytes from input stream");
      if (bytesRead >= 0) {
	updateDigests(contentBytes, bytesRead);
	bytesHashed += bytesRead;
	nodeBytesHashed += bytesRead;
	remaining -= bytesRead;
      } else {
	if (isTrace) log.debug3("done hashing content: "+curCu);
	endOfNode();
	is.close();
	is = null;
	break;
      }
    }
    if (isTrace) log.debug3(bytesHashed+" bytes hashed in this step");
    return bytesHashed;
  }

  public void abortHash() {
    IOUtil.safeClose(is);
    is = null;
    super.abortHash();
  }

  private void updateDigests(byte[] content, int len) {
    for (int ix = 0; ix < digests.length; ix++) {
      if (isTrace) log.debug3("Updating digest " + ix + ", len = " + len);
      digests[ix].update(content, 0, len);
    }
  }

  private void initDigests() {
    for (int ix = 0; ix < digests.length; ix++) {
      byte[] initArr = initByteArrays[ix];
      int len;
      if (initArr != null && (len = initArr.length) != 0) {
	digests[ix].update(initArr, 0, len);
      }
    }
  }

  private void resetDigests() {
    for (int ix = 0; ix < digests.length; ix++) {
      digests[ix].reset();
    }
  }

  protected void startNode() {
    if (isTrace) log.debug3("opening "+curCu+" for hashing");
    is = curCu.openForHashing();
    hblock = new HashBlock(curCu);
    hblock.setUnfilteredOffset(0);
    hblock.setFilteredOffset(0);
    hblock.setUnfilteredLength(curCu.getContentSize());
    nodeBytesHashed = 0;
    initDigests();
  }

  protected void endOfNode() {
    super.endOfNode();
    if (hblock != null) {
      hblock.setDigests(digests);
      hblock.setFilteredLength(nodeBytesHashed);
      if (cb != null) cb.blockDone(hblock);
      hblock = null;
      resetDigests();
    }
  }

  public interface EventHandler {
    /** Called at the completion of each hash block (file or part of file)
     * with the hash results.  The digests in the HashBlock will be reset
     * when this method returns, so it must read the current digest values
     * before returning
     */
    void blockDone(HashBlock hblock);
  }

}
