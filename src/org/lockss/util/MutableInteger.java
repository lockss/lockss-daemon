/*
 * $Id: MutableInteger.java,v 1.1 2004-05-04 22:16:49 tlipkis Exp $
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

import java.io.*;


/** A MutableInteger wraps an int, allowing it to be modified by reference,
 * e.g., to use as an accumulator in a Map. */
public class MutableInteger implements Serializable {
  private int value;

  /** Create a MutableInteger with the value */
  public MutableInteger(int value) {
    this.value = value;
  }

  /** Create a MutableInteger with the value 0 */
  public MutableInteger() {
    this.value = 0;
  }

  /** Return the value as a int */
  public int intValue() {
    return value;
  }

  /** Set the value */
  public void setValue(int value) {
    this.value = value;
  }

  /** Set the value to the sum of n and the old value */
  public void add(int n) {
    value += n;
  }

  public String toString() {
    return Integer.toString(value);
  }
}
