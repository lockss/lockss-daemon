/*
 * $Id$
 */

/*

Copyright (c) 2000-2013 Board of Trustees of Leland Stanford Jr. University,
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

/** A simple integer step function. Should be genericized */

public class IntStepFunction {

  static class Point {
    final int x;
    final int y;

    Point(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public boolean equals(Object o) {
      if (!(o instanceof Point)) {
	return false;
      }
      Point op = (Point)o;
      return x == op.x && y == op.y;
    }

    public int hashCode() {
      return (int)((x ^ (x >>> 32)) + (y ^ (y >>> 32)));
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("(");
      if (x == Integer.MIN_VALUE) {
	sb.append("-INF");
      } else if (x == Integer.MAX_VALUE) {
	sb.append("+INF");
      } else {
	sb.append(Integer.toString(x));
      }
      sb.append(",");
      sb.append(y);
      sb.append(")");
      return sb.toString();
    }
  }

  private Point points[];

  /** Create a IntStepFunction from a string specifying a list of
   * pairs: [x1,y1},[x2,y2},...,[xN,yN} */
  public IntStepFunction(String spec) {
    this(parseString(spec));
  }

  /** Create a IntStepFunction from a List of Points */
  public IntStepFunction(List<Point> pointList) {
    if (pointList == null || pointList.isEmpty()) {
      throw new IllegalArgumentException("Must supply non-empty pointList");
    }
    points = new Point[pointList.size()];
    int ix = 0;
    for (Point p : pointList) {
      points[ix++] = p;
    }
  }

  /** Return the value of the function of X */
  public int getValue(int x) {
    if (x < points[0].x) {
      return 0;
    }
    Point p2 = points[0];
    for (int ix = 0; ix < points.length - 1; ix++) {
      Point p1 = p2;
      p2 = points[ix + 1];
      if (x < p2.x) {
	return p1.y;
      }
    }
    return p2.y;
  }

  //* Pattern picks off first x,y pair into group(1) and group(2) and rest
  //* of string into group(3) */
  static Pattern ONE_POINT_PAT =
    RegexpUtil.uncheckedCompile("^\\s*,?\\s*\\[(\\d+)\\s*,\\s*(-?\\d+)\\](.*)$",
				Perl5Compiler.READ_ONLY_MASK);


  /** Parse a string of the form [x1,y1},[x2,y2},...,[xN,yN} into a list of
   * Points ([int,int]) */
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
      try {
	int x = Integer.parseInt(xstr);
	int y = Integer.parseInt(ystr);
	res.add(new Point(x, y));
      } catch (NumberFormatException e) {
	throw new IllegalArgumentException("bad point [" + xstr + "," + ystr +
					   "] in " + str);
      }
    }
    res.trimToSize();
    return res;
  }

  public String toString() {
    return "[CLS " + "[" + StringUtil.separatedString(points, ", ") + "]]";
  }
}
