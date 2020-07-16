package org.lockss.plugin.clockss.isecs;

import org.apache.commons.collections.map.MultiValueMap;
import org.lockss.extractor.MetadataField;
import org.lockss.extractor.XmlDomMetadataExtractor;
import org.lockss.plugin.clockss.SourceXmlParserHelperUtilities;
import org.lockss.plugin.clockss.SourceXmlSchemaHelper;
import org.lockss.util.Logger;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

public class InternationalStructuralEngineeringConstructionSocietyCrossRefQuerySchemaHelper implements SourceXmlSchemaHelper {

    private static final Logger log = Logger.getLogger(InternationalStructuralEngineeringConstructionSocietyCrossRefQuerySchemaHelper.class);

    // this is global for all articles in the file
    private static final String publisher = "/doi_batch/head/registrant";

    private static String pub_title = "/doi_batch/body/journal/journal_metadata/full_title";
    private static String art_issn = "/doi_batch/body/journal/journal_metadata/issn";

    private static String pub_volume = "/doi_batch/body/journal/journal_issue/journal_volume/volume";
    public static String pub_issue = "/doi_batch/body/journal/journal_issue/issue";
    public static String pub_year = "/doi_batch/body/journal/journal_issue/publication_date/year";

    // journal article, each reprents an article
    private static String art_doi = "./doi_data/doi";
    private static String art_contrib = "./contributors/person_name";
    public static String art_resource = "./doi_data/resource";
    private static String art_title = "./titles/title";
    private static String art_date = "./publication_date";
    private static String access_url = "./doi_data/collection[@property=\"crawler-based\"]/item[@crawler=\"iParadigms\"]/resource";

    private final static XmlDomMetadataExtractor.NodeValue AUTHOR_VALUE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {
            return SourceXmlParserHelperUtilities.getAuthorNameFromAuthorNameXpathNodeValue(node);
        }
    };

    private final static XmlDomMetadataExtractor.NodeValue PUBDATE_VALUE = new XmlDomMetadataExtractor.NodeValue() {
        @Override
        public String getValue(Node node) {
            return SourceXmlParserHelperUtilities.getPubDateFromPubDateXpathNodeValue(node);
        }
    };

    /*
     *  The following 3 variables are needed to use the XPathXmlMetadataParser
     */

    /* 1.  MAP associating xpath & value type definition or evaluator */
    static private final Map<String, XmlDomMetadataExtractor.XPathValue>
            articleMap = new HashMap<String, XmlDomMetadataExtractor.XPathValue>();

    static {
        articleMap.put(publisher, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_year, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_doi, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_resource, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_contrib, AUTHOR_VALUE);
        articleMap.put(art_title, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_volume, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(pub_issue, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(art_date, PUBDATE_VALUE);
        articleMap.put(art_issn, XmlDomMetadataExtractor.TEXT_VALUE);
        articleMap.put(access_url, XmlDomMetadataExtractor.TEXT_VALUE);
    }

    /* 2.  Top level per-article node */
    static private final String articleNode = "/doi_batch/body//journal//journal_article";

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
        cookMap.put(publisher, MetadataField.FIELD_PUBLISHER);
        cookMap.put(pub_title, MetadataField.FIELD_PUBLICATION_TITLE);
        cookMap.put(art_date, MetadataField.FIELD_DATE);
        cookMap.put(art_doi, MetadataField.FIELD_DOI);
        cookMap.put(art_contrib, MetadataField.FIELD_AUTHOR);
        cookMap.put(art_title, MetadataField.FIELD_ARTICLE_TITLE);
        cookMap.put(art_issn, MetadataField.FIELD_ISSN);
        cookMap.put(pub_volume, MetadataField.FIELD_VOLUME);
        cookMap.put(pub_issue, MetadataField.FIELD_ISSUE);
        cookMap.put(access_url, MetadataField.FIELD_ACCESS_URL);
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
        return null;
    }
}


