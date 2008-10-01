/*
 * $Id: CompoundLinearSlope.java,v 1.1.2.2 2008-10-01 23:36:14 tlipkis Exp $
 */

/*

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import org.apache.oro.text.regex.*;

/** A simple slope composed of linear segments. Should be genericized */

public class CompoundLinearSlope {

  static class Point {
    final long x;
    final double y;

    Point(long x, double y) {
      this.x = x;
      this.y = y;
    }

    public String toString() {
      return "(" + x + "," + y + ")";
    }
  }

  private Point points[];

  /** Create a CompoundLinearSlope from a string specifying a list of
   * pairs: [x1,y1},[x2,y2},...,[xN,yN} */
  public CompoundLinearSlope(String spec) {
    this(parseString(spec));
  }

  /** Create a CompoundLinearSlope from a List of Points */
  public CompoundLinearSlope(List<Point> pointList) {
    if (pointList == null || pointList.isEmpty()) {
      throw new IllegalArgumentException("Must supply non-empty pointList");
    }
    points = new Point[pointList.size() + 2];
    int ix = 0;
    points[ix++] = new Point(Long.MIN_VALUE, pointList.get(0).y);
    for (Point p : pointList) {
      if (points[ix - 1].x > p.x) {
	throw
	  new IllegalArgumentException("X coordinates must be non-decreasing");
      }
      points[ix++] = p;
    }
    points[ix] = new Point(Long.MAX_VALUE, points[ix - 2].y);
  }

  /** Return the Y value on the slope at a point on the X axis */
  public double getY(long x) {
    Point p2 = points[0];

    for (int ix = 0; ix < points.length - 1; ix++) {
      Point p1 = p2;
      p2 = points[ix + 1];
      if (x <= p2.x) {
	return interp(x, p1, p2);
      }
    }
    throw new RuntimeException("Impossible error in CompoundLinearSlope");
  }

  /** Interpolate Y value within single slope */
  private double interp(long x, Point p1, Point p2) {
    long deltax = p2.x - p1.x;
    double deltay = p2.y - p1.y;
    double fract = ((double)(x - p1.x)) / deltax;

    return deltay * fract + p1.y;
  }

  //* Pattern picks off first x,y pair into group(1) and group(2) and rest
  //* of string into group(3) */
  static Pattern ONE_POINT_PAT =
    RegexpUtil.uncheckedCompile("^\\s*,?\\s*\\[(\\w+)\\s*,\\s*([0-9.]+)\\](.*)$",
				Perl5Compiler.READ_ONLY_MASK);


  /** Parse a string of the form [x1,y1},[x2,y2},...,[xN,yN} into a list of
   * Points, where each xN is either an integer number of milliseconds or a
   * time interval string (see {@link StringUtil#parseTimeInterval()]) and
   * each yN is an integer */
  public static List<Point> parseString(String str)
      throws NumberFormatException {
    if (StringUtil.isNullString(str)) {
      throw new IllegalArgumentException("Must supply non-empty string");
    }
    ArrayList<Point> res = new ArrayList<Point>();
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    while (matcher.contains(str, ONE_POINT_PAT)) {
      MatchResult matchResult = matcher.getMatch();
      String xstr = matchResult.group(1);
      String ystr = matchResult.group(2);
      str = matchResult.group(3);
      res.add(new Point(StringUtil.parseTimeInterval(xstr),
			Double.valueOf(ystr)));
    }
    res.trimToSize();
    return res;
  }

  public String toString() {
    return "[CLS " + "[" + StringUtil.separatedString(points, ", ") + "]]";
  }
}
