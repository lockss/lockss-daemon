/*
 * $Id: FileUtil.java,v 1.5 2003-07-19 00:05:28 troberts Exp $
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

package org.lockss.test;

import java.util.*;
import java.io.*;
import java.net.*;
import java.security.*;
import org.lockss.util.ListUtil;

/** Utilities for Files
 */
public class FileUtil {
  /** Create and return the name of a temp file that will be deleted
   * when jvm terminated
   */
  public static File tempFile(String prefix)
      throws IOException {
    return tempFile(prefix, null);
  }

  public static File tempFile(String prefix, File dir) 
      throws IOException {
    File f = File.createTempFile(prefix, null, dir);
    f.deleteOnExit();
    return f;
  }

  /** Write  a temp file containing string and return its name */
  public static File writeTempFile(String prefix, String contents)
      throws IOException {
    File file = tempFile(prefix);
    FileWriter fw = new FileWriter(file);
    fw.write(contents);
    fw.close();
    return file;
  }

  /** Store the string in a temp file and return a file: url for it */
  public static String urlOfString(String s) throws IOException {
    File file = FileUtil.writeTempFile("test", s);
    return file.toURL().toString();
  }

  private static final Object tmpFileLock = new Object();
  private static int tmpFileCnt = -1; /* Protected by tmpFileLock */

  private static File generateFile(String prefix, String suffix, File dir)
      throws IOException {
    if (tmpFileCnt == -1) {
      tmpFileCnt = new Random().nextInt() & 0xffff;
    }
    tmpFileCnt++;
    return new File(dir, prefix + Integer.toString(tmpFileCnt) + suffix);
  }

  /** Create an empty directory.  Details are the same as
   * File.createTempFile(), but the File object returned is a directory.
   * @return The newly created directory */
  public static File createTempDir(String prefix, String suffix,
				   File directory)
      throws IOException {
    if (prefix == null) throw new NullPointerException();
    if (prefix.length() < 3)
      throw new IllegalArgumentException("Prefix string too short");
    String s = (suffix == null) ? ".tmp" : suffix;
    synchronized (tmpFileLock) {
      if (directory == null) {
	directory = new File(System.getProperty("java.io.tmpdir"));
      }
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

  /**
   * Generate and return a list of all the files under this directory or file
   * @param file directory or file which to enumerate
   * @return all the files under file, or file if it isn't a directory
   */
  public static List enumerateFiles(File file) {
    if (file == null) {
      throw new IllegalArgumentException("Null file specified");
    }
    if (file.isDirectory()) {
      List list = new ArrayList();
      File[] files = file.listFiles();
      for (int ix=0; ix< files.length; ix++) {
 	list.addAll(enumerateFiles(files[ix]));
      }
      return list;
    }
    return ListUtil.list(file);
  }

  /**
   * Return the path of src relative to root
   */
  public static String getPathUnderRoot(File src, File root) {
    if (src == null || root == null) {
      throw new IllegalArgumentException("Null file specified");
    }
    String srcString = src.getPath();
    String rootString = root.getPath();
    if (srcString.startsWith(rootString)) {
      return srcString.substring(rootString.length());
    }
    return null;
  }

}
