/*
 * $Id: BlockHasher.java,v 1.9 2007-05-09 10:34:16 smorabito Exp $
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

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.util.*;

/**
 * General class to handle content hashing
 */
public class BlockHasher extends GenericHasher {
  
  /** The maximum number of versions of content to hash in any CachedUrl. */
  public static final String PARAM_HASH_MAX_VERSIONS = 
    Configuration.PREFIX + "blockHasher.maxVersions";
  public static final int DEFAULT_HASH_MAX_VERSIONS = 5;
  
  protected static Logger log = Logger.getLogger("BlockHasher");

  private HashBlock hblock;
  byte[][] initByteArrays;
  // 2D array of content buffers, one array per version.
  private byte[][] contentBytes = null;
  // The initial digests passed in at construction time.
  MessageDigest[] initialDigests;
  // 2D Array of message digests, one array per version 
  MessageDigest[][] perVersionDigests;
  // Event handler to be called at the end of every block.
  EventHandler cb;
  // Array of input streams, one per version.
  private InputStream[] is = null;
  // Total bytes hashed per version of this node.  Used for filtered
  // content length
  private long[] versionBytesHashed;
  private int maxVersions = DEFAULT_HASH_MAX_VERSIONS;
  // A counter of versions that have not yet finished hashing.  Used in the
  // per-version loop in hashNodeUpToNumBytes
  private int remainingVersions;

  public BlockHasher(CachedUrlSet cus, MessageDigest[] digests,
		     byte[][]initByteArrays, EventHandler cb) {
    super(cus);
    if (digests == null) throw new NullPointerException("null digests");
    if (initByteArrays == null)
      throw new NullPointerException("null initByteArrays");
    if (digests.length != initByteArrays.length)
      throw new
	IllegalArgumentException("Unequal length digests and initByteArrays");
    
    // BlockHasher only supports cloneable message digest algorithms.
    // Unfortunately, the only way to check this is by trying to clone them
    // and catching CloneNotSupportedException.
    for (int ix = 0; ix < digests.length; ix++) {
      try {
        digests[ix].clone();
      } catch (CloneNotSupportedException ex) {
        log.critical("Uncloneable MessageDigests were passed to " +
                     "BlockHasher at construction time!");
        throw new IllegalArgumentException("Uncloneable MessageDigests " +
                                           "may not be used with BlockHasher.");
      }
    }
    
    this.initialDigests = digests;
    this.initByteArrays = initByteArrays;
    this.cb = cb;
    setConfig();
  }
  
  /** Constuctor that allows specifying number of CU versions to hash */
  public BlockHasher(CachedUrlSet cus, int maxVersions,
		     MessageDigest[] digests,
		     byte[][]initByteArrays, EventHandler cb) {
    this(cus, digests, initByteArrays, cb);
    this.maxVersions = maxVersions;
  }

  private void setConfig() {
    maxVersions = CurrentConfig.getIntParam(PARAM_HASH_MAX_VERSIONS,
                                            DEFAULT_HASH_MAX_VERSIONS);
  }

  private String ts = null;
  public String typeString() {
    if (ts == null) {
      ts = "B(" + initialDigests.length + ")";
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
    return initialDigests;
  }
  
  /**
   * Return all the digests used by this hasher.
   * 
   * @return Array of message digest arrays, one per version of the content.
   */
  public MessageDigest[][] getAllDigests() {
    return perVersionDigests;
  }

  public int getMaxVersions() {
    return maxVersions;
  }
  
  protected int hashNodeUpToNumBytes(int numBytes)
      throws IOException {
    getCurrentCu();
    int bytesHashed = 0;
    if (isTrace) log.debug3("hashing content");
    if (is == null) { // end of block
      if (!curCu.hasContent()) {
        // shouldn't happen
        log.warning("No content: " + curCu);
        endOfNode();
        return 0;
      }
      startNode(curCu.getCuVersions(getMaxVersions()));
    }
    CachedUrl[] cuVersions = curCu.getCuVersions(getMaxVersions());
    contentBytes = new byte[cuVersions.length][];

    outer:
    for (int ix = 0; ix < cuVersions.length; ix++) {
      CachedUrl version = cuVersions[ix];
      int remaining = numBytes; 
      if (contentBytes[ix] == null || contentBytes[ix].length < (remaining)) {
        contentBytes[ix] = new byte[numBytes + 100];
      }
      while (remaining > 0 && is[ix] != null) {
        HashBlock.Version hbVersion = null;
        try {
          int bytesRead = is[ix].read(contentBytes[ix], 0, remaining);
          if (isTrace) log.debug3("Read "+bytesRead+" bytes from input stream");
          if (bytesRead >= 0) {
            int hashed = updateDigests(ix, contentBytes[ix], bytesRead);
            versionBytesHashed[ix] += bytesRead;
            bytesHashed += hashed;
            remaining -= bytesRead;
          } else {
            // This version has no more content to hash.
            if (isTrace) log.debug3("done hashing content: "+version);
            is[ix].close();
            is[ix] = null;
            hbVersion = endVersion(ix, version, versionBytesHashed[ix]);
            if (--remainingVersions == 0) {
              is = null;
              endOfNode();
              break outer;
            }
          }
        } catch (Throwable t) {
          log.error("Caught exception while trying to hash", t);
          if (hbVersion == null) {
            endVersion(ix, version, versionBytesHashed[ix], t);
            if (--remainingVersions == 0) {
              is = null;
              endOfNode();
              break outer;
            }
          } else {
            hbVersion.setHashError(t);
          }
          continue outer;
        }
      }
    }

    
    if (isTrace) log.debug3(bytesHashed+" bytes hashed in this step");
    return bytesHashed;
  }

  public void abortHash() {
    if (is != null) {
      for (int ix = 0; ix < is.length; ix++) {
	IOUtil.safeClose(is[ix]);
	is[ix] = null;
      }
      is = null;
    }
    super.abortHash();
  }

  private int updateDigests(int index, byte[] content, int len) {
    int nhashes = perVersionDigests[index].length;
    for (int ix = 0; ix < nhashes; ix++) {
      if (isTrace) log.debug3("Updating digest " + ix + ", len = " + len);
      perVersionDigests[index][ix].update(content, 0, len);
    }
    log.debug3("updateDigests(" + index + ",, " + len + "): " + len * nhashes);
    return len * nhashes;
  }

  private void initDigests(int size) {
    for (int ix = 0; ix < initialDigests.length; ix++) {
      byte[] initArr = initByteArrays[ix];
      int len;
      if (initArr != null && (len = initArr.length) != 0) {
	initialDigests[ix].update(initArr, 0, len);
      }
    }
    // Clone initialDigests into each slot of perVersionDigests.
    perVersionDigests = new MessageDigest[size][];
    try {
      for (int versionIdx = 0; versionIdx < size; versionIdx++) {
        perVersionDigests[versionIdx] = new MessageDigest[initialDigests.length];
        for (int digestIdx = 0; digestIdx < initialDigests.length; digestIdx++) {
          perVersionDigests[versionIdx][digestIdx] =
            (MessageDigest)initialDigests[digestIdx].clone();
        }
      }
    } catch (CloneNotSupportedException ex) {
      // Should *never* happen.  Should be caught in the constructor.
      log.critical("Caught CloneNotSupportedException!", ex);
    }
  }

  private void resetDigests() {
    for (int ix = 0; ix < initialDigests.length; ix++) {
      initialDigests[ix].reset();
    }
    perVersionDigests = new MessageDigest[perVersionDigests.length][];
    try {
      for (int versionIdx = 0; versionIdx < perVersionDigests.length; versionIdx++) {
        perVersionDigests[versionIdx] = new MessageDigest[initialDigests.length];
        for (int digestIdx = 0; digestIdx < initialDigests.length; digestIdx++) {
          perVersionDigests[versionIdx][digestIdx] =
            (MessageDigest)initialDigests[digestIdx].clone();
        }
      } 
    } catch (CloneNotSupportedException ex) {
      // Should *never* happen.  Should be caught in the constructor.
      log.critical("Caught CloneNotSupportedException!", ex);
    }
  }

  protected void startNode(CachedUrl[] versions) {
    hblock = new HashBlock(curCu);    
    if (isTrace) log.debug3("opening "+curCu+" for hashing");
    is = new InputStream[versions.length];
    for (int ix = 0; ix < versions.length; ix++) {
      is[ix] = getInputStream(versions[ix]);
    }
    remainingVersions = versions.length;
    versionBytesHashed = new long[remainingVersions];
    initDigests(versions.length);
  }
  
  protected InputStream getInputStream(CachedUrl cu) {
    return cu.openForHashing();
  }

  protected void endOfNode() {
    super.endOfNode();
    if (hblock != null) {
      if (cb != null) cb.blockDone(hblock);
      hblock = null;
      resetDigests();
    }
  }
  
  protected HashBlock.Version endVersion(int index, CachedUrl version,
                                         long bytesHashed, 
                                         Throwable hashError) {
    if (hblock != null) {
      hblock.addVersion(0, version.getContentSize(), 0, bytesHashed,
                        perVersionDigests[index], version.getVersion(),
                        hashError);
      return hblock.lastVersion();
    }
    return null;
  }

  protected HashBlock.Version endVersion(int index, CachedUrl version, long bytesHashed) {
    return endVersion(index, version, bytesHashed, null);
  }

  public interface EventHandler {
    /** Called at the completion of each hash block (file or part of file)
     * with the hash results.  The digests in the HashBlock may be reused
     * when this method returns, so the current digest values must be read
     * before returning
     */
    void blockDone(HashBlock hblock);
  }

}
