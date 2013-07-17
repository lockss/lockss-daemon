/*
 * $Id: FitsServiceImpl.java,v 1.1.2.1 2013-07-17 10:12:47 easyonthemayo Exp $
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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
 * The FITS web service implementation.
 */
package org.lockss.ws.fits;

import edu.harvard.hul.ois.fits.FitsOutput;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.lockss.app.LockssDaemon;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.ws.entities.*;

import javax.jws.WebParam;
import javax.jws.WebService;
import java.io.ByteArrayOutputStream;

@WebService
public class FitsServiceImpl implements FitsService {
  private static Logger log = Logger.getLogger(FitsServiceImpl.class);

  /**
   * Provides an indication of whether the daemon is ready.
   * 
   * @return a boolean with the indication.
   * @throws org.lockss.ws.entities.LockssWebServicesFault
   */
  @Override
  public boolean isDaemonReady() throws LockssWebServicesFault {
    final String DEBUG_HEADER = "isDaemonReady(): ";

    try {
      log.debug2(DEBUG_HEADER + "Invoked.");
      PluginManager pluginMgr =
	  (PluginManager) LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
      boolean areAusStarted = pluginMgr.areAusStarted();
      log.debug2(DEBUG_HEADER + "areAusStarted = " + areAusStarted);

      return areAusStarted;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }


  /**
   * Provides the XML result of a FITS analysis of a file in an archival unit
   * in the system. The file is identified by an auid and a URL.
   *
   * @param auId A String with the identifier of the archival unit.
   * @param url A String representing the URL of the file in the AU.
   * @return A full XML document describing the results of FITS analysis on the file.
   * @throws org.lockss.ws.entities.LockssWebServicesFault
   */
  @Override
  public String getAnalysis(@WebParam(name = "auid") String auId,
                            @WebParam(name = "url") String url)
      throws LockssWebServicesFault {
    try {
      final String methodName = "getAnalysis()";
      verifyParams(auId, url, methodName);
      final ArchivalUnit au = getAu(auId, url);
      // Get FITS to interpret the type of a cached URL from the AU
      log.debug(String.format("Getting FITS analysis for AU '%s' id '%s' url '%s'", au, auId, url));
      FitsOutput fitsOut = FitsUtil.doFitsAnalysis(au, url);
      /*ByteArrayOutputStream baos = new ByteArrayOutputStream();
      fitsOut.output(baos);
      String str = baos.toString().replace("&lt;", "<");*/
      return new XMLOutputter(Format.getRawFormat().setOmitDeclaration(true)).outputString(fitsOut.getFitsXml());
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }


  /**
   * Provides the FITS-interpreted file type of a file in an archival unit in
   * the system. The file is identified by an auid and a URL.
   *
   * @param auId A String with the identifier of the archival unit.
   * @param url A String representing the URL of the file in the AU.
   * @return A String describing the type of the file as "MIME-type (description)"
   * @throws org.lockss.ws.entities.LockssWebServicesFault
   */
  /*@Override
  public String getAuItemType(@WebParam(name = "auid") String auId,
                              @WebParam(name = "url") String url)
      throws LockssWebServicesFault {
    try {
      final String methodName = "getAuItemType()";
      verifyParams(auId, url, methodName);
      final ArchivalUnit au = getAu(auId, url);
      // Get FITS to interpret the type of a cached URL from the AU
      log.debug(String.format("Getting content type for AU '%s' id '%s' url '%s'", au, auId, url));
      FitsOutput fitsOutput = FitsUtil.doFitsAnalysis(au, url);
      return String.format("%s (%s)", FitsUtil.getMimeType(fitsOutput), FitsUtil.getContentType(fitsOutput));
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }*/

  /**
   * Provides the FITS-interpreted MIME-type of a file in an archival unit in
   * the system. The file is identified by an auid and a URL.
   *
   * @param auId A String with the identifier of the archival unit.
   * @param url A String representing the URL of the file in the AU.
   * @return An AuStatus with the status information of the archival unit.
   * @throws org.lockss.ws.entities.LockssWebServicesFault
   */
  /*@Override
  public String getAuMimeType(String auId, String url) throws LockssWebServicesFault {
    try {
      final String methodName = "getAuMimeType()";
      verifyParams(auId, url, methodName);
      final ArchivalUnit au = getAu(auId, url);
      // Get FITS to interpret the type of a cached URL from the AU
      log.debug(String.format("Getting MIME type for AU '%s' id '%s' url '%s'", au, auId, url));
      return FitsUtil.getMimeType(FitsUtil.doFitsAnalysis(au, url));
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }*/

  /**
   * Verify the parameters received in the URL, and log the service call.
   * Throw a LockssWebServicesFault if there is a problem.
   * @param auId A String with the identifier of the archival unit.
   * @param url A String representing the URL of the file in the AU.
   * @param methodName the name of the calling method, for logging
   * @throws LockssWebServicesFault if there is a problem with the parameters
   */
  private void verifyParams(String auId, String url, String methodName) throws LockssWebServicesFault {
    final String DEBUG_HEADER = methodName+": ";
    log.debug2(DEBUG_HEADER + "auId = " + auId);
    log.debug2(DEBUG_HEADER + "url  = " + url);
    if (StringUtil.isNullString(auId)) {
      throw new LockssWebServicesFault(
          new IllegalArgumentException("Invalid Archival Unit identifier"),
          new LockssWebServicesFaultInfo("Archival Unit identifier = "
              + auId));
    }
    if (StringUtil.isNullString(url)) {
      throw new LockssWebServicesFault(
          new IllegalArgumentException("Invalid URL"),
          new LockssWebServicesFaultInfo("URL = "
              + url));
    }
  }

  private ArchivalUnit getAu(String auId, String url) throws LockssWebServicesFault {
    PluginManager pluginManager =
        (PluginManager) LockssDaemon.getManager(LockssDaemon.PLUGIN_MANAGER);
    return pluginManager.getAuFromId(auId);
  }


}
