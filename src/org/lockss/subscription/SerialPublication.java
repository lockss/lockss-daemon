/*

 Copyright (c) 2013-2020 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.Collection;
import java.util.Set;
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

  private Long publicationNumber;
  private String publicationName;
  private String providerLid;
  private String providerName;
  private String publisherName;
  private String pIssn;
  private String eIssn;
  private Set<String> proprietaryIds;
  private TdbTitle tdbTitle;
  private String uniqueName;
  private int availableAuCount = -1;
  private int auCount = -1;

  public Long getPublicationNumber() {
    return publicationNumber;
  }

  public void setPublicationNumber(Long publicationNumber) {
    this.publicationNumber = publicationNumber;
  }

  public String getPublicationName() {
    return publicationName;
  }

  public void setPublicationName(String publicationName) {
    this.publicationName = publicationName;
  }

  public String getProviderLid() {
    return providerLid;
  }

  public void setProviderLid(String providerLid) {
    this.providerLid = providerLid;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
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

  public Set<String> getProprietaryIds() {
    return proprietaryIds;
  }

  public void setProprietaryIds(Set<String> proprietaryIds) {
    this.proprietaryIds = proprietaryIds;
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
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "tdbTitle (eISSN = "
	  + eIssn + ") = " + title);

      // Check whether the TdbTitle is not found.
      if (title == null) {
	// Yes: Get the TdbTitle from the pISSN.
	title =
	    TdbUtil.getTdb().getTdbTitleByIssn(MetadataUtil.formatIssn(pIssn));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "tdbTitle (pISSN = "
	    + pIssn + ") = " + title);
      }

      // Check whether the TdbTitle is not found.
      if (title == null) {
	// Yes: Get the TdbTitle from the proprietary identifier.
	for (String proprietaryId : proprietaryIds) {
	  title = TdbUtil.getTdb().getTdbTitleById(proprietaryId);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER
	      + "tdbTitle (proprietaryId = " + proprietaryId + ") = "
	      + title);

	  if (title != null) {
	    break;
	  }
	}
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
	String message = "Cannot find tdbTitle for publication " + this;
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

  public int getAvailableAuCount() {
    if (availableAuCount < 0 && tdbTitle == null) {
      getTdbTitle();

      if (tdbTitle != null) {
	availableAuCount = tdbTitle.getTdbAvailableAuCount();
      }
    }

    return availableAuCount;
  }

  public void setAvailableAuCount(int availableAuCount) {
    this.availableAuCount = availableAuCount;
  }

  public int getAuCount() {
    final String DEBUG_HEADER = "getAuCount(): ";
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auCount = " + auCount);
    if (auCount < 0) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "tdbTitle = " + tdbTitle);
      if (tdbTitle == null) {
	getTdbTitle();
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "tdbTitle = " + tdbTitle);
      if (tdbTitle != null) {
	auCount = tdbTitle.getTdbAuCount();
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auCount = " + auCount);
    return auCount;
  }

  public void setAuCount(int auCount) {
    this.auCount = auCount;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(
	"[SerialPublication publicationNumber=").append(publicationNumber)
	.append(", publicationName='").append(publicationName)
	.append("', providerLid='").append(providerLid)
	.append("', providerName='").append(providerName)
	.append("', publisherName='").append(publisherName)
	.append("', pIssn='").append(pIssn)
	.append("', eIssn='").append(eIssn)
	.append("', proprietaryIds='").append(proprietaryIds)
	.append("', uniqueName='").append(uniqueName)
	.append("', availableAuCount=").append(availableAuCount)
	.append("', auCount=").append(auCount);

    if (tdbTitle != null) {
      sb.append("', ").append(tdbTitle.toString()).append("]");
    } else {
      sb.append("', TdbTitle: [null]").append("]");
    }

    return sb.toString();
  }
}
