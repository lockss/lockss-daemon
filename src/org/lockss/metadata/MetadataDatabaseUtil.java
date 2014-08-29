/*
 * $Id: MetadataDatabaseUtil.java,v 1.21 2014-08-29 20:45:21 pgust Exp $
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.lockss.app.LockssDaemon;
import org.lockss.db.DbException;
import org.lockss.db.DbManager;
import org.lockss.exporter.biblio.BibliographicItem;
import org.lockss.exporter.biblio.BibliographicUtil;
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
    final String seriesTitle;
    final String proprietarySeriesId;
    final String publicationTitle;
    final String proprietaryId;
    final String printisbn;
    final String eisbn;
    final String printissn;
    final String eissn;
    final String year;
    final String volume;
    final String publicationType;
    final String coverageDepth;

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
      seriesTitle = resultSet.getString(2);
      proprietarySeriesId = resultSet.getString(3);
      publicationTitle = resultSet.getString(4);
      proprietaryId = resultSet.getString(5);
      eissn = MetadataUtil.formatIssn(resultSet.getString(6));
      printissn = MetadataUtil.formatIssn(resultSet.getString(7));
      eisbn = MetadataUtil.formatIsbn(resultSet.getString(8));
      printisbn = MetadataUtil.formatIsbn(resultSet.getString(9));
      year = resultSet.getString(10);
      volume = resultSet.getString(11);
      coverageDepth = resultSet.getString(12);
      publicationType = resultSet.getString(13);
    }

    @Override
    public String getPublicationType() {
      if (publicationType == null) {
        return "journal";
      }
      return (publicationType.equals("book_series")) 
          ? "bookSeries" : publicationType;
    }

    @Override
    public String getCoverageDepth() {
      return (coverageDepth == null) ? "fulltext" : coverageDepth;
    }

    @Override
    public String getIsbn() {
      String isbn = getPrintIsbn();
      if (!MetadataUtil.isIsbn(isbn)) {
        isbn = getEisbn();
        if (!MetadataUtil.isIssn(isbn)) {
          isbn = null;
        }
      }
      return isbn;
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
      String issn = getIssnL();
      if (!MetadataUtil.isIssn(issn)) {
        issn = getPrintIssn();
        if (!MetadataUtil.isIssn(issn)) {
          issn = getEissn();
          if (!MetadataUtil.isIssn(issn)) {
            issn = null;
          }
        }
      }
      return issn;
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
    public String getProprietarySeriesId() {
      return proprietarySeriesId;
    }

    @Override
    public String getPublicationTitle() {
      return publicationTitle;
    }

    @Override
    public String getSeriesTitle() {
      return seriesTitle;
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
      return volume;
    }

    @Override
    public String getYear() {
      return year;
    }

    @Override
    public String getIssue() {
      return null;
    }

    @Override
    public String getStartVolume() {
      return BibliographicUtil.getRangeSetStart(getVolume());
    }

    @Override
    public String getEndVolume() {
      return BibliographicUtil.getRangeSetEnd(getVolume());
    }

    @Override
    public String getStartYear() {
      return BibliographicUtil.getRangeSetStart(getYear());
    }

    @Override
    public String getEndYear() {
      return BibliographicUtil.getRangeSetEnd(getYear());
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
   * This query generates a table of publisher_names, publication_names, 
   * p_issns, e_issns, p_isbns, e_isbns, volumes, and years. The query
   * incorporates PUBINFO_QUERY as an additional table for accessing
   * publication level values.
   * <p>
   * Here is the original SQL query:
   *<pre>
select distinct
    p.publisher_name as publisher
  , series_title.name as series_title
  , pn0.publication_id series_proprietary_id
  , title.name as title
  , pn1.publication_id as proprietery_id
  , coalesce(e_issn0.issn,e_issn1.issn) as e_issn
  , coalesce(p_issn0.issn,p_issn1.issn) as p_issn
  , e_isbn.isbn as e_isbn
  , p_isbn.isbn as p_isbn
  , substr(mi2.date, 1, 4) as published_year
  , b.volume as volume
  , mi2.coverage as coverage_depth
  , coalesce(mit0.type_name,mit1.type_name) as publication_type
from 
    publisher p
  , publication pn1
  , md_item_name title
  , md_item mi1
      left join issn e_issn1
        on mi1.md_item_seq = e_issn1.md_item_seq and e_issn1.issn_type = 'e_issn' 
      left join issn p_issn1
        on mi1.md_item_seq = p_issn1.md_item_seq and p_issn1.issn_type = 'p_issn'
      left join isbn e_isbn
        on mi1.md_item_seq = e_isbn.md_item_seq and e_isbn.isbn_type = 'e_isbn'
      left join isbn p_isbn
        on mi1.md_item_seq = p_isbn.md_item_seq and p_isbn.isbn_type = 'p_isbn'
      left join md_item mi0
        on mi0.md_item_seq = mi1.parent_seq
      left join md_item_name series_title
        on mi0.md_item_seq = series_title.md_item_seq and series_title.name_type = 'primary'
      left join publication pn0
        on mi0.md_item_seq = pn0.md_item_seq
      left join md_item_type mit0
        on mi0.md_item_type_seq = mit0.md_item_type_seq
      left join issn e_issn0
        on mi0.md_item_seq = e_issn0.md_item_seq and e_issn0.issn_type = 'e_issn' 
      left join issn p_issn0
        on mi0.md_item_seq = p_issn0.md_item_seq and p_issn0.issn_type = 'p_issn'
      left join md_item mi2
        on mi2.parent_seq = mi1.md_item_seq
      left join bib_item b
        on mi2.md_item_seq = b.md_item_seq
  , md_item_type mit1
where 
      p.publisher_seq = pn1.publisher_seq
  and pn1.md_item_seq = mi1.md_item_seq
  and mi1.md_item_type_seq = mit1.md_item_type_seq
  and mit1.type_name in ('journal','book')"
  and mi1.md_item_seq = title.md_item_seq
  and title.name_type = 'primary'
   *</pre>
   */
  static final String bibliographicItemsQuery =
      "select distinct "
    + "    p.publisher_name as publisher "
    + "  , series_title.name as series_title "
    + "  , pn0.publication_id series_proprietary_id "
    + "  , title.name as title "
    + "  , pn1.publication_id as proprietery_id "
    + "  , coalesce(e_issn0.issn,e_issn1.issn) as e_issn "
    + "  , coalesce(p_issn0.issn,p_issn1.issn) as p_issn "
    + "  , e_isbn.isbn as e_isbn "
    + "  , p_isbn.isbn as p_isbn "
    + "  , substr(mi2.date, 1, 4) as published_year "
    + "  , b.volume as volume "
    + "  , mi2.coverage as coverage_depth "
    + "  , coalesce(mit0.type_name,mit1.type_name) as publication_type "
    + "from "
    + "    publisher p "
    + "  , publication pn1 "
    + "  , md_item_name title "
    + "  , md_item mi1 "
    + "      left join issn e_issn1 "
    + "        on mi1.md_item_seq = e_issn1.md_item_seq and e_issn1.issn_type = 'e_issn' " 
    + "      left join issn p_issn1 "
    + "        on mi1.md_item_seq = p_issn1.md_item_seq and p_issn1.issn_type = 'p_issn' "
    + "      left join isbn e_isbn "
    + "        on mi1.md_item_seq = e_isbn.md_item_seq and e_isbn.isbn_type = 'e_isbn' "
    + "      left join isbn p_isbn "
    + "        on mi1.md_item_seq = p_isbn.md_item_seq and p_isbn.isbn_type = 'p_isbn' "
    + "      left join md_item mi0 "
    + "        on mi0.md_item_seq = mi1.parent_seq "
    + "      left join md_item_name series_title "
    + "        on mi0.md_item_seq = series_title.md_item_seq and series_title.name_type = 'primary' "
    + "      left join publication pn0 "
    + "        on mi0.md_item_seq = pn0.md_item_seq "
    + "      left join md_item_type mit0 "
    + "        on mi0.md_item_type_seq = mit0.md_item_type_seq "
    + "      left join issn e_issn0 "
    + "        on mi0.md_item_seq = e_issn0.md_item_seq and e_issn0.issn_type = 'e_issn' " 
    + "      left join issn p_issn0 "
    + "        on mi0.md_item_seq = p_issn0.md_item_seq and p_issn0.issn_type = 'p_issn' "
    + "      left join md_item mi2 "
    + "        on mi2.parent_seq = mi1.md_item_seq "
    + "      left join bib_item b "
    + "        on mi2.md_item_seq = b.md_item_seq "
    + "  , md_item_type mit1 "
    + "where "
    + "      p.publisher_seq = pn1.publisher_seq "
    + "  and pn1.md_item_seq = mi1.md_item_seq "
    + "  and mi1.md_item_type_seq = mit1.md_item_type_seq "
    + "  and mit1.type_name in ('journal','book')"
    + "  and mi1.md_item_seq = title.md_item_seq "
    + "  and title.name_type = 'primary'";
          

  /**
   * Returns a list of BibliographicItems from the metadata database.
   * 
   * @return a list of BibliobraphicItems from the metadata database.
   */
  static public List<BibliographicItem> getBibliographicItems() {
    List<BibliographicItem> items = Collections.<BibliographicItem>emptyList();
    Connection conn = null;
    try {
      DbManager dbManager = getDaemon().getDbManager();
      conn = dbManager.getConnection();
      items = getBibliographicItems(dbManager, conn);
    } catch (DbException ex) {
      log.warning(ex.getMessage());
      log.warning("bibliographicItemsQuery = " + bibliographicItemsQuery);
    } finally {
      DbManager.safeRollbackAndClose(conn);
    }
    return items;
  }
  
  /**
   * Returns a list of BibliographicItems from the metadata database.
   * @param dbManager the database manager
   * @param conn the database connection 
   * @return a list of BibliobraphicItems from the metadata database.
   */
  static public List<BibliographicItem> getBibliographicItems(
      DbManager dbManager, Connection conn) {
    final String DEBUG_HEADER = "getBibliographicItems(): ";
    List<BibliographicItem> items = new ArrayList<BibliographicItem>();
    PreparedStatement statement = null;
    ResultSet resultSet = null;
    
    try {
      if (log.isDebug3()) log.debug3(DEBUG_HEADER + "bibliographicItemsQuery = "
	  + bibliographicItemsQuery);
      statement = dbManager.prepareStatement(conn, bibliographicItemsQuery);
      resultSet = dbManager.executeQuery(statement);

      while (resultSet.next()) {
	BibliographicItem item = new BibliographicDatabaseItem(resultSet);
	items.add(item);
      }
    } catch (IllegalArgumentException ex) {
      log.warning(ex.getMessage());
      log.warning("bibliographicItemsQuery = " + bibliographicItemsQuery);
    } catch (SQLException ex) {
      log.warning(ex.getMessage());
      log.warning("bibliographicItemsQuery = " + bibliographicItemsQuery);
    } catch (DbException ex) {
      log.warning(ex.getMessage());
      log.warning("bibliographicItemsQuery = " + bibliographicItemsQuery);
    } finally {
      DbManager.safeCloseResultSet(resultSet);
      DbManager.safeCloseStatement(statement);
    }
    return items;
  }
}
