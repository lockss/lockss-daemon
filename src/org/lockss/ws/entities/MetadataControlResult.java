/*
 * $Id$
 */

/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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
 * A wrapper for the result of a Metadata Control web service operation.
 */
public class MetadataControlResult {
  private Boolean isSuccess;
  private String message;

  /**
   * Default constructor.
   */
  public MetadataControlResult() {
  }

  /**
   * Constructor.
   * 
   * @param isSuccess
   *          A Boolean with the indication of whether the operation was
   *          successful.
   * @param message
   *          A String with a descriptive message of the result of the
   *          operation.
   */
  public MetadataControlResult(Boolean isSuccess, String message) {
    this.isSuccess = isSuccess;
    this.message = message;
  }

  /**
   * Provides an indication of whether the operation was successful.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean getIsSuccess() {
    return isSuccess;
  }
  public void setIsSuccess(Boolean isSuccess) {
    this.isSuccess = isSuccess;
  }

  /**
   * Provides a descriptive message of the result of the operation.
   * 
   * @return a String with the message.
   */
  public String getMessage() {
    return message;
  }
  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "[MetadataControlResult isSuccess=" + isSuccess + ", message="
	+ message + "]";
  }
}
