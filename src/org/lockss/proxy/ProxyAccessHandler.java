/*
 * $Id: ProxyAccessHandler.java,v 1.1 2004-02-10 07:52:37 tlipkis Exp $
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
  private static Logger log = Logger.getLogger("ProxyAccess");
  private LockssDaemon daemon;
  private PluginManager pluginMgr = null;
  private IdentityManager idMgr;

  public ProxyAccessHandler(LockssDaemon daemon, String serverName) {
    super(serverName);
    this.daemon = daemon;
    pluginMgr = daemon.getPluginManager();
    idMgr = daemon.getIdentityManager();
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
      String userAgent = request.getField("user-agent");
      if (log.isDebug2()) {
	log.debug2("user-agent: " + userAgent);
      }
      boolean isRepairRequest =
	StringUtil.equalStrings(userAgent, LockssDaemon.getUserAgent());

      if (!isRepairRequest) {
	// Not a repair request from a LOCKSS cache, let the parent
	// IpAccessHandler handle it.
	super.handle(pathInContext, pathParams, request, response);
      } else {
	// This is a repair request from a LOCKSS cache.  If we don't have
	// the URL locally, return a 404.  If we have it, allow the request
	// (passing it on to the ProxyHandler) iff the requestor previously
	// proved that it had the content by agreeing with us in a poll,
	// else return a 403 (forbidden).

	org.mortbay.util.URI uri = request.getURI();
	String urlString = uri.toString();
	CachedUrl cu = pluginMgr.findMostRecentCachedUrl(urlString);
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
	Map agreeMap = idMgr.getAgreed(au);
	boolean didAgree = agreeMap != null && agreeMap.containsKey(ip);
	if (didAgree) {
	  // allow the request to be processed by the ProxyHandler
	  return;
	} else {
	  if (isLogForbidden()) {
	    log.info("Not serving repair of " + cu + " to " + ip +
		     " because it never agreed with us.");
	  }
	  response.sendError(HttpResponse.__403_Forbidden);
	  request.setHandled(true);
	  return;
	}
      }
    } catch (Exception e) {
      log.warning("Error checking proxy access control", e);
      response.sendError(HttpResponse.__500_Internal_Server_Error);
      request.setHandled(true);
    }
  }
}
