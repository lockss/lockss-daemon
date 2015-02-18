/*
 * $Id$
 */

/*

 Copyright (c) 2013-2014 Board of Trustees of Leland Stanford Jr. University,
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

import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import org.lockss.config.ConfigManager;
import org.lockss.config.Configuration;
import org.lockss.daemon.Cron;
import org.lockss.daemon.PluginException;
import org.lockss.db.DbManager;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.extractor.ArticleMetadata;
import org.lockss.extractor.ArticleMetadataExtractor;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.MetadataTarget;
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
import org.lockss.util.MetadataUtil;

/**
 * Test class for org.lockss.metadata.MetadataDatabaseUtil. 
 * Inspired by TestAuMetadataRecorder
 * 
 * @author Philip Gust
 */
public class TestMetadataDatabaseUtil extends LockssTestCase {
  static Logger log = Logger.getLogger(TestMetadataDatabaseUtil.class);

  private SimulatedArchivalUnit sau0;
  private MetadataManager metadataManager;
  private DbManager dbManager;

  public void setUp() throws Exception {
    super.setUp();
    String tempDirPath = setUpDiskSpace();

    ConfigurationUtil.addFromArgs(MetadataManager.PARAM_INDEXING_ENABLED,
	"true");

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

      // index without book titles to create unknown title entries
      int nTitles = 2;
      int nArticles=4;
      ArticleMetadataBuffer metadata = 
        getJournalMetadata("Publisher", nTitles, nArticles, false, false);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
                .getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
          .recordMetadata(conn, metadata.iterator());

      List<BibliographicItem> items = 
          MetadataDatabaseUtil.getBibliographicItems(dbManager,  conn);
      assertEquals(nTitles, items.size());
      
      BibliographicItem previousItem = null;

      for (BibliographicItem item : items) {
        assertEquals("journal", item.getPublicationType());
        assertEquals("Publisher", item.getPublisherName());
        assertEquals("fulltext", item.getCoverageDepth());
        assertNotNull(item.getStartYear());
        assertNotNull(item.getEndYear());
        assertEquals(item.getStartYear(), item.getEndYear());
        assertNull(item.getPrintIsbn());
        assertNull(item.getEisbn());
        assertNull(item.getIsbn());
        assertNotNull(item.getPublicationTitle());
        assertNotNull(item.getPrintIssn());
        assertNotNull(item.getEissn());
        assertNotNull(item.getIssn());
        assertNull(item.getStartIssue());
        assertNull(item.getEndIssue());
        assertNotNull(item.getStartVolume());
        assertNotNull(item.getEndVolume());
        assertEquals(item.getStartVolume(), item.getEndVolume());
        if (item.getProprietaryIds() != null
            && item.getProprietaryIds().length > 0) {
          assertNull(item.getProprietaryIds()[0]);
        }
        if (item.getProprietarySeriesIds() != null
            && item.getProprietarySeriesIds().length > 0) {
          assertNull(item.getProprietarySeriesIds()[0]);
        }
        assertNull(item.getSeriesTitle());
        assertEquals("Publisher", item.getProviderName());

        assertFalse(item.sameInNonProprietaryIdProperties(previousItem));
        previousItem = item;
      }
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  private ArticleMetadataBuffer getJournalMetadata(String publishername,
      int publicationCount, int articleCount, 
      boolean noJournalTitles, boolean noIssns)
      throws IOException {
    ArticleMetadataBuffer result = new ArticleMetadataBuffer(getTempDir());

    for (int i = 1; i <= publicationCount; i++) {
      for (int j = 1; j <= articleCount; j++) {
	ArticleMetadata am = new ArticleMetadata();
	am.put(MetadataField.FIELD_PUBLICATION_TYPE,
	       MetadataField.PUBLICATION_TYPE_JOURNAL);

	if (publishername != null) {
	  am.put(MetadataField.FIELD_PUBLISHER, publishername);
	}

	if (!noJournalTitles) {
	  am.put(MetadataField.FIELD_PUBLICATION_TITLE, "Journal Title" + i);
	}
	if (!noIssns) {
	  am.put(MetadataField.FIELD_ISSN, "1234-567" + i);
	  am.put(MetadataField.FIELD_EISSN, "4321-765" + i);
	}

	am.put(MetadataField.FIELD_VOLUME, Integer.toString(i));
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
   * Records a book.
   * 
   * @throws Exception
   */
  private void runRecordBook1() throws Exception {
    Connection conn = null;

    try {
      conn = dbManager.getConnection();

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

      List<BibliographicItem> items = 
          MetadataDatabaseUtil.getBibliographicItems(dbManager,  conn);
      assertEquals(nTitles, items.size());
      
      BibliographicItem previousItem = null;

      for (BibliographicItem item : items) {
        assertEquals("book", item.getPublicationType());
        assertEquals("Publisher", item.getPublisherName());
        assertEquals("fulltext", item.getCoverageDepth());
        assertNotNull(item.getStartYear());
        assertNotNull(item.getEndYear());
        assertEquals(item.getStartYear(), item.getEndYear());
        assertNotNull(item.getPrintIsbn());
        assertNotNull(item.getEisbn());
        assertNotNull(item.getIsbn());
        assertEquals(  "UNKNOWN_TITLE/isbn=" 
                     + MetadataUtil.toUnpunctuatedIsbn13(item.getPrintIsbn()),
                     item.getPublicationTitle());
        assertNull(item.getPrintIssn());
        assertNull(item.getEissn());
        assertNull(item.getIssn());
        assertNull(item.getStartIssue());
        assertNull(item.getEndIssue());
        assertNull(item.getStartVolume());
        assertNull(item.getEndVolume());
        if (item.getProprietaryIds() != null
            && item.getProprietaryIds().length > 0) {
          assertNull(item.getProprietaryIds()[0]);
        }
        if (item.getProprietarySeriesIds() != null
            && item.getProprietarySeriesIds().length > 0) {
          assertNull(item.getProprietarySeriesIds()[0]);
        }
        assertNull(item.getSeriesTitle());
        assertEquals("Publisher", item.getProviderName());

        assertFalse(item.sameInNonProprietaryIdProperties(previousItem));
        previousItem = item;
      }
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
	  am.put(MetadataField.FIELD_ISBN, "978012345678" + i);
	  am.put(MetadataField.FIELD_EISBN, "978987654321" + i);
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

      // index without book titles to create unknown title entries
      int nSeries = 2;
      int nTitles = 3;
      int nChapters=4;
      ArticleMetadataBuffer metadata = 
        getBookSeriesMetadata("Publisher", nSeries, nTitles, nChapters,
                              false, false, false, false);

      ReindexingTask task = newReindexingTask(sau0, sau0.getPlugin()
                .getArticleMetadataExtractor(MetadataTarget.OpenURL(), sau0));

      // Write the AU metadata to the database.
      new AuMetadataRecorder(task, metadataManager, sau0)
          .recordMetadata(conn, metadata.iterator());

      List<BibliographicItem> items = 
          MetadataDatabaseUtil.getBibliographicItems(dbManager,  conn);
      assertEquals(nSeries*nTitles, items.size());
      
      BibliographicItem previousItem = null;

      for (BibliographicItem item : items) {
        assertEquals("bookSeries", item.getPublicationType());
        assertEquals("Publisher", item.getPublisherName());
        assertEquals("fulltext", item.getCoverageDepth());
        assertNotNull(item.getStartYear());
        assertNotNull(item.getEndYear());
        assertEquals(item.getStartYear(), item.getEndYear());
        assertNotNull(item.getPrintIsbn());
        assertNotNull(item.getEisbn());
        assertNotNull(item.getIsbn());
        assertNotNull(item.getPublicationTitle());
        assertNotNull(item.getPrintIssn());
        assertNotNull(item.getEissn());
        assertNotNull(item.getIssn());
        assertNull(item.getStartIssue());
        assertNull(item.getEndIssue());
        assertNotNull(item.getStartVolume());
        assertNotNull(item.getEndVolume());
        assertEquals(item.getStartVolume(), item.getEndVolume());
        if (item.getProprietaryIds() != null
            && item.getProprietaryIds().length > 0) {
          assertNull(item.getProprietaryIds()[0]);
        }
        if (item.getProprietarySeriesIds() != null
            && item.getProprietarySeriesIds().length > 0) {
          assertNull(item.getProprietarySeriesIds()[0]);
        }
        assertNotNull(item.getSeriesTitle());
        assertEquals("Publisher", item.getProviderName());

        assertFalse(item.sameInNonProprietaryIdProperties(previousItem));
        previousItem = item;
      }
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
  }

  private ArticleMetadataBuffer getBookSeriesMetadata(String publishername,
      int seriesCount, int publicationCount, int chapterCount,
      boolean noSeriesTitles, boolean noIssns, 
      boolean noBookTitles, boolean noIsbns) throws IOException {
    ArticleMetadataBuffer result = new ArticleMetadataBuffer(getTempDir());

    for (int sc = 1; sc <= seriesCount; sc++) {
      for (int pc = 1; pc <= publicationCount; pc++) {
        for (int ac = 1; ac <= chapterCount; ac++) {
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
            am.put(MetadataField.FIELD_ISBN, "97802468024" + sc + pc);
            am.put(MetadataField.FIELD_EISBN, "97886430864" + sc + pc);
          }
          
          am.put(MetadataField.FIELD_VOLUME, Integer.toString(10+pc));
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
