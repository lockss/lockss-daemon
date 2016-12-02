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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.lockss.config.CurrentConfig;
import org.lockss.util.Logger;
import org.lockss.ws.status.DaemonStatusService;

/**
 * A client for the DaemonStatusService.getAuUrls() web service operation or for
 * the equivalent Index REST web service.
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

    // Get the configured REST service location.
    String restServiceLocation =
	CurrentConfig.getParam(PluginManager.PARAM_REST_SERVICE_LOCATION);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "restServiceLocation = "
	+ restServiceLocation);

    // Check whether a REST service location has been configured.
    if (restServiceLocation != null
	&& restServiceLocation.trim().length() > 0) {
      // Yes: Get the Archival Unit URLs from the REST service.
      try {
	// Create a REST service client.
	Client client = ClientBuilder.newClient();
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "client = '" + client + "'");

	// Create the client target.
	ResteasyWebTarget webTarget =
	    (ResteasyWebTarget)client.target(restServiceLocation);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "webTarget = '" + webTarget + "'");

	// Provide the authentication credentials.
	String userName =
	    CurrentConfig.getParam(PluginManager.PARAM_URL_LIST_WS_USER_NAME);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "userName = '" + userName + "'");
	String password =
	    CurrentConfig.getParam(PluginManager.PARAM_URL_LIST_WS_PASSWORD);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "password = '" + password + "'");

	webTarget.register(new BasicAuthentication(userName, password));
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "webTarget = '" + webTarget + "'");

	// Make the request and get the response.
	String encodedAuId = URLEncoder.encode(auId, "UTF-8");
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "encodedAuId = '" + encodedAuId + "'");

	List<Url> result = webTarget.path("aus").path(encodedAuId).path("urls")
	    .request().accept("application/json").get()
	    .readEntity(new GenericType<List<Url>>(){});
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result = " + result);

	// Prepare the results.
	List<String> urls = new ArrayList<String>();

	for (Url url : result) {
	  urls.add(url.getUrl());
	}

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "urls = " + urls);
	return urls;
      } catch (Exception e) {
	log.error("Caught exception accessing REST service", e);
	exceptions.put(auId, e);
      }
    } else {
      // No: Get the Archival Unit URLs from the non-REST service.
      try {
	return getProxy().getAuUrls(auId, null);
      } catch (Exception e) {
	log.error("Caught exception accessing non-REST service", e);
	exceptions.put(auId, e);
      }
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

  /**
   * The object returned by the REST web service.
   */
  public static class Url {
    private String url = null;
    private WarcRecordIndex warcRecordIndex = null;

    public String getUrl() {
      return url;
    }
    public void setUrl(String url) {
      this.url = url;
    }

    public WarcRecordIndex getWarcRecordIndex() {
      return warcRecordIndex;
    }
    public void setWarcRecordIndex(WarcRecordIndex warcRecordIndex) {
      this.warcRecordIndex = warcRecordIndex;
    }

    public static class WarcRecordIndex {
      private String uri = null;
      private Integer timestamp = null;
      private Integer offset = null;

      public String getUri() {
        return uri;
      }
      public void setUri(String uri) {
        this.uri = uri;
      }

      public Integer getTimestamp() {
        return timestamp;
      }
      public void setTimestamp(Integer timestamp) {
        this.timestamp = timestamp;
      }

      public Integer getOffset() {
        return offset;
      }
      public void setOffset(Integer offset) {
        this.offset = offset;
      }
    }
  }
}
