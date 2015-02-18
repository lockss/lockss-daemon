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

import org.apache.oro.text.regex.*;

/**
 * Representation of a platform name and version.  Version string can be
 * name-ver, name-ver-suffix, or (for compatibility) ver or ver-suffix, in
 * which case the name "OpenBSD CD" is implied.
 */
public class PlatformVersion implements Version {

  // This platform doesn't put its name in the version string
  public static final String DEFAULT_PLATFORM_NAME = "OpenBSD CD";

  private static final int BASE = 10;

  // The platform name.
  private String m_name;
  // Platform version string.
  private String m_ver;
  // Optional suffix
  private String m_suffix;
  // The platform version as an integer.
  private long m_versionInt;

  private static Pattern oldPat =
    RegexpUtil.uncheckedCompile("^([0-9]+)(?:-(.+))?$",
				Perl5Compiler.READ_ONLY_MASK);

  private static Pattern newPat =
    RegexpUtil.uncheckedCompile("^([^-]+)-([0-9]+)(?:-(.+))?$",
				Perl5Compiler.READ_ONLY_MASK);


  /**
   * Construct a Platform Version from a string.
   *
   * Platform versions consist of up to three parts, separated by hyphens:
   * name-ver-suffix.  Ver is an integer, the others are strings.  If the
   * suffix is omitted it's null; if the name is omitted it's OpenBSD.
   *
   * Examples:
   *   1
   *   135
   *   16013-test
   *   Linux rpm-135
   *   OpenBSD CD-456-beta
   */
  public PlatformVersion(String ver) {
    Perl5Matcher matcher = RegexpUtil.getMatcher();
    if (matcher.contains(ver, oldPat)) {
      MatchResult matchResult = matcher.getMatch();
      m_name = DEFAULT_PLATFORM_NAME;
      m_ver = matchResult.group(1);
      m_suffix = matchResult.group(2);
    } else if (matcher.contains(ver, newPat)) {
      MatchResult matchResult = matcher.getMatch();
      m_name = matchResult.group(1);
      m_ver = matchResult.group(2);
      m_suffix = matchResult.group(3);
    } else {
      throw new IllegalArgumentException("Unparseable platform version: " +
					 ver);
    }
    if (m_ver.length() > 11) {
      throw new IllegalArgumentException("Version string too long.");
    }
    try {
      m_versionInt = Long.parseLong(m_ver, BASE);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Unparseable platform version: " +
					 ver);
    }
  }

  public long toLong() {
    return m_versionInt;
  }

  /** Return the platform name */
  public String getName() {
    return m_name;
  }

  /** Return the platform version */
  public String getVersion() {
    return m_ver;
  }

  /** Return the optional suffix */
  public String getSuffix() {
    return m_suffix;
  }

  /** Return a parseable string */
  public String toString() {
    return toString("-");
  }

  /** Return a pretty string */
  public String displayString() {
    return toString(" ");
  }

  public String toString(String sep) {
    StringBuffer sb = new StringBuffer();
    sb.append(m_name);
    sb.append(sep);
    sb.append(m_ver);
    if (!StringUtil.isNullString(m_suffix)) {
      sb.append("-");
      sb.append(m_suffix);
    }
    return sb.toString();
  }

}
