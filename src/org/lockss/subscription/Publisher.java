/*

 Copyright (c) 2015-2019 Board of Trustees of Leland Stanford Jr. University,
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
 * Representation of a publisher for subscription purposes.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class Publisher {
  private Long publisherSeq;
  private Long publisherNumber;
  private String publisherName;
  private int availableAuCount;
  private int auCount;

  public Long getPublisherSeq() {
    return publisherSeq;
  }
  public void setPublisherSeq(Long publisherSeq) {
    this.publisherSeq = publisherSeq;
  }
  public Long getPublisherNumber() {
    return publisherNumber;
  }
  public void setPublisherNumber(Long publisherNumber) {
    this.publisherNumber = publisherNumber;
  }
  public String getPublisherName() {
    return publisherName;
  }
  public void setPublisherName(String publisherName) {
    this.publisherName = publisherName;
  }
  public int getAvailableAuCount() {
    return availableAuCount;
  }
  public void setAvailableAuCount(int availableAuCount) {
    this.availableAuCount = availableAuCount;
  }
  public int getAuCount() {
    return auCount;
  }
  public void setAuCount(int auCount) {
    this.auCount = auCount;
  }

  @Override
  public String toString() {
    return "[Publisher publisherNumber=" + publisherNumber
	+ ", publisherName=" + publisherName
	+ ", availableAuCount=" + availableAuCount + ", auCount=" + auCount
	+ "]";
  }
}
