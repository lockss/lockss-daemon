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
package org.lockss.ws.hasher;

import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import org.lockss.ws.entities.HasherWsAsynchronousResult;
import org.lockss.ws.entities.HasherWsParams;
import org.lockss.ws.entities.HasherWsResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * The Hasher web service interface.
 */
@WebService
public interface HasherService {
  /**
   * Performs the hashing of an AU or a URL.
   * 
   * @param hasherParams
   *          A HasherWsParams with the parameters of the hashing operation.
   * @return a HasherWsResult with the result of the hashing operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  HasherWsResult hash(
      @WebParam(name = "hasherParams") HasherWsParams hasherParams)
      throws LockssWebServicesFault;

  /**
   * Performs asynchronously the hashing of an AU or a URL.
   * 
   * @param hasherParams
   *          A HasherWsParams with the parameters of the hashing operation.
   * @return a HasherWsAsynchronousResult with the result of the hashing
   *         operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  HasherWsAsynchronousResult hashAsynchronously(
      @WebParam(name = "hasherParams") HasherWsParams hasherParams)
      throws LockssWebServicesFault;

  /**
   * Provides the result of an asynchronous hashing operation.
   * 
   * @param requestId
   *          A String with the identifier of the requested asynchronous hashing
   *          operation.
   * @return a HasherWsAsynchronousResult with the result of the hashing
   *         operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  HasherWsAsynchronousResult getAsynchronousHashResult(
      @WebParam(name = "requestId") String requestId)
      throws LockssWebServicesFault;

  /**
   * Provides the results of all the asynchronous hashing operations.
   * 
   * @return a List<HasherWsAsynchronousResult> with the result of the hashing
   *         operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  List<HasherWsAsynchronousResult> getAllAsynchronousHashResults()
      throws LockssWebServicesFault;

  /**
   * Removes from the system an asynchronous hashing operation, terminating it
   * if it's still running.
   * 
   * @param requestId
   *          A String with the identifier of the requested asynchronous hashing
   *          operation.
   * @return a HasherWsAsynchronousResult with the result of the removal of the
   *         hashing operation.
   * @throws LockssWebServicesFault
   */
  @WebMethod
  HasherWsAsynchronousResult removeAsynchronousHashRequest(
      @WebParam(name = "requestId") String requestId)
      throws LockssWebServicesFault;
}
