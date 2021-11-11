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
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

public class CrossRefQuerySchemaHelper implements SourceXmlSchemaHelper {
    private static final Logger log = Logger.getLogger(CrossRefQuerySchemaHelper.class);


    /*
     *  CrossRef query deliver schema - used by several publisher_xpaths
     */

    //<publication_date media_type="online">
    //  <month>01</month>
    //  <day>10</day>
    //  <year>2001</year>
    //</publication_date>
    static private final XmlDomMetadataExtractor.NodeValue FULL_DATE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue of CrossRef publication date");
            NodeList elementChildren = node.getChildNodes();
            if (elementChildren == null) return null;

            String tyear = null;
            String tday = null;
            String tmonth = null;
            // look at each child of the TitleElement for information
            for (int j = 0; j < elementChildren.getLength(); j++) {
                Node checkNode = elementChildren.item(j);
                String nodeName = checkNode.getNodeName();
                if ("day".equals(nodeName)) {
                    tday = checkNode.getTextContent();
                } else if ("month".equals(nodeName)) {
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


    //  <person_name sequence="first" contributor_role="author">
    //    <given_name>S</given_name>
    //    <surname>Hengsberger</surname>
    //  </person_name>
    static private final XmlDomMetadataExtractor.NodeValue AUTHOR_NAME = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {

            log.debug3("getValue of CrossRef person name");
            NodeList elementChildren = node.getChildNodes();
            if (elementChildren == null) return null;

            String tgiven = null;
            String tsurname = null;
            // look at each child of the TitleElement for information
            for (int j = 0; j < elementChildren.getLength(); j++) {
                Node checkNode = elementChildren.item(j);
                String nodeName = checkNode.getNodeName();
                if ("given_name".equals(nodeName)) {
                    tgiven = checkNode.getTextContent();
                } else if ("surname".equals(nodeName)) {
                    tsurname = checkNode.getTextContent();
                }
            }

            StringBuilder valbuilder = new StringBuilder();
            if (tsurname != null) {
                valbuilder.append(tsurname);
                if (tgiven != null) {
                    valbuilder.append(", " + tgiven);
                }
            } else {
                log.debug3("no name found");
                return null;
            }
            log.debug3("name found: " + valbuilder.toString());
            return valbuilder.toString();
        }
    };

    // this is global for all articles in the file
    private static final String publisher_xpath = "/crossref_result/query_result/body/query/doi_record/crossref/journal";

    private static final String publisher = "/crossref_result/query_result/body/query/crm-item[@name=\"publisher-name\"]";

    private static String pub_title = publisher_xpath + "/journal_metadata/full_title";
    private static String art_doi = publisher_xpath + "/journal_metadata/doi";
    public static String pub_year = publisher_xpath + "/journal_issue/publication_date/year";
    public static String art_sp = publisher_xpath + "/journal_article/pages/first_page";
    private static String art_lp = publisher_xpath + "/journal_article/pages/last_page";
    private static String art_contrib = publisher_xpath + "/journal_article/contributors/person_name";
    public static String art_resource = publisher_xpath + "/journal_article/doi_data/resource";
    private static String art_title = publisher_xpath + "/journal_article/titles/title";
    private static String pub_volume = publisher_xpath + "/journal_issue/journal_volume/volume";
    public static String pub_issue = publisher_xpath + "/journal_issue/issue";
    private static String art_date = publisher_xpath + "/journal_issue/publication_date";

    /*
     *  The following 3 variables are needed to use the XPathXmlMetadataParser
     */

    /* 1.  MAP associating xpath & value type definition or evaluator */
    static private final Map<String, XmlDomMetadataExtractor.XPathValue>
            articleMap = new HashMap<String, XmlDomMetadataExtractor.XPathValue>();

    static {
        articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_sp, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_lp, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_resource, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_contrib, AUTHOR_NAME);
        articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_date, FULL_DATE);
    }

    /* 2.  Top level per-article node */
    static private final String articleNode = null;

    /* 3. Global metadata is the publisher_xpath - work around if it gets troublesome */
    static private final Map<String, XmlDomMetadataExtractor.XPathValue>
            globalMap = new HashMap<String, XmlDomMetadataExtractor.XPathValue>();

    static {
        globalMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
    }

    /*
     * The emitter will need a map to know how to cook raw values
     */
    protected static final MultiValueMap cookMap = new MultiValueMap();

    static {
        // normal journal article schema
        cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
        cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(art_date, MetadataField.FIELD_DATE);
        cookMap.put(art_doi, MetadataField.FIELD_DOI);
        cookMap.put(art_sp, MetadataField.FIELD_START_PAGE);
        cookMap.put(art_lp, MetadataField.FIELD_END_PAGE);
        cookMap.put(art_contrib, MetadataField.FIELD_AUTHOR);
        cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
        cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);

    }

    /**
     * publisher_xpath comes from a global node
     */
    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getGlobalMetaMap() {
        //no globalMap, so returning null
        return globalMap;
    }

    /**
     * return  article paths representing metadata of interest
     */
    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
        return articleMap;
    }

    /**
     * Return the article node path
     */
    @Override
    public String getArticleNode() {
        return articleNode;
    }

    /**
     * Return a map to translate raw values to cooked values
     */
    @Override
    public MultiValueMap getCookMap() {
        return cookMap;
    }

    /**
     *
     */

    @Override
    public String getDeDuplicationXPathKey() {
        return null;
    }

    /**
     * Return the path for product form so when multiple records for the same
     * item are combined, the product forms are combined together
     */

    @Override
    public String getConsolidationXPathKey() {
        return null;
    }

    /**
     * using filenamePrefix (see above)
     */

    @Override
    public String getFilenameXPathKey() {
        return art_doi;
    }
}


