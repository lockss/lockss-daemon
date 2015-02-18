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

import java.io.*;


/** A half-open interval, including the lower bound but excluding the upper
 * bound.  (If other varieties are needed, this could either be made
 * abtsract or given a mode selector.) */
public class Interval {
  private Comparable lb;		// lower bound
  private Comparable ub;		// upper bound

  /** Create an interval between the lower bound (inclusive) and the upper
   * bound (exclusive).  The only type restriction on the bounds is that
   * they be comparable with each other.
   * @param lb the lower bound
   * @param ub the upper bound
   */
  public Interval(Comparable lb, Comparable ub) {
    this.lb = lb;
    this.ub = ub;
  }

  /** Convenience constructor to create an interval between two ints.
   * @param lb the lower bound
   * @param ub the upper bound
   */
  public Interval(int lb, int ub) {
    this.lb = new Integer(lb);
    this.ub = new Integer(ub);
  }

  /** Return the lower bound. */
  public Comparable getLB() {
    return lb;
  }

  /** Return the upper bound. */
  public Comparable getUB() {
    return ub;
  }

  public boolean equals(Object o) {
    if (o instanceof Interval) {
      Interval oi = (Interval)o;
      return lb.equals(oi.getLB()) && ub.equals(oi.getUB());
    }
    return false;
  }

  public int hashCode() {
    return 3*lb.hashCode() + ub.hashCode();
  }

  /** Determine whether intervals are ordered and non-overlapping.
   * @param other the other interval
   * @return true iff this interval ends before the other starts */
  public boolean isBefore(Interval other) {
    return ub.compareTo(other.getLB()) <= 0;
  }

  /** Determine whether intervals are disjoint.
   * @param other the other interval
   * @return true iff this interval does not overlap the other */
  public boolean isDisjoint(Interval other) {
    return other.getLB().compareTo(ub) >= 0 ||
      lb.compareTo(other.getUB()) >= 0;
  }

  /** Determine whether one interval subsumes the other.
   * @param other the other interval
   * @return true iff this interval completely includes the other */
  public boolean subsumes(Interval other) {
    return lb.compareTo(other.getLB()) <= 0 &&
      ub.compareTo(other.getUB()) >= 0;
  }

  /** Determine whether a point is contained in the interval.
   * @param point must be Comparable to the interval's upper and lower bound
   * @return true iff point is between the lower bound (inclusive) and
   * upper bound (exclusive) */
  public boolean contains(Comparable point) {
    return lb.compareTo(point) <= 0 && point.compareTo(ub) < 0;
  }

  public String toString() {
    return "[" + getLB() + "," + getUB() + ")";
  }

}
