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

package org.lockss.plugin.clockss.nature;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  A helper class that defines a schema for Nature Publishing Group
 *  XML files (NPG DTD)
 *  @author alexohlson
 */
public class NpgXmlSchemaHelper
implements SourceXmlSchemaHelper {
  static Logger log = Logger.getLogger(NpgXmlSchemaHelper.class);
  
  private static final String AUTHOR_SEPARATOR = ",";

  /* 
   * AUTHOR INFORMATION
   * <au><fnm>Xerxes</fnm><snm>Pundole</snm><inits>X</inits><orf rid="a1"/></au>
   */
  static private final NodeValue AUTHOR_VAL = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.debug3("getValue of npg author");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;
      
      String sn = null;
      String fn = null;
      String inits = null;
      // look at each child 
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("snm".equals(nodeName)) {
          sn = checkNode.getTextContent();
        } else if ("fnm".equals(nodeName) ) {
          fn = checkNode.getTextContent();
        } else if ("inits".equals(nodeName) ) {
          inits = checkNode.getTextContent();
        } 
      }

      StringBuilder valbuilder = new StringBuilder();
      //isBlank checks for null, empty & whitespace only
      if (!StringUtils.isBlank(sn)) {
        valbuilder.append(sn);
        if (!StringUtils.isBlank(fn) || !StringUtils.isBlank(inits)) {
          valbuilder.append(AUTHOR_SEPARATOR);
          // only use the inits if the fname is blank...
          if (!StringUtils.isBlank(fn)) {
            valbuilder.append(" " + fn);
          } else if (!StringUtils.isBlank(inits)) {
          valbuilder.append(" " + inits);
          }
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
   *  NPG XML specific XPATH key definitions that we care about
   */
    private static final String NPG_issn = "pubfm/issn";
    private static final String NPG_ptitle = "pubfm/jtl";
    private static final String NPG_publisher = "pubfm/cpg/cpn"; 
    public static final String NPG_copyyear = "pubfm/cpg/cpy"; 
    private static final String NPG_volume = "pubfm/vol";
    private static final String NPG_issue = "pubfm/issue";
    private static final String NPG_atitle = "fm/atl";
    private static final String NPG_doi = "pubfm/doi";
    private static final String NPG_author = "(fm/aug/cau | fm/aug/au)";
    private static final String NPG_pub_year = "fm/pubdate";

    static private final Map<String,XPathValue> NPG_articleMap = 
      new HashMap<String,XPathValue>();
  static {
    NPG_articleMap.put(NPG_issn, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_ptitle, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_volume, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_issue, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_atitle, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_doi, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_author, AUTHOR_VAL);
    NPG_articleMap.put(NPG_pub_year, XmlDomMetadataExtractor.TEXT_VALUE);
    NPG_articleMap.put(NPG_copyyear, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  static private final String NPG_article = "/article"; 

  /* 3. in MARCXML there is no global information because one file/article */
  static private final Map<String,XPathValue> NPG_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // do NOT cook publisher_name; get from TDB file for consistency
    cookMap.put(NPG_issn, MetadataField.FIELD_ISSN);
    cookMap.put(NPG_ptitle, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(NPG_atitle, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(NPG_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(NPG_doi, MetadataField.FIELD_DOI);
    cookMap.put(NPG_author, MetadataField.FIELD_AUTHOR);
    cookMap.put(NPG_publisher, MetadataField.FIELD_PUBLISHER);
    cookMap.put(NPG_pub_year, MetadataField.FIELD_DATE);
  }


  /**
   * NPGXML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return NPG_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return NPG_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return NPG_article;
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
