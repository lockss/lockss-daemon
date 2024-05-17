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

package org.lockss.plugin.clockss.hmp;

import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.plugin.clockss.PubMedSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HmpGlobalSchemaHelper extends PubMedSchemaHelper{

    private static final Logger log = Logger.getLogger(HmpGlobalSchemaHelper.class);

    static private final NodeValue AUTHOR_NAME = new NodeValue() {
        @Override
        public String getValue(Node node) {
    
          log.debug3("getValue of PubMed author name");
          NodeList elementChildren = node.getChildNodes();
          if (elementChildren == null) return null;
    
          String tgiven = null;
          String tsurname = null;
          String tmidname = null;
          // look at each child of the TitleElement for information                                                                                                                                     
          for (int j = 0; j < elementChildren.getLength(); j++) {
            Node checkNode = elementChildren.item(j);
            String nodeName = checkNode.getNodeName();
            if ("FirstName".equals(nodeName)) {
              tgiven = checkNode.getTextContent();
            } else if ("LastName".equals(nodeName) ) {
              tsurname = checkNode.getTextContent();
            } else if ("MiddleName".equals(nodeName) ) {
              tmidname = checkNode.getTextContent();
            }
          }
    
          StringBuilder valbuilder = new StringBuilder();
          if (tsurname != null) {
            valbuilder.append(tsurname);
            if (tgiven != null) {
              valbuilder.append(", " + tgiven);
              if (tmidname != null) {
                valbuilder.append(" " + tmidname);
              }
            }
          } else {
            log.debug3("no name found");
            return null;
          }
          log.debug3("name found: " + valbuilder.toString());
          return valbuilder.toString();
        }
      };
    
    static protected final String art_doi = "ELocationID[@EIdType = \"doi\"]";
    protected static String art_title = "ArticleTitle";
    protected static String art_contrib = "AuthorList/Author";

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
        Map<String, XmlDomMetadataExtractor.XPathValue> articleMap = super.getArticleMetaMap();
        articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_contrib, AUTHOR_NAME);
        return articleMap;
    }

    @Override
    public MultiValueMap getCookMap() {
        MultiValueMap theCookMap = super.getCookMap();
        theCookMap.put(art_doi, MetadataField.FIELD_DOI);
        return theCookMap;
    }
}
