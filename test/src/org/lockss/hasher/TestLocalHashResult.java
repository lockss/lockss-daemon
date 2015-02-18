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

import org.lockss.test.*;

public class TestLocalHashResult extends LockssTestCase {

  LocalHashResult lhr = new LocalHashResult();

  public void testEmpty() {
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());
  }

  public void testMatch() {
    lhr.match("foo", true);
    assertEquals(1, lhr.getMatchingVersions());
    assertEquals(1, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());

    lhr.match("bar", false);
    assertEquals(2, lhr.getMatchingVersions());
    assertEquals(1, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());

    lhr.match("bar", true);
    assertEquals(3, lhr.getMatchingVersions());
    assertEquals(2, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());
  }

  public void testNewlySuspect() {
    lhr.newlySuspect("foo", true);
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(1, lhr.getNewlySuspectVersions());
    assertEquals(1, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());

    lhr.newlySuspect("bar", false);
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(2, lhr.getNewlySuspectVersions());
    assertEquals(1, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());

    lhr.newlySuspect("bar", true);
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(3, lhr.getNewlySuspectVersions());
    assertEquals(2, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());
  }

  public void testNewlyHashed() {
    lhr.newlyHashed("foo", true);
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(1, lhr.getNewlyHashedVersions());
    assertEquals(1, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());

    lhr.newlyHashed("bar", false);
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(2, lhr.getNewlyHashedVersions());
    assertEquals(1, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());

    lhr.newlyHashed("bar", true);
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(3, lhr.getNewlyHashedVersions());
    assertEquals(2, lhr.getNewlyHashedUrls());
    assertEquals(0, lhr.getSkippedVersions());
    assertEquals(0, lhr.getSkippedUrls());
  }

  public void testSkipped() {
    lhr.skipped("foo", true);
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(1, lhr.getSkippedVersions());
    assertEquals(1, lhr.getSkippedUrls());

    lhr.skipped("bar", true);
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(2, lhr.getSkippedVersions());
    assertEquals(2, lhr.getSkippedUrls());

    lhr.skipped("bar", false);
    assertEquals(0, lhr.getMatchingVersions());
    assertEquals(0, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(3, lhr.getSkippedVersions());
    assertEquals(2, lhr.getSkippedUrls());
  }

  public void testMixed() {
    lhr.skipped("u1", true);
    lhr.match("u1", false);
    lhr.match("u2", true);
    lhr.skipped("u3", false);
    lhr.match("u3", true);
    lhr.match("u4", true);
    lhr.skipped("u5", false);
    lhr.match("u5", false);
    lhr.match("u6", true);

    assertEquals(6, lhr.getMatchingVersions());
    assertEquals(4, lhr.getMatchingUrls());
    assertEquals(0, lhr.getNewlySuspectVersions());
    assertEquals(0, lhr.getNewlySuspectUrls());
    assertEquals(0, lhr.getNewlyHashedVersions());
    assertEquals(0, lhr.getNewlyHashedUrls());
    assertEquals(3, lhr.getSkippedVersions());
    assertEquals(1, lhr.getSkippedUrls());
  }
}
