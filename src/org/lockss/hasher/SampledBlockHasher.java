/*
 * $Id$
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
import java.util.*;
import java.security.*;

import org.lockss.config.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;
import org.lockss.state.SubstanceChecker;
import org.lockss.util.*;

/**
 * Class to sample a fraction of the content in an AU for
 * proof-of-possession polls.
 */
public class SampledBlockHasher extends BlockHasher {
  protected static final Logger log =
    Logger.getLogger(SampledBlockHasher.class);

  private final FractionalInclusionPolicy inclusionPolicy;

  /**
   * Make a {@link SampledBlockHasher}.
   * @param cus The {@link CachedUrlSet} containing the URLs to be hashed.
   * @param maxVersions The maximum number of versions, if
   * positive. Otherwise, the number of versions to hash will default
   * to a system-selected value.
   * @param digests Content will be hashed using these {@link MessageDigest}s.
   * @param initByteArrays Contains one {@code byte[]} nonce for each digest.
   * @param cb Called back for each block hashed.
   * @param inclusionPolicy The policy for including and excluding URLs
   * from the sample.
   */
  public SampledBlockHasher(CachedUrlSet cus,
			    int maxVersions,
			    MessageDigest[] digests,
			    byte[][] initByteArrays,
			    EventHandler cb,
			    FractionalInclusionPolicy inclusionPolicy) {
    super(cus, maxVersions, digests, initByteArrays, cb);
    this.inclusionPolicy = inclusionPolicy;
    log.debug("new SampledBlockHasher: " + inclusionPolicy.typeString());
  }

  /**
   * Make a {@link SampledBlockHasher}.
   * @param cus The {@link CachedUrlSet} containing the URLs to be hashed.
   * @param maxVersions The maximum number of versions, if
   * positive. Otherwise, the number of versions to hash will default
   * to a system-selected value.
   * @param digests Content will be hashed using these {@link MessageDigest}s.
   * @param initByteArrays Contains one {@code byte[]} nonce for each digest.
   * @param cb Called back for each block hashed.
   * @param modulus {@code 1/modulus} of the URLs will be included.
   * @param sampleNonce Random bytes to salt the hash. Two instances
   * sharing the same nonce and using the same hshing algorithm will
   * end up with the same result.
   */
  public SampledBlockHasher(CachedUrlSet cus,
			    int maxVersions,
			    MessageDigest[] digests,
			    byte[][] initByteArrays,
			    EventHandler cb,
			    int modulus,
			    byte[] sampleNonce) {
    this(cus, maxVersions, digests, initByteArrays, cb,
	 modulus, sampleNonce, defaultSampleHasher());
  }

  /**
   * Make a {@link SampledBlockHasher}.
   * @param cus The {@link CachedUrlSet} containing the URLs to be hashed.
   * @param maxVersions The maximum number of versions, if
   * positive. Otherwise, the number of versions to hash will default
   * to a system-selected value.
   * @param digests Content will be hashed using these {@link MessageDigest}s.
   * @param initByteArrays Contains one {@code byte[]} nonce for each digest.
   * @param cb Called back for each block hashed.
   * @param modulus {@code 1/modulus} of the URLs will be included.
   * @param sampleNonce Random bytes to salt the hash. Two instances
   * sharing the same nonce and using the same hshing algorithm will
   * end up with the same result.
   * @param sampleHasher A {@link MessageDigest} to be used for
   * hashing the URLs.
   */
  public SampledBlockHasher(CachedUrlSet cus,
			    int maxVersions,
			    MessageDigest[] digests,
			    byte[][] initByteArrays,
			    EventHandler cb,
			    int modulus,
			    byte[] sampleNonce,
			    MessageDigest sampleHasher) {
    this(cus, maxVersions, digests, initByteArrays, cb,
	 new FractionalInclusionPolicy(modulus, sampleNonce, sampleHasher));
  }

  /**
   * A policy class to implement {@link #isIncluded} for use in {@link
   * SampledBlockHasher}.
   *
   * The URL is hashed along with a nonce, and by examining some of
   * the bytes in the hash, roughly {@code 1/modulus} of those URLs
   * are included.
   */
  public final static class FractionalInclusionPolicy {

    private final int modulus;
    private final byte[] sampleNonce;
    private final MessageDigest sampleHasher;
    
    /**
     * @param modulus {@code 1/modulus} of the URLs will be included.
     * @param sampleNonce Random bytes to salt the hash. Two instances
     * sharing the same nonce and using the same hshing algorithm will
     * end up with the same result.
     * @param sampleHasher A {@link MessageDigest} to be used for
     * hashing the URLs.
     */
    public FractionalInclusionPolicy(int modulus, byte[] sampleNonce,
				     MessageDigest sampleHasher) {
      if (sampleNonce == null) {
	throw new IllegalArgumentException("null sampleNonce not allowed.");	
      }
      if (sampleHasher == null) {
	throw new IllegalArgumentException("null sampleHasher not allowed.");
      }
      if (modulus <= 0) {
	throw new IllegalArgumentException("modulus must be positive; was: "+
					   modulus);
      }
      this.modulus = modulus;
      this.sampleNonce = sampleNonce;
      this.sampleHasher = sampleHasher;
    }

    /**
     * @param url A URL.
     * @return {@code true} for roughly {@code 1/modulus} of the URLs.
     */
    public boolean isIncluded(String url) {
      // hash the nonce and the current URL
      sampleHasher.reset();
      sampleHasher.update(getSampleNonce());
      sampleHasher.update(url.getBytes());
      byte[] hash = sampleHasher.digest();
      
      int value = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16) |
	((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
      boolean res = ((value % modulus) == 0);
      if (log.isDebug3()) {
	log.debug3(url + " " + (res ? "is" : "isn't") + " in sample");
      }
      return res;
    }
    
    /**
     * @return The modulus passed at creation of this instance.
     */
    public int getSampleModulus() {
      return modulus;
    }

    /**
     * @return The sampleNonce passed at creation of this instance.
     * NOTE: Changing the bytes in the returned array will change the
     * behavior of the instance. Clients should not do that.
     */
    public byte[] getSampleNonce() {
      return sampleNonce;
    }

    /**
     * @return The hashing algorithim of the digest passed at creation
     * of this instance.
     */
    public String getAlgorithm() {
      return sampleHasher.getAlgorithm();
    }

    /**
     * @return A {@link String} useful for display.
     */
    public String typeString() {
      return "Fract: 1/"+getSampleModulus()+
	" Alg: "+getAlgorithm()+
	" Nonce: "+ByteArray.toBase64(getSampleNonce());
    }
  }

  /**
   * @return A {@link MessageDigest} to use when none is supplied.
   */
  private static MessageDigest defaultSampleHasher() {
    // todo: By 1.62, 
    String alg =
      CurrentConfig.getParam(LcapMessage.PARAM_HASH_ALGORITHM,
			     LcapMessage.DEFAULT_HASH_ALGORITHM);
    try {
      return MessageDigest.getInstance(alg);
    } catch (NoSuchAlgorithmException ex) {
      log.error("No such hash algorithm: " + alg);
      throw new IllegalArgumentException("No such hash algorithm: " + alg);
    }
  }


  /** 
   * @return A short string describing the type of hash, to be used
   * in a status table.
   */
  public String typeString() {
    return "S(" + initialDigests.length + ":" + 
      inclusionPolicy.typeString() + ")";
  }

  /**
   * @return The {@link FractionalInclusionPolicy} supplied or created
   * at creation time.
   */
  public FractionalInclusionPolicy getInclusionPolicy() {
    return inclusionPolicy;
  }

  /**
   * @return {@code true} iff {@code node} should be hashed.
   */
  protected boolean isIncluded(CachedUrlSetNode node) {
    // NOTE: subclass contract requires that super.isIncluded be
    // called in all cases.
    return super.isIncluded(node) && 
      inclusionPolicy.isIncluded(node.getUrl());
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
    reportExclusions();
    // We only hashed 1/modulus of the content.
    int modulus = inclusionPolicy.getSampleModulus();
    cus.storeActualHashDuration(elapsed * modulus, err);
  }

}
