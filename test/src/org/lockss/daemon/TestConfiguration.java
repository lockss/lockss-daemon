/*
 * $Id: TestConfiguration.java,v 1.23 2003-07-21 08:33:12 tlipkis Exp $
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
import java.io.*;
import java.net.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * Test class for <code>org.lockss.util.Configuration</code>
 */

public class TestConfiguration extends LockssTestCase {
  public static Class testedClasses[] = {
    org.lockss.daemon.Configuration.class
  };

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  static Logger log = Logger.getLogger("TestConfig");

  private static final String c1 = "prop1=12\nprop2=foobar\nprop3=true\n" +
    "prop5=False\n";
  private static final String c1a = "prop2=xxx\nprop4=yyy\n";

  private Configuration newConfiguration() {
    return new ConfigurationPropTreeImpl();
  }

  public void testSet() {
    Configuration config = newConfiguration();
    assertEquals(0, config.keySet().size());
    config.put("a", "b");
    assertEquals(1, config.keySet().size());
    assertEquals("b", config.get("a"));
  }    

  public void testRemove() {
    Configuration config = newConfiguration();
    config.put("a", "1");
    config.put("b", "2");
    assertEquals(2, config.keySet().size());
    assertEquals("1", config.get("a"));
    assertEquals("2", config.get("b"));
    config.remove("a");
    assertEquals(1, config.keySet().size());
    assertEquals(null, config.get("a"));
    assertEquals("2", config.get("b"));
  }    

  public void testSeal() {
    Configuration config = newConfiguration();
    config.put("a", "1");
    config.put("b", "2");
    config.seal();
    try {
      config.put("b", "3");
      fail("put into sealed config should throw IllegalStateException");
    } catch (IllegalStateException e) {
    }
    try {
      config.remove("a");
      fail("remove from sealed config should throw IllegalStateException");
    } catch (IllegalStateException e) {
    }
    assertEquals(2, config.keySet().size());
    assertEquals("1", config.get("a"));
    assertEquals("2", config.get("b"));
  }    

  public void testLoad() throws IOException, Configuration.InvalidParam {
    String f = FileUtil.urlOfString(c1);
    Configuration config = newConfiguration();
    config.load(f);
    assertEquals("12", config.get("prop1"));
    assertEquals("12", config.get("prop1", "wrong"));
    assertEquals("foobar", config.get("prop2"));
    assertEquals("not", config.get("propnot", "not"));

    assertTrue(config.getBoolean("prop3"));
    assertTrue(config.getBoolean("prop3", false));
    assertFalse(config.getBoolean("prop1", false));
    assertFalse(config.getBoolean("prop5"));
    assertFalse(config.getBoolean("prop5", true));
    assertEquals(12, config.getInt("prop1"));
    assertEquals(42, config.getInt("propnot", 42));
    assertEquals(123, config.getInt("prop2", 123));
    try {
      config.getBoolean("prop1");
      fail("getBoolean(non-boolean) didn't throw");
    } catch (Configuration.InvalidParam e) {
    }
    try {
      config.getBoolean("propnot");
      fail("getBoolean(missing) didn't throw");
    } catch (Configuration.InvalidParam e) {
    }
    try {
      config.getInt("prop2");
      fail("getInt(non-int) didn't throw");
    } catch (Configuration.InvalidParam e) {
    }
    try {
      config.getInt("propnot");
      fail("getInt(missing) didn't throw");
    } catch (Configuration.InvalidParam e) {
    }
    assertTrue(config.containsKey("prop1"));
    assertFalse( config.containsKey("propnot"));
  }

  public void testLoadList() throws IOException {
    Configuration config = newConfiguration();
    config.loadList(ListUtil.list(FileUtil.urlOfString(c1),
				  FileUtil.urlOfString(c1a)));
    assertEquals("12", config.get("prop1"));
    assertEquals("xxx", config.get("prop2"));
    assertTrue(config.getBoolean("prop3", false));
    assertEquals("yyy", config.get("prop4"));
  }

  private static final String c2 =
    "timeint=14d\n" +
    "prop.p1=12\n" +
    "prop.p2=foobar\n" +
    "prop.p3.a=true\n" +
    "prop.p3.b=false\n" +
    "otherprop.p3.b=foo\n";

  private static HashMap m2 = new HashMap();
  static {
    m2.put("prop.p1", "12");
    m2.put("prop.p2", "foobar");
    m2.put("timeint", "14d");
    m2.put("prop.p3.a", "true");
    m2.put("prop.p3.b", "false");
    m2.put("otherprop.p3.b", "foo");
  };
  private static HashMap m2a = new HashMap();
  static {
    m2a.put("p1", "12");
    m2a.put("p2", "foobar");
    m2a.put("p3.a", "true");
    m2a.put("p3.b", "false");
  };

  private Map mapFromIter(Iterator iter, Configuration config) {
    Map map = new HashMap();
    while (iter.hasNext()) {
      String key = (String)iter.next();
      map.put(key, config.get(key));
    }
    return map;
  }

  private Map mapFromConfig(Configuration config) {
    return mapFromIter(config.keyIterator(), config);
  }

  public void testStruct() throws IOException {
    Configuration config = newConfiguration();
    config.load(FileUtil.urlOfString(c2));
    Set set = new HashSet();
    for (Iterator iter = config.keyIterator(); iter.hasNext();) {
      set.add(iter.next());
    }
    assertEquals(6, set.size());
    assertEquals(m2.keySet(), set);
    {
      Map map = mapFromConfig(config);
      assertEquals(6, map.size());
      assertEquals(m2, map);
    }
    {
      Map map = mapFromIter(config.nodeIterator(), config);
      assertEquals(3, map.size());
    }
    Configuration conf2 = config.getConfigTree("prop");
    {
      Map map = mapFromConfig(conf2);
      assertEquals(4, map.size());
      assertEquals(m2a, map);
    }
  }

  public void testPercentage() throws Exception {
    Properties props = new Properties();
    props.put("p1", "-1");
    props.put("p2", "0");
    props.put("p3", "20");
    props.put("p4", "100");
    props.put("p5", "101");
    props.put("p6", "foo");
    Configuration config = ConfigurationUtil.fromProps(props);
    assertEquals(0.0, config.getPercentage("p2"), 0.0);
    assertEquals(0.2, config.getPercentage("p3"), 0.0000001);
    assertEquals(1.0, config.getPercentage("p4"), 0.0);
    assertEquals(0.1, config.getPercentage("p1", 0.1), 0.0000001);
    assertEquals(0.5, config.getPercentage("p6", 0.5), 0.0);
    assertEquals(0.1, config.getPercentage("p1", 0.1), 0.0000001);

    try {
      config.getPercentage("p1");
      fail("getPercentage(-1) should throw");
    } catch (Configuration.InvalidParam e) {
    }
    try {
      config.getPercentage("p5");
      fail("getPercentage(101) should throw");
    } catch (Configuration.InvalidParam e) {
    }
    try {
      config.getPercentage("p6");
      fail("getPercentage(foo) should throw");
    } catch (Configuration.InvalidParam e) {
    }
  }

  public void testAddPrefix() throws Exception {
    Properties props = new Properties();
    props.put("p1", "a");
    props.put("p2", "b");
    Configuration c1 = ConfigurationUtil.fromProps(props);
    Configuration c2 = c1.addPrefix("a");
    Configuration c3 = c1.addPrefix("foo.bar.");
    assertEquals(2, c2.keySet().size());
    assertEquals("a", c2.get("a.p1"));
    assertEquals("b", c2.get("a.p2"));
    assertEquals(2, c3.keySet().size());
    assertEquals("a", c3.get("foo.bar.p1"));
    assertEquals("b", c3.get("foo.bar.p2"));
  }
}
