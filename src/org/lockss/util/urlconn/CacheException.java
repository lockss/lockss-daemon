/*
 * $Id: CacheException.java,v 1.1 2004-02-23 09:21:22 tlipkis Exp $
 *

Copyright (c) 2000-2003 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import org.lockss.plugin.*;

/** Hierarchy of exceptions that may be returned from a plugin's {@link
 * org.lockss.plugin.UrlCacher#cache()} method, or its componenet methods
 * (<i>eg</i> {@link org.lockss.plugin.UrlCacher#getUncachedInputStream()}.
 * It is the plugin's responsibility to map all possible fetch or store
 * errors into one of these categories, so that the generic crawler can
 * handle the error in a standardized way. */
public class CacheException extends IOException {

  protected String message;
  protected Exception nestedException = null;

  // Most of these exceptions are created at a known place (in
  // HttpResultMap) and there is no point in polluting logs with stack
  // traces.
  protected boolean suppressStackTrace = true;

  public CacheException() {
    super();
  }

  public CacheException(String message) {
    super(message);
    this.message = message;
  }

  void initMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  /** Return the nested (causal) exception, if any. */
  public Exception getNestedException() {
    return nestedException;
  }

  /** If stack trace is suppressed, substitute the stack trace of the
   * nested exception, if any.  Should be cleaned up, but achieves the
   * desired effect for now. */
  public void printStackTrace() {
    if (!suppressStackTrace) {
      super.printStackTrace();
    } else if (nestedException != null) {
      nestedException.printStackTrace();
    }
  }

  /** If stack trace is suppressed, substitute the stack trace of the
   * nested exception, if any.  Should be cleaned up, but achieves the
   * desired effect for now. */
  public void printStackTrace(java.io.PrintStream s) { 
    if (!suppressStackTrace) {
      super.printStackTrace(s);
    } else if (nestedException != null) {
      nestedException.printStackTrace(s);
    }
  }

  /** If stack trace is suppressed, substitute the stack trace of the
   * nested exception, if any.  Should be cleaned up, but achieves the
   * desired effect for now. */
  public void printStackTrace(java.io.PrintWriter s) { 
    if (!suppressStackTrace) {
      super.printStackTrace(s);
    } else if (nestedException != null) {
      nestedException.printStackTrace(s);
    }
  }

  /** Unknown response code */
  public static class UnknownCodeException
      extends CacheException {

    public UnknownCodeException() {
      super();
    }

    public UnknownCodeException(String message) {
      super(message);
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

  /** An error that should be retried with a different URL (in the
   * Location: response header), <i>ie</i>, a redirect */
  public static class RetryNewUrlException
      extends RetryableException {
    public RetryNewUrlException() {
      super();
    }

    public RetryNewUrlException(String message) {
      super(message);
    }
  }

  /** Permenent redirect */
  public static class RetryPermUrlException
      extends RetryNewUrlException {
    public RetryPermUrlException() {
      super();
    }

    public RetryPermUrlException(String message) {
      super(message);
    }
  }

  /** Temporary redirect */
  public static class RetryTempUrlException
      extends RetryNewUrlException {
    public RetryTempUrlException() {
      super();
    }

    public RetryTempUrlException(String message) {
      super(message);
    }
  }

  /** An error that is likely permenent and not likely to succeed if
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
  }

  /** An error connecting to the host serving the URL.  (<i>Eg</i>, host
   * not found, down, connection refused) */
  public static class HostException
      extends UnretryableException {
    public HostException() {
      super();
      suppressStackTrace = false;
    }

    public HostException(String message) {
      super(message);
      suppressStackTrace = false;
    }

    /** Create this if details of causal exception are more relevant. */
    public HostException(Exception e) {
      super(e.toString());
      nestedException = e;
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
      nestedException = e;
    }
  }

  /** Unretryable errors that are expectd to happen in normal operation and
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

  /** Unretryable errors that are not expected to happen in normal
   * operation and might warrant a message or alert. */
  public static class UnexpectedNoRetryException
      extends UnretryableException {
    public UnexpectedNoRetryException() {
      super();
    }

    public UnexpectedNoRetryException(String message) {
      super(message);
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
  }
}
