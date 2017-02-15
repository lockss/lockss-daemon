/*
 * $Id$
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

import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import org.lockss.ws.entities.ContentConfigurationResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * The ContentConfiguration web service interface.
 */
@WebService
public interface ContentConfigurationService {
  /**
   * Configures the archival unit defined by its identifier.
   * 
   * @param auId
   *          A String with the identifier (auid) of the archival unit. The
   *          archival unit to be added must already be in the title db that's
   *          loaded into the daemon.
   * @return a ContentConfigurationResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  ContentConfigurationResult addAuById(@WebParam(name = "auId") String auId)
      throws LockssWebServicesFault;

  /**
   * Configures the archival units defined by a list with their identifiers.
   * 
   * @param auIds
   *          A List<String> with the identifiers (auids) of the archival units.
   *          The archival units to be added must already be in the title db
   *          that's loaded into the daemon.
   * @return a List<ContentConfigurationResult> with the results of the
   *         operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  List<ContentConfigurationResult> addAusByIdList(
      @WebParam(name = "auIds") List<String> auIds)
      throws LockssWebServicesFault;

  /**
   * Unconfigures the archival unit defined by its identifier.
   * 
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a ContentConfigurationResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  ContentConfigurationResult deleteAuById(@WebParam(name = "auId") String auId)
      throws LockssWebServicesFault;

  /**
   * Unconfigures the archival units defined by a list with their identifiers.
   * 
   * @param auIds
   *          A List<String> with the identifiers (auids) of the archival units.
   * @return a List<ContentConfigurationResult> with the results of the
   *         operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  List<ContentConfigurationResult> deleteAusByIdList(
      @WebParam(name = "auIds") List<String> auIds)
      throws LockssWebServicesFault;

  /**
   * Reactivates the archival unit defined by its identifier.
   * 
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a ContentConfigurationResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  ContentConfigurationResult reactivateAuById(
      @WebParam(name = "auId") String auId) throws LockssWebServicesFault;

  /**
   * Reactivates the archival units defined by a list with their identifiers.
   * 
   * @param auIds
   *          A List<String> with the identifiers (auids) of the archival units.
   * @return a List<ContentConfigurationResult> with the results of the
   *         operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  List<ContentConfigurationResult> reactivateAusByIdList(
      @WebParam(name = "auIds") List<String> auIds)
      throws LockssWebServicesFault;

  /**
   * Deactivates the archival unit defined by its identifier.
   * 
   * @param auId
   *          A String with the identifier (auid) of the archival unit.
   * @return a ContentConfigurationResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  ContentConfigurationResult deactivateAuById(
      @WebParam(name = "auId") String auId) throws LockssWebServicesFault;

  /**
   * Deactivates the archival units defined by a list with their identifiers.
   * 
   * @param auIds
   *          A List<String> with the identifiers (auids) of the archival units.
   * @return a List<ContentConfigurationResult> with the results of the
   *         operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  List<ContentConfigurationResult> deactivateAusByIdList(
      @WebParam(name = "auIds") List<String> auIds)
      throws LockssWebServicesFault;
}
