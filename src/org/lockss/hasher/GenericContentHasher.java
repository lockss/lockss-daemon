/*
 * $Id: GenericContentHasher.java,v 1.1 2002-11-06 18:33:26 troberts Exp $
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
/**
 * General class to handle content hashing
 */
public class GenericContentHasher extends GenericHasher {
  private static final char DELIMITER = '&';
  private static final int HASHING_NAME = 1;
  private static final int HASHING_CONTENT = 2;

  private int hashState = HASHING_NAME;

  private byte[] nameBytes = null;
  private int nameIdx = -1;
  
  private InputStream is = null;


  public GenericContentHasher(CachedUrlSet cus, MessageDigest digest) {
    super(cus, digest);
    iterator = cus.leafIterator();
  }

  protected int hashElementUpToNumBytes(Object element, int numBytes) 
      throws IOException {
    CachedUrl cu = (CachedUrl) element;
    int totalHashed = 0;

    if (hashState == HASHING_NAME) {
      if (nameBytes == null) {
	String url = cu.getUrl();
	StringBuffer sb = new StringBuffer(url.length()+2);
	sb.append(DELIMITER);
	sb.append(url);
	sb.append(DELIMITER);
	nameBytes = (sb.toString().getBytes());
	nameIdx = 0;
      }
      int bytesRemaining = nameBytes.length - nameIdx;
      int len = 
	numBytes < bytesRemaining ? numBytes : bytesRemaining;

      digest.update(nameBytes, nameIdx, len);
      nameIdx += len;
      if (nameIdx >= nameBytes.length) {
	hashState = HASHING_CONTENT;
	nameBytes = null;
      }
      totalHashed += len;
    }
    if(hashState == HASHING_CONTENT) {
      if(is == null) {
	is = cu.openForReading();
      }
      byte[] bytes = new byte[numBytes - totalHashed];
      int bytesHashed = is.read(bytes);
      if (bytesHashed < 0) {
	bytesHashed = 0;
      }
      digest.update(bytes, 0, bytesHashed);
      if (bytesHashed < bytes.length) {
	hashState = HASHING_NAME;
	shouldGetNextElement = true;
	is = null;
      }
      totalHashed += bytesHashed;
    }
    return totalHashed;
  }
}
