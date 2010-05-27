/*
 * $Id: MiscConfig.java,v 1.6 2010-05-27 07:00:01 tlipkis Exp $
 */

/*

Copyright (c) 2001-2010 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.config;

import org.lockss.daemon.MimeTypeMap;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.servlet.ServletUtil;

/** Miscellaneous config actions.  Convenient for auxilliary components
 * that don't currently have the ability to register themselves.
 */
public class MiscConfig {
  public static Configuration.Callback getConfigCallback() {
    return
      new Configuration.Callback() {
	public void configurationChanged(Configuration config,
					 Configuration oldConfig,
					 Configuration.Differences diffs) {
	  HttpClientUrlConnection.setConfig(config, oldConfig, diffs);
	  UrlUtil.setConfig(config, oldConfig, diffs);
	  PluginUtil.setConfig(config, oldConfig, diffs);
	  ServletUtil.setConfig(config, oldConfig, diffs);
	  MimeTypeMap.setConfig(config, oldConfig, diffs);
	}
      };
  }
}
