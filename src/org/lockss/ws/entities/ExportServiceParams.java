/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.entities;

/**
 * A wrapper for the parameters used to request a ExportService to download the
 * content.
 * 
 * @author Ahmed AlSum
 */
public class ExportServiceParams {
  private String auid;
  private TypeEnum fileType;
  private boolean isCompress = true;
  private boolean isExcludeDirNodes = true;
  private FilenameTranslationEnum xlateFilenames;
  private String filePrefix;
  private long maxSize = -1; // The default is there is no maximum file size
  private int maxVersions = -1;

  public ExportServiceParams() {
    super();
    fileType = TypeEnum.WARC_RESPONSE;
    xlateFilenames = FilenameTranslationEnum.XLATE_NONE;
    filePrefix = "lockss_export";
  }

  /**
   * @return the auid
   */
  public String getAuid() {
    return auid;
  }

  /**
   * @param auid
   *          to set the AU Id
   */
  public void setAuid(String auid) {
    this.auid = auid;
  }

  /**
   * @return the fileType
   */
  public TypeEnum getFileType() {
    return fileType;
  }

  /**
   * @param fileType
   *          the fileType to set
   */
  public void setFileType(TypeEnum fileType) {
    this.fileType = fileType;
  }

  /**
   * @return the isCompress
   */
  public boolean isCompress() {
    return isCompress;
  }

  /**
   * @param isCompress
   *          the isCompress to set
   */
  public void setCompress(boolean isCompress) {
    this.isCompress = isCompress;
  }

  /**
   * @return the isExcludeDirNodes
   */
  public boolean isExcludeDirNodes() {
    return isExcludeDirNodes;
  }

  /**
   * @param isExcludeDirNodes
   *          the isExcludeDirNodes to set
   */
  public void setExcludeDirNodes(boolean isExcludeDirNodes) {
    this.isExcludeDirNodes = isExcludeDirNodes;
  }

  /**
   * @return the xlateFilenames
   */
  public FilenameTranslationEnum getXlateFilenames() {
    return xlateFilenames;
  }

  /**
   * @param xlateFilenames
   *          the xlateFilenames to set
   */
  public void setXlateFilenames(FilenameTranslationEnum xlateFilenames) {
    this.xlateFilenames = xlateFilenames;
  }

  /**
   * @return the filePrefix
   */
  public String getFilePrefix() {
    return filePrefix;
  }

  /**
   * @param filePrefix
   *          the filePrefix to set
   */
  public void setFilePrefix(String filePrefix) {
    this.filePrefix = filePrefix;
  }

  /**
   * @return the maxSize
   */
  public long getMaxSize() {
    return maxSize;
  }

  /**
   * @param maxSize
   *          the maxSize to set
   */
  public void setMaxSize(long maxSize) {
    this.maxSize = maxSize;
  }

  /**
   * Provides The maximum number of versions included, for content files that
   * have older versions. (ARC and WARC only).
   * 
   * @return the maxVersions
   */
  public int getMaxVersions() {
    return maxVersions;
  }

  /**
   * @param maxVersions
   *          the maxVersions to set
   */
  public void setMaxVersions(int maxVersions) {
    this.maxVersions = maxVersions;
  }

  public enum TypeEnum {
    WARC_RESPONSE, WARC_RESOURCE, ARC_RESPONSE, ARC_RESOURCE, ZIP
  }

  public enum FilenameTranslationEnum {
    XLATE_NONE, XLATE_WINDOWS, XLATE_MAC
  }
}
