/*
 * $Id: MockStatusAccessor.java,v 1.4 2003-03-13 23:13:41 troberts Exp $
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

package org.lockss.test;

import java.util.*;
import org.lockss.daemon.status.*;

public class MockStatusAccessor implements StatusAccessor {
  private boolean requiresKey = false;
  private Map columnDescriptors;
  private Map rows;
  private Map defaultSortRules;


  public MockStatusAccessor() {
    columnDescriptors = new HashMap();
    rows = new HashMap();
    defaultSortRules = new HashMap();
  }

  public List getColumnDescriptors(Object key) throws StatusService.Error{
    return (List)columnDescriptors.get(key);
  }

  public void setColumnDescriptors(List columnDescriptors, Object key) {
    this.columnDescriptors.put(key, columnDescriptors);
  }

  public List getRows(Object key) throws StatusService.Error {
    List list = (List)rows.get(key);
    if (list == null) {
      throw new StatusService.Error("Bad key: "+key);
    }
    return list;
  }

  public void setRows(List rows, Object key) {
    this.rows.put(key, rows);
  }

  public List getDefaultSortRules(Object key) {
    List list = (List)defaultSortRules.get(key);
    return list;
  }

  public void setDefaultSortRules(List sortRules, Object key) {
    defaultSortRules.put(key, sortRules);
  }

  public void setRequiresKey(boolean requiresKey) {
    this.requiresKey = requiresKey;
  }

  public boolean requiresKey() {
    return requiresKey;
  }
}
