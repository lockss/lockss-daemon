/*
 * $Id: TestStatusServiceImpl.java,v 1.6 2003-03-15 02:32:11 troberts Exp $
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

package org.lockss.daemon.status;
import java.util.*;
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
    {"cache", "Cache", new Integer(ColumnDescriptor.TYPE_STRING)},
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
    MockStatusAccessor statusAccessor = generateStatusAccessor(colArray1, 
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
      generateStatusAccessor(colArray1, rowArray1, key);
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
      generateStatusAccessor(colArray1, rowArray1, key);
    statusAccessor.setDefaultSortRules(sortRules1, key);
    statusAccessor.setTitle(tableTitle, key);
    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", key);
    assertEquals(tableTitle, table.getTitle());
  }

  public void testGetTableWithKey() 
      throws StatusService.NoSuchTableException {
    String key = "key1";
    MockStatusAccessor statusAccessor = generateStatusAccessor(colArray1,
							       rowArray1, key);
    statusAccessor.setDefaultSortRules(sortRules1, key);

    statusService.registerStatusAccessor("table1", statusAccessor);
    
    StatusTable table = statusService.getTable("table1", key);

    List expectedColumns = makeColumnDescriptorsFromArray(colArray1);
    assertColumnDescriptorsEqual(expectedColumns, 
				 table.getColumnDescriptors());

    List expectedRows = makeRowsFromArray(expectedColumns, rowArray1);
    assertRowsEqual(expectedRows, table.getSortedRows());
  }


  public void testGetTablesWithDifferentKeys() 
      throws StatusService.NoSuchTableException {
    String key1 = "key1";
    String key2 = "key2";
    MockStatusAccessor statusAccessor = 
      generateStatusAccessor(colArray1, rowArray1, key1);
    statusAccessor.setDefaultSortRules(sortRules1, key1);

    addToStatusAccessor(statusAccessor, colArray2, rowArray2, key2);
    statusAccessor.setDefaultSortRules(sortRules2, key2);

    statusService.registerStatusAccessor("table1", statusAccessor);
    
    StatusTable table = statusService.getTable("table1", key1);
    assertNotNull(table);

    List expectedColumns = makeColumnDescriptorsFromArray(colArray1);
    assertColumnDescriptorsEqual(expectedColumns, 
				 table.getColumnDescriptors());

    List expectedRows = makeRowsFromArray(expectedColumns, rowArray1);
    assertRowsEqual(expectedRows, table.getSortedRows());

    table = statusService.getTable("table1", key2);
    assertNotNull(table);

    expectedColumns = makeColumnDescriptorsFromArray(colArray2);
    assertColumnDescriptorsEqual(expectedColumns, 
				 table.getColumnDescriptors());

    expectedRows = makeRowsFromArray(expectedColumns, rowArray2);
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
    
    MockStatusAccessor statusAccessor = generateStatusAccessor(colArray3, 
							       rowArray3);
    statusAccessor.setDefaultSortRules(sortRules, null);

    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows = 
      makeRowsFromArray(makeColumnDescriptorsFromArray(colArray3),
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

    MockStatusAccessor statusAccessor = generateStatusAccessor(colArray3, 
							       rowArray3);
    statusAccessor.setDefaultSortRules(sortRules, null);

    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows = 
      makeRowsFromArray(makeColumnDescriptorsFromArray(colArray3), 
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
      generateStatusAccessor(colArray3, rowArray3);
    statusAccessor.setDefaultSortRules(sortRules, null);

    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows = 
      makeRowsFromArray(makeColumnDescriptorsFromArray(colArray3), 
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
    
    MockStatusAccessor statusAccessor = generateStatusAccessor(colArray4, 
							       rowArray4);
    statusAccessor.setDefaultSortRules(sortRules, null);
    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows = 
      makeRowsFromArray(makeColumnDescriptorsFromArray(colArray4),
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

    MockStatusAccessor statusAccessor = generateStatusAccessor(colArray3, 
							       rowArray3);
    statusAccessor.setDefaultSortRules(sortRules1, null);

    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows = 
      makeRowsFromArray(makeColumnDescriptorsFromArray(colArray3), 
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

    MockStatusAccessor statusAccessor = generateStatusAccessor(colArray3, 
							       rowArray3);
    statusService.registerStatusAccessor("table1", statusAccessor);
    StatusTable table = statusService.getTable("table1", null);
    List expectedRows = 
      makeRowsFromArray(makeColumnDescriptorsFromArray(colArray3), 
			expectedRowsArray);
    List actualRows = table.getSortedRows();
    assertRowsEqual(expectedRows, actualRows);
  }

  static Object[][] allTablesExpectedColArray = 
  {
    {"table_name", "Table name", new Integer(ColumnDescriptor.TYPE_STRING)}
  };

  static Object[][] allTablesExpectedRowArray = 
  {
    {new StatusTable.Reference("A_table", "A_table", null)},
    {new StatusTable.Reference("B_table", "B_table", null)},
    {new StatusTable.Reference("F_table", "F_table", null)},
    {new StatusTable.Reference("Z_table", "Z_table", null)},
    {new StatusTable.Reference("table_of_all_tables", 
			       "table_of_all_tables", null)}
  };

  public void testGetTableOfAllTables() 
      throws StatusService.NoSuchTableException {
    statusService.startService(); //registers table of all tables
    statusService.registerStatusAccessor("A_table", new MockStatusAccessor());
    statusService.registerStatusAccessor("B_table", new MockStatusAccessor());
    statusService.registerStatusAccessor("F_table", new MockStatusAccessor());
    statusService.registerStatusAccessor("Z_table", new MockStatusAccessor());
    
    StatusTable table = 
      statusService.getTable(StatusService.ALL_TABLES_TABLE, null);

    assertNotNull(table);
    List expectedCols = 
      makeColumnDescriptorsFromArray(allTablesExpectedColArray);
    assertColumnDescriptorsEqual(expectedCols, 
				 table.getColumnDescriptors());

    List expectedRows = makeRowsFromArray(expectedCols,
					  allTablesExpectedRowArray);
    assertRowsEqual(expectedRows, table.getSortedRows());
  }
  
  public void testGetTableOfAllTablesFiltersTablesThatRequireKeys() 
      throws StatusService.NoSuchTableException {
    statusService.startService(); //registers table of all tables
    statusService.registerStatusAccessor("A_table", new MockStatusAccessor());
    statusService.registerStatusAccessor("B_table", new MockStatusAccessor());
    statusService.registerStatusAccessor("F_table", new MockStatusAccessor());
    statusService.registerStatusAccessor("Z_table", new MockStatusAccessor());

    MockStatusAccessor statusAccessor = new MockStatusAccessor();
    statusAccessor.setRequiresKey(true);
    statusService.registerStatusAccessor("excluded_table", statusAccessor);
    
    StatusTable table = 
      statusService.getTable(StatusService.ALL_TABLES_TABLE, null);

    assertNotNull(table);
    List expectedCols = 
      makeColumnDescriptorsFromArray(allTablesExpectedColArray);
    assertColumnDescriptorsEqual(expectedCols, 
				 table.getColumnDescriptors());

    List expectedRows = makeRowsFromArray(expectedCols,
					  allTablesExpectedRowArray);
    assertRowsEqual(expectedRows, table.getSortedRows());
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
      generateStatusAccessor(colArray1, rowArrayWithNulls, key);
    statusAccessor.setDefaultSortRules(sortRules1, key);

    statusService.registerStatusAccessor("table1", statusAccessor);
    
    StatusTable table = statusService.getTable("table1", key);

    List expectedColumns = makeColumnDescriptorsFromArray(colArray1);
    assertColumnDescriptorsEqual(expectedColumns, 
				 table.getColumnDescriptors());

    Object[][] expectedRowsArray = new Object[4][];
    expectedRowsArray[0] = rowArrayWithNulls[2];
    expectedRowsArray[1] = rowArrayWithNulls[0];
    expectedRowsArray[2] = rowArrayWithNulls[1];
    expectedRowsArray[3] = rowArrayWithNulls[3];

    List expectedRows = makeRowsFromArray(expectedColumns, expectedRowsArray);
    assertRowsEqual(expectedRows, table.getSortedRows());
  }




  /**
   * I don't normally like to write tests for test code, but this is a pretty 
   * simple one that can help shake out potential testing problems
   */
  public void testMakeColumnDescriptorsFromArray() {
    List expectedColumns = 
      ListUtil.list(new ColumnDescriptor("blah", "Blah", 0),
		    new ColumnDescriptor("blah2", "Blah2", 0));

    Object[][] cols = {
      {"blah", "Blah", new Integer(0)},
      {"blah2", "Blah2", new Integer(0)}
    };
    List actualColumns = makeColumnDescriptorsFromArray(cols);
    assertColumnDescriptorsEqual(expectedColumns, actualColumns);
  }

  //see comment for testMakeColumnDescriptorsFromArray
  public void testMakeRowsFromArray() {
    Object[][] cols = {
      {"col1", "Column the first", new Integer(0)},
      {"col2", "Column the second", new Integer(0)}
    };
    List columns = makeColumnDescriptorsFromArray(cols);

    HashMap row1 = new HashMap();
    row1.put("col1", "val1");
    row1.put("col2", "val2");

    HashMap row2 = new HashMap();
    row2.put("col1", "val21");
    row2.put("col2", "val22");

    HashMap row3 = new HashMap();
    row3.put("col1", "val31");
    row3.put("col2", "val32");
    List expectedRows = ListUtil.list(row1, row2, row3);

    Object[][] rows = {
      {"val1", "val2"},
      {"val21", "val22"},
      {"val31", "val32"}
    };
    List actualRows = makeRowsFromArray(columns, rows);
    assertRowsEqual(expectedRows, actualRows);
  }

  private List makeColumnDescriptorsFromArray(Object[][] cols) {
    List list = new ArrayList(cols.length);
    for (int ix = 0; ix < cols.length; ix++) {
      String footNote = null;
      if (cols[ix].length == 4) {
 	footNote = (String) cols[ix][3];
      }
      ColumnDescriptor col = 
	new ColumnDescriptor((String)cols[ix][0], (String)cols[ix][1],
			     ((Integer)cols[ix][2]).intValue(), footNote);
      list.add(col);
    }
    return list;
  }

  private List makeRowsFromArray(List cols, Object[][] rows) {
    List rowList = new ArrayList();
    for (int ix=0; ix<rows.length; ix++) {
      Map row = new HashMap();
      for (int jy=0; jy<rows[ix].length; jy++) {
	String colName = 
	  ((ColumnDescriptor)cols.get(jy)).getColumnName();
	if (rows[ix][jy] != null) {
	  row.put(colName, rows[ix][jy]);
	}
      }
      rowList.add(row);
    }
    return rowList;
  }

  private void addToStatusAccessor(MockStatusAccessor statusAccessor,
				   Object[][]colArray, 
				   Object[][]rowArray, String key) {
    List columns = makeColumnDescriptorsFromArray(colArray);
    List rows = makeRowsFromArray(columns, rowArray);
    statusAccessor.setColumnDescriptors(columns, key);
    statusAccessor.setRows(rows, key);
  }

  private MockStatusAccessor generateStatusAccessor(Object[][]colArray, 
						    Object[][]rowArray) {
    return generateStatusAccessor(colArray, rowArray, null);
  }
  private MockStatusAccessor generateStatusAccessor(Object[][]colArray, 
						    Object[][]rowArray,
						    String key) {
    MockStatusAccessor statusAccessor = new MockStatusAccessor();
    List columns = makeColumnDescriptorsFromArray(colArray);
    List rows = makeRowsFromArray(columns, rowArray);

    statusAccessor.setColumnDescriptors(columns, key);
    statusAccessor.setRows(rows, key);

    return statusAccessor;
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
      assertEquals(expectedCol.getFootNote(), actualCol.getFootNote());
    }
    assertFalse(actualIt.hasNext());
  }
}
