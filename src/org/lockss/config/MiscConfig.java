/*
 * $Id$
 */

/*

Copyright (c) 2001-2014 Board of Trustees of Leland Stanford Jr. University,
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

import org.lockss.daemon.*;
import org.lockss.plugin.*;
import org.lockss.util.*;
import org.lockss.util.urlconn.*;
import org.lockss.servlet.*;
import org.lockss.exporter.Exporter;
import org.lockss.exporter.kbart.HtmlKbartExporter;
import org.lockss.rewriter.*;

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
	  StringPool.setConfig(config, oldConfig, diffs);
	  PluginUtil.setConfig(config, oldConfig, diffs);
	  AuUtil.setConfig(config, oldConfig, diffs);
	  ServletUtil.setConfig(config, oldConfig, diffs);
	  ExpertConfig.setConfig(config, oldConfig, diffs);
	  MimeTypeMap.setConfig(config, oldConfig, diffs);
	  MetadataUtil.setConfig(config, oldConfig, diffs);
	  Exporter.setConfig(config, oldConfig, diffs);
	  AuHealthMetric.setConfig(config, oldConfig, diffs);
	  HtmlKbartExporter.setConfig(config, oldConfig, diffs);
	  CreativeCommonsPermissionChecker.setConfig(config, oldConfig, diffs);
	}
      };
  }
}
