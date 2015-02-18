/*
 * $Id$
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

/**
 * Objects wishing to provide status information to {@link StatusService}
 * must create an object which implements this.  StatusAccessor instances
 * may be invoked simultaneously in multiple threads, so they should not
 * maintain any local state (<i>ie</i>, no instance variables).
 */

public interface StatusAccessor {

  /**
   * Fills in the {@link ColumnDescriptor}s and rows for the given table (and
   * optionally the default {@link StatusTable.SortRule}s,
   * {@link StatusTable.SummaryInfo}, and Title.
   *
   * @param table @{link StatusTable} which specifies a name and optionally
   * a key.  This table will be populatred with {@link ColumnDescriptor} and
   * rows, as well as optionally the default {@link StatusTable.SortRule}s,
   * {@link StatusTable.SummaryInfo}, and Title.
   */
  public void populateTable(StatusTable table)
      throws StatusService.NoSuchTableException;

  /**
   * The name of the table in links, menus, etc.  The StatusAccessor may
   * set a different title for any particular table, using {@link
   * StatusTable#setTitle(String)}.  If it does not, this name will also be
   * used for the table title.  This method will only be called for tables
   * that are included in the table of all tables, <i>ie</i>, those that do
   * not require a key.
   * @return the name of the table
   */
  public String getDisplayName();

  /**
   * @return true if a key is required
   */
  public boolean requiresKey();

  /**
   * Marker interface for status accessors that should be included in the
   * table of all tables only for the debug user
   */
  public interface DebugOnly extends StatusAccessor {
  }
}
