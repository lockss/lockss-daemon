/*
 * $Id: TestVariableTimedMap.java,v 1.2 2003-06-20 22:34:57 claire Exp $
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
    TimeBase.setSimulated();
    super.setUp();
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
    TimeBase.step(timeouts[0]-1);
    Object value = map.get(keys[0]);
    assertSame(value,values[0]);
    TimeBase.step(1);
    value = map.get(keys[0]);
    assertNull(value);
    assertSame(map.get(keys[1]),values[1]);
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
      assertEquals(map.get(keys[i]),values[i]);
  }

  public void testOverwrite() {
    VariableTimedMap map = makeGeneric();
    map.put(keys[1],"joe",timeouts[1]);
    String joe = (String)map.get(keys[1]);
    assertSame(joe,"joe");
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
    assertEquals(map.size(),keys.length);
    map.put("joe","sue",10000);
    assertEquals(map.size(),keys.length+1);
    map.remove("joe");
    assertEquals(map.size(),keys.length);
  }

  void checkCollection(Collection coll,Object[] objs) {
    int loc = 0;
    Iterator it = coll.iterator();
    assertEquals(coll.size(), objs.length);
    for (loc=0; loc<objs.length; loc ++) {
      assertTrue(coll.contains(objs[loc]));
    }
  }

  public void testSets() {
    VariableTimedMap map = makeGeneric();

    Set keyset = map.keySet();
    checkCollection(keyset,keys);

    Collection valuecoll = map.values();
    checkCollection(valuecoll,values);
  }

  public void testEntrySet() {
    VariableTimedMap map = makeGeneric();
    int loc = 0;
    Set entryset = map.entrySet();
    Iterator it = entryset.iterator();
    assertEquals(entryset.size(), keys.length);
    assertEquals(entryset.size(), values.length);
    List keylist = Arrays.asList(keys);
    List valuelist = Arrays.asList(values);
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      assertTrue(keylist.contains(entry.getKey()));
      assertTrue(valuelist.contains(entry.getValue()));
    }
  }

  public void testIteratorExpiry() {
    VariableTimedMap map = makeGeneric();
    Set entryset = map.entrySet();
    Iterator entryit = entryset.iterator();
    TimeBase.step(timeouts[0] + 1);
    Object obj;
    try {
      obj = entryit.next();
      fail("Should have thrown TimedIteratorExpiredException");
    } catch (TimedIteratorExpiredException e) {
      Iterator keyit = map.keySet().iterator();
      TimeBase.step(timeouts[1] + 1);
      try {
        obj = keyit.next();
        fail("Should have thrown TimedIteratorExpiredException");
      } catch (TimedIteratorExpiredException f) {
        Iterator valueit = map.values().iterator();
        TimeBase.step(timeouts[2] + 1);
        try {
          obj = valueit.next();
          fail("Should have thrown TimedIteratorExpiredException");
        } catch (TimedIteratorExpiredException g) {
        }
      }
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