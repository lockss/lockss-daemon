/*
 * $Id: IpAccessHandler.java,v 1.7 2007-11-06 07:09:17 tlipkis Exp $
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
// ===========================================================================
// Copyright (c) 1996-2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id: IpAccessHandler.java,v 1.7 2007-11-06 07:09:17 tlipkis Exp $
// ---------------------------------------------------------------------------

package org.lockss.jetty;

import java.io.*;
import java.util.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.lockss.util.*;

/** Handler that disallows access from IP addresses not allowed by an
 * IpFilter */
public class IpAccessHandler extends AbstractHttpHandler {
// A single handler instance may run concurrently in multiple threads, so
// there must not be any per-request local state.

  private static Logger log = Logger.getLogger("IpAccess");

  private IpFilter filter = new IpFilter();
  private String serverName;
  private boolean allowLocal = false;
  private Set localIps;
  private boolean logForbidden;
  private String _403Msg;

  public IpAccessHandler(String serverName) {
    this.serverName = serverName;
  }

  public void setFilter(IpFilter filter) {
    this.filter = filter;
  }

  public void setLogForbidden(boolean logForbidden) {
    this.logForbidden = logForbidden;
  }

  protected boolean isLogForbidden() {
    return logForbidden;
  }

  public void setAllowLocal(boolean allowLocal) {
    if (localIps == null) {
      HashSet set = new HashSet();
      set.add("127.0.0.1");
      localIps = set;			// set atomically
      // tk - add local interfaces
    }
    this.allowLocal = allowLocal;
  }

  public void set403Msg(String text) {
    _403Msg = text;
  }

  public boolean isIpAuthorized(String ip) throws IpFilter.MalformedException {
    return (filter.isIpAllowed(ip) || (allowLocal && localIps.contains(ip)));
  }

  /**
   * Handles the incoming request
   *
   * @param pathInContext
   * @param pathParams
   * @param request	The incoming HTTP-request
   * @param response	The outgoing HTTP-response
   */
  public void handle(String pathInContext,
		     String pathParams,
		     HttpRequest request,
		     HttpResponse response)
      throws HttpException, IOException {

    try	{
      String ip = request.getRemoteAddr();
      boolean authorized = isIpAuthorized(ip);

      if (!authorized) {
	// The IP is NOT allowed
	if (logForbidden) {
	  log.info("Access to " + serverName + " forbidden from " + ip);
	}
	if (_403Msg != null) {
	  response.sendError(HttpResponse.__403_Forbidden,
			     StringUtil.replaceString(_403Msg, "%IP%", ip));
	} else {
	  response.sendError(HttpResponse.__403_Forbidden);
	}
	request.setHandled(true);
	return;
      } else {
	// The IP is allowed
	return;
      }
    } catch (Exception e) {
      log.warning("Error checking IP", e);
      response.sendError(HttpResponse.__500_Internal_Server_Error);
      request.setHandled(true);
    }
  }
}
