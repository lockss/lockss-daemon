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

package org.lockss.plugin.clockss.edituraase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EdituraASEPublishingProceedingsHelper implements SourceXmlSchemaHelper {

    static Logger log = Logger.getLogger(EdituraASEPublishingProceedingsHelper.class);

    /*
        <?xml version="1.0" encoding="UTF-8"?>
        <record>
           <status>ok</status>
           <message-type>work</message-type>
           <message-version>1.0.0</message-version>
           <message>
              <indexed>
                 <date-parts>
                    <value>
                       <value>2024</value>
                    </value>
                    <value>
                       <value>9</value>
                    </value>
                    <value>
                       <value>18</value>
                    </value>
                 </date-parts>
                 <date-time>2024-09-18T08:10:01Z</date-time>
                 <timestamp>1726647001277</timestamp>
              </indexed>
              <reference-count>0</reference-count>
              <publisher>Editura ASE</publisher>
              <content-domain>
                 <crossmark-restriction>False</crossmark-restriction>
              </content-domain>
              <DOI>10.24818/icess/2024/001</DOI>
              <type>proceedings-article</type>
              <created>
                 <date-parts>
                    <value>
                       <value>2024</value>
                    </value>
                    <value>
                       <value>9</value>
                    </value>
                    <value>
                       <value>9</value>
                    </value>
                 </date-parts>
                 <date-time>2024-09-09T11:54:57Z</date-time>
                 <timestamp>1725882897000</timestamp>
              </created>
              <source>Crossref</source>
              <is-referenced-by-count>0</is-referenced-by-count>
              <title>
                 <value>Frontmatter ICESS 2024</value>
              </title>
              <prefix>10.24818</prefix>
              <member>10438</member>
              <published-online>
                 <date-parts>
                    <value>
                       <value>2024</value>
                    </value>
                    <value>
                       <value>9</value>
                    </value>
                    <value>
                       <value>18</value>
                    </value>
                 </date-parts>
              </published-online>
              <event>
                 <name>The International Conference on Economics and Social Sciences</name>
                 <acronym>ICESS</acronym>
              </event>
              <container-title>
                 <value>Proceedings of the International Conference on Economics and Social Sciences</value>
              </container-title>
              <deposited>
                 <date-parts>
                    <value>
                       <value>2024</value>
                    </value>
                    <value>
                       <value>9</value>
                    </value>
                    <value>
                       <value>18</value>
                    </value>
                 </date-parts>
                 <date-time>2024-09-18T07:00:35Z</date-time>
                 <timestamp>1726642835000</timestamp>
              </deposited>
              <score>1</score>
              <resource>
                 <primary>
                    <URL>https://www.icess.ase.ro/001-p4-frontmatter-icess-2024/</URL>
                 </primary>
              </resource>
              <issued>
                 <date-parts>
                    <value>
                       <value>2024</value>
                    </value>
                    <value>
                       <value>9</value>
                    </value>
                    <value>
                       <value>18</value>
                    </value>
                 </date-parts>
              </issued>
              <references-count>0</references-count>
              <URL>https://doi.org/10.24818/icess/2024/001</URL>
              <relation />
              <ISSN>
                 <value>2704-6524</value>
              </ISSN>
              <issn-type>
                 <type>print</type>
                 <value>2704-6524</value>
              </issn-type>
              <published>
                 <date-parts>
                    <value>
                       <value>2024</value>
                    </value>
                    <value>
                       <value>9</value>
                    </value>
                    <value>
                       <value>18</value>
                    </value>
                 </date-parts>
              </published>
           </message>
        </record>
     */

    private static final String AUTHOR_SEPARATOR = ",";

    static private final XmlDomMetadataExtractor.NodeValue AUTHOR_VALUE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue ofauthor");
            NodeList elementChildren = node.getChildNodes();
            // only accept no children if this is a "string-name" node
            if (elementChildren == null &&
                    !("string-name".equals(node.getNodeName()))) return null;

            String familyname = null;
            String tgiven = null;

            if (elementChildren != null) {
                // perhaps pick up iso attr if it's available
                // look at each child
                for (int j = 0; j < elementChildren.getLength(); j++) {
                    Node checkNode = elementChildren.item(j);
                    String nodeName = checkNode.getNodeName();
                    if ("family".equals(nodeName)) {
                        familyname = checkNode.getTextContent();
                    } else if ("given".equals(nodeName) ) {
                        tgiven = checkNode.getTextContent();
                    } 
                }
            } else {
                // we only fall here if the node is a string-name
                // no children - just get the plain text value
                familyname = node.getTextContent();
            }

            // where to put the prefix?
            StringBuilder valbuilder = new StringBuilder();
            //isBlank checks for null, empty & whitespace only
            if (!StringUtils.isBlank(familyname)) {
                valbuilder.append(familyname);
                if (!StringUtils.isBlank(tgiven)) {
                    valbuilder.append(AUTHOR_SEPARATOR + " " + tgiven);
                }
            } else {
                log.debug3("no author found");
                return null;
            }
            log.debug3("author found: " + valbuilder.toString());
            return valbuilder.toString();
        }
    };

    static private final XmlDomMetadataExtractor.NodeValue DATE_VALUE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue of JATS publishing date");
            NodeList elementChildren = node.getChildNodes();
            if (elementChildren == null) return null;

            String tyear = null;
            String tmonth = null;
            String tday = null;

            for (int j = 0; j < elementChildren.getLength(); j++) {
                Node child = elementChildren.item(j);
                String nodeName = child.getNodeName();

                if ("date-parts".equals(nodeName)) {
                    // Expect 3 <value> children, each with a <value> inside
                    NodeList dateParts = child.getChildNodes();
                    int valueCount = 0;
                    for (int k = 0; k < dateParts.getLength(); k++) {
                        Node outerValue = dateParts.item(k);
                        if ("value".equals(outerValue.getNodeName())) {
                            NodeList inner = outerValue.getChildNodes();
                            for (int m = 0; m < inner.getLength(); m++) {
                                Node innerValue = inner.item(m);
                                if ("value".equals(innerValue.getNodeName())) {
                                    String val = innerValue.getTextContent().trim();
                                    if (valueCount == 0) tyear = val;
                                    else if (valueCount == 1) tmonth = val;
                                    else if (valueCount == 2) tday = val;
                                    valueCount++;
                                }
                            }
                        }
                    }
                }
            }

            if (tyear == null) {
                log.debug3("no date found");
                return null;
            }

            StringBuilder valbuilder = new StringBuilder();
            valbuilder.append(tyear);
            if (tmonth != null && tday != null) {
                valbuilder.append("-").append(tmonth).append("-").append(tday);
            }

            String finalDate = valbuilder.toString();
            log.debug3("date found: " + finalDate);
            return finalDate;
        }
    };


        static private final XmlDomMetadataExtractor.NodeValue DOI_VALUE = new XmlDomMetadataExtractor.NodeValue(){
        @Override
        public String getValue(Node node) {
            return node.getTextContent().replace("https://doi.org/","").trim();
        }
    };

    protected static final String article_title = "//record/message/title";
    protected static final String journal_title = "//record/message/container-title";
    private static final String issn = "//record/message/ISSN";
    private static final String doi = "//record/message/DOI";
    private static final String author = "//record/message/author";

    // We decided in the meeting, we will use publisher-date as first choice, published-online as the second, and issue last
    private static final String date_choice_1 = "//record/message/published";
    private static final String date_choice_2 = "//record/message/published-online";
    private static final String date_choice_3 = "//record/message/issued";

    static private final Map<String,XPathValue>
            articleMap = new HashMap<String,XPathValue>();
    static {
        articleMap.put(article_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(journal_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(issn, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(author, AUTHOR_VALUE);
        articleMap.put(date_choice_1, DATE_VALUE);
        articleMap.put(date_choice_2, DATE_VALUE);
        articleMap.put(date_choice_3, DATE_VALUE);
        articleMap.put(doi, DOI_VALUE);
    }


    static private final Map<String,XPathValue> globalMap = null;

    protected static final MultiValueMap cookMap = new MultiValueMap();
    static {
        cookMap.put(article_title, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(journal_title, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(issn, MetadataField.FIELD_ISSN);
        cookMap.put(author, MetadataField.FIELD_AUTHOR);
        cookMap.put(date_choice_1, MetadataField.FIELD_DATE);
        cookMap.put(date_choice_2, MetadataField.FIELD_DATE);
        cookMap.put(date_choice_3, MetadataField.FIELD_DATE);
        cookMap.put(doi, MetadataField.FIELD_DOI);
    }

    @Override
    public Map<String, XPathValue> getGlobalMetaMap() {
        return null; //globalMap;
    }

    @Override
    public Map<String, XPathValue> getArticleMetaMap() {
        return articleMap;
    }
    @Override
    public String getArticleNode() {
        return null;
    }
    @Override
    public String getConsolidationXPathKey() {
        return null;
    }
    @Override
    public MultiValueMap getCookMap() {
        return cookMap;
    }
    @Override
    public String getDeDuplicationXPathKey() {
        return null;
    }
    @Override
    public String getFilenameXPathKey() {
        return null;
    }
}