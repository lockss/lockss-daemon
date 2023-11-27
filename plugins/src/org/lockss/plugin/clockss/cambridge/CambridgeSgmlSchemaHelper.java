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

package org.lockss.plugin.clockss.cambridge;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  cambridge legacy SGML metadata files
 *  
 */
public class CambridgeSgmlSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(CambridgeSgmlSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";
  
  /* 
   * AUTHOR INFORMATION
   * <au>
   *   <fnms>GUSTAF</fnms>
   *   <snm>ARRHENIUS</snm>
   *   <norm>Arrhenius G</norm>
   *   <orf rid="a1">
   * </au>
   */
  static private final NodeValue CAMB_AUTHOR_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of sgml author");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;
      
      String tsurname = null;
      String tgiven = null;
      // look at each child 
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("snm".equals(nodeName)) {
          tsurname = checkNode.getTextContent();
        } else if ("fnms".equals(nodeName) ) {
          tgiven = checkNode.getTextContent();
        } 
      }

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

  /* 
   *  Cambridge SGML specific XPATH key definitions that we care about
   */
  // The following are all under the article node
  private static String pub_name = "issue/pinfo/pnm";
  private static String pub_jid = "issue/jinfo/jid";
  private static String pub_title = "issue/jinfo/jtl";
  private static String pub_issn = "issue/jinfo/issn";
  private static String pub_eissn = "issue/jinfo/eissn";
  private static String pub_volume = "issue/pubinfo/vid";
  private static String pub_issue = "issue/pubinfo/iid";
  private static String pub_year = "issue/pubinfo/cd/@year";
  // article level
  // The title that does not have the purpose attribute set is the choice
  private static String art_title = "artcon/genhdr/tig/atl[not(@purpose)]";
  private static String art_auth = "artcon/genhdr/aug/au";
  private static String art_doi = "artcon/genhdr/artinfo/doi";
  private static String art_sp = "artcon/genhdr/artinfo/ppf";
  private static String art_lp = "artcon/genhdr/artinfo/ppl";

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  camb_articleMap = new HashMap<String,XPathValue>();
  static {
    camb_articleMap.put(pub_name, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_jid, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_issn, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_eissn, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_lp, XmlDomMetadataExtractor.TEXT_VALUE); 
    camb_articleMap.put(art_auth, CAMB_AUTHOR_VALUE);

  }

  /* 2.  Top level Nodepath */
  //static private final String camb_articleNode = "/article/header";
  // could be /article/header or /header...
  static private final String camb_articleNode = "(/article/header | /header)"; 

  /* 3. WK global value we care about: none, so WK_globalMap is null */ 

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(pub_issn, MetadataField.FIELD_ISSN);
    cookMap.put(pub_eissn, MetadataField.FIELD_EISSN);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(art_doi, MetadataField.FIELD_DOI);
    cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
    cookMap.put(art_lp, MetadataField.FIELD_END_PAGE);
    cookMap.put(art_auth, MetadataField.FIELD_AUTHOR);
  }

  /**
   * WK has a single global information outside of article records
   * return global map
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    //no globalMap, so returning null
    return null;
  }

  /**
   * return  article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return camb_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return camb_articleNode;
  }

  /**
   * Return a map to translate raw values to cooked values
   */
  @Override
  public MultiValueMap getCookMap() {
    return cookMap;
  }

  /**
   */

  @Override
  public String getDeDuplicationXPathKey() {
    return null;
  }

  /**
   * Return the path for product form so when multiple records for the same
   * item are combined, the product forms are combined together
   */

  @Override
  public String getConsolidationXPathKey() {
    return null;
  }

  /**
   * using filenamePrefix (see above)
   */

  @Override
  public String getFilenameXPathKey() {
    return null;
  }

}