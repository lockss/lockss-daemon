/*

Copyright (c) 2000-2025, Board of Trustees of Leland Stanford Jr. University

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

package org.lockss.plugin.clockss.vhht;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.NodeValue;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VoprosyKhimiiSchemaHelper implements SourceXmlSchemaHelper{

    private static final Logger log = Logger.getLogger(VoprosyKhimiiSchemaHelper.class);

    private static final String AUTHOR_SEPARATOR = ",";

    static private final NodeValue VOPROSY_AUTHOR_VALUE = new NodeValue() {
        @Override
        public String getValue(Node node) {
            log.debug3("getValue of voprosy author");
            NodeList elementChildren = node.getChildNodes();
            if (elementChildren == null) return null;
            
            String lastname = null;
            String firstname = null;

            // look at each child 
            for (int j = 0; j < elementChildren.getLength(); j++) {
                Node checkNode = elementChildren.item(j);
                String nodeName = checkNode.getNodeName();
                if ("lastname".equals(nodeName)) {
                    lastname = checkNode.getTextContent();
                } else if ("firstname".equals(nodeName) ) {
                    firstname = checkNode.getTextContent();
                }
            }
            StringBuilder valbuilder = new StringBuilder();
            //isBlank checks for null, empty & whitespace only
            if (!StringUtils.isBlank(lastname)) {
                valbuilder.append(lastname);
                if (!StringUtils.isBlank(firstname)) {
                valbuilder.append(AUTHOR_SEPARATOR + " " + firstname);
                }
            } else {
                log.debug3("no author found");
                return null;
            }
            log.debug3("author found: " + valbuilder.toString());
            return valbuilder.toString();
        }
    };

    // this is global for all articles in the file
    private static final String publisher = "/article/publisher";
    private static final String path = "/article/file_name";
    private static final String journal_title = "/article/journal_title";
    private static final String issn = "/article/issn_print";
    private static final String eissn = "/article/issn_online";
    private static final String volume = "/article/volume";
    private static final String issue = "/article/issue";
    private static final String date_published = "/article/publication_date";
    private static final String title = "/article/article_title";
    private static final String doi = "/article/doi";
    private static final String authors = "/article/authors/author";

    /*
     *  The following 3 variables are needed to use the XPathXmlMetadataParser
     */

    /* 1.  MAP associating xpath & value type definition or evaluator */
    static private final Map<String, XmlDomMetadataExtractor.XPathValue>
            articleMap = new HashMap<String, XmlDomMetadataExtractor.XPathValue>();

    static {
        articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(path, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(journal_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(issn, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(eissn, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(volume, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(issue, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(date_published, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(doi, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(authors, VOPROSY_AUTHOR_VALUE);
    }

    /* 2.  Top level per-article node */
    static private final String articleNode = "/article";

    /* 3. Global metadata is the publisher_xpath - work around if it gets troublesome */

    static private final Map<String, XPathValue> 
        globalMap = new HashMap<String,XPathValue>();
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
        cookMap.put(path, MetadataField.FIELD_ACCESS_URL);
        cookMap.put(journal_title, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(issn, MetadataField.FIELD_ISSN);
        cookMap.put(eissn, MetadataField.FIELD_EISSN);
        cookMap.put(volume, MetadataField.FIELD_VOLUME);
        cookMap.put(date_published, MetadataField.FIELD_DATE);
        cookMap.put(title, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(doi, MetadataField.FIELD_DOI);
        cookMap.put(authors, MetadataField.FIELD_AUTHOR);
    }

    /**
     * publisher_xpath comes from a global node
     */
    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getGlobalMetaMap() {
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
        return path;
    }
}
