/*

Copyright (c) 2021-2022 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.laaws;

import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.input.CountingInputStream;
import org.lockss.util.*;
import org.lockss.plugin.*;


/**
 * Wrapper for CachedUrl used to convey info available at a low level
 * (content digest and byte counts) to higher levels operatoing on
 * CachedUrls
 */
public class DigestCachedUrl {
  private static final Logger log = Logger.getLogger(DigestCachedUrl.class);

  MessageDigest md;
  CachedUrl cu;
  static final String HASH_ALGORITHM="SHA-256";
  String contentDigest=null;
  CountingInputStream contentCis;
  CountingInputStream totalCis;
//   long bytesMoved;

  public DigestCachedUrl(CachedUrl cu) {
    this.cu = cu;
  }

  public MessageDigest createMessageDigest() {
    try {
      md = MessageDigest.getInstance(HASH_ALGORITHM);
      contentDigest=null;
    }
    catch (NoSuchAlgorithmException e) {
      // this should never occur
      log.critical("Digest algorithm: " + HASH_ALGORITHM + ": "
                   + e.getMessage());
    }
    return md;
  }

  public MessageDigest getMessageDigest() {
    return md;
  }

  public CachedUrl getCu() {
    return cu;
  }

  public String getContentDigest() {
    if( contentDigest == null) {
      contentDigest = String.format("%s:%s",
                                    HASH_ALGORITHM,
                                    new String(Hex.encodeHex(md.digest())));
      log.debug2("contentDigest: " + contentDigest);
    }
    return contentDigest;
  }

  public long getContentBytesRead() {
    if (contentCis == null) {
      return 0;
    }
    return contentCis.getByteCount();
  }

  public long getTotalBytesRead() {
    if (totalCis == null) {
      return 0;
    }
    return totalCis.getByteCount();
  }

  public void setContentCountingInputStream(CountingInputStream cis) {
    this.contentCis = cis;
  }

  public void setTotalCountingInputStream(CountingInputStream cis) {
    this.totalCis = cis;
  }

  public long getContentBytesMoved() {
    return contentCis.getByteCount();
  }

  public long getTotalBytesMoved() {
    return totalCis.getByteCount();
  }
}
