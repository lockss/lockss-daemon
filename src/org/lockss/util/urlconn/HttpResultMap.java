/*

Copyright (c) 2000-2021 Board of Trustees of Leland Stanford Jr. University,
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
 * Maps an HTTP result (response code or exception thrown during
 * processing) to an action, either success or an exception, usually
 * under CacheException
 */
public class HttpResultMap implements CacheResultMap {
  static Logger log = Logger.getLogger("HttpResultMap");

  /** Multipurpose enum used to:<ul>
   *
   * <li>Declare the default mappings for new HttpResultMap
   * instances</li>
   *
   * <li>Assign names to groups (categories) of results so plugins can
   * remap them without knowing/repeating all the codes/exceptions in
   * a category</li>
   *
   * </ul>
   * 
   */
  public enum HttpResultCodeCategory {
    AuthCodes(L(401, 402, 403, 407, 495, 496, 511, 526, 561),
              CacheException.PermissionException.class),
    ClientErrorCodes(L(400, 405, 411, 413, 414, 415, 416, 417, 421,
                       431, 449, 463, 494, 497, 498, 499, 510),
                     CacheException.UnexpectedNoRetryFailException.class),
    DeadLinkCodes(L(204, 404, 410, 451),
                         CacheException.NoRetryDeadLinkException.class),
    InformationalCodes(L(100, 101, 102, 103),
                       CacheException.UnexpectedNoRetryFailException.class),
    PermenentServerCondiditionCodes(L(406, 501, 505, 506, 520, 525, 530),
                        CacheException.UnexpectedNoRetryFailException.class),
    RedirectTempCodes(L(302, 303, 307),
                  CacheException.NoRetryTempUrlException.class),
    RedirectPermCodes(L(301, 308),
                  CacheException.NoRetryPermUrlException.class),
    RedirectCodes(L(301, 302, 303, 307, 308)),
    RetryableNetworkCodes(L(460, 499),
                          CacheException.RetryableNetworkException_3_30S.class),
    ServerLimitCodes(L(509, 529),
                     CacheException.RetryableNetworkException_3_30S.class),
    SuccessCodes(L(200, 203, 304),
                 CacheSuccess.class),
    TimeoutCodes(L(408, 440, 522, 524, 527, 598),
                 CacheException.RetrySameUrlException.class),
    TransientServerConditionCodes(L(409, 412, 426, 429,
                                    500, 502, 503, 504, 507, 521, 523),
                 CacheException.RetrySameUrlException.class),
    UnexpectedCodes(L(201, 206, 418, 444, 530),
                 CacheException.UnexpectedNoRetryFailException.class),
    UnHandledCodes(L(202, 205, 218, 226, 300, 305, 306,
                     419, 420, 420, 426, 428, 430, 450, 451),
                 CacheException.UnexpectedNoRetryFailException.class),
    WebDAVCodes(L(207, 208, 422, 423, 424, 508),
                 CacheException.UnexpectedNoRetryFailException.class),
    

    // IOExceptions
    MalformedUrl(L(MalformedURLException.class),
                 CacheException.MalformedURLException.class,
                 "Malformed URL: %s"),

    UnkownHost(L(UnknownHostException.class),
               CacheException.RetryableNetworkException_2_30S.class,
               "Unknown host: %s"),

    // Network errors, timeouts, etc. default retryable
    NetworkError(L(
                   // SocketException subsumes ConnectException,
                   // NoRouteToHostException and PortUnreachableException
                   SocketException.class,

                   LockssUrlConnection.ConnectionTimeoutException.class,

                   // SocketTimeoutException is an InterruptedIOException, not a
                   // SocketException
                   SocketTimeoutException.class,
                   javax.net.ssl.SSLException.class,
                   ProtocolException.class,
                   java.nio.channels.ClosedChannelException.class),
                 CacheException.RetryableNetworkException_3_30S.class
                 ),

    // ContentValidationException
    ContentValidation(L(ContentValidationException.class),
                      CacheException.UnretryableException.class),
    EmptyFile(L(ContentValidationException.EmptyFile.class),
              CacheException.WarningOnly.class),
    WrongLength(L(ContentValidationException.WrongLength.class),
                CacheException.RetryableNetworkException_3_10S.class),
    LogOnly(L(ContentValidationException.LogOnly.class),
            CacheException.WarningOnly.class);

    private Class cls;       // Result class, Usually a CacheException
    private Triggers triggers; // Triggering response codes and/or exceptions
    private String fmt;

    /** Create a category mapping a list of HTTP result codes to a
     * CacheException or CacheResultHandler */
    private HttpResultCodeCategory(Triggers triggers, Class cls) {
      this(triggers, cls, null);
    }

    /** Create a category mapping a list of HTTP result codes to a
     * CacheException or CacheResultHandler */
    private HttpResultCodeCategory(Triggers triggers, Class cls, String fmt) {
      this.triggers = triggers;
      this.cls = cls;
      this.fmt = fmt;
    }

    /** Create a category naming a triggering exception, without
     * establishing a default mapping */
    private HttpResultCodeCategory(Triggers triggers) {
      this.triggers = triggers;
    }

    /** Return the list of response codes comprising the category, null */
    public List<Integer> getCodes() {
      return triggers.getCodes();
    }

    /** Return the trigger Exception class, or null */
    public List<Class> getTriggerClasses() {
      return triggers.getTriggerClasses();
    }

    /** Return the default Exception mapping */
    public Class getExceptionClass() {
      return cls;
    }

    /** Return the default Exception mapping */
    public String getFmt() {
      return fmt;
    }
  }

  /** Helper class to represent lists of triggers (result codes and/or
   * exceptions) in HttpResultCodeCategory */
  static class Triggers {
    private List<Integer> codes;
    private List<Class> triggerClasses;

    Triggers(Object[] lst) {
      List<Integer> ints = new ArrayList<>();
      List<Class> eClasses = new ArrayList<>();
      for (Object x : lst) {
        if (x instanceof Integer) {
          ints.add((Integer)x);
        }
        if (x instanceof Class) {
          eClasses.add((Class)x);
        }
      }
      if (!ints.isEmpty()) {
        this.codes = Collections.unmodifiableList(ints);
      }

      if (!eClasses.isEmpty()) {
        this.triggerClasses = Collections.unmodifiableList(eClasses);
      }
    }

    List<Integer> getCodes() {
      return codes;
    }

    List<Class> getTriggerClasses() {
      return triggerClasses;
    }
  }

  /** Terse way to create a Triggers */
  private static Triggers L(Object... triggers) {
    return new Triggers(triggers);
  }

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

  Map<Object,ExceptionInfo> exceptionTable = new HashMap<>();

  public HttpResultMap() {
    initExceptionTable();
  }

  protected void initExceptionTable() {
    // Initialize map with all entries specified by
    // HttpResultCodeCategory enum constants
    for (HttpResultCodeCategory category : HttpResultCodeCategory.values()) {
      if (category.getExceptionClass() != null) {
        storeResultCategoryEntries(category, category.getExceptionClass());
      }
    }
  }

  /** Set the exception for all result codes of specified category */
  public void storeResultCategoryEntries(HttpResultCodeCategory category,
                                         Object response) {
    if (category.getCodes() != null) {
      for (int code : category.getCodes()) {
        storeMapEntry(code, response);
      }
    } else if (category.getTriggerClasses() != null) {
      for (Class cls : category.getTriggerClasses()) {
        if (response instanceof Class) {
          storeMapEntry(cls, (Class)response, category.getFmt());
        } else {
          storeMapEntry(cls, response);
        }
      }
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

  @Deprecated
  public CacheException getMalformedURLException(Exception nestedException) {
    return getMalformedURLException(null, null, nestedException, null);
  }

  public CacheException getMalformedURLException(ArchivalUnit au,
                                                 String url,
                                                 Exception nestedException,
                                                 String message) {
    return mapException(au, url, nestedException, message);
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
    log.critical("nearest to: " + fetchException + " = " + exClass);
    return resultEi;
  }

  public Map<Object,ExceptionInfo> getExceptionMap() {
    return Collections.unmodifiableMap(exceptionTable);
  }

  public String toString() {
    return "[HRM: " + exceptionTable + "]";
  }
}
