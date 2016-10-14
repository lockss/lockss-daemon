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
import org.lockss.util.Logger;

/**
 * Access Control filter.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AccessControlFilter implements ContainerRequestFilter {
  private static final String forbiddenAccess = "Access blocked for all users.";
  private static final String noAuthorizationHeader =
      "No authorization header.";
  private static final String noCredentials = "No userid/password credentials.";
  private static final String badCredentials =
      "Bad userid/password credentials.";
  private static final String noUser = "User not found.";
  private static final String noRequiredRole =
      "User does not have the required role.";

  private static final Logger log = Logger.getLogger(AccessControlFilter.class);

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
   * @param resourceInfo
   *          A ResourceInfo with information about the resource involved.
   * @param requestContext
   *          A ContainerRequestContext with the request context.
   * 
   * @return a Set<String> with the permissible roles.
   */
  protected Set<String> getPermissibleRoles(ResourceInfo resourceInfo,
      ContainerRequestContext requestContext) {
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
    final String DEBUG_HEADER = "filter(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

    Method method = resourceInfo.getResourceMethod();
    if (log.isDebug3()) {
      log.debug3(DEBUG_HEADER + "method = " + method);
      log.debug3(DEBUG_HEADER + "method.getDeclaredAnnotations() = "
	  + Arrays.toString(method.getDeclaredAnnotations()));
    }

    // Check whether access is allowed to anybody.
    if (method.isAnnotationPresent(PermitAll.class)) {
      // Yes: Continue normally.
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "Authorized (like everybody else).");
      return;
    }

    // Get the HTTP request method name.
    String httpMethodName = requestContext.getMethod().toUpperCase();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "httpMethodName = " + httpMethodName);

    // Get the request path segments.
    List<PathSegment> pathSegments =
	requestContext.getUriInfo().getPathSegments();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "pathSegments = " + pathSegments);

    // Check whether it is the base swagger request.
    if ("GET".equals(httpMethodName) &&
	method.getName().equals("getListing") && pathSegments.size() == 1 &&
	"swagger.json".equals(pathSegments.get(0).getPath().toLowerCase())) {
      // Yes: Continue normally.
      if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "Authorized (like everybody else).");
      return;
    }

    // Check whether access is denied to everybody.
    if (method.isAnnotationPresent(DenyAll.class)) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + forbiddenAccess);
      log.info(DEBUG_HEADER + "method = " + method);
      log.info(DEBUG_HEADER + "method.getDeclaredAnnotations() = "
	  + Arrays.toString(method.getDeclaredAnnotations()));

      requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
	  .entity(forbiddenAccess).build());
      return;
    }

    Set<String> permissibleRoles = null;

    // Check whether a role security annotation is present.
    if (method.isAnnotationPresent(RolesAllowed.class))	{
      // Yes: Get the permissible roles from the annotation.
      RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "rolesAnnotation = " + rolesAnnotation);

      permissibleRoles =
	  new HashSet<String>(Arrays.asList(rolesAnnotation.value()));
    } else {
      // No: Get the custom permissible roles.
      permissibleRoles = getPermissibleRoles(resourceInfo, requestContext);
    }

    // Get the authorization header.
    String authorizationHeader =
	requestContext.getHeaderString("authorization");
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "authorizationHeader = " + authorizationHeader);

    // Check whether no authorization header was found.
    if (authorizationHeader == null) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + noAuthorizationHeader);

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(noAuthorizationHeader).build());
      return;
    }

    // Get the user credentials in the authorization header.
    String[] credentials = decodeBasicAuthorizationHeader(authorizationHeader);

    // Check whether no credentials were found.
    if (credentials == null) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + noCredentials);

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(noCredentials).build());
      return;
    }

    // Check whether the found credentials are not what was expected.
    if (credentials.length != 2) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + badCredentials);
      log.info(DEBUG_HEADER
	  + "bad credentials = " + Arrays.toString(credentials));

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(badCredentials).build());
      return;
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "credentials[0] = " + credentials[0]);

    // Get the user account.
    UserAccount userAccount = LockssDaemon.getLockssDaemon().getAccountManager()
	.getUser(credentials[0]);

    // Check whether no user was found.
    if (userAccount == null) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + noUser);

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(badCredentials).build());
      return;
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "userAccount.getName() = " + userAccount.getName());

    // Verify the user credentials.
    boolean goodCredentials = userAccount.check(credentials[1]);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "goodCredentials = " + goodCredentials);

    // Check whether the user credentials are not good.
    if (!goodCredentials) {
      // Yes: Report the problem.
      log.info(DEBUG_HEADER + badCredentials);
      log.info(DEBUG_HEADER
	  + "userAccount.getName() = " + userAccount.getName());
      log.info(DEBUG_HEADER
	  + "bad credentials = " + Arrays.toString(credentials));

      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
	  .entity(badCredentials).build());
      // No: Check whether the user has the role required to execute this
      // operation.
    } else if (isAuthorized(userAccount, permissibleRoles)) {
      // Yes: Continue normally.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Authorized.");
    } else {
      // No: Report the problem.
      log.info(DEBUG_HEADER + noRequiredRole);
      log.info(DEBUG_HEADER + "userName = " + userAccount.getName());

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
    final String DEBUG_HEADER = "decodeBasicAuthorizationHeader(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "header = " + header);

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
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "result = " + Arrays.toString(result));

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
    final String DEBUG_HEADER = "isAuthorized(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER
	  + "userAccount.getName() = " + userAccount.getName());
      log.debug2(DEBUG_HEADER
	  + "userAccount.getRoles() = " + userAccount.getRoles());
      log.debug2(DEBUG_HEADER
	  + "userAccount.getRoleSet() = " + userAccount.getRoleSet());
    }

    // An administrator is always authorized.
    if (userAccount.isUserInRole(Roles.ROLE_USER_ADMIN)) {
      return true;
    }

    // Check whether there are no permissible roles.
    if (permissibleRoles == null || permissibleRoles.size() == 0) {
      // Yes: Normal users are not authorized.
      return false;
    }

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "permissibleRoles = " + permissibleRoles);

    // Loop though all the permissible roles.
    for (String permissibleRole : permissibleRoles) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "permissibleRole = " + permissibleRole);

      // If any role is permitted, this user is authorized.
      if (Roles.ROLE_ANY.equals(permissibleRole)) {
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
