/*
 * $Id: FileUtil.java,v 1.11 2009-06-01 07:35:00 tlipkis Exp $
 *

Copyright (c) 2000-2008 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import org.apache.oro.text.regex.*;

/** Utilities for Files
 */
public class FileUtil {
  static final Logger log = Logger.getLogger("FileUtil");

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

  /** Ensure the directory exists, creating it and any parents if
   * necessary.  mkdirs() has been observed to fail intermittently on some
   * platforms, so try a few times if it fails.
   * @param dir the directory
   * @return true if the directory already exists, if it was successfully
   * created, or if it came into being while we were trying to create it.
   */
  public static boolean ensureDirExists(File dir) {
    if (dir.exists()) {
      return true;
    }
    for (int cnt = 3; cnt > 0; cnt--) {
      if (dir.mkdirs()) {
	return true;
      }
      if (dir.exists()) {
	return true;
      }
      log.error("Failed to mkdirs(" + dir + "), retrying");
      try {
	Deadline.in(100).sleep();
      } catch (InterruptedException e) {
      }
    }
    // If another thread is trying to create the same dir, it might have
    // suceeded, causing our call to mkdirs to return false, so check again
    // to see if it's there.  (I believe this happened creating the v3state
    // dir
    return dir.exists();
  }

  public static String relativeName(String name, String relativeTo) {
    if (relativeTo == null) {
      return name;
    }
    if (!relativeTo.endsWith(File.pathSeparator)) {
      relativeTo = relativeTo + File.separator;
    }
    if (name.startsWith(relativeTo)) {
      return name.substring(relativeTo.length());
    }
    return name;
  }

  public static File relativeFile(File file, String relativeTo) {
    return new File(relativeName(file.getPath(), relativeTo));
  }

  /**
   * Return list of all files in tree below root
   */
  public static List<String> listTree(File root, boolean includeDirs) {
    return listTree(root, (String)null, includeDirs);
  }

  /**
   * Return list of all files in tree below root
   */
  public static List<String> listTree(String root, String relativeTo,
				    boolean includeDirs) {
    return listTree(new File(root), relativeTo, includeDirs);
  }

  /**
   * Return list of all files in tree below root
   */
  public static List<String> listTree(File root, File relativeTo,
				      boolean includeDirs) {
    return listTree(root, relativeTo.toString(), includeDirs);
  }

  /**
   * Return list of all files in tree below root
   */
  public static List<String> listTree(File root, String relativeTo,
				    boolean includeDirs) {
    List<String> res = new ArrayList();
    listTree0(res, root, relativeTo, includeDirs);
    Collections.sort(res);
    return res;
  }

  private static List<String> listTree0(List<String> res, File root,
				      String relativeTo,
				      boolean includeDirs) {
    for (File file : root.listFiles()) {
      if (file.isDirectory()) {
	if (includeDirs) {
	  res.add(relativeName(file.getPath(), relativeTo));
	}
	listTree0(res, file, relativeTo, includeDirs);
      } else {
	res.add(relativeName(file.getPath(), relativeTo));
      }
    }
    return res;
  }

  /** Compare two trees, return true if identical files and contents */
  public static boolean equalTrees(File dir1, File dir2) throws IOException {
    List<String> lst1 = listTree(dir1, dir1, true);
    List<String> lst2 = listTree(dir2, dir2, true);
    Collections.sort(lst1);
    Collections.sort(lst2);
    if (!lst1.equals(lst2)) {
      log.info("not equal: " + lst1);
      log.info("not equal: " + lst2);
      return false;
    }
    for (String file : lst1) {
      File f1 = new File(dir1, file);
      File f2 = new File(dir2, file);
      if (f1.isDirectory() != f2.isDirectory()) {
	return false;
      }
      if (!f1.isDirectory()) {
	if (!isContentEqual(f1, f2)) {
	  return false;
	}
      }
    }
    return true;
  }


  /** Delete the contents of a directory, leaving the empty directory.
   * @return true iff successful */
  public static boolean emptyDir(File dir) {
    String files[] = dir.list();
    if (files == null) {
      return false;		  // true would imply there's an empty
				  // dir, which there doesn't seem to be
    }
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
    emptyDir(dir);
    if (dir.delete()) {
      return true;
    } else return !dir.exists();
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
