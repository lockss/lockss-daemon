/*
 * $Id: SampledContentHasher.java,v 1.1.2.4 2013-02-19 23:45:33 dshr Exp $
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
import java.math.*;
import java.security.*;

import org.lockss.util.*;
import org.lockss.config.*;
import org.lockss.state.*;
import org.lockss.plugin.*;
import org.lockss.plugin.base.*;
/**
 * Class to hash a sample of the URLs in a CachedUrlSet as
 * part of a Proof of Possession poll.
 */
public class SampledContentHasher extends GenericContentHasher {
  protected int mod;
  protected byte[] pollerNonce;
  protected MessageDigest sampleHasher = null;
  protected String alg = null;
  protected SubstanceChecker subCheck = null;

  public SampledContentHasher(CachedUrlSet cus,
			      MessageDigest digest,
			      byte[] pollerNonce,
			      int mod) {
    super(cus, digest);
    this.pollerNonce = pollerNonce;
    this.mod = mod;
    alg =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    try {
      sampleHasher = MessageDigest.getInstance(alg);
    } catch (NoSuchAlgorithmException ex) {
      log.error("No such hash algorithm: " + alg);
      throw new IllegalArgumentException("No such hash algorithm: " + alg);
    }
    boolean enableSubCheck =
      CurrentConfig.getBooleanParam(PARAM_POP_POLL_SUBSTANCE_CHECK,
				    DEFAULT_POP_POLL_SUBSTANCE_CHECK);
    if (enableSubCheck) {
      subCheck = new SubstanceChecker(cus.getArchivalUnit());
    }
  }

  public SampledContentHasher(CachedUrlSet cus,
			      MessageDigest digest,
			      byte[] pollerNonce,
			      int mod,
			      MessageDigest sampleHasher) {
    super(cus, digest);
    this.pollerNonce = pollerNonce;
    this.mod = mod;
    this.sampleHasher = sampleHasher;
    if (pollerNonce == null || sampleHasher == null || mod <= 0) {
      throw new IllegalArgumentException("new SampledContentHasher()");
    }
    alg = sampleHasher.getAlgorithm();
    boolean enableSubCheck =
      CurrentConfig.getBooleanParam(PARAM_POP_POLL_SUBSTANCE_CHECK,
				    DEFAULT_POP_POLL_SUBSTANCE_CHECK);
    if (enableSubCheck) {
      subCheck = new SubstanceChecker(cus.getArchivalUnit());
    }
  }

  public String typeString() {
    return "S";
  }

  protected long hashNodeUpToNumBytes(int numBytes)
      throws IOException {
    if (hashState != HASHING_NAME || nameBytes != null) {
      // Working on a CachedUrl
      return super.hashNodeUpToNumBytes(numBytes);
    }
    // Starting a new CachedUrl
    if (isInSample()) {
      // The current CachedUrl is in the sample, so
      // it gets hashed
      return super.hashNodeUpToNumBytes(numBytes);
    }
    // The current CachedUrl is not in the sample,
    // so it gets skipped.
    hashState = HASHING_NAME;
    endOfNode();
    return 0;
  }

  private boolean isInSample() {
    boolean ret = false;
    CachedUrl cu = getCurrentCu();
    String urlName = cu.getUrl();
    log.debug3("url: " + urlName + " mod: " + mod + " alg: " + alg);
    if (mod > 0 && cu.hasContent() && hasSubstance(urlName)) {
      // First hash the nonce and the current URL's name
      sampleHasher.update(pollerNonce);
      sampleHasher.update(urlName.getBytes());
      byte[] hash = sampleHasher.digest();
      // Compare high byte with mod (simplifies test)
      ret = ((((int)hash[hash.length-1] + 128) % mod) == 0);
      log.debug3(urlName + " byte: " + hash[hash.length-1] + " " +
		 (ret ? "is" : "isn't") + " in sample");
    }
    return ret;
  }

  private boolean hasSubstance(String url) {
    boolean ret = true;
    if (subCheck != null) {
      ret = subCheck.isSubstanceUrl(url);
    }
    return ret;
  }
}
