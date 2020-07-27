package org.lockss.plugin.clockss.apma;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.util.Logger;

import java.util.Map;

public class AmericanPodiatricMedicalAssociationSchemaHelper extends JatsPublishingSchemaHelper {

    private static final Logger log = Logger.getLogger(AmericanPodiatricMedicalAssociationSchemaHelper.class);


    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {

        Map<String, XmlDomMetadataExtractor.XPathValue> JATS_articleMap = super.getArticleMetaMap();
        return JATS_articleMap;
    }

    @Override
    public MultiValueMap getCookMap() {
        MultiValueMap theCookMap = super.getCookMap();
        theCookMap.put(JATS_pubname, MetadataField.FIELD_PUBLISHER);
        return theCookMap;
    }
}

