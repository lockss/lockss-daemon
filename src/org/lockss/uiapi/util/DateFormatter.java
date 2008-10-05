/*
 * $Id: DateFormatter.java,v 1.3 2005-10-11 05:47:56 tlipkis Exp $
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

package org.lockss.uiapi.util;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Common date related definitions
 */
 public class DateFormatter {

  /*
   * Common date formats
   */
  public static final DateFormat Short =
                      new SimpleDateFormat("MM/dd/yy HH:mm:ss");

  public static final DateFormat Time =
                      new SimpleDateFormat("HH:mm:ss");

  public static final DateFormat Explicit =
                      new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

  /**
   * Return current date/time
   * @return <code>dd-MM-yyyy HH:mm:ss</code>
   */
  public static String now() {
    return Explicit.format(new Date());
  }

  /**
   * Return current time
   * @return <code>HH:mm:ss</code>
   */
  public static String currentTime() {
    return Time.format(new Date());
  }

  /**
   * Format a "short" date
   * @param date Date object
   * @return <code>MM/dd/yy HH:mm:ss</code>
   */
  public static String shortFormat(Date date) {
    return Short.format(date);
  }

  /**
   * Format a "long" date
   * @param date Date object
   * @return <code>dd-MM-yyyy HH:mm:ss</code>
   */
  public static String explicitFormat(Date date) {
    return Explicit.format(date);
  }
}

