/*
 * $Id$
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

package org.lockss.daemon;
import java.io.IOException;
import java.security.MessageDigest;
import org.lockss.plugin.*;

/**
 * CachedUrlSetHasher describes a hash to be performed on a CachedUrlSet,
 * and encapsulates the state of the hash in progress
 */
public interface CachedUrlSetHasher {
  /**
   * Determines whether to hash filtered or raw content.  Default is
   * filtered (true)
   * @param val if true hash filtered content, if false hash raw content
   */
  public void setFiltered(boolean val);

  /**
   * @return the CachedUrlSet to be/being hashed by this hasher
   */
  public CachedUrlSet getCachedUrlSet();

  /**
   * @return the estimated time needed for this type of hash over the
   * CachedUrlSet.
   */
  public long getEstimatedHashDuration();

  /**
   * Inform the CachedUrlSet of the time actually needed to do the hash, if
   * appropriate for this type of hash.
   * @param elapsed the measured duration of a hash attempt.
   * @param err the exception that terminated the hash, or null if it
   * succeeded
   */
  public void storeActualHashDuration(long elapsed, Exception err);

  /**
   * @return a short string designating the type of has, suitable for use
   * in status displays
   */
  public String typeString();

  /**
   * @return the array of digests in this hasher.  This is for
   * compatibility with the old content and name hashers (where it returns
   * a single element array containing the single digest), so that the hash
   * done callback can get at the digest.
   */
  public MessageDigest[] getDigests();

  /**
   * Hash the next <code>numBytes</code> bytes
   * @param numBytes the number of bytes to hash
   * @return         the number of bytes hashed
   * @exception java.io.IOException on many kinds of I/O problem
   */
  public int hashStep(int numBytes) throws IOException;

  /**
   * True if there is nothing left to hash.
   * @return <code>true</code> if there is nothing left to hash.
   */
  public boolean finished();

  /**
   * Close files, etc.  Should be called if the hash will not be completed.
   */
  public void abortHash();
}
