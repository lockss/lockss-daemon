/*
 * $Id: MDHashUserRealm.java,v 1.3.68.1 2009-11-03 23:44:51 edwardsb1 Exp $
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

// Largely copied from Jetty's HashUserRealm.  Only the put() method is
// different (in order to use a different Credential factory), but can't
// subclass HashUserRealm because needs access to User and KnownUser, which
// are private

// ========================================================================
// $Id: MDHashUserRealm.java,v 1.3.68.1 2009-11-03 23:44:51 edwardsb1 Exp $
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------

package org.lockss.jetty;

import java.io.*;
import java.util.*;
import java.security.Principal;

import org.mortbay.http.*;
import org.mortbay.util.*;
import org.lockss.util.*;

/** Similar to Jetty's HashUserRealm, but expects credential strings
 * suitable for MDCredential, <i>ie</i>, ALG:DIGEST , where ALG is a
 * known MessageDigest algorithm name (eg "MD5" or "SHA1") and DIGEST is
 * the result of digesting the credential
 */
public class MDHashUserRealm extends HashMap
  implements UserRealm, SSORealm, Externalizable {

  private static Logger log = Logger.getLogger("MDHashUserRealm");

  /* ------------------------------------------------------------ */
  private String _realmName;
  private String _config;
  protected Map<String,Set> _roles=new HashMap<String,Set>(7);
  private SSORealm _ssoRealm;


  /* ------------------------------------------------------------ */
  /** Constructor.
   */
  public MDHashUserRealm()
  {}

  /* ------------------------------------------------------------ */
  /** Constructor.
   * @param name Realm Name
   */
  public MDHashUserRealm(String name)
  {
    _realmName=name;
  }

  /* ------------------------------------------------------------ */
  /** Constructor.
   * @param name Realm name
   * @param config Filename or url of user properties file.
   */
  public MDHashUserRealm(String name, String config)
      throws IOException
  {
    _realmName=name;
    load(config);
  }

  /* ------------------------------------------------------------ */
  public void writeExternal(java.io.ObjectOutput out)
      throws java.io.IOException
  {
    out.writeObject(_realmName);
    out.writeObject(_config);
  }

  /* ------------------------------------------------------------ */
  public void readExternal(java.io.ObjectInput in)
      throws java.io.IOException, ClassNotFoundException
  {
    _realmName= (String)in.readObject();
    _config=(String)in.readObject();
    if (_config!=null)
      load(_config);
  }


  /* ------------------------------------------------------------ */
  /** Load realm users from properties file.
   * The property file maps usernames to password specs followed by
   * an optional comma separated list of role names.
   *
   * @param config Filename or url of user properties file.
   * @exception IOException
   */
  public void load(String config)
      throws IOException
  {
    _config=config;
    if(log.isDebug())log.debug("Load "+this+" from "+config);
    Properties properties = new Properties();
    Resource resource=Resource.newResource(config);
    properties.load(resource.getInputStream());

    Iterator iter = properties.entrySet().iterator();
    while(iter.hasNext())
      {
        Map.Entry entry = (Map.Entry)iter.next();

        String username=entry.getKey().toString().trim();
        String credentials=entry.getValue().toString().trim();
        String roles=null;
        int c=credentials.indexOf(',');
        if (c>0)
          {
            roles=credentials.substring(c+1).trim();
            credentials=credentials.substring(0,c).trim();
          }

        if (username!=null && username.length()>0 &&
            credentials!=null && credentials.length()>0)
          {
            put(username,credentials);
            if(roles!=null && roles.length()>0)
              {
                StringTokenizer tok = new StringTokenizer(roles,", ");
                while (tok.hasMoreTokens())
                  addUserToRole(username,tok.nextToken());
              }
          }
      }
  }

  /* ------------------------------------------------------------ */
  /**
   * @param name The realm name
   */
  public void setName(String name)
  {
    _realmName=name;
  }

  /* ------------------------------------------------------------ */
  /**
   * @return The realm name.
   */
  public String getName()
  {
    return _realmName;
  }

  /* ------------------------------------------------------------ */
  public Principal getPrincipal(String username)
  {
    return (Principal)super.get(username);
  }

  /* ------------------------------------------------------------ */
  public Principal authenticate(String username,
                                Object credentials,
                                HttpRequest request)
  {
    if (log.isDebug2()) {
      log.debug2("authenticate("+username+", "+credentials+")");
    }
    KnownUser user;
    synchronized (this)
      {
        user = (KnownUser)super.get(username);
      }
    if (user==null)
      return null;

    if (user.authenticate(credentials))
      return user;

    return null;
  }

  /* ------------------------------------------------------------ */
  public void disassociate(Principal user)
  {
  }

  /* ------------------------------------------------------------ */
  public Principal pushRole(Principal user, String role)
  {
    if (user==null)
      user=new User();

    return new WrappedUser(user,role);
  }

  /* ------------------------------------------------------------ */
  public Principal popRole(Principal user)
  {
    WrappedUser wu = (WrappedUser)user;
    return wu.getUserPrincipal();
  }

  /* ------------------------------------------------------------ */
  /** Put user into realm.  If the credential cannot be created
   * (<i>eg</i>, no such algorithm), the user is removed from the realm.
   * (This put method cannot throw, and if we can't create an accurate
   * credential the safest thing is to make this user always fail.)
   * @param name User name
   * @param credentials String type:digest, or UserPrinciple
   *                    instance.
   * @return Old UserPrinciple value or null
   */
  public synchronized Object put(Object name, Object credentials)
  {
    if (credentials instanceof Principal)
      return super.put(name.toString(),
                       credentials);

    if (credentials != null) {
      try {
        Credential cred =
          MDCredential.makeCredential(credentials.toString());
        return super.put(name, new KnownUser(name.toString(), cred));
      } catch (Exception e) {
        log.warning("Disabling user " + name +
                    ", can't create credential " + credentials, e);
        return super.remove(name);
      }
    }
    return null;
  }

  /* ------------------------------------------------------------ */
  /** Add a user to a role.
   * @param userName
   * @param roleName
   */
  public synchronized void addUserToRole(String userName, String roleName)
  {
    Set userSet = _roles.get(roleName);
    if (userSet==null)
      {
        userSet=new HashSet(11);
        _roles.put(roleName,userSet);
      }
    userSet.add(userName);
  }

  /* -------------------------------------------------------- */
  public boolean reauthenticate(Principal user)
  {
    return ((User)user).isAuthenticated();
  }

  /* ------------------------------------------------------------ */
  /** Check if a user is in a role.
   * @param user The user, which must be from this realm
   * @param roleName
   * @return True if the user can act in the role.
   */
  public synchronized boolean isUserInRole(Principal user, String roleName)
  {
    if (user instanceof WrappedUser)
      return ((WrappedUser)user).isUserInRole(roleName);

    if (user==null || ((User)user).getUserRealm()!=this)
      return false;

    Set userSet = _roles.get(roleName);
    return userSet!=null && userSet.contains(user.getName());
  }

  /* ------------------------------------------------------------ */
  public void logout(Principal user)
  {}

  /* ------------------------------------------------------------ */
  public String toString()
  {
    return "Realm["+_realmName+"]";
  }

  /* ------------------------------------------------------------ */
  public void dump(PrintStream out)
  {
    out.println(this+":");
    out.println(super.toString());
    out.println(_roles);
  }

  /* ------------------------------------------------------------ */
  /**
   * @return The SSORealm to delegate single sign on requests to.
   */
  public SSORealm getSSORealm()
  {
    return _ssoRealm;
  }

  /* ------------------------------------------------------------ */
  /** Set the SSORealm.
   * A SSORealm implementation may be set to enable support for SSO.
   * @param ssoRealm The SSORealm to delegate single sign on requests to.
   */
  public void setSSORealm(SSORealm ssoRealm)
  {
    _ssoRealm = ssoRealm;
  }

  /* ------------------------------------------------------------ */
  public Credential getSingleSignOn(HttpRequest request,
                                    HttpResponse response)
  {
    if (_ssoRealm!=null)
      return _ssoRealm.getSingleSignOn(request,response);
    return null;
  }


  /* ------------------------------------------------------------ */
  public void setSingleSignOn(HttpRequest request,
                              HttpResponse response,
                              Principal principal,
                              Credential credential)
  {
    if (_ssoRealm!=null)
      _ssoRealm.setSingleSignOn(request,response,principal,credential);
  }

  /* ------------------------------------------------------------ */
  public void clearSingleSignOn(String username)
  {
    if (_ssoRealm!=null)
      _ssoRealm.clearSingleSignOn(username);
  }

  /* ------------------------------------------------------------ */
  /* ------------------------------------------------------------ */
  /* ------------------------------------------------------------ */
  private class User implements Principal
  {

    /* ------------------------------------------------------------ */
    private UserRealm getUserRealm()
    {
      return MDHashUserRealm.this;
    }

    public String getName()
    {
      return "Anonymous";
    }

    public boolean isAuthenticated()
    {
      return false;
    }

    public String toString()
    {
      return getName();
    }
  }

  /* ------------------------------------------------------------ */
  /* ------------------------------------------------------------ */
  /* ------------------------------------------------------------ */
  private class KnownUser extends User
  {
    private String _userName;
    private Credential _cred;

    /* -------------------------------------------------------- */
    KnownUser(String name,Credential credential)
    {
      _userName=name;
      _cred=credential;
    }

    /* -------------------------------------------------------- */
    boolean authenticate(Object credentials)
    {
      return _cred!=null && _cred.check(credentials);
    }

    /* ------------------------------------------------------------ */
    public String getName()
    {
      return _userName;
    }

    /* -------------------------------------------------------- */
    public boolean isAuthenticated()
    {
      return true;
    }
  }

  /* ------------------------------------------------------------ */
  /* ------------------------------------------------------------ */
  /* ------------------------------------------------------------ */
  private class WrappedUser extends User
  {
    private Principal user;
    private String role;

    WrappedUser(Principal user, String role)
    {
      this.user=user;
      this.role=role;
    }

    Principal getUserPrincipal()
    {
      return user;
    }

    public String getName()
    {
      return "role:"+role;
    }

    public boolean isAuthenticated()
    {
      return true;
    }

    public boolean isUserInRole(String role)
    {
      return this.role.equals(role);
    }
  }
}
