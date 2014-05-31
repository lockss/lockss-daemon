/*
 * $Id: PlatformConfigurationWsResult.java,v 1.1.2.2 2014-05-31 01:26:08 fergaloy-sf Exp $
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
 * The daemon platform configuration information.
 */
package org.lockss.ws.entities;

import java.util.List;

public class PlatformConfigurationWsResult {

  private String hostName;
  private String ipAddress;
  private List<String> groups;
  private String project;
  private String v3Identity;
  private String mailRelay;
  private String adminEmail;
  private List<String> disks;
  private long currentTime;
  private long uptime;
  private DaemonVersionWsResult daemonVersion;
  private PlatformWsResult platform;
  private String currentWorkingDirectory;
  private List<String> properties;
  private String buildHost;
  private long buildTimestamp;

  public String getHostName() {
    return hostName;
  }
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }
  public String getIpAddress() {
    return ipAddress;
  }
  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }
  public List<String> getGroups() {
    return groups;
  }
  public void setGroups(List<String> groups) {
    this.groups = groups;
  }
  public String getProject() {
    return project;
  }
  public void setProject(String project) {
    this.project = project;
  }
  public String getV3Identity() {
    return v3Identity;
  }
  public void setV3Identity(String v3Identity) {
    this.v3Identity = v3Identity;
  }
  public String getMailRelay() {
    return mailRelay;
  }
  public void setMailRelay(String mailRelay) {
    this.mailRelay = mailRelay;
  }
  public String getAdminEmail() {
    return adminEmail;
  }
  public void setAdminEmail(String adminEmail) {
    this.adminEmail = adminEmail;
  }
  public List<String> getDisks() {
    return disks;
  }
  public void setDisks(List<String> disks) {
    this.disks = disks;
  }
  public long getCurrentTime() {
    return currentTime;
  }
  public void setCurrentTime(long currentTime) {
    this.currentTime = currentTime;
  }
  public long getUptime() {
    return uptime;
  }
  public void setUptime(long uptime) {
    this.uptime = uptime;
  }
  public DaemonVersionWsResult getDaemonVersion() {
    return daemonVersion;
  }
  public void setDaemonVersion(DaemonVersionWsResult daemonVersion) {
    this.daemonVersion = daemonVersion;
  }
  public PlatformWsResult getPlatform() {
    return platform;
  }
  public void setPlatform(PlatformWsResult platform) {
    this.platform = platform;
  }
  public String getCurrentWorkingDirectory() {
    return currentWorkingDirectory;
  }
  public void setCurrentWorkingDirectory(String currentWorkingDirectory) {
    this.currentWorkingDirectory = currentWorkingDirectory;
  }
  public List<String> getProperties() {
    return properties;
  }
  public void setProperties(List<String> properties) {
    this.properties = properties;
  }
  public String getBuildHost() {
    return buildHost;
  }
  public void setBuildHost(String buildHost) {
    this.buildHost = buildHost;
  }
  public long getBuildTimestamp() {
    return buildTimestamp;
  }
  public void setBuildTimestamp(long buildTimestamp) {
    this.buildTimestamp = buildTimestamp;
  }

  @Override
  public String toString() {
    return "PlatformConfigurationWsResult [hostName=" + hostName
	+ ", ipAddress=" + ipAddress + ", groups=" + groups + ", project="
	+ project + ", v3Identity=" + v3Identity + ", mailRelay=" + mailRelay
	+ ", adminEmail=" + adminEmail + ", disks=" + disks + ", currentTime="
	+ currentTime + ", uptime=" + uptime + ", daemonVersion="
	+ daemonVersion + ", platform=" + platform
	+ ", currentWorkingDirectory=" + currentWorkingDirectory
	+ ", properties=" + properties + ", buildHost=" + buildHost
	+ ", buildTimestamp=" + buildTimestamp + "]";
  }
  
}
