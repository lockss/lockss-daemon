/*
 * $Id: TestSimulatedContentGenerator.java,v 1.4 2002-10-25 23:41:17 aalto Exp $
 */

/*

Copyright (c) 2002 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.simulated;

import org.lockss.daemon.*;
import org.lockss.util.StreamUtil;
import org.lockss.util.StringUtil;
import org.lockss.test.LockssTestCase;
import java.io.*;
import java.net.*;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedCachedUrl
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestSimulatedContentGenerator extends LockssTestCase {
  private SimulatedContentGenerator scgen;

  public TestSimulatedContentGenerator(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    File tempDir = super.getTempDir();
    scgen = new SimulatedContentGenerator(tempDir.getAbsolutePath());
  }

  public void tearDown() throws Exception {
    super.tearDown();
    if ((scgen!=null) && (scgen.isContentTree())) {
      scgen.deleteContentTree();
    }
  }

  public void testAccessors() {
    scgen = new SimulatedContentGenerator("test");
    scgen.setTreeDepth(5);
    assertEquals("setTreeDepth() failed.", scgen.getTreeDepth(), 5);
    scgen.setNumBranches(12);
    assertEquals("setNumBranches() failed.", scgen.getNumBranches(), 12);
    scgen.setNumFilesPerBranch(15);
    assertEquals("setNumFilesPerBranch() failed.", scgen.getNumFilesPerBranch(), 15);
    scgen.setMaxFilenameLength(25);
    assertEquals("setMaxFilenameLength() failed.", scgen.maxFilenameLength(), 25);
    scgen.setFillOutFilenamesFully(true);
    assertTrue("setFillOutFilenamesFully() failed.", scgen.isFillingOutFilenamesFully());

    assertTrue(!scgen.isAbnormalFile());
    scgen.setAbnormalFile("2,3", 4);
    assertTrue("getAbnormalBranchString() failed.", scgen.getAbnormalBranchString().equals("2,3"));
    assertEquals("getAbnormalFileNumber() failed.", scgen.getAbnormalFileNumber(), 4);
    assertTrue(scgen.isAbnormalFile());

    assertEquals("getFileTypes() failed.", scgen.getFileTypes(), scgen.FILE_TYPE_TXT);
    scgen.setFileTypes(scgen.FILE_TYPE_HTML+scgen.FILE_TYPE_TXT);
    assertEquals("setFileTypes() failed.", scgen.getFileTypes(), scgen.FILE_TYPE_TXT+scgen.FILE_TYPE_HTML);
    assertEquals("getContentRoot() failed.", scgen.getContentRoot(), "test"+File.separator+scgen.ROOT_NAME);
  }

  public void testGetIndexContent() {
    try {
      File tempDir = super.getTempDir();
      File directory = new File(tempDir, "testdir");
      if (!directory.exists()) {
        directory.mkdirs();
      }
      File child = new File(directory, "testfile1.txt");
      if (!child.exists()) {
        FileOutputStream fos = new FileOutputStream(child);
        PrintWriter pw = new PrintWriter(fos);
        pw.print("test file 1");
        pw.flush();
        pw.close();
        fos.close();
      }
      child = new File(directory, "testfile2.html");
      if (!child.exists()) {
        FileOutputStream fos = new FileOutputStream(child);
        PrintWriter pw = new PrintWriter(fos);
        pw.print("<html><body>test file 2</body></html>");
        pw.flush();
        pw.close();
        fos.close();
      }
      child = new File(directory, "testsub1");
      if (!child.exists()) {
        child.mkdir();
      }
      child = new File(directory, "testsub2");
      if (!child.exists()) {
        child.mkdir();
      }

      String content = SimulatedContentGenerator.getIndexContent(directory, "index.html").toLowerCase();
      //test for correct links
      assertTrue(content.indexOf("<a href=\"testfile1.txt\">testfile1.txt</a>")>=0);
      assertTrue(content.indexOf("<a href=\"testfile2.html\">testfile2.html</a>")>=0);
      assertTrue(content.indexOf("<a href=\"testsub1/index.html\">testsub1/index.html</a>")>=0);
      assertTrue(content.indexOf("<a href=\"testsub2/index.html\">testsub2/index.html</a>")>=0);
      //test for no extra links
      assertEquals(4, StringUtil.substringCount(content, "<a href="));
    } catch (Exception e) { System.out.println(e); }
  }
  public void testGetHtmlFileContent() {
    String expectedStr = "<HTML><HEAD><TITLE>testfile</TITLE></HEAD><BODY>\n";
    // assumes the value of NORMAL_FILE_CONTENT is unchanged
    expectedStr += "This is file 1, depth 2, branch 3.";
    expectedStr += "\n</BODY></HTML>";
    assertTrue(SimulatedContentGenerator.getHtmlFileContent("testfile", 1, 2, 3, false).equals(expectedStr));
  }
  public void testGetFileContent() {
    // assumes the value of NORMAL_FILE_CONTENT is unchanged
    String expectedStr = "This is file 1, depth 2, branch 3.";
    assertTrue(SimulatedContentGenerator.getFileContent(1, 2, 3, false).equals(expectedStr));
    // assumes the value of ABNORMAL_FILE_CONTENT is unchanged
    expectedStr = "This is abnormal file 3, depth 1, branch 2.";
    assertTrue(SimulatedContentGenerator.getFileContent(3, 1, 2, true).equals(expectedStr));
  }
  public void testGetFileName() {
    String expectedStr = SimulatedContentGenerator.FILE_PREFIX + "11.txt";
    assertTrue(SimulatedContentGenerator.getFileName(11, SimulatedContentGenerator.FILE_TYPE_TXT).equals(expectedStr));
    expectedStr = SimulatedContentGenerator.FILE_PREFIX + "12.html";
    assertTrue(SimulatedContentGenerator.getFileName(12, SimulatedContentGenerator.FILE_TYPE_HTML).equals(expectedStr));
    expectedStr = SimulatedContentGenerator.FILE_PREFIX + "13.pdf";
    assertTrue(SimulatedContentGenerator.getFileName(13, SimulatedContentGenerator.FILE_TYPE_PDF).equals(expectedStr));
    expectedStr = SimulatedContentGenerator.FILE_PREFIX + "14.jpg";
    assertTrue(SimulatedContentGenerator.getFileName(14, SimulatedContentGenerator.FILE_TYPE_JPEG).equals(expectedStr));
    expectedStr = SimulatedContentGenerator.BRANCH_PREFIX + "15";
    assertTrue(SimulatedContentGenerator.getDirectoryName(15).equals(expectedStr));
  }

  public void testTreeExistence() {
    scgen.setTreeDepth(0);
    scgen.setNumFilesPerBranch(0);
    scgen.generateContentTree();
    assertTrue(scgen.isContentTree());
    File rootFile = new File(scgen.getContentRoot());
    assertTrue("root non-existent or non-directory.",
                rootFile.exists() && rootFile.isDirectory());
  }

  public void testTreeDepth() {
    scgen.setTreeDepth(2);
    scgen.setNumBranches(2);
    scgen.setNumFilesPerBranch(0);
    scgen.generateContentTree();
    String depth2Name = scgen.getContentRoot() + File.separator +
                        scgen.getDirectoryName(1) + File.separator +
                        scgen.getDirectoryName(2);
    File depth2Dir = new File(depth2Name);
    assertTrue("Depth 2 directory not found.", depth2Dir.exists() && depth2Dir.isDirectory());
    File[] depth2Children = depth2Dir.listFiles();
    for (int i=0; i<depth2Children.length; i++) {
      System.out.println(depth2Children[i].getName());
    }

    assertTrue("depth2 has subdirectories.", (depth2Children.length == 1));
  }

  public void testFileNumbers() {
    scgen.setTreeDepth(1);
    scgen.setNumBranches(1);
    scgen.setNumFilesPerBranch(2);
    scgen.setFileTypes(scgen.FILE_TYPE_TXT);
    scgen.generateContentTree();
    File rootFile = new File(scgen.getContentRoot());
    File[] rootChildren = rootFile.listFiles();
    assertTrue("root has wrong number of children.", (rootChildren.length == 4));
    String subName = scgen.getContentRoot() + File.separator +
                     scgen.getDirectoryName(1);
    File subDir = new File(subName);
    assertTrue("Directory not found.", subDir.exists() && subDir.isDirectory());
    File[] children = subDir.listFiles();
    assertTrue("dir has wrong number of children.", (children.length == 3));
  }

  public void testTextFileContent() {
    scgen.setTreeDepth(0);
    scgen.setNumFilesPerBranch(1);
    scgen.setFileTypes(scgen.FILE_TYPE_TXT);
    scgen.generateContentTree();
    String childName = scgen.getContentRoot() + File.separator +
                       scgen.getFileName(1, scgen.FILE_TYPE_TXT);
    File child = new File(childName);
    assertTrue("File not found.", child.exists() && !child.isDirectory());
    try {
      FileInputStream fis = new FileInputStream(child);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      StreamUtil.copy(fis, baos);
      fis.close();
      String content = new String(baos.toByteArray());
      baos.close();
      String expectedContent = scgen.getFileContent(1, 0, 0, false);
      assertTrue("content incorrect.", content.equals(expectedContent));
    } catch (Exception e) { System.out.println(e); }
  }

  public void testHtmlFileContent() {
    scgen.setTreeDepth(0);
    scgen.setNumFilesPerBranch(1);
    scgen.setFileTypes(scgen.FILE_TYPE_HTML);
    scgen.generateContentTree();
    String childName = scgen.getContentRoot() + File.separator +
                       scgen.getFileName(1, scgen.FILE_TYPE_HTML);
    File child = new File(childName);
    assertTrue("File not found.", child.exists() && !child.isDirectory());
    try {
      FileInputStream fis = new FileInputStream(child);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      StreamUtil.copy(fis, baos);
      fis.close();
      String content = new String(baos.toByteArray());
      baos.close();
      String expectedContent = scgen.getHtmlFileContent(scgen.getFileName(1, scgen.FILE_TYPE_HTML), 1, 0, 0, false);
      assertTrue("content incorrect.", content.equals(expectedContent));
    } catch (Exception e) { System.out.println(e); }
  }

  public void testAbnormalFileContent() {
    scgen.setTreeDepth(2);
    scgen.setNumBranches(2);
    scgen.setNumFilesPerBranch(2);
    scgen.setAbnormalFile("2,1", 2);
    scgen.setFileTypes(scgen.FILE_TYPE_TXT);
    scgen.generateContentTree();
    String depth2Name = scgen.getContentRoot() + File.separator +
                        scgen.getDirectoryName(2) + File.separator +
                        scgen.getDirectoryName(1) + File.separator +
                        scgen.getFileName(1, scgen.FILE_TYPE_TXT);
    File depth2file = new File(depth2Name);
    assertTrue("Depth 2 file not found.", depth2file.exists() && !depth2file.isDirectory());
    try {
      FileInputStream fis = new FileInputStream(depth2file);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      StreamUtil.copy(fis, baos);
      fis.close();
      String content = new String(baos.toByteArray());
      baos.close();
      String expectedContent = scgen.getFileContent(1, 2, 1, false);
      assertTrue("content incorrect.", content.equals(expectedContent));
      depth2Name = scgen.getContentRoot() + File.separator +
                   scgen.getDirectoryName(2) + File.separator +
                   scgen.getDirectoryName(1) + File.separator +
                   scgen.getFileName(2, scgen.FILE_TYPE_TXT);
      depth2file = new File(depth2Name);
      assertTrue("Depth 2 file not found.", depth2file.exists() && !depth2file.isDirectory());
      fis = new FileInputStream(depth2file);
      baos = new ByteArrayOutputStream();
      StreamUtil.copy(fis, baos);
      fis.close();
      content = new String(baos.toByteArray());
      baos.close();
      expectedContent = scgen.getFileContent(2, 2, 1, true);
      assertTrue("abnormal content incorrect.", content.equals(expectedContent));
    } catch (Exception e) { System.out.println(e); }
  }

  public void testFileTypes() {
    scgen.setTreeDepth(0);
    scgen.setNumBranches(0);
    scgen.setNumFilesPerBranch(2);
    scgen.setFileTypes(scgen.FILE_TYPE_TXT+scgen.FILE_TYPE_HTML);
    scgen.generateContentTree();
    File rootFile = new File(scgen.getContentRoot());
    File[] rootChildren = rootFile.listFiles();
    assertTrue("root has wrong number of children.", (rootChildren.length == 5));
    String depth2Name = scgen.getContentRoot() + File.separator +
                        scgen.getFileName(1, scgen.FILE_TYPE_TXT);
    File child = new File(depth2Name);
    assertTrue("Text file 1 not found.", child.exists() && !child.isDirectory());
    depth2Name = scgen.getContentRoot() + File.separator +
                 scgen.getFileName(1, scgen.FILE_TYPE_HTML);
    child = new File(depth2Name);
    assertTrue("Html file 1 not found.", child.exists() && !child.isDirectory());
    depth2Name = scgen.getContentRoot() + File.separator +
                 scgen.getFileName(2, scgen.FILE_TYPE_TXT);
    child = new File(depth2Name);
    assertTrue("Text file 2 not found.", child.exists() && !child.isDirectory());
    depth2Name = scgen.getContentRoot() + File.separator +
                 scgen.getFileName(2, scgen.FILE_TYPE_HTML);
    child = new File(depth2Name);
    assertTrue("Html file 2 not found.", child.exists() && !child.isDirectory());

  }

  public void testTreeDelete() {
    scgen.setTreeDepth(2);
    scgen.setNumBranches(1);
    scgen.setNumFilesPerBranch(2);
    scgen.generateContentTree();
    scgen.deleteContentTree();
    assertTrue(!scgen.isContentTree());
  }
}
