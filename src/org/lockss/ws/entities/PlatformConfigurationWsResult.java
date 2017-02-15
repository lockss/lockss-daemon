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
 * The daemon platform configuration information.
 */
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
  private JavaVersionWsResult javaVersion;
  private PlatformWsResult platform;
  private String currentWorkingDirectory;
  private List<String> properties;
  private String buildHost;
  private long buildTimestamp;

  /**
   * Provides the host name.
   * 
   * @return a String with the host name.
   */
  public String getHostName() {
    return hostName;
  }
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Provides the IP address.
   * 
   * @return a String with the IP address.
   */
  public String getIpAddress() {
    return ipAddress;
  }
  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  /**
   * Provides the groups.
   * 
   * @return a List<String> with the groups.
   */
  public List<String> getGroups() {
    return groups;
  }
  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  /**
   * Provides the project name.
   * 
   * @return a String with the project name.
   */
  public String getProject() {
    return project;
  }
  public void setProject(String project) {
    this.project = project;
  }

  /**
   * Provides the V3 identity.
   * 
   * @return a String with the V3 identity.
   */
  public String getV3Identity() {
    return v3Identity;
  }
  public void setV3Identity(String v3Identity) {
    this.v3Identity = v3Identity;
  }

  /**
   * Provides the name of the mail relay.
   * 
   * @return a String with the mail relay name.
   */
  public String getMailRelay() {
    return mailRelay;
  }
  public void setMailRelay(String mailRelay) {
    this.mailRelay = mailRelay;
  }

  /**
   * Provides the administrative email account name.
   * 
   * @return a String with the administrative email account name.
   */
  public String getAdminEmail() {
    return adminEmail;
  }
  public void setAdminEmail(String adminEmail) {
    this.adminEmail = adminEmail;
  }

  /**
   * Provides the disk labels.
   * 
   * @return a List<String> with the disks labels.
   */
  public List<String> getDisks() {
    return disks;
  }
  public void setDisks(List<String> disks) {
    this.disks = disks;
  }

  /**
   * Provides the current timestamp.
   * 
   * @return a long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public long getCurrentTime() {
    return currentTime;
  }
  public void setCurrentTime(long currentTime) {
    this.currentTime = currentTime;
  }

  /**
   * Provides the server uptime.
   * 
   * @return a long with the uptime in millisecons.
   */
  public long getUptime() {
    return uptime;
  }
  public void setUptime(long uptime) {
    this.uptime = uptime;
  }

  /**
   * Provides the daemon version information.
   * 
   * @return a DaemonVersionWsResult with the daemon version.
   */
  public DaemonVersionWsResult getDaemonVersion() {
    return daemonVersion;
  }
  public void setDaemonVersion(DaemonVersionWsResult daemonVersion) {
    this.daemonVersion = daemonVersion;
  }

  /**
   * Provides the java version information.
   * 
   * @return a JavaVersionWsResult with the java version.
   */
  public JavaVersionWsResult getJavaVersion() {
    return javaVersion;
  }
  public void setJavaVersion(JavaVersionWsResult javaVersion) {
    this.javaVersion = javaVersion;
  }

  /**
   * Provides the platform information.
   * 
   * @return a PlatformWsResult with the platform information.
   */
  public PlatformWsResult getPlatform() {
    return platform;
  }
  public void setPlatform(PlatformWsResult platform) {
    this.platform = platform;
  }

  /**
   * Provides the current working directory.
   * 
   * @return a String with the current working directory.
   */
  public String getCurrentWorkingDirectory() {
    return currentWorkingDirectory;
  }
  public void setCurrentWorkingDirectory(String currentWorkingDirectory) {
    this.currentWorkingDirectory = currentWorkingDirectory;
  }

  /**
   * Provides the daemon properties.
   * 
   * @return a List<String> with the properties.
   */
  public List<String> getProperties() {
    return properties;
  }
  public void setProperties(List<String> properties) {
    this.properties = properties;
  }

  /**
   * Provides the build host name.
   * 
   * @return a String with the build host name.
   */
  public String getBuildHost() {
    return buildHost;
  }
  public void setBuildHost(String buildHost) {
    this.buildHost = buildHost;
  }

  /**
   * Provides the build timestamp.
   * 
   * @return a long with the timestamp as the number of milliseconds since the
   *         beginning of 1970.
   */
  public long getBuildTimestamp() {
    return buildTimestamp;
  }
  public void setBuildTimestamp(long buildTimestamp) {
    this.buildTimestamp = buildTimestamp;
  }

  @Override
  public String toString() {
    return "[PlatformConfigurationWsResult hostName=" + hostName
	+ ", ipAddress=" + ipAddress + ", groups=" + groups + ", project="
	+ project + ", v3Identity=" + v3Identity + ", mailRelay=" + mailRelay
	+ ", adminEmail=" + adminEmail + ", disks=" + disks + ", currentTime="
	+ currentTime + ", uptime=" + uptime
        + ", daemonVersion=" + daemonVersion
        + ", javaVersion=" + javaVersion
        + ", platform=" + platform
	+ ", currentWorkingDirectory=" + currentWorkingDirectory
	+ ", properties=" + properties + ", buildHost=" + buildHost
	+ ", buildTimestamp=" + buildTimestamp + "]";
  }
}
