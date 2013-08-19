/*
 * $Id: LocalHashResult.java,v 1.2 2013-08-19 20:24:49 tlipkis Exp $
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

  private String prevMatchingUrl = null;
  private String prevSuspectUrl = null;
  private String prevHashedUrl = null;
  private String prevSkippedUrl = null;

  /** Record a match between computed and stored hash */
  public void match(String url) {
    matchingVersions++;
    if (!url.equals(prevMatchingUrl)) {
      prevMatchingUrl = url;
      matchingUrls++;
    }
  }

  /** Record a newly discovered mismatch between computed and stored hash */
  public void newlySuspect(String url) {
    newlySuspectVersions++;
    if (!url.equals(prevSuspectUrl)) {
      prevSuspectUrl = url;
      newlySuspectUrls++;
    }
  }

  /** Record that a hash was computed and stored */
  public void newlyHashed(String url) {
    newlyHashedVersions++;
    if (!url.equals(prevHashedUrl)) {
      prevHashedUrl = url;
      newlyHashedUrls++;
    }
  }

  /** Record that a CU version was skipped becuase it is already marked
   * suspect */
  public void skipped(String url) {
    skippedVersions++;
    if (!url.equals(prevHashedUrl)) {
      prevHashedUrl = url;
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

  public String toString() {
    return String.format("[LHR: Match:%d,%d Suspect:%d,%d" +
			 " Skipped:%d,%d Hashed:%d,%d]",
			 matchingUrls, matchingVersions,
			 newlySuspectUrls, newlySuspectVersions,
			 skippedUrls, skippedVersions,
			 newlyHashedUrls, newlyHashedVersions);
  }

}
