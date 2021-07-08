/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.util.*;
import org.lockss.protocol.*;
import org.lockss.servlet.ServletDescr;

/**
 * Returned by {@link StatusService#getTable(String, String)}
 */
public class StatusTable {
  /** Adding this key to a row, with a non-null value, requests a separator
   * before the row. */
  public static final Object ROW_SEPARATOR = new Object();

  public static final int OPTION_NO_ROWS = 1;
  public static final int OPTION_DEBUG_USER = 2;
  public static final OrderedObject NO_VALUE =
    new OrderedObject("-", new Long(-1));

  public static final String PROP_COLUMNS = "columns";

  private String name;
  private String key;
  private String title = null;
  private String titleFootnote;
  private List columnDescriptors;
  private Map columnDescriptorMap;
  private List rows;
  private List defaultSortRules;
  private static Logger logger = Logger.getLogger("StatusTable");
  private List summaryInfo;
  private BitSet options = new BitSet();
  private boolean isResortable = true;
  private Properties props;
  private List<String> orderedFootnotes;

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
    if (columnDescriptors == null || columnDescriptors.isEmpty()) {
      return Collections.EMPTY_LIST;
    }
    ColumnDescriptor firstCol = (ColumnDescriptor)columnDescriptors.get(0);
    SortRule sortRule = new SortRule(firstCol.getColumnName(), true);
    return ListUtil.list(sortRule);
  }

  /**
   * Get the name of this table
   * @return name of this table
   */
  public String getName() {
    return name;
  }

  protected void setName(String name) {
    this.name = name;
  }

  /**
   * Get the key for this table
   * @return key for this table
   */
  public String getKey() {
    return key;
  }

  /**
   * Get the title for this table
   * @return title for this table
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
   * @return title footnote for this table
   */
  public String getTitleFootnote() {
    return titleFootnote;
  }

  /**
   * Set the title footnote for this table
   * @param footnote the title footnote for this table
   */
  public void setTitleFootnote(String footnote) {
    this.titleFootnote = footnote;
  }

  /**
   * Get the ordered list of footnotes for this table
   * @return the ordered list of footnotes for this table
   */
  public List<String> getOrderedFootnotes() {
    if (orderedFootnotes == null) {
      return Collections.emptyList();
    }
    return orderedFootnotes;
  }

  /**
   * Set the ordered list of footnotes for this table.  They will be added
   * to the display in this order.  Needed in cases where the footnote
   * order is important, because they would otherwise be created in
   * variable order depending on the current table sorting.
   * @param footnotes (a subset of) the footnotes for this table in the
   * order they should appear
   */
  public void setOrderedFootnotes(List<String> footnotes) {
    this.orderedFootnotes = footnotes;
  }

  /**
   * Sets the options for this table
   */
  public void setOptions(BitSet options) {
    this.options = options;
  }

  /**
   * Returns the options for this table
   * @return BitSet in which to set and test options
   */
  public BitSet getOptions() {
    return options;
  }

  public void setProperty(String key, String val) {
    if (props == null) {
      props = new Properties();
    }
    props.setProperty(key, val);
  }

  public Properties getProperties() {
    return props;
  }

  public String getProperty(String key) {
    if (props == null) {
      return null;
    }
    return props.getProperty(key);
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
   * @return list of {@link ColumnDescriptor}s the columns in
   * the table in the preferred display order
   */
  public List<ColumnDescriptor> getColumnDescriptors() {
    return columnDescriptors;
  }

  /**
   * Returns a map that maps column name to ColumnDescriptor
   */
  public Map getColumnDescriptorMap() {
    if (columnDescriptorMap == null) {
      columnDescriptorMap = new HashMap();
      for (Iterator iter = columnDescriptors.iterator(); iter.hasNext(); ) {
	ColumnDescriptor col = (ColumnDescriptor)iter.next();
	columnDescriptorMap.put(col.getColumnName(), col);
      }
    }
    return columnDescriptorMap;
  }

  /**
   * Sets a list of {@link ColumnDescriptor}s in their preferred display
   * order for this table
   * @param columnDescriptors List of {@link ColumnDescriptor}s in their
   * preferred display order for this table
   */
  public void setColumnDescriptors(List columnDescriptors) {
    setColumnDescriptors(columnDescriptors, null, null);
  }

  /**
   * Sets a list of {@link ColumnDescriptor}s in their preferred display
   * order for this table
   * @param columnDescriptors List of {@link ColumnDescriptor}s in their
   * preferred display order for this table.  Will be filtered by the
   * table's <code>columns</code> property, if any, or the default list, if
   * not null.
   * @param defaultCols Default list of column names if not specified in
   * table, or null for no default filter.
   */
  public void setColumnDescriptors(List columnDescriptors,
				   List<String>defaultCols) {
    setColumnDescriptors(columnDescriptors, defaultCols, null);
  }

  /**
   * Sets a list of {@link ColumnDescriptor}s in their preferred display
   * order for this table
   * @param columnDescriptors List of {@link ColumnDescriptor}s in their
   * preferred display order for this table.  Will be filtered by the
   * table's <code>columns</code> property, if any, or the default list, if
   * not null.
   * @param defaultColProp Default columns property (query arg):
   * semicolon-separated list of column names, optionally preceded by "-"
   * (negation).
   */
  public void setColumnDescriptors(List columnDescriptors,
				   String defaultColProp) {
    setColumnDescriptors(columnDescriptors, null, defaultColProp);
  }

  /**
   * Sets a list of {@link ColumnDescriptor}s in their preferred display
   * order for this table
   * @param columnDescriptors List of {@link ColumnDescriptor}s in their
   * preferred display order for this table.  Will be filtered by the
   * table's <code>columns</code> property, if any, or the default list, if
   * not null.
   * @param defaultColProp Default columns property (query arg):
   * semicolon-separated list of column names, optionally preceded by "-"
   * (negation).
   */
  public void setColumnDescriptors(List columnDescriptors,
				   List<String>defaultCols,
				   String defaultColProp) {
    this.columnDescriptors =
      filterColDescs(columnDescriptors, defaultCols, defaultColProp);
    columnDescriptorMap = null;
  }

  /**
   * Filter the list of {@link ColumnDescriptor}s by the list of names
   * specfied by the <code>columns</code> property of the table, if any,
   * else the default list, if any.
   * @param colDescs List of {@link ColumnDescriptor}s in their preferred
   * display order for this table.
   * @param defaultCols Default list of column names if not specified in
   * table, or null for no default filter.
   */
  public List<ColumnDescriptor> filterColDescs(List<ColumnDescriptor>colDescs,
					       List<String>defaultCols) {
    return filterColDescs(colDescs, defaultCols, null);
  }

  /**
   * Filter the list of {@link ColumnDescriptor}s by the list of names
   * specfied by the <code>columns</code> property of the table, if any,
   * else the default list, if any.
   * @param colDescs List of {@link ColumnDescriptor}s in their preferred
   * display order for this table.
   * @param defaultColProp Default columns property (query arg):
   * semicolon-separated list of column names, optionally preceded by "-"
   * (negation).
   */
  public List<ColumnDescriptor> filterColDescs(List<ColumnDescriptor>colDescs,
					       String defaultColProp) {
    return filterColDescs(colDescs, null, defaultColProp);
  }

  /**
   * Filter the list of {@link ColumnDescriptor}s by the list of names
   * specfied by the <code>columns</code> property of the table, if any,
   * else the default list, if any.
   * @param colDescs List of {@link ColumnDescriptor}s in their preferred
   * display order for this table.
   * @param defaultCols Default list of column names if not specified in
   * table, or null for no default filter.
   * @param defaultColProp Default columns property (query arg):
   * semicolon-separated list of column names, optionally preceded by "-"
   * (negation).
   */
    List<ColumnDescriptor> filterColDescs(List<ColumnDescriptor>colDescs,
					  List<String>defaultCols,
					  String defaultColProp) {
    List<String> cols = defaultCols;
    String colprop = getProperty(PROP_COLUMNS);
    if (colprop == null) {
      colprop = defaultColProp;
    }
    boolean neg = false;
    if (!StringUtil.isNullString(colprop)) {
      if ("*".equals(colprop) || "All".equalsIgnoreCase(colprop)) {
	return colDescs;
      }
      neg = colprop.startsWith("-");
      if (neg) {
	colprop = colprop.substring(1);
      }
      cols = (List<String>)StringUtil.breakAt(colprop, ";");
    }
    if (cols == null) {
      return colDescs;
    }
    List<ColumnDescriptor> res = new ArrayList<ColumnDescriptor>();
    if (neg) {
      Set excl = new HashSet(cols);
      for (ColumnDescriptor desc : colDescs) {
	if (!excl.contains(desc.getColumnName())) {
	  res.add(desc);
	}
      }
    } else {
      Map<String,ColumnDescriptor> map = new HashMap<String,ColumnDescriptor>();
      for (ColumnDescriptor col : colDescs) {
	map.put(col.getColumnName(), col);
      }
      for (String colName : cols) {
	ColumnDescriptor desc = map.get(colName);
	if (desc != null) {
	  res.add(desc);
	}
      }
    }
    return res;
  }

  public boolean isIncludeColumn(String colName) {
    return getColumnDescriptorMap().containsKey(colName);
  }

  /**
   * Gets a list of {@link java.util.Map} objects for all the rows in the
   * table in their default sort order.
   * @return list of {@link java.util.Map}s representing rows in the table
   * in their default sort order
   */
  public List getSortedRows() {
    if (rows == null) {
      return Collections.EMPTY_LIST;
    }
    return getSortedRows(getDefaultSortRules());
  }

  /**
   * Same as getSortedRows(), but will sort according to the rules
   * specified in sortRules
   * @param sortRules list of {@link StatusTable.SortRule} objects describing
   *  how to sort  the rows
   * @return list of {@link java.util.Map}s representing rows in the table
   * in the sort order specified by sortRules
   */
  public List getSortedRows(List sortRules) {
    Collections.sort(rows, new SortRuleComparator(sortRules,
						  getColumnDescriptorMap()));
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
   * StatusTable.DisplayedValue} and/or a {@link StatusTable.LinkValue}
   * @param value an object, possibly a DisplayedValue or LinkValue
   * @return The innermost embedded value that is not a DisplayedValue
   * or a LinkValue.
   */
  public static Object getActualValue(Object value) {
    while (value instanceof EmbeddedValue) {
      value = ((EmbeddedValue)value).getValue();
    }
    return value;
  }

  /**
   * Sets the default {@link StatusTable.SortRule}s for this table
   * @param defaultSortRules List of default {@link StatusTable.SortRule}s
   * for this table
   */
  public void setDefaultSortRules(List defaultSortRules) {
    this.defaultSortRules = defaultSortRules;
  }

  /**
   * Gets the default {@link StatusTable.SortRule}s for this table
   */
  public List getDefaultSortRules() {
    if (defaultSortRules == null) {
      defaultSortRules = makeDefaultSortRules();
    }
    return defaultSortRules;
  }

  /** Set whether the table may be resorted by the user via the UI */
  public void setResortable(boolean isResortable) {
    this.isResortable = isResortable;
  }

  /** @return true if the table allows sorting from the UI */
  public boolean isResortable() {
    return isResortable;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
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
   * Interface for embedded values
   */
  public interface EmbeddedValue {
    public Object getValue();
  }

  /** Marker interface for EmbeddedValues that represent some form of link.
   * A LinkValue may not be embedded in another LinkValue, directly or
   * indirectly. */
  public interface LinkValue extends EmbeddedValue {
  }

  /**
   * Wrapper for a value with additional display properties.
   */
  public static class DisplayedValue implements EmbeddedValue {
    private Object value;
    private String color = null;
    private List<String> footnotes = null;
    private boolean bold = false;
    private Layout layout = Layout.None;
    private String displayStr;  // if present, human-friendly display string
    private String hoverText;  // if present, hover text

    /** Create a DisplayedValue with the specified value.  Any
     * non-EmbeddedValue value is legal.
     * @param value the wrapped value
     */
    public DisplayedValue(Object value) {
      if (value instanceof EmbeddedValue) {
	throw new IllegalArgumentException("Value of a DisplayedValue can't be an EmbeddedValue");
      }
      this.value = value;
    }

    /** Create a DisplayedValue with the specified value and display value.
     * Any non-EmbeddedValue value is legal. 
     * @param value the wrapped value
     * @param displayString human-friendly display value
     */
    public DisplayedValue(Object value, String displayString) {
      this(value);
      this.displayStr = displayString;
    }

    /** Get the value */
    public Object getValue() {
      return value;
    }

    /** Set the color.
     * @param color the name of the color (understandable by html)
     */
    public DisplayedValue setColor(String color) {
      this.color = color;
      return this;
    }

    /** Get the color */
    public String getColor() {
      return color;
    }

    /** Set the human-friendly display string.
     * @param displayString human-friendly display string
     */
    public DisplayedValue setDisplayString(String displayString) {
      this.displayStr = displayString;
      return this;
    }

    /** Get the human-friendly display string, if any */
    public String getDisplayString() {
      return displayStr;
    }

    /** Return true if a human-friendly display string has been supplied */
    public boolean hasDisplayString() {
      return displayStr != null;
    }

    /** Set bold.
     * @param bold true if should be bold
     */
    public DisplayedValue setBold(boolean bold) {
      this.bold = bold;
      return this;
    }

    /** Get the bold */
    public boolean getBold() {
      return bold;
    }

    /** Add footnote.
     * @param footnote the footnote string
     */
    public DisplayedValue addFootnote(String footnote) {
      if (footnotes == null) {
        footnotes = new ArrayList<>(4);
      }
      footnotes.add(footnote);
      return this;
    }

    /** Get the footnotes */
    public List<String> getFootnotes() {
      if (footnotes == null) {
        return Collections.emptyList();
      }
      return footnotes;
    }

    /** Set layout.
     * @param layout the layout selector
     */
    public DisplayedValue setLayout(Layout layout) {
      if (layout == null) {
	throw new IllegalArgumentException("null layout");
      }
      this.layout = layout;
      return this;
    }

    /** Get the layout */
    public Layout getLayout() {
      return layout;
    }

    /** Set hover text.
     * @param hoverText the hover text
     */
    public DisplayedValue setHoverText(String hoverText) {
      if (hoverText == null) {
	throw new IllegalArgumentException("null hoverText");
      }
      this.hoverText = hoverText;
      return this;
    }

    /** Get the hover text */
    public String getHoverText() {
      return hoverText;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[StatusTable.DisplayedValue: ");
      sb.append(value);
      if (hasDisplayString()) {
	sb.append(", dv: ");
	sb.append(getDisplayString());
      }
      if (getColor() != null) {
	sb.append(", color: ");
	sb.append(getColor());
      }	
      if (getBold()) {
	sb.append(", bold");
      }	
      if (getFootnotes() != null) {
	sb.append(", foot: ");
	sb.append(getFootnotes());
      }	
      return sb.toString();
    }

    public enum Layout {None, Column};

  }

  /**
   * Object which refers to another table
   */
  public static class Reference implements LinkValue {
    private Object value;
    private PeerIdentity peerId;
    private String tableName;
    private String key;
    private Properties props;

    /**
     * Create a Reference object with an embedded value.
     * @param value value to be displayed.  Any value is
     * legal except another LinkValue
     * @param tableName name of the {@link StatusTable} that this
     * links to
     */
    public Reference(Object value, String tableName) {
      this(value, tableName, null);
    }

    /**
     * Create a Reference object with an embedded value.
     * @param value value to be displayed.  Any value is
     * legal except another LinkValue
     * @param tableName name of the {@link StatusTable} that this
     * links to
     * @param key object further specifying the table this links to
     */
    public Reference(Object value, String tableName, String key) {
      if (value instanceof LinkValue) {
	throw new IllegalArgumentException("Value of a Reference can't be a LinkValue");
      }
      this.value = value;
      this.tableName = tableName;
      this.key = key;
    }

    /**
     * Create a Reference to a table on a peer
     * @param value value to be displayed.  Any value is
     * legal except another LinkValue
     * @param peerId the peer whose table to link to
     * @param tableName name of the {@link StatusTable} that this
     * links to
     */
    public Reference(Object value, PeerIdentity peerId, String tableName) {
      this(value, peerId, tableName, null);
    }

    /**
     * Create a Reference object with an embedded value.
     * @param value value to be displayed.  Any value is
     * legal except another LinkValue
     * @param peerId the peer whose table to link to
     * @param tableName name of the {@link StatusTable} that this
     * links to
     * @param key object further specifying the table this links to
     */
    public Reference(Object value, PeerIdentity peerId,
		     String tableName, String key) {
      if (value instanceof LinkValue) {
	throw new IllegalArgumentException("Value of a Reference can't be a LinkValue");
      }
      this.value = value;
      this.peerId = peerId;
      this.tableName = tableName;
      this.key = key;
    }

    public void setProperty(String key, String val) {
      if (props == null) {
	props = new Properties();
      }
      props.setProperty(key, val);
    }

    public Properties getProperties() {
      return props;
    }

    public Object getValue() {
      return value;
    }

    public PeerIdentity getPeerId() {
      return peerId;
    }

    public String getTableName() {
      return tableName;
    }

    public String getKey() {
      return key;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[StatusTable.Reference: ");
      sb.append(value);
      sb.append(", ");
      if (peerId != null) {
	sb.append(peerId);
	sb.append(", ");
      }	
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
      return (StringUtil.equalStrings(key, ref.getKey())
	      && (peerId == null
		  ? ref.getPeerId() == null
		  : peerId.equals(ref.getPeerId())));
      
    }

    public int hashCode() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Object wrapping a link to a servlet
   */
  public static class SrvLink implements LinkValue {
    private Object value;
    private ServletDescr srvDescr;
    private Properties args;

    /**
     * Create a SrvLink object with an embedded value and a URL to link to.
     * @param value value to be displayed.  Any value is legal except
     * another LinkValue.
     * @param srvDescr descriptor for servlet to link to
     * @param args optional servlet parameters
     */
    public SrvLink(Object value, ServletDescr srvDescr, Properties args){
      if (value instanceof LinkValue) {
	throw new IllegalArgumentException("Value of a SrvLink can't be a LinkValue");
      }
      this.value = value;
      this.srvDescr = srvDescr;
      this.args = args;
    }

    public Object getValue() {
      return value;
    }

    public ServletDescr getServletDescr() {
      return srvDescr;
    }

    public Properties getArgs() {
      return args;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[StatusTable.SrvLink: ");
      sb.append(value);
      sb.append(", ");
      sb.append(srvDescr.getPath());
      sb.append(args);
      sb.append("]");
      return sb.toString();
    }

    public boolean equals(Object obj) {
      if (! (obj instanceof StatusTable.SrvLink)) {
  	return false;
      }
      StatusTable.SrvLink link = (StatusTable.SrvLink)obj;
      return value.equals(link.getValue()) &&
	srvDescr.equals(link.getServletDescr()) &&
	args.equals(link.getArgs());
    }

    public int hashCode() {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Object representing scalar information in a table
   */
  public static class SummaryInfo {
    private String title;
    private int type;
    private Object value;
    private String headerFootnote;
    private String valueFootnote;

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

    public SummaryInfo(String title, int type, int value) {
      this(title, type, new Integer(value));
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

    public String getHeaderFootnote() {
      return this.headerFootnote;
    }

    public void setHeaderFootnote(String footnote) {
      this.headerFootnote = footnote;
    }

    public String getValueFootnote() {
      return this.valueFootnote;
    }

    public void setValueFootnote(String footnote) {
      this.valueFootnote = footnote;
    }
  }

  static class SortRuleComparator implements Comparator {
    List sortRules;

    public SortRuleComparator(List sortRules, Map columnDescriptorMap) {
      this.sortRules = sortRules;
      setSortTypes(columnDescriptorMap);
    }

    private void setSortTypes(Map columnDescriptorMap) {
      Iterator it = sortRules.iterator();
      while (it.hasNext()) {
	SortRule rule = (SortRule)it.next();
	if (rule.getColumnType() < 0) {
	  rule.inferColumnType(columnDescriptorMap);
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
	// Either object might be either an EmbeddedValue.  We want to
	// compare the actual value.
	Object valA = getSortValue(rowA.get(colName));
	Object valB = getSortValue(rowB.get(colName));
	returnVal = sortRule.compare(valA, valB);
      }
      return returnVal;
    }

    // This is an awful hack.  Values (actual values) can be lists, which
    // are displayed (by DaemonStatus) by concatenating their elements.
    // Performing the concatenation in the comparator (n log n times) is
    // too expensive, but allowing the list to get to the underlying
    // comparator can cause a ClassCastException.  As an expedient, use the
    // first element of the list as the sort value.
    Object getSortValue(Object value) {
      Object actual = getActualValue(value);
      if (actual instanceof List) {
	List lst = (List)actual;
	return lst.isEmpty() ? null : lst.get(0);
      }
      return actual;
    }
  }

  /**
   * Encapsulation of the info needed to sort on a single field
   */
  public static class SortRule {
    String columnName;
    boolean sortAscending;
    Comparator comparator = null;
    int columnType = -1;

    public SortRule(String columnName, boolean sortAscending) {
      this.columnName = columnName;
      this.sortAscending = sortAscending;
    }

    public SortRule(String columnName, boolean sortAscending, int columnType) {
      this(columnName, sortAscending);
      this.columnType = columnType;
    }

    public SortRule(String columnName, Comparator comparator) {
      this(columnName, comparator, true);
    }

    public SortRule(String columnName, Comparator comparator,
		    boolean sortAscending) {
      this.columnName = columnName;
      this.comparator = comparator;
      this.sortAscending = sortAscending;
    }

    /**
     * @return name of the field to sort on
     */
    public String getColumnName(){
      return columnName;
    }

    /**
     * @return the value type for the column
     */
    public int getColumnType(){
      return columnType;
    }

    /**
     * @return true if this column should be sorted in ascending order,
     * false if it should be sorted in descending order
     */
    public boolean sortAscending(){
      return sortAscending;
    }

    /**
     * @return the comparator, or null if no explicit comparator supplied
     */
    public Comparator getComparator(){
      return comparator;
    }

    /**
     * Lookup the column type in the columnDescriptors, store in self
     * @param columnDescriptorMap columnDescriptors
     */
    void inferColumnType(Map columnDescriptorMap){
      ColumnDescriptor col =
	(ColumnDescriptor)columnDescriptorMap.get(columnName);
      if (col != null) {
	columnType = col.getType();
	if (comparator == null) {
	  comparator = col.getComparator();
	}
	return;
      }
      // XXX this isn't really an error, just somebody sorting on a
      // column that isn't displayed.
//       logger.warning("Unknown type for sort column: "+ columnName);
      columnType = ColumnDescriptor.TYPE_INT;
    }

    public int compare(Object valA, Object valB) {
      int returnVal = 0;
      if (comparator != null) {
	returnVal = comparator.compare(valA, valB);
      } else {
	switch (getColumnType()) {
	case ColumnDescriptor.TYPE_IP_ADDRESS:
	  if (valA != null && ! (valA instanceof IPAddr))
	    logger.error("StatusTable.compare: valA not IPAddr but " +
			 valA.getClass().toString());
	  if (valB != null && ! (valB instanceof IPAddr))
	    logger.error("StatusTable.compare: valb not IPAddr but " +
			 valB.getClass().toString());
	  returnVal = compareIPAddrs((IPAddr)valA, (IPAddr)valB);
	  break;
	case ColumnDescriptor.TYPE_INT:
	case ColumnDescriptor.TYPE_FLOAT:
	case ColumnDescriptor.TYPE_PERCENT:
	case ColumnDescriptor.TYPE_TIME_INTERVAL:
	case ColumnDescriptor.TYPE_DATE:
	  returnVal = compareHandlingNulls((Comparable)valA, (Comparable)valB);
	  break;
	case ColumnDescriptor.TYPE_STRING:
	  // These warning are spruious; any object with a toString() can
	  // be used as TYPE_STRING
// 	  if ( !(valA instanceof String) && valA != null && logger.isDebug2())
// 	    logger.debug2("StatusTable.compare: valA not String but " +
// 			  valA.getClass().toString());
// 	  if ( !(valB instanceof String) && valB != null && logger.isDebug2())
// 	    logger.debug2("StatusTable.compare: valB not String but " +
// 			  valB.getClass().toString());
	  returnVal = compareHandlingNulls((Comparable)valA, (Comparable)valB);
	  break;
	default: //if we don't know the type, assume comparable
	  logger.warning("StatusTable.compare " + getColumnType() + " unknown");
	  returnVal = compareHandlingNulls((Comparable)valA, (Comparable)valB);
	  break;
	}
      }
      return sortAscending ? returnVal : -returnVal;
    }

    private static int compareIPAddrs(IPAddr addr1, IPAddr addr2) {
      return (addr1.getHostAddress().compareTo(addr2.getHostAddress()));
    }

    static int compareHandlingNulls(Comparable val1,
					    Comparable val2) {
      int returnVal = 0;
      if (isNull(val1)) {
	returnVal = isNull(val2) ? 0 : -1;
      } else if (isNull(val2)) {
	returnVal = 1;
      } else {
	returnVal = val1.compareTo(val2);
      }
      return returnVal;
    }

    static boolean isNull(Object obj) {
      return obj == null || obj == NO_VALUE;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("[SortRule: ");
      sb.append(columnName);
      sb.append(sortAscending ? ":A" : "D:");
      if (comparator != null) {
	sb.append(":");
	sb.append(comparator.toString());
      }
      sb.append("]");
      return sb.toString();
    }
  }
}
