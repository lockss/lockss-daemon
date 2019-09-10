/*
 * Copyright (c) 2019 Board of Trustees of Leland Stanford Jr. University,
 * all rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * STANFORD UNIVERSITY BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Stanford University shall not
 * be used in advertising or otherwise to promote the sale, use or other dealings
 * in this Software without prior written authorization from Stanford University.
 */

package org.lockss.plugin.clockss.emis;

import static org.lockss.plugin.clockss.JsonPathJsonMetadataParser.ARRAY_VALUE;
import static org.lockss.plugin.clockss.JsonPathJsonMetadataParser.STRING_VALUE;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections.map.MultiValueMap;

import org.lockss.extractor.MetadataField;
import org.lockss.plugin.clockss.JsonPathJsonMetadataParser.JsonPathValue;
import org.lockss.util.Logger;

public class EmisSourceJsonSchemaHelper implements
    org.lockss.plugin.clockss.SourceJsonSchemaHelper {

  private static final Logger log = Logger.getLogger(EmisSourceJsonSchemaHelper.class);

  private static final String AUTHOR_SEPARATOR = ",";

  /* 1.  MAP associating xpath with value type with evaluator */
  static private final Map<String, JsonPathValue> mif_articleMap = new HashMap<String, JsonPathValue>();
  static final String mif_pubname = "$[\"SE.TI\"]";
  static final String mif__volume = "$[\"IN.VO\"]";
  static final String mif_issue = "$[\"IN.IS\"]";
  static final String mif_issn = "$[\"SE.IS\"]";
  static final String mif_date = "$[\"IN.PY\"]";
  static final String mif_atitle = "$[\"DE.TI\"]";
  static final String mif_authors = "$[\"DE.AU\"]";
  static final String mif_aurl = "$[\"EM.EL\"]";
  /* 2. Each item (article) has its own JSON file */
  static private final String mif_articleNode = null;
  /* 3. no global information  one file/article */
  static private final Map<String, JsonPathValue> mif_globalMap = null;
  /*
   * The emitter will need a map to know how to cook ONIX raw values
   */
  private static final MultiValueMap mif_cookMap = new MultiValueMap();


  static {
    mif_articleMap.put(mif_pubname, STRING_VALUE);
    mif_articleMap.put(mif__volume, STRING_VALUE);
    mif_articleMap.put(mif_issue, STRING_VALUE);
    mif_articleMap.put(mif_issn, STRING_VALUE);
    mif_articleMap.put(mif_date, STRING_VALUE);
    mif_articleMap.put(mif_atitle, STRING_VALUE);
    mif_articleMap.put(mif_authors, ARRAY_VALUE);
    mif_articleMap.put(mif_aurl, STRING_VALUE);
  }

  static {
    mif_cookMap.put(mif_pubname, MetadataField.FIELD_PUBLICATION_TITLE);
    mif_cookMap.put(mif_atitle, MetadataField.FIELD_ARTICLE_TITLE);
    mif_cookMap.put(mif_aurl, MetadataField.FIELD_ACCESS_URL);
    mif_cookMap.put(mif_issn, MetadataField.FIELD_ISSN);
    mif_cookMap.put(mif__volume, MetadataField.FIELD_VOLUME);
    mif_cookMap.put(mif_issue, MetadataField.FIELD_ISSUE);
    mif_cookMap.put(mif_authors, MetadataField.FIELD_AUTHOR);
    mif_cookMap.put(mif_date, MetadataField.FIELD_DATE);
  }

  /**
   * Return the map for metadata that carries across all records in this JSON
   * schema. It can be null. <br/>
   * If null, only article level information is retrieved.
   */
  @Override
  public Map<String, JsonPathValue> getGlobalMetaMap() {
    return mif_globalMap;
  }

  /**
   * Return the map for metadata that is specific to each "article" record.
   */
  @Override
  public Map<String, JsonPathValue> getArticleMetaMap() {
    return mif_articleMap;
  }

  /**
   * Return the jsonpath string which defines one "article" record
   * It can be null.<br/>
   * If null, jsonpath matching starts at the root of the document.
   */
  @Override
  public String getArticleNode() {
    return mif_articleNode;
  }

  /**
   * Return the map to translate from raw metadata to cooked metadata
   * This must be set or no metadata gets emitted
   */
  @Override
  public MultiValueMap getCookMap() {
    return mif_cookMap;
  }

  /**
   * Return the jsonpath key for an item in the record that identifies a
   * unique article, even if multiple records provide information about
   * different manifestations of that article. For example, an ISBN13 will
   * be consistent, even if there are different records for each book
   * type (eg. pdf, epub, etc)
   * The key will be used to combine metadata from multiple records in to
   * one ArticleMetadata for all associated records.
   * It can be null - no record consolidation will happen.
   */
  @Override
  public String getDeDuplicationJsonKey() {
    return null;
  }

  /**
   * Used only in consolidateRecords()
   * Return the jsonpath key for the item in the record that should be combined
   * when multiple records are consolidated because the refer to the same
   * item. For example - when different formats of a book are described in
   * separate records, the format description would be the item to
   * consolidate when deduping.
   * It can be null. Consolidation may occur, but the record field will
   * not be combined.
   */
  @Override
  public String getConsolidationJsonKey() {
    return null;
  }

  /**
   * Used only in preEmitCheck() which may be overridden by a child
   * Return the jsonpath key to use an item from the raw metadata to build the
   * filename for a preEmitCheck. For example if the ISBN13 value is used
   * as the filename.
   * It can be null if the filename doesn't include metadata information.
   */
  @Override
  public String getFilenameJsonKey() {
    return null;
  }
}
