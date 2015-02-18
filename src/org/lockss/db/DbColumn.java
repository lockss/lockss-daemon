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
 * Representation of a data column in a database table row.
 * 
 * @author Fernando Garcia-Loygorri
 */
package org.lockss.db;

import static java.sql.Types.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DbColumn implements Comparable<DbColumn> {

  // The name of a table that is a foreign key constraint for the column.
  private String fkTable = null;

  // The name of the column.
  private String name;

  // The ordinal position in the row of the column.
  private int ordinalPosition;

  // An indicaation of whether the column is the primary key of the row.
  private boolean pk = false;

  // The java.sql.Types value of the type of the column.
  private int type;

  // The data value of the column.
  private Object value = null;

  /**
   * Constructor.
   * 
   * @param name
   *          A String with the name of the column.
   * @param type
   *          An int with the java.sql.Types value of the type of the column.
   * @param ordinalPosition
   *          An int with the ordinal position in the row of the column.
   */
  public DbColumn (String name, int type, int ordinalPosition) {
    this.name = name;
    this.type = type;
    this.ordinalPosition = ordinalPosition;
  }

  /**
   * Comparator.
   */
  public int compareTo(DbColumn other) {
    // Sort by ordinal position.
    return getOrdinalPosition() - other.getOrdinalPosition();
  }

  public String getFkTable() {
    return fkTable;
  }

  public void setFkTable(String fkTable) {
    this.fkTable = fkTable;
  }

  public String getName() {
    return name;
  }

  public int getOrdinalPosition() {
    return ordinalPosition;
  }

  public void setOrdinalPosition(int ordinalPosition) {
    this.ordinalPosition = ordinalPosition;
  }

  /**
   * Sets the appropriate parameter in a prepared statement with the value of
   * the column.
   * 
   * @param statement
   *          A PreparedStatement with the stament where to set the parameter.
   * @param index
   *          An int with the position of the parameter in the statement.
   * 
   * @throws SQLException
   *           if there are problems setting the parameter.
   */
  public void setParameter(PreparedStatement statement, int index)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, type);
    } else {
      statement.setObject(index, value, type);
    }
  }

  public boolean isPk() {
    return pk;
  }

  public void setPk(boolean pk) {
    this.pk = pk;
  }

  public int getType() {
    return type;
  }

  public Object getValue() {
    return value;
  }

  /**
   * Provides the value of this column in a result set.
   * 
   * @param resultSet
   *          A ResultSet with the result set.
   * @return an Object with the value of this column in the result set.
   * 
   * @throws SQLException
   *           if there are problems reading the result set.
   */
  public Object getValue(ResultSet resultSet) throws SQLException {
    switch (type) {
    case BIGINT:
      value = resultSet.getLong(name);
      break;

    case BOOLEAN:
      value = resultSet.getBoolean(name);
      break;

    case INTEGER:
      value = resultSet.getInt(name);
      break;

    case SMALLINT:
      value = resultSet.getShort(name);
      break;

    case VARCHAR:
      value = resultSet.getString(name);
    }

    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return "DbColumn [fkTable=" + fkTable + ", name=" + name
	+ ", ordinalPosition=" + ordinalPosition + ", pk=" + pk
	+ ", type=" + type + ", value=" + value + "]";
  }
}
