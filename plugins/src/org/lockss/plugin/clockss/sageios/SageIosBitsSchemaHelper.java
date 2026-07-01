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
package org.lockss.plugin.clockss.sageios;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.associationforcomputingmachinery.ACMBitsPublishingSchemaHelper;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

public class SageIosBitsSchemaHelper implements SourceXmlSchemaHelper{

    private static final Logger log = Logger.getLogger(SageIosBitsSchemaHelper.class);
    private static final String AUTHOR_SEPARATOR = ",";

        static private final NodeValue BITS_AUTHOR_VALUE = new NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue of BITS author");
            NodeList elementChildren = node.getChildNodes();
            // only accept no children if this is a "name" node
            if (elementChildren == null &&
                    !("name".equals(node.getNodeName()))) return null;

            String tsurname = null;
            String tgiven = null;
            String tprefix = null;

            if (elementChildren != null) {
                // perhaps pick up iso attr if it's available
                // look at each child
                for (int j = 0; j < elementChildren.getLength(); j++) {
                    Node checkNode = elementChildren.item(j);
                    String nodeName = checkNode.getNodeName();
                    if ("surname".equals(nodeName)) {
                        tsurname = checkNode.getTextContent();
                    } else if ("given-names".equals(nodeName) ) {
                        tgiven = checkNode.getTextContent();
                    } else if ("prefix".equals(nodeName)) {
                        tprefix = checkNode.getTextContent();
                    }
                }
            } else {
                // we only fall here if the node is a string-name
                // no children - just get the plain text value
                tsurname = node.getTextContent();
            }

            // where to put the prefix?
            StringBuilder valbuilder = new StringBuilder();
            //isBlank checks for null, empty & whitespace only
            if (!StringUtils.isBlank(tsurname)) {
                valbuilder.append(tsurname);
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

        static private final NodeValue BITS_DATE_VALUE = new NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue of BITS publishing date");
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

    /*
     *  BITS specific XPATH key definitions that we care about
     */
    private static String BITS_book = "/book";
    private static String BITS_series_title = "/book/collection-meta/title-group/title";
    private static String BITS_eissn = "/book/collection-meta/issn[@publication-format=\"electronic\"]";
    private static String BITS_issn = "/book/collection-meta/issn[@publication-format=\"print\"]";
    private static String BITS_eisbn = "/book/book-meta/isbn[@publication-format=\"electronic\"]";
    private static String BITS_contrib = "/book/book-meta/contrib-group/contrib/name";
    private static String BITS_doi = "/book/book-meta/book-id[@book-id-type=\"doi\"]";
    private static String BITS_book_title =  "/book/book-meta/book-title-group/book-title";
    private static String BITS_volume =  "/book/book-meta/book-volume-number";
    private static String BITS_pub =  "/book/book-meta/publisher/publisher-name";
    private static String BITS_pub_date =  "/book/book-meta/pub-date[@date-type=\"pub\"]";

    
    /*
     *  The following 3 variables are needed to construct the XPathXmlMetadataParser
     */

    /* 1.  MAP associating xpath with value type with evaluator */
    static private final Map<String, XPathValue> BITS_articleMap =
            new HashMap<String, XmlDomMetadataExtractor.XPathValue>();
    static {
        BITS_articleMap.put(BITS_series_title, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_eissn, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_issn, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_eisbn, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_contrib, BITS_AUTHOR_VALUE);
        BITS_articleMap.put(BITS_doi, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_book_title, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_volume, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_pub, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_pub_date, BITS_DATE_VALUE);
    }

    /* 2. Each item (book) has its own XML file */
    static private final String BITS_articleNode = BITS_book;

    /* 3. in BITS there is no global information because one file/article */
    static private final Map<String, XPathValue> BITS_globalMap = null;

    private static final MultiValueMap cookMap = new MultiValueMap();
    static {
        cookMap.put(BITS_series_title, MetadataField.FIELD_SERIES_TITLE);
        cookMap.put(BITS_eissn, MetadataField.FIELD_EISSN);
        cookMap.put(BITS_issn, MetadataField.FIELD_ISSN);
        cookMap.put(BITS_eisbn, MetadataField.FIELD_EISBN);
        cookMap.put(BITS_contrib, MetadataField.FIELD_AUTHOR);
        cookMap.put(BITS_doi, MetadataField.FIELD_DOI);
        cookMap.put(BITS_book_title, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(BITS_volume, MetadataField.FIELD_VOLUME);
        cookMap.put(BITS_pub, MetadataField.FIELD_PUBLISHER);
        cookMap.put(BITS_pub_date, MetadataField.FIELD_DATE);
    }


    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getGlobalMetaMap() {
        return BITS_globalMap;
    }

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
        return BITS_articleMap;
    }

    @Override
    public String getArticleNode() {
        return BITS_articleNode;
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
    public String getConsolidationXPathKey() {
        return null;
    }

    @Override
    public String getFilenameXPathKey() {
        return null;
    }
}
