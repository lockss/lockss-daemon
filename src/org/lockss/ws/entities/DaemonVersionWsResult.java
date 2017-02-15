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

/**
 * The version information of the daemon.
 */
public class DaemonVersionWsResult {
  private String fullVersion;
  private int majorVersion;
  private int minorVersion;
  private int buildVersion;

  /**
   * Provides the full version of the daemon.
   * 
   * @return a String with the full version.
   */
  public String getFullVersion() {
    return fullVersion;
  }
  public void setFullVersion(String fullVersion) {
    this.fullVersion = fullVersion;
  }

  /**
   * Provides the major version of the daemon.
   * 
   * @return an int with the major version.
   */
  public int getMajorVersion() {
    return majorVersion;
  }
  public void setMajorVersion(int majorVersion) {
    this.majorVersion = majorVersion;
  }

  /**
   * Provides the minor version of the daemon.
   * 
   * @return an int with the minor version.
   */
  public int getMinorVersion() {
    return minorVersion;
  }
  public void setMinorVersion(int minorVersion) {
    this.minorVersion = minorVersion;
  }

  /**
   * Provides the build version of the daemon.
   * 
   * @return an int with the build version.
   */
  public int getBuildVersion() {
    return buildVersion;
  }
  public void setBuildVersion(int buildVersion) {
    this.buildVersion = buildVersion;
  }

  @Override
  public String toString() {
    return "DaemonVersionWsResult [fullVersion=" + fullVersion
	+ ", majorVersion=" + majorVersion + ", minorVersion=" + minorVersion
	+ ", buildVersion=" + buildVersion + "]";
  }
}
