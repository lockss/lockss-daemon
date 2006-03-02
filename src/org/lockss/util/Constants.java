/*
 * $Id: Constants.java,v 1.14 2006-03-02 19:45:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

  /** List delimiter in strings */
  public static String LIST_DELIM = ";";
  /** List delimiter char in strings */
  public static char LIST_DELIM_CHAR = ';';

  /** The default timezone, GMT */
  public static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("GMT");

  /** The line separator string on this system */
  public static String EOL = System.getProperty("line.separator");

  /** The RE string matching the EOL string */
  public static String EOL_RE = StringUtil.escapeNonAlphaNum(EOL);

  /** The default encoding used when none is detected */
  public static String DEFAULT_ENCODING = "ISO-8859-1";

  /**
   * <p>The US ASCII encoding.</p>
   */
  public static final String US_ASCII_ENCODING = "US-ASCII";

  /**
   * <p>The encoding of URLs.</p>
   */
  public static final String URL_ENCODING = US_ASCII_ENCODING;

  /** LOCKSS home page */
  public static String LOCKSS_HOME_URL = "http://www.lockss.org/";

  /** LOCKSS HTTP header, can have multiple values */
  public static String X_LOCKSS = "X-Lockss";

  /** X-LOCKSS value indicating this is a repair request */
  public static String X_LOCKSS_REPAIR = "Repair";

  /** X-LOCKSS value indicating this response comes from the cache */
  public static String X_LOCKSS_FROM_CACHE = "from-cache";

  /** The real identity of a repairer sending a request to localhost, for
   * testing */
  public static String X_LOCKSS_REAL_ID = "X-Lockss-Id";

  // Exit codes

  /** Exit code - normal exit */
  public static int EXIT_CODE_NORMAL = 0;

  /** Exit code - thread hung */
  public static int EXIT_CODE_THREAD_HUNG = 101;

  /** Exit code - thread died */
  public static int EXIT_CODE_THREAD_EXIT = 102;

  /** Exit code - unsupported Java version */
  public static int EXIT_CODE_JAVA_VERSION = 103;

  /** Exit code - exception thrown in main loop */
  public static int EXIT_CODE_EXCEPTION_IN_MAIN = 104;

  /** Exit code - required resource unavailable */
  public static int EXIT_CODE_RESOURCE_UNAVAILABLE = 105;

}
