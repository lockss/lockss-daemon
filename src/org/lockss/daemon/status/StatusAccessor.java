/*
 * $Id: StatusAccessor.java,v 1.1 2003-03-13 00:22:05 troberts Exp $
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

public interface StatusAccessor {
  /**
   * Get the description (name, title and type) for the fields this 
   * StatusAccessor will return, for the given key
   * @param key object (such as AU) designating which table to return 
   * @return List of ColumnDescriptor objects for the fields this 
   * StatusAccessor supplies
   * @throws StatusService.Error if we get a key that we don't recognize or 
   * have a table for
   */
  public List getColumnDescriptors(Object key) throws StatusService.Error;

  /**
   * Gets the status rows for a specified key
   * @param key Object which designates a set of status rows to return
   * @return List of status rows (Maps) for the specified key
   * @throws StatusService.Error if we get a key that we don't recognize or 
   * have a table for
   */
  public List getRows(Object key) throws StatusService.Error;

  /**
   * Gives list of the default sort rules for this status info for a given key
   * @param key key identifying the table for which to get the sort rules
   * @returns list of the default sort rules for this status info
   * @throws StatusService.Error if we get a key that we don't recognize or 
   * have a table for
   */
  public List getDefaultSortRules(Object key) throws StatusService.Error;
}
