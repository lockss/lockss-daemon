/*
 * $Id: FileTestUtil.java,v 1.8.8.1 2011-01-03 18:30:06 dshr Exp $
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
import org.lockss.util.*;

import org.apache.commons.vfs.FileContent;
import org.apache.commons.vfs.FileName;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystem;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileType;

/** Utilities for Files and FileObjects involved in the test hierarchy
 */
public class FileTestUtil {
  /** Create and return the name of a temp file that will be deleted
   * when jvm terminated
   */
  public static File tempFile(String prefix)
      throws IOException {
    return tempFile(prefix, null, null);
  }

  public static File tempFile(String prefix, File dir)
      throws IOException {
    return tempFile(prefix, null, dir);
  }

  public static File tempFile(String prefix, String suffix)
      throws IOException {
    return tempFile(prefix, suffix, null);
  }

  public static File tempFile(String prefix, String suffix, File dir)
      throws IOException {
    File f = File.createTempFile(prefix, suffix, dir);
    if (!LockssTestCase.isKeepTempFiles()) {
      f.deleteOnExit();
    }
    return f;
  }

  public static FileObject tempFileObject(String prefix)
      throws IOException {
    return tempFileObject(prefix, null, null);
  }

  public static FileObject tempFileObject(String prefix, FileObject dir)
      throws IOException {
    return tempFileObject(prefix, null, dir);
  }

  public static FileObject tempFileObject(String prefix, String suffix)
      throws IOException {
    return tempFileObject(prefix, suffix, null);
  }

  public static FileObject tempFileObject(String prefix, String suffix, FileObject dir)
      throws IOException {
    if (true) throw new UnsupportedOperationException("XXX implement me");
    FileObject f = null;  // XXX
    // FileObject f = FileObject.createTempFileObject(prefix, suffix, dir);
    if (!LockssTestCase.isKeepTempFiles()) {
      // f.deleteOnExit();
    }
    return f;
  }

  /** Write a temp file containing string and return its name */
  public static File writeTempFile(String prefix, String contents)
      throws IOException {
    return writeTempFile(prefix, null, contents);
  }

  public static File writeTempFile(String prefix, String suffix, String contents)
      throws IOException {
    File file = tempFile(prefix, suffix, null);
    writeFile(file, contents);
    return file;
  }

  public static void writeFile(File file, String contents) throws IOException {
    Writer wrtr = new OutputStreamWriter(new FileOutputStream(file),
					 Constants.DEFAULT_ENCODING);
    wrtr.write(contents);
    wrtr.close();
  }

  /** Store the string in a temp file and return a file: url for it */
  public static String urlOfString(String s) throws IOException {
    File file = FileTestUtil.writeTempFile("test", s);
    return file.toURI().toURL().toString();
  }

  /** Store the string in a temp file and return a file: url for it */
  public static String urlOfString(String s, String suffix) throws IOException {
    File file = FileTestUtil.writeTempFile("test", suffix, s);
    return file.toURI().toURL().toString();
  }

  /** Return the (absolute) url for the (possibly relative) file name */
  public static String urlOfFile(String s) throws IOException {
    File file = new File(s);
    return file.toURI().toURL().toString();
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

  /**
   * Return a FileObject that is guaranteed to throw if created.
   */
  public static FileObject impossibleFileObject()
      throws IOException {
    if (true) throw new UnsupportedOperationException("XXX implement me");
    return null;
  }

}
