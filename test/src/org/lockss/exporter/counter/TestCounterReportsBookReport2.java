/*
 * $Id$
 */

/*

 Copyright (c) 2013-2015 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.db.SqlConstants.*;
import static org.lockss.plugin.ArticleFiles.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.util.Calendar;
import org.lockss.daemon.Cron;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsBookReport2;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.extractor.MetadataField;
import org.lockss.metadata.MetadataManager;
import org.lockss.test.ConfigurationUtil;
import org.lockss.test.LockssTestCase;
import org.lockss.test.MockLockssDaemon;
import org.lockss.util.IOUtil;
import org.lockss.util.TimeBase;

/**
 * Test class for org.lockss.exporter.counter.CounterReportsBookReport2.
 * 
 * @author Fernando Garcia-Loygorri
 * @version 1.0
 */
public class TestCounterReportsBookReport2 extends LockssTestCase {
  private static final String FULL_URL = "http://example.com/full.url";
  private static final String SECTION_URL = "http://example.com/section.url";

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

    counterReportsManager = new CounterReportsManager();
    theDaemon.setCounterReportsManager(counterReportsManager);
    counterReportsManager.initService(theDaemon);
    counterReportsManager.startService();
  }

  /**
   * Runs all the tests.
   * <br />
   * This avoids unnecessary set up and tear down of the database.
   * 
   * @throws Exception
   */
  public void testAll() throws Exception {
    runTestValidation();
    runTestEmptyReport();

    initializeFullBookMetadata();
    initializeSectionBookMetadata();

    counterReportsManager.persistRequest(FULL_URL, false);
    counterReportsManager.persistRequest(FULL_URL, true);
    counterReportsManager.persistRequest(SECTION_URL, false);
    counterReportsManager.persistRequest(SECTION_URL, true);

    CounterReportsRequestAggregator aggregator =
	new CounterReportsRequestAggregator(theDaemon);
    aggregator.getCronTask().execute();

    runTestDefaultPeriodReport();
    runTestCustomPeriodReport();
  }

  /**
   * Tests the validation of the constructor parameters.
   * 
   * @throws Exception
   */
  public void runTestValidation() throws Exception {

    try {
      new CounterReportsBookReport2(theDaemon, 0, 2011, 7, 2012);
      fail("Invalid start month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsBookReport2(theDaemon, 13, 2011, 7, 2012);
      fail("Invalid start month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsBookReport2(theDaemon, 1, 2011, 0, 2012);
      fail("Invalid end month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsBookReport2(theDaemon, 1, 2011, 13, 2012);
      fail("Invalid end month - Must be between 1 and 12");
    } catch (IllegalArgumentException iae) {
    }

    try {
      new CounterReportsBookReport2(theDaemon, 1, 2012, 12, 2011);
      fail("Invalid report period - End must not precede start");
    } catch (IllegalArgumentException iae) {
    }

    boolean validArgument = false;

    try {
      new CounterReportsBookReport2(theDaemon, 1, 2012, 1, 2012);
      validArgument = true;
    } catch (IllegalArgumentException iae) {
    }

    assertEquals(true, validArgument);
  }

  /**
   * Tests an empty report.
   * 
   * @throws Exception
   */
  public void runTestEmptyReport() throws Exception {
    CounterReportsBookReport2 report = new CounterReportsBookReport2(theDaemon);

    report.logReport();
    report.saveCsvReport();
    File reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("csv"));
    assertEquals(true, reportFile.exists());

    report.populateReportHeaderEntries();

    BufferedReader reader = new BufferedReader(new FileReader(reportFile));
    String line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals(
	"\"Total for all books\",,,,,,,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());

    report.saveTsvReport();
    reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("txt"));
    assertEquals(true, reportFile.exists());

    reader = new BufferedReader(new FileReader(reportFile));
    line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals(
	"Total for all books\t\t\t\t\t\t\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());
  }

  /**
   * Creates a full book for which to aggregate requests.
   * 
   * @return a Long with the identifier of the created book.
   * @throws DbException
   */
  private Long initializeFullBookMetadata() throws DbException {
    Long mdItemSeq = null;
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Add the publisher.
      Long publisherSeq = dbManager.findOrCreatePublisher(conn, "publisher");

      // Add the publication -- test direct method
      Long publicationSeq =
	  metadataManager.findOrCreateBook(conn, publisherSeq, null, 
		"9876543210987", "9876543210123", "The Full Book", null);

      // Add an alternative name for the publication -- test indirect method
      Long publicationSeq2 =
	  metadataManager.findOrCreatePublication(conn, publisherSeq,
	        null, null,
	      	"9876543210987", "9876543210123", 
	      	MetadataField.PUBLICATION_TYPE_BOOK,
		null, null, "Full Book Alt", null);

      assertEquals(publicationSeq, publicationSeq2);

      // Add the publishing platform.
      Long platformSeq = metadataManager.findOrCreatePlatform(conn,
	  "fullPlatform");

      // Add the plugin.
      Long pluginSeq = metadataManager.findOrCreatePlugin(conn, "fullPluginId",
	  platformSeq, false);

      // Add the AU.
      Long auSeq = metadataManager.findOrCreateAu(conn, pluginSeq, "fullAuKey");

      // Add the provider.
      Long providerSeq = dbManager.findOrCreateProvider(conn, "fullProviderId",
	  "fullProviderName");

      // Add the AU metadata.
      Long auMdSeq =
	  metadataManager.addAuMd(conn, auSeq, 1, 0L, 123L, providerSeq);

      Long parentSeq =
	  metadataManager.findPublicationMetadataItem(conn, publicationSeq);

      metadataManager.addMdItemDoi(conn, parentSeq, "10.1000/182");

      Long mdItemTypeSeq =
	  metadataManager.findMetadataItemType(conn, MD_ITEM_TYPE_BOOK);

      mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
                                            auMdSeq, "2010-01-01", null, 1234L);

      metadataManager.addMdItemName(conn, mdItemSeq, "The Full Book",
	  			    PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, ROLE_FULL_TEXT_HTML,
                                   FULL_URL);
    } finally {
      DbManager.commitOrRollback(conn, log);
      DbManager.safeCloseConnection(conn);
    }
    
    return mdItemSeq;
  }

  /**
   * Creates a book section for which to aggregate requests.
   * 
   * @return a Long with the identifier of the created book.
   * @throws DbException
   */
  private Long initializeSectionBookMetadata() throws DbException {
    Long mdItemSeq = null;
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Add the publisher.
      Long publisherSeq = dbManager.findOrCreatePublisher(conn, "publisher");

      // Add the publication -- test direct method
      Long publicationSeq = metadataManager.findOrCreateBook(conn, publisherSeq,
	  null, "9876543210234","9876543210345", "The Book In Sections",
	  "propId1");

      // Add an alternative name for the publication -- test indirect method
      Long publicationSeq2 = metadataManager.findOrCreatePublication(conn,
	  publisherSeq, null, null, "9876543210234", "9876543210345", 
	  MetadataField.PUBLICATION_TYPE_BOOK, null, null,
	  "Book In Sections Alt", "propId2");

      assertEquals(publicationSeq, publicationSeq2);

      // Add the publishing platform.
      Long platformSeq = metadataManager.findOrCreatePlatform(conn,
	  "secPlatform");

      // Add the plugin.
      Long pluginSeq = metadataManager.findOrCreatePlugin(conn, "secPluginId",
	  platformSeq, false);

      // Add the AU.
      Long auSeq = metadataManager.findOrCreateAu(conn, pluginSeq, "secAuKey");

      // Add the provider.
      Long providerSeq = dbManager.findOrCreateProvider(conn, "secProviderId",
	  "secProviderName");

      // Add the AU metadata.
      Long auMdSeq =
	  metadataManager.addAuMd(conn, auSeq, 1, 0L, 123L, providerSeq);

      Long parentSeq =
	  metadataManager.findPublicationMetadataItem(conn, publicationSeq);

      Long mdItemTypeSeq =
	  metadataManager.findMetadataItemType(conn, MD_ITEM_TYPE_BOOK_CHAPTER);

      mdItemSeq = metadataManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
                                            auMdSeq, "2010-02-02", null, 1234L);

      metadataManager.addMdItemName(conn, mdItemSeq, "Chapter Name",
	  			    PRIMARY_NAME_TYPE);

      metadataManager.addMdItemUrl(conn, mdItemSeq, ROLE_FULL_TEXT_PDF,
                                   SECTION_URL);
    } finally {
      DbManager.commitOrRollback(conn, log);
      DbManager.safeCloseConnection(conn);
    }
    
    return mdItemSeq;
  }

  /**
   * Tests a report for the default period.
   * 
   * @throws Exception
   */
  public void runTestDefaultPeriodReport() throws Exception {
    CounterReportsBookReport2 report = new CounterReportsBookReport2(theDaemon);

    report.logReport();
    report.saveCsvReport();
    File reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("csv"));
    assertEquals(true, reportFile.exists());

    report.populateReportHeaderEntries();

    BufferedReader reader = new BufferedReader(new FileReader(reportFile));
    String line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals(
	"\"Total for all books\",,,,,,,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());

    report.saveTsvReport();
    reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("txt"));
    assertEquals(true, reportFile.exists());

    reader = new BufferedReader(new FileReader(reportFile));
    line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals(
	"Total for all books\t\t\t\t\t\t\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0\t0",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());
  }

  /**
   * Tests a report for a custom period.
   * 
   * @throws Exception
   */
  public void runTestCustomPeriodReport() throws Exception {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(TimeBase.nowDate());
    int endYear = calendar.get(Calendar.YEAR);
    int endMonth = calendar.get(Calendar.MONTH) + 1;
    calendar.add(Calendar.MONTH, -4);
    int startYear = calendar.get(Calendar.YEAR);
    int startMonth = calendar.get(Calendar.MONTH) + 1;

    CounterReportsBookReport2 report =
	new CounterReportsBookReport2(theDaemon, startMonth, startYear,
	    endMonth, endYear);

    report.logReport();
    report.saveCsvReport();
    File reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("csv"));
    assertEquals(true, reportFile.exists());

    report.populateReportHeaderEntries();

    BufferedReader reader = new BufferedReader(new FileReader(reportFile));
    String line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals("\"Total for all books\",,,,,,,1,0,0,0,0,1", line);
    line = reader.readLine();
    assertEquals("\"The Book In Sections\",publisher,secPlatform,,\"propId1, propId2\",987-654321-0234,987-654321-0345,1,0,0,0,0,1",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());

    report.saveTsvReport();
    reportFile =
	new File(counterReportsManager.getOutputDir(),
	    report.getReportFileName("txt"));
    assertEquals(true, reportFile.exists());

    reader = new BufferedReader(new FileReader(reportFile));
    line = reader.readLine();

    for (int i = 0; i < 8; i++) {
      line = reader.readLine();
    }

    assertEquals("Total for all books\t\t\t\t\t\t\t1\t0\t0\t0\t0\t1", line);
    line = reader.readLine();
    assertEquals(
	"The Book In Sections\tpublisher\tsecPlatform\t\tpropId1, propId2\t987-654321-0234\t987-654321-0345\t1\t0\t0\t0\t0\t1",
	line);
    assertNull(reader.readLine());

    IOUtil.safeClose(reader);
    reportFile.delete();
    assertEquals(false, reportFile.exists());
  }
}
