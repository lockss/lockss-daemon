/*

 Copyright (c) 2017 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.metadata;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.lockss.config.CurrentConfig;
import org.lockss.laaws.mdq.model.ItemMetadata;
import org.lockss.util.Logger;

/**
 * A client for the REST web service operation that stores the metadata of an
 * Archival Unit item.
 */
public class StoreAuItemClient {
  private static Logger log = Logger.getLogger(StoreAuItemClient.class);

  /**
   * Posts the metadata of an Archival Unit item to be stored.
   * 
   * @param item
   *          An ItemMetadata with the metadata.
   * @return a Long with the database identifier of the metadata item.
   */
  public Long storeAuItem(ItemMetadata item) {
    final String DEBUG_HEADER = "storeAuItem(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "item = " + item);

    // Get the configured REST service location.
    String restServiceLocation =
	CurrentConfig.getParam(MetadataManager.PARAM_MD_REST_SERVICE_LOCATION);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "restServiceLocation = "
	+ restServiceLocation);

    // Get the client connection timeout.
    int timeoutValue = CurrentConfig.getIntParam(
	MetadataManager.PARAM_MD_REST_TIMEOUT_VALUE,
	MetadataManager.DEFAULT_MD_REST_TIMEOUT_VALUE);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "timeoutValue = " + timeoutValue);

    // Get the authentication credentials.
    String userName =
	CurrentConfig.getParam(MetadataManager.PARAM_MD_REST_USER_NAME);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "userName = '" + userName + "'");
    String password =
	CurrentConfig.getParam(MetadataManager.PARAM_MD_REST_PASSWORD);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "password = '" + password + "'");

    // Make the request to the REST service and get its response.
//    Response result = new ResteasyClientBuilder()
//	.register(JacksonJsonProvider.class)
//	.establishConnectionTimeout(timeoutValue, TimeUnit.SECONDS)
//	.socketTimeout(timeoutValue, TimeUnit.SECONDS).build()
//	.target(restServiceLocation)
//	.register(new BasicAuthentication(userName, password))
//	//.path("aus").request()
//	.request()
//	.post(Entity.entity(item, MediaType.APPLICATION_JSON_TYPE));
//    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result = " + result);
//
//    Long mdItemSeq = (Long)result.getEntity();
    Long mdItemSeq = new ResteasyClientBuilder()
	.register(JacksonJsonProvider.class)
	.establishConnectionTimeout(timeoutValue, TimeUnit.SECONDS)
	.socketTimeout(timeoutValue, TimeUnit.SECONDS).build()
	.target(restServiceLocation)
	.register(new BasicAuthentication(userName, password))
	.path("aus").request()
	.post(Entity.entity(item, MediaType.APPLICATION_JSON_TYPE), Long.class);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
    return mdItemSeq;
  }
}
