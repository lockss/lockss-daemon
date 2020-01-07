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

import com.sun.jimi.core.util.P;
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
		<leader>01079nab a2200265 i 4500</leader>
		<controlfield tag="001">000002213906</controlfield>
		<controlfield tag="003">ItFiC</controlfield>
		<controlfield tag="005">20091114214923.0</controlfield>
		<controlfield tag="006">m        d        </controlfield>
		<controlfield tag="007">cr uuu---uuuuu</controlfield>
		<controlfield tag="008">091114n        xx uu   s    u0    0und c</controlfield>
		<datafield ind1="7" ind2=" " tag="024">
			<subfield code="a">10.1400/64562</subfield>
			<subfield code="2">DOI</subfield>
		</datafield>
		<datafield ind1=" " ind2=" " tag="040">
			<subfield code="a">ItFiC</subfield>
			<subfield code="b">eng</subfield>
			<subfield code="c">ItFiC</subfield>
		</datafield>
		<datafield ind1="0" ind2=" " tag="097">
			<subfield code="a">2194804</subfield>
			<subfield code="b">013</subfield>
			<subfield code="c">2213906</subfield>
			<subfield code="d">001</subfield>
		</datafield>
		<datafield ind1="1" ind2="3" tag="245">
			<subfield code="a">La svolta post-moderna in psicoanalisi.</subfield>
		</datafield>
		<datafield ind1=" " ind2=" " tag="260">
			<subfield code="a">Milano :</subfield>
			<subfield code="b">Franco Angeli,</subfield>
			<subfield code="c">2000.</subfield>
		</datafield>
		<datafield ind1=" " ind2=" " tag="300">
			<subfield code="a">P. [1-40] [40]</subfield>
		</datafield>
		<datafield ind1="1" ind2=" " tag="700">
			<subfield code="a">Eagle, Morris N.</subfield>
		</datafield>
		<datafield ind1="0" ind2=" " tag="773">
			<subfield code="t">Psicoterapia e scienze umane. Fascicolo 4, 2000.</subfield>
			<subfield code="d">Milano : Franco Angeli, 2000.</subfield>
			<subfield code="w">()2194804</subfield>
		</datafield>
		<datafield ind1="4" ind2="0" tag="856">
			<subfield code="u">http://digital.casalini.it/10.1400/64562</subfield>
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
   */

  /*
  <datafield ind1="7" ind2=" " tag="024">
			<subfield code="a">10.1400/64562</subfield>
			<subfield code="2">DOI</subfield>
  </datafield>
   */
  private static final String DOI_TAG ="024";
  private static final String doi_code ="a";

  /*
  		<datafield ind1="0" ind2=" " tag="097">
			<subfield code="a">2194804</subfield> //this is the last part of PDF file name
			<subfield code="b">013</subfield>
			<subfield code="c">2213906</subfield> // this is an number we call "article number"
			<subfield code="d">001</subfield> // this is the volume part of the PDF
		</datafield>
   */
  private static final String PDF_TAG ="097";
  private static final String file_code ="a";
  private static final String article_code ="c";

  /*
    <datafield ind1="0" ind2="0" tag="245">
		<subfield code="a">Psicoterapia e scienze umane. Fascicolo 4, 2000.</subfield>
	</datafield>
   */
  private static final String TITLE_TAG ="245";
  private static final String title_code ="a";

  /*
	<datafield ind1=" " ind2=" " tag="260">
		<subfield code="a">Milano :</subfield>
		<subfield code="b">Franco Angeli,</subfield>
		<subfield code="c">2000.</subfield> //this is the year of the PDF file "2000_4_2194804.pdf"
	</datafield>
   */
  private static final String PUBLICATION_TAG ="260";
  private static final String pub_code ="b";
  private static final String pubdate_code ="c";

  /*
    <datafield ind1="4" ind2="0" tag="856">
		<subfield code="u">http://digital.casalini.it/2194804</subfield>
	</datafield>

	ELECTRONIC_ACCESS_LOCATION_TAG are not used by current metadata mechanism
   */
  private static final String ELECTRONIC_ACCESS_LOCATION_TAG ="856";
  private static final String url_code ="u";

  /*
    <datafield ind1=" " ind2=" " tag="300">
		<subfield code="a">P. [1-40] [40]</subfield>
    </datafield>
    <datafield ind1=" " ind2=" " tag="300">
		<subfield code="a">370-370 p.</subfield>
	</datafield>
  */
  private static final String START_PAGE_LOCATION_TAG ="300";
  private static final String start_page_code ="a";

  /*
	<datafield ind1="1" ind2=" " tag="700">
		<subfield code="a">Eagle, Morris N.</subfield>
	</datafield>

	<datafield ind1="1" ind2=" " tag="700">
		<subfield code="a">Muhlleitner, Elke.</subfield>
	</datafield>
	<datafield ind1="1" ind2=" " tag="700">
		<subfield code="a">Reichmayr, Johannes.</subfield>
	</datafield>
   */
  private static final String AUTHOR_TAG ="700";
  private static final String author_name_code ="a";

  /*
  	<datafield ind1="0" ind2=" " tag="773">
		<subfield code="t">Psicoterapia e scienze umane. Fascicolo 4, 2000.</subfield>
		<subfield code="d">Milano : Franco Angeli, 2000.</subfield>
		<subfield code="w">()2194804</subfield>
	</datafield>

	<datafield ind1="0" ind2=" " tag="773">
		<subfield code="t">Psicoterapia e scienze umane.</subfield>
		<subfield code="x">1972-5043</subfield>
		<subfield code="w">()4517279</subfield>
	</datafield>
   */
  private static final String PUB_TAG ="773";
  private static final String publication_code ="t";

  // A top level for the worksheet table is
  private static String MARC_record = "/collection/record";

  // these are all relative to the record
  public static String PDF_FILE_YEAR =
      "datafield[@tag = \"" + PUBLICATION_TAG + "\"]" +
          "/subfield[@code = \"" + pubdate_code + "\"]";
  public static String PDF_FILE_VOLUME =
          "datafield[@tag = \"" + PUB_TAG + "\"]" +
                  "/subfield[@code = \"" + publication_code + "\"]";
  public static String PDF_ARTICLE =
          "datafield[@tag = \"" + PDF_TAG + "\"]" +
                  "/subfield[@code = \"" + article_code + "\"]";
  public static String MARC_file =  
      "datafield[@tag = \"" + PDF_TAG + "\"]" +
          "/subfield[@code = \"" + file_code + "\"]";
  public static String MARC_title = 
      "datafield[@tag = \"" + TITLE_TAG + "\"]" +
          "/subfield[@code=\"" + title_code + "\"]";
  public static String MARC_pub_date =
      "datafield[@tag = \"" + PUBLICATION_TAG + "\"]" +
          "/subfield[@code = \"" + pubdate_code + "\"]";
  public static String MARC_publisher = 
      "datafield[@tag = \"" + PUBLICATION_TAG + "\"]" +
          "/subfield[@code = \"" + pub_code + "\"]";
  public static String MARC_doi =
          "datafield[@tag = \"" + DOI_TAG + "\"]" +
                  "/subfield[@code = \"" + doi_code + "\"]";
  public static String MARC_start_page =
          "datafield[@tag = \"" + START_PAGE_LOCATION_TAG + "\"]" +
                  "/subfield[@code = \"" + start_page_code + "\"]";
  public static String MARC_author =
          "datafield[@tag = \"" + AUTHOR_TAG + "\"]" +
                  "/subfield[@code = \"" + author_name_code + "\"]";
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> casalini_articleMap =
      new HashMap<String,XPathValue>();
  static {

    casalini_articleMap.put(MARC_title, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_pub_date,  XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(PDF_FILE_YEAR, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(PDF_FILE_VOLUME, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(PDF_ARTICLE, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_file, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_start_page, XmlDomMetadataExtractor.TEXT_VALUE);
    casalini_articleMap.put(MARC_author, XmlDomMetadataExtractor.TEXT_VALUE);

  }

  /* 2. there is only one XML file */
  static private final String casalini_recordNode = MARC_record;

  /* 3. in MARCXML there is no global information we care about */
  static private final Map<String,XPathValue> MARC_globalMap = null;

  /*
   * The emitter will need a map to know how to cook raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(MARC_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(MARC_publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(MARC_pub_date, MetadataField.FIELD_DATE);
    cookMap.put(MARC_author, MetadataField.FIELD_AUTHOR);
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
    return null;
  }
}
