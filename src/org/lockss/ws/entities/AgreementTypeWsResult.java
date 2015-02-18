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
 * Different agreement situations we record.
 * 
 * Note: in a symmetric poll the poller and voter will not necessarily record
 * the same percent agreement, since each may have content for URLs which the
 * other does not. Since the hint received after a symmetric poll is the other
 * participant's recorded percent agreement, the calculated percent agreements
 * are used to decide if a repair request should be honored, and hints are used
 * to try to find willing repairers likely to honor repair requests.
 */
// The enumerated values in this class need to be match those in class
// {@link org.lockss.protocol.AgreementType}.
public enum AgreementTypeWsResult {
  /**
   * A poll with all content hashed and tallied. Recorded by poller.
   */
  POR,
  /**
   * A poll with a selection of the content hashed and tallied. Recorded by
   * poller.
   */
  POP,
  /**
   * A POR poll where a voter has called for the poller's hashes. Recorded by
   * voter.
   */
  SYMMETRIC_POR,
  /**
   * A POP poll where a voter has called for the poller's hashes. Recorded by
   * voter.
   */
  SYMMETRIC_POP,
  /**
   * The hint given a voter by the poller after a POR poll. Recorded by voter.
   */
  POR_HINT,
  /**
   * The hint given a voter by the poller after a POP poll. Recorded by voter.
   */
  POP_HINT,
  /**
   * The hint given a poller by a voter after a symmetric POR poll. Recorded by
   * poller.
   */
  SYMMETRIC_POR_HINT,
  /**
   * The hint given a poller by a voter after a symmetric POP poll. Recorded by
   * poller.
   */
  SYMMETRIC_POP_HINT
}
