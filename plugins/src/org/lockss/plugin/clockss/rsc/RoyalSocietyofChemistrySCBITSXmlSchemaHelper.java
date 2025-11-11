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

package org.lockss.plugin.clockss.rsc;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

public class RoyalSocietyofChemistrySCBITSXmlSchemaHelper implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(RoyalSocietyofChemistrySCBITSXmlSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";

  static private final NodeValue DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of JATS publishing date");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;

      // perhaps pick up iso attr if it's available
      String tyear = null;
      String tday = null;
      String tmonth = null;
      // look at each child of the TitleElement for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("day".equals(nodeName)) {
          tday = checkNode.getTextContent();
        } else if ("month".equals(nodeName) ) {
          tmonth = checkNode.getTextContent();
        } else if ("year".equals(nodeName)) {
          tyear = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tyear != null) {
        valbuilder.append(tyear);
        if (tday != null && tmonth != null) {
          valbuilder.append("-" + tmonth + "-" + tday);
        }
      } else {
        log.debug3("no date found");
        return null;
      }
      log.debug3("date found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

  static private final NodeValue AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      NodeList elementChildren = node.getChildNodes();
      // only accept no children if this is a "string-name" node
      if (elementChildren == null &&
              !("string-name".equals(node.getNodeName()))) return null;

      String tsurname = null;
      String tgiven = null;
      String tprefix = null;

      if (elementChildren != null) {
        // perhaps pick up iso attr if it's available
        // look at each child
        for (int j = 0; j < elementChildren.getLength(); j++) {
          Node checkNode = elementChildren.item(j);
          String nodeName = checkNode.getNodeName();
          if ("surname".equals(nodeName)) {
            tsurname = checkNode.getTextContent();
          } else if ("given-names".equals(nodeName) ) {
            tgiven = checkNode.getTextContent();
          } else if ("prefix".equals(nodeName)) {
            tprefix = checkNode.getTextContent();
          }
        }
      } else {
        // we only fall here if the node is a string-name
        // no children - just get the plain text value
        tsurname = node.getTextContent();
      }

      // where to put the prefix?
      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, empty & whitespace only
      if (!StringUtils.isBlank(tsurname)) {
        valbuilder.append(tsurname);
        if (!StringUtils.isBlank(tgiven)) {
          valbuilder.append(AUTHOR_SEPARATOR + " " + tgiven);
        }
      } else {
        log.debug3("no author found");
        return null;
      }
      log.debug3("author found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

  private static final String RSC_Book = "/book/book-meta";

  private static final String title = "book-title-group/book-title";
  private static final String AUTHOR = "contrib-group/contrib/name";
  private static final String publisher = "publicationStmt/publisher";
  private static final String DATE = "pub-date";
  private static final String volume = "book-volume-number";

  private static final String eissn = "issn[@publication-format=\"electronic\"]";
  private static final String isbn = "isbn[@publication-format = \"print\"]";
  private static final String eisbn = "isbn[@publication-format = \"pdf\"]";
  private static final String pdf_file = "supplementary-material[@content-type= \"preview-pdf \"]/@xlink:href";
  private static final String doi = "book-id[@book-id-type = \"doi\"]";

  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String,XPathValue> RSC_articleMap =
          new HashMap<String,XPathValue>();
  static {
    RSC_articleMap.put(title, XmlDomMetadataExtractor.TEXT_VALUE);
    RSC_articleMap.put(eissn, XmlDomMetadataExtractor.TEXT_VALUE);
    RSC_articleMap.put(isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    RSC_articleMap.put(eisbn, XmlDomMetadataExtractor.TEXT_VALUE);
    RSC_articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    RSC_articleMap.put(doi, XmlDomMetadataExtractor.TEXT_VALUE);
    RSC_articleMap.put(volume, XmlDomMetadataExtractor.TEXT_VALUE);
    RSC_articleMap.put(AUTHOR, AUTHOR_VALUE);
    RSC_articleMap.put(DATE, DATE_VALUE);
  }

  static private final String RSC_bookNode = RSC_Book;

  static private final Map<String,XPathValue> RSC_globalMap = null;


  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(eissn, MetadataField.FIELD_EISSN);
    cookMap.put(isbn, MetadataField.FIELD_ISBN);
    cookMap.put(eisbn, MetadataField.FIELD_EISBN);
    cookMap.put(doi, MetadataField.FIELD_DOI);
    cookMap.put(title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(volume, MetadataField.FIELD_VOLUME);
    cookMap.put(AUTHOR, MetadataField.FIELD_AUTHOR);
    cookMap.put(DATE, MetadataField.FIELD_DATE);
  }


  /**
   * no global map - this is null
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return RSC_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return RSC_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return RSC_bookNode;
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
    return pdf_file;
  }

}
