/*
 * $Id: WrapperUtil.java,v 1.3 2007-09-24 18:37:12 dshr Exp $
 */

/*

Copyright (c) 2000-2006 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.wrapper;
import java.util.*;
import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.plugin.definable.*;

/** Utilities to wrap plugin classes in error-catching proxy classes */
public class WrapperUtil {
  static final Logger log = Logger.getLogger("WrapperUtil");

  private static Map wrapperFactories = new HashMap();

  static {
    registerFactories();
  }

  /** Register factories for all interfaces that should be wrapped. */
  static void registerFactories() {
    registerWrapperFactory(FilterFactory.class,
			   new FilterFactoryWrapper.Factory());
    registerWrapperFactory(FilterRule.class,
			   new FilterRuleWrapper.Factory());
    registerWrapperFactory(UrlNormalizer.class,
			   new UrlNormalizerWrapper.Factory());
    registerWrapperFactory(org.lockss.extractor.LinkExtractor.class,
			   new LinkExtractorWrapper.Factory());
    registerWrapperFactory(org.lockss.extractor.LinkExtractorFactory.class,
			   new LinkExtractorFactoryWrapper.Factory());
    registerWrapperFactory(LoginPageChecker.class,
			   new LoginPageCheckerWrapper.Factory());
    registerWrapperFactory(PermissionCheckerFactory.class,
			   new PermissionCheckerFactoryWrapper.Factory());
    registerWrapperFactory(DefinableArchivalUnit.ConfigurableCrawlWindow.class,
			   new ConfigurableCrawlWindowWrapper.Factory());
    registerWrapperFactory(org.lockss.util.urlconn.CacheResultHandler.class,
			   new CacheResultHandlerWrapper.Factory());
    registerWrapperFactory(ExploderHelper.class,
			   new ExploderHelperWrapper.Factory());
  }

  /** Register a wrapper factory for instances of the interface */
  static void registerWrapperFactory(Class inter, WrapperFactory fact) {
    wrapperFactories.put(inter, fact);
    log.debug2("Registered " + fact.getClass().getName());
  }  

  /** Wrap the object using the wrapper factory registered for inter */
  public static Object wrap(Object obj, Class inter) {
    WrapperFactory fact = (WrapperFactory)wrapperFactories.get(inter);
    if (fact == null) {
      log.warning("No wrapper for " + obj.getClass().getName());
      return obj;
    }
    Object wrapped = fact.wrap(obj);
    log.debug2("Wrapped " + obj + " in " + wrapped);
    return wrapped;
  }

  /** For tests */
  static Map getWrapperFactories() {
    return new HashMap(wrapperFactories);
  }  

  /** Return the wrapped object, for testing */
  public static Object unwrap(Object wrappedObj) {
    if (wrappedObj instanceof PluginCodeWrapper) {
      return ((PluginCodeWrapper)wrappedObj).getWrappedObj();
    } else {
      return wrappedObj;
    }
  }
}
