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

import java.util.List;

/**
 * Container for the information related to a title database title that is the
 * result of a query.
 */
public class TdbTitleWsResult {
  private String name;
  private TdbPublisherWsResult tdbPublisher;
  private String id;
  private String proprietaryId;
  private List<String> proprietaryIds;
  private String publicationType;
  private String issn;
  private String issnL;
  private String eIssn;
  private String printIssn;
  private List<String> issns;

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public TdbPublisherWsResult getTdbPublisher() {
    return tdbPublisher;
  }
  public void setTdbPublisher(TdbPublisherWsResult tdbPublisher) {
    this.tdbPublisher = tdbPublisher;
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  /**
   * @deprecated Replaced by {@link #getProprietaryIds()}
   */
  @Deprecated public String getProprietaryId() {
    return proprietaryId;
  }
  /**
   * @deprecated Replaced by {@link #setProprietaryIds(List<String>)}
   */
  @Deprecated public void setProprietaryId(String proprietaryId) {
    this.proprietaryId = proprietaryId;
  }
  public List<String> getProprietaryIds() {
    return proprietaryIds;
  }
  public void setProprietaryIds(List<String> proprietaryIds) {
    this.proprietaryIds = proprietaryIds;
  }
  public String getPublicationType() {
    return publicationType;
  }
  public void setPublicationType(String publicationType) {
    this.publicationType = publicationType;
  }
  public String getIssn() {
    return issn;
  }
  public void setIssn(String issn) {
    this.issn = issn;
  }
  public String getIssnL() {
    return issnL;
  }
  public void setIssnL(String issnL) {
    this.issnL = issnL;
  }
  public String getEIssn() {
    return eIssn;
  }
  public void setEIssn(String eIssn) {
    this.eIssn = eIssn;
  }
  public String getPrintIssn() {
    return printIssn;
  }
  public void setPrintIssn(String printIssn) {
    this.printIssn = printIssn;
  }
  public List<String> getIssns() {
    return issns;
  }
  public void setIssns(List<String> issns) {
    this.issns = issns;
  }

  @Override
  public String toString() {
    return "TdbTitleWsResult [name=" + name + ", tdbPublisher=" + tdbPublisher
	+ ", id=" + id + ", proprietaryId=" + proprietaryId
	+ ", proprietaryIds=" + proprietaryIds + ", publicationType="
	+ publicationType + ", issn=" + issn + ", issnL=" + issnL + ", eIssn="
	+ eIssn + ", printIssn=" + printIssn + ", issns=" + issns + "]";
  }
}
