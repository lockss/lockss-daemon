package org.lockss.plugin.clockss.aippublishing;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.Onix3BooksSchemaHelper;
import org.lockss.util.Logger;

import java.util.HashMap;
import java.util.Map;

public class AIPPublishingOnix3BooksSchemaHelper extends Onix3BooksSchemaHelper {

    static Logger log = Logger.getLogger(AIPPublishingOnix3BooksSchemaHelper.class);

    public static String ONIX_website_url = "PublishingDetail/Publisher/Website/WebsiteLink";
    private Map<String, XmlDomMetadataExtractor.XPathValue> ONIX_articleMap =
            new HashMap<String, XmlDomMetadataExtractor.XPathValue>();

    private MultiValueMap cookMap = new MultiValueMap();

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
        ONIX_articleMap = super.getArticleMetaMap();
        ONIX_articleMap.put(ONIX_website_url, XmlDomMetadataExtractor.TEXT_VALUE);
        return ONIX_articleMap;
    }
}

