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

package org.lockss.plugin.clockss.scienceopen;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.util.Logger;
import java.util.Map;

public class ScienceOpenSchemaHelper extends JatsPublishingSchemaHelper {

    private static final Logger log = Logger.getLogger(ScienceOpenSchemaHelper.class);

    static protected final String JATS_issn = "front/article-meta/product/issn";
    static protected final String JATS_jtitle = "front/article-meta/product/publisher-name";

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {

        Map<String, XmlDomMetadataExtractor.XPathValue> JATS_articleMap = super.getArticleMetaMap();
        JATS_articleMap.put(JATS_issn, XmlDomMetadataExtractor.TEXT_VALUE);
        JATS_articleMap.put(JATS_jtitle, XmlDomMetadataExtractor.TEXT_VALUE);
        return JATS_articleMap;
    }

    @Override
    public MultiValueMap getCookMap() {
        MultiValueMap theCookMap = super.getCookMap();
        // only one of these is present, but if both JATS_jtitle overrides.
        theCookMap.put(JATS_pubname, MetadataField.FIELD_PUBLISHER);
        theCookMap.put(JATS_jtitle, MetadataField.FIELD_PUBLISHER);
        return theCookMap;
    }
}

