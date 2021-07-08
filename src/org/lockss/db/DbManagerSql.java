/*

 Copyright (c) 2014-2018 Board of Trustees of Leland Stanford Jr. University,
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

import static org.lockss.db.SqlConstants.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.lockss.app.LockssDaemon;
import org.lockss.config.ConfigManager;
import org.lockss.config.TdbAu;
import org.lockss.exporter.FetchTimeExporter;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;

/**
 * The DbManager SQL code executor.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class DbManagerSql {
  private static final Logger log = Logger.getLogger(DbManagerSql.class);

  /**
   * The indicator to be inserted in the database at the end of truncated text
   * values.
   */
  public static final String TRUNCATION_INDICATOR = "\u0019";

  private static final String DATABASE_VERSION_TABLE_SYSTEM = "database";

  // Derby definition of a big integer primary key column with a value
  // automatically generated from a sequence.
  private static final String BIGINT_SERIAL_PK_DERBY =
      "bigint primary key generated always as identity";

  // PostgreSQL definition of a big integer primary key column with a value
  // automatically generated from a sequence.
  private static final String BIGINT_SERIAL_PK_PG = "bigserial primary key";

  // MySQL definition of a big integer primary key column with a value
  // automatically generated from a sequence.
  private static final String BIGINT_SERIAL_PK_MYSQL =
      "bigint auto_increment primary key";

  // Database metadata keys.
  private static final String COLUMN_NAME = "COLUMN_NAME";
  private static final String COLUMN_SIZE = "COLUMN_SIZE";
  private static final String FUNCTION_NAME = "FUNCTION_NAME";
  private static final String TYPE_NAME = "TYPE_NAME";

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

  // Query to create the MySQL table for recording requests used for COUNTER
  // reports.
  private static final String OBSOLETE_CREATE_REQUESTS_TABLE_MYSQL_QUERY =
      "create " + "table " + OBSOLETE_REQUESTS_TABLE + " ("
      + LOCKSS_ID_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_LOCKSS_ID_REQUESTS (" + LOCKSS_ID_COLUMN + ") "
      + "REFERENCES " + OBSOLETE_TITLES_TABLE + "(" + LOCKSS_ID_COLUMN + "),"
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

  // Query to create the MySQL table for recording type aggregates (PDF vs.
  // HTML, Full vs. Section, etc.) used for COUNTER reports.
  private static final String OBSOLETE_CREATE_TYPE_AGGREGATES_TABLE_MYSQL_QUERY
  = "create table " + OBSOLETE_TYPE_AGGREGATES_TABLE + " ("
      + LOCKSS_ID_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_LOCKSS_ID_TYPE_AGGREGATES (" + LOCKSS_ID_COLUMN + ") "
      + "REFERENCES " + OBSOLETE_TITLES_TABLE + "(" + LOCKSS_ID_COLUMN + "),"
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

  // Query to create the MySQL table for recording publication year aggregates
  // used for COUNTER reports.
  private static final String
  OBSOLETE_CREATE_PUBYEAR_AGGREGATES_TABLE_MYSQL_QUERY = "create table "
      + OBSOLETE_PUBYEAR_AGGREGATES_TABLE + " ("
      + LOCKSS_ID_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_LOCKSS_ID_PUBYEAR_AGGREGATES (" + LOCKSS_ID_COLUMN
      + ") " + "REFERENCES " + OBSOLETE_TITLES_TABLE + "(" + LOCKSS_ID_COLUMN
      + "),"
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
      + OBSOLETE_PLATFORM_COLUMN + " varchar(" + MAX_PLATFORM_COLUMN + ")"
      + ")";

  // Query to create the table for recording archival units.
  static final String CREATE_AU_TABLE_QUERY = "create table "
      + AU_TABLE + " ("
      + AU_SEQ_COLUMN + " --BigintSerialPk--,"
      + PLUGIN_SEQ_COLUMN + " bigint not null references " + PLUGIN_TABLE
      + " (" + PLUGIN_SEQ_COLUMN + ") on delete cascade,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") not null)";

  // Query to create the table for recording archival units metadata.
  static final String CREATE_AU_MD_TABLE_QUERY = "create table "
      + AU_MD_TABLE + " ("
      + AU_MD_SEQ_COLUMN + " --BigintSerialPk--,"
      + AU_SEQ_COLUMN + " bigint not null references " + AU_TABLE
      + " (" + AU_SEQ_COLUMN + ") on delete cascade,"
      + MD_VERSION_COLUMN + " smallint not null,"
      + EXTRACT_TIME_COLUMN + " bigint not null"
      + ")";

  // Query to create the table for recording metadata item types.
  static final String CREATE_MD_ITEM_TYPE_TABLE_QUERY = "create table "
      + MD_ITEM_TYPE_TABLE + " ("
      + MD_ITEM_TYPE_SEQ_COLUMN + " --BigintSerialPk--,"
      + TYPE_NAME_COLUMN + " varchar(" + MAX_TYPE_NAME_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items.
  static final String CREATE_MD_ITEM_TABLE_QUERY = "create table "
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
  static final String CREATE_MD_ITEM_NAME_TABLE_QUERY = "create table "
      + MD_ITEM_NAME_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + NAME_COLUMN + " varchar(" + MAX_NAME_COLUMN + ") not null,"
      + NAME_TYPE_COLUMN + " varchar(" + MAX_NAME_TYPE_COLUMN  + ") not null"
      + ")";

  // Query to create the table for recording metadata keys.
  static final String CREATE_MD_KEY_TABLE_QUERY = "create table "
      + MD_KEY_TABLE + " ("
      + MD_KEY_SEQ_COLUMN + " --BigintSerialPk--,"
      + KEY_NAME_COLUMN + " varchar(" + MAX_NAME_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items generic key/value
  // pairs.
  static final String CREATE_MD_TABLE_QUERY = "create table "
      + MD_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + MD_KEY_SEQ_COLUMN + " bigint not null references " + MD_KEY_TABLE
      + " (" + MD_KEY_SEQ_COLUMN + ") on delete cascade,"
      + MD_VALUE_COLUMN + " varchar(" + MAX_MD_VALUE_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording bibliographic items.
  static final String CREATE_BIB_ITEM_TABLE_QUERY = "create table "
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
  static final String CREATE_URL_TABLE_QUERY = "create table "
      + URL_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + FEATURE_COLUMN + " varchar(" + MAX_FEATURE_COLUMN + ") not null,"
      + URL_COLUMN + " varchar(" + MAX_URL_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items authors.
  static final String CREATE_AUTHOR_TABLE_QUERY = "create table "
      + AUTHOR_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + AUTHOR_NAME_COLUMN + " varchar(" + MAX_AUTHOR_COLUMN + ") not null,"
      + AUTHOR_IDX_COLUMN + " smallint not null"
      + ")";

  // Query to create the table for recording metadata items keywords.
  static final String CREATE_KEYWORD_TABLE_QUERY = "create table "
      + KEYWORD_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + KEYWORD_COLUMN + " varchar(" + MAX_KEYWORD_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items DOIs.
  static final String CREATE_DOI_TABLE_QUERY = "create table "
      + DOI_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + DOI_COLUMN + " varchar(" + MAX_DOI_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items ISSNs.
  static final String CREATE_ISSN_TABLE_QUERY = "create table "
      + ISSN_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + ISSN_COLUMN + " varchar(" + MAX_ISSN_COLUMN + ") not null,"
      + ISSN_TYPE_COLUMN + " varchar(" + MAX_ISSN_TYPE_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording metadata items ISBNs.
  static final String CREATE_ISBN_TABLE_QUERY = "create table "
      + ISBN_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + ISBN_COLUMN + " varchar(" + MAX_ISBN_COLUMN + ") not null,"
      + ISBN_TYPE_COLUMN + " varchar(" + MAX_ISBN_TYPE_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording publishers.
  static final String CREATE_PUBLISHER_TABLE_QUERY = "create table "
      + PUBLISHER_TABLE + " ("
      + PUBLISHER_SEQ_COLUMN + " --BigintSerialPk--,"
      + PUBLISHER_NAME_COLUMN + " varchar(" + MAX_NAME_COLUMN + ") not null"
      + ")";

  // Query to create the table for recording publications.
  static final String CREATE_PUBLICATION_TABLE_QUERY = "create table "
      + PUBLICATION_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " --BigintSerialPk--,"
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + PUBLISHER_SEQ_COLUMN + " bigint not null references " + PUBLISHER_TABLE
      + " (" + PUBLISHER_SEQ_COLUMN + ") on delete cascade,"
      + OBSOLETE_PUBLICATION_ID_COLUMN + " varchar("
      + OBSOLETE_MAX_PUBLICATION_ID_COLUMN + ")" + ")";

  // Query to create the table for recording pending AUs to index.
  static final String CREATE_PENDING_AU_TABLE_QUERY = "create table "
      + PENDING_AU_TABLE + " ("
      + PLUGIN_ID_COLUMN + " varchar(" + MAX_PLUGIN_ID_COLUMN + ") not null,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") not null,"
      + PRIORITY_COLUMN + " bigint not null)";

  // Query to create the table for recording versions.
  static final String CREATE_VERSION_TABLE_QUERY = "create table "
      + VERSION_TABLE + " ("
      + SYSTEM_COLUMN + " varchar(" + MAX_SYSTEM_COLUMN + ") not null,"
      + VERSION_COLUMN + " smallint not null"
      + ")";

  // Query to create the table for recording requests used for COUNTER reports.
  static final String REQUEST_TABLE_CREATE_QUERY = "create table "
      + COUNTER_REQUEST_TABLE + " ("
      + URL_COLUMN + " varchar(" + MAX_URL_COLUMN + ") NOT NULL, "
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + REQUEST_DAY_COLUMN + " smallint NOT NULL,"
      + IN_AGGREGATION_COLUMN + " boolean)";

  // Query to create the table for recording book type aggregates (Full vs.
  // Section) used for COUNTER reports.
  static final String BOOK_TYPE_AGGREGATES_TABLE_CREATE_QUERY = "create table "
      + COUNTER_BOOK_TYPE_AGGREGATES_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PUBLICATION_SEQ_BOOK_TYPE_AGGREGATES"
      + " REFERENCES " + PUBLICATION_TABLE + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + FULL_REQUESTS_COLUMN + " integer,"
      + SECTION_REQUESTS_COLUMN + " integer)";

  // Query to create the MySQL table for recording book type aggregates (Full
  // vs. Section) used for COUNTER reports.
  private static final String BOOK_TYPE_AGGREGATES_TABLE_CREATE_MYSQL_QUERY =
      "create table " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_PUBLICATION_SEQ_BOOK_TYPE_AGGREGATES ("
      + PUBLICATION_SEQ_COLUMN + ") "
      + "REFERENCES " + PUBLICATION_TABLE + "(" + PUBLICATION_SEQ_COLUMN + "),"
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + FULL_REQUESTS_COLUMN + " integer,"
      + SECTION_REQUESTS_COLUMN + " integer)";

  // Query to create the table for recording journal type aggregates (PDF vs.
  // HTML) used for COUNTER reports.
  static final String JOURNAL_TYPE_AGGREGATES_TABLE_CREATE_QUERY = "create "
      + "table " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PUBLICATION_SEQ_JOURNAL_TYPE_AGGREGATES"
      + " REFERENCES " + PUBLICATION_TABLE + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + TOTAL_REQUESTS_COLUMN + " integer,"
      + HTML_REQUESTS_COLUMN + " integer,"
      + PDF_REQUESTS_COLUMN + " integer)";

  // Query to create the MySQL table for recording journal type aggregates (PDF
  // vs. HTML) used for COUNTER reports.
  private static final String JOURNAL_TYPE_AGGREGATES_TABLE_CREATE_MYSQL_QUERY
  = "create table " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_PUBLICATION_SEQ_JOURNAL_TYPE_AGGREGATES ("
      + PUBLICATION_SEQ_COLUMN + ") "
      + "REFERENCES " + PUBLICATION_TABLE + "(" + PUBLICATION_SEQ_COLUMN + "),"
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + TOTAL_REQUESTS_COLUMN + " integer,"
      + HTML_REQUESTS_COLUMN + " integer,"
      + PDF_REQUESTS_COLUMN + " integer)";

  // Query to create the table for recording journal publication year aggregates
  // used for COUNTER reports.
  static final String JOURNAL_PUBYEAR_AGGREGATE_TABLE_CREATE_QUERY = "create "
      + "table " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PUBLICATION_SEQ_JOURNAL_PUBYEAR_AGGREGATE"
      + " REFERENCES " + PUBLICATION_TABLE + ","
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + PUBLICATION_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUESTS_COLUMN + " integer NOT NULL)";

  // Query to create the MySQL table for recording journal publication year
  // aggregates used for COUNTER reports.
  private static final String JOURNAL_PUBYEAR_AGGREGATE_TABLE_CREATE_MYSQL_QUERY
  = "create table " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE + " ("
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_PUBLICATION_SEQ_JOURNAL_PUBYEAR_AGGREGATE ("
      + PUBLICATION_SEQ_COLUMN + ") "
      + "REFERENCES " + PUBLICATION_TABLE + "(" + PUBLICATION_SEQ_COLUMN + "),"
      + IS_PUBLISHER_INVOLVED_COLUMN + " boolean NOT NULL,"
      + REQUEST_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUEST_MONTH_COLUMN + " smallint NOT NULL,"
      + PUBLICATION_YEAR_COLUMN + " smallint NOT NULL,"
      + REQUESTS_COLUMN + " integer NOT NULL)";

  // Query to create the table for platforms.
  static final String CREATE_PLATFORM_TABLE_QUERY = "create table "
      + PLATFORM_TABLE + " ("
      + PLATFORM_SEQ_COLUMN + " --BigintSerialPk--,"
      + PLATFORM_NAME_COLUMN + " varchar(" + MAX_PLATFORM_COLUMN + ") not null"
      + ")";

  // Query to create the table for subscriptions.
  static final String CREATE_SUBSCRIPTION_TABLE_QUERY = "create table "
      + SUBSCRIPTION_TABLE + " ("
      + SUBSCRIPTION_SEQ_COLUMN + " --BigintSerialPk--,"
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PUBLICATION_SEQ_SUBSCRIPTION"
      + " REFERENCES " + PUBLICATION_TABLE + " on delete cascade,"
      + PLATFORM_SEQ_COLUMN + " bigint not null"
      + " CONSTRAINT FK_PLATFORM_SEQ_SUBSCRIPTION"
      + " REFERENCES " + PLATFORM_TABLE + " on delete cascade"
      + ")";

  // Query to create the table for subscriptions for MySQL.
  private static final String CREATE_SUBSCRIPTION_TABLE_MYSQL_QUERY = "create "
      + "table " + SUBSCRIPTION_TABLE + " ("
      + SUBSCRIPTION_SEQ_COLUMN + " --BigintSerialPk--,"
      + PUBLICATION_SEQ_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_PUBLICATION_SEQ_SUBSCRIPTION (" + PUBLICATION_SEQ_COLUMN
      + ") REFERENCES " + PUBLICATION_TABLE + "(" + PUBLICATION_SEQ_COLUMN
      + ") on delete cascade,"
      + PLATFORM_SEQ_COLUMN + " bigint not null,"
      + "FOREIGN KEY FK_PLATFORM_SEQ_SUBSCRIPTION (" + PLATFORM_SEQ_COLUMN
      + ") REFERENCES " + PLATFORM_TABLE + "(" + PLATFORM_SEQ_COLUMN
      + ") on delete cascade"
      + ")";

  // Query to create the table for subscription ranges.
  static final String CREATE_SUBSCRIPTION_RANGE_TABLE_QUERY = "create table "
      + SUBSCRIPTION_RANGE_TABLE + " ("
      + SUBSCRIPTION_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_SUBSCRIPTION_SEQ_COLUMN_SUBSCRIPTION_RANGE"
      + " REFERENCES " + SUBSCRIPTION_TABLE + " on delete cascade,"
      + OBSOLETE_RANGE_COLUMN + " varchar(" + MAX_RANGE_COLUMN + ") not null,"
      + SUBSCRIBED_COLUMN + " boolean not null"
      + ")";

  // Query to create the table for subscription ranges for MySQL.
  private static final String CREATE_SUBSCRIPTION_RANGE_TABLE_MYSQL_QUERY =
      "create table " + SUBSCRIPTION_RANGE_TABLE + " ("
      + SUBSCRIPTION_SEQ_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_SUBSCRIPTION_SEQ_COLUMN_SUBSCRIPTION_RANGE ("
      + SUBSCRIPTION_SEQ_COLUMN
      + ") REFERENCES " + SUBSCRIPTION_TABLE + "(" + SUBSCRIPTION_SEQ_COLUMN
      + ") on delete cascade,"
      + SUBSCRIPTION_RANGE_COLUMN + " varchar(" + MAX_RANGE_COLUMN
      + ") not null,"
      + SUBSCRIBED_COLUMN + " boolean not null"
      + ")";

  // Query to create the table for unconfigured Archival Units.
  static final String CREATE_UNCONFIGURED_AU_TABLE_QUERY = "create table "
      + UNCONFIGURED_AU_TABLE + " ("
      + PLUGIN_ID_COLUMN + " varchar(" + MAX_PLUGIN_ID_COLUMN + ") not null,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") not null"
      + ")";

  // Query to create the table for Archival Unit problems.
  static final String CREATE_AU_PROBLEM_TABLE_QUERY = "create table "
      + AU_PROBLEM_TABLE + " ("
      + PLUGIN_ID_COLUMN + " varchar(" + MAX_PLUGIN_ID_COLUMN + ") not null,"
      + AU_KEY_COLUMN + " varchar(" + MAX_AU_KEY_COLUMN + ") not null,"
      + PROBLEM_COLUMN + " varchar(" + MAX_PROBLEM_COLUMN + ") not null"
      + ")";

  // Query to create the table for identifying the last run of incremental
  // tasks.
  private static final String CREATE_LAST_RUN_TABLE_QUERY = "create table "
      + LAST_RUN_TABLE + " ("
      + LABEL_COLUMN + " varchar(" + MAX_LABEL_COLUMN + ") not null,"
      + LAST_VALUE_COLUMN + " varchar(" + MAX_LAST_VALUE_COLUMN + ") not null"
      + ")";

  // Query to create the table for providers.
  private static final String CREATE_PROVIDER_TABLE_QUERY = "create table "
      + PROVIDER_TABLE + " ("
      + PROVIDER_SEQ_COLUMN + " --BigintSerialPk--,"
      + PROVIDER_LID_COLUMN + " varchar(" + MAX_LID_COLUMN + "),"
      + PROVIDER_NAME_COLUMN + " varchar(" + MAX_NAME_COLUMN + ") not null"
      + ")";

  // Query to create the table for publication proprietary identifiers.
  private static final String CREATE_PROPRIETARY_ID_TABLE_QUERY = "create "
      + "table " + PROPRIETARY_ID_TABLE + " ("
      + MD_ITEM_SEQ_COLUMN + " bigint not null references " + MD_ITEM_TABLE
      + " (" + MD_ITEM_SEQ_COLUMN + ") on delete cascade,"
      + PROPRIETARY_ID_COLUMN + " varchar(" + MAX_PROPRIETARY_ID_COLUMN
      + ") not null"
      + ")";

  // Query to create the table for publisher subscriptions.
  private static final String CREATE_PUBLISHER_SUBSCRIPTION_TABLE_QUERY =
      "create table " + PUBLISHER_SUBSCRIPTION_TABLE + " ("
      + PUBLISHER_SUBSCRIPTION_SEQ_COLUMN + " --BigintSerialPk--,"
      + PUBLISHER_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PUBLISHER_SEQ_SUBSCRIPTION"
      + " REFERENCES " + PUBLISHER_TABLE + " on delete cascade,"
      + PROVIDER_SEQ_COLUMN + " bigint NOT NULL"
      + " CONSTRAINT FK_PROVIDER_SEQ_SUBSCRIPTION"
      + " REFERENCES " + PROVIDER_TABLE + " on delete cascade,"
      + SUBSCRIBED_COLUMN + " boolean not null"
      + ")";

  // Query to create the table for publisher subscriptions for MySQL.
  private static final String CREATE_PUBLISHER_SUBSCRIPTION_TABLE_MYSQL_QUERY =
      "create table " + PUBLISHER_SUBSCRIPTION_TABLE + " ("
      + PUBLISHER_SUBSCRIPTION_SEQ_COLUMN + " --BigintSerialPk--,"
      + PUBLISHER_SEQ_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_PUBLISHER_SEQ_SUBSCRIPTION (" + PUBLISHER_SEQ_COLUMN
      + ") REFERENCES " + PUBLISHER_TABLE + "(" + PUBLISHER_SEQ_COLUMN
      + ") on delete cascade,"
      + PROVIDER_SEQ_COLUMN + " bigint NOT NULL,"
      + "FOREIGN KEY FK_PROVIDER_SEQ_SUBSCRIPTION (" + PROVIDER_SEQ_COLUMN
      + ") REFERENCES " + PROVIDER_TABLE + "(" + PROVIDER_SEQ_COLUMN
      + ") on delete cascade,"
      + SUBSCRIBED_COLUMN + " boolean not null"
      + ")";

  // Query to insert the database version.
  private static final String INSERT_VERSION_QUERY = "insert into "
      + VERSION_TABLE
      + "(" + SYSTEM_COLUMN
      + "," + VERSION_COLUMN
      + ") values (?,?)";

  // Query to insert a type of metadata item.
  private static final String INSERT_MD_ITEM_TYPE_QUERY = "insert into "
      + MD_ITEM_TYPE_TABLE
      + "(" + MD_ITEM_TYPE_SEQ_COLUMN
      + "," + TYPE_NAME_COLUMN
      + ") values (default,?)";

  // Query to get the database versions sorted in ascending order.
  private static final String GET_SORTED_DATABASE_VERSIONS_QUERY = "select "
      + VERSION_COLUMN
      + " from " + VERSION_TABLE
      + " where " + SYSTEM_COLUMN + " = '" + DATABASE_VERSION_TABLE_SYSTEM
      + "' order by " + VERSION_COLUMN + " asc";

  // SQL statement that creates a database in PostgreSQL.
  private static final String CREATE_DATABASE_QUERY_PG =
      "create database \"--databaseName--\" with template template0";

  // SQL statement that finds a database in PostgreSQL.
  private static final String FIND_DATABASE_QUERY_PG = "select datname"
      + " from pg_catalog.pg_database where datname = ?";

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

  // The SQL code used to create the necessary version 1 MySQL database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_1_TABLE_CREATE_MYSQL_QUERIES
  = new LinkedHashMap<String, String>() {
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
      put(OBSOLETE_REQUESTS_TABLE,
	  OBSOLETE_CREATE_REQUESTS_TABLE_MYSQL_QUERY);
      put(OBSOLETE_PUBYEAR_AGGREGATES_TABLE,
	  OBSOLETE_CREATE_PUBYEAR_AGGREGATES_TABLE_MYSQL_QUERY);
      put(OBSOLETE_TYPE_AGGREGATES_TABLE,
	  OBSOLETE_CREATE_TYPE_AGGREGATES_TABLE_MYSQL_QUERY);
    }
  };

  // SQL statements that create the necessary version 1 functions.
  private static final String[] VERSION_1_FUNCTION_CREATE_QUERIES = new String[]
  {
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
	+ "parameter style java no sql"
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

  // The SQL code used to create the necessary version 2 MySQL database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_2_TABLE_CREATE_MYSQL_QUERIES
  = new LinkedHashMap<String, String>() {
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
              JOURNAL_PUBYEAR_AGGREGATE_TABLE_CREATE_MYSQL_QUERY);
          put(COUNTER_BOOK_TYPE_AGGREGATES_TABLE,
              BOOK_TYPE_AGGREGATES_TABLE_CREATE_MYSQL_QUERY);
          put(COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE,
              JOURNAL_TYPE_AGGREGATES_TABLE_CREATE_MYSQL_QUERY);
        }
  };

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
  private static final String[] VERSION_2_FUNCTION_CREATE_QUERIES = new String[]
  {
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
	+ "parameter style java no sql"
  };

  // SQL statements that create the necessary version 3 indices.
  private static final String[] VERSION_3_INDEX_CREATE_QUERIES = new String[] {
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

  // SQL statements that create the necessary MySQL version 3 indices.
  private static final String[] VERSION_3_INDEX_CREATE_MYSQL_QUERIES =
    new String[] {
    // TODO: Make the index unique when MySQL is fixed.
    "create index idx1_" + PLUGIN_TABLE + " on " + PLUGIN_TABLE
    + "(" + PLUGIN_ID_COLUMN + "(255))",

    "create index idx1_" + AU_TABLE + " on " + AU_TABLE
    + "(" + AU_KEY_COLUMN + "(255))",

    "create index idx1_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + DATE_COLUMN + ")",

    "create index idx1_" + MD_ITEM_NAME_TABLE + " on " + MD_ITEM_NAME_TABLE
    + "(" + NAME_COLUMN + "(255))",

    // TODO: Make the index unique when MySQL is fixed.
    "create index idx1_" + PUBLISHER_TABLE + " on " + PUBLISHER_TABLE
    + "(" + PUBLISHER_NAME_COLUMN + "(255))",

    "create index idx1_" + ISSN_TABLE + " on " + ISSN_TABLE
    + "(" + ISSN_COLUMN + ")",

    "create index idx1_" + ISBN_TABLE + " on " + ISBN_TABLE
    + "(" + ISBN_COLUMN + ")",

    "create index idx1_" + URL_TABLE + " on " + URL_TABLE
    + "(" + FEATURE_COLUMN + ")",

    "create index idx2_" + URL_TABLE + " on " + URL_TABLE
    + "(" + URL_COLUMN + "(255))",

    "create index idx1_" + BIB_ITEM_TABLE + " on " + BIB_ITEM_TABLE
    + "(" + VOLUME_COLUMN + ")",

    "create index idx2_" + BIB_ITEM_TABLE + " on " + BIB_ITEM_TABLE
    + "(" + ISSUE_COLUMN + ")",

    "create index idx3_" + BIB_ITEM_TABLE + " on " + BIB_ITEM_TABLE
    + "(" + START_PAGE_COLUMN + ")",

    "create index idx1_" + AUTHOR_TABLE + " on " + AUTHOR_TABLE
    + "(" + AUTHOR_NAME_COLUMN + ")",

    // TODO: Make the index unique when MySQL is fixed.
    "create index idx1_" + PENDING_AU_TABLE + " on " + PENDING_AU_TABLE
    + "(" + PLUGIN_ID_COLUMN + "(255)," + AU_KEY_COLUMN + "(255))"
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

  // The SQL code used to create the necessary version 4 database tables for
  // MySQL.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_4_TABLE_CREATE_MYSQL_QUERIES
    = new LinkedHashMap<String, String>() {{
      put(PLATFORM_TABLE, CREATE_PLATFORM_TABLE_QUERY);
      put(SUBSCRIPTION_TABLE, CREATE_SUBSCRIPTION_TABLE_MYSQL_QUERY);
      put(SUBSCRIPTION_RANGE_TABLE,
	  CREATE_SUBSCRIPTION_RANGE_TABLE_MYSQL_QUERY);
      put(UNCONFIGURED_AU_TABLE, CREATE_UNCONFIGURED_AU_TABLE_QUERY);
    }};

  // SQL statements that create the necessary version 4 indices.
  private static final String[] VERSION_4_INDEX_CREATE_QUERIES = new String[] {
    "create unique index idx1_" + UNCONFIGURED_AU_TABLE
    + " on " + UNCONFIGURED_AU_TABLE
    + "(" + PLUGIN_ID_COLUMN + "," + AU_KEY_COLUMN + ")",
    "create unique index idx1_" + SUBSCRIPTION_RANGE_TABLE
    + " on " + SUBSCRIPTION_RANGE_TABLE
    + "(" + SUBSCRIPTION_SEQ_COLUMN + "," + OBSOLETE_RANGE_COLUMN + ")"
    };

  // SQL statements that create the necessary version 4 indices for MySQL.
  private static final String[] VERSION_4_INDEX_CREATE_MYSQL_QUERIES =
    new String[] {
    // TODO: Make the index unique when MySQL is fixed.
    "create index idx1_" + UNCONFIGURED_AU_TABLE
    + " on " + UNCONFIGURED_AU_TABLE
    + "(" + PLUGIN_ID_COLUMN + "(255)," + AU_KEY_COLUMN + "(255))",
    // TODO: Make the index unique when MySQL is fixed.
    "create index idx1_" + SUBSCRIPTION_RANGE_TABLE
    + " on " + SUBSCRIPTION_RANGE_TABLE
    + "(" + SUBSCRIPTION_SEQ_COLUMN + "," + SUBSCRIPTION_RANGE_COLUMN + ")"
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
      + " set " + OBSOLETE_PLATFORM_COLUMN + " = ?"
      + " where " + OBSOLETE_PLATFORM_COLUMN + " is null";
  
  // SQL statement that obtains all the existing platform names in the plugin
  // table.
  private static final String GET_VERSION_2_PLATFORMS = "select distinct "
      + OBSOLETE_PLATFORM_COLUMN
      + " from " + PLUGIN_TABLE;

  // Query to add a platform.
  private static final String INSERT_PLATFORM_QUERY = "insert into "
      + PLATFORM_TABLE
      + "(" + PLATFORM_SEQ_COLUMN
      + "," + PLATFORM_NAME_COLUMN
      + ") values (default,?)";

  // Query to update the platform reference of a plugin.
  private static final String UPDATE_PLUGIN_PLATFORM_SEQ_QUERY = "update "
      + PLUGIN_TABLE
      + " set " + PLATFORM_SEQ_COLUMN + " = ?"
      + " where " + OBSOLETE_PLATFORM_COLUMN + " = ?";

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
  private static final String[] VERSION_5_INDEX_CREATE_QUERIES = new String[] {
    "create index idx1_" + AU_PROBLEM_TABLE
    + " on " + AU_PROBLEM_TABLE
    + "(" + PLUGIN_ID_COLUMN + "," + AU_KEY_COLUMN + ")"
  };

  // SQL statements that create the necessary version 5 indices for MySQL.
  private static final String[] VERSION_5_INDEX_CREATE_MYSQL_QUERIES =
    new String[] {
    "create index idx1_" + AU_PROBLEM_TABLE
    + " on " + AU_PROBLEM_TABLE
    + "(" + PLUGIN_ID_COLUMN + "(255)," + AU_KEY_COLUMN + "(255))"
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
  private static final String[] VERSION_6_INDEX_CREATE_QUERIES = new String[] {
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
  private static final String[] VERSION_7_INDEX_CREATE_QUERIES = new String[] {
    "create index idx2_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + PARENT_SEQ_COLUMN + ")",
    "create index idx3_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + AU_MD_SEQ_COLUMN + ")",
    "create index idx1_" + PUBLICATION_TABLE + " on " + PUBLICATION_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")"
  };
  
  // SQL statement that creates a schema in PostgreSQL.
  private static final String CREATE_SCHEMA_QUERY_PG =
      "create schema \"--schemaName--\"";
  
  // SQL statement that finds a schema in PostgreSQL.
  private static final String FIND_SCHEMA_QUERY_PG = "select nspname"
      + " from pg_namespace where nspname = ?";

  // SQL statement that adds the index column to the subscription range table.
  private static final String ADD_SUBSCRIPTION_RANGE_INDEX_COLUMN_QUERY =
      "alter table " + SUBSCRIPTION_RANGE_TABLE
      + " add column " + RANGE_IDX_COLUMN + " smallint not null default 0";

  // The SQL code used to create the necessary version 9 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_9_TABLE_CREATE_QUERIES =
    new LinkedHashMap<String, String>() {{
      put(LAST_RUN_TABLE, CREATE_LAST_RUN_TABLE_QUERY);
    }};

  // The SQL code used to add the necessary version 9 database table columns.
  private static final String[] VERSION_9_COLUMN_ADD_QUERIES = new String[] {
    "alter table " + PLUGIN_TABLE + " add column " + IS_BULK_CONTENT_COLUMN
    + " boolean not null default false",
    "alter table " + AU_MD_TABLE + " add column " + CREATION_TIME_COLUMN
    + " bigint not null default -1",
    "alter table " + MD_ITEM_TABLE + " add column " + FETCH_TIME_COLUMN
    + " bigint not null default -1"
    };

  // SQL statements that create the necessary version 9 indices.
  private static final String[] VERSION_9_INDEX_CREATE_QUERIES = new String[] {
    "create index idx2_" + AU_TABLE + " on " + AU_TABLE
    + "(" + PLUGIN_SEQ_COLUMN + ")",
    "create index idx2_" + AU_MD_TABLE + " on " + AU_MD_TABLE
    + "(" + AU_SEQ_COLUMN + ")",
    "create index idx3_" + AU_MD_TABLE + " on " + AU_MD_TABLE
    + "(" + CREATION_TIME_COLUMN + ")",
    "create index idx4_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + MD_ITEM_TYPE_SEQ_COLUMN + ")",
    "create index idx5_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + FETCH_TIME_COLUMN + ")",
    "create index idx3_" + URL_TABLE + " on " + URL_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx2_" + PUBLICATION_TABLE + " on " + PUBLICATION_TABLE
    + "(" + PUBLISHER_SEQ_COLUMN + ")",
    "create index idx2_" + MD_ITEM_NAME_TABLE + " on " + MD_ITEM_NAME_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx3_" + MD_ITEM_NAME_TABLE + " on " + MD_ITEM_NAME_TABLE
    + "(" + NAME_TYPE_COLUMN + ")",
    "create index idx2_" + PENDING_AU_TABLE + " on " + PENDING_AU_TABLE
    + "(" + PRIORITY_COLUMN + ")",
    "create index idx2_" + AUTHOR_TABLE + " on " + AUTHOR_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx1_" + KEYWORD_TABLE + " on " + KEYWORD_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx1_" + DOI_TABLE + " on " + DOI_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx2_" + DOI_TABLE + " on " + DOI_TABLE
    + "(" + DOI_COLUMN + ")",
    "create index idx4_" + BIB_ITEM_TABLE + " on " + BIB_ITEM_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx2_" + PLUGIN_TABLE + " on " + PLUGIN_TABLE
    + "(" + PLATFORM_SEQ_COLUMN + ")",
    "create index idx1_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + URL_COLUMN + ")",
    "create index idx2_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + IS_PUBLISHER_INVOLVED_COLUMN + ")",
    "create index idx3_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + REQUEST_YEAR_COLUMN + ")",
    "create index idx4_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + REQUEST_MONTH_COLUMN + ")",
    "create index idx5_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + IN_AGGREGATION_COLUMN + ")",
    "create index idx1_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + PUBLICATION_SEQ_COLUMN + ")",
    "create index idx2_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + IS_PUBLISHER_INVOLVED_COLUMN + ")",
    "create index idx3_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + REQUEST_YEAR_COLUMN + ")",
    "create index idx4_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + REQUEST_MONTH_COLUMN + ")",
    "create index idx5_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + FULL_REQUESTS_COLUMN + ")",
    "create index idx6_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + SECTION_REQUESTS_COLUMN + ")",
    "create index idx1_" + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + "(" + PUBLICATION_SEQ_COLUMN + ")",
    "create index idx2_" + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + "(" + IS_PUBLISHER_INVOLVED_COLUMN + ")",
    "create index idx3_" + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + "(" + REQUEST_YEAR_COLUMN + ")",
    "create index idx4_" + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + "(" + REQUEST_MONTH_COLUMN + ")",
    "create index idx1_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + PUBLICATION_SEQ_COLUMN + ")",
    "create index idx2_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + IS_PUBLISHER_INVOLVED_COLUMN + ")",
    "create index idx3_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + REQUEST_YEAR_COLUMN + ")",
    "create index idx4_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + REQUEST_MONTH_COLUMN + ")",
    "create index idx5_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + PUBLICATION_YEAR_COLUMN + ")"
  };

  // SQL statements that create the necessary version 9 indices for MySQL.
  private static final String[] VERSION_9_INDEX_CREATE_MYSQL_QUERIES =
      new String[] {
    "create index idx2_" + AU_TABLE + " on " + AU_TABLE
    + "(" + PLUGIN_SEQ_COLUMN + ")",
    "create index idx2_" + AU_MD_TABLE + " on " + AU_MD_TABLE
    + "(" + AU_SEQ_COLUMN + ")",
    "create index idx3_" + AU_MD_TABLE + " on " + AU_MD_TABLE
    + "(" + CREATION_TIME_COLUMN + ")",
    "create index idx4_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + MD_ITEM_TYPE_SEQ_COLUMN + ")",
    "create index idx5_" + MD_ITEM_TABLE + " on " + MD_ITEM_TABLE
    + "(" + FETCH_TIME_COLUMN + ")",
    "create index idx3_" + URL_TABLE + " on " + URL_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx2_" + PUBLICATION_TABLE + " on " + PUBLICATION_TABLE
    + "(" + PUBLISHER_SEQ_COLUMN + ")",
    "create index idx2_" + MD_ITEM_NAME_TABLE + " on " + MD_ITEM_NAME_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx3_" + MD_ITEM_NAME_TABLE + " on " + MD_ITEM_NAME_TABLE
    + "(" + NAME_TYPE_COLUMN + ")",
    "create index idx2_" + PENDING_AU_TABLE + " on " + PENDING_AU_TABLE
    + "(" + PRIORITY_COLUMN + ")",
    "create index idx2_" + AUTHOR_TABLE + " on " + AUTHOR_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx1_" + KEYWORD_TABLE + " on " + KEYWORD_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx1_" + DOI_TABLE + " on " + DOI_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx2_" + DOI_TABLE + " on " + DOI_TABLE
    + "(" + DOI_COLUMN + "(255))",
    "create index idx4_" + BIB_ITEM_TABLE + " on " + BIB_ITEM_TABLE
    + "(" + MD_ITEM_SEQ_COLUMN + ")",
    "create index idx2_" + PLUGIN_TABLE + " on " + PLUGIN_TABLE
    + "(" + PLATFORM_SEQ_COLUMN + ")",
    "create index idx1_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + URL_COLUMN + "(255))",
    "create index idx2_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + IS_PUBLISHER_INVOLVED_COLUMN + ")",
    "create index idx3_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + REQUEST_YEAR_COLUMN + ")",
    "create index idx4_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + REQUEST_MONTH_COLUMN + ")",
    "create index idx5_" + COUNTER_REQUEST_TABLE
    + " on " + COUNTER_REQUEST_TABLE + "(" + IN_AGGREGATION_COLUMN + ")",
    "create index idx1_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + PUBLICATION_SEQ_COLUMN + ")",
    "create index idx2_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + IS_PUBLISHER_INVOLVED_COLUMN + ")",
    "create index idx3_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + REQUEST_YEAR_COLUMN + ")",
    "create index idx4_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + REQUEST_MONTH_COLUMN + ")",
    "create index idx5_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + FULL_REQUESTS_COLUMN + ")",
    "create index idx6_" + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_BOOK_TYPE_AGGREGATES_TABLE
    + "(" + SECTION_REQUESTS_COLUMN + ")",
    "create index idx1_" + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + "(" + PUBLICATION_SEQ_COLUMN + ")",
    "create index idx2_" + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + "(" + IS_PUBLISHER_INVOLVED_COLUMN + ")",
    "create index idx3_" + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + "(" + REQUEST_YEAR_COLUMN + ")",
    "create index idx4_" + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + " on " + COUNTER_JOURNAL_TYPE_AGGREGATES_TABLE
    + "(" + REQUEST_MONTH_COLUMN + ")",
    "create index idx1_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + PUBLICATION_SEQ_COLUMN + ")",
    "create index idx2_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + IS_PUBLISHER_INVOLVED_COLUMN + ")",
    "create index idx3_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + REQUEST_YEAR_COLUMN + ")",
    "create index idx4_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + REQUEST_MONTH_COLUMN + ")",
    "create index idx5_" + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + " on " + COUNTER_JOURNAL_PUBYEAR_AGGREGATE_TABLE
    + "(" + PUBLICATION_YEAR_COLUMN + ")"
  };

  // The SQL code used to add the necessary version 11 database table columns.
  private static final String[] VERSION_11_COLUMN_ADD_QUERIES = new String[] {
    "alter table " + PENDING_AU_TABLE
    + " add column " + FULLY_REINDEX_COLUMN
    +   " boolean not null default false"
  };

  // SQL statement that obtains all existing plugin identifiers in the database.
  private static final String GET_ALL_PLUGIN_IDS_QUERY = "select distinct "
      + PLUGIN_ID_COLUMN
      + " from " + PLUGIN_TABLE;

  // Query to update the bulk content indication of a plugin.
  private static final String UPDATE_PLUGIN_IS_BULK_CONTENT_QUERY = "update "
      + PLUGIN_TABLE
      + " set " + IS_BULK_CONTENT_COLUMN + " = ?"
      + " where " + PLUGIN_ID_COLUMN + " = ?";

  // SQL statement that obtains all existing plugin identifier/AU key pairs in
  // the database for Archival Units with unknown creation times.
  private static final String GET_NO_CTIME_PLUGIN_IDS_AU_KEYS_QUERY = "select "
      + "p." + PLUGIN_ID_COLUMN
      + ", a." + AU_KEY_COLUMN
      + ", am." + AU_MD_SEQ_COLUMN
      + " from " + PLUGIN_TABLE + " p"
      + "," + AU_TABLE + " a"
      + "," + AU_MD_TABLE + " am"
      + " where p." + PLUGIN_SEQ_COLUMN + " = a." + PLUGIN_SEQ_COLUMN
      + " and a." + AU_SEQ_COLUMN + " = am." + AU_SEQ_COLUMN
      + " and am." + CREATION_TIME_COLUMN + " = -1";

  // Query to update the creation time of an Archival Unit.
  private static final String UPDATE_AU_CREATION_TIME_QUERY = "update "
      + AU_MD_TABLE
      + " set " + CREATION_TIME_COLUMN + " = ?"
      + " where " + AU_MD_SEQ_COLUMN + " = ?";
  
  // SQL statement that obtains for a given AU the identifiers of metadata items
  // that have no known fetch time in the database.
  private static final String GET_AU_MD_ITEMS_QUERY = "select "
      + "m." + MD_ITEM_SEQ_COLUMN
      + " from " + MD_ITEM_TABLE + " m"
      + " where m." + AU_MD_SEQ_COLUMN + " = ?"
      + " and m." + FETCH_TIME_COLUMN + " = -1";

  // Query to find the featured URLs of a metadata item.
  private static final String FIND_MD_ITEM_FEATURED_URL_QUERY = "select "
      + FEATURE_COLUMN + "," + URL_COLUMN
      + " from " + URL_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to update the fetch time of a metadata item.
  private static final String UPDATE_MD_ITEM_FETCH_TIME_QUERY = "update "
      + MD_ITEM_TABLE
      + " set " + FETCH_TIME_COLUMN + " = ?"
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to rename the range column in Derby.
  private static final String VERSION_12_RENAME_RANGE_COLUMN_DERBY_QUERY =
      "rename column " + SUBSCRIPTION_RANGE_TABLE + "." + OBSOLETE_RANGE_COLUMN
      + " to " + SUBSCRIPTION_RANGE_COLUMN;

  // Query to rename the range column in PostgreSQL.
  private static final String VERSION_12_RENAME_RANGE_COLUMN_PG_QUERY =
      "alter table " + SUBSCRIPTION_RANGE_TABLE
      + " rename " + OBSOLETE_RANGE_COLUMN + " to " + SUBSCRIPTION_RANGE_COLUMN;

  // SQL statement that finds a database in MySQL.
  private static final String FIND_DATABASE_QUERY_MYSQL = "select schema_name"
      + " from schemata where schema_name = ?";
  
  // SQL statement that creates a database in MySQL.
  private static final String CREATE_DATABASE_QUERY_MYSQL = "create database "
      + "--databaseName-- character set utf8 collate utf8_general_ci";

  // Query to zero out a value in the table of last run items.
  private static final String ZERO_LAST_RUN_VALUE_QUERY = "update "
      + LAST_RUN_TABLE
      + " set " + LAST_VALUE_COLUMN + " = '0'"
      + " where " + LABEL_COLUMN + " = ?";

  // The SQL code used to add the necessary version 14 database table columns.
  private static final String[] VERSION_14_COLUMN_ADD_QUERIES = new String[] {
    "alter table " + AU_TABLE
    + " add column " + ACTIVE_COLUMN + " boolean not null default true"
  };

  // Query to update the active flag of an Archival Unit.
  private static final String UPDATE_AU_ACTIVE_QUERY = "update "
      + AU_TABLE
      + " set " + ACTIVE_COLUMN + " = ?"
      + " where " + AU_SEQ_COLUMN
      + " = (select a." + AU_SEQ_COLUMN
      + " from " + AU_TABLE + " a," + PLUGIN_TABLE + " p"
      + " where a." + AU_KEY_COLUMN + " = ?"
      + " and a." + PLUGIN_SEQ_COLUMN + " = p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?)";

  // Query to update the active flag of an Archival Unit in MySQL.
  private static final String UPDATE_AU_ACTIVE_QUERY_MYSQL = "update "
      + AU_TABLE
      + " set " + ACTIVE_COLUMN + " = ?"
      + " where " + AU_SEQ_COLUMN
      + " = (select " + AU_TABLE + "." + AU_SEQ_COLUMN
      + " from " + PLUGIN_TABLE + " p"
      + " where " + AU_TABLE + "." + AU_KEY_COLUMN + " = ?"
      + " and " + AU_TABLE + "." + PLUGIN_SEQ_COLUMN
      + " = p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?)";

  // SQL statement that obtains all the trimmable publisher names in the
  // database.
  private static final String GET_TRIMMABLE_PUBLISHER_NAMES_QUERY = "select "
      + "distinct " + PUBLISHER_NAME_COLUMN
      + " from " + PUBLISHER_TABLE
      + " where " + PUBLISHER_NAME_COLUMN + " like ' %'"
      + " or " + PUBLISHER_NAME_COLUMN + " like '% '";

  // Query to update the name of a publisher.
  private static final String UPDATE_PUBLISHER_NAME_QUERY = "update "
      + PUBLISHER_TABLE
      + " set " + PUBLISHER_NAME_COLUMN + " = ?"
      + " where " + PUBLISHER_NAME_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable metadata item names in the
  // database.
  private static final String GET_TRIMMABLE_MD_ITEM_NAMES_QUERY = "select "
      + "distinct " + NAME_COLUMN
      + " from " + MD_ITEM_NAME_TABLE
      + " where " + NAME_COLUMN + " like ' %'"
      + " or " + NAME_COLUMN + " like '% '";

  // Query to update the name of a metadata item.
  private static final String UPDATE_MD_ITEM_NAME_QUERY = "update "
      + MD_ITEM_NAME_TABLE
      + " set " + NAME_COLUMN + " = ?"
      + " where " + NAME_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable ISBNs in the database.
  private static final String GET_TRIMMABLE_ISBNS_QUERY = "select distinct "
      + ISBN_COLUMN
      + " from " + ISBN_TABLE
      + " where " + ISBN_COLUMN + " like ' %'"
      + " or " + ISBN_COLUMN + " like '% '";

  // Query to update an ISBN.
  private static final String UPDATE_ISBN_QUERY = "update "
      + ISBN_TABLE
      + " set " + ISBN_COLUMN + " = ?"
      + " where " + ISBN_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable ISSNs in the database.
  private static final String GET_TRIMMABLE_ISSNS_QUERY = "select distinct "
      + ISSN_COLUMN
      + " from " + ISSN_TABLE
      + " where " + ISSN_COLUMN + " like ' %'"
      + " or " + ISSN_COLUMN + " like '% '";

  // Query to update an ISSN.
  private static final String UPDATE_ISSN_QUERY = "update "
      + ISSN_TABLE
      + " set " + ISSN_COLUMN + " = ?"
      + " where " + ISSN_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable bibliographic item volumes in
  // the database.
  private static final String GET_TRIMMABLE_VOLUMES_QUERY = "select distinct "
      + VOLUME_COLUMN
      + " from " + BIB_ITEM_TABLE
      + " where " + VOLUME_COLUMN + " like ' %'"
      + " or " + VOLUME_COLUMN + " like '% '";

  // Query to update a bibliographic item volume.
  private static final String UPDATE_VOLUME_QUERY = "update "
      + BIB_ITEM_TABLE
      + " set " + VOLUME_COLUMN + " = ?"
      + " where " + VOLUME_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable bibliographic item issues in
  // the database.
  private static final String GET_TRIMMABLE_ISSUES_QUERY = "select distinct "
      + ISSUE_COLUMN
      + " from " + BIB_ITEM_TABLE
      + " where " + ISSUE_COLUMN + " like ' %'"
      + " or " + ISSUE_COLUMN + " like '% '";

  // Query to update a bibliographic item issue.
  private static final String UPDATE_ISSUE_QUERY = "update "
      + BIB_ITEM_TABLE
      + " set " + ISSUE_COLUMN + " = ?"
      + " where " + ISSUE_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable bibliographic item start pages
  // in the database.
  private static final String GET_TRIMMABLE_START_PAGES_QUERY = "select "
      + "distinct " + START_PAGE_COLUMN
      + " from " + BIB_ITEM_TABLE
      + " where " + START_PAGE_COLUMN + " like ' %'"
      + " or " + START_PAGE_COLUMN + " like '% '";

  // Query to update a bibliographic item start page.
  private static final String UPDATE_START_PAGE_QUERY = "update "
      + BIB_ITEM_TABLE
      + " set " + START_PAGE_COLUMN + " = ?"
      + " where " + START_PAGE_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable bibliographic item end pages
  // in the database.
  private static final String GET_TRIMMABLE_END_PAGES_QUERY = "select distinct "
      + END_PAGE_COLUMN
      + " from " + BIB_ITEM_TABLE
      + " where " + END_PAGE_COLUMN + " like ' %'"
      + " or " + END_PAGE_COLUMN + " like '% '";

  // Query to update a bibliographic item end page.
  private static final String UPDATE_END_PAGE_QUERY = "update "
      + BIB_ITEM_TABLE
      + " set " + END_PAGE_COLUMN + " = ?"
      + " where " + END_PAGE_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable bibliographic item numbers in
  // the database.
  private static final String GET_TRIMMABLE_ITEM_NOS_QUERY = "select distinct "
      + ITEM_NO_COLUMN
      + " from " + BIB_ITEM_TABLE
      + " where " + ITEM_NO_COLUMN + " like ' %'"
      + " or " + ITEM_NO_COLUMN + " like '% '";

  // Query to update a bibliographic item number.
  private static final String UPDATE_ITEM_NO_QUERY = "update "
      + BIB_ITEM_TABLE
      + " set " + ITEM_NO_COLUMN + " = ?"
      + " where " + ITEM_NO_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable metadata item dates in the
  // database.
  private static final String GET_TRIMMABLE_MD_ITEM_DATES_QUERY = "select "
      + "distinct " + DATE_COLUMN
      + " from " + MD_ITEM_TABLE
      + " where " + DATE_COLUMN + " like ' %'"
      + " or " + DATE_COLUMN + " like '% '";

  // Query to update a metadata item date.
  private static final String UPDATE_MD_ITEM_DATE_QUERY = "update "
      + MD_ITEM_TABLE
      + " set " + DATE_COLUMN + " = ?"
      + " where " + DATE_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable metadata item coverages in the
  // database.
  private static final String GET_TRIMMABLE_MD_ITEM_COVERAGES_QUERY = "select "
      + "distinct " + COVERAGE_COLUMN
      + " from " + MD_ITEM_TABLE
      + " where " + COVERAGE_COLUMN + " like ' %'"
      + " or " + COVERAGE_COLUMN + " like '% '";

  // Query to update a metadata item coverage.
  private static final String UPDATE_MD_ITEM_COVERAGE_QUERY = "update "
      + MD_ITEM_TABLE
      + " set " + COVERAGE_COLUMN + " = ?"
      + " where " + COVERAGE_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable author names in the database.
  private static final String GET_TRIMMABLE_AUTHOR_NAMES_QUERY = "select "
      + "distinct " + AUTHOR_NAME_COLUMN
      + " from " + AUTHOR_TABLE
      + " where " + AUTHOR_NAME_COLUMN + " like ' %'"
      + " or " + AUTHOR_NAME_COLUMN + " like '% '";

  // Query to update an author name.
  private static final String UPDATE_AUTHOR_NAME_QUERY = "update "
      + AUTHOR_TABLE
      + " set " + AUTHOR_NAME_COLUMN + " = ?"
      + " where " + AUTHOR_NAME_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable DOIs in the database.
  private static final String GET_TRIMMABLE_DOIS_QUERY = "select distinct "
      + DOI_COLUMN
      + " from " + DOI_TABLE
      + " where " + DOI_COLUMN + " like ' %'"
      + " or " + DOI_COLUMN + " like '% '";

  // Query to update a DOI.
  private static final String UPDATE_DOI_QUERY = "update "
      + DOI_TABLE
      + " set " + DOI_COLUMN + " = ?"
      + " where " + DOI_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable URLs in the database.
  private static final String GET_TRIMMABLE_URLS_QUERY = "select distinct "
      + URL_COLUMN
      + " from " + URL_TABLE
      + " where " + URL_COLUMN + " like ' %'"
      + " or " + URL_COLUMN + " like '% '";

  // Query to update a URL.
  private static final String UPDATE_URL_QUERY = "update "
      + URL_TABLE
      + " set " + URL_COLUMN + " = ?"
      + " where " + URL_COLUMN + " = ?";

  // SQL statement that obtains all the trimmable keywords in the database.
  private static final String GET_TRIMMABLE_KEYWORDS_QUERY = "select distinct "
      + KEYWORD_COLUMN
      + " from " + KEYWORD_TABLE
      + " where " + KEYWORD_COLUMN + " like ' %'"
      + " or " + KEYWORD_COLUMN + " like '% '";

  // Query to update a keyword.
  private static final String UPDATE_KEYWORD_QUERY = "update "
      + KEYWORD_TABLE
      + " set " + KEYWORD_COLUMN + " = ?"
      + " where " + KEYWORD_COLUMN + " = ?";

  // SQL statements that create the necessary version 18 indices.
  private static final String[] VERSION_18_INDEX_CREATE_QUERIES = new String[] {
    "create unique index idx1_" + VERSION_TABLE + " on " + VERSION_TABLE
    + "(" + SYSTEM_COLUMN + "," + VERSION_COLUMN + ")"
  };

  // The SQL code used to create the necessary version 19 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_19_TABLE_CREATE_QUERIES =
    new LinkedHashMap<String, String>() {{
      put(PROVIDER_TABLE, CREATE_PROVIDER_TABLE_QUERY);
    }};

  // SQL statements that create the necessary version 19 indices.
  private static final String[] VERSION_19_INDEX_CREATE_QUERIES = new String[] {
    "create index idx1_" + PROVIDER_TABLE + " on " + PROVIDER_TABLE
      + "(" + PROVIDER_LID_COLUMN + ")",

    "create unique index idx2_" + PROVIDER_TABLE + " on " + PROVIDER_TABLE
      + "(" + PROVIDER_NAME_COLUMN + ")"
    };

  // SQL statements that create the necessary MySQL version 19 indices.
  private static final String[] VERSION_19_INDEX_CREATE_MYSQL_QUERIES =
    new String[] {
    "create index idx1_" + PROVIDER_TABLE + " on " + PROVIDER_TABLE
      + "(" + PROVIDER_LID_COLUMN + ")",

    // TODO: Make the index unique when MySQL is fixed.
    "create unique index idx2_" + PROVIDER_TABLE + " on " + PROVIDER_TABLE
      + "(" + PROVIDER_NAME_COLUMN + "(255))"
    };

  // The SQL code used to add the necessary version 19 database table columns.
  private static final String[] VERSION_19_COLUMN_ADD_QUERIES = new String[] {
    "alter table " + AU_MD_TABLE
      + " add column " + PROVIDER_SEQ_COLUMN
      + " bigint references " + PROVIDER_TABLE + " (" + PROVIDER_SEQ_COLUMN
      + ") on delete cascade",
    "alter table " + SUBSCRIPTION_TABLE
      + " add column " + PROVIDER_SEQ_COLUMN
      + " bigint references " + PROVIDER_TABLE + " (" + PROVIDER_SEQ_COLUMN
      + ") on delete cascade"
    };

  // SQL statement that obtains all existing publishers in the database that are
  // involved in subscriptions.
  private static final String GET_ALL_SUBSCRIPTION_PUBLISHERS_QUERY = "select "
      + "s." + SUBSCRIPTION_SEQ_COLUMN
      + ", s." + PUBLICATION_SEQ_COLUMN
      + ", pr." + PUBLISHER_NAME_COLUMN
      + " from " + SUBSCRIPTION_TABLE + " s"
      + ", " + PUBLICATION_TABLE + " pn"
      + ", " + PUBLISHER_TABLE + " pr"
      + " where s." + PUBLICATION_SEQ_COLUMN + " = pn." + PUBLICATION_SEQ_COLUMN
      + " and pn." + PUBLISHER_SEQ_COLUMN + " = pr." + PUBLISHER_SEQ_COLUMN
      + " order by s." + PUBLICATION_SEQ_COLUMN;

  // Query to find a provider by its name.
  private static final String FIND_PROVIDER_BY_NAME_QUERY = "select "
      + PROVIDER_SEQ_COLUMN
      + " from " + PROVIDER_TABLE
      + " where " + PROVIDER_NAME_COLUMN + " = ?";

  // Query to find a provider by its LOCKSS identifier.
  private static final String FIND_PROVIDER_BY_LID_QUERY = "select "
      + PROVIDER_SEQ_COLUMN
      + " from " + PROVIDER_TABLE
      + " where " + PROVIDER_LID_COLUMN + " = ?";

  // Query to add a provider.
  private static final String INSERT_PROVIDER_QUERY = "insert into "
      + PROVIDER_TABLE
      + "(" + PROVIDER_SEQ_COLUMN
      + "," + PROVIDER_LID_COLUMN
      + "," + PROVIDER_NAME_COLUMN
      + ") values (default,?,?)";

  // Query to update the LOCKSS identifier of a provider.
  private static final String UPDATE_PROVIDER_LID_QUERY = "update "
      + PROVIDER_TABLE
      + " set " + PROVIDER_LID_COLUMN + " = ?"
      + " where " + PROVIDER_SEQ_COLUMN + " = ?"
      + " and " + PROVIDER_LID_COLUMN + " is null";

  // Query to update the provider of an Archival Unit.
  private static final String UPDATE_AU_MD_PROVIDER_QUERY = "update "
      + AU_MD_TABLE
      + " set " + PROVIDER_SEQ_COLUMN + " = ?"
      + " where " + AU_MD_SEQ_COLUMN + " = ?";

  // Query to update the provider of a subscription.
  private static final String UPDATE_SUBSCRIPTION_PROVIDER_QUERY = "update "
      + SUBSCRIPTION_TABLE
      + " set " + PROVIDER_SEQ_COLUMN + " = ?"
      + " where " + SUBSCRIPTION_SEQ_COLUMN + " = ?";

  // SQL statement that obtains all existing plugin identifier/AU key pairs in
  // the database for Archival Units that have no provider.
  private static final String GET_NO_PROVIDER_PLUGIN_IDS_AU_KEYS_QUERY =
    "select am." + AU_MD_SEQ_COLUMN
      + ", a." + AU_KEY_COLUMN
      + ", p." + PLUGIN_ID_COLUMN
      + " from " + AU_MD_TABLE + " am"
      + ", " + AU_TABLE + " a"
      + ", " + PLUGIN_TABLE + " p"
      + " where p." + PLUGIN_SEQ_COLUMN + " = a." + PLUGIN_SEQ_COLUMN
      + " and a." + AU_SEQ_COLUMN + " = am." + AU_SEQ_COLUMN
      + " and am." + PROVIDER_SEQ_COLUMN + " is null";

  // Query to determine whether a specific database version has been completed.
  private static final String COUNT_DATABASE_VERSION_QUERY = "select count(*) "
      + " from " + VERSION_TABLE
      + " where " + SYSTEM_COLUMN + " = '" + DATABASE_VERSION_TABLE_SYSTEM
      + "' and " + VERSION_COLUMN + " = ?";

  // The SQL code used to create the necessary version 21 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_21_TABLE_CREATE_QUERIES =
    new LinkedHashMap<String, String>() {{
      put(PROPRIETARY_ID_TABLE, CREATE_PROPRIETARY_ID_TABLE_QUERY);
    }};

  // SQL statements that create the necessary version 21 indices.
  private static final String[] VERSION_21_INDEX_CREATE_QUERIES = new String[] {
    "create unique index idx1_" + PROPRIETARY_ID_TABLE + " on "
  + PROPRIETARY_ID_TABLE
  + "(" + MD_ITEM_SEQ_COLUMN + "," + PROPRIETARY_ID_COLUMN + ")"
    };

  // SQL statement that obtains all existing metadata item identifiers and their
  // proprietary identifiers in the publication table.
  private static final String GET_OLD_PUBLICATION_IDS_QUERY = "select "
      + MD_ITEM_SEQ_COLUMN
      + "," + OBSOLETE_PUBLICATION_ID_COLUMN
      + " from " + PUBLICATION_TABLE
      + " where " + OBSOLETE_PUBLICATION_ID_COLUMN + " is not null";

  // Query to insert a proprietary identifier of a metadata item.
  private static final String INSERT_PROPRIETARY_ID_QUERY = "insert into "
      + PROPRIETARY_ID_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + PROPRIETARY_ID_COLUMN
      + ") values (?,?)";

  // SQL statement that obtains all existing plugin identifier/AU key pairs in
  // the database for Archival Units that have an unknown provider.
  private static final String GET_UNKNOWN_PROVIDER_PLUGIN_IDS_AU_KEYS_QUERY =
    "select am." + AU_MD_SEQ_COLUMN
      + ", a." + AU_KEY_COLUMN
      + ", p." + PLUGIN_ID_COLUMN
      + " from " + AU_MD_TABLE + " am"
      + ", " + AU_TABLE + " a"
      + ", " + PLUGIN_TABLE + " p"
      + ", " + PROVIDER_TABLE + " pr"
      + " where p." + PLUGIN_SEQ_COLUMN + " = a." + PLUGIN_SEQ_COLUMN
      + " and a." + AU_SEQ_COLUMN + " = am." + AU_SEQ_COLUMN
      + " and am." + PROVIDER_SEQ_COLUMN + " = pr." + PROVIDER_SEQ_COLUMN
      + " and pr." + PROVIDER_NAME_COLUMN + " = '" + UNKNOWN_PROVIDER_NAME
      + "'";

  // SQL statements that create the necessary version 23 indices.
  private static final String[] VERSION_23_INDEX_CREATE_QUERIES = new String[] {
    "create index idx4_" + AU_MD_TABLE + " on " + AU_MD_TABLE
    + "(" + PROVIDER_SEQ_COLUMN + ")"
    };

  // The SQL code used to create the necessary version 24 database tables.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_24_TABLE_CREATE_QUERIES =
    new LinkedHashMap<String, String>() {{
      put(PUBLISHER_SUBSCRIPTION_TABLE,
	  CREATE_PUBLISHER_SUBSCRIPTION_TABLE_QUERY);
    }};

  // The SQL code used to create the necessary version 24 database tables for
  // MySQL.
  @SuppressWarnings("serial")
  private static final Map<String, String> VERSION_24_TABLE_CREATE_MYSQL_QUERIES
    = new LinkedHashMap<String, String>() {{
      put(PUBLISHER_SUBSCRIPTION_TABLE,
	  CREATE_PUBLISHER_SUBSCRIPTION_TABLE_MYSQL_QUERY);
    }};

  // Query to find a publisher by its name.
  private static final String FIND_PUBLISHER_QUERY = "select "
      + PUBLISHER_SEQ_COLUMN
      + " from " + PUBLISHER_TABLE
      + " where " + PUBLISHER_NAME_COLUMN + " = ?";

  // Query to add a publisher.
  private static final String INSERT_PUBLISHER_QUERY = "insert into "
      + PUBLISHER_TABLE
      + "(" + PUBLISHER_SEQ_COLUMN
      + "," + PUBLISHER_NAME_COLUMN
      + ") values (default,?)";

  // Query to find ISBNs that contain lower case characters.
  private static final String FIND_LOWER_CASE_ISBNS_QUERY = "select "
      + MD_ITEM_SEQ_COLUMN
      + "," + ISBN_COLUMN
      + "," + ISBN_TYPE_COLUMN
      + " from " + ISBN_TABLE
      + " where " + ISBN_COLUMN + " != upper(" + ISBN_COLUMN + ")";

  // Query to find a metadata item typed ISBN.
  private static final String FIND_MD_ITEM_TYPED_ISBN_QUERY = "select "
      + ISBN_COLUMN
      + " from " + ISBN_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + ISBN_COLUMN + " = ?"
      + " and " + ISBN_TYPE_COLUMN + " = ?";
  
  // Query to update an ISBN for a metadata item and type.
  private static final String UPDATE_ISBN_FOR_MD_ITEM_AND_TYPE_QUERY = "update "
      + ISBN_TABLE
      + " set " + ISBN_COLUMN + " = ?"
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + ISBN_COLUMN + " = ?"
      + " and " + ISBN_TYPE_COLUMN + " = ?";

  // Query to find ISSNs that contain lower case characters.
  private static final String FIND_LOWER_CASE_ISSNS_QUERY = "select "
      + MD_ITEM_SEQ_COLUMN
      + "," + ISSN_COLUMN
      + "," + ISSN_TYPE_COLUMN
      + " from " + ISSN_TABLE
      + " where " + ISSN_COLUMN + " != upper(" + ISSN_COLUMN + ")";

  // Query to find a metadata item typed ISSN.
  private static final String FIND_MD_ITEM_TYPED_ISSN_QUERY = "select "
      + ISSN_COLUMN
      + " from " + ISSN_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + ISSN_COLUMN + " = ?"
      + " and " + ISSN_TYPE_COLUMN + " = ?";
  
  // Query to update an ISBN for a metadata item and type.
  private static final String UPDATE_ISSN_FOR_MD_ITEM_AND_TYPE_QUERY = "update "
      + ISSN_TABLE
      + " set " + ISSN_COLUMN + " = ?"
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + ISSN_COLUMN + " = ?"
      + " and " + ISSN_TYPE_COLUMN + " = ?";

  // Query to find DOIs that contain upper case characters.
  private static final String FIND_UPPER_CASE_DOIS_QUERY = "select "
      + MD_ITEM_SEQ_COLUMN
      + "," + DOI_COLUMN
      + " from " + DOI_TABLE
      + " where " + DOI_COLUMN + " != lower(" + DOI_COLUMN + ")";

  // Query to find a metadata item DOI.
  private static final String FIND_MD_ITEM_DOI_QUERY = "select "
      + DOI_COLUMN
      + " from " + DOI_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + DOI_COLUMN + " = ?";

  // SQL statement that removes a row for a given DOI.
  private static final String REMOVE_DOI = "delete from " + DOI_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + DOI_COLUMN + " = ?";

  // Query to update a DOI for a metadata item.
  private static final String UPDATE_DOI_FOR_MD_ITEM_QUERY = "update "
      + DOI_TABLE
      + " set " + DOI_COLUMN + " = ?"
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + DOI_COLUMN + " = ?";

  // SQL statements that create the necessary version 26 indices.
  private static final String[] VERSION_26_INDEX_CREATE_QUERIES = new String[] {
    "create unique index idx1_" + PUBLISHER_SUBSCRIPTION_TABLE + " on "
	+ PUBLISHER_SUBSCRIPTION_TABLE + "(" + PUBLISHER_SEQ_COLUMN + ")"
    };

  // The database data source.
  private DataSource dataSource = null;

  // The data source class name.
  private String dataSourceClassName = null;

  // The data source user.
  private String dataSourceUser = null;

  // The maximum number of retries to be attempted when encountering transient
  // SQL exceptions.
  private int maxRetryCount = -1;

  // The interval to wait between consecutive retries when encountering
  // transient SQL exceptions.
  private long retryDelay = -1;

  // The SQL statement fetch size.
  private int fetchSize = -1;

  /**
   * Constructor.
   * 
   * @param dataSource
   *          A DataSource with the datasource that provides the connection.
   * @param dataSourceClassName
   *          A String with the data source class name.
   * @param dataSourceUser
   *          A String with the data source user name.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @param fetchSize
   *          An int with the SQL statement fetch size.
   */
  DbManagerSql(DataSource dataSource, String dataSourceClassName,
      String dataSourceUser, int maxRetryCount, long retryDelay, int fetchSize)
      {
    this.dataSource = dataSource;
    this.dataSourceClassName = dataSourceClassName;
    this.dataSourceUser = dataSourceUser;
    this.maxRetryCount = maxRetryCount;
    this.retryDelay = retryDelay;
    this.fetchSize = fetchSize;
  }

  DataSource getDataSource() {
    return dataSource;
  }

  void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  String getDataSourceClassName() {
    return dataSourceClassName;
  }

  void setDataSourceClassName(String dataSourceClassName) {
    this.dataSourceClassName = dataSourceClassName;
  }

  String getDataSourceUser() {
    return dataSourceUser;
  }

  void setDataSourceUser(String dataSourceUser) {
    this.dataSourceUser = dataSourceUser;
  }

  long getRetryDelay() {
    return retryDelay;
  }

  void setRetryDelay(long retryDelay) {
    this.retryDelay = retryDelay;
  }

  int getMaxRetryCount() {
    return maxRetryCount;
  }

  void setMaxRetryCount(int maxRetryCount) {
    this.maxRetryCount = maxRetryCount;
  }

  int getFetchSize() {
    return fetchSize;
  }

  void setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
  }

  /**
   * Closes a result set without throwing exceptions.
   * 
   * @param resultSet
   *          A ResultSet with the database result set to be closed.
   */
  static void safeCloseResultSet(ResultSet resultSet) {
    JdbcBridge.safeCloseResultSet(resultSet);
  }

  /**
   * Closes a statement without throwing exceptions.
   * 
   * @param statement
   *          A Statement with the database statement to be closed.
   */
  static void safeCloseStatement(Statement statement) {
    JdbcBridge.safeCloseStatement(statement);
  }

  /**
   * Closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be closed.
   */
  static void safeCloseConnection(Connection conn) {
    JdbcBridge.safeCloseConnection(conn);
  }

  /**
   * Rolls back and closes a connection without throwing exceptions.
   * 
   * @param conn
   *          A Connection with the database connection to be rolled back and
   *          closed.
   */
  static void safeRollbackAndClose(Connection conn) {
    JdbcBridge.safeRollbackAndClose(conn);
  }

  /**
   * Commits a connection or rolls it back if it's not possible.
   * 
   * @param conn
   *          A connection with the database connection to be committed.
   * @param logger
   *          A Logger used to report errors.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static void commitOrRollback(Connection conn, Logger logger)
      throws SQLException {
    JdbcBridge.commitOrRollback(conn, logger);
  }

  /**
   * Rolls back a transaction.
   * 
   * @param conn
   *          A connection with the database connection to be rolled back.
   * @param logger
   *          A Logger used to report errors.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static void rollback(Connection conn, Logger logger) throws SQLException {
    JdbcBridge.rollback(conn, logger);
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
  static String truncateVarchar(String original, int maxLength) {
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
  static boolean isTruncatedVarchar(String text) {
    return text.endsWith(TRUNCATION_INDICATOR);
  }

  /**
   * Provides a description of the tables available in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param catalog
   *          a catalog name; must match the catalog name as it is stored in the
   *          database; "" retrieves those without a catalog; <code>null</code>
   *          means that the catalog name should not be used to narrow the
   *          search.
   * @param schemaPattern
   *          a schema name pattern; must match the schema name as it is stored
   *          in the database; "" retrieves those without a schema;
   *          <code>null</code> means that the schema name should not be used to
   *          narrow the search.
   * @param tableNamePattern
   *          a table name pattern; must match the table name as it is stored in
   *          the database.
   * @return a ResultSet in which each row is a table description.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static ResultSet getStandardTables(Connection conn, String catalog,
      String schemaPattern, String tableNamePattern) throws SQLException {
    return JdbcBridge.getStandardTables(conn, catalog, schemaPattern,
	tableNamePattern);
  }

  /**
   * Provides the database metadata.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @return a DatabaseMetaData with the database metadata.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  static DatabaseMetaData getMetadata(Connection conn) throws SQLException {
    return JdbcBridge.getMetadata(conn);
  }

  /**
   * Provides an indication of whether the Derby database is being used.
   * 
   * @return <code>true</code> if the Derby database is being used,
   *         <code>false</code> otherwise.
   */
  boolean isTypeDerby() {
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
  boolean isTypePostgresql() {
    final String DEBUG_HEADER = "isTypePostgresql(): ";

    boolean result = "org.postgresql.ds.PGSimpleDataSource"
	.equals(dataSourceClassName)
	|| "org.postgresql.ds.PGPoolingDataSource".equals(dataSourceClassName);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides an indication of whether the MySQL database is being used.
   * 
   * @return <code>true</code> if the MySQL database is being used,
   *         <code>false</code> otherwise.
   */
  boolean isTypeMysql() {
    final String DEBUG_HEADER = "isTypeMysql(): ";

    boolean result = "com.mysql.cj.jdbc.MysqlDataSource"
	.equals(dataSourceClassName);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides a database connection using the default datasource, retrying the
   * operation in the default manner in case of transient failures.
   * <p>
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred getting the connection.
   */
  Connection getConnection() throws SQLException {
    return getConnection(dataSource, maxRetryCount, retryDelay, false, true);
  }

  /**
   * Provides a database connection using the default datasource, retrying the
   * operation in the default manner in case of transient failures and with the
   * specified autocommit property.
   * 
   * @param autoCommit
   *          A boolean indicating the value of the connection auto-commit
   *          property.
   * @param logException
   *          A boolean indicating whether any exception received should be
   *          logged.
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred getting the connection.
   */
  Connection getConnection(boolean autoCommit, boolean logException)
      throws SQLException {
    return getConnection(dataSource, maxRetryCount, retryDelay, autoCommit,
	logException);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation in the default manner in case of transient failures.
   * <p>
   * Auto-commit is disabled to allow the client code to manage transactions.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred getting the connection.
   */
  Connection getConnection(DataSource ds) throws SQLException {
    return getConnection(ds, maxRetryCount, retryDelay, false, true);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation in the default manner in case of transient failures.
   * <p>
   * Auto-commit is disabled to allow the client code to manage transactions.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param logException
   *          A boolean indicating whether any exception received should be
   *          logged.
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred getting the connection.
   */
  Connection getConnection(DataSource ds, boolean logException)
      throws SQLException {
    return getConnection(ds, maxRetryCount, retryDelay, false, logException);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation in the default manner in case of transient failures.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param autoCommit
   *          A boolean indicating the value of the connection auto-commit
   *          property.
   * @param logException
   *          A boolean indicating whether any exception received should be
   *          logged.
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred getting the connection.
   */
  Connection getConnection(DataSource ds, boolean autoCommit,
      boolean logException) throws SQLException {
    return getConnection(ds, maxRetryCount, retryDelay, autoCommit,
	logException);
  }

  /**
   * Provides a database connection using a passed datasource, retrying the
   * operation with the specified parameters in case of transient failures.
   * <p>
   * Autocommit is disabled to allow the client code to manage transactions.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param maxRetryCount
   *          An int with the maximum number of retries to be attempted.
   * @param retryDelay
   *          A long with the number of milliseconds to wait between consecutive
   *          retries.
   * @param autoCommit
   *          A boolean indicating the value of the connection auto-commit
   *          property.
   * @param logException
   *          A boolean indicating whether any exception received should be
   *          logged.
   * @return a Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Connection getConnection(DataSource ds, int maxRetryCount, long retryDelay,
      boolean autoCommit, boolean logException) throws SQLException {
    if (ds == null) {
      throw new IllegalArgumentException("Null datasource");
    }

    try {
      return
	  JdbcBridge.getConnection(ds, maxRetryCount, retryDelay, autoCommit);
    } catch (SQLException sqle) {
      // Check whether the client code wants the exception logged.
      if (logException) {
	// Yes: Report the problem.
	log.error("Cannot get a database connection", sqle);
	log.error("maxRetryCount = " + maxRetryCount);
	log.error("retryDelay = " + retryDelay);
	log.error("autoCommit = " + autoCommit);
      }

      throw sqle;
    } catch (RuntimeException re) {
      // Check whether the client code wants the exception logged.
      if (logException) {
	// Yes: Report the problem.
	log.error("Cannot get a database connection", re);
	log.error("maxRetryCount = " + maxRetryCount);
	log.error("retryDelay = " + retryDelay);
	log.error("autoCommit = " + autoCommit);
      }

      throw re;
    }
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  boolean tableExists(Connection conn, String tableName) throws SQLException {
    final String DEBUG_HEADER = "tableExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    boolean result = false;
    ResultSet resultSet = null;

    try {
      // Get the database schema table data.
      if (isTypeDerby()) {
	resultSet = JdbcBridge.getStandardTables(conn, null, dataSourceUser,
	    tableName.toUpperCase());
      } else if (isTypePostgresql()) {
	resultSet = JdbcBridge.getStandardTables(conn, null, dataSourceUser,
	    tableName.toLowerCase());
      } else if (isTypeMysql()) {
	resultSet = JdbcBridge.getStandardTables(conn, null, dataSourceUser,
	    tableName);
      }

      // Determine whether the table exists.
      result = resultSet.next();
    } catch (SQLException sqle) {
	log.error("Cannot determine whether table '" + tableName + "' exists",
	    sqle);
	throw sqle;
    } catch (RuntimeException re) {
	log.error("Cannot determine whether table '" + tableName + "' exists",
	    re);
	throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
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
   * @throws SQLException
   *           if any problem occurred preparing the statement.
   */
  PreparedStatement prepareStatement(Connection conn, String sql)
      throws SQLException {
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  PreparedStatement prepareStatement(Connection conn, String sql,
      int maxRetryCount, long retryDelay) throws SQLException {
    final String DEBUG_HEADER = "prepareStatement(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "sql = '" + sql + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      // Prepare the statement.
      statement = JdbcBridge.prepareStatement(conn, sql,
	  Statement.NO_GENERATED_KEYS, maxRetryCount, retryDelay, fetchSize);
    } catch (SQLException sqle) {
      log.error("Cannot prepare a statement", sqle);
      log.error("sql = '" + sql + "'");
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot prepare a statement", re);
      log.error("sql = '" + sql + "'");
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw re;
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
   * @throws SQLException
   *           if any problem occurred preparing the statement.
   */
  PreparedStatement prepareStatement(Connection conn, String sql,
      int returnGeneratedKeys) throws SQLException {
    return prepareStatement(conn, sql, returnGeneratedKeys,
	maxRetryCount, retryDelay);
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  PreparedStatement prepareStatement(Connection conn, String sql,
      int returnGeneratedKeys, int maxRetryCount, long retryDelay)
      throws SQLException {
    final String DEBUG_HEADER = "prepareStatement(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "sql = '" + sql + "'");
      log.debug2(DEBUG_HEADER + "returnGeneratedKeys = " + returnGeneratedKeys);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      // Prepare the statement.
      statement = JdbcBridge.prepareStatement(conn, sql, returnGeneratedKeys,
	  maxRetryCount, retryDelay, fetchSize);
    } catch (SQLException sqle) {
      log.error("Cannot prepare a statement", sqle);
      log.error("sql = '" + sql + "'");
      log.error("returnGeneratedKeys = " + returnGeneratedKeys);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot prepare a statement", re);
      log.error("sql = '" + sql + "'");
      log.error("returnGeneratedKeys = " + returnGeneratedKeys);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return statement;
  }

  /**
   * Executes a querying prepared statement, retrying the execution in the
   * default manner in case of transient failures.
   * 
   * @param statement
   *          A PreparedStatement with the querying prepared statement to be
   *          executed.
   * @return a ResultSet with the results of the query.
   * @throws SQLException
   *           if any problem occurred executing the query.
   */
  ResultSet executeQuery(PreparedStatement statement) throws SQLException {
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  ResultSet executeQuery(PreparedStatement statement, int maxRetryCount,
      long retryDelay) throws SQLException {
    final String DEBUG_HEADER = "executeQuery(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    ResultSet results = null;

    try {
      // Execute the query.
      results = JdbcBridge.executeQuery(statement, maxRetryCount, retryDelay);
    } catch (SQLException sqle) {
      log.error("Cannot execute a query", sqle);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot execute a query");
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw re;
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
   * @throws SQLException
   *           if any problem occurred executing the query.
   */
  int executeUpdate(PreparedStatement statement) throws SQLException {
    return executeUpdate(statement, maxRetryCount, retryDelay);
  }

  /**
   * Executes an updating prepared statement, retrying the execution  with the
   * specified parameters in case of transient failures.
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  int executeUpdate(PreparedStatement statement, int maxRetryCount,
      long retryDelay) throws SQLException {
    final String DEBUG_HEADER = "executeUpdate(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    int updatedCount = 0;

    try {
      // Execute the query.
      updatedCount =
	  JdbcBridge.executeUpdate(statement, maxRetryCount, retryDelay);
    } catch (SQLException sqle) {
      log.error("Cannot execute an update", sqle);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot execute an update", re);
      log.error("maxRetryCount = " + maxRetryCount);
      log.error("retryDelay = " + retryDelay);
      throw re;
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "updatedCount = " + updatedCount);
    return updatedCount;
  }

  /**
   * Provides the highest-numbered database update version from the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return an int with the database update version.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  int getHighestNumberedDatabaseVersion(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "getHighestNumberedDatabaseVersion(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int version = 1;
    PreparedStatement stmt = null;
    ResultSet resultSet = null;

    try {
      stmt = prepareStatement(conn, GET_SORTED_DATABASE_VERSIONS_QUERY);
      resultSet = executeQuery(stmt);

      while (resultSet.next()) {
	version = resultSet.getShort(VERSION_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "version = " + version);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the database version", sqle);
      log.error("SQL = '" + GET_SORTED_DATABASE_VERSIONS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get the database version", re);
      log.error("SQL = '" + GET_SORTED_DATABASE_VERSIONS_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(stmt);
    }

    log.debug2(DEBUG_HEADER + "version = " + version);
    return version;
  }

  /**
   * Adds to the database the database version.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param version
   *          An int with version to be updated.
   * @return an int with the number of database rows added.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  int addDbVersion(Connection conn, int version) throws SQLException {
    final String DEBUG_HEADER = "addDbVersion(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "version = " + version);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int addedCount = 0;
    PreparedStatement insertVersion = null;

    try {
      insertVersion = prepareStatement(conn, INSERT_VERSION_QUERY);
      insertVersion.setString(1, DATABASE_VERSION_TABLE_SYSTEM);
      insertVersion.setShort(2, (short)version);

      addedCount = executeUpdate(insertVersion);
    } catch (SQLException sqle) {
      log.error("Cannot add the database version", sqle);
      log.error("SQL = '" + INSERT_VERSION_QUERY + "'.");
      log.error("version = " + version + ".");
      log.error("DATABASE_VERSION_TABLE_SYSTEM = '"
	  + DATABASE_VERSION_TABLE_SYSTEM + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot add the database version", re);
      log.error("SQL = '" + INSERT_VERSION_QUERY + "'.");
      log.error("version = " + version + ".");
      log.error("DATABASE_VERSION_TABLE_SYSTEM = '"
	  + DATABASE_VERSION_TABLE_SYSTEM + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(insertVersion);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "addedCount = " + addedCount);
    return addedCount;
  }

  /**
   * Executes a batch of statements.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param statements
   *          A String[] with the statements to be executed.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void executeBatch(Connection conn, String[] statements)
      throws SQLException {
    final String DEBUG_HEADER = "executeBatch(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "statements = "
	+ StringUtil.toString(statements));

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    try {
      // Execute the batch.
      JdbcBridge.executeBatch(conn, statements);
    } catch (SQLException sqle) {
      log.error("Cannot execute batch", sqle);
      log.error("statements = " + StringUtil.toString(statements));
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot execute batch", re);
      log.error("statements = " + StringUtil.toString(statements));
      throw re;
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
   *           if any problem occurred creating the table.
   */
  // TODO: If the table exists, verify that it matches the table to be created.
  boolean createTableIfMissing(Connection conn, String tableName,
      String tableCreateSql) throws SQLException {
    final String DEBUG_HEADER = "createTableIfMissing(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableCreateSql = '" + tableCreateSql + "'.");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Check whether the table needs to be created.
    if (!tableExists(conn, tableName)) {
      // Yes: Create it.
      createTable(conn, tableName, tableCreateSql);
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
   * @throws SQLException
   *           if any problem occurred creating the table.
   */
  void createTable(Connection conn, String tableName, String tableCreateSql)
      throws SQLException {
    final String DEBUG_HEADER = "createTable(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableCreateSql = '" + tableCreateSql + "'.");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    executeDdlQuery(conn, localizeCreateQuery(tableCreateSql));

    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "Table '" + tableName + "' created.");

    logTableSchema(conn, tableName);
  }

  /**
   * Executes a DDL query.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param ddlQuery
   *          A String with the DDL query to be executed.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void executeDdlQuery(Connection conn, String ddlQuery)
      throws SQLException {
    final String DEBUG_HEADER = "executeDdlQuery(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "ddlQuery = '" + ddlQuery + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    try {
      JdbcBridge.executeDdlQuery(conn, ddlQuery, maxRetryCount, retryDelay,
	  fetchSize);
    } catch (SQLException sqle) {
      log.error("Cannot execute a DDL query", sqle);
      log.error("ddlQuery = '" + ddlQuery + "'");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot execute a DDL query", re);
      log.error("ddlQuery = '" + ddlQuery + "'");
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
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
  String localizeCreateQuery(String query) {
    final String DEBUG_HEADER = "localizeCreateQuery(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "query = '" + query + "'");

    String result = query;

    // Check whether the Derby database is being used.
    if (isTypeDerby()) {
      // Yes.
      result = localizeCreateQueryForDerby(query);
      // No: Check whether the PostgreSQL database is being used.
    } else if (isTypePostgresql()) {
      result = localizeCreateQueryForPostgresql(query);
    } else if (isTypeMysql()) {
      result = localizeCreateQueryForMysql(query);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides a version of a table creation query localized for Derby.
   * 
   * @param query
   *          A String with the generic table creation query.
   * 
   * @return a String with the localized table creation query.
   */
  private String localizeCreateQueryForDerby(String query) {
    return StringUtil.replaceString(query, "--BigintSerialPk--",
	BIGINT_SERIAL_PK_DERBY);
  }

  /**
   * Provides a version of a table creation query localized for PostgreSQL.
   * 
   * @param query
   *          A String with the generic table creation query.
   * 
   * @return a String with the localized table creation query.
   */
  private String localizeCreateQueryForPostgresql(String query) {
    return StringUtil.replaceString(query, "--BigintSerialPk--",
	BIGINT_SERIAL_PK_PG);
  }

  /**
   * Provides a version of a table creation query localized for MySQL.
   * 
   * @param query
   *          A String with the generic table creation query.
   * 
   * @return a String with the localized table creation query.
   */
  private String localizeCreateQueryForMysql(String query) {
    return StringUtil.replaceString(query + " ENGINE = INNODB",
	"--BigintSerialPk--", BIGINT_SERIAL_PK_MYSQL);
  }

  /**
   * Writes the named table schema to the log. To be used for debugging purposes
   * only.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param tableName
   *          A String with name of the table to log.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  void logTableSchema(Connection conn, String tableName) throws SQLException {
    final String DEBUG_HEADER = "logTableSchema(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'");

    // Do nothing more if the current log level is not appropriate.
    if (!log.isDebug()) {
      return;
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Report a non-existent table.
    if (!tableExists(conn, tableName)) {
      log.debug("Table '" + tableName + "' does not exist.");
      return;
    }

    String columnName = null;
    String padding = "                               ";
    ResultSet resultSet = null;

    try {
      // Get the table column data.
      if (isTypeDerby()) {
	resultSet = JdbcBridge.getColumns(conn, null, dataSourceUser,
	    tableName.toUpperCase(), null);
      } else if (isTypePostgresql()) {
	resultSet = JdbcBridge.getColumns(conn, null, dataSourceUser,
	    tableName.toLowerCase(), null);
      } else if (isTypeMysql()) {
	resultSet = JdbcBridge.getColumns(conn, null, dataSourceUser,
	    tableName, null);
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
      log.error("Cannot log table schema", sqle);
      log.error("tableName = '" + tableName + "'");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot log table schema", re);
      log.error("tableName = '" + tableName + "'");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a PostgreSQL database if it does not exist.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param databaseName
   *          A String with the name of the database to create, if missing.
   * @return <code>true</code> if the database did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if the database creation process failed.
   */
  boolean createPostgreSqlDbIfMissing(DataSource ds, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "createPostgreSqlDbIfMissing(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    boolean result = false;

    // Connect to the template database. PostgreSQL does not allow a database
    // to be created within a transaction.
    Connection conn = getConnection(ds, true, true);

    // Check whether the database does not exist.
    if (!postgresqlDbExists(conn, databaseName)) {
      // Yes: Create it.
      createPostgresqlDb(conn, databaseName);

      result = true;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private boolean postgresqlDbExists(Connection conn, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "postgresqlDbExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    boolean result = false;
    PreparedStatement findDb = null;
    ResultSet resultSet = null;

    try {
      findDb = prepareStatement(conn, FIND_DATABASE_QUERY_PG);
      findDb.setString(1, databaseName);

      resultSet = executeQuery(findDb);
      result = resultSet.next();
    } catch (SQLException sqle) {
      log.error("Cannot find database", sqle);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_PG + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find database", re);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_PG + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(findDb);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a PostgreSQL database.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database to create.
   * @throws SQLException
   *           if the database creation process failed.
   */
  private void createPostgresqlDb(Connection conn, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "createPostgresqlDb(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    String sql = StringUtil.replaceString(CREATE_DATABASE_QUERY_PG,
					  "--databaseName--", databaseName);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = '" + sql + "'");

    executeDdlQuery(conn, sql);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Turns on user authentication and authorization on a Derby database.
   * 
   * @param user
   *          A String with the user name.
   * @return <code>true</code> if the the database needs to be shut down to make
   *         static properties take effect, <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  boolean setUpDerbyAuthentication(String user) throws SQLException {
    final String DEBUG_HEADER = "setUpDerbyAuthentication(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "user = '" + user + "'");

    // Start with an unmodified database.
    boolean requiresCommit = false;

    Statement statement = null;

    // Get a connection to the database.
    Connection conn = getConnection();

    try {
      // Create a statement for authentication queries.
      statement = JdbcBridge.createStatement(conn);

      // Get the indication of whether the database requires authentication.
      String requiresAuthentication = getDerbyDatabaseProperty(statement,
	  "derby.connection.requireAuthentication", "false");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "requiresAuthentication = '"
	  + requiresAuthentication + "'");

      // Check whether it does not require authentication already.
      if ("false".equals(requiresAuthentication.trim().toLowerCase())) {
	// Yes: Change it to require authentication.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.connection.requireAuthentication', 'true')");

	// Get the indication of whether the database requires authentication.
	requiresAuthentication = getDerbyDatabaseProperty(statement,
	    "derby.connection.requireAuthentication", "false");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "requiresAuthentication = '" + requiresAuthentication + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database authentication provider.
      String authenticationProvider = getDerbyDatabaseProperty(statement,
	  "derby.authentication.provider", "BUILTIN");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "authenticationProvider = '"
	  + authenticationProvider + "'");

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
	authenticationProvider = getDerbyDatabaseProperty(statement,
	    "derby.authentication.provider", "BUILTIN");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "authenticationProvider = '" + authenticationProvider + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database default connection mode.
      String defaultConnectionMode = getDerbyDatabaseProperty(statement,
	  "derby.database.defaultConnectionMode", "fullAccess");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "defaultConnectionMode = '"
	  + defaultConnectionMode + "'");

      // Check whether the database does not prevent unauthenticated access
      // already.
      if (!"noAccess".equals(defaultConnectionMode.trim())) {
	// Yes: Change it to prevent unauthenticated access.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.defaultConnectionMode', 'noAccess')");

	// Get the database default connection mode.
	defaultConnectionMode = getDerbyDatabaseProperty(statement,
	    "derby.database.defaultConnectionMode", "fullAccess");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "defaultConnectionMode = '" + defaultConnectionMode + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the names of the database full access users.
      String fullAccessUsers = getDerbyDatabaseProperty(statement,
	      "derby.database.fullAccessUsers", "");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullAccessUsers = '"
	  + fullAccessUsers + "'");

      // Check whether the user is not in the list of full access users already.
      if (fullAccessUsers.indexOf(user) < 0) {
	// Yes: Change it to allow the configured user full access.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.fullAccessUsers', '" + user + "')");

	// Get the names of the database full access users.
	fullAccessUsers = getDerbyDatabaseProperty(statement,
	    "derby.database.fullAccessUsers", "");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "fullAccessUsers = '"
	    + fullAccessUsers + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database read-only access users.
      String readOnlyAccessUsers = getDerbyDatabaseProperty(statement,
	  "derby.database.readOnlyAccessUsers", "");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "readOnlyAccessUsers = '"
	  + readOnlyAccessUsers + "'");

      // Check whether changes to the database properties have been made.
      if (requiresCommit) {
	// Yes: Allow override using system properties.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.propertiesOnly', 'false')");
      }
    } catch (SQLException sqle) {
      // Prevent partial changes to be committed.
      requiresCommit = false;

      log.error("Cannot set database property", sqle);
      log.error("user = '" + user + "'");
      throw sqle;
    } catch (RuntimeException re) {
      // Prevent partial changes to be committed.
      requiresCommit = false;

      log.error("Cannot set database property", re);
      log.error("user = '" + user + "'");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);

      // Check whether we need to commit the changes made to the database.
      if (requiresCommit) {
	JdbcBridge.commitOrRollback(conn, log);
      }

      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return requiresCommit;
  }

  /**
   * Provides a Derby database property.
   * 
   * @param statement
   *          A Statement to query the database.
   * @param propertyName
   *          A String with the name of the requested property.
   * @param defaultValue
   *          A String with the default value of the requested property.
   * @return a String with the value of the database property.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private String getDerbyDatabaseProperty(Statement statement,
      String propertyName, String defaultValue) throws SQLException {
    final String DEBUG_HEADER = "getDerbyDatabaseProperty(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "propertyName = '" + propertyName + "'");
      log.debug2(DEBUG_HEADER + "defaultValue = '" + defaultValue + "'");
    }

    if (statement == null) {
      throw new IllegalArgumentException("Null statement.");
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
	log.debug3(DEBUG_HEADER + "propertyValue = '" + propertyValue + "'");
    } catch (SQLException sqle) {
      log.error("Cannot get database property with name '" + propertyName
	  + "'.", sqle);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get database property with name '" + propertyName
	  + "'.", re);
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(rs);
    }

    // Return the default, if necessary.
    if (propertyValue == null) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Using defaultValue = '"
	  + defaultValue + "'");
      propertyValue = defaultValue;
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "propertyValue = '" + propertyValue + "'");
    return propertyValue;
  }

  /**
   * Turns off user authentication and authorization on a Derby database.
   * 
   * @return <code>true</code> if the the database needs to be shut down to make
   *         static properties take effect, <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  boolean removeDerbyAuthentication() throws SQLException {
    final String DEBUG_HEADER = "removeDerbyAuthentication(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    // Start with an unmodified database.
    boolean requiresCommit = false;

    Statement statement = null;

    // Get a connection to the database.
    Connection conn = getConnection();

    try {
      // Create a statement for authentication queries.
      statement = JdbcBridge.createStatement(conn);

      // Get the indication of whether the database requires authentication.
      String requiresAuthentication = getDerbyDatabaseProperty(statement,
	  "derby.connection.requireAuthentication", "false");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "requiresAuthentication = '"
	  + requiresAuthentication + "'");

      // Check whether it does require authentication.
      if ("true".equals(requiresAuthentication.trim().toLowerCase())) {
	// Yes: Change it to not require authentication.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.connection.requireAuthentication', 'false')");

	// Get the indication of whether the database requires authentication.
	requiresAuthentication = getDerbyDatabaseProperty(statement,
	    "derby.connection.requireAuthentication", "false");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "requiresAuthentication = '" + requiresAuthentication + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database authentication provider.
      String authenticationProvider = getDerbyDatabaseProperty(statement,
	  "derby.authentication.provider", "BUILTIN");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "authenticationProvider = '"
	  + authenticationProvider + "'");

      // Check whether it does not use the built-in Derby authentication
      // provider already.
      if (!"BUILTIN".equals(authenticationProvider.trim().toUpperCase())) {
	// Yes: Change it to use the built-in Derby authentication provider.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.authentication.provider', 'BUILTIN')");

	// Get the database authentication provider.
	authenticationProvider = getDerbyDatabaseProperty(statement,
	    "derby.authentication.provider", "BUILTIN");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "authenticationProvider = '" + authenticationProvider + "'");

	// Remember that the database has been modified.
	requiresCommit = true;
      }

      // Get the database default connection mode.
      String defaultConnectionMode = getDerbyDatabaseProperty(statement,
	  "derby.database.defaultConnectionMode", "fullAccess");
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "defaultConnectionMode = '"
	  + defaultConnectionMode + "'");

      // Check whether it does not allow full unauthenticated access already.
      if (!"fullAccess".equals(defaultConnectionMode.trim())) {
	// Yes: Change it to allow full unauthenticated access.
	statement.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY("
	    + "'derby.database.defaultConnectionMode', 'fullAccess')");

	// Get the default connection mode.
	defaultConnectionMode = getDerbyDatabaseProperty(statement,
	    "derby.database.defaultConnectionMode", "fullAccess");
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "defaultConnectionMode = '" + defaultConnectionMode + "'");

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

      log.error("Cannot set database property", sqle);
      throw sqle;
    } catch (RuntimeException re) {
      // Prevent partial changes to be committed.
      requiresCommit = false;

      log.error("Cannot set database property", re);
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);

      // Check whether we need to commit the changes made to the database.
      if (requiresCommit) {
	JdbcBridge.commitOrRollback(conn, log);
      }

      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
    return requiresCommit;
  }

  /**
   * Creates a PostgreSQL database schema if it does not exist.
   * 
   * @param schemaName
   *          A String with the name of the schema to create, if missing.
   * @param ds
   *          A DataSource with the data source to be used to get a connection.
   * @return <code>true</code> if the schema did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if the schema creation process failed.
   */
  boolean createPostgresqlSchemaIfMissing(String schemaName,
      DataSource ds) throws SQLException {
    final String DEBUG_HEADER = "createPostgresqlSchemaIfMissing(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "schemaName = '" + schemaName + "'");

    boolean result = false;

    // Get a database connection.
    Connection conn = getConnection(ds);

    try {
      // Check whether the schema does not exist.
      if (!postgresqlSchemaExists(conn, schemaName)) {
	// Yes: Create it.
	createPostgresqlSchema(conn, schemaName);

	// Finish the transaction.
	JdbcBridge.commitOrRollback(conn, log);
	result = true;
      }
    } finally {
      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private boolean postgresqlSchemaExists(Connection conn, String schemaName)
      throws SQLException {
    final String DEBUG_HEADER = "postgresqlSchemaExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "schemaName = '" + schemaName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    boolean result = false;
    PreparedStatement findSchema = null;
    ResultSet resultSet = null;

    try {
      findSchema = prepareStatement(conn, FIND_SCHEMA_QUERY_PG);
      findSchema.setString(1, schemaName);

      resultSet = executeQuery(findSchema);
      result = resultSet.next();
    } catch (SQLException sqle) {
      log.error("Cannot find schema", sqle);
      log.error("schemaName = '" + schemaName + "'.");
      log.error("SQL = '" + FIND_SCHEMA_QUERY_PG + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find schema", re);
      log.error("schemaName = '" + schemaName + "'.");
      log.error("SQL = '" + FIND_SCHEMA_QUERY_PG + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(findSchema);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a PostgreSQL schema.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param schemaName
   *          A String with the name of the schema to create.
   * @throws SQLException
   *           if the schema creation process failed.
   */
  private void createPostgresqlSchema(Connection conn, String schemaName)
      throws SQLException {
    final String DEBUG_HEADER = "createPostgresqlSchema(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "schemaName = '" + schemaName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    String sql = StringUtil.replaceString(CREATE_SCHEMA_QUERY_PG,
					  "--schemaName--", schemaName);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = '" + sql + "'");

    executeDdlQuery(conn, sql);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates a MySQl database if it does not exist.
   * 
   * @param ds
   *          A DataSource with the datasource that provides the connection.
   * @param databaseName
   *          A String with the name of the database to create, if missing.
   * @return <code>true</code> if the database did not exist and it was created,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if the database creation process failed.
   */
  boolean createMySqlDbIfMissing(DataSource ds, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "createMySqlDbIfMissing(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    boolean result = false;

    // Connect to the standard connectable MySql database.
    Connection conn = getConnection(ds);

    // Check whether the database does not exist.
    if (!mysqlDbExists(conn, databaseName)) {
      // Yes: Create it.
      createMysqlDb(conn, databaseName);
      result = true;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Determines whether a named MySQL database exists.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database.
   * @return <code>true</code> if the database exists, <code>false</code>
   *         otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private boolean mysqlDbExists(Connection conn, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "mysqlDbExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    boolean result = false;
    PreparedStatement findDb = null;
    ResultSet resultSet = null;

    try {
      findDb = prepareStatement(conn, FIND_DATABASE_QUERY_MYSQL);
      findDb.setString(1, databaseName);

      resultSet = executeQuery(findDb);
      result = resultSet.next();
    } catch (SQLException sqle) {
      log.error("Cannot find database", sqle);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_MYSQL + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find database", re);
      log.error("databaseName = '" + databaseName + "'.");
      log.error("SQL = '" + FIND_DATABASE_QUERY_MYSQL + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(findDb);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Creates a MySQL database.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param databaseName
   *          A String with the name of the database to create.
   * @throws SQLException
   *           if the database creation process failed.
   */
  private void createMysqlDb(Connection conn, String databaseName)
      throws SQLException {
    final String DEBUG_HEADER = "createMysqlDb(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "databaseName = '" + databaseName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    String sql = StringUtil.replaceString(CREATE_DATABASE_QUERY_MYSQL,
	  "--databaseName--", databaseName);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "sql = '" + sql + "'");

    executeDdlQuery(conn, sql);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the query used to drop a table.
   * 
   * @param tableName
   *          A string with the name of the table to be dropped.
   * @return a String with the query used to drop the table.
   */
  static String dropTableQuery(String tableName) {
    return "drop table " + tableName;
  }

  /**
   * Sets up the database to version 1.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred setting up the database.
   */
  void setUpDatabaseVersion1(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "setUpDatabaseVersion1(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary tables if they do not exist.
    if (isTypeDerby() || isTypePostgresql()) {
      createTablesIfMissing(conn, VERSION_1_TABLE_CREATE_QUERIES);
    } else if (isTypeMysql()) {
      createTablesIfMissing(conn, VERSION_1_TABLE_CREATE_MYSQL_QUERIES);
    }

    // Create new functions.
    if (isTypeDerby()) {
      executeBatch(conn, VERSION_1_FUNCTION_CREATE_QUERIES);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates the necessary database tables if they do not exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableMap
   *          A {@code Map<String, String>} with the creation queries indexed by table
   *          name.
   * @throws SQLException
   *           if any problem occurred creating the tables.
   */
  private void createTablesIfMissing(Connection conn,
      Map<String, String> tableMap) throws SQLException {
    final String DEBUG_HEADER = "createTablesIfMissing(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "tableMap = " + tableMap);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Loop through all the table names.
    for (String tableName : tableMap.keySet()) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "tableName = '" + tableName + "'.");

      // Create the table if it does not exist.
      createTableIfMissing(conn, tableName,
	  localizeCreateQuery(tableMap.get(tableName)));
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Done with tableName '" + tableName + "'.");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 1 to version 2.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom1To2(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom1To2(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Remove obsolete database tables.
    removeTablesIfPresent(conn, VERSION_1_TABLE_DROP_QUERIES);

    // Create the necessary tables if they do not exist.
    if (isTypeDerby() || isTypePostgresql()) {
      createTablesIfMissing(conn, VERSION_2_TABLE_CREATE_QUERIES);
    } else if (isTypeMysql()) {
      createTablesIfMissing(conn, VERSION_2_TABLE_CREATE_MYSQL_QUERIES);
    }

    // Initialize necessary data in new tables.
    addMetadataItemType(conn, MD_ITEM_TYPE_BOOK_SERIES);
    addMetadataItemType(conn, MD_ITEM_TYPE_BOOK);
    addMetadataItemType(conn, MD_ITEM_TYPE_BOOK_CHAPTER);
    addMetadataItemType(conn, MD_ITEM_TYPE_JOURNAL);
    addMetadataItemType(conn, MD_ITEM_TYPE_JOURNAL_ARTICLE);

    if (isTypeDerby()) {
      // Remove old functions.
      removeFunctionsIfPresent(conn, VERSION_1_FUNCTION_DROP_QUERIES);

      // Create the functions.
      executeBatch(conn, VERSION_2_FUNCTION_CREATE_QUERIES);
    }

    if (log.isDebug2())  log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes database tables if they exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param tableMap
   *          A {@code Map<String, String>} with the removal queries indexed by table
   *          name.
   * @throws SQLException
   *           if any problem occurred removing the tables.
   */
  private void removeTablesIfPresent(Connection conn,
      Map<String, String> tableMap) throws SQLException {
    final String DEBUG_HEADER = "removeTablesIfPresent(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "tableMap = " + tableMap);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

      // Loop through all the table names.
    for (String tableName : tableMap.keySet()) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "tableName = '" + tableName + "'.");

      // Remove the table if it does exist.
      removeTableIfPresent(conn, tableName, tableMap.get(tableName));
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Done with tableName '" + tableName + "'.");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
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
   * @throws SQLException
   *           if any problem occurred removing the table.
   */
  private boolean removeTableIfPresent(Connection conn, String tableName,
      String tableDropSql) throws SQLException {
    final String DEBUG_HEADER = "removeTableIfPresent(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "tableName = '" + tableName + "'.");
      log.debug2(DEBUG_HEADER + "tableDropSql = '" + tableDropSql + "'.");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Check whether the table needs to be removed.
    if (tableExists(conn, tableName)) {
      // Yes: Delete it.
      executeBatch(conn, new String[] { tableDropSql });
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
   * Adds a metadata item type to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param typeName
   *          A String with the name of the type to be added.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void addMetadataItemType(Connection conn, String typeName)
      throws SQLException {
    final String DEBUG_HEADER = "addMetadataItemType(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "typeName = '" + typeName + "'.");

    // Ignore an empty metadata item type.
    if (StringUtil.isNullString(typeName)) {
      return;
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement insertMetadataItemType = null;

    try {
      insertMetadataItemType = prepareStatement(conn,
	  INSERT_MD_ITEM_TYPE_QUERY);
      insertMetadataItemType.setString(1, typeName);

      int count = executeUpdate(insertMetadataItemType);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot add a metadata item type", sqle);
      log.error("typeName = '" + typeName + "'.");
      log.error("SQL = '" + INSERT_MD_ITEM_TYPE_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot add a metadata item type", re);
      log.error("typeName = '" + typeName + "'.");
      log.error("SQL = '" + INSERT_MD_ITEM_TYPE_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(insertMetadataItemType);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes database functions if they exist.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param functionMap
   *          A {@code Map<String, String>} with the removal queries indexed by function
   *          name.
   * @throws SQLException
   *           if any problem occurred removing the functions.
   */
  private void removeFunctionsIfPresent(Connection conn,
      Map<String, String> functionMap) throws SQLException {
    final String DEBUG_HEADER = "removeFunctionsIfPresent(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "functionMap = " + functionMap);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Loop through all the function names.
    for (String functionName : functionMap.keySet()) {
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "functionName = '" + functionName + "'");

      // Remove the function if it does exist.
      removeFunctionIfPresent(conn, functionName,
	  functionMap.get(functionName));
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Done with functionName '"
	  + functionName + "'");
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Deletes a database function if it does exist.
   * 
   * @param conn
   *          A connection with the database connection to be used.
   * @param functionName
   *          A String with the name of the function to delete, if present.
   * @param functionDropSql
   *          A String with the SQL code used to drop the function, if present.
   * @return <code>true</code> if the function did exist and it was removed,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred removing the functions.
   */
  private boolean removeFunctionIfPresent(Connection conn, String functionName,
      String functionDropSql) throws SQLException {
    final String DEBUG_HEADER = "removeFunctionIfPresent(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "functionName = '" + functionName + "'.");
      log.debug2(DEBUG_HEADER + "functionDropSql = '" + functionDropSql + "'.");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Check whether the function needs to be removed.
    if (functionExists(conn, functionName)) {
      // Yes: Delete it.
      executeBatch(conn, new String[] { functionDropSql });
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
   * Provides an indication of whether a function exists.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param functionName
   *          A String with name of the function to be checked.
   * @return <code>true</code> if the named function exists, <code>false</code>
   *         otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private boolean functionExists(Connection conn, String functionName)
      throws SQLException {
    final String DEBUG_HEADER = "functionExists(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "functionName = '" + functionName + "'.");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    boolean result = false;
    ResultSet resultSet = null;

    try {
      // Get the database schema function data.
      resultSet = JdbcBridge.getFunctions(conn, null, dataSourceUser, null);

      // Loop through each function found.
      while (resultSet.next()) {
	// Check whether this is the function sought.
	if (functionName.toUpperCase().equals(resultSet
	                                      .getString(FUNCTION_NAME))) {
	  // Found the function: No need to check further.
	  result = true;
	  break;
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot determine whether a function exists", sqle);
      log.error("functionName = '" + functionName + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot determine whether a function exists", re);
      log.error("functionName = '" + functionName + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
    }

    // The function does not exist.
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
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
   * Updates the database from version 2 to version 3.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom2To3(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom2To3(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary indices.
    if (isTypeDerby() || isTypePostgresql()) {
      executeDdlQueries(conn, VERSION_3_INDEX_CREATE_QUERIES);
    } else if (isTypeMysql()) {
      executeDdlQueries(conn, VERSION_3_INDEX_CREATE_MYSQL_QUERIES);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Executes DDL queries.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param queries
   *          A String[] with the database queries.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void executeDdlQueries(Connection conn, String[] queries)
      throws SQLException {
    final String DEBUG_HEADER = "executeDdlQueries(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "queries = " + StringUtil.toString(queries));

    if (conn == null) {
      throw new IllegalArgumentException("Null connection.");
    }

    // Loop through all the indices.
    for (String query : queries) {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Query = '" + query + "'");

      try {
	JdbcBridge.executeDdlQuery(conn, query, maxRetryCount, retryDelay,
	    fetchSize);
      } catch (SQLException sqle) {
	log.error("Cannot execute DDL query '" + query + "'", sqle);
	throw sqle;
      } catch (RuntimeException re) {
	log.error("Cannot execute DDL query '" + query + "'", re);
	throw re;
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 3 to version 4.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom3To4(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom3To4(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Check whether the MySQL database is being used.
    if (isTypeMysql()) {
      // Yes: Create the necessary tables if they do not exist.
      createTablesIfMissing(conn, VERSION_4_TABLE_CREATE_MYSQL_QUERIES);
    } else {
    // No: Create the necessary tables if they do not exist.
      createTablesIfMissing(conn, VERSION_4_TABLE_CREATE_QUERIES);
    }

    createVersion4Indices(conn);

    // Migrate the version 3 platforms.
    executeDdlQuery(conn, ADD_PLUGIN_PLATFORM_SEQ_COLUMN);
    populatePlatformTable(conn);
    executeDdlQuery(conn,
	dropColumnQuery(PLUGIN_TABLE, OBSOLETE_PLATFORM_COLUMN));

    // Fix publication dates.
    setPublicationDatesToNull(conn);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates the indices needed in version 4.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void createVersion4Indices(Connection conn) throws SQLException {
    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    if (isTypeMysql()) {
      // Yes: Create the necessary indices if they do not exist.
      executeDdlQueries(conn, VERSION_4_INDEX_CREATE_MYSQL_QUERIES);
    } else {
    // No: Create the necessary indices if they do not exist.
      executeDdlQueries(conn, VERSION_4_INDEX_CREATE_QUERIES);
    }
  }

  /**
   * Populates the platform table with the platforms existing in the plugin
   * table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void populatePlatformTable(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "populatePlatformTable(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Update the null platforms in the plugin table.
    updatePluginNullPlatform(conn);

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    String platform = null;
    Long platformSeq = null;

    try {
      // Get all the distinct platforms in the plugin table.
      statement = prepareStatement(conn, GET_VERSION_2_PLATFORMS);
      resultSet = executeQuery(statement);

      // Loop through all the distinct platforms in the plugin table.
      while (resultSet.next()) {
	// Get the platform.
  	platform = resultSet.getString(OBSOLETE_PLATFORM_COLUMN);
  	if (log.isDebug3())
  	  log.debug3(DEBUG_HEADER + "platform = '" + platform + "'");

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
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get the platforms", re);
      log.error("SQL = '" + GET_VERSION_2_PLATFORMS + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the null platform name in the plugin table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void updatePluginNullPlatform(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updatePluginNullPlatform(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      statement = prepareStatement(conn, UPDATE_PLUGIN_NULL_PLATFORM_QUERY);
      statement.setString(1, NO_PLATFORM);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      log.error("Cannot update the null platform name", sqle);
      log.error("SQL = '" + UPDATE_PLUGIN_NULL_PLATFORM_QUERY + "'.");
      log.error("platformName = '" + NO_PLATFORM + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot update the null platform name", re);
      log.error("SQL = '" + UPDATE_PLUGIN_NULL_PLATFORM_QUERY + "'.");
      log.error("platformName = '" + NO_PLATFORM + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds a platform to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformName
   *          A String with the platform name.
   * @return a Long with the identifier of the platform just added.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Long addPlatform(Connection conn, String platformName) throws SQLException {
    final String DEBUG_HEADER = "addPlatform(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "platformName = '" + platformName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    Long platformSeq = null;

    try {
      statement = prepareStatement(conn, INSERT_PLATFORM_QUERY,
	  Statement.RETURN_GENERATED_KEYS);
      // Skip auto-increment key field #0.
      statement.setString(1, platformName);
      executeUpdate(statement);
      resultSet = statement.getGeneratedKeys();

      if (!resultSet.next()) {
	String message = "Cannot create PLATFORM row for platformName '"
	    + platformName + "'";
	log.error(message);
	throw new RuntimeException(message);
      }

      platformSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added platformSeq = " + platformSeq);
    } catch (SQLException sqle) {
      log.error("Cannot add platform", sqle);
      log.error("platformName = '" + platformName + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot add platform", re);
      log.error("platformName = '" + platformName + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
    return platformSeq;
  }

  /**
   * Updates the platform reference in the plugin table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformSeq
   *          A Long with the identifier of the platform.
   * @param platformName
   *          A String with the platform name.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void updatePluginPlatformReference(Connection conn, Long platformSeq,
      String platformName) throws SQLException {
    final String DEBUG_HEADER = "updatePluginPlatformReference(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
      log.debug2(DEBUG_HEADER + "platformName = '" + platformName + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      statement =
	  prepareStatement(conn, UPDATE_PLUGIN_PLATFORM_SEQ_QUERY);
      statement.setLong(1, platformSeq);
      statement.setString(2, platformName);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      log.error("Cannot update plugin platform", sqle);
      log.error("platformSeq = " + platformSeq);
      log.error("platformName = '" + platformName + "'.");
      log.error("SQL = '" + UPDATE_PLUGIN_PLATFORM_SEQ_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot update plugin platform", re);
      log.error("platformSeq = " + platformSeq);
      log.error("platformName = '" + platformName + "'.");
      log.error("SQL = '" + UPDATE_PLUGIN_PLATFORM_SEQ_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the query used to drop a column.
   * 
   * @param tableName
   *          A String with the name of the table to which the column to be
   *          dropped belongs.
   * @param columnName
   *          A String with the name of the column to be dropped.
   * @return a String with the query used to drop the column.
   */
  private String dropColumnQuery(String tableName, String columnName) {
    if (isTypeMysql()) {
      return "alter table " + tableName + " drop column " + columnName;
    }

    return "alter table " + tableName
	+ " drop column " + columnName + " restrict";
  }

  /**
   * Nulls out the date column for publications, populated in earlier versions
   * by mistake.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void setPublicationDatesToNull(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "setPublicationDatesToNull(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      statement = prepareStatement(conn, SET_PUBLICATION_DATES_TO_NULL);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      log.error("Cannot set publication dates to null", sqle);
      log.error("SQL = '" + SET_PUBLICATION_DATES_TO_NULL + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot set publication dates to null", re);
      log.error("SQL = '" + SET_PUBLICATION_DATES_TO_NULL + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 4 to version 5.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom4To5(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom4To5(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary tables if they do not exist.
    createTablesIfMissing(conn, VERSION_5_TABLE_CREATE_QUERIES);

    // Create the necessary indices.
    createVersion5Indices(conn);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Creates the indices needed in version 5.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void createVersion5Indices(Connection conn) throws SQLException {
    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    if (isTypeDerby() || isTypePostgresql()) {
      executeDdlQueries(conn, VERSION_5_INDEX_CREATE_QUERIES);
    } else if (isTypeMysql()) {
      executeDdlQueries(conn, VERSION_5_INDEX_CREATE_MYSQL_QUERIES);
    }
  }

  /**
   * Updates the database from version 5 to version 6.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom5To6(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom5To6(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Remove duplicated rows in the ISBN table.
    removeDuplicateIsbns(conn);

    // Remove duplicated rows in the ISSN table.
    removeDuplicateIssns(conn);

    // Create the necessary indices.
    createVersion6Indices(conn);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes duplicated rows in the ISBN table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
  *           if any problem occurred accessing the database.
   */
  private void removeDuplicateIsbns(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "removeDuplicateIsbns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

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
	findStatement = prepareStatement(conn, FIND_ISBNS);
	resultSet = executeQuery(findStatement);

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
	      log.debug3(DEBUG_HEADER + "Duplicated isbn = '" + isbn + "'");
	      log.debug3(DEBUG_HEADER + "Duplicated isbnType = '" + isbnType
		  + "'");
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
	throw sqle;
      } catch (RuntimeException re) {
	log.error("Cannot find ISBNs", re);
	log.error("SQL = '" + FIND_ISBNS + "'.");
	throw re;
      } finally {
	JdbcBridge.safeCloseResultSet(resultSet);
	JdbcBridge.safeCloseStatement(findStatement);
      }

      // Check whether no duplicate ISBNs were found.
      if (!foundDuplicate) {
	// Yes: Done.
	done = true;
	continue;
      }

      // No: Delete all the duplicate rows found.
      try {
	deleteStatement = prepareStatement(conn, REMOVE_ISBN);

	deleteStatement.setLong(1, mdItemSeq);
	deleteStatement.setString(2, isbn);
	deleteStatement.setString(3, isbnType);

	count = executeUpdate(deleteStatement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
      } catch (SQLException sqle) {
	log.error("Cannot delete ISBN", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("isbn = '" + isbn + "'");
	log.error("isbnType = '" + isbnType + "'");
	log.error("SQL = '" + REMOVE_ISBN + "'.");
	throw sqle;
      } catch (RuntimeException re) {
	log.error("Cannot delete ISBN", re);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("isbn = '" + isbn + "'");
	log.error("isbnType = '" + isbnType + "'");
	log.error("SQL = '" + REMOVE_ISBN + "'.");
	throw re;
      } finally {
	JdbcBridge.safeCloseStatement(deleteStatement);
      }

      // Insert back one instance of the deleted rows.
      try {
	insertStatement = prepareStatement(conn, ADD_ISBN);

	insertStatement.setLong(1, mdItemSeq);
	insertStatement.setString(2, isbn);
	insertStatement.setString(3, isbnType);

	count = executeUpdate(insertStatement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	JdbcBridge.commitOrRollback(conn, log);
      } catch (SQLException sqle) {
	log.error("Cannot add ISBN", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("isbn = '" + isbn + "'");
	log.error("isbnType = '" + isbnType + "'");
	log.error("SQL = '" + ADD_ISBN + "'.");
	throw sqle;
      } catch (RuntimeException re) {
	log.error("Cannot add ISBN", re);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("isbn = '" + isbn + "'");
	log.error("isbnType = '" + isbnType + "'");
	log.error("SQL = '" + ADD_ISBN + "'.");
	throw re;
      } finally {
	JdbcBridge.safeCloseStatement(insertStatement);
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
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void removeDuplicateIssns(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "removeDuplicateIssns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

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
	findStatement = prepareStatement(conn, FIND_ISSNS);
	resultSet = executeQuery(findStatement);

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
	      log.debug3(DEBUG_HEADER + "Duplicated issn = '" + issn + "'");
	      log.debug3(DEBUG_HEADER + "Duplicated issnType = '" + issnType
		  + "'");
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
	throw sqle;
      } catch (RuntimeException re) {
	log.error("Cannot find ISSNs", re);
	log.error("SQL = '" + FIND_ISSNS + "'.");
	throw re;
      } finally {
	JdbcBridge.safeCloseResultSet(resultSet);
	JdbcBridge.safeCloseStatement(findStatement);
      }

      // Check whether no duplicate ISSNs were found.
      if (!foundDuplicate) {
	// Yes: Done.
	done = true;
	continue;
      }

      // No: Delete all the duplicate rows found.
      try {
	deleteStatement = prepareStatement(conn, REMOVE_ISSN);

	deleteStatement.setLong(1, mdItemSeq);
	deleteStatement.setString(2, issn);
	deleteStatement.setString(3, issnType);

	count = executeUpdate(deleteStatement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
      } catch (SQLException sqle) {
	log.error("Cannot delete ISSN", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("issn = '" + issn + "'");
	log.error("issnType = '" + issnType + "'");
	log.error("SQL = '" + REMOVE_ISSN + "'.");
	throw sqle;
      } catch (RuntimeException re) {
	log.error("Cannot delete ISSN", re);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("issn = '" + issn + "'");
	log.error("issnType = '" + issnType + "'");
	log.error("SQL = '" + REMOVE_ISSN + "'.");
	throw re;
      } finally {
	JdbcBridge.safeCloseStatement(deleteStatement);
      }

      // Insert back one instance of the deleted rows.
      try {
	insertStatement = prepareStatement(conn, ADD_ISSN);

	insertStatement.setLong(1, mdItemSeq);
	insertStatement.setString(2, issn);
	insertStatement.setString(3, issnType);

	count = executeUpdate(insertStatement);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	JdbcBridge.commitOrRollback(conn, log);
      } catch (SQLException sqle) {
	log.error("Cannot add ISSN", sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("issn = '" + issn + "'");
	log.error("issnType = '" + issnType + "'");
	log.error("SQL = '" + ADD_ISSN + "'.");
	throw sqle;
      } catch (RuntimeException re) {
	log.error("Cannot add ISSN", re);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("issn = '" + issn + "'");
	log.error("issnType = '" + issnType + "'");
	log.error("SQL = '" + ADD_ISSN + "'.");
	throw re;
      } finally {
	JdbcBridge.safeCloseStatement(insertStatement);
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
   * Creates the indices needed in version 6.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void createVersion6Indices(Connection conn) throws SQLException {
    executeDdlQueries(conn, VERSION_6_INDEX_CREATE_QUERIES);
  }

  /**
   * Updates the database from version 6 to version 7.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom6To7(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom6To7(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary indices.
    executeDdlQueries(conn, VERSION_7_INDEX_CREATE_QUERIES);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 7 to version 8.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom7To8(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom7To8(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Add the subscription range index column.
    executeDdlQuery(conn, ADD_SUBSCRIPTION_RANGE_INDEX_COLUMN_QUERY);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 8 to version 9.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom8To9(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom8To9(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary tables if they do not exist.
    createTablesIfMissing(conn, VERSION_9_TABLE_CREATE_QUERIES);

    // Add the new columns.
    executeDdlQueries(conn, VERSION_9_COLUMN_ADD_QUERIES);

    // Create the necessary indices.
    if (isTypeMysql()) {
      executeDdlQueries(conn, VERSION_9_INDEX_CREATE_MYSQL_QUERIES);
    } else {
      executeDdlQueries(conn, VERSION_9_INDEX_CREATE_QUERIES);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 9 to version 10.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom9To10(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom9To10(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Populate in a separate thread the ArchivalUnit creation times and the
    // book chapter/journal article fetch times.
    DbVersion9To10Migrator migrator = new DbVersion9To10Migrator();
    Thread thread = new Thread(migrator, "DbVersion9To10Migrator");
    LockssDaemon.getLockssDaemon().getDbManager().recordThread(thread);
    new Thread(migrator).start();

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Migrates the contents of the database from version 9 to version 10.
   */
  void migrateDatabaseFrom9To10() {
    final String DEBUG_HEADER = "migrateDatabaseFrom9To10(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = getConnection();

      // Populate the indication of bulk content in the plugin table.
      populatePluginIsBulkContentColumn(conn);

      // Populate the archival unit and metadata item timestamps.
      populateArchivalUnitTimestamps(conn);
    } catch (SQLException sqle) {
      log.error("Cannot migrate the database from version 9 to 10", sqle);
    } catch (RuntimeException re) {
      log.error("Cannot migrate the database from version 9 to 10", re);
    } finally {
      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Populates the indication of bulk content in the plugin table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void populatePluginIsBulkContentColumn(Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "populatePluginIsBulkContentColumn(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      // Get all the plugin identifiers in the plugin table.
      statement = prepareStatement(conn, GET_ALL_PLUGIN_IDS_QUERY);
      resultSet = executeQuery(statement);

      // Loop through all the plugin identifiers.
      while (resultSet.next()) {
	// Get the plugin identifier.
	String pluginId = resultSet.getString(PLUGIN_ID_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "pluginId = '" + pluginId + "'");

	// Get the plugin indication of bulk content.
	boolean isBulkContent = LockssDaemon.getLockssDaemon()
	    .getPluginManager().getPlugin(pluginId).isBulkContent();
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "isBulkContent = " + isBulkContent);

  	// Update all the plugins using this platform.
  	updatePluginIsBulkContentColumn(conn, pluginId, isBulkContent);
      }
    } catch (SQLException sqle) {
      log.error("Cannot populate plugin bulk content indication", sqle);
      log.error("SQL = '" + GET_ALL_PLUGIN_IDS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot populate plugin bulk content indication", re);
      log.error("SQL = '" + GET_ALL_PLUGIN_IDS_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the the indication of bulk content for a plugin.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginId
   *          A String with the identifier of the plugin.
   * @param isBulkContent
   *          A boolean with the indication of bulk content for the plugin.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void updatePluginIsBulkContentColumn(Connection conn, String pluginId,
      boolean isBulkContent) throws SQLException {
    final String DEBUG_HEADER = "updatePluginIsBulkContentColumn(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pluginId = '" + pluginId + "'");
      log.debug2(DEBUG_HEADER + "isBulkContent = " + isBulkContent);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      statement = prepareStatement(conn, UPDATE_PLUGIN_IS_BULK_CONTENT_QUERY);
      statement.setBoolean(1, isBulkContent);
      statement.setString(2, pluginId);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      String message = "Cannot update plugin bulk content indication";
      log.error(message, sqle);
      log.error("pluginId = '" + pluginId + "'.");
      log.error("isBulkContent = " + isBulkContent);
      log.error("SQL = '" + UPDATE_PLUGIN_IS_BULK_CONTENT_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      String message = "Cannot update plugin bulk content indication";
      log.error(message, re);
      log.error("pluginId = '" + pluginId + "'.");
      log.error("isBulkContent = " + isBulkContent);
      log.error("SQL = '" + UPDATE_PLUGIN_IS_BULK_CONTENT_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Populates the creation time in the Archival Unit metadata table and the
   * fetch time in the metadata item table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void populateArchivalUnitTimestamps(Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "populateArchivalUnitTimestamps(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    boolean success = false;
    boolean done = false;

    try {
      // Keep going while there are Archival Units that need processing.
      while (!done) {
	// Get pairs of plugin identifier and AU key in the database for some
	// Archival Units that need processing.
	statement =
	    prepareStatement(conn, GET_NO_CTIME_PLUGIN_IDS_AU_KEYS_QUERY);
	statement.setMaxRows(5);
	resultSet = executeQuery(statement);

	// If no more pairs are found, the process is known to be finished.
	done = true;

	// Loop through all the pairs of plugin identifier and AU key found.
	while (resultSet.next()) {
	  // A pair is found, so the process is not known to be finished.
	  done = false;

	  // Get the AU_MD primary key.
	  Long auMdSeq = resultSet.getLong(AU_MD_SEQ_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auMdSeq = " + auMdSeq);

	  // Get the plugin identifier.
	  String pluginId = resultSet.getString(PLUGIN_ID_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "pluginId = '" + pluginId + "'");

	  // Get the AU key.
	  String auKey = resultSet.getString(AU_KEY_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "auKey = '" + auKey + "'");

	  // Get the AU identifier.
	  String auId = PluginManager.generateAuId(pluginId, auKey);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "auId = '" + auId + "'");

	  // Get the AU.
	  ArchivalUnit au = LockssDaemon.getLockssDaemon().getPluginManager()
	      .getAuFromId(auId);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

	  long creationTime = 0;

	  // Check whether it is possible to obtain the Archival Unit creation
	  // time.
	  if (au != null && AuUtil.getAuState(au) != null) {
	    // Yes: Get it.
	    creationTime = AuUtil.getAuCreationTime(au);
	  }

	  // Update it in the database.
	  updateAuCreationTime(conn, auMdSeq, creationTime);

	  JdbcBridge.commitOrRollback(conn, log);

	  // Populate the fetch time of all the book chapters/journal articles
	  // of this Archival Unit.
	  populateAuMdItemFetchTimes(conn, au, auMdSeq);
	}

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "done = " + done);
      }

      success = true;
    } catch (SQLException sqle) {
      log.error("Cannot populate Archival Unit timestamps", sqle);
      log.error("SQL = '" + GET_NO_CTIME_PLUGIN_IDS_AU_KEYS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot populate Archival Unit timestamps", re);
      log.error("SQL = '" + GET_NO_CTIME_PLUGIN_IDS_AU_KEYS_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);

      if (success) {
	// Yes: Record the current database version in the database.
	addDbVersion(conn, 10);

	// Commit this partial update.
	JdbcBridge.commitOrRollback(conn, log);
	if (log.isDebug()) log.debug("Database updated to version " + 10);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the creation time of an Archival Unit in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auMdSeq
   *          A Long with the primary key of the Archival Unit in the archival
   *          unit metadata table.
   * @param auCreationTime
   *          A long with the Archival Unit creation time.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void updateAuCreationTime(Connection conn, Long auMdSeq,
      long auCreationTime) throws SQLException {
    final String DEBUG_HEADER = "updateAuCreationTime(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
      log.debug2(DEBUG_HEADER + "auCreationTime = " + auCreationTime);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      statement =
	  prepareStatement(conn, UPDATE_AU_CREATION_TIME_QUERY);
      statement.setLong(1, auCreationTime);
      statement.setLong(2, auMdSeq);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      String message = "Cannot update AU creation time";
      log.error(message, sqle);
      log.error("auMdSeq = " + auMdSeq);
      log.error("auCreationTime = " + auCreationTime);
      log.error("SQL = '" + UPDATE_AU_CREATION_TIME_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      String message = "Cannot update AU creation time";
      log.error(message, re);
      log.error("auMdSeq = " + auMdSeq);
      log.error("auCreationTime = " + auCreationTime);
      log.error("SQL = '" + UPDATE_AU_CREATION_TIME_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Populates in the database the fetch times of all the metadata items for an
   * Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param au
   *          An ArchivalUnit with the Archival Unit.
   * @param auMdSeq
   *          A Long with the primary key of the Archival Unit in the archival
   *          unit metadata table.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void populateAuMdItemFetchTimes(Connection conn, ArchivalUnit au,
      Long auMdSeq) throws SQLException {
    final String DEBUG_HEADER = "populateAuMdItemFetchTimes(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "au = " + au);
      log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    boolean done = false;

    try {
      // Keep going while there are metadata items that need processing.
      while (!done) {
	// Get from the database the next group of metadata items for the
	// Archival Unit that need processing.
	statement = prepareStatement(conn, GET_AU_MD_ITEMS_QUERY);
	statement.setMaxRows(3);
	statement.setLong(1, auMdSeq);
	resultSet = executeQuery(statement);

	// If no more metadata items are found, the process is known to be
	// finished.
	done = true;

	// Loop through all the metadata items found.
	while (resultSet.next()) {
	  // A metadata item is found, so the process is not known to be
	  // finished.
	  done = false;

	  // Get the metadata item primary key.
	  Long mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

	  long fetchTime = 0;

	  // Check whether the AU exists.
	  if (au != null) {
	    // Yes: Get the earliest fetch time of the metadata items URLs.
	    fetchTime = AuUtil.getAuUrlsEarliestFetchTime(au,
		getMdItemUrls(conn, mdItemSeq).values());
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "fetchTime = " + fetchTime);
	  }

	  // Update it in the database.
	  updateMdItemFetchTime(conn, mdItemSeq, fetchTime);
	}

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "done = " + done);

	if (!done) {
	  JdbcBridge.commitOrRollback(conn, log);
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot populate Archival Unit fetch times", sqle);
      log.error("auMdSeq = " + auMdSeq);
      log.error("SQL = '" + GET_AU_MD_ITEMS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot populate Archival Unit fetch times", re);
      log.error("auMdSeq = " + auMdSeq);
      log.error("SQL = '" + GET_AU_MD_ITEMS_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the URLs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Map<String, String> with the URL/feature pairs.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Map<String, String> getMdItemUrls(Connection conn, Long mdItemSeq)
      throws SQLException {
    final String DEBUG_HEADER = "getMdItemUrls(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    Map<String, String> featuredUrlMap = new HashMap<String, String>();

    PreparedStatement findMdItemFeaturedUrl =
	prepareStatement(conn, FIND_MD_ITEM_FEATURED_URL_QUERY);

    ResultSet resultSet = null;
    String feature;
    String url;

    try {
      // Get the existing URLs.
      findMdItemFeaturedUrl.setLong(1, mdItemSeq);
      resultSet = executeQuery(findMdItemFeaturedUrl);

      // Loop through all the URLs already linked to the metadata item.
      while (resultSet.next()) {
	feature = resultSet.getString(FEATURE_COLUMN);
	url = resultSet.getString(URL_COLUMN);
	log.debug3(DEBUG_HEADER + "Found feature = '" + feature + "', URL = '"
	    + url + "'.");

	featuredUrlMap.put(feature, url);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the URLs of a metadata item", sqle);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("SQL = '" + FIND_MD_ITEM_FEATURED_URL_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get the URLs of a metadata item", re);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("SQL = '" + FIND_MD_ITEM_FEATURED_URL_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(findMdItemFeaturedUrl);
    }

    // Add the URLs that are new.
    return featuredUrlMap;
  }

  /**
   * Updates the fetch time of a metadata item in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param fetchTime
   *          A long with the metadata item fetch time.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void updateMdItemFetchTime(Connection conn, Long mdItemSeq,
      long fetchTime) throws SQLException {
    final String DEBUG_HEADER = "updateMdItemFetchTime(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "fetchTime = " + fetchTime);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      statement =
	  prepareStatement(conn, UPDATE_MD_ITEM_FETCH_TIME_QUERY);
      statement.setLong(1, fetchTime);
      statement.setLong(2, mdItemSeq);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      String message = "Cannot update metadata item fetch time";
      log.error(message, sqle);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("fetchTime = " + fetchTime);
      log.error("SQL = '" + UPDATE_MD_ITEM_FETCH_TIME_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      String message = "Cannot update metadata item fetch time";
      log.error(message, re);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("fetchTime = " + fetchTime);
      log.error("SQL = '" + UPDATE_MD_ITEM_FETCH_TIME_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 10 to version 11.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom10To11(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom10To11(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Add the new columns.
    executeDdlQueries(conn, VERSION_11_COLUMN_ADD_QUERIES);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 11 to version 12.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom11To12(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom11To12(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Check whether the Derby database is being used.
    if (isTypeDerby()) {
      // Yes.
      executeDdlQuery(conn, VERSION_12_RENAME_RANGE_COLUMN_DERBY_QUERY);
      // No: Check whether the PostgreSQL database is being used.
    } else if (isTypePostgresql()) {
      executeDdlQuery(conn, VERSION_12_RENAME_RANGE_COLUMN_PG_QUERY);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 12 to version 13.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom12To13(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom12To13(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Reset to zero the identifier of the last metadata item for which the
    // fetch time has been exported.
    zeroLastExportedMdItemId(conn);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Resets to zero the identifier of the last metadata item for which the fetch
   * time has been exported.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  private void zeroLastExportedMdItemId(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "zeroLastExportedMdItemId(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Get the label of the identifier of the last metadata item for which the
    // fetch time has been exported.
    String lastMdItemSeqLabel = ConfigManager.getCurrentConfig()
	.get(FetchTimeExporter.PARAM_FETCH_TIME_EXPORT_LAST_ITEM_LABEL,
	    FetchTimeExporter.DEFAULT_FETCH_TIME_EXPORT_LAST_ITEM_LABEL);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "lastMdItemSeqLabel = '"
	+ lastMdItemSeqLabel + "'.");

    PreparedStatement statement = null;

    try {
      statement = prepareStatement(conn, ZERO_LAST_RUN_VALUE_QUERY);
      statement.setString(1, lastMdItemSeqLabel);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      log.error("Cannot update the last metadata item identifier", sqle);
      log.error("SQL = '" + ZERO_LAST_RUN_VALUE_QUERY + "'.");
      log.error("lastMdItemSeqLabel = '" + lastMdItemSeqLabel + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot update the last metadata item identifier", re);
      log.error("SQL = '" + ZERO_LAST_RUN_VALUE_QUERY + "'.");
      log.error("lastMdItemSeqLabel = '" + lastMdItemSeqLabel + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 13 to version 14.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom13To14(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom13To14(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    try {
      // Add the new column.
      executeDdlQueries(conn, VERSION_14_COLUMN_ADD_QUERIES);
    } catch (SQLException sqle) {
      // Handle the exception thrown when this same database update is performed
      // more than once.
      if (sqle.getMessage().indexOf("Column 'ACTIVE' already exists") == -1) {
	throw sqle;
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 14 to version 15.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom14To15(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom14To15(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Populate in a separate thread the ArchivalUnit active flag for inactive
    // Archival Units.
    DbVersion14To15Migrator migrator = new DbVersion14To15Migrator();
    Thread thread = new Thread(migrator, "DbVersion14To15Migrator");
    LockssDaemon.getLockssDaemon().getDbManager().recordThread(thread);
    new Thread(migrator).start();

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Migrates the contents of the database from version 14 to version 15.
   */
  void migrateDatabaseFrom14To15() {
    final String DEBUG_HEADER = "migrateDatabaseFrom14To15(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = getConnection();

      // Loop through all the Archival Units inactive in the daemon.
      for (String auId : (Collection<String>)LockssDaemon.getLockssDaemon()
	  .getPluginManager().getInactiveAuIds()) {
        if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = '" + auId + "'");

        // Mark this Archival Unit as inactive in the database.
        updateAuActiveFlag(conn, auId, false);
      }

      // Record the current database version in the database.
      addDbVersion(conn, 15);
      JdbcBridge.commitOrRollback(conn, log);
      if (log.isDebug()) log.debug("Database updated to version " + 15);
    } catch (SQLException sqle) {
      log.error("Cannot migrate the database from version 14 to 15", sqle);
    } catch (RuntimeException re) {
      log.error("Cannot migrate the database from version 14 to 15", re);
    } finally {
      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the active flag of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the identifier of the Archival Unit.
   * @param isActive
   *          A boolean with the indication of whether the ArchivalUnit is
   *          active.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  void updateAuActiveFlag(Connection conn, String auId, boolean isActive)
      throws SQLException {
    final String DEBUG_HEADER = "updateAuActiveFlag(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auId = '" + auId + "'");
      log.debug2(DEBUG_HEADER + "isActive = " + isActive);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    String auKey = null;
    String pluginId = null;
    String sql = getUpdateAuActiveFlagSql();
    if (log.isDebug3())	log.debug3(DEBUG_HEADER + "SQL = '" + sql + "'.");

    try {
      statement = prepareStatement(conn, sql);
      statement.setBoolean(1, isActive);

      auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = '" + auKey + "'");

      statement.setString(2, auKey);

      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "pluginId = '" + pluginId + "'");

      statement.setString(3, pluginId);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      String message = "Cannot update AU active flag";
      log.error(message, sqle);
      log.error("isActive = " + isActive);
      log.error("auId = '" + auId + "'.");
      log.error("auKey = '" + auKey + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("SQL = '" + sql + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      String message = "Cannot update AU active flag";
      log.error(message, re);
      log.error("isActive = '" + isActive);
      log.error("auId = '" + auId + "'.");
      log.error("auKey = '" + auKey + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("SQL = '" + sql + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the text of the query used to update the active flag of an
   * Archival Unit.
   * 
   * @return a String with the text of the query used to update the active flag
   *         of an Archival Unit.
   */
  private String getUpdateAuActiveFlagSql() {
    if (isTypeMysql()) {
      return UPDATE_AU_ACTIVE_QUERY_MYSQL;
    }

    return UPDATE_AU_ACTIVE_QUERY;
  }

  /**
   * Updates the database from version 15 to version 16.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom15To16(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom15To16(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Add new metadata item type.
    addMetadataItemType(conn, MD_ITEM_TYPE_BOOK_VOLUME);

    if (log.isDebug2())  log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 16 to version 17.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom16To17(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom16To17(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Populate in a separate thread the ArchivalUnit creation times and the
    // book chapter/journal article fetch times.
    DbVersion16To17Migrator migrator = new DbVersion16To17Migrator();
    Thread thread = new Thread(migrator, "DbVersion16To17Migrator");
    LockssDaemon.getLockssDaemon().getDbManager().recordThread(thread);
    thread.start();

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Migrates the contents of the database from version 16 to version 17.
   */
  void migrateDatabaseFrom16To17() {
    final String DEBUG_HEADER = "migrateDatabaseFrom16To17(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = getConnection();

      // Trim publisher names.
      trimTextColumns(conn, "publisher name",
	  GET_TRIMMABLE_PUBLISHER_NAMES_QUERY, PUBLISHER_NAME_COLUMN,
	  UPDATE_PUBLISHER_NAME_QUERY);

      // Trim metadata item names.
      trimTextColumns(conn, "metadata item name",
	  GET_TRIMMABLE_MD_ITEM_NAMES_QUERY, NAME_COLUMN,
	  UPDATE_MD_ITEM_NAME_QUERY);

      // Trim ISBNs.
      trimTextColumns(conn, "ISBN", GET_TRIMMABLE_ISBNS_QUERY, ISBN_COLUMN,
	  UPDATE_ISBN_QUERY);

      // Trim ISSNs.
      trimTextColumns(conn, "ISSN", GET_TRIMMABLE_ISSNS_QUERY, ISSN_COLUMN,
	  UPDATE_ISSN_QUERY);

      // Trim bibliographic item volumes.
      trimTextColumns(conn, "volume", GET_TRIMMABLE_VOLUMES_QUERY,
	  VOLUME_COLUMN, UPDATE_VOLUME_QUERY);

      // Trim bibliographic item issues.
      trimTextColumns(conn, "issue", GET_TRIMMABLE_ISSUES_QUERY, ISSUE_COLUMN,
	  UPDATE_ISSUE_QUERY);

      // Trim bibliographic item start pages.
      trimTextColumns(conn, "start page", GET_TRIMMABLE_START_PAGES_QUERY,
	  START_PAGE_COLUMN, UPDATE_START_PAGE_QUERY);

      // Trim bibliographic item end pages.
      trimTextColumns(conn, "start page", GET_TRIMMABLE_END_PAGES_QUERY,
	  END_PAGE_COLUMN, UPDATE_END_PAGE_QUERY);

      // Trim bibliographic item numbers.
      trimTextColumns(conn, "item number", GET_TRIMMABLE_ITEM_NOS_QUERY,
	  ITEM_NO_COLUMN, UPDATE_ITEM_NO_QUERY);

      // Trim metadata item dates.
      trimTextColumns(conn, "metadata item date",
	  GET_TRIMMABLE_MD_ITEM_DATES_QUERY, DATE_COLUMN,
	  UPDATE_MD_ITEM_DATE_QUERY);

      // Trim metadata item coverages.
      trimTextColumns(conn, "metadata item coverage",
	  GET_TRIMMABLE_MD_ITEM_COVERAGES_QUERY, COVERAGE_COLUMN,
	  UPDATE_MD_ITEM_COVERAGE_QUERY);

      // Trim author names.
      trimTextColumns(conn, "author name", GET_TRIMMABLE_AUTHOR_NAMES_QUERY,
	  AUTHOR_NAME_COLUMN, UPDATE_AUTHOR_NAME_QUERY);

      // Trim DOIs.
      trimTextColumns(conn, "DOI", GET_TRIMMABLE_DOIS_QUERY, DOI_COLUMN,
	  UPDATE_DOI_QUERY);

      // Trim URLs.
      trimTextColumns(conn, "URL", GET_TRIMMABLE_URLS_QUERY, URL_COLUMN,
	  UPDATE_URL_QUERY);

      // Trim keywords.
      trimTextColumns(conn, "keyword", GET_TRIMMABLE_KEYWORDS_QUERY,
	  KEYWORD_COLUMN, UPDATE_KEYWORD_QUERY);

      // Record the current database version in the database.
      addDbVersion(conn, 17);
      JdbcBridge.commitOrRollback(conn, log);
      if (log.isDebug()) log.debug("Database updated to version " + 17);
    } catch (SQLException sqle) {
      log.error("Cannot migrate the database from version 16 to 17", sqle);
    } catch (RuntimeException re) {
      log.error("Cannot migrate the database from version 16 to 17", re);
    } finally {
      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Trims text columns in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param textKind
   *          A String with the kind of column text to be trimmed.
   * @param findTrimmablesSql
   *          A String with the SQL query used to find rows in the database that
   *          contain trimmable text.
   * @param columnName
   *          A String with the name of the column with the text to be trimmed.
   * @param updateTrimmedSql
   *          A String with the SQL query used to update the text in the
   *          database with the trimmed version.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void trimTextColumns(Connection conn, String textKind,
      String findTrimmablesSql, String columnName, String updateTrimmedSql)
      throws SQLException {
    final String DEBUG_HEADER = "trimTextColumns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "textKind = '" + textKind + "'");
      log.debug2(DEBUG_HEADER + "findTrimmablesSql = '" + findTrimmablesSql
	  + "'");
      log.debug2(DEBUG_HEADER + "columnName = '" + columnName + "'");
      log.debug2(DEBUG_HEADER + "updateTrimmedSql = '" + updateTrimmedSql
	  + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Find the instances of trimmable text strings.
    List<String> trimmableTextColumns = findTrimmableTextColumns(conn,
	textKind, findTrimmablesSql, columnName);

    // Loop through the trimmable text strings.
    for (String trimmableText : trimmableTextColumns) {
      // Trim this text string.
      trimTextColumn(conn, trimmableText, textKind, updateTrimmedSql);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Finds trimmable text columns in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param textKind
   *          A String with the kind of column text to be trimmed.
   * @param findTrimmablesSql
   *          A String with the SQL query used to find rows in the database that
   *          contain trimmable text.
   * @param columnName
   *          A String with the name of the column with the text to be trimmed.
   * @return a List<String> with the text strings that are trimmable.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private List<String> findTrimmableTextColumns(Connection conn,
      String textKind, String findTrimmablesSql, String columnName)
      throws SQLException {
    final String DEBUG_HEADER = "findTrimmableTextColumns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "textKind = '" + textKind + "'");
      log.debug2(DEBUG_HEADER + "findTrimmablesSql = '" + findTrimmablesSql
	  + "'");
      log.debug2(DEBUG_HEADER + "columnName = '" + columnName + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    List<String> trimmableStrings = new ArrayList<String>();
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      // Get the trimmable text strings.
      statement = prepareStatement(conn, findTrimmablesSql);
      resultSet = executeQuery(statement);

      // Loop through all the trimmable text strings.
      while (resultSet.next()) {
	// Get the trimmable column text.
	String text = resultSet.getString(columnName);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "text = '" + text + "'");

	// Check whether it's not empty.
	if (!StringUtil.isNullString(text) && text.trim().length() > 0) {
	  // Yes: Add it to the result.
	  trimmableStrings.add(text);
	} else {
	  // No: Report the problem.
	  log.warning("Ignored empty " + textKind + " '" + text + "'.");
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot get trimmable " + textKind + "s", sqle);
      log.error("SQL = '" + findTrimmablesSql + "'.");
      log.error("columnName = '" + columnName + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get trimmable " + textKind + "s", re);
      log.error("SQL = '" + findTrimmablesSql + "'.");
      log.error("columnName = '" + columnName + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "trimmableStrings.size() = "
	+ trimmableStrings.size());
    return trimmableStrings;
  }

  /**
   * Trims a text column in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param trimmableText
   *          A String with the text of the column to be trimmed.
   * @param textKind
   *          A String with the kind of column text to be trimmed.
   * @param updateTrimmedSql
   *          A String with the SQL query used to update the text in the
   *          database with the trimmed version.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void trimTextColumn(Connection conn, String trimmableText,
      String textKind, String updateTrimmedSql) throws SQLException {
    final String DEBUG_HEADER = "trimTextColumn(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "trimmableText = '" + trimmableText + "'");
      log.debug2(DEBUG_HEADER + "textKind = '" + textKind + "'");
      log.debug2(DEBUG_HEADER + "updateTrimmedSql = '" + updateTrimmedSql
	  + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    String trimmedText = trimmableText.trim();
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "trimmedText = '" + trimmedText + "'");

    // Update the trimmable text with its trimmed value.
    PreparedStatement updateStatement = null;

    try {
      updateStatement = prepareStatement(conn, updateTrimmedSql);
      updateStatement.setString(1, trimmedText);
      updateStatement.setString(2, trimmableText);
      int count = executeUpdate(updateStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
      commitOrRollback(conn, log);
    } catch (SQLException sqle) {
      String message = "Cannot update trimmed " + textKind + " text";
      log.warning(message, sqle);
      log.warning("trimmableText = '" + trimmableText + "'.");
      log.warning("trimmedText = '" + trimmedText + "'.");
      log.warning("SQL = '" + updateTrimmedSql + "'.");
      rollback(conn, log);
    } catch (RuntimeException re) {
      String message = "Cannot update trimmed " + textKind + " text";
      log.warning(message, re);
      log.warning("trimmableText = '" + trimmableText + "'.");
      log.warning("trimmedText = '" + trimmedText + "'.");
      log.warning("SQL = '" + updateTrimmedSql + "'.");
      rollback(conn, log);
    } finally {
      JdbcBridge.safeCloseStatement(updateStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides all the database updates recorded in the database, sorted in
   * ascending order.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a List<Integer> with the recorded database update versions.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  List<Integer> getSortedDatabaseVersions(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "getSortedDatabaseVersions(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    List<Integer> result = new ArrayList<Integer>();
    PreparedStatement stmt = null;
    ResultSet resultSet = null;

    try {
      stmt = prepareStatement(conn, GET_SORTED_DATABASE_VERSIONS_QUERY);
      resultSet = executeQuery(stmt);

      while (resultSet.next()) {
	int version = resultSet.getShort(VERSION_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "version = " + version);
	result.add(version);
      }
    } catch (SQLException sqle) {
      log.error("Cannot get the database version", sqle);
      log.error("SQL = '" + GET_SORTED_DATABASE_VERSIONS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot get the database version", re);
      log.error("SQL = '" + GET_SORTED_DATABASE_VERSIONS_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(stmt);
    }

    log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Updates the database from version 17 to version 18.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom17To18(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom17To18(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary indices.
    executeDdlQueries(conn, VERSION_18_INDEX_CREATE_QUERIES);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 18 to version 19.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom18To19(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom18To19(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary tables if they do not exist.
    createTablesIfMissing(conn, VERSION_19_TABLE_CREATE_QUERIES);

    // Create the necessary indices.
    if (isTypeMysql()) {
      executeDdlQueries(conn, VERSION_19_INDEX_CREATE_MYSQL_QUERIES);
    } else {
      executeDdlQueries(conn, VERSION_19_INDEX_CREATE_QUERIES);
    }

    // Add the new columns.
    executeDdlQueries(conn, VERSION_19_COLUMN_ADD_QUERIES);

    // Populate the subscription provider reference column.
    populateSubscriptionProvider(conn);

    if (isTypeDerby()) {
      // Drop the foreign key constraint of the now obsolete subscription
      // platform reference column. Otherwise, Derby does not allow the dropping
      // of a foreign key column.
      executeDdlQuery(conn, dropConstraintQuery(SUBSCRIPTION_TABLE,
	  "FK_PLATFORM_SEQ_SUBSCRIPTION"));
    } else if (isTypeMysql()) {
      executeDdlQuery(conn, dropMysqlForeignKeyQuery(SUBSCRIPTION_TABLE,
	  "subscription_ibfk_2"));
    }

    // Drop the now obsolete subscription platform reference column.
    executeDdlQuery(conn,
	dropColumnQuery(SUBSCRIPTION_TABLE, PLATFORM_SEQ_COLUMN));

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Populates the provider reference in the subscription table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void populateSubscriptionProvider(Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "populateSubscriptionProvider(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    Long previousPublicationSeq = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      // Get all the subscriptions and publication data.
      statement = prepareStatement(conn, GET_ALL_SUBSCRIPTION_PUBLISHERS_QUERY);
      resultSet = executeQuery(statement);

      // Loop through all the subscriptions and publication data.
      while (resultSet.next()) {
	// Get the subscription identifier.
	Long subscriptionSeq = resultSet.getLong(SUBSCRIPTION_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);

	// Get the publication identifier.
	Long publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

	// Get the publisher name.
	String publisherName = resultSet.getString(PUBLISHER_NAME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publisherName = '" + publisherName + "'");

	// Get the provider primary key for the publisher.
	Long providerSeq = findOrCreateProvider(conn, null, publisherName);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);

	// Check whether this is not the same publication as the previous one.
	if (!publicationSeq.equals(previousPublicationSeq)) {
	  // Yes: Update the subscription provider.
	  updateSubscriptionProvider(conn, subscriptionSeq, providerSeq);

	  // Remember this publication.
	  previousPublicationSeq = publicationSeq;
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER
	      + "previousPublicationSeq = " + previousPublicationSeq);
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot populate subscription provider", sqle);
      log.error("SQL = '" + GET_ALL_SUBSCRIPTION_PUBLISHERS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot populate subscription provider", re);
      log.error("SQL = '" + GET_ALL_SUBSCRIPTION_PUBLISHERS_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifier of a provider if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param providerLid
   *          A String with the provider LOCKSS identifier.
   * @param providerName
   *          A String with the provider name.
   * @return a Long with the identifier of the provider.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Long findOrCreateProvider(Connection conn, String providerLid,
      String providerName) throws SQLException {
    final String DEBUG_HEADER = "findOrCreateProvider(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "providerLid = '" + providerLid + "'");
      log.debug2(DEBUG_HEADER + "providerName = '" + providerName + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    Long providerSeq = null;

    // Check whether a LOCKSS identifier was passed.
    if (!StringUtil.isNullString(providerLid)) {
      // Yes: Find the provider in the database by its LOCKSS identifier.
      providerSeq = findProviderByLid(conn, providerLid);
      if (log.isDebug3())
        log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);
    }

    // Check whether the provider was not found and a name was passed.
    if (providerSeq == null && !StringUtil.isNullString(providerName)) {
      // Yes: Find the provider in the database by its name.
      providerSeq = findProviderByName(conn, providerName);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);

      // Check whether the provider was found and a LOCKSS identifier was
      // passed.
      if (providerSeq != null && !StringUtil.isNullString(providerLid)) {
        // Yes: Try to update the LOCKSS identifier.
	int count = updateProviderLid(conn, providerSeq, providerLid);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	// Check whether no database row was updated.
	if (count == 0) {
	  // Yes: The database row already has a LOCKSS identifier different
	  // than this one, so it is a different provider, albeit with the same
	  // name.
	  providerSeq = null;
	}
      }
    }

    // Check whether it is a new provider.
    if (providerSeq == null) {
      // Yes: Add to the database the new provider.
      providerSeq = addProvider(conn, providerLid, providerName);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "new providerSeq = " + providerSeq);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    return providerSeq;
  }

  /**
   * Provides the identifier of a provider.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param providerLid
   *          A String with the provider LOCKSS identifier.
   * @param providerName
   *          A String with the provider name.
   * @return a Long with the identifier of the provider.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Long findProvider(Connection conn, String providerLid, String providerName)
      throws SQLException {
    final String DEBUG_HEADER = "findProvider(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "providerLid = '" + providerLid + "'");
      log.debug2(DEBUG_HEADER + "providerName = '" + providerName + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    Long providerSeq = null;

    try {
      if (!StringUtil.isNullString(providerLid)) {
	providerSeq = findProviderByLid(conn, providerLid);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);
      }

      if (providerSeq == null && !StringUtil.isNullString(providerName)) {
	providerSeq = findProviderByName(conn, providerName);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find provider";
      log.error(message);
      log.error("providerName = '" + providerName + "'");
      log.error("providerLid = '" + providerLid + "'");
      throw sqle;
    } catch (RuntimeException re) {
      String message = "Cannot find provider";
      log.error(message);
      log.error("providerName = '" + providerName + "'");
      log.error("providerLid = '" + providerLid + "'");
      throw re;
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    return providerSeq;
  }

  /**
   * Provides the identifier of a provider with a given name.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param providerName
   *          A String with the provider name.
   * @return a Long with the identifier of the provider.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private Long findProviderByName(Connection conn, String providerName)
      throws SQLException {
    final String DEBUG_HEADER = "findProviderByName(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "providerName = '" + providerName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    Long providerSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findProvider =
	prepareStatement(conn, FIND_PROVIDER_BY_NAME_QUERY);

    try {
      findProvider.setString(1, providerName);

      resultSet = executeQuery(findProvider);
      if (resultSet.next()) {
	providerSeq = resultSet.getLong(PROVIDER_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find provider", sqle);
      log.error("SQL = '" + FIND_PROVIDER_BY_NAME_QUERY + "'.");
      log.error("providerName = '" + providerName + "'");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find provider", re);
      log.error("SQL = '" + FIND_PROVIDER_BY_NAME_QUERY + "'.");
      log.error("providerName = '" + providerName + "'");
      throw re;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findProvider);
    }

    if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    return providerSeq;
  }

  /**
   * Provides the identifier of a provider with a given LOCKSS identifier.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param providerLid
   *          A String with the provider LOCKSS identifier.
   * @return a Long with the identifier of the provider.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private Long findProviderByLid(Connection conn, String providerLid)
      throws SQLException {
    final String DEBUG_HEADER = "findProviderByLid(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "providerLid = '" + providerLid + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    Long providerSeq = null;

    if (!StringUtil.isNullString(providerLid)) {
      ResultSet resultSet = null;

      PreparedStatement findProvider =
	  prepareStatement(conn, FIND_PROVIDER_BY_LID_QUERY);

      try {
	findProvider.setString(1, providerLid);

	resultSet = executeQuery(findProvider);
	if (resultSet.next()) {
	  providerSeq = resultSet.getLong(PROVIDER_SEQ_COLUMN);
	}
      } catch (SQLException sqle) {
	log.error("Cannot find provider", sqle);
	log.error("SQL = '" + FIND_PROVIDER_BY_LID_QUERY + "'.");
	log.error("providerLid = '" + providerLid + "'");
	throw sqle;
      } catch (RuntimeException re) {
	log.error("Cannot find provider", re);
	log.error("SQL = '" + FIND_PROVIDER_BY_LID_QUERY + "'.");
	log.error("providerLid = '" + providerLid + "'");
	throw re;
      } finally {
	DbManager.safeCloseResultSet(resultSet);
	DbManager.safeCloseStatement(findProvider);
      }
    }

    if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    return providerSeq;
  }

  /**
   * Adds a provider to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param providerLid
   *          A String with the provider LOCKSS identifier.
   * @param providerName
   *          A String with the provider name.
   * @return a Long with the identifier of the provider just added.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Long addProvider(Connection conn, String providerLid, String providerName)
      throws SQLException {
    final String DEBUG_HEADER = "addProvider(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "providerLid = '" + providerLid + "'");
      log.debug2(DEBUG_HEADER + "providerName = '" + providerName + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    Long providerSeq = null;
    ResultSet resultSet = null;
    PreparedStatement insertProvider = prepareStatement(conn,
	INSERT_PROVIDER_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertProvider.setString(1, providerLid);
      insertProvider.setString(2, providerName);
      executeUpdate(insertProvider);
      resultSet = insertProvider.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create provider table row.");
	return null;
      }

      providerSeq = resultSet.getLong(1);
      log.debug3(DEBUG_HEADER + "Added providerSeq = " + providerSeq);
    } catch (SQLException sqle) {
      log.error("Cannot add a provider", sqle);
      log.error("SQL = '" + INSERT_PROVIDER_QUERY + "'.");
      log.error("providerLid = '" + providerLid + "'");
      log.error("providerName = '" + providerName + "'");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot add a provider", re);
      log.error("SQL = '" + INSERT_PROVIDER_QUERY + "'.");
      log.error("providerLid = '" + providerLid + "'");
      log.error("providerName = '" + providerName + "'");
      throw re;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertProvider);
    }

    if (log.isDebug2())
	log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    return providerSeq;
  }

  /**
   * Updates the LOCKSS identifier of a provider.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param providerSeq
   *          A Long with the identifier of the provider.
   * @param lockssId
   *          A String with the provider LOCKSS identifier.
   * @return an int with the number of database rows updated.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int updateProviderLid(Connection conn, Long providerSeq,
      String lockssId) throws SQLException {
    final String DEBUG_HEADER = "updateProviderLid(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
      log.debug2(DEBUG_HEADER + "lockssId = '" + lockssId + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int count = -1;
    PreparedStatement statement = null;

    try {
      statement = prepareStatement(conn, UPDATE_PROVIDER_LID_QUERY);
      statement.setString(1, lockssId);
      statement.setLong(2, providerSeq);

      count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      log.error("Cannot update the provider LOCKSS identifier", sqle);
      log.error("SQL = '" + UPDATE_PROVIDER_LID_QUERY + "'.");
      log.error("providerSeq = " + providerSeq);
      log.error("lockssId = '" + lockssId + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot update the provider LOCKSS identifier", re);
      log.error("SQL = '" + UPDATE_PROVIDER_LID_QUERY + "'.");
      log.error("providerSeq = " + providerSeq);
      log.error("lockssId = '" + lockssId + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count + ".");
    return count;
  }

  /**
   * Updates the provider in the subscription table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param subscriptionSeq
   *          A Long with the primary key of the subscription.
   * @param providerSeq
   *          A Long with the primary key of the provider.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void updateSubscriptionProvider(Connection conn, Long subscriptionSeq,
      Long providerSeq) throws SQLException {
    final String DEBUG_HEADER = "updateSubscriptionProvider(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "subscriptionSeq = " + subscriptionSeq);
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      statement = prepareStatement(conn, UPDATE_SUBSCRIPTION_PROVIDER_QUERY);
      statement.setLong(1, providerSeq);
      statement.setLong(2, subscriptionSeq);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      log.error("Cannot update the subscription provider", sqle);
      log.error("SQL = '" + UPDATE_SUBSCRIPTION_PROVIDER_QUERY + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      log.error("providerSeq = " + providerSeq);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot update the subscription provider", re);
      log.error("SQL = '" + UPDATE_SUBSCRIPTION_PROVIDER_QUERY + "'.");
      log.error("subscriptionSeq = " + subscriptionSeq);
      log.error("providerSeq = " + providerSeq);
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the query used to drop a constraint.
   * 
   * @param tableName
   *          A String with the name of the table to which the constraint to be
   *          dropped belongs.
   * @param constraintName
   *          A String with the name of the constraint to be dropped.
   * @return a String with the query used to drop the constraint.
   */
  private static String dropConstraintQuery(String tableName,
      String constraintName) {
    return "alter table " + tableName + " drop constraint " + constraintName;
  }

  /**
   * Provides the query used to drop a MySQL foreign key constraint.
   * 
   * @param tableName
   *          A String with the name of the table to which the constraint to be
   *          dropped belongs.
   * @param foreignKeyIndexName
   *          A String with the name used to index the foreign key.
   * @return a String with the query used to drop the constraint.
   */
  private static String dropMysqlForeignKeyQuery(String tableName,
      String foreignKeyIndexName) {
    return "alter table " + tableName
	+ " drop foreign key " + foreignKeyIndexName;
  }

  /**
   * Updates the database from version 19 to version 20.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom19To20(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom19To20(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Populate in a separate thread the Archival Unit providers.
    DbVersion19To20Migrator migrator = new DbVersion19To20Migrator();
    Thread thread = new Thread(migrator, "DbVersion19To20Migrator");
    LockssDaemon.getLockssDaemon().getDbManager().recordThread(thread);
    thread.start();

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Migrates the contents of the database from version 19 to version 20.
   */
  void migrateDatabaseFrom19To20() {
    final String DEBUG_HEADER = "migrateDatabaseFrom19To20(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = getConnection();

      // Populate the AU metadata provider reference column.
      populateAuMetadataProvider(conn);

      // Reset to zero the identifier of the last metadata item for which the
      // fetch time has been exported.
      zeroLastExportedMdItemId(conn);

      // Record the current database version in the database.
      addDbVersion(conn, 20);
      JdbcBridge.commitOrRollback(conn, log);
      if (log.isDebug()) log.debug("Database updated to version " + 20);
    } catch (SQLException sqle) {
      log.error("Cannot migrate the database from version 19 to 20", sqle);
    } catch (RuntimeException re) {
      log.error("Cannot migrate the database from version 19 to 20", re);
    } finally {
      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Populates the provider reference in the Archival Unit metadata table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void populateAuMetadataProvider(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "populateAuMetadataProvider(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    Long unknownProviderSeq = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    boolean done = false;

    try {
      // Keep going while there are Archival Units that need processing.
      while (!done) {
	// Get pairs of plugin identifier and AU key in the database for AUs
	// with no provider.
	statement =
	    prepareStatement(conn, GET_NO_PROVIDER_PLUGIN_IDS_AU_KEYS_QUERY);
	statement.setMaxRows(5);
	resultSet = executeQuery(statement);

	// If no more pairs are found, the process is known to be finished.
	done = true;

	// Loop through all the pairs of plugin identifier and AU key found.
	while (resultSet.next()) {
	  // A pair is found, so the process is not known to be finished.
	  done = false;

	  // Get the AU_MD primary key.
	  Long auMdSeq = resultSet.getLong(AU_MD_SEQ_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auMdSeq = " + auMdSeq);

	  // Get the plugin identifier.
	  String pluginId = resultSet.getString(PLUGIN_ID_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "pluginId = '" + pluginId + "'");

	  // Get the AU key.
	  String auKey = resultSet.getString(AU_KEY_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "auKey = '" + auKey + "'");

	  // Determine the provider.
	  Long providerSeq = getAuProvider(conn, pluginId, auKey);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);

	  // Check whether no provider could be found or created.
	  if (providerSeq == null) {
	    // Yes: Find the unknown provider.
	    if (unknownProviderSeq == null) {
	      unknownProviderSeq =
		  findOrCreateProvider(conn, null, UNKNOWN_PROVIDER_NAME);
	      if (log.isDebug3()) log.debug3(DEBUG_HEADER
		  + "unknownProviderSeq = " + unknownProviderSeq);
	    }

	    // Use the unknown provider.
	    providerSeq = unknownProviderSeq;
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);
	  }

	  // Update it in the database.
	  updateAuMdProvider(conn, auMdSeq, providerSeq);

	  JdbcBridge.commitOrRollback(conn, log);
	}

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "done = " + done);
      }
    } catch (SQLException sqle) {
      log.error("Cannot populate Archival Unit providers", sqle);
      log.error("SQL = '" + GET_NO_PROVIDER_PLUGIN_IDS_AU_KEYS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot populate Archival Unit providers", re);
      log.error("SQL = '" + GET_NO_PROVIDER_PLUGIN_IDS_AU_KEYS_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifier of an Archival Unit provider.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginId
   *          A String with the Archival Unit plugin identifier.
   * @param auKey
   *          A String with the Archival Unit key.
   * @return a Long with the provider identifier, or null if it cannot be
   *         determined.
   */
  Long getAuProvider(Connection conn, String pluginId, String auKey) {
    final String DEBUG_HEADER = "getAuProvider(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pluginId = " + pluginId);
      log.debug2(DEBUG_HEADER + "auKey = " + auKey);
    }

    Long providerSeq = null;
    String auId = null;
    ArchivalUnit au = null;
    TdbAu tdbAu = null;
    String providerLid = null;
    String providerName = null;

    try {
      // Get the AU identifier.
      auId = PluginManager.generateAuId(pluginId, auKey);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = '" + auId + "'");

      // Get the AU.
      au = LockssDaemon.getLockssDaemon().getPluginManager().getAuFromId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "au = " + au);

      if (au != null) {
	tdbAu = au.getTdbAu();
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "tdbAu = " + tdbAu);

	if (tdbAu != null) {
	  // Get the provider LOCKSS identifier.
	  // TODO: Replace with tdbAu.getProvider().getLid() when available.
	  providerLid = null;
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "providerLid = '" + providerLid + "'");

	  // Get the provider name.
	  providerName = tdbAu.getProviderName();
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "providerName = '" + providerName + "'");

	  // Determine the provider.
	  providerSeq = findOrCreateProvider(conn, providerLid, providerName);
	} else {
	  if (log.isDebug()) log.debug("Cannot find tdbAU for au = " + au);
	}
      } else {
	if (log.isDebug()) log.debug("Cannot find archival unit for pluginId = "
	    + pluginId + ", auKey = " + auKey);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find the provider identifier", sqle);
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      log.error("auId = " + auId);
      log.error("au = " + au);
      log.error("tdbAu = " + tdbAu);
      log.error("providerLid = " + providerLid);
      log.error("providerName = " + providerName);
    } catch (RuntimeException re) {
      log.error("Cannot find the provider identifier", re);
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      log.error("auId = " + auId);
      log.error("au = " + au);
      log.error("tdbAu = " + tdbAu);
      log.error("providerLid = " + providerLid);
      log.error("providerName = " + providerName);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);

    return providerSeq;
  }

  /**
   * Updates the provider in the Archival Unit metadata table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auMdSeq
   *          A Long with the primary key of the Archival Unit in the archival
   *          unit metadata table.
   * @param providerSeq
   *          A Long with the primary key of the provider.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void updateAuMdProvider(Connection conn, Long auMdSeq,
      Long providerSeq) throws SQLException {
    final String DEBUG_HEADER = "updateAuMdProvider(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;

    try {
      statement = prepareStatement(conn, UPDATE_AU_MD_PROVIDER_QUERY);
      statement.setLong(1, providerSeq);
      statement.setLong(2, auMdSeq);

      int count = executeUpdate(statement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count + ".");
    } catch (SQLException sqle) {
      log.error("Cannot update the Archival Unit provider", sqle);
      log.error("SQL = '" + UPDATE_AU_MD_PROVIDER_QUERY + "'.");
      log.error("auMdSeq = " + auMdSeq);
      log.error("providerSeq = " + providerSeq);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot update the Archival Unit provider", re);
      log.error("SQL = '" + UPDATE_AU_MD_PROVIDER_QUERY + "'.");
      log.error("auMdSeq = " + auMdSeq);
      log.error("providerSeq = " + providerSeq);
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides an indication of whether a given database upgrade has been
   * completed.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param version
   *          An int with the version of the database upgrade to check.
   * @return <code>true</code> if the database upgrade has been completed,
   *         <code>false</code> otherwise.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  boolean isVersionCompleted(Connection conn, int version) throws SQLException {
    final String DEBUG_HEADER = "isVersionCompleted(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "version = " + version);

    boolean result = false;

    PreparedStatement stmt = null;
    ResultSet resultSet = null;

    try {
      stmt = prepareStatement(conn, COUNT_DATABASE_VERSION_QUERY);
      stmt.setInt(1, version);

      resultSet = executeQuery(stmt);
      resultSet.next();
      result = resultSet.getLong(1) > 0;
    } catch (SQLException sqle) {
      log.error("Cannot find a database version", sqle);
      log.error("SQL = '" + COUNT_DATABASE_VERSION_QUERY + "'.");
      log.error("version = " + version);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find a database version", re);
      log.error("SQL = '" + COUNT_DATABASE_VERSION_QUERY + "'.");
      log.error("version = " + version);
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(stmt);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Updates the database from version 20 to version 21.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom20To21(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom20To21(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary tables if they do not exist.
    createTablesIfMissing(conn, VERSION_21_TABLE_CREATE_QUERIES);

    // Create the necessary indices.
    executeDdlQueries(conn, VERSION_21_INDEX_CREATE_QUERIES);

    // Populate the publication proprietary identifier table.
    populateProprietaryIds(conn);

    // Drop the now obsolete publication identifier column.
    executeDdlQuery(conn,
	dropColumnQuery(PUBLICATION_TABLE, OBSOLETE_PUBLICATION_ID_COLUMN));

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Populates the table of publication proprietary identifiers.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private void populateProprietaryIds(Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "populateProprietaryIds(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      // Get all the publication propietary identifiers from the publication
      // table.
      statement = prepareStatement(conn, GET_OLD_PUBLICATION_IDS_QUERY);
      resultSet = executeQuery(statement);

      // Loop through all the publication propietary identifiers.
      while (resultSet.next()) {
	// Get the publication identifier.
	Long mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

	// Get the proprietary identifier.
	String publicationId =
	    resultSet.getString(OBSOLETE_PUBLICATION_ID_COLUMN).trim();
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationId = " + publicationId);

	// Yes: Add the row to the proprietary identifier table.
	addMdItemProprietaryId(conn, mdItemSeq, publicationId);
      }
    } catch (SQLException sqle) {
      log.error("Cannot populate proprietary identifier", sqle);
      log.error("SQL = '" + GET_OLD_PUBLICATION_IDS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot populate proprietary identifier", re);
      log.error("SQL = '" + GET_OLD_PUBLICATION_IDS_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds to the database a metadata item proprietary identifier.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param proprietaryId
   *          A String with the proprietary identifier of the metadata item.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  void addMdItemProprietaryId(Connection conn, Long mdItemSeq,
      String proprietaryId) throws SQLException {
    final String DEBUG_HEADER = "addMdItemProprietaryId(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "proprietaryId = " + proprietaryId);
    }

    if (StringUtil.isNullString(proprietaryId)) {
      return;
    }

    PreparedStatement insertMdItemProprietaryId =
	prepareStatement(conn, INSERT_PROPRIETARY_ID_QUERY);

    try {
      insertMdItemProprietaryId.setLong(1, mdItemSeq);
      insertMdItemProprietaryId.setString(2, proprietaryId);
      int count = executeUpdate(insertMdItemProprietaryId);

      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "count = " + count);
	log.debug3(DEBUG_HEADER + "Added proprietaryId = " + proprietaryId);
      }
    } catch (SQLException sqle) {
      log.error("Cannot add proprietary identifier", sqle);
      log.error("SQL = '" + INSERT_PROPRIETARY_ID_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("proprietaryId = " + proprietaryId);
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot add proprietary identifier", re);
      log.error("SQL = '" + INSERT_PROPRIETARY_ID_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("proprietaryId = " + proprietaryId);
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(insertMdItemProprietaryId);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 21 to version 22.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom21To22(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom21To22(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Populate in a separate thread the unknown Archival Unit providers.
    DbVersion21To22Migrator migrator = new DbVersion21To22Migrator();
    Thread thread = new Thread(migrator, "DbVersion21To22Migrator");
    LockssDaemon.getLockssDaemon().getDbManager().recordThread(thread);
    thread.start();

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Migrates the contents of the database from version 21 to version 22.
   */
  void migrateDatabaseFrom21To22() {
    final String DEBUG_HEADER = "migrateDatabaseFrom21To22(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = getConnection();

      // Check whether the pre-requisite database upgrade has been completed.
      if (isVersionCompleted(conn, 20)) {
	// Yes: Populate the metadata provider for those Archival Units with an
	// unknown metadata provider.
	int populatedFlag = populateUnknownAuMetadataProvider(conn);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "populatedFlag = " + populatedFlag);

	// Check whether some unknown metadata providers have been populated.
	if (populatedFlag > 0) {
	  // Yes: Reset to zero the identifier of the last metadata item for
	  // which the fetch time has been exported.
	  zeroLastExportedMdItemId(conn);

	  // Populate again the metadata provider for those Archival Units with
	  // an unknown metadata provider.
	  if (populateUnknownAuMetadataProvider(conn) == 0) {
	    // Record the current database version in the database.
	    addDbVersion(conn, 22);
	  }

	  // No: Check whether no unknown metadata providers remain in the
	  // database.
	} else if (populatedFlag == 0) {
	  // Record the current database version in the database.
	  addDbVersion(conn, 22);
	}

	JdbcBridge.commitOrRollback(conn, log);
	if (log.isDebug()) log.debug("Database updated to version 22");
      } else {
	// No.
	if (log.isDebug()) log.debug("Database update 22 requires update 20");
      }
    } catch (SQLException sqle) {
      log.error("Cannot migrate the database from version 21 to 22", sqle);
    } catch (RuntimeException re) {
      log.error("Cannot migrate the database from version 21 to 22", re);
    } finally {
      JdbcBridge.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Populates the provider reference in the subscription table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return an int with the indication of whether previously unknown providers
   *         have been populated (= 1), no unknown providers were found (= 0)
   *         or no existing unknown providers have been populated and they
   *         remain unknown (= -1).
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int populateUnknownAuMetadataProvider(Connection conn)
      throws SQLException {
    final String DEBUG_HEADER = "populateUnknownAuMetadataProvider(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    int populatedFlag = 0;

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      // Get pairs of plugin identifier and AU key in the database for AUs with
      // an unknown provider.
      statement =
	  prepareStatement(conn, GET_UNKNOWN_PROVIDER_PLUGIN_IDS_AU_KEYS_QUERY);
      resultSet = executeQuery(statement);

      // Loop through all the pairs of plugin identifier and AU key found.
      while (resultSet.next()) {
	// Remember that there are Archival Units with unknown providers.
	if (populatedFlag == 0) {
	  populatedFlag = -1;
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "populatedFlag = " + populatedFlag);
	}

	// Get the AU_MD primary key.
	Long auMdSeq = resultSet.getLong(AU_MD_SEQ_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auMdSeq = " + auMdSeq);

	// Get the plugin identifier.
	String pluginId = resultSet.getString(PLUGIN_ID_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "pluginId = '" + pluginId + "'");

	// Get the AU key.
	String auKey = resultSet.getString(AU_KEY_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "auKey = '" + auKey + "'");

	// Determine the provider.
	Long providerSeq = getAuProvider(conn, pluginId, auKey);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "providerSeq = " + providerSeq);

	// Check whether a provider could be found or created.
	if (providerSeq != null) {
	  // Yes: Update it in the database.
	  updateAuMdProvider(conn, auMdSeq, providerSeq);

	  JdbcBridge.commitOrRollback(conn, log);

	  // Remember that there are Archival Units that have unknown providers
	  // replaced.
	  populatedFlag = 1;
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "populatedFlag = " + populatedFlag);
	}
      }
    } catch (SQLException sqle) {
      log.error("Cannot populate Archival Unit providers", sqle);
      log.error("SQL = '" + GET_UNKNOWN_PROVIDER_PLUGIN_IDS_AU_KEYS_QUERY
	  + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot populate Archival Unit providers", re);
      log.error("SQL = '" + GET_UNKNOWN_PROVIDER_PLUGIN_IDS_AU_KEYS_QUERY
	  + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseResultSet(resultSet);
      JdbcBridge.safeCloseStatement(statement);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "populatedFlag = " + populatedFlag);
    return populatedFlag;
  }

  /**
   * Updates the database from version 22 to version 23.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom22To23(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom22To23(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Create the necessary indices.
    executeDdlQueries(conn, VERSION_23_INDEX_CREATE_QUERIES);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 23 to version 24.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom23To24(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom23To24(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Check whether the MySQL database is being used.
    if (isTypeMysql()) {
      // Yes: Create the necessary tables if they do not exist.
      createTablesIfMissing(conn, VERSION_24_TABLE_CREATE_MYSQL_QUERIES);
    } else {
    // No: Create the necessary tables if they do not exist.
      createTablesIfMissing(conn, VERSION_24_TABLE_CREATE_QUERIES);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifier of a publisher if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherName
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Long findOrCreatePublisher(Connection conn, String publisherName)
      throws SQLException {
    final String DEBUG_HEADER = "findOrCreatePublisher(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherName = '" + publisherName + "'");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    Long publisherSeq = findPublisher(conn, publisherName);
    if (log.isDebug3())
      log.debug3(DEBUG_HEADER + "publisherSeq = " + publisherSeq);

    // Check whether it is a new publisher.
    if (publisherSeq == null) {
      // Yes: Add to the database the new publisher.
      publisherSeq = addPublisher(conn, publisherName);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "new publisherSeq = " + publisherSeq);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }

  /**
   * Provides the identifier of a publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherName
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Long findPublisher(Connection conn, String publisherName)
      throws SQLException {
    final String DEBUG_HEADER = "findPublisher(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherName = " + publisherName);

    Long publisherSeq = null;
    PreparedStatement findPublisher = null;
    ResultSet resultSet = null;

    try {
      findPublisher = prepareStatement(conn, FIND_PUBLISHER_QUERY);
      findPublisher.setString(1, publisherName);

      resultSet = executeQuery(findPublisher);
      if (resultSet.next()) {
	publisherSeq = resultSet.getLong(PUBLISHER_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find publisher";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PUBLISHER_QUERY + "'.");
      log.error("publisherName = '" + publisherName + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublisher);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }

  /**
   * Adds a publisher to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherName
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher just added.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  Long addPublisher(Connection conn, String publisherName) throws SQLException {
    final String DEBUG_HEADER = "addPublisher(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherName = " + publisherName);

    Long publisherSeq = null;
    ResultSet resultSet = null;
    PreparedStatement insertPublisher = null;

    try {
      insertPublisher = prepareStatement(conn, INSERT_PUBLISHER_QUERY,
	  Statement.RETURN_GENERATED_KEYS);
      // skip auto-increment key field #0
      insertPublisher.setString(1, publisherName);
      executeUpdate(insertPublisher);
      resultSet = insertPublisher.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create publisher table row.");
	return null;
      }

      publisherSeq = resultSet.getLong(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Added publisherSeq = " + publisherSeq);
    } catch (SQLException sqle) {
      String message = "Cannot add publisher";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_PUBLISHER_QUERY + "'.");
      log.error("publisherName = '" + publisherName + "'.");
      throw sqle;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertPublisher);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }

  /**
   * Updates the database from version 24 to version 25.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom24To25(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom24To25(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    convertIsbnsToUpperCase(conn);
    convertIssnsToUpperCase(conn);
    convertDoisToLowerCase(conn);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Converts ISBNs to upper case, if necessary.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  private void convertIsbnsToUpperCase(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "convertIsbnsToUpperCase(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    boolean done = false;

    try {
      // Keep going while there are ISBNs that need processing.
      while (!done) {
	// Get ISBNs in the database that need processing.
	statement = prepareStatement(conn, FIND_LOWER_CASE_ISBNS_QUERY);
	statement.setMaxRows(500);
	resultSet = executeQuery(statement);

	// If no more ISBNs are found, the process is known to be finished.
	done = true;

	// Loop through all the ISBNs found.
	while (resultSet.next()) {
	  // An ISBN is found, so the process is not known to be finished.
	  done = false;

	  // Get the item identifier.
	  Long mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

	  // Get the ISBN.
	  String isbn = resultSet.getString(ISBN_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isbn = " + isbn);

	  // Get the ISBN type.
	  String isbnType = resultSet.getString(ISBN_TYPE_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "isbnType = " + isbnType);

	  // Check whether the upper case version of the ISBN already exists.
	  if (findMdItemTypedIsbn(conn, mdItemSeq, isbn.toUpperCase(),
	      isbnType) != null) {
	    // Yes: Delete the lower case version.
	    int count = removeIsbn(conn, mdItemSeq, isbn, isbnType);
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	    if (count != 1) {
	      log.error("Removing ISBN = " + isbn + " (because "
		  + isbn.toUpperCase()
		  + " already exists) resulted in a count of " + count
		  + " not the expected 1.");
	    }
	  } else {
	    // No: Update it to upper case.
	    int count =
		updateIsbn(conn, mdItemSeq, isbn, isbnType, isbn.toUpperCase());
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	    if (count != 1) {
	      log.error("Updating ISBN = " + isbn + " to " + isbn.toUpperCase()
		  + " resulted in a count of " + count
		  + " not the expected 1.");
	    }
	  }
	}

	JdbcBridge.commitOrRollback(conn, log);
	JdbcBridge.safeCloseResultSet(resultSet);
	JdbcBridge.safeCloseStatement(statement);

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "done = " + done);
      }
    } catch (SQLException sqle) {
      log.error("Cannot convert ISBNs to upper case", sqle);
      log.error("SQL = '" + FIND_LOWER_CASE_ISBNS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot convert ISBNs to upper case", re);
      log.error("SQL = '" + FIND_LOWER_CASE_ISBNS_QUERY + "'.");
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Finds a row in the ISBN table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param isbn
   *          A String with the ISBN.
   * @param isbnType
   *          A String with the ISBN type.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private String findMdItemTypedIsbn(Connection conn, Long mdItemSeq,
      String isbn, String isbnType) throws SQLException {
    final String DEBUG_HEADER = "findMdItemTypedIsbn(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "isbn = '" + isbn + "'");
      log.debug2(DEBUG_HEADER + "isbnType = '" + isbnType + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    String result = null;
    ResultSet resultSet = null;

    PreparedStatement findIsbn =
	prepareStatement(conn, FIND_MD_ITEM_TYPED_ISBN_QUERY);

    try {
      findIsbn.setLong(1, mdItemSeq);
      findIsbn.setString(2, isbn);
      findIsbn.setString(3, isbnType);

      resultSet = executeQuery(findIsbn);
      if (resultSet.next()) {
	result = resultSet.getString(ISBN_COLUMN);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find ISBN", sqle);
      log.error("SQL = '" + FIND_MD_ITEM_TYPED_ISBN_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("isbn = '" + isbn + "'");
      log.error("isbnType = '" + isbnType + "'");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find ISBN", re);
      log.error("SQL = '" + FIND_MD_ITEM_TYPED_ISBN_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("isbn = '" + isbn + "'");
      log.error("isbnType = '" + isbnType + "'");
      throw re;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findIsbn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Removes a row from the ISBN table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param isbn
   *          A String with the ISBN.
   * @param isbnType
   *          A String with the ISBN type.
   * @return an int with the count of rows deleted.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int removeIsbn(Connection conn, Long mdItemSeq, String isbn,
      String isbnType) throws SQLException {
    final String DEBUG_HEADER = "removeIsbn(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "isbn = '" + isbn + "'");
      log.debug2(DEBUG_HEADER + "isbnType = '" + isbnType + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int count;

    PreparedStatement deleteStatement = prepareStatement(conn, REMOVE_ISBN);

    try {
      deleteStatement.setLong(1, mdItemSeq);
      deleteStatement.setString(2, isbn);
      deleteStatement.setString(3, isbnType);

      count = executeUpdate(deleteStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete ISBN", sqle);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("isbn = '" + isbn + "'");
      log.error("isbnType = '" + isbnType + "'");
      log.error("SQL = '" + REMOVE_ISBN + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot delete ISBN", re);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("isbn = '" + isbn + "'");
      log.error("isbnType = '" + isbnType + "'");
      log.error("SQL = '" + REMOVE_ISBN + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(deleteStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Updates an ISBN.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param originalIsbn
   *          A String with the original ISBN.
   * @param isbnType
   *          A String with the ISBN type.
   * @param newIsbn
   *          A String with the new ISBN.
   * @return an int with the count of rows updated.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int updateIsbn(Connection conn, Long mdItemSeq, String originalIsbn,
      String isbnType, String newIsbn) throws SQLException {
    final String DEBUG_HEADER = "updateIsbn(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "originalIsbn = '" + originalIsbn + "'");
      log.debug2(DEBUG_HEADER + "isbnType = '" + isbnType + "'");
      log.debug2(DEBUG_HEADER + "newIsbn = '" + newIsbn + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int count;

    PreparedStatement updateStatement =
	prepareStatement(conn, UPDATE_ISBN_FOR_MD_ITEM_AND_TYPE_QUERY);

    try {
      updateStatement.setString(1, newIsbn);
      updateStatement.setLong(2, mdItemSeq);
      updateStatement.setString(3, originalIsbn);
      updateStatement.setString(4, isbnType);

      count = executeUpdate(updateStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update ISBN", sqle);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("originalIsbn = '" + originalIsbn + "'");
      log.error("isbnType = '" + isbnType + "'");
      log.error("newIsbn = '" + newIsbn + "'");
      log.error("SQL = '" + UPDATE_ISBN_FOR_MD_ITEM_AND_TYPE_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot update ISBN", re);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("originalIsbn = '" + originalIsbn + "'");
      log.error("isbnType = '" + isbnType + "'");
      log.error("newIsbn = '" + newIsbn + "'");
      log.error("SQL = '" + UPDATE_ISBN_FOR_MD_ITEM_AND_TYPE_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(updateStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Converts ISSNs to upper case, if necessary.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  private void convertIssnsToUpperCase(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "convertIssnsToUpperCase(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    boolean done = false;

    try {
      // Keep going while there are ISSNs that need processing.
      while (!done) {
	// Get ISSNs in the database that need processing.
	statement = prepareStatement(conn, FIND_LOWER_CASE_ISSNS_QUERY);
	statement.setMaxRows(500);
	resultSet = executeQuery(statement);

	// If no more ISSNs are found, the process is known to be finished.
	done = true;

	// Loop through all the ISSNs found.
	while (resultSet.next()) {
	  // An ISSN is found, so the process is not known to be finished.
	  done = false;

	  // Get the item identifier.
	  Long mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

	  // Get the ISSN.
	  String issn = resultSet.getString(ISSN_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "issn = " + issn);

	  // Get the ISSN type.
	  String issnType = resultSet.getString(ISSN_TYPE_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "issnType = " + issnType);

	  // Check whether the upper case version of the ISSN already exists.
	  if (findMdItemTypedIssn(conn, mdItemSeq, issn.toUpperCase(),
	      issnType) != null) {
	    // Yes: Delete the lower case version.
	    int count = removeIssn(conn, mdItemSeq, issn, issnType);
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	    if (count != 1) {
	      log.error("Removing ISSN = " + issn + " (because "
		  + issn.toUpperCase()
		  + " already exists) resulted in a count of " + count
		  + " not the expected 1.");
	    }
	  } else {
	    // No: Update it to upper case.
	    int count =
		updateIssn(conn, mdItemSeq, issn, issnType, issn.toUpperCase());
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	    if (count != 1) {
	      log.error("Updating ISSN = " + issn + " to " + issn.toUpperCase()
		  + " resulted in a count of " + count
		  + " not the expected 1.");
	    }
	  }
	}

	JdbcBridge.commitOrRollback(conn, log);
	JdbcBridge.safeCloseResultSet(resultSet);
	JdbcBridge.safeCloseStatement(statement);

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "done = " + done);
      }
    } catch (SQLException sqle) {
      log.error("Cannot convert ISSNs to upper case", sqle);
      log.error("SQL = '" + FIND_LOWER_CASE_ISSNS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot convert ISSNs to upper case", re);
      log.error("SQL = '" + FIND_LOWER_CASE_ISSNS_QUERY + "'.");
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Finds a row in the ISSN table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param issn
   *          A String with the ISSN.
   * @param issnType
   *          A String with the ISSN type.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private String findMdItemTypedIssn(Connection conn, Long mdItemSeq,
      String issn, String issnType) throws SQLException {
    final String DEBUG_HEADER = "findMdItemTypedIssn(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "issn = '" + issn + "'");
      log.debug2(DEBUG_HEADER + "issnType = '" + issnType + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    String result = null;
    ResultSet resultSet = null;

    PreparedStatement findIssn =
	prepareStatement(conn, FIND_MD_ITEM_TYPED_ISSN_QUERY);

    try {
      findIssn.setLong(1, mdItemSeq);
      findIssn.setString(2, issn);
      findIssn.setString(3, issnType);

      resultSet = executeQuery(findIssn);
      if (resultSet.next()) {
	result = resultSet.getString(ISSN_COLUMN);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find ISSN", sqle);
      log.error("SQL = '" + FIND_MD_ITEM_TYPED_ISSN_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("issn = '" + issn + "'");
      log.error("issnType = '" + issnType + "'");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find ISSN", re);
      log.error("SQL = '" + FIND_MD_ITEM_TYPED_ISSN_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("issn = '" + issn + "'");
      log.error("issnType = '" + issnType + "'");
      throw re;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findIssn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Removes a row from the ISSN table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param issn
   *          A String with the ISSN.
   * @param issnType
   *          A String with the ISSN type.
   * @return an int with the count of rows deleted.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int removeIssn(Connection conn, Long mdItemSeq, String issn,
      String issnType) throws SQLException {
    final String DEBUG_HEADER = "removeIssn(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "issn = '" + issn + "'");
      log.debug2(DEBUG_HEADER + "issnType = '" + issnType + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int count;

    PreparedStatement deleteStatement = prepareStatement(conn, REMOVE_ISSN);

    try {
      deleteStatement.setLong(1, mdItemSeq);
      deleteStatement.setString(2, issn);
      deleteStatement.setString(3, issnType);

      count = executeUpdate(deleteStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete ISSN", sqle);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("issn = '" + issn + "'");
      log.error("issnType = '" + issnType + "'");
      log.error("SQL = '" + REMOVE_ISSN + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot delete ISSN", re);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("issn = '" + issn + "'");
      log.error("issnType = '" + issnType + "'");
      log.error("SQL = '" + REMOVE_ISSN + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(deleteStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Updates an ISSN.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param originalIssn
   *          A String with the original ISSN.
   * @param issnType
   *          A String with the ISSN type.
   * @param newIssn
   *          A String with the new ISSN.
   * @return an int with the count of rows updated.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int updateIssn(Connection conn, Long mdItemSeq, String originalIssn,
      String issnType, String newIssn) throws SQLException {
    final String DEBUG_HEADER = "updateIssn(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "originalIssn = '" + originalIssn + "'");
      log.debug2(DEBUG_HEADER + "issnType = '" + issnType + "'");
      log.debug2(DEBUG_HEADER + "newIssn = '" + newIssn + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int count;

    PreparedStatement updateStatement =
	prepareStatement(conn, UPDATE_ISSN_FOR_MD_ITEM_AND_TYPE_QUERY);

    try {
      updateStatement.setString(1, newIssn);
      updateStatement.setLong(2, mdItemSeq);
      updateStatement.setString(3, originalIssn);
      updateStatement.setString(4, issnType);

      count = executeUpdate(updateStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update ISSN", sqle);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("originalIssn = '" + originalIssn + "'");
      log.error("issnType = '" + issnType + "'");
      log.error("newIssn = '" + newIssn + "'");
      log.error("SQL = '" + UPDATE_ISSN_FOR_MD_ITEM_AND_TYPE_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot update ISSN", re);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("originalIssn = '" + originalIssn + "'");
      log.error("issnType = '" + issnType + "'");
      log.error("newIssn = '" + newIssn + "'");
      log.error("SQL = '" + UPDATE_ISSN_FOR_MD_ITEM_AND_TYPE_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(updateStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Converts DOIs to lower case, if necessary.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  private void convertDoisToLowerCase(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "convertDoisToLowerCase(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    PreparedStatement statement = null;
    ResultSet resultSet = null;
    boolean done = false;

    try {
      // Keep going while there are DOIs that need processing.
      while (!done) {
	// Get DOIs in the database that need processing.
	statement = prepareStatement(conn, FIND_UPPER_CASE_DOIS_QUERY);
	statement.setMaxRows(500);
	resultSet = executeQuery(statement);

	// If no more DOIs are found, the process is known to be finished.
	done = true;

	// Loop through all the DOIs found.
	while (resultSet.next()) {
	  // A DOI is found, so the process is not known to be finished.
	  done = false;

	  // Get the item identifier.
	  Long mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	  if (log.isDebug3())
	    log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

	  // Get the DOI.
	  String doi = resultSet.getString(DOI_COLUMN);
	  if (log.isDebug3()) log.debug3(DEBUG_HEADER + "doi = " + doi);

	  // Check whether the lower case version of the DOI already exists.
	  if (findMdItemDoi(conn, mdItemSeq, doi.toLowerCase()) != null) {
	    // Yes: Delete the upper case version.
	    int count = removeDoi(conn, mdItemSeq, doi);
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	    if (count != 1) {
	      log.error("Removing DOI = " + doi + " (because "
		  + doi.toLowerCase()
		  + " already exists) resulted in a count of " + count
		  + " not the expected 1.");
	    }
	  } else {
	    // No: Update it to lower case.
	    int count = updateDoi(conn, mdItemSeq, doi, doi.toLowerCase());
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);

	    if (count != 1) {
	      log.error("Updating DOI = " + doi + " to " + doi.toLowerCase()
		  + " resulted in a count of " + count
		  + " not the expected 1.");
	    }
	  }
	}

	JdbcBridge.commitOrRollback(conn, log);
	JdbcBridge.safeCloseResultSet(resultSet);
	JdbcBridge.safeCloseStatement(statement);

	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "done = " + done);
      }
    } catch (SQLException sqle) {
      log.error("Cannot convert DOIs to lower case", sqle);
      log.error("SQL = '" + FIND_UPPER_CASE_DOIS_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot convert DOIs to upper case", re);
      log.error("SQL = '" + FIND_UPPER_CASE_DOIS_QUERY + "'.");
      throw re;
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Finds a row in the DOI table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param doi
   *          A String with the DOI.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private String findMdItemDoi(Connection conn, Long mdItemSeq, String doi)
      throws SQLException {
    final String DEBUG_HEADER = "findMdItemDoi(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "doi = '" + doi + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    String result = null;
    ResultSet resultSet = null;

    PreparedStatement findDoi = prepareStatement(conn, FIND_MD_ITEM_DOI_QUERY);

    try {
      findDoi.setLong(1, mdItemSeq);
      findDoi.setString(2, doi);

      resultSet = executeQuery(findDoi);
      if (resultSet.next()) {
	result = resultSet.getString(DOI_COLUMN);
      }
    } catch (SQLException sqle) {
      log.error("Cannot find DOI", sqle);
      log.error("SQL = '" + FIND_MD_ITEM_DOI_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("doi = '" + doi + "'");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot find DOI", re);
      log.error("SQL = '" + FIND_MD_ITEM_DOI_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("doi = '" + doi + "'");
      throw re;
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findDoi);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Removes a row from the DOI table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param doi
   *          A String with the DOI.
   * @return an int with the count of rows deleted.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int removeDoi(Connection conn, Long mdItemSeq, String doi)
      throws SQLException {
    final String DEBUG_HEADER = "removeDoi(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "doi = '" + doi + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int count;

    PreparedStatement deleteStatement = prepareStatement(conn, REMOVE_DOI);

    try {
      deleteStatement.setLong(1, mdItemSeq);
      deleteStatement.setString(2, doi);

      count = executeUpdate(deleteStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot delete DOI", sqle);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("doi = '" + doi + "'");
      log.error("SQL = '" + REMOVE_DOI + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot delete DOI", re);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("doi = '" + doi + "'");
      log.error("SQL = '" + REMOVE_DOI + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(deleteStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Updates an DOI.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param originalDoi
   *          A String with the original DOI.
   * @param newDoi
   *          A String with the new DOI.
   * @return an int with the count of rows updated.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  private int updateDoi(Connection conn, Long mdItemSeq, String originalDoi,
      String newDoi) throws SQLException {
    final String DEBUG_HEADER = "updateDoi(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "originalDoi = '" + originalDoi + "'");
      log.debug2(DEBUG_HEADER + "newDoi = '" + newDoi + "'");
    }

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    int count;

    PreparedStatement updateStatement =
	prepareStatement(conn, UPDATE_DOI_FOR_MD_ITEM_QUERY);

    try {
      updateStatement.setString(1, newDoi);
      updateStatement.setLong(2, mdItemSeq);
      updateStatement.setString(3, originalDoi);

      count = executeUpdate(updateStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      log.error("Cannot update DOI", sqle);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("originalDoi = '" + originalDoi + "'");
      log.error("newDoi = '" + newDoi + "'");
      log.error("SQL = '" + UPDATE_DOI_FOR_MD_ITEM_QUERY + "'.");
      throw sqle;
    } catch (RuntimeException re) {
      log.error("Cannot update DOI", re);
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("originalDoi = '" + originalDoi + "'");
      log.error("newDoi = '" + newDoi + "'");
      log.error("SQL = '" + UPDATE_DOI_FOR_MD_ITEM_QUERY + "'.");
      throw re;
    } finally {
      JdbcBridge.safeCloseStatement(updateStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Updates the database from version 25 to version 26.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom25To26(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom25To26(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    if (isTypeDerby()) {
      // Drop the foreign key constraint of the now obsolete provider reference
      // column. Otherwise, Derby does not allow the dropping of a foreign key
      // column.
      executeDdlQuery(conn, dropConstraintQuery(PUBLISHER_SUBSCRIPTION_TABLE,
	  "FK_PROVIDER_SEQ_SUBSCRIPTION"));
    } else if (isTypeMysql()) {
      executeDdlQuery(conn,
	  dropMysqlForeignKeyQuery(PUBLISHER_SUBSCRIPTION_TABLE,
	  "publisher_subscription_ibfk_2"));
    }

    // Drop the now obsolete provider identifier column.
    executeDdlQuery(conn,
	dropColumnQuery(PUBLISHER_SUBSCRIPTION_TABLE, PROVIDER_SEQ_COLUMN));

    // Create the necessary indices.
    executeDdlQueries(conn, VERSION_26_INDEX_CREATE_QUERIES);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 26 to version 27.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom26To27(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom26To27(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Add new metadata item types.
    addMetadataItemType(conn, MD_ITEM_TYPE_PROCEEDINGS);
    addMetadataItemType(conn, MD_ITEM_TYPE_PROCEEDINGS_ARTICLE);
    addMetadataItemType(conn, MD_ITEM_TYPE_UNKNOWN_PUBLICATION);
    addMetadataItemType(conn, MD_ITEM_TYPE_UNKNOWN_ARTICLE);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Updates the database from version 27 to version 28.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @throws SQLException
   *           if any problem occurred updating the database.
   */
  void updateDatabaseFrom27To28(Connection conn) throws SQLException {
    final String DEBUG_HEADER = "updateDatabaseFrom27To28(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    if (conn == null) {
      throw new IllegalArgumentException("Null connection");
    }

    // Add the new metadata item type foe files.
    addMetadataItemType(conn, MD_ITEM_TYPE_FILE);

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }
}
