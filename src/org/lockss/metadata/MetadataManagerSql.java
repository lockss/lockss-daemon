/*
 * $Id$
 */

/*

 Copyright (c) 2015 Board of Trustees of Leland Stanford Jr. University,
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

import static java.sql.Types.BIGINT;
import static org.lockss.db.SqlConstants.*;
import static org.lockss.metadata.MetadataManager.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.metadata.MetadataManager.PrioritizedAuId;
import org.lockss.plugin.ArchivalUnit;
import org.lockss.plugin.AuUtil;
import org.lockss.plugin.PluginManager;
import org.lockss.util.Logger;
import org.lockss.util.StringUtil;
import org.lockss.util.TimeBase;

public class MetadataManagerSql {
  private static final Logger log = Logger.getLogger(MetadataManagerSql.class);

  private static final int UNKNOWN_VERSION = -1;

  // Query to count enabled pending AUs.
  private static final String COUNT_ENABLED_PENDING_AUS_QUERY = "select "
      + "count(*) from " + PENDING_AU_TABLE
      + " where " + PRIORITY_COLUMN + " >= 0";

  // Query to count bibliographic items.
  private static final String COUNT_BIB_ITEM_QUERY = "select count(*) from "
      + BIB_ITEM_TABLE;

  // Query to count publication items that have associated AU_ITEMs
  // of type 'journal' or 'book'
  private static final String COUNT_PUBLICATION_QUERY = 
        "select count(distinct "
      + PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN + ") from "
      + PUBLISHER_TABLE + "," + PUBLICATION_TABLE + "," 
      + MD_ITEM_TABLE + "," + MD_ITEM_TYPE_TABLE
      + " where " + PUBLISHER_TABLE + "." + PUBLISHER_SEQ_COLUMN
      + "=" + PUBLICATION_TABLE + "." + PUBLISHER_SEQ_COLUMN
      + " and " + PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN
      + "=" + MD_ITEM_TABLE + "." + MD_ITEM_SEQ_COLUMN
      + " and " + MD_ITEM_TABLE + "." + MD_ITEM_TYPE_SEQ_COLUMN
      + "=" + MD_ITEM_TYPE_TABLE + "." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and " + MD_ITEM_TYPE_TABLE + "." + TYPE_NAME_COLUMN
      + " in ('journal','book')";

  // Query to count PUBLISHER items that have associated AU_ITEMs
  private static final String COUNT_PUBLISHER_QUERY = 
        "select count(distinct "
      + PUBLISHER_TABLE + "." + PUBLISHER_SEQ_COLUMN + ") from "
      + PUBLISHER_TABLE + "," + PUBLICATION_TABLE + "," + MD_ITEM_TABLE 
      + " where " + PUBLISHER_TABLE + "." + PUBLISHER_SEQ_COLUMN
      + "=" + PUBLICATION_TABLE + "." + PUBLISHER_SEQ_COLUMN
      + " and " + PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN
      + "=" + MD_ITEM_TABLE + "." + MD_ITEM_SEQ_COLUMN;

  // Query to count PROVIDER items that have associated AU_ITEMs
  private static final String COUNT_PROVIDER_QUERY =
      "select count(distinct "
    + PROVIDER_TABLE + "." + PROVIDER_SEQ_COLUMN + ") from "
    + PROVIDER_TABLE + "," + AU_MD_TABLE + "," + MD_ITEM_TABLE 
    + " where " + PROVIDER_TABLE + "." + PROVIDER_SEQ_COLUMN
    + "=" + AU_MD_TABLE + "." + PROVIDER_SEQ_COLUMN
    + " and " + AU_MD_TABLE + "." + AU_MD_SEQ_COLUMN
    + "=" + MD_ITEM_TABLE + "." + AU_MD_SEQ_COLUMN;
  
  // Query to find enabled pending AUs sorted by priority. Subsitute "true"
  // to prioritize indexing new AUs ahead of reindexing existing ones, "false"
  // to index in the order they were added to the queue
  private static final String FIND_PRIORITIZED_ENABLED_PENDING_AUS_QUERY =
        "select "
      +       PENDING_AU_TABLE + "." + PLUGIN_ID_COLUMN
      + "," + PENDING_AU_TABLE + "." + AU_KEY_COLUMN
      + "," + PENDING_AU_TABLE + "." + PRIORITY_COLUMN
      + ",(" + AU_MD_TABLE + "." + AU_SEQ_COLUMN + " is null) " + ISNEW_COLUMN
      + "," + PENDING_AU_TABLE + "." + FULLY_REINDEX_COLUMN
      + " from " + PENDING_AU_TABLE
      + "   left join " + PLUGIN_TABLE
      + "     on " + PLUGIN_TABLE + "." + PLUGIN_ID_COLUMN
      + "        = " + PENDING_AU_TABLE + "." + PLUGIN_ID_COLUMN
      + "   left join " + AU_TABLE
      + "     on " + AU_TABLE + "." + AU_KEY_COLUMN
      + "        = " + PENDING_AU_TABLE + "." + AU_KEY_COLUMN
      + "    and " + AU_TABLE + "." + PLUGIN_SEQ_COLUMN
      + "        = " + PLUGIN_TABLE + "." + PLUGIN_SEQ_COLUMN
      + "   left join " + AU_MD_TABLE
      + "     on " + AU_MD_TABLE + "." + AU_SEQ_COLUMN
      + "        = " + AU_TABLE + "." + AU_SEQ_COLUMN
      + " where " + PRIORITY_COLUMN + " >= 0"
      + " order by (true = ? and "
      +            AU_MD_TABLE + "." + AU_SEQ_COLUMN + " is not null)," 
      +            PENDING_AU_TABLE + "." + PRIORITY_COLUMN;

  // Query to delete a pending AU by its key and plugin identifier.
  private static final String DELETE_PENDING_AU_QUERY = "delete from "
      + PENDING_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to delete the metadata items of an Archival Unit.
  private static final String DELETE_AU_MD_ITEM_QUERY = "delete from "
      + MD_ITEM_TABLE
      + " where "
      + AU_MD_SEQ_COLUMN + " = ?";

  // Query to get the identifier of the metadata of an AU in the database.
  private static final String FIND_AU_MD_BY_AU_ID_QUERY = "select m."
      + AU_MD_SEQ_COLUMN
      + " from " + AU_MD_TABLE + " m,"
      + AU_TABLE + " a,"
      + PLUGIN_TABLE + " p"
      + " where m." + AU_SEQ_COLUMN + " = " + "a." + AU_SEQ_COLUMN
      + " and a." + PLUGIN_SEQ_COLUMN + " = " + "p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?"
      + " and a." + AU_KEY_COLUMN + " = ?";

  // Query to delete an AU by Archival Unit key and plugin identifier.
  private static final String DELETE_AU_QUERY = "delete from " + AU_TABLE
      + " where "
      + AU_SEQ_COLUMN + " = ?";

  // Query to get the identifier of an AU in the database.
  private static final String FIND_AU_BY_AU_ID_QUERY = "select a."
      + AU_SEQ_COLUMN
      + " from " + AU_TABLE + " a,"
      + PLUGIN_TABLE + " p"
      + " where a." + PLUGIN_SEQ_COLUMN + " = " + "p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?"
      + " and a." + AU_KEY_COLUMN + " = ?";

  // Query to find a plugin by its identifier.
  private static final String FIND_PLUGIN_QUERY = "select "
      + PLUGIN_SEQ_COLUMN
      + " from " + PLUGIN_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?";

  // Query to add a plugin.
  private static final String INSERT_PLUGIN_QUERY = "insert into "
      + PLUGIN_TABLE
      + "(" + PLUGIN_SEQ_COLUMN
      + "," + PLUGIN_ID_COLUMN
      + "," + PLATFORM_SEQ_COLUMN
      + "," + IS_BULK_CONTENT_COLUMN
      + ") values (default,?,?,?)";

  // Query to find an Archival Unit by its plugin and key.
  private static final String FIND_AU_QUERY = "select "
      + AU_SEQ_COLUMN
      + " from " + AU_TABLE
      + " where " + PLUGIN_SEQ_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to add an Archival Unit.
  private static final String INSERT_AU_QUERY = "insert into "
      + AU_TABLE
      + "(" + AU_SEQ_COLUMN
      + "," + PLUGIN_SEQ_COLUMN
      + "," + AU_KEY_COLUMN
      + ") values (default,?,?)";

  // Query to add an Archival Unit metadata entry.
  private static final String INSERT_AU_MD_QUERY = "insert into "
      + AU_MD_TABLE
      + "(" + AU_MD_SEQ_COLUMN
      + "," + AU_SEQ_COLUMN
      + "," + MD_VERSION_COLUMN
      + "," + EXTRACT_TIME_COLUMN
      + "," + CREATION_TIME_COLUMN
      + "," + PROVIDER_SEQ_COLUMN
      + ") values (default,?,?,?,?,?)";

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

  // Query to update the extraction time of the metadata of an Archival Unit.
  private static final String UPDATE_AU_MD_EXTRACT_TIME_QUERY = "update "
      + AU_MD_TABLE
      + " set " + EXTRACT_TIME_COLUMN + " = ?"
      + " where " + AU_MD_SEQ_COLUMN + " = ?";

  // Query to add a publication.
  private static final String INSERT_PUBLICATION_QUERY = "insert into "
      + PUBLICATION_TABLE
      + "(" + PUBLICATION_SEQ_COLUMN
      + "," + MD_ITEM_SEQ_COLUMN
      + "," + PUBLISHER_SEQ_COLUMN
      + ") values (default,?,?)";

  // Query to find the metadata item of a publication.
  private static final String FIND_PUBLICATION_METADATA_ITEM_QUERY = "select "
      + MD_ITEM_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE
      + " where " + PUBLICATION_SEQ_COLUMN + " = ?";

  // Query to find the parent metadata item
  private static final String FIND_PARENT_METADATA_ITEM_QUERY = "select "
      + PARENT_SEQ_COLUMN
      + " from " + MD_ITEM_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";
	
  // Query to add an ISSN.
  private static final String INSERT_ISSN_QUERY = "insert into "
      + ISSN_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + ISSN_COLUMN
      + "," + ISSN_TYPE_COLUMN
      + ") values (?,?,?)";
	
  // Query to add an ISBN.
  private static final String INSERT_ISBN_QUERY = "insert into "
      + ISBN_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + ISBN_COLUMN
      + "," + ISBN_TYPE_COLUMN
      + ") values (?,?,?)";

  // Query to find the ISSNs of a metadata item.
  private static final String FIND_MD_ITEM_ISSN_QUERY = "select "
      + ISSN_COLUMN
      + "," + ISSN_TYPE_COLUMN
      + " from " + ISSN_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to find the proprietary identifiers of a metadata item.
  private static final String FIND_MD_ITEM_PROPRIETARY_ID_QUERY = "select "
      + PROPRIETARY_ID_COLUMN
      + " from " + PROPRIETARY_ID_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to find the ISBNs of a metadata item.
  private static final String FIND_MD_ITEM_ISBN_QUERY = "select "
      + ISBN_COLUMN
      + "," + ISBN_TYPE_COLUMN
      + " from " + ISBN_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to find a publication by its ISSNs.
  private static final String FIND_PUBLICATION_BY_ISSNS_QUERY = "select"
      + " p." + PUBLICATION_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p,"
      + ISSN_TABLE + " i,"
      + MD_ITEM_TABLE + " m,"
      + MD_ITEM_TYPE_TABLE + " t"
      + " where p." + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and m." + AU_MD_SEQ_COLUMN + " is null"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = i." + MD_ITEM_SEQ_COLUMN
      + " and (i." + ISSN_COLUMN + " = ?"
      + " or i." + ISSN_COLUMN + " = ?)"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and m." + MD_ITEM_TYPE_SEQ_COLUMN + " = t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and t." + TYPE_NAME_COLUMN + " = ?";

  // Query to find a publication by its ISBNs.
  private static final String FIND_PUBLICATION_BY_ISBNS_QUERY = "select"
      + " p." + PUBLICATION_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p,"
      + ISBN_TABLE + " i,"
      + MD_ITEM_TABLE + " m,"
      + MD_ITEM_TYPE_TABLE + " t"
      + " where p." + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and m." + AU_MD_SEQ_COLUMN + " is null"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = i." + MD_ITEM_SEQ_COLUMN
      + " and (i." + ISBN_COLUMN + " = ?"
      + " or i." + ISBN_COLUMN + " = ?)"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and m." + MD_ITEM_TYPE_SEQ_COLUMN + " = t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and t." + TYPE_NAME_COLUMN + " = ?";

  // Query to find a publication by its name.
  private static final String FIND_PUBLICATION_BY_NAME_QUERY =
      "select p." + PUBLICATION_SEQ_COLUMN
      + " from " + PUBLICATION_TABLE + " p,"
      + MD_ITEM_TABLE + " m,"
      + MD_ITEM_NAME_TABLE + " n,"
      + MD_ITEM_TYPE_TABLE + " t"
      + " where p." + PUBLISHER_SEQ_COLUMN + " = ?"
      + " and m." + AU_MD_SEQ_COLUMN + " is null"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = n." + MD_ITEM_SEQ_COLUMN
      + " and n." + NAME_COLUMN + " = ?"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + MD_ITEM_SEQ_COLUMN
      + " and m." + MD_ITEM_TYPE_SEQ_COLUMN + " = t." + MD_ITEM_TYPE_SEQ_COLUMN
      + " and t." + TYPE_NAME_COLUMN + " = ?";
  
  // Query to find a metadata item type by its name.
  private static final String FIND_MD_ITEM_TYPE_QUERY = "select "
      + MD_ITEM_TYPE_SEQ_COLUMN
      + " from " + MD_ITEM_TYPE_TABLE
      + " where " + TYPE_NAME_COLUMN + " = ?";

  // Query to add a metadata item.
  private static final String INSERT_MD_ITEM_QUERY = "insert into "
      + MD_ITEM_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + PARENT_SEQ_COLUMN
      + "," + MD_ITEM_TYPE_SEQ_COLUMN
      + "," + AU_MD_SEQ_COLUMN
      + "," + DATE_COLUMN
      + "," + COVERAGE_COLUMN
      + "," + FETCH_TIME_COLUMN
      + ") values (default,?,?,?,?,?,?)";
  
  // Query to count the ISBNs of a publication.
  private static final String COUNT_PUBLICATION_ISBNS_QUERY = "select "
      + "count(*)"
      + " from " + ISBN_TABLE + " i"
      + "," + PUBLICATION_TABLE + " p"
      + " where p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = i." + MD_ITEM_SEQ_COLUMN;
  
  // Query to count the ISSNs of a publication.
  private static final String COUNT_PUBLICATION_ISSNS_QUERY = "select "
      + "count(*)"
      + " from " + ISSN_TABLE + " i"
      + "," + PUBLICATION_TABLE + " p"
      + " where p." + PUBLICATION_SEQ_COLUMN + " = ?"
      + " and p." + MD_ITEM_SEQ_COLUMN + " = i." + MD_ITEM_SEQ_COLUMN;

  // Query to find the secondary names of a metadata item.
  private static final String FIND_MD_ITEM_NAME_QUERY = "select "
      + NAME_COLUMN
      + "," + NAME_TYPE_COLUMN
      + " from " + MD_ITEM_NAME_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to add a metadata item name.
  private static final String INSERT_MD_ITEM_NAME_QUERY = "insert into "
      + MD_ITEM_NAME_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + NAME_COLUMN
      + "," + NAME_TYPE_COLUMN
      + ") values (?,?,?)";

  // Query to add a metadata item URL.
  private static final String INSERT_URL_QUERY = "insert into "
      + URL_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + FEATURE_COLUMN
      + "," + URL_COLUMN
      + ") values (?,?,?)";

  // Query to add a metadata item DOI.
  private static final String INSERT_DOI_QUERY = "insert into "
      + DOI_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + DOI_COLUMN
      + ") values (?,?)";

  // Query to delete a disabled pending AU by its key and plugin identifier.
  private static final String DELETE_DISABLED_PENDING_AU_QUERY = "delete from "
      + PENDING_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?"
      + " and " + PRIORITY_COLUMN + " < 0";

  // Query to add an enabled pending AU at the bottom of the current priority
  // list.
  private static final String INSERT_ENABLED_PENDING_AU_QUERY = "insert into "
      + PENDING_AU_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + PRIORITY_COLUMN
      + "," + FULLY_REINDEX_COLUMN
      + ") values (?,?,"
      + "(select coalesce(max(" + PRIORITY_COLUMN + "), 0) + 1"
      + " from " + PENDING_AU_TABLE
      + " where " + PRIORITY_COLUMN + " >= 0),?)";

  // Query to add an enabled pending AU at the bottom of the current priority
  // list using MySQL.
  private static final String INSERT_ENABLED_PENDING_AU_MYSQL_QUERY = "insert "
      + "into " + PENDING_AU_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + PRIORITY_COLUMN
      + "," + FULLY_REINDEX_COLUMN
      + ") values (?,?,"
      + "(select next_priority from "
      + "(select coalesce(max(" + PRIORITY_COLUMN + "), 0) + 1 as next_priority"
      + " from " + PENDING_AU_TABLE
      + " where " + PRIORITY_COLUMN + " >= 0) as temp_pau_table),?)";

  // Query to find a pending AU by its key and plugin identifier.
  private static final String FIND_PENDING_AU_QUERY = "select "
      + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + " from " + PENDING_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to get the version of the metadata of an AU as is recorded in the
  // database.
  private static final String FIND_AU_METADATA_VERSION_QUERY = "select m."
      + MD_VERSION_COLUMN
      + " from " + AU_MD_TABLE + " m,"
      + AU_TABLE + " a,"
      + PLUGIN_TABLE + " p"
      + " where m." + AU_SEQ_COLUMN + " = " + " a." + AU_SEQ_COLUMN
      + " and a." + PLUGIN_SEQ_COLUMN + " = " + " p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?"
      + " and a." + AU_KEY_COLUMN + " = ?";

  // Query to find the full reindexing flag of an Archival Unit.
  private static final String FIND_AU_FULL_REINDEXING_BY_AU_QUERY = "select "
      + FULLY_REINDEX_COLUMN
      + " from " + PENDING_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";
  
  // Query to update the full reindexing of an Archival Unit.
  private static final String UPDATE_AU_FULL_REINDEXING_QUERY = "update "
      + PENDING_AU_TABLE
      + " set " + FULLY_REINDEX_COLUMN + " = ?"
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to find the extraction time of an Archival Unit.
  private static final String FIND_AU_MD_EXTRACT_TIME_BY_AUSEQ_QUERY = "select "
      + EXTRACT_TIME_COLUMN
      + " from " + AU_MD_TABLE
      + " where " + AU_SEQ_COLUMN + " = ?";

  // Query to find the extraction time of an Archival Unit.
  private static final String FIND_AU_MD_EXTRACT_TIME_BY_AU_QUERY = "select m."
      + EXTRACT_TIME_COLUMN
      + " from " + AU_MD_TABLE + " m,"
      + AU_TABLE + " a,"
      + PLUGIN_TABLE + " p"
      + " where m." + AU_SEQ_COLUMN + " = " + " a." + AU_SEQ_COLUMN
      + " and a." + PLUGIN_SEQ_COLUMN + " = " + " p." + PLUGIN_SEQ_COLUMN
      + " and p." + PLUGIN_ID_COLUMN + " = ?"
      + " and a." + AU_KEY_COLUMN + " = ?";

  // Query to find a platform by its name.
  private static final String FIND_PLATFORM_QUERY = "select "
      + PLATFORM_SEQ_COLUMN
      + " from " + PLATFORM_TABLE
      + " where " + PLATFORM_NAME_COLUMN + " = ?";

  // Query to add a platform.
  private static final String INSERT_PLATFORM_QUERY = "insert into "
      + PLATFORM_TABLE
      + "(" + PLATFORM_SEQ_COLUMN
      + "," + PLATFORM_NAME_COLUMN
      + ") values (default,?)";

  // Query to add a disabled pending AU.
  private static final String INSERT_DISABLED_PENDING_AU_QUERY = "insert into "
      + PENDING_AU_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + PRIORITY_COLUMN
      + ") values (?,?," + MIN_INDEX_PRIORITY + ")";

  // Query to add a pending AU with failed indexing.
  private static final String INSERT_FAILED_INDEXING_PENDING_AU_QUERY = "insert"
      + " into "
      + PENDING_AU_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + "," + PRIORITY_COLUMN
      + ") values (?,?," + FAILED_INDEX_PRIORITY + ")";
  
  // Query to find pending AUs with a given priority.
  private static final String FIND_PENDING_AUS_WITH_PRIORITY_QUERY =
      "select "
      + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + " from " + PENDING_AU_TABLE
      + " where " + PRIORITY_COLUMN + " = ?";

  // Query to find the publisher of an Archival Unit.
  private static final String FIND_AU_PUBLISHER_QUERY = "select distinct "
      + "pr." + PUBLISHER_SEQ_COLUMN
      + " from " + PUBLISHER_TABLE + " pr"
      + "," + PUBLICATION_TABLE + " p"
      + "," + MD_ITEM_TABLE + " m"
      + "," + AU_MD_TABLE + " am"
      + " where pr." + PUBLISHER_SEQ_COLUMN + " = p." + PUBLISHER_SEQ_COLUMN
      + " and p." + MD_ITEM_SEQ_COLUMN + " = m." + PARENT_SEQ_COLUMN
      + " and m." + AU_MD_SEQ_COLUMN + " = am." + AU_MD_SEQ_COLUMN
      + " and am." + AU_SEQ_COLUMN + " = ?";

  // Query to find the authors of a metadata item.
  private static final String FIND_MD_ITEM_AUTHOR_QUERY = "select "
      + AUTHOR_NAME_COLUMN
      + " from " + AUTHOR_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to find the keywords of a metadata item.
  private static final String FIND_MD_ITEM_KEYWORD_QUERY = "select "
      + KEYWORD_COLUMN
      + " from " + KEYWORD_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?";

  // Query to add a metadata item author.
  private static final String INSERT_AUTHOR_QUERY = "insert into "
      + AUTHOR_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + AUTHOR_NAME_COLUMN
      + "," + AUTHOR_IDX_COLUMN
      + ") values (?,?,"
      + "(select coalesce(max(" + AUTHOR_IDX_COLUMN + "), 0) + 1"
      + " from " + AUTHOR_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?))";

  // Query to add a metadata item author using MySQL.
  private static final String INSERT_AUTHOR_MYSQL_QUERY = "insert into "
      + AUTHOR_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + AUTHOR_NAME_COLUMN
      + "," + AUTHOR_IDX_COLUMN
      + ") values (?,?,"
      + "(select next_idx from "
      + "(select coalesce(max(" + AUTHOR_IDX_COLUMN + "), 0) + 1 as next_idx"
      + " from " + AUTHOR_TABLE
      + " where " + MD_ITEM_SEQ_COLUMN + " = ?) as temp_author_table))";
  
  // Query to add a metadata item keyword.
  private static final String INSERT_KEYWORD_QUERY = "insert into "
      + KEYWORD_TABLE
      + "(" + MD_ITEM_SEQ_COLUMN
      + "," + KEYWORD_COLUMN
      + ") values (?,?)";

  // Query to add an archival unit to the UNCONFIGURED_AU table.
  private static final String INSERT_UNCONFIGURED_AU_QUERY = "insert into "
      + UNCONFIGURED_AU_TABLE
      + "(" + PLUGIN_ID_COLUMN
      + "," + AU_KEY_COLUMN
      + ") values (?,?)";

  // Query to remove an archival unit from the UNCONFIGURED_AU table.
  private static final String DELETE_UNCONFIGURED_AU_QUERY = "delete from "
      + UNCONFIGURED_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to count recorded unconfigured archival units.
  private static final String UNCONFIGURED_AU_COUNT_QUERY = "select "
      + "count(*)"
      + " from " + UNCONFIGURED_AU_TABLE;
  
  // Query to find if an archival unit is in the UNCONFIGURED_AU table.
  private static final String FIND_UNCONFIGURED_AU_COUNT_QUERY = "select "
      + "count(*)"
      + " from " + UNCONFIGURED_AU_TABLE
      + " where " + PLUGIN_ID_COLUMN + " = ?"
      + " and " + AU_KEY_COLUMN + " = ?";

  // Query to delete an Archival Unit child metadata item.
  private static final String DELETE_AU_CHILD_MD_ITEM_QUERY = "delete from "
      + MD_ITEM_TABLE
      + " where "
      + AU_MD_SEQ_COLUMN + " = ?"
      + " and " + MD_ITEM_SEQ_COLUMN + " = ?"
      + " and " + PARENT_SEQ_COLUMN + " is not null";

  private DbManager dbManager;
  private MetadataManager metadataManager;

  /**
   * Constructor.
   * 
   * @param dbManager
   *          A DbManager with the database manager.
   * @param metadataManager
   *          A MetadataManager with the metadata manager.
   */
  MetadataManagerSql(DbManager dbManager, MetadataManager metadataManager)
      throws DbException {
    this.dbManager = dbManager;
    this.metadataManager = metadataManager;
  }

  /**
   * Provides the number of enabled pending AUs.
   * 
   * @return a long with the number of enabled pending AUs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getEnabledPendingAusCount() throws DbException {
    final String DEBUG_HEADER = "getEnabledPendingAusCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    try {
      rowCount = getEnabledPendingAusCount(conn);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of enabled pending AUs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the number of enabled pending AUs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getEnabledPendingAusCount(Connection conn) throws DbException {
    final String DEBUG_HEADER = "getEnabledPendingAusCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    PreparedStatement stmt =
	dbManager.prepareStatement(conn, COUNT_ENABLED_PENDING_AUS_QUERY);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(stmt);
      resultSet.next();
      rowCount = resultSet.getLong(1);
    } catch (SQLException sqle) {
      String message = "Cannot get the count of enabled pending AUs";
      log.error(message, sqle);
      log.error("SQL = '" + COUNT_ENABLED_PENDING_AUS_QUERY + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of articles in the metadata database.
   * 
   * @return a long with the number of articles in the metadata database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getArticleCount() throws DbException {
    final String DEBUG_HEADER = "getArticleCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    try {
      rowCount = getArticleCount(conn);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of articles in the metadata database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the number of articles in the metadata database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getArticleCount(Connection conn) throws DbException {
    final String DEBUG_HEADER = "getArticleCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    PreparedStatement stmt =
        dbManager.prepareStatement(conn, COUNT_BIB_ITEM_QUERY);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(stmt);
      resultSet.next();
      rowCount = resultSet.getLong(1);
    } catch (SQLException sqle) {
      String message = "Cannot get the count of articles";
      log.error(message, sqle);
      log.error("SQL = '" + COUNT_BIB_ITEM_QUERY + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of publications in the metadata database.
   * 
   * @return a long with the number of publications in the metadata database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getPublicationCount() throws DbException {
    final String DEBUG_HEADER = "getPublicationCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    try {
      rowCount = getPublicationCount(conn);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of publications in the metadata database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the number of publications in the metadata database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getPublicationCount(Connection conn) throws DbException {
    final String DEBUG_HEADER = "getPublicationCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    PreparedStatement stmt =
	dbManager.prepareStatement(conn, COUNT_PUBLICATION_QUERY);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(stmt);
      resultSet.next();
      rowCount = resultSet.getLong(1);
    } catch (SQLException sqle) {
      String message = "Cannot get the count of publications";
      log.error(message, sqle);
      log.error("SQL = '" + COUNT_PUBLICATION_QUERY + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of publishers in the metadata database.
   * 
   * @return a long with the number of publishers in the metadata database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getPublisherCount() throws DbException {
    final String DEBUG_HEADER = "getPublisherCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    try {
      rowCount = getPublisherCount(conn);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of publishers in the metadata database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the number of publishers in the metadata database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getPublisherCount(Connection conn) throws DbException {
    final String DEBUG_HEADER = "getPublisherCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    PreparedStatement stmt =
        dbManager.prepareStatement(conn, COUNT_PUBLISHER_QUERY);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(stmt);
      resultSet.next();
      rowCount = resultSet.getLong(1);
    } catch (SQLException sqle) {
      String message = "Cannot get the count of publishers";
      log.error(message, sqle);
      log.error("SQL = '" + COUNT_PUBLISHER_QUERY + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of publishers in the metadata database.
   * 
   * @return a long with the number of publishers in the metadata database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getProviderCount() throws DbException {
    final String DEBUG_HEADER = "getProviderCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    // Get a connection to the database.
    Connection conn = dbManager.getConnection();

    try {
      rowCount = getProviderCount(conn);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides the number of publishers in the metadata database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the number of publishers in the metadata database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getProviderCount(Connection conn) throws DbException {
    final String DEBUG_HEADER = "getProviderCount(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    long rowCount = -1;

    PreparedStatement stmt =
        dbManager.prepareStatement(conn, COUNT_PROVIDER_QUERY);
    ResultSet resultSet = null;

    try {
      resultSet = dbManager.executeQuery(stmt);
      resultSet.next();
      rowCount = resultSet.getLong(1);
    } catch (SQLException sqle) {
      String message = "Cannot get the count of providers";
      log.error(message, sqle);
      log.error("SQL = '" + COUNT_PROVIDER_QUERY + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(stmt);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides a list of AuIds that require reindexing sorted by priority.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param maxAuIds
   *          An int with the maximum number of AuIds to return.
   * @param prioritizeIndexingNewAus
   *          A boolean with the indication of whether to prioritize new
   *          Archival Units for indexing purposes.
   * @return a List<String> with the list of AuIds that require reindexing
   *         sorted by priority.
   */
  List<PrioritizedAuId> getPrioritizedAuIdsToReindex(Connection conn,
      int maxAuIds, boolean prioritizeIndexingNewAus) {
    final String DEBUG_HEADER = "getPrioritizedAuIdsToReindex(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "maxAuIds = " + maxAuIds);
      log.debug2(DEBUG_HEADER + "prioritizeIndexingNewAus = "
	  + prioritizeIndexingNewAus);
    }

    ArrayList<PrioritizedAuId> auIds = new ArrayList<PrioritizedAuId>();

    PreparedStatement selectPendingAus = null;
    ResultSet results = null;
    String sql = FIND_PRIORITIZED_ENABLED_PENDING_AUS_QUERY;
      
    try {
      selectPendingAus = dbManager.prepareStatement(conn, sql);
      selectPendingAus.setBoolean(1, prioritizeIndexingNewAus);
      results = dbManager.executeQuery(selectPendingAus);

      while ((auIds.size() < maxAuIds) && results.next()) {
	String pluginId = results.getString(PLUGIN_ID_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
	String auKey = results.getString(AU_KEY_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);
	String auId = PluginManager.generateAuId(pluginId, auKey);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	if (metadataManager.isEligibleForReindexing(auId)) {
	  if (!metadataManager.activeReindexingTasks.containsKey(auId)) {
	    PrioritizedAuId auToReindex = new PrioritizedAuId();
	    auToReindex.auId = auId;

	    long priority = results.getLong(PRIORITY_COLUMN);
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "priority = " + priority);
	    auToReindex.priority = priority;

	    boolean isNew = results.getBoolean(ISNEW_COLUMN);
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "isNew = " + isNew);
	    auToReindex.isNew = isNew;

	    boolean needFullReindex = results.getBoolean(FULLY_REINDEX_COLUMN);
	    if (log.isDebug3())
	      log.debug3(DEBUG_HEADER + "needFullReindex = " + needFullReindex);
	    auToReindex.needFullReindex = needFullReindex;

	    auIds.add(auToReindex);
	    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Added auId = " + auId
		+ " to reindex list");
	  }
	}
      }
    } catch (SQLException sqle) {
      String message = "Cannot identify the enabled pending AUs";
      log.error(message, sqle);
      log.error("maxAuIds = " + maxAuIds);
      log.error("SQL = '" + sql + "'.");
      log.error("prioritizeIndexingNewAus = " + prioritizeIndexingNewAus);
    } catch (DbException dbe) {
      String message = "Cannot identify the enabled pending AUs";
      log.error(message, dbe);
      log.error("SQL = '" + sql + "'.");
      log.error("prioritizeIndexingNewAus = " + prioritizeIndexingNewAus);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(selectPendingAus);
    }

    auIds.trimToSize();
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "auIds.size() = " + auIds.size());
    return auIds;
  }

  /**
   * Removes an AU from the pending Aus table.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long removeFromPendingAus(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "removeFromPendingAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    String pluginId = null;
    String auKey = null;
    PreparedStatement deletePendingAu =
	dbManager.prepareStatement(conn, DELETE_PENDING_AU_QUERY);

    try {
      pluginId = PluginManager.pluginIdFromAuId(auId);
      auKey = PluginManager.auKeyFromAuId(auId);
  
      deletePendingAu.setString(1, pluginId);
      deletePendingAu.setString(2, auKey);
      dbManager.executeUpdate(deletePendingAu);
    } catch (SQLException sqle) {
      String message = "Cannot remove AU from pending table";
      log.error(message, sqle);
      log.error("auId = '" + auId + "'.");
      log.error("SQL = '" + DELETE_PENDING_AU_QUERY + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("auKey = '" + auKey + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(deletePendingAu);
    }

    long enabledPendingAusCount = getEnabledPendingAusCount(conn);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "enabledPendingAusCount = "
	+ enabledPendingAusCount);
    return enabledPendingAusCount;
  }

  /**
   * Removes all metadata items for an AU.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @return an int with the number of metadata items deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int removeAuMetadataItems(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "removeAuMetadataItems(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);
    int count = 0;

    Long auMdSeq = findAuMdByAuId(conn, auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auMdSeq = " + auMdSeq);

    if (auMdSeq != null) {
      PreparedStatement deleteMetadataItems =
	  dbManager.prepareStatement(conn, DELETE_AU_MD_ITEM_QUERY);

      try {
	deleteMetadataItems.setLong(1, auMdSeq);
	count = dbManager.executeUpdate(deleteMetadataItems);
      } catch (SQLException sqle) {
	String message = "Cannot delete AU metadata items";
	log.error(message, sqle);
	log.error("auId = " + auId);
	log.error("SQL = '" + DELETE_AU_MD_ITEM_QUERY + "'.");
	log.error("auMdSeq = " + auMdSeq);
	throw new DbException(message, sqle);
      } finally {
	DbManager.safeCloseStatement(deleteMetadataItems);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Provides the identifier of an Archival Unit metadata.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @return a Long with the identifier of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long findAuMdByAuId(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "findAuMdByAuId(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    String pluginId = null;
    String auKey = null;
    Long auMdSeq = null;
    PreparedStatement findAuMd =
	dbManager.prepareStatement(conn, FIND_AU_MD_BY_AU_ID_QUERY);
    ResultSet resultSet = null;

    try {
      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId() = " + pluginId);

      auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      findAuMd.setString(1, pluginId);
      findAuMd.setString(2, auKey);
      resultSet = dbManager.executeQuery(findAuMd);

      if (resultSet.next()) {
	auMdSeq = resultSet.getLong(AU_MD_SEQ_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find AU metadata identifier";
      log.error(message, sqle);
      log.error("auId = " + auId);
      log.error("SQL = '" + FIND_AU_MD_BY_AU_ID_QUERY + "'.");
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAuMd);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
    return auMdSeq;
  }

  /**
   * Removes an AU.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @return an int with the number of rows deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int removeAu(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "removeAu(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);
    int count = 0;

    Long auSeq = findAuByAuId(conn, auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auSeq = " + auSeq);

    if (auSeq != null) {
      PreparedStatement deleteAu =
	  dbManager.prepareStatement(conn, DELETE_AU_QUERY);

      try {
	deleteAu.setLong(1, auSeq);
	count = dbManager.executeUpdate(deleteAu);
      } catch (SQLException sqle) {
	String message = "Cannot delete AU";
	log.error(message, sqle);
	log.error("auId = " + auId);
	log.error("SQL = '" + DELETE_AU_QUERY + "'.");
	log.error("auSeq = " + auSeq);
	throw new DbException(message, sqle);
      } finally {
	DbManager.safeCloseStatement(deleteAu);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }

  /**
   * Provides the identifier of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @return a Long with the identifier of the Archival Unit.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findAuByAuId(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "findAuByAuId(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    String pluginId = PluginManager.pluginIdFromAuId(auId);
    String auKey = PluginManager.auKeyFromAuId(auId);
    Long auSeq = null;
    PreparedStatement findAu =
	dbManager.prepareStatement(conn, FIND_AU_BY_AU_ID_QUERY);
    ResultSet resultSet = null;

    try {
      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId() = " + pluginId);

      auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      findAu.setString(1, pluginId);
      findAu.setString(2, auKey);
      resultSet = dbManager.executeQuery(findAu);

      if (resultSet.next()) {
	auSeq = resultSet.getLong(AU_SEQ_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auSeq = " + auSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find AU identifier";
      log.error(message, sqle);
      log.error("auId = " + auId);
      log.error("SQL = '" + FIND_AU_BY_AU_ID_QUERY + "'.");
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auSeq = " + auSeq);
    return auSeq;
  }

  /**
   * Provides the identifier of a plugin if existing or after creating it
   * otherwise.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginId
   *          A String with the plugin identifier.
   * @param platformSeq
   *          A Long with the publishing platform identifier.
   * @param isBulkContent
   *          A boolean with the indication of bulk content for the plugin.
   * @return a Long with the identifier of the plugin.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findOrCreatePlugin(Connection conn, String pluginId, Long platformSeq,
      boolean isBulkContent) throws DbException {
    final String DEBUG_HEADER = "findOrCreatePlugin(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pluginId = " + pluginId);
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
      log.debug2(DEBUG_HEADER + "isBulkContent = " + isBulkContent);
    }

    Long pluginSeq = findPlugin(conn, pluginId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginSeq = " + pluginSeq);

    // Check whether it is a new plugin.
    if (pluginSeq == null) {
      // Yes: Add to the database the new plugin.
      pluginSeq = addPlugin(conn, pluginId, platformSeq, isBulkContent);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "new pluginSeq = " + pluginSeq);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "pluginSeq = " + pluginSeq);
    return pluginSeq;
  }

  /**
   * Provides the identifier of a plugin.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginId
   *          A String with the plugin identifier.
   * @return a Long with the identifier of the plugin.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findPlugin(Connection conn, String pluginId) throws DbException {
    final String DEBUG_HEADER = "findPlugin(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "pluginId = " + pluginId);
    Long pluginSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findPlugin =
	dbManager.prepareStatement(conn, FIND_PLUGIN_QUERY);

    try {
      findPlugin.setString(1, pluginId);

      resultSet = dbManager.executeQuery(findPlugin);
      if (resultSet.next()) {
	pluginSeq = resultSet.getLong(PLUGIN_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find plugin";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PLUGIN_QUERY + "'.");
      log.error("pluginId = " + pluginId);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPlugin);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "pluginSeq = " + pluginSeq);
    return pluginSeq;
  }

  /**
   * Adds a plugin to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginId
   *          A String with the plugin identifier.
   * @param platformSeq
   *          A Long with the publishing platform identifier.
   * @param isBulkContent
   *          A boolean with the indication of bulk content for the plugin.
   * @return a Long with the identifier of the plugin just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  private Long addPlugin(Connection conn, String pluginId, Long platformSeq,
      boolean isBulkContent) throws DbException {
    final String DEBUG_HEADER = "addPlugin(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pluginId = " + pluginId);
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
      log.debug2(DEBUG_HEADER + "isBulkContent = " + isBulkContent);
    }

    Long pluginSeq = null;
    ResultSet resultSet = null;

    PreparedStatement insertPlugin = dbManager.prepareStatement(conn,
	INSERT_PLUGIN_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertPlugin.setString(1, pluginId);

      if (platformSeq != null) {
	insertPlugin.setLong(2, platformSeq);
      } else {
	insertPlugin.setNull(2, BIGINT);
      }

      insertPlugin.setBoolean(3, isBulkContent);

      dbManager.executeUpdate(insertPlugin);
      resultSet = insertPlugin.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create plugin table row.");
	return null;
      }

      pluginSeq = resultSet.getLong(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Added pluginSeq = " + pluginSeq);
    } catch (SQLException sqle) {
      String message = "Cannot add plugin";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_PLUGIN_QUERY + "'.");
      log.error("pluginId = " + pluginId);
      log.error("platformSeq = " + platformSeq);
      log.error("isBulkContent = " + isBulkContent);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertPlugin);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "pluginSeq = " + pluginSeq);
    return pluginSeq;
  }

  /**
   * Provides the identifier of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginSeq
   *          A Long with the identifier of the plugin.
   * @param auKey
   *          A String with the Archival Unit key.
   * @return a Long with the identifier of the Archival Unit.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findAu(Connection conn, Long pluginSeq, String auKey)
      throws DbException {
    final String DEBUG_HEADER = "findAu(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pluginSeq = " + pluginSeq);
      log.debug2(DEBUG_HEADER + "auKey = " + auKey);
    }

    ResultSet resultSet = null;
    Long auSeq = null;

    PreparedStatement findAu = dbManager.prepareStatement(conn, FIND_AU_QUERY);

    try {
      findAu.setLong(1, pluginSeq);
      findAu.setString(2, auKey);
      resultSet = dbManager.executeQuery(findAu);
      if (resultSet.next()) {
	auSeq = resultSet.getLong(AU_SEQ_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found auSeq = " + auSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find AU";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_AU_QUERY + "'.");
      log.error("pluginSeq = " + pluginSeq);
      log.error("auKey = " + auKey);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auSeq = " + auSeq);
    return auSeq;
  }

  /**
   * Adds an Archival Unit to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginSeq
   *          A Long with the identifier of the plugin.
   * @param auKey
   *          A String with the Archival Unit key.
   * @return a Long with the identifier of the Archival Unit just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long addAu(Connection conn, Long pluginSeq, String auKey) throws DbException {
    final String DEBUG_HEADER = "addAu(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pluginSeq = " + pluginSeq);
      log.debug2(DEBUG_HEADER + "auKey = " + auKey);
    }

    ResultSet resultSet = null;
    Long auSeq = null;

    PreparedStatement insertAu = dbManager.prepareStatement(conn,
	INSERT_AU_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertAu.setLong(1, pluginSeq);
      insertAu.setString(2, auKey);
      dbManager.executeUpdate(insertAu);
      resultSet = insertAu.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create AU table row for AU key " + auKey);
	return null;
      }

      auSeq = resultSet.getLong(1);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Added auSeq = " + auSeq);
    } catch (SQLException sqle) {
      String message = "Cannot add AU";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_AU_QUERY + "'.");
      log.error("pluginSeq = " + pluginSeq);
      log.error("auKey = " + auKey);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auSeq = " + auSeq);
    return auSeq;
  }

  /**
   * Adds an Archival Unit metadata to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auSeq
   *          A Long with the identifier of the Archival Unit.
   * @param version
   *          An int with the metadata version.
   * @param extractTime
   *          A long with the extraction time of the metadata.
   * @param creationTime
   *          A long with the creation time of the archival unit.
   * @param providerSeq
   *          A Long with the identifier of the Archival Unit provider.
   * @return a Long with the identifier of the Archival Unit metadata just
   *         added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long addAuMd(Connection conn, Long auSeq, int version, long extractTime,
      long creationTime, Long providerSeq) throws DbException {
    final String DEBUG_HEADER = "addAuMd(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auSeq = " + auSeq);
      log.debug2(DEBUG_HEADER + "version = " + version);
      log.debug2(DEBUG_HEADER + "extractTime = " + extractTime);
      log.debug2(DEBUG_HEADER + "creationTime = " + creationTime);
      log.debug2(DEBUG_HEADER + "providerSeq = " + providerSeq);
    }

    ResultSet resultSet = null;
    Long auMdSeq = null;

    PreparedStatement insertAuMd = dbManager.prepareStatement(conn,
	INSERT_AU_MD_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertAuMd.setLong(1, auSeq);
      insertAuMd.setShort(2, (short) version);
      insertAuMd.setLong(3, extractTime);
      insertAuMd.setLong(4, creationTime);
      insertAuMd.setLong(5, providerSeq);
      dbManager.executeUpdate(insertAuMd);
      resultSet = insertAuMd.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create AU_MD table row for auSeq " + auSeq);
	return null;
      }

      auMdSeq = resultSet.getLong(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Added auMdSeq = " + auMdSeq);
    } catch (SQLException sqle) {
      String message = "Cannot add AU metadata";
      log.error(message, sqle);
      log.error("sql = " + INSERT_AU_MD_QUERY);
      log.error("auSeq = " + auSeq);
      log.error("version = " + version);
      log.error("extractTime = " + extractTime);
      log.error("creationTime = " + creationTime);
      log.error("providerSeq = " + providerSeq);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertAuMd);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
    return auMdSeq;
  }

  /**
   * Updates the timestamp of the last extraction of an Archival Unit metadata.
   * 
   * @param au
   *          The ArchivalUnit whose time to update.
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auMdSeq
   *          A Long with the identifier of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void updateAuLastExtractionTime(ArchivalUnit au, Connection conn,
      Long auMdSeq) throws DbException {
    final String DEBUG_HEADER = "updateAuLastExtractionTime(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "au = " + au);
      log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
    }

    long now = TimeBase.nowMs();
    AuUtil.getAuState(au).setLastMetadataIndex(now);

    PreparedStatement updateAuLastExtractionTime =
	dbManager.prepareStatement(conn, UPDATE_AU_MD_EXTRACT_TIME_QUERY);

    try {
      updateAuLastExtractionTime.setLong(1, now);
      updateAuLastExtractionTime.setLong(2, auMdSeq);
      dbManager.executeUpdate(updateAuLastExtractionTime);
    } catch (SQLException sqle) {
      String message = "Cannot update the AU extraction time";
      log.error(message, sqle);
      log.error("au = '" + au + "'.");
      log.error("SQL = '" + UPDATE_AU_MD_EXTRACT_TIME_QUERY + "'.");
      log.error("now = " + now + ".");
      log.error("auMdSeq = '" + auMdSeq + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(updateAuLastExtractionTime);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifier of a publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisher
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findPublisher(Connection conn, String publisher) throws DbException {
    final String DEBUG_HEADER = "findPublisher(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "publisher = " + publisher);

    Long publisherSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findPublisher =
	dbManager.prepareStatement(conn, FIND_PUBLISHER_QUERY);

    try {
      findPublisher.setString(1, publisher);

      resultSet = dbManager.executeQuery(findPublisher);
      if (resultSet.next()) {
	publisherSeq = resultSet.getLong(PUBLISHER_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find publisher";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PUBLISHER_QUERY + "'.");
      log.error("publisher = '" + publisher + "'.");
      throw new DbException(message, sqle);
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
   * @param publisher
   *          A String with the publisher name.
   * @return a Long with the identifier of the publisher just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long addPublisher(Connection conn, String publisher) throws DbException {
    final String DEBUG_HEADER = "addPublisher(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "publisher = " + publisher);

    Long publisherSeq = null;
    ResultSet resultSet = null;
    PreparedStatement insertPublisher = dbManager.prepareStatement(conn,
	INSERT_PUBLISHER_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertPublisher.setString(1, publisher);
      dbManager.executeUpdate(insertPublisher);
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
      log.error("publisher = '" + publisher + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertPublisher);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }

  /**
   * Adds a publication to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param parentMdItemSeq
   *          A Long with the publication parent metadata item parent identifier.
   * @param mdItemType
   *          A String with the type of publication.
   * @param title
   *          A String with the title of the publication.
   * @return a Long with the identifier of the publication just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long addPublication(Connection conn, Long publisherSeq, Long parentMdItemSeq,
      String mdItemType, String title) throws DbException {
    final String DEBUG_HEADER = "addPublication(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "parentMdItemSeq = " + parentMdItemSeq);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
      log.debug2(DEBUG_HEADER + "title = " + title);
    }

    Long publicationSeq = null;

    Long mdItemTypeSeq = findMetadataItemType(conn, mdItemType);
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "mdItemTypeSeq = " + mdItemTypeSeq);

    if (mdItemTypeSeq == null) {
	log.error("Unable to find the metadata item type " + mdItemType);
	return null;
    }

    Long mdItemSeq =
	addMdItem(conn, parentMdItemSeq, mdItemTypeSeq, null, null, null, -1);
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    if (mdItemSeq == null) {
	log.error("Unable to create metadata item table row.");
	return null;
    }

    addMdItemName(conn, mdItemSeq, title, PRIMARY_NAME_TYPE);

    ResultSet resultSet = null;

    PreparedStatement insertPublication = dbManager.prepareStatement(conn,
	INSERT_PUBLICATION_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // skip auto-increment key field #0
      insertPublication.setLong(1, mdItemSeq);
      insertPublication.setLong(2, publisherSeq);
      dbManager.executeUpdate(insertPublication);
      resultSet = insertPublication.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create publication table row.");
	return null;
      }

      publicationSeq = resultSet.getLong(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Added publicationSeq = " + publicationSeq);
    } catch (SQLException sqle) {
      String message = "Cannot insert publication";
      log.error(message, sqle);
      log.error("parentMdItemSeq = " + parentMdItemSeq);
      log.error("mdItemType = " + mdItemType);
      log.error("title = " + title);
      log.error("SQL = '" + INSERT_PUBLICATION_QUERY + "'.");
      log.error("mdItemSeq = '" + mdItemSeq + "'.");
      log.error("publisherSeq = '" + publisherSeq + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertPublication);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides the identifier of the metadata item of a publication.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the identifier of the publication.
   * @return a Long with the identifier of the metadata item of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findPublicationMetadataItem(Connection conn, Long publicationSeq)
      throws DbException {
    final String DEBUG_HEADER = "findPublicationMetadataItem(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    Long mdItemSeq = null;
    PreparedStatement findMdItem =
	dbManager.prepareStatement(conn, FIND_PUBLICATION_METADATA_ITEM_QUERY);
    ResultSet resultSet = null;

    try {
      findMdItem.setLong(1, publicationSeq);

      resultSet = dbManager.executeQuery(findMdItem);
      if (resultSet.next()) {
	mdItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find publication metadata item";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PUBLICATION_METADATA_ITEM_QUERY + "'.");
      log.error("publicationSeq = " + publicationSeq + ".");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItem);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
    return mdItemSeq;
  }

  /**
   * Provides the identifier of the parent of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mditemSeq
   *          A Long with the identifier of the metadata item.
   * @return a Long with the identifier of the parent of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findParentMetadataItem(Connection conn, Long mditemSeq)
      throws DbException {
    final String DEBUG_HEADER = "findParentMetadataItem(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mditemSeq = " + mditemSeq);

    Long mdParentItemSeq = null;
    PreparedStatement findParentMdItem =
        dbManager.prepareStatement(conn, FIND_PARENT_METADATA_ITEM_QUERY);
    ResultSet resultSet = null;

    try {
      findParentMdItem.setLong(1, mditemSeq);

      resultSet = dbManager.executeQuery(findParentMdItem);
      if (resultSet.next()) {
        mdParentItemSeq = resultSet.getLong(MD_ITEM_SEQ_COLUMN);
        if (log.isDebug3())
          log.debug3(DEBUG_HEADER + "mdParentItemSeq = " + mdParentItemSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find parent metadata item";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PARENT_METADATA_ITEM_QUERY + "'.");
      log.error("mditemSeq = " + mditemSeq + ".");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findParentMdItem);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "mdParentItemSeq = " + mdParentItemSeq);
    return mdParentItemSeq;
  }

  /**
   * Adds to the database the ISSNs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param pIssn
   *          A String with the print ISSN of the metadata item.
   * @param eIssn
   *          A String with the electronic ISSN of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemIssns(Connection conn, Long mdItemSeq, String pIssn,
      String eIssn) throws DbException {
    final String DEBUG_HEADER = "addMdItemIssns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
    }

    if (pIssn == null && eIssn == null) {
      return;
    }

    PreparedStatement insertIssn =
	dbManager.prepareStatement(conn, INSERT_ISSN_QUERY);

    try {
      if (pIssn != null) {
	insertIssn.setLong(1, mdItemSeq);
	insertIssn.setString(2, pIssn);
	insertIssn.setString(3, P_ISSN_TYPE);
	int count = dbManager.executeUpdate(insertIssn);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added PISSN = " + pIssn);
	}

	insertIssn.clearParameters();
      }

      if (eIssn != null) {
	insertIssn.setLong(1, mdItemSeq);
	insertIssn.setString(2, eIssn);
	insertIssn.setString(3, E_ISSN_TYPE);
	int count = dbManager.executeUpdate(insertIssn);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added EISSN = " + eIssn);
	}
      }
    } catch (SQLException sqle) {
      String message = "Cannot add metadata item ISSNs";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_ISSN_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("pIssn = " + pIssn);
      log.error("eIssn = " + eIssn);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(insertIssn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds to the database the ISBNs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param pIsbn
   *          A String with the print ISBN of the metadata item.
   * @param eIsbn
   *          A String with the electronic ISBN of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemIsbns(Connection conn, Long mdItemSeq, String pIsbn,
      String eIsbn) throws DbException {
    final String DEBUG_HEADER = "addMdItemIsbns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
    }

    if (pIsbn == null && eIsbn == null) {
      return;
    }

    PreparedStatement insertIsbn =
	dbManager.prepareStatement(conn, INSERT_ISBN_QUERY);

    try {
      if (pIsbn != null) {
	insertIsbn.setLong(1, mdItemSeq);
	insertIsbn.setString(2, pIsbn);
	insertIsbn.setString(3, P_ISBN_TYPE);
	int count = dbManager.executeUpdate(insertIsbn);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added PISBN = " + pIsbn);
	}

	insertIsbn.clearParameters();
      }

      if (eIsbn != null) {
	insertIsbn.setLong(1, mdItemSeq);
	insertIsbn.setString(2, eIsbn);
	insertIsbn.setString(3, E_ISBN_TYPE);
	int count = dbManager.executeUpdate(insertIsbn);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added EISBN = " + eIsbn);
	}
      }
    } catch (SQLException sqle) {
      String message = "Cannot add metadata item ISBNs";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_ISBN_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      log.error("pIssn = " + pIsbn);
      log.error("eIssn = " + eIsbn);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(insertIsbn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the ISSNs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Set<Issn> with the ISSNs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Set<Issn> getMdItemIssns(Connection conn, Long mdItemSeq) throws DbException {
    final String DEBUG_HEADER = "getMdItemIssns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    Set<Issn> issns = new HashSet<Issn>();

    PreparedStatement findIssns =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_ISSN_QUERY);

    ResultSet resultSet = null;
    Issn issn;

    try {
      // Get the metadata item ISSNs.
      findIssns.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findIssns);

      // Loop through the results.
      while (resultSet.next()) {
	// Get the next ISSN.
	issn = new Issn(resultSet.getString(ISSN_COLUMN),
	    resultSet.getString(ISSN_TYPE_COLUMN));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found " + issn);

	// Add it to the results.
	issns.add(issn);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find metadata item ISSNs";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_MD_ITEM_ISSN_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findIssns);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "issns = " + issns);
    return issns;
  }

  /**
   * Provides the proprietary identifiers of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return A Collection<String> with the proprietary identifiers of the
   *         metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Collection<String> getMdItemProprietaryIds(Connection conn, Long mdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "getMdItemProprietaryIds(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    List<String> proprietaryIds = new ArrayList<String>();

    PreparedStatement findMdItemProprietaryId =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_PROPRIETARY_ID_QUERY);

    ResultSet resultSet = null;

    try {
      // Get the existing proprietary identifiers.
      findMdItemProprietaryId.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findMdItemProprietaryId);

      while (resultSet.next()) {
	proprietaryIds.add(resultSet.getString(PROPRIETARY_ID_COLUMN));
      }
    } catch (SQLException sqle) {
      String message =
	  "Cannot get the proprietary identifiers of a metadata item";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_MD_ITEM_PROPRIETARY_ID_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItemProprietaryId);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "proprietaryIds = " + proprietaryIds);
    return proprietaryIds;
  }

  /**
   * Provides the ISBNs of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Set<Isbn> with the ISBNs.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Set<Isbn> getMdItemIsbns(Connection conn, Long mdItemSeq) throws DbException {
    final String DEBUG_HEADER = "getMdItemIsbns(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    Set<Isbn> isbns = new HashSet<Isbn>();

    PreparedStatement findIsbns =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_ISBN_QUERY);

    ResultSet resultSet = null;
    Isbn isbn;

    try {
      // Get the metadata item ISBNs.
      findIsbns.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findIsbns);

      // Loop through the results.
      while (resultSet.next()) {
	// Get the next ISBN.
	isbn = new Isbn(resultSet.getString(ISBN_COLUMN),
	    resultSet.getString(ISBN_TYPE_COLUMN));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "Found " + isbn);

	// Add it to the results.
	isbns.add(isbn);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find metadata item ISBNs";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_MD_ITEM_ISBN_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findIsbns);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "isbns = " + isbns);
    return isbns;
  }

  /**
   * Provides the identifier of a publication by its publisher and ISSNs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIssn
   *          A String with the print ISSN of the publication.
   * @param eIssn
   *          A String with the electronic ISSN of the publication.
   * @param mdItemType
   *          A String with the type of publication to be identified.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findPublicationByIssns(Connection conn, Long publisherSeq, String pIssn,
      String eIssn, String mdItemType) throws DbException {
    final String DEBUG_HEADER = "findPublicationByIssns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "pIssn = " + pIssn);
      log.debug2(DEBUG_HEADER + "eIssn = " + eIssn);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
    }

    Long publicationSeq = null;
    ResultSet resultSet = null;
    PreparedStatement findPublicationByIssns =
	dbManager.prepareStatement(conn, FIND_PUBLICATION_BY_ISSNS_QUERY);

    try {
      findPublicationByIssns.setLong(1, publisherSeq);
      findPublicationByIssns.setString(2, pIssn);
      findPublicationByIssns.setString(3, eIssn);
      findPublicationByIssns.setString(4, mdItemType);

      resultSet = dbManager.executeQuery(findPublicationByIssns);
      if (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find publication";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PUBLICATION_BY_ISSNS_QUERY + "'.");
      log.error("publisherSeq = " + publisherSeq + ".");
      log.error("pIssn = " + pIssn);
      log.error("eIssn = " + eIssn);
      log.error("mdItemType = " + mdItemType);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublicationByIssns);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its publisher and ISBNs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param pIsbn
   *          A String with the print ISBN of the publication.
   * @param eIsbn
   *          A String with the electronic ISBN of the publication.
   * @param mdItemType
   *          A String with the type of publication to be identified.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findPublicationByIsbns(Connection conn, Long publisherSeq, String pIsbn,
      String eIsbn, String mdItemType) throws DbException {
    final String DEBUG_HEADER = "findPublicationByIsbns(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "pIsbn = " + pIsbn);
      log.debug2(DEBUG_HEADER + "eIsbn = " + eIsbn);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
    }

    Long publicationSeq = null;
    ResultSet resultSet = null;
    PreparedStatement findPublicationByIsbns =
	dbManager.prepareStatement(conn, FIND_PUBLICATION_BY_ISBNS_QUERY);

    try {
      findPublicationByIsbns.setLong(1, publisherSeq);
      findPublicationByIsbns.setString(2, pIsbn);
      findPublicationByIsbns.setString(3, eIsbn);
      findPublicationByIsbns.setString(4, mdItemType);

      resultSet = dbManager.executeQuery(findPublicationByIsbns);
      if (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find publication";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PUBLICATION_BY_ISBNS_QUERY + "'.");
      log.error("publisherSeq = " + publisherSeq);
      log.error("pIsbn = " + pIsbn);
      log.error("eIsbn = " + eIsbn);
      log.error("mdItemType = " + mdItemType);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublicationByIsbns);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides the identifier of a publication by its title and publisher.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param title
   *          A String with the title of the publication.
   * @param publisherSeq
   *          A Long with the publisher identifier.
   * @param mdItemType
   *          A String with the type of publication to be identified.
   * @return a Long with the identifier of the publication.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findPublicationByName(Connection conn, Long publisherSeq, String title,
      String mdItemType) throws DbException {
    final String DEBUG_HEADER = "findPublicationByName(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
      log.debug2(DEBUG_HEADER + "title = " + title);
      log.debug2(DEBUG_HEADER + "mdItemType = " + mdItemType);
    }

    Long publicationSeq = null;
    ResultSet resultSet = null;
    PreparedStatement findPublicationByName =
	dbManager.prepareStatement(conn, FIND_PUBLICATION_BY_NAME_QUERY);

    try {
      findPublicationByName.setLong(1, publisherSeq);
      findPublicationByName.setString(2, title);
      findPublicationByName.setString(3, mdItemType);

      resultSet = dbManager.executeQuery(findPublicationByName);
      if (resultSet.next()) {
	publicationSeq = resultSet.getLong(PUBLICATION_SEQ_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find publication";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PUBLICATION_BY_NAME_QUERY + "'.");
      log.error("publisherSeq = '" + publisherSeq + "'.");
      log.error("title = " + title);
      log.error("mdItemType = " + mdItemType);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublicationByName);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);
    return publicationSeq;
  }

  /**
   * Provides an indication of whether a publication has ISBNs in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the publication identifier.
   * @return a boolean with <code>true</code> if the publication has ISBNs,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean publicationHasIsbns(Connection conn, Long publicationSeq)
      throws DbException {
    final String DEBUG_HEADER = "publicationHasIsbns(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    long rowCount = -1;
    ResultSet results = null;
    PreparedStatement countIsbns =
	dbManager.prepareStatement(conn, COUNT_PUBLICATION_ISBNS_QUERY);

    try {
      countIsbns.setLong(1, publicationSeq);

      // Find the ISBNs.
      results = dbManager.executeQuery(countIsbns);
      results.next();
      rowCount = results.getLong(1);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);
    } catch (SQLException sqle) {
      String message = "Cannot count publication ISBNs";
      log.error(message, sqle);
      log.error("SQL = '" + COUNT_PUBLICATION_ISBNS_QUERY + "'.");
      log.error("publicationSeq = " + publicationSeq);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(countIsbns);
    }

    boolean result = rowCount > 0;
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides an indication of whether a publication has ISSNs in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param publicationSeq
   *          A Long with the publication identifier.
   * @return a boolean with <code>true</code> if the publication has ISSNs,
   *         <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean publicationHasIssns(Connection conn, Long publicationSeq)
      throws DbException {
    final String DEBUG_HEADER = "publicationHasIssns(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publicationSeq = " + publicationSeq);

    long rowCount = -1;
    ResultSet results = null;
    PreparedStatement countIssns =
	dbManager.prepareStatement(conn, COUNT_PUBLICATION_ISSNS_QUERY);

    try {
      countIssns.setLong(1, publicationSeq);

      // Find the ISSNs.
      results = dbManager.executeQuery(countIssns);
      results.next();
      rowCount = results.getLong(1);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);
    } catch (SQLException sqle) {
      String message = "Cannot count publication ISSNs";
      log.error(message, sqle);
      log.error("SQL = '" + COUNT_PUBLICATION_ISSNS_QUERY + "'.");
      log.error("publicationSeq = " + publicationSeq);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(countIssns);
    }

    boolean result = rowCount > 0;
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Provides the identifier of a metadata item type by its name.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param typeName
   *          A String with the name of the metadata item type.
   * @return a Long with the identifier of the metadata item type.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findMetadataItemType(Connection conn, String typeName)
      throws DbException {
    final String DEBUG_HEADER = "findMetadataItemType(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "typeName = " + typeName);

    Long mdItemTypeSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findMdItemType =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_TYPE_QUERY);

    try {
      findMdItemType.setString(1, typeName);

      resultSet = dbManager.executeQuery(findMdItemType);
      if (resultSet.next()) {
	mdItemTypeSeq = resultSet.getLong(MD_ITEM_TYPE_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find metadata item type";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_MD_ITEM_TYPE_QUERY + "'.");
      log.error("typeName = '" + typeName + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItemType);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "mdItemTypeSeq = " + mdItemTypeSeq);
    return mdItemTypeSeq;
  }

  /**
   * Adds a metadata item to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param parentSeq
   *          A Long with the metadata item parent identifier.
   * @param auMdSeq
   *          A Long with the identifier of the Archival Unit metadata.
   * @param mdItemTypeSeq
   *          A Long with the identifier of the type of metadata item.
   * @param date
   *          A String with the publication date of the metadata item.
   * @param coverage
   *          A String with the metadata item coverage.
   * @param fetchTime
   *          A long with the fetch time of metadata item.
   * @return a Long with the identifier of the metadata item just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long addMdItem(Connection conn, Long parentSeq, Long mdItemTypeSeq,
      Long auMdSeq, String date, String coverage, long fetchTime)
	  throws DbException {
    final String DEBUG_HEADER = "addMdItem(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "parentSeq = " + parentSeq);
      log.debug2(DEBUG_HEADER + "mdItemTypeSeq = " + mdItemTypeSeq);
      log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
      log.debug2(DEBUG_HEADER + "date = " + date);
      log.debug2(DEBUG_HEADER + "coverage = " + coverage);
      log.debug2(DEBUG_HEADER + "fetchTime = " + fetchTime);
    }

    PreparedStatement insertMdItem = dbManager.prepareStatement(conn,
	INSERT_MD_ITEM_QUERY, Statement.RETURN_GENERATED_KEYS);

    ResultSet resultSet = null;
    Long mdItemSeq = null;

    try {
      // skip auto-increment key field #0
      if (parentSeq != null) {
	insertMdItem.setLong(1, parentSeq);
      } else {
	insertMdItem.setNull(1, BIGINT);
      }
      insertMdItem.setLong(2, mdItemTypeSeq);
      if (auMdSeq != null) {
	insertMdItem.setLong(3, auMdSeq);
      } else {
	insertMdItem.setNull(3, BIGINT);
      }
      insertMdItem.setString(4, date);
      insertMdItem.setString(5, coverage);
      insertMdItem.setLong(6, fetchTime);
      dbManager.executeUpdate(insertMdItem);
      resultSet = insertMdItem.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create metadata item table row.");
	return null;
      }

      mdItemSeq = resultSet.getLong(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Added mdItemSeq = " + mdItemSeq);
    } catch (SQLException sqle) {
      String message = "Cannot insert metadata item";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_MD_ITEM_QUERY + "'.");
      log.error("parentSeq = " + parentSeq + ".");
      log.error("mdItemTypeSeq = " + mdItemTypeSeq + ".");
      log.error("auMdSeq = " + auMdSeq + ".");
      log.error("date = '" + date + "'.");
      log.error("coverage = '" + coverage + "'.");
      log.error("fetchTime = " + fetchTime);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertMdItem);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
    return mdItemSeq;
  }

  /**
   * Provides the names of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Map<String, String> with the names and name types of the metadata
   *         item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Map<String, String> getMdItemNames(Connection conn, Long mdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "getMdItemNames(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    Map<String, String> names = new HashMap<String, String>();
    PreparedStatement getNames =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_NAME_QUERY);
    ResultSet resultSet = null;

    try {
      getNames.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(getNames);
      while (resultSet.next()) {
	names.put(resultSet.getString(NAME_COLUMN),
		  resultSet.getString(NAME_TYPE_COLUMN));
	if (log.isDebug3()) log.debug3(DEBUG_HEADER
	    + "Found metadata item name = '" + resultSet.getString(NAME_COLUMN)
	    + "' of type '" + resultSet.getString(NAME_TYPE_COLUMN) + "'.");
      }
    } catch (SQLException sqle) {
      String message = "Cannot get the names of a metadata item";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_MD_ITEM_NAME_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq + ".");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(getNames);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "names = " + names);
    return names;
  }

  /**
   * Adds a metadata item name to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param name
   *          A String with the name of the metadata item.
   * @param type
   *          A String with the type of name of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemName(Connection conn, Long mdItemSeq, String name, String type)
      throws DbException {
    final String DEBUG_HEADER = "addMdItemName(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "name = " + name);
      log.debug2(DEBUG_HEADER + "type = " + type);
    }

    if (name == null || type == null) {
      return;
    }

    PreparedStatement insertMdItemName =
	dbManager.prepareStatement(conn, INSERT_MD_ITEM_NAME_QUERY);

    try {
      insertMdItemName.setLong(1, mdItemSeq);
      insertMdItemName.setString(2, name);
      insertMdItemName.setString(3, type);
      int count = dbManager.executeUpdate(insertMdItemName);

      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "count = " + count);
	log.debug3(DEBUG_HEADER + "Added metadata item name = " + name);
      }
    } catch (SQLException sqle) {
      String message = "Cannot add a metadata item name";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_MD_ITEM_NAME_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq + ".");
      log.error("name = " + name + ".");
      log.error("type = " + type + ".");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemName);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds to the database a metadata item URL.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param feature
   *          A String with the feature of the metadata item URL.
   * @param url
   *          A String with the metadata item URL.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemUrl(Connection conn, Long mdItemSeq, String feature, String url)
      throws DbException {
    final String DEBUG_HEADER = "addMdItemUrl(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "feature = " + feature);
      log.debug2(DEBUG_HEADER + "url = " + url);
    }

    PreparedStatement insertMdItemUrl =
	dbManager.prepareStatement(conn, INSERT_URL_QUERY);

    try {
      insertMdItemUrl.setLong(1, mdItemSeq);
      insertMdItemUrl.setString(2, feature);
      insertMdItemUrl.setString(3, url);
      int count = dbManager.executeUpdate(insertMdItemUrl);

      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "count = " + count);
	log.debug3(DEBUG_HEADER + "Added URL = " + url);
      }
    } catch (SQLException sqle) {
      String message = "Cannot add a metadata item URL";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_URL_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq + ".");
      log.error("feature = " + feature + ".");
      log.error("url = " + url + ".");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemUrl);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds to the database a metadata item DOI.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param doi
   *          A String with the DOI of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemDoi(Connection conn, Long mdItemSeq, String doi)
      throws DbException {
    final String DEBUG_HEADER = "addMdItemDoi(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "doi = " + doi);
    }

    if (StringUtil.isNullString(doi)) {
      return;
    }

    PreparedStatement insertMdItemDoi =
	dbManager.prepareStatement(conn, INSERT_DOI_QUERY);

    try {
      insertMdItemDoi.setLong(1, mdItemSeq);
      insertMdItemDoi.setString(2, doi);
      int count = dbManager.executeUpdate(insertMdItemDoi);

      if (log.isDebug3()) {
	log.debug3(DEBUG_HEADER + "count = " + count);
	log.debug3(DEBUG_HEADER + "Added DOI = " + doi);
      }
    } catch (SQLException sqle) {
      String message = "Cannot add a metadata item DOI";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_DOI_QUERY + "'.");
      log.error("mdItemSeq = " + mdItemSeq + ".");
      log.error("doi = " + doi + ".");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemDoi);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes an AU with disabled indexing from the table of pending AUs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archiva lUnit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void removeDisabledFromPendingAus(Connection conn, String auId)
      throws DbException {
    final String DEBUG_HEADER = "removeDisabledFromPendingAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    String pluginId = null;
    String auKey = null;
    PreparedStatement deletePendingAu =
	dbManager.prepareStatement(conn, DELETE_DISABLED_PENDING_AU_QUERY);

    try {
      pluginId = PluginManager.pluginIdFromAuId(auId);
      auKey = PluginManager.auKeyFromAuId(auId);
  
      deletePendingAu.setString(1, pluginId);
      deletePendingAu.setString(2, auKey);
      dbManager.executeUpdate(deletePendingAu);
    } catch (SQLException sqle) {
      String message = "Cannot remove disabled AU from pending table";
      log.error(message, sqle);
      log.error("auId = '" + auId + "'.");
      log.error("SQL = '" + DELETE_DISABLED_PENDING_AU_QUERY + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("auKey = '" + auKey + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(deletePendingAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the prepared statement used to insert pending AUs.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a PreparedStatement with the prepared statement used to insert
   *         pending AUs.
   */
  PreparedStatement getInsertPendingAuBatchStatement(Connection conn)
      throws DbException {
    if (dbManager.isTypeMysql()) {
      return dbManager.prepareStatement(conn,
	  INSERT_ENABLED_PENDING_AU_MYSQL_QUERY);
    }

    return dbManager.prepareStatement(conn, INSERT_ENABLED_PENDING_AU_QUERY);
  }

  /**
   * Provides an indication of whether an Archival Unit is pending reindexing.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param pluginId
   *          A String with the plugin identifier.
   * @param auKey
   *          A String with the Archival Unit key.
   * @return a boolean with <code>true</code> if the Archival Unit is pending
   *         reindexing, <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean isAuPending(Connection conn, String pluginId, String auKey)
      throws DbException {
    final String DEBUG_HEADER = "isAuPending(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "pluginId = " + pluginId);
      log.debug2(DEBUG_HEADER + "auKey = " + auKey);
    }

    boolean result = false;
    PreparedStatement selectPendingAu = null;
    ResultSet results = null;

    try {
      selectPendingAu = dbManager.prepareStatement(conn, FIND_PENDING_AU_QUERY);

      // Find the AU in the table.
      selectPendingAu.setString(1, pluginId);
      selectPendingAu.setString(2, auKey);
      results = dbManager.executeQuery(selectPendingAu);
      result = results.next();
    } catch (SQLException sqle) {
      String message = "Cannot find pending AU";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PENDING_AU_QUERY + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("auKey = '" + auKey + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(selectPendingAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Adds an Archival Unit to the batch of Archival Units to be added to the
   * pending Archival Units table in the database.
   * 
   * @param pluginId
   *          A String with the plugin identifier.
   * @param auKey
   *          A String with the Archival Unit key.
   * @param fullReindex
   *          A boolean indicating whether a full reindex of the Archival Unit
   *          is required.
   * @param insertPendingAuBatchStatement
   *          A PreparedStatement with the SQL staement used to add Archival
   *          Units to the pending Archival Units table in the database.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  void addAuToPendingAusBatch(String pluginId, String auKey,
      boolean fullReindex, PreparedStatement insertPendingAuBatchStatement)
	  throws SQLException {
    insertPendingAuBatchStatement.setString(1, pluginId);
    insertPendingAuBatchStatement.setString(2, auKey);
    insertPendingAuBatchStatement.setBoolean(3, fullReindex);
    insertPendingAuBatchStatement.addBatch();
  }

  /**
   * Adds a batch of Archival Units to the pending Archival Units table in the
   * database.
   * 
   * @param insertPendingAuBatchStatement
   *          A PreparedStatement with the SQL staement used to add Archival
   *          Units to the pending Archival Units table in the database.
   * @throws SQLException
   *           if any problem occurred accessing the database.
   */
  void addAuBatchToPendingAus(PreparedStatement insertPendingAuBatchStatement)
      throws SQLException {
    final String DEBUG_HEADER = "addAuBatchToPendingAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");
    insertPendingAuBatchStatement.executeBatch();
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the version of the metadata of an AU stored in the database.
   * 
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return an int with the version of the metadata of the AU stored in the
   *         database.
   */
  int getAuMetadataVersion(ArchivalUnit au) {
    final String DEBUG_HEADER = "getAuMetadataVersion(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "au = " + au);

    int version = UNKNOWN_VERSION;
    Connection conn = null;

    try {
      // Get a connection to the database.
      conn = dbManager.getConnection();

      // Get the version.
      version = getAuMetadataVersion(conn, au);
    } catch (DbException dbe) {
      log.error("Cannot get AU metadata version - Using " + version + ": "
	  + dbe);
      log.error("au = '" + au + "'.");
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "version = " + version);
    return version;
  }


  /**
   * Provides the version of the metadata of an AU stored in the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return an int with the version of the metadata of the AU stored in the
   *         database.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int getAuMetadataVersion(Connection conn, ArchivalUnit au)
      throws DbException {
    final String DEBUG_HEADER = "getAuMetadataVersion(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "au = " + au);

    String pluginId = null;
    String auKey = null;
    int version = UNKNOWN_VERSION;
    PreparedStatement selectMetadataVersion = null;
    ResultSet resultSet = null;

    try {
      String auId = au.getAuId();
      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId() = " + pluginId);

      auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      selectMetadataVersion =
	  dbManager.prepareStatement(conn, FIND_AU_METADATA_VERSION_QUERY);
      selectMetadataVersion.setString(1, pluginId);
      selectMetadataVersion.setString(2, auKey);
      resultSet = dbManager.executeQuery(selectMetadataVersion);

      if (resultSet.next()) {
	version = resultSet.getShort(MD_VERSION_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "version = " + version);
      }
    } catch (SQLException sqle) {
      String message = "Cannot get AU metadata version";
      log.error(message, sqle);
      log.error("au = '" + au + "'.");
      log.error("SQL = '" + FIND_AU_METADATA_VERSION_QUERY + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("auKey = '" + auKey + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(selectMetadataVersion);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "version = " + version);
    return version;
  }

  /**
   * Provides an indication of whether an Archival Unit requires full
   * reindexing.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return an boolean indicating whether the Archival Unit requires full
   *         reindexing.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean needAuFullReindexing(Connection conn, ArchivalUnit au)
      throws DbException {
    final String DEBUG_HEADER = "needAuFullReindexing(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "au = " + au);

    String auId = au.getAuId();
    String pluginId = PluginManager.pluginIdFromAuId(auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId() = " + pluginId);

    String auKey = PluginManager.auKeyFromAuId(auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

    boolean fullReindexing = false;
    PreparedStatement selectFullReindexing = null;
    ResultSet resultSet = null;
  
    try {
      selectFullReindexing =
          dbManager.prepareStatement(conn, FIND_AU_FULL_REINDEXING_BY_AU_QUERY);
      selectFullReindexing.setString(1, pluginId);
      selectFullReindexing.setString(2, auKey);
      resultSet = dbManager.executeQuery(selectFullReindexing);
  
      if (resultSet.next()) {
        fullReindexing = resultSet.getBoolean(FULLY_REINDEX_COLUMN);
        if (log.isDebug3())
          log.debug3(DEBUG_HEADER + "full reindexing = " + fullReindexing);
      }
    } catch (SQLException sqle) {
      String message = "Cannot get AU fully reindexing flag";
      log.error(message, sqle);
      log.error("au = '" + au + "'.");
      log.error("SQL = '" + FIND_AU_FULL_REINDEXING_BY_AU_QUERY + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("auKey = '" + auKey + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(selectFullReindexing);
    }
  
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "fullReindexing = " + fullReindexing);
    return fullReindexing;
  }

  /**
   * Sets whether AU stored in the database requires full reindexing.
   * @param conn
   *          A Connection with the database connection to be used.
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @param fullReindexing the new value of full_reindexing for the AU
   *         in the database
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void updateAuFullReindexing(Connection conn, ArchivalUnit au,
      boolean fullReindexing) throws DbException {
    final String DEBUG_HEADER = "updateAuFullReindexing(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "au = " + au);
      log.debug2(DEBUG_HEADER + "fullReindexing = " + fullReindexing);
    }

    PreparedStatement updateFullReindexing = null;
  
    String auId = au.getAuId();
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

    String pluginId = PluginManager.pluginIdFromAuId(auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId() = " + pluginId);

    String auKey = PluginManager.auKeyFromAuId(auId);
    if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

    try {
      updateFullReindexing =
        dbManager.prepareStatement(conn, UPDATE_AU_FULL_REINDEXING_QUERY);
      updateFullReindexing.setBoolean(1, fullReindexing);
      updateFullReindexing.setString(2, pluginId);
      updateFullReindexing.setString(3, auKey);
      dbManager.executeUpdate(updateFullReindexing);
    } catch (SQLException sqle) {
      String message = "Cannot set AU fully reindex flag";
      log.error(message, sqle);
      log.error("au = '" + au + "'.");
      log.error("SQL = '" + UPDATE_AU_FULL_REINDEXING_QUERY + "'.");
      log.error("fullReindexing = '" + fullReindexing + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("auKey = '" + auKey + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(updateFullReindexing);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the extraction time of an Archival Unit metadata.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auSeq
   *          A Long with the identifier of the Archival Unit.
   * @return a long with the extraction time of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getAuExtractionTime(Connection conn, Long auSeq) throws DbException {
    final String DEBUG_HEADER = "getAuExtractionTime(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auSeq = " + auSeq);

    long timestamp = NEVER_EXTRACTED_EXTRACTION_TIME;
    PreparedStatement selectLastExtractionTime = null;
    ResultSet resultSet = null;

    try {
      selectLastExtractionTime = dbManager.prepareStatement(conn,
	  FIND_AU_MD_EXTRACT_TIME_BY_AUSEQ_QUERY);
      selectLastExtractionTime.setLong(1, auSeq);
      resultSet = dbManager.executeQuery(selectLastExtractionTime);

      if (resultSet.next()) {
	timestamp = resultSet.getLong(EXTRACT_TIME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "timestamp = " + timestamp);
      }
    } catch (SQLException sqle) {
      String message = "Cannot get AU extraction time";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_AU_MD_EXTRACT_TIME_BY_AUSEQ_QUERY + "'.");
      log.error("auSeq = '" + auSeq + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(selectLastExtractionTime);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "timestamp = " + timestamp);
    return timestamp;
  }

  /**
   * Provides the extraction time of an Archival Unit metadata.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param au
   *          An ArchivalUnit with the AU involved.
   * @return a long with the extraction time of the Archival Unit metadata.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long getAuExtractionTime(Connection conn, ArchivalUnit au)
      throws DbException {
    final String DEBUG_HEADER = "getAuExtractionTime(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "au = " + au);

    String pluginId = null;
    String auKey = null;
    long timestamp = NEVER_EXTRACTED_EXTRACTION_TIME;
    PreparedStatement selectLastExtractionTime = null;
    ResultSet resultSet = null;

    try {
      String auId = au.getAuId();
      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId() = " + pluginId);

      auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      selectLastExtractionTime =
	  dbManager.prepareStatement(conn, FIND_AU_MD_EXTRACT_TIME_BY_AU_QUERY);
      selectLastExtractionTime.setString(1, pluginId);
      selectLastExtractionTime.setString(2, auKey);
      resultSet = dbManager.executeQuery(selectLastExtractionTime);

      if (resultSet.next()) {
	timestamp = resultSet.getLong(EXTRACT_TIME_COLUMN);
	if (log.isDebug3())
	  log.debug3(DEBUG_HEADER + "timestamp = " + timestamp);
      }
    } catch (SQLException sqle) {
      String message = "Cannot get AU extraction time";
      log.error(message, sqle);
      log.error("au = '" + au + "'.");
      log.error("SQL = '" + FIND_AU_MD_EXTRACT_TIME_BY_AU_QUERY + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("auKey = '" + auKey + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(selectLastExtractionTime);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "timestamp = " + timestamp);
    return timestamp;
  }

  /**
   * Provides the identifier of a platform.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformName
   *          A String with the platform identifier.
   * @return a Long with the identifier of the platform.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findPlatform(Connection conn, String platformName) throws DbException {
    final String DEBUG_HEADER = "findPlatform(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "platformName = " + platformName);

    Long platformSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findPlatform =
	dbManager.prepareStatement(conn, FIND_PLATFORM_QUERY);

    try {
      findPlatform.setString(1, platformName);

      resultSet = dbManager.executeQuery(findPlatform);
      if (resultSet.next()) {
	platformSeq = resultSet.getLong(PLATFORM_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find platform";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_PLATFORM_QUERY + "'.");
      log.error("platformName = '" + platformName + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPlatform);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
    return platformSeq;
  }

  /**
   * Adds a platform to the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param platformName
   *          A String with the platform name.
   * @return a Long with the identifier of the platform just added.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long addPlatform(Connection conn, String platformName) throws DbException {
    final String DEBUG_HEADER = "addPlatform(): ";
    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "platformName = " + platformName);

    Long platformSeq = null;
    ResultSet resultSet = null;
    PreparedStatement insertPlatform = dbManager.prepareStatement(conn,
	INSERT_PLATFORM_QUERY, Statement.RETURN_GENERATED_KEYS);

    try {
      // Skip auto-increment key field #0
      insertPlatform.setString(1, platformName);
      dbManager.executeUpdate(insertPlatform);
      resultSet = insertPlatform.getGeneratedKeys();

      if (!resultSet.next()) {
	log.error("Unable to create platform table row.");
	return null;
      }

      platformSeq = resultSet.getLong(1);
      if (log.isDebug3())
	log.debug3(DEBUG_HEADER + "Added platformSeq = " + platformSeq);
    } catch (SQLException sqle) {
      String message = "Cannot add platform";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_PLATFORM_QUERY + "'.");
      log.error("platformName = '" + platformName + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(insertPlatform);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "platformSeq = " + platformSeq);
    return platformSeq;
  }

  /**
   * Adds a disabled AU to the list of pending AUs to reindex.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addDisabledAuToPendingAus(Connection conn, String auId)
      throws DbException {
    final String DEBUG_HEADER = "addDisabledAuToPendingAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    String pluginId = null;
    String auKey = null;
    PreparedStatement addPendingAuStatement =
	dbManager.prepareStatement(conn, INSERT_DISABLED_PENDING_AU_QUERY);

    try {
      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      addPendingAuStatement.setString(1, pluginId);
      addPendingAuStatement.setString(2, auKey);
      int count = dbManager.executeUpdate(addPendingAuStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      String message = "Cannot add disabled pending AU";
      log.error(message, sqle);
      log.error("auId = '" + auId + "'.");
      log.error("SQL = '" + INSERT_PLATFORM_QUERY + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("auKey = '" + auKey + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(addPendingAuStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds an AU with failed indexing to the list of pending AUs to reindex.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addFailedIndexingAuToPendingAus(Connection conn, String auId)
      throws DbException {
    final String DEBUG_HEADER = "addFailedIndexingAuToPendingAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    String pluginId = null;
    String auKey = null;
    PreparedStatement addPendingAuStatement =
	dbManager.prepareStatement(conn,
	    INSERT_FAILED_INDEXING_PENDING_AU_QUERY);

    try {
      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      addPendingAuStatement.setString(1, pluginId);
      addPendingAuStatement.setString(2, auKey);
      int count = dbManager.executeUpdate(addPendingAuStatement);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      String message = "Cannot add failed pending AU";
      log.error(message, sqle);
      log.error("auId = '" + auId + "'.");
      log.error("SQL = '" + INSERT_PLATFORM_QUERY + "'.");
      log.error("pluginId = '" + pluginId + "'.");
      log.error("auKey = '" + auKey + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(addPendingAuStatement);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the identifiers of pending Archival Units with a given priority.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param priority
   *          An int with the priority of the requested Archival Units.
   * @return a Collection<String> with the identifiers of pending Archival Units
   *         with the given priority.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Collection<String> findPendingAusWithPriority(Connection conn, int priority)
      throws DbException {
    final String DEBUG_HEADER = "findPendingAusWithPriority(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "priority = " + priority);

    Collection<String> aus = new ArrayList<String>();
    String pluginId;
    String auKey;
    String auId;
    ResultSet results = null;

    PreparedStatement selectAus =
	dbManager.prepareStatement(conn, FIND_PENDING_AUS_WITH_PRIORITY_QUERY);

    try {
      selectAus.setInt(1, priority);
      results = dbManager.executeQuery(selectAus);

      while (results.next()) {
	pluginId = results.getString(PLUGIN_ID_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
	auKey = results.getString(AU_KEY_COLUMN);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);
	auId = PluginManager.generateAuId(pluginId, auKey);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auId = " + auId);

	aus.add(auId);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find pending AUs";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_PLATFORM_QUERY + "'.");
      log.error("priority = '" + priority + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(selectAus);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "aus.size() = " + aus.size());
    return aus;
  }

  /**
   * Provides the identifier of the publisher of an Archival Unit.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auSeq
   *          A Long with the identifier of the Archival Unit.
   * @return a Long with the identifier of the publisher.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Long findAuPublisher(Connection conn, Long auSeq) throws DbException {
    final String DEBUG_HEADER = "findAuPublisher(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auSeq = " + auSeq);

    Long publisherSeq = null;
    ResultSet resultSet = null;

    PreparedStatement findPublisher =
	dbManager.prepareStatement(conn, FIND_AU_PUBLISHER_QUERY);

    try {
      findPublisher.setLong(1, auSeq);

      resultSet = dbManager.executeQuery(findPublisher);
      if (resultSet.next()) {
	publisherSeq = resultSet.getLong(PUBLISHER_SEQ_COLUMN);
      }
    } catch (SQLException sqle) {
      String message = "Cannot find the publisher of an AU";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_AU_PUBLISHER_QUERY + "'.");
      log.error("auSeq = '" + auSeq + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findPublisher);
    }

    if (log.isDebug2())
      log.debug2(DEBUG_HEADER + "publisherSeq = " + publisherSeq);
    return publisherSeq;
  }

  /**
   * Provides the authors of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return a Collection<String> with the authors of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Collection<String> getMdItemAuthors(Connection conn, Long mdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "getMdItemAuthors(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    List<String> authors = new ArrayList<String>();

    PreparedStatement findMdItemAuthor =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_AUTHOR_QUERY);

    ResultSet resultSet = null;

    try {
      // Get the existing authors.
      findMdItemAuthor.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findMdItemAuthor);

      while (resultSet.next()) {
	authors.add(resultSet.getString(AUTHOR_NAME_COLUMN));
      }
    } catch (SQLException sqle) {
      String message = "Cannot get the authors of a metadata item";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_MD_ITEM_AUTHOR_QUERY + "'.");
      log.error("mdItemSeq = '" + mdItemSeq + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItemAuthor);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "authors = " + authors);
    return authors;
  }

  /**
   * Provides the keywords of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @return A Collection<String> with the keywords of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  Collection<String> getMdItemKeywords(Connection conn, Long mdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "getMdItemKeywords(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);

    List<String> keywords = new ArrayList<String>();

    PreparedStatement findMdItemKeyword =
	dbManager.prepareStatement(conn, FIND_MD_ITEM_KEYWORD_QUERY);

    ResultSet resultSet = null;

    try {
      // Get the existing keywords.
      findMdItemKeyword.setLong(1, mdItemSeq);
      resultSet = dbManager.executeQuery(findMdItemKeyword);

      while (resultSet.next()) {
	keywords.add(resultSet.getString(KEYWORD_COLUMN));
      }
    } catch (SQLException sqle) {
      String message = "Cannot get the keywords of a metadata item";
      log.error(message, sqle);
      log.error("SQL = '" + FIND_MD_ITEM_KEYWORD_QUERY + "'.");
      log.error("mdItemSeq = '" + mdItemSeq + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(findMdItemKeyword);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "keywords = " + keywords);
    return keywords;
  }

  /**
   * Adds to the database the authors of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param authors
   *          A Collection<String> with the authors of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemAuthors(Connection conn, Long mdItemSeq,
      Collection<String> authors) throws DbException {
    final String DEBUG_HEADER = "addMdItemAuthors(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "authors = " + authors);
    }

    if (authors == null || authors.size() == 0) {
      return;
    }

    String sql = getInsertMdItemAuthorSql();
    PreparedStatement insertMdItemAuthor =
	dbManager.prepareStatement(conn, sql);

    try {
      for (String author : authors) {
	insertMdItemAuthor.setLong(1, mdItemSeq);
	insertMdItemAuthor.setString(2, author);
	insertMdItemAuthor.setLong(3, mdItemSeq);
	int count = dbManager.executeUpdate(insertMdItemAuthor);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added author = " + author);
	}
      }
    } catch (SQLException sqle) {
      String message = "Cannot add metadata item authors";
      log.error(message, sqle);
      log.error("SQL = '" + sql + "'.");
      log.error("mdItemSeq = '" + mdItemSeq + "'.");
      log.error("authors = " + authors + ".");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemAuthor);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the SQL query used to insert a metadata item author.
   * 
   * @return a String with the SQL query used to insert a metadata item author.
   */
  private String getInsertMdItemAuthorSql() {
    if (dbManager.isTypeMysql()) {
      return INSERT_AUTHOR_MYSQL_QUERY;
    }

    return INSERT_AUTHOR_QUERY;
  }

  /**
   * Adds to the database the keywords of a metadata item.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param mdItemSeq
   *          A Long with the metadata item identifier.
   * @param keywords
   *          A Collection<String> with the keywords of the metadata item.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void addMdItemKeywords(Connection conn, Long mdItemSeq,
      Collection<String> keywords) throws DbException {
    final String DEBUG_HEADER = "addMdItemKeywords(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
      log.debug2(DEBUG_HEADER + "keywords = " + keywords);
    }

    if (keywords == null || keywords.size() == 0) {
      return;
    }

    PreparedStatement insertMdItemKeyword =
	dbManager.prepareStatement(conn, INSERT_KEYWORD_QUERY);

    try {
      for (String keyword : keywords) {
	insertMdItemKeyword.setLong(1, mdItemSeq);
	insertMdItemKeyword.setString(2, keyword);
	int count = dbManager.executeUpdate(insertMdItemKeyword);

	if (log.isDebug3()) {
	  log.debug3(DEBUG_HEADER + "count = " + count);
	  log.debug3(DEBUG_HEADER + "Added keyword = " + keyword);
	}
      }
    } catch (SQLException sqle) {
      String message = "Cannot add metadata item keywords";
      log.error(message, sqle);
      log.error("SQL = '" + INSERT_KEYWORD_QUERY + "'.");
      log.error("mdItemSeq = '" + mdItemSeq + "'.");
      log.error("keywords = " + keywords + ".");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseStatement(insertMdItemKeyword);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Adds an Archival Unit to the table of unconfigured Archival Units.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void persistUnconfiguredAu(Connection conn, String auId) throws DbException {
    final String DEBUG_HEADER = "persistUnconfiguredAu(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    PreparedStatement insertUnconfiguredAu = null;
    String pluginId = null;
    String auKey = null;

    try {
      insertUnconfiguredAu =
	  dbManager.prepareStatement(conn, INSERT_UNCONFIGURED_AU_QUERY);

      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      insertUnconfiguredAu.setString(1, pluginId);
      insertUnconfiguredAu.setString(2, auKey);
      int count = dbManager.executeUpdate(insertUnconfiguredAu);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
    } catch (SQLException sqle) {
      String message = "Cannot insert archival unit in unconfigured table";
      log.error(message, sqle);
      log.error("auId = " + auId);
      log.error("SQL = '" + INSERT_UNCONFIGURED_AU_QUERY + "'.");
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      throw new DbException(message, sqle);
    } catch (DbException dbe) {
      String message = "Cannot insert archival unit in unconfigured table";
      log.error(message, dbe);
      log.error("auId = " + auId);
      log.error("SQL = '" + INSERT_UNCONFIGURED_AU_QUERY + "'.");
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      throw dbe;
    } finally {
      DbManager.safeCloseStatement(insertUnconfiguredAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Removes an Archival Unit from the table of unconfigured Archival Units.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the AU identifier.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  void removeFromUnconfiguredAus(Connection conn, String auId) {
    final String DEBUG_HEADER = "removeFromUnconfiguredAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    PreparedStatement deleteUnconfiguredAu = null;
    String pluginId = null;
    String auKey = null;

    try {
      if (isAuInUnconfiguredAuTable(conn, auId)) {
	deleteUnconfiguredAu =
	    dbManager.prepareStatement(conn, DELETE_UNCONFIGURED_AU_QUERY);

	pluginId = PluginManager.pluginIdFromAuId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
	auKey = PluginManager.auKeyFromAuId(auId);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

	deleteUnconfiguredAu.setString(1, pluginId);
	deleteUnconfiguredAu.setString(2, auKey);
	int count = dbManager.executeUpdate(deleteUnconfiguredAu);
	if (log.isDebug3()) log.debug3(DEBUG_HEADER + "count = " + count);
	DbManager.commitOrRollback(conn, log);
      }
    } catch (SQLException sqle) {
      String message = "Cannot delete archival unit from unconfigured table";
      log.error(message, sqle);
      log.error("auId = " + auId);
      log.error("SQL = '" + DELETE_UNCONFIGURED_AU_QUERY + "'.");
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
    } catch (DbException dbe) {
      String message = "Cannot delete archival unit from unconfigured table";
      log.error(message, dbe);
      log.error("auId = " + auId);
      log.error("SQL = '" + DELETE_UNCONFIGURED_AU_QUERY + "'.");
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
    } finally {
      DbManager.safeCloseStatement(deleteUnconfiguredAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Done.");
  }

  /**
   * Provides the count of recorded unconfigured archival units.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @return a long with the count of recorded unconfigured archival units.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  long countUnconfiguredAus(Connection conn) throws DbException {
    final String DEBUG_HEADER = "countUnconfiguredAus(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "Starting...");

    long rowCount = -1;
    ResultSet results = null;
    PreparedStatement unconfiguredAu =
	dbManager.prepareStatement(conn, UNCONFIGURED_AU_COUNT_QUERY);

    try {
      // Count the rows in the table.
      results = dbManager.executeQuery(unconfiguredAu);
      results.next();
      rowCount = results.getLong(1);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);
    } catch (SQLException sqle) {
      String message = "Cannot count unconfigured archival units";
      log.error(message, sqle);
      log.error("SQL = '" + UNCONFIGURED_AU_COUNT_QUERY + "'.");
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(unconfiguredAu);
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "rowCount = " + rowCount);
    return rowCount;
  }

  /**
   * Provides an indication of whether an Archival Unit is in the table of
   * unconfigured Archival Units.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auId
   *          A String with the Archival Unit identifier.
   * @return a boolean with <code>true</code> if the Archival Unit is in the
   *         UNCONFIGURED_AU table, <code>false</code> otherwise.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  boolean isAuInUnconfiguredAuTable(Connection conn, String auId)
      throws DbException {
    final String DEBUG_HEADER = "isAuInUnconfiguredAuTable(): ";
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "auId = " + auId);

    String pluginId = null;
    String auKey = null;
    long rowCount = -1;
    ResultSet results = null;
    PreparedStatement unconfiguredAu =
	dbManager.prepareStatement(conn, FIND_UNCONFIGURED_AU_COUNT_QUERY);

    try {
      pluginId = PluginManager.pluginIdFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "pluginId = " + pluginId);
      auKey = PluginManager.auKeyFromAuId(auId);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "auKey = " + auKey);

      unconfiguredAu.setString(1, pluginId);
      unconfiguredAu.setString(2, auKey);

      // Find the archival unit in the table.
      results = dbManager.executeQuery(unconfiguredAu);
      results.next();
      rowCount = results.getLong(1);
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "rowCount = " + rowCount);
    } catch (SQLException sqle) {
      String message = "Cannot find archival unit in unconfigured table";
      log.error(message, sqle);
      log.error("auId = " + auId);
      log.error("SQL = '" + FIND_UNCONFIGURED_AU_COUNT_QUERY + "'.");
      log.error("pluginId = " + pluginId);
      log.error("auKey = " + auKey);
      throw new DbException(message, sqle);
    } finally {
      DbManager.safeCloseResultSet(results);
      DbManager.safeCloseStatement(unconfiguredAu);
    }

    boolean result = rowCount > 0;
    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "result = " + result);
    return result;
  }

  /**
   * Removes an Archival Unit child metadata item from the database.
   * 
   * @param conn
   *          A Connection with the database connection to be used.
   * @param auMdSeq
   *          A Long with the identifier of the Archival Unit metadata.
   * @param mdItemSeq
   *          A Long with the metadata identifier.
   * @return an int with the number of metadata items deleted.
   * @throws DbException
   *           if any problem occurred accessing the database.
   */
  int removeAuChildMetadataItem(Connection conn, Long auMdSeq, Long mdItemSeq)
      throws DbException {
    final String DEBUG_HEADER = "removeAuChildMetadataItem(): ";
    if (log.isDebug2()) {
      log.debug2(DEBUG_HEADER + "auMdSeq = " + auMdSeq);
      log.debug2(DEBUG_HEADER + "mdItemSeq = " + mdItemSeq);
    }

    int count = 0;

    // Do nothing if any of the parameters are null.
    if (auMdSeq != null && mdItemSeq != null) {
      PreparedStatement deleteMetadataItem =
	  dbManager.prepareStatement(conn, DELETE_AU_CHILD_MD_ITEM_QUERY);

      try {
	deleteMetadataItem.setLong(1, auMdSeq);
	deleteMetadataItem.setLong(2, mdItemSeq);
	count = dbManager.executeUpdate(deleteMetadataItem);
      } catch (SQLException sqle) {
	String message = "Cannot delete child metadata item";
	log.error(message, sqle);
	log.error("mdItemSeq = " + mdItemSeq);
	log.error("SQL = '" + DELETE_AU_CHILD_MD_ITEM_QUERY + "'.");
	throw new DbException(message, sqle);
      } finally {
	DbManager.safeCloseStatement(deleteMetadataItem);
      }
    }

    if (log.isDebug2()) log.debug2(DEBUG_HEADER + "count = " + count);
    return count;
  }
}
