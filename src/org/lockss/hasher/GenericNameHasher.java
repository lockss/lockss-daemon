/*
 * $Id$
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

package org.lockss.hasher;

import java.security.*;
import java.util.*;

import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * General class to handle name hashing
 */
public class GenericNameHasher extends GenericHasher {
  private static final byte NO_CONTENT=0;
  private static final byte CONTENT=1;

  byte[] nameBytes = null;
  int nameIdx = -1;

  private boolean isTrace = log.isDebug3();

  public GenericNameHasher(CachedUrlSet cus, MessageDigest dig) {
    super(cus, dig);
  }

  public String typeString() {
    return "N";
  }

  public long getEstimatedHashDuration() {
    return 1000;
  }

  public void storeActualHashDuration(long elapsed, Exception err) {
    // don't store name poll duration
  }

  protected Iterator getIterator(CachedUrlSet cus) {
    return cus.flatSetIterator();
  }

  protected long hashNodeUpToNumBytes(int numBytes) {
    int totalHashed = 0;
    if (nameBytes == null) {
      String nameStr = curNode.getUrl();
      if (isTrace) {
	log.debug3("Getting new name: "+nameStr);
      }
      nameBytes = nameStr.getBytes();
      nameIdx = 0;

      if (curNode.hasContent()) {
 	digest.update(CONTENT);
      } else {
	digest.update(NO_CONTENT);
      }

      byte[] sizeBytes = ByteArray.encodeLong(nameStr.length());
      digest.update((byte)sizeBytes.length);
      digest.update(sizeBytes);
      totalHashed += sizeBytes.length + 2;
    }

    int len = Math.min(numBytes, (nameBytes.length - nameIdx));

    digest.update(nameBytes, nameIdx, len);
    nameIdx += len;
    totalHashed += len;
    if (nameIdx >= nameBytes.length) {
      endOfNode();
      nameBytes = null;
    }
    if (isTrace) {
      log.debug3("Hashed "+totalHashed+" bytes in this step");
    }
    return totalHashed;
  }

}


