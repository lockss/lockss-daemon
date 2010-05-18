/*
 * $Id: StatusServiceImpl.java,v 1.33 2010-05-18 06:15:57 tlipkis Exp $
 */

/*

Copyright (c) 2000-2005 Board of Trustees of Leland Stanford Jr. University,
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

import org.apache.oro.text.regex.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.config.*;

/**
 * Main implementation of {@link StatusService}
 */
public class StatusServiceImpl
  extends BaseLockssManager implements StatusService, ConfigurableManager {

  /**
   * Name of default daemon status table
   */
  public static final String PARAM_DEFAULT_TABLE =
    Configuration.PREFIX + "status.defaultTable";
  public static final String DEFAULT_DEFAULT_TABLE =
    OverviewStatus.OVERVIEW_STATUS_TABLE;

  private static Logger logger = Logger.getLogger("StatusServiceImpl");

  private String paramDefaultTable = DEFAULT_DEFAULT_TABLE;

  private Map statusAccessors = new HashMap();
  private Map overviewAccessors = new HashMap();
  private Map objRefAccessors = new HashMap();

  public void startService() {
    super.startService();
    registerStatusAccessor(ALL_TABLES_TABLE, new AllTableStatusAccessor());
  }

  public void setConfig(Configuration config, Configuration oldConfig,
			Configuration.Differences changedKeys) {
    paramDefaultTable = config.get(PARAM_DEFAULT_TABLE, DEFAULT_DEFAULT_TABLE);
  }

  public String getDefaultTableName() {
    return paramDefaultTable;
  }

  public StatusTable getTable(String tableName, String key)
      throws StatusService.NoSuchTableException {
    return getTable(tableName, key, null);
  }

  public StatusTable getTable(String tableName, String key, BitSet options)
      throws StatusService.NoSuchTableException {
    if (tableName == null) {
      throw new
	StatusService.NoSuchTableException("Called with null tableName");
    }

    StatusTable table = new StatusTable(tableName, key);
    if (options != null) {
      table.setOptions(options);
    }
    fillInTable(table);
    return table;
  }

  public void fillInTable(StatusTable table)
      throws StatusService.NoSuchTableException {
    StatusAccessor statusAccessor;
    String tableName = table.getName();
    String key = table.getKey();
    synchronized(statusAccessors) {
      statusAccessor = (StatusAccessor)statusAccessors.get(tableName);
    }
    if (statusAccessor == null) {
      throw new StatusService.NoSuchTableException("Table not found: "
						   +tableName+" "+key);
    }
    if (statusAccessor.requiresKey() && table.getKey() == null) {
      throw new StatusService.NoSuchTableException(tableName +
						   " requires a key value");
    }
    statusAccessor.populateTable(table);
    if (table.getTitle() == null) {
      try {
	table.setTitle(statusAccessor.getDisplayName());
      } catch (Exception e) {
	// ignored
      }
    }
  }

  static Pattern badTablePat =
    RegexpUtil.uncheckedCompile("[^a-zA-Z0-9_-]",
				Perl5Compiler.READ_ONLY_MASK);

  private boolean isBadTableName(String tableName) {
    return RegexpUtil.getMatcher().contains(tableName, badTablePat);
  }

  public void registerStatusAccessor(String tableName,
				     StatusAccessor statusAccessor) {
    if (isBadTableName(tableName)) {
      throw new InvalidTableNameException("Invalid table name: "+tableName);
    }

    synchronized(statusAccessors) {
      Object oldAccessor = statusAccessors.get(tableName);
      if (oldAccessor != null) {
	throw new
	  StatusService.MultipleRegistrationException(oldAccessor
						      +" already registered "
						      +"for "+tableName);
      }
      statusAccessors.put(tableName, statusAccessor);
    }
    logger.debug2("Registered statusAccessor for table "+tableName);
  }

  public void unregisterStatusAccessor(String tableName){
    synchronized(statusAccessors) {
      statusAccessors.remove(tableName);
    }
    logger.debug2("Unregistered statusAccessor for table "+tableName);
  }

  public void registerOverviewAccessor(String tableName,
				       OverviewAccessor acc) {
    if (isBadTableName(tableName)) {
      throw new InvalidTableNameException("Invalid table name: "+tableName);
    }

    synchronized(overviewAccessors) {
      Object oldAccessor = overviewAccessors.get(tableName);
      if (oldAccessor != null) {
	throw new
	  StatusService.MultipleRegistrationException(oldAccessor
						      +" already registered "
						      +"for "+tableName);
      }
      overviewAccessors.put(tableName, acc);
    }
    logger.debug2("Registered overview accessor for table "+tableName);
  }

  public void unregisterOverviewAccessor(String tableName){
    synchronized(overviewAccessors) {
      overviewAccessors.remove(tableName);
    }
    logger.debug2("Unregistered overviewAccessor for table "+tableName);
  }

  static final BitSet EMPTY_BITSET = new BitSet();

  public Object getOverview(String tableName) {
    return getOverview(tableName, null);
  }

  public Object getOverview(String tableName, BitSet options) {
    OverviewAccessor acc;
    synchronized (overviewAccessors) {
      acc = (OverviewAccessor)overviewAccessors.get(tableName);
    }
    if (acc != null) {
      return acc.getOverview(tableName,
			     (options == null) ? EMPTY_BITSET : options);
    } else {
      return null;
    }
  }

  public StatusTable.Reference getReference(String tableName, Object obj) {
    ObjRefAccessorSpec spec;
    synchronized (objRefAccessors) {
      spec = (ObjRefAccessorSpec)objRefAccessors.get(tableName);
    }
    if (spec != null && spec.cls.isInstance(obj)) {
      return spec.accessor.getReference(tableName, obj);
    } else {
      return null;
    }
  }

  // not implemented yet.
  public List getReferences(Object obj) {
    return Collections.EMPTY_LIST;
  }

  public void
    registerObjectReferenceAccessor(String tableName, Class cls,
				    ObjectReferenceAccessor objRefAccessor) {
    synchronized (objRefAccessors) {
      Object oldEntry = objRefAccessors.get(tableName);
      if (oldEntry != null) {
	ObjRefAccessorSpec oldSpec = (ObjRefAccessorSpec)oldEntry;
	throw new
	  StatusService.MultipleRegistrationException(oldSpec.accessor
						      +" already registered "
						      +"for "+tableName);
      }
      ObjRefAccessorSpec spec = new ObjRefAccessorSpec(cls, tableName,
						       objRefAccessor);
      objRefAccessors.put(tableName, spec);
    }
    logger.debug2("Registered ObjectReferenceAccessor for table "+tableName +
		  ", class " + cls);
  }

  public void
    unregisterObjectReferenceAccessor(String tableName, Class cls) {
    synchronized (objRefAccessors) {
      objRefAccessors.remove(tableName);
    }
    logger.debug2("Unregistered ObjectReferenceAccessor for table "+tableName);
  }

  private static class ObjRefAccessorSpec {
    Class cls;
    String table;
    ObjectReferenceAccessor accessor;

    ObjRefAccessorSpec(Class cls, String table,
		       ObjectReferenceAccessor accessor) {
      this.cls = cls;
      this.table = table;
      this.accessor = accessor;
    }
  }

  private class AllTableStatusAccessor implements StatusAccessor {
    private List columns;
    private List sortRules;
    private static final String COL_NAME = "table_name";
    private static final String COL_TITLE = "Available Tables";
    private static final String ALL_TABLE_TITLE = "Box Overview";

    public AllTableStatusAccessor() {
      ColumnDescriptor col =
	new ColumnDescriptor(COL_NAME, COL_TITLE,
			     ColumnDescriptor.TYPE_STRING);
      columns = ListUtil.list(col);

      StatusTable.SortRule sortRule =
	new StatusTable.SortRule(COL_NAME, true);

      sortRules = ListUtil.list(sortRule);
    }

    public String getDisplayName() {
      return ALL_TABLE_TITLE;
    }

    private List getRows(boolean isDebugUser) {
      synchronized(statusAccessors) {
	Set tables = statusAccessors.keySet();
	Iterator it = tables.iterator();
	List rows = new ArrayList(tables.size());
	while (it.hasNext()) {
	  String tableName = (String) it.next();
	  StatusAccessor statusAccessor =
	    (StatusAccessor)statusAccessors.get(tableName);
	  if (!ALL_TABLES_TABLE.equals(tableName) &&
	      !statusAccessor.requiresKey() &&
	      (isDebugUser ||
	       !(statusAccessor instanceof StatusAccessor.DebugOnly))) {
	    Map row = new HashMap(1); //will only have the one key-value pair
	    String title = null;
 	    try {
	      title = statusAccessor.getDisplayName();
 	    } catch (Exception e) {
 	      // no action, title is null here
 	    }
 	    // getTitle might return null or throw
	    if (title == null) {
	      title = tableName;
	    }
	    row.put(COL_NAME,
		    new StatusTable.Reference(title, tableName, null));
	    rows.add(row);
	  }
	}
	return rows;
      }
    }

    /**
     * Returns false
     * @return false
     */
    public boolean requiresKey() {
      return false;
    }

    /**
     * Populate the {@link StatusTable} with entries for each table that
     * doesn't require a key
     * @param table {@link StatusTable} to populate as the table of all tables
     * that don't require a key
     */
    public void populateTable(StatusTable table) {
      table.setColumnDescriptors(columns);
      table.setDefaultSortRules(sortRules);
      table.setRows(getRows(table.getOptions().get(StatusTable.OPTION_DEBUG_USER)));
    }

  }
}
