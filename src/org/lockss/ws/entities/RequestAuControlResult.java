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
 * A wrapper for the result of an archival unit generic control request web
 * service operation.
 */
public class RequestAuControlResult {
  private String id;
  private boolean success = false;
  private String errorMessage;

  /**
   * Default constructor.
   */
  public RequestAuControlResult() {
  }

  /**
   * Constructor.
   * 
   * @param id
   *          A String with the Archival Unit identifier.
   * @param success
   *          A boolean with <code>true</code> if the request was made
   *          successfully, <code>false</code> otherwise.
   * @param errorMessage
   *          A String with any error message as a result of the operation.
   */
  public RequestAuControlResult(String id, boolean success,
      String errorMessage) {
    this.id = id;
    this.success = success;
    this.errorMessage = errorMessage;
  }

  /**
   * Provides the Archival Unit identifier.
   * 
   * @return a String with the identifier.
   */
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Provides the indication of whether the request was made successfully.
   * 
   * @return a boolean with <code>true</code> if the request was made
   *          successfully, <code>false</code> otherwise.
   */
  public boolean isSuccess() {
    return success;
  }
  public void setSuccess(boolean success) {
    this.success = success;
  }

  /**
   * Provides any error message as a result of the operation.
   * 
   * @return a String with the error message.
   */
  public String getErrorMessage() {
    return errorMessage;
  }
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @Override
  public String toString() {
    return "[RequestAuControlResult id=" + id + ", success=" + success
	+ ", errorMessage=" + errorMessage + "]";
  }
}
