/*
 * $Id: PlatformVersion.java,v 1.1 2004-05-28 04:57:31 smorabito Exp $
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

/**
 * Representation of a platform version.  Currently this is a string
 * in the format of an integer, i.e. "150".
 */
public class PlatformVersion extends Version {
  
  // The platform version as an integer.
  private int m_versionInt;
  
  /**
   * Construct a Platform Version from a string.
   */
  public PlatformVersion(String ver) {
    try {
      m_versionInt = Integer.parseInt(ver);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Illegal format for Platform Version: " +
        ver);
    }
  }
  
  public boolean equals(Object o) {
    if (this == o) return true;
    if ((o == null) || (o.getClass() != this.getClass())) return false;
    
    PlatformVersion v = (PlatformVersion)o;
    
    return (this.m_versionInt == v.m_versionInt);
  }
  
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + m_versionInt;
    return hash;      
  }
  
  public int compareTo(Object o) throws ClassCastException {
    PlatformVersion other = (PlatformVersion)o;
    if (this.m_versionInt == other.m_versionInt) return 0;
    if (this.m_versionInt < other.m_versionInt) return -1;
    else return 1;
  }

}
