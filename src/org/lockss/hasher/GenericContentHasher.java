/*
 * $Id: GenericContentHasher.java,v 1.22 2004-10-13 23:07:17 clairegriffin Exp $
 */

/*

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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
  private byte[] contentBytes = null;
  private int nameIdx = -1;


  private ArchivalUnit au = null;
  private CachedUrl cu = null;
  private InputStream is = null;
  private long hashedContentSize = 0;


  public GenericContentHasher(CachedUrlSet cus, MessageDigest digest) {
    super(cus, digest);
    au = cus.getArchivalUnit();
    iterator = cus.contentHashIterator();
    if (iterator == null) {
      throw new IllegalArgumentException("Called with a CachedUrlSet that "+
					 "gave me a null contentHashIterator");
    }
  }

  protected int hashElementUpToNumBytes(int numBytes)
      throws IOException {
    if (cu == null) {
      switch (curElement.getType()) {
      case CachedUrlSetNode.TYPE_CACHED_URL_SET:
        CachedUrlSet cus = (CachedUrlSet)curElement;
        cu = au.makeCachedUrl(cus.getUrl());
        break;
      case CachedUrlSetNode.TYPE_CACHED_URL:
        cu = (CachedUrl)curElement;
        break;
      }
    }
    int totalHashed = 0;
    if (hashState == HASHING_NAME) {
      totalHashed += hashName(numBytes);
      if (totalHashed >= numBytes) {
	return totalHashed;
      }
    }
    if (hashState == HASHING_CONTENT) {
      totalHashed += hashContent(numBytes);
    }
    return totalHashed;
  }

  private int hashName(int numBytes) {
    int totalHashed = 0;
    if (isTrace) log.debug3("Hashing name");
    if (nameBytes == null) {
      String nameStr = cu.getUrl();
      nameBytes = nameStr.getBytes();
      nameIdx = 0;
      if (isTrace) log.debug3("got new name to hash: "+nameStr);

//       if (cu.hasContent()) {
// 	byte[] sizeBytes = cu.getUnfilteredContentSize();
// 	if (isTrace) {
// 	  log.debug3("sizeBytes has length of "+sizeBytes.length);
// 	}
// //  	digest.update((byte)sizeBytes.length);
// //  	digest.update(sizeBytes);
// //  	totalHashed += (sizeBytes.length+1);
//       } else {
// 	digest.update(NO_CONTENT);
// 	totalHashed++;
//       }
    }

    int bytesRemaining = nameBytes.length - nameIdx;
    int len = numBytes < bytesRemaining ? numBytes : bytesRemaining;

    if (isTrace) log.debug3("Going to hash "+len+" name bytes");
    digest.update(nameBytes, nameIdx, len);
    nameIdx += len;
    if (nameIdx >= nameBytes.length) {
      if (isTrace) log.debug3("done hashing name: "+cu);
      hashState = HASHING_CONTENT;
      nameBytes = null;
    }
    totalHashed += len;
    if (isTrace) log.debug3(totalHashed+" bytes hashed in this step");
    return totalHashed;
  }

  private int hashContent(int numBytes) throws IOException {
    int totalHashed = 0;
    if (isTrace) log.debug3("hashing content");
    if (is == null) {
      if (cu.hasContent()) {
	if (isTrace) log.debug3("opening "+cu+" for hashing");
	is = cu.openForHashing();
      } else {
	if (isTrace) log.debug3(cu+" has no content, not hashing");
	digest.update(NO_CONTENT);
	totalHashed++;

	hashState = HASHING_NAME;
	curElement = null;
	cu = null;
	return totalHashed;
      }
    }
    int bytesLeftToHash = numBytes - totalHashed;
    if (contentBytes == null || contentBytes.length < (bytesLeftToHash)) {
      contentBytes = new byte[numBytes + 100];
    }
    int bytesHashed = is.read(contentBytes, 0, bytesLeftToHash);
    if (isTrace)
      log.debug3("Read "+bytesHashed+" bytes from the input stream");
    if (bytesHashed >= 0) {
      digest.update(contentBytes, 0, bytesHashed);
      totalHashed += bytesHashed;
      hashedContentSize += bytesHashed;
    } else {
//     if (bytesHashed != 0 && bytesHashed < bytesLeftToHash) {
      if (isTrace) log.debug3("done hashing content: "+cu);
      byte[] sizeBytes =
	(new BigInteger(Long.toString(hashedContentSize)).toByteArray());
      digest.update((byte)sizeBytes.length);
      digest.update(sizeBytes);
      totalHashed += sizeBytes.length+1;

      hashedContentSize = 0;
      hashState = HASHING_NAME;
      curElement = null;
      cu = null;
      is.close();
      is = null;
    }
    if (isTrace) log.debug3(totalHashed+" bytes hashed in this step");
    return totalHashed;
  }

}
