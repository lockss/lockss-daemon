/*
 * $Id: TestConfiguration.java,v 1.29 2004-05-28 04:57:31 smorabito Exp $
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

  private String m_testXml = null;

  public static Class testedClasses[] = {
    org.lockss.daemon.Configuration.class
  };

  public void setUp() throws Exception {
    super.setUp();
    m_testXml = setUpXml();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  static Logger log = Logger.getLogger("TestConfig");

  private static final String c1 = "prop1=12\nprop2=foobar\nprop3=true\n" +
    "prop5=False\n";
  private static final String c1a = "prop2=xxx\nprop4=yyy\n";

  /**
   * Set up a test XML configuration.
   * Expect:
   * Basic functionality...
   *   org.lockss.a=true
   *   org.lockss.b.c=true
   *   org.lockss.d={1,2,3,4,5}
   * Set-up for testing conditionals...
   *   org.lockss.daemon.version=1.2.8
   *   org.lockss.platform.version=135
   *   org.lockss.group=beta
   * As a result of these conditionals...
   *   org.lockss.test.a=null
   *   org.lockss.test.b=foo
   *   org.lockss.test.c=null
   *   org.lockss.test.d=foo
   *   org.lockss.test.e=foo
   *   org.lockss.test.f=null
   *   org.lockss.test.g=foo
   *   org.lockss.test.h=null
   *   org.lockss.test.i=foo
   *   org.lockss.test.j=null
   *   org.lockss.test.k=null
   *   org.lockss.test.l=foo
   *   org.lockss.test.m=foo
   *   org.lockss.test.n=null
   *   org.lockss.test.o=foo
   *   org.lockss.test.p=null
   *   org.lockss.test.q=foo
   *   org.lockss.test.r=null
   *   org.lockss.test.s=foo
   *   org.lockss.test.t=bar
   *   org.lockss.test.u=bar
   *   org.lockss.test.v=foo
   */
  private String setUpXml() {
    StringBuffer sb = new StringBuffer();
    sb.append("<lockss-config>\n");
    sb.append("  <property name=\"org.lockss\">\n");

    sb.append("    <property name=\"a\" value=\"true\"/>\n");
    sb.append("    <property name=\"b\">\n");
    sb.append("      <property name=\"c\" value=\"true\"/>\n");
    sb.append("    </property>\n");
    sb.append("    <property name=\"d\">\n");
    sb.append("      <list>\n");
    sb.append("        <value>1</value>\n");
    sb.append("        <value>2</value>\n");
    sb.append("        <value>3</value>\n");
    sb.append("        <value>4</value>\n");
    sb.append("        <value>5</value>\n");
    sb.append("      </list>\n");
    sb.append("    </property>\n");

    sb.append("    <property name=\"daemon.version\" value=\"1.2.8\"/>\n");
    sb.append("    <property name=\"platform.version\" value=\"135\"/>\n");
    sb.append("    <property name=\"platform.group\" value=\"beta\"/>\n");
    // Test equivalence of Daemon Version...
    sb.append("    <propgroup daemonVersion=\"1.2.7\">\n");
    sb.append("      <property name=\"test.a\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup daemonVersion=\"1.2.8\">\n");
    sb.append("      <property name=\"test.b\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    // Test max Daemon Version
    sb.append("    <propgroup daemonVersionMax=\"1.2.7\">\n");
    sb.append("      <property name=\"test.c\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup daemonVersionMax=\"1.2.9\">\n");
    sb.append("      <property name=\"test.d\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    // Test min Daemon Version
    sb.append("    <propgroup daemonVersionMin=\"1.2.7\">\n");
    sb.append("      <property name=\"test.e\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup daemonVersionMin=\"1.2.9\">\n");
    sb.append("      <property name=\"test.f\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    // Test max and min daemon Version
    sb.append("    <propgroup daemonVersionMin=\"1.2.7\" daemonVersionMax=\"1.2.9\">\n");
    sb.append("      <property name=\"test.g\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup daemonVersionMin=\"1.2.0\" daemonVersionMax=\"1.2.7\">\n");
    sb.append("      <property name=\"test.h\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    // Test equivalence of Platform Version...
    sb.append("    <propgroup platformVersion=\"135\">\n");
    sb.append("      <property name=\"test.i\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup platformVersion=\"136\">\n");
    sb.append("      <property name=\"test.j\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    // Test max Platform Version
    sb.append("    <propgroup platformVersionMax=\"134\">\n");
    sb.append("      <property name=\"test.k\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup platformVersionMax=\"136\">\n");
    sb.append("      <property name=\"test.l\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    // Test min Platform Version
    sb.append("    <propgroup platformVersionMin=\"134\">\n");
    sb.append("      <property name=\"test.m\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup platformVersionMin=\"136\">\n");
    sb.append("      <property name=\"test.n\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    // Test max and min Platform Version
    sb.append("    <propgroup platformVersionMin=\"134\" platformVersionMax=\"136\">\n");
    sb.append("      <property name=\"test.o\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup platformVersionMin=\"130\" platformVersionMax=\"134\">\n");
    sb.append("      <property name=\"test.p\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    // Test group
    sb.append("    <propgroup group=\"beta\">\n");
    sb.append("      <property name=\"test.q\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup group=\"dev\">\n");
    sb.append("      <property name=\"test.r\" value=\"foo\"/>\n");
    sb.append("    </propgroup>\n");
    // Test then/else
    sb.append("    <propgroup group=\"beta\">\n");
    sb.append("      <then>\n");
    sb.append("        <property name=\"test.s\" value=\"foo\"/>\n");
    sb.append("      </then>\n");
    sb.append("      <else>\n");
    sb.append("        <property name=\"test.s\" value=\"bar\"/>\n");
    sb.append("      </else>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup group=\"dev\">\n");
    sb.append("      <then>\n");
    sb.append("        <property name=\"test.t\" value=\"foo\"/>\n");
    sb.append("      </then>\n");
    sb.append("      <else>\n");
    sb.append("        <property name=\"test.t\" value=\"bar\"/>\n");
    sb.append("      </else>\n");
    sb.append("    </propgroup>\n");
    // Test a combination of conditionals
    sb.append("    <propgroup platformVersion=\"134\" group=\"beta\">\n");
    sb.append("      <then>\n");
    sb.append("        <property name=\"test.u\" value=\"foo\"/>\n");
    sb.append("      </then>\n");
    sb.append("      <else>\n");
    sb.append("        <property name=\"test.u\" value=\"bar\"/>\n");
    sb.append("      </else>\n");
    sb.append("    </propgroup>\n");
    sb.append("    <propgroup platformVersion=\"135\" group=\"beta\">\n");
    sb.append("      <then>\n");
    sb.append("        <property name=\"test.v\" value=\"foo\"/>\n");
    sb.append("      </then>\n");
    sb.append("      <else>\n");
    sb.append("        <property name=\"test.v\" value=\"bar\"/>\n");
    sb.append("      </else>\n");
    sb.append("    </propgroup>\n");

    sb.append("  </property>\n");
    sb.append("</lockss-config>\n");
    return sb.toString();
  }

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

  public void testRemoveTree() {
    Configuration config = newConfiguration();
    config.put("a", "1");
    config.put("b", "2");
    config.put("b.a", "3");
    config.put("b.a.1", "4");
    config.put("b.a.1.1", "5");
    config.put("b.a.2", "6");
    config.put("b.b", "7");
    assertEquals(7, config.keySet().size());
    config.removeConfigTree("b.a");
    assertEquals(3, config.keySet().size());
    assertEquals("1", config.get("a"));
    assertEquals("2", config.get("b"));
    assertEquals(null, config.get("b.a"));
    assertEquals(null, config.get("b.a.1"));
    // removing a non-existent tree should do nothing
    config.removeConfigTree("dkdkdk");
    assertEquals(3, config.keySet().size());
  }

  public void testCopyTree() {
    Configuration from = newConfiguration();
    from.put("a", "1");
    from.put("b", "2");
    from.put("b.a", "3");
    from.put("b.a.1", "4");
    from.put("b.a.1.1", "5");
    from.put("b.a.2", "6");
    from.put("b.b", "7");
    Configuration to = newConfiguration();
    to.put("a", "2");
    to.copyConfigTreeFrom(from, "b.a");
    assertEquals(5, to.keySet().size());
    assertEquals("2", to.get("a"));
    assertEquals("3", to.get("b.a"));
    assertEquals("4", to.get("b.a.1"));
    assertEquals("5", to.get("b.a.1.1"));
    assertEquals("6", to.get("b.a.2"));
  }

  private void assertSealed(Configuration config) {
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
    try {
      config.removeConfigTree("a");
      fail("remove from sealed config should throw IllegalStateException");
    } catch (IllegalStateException e) {
    }
  }

  public void testSeal() {
    Configuration config = newConfiguration();
    config.put("a", "1");
    config.put("b", "2");
    config.put("b.x", "3");
    config.seal();
    assertSealed(config);
    assertEquals(3, config.keySet().size());
    assertEquals("1", config.get("a"));
    assertEquals("2", config.get("b"));
    // check that subconfig of sealed config is sealed
    assertSealed(config.getConfigTree("b"));
  }

  public void testCopy() {
    Configuration c1 = newConfiguration();
    c1.put("a", "1");
    c1.put("b", "2");
    c1.put("b.x", "3");
    c1.seal();
    Configuration c2 = c1.copy();
    assertEquals(3, c2.keySet().size());
    assertEquals("1", c2.get("a"));
    assertEquals("2", c2.get("b"));
    assertFalse(c2.isSealed());
    c2.put("a", "cc");
    assertEquals("cc", c2.get("a"));
  }

  public void testLoad() throws IOException, Configuration.InvalidParam {
    String f = FileTestUtil.urlOfString(c1);
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
    config.loadList(ListUtil.list(FileTestUtil.urlOfString(c1),
				  FileTestUtil.urlOfString(c1a)));
    assertEquals("12", config.get("prop1"));
    assertEquals("xxx", config.get("prop2"));
    assertTrue(config.getBoolean("prop3", false));
    assertEquals("yyy", config.get("prop4"));
  }

  public void testLoadXml() throws IOException {
    Configuration config = newConfiguration();
    InputStream istr = new ReaderInputStream(new StringReader(m_testXml));
    config.loadXmlProperties(istr);

    try {
      assertTrue(config.getBoolean("org.lockss.a"));
      assertTrue(config.getBoolean("org.lockss.b.c"));
    } catch (Configuration.InvalidParam e) {
    }

    List l = config.getList("org.lockss.d");
    assertEquals(5, l.size());
    assertNotNull(l);

    Collections.sort(l);

    assertEquals("1", (String)l.get(0));
    assertEquals("2", (String)l.get(1));
    assertEquals("3", (String)l.get(2));
    assertEquals("4", (String)l.get(3));
    assertEquals("5", (String)l.get(4));
    
    assertNull(config.get("org.lockss.test.a"));
    assertEquals("foo", config.get("org.lockss.test.b"));

    assertNull(config.get("org.lockss.test.c"));
    assertEquals("foo", config.get("org.lockss.test.d"));

    assertEquals("foo", config.get("org.lockss.test.e"));
    assertNull(config.get("org.lockss.test.f"));

    assertEquals("foo", config.get("org.lockss.test.g"));
    assertNull(config.get("org.lockss.test.h"));

    assertEquals("foo", config.get("org.lockss.test.i"));
    assertNull(config.get("org.lockss.test.j"));

    assertNull(config.get("org.lockss.test.k"));
    assertEquals("foo", config.get("org.lockss.test.l"));

    assertEquals("foo", config.get("org.lockss.test.m"));
    assertNull(config.get("org.lockss.test.n"));

    assertEquals("foo", config.get("org.lockss.test.o"));
    assertNull(config.get("org.lockss.test.p"));

    assertEquals("foo", config.get("org.lockss.test.q"));
    assertNull(config.get("org.lockss.test.r"));

    assertEquals("foo", config.get("org.lockss.test.s"));
    assertEquals("bar", config.get("org.lockss.test.t"));

    assertEquals("bar", config.get("org.lockss.test.u"));
    assertEquals("foo", config.get("org.lockss.test.v"));

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
    config.load(FileTestUtil.urlOfString(c2));
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

  public void testEmptyTree() {
    Configuration config = newConfiguration();
    Iterator it1 = config.nodeIterator();
    assertNotNull(it1);
    assertFalse(it1.hasNext());
    Iterator it2 = config.nodeIterator("foo.bar");
    assertNotNull(it2);
    assertFalse(it2.hasNext());
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
