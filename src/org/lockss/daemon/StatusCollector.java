/*
 * $Id: StatusCollector.java,v 1.1 2003-03-10 18:45:52 troberts Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * This object sits between the daemon and the UI code.  Basically, it 
 * functions as a centralized place to query for status information on the 
 * system.
 */
public interface StatusCollector {

  /**
   * Returns the StatusCollector.Table object identified by the tableName 
   * and key specified.
   * @param tableName name of the table to get
   * @param key object which further specifies the needed table
   * @returns StatusTable object with specified name
   * @throws TableNotFoundException if there is no status table with 
   * that name
   */
  public Table getTable(String tableName, Object key) 
      throws TableNotFoundException;

  public interface Table {
    /**
     * Get the name of this table
     * @returns name of this table
     */
    public String getName();

    /**
     * Get the key for this table
     * @returns key for this table
     */
    public Object getKey();

    /**
     * Gets a list of all the fields in this table in their perferred display
     * order.
     * @returns list of the fields in the table in the perferred display order
     */
    public List getFieldNames();

    /**
     * Gets a list of TableRow objects for all the rows in the table in their
     * default sort order.
     * @returns list of rows in the table in the perferred order
     */
    public List getSortedRows();

    /**
     * Same as getSortedRows(), but will sort according to the rules 
     * specified in sortRules
     * @param sortRules list of SortRule objects describing how to sort 
     * the rows
     * @returns list of rows sorted by the sorter
     */
    public List getSortedRows(List sortRules);
  }

  public interface TableRow {
    /**
     * @param fieldName name of the field to return
     * @returns object in field specified by fieldName
     */
    public Object getField(String fieldName);
  }

  public interface SortRule {
    /**
     * @returns name of the field to sort on
     */
    public String getFieldName();

    /**
     * @returns true if this field should be sorted in ascending order,
     * false if it should be sorted in decending order
     */
    public boolean sortAscending();
  }

  public class TableNotFoundException extends Exception {
  }

}
