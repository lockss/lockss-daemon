/*

Copyright (c) 2000-2022 Board of Trustees of Leland Stanford Jr. University,
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

import java.lang.reflect.Constructor;
import java.util.*;
import java.net.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ContentValidationException;

/**
 * Maps an HTTP result (response code or exception thrown during
 * processing) to an action represented as a CacheException.  Many of
 * the CacheExceptions are handled specially by the crawler
 * infrastructure.
 */
public class HttpResultMap implements CacheResultMap {
  static Logger log = Logger.getLogger("HttpResultMap");

  /** This enum is used to:<ul>
   *
   * <li>Establish the default mappings.</li>
   *
   * <li>Assign names to categories of triggers (response codes or
   * Exceptions) so plugins can remap the whole set without
   * knowing/repeating all the codes/exceptions.  E.g., to change the
   * retry count/interval for all retryable errors, or to map a set of
   * special purpose codes (WebDAV) to a handler.  The list of
   * triggers may include other categories, which will be
   * included/expanded recursively.  Do NOT create cycles in category
   * references.</li>
   *
   * </ul>
   *
   * These are the CacheExceptions used in the default mapping.  Their
   * characteristics are defined in {@link CacheException} but are
   * repeated here<! --- because I only need them when I'm looking at
   * this file and I'm tired of looking them up --->.  "warning" does
   * not prevent crawl from succeeding, "error" allows crawl to
   * proceed but will end with an error, "abort" immediately
   * terminates crawl with an error.  There's some inconsistent naming
   * and overlap in behavior.<ul>
   *
   * <li>CacheSuccess - "Exception" that indicates success.</li>
   *
   * <li>CacheException.WarningOnly - Warning in log, no error.</li>
   *
   * <li>CacheException.NoRetryDeadLinkException - No retry, warning.</li>
   *
   * <li>CacheException.UnexpectedNoRetryFailException - No retry, error.</li>
   *
   * <li>CacheException.PermissionException - No retry, abort.</li>
   *
   * <li>CacheException.RetrySameUrlException - Retry, then error if
   * no retry succeeds.  Retries {@value
   * org.lockss.crawler.BaseCrawler#DEFAULT_DEFAULT_RETRY_COUNT} times
   * at {@value
   * org.lockss.crawler.BaseCrawler#DEFAULT_DEFAULT_RETRY_DELAY}ms
   * intervals by default; can be configured globally with {@value
   * org.lockss.crawler.BaseCrawler#PARAM_DEFAULT_RETRY_COUNT} and
   * {@value
   * org.lockss.crawler.BaseCrawler#PARAM_DEFAULT_RETRY_DELAY}.</li>
   *
   * <li>CacheException.RetryableNetworkException - Retry, then error if
   * no retry succeeds.  Retries {@value
   * org.lockss.crawler.BaseCrawler#DEFAULT_DEFAULT_NETWORK_RETRY_COUNT} times
   * at {@value
   * org.lockss.crawler.BaseCrawler#DEFAULT_DEFAULT_NETWORK_RETRY_DELAY}ms
   * intervals by default; can be configured globally with {@value
   * org.lockss.crawler.BaseCrawler#PARAM_DEFAULT_NETWORK_RETRY_COUNT} and
   * {@value
   * org.lockss.crawler.BaseCrawler#PARAM_DEFAULT_NETWORK_RETRY_DELAY}.</li>
   *
   * <li>CacheException.NoRetryPermUrlException<br>
   * CacheException.NoRetryTempUrlException - Trigger logic to follow
   * redirect.</li>
   *
   * <li>CacheException.MalformedURLException - no retry, no error</li>
   *
   * <li>CacheException.UnretryableException - no retry, error</li>
   *
   * </ul>
   */
  public enum HttpResultCodeCategory {
    /** Unexpected (and unhandled) informational result codes. */
    InformationalCodes(L(100, 101, 103),
                       CacheException.UnexpectedNoRetryFailException.class),
    /** Result codes indicating success. */
    SuccessCodes(L(200, 203, 304),
                 CacheSuccess.class),
    /** Result codes indicating an authorization failure (loosely
     * interpreted). */
    AuthCodes(L(401, 402, 403, 407, 495, 496, 511, 526, 561),
              CacheException.PermissionException.class),
    /** Result codes indicating the client sent a request that can't
     * be processed (e.g., malformed). */
    ClientErrorCodes(L(400, 405, 406, 411, 412, 414, 415, 416, 417, 421,
                       431, 449, 463, 494, 497, 498, 510),
                     CacheException.UnexpectedNoRetryFailException.class),
    /** Result codes indicating a routine dead link that normally
     * shouldn't cause the crawl to fail. */
    DeadLinkCodes(L(204, 404, 410, 451),
                  CacheException.NoRetryDeadLinkException.class),
    /** Result codes indicating the server is unable to full. */
    PermenentServerCondiditionCodes(L(501, 505, 506, 520, 525, 530),
                                    CacheException.UnexpectedNoRetryFailException.class),
    /** Result codes indicating a server condition that may be transient. */
    TransientServerConditionCodes(L(409, 413, 429,
                                    500, 502, 503, 504, 507, 521, 523),
                                  CacheException.RetrySameUrlException.class),
    /** Result codes indicating a temporary redirect. */
    RedirectTempCodes(L(302, 303, 307),
                      CacheException.NoRetryTempUrlException.class),
    /** Result codes indicating a permanent redirect. */
    RedirectPermCodes(L(301, 308),
                      CacheException.NoRetryPermUrlException.class),
    /** All redirect codes */
    RedirectCodes(L(RedirectPermCodes, RedirectTempCodes)),
    /** Result codes indicating a possibly transient network error. */
    RetryableNetworkCodes(L(460, 499),
                          CacheException.RetryableNetworkException.class),
    /** Result codes indicating some server limit has been exceeded. */
    ServerLimitCodes(L(509, 529),
                     CacheException.RetrySameUrlException.class),
    /** Result codes indicating a server timeout. */
    ServerTimeoutCodes(L(440, 408, 522, 524, 527, 598),
                       CacheException.RetrySameUrlException.class),
    /** Result codes that should not be received (mostly because we
     * don't issue any request thatr would cause them). */
    UnexpectedCodes(L(201, 206, 418, 444, 530),
                    CacheException.UnexpectedNoRetryFailException.class),
    /** Result codes that servers might send in unusual circumstances,
     * which we don't handle. */
    UnHandledCodes(L(202, 205, 218, 226, 300, 305, 306,
                     419, 420, 426, 428, 430, 450),
                   CacheException.UnexpectedNoRetryFailException.class),
    /** WebDAV result codes. */
    WebDAVCodes(L(102, 207, 208, 422, 423, 424, 508),
                CacheException.UnexpectedNoRetryFailException.class),

    // IOExceptions
    /** Malformed URL */
    MalformedUrl(L(MalformedURLException.class),
                 CacheException.MalformedURLException.class,
                 "Malformed URL: %s"),

    /** Unknown host */
    UnkownHost(L(UnknownHostException.class),
               CacheException.RetryableNetworkException.class,
               "Unknown host: %s"),

    // Network errors, timeouts, etc. default retryable

    /** Network timeouts, may be transient. */
    NetworkTimeout(L(
                     LockssUrlConnection.ConnectionTimeoutException.class,
                     // SocketTimeoutException is an
                     // InterruptedIOException, not a SocketException
                     SocketTimeoutException.class),
                   CacheException.RetryableNetworkException.class),

    /** Various network errors that may be transient. */
    NetworkError(L(NetworkTimeout,
                   // SocketException subsumes ConnectException,
                   // NoRouteToHostException and PortUnreachableException
                   SocketException.class,
                   javax.net.ssl.SSLException.class,
                   ProtocolException.class,
                   java.nio.channels.ClosedChannelException.class),
                 CacheException.RetryableNetworkException.class
                 ),

    /** Undifferentiated content validation error. */
    ContentValidation(L(ContentValidationException.class),
                      CacheException.UnretryableException.class),
    /** Empty file received.  Normally just a warning. */
    EmptyFile(L(ContentValidationException.EmptyFile.class),
              CacheException.WarningOnly.class),
    /** Length of received file doesn't match Content-Length header. */
    WrongLength(L(ContentValidationException.WrongLength.class),
                CacheException.RetrySameUrlException.class),
    /** Log-only ContentValidationException */
    LogOnly(L(ContentValidationException.LogOnly.class),
            CacheException.WarningOnly.class),
    ValidatorError(L(ContentValidationException.ValidatorExeception.class),
                   CacheException.UnexpectedNoRetryFailException.class),

    /** All timeouts, server and network. */
    Timeout(L(ServerTimeoutCodes, NetworkTimeout)),

    /** All retryable conditions, both server responses and network
     * errors. */
    Retryable(L(RetryableNetworkCodes, NetworkError,
                ServerTimeoutCodes, ServerLimitCodes,
                TransientServerConditionCodes, WrongLength));


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

    /** Create a category naming a set of triggering exceptions, without
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

    /** Return the trigger categories to expand, or null */
    public List<HttpResultCodeCategory> getTriggerCategories() {
      return triggers.getTriggerCategories();
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

  /** Helper class to represent lists of triggers (which can be result
   * code, exception, or HttpResultCodeCategory).  These are the LHS
   * of the mappings. */
  static class Triggers {
    private List<Integer> codes;
    private List<Class> triggerClasses;
    private List<HttpResultCodeCategory> categories;

    Triggers(Object[] lst) {
      List<Integer> ints = new ArrayList<>();
      List<Class> eClasses = new ArrayList<>();
      List<HttpResultCodeCategory> cats = new ArrayList<>();
      for (Object x : lst) {
        if (x instanceof Integer) {
          ints.add((Integer)x);
        }
        if (x instanceof HttpResultCodeCategory) {
          cats.add((HttpResultCodeCategory)x);
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

      if (!cats.isEmpty()) {
        this.categories = Collections.unmodifiableList(cats);
      }
    }

    List<Integer> getCodes() {
      return codes;
    }

    List<Class> getTriggerClasses() {
      return triggerClasses;
    }

    List<HttpResultCodeCategory> getTriggerCategories() {
      return categories;
    }
  }

  /** Terse way to create a Triggers */
  private static Triggers L(Object... triggers) {
    return new Triggers(triggers);
  }

  Map<Object,ResultAction> exceptionTable = new HashMap<>();

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

  /** Set the exception for all result codes and exceptions of
   * specified category. */
  public void storeResultCategoryEntries(HttpResultCodeCategory category,
                                         Object response) {
    if (category.getCodes() != null) {
      for (int code : category.getCodes()) {
        storeMapEntry(code, response);
      }
    }
    if (category.getTriggerClasses() != null) {
      for (Class cls : category.getTriggerClasses()) {
        if (response instanceof Class) {
          storeMapEntry(cls, (Class)response, category.getFmt());
        } else {
          storeMapEntry(cls, response);
        }
      }
    }
    // Cycles in Category macros will cause stack overflow or infinite
    // loop here.
    if (category.getTriggerCategories() != null) {
      for (HttpResultCodeCategory cat : category.getTriggerCategories()) {
        storeResultCategoryEntries(cat, response);
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
		    ResultAction.handler((CacheResultHandler)response));
    } else if (response instanceof Class) {
      storeMapEntry(code, ResultAction.exClass((Class)response));
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
    storeMapEntry(code, ResultAction.exClass(exceptionClass, fmt));
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
		  ResultAction.exClass(exceptionClass, fmt));
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheResultHandler instance */
  public void storeMapEntry(Class fetchExceptionClass,
			    CacheResultHandler handler) {
    storeMapEntry(fetchExceptionClass,
		  ResultAction.handler(handler));
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheResultHandler instance */
  public void storeMapEntry(Class fetchExceptionClass,
			    CacheResultHandler handler,
			    String fmt) {
    storeMapEntry(fetchExceptionClass,
		  ResultAction.handler(handler, fmt));
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheResultHandler instance */
  public void storeMapEntry(Class fetchExceptionClass, ResultAction ra) {
    exceptionTable.put(fetchExceptionClass, ra);
  }

  /** Map the fetch exception (SocketException, IOException, etc.) to the
   * CacheResultHandler instance */
  public void storeMapEntry(int code, ResultAction ra) {
    exceptionTable.put(code, ra);
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

  public CacheException triggerAction(ArchivalUnit au,
                                      String url,
                                      CacheEvent evt,
                                      ResultAction ra,
                                      String message)  {
    if (ra.isRemap()) {
      CacheEvent remapEvent = CacheEvent.fromRemapResult(ra, message);
      return checkSuccess(mapException(au, url, remapEvent, message));
    }
    return checkSuccess(ra.getCacheException(au, url, evt));
  }

  public CacheException mapException(ArchivalUnit au,
				     String url,
				     int responseCode,
				     String message)  {

    return mapException(au, url,
                        new CacheEvent.ResponseEvent(responseCode, message),
                        message);
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

    if (fetchException instanceof CacheException) {
      return (CacheException)fetchException;
    }
    return mapException(au, url,
                        new CacheEvent.ExceptionEvent(fetchException, message),
                        message);
  }

  public CacheException mapException(ArchivalUnit au,
				     String url,
                                     CacheEvent evt,
				     String message)  {

    ResultAction ra = evt.lookupIn(exceptionTable);

    if (ra == null) {
      return evt.makeUnknownException(message);
    }
    return checkSuccess(ra.getCacheException(au, url, evt));
  }

  private CacheException checkSuccess(CacheException ex) {
    // Instance of marker class means success
    return (ex instanceof CacheSuccess) ? null : ex;
  }

  ResultAction findNearestException(Exception fetchException) {
    Class exClass = fetchException.getClass();
    ResultAction ra;
    do {
      ra = exceptionTable.get(exClass);
    } while (ra == null
	     && ((exClass = exClass.getSuperclass()) != null
		 && exClass != Exception.class));
    log.debug3("nearest to: " + fetchException + " = " + exClass);
    return ra;
  }

  public Map<Object,ResultAction> getExceptionMap() {
    return Collections.unmodifiableMap(exceptionTable);
  }

  static String getConnUrl(LockssUrlConnection conn) {
    return conn != null ? conn.getURL() : null;
  }

  public String toString() {
    return "[HRM: " + exceptionTable + "]";
  }

}
