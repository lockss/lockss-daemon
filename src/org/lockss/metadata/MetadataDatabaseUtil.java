/*
 * $Id: MetadataDatabaseUtil.java,v 1.12 2013-03-31 23:55:37 pgust Exp $
 */

/*

 Copyright (c) 2012 Board of Trustees of Leland Stanford Jr. University,
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
import java.util.ArrayList;
import java.util.List;
import org.lockss.app.LockssDaemon;
import org.lockss.db.DbManager;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.util.Logger;
import org.lockss.util.MetadataUtil;

/**
 * This class contains a set of static methods for returning metadata database
 * records as BibliobraphicItem records.
 * 
 * @author phil
 * 
 */
final public class MetadataDatabaseUtil {
  protected static Logger log = Logger.getLogger(MetadataDatabaseUtil.class);

  private MetadataDatabaseUtil() {
  }

  /**
   * Get the current LOCKSS daemon.
   * 
   * @return the LOCKSS daemon.
   */
  static private LockssDaemon getDaemon() {
    return LockssDaemon.getLockssDaemon();
  }

  /**
   * This class implements a BibliographicItem from a metadata database query.
   * 
   * @author Philip Gust
   * 
   */
  static class BibliographicDatabaseItem implements BibliographicItem {
    final String publisher;
    final String title;
    final String proprietaryId;
    final String printisbn;
    final String eisbn;
    final String printissn;
    final String eissn;
    final String startyear;
    final String endyear;
    final String startvolume;
    final String endvolume;

    /**
     * Creates an instance from the current query result set record.
     * 
     * @param resultSet
     *          the query result set
     * @throws SQLException
     *           if any problem occurred accessing the database.
     */
    public BibliographicDatabaseItem(ResultSet resultSet) throws SQLException {
      publisher = resultSet.getString(1);
      title = resultSet.getString(2);
      printissn = MetadataUtil.formatIssn(resultSet.getString(3));
      eissn = MetadataUtil.formatIssn(resultSet.getString(4));
      printisbn = MetadataUtil.formatIssn(resultSet.getString(5));
      eisbn = MetadataUtil.formatIssn(resultSet.getString(6));
      startvolume = resultSet.getString(7);
      endvolume = resultSet.getString(7);
      startyear = resultSet.getString(8);
      endyear = resultSet.getString(8);
      proprietaryId = resultSet.getString(9);
    }

    @Override
    public String getIsbn() {
      return (printisbn != null) ? printisbn : eisbn;
    }

    @Override
    public String getPrintIsbn() {
      return printisbn;
    }

    @Override
    public String getEisbn() {
      return eisbn;
    }

    @Override
    public String getIssn() {
      return (printissn != null) ? printissn : eissn;
    }

    @Override
    public String getPrintIssn() {
      return printissn;
    }

    @Override
    public String getEissn() {
      return eissn;
    }

    @Override
    public String getIssnL() {
      return null;
    }

    @Override
    public String getProprietaryId() {
      return proprietaryId;
    }

    @Override
    public String getJournalTitle() {
      return title;
    }

    @Override
    public String getPublisherName() {
      return publisher;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getVolume() {
      if (startvolume == null) {
	return null;
      } else if (endvolume == null) {
	return endvolume;
      }
      return startvolume + "-" + endvolume;
    }

    @Override
    public String getYear() {
      if (startyear == null) {
	return null;
      } else if (endyear == null) {
	return endyear;
      }
      return startyear + "-" + endyear;
    }

    @Override
    public String getIssue() {
      return null;
    }

    @Override
    public String getStartVolume() {
      return startvolume;
    }

    @Override
    public String getEndVolume() {
      return endvolume;
    }

    @Override
    public String getStartYear() {
      return startyear;
    }

    @Override
    public String getEndYear() {
      return endyear;
    }

    @Override
    public String getStartIssue() {
      return null;
    }

    @Override
    public String getEndIssue() {
      return null;
    }
  }

  /**
   * This query generates a table of publication_seq keys and their
   * corresponding publication names, p_issns, e_issns, p_isbns, and e_isbns.
   * It can be embedded in other queries to provide item-level lists that
   * include this information. 
   * <p>
   * Here is the original SQL query:
   *<pre>
select
  publication_seq,
  (select name from md_item_name
   where
     md_item_seq = publication.md_item_seq and
     name_type = 'primary') publication_name,
  (select formatissn(issn) from issn
   where
     md_item_seq = publication.md_item_seq and
     issn.issn_type = 'p_issn') p_issn,
  (select formatissn(issn) from issn
   where 
     md_item_seq = publication.md_item_seq and
     issn.issn_type = 'e_issn') e_issn,
  (select formatisbn(isbn) from isbn
   where 
     md_item_seq = publication.md_item_seq and
     isbn.isbn_type = 'p_isbn') p_isbn,
  (select formatisbn(isbn) from isbn
   where 
     md_item_seq = publication.md_item_seq and
     isbn.isbn_type = 'e_isbn') e_isbn
from
  publisher, publication, md_item
where
  publisher.publisher_seq = publication.publisher_seq and
  publication.md_item_seq = md_item.md_item_seq;
   *</pre>
   */
  static final String PUBINFO_QUERY =
        "select "
      +    PUBLICATION_SEQ_COLUMN + ", "
      +   "(select " + NAME_COLUMN + " from " + MD_ITEM_NAME_TABLE
      +   " where " 
      +      MD_ITEM_SEQ_COLUMN 
      +      " = " + PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN + " and "
      +      NAME_TYPE_COLUMN
      +      " = '" + PRIMARY_NAME_TYPE + "') publication_name, "
      +   "(select formatissn(" + ISSN_COLUMN + ") from " + ISSN_TABLE
      +   " where " 
      +      MD_ITEM_SEQ_COLUMN 
      +      " = " + PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN + " and "
      +      ISSN_TABLE + "." + ISSN_TYPE_COLUMN
      +      " = 'p_issn') p_issn, "
      +   "(select formatissn(" + ISSN_COLUMN + ") from " + ISSN_TABLE
      +   " where " 
      +      MD_ITEM_SEQ_COLUMN 
      +      " = " + PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN + " and "
      +      ISSN_TABLE + "." + ISSN_TYPE_COLUMN
      +      " = 'e_issn') e_issn, "
      +   "(select formatisbn(" + ISBN_COLUMN + ") from " + ISBN_TABLE
      +   " where " 
      +      MD_ITEM_SEQ_COLUMN 
      +      " = " + PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN + " and "
      +      ISBN_TABLE + "." + ISBN_TYPE_COLUMN
      +      " = 'p_isbn') p_isbn, "
      +   "(select formatisbn(" + ISBN_COLUMN + ") from " + ISBN_TABLE
      +   " where " 
      +      MD_ITEM_SEQ_COLUMN 
      +      " = " + PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN + " and "
      +      ISBN_TABLE + "." + ISBN_TYPE_COLUMN
      +      " = 'e_isbn') e_isbn "
      + "from " 
      +   PUBLISHER_TABLE + ", " + PUBLICATION_TABLE + ", " + MD_ITEM_TABLE
      + " where "
      +   PUBLISHER_TABLE + "." + PUBLISHER_SEQ_COLUMN
      +   " = " + PUBLICATION_TABLE + "." + PUBLISHER_SEQ_COLUMN + " and "
      +   PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN
      +   " = " + MD_ITEM_TABLE + "." + MD_ITEM_SEQ_COLUMN;
          
  /**
   * This query generates a table of publisher_names, publication_names, 
   * p_issns, e_issns, p_isbns, e_isbns, volumes, and years. The query
   * incorporates PUBINFO_QUERY as an additional table for accessing
   * publication level values.
   * <p>
   * Here is the original SQL query:
   *<pre>
select distinct
  publisher_name, 
  publication_name, 
  p_issn, e_issn, p_isbn, e_isbn, 
  volume, substr(date,1,4) "year", publication_id
from
  bib_item, 
  md_item, 
  publication, 
  publisher, 
  (select
     publication_seq,
     (select name from md_item_name
      where
        md_item_seq = publication.md_item_seq and
        name_type = 'primary') publication_name,
     (select formatissn(issn) from issn
      where
        md_item_seq = publication.md_item_seq and
        issn.issn_type = 'p_issn') p_issn,
     (select formatissn(issn) from issn
      where 
        md_item_seq = publication.md_item_seq and
        issn.issn_type = 'e_issn') e_issn,
     (select formatisbn(isbn) from isbn
      where 
        md_item_seq = publication.md_item_seq and
        isbn.isbn_type = 'p_isbn') p_isbn,
     (select formatisbn(isbn) from isbn
      where 
        md_item_seq = publication.md_item_seq and
        isbn.isbn_type = 'e_isbn') e_isbn
   from
     publisher, publication, md_item
   where
     publisher.publisher_seq = publication.publisher_seq and
     publication.md_item_seq = md_item.md_item_seq
  ) pubinfo
where
  bib_item.md_item_seq = md_item.md_item_seq and
  publication.md_item_seq = md_item.parent_seq and
  publication.publisher_seq = publisher.publisher_seq and
  pubinfo.publication_seq = publication.publication_seq;   *</pre>
   */
  static final String bibliographicItemsQuery =
        "select distinct "
      +   "publisher_name, publication_name, "
      +   "p_issn, e_issn, p_isbn, e_isbn, "
      +   "volume, substr(" + DATE_COLUMN + ",1,4) \"year\", "
      +   "publication_id "
      + "from "
      +    BIB_ITEM_TABLE + ", "
      +    MD_ITEM_TABLE + ", "
      +    PUBLICATION_TABLE + ", " 
      +    PUBLISHER_TABLE + ","
      +    "(" + PUBINFO_QUERY + ") pubinfo "
      + "where "
      +   BIB_ITEM_TABLE + "." + MD_ITEM_SEQ_COLUMN 
      +     " = " + MD_ITEM_TABLE + "." + MD_ITEM_SEQ_COLUMN + " and "
      +   PUBLICATION_TABLE + "." + MD_ITEM_SEQ_COLUMN
      +     " = " + MD_ITEM_TABLE + "." + PARENT_SEQ_COLUMN + " and "
      +   PUBLICATION_TABLE + "." + PUBLISHER_SEQ_COLUMN
      +     " = " + PUBLISHER_TABLE + "." + PUBLISHER_SEQ_COLUMN + " and "
      +   "pubinfo." + PUBLICATION_SEQ_COLUMN
      +     " = " + PUBLICATION_TABLE + "." + PUBLICATION_SEQ_COLUMN;

  /**
   * Returns a list of BibliographicItems from the metadata database.
   * 
   * @return a list of BibliobraphicItems from the metadata database.
   */
  static public List<BibliographicItem> getBibliographicItems() {
    List<BibliographicItem> items = new ArrayList<BibliographicItem>();
    Connection conn = null;
    DbManager dbManager = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    
    try {
      dbManager = getDaemon().getDbManager();
      conn = dbManager.getConnection();
      statement = dbManager.prepareStatement(conn, bibliographicItemsQuery);
      resultSet = dbManager.executeQuery(statement);

      while (resultSet.next()) {
	BibliographicItem item = new BibliographicDatabaseItem(resultSet);
	items.add(item);
      }
    } catch (IllegalArgumentException ex) {
      log.warning(ex.getMessage());
    } catch (SQLException ex) {
      log.warning(ex.getMessage());
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
      DbManager.safeRollbackAndClose(conn);
    }
    return items;
  }
}
