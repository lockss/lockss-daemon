/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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
 * Container for the metadata information of an archival unit.
 */
public class AuMetadataWsResult {
  private String auId;
  private Long auSeq;
  private Long auMdSeq;
  private Integer mdVersion;
  private Long extractTime;
  private Long creationTime;
  private Long providerSeq;
  private String providerName;
  private Integer itemCount;

  public String getAuId() {
    return auId;
  }
  public void setAuId(String auId) {
    this.auId = auId;
  }

  public Long getAuSeq() {
    return auSeq;
  }
  public void setAuSeq(Long auSeq) {
    this.auSeq = auSeq;
  }

  public Long getAuMdSeq() {
    return auMdSeq;
  }
  public void setAuMdSeq(Long auMdSeq) {
    this.auMdSeq = auMdSeq;
  }

  public Integer getMdVersion() {
    return mdVersion;
  }
  public void setMdVersion(Integer mdVersion) {
    this.mdVersion = mdVersion;
  }

  public Long getExtractTime() {
    return extractTime;
  }
  public void setExtractTime(Long extractTime) {
    this.extractTime = extractTime;
  }

  public Long getCreationTime() {
    return creationTime;
  }
  public void setCreationTime(Long creationTime) {
    this.creationTime = creationTime;
  }

  public Long getProviderSeq() {
    return providerSeq;
  }
  public void setProviderSeq(Long providerSeq) {
    this.providerSeq = providerSeq;
  }

  public String getProviderName() {
    return providerName;
  }
  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public Integer getItemCount() {
    return itemCount;
  }
  public void setItemCount(Integer itemCount) {
    this.itemCount = itemCount;
  }

  @Override
  public String toString() {
    return "[AuMetadataWsResult auId=" + auId + ", auSeq=" + auSeq
	+ ", auMdSeq=" + auMdSeq + ", mdVersion=" + mdVersion
	+ ", extractTime=" + extractTime + ", creationTime=" + creationTime
	+ ", providerSeq=" + providerSeq + ", providerName=" + providerName
	+ ", itemCount=" + itemCount + "]";
  }
}
