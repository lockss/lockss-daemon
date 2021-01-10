/*
 * $Id$
 */

/*

Copyright (c) 2021 Board of Trustees of Leland Stanford Jr. University,
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
import java.io.*;
import java.util.regex.*;

import org.lockss.util.*;

/** Base class for StatusAccessors that displays the keys and values in a
 * Java Properties file (or one conforming to that syntax).  Subclasses
 * must supply a getDisplayName() method and a constructor that calls this
 * class's constructor with the File to load.  The keys are initially
 * displayed in the order they occur in the file.  To have, double-quotes
 * surrounding values removed; call {@link #setStripQuotes(boolean)} with
 * true..
 */
public abstract class PropFileStatusAccessor implements StatusAccessor {

  private static final List PROP_FILE_COL_DESCS =
    ListUtil.list(new ColumnDescriptor("name", "Name",
                                       ColumnDescriptor.TYPE_STRING),
                  new ColumnDescriptor("value", "Value",
                                       ColumnDescriptor.TYPE_STRING)
                  );

  private static final List PROP_FILE_SORT_RULES =
    ListUtil.list(new StatusTable.SortRule("sort", true));

  protected static final Pattern STRIP_QUOTES_PAT =
    Pattern.compile("\"(.*)\"");

  private File propFile;
  private boolean stripQuotes = false;

  public PropFileStatusAccessor(File propFile) {
    this.propFile = propFile;
  }

  public boolean requiresKey() {
    return false;
  }

  /** Return true if the specified property file is readable */
  public boolean canRead() {
    return propFile.canRead();
  }

  protected void setStripQuotes(boolean val) {
    stripQuotes = val;
  }

  public void populateTable(StatusTable table)
      throws StatusService.NoSuchTableException {
    table.setColumnDescriptors(PROP_FILE_COL_DESCS);
    table.setDefaultSortRules(PROP_FILE_SORT_RULES);
    table.setRows(getRows(table.getOptions()));
  }

  public List getRows(BitSet options)
      throws StatusService.NoSuchTableException {
    List rows = new ArrayList();

    try {
      try (InputStream ins = new FileInputStream(propFile)) {
        Properties props = new OrderedProperties();
        props.load(ins);
        int ix = 0;
        for (Enumeration en = props.keys(); en.hasMoreElements(); ) {
          String key = (String)en.nextElement();
          Map row = new HashMap();
          row.put("name", key);
          String val = props.getProperty(key);
          if (stripQuotes) {
            Matcher mat = STRIP_QUOTES_PAT.matcher(val);
            if (mat.matches()) {
              val = mat.group(1);
            }
          }
          row.put("value", val);
          row.put("sort", ix++);
          rows.add(row);
        }
      }
    } catch (IOException e) {
      throw new StatusService.NoSuchTableException("Can't read " + propFile,
                                                   e);
    }
    return rows;
  }
}

