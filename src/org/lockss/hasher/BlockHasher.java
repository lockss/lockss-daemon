/*
 * $Id: BlockHasher.java,v 1.21 2013-03-19 04:26:15 tlipkis Exp $
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.oro.text.regex.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.state.SubstanceChecker;
import org.lockss.util.*;

/**
 * General class to handle content hashing
 */
public class BlockHasher extends GenericHasher {
  
  /** The maximum number of versions of content to hash in any CachedUrl. */
  public static final String PARAM_HASH_MAX_VERSIONS = 
    Configuration.PREFIX + "blockHasher.maxVersions";
  public static final int DEFAULT_HASH_MAX_VERSIONS = 5;
  
  /** If true, files not within the crawl spec are ignored by polls.  (Can
   * happen if spec has changed since files were collected.)  */
  public static final String PARAM_IGNORE_FILES_OUTSIDE_CRAWL_SPEC = 
    Configuration.PREFIX + "blockHasher.ignoreFilesOutsideCrawlSpec";
  public static final boolean DEFAULT_IGNORE_FILES_OUTSIDE_CRAWL_SPEC = false;

  protected static Logger log = Logger.getLogger("BlockHasher");

  private int maxVersions = DEFAULT_HASH_MAX_VERSIONS;
  private boolean includeUrl = false;
  private boolean ignoreFilesOutsideCrawlSpec =
    DEFAULT_IGNORE_FILES_OUTSIDE_CRAWL_SPEC;
  private int filesIgnored = 0;

  protected MessageDigest[] initialDigests;
  protected byte[][] initByteArrays;
  EventHandler cb;

  private HashBlock hblock;
  private byte[] contentBytes = null;

  CachedUrl[] cuVersions;
  CachedUrl curVer;
  int vix = -1;
  private long verBytesRead;
  private long verBytesHashed;
  InputStream is = null;
  MessageDigest[] peerDigests;
  List<Pattern> excludeUrlPats;

  protected SubstanceChecker subChecker = null;

  public BlockHasher(CachedUrlSet cus,
		     MessageDigest[] digests,
		     byte[][]initByteArrays,
		     EventHandler cb) {
    this(cus, -1, digests, initByteArrays, cb);
  }
  
  /** Constuctor that allows specifying number of CU versions to hash */
  public BlockHasher(CachedUrlSet cus,
		     int maxVersions,
		     MessageDigest[] digests,
		     byte[][]initByteArrays,
		     EventHandler cb) {
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
    this.peerDigests = new MessageDigest[digests.length];
    this.initByteArrays = initByteArrays;
    this.cb = cb;
    setConfig();
    if (maxVersions > 0) {
      this.maxVersions = maxVersions;
    }
    initDigests();
    try {
      excludeUrlPats = cus.getArchivalUnit().makeExcludeUrlsFromPollsPatterns();
    } catch (NullPointerException e) {
      log.warning("No AU, thus no excludeUrlPats: " + cus);
    } catch (ArchivalUnit.ConfigurationException e) {
      log.warning("Error building excludeUrlPats", e);
    }
  }

  private void setConfig() {
    Configuration config = ConfigManager.getCurrentConfig();
    maxVersions = config.getInt(PARAM_HASH_MAX_VERSIONS,
				DEFAULT_HASH_MAX_VERSIONS);
    ignoreFilesOutsideCrawlSpec =
      config.getBoolean(PARAM_IGNORE_FILES_OUTSIDE_CRAWL_SPEC,
			DEFAULT_IGNORE_FILES_OUTSIDE_CRAWL_SPEC);
  }

  /** Tell the hasher whether to include the URL in the hash */
  public void setIncludeUrl(boolean val) {
    includeUrl = val;
  }

  /** Tell the BlockHasher to run all the hashed URLs through a substance
   * checker */
  public void setSubstanceChecker(SubstanceChecker subChecker) {
    this.subChecker = subChecker;
  }

  private String ts = null;
  public String typeString() {
    if (ts == null) {
      ts = "B(" + initialDigests.length + ")";
    }
    return ts;
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
    if (filesIgnored != 0) {
      log.info(filesIgnored +
	       " files ignored because they're no longer in the crawl spec");
    }
    // XXX need to account for number of parallel hashes
    cus.storeActualHashDuration(elapsed, err);
  }

  protected Iterator getIterator(CachedUrlSet cus) {
    return cus.contentHashIterator();
  }

  /** V3 hashes only content nodes */
  protected boolean isIncluded(CachedUrlSetNode node) {
    String url = node.getUrl();
    if (crawlMgr != null && crawlMgr.isGloballyExcludedUrl(au, url)) {
      if (isTrace) log.debug3("isIncluded(" + url + "): globally excluded");
      return false;
    }
    if (ignoreFilesOutsideCrawlSpec && !au.shouldBeCached(url)) {
      filesIgnored++;
      if (isTrace) log.debug3("isIncluded(" + url + "): not in spec");
      return false;
    }
    if (excludeUrlPats != null && isMatch(url, excludeUrlPats)) {
      filesIgnored++;
      if (isTrace) log.debug3("isIncluded(" + url + "): excluded by plugin");
      return false;
    }
    boolean res = node.hasContent();
    if (isTrace) {
      log.debug3("isIncluded(" + url + "): " + (res ? "true" : "no content"));
    }
    if (res && subChecker != null) {
      CachedUrl cu = AuUtil.getCu(node);
      try {
	subChecker.checkSubstance(cu);
      } finally {
	AuUtil.safeRelease(cu);
      }
    }
    return res;
  }

  boolean isMatch(String url, List<Pattern> pats) {
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    for (Pattern pat : pats) {
      if (matcher.contains(url, pat)) {
	return true;
      }
    }
    return false;
  }

  public MessageDigest[] getDigests() {
    throw
      new UnsupportedOperationException("getDigests() has no meaning for V3");
  }
  
  public int getMaxVersions() {
    return maxVersions;
  }
  
  // return false iff no more versions
  protected boolean startVersion() {
    if (++vix >= cuVersions.length) {
      return false;
    }
    curVer = cuVersions[vix];
    verBytesRead = 0;
    cloneDigests();
    // Account for nonce in hash count
    verBytesHashed = nonceLength();
    try {
      is = getInputStream(curVer);
      return true;
    } catch (OutOfMemoryError e) {
      // Log and rethrow OutOfMemoryError so can see what file caused it.
      // No stack trace, as it's uninformative and reduces chance message
      // will actually be logged.
      log.error("OutOfMemoryError opening CU for hashing: " + curVer);
      throw e;
    } catch (RuntimeException e) {
      log.error("Error opening CU for hashing: " + curVer, e);
      endVersion(e);
      return true;
    }
  }

  protected void startNode() {
    getCurrentCu();
    if (isTrace) log.debug3("startNode(" + curCu + ")");
    hblock = new HashBlock(curCu);    
    vix = -1;
    cuVersions = curCu.getCuVersions(getMaxVersions());
  }
  
  protected void endOfNode() {
    super.endOfNode();
    if (hblock != null) {
      if (cb != null) cb.blockDone(hblock);
      hblock = null;
    }
  }
  
  protected long hashNodeUpToNumBytes(int numBytes) {
    if (isTrace) log.debug3("hashing content");
    int remaining = numBytes; 
    long bytesHashed = 0;

    if (is == null) {
      if (curCu == null) {
	startNode();
      }
      if (!startVersion()) {
	endOfNode();
	return 0;
      }
      if (includeUrl) {
	byte [] nameBytes = curCu.getUrl().getBytes();
	int hashed = updateDigests(nameBytes, nameBytes.length);
	bytesHashed += hashed;
      }
    }
    if (contentBytes == null || contentBytes.length < remaining) {
      contentBytes = new byte[numBytes + 100];
    }
    while (remaining > 0 && is != null) {
      HashBlock.Version hbVersion = null;
      try {
	int bytesRead = is.read(contentBytes, 0, remaining);
	if (isTrace) log.debug3("Read "+bytesRead+" bytes from input stream");
	if (bytesRead >= 0) {
	  int hashed = updateDigests(contentBytes, bytesRead);
	  bytesHashed += hashed;
	  verBytesHashed += hashed;
	  verBytesRead += bytesRead;
	  remaining -= bytesRead;
	} else {
	  // end of file
	  if (isTrace) log.debug3("done hashing version ix "+ vix +
				  ", bytesHashed: " + bytesHashed);
	  is.close();
	  is = null;
	  hbVersion = endVersion(null);
	}
      } catch (OutOfMemoryError e) {
	// Log and rethrow OutOfMemoryError so can see what file caused it.
	// No stack trace, as it's uninformative and reduces chance message
	// will actually be logged.
	log.error("OutOfMemoryError opening CU for hashing: " + curVer);
	throw e;
      } catch (Exception e) {
	log.error("Error hashing CU: " + curVer, e);
	if (hbVersion == null) {
	  endVersion(e);
	} else {
	  hbVersion.setHashError(e);
	}
	IOUtil.safeClose(is);
	is = null;
      }
    }
    if (is == null && vix == (cuVersions.length - 1)) {
      endOfNode();
    }
    if (isTrace) log.debug3(bytesHashed+" bytes hashed in this step");
    return bytesHashed;
  }

  public void abortHash() {
    if (is != null) {
      IOUtil.safeClose(is);
      is = null;
    }
    super.abortHash();
  }

  private int updateDigests(byte[] content, int len) {
    for (int ix = 0; ix < peerDigests.length; ix++) {
      if (isTrace) log.debug3("Updating digest " + ix + ", len = " + len);
      peerDigests[ix].update(content, 0, len);
    }
    return len * peerDigests.length;
  }

  private int nonceLength = -1;

  int nonceLength() {
    if (nonceLength < 0) {
      int res = 0;
      for (byte[] nonce : initByteArrays) {
	if (nonce != null) {
	  res += nonce.length;
	}
      }
      nonceLength = res;
    }
    return nonceLength;
  }

  private void initDigests() {
    for (int ix = 0; ix < initialDigests.length; ix++) {
      byte[] initArr = initByteArrays[ix];
      int len;
      if (initArr != null && (len = initArr.length) != 0) {
	initialDigests[ix].update(initArr, 0, len);
      }
    }
  }

  // todo(bhayes): To clone 100M SHA-1 digests takes about 20 seconds
  // on my desktop; to just reinitialize from the byte[40] takes about
  // 30 seconds. Given the reach the code goes through to ensure that
  // digests can be cloned, I'm not convinced it's worth it.
  private void cloneDigests() {
    // Clone initialDigests into peerDigests.
    try {
      for (int ix = 0; ix < initialDigests.length; ix++) {
	peerDigests[ix] = (MessageDigest)initialDigests[ix].clone();
      }
    } catch (CloneNotSupportedException ex) {
      // Should *never* happen.  Should be caught in the constructor.
      log.critical("Caught CloneNotSupportedException!", ex);
    }
  }

  protected HashBlock.Version endVersion(Throwable hashError) {
    if (hblock != null) {
      hblock.addVersion(0, curVer.getContentSize(),
			0, verBytesRead, verBytesHashed,
                        peerDigests, curVer.getVersion(), hashError);
      return hblock.lastVersion();
    }
    return null;
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
