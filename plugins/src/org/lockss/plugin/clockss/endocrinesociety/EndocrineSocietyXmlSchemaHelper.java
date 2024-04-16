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

package org.lockss.plugin.clockss.endocrinesociety;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.extractor.XmlDomMetadataExtractor.XPathValue;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;

public class EndocrineSocietyXmlSchemaHelper implements SourceXmlSchemaHelper {

    static Logger log = Logger.getLogger(EndocrineSocietyXmlSchemaHelper.class);

    protected static final String articleNode = "/metadatas/metadata";

    protected static final String article_title = articleNode + "/title";
    protected static final String publisher = articleNode + "/publisher";
    protected static final String isbn = articleNode + "/pisbn";
    protected static final String eisbn = articleNode + "/eisbn";
    protected static final String date = articleNode + "/yop";
    protected static final String id = articleNode + "/id";

    static private final Map<String,XPathValue>
    articleMap = new HashMap<String,XPathValue>();
    static {
        articleMap.put(article_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(isbn, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(eisbn, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(date, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(id, XmlDomMetadataExtractor.TEXT_VALUE);
    }


    static private final Map<String,XPathValue> globalMap = null;

    protected static final MultiValueMap cookMap = new MultiValueMap();
    static {
        cookMap.put(article_title, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
        cookMap.put(isbn, MetadataField.FIELD_ISBN);
        cookMap.put(eisbn, MetadataField.FIELD_EISBN);
        cookMap.put(date, MetadataField.FIELD_DATE);
        cookMap.put(id, MetadataField.FIELD_PROPRIETARY_IDENTIFIER);
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
        return articleNode;
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
