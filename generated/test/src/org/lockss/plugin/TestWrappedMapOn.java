/*
 * $Id: TestWrappedMapOn.java,v 1.2 2004-01-27 00:41:49 tyronen Exp $
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

package org.lockss.plugin;

import java.util.*;
import junit.framework.*;
import org.lockss.test.*;
import org.lockss.plugin.wrapper.*;

/**
 * <p>Title: TestWrappedMapOn </p>
 * <p>Description: A set of unit test for the WrappedMap class.</p>
 * @author Tyrone Nicholas
 * @version 1.0
 */

public class TestWrappedMapOn extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.plugin.WrappedMap.class
  };


  Object keys[], values[];
  Set keyset, valueset;

  protected void setUp() {
    MockArchivalUnit mgfau = new MockArchivalUnit();
    MockPlugin mplug = new MockPlugin();
    MockCachedUrlSet mcus = new MockCachedUrlSet();
    MockCachedUrl mcu = new MockCachedUrl("http://www.example.com/testDir");
    Object keyarr[] = { mgfau, mplug, mcus, mcu };
    WrappedArchivalUnit wau = (WrappedArchivalUnit)WrapperState.getWrapper(mgfau);
    WrappedPlugin wplug = (WrappedPlugin)WrapperState.getWrapper(mplug);
    WrappedCachedUrlSet wcus = (WrappedCachedUrlSet)WrapperState.getWrapper(mcus);
    WrappedCachedUrl wcu = (WrappedCachedUrl)WrapperState.getWrapper(mcu);
    Object valuearr[] = { wau, wplug, wcus, wcu };
    keys = keyarr;
    values = valuearr;
    keyset = setFromArray(keys);
    valueset = setFromArray(values);
  }

  WrappedMap makeGeneric()
  {
    WrappedMap map = new WrappedMap();
    map.put(keys[0],values[0]);
    map.put(keys[1],values[1]);
    map.put(keys[2],values[2]);
    map.put(keys[3],values[3]);
    return map;
  }

  public void testClear()
  {
    WrappedMap map = makeGeneric();
    assertFalse(map.isEmpty());
    map.clear();
    assertTrue(map.isEmpty());
  }

  public void testPutGet() {
    WrappedMap map = makeGeneric();
    assertEquals(keys.length,values.length);
    for (int i=0; i<keys.length; i++)
      assertSame(map.get(keys[i]),values[i]);
  }

  public void testOverwrite() {
    WrappedMap map = makeGeneric();
    map.put(keys[1],"joe");
    String joe = (String)map.get(keys[1]);
    assertSame(joe,"joe");
  }

  public void testUpdate() {
    WrappedMap map = makeGeneric();
    map.put(keys[1],values[1]);
    assertTrue(map.containsKey(keys[1]));
  }

  public void testEqualityHash() {
    WrappedMap map = makeGeneric();
    WrappedMap map3 = makeGeneric();
    WrappedMap map2 = new WrappedMap();
    map2.putAll(map);
    assertEquals(map.hashCode(),map3.hashCode());
  }

  public void testSizeRemove() {
    WrappedMap map = makeGeneric();
    assertEquals(map.size(),keys.length);
    map.put("joe","sue");
    assertEquals(map.size(),keys.length+1);
    map.remove("joe");
    assertEquals(map.size(),keys.length);
  }

  public void testPutAll() {
    WrappedMap map = makeGeneric();
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

  private Set setFromArray(Object[] array) {
    Set set = new HashSet();
    set.addAll(Arrays.asList(array));
    return set;
  }

  void checkCollection(Collection coll,Object[] objs) {
    int loc = 0;
    Iterator it = coll.iterator();
    assertEquals(coll.size(), objs.length);
    Set objSet = setFromArray(objs);
    verifyIteratorInList(it,objSet);
  }

  public void testSets() {
    WrappedMap map = makeGeneric();

    Set mykeyset = map.keySet();
    checkCollection(mykeyset,keys);

    Collection myvaluecoll = map.values();
    checkCollection(myvaluecoll,values);
  }

  public void testEntrySet() {
    WrappedMap map = makeGeneric();
    int loc = 0;
    Set entryset = map.entrySet();
    Iterator it = entryset.iterator();
    assertEquals(entryset.size(), keys.length);
    assertEquals(entryset.size(), values.length);
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      assertTrue(keyset.contains(entry.getKey()));
      assertTrue(valueset.contains(entry.getValue()));
    }
  }

  void verifyIteratorInList(Iterator it, Collection list) {
    while (it.hasNext()) {
      assertTrue(list.contains(it.next()));
    }
  }

  public void testIterators() {
    WrappedMap map = makeGeneric();
    Iterator entryit = map.entrySet().iterator();
    Iterator keyit = map.keySet().iterator();
    Iterator valueit = map.values().iterator();
    while (entryit.hasNext()) {
      Map.Entry entry = (Map.Entry)entryit.next();
      assertTrue(keyset.contains(entry.getKey()));
      assertTrue(valueset.contains(entry.getValue()));
    }
    verifyIteratorInList(keyit,keyset);
    verifyIteratorInList(valueit,valueset);
  }

  public static Test suite() {
    return new TestSuite(TestWrappedMapOn.class);
  }
}
