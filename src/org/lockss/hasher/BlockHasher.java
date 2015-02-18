/*
 * $Id$
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.InputStream;
import java.security.*;
import java.util.*;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.oro.text.regex.*;
import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.repository.AuSuspectUrlVersions;
import org.lockss.state.*;
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
  /** If true, enable local hash generation and verification */
  public static final String PARAM_ENABLE_LOCAL_HASH =
    Configuration.PREFIX + "blockHasher.enableLocalHash";
  public static final boolean DEFAULT_ENABLE_LOCAL_HASH = false;
  /** Algorithm to use for newly-generated stored local hashes */
  public static final String PARAM_LOCAL_HASH_ALGORITHM =
    Configuration.PREFIX + "blockHasher.localHashAlgorithm";
  // This MUST NOT be null - see PollManager.processConfigMacros()
  public static final String DEFAULT_LOCAL_HASH_ALGORITHM = "SHA-1";

  private static final Logger log = Logger.getLogger(BlockHasher.class);

  private int maxVersions = DEFAULT_HASH_MAX_VERSIONS;
  private boolean includeUrl = false;
  private boolean ignoreFilesOutsideCrawlSpec =
    DEFAULT_IGNORE_FILES_OUTSIDE_CRAWL_SPEC;
  private boolean isExcludeSuspectVersions = false;

  protected int excludedByPlugin = 0;
  protected int excludedByCrawlRule = 0;
  protected int excludedByIterator = 0;

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
  private HashMap<String,MessageDigest> localHashDigestMap = null;
  private HashedInputStream.Hasher currentVersionLocalHasher = null;
  private String localHashAlgorithm = null;
  private byte[] currentVersionStoredHash = null;
  private boolean enableLocalHash = DEFAULT_ENABLE_LOCAL_HASH;
  // private LocalHashHandler localHashHandler = null;
  private AuSuspectUrlVersions asuv = null;
  private CuIterator cuIter;
  private boolean needSaveSuspectUrlVersions = false;

  LocalHashResult lhr = null;

  private boolean isTrace = log.isDebug3();
   
  protected SubstanceChecker subChecker = null;

  public BlockHasher(CachedUrlSet cus,
		     MessageDigest[] digests,
		     byte[][]initByteArrays,
		     EventHandler cb) {
    this(cus, -1, digests, initByteArrays, cb);
  }
  
  /** Constructor that allows specifying number of CU versions to hash */
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
    enableLocalHash = config.getBoolean(PARAM_ENABLE_LOCAL_HASH,
					DEFAULT_ENABLE_LOCAL_HASH);
    if (enableLocalHash) {
//       localHashHandler = new DefaultLocalHashHandler();
      lhr = new LocalHashResult();
      localHashDigestMap = new HashMap<String,MessageDigest>();
      localHashAlgorithm =
	config.get(PARAM_LOCAL_HASH_ALGORITHM, DEFAULT_LOCAL_HASH_ALGORITHM);
    }
    
  }

  /** Tell the hasher whether to include the URL in the hash */
  public void setIncludeUrl(boolean val) {
    includeUrl = val;
  }

  /** Tell the hasher whether to exclude versions of CUs that are marked as
   * suspect due to checksum mismatch. */
  public void setExcludeSuspectVersions(boolean val) {
    isExcludeSuspectVersions = val;
  }

  /** Tell the hasher whether to exclude versions of CUs that are marked as
   * suspect due to checksum mismatch. */
  public boolean isExcludeSuspectVersions() {
    return isExcludeSuspectVersions;
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
    reportExclusions();
    // XXX need to account for number of parallel hashes
    cus.storeActualHashDuration(elapsed, err);
  }

  protected void reportExclusions() {
    if (excludedByPlugin != 0) {
      log.info(excludedByPlugin +
	       " files excluded by plugin poll-exclusion pattern");
    }
    if (excludedByIterator != 0) {
      log.info(excludedByIterator +
	       " files excluded by iterator (no longer in the crawl spec or globally excluded)");
    }
    if (excludedByCrawlRule != 0) {
      log.info(excludedByCrawlRule +
	       " files excluded (no longer in the crawl spec or globally excluded)");
    }
  }

  protected Iterator getIterator(CachedUrlSet cus) {
    return (cuIter = cus.getCuIterator());
  }

  @Override
  public boolean finished() {
    boolean ret = super.finished();
    if (ret) {
      excludedByIterator = cuIter.getExcludedCount();
    }
    return ret;
  }

  /** V3 hashes only content nodes.  NOTE: This routine also
   * implements substance checking. Subclasses overriding this method
   * must call {@code super.isIncluded()}.
   */
  protected boolean isIncluded(CachedUrlSetNode node) {
    String url = node.getUrl();
    if (crawlMgr != null && crawlMgr.isGloballyExcludedUrl(au, url)) {
      if (isTrace) log.debug3("isIncluded(" + url + "): globally excluded");
      excludedByCrawlRule++;
      return false;
    }
    if (ignoreFilesOutsideCrawlSpec && !au.shouldBeCached(url)) {
      excludedByCrawlRule++;
      if (isTrace) log.debug3("isIncluded(" + url + "): not in spec");
      return false;
    }
    if (excludeUrlPats != null && RegexpUtil.isMatch(url, excludeUrlPats)) {
      excludedByPlugin++;
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

    String useHashAlgorithm = startVersionLocalHash();

    // Account for nonce in hash count
    verBytesHashed = nonceLength();
    try {
      HashedInputStream.Hasher hasher = null;
      if (useHashAlgorithm != null) {
	hasher = getStreamHasher(useHashAlgorithm);
      }
      if (hasher == null) {
	is = getInputStream(curVer);
      } else {
	if (isTrace) log.debug3("Local hash for " + curVer.getUrl());
	currentVersionLocalHasher = hasher;
	is = getInputStream(curVer, currentVersionLocalHasher);
      }
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

  /** Return the algorithm to use for local hash for the current file, or
   * null */
  String startVersionLocalHash() {
    String useAlg = null;
    if (enableLocalHash) {
      // Default to configured hash alg, maybe change if file already has
      // hash in properties
      useAlg = localHashAlgorithm;
      currentVersionStoredHash = null;
      CIProperties verProps = curVer.getProperties();
      if (verProps.containsKey(CachedUrl.PROPERTY_CHECKSUM)) {
	// Parse the hash in the properties
	String cksumProp = verProps.getProperty(CachedUrl.PROPERTY_CHECKSUM);
	String algorithm = null;
	byte[] hash = null;
	try {
	  HashResult hr = HashResult.make(cksumProp);
	  algorithm = hr.getAlgorithm();
	  hash = hr.getBytes();
	  if (isTrace) log.debug3("Found current hash: " + hr);
	} catch (HashResult.IllegalByteArray ex) {
	  log.error(cksumProp + " badly formatted checksum");
	  return null;
	}
	// Does the current version have content?
	if (curVer.hasContent()) {
	  // Yes, we need to verify the checksum against the content
	  if (isTrace) {
	    log.debug3(curVer.getUrl() + ":" + curVer.getVersion() +
		       " checksum " + cksumProp);
	  }
	  useAlg = algorithm;
	  currentVersionStoredHash = hash;
	} else {
	  // Checksum but no content - record this version as suspect
	  log.error(curVer.getUrl() + ":" + curVer.getVersion() +
		    " checksum but no content");
	  mismatch(curVer, algorithm, null, hash);
	  useAlg = null;
	  currentVersionStoredHash = null;
	}
      } else {
	// No checksum in the properties - make one (
	if (isTrace) {
	  log.debug3(curVer.getUrl() + ":" + curVer.getVersion() +
		     " no checksum property");
	}
      }
    }
    return useAlg;
  }

  private HashedInputStream.Hasher getStreamHasher(String alg) {
    // Either to create or to verify a hash in the properties we need to
    // hash this version's unfiltered content with the specified algorithm.
    // Do we already have a suitable MessageDigest?
    MessageDigest dig = localHashDigestMap.get(alg);
    if (dig != null) {
      // Yes, reset it
      dig.reset();
    } else {
      // No, make one
      try {
	dig = MessageDigest.getInstance(alg);
	localHashDigestMap.put(alg, dig);
	if (isTrace) {
	  log.debug3("Created MessageDigest " + alg +
		     " for " + curVer.getUrl() + ":" + curVer.getVersion());
	}
      } catch (NoSuchAlgorithmException ex) {
	log.error(alg + " for " + curVer.getUrl() +
		  ":" + curVer.getVersion() + " throws " + ex);
	return null;
      }
    }
    return new HashedInputStream.Hasher(dig);
  }

  protected void startNode() {
    getCurrentCu();
    if (isTrace) log.debug3("startNode(" + curCu + ")");
    hblock = new HashBlock(curCu);    
    vix = -1;
    if (isExcludeSuspectVersions) {
      cuVersions = pruneSuspectVersions(curCu);
    } else {
      cuVersions = curCu.getCuVersions(getMaxVersions());
    }
  }
  
  private CachedUrl[] pruneSuspectVersions(CachedUrl cu) {
    ensureAuSuspectUrlVersions();
    if (asuv.isEmpty()) {
      return cu.getCuVersions(getMaxVersions());
    }
    CachedUrl[] vers = cu.getCuVersions();
    int outIx = 0;
    for (int inIx = 0; inIx < vers.length && outIx < maxVersions; inIx++) {
      CachedUrl verCu = vers[inIx];
      if (asuv.isSuspect(verCu.getUrl(), verCu.getVersion())) {
	skipped(verCu);
      } else {
        if (isTrace) log.debug3("Not suspect: " + verCu); 
	if (outIx != inIx) {
	  vers[outIx] = vers[inIx];
	}
	outIx++;
      }
    }
    if (isTrace) log.debug3("# versions: " + outIx);
    return (CachedUrl[])ArrayUtils.subarray(vers, 0, outIx);
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
    endVersionLocalHash();
    if (hblock != null) {
      hblock.addVersion(0, curVer.getContentSize(),
			0, verBytesRead, verBytesHashed,
                        peerDigests, curVer.getVersion(), hashError);
      return hblock.lastVersion();
    }
    return null;
  }

  private void endVersionLocalHash() {
    if (currentVersionLocalHasher != null &&
	currentVersionLocalHasher.isValid()) {
      // Local hash is enabled for this version
      byte[] hashOfContent = currentVersionLocalHasher.getDigest().digest();
      if (currentVersionStoredHash != null) {
	// This vesion has a stored hash in the properties
	if (isTrace) {
	  log.debug3("Computed: " + ByteArray.toHexString(hashOfContent) +
		     " Stored: " + ByteArray.toHexString(currentVersionStoredHash));
	}
	if (MessageDigest.isEqual(hashOfContent,
				  currentVersionStoredHash)) {
	  // hashes match - all is well
	  match(curVer);
	} else {
	  // Something bad happened to either the content or the hash
	  mismatch(curVer, localHashAlgorithm,
		   hashOfContent, currentVersionStoredHash);
	}
      } else {
	// No checksum property - create one
	missing(curVer, localHashAlgorithm, hashOfContent);
      }
      currentVersionLocalHasher = null;
      currentVersionStoredHash = null;
    }
  }

  /**
   * Local hash match
   * @param cu CachedUrl of the version for which mismatch was detected
   */
  protected void match(CachedUrl cu) {
    if (isTrace) {
      log.debug3(cu.getUrl() + ":" + cu.getVersion() +
		 " local hash OK");
    }
    lhr.match(cu.getUrl(), isHighestVersion(cu));
  }

  boolean isHighestVersion(CachedUrl cu) {
    return curCu.getVersion() == cu.getVersion();
  }

  /**
   * Local hash mismatch
   * @param cu CachedUrl of the version for which mismatch was detected
   * @param alg message digest algorithm in use
   * @param contentHash computed message digest of current content
   * @param storedHash message digest in version properties
   */
  protected void mismatch(CachedUrl cu, String alg, byte[] contentHash,
			  byte[] storedHash) {
    log.error(cu.getUrl() + ":" + cu.getVersion() +
	      " hash mismatch");
    markAsSuspect(cu, alg, contentHash, storedHash);
  }

  /**
   * Local hash missing
   * @param cu CachedUrl of the version without hash
   * @param alg String name of hash algorithm
   * @param hash byte array of computed hash
   */
  protected void missing(CachedUrl cu, String alg, byte[] hash) {
    String hashStr = alg + ":" + ByteArray.toHexString(hash);
    log.debug3("Storing checksum: " + hashStr);
    lhr.newlyHashed(cu.getUrl(), isHighestVersion(cu));
    try {
      cu.addProperty(CachedUrl.PROPERTY_CHECKSUM, hashStr);
    } catch (UnsupportedOperationException ex) {
      log.error("Storing checksum: " + hashStr + " threw " + ex);
    }
  }

  /** Skipped because already suspect */
  protected void skipped(CachedUrl cu) {
    if (isTrace) log.debug3("Skipped (already suspect): " + cu);
    lhr.skipped(cu.getUrl(), isHighestVersion(cu));
  }

  // Overridable for testing
  protected void markAsSuspect(CachedUrl cu, String alg,
				      byte[] contentHash,
				      byte[] storedHash) {
    ensureAuSuspectUrlVersions();
    if (log.isDebug2()) {
      log.debug2("isExcludeSuspectVersions: " + isExcludeSuspectVersions);
      log.debug2("isSuspect: " + cu + ": " + asuv.isSuspect(cu.getUrl(), cu.getVersion()));
    }
    if (isExcludeSuspectVersions ||
	!asuv.isSuspect(cu.getUrl(), cu.getVersion())) {
      asuv.markAsSuspect(cu.getUrl(), cu.getVersion(), alg,
			 contentHash, storedHash);
      needSaveSuspectUrlVersions = true;
      lhr.newlySuspect(cu.getUrl(), isHighestVersion(cu));
    }
  }

  private void ensureAuSuspectUrlVersions() {
    if (asuv == null) {
      asuv = AuUtil.getSuspectUrlVersions(cus.getArchivalUnit());
    }
  }

  @Override
  protected void done() {
    super.done();
    if (needSaveSuspectUrlVersions) {
      log.debug("Saving suspect URL versions");
      saveAuSuspectUrlVersions();
    }
    // Recompute numCurrentSuspectVersions here even if no new suspect
    // versions, to avoid dependence on long-term accuracy of incremental
    // changes.
    AuState aus = AuUtil.getAuState(cus.getArchivalUnit());
    aus.recomputeNumCurrentSuspectVersions();
  }

  private void saveAuSuspectUrlVersions() {
    try {
      AuUtil.saveSuspectUrlVersions(cus.getArchivalUnit(), asuv);
    } catch (SerializationException e) {
      // XXX ???
    }
  }

  public LocalHashResult getLocalHashResult() {
    if (log.isDebug2()) log.debug2("getLocalHashResult: " + lhr);
    return lhr;
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
