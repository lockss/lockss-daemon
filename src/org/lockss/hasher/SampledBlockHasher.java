/*
 * $Id: SampledBlockHasher.java,v 1.3 2013-05-29 15:02:25 dshr Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
  protected static Logger log = Logger.getLogger("SampledBlockHasher");
  protected int modulus = 0;
  /* Because we want all participants in the poll to construct
   * the same sample of URLs we can't reuse the poller or voter
   * nonces because they are different for each participant.
   */
  protected byte[] sampleNonce;
  protected MessageDigest sampleHasher = null;
  protected String alg = null;

  public SampledBlockHasher(CachedUrlSet cus,
			    int maxVersions,
			    int modulus,
			    byte[] sampleNonce,
			    MessageDigest[] digests,
			    byte[][]initByteArrays,
			    EventHandler cb) {
    super(cus, maxVersions, digests, initByteArrays, cb);
    this.modulus = modulus;
    this.sampleNonce = sampleNonce;
    alg =
      CurrentConfig.getParam(LcapMessage.PARAM_HASH_ALGORITHM,
			     LcapMessage.DEFAULT_HASH_ALGORITHM);
    try {
      sampleHasher = MessageDigest.getInstance(alg);
    } catch (NoSuchAlgorithmException ex) {
      log.error("No such hash algorithm: " + alg);
      throw new IllegalArgumentException("No such hash algorithm: " + alg);
    }
    if (sampleNonce == null || sampleHasher == null || modulus <= 0) {
      throw new IllegalArgumentException("new SampledBlockHashher()");
    }
    log.debug("new SampledBlockHasher: " + modulus + " " + alg);
  }

  public SampledBlockHasher(CachedUrlSet cus,
			    int maxVersions,
			    int modulus,
			    byte[] sampleNonce,
			    MessageDigest[] digests,
			    byte[][]initByteArrays,
			    EventHandler cb,
			    MessageDigest sampleHasher) {
    super(cus, maxVersions, digests, initByteArrays, cb);
    this.modulus = modulus;
    this.sampleNonce = sampleNonce;
    this.sampleHasher = sampleHasher;
    if (sampleNonce == null || sampleHasher == null || modulus <= 0) {
      throw new IllegalArgumentException("new SampledBlockHasher()");
    }
    alg = sampleHasher.getAlgorithm();
    log.debug("new SampledBlockHasher: " + modulus + " " + alg);
  }

  private String ts = null;
  public String typeString() {
    if (ts == null) {
      ts = "S(" + initialDigests.length + ":" + modulus + ")";
    }
    return ts;
  }

  protected boolean isIncluded(CachedUrlSetNode node) {
    boolean res = super.isIncluded(node);
    if (res) {
      String urlName = node.getUrl();
      log.debug3("url: " + urlName + " mod: " + modulus + " alg: " + alg);
      if (modulus != 0) {
	res = urlIsIncluded(sampleNonce, urlName, modulus, sampleHasher);
      }
    }
    return res;
  }

  public static boolean urlIsIncluded(byte[] sampleNonce, String urlName,
				      int modulus, MessageDigest sampleHasher) {
    boolean res = true;
    if (modulus != 0) {
      // First hash the nonce and the current URL's name
      sampleHasher.update(sampleNonce);
      sampleHasher.update(urlName.getBytes());
      byte[] hash = sampleHasher.digest();
      // Compare high byte with mod (simplifies test)
      res = ((((int)hash[hash.length-1] + 128) % modulus) == 0);
      log.debug(urlName + " byte: " + hash[hash.length-1] + " " +
		(res ? "is" : "isn't") + " in sample");
    }
    return res;
  }
}
