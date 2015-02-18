/*
 * $Id$
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
import java.net.*;
import org.lockss.test.*;
import org.lockss.util.*;


public class TestStatusServiceImpl extends LockssTestCase {
  private static final Object[][] colArray1 = {
    {"name", "Name", new Integer(ColumnDescriptor.TYPE_STRING), "Foot note"},
    {"rank", "Rank", new Integer(ColumnDescriptor.TYPE_INT)}
  };

  private static final Object[][] rowArray1 = {
    {"AA", "1"},
    {"BB", "2"},
    {"CC", "3"},
    {"DD", "4"}
  };

  private static final List sortRules1 =
    ListUtil.list(new StatusTable.SortRule("name", true));

  private static final Object[][] colArray2 = {
    {"cache", "Box", new Integer(ColumnDescriptor.TYPE_STRING)},
  };

  private static final Object[][] rowArray2 = {
    {"Cache A"},
    {"Cache B"},
    {"Cache C"},
    {"Cache D"},
    {"Cache E"},
    {"Cache F"},
    {"Cache G"}
  };

  private static final List sortRules2 =
    ListUtil.list(new StatusTable.SortRule("cache", true));

  private static final Object[][] colArray3 = {
    {"name", "Name", new Integer(ColumnDescriptor.TYPE_STRING)},
    {"rank", "Rank", new Integer(ColumnDescriptor.TYPE_INT)},
  };

  private static final Object[][] rowArray3 = {
    {"Cache B", new Integer(1)},
    {"Cache A", new Integer(2)},
    {"Cache C", new Integer(0)}
  };

  private static final Object[][] colArray4 = {
    {"name", "Name", new Integer(ColumnDescriptor.TYPE_STRING)},
    {"rank", "Rank", new Integer(ColumnDescriptor.TYPE_INT)},
    {"secondRank", "SecondRank", new Integer(ColumnDescriptor.TYPE_INT)},
  };

  private static final Object[][] rowArray4 = {
    {"AName", new Integer(0), new Integer(400)},
    {"BName", new Integer(2), new Integer(450)},
    {"BName", new Integer(4), new Integer(0)},
    {"BName", new Integer(4), new Integer(2)},
    {"CName", new Integer(1), new Integer(-1)}
  };

  private StatusServiceImpl statusService;

  public void setUp() throws Exception {
    super.setUp();
    statusService = new StatusServiceImpl();
    statusService.initService(getMockLockssDaemon());
  }


  public void testGetTableWithNullTableNameThrows()
      throws StatusService.NoSuchTableException {
    try {
      statusService.getTable(null, "blah");
      fail("Should have thrown when given a null name");
    } catch (StatusService.NoSuchTableException iae) {
    }
  }

  public void testGetTableWithUnknownTableThrows() {
    try {
      statusService.getTable("Bad name", "bad key");
      fail("Should have thrown when given a bad name and key");
    } catch (StatusService.NoSuchTableException tnfe) {
    }
  }

  public void testMultipleRegistriesThrows() {
    statusService.registerStatusAccessor("table1", new MockStatusAccessor());
    try {
      statusService.registerStatusAccessor("table1", new MockStatusAccessor());
      fail("Should have thrown after multiple register attempts");
    } catch (StatusService.MultipleRegistrationException re) {
    }
  }

  public void testRegisteringAllTableThrows() {
    try {
      statusService.startService(); //registers table of all tables
      statusService.registerStatusAccessor(StatusService.ALL_TABLES_TABLE,
					   new MockStatusAccessor());
      fail("Should have thrown after trying to register StatusAccessor for "+
	   "all tables");
    } catch (StatusService.MultipleRegistrationException re) {
    }
  }

  public void testRegisteringInvalidTableNameThrows() {
    try {
      statusService.registerStatusAccessor("!Table",
					   new MockStatusAccessor());
      fail("Should have thrown after trying to register StatusAccessor "+
	   "with bad table name");
    } catch (StatusService.InvalidTableNameException re) {
    }

    try {
      statusService.registerStatusAccessor("name with spaces",
					   new MockStatusAccessor());
      fail("Should have thrown after trying to register StatusAccessor "+
	   "with bad table name");
    } catch (StatusService.InvalidTableNameException re) {
    }
  }

  public void testUnregisteringBadDoesntThrow() {
    statusService.unregisterStatusAccessor("table1");
  }

  public void testMultipleUnregisteringDontThrow() {
    statusService.registerStatusAccessor("table1", new MockStatusAccessor());
    statusService.unregisterStatusAccessor("table1");
    statusService.unregisterStatusAccessor("table1");
  }

  public void testCanRegisterUnregisteredTable() {
    statusService.registerStatusAccessor("table1", new MockStatusAccessor());
    statusService.unregisterStatusAccessor("table1");
    statusService.registerStatusAccessor("table1", new MockStatusAccessor());
  }

  public void testGetTableHasName()
      throws StatusService.NoSuchTableException {
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray1,
						rowArray1);
    statusAccessor.setDefaultSortRules(sortRules1, null);
    statusService.registerStatusAccessor("table1", statusAccessor);

    StatusTable table = statusService.getTable("table1", null);
    assertEquals("table1", table.getName());
    assertNull(table.getKey());
  }

  public void testGetTableHasKey()
      throws StatusService.NoSuchTableException {
    String key = "theKey";
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray1, rowArray1, key);
    statusAccessor.setDefaultSortRules(sortRules1, key);
    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", key);
    assertEquals(key, table.getKey());
  }

  public void testGetTableTitle()
      throws StatusService.NoSuchTableException {
    String key = "theKey";
    String tableTitle = "Table title";
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray1, rowArray1, key);
    statusAccessor.setDefaultSortRules(sortRules1, key);
    statusAccessor.setTitle(tableTitle, key);
    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", key);
    assertEquals(tableTitle, table.getTitle());
  }

  static final Object[][] summaryInfo = {
    {"SummaryInfo1", new Integer(ColumnDescriptor.TYPE_STRING), "SummaryInfo value one"},
    {"SummaryInfo2", new Integer(ColumnDescriptor.TYPE_INT), new Integer(2)},
    {"SummaryInfo3", new Integer(ColumnDescriptor.TYPE_STRING), "SummaryInfo value 3"}
  };

  public void testGetTableSummaryInfo()
      throws StatusService.NoSuchTableException {
    String key = "theKey";
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray1, rowArray1,
						key, summaryInfo);

    statusAccessor.setDefaultSortRules(sortRules1, key);
    statusService.registerStatusAccessor("table1", statusAccessor);

    StatusTable table = statusService.getTable("table1", key);
    List expectedSummaryInfo =
      MockStatusAccessor.makeSummaryInfoFrom(summaryInfo);
    assertNotNull(table.getSummaryInfo());
    assertSummaryInfoEqual(expectedSummaryInfo, table.getSummaryInfo());
  }

  private void assertSummaryInfoEqual(List expected, List actual) {
    assertEquals("Lists had different sizes", expected.size(), actual.size());
    Iterator expectedIt = expected.iterator();
    Iterator actualIt = actual.iterator();
    while(expectedIt.hasNext()) {
      StatusTable.SummaryInfo expectedSInfo =
	(StatusTable.SummaryInfo)expectedIt.next();
      StatusTable.SummaryInfo actualSInfo =
	(StatusTable.SummaryInfo)actualIt.next();
      assertEquals(expectedSInfo.getTitle(), actualSInfo.getTitle());
      assertEquals(expectedSInfo.getType(), actualSInfo.getType());
      assertEquals(expectedSInfo.getValue(), actualSInfo.getValue());
    }
    assertFalse(actualIt.hasNext());
  }

  public void testGetTableWithKey()
      throws StatusService.NoSuchTableException {
    String key = "key1";
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray1, rowArray1, key);
    statusAccessor.setDefaultSortRules(sortRules1, key);

    statusService.registerStatusAccessor("table1", statusAccessor);

    StatusTable table = statusService.getTable("table1", key);

    List expectedColumns =
      MockStatusAccessor.makeColumnDescriptorsFrom(colArray1);
    assertColumnDescriptorsEqual(expectedColumns,
				 table.getColumnDescriptors());

    List expectedRows =
      MockStatusAccessor.makeRowsFrom(expectedColumns, rowArray1);
    assertRowsEqual(expectedRows, table.getSortedRows());
  }


  public void testGetTablesWithDifferentKeys()
      throws StatusService.NoSuchTableException {
    String key1 = "key1";
    String key2 = "key2";
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray1, rowArray1, key1);
    statusAccessor.setDefaultSortRules(sortRules1, key1);

    MockStatusAccessor.addToStatusAccessor(statusAccessor, colArray2,
					   rowArray2, key2);
    statusAccessor.setDefaultSortRules(sortRules2, key2);

    statusService.registerStatusAccessor("table1", statusAccessor);

    StatusTable table = statusService.getTable("table1", key1);
    assertNotNull(table);

    List expectedColumns =
      MockStatusAccessor.makeColumnDescriptorsFrom(colArray1);
    assertColumnDescriptorsEqual(expectedColumns,
				 table.getColumnDescriptors());

    List expectedRows =
      MockStatusAccessor.makeRowsFrom(expectedColumns, rowArray1);
    assertRowsEqual(expectedRows, table.getSortedRows());

    table = statusService.getTable("table1", key2);
    assertNotNull(table);

    expectedColumns = MockStatusAccessor.makeColumnDescriptorsFrom(colArray2);
    assertColumnDescriptorsEqual(expectedColumns,
				 table.getColumnDescriptors());

    expectedRows = MockStatusAccessor.makeRowsFrom(expectedColumns, rowArray2);
    assertRowsEqual(expectedRows, table.getSortedRows());
  }

  public void testSortsByAscStrings()
      throws StatusService.NoSuchTableException {
    StatusTable.SortRule rule = new StatusTable.SortRule("name", true);
    List sortRules = ListUtil.list(rule);

    Object[][] expectedRowsArray = new Object[3][];
    expectedRowsArray[0] = rowArray3[1];
    expectedRowsArray[1] = rowArray3[0];
    expectedRowsArray[2] = rowArray3[2];

    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray3, rowArray3);
    statusAccessor.setDefaultSortRules(sortRules, null);

    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows =
      MockStatusAccessor.makeRowsFrom(MockStatusAccessor.makeColumnDescriptorsFrom(colArray3),
				      expectedRowsArray);
    List actualRows = table.getSortedRows();
    assertRowsEqual(expectedRows, actualRows);
  }

  public void testGetTableSortsDescStrings()
      throws StatusService.NoSuchTableException {
    StatusTable.SortRule rule = new StatusTable.SortRule("name", false);
    List sortRules = ListUtil.list(rule);

    Object[][] expectedRowsArray = new Object[3][];
    expectedRowsArray[0] = rowArray3[2];
    expectedRowsArray[1] = rowArray3[0];
    expectedRowsArray[2] = rowArray3[1];

    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray3, rowArray3);
    statusAccessor.setDefaultSortRules(sortRules, null);

    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows =
      MockStatusAccessor.makeRowsFrom(MockStatusAccessor.makeColumnDescriptorsFrom(colArray3),
				      expectedRowsArray);
    List actualRows = table.getSortedRows(sortRules);
    assertRowsEqual(expectedRows, actualRows);
  }

  public void testGetTableSortsNumsAsc()
      throws StatusService.NoSuchTableException {
    StatusTable.SortRule rule = new StatusTable.SortRule("rank", true);
    List sortRules = ListUtil.list(rule);

    Object[][] expectedRowsArray = new Object[3][];
    expectedRowsArray[0] = rowArray3[2];
    expectedRowsArray[1] = rowArray3[0];
    expectedRowsArray[2] = rowArray3[1];

    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray3, rowArray3);
    statusAccessor.setDefaultSortRules(sortRules, null);

    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows =
      MockStatusAccessor.makeRowsFrom(MockStatusAccessor.makeColumnDescriptorsFrom(colArray3),
				      expectedRowsArray);
    List actualRows = table.getSortedRows(sortRules);
    assertRowsEqual(expectedRows, actualRows);
  }

  public void testGetTableSortsMultiCols()
      throws StatusService.NoSuchTableException {
    List sortRules =
      ListUtil.list(new StatusTable.SortRule("name", false),
		    new StatusTable.SortRule("rank", true),
		    new StatusTable.SortRule("secondRank", false));

    Object[][] expectedRowsArray = new Object[5][];
    expectedRowsArray[0] = rowArray4[4];
    expectedRowsArray[1] = rowArray4[1];
    expectedRowsArray[2] = rowArray4[3];
    expectedRowsArray[3] = rowArray4[2];
    expectedRowsArray[4] = rowArray4[0];

    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray4, rowArray4);
    statusAccessor.setDefaultSortRules(sortRules, null);
    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows =
      MockStatusAccessor.makeRowsFrom(MockStatusAccessor.makeColumnDescriptorsFrom(colArray4),
				      expectedRowsArray);
    List actualRows = table.getSortedRows();
    assertRowsEqual(expectedRows, actualRows);
  }

  public void testSortsByNonDefaultRules()
      throws StatusService.NoSuchTableException {
    List sortRules = ListUtil.list(new StatusTable.SortRule("name", false));

    Object[][] expectedRowsArray = new Object[3][];
    expectedRowsArray[0] = rowArray3[2];
    expectedRowsArray[1] = rowArray3[0];
    expectedRowsArray[2] = rowArray3[1];

    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray3, rowArray3);
    statusAccessor.setDefaultSortRules(sortRules1, null);

    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows =
      MockStatusAccessor.makeRowsFrom(MockStatusAccessor.makeColumnDescriptorsFrom(colArray3),
				      expectedRowsArray);
    List actualRows = table.getSortedRows(sortRules);
    assertRowsEqual(expectedRows, actualRows);
  }

  public void testSortsByDefaultDefaultRules()
      throws StatusService.NoSuchTableException {
    Object[][] expectedRowsArray = new Object[3][];
    expectedRowsArray[0] = rowArray3[1];
    expectedRowsArray[1] = rowArray3[0];
    expectedRowsArray[2] = rowArray3[2];

    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray3, rowArray3);
    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows =
      MockStatusAccessor.makeRowsFrom(MockStatusAccessor.makeColumnDescriptorsFrom(colArray3),
				      expectedRowsArray);
    List actualRows = table.getSortedRows();
    assertRowsEqual(expectedRows, actualRows);
  }

  static Object[][] allTablesExpectedColArray =
  {
    {"table_name", "Available Tables",
     new Integer(ColumnDescriptor.TYPE_STRING)}
  };

  static Object[][] allTablesExpectedRowArray =
  {
    {new StatusTable.Reference("MockStatusAccessor", "A_table", null)},
    {new StatusTable.Reference("MockStatusAccessor", "B_table", null)},
    {new StatusTable.Reference("MockStatusAccessor", "F_table", null)},
    {new StatusTable.Reference("MockStatusAccessor", "Z_table", null)},
  };

  static Object[][] allTablesExpectedRowArrayDebugUser =
  {
    {new StatusTable.Reference("MockStatusAccessor", "A_table", null)},
    {new StatusTable.Reference("MockStatusAccessor", "B_table", null)},
    {new StatusTable.Reference("MockStatusAccessor", "F_table", null)},
    {new StatusTable.Reference("MockStatusAccessor", "Debug_table", null)},
    {new StatusTable.Reference("MockStatusAccessor", "Z_table", null)},
  };

  public void registerSomeTables() {
    statusService.registerStatusAccessor("A_table",
					 makeMockStatusAccessor(null));
    statusService.registerStatusAccessor("B_table",
					 makeMockStatusAccessor("B Title"));
    statusService.registerStatusAccessor("F_table",
					 makeMockStatusAccessor("F_table"));
    statusService.registerStatusAccessor("Debug_table",
					 makeMockStatusAccessorDebugOnly("Debug_table"));
    statusService.registerStatusAccessor("Z_table",
					 makeMockStatusAccessor("Z_table"));
  }

  public void testGetTableOfAllTables()
      throws StatusService.NoSuchTableException {
    statusService.startService(); //registers table of all tables
    registerSomeTables();

    StatusTable table =
      statusService.getTable(StatusService.ALL_TABLES_TABLE, null);

    assertNotNull(table);
    List expectedCols =
      MockStatusAccessor.makeColumnDescriptorsFrom(allTablesExpectedColArray);
    assertColumnDescriptorsEqual(expectedCols,
				 table.getColumnDescriptors());

    List expectedRows = MockStatusAccessor.makeRowsFrom(expectedCols,
					  allTablesExpectedRowArray);
    assertRowsEqualNoOrder(expectedRows, table.getSortedRows());
  }

  public void testGetTableOfAllTablesDebugUser()
      throws StatusService.NoSuchTableException {
    statusService.startService(); //registers table of all tables
    registerSomeTables();

    BitSet tableOptions = new BitSet();
    tableOptions.set(StatusTable.OPTION_DEBUG_USER);
    StatusTable table = new StatusTable(StatusService.ALL_TABLES_TABLE, null);
    table.setOptions(tableOptions);
    statusService.fillInTable(table);

    List expectedCols =
      MockStatusAccessor.makeColumnDescriptorsFrom(allTablesExpectedColArray);
    assertColumnDescriptorsEqual(expectedCols,
				 table.getColumnDescriptors());

    List expectedRows = MockStatusAccessor.makeRowsFrom(expectedCols,
					  allTablesExpectedRowArrayDebugUser);
    assertRowsEqualNoOrder(expectedRows, table.getSortedRows());
  }

  MockStatusAccessor makeMockStatusAccessor(String title) {
    MockStatusAccessor sa = new MockStatusAccessor();
    sa.setTitle(title, null);
    return sa;
  }

  MockStatusAccessor makeMockStatusAccessorDebugOnly(String title) {
    MockStatusAccessor sa = new MockStatusAccessorDebugOnly();
    sa.setTitle(title, null);
    return sa;
  }

  class MockStatusAccessorDebugOnly
    extends MockStatusAccessor implements StatusAccessor.DebugOnly {
  }

  public void testGetTableOfAllTablesFiltersTablesThatRequireKeys()
      throws StatusService.NoSuchTableException {
    statusService.startService(); //registers table of all tables
    statusService.registerStatusAccessor("A_table",
					 makeMockStatusAccessor("A_table"));
    statusService.registerStatusAccessor("B_table",
					 makeMockStatusAccessor("B Title"));
    statusService.registerStatusAccessor("F_table",
					 makeMockStatusAccessor("F_table"));
    statusService.registerStatusAccessor("Z_table",
					 makeMockStatusAccessor("Z_table"));

    MockStatusAccessor statusAccessor = new MockStatusAccessor();
    statusAccessor.setRequiresKey(true);
    statusService.registerStatusAccessor("excluded_table", statusAccessor);

    StatusTable table =
      statusService.getTable(StatusService.ALL_TABLES_TABLE, null);

    assertNotNull(table);
    List expectedCols =
      MockStatusAccessor.makeColumnDescriptorsFrom(allTablesExpectedColArray);
    assertColumnDescriptorsEqual(expectedCols,
				 table.getColumnDescriptors());

    List expectedRows = MockStatusAccessor.makeRowsFrom(expectedCols,
					  allTablesExpectedRowArray);
    assertRowsEqualNoOrder(expectedRows, table.getSortedRows());
  }


  private static final Object[][] rowArrayWithNulls = {
    {"AA", "1"},
    {"BB", "2"},
    {null, "3"},
    {"DD", "4"}
  };

  public void testNullRowValues()
      throws StatusService.NoSuchTableException {
    String key = "key1";
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(colArray1, rowArrayWithNulls,
						key);
    statusAccessor.setDefaultSortRules(sortRules1, key);

    statusService.registerStatusAccessor("table1", statusAccessor);

    StatusTable table = statusService.getTable("table1", key);

    List expectedColumns =
      MockStatusAccessor.makeColumnDescriptorsFrom(colArray1);
    assertColumnDescriptorsEqual(expectedColumns,
				 table.getColumnDescriptors());

    Object[][] expectedRowsArray = new Object[4][];
    expectedRowsArray[0] = rowArrayWithNulls[2];
    expectedRowsArray[1] = rowArrayWithNulls[0];
    expectedRowsArray[2] = rowArrayWithNulls[1];
    expectedRowsArray[3] = rowArrayWithNulls[3];

    List expectedRows =
      MockStatusAccessor.makeRowsFrom(expectedColumns, expectedRowsArray);
    assertRowsEqual(expectedRows, table.getSortedRows());
  }

  private static final Object[][] inetAddrColArray = {
    {"address", "Address", new Integer(ColumnDescriptor.TYPE_IP_ADDRESS)},
    {"name", "Name", new Integer(ColumnDescriptor.TYPE_STRING)}
  };

  public void testSortsIPAddres()
      throws UnknownHostException, StatusService.NoSuchTableException {
    Object[][] inetAddrRowArray = {
      {IPAddr.getByName("127.0.0.2"), "A"},
      {IPAddr.getByName("127.0.0.1"), "B"},
      {IPAddr.getByName("127.0.0.4"), "C"},
      {IPAddr.getByName("127.0.0.3"), "D"}
    };
    String key = "key1";
    MockStatusAccessor statusAccessor =
      MockStatusAccessor.generateStatusAccessor(inetAddrColArray,
						inetAddrRowArray, key);
    List rules = ListUtil.list(new StatusTable.SortRule("address", true));
    statusAccessor.setDefaultSortRules(rules, key);

    statusService.registerStatusAccessor("table1", statusAccessor);

    StatusTable table = statusService.getTable("table1", key);

    List expectedColumns =
      MockStatusAccessor.makeColumnDescriptorsFrom(inetAddrColArray);

    Object[][] expectedRowsArray = new Object[4][];
    expectedRowsArray[0] = inetAddrRowArray[1];
    expectedRowsArray[1] = inetAddrRowArray[0];
    expectedRowsArray[2] = inetAddrRowArray[3];
    expectedRowsArray[3] = inetAddrRowArray[2];

    List expectedRows =
      MockStatusAccessor.makeRowsFrom(expectedColumns, expectedRowsArray);
    assertRowsEqual(expectedRows, table.getSortedRows());
  }

  private void assertRowsEqual(List expected, List actual) {
    assertEquals("Different number of rows", expected.size(), actual.size());
    Iterator expectedIt = expected.iterator();
    Iterator actualIt = actual.iterator();
    int rowNum=0;
    while (expectedIt.hasNext()) {
      Map expectedMap = (Map)expectedIt.next();
      Map actualMap = (Map)actualIt.next();
      assertEquals("Failed on row "+rowNum, expectedMap, actualMap);
      rowNum++;
    }
  }

  // This should be equivalent to Set.equals(), but when called on two sets
  // of Maps that fails and this succeeds.  What am I missing?
  private void assertRowsEqualNoOrder(Collection expected, Collection actual) {
    assertEquals("Different number of rows", expected.size(), actual.size());
    Iterator expectedIt = expected.iterator();
    while (expectedIt.hasNext()) {
      Map expectedMap = (Map)expectedIt.next();
      assertTrue("missing: " + expectedMap, actual.contains(expectedMap));
    }
  }

  private void assertColumnDescriptorsEqual(List expected, List actual) {
    assertEquals("Lists had different sizes", expected.size(), actual.size());
    Iterator expectedIt = expected.iterator();
    Iterator actualIt = actual.iterator();
    while(expectedIt.hasNext()) {
      ColumnDescriptor expectedCol = (ColumnDescriptor)expectedIt.next();
      ColumnDescriptor actualCol = (ColumnDescriptor)actualIt.next();
      assertEquals(expectedCol.getColumnName(), actualCol.getColumnName());
      assertEquals(expectedCol.getTitle(), actualCol.getTitle());
      assertEquals(expectedCol.getType(), actualCol.getType());
      assertEquals(expectedCol.getFootnote(), actualCol.getFootnote());
    }
    assertFalse(actualIt.hasNext());
  }

  public void testGetDefaultTableName() {
    assertEquals(OverviewStatus.OVERVIEW_STATUS_TABLE,
		 statusService.getDefaultTableName());
    ConfigurationUtil.addFromArgs(StatusServiceImpl.PARAM_DEFAULT_TABLE,
				  "other_table_and_chairs");
    assertEquals("other_table_and_chairs",
		 statusService.getDefaultTableName());
  }

  public void testRegisterOveriewAccessor() {
    statusService.registerOverviewAccessor("table1",
					   new OverviewAccessor() {
					     public Object getOverview(String tableName, 
								       BitSet options) {
					       return "over1";
					     }});
    statusService.registerOverviewAccessor("table2",
					   new OverviewAccessor() {
					     public Object getOverview(String tableName, 
								       BitSet options) {
					       return "over2";
					     }});
    assertEquals("over1", statusService.getOverview("table1"));
    assertEquals("over2", statusService.getOverview("table2"));
    assertNull(statusService.getOverview("table3"));
  }

  public void testRegisterObjectReferenceAccessor() {
    MockObjectReferenceAccessor refAcc = new MockObjectReferenceAccessor();
    statusService.registerObjectReferenceAccessor("table1", Integer.class,
						  refAcc);
    try {
      // 2nd register should fail
      statusService.registerObjectReferenceAccessor("table1", Integer.class,
						    refAcc);
      fail("Should have thrown after multiple register attempts");
    } catch (StatusService.MultipleRegistrationException re) {
    }
    // should be able to unregister then reregister
    statusService.unregisterObjectReferenceAccessor("table1", Integer.class);
    statusService.registerObjectReferenceAccessor("table1", Integer.class,
						  refAcc);
  }

  public void testGetReference() {
    MockObjectReferenceAccessor refAcc = new MockObjectReferenceAccessor();
    refAcc.setRef(new StatusTable.Reference("value", "table1", "key"));
    statusService.registerObjectReferenceAccessor("table1", C2.class,
						  refAcc);
    assertNull(statusService.getReference("table2", new C2()));
    assertNull(statusService.getReference("table1", new Integer(42)));
    assertNull(statusService.getReference("table1", new C1()));
    StatusTable.Reference ref = statusService.getReference("table1", new C2());
    assertNotNull(ref);
    assertEquals("value", ref.getValue());
    assertEquals("table1", ref.getTableName());
  }

  public void testSAThrowsAreTrapped() throws
    StatusService.NoSuchTableException {
    StatusAccessor statusAccessor = new StatusAccessor() {
	public String getDisplayName() {
	  return null;
	}
	public void populateTable(StatusTable table) {
	  throw new NullPointerException();
	}
	public boolean requiresKey() {
	  return false;
	}
      };
    statusService.startService(); //registers table of all keyless tables
    statusService.registerStatusAccessor("table1", statusAccessor);
    statusService.getTable("table_of_all_tables", null);
  }

  private class C1 {}
  private class C2 extends C1 {}
  private class C3 extends C2 {}
}
