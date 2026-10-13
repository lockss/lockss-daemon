/*

Copyright (c) 2000-2026, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.permanyer;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.util.Logger;

/**
 * Permanyer delivers a single, attribute-less ISSN in journal-meta:
 *
 *   <journal-meta>
 *     ...
 *     <issn>0120-5633</issn>
 *   </journal-meta>
 *
 * The base JatsPublishingSchemaHelper already extracts that node (xpath
 * "front/journal-meta/issn[not(@*)]") but only cooks it to FIELD_ISSN, so
 * FIELD_EISSN is never set. This helper reuses everything from the base
 * class and only adds a cook mapping of the bare ISSN to FIELD_EISSN.
 */
public class PermanyerJatsXmlSchemaHelper extends JatsPublishingSchemaHelper {

  private static final Logger log = Logger.getLogger(PermanyerJatsXmlSchemaHelper.class);

  /*
   * Must be character-for-character identical to the (private) JATS_issn
   * key in JatsPublishingSchemaHelper, because raw values are keyed by the
   * xpath string. The base class already puts this xpath in the article map,
   * so we don't need to override getArticleMetaMap().
   */
  public static final String PERMANYER_issn = "front/journal-meta/issn[not(@*)]";

  /*
   * Set to false if the bare <issn> should be cooked ONLY as the e-ISSN
   * (i.e. remove the base class's FIELD_ISSN mapping for it).
   */
  private static final boolean KEEP_AS_PRINT_ISSN_TOO = true;

  private static MultiValueMap permanyerCookMap = null;

  @Override
  public MultiValueMap getCookMap() {
    if (permanyerCookMap == null) {
      MultiValueMap map = new MultiValueMap();
      // copy every key -> field mapping from the base JATS helper
      map.putAll(super.getCookMap());
      if (!KEEP_AS_PRINT_ISSN_TOO) {
        map.remove(PERMANYER_issn, MetadataField.FIELD_ISSN);
      }
      map.put(PERMANYER_issn, MetadataField.FIELD_EISSN);
      log.debug3("Permanyer cook map: " + map);
      permanyerCookMap = map;
    }
    return permanyerCookMap;
  }
}
