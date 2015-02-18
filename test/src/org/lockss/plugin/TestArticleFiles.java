/*
 * $Id$
 */

/*

Copyright (c) 2000-2011 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin;

import java.util.*;
import org.lockss.util.*;
import org.lockss.test.*;

public class TestArticleFiles extends LockssTestCase {

  public void testAccessors() {
    ArticleFiles af1 = new ArticleFiles();
    assertNull(af1.getFullTextCu());
    assertNull(af1.getRoleCu("xml"));
    assertEmpty(af1.getRoleMap());
    CachedUrl cu1 = new MockCachedUrl("full");
    af1.setFullTextCu(cu1);
    assertSame(cu1, af1.getFullTextCu());
    assertNull(af1.getRoleCu("xml"));
    assertEmpty(af1.getRoleMap());
    try {
      af1.getRoleMap().put("foo", "bar");
      fail("Role map should be unmodifiable");
    } catch (UnsupportedOperationException e) {
    }

    CachedUrl cu2 = new MockCachedUrl("xml");
    af1.setRoleCu("xml", cu2);
    assertSame(cu1, af1.getFullTextCu());
    assertSame(cu2, af1.getRoleCu("xml"));
    assertEquals("full", af1.getFullTextUrl());
    assertEquals("xml", af1.getRoleUrl("xml"));
    assertEquals(null, af1.getRoleUrl("html"));
    assertEquals(MapUtil.map("xml", cu2), af1.getRoleMap());
    try {
      af1.getRoleMap().put("foo", "bar");
      fail("Role map should be unmodifiable");
    } catch (UnsupportedOperationException e) {
    }

    af1.setRoleString("handle", "shovel");
    Map map = new HashMap();
    af1.setRole("map", map);
    assertEquals("shovel", af1.getRoleString("handle"));
    assertSame(map, af1.getRole("map"));
    assertEquals(MapUtil.map("xml", cu2,
			     "handle", "shovel",
			     "map", map), af1.getRoleMap());
  }

  public void testIsEmpty() {
    ArticleFiles af1 = new ArticleFiles();
    CachedUrl cu2 = new MockCachedUrl("xml");
    CachedUrl cu1 = new MockCachedUrl("full");

    assertTrue(af1.isEmpty());
    af1.setFullTextCu(cu1);
    assertFalse(af1.isEmpty());
    af1.setRoleCu("xml", cu2);
    assertFalse(af1.isEmpty());

    ArticleFiles af2 = new ArticleFiles();

    assertTrue(af2.isEmpty());
    af2.setRoleCu("xml", cu2);
    assertFalse(af2.isEmpty());
    af2.setFullTextCu(cu1);
    assertFalse(af2.isEmpty());
  }

}
