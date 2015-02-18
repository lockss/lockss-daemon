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

/**
 * Representation of a plugin version.  Currently this is a string in
 * the format of a base-36 integer, i.e. "150" or "110b".
 */
public class PluginVersion implements Version {

  // The original version string.
  private String m_versionString;
  // The platform version as an integer.
  private long m_versionInt;

  /**
   * Construct a Plugin Version from a string.
   *
   * Plugin versions are one to twelve base 36 integers (chars a-z,
   * A-Z, and 0-9: anything that will fit into a long), plus an
   * optional dash followed by any string.  The dash and following
   * characters are ignored.
   *
   * Examples:
   *   1
   *   135
   *   16013-test
   *   135abc
   *   123456abcdef-beta
   *
   */
  public PluginVersion(String ver) {
    m_versionString = ver;
    try {
      int dash = ver.indexOf('-');
      if ((dash == -1 && ver.length() > 11) || dash > 11) {
	throw new IllegalArgumentException("Version string too long.");
      }
      if (dash > -1) {
	m_versionInt = Long.parseLong(ver.substring(0, dash), 36);
      } else {
	m_versionInt = Long.parseLong(ver, 36);
      }
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Illegal format for Plugin " +
					 "Version: " + ver);
    }
  }

  public long toLong() {
    return m_versionInt;
  }


  public String toString() {
    return m_versionString;
  }

}
