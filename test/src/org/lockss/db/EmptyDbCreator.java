/*
 * $Id$
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
package org.lockss.db;

import java.io.File;
import java.util.Properties;
import org.lockss.config.ConfigManager;
import org.lockss.repository.LockssRepositoryImpl;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.Logger;
import org.lockss.util.PlatformUtil;

/**
 * Creates an empty database.
 */
public class EmptyDbCreator {

  /**
   * Main method.
   * @param args
   */
  public static void main(String[] args) throws Exception {
    String tempDirPath = 
	new File(PlatformUtil.getSystemTempDir(), "empty_doc_db").toString();
    String arg;

    for (int i = 0; i < args.length; i++) {
      arg = args[i];

      if (i < args.length - 1 && "-d".equals(arg)) {
	tempDirPath = args[++i];
      }
    }

    ConfigManager.makeConfigManager();
    Logger.resetLogs();
    MockLockssDaemon daemon = new MockLockssDaemon() {};

    // Set the database log.
    System.setProperty("derby.stream.error.file",
		       new File(tempDirPath, "derby.log").getAbsolutePath());

    Properties props = new Properties();
    props.setProperty(LockssRepositoryImpl.PARAM_CACHE_LOCATION, tempDirPath);
    props.setProperty(ConfigManager.PARAM_PLATFORM_DISK_SPACE_LIST,
	tempDirPath);
    ConfigurationUtil.setCurrentConfigFromProps(props);

    DbManager dbManager = new DbManager(true);
    daemon.setDbManager(dbManager);
    dbManager.initService(daemon);
    dbManager.startService();
    dbManager.waitForThreadsToFinish(500);
    System.exit(0);
  }
}
