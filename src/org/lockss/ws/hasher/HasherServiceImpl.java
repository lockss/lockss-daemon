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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import org.lockss.hasher.HasherParams;
import org.lockss.hasher.HasherResult;
import org.lockss.hasher.SimpleHasher;
import org.lockss.hasher.SimpleHasher.HasherStatus;
import org.lockss.hasher.SimpleHasher.ParamsAndResult;
import org.lockss.util.FileUtil;
import org.lockss.util.Logger;
import org.lockss.util.PlatformUtil;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;
import org.lockss.ws.entities.HasherWsAsynchronousResult;
import org.lockss.ws.entities.HasherWsParams;
import org.lockss.ws.entities.HasherWsResult;
import org.lockss.ws.entities.LockssWebServicesFault;

/**
 * The Hasher web service implementation.
 */
public class HasherServiceImpl implements HasherService {
  private static Logger log = Logger.getLogger(HasherServiceImpl.class);

  private static final Map<String, SimpleHasher.ParamsAndResult> HASH_REQUESTS =
      new LinkedHashMap<String, SimpleHasher.ParamsAndResult>();

  /**
   * Performs the hashing of an AU or a URL.
   * 
   * @param wsParams
   *          A HasherWsParams with the parameters of the hashing operation.
   * @return a HasherWsResult with the result of the hashing operation.
   * @throws LockssWebServicesFault
   */
  @Override
  public HasherWsResult hash(HasherWsParams wsParams)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "hash(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

      // Prepare the hash parameters.
      HasherParams params =
	  new HasherParams(PlatformUtil.getLocalHostname(), false);

      params.setAlgorithm(wsParams.getAlgorithm());
      params.setAuId(wsParams.getAuId());
      params.setChallenge(wsParams.getChallenge());

      Boolean excludeSuspectVersions = wsParams.isExcludeSuspectVersions();
      if (excludeSuspectVersions == null) {
	params.setExcludeSuspectVersions(false);
      } else {
	params.setExcludeSuspectVersions(excludeSuspectVersions.booleanValue());
      }

      params.setHashType(wsParams.getHashType());
      params.setLower(wsParams.getLower());

      Boolean recordFilteredStream = wsParams.isRecordFilteredStream();
      if (recordFilteredStream == null) {
	params.setRecordFilteredStream(false);
      } else {
	params.setRecordFilteredStream(recordFilteredStream.booleanValue());
      }

      params.setResultEncoding(wsParams.getResultEncoding());
      params.setUpper(wsParams.getUpper());
      params.setUrl(wsParams.getUrl());
      params.setVerifier(wsParams.getVerifier());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "params = " + params);

      // Perform the hash.
      HasherResult result = new HasherResult();
      new SimpleHasher(null).hash(params, result);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result = " + result);

      // Prepare the result to be returned.
      HasherWsResult wsResult = new HasherWsResult();
      transferResult(result, wsResult);

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
      return wsResult;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Populates the web services result object with the hash result object.
   * 
   * @param result
   *          A HasherResult with the source of the hash result.
   * @param wsResult
   *          A HasherWsResult to be populated.
   */
  private void transferResult(HasherResult result, HasherWsResult wsResult) {
    wsResult.setStartTime(result.getStartTime());

    File recordFile = result.getRecordFile();
    if (recordFile != null && recordFile.exists() && recordFile.length() > 0) {
      wsResult.setRecordFileName(recordFile.getName());
      wsResult.setRecordFileDataHandler(new DataHandler(new FileDataSource(
	    recordFile)));
    }

    File blockFile = result.getBlockFile();
    if (blockFile != null && blockFile.exists() && blockFile.length() > 0) {
      wsResult.setBlockFileName(blockFile.getName());
      wsResult.setBlockFileDataHandler(new DataHandler(new FileDataSource(
	  blockFile)));
    }

    wsResult.setErrorMessage(result.getRunnerError());
    wsResult.setStatus(result.getRunnerStatus().toString());

    if (result.getHashResult() != null) {
      wsResult.setHashResult(result.getHashResult());
    }

    wsResult.setBytesHashed(result.getBytesHashed());
    wsResult.setFilesHashed(result.getFilesHashed());
    wsResult.setElapsedTime(result.getElapsedTime());
  }

  /**
   * Performs asynchronously the hashing of an AU or a URL.
   * 
   * @param wsParams
   *          A HasherWsParams with the parameters of the hashing operation.
   * @return a HasherWsAsynchronousResult with the result of the hashing
   *         operation.
   * @throws LockssWebServicesFault
   */
  @Override
  public HasherWsAsynchronousResult hashAsynchronously(HasherWsParams wsParams)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "hashAsynchronously(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Invoked.");

      // Prepare the hash parameters.
      HasherParams params =
	  new HasherParams(PlatformUtil.getLocalHostname(), true);

      params.setAlgorithm(wsParams.getAlgorithm());
      params.setAuId(wsParams.getAuId());
      params.setChallenge(wsParams.getChallenge());

      Boolean excludeSuspectVersions = wsParams.isExcludeSuspectVersions();
      if (excludeSuspectVersions == null) {
	params.setExcludeSuspectVersions(false);
      } else {
	params.setExcludeSuspectVersions(excludeSuspectVersions.booleanValue());
      }

      params.setHashType(wsParams.getHashType());
      params.setLower(wsParams.getLower());

      Boolean recordFilteredStream = wsParams.isRecordFilteredStream();
      if (recordFilteredStream == null) {
	params.setRecordFilteredStream(false);
      } else {
	params.setRecordFilteredStream(recordFilteredStream.booleanValue());
      }

      params.setResultEncoding(wsParams.getResultEncoding());
      params.setUpper(wsParams.getUpper());
      params.setUrl(wsParams.getUrl());
      params.setVerifier(wsParams.getVerifier());
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "params = " + params);

      HasherResult result = new HasherResult();
      HasherWsAsynchronousResult wsResult = new HasherWsAsynchronousResult();

      try {
	// Initialize the request time.
	long requestTime = TimeBase.nowMs();
	result.setRequestTime(requestTime);
	wsResult.setRequestTime(requestTime);

	// Obtain a request identifier.
	String requestId;
	synchronized (HASH_REQUESTS) {
	  requestId = SimpleHasher.getReqId(params, result, HASH_REQUESTS);
	}
        if (log.isDebug3())
          log.debug3(DEBUG_HEADER + "requestId = " + requestId);

        // Perform the hash.
        new SimpleHasher(null).startHashingThread(params, result);
	wsResult.setRequestId(requestId);
      } catch (RuntimeException re) {
        log.warning(DEBUG_HEADER, re);
        String errorMessage = "Error starting asynchronous hash thread: "
            + re.toString();
        result.setRunnerError(errorMessage);
        wsResult.setErrorMessage(errorMessage);
      }

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
      return wsResult;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

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
  @Override
  public HasherWsAsynchronousResult getAsynchronousHashResult(String requestId)
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAsynchronousHashResult(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "requestId = " + requestId);

      HasherWsAsynchronousResult wsResult = new HasherWsAsynchronousResult();
      wsResult.setRequestId(requestId);

      // Get the result.
      HasherResult result = getResultByRequestId(requestId, wsResult);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result = " + result);

      if (result != null) {
	// Prepare the result to be returned.
	transferResult(result, wsResult);
	wsResult.setRequestTime(result.getStartTime());
      }

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
      return wsResult;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

  /**
   * Provides the result of an asynchronous hashing operation.
   * 
   * @param requestId
   *          A String with the identifier of the requested asynchronous hashing
   *          operation.
   * @param wsResult
   *          A HasherWsAsynchronousResult where to report problems obtaining
   *          the result of the asynchronous hashing operation.
   * @return a HasherResult with the result of the asynchronous hashing
   *         operation.
   */
  private HasherResult getResultByRequestId(String requestId,
      HasherWsAsynchronousResult wsResult) {
    final String DEBUG_HEADER = "getResultByRequestId(): ";

    // Handle a missing request identifier.
    if (StringUtil.isNullString(requestId)) {
	String errorMessage = "Must supply request identifier";
      log.warning(DEBUG_HEADER + errorMessage);
      wsResult.setStatus(HasherStatus.RequestError.toString());
      wsResult.setErrorMessage(errorMessage);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "wsResult = " + wsResult);
      return null;
    }

    // Get the hash request data.
    ParamsAndResult paramsAndResult;
    synchronized (HASH_REQUESTS) {
      paramsAndResult = HASH_REQUESTS.get(requestId);
    }
    // Handle a missing request.
    if (paramsAndResult == null) {
      String errorMessage = "Cannot find asynchronous hash request '"
	  + requestId + "'";
      log.warning(DEBUG_HEADER + errorMessage);
      wsResult.setStatus(HasherStatus.RequestError.toString());
      wsResult.setErrorMessage(errorMessage);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "wsResult = " + wsResult);
      return null;
    }

    // Get the result.
    HasherResult result = paramsAndResult.result;
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the results of all the asynchronous hashing operations.
   * 
   * @return a List<HasherWsAsynchronousResult> with the results of the hashing
   *         operations.
   * @throws LockssWebServicesFault
   */
  @Override
  public List<HasherWsAsynchronousResult> getAllAsynchronousHashResults()
      throws LockssWebServicesFault {
    final String DEBUG_HEADER = "getAllAsynchronousHashResults(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

      // Initialize the response.
      List<HasherWsAsynchronousResult> wsResults =
	  new ArrayList<HasherWsAsynchronousResult>();

      // Loop through all the existing requests.
      synchronized (HASH_REQUESTS) {
	for (String requestId : HASH_REQUESTS.keySet()) {
	  HasherWsAsynchronousResult wsResult = new HasherWsAsynchronousResult();
	  wsResult.setRequestId(requestId);

	  // Get the result.
	  HasherResult result = HASH_REQUESTS.get(requestId).result;
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result = " + result);

	  // Prepare the result to be returned.
	  transferResult(result, wsResult);
	  wsResult.setRequestTime(result.getStartTime());

	  // Add the result to the response.  
	  wsResults.add(wsResult);
	}
      }
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResults = " + wsResults);
      return wsResults;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }

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
  @Override
  public HasherWsAsynchronousResult removeAsynchronousHashRequest(String
      requestId) throws LockssWebServicesFault {
    final String DEBUG_HEADER = "removeAsynchronousHashRequest(): ";

    try {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "requestId = " + requestId);

      HasherWsAsynchronousResult wsResult = new HasherWsAsynchronousResult();
      wsResult.setRequestId(requestId);

      // Get the result.
      HasherResult result = getResultByRequestId(requestId, wsResult);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "result = " + result);

      // Handle a missing result.
      if (result != null) {
	switch (result.getRunnerStatus()) {
	case NotStarted:
	case Init:
	case Starting:
	case Running:
	  Future<Void> future = result.getFuture();

	  if (future != null) {
	    future.cancel(true);
	  }

	  break;
	default:
	}

	FileUtil.safeDeleteFile(result.getBlockFile());
	FileUtil.safeDeleteFile(result.getRecordFile());

	synchronized (HASH_REQUESTS) {
	  HASH_REQUESTS.remove(requestId);
	}

	wsResult.setStatus(HasherStatus.Done.toString());
      }

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "wsResult = " + wsResult);
      return wsResult;
    } catch (Exception e) {
      throw new LockssWebServicesFault(e);
    }
  }
}
