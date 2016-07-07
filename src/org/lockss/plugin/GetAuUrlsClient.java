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
package org.lockss.plugin;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.lockss.config.CurrentConfig;
import org.lockss.util.Logger;
import org.lockss.ws.status.DaemonStatusService;

/**
 * A client for the DaemonStatusService.getAuUrls() web service operation.
 */
public class GetAuUrlsClient {
  private static Logger log = Logger.getLogger(GetAuUrlsClient.class);
  private static final String TIMEOUT_KEY =
      "com.sun.xml.internal.ws.request.timeout";

  private static ConcurrentMap<String, Exception> exceptions =
      new ConcurrentHashMap<String, Exception>();

  /**
   * Provides the URLs in an archival unit.
   * 
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a List<String> with the archival unit URLs.
   */
  public List<String> getAuUrls(String auId) {
    final String DEBUG_HEADER = "getAuUrls(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);
    try {
      return getProxy().getAuUrls(auId, null);
    } catch (Exception e) {
      exceptions.put(auId, e);
    }

    return null;
  }

  /**
   * Provides a proxy to the web service.
   * 
   * @return a DaemonStatusService with the proxy to the web service.
   * @throws Exception
   *           if there are problems getting the proxy.
   */
  protected DaemonStatusService getProxy() throws Exception {
    final String DEBUG_HEADER = "getProxy(): ";
    authenticate();
    String addressLocation = CurrentConfig.getParam(
	PluginManager.PARAM_URL_LIST_WS_ADDRESS_LOCATION);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "addressLocation = " + addressLocation);

    String targetNamespace = CurrentConfig.getParam(
	PluginManager.PARAM_URL_LIST_WS_TARGET_NAMESPACE);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "targetNamespace = " + targetNamespace);

    String serviceName = CurrentConfig.getParam(
	PluginManager.PARAM_URL_LIST_WS_SERVICE_NAME);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "serviceName = " + serviceName);

    Service service = Service.create(new URL(addressLocation), new QName(
	targetNamespace, serviceName));

    DaemonStatusService port = service.getPort(DaemonStatusService.class);

    // Set the client connection timeout.
    int timeoutValue = CurrentConfig.getIntParam(
	PluginManager.PARAM_URL_LIST_WS_TIMEOUT_VALUE,
	PluginManager.DEFAULT_URL_LIST_WS_TIMEOUT_VALUE);
    ((javax.xml.ws.BindingProvider) port).getRequestContext().put(TIMEOUT_KEY,
	new Integer(timeoutValue*1000));

    return port;
  }

  /**
   * Sets the authenticator that will be used by the networking code when the
   * HTTP server asks for authentication.
   */
  protected void authenticate() {
    Authenticator.setDefault(new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
	String userName =
	    CurrentConfig.getParam(PluginManager.PARAM_URL_LIST_WS_USER_NAME);
	String password =
	    CurrentConfig.getParam(PluginManager.PARAM_URL_LIST_WS_PASSWORD);
	return new PasswordAuthentication(userName, password.toCharArray());
      }
    });
  }

  /**
   * Provides, after deleting it, any exception thrown while getting the URLs
   * for an archival unit.
   * 
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return an Exception that was thrown while getting the URLs or null if the
   *         URL retrieval process did not throw.
   */
  public static Exception getAndDeleteAnyException(String auId) {
    return exceptions.remove(auId);
  }
}
