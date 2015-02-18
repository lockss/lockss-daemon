/*
 * $Id$
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

package org.lockss.proxy;

import java.io.*;
import java.util.*;
import org.mortbay.http.*;
import org.mortbay.http.handler.*;
import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.jetty.*;
import org.lockss.plugin.*;
import org.lockss.protocol.*;

/** Extension of IpAccessHandler that also allows fetches from any cache
 * that has proved it previous had the content it is trying to fetch. */
public class ProxyAccessHandler extends IpAccessHandler {
// A single handler instance may run concurrently in multiple threads, so
// there must not be any per-request local state.

  private static Logger log = Logger.getLogger("ProxyAccess");
  private final LockssDaemon daemon;
  private final PluginManager pluginMgr;
  private final IdentityManager idMgr;
  private final ProxyManager proxyMgr;

  public ProxyAccessHandler(LockssDaemon daemon, String serverName) {
    super(serverName);
    this.daemon = daemon;
    pluginMgr = daemon.getPluginManager();
    idMgr = daemon.getIdentityManager();
    proxyMgr = daemon.getProxyManager();
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
      boolean isRepairRequest = proxyMgr.isRepairRequest(request);

      if (!isRepairRequest) {
	// Not a repair request from a LOCKSS cache, let the parent
	// IpAccessHandler handle it.
	log.debug2("Passing proxy request to parent class");
	super.handle(pathInContext, pathParams, request, response);
      } else {
	log.debug2("Repair request from a LOCKSS cache");
	// This is a repair request from a LOCKSS cache.  If we don't have
	// the URL locally, return a 404.  If we have it, allow the request
	// (passing it on to the ProxyHandler) iff the requestor previously
	// proved that it had the content by agreeing with us in a poll,
	// else return a 403 (forbidden).

	org.mortbay.util.URI uri = request.getURI();
	String urlString = uri.toString();
	// XXX this should check all AUs containing URL for *any* with
	// legal access, then should pass cuurl to next handler
	CachedUrl cu = pluginMgr.findCachedUrl(urlString);
	try {
	  if (log.isDebug2()) {
	    log.debug2("cu: " + cu);
	  }
	  if (cu == null || !cu.hasContent()) {
	    response.sendError(HttpResponse.__404_Not_Found);
	    request.setHandled(true);
	    return;
	  }
	  ArchivalUnit au = cu.getArchivalUnit();
	  String ip = request.getRemoteAddr();
	  if (IPAddr.isLoopbackAddress(ip)) {
	    String id = request.getField(Constants.X_LOCKSS_REAL_ID);
	    if (!StringUtil.isNullString(id)) {
	      log.info("Repair req from loopback (" + ip + "), using id " +
		       id);
	      ip = id;
	    }
	  }
	  if (idMgr.hasAgreed(ip, au)) {
	    // Allow the request to be processed by the ProxyHandler.
	    log.debug3("Found "+ip+" in agree map");
	    return;
	  } else {
	    if (log.isDebug3()) {
	      log.debug3("Agree map: "+idMgr.getAgreed(au));
	    }
	    if (isLogForbidden()) {
	      log.info("Not serving repair of " + cu + " to " + ip +
		       " because it never agreed with us.");
	    }
	    response.sendError(HttpResponse.__403_Forbidden);
	    request.setHandled(true);
	    return;
	  }
	} finally {
	  AuUtil.safeRelease(cu);
	}
      }
    } catch (Exception e) {
      log.warning("Error checking proxy access control", e);
      response.sendError(HttpResponse.__500_Internal_Server_Error);
      request.setHandled(true);
    }
  }
}
