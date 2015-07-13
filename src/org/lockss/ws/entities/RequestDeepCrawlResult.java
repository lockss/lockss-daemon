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
 * A wrapper for the result of an archival unit deep crawl request web service
 * operation.
 */
public class RequestDeepCrawlResult extends RequestCrawlResult {
  private int refetchDepth;

  /**
   * Default constructor.
   */
  public RequestDeepCrawlResult() {
  }

  /**
   * Constructor.
   * 
   * @param id
   *          A String with the Archival Unit identifier.
   * @param refetchDepth
   *          An int with the refetch depth of the crawl.
   * @param success
   *          A boolean with <code>true</code> if the request was made
   *          successfully, <code>false</code> otherwise.
   * @param delayReason
   *          A String with the reason for any delay in performing the
   *          operation.
   * @param errorMessage
   *          A String with any error message as a result of the operation.
   */
  public RequestDeepCrawlResult(String id, int refetchDepth, boolean success,
      String delayReason, String errorMessage) {
    setId(id);
    setSuccess(success);
    setDelayReason(delayReason);
    setErrorMessage(errorMessage);
    this.refetchDepth = refetchDepth;
  }

  /**
   * Provides the crawl refetch depth.
   * 
   * @return an int with the crawl refetch depth.
   */
  public int getRefetchDepth() {
    return refetchDepth;
  }
  public void setRefetchDepth(int refetchDepth) {
    this.refetchDepth = refetchDepth;
  }

  @Override
  public String toString() {
    return "[RequestDeepCrawlResult: refetchDepth=" + refetchDepth + ", "
	+ super.toString() + "]";
  }
}
