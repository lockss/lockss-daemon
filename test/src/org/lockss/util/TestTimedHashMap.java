package org.lockss.util;

import junit.framework.*;
import org.lockss.test.LockssTestCase;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class TestTimedHashMap extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.util.TimedHashMap.class
  };

  public TestTimedHashMap(String msg) {
    super(msg);
    TimeBase.setSimulated();
  }

  /* Things to test for:

  - Does it implement the Map interface properly?  maybe can find a canned
  test

  - Do old elements expire on time?  */

  static int timeout = 60000;

  public void testTimeout()
  {
    TimedHashMap map = new TimedHashMap(timeout);
    /* put an element in, check  */
    Integer three = new Integer(3);
    map.put(three,"foo");
    TimeBase.step(timeout-1);
    Object value = map.get(new Integer(3));
    assertEquals(value,"foo");
    TimeBase.step(1);
    value = map.get(new Integer(3));
    assertNull(value);
  }

  TimedHashMap makeGeneric()
  {
    TimedHashMap map = new TimedHashMap(timeout);
    Integer three = new Integer(3);
    map.put(three,"three");
    TimeBase.step(timeout/10);
    map.put("foo","bar");
    map.put(new Double(4.0),new Integer(4));
    return map;
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
    String bar = (String)map.get("foo");
    assertEquals(bar,"bar");
    Integer four = (Integer)map.get(new Double(4.0));
    assertEquals(four,new Integer(4));
  }

  public void testOverwrite() {
    TimedHashMap map = makeGeneric();
    map.put("foo","joe");
    String joe = (String)map.get("foo");
    assertEquals(joe,"joe");
  }

  public void testUpdate() {
    TimedHashMap map = makeGeneric();
    TimeBase.step(timeout-1);
    map.put("foo","bar");
    TimeBase.step(timeout-1);
    assertTrue(map.containsKey("foo"));
  }

  public void testEqualityHash() {
    TimeBase.setSimulated();
    TimedHashMap map = makeGeneric();
    TimeBase.setSimulated();
    TimedHashMap map3 = makeGeneric();
    TimedHashMap map2 = new TimedHashMap(timeout);
    map2.putAll(map);
    assertNotEquals(map,map2);
    assertEquals(map,map3);
    assertEquals(map.hashCode(),map3.hashCode());
  //  assertNotEquals(map.hashCode(),map2.hashCode());
  }

  public static Test suite() {
    return new TestSuite(TestTimedHashMap.class);
  }
}