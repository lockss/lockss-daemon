/*
 * $Id$
 */

/*

 Copyright (c) 2014-2015 Board of Trustees of Leland Stanford Jr. University,
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

/**
 * Constants used in SQL code.
 * 
 * @author Fernando Garcia-Loygorri
 */
public class SqlConstants {
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

  /** Name of the table used to identify the last run of incremental tasks. */
  public static final String LAST_RUN_TABLE = "last_run";

  /** Name of the provider table. */
  public static final String PROVIDER_TABLE = "provider";

  /** Name of the publication proprietary identifier table. */
  public static final String PROPRIETARY_ID_TABLE = "proprietary_id";

  /** Name of the publisher subscription table. */
  public static final String PUBLISHER_SUBSCRIPTION_TABLE =
      "publisher_subscription";

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
  public static final String OBSOLETE_PLATFORM_COLUMN = "platform";

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

  /** Obsolete publication publisher identifier column. */
  public static final String OBSOLETE_PUBLICATION_ID_COLUMN = "publication_id";

  /** Priority column. */
  public static final String PRIORITY_COLUMN = "priority";

  /** is_new column. */
  public static final String ISNEW_COLUMN = "is_new";

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

  /** Obsolete subscription range column. */
  public static final String OBSOLETE_RANGE_COLUMN = "range";

  /** Subscription range type column. */
  public static final String SUBSCRIBED_COLUMN = "subscribed";

  /** Name of the Archival Unit problem description column. */
  public static final String PROBLEM_COLUMN = "problem";

  /** Subscription range index column. */
  public static final String RANGE_IDX_COLUMN = "range_idx";

  /** Archival Unit creation time column. */
  public static final String CREATION_TIME_COLUMN = "creation_time";

  /** Archival Unit metadata needs full reindexing column */
  public static final String FULLY_REINDEX_COLUMN = "fully_reindex";
  
  /** Metadata item fetch time column. */
  public static final String FETCH_TIME_COLUMN = "fetch_time";

  /** Plugin bulk content indication column. */
  public static final String IS_BULK_CONTENT_COLUMN = "is_bulk_content";

  /** Last run label column. */
  public static final String LABEL_COLUMN = "label";

  /** Last run last value column. */
  public static final String LAST_VALUE_COLUMN = "last_value";

  /** Subscription range column. */
  public static final String SUBSCRIPTION_RANGE_COLUMN = "subscription_range";

  /** Archival Unit active indication column */
  public static final String ACTIVE_COLUMN = "active";

  /** Provider columns. */
  public static final String PROVIDER_SEQ_COLUMN = "provider_seq";
  public static final String PROVIDER_LID_COLUMN = "provider_lid";
  public static final String PROVIDER_NAME_COLUMN = "provider_name";

  /** Publisher subscription identifier column. */
  public static final String PUBLISHER_SUBSCRIPTION_SEQ_COLUMN =
      "publisher_subscription_seq";

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

  /** Length of the obsolete publication publisher identifier column. */
  public static final int OBSOLETE_MAX_PUBLICATION_ID_COLUMN = 32;

  /** Length of the system column. */
  public static final int MAX_SYSTEM_COLUMN = 16;

  /** Length of the range column. */
  public static final int MAX_RANGE_COLUMN = 64;

  /** Length of the problem column. */
  public static final int MAX_PROBLEM_COLUMN = 512;

  /** Length of the label column. */
  public static final int MAX_LABEL_COLUMN = 32;

  /** Length of the last value column. */
  public static final int MAX_LAST_VALUE_COLUMN = 32;

  /** Length of the last LOCKSS identifier column. */
  public static final int MAX_LID_COLUMN = 32;

  /** Length of the publication proprietary identifier column. */
  public static final int MAX_PROPRIETARY_ID_COLUMN = 32;

  //
  //Types of metadata items.
  //
  public static final String MD_ITEM_TYPE_BOOK = "book";
  public static final String MD_ITEM_TYPE_BOOK_CHAPTER = "book_chapter";
  public static final String MD_ITEM_TYPE_BOOK_SERIES = "book_series";
  public static final String MD_ITEM_TYPE_JOURNAL = "journal";
  public static final String MD_ITEM_TYPE_JOURNAL_ARTICLE = "journal_article";
  public static final String MD_ITEM_TYPE_BOOK_VOLUME = "book_volume";

  /** The platform name when there is no platform name. */
  public static final String NO_PLATFORM = "";

  /** The name of the unknown provider. */
  public static final String UNKNOWN_PROVIDER_NAME = "UNKNOWN PROVIDER";

  /**
   * The standard type of a name that is primary.
   */
  public static final String PRIMARY_NAME_TYPE = "primary";

  /**
   * The standard type of a name that is not primary.
   */
  public static final String NOT_PRIMARY_NAME_TYPE = "not_primary";

  /**
   * The standard types of ISBNs and ISSNs.
   */
  public static final String E_ISBN_TYPE = "e_isbn";
  public static final String E_ISSN_TYPE = "e_issn";
  public static final String L_ISSN_TYPE = "l_issn";
  public static final String P_ISBN_TYPE = "p_isbn";
  public static final String P_ISSN_TYPE = "p_issn";
}
