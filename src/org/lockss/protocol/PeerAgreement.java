/*
 * $Id$
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

package org.lockss.protocol;

import org.lockss.util.LockssSerializable;


/**
 * This class records the percent agreement between two peers, along
 * with a high-water mark and timestamps for both the most recent and
 * the highest recorded value.
 */
final public class PeerAgreement implements LockssSerializable {
  private final float percentAgreement;
  private final long percentAgreementTime;
  private final float highestPercentAgreement;
  private final long highestPercentAgreementTime;

  /**
   * A static instance indicating no agreement has been reached.
   */
  public static final PeerAgreement NO_AGREEMENT =
    new PeerAgreement(-1.0f, 0, -1.0f, 0);

  private PeerAgreement(
    float percentAgreement, long percentAgreementTime,
    float highestPercentAgreement, long highestPercentAgreementTime) {
    this.percentAgreement = percentAgreement;
    this.percentAgreementTime = percentAgreementTime;
    this.highestPercentAgreement = highestPercentAgreement;
    this.highestPercentAgreementTime = highestPercentAgreementTime;
  }

  /**
   * @return The {@link PeerAgreement} for POR to use when moving from
   * pre-1.63 data. The {@link getPercentAgreementTime} value will be
   * a best-estimate.  The {@link #getHighestPercentAgreementTime}
   * value will be zero.
   */
  static PeerAgreement
    porAgreement(IdentityManager.IdentityAgreement idAgreement) {
    long lastSignalTime = idAgreement.getLastSignalTime();
    return new PeerAgreement(idAgreement.getPercentAgreement(),
			     lastSignalTime,
			     idAgreement.getHighestPercentAgreement(),
			     0);
  }

  /**
   * @return The {@link PeerAgreement} for POR_HINT to use when moving
   * from pre-1.63 data. The {@link getPercentAgreementTime} value
   * will be zero.  The {@link #getHighestPercentAgreementTime} value
   * will be zero.
   */
  static PeerAgreement
    porAgreementHint(IdentityManager.IdentityAgreement idAgreement) {
    return new PeerAgreement(idAgreement.getPercentAgreementHint(),
			     0,
			     idAgreement.getHighestPercentAgreementHint(),
			     0);
  }

  @Override
  public String toString() {
    return "PeerAgreement["+
      "percentAgreement="+getPercentAgreement()+
      ", percentAgreementTime="+getPercentAgreementTime()+
      ", highestPercentAgreement="+getHighestPercentAgreement()+
      ", highestPercentAgreementTime="+getHighestPercentAgreementTime()+
      "]";
  }

@Override
  public boolean equals(Object o) {
    if (o instanceof PeerAgreement) {
      PeerAgreement other = (PeerAgreement)o;
      return other.getPercentAgreement() == getPercentAgreement()
	&& other.getPercentAgreementTime() == getPercentAgreementTime()
	&& other.getHighestPercentAgreement() == getHighestPercentAgreement()
	&& other.getHighestPercentAgreementTime() == getHighestPercentAgreementTime();
    }
    return false;
  }

@Override
  public int hashCode() {
    return (int)(getPercentAgreementTime()+getHighestPercentAgreementTime());
  }

  /**
   * @return the most recent value signaled.
   */
  public float getPercentAgreement() {
    return percentAgreement;
  }

  /**
   * @return the time of the most recent signal.
   */
  public long getPercentAgreementTime() {
    return percentAgreementTime;
  }

  /**
   * @return the highest value ever signaled.
   */
  public float getHighestPercentAgreement() {
    return highestPercentAgreement;
  }

  /**
   * @return the time when the highest value was first signaled.
   */
  public long getHighestPercentAgreementTime() {
    return highestPercentAgreementTime;
  }

  /**
   * @return a new instance which folds the given values into this
   * instance.
   */
  public PeerAgreement signalAgreement(float percentAgreement, long time) {
    float highestPercentAgreement;
    long highestPercentAgreementTime;
    
    if (percentAgreement < 0.0f || percentAgreement > 1.0f) {
      throw new IllegalArgumentException("pecentAgreement must be between "+
				   "0.0 and 1.0. It was: "+percentAgreement);
    }

    if (percentAgreement > this.highestPercentAgreement) {
      highestPercentAgreement = percentAgreement;
      highestPercentAgreementTime = time;
    } else {
      highestPercentAgreement = this.percentAgreement;
      highestPercentAgreementTime = this.highestPercentAgreementTime;
    }

    return new PeerAgreement(
      percentAgreement, time,
      highestPercentAgreement, highestPercentAgreementTime);
  }
}
