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

package org.lockss.plugin.clockss.eajse;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EajseXmlSchemaHelper implements SourceXmlSchemaHelper{

    static Logger log = Logger.getLogger(EajseXmlSchemaHelper.class);   

    static private final XmlDomMetadataExtractor.NodeValue AUTHOR_VALUE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {
            NodeList elementChildren = node.getChildNodes();
            if (elementChildren == null) return null;

            String name = null;
            for (int j = 0; j < elementChildren.getLength(); j++) {
                Node checkNode = elementChildren.item(j);
                String nodeName = checkNode.getNodeName();
                if ("name".equals(nodeName)) {
                    name = checkNode.getTextContent();
                }
            }

            if (name == null) {
                return null;
            }
            log.debug3("name found: " + name.toString());
            return name.toString();
        }
    };

    protected static final String article_title = "/ArticleSet/Article/ArticleTitle";
    protected static final String journal_title = "/ArticleSet/Article/Journal/JournalTitle";
    protected static final String author = "/ArticleSet/Article/AuthorList/Author";
    private static final String publisher = "/ArticleSet/Article/Journal/PublisherName";
    private static final String art_pubdate = "/ArticleSet/Article/Publication";
    private static final String volume = "/ArticleSet/Article/Journal/Volume";
    private static final String issue = "/ArticleSet/Article/Journal/Issue";
    private static final String issn = "/ArticleSet/Article/Journal/PISSN";
    private static final String eissn = "/ArticleSet/Article/Journal/EISSN";
    private static final String doi = "/ArticleSet/Article/DOI";
    protected static final String start_page = "/ArticleSet/Article/FirstPage";
    protected static final String end_page = "/ArticleSet/Article/LastPage";

    static private final Map<String,XPathValue>     
    articleMap = new HashMap<String,XPathValue>();
    static {
        articleMap.put(author, AUTHOR_VALUE);

    }

    @Override
    public Map<String, XPathValue> getArticleMetaMap() {
        return null;
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
        return null;
    }
    @Override
    public String getDeDuplicationXPathKey() {
        return null;
    }
    @Override
    public String getFilenameXPathKey() {
        return null;
    }
    @Override
    public Map<String, XPathValue> getGlobalMetaMap() {
        return null;
    }
}
