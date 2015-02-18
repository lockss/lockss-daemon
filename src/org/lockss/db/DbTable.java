/*
 * $Id$
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Representation of a database table.
 * 
 * @author Fernando Garcia-Loygorri
 */
package org.lockss.db;

public class DbTable {

  // The name of the database table.
  private String name;

  // The SQL query used to create the table in the database.
  private String createQuery;

  // An indication of whether identical data rows may appear in the table.
  private boolean repeatedRowsAllowed = false;

  // The representation of a row in the database table.
  private DbRow row;

  /**
   * Constructor.
   * 
   * @param name A String with the name of the database table.
   */
  public DbTable(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getCreateQuery() {
    return createQuery;
  }

  public void setCreateQuery(String createQuery) {
    this.createQuery = createQuery;
  }

  public boolean isRepeatedRowsAllowed() {
    return repeatedRowsAllowed;
  }

  public void setRepeatedRowsAllowed(boolean repeatedRowsAllowed) {
    this.repeatedRowsAllowed = repeatedRowsAllowed;
  }

  public DbRow getRow() {
    return row;
  }

  public void setRow(DbRow row) {
    this.row = row;
  }
}
