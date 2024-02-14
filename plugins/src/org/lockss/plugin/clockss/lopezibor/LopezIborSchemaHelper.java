/*
Copyright (c) 2000-2024, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.lopezibor;

import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LopezIborSchemaHelper extends JatsPublishingSchemaHelper {
    private static final Logger log = Logger.getLogger(LopezIborSchemaHelper.class);
    
    static protected final String JATS_date = "front/article-meta/pub-date[@pub-type=\"epublish\"]";
    static protected final String JATS_eissn = "/issn[@pub-type = \"epub\" or @publication-format=\"online-only\"]";

    static private final NodeValue JATS_DATE_VALUE = new NodeValue() {
    @Override
    public String getValue(Node node) {

      log.info("getValue of JATS publishing date");
      NodeList elementChildren = node.getChildNodes();
      if (elementChildren == null) return null;
      
      // perhaps pick up iso attr if it's available 
      String tyear = null;
      String tday = null;
      String tmonth = null;
      // look at each child of the TitleElement for information
      for (int j = 0; j < elementChildren.getLength(); j++) {
        Node checkNode = elementChildren.item(j);
        String nodeName = checkNode.getNodeName();
        if ("day".equals(nodeName)) {
          tday = checkNode.getTextContent();
        } else if ("month".equals(nodeName) ) {
          tmonth = checkNode.getTextContent();
        } else if ("year".equals(nodeName)) {
          tyear = checkNode.getTextContent();
        }
      }

      StringBuilder valbuilder = new StringBuilder();
      if (tyear != null) {
        valbuilder.append(tyear);
        if (tday != null && tmonth != null) {
          valbuilder.append("-" + tmonth + "-" + tday);
        }
      } else {
        log.debug3("no date found");
        return null;
      }
      log.debug3("date found: " + valbuilder.toString());
      return valbuilder.toString();
    }
  };

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {

        Map<String, XmlDomMetadataExtractor.XPathValue> JATS_articleMap = super.getArticleMetaMap();
        JATS_articleMap.put(JATS_date, JATS_DATE_VALUE);
        //JATS_articleMap.put(JATS_eissn, JATS_ISSN_VALUE);
        return JATS_articleMap;
    }

    @Override
    public MultiValueMap getCookMap() {
        MultiValueMap theCookMap = super.getCookMap();
        theCookMap.put(JATS_date, MetadataField.FIELD_DATE);
        return theCookMap;
    }
}
