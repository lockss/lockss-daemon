/*
 * $Id: SubscriptionOperationStatus.java,v 1.1 2013-05-22 23:40:20 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.subscription;


/**
 * Status representation of a subscription operation.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class SubscriptionOperationStatus {
  private int failureAuAddCount = 0;
  private int failureSubscriptionAddCount = 0;
  private int failureSubscriptionUpdateCount = 0;
  private int successAuAddCount = 0;
  private int successSubscriptionAddCount = 0;
  private int successSubscriptionUpdateCount = 0;

  public int getFailureAuAddCount() {
    return failureAuAddCount;
  }
  public void addFailureAuAdd(int count) {
    setFailureAuAddCount(getFailureAuAddCount() + count);
  }
  private void setFailureAuAddCount(int failureAuAddCount) {
    this.failureAuAddCount = failureAuAddCount;
  }
  public int getFailureSubscriptionAddCount() {
    return failureSubscriptionAddCount;
  }
  public void addFailureSubscriptionAdd(int count) {
    setFailureSubscriptionAddCount(getFailureSubscriptionAddCount() + count);
  }
  private void setFailureSubscriptionAddCount(int failureSubscriptionAddCount) {
    this.failureSubscriptionAddCount = failureSubscriptionAddCount;
  }
  public int getFailureSubscriptionUpdateCount() {
    return failureSubscriptionUpdateCount;
  }
  public void addFailureSubscriptionUpdate(int count) {
    setFailureSubscriptionUpdateCount(getFailureSubscriptionUpdateCount()
	+ count);
  }
  private void setFailureSubscriptionUpdateCount(int
      failureSubscriptionUpdateCount) {
    this.failureSubscriptionUpdateCount = failureSubscriptionUpdateCount;
  }
  public int getSuccessAuAddCount() {
    return successAuAddCount;
  }
  public void addSuccessAuAdd(int count) {
    setSuccessAuAddCount(getSuccessAuAddCount() + count);
  }
  private void setSuccessAuAddCount(int successAuAddCount) {
    this.successAuAddCount = successAuAddCount;
  }
  public int getSuccessSubscriptionAddCount() {
    return successSubscriptionAddCount;
  }
  public void addSuccessSubscriptionAdd(int count) {
    setSuccessSubscriptionAddCount(getSuccessSubscriptionAddCount() + count);
  }
  private void setSuccessSubscriptionAddCount(int successSubscriptionAddCount) {
    this.successSubscriptionAddCount = successSubscriptionAddCount;
  }
  public int getSuccessSubscriptionUpdateCount() {
    return successSubscriptionUpdateCount;
  }
  public void addSuccessSubscriptionUpdate(int count) {
    setSuccessSubscriptionUpdateCount(getSuccessSubscriptionUpdateCount()
	+ count);
  }
  private void setSuccessSubscriptionUpdateCount(int
      successSubscriptionUpdateCount) {
    this.successSubscriptionUpdateCount = successSubscriptionUpdateCount;
  }

  @Override
  public String toString() {
    return "SubscriptionOperationStatus [failureAuAddCount=" + failureAuAddCount
	+ ", failureSubscriptionAddCount=" + failureSubscriptionAddCount
	+ ", failureSubscriptionUpdateCount=" + failureSubscriptionUpdateCount
	+ ", successAuAddCount=" + successAuAddCount
	+ ", successSubscriptionAddCount=" + successSubscriptionAddCount
	+ ", successSubscriptionUpdateCount=" + successSubscriptionUpdateCount
	+ "]";
  }
}
