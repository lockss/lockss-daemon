/*
 * $Id: BlockHasher.java,v 1.27 2013-07-18 03:14:11 dshr Exp $
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
import org.lockss.repository.*;
import org.lockss.app.LockssDaemon;
import org.lockss.protocol.IdentityManager;
import org.lockss.protocol.PeerIdentity;
import org.lockss.poller.Poll;

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
  public static final String DEFAULT_LOCAL_HASH_ALGORITHM = "SHA-1";
  
  protected static Logger log = Logger.getLogger("BlockHasher");

  private int maxVersions = DEFAULT_HASH_MAX_VERSIONS;
  private boolean includeUrl = false;
  private boolean ignoreFilesOutsideCrawlSpec =
    DEFAULT_IGNORE_FILES_OUTSIDE_CRAWL_SPEC;
  protected int filesIgnored = 0;

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
  private MessageDigest currentVersionLocalDigest = null;
  private String localHashAlgorithm = null;
  private byte[] currentVersionStoredHash = null;
  private LocalHashHandler localHashHandler = null;
  private AuSuspectUrlVersions asuv = null;
  private int versionsMarkedSuspect = 0;
  private int versionsNewlyHashed = 0;
   
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
    if (config.getBoolean(PARAM_ENABLE_LOCAL_HASH,
			  DEFAULT_ENABLE_LOCAL_HASH)) {
      localHashHandler = new DefaultLocalHashHandler();
      localHashDigestMap = new HashMap<String,MessageDigest>();
      localHashAlgorithm =
	config.get(PARAM_LOCAL_HASH_ALGORITHM, DEFAULT_LOCAL_HASH_ALGORITHM);
    }
    
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

  /** V3 hashes only content nodes.  NOTE: This routine also
   * implements substance checking. Subclasses overriding this method
   * must call {@code super.isIncluded()}.
   */
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
    if (localHashDigestMap != null) {
      /*
       * Local Hash is enabled. Does this version have a hash
       * in the properties?
       */
      String useHashAlgorithm = localHashAlgorithm;
      currentVersionStoredHash = null;
      CIProperties verProps = curVer.getProperties();
      if (verProps.containsKey(CachedUrl.PROPERTY_CHECKSUM)) {
	// Parse the hash in the properties
	String cksumProp = verProps.getProperty(CachedUrl.PROPERTY_CHECKSUM);
	HashResult hr = null;
	String algorithm = null;
	byte[] hash = null;
	try {
	  hr = HashResult.make(cksumProp);
	  algorithm = hr.getAlgorithm();
	  hash = hr.getBytes();
	} catch (HashResult.IllegalByteArray ex) {
	    log.error(cksumProp + " badly formatted checksum");
	}
	// Does the current version have content?
	if (curVer.hasContent()) {
	  // Yes, we need to verify the checksum against the content
	  if (log.isDebug3()) {
	    log.debug3(curVer.getUrl() + ":" + curVer.getVersion() + " checksum " + cksumProp);
	  }
	  useHashAlgorithm = algorithm;
	  currentVersionStoredHash = hash;
	} else {
	  // Checksum but no content - record this version as suspect
	  log.error(curVer.getUrl() + ":" + curVer.getVersion() + " checksum but no content");
	  markAsSuspect(curVer, algorithm, null, hash);
	  useHashAlgorithm = null;
	  currentVersionStoredHash = null;
	}
      } else {
	// No checksum in the properties - make one
	if (log.isDebug3()) {
	  log.debug3(curVer.getUrl() + ":" + curVer.getVersion() + " no checksum property");
	}
      }
      if (useHashAlgorithm != null) {
	// Either to create or to verify a hash in the properties
	// we need to hash this version's unfiltered content with
	// useHashAlgorithm. Do we already have a suitable MessageDigest?
	currentVersionLocalDigest = localHashDigestMap.get(useHashAlgorithm);
	if (currentVersionLocalDigest == null) {
	  // No - make one
	  try {
	    currentVersionLocalDigest =
	      MessageDigest.getInstance(useHashAlgorithm);
	    localHashDigestMap.put(useHashAlgorithm, currentVersionLocalDigest);
	    if (log.isDebug3()) {
	      log.debug3("Created MessageDigest " + useHashAlgorithm +
			 " for " + curVer.getUrl() + ":" + curVer.getVersion());
	    }
	  } catch (NoSuchAlgorithmException ex) {
	    log.error(useHashAlgorithm + " for " + curVer.getUrl() +
		      ":" + curVer.getVersion() + " throws " + ex);
	    currentVersionLocalDigest = null;
	  }
	}
      }
    }
    // Account for nonce in hash count
    verBytesHashed = nonceLength();
    try {
      if (currentVersionLocalDigest == null) {
	log.debug3("No local hash for " + curVer.getUrl());
	is = getInputStream(curVer);
      } else {
	log.debug3("Local hash for " + curVer.getUrl());
	currentVersionLocalDigest.reset();
	is = getInputStream(curVer, currentVersionLocalDigest);
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

  protected void startNode() {
    getCurrentCu();
    if (isTrace) log.debug3("startNode(" + curCu + ")");
    hblock = new HashBlock(curCu);    
    vix = -1;
    int ix = 0;
    CachedUrl[] allVers = curCu.getCuVersions();
    CachedUrl[] tempVers = new CachedUrl[allVers.length];
    ensureAuSuspectUrlVersions();
    for (int i = 0; i < allVers.length && ix < maxVersions; i++) {
      if (!asuv.isSuspect(allVers[i].getUrl(), allVers[i].getVersion())) {
        if (log.isDebug3()) {
          log.debug3(allVers[i].getUrl() + " ver " + allVers[i].getVersion() + " not suspect");
        }
        tempVers[ix++] = allVers[i];
      } else {
        if (log.isDebug3()) {
          log.debug3(allVers[i].getUrl() + " ver " + allVers[i].getVersion() + " suspect");
        }
      }
    }
    if (log.isDebug3()) {
      log.debug3("# versions: " + ix);
    }
    cuVersions = new CachedUrl[ix];
    for (int i = 0; i < ix; i++) {
      cuVersions[i] = tempVers[i];
    }
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
    if (currentVersionLocalDigest != null) {
      // Local hash is enabled for this version
      byte[] hashOfContent = currentVersionLocalDigest.digest();
      if (currentVersionStoredHash != null) {
	// This vesion has a stored hash in the properties
	log.debug3("Computed: " + ByteArray.toHexString(hashOfContent) +
		   " Stored: " + ByteArray.toHexString(currentVersionStoredHash));
	if (MessageDigest.isEqual(hashOfContent,
				  currentVersionStoredHash)) {
	  // hashes match - all is well
	  localHashHandler.match(curVer);
	} else {
	  // Something bad happened to either the content or the hash
	  localHashHandler.mismatch(curVer, localHashAlgorithm,
				    hashOfContent, currentVersionStoredHash);
	}
      } else {
	// No checksum property - create one
	localHashHandler.missing(curVer, localHashAlgorithm, hashOfContent);
      }
      currentVersionLocalDigest = null;
      currentVersionStoredHash = null;
    }
    if (hblock != null) {
      hblock.addVersion(0, curVer.getContentSize(),
			0, verBytesRead, verBytesHashed,
                        peerDigests, curVer.getVersion(), hashError);
      return hblock.lastVersion();
    }
    return null;
  }

  private void markAsSuspect(CachedUrl curVer, String alg,
				      byte[] contentHash,
				      byte[] storedHash) {
    ensureAuSuspectUrlVersions();
    versionsMarkedSuspect++;
    asuv.markAsSuspect(curVer.getUrl(), curVer.getVersion(), alg,
		       contentHash, storedHash);
    // save on each change.  Should have option to save only at end of hash?
    saveAuSuspectUrlVersions();
  }

  private void ensureAuSuspectUrlVersions() {
    if (asuv == null) {
      asuv = AuUtil.getSuspectUrlVersions(cus.getArchivalUnit());
    }
  }

  private void saveAuSuspectUrlVersions() {
    try {
      AuUtil.saveSuspectUrlVersions(cus.getArchivalUnit(), asuv);
    } catch (SerializationException e) {
      // XXX ???
    }
  }

  public void signalLocalHashResult(ArchivalUnit au) {
    if (localHashDigestMap != null && versionsNewlyHashed == 0) {
      /* every URL was verified against previous hash */
      IdentityManager idmgr =
	LockssDaemon.getLockssDaemon().getIdentityManager();
      PeerIdentity pid =
	idmgr.getLocalPeerIdentity(Poll.V3_PROTOCOL);
      if (versionsMarkedSuspect > 0) {
	/* Some versions had problems */
	idmgr.signalDisagreed(pid, au);
      } else {
	/* All versions OK */
	idmgr.signalAgreed(pid, au);
      }
    }
  }

  public static interface LocalHashHandler {
    /**
     * Local hash match
     * @param curVer CachedUrl of the version for which mismatch was detected
     */
    public void match(CachedUrl curVer);
    /**
     * Local hash mismatch
     * @param curVer CachedUrl of the version for which mismatch was detected
     * @param alg message digest algorithm in use
     * @param contentHash computed message digest of current content
     * @param storedHash message digest in version properties
     */
    public void mismatch(CachedUrl curVer, String alg, byte[] contentHash,
			 byte[] storedHash);
    /**
     * Local hash missing
     * @param curVer CachedUrl of the version without hash
     * @param alg String name of hash algorithm
     * @param hash byte array of computed hash
     */
    public void missing(CachedUrl curVer, String alg, byte[] hash);
  }

  public class DefaultLocalHashHandler implements LocalHashHandler {
    public DefaultLocalHashHandler() {}
    /**
     * Local hash match
     * @param curVer CachedUrl of the version for which mismatch was detected
     */
    public void match(CachedUrl curVer) {
      if (log.isDebug3()) {
	log.debug3(curVer.getUrl() + ":" + curVer.getVersion() +
		   " local hash OK");
      }
    }
    /**
     * Local hash mismatch
     * @param curVer CachedUrl of the version for which mismatch was detected
     * @param alg message digest algorithm in use
     * @param contentHash computed message digest of current content
     * @param storedHash message digest in version properties
     */
    public void mismatch(CachedUrl curVer, String alg, byte[] contentHash,
			 byte[] storedHash) {
      log.error(curVer.getUrl() + ":" + curVer.getVersion() +
		" hash mismatch");
      markAsSuspect(curVer, alg, contentHash, storedHash);
    }
    /**
     * Local hash missing
     * @param curVer CachedUrl of the version without hash
     * @param alg String name of hash algorithm
     * @param hash byte array of computed hash
     */
    public void missing(CachedUrl curVer, String alg, byte[] hash) {
      String hashStr = alg + ":" + ByteArray.toHexString(hash);
      log.debug3("Storing checksum: " + hashStr);
      versionsNewlyHashed++;
      try {
	curVer.addProperty(CachedUrl.PROPERTY_CHECKSUM, hashStr);
      } catch (UnsupportedOperationException ex) {
	log.error("Storing checksum: " + hashStr + " threw " + ex);
      }
    }
  }

  public void setLocalHashHandler(LocalHashHandler h) {
    localHashHandler = h;
  }

  public LocalHashHandler getLocalHashHandler() {
    return localHashHandler;
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
