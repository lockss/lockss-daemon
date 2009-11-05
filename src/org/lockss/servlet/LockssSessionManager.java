/*
 * $Id: LockssSessionManager.java,v 1.4 2009-11-05 23:38:39 dshr Exp $
 */

/*

Copyright (c) 2000-2009 Board of Trustees of Leland Stanford Jr. University,
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
// Some portions of this code are:
// ========================================================================
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
// ========================================================================

package org.lockss.servlet;

import java.util.*;
import org.mortbay.jetty.servlet.*;
import javax.servlet.http.*;
import org.lockss.util.*;
import org.lockss.account.*;
import org.lockss.jetty.*;


/** Derived from Jetty SessionManager with some extra LOCKSS fetaures
 */
public class LockssSessionManager extends AbstractSessionManager {
  private static Logger log = Logger.getLogger("LockssSessionManager");

  public LockssSessionManager() {
    super();
  }

  public LockssSessionManager(Random random) {
    super(random);
  }

  public HttpSession getHttpSession(String id) {
    HttpSession res = super.getHttpSession(id);
    if (log.isDebug2()) log.debug2("getHttpSession("+id+"): " + res);
    return res;
  }

  public static boolean isAuthenticated(HttpSession sess) {
    return sess.getAttribute(LockssFormAuthenticator.__J_AUTHENTICATED) != null;
  }

  public static UserAccount getUserAccount(HttpSession sess) {
    Object ret = sess.getAttribute(LockssFormAuthenticator.__J_LOCKSS_USER);
    return (UserAccount)ret;
  }

  protected AbstractSessionManager.Session newSession(HttpServletRequest request) {
    return new Session(request);
  }

  /** Collect info about all active user sessions */
  public Collection<UserSession> getUserSessions() {
    List<UserSession> res = new ArrayList<UserSession>();
    for (Map.Entry ent : (Set<Map.Entry>)_sessions.entrySet()) {
      HttpSession sess = (HttpSession)ent.getValue();
      // Process only authenticated sessions that won't be logged out due
      // to inactivity on their next use
      if (isAuthenticated(sess) && !isInactiveTimeout(sess)) {
	UserAccount acct = getUserAccount(sess);
	if (acct != null) {
	  UserSession usess = new UserSession(acct.getName());
	  usess.setLoginTime((Long)sess.getAttribute(LockssFormAuthenticator.__J_LOGIN_TIME));
	  usess.setRunningServlet((String)sess.getAttribute(LockssServlet.SESSION_KEY_RUNNING_SERVLET));
	  usess.setReqHost((String)sess.getAttribute(LockssServlet.SESSION_KEY_REQUEST_HOST));

	  if (!StringUtil.isNullString((String)sess.getAttribute(LockssServlet.SESSION_KEY_RUNNING_SERVLET))) {
	    usess.setIdleTime(0);
	  } else {
	    Long active =
	      (Long)sess.getAttribute(LockssFormAuthenticator.__J_AUTH_ACTIVITY);
	    if (active != null) {
	      usess.setIdleTime(TimeBase.msSince(active.longValue()));
	    }
	  }
	  res.add(usess);
	}
      }
    }
    return res;
  }

  /** Collect info about user sessions that are idle too long */
  public Collection<HttpSession> getZombieSessions() {
    List<HttpSession> res = new ArrayList<HttpSession>();
    for (Map.Entry ent : (Set<Map.Entry>)_sessions.entrySet()) {
      HttpSession sess = (HttpSession)ent.getValue();
      // Process only authenticated sessions that will be logged out due
      // to inactivity on their next use
      if (isAuthenticated(sess) && isInactiveTimeout(sess)) {
	res.add(sess);
      }
    }
    return res;
  }

  protected class Session extends AbstractSessionManager.Session {

    protected Session(HttpServletRequest request) {
      super(request);
    }

    protected Map newAttributeMap() {
      return new HashMap(3);
    }
  }

  public static boolean isInactiveTimeout(HttpSession session) {
    if (!StringUtil.isNullString((String)session.getAttribute(LockssServlet.SESSION_KEY_RUNNING_SERVLET))) {
      return false;
    }

    Long active =
      (Long)session.getAttribute(LockssFormAuthenticator.__J_AUTH_ACTIVITY);
    Long maxInactivity =
      (Long)session.getAttribute(LockssFormAuthenticator.__J_AUTH_MAX_INACTIVITY);
    if (active != null && maxInactivity != null) {
      return TimeBase.msSince(active.longValue()) > maxInactivity.longValue();
    }
    return false;
  }

}
