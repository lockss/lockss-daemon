/*
 * $Id: DaemonVersion.java,v 1.1 2004-05-28 04:57:31 smorabito Exp $
 */
                                                                                                          
/*
                                                                                                          
Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.util;

import java.util.StringTokenizer;

/**
 * Representation of a daemon version.  Currently this is a string
 * with three integer parts separated by a period ("."), i.e. "1.1.3".
 */
public class DaemonVersion extends Version {
  
  private int m_versionMajor;
  private int m_versionMinor;
  private int m_versionBuild;
  
  /**
   * Construct a Daemon Version from a string.
   */
  public DaemonVersion(String ver) {
    StringTokenizer st = new StringTokenizer(ver, ".");
    
    if (st.countTokens() != 3) {
      throw new IllegalArgumentException("Illegal format for Daemon Version: " 
        + ver);
    }
    
    try {
      if (st.hasMoreTokens())
	m_versionMajor = Integer.parseInt((String)st.nextToken());
      if (st.hasMoreTokens())
	m_versionMinor = Integer.parseInt((String)st.nextToken());
      if (st.hasMoreTokens())
	m_versionBuild = Integer.parseInt((String)st.nextToken());
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(ex.toString());
    }

  }
  
  public boolean equals(Object o) {
    if (this == o) return true;
    if ((o == null) || (o.getClass() != this.getClass())) return false;
    
    DaemonVersion v = (DaemonVersion)o;
    
    return ((this.m_versionMajor == v.m_versionMajor) &&
	    (this.m_versionMinor == v.m_versionMinor) &&
	    (this.m_versionBuild == v.m_versionBuild));
  }
  
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + m_versionMajor;
    hash = 31 * hash + m_versionMinor;
    hash = 31 * hash + m_versionBuild;
    return hash;      
  }
  
  public int compareTo(Object o) throws ClassCastException {
 
    DaemonVersion other = (DaemonVersion)o;
    
    if (this.m_versionMajor == other.m_versionMajor &&
	this.m_versionMinor == other.m_versionMinor &&
	this.m_versionBuild == other.m_versionBuild) return 0;
    
    if (this.m_versionMajor > other.m_versionMajor) {
      return 1;
    } else if (this.m_versionMajor == other.m_versionMajor && 
	       this.m_versionMinor > other.m_versionMinor) {
      return 1;
    } else if (this.m_versionMajor == other.m_versionMajor &&
	       this.m_versionMinor == other.m_versionMinor &&
	       this.m_versionBuild > other.m_versionBuild) {
      return 1;
    } else {
      return -1;
    }
  } 
}
