/*
 * $Id$
 */

/*

Copyright (c) 2000-2010 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.*;
import java.util.*;

import org.lockss.util.Constants;
import org.lockss.config.*;
import org.lockss.crawler.BaseCrawler;
import org.lockss.plugin.PluginFetchEventResponse;

/** Hierarchy of exceptions that may be returned from a plugin's {@link
 * org.lockss.plugin.UrlCacher#cache()} method, or its componenet methods
 * (<i>eg</i> {@link org.lockss.plugin.UrlCacher#getUncachedInputStream()}.
 * It is the plugin's responsibility to map all possible fetch or store
 * errors into one of these categories, so that the generic crawler can
 * handle the error in a standardized way. */
public class CacheException
  extends IOException
  implements PluginFetchEventResponse {

  //Exceptions with this attribute will cause the crawl to be marked
  //as a failure
  public static final int ATTRIBUTE_FAIL = 1;

  //Exceptions with this attribute will cause the URL being fetched to be
  //retried a fixed number of times
  public static final int ATTRIBUTE_RETRY = 2;

  //Exceptions with this attribute will signal that we have a serious error
  //such as a permission problem or a site wide issue
  public static final int ATTRIBUTE_FATAL = 3;

  //If an exception with this attribute is thrown in a context where a file
  //would otherwise have been stored, the file won't be stored.  Useful for
  //validators to reject files (possibly after retries, if used with
  //ATTRIBUTE_RETRY) without causing the crawl to fail.
  public static final int ATTRIBUTE_NO_STORE = 4;

  protected static boolean defaultSuppressStackTrace = true;

  protected String message;
  protected BitSet attributeBits = new BitSet();

  // Most of these exceptions are created at a known place (in
  // HttpResultMap) and there is no point in polluting logs with stack
  // traces.
  protected boolean suppressStackTrace = defaultSuppressStackTrace;

  public CacheException() {
    super();
    setAttributes();
  }

  public CacheException(String message) {
    super(message);
    this.message = message;
    setAttributes();
  }

  public static boolean
    setDefaultSuppressStackTrace(boolean defaultSuppress) {
    boolean res = defaultSuppressStackTrace;
    defaultSuppressStackTrace = defaultSuppress;
    return res;
  }

  void initMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public boolean isAttributeSet(int attribute) {
    return attributeBits.get(attribute);
  }

  protected void setAttributes() {

  }

  public long getRetryDelay() {
    return CurrentConfig.getLongParam(BaseCrawler.PARAM_DEFAULT_RETRY_DELAY,
                                      BaseCrawler.DEFAULT_DEFAULT_RETRY_DELAY);
  }

  public int getRetryCount() {
    return CurrentConfig.getIntParam(BaseCrawler.PARAM_DEFAULT_RETRY_COUNT,
                                     BaseCrawler.DEFAULT_DEFAULT_RETRY_COUNT);
  }

  /** If stack trace is suppressed, substitute the stack trace of the
   * nested exception, if any.  Should be cleaned up, but achieves the
   * desired effect for now. */
  public void printStackTrace() {
    if (!suppressStackTrace) {
      super.printStackTrace();
    } else if (getCause() != null) {
      getCause().printStackTrace();
    } else {
      System.err.println(this);
    }
  }

  /** If stack trace is suppressed, substitute the stack trace of the
   * nested exception, if any.  Should be cleaned up, but achieves the
   * desired effect for now. */
  public void printStackTrace(java.io.PrintStream s) {
    if (!suppressStackTrace) {
      super.printStackTrace(s);
    } else if (getCause() != null) {
      getCause().printStackTrace(s);
    } else {
      s.println(this);
    }
  }

  /** If stack trace is suppressed, substitute the stack trace of the
   * nested exception, if any.  Should be cleaned up, but achieves the
   * desired effect for now. */
  public void printStackTrace(java.io.PrintWriter s) {
    if (!suppressStackTrace) {
      super.printStackTrace(s);
    } else if (getCause() != null) {
      getCause().printStackTrace(s);
    } else {
      s.println(this);
    }
  }

  /** Ignore exception on close (return success) */
  public static class IgnoreCloseException extends CacheException {

    public IgnoreCloseException() {
      super();
    }

    public IgnoreCloseException(String message) {
      super(message);
    }
  }

  /** Unknown response code */
  public static class UnknownCodeException extends CacheException {

    public UnknownCodeException() {
      super();
    }

    public UnknownCodeException(String message) {
      super(message);
    }

    protected void setAttributes() {
      attributeBits.set(ATTRIBUTE_FAIL);
    }
  }

  /** Unknown exception exception */
  public static class UnknownExceptionException extends CacheException {

    public UnknownExceptionException() {
      super();
    }

    public UnknownExceptionException(String message) {
      super(message);
    }

    public UnknownExceptionException(Exception e) {
      super(e.toString());
      initCause(e);
    }

    public UnknownExceptionException(String message, Exception e) {
      super(message);
      initCause(e);
    }

    protected void setAttributes() {
      attributeBits.set(ATTRIBUTE_FAIL);
    }
  }

  /** Any error result that should be retried */
  public static class RetryableException
      extends CacheException {
    public RetryableException() {
      super();
    }

    public RetryableException(String message) {
      super(message);
    }

    protected void setAttributes() {
      attributeBits.set(ATTRIBUTE_RETRY);
      attributeBits.set(ATTRIBUTE_FAIL);
    }
  }

  /** An error that is likely permanent and not likely to succeed if
   * retried.
   */
  public static class UnretryableException
      extends CacheException {
    public UnretryableException() {
      super();
    }

    public UnretryableException(String message) {
      super(message);
    }
    protected void setAttributes() {
      attributeBits.clear(ATTRIBUTE_RETRY);
      attributeBits.set(ATTRIBUTE_FAIL);
    }
  }

  /** Retryable network error, with default retry characteristics */
  public static class RetryableNetworkException extends RetryableException {
    public RetryableNetworkException() {
      super();
    }

    public RetryableNetworkException(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException(Exception e) {
      super(e.toString());
      initCause(e);
    }

    public long getRetryDelay() {
      return CurrentConfig.getLongParam(BaseCrawler.PARAM_DEFAULT_NETWORK_RETRY_DELAY,
                                        BaseCrawler.DEFAULT_DEFAULT_NETWORK_RETRY_DELAY);
    }

    public int getRetryCount() {
      return CurrentConfig.getIntParam(BaseCrawler.PARAM_DEFAULT_NETWORK_RETRY_COUNT,
                                       BaseCrawler.DEFAULT_DEFAULT_NETWORK_RETRY_COUNT);
    }
  }

  /** Retryable network error; two tries with default */
  public static class RetryableNetworkException_2 extends RetryableNetworkException {
    public RetryableNetworkException_2() {
      super();
    }

    public RetryableNetworkException_2(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_2(Exception e) {
      super(e.toString());
      initCause(e);
    }

    public int getRetryCount() {
      return 2;
    }
  }

  /** Retryable network error; three tries with default */
  public static class RetryableNetworkException_3 extends RetryableNetworkException {
    public RetryableNetworkException_3() {
      super();
    }

    public RetryableNetworkException_3(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_3(Exception e) {
      super(e.toString());
      initCause(e);
    }

    public int getRetryCount() {
      return 3;
    }
  }

  /** Retryable network error; five tries with default */
  public static class RetryableNetworkException_5 extends RetryableNetworkException {
    public RetryableNetworkException_5() {
      super();
    }

    public RetryableNetworkException_5(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_5(Exception e) {
      super(e.toString());
      initCause(e);
    }

    public int getRetryCount() {
      return 5;
    }
  }

  /** Retryable network error; two tries with 10 second delay */
  public static class RetryableNetworkException_2_10S
    extends RetryableNetworkException_2 {
    public RetryableNetworkException_2_10S() {
      super();
    }

    public RetryableNetworkException_2_10S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_2_10S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 10 * Constants.SECOND;
    }

  }

  /** Retryable network error; two tries with 30 second delay */
  public static class RetryableNetworkException_2_30S
    extends RetryableNetworkException_2 {
    public RetryableNetworkException_2_30S() {
      super();
    }

    public RetryableNetworkException_2_30S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_2_30S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 30 * Constants.SECOND;
    }

  }

  /** Retryable network error; two tries with 60 second delay */
  public static class RetryableNetworkException_2_60S
    extends RetryableNetworkException_2 {
    public RetryableNetworkException_2_60S() {
      super();
    }

    public RetryableNetworkException_2_60S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_2_60S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 60 * Constants.SECOND;
    }

  }

  /** Retryable network error; two tries with 5 minute delay */
  public static class RetryableNetworkException_2_5M
    extends RetryableNetworkException_2 {
    public RetryableNetworkException_2_5M() {
      super();
    }

    public RetryableNetworkException_2_5M(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_2_5M(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 5 * Constants.MINUTE;
    }

  }

  /** Retryable network error; three tries with 10 second delay */
  public static class RetryableNetworkException_3_10S
    extends RetryableNetworkException_3 {
    public RetryableNetworkException_3_10S() {
      super();
    }

    public RetryableNetworkException_3_10S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_3_10S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 10 * Constants.SECOND;
    }

  }

  /** Retryable network error; three tries with 30 second delay */
  public static class RetryableNetworkException_3_30S
    extends RetryableNetworkException_3 {
    public RetryableNetworkException_3_30S() {
      super();
    }

    public RetryableNetworkException_3_30S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_3_30S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 30 * Constants.SECOND;
    }

  }

  /** Retryable network error; three tries with 60 second delay */
  public static class RetryableNetworkException_3_60S
    extends RetryableNetworkException_3 {
    public RetryableNetworkException_3_60S() {
      super();
    }

    public RetryableNetworkException_3_60S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_3_60S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 60 * Constants.SECOND;
    }

  }

  /** Retryable network error; three tries with 5 minute delay */
  public static class RetryableNetworkException_3_5M
    extends RetryableNetworkException_3 {
    public RetryableNetworkException_3_5M() {
      super();
    }

    public RetryableNetworkException_3_5M(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_3_5M(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 5 * Constants.MINUTE;
    }

  }

  /** Retryable network error; five tries with 10 second delay */
  public static class RetryableNetworkException_5_10S
    extends RetryableNetworkException_5 {
    public RetryableNetworkException_5_10S() {
      super();
    }

    public RetryableNetworkException_5_10S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_5_10S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 10 * Constants.SECOND;
    }

  }

  /** Retryable network error; five tries with 30 second delay */
  public static class RetryableNetworkException_5_30S
    extends RetryableNetworkException_5 {
    public RetryableNetworkException_5_30S() {
      super();
    }

    public RetryableNetworkException_5_30S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_5_30S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 30 * Constants.SECOND;
    }

  }

  /** Retryable network error; five tries with 60 second delay */
  public static class RetryableNetworkException_5_60S
    extends RetryableNetworkException_5 {
    public RetryableNetworkException_5_60S() {
      super();
    }

    public RetryableNetworkException_5_60S(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_5_60S(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 60 * Constants.SECOND;
    }

  }

  /** Retryable network error; five tries with 5 minute delay */
  public static class RetryableNetworkException_5_5M
    extends RetryableNetworkException_5 {
    public RetryableNetworkException_5_5M() {
      super();
    }

    public RetryableNetworkException_5_5M(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public RetryableNetworkException_5_5M(Exception e) {
      super(e);
    }

    public long getRetryDelay() {
      return 5 * Constants.MINUTE;
    }

  }



  /** An error that should be retried with the same URL */
  public static class RetrySameUrlException
      extends RetryableException {
    public RetrySameUrlException() {
      super();
    }

    public RetrySameUrlException(String message) {
      super(message);
    }

  }

  public static class RetryDeadLinkException extends RetryableException {
    public RetryDeadLinkException() {
      super();
    }

    public RetryDeadLinkException(String message) {
      super(message);
    }

    protected void setAttributes() {
      super.setAttributes();
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }


  public static class PermissionException extends UnretryableException {
    public PermissionException() {
      super();
    }

    public PermissionException(String message) {
      super(message);
    }

    protected void setAttributes() {
      super.setAttributes();
      attributeBits.set(ATTRIBUTE_FATAL);
    }
  }

  /** An error esploding a archive file during a crawl */
  public static class ExploderException
      extends UnretryableException {
    public ExploderException() {
      super();
      suppressStackTrace = false;
    }

    public ExploderException(String message) {
      super(message);
      suppressStackTrace = false;
    }

    public ExploderException(Exception e) {
      super(e.toString());
      initCause(e);
    }

    public ExploderException(String message, Exception e) {
      super(message);
      initCause(e);
    }
  }

  /** An error from trying to connect to a malformed URL*/
  public static class MalformedURLException
      extends UnretryableException {
    public MalformedURLException() {
      super();
    }

    public MalformedURLException(String message) {
      super(message);
    }

    /** Create this if details of causal exception are more relevant. */
    public MalformedURLException(Exception e) {
      super(e.toString());
      initCause(e);
    }

    protected void setAttributes() {
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }

  /** An error extracting URLs from a collected file.  No failure. */
  public static class ExtractionError extends UnretryableException {
    public ExtractionError() {
      super();
    }

    public ExtractionError(String message) {
      super(message);
    }

    public ExtractionError(Exception e) {
      super(e.toString());
      initCause(e);
    }

    public ExtractionError(String message, Exception e) {
      super(message);
      initCause(e);
    }

    protected void setAttributes() {
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }

  public static class RedirectOutsideCrawlSpecException
      extends UnretryableException {
    public RedirectOutsideCrawlSpecException() {
      super();
    }

    public RedirectOutsideCrawlSpecException(String message) {
      super(message);
    }

    protected void setAttributes() {
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }

  /** An error storing the cached content or properties in the repository */
  public static class RepositoryException
      extends UnretryableException {
    public RepositoryException() {
      super();
      suppressStackTrace = false;
    }

    public RepositoryException(String message) {
      super(message);
      suppressStackTrace = false;
    }

    /** Create this if details of causal exception are more relevant. */
    public RepositoryException(Exception e) {
      super(e.toString());
      initCause(e);
    }
  }

  /** An error that should be retried with a different URL (in the
   * Location: response header), <i>ie</i>, a redirect */
  public static class NoRetryNewUrlException
      extends UnretryableException {
    public NoRetryNewUrlException() {
      super();
    }

    public NoRetryNewUrlException(String message) {
      super(message);
    }

    protected void setAttributes() {
      attributeBits.clear(ATTRIBUTE_RETRY);
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }

  /** Permanent redirect */
  public static class NoRetryPermUrlException
      extends NoRetryNewUrlException {
    public NoRetryPermUrlException() {
      super();
    }

    public NoRetryPermUrlException(String message) {
      super(message);
    }
  }

  /** Temporary redirect */
  public static class NoRetryTempUrlException
      extends NoRetryNewUrlException {
    public NoRetryTempUrlException() {
      super();
    }

    public NoRetryTempUrlException(String message) {
      super(message);
    }
  }

  /** Unretryable errors that are expected to happen in normal operation and
   * don't necessarily indicate anything is wrong. */
  public static class ExpectedNoRetryException
      extends UnretryableException {
    public ExpectedNoRetryException() {
      super();
    }

    public ExpectedNoRetryException(String message) {
      super(message);
    }
  }

  public static class NoRetryDeadLinkException
      extends ExpectedNoRetryException {
    public NoRetryDeadLinkException() {
      super();
    }

    public NoRetryDeadLinkException(String message) {
      super(message);
    }

    protected void setAttributes() {
      attributeBits.clear(ATTRIBUTE_RETRY);
      attributeBits.clear(ATTRIBUTE_FAIL);
    }

  }
  /** Unretryable errors that are not expected to happen in normal
   * operation and might warrant a message or alert. */
  public static class UnexpectedNoRetryFailException
      extends UnretryableException {
    public UnexpectedNoRetryFailException() {
      super();
    }

    public UnexpectedNoRetryFailException(String message) {
      super(message);
    }
  }

  public static class UnexpectedNoRetryNoFailException
      extends UnretryableException {
    public UnexpectedNoRetryNoFailException() {
      super();
    }

    public UnexpectedNoRetryNoFailException(String message) {
      super(message);
    }

    protected void setAtrributes() {
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }

  public static class NoRetryHostException
      extends UnretryableException {
    public NoRetryHostException() {
      super();
    }

    public NoRetryHostException(String message) {
      super(message);
    }
  }

  public static class NoRetryRepositoryException
      extends UnretryableException {
    public NoRetryRepositoryException() {
      super();
    }

    public NoRetryRepositoryException(String message) {
      super(message);
    }
  }

  public static class UnimplementedCodeException
      extends ExpectedNoRetryException {
    public UnimplementedCodeException() {
      super();
    }

    public UnimplementedCodeException(String message) {
      super(message);
    }

    protected void setAttributes() {
      attributeBits.clear(ATTRIBUTE_RETRY);
      attributeBits.clear(ATTRIBUTE_FAIL);
    }
  }

  /** Record a warning but otherwise proceed normally.  Use only in
   * contexts where "proceed normally" makes sense, such as
   * DefaultUrlCacher.validate() */
  public static class WarningOnly
      extends CacheException {
    public WarningOnly() {
      super();
    }

    public WarningOnly(String message) {
      super(message);
    }
  }

  /** Do not store the received file, record a warning and proceed.  Makes
   * sense only in a ContentValidator, as that's the only place it's possible to prevent the file from being stored. */
  public static class NoStoreWarningOnly
      extends CacheException {
    public NoStoreWarningOnly() {
      super();
    }

    public NoStoreWarningOnly(String message) {
      super(message);
    }

    protected void setAttributes() {
      attributeBits.set(ATTRIBUTE_NO_STORE);
    }
  }
}
