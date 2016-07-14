/*

 Copyright (c) 2013-2016 Board of Trustees of Leland Stanford Jr. University,
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
 * The DaemonStatus web service implementation.
 */
package org.lockss.ws.status;

import java.util.ArrayList;
import java.util.List;
import javax.jws.WebService;
import org.lockss.app.LockssDaemon;
import org.lockss.daemon.RangeCachedUrlSetSpec;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.CachedUrlSet;
import org.lockss.plugin.CuIterator;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.LockssWebServicesFault;
import org.lockss.ws.entities.LockssWebServicesFaultInfo;
import org.lockss.ws.status.DaemonStatusService;

@WebService
public class DaemonStatusServiceImpl implements DaemonStatusService {
  private static Logger log = Logger.getLogger(DaemonStatusServiceImpl.class);

  /**
   * Provides the URLs in an archival unit.
   * 
   * @param auId
   *          A String with the identifier of the archival unit.
   * @param url
   *          A String with the URL above which no results will be provided, or
   *          <code>NULL</code> if all the URLS are to be provided.
   * @return a List<String> with the results.
   * @throws LockssWebServicesFault
   */
  public List<String> getAuUrls(String auId, String url)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAuUrls(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      log.debug2(DEBUG_HEADER + "url = " + url);
    }

    // Input validation.
    if (StringUtil.isNullString(auId)) {
      throw new LockssWebServicesFault(
	  new IllegalArgumentException("Invalid Archival Unit identifier"),
	  new LockssWebServicesFaultInfo("Archival Unit identifier = " + auId));
    }

    LockssDaemon theDaemon = LockssDaemon.getLockssDaemon();
    PluginManager pluginMgr = theDaemon.getPluginManager();
    ArchivalUnit au = pluginMgr.getAuFromId(auId);

    if (au == null) {
      throw new LockssWebServicesFault(
	  "No Archival Unit with provided identifier",
	  new LockssWebServicesFaultInfo("Archival Unit identifier = " + auId));
    }

    CachedUrlSet cuSet = null;

    if (StringUtil.isNullString(url)) {
      cuSet = au.getAuCachedUrlSet();
    } else {
      cuSet = au.makeCachedUrlSet(new RangeCachedUrlSetSpec(url));
    }

    CuIterator iterator = cuSet.getCuIterator();
    CachedUrl cu = null;
    List<String> results = new ArrayList<String>();

    // Loop through all the cached URLs.
    while (iterator.hasNext()) {
      try {
	// Get the next URL.
	cu = iterator.next();

	// Add it to the results.
	results.add(cu.getUrl());
      } finally {
	AuUtil.safeRelease(cu);
      }
    }

    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "results = " + results);

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "results.size() = " + results.size());
    return results;
  }
}
