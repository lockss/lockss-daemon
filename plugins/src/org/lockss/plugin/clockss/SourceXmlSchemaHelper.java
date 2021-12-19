/*

Copyright (c) 2000-2021, Board of Trustees of Leland Stanford Jr. University
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
