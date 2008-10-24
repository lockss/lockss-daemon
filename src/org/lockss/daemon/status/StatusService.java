/*
 * $Id: StatusService.java,v 1.15 2008-10-24 07:11:19 tlipkis Exp $
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

/**
 * This object sits between the daemon and the UI code to function as a
 * centralized place to query for status information on the system.
 */
public interface StatusService {

  /**
   * Name of the table that contains references to all tables
   */
  public static final String ALL_TABLES_TABLE = "table_of_all_tables";

  /** Returns the name of the table to be displayed if no table is
   * specified.  Set to <code>table_of_all_tables</code> to display index
   * of tables */
  public String getDefaultTableName();

  /**
   * Returns the StatusService.Table object identified by the tableName
   * and key specified.
   * @param tableName name of the table to get
   * @param key object which further specifies the needed table
   * @return Populated StatusTable
   * @throws  StatusService.NoSuchTableException if there is no status table
   * with that name-key combination
   */
  public StatusTable getTable(String tableName, String key)
      throws StatusService.NoSuchTableException;

  /**
   * Returns the StatusService.Table object identified by the tableName,
   * key and options specified.
   * @param tableName name of the table to get
   * @param key object which further specifies the needed table
   * @param options BitSet of StatusTable.OPTION_<iXXX</i>
   * @return Populated StatusTable
   * @throws  StatusService.NoSuchTableException if there is no status table
   * with that name-key combination
   */
  public StatusTable getTable(String tableName, String key, BitSet options)
      throws StatusService.NoSuchTableException;

  /**
   * Call the StatusAccessor to fill in the table, using the already stored
   * name, key, options, etc. */
  public void fillInTable(StatusTable table)
      throws StatusService.NoSuchTableException;

  /**
   * Register a StatusAccessor that knows how to get a table for a certain name
   * @param tableName name of the table that statusAccessor can provide
   * @param statusAccessor StatusAccessor that can provide the specified table
   * @throws StatusService.MultipleRegistrationException if multiple
   * StatusAccessors are registered to the same tableName
   * @throws StatusService.InvalidTableNameException if you attempt to register
   * a StatusAccessor for a table name that has anything other than
   * <i>[a-zA-Z0-9]</i>, <i>-</i>, and <i>_</i>  in it
   */
  public void registerStatusAccessor(String tableName,
				     StatusAccessor statusAccessor);

  /**
   * Unregister a previously registered StatusAccessor
   * @param tableName name of the table to unregister
   */
  public void unregisterStatusAccessor(String tableName);


  /**
   * Register an OverviewAccessor that returns a single line overview of
   * the subsystem */
  public void registerOverviewAccessor(String tableName,
				      OverviewAccessor acc);

  /**
   * Unregister an OverviewAccessor */
  public void unregisterOverviewAccessor(String tableName);

  /**
   * Get the one-line overview of the subsystem associated with the table
   * name */
  public Object getOverview(String tableName);

  /**
   * Get the one-line overview of the subsystem associated with the table
   * name */
  public Object getOverview(String tableName, BitSet options);

  /**
   * Get a reference to the named table for the given object.
   * virtue of its class) and ask each one to create a reference to their
   * table for the object.
   * @param tableName the table for which a reference is desired
   * @param obj the object for which a reference is desired
   * @return a reference to the supplied object, or null if no method to
   * create one has been registered.
   */
  public StatusTable.Reference getReference(String tableName, Object obj);


  /**
   * Find all the ObjectReferenceAccessors applicable to the object (by
   * virtue of its class) and ask each one to create a reference to their
   * table for the object.
   * @param obj the object for which references are desired
   * @return list of {@link StatusTable.Reference}s to tables for the
   * supplied object.  Returns null or an empty list if there are none.
   */
  public List getReferences(Object obj);


  /**
   * Register an ObjectReferenceAccessor that knows how to create a {@link
   * StatusTable.Reference}s to one or more tables with a key appropriate to
   * select and/or filter on a supplied object.  Multiple accessors may be
   * registered for the same object class, but not for the same table.
   * @param tableName name of the table to which the references returned by
   * the accessor refer.
   * @param cls the class of objects for which the supplied
   * ObjectReferenceAccessor can create references.
   * @param objRefAccessor ObjectReferenceAccessor that creates references
   * to named table
   * @throws StatusService.MultipleRegistrationException if multiple
   * ObjectReferenceAccessor are registered for the same table
   * @throws StatusService.InvalidTableNameException if you attempt to register
   * a StatusAccessor for a table name that has anything other than
   * <i>[a-zA-Z0-9]</i>, <i>-</i>, and <i>_</i>  in it
   */
  public void
    registerObjectReferenceAccessor(String tableName, Class cls,
				    ObjectReferenceAccessor objRefAccessor);

  /**
   * Unregister a previously registered ObjectReferenceAccessor
   * @param tableName name of the table for which the previously supplied
   * @param cls the class of objects for which the previously supplied
   * ObjectReferenceAccessor is no longer willing to create references.
   * ObjectReferenceAccessor is no longer willing to create references.
   */
  public void unregisterObjectReferenceAccessor(String tableName, Class cls);


  /**
   * Thrown for various errors related to status queries
   */
  public class NoSuchTableException extends Exception {
    public NoSuchTableException(String msg) {
      super(msg);
    }
    public NoSuchTableException(String msg, Exception cause) {
      super(msg, cause);
    }
  }

  /**
   * Thrown if someone tries to register a status accessor for an
   * invalid table name
   */
  public class InvalidTableNameException extends RuntimeException {
    public InvalidTableNameException(String msg) {
      super(msg);
    }
  }

  /**
   * Thrown when multiple StatusAccessors are registered for the same
   * table name
   */
  public class MultipleRegistrationException extends RuntimeException {
    public MultipleRegistrationException(String msg) {
      super(msg);
    }
  }

}
