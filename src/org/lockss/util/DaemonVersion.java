/*
 * $Id: DaemonVersion.java,v 1.3 2004-06-15 21:40:38 smorabito Exp $
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

import java.util.StringTokenizer;

/**
 * Representation of a daemon version.  Currently this is a string
 * with three integer parts separated by a period ("."), i.e. "1.1.3".
 */
public class DaemonVersion implements Version {

  private int m_versionMajor;
  private int m_versionMinor;
  private int m_versionBuild;

  /**
   * Construct a Daemon Version from a string.
   *
   * Valid formats are three period (.) separated tokens that include the
   * characters a-z, A-Z, 0-9, and a dash followed by any string.  Characters
   * following the dash in each token are ignored.  Tokens must be three
   * characters or less, not including dashes and characters following the
   * dash.  For example:
   *
   *   1.2.3
   *   1.2.3-testing
   *   1a.2b.3c
   *   1.2.3ab
   *
   * Illegal formats:
   *   1.2323.3b
   *   1.0
   *   1.0.0.0
   */
  public DaemonVersion(String ver) {
    StringTokenizer st = new StringTokenizer(ver, ".");

    if (st.countTokens() != 3) {
      throw new IllegalArgumentException("Illegal format for Daemon Version: "
	+ ver);
    }

    try {
      if (st.hasMoreTokens()) {
	m_versionMajor = parseToken(st.nextToken());
      }
      if (st.hasMoreTokens()) {
	m_versionMinor = parseToken(st.nextToken());
      }
      if (st.hasMoreTokens()) {
	m_versionBuild = parseToken(st.nextToken());
      }
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(ex.toString());
    }

  }

  public long toLong() {
    long base = 36 * 36 * 36;
    long num = m_versionMajor;
    num = (num * base) + m_versionMinor;
    num = (num * base) + m_versionBuild;
    return num;
  }

  private static int parseToken(String token) {
    int dash = token.indexOf('-');

    if (dash > -1) {
      String intValue = token.substring(0, dash);
      if (intValue.length() > 3) {
	throw new IllegalArgumentException("Token is too long: " + intValue);
      }
      return Integer.parseInt(intValue, 36);
    } else {
      if (token.length() > 3) {
	throw new IllegalArgumentException("Token is too long: " + token);
      }
      return Integer.parseInt(token, 36);
    }
  }
}
