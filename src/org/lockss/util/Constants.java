/*
 * $Id: Constants.java,v 1.7 2004-03-14 01:05:30 tlipkis Exp $
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

/**
 * Constants of general use
 */
public interface Constants {
  /** The number of milliseconds in a second */
  public static final long SECOND = 1000;
  /** The number of milliseconds in a minute */
  public static final long MINUTE = 60 * SECOND;
  /** The number of milliseconds in an hour */
  public static final long HOUR = 60 * MINUTE;
  /** The number of milliseconds in a day */
  public static final long DAY = 24 * HOUR;
  /** The number of milliseconds in a week */
  public static final long WEEK = 7 * DAY;

  /** The line separator string on this system */
  public static String EOL = System.getProperty("line.separator");

  /** The RE string matching the EOL string */
  public static String EOL_RE = StringUtil.escapeNonAlphaNum(EOL);

  /** The default encoding used when none is detected */
  public static String DEFAULT_ENCODING = "ISO-8859-1";

  /** LOCKSS home page */
  public static String LOCKSS_HOME_URL = "http://www.lockss.org/";

}
