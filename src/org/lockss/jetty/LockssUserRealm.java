/*
 * $Id: LockssUserRealm.java,v 1.1 2009-06-01 07:53:32 tlipkis Exp $
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

// Largely copied from Jetty's HashUserRealm.  Delegates credential
// generation and role check to UserAccount.  Probably, the local User
// classes should be eliminated and UserAccount made to implement
// Principal.  The Credential object could also be eliminated, but I'm not
// sure how thay might interact with Jetty

// ========================================================================
// $Id: LockssUserRealm.java,v 1.1 2009-06-01 07:53:32 tlipkis Exp $
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------

package org.lockss.jetty;

import java.io.*;
import java.util.*;
import java.security.Principal;

import javax.servlet.http.*;
import org.mortbay.http.*;
import org.mortbay.util.*;
import org.mortbay.jetty.servlet.*;
import org.lockss.util.*;
import org.lockss.account.*;

/** Realm wrapper around UserAccount */
public class LockssUserRealm implements UserRealm {
  private static Logger log = Logger.getLogger("LockssUserRealm");

  private AccountManager _acctMgr;
  private String _realmName;
  protected Map<String,Principal> _users = new HashMap<String,Principal>();

  /** Construct new realm
   * @param name Realm Name
   * @param name acctMgr the LOCKSS AccountManager
   */
  public LockssUserRealm(String name, AccountManager acctMgr) {
    _realmName=name;
    _acctMgr = acctMgr;
  }

  /** Set the realm name
   * @param name The realm name
   */
  public void setName(String name) {
    _realmName=name;
  }

  /** Return the realm name
   * @return The realm name.
   */
  public String getName() {
    return _realmName;
  }

  /** Return the Principal object for the named user, creating one if it
   * doesn't exist. */
  public synchronized Principal getPrincipal(String username) {
    Principal p = _users.get(username);
    if (p == null) {
      p = new KnownUser(username);
      _users.put(username, p);
    }
    return p;
  }

  /** Authenticate a user's credentials.
   * @param username The username. 
   * @param credentials The user credentials
   * @param request The request to be authenticated.
   * @return The authenticated UserPrincipal, or null if not authenticated
   */
  public Principal authenticate(String username,
				Object credentials,
				HttpRequest request) {
    if (log.isDebug2()) {
      log.debug2("authenticate("+username+", "+credentials+")");
    }
    KnownUser user = (KnownUser)getPrincipal(username);
    if (user==null) {
      return null;
    }
    if (user.authenticate(credentials, request)) {
      return user;
    }
    return null;
  }

  public void disassociate(Principal user) {
  }

  public Principal pushRole(Principal user, String role) {
    if (user==null) {
      user=new User();
    }
    return new WrappedUser(user,role);
  }

  public Principal popRole(Principal user) {
    WrappedUser wu = (WrappedUser)user;
    return wu.getUserPrincipal();
  }

  public boolean reauthenticate(Principal user) {
    log.info("reauthenticate("+user+")");
    return ((User)user).isAuthenticated();
  }

  /** Check if a user is in a role.
   * @param user The user, which must be from this realm
   * @param roleName
   * @return True if the user can act in the role.
   */
  public synchronized boolean isUserInRole(Principal user, String roleName) {
    if (user instanceof KnownUser) {
      return ((KnownUser)user).isUserInRole(roleName);
    }
    return false;
  }

  public void logout(Principal user) {
  }

  public String toString() {
    return "Realm["+_realmName+"]";
  }

//   /* ------------------------------------------------------------ */
//   /** Load realm users from properties file.
//    * The property file maps usernames to password specs followed by
//    * an optional comma separated list of role names.
//    *
//    * @param config Filename or url of user properties file.
//    * @exception IOException
//    */
//   public void load(String config) throws IOException {
//     _config=config;
//     if(log.isDebug())log.debug("Load "+this+" from "+config);
//     Properties properties = new Properties();
//     Resource resource=Resource.newResource(config);
//     properties.load(resource.getInputStream());

//     Iterator iter = properties.entrySet().iterator();
//     while(iter.hasNext())
//       {
// 	Map.Entry entry = (Map.Entry)iter.next();

// 	String username=entry.getKey().toString().trim();
// 	String credentials=entry.getValue().toString().trim();
// 	String roles=null;
// 	int c=credentials.indexOf(',');
// 	if (c>0)
// 	  {
// 	    roles=credentials.substring(c+1).trim();
// 	    credentials=credentials.substring(0,c).trim();
// 	  }

// 	if (username!=null && username.length()>0 &&
// 	    credentials!=null && credentials.length()>0)
// 	  {
// 	    put(username,credentials);
// 	    if(roles!=null && roles.length()>0)
// 	      {
// 		StringTokenizer tok = new StringTokenizer(roles,", ");
// 		while (tok.hasMoreTokens())
// 		  addUserToRole(username,tok.nextToken());
// 	      }
// 	  }
//       }
//   }

  private class User implements Principal {

    private UserRealm getUserRealm() {
      return LockssUserRealm.this;
    }

    public String getName() {
      return "Anonymous";
    }

    public boolean isAuthenticated() {
      return false;
    }

    public String toString() {
      return getName();
    }
  }

  private class KnownUser extends User {
    private final String _userName;
    private final UserAccount _acct;

    KnownUser(String name) {
      _userName=name;
      _acct = _acctMgr.getUser(name);
    }

    boolean authenticate(Object credentials, HttpRequest request) {
      boolean res;
      String msg = null;
      if (_acct == null) {
	res = false;
      } else {
	res = _acct.check(credentials);
      }
      if (res) {
	if (_acct.hasPasswordExpired()) {
	  msg = "Password expired";
	  res = false;
	}
      }
      HttpServletRequest servletRequest =
	(ServletHttpRequest)request.getWrapper();
      HttpSession session = servletRequest.getSession();
      if (msg != null) {
	if (servletRequest != null) {
	  session.setAttribute(LockssFormAuthenticator.__J_LOCKSS_AUTH_ERROR_MSG,
			       msg);
	}
      }
      return res;
    }

    public String getName() {
      return _userName;
    }

    public boolean isAuthenticated() {
      return _acct != null && _acct.isEnabled();
    }

    public boolean isUserInRole(String role) {
      return _acct != null && _acct.isUserInRole(role);
    }
  }

  private class WrappedUser extends User {
    private Principal _user;
    private String _role;

    WrappedUser(Principal user, String role) {
      _user=user;
      _role=role;
    }

    Principal getUserPrincipal() {
      return _user;
    }

    public String getName() {
      return "role:"+_role;
    }

    public boolean isAuthenticated() {
      return _user instanceof KnownUser && ((KnownUser)_user).isAuthenticated();
    }

    public boolean isUserInRole(String role) {
      return _role.equals(role);
    }
  }
}
