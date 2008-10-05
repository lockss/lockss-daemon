/*
 * $Id: MDCredential.java,v 1.6 2006-01-13 23:21:06 thib_gc Exp $
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

package org.lockss.jetty;

import java.io.*;
import java.util.*;
import java.security.*;

import org.mortbay.util.*;
import org.lockss.util.*;

/** A Credential derived from a MessageDigest, accepts credential strings of
 * the form ALG:DIGEST , where ALG is a known MessageDigest algorithm name
 * (eg "SHA1" or "SHA-512") and DIGEST is the result of digesting the
 * password
 */
public class MDCredential extends Credential {
  static Logger log = Logger.getLogger("MDCredential");

  private static HashMap mdMap = new HashMap();

  /** Factory method to create a credential from a String of the form
   * ALG:DIGEST
   * @param credential String representation of the credential
   * @return A MDCredential instance.
   * @throws NoSuchAlgorithmException
   */
  public static MDCredential makeCredential(String credential)
      throws NoSuchAlgorithmException {
    return new MDCredential(credential);
  }

  /** Disable inherited factory method from Credential, because our factory
   * wants to through a checked exception */
  public static Credential getCredential(String credential) {
    throw new UnsupportedOperationException("Not part of API of this class");
  }

  /** Internal factory for MessageDigest instances.  Creates only one
   * MessageDigest for each algorithm; the caller must synchronize use of
   * the returned digest.
   */
  private static MessageDigest getMessageDigest(String type)
      throws NoSuchAlgorithmException {
    if (type == null) throw new NoSuchAlgorithmException("null algorithm");
    synchronized (mdMap) {
      MessageDigest md = (MessageDigest)mdMap.get(type);
      if (md == null) {
	md = MessageDigest.getInstance(type);
	mdMap.put(type, md);
      }
      return md;
    }
  }

  // Instance members

  private byte[] _digest;
  private String _type;

  /** Create a credential from a ALG:DIGEST string */
  MDCredential(String digest) throws NoSuchAlgorithmException {
    this(null, digest);
  }

  /** Create a credential, finding the alroithm either in the specified
   * type or in the digest string */
  MDCredential(String type, String digest) throws NoSuchAlgorithmException {
    int pos = digest.indexOf(':');
    if (pos > 0) {
      String prefix = digest.substring(0, pos);
      if (type == null) {
	type = prefix;
      } else if (!prefix.equals(type)) {
	throw new IllegalArgumentException("Digest " + digest +
					   " is not of type " + type);
      }
      digest = digest.substring(pos + 1);
    }
    log.debug("type: " + type + ", digest: " + digest);
    // throw now if unknown algorithm
    getMessageDigest(type);
    _digest=TypeUtil.parseBytes(digest,16);
    _type = type;
  }

  /** Return the stored digest */
  public byte[] getDigest() {
    return _digest;
  }

  /** Return the digest type (algorithm) */
  public String getType() {
    return _type;
  }

  byte[] calculateDigest(String message) throws UnsupportedEncodingException {
    try {
      MessageDigest md = getMessageDigest(_type);
      synchronized(md) {
	md.reset();
	md.update(message.getBytes(Constants.DEFAULT_ENCODING));
	return md.digest();
      }
    } catch (NoSuchAlgorithmException e) {
      // impossible
      throw new RuntimeException("Shouldn't happen");
    }
  }

  boolean equals(byte[] digest) {
    if (digest == null || digest.length != _digest.length) {
      return false;
    }
    for (int ix = 0; ix < digest.length; ix++) {
      if (digest[ix] != _digest[ix]) {
	return false;
      }
    }
    return true;
  }

  /** Check a credential
   * @param credentials The credential to check against. This may either be
   * another Credential object or a String which is interpreted by this
   * credential.
   * @return True if the credentials indicated that the shared secret is
   * known to both this Credential and the passed credential.
   */
  public boolean check(Object credentials) {
    log.debug3("check(" + credentials + ")");
    try {
      byte[] digest=null;
      if (credentials instanceof Password ||
	  credentials instanceof String) {
	digest = calculateDigest(credentials.toString());
	return equals(digest);
      } else if (credentials instanceof MDCredential) {
	return equals(((MDCredential)credentials)._digest);
      } else {
	log.warning("Can't check " + credentials.getClass() +
		    "against MDCredential");
	return false;
      }
    } catch (Exception e) {
      log.warning("Checking " + this, e);
      return false;
    }
  }
}
