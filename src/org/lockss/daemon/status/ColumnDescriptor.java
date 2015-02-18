/*
 * $Id$
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
 * Encapsulation of the info needed to describe a single column (name,
 * display title, and type).
 *
 * Type information is used for formatting the values in that column.  Specific
 * requirements for each type are listed below.
 */
public class ColumnDescriptor {
  /**
   * Must have meaningful toString() method
   */
  public static final int TYPE_INT=0;

  /**
   * Instanceof floating point number (Float, Double, etc.)
   */
  public static final int TYPE_FLOAT=1;

  /**
   * Instanceof floating point number (Float, Double, etc.) and floatValue()
   * must return between 0 and 1, inclusive
   */
  public static final int TYPE_PERCENT=2;

  /**
   * Instanceof number (Integer, Long, Float, etc.)
   */
  public static final int TYPE_TIME_INTERVAL=3;

  /**
   * Objects of this type must have meaningful toString() method
   */
  public static final int TYPE_STRING=4;

  /**
   * Instanceof IPAddr
   */
  public static final int TYPE_IP_ADDRESS=5;

  /**
   * Instanceof number (Integer, Long, Float, etc.)
   */
  public static final int TYPE_DATE=6;

  /**
   * Like TYPE_PERCENT but formatted as 
   * Instanceof floating point number (Float, Double, etc.) and floatValue()
   * must return between 0 and 1, inclusive
   */
  public static final int TYPE_AGREEMENT=7;


  private String columnName;
  private String title;
  private int type;
  private String footNote;
  protected boolean sortable = true;
  protected Comparator comparator = null;

  public ColumnDescriptor(String columnName, String title, int type) {
    this.columnName = columnName;
    this.title = title;
    this.type = type;
  }

  public ColumnDescriptor(String columnName, String title,
			  int type, String footNote) {
    this(columnName,title, type);
    this.footNote = footNote;
  }

  public String getColumnName() {
    return columnName;
  }

  public String getTitle() {
    return title;
  }

  public int getType() {
    return type;
  }

  public String getFootnote() {
    return footNote;
  }

  public Comparator getComparator() {
    return comparator;
  }

  public ColumnDescriptor setComparator(Comparator comparator) {
    this.comparator = comparator;
    return this;
  }

  public boolean isSortable() {
    return sortable;
  }

  public ColumnDescriptor setSortable(boolean sortable) {
    this.sortable = sortable;
    return this;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("[ColumnDescriptor:");
    sb.append(columnName);
    sb.append(", ");
    sb.append(title);
    sb.append(", ");
    sb.append(type);
    sb.append("]");
    return sb.toString();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof ColumnDescriptor)) {
      return false;
    }
    ColumnDescriptor colDesc = (ColumnDescriptor) obj;
    return (title.equals(colDesc.getTitle()) &&
	    type == colDesc.getType() &&
	    columnName.equals(colDesc.getColumnName()));
  }

  public int hashCode() {
    return title.hashCode() * 3
      + columnName.hashCode() * 5
      + type * 7;
  }

}

