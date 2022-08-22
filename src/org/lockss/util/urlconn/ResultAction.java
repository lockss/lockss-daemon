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

import java.util.*;
import java.lang.reflect.Constructor;
import java.net.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ContentValidationException;
import org.lockss.plugin.wrapper.*;

/** Value in exceptionTable map, specified by plugin: either the class of
 * an exception to throw or a CacheResultHandler instance.
 */
public abstract class ResultAction {
  static Logger log = Logger.getLogger("ResultAction");

  String fmt;

  public enum Type {Class, Handler, ReMap};

  ResultAction(String fmt) {
    this.fmt = fmt;
  }

  ResultAction() {
  }

  public abstract Type getType();

  public boolean isReMap() {
    return false;
  }

  public Object getRemapVal(String message) {
    throw new UnsupportedOperationException("Attempt to get remapVal from non Remp action: " + this);
  }

  public static ResultAction handler(CacheResultHandler handler) {
    return new ResultAction.Handler(handler);
  }

  public static ResultAction handler(CacheResultHandler handler, String fmt) {
    return new ResultAction.Handler(handler, fmt);
  }

  public static ResultAction exClass(Class cls) {
    return new ResultAction.Cls(cls);
  }

  public static ResultAction exClass(Class cls, String fmt) {
    return new ResultAction.Cls(cls, fmt);
  }

  public static ResultAction remap(Object obj) {
    return new ResultAction.ReMap(obj);
  }

  public static ResultAction fromActionSpec(Object spec)
      throws IllegalArgumentException {
    if (spec instanceof Class) {
      if (CacheResultHandler.class.isAssignableFrom((Class)spec)) {
        return new ResultAction.Handler((CacheResultHandler)spec);
      } else if (CacheException.class.isAssignableFrom((Class)spec)) {
        return new ResultAction.Cls((Class)spec);
      }
    }
    throw new IllegalArgumentException("Action spec must be a CacheException class or a CacheResultHandler class: " + spec);
  }

  /** Return the CacheException appropriate for the event, either from
   * the specified class or CacheResultHandler */
  @Deprecated
  abstract CacheException makeException(ArchivalUnit au,
                                        LockssUrlConnection connection,
                                        CacheEvent evt)
      throws Exception;

  abstract CacheException makeException(ArchivalUnit au,
                                        String url,
                                        CacheEvent evt)
      throws Exception;

  public Class getExceptionClass() {
    throw new UnsupportedOperationException();
  }

  public CacheResultHandler getHandler() {
    throw new UnsupportedOperationException();
  }

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
                                   CacheEvent evt) {
    return getCacheException(au, HttpResultMap.getConnUrl(connection), evt);
  }

  /** Return the exception to throw for this event, or an
   * UnknownCodeException if an error occurs trying to create it */
  CacheException getCacheException(ArchivalUnit au,
                                   String url,
                                   CacheEvent evt) {
    try {
      CacheException cacheException = makeException(au, url, evt);
      return cacheException;
    } catch (Exception ex) {
      log.error("Can't make CacheException for: " + evt.getResultString(),
                ex);
      return new
        CacheException.UnknownExceptionException("Unable to make exception:"
                                                 + ex.getMessage());
    }
  }

  /** Return a ResultAction for the supplied Object, which may be:<ul>
   * <li>CacheResultHandler instance</li>
   * <li>CacheException instance</li>
   * <li>CacheException class</li>
   * <li>Other -> ReMap action</li>
   * </ul>
   */
  public static ResultAction fromObject(Object response) {
    if (response instanceof CacheResultHandler) {
      return new Handler((CacheResultHandler)response);
    } else if (response instanceof CacheException) {
      return new Cls(response.getClass());
    } else if (response instanceof Class &&
               CacheException.class.isAssignableFrom((Class)response)) {
      return new Cls((Class)response);
    } else {
      return new ReMap(response);
    }
  }

  /** Wraps a plugin-supplied CacheResultHandler */
  public static class Handler extends ResultAction {
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
    public CacheResultHandler getHandler() {
      return handler;
    }

    @Deprecated
    CacheException makeException(ArchivalUnit au,
                                 LockssUrlConnection connection,
                                 CacheEvent evt)
        throws PluginException {
      return makeException(au, HttpResultMap.getConnUrl(connection), evt);
    }
      
    CacheException makeException(ArchivalUnit au,
                                 String url,
                                 CacheEvent evt)
        throws PluginException {
      return evt.invokeHandler(handler, au, url);
    }

    public boolean equals(Object o) {
      if (o instanceof Handler) {
        Handler oh = (Handler)o;
        return WrapperUtil.unwrap(handler).getClass().equals(WrapperUtil.unwrap(oh.handler).getClass());
      }
      return false;
    }

    public String toString() {
      return "[RAH: " + handler + "]";
    }
  }

  /** Action that throws a CacheException */
  public static class Cls extends ResultAction {
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

    @Deprecated
    CacheException makeException(ArchivalUnit au,
                                 LockssUrlConnection connection,
                                 CacheEvent evt)
        throws Exception {
      return makeException(au, HttpResultMap.getConnUrl(connection), evt);
    }

    CacheException makeException(ArchivalUnit au,
                                 String url,
                                 CacheEvent evt)
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
      return "[RAC: " + ex + "]";
    }
  }

  /** Action that remaps the result */
  public static class ReMap extends ResultAction {
    Object remapVal;

    ReMap(Object remapVal) {
      this.remapVal = remapVal;
    }

    public boolean isReMap() {
      return true;
    }

    public Object getRemapVal(String message) {
      if (remapVal instanceof Integer) {
        return remapVal;
      } else if (remapVal instanceof Class &&
                 Exception.class.isAssignableFrom((Class)remapVal)) {
        Class<Exception> exClass = (Class<Exception>)remapVal;
        Class[] sig = { String.class };
        Object[] args = {message};
        try {
          Constructor cons = exClass.getConstructor(sig);
          return (Exception)cons.newInstance(args);
        } catch (Exception e) {
          throw new UnsupportedOperationException("Unexpeced failure instantiating remap class: " + exClass);
        }
      } else {
        throw new IllegalArgumentException("Remap value neither Integer nor Exception class: " + remapVal);
      }
    }

    @Override
    public Type getType() {
      return Type.ReMap;
    }

    @Deprecated
    CacheException makeException(ArchivalUnit au,
                                 LockssUrlConnection connection,
                                 CacheEvent evt)
        throws Exception {
      throw new UnsupportedOperationException("Can't makeException from ResultAction type ReMap: " + this);
    }

    CacheException makeException(ArchivalUnit au,
                                 String url,
                                 CacheEvent evt)
        throws Exception {
      throw new UnsupportedOperationException("Can't makeException from ResultAction type ReMap: " + this);
    }

    public boolean equals(Object o) {
      if (o instanceof ReMap) {
        ReMap oh = (ReMap)o;
        return remapVal.equals(oh.remapVal);
      }
      return false;
    }

    public String toString() {
      return "[RAR: " + remapVal + "]";
    }
  }
}
