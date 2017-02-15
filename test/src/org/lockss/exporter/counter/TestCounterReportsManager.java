/*
 * $Id$
 */

/*

 Copyright (c) 2012-2014 Board of Trustees of Leland Stanford Jr. University,
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
package org.lockss.exporter.counter;

import java.io.PrintWriter;
import org.lockss.daemon.Cron;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.metadata.MetadataManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.IOUtil;

/**
 * Test class for org.lockss.exporter.counter.CounterReportsManager.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
public class TestCounterReportsManager extends LockssTestCase {
  private MockLockssDaemon theDaemon;
  private DbManager dbManager;
  private MetadataManager metadataManager;
  private CounterReportsManager counterReportsManager;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    ConfigurationUtil.addFromArgs(CounterReportsManager.PARAM_COUNTER_ENABLED,
	"true");
    ConfigurationUtil.addFromArgs(CounterReportsManager
	.PARAM_REPORT_BASEDIR_PATH, tempDirPath);

    theDaemon = getMockLockssDaemon();
    theDaemon.setDaemonInited(true);

    dbManager = getTestDbManager(tempDirPath);

    metadataManager = new MetadataManager();
    theDaemon.setMetadataManager(metadataManager);
    metadataManager.initService(theDaemon);
    metadataManager.startService();

    Cron cron = new Cron();
    theDaemon.setCron(cron);
    cron.initService(theDaemon);
    cron.startService();
  }

  /**
   * Runs all the tests.
   * <br />
   * This avoids unnecessary set up and tear down of the database.
   * 
   * @throws Exception
   */
  public void testAll() throws Exception {
    startService();
    runTestReady();
    runTestAllBooksAllJournalsTitles();
    runTestOutputDir();
    runTestWriteDeleteReportFile();
    runTestNotReady();
  }

  /**
   * Creates and starts the CounterReportsManager.
   * 
   * @throws Exception
   */
  private void startService() throws Exception {
    counterReportsManager = new CounterReportsManager();
    theDaemon.setCounterReportsManager(counterReportsManager);
    counterReportsManager.initService(theDaemon);
    counterReportsManager.startService();
  }

  /**
   * Tests a CounterReportsManager that is ready to be used.
   * 
   * @throws Exception
   */
  public void runTestReady() throws Exception {
    assertEquals(true, counterReportsManager.isReady());
  }

  /**
   * Tests the generation of titles used for totals.
   * 
   * @throws Exception
   */
  public void runTestAllBooksAllJournalsTitles() throws Exception {
    assertEquals(1L,
	counterReportsManager.getAllBooksPublicationSeq().longValue());
    assertEquals(2L,
	counterReportsManager.getAllJournalsPublicationSeq().longValue());
  }

  /**
   * Tests the output directory.
   * 
   * @throws Exception
   */
  public void runTestOutputDir() throws Exception {
    assertEquals(true, counterReportsManager.getOutputDir().exists());
  }

  /**
   * Tests the writing and removal of a report file.
   * 
   * @throws Exception
   */
  public void runTestWriteDeleteReportFile() throws Exception {
    String fileName = "testFile";
    PrintWriter writer = counterReportsManager.getReportOutputWriter(fileName);
    writer.println("test line");
    assertEquals(false, writer.checkError());
    IOUtil.safeClose(writer);
    assertEquals(true, counterReportsManager.deleteReportOutputFile(fileName));
    assertEquals(false, counterReportsManager.deleteReportOutputFile(fileName));
  }

  /**
   * Tests a CounterReportsManager that is not ready to be used.
   * 
   * @throws Exception
   */
  public void runTestNotReady() throws Exception {
    counterReportsManager.stopService();
    dbManager.stopService();
    startService();
    assertEquals(false, counterReportsManager.isReady());
    assertEquals(false, counterReportsManager.deleteReportOutputFile(""));
    assertNull(counterReportsManager.getReportOutputWriter(""));
  }
}
