/*
 * $Id: GenericHasher.java,v 1.4 2002-12-17 20:20:26 aalto Exp $
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

/**
 * General class to handle content hashing
 */
public abstract class GenericHasher implements CachedUrlSetHasher {
  static final int HASH_TEST_DURATION = 1000;
  static final int HASH_TEST_BYTE_STEP = 1024;
  static final int MIN_BYTES_TO_HASH = 100;

  private CachedUrlSet cus = null;
  protected MessageDigest digest = null;
  private Object curElement = null;
  protected Iterator iterator = null;
  protected boolean isFinished = false;
  protected boolean shouldGetNextElement = true;
  protected static Logger log = Logger.getLogger("GenericHasher");
  private static int bytesPerMsEst = 0;


  protected GenericHasher(CachedUrlSet cus, MessageDigest digest) {
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
    if (digest == null || cus == null || iterator == null) {
      log.warning("Called with a null value for digest, cus, or iterator");
      isFinished = true;
      return 0;
    }
    int bytesLeftToHash = numBytes;
    log.debug(numBytes+" bytes left to hash in this step");

    while (bytesLeftToHash > 0) {
      if (curElement == null || shouldGetNextElement) {
	shouldGetNextElement = false;
	if (iterator.hasNext()) {
	  log.debug("Getting next element to hash");
	  curElement = iterator.next();
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
    }
    return numBytes;
  }

  public int getBytesPerMsEstimate() throws IOException {
    if (bytesPerMsEst == 0) {
      long timeTaken = 0;
      long bytesHashed = 0;
      long startTime = TimeBase.nowMs();
      Deadline deadline = Deadline.in(HASH_TEST_DURATION);
      while (!deadline.expired()) {
        bytesHashed += hashStep(HASH_TEST_BYTE_STEP);
      }
      timeTaken = TimeBase.nowMs() - startTime;
      bytesPerMsEst = (int)(bytesHashed / timeTaken);
    }
    return bytesPerMsEst;
  }

  /*
   * Subclasses should override this to correctly hash the specified element
   */
  protected abstract int hashElementUpToNumBytes(Object element, int numBytes)
      throws IOException;
}
