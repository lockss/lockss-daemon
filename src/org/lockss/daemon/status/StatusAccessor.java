/*
 * $Id: StatusAccessor.java,v 1.6 2003-03-15 00:27:22 troberts Exp $
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
 * Objects wishing to provide status information to {@link StatusService} must
 * create an object which implements this.
 *
 * Used by {@link StatusService} to generate {@link StatusTable}s.  
 * All of the lists returned may be modified by {@link StatusService}, so 
 * they should not mirror any internal data structures.
 */

public interface StatusAccessor {
  /**
   * Get the list of {@link StatusTable.ColumnDescriptor}s for this key
   * @param key object (such as AUID) designating which table to return 
   * @return List of {@link StatusTable.ColumnDescriptor}s for the columns 
   * this StatusAccessor supplies.
   * @throws StatusService.NoSuchTableException if we get a key that we don't 
   * recognize or have a table for
   */
  public List getColumnDescriptors(String key) 
      throws StatusService.NoSuchTableException;

  /**
   * Gets the status rows for a specified key
   * @param key string which designates a set of status rows to return
   * @return List of rows (which are represented by Maps) for the specified key
   * @throws StatusService.NoSuchTableException if we get a key that we don't 
   * recognize or have a table for
   */
  public List getRows(String key) throws StatusService.NoSuchTableException;

  /**
   * Gives list of the default {@link StatusTable.SortRule}s for this status 
   * info for a given key
   * @param key key identifying the table for which to get the sort rules
   * @return list of {@link StatusTable.SortRule}s representing the default
   * sort rules
   * @throws StatusService.NoSuchTableException if we get a key that we don't 
   * recognize or have a table for
   */
  public List getDefaultSortRules(String key) 
      throws StatusService.NoSuchTableException;


  /**
   * Returns the title for the table specified by the key
   * @param key optional key to identify the desired table
   * @return String representation of the table title
   * @throws StatusService.NoSuchTableException if we get a key that we don't 
   * recognize or have a table for
   */
  public String getTitle(String key) 
      throws StatusService.NoSuchTableException;

  /**
   * @returns true if a key is required
   */
  public boolean requiresKey();

}
