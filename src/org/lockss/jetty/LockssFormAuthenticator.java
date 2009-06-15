/*
 * $Id: LockssFormAuthenticator.java,v 1.1.2.3 2009-06-15 07:47:45 tlipkis Exp $
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

package org.lockss.jetty;

import java.io.*;
import java.security.Principal;

import javax.servlet.http.*;
import org.mortbay.jetty.servlet.*;
import org.mortbay.http.*;
import org.mortbay.util.*;

import org.lockss.app.*;
import org.lockss.util.*;
import org.lockss.util.StringUtil;
import org.lockss.account.*;

/* ------------------------------------------------------------ */
/** FORM Authentication Authenticator, with automatic inactivity logout
 * The HTTP Session is used to store the authentication status of the
 * user, which can be distributed.
 * If the realm implements SSORealm, SSO is supported.
 */
public class LockssFormAuthenticator implements Authenticator {
  static Logger log = Logger.getLogger("LockssFormAuthenticator");

  /* ------------------------------------------------------------ */
  public final static String __J_URI="org.mortbay.jetty.URI";
  public final static String __J_AUTHENTICATED="org.mortbay.jetty.Auth";
  public final static String __J_SECURITY_CHECK="j_security_check";
  public final static String __J_USERNAME="j_username";
  public final static String __J_PASSWORD="j_password";
  public final static String __J_AUTH_ACTIVITY="org.lockss.jetty.AuthActivity";
  public final static String __J_AUTH_MAX_INACTIVITY="org.lockss.jetty.maxInactivity";
  public final static String __J_LOCKSS_USER="org.lockss.jetty.UserAccount";
  public final static String __J_LOCKSS_AUTH_ERROR_MSG="org.lockss.jetty.authErrorMsg";

  private LockssDaemon _daemon;

  private String _formErrorPage;
  private String _formErrorPath;
  private String _formLoginPage;
  private String _formLoginPath;
  private long _maxInactivity = -1;
    
  public LockssFormAuthenticator(LockssDaemon daemon) {
    _daemon = daemon;
  }

  /* ------------------------------------------------------------ */
  public String getAuthMethod()
  {
    return HttpServletRequest.FORM_AUTH;
  }

  /* ------------------------------------------------------------ */
  public void setLoginPage(String path)
  {
    if (!path.startsWith("/"))
      {
	log.warning("form-login-page must start with /");
	path="/"+path;
      }
    _formLoginPage=path;
    _formLoginPath=path;
    if (_formLoginPath.indexOf('?')>0)
      _formLoginPath=_formLoginPath.substring(0,_formLoginPath.indexOf('?'));
  }

  /* ------------------------------------------------------------ */
  public String getLoginPage()
  {
    return _formLoginPage;
  }
    
  /* ------------------------------------------------------------ */
  public void setErrorPage(String path)
  {
    if (path==null || path.trim().length()==0)
      {
	_formErrorPath=null;
	_formErrorPage=null;
      }
    else
      {
	if (!path.startsWith("/"))
	  {
	    log.warning("form-error-page must start with /");
	    path="/"+path;
	  }
	_formErrorPage=path;
	_formErrorPath=path;

	if (_formErrorPath!=null && _formErrorPath.indexOf('?')>0)
	  _formErrorPath=_formErrorPath.substring(0,_formErrorPath.indexOf('?'));
      }
  }    

  /* ------------------------------------------------------------ */
  public long getMaxInactivity()
  {
    return _maxInactivity;
  }
    
  /* ------------------------------------------------------------ */
  public void setMaxInactivity(long time)
  {
    _maxInactivity=time;
  }

  /* ------------------------------------------------------------ */
  void updateActivity(HttpSession session) {
    session.setAttribute(__J_AUTH_ACTIVITY, TimeBase.nowMs());
  }

  /* ------------------------------------------------------------ */
  void checkInactivity(HttpSession session) {
    Long active = (Long)session.getAttribute(__J_AUTH_ACTIVITY);
    Long maxInactivity = (Long)session.getAttribute(__J_AUTH_MAX_INACTIVITY);
    if (active != null && maxInactivity != null) {
      if (TimeBase.msSince(active.longValue()) > maxInactivity.longValue()) {
	logout(session, "Logged out due to inactivity");
      }
    }
  }

  public void logout(HttpSession session) {
    logout(session, null);
  }

  public void logout(HttpSession session, String message) {
    session.setAttribute(__J_LOCKSS_AUTH_ERROR_MSG, message);
    session.setAttribute(__J_AUTHENTICATED, null);
    session.setAttribute(__J_LOCKSS_USER, null);
    session.setAttribute(__J_AUTH_MAX_INACTIVITY, null);
//     session.invalidate();
  }

  /* ------------------------------------------------------------ */
  /** Perform form authentication.
   * Called from SecurityHandler.
   * @return UserPrincipal if authenticated else null.
   */
  public Principal authenticate(UserRealm realm,
				String pathInContext,
				HttpRequest httpRequest,
				HttpResponse httpResponse)
      throws IOException
  {
    HttpServletRequest request =(ServletHttpRequest)httpRequest.getWrapper();
    HttpServletResponse response = httpResponse==null?null:(HttpServletResponse) httpResponse.getWrapper();
        
    // Handle paths
    String uri = pathInContext;

    // Setup session 
    HttpSession session=request.getSession(response!=null);
    if (session==null)
      return null;
        
    // Handle a request for authentication.
    if ( uri.substring(uri.lastIndexOf("/")+1).startsWith(__J_SECURITY_CHECK) )
      {
	// Check the session object for login info.
	FormCredential form_cred=new FormCredential();
	form_cred.authenticate(realm,
			       request.getParameter(__J_USERNAME),
			       request.getParameter(__J_PASSWORD),
			       httpRequest);
            
	String nuri=(String)session.getAttribute(__J_URI);
	if (nuri==null || nuri.length()==0)
	  {
	    nuri=request.getContextPath();
	    if (nuri.length()==0)
	      nuri="/";
	  }
            
	if (form_cred._userPrincipal!=null)
	  {
	    // Authenticated OK
	    log.info("User " + form_cred._jUserName
		     + " authenticated from "
		     + request.getRemoteAddr());
	    session.removeAttribute(__J_URI); // Remove popped return URI.
	    httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
	    httpRequest.setAuthUser(form_cred._jUserName);
	    httpRequest.setUserPrincipal(form_cred._userPrincipal);
	    nowAuthenticated(session, form_cred);

	    // Sign-on to SSO mechanism
	    if (realm instanceof SSORealm)
	      {
		((SSORealm)realm).setSingleSignOn(httpRequest,
						  httpResponse,
						  form_cred._userPrincipal,
						  new Password(form_cred._jPassword));
	      }

	    // Redirect to original request
	    if (response!=null)
	      {
		response.setContentLength(0);
		response.sendRedirect(response.encodeRedirectURL(nuri));
	      }
	  }   
	else if (response!=null)
	  {
	    if(log.isDebug())
	      log.info("User " + form_cred._jUserName +
		       " authentication FAILED from "
		       + request.getRemoteAddr());
	    if (_formErrorPage!=null)
	      {
		response.setContentLength(0);
		response.sendRedirect(response.encodeRedirectURL
				      (URI.addPaths(request.getContextPath(),
						    _formErrorPage)));
	      }
	    else
	      {
		response.sendError(HttpResponse.__403_Forbidden);
	      }
	  }
            
	// Security check is always false, only true after final redirection.
	return null;
      }
        
    checkInactivity(session);

    // Check if the session is already authenticated.
    FormCredential form_cred = (FormCredential) session.getAttribute(__J_AUTHENTICATED);
        
    if (form_cred != null)
      {
	// We have a form credential. Has it been distributed?
	if (form_cred._userPrincipal==null)
	  {
	    // This form_cred appears to have been distributed.  Need to reauth
	    form_cred.authenticate(realm, httpRequest);
                
	    // Sign-on to SSO mechanism
	    if (form_cred._userPrincipal!=null && realm instanceof SSORealm)
	      {
		((SSORealm)realm).setSingleSignOn(httpRequest,
						  httpResponse,
						  form_cred._userPrincipal,
						  new Password(form_cred._jPassword));
	      }
	  }
	else if (!realm.reauthenticate(form_cred._userPrincipal))
	  // Else check that it is still authenticated.
	  form_cred._userPrincipal=null;

	// If this credential is still authenticated
	if (form_cred._userPrincipal!=null)
	  {
	    if(log.isDebug3())
	      log.debug3("User " + form_cred._userPrincipal.getName()
			 + " still authenticated");
	    httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
	    httpRequest.setAuthUser(form_cred._userPrincipal.getName());
	    httpRequest.setUserPrincipal(form_cred._userPrincipal);
	    return form_cred._userPrincipal;
	  }
	else
	  {
	    session.setAttribute(__J_AUTHENTICATED,null);
	    updateActivity(session);
	  }
      }
    else if (realm instanceof SSORealm)
      {
	// Try a single sign on.
	Credential cred = ((SSORealm)realm).getSingleSignOn(httpRequest,httpResponse);
            
	if (httpRequest.hasUserPrincipal())
	  {
	    form_cred=new FormCredential();
	    form_cred._userPrincipal=request.getUserPrincipal();
	    form_cred._jUserName=form_cred._userPrincipal.getName();
	    if (cred!=null)
	      form_cred._jPassword=cred.toString();
	    if(log.isDebug()) log.debug("SSO for "+form_cred._userPrincipal);
                           
	    httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
	    nowAuthenticated(session, form_cred);
	    return form_cred._userPrincipal;
	  }
      }
        
    // Don't authenticate authform or errorpage
    if (isLoginOrErrorPage(pathInContext))
      return SecurityConstraint.__NOBODY;
        
    // redirect to login page
    if (response!=null)
      {
	if (httpRequest.getQuery()!=null)
	  uri+="?"+httpRequest.getQuery();
	session.setAttribute(__J_URI, 
			     request.getScheme() +
			     "://" + request.getServerName() +
			     ":" + request.getServerPort() +
			     URI.addPaths(request.getContextPath(),uri));
	updateActivity(session);
	response.setContentLength(0);
	response.sendRedirect(response.encodeRedirectURL(URI.addPaths(request.getContextPath(),
								      _formLoginPage)));
      }

    return null;
  }

  void nowAuthenticated(HttpSession session, FormCredential form_cred) {
    long maxInact = -1;
    session.setAttribute(__J_AUTHENTICATED, form_cred);
    AccountManager acctMgr = _daemon.getAccountManager();
    if (acctMgr != null) {
      UserAccount acc = acctMgr.getUser(form_cred._userPrincipal.getName());
      if (acc != null) {
	session.setAttribute(__J_LOCKSS_USER, acc);
	maxInact = acc.getInactivityLogout();
      }
    }
    if (maxInact < 0) {
      maxInact = getMaxInactivity();
    }
    if (maxInact > 0) {
      session.setAttribute(__J_AUTH_MAX_INACTIVITY, maxInact);
    }
    updateActivity(session);
  }

  public boolean isLoginOrErrorPage(String pathInContext) {
    boolean res =
      pathInContext!=null &&
      (pathInContext.equals(_formErrorPath)
       || pathInContext.equals(_formLoginPath)
       || pathInContext.startsWith("/images")
       || pathInContext.startsWith("/favicon.ico"));
    if (log.isDebug2())
      log.debug2("isLoginOrErrorPage("+pathInContext+"): " + res);
    return res;
  }
    
  /* ------------------------------------------------------------ */
  /** FORM Authentication credential holder.
   */
  private static class FormCredential implements Serializable, HttpSessionBindingListener
  {
    String _jUserName;
    String _jPassword;
    transient Principal _userPrincipal;
    transient UserRealm _realm;

    void authenticate(UserRealm realm,String user,String password,HttpRequest request)
    {
      _jUserName=user;
      _jPassword=password;
      _userPrincipal = realm.authenticate(user, password, request);
      if (_userPrincipal!=null)
	_realm=realm;
    }

    void authenticate(UserRealm realm,HttpRequest request)
    {
      _userPrincipal = realm.authenticate(_jUserName, _jPassword, request);
      if (_userPrincipal!=null)
	_realm=realm;
    }
        

    public void valueBound(HttpSessionBindingEvent event) {}
        
    public void valueUnbound(HttpSessionBindingEvent event)
    {
      if(log.isDebug())log.debug("Logout "+_jUserName);
            
      if(_realm instanceof SSORealm)
	((SSORealm)_realm).clearSingleSignOn(_jUserName);
               
      if(_realm!=null && _userPrincipal!=null)
	_realm.logout(_userPrincipal); 
    }
        
    public int hashCode()
    {
      return _jUserName.hashCode()+_jPassword.hashCode();
    }

    public boolean equals(Object o)
    {
      if (!(o instanceof FormCredential))
	return false;
      FormCredential fc = (FormCredential)o;
      return
	_jUserName.equals(fc._jUserName) &&
	_jPassword.equals(fc._jPassword);
    }

    public String toString()
    {
      return "Cred["+_jUserName+"]";
    }

  }
}
