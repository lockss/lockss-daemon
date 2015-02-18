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

import static org.lockss.db.SqlConstants.*;
import static org.lockss.metadata.MetadataManager.*;
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
    final String provider;
    final String publisher;
    final String seriesTitle;
    final String[] proprietarySeriesIds;
    final String publicationTitle;
    final String[] proprietaryIds;
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
      proprietarySeriesIds = new String[1];
      proprietarySeriesIds[0] = resultSet.getString(3);
      publicationTitle = resultSet.getString(4);
      proprietaryIds = new String[1];
      proprietaryIds[0] = resultSet.getString(5);
      eissn = MetadataUtil.formatIssn(resultSet.getString(6));
      printissn = MetadataUtil.formatIssn(resultSet.getString(7));
      eisbn = MetadataUtil.formatIsbn(resultSet.getString(8));
      printisbn = MetadataUtil.formatIsbn(resultSet.getString(9));
      year = resultSet.getString(10);
      volume = resultSet.getString(11);
      coverageDepth = resultSet.getString(12);
      publicationType = resultSet.getString(13);
      provider = resultSet.getString(14);
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
    public String[] getProprietaryIds() {
      return proprietaryIds;
    }

    @Override
    public String[] getProprietarySeriesIds() {
      return proprietarySeriesIds;
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
    public String getProviderName() {
      return provider;
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

    /**
     * Provides an indication of whether there are no differences between this
     * object and another one in anything other than proprietary identifiers.
     * 
     * @param other
     *          A BibliographicItem with the other object.
     * @return <code>true</code> if there are no differences in anything other
     *         than their proprietary identifiers, <code>false</code> otherwise.
     */
    @Override
    public boolean sameInNonProprietaryIdProperties(BibliographicItem other){
      return other != null
	  && areSameProperty(publisher, other.getPublisherName())
	  && areSameProperty(seriesTitle, other.getSeriesTitle())
	  && areSameProperty(publicationTitle, other.getPublicationTitle())
	  && areSameProperty(eissn, other.getEissn())
	  && areSameProperty(printissn, other.getPrintIssn())
	  && areSameProperty(eisbn, other.getEisbn())
	  && areSameProperty(printisbn, other.getPrintIsbn())
	  && areSameProperty(year, other.getYear())
	  && areSameProperty(volume, other.getVolume())
	  && areSameProperty(coverageDepth, other.getCoverageDepth())
	  && areSameProperty(publicationType, other.getPublicationType())
	  && areSameProperty(provider, other.getProviderName());
    }

    /**
     * Provides an indication of whether two properties are the same.
     * 
     * @param property1
     *          A String with the first property to be compared.
     * @param property2
     *          A String with the second property to be compared.
     * @return <code>true</code> if the two properties are the same,
     *         <code>false</code> otherwise.
     */
    private boolean areSameProperty(String property1, String property2) {
      return ((property1 == null && property2 == null)
	  || (property1 != null && property1.equals(property2)));
    }
  }
    
  /**
   * This query generates information to populate BibliographicItem records.
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
  and mit1.type_name in ('journal','book')
  and mi1.md_item_seq = title.md_item_seq
  and title.name_type = 'primary'
   *</pre>
   */
  static final String bibliographicItemsQuery = "select distinct"
      + "     p." + PUBLISHER_NAME_COLUMN + " as publisher"
      + "   , series_title." + NAME_COLUMN + " as series_title"
      + "   , pi0." + PROPRIETARY_ID_COLUMN + " series_proprietary_id"
      + "   , title." + NAME_COLUMN + " as title"
      + "   , pi1." + PROPRIETARY_ID_COLUMN + " as proprietery_id "
      + "   , coalesce(e_issn0." + ISSN_COLUMN
      + "       , e_issn1." + ISSN_COLUMN + ") as e_issn"
      + "   , coalesce(p_issn0." + ISSN_COLUMN
      + "       , p_issn1." + ISSN_COLUMN + ") as p_issn"
      + "   , e_isbn." + ISBN_COLUMN + " as e_isbn"
      + "   , p_isbn." + ISBN_COLUMN + " as p_isbn"
      + "   , substr(mi2." + DATE_COLUMN + ", 1, 4) as published_year"
      + "   , b." + VOLUME_COLUMN + " as volume"
      + "   , mi2." + COVERAGE_COLUMN + " as coverage_depth"
      + "   , coalesce(mit0." + TYPE_NAME_COLUMN
      + "       , mit1." + TYPE_NAME_COLUMN + ") as publication_type"
      + "   , pv." + PROVIDER_NAME_COLUMN
      + " from "
      + "     " + PUBLISHER_TABLE + " p"
      + "   , " + PUBLICATION_TABLE + " pn1"
      + "   , " + MD_ITEM_NAME_TABLE + " title"
      + "   , " + MD_ITEM_TABLE + " mi1"
      + "   left join " + ISSN_TABLE + " e_issn1"
      + "     on mi1." + MD_ITEM_SEQ_COLUMN + " = e_issn1." + MD_ITEM_SEQ_COLUMN
      + "       and e_issn1." + ISSN_TYPE_COLUMN + " = '" + E_ISSN_TYPE + "'" 
      + "   left join " + ISSN_TABLE + " p_issn1"
      + "     on mi1." + MD_ITEM_SEQ_COLUMN + " = p_issn1." + MD_ITEM_SEQ_COLUMN
      + "       and p_issn1." + ISSN_TYPE_COLUMN + " = '" + P_ISSN_TYPE + "'"
      + "   left join " + ISBN_TABLE + " e_isbn"
      + "     on mi1." + MD_ITEM_SEQ_COLUMN + " = e_isbn." + MD_ITEM_SEQ_COLUMN
      + "       and e_isbn." + ISBN_TYPE_COLUMN + " = '" + E_ISBN_TYPE + "'"
      + "   left join " + ISBN_TABLE + " p_isbn"
      + "     on mi1." + MD_ITEM_SEQ_COLUMN + " = p_isbn." + MD_ITEM_SEQ_COLUMN
      + "       and p_isbn." + ISBN_TYPE_COLUMN + " = '" + P_ISBN_TYPE + "'"
      + "   left join " + MD_ITEM_TABLE + " mi0"
      + "     on mi0." + MD_ITEM_SEQ_COLUMN + " = mi1." + PARENT_SEQ_COLUMN
      + "   left join " + MD_ITEM_NAME_TABLE + " series_title"
      + "     on mi0." + MD_ITEM_SEQ_COLUMN
      + "       = series_title." + MD_ITEM_SEQ_COLUMN
      + "       and series_title." + NAME_TYPE_COLUMN
      + "         = '" + PRIMARY_NAME_TYPE + "'"
      + "   left join " + PUBLICATION_TABLE + " pn0"
      + "     on mi0." + MD_ITEM_SEQ_COLUMN + " = pn0." + MD_ITEM_SEQ_COLUMN
      + "   left join " + MD_ITEM_TYPE_TABLE + " mit0"
      + "     on mi0." + MD_ITEM_TYPE_SEQ_COLUMN
      + "       = mit0." + MD_ITEM_TYPE_SEQ_COLUMN
      + "   left join " + ISSN_TABLE + " e_issn0"
      + "     on mi0." + MD_ITEM_SEQ_COLUMN + " = e_issn0." + MD_ITEM_SEQ_COLUMN
      + "       and e_issn0." + ISSN_TYPE_COLUMN + " = '" + E_ISSN_TYPE + "'" 
      + "   left join " + ISSN_TABLE + " p_issn0"
      + "     on mi0." + MD_ITEM_SEQ_COLUMN + " = p_issn0." + MD_ITEM_SEQ_COLUMN
      + "       and p_issn0." + ISSN_TYPE_COLUMN + " = '" + P_ISSN_TYPE + "'"
      + "   left join " + MD_ITEM_TABLE + " mi2"
      + "     on mi2." + PARENT_SEQ_COLUMN + " = mi1." + MD_ITEM_SEQ_COLUMN
      + "   left join " + BIB_ITEM_TABLE + " b"
      + "     on mi2." + MD_ITEM_SEQ_COLUMN + " = b." + MD_ITEM_SEQ_COLUMN
      + "   left join " + PROPRIETARY_ID_TABLE + " pi0"
      + "     on mi0." + MD_ITEM_SEQ_COLUMN + " = pi0." + MD_ITEM_SEQ_COLUMN
      + "   left join " + PROPRIETARY_ID_TABLE + " pi1"
      + "     on mi1." + MD_ITEM_SEQ_COLUMN + " = pi1." + MD_ITEM_SEQ_COLUMN
      + "   , " + MD_ITEM_TYPE_TABLE + " mit1"
      + "   , " + AU_MD_TABLE + " am"
      + "   , " + PROVIDER_TABLE + " pv"
      + " where"
      + "   p." + PUBLISHER_SEQ_COLUMN + " = pn1." + PUBLISHER_SEQ_COLUMN
      + "   and pn1." + MD_ITEM_SEQ_COLUMN + " = mi1." + MD_ITEM_SEQ_COLUMN
      + "   and mi1." + MD_ITEM_TYPE_SEQ_COLUMN
      + "     = mit1." + MD_ITEM_TYPE_SEQ_COLUMN
      + "   and mit1." + TYPE_NAME_COLUMN
      + "     in ('" + MD_ITEM_TYPE_JOURNAL + "','" + MD_ITEM_TYPE_BOOK + "')"
      + "   and mi1." + MD_ITEM_SEQ_COLUMN + " = title." + MD_ITEM_SEQ_COLUMN
      + "   and title." + NAME_TYPE_COLUMN + " = '" + PRIMARY_NAME_TYPE + "'"
      + "   and mi2." + AU_MD_SEQ_COLUMN + " = am." + AU_MD_SEQ_COLUMN
      + "   and am." + PROVIDER_SEQ_COLUMN + " = pv." + PROVIDER_SEQ_COLUMN
      + " order by"
      + "     publisher"
      + "   , series_title"
      + "   , title"
      + "   , e_issn"
      + "   , p_issn"
      + "   , e_isbn"
      + "   , p_isbn"
      + "   , published_year"
      + "   , volume"
      + "   , coverage_depth"
      + "   , publication_type"
      + "   , pv." + PROVIDER_NAME_COLUMN
      + "   , series_proprietary_id"
      + "   , proprietery_id";

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
    BibliographicItem previousItem = null;
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
	// Avoid adding items that differ only in some proprietary identifier. 
	if (!item.sameInNonProprietaryIdProperties(previousItem)) {
	  items.add(item);
	}
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
