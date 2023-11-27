/*

Copyright (c) 2000-2023, Board of Trustees of Leland Stanford Jr. University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

*/

package org.lockss.plugin.clockss.frontiers;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.w3c.dom.Node;

/**
 *  A helper class that defines a schema for Frontiers Ebooks
 *  The metadata comes from an XML file that was created via export
 *  from an excel spreadsheet.
 *  We count on the format and ordering of the columns to stay
 *  consistent
 *  @author alexohlson
 */
public class FrontiersBooksWorksheetXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(FrontiersBooksWorksheetXmlSchemaHelper.class);
  
  //used for date formatting, set up once
  private static final String OLD_FORMAT = "dd.MM.yyyy";
  private static final String NEW_FORMAT = "yyyy-MM-dd";
  private static SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
  /*
   *  Put date in to a format we want
   *  <Data ss:Type="DateTime">2013-02-26T00:00:00.000</Data>
   *  return just the 2013-02-26
   */
  static private final NodeValue DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      // This is a smart function 
      // it returns what is given as the first arg (null, empty or the entire
      // thing if the separator isn't found (in this case the "T"
      // date is either dateTime: 1909-07-06T00:00:00.000
      // or date is a string: 13.01.2013
      // or date is a string with extra explanation: 20.02.2013/ description updated 21.03.2014 (link to 2nd Edition)
      // we can just strip anything after "T" or "/"
      // and then if it's hyphenated, leave it as is,
      // if it's got "." separators, change order and use hypen
      String dateString =  StringUtils.substringBefore(node.getTextContent(), "T");
      dateString = StringUtils.substringBefore(dateString, "/");
      if (StringUtils.contains(dateString,".")) {
        Date d;
        try {
          d = sdf.parse(dateString);
        } catch (ParseException e) {
          log.debug3("couldn't parse date string");
          // just return the original
          return dateString;
        }
        sdf.applyPattern(NEW_FORMAT);
        //return reformatted
        return sdf.format(d);
      }
      // return what we were given
      return dateString;
    }
  };

  /* 
   *  Frontiers worksheet specific XPATH key definitions that we care about
   *  Columns are:
   *  Journal (specified topic within the series, probably not usable)
   *  Title (of book)
   *  Publication Date
   *  ISBN
   *  DOI
   *  Editors
   *  (G,H,I, unused by us)
   *  Series Title
   *  Series ISSN
   */
  
  // We count on column positioning being consistent
  private static final String TITLE_COL ="2";
  private static final String DATE_COL ="3";
  private static final String ISBN_COL ="4";
  private static final String DOI_COL ="5";
  private static final String EDITOR_COL ="6";
  private static final String SERIES_COL ="10";
  private static final String SERIES_ISSN_COL ="11";

  // A top level for the worksheet table is
  private static String FEB_table = "/Workbook/Worksheet/Table";
  private static String FEB_book = FEB_table + "/Row[position()>1]"; 

  // The first ROW in the table are headers 
  // each subsequent row represents a new record, one book/row 
  
  // these are all relative to the row  
  private static String FEB_title = "Cell[" + TITLE_COL + "]/Data";
  public static String FEB_isbn =  "Cell[" + ISBN_COL + "]/Data";
  private static String FEB_pub_date = "Cell[" + DATE_COL + "]/Data";
  private static String FEB_doi = "Cell[" + DOI_COL + "]/Data";
  public static String FEB_editor = "Cell[" + EDITOR_COL + "]/Data";
  public static String FEB_series = "Cell[" + SERIES_COL + "]/Data";
  public static String FEB_series_issn = "Cell[" + SERIES_ISSN_COL + "]/Data";
  

  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> FEB_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    FEB_articleMap.put(FEB_title, XmlDomMetadataExtractor.TEXT_VALUE);
    FEB_articleMap.put(FEB_isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    FEB_articleMap.put(FEB_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    FEB_articleMap.put(FEB_editor, XmlDomMetadataExtractor.TEXT_VALUE);
    FEB_articleMap.put(FEB_pub_date, DATE_VALUE);
    FEB_articleMap.put(FEB_series, XmlDomMetadataExtractor.TEXT_VALUE);
    FEB_articleMap.put(FEB_series_issn, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  static private final String FEB_bookNode = FEB_book; 

  /* 3. in MARCXML there is no global information because one file/article */
  static private final Map<String,XPathValue> FEB_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(FEB_isbn, MetadataField.FIELD_ISBN);
    cookMap.put(FEB_series_issn, MetadataField.FIELD_ISSN);
    cookMap.put(FEB_series, MetadataField.FIELD_SERIES_TITLE);
    cookMap.put(FEB_doi, MetadataField.FIELD_DOI);
    // we will add subtitle/edition as needed in post-cook process
    cookMap.put(FEB_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(FEB_editor, MetadataField.FIELD_AUTHOR);
    cookMap.put(FEB_pub_date, MetadataField.FIELD_DATE);
  }


  /**
   * no global map - this is null
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return FEB_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return FEB_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return FEB_bookNode;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /**
   * No duplicate data 
   */
  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /**
   * No consolidation required
   */
  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /**
   * The filenames are the same as the XML filenames with .pdf suffix
   */
  @Override
  public String getFilenameXPathKey() {
    return FEB_isbn;
  }

}
