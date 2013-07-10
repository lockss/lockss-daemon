/*
 * $Id: DbManager.java,v 1.25 2013-07-10 21:59:20 fergaloy-sf Exp $
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

/**
 * Database manager.
 * 
 * @author Fernando Garcia-Loygorri
 */
package org.lockss.db;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.derby.jdbc.ClientDataSource;
import org.lockss.app.*;
import org.lockss.config.*;
import org.lockss.util.*;

public class DbManager extends BaseLockssDaemonManager
  implements ConfigurableManager {
  private static final Logger log = Logger.getLogger(DbManager.class);

  // Prefix for the database manager configuration entries.
  private static final String PREFIX = Configuration.PREFIX + "dbManager.";

  // Prefix for the Derby configuration entries.
  private static final String DERBY_ROOT = PREFIX + "derby";

  /**
   * Derby log append option. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_INFOLOG_APPEND = DERBY_ROOT
      + ".infologAppend";
  public static final String DEFAULT_DERBY_INFOLOG_APPEND = "false";

  /**
   * Derby log query plan option. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_LANGUAGE_LOGQUERYPLAN = DERBY_ROOT
      + ".languageLogqueryplan";
  public static final String DEFAULT_DERBY_LANGUAGE_LOGQUERYPLAN = "false";

  /**
   * Derby log statement text option. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_LANGUAGE_LOGSTATEMENTTEXT = DERBY_ROOT
      + ".languageLogstatementtext";
  public static final String DEFAULT_DERBY_LANGUAGE_LOGSTATEMENTTEXT = "false";

  /**
   * Name of the Derby log file path. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_STREAM_ERROR_FILE = DERBY_ROOT
      + ".streamErrorFile";
  public static final String DEFAULT_DERBY_STREAM_ERROR_FILE = "derby.log";

  /**
   * Name of the Derby log severity level. Changes require daemon restart.
   */
  public static final String PARAM_DERBY_STREAM_ERROR_LOGSEVERITYLEVEL =
      DERBY_ROOT + ".streamErrorLogseveritylevel";
  public static final String DEFAULT_DERBY_STREAM_ERROR_LOGSEVERITYLEVEL =
      "4000";

  // Prefix for the datasource configuration entries.
  private static final String DATASOURCE_ROOT = PREFIX + "datasource";

  /**
   * Name of the database datasource class. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_CLASSNAME = DATASOURCE_ROOT
      + ".className";
  public static final String DEFAULT_DATASOURCE_CLASSNAME =
      "org.apache.derby.jdbc.EmbeddedDataSource";

  /**
   * Name of the database create. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_CREATEDATABASE = DATASOURCE_ROOT
      + ".createDatabase";
  public static final String DEFAULT_DATASOURCE_CREATEDATABASE = "create";

  /**
   * Name of the database with the relative path to the DB directory. Changes
   * require daemon restart.
   */
  public static final String PARAM_DATASOURCE_DATABASENAME = DATASOURCE_ROOT
      + ".databaseName";
  public static final String DEFAULT_DATASOURCE_DATABASENAME = "db/DbManager";

  /**
   * Port number of the database. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_PORTNUMBER = DATASOURCE_ROOT
      + ".portNumber";
  public static final String DEFAULT_DATASOURCE_PORTNUMBER = "1527";

  public static final String DEFAULT_DATASOURCE_PORTNUMBER_PG = "5432";

  /**
   * Name of the server. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_SERVERNAME = DATASOURCE_ROOT
      + ".serverName";
  public static final String DEFAULT_DATASOURCE_SERVERNAME = "localhost";

  /**
   * Name of the database user. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_USER = DATASOURCE_ROOT + ".user";
  public static final String DEFAULT_DATASOURCE_USER = "LOCKSS";

  /**
   * Name of the existing database password. Changes require daemon restart.
   */
  public static final String PARAM_DATASOURCE_PASSWORD = DATASOURCE_ROOT
      + ".password";
  public static final String DEFAULT_DATASOURCE_PASSWORD = "insecure";

  /**
   * Set to false to prevent DbManager from running
   */
  public static final String PARAM_DBMANAGER_ENABLED = PREFIX + "enabled";
  static final boolean DEFAULT_DBMANAGER_ENABLED = true;

  /**
   * Maximum number of retries for transient SQL exceptions.
   */
  public static final String PARAM_MAX_RETRY_COUNT = PREFIX + "maxRetryCount";
  public static final int DEFAULT_MAX_RETRY_COUNT = 10;

  /**
   * Delay  between retries for transient SQL exceptions.
   */
  public static final String PARAM_RETRY_DELAY = PREFIX + "retryDelay";
  public static final long DEFAULT_RETRY_DELAY = 3 * Constants.SECOND;

  /**
   * The indicator to be inserted in the database at the end of truncated text
   * values.
   */
  public static final String TRUNCATION_INDICATOR = "\u0019";

  //
  // Database table names.
  //
  /** Name of the obsolete DOI table. */
  public static final String OBSOLETE_TITLE_TABLE = "TITLE";

  /** Name of the obsolete feature table. */
  public static final String OBSOLETE_FEATURE_TABLE = "FEATURE";

  /** Name of the obsolete pending AUs table. */
  public static final String OBSOLETE_PENDINGAUS_TABLE = "PENDINGAUS";

  /** Name of the obsolete metadata table. */
  public static final String OBSOLETE_METADATA_TABLE = "METADATA";

  /** Name of the plugin table. */
  public static final String PLUGIN_TABLE = "plugin";

  /** Name of the archival unit table. */
  public static final String AU_TABLE = "au";

  /** Name of the archival unit metadata table. */
  public static final String AU_MD_TABLE = "au_md";

  /** Name of the metadata item type table. */
  public static final String MD_ITEM_TYPE_TABLE = "md_item_type";

  /** Name of the metadata item table. */
  public static final String MD_ITEM_TABLE = "md_item";

  /** Name of the metadata item name table. */
  public static final String MD_ITEM_NAME_TABLE = "md_item_name";

  /** Name of the metadata key table. */
  public static final String MD_KEY_TABLE = "md_key";

  /** Name of the metadata table. */
  public static final String MD_TABLE = "md";

  /** Name of the bibliographic item table. */
  public static final String BIB_ITEM_TABLE = "bib_item";

  /** Name of the URL table. */
  public static final String URL_TABLE = "url";

  /** Name of the author table. */
  public static final String AUTHOR_TABLE = "author";

  /** Name of the keyword table. */
  public static final String KEYWORD_TABLE = "keyword";

  /** Name of the DOI table. */
  public static final String DOI_TABLE = "doi";

  /** Name of the ISSN table. */
  public static final String ISSN_TABLE = "issn";

  /** Name of the ISBN table. */
  public static final String ISBN_TABLE = "isbn";

  /** Name of the publisher table. */
  public static final String PUBLISHER_TABLE = "publisher";

  /** Name of the publication table. */
  public static final String PUBLICATION_TABLE = "publication";

  /** Name of the pending AUs table. */
  public static final String PENDING_AU_TABLE = "pending_au";

  /** Name of the version table. */
  public static final String VERSION_TABLE = "version";

  /** Name of the obsolete COUNTER publication year aggregate table. */
  public static final String OBSOLETE_PUBYEAR_AGGREGATES_TABLE =
      "counter_pubyear_aggregates";

  /** Name of the obsolete COUNTER title table. */
  public static final String OBSOLETE_TITLES_TABLE = "counter_titles";

  /** Name of the obsolete COUNTER type aggregates table. */
  public static final String OBSOLETE_TYPE_AGGREGATES_TABLE =
      "counter_type_aggregates";

  /** Name of the obsolete COUNTER request table. */
  public static final String OBSOLETE_REQUESTS_TABLE = "counter_requests";

  /** Name of the COUNTER request table. */
  public static final String COUNTER_REQUEST_TABLE = "counter_request";

  /** Name of the COUNTER books type aggregates table. */
  public static final String COUNTER_BOOK_TYPE_AGGREGATES_TABLE =
      "counter_book_type_aggregates";

  /** Name of the COUNTER books type aggregates table. */
  public static final String COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE =
      "counter_journal_type_aggregates";

  /** Name of the obsolete COUNTER journal publication year aggregate table. */
  public static final String COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE =
      "counter_journal_pubyear_aggregate";

  /** Name of the platform table. */
  public static final String PLATFORM_TABLE = "platform";

  /** Name of the subscription table. */
  public static final String SUBSCRIPTION_TABLE = "subscription";

  /** Name of the subscription range table. */
  public static final String SUBSCRIPTION_RANGE_TABLE = "subscription_range";

  /** Name of the unconfigured Archival Unit table. */
  public static final String UNCONFIGURED_AU_TABLE = "unconfigured_au";

  /** Name of the Archival Unit problem table. */
  public static final String AU_PROBLEM_TABLE = "au_problem";

  //
  // Database table column names.
  //
  /** Metadata identifier column. */
  public static final String MD_ID_COLUMN = "md_id";

  /** Article title column. */
  public static final String ARTICLE_TITLE_COLUMN = "article_title";

  /** Author column. */
  public static final String AUTHOR_COLUMN = "author";

  /** Access URL column. */
  public static final String ACCESS_URL_COLUMN = "access_url";

  /** Title column. */
  public static final String TITLE_COLUMN = "title";

  /** LOCKSS identifier column. */
  public static final String LOCKSS_ID_COLUMN = "lockss_id";
  public static final String TITLE_NAME_COLUMN = "title_name";

  /** Publisher name column. */
  public static final String PUBLISHER_NAME_COLUMN = "publisher_name";
  public static final String PLATFORM_NAME_COLUMN = "platform_name";

  /** DOI column. */
  public static final String DOI_COLUMN = "doi";
  public static final String PROPRIETARY_ID_COLUMN = "proprietary_id";
  public static final String PRINT_ISSN_COLUMN = "print_issn";
  public static final String ONLINE_ISSN_COLUMN = "online_issn";

  /** ISBN column. */
  public static final String ISBN_COLUMN = "isbn";
  public static final String BOOK_ISSN_COLUMN = "book_issn";

  /** Publication year column. */
  public static final String PUBLICATION_YEAR_COLUMN = "publication_year";
  public static final String IS_BOOK_COLUMN = "is_book";
  public static final String IS_SECTION_COLUMN = "is_section";
  public static final String IS_HTML_COLUMN = "is_html";
  public static final String IS_PDF_COLUMN = "is_pdf";

  /** Publisher involvement indicator column. */
  public static final String IS_PUBLISHER_INVOLVED_COLUMN =
      "is_publisher_involved";

  /** In-aggregation indicator column. */
  public static final String IN_AGGREGATION_COLUMN = "in_aggregation";

  /** Request day column. */
  public static final String REQUEST_DAY_COLUMN = "request_day";

  /** Request month column. */
  public static final String REQUEST_MONTH_COLUMN = "request_month";

  /** Request year column. */
  public static final String REQUEST_YEAR_COLUMN = "request_year";
  public static final String TOTAL_JOURNAL_REQUESTS_COLUMN =
      "total_journal_requests";
  public static final String HTML_JOURNAL_REQUESTS_COLUMN =
      "html_journal_requests";
  public static final String PDF_JOURNAL_REQUESTS_COLUMN =
      "pdf_journal_requests";
  public static final String FULL_BOOK_REQUESTS_COLUMN =
      "full_book_requests";
  public static final String SECTION_BOOK_REQUESTS_COLUMN =
      "section_book_requests";
  public static final String REQUEST_COUNT_COLUMN = "request_count";

  /** Title sequential identifier column. */
  public static final String PLUGIN_SEQ_COLUMN = "plugin_seq";

  /** Name of plugin_id column. */
  public static final String PLUGIN_ID_COLUMN = "plugin_id";

  /** Name of the plugin platform column. */
  public static final String PLATFORM_COLUMN = "platform";

  /** Archival unit sequential identifier column. */
  public static final String AU_SEQ_COLUMN = "au_seq";

  /** Name of au_key column. */
  public static final String AU_KEY_COLUMN = "au_key";

  /** Archival unit metadata sequential identifier column. */
  public static final String AU_MD_SEQ_COLUMN = "au_md_seq";

  /** Name of the metadata version column. */
  public static final String MD_VERSION_COLUMN = "md_version";

  /** Metadata extraction time column. */
  public static final String EXTRACT_TIME_COLUMN = "extract_time";

  /** Metadata item type identifier column. */
  public static final String MD_ITEM_TYPE_SEQ_COLUMN = "md_item_type_seq";

  /** Type name column. */
  public static final String TYPE_NAME_COLUMN = "type_name";

  /** Metadata item identifier column. */
  public static final String MD_ITEM_SEQ_COLUMN = "md_item_seq";

  /** Parent identifier column. */
  public static final String PARENT_SEQ_COLUMN = "parent_seq";

  /** Date column. */
  public static final String DATE_COLUMN = "date";

  /** Name of the coverage column. */
  public static final String COVERAGE_COLUMN = "coverage";

  /** Name column. */
  public static final String NAME_COLUMN = "name";

  /** Name type column. */
  public static final String NAME_TYPE_COLUMN = "name_type";

  /** Metadata key identifier column. */
  public static final String MD_KEY_SEQ_COLUMN = "md_key_seq";

  /** Key name column. */
  public static final String KEY_NAME_COLUMN = "key_name";

  /** Metadata value column. */
  public static final String MD_VALUE_COLUMN = "md_value";

  /** Volume column. */
  public static final String VOLUME_COLUMN = "volume";

  /** Issue column. */
  public static final String ISSUE_COLUMN = "issue";

  /** Start page column. */
  public static final String START_PAGE_COLUMN = "start_page";

  /** End page column. */
  public static final String END_PAGE_COLUMN = "end_page";
  
  /** Item number column. */
  public static final String ITEM_NO_COLUMN = "item_no";

  /** Feature column (e.g. "fulltext", "abstract", "toc") */
  public static final String FEATURE_COLUMN = "feature";

  /** URL column. */
  public static final String URL_COLUMN = "url";

  /** Author column. */
  public static final String AUTHOR_NAME_COLUMN = "author_name";

  /** Author index column. */
  public static final String AUTHOR_IDX_COLUMN = "author_idx";

  /** Keyword column. */
  public static final String KEYWORD_COLUMN = "keyword";

  /** ISSN column. */
  public static final String ISSN_COLUMN = "issn";

  /** ISSN type column. */
  public static final String ISSN_TYPE_COLUMN = "issn_type";

  /** ISBN type column. */
  public static final String ISBN_TYPE_COLUMN = "isbn_type";

  /** Publisher identifier column. */
  public static final String PUBLISHER_SEQ_COLUMN = "publisher_seq";

  /** Publication identifier column. */
  public static final String PUBLICATION_SEQ_COLUMN = "publication_seq";

  /** Publication publisher identifier column. */
  public static final String PUBLICATION_ID_COLUMN = "publication_id";

  /** Priority column. */
  public static final String PRIORITY_COLUMN = "priority";

  /** System column. */
  public static final String SYSTEM_COLUMN = "system";

  /** Version column. */
  public static final String VERSION_COLUMN = "version";

  /** Total requests column. */
  public static final String TOTAL_REQUESTS_COLUMN = "total_requests";

  /** HTML requests column. */
  public static final String HTML_REQUESTS_COLUMN = "html_requests";

  /** PDF requests column. */
  public static final String PDF_REQUESTS_COLUMN = "pdf_requests";

  /** Full requests column. */
  public static final String FULL_REQUESTS_COLUMN = "full_requests";

  /** Section requests column. */
  public static final String SECTION_REQUESTS_COLUMN = "section_requests";

  /** Requests column. */
  public static final String REQUESTS_COLUMN = "requests";

  /** Platform identifier column. */
  public static final String PLATFORM_SEQ_COLUMN = "platform_seq";

  /** Subscription identifier column. */
  public static final String SUBSCRIPTION_SEQ_COLUMN = "subscription_seq";

  /** Requests column. */
  public static final String RANGE_COLUMN = "range";

  /** Requests column. */
  public static final String SUBSCRIBED_COLUMN = "subscribed";

  /** Name of the Archival Unit problem description column. */
  public static final String PROBLEM_COLUMN = "problem";

  //
  // Maximum lengths of variable text length database columns.
  //
  /** Length of the article title column. */
  public static final int MAX_ARTICLE_TITLE_COLUMN = 512;

  /** Length of the author column. */
  public static final int OBSOLETE_MAX_AUTHOR_COLUMN = 512;

  /**
   * Length of the plugin ID column. This column will be used as a horizontal
   * partitioning column in the future, so its length must be compatible for
   * that purpose for the database product used.
   */
  public static final int OBSOLETE_MAX_PLUGIN_ID_COLUMN = 128;

  /** Length of the title column. */
  public static final int MAX_TITLE_COLUMN = 512;

  /**
   * Length of the plugin ID column. This column will be used as a horizontal
   * partitioning column in the future, so its length must be compatible for
   * that purpose for the database product used.
   */
  public static final int MAX_PLUGIN_ID_COLUMN = 256;

  /** Length of the publishing platform column. */
  public static final int MAX_PLATFORM_COLUMN = 64;

  /** Length of the AU key column. */
  public static final int MAX_AU_KEY_COLUMN = 512;

  /** Length of the coverage column. */
  public static final int MAX_COVERAGE_COLUMN = 16;

  /** Length of the type name column. */
  public static final int MAX_TYPE_NAME_COLUMN = 32;

  /** Length of the date column. */
  public static final int MAX_DATE_COLUMN = 16;

  /** Length of the name column. */
  public static final int MAX_NAME_COLUMN = 512;

  /** Length of the name type column. */
  public static final int MAX_NAME_TYPE_COLUMN = 16;

  /** Length of the key name column. */
  public static final int MAX_KEY_NAME_COLUMN = 128;

  /** Length of the metadata value column. */
  public static final int MAX_MD_VALUE_COLUMN = 128;

  /** Length of the volume column. */
  public static final int MAX_VOLUME_COLUMN = 16;

  /** Length of the issue column. */
  public static final int MAX_ISSUE_COLUMN = 16;

  /** Length of the start page column. */
  public static final int MAX_START_PAGE_COLUMN = 16;

  /** Length of the end page column. */
  public static final int MAX_END_PAGE_COLUMN = 16;

  /** Length of the item number column. */
  public static final int MAX_ITEM_NO_COLUMN = 16;

  /** Length of feature column. */
  public static final int MAX_FEATURE_COLUMN = 32;

  /** Length of the URL column. */
  public static final int MAX_URL_COLUMN = 4096;

  /** Length of the author column. */
  public static final int MAX_AUTHOR_COLUMN = 128;

  /** Length of the keyword column. */
  public static final int MAX_KEYWORD_COLUMN = 64;

  /** Length of the DOI column. */
  public static final int MAX_DOI_COLUMN = 256;

  /** Length of the ISSN column. */
  public static final int MAX_ISSN_COLUMN = 8;

  /** Length of the ISSN type column. */
  public static final int MAX_ISSN_TYPE_COLUMN = 16;

  /** Length of the ISBN column. */
  public static final int MAX_ISBN_COLUMN = 13;

  /** Length of the ISBN type column. */
  public static final int MAX_ISBN_TYPE_COLUMN = 16;

  /** Length of the publication proprietary identifier column. */
  public static final int MAX_PUBLICATION_ID_COLUMN = 32;

  /** Length of the system column. */
  public static final int MAX_SYSTEM_COLUMN = 16;

  /** Length of the range column. */
  public static final int MAX_RANGE_COLUMN = 64;

  /** Length of the problem column. */
  public static final int MAX_PROBLEM_COLUMN = 512;

  //
  //Types of metadata items.
  //
  public static final String MD_ITEM_TYPE_BOOK = "book";
  public static final String MD_ITEM_TYPE_BOOK_CHAPTER = "book_chapter";
  public static final String MD_ITEM_TYPE_BOOK_SERIES = "book_series";
  public static final String MD_ITEM_TYPE_JOURNAL = "journal";
  public static final String MD_ITEM_TYPE_JOURNAL_ARTICLE = "journal_article";

  /**
   * The platform name when there is no platform name.
   */
  public static final String NO_PLATFORM = "";

  // Derby definition of a big integer primary key column with a value
  // automatically generated from a sequence.
  protected static final String BIGINT_SERIAL_PK_DERBY =
      "bigint primary key generated always as identity";

  // PostgreSQL definition of a big integer primary key column with a value
  // automatically generated from a sequence.
  protected static final String BIGINT_SERIAL_PK_PG = "bigserial primary key";

  // Query to create the table for recording bibliobraphic metadata for an
  // article.
  private static final String OBSOLETE_CREATE_METADATA_TABLE_QUERY = "create "
      + "table " + OBSOLETE_METADATA_TABLE + " ("
      + MD_ID_COLUMN + " --BigintSerialPk--,"
      + DATE_COLUMN + " varchar(" + MAX_DATE_COLUMN + "),"
      + VOLUME_COLUMN + " varchar(" + MAX_VOLUME_COLUMN + "),"
      + ISSUE_COLUMN + " varchar(" + MAX_ISSUE_COLUMN + "),"
      + START_PAGE_COLUMN + " varchar(" + MAX_START_PAGE_COLUMN + "),"
      + ARTICLE_TITLE_COLUMN + " varchar(" + MAX_ARTICLE_TITLE_COLUMN + "),"
      // author column is a semicolon-separated list
      + AUTHOR_COLUMN + " varchar(" + OBSOLETE_MAX_AUTHOR_COLUMN + "),"
      + PLUGIN_ID_COLUMN + " varchar(" + OBSOLETE_MAX_PLUGIN_ID_COLUMN
      + ") NOT NULL,"
      // partition by
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") NOT NULL,"
      + ACCESS_URL_COLUMN + " varchar(" + MAX_URL_COLUMN + ") NOT NULL)";

  // Query to create the table for recording title journal/book title of an
  // article.
  private static final String OBSOLETE_CREATE_TITLE_TABLE_QUERY = "create "
      + "table " + OBSOLETE_TITLE_TABLE + " ("
      + TITLE_COLUMN + " varchar(" + MAX_TITLE_COLUMN + ") NOT NULL,"
      + MD_ID_COLUMN + " bigint NOT NULL references " + OBSOLETE_METADATA_TABLE
      + " (" + MD_ID_COLUMN + ") on delete cascade)";

  // Query to create the table for recording pending AUs to index.
  private static final String OBSOLETE_CREATE_PENDINGAUS_TABLE_QUERY = "create "
      + "table " + OBSOLETE_PENDINGAUS_TABLE + " ("
      + PLUGIN_ID_COLUMN + " varchar(" + OBSOLETE_MAX_PLUGIN_ID_COLUMN
      + ") NOT NULL,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") NOT NULL)";

  // Query to create the table for recording a feature URL for an article.
  private static final String OBSOLETE_CREATE_FEATURE_TABLE_QUERY = "create "
      + "table " + OBSOLETE_FEATURE_TABLE + " ("
      + FEATURE_COLUMN + " VARCHAR(" + MAX_FEATURE_COLUMN + ") NOT NULL,"
      + ACCESS_URL_COLUMN + " VARCHAR(" + MAX_URL_COLUMN + ") NOT NULL," 
      + MD_ID_COLUMN + " BIGINT NOT NULL REFERENCES " + OBSOLETE_METADATA_TABLE
      + " (" + MD_ID_COLUMN + ") on delete cascade)";
  
  // Query to create the table for recording a DOI for an article.
  private static final String OBSOLETE_CREATE_DOI_TABLE_QUERY = "create table "
      + DOI_TABLE + " (" 
      + DOI_COLUMN + " VARCHAR(" + MAX_DOI_COLUMN + ") NOT NULL,"
      + MD_ID_COLUMN + " BIGINT NOT NULL REFERENCES " + OBSOLETE_METADATA_TABLE
      + " (" + MD_ID_COLUMN + ") on delete cascade)";
      
  // Query to create the table for recording an ISBN for an article.
  private static final String OBSOLETE_CREATE_ISBN_TABLE_QUERY = "create table "
      + ISBN_TABLE + " (" 
      + ISBN_COLUMN + " VARCHAR(" + MAX_ISBN_COLUMN + ") NOT NULL,"
      + MD_ID_COLUMN + " BIGINT NOT NULL REFERENCES " + OBSOLETE_METADATA_TABLE
      + " (" + MD_ID_COLUMN + ") on delete cascade)";

  // Query to create the table for recording an ISSN for an article.
  private static final String OBSOLETE_CREATE_ISSN_TABLE_QUERY = "create table "
      + ISSN_TABLE + " (" 
      + ISSN_COLUMN + " VARCHAR(" + MAX_ISSN_COLUMN + ") NOT NULL,"
      + MD_ID_COLUMN + " BIGINT NOT NULL REFERENCES " + OBSOLETE_METADATA_TABLE
      + " (" + MD_ID_COLUMN + ") on delete cascade)";

  // Query to create the table for recording title data used for COUNTER
  // reports.
  private static final String OBSOLETE_CREATE_TITLES_TABLE_QUERY = "create "
      + "table " + OBSOLETE_TITLES_TABLE + " ("
      + LOCKSS_ID_COLUMN + " bigint NOT NULL PRIMARY KEY,"
      + TITLE_NAME_COLUMN + " varchar(512) NOT NULL,"
      + PUBLISHER_NAME_COLUMN + " varchar(512),"
      + PLATFORM_NAME_COLUMN + " varchar(512),"
      + DOI_COLUMN + " varchar(256),"
      + PROPRIETARY_ID_COLUMN + " varchar(256),"
      + IS_BOOK_COLUMN + " boolean NOT NULL,"
      + PRINT_ISSN_COLUMN + " varchar(9),"
      + ONLINE_ISSN_COLUMN + " varchar(9),"
      + ISBN_COLUMN + " varchar(15),"
      + BOOK_ISSN_COLUMN + " varchar(9))";

  // Query to create the table for recording requests used for COUNTER reports.
  private static final String OBSOLETE_CREATE_REQUESTS_TABLE_QUERY = "create "
      + "table " + OBSOLETE_REQUESTS_TABLE + " ("
      + LOCKSS_ID_COLUMN
      + " bigint NOT NULL CONSTRAINT FK_LOCKSS_ID_REQUESTS REFERENCES "
      + OBSOLETE_TITLES_TABLE + ","
      + PUBLICATION_YEAR_COLUMN + " smallint,"
      + IS_SECTION_COLUMN + " boolean,"
      + IS_HTML_COLUMN + " boolean,"
      + IS_PDF_COLUMN + " boolean,"
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + REQUEST_DAY_COLUMN + " smallint NOT NULL,"
      + IN_AGGREGATION_COLUMN + " boolean)";

  // Query to create the table for recording type aggregates (PDF vs. HTML, Full
  // vs. Section, etc.) used for COUNTER reports.
  private static final String OBSOLETE_CREATE_TYPE_AGGREGATES_TABLE_QUERY
  = "create table " + OBSOLETE_TYPE_AGGREGATES_TABLE + " ("
      + LOCKSS_ID_COLUMN
      + " bigint NOT NULL CONSTRAINT FK_LOCKSS_ID_TYPE_AGGREGATES REFERENCES "
      + OBSOLETE_TITLES_TABLE + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + TOTAL_JOURNAL_REQUESTS_COLUMN + " integer,"
      + HTML_JOURNAL_REQUESTS_COLUMN + " integer,"
      + PDF_JOURNAL_REQUESTS_COLUMN + " integer,"
      + FULL_BOOK_REQUESTS_COLUMN + " integer,"
      + SECTION_BOOK_REQUESTS_COLUMN + " integer)";

  // Query to create the table for recording publication year aggregates used
  // for COUNTER reports.
  private static final String OBSOLETE_CREATE_PUBYEAR_AGGREGATES_TABLE_QUERY
  = "create table " + OBSOLETE_PUBYEAR_AGGREGATES_TABLE + " ("
      + LOCKSS_ID_COLUMN + " bigint NOT NULL CONSTRAINT "
      + "FK_LOCKSS_ID_PUBYEAR_AGGREGATES REFERENCES "
      + OBSOLETE_TITLES_TABLE + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + PUBLICATION_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_COUNT_COLUMN + " integer NOT NULL)";

  // Query to create the table for recording plugins.
  private static final String CREATE_PLUGIN_TABLE_QUERY = "create table "
      + PLUGIN_TABLE + " ("
      + PLUGIN_SEQ_COLUMN + " --BigintSerialPk--,"
      + PLUGIN_ID_COLUMN + " varchar(" + MAX_PLUGIN_ID_COLUMN + ") not null,"
      + PLATFORM_COLUMN + " varchar(" + MAX_PLATFORM_COLUMN + ")"
      + ")";

  // Query to create the table for recording archival units.
  protected static final String CREATE_AU_TABLE_QUERY = "create table "
      + AU_TABLE + " ("
      + AU_SEQ_COLUMN + " --BigintSerialPk--,"
      + PLUGIN_SEQ_COLUMN + " bigint not null references " + PLUGIN_TABLE
      + " (" + PLUGIN_SEQ_COLUMN + ") on delete cascade,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") not null)";

  // Query to create the table for recording archival units metadata.
  protected static final String CREATE_AU_MD_TABLE_QUERY = "create table "
      + AU_MD_TABLE + " ("
      + AU_MD_SEQ_COLUMN + " --BigintSerialPk--,"
      + AU_SEQ_COLUMN + " bigint not null references " + AU_TABLE
      + " (" + AU_SEQ_COLUMN + ") on delete cascade,"
      + MD_VERSION_COLUMN + " smallint not null,"
      + EXTRACT_TIME_COLUMN + " bigint not null"
      + ")";

  // Query to create the table for recording metadata item types.
  protected static final String CREATE_MD_ITEM_TYPE_TABLE_QUERY = "create "
      + "table "
      + MD_ITEM_TYPE_TABLE + " ("
      + MD_ITEM_TYPE_SEQ_COLUMN + " --BigintSerialPk--,"
      + TYPE_NAME_COLUMN + " varchar(" + MAX_TYPE_NAME_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items.
  protected static final String CREATE_MD_ITEM_TABLE_QUERY = "create table "
      + MD_ITEM_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " --BigintSerialPk--,"
      + PARENT_SEQ_COLUMN + " bigint references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + MD_ITEM_TYPE_SEQ_COLUMN + " bigint not null references "
      + MD_ITEM_TYPE_TABLE + " (" + MD_ITEM_TYPE_SEQ_COLUMN + ")"
      + " on delete cascade,"
      + AU_MD_SEQ_COLUMN + " bigint references " + AU_MD_TABLE
      + " (" + AU_MD_SEQ_COLUMN + ") on delete cascade,"
      + DATE_COLUMN + " varchar(" + MAX_DATE_COLUMN + "),"
      + COVERAGE_COLUMN + " varchar(" + MAX_COVERAGE_COLUMN + ")"
      + ")";

  // Query to create the table for recording metadata items names.
  protected static final String CREATE_MD_ITEM_NAME_TABLE_QUERY = "create "
      + "table " + MD_ITEM_NAME_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + NAME_COLUMN + " varchar(" + MAX_NAME_COLUMN + ") not null,"
      + NAME_TYPE_COLUMN + " varchar(" + MAX_NAME_TYPE_COLUMN  + ") not null"
      + ")";

  // Query to create the table for recording metadata keys.
  protected static final String CREATE_MD_KEY_TABLE_QUERY = "create table "
      + MD_KEY_TABLE + " ("
      + MD_KEY_SEQ_COLUMN + " --BigintSerialPk--,"
      + KEY_NAME_COLUMN + " varchar(" + MAX_NAME_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items generic key/value
  // pairs.
  protected static final String CREATE_MD_TABLE_QUERY = "create table "
      + MD_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + MD_KEY_SEQ_COLUMN + " bigint not null references " + MD_KEY_TABLE
      + " (" + MD_KEY_SEQ_COLUMN + ") on delete cascade,"
      + MD_VALUE_COLUMN + " varchar(" + MAX_MD_VALUE_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording bibliographic items.
  protected static final String CREATE_BIB_ITEM_TABLE_QUERY = "create table "
      + BIB_ITEM_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + VOLUME_COLUMN + " varchar(" + MAX_VOLUME_COLUMN + "),"
      + ISSUE_COLUMN + " varchar(" + MAX_ISSUE_COLUMN + "),"
      + START_PAGE_COLUMN + " varchar(" + MAX_START_PAGE_COLUMN + "),"
      + END_PAGE_COLUMN + " varchar(" + MAX_END_PAGE_COLUMN + "),"
      + ITEM_NO_COLUMN + " varchar(" + MAX_ITEM_NO_COLUMN + ")"
      + ")";

  // Query to create the table for recording metadata items URLs.
  protected static final String CREATE_URL_TABLE_QUERY = "create table "
      + URL_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + FEATURE_COLUMN + " varchar(" + MAX_FEATURE_COLUMN + ") not null,"
      + URL_COLUMN + " varchar(" + MAX_URL_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items authors.
  protected static final String CREATE_AUTHOR_TABLE_QUERY = "create table "
      + AUTHOR_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + AUTHOR_NAME_COLUMN + " varchar(" + MAX_AUTHOR_COLUMN + ") not null,"
      + AUTHOR_IDX_COLUMN + " smallint not null"
      + ")";

  // Query to create the table for recording metadata items keywords.
  protected static final String CREATE_KEYWORD_TABLE_QUERY = "create table "
      + KEYWORD_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + KEYWORD_COLUMN + " varchar(" + MAX_KEYWORD_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items DOIs.
  protected static final String CREATE_DOI_TABLE_QUERY = "create table "
      + DOI_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + DOI_COLUMN + " varchar(" + MAX_DOI_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items ISSNs.
  protected static final String CREATE_ISSN_TABLE_QUERY = "create table "
      + ISSN_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + ISSN_COLUMN + " varchar(" + MAX_ISSN_COLUMN + ") not null,"
      + ISSN_TYPE_COLUMN + " varchar(" + MAX_ISSN_TYPE_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items ISBNs.
  protected static final String CREATE_ISBN_TABLE_QUERY = "create table "
      + ISBN_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + ISBN_COLUMN + " varchar(" + MAX_ISBN_COLUMN + ") not null,"
      + ISBN_TYPE_COLUMN + " varchar(" + MAX_ISBN_TYPE_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording publishers.
  protected static final String CREATE_PUBLISHER_TABLE_QUERY = "create table "
      + PUBLISHER_TABLE + " ("
      + PUBLISHER_SEQ_COLUMN + " --BigintSerialPk--,"
      + PUBLISHER_NAME_COLUMN + " varchar(" + MAX_NAME_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording publications.
  protected static final String CREATE_PUBLICATION_TABLE_QUERY = "create table "
      + PUBLICATION_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " --BigintSerialPk--,"
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + PUBLISHER_SEQ_COLUMN + " bigint not null references " + PUBLISHER_TABLE
      + " (" + PUBLISHER_SEQ_COLUMN + ") on delete cascade,"
      + PUBLICATION_ID_COLUMN + " varchar(" + MAX_PUBLICATION_ID_COLUMN + ")"
      + ")";

  // Query to create the table for recording pending AUs to index.
  protected static final String CREATE_PENDING_AU_TABLE_QUERY = "create table "
      + PENDING_AU_TABLE + " ("
      + PLUGIN_ID_COLUMN + " varchar(" + MAX_PLUGIN_ID_COLUMN + ") not null,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") not null,"
      + PRIORITY_COLUMN + " bigint not null)";

  // Query to create the table for recording versions.
  protected static final String CREATE_VERSION_TABLE_QUERY = "create table "
      + VERSION_TABLE + " ("
      + SYSTEM_COLUMN + " varchar(" + MAX_SYSTEM_COLUMN + ") not null,"
      + VERSION_COLUMN + " smallint not null"
      + ")";

  // Query to create the table for recording requests used for COUNTER reports.
  protected static final String REQUEST_TABLE_CREATE_QUERY = "create table "
      + COUNTER_REQUEST_TABLE + " ("
      + URL_COLUMN + " varchar(" + MAX_URL_COLUMN + ") NOT NULL, "
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + REQUEST_DAY_COLUMN + " smallint NOT NULL,"
      + IN_AGGREGATION_COLUMN + " boolean)";

  // Query to create the table for recording book type aggregates (Full vs.
  // Section) used for COUNTER reports.
  protected static final String BOOK_TYPE_AGGREGATES_TABLE_CREATE_QUERY =
      "create table " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PUBLICATION_SEQ_BOOK_TYPE_AGGREGATES"
      + " REFERENCES " + PUBLICATION_TABLE + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + FULL_REQUESTS_COLUMN + " integer,"
      + SECTION_REQUESTS_COLUMN + " integer)";

  // Query to create the table for recording journal type aggregates (PDF vs.
  // HTML) used for COUNTER reports.
  protected static final String JOURNAL_TYPE_AGGREGATES_TABLE_CREATE_QUERY =
      "create table " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PUBLICATION_SEQ_JOURNAL_TYPE_AGGREGATES"
      + " REFERENCES " + PUBLICATION_TABLE + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + TOTAL_REQUESTS_COLUMN + " integer,"
      + HTML_REQUESTS_COLUMN + " integer,"
      + PDF_REQUESTS_COLUMN + " integer)";

  // Query to create the table for recording journal publication year aggregates
  // used for COUNTER reports.
  protected static final String JOURNAL_PUBYEAR_AGGREGATE_TABLE_CREATE_QUERY =
      "create table " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PUBLICATION_SEQ_JOURNAL_PUBYEAR_AGGREGATE"
      + " REFERENCES " + PUBLICATION_TABLE + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + PUBLICATION_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUESTS_COLUMN + " integer NOT NULL)";

  // Query to create the table for platforms.
  protected static final String CREATE_PLATFORM_TABLE_QUERY = "create table "
      + PLATFORM_TABLE + " ("
      + PLATFORM_SEQ_COLUMN + " --BigintSerialPk--,"
      + PLATFORM_NAME_COLUMN + " varchar(" + MAX_PLATFORM_COLUMN + ") not null"
      + ")";

  // Query to create the table for subscriptions.
  protected static final String CREATE_SUBSCRIPTION_TABLE_QUERY =
      "create table "
      + SUBSCRIPTION_TABLE + " ("
      + SUBSCRIPTION_SEQ_COLUMN + " --BigintSerialPk--,"
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PUBLICATION_SEQ_SUBSCRIPTION"
      + " REFERENCES " + PUBLICATION_TABLE + " on delete cascade,"
      + PLATFORM_SEQ_COLUMN + " bigint not null"
      + " CONSTRAINT FK_PLATFORM_SEQ_SUBSCRIPTION"
      + " REFERENCES " + PLATFORM_TABLE + " on delete cascade"
      + ")";

  // Query to create the table for subscription ranges.
  protected static final String CREATE_SUBSCRIPTION_RANGE_TABLE_QUERY =
      "create table "
      + SUBSCRIPTION_RANGE_TABLE + " ("
      + SUBSCRIPTION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_SUBSCRIPTION_SEQ_COLUMN_SUBSCRIPTION_RANGE"
      + " REFERENCES " + SUBSCRIPTION_TABLE + " on delete cascade,"
      + RANGE_COLUMN + " varchar(" + MAX_RANGE_COLUMN + ") not null,"
      + SUBSCRIBED_COLUMN + " boolean not null"
      + ")";

  // Query to create the table for unconfigured Archival Units.
  protected static final String CREATE_UNCONFIGURED_AU_TABLE_QUERY = "create "
      + "table "
      + UNCONFIGURED_AU_TABLE + " ("
      + PLUGIN_ID_COLUMN + " varchar(" + MAX_PLUGIN_ID_COLUMN + ") not null,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") not null"
      + ")";

  // Query to create the table for Archival Unit problems.
  protected static final String CREATE_AU_PROBLEM_TABLE_QUERY = "create table "
      + AU_PROBLEM_TABLE + " ("
      + PLUGIN_ID_COLUMN + " varchar(" + MAX_PLUGIN_ID_COLUMN + ") not null,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") not null,"
      + PROBLEM_COLUMN + " varchar(" + MAX_PROBLEM_COLUMN + ") not null"
      + ")";

  // The SQL code used to create the necessary version 1 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_1_TABLE_CREATE_QUERIES =
      new LinkedHashMap<String, String>() {
	{
	  put(OBSOLETE_METADATA_TABLE, OBSOLETE_CREATE_METADATA_TABLE_QUERY);
	  put(OBSOLETE_TITLE_TABLE, OBSOLETE_CREATE_TITLE_TABLE_QUERY);
	  put(OBSOLETE_PENDINGAUS_TABLE,
	      OBSOLETE_CREATE_PENDINGAUS_TABLE_QUERY);
	  put(OBSOLETE_FEATURE_TABLE, OBSOLETE_CREATE_FEATURE_TABLE_QUERY);
	  put(DOI_TABLE, OBSOLETE_CREATE_DOI_TABLE_QUERY);
	  put(ISBN_TABLE, OBSOLETE_CREATE_ISBN_TABLE_QUERY);
	  put(ISSN_TABLE, OBSOLETE_CREATE_ISSN_TABLE_QUERY);
	  put(OBSOLETE_TITLES_TABLE, OBSOLETE_CREATE_TITLES_TABLE_QUERY);
	  put(OBSOLETE_REQUESTS_TABLE, OBSOLETE_CREATE_REQUESTS_TABLE_QUERY);
	  put(OBSOLETE_PUBYEAR_AGGREGATES_TABLE,
	      OBSOLETE_CREATE_PUBYEAR_AGGREGATES_TABLE_QUERY);
	  put(OBSOLETE_TYPE_AGGREGATES_TABLE,
	      OBSOLETE_CREATE_TYPE_AGGREGATES_TABLE_QUERY);
	}
      };

  // The SQL code used to remove the obsolete version 1 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_1_TABLE_DROP_QUERIES =
      new LinkedHashMap<String, String>() {
	{
	  put(OBSOLETE_TYPE_AGGREGATES_TABLE,
	      dropTableQuery(OBSOLETE_TYPE_AGGREGATES_TABLE));
	  put(OBSOLETE_PUBYEAR_AGGREGATES_TABLE,
	      dropTableQuery(OBSOLETE_PUBYEAR_AGGREGATES_TABLE));
	  put(OBSOLETE_REQUESTS_TABLE, dropTableQuery(OBSOLETE_REQUESTS_TABLE));
	  put(OBSOLETE_TITLES_TABLE, dropTableQuery(OBSOLETE_TITLES_TABLE));
	  put(ISSN_TABLE, dropTableQuery(ISSN_TABLE));
	  put(ISBN_TABLE, dropTableQuery(ISBN_TABLE));
	  put(DOI_TABLE, dropTableQuery(DOI_TABLE));
	  put(OBSOLETE_FEATURE_TABLE, dropTableQuery(OBSOLETE_FEATURE_TABLE));
	  put(OBSOLETE_PENDINGAUS_TABLE,
	      dropTableQuery(OBSOLETE_PENDINGAUS_TABLE));
	  put(OBSOLETE_TITLE_TABLE, dropTableQuery(OBSOLETE_TITLE_TABLE));
	  put(OBSOLETE_METADATA_TABLE, dropTableQuery(OBSOLETE_METADATA_TABLE));
	}
      };

  // The SQL code used to create the necessary version2 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_2_TABLE_CREATE_QUERIES =
      new LinkedHashMap<String, String>() {
    	{
    	  put(PLUGIN_TABLE, CREATE_PLUGIN_TABLE_QUERY);
    	  put(AU_TABLE, CREATE_AU_TABLE_QUERY);
    	  put(AU_MD_TABLE, CREATE_AU_MD_TABLE_QUERY);
    	  put(MD_ITEM_TYPE_TABLE, CREATE_MD_ITEM_TYPE_TABLE_QUERY);
    	  put(MD_ITEM_TABLE, CREATE_MD_ITEM_TABLE_QUERY);
    	  put(MD_ITEM_NAME_TABLE, CREATE_MD_ITEM_NAME_TABLE_QUERY);
    	  put(MD_KEY_TABLE, CREATE_MD_KEY_TABLE_QUERY);
    	  put(MD_TABLE, CREATE_MD_TABLE_QUERY);
    	  put(BIB_ITEM_TABLE, CREATE_BIB_ITEM_TABLE_QUERY);
    	  put(URL_TABLE, CREATE_URL_TABLE_QUERY);
    	  put(AUTHOR_TABLE, CREATE_AUTHOR_TABLE_QUERY);
    	  put(KEYWORD_TABLE, CREATE_KEYWORD_TABLE_QUERY);
    	  put(DOI_TABLE, CREATE_DOI_TABLE_QUERY);
    	  put(ISSN_TABLE, CREATE_ISSN_TABLE_QUERY);
    	  put(ISBN_TABLE, CREATE_ISBN_TABLE_QUERY);
    	  put(PUBLISHER_TABLE, CREATE_PUBLISHER_TABLE_QUERY);
    	  put(PUBLICATION_TABLE, CREATE_PUBLICATION_TABLE_QUERY);
    	  put(PENDING_AU_TABLE, CREATE_PENDING_AU_TABLE_QUERY);
    	  put(VERSION_TABLE, CREATE_VERSION_TABLE_QUERY);
    	  put(COUNTER_REQUEST_TABLE, REQUEST_TABLE_CREATE_QUERY);
    	  put(COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE,
    	      JOURNAL_PUBYEAR_AGGREGATE_TABLE_CREATE_QUERY);
    	  put(COUNTER_BOOK_TYPE_AGGREGATES_TABLE,
    	      BOOK_TYPE_AGGREGATES_TABLE_CREATE_QUERY);
    	  put(COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE,
    	      JOURNAL_TYPE_AGGREGATES_TABLE_CREATE_QUERY);
    	}
          };

  // SQL statements that create the necessary version 1 functions.
  private static final String[] VERSION_1_FUNCTION_CREATE_QUERIES =
      new String[] {
    "create function contentSizeFromUrl(url varchar(4096)) "
    	+ "returns bigint language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getContentSizeFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function contentTypeFromUrl(url varchar(4096)) "
    	+ "returns varchar(512) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getContentTypeFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function eisbnFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getEisbnFromAuId' "
    	+ "parameter style java no sql",

        "create function eisbnFromUrl(url varchar(4096)) "
    	+ "returns varchar(13) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getEisbnFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function eissnFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getEissnFromAuId' "
    	+ "parameter style java no sql",

        "create function eissnFromUrl(url varchar(4096)) "
    	+ "returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getEissnFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function endVolumeFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getEndVolumeFromAuId' "
    	+ "parameter style java no sql",

        "create function endVolumeFromUrl(url varchar(4096)) "
    	+ "returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getEndVolumeFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function endYearFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getEndYearFromAuId' "
    	+ "parameter style java no sql",

        "create function endYearFromUrl(url varchar(4096)) "
    	+ "returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getEndYearFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function generateAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(640) language java external "
        + "name " + "'org.lockss.plugin.PluginManager.generateAuId' "
    	+ "parameter style java no sql",

        "create function formatIsbn(isbn varchar(17)) "
    	+ "returns varchar(17) language java external name "
    	+ "'org.lockss.util.MetadataUtil.formatIsbn' "
    	+ "parameter style java no sql",

        "create function formatIssn(issn varchar(9)) "
    	+ "returns varchar(9) language java external name "
    	+ "'org.lockss.util.MetadataUtil.formatIssn' "
    	+ "parameter style java no sql",

        "create function ingestDateFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getingestDateFromAuId' "
    	+ "parameter style java no sql",

        "create function ingestDateFromUrl(url varchar(4096)) "
    	+ "returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getIngestDateFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function ingestYearFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(4) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getIngestYearFromAuId' "
    	+ "parameter style java no sql",

        "create function ingestYearFromUrl(url varchar(4096)) "
    	+ "returns varchar(4) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getIngestYearFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function isbnFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getPrintIsbnFromAuId' "
    	+ "parameter style java no sql",

        "create function isbnFromUrl(url varchar(4096)) "
    	+ "returns varchar(13) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getIsbnFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function issnFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getPrintIssnFromAuId' "
    	+ "parameter style java no sql",

        "create function issnFromUrl(url varchar(4096)) "
    	+ "returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getIssnFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function issnlFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getIssnLFromAuId' "
    	+ "parameter style java no sql",

        "create function issnlFromUrl(url varchar(4096)) "
    	+ "returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getIssnLFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function printIsbnFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getPrintIsbnFromAuId' "
    	+ "parameter style java no sql",

        "create function printIsbnFromUrl(url varchar(4096)) "
    	+ "returns varchar(13) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getPrintIsbnFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function printIssnFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getPrintIssnFromAuId' "
    	+ "parameter style java no sql",

        "create function printIssnFromUrl(url varchar(4096)) "
    	+ "returns varchar(8) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getPrintIssnFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function publisherFromUrl(url varchar(4096)) "
    	+ "returns varchar(256) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getPublisherFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function publisherFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(256)  language java external "
    	+ "name 'org.lockss.util.SqlStoredProcedures.getPublisherFromAuId' "
    	+ "parameter style java no sql",

        "create function startVolumeFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getStartVolumeFromAuId' "
    	+ "parameter style java no sql",

        "create function startVolumeFromUrl(url varchar(4096)) "
    	+ "returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getStartVolumeFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function startYearFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getStartYearFromAuId' "
    	+ "parameter style java no sql",

        "create function startYearFromUrl(url varchar(4096)) "
    	+ "returns varchar(16) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getStartYearFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function titleFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(256) language java external "
    	+ "name 'org.lockss.util.SqlStoredProcedures.getTitleFromAuId' "
    	+ "parameter style java no sql",

        "create function titleFromIssn(issn varchar(9)) "
    	+ "returns varchar(512) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getTitleFromIssn' "
    	+ "parameter style java no sql",

        "create function titleFromUrl(url varchar(4096)) "
    	+ "returns varchar(512) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getTitleFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function volumeTitleFromAuId(pluginId varchar(128), "
    	+ "auKey varchar(512)) returns varchar(256) language java external "
    	+ "name 'org.lockss.util.SqlStoredProcedures.getVolumeTitleFromAuId' "
    	+ "parameter style java no sql",

        "create function volumeTitleFromIsbn(issn varchar(18)) "
    	+ "returns varchar(512) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getVolumeTitleFromIsbn' "
    	+ "parameter style java no sql",

        "create function volumeTitleFromUrl(url varchar(4096)) "
    	+ "returns varchar(512) language java external name "
    	+ "'org.lockss.util.SqlStoredProcedures.getVolumeTitleFromArticleUrl' "
    	+ "parameter style java no sql",

        "create function yearFromDate(date varchar(16)) returns varchar(4) "
    	+ "language java external name "
    	+ "'org.lockss.util.MetadataUtil.getYearFromDate' "
    	+ "parameter style java no sql", };

  // SQL statements that drop the obsolete version 1 functions.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_1_FUNCTION_DROP_QUERIES =
      new LinkedHashMap<String, String>() {
	{
	  put("contentSizeFromUrl", dropFunctionQuery("contentSizeFromUrl"));
	  put("contentTypeFromUrl", dropFunctionQuery("contentTypeFromUrl"));
	  put("eisbnFromAuId", dropFunctionQuery("eisbnFromAuId"));
	  put("eisbnFromUrl", dropFunctionQuery("eisbnFromUrl"));
	  put("eissnFromAuId", dropFunctionQuery("eissnFromAuId"));
	  put("eissnFromUrl", dropFunctionQuery("eissnFromUrl"));
	  put("endVolumeFromAuId", dropFunctionQuery("endVolumeFromAuId"));
	  put("endVolumeFromUrl", dropFunctionQuery("endVolumeFromUrl"));
	  put("endYearFromAuId", dropFunctionQuery("endYearFromAuId"));
	  put("endYearFromUrl", dropFunctionQuery("endYearFromUrl"));
	  put("formatIsbn", dropFunctionQuery("formatIsbn"));
	  put("formatIssn", dropFunctionQuery("formatIssn"));
	  put("generateAuId", dropFunctionQuery("generateAuId"));
	  put("ingestDateFromAuId", dropFunctionQuery("ingestDateFromAuId"));
	  put("ingestDateFromUrl", dropFunctionQuery("ingestDateFromUrl"));
	  put("ingestYearFromAuId", dropFunctionQuery("ingestYearFromAuId"));
	  put("ingestYearFromUrl", dropFunctionQuery("ingestYearFromUrl"));
	  put("isbnFromAuId", dropFunctionQuery("isbnFromAuId"));
	  put("isbnFromUrl", dropFunctionQuery("isbnFromUrl"));
	  put("issnFromAuId", dropFunctionQuery("issnFromAuId"));
	  put("issnFromUrl", dropFunctionQuery("issnFromUrl"));
	  put("issnlFromAuId", dropFunctionQuery("issnlFromAuId"));
	  put("issnlFromUrl", dropFunctionQuery("issnlFromUrl"));
	  put("printIsbnFromAuId", dropFunctionQuery("printIsbnFromAuId"));
	  put("printIsbnFromUrl", dropFunctionQuery("printIsbnFromUrl"));
	  put("printIssnFromAuId", dropFunctionQuery("printIssnFromAuId"));
	  put("printIssnFromUrl", dropFunctionQuery("printIssnFromUrl"));
	  put("publisherFromAuId", dropFunctionQuery("publisherFromAuId"));
	  put("publisherFromUrl", dropFunctionQuery("publisherFromUrl"));
	  put("startVolumeFromAuId", dropFunctionQuery("startVolumeFromAuId"));
	  put("startVolumeFromUrl", dropFunctionQuery("startVolumeFromUrl"));
	  put("startYearFromAuId", dropFunctionQuery("startYearFromAuId"));
	  put("startYearFromUrl", dropFunctionQuery("startYearFromUrl"));
	  put("titleFromAuId", dropFunctionQuery("titleFromAuId"));
	  put("titleFromIssn", dropFunctionQuery("titleFromIssn"));
	  put("titleFromUrl", dropFunctionQuery("titleFromUrl"));
	  put("volumeTitleFromAuId", dropFunctionQuery("volumeTitleFromAuId"));
	  put("volumeTitleFromIsbn", dropFunctionQuery("volumeTitleFromIsbn"));
	  put("volumeTitleFromUrl", dropFunctionQuery("volumeTitleFromUrl"));
	  put("yearFromDate", dropFunctionQuery("yearFromDate"));
	}
      };

  // SQL statements that create the necessary version 2 functions.
  private static final String[] VERSION_2_FUNCTION_CREATE_QUERIES =
      new String[] {
    "create function contentSizeFromUrl(url varchar(4096)) "
	+ "returns bigint language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getContentSizeFromArticleUrl' "
	+ "parameter style java no sql",

    "create function contentTypeFromUrl(url varchar(4096)) "
	+ "returns varchar(512) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getContentTypeFromArticleUrl' "
	+ "parameter style java no sql",

    "create function eisbnFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getEisbnFromAuId' "
	+ "parameter style java no sql",

    "create function eisbnFromUrl(url varchar(4096)) "
	+ "returns varchar(13) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getEisbnFromArticleUrl' "
	+ "parameter style java no sql",

    "create function eissnFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getEissnFromAuId' "
	+ "parameter style java no sql",

    "create function eissnFromUrl(url varchar(4096)) "
	+ "returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getEissnFromArticleUrl' "
	+ "parameter style java no sql",

    "create function endVolumeFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getEndVolumeFromAuId' "
	+ "parameter style java no sql",

    "create function endVolumeFromUrl(url varchar(4096)) "
	+ "returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getEndVolumeFromArticleUrl' "
	+ "parameter style java no sql",

    "create function endYearFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getEndYearFromAuId' "
	+ "parameter style java no sql",

    "create function endYearFromUrl(url varchar(4096)) "
	+ "returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getEndYearFromArticleUrl' "
	+ "parameter style java no sql",

    "create function generateAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(640) language java external "
	+ "name 'org.lockss.plugin.PluginManager.generateAuId' "
	+ "parameter style java no sql",

    "create function formatIsbn(isbn varchar(17)) "
	+ "returns varchar(17) language java external name "
	+ "'org.lockss.util.MetadataUtil.formatIsbn' "
	+ "parameter style java no sql",

    "create function formatIssn(issn varchar(9)) "
	+ "returns varchar(9) language java external name "
	+ "'org.lockss.util.MetadataUtil.formatIssn' "
	+ "parameter style java no sql",

    "create function ingestDateFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getingestDateFromAuId' "
	+ "parameter style java no sql",

    "create function ingestDateFromUrl(url varchar(4096)) "
	+ "returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getIngestDateFromArticleUrl' "
	+ "parameter style java no sql",

    "create function ingestYearFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(4) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getIngestYearFromAuId' "
	+ "parameter style java no sql",

    "create function ingestYearFromUrl(url varchar(4096)) "
	+ "returns varchar(4) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getIngestYearFromArticleUrl' "
	+ "parameter style java no sql",

    "create function isbnFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getPrintIsbnFromAuId' "
	+ "parameter style java no sql",

    "create function isbnFromUrl(url varchar(4096)) "
	+ "returns varchar(13) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getIsbnFromArticleUrl' "
	+ "parameter style java no sql",

    "create function issnFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getPrintIssnFromAuId' "
	+ "parameter style java no sql",

    "create function issnFromUrl(url varchar(4096)) "
	+ "returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getIssnFromArticleUrl' "
	+ "parameter style java no sql",

    "create function issnlFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getIssnLFromAuId' "
	+ "parameter style java no sql",

    "create function issnlFromUrl(url varchar(4096)) "
	+ "returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getIssnLFromArticleUrl' "
	+ "parameter style java no sql",

    "create function printIsbnFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getPrintIsbnFromAuId' "
	+ "parameter style java no sql",

    "create function printIsbnFromUrl(url varchar(4096)) "
	+ "returns varchar(13) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getPrintIsbnFromArticleUrl' "
	+ "parameter style java no sql",

    "create function printIssnFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getPrintIssnFromAuId' "
	+ "parameter style java no sql",

    "create function printIssnFromUrl(url varchar(4096)) "
	+ "returns varchar(8) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getPrintIssnFromArticleUrl' "
	+ "parameter style java no sql",

    "create function publisherFromUrl(url varchar(4096)) "
	+ "returns varchar(256) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getPublisherFromArticleUrl' "
	+ "parameter style java no sql",

    "create function publisherFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(256)  language java external "
	+ "name 'org.lockss.util.SqlStoredProcedures.getPublisherFromAuId' "
	+ "parameter style java no sql",

    "create function startVolumeFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getStartVolumeFromAuId' "
	+ "parameter style java no sql",

    "create function startVolumeFromUrl(url varchar(4096)) "
	+ "returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getStartVolumeFromArticleUrl' "
	+ "parameter style java no sql",

    "create function startYearFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getStartYearFromAuId' "
	+ "parameter style java no sql",

    "create function startYearFromUrl(url varchar(4096)) "
	+ "returns varchar(16) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getStartYearFromArticleUrl' "
	+ "parameter style java no sql",

    "create function titleFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(256) language java external "
	+ "name 'org.lockss.util.SqlStoredProcedures.getTitleFromAuId' "
	+ "parameter style java no sql",

    "create function titleFromIssn(issn varchar(9)) "
	+ "returns varchar(512) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getTitleFromIssn' "
	+ "parameter style java no sql",

    "create function titleFromUrl(url varchar(4096)) "
	+ "returns varchar(512) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getTitleFromArticleUrl' "
	+ "parameter style java no sql",

    "create function volumeTitleFromAuId(pluginId varchar(128), "
	+ "auKey varchar(512)) returns varchar(256) language java external "
	+ "name 'org.lockss.util.SqlStoredProcedures.getVolumeTitleFromAuId' "
	+ "parameter style java no sql",

    "create function volumeTitleFromIsbn(issn varchar(18)) "
	+ "returns varchar(512) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getVolumeTitleFromIsbn' "
	+ "parameter style java no sql",

    "create function volumeTitleFromUrl(url varchar(4096)) "
	+ "returns varchar(512) language java external name "
	+ "'org.lockss.util.SqlStoredProcedures.getVolumeTitleFromArticleUrl' "
	+ "parameter style java no sql",

    "create function yearFromDate(date varchar(16)) returns varchar(4) "
	+ "language java external name "
	+ "'org.lockss.util.MetadataUtil.getYearFromDate' "
	+ "parameter style java no sql", };

  // SQL statements that create the necessary version 3 indices.
  protected static final String[] VERSION_3_INDEX_CREATE_QUERIES =
    new String[] {
    "create unique index idx1_" + PLUGIN_TABLE + " on " + PLUGIN_TABLE
    + "(" + PLUGIN_ID_COLUMN + ")",

    "create index idx1_" + AU_TABLE + " on " + AU_TABLE
    + "(" + AU_KEY_COLUMN + ")",

    "create index idx1_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + DATE_COLUMN + ")",

    "create index idx1_" + MD_ITEM_NAME_TABLE + " on " + MD_ITEM_NAME_TABLE
    + "(" + NAME_COLUMN + ")",

    "create unique index idx1_" + PUBLISHER_TABLE + " on " + PUBLISHER_TABLE
    + "(" + PUBLISHER_NAME_COLUMN + ")",

    "create index idx1_" + ISSN_TABLE + " on " + ISSN_TABLE
    + "(" + ISSN_COLUMN + ")",

    "create index idx1_" + ISBN_TABLE + " on " + ISBN_TABLE
    + "(" + ISBN_COLUMN + ")",

    "create index idx1_" + URL_TABLE + " on " + URL_TABLE
    + "(" + FEATURE_COLUMN + ")",

    "create index idx2_" + URL_TABLE + " on " + URL_TABLE
    + "(" + URL_COLUMN + ")",

    "create index idx1_" + BIB_ITEM_TABLE + " on " + BIB_ITEM_TABLE
    + "(" + VOLUME_COLUMN + ")",

    "create index idx2_" + BIB_ITEM_TABLE + " on " + BIB_ITEM_TABLE
    + "(" + ISSUE_COLUMN + ")",

    "create index idx3_" + BIB_ITEM_TABLE + " on " + BIB_ITEM_TABLE
    + "(" + START_PAGE_COLUMN + ")",

    "create index idx1_" + AUTHOR_TABLE + " on " + AUTHOR_TABLE
    + "(" + AUTHOR_NAME_COLUMN + ")",

    "create unique index idx1_" + PENDING_AU_TABLE + " on " + PENDING_AU_TABLE
    + "(" + PLUGIN_ID_COLUMN + "," + AU_KEY_COLUMN + ")"
    };

  // The SQL code used to create the necessary version 4 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_4_TABLE_CREATE_QUERIES =
    new LinkedHashMap<String, String>() {{
      put(PLATFORM_TABLE, CREATE_PLATFORM_TABLE_QUERY);
      put(SUBSCRIPTION_TABLE, CREATE_SUBSCRIPTION_TABLE_QUERY);
      put(SUBSCRIPTION_RANGE_TABLE, CREATE_SUBSCRIPTION_RANGE_TABLE_QUERY);
      put(UNCONFIGURED_AU_TABLE, CREATE_UNCONFIGURED_AU_TABLE_QUERY);
    }};

  // SQL statements that create the necessary version 4 indices.
  protected static final String[] VERSION_4_INDEX_CREATE_QUERIES =
    new String[] {
    "create unique index idx1_" + UNCONFIGURED_AU_TABLE
    + " on " + UNCONFIGURED_AU_TABLE
    + "(" + PLUGIN_ID_COLUMN + "," + AU_KEY_COLUMN + ")",
    "create unique index idx1_" + SUBSCRIPTION_RANGE_TABLE
    + " on " + SUBSCRIPTION_RANGE_TABLE
    + "(" + SUBSCRIPTION_SEQ_COLUMN + "," + RANGE_COLUMN + ")"
    };

  // SQL statement that adds the platform reference column to the plugin table.
  private static final String ADD_PLUGIN_PLATFORM_SEQ_COLUMN = "alter table "
      + PLUGIN_TABLE
      + " add column " + PLATFORM_SEQ_COLUMN
      + " bigint references " + PLATFORM_TABLE + " (" + PLATFORM_SEQ_COLUMN
      + ") on delete cascade";

  // Query to update the null platforms of plugins.
  private static final String UPDATE_PLUGIN_NULL_PLATFORM_QUERY = "update "
      + PLUGIN_TABLE
      + " set " + PLATFORM_COLUMN + " = ?"
      + " where " + PLATFORM_COLUMN + " is null";
  
  // SQL statement that obtains all the existing platform names in the plugin
  // table.
  private static final String GET_VERSION_2_PLATFORMS = "select distinct "
      + PLATFORM_COLUMN
      + " from " + PLUGIN_TABLE;

  // Query to add a platform.
  protected static final String INSERT_PLATFORM_QUERY = "insert into "
      + PLATFORM_TABLE
      + "(" + PLATFORM_SEQ_COLUMN
      + "," + PLATFORM_NAME_COLUMN
      + ") values (default,?)";

  // Query to update the platform reference of a plugin.
  private static final String UPDATE_PLUGIN_PLATFORM_SEQ_QUERY = "update "
      + PLUGIN_TABLE
      + " set " + PLATFORM_SEQ_COLUMN + " = ?"
      + " where " + PLATFORM_COLUMN + " = ?";

  // SQL statement that removes the obsolete platform column from the plugin
  // table.
  private static final String REMOVE_OBSOLETE_PLUGIN_PLATFORM_COLUMN = "alter "
      + "table " + PLUGIN_TABLE
      + " drop column " + PLATFORM_COLUMN + " restrict";

  // SQL statement that nulls out publication dates.
  private static final String SET_PUBLICATION_DATES_TO_NULL = "update "
      + MD_ITEM_TABLE
      + " set " + DATE_COLUMN + " = null"
      + " where " + AU_MD_SEQ_COLUMN + " is null";

  // The SQL code used to create the necessary version 5 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_5_TABLE_CREATE_QUERIES =
    new LinkedHashMap<String, String>() {{
      put(AU_PROBLEM_TABLE, CREATE_AU_PROBLEM_TABLE_QUERY);
    }};

  // SQL statements that create the necessary version 5 indices.
  protected static final String[] VERSION_5_INDEX_CREATE_QUERIES =
    new String[] {
    "create index idx1_" + AU_PROBLEM_TABLE
    + " on " + AU_PROBLEM_TABLE
    + "(" + PLUGIN_ID_COLUMN + "," + AU_KEY_COLUMN + ")"
  };
  
  // SQL statement that obtains all the rows from the ISBN table.
  private static final String FIND_ISBNS = "select "
      + MD_ITEM_SEQ_COLUMN
      + "," + ISBN_COLUMN
      + "," + ISBN_TYPE_COLUMN
      + " from " + ISBN_TABLE
      + " order by "+ MD_ITEM_SEQ_COLUMN
      + "," + ISBN_COLUMN
      + "," + ISBN_TYPE_COLUMN;

  // SQL statement that removes all the rows for a given publication ISBN of a
  // given type.
  private static final String REMOVE_ISBN = "delete from " + ISBN_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + ISBN_COLUMN + " = ?"
      + " and " + ISBN_TYPE_COLUMN + " = ?";

  // SQL statement that adds a row for a given publication ISBN of a given type.
  private static final String ADD_ISBN = "insert into " + ISBN_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + ISBN_COLUMN
      + "," + ISBN_TYPE_COLUMN
      + ") values (?, ?, ?)";
  
  // SQL statement that obtains all the rows from the ISSN table.
  private static final String FIND_ISSNS = "select "
      + MD_ITEM_SEQ_COLUMN
      + "," + ISSN_COLUMN
      + "," + ISSN_TYPE_COLUMN
      + " from " + ISSN_TABLE
      + " order by "+ MD_ITEM_SEQ_COLUMN
      + "," + ISSN_COLUMN
      + "," + ISSN_TYPE_COLUMN;

  // SQL statement that removes all the rows for a given publication ISSN of a
  // given type.
  private static final String REMOVE_ISSN = "delete from " + ISSN_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + ISSN_COLUMN + " = ?"
      + " and " + ISSN_TYPE_COLUMN + " = ?";

  // SQL statement that adds a row for a given publication ISSN of a given type.
  private static final String ADD_ISSN = "insert into " + ISSN_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + ISSN_COLUMN
      + "," + ISSN_TYPE_COLUMN
      + ") values (?, ?, ?)";

  // SQL statements that create the necessary version 6 indices.
  protected static final String[] VERSION_6_INDEX_CREATE_QUERIES =
    new String[] {
    "create unique index idx2_" + ISBN_TABLE
    + " on " + ISBN_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + "," + ISBN_COLUMN + "," + ISBN_TYPE_COLUMN
    + ")",
    "create unique index idx2_" + ISSN_TABLE
    + " on " + ISSN_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + "," + ISSN_COLUMN + "," + ISSN_TYPE_COLUMN
    + ")"
  };

  // SQL statements that create the necessary version 7 indices.
  protected static final String[] VERSION_7_INDEX_CREATE_QUERIES =
    new String[] {
    "create index idx2_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + PARENT_SEQ_COLUMN + ")",
    "create index idx3_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + AU_MD_SEQ_COLUMN + ")",
    "create index idx1_" + PUBLICATION_TABLE + " on " + PUBLICATION_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")"
  };
  
  // SQL statement that creates a database in PostgreSQL.
  private static final String CREATE_DATABASE_QUERY_PG =
      "create database \"--databaseName--\" with template template0";
  
  // SQL statement that creates a schema in PostgreSQL.
  private static final String CREATE_SCHEMA_QUERY_PG =
      "create schema \"--schemaName--\"";

  // SQL statement that finds a database in PostgreSQL.
  private static final String FIND_DATABASE_QUERY_PG = "select datname"
      + " from pg_catalog.pg_database where datname = ?";
  
  // SQL statement that finds a schema in PostgreSQL.
  private static final String FIND_SCHEMA_QUERY_PG = "select nspname"
      + " from pg_namespace where nspname = ?";

  // Database metadata keys.
  private static final String COLUMN_NAME = "COLUMN_NAME";
  private static final String COLUMN_SIZE = "COLUMN_SIZE";
  private static final String FUNCTION_NAME = "FUNCTION_NAME";
  private static final String TYPE_NAME = "TYPE_NAME";
  protected static final String[] TABLE_TYPES = new String[] {"TABLE"};

  private static final String DATABASE_VERSION_TABLE_SYSTEM = "database";

  // Query to get the database version.
  private static final String GET_DATABASE_VERSION_QUERY = "select "
      + VERSION_COLUMN
      + " from " + VERSION_TABLE
      + " where " + SYSTEM_COLUMN + " = '" + DATABASE_VERSION_TABLE_SYSTEM
      + "'";

  // Query to insert a type of metadata item.
  protected static final String INSERT_MD_ITEM_TYPE_QUERY = "insert into "
      + MD_ITEM_TYPE_TABLE
      + "(" + MD_ITEM_TYPE_SEQ_COLUMN
      + "," + TYPE_NAME_COLUMN
      + ") values (default,?)";

  // Query to insert the database version.
  protected static final String INSERT_VERSION_QUERY = "insert into "
      + VERSION_TABLE
      + "(" + SYSTEM_COLUMN
      + "," + VERSION_COLUMN
      + ") values (?,?)";

  // Query to update the database version.
  private static final String UPDATE_DB_VERSION_QUERY = "update "
      + VERSION_TABLE
      + " set " + VERSION_COLUMN + " = ?"
      + " where " + SYSTEM_COLUMN + " = ?";

  // Derby SQL state of exception thrown on successful database shutdown.
  private static final String SHUTDOWN_SUCCESS_STATE_CODE = "08006";

  // An indication of whether this object has been enabled.
  private boolean dbManagerEnabled = DEFAULT_DBMANAGER_ENABLED;

  // The database data source.
  private DataSource dataSource = null;

  // The data source configuration.
  private Configuration dataSourceConfig = null;

  // The data source class name.
  private String dataSourceClassName = null;

  // The data source database name.
  private String dataSourceDbName = null;

  // The data source user.
  private String dataSourceUser = null;

  // The data source password.
  private String dataSourcePassword = null;

  // The network server control.
  private NetworkServerControl networkServerControl = null;

  // An indication of whether this object is ready to be used.
  private boolean ready = false;

  // The version of the database to be targeted by this daemon.
  //
  // After this service has started successfully, this is the version of the
  // database that will be in place, as long as the database version prior to
  // starting the service was not higher already.
  private int targetDatabaseVersion = 7;

  // The maximum number of retries to be attempted when encountering transient
  // SQL exceptions.
  private int maxRetryCount = DEFAULT_MAX_RETRY_COUNT;

  // The interval to wait between consecutive retries when encountering
  // transient SQL exceptions.
  private long retryDelay = DEFAULT_RETRY_DELAY;

  // An indication of whether the database was booted.
  private boolean dbBooted = false;

  /**
   * Starts the DbManager service.
   */
  @Override
  public void startService() {
    final String DEBUG_HEADER = "startService(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Do nothing if not enabled
    if (!dbManagerEnabled) {
      log.info("DbManager not enabled.");
      return;
    }

    // Do nothing more if it is already initialized.
    log.debug2(DEBUG_HEADER + "dataSource != null = " + (dataSource != null));
    ready = ready && dataSource != null;
    if (ready) {
      return;
    }

    try {
      // Set up the database infrastructure.
      setUpInfrastructure();

      // Update the existing database if necessary.
      updateDatabaseIfNeeded(targetDatabaseVersion);

      ready = true;
    } catch (DbException dbe) {
      log.error(dbe.getMessage() + " - DbManager not ready", dbe);
      // Do nothing more if the database infrastructure cannot be setup.
      dataSource = null;
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "DbManager ready? = " + ready);
  }

  /**
   * Handler of configuration changes.
   * 
   * @param config
   *          A Configuration with the new configuration.
   * @param prevConfig
   *          A Configuration with the previous configuration.
   * @param changedKeys
   *          A Configuration.Differences with the keys of the configuration
   *          elements that have changed.
   */
  @Override
  public void setConfig(Configuration config, Configuration prevConfig,
			Configuration.Differences changedKeys) {
    final String DEBUG_HEADER = "setConfig(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (changedKeys.contains(PREFIX)) {
      // Update the reconfigured parameters.
      maxRetryCount =
	  config.getInt(PARAM_MAX_RETRY_COUNT, DEFAULT_MAX_RETRY_COUNT);

      retryDelay =
	  config.getTimeInterval(PARAM_RETRY_DELAY, DEFAULT_RETRY_DELAY);

      dbManagerEnabled =
	  config.getBoolean(PARAM_DBMANAGER_ENABLED, DEFAULT_DBMANAGER_ENABLED);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Sets up the database infrastructure.
   * 
   * @throws DbException
   *           if any problem occurred setting up the database infrastructure.
   */
  private void setUpInfrastructure() throws DbException {
    final String DEBUG_HEADER = "setUpInfrastructure(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Get the datasource configuration.
    dataSourceConfig = getDataSourceConfig();

    // Check whether the Derby database is being used.
    if (isTypeDerby()) {
      // Yes: Set up the Derby database properties.
      setDerbyDatabaseConfiguration();
    }

    // Check whether authentication is required and it is not available.
    if (StringUtil.isNullString(dataSourceUser)
	|| (isTypeDerby()
	&& !"org.apache.derby.jdbc.EmbeddedDataSource"
	    .equals(dataSourceClassName)
	&& !"org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource"
	    .equals(dataSourceClassName)
	&& StringUtil.isNullString(dataSourcePassword))) {
      // Yes: Report the problem.
      throw new DbException("Missing required authentication");
    }

    // No: Create the datasource.
    dataSource = createDataSource(dataSourceConfig.get("className"));

    // Check whether the PostgreSQL database is being used.
    if (isTypePostgresql()) {
      // Yes: Initialize the database, if necessary.
      initializePostgresqlDbIfNeeded(dataSourceConfig);
    }

    // Initialize the datasource properties.
    initializeDataSourceProperties(dataSourceConfig, dataSource);

    // Check whether the Derby database is being used.
    if (isTypeDerby()) {
      // Yes: Check whether the Derby NetworkServerControl for client
      // connections needs to be started.
      if (dataSource instanceof ClientDataSource) {
	// Yes: Start it.
	ClientDataSource cds = (ClientDataSource)dataSource;
	startDerbyNetworkServerControl(cds);

	// Set up the Derby authentication configuration, if necessary.
	setUpDerbyAuthentication(dataSourceConfig, cds);
      } else {
	// No: Remove the Derby authentication configuration, if necessary.
	removeDerbyAuthentication(dataSourceConfig, dataSource);
      }

      // Remember that the Derby database has been booted.
      dbBooted = true;

      // No: Check whether the PostgreSQL database is being used.
    } else if (isTypePostgresql()) {
      // Yes: Get the name of the database from the configuration.
      String databaseName = dataSourceConfig.get("databaseName");
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "databaseName = " + databaseName);

      // Create the schema if it does not exist.
      createPostgresqlSchemaIfMissing(databaseName, dataSource);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database to a given version, if necessary.
   * 
   * @param targetDbCersion
   *          An int with the database version that is the target of the update.
   * 
   * @throws DbException
   *           if any problem occurred updating the database.
   */
  private void updateDatabaseIfNeeded(int targetDbVersion) throws DbException {
    final String DEBUG_HEADER = "updateDatabaseIfNeeded(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "targetDbVersion = " + targetDbVersion);

    Connection conn = null;

    try {
      conn = getConnectionBeforeReady();

      // Find the current database version.
      int existingDbVersion = 0;

      if (tableExistsBeforeReady(conn, VERSION_TABLE)) {
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + VERSION_TABLE + " table exists.");
	existingDbVersion = getDatabaseVersionBeforeReady(conn);
      } else if (tableExistsBeforeReady(conn, OBSOLETE_METADATA_TABLE)){
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + OBSOLETE_METADATA_TABLE + " table exists.");
	existingDbVersion = 1;
      }

      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "existingDbVersion = "
	  + existingDbVersion);

      // Check whether the database needs to be updated.
      if (targetDbVersion > existingDbVersion) {
	// Yes.
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Database needs to be updated from existing version "
	    + existingDbVersion + " to new version "
	    + targetDbVersion);

	// Update the database.
	updateDatabase(conn, existingDbVersion, targetDbVersion);
	log.info("Database has been updated to version " + targetDbVersion);

	// No: Check whether the database is already up-to-date.
      } else if (targetDbVersion == existingDbVersion) {
	// Yes: Nothing more to do.
	log.info("Database is up-to-date (version " + targetDbVersion
	         + ")");
      } else {
	// No: The existing database is newer than what this version of the
	// daemon expects: Disable the use of the database.
	throw new DbException("Existing database is version "
	    + existingDbVersion
	    + ", which is higher than the target database version "
	    + targetDbVersion + " for this daemon.");
      }
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides a database connection using the default datasource, retrying the
   * operation in the default manner in case of transient failures. To be used
   * during initialization.
   * <p />
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @return a Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred getting the connection.
   */
  private Connection getConnectionBeforeReady() throws DbException {
    return getConnectionBeforeReady(dataSource, maxRetryCount, retryDelay);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation in the default manner in case of transient failures. To be used
   * during initialization.
   * <p />
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @return a Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred getting the connection.
   */
  protected Connection getConnectionBeforeReady(DataSource ds)
      throws DbException {
    return getConnectionBeforeReady(ds, maxRetryCount, retryDelay);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation with the specified parameters in case of transient failures. To
   * be used during initialization.
   * <p />
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Connection getConnectionBeforeReady(DataSource ds, int maxRetryCount,
      long retryDelay) throws DbException {
    final String DEBUG_HEADER = "getConnectionBeforeReady(): ";
    if (log.isDebug2()) {
      log.debug2("maxRetryCount = " + maxRetryCount);
      log.debug2("retryDelay = " + retryDelay);
    }

    boolean success = false;
    int retryCount = 0;
    Connection conn = null;

    // Keep trying until success.
    while (!success) {
      try {
	// Get the connection.
	if (ds instanceof javax.sql.ConnectionPoolDataSource) {
	  conn = ((javax.sql.ConnectionPoolDataSource) ds)
	      .getPooledConnection().getConnection();
	} else {
	  conn = ds.getConnection();
    	}

	// Use transactions by  default.
	conn.setAutoCommit(false);
	success = true;
      } catch (SQLTransientException sqlte) {
	// A SQLTransientException is caught: Count the next retry.
	retryCount++;

	// Check whether the next retry would go beyond the specified maximum
	// number of retries.
	if (retryCount > maxRetryCount) {
	  // Yes: Report the failure.
	  log.error("Transient exception caught", sqlte);
	  log.error("Maximum retry count of " + maxRetryCount + " reached.");
	  throw new DbException("Cannot get a database connection", sqlte);
	} else {
	  // No: Wait for the specified amount of time before attempting the
	  // next retry.
	  log.debug(DEBUG_HEADER + "Exception caught", sqlte);
	  log.debug(DEBUG_HEADER + "Waiting "
	      	    + StringUtil.timeIntervalToString(retryDelay)
	      	    + " before retry number " + retryCount + "...");

	  try {
	    Deadline.in(retryDelay).sleep();
	  } catch (InterruptedException ie) {
	    // Continue with the next retry.
	  }
	}
      } catch (SQLException sqle) {
	// A non-transient exception is caught: Report the problem.
	throw new DbException("Cannot get a database connection", sqle);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return conn;
  }

  /**
   * Provides an indication of whether a table exists. To be used during
   * initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with name of the table to be checked.
   * @return <code>true</code> if the named table exists, <code>false</code>
   *         otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private boolean tableExistsBeforeReady(Connection conn, String tableName)
      throws DbException {
    final String DEBUG_HEADER = "tableExistsBeforeReady(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "tableName = " + tableName);

    if (conn == null) {
      throw new DbException("Null connection");
    }

    boolean result = false;
    ResultSet resultSet = null;

    try {
      // Get the database schema table data.
      if (isTypeDerby()) {
	resultSet = conn.getMetaData().getTables(null, dataSourceUser,
	    tableName.toUpperCase(), TABLE_TYPES);
      } else if (isTypePostgresql()) {
	resultSet = conn.getMetaData().getTables(null, dataSourceUser,
	    tableName.toLowerCase(), TABLE_TYPES);
      }

      // Determine whether the table exists.
      result = resultSet.next();
    } catch (SQLException sqle) {
	throw new DbException("Cannot determine whether table '" + tableName
	    + "' exists", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Get the database version to be used during initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return an int with the database version.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  protected int getDatabaseVersionBeforeReady(Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "getDatabaseVersionBeforeReady(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    int version = 1;
    PreparedStatement stmt = null;
    ResultSet resultSet = null;

    try {
      stmt = prepareStatementBeforeReady(conn, GET_DATABASE_VERSION_QUERY);
      resultSet = executeQueryBeforeReady(stmt);

      if (resultSet.next()) {
	version = resultSet.getShort(VERSION_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "version = " + version);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the database version", sqle);
      log.error("SQL = '" + GET_DATABASE_VERSION_QUERY + "'.");
      throw new DbException("Cannot get the database version", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    log.debug2(DEBUG_HEADER + "version = " + version);
    return version;
  }

  /**
   * Updates the database to the target version.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param existingDatabaseVersion
   *          An int with the existing database version.
   * @param finalDatabaseVersion
   *          An int with the version of the database to which the database is
   *          to be updated.
   * @return <code>true</code> if the database was successfully updated,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred updating the database.
   */
  private void updateDatabase(Connection conn, int existingDatabaseVersion,
      int finalDatabaseVersion) throws DbException {
    final String DEBUG_HEADER = "updateDatabase(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "existingDatabaseVersion = "
	  + existingDatabaseVersion);
      log.debug2(DEBUG_HEADER + "finalDatabaseVersion = "
	  + finalDatabaseVersion);
    }

    // Loop through all the versions to be updated to reach the targeted
    // version.
    for (int from = existingDatabaseVersion; from < finalDatabaseVersion;
	from++) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Updating from version " + from + "...");

      // Assume failure.
      boolean success = false;

      try {
	// Perform the appropriate update for this version.
	if (from == 0) {
	  setUpDatabaseVersion1(conn);
	} else if (from == 1) {
	  updateDatabaseFrom1To2(conn);
	} else if (from == 2) {
	  updateDatabaseFrom2To3(conn);
	} else if (from == 3) {
	  updateDatabaseFrom3To4(conn);
	} else if (from == 4) {
	  updateDatabaseFrom4To5(conn);
	} else if (from == 5) {
	  updateDatabaseFrom5To6(conn);
	} else if (from == 6) {
	  updateDatabaseFrom6To7(conn);
	} else {
	  throw new DbException("Non-existent method to update the database "
	      + "from version " + from + ".");
	}

	success = true;
      } finally {
	// Check whether the partial update was successful.
	if (success) {
	  // Yes: Check whether the updated database is at least at version 2.
	  if (from > 0) {
	    // Yes: Record the current database version in the database.
	    recordDbVersion(conn, from + 1);
	  }

	  // Commit this partial update.
	  DbManager.commitOrRollback(conn, log);
	} else {
	  // No: Do not continue with subsequent updates.
	  break;
	}
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 1 to version 2.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred updating the database.
   */
  private void updateDatabaseFrom1To2(Connection conn) throws DbException {
    final String DEBUG_HEADER = "updateDatabaseFrom1To2(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Remove obsolete database tables.
    removeVersion1ObsoleteTablesIfPresent(conn);

    // Create the necessary tables if they do not exist.
    createTablesIfMissing(conn, VERSION_2_TABLE_CREATE_QUERIES);

    // Initialize necessary data in new tables.
    addMetadataItemType(conn, MD_ITEM_TYPE_BOOK_SERIES);
    addMetadataItemType(conn, MD_ITEM_TYPE_BOOK);
    addMetadataItemType(conn, MD_ITEM_TYPE_BOOK_CHAPTER);
    addMetadataItemType(conn, MD_ITEM_TYPE_JOURNAL);
    addMetadataItemType(conn, MD_ITEM_TYPE_JOURNAL_ARTICLE);

    if (isTypeDerby()) {
      // Remove old functions.
      removeVersion1FunctionsIfPresent(conn);

      // Create new functions.
      createVersion2FunctionsIfMissing(conn);
    }

    if (log.isDebug2())  log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes all the obsolete version 1 database tables if they exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred removing the tables.
   */
  private void removeVersion1ObsoleteTablesIfPresent(Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "removeVersion1ObsoleteTablesIfPresent(): ";

      // Loop through all the table names.
    for (String tableName : VERSION_1_TABLE_DROP_QUERIES.keySet()) {
      log.debug2(DEBUG_HEADER + "Checking table = " + tableName);

      // Remove the table if it does exist.
      removeTableIfPresent(conn, tableName,
                           VERSION_1_TABLE_DROP_QUERIES.get(tableName));
    }
  }

  /**
   * Creates the necessary database tables if they do not exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableMap
   *          A Map<String, String> with the creation queries indexed by table
   *          name.
   * @throws DbException
   *           if any problem occurred creating the tables.
   */
  private void createTablesIfMissing(Connection conn,
      Map<String, String> tableMap) throws DbException {
    final String DEBUG_HEADER = "createTablesIfMissing(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "tableMap = " + tableMap);

    // Loop through all the table names.
    for (String tableName : tableMap.keySet()) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "tableName = " + tableName);

      // Create the table if it does not exist.
      createTableIfMissingBeforeReady(conn, tableName,
	  			localizeCreateQuery(tableMap.get(tableName)));
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Done with tableName '" + tableName + "'.");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds a metadata item type to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param typeName
   *          A String with the name of the type to be added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void addMetadataItemType(Connection conn, String typeName)
      throws DbException {
    final String DEBUG_HEADER = "addMetadataItemType(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "typeName = " + typeName);

    // Ignore an empty metadata item type.
    if (StringUtil.isNullString(typeName)) {
      return;
    }

    PreparedStatement insertMetadataItemType = null;

    try {
      insertMetadataItemType =
	  prepareStatementBeforeReady(conn, INSERT_MD_ITEM_TYPE_QUERY);
      insertMetadataItemType.setString(1, typeName);

      int count = executeUpdateBeforeReady(insertMetadataItemType);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot add a metadata item type", sqle);
      log.error("typeName = '" + typeName + "'.");
      log.error("SQL = '" + INSERT_MD_ITEM_TYPE_QUERY + "'.");
      throw new DbException("Cannot add a metadata item type", sqle);
    } finally {
      DbManager.safeCloseStatement(insertMetadataItemType);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes all the obsolete version 1 database functions if they exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred removing the functions.
   */
  private void removeVersion1FunctionsIfPresent(Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "removeVersion1FunctionsIfPresent(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Loop through all the function names.
    for (String functionName : VERSION_1_FUNCTION_DROP_QUERIES.keySet()) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Checking function = " + functionName);

      // Remove the function if it does exist.
      removeFunctionIfPresentBeforeReady(conn, functionName,
					 VERSION_1_FUNCTION_DROP_QUERIES
					     .get(functionName));

      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Removed function = " + functionName);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates all the necessary version 2 database functions if they do not
   * exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred creating the functions.
   */
  private void createVersion2FunctionsIfMissing(Connection conn)
      throws DbException {
    // Create the functions.
    executeBatchBeforeReady(conn, VERSION_2_FUNCTION_CREATE_QUERIES);
  }

  /**
   * Deletes a database table if it does exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table to delete, if present.
   * @param tableDropSql
   *          A String with the SQL code used to drop the table, if present.
   * @return <code>true</code> if the table did exist and it was removed,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred removing the table.
   */
  private boolean removeTableIfPresent(Connection conn, String tableName,
      String tableDropSql) throws DbException {
    final String DEBUG_HEADER = "removeTableIfPresent(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableDropSql = '" + tableDropSql + "'.");
    }

    if (conn == null) {
      throw new DbException("Null connection.");
    }

    // Check whether the table needs to be removed.
    if (tableExistsBeforeReady(conn, tableName)) {
      // Yes: Delete it.
      executeBatchBeforeReady(conn, new String[] { tableDropSql });
      log.debug2(DEBUG_HEADER + "Dropped table '" + tableName + "'.");
      return true;
    } else {
      // No.
      log.debug2(DEBUG_HEADER + "Table '" + tableName
	  + "' does not exist - Not dropping it.");
      return false;
    }
  }

  /**
   * Creates a database table if it does not exist. To be used during
   * initialization.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table to create, if missing.
   * @param tableCreateSql
   *          A String with the SQL code used to create the table, if missing.
   * @return <code>true</code> if the table did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred creating the table.
   */
  // TODO: If the table exists, verify that it matches the table to be created.
  private boolean createTableIfMissingBeforeReady(Connection conn,
      String tableName, String tableCreateSql) throws DbException {
    final String DEBUG_HEADER = "createTableIfMissingBeforeReady(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableCreateSql = '" + tableCreateSql + "'.");
    }

    if (conn == null) {
      throw new DbException("Null connection.");
    }

    // Check whether the table needs to be created.
    if (!tableExistsBeforeReady(conn, tableName)) {
      // Yes: Create it.
      createTableBeforeReady(conn, tableName, tableCreateSql);
      return true;
    } else {
      // No.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Table '" + tableName
	  + "' exists - Not creating it.");
      return false;
    }
  }

  /**
   * Creates a database table.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table to create, if missing.
   * @param tableCreateSql
   *          A String with the SQL code used to create the table, if missing.
   * @return <code>true</code> if the table did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws DbMigratorException
   *           if any problem occurred creating the table.
   */
  protected void createTableBeforeReady(Connection conn, String tableName,
      String tableCreateSql) throws DbException {
    final String DEBUG_HEADER = "createTable(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableCreateSql = '" + tableCreateSql + "'.");
    }

    if (conn == null) {
      throw new DbException("Null connection.");
    }

    PreparedStatement statement = null;

    // Create the table.
    try {
      statement = prepareStatementBeforeReady(conn,
	  localizeCreateQuery(tableCreateSql));
      int count = executeUpdateBeforeReady(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } finally {
      DbManager.safeCloseStatement(statement);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "Table '" + tableName + "' created.");

    logTableSchemaBeforeReady(conn, tableName);
  }

  /**
   * Deletes a database function if it does exist. To be used during
   * initialization.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param functionName
   *          A String with the name of the function to delete, if present.
   * @param functionDropSql
   *          A String with the SQL code used to drop the function, if present.
   * @return <code>true</code> if the function did exist and it was removed,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred removing the functions.
   */
  private boolean removeFunctionIfPresentBeforeReady(Connection conn,
      String functionName, String functionDropSql) throws DbException {
    final String DEBUG_HEADER = "removeFunctionIfPresentBeforeReady(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "functionName = '" + functionName + "'.");
      log.debug2(DEBUG_HEADER + "functionDropSql = '" + functionDropSql + "'.");
    }

    if (conn == null) {
      throw new DbException("Null connection.");
    }

    // Check whether the function needs to be removed.
    if (functionExistsBeforeReady(conn, functionName)) {
      // Yes: Delete it.
      executeBatchBeforeReady(conn, new String[] { functionDropSql });
      log.debug2(DEBUG_HEADER + "Dropped function '" + functionName + "'.");
      return true;
    } else {
      // No.
      log.debug2(DEBUG_HEADER + "Function '" + functionName
	  + "' does not exist - Not dropping it.");
      return false;
    }
  }

  /**
   * Executes a batch of statements. To be used during initialization.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param stmts
   *          A String[] with the statements to be executed.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void executeBatchBeforeReady(Connection conn, String[] stmts)
      throws DbException {
    final String DEBUG_HEADER = "executeBatchBeforeReady(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "stmts.length = '" + stmts.length + "'.");

    if (conn == null) {
      throw new DbException("Null connection.");
    }

    Statement statement = null;

    try {
      statement = conn.createStatement();

      // Loop through all the statements.
      for (String stmt : stmts) {
	// Add each statement to the batch.
	if (log.isDebug3()) log.debug2(DEBUG_HEADER + "stmt = '" + stmt + "'.");
	statement.addBatch(stmt);
      }

      // Execute the batch.
      statement.executeBatch();
    } catch (SQLException sqle) {
      log.error("Cannot execute batch", sqle);
      log.error("stmts = '" + stmts + "'.");
      throw new DbException("Cannot execute batch", sqle);
    } finally {
      DbManager.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides an indication of whether a function exists. To be used during
   * initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param functionName
   *          A String with name of the function to be checked.
   * @return <code>true</code> if the named function exists, <code>false</code>
   *         otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private boolean functionExistsBeforeReady(Connection conn,
      String functionName) throws DbException {
    final String DEBUG_HEADER = "functionExistsBeforeReady(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "functionName = '" + functionName + "'.");

    if (conn == null) {
      throw new DbException("Null connection.");
    }

    ResultSet resultSet = null;

    try {
      // Get the database schema function data.
      resultSet = conn.getMetaData().getFunctions(null, dataSourceUser, null);

      // Loop through each function found.
      while (resultSet.next()) {
	// Check whether this is the function sought.
	if (functionName.toUpperCase().equals(resultSet
	                                      .getString(FUNCTION_NAME))) {
	  // Found the function: No need to check further.
	  if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true.");
	  return true;
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot determine whether a function exists", sqle);
      log.error("functionName = '" + functionName + "'.");
      throw new DbException("Cannot determine whether a function exists", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
    }

    // The function does not exist.
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false.");
    return false;
  }

  /**
   * Records in the database the database version.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param version
   *          An int with version to be recorded.
   * @return an int with the number of database rows recorded.
   * @throws DbException
   *           if any problem occurred recording the database version.
   */
  private int recordDbVersion(Connection conn, int version)
      throws DbException {
    final String DEBUG_HEADER = "recordDbVersion(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "version = " + version);

    // Try to update the version.
    int count = updateDbVersion(conn, version);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "updatedCount = " + count);

    // Check whether the update was successful.
    if (count > 0) {
      // Yes: Done.
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "updatedCount = " + count);
      return count;
    }

    // No: Add the version.
    count = addDbVersion(conn, version);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "addedCount = " + count);
    return count;
  }

  /**
   * Updates in the database the database version.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param version
   *          An int with version to be updated.
   * @return an int with the number of database rows updated.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int updateDbVersion(Connection conn, int version) throws DbException {
    final String DEBUG_HEADER = "updateDbVersion(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "version = " + version);

    int updatedCount = 0;
    PreparedStatement updateVersion = null;

    try {
      updateVersion =
	  prepareStatementBeforeReady(conn, UPDATE_DB_VERSION_QUERY);
      updateVersion.setShort(1, (short)version);
      updateVersion.setString(2, DATABASE_VERSION_TABLE_SYSTEM);

      updatedCount = executeUpdateBeforeReady(updateVersion);
    } catch (SQLException sqle) {
      log.error("Cannot update the database version", sqle);
      log.error("SQL = '" + UPDATE_DB_VERSION_QUERY + "'.");
      log.error("version = " + version + ".");
      log.error("DATABASE_VERSION_TABLE_SYSTEM = '"
	  + DATABASE_VERSION_TABLE_SYSTEM + "'.");
      throw new DbException("Cannot update the database version", sqle);
    } finally {
      DbManager.safeCloseStatement(updateVersion);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "updatedCount = " + updatedCount);
    return updatedCount;
  }

  /**
   * Adds to the database the database version.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param version
   *          An int with version to be updated.
   * @return an int with the number of database rows added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int addDbVersion(Connection conn, int version) throws DbException {
    final String DEBUG_HEADER = "addDbVersion(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "version = " + version);

    int addedCount = 0;
    PreparedStatement insertVersion = null;

    try {
      insertVersion =
	  prepareStatementBeforeReady(conn, INSERT_VERSION_QUERY);
      insertVersion.setString(1, DATABASE_VERSION_TABLE_SYSTEM);
      insertVersion.setShort(2, (short)version);

      addedCount = executeUpdateBeforeReady(insertVersion);
    } catch (SQLException sqle) {
      log.error("Cannot add the database version", sqle);
      log.error("SQL = '" + INSERT_VERSION_QUERY + "'.");
      log.error("version = " + version + ".");
      log.error("DATABASE_VERSION_TABLE_SYSTEM = '"
	  + DATABASE_VERSION_TABLE_SYSTEM + "'.");
      throw new DbException("Cannot add the database version", sqle);
    } finally {
      DbManager.safeCloseStatement(insertVersion);
    }

    if (log.isDebug2()) log.debug(DEBUG_HEADER + "addedCount = " + addedCount);
    return addedCount;
  }

  /**
   * Provides an indication of whether this object is ready to be used.
   * 
   * @return <code>true</code> if this object is ready to be used,
   *         <code>false</code> otherwise.
   */
  public boolean isReady() {
    return ready;
  }

  /**
   * Provides a database connection using the default datasource, retrying the
   * operation in the default manner in case of transient failures.
   * <p />
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @return a Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred getting the connection.
   */
  public Connection getConnection() throws DbException {
    return getConnection(maxRetryCount, retryDelay);
  }

  /**
   * Provides a database connection using the default datasource, retrying the
   * operation with the specified parameters in case of transient failures.
   * <p />
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a Connection with the database connection to be used.
   * @throws DbException
   *           if this object is not ready or any problem occurred getting the
   *           connection.
   */
  public Connection getConnection(int maxRetryCount, long retryDelay)
      throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    return getConnectionBeforeReady(dataSource, maxRetryCount, retryDelay);
  }

  /**
   * Commits a connection or rolls it back if it's not possible.
   * 
   * @param conn
   *          A connection with the database connection to be committed.
   * @param logger
   *          A Logger used to report errors.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public static void commitOrRollback(Connection conn, Logger logger)
      throws DbException {
    try {
      conn.commit();
    } catch (SQLException sqle) {
      logger.error("Exception caught committing the connection", sqle);
      DbManager.safeRollbackAndClose(conn);
      throw new DbException("Cannot commit the transaction", sqle);
    }
  }

  /**
   * Rolls back a transaction.
   * 
   * @param conn
   *          A connection with the database connection to be rolled back.
   * @param logger
   *          A Logger used to report errors.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  public static void rollback(Connection conn, Logger logger)
      throws DbException {
    try {
      conn.rollback();
    } catch (SQLException sqle) {
      logger.error("Exception caught rolling back the connection", sqle);
      DbManager.safeRollbackAndClose(conn);
      throw new DbException("Cannot roll back the transaction", sqle);
    }
  }

  /**
   * Creates a database table if it does not exist.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with the name of the table to create, if missing.
   * @param tableCreateSql
   *          A String with the SQL code used to create the table, if missing.
   * @return <code>true</code> if the table did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if this object is not ready or any problem occurred creating the
   *           table.
   */
  boolean createTableIfMissing(Connection conn, String tableName,
      String tableCreateSql) throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    return createTableIfMissingBeforeReady(conn, tableName, tableCreateSql);
  }

  /**
   * Executes a batch of statements.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param stmts
   *          A String[] with the statements to be executed.
   * @throws DbException
   *           if this object is not ready or any problem occurred executing the
   *           batch.
   */
  public void executeBatch(Connection conn, String[] stmts) throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    executeBatchBeforeReady(conn, stmts);
  }

  /**
   * Provides the database version.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return an int with the database version.
   * @throws DbException
   *           if this object is not ready or any problem occurred getting the
   *           database version.
   */
  public int getDatabaseVersion(Connection conn) throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    return getDatabaseVersionBeforeReady(conn);
  }

  /**
   * Writes the named table schema to the log. For debugging purposes only.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with name of the table to log.
   * @throws DbException
   *           if this object is not ready or any problem occurred logging the
   *           table schema.
   */
  public void logTableSchema(Connection conn, String tableName)
      throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    logTableSchemaBeforeReady(conn, tableName);
  }

  /**
   * Writes the named table schema to the log. To be used during initialization
   * for debugging purposes only.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with name of the table to log.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  protected void logTableSchemaBeforeReady(Connection conn, String tableName)
      throws DbException {
    final String DEBUG_HEADER = "logTableSchemaBeforeReady(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "tableName = " + tableName);

    // Do nothing more if the current log level is not appropriate.
    if (!log.isDebug()) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "log.isDebug() = false.");
      return;
    }

    if (conn == null) {
      throw new DbException("Null connection.");
    }

    // Report a non-existent table.
    if (!tableExistsBeforeReady(conn, tableName)) {
      log.debug("Table '" + tableName + "' does not exist.");
      return;
    }

    String columnName = null;
    String padding = "                               ";
    ResultSet resultSet = null;

    try {
      // Get the table column data.
      if (isTypeDerby()) {
	resultSet = conn.getMetaData().getColumns(null, dataSourceUser,
	    tableName.toUpperCase(), null);
      } else if (isTypePostgresql()) {
	resultSet = conn.getMetaData().getColumns(null, dataSourceUser,
	    tableName.toLowerCase(), null);
      }
      
      log.debug("Table Name : " + tableName);
      log.debug("Column" + padding.substring(0, 32 - "Column".length())
	  + "\tsize\tDataType");

      // Loop through each column.
      while (resultSet.next()) {
	// Output the column data.
	StringBuilder sb = new StringBuilder();

	columnName = resultSet.getString(COLUMN_NAME);
	sb.append(columnName);
	sb.append(padding.substring(0, 32 - columnName.length()));
	sb.append("\t");
	sb.append(resultSet.getString(COLUMN_SIZE));
	sb.append(" \t");
	sb.append(resultSet.getString(TYPE_NAME));

	log.debug(sb.toString());
      }
    } catch (SQLException sqle) {
      throw new DbException("Cannot log table schema");
    } finally {
      DbManager.safeCloseResultSet(resultSet);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Closes a result set without throwing exceptions.
   * 
   * @param resultSet
   *          A ResultSet with the database result set to be closed.
   */
  public static void safeCloseResultSet(ResultSet resultSet) {
    if (resultSet != null) {
      try {
	resultSet.close();
      } catch (SQLException sqle) {
	log.error("Cannot close result set", sqle);
      }
    }
  }

  /**
   * Closes a statement without throwing exceptions.
   * 
   * @param statement
   *          A Statement with the database statement to be closed.
   */
  public static void safeCloseStatement(Statement statement) {
    if (statement != null) {
      try {
	statement.close();
      } catch (SQLException sqle) {
	log.error("Cannot close statement", sqle);
      }
    }
  }

  /**
   * Closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be closed.
   */
  public static void safeCloseConnection(Connection conn) {
    try {
      if ((conn != null) && !conn.isClosed()) {
	conn.close();
      }
    } catch (SQLException sqle) {
      log.error("Cannot close connection", sqle);
    }
  }

  /**
   * Rolls back and closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be rolled back and
   *          closed.
   */
  public static void safeRollbackAndClose(Connection conn) {
    // Roll back the connection.
    try {
      DbManager.rollback(conn, log);
    } catch (DbException sqle) {
      log.error("Cannot roll back the connection", sqle);
    }
    // Close it.
    DbManager.safeCloseConnection(conn);
  }

  /**
   * Provides an indication of whether a table exists.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableName
   *          A String with name of the table to be checked.
   * @return <code>true</code> if the named table exists, <code>false</code>
   *         otherwise.
   * @throws DbException
   *           if this object is not ready or any problem occurred determining
   *           whether the table exists.
   */
  boolean tableExists(Connection conn, String tableName)
      throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized");
    }

    return tableExistsBeforeReady(conn, tableName);
  }

  /**
   * Stops the DbManager service.
   */
  @Override
  public void stopService() {
    // Check whether the Derby database was booted.
    if (isTypeDerby() && dbBooted) {
      try {
	// Yes: Shutdown the database.
	shutdownDerbyDb(dataSourceConfig);

	// Stop the network server control, if it had been started.
	if (networkServerControl != null) {
	  networkServerControl.shutdown();
	}
      } catch (Exception e) {
	log.error("Cannot shutdown the database cleanly", e);
      }
    }

    ready = false;
    dataSource = null;
  }

  /**
   * Shuts down the Derby database.
   * 
   * @throws SQLException
   *           if there are problems shutting down the database.
   */
  private void shutdownDerbyDb(Configuration dsConfig) throws DbException {
    final String DEBUG_HEADER = "shutdownDerbyDb(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Modify the datasource configuration for shutdown.
    Configuration shutdownConfig = dsConfig.copy();
    shutdownConfig.remove("createDatabase");
    shutdownConfig.put("shutdownDatabase", "shutdown");

    // Create the shutdown datasource.
    DataSource ds = createDataSource(shutdownConfig.get("className"));

    // Initialize the shutdown datasource properties.
    initializeDataSourceProperties(shutdownConfig, ds);

    // Get a connection, which will shutdown the Derby database.
    try {
      getConnectionBeforeReady(ds);
    } catch (DbException dbe) {
      // Get the underlying exception.
      SQLException sqle = (SQLException)dbe.getCause();

      // Check whether it is the expected exception.
      if (SHUTDOWN_SUCCESS_STATE_CODE.equals(sqle.getSQLState())) {
	// Yes.
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "Expected exception caught", sqle);
      } else {
	// No: Report the problem.
	log.error("Unexpected exception caught shutting down database", sqle);
      }
    }

    if (log.isDebug())
      log.debug(DEBUG_HEADER + "Derby database has been shutdown.");
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the datasource configuration.
   * 
   * @return a Configuration with the datasource configuration parameters.
   */
  private Configuration getDataSourceConfig() {
    final String DEBUG_HEADER = "getDataSourceConfig(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Get the current configuration.
    Configuration currentConfig = ConfigManager.getCurrentConfig();

    // Create the new datasource configuration.
    Configuration dsConfig = ConfigManager.newConfiguration();

    // Populate it from the current configuration datasource tree.
    dsConfig.copyFrom(currentConfig.getConfigTree(DATASOURCE_ROOT));

    // Save the default class name, if not configured.
    dsConfig.put("className", currentConfig.get(
	PARAM_DATASOURCE_CLASSNAME, DEFAULT_DATASOURCE_CLASSNAME));
    dataSourceClassName = dsConfig.get("className");
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "dataSourceClassName = " + dataSourceClassName);

    // Check whether the Derby database is being used.
    if (isTypeDerby()) {
      // Yes: Save the Derby creation directive, if not configured.
      dsConfig.put("createDatabase", currentConfig.get(
	  PARAM_DATASOURCE_CREATEDATABASE, DEFAULT_DATASOURCE_CREATEDATABASE));
    }

    // Save the port number, if not configured.
    if (isTypeDerby()) {
      dsConfig.put("portNumber", currentConfig.get(
	  PARAM_DATASOURCE_PORTNUMBER, DEFAULT_DATASOURCE_PORTNUMBER));
    } else if (isTypePostgresql()) {
      dsConfig.put("portNumber",
	  currentConfig.get(PARAM_DATASOURCE_PORTNUMBER,
	      		    DEFAULT_DATASOURCE_PORTNUMBER_PG));
    }

    // Save the server name, if not configured.
    dsConfig.put("serverName", currentConfig.get(
	PARAM_DATASOURCE_SERVERNAME, DEFAULT_DATASOURCE_SERVERNAME));

    // Save the user name, if not configured.
    dsConfig.put("user",
	currentConfig.get(PARAM_DATASOURCE_USER, DEFAULT_DATASOURCE_USER));
    dataSourceUser = dsConfig.get("user");
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "dataSourceUser = " + dataSourceUser);

    // Save the configured password.
    dataSourcePassword = currentConfig.get(PARAM_DATASOURCE_PASSWORD,
					   DEFAULT_DATASOURCE_PASSWORD);
    //if (log.isDebug3())
      //log.debug3(DEBUG_HEADER + "dataSourcePassword = " + dataSourcePassword);

    dsConfig.put("password", dataSourcePassword);

    // Check whether the configured datasource database name does not exist.
    if (dsConfig.get("databaseName") == null) {
      // Yes: Check whether the Derby database is being used.
      if (isTypeDerby()) {
	// Yes: Get the data source root directory.
	File datasourceDir = ConfigManager.getConfigManager()
	    .findConfiguredDataDir(PARAM_DATASOURCE_DATABASENAME,
				   DEFAULT_DATASOURCE_DATABASENAME, false);

	// Save the database name.
	dsConfig.put("databaseName",
	    FileUtil.getCanonicalOrAbsolutePath(datasourceDir));

	// No: Check whether the PostgreSQL database is being used.
      } else if (isTypePostgresql()) {
	// Yes: Use the user name as the database name.
	dsConfig.put("databaseName", dataSourceUser);
      }
    }

    dataSourceDbName = dsConfig.get("databaseName");
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "datasourceDatabaseName = '"
	  + dsConfig.get("databaseName") + "'.");

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return dsConfig;
  }

  /**
   * Provides an indication of whether the Derby database is being used.
   * 
   * @return <code>true</code> if the Derby database is being used,
   *         <code>false</code> otherwise.
   */
  private boolean isTypeDerby() {
    final String DEBUG_HEADER = "isTypeDerby(): ";

    boolean result = "org.apache.derby.jdbc.EmbeddedDataSource"
	.equals(dataSourceClassName)
	|| "org.apache.derby.jdbc.ClientDataSource".equals(dataSourceClassName)
	|| "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource"
	.equals(dataSourceClassName)
	|| "org.apache.derby.jdbc.ClientConnectionPoolDataSource"
	.equals(dataSourceClassName);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides an indication of whether the PostgreSQL database is being used.
   * 
   * @return <code>true</code> if the PostgreSQL database is being used,
   *         <code>false</code> otherwise.
   */
  private boolean isTypePostgresql() {
    final String DEBUG_HEADER = "isTypePostgresql(): ";

    boolean result = "org.postgresql.ds.PGSimpleDataSource"
	.equals(dataSourceClassName)
	|| "org.postgresql.ds.PGPoolingDataSource".equals(dataSourceClassName);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Sets the Derby database properties.
   */
  private void setDerbyDatabaseConfiguration() {
    final String DEBUG_HEADER = "setDerbyDatabaseConfiguration(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Get the current configuration.
    Configuration currentConfig = ConfigManager.getCurrentConfig();

    // Save the default Derby log append option, if not configured.
    System.setProperty("derby.infolog.append", currentConfig.get(
	PARAM_DERBY_INFOLOG_APPEND, DEFAULT_DERBY_INFOLOG_APPEND));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "derby.infolog.append = "
	  + System.getProperty("derby.infolog.append"));

    // Save the default Derby log query plan option, if not configured.
    System.setProperty("derby.language.logQueryPlan", currentConfig.get(
	PARAM_DERBY_LANGUAGE_LOGQUERYPLAN,
	DEFAULT_DERBY_LANGUAGE_LOGQUERYPLAN));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "derby.language.logQueryPlan = "
	+ System.getProperty("derby.language.logQueryPlan"));

    // Save the default Derby log statement text option, if not configured.
    System.setProperty("derby.language.logStatementText", currentConfig.get(
	PARAM_DERBY_LANGUAGE_LOGSTATEMENTTEXT,
	DEFAULT_DERBY_LANGUAGE_LOGSTATEMENTTEXT));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "derby.language.logStatementText = "
	+ System.getProperty("derby.language.logStatementText"));

    // Save the default Derby log file path, if not configured.
    System.setProperty("derby.stream.error.file", currentConfig.get(
	PARAM_DERBY_STREAM_ERROR_FILE, DEFAULT_DERBY_STREAM_ERROR_FILE));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "derby.stream.error.file = "
	+ System.getProperty("derby.stream.error.file"));

    // Save the default Derby log severity level, if not configured.
    System.setProperty("derby.stream.error.logSeverityLevel", currentConfig.get(
	PARAM_DERBY_STREAM_ERROR_LOGSEVERITYLEVEL,
	DEFAULT_DERBY_STREAM_ERROR_LOGSEVERITYLEVEL));
    if (log.isDebug3()) log.debug3(DEBUG_HEADER
	+ "derby.stream.error.logSeverityLevel = "
	+ System.getProperty("derby.stream.error.logSeverityLevel"));

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a datasource using the specified class name.
   * 
   * @param dsClassName
   *          A String with the datasource class name.
   * @return a DataSource with the created datasource.
   * @throws DbException
   *           if the datasource could not be created.
   */
  protected DataSource createDataSource(String dsClassName) throws DbException {
    final String DEBUG_HEADER = "createDataSource(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "dsClassName = '" + dsClassName + "'.");

    // Locate the datasource class.
    Class<?> dataSourceClass;

    try {
      dataSourceClass = Class.forName(dsClassName);
    } catch (Throwable t) {
      throw new DbException("Cannot locate datasource class '" + dsClassName
	  + "'", t);
    }

    // Create the datasource.
    try {
      return ((DataSource) dataSourceClass.newInstance());
    } catch (ClassCastException cce) {
      throw new DbException("Class '" + dsClassName + "' is not a DataSource.",
	  cce);
    } catch (Throwable t) {
      throw new DbException("Cannot create instance of datasource class '"
	  + dsClassName + "'", t);
    }
  }

  /**
   * Initializes a PostreSQl database, if it does not exist already.
   * 
   * @param dsConfig
   *          A Configuration with the datasource configuration.
   * @throws DbException
   *           if the database discovery or initialization processes failed.
   */
  private void initializePostgresqlDbIfNeeded(Configuration dsConfig)
      throws DbException {
    final String DEBUG_HEADER = "initializePostgresqlDbIfNeeded(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Create a datasource.
    DataSource ds = createDataSource(dsConfig.get("className"));

    // Initialize the datasource properties.
    initializeDataSourceProperties(dsConfig, ds);

    // Get the configured database name.
    String databaseName = dsConfig.get("databaseName");
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "databaseName = " + databaseName);

    // Replace the database name with the standard connectable template.
    try {
      BeanUtils.setProperty(ds, "databaseName", "template1");
    } catch (Throwable t) {
      throw new DbException("Could not initialize the datasource", t);
    }

    // Connect to the template database.
    Connection conn = getConnectionBeforeReady(ds);

    try {
      // PostgreSQL does not allow a database to be created within a
      // transaction.
      conn.setAutoCommit(true);

      // Create the database if it does not exist.
      createPostgreSqlDbIfMissing(conn, databaseName);
    } catch (SQLException sqle) {
      throw new DbException("Cannot set the connection to auto-commit", sqle);
    } finally {
      DbManager.safeCloseConnection(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Initializes the properties of the datasource using the specified
   * configuration.
   * 
   * @param dsConfig
   *          A Configuration with the datasource configuration.
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @throws DbException
   *           if the datasource properties could not be initialized.
   */
  private void initializeDataSourceProperties(Configuration dsConfig,
      DataSource ds) throws DbException {
    final String DEBUG_HEADER = "initializeDataSourceProperties(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    String dsClassName = dsConfig.get("className");
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "dsClassName = '" + dsClassName + "'.");

    // Loop through all the configured datasource property names.
    for (String key : dsConfig.keySet()) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "key = '" + key + "'.");

      // Check whether it is an applicable datasource property.
      if (isApplicableDataSourceProperty(key)) {
	// Yes: Get the value of the property.
	String value = dsConfig.get(key);
	if (log.isDebug3() && !"password".equals(key))
	  log.debug3(DEBUG_HEADER + "value = '" + value + "'.");

	// Set the property value in the datasource.
	try {
	  BeanUtils.setProperty(ds, key, value);
	} catch (Throwable t) {
	  throw new DbException("Cannot set value '" + value
	      + "' for property '" + key
	      + "' for instance of datasource class '" + dsClassName, t);
	}
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides an indication of whether a property is applicable to a datasource.
   * 
   * @param name
   *          A String with the name of the property.
   * @return <code>true</code> if the named property is applicable to a
   *         datasource, <code>false</code> otherwise.
   */
  private boolean isApplicableDataSourceProperty(String name) {
    final String DEBUG_HEADER = "isApplicableDataSourceProperty(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "name = '" + name + "'.");

    // Handle the names of properties that are always applicable.
    if ("serverName".equals(name) || "portNumber".equals(name)
	|| "dataSourceName".equals(name) || "databaseName".equals(name)
	|| "user".equals(name) || "password".equals(name)
	|| "description".equals(name)) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true.");
      return true;
    }

    // Handle the names of properties applicable to the Derby database being
    // used.
    if (isTypeDerby()
	&& ("createDatabase".equals(name) || "shutdownDatabase".equals(name))) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true.");
      return true;
      // Handle the names of properties applicable to the Derby database being
      // used.
    } else if (isTypePostgresql()
	&& ("initialConnections".equals(name)
	    || "maxConnections".equals(name))) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true.");
      return true;
    }

    // Any other named property is not applicable.
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false.");
    return false;
  }

  /**
   * Starts the Derby NetworkServerControl and waits for it to be ready.
   * 
   * @param cds
   *          A ClientDataSource with the client datasource that provides the
   *          connection.
   * @throws DbException
   *           if the network server control could not be started.
   */
  private void startDerbyNetworkServerControl(ClientDataSource cds)
      throws DbException {
    final String DEBUG_HEADER = "startDerbyNetworkServerControl(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Get the configured server name.
    String serverName = cds.getServerName();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "serverName = '" + serverName + "'.");

    // Determine the IP address of the server.
    InetAddress inetAddr;
    
    try {
      inetAddr = InetAddress.getByName(serverName);
    } catch (UnknownHostException uhe) {
      throw new DbException("Cannot determine the IP address of server '"
	  + serverName + "'", uhe);
    }

    // Get the configured server port number.
    int serverPort = cds.getPortNumber();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "serverPort = " + serverPort + ".");

    // Create the network server control.
    try {
      networkServerControl = new NetworkServerControl(inetAddr, serverPort);
    } catch (Exception e) {
      throw new DbException("Cannot create the Network Server Control", e);
    }

    // Start the network server control.
    try {
      networkServerControl.start(null);
    } catch (Exception e) {
      throw new DbException("Cannot start the Network Server Control", e);
    }

    // Wait for the network server control to be ready.
    for (int i = 0; i < 40; i++) { // At most 20 seconds.
      try {
	networkServerControl.ping();
	log.debug(DEBUG_HEADER + "Remote access to Derby database enabled");
	return;
      } catch (Exception e) {
	// Control is not ready: wait and try again.
	try {
	  Deadline.in(500).sleep(); // About 1/2 second.
	} catch (InterruptedException ie) {
	  break;
	}
      }
    }

    // At this point we give up.
    throw new DbException("Cannot enable remote access to Derby database");
  }

  /**
   * Turns on user authentication and authorization on a Derby database.
   * 
   * @param dsConfig
   *          A Configuration with the datasource configuration.
   * @param cds
   *          A ClientDataSource with the client datasource that provides the
   *          connection.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void setUpDerbyAuthentication(Configuration dsConfig,
      ClientDataSource cds) throws DbException {
    final String DEBUG_HEADER = "setUpDerbyAuthentication(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Start with an unmodified database.
    boolean requiresCommit = false;

    Statement statement = null;

    // Get a connection to the database.
    Connection conn = getConnectionBeforeReady();

    try {
      // Create a statement for authentatication queries.
      statement = conn.createStatement();

      // Get the indication of whether the database requires authentication.
      String requiresAuthentication = getDatabaseProperty(statement,
	  "derby.connection.requireAuthentication", "false");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "requiresAuthentication = "
	  + requiresAuthentication);

      // Check whether it does not require authentication already.
      if ("false".equals(requiresAuthentication.trim().toLowerCase())) {
	// Yes: Change it to require authentication.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.connection.requireAuthentication', 'true')");

	// Get the indication of whether the database requires authentication.
	requiresAuthentication = getDatabaseProperty(statement,
	    "derby.connection.requireAuthentication", "false");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "requiresAuthentication = " + requiresAuthentication);

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database authentication provider.
      String authenticationProvider = getDatabaseProperty(statement,
	  "derby.authentication.provider", "BUILTIN");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "authenticationProvider = "
	  + authenticationProvider);

      // Check whether the database does not use the LOCKSS custom Derby
      // authentication provider already.
      if (!"org.lockss.db.DerbyUserAuthenticator"
	  .equals(authenticationProvider.trim())) {
	// Yes: Change it to use the LOCKSS custom Derby authentication
	// provider.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.authentication.provider', "
	    + "'org.lockss.db.DerbyUserAuthenticator')");

	// Get the database authentication provider.
	authenticationProvider = getDatabaseProperty(statement,
	    "derby.authentication.provider", "BUILTIN");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "authenticationProvider = " + authenticationProvider);

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database default connection mode.
      String defaultConnectionMode = getDatabaseProperty(statement,
	  "derby.database.defaultConnectionMode", "fullAccess");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "defaultConnectionMode = "
	  + defaultConnectionMode);

      // Check whether the database does not prevent unauthenticated access
      // already.
      if (!"noAccess".equals(defaultConnectionMode.trim())) {
	// Yes: Change it to prevent unauthenticated access.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.defaultConnectionMode', 'noAccess')");

	// Get the database default connection mode.
	defaultConnectionMode = getDatabaseProperty(statement,
	    "derby.database.defaultConnectionMode", "fullAccess");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "defaultConnectionMode = "
	    + defaultConnectionMode);

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the names of the database full access users.
      String fullAccessUsers =
	  getDatabaseProperty(statement, "derby.database.fullAccessUsers", "");
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "fullAccessUsers = " + fullAccessUsers);

      // Get the configured user name.
      String user = dsConfig.get("user");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "user = " + user);

      // Check whether the user is not in the list of full access users already.
      if (fullAccessUsers.indexOf(user) < 0) {
	// Yes: Change it to allow the configured user full access.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.fullAccessUsers', '" + user + "')");

	// Get the names of the database full access users.
	fullAccessUsers = getDatabaseProperty(statement,
	    "derby.database.fullAccessUsers", "");
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "fullAccessUsers = " + fullAccessUsers);

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database read-only access users.
      String readOnlyAccessUsers = getDatabaseProperty(statement,
	  "derby.database.readOnlyAccessUsers", "");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "readOnlyAccessUsers = "
	  + readOnlyAccessUsers);

      // Check whether changes to the database properties have been made.
      if (requiresCommit) {
	// Yes: Allow override using system properties.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.propertiesOnly', 'false')");
      }
    } catch (SQLException sqle) {
      // Prevent partial changes to be committed.
      requiresCommit = false;

      throw new DbException("Cannot set database property", sqle);
    } finally {
      DbManager.safeCloseStatement(statement);

      // Check whether we need to commit the changes made to the database.
      if (requiresCommit) {
	DbManager.commitOrRollback(conn, log);
      }

      DbManager.safeRollbackAndClose(conn);
    }

    // Check whether the database needs to be shut down to make static
    // properties take effect.
    if (requiresCommit) {
      // Yes: Shut down the database.
      shutdownDerbyDb(dsConfig);

      // Initialize the datasource properties.
      initializeDataSourceProperties(dsConfig, cds);

      // Restart the network server control.
      startDerbyNetworkServerControl(cds);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Turns off user authentication and authorization on a Derby database.
   * 
   * @param dsConfig
   *          A Configuration with the datasource configuration.
   * @param ds
   *          A DataSource with the client datasource that provides the
   *          connection.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void removeDerbyAuthentication(Configuration dsConfig, DataSource ds)
      throws DbException {
    final String DEBUG_HEADER = "removeDerbyAuthentication(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Start with an unmodified database.
    boolean requiresCommit = false;

    Statement statement = null;

    // Get a connection to the database.
    Connection conn = getConnectionBeforeReady();

    try {
      // Create a statement for authentication queries.
      statement = conn.createStatement();

      // Get the indication of whether the database requires authentication.
      String requiresAuthentication = getDatabaseProperty(statement,
	  "derby.connection.requireAuthentication", "false");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "requiresAuthentication = "
	  + requiresAuthentication);

      // Check whether it does require authentication.
      if ("true".equals(requiresAuthentication.trim().toLowerCase())) {
	// Yes: Change it to not require authentication.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.connection.requireAuthentication', 'false')");

	// Get the indication of whether the database requires authentication.
	requiresAuthentication = getDatabaseProperty(statement,
	    "derby.connection.requireAuthentication", "false");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "requiresAuthentication = " + requiresAuthentication);

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database authentication provider.
      String authenticationProvider = getDatabaseProperty(statement,
	  "derby.authentication.provider", "BUILTIN");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "authenticationProvider = "
	  + authenticationProvider);

      // Check whether it does not use the built-in Derby authentication
      // provider already.
      if (!"BUILTIN".equals(authenticationProvider.trim().toUpperCase())) {
	// Yes: Change it to use the built-in Derby authentication provider.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.authentication.provider', 'BUILTIN')");

	// Get the database authentication provider.
	authenticationProvider = getDatabaseProperty(statement,
	    "derby.authentication.provider", "BUILTIN");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "authenticationProvider = " + authenticationProvider);

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database default connection mode.
      String defaultConnectionMode = getDatabaseProperty(statement,
	  "derby.database.defaultConnectionMode", "fullAccess");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "defaultConnectionMode = "
	  + defaultConnectionMode);

      // Check whether it does not allow full unauthenticated access already.
      if (!"fullAccess".equals(defaultConnectionMode.trim())) {
	// Yes: Change it to allow full unauthenticated access.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.defaultConnectionMode', 'fullAccess')");

	// Get the default connection mode.
	defaultConnectionMode = getDatabaseProperty(statement,
	    "derby.database.defaultConnectionMode", "fullAccess");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "defaultConnectionMode = "
	    + defaultConnectionMode);

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Check whether changes to the database properties have been made.
      if (requiresCommit) {
	// Yes: Allow override using system properties.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.propertiesOnly', 'false')");
      }
    } catch (SQLException sqle) {
      // Prevent partial changes to be committed.
      requiresCommit = false;

      throw new DbException("Cannot set database property", sqle);
    } finally {
      DbManager.safeCloseStatement(statement);

      // Check whether we need to commit the changes made to the database.
      if (requiresCommit) {
	DbManager.commitOrRollback(conn, log);
      }

      DbManager.safeRollbackAndClose(conn);
    }

    // Check whether the database needs to be shut down to make static
    // properties take effect.
    if (requiresCommit) {
      // Yes: Shut down the database.
      shutdownDerbyDb(dsConfig);

      // Initialize the datasource properties.
      initializeDataSourceProperties(dsConfig, ds);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides a database property.
   * 
   * @param statement
   *          A Statement to query the database.
   * @param propertyName
   *          A String with the name of the requested property.
   * @param defaultValue
   *          A String with the default value of the requested property.
   * @return a String with the value of the database property.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private String getDatabaseProperty(Statement statement, String propertyName,
      String defaultValue) throws DbException {
    final String DEBUG_HEADER = "getDatabaseProperty(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "propertyName = " + propertyName);
      log.debug2(DEBUG_HEADER + "defaultValue = " + defaultValue);
    }

    ResultSet rs = null;
    String propertyValue = null;

    try {
      // Get the property.
      rs = statement
	  .executeQuery("VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY('"
	      + propertyName + "')");
      rs.next();
      propertyValue = rs.getString(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "propertyValue = " + propertyValue);
    } catch (SQLException sqle) {
      throw new DbException("Cannot get database property with name '"
	  + propertyName + "'.", sqle);
    } finally {
      DbManager.safeCloseResultSet(rs);
    }

    // Return the default, if necessary.
    if (propertyValue == null) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Using defaultValue = " + defaultValue);
      propertyValue = defaultValue;
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "propertyValue = " + propertyValue);
    return propertyValue;
  }

  /**
   * Provides the query used to drop a function.
   * 
   * @param functionName
   *          A string with the name of the function to be dropped.
   * @return a String with the query used to drop the function.
   */
  private static String dropFunctionQuery(String functionName) {
    return "drop function " + functionName;
  }

  /**
   * Provides the query used to drop a table.
   * 
   * @param tableName
   *          A string with the name of the table to be dropped.
   * @return a String with the query used to drop the table.
   */
  protected static String dropTableQuery(String tableName) {
    return "drop table " + tableName;
  }

  /**
   * Provides the version of the database that is the upgrade target of this
   * daemon.
   * 
   * @return an int with the target version of the database.
   * @throws DbException
   *           if this object is not ready.
   */
  public int getTargetDatabaseVersion() throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    return targetDatabaseVersion;
  }

  /**
   * Sets the version of the database that is the upgrade target of this daemon.
   * 
   * @param version
   *          An int with the target version of the database.
   */
  void setTargetDatabaseVersion(int version) {
    targetDatabaseVersion = version;
  }

  /**
   * Sets up the database for a given version.
   * 
   * @param finalVersion
   *          An int with the version of the database to be set up.
   * @return <code>true</code> if the database was successfully set up,
   *         <code>false</code> otherwise.
   */
  boolean setUpDatabase(int finalVersion) {
    final String DEBUG_HEADER = "setUpDatabase(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "finalVersion = " + finalVersion);

    // Do nothing to set up a non-existent database.
    if (finalVersion < 1) {
      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "success = true");
      return true;
    }

    boolean success = false;
    Connection conn = null;

    try {
      // Set up the database infrastructure.
      setUpInfrastructure();

      // Get a connection to the database.
      conn = getConnectionBeforeReady();

      // Set up the database to version 1.
      setUpDatabaseVersion1(conn);

      // Update the database to the final version.
      updateDatabase(conn, 1, finalVersion);

      // Check whether the updated database is at least at version 2.
      if (finalVersion > 1) {
	// Yes: Record the current database version in the database.
	recordDbVersion(conn, finalVersion);
      }

      // Commit this partial update.
      DbManager.commitOrRollback(conn, log);

      success = true;
    } catch (DbException dbe) {
      log.error(dbe.getMessage() + " - DbManager not ready", dbe);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "success = " + success);
    return success;
  }

  /**
   * Sets up the database to version 1.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred setting up the database.
   */
  private void setUpDatabaseVersion1(Connection conn) throws DbException {
    final String DEBUG_HEADER = "setUpDatabaseVersion1(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Create the necessary tables if they do not exist.
    createTablesIfMissing(conn, VERSION_1_TABLE_CREATE_QUERIES);

    // Create new functions.
    if (isTypeDerby()) {
      createVersion1FunctionsIfMissing(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates all the necessary version 1 database functions if they do not
   * exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred creating the functions.
   */
  private void createVersion1FunctionsIfMissing(Connection conn)
      throws DbException {

    // Create the functions.
    executeBatchBeforeReady(conn, VERSION_1_FUNCTION_CREATE_QUERIES);
  }

  /**
   * Provides a version of a text truncated to a maximum length, if necessary,
   * including an indication of the truncation.
   * 
   * @param original
   *          A String with the original text to be truncated, if necessary.
   * @param maxLength
   *          An int with the maximum length of the truncated text to be
   *          provided.
   * @return a String with the original text if it is not longer than the
   *         maximum length allowed or the truncated text including an
   *         indication of the truncation.
   */
  public static String truncateVarchar(String original, int maxLength) {
    if (original.length() <= maxLength) {
      return original;
    }

    return original.substring(0, maxLength - TRUNCATION_INDICATOR.length())
	+ TRUNCATION_INDICATOR;
  }

  /**
   * Provides an indication of whether a text has been truncated.
   * 
   * @param text
   *          A String with the text to be evaluated for truncation.
   * @return <code>true</code> if the text has been truncated,
   *         <code>false</code> otherwise.
   */
  public static boolean isTruncatedVarchar(String text) {
    return text.endsWith(TRUNCATION_INDICATOR);
  }

  /**
   * Executes a querying prepared statement, retrying the execution in the
   * default manner in case of transient failures.
   * 
   * @param statement
   *          A PreparedStatement with the querying prepared statement to be
   *          executed.
   * @return a ResultSet with the results of the query.
   * @throws DbException
   *           if any problem occurred executing the query.
   */
  public ResultSet executeQuery(PreparedStatement statement)
      throws DbException {
    return executeQuery(statement, maxRetryCount, retryDelay);
  }

  /**
   * Executes a querying prepared statement, retrying the execution with the
   * specified parameters in case of transient failures.
   * 
   * @param statement
   *          A PreparedStatement with the querying prepared statement to be
   *          executed.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a ResultSet with the results of the query.
   * @throws DbException
   *           if this object is not ready or any problem occurred executing the
   *           query.
   */
  public ResultSet executeQuery(PreparedStatement statement, int maxRetryCount,
      long retryDelay) throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    return executeQueryBeforeReady(statement, maxRetryCount, retryDelay);
  }

  /**
   * Executes a querying prepared statement, retrying the execution in the
   * default manner in case of transient failures. To be used during
   * initialization.
   * 
   * @param statement
   *          A PreparedStatement with the querying prepared statement to be
   *          executed.
   * @return a ResultSet with the results of the query.
   * @throws DbException
   *           if any problem occurred executing the query.
   */
  protected ResultSet executeQueryBeforeReady(PreparedStatement statement)
      throws DbException {
    return executeQueryBeforeReady(statement, maxRetryCount, retryDelay);
  }

  /**
   * Executes a querying prepared statement, retrying the execution with the
   * specified parameters in case of transient failures. To be used during
   * initialization.
   * 
   * @param statement
   *          A PreparedStatement with the querying prepared statement to be
   *          executed.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a ResultSet with the results of the query.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private ResultSet executeQueryBeforeReady(PreparedStatement statement,
      int maxRetryCount, long retryDelay) throws DbException {
    final String DEBUG_HEADER = "executeQueryBeforeReady(): ";
    if (log.isDebug2()) {
      log.debug2("maxRetryCount = " + maxRetryCount);
      log.debug2("retryDelay = " + retryDelay);
    }

    boolean success = false;
    int retryCount = 0;
    ResultSet results = null;

    // Keep trying until success.
    while (!success) {
      try {
	// Execute the query.
	results = statement.executeQuery();
	success = true;
      } catch (SQLTransientException sqlte) {
	// A SQLTransientException is caught: Count the next retry.
	retryCount++;

	// Check whether the next retry would go beyond the specified maximum
	// number of retries.
	if (retryCount > maxRetryCount) {
	  // Yes: Report the failure.
	  log.error("Transient exception caught", sqlte);
	  log.error("Maximum retry count of " + maxRetryCount + " reached.");
	  throw new DbException("Cannot execute a query", sqlte);
	} else {
	  // No: Wait for the specified amount of time before attempting the
	  // next retry.
	  log.debug(DEBUG_HEADER + "Exception caught", sqlte);
	  log.debug(DEBUG_HEADER + "Waiting "
		    + StringUtil.timeIntervalToString(retryDelay)
		    + " before retry number " + retryCount + "...");

	  try {
	    Deadline.in(retryDelay).sleep();
	  } catch (InterruptedException ie) {
	    // Continue with the next retry.
	  }
	}
      } catch (SQLException sqle) {
	// A non-transient exception is caught: Report the problem.
	throw new DbException("Cannot execute a query", sqle);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return results;
  }

  /**
   * Executes an updating prepared statement, retrying the execution in the
   * default manner in case of transient failures.
   * 
   * @param statement
   *          A PreparedStatement with the updating prepared statement to be
   *          executed.
   * @return an int with the number of database rows updated.
   * @throws DbException
   *           if any problem occurred executing the query.
   */
  public int executeUpdate(PreparedStatement statement) throws DbException {
    return executeUpdate(statement, maxRetryCount, retryDelay);
  }

  /**
   * Executes an updating prepared statement, retrying the execution with the
   * specified parameters in case of transient failures.
   * 
   * @param statement
   *          A PreparedStatement with the updating prepared statement to be
   *          executed.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between
   *          consecutive retries.
   * @return an int with the number of database rows updated.
   * @throws DbException
   *           if this object is not ready or any problem occurred executing the
   *           query.
   */
  public int executeUpdate(PreparedStatement statement, int maxRetryCount,
      long retryDelay) throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    return executeUpdateBeforeReady(statement, maxRetryCount, retryDelay);
  }

  /**
   * Executes an updating prepared statement, retrying the execution in the
   * default manner in case of transient failures. To be used during
   * initialization.
   * 
   * @param statement
   *          A PreparedStatement with the updating prepared statement to be
   *          executed.
   * @return an int with the number of database rows updated.
   * @throws DbException
   *           if any problem occurred executing the query.
   */
  protected int executeUpdateBeforeReady(PreparedStatement statement)
      throws DbException {
    return executeUpdateBeforeReady(statement, maxRetryCount, retryDelay);
  }

  /**
   * Executes an updating prepared statement, retrying the execution  with the
   * specified parameters in case of transient failures. To be used during
   * initialization.
   * 
   * @param statement
   *          A PreparedStatement with the updating prepared statement to be
   *          executed.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return an int with the number of database rows updated.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private int executeUpdateBeforeReady(PreparedStatement statement,
      int maxRetryCount, long retryDelay) throws DbException {
    final String DEBUG_HEADER = "executeUpdateBeforeReady(): ";
    if (log.isDebug2()) {
      log.debug2("maxRetryCount = " + maxRetryCount);
      log.debug2("retryDelay = " + retryDelay);
    }

    boolean success = false;
    int retryCount = 0;
    int updatedCount = 0;

    // Keep trying until success.
    while (!success) {
      try {
	// Execute the query.
	updatedCount = statement.executeUpdate();
	success = true;
      } catch (SQLTransientException sqlte) {
	// A SQLTransientException is caught: Count the next retry.
	retryCount++;

	// Check whether the next retry would go beyond the specified maximum
	// number of retries.
	if (retryCount > maxRetryCount) {
	  // Yes: Report the failure.
	  log.error("Transient exception caught", sqlte);
	  log.error("Maximum retry count of " + maxRetryCount + " reached.");
	  throw new DbException("Cannot execute a query", sqlte);
	} else {
	  // No: Wait for the specified amount of time before attempting the
	  // next retry.
	  log.debug(DEBUG_HEADER + "Exception caught", sqlte);
	  log.debug(DEBUG_HEADER + "Waiting "
		    + StringUtil.timeIntervalToString(retryDelay)
		    + " before retry number " + retryCount + "...");

	  try {
	    Deadline.in(retryDelay).sleep();
	  } catch (InterruptedException ie) {
	    // Continue with the next retry.
	  }
	}
      } catch (SQLException sqle) {
	// A non-transient exception is caught: Report the problem.
	throw new DbException("Cannot execute a query", sqle);
      }
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "updatedCount = " + updatedCount);
    return updatedCount;
  }

  /**
   * Prepares a statement, retrying the preparation in the default manner in
   * case of transient failures.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @return a PreparedStatement with the prepared statement.
   * @throws DbException
   *           if any problem occurred preparing the statement.
   */
  public PreparedStatement prepareStatement(Connection conn, String sql)
      throws DbException {
    return prepareStatement(conn, sql, maxRetryCount, retryDelay);
  }

  /**
   * Prepares a statement, retrying the preparation with the specified
   * parameters in case of transient failures.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a PreparedStatement with the prepared statement.
   * @throws DbException
   *           if this object is not ready or any problem occurred preparing the
   *           statement.
   */
  public PreparedStatement prepareStatement(Connection conn, String sql,
      int maxRetryCount, long retryDelay) throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    return prepareStatementBeforeReady(conn, sql, maxRetryCount, retryDelay);
  }

  /**
   * Prepares a statement, retrying the preparation in the default manner in
   * case of transient failures. To be used during initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @return a PreparedStatement with the prepared statement.
   * @throws DbException
   *           if any problem occurred preparing the statement.
   */
  protected PreparedStatement prepareStatementBeforeReady(Connection conn,
      String sql) throws DbException {
    return prepareStatementBeforeReady(conn, sql, maxRetryCount, retryDelay);
  }

  /**
   * Prepares a statement, retrying the preparation with the specified
   * parameters in case of transient failures. To be used during initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a PreparedStatement with the prepared statement.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private PreparedStatement prepareStatementBeforeReady(Connection conn,
      String sql, int maxRetryCount, long retryDelay) throws DbException {
    final String DEBUG_HEADER = "prepareStatementBeforeReady(): ";
    if (log.isDebug2()) {
      log.debug2("maxRetryCount = " + maxRetryCount);
      log.debug2("retryDelay = " + retryDelay);
    }

    boolean success = false;
    int retryCount = 0;
    PreparedStatement statement = null;

    // Keep trying until success.
    while (!success) {
      try {
	// Prepare the statement.
	statement = conn.prepareStatement(sql);
	success = true;
      } catch (SQLTransientException sqlte) {
	// A SQLTransientException is caught: Count the next retry.
	retryCount++;

	// Check whether the next retry would go beyond the specified maximum
	// number of retries.
	if (retryCount > maxRetryCount) {
	  // Yes: Report the failure.
	  log.error("Transient exception caught", sqlte);
	  log.error("Maximum retry count of " + maxRetryCount + " reached.");
	  throw new DbException("Cannot prepare a statement", sqlte);
	} else {
	  // No: Wait for the specified amount of time before attempting the
	  // next retry.
	  log.debug(DEBUG_HEADER + "Exception caught", sqlte);
	  log.debug(DEBUG_HEADER + "Waiting "
	      	    + StringUtil.timeIntervalToString(retryDelay)
	      	    + " before retry number " + retryCount + "...");

	  try {
	    Deadline.in(retryDelay).sleep();
	  } catch (InterruptedException ie) {
	    // Continue with the next retry.
	  }
	}
      } catch (SQLException sqle) {
	// A non-transient exception is caught: Report the problem.
	throw new DbException("Cannot prepare a statement", sqle);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return statement;
  }

  /**
   * Prepares a statement, retrying the preparation in the default manner in
   * case of transient failures and specifying whether to return generated keys.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @param returnGeneratedKeys
   *          An int indicating whether generated keys should be made available
   *          for retrieval.
   * @return a PreparedStatement with the prepared statement.
   * @throws DbException
   *           if any problem occurred preparing the statement.
   */
  public PreparedStatement prepareStatement(Connection conn, String sql,
      int returnGeneratedKeys) throws DbException {
    return prepareStatement(conn, sql, returnGeneratedKeys, maxRetryCount,
	retryDelay);
  }

  /**
   * Prepares a statement, retrying the preparation with the specified
   * parameters in case of transient failures and specifying whether to return
   * generated keys.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @param returnGeneratedKeys
   *          An int indicating whether generated keys should be made available
   *          for retrieval.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a PreparedStatement with the prepared statement.
   * @throws DbException
   *           if this object is not ready or any problem occurred preparing the
   *           statement.
   */
  public PreparedStatement prepareStatement(Connection conn, String sql,
      int returnGeneratedKeys, int maxRetryCount, long retryDelay)
      throws DbException {
    if (!ready) {
      throw new DbException("DbManager has not been initialized.");
    }

    return prepareStatementBeforeReady(conn, sql, returnGeneratedKeys,
	maxRetryCount, retryDelay);
  }

  /**
   * Prepares a statement, retrying the preparation in the default manner in
   * case of transient failures and specifying whether to return generated keys.
   * To be used during initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @param returnGeneratedKeys
   *          An int indicating whether generated keys should be made available
   *          for retrieval.
   * @return a PreparedStatement with the prepared statement.
   * @throws DbException
   *           if any problem occurred preparing the statement.
   */
  protected PreparedStatement prepareStatementBeforeReady(Connection conn,
      String sql, int returnGeneratedKeys) throws DbException {
    return prepareStatementBeforeReady(conn, sql, returnGeneratedKeys,
	maxRetryCount, retryDelay);
  }

  /**
   * Prepares a statement, retrying the preparation with the specified
   * parameters in case of transient failures and specifying whether to return
   * generated keys. To be used during initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param sql
   *          A String with the prepared statement SQL query.
   * @param returnGeneratedKeys
   *          An int indicating whether generated keys should be made available
   *          for retrieval.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @return a PreparedStatement with the prepared statement.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private PreparedStatement prepareStatementBeforeReady(Connection conn,
      String sql, int returnGeneratedKeys, int maxRetryCount, long retryDelay)
      throws DbException {
    final String DEBUG_HEADER = "prepareStatementBeforeReady(): ";
    if (log.isDebug2()) {
      log.debug2("returnGeneratedKeys = " + returnGeneratedKeys);
      log.debug2("maxRetryCount = " + maxRetryCount);
      log.debug2("retryDelay = " + retryDelay);
    }

    boolean success = false;
    int retryCount = 0;
    PreparedStatement statement = null;

    // Keep trying until success.
    while (!success) {
      try {
	// Prepare the statement.
	statement = conn.prepareStatement(sql, returnGeneratedKeys);
	success = true;
      } catch (SQLTransientException sqlte) {
	// A SQLTransientException is caught: Count the next retry.
	retryCount++;

	// Check whether the next retry would go beyond the specified maximum
	// number of retries.
	if (retryCount > maxRetryCount) {
	  // Yes: Report the failure.
	  log.error("Transient exception caught", sqlte);
	  log.error("Maximum retry count of " + maxRetryCount + " reached.");
	  throw new DbException("Cannot prepare a statement", sqlte);
	} else {
	  // No: Wait for the specified amount of time before attempting the
	  // next retry.
	  log.debug(DEBUG_HEADER + "Exception caught", sqlte);
	  log.debug(DEBUG_HEADER + "Waiting "
	      	    + StringUtil.timeIntervalToString(retryDelay)
	      	    + " before retry number " + retryCount + "...");

	  try {
	    Deadline.in(retryDelay).sleep();
	  } catch (InterruptedException ie) {
	    // Continue with the next retry.
	  }
	}
      } catch (SQLException sqle) {
	// A non-transient exception is caught: Report the problem.
	throw new DbException("Cannot prepare a statement", sqle);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return statement;
  }

  /**
   * Updates the database from version 2 to version 3.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred updating the database.
   */
  private void updateDatabaseFrom2To3(Connection conn) throws DbException {
    final String DEBUG_HEADER = "updateDatabaseFrom2To3(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Create the necessary indices.
    createIndices(conn, VERSION_3_INDEX_CREATE_QUERIES);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 3 to version 4.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred updating the database.
   */
  private void updateDatabaseFrom3To4(Connection conn) throws DbException {
    final String DEBUG_HEADER = "updateDatabaseFrom3To4(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Create the necessary tables if they do not exist.
    createTablesIfMissing(conn, VERSION_4_TABLE_CREATE_QUERIES);

    // Create the necessary indices.
    createIndices(conn, VERSION_4_INDEX_CREATE_QUERIES);

    // Migrate the version 3 platforms.
    addPluginTablePlatformReferenceColumn(conn);
    populatePlatformTable(conn);
    removeObsoletePluginTablePlatformColumn(conn);

    // Fix publication dates.
    setPublicationDatesToNull(conn);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds the platform reference column to the plugin table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void addPluginTablePlatformReferenceColumn(Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "addPluginTablePlatformReferenceColumn(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    PreparedStatement statement = null;

    try {
      statement =
	  prepareStatementBeforeReady(conn, ADD_PLUGIN_PLATFORM_SEQ_COLUMN);

      int count = executeUpdateBeforeReady(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } finally {
      DbManager.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Populates the platform table with the platforms existing in the plugin
   * table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void populatePlatformTable(Connection conn) throws DbException {
    final String DEBUG_HEADER = "populatePlatformTable(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Update the null platforms in the plugin table.
    updatePluginNullPlatform(conn);

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String platform = null;
    Long platformSeq = null;

    try {
      // Get all the distinct platforms in the plugin table.
      statement = prepareStatementBeforeReady(conn, GET_VERSION_2_PLATFORMS);
      resultSet = executeQueryBeforeReady(statement);

      // Loop through all the distinct platforms in the plugin table.
      while (resultSet.next()) {
	// Get the platform.
  	platform = resultSet.getString(PLATFORM_COLUMN);
  	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "platform = " + platform);

        // Add the publishing platform to its own table.
  	platformSeq = addPlatform(conn, platform);
  	if (log.isDebug3())
  	  log.debug3(DEBUG_HEADER + "platformSeq = " + platformSeq);

  	// Update all the plugins using this platform.
  	updatePluginPlatformReference(conn, platformSeq, platform);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the platforms", sqle);
      log.error("SQL = '" + GET_VERSION_2_PLATFORMS + "'.");
      throw new DbException("Cannot get the platforms", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the null platform name in the plugin table during initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void updatePluginNullPlatform(Connection conn) throws DbException {
    final String DEBUG_HEADER = "updatePluginNullPlatform(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    PreparedStatement statement = null;

    try {
      statement =
	  prepareStatementBeforeReady(conn, UPDATE_PLUGIN_NULL_PLATFORM_QUERY);
      statement.setString(1, NO_PLATFORM);

      int count = executeUpdateBeforeReady(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      log.error("Cannot update the null platform name", sqle);
      log.error("SQL = '" + UPDATE_PLUGIN_NULL_PLATFORM_QUERY + "'.");
      throw new DbException("Cannot update the null platform name", sqle);
    } finally {
      DbManager.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds a platform to the database during initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformName
   *          A String with the platform name.
   * @return a Long with the identifier of the platform just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long addPlatform(Connection conn, String platformName)
      throws DbException {
    final String DEBUG_HEADER = "addPlatform(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "platformName = " + platformName);
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Long platformSeq = null;

    try {
      statement = prepareStatementBeforeReady(conn, INSERT_PLATFORM_QUERY,
	  Statement.RETURN_GENERATED_KEYS);
      // Skip auto-increment key field #0.
      statement.setString(1, platformName);
      executeUpdateBeforeReady(statement);
      resultSet = statement.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Cannot create PLATFORM row for platformName '" + platformName
	    + "'");
	throw new DbException("Cannot create PLATFORM row for platformName '"
	    + platformName + "'");
      }

      platformSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added platformSeq = " + platformSeq);
    } catch (SQLException sqle) {
      throw new DbException("Cannot add platform", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
    return platformSeq;
  }

  /**
   * Updates the platform reference in the plugin table during initialization.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformSeq
   *          A Long with the identifier of the platform.
   * @param platformName
   *          A String with the platform name.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void updatePluginPlatformReference(Connection conn, Long platformSeq,
      String platformName) throws DbException {
    final String DEBUG_HEADER = "updatePluginPlatformReference(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
      log.debug2(DEBUG_HEADER + "platformName = " + platformName);
    }

    PreparedStatement statement = null;

    try {
      statement =
	  prepareStatementBeforeReady(conn, UPDATE_PLUGIN_PLATFORM_SEQ_QUERY);
      statement.setLong(1, platformSeq);
      statement.setString(2, platformName);

      int count = executeUpdateBeforeReady(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      log.error("Cannot update plugin platform", sqle);
      log.error("platformSeq = " + platformSeq);
      log.error("platformName = '" + platformName + "'.");
      log.error("SQL = '" + UPDATE_PLUGIN_PLATFORM_SEQ_QUERY + "'.");
      throw new DbException("Cannot update plugin platform", sqle);
    } finally {
      DbManager.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes the obsolete platform column from the plugin table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void removeObsoletePluginTablePlatformColumn(Connection conn)
      throws DbException {
    final String DEBUG_HEADER = "removObsoletePluginTablePlatformColumn(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    PreparedStatement statement = null;

    try {
      statement = prepareStatementBeforeReady(conn,
	  REMOVE_OBSOLETE_PLUGIN_PLATFORM_COLUMN);

      int count = executeUpdateBeforeReady(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } finally {
      DbManager.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Nulls out the date column for publications, populated in earlier versions
   * by mistake.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void setPublicationDatesToNull(Connection conn) throws DbException {
    final String DEBUG_HEADER = "setPublicationDatesToNull(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    PreparedStatement statement = null;

    try {
      statement =
	  prepareStatementBeforeReady(conn, SET_PUBLICATION_DATES_TO_NULL);

      int count = executeUpdateBeforeReady(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } finally {
      DbManager.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 4 to version 5.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred updating the database.
   */
  private void updateDatabaseFrom4To5(Connection conn) throws DbException {
    final String DEBUG_HEADER = "updateDatabaseFrom4To5(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Create the necessary tables if they do not exist.
    createTablesIfMissing(conn, VERSION_5_TABLE_CREATE_QUERIES);

    // Create the necessary indices.
    createIndices(conn, VERSION_5_INDEX_CREATE_QUERIES);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 5 to version 6.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred updating the database.
   */
  private void updateDatabaseFrom5To6(Connection conn) throws DbException {
    final String DEBUG_HEADER = "updateDatabaseFrom5To6(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Remove duplicated rows in the ISBN table.
    removeDuplicateIsbns(conn);

    // Remove duplicated rows in the ISSN table.
    removeDuplicateIssns(conn);

    // Create the necessary indices.
    createIndices(conn, VERSION_6_INDEX_CREATE_QUERIES);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes duplicated rows in the ISBN table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
  *           if any problem occurred accessing the database.
   */
  private void removeDuplicateIsbns(Connection conn) throws DbException {
    final String DEBUG_HEADER = "removeDuplicateIsbns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement findStatement = null;
    PreparedStatement deleteStatement = null;
    PreparedStatement insertStatement = null;
    Long previousMdItemSeq = null;
    String previousIsbn = null;
    String previousIsbnType = null;
    Long mdItemSeq = null;
    String isbn = null;
    String isbnType = null;
    ResultSet resultSet = null;
    int count;
    boolean done = false;
    boolean foundDuplicate = false;

    // Repeat until there are no duplicated ISBNs.
    while (!done) {
      try {
	// Get all the ISBN rows.
	findStatement = prepareStatementBeforeReady(conn, FIND_ISBNS);
	resultSet = executeQueryBeforeReady(findStatement);

	// Loop through all the ISBN rows.
	while (resultSet.next()) {
	  // Get the data of the row.
	  mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	  isbn = resultSet.getString(ISBN_COLUMN);
	  isbnType = resultSet.getString(ISBN_TYPE_COLUMN);

	  // Check whether this row is a duplicate of the previous one.
	  if (mdItemSeq.equals(previousMdItemSeq)
	      && isbn.equals(previousIsbn)
	      && isbnType.equals(previousIsbnType)) {
	    // Yes: Handle it.
	    if (log.isDebug3()) {
	      log.debug3(DEBUG_HEADER + "Duplicated mdItemSeq = " + mdItemSeq);
	      log.debug3(DEBUG_HEADER + "Duplicated isbn = " + isbn);
	      log.debug3(DEBUG_HEADER + "Duplicated isbnType = " + isbnType);
	    }

	    foundDuplicate = true;
	    break;
	  } else {
	    // No: Rememeber it.
	    previousMdItemSeq = mdItemSeq;
	    previousIsbn = isbn;
	    previousIsbnType = isbnType;
	  }
	}
      } catch (SQLException sqle) {
	log.error("Cannot find ISBNs", sqle);
	log.error("SQL = '" + FIND_ISBNS + "'.");
	throw new DbException("Cannot find ISBNs", sqle);
      } finally {
	DbManager.safeCloseResultSet(resultSet);
	DbManager.safeCloseStatement(findStatement);
      }

      // Check whether no duplicate ISBNs were found.
      if (!foundDuplicate) {
	// Yes: Done.
	done = true;
	continue;
      }

      // No: Delete all the duplicate rows found.
      try {
	deleteStatement = prepareStatementBeforeReady(conn, REMOVE_ISBN);

	deleteStatement.setLong(1, mdItemSeq);
	deleteStatement.setString(2, isbn);
	deleteStatement.setString(3, isbnType);

	count = executeUpdateBeforeReady(deleteStatement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
      } catch (SQLException sqle) {
	log.error("Cannot delete ISBN", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("isbn = " + isbn);
	log.error("isbnType = " + isbnType);
	log.error("SQL = '" + REMOVE_ISBN + "'.");
	throw new DbException("Cannot delete ISBN '" + isbn + "'", sqle);
      } finally {
	DbManager.safeCloseStatement(deleteStatement);
      }

      // Insert back one instance of the deleted rows.
      try {
	insertStatement = prepareStatementBeforeReady(conn, ADD_ISBN);

	insertStatement.setLong(1, mdItemSeq);
	insertStatement.setString(2, isbn);
	insertStatement.setString(3, isbnType);

	count = executeUpdateBeforeReady(insertStatement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	DbManager.commitOrRollback(conn, log);
      } catch (SQLException sqle) {
	log.error("Cannot add ISBN", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("isbn = " + isbn);
	log.error("isbnType = " + isbnType);
	log.error("SQL = '" + ADD_ISBN + "'.");
	throw new DbException("Cannot add ISBN '" + isbn + "'", sqle);
      } finally {
	DbManager.safeCloseStatement(insertStatement);
      }

      // Prepare to repeat the process.
      previousMdItemSeq = null;
      previousIsbn = null;
      previousIsbnType = null;
      foundDuplicate = false;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes duplicated rows in the ISSN table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private void removeDuplicateIssns(Connection conn) throws DbException {
    final String DEBUG_HEADER = "removeDuplicateIssns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    PreparedStatement findStatement = null;
    PreparedStatement deleteStatement = null;
    PreparedStatement insertStatement = null;
    Long previousMdItemSeq = null;
    String previousIssn = null;
    String previousIssnType = null;
    Long mdItemSeq = null;
    String issn = null;
    String issnType = null;
    ResultSet resultSet = null;
    int count;
    boolean done = false;
    boolean foundDuplicate = false;

    // Repeat until there are no duplicated ISSNs.
    while (!done) {
      try {
	// Get all the ISSN rows.
	findStatement = prepareStatementBeforeReady(conn, FIND_ISSNS);
	resultSet = executeQueryBeforeReady(findStatement);

	// Loop through all the ISSN rows.
	while (resultSet.next()) {
	  // Get the data of the row.
	  mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	  issn = resultSet.getString(ISSN_COLUMN);
	  issnType = resultSet.getString(ISSN_TYPE_COLUMN);

	  // Check whether this row is a duplicate of the previous one.
	  if (mdItemSeq.equals(previousMdItemSeq)
	      && issn.equals(previousIssn)
	      && issnType.equals(previousIssnType)) {
	    // Yes: Handle it.
	    if (log.isDebug3()) {
	      log.debug3(DEBUG_HEADER + "Duplicated mdItemSeq = " + mdItemSeq);
	      log.debug3(DEBUG_HEADER + "Duplicated issn = " + issn);
	      log.debug3(DEBUG_HEADER + "Duplicated issnType = " + issnType);
	    }

	    foundDuplicate = true;
	    break;
	  } else {
	    // No: Rememeber it.
	    previousMdItemSeq = mdItemSeq;
	    previousIssn = issn;
	    previousIssnType = issnType;
	  }
	}
      } catch (SQLException sqle) {
	log.error("Cannot find ISSNs", sqle);
	log.error("SQL = '" + FIND_ISSNS + "'.");
	throw new DbException("Cannot find ISSNs", sqle);
      } finally {
	DbManager.safeCloseResultSet(resultSet);
	DbManager.safeCloseStatement(findStatement);
      }

      // Check whether no duplicate ISSNs were found.
      if (!foundDuplicate) {
	// Yes: Done.
	done = true;
	continue;
      }

      // No: Delete all the duplicate rows found.
      try {
	deleteStatement = prepareStatementBeforeReady(conn, REMOVE_ISSN);

	deleteStatement.setLong(1, mdItemSeq);
	deleteStatement.setString(2, issn);
	deleteStatement.setString(3, issnType);

	count = executeUpdateBeforeReady(deleteStatement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
      } catch (SQLException sqle) {
	log.error("Cannot delete ISSN", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("issn = " + issn);
	log.error("issnType = " + issnType);
	log.error("SQL = '" + REMOVE_ISSN + "'.");
	throw new DbException("Cannot delete ISSN '" + issn + "'", sqle);
      } finally {
	DbManager.safeCloseStatement(deleteStatement);
      }

      // Insert back one instance of the deleted rows.
      try {
	insertStatement = prepareStatementBeforeReady(conn, ADD_ISSN);

	insertStatement.setLong(1, mdItemSeq);
	insertStatement.setString(2, issn);
	insertStatement.setString(3, issnType);

	count = executeUpdateBeforeReady(insertStatement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	DbManager.commitOrRollback(conn, log);
      } catch (SQLException sqle) {
	log.error("Cannot add ISSN", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("issn = " + issn);
	log.error("issnType = " + issnType);
	log.error("SQL = '" + ADD_ISSN + "'.");
	throw new DbException("Cannot add ISSN '" + issn + "'", sqle);
      } finally {
	DbManager.safeCloseStatement(insertStatement);
      }

      // Prepare to repeat the process.
      previousMdItemSeq = null;
      previousIssn = null;
      previousIssnType = null;
      foundDuplicate = false;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 6 to version 7.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws DbException
   *           if any problem occurred updating the database.
   */
  private void updateDatabaseFrom6To7(Connection conn) throws DbException {
    final String DEBUG_HEADER = "updateDatabaseFrom6To7(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Create the necessary indices.
    createIndices(conn, VERSION_7_INDEX_CREATE_QUERIES);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates database table indices.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param queries
   *          A String[] with the database queries needed to create the indices.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  protected void createIndices(Connection conn, String[] queries)
      throws DbException {
    final String DEBUG_HEADER = "createIndices(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "queries.length = '" + queries.length + "'.");

    // Loop through all the indices.
    for (String query : queries) {
      log.debug2(DEBUG_HEADER + "Query = " + query);
      PreparedStatement statement = null;

      try {
	statement = prepareStatementBeforeReady(conn, query);
	executeUpdateBeforeReady(statement);
      } finally {
	DbManager.safeCloseStatement(statement);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the data source class name. To be used during initialization.
   * 
   * @return a String with the data source class name.
   */
  String getDataSourceClassNameBeforeReady() {
    return dataSourceClassName;
  }

  /**
   * Provides the data source database name. To be used during initialization.
   * 
   * @return a String with the data source database name.
   */
  String getDataSourceDbNameBeforeReady() {
    return dataSourceDbName;
  }

  /**
   * Provides the data source user name. To be used during initialization.
   * 
   * @return a String with the data source user name.
   */
  String getDataSourceUserBeforeReady() {
    return dataSourceUser;
  }

  /**
   * Provides the data source password. To be used during initialization.
   * 
   * @return a String with the data source password.
   */
  String getDataSourcePasswordBeforeReady() {
    return dataSourcePassword;
  }

  /**
   * Provides a version of a table creation query localized for the database
   * being used.
   * 
   * @param query
   *          A String with the generic table creation query.
   * 
   * @return a String with the localized table creation query.
   */
  private String localizeCreateQuery(String query) {
    final String DEBUG_HEADER = "localizeCreateQuery(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "query = " + query);

    String result = query;

    // Check whether the Derby database is being used.
    if (isTypeDerby()) {
      // Yes.
      result = StringUtil.replaceString(query, "--BigintSerialPk--",
	  				BIGINT_SERIAL_PK_DERBY);
      // No: Check whether the PostgreSQL database is being used.
    } else if (isTypePostgresql()) {
      result = StringUtil.replaceString(query, "--BigintSerialPk--",
	  				BIGINT_SERIAL_PK_PG);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a PostgreSQL database if it does not exist. To be used during
   * initialization.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database to create, if missing.
   * @return <code>true</code> if the database did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if the database creation process failed.
   */
  protected boolean createPostgreSqlDbIfMissing(Connection conn,
      String databaseName) throws DbException {
    final String DEBUG_HEADER = "createPostgreSqlDbIfMissing(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = " + databaseName);

    if (conn == null) {
      throw new DbException("Null connection.");
    }

    // Check whether the database does not exist.
    if (!postgresqlDbExists(conn, databaseName)) {
      // Yes: Create it.
      createPostgresqlDb(conn, databaseName);

      if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true.");
      return true;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false.");
    return false;
  }

  /**
   * Determines whether a named PostgreSQL database exists.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database.
   * @return <code>true</code> if the database exists, <code>false</code>
   *         otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private boolean postgresqlDbExists(Connection conn, String databaseName)
      throws DbException {
    final String DEBUG_HEADER = "postgresqlDbExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = " + databaseName);

    boolean result = false;
    PreparedStatement findDb = null;
    ResultSet resultSet = null;

    try {
      findDb = prepareStatementBeforeReady(conn, FIND_DATABASE_QUERY_PG);
      findDb.setString(1, databaseName);

      resultSet = executeQueryBeforeReady(findDb);
      result = resultSet.next();
    } catch (SQLException sqle) {
      log.error("Cannot find database", sqle);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_PG + "'.");
      throw new DbException("Cannot find database", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findDb);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a PostgreSQL database. To be used during initialization.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database to create.
   * @throws DbException
   *           if the database creation process failed.
   */
  private void createPostgresqlDb(Connection conn, String databaseName)
      throws DbException {
    final String DEBUG_HEADER = "createPostgresqlDb(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = " + databaseName);

    String sql = StringUtil.replaceString(CREATE_DATABASE_QUERY_PG,
					  "--databaseName--", databaseName);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = " + sql);

    PreparedStatement createDb = null;

    try {
      createDb = prepareStatementBeforeReady(conn, sql);

      int count = executeUpdateBeforeReady(createDb);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } finally {
      DbManager.safeCloseStatement(createDb);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a PostgreSQL database schema if it does not exist. To be used
   * during initialization.
   * 
   * @param schemaName
   *          A String with the name of the schema to create, if missing.
   * @param ds
   *          A DataSource with the data source to be used to get a connection.
   * @return <code>true</code> if the schema did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if the schema creation process failed.
   */
  protected boolean createPostgresqlSchemaIfMissing(String schemaName,
      DataSource ds) throws DbException {
    final String DEBUG_HEADER = "createPostgresqlSchemaIfMissing(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "schemaName = " + schemaName);

    // Get a database connection.
    Connection conn = getConnectionBeforeReady(ds);

    try {
      // Check whether the schema does not exist.
      if (!postgresqlSchemaExists(conn, schemaName)) {
	// Yes: Create it.
	createPostgresqlSchema(conn, schemaName);

	// Finish the transaction.
	DbManager.commitOrRollback(conn, log);

	if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = true.");
	return true;
      }
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = false.");
    return false;
  }

  /**
   * Determines whether a named PostgreSQL schema exists.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param schemaName
   *          A String with the name of the schema.
   * @return <code>true</code> if the schema exists, <code>false</code>
   *         otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private boolean postgresqlSchemaExists(Connection conn, String schemaName)
      throws DbException {
    final String DEBUG_HEADER = "postgresqlSchemaExists(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "schemaName = " + schemaName);

    boolean result = false;
    PreparedStatement findSchema = null;
    ResultSet resultSet = null;

    try {
      findSchema = prepareStatementBeforeReady(conn, FIND_SCHEMA_QUERY_PG);
      findSchema.setString(1, schemaName);

      resultSet = executeQueryBeforeReady(findSchema);
      result = resultSet.next();
    } catch (SQLException sqle) {
      log.error("Cannot find schema", sqle);
      log.error("schemaName = '" + schemaName + "'.");
      log.error("SQL = '" + FIND_SCHEMA_QUERY_PG + "'.");
      throw new DbException("Cannot find schema", sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findSchema);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a PostgreSQL schema. To be used during initialization.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param schemaName
   *          A String with the name of the schema to create.
   * @throws DbException
   *           if the schema creation process failed.
   */
  private void createPostgresqlSchema(Connection conn, String schemaName)
      throws DbException {
    final String DEBUG_HEADER = "createPostgresqlSchema(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "schemaName = " + schemaName);

    String sql = StringUtil.replaceString(CREATE_SCHEMA_QUERY_PG,
					  "--schemaName--", schemaName);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = " + sql);

    PreparedStatement createSchema = null;

    try {
      createSchema = prepareStatementBeforeReady(conn, sql);

      int count = executeUpdateBeforeReady(createSchema);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } finally {
      DbManager.safeCloseStatement(createSchema);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }
}
