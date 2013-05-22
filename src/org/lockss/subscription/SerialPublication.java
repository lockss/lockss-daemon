/*
 * $Id: SerialPublication.java,v 1.1 2013-05-22 23:40:20 fergaloy-sf Exp $
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

import static org.lockss.db.DbManager.*;
import org.lockss.config.TdbTitle;

/**
 * Representation of a serial publication for subscription purposes.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class SerialPublication {
  private Integer publicationNumber;
  private String publicationName;
  private String platformName = NO_PLATFORM;
  private String publisherName;
  private String pIssn;
  private String eIssn;
  private String proprietaryId;
  private TdbTitle tdbTitle;
  private String uniqueName;

  public Integer getPublicationNumber() {
    return publicationNumber;
  }

  public void setPublicationNumber(Integer publicationNumber) {
    this.publicationNumber = publicationNumber;
  }

  public String getPublicationName() {
    return publicationName;
  }

  public void setPublicationName(String publicationName) {
    this.publicationName = publicationName;
  }

  public String getPlatformName() {
    return platformName;
  }

  public void setPlatformName(String platformName) {
    this.platformName = platformName;
  }

  public String getPublisherName() {
    return publisherName;
  }

  public void setPublisherName(String publisherName) {
    this.publisherName = publisherName;
  }

  public String getPissn() {
    return pIssn;
  }

  public void setPissn(String pIssn) {
    this.pIssn = pIssn;
  }

  public String getEissn() {
    return eIssn;
  }

  public void setEissn(String eIssn) {
    this.eIssn = eIssn;
  }

  public String getProprietaryId() {
    return proprietaryId;
  }

  public void setProprietaryId(String proprietaryId) {
    this.proprietaryId = proprietaryId;
  }

  public TdbTitle getTdbTitle() {
    return tdbTitle;
  }

  public void setTdbTitle(TdbTitle tdbTitle) {
    this.tdbTitle = tdbTitle;
  }

  public String getUniqueName() {
    return uniqueName;
  }

  public void setUniqueName(String uniqueName) {
    this.uniqueName = uniqueName;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(
	"SerialPublication [publicationNumber=").append(publicationNumber)
	.append(", publicationName='").append(publicationName)
	.append("', platformName='").append(platformName)
	.append("', publisherName='").append(publisherName)
	.append("', pIssn='").append(pIssn)
	.append("', eIssn='").append(eIssn)
	.append("', proprietaryId='").append(proprietaryId)
	.append("', uniqueName='").append(uniqueName);

    if (tdbTitle != null) {
      sb.append("', ").append(tdbTitle.toString()).append("]");
    } else {
      sb.append("', TdbTitle: [null]");
    }

    return sb.toString();
  }
}
