/*
 * $Id: SimulatedContentGenerator.java,v 1.1 2002-10-23 23:43:05 aalto Exp $
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

import java.io.*;
import org.lockss.util.StringUtil;

/**
 * This is a convenience class which takes care of handling the content tree itself
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class SimulatedContentGenerator {
  public static final String NORMAL_FILE_CONTENT = "This is file %1, depth %2, branch %3.";
  public static final String ABNORMAL_FILE_CONTENT = "This is abnormal file %1, depth %2, branch %3.";
  public static final String ROOT_NAME = "simcontent";
  public static final String FILE_PREFIX = "file";
  public static final String BRANCH_PREFIX = "branch";
  public static final String INDEX_NAME = "index.html";

  public static final int FILE_TYPE_TXT = 1;
  public static final int FILE_TYPE_HTML = 2;
  public static final int FILE_TYPE_PDF = 4;
  public static final int FILE_TYPE_JPEG = 8;

  // how deep the tree extends
  private int treeDepth = 4;
  // number of branches at each level
  private int numBranches = 4;
  // number of files per node
  private int numFilesPerBranch = 10;
  private int maxFilenameLength = 20;
  private boolean fillOutFilenames = false;
  private int fileTypes = FILE_TYPE_TXT;

  private boolean isAbnormalFile = false;
  private String abnormalBranchStr = "";
  private int abnormalFileNum = 0;

  private String contentRootParent = "";
  private String contentRoot;

  public SimulatedContentGenerator(String rootPath) {
    contentRootParent = rootPath;
    if (contentRootParent.length()>0 &&
        contentRootParent.charAt(contentRootParent.length()-1)!=File.separatorChar) {
      contentRootParent += File.separator;
    }
    contentRoot = contentRootParent + ROOT_NAME;
  }

  // accessors
  public int getTreeDepth() { return treeDepth; }
  public void setTreeDepth(int newDepth) { treeDepth = newDepth; }
  public int getNumBranches() { return numBranches; }
  public void setNumBranches(int newNumBranches) { numBranches = newNumBranches; }
  public int getNumFilesPerBranch() { return numFilesPerBranch; }
  public void setNumFilesPerBranch(int newNumFiles) { numFilesPerBranch = newNumFiles; }
  public int maxFilenameLength() { return maxFilenameLength; }
  public void setMaxFilenameLength(int newMaxLength) { maxFilenameLength = newMaxLength; }
  public void setFillOutFilenamesFully(boolean fillOut) { fillOutFilenames = fillOut; }
  public boolean isFillingOutFilenamesFully() { return fillOutFilenames; }
  public boolean isAbnormalFile() { return isAbnormalFile; }
  public String getAbnormalBranchString() { return abnormalBranchStr; }
  public int getAbnormalFileNumber() { return abnormalFileNum; }
  public void setAbnormalFile(String branchStr, int fileNum) {
    abnormalBranchStr = branchStr;
    abnormalFileNum = fileNum;
    isAbnormalFile = true;
  }
  public int getFileTypes() { return fileTypes; }
  public void setFileTypes(int types) { fileTypes = types; }
  public String getContentRoot() { return contentRoot; }

  public boolean isContentTree() {
    // check to see if the content tree root exists
    File treeRoot = new File(contentRoot);
    return (treeRoot.exists() && treeRoot.isDirectory());
  }
  public void deleteContentTree() {
    recurseDeleteFileTree(new File(contentRoot));
  }
  private void recurseDeleteFileTree(File file) {
    if (file.exists()) {
      if (file.isDirectory()) {
        File[] files = file.listFiles();
        for (int ii=0; ii<files.length; ii++) {
          recurseDeleteFileTree(files[ii]);
        }
      }
      file.delete();
    }
  }
  public void generateContentTree() {
    // make an appropriate file tree
    File treeRoot = new File(contentRoot);
    if (!treeRoot.exists()) {
      treeRoot.mkdirs();
    }
    // test abnormal status
    boolean alterFile = isAlteredLevel(0);
    int branchPath = 0;
    if (isAbnormalFile() && !alterFile) {
      branchPath = getNextAbnormalBranch(0);
    }
    if (getTreeDepth()>0) {
      for (int ii=1; ii<=getNumBranches(); ii++) {
        recurseGenerateBranch(treeRoot, ii, 1, (ii==branchPath));
      }
    }
    for (int jj=1; jj<=getNumFilesPerBranch(); jj++) {
      if (jj==1) generateIndexFile(treeRoot, 0);
      generateFile(treeRoot, jj, 0, 0, (alterFile && (jj==getAbnormalFileNumber())));
    }
  }
  private void recurseGenerateBranch(File parentDir, int branchNum, int depth, boolean onAbnormalPath) {
    // generates this branch, its files, and its subbranches (if any)
    String branchName = getFileName(branchNum, true, -1);
    File branchFile = new File(parentDir, branchName);
    if (!branchFile.exists()) {
      branchFile.mkdirs();
    }
    boolean alterFile = false;
    int branchPath = 0;
    if (onAbnormalPath) {
      alterFile = isAlteredLevel(depth);
      branchPath = getNextAbnormalBranch(depth);
    }
    if (depth<getTreeDepth()) {
      for (int ii=1; ii<=getNumBranches(); ii++) {
        recurseGenerateBranch(branchFile, ii, depth+1, (ii==branchPath));
      }
    }
    for (int jj=1; jj<=getNumFilesPerBranch(); jj++) {
      if (jj==1) generateIndexFile(branchFile, depth);
      generateFile(branchFile, jj, depth, branchNum, (alterFile && (jj==getAbnormalFileNumber())));
    }
  }

  private void generateIndexFile(File parentDir, int depth) {
    try {
      String filename = INDEX_NAME;
      File file = new File(parentDir, filename);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      filename = parentDir.getPath() + File.separator + filename;
      String file_content = getIndexContent(parentDir.getPath(), filename, depth,
                               getTreeDepth(), getNumBranches(), getNumFilesPerBranch(), getFileTypes());
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }

  private void generateFile(File parentDir, int fileNum, int depth, int branchNum, boolean isAbnormal) {
    // generate said file, with correct content
    // generate one type for each necessary
    if ((getFileTypes() & FILE_TYPE_TXT) > 0) {
      createTxtFile(parentDir, fileNum, depth, branchNum, isAbnormal);
    }
    if ((getFileTypes() & FILE_TYPE_HTML) > 0) {
      createHtmlFile(parentDir, fileNum, depth, branchNum, isAbnormal);
    }
    if ((getFileTypes() & FILE_TYPE_PDF) > 0) {
      createPdfFile(parentDir, fileNum, depth, branchNum, isAbnormal);
    }
    if ((getFileTypes() & FILE_TYPE_JPEG) > 0) {
      createJpegFile(parentDir, fileNum, depth, branchNum, isAbnormal);
    }
  }

  private void createTxtFile(File parentDir, int fileNum, int depth, int branchNum, boolean isAbnormal) {
    try {
      String fileName = getFileName(fileNum, false, FILE_TYPE_TXT);
      File file = new File(parentDir, fileName);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      String file_content = getFileContent(fileNum, depth, branchNum, isAbnormal);
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }

  private void createHtmlFile(File parentDir, int fileNum, int depth, int branchNum, boolean isAbnormal) {
    try {
      String filename = getFileName(fileNum, false, FILE_TYPE_HTML);
      File file = new File(parentDir, filename);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      filename = parentDir.getPath() + File.separator + filename;
      String file_content = getHtmlFileContent(filename, fileNum, depth, branchNum, isAbnormal);
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }
  private void createPdfFile(File parentDir, int fileNum, int depth, int branchNum, boolean isAbnormal) {
    try {
      String fileName = getFileName(fileNum, false, FILE_TYPE_PDF);
      File file = new File(parentDir, fileName);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      String file_content = "";
// XXX open local pdf file, copy to pw
      pw.println(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }
  private void createJpegFile(File parentDir, int fileNum, int depth, int branchNum, boolean isAbnormal) {
    try {
      String fileName = getFileName(fileNum, false, FILE_TYPE_JPEG);
      File file = new File(parentDir, fileName);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      String file_content = "";
// XXX open local jpeg file, copy to pw
      pw.println(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }

  public static String getFileContent(int fileNum, int depth, int branchNum, boolean isAbnormal) {
    String file_content = NORMAL_FILE_CONTENT;
    if (isAbnormal) file_content = ABNORMAL_FILE_CONTENT;
    file_content = StringUtil.replaceString(file_content, "%1", ""+fileNum);
    file_content = StringUtil.replaceString(file_content, "%2", ""+depth);
    file_content = StringUtil.replaceString(file_content, "%3", ""+branchNum);
    return file_content;
  }

  public static String getHtmlFileContent(String filename, int fileNum,
                                          int depth, int branchNum, boolean isAbnormal) {
    String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
    file_content += getFileContent(fileNum, depth, branchNum, isAbnormal);
    file_content += "\n</BODY></HTML>";
    return file_content;
  }

  public static String getIndexContent(String parentPath, String filename, int curDepth,
                          int maxDepth, int numBranches, int numFiles, int fileTypes) {
    String file_content = "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>";
    file_content += "<B>"+filename+"</B>";

    if (curDepth < maxDepth) {
      for (int ii=1; ii<=numBranches; ii++) {
        String subDirName = getFileName(ii, true, 0) + File.separator + INDEX_NAME;
        file_content += "<BR><A HREF=\"" + subDirName + "\">"+ subDirName + "</A>";
      }
    }

    for (int jj=1; jj<=numFiles; jj++) {
      String subFileName = "";
      if ((fileTypes & FILE_TYPE_TXT) > 0) {
        subFileName = getFileName(jj, false, FILE_TYPE_TXT);
        file_content += "<BR><A HREF=\"" + subFileName + "\">" + subFileName + "</A>";
      }
      if ((fileTypes & FILE_TYPE_HTML) > 0) {
        subFileName = getFileName(jj, false, FILE_TYPE_HTML);
        file_content += "<BR><A HREF=\"" + subFileName + "\">" + subFileName + "</A>";
      }
      if ((fileTypes & FILE_TYPE_PDF) > 0) {
        subFileName = getFileName(jj, false, FILE_TYPE_PDF);
        file_content += "<BR><A HREF=\"" + subFileName + "\">" + subFileName + "</A>";
      }
      if ((fileTypes & FILE_TYPE_JPEG) > 0) {
        subFileName = getFileName(jj, false, FILE_TYPE_JPEG);
        file_content += "<BR><A HREF=\"" + subFileName + "\">" + subFileName + "</A>";
      }
    }
    file_content += "</BODY></HTML>";
    return file_content;
  }


  public static String getFileName(int fileNum, boolean isDirectory, int fileType) {
    String fileName = "";
    if (isDirectory) {
      fileName += BRANCH_PREFIX + fileNum;
    } else {
      fileName += FILE_PREFIX + fileNum;
      switch (fileType) {
        case FILE_TYPE_TXT:
          fileName += ".txt";
          break;
        case FILE_TYPE_HTML:
          fileName += ".html";
          break;
        case FILE_TYPE_PDF:
          fileName += ".pdf";
          break;
        case FILE_TYPE_JPEG:
          fileName += ".jpg";
          break;
      }
    }
    return fileName;
  }

  private boolean isAlteredLevel(int depth) {
    if (!isAbnormalFile()) return false;
    String branchStr = getAbnormalBranchString();
    if ((depth==0)&&(branchStr.equals(""))) return true;
    else {
      int depthCount = 1;
      while (branchStr.indexOf(",")>0) {
        if (branchStr.length()<=(branchStr.indexOf(",")+1)) break;
        branchStr = branchStr.substring(branchStr.indexOf(",")+1);
        depthCount++;
      }
      return (depth==depthCount);
    }
  }

  private int getNextAbnormalBranch(int depth) {
    if (!isAbnormalFile()) return 0;
    String branchStr = getAbnormalBranchString();
    if (branchStr.equals("")) return 0;
    String branchNum = "";
    int depthCount = 0;
    while (true) {
      int index = branchStr.indexOf(",");
      if (index>=0) {
        branchNum = branchStr.substring(0, index);
        if (branchStr.length()<=index+1) branchStr = "";
        else branchStr = branchStr.substring(index+1);
      } else {
        branchNum = branchStr;
        branchStr = "";
      }
      if (depthCount==depth) {
        try {
          return Integer.parseInt(branchNum);
        } catch (Exception e) { return 0; }
      } else depthCount++;
      if (branchStr.equals("")) return 0;
    }
  }
}
