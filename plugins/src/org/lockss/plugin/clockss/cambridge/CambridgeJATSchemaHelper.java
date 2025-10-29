package org.lockss.plugin.clockss.cambridge;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.JatsPublishingSchemaHelper;
import org.lockss.util.Logger;
import java.util.Map;

public class CambridgeJATSchemaHelper extends JatsPublishingSchemaHelper {

    private static final Logger log = Logger.getLogger(CambridgeJATSchemaHelper.class);


    static protected final String ArticleReviewTitle =  "front/article-meta/product[@product-type = \"book\" ]/source";
    static protected final String ArticleReviewTitleAlt =  "front/article-meta/product/source";
    static protected final String ArticleReviewTitleAlt2 =  "front/article-meta/product";

    @Override
    public Map<String, XmlDomMetadataExtractor.XPathValue> getArticleMetaMap() {
        Map<String, XmlDomMetadataExtractor.XPathValue> theMap = super.getArticleMetaMap();
        theMap.put(ArticleReviewTitle, XmlDomMetadataExtractor.TEXT_VALUE);
        theMap.put(ArticleReviewTitleAlt, XmlDomMetadataExtractor.TEXT_VALUE);
        theMap.put(ArticleReviewTitleAlt2, XmlDomMetadataExtractor.TEXT_VALUE);
        return theMap;
    }


    @Override
    public MultiValueMap getCookMap() {
        MultiValueMap theCookMap = super.getCookMap();
        theCookMap.put(ArticleReviewTitle, MetadataField.FIELD_ARTICLE_TITLE);
        theCookMap.put(ArticleReviewTitleAlt, MetadataField.FIELD_ARTICLE_TITLE);
        theCookMap.put(ArticleReviewTitleAlt2, MetadataField.FIELD_ARTICLE_TITLE);
        return theCookMap;
    }
}

