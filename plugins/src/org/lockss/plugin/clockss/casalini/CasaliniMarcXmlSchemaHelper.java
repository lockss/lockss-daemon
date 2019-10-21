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

package org.lockss.plugin.clockss.casalini;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

import org.w3c.dom.Node;

/**
 *  A helper class that defines a schema for Casalini Libri
 *  Please see MARC21 schema: http://www.loc.gov/marc/bibliographic/
 */
public class CasaliniMarcXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(CasaliniMarcXmlSchemaHelper.class);

  /*
    <collection xmlns="http://www.loc.gov/MARC21/slim">
	<record>
		<leader>00938nab a2200229 i 4500</leader>
		<controlfield tag="001">000002194804</controlfield>
		<controlfield tag="003">ItFiC</controlfield>
		<controlfield tag="005">20070508000000.0</controlfield>
		<controlfield tag="006">m        d        </controlfield>
		<controlfield tag="007">cr uuu---uuuuu</controlfield>
		<controlfield tag="008">091114s2000    xx uu   s    u0    0und c</controlfield>
		<datafield ind1=" " ind2=" " tag="040">
			<subfield code="a">ItFiC</subfield>
			<subfield code="b">eng</subfield>
			<subfield code="c">ItFiC</subfield>
		</datafield>
		<datafield ind1="0" ind2=" " tag="097">
			<subfield code="a">2193101</subfield>
			<subfield code="b">012</subfield>
			<subfield code="c">2194804</subfield>
			<subfield code="d">004</subfield>
		</datafield>
		<datafield ind1="0" ind2="0" tag="245">
			<subfield code="a">Psicoterapia e scienze umane. Fascicolo 4, 2000.</subfield>
		</datafield>
		<datafield ind1=" " ind2=" " tag="260">
			<subfield code="a">Milano :</subfield>
			<subfield code="b">Franco Angeli,</subfield>
			<subfield code="c">2000.</subfield>
		</datafield>
		<datafield ind1="0" ind2=" " tag="773">
			<subfield code="t">Psicoterapia e scienze umane.</subfield>
			<subfield code="d">Milano : Franco Angeli </subfield>
			<subfield code="x">1972-5043</subfield>
			<subfield code="w">()2193101</subfield>
		</datafield>
		<datafield ind1="4" ind2="0" tag="856">
			<subfield code="u">http://digital.casalini.it/2194804</subfield>
		</datafield>
		<datafield ind1=" " ind2=" " tag="900">
			<subfield code="a">(c) Casalini Libri, 50014 Fiesole (Italy) - www.casalini.it</subfield>
		</datafield>
		<datafield ind1=" " ind2=" " tag="910">
			<subfield code="a">aBibliographic data</subfield>
			<subfield code="e">Torrossa Fulltext Resource</subfield>
			<subfield code="g">Casalini Libri</subfield>
		</datafield>
	</record>
	</collection>
   */

  private static final String ISBN_TAG ="020";
  private static final String isbn_code ="a";

  private static final String LOCATOR2_TAG ="097";
  private static final String dir_code ="a";
  private static final String file_code ="c";

  private static final String TITLE_TAG ="245";
  private static final String title_code ="a";
  private static final String subtitle_code ="b";
  private static final String author_code ="c";

  private static final String PUBLICATION_TAG ="260";
  private static final String pub_code ="b";
  private static final String pubdate_code ="c";

  private static final String NAME_TAG ="700";
  private static final String name_code ="a";

  // A top level for the worksheet table is
  private static String MARC_record = "/collection/record";

  // these are all relative to the record
  public static String MARC_dir =  
      "datafield[@tag = \"" + LOCATOR2_TAG + "\"]" +
          "/subfield[@code = \"" + dir_code + "\"]";
  public static String MARC_file =  
      "datafield[@tag = \"" + LOCATOR2_TAG + "\"]" +
          "/subfield[@code = \"" + file_code + "\"]";
  public static String MARC_isbn =  
      "datafield[@tag = \"" + ISBN_TAG + "\"]" +
          "/subfield[@code = \"" + isbn_code + "\"]";
  public static String MARC_title = 
      "datafield[@tag = \"" + TITLE_TAG + "\"]" +
          "/subfield[@code=\"" + title_code + "\"]";
  public static String MARC_subtitle = 
      "datafield[@tag = \"" + TITLE_TAG + "\"]" +
          "/subfield[@code = \"" + subtitle_code + "\"]";
  private static String MARC_pub_date = 
      "datafield[@tag = \"" + PUBLICATION_TAG + "\"]" +
          "/subfield[@code = \"" + pubdate_code + "\"]";
  public static String MARC_publisher = 
      "datafield[@tag = \"" + PUBLICATION_TAG + "\"]" +
          "/subfield[@code = \"" + pub_code + "\"]";
  private static String MARC_author = 
      "datafield[@tag = \"" + NAME_TAG + "\"]" +
          "/subfield[@code = \"" + name_code + "\"]";
  private static String MARC_altauthor = 
      "datafield[@tag = \"" + TITLE_TAG + "\"]" +
          "/subfield[@code = \"" + author_code + "\"]";

  static private final NodeValue ADATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      String cleandate =  StringUtils.stripStart(node.getTextContent(), ".-[] ");
      return StringUtils.substring(cleandate,0,4);
    }
  };
  
  /*
   *  Cleanup the title or subtitle so they look nice 
   */
  static private final NodeValue TITLE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      // remove extraneous spaces and punctuation
      // so that when put together Title: Subtitle it looks correct
      //
      return StringUtils.strip(node.getTextContent(), ",/: "); 
    }
  };
  
  // could be 13 or 10, take out hyphens
  static private final NodeValue ISBN_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {
      String justisbn = StringUtils.strip(node.getTextContent(), "(* ");
      return StringUtils.remove(justisbn, "-");
    }
  };

  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> casalini_articleMap =
      new HashMap<String,XPathValue>();
  static {

    casalini_articleMap.put(MARC_isbn, ISBN_VALUE);
    casalini_articleMap.put(MARC_title, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_pub_date,  ADATE_VALUE);
    casalini_articleMap.put(MARC_author, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_dir, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_file, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_altauthor, XmlDomMetadataExtractor.TEXT_VALUE);

  }

  /* 2. there is only one XML file */
  static private final String casalini_recordNode = MARC_record;

  /* 3. in MARCXML there is no global information we care about */
  static private final Map<String,XPathValue> MARC_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(MARC_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(MARC_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(MARC_publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(MARC_pub_date, MetadataField.FIELD_DATE);
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
    return casalini_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return casalini_recordNode;
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
    return MARC_dir;
  }

}
