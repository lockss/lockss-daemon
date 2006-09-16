/*
 * $Id: TestFilterRuleRunner.java,v 1.1 2006-09-16 22:55:22 tlipkis Exp $
 */

/*

Copyright (c) 2000-2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.devtools;
import java.io.*;
import java.util.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * @deprecated this class should be deleted when all plugins have been
 * converted to use {@link FilterFactory}s instead of {@link FilterRule}s
 */
public class TestFilterRuleRunner extends LockssTestCase {

  //tests for filterDirectory(FilterRule, File, File)

  public void testDirThrowsOnNullFilterRule()
      throws FileNotFoundException, IOException {
    try {
      FilterRunner.filterDirectory((FilterRule)null,
				   new MockFile("blah"),
				   new MockFile("blah2"));
      fail("Calling filterSingleFile with a null FilterRule "
	   +"should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testDirThrowsOnNullSrc()
      throws FileNotFoundException, IOException {
    try {
      FilterRunner.filterDirectory(new MockFilterRule(),
				   null,
				   new MockFile("blah2"));
      fail("Calling filterSingleFile with a null source file "
	   +"should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testDirThrowsOnExistingSrcOtherThanDir()
      throws FileNotFoundException, IOException {
    try {
      MockFile src = new MockFile("blah");
      src.setExists(true);
      FilterRunner.filterDirectory(new MockFilterRule(),
				   src,
				   new MockFile("blah2"));
      fail("Calling filterSingleFile with a src file that returns false "
	   +"on isFile() should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testDirThrowsOnNullDest()
      throws FileNotFoundException, IOException {
    try {
      MockFile src = new MockFile("blah");
      src.setIsDirectory(true);
      FilterRunner.filterDirectory(new MockFilterRule(),
				   src,
				   null);
      fail("Calling filterSingleFile with a null destination file "
	   +"should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testFilterDirectoryCreatesDirIfItDoesntExist()
      throws FileNotFoundException, IOException {
    MockFile src = new MockFile("blah");
    src.setIsDirectory(true);
    src.setExists(true);
    MockFile dest = new MockFile("blah2");
    dest.setExists(false);

    FilterRunner.filterDirectory(new MockFilterRule(), src, dest);
    dest.assertMkdirCalled();
  }

  public void testDirThrowsOnDestOtherThanDir()
      throws FileNotFoundException, IOException {
    try {
      MockFile src = new MockFile("blah");
      src.setIsDirectory(true);
      src.setExists(true);
      MockFile dest = new MockFile("blah2");
      dest.setExists(true);
      dest.setIsDirectory(false);
      FilterRunner.filterDirectory(new MockFilterRule(), src, dest);
      fail("Calling filterSingleFile with a dest file that returns false "
	   +"on isDirectory() should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  private static String hwFilterRuleStr =
    "org.lockss.plugin.highwire.HighWireFilterRule";

  private static String mockFilterRuleStr =
    "org.lockss.test.MockFilterRule";

  public void testFilterRuleFromStringHighWire()
      throws ClassNotFoundException, InstantiationException,
	     IllegalAccessException {
    FilterRule filterRule = FilterRunner.filterRuleFromString(hwFilterRuleStr);
    assertTrue(filterRule instanceof
	       org.lockss.plugin.highwire.HighWireFilterRule);
  }

  public void testFilterRuleFromStringMock()
      throws ClassNotFoundException, InstantiationException,
	     IllegalAccessException  {
    FilterRule filterRule =
      FilterRunner.filterRuleFromString(mockFilterRuleStr);
    assertTrue(filterRule instanceof org.lockss.test.MockFilterRule);
  }

  public void testOneLevel() throws IOException {
    File src = getTempDir();
    File dest = getTempDir();
    File files[] = populateDir(src, "file", 3);
    MyMockFilterRule filter = new MyMockFilterRule();
    filter.setString("Test String");
    FilterRunner.filterDirectory(filter, src, dest);

    for (int ix=0; ix<files.length; ix++) {
      String file = files[ix].getPath();
      file = file.substring(file.lastIndexOf(File.separator));
      File tmpFile = new File(dest, file);
      assertFileExists(tmpFile);
      assertReaderMatchesString("Test String", new FileReader(tmpFile));
    }
  }

  private static File[] populateDir(File dir, String fileBase, int numFiles)
      throws IOException {
    File files[] = new File[numFiles];
    for (int ix=0; ix<numFiles; ix++) {
      files[ix] = new File(dir, fileBase+ix);
      files[ix].createNewFile();
    }
    return files;
  }

  private static File[] populateDir(File dir,
				    String fileBase, int numFiles,
				    String dirBase, int numDirs)
      throws IOException {
    File dirs[] = new File[numDirs];
    File files[] = new File[numFiles * numDirs];
    for (int ix=0; ix<numDirs; ix++) {
      dirs[ix] = new File(dir, dirBase+ix);
      dirs[ix].mkdir();
      File tmpFiles[] = populateDir(dirs[ix], fileBase, numFiles);
      for (int jy=0; jy<tmpFiles.length; jy++) {
	files[jy + (ix*numFiles)] = tmpFiles[jy];
      }
    }
    return files;
  }

  public static File mapFileToOtherRoot(File file,
					File oldRoot,
					File newRoot) {
    String path = file.getPath();
    String oldRootPath = oldRoot.getPath();
    String newRootPath = newRoot.getPath();
    String newPath = StringUtil.replaceFirst(path, oldRootPath, newRootPath);
    return new File(newPath);
  }

  public void testTwoLevels() throws IOException {
    File src = getTempDir();
    File dest = getTempDir();
    File files[] = populateDir(src, "file", 3, "dir", 4);
    MyMockFilterRule filter = new MyMockFilterRule();
    filter.setString("Test String");
    FilterRunner.filterDirectory(filter, src, dest);

    for (int ix=0; ix<files.length; ix++) {
      File tmpFile = mapFileToOtherRoot(files[ix], src, dest);
      assertFileExists(tmpFile);
      assertReaderMatchesString("Test String", new FileReader(tmpFile));
    }
  }

  //tests for filterSingleFile(FilterRule, File, File)
  public void testThrowsOnNullFilterRule()
      throws FileNotFoundException, IOException {
    try {
      FilterRunner.filterSingleFile((FilterRule)null,
				    new MockFile("blah"),
				    new MockFile("blah2"));
      fail("Calling filterSingleFile with a null FilterRule "
	   +"should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testThrowsOnNullSrc()
      throws FileNotFoundException, IOException {
    try {
      FilterRunner.filterSingleFile(new MockFilterRule(),
				    null,
				    new MockFile("blah2"));
      fail("Calling filterSingleFile with a null source file "
	   +"should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testThrowsOnSrcOtherThanFile()
      throws FileNotFoundException, IOException {
    try {
      FilterRunner.filterSingleFile(new MockFilterRule(),
				    new MockFile("blah"),
				    new MockFile("blah2"));
      fail("Calling filterSingleFile with a src file that returns false "
	   +"on isFile() should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testThrowsOnNullDest()
      throws FileNotFoundException, IOException {
    try {
      FilterRunner.filterSingleFile(new MockFilterRule(),
				    new MockFile("blah"),
				    null);
      fail("Calling filterSingleFile with a null destination file "
	   +"should have thrown");
    } catch (IllegalArgumentException e) {
    }
  }

  public void testSingleFileFiltered()
      throws IOException, FileNotFoundException {
    File tmpDir = getTempDir();
    File dummy = new File(getTempDir(), "dummy");
    assertTrue("Couldn't create "+dummy, dummy.createNewFile());
    File destFile = new File(tmpDir, "testFile");
    MockFilterRule filter = new MockFilterRule();
    Reader reader = new StringReader("Test String");
    filter.setFilteredReader(reader);

    FilterRunner.filterSingleFile(filter, dummy, destFile);
    assertFileExists(destFile);
    assertReaderMatchesString("Test String", new FileReader(destFile));
  }

  public void testSingleFileFilteredToDirectory()
      throws IOException, FileNotFoundException {
    File tmpDir = getTempDir();
    File dummy = new File(getTempDir(), "dummy");
    assertTrue("Couldn't create "+dummy, dummy.createNewFile());
    MockFilterRule filter = new MockFilterRule();
    Reader reader = new StringReader("Test String");
    filter.setFilteredReader(reader);

    FilterRunner.filterSingleFile(filter, dummy, tmpDir);

    File expectedFile = new File(tmpDir, "dummy");
    assertFileExists(expectedFile);
    assertReaderMatchesString("Test String", new FileReader(expectedFile));
  }

  private void assertFileExists(File file) {
    assertTrue("File: "+file+" doesn't exist", file.exists());
  }

  //tests for filterSingleFile(FilterRule, Reader, OutputStream)
//   public void testStreamThrowsOnNullFilterRule() throws IOException {
//     try {
//       FilterRunner.filterSingleFile(null,
// 				    new StringReader("blah"),
// 				    new ByteArrayOutputStream());
//       fail("Calling filterSingleFile with a null FilterRule "
// 	   +"should have thrown");
//     } catch (IllegalArgumentException e) {
//     }
//   }

//   public void testStreamThrowsOnNullInputStream() throws IOException {
//     try {
//       FilterRunner.filterSingleFile(new MockFilterRule(),
// 				    null,
// 				    new ByteArrayOutputStream());
//       fail("Calling filterSingleFile with a null Reader "
// 	   +"should have thrown");
//     } catch (IllegalArgumentException e) {
//     }
//   }

//   public void testStreamThrowsOnNullOutputStream() throws IOException {
//     try {
//       FilterRunner.filterSingleFile(new MockFilterRule(),
// 				    new StringReader("blah"),
// 				    null);
//       fail("Calling filterSingleFile with a null OutputStream "
// 	   +"should have thrown");
//     } catch (IllegalArgumentException e) {
//     }
//   }

//   public void testSingleFilter() throws IOException {
//     InputStream filteredStream =
//       new ReaderInputStream(new StringReader("Test String"));
//     MockFilterRule filter = new MockFilterRule();
//     filter.setFilteredInputStream(filteredStream);
//     StringWriter writer = new StringWriter();
//     FilterRunner.filterSingleFile(filter,
// 				  new FailingReader(),
// 				  new WriterOutputStream(writer));

//     assertEquals("Test String", writer.toString());
//   }


  /**
   * Reader which fails if read or close are called
   */
  public class FailingReader extends Reader {
    public FailingReader() {
    }

    public int read(char[] buf, int len, int off) {
      fail("Shouldn't have been called");
      return -1;
    }

    public void close() {
      fail("Shouldn't have been called");
    }
  }

  public class MyMockFilterRule extends MockFilterRule {
    String str = null;
    public void setString(String str) {
      this.str = str;
    }
    public Reader createFilteredReader(Reader reader) {
      return new StringReader(str);
    }
  }

  public static void main(String[] argv) {
    String[] testCaseList = {TestFilterRunner.class.getName()};
    junit.textui.TestRunner.main(testCaseList);
  }
}
