/*
 * $Id: ServletDescr.java,v 1.13 2008-08-17 08:48:00 tlipkis Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.servlet;

public class ServletDescr {

  /** Marker for servlets whose class can't be found */
  static class UnavailableServletMarker {}

  public String servletName;

  public Class cls;

  public String heading;	// display name

  public String path;		// url path component to invoke servlet

  public String expl;

  public int flags = 0;

  static Class UNAVAILABLE_SERVLET_MARKER = UnavailableServletMarker.class;

  // flags
  /** Include link in nav table */
  public static final int IN_NAV = 0x04;

  /** Include link in UI home page */
  public static final int IN_UIHOME = 0x08;

  /** Use large LOCKSS logo */
  public static final int LARGE_LOGO = 0x10;

  /** The servlet path is actually the entire URL that should appear in
   * links */
  public static final int PATH_IS_URL = 0x20;

  /** User role: Debug user only */
  public static final int DEBUG_ONLY = 0x1000;

  /** User role: Admin (read/write) user only */
  public static final int ADMIN_ONLY = 0x2000;



  public ServletDescr(String servletName,
		      Class cls,
                      String heading,
                      String path,
                      int flags) {
    this.servletName = servletName;
    this.cls = cls;
    this.heading = heading;
    this.path = path;
    this.flags = flags;
  }

  public ServletDescr(String servletName,
		      Class cls,
                      String heading,
                      String path,
                      int flags,
                      String expl) {
    this(servletName,
	 cls,
         heading,
         path,
         flags);
    setExplanation(expl);
  }

  public ServletDescr(String servletName,
		      Class cls,
                      String heading,
                      int flags) {
    this(servletName,
	 cls,
         heading,
         cls.getName().substring(cls.getName().lastIndexOf('.') + 1),
         flags);
  }

  public ServletDescr(String servletName,
		      Class cls,
                      String heading,
                      int flags,
                      String expl) {
    this(servletName,
	 cls,
         heading,
         flags);
    setExplanation(expl);
  }


  public ServletDescr(String servletName,
		      String className,
                      String heading,
                      int flags) {
    this(servletName,
	 classForName(className),
         heading,
         flags);
  }

  public ServletDescr(String servletName,
		      String className,
                      String heading,
                      int flags,
                      String expl) {
    this(servletName,
	 className,
         heading,
         flags);
    setExplanation(expl);
  }

  public ServletDescr(String servletName,
		      Class cls,
                      String heading) {
    this(servletName,
	 cls,
         heading,
         0);
  }

  public ServletDescr(String servletName,
		      Class cls,
                      String heading,
                      String expl) {
    this(servletName,
	 cls,
         heading);
    setExplanation(expl);
  }


  static Class classForName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      return ServletDescr.UNAVAILABLE_SERVLET_MARKER;
    }
  }

  public String getServletName() {
    return servletName;
  }

  public Class getServletClass() {
    return cls;
  }

  public String getPath() {
    return path;
  }

  String getExplanation() {
    return expl;
  }

  void setExplanation(String s) {
    expl = s;
  }

  boolean isDebugOnly() {
    return (flags & DEBUG_ONLY) != 0;
  }

  boolean isAdminOnly() {
    return (flags & ADMIN_ONLY) != 0;
  }

  boolean isLargeLogo() {
    return (flags & LARGE_LOGO) != 0;
  }

  boolean isPathIsUrl() {
    return (flags & PATH_IS_URL) != 0;
  }

  /** return true if servlet should be in the nav table of ofServlet */
  public boolean isInNav(LockssServlet ofServlet) {
    return isFlagSet(IN_NAV);
  }

  /** return true if servlet should be in UI home page */
  public boolean isInUiHome(LockssServlet uiHomeServlet) {
    return isFlagSet(IN_UIHOME);
  }

  boolean isFlagSet(int flag) {
    return (flags & flag) != 0;
  }

  private boolean isFlagClear(int flag) {
    return (flags & flag) == 0;
  }

  public String toString() {
    String name = getServletName();
    return "[ServletDescr: " + (name != null ? name : getPath()) + "]";
  }
}
