/*
 * $Id$
 */

/*

Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ContentValidationException;

/**
 * Maps an HTTP result to success (null) or an exception, usually one under
 * CacheException
 */

public class HttpResultMap implements CacheResultMap {
  static Logger log = Logger.getLogger("HttpResultMap");

  int[] SuccessCodes = {200, 203, 304};
  int[] RetrySameUrlCodes = { 408, 409, 413, 500, 502, 503, 504};
  int[] MovePermCodes = {301};
  int[] MoveTempCodes = { 307, 303, 302};
  int[] UnimplementedCodes = {};
  int[] PermissionCodes = { 401, 403,  407};
  int[] ExpectedCodes = { 305, 402};
  int[] RetryDeadLinkCodes = {};
  int[] NoRetryDeadLinkCodes= {204, 300, 400, 404, 405, 406, 410};
  int[] UnexpectedFailCodes = {
    201, 202, 205, 206, 306,
    411, 412, 416, 417, 501, 505};
  int[] UnexpectedNoFailCodes = { 414, 415 };


  /** Abstracts the event we're determining how to handle.  Either an HTTP
   * response or an Exception */
  abstract static class Event {
    String message;

    Event(String message) {
      this.message = message;
    }

    void storeCauseIn(Exception e) {
    }

    String getMessage() {
      return message;
    }

    /** Return string for either response code or Exception */
    abstract String getResultString();

    /** Invoke the handler on the result (responseCode or Exception) */
    @Deprecated
    abstract CacheException invokeHandler(CacheResultHandler handler,
					  ArchivalUnit au,
					  LockssUrlConnection conn)
	throws PluginException;
    
    /** Invoke the handler on the result (responseCode or Exception) */
    abstract CacheException invokeHandler(CacheResultHandler handler,
					  ArchivalUnit au,
					  String url)
	throws PluginException;
  }

  /**  HTTP response event */
  static class ResponseEvent extends Event {
    int responseCode;

    ResponseEvent(int responseCode, String message) {
      super(message);
      this.responseCode = responseCode;
    }

    String getResultString() {
      return Integer.toString(responseCode);
    }
    
    @Deprecated
    CacheException invokeHandler(CacheResultHandler handler,
				 ArchivalUnit au,
				 LockssUrlConnection conn)
	throws PluginException {
      return invokeHandler(handler, au, getConnUrl(conn));
    }
    
    CacheException invokeHandler(CacheResultHandler handler,
				 ArchivalUnit au,
				 String url)
	throws PluginException {
      return handler.handleResult(au, url, responseCode);
    }
  }

  /** Exception thrown attempting HTTP request or reading response */
  static class ExceptionEvent extends Event {
    Exception fetchException;

    ExceptionEvent(Exception fetchException, String message) {
      super(message);
      this.fetchException = fetchException;
    }

    void storeCauseIn(Exception e) {
      e.initCause(fetchException);
    }

    String getResultString() {
      return fetchException.getMessage();
    }
    
    @Deprecated
    CacheException invokeHandler(CacheResultHandler handler, ArchivalUnit au,
				 LockssUrlConnection conn)
	throws PluginException {
      return handler.handleResult(au, getConnUrl(conn), fetchException);
    }
    
    CacheException invokeHandler(CacheResultHandler handler, ArchivalUnit au,
				 String url)
	throws PluginException {
      return handler.handleResult(au, url, fetchException);
    }
  }

  static String getConnUrl(LockssUrlConnection conn) {
    return conn != null ? conn.getURL() : null;
  }

  /** Value in exceptionTable map, specified by plugin: either the class of
   * an exception to throw or a CacheResultHandler instance. */
  public abstract static class ExceptionInfo {
    String fmt;

    public enum Type {Class, Handler};

    ExceptionInfo(String fmt) {
      this.fmt = fmt;
    }

    ExceptionInfo() {
    }

    public abstract Type getType();
    public abstract Class getExceptionClass();
    public abstract CacheResultHandler getHandler();

    /** Return the CacheException appropriate for the event, either from
     * the specified class or CacheResultHandler */
    @Deprecated
    abstract CacheException makeException(ArchivalUnit au,
					  LockssUrlConnection connection,
					  Event evt)
	throws Exception;
    
    abstract CacheException makeException(ArchivalUnit au,
					  String url,
					  Event evt)
	throws Exception;

    String fmtMsg(String s, String msg) {
      if (fmt != null) {
	return String.format(fmt, s);
      }
      if (msg != null) {
	return s + " " + msg;
      }
      return s;
    }
    
    @Deprecated
    CacheException getCacheException(ArchivalUnit au,
				     LockssUrlConnection connection,
				     Event evt) {
      return getCacheException(au, getConnUrl(connection), evt);
    }

    /** Return the exception to throw for this event, or an
     * UnknownCodeException if an error occurs trying to create it */
    CacheException getCacheException(ArchivalUnit au,
				     String url,
				     Event evt) {
      try {
	CacheException cacheException = makeException(au, url, evt);
	return cacheException;
      } catch (Exception ex) {
 	log.error("Can't make CacheException for: " + evt.getResultString(),
		  ex);
	return new
	  CacheException.UnknownCodeException("Unable to make exception:"
					      + ex.getMessage());
      }
    }

    /** Wraps a plugin-supplied CacheResultHandler */
    public static class Handler extends ExceptionInfo {
      CacheResultHandler handler;
      
      Handler(CacheResultHandler handler) {
	this.handler = handler;
      }

      Handler(CacheResultHandler handler, String fmt) {
	super(fmt);
	this.handler = handler;
      }

      @Override
      public Type getType() {
        return Type.Handler;
      }
      @Override
      public Class getExceptionClass() {
        throw new UnsupportedOperationException();
      }
      @Override
      public CacheResultHandler getHandler() {
        return handler;
      }

      @Deprecated
      CacheException makeException(ArchivalUnit au,
				   LockssUrlConnection connection,
				   Event evt)
	  throws PluginException {
	return makeException(au, getConnUrl(connection), evt);
      }
      
      CacheException makeException(ArchivalUnit au,
				   String url,
				   Event evt)
	  throws PluginException {
	return evt.invokeHandler(handler, au, url);
      }

      public boolean equals(Object o) {
        if (o instanceof Handler) {
          Handler oh = (Handler)o;
          return handler.equals(oh.handler);
        }
        return false;
      }

      public String toString() {
	return "[EIH: " + handler + "]";
      }
    }

    /** Wraps a CacheException class */
    public static class Cls extends ExceptionInfo {
      Class ex;
      
      Cls(Class ex) {
	this.ex = ex;
      }

      Cls(Class ex, String fmt) {
	super(fmt);
	this.ex = ex;
      }

      @Override
      public Type getType() {
        return Type.Class;
      }
      @Override
      public Class getExceptionClass() {
        return ex;
      }
      @Override
      public CacheResultHandler getHandler() {
        throw new UnsupportedOperationException();
      }

      @Deprecated
      CacheException makeException(ArchivalUnit au,
				   LockssUrlConnection connection,
				   Event evt)
	  throws Exception {
        return makeException(au, getConnUrl(connection), evt);
      }
      
      CacheException makeException(ArchivalUnit au,
				   String url,
				   Event evt)
	  throws Exception {
	CacheException exception = (CacheException)ex.newInstance();
        exception.initMessage(fmtMsg(evt.getResultString(),
				     evt.getMessage()));
	evt.storeCauseIn(exception);
	return exception;
      }

      public boolean equals(Object o) {
        if (o instanceof Cls) {
          Cls oh = (Cls)o;
          return ex.equals(oh.ex);
        }
        return false;
      }

      public String toString() {
	return "[EIC: " + ex + "]";
      }
    }
  }

  HashMap<Object,ExceptionInfo> exceptionTable = new HashMap();

  public HttpResultMap() {
    initExceptionTable();
  }

  protected void initExceptionTable() {
    // HTTP result codes
    storeArrayEntries(SuccessCodes, CacheSuccess.class);
    storeArrayEntries(RetrySameUrlCodes,
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
 		  CacheException.RetryableNetworkException_2_30S.class,
		  "Unknown host: %s");
    // SocketException subsumes ConnectException, NoRouteToHostException
    // and PortUnreachableException
    storeMapEntry(SocketException.class,
 		  CacheException.RetryableNetworkException_3_30S.class);
    storeMapEntry(LockssUrlConnection.ConnectionTimeoutException.class,
 		  CacheException.RetryableNetworkException_3_30S.class);
    // SocketTimeoutException is an InterruptedIOException, not a
    // SocketException
    storeMapEntry(SocketTimeoutException.class,
 		  CacheException.RetryableNetworkException_3_30S.class);
    // SSL
    storeMapEntry(javax.net.ssl.SSLException.class,
 		  CacheException.RetryableNetworkException_3_30S.class);
    // I don't think these can happen
    storeMapEntry(ProtocolException.class,
 		  CacheException.RetryableNetworkException_3_30S.class);
    storeMapEntry(java.nio.channels.ClosedChannelException.class,
 		  CacheException.RetryableNetworkException_3_30S.class);

    // Default ContentValidationException
    storeMapEntry(ContentValidationException.class,
		  CacheException.UnretryableException.class);
    // Specific ContentValidationException s
    storeMapEntry(ContentValidationException.EmptyFile.class,
		  CacheException.WarningOnly.class);
    storeMapEntry(ContentValidationException.WrongLength.class,
		  CacheException.RetryableNetworkException_3_10S.class);
    storeMapEntry(ContentValidationException.LogOnly.class,
		  CacheException.WarningOnly.class);

    storeMapEntry(ContentValidationException.ValidatorExeception.class,
		  CacheException.UnexpectedNoRetryFailException.class);
}

  public void storeArrayEntries(int[] codeArray, Class exceptionClass) {
    for (int i=0; i< codeArray.length; i++) {
      storeMapEntry(codeArray[i], exceptionClass);
    }
  }

  // For unit tests
  void storeArrayEntries(int[] codeArray, CacheResultHandler handler) {
    for (int i=0; i< codeArray.length; i++) {
      storeMapEntry(codeArray[i], handler);
    }
  }

  /** Map the http result code to the response.  DefinablePlugin calls this
   * without knowing whether it's passing a CacheResultHandler instance or
   * a CacheException class.  Should be cleaned up but awkwardness is
   * inconsequential */
  public void storeMapEntry(int code, Object response) {
    if (response instanceof CacheResultHandler) {
      storeMapEntry(code,
		    new ExceptionInfo.Handler((CacheResultHandler)response));
    } else if (response instanceof Class) {
      storeMapEntry(code, new ExceptionInfo.Cls((Class)response));
    } else {
      throw new RuntimeException("Unsupported response type: " + response);
    }      
  }

  /** Map the http result code to the response.  DefinablePlugin calls this
   * without knowing whether it's passing a CacheResultHandler instance or
   * a CacheException class.  Should be cleaned up but awkwardness is
   * inconsequential */
  public void storeMapEntry(Class fetchExceptionClass, Object response) {
    if (response instanceof CacheResultHandler) {
      storeMapEntry(fetchExceptionClass, (CacheResultHandler)response);
    } else if (response instanceof Class) {
      storeMapEntry(fetchExceptionClass, (Class)response);
    } else {
      throw new RuntimeException("Unsupported response type: " + response);
    }      
  }

  /** Map the http result code to the CacheException class */
  public void storeMapEntry(int code, Class exceptionClass) {
    storeMapEntry(code, exceptionClass, null);
  }

  /** Map the http result code to the CacheException class */
  public void storeMapEntry(int code, Class exceptionClass, String fmt) {
    storeMapEntry(code, new ExceptionInfo.Cls(exceptionClass, fmt));
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheException class */
  public void storeMapEntry(Class fetchExceptionClass, Class exceptionClass) {
    storeMapEntry(fetchExceptionClass, exceptionClass, null);
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheException class */
  public void storeMapEntry(Class fetchExceptionClass, Class exceptionClass,
			    String fmt) {
    storeMapEntry(fetchExceptionClass,
		  new ExceptionInfo.Cls(exceptionClass, fmt));
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheResultHandler instance */
  public void storeMapEntry(Class fetchExceptionClass,
			    CacheResultHandler handler) {
    storeMapEntry(fetchExceptionClass,
		  new ExceptionInfo.Handler(handler));
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheResultHandler instance */
  public void storeMapEntry(Class fetchExceptionClass,
			    CacheResultHandler handler,
			    String fmt) {
    storeMapEntry(fetchExceptionClass,
		  new ExceptionInfo.Handler(handler, fmt));

  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheResultHandler instance */
  public void storeMapEntry(Class fetchExceptionClass,
			    ExceptionInfo ei) {
    exceptionTable.put(fetchExceptionClass, ei);
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheResultHandler instance */
  public void storeMapEntry(int code,
			    ExceptionInfo ei) {
    exceptionTable.put(code, ei);
  }

  public CacheException getMalformedURLException(Exception nestedException) {
    return new CacheException.MalformedURLException(nestedException);
  }

  public CacheException getRepositoryException(Exception nestedException) {
    return new CacheException.RepositoryException(nestedException);
  }

  public CacheException checkResult(ArchivalUnit au,
				    LockssUrlConnection connection) {
    try {
      int code = connection.getResponseCode();
      String msg = connection.getResponseMessage();
      return mapException(au, connection, code, msg);
    }
    catch (RuntimeException ex) {
      return new CacheException.UnknownExceptionException(ex);
    }
  }
  public CacheException mapException(ArchivalUnit au,
				     LockssUrlConnection connection,
				     int responseCode,
				     String message)  {
    return mapException(au, getConnUrl(connection),
			responseCode, message);
  }
  public CacheException mapException(ArchivalUnit au,
				     String url,
				     int responseCode,
				     String message)  {

    Event evt = new ResponseEvent(responseCode, message);
    ExceptionInfo ei = exceptionTable.get(responseCode);
    if (ei == null) {
      if (message != null) {
	return new CacheException.UnknownCodeException("Unknown result code: "
						       + responseCode + ": "
						       + message);
      } else {
	return new CacheException.UnknownCodeException("Unknown result code: "
						       + responseCode);
      }
    }
    CacheException cacheException = ei.getCacheException(au, url, evt);
      
    // Instance of marker class means success
    if (cacheException instanceof CacheSuccess) {
      return null;
    }
    return cacheException;
  }

  public CacheException mapException(ArchivalUnit au,
				     LockssUrlConnection connection,
				     Exception fetchException,
				     String message)  {
    return mapException(au, getConnUrl(connection),
			fetchException, message);
  }
  public CacheException mapException(ArchivalUnit au,
				     String url,
				     Exception fetchException,
				     String message)  {

    Event evt = new ExceptionEvent(fetchException, message);
    ExceptionInfo ei = findNearestException(fetchException);
    if (ei == null) {
      if (message != null) {
	return
	  new CacheException.UnknownExceptionException(("Unmapped exception: "
							+ fetchException + ": "
							+ message),
						       fetchException);
      } else {
	return
	  new CacheException.UnknownExceptionException(("Unmapped exception: "
							+ fetchException),
						       fetchException);
      }
    }

    CacheException cacheException = ei.getCacheException(au, url, evt);
      
    // Instance of marker class means success
    if (cacheException instanceof CacheSuccess) {
      return null;
    }
    return cacheException;
  }

  ExceptionInfo findNearestException(Exception fetchException) {
    Class exClass = fetchException.getClass();
    ExceptionInfo resultEi;
    do {
      resultEi = exceptionTable.get(exClass);
    } while (resultEi == null
	     && ((exClass = exClass.getSuperclass()) != null
		 && exClass != Exception.class));
    return resultEi;
  }

  public Map<Object,ExceptionInfo> getExceptionMap() {
    return Collections.unmodifiableMap(exceptionTable);
  }

  public String toString() {
    return "[HRM: " + exceptionTable + "]";
  }
}
