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

import java.util.Map;

/**
 * Container for the information related to an archival unit poll agreements
 * with another peer that is the result of a query.
 */
public class PeerAgreementsWsResult {
  private String peerId;
  private Map<AgreementTypeWsResult, PeerAgreementWsResult> agreements;

  /**
   * Provides the identifier of the other peer in the agreement.
   * 
   * @return a String with the identifier.
   */
  public String getPeerId() {
    return peerId;
  }
  public void setPeerId(String peerId) {
    this.peerId = peerId;
  }

  /**
   * Provides the data about the agreements with the other peer.
   * 
   * @return a Map<AgreementTypeWsResult, PeerAgreementWsResult> with the
   *         agreements data.
   */
  public Map<AgreementTypeWsResult, PeerAgreementWsResult> getAgreements() {
    return agreements;
  }
  public void setAgreements(
      Map<AgreementTypeWsResult, PeerAgreementWsResult> agreements) {
    this.agreements = agreements;
  }

  @Override
  public String toString() {
    return "PeerAgreementsWsResult [peerId=" + peerId + ", agreements="
	+ agreements + "]";
  }
}
