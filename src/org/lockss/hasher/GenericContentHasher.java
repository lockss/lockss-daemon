/*
 * $Id: GenericContentHasher.java,v 1.13 2003-04-15 01:27:01 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.CollectionUtil;
/**
 * General class to handle content hashing
 */
public class GenericContentHasher extends GenericHasher {
  private static final int HASHING_NAME = 1;
  private static final int HASHING_CONTENT = 2;

  private static final byte NO_CONTENT = -1;

  private int hashState = HASHING_NAME;

  private byte[] nameBytes = null;
  private int nameIdx = -1;

  private InputStream is = null;
  private boolean hashedRootCus = false;


  public GenericContentHasher(CachedUrlSet cus, MessageDigest digest) {
    super(cus, digest);
    iterator = cus.contentHashIterator();
    if (iterator == null) {
      throw new IllegalArgumentException("Called with a CachedUrlSet that "+
					 "gave me a null contentHashIterator");
    }
  }

  protected int hashElementUpToNumBytes(CachedUrlSetNode element, int numBytes)
      throws IOException {
    CachedUrl cu = null;
    switch (element.getType()) {
      case CachedUrlSetNode.TYPE_CACHED_URL_SET:
        CachedUrlSet cus = (CachedUrlSet)element;
        cu = cus.makeCachedUrl(cus.getUrl());
        break;
      case CachedUrlSetNode.TYPE_CACHED_URL:
        cu = (CachedUrl)element;
        break;
    }

    int totalHashed = 0;
    if (hashState == HASHING_NAME) {
      totalHashed += hashName(cu, numBytes);
      if (totalHashed >= numBytes) {
	return totalHashed;
      }
    }
    if (hashState == HASHING_CONTENT) {
      totalHashed += hashContent(cu, numBytes);
    }
    return totalHashed;
  }

  private int hashName(CachedUrl cu, int numBytes) {
    int totalHashed = 0;
    log.debug3("Hashing name");
    if (nameBytes == null) {
      String url = cu.getUrl();
      StringBuffer sb = new StringBuffer(url.length());
      sb.append(url);
      String nameStr = sb.toString();
      nameBytes = nameStr.getBytes();
      nameIdx = 0;
      log.debug3("got new name to hash: "+nameStr);

      if (cu.hasContent()) {
	byte[] sizeBytes = cu.getContentSize();
	log.debug3("sizeBytes has length of "+sizeBytes.length);
	digest.update((byte)sizeBytes.length);
	digest.update(sizeBytes);
	totalHashed += (sizeBytes.length+1);
      } else {
	digest.update(NO_CONTENT);
	totalHashed++;
      }
    }

    int bytesRemaining = nameBytes.length - nameIdx;
    int len = numBytes < bytesRemaining ? numBytes : bytesRemaining;

    log.debug3("Going to hash "+len+" name bytes");
    digest.update(nameBytes, nameIdx, len);
    nameIdx += len;
    if (nameIdx >= nameBytes.length) {
      log.debug3("done hashing name: "+cu);
      hashState = HASHING_CONTENT;
      nameBytes = null;
    }
    totalHashed += len;
    log.debug3(totalHashed+" bytes hashed in this step");
    return totalHashed;
  }

  private int hashContent(CachedUrl cu, int numBytes) throws IOException {
    int totalHashed = 0;
    log.debug3("hashing content");
    if(is == null) {
      if (cu.hasContent()) {
	log.debug3("opening "+cu+" for hashing");
	is = cu.openForHashing();
      } else {
	log.debug3(cu+" has no content, not hashing");
	hashState = HASHING_NAME;
	shouldGetNextElement = true;
	return totalHashed;
      }
    }
    byte[] bytes = new byte[numBytes - totalHashed];
    int bytesHashed = is.read(bytes);
    log.debug3("Read "+bytesHashed+" bytes from the input stream");
    if (bytesHashed >= 0) {
      digest.update(bytes, 0, bytesHashed);
    }
    if (bytesHashed != 0 && bytesHashed < bytes.length) {
      log.debug3("done hashing content: "+cu);
      hashState = HASHING_NAME;
      shouldGetNextElement = true;
      is.close();
      is = null;
    }
    if (bytesHashed > 0) {
      totalHashed += bytesHashed;
    }
    log.debug3(totalHashed+" bytes hashed in this step");
    return totalHashed;
  }

}
