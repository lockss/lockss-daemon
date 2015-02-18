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


package org.lockss.state;

import java.util.*;
import org.lockss.test.*;
import org.lockss.plugin.*;
import org.lockss.util.ExtMapBean;

/**
 * DamagedNodeMap is a write-through persistent wrapper for a hashmap and a set.
 * It stores a Set of nodes with damage and a map of CachedUrlSets which need
 * repair.
 */
public class TestDamagedNodeSet extends LockssTestCase {

//   public void setUp() {

//   }

  public void testHasDamage() {
    DamagedNodeSet dns = new DamagedNodeSet(new MockArchivalUnit(),
					    new MockHistoryRepository());
    dns.addToDamage("http://www.example.com/");
    assertTrue(dns.hasDamage("http://www.example.com/"));
  }

  public void testDoesnotWriteDamageIfNoChange() {
    MockHistoryRepository histRep = new MockHistoryRepository();
    DamagedNodeSet dns = new DamagedNodeSet(new MockArchivalUnit(), histRep);
    assertEquals(0, histRep.timesStoreDamagedNodeSetCalled());
    dns.addToDamage("http://www.example.com/");
    assertEquals(1, histRep.timesStoreDamagedNodeSetCalled());
    dns.addToDamage("http://www.example.com/");
    assertEquals(1, histRep.timesStoreDamagedNodeSetCalled());
    assertTrue(dns.hasDamage("http://www.example.com/"));

    dns.removeFromDamage("http://www.example.com/");
    assertEquals(2, histRep.timesStoreDamagedNodeSetCalled());
    dns.removeFromDamage("http://www.example.com/");
    assertEquals(2, histRep.timesStoreDamagedNodeSetCalled());

    assertFalse(dns.hasDamage("http://www.example.com/"));
  }

}
