/*
 * $Id: BaseFuncExporter.java,v 1.1.2.1 2010-02-22 06:42:03 tlipkis Exp $
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

package org.lockss.exporter;

import java.io.*;
import java.util.*;

import org.archive.io.*;
import org.archive.io.arc.*;
import org.lockss.test.*;
import org.lockss.daemon.*;
import org.lockss.config.*;
import org.lockss.util.*;
import org.lockss.plugin.*;
import org.lockss.plugin.simulated.*;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.crawler.NewContentCrawler;
import org.lockss.protocol.*;

public abstract class BaseFuncExporter extends LockssTestCase {
  protected static final int DEFAULT_FILESIZE = 3000;

  protected MockLockssDaemon daemon;
  protected SimulatedArchivalUnit sau;
  protected int numAuFiles = 0;

  protected File exportDir;
  protected File[] exportFiles = null;
  protected int exportFileIx;


  protected int fileSize = DEFAULT_FILESIZE;

  public void setUp() throws Exception {
    super.setUp();
    daemon = getMockLockssDaemon();

    String tempDirPath = getTempDir().getAbsolutePath() + File.separator;
    String auId = "org|lockss|plugin|simulated|SimulatedPlugin.root~" +
      PropKeyEncoder.encode(tempDirPath);
    String pref = "org.lockss.au." + auId + ".";

    Properties props = new Properties();
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST, tempDirPath);
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(pref + SimulatedPlugin.AU_PARAM_ROOT, tempDirPath);
    props.setProperty(pref + SimulatedPlugin.AU_PARAM_DEPTH, "2");
    props.setProperty(pref + SimulatedPlugin.AU_PARAM_BRANCH, "2");
    props.setProperty(pref + SimulatedPlugin.AU_PARAM_NUM_FILES, "3");
    props.setProperty(pref + SimulatedPlugin.AU_PARAM_FILE_TYPES,
                      ""+SimulatedContentGenerator.FILE_TYPE_BIN);
//     props.setProperty(pref + SimulatedPlugin.AU_PARAM_BIN_FILE_SIZE,
// 		      ""+fileSize);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    daemon.getPluginManager();
    daemon.setDaemonInited(true);
    daemon.getPluginManager().startService();
    daemon.getPluginManager().startLoadablePlugins();

    sau =
        (SimulatedArchivalUnit)daemon.getPluginManager().getAllAus().get(0);
    daemon.getLockssRepository(sau).startService();
    daemon.setNodeManager(new MockNodeManager(), sau);

    sau.generateContentTree();

    crawlContent();
  }

  public void tearDown() throws Exception {
    daemon.stopDaemon();
    super.tearDown();
  }

  protected void crawlContent() {
    CrawlSpec spec = new SpiderCrawlSpec(sau.getNewContentCrawlUrls(), null);
    Crawler crawler = new NewContentCrawler(sau, spec, new MockAuState());
    crawler.doCrawl();
    numAuFiles = 0;
    Iterator iter = sau.getAuCachedUrlSet().contentHashIterator();
    while (iter.hasNext()) {
      CachedUrlSetNode node = (CachedUrlSetNode)iter.next();
      if (node.hasContent()) {
	numAuFiles++;
      }
    }      
  }

  protected File nextExportFile() {
    if (exportFiles == null) {
      exportFiles = exportDir.listFiles();
      exportFileIx = 0;
      if (log.isDebug()) {
	log.debug("Exported files: " + StringUtil.separatedString(exportFiles,
								  ", "));
      }
    }
    if (exportFileIx >= exportFiles.length) {
      return null;
    }
    return exportFiles[exportFileIx++];
  }

  String readHeader(InputStream ins) throws IOException {
    int end[] = {'\r', '\n', '\r', '\n'};
    int endix = 0;
    StringBuilder sb = new StringBuilder();
    int b;
    while ((b = ins.read()) != -1) {
      sb.append((char)b);
      if (b == end[endix]) {
	if (++endix >= end.length) {
	  break;
	}
      } else {
	endix = 0;
      }
    }
    return sb.toString();
  }

}

