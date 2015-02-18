/*
 * $Id$
 */

/*

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

package org.lockss.util;

import org.lockss.config.*;
import org.lockss.plugin.CachedUrl;
import org.mortbay.util.*;

/**
 * @author  Thomas S. Robertson
 * @version 0.0
 */
public class PluginUtil {
  static Logger log = Logger.getLogger("PluginUtil");

  static final String PREFIX = Configuration.PREFIX + "PluginUtil.";

  /** If true, the logic in getBaseUrl() that is responsible for turning
   * the url <code>foo</code> into the base url <code>foo/</code> (if that
   * is the name the page was actually collected under) will do that only
   * if the url doesn't end with slash, and the nodeUrl in the props does.
   * This matches the logic in
   * LockssResourceHandler.handleLockssRedirect().  But it's expensive, and
   * I don't think the nodeUrl prop should ever be different from the url
   * in any other situation, so I don't think it's necessary.
   */
  public static final String PARAM_DIR_NODE_CHECK_SLASH =
    PREFIX + "dirNodeCheckSlash";
  public static final boolean DEFAULT_DIR_NODE_CHECK_SLASH = false;

  private static boolean dirNodeCheckSlash = DEFAULT_DIR_NODE_CHECK_SLASH;

  /** Called by org.lockss.config.MiscConfig
   */
  public static void setConfig(Configuration config,
			       Configuration oldConfig,
			       Configuration.Differences diffs) {
    if (diffs.contains(PREFIX)) {
      dirNodeCheckSlash = config.getBoolean(PARAM_DIR_NODE_CHECK_SLASH,
					    DEFAULT_DIR_NODE_CHECK_SLASH);
    }
  }


  /**
   * Returns the base url of the provided CachedUrl, checking to see if
   * it's the result of a redirect.  
   */
  public static String getBaseUrl(CachedUrl cu) {
    // See the comments in LockssResourceHandler.handleLockssRedirect();
    // this is the same logic.
    CIProperties props = cu.getProperties();
    if (props != null) {
      String redir = props.getProperty(CachedUrl.PROPERTY_CONTENT_URL);
      if (redir != null) {
	return redir;
      } else {
	String url = cu.getUrl();
	String nodeUrl = props.getProperty(CachedUrl.PROPERTY_NODE_URL);
	if (nodeUrl != null && !nodeUrl.equals(url)) {
	  log.debug2("getBaseUrl(" + url + "), nodeUrl: " + nodeUrl);
	  if (dirNodeCheckSlash) {
	    URI uri = new URI(url);
	    if (!uri.getPath().endsWith("/")) {
	      URI nodeUri = new URI(nodeUrl);
	      if (nodeUri.getPath().endsWith("/")) {
		return nodeUrl;
	      }
	    }
	  } else {
	    return nodeUrl;
	  }
	}
      }
    }
    return cu.getUrl();
  }
}
