/*
 * $Id: StatusServiceImpl.java,v 1.5 2003-03-14 00:28:01 troberts Exp $
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
import org.lockss.app.*;
import org.lockss.util.*;

/**
 * Main implementation of {@link StatusService}
 */
public class StatusServiceImpl 
  extends BaseLockssManager implements StatusService {
  private static Logger logger = Logger.getLogger("StatusServiceImpl");
  private Map statusAccessors;


  public StatusServiceImpl() {
    statusAccessors = new Hashtable();
    registerStatusAccessor(ALL_TABLES_TABLE, new AllTableStatusAccessor());
  }

  public StatusTable getTable(String tableName, Object key) 
      throws StatusService.Error {
    if (tableName == null) {
      throw new StatusService.Error("Called with null tableName");
    }
    StatusAccessor statusAccessor = 
      (StatusAccessor)statusAccessors.get(tableName);

    if (statusAccessor == null) {
      throw new StatusService.Error("Table not found: "+tableName+" "+key);
    } 
    StatusTable table = 
      new StatusTable(tableName, key, 
		      statusAccessor.getColumnDescriptors(key),
		      statusAccessor.getDefaultSortRules(key),
		      statusAccessor.getRows(key));
    return table;
  }

  public synchronized void 
    registerStatusAccessor(String tableName, StatusAccessor statusAccessor){

    if (statusAccessors.get(tableName) != null) {
      throw new StatusService.RuntimeError("Called multiple times for "
					   +tableName);
    }
    statusAccessors.put(tableName, statusAccessor);
    logger.info("Registered statusAccessor for table "+tableName);
  }

  public synchronized void unregisterStatusAccessor(String tableName){
    statusAccessors.remove(tableName);
    logger.info("Unregistered statusAccessor for table "+tableName);
  }

  private synchronized List getAllTableNames() {
    /**
     * keySet() calls a synchronized method, so it is thread safe, but the set
     * returned is backed by the Hashtable, which mean that changes to that are
     * reflected in the set.  We want a snap shot, so I synched this
     */
    Iterator it = statusAccessors.keySet().iterator();

    List tables = new ArrayList();
    while (it.hasNext()) {
      String tableName = (String) it.next();
      StatusAccessor statusAccessor = 
	(StatusAccessor)statusAccessors.get(tableName);
      if (!statusAccessor.requiresKey()) {
	tables.add(tableName);
      }
    }
    return tables;
  }

  private class AllTableStatusAccessor implements StatusAccessor {
    private List columns;
    private List sortRules;
    private static final String COL_NAME = "table_name";
    private static final String COL_TITLE = "Table name";

    public AllTableStatusAccessor() {
      StatusTable.ColumnDescriptor col = 
	new StatusTable.ColumnDescriptor(COL_NAME, COL_TITLE, 
					 StatusTable.TYPE_STRING);
      columns = ListUtil.list(col);

      StatusTable.SortRule sortRule = 
	new StatusTable.SortRule(COL_NAME, true);

      sortRules = ListUtil.list(sortRule);
    }

    public List getColumnDescriptors(Object key) throws StatusService.Error {
      return columns;
    }

    public List getRows(Object key) throws StatusService.Error {
      List tableNames =  getAllTableNames();
      Iterator it = tableNames.iterator();
      List rows = new ArrayList(tableNames.size());
      while (it.hasNext()) {
	String tableName = (String) it.next();
	Map row = new HashMap(1); //will only have the one key-value pair
	row.put(COL_NAME, 
		new StatusTable.Reference(tableName, tableName, null));
	rows.add(row);
      }
      return rows;
    }

    public List getDefaultSortRules(Object key) throws StatusService.Error {
      return sortRules;
    }

    public boolean requiresKey() {
      return false;
    }
  }
}
