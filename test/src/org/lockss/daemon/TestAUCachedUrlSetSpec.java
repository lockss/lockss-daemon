/*
 * $Id: TestAUCachedUrlSetSpec.java,v 1.2 2003-06-03 01:52:50 tal Exp $
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

package org.lockss.daemon;
import java.util.*;
import junit.framework.TestCase;
import gnu.regexp.*;
import org.lockss.daemon.*;
import org.lockss.test.*;
import org.lockss.util.*;

/**
 * This is the test class for org.lockss.daemon.AUCachedUrlSetSpec
 */

public class TestAUCachedUrlSetSpec extends LockssTestCase {
  public TestAUCachedUrlSetSpec(String msg){
    super(msg);
  }

  public void testAUCachedUrlSetSpec() {
    CachedUrlSetSpec cuss = new AUCachedUrlSetSpec();
    CachedUrlSetSpec cuss2 = new AUCachedUrlSetSpec();
    assertEquals("lockssau:", cuss.getUrl());
    assertTrue(cuss.matches("anything"));
    assertEquals(cuss, cuss2);
    assertEquals(cuss.hashCode(), cuss2.hashCode());
  }

  public void testTypePredicates() {
    CachedUrlSetSpec cuss = new AUCachedUrlSetSpec();
    assertTrue(cuss.isAU());
    assertFalse(cuss.isSingleNode());
    assertFalse(cuss.isRangeRestricted());
  }

  public void testDisjoint() {
    CachedUrlSetSpec cuss1 = new AUCachedUrlSetSpec();
    assertFalse(cuss1.isDisjoint(new AUCachedUrlSetSpec()));
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("foo")));
    assertFalse(cuss1.isDisjoint(new RangeCachedUrlSetSpec("foo", "1", "2")));
    assertFalse(cuss1.isDisjoint(new SingleNodeCachedUrlSetSpec("foo")));
  }

  public void testSubsumes() {
    CachedUrlSetSpec cuss1 = new AUCachedUrlSetSpec();
    assertTrue(cuss1.subsumes(new AUCachedUrlSetSpec()));
    assertTrue(cuss1.subsumes(new RangeCachedUrlSetSpec("foo")));
    assertTrue(cuss1.subsumes(new RangeCachedUrlSetSpec("foo", "1", "2")));
    assertTrue(cuss1.subsumes(new SingleNodeCachedUrlSetSpec("foo")));
  }
}
