/*
 * $Id: TestTimedHashMap.java,v 1.2 2003-05-22 20:49:34 tyronen Exp $
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

package org.lockss.util;

import java.util.*;
import junit.framework.*;
import org.lockss.test.LockssTestCase;

/**
 * <p>Title: TestTimedHashMap </p>
 * <p>Description: A set of unit test for the TimedHashMap class.</p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class TestTimedHashMap extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.util.TimedHashMap.class
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

  TimedHashMap makeGeneric()
  {
    TimedHashMap map = new TimedHashMap(timeout);
    map.put(keys[0],values[0]);
    assertTrue(timeout>10);
    TimeBase.step(timeout/10);
    map.put(keys[1],values[1]);
    map.put(keys[2],values[2]);
    return map;
  }

  public void testTimeout()
  {
    TimedHashMap map = makeGeneric();
    TimeBase.step(timeout*9/10-1);
    Object value = map.get(keys[0]);
    assertSame(value,values[0]);
    TimeBase.step(1);
    value = map.get(keys[0]);
    assertNull(value);
  }

  public void testClear()
  {
    TimedHashMap map = makeGeneric();
    assertFalse(map.isEmpty());
    map.clear();
    assertTrue(map.isEmpty());
  }

  public void testPutGet() {
    TimedHashMap map = makeGeneric();
    assertEquals(keys.length,values.length);
    for (int i=0; i<keys.length; i++)
      assertEquals(map.get(keys[i]),values[i]);
  }

  public void testOverwrite() {
    TimedHashMap map = makeGeneric();
    map.put(keys[1],"joe");
    String joe = (String)map.get(keys[1]);
    assertSame(joe,"joe");
  }

  public void testUpdate() {
    TimedHashMap map = makeGeneric();
    TimeBase.step(timeout-1);
    map.put(keys[1],values[1]);
    TimeBase.step(timeout-1);
    assertTrue(map.containsKey(keys[1]));
  }

  public void testEqualityHash() {
    TimeBase.setSimulated();
    TimedHashMap map = makeGeneric();
    TimeBase.setSimulated();
    TimedHashMap map3 = makeGeneric();
    TimedHashMap map2 = new TimedHashMap(timeout);
    map2.putAll(map);
    /*assertNotEquals(map,map2);
    assertEquals(map,map3);*/
    assertEquals(map.hashCode(),map3.hashCode());
  }

  public void testSizeRemove() {
    TimedHashMap map = makeGeneric();
    assertEquals(map.size(),keys.length);
    map.put("joe","sue");
    assertEquals(map.size(),keys.length+1);
    map.remove("joe");
    assertEquals(map.size(),keys.length);
  }

  public void testPutAll() {
    TimedHashMap map = makeGeneric();
    Map t = new HashMap();
    t.put("hack","burn");
    t.put(new Integer(18),"eighteen");
    Integer eight = new Integer(8);
    t.put(new Float(8.8),eight);
    map.putAll(t);
    t = null;
    assertSame(map.get("hack"),"burn");
    assertEquals(map.size(),keys.length+3);
    assertSame(map.get(new Float(8.8)),eight);
  }


  void checkCollection(Collection coll,Object[] objs) {
    int loc = 0;
    Iterator it = coll.iterator();
    assertEquals(coll.size(), objs.length);
    while (it.hasNext()) {
      assertEquals(it.next(), objs[loc++]);
    }
  }
  public void testSets() {
    TimedHashMap map = makeGeneric();

    Set keyset = map.keySet();
    checkCollection(keyset,keys);

    Collection valuecoll = map.values();
    checkCollection(valuecoll,values);
  }

  public void testEntrySet() {
    TimedHashMap map = makeGeneric();
    int loc = 0;
    Set entryset = map.entrySet();
    Iterator it = entryset.iterator();
    assertEquals(entryset.size(), keys.length);
    assertEquals(entryset.size(), values.length);
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      assertEquals(entry.getKey(), keys[loc]);
      assertEquals(entry.getValue(), values[loc++]);
    }
  }

  public static Test suite() {
    return new TestSuite(TestTimedHashMap.class);
  }
}