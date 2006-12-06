/*
 * $Id: TestFixedTimedMap.java,v 1.5 2006-12-06 05:19:01 tlipkis Exp $
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

package org.lockss.util;

import java.util.*;
import junit.framework.*;
import org.lockss.test.LockssTestCase;

/**
 * <p>Title: TestFixedTimedMap </p>
 * <p>Description: A set of unit test for the FixedTimedMap class.</p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class TestFixedTimedMap extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.util.FixedTimedMap.class
  };

  protected void setUp() throws Exception {
    TimeBase.setSimulated();
    super.setUp();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  static int timeout = 60000;

  Object keys[] = { new Integer(3),"foo",new Double(4.0) };
  Object values[] = { "three","bar",new Integer(4) };

  FixedTimedMap makeGeneric() {
    FixedTimedMap map = new FixedTimedMap(timeout);
    map.put(keys[0],values[0]);
    assertTrue(timeout>10);
    TimeBase.step(timeout/10);
    map.put(keys[1],values[1]);
    map.put(keys[2],values[2]);
    return map;
  }

  public void testTimeoutValue() {
    assertEquals("timeout value must be evenly divisable by 10",
		 timeout, (timeout / 10) * 10);
  }

  public void testTimeout() {
    FixedTimedMap map = makeGeneric();
    TimeBase.step(timeout*9/10-1);
    assertSame(values[0], map.get(keys[0]));
    TimeBase.step(1);
    assertNull(map.get(keys[0]));
    assertSame(values[1], map.get(keys[1]));
    assertSame(values[2], map.get(keys[2]));
  }

  public void testClear() {
    FixedTimedMap map = makeGeneric();
    assertFalse(map.isEmpty());
    map.clear();
    assertTrue(map.isEmpty());
  }

  public void testPutGet() {
    FixedTimedMap map = makeGeneric();
    assertEquals(keys.length,values.length);
    for (int i=0; i<keys.length; i++)
      assertSame(values[i], map.get(keys[i]));
  }

  public void testOverwrite() {
    FixedTimedMap map = makeGeneric();
    assertNotEquals("joe", (String)map.get(keys[1]));
    map.put(keys[1],"joe");
    String joe = (String)map.get(keys[1]);
    assertEquals("joe", (String)map.get(keys[1]));
  }

  public void testSameDeadline() {
    FixedTimedMap map = new FixedTimedMap(1000);
    map.put("1", "A");
    map.put("2", "B");
    assertEquals("A", map.get("1"));
    assertEquals("B", map.get("2"));
    TimeBase.step(2000);
    assertEquals(null, map.get("1"));
    assertEquals(null, map.get("2"));
  }

  public void testUpdate() {
    FixedTimedMap map = makeGeneric();
    TimeBase.step(timeout-1);
    map.put(keys[1],values[1]);
    TimeBase.step(timeout-1);
    assertTrue(map.containsKey(keys[1]));
  }

  public void testEqauls() {
    FixedTimedMap map = makeGeneric();
    FixedTimedMap map2 = new FixedTimedMap(timeout);
    map2.putAll(map);
    assertEquals(map, map2);
    map.put(new Object(), "foo");
    assertNotEquals(map, map2);
    assertFalse(map.equals(null));
  }

  public void testSizeRemove() {
    FixedTimedMap map = makeGeneric();
    assertEquals(keys.length, map.size());
    map.put("joe","sue");
    assertEquals(keys.length+1, map.size());
    map.remove("joe");
    assertEquals(keys.length, map.size());
  }

  public void testPutAll() {
    FixedTimedMap map = makeGeneric();
    Map t = new HashMap();
    t.put("hack","burn");
    t.put(new Integer(18),"eighteen");
    Integer eight = new Integer(8);
    t.put(new Float(8.8),eight);
    map.putAll(t);
    t = null;
    assertEquals("burn", map.get("hack"));
    assertEquals(keys.length+3, map.size());
    assertSame(eight, map.get(new Float(8.8)));
  }

  void checkCollection(Collection coll,Object[] objs) {
    int loc = 0;
    Iterator it = coll.iterator();
    assertEquals(coll.size(), objs.length);
    while (it.hasNext()) {
      assertSame(objs[loc++], it.next());
    }
  }
  public void testSets() {
    FixedTimedMap map = makeGeneric();

    Set keyset = map.keySet();
    checkCollection(keyset,keys);

    Collection valuecoll = map.values();
    checkCollection(valuecoll,values);
  }

  public void testEntrySet() {
    FixedTimedMap map = makeGeneric();
    int loc = 0;
    Set entryset = map.entrySet();
    Iterator it = entryset.iterator();
    assertEquals(keys.length, entryset.size());
    assertEquals(values.length, entryset.size());
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      assertSame(keys[loc], entry.getKey());
      assertSame(values[loc++], entry.getValue());
    }
  }

  // force the map to update
  private void updateMap(Map map) {
    map.get("");
  }

  public void testSetsUnmodifiable() {
    FixedTimedMap map = makeGeneric();
    assertUnmodifiable(map.keySet());
    assertUnmodifiable(map.entrySet());
    assertUnmodifiable(map.values());
  }

  public void testEntryIteratorExpiry() {
    FixedTimedMap map = makeGeneric();

    // timeout before getting set should not cause exception
    TimeBase.step(timeout*9/10);
    updateMap(map);
    Iterator entryit = map.entrySet().iterator();
    Object obj = entryit.next();
    TimeBase.step(timeout);
    updateMap(map);
    try {
      obj = entryit.next();
      fail("Should have thrown ConcurrentModificationException");
    } catch (ConcurrentModificationException e) {
    }
  }

  public void testKeyIteratorExpiry() {
    FixedTimedMap map = makeGeneric();

    TimeBase.step(timeout*9/10);
    updateMap(map);
    Iterator keyit = map.keySet().iterator();
    Object obj = keyit.next();
    TimeBase.step(timeout);
    updateMap(map);
    try {
      obj = keyit.next();
      fail("Should have thrown ConcurrentModificationException");
    } catch (ConcurrentModificationException e) {
    }
  }

  public void testValueIteratorExpiry() {
    FixedTimedMap map = makeGeneric();

    TimeBase.step(timeout*9/10);
    updateMap(map);
    Iterator valueit = map.values().iterator();
    Object obj = valueit.next();
    TimeBase.step(timeout);
    updateMap(map);
    try {
      obj = valueit.next();
      fail("Should have thrown ConcurrentModificationException");
    } catch (ConcurrentModificationException e) {
    }
  }

  void verifyIteratorInList(Iterator it, List list) {
    while (it.hasNext()) {
      assertTrue(list.contains(it.next()));
    }
  }

  public void testIterators() {
    FixedTimedMap map = makeGeneric();
    Iterator entryit = map.entrySet().iterator();
    Iterator keyit = map.keySet().iterator();
    Iterator valueit = map.values().iterator();
    List keylist = Arrays.asList(keys);
    List valuelist = Arrays.asList(values);
    while (entryit.hasNext()) {
      Map.Entry entry = (Map.Entry)entryit.next();
      assertTrue(keylist.contains(entry.getKey()));
      assertTrue(valuelist.contains(entry.getValue()));
    }
    verifyIteratorInList(keyit,keylist);
    verifyIteratorInList(valueit,valuelist);
  }

  public static Test suite() {
    return new TestSuite(TestFixedTimedMap.class);
  }
}
