/*
 * $Id: FileUtil.java,v 1.7 2006-11-09 01:44:54 thib_gc Exp $
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

import java.io.*;
import java.util.Arrays;
import org.apache.oro.text.regex.*;

/** Utilities for Files
 */
public class FileUtil {
  static final int FILE_CHUNK_SIZE = 1024;

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
    }
    return true;
  }

  /**
   * Compares the content of two files and returns true if they are the same.
   * If either file is null or a directory, returns false.
   * @param file1 the first file
   * @param file2 the second file
   * @return true iff content is identical
   * @throws IOException
   */
  public static boolean isContentEqual(File file1, File file2)
      throws IOException {
    if ((file1==null) || (file2==null)) {
      // null is never equal
      return false;
    }

    if ((file1.isDirectory()) || (file2.isDirectory())) {
      // don't compare directories
      return false;
    }

    if (file1.length() != file2.length()) {
      // easy length check
      return false;
    }

    // compare both streams
    FileInputStream fis1 = null;
    FileInputStream fis2 = null;
    try {
      fis1 = new FileInputStream(file1);
      fis2 = new FileInputStream(file2);

      byte[] bytes1 = new byte[FILE_CHUNK_SIZE];
      byte[] bytes2 = new byte[FILE_CHUNK_SIZE];
      while (true) {
        int bytesRead1 = fis1.read(bytes1);
        int bytesRead2 = fis2.read(bytes2);

        if (bytesRead1 != bytesRead2) {
          // shouldn't really happen, since lengths are equal
          return false;
        } else if (bytesRead1==-1) {
          // EOF reached, exit
          break;
        }

        if (!Arrays.equals(bytes1, bytes2)) {
          return false;
        }
      }
      return true;
    } catch (FileNotFoundException fnfe) {
      // if the file is absent, no comparison
      return false;
    } finally {
      // make sure to close open inputstreams
      if (fis1!=null) {
        fis1.close();
      }
      if (fis2!=null) {
        fis2.close();
      }
    }
  }

  /**
   * Checks to see if a FileOutputStream can be created to a file containing
   * the given char.  For example, '?' is illegal on Windows but not Unix.
   * This function does not actually write to the output stream.
   * @param location the location to attempt file creation in.
   * @param testChar the char to test
   * @return boolean true iff an output stream can be created
   */
  public static boolean canWriteToFileWithChar(String location, char testChar) {
    File file = new File(location, "test"+testChar+"test");
    try {
      // should throw if this is an illegal destination
      FileOutputStream fos = new FileOutputStream(file);
      fos.close();
      return true;
    } catch (IOException ioe) {
      return false;
    }
  }

  static Pattern resourceErrorPat =
    RegexpUtil.uncheckedCompile("Too many open files",
				Perl5Compiler.READ_ONLY_MASK);

  /** Return true if the exception was caused by a temporary resource
   * problem (e.g., running out of file descriptors), not a problem with
   * the file itself
   */
  public static boolean isTemporaryResourceException(IOException ex) {
    if (!(ex instanceof FileNotFoundException)) {
      return false;
    }
    return RegexpUtil.getMatcher().contains(ex.getMessage(), resourceErrorPat);
  }


  // Support for creating temporary files and directories

  private static int tmpFileCnt = -1;
  private static final Object tmpFileLock = new Object(); // tmpFileCnt lock

  public static File createTempFile(String prefix, String suffix, File dir)
      throws IOException {
    if (dir == null) {
      dir = new File(PlatformUtil.getSystemTempDir());
    }
    return File.createTempFile(prefix, suffix, dir);
  }

  public static File createTempFile(String prefix, String suffix)
      throws IOException {
    return createTempFile(prefix, suffix, null);
  }

  /** Create an empty directory.  Details are the same as
   * File.createTempFile(), but the File object returned is a directory.
   * @param directory the directory under which to create the new dir
   * @param prefix dir name prefix
   * @param suffix dir name suffix
   * @return The newly created directory
   */
  public static File createTempDir(String prefix, String suffix,
				   File directory)
      throws IOException {
    if (prefix == null) throw new NullPointerException();
    if (prefix.length() < 3)
      throw new IllegalArgumentException("Prefix string too short");
    String s = (suffix == null) ? ".tmp" : suffix;
    if (directory == null) {
      directory = new File(PlatformUtil.getSystemTempDir());
    }
    synchronized (tmpFileLock) {
      File f = null;
      for (int ix = 0; ix < 1000; ix++) {
	f = generateFile(prefix, s, directory);
	if (f.mkdir()) {
	  return f;
	}
      }
      throw new IOException("Couldn't create temp dir " + f.getPath());
    }
  }

  /** Create an empty directory in the default temporary-file directory.
   * Details are the same as File.createTempFile(), but the File object
   * returned is a directory.
   * @return The newly created directory
   */
  public static File createTempDir(String prefix, String suffix)
      throws IOException {
    return createTempDir(prefix, suffix, null);
  }

  /** Delete the contents of a directory, leaving the empty directory.
   * @return true iff successful */
  public static boolean emptyDir(File dir) {
    String files[] = dir.list();
    boolean ret = true;
    for (int i = 0; i < files.length; i++) {
      File f = new File(dir, files[i]);
      if (f.isDirectory()) {
	ret = ret && emptyDir(f);
      }
      if (!f.delete()) {
	ret = false;
      }
    }
    return ret;
  }

  /** Delete a directory and its contents.
   * @return true iff successful */
  public static boolean delTree(File dir) {
    return emptyDir(dir) && dir.delete();
  }

  private static File generateFile(String prefix, String suffix, File dir)
      throws IOException {
    if (tmpFileCnt == -1) {
      tmpFileCnt = new LockssRandom().nextInt() & 0xffff;
    }
    tmpFileCnt++;
    return new File(dir, prefix + Integer.toString(tmpFileCnt) + suffix);
  }
}
