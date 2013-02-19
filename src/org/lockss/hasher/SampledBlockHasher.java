/*
 * $Id: SampledBlockHasher.java,v 1.1.2.1 2013-02-19 23:45:33 dshr Exp $
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
import org.lockss.plugin.base.*;
import org.lockss.state.SubstanceChecker;
import org.lockss.util.*;

/**
 * General class to handle content hashing
 */
public class SampledBlockHasher extends BlockHasher {
  protected static Logger log = Logger.getLogger("SampledBlockHasher");
  protected int modulus = 0;
  protected byte[] pollerNonce;
  protected MessageDigest sampleHasher = null;
  protected String alg = null;

  public SampledBlockHasher(CachedUrlSet cus,
			    int maxVersions,
			    int modulus,
			    byte[] pollerNonce,
			    MessageDigest[] digests,
			    byte[][]initByteArrays,
			    EventHandler cb) {
    super(cus, maxVersions, digests, initByteArrays, cb);
    this.modulus = modulus;
    this.pollerNonce = pollerNonce;
    alg =
      CurrentConfig.getParam(BaseUrlCacher.PARAM_CHECKSUM_ALGORITHM,
			     BaseUrlCacher.DEFAULT_CHECKSUM_ALGORITHM);
    try {
      sampleHasher = MessageDigest.getInstance(alg);
    } catch (NoSuchAlgorithmException ex) {
      log.error("No such hash algorithm: " + alg);
      throw new IllegalArgumentException("No such hash algorithm: " + alg);
    }
    if (pollerNonce == null || sampleHasher == null || modulus <= 0) {
      throw new IllegalArgumentException("new SampledBlockHashher()");
    }
  }

  public SampledBlockHasher(CachedUrlSet cus,
			    int maxVersions,
			    int modulus,
			    byte[] pollerNonce,
			    MessageDigest[] digests,
			    byte[][]initByteArrays,
			    EventHandler cb,
			    MessageDigest sampleHasher) {
    super(cus, maxVersions, digests, initByteArrays, cb);
    this.modulus = modulus;
    this.pollerNonce = pollerNonce;
    this.sampleHasher = sampleHasher;
    if (pollerNonce == null || sampleHasher == null || modulus <= 0) {
      throw new IllegalArgumentException("new SampledBlockHasher()");
    }
    alg = sampleHasher.getAlgorithm();
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
      if (modulus > 0) {
	// First hash the nonce and the current URL's name
	sampleHasher.update(pollerNonce);
	sampleHasher.update(urlName.getBytes());
	byte[] hash = sampleHasher.digest();
	// Compare high byte with mod (simplifies test)
	res = ((((int)hash[hash.length-1] + 128) % modulus) == 0);
	log.debug3(urlName + " byte: " + hash[hash.length-1] + " " +
		   (res ? "is" : "isn't") + " in sample");
      }
    }
    return res;
  }
}
