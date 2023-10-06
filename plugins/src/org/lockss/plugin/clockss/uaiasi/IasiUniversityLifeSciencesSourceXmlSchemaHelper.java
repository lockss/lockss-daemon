/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.uaiasi;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;

import java.util.HashMap;
import java.util.Map;

public class IasiUniversityLifeSciencesSourceXmlSchemaHelper
implements SourceXmlSchemaHelper {

  /*
<?xml version="1.0" encoding="UTF-8"?>
<xml>
    <records>
        <record>
            <database name="MyLibrary">MyLibrary</database>
            <source-app name="Zotero">Zotero</source-app>
            <ref-type name="Journal Article">17</ref-type>
            <contributors>
                <authors>
                    <author>Khoshkharam, Mehdi</author>
                    <author>Shahrajabian, Mohamad Hesam</author>
                </authors>
            </contributors>
            <titles>
                <title>MANAGING YIELD AND YIELD ATTRIBUTES OF TRITICALE IN A DEFICIT IRRIGATION SYSTEM WITH METHANOL FOLIAR APPLICATION</title>
                <secondary-title>Journal of Applied Life Sciences and Environment</secondary-title>
            </titles>
            <periodical>
                <full-title>Journal of Applied Life Sciences and Environment</full-title>
                <abbr-1>JALSE</abbr-1>
            </periodical>
            <pages>100-110</pages>
            <volume>185</volume>
            <number>1</number>
            <issue>1</issue>
            <dates>
                <year>2021</year>
                <pub-dates>
                    <date>2021-6-25</date>
                </pub-dates>
            </dates>
            <isbn>2784-0360, 2784-0379</isbn> --- is this supposed to be ISSN?
            <electronic-resource-num>10.46909/journalalse-2021-010</electronic-resource-num>
            <abstract>Triticale is mainly grown for feed grain and biomass production for thatching straw and general human use. A combined analysis with a factorial layout in the two years of 2016 and 2017 with five replications was used to evaluate the yield and yield components of triticale under different methanol concentrations and irrigation managements in Isfahan, Iran. Irrigation treatments consisted of irrigation on the basis of 70%, 80%, 90% and 100% crop water requirements, and methanol treatments as foliar application on the basis of 15% methanol concentration, 30% methanol concentration and control treatment (0%). Methanol application influence on one hundred grain weight was significant. The maximum plant height, number of tillers, Leaf area index (LAI), leaf area duration (LAD), one hundred grain weight, grain yield, biological yield, harvest index and protein content were achieved in irrigation on the basis of 100% crop water requirement. The maximum plant height, number of tillers, LAI, LAD, one hundred grain weight, grain yield, biological yield, harvest index and protein were obtained in 2017. Foliar methanol application with 15% concentration obtained the maximum plant height, LAI, LAD, one hundred grain weight, biological yield, soil plant analytical development (SPAD) and protein percentage. The results of this experiment suggest that methanol can aid in alleviating the effects of drought stress on triticale in the climatic condition of Isfahan. It is concluded that triticale cultivars performed better in 2017, with 15% concentration of methanol application and irrigation on the basis of 100% crop water requirement.</abstract>
            <remote-database-name>DOI.org (Crossref)</remote-database-name>
            <language>en</language>
            <urls>
                <web-urls>
                    <url>https://jurnalalse.com/wp-content/uploads/JALSE1-21-10-1.pdf</url>
                </web-urls>
                <pdf-urls>
                    <url>internal-pdf://2021-1/JALSE1-21-10-1.pdf</url>
                </pdf-urls>
            </urls>
            <access-date>2023-09-12 13:46:01</access-date>
        </record>
    </records>
</xml>

  */

  protected static final String articleNode = "//records/record";

  protected static final String article_title = articleNode + "/titles/title";

  protected static final String author = articleNode + "/contributors/authors/author";
  private static final String art_pubdate = articleNode + "/dates/pub-dates/date";

  private static final String issn = articleNode + "/isbn"; //publisher use isbn mistakenly as issn
  private static final String issue = articleNode + "/issue";
  private static final String volume = articleNode + "/volume";
  private static final String doi = articleNode + "/electronic-resource-num";
  private static final String pdf_path = articleNode + "/urls/pdf-urls/url";

  static private final Map<String,XPathValue>     
  articleMap = new HashMap<String,XPathValue>();
  static {
    // article specific stuff
    articleMap.put(art_pubdate, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(article_title, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(author, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(issue, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(volume, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(doi, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(issn, XmlDomMetadataExtractor.TEXT_VALUE);
    articleMap.put(pdf_path, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  static private final Map<String,XPathValue>     
  globalMap = null;

  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    cookMap.put(article_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(author, MetadataField.FIELD_AUTHOR);
    cookMap.put(art_pubdate, MetadataField.FIELD_DATE);
    //cookMap.put(issn, MetadataField.FIELD_ISSN);
    cookMap.put(issue, MetadataField.FIELD_ISSUE);
    cookMap.put(volume, MetadataField.FIELD_VOLUME);
    cookMap.put(doi, MetadataField.FIELD_DOI);

  }


  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return null; //globalMap;
  }

  /**
   * return Chinese University of Hong Kong article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return articleNode;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  @Override
  public String getConsolidationXPathKey() {
    return issn;
  }

  @Override
  public String getFilenameXPathKey() {
    return pdf_path;
  }
}