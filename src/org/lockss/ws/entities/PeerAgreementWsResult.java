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

/**
 * Container for the information related to an archival unit poll agreement
 * that is the result of a query.
 */
public class PeerAgreementWsResult {
  private Float percentAgreement;
  private Long percentAgreementTimestamp;
  private Float highestPercentAgreement;
  private Long highestPercentAgreementTimestamp;

  /**
   * No-argument constructor required by CXF.
   */
  public PeerAgreementWsResult() {
    
  }

  /**
   * Constructor.
   * 
   * @param percentAgreement
   *          A Float with the most recent agreement percentage.
   * @param percentAgreementTimestamp
   *          A Long with the timestamp of the most recent agreement percentage.
   * @param highestPercentAgreement
   *          A Float with the highest-ever agreement percentage.
   * @param highestPercentAgreementTimestamp
   *          A Long with the timestamp of the highest-ever agreement
   *          percentage.
   */
  public PeerAgreementWsResult(
    Float percentAgreement, Long percentAgreementTimestamp,
    Float highestPercentAgreement, Long highestPercentAgreementTimestamp) {
    this.percentAgreement = percentAgreement;
    this.percentAgreementTimestamp = percentAgreementTimestamp;
    this.highestPercentAgreement = highestPercentAgreement;
    this.highestPercentAgreementTimestamp = highestPercentAgreementTimestamp;
  }

  /**
   * Provides the most recent agreement percentage.
   * 
   * @return a Float with the percentage.
   */
  public Float getPercentAgreement() {
    return percentAgreement;
  }
  public void setPercentAgreement(Float percentAgreement) {
    this.percentAgreement = percentAgreement;
  }

  /**
   * Provides the timestamp of the most recent agreement percentage.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getPercentAgreementTimestamp() {
    return percentAgreementTimestamp;
  }
  public void setPercentAgreementTimestamp(Long percentAgreementTimestamp) {
    this.percentAgreementTimestamp = percentAgreementTimestamp;
  }

  /**
   * Provides the highest-ever agreement percentage.
   * 
   * @return a Float with the percentage.
   */
  public Float getHighestPercentAgreement() {
    return highestPercentAgreement;
  }
  public void setHighestPercentAgreement(Float highestPercentAgreement) {
    this.highestPercentAgreement = highestPercentAgreement;
  }

  /**
   * Provides the timestamp of the highest-ever agreement percentage.
   * 
   * @return a Long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public Long getHighestPercentAgreementTimestamp() {
    return highestPercentAgreementTimestamp;
  }
  public void setHighestPercentAgreementTimestamp(
      Long highestPercentAgreementTimestamp) {
    this.highestPercentAgreementTimestamp = highestPercentAgreementTimestamp;
  }

  @Override
  public String toString() {
    return "PeerAgreementWsResult [percentAgreement=" + percentAgreement
	+ ", percentAgreementTimestamp=" + percentAgreementTimestamp
	+ ", highestPercentAgreement=" + highestPercentAgreement
	+ ", highestPercentAgreementTimestamp="
	+ highestPercentAgreementTimestamp + "]";
  }
}
