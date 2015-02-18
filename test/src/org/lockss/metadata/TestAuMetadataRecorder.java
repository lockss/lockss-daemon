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
package org.lockss.metadata;

import static org.lockss.db.SqlConstants.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Cron;
import org.lockss.daemon.PluginException;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.exporter.counter.CounterReportsRequestAggregator;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
import org.lockss.metadata.ArticleMetadataBuffer.ArticleMetadataInfo;
import org.lockss.metadata.TestMetadataManager.MySubTreeArticleIteratorFactory;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.ArticleFiles;
import org.lockss.plugin.ArticleIteratorFactory;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.plugin.PluginTestUtil;
import org.lockss.plugin.simulated.SimulatedArchivalUnit;
import org.lockss.plugin.simulated.SimulatedContentGenerator;
import org.lockss.plugin.simulated.SimulatedPlugin;
import org.lockss.test.*;
import org.lockss.util.ExternalizableMap;
import org.lockss.util.Logger;

/**
 * Test class for org.lockss.metadata.AuMetadataRecorder
 * 
 * @author Fernando Garcia-Loygorri
 */
public class TestAuMetadataRecorder extends LockssTestCase {
  static Logger log = Logger.getLogger(TestAuMetadataRecorder.class);

  private SimulatedArchivalUnit sau0;
  private MetadataManager metadataManager;
  private DbManager dbManager;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED,
	"true");
    ConfigurationUtil.addFromArgs(CounterReportsManager.PARAM_COUNTER_ENABLED,
	"true");
    ConfigurationUtil.addFromArgs(CounterReportsManager
	.PARAM_REPORT_BASEDIR_PATH, tempDirPath);
    ConfigurationUtil.addFromArgs(CounterReportsRequestAggregator
	.PARAM_COUNTER_REQUEST_AGGREGATION_TASK_FREQUENCY, "hourly");

    MockLockssDaemon theDaemon = getMockLockssDaemon();
    theDaemon.getAlertManager();

    PluginManager pluginManager = theDaemon.getPluginManager();
    pluginManager.setLoadablePluginsReady(true);
    theDaemon.setDaemonInited(true);
    pluginManager.startService();
    theDaemon.getCrawlManager();

    sau0 = PluginTestUtil.createAndStartSimAu(MySimulatedPlugin0.class,
                                              simAuConfig(tempDirPath + "/0"));

    dbManager = getTestDbManager(tempDirPath);

    metadataManager = new MetadataManager();
    theDaemon.setMetadataManager(metadataManager);
    metadataManager.initService(theDaemon);
    metadataManager.startService();

    Cron cron = new Cron();
    theDaemon.setCron(cron);
    cron.initService(theDaemon);
    cron.startService();

    CounterReportsManager counterReportsManager = new CounterReportsManager();
    theDaemon.setCounterReportsManager(counterReportsManager);
    counterReportsManager.initService(theDaemon);
    counterReportsManager.startService();

    theDaemon.setAusStarted(true);
  }

  private Configuration simAuConfig(String rootPath) {
    Configuration conf = ConfigManager.newConfiguration();
    conf.put("root", rootPath);
    conf.put("depth", "2");
    conf.put("branch", "1");
    conf.put("numFiles", "3");
    conf.put("fileTypes", "" + (SimulatedContentGenerator.FILE_TYPE_PDF +
                                SimulatedContentGenerator.FILE_TYPE_HTML));
    conf.put("binFileSize", "7");
    return conf;
  }

  /**
   * Runs all the tests.
   * <br />
   * This avoids unnecessary set up and tear down of the database.
   * 
   * @throws Exception
   */
  public void testAll() throws Exception {
    runRecordJournal1();
    runRecordBook1();
    runRecordBookSeries1();
    runRecordUnknownPublisher1();
    runRecordUnknownPublisher2();
    runRecordUnknownPublisher3();
    runRecordUnknownPublisher4();
    runRecordNoJournalTitleNoISSNJournal();
    runNormalizeMetadataTest();
    runRecordMultipleJournalsAndPublishers();
  }

  ReindexingTask newReindexingTask(ArchivalUnit au,
				   ArticleMetadataExtractor ame) {
    ReindexingTask res = new ReindexingTask(au, ame);
    res.setWDog(new MockLockssWatchdog());
    return res;
  }

  /**
   * Records a journal.
   * 
   * @throws Exception
   */
  private void runRecordJournal1() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Get the initial number of publishers.
      int initialPublisherCount = countPublishers(conn);
      assertNotEquals(-1, initialPublisherCount);

      // Get the initial number of publications.
      int initialPublicationCount = countPublications(conn);
      assertNotEquals(-1, initialPublicationCount);
      
      // Get the initial number of unknown publication titles
      int initialUnknownTitlesCount = 
          countUnknownTitles(conn);

      // Get the initial number of archival units.
      int initialAuMdCount = countArchivalUnits(conn);
      assertNotEquals(-1, initialAuMdCount);

      // Get the initial number of articles.
      int initialArticleCount = countAuMetadataItems(conn);
      assertNotEquals(-1, initialArticleCount);

      // index with no publication titles to create unknown title entries 
      int nTitles = 1;
      int nArticles = 6;
      ArticleMetadataBuffer metadata = getJournalMetadata("Publisher", nTitles,
	  nArticles, true, false, null, false);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
                .getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
          .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that nTitles publication exists.
      assertEquals(nTitles, countPublications(conn) - initialPublicationCount);

      // Check that nTitles unknown publication title exists.
      assertEquals(nTitles, 
                   countUnknownTitles(conn) - initialUnknownTitlesCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that nArticles articles for each of the nTitle titles
      assertEquals(nTitles*nArticles, 
                   countAuMetadataItems(conn) - initialArticleCount);
      
      // now index the same AU, this time with a publication title
      metadata = getJournalMetadata("Publisher", nTitles, nArticles, false,
	  false, null, false);
      
      task = newReindexingTask(sau0, sau0.getPlugin()
          .getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
          .recordMetadata(conn, metadata.iterator());
      
      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);
      
      // Check that nTitles publication exists.
      assertEquals(1, countPublications(conn) - initialPublicationCount);
      
      // Check that 0 unknown publication title exists.
      assertEquals(0, countUnknownTitles(conn) - initialUnknownTitlesCount);
      
      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);
      
      // Check that nArticles articles for each of the nTitle titles
      assertEquals(nTitles*nArticles, 
                   countAuMetadataItems(conn) - initialArticleCount);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  private ArticleMetadataBuffer getJournalMetadata(String publishername,
      int publicationCount, int articleCount, boolean noJournalTitles,
      boolean noIssns, Map<String, String> featuredUrls,
      boolean multiplePublishers) throws IOException {
    ArticleMetadataBuffer result = new ArticleMetadataBuffer(getTempDir());

    for (int i = 1; i <= publicationCount; i++) {
      for (int j = 1; j <= articleCount; j++) {
	ArticleMetadata am = new ArticleMetadata();
	am.put(MetadataField.FIELD_PUBLICATION_TYPE,
	       MetadataField.PUBLICATION_TYPE_JOURNAL);

	if (publishername != null) {
	  if (multiplePublishers) {
	    am.put(MetadataField.FIELD_PUBLISHER, publishername + i);
	  } else {
	    am.put(MetadataField.FIELD_PUBLISHER, publishername);
	  }
	}

	if (!noJournalTitles) {
	  am.put(MetadataField.FIELD_PUBLICATION_TITLE, "Journal Title" + i);
	}
	if (!noIssns) {
	  am.put(MetadataField.FIELD_ISSN, "1234-567" + i);
	  am.put(MetadataField.FIELD_EISSN, "4321-765" + i);
	}

	am.put(MetadataField.FIELD_DATE, "2012-12-0" + j);
	am.put(MetadataField.FIELD_ARTICLE_TITLE, "Article Title" + i + j);
	am.put(MetadataField.FIELD_AUTHOR, "Author,First" + i + j);
	am.put(MetadataField.FIELD_AUTHOR, "Author,Second" + i + j);
	am.put(MetadataField.FIELD_ACCESS_URL, "http://xyz.com/" + i + j);

	if (featuredUrls != null) {
	  am.putRaw(MetadataField.FIELD_FEATURED_URL_MAP.getKey(),
	      featuredUrls);
	}

	result.add(am);
      }
    }

    return result;
  }


  /**
   * Records a book.
   * 
   * @throws Exception
   */
  private void runRecordBook1() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Get the initial number of publishers.
      int initialPublisherCount = countPublishers(conn);
      assertNotEquals(-1, initialPublisherCount);

      // Get the initial number of publications.
      int initialPublicationCount = countPublications(conn);
      assertNotEquals(-1, initialPublicationCount);
      
      // Get the initial number of unknown publication titles
      int initialUnknownTitlesCount = countUnknownTitles(conn);

      // Get the initial number of archival units.
      int initialAuMdCount = countArchivalUnits(conn);
      assertNotEquals(-1, initialAuMdCount);

      // Get the initial number of chapters.
      int initialChapterCount = countAuMetadataItems(conn);
      assertNotEquals(-1, initialChapterCount);

      // index without book titles to create unknown title entries
      int nTitles = 2;
      int nChapters = 3;
      ArticleMetadataBuffer metadata = 
        getBookMetadata("Publisher", nTitles, nChapters, true, false);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
		.getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that nTitles publications exists.
      assertEquals(nTitles, countPublications(conn) - initialPublicationCount);

      // check that each series is an unknown publication 
      // because it had no title
      int totalUnknowns = nTitles;
      assertEquals(totalUnknowns, 
                   countUnknownTitles(conn) - initialUnknownTitlesCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that the total number of chapters is nChapters for each title
      assertEquals(nTitles*nChapters, 
                   countAuMetadataItems(conn) - initialChapterCount);
      
      // now reindex, filling in the book titles
      metadata = 
          getBookMetadata("Publisher", nTitles, nChapters, false, false);

        // Write the AU metadata to the database.
        new AuMetadataRecorder(task, metadataManager, sau0)
            .recordMetadata(conn, metadata.iterator());

        // Check that 1 publisher exists.
        assertEquals(1, countPublishers(conn) - initialPublisherCount);

        // Check that nTitles publications exists.
        assertEquals(nTitles, countPublications(conn) - initialPublicationCount);

        // check that there are now no unknown titles
        assertEquals(0, countUnknownTitles(conn) - initialUnknownTitlesCount);

        // Check that 1 archival unit exists.
        assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

        // Check that the total number of chapters is nChapters for each title
        assertEquals(nTitles*nChapters, 
                     countAuMetadataItems(conn) - initialChapterCount);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  private ArticleMetadataBuffer getBookMetadata(String publishername,
      int publicationCount, int articleCount,
      boolean noTitles, boolean noIsbns) throws IOException {
    ArticleMetadataBuffer result = new ArticleMetadataBuffer(getTempDir());

    for (int i = 1; i <= publicationCount; i++) {
      for (int j = 1; j <= articleCount; j++) {
	ArticleMetadata am = new ArticleMetadata();
        am.put(MetadataField.FIELD_PUBLICATION_TYPE,
            MetadataField.PUBLICATION_TYPE_BOOK);

	if (publishername != null) {
	  am.put(MetadataField.FIELD_PUBLISHER, publishername);
	}

	if (!noTitles) {
	  am.put(MetadataField.FIELD_PUBLICATION_TITLE, "Book Title" + i);
	}
	
	if (!noIsbns) {
	  am.put(MetadataField.FIELD_ISBN, "01234567" + i + "X");
	  am.put(MetadataField.FIELD_EISBN, "98765432" + i + "X");
	}
	
	am.put(MetadataField.FIELD_DATE, "2012-12-0" + j);
	am.put(MetadataField.FIELD_ARTICLE_TITLE, "Article Title" + i + j);
	am.put(MetadataField.FIELD_AUTHOR, "Author,First" + i + j);
	am.put(MetadataField.FIELD_AUTHOR, "Author,Second" + i + j);
	am.put(MetadataField.FIELD_ACCESS_URL, "http://xyz.com/" + i + j);

	result.add(am);
      }
    }

    return result;
  }

  /**
   * Records a book series.
   * 
   * @throws Exception
   */
  private void runRecordBookSeries1() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Get the initial number of publishers.
      int initialPublisherCount = countPublishers(conn);
      assertNotEquals(-1, initialPublisherCount);

      // Get the initial number of publications.
      int initialPublicationCount = countPublications(conn);
      assertNotEquals(-1, initialPublicationCount);

      // Get the initial number of archival units.
      int initialAuMdCount = countArchivalUnits(conn);
      assertNotEquals(-1, initialAuMdCount);

      // Get the initial number of chapters.
      int initialChapterCount = countAuMetadataItems(conn);
      assertNotEquals(-1, initialChapterCount);

      // Get the initial number of unknown publication titles
      int initialUnknownTitlesCount = countUnknownTitles(conn);
      
      // index with no publication or series titles 
      // to create unknown title entries
      int nSeries = 1;
      int nTitles = 2;
      int nChapters = 3;
      ArticleMetadataBuffer metadata = 
          getBookSeriesMetadata("Publisher", 
                                nSeries, nTitles, nChapters,
                                true, false, true, false);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
                .getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
          .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that there is a publication for each title in each series for
      // each publisher, plus one for each series which counts as a publication
      int totalPublications = nSeries*(nTitles + 1); 
      assertEquals(totalPublications, 
                   countPublications(conn) - initialPublicationCount);

      // check that each series and each title in the series
      // is an unknown publication because it had no title
      int totalUnknowns = nSeries * (nTitles + 1);
      assertEquals(totalUnknowns, 
                   countUnknownTitles(conn) - initialUnknownTitlesCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that that the number of chapters is the number of
      // chapters for all the titles and series
      int totalChapters = nSeries*nTitles*nChapters;
      assertEquals(totalChapters, 
                   countAuMetadataItems(conn) - initialChapterCount);
      
      // now reindex the same content but provide 
      // publication and series titles
      metadata = getBookSeriesMetadata("Publisher", 
          nSeries, nTitles, nChapters,
          false, false, false, false);

      task = newReindexingTask(sau0, sau0.getPlugin()
          .getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
          .recordMetadata(conn, metadata.iterator());
      
      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);
      
      // check that each series is an unknown publication 
      // because it had no title
      assertEquals(totalPublications, 
                   countPublications(conn) - initialPublicationCount);
      
      // check that there are now no unknown publications because
      // the series unknown series titles have been replaced
      assertEquals(0, countUnknownTitles(conn) - initialUnknownTitlesCount);
      
      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);
      
      // Check that that the number of chapters is the number of
      // chapters for all the titles and series
      assertEquals(totalChapters, 
                   countAuMetadataItems(conn) - initialChapterCount);
    
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  private ArticleMetadataBuffer getBookSeriesMetadata(String publishername,
      int seriesCount, int publicationCount, int articleCount,
      boolean noSeriesTitles, boolean noIssns, 
      boolean noBookTitles, boolean noIsbns) throws IOException {
    ArticleMetadataBuffer result = new ArticleMetadataBuffer(getTempDir());

    for (int sc = 1; sc <= seriesCount; sc++) {
      for (int pc = 1; pc <= publicationCount; pc++) {
        for (int ac = 1; ac <= articleCount; ac++) {
          ArticleMetadata am = new ArticleMetadata();
          am.put(MetadataField.FIELD_PUBLICATION_TYPE, 
                 MetadataField.PUBLICATION_TYPE_BOOKSERIES);

          if (publishername != null) {
            am.put(MetadataField.FIELD_PUBLISHER, publishername);
          }
  
          if (!noSeriesTitles) {
            am.put(MetadataField.FIELD_SERIES_TITLE, "Book Series Title" + sc);
          }
          if (!noIssns) {
            am.put(MetadataField.FIELD_ISSN, "1234-567" + sc);
            am.put(MetadataField.FIELD_EISSN, "4321-765" + sc);
          }
          
          if (!noBookTitles) {
            am.put(MetadataField.FIELD_PUBLICATION_TITLE, 
                   "Book In Series Title" + sc + pc);
          }
          
          if (!noIsbns) {
            am.put(MetadataField.FIELD_ISBN, "02468024" + sc + pc);
            am.put(MetadataField.FIELD_EISBN, "86430864" + sc + pc);
          }
          
          am.put(MetadataField.FIELD_DATE, "2012-12-0" + ac);
          am.put(MetadataField.FIELD_ARTICLE_TITLE, 
                 "Article Title" + sc + pc + ac);
          am.put(MetadataField.FIELD_AUTHOR, 
                 "Author,First" + sc + pc + ac);
          am.put(MetadataField.FIELD_AUTHOR, 
                 "Author,Second" + sc + pc + ac);
          am.put(MetadataField.FIELD_ACCESS_URL, 
                 "http://xyz.com/" + sc + pc + ac);
  
          result.add(am);
        }
      }
    }

    return result;
  }

  /**
   * Records a journal with no publisher name first and with a publisher name
   * afterwards.
   * 
   * @throws Exception
   */
  private void runRecordUnknownPublisher1() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Get the initial number of publishers.
      int initialPublisherCount = countPublishers(conn);
      assertNotEquals(-1, initialPublisherCount);

      // Get the initial number of publications.
      int initialPublicationCount = countPublications(conn);
      assertNotEquals(-1, initialPublicationCount);

      // Get the initial number of archival units.
      int initialAuMdCount = countArchivalUnits(conn);
      assertNotEquals(-1, initialAuMdCount);

      // Get the initial number of articles.
      int initialArticleCount = countAuMetadataItems(conn);
      assertNotEquals(-1, initialArticleCount);

      // Get the initial number of archival unit problems.
      int initialProblemCount = countAuProblems(conn);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
		.getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      ArticleMetadataBuffer metadata = getJournalMetadata(null, 1, 8, false,
	  false, null, false);

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that 1 publication exists.
      assertEquals(1, countPublications(conn) - initialPublicationCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that 8 articles exist.
      assertEquals(8, countAuMetadataItems(conn) - initialArticleCount);

      // Check that 1 archival unit problem exists.
      assertEquals(1, countAuProblems(conn) - initialProblemCount);

      Long publicationSeq = getPublicationSeq(conn,
		AuMetadataRecorder.UNKNOWN_PUBLISHER_AU_PROBLEM + "%");
      assertNotNull(publicationSeq);

      addJournalTypeAggregates(conn, publicationSeq, true, 2013, 1, 30, 20, 10);

      metadata =
	  getJournalMetadata("Publisher", 1, 8, false, false, null, false);

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that 1 publication exists.
      assertEquals(1, countPublications(conn) - initialPublicationCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that 8 articles exist.
      assertEquals(8, countAuMetadataItems(conn) - initialArticleCount);

      // Check that 0 archival unit problems exist.
      assertEquals(0, countAuProblems(conn) - initialProblemCount);

      checkJournalTypeAggregates(conn, "Publisher", true, 2013, 1, 30, 20, 10);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Records a journal with a publisher name first and with no publisher name
   * afterwards.
   * 
   * @throws Exception
   */
  private void runRecordUnknownPublisher2() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Get the initial number of publishers.
      int initialPublisherCount = countPublishers(conn);
      assertNotEquals(-1, initialPublisherCount);

      // Get the initial number of publications.
      int initialPublicationCount = countPublications(conn);
      assertNotEquals(-1, initialPublicationCount);

      // Get the initial number of archival units.
      int initialAuMdCount = countArchivalUnits(conn);
      assertNotEquals(-1, initialAuMdCount);

      // Get the initial number of articles.
      int initialArticleCount = countAuMetadataItems(conn);
      assertNotEquals(-1, initialArticleCount);

      // Get the initial number of archival unit problems.
      int initialProblemCount = countAuProblems(conn);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
		.getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      ArticleMetadataBuffer metadata =
	  getJournalMetadata("Publisher", 1, 8, false, false, null, false);

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that 1 publication exists.
      assertEquals(1, countPublications(conn) - initialPublicationCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that 8 articles exist.
      assertEquals(8, countAuMetadataItems(conn) - initialArticleCount);

      // Check that 0 archival unit problems exist.
      assertEquals(0, countAuProblems(conn) - initialProblemCount);

      metadata = getJournalMetadata(null, 1, 8, false, false, null, false);

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that 1 publication exists.
      assertEquals(1, countPublications(conn) - initialPublicationCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that 8 articles exist.
      assertEquals(8, countAuMetadataItems(conn) - initialArticleCount);

      // Check that 0 archival unit problems exist.
      assertEquals(0, countAuProblems(conn) - initialProblemCount);
      
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Records a journal with no publisher name first and with a publisher name
   * afterwards deleting the AU metadata in between.
   * 
   * @throws Exception
   */
  private void runRecordUnknownPublisher3() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Get the initial number of publishers.
      int initialPublisherCount = countPublishers(conn);
      assertNotEquals(-1, initialPublisherCount);

      // Get the initial number of publications.
      int initialPublicationCount = countPublications(conn);
      assertNotEquals(-1, initialPublicationCount);

      // Get the initial number of archival units.
      int initialAuMdCount = countArchivalUnits(conn);
      assertNotEquals(-1, initialAuMdCount);

      // Get the initial number of articles.
      int initialArticleCount = countAuMetadataItems(conn);
      assertNotEquals(-1, initialArticleCount);

      // Get the initial number of archival unit problems.
      int initialProblemCount = countAuProblems(conn);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
		.getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      ArticleMetadataBuffer metadata = 
          getJournalMetadata(null, 1, 8, false, false, null, false);

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that 1 publication exists.
      assertEquals(1, countPublications(conn) - initialPublicationCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that 8 articles exist.
      assertEquals(8, countAuMetadataItems(conn) - initialArticleCount);

      // Check that 1 archival unit problem exists.
      assertEquals(1, countAuProblems(conn) - initialProblemCount);

      Long publicationSeq = getPublicationSeq(conn,
		AuMetadataRecorder.UNKNOWN_PUBLISHER_AU_PROBLEM + "%");
      assertNotNull(publicationSeq);

      addJournalTypeAggregates(conn, publicationSeq, true, 2013, 1, 30, 20, 10);

      metadataManager.getMetadataManagerSql().removeAu(conn, sau0.getAuId());

      metadata =
	  getJournalMetadata("Publisher", 1, 8, false, false, null, false);

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that 1 publication exists.
      assertEquals(1, countPublications(conn) - initialPublicationCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that 8 articles exist.
      assertEquals(8, countAuMetadataItems(conn) - initialArticleCount);

      // Check that 0 archival unit problems exist.
      assertEquals(0, countAuProblems(conn) - initialProblemCount);

      checkJournalTypeAggregates(conn, "Publisher", true, 2013, 1, 30, 20, 10);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Records a journal with a publisher name first and with no publisher name
   * afterwards deleting the AU metadata in between.
   * 
   * @throws Exception
   */
  private void runRecordUnknownPublisher4() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Get the initial number of publishers.
      int initialPublisherCount = countPublishers(conn);
      assertNotEquals(-1, initialPublisherCount);

      // Get the initial number of publications.
      int initialPublicationCount = countPublications(conn);
      assertNotEquals(-1, initialPublicationCount);

      // Get the initial number of archival units.
      int initialAuMdCount = countArchivalUnits(conn);
      assertNotEquals(-1, initialAuMdCount);

      // Get the initial number of articles.
      int initialArticleCount = countAuMetadataItems(conn);
      assertNotEquals(-1, initialArticleCount);

      // Get the initial number of archival unit problems.
      int initialProblemCount = countAuProblems(conn);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
		.getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      ArticleMetadataBuffer metadata =
	  getJournalMetadata("Publisher", 1, 8, false, false,  null, false);

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exists.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that 1 publication exists.
      assertEquals(1, countPublications(conn) - initialPublicationCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that 8 articles exist.
      assertEquals(8, countAuMetadataItems(conn) - initialArticleCount);

      // Check that 0 archival unit problems exist.
      assertEquals(0, countAuProblems(conn) - initialProblemCount);

      metadataManager.getMetadataManagerSql().removeAu(conn, sau0.getAuId());

      metadata = getJournalMetadata(null, 1, 8, false, false, null, false);

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 2 publisher exist.
      assertEquals(2, countPublishers(conn) - initialPublisherCount);

      // Check that 2 publication exist.
      assertEquals(2, countPublications(conn) - initialPublicationCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that 8 articles exist.
      assertEquals(8, countAuMetadataItems(conn) - initialArticleCount);

      // Check that 1 archival unit problem exists.
      assertEquals(1, countAuProblems(conn) - initialProblemCount);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  /**
   * Records a journal with no title.
   * 
   * @throws Exception
   */
  private void runRecordNoJournalTitleNoISSNJournal() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Get the initial number of publishers.
      int initialPublisherCount = countPublishers(conn);
      assertNotEquals(-1, initialPublisherCount);

      // Get the initial number of publications.
      int initialPublicationCount = countPublications(conn);
      assertNotEquals(-1, initialPublicationCount);
      
      // Get the initial number of unknown publication titles
      int initialUnknownTitlesCount = countUnknownTitles(conn);

      // Get the initial number of archival units.
      int initialAuMdCount = countArchivalUnits(conn);
      assertNotEquals(-1, initialAuMdCount);

      // Get the initial number of articles.
      int initialArticleCount = countAuMetadataItems(conn);
      assertNotEquals(-1, initialArticleCount);

      int nTitles = 1;
      int nArticles = 6;
      ArticleMetadataBuffer metadata = getJournalMetadata("Publisher", nTitles,
	  nArticles, true, true, null, false);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
		.getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
	  .recordMetadata(conn, metadata.iterator());

      // Check that 1 publisher exist.
      assertEquals(1, countPublishers(conn) - initialPublisherCount);

      // Check that a title was created for each article in each publication.
      // This happens because there is no way to match publications with
      // no title or ISSN.
      assertEquals(nTitles*nArticles, 
                   countPublications(conn) - initialPublicationCount);
      
      // Check that all of the titles created are unknown titles
      assertEquals(nTitles*nArticles, 
                   countUnknownTitles(conn) - initialUnknownTitlesCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that nArticle articles exist for each of nTitles
      assertEquals(nTitles*nArticles, 
                   countAuMetadataItems(conn) - initialArticleCount);
    } catch (MetadataException me) {
      // Expected.
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  private int countPublishers(Connection conn)
      throws SQLException, DbException {
    int count = -1;
    PreparedStatement stmt = null;

    try {
      String query = "select count(*) from " + PUBLISHER_TABLE;
      stmt = dbManager.prepareStatement(conn, query);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      stmt.close();
    }

    return count;
  }

  private int countPublications(Connection conn)
      throws SQLException, DbException {
    int count = -1;
    PreparedStatement stmt = null;

    try {
      String q = "  select " + MD_ITEM_NAME_TABLE + ".name from "
          + PUBLICATION_TABLE + "," + MD_ITEM_TABLE + "," + MD_ITEM_NAME_TABLE
          + " where " + PUBLICATION_TABLE + ".md_item_seq = "
          + MD_ITEM_TABLE + ".md_item_seq and "
          + MD_ITEM_NAME_TABLE + ".md_item_seq = " 
          + MD_ITEM_TABLE + ".md_item_seq";
      stmt = dbManager.prepareStatement(conn, q);
      ResultSet r = dbManager.executeQuery(stmt);

      while (r.next()) {
        r.getString(1);
      }
      
      String query = "select count(*) from " + PUBLICATION_TABLE;
      stmt = dbManager.prepareStatement(conn, query);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      stmt.close();
    }

    return count;
  }

  private int countUnknownTitles(Connection conn)
      throws SQLException, DbException {
    int count = -1;
    PreparedStatement stmt = null;

    try {
      String query = "select " + MD_ITEM_NAME_TABLE + ".name from " 
          + PUBLICATION_TABLE + "," + MD_ITEM_TABLE + "," + MD_ITEM_NAME_TABLE
          + " where " + PUBLICATION_TABLE + ".md_item_seq = " + MD_ITEM_TABLE
          + ".md_item_seq and " + MD_ITEM_TABLE + ".md_item_seq = "
          + MD_ITEM_NAME_TABLE + ".md_item_seq and " + MD_ITEM_NAME_TABLE
          + ".name like 'UNKNOWN%'";  // name starts with "UNKNOWN"
                    
      stmt = dbManager.prepareStatement(conn, query);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      while (resultSet.next()) {
        resultSet.getString(1);
      }
    } finally {
      stmt.close();
    }
    
    try {
      String query = "select count(*) from " 
          + PUBLICATION_TABLE + "," + MD_ITEM_TABLE + "," + MD_ITEM_NAME_TABLE
          + " where " + PUBLICATION_TABLE + ".md_item_seq = " + MD_ITEM_TABLE
          + ".md_item_seq and " + MD_ITEM_TABLE + ".md_item_seq = "
          + MD_ITEM_NAME_TABLE + ".md_item_seq and " + MD_ITEM_NAME_TABLE
          + ".name like 'UNKNOWN%'";  // name starts with "UNKNOWN"
                    
      stmt = dbManager.prepareStatement(conn, query);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      if (resultSet.next()) {
        count = resultSet.getInt(1);
      }
    } finally {
      stmt.close();
    }

    return count;
  }

  private int countArchivalUnits(Connection conn)
      throws SQLException, DbException {
    int count = -1;
    PreparedStatement stmt = null;

    try {
      String query = "select count(*) from " + AU_MD_TABLE;
      stmt = dbManager.prepareStatement(conn, query);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      stmt.close();
    }

    return count;
  }

  private int countAuMetadataItems(Connection conn)
      throws SQLException, DbException {
    int count = -1;
    PreparedStatement stmt = null;

    try {
      String query = "select count(*) from " + MD_ITEM_TABLE + " where "
	  + AU_MD_SEQ_COLUMN + " is not null";
      stmt = dbManager.prepareStatement(conn, query);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      stmt.close();
    }

    return count;
  }

  private int countAuProblems(Connection conn)
      throws SQLException, DbException {
    int count = -1;
    PreparedStatement stmt = null;

    try {
      String query = "select count(*) from " + AU_PROBLEM_TABLE;
      stmt = dbManager.prepareStatement(conn, query);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      if (resultSet.next()) {
	count = resultSet.getInt(1);
      }
    } finally {
      stmt.close();
    }

    return count;
  }

  private void addJournalTypeAggregates(Connection conn, Long publicationSeq,
      boolean isPublisherInvolved, int year, int month, int totalRequests,
      int htmlRequests, int pdfRequests) throws SQLException, DbException {
    PreparedStatement stmt = null;

    try {
      String query = "insert into " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
	  + " (" + PUBLICATION_SEQ_COLUMN
	  + "," + IS_PUBLISHER_INVOLVED_COLUMN
	  + "," + REQUEST_YEAR_COLUMN
	  + "," + REQUEST_MONTH_COLUMN
	  + "," + TOTAL_REQUESTS_COLUMN
	  + "," + HTML_REQUESTS_COLUMN
	  + "," + PDF_REQUESTS_COLUMN
	  + ") values (?, ?, ?, ?, ?, ?, ?)";
      stmt = dbManager.prepareStatement(conn, query);
      stmt.setLong(1, publicationSeq);
      stmt.setBoolean(2, isPublisherInvolved);
      stmt.setShort(3, (short)year);
      stmt.setShort(4, (short)month);
      stmt.setInt(5, totalRequests);
      stmt.setInt(6, htmlRequests);
      stmt.setInt(7, pdfRequests);
      dbManager.executeUpdate(stmt);
    } finally {
      stmt.close();
    }
  }

  private Long getPublicationSeq(Connection conn, String publisherName)
      throws SQLException, DbException {
    Long publicationSeq = null;
    PreparedStatement stmt = null;

    try {
      String query = "select p." + PUBLICATION_SEQ_COLUMN
	  + " from " + PUBLICATION_TABLE + " p"
	  + "," + PUBLISHER_TABLE + " pr"
	  + " where p." + PUBLISHER_SEQ_COLUMN + " = pr." + PUBLISHER_SEQ_COLUMN
	  + " and pr." + PUBLISHER_NAME_COLUMN + " like ?";

      stmt = dbManager.prepareStatement(conn, query);
      stmt.setString(1, publisherName);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      if (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
      }
    } finally {
      stmt.close();
    }

    return publicationSeq;
  }

  private void checkJournalTypeAggregates(Connection conn, String publisherName,
      boolean isPublisherInvolved, int year, int month, int totalRequests,
      int htmlRequests, int pdfRequests) throws SQLException, DbException {
    PreparedStatement stmt = null;

    try {
      String query = "select a.* from "
	  + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE + " a"
	  + "," + PUBLICATION_TABLE + " p"
	  + "," + PUBLISHER_TABLE + " pr"
	  + " where a." + PUBLICATION_SEQ_COLUMN
	  + " = p." + PUBLICATION_SEQ_COLUMN
	  + " and p." + PUBLISHER_SEQ_COLUMN + " = pr." + PUBLISHER_SEQ_COLUMN
	  + " and pr." + PUBLISHER_NAME_COLUMN + " like ?";
      stmt = dbManager.prepareStatement(conn, query);
      stmt.setString(1, publisherName);
      ResultSet resultSet = dbManager.executeQuery(stmt);

      assertEquals(true, resultSet.next());
      assertEquals(isPublisherInvolved,
	  resultSet.getBoolean(IS_PUBLISHER_INVOLVED_COLUMN));
    } finally {
      stmt.close();
    }
  }

  private void runNormalizeMetadataTest() throws Exception {
    // Set up the featured URL map.
    String validFeatureUrl = "UrlForValidFeature";
    String validFeature = "ValidFeature";
    String tooLongFeatureUrl = "UrlForTooLongFeatureName";

    StringBuilder sb = new StringBuilder("FeatureName");

    for (int i = 0; i < MAX_FEATURE_COLUMN; i++) {
      sb.append("1");
    }

    String tooLongFeature = sb.append("IsLongerThanMaximumLength").toString();

    Map<String, String> featuredUrls = new HashMap<String, String>();
    featuredUrls.put(validFeature, validFeatureUrl);
    featuredUrls.put(tooLongFeature, tooLongFeatureUrl);

    // The expected truncated feature name.
    String truncatedFeature =
	DbManager.truncateVarchar(tooLongFeature, MAX_FEATURE_COLUMN);
    assertNotEquals(tooLongFeature, truncatedFeature);

    // Generate the metadata.
    ArticleMetadataBuffer metadata =
	getJournalMetadata("Publisher", 1, 1, true, false, featuredUrls, false);

    Iterator<ArticleMetadataInfo> mditr = metadata.iterator();

    ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
	.getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

    AuMetadataRecorder amr =
	new AuMetadataRecorder(task, metadataManager, sau0);

    // Loop through the metadata for each article.
    while (mditr.hasNext()) {
      // Normalize all the metadata fields.
      ArticleMetadataInfo normalizedMdInfo =
	  amr.normalizeMetadata(mditr.next());

      for (String feature : normalizedMdInfo.featuredUrlMap.keySet()) {
        String url = normalizedMdInfo.featuredUrlMap.get(feature).trim();

        if (url.equals(validFeatureUrl)) {
          assertEquals(validFeature, feature);
        } else if (url.equals(tooLongFeatureUrl)) {
          assertEquals(truncatedFeature, feature);
        } else {
          fail("Unexpected URL: " + url);
        }
      }
    }
  }

  /**
   * Records an AU with multiple journals, each with a different publisher.
   * 
   * @throws Exception
   */
  private void runRecordMultipleJournalsAndPublishers() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

      // Get the initial number of publishers.
      int initialPublisherCount = countPublishers(conn);
      assertNotEquals(-1, initialPublisherCount);

      // Get the initial number of publications.
      int initialPublicationCount = countPublications(conn);
      assertNotEquals(-1, initialPublicationCount);
      
      // Get the initial number of unknown publication titles
      int initialUnknownTitlesCount = 
          countUnknownTitles(conn);

      // Get the initial number of archival units.
      int initialAuMdCount = countArchivalUnits(conn);
      assertNotEquals(-1, initialAuMdCount);

      // Get the initial number of articles.
      int initialArticleCount = countAuMetadataItems(conn);
      assertNotEquals(-1, initialArticleCount);

      // Each publication has its own publisher. 
      int nTitles = 4;
      int nArticles = 3;
      ArticleMetadataBuffer metadata = getJournalMetadata("Publisher", nTitles,
	  nArticles, false, false, null, true);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
                .getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
          .recordMetadata(conn, metadata.iterator());

      // Check that as many publishers as publications exist.
      assertEquals(nTitles, countPublishers(conn) - initialPublisherCount);

      // Check that nTitles publication exists.
      assertEquals(nTitles, countPublications(conn) - initialPublicationCount);

      // Check that no unknown publication title exist.
      assertEquals(0, countUnknownTitles(conn) - initialUnknownTitlesCount);

      // Check that 1 archival unit exists.
      assertEquals(1, countArchivalUnits(conn) - initialAuMdCount);

      // Check that nArticles articles for each of the nTitle titles
      assertEquals(nTitles*nArticles, 
                   countAuMetadataItems(conn) - initialArticleCount);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  private static class MySimulatedPlugin extends SimulatedPlugin {
    ArticleMetadataExtractor simulatedArticleMetadataExtractor = null;
    int version = 2;
    /**
     * Returns the article iterator factory for the mime type, if any
     * @param contentType the content type
     * @return the ArticleIteratorFactory
     */
    @Override
    public ArticleIteratorFactory getArticleIteratorFactory() {
      return new MySubTreeArticleIteratorFactory(null);
    }
    @Override
    public ArticleMetadataExtractor 
      getArticleMetadataExtractor(MetadataTarget target, ArchivalUnit au) {
      return simulatedArticleMetadataExtractor;
    }

    @Override
    public String getFeatureVersion(Plugin.Feature feat) {
      if (Feature.Metadata == feat) {
	return feat + "_" + version;
      } else {
	return null;
      }
    }
  }

  public static class MySimulatedPlugin0 extends MySimulatedPlugin {
    public MySimulatedPlugin0() {
      simulatedArticleMetadataExtractor = new ArticleMetadataExtractor() {
        public void extract(MetadataTarget target, ArticleFiles af,
            Emitter emitter) throws IOException, PluginException {
          ArticleMetadata md = new ArticleMetadata();
          emitter.emitMetadata(af, md);
        }
      };
    }
    public ExternalizableMap getDefinitionMap() {
      ExternalizableMap map = new ExternalizableMap();
      map.putString("au_start_url", "\"%splugin0/%s\", base_url, volume");
      return map;
    }
  }
}
