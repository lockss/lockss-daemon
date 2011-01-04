/*
 * $Id: FileTestUtil.java,v 1.8.8.2 2011-01-04 04:52:09 dshr Exp $
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
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.NameScope;
import org.apache.commons.vfs.operations.*;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.provider.ram.RamFileProvider;

/** Utilities for Files and FileObjects involved in the test hierarchy
 */
public class FileTestUtil {
  /** Create and return the name of a temp file that will be deleted
   * when jvm terminated
   */
  private static FileSystem ramfs = null;
  private static Random rand = new Random();

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
    FileObject f = null;
    if (ramfs == null) {
      initRamFs();
    }
    try {
      if (dir == null) {
	dir = ramfs.getRoot();
      }
      String tempNamePart = null;
      FileObject tempFileObject = null;
      do {
	tempNamePart = Integer.toHexString(rand.nextInt());
	tempFileObject = dir.resolveFile(prefix + tempNamePart + suffix);
      } while (tempFileObject.exists());
      tempFileObject.createFile();
      f = tempFileObject;
    } catch (FileSystemException ex) {
      // No action intended
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
   * Initialize a RAM file system
   */
   private static void initRamFs() {
     if (true) throw new UnsupportedOperationException("XXX implement me");
   }

  /**
   * Return a FileObject that is guaranteed to throw if created.
   */
  private static FileObject mfo = new MyFileObject();

  public static FileObject impossibleFileObject()
      throws IOException {
    return mfo;
  }

  private static class MyFileObject implements FileObject {
    private MyFileObject() {
    }

    public boolean canRenameTo(FileObject file) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public void close() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public void copyFrom(FileObject srcFile, FileSelector selector) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public void createFile() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public void createFolder() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public boolean delete() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public int delete(FileSelector selector) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public boolean exists() {
      return false;
    }

    public FileObject[] findFiles(FileSelector selector) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public void findFiles(FileSelector selector,
			  boolean depthwise, java.util.List selected) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileObject getChild(String name) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileObject[] getChildren() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileContent getContent() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileOperations getFileOperations() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileSystem getFileSystem() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileName getName() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileObject getParent() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileType getType() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public URL getURL() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileObject resolveFile(String name) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public boolean isAttached() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public boolean isContentOpen() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public boolean isHidden() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public boolean isReadable() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public boolean isWriteable() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public void moveTo(FileObject destFile) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public void refresh() {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileObject resolveFile(FileName name) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }

    public FileObject resolveFile(java.lang.String name, NameScope scope) {
      throw new UnsupportedOperationException("ImpossibleFileObject");
    }
  }
}
