/*
 * $Id: MockFile.java,v 1.6 2006-09-28 02:09:17 tlipkis Exp $
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

package org.lockss.test;
import junit.framework.Assert;
import java.io.*;
import java.net.URL;
import java.util.*;


public class MockFile extends File {

  String path;
  boolean isDirectory = false;
  boolean isFile = false;
  boolean exists = false;
  boolean mkdirCalled = false;
  List children = new ArrayList();

  public MockFile(String path) {
    super(path);
    this.path = path;
  }

  public String getName() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String getParent() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public File getParentFile() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String getPath() {
    return path;
  }

  public boolean isAbsolute() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String getAbsolutePath() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public File getAbsoluteFile() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String getCanonicalPath() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public File getCanonicalFile() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public URL toURL() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean canRead() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean canWrite() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean exists() {
    return exists;
  }

  public void setExists(boolean exists) {
    this.exists = exists;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public void setIsDirectory(boolean isDirectory) {
    this.isDirectory = isDirectory;
  }

  public boolean isFile() {
    return isFile;
  }

  public void setIsFile(boolean isFile) {
    this.isFile = isFile;
  }

  public boolean isHidden() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public long lastModified() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public long length() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean createNewFile() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean delete() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void deleteOnExit() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String[] list() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String[] list(FilenameFilter filter) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public File[] listFiles() {
    File[] files = new File[children.size()];
    for (int ix=0; ix<files.length; ix++) {
      files[ix] = (File)children.remove(0);
    }
    return files;
  }

  public File[] listFiles(FilenameFilter filter) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public File[] listFiles(FileFilter filter) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public void setChild(File child) {
    children.add(child);
  }

  public boolean mkdir() {
    mkdirCalled = true;
    return true;
  }

  public void assertMkdirCalled() {
    if (!mkdirCalled) {
      Assert.fail("mkdir not called");
    }
  }

  public boolean mkdirs() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean renameTo(File dest) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean setLastModified(long time) {
    throw new UnsupportedOperationException("Not implemented");
  }

  public boolean setReadOnly() {
    throw new UnsupportedOperationException("Not implemented");
  }

//   public int compareTo(File pathname) {
//     throw new UnsupportedOperationException("Not implemented");
//   }

//   public int compareTo(Object o) {
//     throw new UnsupportedOperationException("Not implemented");
//   }

  public boolean equals(Object obj) {
    if (obj instanceof MockFile) {
      return path.equals(((MockFile)obj).getPath());
    }
    return false;
  }

  public int hashCode() {
    throw new UnsupportedOperationException("Not implemented");
  }

  public String toString() {
    return "[MockFile: path="+path+"]";
  }
}
