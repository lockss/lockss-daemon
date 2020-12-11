/*
 * $Id$
 */

/*

 Copyright (c) 2000-2019 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss.iop;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 *  Schema helper for IOP's legacy ".article" schema
 *  @author alexohlson
 */
public class IopCustomizedArticleXmlSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(IopCustomizedArticleXmlSchemaHelper.class);

  private static final String IOP_article = "/article";
  /* these are all relative to the /header node */
  private static final String IOP_issn = "/article/article-metadata/jnl-data/jnl-issn";
  private static final String IOP_volume = "/article/article-metadata/volume-data/volume-number";
  private static final String IOP_issue = "/article/article-metadata/issue-data/issue-number";
  private static final String IOP_doi = "/article/article-metadata/article-data/doi";
  private static final String IOP_startpage = "/article/article-metadata/article-data/first-page";
  private static final String IOP_endpage = "/article/article-metadata/article-data/last-page";

  // this xml has no publication level title other than ISSN
  private static final String IOP_title = "/article/article-metadata/jnl-data/jnl-fullname";
  private static final String IOP_author = "/article/header/author-group/author";
  private static final String IOP_pubdate = "/article/article-metadata/issue-data/coverdate";
  


  
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
