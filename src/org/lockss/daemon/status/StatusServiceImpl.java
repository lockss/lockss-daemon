/*
 * $Id: StatusServiceImpl.java,v 1.21 2004-04-29 10:11:17 tlipkis Exp $
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
import org.lockss.app.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import gnu.regexp.*;

/**
 * Main implementation of {@link StatusService}
 */
public class StatusServiceImpl 
  extends BaseLockssManager implements StatusService {
  private static Logger logger = Logger.getLogger("StatusServiceImpl");
  private Map statusAccessors = new HashMap();
  private Map objRefAccessors = new HashMap();

  public void startService() {
    super.startService();
    registerStatusAccessor(ALL_TABLES_TABLE, new AllTableStatusAccessor());
  }

  protected void setConfig(Configuration config, Configuration prevConfig,
			   Set changedKeys) {
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

    StatusAccessor statusAccessor;
    synchronized(statusAccessors) {
      statusAccessor = (StatusAccessor)statusAccessors.get(tableName);
    }

    if (statusAccessor == null) {
      throw new StatusService.NoSuchTableException("Table not found: "
						   +tableName+" "+key);
    } 
    StatusTable table = new StatusTable(tableName, key);
    if (options != null) {
      BitSet tableOpts = table.getOptions();
      tableOpts.xor(tableOpts);
      tableOpts.or(options);
    }
    statusAccessor.populateTable(table);
    if (table.getTitle() == null) {
      try {
	table.setTitle(statusAccessor.getDisplayName());
      } catch (Exception e) {
	// ignored
      }
    }
    return table;
  }

  private boolean isBadTableName(String tableName) {
    try {
      RE re = new RE(".*[^a-zA-Z0-9_-].*");
      return re.isMatch(tableName);
    } catch (REException ree) {
      logger.error("Bad regular expression", ree);
      return false;
    }
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
    logger.debug("Registered statusAccessor for table "+tableName);
  }

  public void unregisterStatusAccessor(String tableName){
    synchronized(statusAccessors) {
      statusAccessors.remove(tableName);
    }
    logger.debug("Unregistered statusAccessor for table "+tableName);
  }

  public StatusTable.Reference getReference(String tableName, Object obj) {
    ObjRefAccessorSpec spec;
    synchronized (objRefAccessors) {
      spec = (ObjRefAccessorSpec)objRefAccessors.get(tableName);
    }
    if (spec != null && spec.cls.isInstance(obj)) {
      return spec.accessor.getReference(obj, tableName);
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
    logger.debug("Registered ObjectReferenceAccessor for table "+tableName +
		 ", class " + cls);
  }

  public void
    unregisterObjectReferenceAccessor(String tableName, Class cls) {
    synchronized (objRefAccessors) {
      objRefAccessors.remove(tableName);
    }
    logger.debug("Unregistered ObjectReferenceAccessor for table "+tableName);
  }

  private class ObjRefAccessorSpec {
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
    private static final String ALL_TABLE_TITLE = "Cache Overview";

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

    private List getRows() {
      synchronized(statusAccessors) {
	Set tables = statusAccessors.keySet();
	Iterator it = tables.iterator();
	List rows = new ArrayList(tables.size());
	while (it.hasNext()) {
	  String tableName = (String) it.next();
	  StatusAccessor statusAccessor = 
	    (StatusAccessor)statusAccessors.get(tableName);
	  if (!ALL_TABLES_TABLE.equals(tableName) &&
	      !statusAccessor.requiresKey()) {
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
      table.setRows(getRows());
    }

  }
}
