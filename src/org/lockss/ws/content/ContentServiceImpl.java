/*

 Copyright (c) 2014-2016 Board of Trustees of Leland Stanford Jr. University,
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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
import org.lockss.ws.entities.FileWsResult;
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
   * @param url
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

    return fetchVersionedFile(url, auId, null);
  }

  /**
   * Provides the content defined by a URL, an Archival Unit and a version.
   * 
   * @param url
   *          A String with the URL.
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @param version
   *          An Integer with the requested version of the content.
   * @return a ContentResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  public ContentResult fetchVersionedFile(String url, String auId,
      Integer version) throws LockssWebServicesFault {
    final String DEBUG_HEADER = "fetchVersionedFile(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "url = " + url);
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      log.debug2(DEBUG_HEADER + "version = " + version);
    }

    boolean cuInUse = false;

    if (StringUtil.isNullString(url)) {
      throw new LockssWebServicesFault("Missing required URL");
    }

    if (StringUtil.isNullString(auId) && version != null) {
      throw new LockssWebServicesFault("To fetch a specific version, the "
	  + "Archival Unit identifier (auId) is required");
    }

    CachedUrl cu = null;

    try {
      PluginManager pluginMgr =
	  LockssDaemon.getLockssDaemon().getPluginManager();

      if (StringUtil.isNullString(auId)) {
        // Find a CU with content.
        cu = pluginMgr.findCachedUrl(url, CuContentReq.PreferContent);
        if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cu = " + cu);

        if (cu == null) {
          throw new LockssWebServicesFault("Missing CachedUrl for url '" + url
              + "'");
        }
      } else {
	ArchivalUnit au = pluginMgr.getAuFromId(auId);
        if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

        if (au == null) {
          throw new LockssWebServicesFault("Missing AU with auid '" + auId
              + "'");
        }

        cu = au.makeCachedUrl(url);
        if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cu = " + cu);

        if (cu == null) {
          throw new LockssWebServicesFault("Missing CachedUrl for url '" + url
              + "', auId '" + auId + "'");
        }

        // Check whether a specific version has been requested that is not the
        // version of the current CachedUrl.
        if (version != null && version.intValue() != cu.getVersion()) {
          // Yes: Get the requested version CachedUrl.
          CachedUrl versionedCu = cu.getCuVersion(version);

          // Check whether the requested version CachedUrl does not exist.
          if (versionedCu == null) {
            // Yes: Report the problem.
            throw new Exception("Missing CachedUrl for url '" + url
        	+ "', auId '" + auId + "', version " + version);

            // No: Check whether the requested version CachedUrl does not have
            // content.
          } else if (!versionedCu.hasContent()) {
            // Yes: Report the problem.
            AuUtil.safeRelease(versionedCu);
            throw new Exception("Version " + version + " of " + url
        	+ " for the requested Archival Unit '" + auId
        	+ "' has no content");
          }

          // No: Replace the current CachedUrl with the versioned one.
          AuUtil.safeRelease(cu);
          cu = versionedCu;
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cu = " + cu);
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
	  throw new LockssWebServicesFault("No content for url '" + url
	      + "', auId '" + auId + "', version " + version);
	}
      }

      String ctype = cu.getContentType();
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "ctype = " + ctype);

      String mimeType = HeaderUtil.getMimeTypeFromContentType(ctype);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "mimeType = " + mimeType);

      // Populate the response.
      ContentResult result = new ContentResult();
      result.setProperties((Properties)props);
      result.setDataHandler(new DataHandler(
	  new InputStreamDataSource(cu.getUnfilteredInputStream(), mimeType,
	      url)));

      // Cannot release CU when return as DataHandler will read from (and
      // then close) the InputStream in the InputStreamDataSource
      cuInUse = true;
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
      return result;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new LockssWebServicesFault(e);
    } finally {
      if (!cuInUse) {
	AuUtil.safeRelease(cu);
      }
    }
  }

  /**
   * Provides a list of the versions of a URL in an Archival Unit.
   * 
   * @param url
   *          A String with the URL.
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a {@code List<FileWsResult>} with the results.
   * @throws LockssWebServicesFault
   */
  public List<FileWsResult> getVersions(String url, String auId)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getVersions(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "url = " + url);
      log.debug2(DEBUG_HEADER + "auId = " + auId);
    }

    if (StringUtil.isNullString(url)) {
      throw new LockssWebServicesFault("Missing required URL");
    }

    if (StringUtil.isNullString(auId)) {
      throw new LockssWebServicesFault("Missing required Archival Unit "
	  + "identifier (auId)");
    }

    CachedUrl cu = null;

    try {
      ArchivalUnit au =
	  LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

      if (au == null) {
	throw new LockssWebServicesFault("Missing AU with auid '" + auId + "'");
      }

      cu = au.makeCachedUrl(url);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cu = " + cu);

      if (cu == null) {
	throw new LockssWebServicesFault("Missing CachedUrl for url '" + url
	    + "', auId '" + auId + "'");
      }

      // Get the versions.
      CachedUrl[] cuVersions = cu.getCuVersions(Integer.MAX_VALUE);

      // Initialize the result.
      List<FileWsResult> result =
	  new ArrayList<FileWsResult>(cuVersions.length);

      for (CachedUrl versionedCu : cuVersions) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "versionedCu = " + versionedCu);

	try {
	  FileWsResult versionedFile = new FileWsResult();

	  versionedFile.setUrl(url);
	  versionedFile.setVersion(versionedCu.getVersion());
	  versionedFile.setSize(versionedCu.getContentSize());
	  versionedFile.setCollectionDate(Long.parseLong(versionedCu
	      .getProperties().getProperty(CachedUrl.PROPERTY_FETCH_TIME)));

	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "versionedFile = " + versionedFile);

	  result.add(versionedFile);
	} catch (Exception e) {
	  log.error(e.getMessage(), e);
	  throw new LockssWebServicesFault(e);
	} finally {
	  AuUtil.safeRelease(versionedCu);
	}
      }

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
      return result;
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new LockssWebServicesFault(e);
    } finally {
      AuUtil.safeRelease(cu);
    }
  }

  /**
   * Provides an indication of whether the content defined by a URL and Archival
   * Unit is cached.
   * 
   * @param url
   *          A String with the URL.
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a boolean with the indication.
   * @throws LockssWebServicesFault
   */
  @Override
  public boolean isUrlCached(String url, String auId)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "isUrlCached(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "url = " + url);
      log.debug2(DEBUG_HEADER + "auId = " + auId);
    }

    if (StringUtil.isNullString(url)) {
      throw new LockssWebServicesFault("Missing required URL");
    }

    return isUrlVersionCached(url, auId, null);
  }

  /**
   * Provides an indication of whether the content defined by a URL, an Archival
   * Unit and a version is cached.
   * 
   * @param url
   *          A String with the URL.
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @param version
   *          An Integer with the requested version of the content.
   * @return a boolean with the indication.
   * @throws LockssWebServicesFault
   */
  @Override
  public boolean isUrlVersionCached(String url, String auId, Integer version)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "isUrlVersionCached(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "url = " + url);
      log.debug2(DEBUG_HEADER + "auId = " + auId);
      log.debug2(DEBUG_HEADER + "version = " + version);
    }

    if (StringUtil.isNullString(url)) {
      throw new LockssWebServicesFault("Missing required URL");
    }

    if (StringUtil.isNullString(auId) && version != null) {
      throw new LockssWebServicesFault("To check a specific version, the "
	  + "Archival Unit identifier (auId) is required");
    }

    boolean result = true;
    CachedUrl cu = null;

    try {
      PluginManager pluginMgr =
	  LockssDaemon.getLockssDaemon().getPluginManager();

      if (StringUtil.isNullString(auId)) {
        // Find a CU with content.
        cu = pluginMgr.findCachedUrl(url, CuContentReq.PreferContent);
        if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cu = " + cu);

        result = !(cu == null);
      } else {
	ArchivalUnit au = pluginMgr.getAuFromId(auId);
        if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

        if (au == null) {
          result = false;
        } else {
          cu = au.makeCachedUrl(url);
          if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cu = " + cu);

          if (cu == null) {
            result = false;
          } else {
            // Check whether a specific version has been requested that is not
            // the version of the current CachedUrl.
            if (version != null && version.intValue() != cu.getVersion()) {
              // Yes: Get the requested version CachedUrl.
              CachedUrl versionedCu = cu.getCuVersion(version);

              // Check whether the requested version CachedUrl does not exist.
              if (versionedCu == null) {
        	// Yes: Report the problem.
        	result = false;
              } else {
        	// No: Replace the current CachedUrl with the versioned one.
        	AuUtil.safeRelease(cu);
        	cu = versionedCu;
        	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "cu = " + cu);
              }
            }
          }
        }
      }

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
