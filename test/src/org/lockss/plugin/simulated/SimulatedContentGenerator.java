/*
 * $Id: SimulatedContentGenerator.java,v 1.6 2003-02-27 21:53:17 tal Exp $
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
import java.util.Arrays;
import org.lockss.util.StringUtil;
import org.lockss.test.*;

/**
 * This is a convenience class which takes care of handling the content
 * tree itself
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class SimulatedContentGenerator {
/**
 * Content of a generated text or html file.  Values are substituted for
 * the %Xs.
 */
  public static final String NORMAL_FILE_CONTENT =
    "This is file %1, depth %2, branch %3.";
  /**
   * Content of a generated text or html file with 'abnormal' status.
   * Values are substituted for the %Xs.
   */
  public static final String ABNORMAL_FILE_CONTENT =
    "This is abnormal file %1, depth %2, branch %3.";
/**
 * Name of top directory in which the content is generated.
 */
  public static final String ROOT_NAME = "simcontent";
  /**
   * The name prefix for generated files.  The local file number is appended.
   */
  public static final String FILE_PREFIX = "file";
  /**
   * The name prefix for generated sub-directories.  The local branch
   * number is appended.
   */
  public static final String BRANCH_PREFIX = "branch";
  /**
   * The name of the 'index' file in each directory which lists the
   * children as html links for crawling.
   */
  public static final String INDEX_NAME = "index.html";

 /**
  * File-type value for text files.  Independent bitwise from the other
  * file-types.
  */
  public static final int FILE_TYPE_TXT = 1;
  /**
   * File-type value for html files.  Independent bitwise from the other
   * file-types.
   */
  public static final int FILE_TYPE_HTML = 2;
  /**
   * File-type value for pdf files.  Independent bitwise from the other
   * file-types.
   */
  public static final int FILE_TYPE_PDF = 4;
  /**
   * File-type value for jpeg files.  Independent bitwise from the other
   * file-types.
   */
  public static final int FILE_TYPE_JPEG = 8;
  /**
   * File-type value for binary files.  Independent bitwise from the other
   * file-types.
   */
  public static final int FILE_TYPE_BIN = 16;

  // how deep the tree extends
  private int treeDepth = 4;
  // number of branches at each level
  private int numBranches = 4;
  // number of files per node
  private int numFilesPerBranch = 10;
  private int maxFilenameLength = 20;
  private int binaryFileSize = 256;
  private boolean fillOutFilenames = false;
  private int fileTypes = FILE_TYPE_TXT;

  private boolean isAbnormalFile = false;
  private String abnormalBranchStr = "";
  private int abnormalFileNum = 0;

  private String contentRootParent = "";
  private String contentRoot;
/**
 * @param rootPath path where the content directory will be generated
 */
  public SimulatedContentGenerator(String rootPath) {
    contentRootParent = rootPath;
    if (contentRootParent.length()>0 &&
        !contentRootParent.endsWith(File.separator)) {
      contentRootParent += File.separator;
    }
    contentRoot = contentRootParent + ROOT_NAME;
  }

  /**
   * Depth 0 generates only the root directory.
   * @return depth of the generated tree
   */
  public int getTreeDepth() { return treeDepth; }
  /**
   * Depth 0 generates only the root directory.
   * @param newDepth new depth for generated tree
   */
  public void setTreeDepth(int newDepth) { treeDepth = newDepth; }
  /**
   * @return number of branches per internal node
   */
  public int getNumBranches() { return numBranches; }
  /**
   * @param newNumBranches new number of branches per internal node
   */
  public void setNumBranches(int newNumBranches) {
    numBranches = newNumBranches;
  }
  /**
   * @return number of files per internal node
   */
  public int getNumFilesPerBranch() { return numFilesPerBranch; }
  /**
   * @param newNumFiles new number of files per internal node
   */
  public void setNumFilesPerBranch(int newNumFiles) {
    numFilesPerBranch = newNumFiles;
  }
  /**
   * @return the size binary files will be created as
   */
  public int getBinaryFileSize() { return binaryFileSize; }
  /**
   * @param newBinarySize new binary file size
   */
  public void setBinaryFileSize(int newBinarySize) {
    binaryFileSize = newBinarySize;
  }
  /**
   * @return maximum length for a file name
   */
  public int maxFilenameLength() { return maxFilenameLength; }
  /**
   * @param newMaxLength new maximum length for a file name
   */
  public void setMaxFilenameLength(int newMaxLength) {
    maxFilenameLength = newMaxLength;
  }
  /**
   * Determine whether or not to expand all file names to maximum length.
   * @param fillOut expand to max length
   */
  public void setFillOutFilenamesFully(boolean fillOut) {
    fillOutFilenames = fillOut;
  }
  /**
   * Returns whether or not set to expand all file names to maximum length.
   * @return is expanding to max length
   */
  public boolean isFillingOutFilenamesFully() { return fillOutFilenames; }
  /**
   * @return is set to create abnormal file
   */
  public boolean isAbnormalFile() { return isAbnormalFile; }
  /**
   * Returns 'branch path' leading to abnormal file.  Format is "1,2,3..."
   * for ROOT/branch1/branch2/branch3/...  Empty string refers to root.
   * @return branch path
   */
  public String getAbnormalBranchString() { return abnormalBranchStr; }
  /**
   * @return local file number for abnormal file.
   */
  public int getAbnormalFileNumber() { return abnormalFileNum; }
  /**
   * Sets the parameters to create an abnormal file.
   * Format for branch path is "1,2,3..." for ROOT/branch1/branch2/branch3/...
   * Empty string refers to root.
   *
   * @param branchStr branch path to file location
   * @param fileNum local file number
   */
  public void setAbnormalFile(String branchStr, int fileNum) {
    abnormalBranchStr = branchStr;
    abnormalFileNum = fileNum;
    isAbnormalFile = true;
  }
  /**
   * Gets the file types which will be generated.
   * Uses a bitwise AND technique to store the various file types
   * in a single int (i.e. FILE_TYPE_TXT + FILE_TYPE_JPEG)
   * @return file types to be created
   */
  public int getFileTypes() { return fileTypes; }
  /**
   * Sets the file types to be generated.
   * Uses a bitwise AND technique to store the various file types
   * in a single int (i.e. FILE_TYPE_TXT + FILE_TYPE_JPEG)
   * @param types file types to be created
   */
  public void setFileTypes(int types) { fileTypes = types; }
  /**
   * @return location of content root
   */
  public String getContentRoot() { return contentRoot; }

  /**
   * Tests whether the root of the generated tree exists.
   * @return content tree exists
   */
  public boolean isContentTree() {
    File treeRoot = new File(contentRoot);
    return (treeRoot.exists() && treeRoot.isDirectory());
  }
  /**
   * Deletes the generated content tree.
   */
  public void deleteContentTree() {
    FileUtil.delTree(new File(contentRoot));
  }
/**
 * Generates a content tree using the current parameters.  Depth of 0 is
 * root (and files) only.  Depth >0 is number of sub-levels.  Each
 * directory contains the set number of files X number of file types (so
 * for numFiles=2, types=TXT + HTML, each directory would contain 2 txt
 * files, 2 html files, the index file, and any subdirectories).
 */
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
      generateFile(treeRoot, jj, 0, 0,
		   (alterFile && (jj==getAbnormalFileNumber())));
    }
    generateIndexFile(treeRoot);
  }
  private void recurseGenerateBranch(File parentDir, int branchNum,
				     int depth, boolean onAbnormalPath) {
    // generates this branch, its files, and its subbranches (if any)
    String branchName = getDirectoryName(branchNum);
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
      generateFile(branchFile, jj, depth, branchNum,
		   (alterFile && (jj==getAbnormalFileNumber())));
    }
    generateIndexFile(branchFile);
  }

  private void generateIndexFile(File parentDir) {
    try {
      String filename = INDEX_NAME;
      File file = new File(parentDir, filename);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      String file_content = getIndexContent(parentDir, filename);
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }

  private void generateFile(File parentDir, int fileNum, int depth,
			    int branchNum, boolean isAbnormal) {
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
    if ((getFileTypes() & FILE_TYPE_BIN) > 0) {
      createBinaryFile(parentDir, fileNum, binaryFileSize);
    }
  }

  private void createTxtFile(File parentDir, int fileNum, int depth,
			     int branchNum, boolean isAbnormal) {
    try {
      String fileName = getFileName(fileNum, FILE_TYPE_TXT);
      File file = new File(parentDir, fileName);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      String file_content =
	getFileContent(fileNum, depth, branchNum, isAbnormal);
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }

  private void createHtmlFile(File parentDir, int fileNum, int depth,
			      int branchNum, boolean isAbnormal) {
    try {
      String filename = getFileName(fileNum, FILE_TYPE_HTML);
      File file = new File(parentDir, filename);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      String file_content = getHtmlFileContent(filename, fileNum, depth,
					       branchNum, isAbnormal);
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }
  private void createPdfFile(File parentDir, int fileNum, int depth,
			     int branchNum, boolean isAbnormal) {
    try {
      String fileName = getFileName(fileNum, FILE_TYPE_PDF);
      File file = new File(parentDir, fileName);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      String file_content = "";
// XXX open local pdf file, copy to pw
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }
  private void createJpegFile(File parentDir, int fileNum, int depth,
			      int branchNum, boolean isAbnormal) {
    try {
      String fileName = getFileName(fileNum, FILE_TYPE_JPEG);
      File file = new File(parentDir, fileName);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      String file_content = "";
// XXX open local jpeg file, copy to pw
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }


  private void createBinaryFile(File parentDir, int fileNum, int size) {
      try {
        String fileName = getFileName(fileNum, FILE_TYPE_BIN);
        File file = new File(parentDir, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte)1);
        fos.write(bytes);
        fos.close();
      } catch (Exception e) { System.err.println(e); }
  }

  /**
   * Generates standard text content for a file (text or html types only).
   * @param fileNum local file number
   * @param depth file depth (0 for root)
   * @param branchNum local branch number (0 for root)
   * @param isAbnormal whether or not to generate abnormal content
   * @return file content
   */
  public static String getFileContent(int fileNum, int depth,
				      int branchNum, boolean isAbnormal) {
    String file_content = NORMAL_FILE_CONTENT;
    if (isAbnormal) file_content = ABNORMAL_FILE_CONTENT;
    file_content = StringUtil.replaceString(file_content, "%1", ""+fileNum);
    file_content = StringUtil.replaceString(file_content, "%2", ""+depth);
    file_content = StringUtil.replaceString(file_content, "%3", ""+branchNum);
    return file_content;
  }

  /**
   * Generates standard html content for an html file.
   * Standard text content is included as the body of the html.
   * @param filename name of file (used in title)
   * @param fileNum local file number
   * @param depth file depth (0 for root)
   * @param branchNum local branch number (0 for root)
   * @param isAbnormal whether or not to generate abnormal content
   * @return file content
   */
  public static String getHtmlFileContent(String filename, int fileNum,
                                          int depth, int branchNum,
					  boolean isAbnormal) {
    String file_content =
      "<HTML><HEAD><TITLE>" + filename + "</TITLE></HEAD><BODY>\n";
    file_content += getFileContent(fileNum, depth, branchNum, isAbnormal);
    file_content += "\n</BODY></HTML>";
    return file_content;
  }

  /**
   * Generates index file for a directory, in html form with each sibling
   * file or sub-directory index file as a link.  This is to allow crawling.
   *
   * @param directory to generate index content for
   * @param filename the name of the index file
   * @return index file content
   */

  public static String getIndexContent(File directory, String filename) {
    if ((directory==null) || (!directory.exists()) ||
	(!directory.isDirectory())) {
      return "";
    }
    String fullName = directory.getName() + File.separator + filename;
    String file_content =
      "<HTML><HEAD><TITLE>" + fullName + "</TITLE></HEAD><BODY>";
    file_content += "<B>"+fullName+"</B>";

    File[] children = directory.listFiles();

    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      String subLink = child.getName();
      if (child.isDirectory()) {
        subLink += File.separator + SimulatedContentGenerator.INDEX_NAME;
      }
      file_content += "<BR><A HREF=\"" + subLink + "\">" + subLink + "</A>";
    }
    file_content += "</BODY></HTML>";
    return file_content;
  }

  /**
   * Generates a standard file name.
   * @param fileNum local file number
   * @param fileType file type (single type only)
   * @return standard file name
   */
  public static String getFileName(int fileNum, int fileType) {
    String fileName = FILE_PREFIX + fileNum;
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
      case FILE_TYPE_BIN:
        fileName += ".bin";
        break;
    }
    return fileName;
  }
  /**
   * Generates a standard directory name.
   * @param branchNum local branch number
   * @return standard file name
   */
  public static String getDirectoryName(int branchNum) {
    return BRANCH_PREFIX + branchNum;
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
