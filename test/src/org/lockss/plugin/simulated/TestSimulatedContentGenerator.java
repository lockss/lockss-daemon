/*
 * $Id: TestSimulatedContentGenerator.java,v 1.2 2002-10-24 02:17:43 aalto Exp $
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

import junit.framework.TestCase;
import org.lockss.daemon.*;
import org.lockss.util.StreamUtil;
import java.io.*;

/**
 * This is the test class for org.lockss.plugin.simulated.SimulatedCachedUrl
 *
 * @author  Emil Aalto
 * @version 0.0
 */


public class TestSimulatedContentGenerator extends TestCase {
  public TestSimulatedContentGenerator(String msg) {
    super(msg);
  }

  public void testAccessors() {
    SimulatedContentGenerator scgen = new SimulatedContentGenerator("test");
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
    System.out.println("All accessors test correctly.");
  }

 /* public void testContentTree() {
    //XXX get root dir from system props
  //XXX bug: will fail if more than one file type
    SimulatedContentGenerator scgen = new SimulatedContentGenerator("");
    scgen.setTreeDepth(2);
    scgen.setNumBranches(2);
    scgen.setNumFilesPerBranch(2);
    scgen.setAbnormalFile("2,1", 2);
    if (scgen.isContentTree()) {
      scgen.deleteContentTree();
      assertTrue("deleteContentTree() failed.", !scgen.isContentTree());
    }
    scgen.generateContentTree();
    assertTrue("generateContentTree() failed.", scgen.isContentTree());
    File rootFile = new File(scgen.getContentRoot());
    assertTrue("root non-existent or non-directory.",
                rootFile.exists() && rootFile.isDirectory());
    File[] rootChildren = rootFile.listFiles();
    assertTrue("root has wrong number of children.",
              (rootChildren.length == (scgen.getNumBranches() + scgen.getNumFilesPerBranch()+1)));
    String depth2Name = scgen.getContentRoot() + File.separator +
                        scgen.getFileName(1, true, -1) + File.separator +
                        scgen.getFileName(2, true, -1);
    File depth2Dir = new File(depth2Name);
    assertTrue("Depth 2 directory not found.", depth2Dir.exists() && depth2Dir.isDirectory());
    File[] depth2Children = depth2Dir.listFiles();
    assertTrue("depth2 has wrong number of children.",
              (depth2Children.length == scgen.getNumFilesPerBranch()+1));
    depth2Name += File.separator + scgen.getFileName(2, false, scgen.FILE_TYPE_TXT);
    File depth2file = new File(depth2Name);
    assertTrue("Depth 2 file not found.", depth2file.exists() && !depth2file.isDirectory());
    try {
      FileInputStream fis = new FileInputStream(depth2file);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      StreamUtil.copy(fis, baos);
      fis.close();
      String content = new String(baos.toByteArray());
      baos.close();
      String expectedContent = scgen.getFileContent(2, 2, 2, false);
      assertTrue("content incorrect.", content.equals(expectedContent));
      String depth2Name = scgen.getContentRoot() + File.separator +
                        scgen.getFileName(2, true, -1) + File.separator +
                        scgen.getFileName(1, true, -1);
      depth2Name += File.separator + scgen.getFileName(2, false, scgen.FILE_TYPE_TXT);
      File depth2file = new File(depth2Name);
      assertTrue("Depth 2 file not found.", depth2file.exists() && !depth2file.isDirectory());
      fis = new FileInputStream(depth2file);
      baos = new ByteArrayOutputStream();
      StreamUtil.copy(fis, baos);
      fis.close();
      content = new String(baos.toByteArray());
      baos.close();
      expectedContent = scgen.getFileContent(2, 2, 1, true);
      assertTrue("abnormal content incorrect.", content.equals(expectedContent));
      System.out.println("All content tested correctly.");
    } catch (Exception e) { System.out.println(e); }
  }

  */
}
