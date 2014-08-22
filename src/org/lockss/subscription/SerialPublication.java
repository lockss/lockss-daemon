/*
 * $Id: SerialPublication.java,v 1.4 2014-08-22 22:15:00 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.db.SqlConstants.*;
import java.util.Collection;
import org.lockss.config.TdbTitle;
import org.lockss.config.TdbUtil;
import org.lockss.util.Logger;
import org.lockss.util.MetadataUtil;

/**
 * Representation of a serial publication for subscription purposes.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class SerialPublication {
  private static final Logger log = Logger.getLogger(SerialPublication.class);

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

  /**
   * Provides the TdbTitle that corresponds to this publication.
   * 
   * @return a TdbTitle that corresponds to this publication.
   */
  public TdbTitle getTdbTitle() {
    final String DEBUG_HEADER = "getTdbTitle(): ";

    // Check whether the publication has no TdbTitle.
    if (tdbTitle == null) {
      // Yes: Get the TdbTitles for the publication name.
      Collection<TdbTitle> tdbTitles =
	  TdbUtil.getTdb().getTdbTitlesByName(publicationName);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "tdbTitles = " + tdbTitles);

      // Check whether a TdbTitle was found.
      if (tdbTitles != null && tdbTitles.size() > 0) {
	// Yes: Check whether there is a single title.
	if (tdbTitles.size() == 1) {
	  // Yes: Populate it into the publication.
	  setTdbTitle(tdbTitles.iterator().next());
	} else {
	  // No: Check whether the publication has a publisher name.
	  if (publisherName != null) {
	    // Yes.
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	    // Loop through all the titles.
	    for (TdbTitle title : tdbTitles) {
	      // Get the title publisher name.
	      String titlePublisherName = title.getTdbPublisher().getName();
	      if (log.isDebug3()) log.debug3(DEBUG_HEADER
		  + "titlePublisherName = " + titlePublisherName);

	      // Check whether this title belongs to the publication publisher.
	      if (publisherName.equals(titlePublisherName)) {
		// Yes: Populate this title into the publication.
		setTdbTitle(title);
		break;
	      }
	    }
	  }
	}
      }
    }

    // Check whether the publication still has no TdbTitle.
    if (tdbTitle == null) {
      // Yes: Get the TdbTitle from the eISSN.
      TdbTitle title =
	  TdbUtil.getTdb().getTdbTitleByIssn(MetadataUtil.formatIssn(eIssn));

      // Check whether the TdbTitle is not found.
      if (title == null) {
	// Yes: Get the TdbTitle from the pISSN.
	title =
	    TdbUtil.getTdb().getTdbTitleByIssn(MetadataUtil.formatIssn(pIssn));
      }

      // Check whether the TdbTitle was found.
      if (title != null) {
	// Yes: Populate it into the publication.
	setTdbTitle(title);

	// Use the publisher name from the TdbTitle, if different.
	String titlePublisherName = title.getTdbPublisher().getName();
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "titlePublisherName = " + titlePublisherName);

	if (!publisherName.equals(titlePublisherName)) {
	  setPublisherName(titlePublisherName);
	}

	// Use the publication name from the TdbTitle.
	setPublicationName(title.getName());
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Changed publication name to '" + publicationName + "'.");
      } else {
	// No: Report the problem.
	String message = "Cannot find tdbTitle with name '" + publicationName
	    + "' for publisher with name '" + publisherName + "'.";
	log.error(message);
      }
    }

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
