/*

Copyright (c) 2000-2022, Board of Trustees of Leland Stanford Jr. University
All rights reserved.

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

package org.lockss.plugin.clockss.iop;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.*;
import org.lockss.extractor.*;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;

/**
 *  Schema helper for IOP's legacy ".article" schema
 *  @author alexohlson
 */
public class IopArticleXmlSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(IopArticleXmlSchemaHelper.class);

  private static final String IOP_article = "/header";
  /* these are all relative to the /header node */
  private static final String IOP_issn = "ident/issn";
  private static final String IOP_volume = "ident/volume";
  private static final String IOP_issue = "ident/issue";
  private static final String IOP_doi = "ident/doi";
  private static final String IOP_startpage = "ident/pages/@start";
  private static final String IOP_endpage = "ident/pages/@end";

  // this xml has no publication level title other than ISSN
  private static final String IOP_title = "title/title_full";
  private static final String IOP_author = "authors/author";
  private static final String IOP_pubdate = "dates/date_cover";
  


  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  private static final Map<String,XPathValue> IOP_articleMap = 
      new HashMap<String,XPathValue>();
  static {
	  IOP_articleMap.put(IOP_issn, XmlDomMetadataExtractor.TEXT_VALUE);
	  IOP_articleMap.put(IOP_volume, XmlDomMetadataExtractor.TEXT_VALUE);
	  IOP_articleMap.put(IOP_issue, XmlDomMetadataExtractor.TEXT_VALUE);
	  IOP_articleMap.put(IOP_doi, XmlDomMetadataExtractor.TEXT_VALUE);
	  IOP_articleMap.put(IOP_startpage, XmlDomMetadataExtractor.TEXT_VALUE);
	  IOP_articleMap.put(IOP_endpage, XmlDomMetadataExtractor.TEXT_VALUE);
	  IOP_articleMap.put(IOP_title, XmlDomMetadataExtractor.TEXT_VALUE);
	  IOP_articleMap.put(IOP_author, XmlDomMetadataExtractor.TEXT_VALUE);
	  IOP_articleMap.put(IOP_pubdate, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  private static final String IOP_articleNode = IOP_article; 

  /* 3. in MARCXML there is no global information because one file/article */
  private static final Map<String,XPathValue> IOP_globalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {
	  // do NOT cook publisher_name; get from TDB file for consistency
	  //cookMap.put(IOP_title, MetadataField.FIELD_PUBLICATION_TITLE);
	  cookMap.put(IOP_issn, MetadataField.FIELD_ISSN);
	  cookMap.put(IOP_volume, MetadataField.FIELD_VOLUME);
	  cookMap.put(IOP_issue, MetadataField.FIELD_ISSUE);
	  cookMap.put(IOP_title, MetadataField.FIELD_ARTICLE_TITLE);
	  cookMap.put(IOP_doi, MetadataField.FIELD_DOI);
	  cookMap.put(IOP_startpage, MetadataField.FIELD_START_PAGE);
	  cookMap.put(IOP_endpage, MetadataField.FIELD_END_PAGE);
	  cookMap.put(IOP_author, MetadataField.FIELD_AUTHOR);
	  cookMap.put(IOP_pubdate, MetadataField.FIELD_DATE);
  }


  /**
   * MARCXML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return IOP_globalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return IOP_articleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return IOP_articleNode;
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
