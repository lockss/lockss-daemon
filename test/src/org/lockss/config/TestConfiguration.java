/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import java.util.*;
import java.io.*;
import java.net.*;
import org.apache.commons.io.*;

import org.lockss.util.*;
import org.lockss.config.ConfigFile;
import org.lockss.config.Configuration;
import org.lockss.config.ConfigurationPropTreeImpl;
import org.lockss.config.Tdb;
import org.lockss.config.Tdb.TdbException;
import org.lockss.config.TdbAu;
import org.lockss.config.TdbPublisher;
import org.lockss.config.TdbTitle;
import org.lockss.test.*;
import static org.lockss.config.Configuration.Differences;

/**
 * Test class for <code>org.lockss.util.Configuration</code>
 */

public class TestConfiguration extends LockssTestCase {

  public static Class testedClasses[] = {
    org.lockss.config.Configuration.class
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

  private Tdb newTdb() throws TdbException {
    return newTdb("");
  }

  private Tdb newTdb(String pref) throws TdbException {
    Tdb tdb = new Tdb();
    TdbPublisher p1 = new TdbPublisher("p1");
    // create title with 2 aus with different plugins
    TdbTitle t1p1 = new TdbTitle("t1p1", "0000-0001");
    p1.addTdbTitle(t1p1);
    TdbAu a1t1p1 = new TdbAu(pref + "a1t1p1", pref + "plugin_t1p1");
    a1t1p1.setParam("auName", pref + "a1t1p1");  // distinguishing parameter
    t1p1.addTdbAu(a1t1p1);
    TdbAu a2t1p1 = new TdbAu(pref + "a2t1p1", pref + "plugin_t1p1");
    a2t1p1.setParam("auName", pref + "a2t1p1");  // distinguishing parameter
    t1p1.addTdbAu(a2t1p1);

    // create title with 2 aus with the same plugin
    TdbTitle t2p1 = new TdbTitle("t2p1", "0000-0002");
    p1.addTdbTitle(t2p1);
    TdbAu a1t2p1 = new TdbAu(pref + "a1t2p1", pref + "plugin_t2p1");
    a1t2p1.setParam("auName", pref + "a1t2p1");  // distinguishing parameter
    t2p1.addTdbAu(a1t2p1);
    TdbAu a2t2p1 = new TdbAu(pref + "a2t2p1", pref + "plugin_t2p1");
    a2t2p1.setParam("auName", pref + "a2t2p1");  // distinguishing parameter
    t2p1.addTdbAu(a2t2p1);


    // add AUs for publisher p1
    tdb.addTdbAu(a1t1p1);
    tdb.addTdbAu(a2t1p1);
    tdb.addTdbAu(a1t2p1);
    tdb.addTdbAu(a2t2p1);
    
    return tdb;
  }
  
  public void testSet() {
    Configuration config = newConfiguration();
    assertEquals(0, config.keySet().size());
    config.put("a", "b");
    assertEquals(1, config.keySet().size());
    assertEquals("b", config.get("a"));
  }

  public void testSetTdb() throws TdbException {
    Configuration config = newConfiguration();
    assertNull(config.getTdb());
    
    Tdb tdb = newTdb();
    config.setTdb(tdb);
    assertNotNull(config.getTdb());
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
    
    Tdb tdb = config.getTdb();
    assertTrue((tdb == null) || tdb.isSealed());
  }

  public void testSeal() throws TdbException {
    Configuration config = newConfiguration();
    config.put("a", "1");
    config.put("b", "2");
    config.put("b.x", "3");
    config.setTdb(newTdb());
    config.seal();
    assertSealed(config);
    assertEquals(3, config.keySet().size());
    assertEquals("1", config.get("a"));
    assertEquals("2", config.get("b"));
    assertSealed(config.getConfigTree("b"));
  }

  public void testCopy() throws TdbException {
    Configuration c1 = newConfiguration();
    c1.put("a", "1");
    c1.put("b", "2");
    c1.put("b.x", "3");
    Tdb tdb1 = newTdb();
    c1.setTdb(tdb1);
    c1.seal();
    Configuration c2 = c1.copy();
    assertEquals(3, c2.keySet().size());
    assertEquals("1", c2.get("a"));
    assertEquals("2", c2.get("b"));
    assertFalse(c2.isSealed());
    c2.put("a", "cc");
    assertEquals("cc", c2.get("a"));
    Tdb tdb2 = c2.getTdb();
    assertEmpty(tdb1.computeDifferences(tdb2).getPluginIdsForDifferences());
    assertNotSame(tdb1, tdb2);
  }

  public void testCopyFrom() throws TdbException {
    Configuration c1 = newConfiguration();
    c1.put("a", "1");
    c1.put("b", "2");
    c1.put("b.x", "3");
    Tdb tdb1 = newTdb();
    c1.setTdb(tdb1);
    c1.seal();
    Configuration c2 = newConfiguration();
    c2.copyFrom(c1);
    assertEquals(3, c2.keySet().size());
    assertEquals("1", c2.get("a"));
    assertEquals("2", c2.get("b"));
    assertFalse(c2.isSealed());
    c2.put("a", "cc");
    assertEquals("cc", c2.get("a"));
    Tdb tdb2 = c2.getTdb();
    assertEmpty(tdb1.computeDifferences(tdb2).getPluginIdsForDifferences());
    assertNotSame(tdb1, tdb2);
  }

  public void testCopyFromEvent() throws TdbException {
    Configuration c1 = newConfiguration();
    c1.put("a", "1");
    c1.put("b", "2");
    c1.put("b.x", "3");
    Tdb tdb1 = newTdb();
    c1.setTdb(tdb1);
    c1.seal();
    Configuration c2 = newConfiguration();
    final Set evts = new HashSet();
    c2.copyFrom(c1,
		new Configuration.ParamCopyEvent() {
		  public void paramCopied(String name, String val){
		    evts.add(ListUtil.list(name, val));
		  }
		});
    assertEquals(SetUtil.set(ListUtil.list("a", "1"),
			     ListUtil.list("b", "2"),
			     ListUtil.list("b.x", "3")), evts);
    assertEquals(3, c2.keySet().size());
    assertEquals("1", c2.get("a"));
    assertEquals("2", c2.get("b"));
    assertFalse(c2.isSealed());
    c2.put("a", "cc");
    assertEquals("cc", c2.get("a"));
    Tdb tdb2 = c2.getTdb();
    assertEmpty(tdb1.computeDifferences(tdb2).getPluginIdsForDifferences());
    assertNotSame(tdb1, tdb2);
  }

  public void testEquals() throws TdbException {
    // ensure configuration always equal to itself
    Configuration c1 = newConfiguration();
    c1.put("a", "1");
    c1.put("b", "2");
    c1.put("b.x", "3");
    Tdb tdb1 = newTdb();
    c1.setTdb(tdb1);
    assertEquals(c1, c1);
    
    // ensure that a copy of a configuration same as the original
    Configuration c2 = c1.copy();
    Configuration.Differences diffc1c2 = c1.differences(c2);
    assertEquals(c1, c2);
    assertTrue(diffc1c2.isEmpty());
    
    // ensure config with additional tdbau is not equal
    Configuration c3 = c1.copy();
    Properties p1 = new Properties();
    p1.put("title", "Air & Space volume 3");
    p1.put("plugin", "org.lockss.testplugin1");
    p1.put("pluginVersion", "4");
    p1.put("issn", "0003-0031");
    p1.put("journal.link.1.type", TdbTitle.LinkType.continuedBy.toString());
    p1.put("journal.link.1.journalId", "0003-0031");  // link to self
    p1.put("param.1.key", "volume");
    p1.put("param.1.value", "3");
    p1.put("param.2.key", "year");
    p1.put("param.2.value", "1999");
    p1.put("attributes.publisher", "The Smithsonian Institution");
    c3.getTdb().addTdbAuFromProperties(p1);
    assertNotEquals(c1, c3);
    
    // ensure config with additional property not equal
    Configuration c4 = c1.copy();
    c4.put("c", "4");
    assertNotEquals(c1, c4);
  }

  public void testDifferencesNullOrEmpty(Configuration other)
      throws TdbException {
    Configuration c1 = newConfiguration();
    c1.put("a", "1");
    c1.put("b", "2");
    c1.put("b.x", "3");
    Tdb tdb1 = newTdb();
    c1.setTdb(tdb1);
    Differences diffs = c1.differences(other);
    assertTrue(diffs.isAllKeys());
    assertSame(c1.keySet(), diffs.getDifferenceSet());
    assertEquals(4, diffs.getTdbAuDifferenceCount());
    assertEquals(SetUtil.set("plugin_t1p1", "plugin_t2p1"),
		 diffs.getTdbDifferencePluginIds());

    Map<String,TdbAu> tdbAus = getTdbAuMap(tdb1);
    Tdb.Differences tdbDiffs = diffs.getTdbDifferences();
    assertSameElements(ListUtil.list(tdbAus.get("a2t2p1"),
				     tdbAus.get("a1t2p1"),
				     tdbAus.get("a2t1p1"),
				     tdbAus.get("a1t1p1")),
		       ListUtil.fromIterator(tdbDiffs.newTdbAuIterator()));
  }

  Map<String,TdbAu> getTdbAuMap(final Tdb tdb) {
    Map<String,TdbAu> res = new HashMap<String,TdbAu>() {{
	for (Iterator<TdbAu> iter = tdb.tdbAuIterator(); iter.hasNext(); ) {
	  TdbAu tau = iter.next();
	  put(tau.getName(), tau);
	}}};
    return res;
  }


  public void testDifferencesNull() throws TdbException {
    testDifferencesNullOrEmpty(null);
  }

  public void testDifferencesEmpty() throws TdbException {
    testDifferencesNullOrEmpty(newConfiguration());
  }

  public void testDifferencesSelfEmpty()
      throws TdbException {
    Configuration c1 = newConfiguration();
    Configuration c2 = newConfiguration();
    c2.put("a", "1");
    c2.put("b", "2");
    c2.put("b.x", "3");
    Tdb tdb1 = newTdb();
    c2.setTdb(tdb1);
    Differences diffs = c1.differences(c2);
    assertTrue(diffs.isAllKeys());
    assertSame(c2.keySet(), diffs.getDifferenceSet());
    assertEquals(-4, diffs.getTdbAuDifferenceCount());
    assertEquals(SetUtil.set("plugin_t1p1", "plugin_t2p1"),
		 diffs.getTdbDifferencePluginIds());
    Tdb.Differences tdbDiffs = diffs.getTdbDifferences();
    assertEmpty(ListUtil.fromIterator(tdbDiffs.newTdbAuIterator()));
  }

  public void testDifferences() throws TdbException {
    Configuration c1 = newConfiguration();
    c1.put("a", "1");
    c1.put("b", "2");
    c1.put("b.x", "3");
    c1.put("c", "0");
    Tdb tdb1 = newTdb();
    c1.setTdb(tdb1);
    Configuration c2 = newConfiguration();
    c2.put("a", "4");
    c2.put("b", "2");
    c2.put("b.x", "7");
    c2.put("b.y", "8");
    Tdb tdb2 = newTdb("X2");
    tdb2.copyFrom(newTdb("Y2"));
    c2.setTdb(tdb2);
    Differences diffs = c1.differences(c2);
    assertFalse(diffs.isAllKeys());
    assertEquals(SetUtil.set("a", "b", "b.", "b.x", "b.y", "c"),
		 diffs.getDifferenceSet());
    assertTrue(diffs.contains("a"));
    assertTrue(diffs.contains("b"));
    assertTrue(diffs.contains("b."));
    assertTrue(diffs.contains("b.x"));

    assertNotSame(c1.keySet(), diffs.getDifferenceSet());
    assertEquals(-4, diffs.getTdbAuDifferenceCount());
    assertSameElements(new String[] {"plugin_t1p1", "plugin_t2p1",
				     "X2plugin_t1p1", "X2plugin_t2p1",
				     "Y2plugin_t1p1", "Y2plugin_t2p1"},
      diffs.getTdbDifferencePluginIds());
    assertTrue(diffs.contains("a"));

    Map<String,TdbAu> tdbAus = getTdbAuMap(tdb1);
    Tdb.Differences tdbDiffs = diffs.getTdbDifferences();
    assertSameElements(ListUtil.list(tdbAus.get("a2t2p1"),
				     tdbAus.get("a1t2p1"),
				     tdbAus.get("a2t1p1"),
				     tdbAus.get("a1t1p1")),
		       ListUtil.fromIterator(tdbDiffs.newTdbAuIterator()));
  }

  public void testLoad() throws IOException, Configuration.InvalidParam {
    String f = FileTestUtil.urlOfString(c1);
    Configuration config = newConfiguration();
    config.load(loadFCF(f));
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

  public void testStore() throws Exception {
    File file = FileTestUtil.tempFile("cfgtest", ".txt");
    Configuration config = newConfiguration();
    config.put("fo.o", "x");
    config.put("baz", "y");
    config.put("bar", "Fran\u00E7ais");
    config.put("fo.o3", " leading space");
    config.put("fo.o1", "y");
    config.put("Buenos d\u00edas", "Buenos d\u00edas");


    OutputStream os = new FileOutputStream(file);
    Properties addtl =
      PropUtil.fromArgs("addtl1", "1");
    config.store(os, "test header", addtl);
    os.close();

    // ensure result is sorted
    String[] exp = {
      "#test header",
      "#\\w\\w\\w \\w\\w\\w ",		// date
      "addtl1=1",
      "Buenos\\\\ d\\\\u00EDas=Buenos d\\\\u00EDas",
      "bar=Fran\\\\u00E7ais",
      "baz=y",
      "fo\\.o=x",
      "fo\\.o1=y",
      "fo\\.o3=\\\\ leading space",
    };
    assertMatchesREs(exp, FileUtils.readLines(file));
  }

  enum TestEnum {x, Y, zZ};

  public void testGetNonEmpty() throws Exception {
    Configuration config = newConfiguration();
    config.put("foo", "x");
    config.put("bar", "");
    assertEquals("x", config.getNonEmpty("foo"));
    assertNull("x", config.getNonEmpty("bar"));
    assertNull("x", config.getNonEmpty("nokey"));
  }

  public void testGetEnum() throws Exception {
    Configuration config = newConfiguration();
    config.put("foo", "x");
    config.put("bar", "y");
    config.put("baz", "Y");
    config.put("frob", "zz");
    assertSame(TestEnum.x, config.getEnum(TestEnum.class, "foo"));
    assertSame(TestEnum.Y, config.getEnum(TestEnum.class, "baz", TestEnum.x));
    assertSame(TestEnum.x, config.getEnum(TestEnum.class, "xxx", TestEnum.x));
    try {
      config.getEnum(TestEnum.class, "bar");
      fail("Should have thrown IllegalArgumentException");
    } catch (Exception ex) {
    }

    // Test case independent match
    try {
      config.getEnum(TestEnum.class, "frob");
      fail("Should have thrown IllegalArgumentException");
    } catch (Exception ex) {
    }
    assertSame(TestEnum.zZ, config.getEnumIgnoreCase(TestEnum.class, "frob"));
  }

  private static final String c3 =
    "prop.p1=a;b;c;d;e;f;g\n" +
    "prop.p2=xxx";

  public void testGetList() throws IOException {
    Configuration config = newConfiguration();
    config.load(loadFCF(FileTestUtil.urlOfString(c3)));
    List l = config.getList("prop.p1");
    assertEquals(ListUtil.list("a", "b", "c", "d", "e", "f", "g"), 
		 config.getList("prop.p1"));
    assertEquals(ListUtil.list("xxx"), config.getList("prop.p2"));
  }

  public void testGetListEmptyStrings() throws IOException {
    Configuration config = newConfiguration();
    config.load(loadFCF(FileTestUtil.urlOfString("prop.p1=a;;b;")));
    assertEquals(ListUtil.list("a", "b"), config.getList("prop.p1"));
  }

  public void testGetListDefault() throws IOException {
    Configuration config = newConfiguration();
    assertEquals(Collections.EMPTY_LIST, config.getList("foo"));
    assertEquals(ListUtil.list("bar"), config.getList("foo",
						      ListUtil.list("bar")));
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
  }
  private static HashMap m2a = new HashMap();
  static {
    m2a.put("p1", "12");
    m2a.put("p2", "foobar");
    m2a.put("p3.a", "true");
    m2a.put("p3.b", "false");
  }

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

  public void testSubTree() {
    Properties props = new Properties();
    props.put("p1", "1");
    props.put("p2", "2");
    props.put("p1.a", "1a");
    props.put("p1.b", "1b");
    props.put("p2.a", "2a");
    Configuration config = ConfigurationUtil.fromProps(props);
    Configuration sub = config.getConfigTree("p1");
    assertEquals("1a", sub.get("a"));
    assertEquals("1a", config.get("p1.a"));
    sub.put("a", "xx");
    assertEquals("xx", sub.get("a"));
    assertEquals("1a", config.get("p1.a"));

    // Ensure nonexistent subtree returns empty config, not null
    Configuration emptysub = config.getConfigTree("not.a.root");
    assertTrue(emptysub.isEmpty());
  }


  public void testStruct() throws IOException {
    Configuration config = newConfiguration();
    config.load(loadFCF(FileTestUtil.urlOfString(c2)));
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

  public void testTimeInterval() throws Exception {
    Properties props = new Properties();
    props.put("p1", "1");
    props.put("p2", "0s");
    props.put("p3", "20m");
    props.put("p4", "100h");
    props.put("p5", "101d");
    props.put("p6", "foo");
    props.put("p7", "250x");
    Configuration config = ConfigurationUtil.fromProps(props);
    assertEquals(1, config.getTimeInterval("p1"));
    assertEquals(0, config.getTimeInterval("p2"));
    assertEquals(20*Constants.MINUTE, config.getTimeInterval("p3"));
    assertEquals(100*Constants.HOUR, config.getTimeInterval("p4"));
    assertEquals(101*Constants.DAY, config.getTimeInterval("p5"));
    try {
      config.getTimeInterval("p6");
      fail("getTimeInterval(foo) should throw");
    } catch (Configuration.InvalidParam e) {
    }
    try {
      config.getTimeInterval("p7");
      fail("getTimeInterval(250x) should throw");
    } catch (Configuration.InvalidParam e) {
    }
  }

  public void testSize() throws Exception {
    long k = 1024;
    long m = k*k;
    Properties props = new Properties();
    props.put("p0", "1");
    props.put("p1", "1000000b");
    props.put("p2", "100kb");
    props.put("p3", "2.5mb");
    props.put("p4", "100gb");
    props.put("p5", "6.8tb");
    props.put("p6", "1.5pb");
    props.put("p7", "foo");
    props.put("p8", "250x");
    Configuration config = ConfigurationUtil.fromProps(props);
    assertEquals(1, config.getSize("p0"));
    assertEquals(1000000, config.getSize("p1"));
    assertEquals(100*k, config.getSize("p2"));
    assertEquals((long)(2.5*m), config.getSize("p3"));
    assertEquals(100*k*m, config.getSize("p4"));
    assertEquals((long)(6.8f*(m*m)), config.getSize("p5"));
    assertEquals((long)(1.5f*(m*m*k)), config.getSize("p6"));
    try {
      config.getSize("p7");
      fail("getSize(foo) should throw");
    } catch (Configuration.InvalidParam e) {
    }
    try {
      config.getSize("p8");
      fail("getSize(250x) should throw");
    } catch (Configuration.InvalidParam e) {
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
    props.put("p7", "250");
    Configuration config = ConfigurationUtil.fromProps(props);
    assertEquals(0.0, config.getPercentage("p2"), 0.0);
    assertEquals(0.2, config.getPercentage("p3"), 0.0000001);
    assertEquals(1.0, config.getPercentage("p4"), 0.0);
    assertEquals(0.1, config.getPercentage("p1", 0.1), 0.0000001);
    assertEquals(0.5, config.getPercentage("p6", 0.5), 0.0);
    assertEquals(0.1, config.getPercentage("p1", 0.1), 0.0000001);
    assertEquals(2.5, config.getPercentage("p7", 0.1), 0.0000001);
    assertEquals(2.5, config.getPercentage("p7"), 0.0000001);

    try {
      config.getPercentage("p1");
      fail("getPercentage(-1) should throw");
    } catch (Configuration.InvalidParam e) {
    }
    try {
      config.getPercentage("p6");
      fail("getPercentage(foo) should throw");
    } catch (Configuration.InvalidParam e) {
    }
  }

  public void testDouble() throws Exception {
    Properties props = new Properties();
    props.put("p1", "-1");
    props.put("p2", "0");
    props.put("p3", "0.0");
    props.put("p4", "1.0");
    props.put("p5", "-10.25");
    props.put("p6", "foo");
    props.put("p7", "1.4e16");
    props.put("p8", "1.4e-16");
    props.put("p9", "10.0xx");
    Configuration config = ConfigurationUtil.fromProps(props);
    assertEquals(-1.0, config.getDouble("p1"), 0.0000001);
    assertEquals(-1.0, config.getDouble("p1", 0.1), 0.0000001);
    assertEquals(0.0, config.getDouble("p2"));
    assertEquals(0.0, config.getDouble("p2"), 0.0);
    assertEquals(0.0, config.getDouble("p3"));
    assertEquals(0.0, config.getDouble("p3"), 0.0);
    assertEquals(1.0, config.getDouble("p4"));
    assertEquals(1.0, config.getDouble("p4"), 0.0);
    assertEquals(-10.25, config.getDouble("p5"));
    assertEquals(-10.25, config.getDouble("p5"), 0.0);
    assertEquals(0.5, config.getDouble("p6", 0.5), 0.0);
    assertEquals(14000000000000000.0, config.getDouble("p7"), 10);
    assertEquals(14000000000000000.0, config.getDouble("p7", 0.1), 10);
    assertEquals(0.00000000000000014, config.getDouble("p8"),
		 .000000000000000001);
    assertEquals(0.00000000000000014, config.getDouble("p8", 0.1),
		 .000000000000000001);

    try {
      config.getDouble("p6");
      fail("getDouble(foo) should throw");
    } catch (Configuration.InvalidParam e) {
    }
    try {
      config.getDouble("p9");
      fail("getDouble(10.0xx) should throw");
    } catch (Configuration.InvalidParam e) {
    }
  }

  public void testGetIndirect() throws Exception {
    Configuration config =
      ConfigurationUtil.fromArgs("org.lockss.foo1", "bar",
				 "org.lockss.foo2", "org.lockss.foo4",
				 "org.lockss.foo3", "@org.lockss.foo4",
				 "org.lockss.foo4", "twice");
    assertEquals(null, config.getIndirect(null, null));
    assertEquals("val", config.getIndirect("val", null));
    assertEquals(null, config.getIndirect("@org.lockss.foo0", null));
    assertEquals("aa", config.getIndirect("@org.lockss.foo0", "aa"));
    assertEquals("bar", config.getIndirect("@org.lockss.foo1", null));
    assertEquals("@org.lockss.foo4",
		 config.getIndirect("@org.lockss.foo3", null));
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

  private ConfigFile loadFCF(String url) throws IOException {
    FileConfigFile cf = new FileConfigFile(url);
    cf.reload();
    return cf;
  }
}
