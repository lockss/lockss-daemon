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

package org.lockss.plugin.clockss.igpublishing;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

import java.util.HashMap;
import java.util.Map;

public class IGPublishingSchemaHelper
implements SourceXmlSchemaHelper {
  private static final Logger log = Logger.getLogger(IGPublishingSchemaHelper.class);

  private static final String IGParticle = "/metadata";
  private static final String IGPisbn = "/metadata/isbn";
  private static final String IGPeisbn = "/metadata/eisbn";
  private static final String IGPpublisher = "/metadata/publisher";
  private static final String IGPtitle = "/metadata/title";
  private static final String IGPauthor = "/metadata/author";
  private static final String IGPpubdate = "/metadata/yop";
  
  /*
   *  The following 3 variables are needed to construct the XPathXmlMetadataParser
   */

  /* 1.  MAP associating xpath with value type with evaluator */
  private static final Map<String,XPathValue> IGParticleMap = 
      new HashMap<String,XPathValue>();
  static {
      IGParticleMap.put(IGPisbn, XmlDomMetadataExtractor.TEXT_VALUE);
      IGParticleMap.put(IGPeisbn, XmlDomMetadataExtractor.TEXT_VALUE);
      IGParticleMap.put(IGPpublisher, XmlDomMetadataExtractor.TEXT_VALUE);
      IGParticleMap.put(IGPtitle, XmlDomMetadataExtractor.TEXT_VALUE);
      IGParticleMap.put(IGPauthor, XmlDomMetadataExtractor.TEXT_VALUE);
      IGParticleMap.put(IGPpubdate, XmlDomMetadataExtractor.TEXT_VALUE);
      IGParticleMap.put(IGPpublisher, XmlDomMetadataExtractor.TEXT_VALUE);
  }

  /* 2. Each item (book) has its own XML file */
  private static final String IGParticleNode = IGParticle; 

  /* 3. in MARCXML there is no global information because one file/article */
  private static final Map<String,XPathValue> IGPglobalMap = null;

  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap cookMap = new MultiValueMap();
  static {

      cookMap.put(IGPisbn, MetadataField.FIELD_ISBN);
      cookMap.put(IGPeisbn, MetadataField.FIELD_EISBN);
      //cookMap.put(ONIX_idtype_doi, MetadataField.FIELD_DOI);
      cookMap.put(IGPpublisher, MetadataField.FIELD_PUBLICATION_TITLE);
      cookMap.put(IGPtitle, MetadataField.FIELD_ARTICLE_TITLE);
      cookMap.put(IGPauthor, MetadataField.FIELD_AUTHOR);
      cookMap.put(IGPpubdate, MetadataField.FIELD_DATE);
      cookMap.put(IGPpublisher, MetadataField.FIELD_PUBLISHER);
  }


  /**
   * MARCXML does not contain needed global information outside of article records
   * return NULL
   */
  @Override
  public Map<String, XPathValue> getGlobalMetaMap() {
    return IGPglobalMap;
  }

  /**
   * return NAP article map to identify xpaths of interest
   */
  @Override
  public Map<String, XPathValue> getArticleMetaMap() {
    return IGParticleMap;
  }

  /**
   * Return the article node path
   */
  @Override
  public String getArticleNode() {
    return IGParticleNode;
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
