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

package org.lockss.plugin.clockss.phildoc;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;

/**
 *  A helper class that defines a schema for XML metadata extraction for
 *  Philosophy Documentation Center issue TOC XML files
 *  
 */
public class PhilDocSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(PhilDocSchemaHelper.class);


  /* 
   *  Philosophy Documentation Center 
   */
  // The following are all under the article node /doc
  public static final String pub_title = "field[@name=\"publication\"]";
  public static final String pub_issn = "field[@name=\"issn\"]";
  public static final String pub_eissn = "field[@name=\"onlineissn\"]";
  public static final String pub_isbn = "field[@name=\"isbn\"]";
  public static final String art_title = "field[@name=\"title\"]";
  public static final String art_subtitle = "field[@name=\"subtitle\"]";
  public static final String pub_volume = "field[@name=\"volume\"]";
  public static final String pub_issue = "field[@name=\"issue\"]";
  public static final String pub_year = "field[@name=\"year\"]";
  public static final String art_auth = "field[@name=\"author\"]";
  public static final String art_sp = "field[@name=\"pagenumber_first\"]";
  public static final String art_lp = "field[@name=\"pagenumber_last\"]";
  public static final String art_imuse_id = "field[@name=\"imuse_id\"]";  
  public static final String art_pdfname = "field[@name=\"pdfname\"]";  
  public static final String art_xmlname = "field[@name=\"xmlname\"]";
  public static final String art_doi = "field[@name=\"DOI\"]";

  /*
   *  The following 3 variables are needed to use the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath & value type definition or evaluator */
  static private final Map<String,XPathValue>     
  pdoc_articleMap = new HashMap<String,XPathValue>();
  static {
    pdoc_articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE);
    pdoc_articleMap.put(pub_issn, XmlDomMetadataExtractor.TEXT_VALUE);
    pdoc_articleMap.put(pub_eissn, XmlDomMetadataExtractor.TEXT_VALUE);
    pdoc_articleMap.put(pub_isbn, XmlDomMetadataExtractor.TEXT_VALUE);
    pdoc_articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE);
    pdoc_articleMap.put(art_subtitle, XmlDomMetadataExtractor.TEXT_VALUE);
    pdoc_articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE);
    pdoc_articleMap.put(art_auth, XmlDomMetadataExtractor.TEXT_VALUE);
    pdoc_articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_lp, XmlDomMetadataExtractor.TEXT_VALUE);
    pdoc_articleMap.put(art_imuse_id, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_pdfname, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_xmlname, XmlDomMetadataExtractor.TEXT_VALUE); 
    pdoc_articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE); 
  }

  /* 2.  Top level per-article node */
  static private final String pdoc_articleNode = "/add/doc";

  /* 3. Global metadata is null */
  static private final Map<String, XPathValue> pdoc_globalNode = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  protected static final MultiValueMap cookMap = new MultiValueMap();
  static {
    // normal journal article schema
    cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
    cookMap.put(pub_issn, MetadataField.FIELD_ISSN);
    cookMap.put(pub_eissn, MetadataField.FIELD_EISSN);
    cookMap.put(pub_isbn, MetadataField.FIELD_ISBN);
    cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
    cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
    cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
    cookMap.put(pub_year, MetadataField.FIELD_DATE);
    cookMap.put(art_auth, MetadataField.FIELD_AUTHOR);
    cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
    cookMap.put(art_lp, MetadataField.FIELD_END_PAGE);
    cookMap.put(art_doi, MetadataField.FIELD_DOI);
  }

  /**
   * PhilDoc has no global metadata
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    //no globalMap, so returning null
    return pdoc_globalNode; 
  }

  /**
   * return  article paths representing metadata of interest  
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return pdoc_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return pdoc_articleNode;
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