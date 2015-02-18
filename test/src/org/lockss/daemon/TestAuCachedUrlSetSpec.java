/*
 * $Id$
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

package org.lockss.daemon;
import java.util.*;
import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.AuCachedUrlSetSpec
 */

public class TestAuCachedUrlSetSpec extends LockssTestCase {
  public TestAuCachedUrlSetSpec(String msg){
    super(msg);
  }

  public void testGetUrl() {
    CachedUrlSetSpec cuss = new AuCachedUrlSetSpec();
    assertEquals("lockssau:", cuss.getUrl());
  }

  public void testMatches() {
    CachedUrlSetSpec cuss = new AuCachedUrlSetSpec();
    assertTrue(cuss.matches("anything"));
    assertTrue(cuss.matches(""));
  }

  public void testEquals() {
    CachedUrlSetSpec cuss = new AuCachedUrlSetSpec();
    CachedUrlSetSpec cuss2 = new AuCachedUrlSetSpec();
    assertEquals(cuss, cuss2);
    assertNotEquals(cuss, new SingleNodeCachedUrlSetSpec("foo"));
    assertNotEquals(cuss, new RangeCachedUrlSetSpec("foo"));
  }

  public void testHashCode() {
    CachedUrlSetSpec cuss = new AuCachedUrlSetSpec();
    CachedUrlSetSpec cuss2 = new AuCachedUrlSetSpec();
    assertEquals(cuss.hashCode(), cuss2.hashCode());
  }

  public void testTypePredicates() {
    CachedUrlSetSpec cuss = new AuCachedUrlSetSpec();
    assertTrue(cuss.isAu());
    assertFalse(cuss.isSingleNode());
    assertFalse(cuss.isRangeRestricted());
  }

  public void testDisjoint() {
    CachedUrlSetSpec cuss = new AuCachedUrlSetSpec();
    assertFalse(cuss.isDisjoint(new AuCachedUrlSetSpec()));
    assertFalse(new AuCachedUrlSetSpec().isDisjoint(cuss));
    assertFalse(cuss.isDisjoint(new RangeCachedUrlSetSpec("foo")));
    assertFalse(new RangeCachedUrlSetSpec("foo").isDisjoint(cuss));
    assertFalse(cuss.isDisjoint(new RangeCachedUrlSetSpec("foo", "1", "2")));
    assertFalse(new RangeCachedUrlSetSpec("foo", "1", "2").isDisjoint(cuss));
    assertFalse(cuss.isDisjoint(new SingleNodeCachedUrlSetSpec("foo")));
    assertFalse(new SingleNodeCachedUrlSetSpec("foo").isDisjoint(cuss));
  }

  public void testSubsumes() {
    CachedUrlSetSpec cuss = new AuCachedUrlSetSpec();
    assertTrue(cuss.subsumes(new AuCachedUrlSetSpec()));
    assertTrue(new AuCachedUrlSetSpec().subsumes(cuss));

    assertTrue(cuss.subsumes(new RangeCachedUrlSetSpec("foo")));
    assertFalse(new RangeCachedUrlSetSpec("foo").subsumes(cuss));

    assertTrue(cuss.subsumes(new RangeCachedUrlSetSpec("foo", "1", "2")));
    assertFalse(new RangeCachedUrlSetSpec("foo", "1", "2").subsumes(cuss));

    assertTrue(cuss.subsumes(new SingleNodeCachedUrlSetSpec("foo")));
    assertFalse(new SingleNodeCachedUrlSetSpec("foo").subsumes(cuss));
  }

}
