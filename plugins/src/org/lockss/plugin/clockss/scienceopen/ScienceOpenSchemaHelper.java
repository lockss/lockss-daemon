package org.lockss.plugin.clockss.scienceopen;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.util.Logger;
import java.util.Map;

public class ScienceOpenSchemaHelper extends JatsPublishingSchemaHelper {

    private static final Logger log = Logger.getLogger(ScienceOpenSchemaHelper.class);

    static private final String JATS_issn = "front/article-meta/product/issn";
    static private final String JATS_jtitle = "front/article-meta/product/publisher-name";

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
        theCookMap.put(JATS_issn, MetadataField.FIELD_ISSN);
        theCookMap.put(JATS_jtitle, MetadataField.FIELD_PUBLICATION_TITLE);
        return theCookMap;
    }
}

