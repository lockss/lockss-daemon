/*
 * $Id$
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Representation of a data row in a database table.
 * 
 * @author Fernando Garcia-Loygorri
 */
package org.lockss.db;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DbRow {

  // The columns that form the row.
  private List<DbColumn> columns = new ArrayList<DbColumn>();

  // The name of the database table.
  private String tableName;

  /**
   * Constructor.
   * 
   * @param name A String with the name of the database table.
   */
  public DbRow(String tableName) {
    this.tableName = tableName;
  }

  public List<DbColumn> getColumns() {
    return columns;
  }

  public void setColumns(List<DbColumn> columns) {
    this.columns = columns;
  }

  public String getTableName() {
    return tableName;
  }

  public DbColumn[] getColumnsAsArray() {
    return columns.toArray(new DbColumn[columns.size()]);
  }

  /**
   * Provides the names of the tables that are foreign keys.
   * 
   * @return a Set<String> with the names of the tables that are foreign keys.
   */
  public Set<String> getFkTables() {
    Set<String> result = new HashSet<String>();

    // Loop through all the columns in the row.
    for (DbColumn column : columns) {
      String fkTable = column.getFkTable();

      // Check whether this column has a foreign key constraint.
      if (fkTable != null) {
	// Yes: Add it to the result.
	result.add(fkTable);
      }
    }

    return result;
  }

  /**
   * Provides the SQL query used to count data rows in the table that match the
   * data in this row.
   * 
   * @return a String with the SQL query used to count data rows in the table
   *         that match the data in this row.
   */
  public String getMatchingRowCountSql() {
    StringBuilder sql = new StringBuilder("select count(*) from " + tableName);
    boolean isFirst = true;

    // Loop through all the columns in the row.
    for (DbColumn column : getColumnsAsArray()) {
      // Handle the first column differently.
      if (isFirst) {
	sql.append(" where ");
	isFirst = false;
      } else {
	sql.append(" and ");
      }

      sql.append(column.getName());

      // Handle null values differently than non-null values.
      if (column.getValue() != null) {
	sql.append(" = ?");
      } else {
	sql.append(" is null");
      }
    }

    return sql.toString();
  }

  /**
   * Provides the SQL query used to read from the database all the values in one
   * of the rows in the same table as this row.
   * 
   * @return a String with the SQL query used to read from the database all the
   *         values in one of the rows in the same table as this row.
   */
  public String getReadRowSql() {
    return "select * from " + tableName;
  }

  /**
   * Provides the SQL query used to count the rows in the same table as this
   * row.
   * 
   * @return a String with the SQL query used to count the rows in the same
   *         table as this row.
   */
  public String getRowCountSql() {
    return "select count(*) from " + tableName;
  }

  /**
   * Provides the SQL query used to write to the database this row.
   * 
   * @return a String with the SQL query used to write to the database this row.
   */
  public String getWriteRowSql() {
    StringBuilder sql = new StringBuilder("insert into " + tableName + " (");
    StringBuilder values = new StringBuilder(") values (");
    boolean isFirst = true;

    // Loop through all the columns in the row.
    for (DbColumn column : getColumnsAsArray()) {
      // Handle the first column differently.
      if (isFirst) {
	isFirst = false;
      } else {
	sql.append(",");
	values.append(",");
      }

      sql.append(column.getName());

      // Handle primary keys differently than other values.
      if (column.isPk()) {
	values.append("default");
      } else {
	values.append("?");
      }
    }

    values.append(")");
    sql.append(values.toString());

    return sql.toString();
  }
}
