/*
 * $Id: RepositoryWsResult.java,v 1.1.2.2 2014-05-05 17:32:30 wkwilson Exp $
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

/**
 * Container for the information related to a repository that is the result of a
 * query.
 */
package org.lockss.ws.entities;

import java.util.Map;

public class RepositoryWsResult {
  private String repositorySpaceId;
  private String directoryName;
  private String auName;
  private Boolean internal;
  private String status;
  private Long diskUsage;
  private String pluginName;
  private Map<String, String> params;

  public String getRepositorySpaceId() {
    return repositorySpaceId;
  }
  public void setRepositorySpaceId(String repositorySpaceId) {
    this.repositorySpaceId = repositorySpaceId;
  }
  public String getDirectoryName() {
    return directoryName;
  }
  public void setDirectoryName(String directoryName) {
    this.directoryName = directoryName;
  }
  public String getAuName() {
    return auName;
  }
  public void setAuName(String auName) {
    this.auName = auName;
  }
  public Boolean getInternal() {
    return internal;
  }
  public void setInternal(Boolean internal) {
    this.internal = internal;
  }
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }
  public Long getDiskUsage() {
    return diskUsage;
  }
  public void setDiskUsage(Long diskUsage) {
    this.diskUsage = diskUsage;
  }
  public String getPluginName() {
    return pluginName;
  }
  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }
  public Map<String, String> getParams() {
    return params;
  }
  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  @Override
  public String toString() {
    return "RepositoryWsResult [repositorySpaceId=" + repositorySpaceId
	+ ", directoryName=" + directoryName + ", auName=" + auName
	+ ", internal="	+ internal + ", status=" + status + ", diskUsage="
	+ diskUsage + ", pluginName=" + pluginName + ", params=" + params + "]";
  }
}
