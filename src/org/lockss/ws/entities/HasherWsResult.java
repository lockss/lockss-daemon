/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.Arrays;
import javax.activation.DataHandler;

/**
 * A wrapper for the result of a hash operation.
 */
public class HasherWsResult {
  private Long startTime;
  private String recordFileName;
  private DataHandler recordFileDataHandler;
  private String blockFileName;
  private DataHandler blockFileDataHandler;
  private byte[] hashResult;
  private String errorMessage;
  private String status;
  private Long bytesHashed;
  private Integer filesHashed;
  private Long elapsedTime;

  /**
   * Provides the instant when the hashing operation started.
   * 
   * @return a Long with the instant in milliseconds since the start of 1970.
   */
  public Long getStartTime() {
    return startTime;
  }
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  /**
   * Provides the name of the record file.
   * 
   * @return a String with the name of the record file.
   */
  public String getRecordFileName() {
    return recordFileName;
  }
  public void setRecordFileName(String recordFileName) {
    this.recordFileName = recordFileName;
  }

  /**
   * Provides the content of the record file.
   * 
   * @return a DataHandler through which to obtain the content of the record
   *         file.
   */
  public DataHandler getRecordFileDataHandler() {
    return recordFileDataHandler;
  }
  public void setRecordFileDataHandler(DataHandler recordFileDataHandler) {
    this.recordFileDataHandler = recordFileDataHandler;
  }

  /**
   * Provides the name of the block file.
   * 
   * @return a String with the name of the block file.
   */
  public String getBlockFileName() {
    return blockFileName;
  }
  public void setBlockFileName(String blockFileName) {
    this.blockFileName = blockFileName;
  }

  /**
   * Provides the content of the block file.
   * 
   * @return a DataHandler through which to obtain the content of the block
   *         file.
   */
  public DataHandler getBlockFileDataHandler() {
    return blockFileDataHandler;
  }
  public void setBlockFileDataHandler(DataHandler blockFileDataHandler) {
    this.blockFileDataHandler = blockFileDataHandler;
  }

  /**
   * Provides the result of the hash.
   * 
   * @return a byte[] with the result of the hash.
   */
  public byte[] getHashResult() {
    return hashResult;
  }
  public void setHashResult(byte[] hashResult) {
    this.hashResult = hashResult;
  }

  /**
   * Provides the error message.
   * 
   * @return a String with the error message, if any.
   */
  public String getErrorMessage() {
    return errorMessage;
  }
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  /**
   * Provides the status. <br />
   * The possible values are Init, Starting, Running, Done, Error and
   * RequestError.
   * 
   * @return a String with the status.
   */
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Provides the count of the bytes hashed.
   * 
   * @return a Long with the count of the bytes hashed..
   */
  public Long getBytesHashed() {
    return bytesHashed;
  }
  public void setBytesHashed(Long bytesHashed) {
    this.bytesHashed = bytesHashed;
  }

  /**
   * Provides the count of the files hashed.
   * 
   * @return an Integer with the count of the files hashed..
   */
  public Integer getFilesHashed() {
    return filesHashed;
  }
  public void setFilesHashed(Integer filesHashed) {
    this.filesHashed = filesHashed;
  }

  /**
   * Provides the length of time that the hashing operation took to complete.
   * 
   * @return a Long with the length of time in milliseconds.
   */
  public Long getElapsedTime() {
    return elapsedTime;
  }
  public void setElapsedTime(Long elapsedTime) {
    this.elapsedTime = elapsedTime;
  }

  @Override
  public String toString() {
    return "[HasherWsResult: startTime=" + startTime + ", recordFileName="
	+ recordFileName + ", blockFileName=" + blockFileName
	+ ", hashResult=" + Arrays.toString(hashResult) + ", errorMessage="
	+ errorMessage + ", status=" + status + ", bytesHashed=" + bytesHashed
	+ ", filesHashed=" + filesHashed + ", elapsedTime=" + elapsedTime + "]";
  }

}
