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

import org.lockss.daemon.*;
import org.lockss.plugin.*;

/** Records type and details of an (usually anomalous) event that
 * occurs while attempting to cache (fetch and store) an artifact.  */
public abstract class CacheEvent {
  /** The type of event */
  public enum EventType {RESPONSE_CODE, EXCEPTION, REDIRECT_TO_URL};

  protected String message;

  protected CacheEvent(String message) {
    this.message = message;
  }

  /** Store the exception causing the event.  Only implemented for
   * EXCEPTION type events */
  public void storeCauseIn(Exception e) {
  }

  /** Return the type of event */
  public abstract EventType getEventType();

  /** Return a human readable explanation of the event */
  public String getMessage() {
    return message;
  }

  /** Return string for either response code or Exception */
  public abstract String getResultString();

  /** Invoke the handler on the result (responseCode or Exception) */
  @Deprecated
  public CacheException invokeHandler(CacheResultHandler handler,
                                      ArchivalUnit au,
                                      LockssUrlConnection conn)
      throws PluginException {
    return invokeHandler(handler, au, (conn != null ? conn.getURL() : null));
  }
    
  /** Invoke the handler on the result (responseCode or Exception) */
  public abstract CacheException invokeHandler(CacheResultHandler handler,
                                               ArchivalUnit au,
                                               String url)
      throws PluginException;


  /**  An Event representing an HTTP response received as a result of
   *  a request */
  public static class ResponseEvent extends CacheEvent {
    int responseCode;

    public ResponseEvent(int responseCode, String message) {
      super(message);
      this.responseCode = responseCode;
    }

    @Override
    public EventType getEventType() {
      return EventType.RESPONSE_CODE;
    }

    @Override
    public String getResultString() {
      return "Response status " + Integer.toString(responseCode);
    }
    
    @Override
    public CacheException invokeHandler(CacheResultHandler handler,
				 ArchivalUnit au,
				 String url)
	throws PluginException {
      return handler.handleResult(au, url, responseCode);
    }
  }

  /** An Event representing an Exception thrown attempting an HTTP
   * request or reading or storing the response.  The exception may be:<ul>
   *
   * <li>An IOException thrown while opening a socket (e.g.,
   * UnknownHostException, SocketException)</li>
   *
   * <li>An IOException thrown while reading data from a socket (e.g.,
   * LockssUrlConnection.ConnectionTimeoutException,
   * SocketTimeoutException)</li>
   *
   * <li>a CacheException.RepositoryException wrapping an exception thrown
   * while storing data in the repository</li>
   *
   * <li>a ContentValidationException throws by DefaultUrlCacher or a
   * plugin FileValidator</li>
   *
   * </ul>
   *
   */
  public static class ExceptionEvent extends CacheEvent {
    Exception fetchException;

    ExceptionEvent(Exception fetchException, String message) {
      super(message);
      this.fetchException = fetchException;
    }

    @Override
    public EventType getEventType() {
      return EventType.EXCEPTION;
    }

    public void storeCauseIn(Exception e) {
      e.initCause(fetchException);
    }

    public String getResultString() {
      return fetchException.getMessage();
    }
    
    public CacheException invokeHandler(CacheResultHandler handler,
                                        ArchivalUnit au,
                                        String url)
	throws PluginException {
      return handler.handleResult(au, url, fetchException);
    }
  }

  /**  An Event representing a redirect to a URL that the plugin
   *  stated interest in handling specially. */
  public static class RedirectEvent extends CacheEvent {
    String redirToUrl;

    RedirectEvent(String redirToUrl, String message) {
      super(message);
      this.redirToUrl = redirToUrl;
    }

    @Override
    public EventType getEventType() {
      return EventType.REDIRECT_TO_URL;
    }

    public String getResultString() {
//       return "Redirect to: " + redirToUrl;
      return redirToUrl;
    }

    @Override
    public CacheException invokeHandler(CacheResultHandler handler,
				 ArchivalUnit au,
				 String url)
	throws PluginException {
      return handler.handleRedirect(au, url, redirToUrl);
    }
  }
}
