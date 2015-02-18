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
 * Container for the information related to a repository that is the result of a
 * query.
 */
public class RepositoryWsResult {
  private String repositorySpaceId;
  private String directoryName;
  private String auName;
  private Boolean internal;
  private String status;
  private Long diskUsage;
  private String pluginName;
  private Map<String, String> params;

  /**
   * Provides the repository space identifier.
   * 
   * @return a String with the identifier.
   */
  public String getRepositorySpaceId() {
    return repositorySpaceId;
  }
  public void setRepositorySpaceId(String repositorySpaceId) {
    this.repositorySpaceId = repositorySpaceId;
  }

  /**
   * Provides the repository directory name.
   * 
   * @return a String with the directory name.
   */
  public String getDirectoryName() {
    return directoryName;
  }
  public void setDirectoryName(String directoryName) {
    this.directoryName = directoryName;
  }

  /**
   * Provides the Archival Unit name.
   * 
   * @return a String with the name.
   */
  public String getAuName() {
    return auName;
  }
  public void setAuName(String auName) {
    this.auName = auName;
  }

  /**
   * Provides an indication of whether the repository is internal.
   * 
   * @return a Boolean with the indication.
   */
  public Boolean getInternal() {
    return internal;
  }
  public void setInternal(Boolean internal) {
    this.internal = internal;
  }

  /**
   * Provides the repository status.
   * 
   * @return a String with the status.
   */
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Provides the space occupied on disk by the repository.
   * 
   * @return a Long with the occupied space in bytes.
   */
  public Long getDiskUsage() {
    return diskUsage;
  }
  public void setDiskUsage(Long diskUsage) {
    this.diskUsage = diskUsage;
  }

  /**
   * Provides the Archival Unit plugin name.
   * 
   * @return a String with the plugin name.
   */
  public String getPluginName() {
    return pluginName;
  }
  public void setPluginName(String pluginName) {
    this.pluginName = pluginName;
  }

  /**
   * Provides the Archival Unit configuration parameters.
   * 
   * @return a Map<String, String> with the parameters.
   */
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
