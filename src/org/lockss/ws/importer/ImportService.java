/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.ws.importer;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import org.lockss.ws.entities.ImportWsParams;
import org.lockss.ws.entities.ImportWsResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * The Import web service interface.
 */
@WebService
public interface ImportService {
  /**
   * Imports a pulled file into an archival unit.
   * 
   * @param importParams
   *          An ImportWsParams with the parameters of the importing operation.
   * @return an ImportWsResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  public ImportWsResult importPulledFile(
      @WebParam(name = "importParams") ImportWsParams importParams)
      throws LockssWebServicesFault;

  /**
   * Imports a pushed file into an archival unit.
   * 
   * @param importParams
   *          An ImportWsParams with the parameters of the importing operation.
   * @return an ImportWsResult with the result of the operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  public ImportWsResult importPushedFile(
      @WebParam(name = "importParams") ImportWsParams importParams)
      throws LockssWebServicesFault;

  /**
   * Provides the names of the supported checksum algorithms.
   * 
   * @return a String[] with the names of the supported checksum algorithms.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  public String[] getSupportedChecksumAlgorithms()
      throws LockssWebServicesFault;
}
