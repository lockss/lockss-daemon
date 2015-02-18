/*
 * $Id$
 */

/*

 Copyright (c) 2000-2014 Board of Trustees of Leland Stanford Jr. University,
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

package org.lockss.plugin.clockss;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;

import java.util.*;


/** Class to set up specific schema information for the
 * SourceXmlMetadataExtractor. A helper class defines the schema
 * and provides the information to the extractor via the get methods.
 */

public interface SourceXmlSchemaHelper {

  /**
   * Return the map for metadata that carries across all records in this XML
   * schema. It can be null. <br/>
   * If null, only article level information is retrieved.
   * @return
   */
  public Map<String,XPathValue> getGlobalMetaMap();

  /**
   * Return the map for metadata that is specific to each "article" record.
   * @return
   */
  public Map<String,XPathValue> getArticleMetaMap();

  /**
   * Return the xPath string which defines one "article" record
   * It can be null.<br/>
   * If null, xPath matching starts at the root of the document.
   * @return
   */
  public String getArticleNode();

  /**
   * Return the map to translate from raw metadata to cooked metadata
   * This must be set or no metadata gets emitted
   * @return
   */
  public MultiValueMap getCookMap();


  /**
   * Return the xPath key for an item in the record that identifies a 
   * unique article, even if multiple records provide information about 
   * different manifestations of that article. For example, an ISBN13 will
   * be consistent, even if there are different records for each book 
   * type (eg. pdf, epub, etc)
   * The key will be used to combine metadata from multiple records in to
   * one ArticleMetadata for all associated records. 
   * It can be null - no record consolidation will happen.
   * @return
   */
  public String getDeDuplicationXPathKey();

  /**
   * Used only in consolidateRecords() 
   * Return the xPath key for the item in the record that should be combined
   * when multiple records are consolidated because the refer to the same
   * item. For example - when different formats of a book are described in
   * separate records, the format description would be the item to 
   * consolidate when deduping.
   * It can be null. Consolidation may occur, but the record field will
   * not be combined.
   * @return
   */
  public String getConsolidationXPathKey();


  /**
   * Used only in preEmitCheck() which may be overridden by a child
   * Return the xPath key to use an item from the raw metadata to build the
   * filename for a preEmitCheck. For example if the ISBN13 value is used
   * as the filename.
   * It can be null if the filename doesn't include metadata information.
   * @return
   */
  public String getFilenameXPathKey();

}
