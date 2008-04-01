/*
 * $Id: HttpResultMap.java,v 1.8 2008-04-01 08:02:11 tlipkis Exp $
 */

/*

Copyright (c) 2000-2004 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.util.urlconn;

import java.util.*;
import java.net.*;

import org.lockss.util.*;

/**
 * Maps an HTTP result to success (null) or an exception, usually one under
 * CacheException
 */

public class HttpResultMap implements CacheResultMap {
  static Logger log = Logger.getLogger("HttpResultMap");

  int[] SuccessCodes = {200, 203, 304};
  int[] SameUrlCodes = { 408, 409, 413, 500, 502, 503, 504};
  int[] MovePermCodes = {301};
  int[] MoveTempCodes = { 307, 303, 302};
  int[] UnimplementedCodes = {};
  int[] PermissionCodes = { 401, 403,  407};
  int[] ExpectedCodes = { 305, 402};
  int[] RetryDeadLinkCodes = {};
  int[] NoRetryDeadLinkCodes= {204, 300, 404, 405, 406, 410};
  int[] UnexpectedFailCodes = {
      201, 202, 205, 206, 306, 400,
      411, 412, 416, 417, 501, 505};
  int[] UnexpectedNoFailCodes = { 414, 415 };

  HashMap<Object,Class> exceptionTable = new HashMap();

  public HttpResultMap() {
    initExceptionTable();
  }

  protected void initExceptionTable() {
    // HTTP result codes
    storeArrayEntries(SuccessCodes, CacheSuccess.MARKER);
    storeArrayEntries(SameUrlCodes,
                      CacheException.RetrySameUrlException.class);
    storeArrayEntries(MovePermCodes,
                      CacheException.NoRetryPermUrlException.class);
    storeArrayEntries(MoveTempCodes,
                      CacheException.NoRetryTempUrlException.class);
    storeArrayEntries(UnimplementedCodes,
                      CacheException.UnimplementedCodeException.class);
    storeArrayEntries(PermissionCodes,
                      CacheException.PermissionException.class);
    storeArrayEntries(ExpectedCodes,
                      CacheException.ExpectedNoRetryException.class);
    storeArrayEntries(UnexpectedFailCodes,
                      CacheException.UnexpectedNoRetryFailException.class);
    storeArrayEntries(UnexpectedNoFailCodes,
                      CacheException.UnexpectedNoRetryNoFailException.class);
    storeArrayEntries(RetryDeadLinkCodes,
                      CacheException.RetryDeadLinkException.class);
    storeArrayEntries(NoRetryDeadLinkCodes,
                      CacheException.NoRetryDeadLinkException.class);

    // IOExceptions
    storeMapEntry(UnknownHostException.class,
 		  CacheException.RetryableNetworkException_2_30S.class);
    // SocketException subsumes ConnectException, NoRouteToHostException
    // and PortUnreachableException
    storeMapEntry(SocketException.class,
 		  CacheException.RetryableNetworkException_3_30S.class);
    // I don't think these can happen
    storeMapEntry(ProtocolException.class,
 		  CacheException.RetryableNetworkException_3_30S.class);
    storeMapEntry(java.nio.channels.ClosedChannelException.class,
 		  CacheException.RetryableNetworkException_3_30S.class);
  }

  public void storeArrayEntries(int[] codeArray, Class exceptionClass) {
    for (int i=0; i< codeArray.length; i++) {
      storeMapEntry(codeArray[i], exceptionClass);
    }
  }

  /** Map the http result code to the CacheException class */
  public void storeMapEntry(int code, Class exceptionClass) {
    exceptionTable.put(new Integer(code), exceptionClass);
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheException class */
  public void storeMapEntry(Class fetchExceptionClass, Class exceptionClass) {
    exceptionTable.put(fetchExceptionClass, exceptionClass);
  }

  public Class getExceptionClass(int resultCode) {
    return exceptionTable.get(resultCode);
  }

  public CacheException getMalformedURLException(Exception nestedException) {
    return new CacheException.MalformedURLException(nestedException);
  }

  public CacheException getRepositoryException(Exception nestedException) {
    return new CacheException.RepositoryException(nestedException);
  }

  public CacheException checkResult(LockssUrlConnection connection) {
    try {
      int code = connection.getResponseCode();
      String msg = connection.getResponseMessage();
      return mapException(connection, code, msg);
    }
    catch (RuntimeException ex) {
      return new CacheException.UnknownExceptionException(ex);
    }
  }

  public CacheException mapException(LockssUrlConnection connection,
				     int resultCode, String message)  {

    Integer key = new Integer(resultCode);
    Class exceptionClass = exceptionTable.get(key);

    // Marker class means success
    if (exceptionClass == CacheSuccess.MARKER) {
      return null;
    }
    if (exceptionClass == null) {
      if (message != null) {
	return new CacheException.UnknownCodeException(
          "Unknown result code: " + resultCode + ": " + message);
      } else {
	return new CacheException.UnknownCodeException(
          "Unknown result code: " + resultCode);
      }
    }
    try {
      Object exception = exceptionClass.newInstance();
      CacheException cacheException;
//       // check for an instance of handler class
//       if (exception instanceof CacheResultHandler) {
// 	CacheResultHandler handler = (CacheResultHandler)exception;
//         cacheException = handler.handleResult(resultCode, connection);
//       }
//       else {
        cacheException = (CacheException)exception;
        cacheException.initMessage((message != null)
				   ? (resultCode + " " + message)
				   : Integer.toString(resultCode));
//       }
      return cacheException;
    }
    catch (Exception ex) {
      log.error("Can't make CacheException for: " + resultCode, ex);
      return new CacheException.UnknownCodeException(
          "Unable to make exception:" + ex.getMessage());
    }
  }

  public CacheException mapException(LockssUrlConnection connection,
				     Exception fetchException,
				     String message)  {

    Class exceptionClass = findNearestException(fetchException);

    // Marker class means success
    if (exceptionClass == CacheSuccess.MARKER) {
      return null;
    }
    if (exceptionClass == null) {
      if (message != null) {
	return new CacheException.UnknownExceptionException(
          "Unmapped exception: " + fetchException + ": " + message);
      } else {
	return new CacheException.UnknownExceptionException(
          "Unmapped exception: " + fetchException);
      }
    }
    try {
      Object exception = exceptionClass.newInstance();
      CacheException cacheException;
//       // check for an instance of handler class
//       if (exception instanceof CacheResultHandler) {
// 	CacheResultHandler handler = (CacheResultHandler)exception;
//         cacheException = handler.handleResult(fetchException, connection);
//       }
//       else {
        cacheException = (CacheException)exception;
	if (fetchException != null) {
	  cacheException.initCause(fetchException);
	}
        cacheException.initMessage((message != null)
				   ? (fetchException.getMessage() +
				      " " + message)
				   : fetchException.getMessage());
//       }
      return cacheException;
    }
    catch (Exception ex) {
      log.error("Can't make CacheException for: " + fetchException, ex);
      return new CacheException.UnknownExceptionException(
          "Unable to make exception:" + ex.getMessage());
    }
  }

  Class findNearestException(Exception fetchException) {
    Class exClass = fetchException.getClass();
    Class resultClass;
    do {
      resultClass = exceptionTable.get(exClass);
    } while (resultClass == null
	     && ((exClass = exClass.getSuperclass()) != null
		 && exClass != Exception.class));
    return resultClass;
  }
}
