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
 * Maps a URL pattern to HTTP result (response code or exception
 * thrown during processing) to an action represented as a
 * CacheException.
 */
public class AuHttpResultMap implements AuCacheResultMap {
  static Logger log = Logger.getLogger("AuHttpResultMap");

  public static final AuHttpResultMap DEFAULT =
    new AuHttpResultMap(new HttpResultMap(), PatternObjectMap.EMPTY);

  PatternObjectMap urlMap;
  CacheResultMap resultMap;

  public AuHttpResultMap(CacheResultMap resultMap, PatternObjectMap urlMap) {
    this.resultMap = resultMap;
    this.urlMap = urlMap;
  }

  // map url.  If result is
  //  int: map through HttpResultMap.mapException()
  //  class name of CacheException or HttpReaultHandler:
  //    use ExceptionInfo.makeException() to instantiate or call handler
  //  else: assume is name of exception to map through HttpReaultMap.
  //    instantiate and call mapException()
  
  public CacheException mapUrl(ArchivalUnit au,
                               LockssUrlConnection connection,
                               String url,
                               String message) {
    log.critical("mapping: " + url);
    Object rhs = urlMap.getMatch(url);
    if (rhs == null) {
      return null;
    }
    log.critical("rhs: " + rhs);
    CacheException c_ex = null;
    if (rhs instanceof Integer) {
      return resultMap.mapException(au, url, (int)rhs, message);
    } else {
      try {
        // See if it's a legal rhs action
        HttpResultMap.ExceptionInfo ei =
          HttpResultMap.ExceptionInfo.fromActionSpec((Class)rhs);
        // Yes, return its result (newInstance or run handler)
        CacheEvent evt = new CacheEvent.RedirectEvent(url, message);
        return ei.makeException(au, connection, evt);
      } catch (Exception e) {
        log.warning("Couldn't interpret rhs as an HttpResultMap action", e);
      }
      log.critical("Assuming urlMap rhs the name of a class meant to be mapped by HttpResultMap: " + rhs);
      if (rhs instanceof Class &&
          Exception.class.isAssignableFrom((Class)rhs)) {
        try {
          Exception ex = instantiateException(((Class)rhs), message);
          return resultMap.mapException(au, url, ex, message);
        } catch (Exception e) {
          log.error("Couldn't instantiate urlMap rhs: " + rhs, e);
          throw new IllegalArgumentException("Couldn't instantiate urlMap rhs: "
                                             + rhs);
        }
      }
    }    
    throw new IllegalArgumentException("Unhandled urlMap rhs: "
                                       + rhs);
  }

  Exception instantiateException(Class exclass, String message)
      throws Exception{
    Class[] sig = { String.class };
    Object[] args = {message};
    Constructor cons = exclass .getConstructor(sig);
    return (Exception)cons.newInstance(args);
  }

  public String toString() {
    return "[AuHRM: " + urlMap + "]";
  }
}
