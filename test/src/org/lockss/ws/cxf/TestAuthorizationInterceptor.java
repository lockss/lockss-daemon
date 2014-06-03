/*
 * $Id: TestAuthorizationInterceptor.java,v 1.2 2014-06-03 22:45:53 fergaloy-sf Exp $
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

/**
 * Test class for org.lockss.ws.cxf.AuthorizationInterceptor
 * 
 * @author Fernando Garcia-Loygorri
 */
package org.lockss.ws.cxf;

import org.lockss.account.AccountManager;
import org.lockss.account.UserAccount;
import org.lockss.servlet.LockssServlet;
import org.lockss.test.LockssTestCase;

public class TestAuthorizationInterceptor extends LockssTestCase {
  AccountManager accountManager;
  MyAuthorizationInterceptor interceptor = new MyAuthorizationInterceptor();

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
    assertTrue(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_USER_ADMIN));
    assertTrue(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_CONTENT_ADMIN));
    assertTrue(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_AU_ADMIN));
    assertTrue(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_CONTENT_ACCESS));

    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ADMIN);
    assertFalse(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_USER_ADMIN));
    assertTrue(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_CONTENT_ADMIN));
    assertFalse(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_AU_ADMIN));
    assertFalse(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_CONTENT_ACCESS));

    userAccount.setRoles(LockssServlet.ROLE_AU_ADMIN);
    assertFalse(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_USER_ADMIN));
    assertFalse(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_CONTENT_ADMIN));
    assertTrue(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_AU_ADMIN));
    assertFalse(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_CONTENT_ACCESS));

    userAccount.setRoles(LockssServlet.ROLE_CONTENT_ACCESS);
    assertFalse(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_USER_ADMIN));
    assertFalse(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_CONTENT_ADMIN));
    assertFalse(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_AU_ADMIN));
    assertTrue(interceptor.isAuthorized(userAccount,
	LockssServlet.ROLE_CONTENT_ACCESS));
  }

  /**
   * Instantiable interceptor.
   */
  static class MyAuthorizationInterceptor extends AuthorizationInterceptor {
    @Override
    protected String getRequiredRole() {
      return null;
    }
  }
}
