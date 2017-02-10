/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

/** Accumulates results of local hash generation and checking. */

public class LocalHashResult {

  private int matchingVersions = 0;
  private int matchingUrls = 0;
  private int newlySuspectVersions = 0;
  private int newlySuspectUrls = 0;
  private int newlyHashedVersions = 0;
  private int newlyHashedUrls = 0;
  private int skippedVersions = 0;
  private int skippedUrls = 0;

  /** Record a match between computed and stored hash */
  public void match(String url, boolean isCurrent) {
    matchingVersions++;
    if (isCurrent) {
      matchingUrls++;
    }
  }

  /** Record a newly discovered mismatch between computed and stored hash */
  public void newlySuspect(String url, boolean isCurrent) {
    newlySuspectVersions++;
    if (isCurrent) {
      newlySuspectUrls++;
    }
  }

  /** Record that a hash was computed and stored */
  public void newlyHashed(String url, boolean isCurrent) {
    newlyHashedVersions++;
    if (isCurrent) {
      newlyHashedUrls++;
    }
  }

  /** Record that a CU version was skipped becuase it is already marked
   * suspect */
  public void skipped(String url, boolean isCurrent) {
    skippedVersions++;
    if (isCurrent) {
      skippedUrls++;
    }
  }

  /** Return the number of CU versions whose computed and stored hashes
   * matched */
  public int getMatchingVersions() {
    return matchingVersions;
  }

  /** Return the number of distinct URLs whose computed and stored hashes
   * matched */
  public int getMatchingUrls() {
    return matchingUrls;
  }

  /** Return the number of CU versions newly marked as suspect */
  public int getNewlySuspectVersions() {
    return newlySuspectVersions;
  }

  /** Return the number of distinct URLs newly marked as suspect */
  public int getNewlySuspectUrls() {
    return newlySuspectUrls;
  }

  /** Return the number of CU versions with newly stored hashes */
  public int getNewlyHashedVersions() {
    return newlyHashedVersions;
  }

  /** Return the number of distinct URLs with newly stored hashes */
  public int getNewlyHashedUrls() {
    return newlyHashedUrls;
  }

  /** Return the number of CU versions skipped because they're already
   * marked suspect */
  public int getSkippedVersions() {
    return skippedVersions;
  }

  /** Return the number of distinct URLs skipped because they're already
   * marked suspect */
  public int getSkippedUrls() {
    return skippedUrls;
  }

  /** Return the total number of CU versions recorded */
  public int getTotalVersions() {
    return matchingVersions + newlySuspectVersions
      + newlyHashedVersions + skippedVersions;
  }

  /** Return the total number of URLs recorded */
  public int getTotalUrls() {
    return matchingUrls + newlySuspectUrls
      + newlyHashedUrls + skippedUrls;
  }

  public String toString() {
    return String.format("[LHR: Match:%d,%d Suspect:%d,%d" +
			 " Skipped:%d,%d Hashed:%d,%d]",
			 matchingUrls, matchingVersions,
			 newlySuspectUrls, newlySuspectVersions,
			 skippedUrls, skippedVersions,
			 newlyHashedUrls, newlyHashedVersions);
  }

}
