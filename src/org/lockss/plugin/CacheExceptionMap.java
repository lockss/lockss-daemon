/*
 * $Id: CacheExceptionMap.java,v 1.2 2004-02-05 03:01:21 clairegriffin Exp $
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
package org.lockss.plugin;

import java.util.HashMap;
import java.net.HttpURLConnection;
import java.io.*;

/**
 * <p>CacheExceptionHandler: </p>
 * <p>@author Claire Griffin</p>
 * <p>@version 1.0</p>
 *
 */

public class CacheExceptionMap {
  int[] SuccessCodes = {200, 203};
  int[] SameUrlCodes = {
      408, 413, 500, 502, 503, 504};
  int[] MovePermCodes = {
      301};
  int[] MoveTempCodes = {
      307, 303, 302};
  int[] UnimplementedCodes = {
      300, 204};
  int[] ExpectedCodes = {
      401, 402, 403, 404, 405, 406, 407, 410, 305};
  int[] UnexpectedCodes = {
      201, 202, 205, 206, 304, 306, 400, 409,
      411, 412, 414, 415, 416, 417, 501, 505};

  static final String EXCEPTION_CLASS_ROOT = "org.lockss.plugin.CacheException$";
  static final String SUCCESS_STRING = "OK";

  HashMap exceptionTable = new HashMap();

  public CacheExceptionMap() {
    initExceptionTable();
  }

  protected void initExceptionTable() {
    storeArrayEntries(SuccessCodes, SUCCESS_STRING);
    storeArrayEntries(SameUrlCodes,
                      EXCEPTION_CLASS_ROOT + "RetrySameUrlException");
    storeArrayEntries(MovePermCodes,
                      EXCEPTION_CLASS_ROOT + "RetryPermUrlException");
    storeArrayEntries(MoveTempCodes,
                      EXCEPTION_CLASS_ROOT + "RetryTempUrlException");
    storeArrayEntries(UnimplementedCodes,
                      EXCEPTION_CLASS_ROOT + "UnimplementedCodeException");
    storeArrayEntries(ExpectedCodes,
                      EXCEPTION_CLASS_ROOT + "ExpectedNoRetryException");
    storeArrayEntries(UnexpectedCodes,
                      EXCEPTION_CLASS_ROOT + "UnexpectedNoRetryException");
  }

  public void storeArrayEntries(int[] codeArray, String exceptionClassName) {
    for(int i=0; i< codeArray.length; i++) {
      storeMapEntry(codeArray[i], exceptionClassName);
    }
  }

  public void storeMapEntry(int code, String exceptionClassName) {
    exceptionTable.put(new Integer(code), exceptionClassName);
  }


  protected String getExceptionClassName(int resultCode) {
    return (String) exceptionTable.get(new Integer(resultCode));
  }

  public CacheException getHostException(String message) {
    return new CacheException.HostException(message);
  }

  public CacheException getRepositoryException(String message) {
    return new CacheException.RepositoryException(message);
  }

  public CacheException checkException(HttpURLConnection connection) {
    try {
      int code = connection.getResponseCode();
      String msg = connection.getResponseMessage();
      return mapException(connection, code, "response " + code + ": " + msg);
    }
    catch (IOException ex) {
      return getHostException(ex.getMessage());
    }
  }

  protected CacheException mapException(HttpURLConnection connection,
                                        int resultCode, String message)  {

    Integer key = new Integer(resultCode);
    String exceptionClassName = (String) exceptionTable.get(key);

    try {
      // check for return codes that we can ignore
      if(exceptionClassName.equals(SUCCESS_STRING)) {
        return null;
      }
      Class exceptionClass = Class.forName(exceptionClassName);
      Object exception = exceptionClass.newInstance();
      CacheException cacheException;
      // check for an instance of handler class
      if (exception instanceof CacheExceptionHandler) {
        cacheException = ( (CacheExceptionHandler) exception).handleException(
            resultCode, connection);
      }
      else {
        cacheException = (CacheException) exception;
        cacheException.initMessage(message);
      }
      return cacheException;
    }
    catch (Exception ex) {
      return new CacheException.UnknownCodeException(
          "Unable to make exception:" + ex.getMessage());
    }
  }

}
