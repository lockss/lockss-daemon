/*
 * $Id: ContentConfigurationServiceImpl.java,v 1.1.2.1 2014-06-16 23:22:58 fergaloy-sf Exp $
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
 * The ContentConfiguration web service implementation.
 */
package org.lockss.ws.content;

import javax.jws.WebService;
import org.lockss.app.LockssDaemon;
import org.lockss.remote.RemoteApi.BatchAuStatus;
import org.lockss.util.Logger;
import org.lockss.ws.entities.ContentConfigurationResult;
import org.lockss.ws.entities.LockssWebServicesFault;

@WebService
public class ContentConfigurationServiceImpl implements
    ContentConfigurationService {
  private static Logger log =
      Logger.getLogger(ContentConfigurationServiceImpl.class);

  /**
   * Configures the archival unit defined by its identifier.
   * 
   * @param auId
   *          A String with the identifier of the archival unit. The
   *          archival unit to be added must already be in the title db that's
   *          loaded into the daemon.
   * @return a ContentConfigurationResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @Override
  public ContentConfigurationResult addAuById(String auId)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "addByAuId(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    ContentConfigurationResult result = null;

    // Add the archival unit.
    BatchAuStatus status =
	LockssDaemon.getLockssDaemon().getRemoteApi().addByAuId(auId);

    // Handle the result.
    BatchAuStatus.Entry statusEntry = status.getStatusList().get(0);
    if (statusEntry.isOk()) {
      if (log.isDebug()) log.debug("Success configuring AU '"
	  + statusEntry.getName() + "': " + statusEntry.getExplanation());

      result = new ContentConfigurationResult(auId, statusEntry.getName(),
	  Boolean.TRUE, statusEntry.getExplanation());
    } else {
      log.error("Error configuring AU '" + statusEntry.getName() + "': "
	  + statusEntry.getExplanation());

      result = new ContentConfigurationResult(auId, statusEntry.getName(),
	  Boolean.FALSE, statusEntry.getExplanation());
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }
}
