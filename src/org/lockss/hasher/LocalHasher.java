/*
 * $Id: LocalHasher.java,v 1.1.2.3 2013-05-19 21:11:12 dshr Exp $
 */

/*

Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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
import java.security.*;
import java.util.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
import org.lockss.daemon.*;
import org.lockss.config.*;

/** Hash a CachedUrlSet or an AU and, if a URL has a pre-existing hash
 * stored in the properties, compare them and flag any mis-matches. If
 * there is no hash, store one in the properties.
 */
public class LocalHasher {
  protected static Logger logger = Logger.getLogger("LocalHasher");

  private String checksumAlgorithm = null;

  private int nbytes = 1000;
  private long bytesHashed = 0;
  private int filesHashed = 0;
  private long startTime = 0;
  private long elapsedTime;
  private Callback callback;

  public LocalHasher(Callback cb) {
    checksumAlgorithm =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    startTime = System.currentTimeMillis();
    callback = cb;
  }

  public long getBytesHashed() {
    return bytesHashed;
  }

  public int getFilesHashed() {
    return filesHashed;
  }

  public long getElapsedTime() {
    return elapsedTime;
  }

  /** Do a local hash of the AU, creating or checking local hashes */
  public void doLocalHash(ArchivalUnit au)
      throws IOException {
    doLocalHash(au.getAuCachedUrlSet());
  }

  /** Do a local hash of the CUS, creating or checking local hashes */
  public void doLocalHash(CachedUrlSet cus) throws IOException {
    Iterator iterCUS = cus.contentHashIterator();
    while (iterCUS.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode)iterCUS.next();
      switch (node.getType()) {
      case CachedUrlSetNode.TYPE_CACHED_URL:
	doLocalHashCachedUrlNode(node);
	elapsedTime = System.currentTimeMillis() - startTime;
	break;
      case CachedUrlSetNode.TYPE_CACHED_URL_SET:
	doLocalHash((CachedUrlSet) node);
	break;
      default:
	throw new IllegalArgumentException("Bad CachedUrlSetNode type: "
					   + node.getType());
      }
    }
  }

  public void doLocalHashCachedUrlNode(CachedUrlSetNode node)
    throws IOException {
    CachedUrl cu = (CachedUrl) node;
    if (node.hasContent()) {
      logger.debug3(cu.getUrl() + " has content");
      doLocalHashNode(cu);
    } else {
      logger.debug3(cu.getUrl() + " has no content");
      doLocalEmptyNode(cu);
    }
  }

  void doLocalEmptyNode(CachedUrl cu) {
    byte[] checksum = cu.getChecksum();
    if (checksum != null) {
      logger.error(cu.getUrl() + " no content but a checksum " +
		   ByteArray.toHexString(checksum));
      if (callback != null) {
	callback.hashButNoContent(cu);
      }
    }
  }

  void doLocalHashNode(CachedUrl cu) throws IOException {
    byte[] hash0 = cu.getChecksum();
    if (hash0 != null) {
      logger.debug3("doLocalHashNode: " +
		    ByteArray.toHexString(hash0) + " v " +
		    cu.getVersion());
    } else {
      logger.debug3("doLocalHashNode: null v " + cu.getVersion());
    }
    CachedUrl[] versions;
    try {
      versions = cu.getCuVersions();
      for (int i = 0; i < versions.length; i++) {
	logger.debug3("Version: " + i + "  " + versions[i].getVersion());
	byte[] hash = versions[i].getChecksum();
	if (hash != null) {
	  logger.debug3("Hash: " + ByteArray.toHexString(hash));
	} else {
	  logger.debug3("Hash: null");
	}
	doLocalHashVersion(versions[i]);
      }
    } catch (UnsupportedOperationException ex) {
      logger.error("LocalHash: " + cu.getUrl() + " has no versions");
    }
  }

  void doLocalHashVersion(CachedUrl cu) throws IOException {
    byte[] hash = cu.getChecksum();
    if (hash != null) {
      logger.debug3(cu.getUrl() + " stored hash is " +
		    ByteArray.toHexString(hash));
    }
    MessageDigest checksumProducer = makeDigest(checksumAlgorithm);
    if (checksumProducer != null) {
      // Hash the content of this version
      doLocalHashVersionContent(cu, checksumProducer);
    } else {
      logger.error("LocalHash: " + checksumAlgorithm + " not available");
    }
  }

  void doLocalHashVersionContent(CachedUrl cu, MessageDigest digest)
    throws IOException {
    if (callback == null) {
      logger.warning("null callback");
    }
    byte[] checksumStored = cu.getChecksum();
    // Hash the CachedUrl with the digest
    byte[] checksumComputed = localHashVersionContent(cu, digest);;
    // Is there a stored checksum
    if (checksumStored == null) {
      logger.debug3("No stored checksum for " + cu.getUrl());
      callback.hashMissing(cu, checksumComputed, checksumAlgorithm);
    } else {
      if (logger.isDebug3()) {
	logger.debug3(cu.getUrl() + " stored hash " +
		      ByteArray.toHexString(checksumStored) + " vs " +
		      ByteArray.toHexString(checksumComputed));
      }
      // Compare the hash we just did with the one in the props
      if (MessageDigest.isEqual(checksumStored, checksumComputed)) {
	// Content OK
	logger.debug2("Hash OK: " + cu.getUrl() + " v: " + cu.getVersion());
      } else {
	logger.debug3("hashes don't match for " + cu.getUrl());
	String oldAlgorithm = cu.getChecksumAlgorithm();
	logger.debug3("Algorithms: " + checksumAlgorithm + " vs. " +
		      oldAlgorithm);
	if (checksumAlgorithm.equalsIgnoreCase(oldAlgorithm)) {
	  // Content not OK
	  logger.error("Hash BAD: " + cu.getUrl() + " v: " + cu.getVersion() +
		       " " + ByteArray.toHexString(checksumStored) +
		       " != " + ByteArray.toHexString(checksumComputed));
	  if (callback != null) {
	    callback.hashMismatch(cu, checksumComputed, checksumAlgorithm);
	  }
	} else {
	  // Algorithm change - recompute with old algorithm
	  if ((digest = makeDigest(oldAlgorithm)) != null ) {
	    byte[] oldHash = localHashVersionContent(cu, digest);
	    logger.debug3("oldHash: " + ByteArray.toHexString(oldHash));
	    if (MessageDigest.isEqual(checksumStored, oldHash)) {
	      // Content OK, hash obsolete
	      logger.error("Hash obsolete but content OK: " +
			   cu.getUrl() + " v: " + cu.getVersion());
	      callback.hashObsolete(cu, checksumComputed, checksumAlgorithm);
	    } else {
	      // Content not OK, hash obsolete
	      logger.error("Hash BAD: " + cu.getUrl() + " v: " +
			   cu.getVersion());
	      callback.hashMismatch(cu, checksumComputed, checksumAlgorithm);
	    }
	  } else {
	    logger.error("Old algorithm not supported: " + oldAlgorithm);
	    throw new UnsupportedOperationException(oldAlgorithm);
	  }
	}
      }
    }
  }

  byte[] localHashVersionContent(CachedUrl cu, MessageDigest digest)
    throws IOException {
    InputStream is = cu.getUnfilteredInputStream();
    bytesHashed += StreamUtil.hash(is, -1, null, true, digest);
    filesHashed++;
    logger.debug3("Files " + filesHashed + " bytes " + bytesHashed + " alg " +
		  digest.getAlgorithm());
    is.close();
    return digest.digest();
  }

  MessageDigest makeDigest(String checksumAlgorithm) {
    // Create digester
    MessageDigest checksumProducer = null;
    if (!StringUtil.isNullString(checksumAlgorithm)) {
      try {
	checksumProducer = MessageDigest.getInstance(checksumAlgorithm);
      } catch (NoSuchAlgorithmException ex) {
	logger.warning(String.format("Checksum algorithm %s not found, checksuming disabled", checksumAlgorithm));
      }
    } else {
      throw new IllegalArgumentException("Null checksum algorithm");
    }
    return checksumProducer;
  }

  /**
   * The LocalHasher.Callback interface defines the methods that will be
   * called when:
   * - a CachedUrl fails to match its local hash,
   * - a CachedUrl has no local hash available,
   * - when a CachedUrl has a hash but no content,
   * - when a CachedUrl has a hash using an obsolete algorithm
   */
  public interface Callback {
    /*
     * Called when a CachedUrl version is found that does not match
     * its local hash.
     * @param cu CachedUrl
     * @param digest the computed hash of the CachedUrl version's content
     * @param alg the algorithm used for the hash
     */
    void hashMismatch(CachedUrl cu, byte[] digest, String alg);
    /*
     * Called when a CachedUrl version is found that has no local hash
     * @param cu CachedUrl
     * @param digest the computed hash of the CachedUrl version's content
     * @param alg the algorithm used for the hash
     */
    void hashMissing(CachedUrl cu, byte[] digest, String alg);
    /*
     * Called when a CachedUrl version is found that has a local hash
     * but no content
     * @param cu CachedUrl
     */
    void hashButNoContent(CachedUrl cu);
    /*
     * Called when a CachedUrl version is found with a matching local hash
     * using an obsolete algorithm
     * @param cu CachedUrl
     * @param digest the computed hash of the CachedUrl version's content
     * @param alg the algorithm used for the hash
     */
    void hashObsolete(CachedUrl cu, byte[] digest, String alg);
  }
}
