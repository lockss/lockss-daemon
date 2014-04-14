/*
 * $Id: LockssOutputPlugin.java,v 1.1 2014-04-14 23:08:24 clairegriffin Exp $
 */

/*

Copyright (c) 2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.crawljax;

import com.crawljax.core.CrawlSession;
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.plugin.PreCrawlingPlugin;
import com.google.common.base.Preconditions;
import org.owasp.webscarab.model.Request;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * An OutputPlugin for use with process a file of links which are
 */
public class LockssOutputPlugin implements PostCrawlingPlugin,
                                             PreCrawlingPlugin {

  File m_outputDir;
  LockssWebScarabProxyAddon m_webScarabProxyAddon;

  public LockssOutputPlugin(final LockssWebScarabProxyAddon proxyAddon) {

    m_webScarabProxyAddon = proxyAddon;
  }


  @Override
  public void preCrawling(final CrawljaxConfiguration config)
  throws RuntimeException {
    m_outputDir = config.getOutputDir();
    Preconditions.checkNotNull(m_outputDir,
                               "Output folder cannot be null");
  }


  @Override
  public void postCrawling(final CrawlSession crawlSession,
                           final ExitStatus exitStatus) {

    try {
      m_webScarabProxyAddon.writeIndex();
    } catch (IOException e) {
      System.err.println("Attempt to write index file failed!: "
                            + e.getMessage());
    }
    m_webScarabProxyAddon.clearCache();
  }

}
