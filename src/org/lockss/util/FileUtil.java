/*
 * $Id: FileUtil.java,v 1.2 2003-09-18 06:50:12 tlipkis Exp $
 *

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

import java.io.File;

/** Utilities for Files
 */
public class FileUtil {
  /**
   * Converts the file path given into a system-dependent form.
   * For example, 'var/foo/bar' becomes 'var\foo\bar' on a Windows machine
   * and vice versa.
   * @param filePath the path
   * @return the new path
   */
  public static String sysDepPath(String filePath) {
    if (File.separatorChar == '/') {
      return filePath.replace('\\', File.separatorChar);
    } else {
      return filePath.replace('/', File.separatorChar);
    }
  }

  /**
   * Converts the file path given into a system-independent form, utilizing only
   * '/' as a separator.
   * @param filePath the path
   * @return the new path
   */
  public static String sysIndepPath(String filePath) {
    if (File.separatorChar == '/') {
      return filePath.replace('\\', '/');
    } else {
      return filePath.replace(File.separatorChar, '/');
    }
  }

  /**
   * Tests a path to see if it moves 'above' the root via '..'.
   * I.e. '/test/../..' would return 'false'.
   * @param path the path to be tested
   * @return true iff the path is legal
   */
  public static boolean isLegalPath(String path) {
    int len = path.length();
    int depth = 0;
    int index = -1;			// Points to char before start of next
					// path component.  (Normally a slash)
    while (index<len-2) {
      depth++;				// assume it's a real path component

      // index+1 points at start of path component.  Check first char
      switch (path.charAt(index+1)) {
      case '/':
	depth--;			// empty path component ("//") doesn't
	break;				// count. (Equivalent to single slash)
      case '.':
	// component starts with "."
	switch (path.charAt(index+2)) {
	case '/':
	  depth--;			// './' doesn't count
	  break;
	case '.':
	  // component starts with '..'; is next char '/' or end of string?
	  if (((index+3)==len) || (path.charAt(index+3)=='/')) {
	    depth-=2;	   // '../' doesn't count, and reduces depth by one
	  }
	  break;
	}
	break;
      }
      // if depth is negative, path has too many '..'s
      if (depth < 0) {
	return false;
      }
      index = path.indexOf("/", index+1);
      if (index < 0) break;
    };
    return true;
  }
}
