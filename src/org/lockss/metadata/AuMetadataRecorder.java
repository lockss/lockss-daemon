/*
 * $Id: AuMetadataRecorder.java,v 1.13 2013-08-20 16:30:38 fergaloy-sf Exp $
 */

/*

 Copyright (c) 2013 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.db.DbManager.*;
import static org.lockss.metadata.MetadataManager.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lockss.app.LockssDaemon;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.exporter.counter.CounterReportsManager;
import org.lockss.metadata.ArticleMetadataBuffer.ArticleMetadataInfo;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.Plugin;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;

/**
 * Writes to the database metadata related to an archival unit.
 */
public class AuMetadataRecorder {
  private static Logger log = Logger.getLogger(AuMetadataRecorder.class);

  static final String UNKNOWN_PUBLISHER_AU_PROBLEM = "UNKNOWN_PUBLISHER";

  // Query to update the version of an Archival Unit metadata.
  private static final String UPDATE_AU_MD_QUERY = "update "
      + AU_MD_TABLE
      + " set " + MD_VERSION_COLUMN + " = ?"
      + " where " + AU_MD_SEQ_COLUMN + " = ?";

  // Query to find the name of the type of a metadata item.
  private static final String GET_MD_ITEM_TYPE_NAME_QUERY = "select "
      + "t." + TYPE_NAME_COLUMN
      + " from " + MD_ITEM_TYPE_TABLE + " t"
      + "," + MD_ITEM_TABLE + " m"
      + " where m." + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and m." + MD_ITEM_TYPE_SEQ_COLUMN + " = t." + MD_ITEM_TYPE_SEQ_COLUMN;

  // Query to find the DOIs of a metadata item.
  private static final String FIND_MD_ITEM_DOI_QUERY = "select "
      + DOI_COLUMN
      + " from " + DOI_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to add a bibliographic item.
  private static final String INSERT_BIB_ITEM_QUERY = "insert into "
      + BIB_ITEM_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + VOLUME_COLUMN
      + "," + ISSUE_COLUMN
      + "," + START_PAGE_COLUMN
      + "," + END_PAGE_COLUMN
      + "," + ITEM_NO_COLUMN
      + ") values (?,?,?,?,?,?)";

  // Query to update a bibliographic item.
  private static final String UPDATE_BIB_ITEM_QUERY = "update "
      + BIB_ITEM_TABLE
      + " set " + VOLUME_COLUMN + " = ?"
      + "," + ISSUE_COLUMN + " = ?"
      + "," + START_PAGE_COLUMN + " = ?"
      + "," + END_PAGE_COLUMN + " = ?"
      + "," + ITEM_NO_COLUMN + " = ?"
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to delete the metadata item of a publication created for an unknown
  // publisher.
  private static final String
      DELETE_UNKNOWN_PUBLISHER_PUBLICATION_MD_ITEM_QUERY = "delete from "
      + MD_ITEM_TABLE
      + " where "
      + MD_ITEM_SEQ_COLUMN + " in ("
      + "select " + MD_ITEM_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE
      + " where "
      + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and (select count(*) from "
      + PUBLISHER_TABLE + " pr"
      + "," + PUBLICATION_TABLE + " p"
      + " where pr." + PUBLISHER_SEQ_COLUMN + " = p." + PUBLISHER_SEQ_COLUMN
      + " and p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and pr." + PUBLISHER_NAME_COLUMN
      + " like '" + UNKNOWN_PUBLISHER_AU_PROBLEM + "%') = 1)";

  // Query to delete a publication created for an unknown publisher.
  private static final String DELETE_UNKNOWN_PUBLISHER_PUBLICATION_QUERY =
      "delete from " + PUBLICATION_TABLE
      + " where "
      + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and (select count(*) from "
      + PUBLISHER_TABLE + " pr"
      + "," + PUBLICATION_TABLE + " p"
      + " where pr." + PUBLISHER_SEQ_COLUMN + " = p." + PUBLISHER_SEQ_COLUMN
      + " and p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and pr." + PUBLISHER_NAME_COLUMN
      + " like '" + UNKNOWN_PUBLISHER_AU_PROBLEM + "%') = 1";

  // Query to delete an unknown publisher.
  private static final String DELETE_UNKNOWN_PUBLISHER_QUERY =
      "delete from " + PUBLISHER_TABLE
      + " where "
      + PUBLISHER_NAME_COLUMN + " = ?"
      + " and " + PUBLISHER_NAME_COLUMN
      + " like '" + UNKNOWN_PUBLISHER_AU_PROBLEM + "%'";

  // The calling task.
  private final ReindexingTask task;

  // The metadata manager.
  private final MetadataManager mdManager;

  // The database manager.
  private final DbManager dbManager;

  // The archival unit.
  private final ArchivalUnit au;

  // AU-related properties independent of the database.
  private final Plugin plugin;
  private final String platform;
  private final int pluginVersion;
  private final String auId;
  private final String auKey;
  private final String pluginId;

  // Database identifiers related to the AU. 
  private Long publisherSeq = null;
  private Long publicationSeq = null;
  private Long pluginSeq = null;
  private Long auSeq = null;
  private Long auMdSeq = null;
  private Long parentSeq = null;

  // Properties used to take shortcuts in processing.
  private String journalTitle = null;
  private String pIsbn = null;
  private String eIsbn = null;
  private String pIssn = null;
  private String eIssn = null;
  private String proprietaryId = null;
  private String volume = null;
  private String parentMdItemType = null;
  private boolean newAu = false;
  private String publisherName;

  /**
   * Constructor.
   * 
   * @param task A ReindexingTaskwith the calling task.
   * @param mdManager A MetadataManager with the metadata manager.
   * @param au An ArchivalUnit with the archival unit.
   */
  public AuMetadataRecorder(ReindexingTask task, MetadataManager mdManager,
      ArchivalUnit au) {
    this.task = task;
    this.mdManager = mdManager;
    dbManager = mdManager.getDbManager();
    this.au = au;

    plugin = au.getPlugin();
    platform = plugin.getPublishingPlatform();
    pluginVersion = mdManager.getPluginMetadataVersionNumber(plugin);
    auId = au.getAuId();
    auKey = PluginManager.auKeyFromAuId(auId);
    pluginId = PluginManager.pluginIdFromAuId(auId);
  }

  /**
   * Writes to the database metadata related to an archival unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mditr
   *          An Iterator<ArticleMetadataInfo> with the metadata.
   * @throws MetadataException
   *           if any problem is detected with the passed metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public void recordMetadata(Connection conn,
      Iterator<ArticleMetadataInfo> mditr) throws MetadataException,
      DbException {
    final String DEBUG_HEADER = "recordMetadata(): ";

    // Loop through the metadata for each article.
    while (mditr.hasNext()) {
      task.pokeWDog();

      // Normalize all the metadata fields.
      ArticleMetadataInfo normalizedMdInfo = normalizeMetadata(mditr.next());

      // Store the metadata fields in the database.
      storeMetadata(conn, normalizedMdInfo);

      // Count the processed article.
      task.incrementUpdatedArticleCount();
      log.debug3(DEBUG_HEADER + "updatedArticleCount = "
	  + task.getUpdatedArticleCount());
    }

    if (auMdSeq != null) {
      // Update the AU last extraction timestamp.
      mdManager.updateAuLastExtractionTime(conn, auMdSeq);
    } else {
      log.warning("auMdSeq is null for auid = '" + au.getAuId() + "'.");
    }

    // Find the list of previous problems indexing this Archival Unit.
    List<String> problems = mdManager.findAuProblems(conn, auId);
    log.debug3(DEBUG_HEADER + "problems.size() = " + problems.size());

    // Check whether the publisher name used is a synthetic name.
    if (publisherName.startsWith(UNKNOWN_PUBLISHER_AU_PROBLEM)) {
      // Yes: Check whether an entry in the AU problem table does not exist.
      if (!problems.contains(publisherName)) {
	// Yes: Add an unknown publisher entry to the AU problem table.
	mdManager.addAuProblem(conn, auId, publisherName);
      }
    } else {
      // No: Check whether there is data obtained when the publisher was unknown
      // that needs to be merged.
      if (problems.size() > 0) {
	// Yes: Merge it.
	fixUnknownPublishersAuData(conn, problems);
      }
    }
  }

  /**
   * Normalizes metadata info fields.
   * 
   * @param mdinfo
   *          the ArticleMetadataInfo
   * @return an ArticleMetadataInfo with the normalized properties.
   */
  private ArticleMetadataInfo normalizeMetadata(ArticleMetadataInfo mdinfo) {
    if (mdinfo.accessUrl != null) {
      if (mdinfo.accessUrl.length() > MAX_URL_COLUMN) {
	log.warning("accessUrl too long '" + mdinfo.accessUrl
	    + "' for title: '" + mdinfo.journalTitle + "' publisher: "
	    + mdinfo.publisher + "'");
	mdinfo.accessUrl =
	    DbManager.truncateVarchar(mdinfo.accessUrl, MAX_URL_COLUMN);
      }
    }

    mdinfo.isbn = mdManager.normalizeIsbnOrIssn(mdinfo.isbn, MAX_ISBN_COLUMN,
	"ISBN", mdinfo.journalTitle, mdinfo.publisher);

    mdinfo.eisbn = mdManager.normalizeIsbnOrIssn(mdinfo.eisbn, MAX_ISBN_COLUMN,
	"ISBN", mdinfo.journalTitle, mdinfo.publisher);

    mdinfo.issn = mdManager.normalizeIsbnOrIssn(mdinfo.issn, MAX_ISSN_COLUMN,
	"ISSN", mdinfo.journalTitle, mdinfo.publisher);

    mdinfo.eissn = mdManager.normalizeIsbnOrIssn(mdinfo.eissn, MAX_ISSN_COLUMN,
	"ISSN", mdinfo.journalTitle, mdinfo.publisher);

    if (mdinfo.doi != null) {
      String doi = mdinfo.doi;
      if (StringUtil.startsWithIgnoreCase(doi, "doi:")) {
	doi = doi.substring("doi:".length());
	log.debug3("doi = '" + doi + "'.");
      }

      if (doi.length() > MAX_DOI_COLUMN) {
	log.warning("doi too long '" + mdinfo.doi + "' for title: '"
	    + mdinfo.journalTitle + "' publisher: " + mdinfo.publisher + "'");
	mdinfo.doi = DbManager.truncateVarchar(doi, MAX_DOI_COLUMN);
      } else {
	mdinfo.doi = doi;
      }
    }

    if (mdinfo.pubDate != null) {
      if (mdinfo.pubDate.length() > MAX_DATE_COLUMN) {
	log.warning("pubDate too long '" + mdinfo.pubDate + "' for title: '"
	    + mdinfo.journalTitle + "' publisher: " + mdinfo.publisher + "'");
	mdinfo.pubDate =
	    DbManager.truncateVarchar(mdinfo.pubDate, MAX_DATE_COLUMN);
      }
    }

    if (mdinfo.volume != null) {
      if (mdinfo.volume.length() > MAX_VOLUME_COLUMN) {
	log.warning("volume too long '" + mdinfo.pubDate + "' for title: '"
	    + mdinfo.journalTitle + "' publisher: " + mdinfo.publisher + "'");
	mdinfo.volume =
	    DbManager.truncateVarchar(mdinfo.volume, MAX_VOLUME_COLUMN);
      }
    }

    if (mdinfo.issue != null) {
      if (mdinfo.issue.length() > MAX_ISSUE_COLUMN) {
	log.warning("issue too long '" + mdinfo.issue + "' for title: '"
	    + mdinfo.journalTitle + "' publisher: " + mdinfo.publisher + "'");
	mdinfo.issue =
	    DbManager.truncateVarchar(mdinfo.issue, MAX_ISSUE_COLUMN);
      }
    }

    if (mdinfo.startPage != null) {
      if (mdinfo.startPage.length() > MAX_START_PAGE_COLUMN) {
	log.warning("startPage too long '" + mdinfo.startPage
	    + "' for title: '" + mdinfo.journalTitle + "' publisher: "
	    + mdinfo.publisher + "'");
	mdinfo.startPage =
	    DbManager.truncateVarchar(mdinfo.startPage, MAX_START_PAGE_COLUMN);
      }
    }

    if (mdinfo.articleTitle != null) {
      if (mdinfo.articleTitle.length() > MAX_NAME_COLUMN) {
	log.warning("article title too long '" + mdinfo.articleTitle
	    + "' for title: '" + mdinfo.journalTitle + "' publisher: "
	    + mdinfo.publisher + "'");
	mdinfo.articleTitle =
	    DbManager.truncateVarchar(mdinfo.articleTitle, MAX_NAME_COLUMN);
      }
    }

    if (mdinfo.publisher != null) {
      if (mdinfo.publisher.length() > MAX_NAME_COLUMN) {
	log.warning("publisher too long '" + mdinfo.publisher
	    + "' for title: '" + mdinfo.journalTitle + "'");
	mdinfo.publisher =
	    DbManager.truncateVarchar(mdinfo.publisher, MAX_NAME_COLUMN);
      }
    }

    if (mdinfo.journalTitle != null) {
      if (mdinfo.journalTitle.length() > MAX_NAME_COLUMN) {
	log.warning("journal title too long '" + mdinfo.journalTitle
	    + "' for publisher: " + mdinfo.publisher + "'");
	mdinfo.journalTitle =
	    DbManager.truncateVarchar(mdinfo.journalTitle, MAX_NAME_COLUMN);
      }
    }

    if (mdinfo.authorSet != null) {
      Set<String> authors = new HashSet<String>();
      for (String author : mdinfo.authorSet) {
	if (author.length() > MAX_AUTHOR_COLUMN) {
	  log.warning("author too long '" + author + "' for title: '"
	      + mdinfo.journalTitle + "' publisher: " + mdinfo.publisher + "'");
	  authors.add(DbManager.truncateVarchar(author, MAX_AUTHOR_COLUMN));
	} else {
	  authors.add(author);
	}
      }
      mdinfo.authorSet = authors;
    }

    if (mdinfo.keywordSet != null) {
      Set<String> keywords = new HashSet<String>();
      for (String keyword : mdinfo.keywordSet) {
	if (keyword.length() > MAX_KEYWORD_COLUMN) {
	  log.warning("keyword too long '" + keyword + "' for title: '"
	      + mdinfo.journalTitle + "' publisher: " + mdinfo.publisher + "'");
	  keywords.add(DbManager.truncateVarchar(keyword, MAX_KEYWORD_COLUMN));
	} else {
	  keywords.add(keyword);
	}
      }
      mdinfo.keywordSet = keywords;
    }

    if (mdinfo.featuredUrlMap != null) {
      Map<String, String> featuredUrls = new HashMap<String, String>();
      for (String key : mdinfo.featuredUrlMap.keySet()) {
	if (mdinfo.featuredUrlMap.get(key).length() > MAX_URL_COLUMN) {
	  log.warning("URL too long '" + mdinfo.featuredUrlMap.get(key)
	      + "' for title: '" + mdinfo.journalTitle + "' publisher: "
	      + mdinfo.publisher + "'");
	  featuredUrls.put(key,
	                   DbManager.truncateVarchar(mdinfo.featuredUrlMap.
	                                             get(key), MAX_URL_COLUMN));
	} else {
	  featuredUrls.put(key, mdinfo.featuredUrlMap.get(key));
	}
      }
      mdinfo.featuredUrlMap = featuredUrls;
    }

    if (mdinfo.endPage != null) {
      if (mdinfo.endPage.length() > MAX_END_PAGE_COLUMN) {
	log.warning("endPage too long '" + mdinfo.endPage + "' for title: '"
	    + mdinfo.journalTitle + "' publisher: " + mdinfo.publisher + "'");
	mdinfo.endPage =
	    DbManager.truncateVarchar(mdinfo.endPage, MAX_END_PAGE_COLUMN);
      }
    }

    if (mdinfo.coverage != null) {
      if (mdinfo.coverage.length() > MAX_COVERAGE_COLUMN) {
	log.warning("coverage too long '" + mdinfo.coverage + "' for title: '"
	    + mdinfo.journalTitle + "' publisher: " + mdinfo.publisher + "'");
	mdinfo.coverage =
	    DbManager.truncateVarchar(mdinfo.coverage, MAX_COVERAGE_COLUMN);
      }
    } else {
	mdinfo.coverage = "fulltext";
    }

    if (mdinfo.itemNumber != null) {
      if (mdinfo.itemNumber.length() > MAX_ITEM_NO_COLUMN) {
	log.warning("itemNumber too long '" + mdinfo.itemNumber
	    + "' for title: '" + mdinfo.journalTitle + "' publisher: "
	    + mdinfo.publisher + "'");
	mdinfo.itemNumber =
	    DbManager.truncateVarchar(mdinfo.itemNumber, MAX_ITEM_NO_COLUMN);
      }
    }

    if (mdinfo.proprietaryIdentifier != null) {
      if (mdinfo.proprietaryIdentifier.length() > MAX_PUBLICATION_ID_COLUMN) {
	log.warning("proprietaryIdentifier too long '"
	    + mdinfo.proprietaryIdentifier + "' for title: '"
	    + mdinfo.journalTitle + "' publisher: " + mdinfo.publisher + "'");
	mdinfo.proprietaryIdentifier =
	    DbManager.truncateVarchar(mdinfo.proprietaryIdentifier,
				      MAX_PUBLICATION_ID_COLUMN);
      }
    }

    return mdinfo;
  }

  /**
   * Stores in the database metadata for the Archival Unit.
   * 
   * @param conn
   *          A Connection with the connection to the database
   * @param mdinfo
   *          An ArticleMetadataInfo providing the metadata.
   * @throws MetadataException
   *           if any problem is detected with the passed metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void storeMetadata(Connection conn, ArticleMetadataInfo mdinfo)
      throws MetadataException, DbException {
    final String DEBUG_HEADER = "storeMetadata(): ";
    if (log.isDebug3()) {
      log.debug3(DEBUG_HEADER + "Starting: auId = " + auId);
      log.debug3(DEBUG_HEADER + "auKey = " + auKey);
      log.debug3(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
      log.debug3(DEBUG_HEADER + "mdinfo.articleTitle = " + mdinfo.articleTitle);
    }

    // Check whether the publisher has not been located in the database.
    if (publisherSeq == null) {
      // Yes: Get the publisher received in the metadata.
      publisherName = mdinfo.publisher;
      log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

      // Check whether the publisher is in the metadata.
      if (publisherName != null) {
	// Yes: Find the publisher or create it.
	publisherSeq = mdManager.findOrCreatePublisher(conn, publisherName);
	log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      } else {
	// No: Find the AU in the database.
	auSeq = mdManager.findAuByAuId(conn, auId);
	log.debug3(DEBUG_HEADER + "auSeq = " + auSeq);

	// Check whether the AU was found.
	if (auSeq != null) {
	  // Yes: Get the publisher of the AU.
	  publisherSeq = mdManager.findAuPublisher(conn, auSeq);
	  log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);

	  // Check whether the AU publisher was found.
	  if (publisherSeq != null) {
	    // Yes: Get its name.
	    publisherName = mdManager.getPublisherName(conn, publisherSeq);
	    log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);
	  } else {
	    // No: Report the problem.
	    log.error("Null publisherSeq for auSeq = " + auSeq);
	    log.error("auId = " + auId);
	    log.error("auKey = " + auKey);
	    log.error("auMdSeq = " + auMdSeq);
	    log.error("auSeq = " + auSeq);
	    throw new MetadataException("Null publisherSeq for auSeq = "
		+ auSeq, mdinfo);
	  }
	} else {
	  // No: Loop through all outstanding previous problems for this AU.
	  for (String problem : mdManager.findAuProblems(conn, auId)) {
	    // Check whether there is an unknown publisher already for this AU.
	    if (problem.startsWith(UNKNOWN_PUBLISHER_AU_PROBLEM)) {
	      // Yes: Get the corresponding publisher identifier.
	      publisherSeq = mdManager.findPublisher(conn, problem);
	      log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);

	      // Check whether the publisher exists.
	      if (publisherSeq != null) {
		// Yes: Use it.
		publisherName = problem;
		break;
	      } else {
		// No: Remove the obsolete problem.
		mdManager.removeAuProblem(conn, auId, problem);
	      }
	    }
	  }

	  // Check whether no previous unknown publisher for this AU exists.
	  if (publisherName == null) {
	    // Yes: Create a synthetic publisher name to be able to process the
	    // Archival Unit.
	    publisherName = UNKNOWN_PUBLISHER_AU_PROBLEM + TimeBase.nowMs();
	    log.debug3(DEBUG_HEADER + "publisherName = " + publisherName);

	    // Create the publisher.
	    publisherSeq = mdManager.addPublisher(conn, publisherName);
	    log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
	  }
	}
      }
    }

    // Check whether this is a new publication.
    if (publicationSeq == null || !isSamePublication(mdinfo)) {
      // Yes.
      log.debug3(DEBUG_HEADER + "is new publication.");

      // Get the journal title received in the metadata.
      journalTitle = mdinfo.journalTitle;
      log.debug3(DEBUG_HEADER + "journalTitle = " + journalTitle);

      // Check whether no name was received in the metadata.
      if (StringUtil.isNullString(journalTitle)) {
	// Yes: Synthesize a name.
	journalTitle = synthesizePublicationTitle(mdinfo);
      }

      // Get any ISBN values received in the metadata.
      pIsbn = mdinfo.isbn;
      log.debug3(DEBUG_HEADER + "pIsbn = " + pIsbn);

      eIsbn = mdinfo.eisbn;
      log.debug3(DEBUG_HEADER + "eIsbn = " + eIsbn);

      // Get any ISSN values received in the metadata.
      pIssn = mdinfo.issn;
      log.debug3(DEBUG_HEADER + "pIssn = " + pIssn);

      eIssn = mdinfo.eissn;
      log.debug3(DEBUG_HEADER + "eIssn = " + eIssn);

      // Get the proprietary identifier received in the metadata.
      proprietaryId = mdinfo.proprietaryIdentifier;
      log.debug3(DEBUG_HEADER + "proprietaryId = " + proprietaryId);

      // Get the volume received in the metadata.
      volume = mdinfo.volume;
      log.debug3(DEBUG_HEADER + "volume = " + volume);

      // Get the publication to which this metadata belongs.
      publicationSeq = mdManager.findOrCreatePublication(conn, pIssn, eIssn,
	  pIsbn, eIsbn, publisherSeq, journalTitle, proprietaryId, volume);
      log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

      // Get the identifier of the parent, which is the publication metadata
      // item.
      parentSeq = mdManager.findPublicationMetadataItem(conn, publicationSeq);
      log.debug3(DEBUG_HEADER + "parentSeq = " + parentSeq);

      // Find the publication names.
      Map<String, String> names = mdManager.getMdItemNames(conn, parentSeq);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "names.size() = " + names.size());

      // Loop through each publication name.
      for (Map.Entry<String, String> entry : names.entrySet()) {
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "entry = " + entry);

	// Check whether this is the primary name.
	if (entry.getValue().equals(PRIMARY_NAME_TYPE)) {
	  // Yes: Check whether the publication name has been synthesized.
	  if (journalTitle.startsWith(UNKNOWN_TITLE_NAME_ROOT)) {
	    // Yes: Check whether this is not a synthesized name.
	    if (!entry.getKey().startsWith(UNKNOWN_TITLE_NAME_ROOT)) {
	      // Yes: Remove any synthesized names.
	      mdManager.removeNotPrimarySynthesizedMdItemNames(conn, parentSeq);

	      // Use the primary name instead of the synthesized name.
	      journalTitle = entry.getKey();
	    }
	  } else {
	    // No: Check whether this is a synthesized name.
	    if (entry.getKey().startsWith(UNKNOWN_TITLE_NAME_ROOT)) {
	      // Yes: Update the synthesized primary name with the current one.
	      mdManager.updatePrimarySynthesizedMdItemName(conn, parentSeq,
		  journalTitle);

	      // Remove the previously entered non-primary name for this
	      // publication.
	      mdManager.removeNotPrimaryMdItemName(conn, parentSeq,
		  journalTitle);
	    }
	  }

	  break;
	}
      }

      if (!journalTitle.startsWith(UNKNOWN_TITLE_NAME_ROOT)) {
	// Remove any previously synthesized names for this publication.
	mdManager.removeNotPrimarySynthesizedMdItemNames(conn, parentSeq);
      }

      // Get the type of the parent.
      parentMdItemType = getMdItemTypeName(conn, parentSeq);
      log.debug3(DEBUG_HEADER + "parentMdItemType = " + parentMdItemType);
    }

    // Skip it if the publication could not be found or created.
    if (publicationSeq == null || parentSeq == null ||
	parentMdItemType == null) {
      log.debug3(DEBUG_HEADER
	  + "Done: publicationSeq or parentSeq or parentMdItemType is null.");
      return;
    }

    // Check whether the plugin has not been located in the database.
    if (pluginSeq == null) {
      // Yes: Find the publishing platform or create it.
      Long platformSeq = mdManager.findOrCreatePlatform(conn, platform);
      log.debug3(DEBUG_HEADER + "platformSeq = " + platformSeq);

      // Find the plugin or create it.
      pluginSeq = mdManager.findOrCreatePlugin(conn, pluginId, platformSeq);
      log.debug3(DEBUG_HEADER + "pluginSeq = " + pluginSeq);

      // Skip it if the plugin could not be found or created.
      if (pluginSeq == null) {
        log.debug3(DEBUG_HEADER + "Done: pluginSeq is null.");
        return;
      }
    }

    // Check whether the Archival Unit has not been located in the database.
    if (auSeq == null) {
      // Yes: Find it or create it.
      auSeq = mdManager.findOrCreateAu(conn, pluginSeq, auKey);
      log.debug3(DEBUG_HEADER + "auSeq = " + auSeq);

      // Skip it if the Archival Unit could not be found or created.
      if (auSeq == null) {
        log.debug3(DEBUG_HEADER + "Done: auSeq is null.");
        return;
      }
    }

    // Check whether the Archival Unit metadata has not been located in the
    // database.
    if (auMdSeq == null) {
      // Yes: Find the Archival Unit metadata in the database.
      auMdSeq = mdManager.findAuMd(conn, auSeq);
      log.debug3(DEBUG_HEADER + "new auMdSeq = " + auMdSeq);
    }

    // Check whether it is a new Archival Unit metadata.
    if (auMdSeq == null) {
      // Yes: Add to the database the new Archival Unit metadata.
      auMdSeq = mdManager.addAuMd(conn, auSeq, pluginVersion,
                                  NEVER_EXTRACTED_EXTRACTION_TIME);
      log.debug3(DEBUG_HEADER + "new auSeq = " + auMdSeq);

      // Skip it if the new Archival Unit metadata could not be created.
      if (auMdSeq == null) {
	log.debug3(DEBUG_HEADER + "Done: auMdSeq is null.");
	return;
      }

      newAu = true;
    } else {
      // No: Update the Archival Unit metadata ancillary data.
      updateAuMd(conn, auMdSeq, pluginVersion);
      log.debug3(DEBUG_HEADER + "updated AU.");
    }

    // Update or create the metadata item.
    updateOrCreateMdItem(conn, mdinfo);

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a synthetic publication title using the available metadata.
   * 
   * @param mdinfo
   *          An ArticleMetadataInfo providing the metadata.
   * @return a String with the synthetic publication title.
   */
  private String synthesizePublicationTitle(ArticleMetadataInfo mdinfo) {
    final String DEBUG_HEADER = "synthesizePublicationTitle(): ";
    String result = null;

    // Check whether the metadata included the ISBN.
    if (!StringUtil.isNullString(mdinfo.isbn)) {
      // Yes: Use it.
      result = UNKNOWN_TITLE_NAME_ROOT + "/isbn=" + mdinfo.isbn;
      // No: Check whether the metadata included the eISBN.
    } else if (!StringUtil.isNullString(mdinfo.eisbn)) {
      // Yes: Use it.
      result = UNKNOWN_TITLE_NAME_ROOT + "/eisbn=" + mdinfo.eisbn;
      // No: Check whether the metadata included the ISSN.
    } else if (!StringUtil.isNullString(mdinfo.issn)) {
      // Yes: Use it.
      result = UNKNOWN_TITLE_NAME_ROOT + "/issn=" + mdinfo.issn;
      // No: Check whether the metadata included the eISSN.
    } else if (!StringUtil.isNullString(mdinfo.eissn)) {
      // Yes: Use it.
      result = UNKNOWN_TITLE_NAME_ROOT + "/eissn=" + mdinfo.eissn;
      // No: Check whether the metadata included the proprietary identifier.
    } else if (!StringUtil.isNullString(mdinfo.proprietaryIdentifier)) {
      // Yes: Use it.
      result = UNKNOWN_TITLE_NAME_ROOT + "/journalId="
	  + mdinfo.proprietaryIdentifier;
    } else {
      // No: Generate a random name.
      result = UNKNOWN_TITLE_NAME_ROOT + "/id=" +  + TimeBase.nowMs();
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Updates the metadata version an Archival Unit in the database.
   * 
   * @param conn
   *          A Connection with the connection to the database
   * @param auMdSeq
   *          A Long with the identifier of the archival unit metadata.
   * @param version
   *          A String with the archival unit metadata version.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void updateAuMd(Connection conn, Long auMdSeq, int version)
      throws DbException {
    final String DEBUG_HEADER = "updateAuMd(): ";
    try {
      PreparedStatement updateAu = dbManager.prepareStatement(conn,
	  UPDATE_AU_MD_QUERY);

      try {
	updateAu.setShort(1, (short) version);
	updateAu.setLong(2, auMdSeq);
	int count = dbManager.executeUpdate(updateAu);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Updated auMdSeq = " + auMdSeq);
	}
      } finally {
	updateAu.close();
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot update AU metadata version", sqle);
    }
  }

  /**
   * Updates a metadata item if it exists in the database, otherwise it creates
   * it.
   * 
   * @param conn
   *          A Connection with the connection to the database
   * @param mdinfo
   *          An ArticleMetadataInfo providing the metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void updateOrCreateMdItem(Connection conn, ArticleMetadataInfo mdinfo)
      throws DbException {
    final String DEBUG_HEADER = "updateOrCreateMdItem(): ";

    // Get the publication date received in the metadata.
    String date = mdinfo.pubDate;
    log.debug3(DEBUG_HEADER + "date = " + date);

    // Get the issue received in the metadata.
    String issue = mdinfo.issue;
    log.debug3(DEBUG_HEADER + "issue = " + issue);

    // Get the start page received in the metadata.
    String startPage = mdinfo.startPage;
    log.debug3(DEBUG_HEADER + "startPage = " + startPage);

    // Get the end page received in the metadata.
    String endPage = mdinfo.endPage;
    log.debug3(DEBUG_HEADER + "endPage = " + endPage);

    // Get the item number received in the metadata.
    String itemNo = mdinfo.itemNumber;
    log.debug3(DEBUG_HEADER + "itemNo = " + itemNo);

    // Get the item title received in the metadata.
    String itemTitle = mdinfo.articleTitle;
    log.debug3(DEBUG_HEADER + "itemTitle = " + itemTitle);

    // Get the coverage received in the metadata.
    String coverage = mdinfo.coverage;
    log.debug3(DEBUG_HEADER + "coverage = " + coverage);

    // Get the DOI received in the metadata.
    String doi = mdinfo.doi;
    log.debug3(DEBUG_HEADER + "doi = " + doi);

    // Get the featured URLs received in the metadata.
    Map<String, String> featuredUrlMap = mdinfo.featuredUrlMap;

    if (log.isDebug3()) {
      for (String feature : featuredUrlMap.keySet()) {
	log.debug3(DEBUG_HEADER + "feature = " + feature + ", URL = "
	    + featuredUrlMap.get(feature));
      }
    }

    // Get the access URL received in the metadata.
    String accessUrl = mdinfo.accessUrl;
    log.debug3(DEBUG_HEADER + "accessUrl = " + accessUrl);

    // Determine what type of a metadata item it is.
    String mdItemType = null;
    if (MD_ITEM_TYPE_BOOK.equals(parentMdItemType)) {
      if (StringUtil.isNullString(startPage)
	  && StringUtil.isNullString(endPage)
	  && StringUtil.isNullString(itemNo)) {
	mdItemType = MD_ITEM_TYPE_BOOK;
      } else {
	mdItemType = MD_ITEM_TYPE_BOOK_CHAPTER;
      }
    } else if (MD_ITEM_TYPE_JOURNAL.equals(parentMdItemType)) {
      mdItemType = MD_ITEM_TYPE_JOURNAL_ARTICLE;
    } else {
      // Skip it if the parent type is not a book or journal.
      log.error(DEBUG_HEADER + "Unknown parentMdItemType = "
	  + parentMdItemType);
      return;
    }

    log.debug3(DEBUG_HEADER + "mdItemType = " + mdItemType);

    // Find the metadata item type record sequence.
    Long mdItemTypeSeq = mdManager.findMetadataItemType(conn, mdItemType);
    log.debug3(DEBUG_HEADER + "mdItemTypeSeq = " + mdItemTypeSeq);

    Long mdItemSeq = null;
    boolean newMdItem = false;

    // Check whether it is a metadata item for a new Archival Unit.
    if (newAu) {
      // Yes: Create the new metadata item in the database.
      mdItemSeq = mdManager.addMdItem(conn, parentSeq, mdItemTypeSeq, auMdSeq,
	  date, coverage);
      log.debug3(DEBUG_HEADER + "new mdItemSeq = " + mdItemSeq);

      mdManager.addMdItemName(conn, mdItemSeq, itemTitle, PRIMARY_NAME_TYPE);

      newMdItem = true;
    } else {
      // No: Find the metadata item in the database.
      mdItemSeq = mdManager.findMdItem(conn, mdItemTypeSeq, auMdSeq, accessUrl);
      log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

      // Check whether it is a new metadata item.
      if (mdItemSeq == null) {
	// Yes: Create it.
	mdItemSeq = mdManager.addMdItem(conn, parentSeq, mdItemTypeSeq,
	    auMdSeq, date, coverage);
	log.debug3(DEBUG_HEADER + "new mdItemSeq = " + mdItemSeq);

	mdManager.addMdItemName(conn, mdItemSeq, itemTitle, PRIMARY_NAME_TYPE);

	newMdItem = true;
      }
    }

    log.debug3(DEBUG_HEADER + "newMdItem = " + newMdItem);

    String volume = null;

    // Check  whether this is a journal article.
    if (MD_ITEM_TYPE_JOURNAL_ARTICLE.equals(mdItemType)) {
      // Yes: Get the volume received in the metadata.
      volume = mdinfo.volume;
      log.debug3(DEBUG_HEADER + "volume = " + volume);
    }

    // Get the authors received in the metadata.
    Set<String> authors = mdinfo.authorSet;
    log.debug3(DEBUG_HEADER + "authors = " + authors);

    // Get the keywords received in the metadata.
    Set<String> keywords = mdinfo.keywordSet;
    log.debug3(DEBUG_HEADER + "keywords = " + keywords);

    // Check whether it is a new metadata item.
    if (newMdItem) {
      // Yes: Add the bibliographic data.
      int addedCount =
  	addBibItem(conn, mdItemSeq, volume, issue, startPage, endPage, itemNo);
      log.debug3(DEBUG_HEADER + "addedCount = " + addedCount);

      // Add the item URLs.
      mdManager.addMdItemUrls(conn, mdItemSeq, accessUrl, featuredUrlMap);
      log.debug3(DEBUG_HEADER + "added AUItem URL.");

      // Add the item authors.
      mdManager.addMdItemAuthors(conn, mdItemSeq, authors);
      log.debug3(DEBUG_HEADER + "added AUItem authors.");

      // Add the item keywords.
      mdManager.addMdItemKeywords(conn, mdItemSeq, keywords);
      log.debug3(DEBUG_HEADER + "added AUItem keywords.");

      // Add the item DOI.
      mdManager.addMdItemDoi(conn, mdItemSeq, doi);
      log.debug3(DEBUG_HEADER + "added AUItem DOI.");
    } else {
      // No: Since the record exists, only add the properties that are new.
      int updatedCount = updateBibItem(conn, mdItemSeq, volume, issue,
	  startPage, endPage, itemNo);
      log.debug3(DEBUG_HEADER + "updatedCount = " + updatedCount);

      // Add the item new URLs.
      mdManager.addNewMdItemUrls(conn, mdItemSeq, accessUrl, featuredUrlMap);
      log.debug3(DEBUG_HEADER + "added AUItem URL.");

      // Add the item new authors.
      mdManager.addNewMdItemAuthors(conn, mdItemSeq, authors);
      log.debug3(DEBUG_HEADER + "updated AUItem authors.");

      // Add the item new keywords.
      mdManager.addNewMdItemKeywords(conn, mdItemSeq, keywords);
      log.debug3(DEBUG_HEADER + "updated AUItem keywords.");

      // Update the item DOI.
      updateMdItemDoi(conn, mdItemSeq, doi);
      log.debug3(DEBUG_HEADER + "updated AUItem DOI.");
    }

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the name of the type of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the identifier of the metadata item.
   * @return a String with the name of the type of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private String getMdItemTypeName(Connection conn, Long mdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "getMdItemTypeName(): ";
    String typeName = null;

    try {
      PreparedStatement getMdItemTypeName = dbManager.prepareStatement(conn,
	  GET_MD_ITEM_TYPE_NAME_QUERY);
      ResultSet resultSet = null;

      try {
	getMdItemTypeName.setLong(1, mdItemSeq);
	resultSet = dbManager.executeQuery(getMdItemTypeName);

	if (resultSet.next()) {
	  typeName = resultSet.getString(TYPE_NAME_COLUMN);
	  log.debug3(DEBUG_HEADER + "typeName = " + typeName);
	}
      } finally {
	DbManager.safeCloseResultSet(resultSet);
	getMdItemTypeName.close();
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot get a metadata item type name", sqle);
    }

    return typeName;
  }

  /**
   * Updates the DOI of a metadata item in the database.
   * 
   * @param conn
   *          A Connection with the connection to the database
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param doi
   *          A String with the metadata item DOI.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void updateMdItemDoi(Connection conn, Long mdItemSeq, String doi)
      throws DbException {
    if (StringUtil.isNullString(doi)) {
      return;
    }

    try {
      PreparedStatement findMdItemDoi = dbManager.prepareStatement(conn,
	  FIND_MD_ITEM_DOI_QUERY);

      ResultSet resultSet = null;

      try {
	findMdItemDoi.setLong(1, mdItemSeq);
	resultSet = dbManager.executeQuery(findMdItemDoi);

	if (!resultSet.next()) {
	  mdManager.addMdItemDoi(conn, mdItemSeq, doi);
	}
      } finally {
	DbManager.safeCloseResultSet(resultSet);
	findMdItemDoi.close();
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot update AU metadata version", sqle);
    }
  }

  /**
   * Updates a bibliographic item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param volume
   *          A String with the bibliographic volume.
   * @param issue
   *          A String with the bibliographic issue.
   * @param startPage
   *          A String with the bibliographic starting page.
   * @param endPage
   *          A String with the bibliographic ending page.
   * @param itemNo
   *          A String with the bibliographic item number.
   * @return an int with the number of database rows updated.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int updateBibItem(Connection conn, Long mdItemSeq, String volume,
      String issue, String startPage, String endPage, String itemNo)
      throws DbException {
    final String DEBUG_HEADER = "updateBibItem(): ";
    int updatedCount = 0;

    try {
      PreparedStatement updateBibItem = dbManager.prepareStatement(conn,
	  UPDATE_BIB_ITEM_QUERY);

      try {
	updateBibItem.setString(1, volume);
	updateBibItem.setString(2, issue);
	updateBibItem.setString(3, startPage);
	updateBibItem.setString(4, endPage);
	updateBibItem.setString(5, itemNo);
	updateBibItem.setLong(6, mdItemSeq);
	updatedCount = dbManager.executeUpdate(updateBibItem);
      } finally {
	DbManager.safeCloseStatement(updateBibItem);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot update bibliographic item", sqle);
    }

    log.debug3(DEBUG_HEADER + "updatedCount = " + updatedCount);
    return updatedCount;
  }

  /**
   * Adds to the database a bibliographic item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param volume
   *          A String with the bibliographic volume.
   * @param issue
   *          A String with the bibliographic issue.
   * @param startPage
   *          A String with the bibliographic starting page.
   * @param endPage
   *          A String with the bibliographic ending page.
   * @param itemNo
   *          A String with the bibliographic item number.
   * @return an int with the number of database rows inserted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int addBibItem(Connection conn, Long mdItemSeq, String volume,
      String issue, String startPage, String endPage, String itemNo)
      throws DbException {
    final String DEBUG_HEADER = "addBibItem(): ";
    int addedCount = 0;

    try {
      PreparedStatement insertBibItem = dbManager.prepareStatement(conn,
	  INSERT_BIB_ITEM_QUERY);

      try {
	insertBibItem.setLong(1, mdItemSeq);
	insertBibItem.setString(2, volume);
	insertBibItem.setString(3, issue);
	insertBibItem.setString(4, startPage);
	insertBibItem.setString(5, endPage);
	insertBibItem.setString(6, itemNo);
	addedCount = dbManager.executeUpdate(insertBibItem);
      } finally {
	DbManager.safeCloseStatement(insertBibItem);
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot add bibliographic item", sqle);
    }

    log.debug3(DEBUG_HEADER + "addedCount = " + addedCount);
    return addedCount;
  }
  
  /**
   * Provides an indication of whether the previous publication is the same as
   * the current publication.
   * 
   * @param mdinfo
   *          An ArticleMetadataInfo providing the metadata of the current
   *          publication.
   * @return <code>true</code> if the previous publication is the same as the
   *         current publication, <code>false</code> otherwise.
   */
  private boolean isSamePublication(ArticleMetadataInfo mdinfo) {
    return isSameProperty(journalTitle, mdinfo.journalTitle) &&
	isSameProperty(pIsbn, mdinfo.isbn) &&
	isSameProperty(eIsbn, mdinfo.eisbn) &&
	isSameProperty(pIssn, mdinfo.issn) &&
	isSameProperty(eIssn, mdinfo.eissn) &&
	isSameProperty(proprietaryId, mdinfo.proprietaryIdentifier) &&
	isSameProperty(volume, mdinfo.volume);
  }
  
  /**
   * Provides an indication of whether the previous property is the same as the
   * current property.
   * 
   * @param previous
   *          A String with the previous property.
   * @param current
   *          A String with the current property.
   * @return <code>true</code> if the previous property is the same as the
   *         current property, <code>false</code> otherwise.
   */
  private boolean isSameProperty(String previous, String current) {
    if (!StringUtil.isNullString(previous)) {
      return !StringUtil.isNullString(current) && previous.equals(current);
    }

    return StringUtil.isNullString(current);
  }

  /**
   * Fixes the Archival Unit data of unknown publishers.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param problems
   *          A List<String> with the recorded problems for the Archival Unit.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void fixUnknownPublishersAuData(Connection conn,
      List<String> problems) throws DbException {
    final String DEBUG_HEADER = "fixUnknownPublishersAuData(): ";
    log.debug3(DEBUG_HEADER + "Starting...");

    // Loop through all the problems.
    for (String problem : problems) {
      // Consider only problems created by an unknown publisher.
      if (problem.startsWith(UNKNOWN_PUBLISHER_AU_PROBLEM)) {
	log.debug3(DEBUG_HEADER + "Need to migrate data under publisher '"
	    + problem + "' to publisher '" + publisherName + "'.");
	fixUnknownPublisherAuData(conn, problem);
      }
    }

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Fixes the Archival Unit data of an unknown publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param unknownPublisherName
   *          A String with the name of the unknown publisher.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void fixUnknownPublisherAuData(Connection conn,
      String unknownPublisherName) throws DbException {
    final String DEBUG_HEADER = "fixUnknownPublisherAuData(): ";
    log.debug3(DEBUG_HEADER + "unknownPublisherName = " + unknownPublisherName);

    // Get the identifier of the unknown publisher.
    Long unknownPublisherSeq =
	mdManager.findPublisher(conn, unknownPublisherName);
    log.debug3(DEBUG_HEADER + "unknownPublisherSeq = " + unknownPublisherSeq);

    // Check whether the unknown publisher is not the current one.
    if (unknownPublisherSeq != null && unknownPublisherSeq != publisherSeq) {
      // Yes: Get the identifiers of any publications of the unknown publisher.
      Set<Long> unknownPublicationSeqs =
	  mdManager.findPublisherPublications(conn, unknownPublisherSeq);

      // Get the identifiers of the metadata items of the current publication.
      Set<Long> mdItemSeqs =
	    mdManager.findPublicationChildMetadataItems(conn, publicationSeq);

      Map<String, Long> mdItemMapByName = new HashMap<String, Long>();

      // Loop through all the identifiers of the metadata items of the current
      // publication.
      for (Long mdItemSeq : mdItemSeqs) {
	// Get allthe names of this metadata item.
	Map<String, String> mdItemSeqNames =
	    mdManager.getMdItemNames(conn, mdItemSeq);

	// Map the identifier by each of its names.
	for (String mdItemSeqName : mdItemSeqNames.keySet()) {
	  mdItemMapByName.put(mdItemSeqName, mdItemSeq);
	}
      }

      // Loop though all the identifiers of any publications of the unknown
      // publisher.
      for (Long unknownPublicationSeq : unknownPublicationSeqs) {
	log.debug3(DEBUG_HEADER + "unknownPublicationSeq = "
	    + unknownPublicationSeq);

	// Ignore the publication if it is the current one.
	if (unknownPublicationSeq != publicationSeq) {
	  // Fix the metadata of the publication of the unknown publisher.
	  fixUnknownPublisherPublicationMetadata(conn, unknownPublicationSeq,
	      mdItemMapByName);

	  // Fix COUNTER reports references.
	  fixUnknownPublisherPublicationCounterReportsData(conn,
	      unknownPublicationSeq);

	  // Remove the metadata item of the publication created for an unknown
	  // publisher.
	  removeUnknownPublisherPublicationMdItem(conn, unknownPublicationSeq);

	  // Remove the publication created for an unknown publisher.
	  removeUnknownPublisherPublication(conn, unknownPublicationSeq);
	}
      }
    }

    // Remove the record of the fixed unknown publisher problem.
    removeUnknownPublisher(conn, unknownPublisherName);

    // Remove the record of the fixed unknown publisher problem.
    mdManager.removeAuProblem(conn, auId, unknownPublisherName);

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Fixes the metadata of a publication of an unknown publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param unknownPublicationSeq
   *          A Long with the identifier of the publication.
   * @param mdItemMapByName
   *          A Map<String, Long> with a map of the current publication metadata
   *          items by their names.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void fixUnknownPublisherPublicationMetadata(Connection conn,
      Long unknownPublicationSeq, Map<String, Long> mdItemMapByName)
      throws DbException {
    final String DEBUG_HEADER = "fixUnknownPublisherPublicationMetadata(): ";
    log.debug3(DEBUG_HEADER + "unknownPublicationSeq = "
	+ unknownPublicationSeq);

    // Get the identifiers of the metadata items of the unknown publication.
    Set<Long> unknownMdItemSeqs =
	mdManager.findPublicationChildMetadataItems(conn,
	    unknownPublicationSeq);

    // Loop through all the identifiers of the metadata items of the unknown
    // publication.
    for (Long unknownMdItemSeq : unknownMdItemSeqs) {
      boolean merged = false;

      // Map the identifier by each of its names.
      Map<String, String> unknownMdItemSeqNames =
	  mdManager.getMdItemNames(conn, unknownMdItemSeq);

      // Loop through all of the names of the unknown publication metadata item.
      for (String unknownMdItemSeqName : unknownMdItemSeqNames.keySet()) {
	// Check whether the current publication has a child metadata item with
	// the same name.
	if (mdItemMapByName.containsKey(unknownMdItemSeqName)) {
	  // Yes: Merge the properties of the unknown publication child metadata
	  // item into the corresponding current publication child metadata
	  // item.
	  mdManager.mergeChildMdItemProperties(conn, unknownMdItemSeq,
	      mdItemMapByName.get(unknownMdItemSeqName));

	  merged = true;
	  break;
	}
      }

      // Check whether the properties were not merged.
      if (!merged) {
	// Yes: Assign the unknown publication metadata item to the current
	// publication.
	mdManager.updateMdItemParentSeq(conn, unknownMdItemSeq, parentSeq);
      }
    }

    // Get the identifier of the unknown publication metadata item.
    Long unknownParentSeq =
	mdManager.findPublicationMetadataItem(conn, unknownPublicationSeq);
    log.debug3(DEBUG_HEADER + "unknownParentSeq = " + unknownParentSeq);

    // Merge the properties of the unknown publication metadata item into the
    // current publication metadata item.
    mdManager.mergeParentMdItemProperties(conn, unknownParentSeq, parentSeq);

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Fixes the COUNTER Reports data of a publication of an unknown publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param unknownPublicationSeq
   *          A Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void fixUnknownPublisherPublicationCounterReportsData(Connection conn,
      Long unknownPublicationSeq)throws DbException {
    final String DEBUG_HEADER =
	"fixUnknownPublisherPublicationCounterReportsData(): ";
    log.debug3(DEBUG_HEADER + "unknownPublicationSeq = "
	+ unknownPublicationSeq);

    CounterReportsManager crManager =
	LockssDaemon.getLockssDaemon().getCounterReportsManager();

    // Merge the book type aggregate counts.
    crManager.mergeBookTypeAggregates(conn, unknownPublicationSeq,
	publicationSeq);

    // Delete the book type aggregate counts for the unknown publisher
    // publication.
    crManager.deleteBookTypeAggregates(conn, unknownPublicationSeq);

    // Merge the journal type aggregate counts.
    crManager.mergeJournalTypeAggregates(conn, unknownPublicationSeq,
	publicationSeq);

    // Delete the journal type aggregate counts for the unknown publisher
    // publication.
    crManager.deleteJournalTypeAggregates(conn, unknownPublicationSeq);

    // Merge the journal publication year aggregate counts.
    crManager.mergeJournalPubYearAggregates(conn, unknownPublicationSeq,
	publicationSeq);

    // Delete the journal publication year aggregate counts for the unknown
    // publisher
    // publication.
    crManager.deleteJournalPubYearAggregates(conn, unknownPublicationSeq);

    log.debug3(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes the metadata item of a publication created for an unknown
   * publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the publication identifier.
   * @return an int with the number of rows deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int removeUnknownPublisherPublicationMdItem(Connection conn,
      Long publicationSeq) throws DbException {
    final String DEBUG_HEADER = "removeUnknownPublisherPublicationMdItem(): ";
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    int count = 0;

    if (publicationSeq != null) {
      log.debug3(DEBUG_HEADER + "SQL = '"
	  + DELETE_UNKNOWN_PUBLISHER_PUBLICATION_MD_ITEM_QUERY + "'.");
      PreparedStatement deleteMdItem = null;

      try {
	deleteMdItem = dbManager.prepareStatement(conn,
	    DELETE_UNKNOWN_PUBLISHER_PUBLICATION_MD_ITEM_QUERY);
	deleteMdItem.setLong(1, publicationSeq);
	deleteMdItem.setLong(2, publicationSeq);
	count = dbManager.executeUpdate(deleteMdItem);
      } catch (SQLException sqle) {
	log.error("Cannot delete an unknown publisher publication", sqle);
	log.error("publicationSeq = " + publicationSeq);
	log.error("SQL = '"
	    + DELETE_UNKNOWN_PUBLISHER_PUBLICATION_MD_ITEM_QUERY + "'.");
	throw new DbException("Cannot delete an unknown publisher publication",
	    sqle);
      } finally {
	DbManager.safeCloseStatement(deleteMdItem);
      }
    }

    log.debug3(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Removes a publication created for an unknown publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the publication identifier.
   * @return an int with the number of rows deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int removeUnknownPublisherPublication(Connection conn,
      Long publicationSeq) throws DbException {
    final String DEBUG_HEADER = "removeUnknownPublisherPublication(): ";
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    int count = 0;

    if (publicationSeq != null) {
      log.debug3(DEBUG_HEADER + "SQL = '"
	  + DELETE_UNKNOWN_PUBLISHER_PUBLICATION_QUERY + "'.");
      PreparedStatement deletePublication = null;

      try {
	deletePublication = dbManager.prepareStatement(conn,
	    DELETE_UNKNOWN_PUBLISHER_PUBLICATION_QUERY);
	deletePublication.setLong(1, publicationSeq);
	deletePublication.setLong(2, publicationSeq);
	count = dbManager.executeUpdate(deletePublication);
      } catch (SQLException sqle) {
	log.error("Cannot delete an unknown publisher publication", sqle);
	log.error("publicationSeq = " + publicationSeq);
	log.error("SQL = '" + DELETE_UNKNOWN_PUBLISHER_PUBLICATION_QUERY
	    + "'.");
	throw new DbException("Cannot delete an unknown publisher publication",
	    sqle);
      } finally {
	DbManager.safeCloseStatement(deletePublication);
      }
    }

    log.debug3(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Removes an unknown publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherName
   *          A String with the publisher name.
   * @return an int with the number of rows deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int removeUnknownPublisher(Connection conn,
      String publisherName) throws DbException {
    final String DEBUG_HEADER = "removeUnknownPublisherPublication(): ";
    log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    int count = 0;

    if (publisherName != null
	&& publisherName.startsWith(UNKNOWN_PUBLISHER_AU_PROBLEM)) {
      log.debug3(DEBUG_HEADER + "SQL = '"
	  + DELETE_UNKNOWN_PUBLISHER_QUERY + "'.");
      PreparedStatement deletePublisher = null;

      try {
	deletePublisher =
	    dbManager.prepareStatement(conn, DELETE_UNKNOWN_PUBLISHER_QUERY);
	deletePublisher.setString(1, publisherName);
	count = dbManager.executeUpdate(deletePublisher);
      } catch (SQLException sqle) {
	log.error("Cannot delete an unknown publisher", sqle);
	log.error("publisherName = " + publisherName);
	log.error("SQL = '" + DELETE_UNKNOWN_PUBLISHER_QUERY
	    + "'.");
	throw new DbException("Cannot delete an unknown publisher", sqle);
      } finally {
	DbManager.safeCloseStatement(deletePublisher);
      }
    }

    log.debug3(DEBUG_HEADER + "count = " + count);
    return count;
  }
}
