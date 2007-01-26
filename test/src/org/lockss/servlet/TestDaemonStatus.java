/*
 * $Id: TestDaemonStatus.java,v 1.7.32.1 2007-01-26 17:39:10 thib_gc Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

import java.util.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.lockss.test.*;
import org.lockss.util.*;
import org.lockss.daemon.status.*;
//import com.meterware.servletunit.*;
import com.meterware.httpunit.*;

/**
 * Test class for <code>org.lockss.servlet.DaemonStatus</code>
 */
public class TestDaemonStatus extends LockssServletTestCase {

  static Logger log = Logger.getLogger("TestDaemonStatus");

//  private DaemonStatus ds;

  private StatusService statSvc;
  private StatusServiceImpl ssi;

  protected void setUp() throws Exception {
    super.setUp();
//    ds = new DaemonStatus();
    statSvc = theDaemon.getStatusService();
    ssi = (StatusServiceImpl)statSvc;
    ssi.startService();
  }

  // Tests that don't need a servlet environment
  public void testConvertDisplayString() throws Exception {
    // test null
    Object testObj = null;
    assertEquals("", format(testObj, ColumnDescriptor.TYPE_STRING));

    // test standard numbers
    testObj = new Integer(123);
    assertEquals("123", format(testObj, ColumnDescriptor.TYPE_INT));
    testObj = new Float(123321);
    assertEquals(testObj.toString(),
                 format(testObj, ColumnDescriptor.TYPE_FLOAT));

    // check proper 'big int' formatting
    testObj = new Long(12345678);
    assertEquals("12,345,678", format(testObj, ColumnDescriptor.TYPE_INT));

    // test string
    testObj = "test string";
    assertEquals("test string", format(testObj, ColumnDescriptor.TYPE_STRING));
    
    // Issue 1901: verify that there is no encoding bias
    testObj = "<>&'\"\n";
    String res = format(testObj, ColumnDescriptor.TYPE_STRING);
    assertEquals("Expected \"" + StringEscapeUtils.escapeJava(testObj.toString())
                 + "\" but got \"" + StringEscapeUtils.escapeJava(res)
                 + "\"; encoding bias?",
                 "<>&'\"\n",
                 res);

    // test percentage
    testObj = new Double(.453);
    assertEquals("45%", format(testObj, ColumnDescriptor.TYPE_PERCENT));

    // test date
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 2004);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DATE, 1);
    cal.set(Calendar.HOUR_OF_DAY, 15);
    cal.set(Calendar.MINUTE, 15);
    testObj = cal.getTime();
    assertEquals(DaemonStatus.tableDf.format(testObj),
                 format(testObj, ColumnDescriptor.TYPE_DATE));

    // test IPAddr
    testObj = IPAddr.getLocalHost();
    assertEquals(IPAddr.getLocalHost().getHostAddress(),
                 format(testObj, ColumnDescriptor.TYPE_IP_ADDRESS));

    // test time interval
    long timeInt = Constants.HOUR + Constants.MINUTE;
    testObj = new Long(timeInt);
    assertEquals(StringUtil.timeIntervalToString(timeInt),
                 format(testObj, ColumnDescriptor.TYPE_TIME_INTERVAL));

    // test unknown
    testObj = "unknown string";
    assertEquals("unknown string", format(testObj, -1));
  }

  private String format(Object obj, int type) {
    return DaemonStatus.convertDisplayString(obj, type);
  }

  // Utilities for running the servlet

  protected void initServletRunner() {
    super.initServletRunner();
    sRunner.registerServlet("/DaemonStatus", DaemonStatus.class.getName() );
    // DaemonStatus wants there to be a local ip address
    ConfigurationUtil.setFromArgs(LockssServlet.PARAM_LOCAL_IP, "2.4.6.8");
  }


  // request a table from the servlet
  WebResponse getTable(String table, boolean text) throws Exception {
    initServletRunner();
    WebRequest request = new GetMethodWebRequest("http://null/DaemonStatus");
    request.setParameter( "table", table );
    if (text) {
      request.setParameter( "text", "1");
    }
    return sClient.getResponse(request);
  }

  // Break the line at commas, return a map of the resulting strings
  // broken at equals sign.  (<i>Ie</i>, name value pairs.)
  Map getRow(String line) {
    Map map = new HashMap();
    for (Iterator iter = StringUtil.breakAt(line, ',').iterator();
	 iter.hasNext(); ) {
      String item = (String)iter.next();
      List pair = StringUtil.breakAt(item, '=');
      map.put(pair.get(0), pair.get(1));
    }
    return map;
  }

  protected void assertEqualTables(Object[][]a1, List lines) {
    assertEquals("numrows", a1.length, lines.size() - NUM_HEADER_LINES);
    for (int irow = 0; irow <= a1.length-1; irow++) {
      Object expRow[] = a1[irow];
      List row =
	StringUtil.breakAt((String)lines.get(irow + NUM_HEADER_LINES), ',');
      assertEquals("numcols", expRow.length, row.size());
      assertEquals(("row " + irow),
		   SetUtil.fromArray(expRow), new HashSet(row));
    }
  }


  // Tests for text output

  /** Number of lines before start of table proper */
  static final int NUM_HEADER_LINES = 3;

  private static final Object[][] colArray1 = {
    {"name", "Name", new Integer(ColumnDescriptor.TYPE_STRING), "Foot note"},
    {"rank", "Rank", new Integer(ColumnDescriptor.TYPE_INT)}
  };

  private static final Object[][] colArrayWithNonString = {
    {StatusTable.ROW_SEPARATOR, "Foo", new Integer(ColumnDescriptor.TYPE_STRING)},
    {"rank", "Rank", new Integer(ColumnDescriptor.TYPE_INT)}
  };

  private static final Object[][] rowArray1 = {
    {"AA", "1"},
    {"BB", "2"},
    {"CC", "3"},
    {"DD", "4"}
  };

  private static final Object[][] table1 = {
    {"name=AA", "rank=1"},
    {"name=BB", "rank=2"},
    {"name=CC", "rank=3"},
    {"name=DD", "rank=4"}
  };

  private static final Object[][] rowArrayWithNulls = {
    {"AA", "1"},
    {"BB", "2"},
    {null, "3"},
    {"DD", null}
  };

  // null sorts to beginning of table
  private static final Object[][] tableWithNulls = {
    {"name=(null)", "rank=3"},
    {"name=AA", "rank=1"},
    {"name=BB", "rank=2"},
    {"name=DD", "rank=(null)"}
  };

  public void testText() throws Exception {
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray1,
						rowArray1);
    statusAccessor.setTitle("testtbl", null);
    statSvc.registerStatusAccessor("testtbl", statusAccessor);


    WebResponse resp = getTable("testtbl", true);
    assertResponseOk(resp);
    assertEquals("Content type", "text/plain", resp.getContentType());
    log.debug(resp.getText());
    List lines = getLines(resp);
    assertEquals(rowArray1.length + 3, lines.size());
    Map row0 = getRow((String)lines.get(0));
    assertEquals("2.4.6.8", row0.get("host"));

    Map row2 = getRow((String)lines.get(2));
    assertEquals("testtbl", row2.get("table"));

    assertEqualTables(table1, lines);
  }

  // test null value in rows doesn't throw
  public void testTextNull() throws Exception {
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray1,
						rowArrayWithNulls);
    statSvc.registerStatusAccessor("testtbl", statusAccessor);

    WebResponse resp = getTable("testtbl", true);
    log.debug(resp.getText());

    List lines = getLines(resp);
    assertEqualTables(tableWithNulls, lines);
  }

  // test special (non-string) key in row doesn't throw
  public void testTextNonStringKey() throws Exception {
    MockStatusAccessor statusAccessor = new MockStatusAccessor();
    List cols = ListUtil.list("foo", StatusTable.ROW_SEPARATOR);
    statusAccessor.setRows(MockStatusAccessor.makeRowsFrom(cols, rowArray1),
                           null);
    statusAccessor.setColumnDescriptors(MockStatusAccessor.makeColumnDescriptorsFrom(colArray1), null);

    statSvc.registerStatusAccessor("testtbl", statusAccessor);

    WebResponse resp = getTable("testtbl", true);
    log.debug(resp.getText());
  }

}
