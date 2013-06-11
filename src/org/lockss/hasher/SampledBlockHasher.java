/*
 * $Id: SampledBlockHasher.java,v 1.4 2013-06-11 17:00:54 barry409 Exp $
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
 * General class to handle content hashing
 */
public class SampledBlockHasher extends BlockHasher {
  protected static Logger log = Logger.getLogger(SampledBlockHasher.class);

  final protected int modulus;
  final protected byte[] sampleNonce;
  final protected MessageDigest sampleHasher;

  SampledBlockHasher(CachedUrlSet cus,
		     int maxVersions,
		     MessageDigest[] digests,
		     byte[][]initByteArrays,
		     EventHandler cb,
		     int modulus,
		     byte[] sampleNonce,
		     MessageDigest sampleHasher) {
    super(cus, maxVersions, digests, initByteArrays, cb);
    if (sampleNonce == null || sampleHasher == null || modulus <= 0) {
      throw new IllegalArgumentException("new SampledBlockHasher()");
    }
    this.modulus = modulus;
    this.sampleNonce = sampleNonce;
    this.sampleHasher = sampleHasher;
    log.debug("new SampledBlockHasher: " + modulus + " " + getAlgorithm());
  }

  public SampledBlockHasher(CachedUrlSet cus,
			    int maxVersions,
			    MessageDigest[] digests,
			    byte[][]initByteArrays,
			    EventHandler cb,
			    int modulus,
			    byte[] sampleNonce) {
    this(cus, maxVersions, digests, initByteArrays, cb,
	 modulus, sampleNonce, defaultSampleHasher());
  }

  private static MessageDigest defaultSampleHasher() {
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

  public String getAlgorithm() {
    return this.sampleHasher.getAlgorithm();
  }

  private String ts = null;
  public String typeString() {
    if (ts == null) {
      ts = "S(" + initialDigests.length + ":" + modulus + ")";
    }
    return ts;
  }

  protected boolean isIncluded(CachedUrlSetNode node) {
    return super.isIncluded(node) && 
      urlIsIncluded(sampleNonce, node.getUrl(), modulus, sampleHasher);
  }

  public static boolean urlIsIncluded(byte[] sampleNonce, String url,
				      int modulus, MessageDigest sampleHasher) {
    // First hash the nonce and the current URL's name
    sampleHasher.update(sampleNonce);
    sampleHasher.update(url.getBytes());
    byte[] hash = sampleHasher.digest();

    // Compare high byte with mod (simplifies test)
    boolean res = ((((int)hash[hash.length-1] + 128) % modulus) == 0);
    if (log.isDebug3()) log.debug3(url + " byte: " + hash[hash.length-1] + " " +
				   (res ? "is" : "isn't") + " in sample");
    return res;
  }
}
