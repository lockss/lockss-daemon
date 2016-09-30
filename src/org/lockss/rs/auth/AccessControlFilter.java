/*

 Copyright (c) 2016 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.rs.auth;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.lockss.account.UserAccount;
import org.lockss.app.LockssDaemon;
import org.lockss.servlet.LockssServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access Control filter.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AccessControlFilter implements ContainerRequestFilter {
  // Minimum role of any authenticated user.
  public static final String ROLE_ANY = "anyRole";

  private static final String forbiddenAccess = "Access blocked for all users.";
  private static final String noAuthorizationHeader =
      "No authorization header.";
  private static final String noCredentials = "No userid/password credentials.";
  private static final String badCredentials =
      "Bad userid/password credentials.";
  private static final String noUser = "User not found.";
  private static final String noRequiredRole =
      "User does not have the required role.";

  private static final Logger log =
      LoggerFactory.getLogger(AccessControlFilter.class);

  @Context
  protected ResourceInfo resourceInfo;

  /**
   * Provides the names of the roles permissible for the user to be able to
   * execute operations of this web service when no javax.annotation.security
   * annotations are specified for web service operations.
   *
   * By default, it does not allow any role. Overriden in a subclass when
   * the javax.annotation.security annotations do not provide enough access
   * control.
   * 
   * @return a Set<String> with the permissible roles.
   */
  protected Set<String> getPermissibleRoles(String method,
      List<PathSegment> pathSegments) {
    return new HashSet<String>();
  }

  /**
   * Filter method called after a resource has been matched to a request, but
   * before the request has been dispatched to the resource.
   *
   * @param requestContext
   *          A ContainerRequestContext with the request context.
   */
  @Override
  public void filter(ContainerRequestContext requestContext) {
    if (log.isDebugEnabled()) log.debug("Invoked.");

    Method method = resourceInfo.getResourceMethod();
    if (log.isDebugEnabled()) {
      log.debug("method = " + method);
      log.debug("method.getDeclaredAnnotations() = "
	  + Arrays.toString(method.getDeclaredAnnotations()));
    }

    // Check whether access is allowed to anybody.
    if (method.isAnnotationPresent(PermitAll.class)) {
      // Yes: Continue normally.
      if (log.isDebugEnabled()) log.debug("Authorized (like everybody else).");
      return;
    }

    // Check whether access is denied to everybody.
    if (method.isAnnotationPresent(DenyAll.class)) {
      // Yes: Report the problem.
      log.info(forbiddenAccess);
      log.info("method = " + method);
      log.info("method.getDeclaredAnnotations() = "
	  + Arrays.toString(method.getDeclaredAnnotations()));

      requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
	  .entity(forbiddenAccess).build());
      return;
    }

    Set<String> permissibleRoles = null;

    // Check whether a role security annotation is present.
    if (method.isAnnotationPresent(RolesAllowed.class))	{
      // Yes.
      RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
      if (log.isDebugEnabled())
	log.debug("rolesAnnotation = " + rolesAnnotation);

      permissibleRoles =
	  new HashSet<String>(Arrays.asList(rolesAnnotation.value()));
    } else {
      // No: Get the request method name.
      String methodName = requestContext.getMethod().toUpperCase();
      if (log.isDebugEnabled()) log.debug("methodName = " + methodName);

      // Get the request path segments.
      List<PathSegment> pathSegments =
  	requestContext.getUriInfo().getPathSegments();
      if (log.isDebugEnabled()) log.debug("pathSegments = " + pathSegments);

      permissibleRoles = getPermissibleRoles(methodName, pathSegments);
    }

    // Get the authorization header.
    String authorizationHeader =
	requestContext.getHeaderString("authorization");
    if (log.isDebugEnabled())
      log.debug("authorizationHeader = " + authorizationHeader);

    // Check whether no authorization header was found.
    if (authorizationHeader == null) {
      // Yes: Report the problem.
      log.info(noAuthorizationHeader);

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(noAuthorizationHeader).build());
      return;
    }

    // Get the user credentials in the authorization header.
    String[] credentials = decodeBasicAuthorizationHeader(authorizationHeader);

    // Check whether no credentials were found.
    if (credentials == null) {
      // Yes: Report the problem.
      log.info(noCredentials);

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(noCredentials).build());
      return;
    }

    // Check whether the found credentials are not what was expected.
    if (credentials.length != 2) {
      // Yes: Report the problem.
      log.info(badCredentials);
      log.info(Arrays.toString(credentials));

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(badCredentials).build());
      return;
    }

    if (log.isDebugEnabled()) log.debug("credentials[0] = " + credentials[0]);

    // Get the user account.
    UserAccount userAccount = LockssDaemon.getLockssDaemon().getAccountManager()
	.getUser(credentials[0]);

    // Check whether no user was found.
    if (userAccount == null) {
      // Yes: Report the problem.
      log.info(noUser);

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(badCredentials).build());
      return;
    }

    if (log.isDebugEnabled())
      log.debug("userAccount.getName() = " + userAccount.getName());

    // Verify the user credentials.
    boolean goodCredentials = userAccount.check(credentials[1]);
    if (log.isDebugEnabled()) log.debug("goodCredentials = " + goodCredentials);

    // Check whether the user credentials are not good.
    if (!goodCredentials) {
      // Yes: Report the problem.
      log.info(badCredentials);
      log.info("userAccount.getName() = " + userAccount.getName());
      log.info("bad credentials = " + Arrays.toString(credentials));

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(badCredentials).build());
      // No: Check whether the user has the role required to execute this
      // operation.
    } else if (isAuthorized(userAccount, permissibleRoles)) {
      // Yes: Continue normally.
      if (log.isDebugEnabled()) log.debug("Authorized.");
    } else {
      // No: Report the problem.
      log.info(noRequiredRole);
      log.info("userName = " + userAccount.getName());

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(noRequiredRole).build());
    }
  }

  /**
   * Decodes the basic authorization header
   * 
   * @param header
   *          A String with the Authorization header.
   * @return a String[] with the user name and the password.
   */
  private String[] decodeBasicAuthorizationHeader(String header) {
    if (log.isDebugEnabled()) log.debug("header = " + header);

    // Get the header meaningful bytes.
    byte[] decodedBytes =
	Base64.getDecoder().decode(header.replaceFirst("[B|b]asic ", ""));

    // Check whether nothing was decoded.
    if (decodedBytes == null || decodedBytes.length == 0) {
      // Yes: Done.
      return null;
    }

    // No: Extract the individual credential items, the user name and the
    // password.
    String[] result = new String(decodedBytes).split(":", 2);
    if (log.isDebugEnabled()) log.debug("result = " + Arrays.toString(result));

    return result;
  }

  /**
   * Provides an indication of whether the user has the role required to execute
   * operations of this web service.
   * 
   * @param userAccount
   *          A UserAccount with the user account data.
   * @param permissibleRoles
   *          A String[] with the roles permissible for the user to be able to
   *          execute operations of this web service.
   * @return a boolean with <code>TRUE</code> if the user has the role required
   *         to execute operations of this web service, <code>FALSE</code>
   *         otherwise.
   */
  private boolean isAuthorized(UserAccount userAccount,
      Set<String> permissibleRoles) {
    if (log.isDebugEnabled()) {
      log.debug("userAccount.getName() = " + userAccount.getName());
      log.debug("userAccount.getRoles() = " + userAccount.getRoles());
      log.debug("userAccount.getRoleSet() = " + userAccount.getRoleSet());
    }

    // An administrator is always authorized.
    if (userAccount.isUserInRole(LockssServlet.ROLE_USER_ADMIN)) {
      return true;
    }

    // Check whether there are no permissible roles.
    if (permissibleRoles == null || permissibleRoles.size() == 0) {
      // Yes: Normal users are not authorized.
      return false;
    }

    if (log.isDebugEnabled())
      log.debug("permissibleRoles = " + permissibleRoles);

    // Loop though all the permissible roles.
    for (String permissibleRole : permissibleRoles) {
      if (log.isDebugEnabled())
	log.debug("permissibleRole = " + permissibleRole);

      // If any role is permitted, this user is authorized.
      if (ROLE_ANY.equals(permissibleRole)) {
	return true;
      }

      // The user is authorized if it has this permissible role.
      if (userAccount.isUserInRole(permissibleRole)) {
	return true;
      }
    }

    // The user is not authorized because it does not have any of the
    // permissible roles.
    return false;
  }
}
