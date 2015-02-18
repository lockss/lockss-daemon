/*
 * $Id$
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import org.lockss.util.*;
import org.lockss.daemon.*;
import org.lockss.plugin.*;

/**
 * Default set of functions that may be applied to AU config params in
 * printf arguments.  Plugins may subclass to add functions.
 * <p>
 * Supported functions: <dl>
 * <dt>url_host</dt><dd>Returns the host part of a URL.</dd>
 * <dt>url_path</dt><dd>Returns the path part of a URL.</dd>
 * <dt>add_www</dt><dd>Adds leading <code>www</code> subdomain to host, if
 * not already present.</dd>
 * <dt>del_www</dt><dd>Removes leading <code>www</code> subdomain from
 * host.</dd>
 * <dt>to_https</dt><dd>Replaces <code>http:</code> with
 * <code>https:</code> at start of URL.</dd>
 * <dt>to_http</dt><dd>Replaces <code>https:</code> with
 * <code>http:</code> at start of URL.</dd>
 * <dt>url_encode</dt><dd>URL encodes argument.</dd>
 * <dt>url_decode</dt><dd>URL decodes argument.</dd>
 * </dl>  
 */
public class BaseAuParamFunctor implements AuParamFunctor {
  static Logger log = Logger.getLogger("BaseAuParamFunctor");

  public static final AuParamFunctor SINGLETON = new BaseAuParamFunctor();

  public static final String URL_ENCODE_CHARSET = "UTF-8";


  /** Apply the function named by fn to a single arg */
  public Object apply(AuParamFunctor.FunctorData fd, String fn,
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
      } else if (fn.equals("to_https")) {
	return UrlUtil.replaceScheme((String)arg, "http", "https");
      } else if (fn.equals("to_http")) {
	return UrlUtil.replaceScheme((String)arg, "https", "http");
      } else if (fn.equals("url_encode")) {
	return URLEncoder.encode((String)arg, URL_ENCODE_CHARSET);
      } else if (fn.equals("url_decode")) {
	return URLDecoder.decode((String)arg, URL_ENCODE_CHARSET);
      }
      throw new PluginException.InvalidDefinition("Undefined function: " + fn);
    } catch (ClassCastException e) {
      throw new PluginException.BehaviorException("Illegal arg type", e);
    } catch (MalformedURLException e) {
      throw new PluginException.BehaviorException("Malformed fn arg", e);
    } catch (UnsupportedEncodingException e) {
      throw new PluginException.BehaviorException("Unsupported charset (shouldn't happen)", e);
    }
  }

  static Map<String,AuParamType> fnTypes = new HashMap<String,AuParamType>();
  static {
    for (String x : new String[] {
	"url_host", "url_path",
	"add_www", "del_www",
	"to_http", "to_https",
	"url_encode", "url_decode",
      }) {
      fnTypes.put(x, AuParamType.String);
    }
  };

  /** Return the AuParamType of the value returned by fn.  Return null iff
   * fn is undefined */
  public AuParamType type(AuParamFunctor.FunctorData fd, String fn) {
    return fnTypes.get(fn);
  }

}
