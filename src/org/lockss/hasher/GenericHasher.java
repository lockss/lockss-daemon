/*
 * $Id: GenericHasher.java,v 1.12 2003-03-18 01:32:57 troberts Exp $
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
import org.lockss.util.*;
import org.lockss.plugin.*;

/**
 * General class to handle content hashing
 */
public abstract class GenericHasher implements CachedUrlSetHasher {
  protected CachedUrlSet cus = null;
  protected MessageDigest digest = null;
  private CachedUrlSetNode curElement = null;
  protected Iterator iterator = null;
  protected boolean isFinished = false;
  protected boolean shouldGetNextElement = true;
  protected static Logger log = Logger.getLogger("GenericHasher");

  protected GenericHasher(CachedUrlSet cus, MessageDigest digest) {
    if (digest == null) {
      throw new IllegalArgumentException("Called with a null MessageDigest");
    } else if (cus == null) {
      throw new IllegalArgumentException("Called with a null CachedUrlSet");
    }
    this.cus = cus;
    this.digest = digest;
  }

  /**
   * @return true if there is nothing left to hash
   */
  public boolean finished() {
    return isFinished;
  }  
  /**
   * @param numBytes maximum number of bytes to hash (counting delimiters)
   * @return number of bytes actually hashed.  This will only be less than
   * numBytes if there is nothing left to hash.
   * @throws IOException
   */
  public int hashStep(int numBytes) throws IOException {
    int bytesLeftToHash = numBytes;
    if (finished()) {
      log.warning("Hash step called after hasher was finished");
      return 0;
    }

    log.debug(numBytes+" bytes left to hash in this step");

    int totalBytesHashed = 0;
    while (bytesLeftToHash > 0) {
      if (curElement == null || shouldGetNextElement) {
	shouldGetNextElement = false;
	curElement = getNextElement();
	if (curElement != null) {
	  log.debug("Getting next element to hash");
// 	  curElement = (CachedUrlSetNode)iterator.next();
	}
	else {
	  log.debug("No more elements to hash");
	  this.isFinished = true;
	  return numBytes - bytesLeftToHash;
	}
      }
      int numBytesHashed =
	hashElementUpToNumBytes(curElement, bytesLeftToHash);
      bytesLeftToHash -= numBytesHashed;
      totalBytesHashed += numBytesHashed;
    }
    return totalBytesHashed;
  }

  /*
   * Subclasses should override this to correctly hash the specified element
   */
  protected abstract int hashElementUpToNumBytes(CachedUrlSetNode element, 
						 int numBytes)
      throws IOException;

  protected CachedUrlSetNode getNextElement() {
    if (iterator.hasNext()) {
      return (CachedUrlSetNode)iterator.next();
    }
    return null;
  }

}
