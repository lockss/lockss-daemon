/*
 * $Id: TestFileUtil.java,v 1.11 2009-06-01 07:35:00 tlipkis Exp $
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

import java.io.*;
import java.util.*;
import org.apache.commons.collections.*;
import org.lockss.test.*;

/**
 * test class for org.lockss.util.TestFileTestUtil
 */

public class TestFileUtil extends LockssTestCase {
  String tempDirPath;

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
  }

  public void testSysDepPath() {
    String testStr = "test/var\\foo";
    String expectedStr = "test"+File.separator+"var"+File.separator+"foo";
    assertEquals(expectedStr, FileUtil.sysDepPath(testStr));
  }

  public void testSysIndepPath() {
    String testStr = "test/var\\foo";
    String expectedStr = "test/var/foo";
    assertEquals(expectedStr, FileUtil.sysIndepPath(testStr));
  }

  boolean isLegal(String x) {
    return FileUtil.isLegalPath(x);
  }

  public void testIsLegalPath() {
    assertTrue(isLegal("."));
    assertTrue(isLegal("/"));
    assertTrue(isLegal("/."));
    assertTrue(isLegal("./"));
    assertTrue(isLegal("//"));

    assertFalse(isLegal(".."));
    assertFalse(isLegal("../"));
    assertFalse(isLegal("..//"));
    assertFalse(isLegal("/.."));
    assertFalse(isLegal("//.."));
    assertFalse(isLegal("./.."));
    assertFalse(isLegal("./../"));
    assertFalse(isLegal("/./../"));
    assertFalse(isLegal("/./././.."));
    assertTrue(isLegal("/./././x/.."));

    assertTrue(isLegal("/var"));
    assertTrue(isLegal("/var/"));
    assertTrue(isLegal("/var/foo"));
    assertTrue(isLegal("/var/../foo"));
    assertTrue(isLegal("/var/.."));
    assertTrue(isLegal("/var/../foo/.."));

    assertTrue(isLegal("var/./foo"));
    assertTrue(isLegal("var/."));

    assertFalse(isLegal("/var/../.."));
    assertFalse(isLegal("/var/../../foo"));
    assertFalse(isLegal("/var/.././.."));
    assertFalse(isLegal("/var/.././..///"));

    assertFalse(isLegal("var/../.."));
    assertFalse(isLegal("var/../../foo"));
    assertFalse(isLegal("var/.././.."));
    assertFalse(isLegal("var/.././..///"));
  }

  public void testFileContentIsIdentical() throws Exception {
    File file1 = createFile(tempDirPath + "file1", "content 1");
    File file2 = createFile(tempDirPath + "file2", "content 2");
    File file3 = createFile(tempDirPath + "file3", "content 1");
    // shorter length
    File file4 = createFile(tempDirPath + "file4", "con 4");

    assertFalse(FileUtil.isContentEqual(file1, null));
    assertFalse(FileUtil.isContentEqual(null, file1));
    assertFalse(FileUtil.isContentEqual(null, null));
    assertFalse(FileUtil.isContentEqual(file1, file2));
    assertFalse(FileUtil.isContentEqual(file1, file4));

    assertTrue(FileUtil.isContentEqual(file1, file1));
    assertTrue(FileUtil.isContentEqual(file1, file3));
  }

  File createFile(String name, String content) throws Exception {
    File file = new File(name);
    FileOutputStream fos = new FileOutputStream(file);
    InputStream sis = new StringInputStream(content);
    StreamUtil.copy(sis, fos);
    sis.close();
    fos.close();
    return file;
  }

  public void testIsTemporaryResourceException() throws IOException {
    String EMFILE = "foo.bar (Too many open files)";
    assertTrue(FileUtil.isTemporaryResourceException(new FileNotFoundException(EMFILE)));
    assertFalse(FileUtil.isTemporaryResourceException(new FileNotFoundException("No such file or directory")));
    assertFalse(FileUtil.isTemporaryResourceException(new IOException(("No such file or directory"))));
  }

  public void testTempDir() throws IOException {
    try {
      File dir = FileUtil.createTempDir("pre", "suff", new File("/nosuchdir"));
      fail("Shouldn't be able to create temp dir in /nosuchdir");
    } catch (IOException e) {
    }
    File dir = FileUtil.createTempDir("pre", "suff");
    assertTrue(dir.exists());
    assertTrue(dir.isDirectory());
    assertEquals(0, dir.listFiles().length);
    File f = new File(dir, "foo");
    assertFalse(f.exists());
    assertTrue(f.createNewFile());
    assertTrue(f.exists());
    assertEquals(1, dir.listFiles().length);
    assertEquals("foo", dir.listFiles()[0].getName());
    assertTrue(f.delete());
    assertEquals(0, dir.listFiles().length);
    assertTrue(dir.delete());
    assertFalse(dir.exists());
    File parentDir = FileUtil.createTempDir("testTempDir", ".foo");
    assertTrue(parentDir.exists());
    assertTrue(parentDir.isDirectory());
    // Test creating under another directory
    File subDir = FileUtil.createTempDir("subTempDir", ".bar", parentDir);
    assertTrue(subDir.exists());
    assertTrue(subDir.isDirectory());
    FileUtil.delTree(parentDir);

  }

  void writeFile(File dir, String relPath) throws IOException {
    File file = new File(dir, relPath);
    File parent = new File(file.getParent());
    parent.mkdirs();
    FileTestUtil.writeFile(file, relPath);
  }

  String apath = "d1/d2/1";

  String[] relPaths = {
    "one",
    "two",
    "d1/1",
    "d1/2",
    apath,
    "d1/d2/2",
    "d1/d2/d3/d4/1",
  };

  String[] dirNames = {
    "d1",
    "d1/d2",
    "d1/d2/d3",
    "d1/d2/d3/d4",
  };

  public void buildTree(File dir) throws IOException {
    for (String rel : relPaths) {
      writeFile(dir, rel);
    }
  }

  List<File> listOfFiles(List lst) {
    return (List<File>)
      CollectionUtils.collect(lst,
			      new Transformer() {
				public Object transform(Object o) {
				  return new File(o.toString());
				}});
  }

  List<String> prepend(final String prefix, List lst) {
    return (List<String>)
      CollectionUtils.collect(lst,
			      new Transformer() {
				public Object transform(Object o) {
				  return prefix + o.toString();
				}});
  }

  public void testListTree() throws IOException {
    File dir = getTempDir();
    buildTree(dir);
    List exp = ListUtil.fromArray(relPaths);
    Collections.sort(exp);
    assertEquals(exp, FileUtil.listTree(dir, dir.getPath(), false));
    assertEquals(prepend(dir.getPath() + File.separator, exp),
		 FileUtil.listTree(dir, false));
    exp.addAll(ListUtil.fromArray(dirNames));
    Collections.sort(exp);
    assertEquals(exp, FileUtil.listTree(dir, dir.getPath(), true));
    assertEquals(prepend(dir.getPath() + File.separator, exp),
		 FileUtil.listTree(dir, true));
  }

  public void testEqualTrees() throws IOException {
    File dir1 = getTempDir();
    File dir2 = getTempDir();
    buildTree(dir1);
    buildTree(dir2);
    assertTrue(FileUtil.equalTrees(dir1, dir2));
    writeFile(dir1, "afile");
    File f1 = new File(dir1, "afile");
    assertTrue(f1.exists());
    assertFalse(FileUtil.equalTrees(dir1, dir2));
    f1.delete();
    assertFalse(f1.exists());
    assertTrue(FileUtil.equalTrees(dir1, dir2));
    File f2 = new File(dir1, apath);
    assertTrue(f2.exists());
    FileTestUtil.writeFile(f2, "foobar");
    assertEquals(FileUtil.listTree(dir1, dir1.getPath(), true),
		 FileUtil.listTree(dir2, dir2.getPath(), true));
    assertFalse(FileUtil.equalTrees(dir1, dir2));
  }

  public void testDelTree() throws IOException {
    File dir = getTempDir("deltree");
    File d1 = new File(dir, "foo");
    assertTrue(d1.mkdir());
    File d2 = new File(d1, "bar");
    assertTrue(d2.mkdir());
    assertTrue(new File(dir, "f1").createNewFile());
    assertTrue(new File(d1, "d1f1").createNewFile());
    assertTrue(new File(d2, "d2f1").createNewFile());
    assertFalse(dir.delete());
    assertTrue(FileUtil.delTree(dir));
    assertFalse(dir.exists());
  }

  public void testDelTreeNoDir() throws IOException {
    File dir = getTempDir("deltree");
    File d1 = new File(dir, "foo");
    assertFalse(d1.exists());
    assertTrue(FileUtil.delTree(d1));
  }

  public void testEmptyDir() throws IOException {
    File dir = getTempDir("deltree");
    File d1 = new File(dir, "foo");
    assertTrue(d1.mkdir());
    File d2 = new File(d1, "bar");
    assertTrue(d2.mkdir());
    assertTrue(new File(dir, "f1").createNewFile());
    assertTrue(new File(d1, "d1f1").createNewFile());
    assertTrue(new File(d2, "d2f1").createNewFile());
    assertFalse(dir.delete());
    assertTrue(FileUtil.emptyDir(dir));
    String files[] = dir.list();
    assertEquals(0, files.length);
  }

  public void testEmptyDirNoDir() throws IOException {
    File dir = getTempDir("deltree");
    File d1 = new File(dir, "foo");
    assertFalse(d1.exists());
    assertFalse(FileUtil.emptyDir(d1));
  }
}

