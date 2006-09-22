/*
 * $Id: TestStatusTable.java,v 1.12 2006-09-22 06:24:27 tlipkis Exp $
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

package org.lockss.daemon.status;
import java.util.*;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.servlet.*;


public class TestStatusTable extends LockssTestCase {
  StatusTable table;

  static ServletDescr srvDescr =
    new ServletDescr(LockssServlet.class, "name");
  static Properties args = new Properties();
  static {
    args.setProperty("foo", "bar");
  }


  public void setUp() {
    table = new StatusTable("table1", "key1");
  }

  public void testAccessors() {
    StatusTable tbl;
    tbl = new StatusTable("nom");
    assertEquals("nom", tbl.getName());
    assertNull(tbl.getKey());
    tbl = new StatusTable("nom", "cle");
    assertEquals("nom", tbl.getName());
    assertEquals("cle", tbl.getKey());
    tbl.setName("jacques");
    assertEquals("jacques", tbl.getName());

    assertNull(tbl.getTitle());
    tbl.setTitle("titre");
    assertEquals("titre", tbl.getTitle());

    assertNull(tbl.getTitleFootnote());
    tbl.setTitleFootnote("note");
    assertEquals("note", tbl.getTitleFootnote());

    assertTrue(tbl.isResortable());
    tbl.setResortable(false);
    assertFalse(tbl.isResortable());

    assertNull(tbl.getSummaryInfo());
    List lst = new ArrayList();
    tbl.setSummaryInfo(lst);
    assertSame(lst, tbl.getSummaryInfo());

    assertEmpty(tbl.getDefaultSortRules());
    tbl.setColumnDescriptors(Collections.EMPTY_LIST);
    assertEmpty(tbl.getDefaultSortRules());
    List rules = new ArrayList();
    tbl.setDefaultSortRules(rules);
    assertSame(rules, tbl.getDefaultSortRules());

    assertNotNull(tbl.getOptions());
  }

  public void testColDescMap() {
    List cols =
      ListUtil.list (
		     new ColumnDescriptor("col1", "Column 1 Title",
					  ColumnDescriptor.TYPE_STRING),
		     new ColumnDescriptor("col2", "Column 2 Title",
					  ColumnDescriptor.TYPE_INT,
					  "Column 2 footnote"),
		     new ColumnDescriptor("col3", "Column 2 Title",
					  ColumnDescriptor.TYPE_STRING) {
		       { comparator = new MyComparator(); } }
		     );
    table.setColumnDescriptors(cols);
    Map expMap = new HashMap();
    expMap.put("col1", cols.get(0));
    expMap.put("col2", cols.get(1));
    expMap.put("col3", cols.get(2));
    assertEquals(expMap, table.getColumnDescriptorMap());
  }

  Map testMap(Object key, Object val) {
    Map map = new HashMap();
    map.put(key, val);
    return map;
  }

  Map testMap(Object k1, Object v1, Object k2, Object v2) {
    Map map = testMap(k1, v1);
    map.put(k2, v2);
    return map;
  }

  public void testSortRule() throws Exception {
    MyComparator cmpr = new MyComparator();
    List cols =
      ListUtil.list(new ColumnDescriptor("a", "Column 1 Title",
					 ColumnDescriptor.TYPE_INT)
		    .setComparator(cmpr),
		    new ColumnDescriptor("b", "Column 2 Title",
					 ColumnDescriptor.TYPE_STRING)
		    );
    table.setColumnDescriptors(cols);
    Map colMap = table.getColumnDescriptorMap();

    StatusTable.SortRule rule1 = new StatusTable.SortRule("a", true);
    assertEquals(-1, rule1.getColumnType());
    assertNull(rule1.getComparator());
    rule1.inferColumnType(colMap);
    assertEquals(ColumnDescriptor.TYPE_INT, rule1.getColumnType());
    assertSame(cmpr, rule1.getComparator());
    assertTrue(rule1.compare("a", "b") < 0);
    cmpr.setReverse(true);
    assertTrue(rule1.compare("a", "b") > 0);
    cmpr.setReverse(false);

    StatusTable.SortRule rule2 = new StatusTable.SortRule("b", true);
    assertEquals(-1, rule2.getColumnType());
    assertNull(rule2.getComparator());
    rule2.inferColumnType(colMap);
    assertEquals(ColumnDescriptor.TYPE_STRING, rule2.getColumnType());
    assertNull(rule2.getComparator());
    assertEquals(0, rule2.compare("aaa", "aaa"));
    assertTrue(rule2.compare("aaa", "bbb") < 0);
    assertTrue(rule2.compare("bbb", "aaa") > 0);
    assertTrue(rule2.compare(null, "bbb") < 0);
    assertTrue(rule2.compare("bbb", null) > 0);

    StatusTable.SortRule rule3 = new StatusTable.SortRule("b", false);
    rule3.inferColumnType(colMap);
    assertTrue(rule3.compare("aaa", "bbb") > 0);
    assertTrue(rule3.compare("bbb", "aaa") < 0);

    StatusTable.SortRule rule4 = new StatusTable.SortRule("b", cmpr);
    assertTrue(rule4.compare("aaa", "bbb") < 0);
    assertTrue(rule4.compare("bbb", "aaa") > 0);
    cmpr.setReverse(true);
    assertTrue(rule4.compare("aaa", "bbb") > 0);
    assertTrue(rule4.compare("bbb", "aaa") < 0);
    cmpr.setReverse(false);

    StatusTable.SortRule rule5 = new StatusTable.SortRule("b", cmpr, false);
    assertTrue(rule5.compare("aaa", "bbb") > 0);
    assertTrue(rule5.compare("bbb", "aaa") < 0);
    cmpr.setReverse(true);
    assertTrue(rule5.compare("aaa", "bbb") < 0);
    assertTrue(rule5.compare("bbb", "aaa") > 0);
    cmpr.setReverse(false);
  }

  public void testSortRuleComparator() throws Exception {
    List cols =
      ListUtil.list(
		    new ColumnDescriptor("a", "Column 1 Title",
					 ColumnDescriptor.TYPE_STRING),
		    new ColumnDescriptor("b", "Column 2 Title",
					 ColumnDescriptor.TYPE_IP_ADDRESS)
		    );
    table.setColumnDescriptors(cols);
    Map colMap = table.getColumnDescriptorMap();

    Map r1 = testMap("a", "a1", "b", IPAddr.getByName("2.2.2.2"));
    Map r2 = testMap("a", "a2", "b", IPAddr.getByName("1.1.1.1"));
    Map r3 = testMap("a", "a2", "b", IPAddr.getByName("1.1.1.1"));
    Map r4 = testMap("a", ListUtil.list("a2", "b"),
		     "b", IPAddr.getByName("2.2.2.2"));
    List rules1 = ListUtil.list(new StatusTable.SortRule("a", true));
    List rules2 = ListUtil.list(new StatusTable.SortRule("a", true),
				new StatusTable.SortRule("b", true));
    List rules3 = ListUtil.list(new StatusTable.SortRule("b", true),
				new StatusTable.SortRule("a", true));
    StatusTable.SortRuleComparator src;

    src = new StatusTable.SortRuleComparator(rules1, colMap);
    assertEquals(0, src.compare(r1, r1));
    assertTrue(src.compare(r1, r2) < 0);
    assertTrue(src.compare(r2, r1) > 0);
    assertEquals(0, src.compare(r2, r3));
    assertTrue(src.compare(r1, r4) < 0);

    src = new StatusTable.SortRuleComparator(rules2, colMap);

    // check that inferColumnType was invoked on the rules
    StatusTable.SortRule iprule = (StatusTable.SortRule)rules2.get(1);
    assertEquals(ColumnDescriptor.TYPE_IP_ADDRESS, iprule.getColumnType());

    assertEquals(0, src.compare(r1, r1));
    assertTrue(src.compare(r1, r2) < 0);
    assertTrue(src.compare(r2, r1) > 0);
    assertEquals(0, src.compare(r2, r3));
    assertTrue(src.compare(r2, r4) < 0);

    src = new StatusTable.SortRuleComparator(rules3, colMap);
    assertEquals(0, src.compare(r1, r1));
    assertTrue(src.compare(r1, r2) > 0);
    assertTrue(src.compare(r2, r1) < 0);
    assertEquals(0, src.compare(r2, r3));
    assertTrue(src.compare(r2, r4) < 0);
  }

  class MyComparator implements Comparator {
    private boolean reverse = false;
    public int compare(Object o1, Object o2) {
      int res = ((Comparable)o1).compareTo((Comparable)o2);
      return reverse ? -res : res;
    }
    void setReverse(boolean reverse) {
      this.reverse = reverse;
    }
  }

  public void testEmbeddedValue() {
    Integer val = new Integer(3);
    StatusTable.DisplayedValue dval = new StatusTable.DisplayedValue(val);
    StatusTable.Reference rval = new StatusTable.Reference(val, "foo", "bar");
    StatusTable.SrvLink lval = new StatusTable.SrvLink(val, srvDescr, args);
    // should be able to embed DisplayedValue in Reference or SrvLink
    new StatusTable.Reference(dval, "foo", "bar");
    new StatusTable.SrvLink(dval, srvDescr, args);

    try {
      new StatusTable.DisplayedValue(rval);
      fail("Shouldn't be able to embed Reference in DisplayedValue");
    } catch (IllegalArgumentException e) {
    }
    try {
      new StatusTable.DisplayedValue(lval);
      fail("Shouldn't be able to embed SrvLink in DisplayedValue");
    } catch (IllegalArgumentException e) {
    }
    try {
      new StatusTable.DisplayedValue(dval);
      fail("Shouldn't be able to embed DisplayedValue in DisplayedValue");
    } catch (IllegalArgumentException e) {
    }
    try {
      new StatusTable.Reference(rval, "foo", "bar");
      fail("Shouldn't be able to embed Reference in Reference");
    } catch (IllegalArgumentException e) {
    }
    try {
      new StatusTable.SrvLink(lval, srvDescr, args);
      fail("Shouldn't be able to embed SrvLink in SrvLink");
    } catch (IllegalArgumentException e) {
    }
    try {
      new StatusTable.Reference(lval, "foo", "bar");
      fail("Shouldn't be able to embed SrvLink in Reference");
    } catch (IllegalArgumentException e) {
    }
    try {
      new StatusTable.SrvLink(rval, srvDescr, args);
      fail("Shouldn't be able to embed Reference in SrvLink");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testReferenceGetActualValue() {
    Integer val = new Integer(3);
    StatusTable.DisplayedValue dval = new StatusTable.DisplayedValue(val);
    StatusTable.Reference rval = new StatusTable.Reference(val, "foo", "bar");
    StatusTable.Reference rdval = new StatusTable.Reference(dval,
							    "foo", "bar");
    assertEquals(val, StatusTable.getActualValue(val));
    assertEquals(val, StatusTable.getActualValue(dval));
    assertEquals(val, StatusTable.getActualValue(rval));
    assertEquals(val, StatusTable.getActualValue(rdval));
  }

  public void testReferenceProps() {
    StatusTable.Reference ref = new StatusTable.Reference("C", "blah", null);
    assertNull(ref.getProperties());
    ref.setProperty("foo", "bar");
    assertEquals("bar", ref.getProperties().getProperty("foo"));
  }

  public void testReferenceEquals() {
    StatusTable.Reference ref = new StatusTable.Reference("C", "blah", null);
    assertFalse(ref.equals("blah"));
    assertTrue(ref.equals(new StatusTable.Reference("C", "blah", null)));
    assertFalse(ref.equals(new StatusTable.Reference("D", "blah", null)));
    assertFalse(ref.equals(new StatusTable.Reference("C", "blah2", null)));
    assertFalse(ref.equals(new StatusTable.Reference("C", "blah", "key")));
    ref = new StatusTable.Reference("C", "blah", "key1");
    assertTrue(ref.equals(new StatusTable.Reference("C", "blah", "key1")));
    assertFalse(ref.equals(new StatusTable.Reference("C", "blah", "key")));
  }

  public void testSrvLinkGetActualValue() {
    Integer val = new Integer(3);
    StatusTable.DisplayedValue dval = new StatusTable.DisplayedValue(val);
    StatusTable.SrvLink lval = new StatusTable.SrvLink(val, srvDescr, args);
    StatusTable.SrvLink ldval = new StatusTable.SrvLink(dval, srvDescr, args);

    assertEquals(val, StatusTable.getActualValue(val));
    assertEquals(val, StatusTable.getActualValue(dval));
    assertEquals(val, StatusTable.getActualValue(lval));
    assertEquals(val, StatusTable.getActualValue(ldval));
  }

  public void testSrvLink() {
    StatusTable.SrvLink lnk = new StatusTable.SrvLink("C", srvDescr, args);
    assertEquals("C", lnk.getValue());
    assertEquals(srvDescr, lnk.getServletDescr());
    assertEquals(args, lnk.getArgs());
  }

  public void testSrvLinkEquals() {
    StatusTable.SrvLink lnk = new StatusTable.SrvLink("C", srvDescr, args);
    assertFalse(lnk.equals("blah"));
    assertTrue(lnk.equals(new StatusTable.SrvLink("C", srvDescr, args)));
    assertFalse(lnk.equals(new StatusTable.SrvLink("D", srvDescr, args)));
    assertFalse(lnk.equals(new StatusTable.SrvLink("C", srvDescr, null)));
    assertFalse(lnk.equals(new StatusTable.SrvLink("C", new ServletDescr(LockssServlet.class, "bar"), args)));
    assertFalse(lnk.equals(new StatusTable.SrvLink("C", srvDescr,
						   new Properties())));
  }

  public void testSummaryInfo() {
    StatusTable.SummaryInfo si =
      new StatusTable.SummaryInfo("Foo", ColumnDescriptor.TYPE_STRING, "val");
    assertEquals("Foo", si.getTitle());
    assertEquals(ColumnDescriptor.TYPE_STRING, si.getType());
    assertEquals("val", si.getValue());

    si = new StatusTable.SummaryInfo("bar", ColumnDescriptor.TYPE_INT, 23);
    assertEquals("bar", si.getTitle());
    assertEquals(ColumnDescriptor.TYPE_INT, si.getType());
    assertEquals(new Integer(23), si.getValue());
  }
}
