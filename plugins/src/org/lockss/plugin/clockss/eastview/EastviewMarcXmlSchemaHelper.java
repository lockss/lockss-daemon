/*
 * $Id$
 */

/*

 Copyright (c) 2000-2016 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.eastview;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 *  A helper class that defines a schema for Casalini Libri
 *  Please see MARC21 schema: http://www.loc.gov/marc/bibliographic/
 */
public class EastviewMarcXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(EastviewMarcXmlSchemaHelper.class);

  /*
    <?xml version="1.0" encoding="UTF-8"?>
    <marc:collection xmlns:marc="http://www.loc.gov/MARC21/slim" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/MARC21/slim http://www.loc.gov/standards/marcxml/schema/MARC21slim.xsd">
        <marc:record>
            <marc:leader>01295nam a22003012i 4500</marc:leader>
            <marc:controlfield tag="001">1275770BO</marc:controlfield>
            <marc:controlfield tag="003">RuMoEVIS</marc:controlfield>
            <marc:controlfield tag="005">20200506161925.0</marc:controlfield>
            <marc:controlfield tag="006">m     o  d</marc:controlfield>
            <marc:controlfield tag="007">cr  n</marc:controlfield>
            <marc:controlfield tag="008">190929s2018    bw      o  d        rus d</marc:controlfield>
            <marc:datafield tag="040" ind1=" " ind2=" ">
                <marc:subfield code="a">RuMoEVIS</marc:subfield>
                <marc:subfield code="b">eng</marc:subfield>
                <marc:subfield code="e">rda</marc:subfield>
                <marc:subfield code="c">RuMoEVIS</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="245" ind1="0" ind2="0">
                <marc:subfield code="6">880-01</marc:subfield>
                <marc:subfield code="a">Soedinennye Shtaty Ameriki :</marc:subfield>
                <marc:subfield code="b">istorii︠a︡, politika, kulʹtura : sbornik nauchnykh stateĭ /</marc:subfield>
                <marc:subfield code="c">Gl. red. A.A. Kovaleni︠a︡.</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="264" ind1=" " ind2="1">
                <marc:subfield code="6">880-02</marc:subfield>
                <marc:subfield code="a">Minsk :</marc:subfield>
                <marc:subfield code="b">Belarusskai︠a︡ nauka,</marc:subfield>
                <marc:subfield code="c">2018.</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="300" ind1=" " ind2=" ">
                <marc:subfield code="a">1 online resource (277 pages)</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="336" ind1=" " ind2=" ">
                <marc:subfield code="a">text</marc:subfield>
                <marc:subfield code="b">txt</marc:subfield>
                <marc:subfield code="2">rdacontent</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="337" ind1=" " ind2=" ">
                <marc:subfield code="a">computer</marc:subfield>
                <marc:subfield code="b">c</marc:subfield>
                <marc:subfield code="2">rdamedia</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="338" ind1=" " ind2=" ">
                <marc:subfield code="a">online resource</marc:subfield>
                <marc:subfield code="b">cr</marc:subfield>
                <marc:subfield code="2">rdacarrier</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="533" ind1=" " ind2=" ">
                <marc:subfield code="b">Minsk :</marc:subfield>
                <marc:subfield code="c">Belarusskai︠a︡ nauka,</marc:subfield>
                <marc:subfield code="d">2019.</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="653" ind1="0" ind2=" ">
                <marc:subfield code="a">America.</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="653" ind1="0" ind2=" ">
                <marc:subfield code="a">History (General)</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="653" ind1="0" ind2=" ">
                <marc:subfield code="a">United States.</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="700" ind1="1" ind2=" ">
                <marc:subfield code="6">880-03</marc:subfield>
                <marc:subfield code="a">Kovaleni︠a︡, A. A.,</marc:subfield>
                <marc:subfield code="e">editor.</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="776" ind1="1" ind2=" ">
                <marc:subfield code="z">9789850822628</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="856" ind1="4" ind2="0">
                <marc:subfield code="u">https://dlib.eastview.com/browse/book/109986</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="880" ind1="0" ind2="0">
                <marc:subfield code="6">245-01</marc:subfield>
                <marc:subfield code="a">Соединенные Штаты Америки :</marc:subfield>
                <marc:subfield code="b">история, политика, культура : сборник научных статей /</marc:subfield>
                <marc:subfield code="c">Гл. ред. А.А. Коваленя.</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="880" ind1=" " ind2="1">
                <marc:subfield code="6">264-02</marc:subfield>
                <marc:subfield code="a">Мінск :</marc:subfield>
                <marc:subfield code="b">Беларусская наука,</marc:subfield>
                <marc:subfield code="c">2018.</marc:subfield>
            </marc:datafield>
            <marc:datafield tag="880" ind1="1" ind2=" ">
                <marc:subfield code="6">700-03</marc:subfield>
                <marc:subfield code="a">Коваленя, А. А.,</marc:subfield>
                <marc:subfield code="e">editor.</marc:subfield>
            </marc:datafield>
        </marc:record>
    </marc:collection>
   */

  /*
    001 - Control Number Full | Concise
    003 - Control Number Identifier Full | Concise
    005 - Date and Time of Latest Transaction Full | Concise
    006 - Fixed-Length Data Elements - Additional Material Characteristics Full | Concise
    007 - Physical Description Fixed Field Full | Concise
    008 - Fixed-Length Data Elements Full | Concise

    <marc:controlfield tag="001">1275770BO</marc:controlfield>
    <marc:controlfield tag="003">RuMoEVIS</marc:controlfield>
    <marc:controlfield tag="005">20200506161925.0</marc:controlfield>
    <marc:controlfield tag="006">m     o  d</marc:controlfield>
    <marc:controlfield tag="007">cr  n</marc:controlfield>
    <marc:controlfield tag="008">190929s2018    bw      o  d        rus d</marc:controlfield>
   */

  private static final String PDF_TAG_CODE = "001";


  /*
  245 - Title Statement (NR)
  Subfield Codes
  $a - Title (NR)
  $b - Remainder of title (NR)
  $c - Statement of responsibility, etc. (NR)
  $f - Inclusive dates (NR)
  $g - Bulk dates (NR)
  $h - Medium (NR)	$k - Form (R)
  $n - Number of part/section of a work (R)
  $p - Name of part/section of a work (R)
  $s - Version (NR)
  $6 - Linkage (NR)
  $8 - Field link and sequence number (R)

  <marc:datafield tag="245" ind1="0" ind2="0">
      <marc:subfield code="6">880-01</marc:subfield>
      <marc:subfield code="a">Soedinennye Shtaty Ameriki :</marc:subfield>
      <marc:subfield code="b">istorii︠a︡, politika, kulʹtura : sbornik nauchnykh stateĭ /</marc:subfield>
      <marc:subfield code="c">Gl. red. A.A. Kovaleni︠a︡.</marc:subfield>
  </marc:datafield>

   */
  
  private static final String TITLE_TAG ="245";
  private static final String title_code ="a";

  /*
  264 - Production, Publication, Distribution, Manufacture, and Copyright Notice (R)
  Subfield Codes
  $a - Place of production, publication, distribution, manufacture (R)
  $b - Name of producer, publisher, distributor, manufacturer (R)
  $c - Date of production, publication, distribution, manufacture, or copyright notice (R)
  $3 - Materials specified (NR)
  $6 - Linkage (NR)
  $8 - Field link and sequence number (R)

   <marc:datafield tag="264" ind1=" " ind2="1">
      <marc:subfield code="6">880-02</marc:subfield>
      <marc:subfield code="a">Minsk :</marc:subfield>
      <marc:subfield code="b">Belarusskai︠a︡ nauka,</marc:subfield>
      <marc:subfield code="c">2018.</marc:subfield>
   </marc:datafield>

   */

  private static final String PUBLISHER_TAG ="264";
  private static final String publisher_title_code ="b";
  private static final String publisher_date_code ="c";


  /*
  <marc:datafield tag="533" ind1=" " ind2=" ">
  <marc:subfield code="b">Saint Petersburg :</marc:subfield>
  <marc:subfield code="c">Pushkinskiĭ Dom,</marc:subfield>
  <marc:subfield code="d">2014.</marc:subfield>
  </marc:datafield>
   */
  private static final String PUBLISHER_TAG2 ="533";
  private static final String publisher_date_code2 ="d";

  /*
  300 - Physical Description (R)

  Subfield Codes
  $a - Extent (R)
  $b - Other physical details (NR)
  $c - Dimensions (R)
  $e - Accompanying material (NR)
  $f - Type of unit (R)
  $g - Size of unit (R)
  $3 - Materials specified (NR)
  $6 - Linkage (NR)
  $8 - Field link and sequence number (R)

  <marc:datafield tag="300" ind1=" " ind2=" ">
    <marc:subfield code="a">1 online resource (277 pages)</marc:subfield>
  </marc:datafield>

   These fields are not used in metadata
   */

  /*
  700 - Added Entry-Personal Name (R)

  Subfield Codes
  $a - Personal name (NR)
  $b - Numeration (NR)
  $c - Titles and other words associated with a name (R)
  $d - Dates associated with a name (NR)
  $e - Relator term (R)
  $f - Date of a work (NR)
  $g - Miscellaneous information (R)
  $h - Medium (NR)
  $i - Relationship information (R)
  $j - Attribution qualifier (R)
  $k - Form subheading (R)
  $l - Language of a work (NR)
  $m - Medium of performance for music (R)
  $n - Number of part/section of a work (R)
  $o - Arranged statement for music (NR)
  $p - Name of part/section of a work (R)
  $q - Fuller form of name (NR)
  $r - Key for music (NR)
  $s - Version (R)
  $t - Title of a work (NR)
  $u - Affiliation (NR)
  $x - International Standard Serial Number (NR)
  $0 - Authority record control number or standard number (R)
  $1 - Real World Object URI (R)
  $2 - Source of heading or term (NR)
  $3 - Materials specified (NR)
  $4 - Relationship (R)
  $5 - Institution to which field applies (NR)
  $6 - Linkage (NR)
  $8 - Field link and sequence number (R)

  <marc:datafield tag="700" ind1="1" ind2=" ">
      <marc:subfield code="6">880-03</marc:subfield>
      <marc:subfield code="a">Kovaleni︠a︡, A. A.,</marc:subfield>
      <marc:subfield code="e">editor.</marc:subfield>
  </marc:datafield>

  These fields are not used in metadata

   */

  /*
  776 - Additional Physical Form Entry (R)
  Subfield Codes
  $a - Main entry heading (NR)
  $b - Edition (NR)
  $c - Qualifying information (NR)
  $d - Place, publisher, and date of publication (NR)
  $g - Related parts (R)
  $h - Physical description (NR)
  $i - Relationship information (R)
  $k - Series data for related item (R)
  $m - Material-specific details (NR)
  $n - Note (R)
  $o - Other item identifier (R)
  $r - Report number (R)
  $s - Uniform title (NR)
  $t - Title (NR)
  $u - Standard Technical Report Number (NR)
  $w - Record control number (R)
  $x - International Standard Serial Number (NR)
  $y - CODEN designation (NR)
  $z - International Standard Book Number (R)
  $4 - Relationship (R)
  $6 - Linkage (NR)
  $7 - Control subfield (NR)
       /0 - Type of main entry heading
       /1 - Form of name
       /2 - Type of record
       /3 - Bibliographic level
  $8 - Field link and sequence number (R)

<marc:datafield tag="020" ind1=" " ind2=" ">
<marc:subfield code="a">9785433001459</marc:subfield>
</marc:datafield>

 <marc:datafield tag="776" ind1="1" ind2=" ">
      <marc:subfield code="z">9789850822628</marc:subfield>
  </marc:datafield>
   */

  private static final String ISBN_TAG_ALT = "776";
  private static final String isbn_code_alt = "z";

  private static final String ISBN_TAG = "020";
  private static final String isbn_code = "a";

  /*
  856 - Electronic Location and Access (R)
  Subfield Codes
  $a - Host name (R)
  $b - Access number (R)
  $c - Compression information (R)
  $d - Path (R)
  $f - Electronic name (R)
  $h - Processor of request (NR)
  $i - Instruction (R)
  $j - Bits per second (NR)
  $k - Password (NR)
  $l - Logon (NR)
  $m - Contact for access assistance (R)
  $n - Name of location of host (NR)
  $o - Operating system (NR)
  $p - Port (NR)
  $q - Electronic format type (NR)
  $r - Settings (NR)
  $s - File size (R)
  $t - Terminal emulation (R)
  $u - Uniform Resource Identifier (R)
  $v - Hours access method available (R)
  $w - Record control number (R)
  $x - Nonpublic note (R)
  $y - Link text (R)
  $z - Public note (R)
  $2 - Access method (NR)
  $3 - Materials specified (NR)
  $6 - Linkage (NR)
  $7 - Access status (NR)
  $8 - Field link and sequence number (R)

  <marc:datafield tag="856" ind1="4" ind2="0">
      <marc:subfield code="u">https://dlib.eastview.com/browse/book/109986</marc:subfield>
  </marc:datafield>

  These fields are not used in metadata
   */
  private static final String ELECTRONIC_ACCESS_LOCATION_TAG ="856";
  private static final String url_code ="u";

  /*
  880 - Alternate Graphic Representation (R)
  Subfield Codes
  $a-z - Same as associated field
  $0-5 - Same as associated field
  $6 - Linkage (NR)
  $7-9 - Same as associated field

    <marc:datafield tag="880" ind1="0" ind2="0">
        <marc:subfield code="6">245-01</marc:subfield>
        <marc:subfield code="a">Соединенные Штаты Америки :</marc:subfield>
        <marc:subfield code="b">история, политика, культура : сборник научных статей /</marc:subfield>
        <marc:subfield code="c">Гл. ред. А.А. Коваленя.</marc:subfield>
    </marc:datafield>
    <marc:datafield tag="880" ind1=" " ind2="1">
        <marc:subfield code="6">264-02</marc:subfield>
        <marc:subfield code="a">Мінск :</marc:subfield>
        <marc:subfield code="b">Беларусская наука,</marc:subfield>
        <marc:subfield code="c">2018.</marc:subfield>
    </marc:datafield>
    <marc:datafield tag="880" ind1="1" ind2=" ">
        <marc:subfield code="6">700-03</marc:subfield>
        <marc:subfield code="a">Коваленя, А. А.,</marc:subfield>
        <marc:subfield code="e">editor.</marc:subfield>
    </marc:datafield>
   */

  private static String AUTHOR_TAG = "880";
  private static String author_name_code = "a";


  // A top level for the worksheet table is
  private static String MARC_record = "/collection/record";

  // these are all relative to the record
  public static String MARC_pdf =
      "controlfield[@tag = \"" + PDF_TAG_CODE + "\"]";
  public static String MARC_publisher =
          "datafield[@tag = \"" + PUBLISHER_TAG + "\"]" +
                  "/subfield[@code = \"" + publisher_title_code + "\"]";
  public static String MARC_pub_date =
          "datafield[@tag = \"" + PUBLISHER_TAG + "\"]" +
                  "/subfield[@code = \"" + publisher_date_code + "\"]";
  public static String MARC_title = 
      "datafield[@tag = \"" + TITLE_TAG + "\"]" +
          "/subfield[@code=\"" + title_code + "\"]";
  public static String MARC_isbn =
          "datafield[@tag = \"" + ISBN_TAG + "\"]" +
                  "/subfield[@code = \"" + isbn_code + "\"]";
  public static String MARC_isbn_alt =
          "datafield[@tag = \"" + ISBN_TAG_ALT + "\"]" +
                  "/subfield[@code = \"" + isbn_code_alt + "\"]";
  public static String MARC_author =
          "datafield[@tag = \"" + AUTHOR_TAG + "\" and @ind1=\"1\" and @ind2=\" \" ]" +
                  "/subfield[@code = \"" + author_name_code + "\"]";

  public static String MARC_pub_date2 =
          "datafield[@tag = \"" + PUBLISHER_TAG2 + "\"]" +
                  "/subfield[@code = \"" + publisher_date_code2 + "\"]";    

  public static String zippedFolderName;
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> eastview_articleMap =
      new HashMap<String,XPathValue>();
  static {
    eastview_articleMap.put(MARC_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    eastview_articleMap.put(MARC_pub_date,  XmlDomMetadataExtractor.TEXT_VALUE);
    eastview_articleMap.put(MARC_pub_date2,  XmlDomMetadataExtractor.TEXT_VALUE);
    eastview_articleMap.put(MARC_title, XmlDomMetadataExtractor.TEXT_VALUE);
    eastview_articleMap.put(MARC_pdf, XmlDomMetadataExtractor.TEXT_VALUE);
    eastview_articleMap.put(MARC_isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    eastview_articleMap.put(MARC_isbn_alt, XmlDomMetadataExtractor.TEXT_VALUE);
    eastview_articleMap.put(MARC_author, XmlDomMetadataExtractor.TEXT_VALUE);

  }

  /* 2. there is only one XML file */
  static private final String eastview_recordNode = MARC_record;

  /* 3. in MARCXML there is no global information we care about */
  static private final Map<String,XPathValue> MARC_globalMap = null;

  /*
   * The emitter will need a map to know how to cook raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // A few other cookMap need post process, so they will be in the post process function
    cookMap.put(MARC_isbn, MetadataField.FIELD_ISBN);
    cookMap.put(MARC_isbn_alt, MetadataField.FIELD_ISBN);
  }


  /**
   * MARCXML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return MARC_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return eastview_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return eastview_recordNode;
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
    return null;
  }

  public static String getZippedFolderName() {
    return zippedFolderName;
  }

  public static void setZippedFolderName(String fname) {
    zippedFolderName = fname;
  }
}
