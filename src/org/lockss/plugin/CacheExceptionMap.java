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
