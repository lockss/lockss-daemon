/*
 * $Id: StatusTable.java,v 1.19 2003-03-26 23:12:04 tal Exp $
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
import java.net.*;
import org.lockss.util.*;

/**
 * Returned by {@link StatusService#getTable(String, String)} 
 */
public class StatusTable {
  private String name;
  private String key;
  private String title;
  private String titleFootnote;
  private List columnDescriptors;
  private List rows;
  private List defaultSortRules;
  private static Logger logger = Logger.getLogger("StatusTable");
  private List summaryInfo;


  /**
   * @param name String representing table name
   * @param key String representing the key for this table, may be null
   */
  public StatusTable(String name, String key) {
    this.name = name;
    this.key = key;
  }

  /**
   * Constructor for tables that don't have a key
   * @param name String representing table name
   */
  public StatusTable(String name) {
    this(name, null);
  }

  private List makeDefaultSortRules() {
    ColumnDescriptor firstCol = (ColumnDescriptor)columnDescriptors.get(0);
    SortRule sortRule = new SortRule(firstCol.getColumnName(), true);
    return ListUtil.list(sortRule);
  }

  /**
   * Get the name of this table
   * @returns name of this table
   */
  public String getName() {
    return name;
  }

  protected void setName(String name) {
    this.name = name;
  }

  /**
   * Get the key for this table
   * @returns key for this table
   */
  public String getKey() {
    return key;
  }

  /**
   * Get the title for this table
   * @returns title for this table
   */
  public String getTitle() {
    return title;
  }
  /**
   * Sets the title for this table
   * @param title title of this table
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Get the title footnote for this table
   * @returns title footnote for this table
   */
  public String getTitleFootnote() {
    return titleFootnote;
  }

  /**
   * Set the title footnote for this table
   * @paramfootnote the title footnote for this table
   */
  public void setTitleFootnote(String footnote) {
    this.titleFootnote = footnote;
  }

  /**
   * Returns a List of {@link SummaryInfo} objects for this table
   * @return List of {@link SummaryInfo} objects for this table
   */
  public List getSummaryInfo() {
    return summaryInfo;
  }

  /**
   * Sets a list of {@link SummaryInfo} objects for this table
   * @param summaryInfo list of {@link SummaryInfo} objects for this table
   */
  public void setSummaryInfo(List summaryInfo) {
    this.summaryInfo = summaryInfo;
  }

  
  /**
   * Gets a list of {@link ColumnDescriptor}s representing the 
   * columns in this table in their preferred display order.
   * @returns list of {@link ColumnDescriptor}s the columns in 
   * the table in the preferred display order
   */
  public List getColumnDescriptors() {
    return columnDescriptors;
  }

  /**
   * Sets a list of {@link ColumnDescriptor}s in their perferred display 
   * order for this table
   * @param columnDescriptors List of {@link ColumnDescriptor}s in their 
   * perferred display order for this table
   */
  public void setColumnDescriptors(List columnDescriptors) {
    this.columnDescriptors = columnDescriptors;
  }

  /**
   * Gets a list of {@link java.util.Map} objects for all the rows in the 
   * table in their default sort order.
   * @returns list of {@link java.util.Map}s representing rows in the table 
   * in their default sort order 
   */
  public List getSortedRows() {
    if (defaultSortRules == null) {
      defaultSortRules = makeDefaultSortRules();
    }      
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

  /**
   * Set the rows ({@link Map}s) for this table
   * @param rows List of unsorted rows for this table
   */
  public void setRows(List rows) {
    this.rows = rows;
  }

  /** Return the actual value, possibly embedded in a {@link
   * StatusTable.DisplayedValue} and/or a {@link
   * StatusTable.Reference}
   * @param value an object, possibly an DisplayedValue or Reference
   * @return The innermost embedded value that is not an DisplayedValue
   * or a Reference.
   */
  public static Object getActualValue(Object value) {
    if (value instanceof DisplayedValue) {
      return getActualValue(((DisplayedValue)value).getValue());
    } else if (value instanceof Reference) {
      return getActualValue(((Reference)value).getValue());
    } else return value;
  }

  /**
   * Sets the default {@link StatusTable.SortRule}s for this table
   * @param defaultSortRules List of default {@link StatusTable.SortRule}s 
   * for this table
   */
  public void setDefaultSortRules(List defaultSortRules) {
    this.defaultSortRules = defaultSortRules;
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

  /**
   * Wrapper for a value with additional display properties
   */
  public static class DisplayedValue {
    private Object value;
    private String color = null;

    /** Create a DisplayedValue with the specified value */
    public DisplayedValue(Object value) {
      this.value = value;
    }

    /** Get the value */
    public Object getValue() {
      return value;
    }

    /** Set the color.
     * @param color the name of the color (understandable by html)
     */
    public void setColor(String color) {
      this.color = color;
    }

    /** Get the color */
    public String getColor() {
      return color;
    }

  }
  /**
   * Object which refers to another table
   */
  public static class Reference {
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

      //true iff both strings are equal or null
      return StringUtil.equalStrings(key, ref.getKey());
    }
  }

  /**
   * Object representing scalar information in a table
   */
  public static class SummaryInfo {
    private String title;
    private int type;
    private Object value;

    /**
     * @param title title for this SummaryInfo
     * @param type int representing the type of value
     * @param value value object associated with this SummaryInfo
     */    
    public SummaryInfo(String title, int type, Object value) {
      this.title = title;
      this.type = type;
      this.value = value;
    }

    public String getTitle() {
      return this.title;
    }

    public int getType() {
      return this.type;
    }

    public Object getValue() {
      return value;
    }
  }

  private static class SortRuleComparator implements Comparator {
    List sortRules;

    public SortRuleComparator(List sortRules, List columnDescriptors) {
      this.sortRules = sortRules;
      setSortTypes(columnDescriptors);
    }

    private void setSortTypes(List columnDescriptors) {
      Iterator it = sortRules.iterator();
      while (it.hasNext()) {
	SortRule rule = (SortRule)it.next();
	if (rule.getColumnType() < 0) {
	  rule.inferColumnType(columnDescriptors);
	}
      }
    }

    public int compare(Object a, Object b) {
      Map rowA = (Map)a;
      Map rowB = (Map)b;
      int returnVal = 0;
      Iterator it = sortRules.iterator();

      while (returnVal == 0 && it.hasNext()){
	SortRule sortRule = (SortRule)it.next();
	String colName = sortRule.getColumnName();
	// Either of both objects might be either a Reference or an
	// DisplayedValue.  We want to compare the actual value.
	Object valA = getActualValue(rowA.get(colName));
	Object valB = getActualValue(rowB.get(colName));

	
	switch (sortRule.getColumnType()) {
	case ColumnDescriptor.TYPE_IP_ADDRESS:
	  returnVal = compareInetAddresses((InetAddress)valA, 
					   (InetAddress)valB);
	  break;
	case ColumnDescriptor.TYPE_INT:
	case ColumnDescriptor.TYPE_FLOAT:
	case ColumnDescriptor.TYPE_PERCENT:
	case ColumnDescriptor.TYPE_TIME_INTERVAL:
	case ColumnDescriptor.TYPE_STRING:
	default: //if we don't know the type, assume comparable
	  returnVal = compareHandlingNulls((Comparable)valA, (Comparable)valB);
	  break;
	}
	returnVal = sortRule.sortAscending ? returnVal : -returnVal;
      }
      return returnVal;
    }
    
    private int compareInetAddresses(InetAddress addr1, InetAddress addr2) {
      return (addr1.getHostAddress().compareTo(addr2.getHostAddress()));
    }

    private static int compareHandlingNulls(Comparable val1,
					    Comparable val2) {
      int returnVal = 0;
      if (val1 == null) {
	returnVal = val2 == null ? 0 : -1;
      } else if (val2 == null) {
	returnVal = 1;
      } else {
	returnVal = val1.compareTo(val2);
      }
      return returnVal;
    }
  }

  /**
   * Encapsulation of the info needed to sort on a single field
   */
  public static class SortRule {
    String columnName;
    boolean sortAscending;
    int columnType = -1;
    
    public SortRule(String columnName, boolean sortAscending) {
      this.columnName = columnName;
      this.sortAscending = sortAscending;
    }

    public SortRule(String columnName, boolean sortAscending, int columnType) {
      this(columnName, sortAscending);
      this.columnType = columnType;
    }
    /**
     * @returns name of the field to sort on
     */
    public String getColumnName(){
      return columnName;
    }
    
    /**
     * @returns the value type for the column
     */
    public int getColumnType(){
      return columnType;
    }
    
    /**
     * @returns true if this column should be sorted in ascending order,
     * false if it should be sorted in descending order
     */
    public boolean sortAscending(){
      return sortAscending;
    }

    /**
     * Lookup the column type in the columnDescriptors, store in self
     * @param columnDescriptors
     */
    void inferColumnType(List columnDescriptors){
      for (Iterator iter = columnDescriptors.iterator(); iter.hasNext(); ) {
	ColumnDescriptor col = (ColumnDescriptor)iter.next();
	if (columnName.equals(col.getColumnName())) {
	  columnType = col.getType();
	  return;
	}
      }
      // XXX this isn't really an error, just somebody sorting on a
      // column that isn't displayed.
      logger.warning("Unknown type for sort column: "+ columnName);
      columnType = ColumnDescriptor.TYPE_INT;
    }
  }

}
