/*
 * $Id$
 */

/*

 Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.cxf;

import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.servlet.LockssServlet;
import org.lockss.test.LockssTestCase;

/**
 * Test class for org.lockss.ws.cxf.AuthorizationInterceptor
 * 
 * @author Fernando Garcia-Loygorri
 */
public class TestAuthorizationInterceptor extends LockssTestCase {
  private String[] allowUserAdmin = {LockssServlet.ROLE_USER_ADMIN};
  private String[] allowContentAdmin = {LockssServlet.ROLE_CONTENT_ADMIN};
  private String[] allowAuAdmin = {LockssServlet.ROLE_AU_ADMIN};
  private String[] allowContentAccess = {LockssServlet.ROLE_CONTENT_ACCESS};
  private String[] allowDebug = {LockssServlet.ROLE_DEBUG};

  private String[] allowUserAdminOrContentAdmin =
    {LockssServlet.ROLE_USER_ADMIN, LockssServlet.ROLE_CONTENT_ADMIN};
  private String[] allowUserAdminOrAuAdmin =
    {LockssServlet.ROLE_USER_ADMIN, LockssServlet.ROLE_AU_ADMIN};
  private String[] allowUserAdminOrContentAccess =
    {LockssServlet.ROLE_USER_ADMIN, LockssServlet.ROLE_CONTENT_ACCESS};
  private String[] allowUserAdminOrDebug = {LockssServlet.ROLE_USER_ADMIN,
      LockssServlet.ROLE_DEBUG};
  private String[] allowContentAdminOrAuAdmin =
    {LockssServlet.ROLE_CONTENT_ADMIN, LockssServlet.ROLE_AU_ADMIN};
  private String[] allowContentAdminOrContentAccess =
    {LockssServlet.ROLE_CONTENT_ADMIN, LockssServlet.ROLE_CONTENT_ACCESS};
  private String[] allowContentAdminOrDebug = {LockssServlet.ROLE_CONTENT_ADMIN,
      LockssServlet.ROLE_DEBUG};
  private String[] allowAuAdminOrContentAccess =
    {LockssServlet.ROLE_AU_ADMIN, LockssServlet.ROLE_CONTENT_ACCESS};
  private String[] allowAuAdminOrDebug = {LockssServlet.ROLE_AU_ADMIN,
      LockssServlet.ROLE_DEBUG};
  private String[] allowContentAccessOrDebug =
    {LockssServlet.ROLE_CONTENT_ACCESS, LockssServlet.ROLE_DEBUG};

  private String[] allowUserAdminOrContentAdminOrAuAdmin =
    {LockssServlet.ROLE_USER_ADMIN, LockssServlet.ROLE_CONTENT_ADMIN,
      LockssServlet.ROLE_AU_ADMIN};
  private String[] allowUserAdminOrContentAdminOrContentAccess =
    {LockssServlet.ROLE_USER_ADMIN, LockssServlet.ROLE_CONTENT_ADMIN,
      LockssServlet.ROLE_CONTENT_ACCESS};
  private String[] allowUserAdminOrAuAdminOrContentAccess =
    {LockssServlet.ROLE_USER_ADMIN, LockssServlet.ROLE_AU_ADMIN,
      LockssServlet.ROLE_CONTENT_ACCESS};
  private String[] allowContentAdminOrAuAdminOrContentAccess =
    {LockssServlet.ROLE_CONTENT_ADMIN, LockssServlet.ROLE_AU_ADMIN,
      LockssServlet.ROLE_CONTENT_ACCESS};

  private String[] allowUserAdminOrContentAdminOrAuAdminOrContentAccess =
    {LockssServlet.ROLE_USER_ADMIN, LockssServlet.ROLE_CONTENT_ADMIN,
      LockssServlet.ROLE_AU_ADMIN, LockssServlet.ROLE_CONTENT_ACCESS};

  private AccountManager accountManager;
  private MyAuthorizationInterceptor interceptor =
      new MyAuthorizationInterceptor();

  public void setUp() throws Exception {
    super.setUp();

    setUpDiskSpace();

    accountManager = new AccountManager();
    getMockLockssDaemon().setAccountManager(accountManager);
    accountManager.initService(getMockLockssDaemon());
    accountManager.startService();
  }

  /**
   * Tests role authorization.
   */
  public void testIsAuthorized() {
    UserAccount userAccount = accountManager.createUser("fgl");
    userAccount.setRoles(LockssServlet.ROLE_USER_ADMIN);
    assertTrue(interceptor.isAuthorized(userAccount, allowUserAdmin));
    assertTrue(interceptor.isAuthorized(userAccount, allowContentAdmin));
    assertTrue(interceptor.isAuthorized(userAccount, allowAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount, allowContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowDebug));

    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdmin));
    assertTrue(interceptor.isAuthorized(userAccount, allowUserAdminOrAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowUserAdminOrDebug));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowContentAdminOrDebug));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowAuAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowAuAdminOrDebug));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAccessOrDebug));

    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrAuAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdminOrContentAccess));

    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdminOrContentAccess));

    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdmin));
    assertTrue(interceptor.isAuthorized(userAccount, allowContentAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount, allowDebug));

    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdminOrAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdminOrDebug));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowContentAdminOrDebug));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowAuAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount, allowAuAdminOrDebug));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowContentAccessOrDebug));

    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrAuAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdminOrContentAccess));

    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdminOrContentAccess));

    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowContentAdmin));
    assertTrue(interceptor.isAuthorized(userAccount, allowAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount, allowDebug));

    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdmin));
    assertTrue(interceptor.isAuthorized(userAccount, allowUserAdminOrAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdminOrDebug));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowContentAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowContentAdminOrDebug));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowAuAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowAuAdminOrDebug));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowContentAccessOrDebug));

    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrAuAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdminOrContentAccess));

    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdminOrContentAccess));

    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowContentAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount, allowContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount, allowDebug));

    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdminOrAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdminOrDebug));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowContentAdminOrDebug));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowAuAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount, allowAuAdminOrDebug));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAccessOrDebug));

    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdmin));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrAuAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdminOrContentAccess));

    assertTrue(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdminOrContentAccess));

    userAccount.setRoles(LockssServlet.ROLE_DEBUG);
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowContentAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowDebug));

    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdmin));
    assertFalse(interceptor.isAuthorized(userAccount, allowUserAdminOrAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowUserAdminOrDebug));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowContentAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowContentAdminOrDebug));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowAuAdminOrContentAccess));
    assertTrue(interceptor.isAuthorized(userAccount, allowAuAdminOrDebug));
    assertTrue(interceptor.isAuthorized(userAccount,
	allowContentAccessOrDebug));

    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdmin));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrAuAdminOrContentAccess));
    assertFalse(interceptor.isAuthorized(userAccount,
	allowContentAdminOrAuAdminOrContentAccess));

    assertFalse(interceptor.isAuthorized(userAccount,
	allowUserAdminOrContentAdminOrAuAdminOrContentAccess));
  }

  /**
   * Instantiable interceptor.
   */
  static class MyAuthorizationInterceptor extends AuthorizationInterceptor {
    @Override
    protected String[] getPermissibleRoles() {
      return null;
    }
  }
}
