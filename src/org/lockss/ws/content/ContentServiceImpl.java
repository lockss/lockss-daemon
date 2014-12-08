/*
 * $Id: ContentServiceImpl.java,v 1.1 2014-12-08 19:16:22 fergaloy-sf Exp $
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
package org.lockss.ws.content;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.soap.MTOM;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.lockss.app.LockssDaemon;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.CachedUrl;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginManager.CuContentReq;
import org.lockss.util.CIProperties;
import org.lockss.util.HeaderUtil;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.ws.entities.ContentResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * The Content web service implementation.
 */
@MTOM
@WebService
public class ContentServiceImpl implements ContentService {
  private static Logger log = Logger.getLogger(ContentServiceImpl.class);

  /**
   * Provides the content defined by a URL and Archival Unit.
   * 
   * @param auId
   *          A String with the URL.
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a ContentResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  public ContentResult fetchFile(String url, String auId)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "fetchFile(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "url = " + url);
      log.debug2(DEBUG_HEADER + "auId = " + auId);
    }

    if (StringUtil.isNullString(url)) {
      throw new LockssWebServicesFault("Missing required URL");
    }

    CachedUrl cu = null;

    try {
      PluginManager pluginMgr =
	  LockssDaemon.getLockssDaemon().getPluginManager();

      if (StringUtil.isNullString(auId)) {
        // Find a CU with content.
        cu = pluginMgr.findCachedUrl(url, CuContentReq.PreferContent);
      } else {
	ArchivalUnit au = pluginMgr.getAuFromId(auId);
        if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

        if (au == null) {
          throw new LockssWebServicesFault("Missing AU with auid '" + auId
              + "'");
        }

        cu = au.makeCachedUrl(url);
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cu = " + cu);

      if (cu == null) {
	if (StringUtil.isNullString(auId)) {
	  throw new LockssWebServicesFault("Missing CachedUrl for url '" + url
	      + "'");
	} else {
	  throw new LockssWebServicesFault("Missing CachedUrl for auid '" + auId
	      + "', url '" + url + "'");
	}
      }

      CIProperties props = null;

      try {
	props = cu.getProperties();
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "props = " + props);
      } catch (Exception e) {
	if (StringUtil.isNullString(auId)) {
	  throw e;
	} else {
	  throw new LockssWebServicesFault("No content for auid '" + auId
	      + "', url '" + url + "'");
	}
      }

      String ctype = props.getProperty(CachedUrl.PROPERTY_CONTENT_TYPE);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "ctype = " + ctype);

      String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "mimeType = " + mimeType);

      // Populate the response.
      ContentResult result = new ContentResult();
      result.setDataHandler(new DataHandler(
	  new InputStreamDataSource(cu.getUnfilteredInputStream(), mimeType,
	      url)));

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
      return result;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new LockssWebServicesFault(e);
    } finally {
      AuUtil.safeRelease(cu);
    }
  }
}
