/*
 * $Id$
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

package org.lockss.plugin.simulated;

import java.io.*;
import java.net.*;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.test.*;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedCachedUrl
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestSimulatedContentGenerator extends LockssTestCase {
  private SimulatedContentGenerator scgen;
  private String tempDirPath;

  public TestSimulatedContentGenerator(String msg) {
    super(msg);
  }

  public void setUp() throws Exception {
    super.setUp();
    tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    scgen = new SimulatedContentGenerator(tempDirPath);
    scgen.setMaxFilenameLength(-1);
  }

  public void testAccessors() {
    scgen = new SimulatedContentGenerator("test");
    scgen.setTreeDepth(5);
    assertEquals("setTreeDepth() failed.", 5, scgen.getTreeDepth());
    scgen.setNumBranches(12);
    assertEquals("setNumBranches() failed.", 12, scgen.getNumBranches());
    scgen.setNumFilesPerBranch(15);
    assertEquals("setNumFilesPerBranch() failed.", 15,
                 scgen.getNumFilesPerBranch());
    scgen.setBinaryFileSize(128);
    assertEquals("setBinaryFileSize() failed.", 128,
                 scgen.getBinaryFileSize());
    scgen.setMaxFilenameLength(25);
    assertEquals("setMaxFilenameLength() failed.", 25,
                 scgen.maxFilenameLength());
    scgen.setFillOutFilenamesFully(true);
    assertTrue("setFillOutFilenamesFully() failed.",
               scgen.isFillingOutFilenamesFully());

    assertFalse(scgen.isAbnormalFile());
    scgen.setAbnormalFile("2,3", 4);
    assertEquals("getAbnormalBranchString() failed.", "2,3",
                 scgen.getAbnormalBranchString());
    assertEquals("getAbnormalFileNumber() failed.", 4,
                 scgen.getAbnormalFileNumber());
    assertTrue(scgen.isAbnormalFile());

    assertEquals("getFileTypes() failed.", SimulatedContentGenerator.FILE_TYPE_TXT,
                 scgen.getFileTypes());
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_HTML+SimulatedContentGenerator.FILE_TYPE_TXT);
    assertEquals("setFileTypes() failed.",
                 SimulatedContentGenerator.FILE_TYPE_TXT+SimulatedContentGenerator.FILE_TYPE_HTML,
                 scgen.getFileTypes());
    assertEquals("getContentRoot() failed.",
                 "test"+File.separator+SimulatedContentGenerator.ROOT_NAME,
                 scgen.getContentRoot());
  }

  public void testGetIndexContent() throws Exception {
    File tempDir = getTempDir();
    File directory = new File(tempDir, "testdir");
    makeDir(directory);
    makeFile(new File(directory, "branch_content"), "content");
    makeFile(new File(directory, "file1.txt"), "test file 1");
    makeFile(new File(directory, "file2.html"), "<html><body>test file 2</body></html>");
    makeDir(new File(directory, "branch1"));
    makeDir(new File(directory, "branch2"));

    String content = scgen.getIndexContent(directory,
        "index.html", null).toLowerCase();
    //test for correct links
    assertTrue(content.indexOf("<a href=\"file1.txt\">file1.txt</a>")>=0);
    assertTrue(content.indexOf("<a href=\"file2.html\">file2.html</a>")>=0);
    assertTrue(content.indexOf("<a href=\"branch1/index.html\">"+
                               FileUtil.sysDepPath("branch1/index.html")+
                               "</a>")>=0);
    assertTrue(content.indexOf("<a href=\"branch2/index.html\">"+
                               FileUtil.sysDepPath("branch2/index.html")+
                               "</a>")>=0);
    assertTrue(content.indexOf("<a href=\".\">.</a>")>=0);
    //test for no extra links
    assertEquals(8, StringUtil.countOccurences(content, "<a href="));
  }

  public void testGetHtmlFileContent() {
    String expectedStr = "<HTML><HEAD><TITLE>testfile</TITLE></HEAD><BODY>\n";
    expectedStr += SimulatedContentGenerator.NORMAL_HTML_FILE_CONTENT;
    expectedStr += "\n</BODY></HTML>";
    expectedStr = StringUtil.replaceString(expectedStr, "%", "");
    assertEquals(expectedStr,
                 scgen.getHtmlFileContent("testfile",
                 1, 2, 3, false));
  }

  public void testGetFileContent() {
    // assumes the value of NORMAL_TXT_FILE_CONTENT is unchanged
    String expectedStr = "This is file 1, depth 2, branch 3.";
    assertEquals(expectedStr,
                 scgen.getTxtContent(
                 1, 2, 3, false));
    // assumes the value of ABNORMAL_TXT_FILE_CONTENT is unchanged
    expectedStr = "This is abnormal file 3, depth 1, branch 2.";
    assertEquals(expectedStr, scgen.getTxtContent(
        3, 1, 2, true));
  }
  public void testGetFileName() {
    String expectedStr;
    expectedStr = "011" + SimulatedContentGenerator.FILE_PREFIX + ".txt";
    assertEquals(expectedStr, scgen.getFileName(11,
        SimulatedContentGenerator.FILE_TYPE_TXT));
    expectedStr = "012" + SimulatedContentGenerator.FILE_PREFIX + ".html";
    assertEquals(expectedStr, scgen.getFileName(12,
        SimulatedContentGenerator.FILE_TYPE_HTML));
    expectedStr = "013" + SimulatedContentGenerator.FILE_PREFIX + ".pdf";
    assertEquals(expectedStr, scgen.getFileName(13,
        SimulatedContentGenerator.FILE_TYPE_PDF));
    expectedStr = "014" + SimulatedContentGenerator.FILE_PREFIX + ".jpg";
    assertEquals(expectedStr, scgen.getFileName(14,
        SimulatedContentGenerator.FILE_TYPE_JPEG));
    expectedStr = SimulatedContentGenerator.BRANCH_PREFIX + "15";
    assertEquals(expectedStr, scgen.getDirectoryName(15));
    expectedStr = "016" + SimulatedContentGenerator.FILE_PREFIX + ".xml";
    assertEquals(expectedStr, scgen.getFileName(16,
        SimulatedContentGenerator.FILE_TYPE_XML));
    expectedStr = "017" + SimulatedContentGenerator.FILE_PREFIX + ".xhtml";
    assertEquals(expectedStr, scgen.getFileName(17,
        SimulatedContentGenerator.FILE_TYPE_XHTML));

    // Test with maxFilenameLength set.

  }

  public void testFileLocation() throws IOException {
    tempDirPath += "simcontent/";
    File testDir = new File(tempDirPath);
    assertFalse(testDir.exists());
    scgen.setTreeDepth(1);
    scgen.setNumFilesPerBranch(1);
    assertEquals(testDir.toString(), scgen.generateContentTree());
    assertTrue(testDir.exists());
    String testStr = tempDirPath + "001file.txt";
    testDir = new File(testStr);
    assertTrue(testDir.exists());
    testStr = tempDirPath + "index.html";
    testDir = new File(testStr);
    assertTrue(testDir.exists());
    testStr = tempDirPath + "branch1";
    testDir = new File(testStr);
    assertTrue(testDir.exists());
    testStr = tempDirPath + "branch1/001file.txt";
    testDir = new File(testStr);
    assertTrue(testDir.exists());
    testStr = tempDirPath + "branch1/index.html";
    testDir = new File(testStr);
    assertTrue(testDir.exists());
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
                        scgen.getDirectoryName(1) +
                        File.separator +
                        scgen.getDirectoryName(2);
    File depth2Dir = new File(depth2Name);
    assertTrue("Depth 2 directory not found.",
               depth2Dir.exists() && depth2Dir.isDirectory());
    File[] depth2Children = depth2Dir.listFiles();
    assertEquals("depth2 has subdirectories.", 1, depth2Children.length);
  }

  public void testFileNumbers() {
    scgen.setTreeDepth(1);
    scgen.setNumBranches(1);
    scgen.setNumFilesPerBranch(2);
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_TXT);
    scgen.generateContentTree();
    File rootFile = new File(scgen.getContentRoot());
    File[] rootChildren = rootFile.listFiles();
    assertEquals("root has wrong number of children.", 4, rootChildren.length);
    String subName = scgen.getContentRoot() + File.separator +
                     scgen.getDirectoryName(1);
    File subDir = new File(subName);
    assertTrue("Directory not found.", subDir.exists() && subDir.isDirectory());
    File[] children = subDir.listFiles();
    assertEquals("dir has wrong number of children.", 3, children.length);
  }

  public void testTextFileContent() throws Exception {
    scgen.setTreeDepth(0);
    scgen.setNumFilesPerBranch(1);
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_TXT);
    scgen.generateContentTree();
    String childName = scgen.getContentRoot() + File.separator +
                       scgen.getFileName(1, SimulatedContentGenerator.FILE_TYPE_TXT);
    File child = new File(childName);
    assertTrue("File not found.", child.exists() && !child.isDirectory());
    String content = getFileContent(child);
    String expectedContent = scgen.getTxtContent(1, 0, 0, false);
    assertEquals("content incorrect.", expectedContent, content);
  }

  public void testHtmlFileContent() throws Exception {
    scgen.setTreeDepth(0);
    scgen.setNumFilesPerBranch(1);
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_HTML);
    scgen.generateContentTree();
    String childName = scgen.getContentRoot() + File.separator +
                       scgen.getFileName(1, SimulatedContentGenerator.FILE_TYPE_HTML);
    File child = new File(childName);
    assertTrue("File not found.", child.exists() && !child.isDirectory());
    String content = getFileContent(child);
    String expectedContent =
      scgen.getHtmlFileContent(scgen.getFileName(1,
        SimulatedContentGenerator.FILE_TYPE_HTML), 1, 0, 0, false);
    assertEquals("content incorrect.", expectedContent, content);
  }

  public void testXmlFileContent() throws Exception {
    scgen.setTreeDepth(0);
    scgen.setNumFilesPerBranch(1);
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_XML);
    scgen.generateContentTree();
    String childName = scgen.getContentRoot() + File.separator +
                       scgen.getFileName(1, SimulatedContentGenerator.FILE_TYPE_XML);
    File child = new File(childName);
    assertTrue("File not found.", child.exists() && !child.isDirectory());
    String content = getFileContent(child);
    String expectedContent =
      scgen.getXmlFileContent(scgen.getFileName(1,
        SimulatedContentGenerator.FILE_TYPE_XML), 1, 0, 0, false);
    assertEquals("content incorrect.", expectedContent, content);
  }

  public void testBinaryFileSize() throws Exception {
    scgen.setTreeDepth(0);
    scgen.setBinaryFileSize(128);
    scgen.setNumFilesPerBranch(1);
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_BIN);
    scgen.generateContentTree();
    String childName = scgen.getContentRoot() + File.separator +
                       scgen.getFileName(1, SimulatedContentGenerator.FILE_TYPE_BIN);
    File child = new File(childName);
    assertTrue("File not found.", child.exists() && !child.isDirectory());
    assertEquals(128, child.length());
  }

  public void testAbnormalFileContent() throws Exception {
    scgen.setTreeDepth(2);
    scgen.setNumBranches(2);
    scgen.setNumFilesPerBranch(2);
    scgen.setAbnormalFile("2,1", 2);
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_TXT);
    scgen.generateContentTree();
    String depth2Name = scgen.getContentRoot() + File.separator +
        scgen.getDirectoryName(2) + File.separator +
        scgen.getDirectoryName(1) + File.separator +
        scgen.getFileName(1, SimulatedContentGenerator.FILE_TYPE_TXT);
    File depth2file = new File(depth2Name);
    assertTrue("Depth 2 file not found.",
               depth2file.exists() && !depth2file.isDirectory());
    String content = getFileContent(depth2file);
    String expectedContent = scgen.getTxtContent(1, 2, 1, false);
    assertTrue("content incorrect.", content.equals(expectedContent));
    depth2Name = scgen.getContentRoot() + File.separator +
        scgen.getDirectoryName(2) + File.separator +
        scgen.getDirectoryName(1) + File.separator +
        scgen.getFileName(2, SimulatedContentGenerator.FILE_TYPE_TXT);
    depth2file = new File(depth2Name);
    assertTrue("Depth 2 file not found.",
               depth2file.exists() && !depth2file.isDirectory());
    content = getFileContent(depth2file);
    expectedContent = scgen.getTxtContent(2, 2, 1, true);
    assertEquals("abnormal content incorrect.", expectedContent, content);
  }

  public void testFileTypes() {
    scgen.setTreeDepth(0);
    scgen.setNumBranches(0);
    scgen.setNumFilesPerBranch(2);
    scgen.setFileTypes(SimulatedContentGenerator.FILE_TYPE_TXT+SimulatedContentGenerator.FILE_TYPE_HTML);
    scgen.generateContentTree();
    File rootFile = new File(scgen.getContentRoot());
    File[] rootChildren = rootFile.listFiles();
    assertEquals("root has wrong number of children.", 5, rootChildren.length);
    String depth2Name = scgen.getContentRoot() + File.separator +
                        scgen.getFileName(1, SimulatedContentGenerator.FILE_TYPE_TXT);
    File child = new File(depth2Name);
    assertTrue("Text file 1 not found.",
               child.exists() && !child.isDirectory());
    depth2Name = scgen.getContentRoot() + File.separator +
                 scgen.getFileName(1, SimulatedContentGenerator.FILE_TYPE_HTML);
    child = new File(depth2Name);
    assertTrue("Html file 1 not found.",
               child.exists() && !child.isDirectory());
    depth2Name = scgen.getContentRoot() + File.separator +
                 scgen.getFileName(2, SimulatedContentGenerator.FILE_TYPE_TXT);
    child = new File(depth2Name);
    assertTrue("Text file 2 not found.",
               child.exists() && !child.isDirectory());
    depth2Name = scgen.getContentRoot() + File.separator +
                 scgen.getFileName(2, SimulatedContentGenerator.FILE_TYPE_HTML);
    child = new File(depth2Name);
    assertTrue("Html file 2 not found.",
               child.exists() && !child.isDirectory());

  }

  public void testBranchContent() throws Exception {
    scgen.setTreeDepth(2);
    scgen.setNumBranches(2);
    scgen.setNumFilesPerBranch(0);
    scgen.setOddBranchesHaveContent(true);
    scgen.setAbnormalFile("1,2", -1);
    scgen.generateContentTree();

    String branchName = scgen.getContentRoot() + File.separator +
        scgen.getDirectoryName(1);
    File child = new File(branchName, SimulatedContentGenerator.DIR_CONTENT_NAME);
    assertTrue(child.exists());
    FileInputStream fis = new FileInputStream(child);
    ByteArrayOutputStream baos = new ByteArrayOutputStream(30);
    StreamUtil.copy(fis, baos);
    fis.close();
    assertEquals(scgen.getBranchContent(scgen.getDirectoryName(1),
                                                            1, false),
                 baos.toString());

    branchName = scgen.getContentRoot() + File.separator +
        scgen.getDirectoryName(2);
    child = new File(branchName, SimulatedContentGenerator.DIR_CONTENT_NAME);
    assertFalse(child.exists());

    branchName = scgen.getContentRoot() + File.separator +
        scgen.getDirectoryName(1) + File.separator +
        scgen.getDirectoryName(2);
    child = new File(branchName, SimulatedContentGenerator.DIR_CONTENT_NAME);
    assertTrue(child.exists());
    fis = new FileInputStream(child);
    baos = new ByteArrayOutputStream(30);
    StreamUtil.copy(fis, baos);
    fis.close();
    assertEquals(scgen.getBranchContent(scgen.getDirectoryName(2),
                                                            2, true),
                 baos.toString());

    branchName = scgen.getContentRoot() + File.separator +
        scgen.getDirectoryName(2) + File.separator +
        scgen.getDirectoryName(1);
    child = new File(branchName, SimulatedContentGenerator.DIR_CONTENT_NAME);
    assertTrue(child.exists());
    fis = new FileInputStream(child);
    baos = new ByteArrayOutputStream(30);
    StreamUtil.copy(fis, baos);
    fis.close();
    assertEquals(scgen.getBranchContent(scgen.getDirectoryName(1),
                                                            2, false),
                 baos.toString());
  }

  public void testTreeDelete() {
    scgen.setTreeDepth(2);
    scgen.setNumBranches(1);
    scgen.setNumFilesPerBranch(2);
    scgen.generateContentTree();
    assertTrue(scgen.isContentTree());
    scgen.deleteContentTree();
    assertEquals(isKeepTempFiles(), scgen.isContentTree());
  }

  private void makeFile(File file, String content) throws Exception {
    if (!file.exists() && (content!=null)) {
      OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
      InputStream is = new StringInputStream(content);
      StreamUtil.copy(is, os);
      os.close();
      is.close();
    }
  }

  private void makeDir(File file) {
    if (!file.exists()) {
      file.mkdirs();
    }
  }

  private String getFileContent(File file) throws Exception {
    return StringUtil.fromFile(file);
//     FileInputStream fis = new FileInputStream(file);
//     ByteArrayOutputStream baos = new ByteArrayOutputStream();
//     StreamUtil.copy(fis, baos);
//     fis.close();
//     String content = new String(baos.toByteArray());
//     baos.close();
//     return content;
  }
}
