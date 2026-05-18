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

import org.lockss.plugin.associationforcomputingmachinery.ACMBitsPublishingSchemaHelper;
import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;

import java.util.Map;

public class SageIosBitsSchemaHelper extends ACMBitsPublishingSchemaHelper{
    //Some xml paths are missing from ACM Bits Publishing Schema Helper.
    private static String BITS_doi = "/book-part-wrapper/book-meta/book-id[@book-id-type=\"doi\"]";
    private static String BITS_eisbn = "/book-part-wrapper/book-meta/isbn[@publication-format=\"electronic\"]";
    private static String BITS_issn = "/book-part-wrapper/collection-meta/issn[@publication-format=\"print\"]";
    private static String BITS_eissn = "/book-part-wrapper/collection-meta/issn[@publication-format=\"electronic\"]";
    private static String BITS_volume = "/book-part-wrapper/book-meta/book-volume-number";

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
        Map<String, XmlDomMetadataExtractor.XPathValue> BITS_articleMap = super.getArticleMetaMap();
        BITS_articleMap.put(BITS_doi, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_eisbn, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_issn, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_eissn, XmlDomMetadataExtractor.TEXT_VALUE);
        BITS_articleMap.put(BITS_volume, XmlDomMetadataExtractor.TEXT_VALUE);
        return BITS_articleMap;
    }

    @Override
    public MultiValueMap getCookMap() {
        MultiValueMap theCookMap = super.getCookMap();
        theCookMap.put(BITS_doi, MetadataField.FIELD_DOI);
        theCookMap.put(BITS_eisbn, MetadataField.FIELD_EISBN);
        theCookMap.put(BITS_issn, MetadataField.FIELD_ISSN);
        theCookMap.put(BITS_eissn, MetadataField.FIELD_EISSN);
        theCookMap.put(BITS_volume, MetadataField.FIELD_VOLUME);
        return theCookMap;
    }
}
