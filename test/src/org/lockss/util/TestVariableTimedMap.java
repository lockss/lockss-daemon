/*
 * $Id: TestVariableTimedMap.java,v 1.5 2006-12-06 05:19:01 tlipkis Exp $
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

import java.lang.reflect.*;
import java.util.*;
import junit.framework.*;
import org.lockss.test.LockssTestCase;

/**
 * <p>Title: TestVariableTimedMap </p>
 * <p>Description: A set of unit test for the VariableTimedMap class.</p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class TestVariableTimedMap extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.util.VariableTimedMap.class
  };

  protected void setUp() throws Exception {
    super.setUp();
    TimeBase.setSimulated();
  }

  public void tearDown() throws Exception {
    TimeBase.setReal();
    super.tearDown();
  }

  Object keys[] = { new Integer(3),"foo",new Double(4.0), new Float(4.3) };
  Object values[] = { "three","bar",new Integer(4), new Boolean(true) };
  int timeouts[] = { 10000, 20000, 30000, 40000 };


  VariableTimedMap makeGeneric()
  {
    VariableTimedMap map = new VariableTimedMap();
    for (int var=0; var<Array.getLength(keys); var++) {
      map.put(keys[var],values[var],timeouts[var]);
    }
    return map;

  }

  public void testTimeout()
  {
    VariableTimedMap map = makeGeneric();
    assertSame(values[0], map.get(keys[0]));
    TimeBase.step(timeouts[0]-1);
    assertSame(values[0], map.get(keys[0]));
    TimeBase.step(1);
    assertFalse(map.containsKey(keys[0]));
    assertNull(map.get(keys[0]));
    assertSame(values[1], map.get(keys[1]));
    assertEquals(keys.length - 1, map.size());
  }

  public void testClear()
  {
    VariableTimedMap map = makeGeneric();
    assertFalse(map.isEmpty());
    map.clear();
    assertTrue(map.isEmpty());
  }

  public void testPutGet() {
    VariableTimedMap map = makeGeneric();
    assertEquals(keys.length,values.length);
    for (int i=0; i<keys.length; i++)
      assertEquals(values[i], map.get(keys[i]));
  }

  public void testEqauls() {
    VariableTimedMap map = makeGeneric();
    VariableTimedMap map2 = new VariableTimedMap();
    map2.putAll(map, 1000);
    assertEquals(map, map2);
    map.put(new Object(), "foo", 1);
    assertNotEquals(map, map2);
    assertFalse(map.equals(null));
  }

  public void testPutAll() {
    VariableTimedMap map = makeGeneric();
    Map t = new HashMap();
    t.put("hack","burn");
    t.put(new Integer(18),"eighteen");
    Integer eight = new Integer(8);
    t.put(new Float(8.8),eight);
    map.putAll(t, 1000);
    t = null;
    assertEquals("burn", map.get("hack"));
    assertEquals(keys.length+3, map.size());
    assertSame(eight, map.get(new Float(8.8)));
  }

  public void testOverwrite() {
    VariableTimedMap map = makeGeneric();
    map.put(keys[1],"joe",timeouts[1]);
    assertSame("joe", map.get(keys[1]));
  }

  public void testSameDeadline() {
    VariableTimedMap map = new VariableTimedMap();
    map.put("1", "A", 1000);
    map.put("2", "B", 1000);
    assertEquals("A", map.get("1"));
    assertEquals("B", map.get("2"));
    TimeBase.step(2000);
    assertEquals(null, map.get("1"));
    assertEquals(null, map.get("2"));
    assertEmpty(map);
  }

  public void testUpdate() {
    VariableTimedMap map = makeGeneric();
    TimeBase.step(timeouts[0]-1);
    map.put(keys[0],values[0],timeouts[0]);
    TimeBase.step(timeouts[0]-1);
    assertTrue(map.containsKey(keys[0]));
  }

  public void testSizeRemove() {
    VariableTimedMap map = makeGeneric();
    assertEquals(keys.length, map.size());
    map.put("joe","sue",10000);
    assertEquals(keys.length+1, map.size());
    map.remove("joe");
    assertEquals(keys.length, map.size());
  }

  public void testSets() {
    VariableTimedMap map = makeGeneric();

    assertEquals(SetUtil.fromArray(keys), map.keySet());
    assertEquals(SetUtil.fromArray(values), SetUtil.theSet(map.values()));
  }

  public void testEntrySet() {
    VariableTimedMap map = makeGeneric();
    Set entryset = map.entrySet();
    assertEquals(keys.length, entryset.size());
    assertEquals(values.length, entryset.size());
    List keylist = new ArrayList();
    List valuelist = new ArrayList();
    for (Iterator it = entryset.iterator(); it.hasNext(); ) {
      Map.Entry entry = (Map.Entry) it.next();
      keylist.add(entry.getKey());
      valuelist.add(entry.getValue());
    }
    assertEquals(SetUtil.fromArray(keys), SetUtil.theSet(keylist));
    assertEquals(SetUtil.fromArray(values), SetUtil.theSet(valuelist));
  }

  public void testSetsUnmodifiable() {
    VariableTimedMap map = makeGeneric();
    assertUnmodifiable(map.keySet());
    assertUnmodifiable(map.entrySet());
    assertUnmodifiable(map.values());
  }

  // force the map to update
  private void updateMap(Map map) {
    map.get("");
  }

  public void testEntryIteratorExpiry() {
    VariableTimedMap map = makeGeneric();

    // timeout before getting set should not cause exception
    TimeBase.step(timeouts[0] + 1);
    updateMap(map);
    Iterator entryit = map.entrySet().iterator();
    Object obj = entryit.next();
    TimeBase.step(timeouts[1] + 1);
    updateMap(map);
    try {
      obj = entryit.next();
      fail("Should have thrown ConcurrentModificationException");
    } catch (ConcurrentModificationException e) {
    }
  }

  public void testKeyIteratorExpiry() {
    VariableTimedMap map = makeGeneric();

    TimeBase.step(timeouts[0] + 1);
    updateMap(map);
    Iterator keyit = map.keySet().iterator();
    Object obj = keyit.next();
    TimeBase.step(timeouts[1] + 1);
    updateMap(map);
    try {
      obj = keyit.next();
      fail("Should have thrown ConcurrentModificationException");
    } catch (ConcurrentModificationException e) {
    }
  }

  public void testValueIteratorExpiry() {
    VariableTimedMap map = makeGeneric();

    TimeBase.step(timeouts[0] + 1);
    updateMap(map);
    Iterator valueit = map.values().iterator();
    Object obj = valueit.next();
    TimeBase.step(timeouts[1] + 1);
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
    VariableTimedMap map = makeGeneric();
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
    return new TestSuite(TestVariableTimedMap.class);
  }
}
