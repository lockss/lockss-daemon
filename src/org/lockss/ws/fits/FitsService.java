/*
 * $Id: FitsService.java,v 1.1.2.1 2013-07-17 10:12:47 easyonthemayo Exp $
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
 * The DaemonStatus web service interface.
 */
package org.lockss.ws.fits;

import org.lockss.ws.entities.*;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import java.net.URL;
import java.util.Collection;

@WebService
public interface FitsService {
  /**
   * Provides an indication of whether the daemon is ready.
   * 
   * @return a boolean with the indication.
   * @throws org.lockss.ws.entities.LockssWebServicesFault
   */
  @WebMethod
  boolean isDaemonReady() throws LockssWebServicesFault;

  /**
   * Provides the FITS-interpreted file type of a file in an archival unit in
   * the system. The file is identified by an auid and a URL.
   *
   * @param auId A String with the identifier of the archival unit.
   * @param url A String representing the URL of the file in the AU.
   * @return An AuStatus with the status information of the archival unit's file.
   * @throws org.lockss.ws.entities.LockssWebServicesFault
   */
  /*@WebMethod
  String getAuItemType(@WebParam(name = "auid") String auId,
                       @WebParam(name = "url") String url)
      throws LockssWebServicesFault;
  */

  /**
   * Provides the FITS-interpreted MIME-type of a file in an archival unit in
   * the system. The file is identified by an auid and a URL.
   *
   * @param auId A String with the identifier of the archival unit.
   * @param url A String representing the URL of the file in the AU.
   * @return An AuStatus with the status information of the archival unit's file.
   * @throws org.lockss.ws.entities.LockssWebServicesFault
   */
  /*@WebMethod
  String getAuMimeType(@WebParam(name = "auid") String auId,
                       @WebParam(name = "url") String url)
      throws LockssWebServicesFault;
  */

  // NOTE Returning small result values individually is expensive, unless
  // we start caching the results so they can be accessed by auid and url.
  // Instead, let's return the whole result and let the client parse it:
  /**
   * Provides the XML result of a FITS analysis of a file in an archival unit
   * in the system. The file is identified by an auid and a URL.
   *
   * @param auId A String with the identifier of the archival unit.
   * @param url A String representing the URL of the file in the AU.
   * @return A full XML document describing the results of FITS analysis on the file.
   * @throws org.lockss.ws.entities.LockssWebServicesFault
   */
  @WebMethod
  String getAnalysis(@WebParam(name = "auid") String auId,
                     @WebParam(name = "url") String url)
      throws LockssWebServicesFault;

}
