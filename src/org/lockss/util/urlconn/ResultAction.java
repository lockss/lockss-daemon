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
import java.net.*;

import org.lockss.util.*;
import org.lockss.daemon.PluginException;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ContentValidationException;

/** Value in exceptionTable map, specified by plugin: either the class of
 * an exception to throw or a CacheResultHandler instance.
 */
public abstract class ResultAction {
  static Logger log = Logger.getLogger("ResultAction");

    String fmt;

    public enum Type {Class, Handler};

    ResultAction(String fmt) {
      this.fmt = fmt;
    }

    ResultAction() {
    }

    public abstract Type getType();
    public abstract Class getExceptionClass();
    public abstract CacheResultHandler getHandler();

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
	  CacheException.UnknownCodeException("Unable to make exception:"
					      + ex.getMessage());
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
          return handler.equals(oh.handler);
        }
        return false;
      }

      public String toString() {
	return "[EIH: " + handler + "]";
      }
    }

    /** Wraps a CacheException class */
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
      @Override
      public CacheResultHandler getHandler() {
        throw new UnsupportedOperationException();
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
	return "[EIC: " + ex + "]";
      }
    }
  }

