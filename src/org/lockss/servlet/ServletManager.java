/*
 * $Id: ServletManager.java,v 1.38.14.1 2009-11-03 23:44:52 edwardsb1 Exp $
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

import java.io.*;
import java.net.*;
import java.util.*;
import org.lockss.app.*;
import org.lockss.config.Configuration;
import org.lockss.daemon.*;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.mortbay.http.*;
import org.mortbay.http.Authenticator;
import org.mortbay.http.handler.*;
import org.mortbay.jetty.servlet.*;

/**
 * Interface for servlet managers
 */
public interface ServletManager extends LockssManager {

  public static final String SUFFIX_IP_PLATFORM_SUBNET =
    IpAccessControl.SUFFIX_PLATFORM_ACCESS;

  /** Username established during platform config */
  public static final String PARAM_PLATFORM_USERNAME =
    Configuration.PLATFORM + "ui.username";

  /** Password established during platform config */
  public static final String PARAM_PLATFORM_PASSWORD =
    Configuration.PLATFORM + "ui.password";

  /** HttpContext attribute holding LockssApp (daemon) instance */
  public static final String CONTEXT_ATTR_LOCKSS_APP = "LockssApp";

  /** HttpContext attribute holding ServletManager instance that started
   * context */
  public static final String CONTEXT_ATTR_SERVLET_MGR = "ServletMgr";

  /** HttpContext attribute holding ServletHandler instance in context */
  public static final String CONTEXT_ATTR_SERVLET_HANDLER = "ServletHandler";

  /** HttpContext attribute holding resource handler redirect map in context */
  public static final String CONTEXT_ATTR_RESOURCE_REDIRECT_MAP = "RedirectMap";

  public ServletDescr[] getServletDescrs();
  public ServletDescr findServletDescr(Object o);
  public Authenticator getAuthenticator();
}
