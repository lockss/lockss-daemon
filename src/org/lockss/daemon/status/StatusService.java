/*
 * $Id: StatusService.java,v 1.6 2003-03-15 02:32:11 troberts Exp $
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
 * This object sits between the daemon and the UI code to function as a 
 * centralized place to query for status information on the system.
 */
public interface StatusService {

  /**
   * Name of the table that contains references to all tables
   */
  public static final String ALL_TABLES_TABLE = "table_of_all_tables";

  /**
   * Returns the StatusService.Table object identified by the tableName 
   * and key specified.
   * @param tableName name of the table to get
   * @param key object which further specifies the needed table
   * @returns StatusTable object with specified name
   * @throws  StatusService.NoSuchTableException if there is no status table 
   * with that name-key combination
   */
  public StatusTable getTable(String tableName, String key) 
      throws StatusService.NoSuchTableException;

  /**
   * Register a StatusAccessor that knows how to get a table for a certain name
   * @param tableName name of the table that statusAccessor can provide
   * @param statusAccessor StatusAccessor that can provide the specified table
   * @throws StatusService.MultpleRegistrationException if multiple 
   * StatusAccessors are registered to the same tableName
   * @throws StatusService.InvalidTableNameException if you attemtp to register
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
   * Thrown for various errors related to status queries
   */  
  public class NoSuchTableException extends Exception {
    public NoSuchTableException(String msg) {
      super(msg);
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
