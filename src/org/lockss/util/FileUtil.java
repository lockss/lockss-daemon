/*
 * $Id: FileUtil.java,v 1.1 2003-09-16 23:22:45 eaalto Exp $
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
   * I.e. '/test/../..' would return 'false'.  It assumes the path begins
   * with '/', and isn't guaranteed to handle other forms.
   * @param path the path to be tested
   * @return true iff the path is legal
   */
  public static boolean isLegalPath(String path) {
    int dirCount = 0;
    // don't count first '/' at head of path
    boolean skipNextSlash = true;

    if (path.startsWith("..")) {
      return false;
    }

    // path should start with '/', since it's from a URL object
    int index = path.indexOf("/");
    while (index>-1) {
      if (index>=path.length()-2) {
        // exit if there isn't enough room left for a '..'
        break;
      }
      // increment dir counter or skip
      if (!skipNextSlash) {
        dirCount++;
      } else {
        skipNextSlash = false;
      }

      // check next char
      char c = path.charAt(index+1);
      if (c=='.') {
        // check char after '.'
        c = path.charAt(index+2);
        if (c=='/') {
          // ignore effect of '/./' by skipping next '/' in count
          skipNextSlash = true;
        } else if (c=='.') {
          // '..' detected; check if next char is '/' or end of string
          if (((index+3)==path.length()) || (path.charAt(index+3)=='/')) {
            dirCount--;
            skipNextSlash = true;
          }
        }
      }
      // test for illegality
      if (dirCount < 0) {
        return false;
      }

      // move on to next '/'
      index = path.indexOf("/", index+1);
    }

    return true;
  }

}
