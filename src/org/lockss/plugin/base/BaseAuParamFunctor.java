/*
 * $Id: BaseAuParamFunctor.java,v 1.1 2014-08-25 08:57:03 tlipkis Exp $
 */

/*

Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.base;

import java.net.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Functions that may be applied to AU config params in printf arguments.
 */
public class BaseAuParamFunctor implements AuParamFunctor {
  static Logger log = Logger.getLogger("BaseAuParamFunctor");

  public static AuParamFunctor SINGLETON = new BaseAuParamFunctor();

  /** Evaluate the function named by fn, with single arg */
  public Object eval(AuParamFunctor.FunctorData fd, String fn,
		     Object arg, AuParamType type)
      throws PluginException {
    try {
      if (fn.equals("url_host")) {
	return UrlUtil.getHost((String)arg);
      } else if (fn.equals("url_path")) {
	return UrlUtil.getPath((String)arg);
      } else if (fn.equals("add_www")) {
	return UrlUtil.addSubDomain((String)arg, "www");
      } else if (fn.equals("del_www")) {
	return UrlUtil.delSubDomain((String)arg, "www");
      }
      throw new PluginException.InvalidDefinition("Undefined function: " + fn);
    } catch (ClassCastException e) {
      throw new PluginException.BehaviorException("Illegal arg type", e);
    } catch (MalformedURLException e) {
      throw new PluginException.BehaviorException("Malformed fn arg", e);
    }
  }

  /** Return the BaseAuParamType named by fn, with array of args */
  public AuParamType type(AuParamFunctor.FunctorData fd, String fn) {
    return AuParamType.String;
  }

}
