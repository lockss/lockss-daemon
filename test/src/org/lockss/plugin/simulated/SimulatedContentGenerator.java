/*
 * $Id: SimulatedContentGenerator.java,v 1.26 2007-09-24 18:37:14 dshr Exp $
 */

/*

Copyright (c) 2000-2007 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.*;
import java.text.*;
import org.lockss.util.*;
import org.lockss.test.*;
import org.lockss.config.*;
import org.lockss.plugin.base.*;
import org.lockss.crawler.*;
import org.lockss.daemon.*;

/**
 * This is a convenience class which takes care of handling the content
 * tree itself
 *
 * @author  Emil Aalto
 * @version 0.0
 */

public class SimulatedContentGenerator {
  /**
   * Content of a generated text file.  Values are substituted for
   * the %Xs.
   */
  public static final String NORMAL_TXT_FILE_CONTENT =
    "This is file %1, depth %2, branch %3.";
  /**
   * Content of a generated text file with 'abnormal' status.
   * Values are substituted for the %Xs.
   */
  public static final String ABNORMAL_TXT_FILE_CONTENT =
    "This is abnormal file %1, depth %2, branch %3.";

  /**
   * Content of a generated html file.  Values are substituted for
   * the %Xs.
   */
  public static final String NORMAL_HTML_FILE_CONTENT =
    "This is file %1, depth %2, branch %3.<br>" +
    "<!-- comment -->    Citation String   foobar<br>" +
    "<script>(defun fact (n) (cond ((= n 0) 1) (t (fact (sub1 n)))))</script>";

  /**
   * Content of a generated html file with 'abnormal' status.
   * Values are substituted for the %Xs.
   */
  public static final String ABNORMAL_HTML_FILE_CONTENT =
    ABNORMAL_TXT_FILE_CONTENT;

  /**
   * Artificial content of a directory, if required.
   * Path is substituted for the %1.
   */
  public static final String NORMAL_DIR_CONTENT =
    "This is directory %1, depth %2.";
  /**
   * Artificial content of a directory 'abnormal' status.
   * Path is substituted for the %1.
   */
  public static final String ABNORMAL_DIR_CONTENT =
    "This is abnormal directory %1, depth %2.";

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
   * The name of the file in each directory which contains the 'content' for
   * that directory, if any.
   */
  public static final String DIR_CONTENT_NAME = "branch_content";


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
  private int maxFilenameLength = -1; // vals <= 0 are ignored.
  private int binaryFileSize = 256;
  private boolean fillOutFilenames = false;
  private boolean oddBranchesHaveContent = false;
  private int fileTypes = FILE_TYPE_TXT;

  private boolean isAbnormalFile = false;
  private String abnormalBranchStr = "";
  private int abnormalFileNum = 0;

  // Formatter to pad filename numbers with up to three leading zeros.
  private static NumberFormat fileNameFormatter = new DecimalFormat("000");

  protected String contentRoot;
  protected static Logger logger = Logger.getLogger("SimulatedContentGenerator");

  /**
   * @param rootPath path where the content directory will be generated
   */
  protected SimulatedContentGenerator(String rootPath) {
    String contentRootParent = rootPath;
    if (contentRootParent.length()>0 &&
        !contentRootParent.endsWith(File.separator)) {
      contentRootParent += File.separator;
    }
    contentRoot = contentRootParent + ROOT_NAME;
  }

  static SimulatedContentGenerator getInstance(String rootPath) {
    SimulatedContentGenerator ret = null;
    logger.debug3("SimulatedContentGenerator.getInstance(" + rootPath + ")");
    boolean arc = CurrentConfig.getBooleanParam("org.lockss.plugin.simulated.SimulatedContentGenerator.doArcFile", false);
    boolean zip = CurrentConfig.getBooleanParam("org.lockss.plugin.simulated.SimulatedContentGenerator.doZipFile", false);
    boolean tar = CurrentConfig.getBooleanParam("org.lockss.plugin.simulated.SimulatedContentGenerator.doTarFile", false);
    logger.debug3("SimulatedContentGenerator: arc " + arc + " zip " + zip +
		  " tar " + tar);
    if (arc) {
      boolean actual = CurrentConfig.getBooleanParam("org.lockss.plugin.simulated.SimulatedContentGenerator.actualArcFile", false);
      if (actual) {
	ret = new ActualArcContentGenerator(rootPath);
      } else {
	ret = new SimulatedArcContentGenerator(rootPath);
      }
    } else if (zip) {
      boolean actual = CurrentConfig.getBooleanParam("org.lockss.plugin.simulated.SimulatedContentGenerator.actualZipFile", false);
      if (actual) {
	ret = new ActualZipContentGenerator(rootPath);
      } else {
	ret = new SimulatedZipContentGenerator(rootPath);
      }
    } else if (tar) {
      boolean actual = CurrentConfig.getBooleanParam("org.lockss.plugin.simulated.SimulatedContentGenerator.actualTarFile", false);
      if (actual) {
	ret = new ActualTarContentGenerator(rootPath);
      } else {
	ret = new SimulatedTarContentGenerator(rootPath);
      }
    } else {
      ret = new SimulatedContentGenerator(rootPath);
    }
    return ret;
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
   * Sets whether or not all the odd branches should have content.
   * @param haveContent whether or not to make content in odd branches
   */
  public void setOddBranchesHaveContent(boolean haveContent) {
    oddBranchesHaveContent = haveContent;
  }

  /**
   * Whether or not all the odd branches have content.
   * @return true if odd branches have content
   */
  public boolean oddBranchesHaveContent() {
    return oddBranchesHaveContent;
  }

  /**
   * @return is set to create abnormal file
   */
  public boolean isAbnormalFile() { return isAbnormalFile; }
  /**
   * Returns 'branch path' leading to abnormal content.  Format is "1,2,3..."
   * for ROOT/branch1/branch2/branch3/...  Empty string refers to root.
   * @return branch path
   */
  public String getAbnormalBranchString() { return abnormalBranchStr; }
  /**
   * Returns the local file number for abnormal file.  For the branch itself
   * to have abnormal content, use '-1'.
   * @return local file number for abnormal file.
   */
  public int getAbnormalFileNumber() { return abnormalFileNum; }
  /**
   * Sets the parameters to create an abnormal file.
   * Format for branch path is "1,2,3..." for ROOT/branch1/branch2/branch3/...
   * Empty string refers to root.  For the directory itself to have abnormal
   * content, use '-1' as the file number.  The root directory cannot have
   * content.
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
   * @return max file name length
   */
  public int getMaxFilenameLength() { return maxFilenameLength; }

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
    logger.debug("deleting " + contentRoot);
    FileUtil.delTree(new File(contentRoot));
  }
  /**
   * Generates a content tree using the current parameters.  Depth of 0 is
   * root (and files) only.  Depth >0 is number of sub-levels.  Each
   * directory contains the set number of files X number of file types (so
   * for numFiles=2, types=TXT + HTML, each directory would contain 2 txt
   * files, 2 html files, the index file, and any subdirectories).
   * @return the filename of the tree root
   */
  public String generateContentTree() {
    // make an appropriate file tree
    File treeRoot = new File(contentRoot);
    if (!treeRoot.exists()) {
      logger.debug3("Creating root at " + contentRoot);
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
    generateIndexFile(treeRoot, LockssPermission.LOCKSS_PERMISSION_STRING);
    return treeRoot.toString();
  }

  private void recurseGenerateBranch(File parentDir, int branchNum,
				     int depth, boolean onAbnormalPath) {
    // generates this branch, its files, and its subbranches (if any)
    boolean alterFile = false;
    boolean alterDir = false;
    int branchPath = 0;
    if (onAbnormalPath) {
      if (isAlteredLevel(depth)) {
        alterFile = (getAbnormalFileNumber() > 0);
        alterDir = (getAbnormalFileNumber() == -1);
      }
      branchPath = getNextAbnormalBranch(depth);
    }

    String branchName = getDirectoryName(branchNum);
    File branchFile = new File(parentDir, branchName);
    if (!branchFile.exists()) {
      logger.debug3("Creating branch at " + branchName);
      branchFile.mkdirs();
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

    if ((oddBranchesHaveContent() && (branchNum%2 == 1)) || alterDir) {
      generateDirContentFile(branchFile, depth, alterDir);
    }
    generateIndexFile(branchFile, null);
  }

  private void generateDirContentFile(File parentDir, int depth,
                                      boolean abnormal) {
    try {
      String filename = DIR_CONTENT_NAME;
      File file = new File(parentDir, filename);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      logger.debug3("Creating file at " + file.getAbsolutePath());
      String file_content = getBranchContent(parentDir.getName(), depth,
                                             abnormal);
      pw.print(file_content);
      pw.flush();
      pw.close();
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }


  private void generateIndexFile(File parentDir, String permission) {
    try {
      String filename = INDEX_NAME;
      File file = new File(parentDir, filename);
      FileOutputStream fos = new FileOutputStream(file);
      PrintWriter pw = new PrintWriter(fos);
      logger.debug3("Creating index file at " + file.getAbsolutePath());
      String file_content = getIndexContent(parentDir, filename, permission);
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
      logger.debug3("Creating TXT file at " + file.getAbsolutePath());
      String file_content =
	getTxtContent(fileNum, depth, branchNum, isAbnormal);
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
      logger.debug3("Creating HTML file at " + file.getAbsolutePath());
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
      logger.debug3("Creating PDF file at " + file.getAbsolutePath());
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
      logger.debug3("Creating JPEG file at " + file.getAbsolutePath());
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
      logger.debug3("Creating BIN file at " + file.getAbsolutePath());
      byte[] bytes = new byte[size];
      Arrays.fill(bytes, (byte)1);
      fos.write(bytes);
      fos.close();
    } catch (Exception e) { System.err.println(e); }
  }

  /**
   * Generates standard artifical content for a branch.
   * @param name branch name
   * @param depth the depth
   * @param isAbnormal whether or not to generate abnormal content
   * @return branch content
   */
  public static String getBranchContent(String name, int depth,
                                        boolean isAbnormal) {
    String branch_content = NORMAL_DIR_CONTENT;
    if (isAbnormal) branch_content = ABNORMAL_DIR_CONTENT;
    branch_content = StringUtil.replaceString(branch_content, "%1", name);
    branch_content = StringUtil.replaceString(branch_content, "%2", ""+depth);
    return branch_content;
  }

  /**
   * Generates standard text content for a file (text type only).
   * @param fileNum local file number
   * @param depth file depth (0 for root)
   * @param branchNum local branch number (0 for root)
   * @param isAbnormal whether or not to generate abnormal content
   * @return file content
   */
  public static String getTxtContent(int fileNum, int depth,
				     int branchNum, boolean isAbnormal) {
    String file_content = NORMAL_TXT_FILE_CONTENT;
    if (isAbnormal) file_content = ABNORMAL_TXT_FILE_CONTENT;
    file_content = StringUtil.replaceString(file_content, "%1", ""+fileNum);
    file_content = StringUtil.replaceString(file_content, "%2", ""+depth);
    file_content = StringUtil.replaceString(file_content, "%3", ""+branchNum);
    return file_content;
  }

  /**
   * Generates standard content for an html file.
   * @param fileNum local file number
   * @param depth file depth (0 for root)
   * @param branchNum local branch number (0 for root)
   * @param isAbnormal whether or not to generate abnormal content
   * @return file content
   */
  public static String getHtmlContent(int fileNum, int depth,
				      int branchNum, boolean isAbnormal) {
    String file_content = NORMAL_HTML_FILE_CONTENT;
    if (isAbnormal) file_content = ABNORMAL_HTML_FILE_CONTENT;
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
    file_content += getHtmlContent(fileNum, depth, branchNum, isAbnormal);
    file_content += "\n</BODY></HTML>";
    return file_content;
  }

  /**
   * Generates index file for a directory, in html form with each sibling
   * file or sub-directory index file as a link.  This is to allow crawling.
   *
   * @param directory to generate index content for
   * @param filename the name of the index file
   * @param permission permission string
   * @return index file content
   */

  public static String getIndexContent(File directory,
                                       String filename,
                                       String permission) {
    if ((directory==null) || (!directory.exists()) ||
	(!directory.isDirectory())) {
      return "";
    }
    String fullName = directory.getName() + File.separator + filename;
    String file_content =
      "<HTML><HEAD><TITLE>" + fullName + "</TITLE></HEAD><BODY>";
    file_content += "<B>"+fullName+"</B>";

    if(permission != null) {
      file_content += "<BR>" + permission;
    }
    File[] children = directory.listFiles();

    Arrays.sort(children);    // must sort to ensure index page always same

    for (int ii=0; ii<children.length; ii++) {
      File child = children[ii];
      String subLink = child.getName();
      if (child.isDirectory()) {
        subLink += File.separator + SimulatedContentGenerator.INDEX_NAME;
      }
      if (subLink.equals(DIR_CONTENT_NAME)) {
        subLink = ".";
      }
      file_content += "<BR><A HREF=\"" + FileUtil.sysIndepPath(subLink) +
	"\">" + subLink + "</A>";
    }
    // insert a link to parent to ensure there are some duplicate links
    file_content += "<BR><A HREF=\"../index.html\">" + "parent" + "</A>";
    // insert a link to a fixed excluded URL to ensure there are some
    // duplicate excluded links
    file_content += "<BR><A HREF=\"/xxxexcluded.html\">" + "excluded" + "</A>";
    // insert a link to a fixed failing URL to ensure there are some
    // duplicate failing links
    file_content += "<BR><A HREF=\"/xxxfail.html\">" + "fail" + "</A>";
    file_content += "</BODY></HTML>";
    return file_content;
  }

  /**
   * Generates a standard file name.
   * @param fileNum local file number
   * @param fileType file type (single type only)
   * @return standard file name
   */
  public String getFileName(int fileNum, int fileType) {
    StringBuffer fileName = new StringBuffer(fileNameFormatter.format(fileNum));

    if (maxFilenameLength > 0) {
      char[] buf = new char[maxFilenameLength];
      // Generate maxFilenameLength letters in alphabetical order
      // to append.
      for (int i = 0; i < maxFilenameLength; i++) {
	buf[i] = (char)((i % 26) + 'a');
      }
      fileName.append(buf);
    } else {
      fileName.append(FILE_PREFIX);
    }

    switch (fileType) {
    case FILE_TYPE_TXT:
      fileName.append(".txt");
      break;
    case FILE_TYPE_HTML:
      fileName.append( ".html");
      break;
    case FILE_TYPE_PDF:
      fileName.append( ".pdf");
      break;
    case FILE_TYPE_JPEG:
      fileName.append( ".jpg");
      break;
    case FILE_TYPE_BIN:
      fileName.append( ".bin");
      break;
    }
    return fileName.toString();
  }

  public static String getDirectoryName(int branchNum) {
    return BRANCH_PREFIX + branchNum;
  }

  public static String getDirectoryContentFile(String dirPath) {
    return dirPath + File.separator + DIR_CONTENT_NAME;
  }

  /**
   * Converts a specification to a url.
   * @param fileLoc Represents the branch number and depth, separated by a
   * comma
   * @param fileNum Actual file number
   * @return the url
   */
  public String getUrlFromLoc(String fileLoc, String fileNum) {
    StringTokenizer tok = new StringTokenizer(fileLoc,",");
    String path = "/";
    while (tok.hasMoreTokens()) {
      int loc = Integer.parseInt(tok.nextToken());
      path += getDirectoryName(loc) + "/";
    }
    path += getFileName(Integer.parseInt(fileNum),getFileTypes());
    return path;
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
