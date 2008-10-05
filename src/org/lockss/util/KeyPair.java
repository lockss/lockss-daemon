/*
 * $Id: KeyPair.java,v 1.1 2004-10-18 03:23:08 tlipkis Exp $
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

/** Wrapper around a pair of objects, with appropriate equals() and
 * hashCode() for use as a combined key in maps, etc. */
public class KeyPair {
  public final Object car;
  public final Object cdr;

  public KeyPair(Object o1, Object o2) {
    super();
    this.car = o1;
    this.cdr = o2;
  }

  private static boolean equals(Object x, Object y) {
    return (x == null && y == null) || (x != null && x.equals(y));
  }

  public boolean equals(Object other) {
    return other instanceof KeyPair && equals(car, ((KeyPair) other).car) &&
      equals(cdr, ((KeyPair) other).cdr);
  }

  public int hashCode() {
    if (car == null) {
      if (cdr == null) return 643;
      return cdr.hashCode() + 1;
    }
    if (cdr == null) return car.hashCode() + 2;
    return car.hashCode() * cdr.hashCode();
  }
}
