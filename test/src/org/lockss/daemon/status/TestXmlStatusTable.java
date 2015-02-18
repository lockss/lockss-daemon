/*
 * $Id$
 */

/*

Copyright (c) 2000-2012 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.net.*;
import java.util.*;

import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.servlet.*;

import org.w3c.dom.*;

public class TestXmlStatusTable extends LockssTestCase {

  static ServletDescr srvDescr =
    new ServletDescr("test", LockssServlet.class, "name");
  static String peerKey = "TCP:[127.0.0.1]:9729";

  public void setUp() throws Exception {
    super.setUp();
  }

  // The expected value for this test is in statustest1.xml in this dir.
  // Edit it to correspond to changes in the xml generation or the table
  // contents.
  // This should use a more xml-aware comparison (one that doesn't depend
  // on the output formatting), either by parsing the expected file, or
  // canonicalizing both.
  public void testCreateTableDocument(int outver, String file)
      throws Exception {
    ConfigurationUtil.addFromArgs(XmlStatusTable.PARAM_XML_DATE_FORMAT, "GMT");
    // create a status table
    StatusTable table = new StatusTable("table", "key");
    MockStatusAccessor accessor = new MockStatusAccessor();
    accessor.setTitleFoot("title foot", "key");
    List colList = ListUtil.list(
      new ColumnDescriptor("intCol", "title1", ColumnDescriptor.TYPE_INT),
      new ColumnDescriptor("strCol", "title2", ColumnDescriptor.TYPE_STRING,
			   "c2foot"),
      new ColumnDescriptor("dateCol", "title3", ColumnDescriptor.TYPE_DATE)
      );
    accessor.setColumnDescriptors(colList, "key");

    StatusTable.DisplayedValue dispValue =
        new StatusTable.DisplayedValue(new Integer(456));
    dispValue.setColor("color1");
    dispValue.setBold(true);
    StatusTable.Reference refValue1 =
      new StatusTable.Reference("row2 string", "table2", "key2");

    PeerIdentity pid = V3TestUtils.findPeerIdentity(getMockLockssDaemon(),
						    peerKey);
    StatusTable.Reference refValue2 =
      new StatusTable.Reference("row3 string3", pid, "tableN", "keyN");
    refValue2.setProperty("prop4", "val4");
    refValue2.setProperty("prop8", "no");

    Object[][] rowObj = {
      {new Integer(123), "row1 string"},
      {dispValue, refValue1},
      {dispValue, refValue2, new Date(30000000)},
      {new Integer(99960), Collections.EMPTY_LIST},
      {new Integer(99970)},		// sparse row
      {StatusTable.NO_VALUE, "missing value row"}, // elem w/ no value, sorts first
      {new Integer(99980), new StatusTable.DisplayedValue("raw value", "disp value")},
      {new StatusTable.SrvLink(new Integer(99990), srvDescr, null),
       ListUtil.list("cc", new StatusTable.Reference("x1", "tt", "k42"))},
      {new Integer(99990),
       new StatusTable.DisplayedValue(ListUtil.list("int1", "out2")).setLayout(StatusTable.DisplayedValue.Layout.Column)},
    };


    List rowList = MockStatusAccessor.makeRowsFrom(colList, rowObj);
    Map row97 = (Map)rowList.get(3);
    row97.put(StatusTable.ROW_SEPARATOR, "1");
    accessor.setRows(rowList, "key");

    StatusTable.DisplayedValue dv2 = new StatusTable.DisplayedValue("v1");
    dv2.setColor("blue");
    Object[][] sumObj = {
      {"summary", new Integer(ColumnDescriptor.TYPE_STRING), "sum value",
       "s1foot"},
      {"s2", new Integer(ColumnDescriptor.TYPE_STRING),
       ListUtil.list(dv2, new StatusTable.Reference("v2", "t2", null))},
      {"Empty List", new Integer(ColumnDescriptor.TYPE_STRING),
       Collections.EMPTY_LIST},
    };

    List sumList = MockStatusAccessor.makeSummaryInfoFrom(sumObj);
    accessor.setSummaryInfo("key", sumList);
    accessor.populateTable(table);
    table.setTitle("Splunge");

    // create the XML
    XmlStatusTable xmlTable = new XmlStatusTable(table);
    if (outver >= 0) {
      xmlTable.setOutputVersion(outver);
    }
    Document tableDoc = xmlTable.getTableDocument();

    // serialize it and compare to the expected file, statustest1.xml
    StringWriter wrtr = new StringWriter();
    XmlDomBuilder.serialize(tableDoc, wrtr);
    URL url = getResource(file);
    Reader rdr = new InputStreamReader(UrlUtil.openInputStream(url.toString()),
				       Constants.DEFAULT_ENCODING);
    String exp = StringUtil.fromReader(rdr);
    String actual = wrtr.toString();
    log.debug3("XML output:\n" + actual);
    assertEquals(exp, actual);
  }

  public void testCreateTableDocument1() throws Exception {
    testCreateTableDocument(-1, "statustest1.xml");
  }

  public void testCreateTableDocument2() throws Exception {
    testCreateTableDocument(1, "statustest1.xml");
  }

  public void testCreateTableDocument3() throws Exception {
    testCreateTableDocument(2, "statustest2.xml");
  }
}
