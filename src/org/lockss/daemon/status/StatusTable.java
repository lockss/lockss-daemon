/*
 * $Id: StatusTable.java,v 1.6 2003-03-14 01:49:29 troberts Exp $
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

package org.lockss.daemon.status;

import java.util.*;

/**
 * Returned by {@link StatusService#getTable(String, Object)} 
 */
public class StatusTable {
  public static final int TYPE_INT=0;
  public static final int TYPE_FLOAT=1;
  public static final int TYPE_PERCENT=2;
  public static final int TYPE_TIME_INTERVAL=3;
  public static final int TYPE_STRING=4;
  public static final int TYPE_IP_ADDRESS=5;
  public static final int TYPE_DATE=6;


  private String name=null;
  private Object key=null;
  private List columnDescriptors;
  private List rows;
  private List defaultSortRules;

  protected StatusTable(String name, Object key, List columnDescriptors, 
			List defaultSortRules, List rows) {
    if (defaultSortRules == null) {
      throw new IllegalArgumentException("Created with an null list of "
					 +"sort rules");
    }
    if (defaultSortRules.size() == 0) {
      throw new IllegalArgumentException("Created with an empty list of "
					 +"sort rules");
    }
    this.name = name;
    this.columnDescriptors = columnDescriptors;
    this.rows = makeRowList(rows);
    this.defaultSortRules = defaultSortRules;
    this.key = key;
  }

  /**
   * Get the name of this table
   * @returns name of this table
   */
  public String getName() {
    return name;
  }

  /**
   * Get the key for this table
   * @returns key for this table
   */
  public Object getKey() {
    return key;
  }

  /**
   * Gets a list of {@link StatusTable.ColumnDescriptor}s representing the 
   * columns in this table in their perferred display order.
   * @returns list of {@link StatusTable.ColumnDescriptor}s the columns in 
   * the table in the perferred display order
   */
  public List getColumnDescriptors() {
    return columnDescriptors;
  }

  /**
   * Gets a list of {@link java.util.Map} objects for all the rows in the 
   * table in their default sort order.
   * @returns list of {@link java.util.Map}s representing rows in the table 
   * in their default sort order 
   */
  public List getSortedRows() {
    return getSortedRows(defaultSortRules);
  }

  /**
   * Same as getSortedRows(), but will sort according to the rules 
   * specified in sortRules
   * @param sortRules list of {@link StatusTable.SortRule} objects describing
   *  how to sort  the rows
   * @returns list of {@link java.util.Map}s representing rows in the table 
   * in the sort order specified by sortRules 
   */
  public List getSortedRows(List sortRules) {
    Collections.sort(rows, 
		     new SortRuleComparator(sortRules, columnDescriptors));
    return rows;
  }

  private List makeRowList(List newRows) {
    List rows = new ArrayList(newRows.size());
    Iterator it = newRows.iterator();
    while (it.hasNext()) {
      rows.add((Map)it.next());  //cast to catch bad type
    }
    return rows;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[StatusTable:");
    sb.append(name);
    sb.append(", ");
    sb.append(key);
    sb.append(", ");
    sb.append(columnDescriptors);
    sb.append(", ");
    sb.append(rows);
    sb.append("]");
    return sb.toString();
  }

  private static class SortRuleComparator implements Comparator {
    List sortRules;
    Map columnTypeMap;

    public SortRuleComparator(List sortRules, List columns) {
      this.sortRules = sortRules;
      columnTypeMap = makeColumnTypeMap(columns);
    }

    private Map makeColumnTypeMap(List columns) {
      Iterator it = columns.iterator();
      Map columnTypeMap = new HashMap();
      while (it.hasNext()) {
	ColumnDescriptor col = (ColumnDescriptor) it.next();
	columnTypeMap.put(col.getColumnName(), 
			  new Integer(col.getType()));
      }
      return columnTypeMap;
    }

    public int compare(Object a, Object b) {
      Map mapA = (Map)a;
      Map mapB = (Map)b;
      int returnVal = 0;
      Iterator it = sortRules.iterator();

      while (returnVal == 0 && it.hasNext()){
	SortRule sortRule = (SortRule)it.next();
	int type = getType(sortRule.getFieldName());
	Comparable val1 = (Comparable)mapA.get(sortRule.getFieldName());
	Comparable val2 = (Comparable)mapB.get(sortRule.getFieldName());
	returnVal = sortRule.sortAscending() ? 
	            val1.compareTo(val2) : -val1.compareTo(val2);
      }
      return returnVal;
    }
    private int getType(String field) {
      return ((Integer)columnTypeMap.get(field)).intValue();
    }
  }

  /**
   * Encapsulation of the info needed to sort on a single field
   */
  public static class SortRule {
    String field;
    boolean sortAscending;
    
    public SortRule(String field, boolean sortAscending) {
      this.field = field;
      this.sortAscending = sortAscending;
    }
    /**
     * @returns name of the field to sort on
     */
    public String getFieldName(){
      return field;
    }
    
    /**
     * @returns true if this field should be sorted in ascending order,
     * false if it should be sorted in decending order
     */
    public boolean sortAscending(){
      return sortAscending;
    }
  }

  /**
   * Encapsulation of the info needed to describe a single column (name, 
   * display title, and type)
   */
  public static class ColumnDescriptor {
    private String columnName;
    private String title;
    private int type;

    public ColumnDescriptor(String columnName, String title, int type) {
      this.columnName = columnName;
      this.title = title;
      this.type = type;
    }

    public String getColumnName() {
      return columnName;
    }

    public String getTitle() {
      return title;
    }

    public int getType() {
      return type;
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[StatusTable.ColumnDescriptor:");
      sb.append(columnName);
      sb.append(", ");
      sb.append(title);
      sb.append(", ");
      sb.append(type);
      sb.append("]");
      return sb.toString();
    }

  }

  /**
   * Object which refers to another table
   */
  public static class Reference implements Comparable {
    private Object value;
    private String tableName;
    private String key;

    /**
     * @param value value to be displayed
     * @param tableName name of the {@link StatusTable} that this 
     * links to
     * @param key object further specifying the table this links to
     */
    public Reference(Object value, String tableName, String key){
      this.value = value;
      this.tableName = tableName;
      this.key = key;
    }

    public Object getValue() {
      return value;
    }
    
    public String getTableName() {
      return tableName;
    }

    public String getKey() {
      return key;
    }

    public int compareTo(Object obj) {
      Reference ref = (Reference)obj;
      return ((Comparable)value).compareTo(ref.getValue());
    }

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("[StatusTable.Reference:");
      sb.append(value);
      sb.append(", ");
      sb.append(tableName);
      sb.append(", ");
      sb.append(key);
      sb.append("]");
      return sb.toString();
    }

    public boolean equals(Object obj) {
      if (! (obj instanceof StatusTable.Reference)) {
  	return false;
      }
      StatusTable.Reference ref = (StatusTable.Reference)obj;
      if (!value.equals(ref.getValue())) {
	return false;
      }
      if (!tableName.equals(ref.getTableName())) {
	return false;
      }
      if (key == null) {
	return ref.getKey() == null;
      }
      return key.equals(ref.getKey());
    }
      

  }
}
