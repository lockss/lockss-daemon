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

package org.lockss.util;
import java.util.*;
import java.text.*;

/** OrderedObject imposes an independent order on an arbitrary value.
 * Useful for displayed values that want to be ordered independent of their
 * display string.
 */
public class OrderedObject implements Comparable {
  private Object value;
  private Comparable order;

  /** Create an OrderedObject with the specified value and order
   */
  public OrderedObject(Object value, Comparable order) {
    this.value = value;
    this.order = order;
  }

  /** Create an OrderedObject with the specified value and order
   */
  public OrderedObject(Object value, int order) {
    this(value, new Integer(order));
  }

  /** Create an OrderedObject with the same value and order
   */
  public OrderedObject(Comparable value) {
    this.value = value;
    this.order = value;
  }

  /** Returns the value */
  public Object getValue() {
    return value;
  }

  /** Returns the explicit order */
  public Comparable getOrder() {
    return order;
  }

  /** Returns getValue().toString() */
  public String toString() {
    return (value == null) ? "null" : value.toString();
  }

  // Comparable interface

  public int compareTo(Object o) {
    if (o instanceof OrderedObject) {
      return order.compareTo(((OrderedObject)o).getOrder());
    } else {
      return order.compareTo(((Comparable)o));
    }
  }

  /** Returns true if both objects are OrderedObjects and have equal values */
  public boolean equals(Object o) {
    if (o instanceof OrderedObject) {
      Object ov = ((OrderedObject)o).getValue();
      return (value == null) ? ov == null : value.equals(ov);
    } else {
      return false;
    }
  }

  /** Returns the hashCode of the value */
  public int hashCode() {
    return (value == null) ? 0 : value.hashCode();
  }

}
