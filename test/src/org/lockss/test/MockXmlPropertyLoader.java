/*

Copyright (c) 2001-2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.test;

import java.util.*;
import org.lockss.util.*;
import org.lockss.app.*;

public class MockXmlPropertyLoader extends XmlPropertyLoader {

  // Override getXXXVersion() methods to return known values for
  // unit testing.
  private Version m_daemonVersion;
  private PlatformVersion m_platformVersion;
  private String m_hostname;
  private String m_hostIP;
  private String m_groups;

  public void setVersions(String daemonVersion, String platformVersion,
			  String hostname, String groups) {
    if (daemonVersion == null) {
      this.m_daemonVersion = null;
    } else {
      this.m_daemonVersion = new DaemonVersion(daemonVersion);
    }

    if (platformVersion == null) {
      this.m_platformVersion = null;
    } else {
      this.m_platformVersion = new PlatformVersion(platformVersion);
    }

    this.m_hostname = hostname;
    this.m_groups = groups;
  }

  public MockXmlPropertyLoader setDaemonVersion(String val) {
    if (val == null) {
      m_daemonVersion = null;
    } else {
      m_daemonVersion = new DaemonVersion(val);
    }
    return this;
  }

  @Override
  public Version getDaemonVersion() {
    return m_daemonVersion;
  }

  public MockXmlPropertyLoader setPlatformVersion(String val) {
    if (val == null) {
      m_platformVersion = null;
    } else {
      m_platformVersion = new PlatformVersion(val);
    }
    return this;
  }

  @Override
  public PlatformVersion getPlatformVersion() {
    return m_platformVersion;
  }

  public MockXmlPropertyLoader setPlatformHostname(String val) {
    m_hostname = val;
    return this;
  }

  @Override
  public String getPlatformHostname() {
    return m_hostname;
  }

  public MockXmlPropertyLoader setPlatformHostIP(String val) {
    m_hostIP = val;
    return this;
  }

  @Override
  public String getPlatformHostIP() {
    return m_hostIP;
  }

  public MockXmlPropertyLoader setPlatformGroups(String val) {
    m_groups = val;
    return this;
  }

  @Override
  public List<String> getPlatformGroupList() {
    return StringUtil.breakAt(m_groups, ';');
  }

  public MockXmlPropertyLoader setServiceDescr(ServiceDescr descr) {
    m_serviceDescr = descr;
    return this;
  }

  @Override
  protected ServiceDescr getServiceDescr() {
    if (m_serviceDescr != null) {
      return m_serviceDescr;
    }
    return super.getServiceDescr();
  }
}
